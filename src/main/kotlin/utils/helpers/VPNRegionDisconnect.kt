package utils.helpers

fun disconnectVPN(deviceId: String) {
    adb("-s", deviceId, "shell", "am", "start",
        "-n", "com.vpn.android.pureb2b/com.vpn.SplashActivity")
    Thread.sleep(2000)
    tap(deviceId, 540, 928)
    Thread.sleep(2000)
}