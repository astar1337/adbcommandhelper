package utils.helpers

import androidx.compose.runtime.Immutable

@Immutable
sealed class CommandLabel {

    data class Text(
        val value: String
    ) : CommandLabel()

    data class AppIcon(
        val appKey: String
    ) : CommandLabel()
}

@Immutable
data class Command(
    val labelParts: List<CommandLabel>,
    val adbCommand: String,
    val id: String = "",

    ){
    val displayLabel: String
        get() = labelParts
            .filterIsInstance<CommandLabel.Text>()
            .joinToString("") { it.value }

    val isNotificationPermissionCommand: Boolean
        get() = labelParts
            .filterIsInstance<CommandLabel.Text>()
            .any {
                it.value.contains("Notification Permissions", ignoreCase = true)
            }
}


