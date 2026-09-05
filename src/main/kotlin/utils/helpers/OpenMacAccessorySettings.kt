package utils.helpers

fun macOsMajorVersion(): Int? {
    return try {
        val process = ProcessBuilder("sw_vers", "-productVersion")
            .start()

        val version = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()

        version.substringBefore(".").toInt()
    } catch (e: Exception) {
        null
    }
}


fun openAccessorySettings() {
    if (!System.getProperty("os.name").contains("Mac")) return

    val major = macOsMajorVersion() ?: return

    val url = when {
        major >= 13 ->
            "x-apple.systempreferences:com.apple.settings.PrivacySecurity.extension?Privacy_Accessories"
        major == 12 ->
            "x-apple.systempreferences:com.apple.preference.security?Privacy"
        else -> return
    }

    ProcessBuilder("open", url).start()
}
