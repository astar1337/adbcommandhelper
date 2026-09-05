package utils.helpers

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object WorkflowMonitor {

    private val _trackedRuns = mutableListOf<TrackedRun>()
    private val _runsFlow = MutableStateFlow<List<TrackedRun>>(emptyList())
    val runsFlow: StateFlow<List<TrackedRun>> = _runsFlow

    private var monitorJob: Job? = null
    private var onStatusChanged: ((TrackedRun) -> Unit)? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun getTrackedRuns(): List<TrackedRun> = _trackedRuns.toList()

    private fun hasActiveRuns(): Boolean = _trackedRuns.any {
        it.status == WorkflowRunStatus.QUEUED || it.status == WorkflowRunStatus.IN_PROGRESS
    }

    fun trackRun(
        runId: Long,
        branch: String,
        brand: String,
        store: String,
        appSealing: String = "true"
    ) {
        val run = TrackedRun(
            runId = runId,
            branch = branch,
            brand = brand,
            store = store,
            appSealing = appSealing,
            status = WorkflowRunStatus.IN_PROGRESS,
            triggeredAt = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        )
        _trackedRuns.add(0, run)
        _runsFlow.value = _trackedRuns.toList()
        println("DEBUG WorkflowMonitor: Tracking run $runId on branch $branch")
        startMonitoring()
    }

    fun updateRun(updatedRun: TrackedRun) {
        val index = _trackedRuns.indexOfFirst { it.runId == updatedRun.runId }
        if (index >= 0) {
            _trackedRuns[index] = updatedRun
            _runsFlow.value = _trackedRuns.toList()
            println("DEBUG WorkflowMonitor: Updated run ${updatedRun.runId} to ${updatedRun.status}")
        }
    }

    fun setOnStatusChanged(callback: (TrackedRun) -> Unit) {
        onStatusChanged = callback
    }

    private fun startMonitoring() {
        if (monitorJob?.isActive == true) {
            println("DEBUG WorkflowMonitor: Monitor already running")
            return
        }

        println("DEBUG WorkflowMonitor: Starting monitor")
        monitorJob = scope.launch {
            while (hasActiveRuns()) {
                delay(60_000)
                println("DEBUG WorkflowMonitor: Polling ${_trackedRuns.count { it.status == WorkflowRunStatus.QUEUED || it.status == WorkflowRunStatus.IN_PROGRESS }} active runs")

                val token = GitHubClient.getAuthToken()
                if (token == null) {
                    println("DEBUG WorkflowMonitor: No auth token, skipping")
                    continue
                }

                _trackedRuns.forEachIndexed { index, run ->
                    if (run.status == WorkflowRunStatus.QUEUED || run.status == WorkflowRunStatus.IN_PROGRESS) {
                        println("DEBUG WorkflowMonitor: Checking run ${run.runId}")
                        val updatedRun = GitHubClient.getRunStatus(token, run.runId)
                        if (updatedRun != null) {
                            val newStatus = WorkflowRunStatus.from(updatedRun.status, updatedRun.conclusion)
                            println("DEBUG WorkflowMonitor: Run ${run.runId} status=$newStatus (raw: status=${updatedRun.status} conclusion=${updatedRun.conclusion})")

                            if (newStatus != run.status) {
                                var updated = run.copy(status = newStatus)

                                if (newStatus == WorkflowRunStatus.COMPLETED) {
                                    println("DEBUG WorkflowMonitor: Fetching artifacts for run ${run.runId} ${run.branch} ${run.brand}")
                                    val artifacts = GitHubClient.getArtifacts(token, run.runId)
                                    println("DEBUG WorkflowMonitor: Found ${artifacts.size} artifacts")
                                    updated = updated.copy(artifacts = artifacts)
                                }

                                _trackedRuns[index] = updated
                                _runsFlow.value = _trackedRuns.toList()
                                onStatusChanged?.invoke(updated)
                            }
                        } else {
                            println("DEBUG WorkflowMonitor: Failed to get status for run ${run.runId}")
                        }
                    }
                }
            }
            println("DEBUG WorkflowMonitor: No more active runs, stopping monitor")
        }
    }

    fun forceRefresh() {
        scope.launch {
            val token = GitHubClient.getAuthToken() ?: return@launch
            println("DEBUG WorkflowMonitor: Force refresh")

            _trackedRuns.forEachIndexed { index, run ->
                if (run.status == WorkflowRunStatus.QUEUED || run.status == WorkflowRunStatus.IN_PROGRESS) {
                    val updatedRun = GitHubClient.getRunStatus(token, run.runId)
                    if (updatedRun != null) {
                        val newStatus = WorkflowRunStatus.from(updatedRun.status, updatedRun.conclusion)
                        println("DEBUG WorkflowMonitor: Force refresh run ${run.runId} status=$newStatus")

                        var updated = run.copy(status = newStatus)

                        if (newStatus == WorkflowRunStatus.COMPLETED) {
                            val artifacts = GitHubClient.getArtifacts(token, run.runId)
                            updated = updated.copy(artifacts = artifacts)
                        }

                        _trackedRuns[index] = updated
                        _runsFlow.value = _trackedRuns.toList()
                        onStatusChanged?.invoke(updated)
                    }
                }
            }
        }
    }

//    fun clearHistory() { //leaving this in case I want to add a manual refresh button
//        _trackedRuns.clear()
//        _runsFlow.value = emptyList()
//        stopMonitoring()
//    }

    fun restartMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        if (hasActiveRuns()) {
            println("DEBUG WorkflowMonitor: Restarting monitor")
            startMonitoring()
        }
    }
}