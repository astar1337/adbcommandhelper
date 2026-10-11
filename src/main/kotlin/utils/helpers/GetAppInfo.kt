package utils.helpers




fun getAppVersion(deviceId: String, appName: String): String {
    val command =
        "dumpsys package $appName | " +
                "grep -E \"versionName|versionCode\" | " +
                "sed -E 's/^[^=]*=//'"
    val process = ProcessBuilder(
        AppConfig.adbPath,
        "-s", deviceId,
        "shell",
        command
    )
        .redirectErrorStream(true)
        .start()

    val output = process.inputStream.bufferedReader().readText().trim()
    println("Output:\n$output")
    return output
}


fun getAppSizes(deviceId: String, appName: String): String {
    val process = ProcessBuilder(
        AppConfig.adbPath,
        "-s", deviceId,
        "shell",
        "dumpsys package $appName | grep -E 'Compressed apks info'"

    ).start()

    val model = process.inputStream.bufferedReader().readText().trim()
    return model
}
fun getAppInstallTime(deviceId: String, appName: String): String {
    if (deviceId.isEmpty() || appName.isEmpty()) return "Not available"

    return try {
        val output = runAdbWithTimeout(
            AppConfig.adbPath, "-s", deviceId, "shell",
            "dumpsys package $appName | grep -E 'User [0-9]+:|firstInstallTime'",
            timeoutSeconds = 5
        )

        val timeRegex = Regex("""firstInstallTime=(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})""")
        val times = mutableListOf<String>()
        var currentUserInstalled: Boolean? = null

        for (line in output.lines()) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("User ") && trimmed.contains("installed=") ->
                    currentUserInstalled = trimmed.contains("installed=true")

                trimmed.startsWith("firstInstallTime=") -> {
                    val time = timeRegex.find(trimmed)?.groupValues?.get(1) ?: continue
                    // Older Android has no per-user header, so accept it there
                    val accept = currentUserInstalled ?: true
                    if (accept && !time.startsWith("1970-01-01")) times += time
                }
            }
        }
        times.distinct().minOrNull() ?: "Not available"
    } catch (e: Exception) {
        println("Error reading install time: ${e.message}")
        "Not available"
    }
}

fun getAppGUID(deviceId: String, appName: String): String {
    val process = ProcessBuilder(
        AppConfig.adbPath,
        "-s", deviceId,
        "shell",
        "run-as $appName cat /data/data/$appName/shared_prefs/${appName}_preferences.xml"
    ).start()

    val output = process.inputStream.bufferedReader().readText()

    val guidRegex = """<string name="device_guid">(.*?)</string>""".toRegex()
    val match = guidRegex.find(output)

    return match?.groupValues?.get(1)?.trim() ?: ""
}