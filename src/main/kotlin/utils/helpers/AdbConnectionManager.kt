package utils.helpers

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

enum class DisconnectMode {
    ADB,       // really disconnected via `adb disconnect` (ip:port devices)
    DETACHED   // still attached to adb, hidden by this app (USB / mDNS)
}

data class DisconnectedDevice(
    val serial: String,
    val label: String,
    val sdkRelease: String,
    val deviceType: DeviceType,
    val mode: DisconnectMode = DisconnectMode.DETACHED
)

enum class ConnectionAction { NONE, DISCONNECTING, RECONNECTING }

object AdbConnectionManager {

    val disconnected = mutableStateListOf<DisconnectedDevice>()

    private val hostPortRegex = Regex("""^[\w.\-]+:\d+$""")

    private fun supportsRealDisconnect(serial: String): Boolean = hostPortRegex.matches(serial)

    private fun isDetached(serial: String): Boolean =
        disconnected.any { it.serial == serial && it.mode == DisconnectMode.DETACHED }

    fun visibleDevices(raw: List<String>): List<String> = raw.filterNot { isDetached(it) }

    fun syncWith(raw: List<String>) {
        disconnected.removeAll { it.mode == DisconnectMode.ADB && it.serial in raw }
    }

    private fun run(vararg args: String): String =
        runAdbWithTimeout(AppConfig.adbPath, *args, timeoutSeconds = 10)

    private fun rawDevices(): List<String> = AdbDevices.getDevicesList()

    suspend fun disconnect(device: DisconnectedDevice): DisconnectMode {
        val mode = withContext(Dispatchers.IO) {
            if (supportsRealDisconnect(device.serial)) {
                run("disconnect", device.serial)
                delay(500)
                if (device.serial !in rawDevices()) DisconnectMode.ADB else DisconnectMode.DETACHED
            } else {
                DisconnectMode.DETACHED   // adb would re-attach it instantly, so just detach
            }
        }
        disconnected.removeAll { it.serial == device.serial }
        disconnected += device.copy(mode = mode)
        return mode
    }

    suspend fun reconnect(device: DisconnectedDevice): Result<Unit> {
        val ok = withContext(Dispatchers.IO) {
            when (device.mode) {
                DisconnectMode.DETACHED -> device.serial in rawDevices()
                DisconnectMode.ADB -> {
                    run("connect", device.serial)
                    waitForDevice(device.serial)
                }
            }
        }
        return if (ok) {
            forget(device.serial)
            Result.success(Unit)
        } else {
            Result.failure(
                IllegalStateException(
                    when (device.mode) {
                        DisconnectMode.DETACHED ->
                            "Adb can't see it right now. Check the cable or Wi-Fi debugging, then try again."
                        DisconnectMode.ADB ->
                            "Couldn't connect to ${device.serial}. Check the device is on the same network."
                    }
                )
            )
        }
    }

    private suspend fun waitForDevice(serial: String, attempts: Int = 5): Boolean {
        repeat(attempts) {
            delay(600)
            if (serial in rawDevices()) return true
        }
        return false
    }

    private fun forget(serial: String) {
        disconnected.removeAll { it.serial == serial }
    }
}