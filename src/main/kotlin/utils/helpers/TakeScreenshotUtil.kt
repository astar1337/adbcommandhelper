package utils.helpers

import java.io.File

fun takeScreenshotToClipboard(deviceId: String): String {
    val tempPath = "/tmp/adb.png"
    return try {
        val adbProcess = ProcessBuilder(
            AppConfig.adbPath, "-s", deviceId, "exec-out", "screencap", "-p"
        ).start()

        val outputFile = File(tempPath)
        adbProcess.inputStream.use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val adbExitCode = adbProcess.waitFor()
        if (adbExitCode != 0) return "Error: ADB failed to capture screen (Code $adbExitCode)"

        val appleScript = "set the clipboard to (read (POSIX file \"$tempPath\") as picture)"
        val scriptProcess = ProcessBuilder("osascript", "-e", appleScript).start()
        val scriptExitCode = scriptProcess.waitFor()

        if (scriptExitCode == 0) {
            "Screenshot captured from $deviceId and copied to clipboard successfully."
        } else {
            "Screenshot saved to $tempPath, but failed to copy to clipboard."
        }
    } catch (e: Exception) {
        "Failed to take screenshot: ${e.message}"
    }
}