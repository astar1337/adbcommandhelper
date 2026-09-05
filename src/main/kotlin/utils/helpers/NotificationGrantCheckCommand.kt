package utils.helpers

import utils.helpers.AppConfig.adbPath

fun notificationCommand(
    deviceId: String,
    appId: String,
    envName: String,
    sdk: Int,
    appKey: String,
    state: NotificationState
): Command {
    val adb = adbPath
    val action = if (state == NotificationState.ENABLED) "Revoke" else "Grant"

    val command = if (sdk >= 33) {
        if (state == NotificationState.ENABLED) {
            "$adb -s $deviceId shell pm revoke $appId android.permission.POST_NOTIFICATIONS"
        } else {
            "$adb -s $deviceId shell pm grant $appId android.permission.POST_NOTIFICATIONS"
        }
    } else {
        val opState = if (state == NotificationState.ENABLED) "deny" else "allow"
        "$adb -s $deviceId shell cmd appops set $appId POST_NOTIFICATION $opState"
    }

    return Command(
        id = "notifications",
        labelParts = listOf(
            CommandLabel.Text("$action $envName "),
            CommandLabel.AppIcon(appKey),
            CommandLabel.Text(" Notification Permissions")
        ),
        adbCommand = command
    )
}
