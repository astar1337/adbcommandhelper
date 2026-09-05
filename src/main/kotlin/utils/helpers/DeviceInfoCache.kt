package utils.helpers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object DeviceInfoCache {

    data class DeviceInfo(
        val name: String = "",
        val model: String = "",
        val sdk: Int = 0,
        val sdkRelease: String = "",
        val timestamp: Long = 0L
    )

    private val cache = ConcurrentHashMap<String, DeviceInfo>()
    private const val CACHE_TTL_MS = 60_000L

    private fun getCached(deviceId: String): DeviceInfo? {
        val info = cache[deviceId] ?: return null
        if (System.currentTimeMillis() - info.timestamp > CACHE_TTL_MS) {
            cache.remove(deviceId)
            return null
        }
        return info
    }

    suspend fun fetchAndCache(deviceId: String): DeviceInfo = withContext(Dispatchers.IO) {
        val cached = getCached(deviceId)
        if (cached != null) return@withContext cached

        val info = batchFetchDeviceInfo(deviceId)
        cache[deviceId] = info
        info
    }

    fun getNameSync(deviceId: String): String {
        return cache[deviceId]?.name ?: ""
    }

    fun getSdkSync(deviceId: String): Int {
        return cache[deviceId]?.sdk ?: 0
    }


    fun invalidate(deviceId: String) {
        cache.remove(deviceId)
    }

    private fun batchFetchDeviceInfo(deviceId: String): DeviceInfo {
        val output = runAdbWithTimeout(
            AppConfig.adbPath, "-s", deviceId, "shell",
            "echo NAME_START && settings get global device_name && " +
                    "echo MODEL_START && getprop ro.product.model && " +
                    "echo SDK_START && getprop ro.build.version.sdk && " +
                    "echo RELEASE_START && getprop ro.build.version.release",
            timeoutSeconds = 5
        )

        var name = ""
        var model = ""
        var sdk = 0
        var sdkRelease = ""

        val lines = output.lines()
        var section = ""

        for (line in lines) {
            val trimmed = line.trim()
            when (trimmed) {
                "NAME_START" -> { section = "name"; continue }
                "MODEL_START" -> { section = "model"; continue }
                "SDK_START" -> { section = "sdk"; continue }
                "RELEASE_START" -> { section = "release"; continue }
            }
            when (section) {
                "name" -> if (trimmed.isNotEmpty() && !trimmed.contains("null")) name = trimmed
                "model" -> if (trimmed.isNotEmpty()) model = trimmed
                "sdk" -> if (trimmed.isNotEmpty()) sdk = trimmed.toIntOrNull() ?: 0
                "release" -> if (trimmed.isNotEmpty()) sdkRelease = trimmed
            }
        }

        return DeviceInfo(
            name = name,
            model = model,
            sdk = sdk,
            sdkRelease = sdkRelease,
            timestamp = System.currentTimeMillis()
        )
    }
}