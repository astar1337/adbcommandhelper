package ui.components

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import utils.helpers.MaestroInstallation

class MaestroGateState(private val scope: CoroutineScope) {

    var isChecking by mutableStateOf(false)
        private set

    private var showInstallDialog by mutableStateOf(false)

    private var pendingAction: (() -> Unit)? = null

    fun ensureInstalled(onMissing: (() -> Unit)? = null, action: () -> Unit) {
        if (MaestroInstallation.quickCheck() != null) {
            action()
            return
        }

        if (isChecking) return
        isChecking = true
        scope.launch {
            val installed = try {
                MaestroInstallation.isInstalled()
            } catch (t: Throwable) {
                println("[MaestroGate] check threw, failing open: ${t.message}")
                true
            } finally {
                isChecking = false
            }

            if (installed) {
                action()
            } else {
                pendingAction = action
                showInstallDialog = true
                onMissing?.invoke()
            }
        }
    }
}

@Composable
fun rememberMaestroGate(): MaestroGateState {
    val scope = rememberCoroutineScope()
    return remember { MaestroGateState(scope) }
}