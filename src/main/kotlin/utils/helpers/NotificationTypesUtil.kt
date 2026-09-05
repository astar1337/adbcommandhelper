package utils.helpers


enum class NotificationType {
    SUCCESS,
    ERROR,
    WARNING
}

data class Notification(
    val message: String,
    val type: NotificationType
)
