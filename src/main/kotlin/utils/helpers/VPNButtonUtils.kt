package utils.helpers

fun adb(vararg cmd: String) {
    ProcessBuilder(listOf(AppConfig.adbPath, *cmd))
        .redirectErrorStream(true)
        .start()
        .waitFor()
}

fun tap(deviceId: String, x: Int, y: Int) {
    adb("shell","input","tap",x.toString(),y.toString())
}
