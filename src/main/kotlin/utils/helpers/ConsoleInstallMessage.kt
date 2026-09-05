package utils.helpers

sealed class ConsoleInstallMessage {
    data class Text(val content: String) : ConsoleInstallMessage()

    data class InstallAction(
        val preText: String,
        val filePath: String,
        val buttonText: String = "Install"
    ) : ConsoleInstallMessage()
}