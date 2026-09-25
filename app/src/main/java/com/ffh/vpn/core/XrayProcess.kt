package com.ffh.vpn.core

import android.content.Context
import com.ffh.vpn.data.LogStore
import java.io.File
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
    private var process: Process? = null

    @Volatile
    private var reader: Thread? = null

    val isRunning: Boolean get() = running.get()

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

        val file = configFile(context)
        file.writeText(config)
        prepareAssets(context)

        val exe = binary(context)
        if (!exe.exists()) {
            LogStore.append("core", "xray binary is missing at ${exe.absolutePath}")
            return false
        }

        return try {
            val builder = ProcessBuilder(exe.absolutePath, "run", "-c", file.absolutePath)
            builder.directory(File(context.filesDir, "ffh"))
            builder.redirectErrorStream(true)
            val env = builder.environment()
            env["XRAY_TUN_FD"] = tunFd.toString()
            env["XRAY_LOCATION_ASSET"] = assetsDir(context).absolutePath
            env["XRAY_LOCATION_CONFIG"] = File(context.filesDir, "ffh").absolutePath
            env["TMPDIR"] = context.cacheDir.absolutePath

            val started = builder.start()
            process = started
            running.set(true)
            reader = Thread {
                runCatching {
                    started.inputStream.bufferedReader().useLines { lines ->
                        for (line in lines) {
                            LogStore.append("xray", line)
                        }
                    }
                }
                running.set(false)
            }.also { it.isDaemon = true; it.start() }
            LogStore.append("core", "started with tun fd $tunFd")
            true
        } catch (t: Throwable) {
            LogStore.append("core", "failed to start: ${t.message}")
            false
        }
    }

    fun stop() {
        val current = process ?: run {
            running.set(false)
            return
        }
        try {
            current.destroy()
            runCatching {
                if (!current.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)) {
                    current.destroyForcibly()
                }
            }
        } catch (t: Throwable) {
            LogStore.append("core", "stop error: ${t.message}")
        } finally {
            process = null
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
            process.waitFor()
            output.lineSequence().firstOrNull()?.trim()?.takeIf { it.isNotBlank() } ?: "unknown"
        }.getOrDefault("unknown")
    }
}
