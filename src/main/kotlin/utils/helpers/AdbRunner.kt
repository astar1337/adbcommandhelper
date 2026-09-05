package utils.helpers

import java.util.concurrent.TimeUnit

fun runAdbWithTimeout(
    vararg command: String,
    timeoutSeconds: Long = 5
): String {
    return try {
        val process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)

        if (!completed) {
            process.destroyForcibly()
            ""
        } else {
            output.trim()
        }
    } catch (e: Exception) {
        ""
    }
}