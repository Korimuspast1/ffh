package com.ffh.vpn.core

import android.content.Context
import android.os.ParcelFileDescriptor
import com.ffh.vpn.data.LogStore
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Owns the Xray-core child process.
 *
 * The core is shipped as `libxray.so` inside the APK (see
 * `.github/scripts/fetch-xray.sh`): Android refuses to execute files that live
 * in the app data directory, but it does extract — and allow execution of —
 * native libraries, so the official Xray binary is packaged as one.
 */
object XrayProcess {

    private const val LIB_NAME = "libxray.so"
    private const val CONFIG_NAME = "xray-config.json"
    private val running = AtomicBoolean(false)

    @Volatile
    private var handle: CoreHandle? = null

    @Volatile
    private var reader: Thread? = null

    @Volatile
    private var expectStop = false

    /** Invoked when the tunnel core exits without [stop] being called. */
    @Volatile
    var onUnexpectedExit: ((Int) -> Unit)? = null

    val isRunning: Boolean get() = running.get() && (handle?.isAlive() != false)

    fun binary(context: Context): File = File(context.applicationInfo.nativeLibraryDir, LIB_NAME)

    fun isAvailable(context: Context): Boolean {
        val file = binary(context)
        return file.exists() && file.canExecute()
    }

    fun assetsDir(context: Context): File {
        val dir = File(context.filesDir, "ffh/assets")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** Each latency probe needs its own config file, the tunnel owns the main one. */
    fun probeConfigFile(context: Context, port: Int): File {
        val dir = File(context.filesDir, "ffh/probe")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "probe-$port.json")
    }

    fun configFile(context: Context): File {
        val dir = File(context.filesDir, "ffh")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, CONFIG_NAME)
    }

    /** Extracts geoip.dat / geosite.dat from the APK assets on first launch. */
    fun prepareAssets(context: Context) {
        val dir = assetsDir(context)
        for (name in listOf("geoip.dat", "geosite.dat")) {
            val target = File(dir, name)
            if (target.exists() && target.length() > 0) continue
            runCatching {
                context.assets.open(name).use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }.onFailure {
                LogStore.append("core", "$name is not bundled: ${it.message}")
            }
        }
    }

    fun start(context: Context, config: String, tunFd: Int): Boolean {
        stop()
        expectStop = false

        val file = configFile(context)
        file.writeText(config)
        prepareAssets(context)

        if (tunFd >= 0 && !CoreLauncher.loaded) {
            LogStore.append("core", "native launcher is missing, the tunnel fd cannot be passed")
            return false
        }

        val started = launch(context, file, mapOf("XRAY_TUN_FD" to tunFd.toString(), "xray.tun.fd" to tunFd.toString()), tunFd)
            ?: return false

        handle = started
        running.set(true)
        reader = Thread {
            runCatching {
                started.logs.bufferedReader().useLines { lines ->
                    for (line in lines) LogStore.append("xray", line)
                }
            }
            val code = started.reap()
            running.set(false)
            if (!expectStop) {
                LogStore.append("core", "exited ($code)")
                onUnexpectedExit?.invoke(code)
            }
        }.also { it.isDaemon = true; it.name = "ffh-xray-log"; it.start() }
        LogStore.append("core", "started pid ${started.pid} tun fd ${started.tunFd}")
        return true
    }

    /**
     * Starts one detached core process. Used by the tunnel and by the
     * per-server latency probe (which runs several instances in parallel).
     *
     * [tunFd] is the VPN interface fd. Pass -1 when the process has no TUN
     * inbound (the probe).
     */
    fun launch(
        context: Context,
        configFile: File,
        extraEnv: Map<String, String> = emptyMap(),
        tunFd: Int = -1
    ): CoreHandle? {
        prepareAssets(context)
        val exe = binary(context)
        if (!exe.exists()) {
            LogStore.append("core", "xray binary is missing at ${exe.absolutePath}")
            return null
        }

        val env = LinkedHashMap<String, String>()
        env.putAll(System.getenv())
        env["XRAY_LOCATION_ASSET"] = assetsDir(context).absolutePath
        env["TMPDIR"] = context.cacheDir.absolutePath
        env.putAll(extraEnv)

        // The latency probe has no tunnel fd. The official Android binary is
        // built with cgo; a raw fork from this process leaves it unable to
        // bind, so the SOCKS port never opens. ProcessBuilder is the path
        // Android prepares for networking, and it is what the probe must use.
        if (tunFd < 0 || !CoreLauncher.loaded) {
            if (tunFd >= 0) {
                LogStore.append("core", "refusing to start the tunnel without the native launcher")
                return null
            }
            LogStore.append("core", "starting probe via process")
            return runCatching {
                val builder = ProcessBuilder(exe.absolutePath, "run", "-c", configFile.absolutePath)
                builder.directory(File(context.filesDir, "ffh"))
                builder.redirectErrorStream(true)
                builder.environment().apply {
                    putAll(env)
                    remove("XRAY_TUN_FD")
                    remove("xray.tun.fd")
                }
                CoreHandle.java(builder.start())
            }.getOrElse {
                LogStore.append("core", "failed to start: ${it.message}")
                null
            }
        }

        val argv = arrayOf(exe.absolutePath, "run", "-c", configFile.absolutePath)
        val envp = env.map { (key, value) -> "$key=$value" }.toTypedArray()
        val result = runCatching {
            CoreLauncher.spawn(exe.absolutePath, argv, envp, tunFd)
        }.getOrElse {
            LogStore.append("core", "spawn threw: ${it.message}")
            null
        }
        if (result == null || result.size < 2 || result[0] <= 0) {
            LogStore.append("core", "spawn failed (errno ${runCatching { CoreLauncher.lastError() }.getOrDefault(-1)})")
            return null
        }
        return CoreHandle.native(result[0], result[1], if (result.size > 2) result[2] else tunFd)
    }

    fun stop() {
        expectStop = true
        val current = handle ?: run {
            running.set(false)
            return
        }
        try {
            current.destroy()
            if (!current.waitFor(2_000)) current.destroyForcibly()
            current.waitFor(1_000)
            current.reap()
        } catch (t: Throwable) {
            LogStore.append("core", "stop error: ${t.message}")
        } finally {
            handle = null
            running.set(false)
            reader?.let { runCatching { it.interrupt() } }
            reader = null
        }
    }

    fun version(context: Context): String {
        if (!isAvailable(context)) return "not installed"
        return runCatching {
            val process = ProcessBuilder(binary(context).absolutePath, "version")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor(3, TimeUnit.SECONDS)
            output.lineSequence().firstOrNull()?.trim()?.takeIf { it.isNotBlank() } ?: "unknown"
        }.getOrDefault("unknown")
    }
}

/** A running core, either spawned natively (tunnel) or via [ProcessBuilder] (probe fallback). */
class CoreHandle private constructor(
    val pid: Int,
    val logs: InputStream,
    val tunFd: Int,
    private val nativePid: Boolean,
    private val javaProcess: Process?
) {
    fun isAlive(): Boolean = if (nativePid) {
        runCatching { CoreLauncher.alive(pid) }.getOrDefault(false)
    } else {
        javaProcess?.isAlive == true
    }

    fun destroy() {
        if (nativePid) {
            runCatching { CoreLauncher.signal(pid, SIGTERM) }
            runCatching { CoreLauncher.signal(-pid, SIGTERM) }
        } else {
            javaProcess?.destroy()
        }
    }

    fun destroyForcibly() {
        if (nativePid) {
            runCatching { CoreLauncher.signal(pid, SIGKILL) }
            runCatching { CoreLauncher.signal(-pid, SIGKILL) }
        } else {
            javaProcess?.destroyForcibly()
        }
    }

    fun waitFor(timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (!isAlive()) return true
            Thread.sleep(50)
        }
        return !isAlive()
    }

    /** Reaps a native child so it does not stay a zombie. Safe to call twice. */
    fun reap(): Int {
        if (!nativePid) {
            return runCatching { javaProcess?.waitFor() ?: -2 }.getOrDefault(-2)
        }
        return runCatching { CoreLauncher.waitPid(pid, true) }.getOrDefault(-2)
    }

    companion object {
        private const val SIGTERM = 15
        private const val SIGKILL = 9

        fun native(pid: Int, logFd: Int, tunFd: Int): CoreHandle {
            val stream = ParcelFileDescriptor.AutoCloseInputStream(ParcelFileDescriptor.adoptFd(logFd))
            return CoreHandle(pid, stream, tunFd, nativePid = true, javaProcess = null)
        }

        fun java(process: Process): CoreHandle =
            CoreHandle(pid = -1, logs = process.inputStream, tunFd = -1, nativePid = false, javaProcess = process)
    }
}
