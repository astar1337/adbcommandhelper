package utils.helpers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class MaestroStatus(
    val installed: Boolean,
    val executablePath: String? = null
)

object MaestroInstallation {

    private const val NEGATIVE_CACHE_TTL_MS = 10_000L

    private val forceMissing: Boolean =
        System.getProperty("maestro.forceMissing")?.toBoolean() == true

    @Volatile private var cached: MaestroStatus? = null
    @Volatile private var cachedAt: Long = 0L

    private val userHome: String = System.getProperty("user.home") ?: ""

    private val probePool = Executors.newCachedThreadPool { r ->
        Thread(r, "maestro-probe").apply { isDaemon = true }
    }

    private val knownLocations = listOf(
        "$userHome/.maestro/bin/maestro",
        "/opt/homebrew/bin/maestro",
        "/usr/local/bin/maestro",
        "/usr/bin/maestro",
        "$userHome/.local/bin/maestro"
    )


    fun quickCheck(): MaestroStatus? {
        if (forceMissing) return null
        cached?.let { if (it.installed) return it }

        val hit = knownLocations.firstOrNull { isExecutable(it) } ?: pathEnvLookup()
        return hit?.let {
            MaestroStatus(installed = true, executablePath = it).also { s ->
                cached = s
                cachedAt = System.currentTimeMillis()
            }
        }
    }

    suspend fun isInstalled(forceRefresh: Boolean = false): Boolean = status(forceRefresh).installed


    @Suppress("unused")
    suspend fun version(): String? = withContext(Dispatchers.IO) {
        val path = status().executablePath ?: return@withContext null
        runCommand(listOf(path, "--version"), timeoutMs = 10_000)
            ?.lineSequence()
            ?.map { it.trim() }
            ?.firstOrNull { it.isNotEmpty() }
    }


    private suspend fun status(forceRefresh: Boolean = false): MaestroStatus =
        withContext(Dispatchers.IO) {
            cached?.let { c ->
                val fresh = c.installed ||
                        System.currentTimeMillis() - cachedAt < NEGATIVE_CACHE_TTL_MS
                if (!forceRefresh && fresh) return@withContext c
            }

            val resolved = try {
                resolve()
            } catch (t: Throwable) {
                println("[Maestro] detection failed: ${t.message}")
                MaestroStatus(installed = false)
            }

            cached = resolved
            cachedAt = System.currentTimeMillis()
            resolved
        }

    private fun resolve(): MaestroStatus {
        if (forceMissing) {
            println("[Maestro] forceMissing=true — reporting not installed")
            return MaestroStatus(installed = false)
        }

        knownLocations.firstOrNull { isExecutable(it) }?.let { return MaestroStatus(true, it) }
        pathEnvLookup()?.let { return MaestroStatus(true, it) }
        shellLookup()?.let { return MaestroStatus(true, it) }

        return MaestroStatus(installed = false)
    }

    private fun isExecutable(path: String): Boolean =
        File(path).let { it.isFile && it.canExecute() }

    private fun pathEnvLookup(): String? =
        System.getenv("PATH")
            ?.split(File.pathSeparator)
            ?.asSequence()
            ?.filter { it.isNotBlank() }
            ?.map { File(it, "maestro").absolutePath }
            ?.firstOrNull { isExecutable(it) }

    private fun shellLookup(): String? {
        val shell = System.getenv("SHELL")?.takeIf { it.isNotBlank() } ?: "/common/bin/zsh"
        val output = runCommand(listOf(shell, "-l", "-c", "command -v maestro"), timeoutMs = 3_000)
            ?: return null
        return output.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() && isExecutable(it) }
    }

    private fun runCommand(command: List<String>, timeoutMs: Long): String? {
        var process: Process? = null
        return try {
            val p = ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectInput(ProcessBuilder.Redirect.from(File("/dev/null")))
                .apply { environment()["MAESTRO_DISABLE_ANALYTICS"] = "1" }
                .start()
            process = p

            // Read on a daemon thread so a stuck pipe can never freeze us.
            val reader = probePool.submit<String> {
                p.inputStream.bufferedReader().use { it.readText() }
            }

            if (!p.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
                println("[Maestro] timed out: ${command.joinToString(" ")}")
                reader.cancel(true)
                return null
            }

            val output = try {
                reader.get(500, TimeUnit.MILLISECONDS)
            } catch (t: Throwable) {
                reader.cancel(true)
                ""
            }

            if (p.exitValue() == 0) output else null
        } catch (t: Throwable) {
            println("[Maestro] command error (${command.firstOrNull()}): ${t.message}")
            null
        } finally {
            process?.let { if (it.isAlive) it.destroyForcibly() }
        }
    }
}