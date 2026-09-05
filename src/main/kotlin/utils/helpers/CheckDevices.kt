package utils.helpers

import java.io.BufferedReader
import java.io.InputStreamReader


data class AdbDeviceId(
    val serialNumber: String,
    val status: String
)

data class AdbDevicesResult(
    val devices: List<AdbDeviceId>,
    val rawOutput: String,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

object AdbDevices {

    private fun checkDevices(): AdbDevicesResult {
        return try {
            val process = ProcessBuilder(AppConfig.adbPath, "devices")
                .redirectErrorStream(true)
                .start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            reader.close()

            val exitCode = process.waitFor()

            if (exitCode == 0) {
                val devices = parseAdbDevices(output)
                AdbDevicesResult(
                    devices = devices,
                    rawOutput = output,
                    isSuccess = true
                )
            } else {
                AdbDevicesResult(
                    devices = emptyList(),
                    rawOutput = output,
                    isSuccess = false,
                    errorMessage = "ADB command failed with exit code: $exitCode"
                )
            }
        } catch (e: Exception) {
            AdbDevicesResult(
                devices = emptyList(),
                rawOutput = "",
                isSuccess = false,
                errorMessage = "Error executing ADB: ${e.message}\n"
            )
        }
    }

    fun runCommandFromMap(command: String): AdbDevicesResult {
        return try {
            val commandList = command.split(" ").filter { it.isNotEmpty() }.toMutableList()

            // Replace "adb" with resolved path
            if (commandList.isNotEmpty() && commandList[0] == AppConfig.adbPath) {
                commandList[0] = AppConfig.adbPath
            }

            val process = ProcessBuilder(commandList)
                .redirectErrorStream(true)
                .start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            reader.close()

            val exitCode = process.waitFor()

            if (exitCode == 0) {
                val devices = parseAdbDevices(output)
                AdbDevicesResult(
                    devices = devices,
                    rawOutput = output,
                    isSuccess = true
                )
            } else {
                AdbDevicesResult(
                    devices = emptyList(),
                    rawOutput = output,
                    isSuccess = false,
                    errorMessage = "ADB command failed with exit code: $exitCode"
                )
            }
        } catch (e: Exception) {
            AdbDevicesResult(
                devices = emptyList(),
                rawOutput = "",
                isSuccess = false,
                errorMessage = "Error executing ADB: ${e.message}\n" +
                        "Make sure ADB is installed and in your PATH"
            )
        }
    }

    private fun parseAdbDevices(output: String): List<AdbDeviceId> {
        val devices = mutableListOf<AdbDeviceId>()

        val lines = output.lines()

        for (i in 1 until lines.size) {
            val line = lines[i].trim()

            if (line.isEmpty()) continue

            val parts = line.split(Regex("\\s+"))

            if (parts.size >= 2) {
                val deviceIdNumber = parts[0]
                val status = parts[1]
                devices.add(AdbDeviceId(deviceIdNumber, status))
            }
        }
        return devices
    }

    fun getDevicesList(): List<String> {
        val result = checkDevices()

        if (!result.isSuccess || result.devices.isEmpty()) {
            return emptyList()
        }
        return result.devices.map { it.serialNumber }
    }
}

