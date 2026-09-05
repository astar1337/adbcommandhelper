package utils.helpers
import commands.appConfigs
import utils.helpers.AppConfig.adbPath


data class InstalledApp(
    val key: String,
    val displayName: String,
    val hasProd: Boolean,
    val hasStaging: Boolean
)

object AppDetection {
    const val PUREDOME_PACKAGE = "com.vpn.android.pureb2b"

    private fun isPackageInstalled(deviceId: String, packageName: String): Boolean {
        if (deviceId.isEmpty()) return false
        val command =
            "$adbPath -s $deviceId shell pm list packages | grep -x \"package:$packageName\""
        return AdbDevices.runCommandFromMap(command).isSuccess
    }

    fun isPureDomeInstalled(deviceId: String): Boolean =
        isPackageInstalled(deviceId, PUREDOME_PACKAGE)

    fun getInstalledApps(deviceId: String): List<InstalledApp> {
        if (deviceId.isEmpty()) return emptyList()

        val installedApps = mutableListOf<InstalledApp>()

        appConfigs.forEach { (key, config) ->
            val prodCommand = "$adbPath -s $deviceId shell pm list packages | grep -x \"package:${config.prodPackage}\""
            val prodInstalled = AdbDevices.runCommandFromMap(prodCommand).isSuccess

            val stagCommand = "$adbPath -s $deviceId shell pm list packages | grep -x \"package:${config.stagPackage}\""
            val stagInstalled = AdbDevices.runCommandFromMap(stagCommand).isSuccess

            if (prodInstalled || stagInstalled) {
                installedApps.add(
                    InstalledApp(
                        key = key,
                        displayName = config.displayName,
                        hasProd = prodInstalled,
                        hasStaging = stagInstalled
                    )
                )
            }
        }
        println(installedApps)
        return installedApps

    }
}