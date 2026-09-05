package utils.helpers

data class FormatOutput(
    val device: String,
    val selectedApp: String,
    val environment: String,
    val commandLabel: String,
    val commandExecuted: String,
    val result: String,
    val timestamp: String = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
    val appNumber: String
) {
    fun toConsoleMessage(): ConsoleInstallMessage.Text {
        val isSuccess = result.contains("successfully", ignoreCase = true) || result.contains("Success", ignoreCase = true)
        val status = if (isSuccess) "successfully" else "FAIL"

        val versionName = appNumber.lines()
            .lastOrNull { it.isNotBlank() && !it.contains("minSdk", ignoreCase = true) }
            ?.trim() ?: ""

        val versionCode = appNumber.lines()
            .firstOrNull { it.contains("minSdk", ignoreCase = true) }
            ?.split(" ")
            ?.firstOrNull { it.all { c -> c.isDigit() } }
            ?.trim() ?: ""

        val displayVersion = if (versionName.isNotEmpty() && versionCode.isNotEmpty()) {
            "$versionName ($versionCode)"
        } else {
            versionName.ifEmpty { versionCode }
        }

        val actionMessage = when {
            commandLabel.contains("Launch", ignoreCase = true) ->
                "Launched $selectedApp ($environment) ($displayVersion) $status at $timestamp"
            commandLabel.contains("Force Stop", ignoreCase = true) ->
                "Force stopped $selectedApp ($environment) $status at $timestamp"
            commandLabel.contains("Clear", ignoreCase = true) ->
                "Cleared data on $selectedApp ($environment) $status at $timestamp"
            commandLabel.contains("Delete", ignoreCase = true) && commandLabel.contains("without", ignoreCase = true) ->
                "Deleted $selectedApp ($environment) without files $status at $timestamp"
            commandLabel.contains("Delete", ignoreCase = true) ->
                "Deleted $selectedApp ($environment) $status at $timestamp"
            commandLabel.contains("Grant", ignoreCase = true) ->
                "Granted notification permission on $selectedApp ($environment) $status at $timestamp"
            commandLabel.contains("Revoke", ignoreCase = true) ->
                "Revoked notification permission on $selectedApp ($environment) $status at $timestamp"
            else ->
                "$commandLabel — $selectedApp($environment) $status at $timestamp"
        }

        val truncatedCommand = if (commandExecuted.length > 100) {
            commandExecuted.take(80) + "..."
        } else {
            commandExecuted
        }

        val formattedString = buildString {
            appendLine(actionMessage)
            appendLine("ADB Command: $truncatedCommand")
        }
        return ConsoleInstallMessage.Text(formattedString)
    }
}