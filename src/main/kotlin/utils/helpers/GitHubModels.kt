package utils.helpers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class GitHubUser(
    val login: String,
    val name: String?
)

data class GitHubBranch(
    val name: String
)

data class GitHubWorkflowRun(
    val id: Long,
    val status: String,
    val conclusion: String?,
    val branch: String,
    val createdAt: String,
    val inputs: Map<String, String> = emptyMap()
)

@Serializable
data class GitHubArtifact(
    val id: Long,
    val name: String,
    val sizeInBytes: Long,
    val expired: Boolean
)

enum class WorkflowRunStatus {
    QUEUED,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED,
    UNKNOWN;

    companion object {
        fun from(status: String, conclusion: String? = null): WorkflowRunStatus {
            return when (status) {
                "queued" -> QUEUED
                "in_progress" -> IN_PROGRESS
                "completed" -> when (conclusion) {
                    "success" -> COMPLETED
                    "failure" -> FAILED
                    "cancelled" -> CANCELLED
                    else -> UNKNOWN
                }

                else -> UNKNOWN
            }
        }
    }
}

data class TrackedRun(
    val runId: Long,
    val branch: String,
    val brand: String,
    val store: String,
    val status: WorkflowRunStatus,
    val triggeredAt: String,
    val artifacts: List<GitHubArtifact> = emptyList(),
    val appSealing: String = "true",
)

@Serializable
data class BranchesResponse(
    @SerialName("workflow_runs") val runs: List<BranchRunHistoryItem> = emptyList(),
)

@Serializable
data class BranchRunHistoryItem(
    @SerialName("id") val runId: Long,
    @SerialName("display_title") val displayTitle: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("status") val status: String,
    @SerialName("html_url") val htmlUrl: String,
   // @SerialName("artifacts") val artifacts: List<GitHubArtifact>,
    @SerialName("head_branch") val headBranch: String = "",
    @SerialName("actor") val actor: Actor = Actor(""),
    @SerialName("run_number") val runNumber: Int = 0,
    @SerialName("conclusion") val conclusion: String? = "",

) {
    fun getStatus(): WorkflowRunStatus = WorkflowRunStatus.from(status = status, conclusion = conclusion)
}

@Serializable
data class Actor(
    @SerialName("login") val login: String,
)

