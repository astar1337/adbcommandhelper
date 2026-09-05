package utils.helpers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import org.yaml.snakeyaml.Yaml
import commands.WorkflowInput
import kotlinx.serialization.json.Json
import java.net.URI

object GitHubClient {

    private const val REPO = "getndazn/android-dazn-app"
    private const val WORKFLOW_ID = 88880347L
    private const val API_BASE = "https://api.github.com"

    private var cachedToken: String? = null
    @Volatile
    var isDownloadCancelled: Boolean = false
        private set

    fun cancelDownload() {
        isDownloadCancelled = true
    }

    fun resetDownloadCancel() {
        isDownloadCancelled = false
    }
    fun clearTokenCache() {
        cachedToken = null
    }


    fun isGhCliInstalled(): Boolean {
        return try {
            val process = ProcessBuilder(AppConfig.ghPath, "--version")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.contains("gh version")
        } catch (e: Exception) {
            false
        }
    }
    suspend fun validateToken(token: String): GitHubUser? = withContext(Dispatchers.IO) {
        try {
            val response = apiGet("/user", token)
            if (response != null) {
                val json = JSONObject(response)
                GitHubUser(
                    login = json.getString("login"),
                    name = json.optString("name", null)
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun fetchBranchRunHistory(token: String, branch: String): List<BranchRunHistoryItem> {
        return try {
            val response = apiGet(
                "/repos/$REPO/actions/workflows/$WORKFLOW_ID/runs?branch=$branch&per_page=10",
                token
            )
            if (response != null) {
                val jsonParser = Json { ignoreUnknownKeys = true }
                jsonParser.decodeFromString<BranchesResponse>(response).runs
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            println("DEBUG fetchBranchRunHistory: ${e.message}")
            emptyList()
        }
    }

    fun getAuthToken(): String? {
        if (cachedToken != null) return cachedToken
        return try {
            val process = ProcessBuilder(AppConfig.ghPath, "auth", "token")
                .redirectErrorStream(true)
                .start()
            val token = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            if (token.isNotEmpty() && !token.contains("error") && !token.contains("not logged")) {
                cachedToken = token
                token
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    private val workflowInputsCache = mutableMapOf<String, List<WorkflowInput>>()

    suspend fun fetchWorkflowInputs(token: String, branch: String): List<WorkflowInput> = withContext(Dispatchers.IO) {
        workflowInputsCache[branch]?.let { return@withContext it }

        try {
            val url = URI("$API_BASE/repos/$REPO/contents/.github/workflows/apk-builder-mobile.yml?ref=$branch").toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.raw+json")
            connection.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                connection.disconnect()
                println("DEBUG fetchWorkflowInputs: Failed with code $responseCode for branch $branch")
                return@withContext emptyList()
            }
            val yamlContent = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val yaml = Yaml()
            val parsed = yaml.load<Map<Any, Any>>(yamlContent)

            println("DEBUG fetchWorkflowInputs: All keys: ${parsed.keys.map { "${it::class.simpleName}=$it" }}")

            val onValue = parsed.entries.firstOrNull { entry ->
                entry.key == true || entry.key.toString() == "on" || entry.key.toString() == "true"
            }?.value as? Map<*, *>

            if (onValue == null) {
                println("DEBUG fetchWorkflowInputs: Could not find 'on' key")
                return@withContext emptyList()
            }

            println("DEBUG fetchWorkflowInputs: onValue keys = ${onValue.keys}")

            val workflowDispatch = onValue["workflow_dispatch"] as? Map<*, *>
            if (workflowDispatch == null) {
                println("DEBUG fetchWorkflowInputs: No workflow_dispatch found")
                return@withContext emptyList()
            }

            val inputs = workflowDispatch["inputs"] as? Map<*, *>
            if (inputs == null) {
                println("DEBUG fetchWorkflowInputs: No inputs found")
                return@withContext emptyList()
            }

            println("DEBUG fetchWorkflowInputs: Found inputs: ${inputs.keys}")

            val result = inputs.map { (key, value) ->
                val inputMap = value as? Map<*, *> ?: return@map null

                val options = when (val opts = inputMap["options"]) {
                    is List<*> -> opts.map { it.toString() }
                    else -> emptyList()
                }

                WorkflowInput(
                    id = key.toString(),
                    description = ((inputMap["description"] as? String) ?: key).toString(),
                    required = (inputMap["required"] as? Boolean) ?: false,
                    type = (inputMap["type"] as? String) ?: "string",
                    default = (inputMap["default"] as? String) ?: "",
                    options = options
                )
            }.filterNotNull()

            workflowInputsCache[branch] = result
            println("DEBUG fetchWorkflowInputs: Found ${result.size} inputs for branch $branch: ${result.map { it.id }}")
            result
        } catch (e: Exception) {
            println("DEBUG fetchWorkflowInputs: Exception for branch $branch: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchBranches(token: String): List<GitHubBranch> = withContext(Dispatchers.IO) {
        val allBranches = mutableListOf<GitHubBranch>()
        var page = 1
        var hasMore = true

        while (hasMore) {
            try {
                val response = apiGet("/repos/$REPO/branches?per_page=100&page=$page", token)
                if (response != null) {
                    val jsonArray = JSONArray(response)
                    if (jsonArray.length() == 0) {
                        hasMore = false
                    } else {
                        for (i in 0 until jsonArray.length()) {
                            val branch = jsonArray.getJSONObject(i)
                            allBranches.add(GitHubBranch(name = branch.getString("name")))
                        }
                        if (jsonArray.length() < 100) {
                            hasMore = false
                        }
                        page++
                    }
                } else {
                    hasMore = false
                }
            } catch (e: Exception) {
                hasMore = false
            }
        }

        allBranches
    }

    suspend fun triggerWorkflow(
        token: String,
        branch: String,
        inputs: Map<String, String>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputsJson = JSONObject(inputs)
            val body = JSONObject().apply {
                put("ref", branch)
                put("inputs", inputsJson)
            }

            val url = URI("$API_BASE/repos/$REPO/actions/workflows/$WORKFLOW_ID/dispatches").toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            connection.outputStream.use { os ->
                os.write(body.toString().toByteArray())
            }

            val responseCode = connection.responseCode
            connection.disconnect()

            responseCode == 204
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getLatestRun(token: String, branch: String): GitHubWorkflowRun? = withContext(Dispatchers.IO) {
        val beforeTrigger = System.currentTimeMillis()

        repeat(10) {
            try {
                kotlinx.coroutines.delay(3000)

                val response = apiGet(
                    "/repos/$REPO/actions/workflows/$WORKFLOW_ID/runs?per_page=5&branch=$branch",
                    token
                )
                if (response != null) {
                    val json = JSONObject(response)
                    val runs = json.getJSONArray("workflow_runs")

                    for (i in 0 until runs.length()) {
                        val run = runs.getJSONObject(i)
                        val status = run.getString("status")
                        val createdAt = run.getString("created_at")

                        val createdTime = java.time.Instant.parse(createdAt).toEpochMilli()

                        if (createdTime > beforeTrigger - 30000 &&
                            (status == "queued" || status == "in_progress")
                        ) {
                            return@withContext GitHubWorkflowRun(
                                id = run.getLong("id"),
                                status = status,
                                conclusion = if (run.isNull("conclusion")) null else run.getString("conclusion"),
                                branch = run.getString("head_branch"),
                                createdAt = createdAt
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // retry
            }
        }
        null
    }

    suspend fun getRunStatus(token: String, runId: Long): GitHubWorkflowRun? = withContext(Dispatchers.IO) {
        try {
            val response = apiGet("/repos/$REPO/actions/runs/$runId", token)
            if (response != null) {
                val run = JSONObject(response)
                GitHubWorkflowRun(
                    id = run.getLong("id"),
                    status = run.getString("status"),
                    conclusion = if (run.isNull("conclusion")) null else run.getString("conclusion"),
                    branch = run.getString("head_branch"),
                    createdAt = run.getString("created_at")
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getArtifacts(token: String, runId: Long): List<GitHubArtifact> = withContext(Dispatchers.IO) {
        try {
            val response = apiGet("/repos/$REPO/actions/runs/$runId/artifacts", token)
            if (response != null) {
                val json = JSONObject(response)
                val artifacts = json.getJSONArray("artifacts")
                val result = mutableListOf<GitHubArtifact>()
                for (i in 0 until artifacts.length()) {
                    val artifact = artifacts.getJSONObject(i)
                    val name = artifact.getString("name")
                    if (name.startsWith("APKS")) {
                        result.add(
                            GitHubArtifact(
                                id = artifact.getLong("id"),
                                name = name,
                                sizeInBytes = artifact.getLong("size_in_bytes"),
                                expired = artifact.getBoolean("expired")
                            )
                        )
                    }
                }
                result
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    suspend fun cancelWorkflowRun(token: String, runId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URI("$API_BASE/repos/$REPO/actions/runs/$runId/cancel").toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.doOutput = true
            connection.outputStream.close()

            val responseCode = connection.responseCode
            connection.disconnect()
            println("DEBUG cancelWorkflowRun: runId=$runId responseCode=$responseCode")
            responseCode == 202
        } catch (e: Exception) {
            println("DEBUG cancelWorkflowRun: exception ${e.message}")
            false
        }
    }

    suspend fun rerunWorkflow(token: String, runId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URI("$API_BASE/repos/$REPO/actions/runs/$runId/rerun").toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.doOutput = true
            connection.outputStream.close()

            val responseCode = connection.responseCode
            connection.disconnect()
            println("DEBUG rerunWorkflow: runId=$runId responseCode=$responseCode")
            responseCode == 201
        } catch (e: Exception) {
            println("DEBUG rerunWorkflow: exception ${e.message}")
            false
        }
    }

    suspend fun downloadArtifact(
        token: String,
        artifactId: Long,
        fileName: String,
        onProgress: (String) -> Unit
    ): String? = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = File(System.getProperty("user.home"), "Downloads")
            val outputFile = File(downloadsDir, "$fileName.zip")

            onProgress("Starting download...")

            val url = URI("$API_BASE/repos/$REPO/actions/artifacts/$artifactId/zip").toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15000
            connection.readTimeout = 30000

            val downloadUrl = if (connection.responseCode == 302) {
                val redirect = connection.getHeaderField("Location")
                connection.disconnect()
                redirect
            } else {
                null
            }

            val downloadConnection = if (downloadUrl != null) {
                val rc = URI(downloadUrl).toURL().openConnection() as HttpURLConnection
                rc.readTimeout = 30000
                rc
            } else {
                connection
            }

            val totalSize = downloadConnection.contentLengthLong

            downloadConnection.inputStream.use { input ->
                outputFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytes = 0L

                    while (true) {
                        if (isDownloadCancelled) {
                            isDownloadCancelled = false
                            downloadConnection.disconnect()
                            outputFile.delete()
                            onProgress("Download cancelled")
                            return@withContext null
                        }

                        bytesRead = try {
                            input.read(buffer)
                        } catch (e: Exception) {
                            onProgress("Connection lost during download")
                            return@withContext null
                        }

                        if (bytesRead == -1) break

                        output.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead

                        val mb = totalBytes / (1024.0 * 1024.0)
                        val progressText = if (totalSize > 0) {
                            val totalMb = totalSize / (1024.0 * 1024.0)
                            val percent = (totalBytes * 100 / totalSize).toInt()
                            "${String.format("%.1f", mb)} / ${String.format("%.1f", totalMb)} MB ($percent%)"
                        } else {
                            "${String.format("%.1f", mb)} MB"
                        }
                        onProgress(progressText)
                    }
                }
            }

            downloadConnection.disconnect()
            onProgress("Download complete")
            outputFile.absolutePath
        } catch (e: java.net.SocketTimeoutException) {
            onProgress("Download timed out")
            null
        } catch (e: java.net.ConnectException) {
            onProgress("Connection lost")
            null
        } catch (e: java.io.IOException) {
            onProgress("Download interrupted: ${e.message}")
            null
        } catch (e: Exception) {
            onProgress("Download failed: ${e.message}")
            null
        }
    }

    private fun apiGet(endpoint: String, token: String): String? {
        return try {
            val url = URI("$API_BASE$endpoint").toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                connection.disconnect()
                response
            } else {
                connection.disconnect()
                null
            }
        } catch (e: Exception) {
            null
        }
    }

}