package utils.helpers

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object CancellableProcess {

    private val active = ConcurrentHashMap.newKeySet<Process>()
    private val cancelled = AtomicBoolean(false)

    fun resetCancelFlag() {
        cancelled.set(false)
    }

    fun wasCancelled(): Boolean = cancelled.get()

    fun track(process: Process): Process {
        active.add(process)
        return process
    }

    fun unTrack(process: Process) {
        active.remove(process)
    }

    fun cancelAll() {
        cancelled.set(true)
        active.forEach { process ->
            runCatching {
                process.toHandle().descendants().forEach { child ->
                    child.destroyForcibly()
                }
                process.destroyForcibly()
            }
        }
        active.clear()
    }
}