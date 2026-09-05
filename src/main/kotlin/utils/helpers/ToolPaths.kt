package utils.helpers

import java.io.File

fun findAdbPath(): String {
    val possiblePaths = listOf(
        "/usr/local/bin/adb",
        "/opt/homebrew/bin/adb",
        "${System.getProperty("user.home")}/Library/Android/sdk/platform-tools/adb",
        "${System.getenv("ANDROID_HOME") ?: ""}/platform-tools/adb",
        "${System.getenv("ANDROID_SDK_ROOT") ?: ""}/platform-tools/adb"
    )

    for (path in possiblePaths) {
        if (File(path).exists()) {
            println("Found adb at: $path")
            return path
        }
    }

    try {
        val process = ProcessBuilder("/common/bin/sh", "-c", "which adb")
            .redirectErrorStream(true)
            .start()
        val result = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        if (result.isNotEmpty() && File(result).exists()) {
            println("Found adb via which: $result")
            return result
        }
    } catch (_: Exception) { }

    return "adb"
}
private val EXTRA_PATHS = listOf(
    "/opt/homebrew/bin",
    "/usr/local/bin",
    "/usr/bin",
    "/common/bin",
    "${System.getProperty("user.home")}/.maestro/bin",
    "${System.getProperty("user.home")}/Library/Android/sdk/platform-tools",
    "${System.getenv("ANDROID_HOME") ?: ""}/platform-tools",
    "${System.getenv("ANDROID_SDK_ROOT") ?: ""}/platform-tools"
).filter { it.isNotBlank() }

fun augmentedPath(): String =
    (EXTRA_PATHS + (System.getenv("PATH") ?: "")).joinToString(":")

private fun whichWithPath(binary: String): String? = try {
    val process = ProcessBuilder("/common/bin/sh", "-lc", "command -v $binary")
        .apply { environment()["PATH"] = augmentedPath() }
        .redirectErrorStream(true)
        .start()
    val result = process.inputStream.bufferedReader().readText().trim()
    process.waitFor()
    result.takeIf { it.isNotEmpty() && File(it).exists() }
} catch (_: Exception) {
    null
}

fun findScrcpyPath(): String {
    val possiblePaths = listOf(
        "/usr/local/bin/scrcpy",
        "/opt/homebrew/bin/scrcpy"
    )

    for (path in possiblePaths) {
        if (File(path).exists()) return path
    }

    try {
        val process = ProcessBuilder("/common/bin/sh", "-c", "which scrcpy")
            .redirectErrorStream(true)
            .start()
        val result = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        if (result.isNotEmpty() && File(result).exists()) return result
    } catch (_: Exception) { }

    return "scrcpy"
}

fun findGhPath(): String {
    val possiblePaths = listOf(
        "/usr/local/bin/gh",
        "/opt/homebrew/bin/gh"
    )

    for (path in possiblePaths) {
        if (File(path).exists()) return path
    }

    try {
        val process = ProcessBuilder("/common/bin/sh", "-c", "which gh")
            .redirectErrorStream(true)
            .start()
        val result = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        if (result.isNotEmpty() && File(result).exists()) return result
    } catch (_: Exception) { }

    return "gh"
}
fun findMaestroPath(): String {
    val possiblePaths = listOf(
        "${System.getProperty("user.home")}/.maestro/bin/maestro",
        "/opt/homebrew/bin/maestro",
        "/usr/local/bin/maestro"
    )

    for (path in possiblePaths) {
        if (File(path).exists()) {
            println("Found maestro at: $path")
            return path
        }
    }

    whichWithPath("maestro")?.let {
        println("Found maestro via command -v: $it")
        return it
    }

    return "maestro"
}

object AppConfig {
    var adbPath: String = "adb"
    var scrcpyPath: String = "scrcpy"
    var ghPath: String = "gh"
    var maestroPath: String = "maestro"

    fun initialize() {
        adbPath = findAdbPath()
        scrcpyPath = findScrcpyPath()
        ghPath = findGhPath()
        maestroPath = findMaestroPath()
        println("Resolved paths: adb=$adbPath, scrcpy=$scrcpyPath, gh=$ghPath, maestro=$maestroPath")
    }
}