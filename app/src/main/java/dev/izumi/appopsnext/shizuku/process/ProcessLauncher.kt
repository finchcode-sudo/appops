package dev.izumi.appopsnext.shizuku.process

internal interface ProcessLauncher {
    fun launch(
        arguments: List<String>,
        environment: Map<String, String> = emptyMap(),
        workingDirectory: String? = null,
    ): RemoteProcessHandle
}

/** Runs the command as root through `su -c`. */
internal class RootProcessLauncher : ProcessLauncher {
    override fun launch(
        arguments: List<String>,
        environment: Map<String, String>,
        workingDirectory: String?,
    ): RemoteProcessHandle {
        require(arguments.isNotEmpty()) { "A process requires at least one argument" }
        val command = buildString {
            if (workingDirectory != null) append("cd ${quote(workingDirectory)} && ")
            environment.forEach { (name, value) -> append("$name=${quote(value)} ") }
            append("exec ")
            append(arguments.joinToString(" ") { quote(it) })
        }
        return RemoteProcessHandle(ProcessBuilder("su", "-c", command).start())
    }

    private fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
}
