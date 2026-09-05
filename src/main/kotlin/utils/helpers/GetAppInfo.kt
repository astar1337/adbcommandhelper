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
    val process = ProcessBuilder(
        AppConfig.adbPath,
        "-s", deviceId,
        "shell",
        "dumpsys package $appName | grep firstInstallTime | cut -d= -f2"

    ).start()

    val model = process.inputStream.bufferedReader().readText().trim()
    return model
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