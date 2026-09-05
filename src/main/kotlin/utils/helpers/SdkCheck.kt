package utils.helpers

fun getAndroidSdk(deviceId: String): Int {
    val process = ProcessBuilder(
        AppConfig.adbPath, "-s", deviceId, "shell", "getprop", "ro.build.version.sdk"
    ).start()

    val sdk = process.inputStream.bufferedReader().readText().trim()
    return sdk.toIntOrNull() ?: 0
}

fun getAndroidSdkRelease(deviceId: String): String {
    val process = ProcessBuilder(
        AppConfig.adbPath, "-s", deviceId, "shell", "getprop", "ro.build.version.release"
    ).start()
    val sdk = process.inputStream.bufferedReader().readText().trim()
    return sdk
}

