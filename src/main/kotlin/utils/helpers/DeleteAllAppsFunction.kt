package utils.helpers

import commands.appConfigs

fun deleteAllInstalledApps(deviceId: String): DeleteAllAppsResult {
    val installed = AppDetection.installedPackages(deviceId)
        ?: return DeleteAllAppsResult(0, emptyList(), "Couldn't read installed packages from the device")

    val targets = appConfigs.values.flatMap { config ->
        config.allPackages.filter { it in installed }.map { pkg -> config to pkg }
    }

    if (targets.isEmpty()) {
        return DeleteAllAppsResult(0, emptyList(), "No apps found to delete")
    }

    val deletionResults = targets.map { (config, pkg) ->
        val env = when (pkg) {
            in config.allStagPackages -> "STAG"
            in config.allBetaPackages -> "BETA"
            else -> "PROD"
        }
        val result = AdbDevices.runCommandFromMap("${AppConfig.adbPath} -s $deviceId uninstall $pkg")
        AppDeletionResult(
            appName = "${config.displayName} $env ($pkg)",
            packageName = pkg,
            success = result.isSuccess && result.rawOutput.contains("Success", ignoreCase = true)
        )
    }

    val succeeded = deletionResults.filter { it.success }
    val failed = deletionResults.filterNot { it.success }

    val summary = when {
        failed.isEmpty() -> "Successfully deleted: ${succeeded.joinToString(", ") { it.appName }}"
        succeeded.isNotEmpty() -> "Deleted: ${succeeded.joinToString(", ") { it.appName }}\nFailed: ${failed.joinToString(", ") { it.appName }}"
        else -> "Failed to delete all apps"
    }

    return DeleteAllAppsResult(
        totalDeleted = succeeded.size,
        results = deletionResults,
        summary = summary
    )
}

data class AppDeletionResult(
    val appName: String,
    val packageName: String,
    val success: Boolean
)

data class DeleteAllAppsResult(
    val totalDeleted: Int,
    val results: List<AppDeletionResult>,
    val summary: String
)