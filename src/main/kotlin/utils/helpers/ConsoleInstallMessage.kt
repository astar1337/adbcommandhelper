package utils.helpers

sealed class ConsoleInstallMessage {
    data class Text(
        val content: String,
        val rawCommand: String? = null   // full, untruncated command for clipboard copy
    ) : ConsoleInstallMessage()

    data class InstallAction(
        val preText: String,
        val filePath: String,
        val buttonText: String = "Install"
    ) : ConsoleInstallMessage()
}