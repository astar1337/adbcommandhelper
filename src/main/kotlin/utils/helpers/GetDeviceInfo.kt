package utils.helpers


fun getDeviceName(deviceId: String): String {
    val process = ProcessBuilder(
        AppConfig.adbPath, "-s", deviceId, "shell", "settings", "get", "global", "device_name"
    ).start()

    val deviceName = process.inputStream.bufferedReader().readText().trim()
    return deviceName
}

fun getDeviceModel(deviceId: String): String {
    val process = ProcessBuilder(
        AppConfig.adbPath, "-s", deviceId, "shell", "getprop", "ro.product.model"
    ).start()

    val model = process.inputStream.bufferedReader().readText().trim()
    return model
}

fun getDeviceWifiIp(deviceId: String): String {
    return try {
        val process = ProcessBuilder(
            AppConfig.adbPath, "-s", deviceId, "shell", "ip", "-f", "inet", "addr", "show", "wlan0"
        ).redirectErrorStream(true).start()

        val output = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()

        val inetRegex = """inet\s+(\d+\.\d+\.\d+\.\d+)/\d+""".toRegex()
        val match = inetRegex.find(output)

        if (match != null) {
            match.groupValues[1]
        } else {
            "Not connected to WiFi"
        }
    } catch (e: Exception) {
        "N/A"
    }
}