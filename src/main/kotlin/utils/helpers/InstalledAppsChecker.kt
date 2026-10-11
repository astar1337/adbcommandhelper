package utils.helpers

import commands.appConfigs
import java.util.concurrent.ConcurrentHashMap

data class InstalledApp(
    val key: String,
    val displayName: String,
    val prodPackage: String? = null,
    val stagPackage: String? = null,
    val betaPackage: String? = null
)

object AppDetection {
    const val PUREDOME_PACKAGE = "com.vpn.android.pureb2b"

    private val lastKnown = ConcurrentHashMap<String, List<InstalledApp>>()

    private fun listPackages(deviceId: String): Set<String>? {
        if (deviceId.isEmpty()) return null
        val output = runAdbWithTimeout(
            AppConfig.adbPath, "-s", deviceId, "shell", "pm list packages",
            timeoutSeconds = 2
        )
        val packages = output.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("package:") }
            .map { it.removePrefix("package:") }
            .toSet()
        // A working device always has system packages, so empty means the call failed
        return packages.ifEmpty { null }
    }

    private fun isPackageInstalled(deviceId: String, packageName: String): Boolean =
        listPackages(deviceId)?.contains(packageName) == true

    fun isPureDomeInstalled(deviceId: String): Boolean =
        isPackageInstalled(deviceId, PUREDOME_PACKAGE)

    fun installedPackages(deviceId: String): Set<String>? = listPackages(deviceId)

    fun getInstalledApps(deviceId: String, deviceType: DeviceType? = null): List<InstalledApp> {
        if (deviceId.isEmpty()) return emptyList()

        val installed = listPackages(deviceId)
            ?: return lastKnown[deviceId] ?: emptyList()

        val apps = appConfigs.mapNotNull { (key, config) ->
            val prod = config.prodCandidates(deviceType).firstOrNull { it in installed }
            val stag = config.stagCandidates(deviceType).firstOrNull { it in installed }
            val beta = config.betaCandidates().firstOrNull { it in installed }
            if (prod == null && stag == null && beta == null) null
            else InstalledApp(key, config.displayName, prodPackage = prod, stagPackage = stag, betaPackage = beta)
        }

        lastKnown[deviceId] = apps
        return apps
    }

    fun forget(deviceId: String) {
        lastKnown.remove(deviceId)
    }
}