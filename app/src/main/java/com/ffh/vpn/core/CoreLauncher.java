package com.ffh.vpn.core;

/**
 * Starts the Xray binary without going through {@link ProcessBuilder}.
 *
 * <p>{@code ProcessBuilder} on Android closes every file descriptor except
 * stdin, stdout and stderr in the child. The VPN tunnel fd would be gone
 * before Xray could read it, the interface would stay up, and no packet would
 * ever leave the device. This launcher forks and execs itself so the fd is
 * still open after exec.
 */
public final class CoreLauncher {

    public static final boolean loaded;

    static {
        boolean ok = false;
        try {
            System.loadLibrary("ffh");
            ok = true;
        } catch (Throwable ignored) {
            ok = false;
        }
        loaded = ok;
    }

    private CoreLauncher() {
    }

    /**
     * @return {@code [pid, logFd, tunFdSeenByChild]} or null when spawn failed
     */
    public static synchronized native int[] spawn(
            String binary,
            String[] argv,
            String[] env,
            int tunFd
    );

    public static native int lastError();

    /** {@code pid} may be negative, which signals the whole process group. */
    public static native int signal(int pid, int sig);

    public static native boolean alive(int pid);

    /** @return -1 still running, -2 wait failed, otherwise the exit code */
    public static native int waitPid(int pid, boolean noHang);
}
