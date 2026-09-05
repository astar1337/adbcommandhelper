package utils.helpers

object VideoRecorder {
    private var recordingProcess: Process? = null
    private var lastFileName: String = ""
    private var lastFilePath: String = ""

    fun startRecording(deviceId: String, fileName: String, noSound: Boolean, showTouches: Boolean): Boolean {
        if (recordingProcess != null) return false

        val outputPath = "${System.getProperty("user.home")}/Desktop/$fileName.mp4"
        lastFileName = fileName
        lastFilePath = outputPath

        val command = mutableListOf(
            AppConfig.scrcpyPath,
            "-s", deviceId,
            "--record", outputPath
        )

        if (noSound) {
            command.add("--no-audio")
        }

        if (showTouches) {
            command.add("--show-touches")
        }

        return try {
            val pb = ProcessBuilder(command)
            val env = pb.environment()
            val adbDir = java.io.File(AppConfig.adbPath).parent ?: ""
            val scrcpyDir = java.io.File(AppConfig.scrcpyPath).parent ?: ""
            env["PATH"] = listOf(adbDir, scrcpyDir, "/usr/local/bin", "/opt/homebrew/bin", "/usr/bin")
                .filter { it.isNotEmpty() }
                .joinToString(":") + ":" + (env["PATH"] ?: "")

            recordingProcess = pb
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
            true
        } catch (e: Exception) {
            recordingProcess = null
            false
        }
    }

    fun isScrcpyInstalled(): Boolean {
        return try {
            val pb = ProcessBuilder(AppConfig.scrcpyPath, "--version")
            val env = pb.environment()
            val scrcpyDir = java.io.File(AppConfig.scrcpyPath).parent ?: ""
            env["PATH"] = listOf(scrcpyDir, "/usr/local/bin", "/opt/homebrew/bin", "/usr/bin")
                .filter { it.isNotEmpty() }
                .joinToString(":") + ":" + (env["PATH"] ?: "")

            pb.redirectErrorStream(true)
            val process = pb.start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.contains("scrcpy")
        } catch (e: Exception) {
            false
        }
    }

    fun stopRecording(): Boolean {
        val process = recordingProcess ?: return false
        return try {
            process.destroy()
            process.waitFor()
            recordingProcess = null
            true
        } catch (e: Exception) {
            recordingProcess = null
            false
        }
    }

    fun getLastFileName(): String = lastFileName
}