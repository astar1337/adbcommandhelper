package utils.helpers

import commands.appConfigs
import utils.helpers.AppDetection.getInstalledApps

fun deleteAllInstalledApps(deviceId: String): DeleteAllAppsResult {

    val installedApps = getInstalledApps(deviceId)

    if (installedApps.isEmpty()) {
        return DeleteAllAppsResult(
            totalDeleted = 0,
            results = emptyList(),
            summary = "No apps found to delete"
        )
    }

    val deletionResults = mutableListOf<AppDeletionResult>()
    var successCount = 0

    installedApps.forEach { app ->
        val config = appConfigs[app.key] ?: return@forEach

        if (app.hasProd) {
            val prodDeleteCommand = "adb -s $deviceId uninstall ${config.prodPackage}"
            val prodResult = AdbDevices.runCommandFromMap(prodDeleteCommand)

            deletionResults.add(
                AppDeletionResult(
                    appName = "${app.displayName} PROD",
                    packageName = config.prodPackage,
                    success = prodResult.isSuccess
                )
            )

            if (prodResult.isSuccess) successCount++
        }
        // Delete staging version if installed
        if (app.hasStaging) {
            val stagDeleteCommand = "adb -s $deviceId uninstall ${config.stagPackage}"
            val stagResult = AdbDevices.runCommandFromMap(stagDeleteCommand)

            deletionResults.add(
                AppDeletionResult(
                    appName = "${app.displayName} STAG",
                    packageName = config.stagPackage,
                    success = stagResult.isSuccess
                )
            )

            if (stagResult.isSuccess) successCount++
        }
    }

    val totalAttempts = deletionResults.size

    val summary = if (successCount == totalAttempts) {
        val deletedApps = deletionResults
            .filter { it.success }
            .joinToString(", ") { it.appName }
        "Successfully deleted: $deletedApps"
    } else if (successCount > 0) {
        val deletedApps = deletionResults
            .filter { it.success }
            .joinToString(", ") { it.appName }
        val failedApps = deletionResults
            .filter { !it.success }
            .joinToString(", ") { it.appName }
        "Deleted: $deletedApps\nFailed: $failedApps"
    } else {
        "Failed to delete all apps"
    }

    return DeleteAllAppsResult(
        totalDeleted = successCount,
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