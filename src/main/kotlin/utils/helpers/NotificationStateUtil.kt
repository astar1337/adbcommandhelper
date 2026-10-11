package utils.helpers

enum class NotificationState { ENABLED, DISABLED }

fun getNotificationState(deviceId: String, packageName: String, sdk: Int = 0): NotificationState {
    return try {
        val appops = runAdbWithTimeout(
            AppConfig.adbPath, "-s", deviceId, "shell",
            "cmd appops get $packageName POST_NOTIFICATION",
            timeoutSeconds = 5
        ).trim().lowercase()

        // An explicit user/ADB block always wins
        if (appops.contains("ignore") || appops.contains("deny")) {
            return NotificationState.DISABLED
        }

        if (sdk >= 33) {
            // Android 13+: the runtime permission decides
            val perm = runAdbWithTimeout(
                AppConfig.adbPath, "-s", deviceId, "shell",
                "dumpsys package $packageName | grep POST_NOTIFICATIONS",
                timeoutSeconds = 5
            ).lowercase()

            if (perm.contains("granted=true")) NotificationState.ENABLED else NotificationState.DISABLED
        } else {
            // Android 12 and below: allowed unless blocked via appops
            when {
                appops.contains("allow") || appops.contains("default") -> NotificationState.ENABLED
                else -> NotificationState.DISABLED
            }
        }
    } catch (e: Exception) {
        println("Error checking notification state: ${e.message}")
        NotificationState.DISABLED
    }
}