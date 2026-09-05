package ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import utils.helpers.AppDetection

class PureDomeGateState(private val scope: CoroutineScope) {

    var isChecking by mutableStateOf(false)
        private set

    private var lastCheckedDeviceId by mutableStateOf("")

    fun ensureInstalled(
        deviceId: String,
        onMissing: () -> Unit,
        onReady: () -> Unit
    ) {
        if (isChecking) return

        scope.launch {
            isChecking = true
            lastCheckedDeviceId = deviceId

            val installed = try {
                withContext(Dispatchers.IO) {
                    AppDetection.isPureDomeInstalled(deviceId)
                }
            } catch (e: Exception) {
                false
            } finally {
                isChecking = false
            }

            if (installed) onReady() else onMissing()
        }
    }
}

@Composable
fun rememberPureDomeGate(): PureDomeGateState {
    val scope = rememberCoroutineScope()
    return remember(scope) { PureDomeGateState(scope) }
}