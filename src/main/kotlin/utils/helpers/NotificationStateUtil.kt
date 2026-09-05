package utils.helpers

enum class NotificationState { ENABLED, DISABLED }

fun getNotificationState(deviceId: String, packageName: String): NotificationState {
    return try {
        val appopsProcess = ProcessBuilder(
            AppConfig.adbPath, "-s", deviceId, "shell", "cmd", "appops", "get", packageName, "POST_NOTIFICATION"
        ).start()

        val appopsOutput = appopsProcess.inputStream.bufferedReader().readText().trim().lowercase()
        println("ADB Check for $packageName: $appopsOutput")

        when {
            appopsOutput.contains("allow") -> NotificationState.ENABLED
            appopsOutput.contains("ignore") -> NotificationState.DISABLED
            appopsOutput.contains("default") -> NotificationState.ENABLED
            else -> NotificationState.DISABLED
        }
    } catch (e: Exception) {
        println("Error checking notification state: ${e.message}")
        NotificationState.DISABLED
    }
}