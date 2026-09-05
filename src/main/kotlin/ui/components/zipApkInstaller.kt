package ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ui.components.theme.AppFontFamily
import ui.components.theme.DropdownColors
import ui.components.theme.gradientColors
import utils.helpers.AppConfig
import utils.helpers.ConsoleInstallMessage
import utils.helpers.moveZipToTrash
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream


enum class BuildVariant(val displayName: String, val folderName: String) {
    DEBUG("Debug", "googleDebug"),
    DEBUG_DEBUGGABLE("Debug Debuggable", "googleDebug"),
    PROD_DEBUG("Prod Debug", "googleProdDebug"),
    RELEASE("Release", "googleRelease"),
}

data class InstallResult(val success: Boolean, val message: String)

/** Only these two are offered in the picker. */
private val ALLOWED_EXTENSIONS = listOf("zip", "apk")

/** Real Downloads dir (the old "/Downloads" was an invalid absolute path). */
private val downloadsDir: File
    get() = File(System.getProperty("user.home"), "Downloads")

/**
 * FileKit's PlatformFile -> java.io.File.
 * If `path` doesn't resolve on your FileKit version, try `this.file` instead.
 */
private fun PlatformFile.toJavaFile(): File = File(path)


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun zipApkInstaller(
    deviceId: String,
    enabled: Boolean = true,
    onInstallStarted: (ConsoleInstallMessage) -> Unit,
    onInstallCompleted: (ConsoleInstallMessage) -> Unit,
    modifier: Modifier = Modifier,
    removeZipAfterInstall: Boolean
) {
    var selectedZipPath by remember { mutableStateOf<String?>(null) }
    var showVariantDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var isHovering by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val contentColor by animateColorAsState(
        targetValue = if (isHovering) Color.White else Color(0xFF9CA3AF),
        animationSpec = tween(durationMillis = 300),
        label = "contentColor"
    )

    val borderAlpha by animateFloatAsState(
        targetValue = if (isHovering) 1f else 0.2f,
        animationSpec = tween(durationMillis = 300),
        label = "borderAlpha"
    )
    val pickerLauncher = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = ALLOWED_EXTENSIONS),
        title = "Select ZIP or APK",
        directory = PlatformFile(downloadsDir)
    ) { platformFile: PlatformFile? ->
        // null == user cancelled
        val file = platformFile?.toJavaFile() ?: return@rememberFilePickerLauncher

        when (file.extension.lowercase()) {
            "apk" -> {
                coroutineScope.launch {
                    isProcessing = true
                    try {
                        onInstallStarted(ConsoleInstallMessage.Text("Installing ${file.name}..."))
                        val result = installApkDirectly(deviceId, file.absolutePath, file.name)
                        onInstallCompleted(ConsoleInstallMessage.Text(result.message))
                    } catch (e: Exception) {
                        onInstallCompleted(
                            ConsoleInstallMessage.Text(
                                "Installation failed: ${e.message ?: "Unknown error"}"
                            )
                        )
                    } finally {
                        isProcessing = false
                    }
                }
            }

            "zip" -> {
                selectedZipPath = file.absolutePath
                showVariantDialog = true
            }

            else -> onInstallCompleted(
                ConsoleInstallMessage.Text("Error: Unsupported file type chosen")
            )
        }
    }

    if (showVariantDialog && selectedZipPath != null) {
        val zipPath = selectedZipPath!!
        variantSelectionDialog(
            zipFileName = File(zipPath).nameWithoutExtension,
            onVariantSelected = { variant ->
                showVariantDialog = false
                coroutineScope.launch {
                    isProcessing = true
                    try {
                        val zipName = File(zipPath).nameWithoutExtension
                        onInstallStarted(ConsoleInstallMessage.Text("Unzipping $zipName..."))

                        val result = unzipAndInstall(
                            zipPath = zipPath,
                            zipName = zipName,
                            variant = variant,
                            deviceId = deviceId,
                            onProgress = { onInstallStarted(ConsoleInstallMessage.Text(it)) },
                            permanentlyDelete = removeZipAfterInstall
                        )

                        onInstallCompleted(ConsoleInstallMessage.Text(result.message))
                    } catch (e: Exception) {
                        onInstallCompleted(
                            ConsoleInstallMessage.Text("Error: ${e.message ?: "Unknown error"}")
                        )
                    } finally {
                        isProcessing = false
                        selectedZipPath = null
                    }
                }
            },
            onDismiss = {
                showVariantDialog = false
                selectedZipPath = null
            }
        )
    }
    Box(
        modifier = modifier
            .height(48.dp)
    ) {
        OutlinedButton(
            onClick = { pickerLauncher.launch() },
            enabled = enabled && !isProcessing,
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = Color(0xFF0C161C),
                contentColor = contentColor
            ),
            shape = RoundedCornerShape(8.dp),
            border = null,
            modifier = Modifier
                .fillMaxHeight()
                .onPointerEvent(PointerEventType.Enter) { isHovering = true }
                .onPointerEvent(PointerEventType.Exit) { isHovering = false }
                .drawBehind {
                    val borderWidth = 3.dp.toPx()
                    val gradient = Brush.sweepGradient(gradientColors)

                    drawRoundRect(
                        brush = gradient,
                        style = Stroke(width = borderWidth),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                        alpha = borderAlpha,
                    )
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .widthIn(min = 190.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFFFFEB3B),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.FolderZip,
                        contentDescription = "Browse ZIP",
                        modifier = Modifier.size(22.dp),
                        tint = contentColor
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Install from zip or apk",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            }
        }
    }
}


@Composable
internal fun variantSelectionDialog(
    zipFileName: String,
    onVariantSelected: (BuildVariant) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DropdownColors.cardBg,
            elevation = 16.dp,
            modifier = Modifier
                .width(500.dp)
                .heightIn(max = 450.dp)
        ) {
            Column {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DropdownColors.dialogHeaderBg)
                        .padding(20.dp, 16.dp, 20.dp, 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Build Variant",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = DropdownColors.textPrimary
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = DropdownColors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = zipFileName,
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 12.sp,
                        color = DropdownColors.textMuted
                    )
                }

                Divider(color = DropdownColors.divider)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = Color(0xFF3DDC84),
                        modifier = Modifier.size(40.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    BuildVariant.entries.forEach { variant ->
                        val interaction = remember { MutableInteractionSource() }
                        val isHovered by interaction.collectIsHoveredAsState()

                        Button(
                            onClick = { onVariantSelected(variant) },
                            interactionSource = interaction,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = when {
                                    isHovered -> when (variant) {
                                        BuildVariant.DEBUG -> Color(0xFF1976D2)
                                        BuildVariant.DEBUG_DEBUGGABLE -> Color(0xFF1976D2)
                                        BuildVariant.PROD_DEBUG -> Color(0xFFE68900)
                                        BuildVariant.RELEASE -> Color(0xFF388E3C)
                                    }

                                    else -> when (variant) {
                                        BuildVariant.DEBUG -> Color(0xFF2196F3)
                                        BuildVariant.DEBUG_DEBUGGABLE -> Color(0xFF1976D2)
                                        BuildVariant.PROD_DEBUG -> Color(0xFFFF9800)
                                        BuildVariant.RELEASE -> Color(0xFF4CAF50)
                                    }
                                },
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            elevation = ButtonDefaults.elevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp,
                                hoveredElevation = 0.dp
                            )
                        ) {
                            Text(
                                text = variant.displayName,
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}


private suspend fun installApkDirectly(
    deviceId: String,
    apkPath: String,
    apkName: String
): InstallResult = withContext(Dispatchers.IO) {
    try {
        val process = ProcessBuilder(AppConfig.adbPath, "-s", deviceId, "install", "-r", apkPath)
            .redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        if (exitCode == 0 && output.contains("Success", ignoreCase = true)) {
            InstallResult(true, "Successfully installed: $apkName")
        } else {
            val errorLine = output.lines().find { it.contains("INSTALL_FAILED") }
            val errorMsg = if (errorLine != null) {
                val reason = errorLine.substringAfter("INSTALL_FAILED").replace("_", " ").trim()
                "Installation failed: INSTALL_FAILED$reason"
            } else {
                val cleanOutput = output.lines()
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                "Installation failed: ${cleanOutput.ifEmpty { "Unknown error" }}"
            }
            InstallResult(false, errorMsg)
        }
    } catch (e: Exception) {
        InstallResult(false, "Installation failed: ${e.message ?: "Unknown error"}")
    }
}


internal suspend fun unzipAndInstall(
    zipPath: String,
    zipName: String,
    variant: BuildVariant,
    deviceId: String,
    onProgress: (String) -> Unit,
    permanentlyDelete: Boolean
): InstallResult = withContext(Dispatchers.IO) {
    val extractDir = File(downloadsDir, zipName)

    try {
        if (extractDir.exists()) extractDir.deleteRecursively()
        extractDir.mkdirs()

        val extractRoot = extractDir.canonicalFile

        ZipInputStream(FileInputStream(zipPath)).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                val filePath = File(extractDir, entry.name).canonicalFile

                if (!filePath.path.startsWith(extractRoot.path + File.separator)) {
                    return@withContext InstallResult(
                        false,
                        "Error: zip entry escapes target directory (${entry.name})"
                    )
                }

                if (entry.isDirectory) {
                    filePath.mkdirs()
                } else {
                    filePath.parentFile?.mkdirs()
                    FileOutputStream(filePath).use { zipIn.copyTo(it) }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }

        onProgress("Unzipped successfully")

        val apkBaseDir = File(extractDir, "apk")
        val variantDir = File(apkBaseDir, variant.folderName)
        val apkFile = variantDir.listFiles()?.firstOrNull { it.extension == "apk" }

        if (apkFile == null || !apkFile.exists()) {
            return@withContext InstallResult(false, "Error: No APK found in ${variant.folderName}")
        }

        onProgress("Installing ${apkFile.name}...")
        val installResult = installApkDirectly(deviceId, apkFile.absolutePath, apkFile.name)

        if (installResult.success) {
            onProgress("Cleaning up...")

            if (extractDir.exists()) {
                try {
                    moveZipToTrash(
                        filePath = extractDir.absolutePath,
                        permanentlyDelete = permanentlyDelete
                    )
                } catch (e: Exception) {
                    // If moving to trash fails, just delete it
                    extractDir.deleteRecursively()
                }
            }
            try {
                moveZipToTrash(filePath = zipPath, permanentlyDelete = permanentlyDelete)
                println("Moved zip file to trash: $zipPath")
            } catch (e: Exception) {
                println("Failed to move zip to trash: ${e.message}")
            }
        }

        return@withContext installResult

    } catch (e: Exception) {
        InstallResult(false, "Error: ${e.message}")
    }
}