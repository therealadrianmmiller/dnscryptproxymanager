package io.github.dnscryptmonitor

import java.util.concurrent.TimeUnit

data class ShellResult(val exitCode: Int, val output: String) {
    val ok get() = exitCode == 0
}

object RootShell {
    fun execute(command: String, timeoutSeconds: Long = 30): ShellResult {
        return try {
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()
            val output = StringBuilder()
            val reader = Thread {
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        // Keep draining stdout to prevent a full pipe blocking the root process.
                        if (output.length < MAX_CAPTURE_CHARS) {
                            output.appendLine(line)
                        }
                    }
                }
            }.apply {
                isDaemon = true
                start()
            }
            val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                ShellResult(-1, "Command timed out")
            } else {
                reader.join(1_000)
                ShellResult(process.exitValue(), output.toString().trim())
            }
        } catch (error: Exception) {
            ShellResult(-1, error.message ?: "Unable to run root command")
        }
    }

    fun quote(value: String): String = "'" + value.replace("'", "'\"'\"'") + "'"

    private const val MAX_CAPTURE_CHARS = 4_000_000
}
