package utils.helpers


fun getAirplaneModeState(deviceId: String): Boolean {
    return try {
        val process = ProcessBuilder(
            AppConfig.adbPath, "-s", deviceId, "shell",
            "settings", "get", "global", "airplane_mode_on"
        ).start()

        val output = process.inputStream.bufferedReader().readText().trim()
        output == "1"
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
//0 = OFF 1 = ON
fun getWifiState(deviceId: String): Boolean {
    return try {
        val process = ProcessBuilder(
            AppConfig.adbPath, "-s", deviceId, "shell",
            "settings", "get", "global", "wifi_on"
        ).start()

        val output = process.inputStream.bufferedReader().readText().trim()
        // 0 = Fully Disabled
        // 1 = Enabled
        // 2 = Enabled (but restricted or transitioning due to Airplane mode I think)
        output == "1" || output == "2" // FIXED THIS STATE BY ADDING OUTPUT ==2 BECAUSE Wi-Fi SETTING HAS TWO STATES
    } catch (e: Exception) {
        false
    }
}

