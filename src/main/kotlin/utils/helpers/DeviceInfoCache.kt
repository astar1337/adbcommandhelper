package utils.helpers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

enum class DeviceType { MOBILE, TV }

object DeviceInfoCache {

    data class DeviceInfo(
        val name: String = "",
        val model: String = "",
        val sdk: Int = 0,
        val sdkRelease: String = "",
        val deviceType: DeviceType = DeviceType.MOBILE,
        val timestamp: Long = 0L
    ) {
        val isValid: Boolean get() = sdk > 0
    }

    private val cache = ConcurrentHashMap<String, DeviceInfo>()
    private const val CACHE_TTL_MS = 60_000L

    private val TV_FEATURES = setOf(
        "android.software.leanback",
        "android.hardware.type.television",
        "amazon.hardware.fire_tv"
    )

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
        // Don't cache failed fetches (unauthorized/offline device), so the next call retries
        if (info.isValid) cache[deviceId] = info
        info
    }

    fun getNameSync(deviceId: String): String = cache[deviceId]?.name ?: ""
    fun getModelSync(deviceId: String): String = cache[deviceId]?.model ?: ""
    fun getSdkSync(deviceId: String): Int = cache[deviceId]?.sdk ?: 0
    fun getSdkReleaseSync(deviceId: String): String = cache[deviceId]?.sdkRelease ?: ""
    fun getDeviceTypeSync(deviceId: String): DeviceType = cache[deviceId]?.deviceType ?: DeviceType.MOBILE
    fun isTvSync(deviceId: String): Boolean = getDeviceTypeSync(deviceId) == DeviceType.TV

    fun invalidate(deviceId: String) {
        cache.remove(deviceId)
    }

    private fun batchFetchDeviceInfo(deviceId: String): DeviceInfo {
        // ';' instead of '&&': if one command fails, the remaining sections still run
        val output = runAdbWithTimeout(
            AppConfig.adbPath, "-s", deviceId, "shell",
            "echo NAME_START; settings get global device_name; " +
                    "echo MODEL_START; getprop ro.product.model; " +
                    "echo SDK_START; getprop ro.build.version.sdk; " +
                    "echo RELEASE_START; getprop ro.build.version.release; " +
                    "echo CHAR_START; getprop ro.build.characteristics; " +
                    "echo FEATURES_START; pm list features",
            timeoutSeconds = 5
        )

        var name = ""
        var model = ""
        var sdk = 0
        var sdkRelease = ""
        var characteristics = ""
        val features = mutableSetOf<String>()

        var section = ""
        for (line in output.lines()) {
            val trimmed = line.trim()
            when (trimmed) {
                "NAME_START" -> { section = "name"; continue }
                "MODEL_START" -> { section = "model"; continue }
                "SDK_START" -> { section = "sdk"; continue }
                "RELEASE_START" -> { section = "release"; continue }
                "CHAR_START" -> { section = "characteristics"; continue }
                "FEATURES_START" -> { section = "features"; continue }
            }
            if (trimmed.isEmpty()) continue

            when (section) {
                "name" -> if (!trimmed.contains("null")) name = trimmed
                "model" -> model = trimmed
                "sdk" -> sdk = trimmed.toIntOrNull() ?: 0
                "release" -> sdkRelease = trimmed
                "characteristics" -> characteristics = trimmed
                "features" -> if (trimmed.startsWith("feature:")) {
                    // e.g. "feature:android.software.leanback" or "feature:android.hardware.vulkan.level=1"
                    features += trimmed.removePrefix("feature:").substringBefore("=")
                }
            }
        }

        val isTv = features.any { it in TV_FEATURES } ||
                characteristics.split(",").any { it.trim().equals("tv", ignoreCase = true) }

        return DeviceInfo(
            name = name.ifEmpty { model },   // Fire TV / some TVs have no device_name
            model = model,
            sdk = sdk,
            sdkRelease = sdkRelease,
            deviceType = if (isTv) DeviceType.TV else DeviceType.MOBILE,
            timestamp = System.currentTimeMillis()
        )
    }
}