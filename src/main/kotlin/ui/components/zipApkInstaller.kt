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
import commands.Emoji
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle


enum class BuildType(val label: String, val defaultColor: Color, val hoverColor: Color) {
    DEBUG("Debug", Color(0xFF2196F3), Color(0xFF1976D2)),
    DEBUG_DEBUGGABLE("Debug Debuggable", Color(0xFF2196F3), Color(0xFF1976D2)),
    PROD_DEBUG("Prod Debug", Color(0xFFFF9800), Color(0xFFE68900)),
    RELEASE("Release", Color(0xFF4CAF50), Color(0xFF388E3C));
}

enum class Store(val displayName: String, val color: Color) {
    GOOGLE("Google", Color(0xff0262f6)),   // blue
    AMAZON("Amazon", Color(0xFFFFC107)),   // yellow
    HUAWEI("Huawei", Color(0xFFE53935));   // red

    companion object {
        private val storeRegex = Regex("""-mobile-([A-Za-z]+)""", RegexOption.IGNORE_CASE)

        fun fromZipName(zipName: String): Store? {
            val storeName = storeRegex.findAll(zipName).lastOrNull()?.groupValues?.get(1)
                ?: return null
            return entries.firstOrNull { it.displayName.equals(storeName, ignoreCase = true) }
        }
    }
}

enum class BuildVariant(
    val folderName: String,
    val buildType: BuildType,
    val store: Store
) {
    DEBUG_GOOGLE("googleDebug", BuildType.DEBUG, Store.GOOGLE),
    DEBUG_DEBUGGABLE_GOOGLE("googleDebug", BuildType.DEBUG_DEBUGGABLE, Store.GOOGLE),
    PROD_DEBUG_GOOGLE("googleProdDebug", BuildType.PROD_DEBUG, Store.GOOGLE),
    RELEASE_GOOGLE("googleRelease", BuildType.RELEASE, Store.GOOGLE),

    DEBUG_AMAZON("amazonDebug", BuildType.DEBUG, Store.AMAZON),
    DEBUG_DEBUGGABLE_AMAZON("amazonDebug", BuildType.DEBUG_DEBUGGABLE, Store.AMAZON),
    PROD_DEBUG_AMAZON("amazonProdDebug", BuildType.PROD_DEBUG, Store.AMAZON),
    RELEASE_AMAZON("amazonRelease", BuildType.RELEASE, Store.AMAZON),

    DEBUG_HUAWEI("huaweiDebug", BuildType.DEBUG, Store.HUAWEI),
    DEBUG_DEBUGGABLE_HUAWEI("huaweiDebug", BuildType.DEBUG_DEBUGGABLE, Store.HUAWEI),
    PROD_DEBUG_HUAWEI("huaweiProdDebug", BuildType.PROD_DEBUG, Store.HUAWEI),
    RELEASE_HUAWEI("huaweiRelease", BuildType.RELEASE, Store.HUAWEI);

    val label: String
        get() = "${buildType.label} ${store.displayName}"

}

data class InstallResult(val success: Boolean, val message: String)

private val ALLOWED_EXTENSIONS = listOf("zip", "apk")

private val downloadsDir: File
    get() = File(System.getProperty("user.home"), "Downloads")

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
        val zipName = File(zipPath).nameWithoutExtension
        variantSelectionDialog(
            zipFileName = zipName,
            onVariantSelected = { variant ->
                showVariantDialog = false
                coroutineScope.launch {
                    isProcessing = true
                    try {
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
    val detectedStore = remember(zipFileName) { Store.fromZipName(zipFileName) }
    val store = detectedStore ?: Store.GOOGLE
    val variants = remember(store) { BuildVariant.entries.filter { it.store == store } }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DropdownColors.cardBg,
            elevation = 16.dp,
            modifier = Modifier.width(500.dp)
        ) {
            Column {
                // Header
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
                            text = "${Emoji.MOBILE_PHONE_WITH_ARROW} Select Build Variant",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = DropdownColors.textPrimary
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
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
                        text = buildAnnotatedString {
                            if (detectedStore != null) {
                                append("Store type: ")
                            } else {
                                withStyle(SpanStyle(color = Color(0xFFFFB300))) {
                                    append("Store not detected, defaulting to ")
                                }
                            }
                            withStyle(SpanStyle(color = store.color)) {
                                append(store.displayName)
                            }
                        },
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    Text(
                        text = zipFileName,
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 12.sp,
                        color = DropdownColors.textMuted
                    )
                }

                Divider(color = DropdownColors.divider)

                // Buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = Color(0xFF3DDC84),
                        modifier = Modifier.size(40.dp)
                    )
                    variants.forEach { variant ->
                        key(variant) {
                            val interaction = remember { MutableInteractionSource() }
                            val isHovered by interaction.collectIsHoveredAsState()

                            Button(
                                onClick = { onVariantSelected(variant) },
                                interactionSource = interaction,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (isHovered) variant.buildType.hoverColor
                                    else variant.buildType.defaultColor,
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
                                    text = variant.label,
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