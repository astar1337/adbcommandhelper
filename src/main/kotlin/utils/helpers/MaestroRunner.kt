package utils.helpers

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ui.components.theme.DropdownColors.green
import ui.components.theme.MenuColors.dangerRed
import java.io.File
import java.util.Collections
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Job

enum class  MaestroApp(
    val key: String,
    val displayName: String,
    private val dirProvider: () -> String,
    private val fileSuffix: String = ""
) {
    DAZN("DAZN", "DAZN", { MaestroRunner.DAZN_FLOWS_DIR }, "_dazn"),
    KAYO("KAYO", "Kayo Sports", { MaestroRunner.KAYO_FLOWS_DIR }, "_kayo"),
    BINGE("BINGE", "Binge", { MaestroRunner.BINGE_FLOWS_DIR }, "_binge");

    val flowsDir: String get() = dirProvider()

    fun resolveFlowFile(logicalName: String): String {
        val base = logicalName.removeSuffix(".yaml").removeSuffix(".yml")
        val candidates = listOf(
            "$base$fileSuffix.yaml",
            "$base$fileSuffix.yml",
            "$base.yaml",
            "$base.yml"
        )
        return candidates.firstOrNull { File("$flowsDir/$it").exists() }
            ?: "$base$fileSuffix.yaml"
    }


    companion object {
        fun fromKey(appKey: String): MaestroApp? =
            entries.firstOrNull { it.key.equals(appKey, ignoreCase = true) }
    }
}

object MaestroRunner {
    data class TestResult(
        val success: Boolean,
        val output: String,
        val durationMs: Long
    )

    private val projectDir = System.getProperty("user.dir")
    private val FLOWS_ROOT: String by lazy {
        val candidates = listOfNotNull(
            System.getProperty("compose.application.resources.dir")
                ?.let { File(it, "maestroflows") },
            File(System.getProperty("user.dir"), "resources/common/maestroflows"),
            File(System.getProperty("user.dir"), "src/main/kotlin/utils/helpers/maestroflows")
        )
        val found = candidates.firstOrNull { it.isDirectory }
        println("MAESTRO FLOWS ROOT: ${found?.absolutePath ?: "NOT FOUND"}")
        (found ?: candidates.first()).absolutePath
    }

    private val maestroPath: String
        get() = MaestroInstallation.quickCheck()?.executablePath ?: AppConfig.maestroPath


    val DAZN_FLOWS_DIR = "$FLOWS_ROOT/dazn"
    val KAYO_FLOWS_DIR = "$FLOWS_ROOT/kayo"
    val BINGE_FLOWS_DIR = "$FLOWS_ROOT/binge"
    val VPN_FLOWS_DIR = "$projectDir/src/main/kotlin/utils/helpers/maestroflowspuredome"

    private fun buildCommand(
        flowFile: File,
        appId: String?,
        deviceId: String?,
        email: String?,
        password: String?,
        extraEnv: Map<String, String>
    ): List<String> = buildList {
        add(maestroPath)
        if (!deviceId.isNullOrBlank()) { add("--device"); add(deviceId) }
        add("test")
        if (!appId.isNullOrBlank()) { add("--app-id"); add(appId) }
        if (!email.isNullOrBlank()) { add("-e"); add("EMAIL=$email") }
        if (!password.isNullOrBlank()) { add("-e"); add("PASSWORD=$password") }
        extraEnv.forEach { (key, value) -> add("-e"); add("$key=$value") }
        add(flowFile.absolutePath)
    }

    private fun nullDevice(): File =
        if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) File("NUL")
        else File("/dev/null")

    private fun killTree(process: Process) {
        runCatching {
            process.toHandle().descendants().forEach { it.destroyForcibly() }
        }
        runCatching { process.destroyForcibly() }
        runCatching { process.waitFor(2, TimeUnit.SECONDS) }
    }

    suspend fun runFlowCancellable(
        flowFileName: String,
        appId: String? = null,
        deviceId: String? = null,
        flowsDir: String = DAZN_FLOWS_DIR,
        email: String? = null,
        password: String? = null,
        extraEnv: Map<String, String> = emptyMap(),
        timeoutMs: Long = TimeUnit.MINUTES.toMillis(5),
        onLineOutput: ((String) -> Unit)? = null
    ): TestResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val dir = File(flowsDir)

        if (!dir.isDirectory) {
            val errorMsg = buildString {
                append("Flows directory not found: ${dir.absolutePath}\n")
                append("resources.dir=${System.getProperty("compose.application.resources.dir") ?: "unset"}\n")
                append("user.dir=${System.getProperty("user.dir")}")
            }
            onLineOutput?.invoke(errorMsg)
            return@withContext TestResult(false, errorMsg, System.currentTimeMillis() - startTime)
        }

        val flowFile = File("$flowsDir/$flowFileName")

        if (!flowFile.exists()) {
            val available = dir.listFiles { f -> f.isFile && (f.extension == "yaml" || f.extension == "yml") }
                ?.joinToString(", ") { it.name }
                ?.ifBlank { "none" }
                ?: "none"
            val errorMsg = "Flow file not found: ${flowFile.absolutePath}\nAvailable flows: $available"
            onLineOutput?.invoke(errorMsg)
            return@withContext TestResult(false, errorMsg, System.currentTimeMillis() - startTime)
        }

        val command = buildCommand(
            flowFile = flowFile,
            appId = appId,
            deviceId = deviceId,
            email = email,
            password = password,
            extraEnv = extraEnv
        )
        val outputLines = Collections.synchronizedList(mutableListOf<String>())
        var process: Process? = null

        try {
            val p = ProcessBuilder(command)
                .directory(flowFile.parentFile)
                .redirectErrorStream(true)
                .redirectInput(ProcessBuilder.Redirect.from(nullDevice()))
                .apply {
                    environment()["MAESTRO_DISABLE_ANALYTICS"] = "1"
                    environment()["PATH"] = augmentedPath()
                }
                .start()
            process = CancellableProcess.track(p)

            val readerThread = Thread {
                runCatching {
                    p.inputStream.bufferedReader().forEachLine { line ->
                        if (line.isNotBlank()) {
                            outputLines.add(line)
                            onLineOutput?.invoke(line)
                        }
                    }
                }
            }.apply {
                name = "maestro-output"
                isDaemon = true
                start()
            }

            val deadline = startTime + timeoutMs
            var timedOut = false
            while (!p.waitFor(100, TimeUnit.MILLISECONDS)) {
                coroutineContext.ensureActive()
                if (System.currentTimeMillis() > deadline) {
                    timedOut = true
                    break
                }
            }

            if (timedOut) killTree(p)

            readerThread.join(1_000)

            val exitCode = if (timedOut) -1 else runCatching { p.exitValue() }.getOrDefault(-1)
            val output = synchronized(outputLines) { outputLines.joinToString("\n") }

            TestResult(
                success = exitCode == 0,
                output = if (timedOut) "$output\nTimed out after ${timeoutMs / 1000}s" else output,
                durationMs = System.currentTimeMillis() - startTime
            )
        } catch (c: CancellationException) {
            withContext(NonCancellable) { process?.let { killTree(it) } }
            throw c
        } catch (e: Exception) {
            val errorMsg = "${e::class.simpleName}: ${e.message}"
            onLineOutput?.invoke(errorMsg)
            TestResult(false, errorMsg, System.currentTimeMillis() - startTime)
        } finally {
            process?.let { CancellableProcess.unTrack(it) }
        }
    }
}

fun runMaestroFlow(
    scope: CoroutineScope,
    flowFileName: String,
    label: String,
    app: MaestroApp = MaestroApp.DAZN,
    appId: String? = null,
    deviceId: String? = null,
    email: String? = null,
    password: String? = null,
    extraEnv: Map<String, String> = emptyMap(),
    onCommandExecuted: (ConsoleInstallMessage, Color?) -> Unit,
    onCommandReplaced: (ConsoleInstallMessage, Color?) -> Unit
): Job = scope.launch{
        val lines = Collections.synchronizedList(mutableListOf<String>())
        val dirty = AtomicBoolean(false)
        val prefix = "Maestro [${app.displayName}]"

        onCommandExecuted(
            ConsoleInstallMessage.Text("$prefix Running $label flow..."),
            Color(0xFFA855F7)
        )

        val flusher = launch {
            while (isActive) {
                delay(120)
                if (dirty.compareAndSet(true, false)) {
                    val snapshot = synchronized(lines) { lines.joinToString("\n") }
                    onCommandReplaced(ConsoleInstallMessage.Text(snapshot), null)
                }
            }
        }

    try {
        val resolvedFlow = app.resolveFlowFile(flowFileName)

        val result = MaestroRunner.runFlowCancellable(
            flowFileName = resolvedFlow,
            deviceId = deviceId,
            flowsDir = app.flowsDir,
            email = email,
            password = password,
            extraEnv = extraEnv + buildMap {
                if (!appId.isNullOrBlank()) put("APP_ID", appId)
            },
            onLineOutput = { line ->
                lines.add(line)
                dirty.set(true)
            }
        )

            flusher.cancel()

            val duration = "%.1fs".format(result.durationMs / 1000.0)
            val finalColor = if (result.success) green else dangerRed
            val header = if (result.success) {
                "$prefix $label: PASSED ($duration)"
            } else {
                "$prefix $label: FAILED ($duration)"
            }
            onCommandReplaced(
                ConsoleInstallMessage.Text("$header\n${result.output}"),
                finalColor
            )
        } catch (c: CancellationException) {
            flusher.cancel()
            val snapshot = synchronized(lines) { lines.joinToString("\n") }
            onCommandReplaced(
                ConsoleInstallMessage.Text("$prefix $label: CANCELLED\n$snapshot"),
                Color(0xFFF59E0B)
            )
            throw c
        } finally {
            flusher.cancel()
        }
    }