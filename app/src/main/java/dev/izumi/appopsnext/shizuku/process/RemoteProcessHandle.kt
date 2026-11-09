package dev.izumi.appopsnext.shizuku.process

import android.os.ParcelFileDescriptor
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import moe.shizuku.server.IRemoteProcess

/**
 * Small ownership wrapper around a process started by the privileged backend:
 * either Shizuku's internal remote-process Binder or a local `su` process.
 *
 * Keeping this type in one package prevents the compatibility backend from
 * leaking Shizuku's deprecated process API into AppOps business code.
 */
internal class RemoteProcessHandle private constructor(
    private val remoteProcess: IRemoteProcess?,
    private val localProcess: Process?,
) {
    constructor(remoteProcess: IRemoteProcess) : this(remoteProcess, null)

    constructor(localProcess: Process) : this(null, localProcess)

    val stdin: OutputStream by lazy {
        localProcess?.outputStream
            ?: ParcelFileDescriptor.AutoCloseOutputStream(remoteProcess!!.outputStream)
    }

    val stdout: InputStream by lazy {
        localProcess?.inputStream
            ?: ParcelFileDescriptor.AutoCloseInputStream(remoteProcess!!.inputStream)
    }

    val stderr: InputStream by lazy {
        localProcess?.errorStream
            ?: ParcelFileDescriptor.AutoCloseInputStream(remoteProcess!!.errorStream)
    }

    fun waitFor(
        timeout: Long,
        unit: TimeUnit,
    ): Boolean = localProcess?.waitFor(timeout, unit)
        ?: remoteProcess!!.waitForTimeout(timeout, unit.name)

    fun exitValue(): Int = localProcess?.exitValue() ?: remoteProcess!!.exitValue()

    fun isAlive(): Boolean = localProcess?.isAlive ?: remoteProcess!!.alive()

    fun destroy() {
        runCatching { localProcess?.destroy() ?: remoteProcess?.destroy() }
        runCatching { stdin.close() }
        runCatching { stdout.close() }
        runCatching { stderr.close() }
    }
}
