#include <jni.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <signal.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/prctl.h>
#include <sys/wait.h>

/* Fd number the core will see. dup2'd in the child so CLOEXEC on the original
 * TUN fd does not matter and the number is stable. */
#define TUN_TARGET 150

static int last_error = 0;

static void set_fd_cloexec(int fd, int on) {
    int flags = fcntl(fd, F_GETFD);
    if (flags < 0) return;
    if (on) flags |= FD_CLOEXEC;
    else flags &= ~FD_CLOEXEC;
    fcntl(fd, F_SETFD, flags);
}

static char *dup_string(JNIEnv *env, jstring value) {
    const char *utf = (*env)->GetStringUTFChars(env, value, NULL);
    if (utf == NULL) return NULL;
    char *copy = strdup(utf);
    (*env)->ReleaseStringUTFChars(env, value, utf);
    return copy;
}

static char **dup_array(JNIEnv *env, jobjectArray values, int extra) {
    jsize count = values == NULL ? 0 : (*env)->GetArrayLength(env, values);
    char **out = calloc((size_t)count + (size_t)extra + 1, sizeof(char *));
    if (out == NULL) return NULL;
    for (jsize i = 0; i < count; i++) {
        jstring item = (jstring)(*env)->GetObjectArrayElement(env, values, i);
        if (item == NULL) continue;
        out[i] = dup_string(env, item);
        (*env)->DeleteLocalRef(env, item);
        if (out[i] == NULL) {
            for (jsize j = 0; j < i; j++) free(out[j]);
            free(out);
            return NULL;
        }
    }
    return out;
}

static void free_array(char **values) {
    if (values == NULL) return;
    for (int i = 0; values[i] != NULL; i++) free(values[i]);
    free(values);
}

/* Rewrite XRAY_TUN_FD / xray.tun.fd so they name the fd the child actually has. */
static void rewrite_tun_env(char **envp, int target) {
    char rendered[64];
    snprintf(rendered, sizeof(rendered), "%d", target);
    for (int i = 0; envp[i] != NULL; i++) {
        const char *key = NULL;
        if (strncmp(envp[i], "XRAY_TUN_FD=", 12) == 0) key = "XRAY_TUN_FD=";
        else if (strncmp(envp[i], "xray.tun.fd=", 12) == 0) key = "xray.tun.fd=";
        if (key == NULL) continue;
        size_t key_len = strlen(key);
        char *updated = malloc(key_len + strlen(rendered) + 1);
        if (updated == NULL) continue;
        memcpy(updated, key, key_len);
        strcpy(updated + key_len, rendered);
        free(envp[i]);
        envp[i] = updated;
    }
}

static void child_exec(int tun_fd, int log_write, char *binary, char **argv, char **envp) {
    /* Die with the app. Otherwise a killed UI leaves an orphan core holding
     * the tunnel and the next start cannot bind it. */
    prctl(PR_SET_PDEATHSIG, SIGKILL);
    if (getppid() == 1) _exit(1);
    setpgid(0, 0);

    int devnull = open("/dev/null", O_RDONLY);
    if (devnull >= 0) {
        dup2(devnull, STDIN_FILENO);
        if (devnull > STDERR_FILENO) close(devnull);
    }
    dup2(log_write, STDOUT_FILENO);
    dup2(log_write, STDERR_FILENO);

    if (tun_fd >= 0) {
        if (dup2(tun_fd, TUN_TARGET) < 0) _exit(126);
        set_fd_cloexec(TUN_TARGET, 0);
    }

    /* Constant bound: sysconf is not async-signal-safe. Android app fds sit well below this. */
    for (int fd = STDERR_FILENO + 1; fd < 1024; fd++) {
        if (tun_fd >= 0 && fd == TUN_TARGET) continue;
        close(fd);
    }

    execve(binary, argv, envp);
    _exit(127);
}

JNIEXPORT jintArray JNICALL
Java_com_ffh_vpn_core_CoreLauncher_spawn(
        JNIEnv *env, jclass clazz,
        jstring j_binary, jobjectArray j_argv, jobjectArray j_env, jint tun_fd) {
    (void)clazz;
    last_error = 0;

    char *binary = dup_string(env, j_binary);
    char **argv = dup_array(env, j_argv, 0);
    char **envp = dup_array(env, j_env, 0);
    if (binary == NULL || argv == NULL || envp == NULL) {
        last_error = ENOMEM;
        free(binary);
        free_array(argv);
        free_array(envp);
        return NULL;
    }

    if (tun_fd >= 0) rewrite_tun_env(envp, TUN_TARGET);

    int log_pipe[2];
    if (pipe(log_pipe) != 0) {
        last_error = errno;
        free(binary);
        free_array(argv);
        free_array(envp);
        return NULL;
    }
    set_fd_cloexec(log_pipe[0], 1);
    set_fd_cloexec(log_pipe[1], 1);

    pid_t pid = fork();
    if (pid < 0) {
        last_error = errno;
        close(log_pipe[0]);
        close(log_pipe[1]);
        free(binary);
        free_array(argv);
        free_array(envp);
        return NULL;
    }
    if (pid == 0) {
        close(log_pipe[0]);
        child_exec(tun_fd, log_pipe[1], binary, argv, envp);
    }

    close(log_pipe[1]);
    free(binary);
    free_array(argv);
    free_array(envp);

    jint values[3];
    values[0] = (jint)pid;
    values[1] = log_pipe[0];
    values[2] = tun_fd >= 0 ? TUN_TARGET : -1;
    jintArray result = (*env)->NewIntArray(env, 3);
    if (result == NULL) {
        close(log_pipe[0]);
        return NULL;
    }
    (*env)->SetIntArrayRegion(env, result, 0, 3, values);
    return result;
}

JNIEXPORT jint JNICALL
Java_com_ffh_vpn_core_CoreLauncher_lastError(JNIEnv *env, jclass clazz) {
    (void)env;
    (void)clazz;
    return last_error;
}

JNIEXPORT jint JNICALL
Java_com_ffh_vpn_core_CoreLauncher_signal(JNIEnv *env, jclass clazz, jint pid, jint sig) {
    (void)env;
    (void)clazz;
    if (pid == 0) return -1;
    return kill((pid_t)pid, sig);
}

JNIEXPORT jboolean JNICALL
Java_com_ffh_vpn_core_CoreLauncher_alive(JNIEnv *env, jclass clazz, jint pid) {
    (void)env;
    (void)clazz;
    if (pid <= 0) return JNI_FALSE;
    return kill((pid_t)pid, 0) == 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_com_ffh_vpn_core_CoreLauncher_waitPid(JNIEnv *env, jclass clazz, jint pid, jboolean no_hang) {
    (void)env;
    (void)clazz;
    int status = 0;
    pid_t result = waitpid((pid_t)pid, &status, no_hang ? WNOHANG : 0);
    if (result == 0) return -1;
    if (result < 0) return -2;
    if (WIFEXITED(status)) return WEXITSTATUS(status);
    if (WIFSIGNALED(status)) return 128 + WTERMSIG(status);
    return status;
}
