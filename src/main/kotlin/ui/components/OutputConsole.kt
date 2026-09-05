package ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ui.components.theme.AppFontFamily
import ui.components.theme.DropdownColors
import ui.components.theme.gradientColorsIcons
import utils.helpers.AppConfig
import utils.helpers.ConsoleInstallMessage
import java.io.File
import ui.components.theme.styledWords

private fun stripToolPaths(text: String): String {
    return text.replace(AppConfig.adbPath, "adb")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun outputConsole(
    messages: List<ConsoleInstallMessage>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onExpandChanged: ((Boolean) -> Unit)? = null,
    deviceId: String,
    removeZipAfterInstall: Boolean,
    onNewMessage: (ConsoleInstallMessage) -> Unit,
    emptyStateMessage: String,
    isCleared: Boolean
) {
    val scrollState = rememberLazyListState()
    var isExpanded by remember { mutableStateOf(true) }
    val arrowRotation by animateFloatAsState(if (isExpanded) 180f else 0f, tween(200))
    val coroutineScope = rememberCoroutineScope()

    // State for the variant selection dialog, triggered from within the console
    var showVariantDialog by remember { mutableStateOf(false) }
    var pendingInstallPath by remember { mutableStateOf<String?>(null) }

    if (showVariantDialog && pendingInstallPath != null) {
        variantSelectionDialog(
            zipFileName = File(pendingInstallPath!!).nameWithoutExtension,
            onVariantSelected = { variant ->
                val zipPath = pendingInstallPath!!
                showVariantDialog = false
                pendingInstallPath = null

                coroutineScope.launch {
                    onNewMessage(ConsoleInstallMessage.Text("Starting install from downloaded file..."))
                    val result = unzipAndInstall(
                        zipPath = zipPath,
                        zipName = File(zipPath).nameWithoutExtension,
                        variant = variant,
                        deviceId = deviceId,
                        onProgress = { progress -> onNewMessage(ConsoleInstallMessage.Text(progress)) },
                        permanentlyDelete = removeZipAfterInstall
                    )
                    onNewMessage(ConsoleInstallMessage.Text(result.message))
                }
            },
            onDismiss = {
                showVariantDialog = false
                pendingInstallPath = null
            }
        )
    }


    LaunchedEffect(isExpanded) {
        onExpandChanged?.invoke(isExpanded)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size - 1)
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val spinRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Card(
        modifier = modifier,
        elevation = 8.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = DropdownColors.cardBg,
        border = BorderStroke(1.dp, DropdownColors.border)
    ) {
        Column(
            modifier = if (isExpanded) Modifier.fillMaxSize() else Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(DropdownColors.cardBg)
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Output,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                drawRect(brush = Brush.sweepGradient(gradientColorsIcons), blendMode = BlendMode.SrcAtop)
                            }
                    )
                    Text(
                        text = "Output Console",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (enabled) DropdownColors.textPrimary else DropdownColors.textDim
                    )
                    if (!isExpanded) {
                        val previewText = when (val lastMessage = messages.lastOrNull()) {
                            is ConsoleInstallMessage.Text -> lastMessage.content.lines().firstOrNull { it.isNotBlank() } ?: ""
                            is ConsoleInstallMessage.InstallAction -> lastMessage.preText.lines().firstOrNull { it.isNotBlank() } ?: "Download complete"
                            null -> "Awaiting output from console..."
                        }
                        Text(
                            text = "— $previewText",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Light,
                            fontSize = 11.sp,
                            color = DropdownColors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (messages.isNotEmpty()) {
                        val clearInteraction = remember { MutableInteractionSource() }
                        val isClearHovered by clearInteraction.collectIsHoveredAsState()

                        IconButton(
                            onClick = onClear,
                            enabled = enabled,
                            interactionSource = clearInteraction,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear console",
                                tint = when {
                                    !enabled -> DropdownColors.textDim.copy(alpha = 0.4f)
                                    isClearHovered -> DropdownColors.textPrimary
                                    else -> DropdownColors.textMuted
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = DropdownColors.textMuted,
                        modifier = Modifier.size(20.dp).rotate(arrowRotation)
                    )
                }
            }

            if (isExpanded) {
                Divider(color = DropdownColors.divider)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    if (messages.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Spacer(Modifier.height(8.dp))
                            if (isCleared) {
                                Spacer(Modifier.height(6.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF22C55E),
                                    modifier = Modifier.size(32.dp)
                                )
                            } else {
                                Spacer(Modifier.height(8.dp))
                                Box(modifier = Modifier.size(28.dp).graphicsLayer { rotationZ = spinRotation }) {
                                    CircularProgressIndicator(
                                        progress = 0.7f,
                                        color = DropdownColors.textMuted,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = emptyStateMessage,
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                color = DropdownColors.textMuted
                            )
                        }
                    } else {
                        LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize()) {
                            items(messages) { message ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DropdownColors.fieldBg)
                                        .border(1.dp, DropdownColors.fieldBorder, RoundedCornerShape(6.dp))
                                        .padding(10.dp)
                                ) {
                                    when (message) {
                                        is ConsoleInstallMessage.Text -> {
                                            message.content.lines()
                                                .filter { it.isNotBlank() }
                                                .forEach { line ->
                                                    RenderStyledLine(stripToolPaths(line))
                                                }
                                        }
                                        is ConsoleInstallMessage.InstallAction -> {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(Modifier.weight(1f)) {
                                                    message.preText.lines().filter { it.isNotBlank() }.forEach { line ->
                                                        RenderStyledLine(line)
                                                    }
                                                    RenderStyledLine("File: ${message.filePath}")
                                                }
                                                val interactionSource = remember { MutableInteractionSource() }
                                                val isHovered by interactionSource.collectIsHoveredAsState()

                                                TooltipArea(
                                                    tooltip = {
                                                        Surface(color = Color.Black, shape = RoundedCornerShape(4.dp)) {
                                                            Text("Install this build", modifier = Modifier.padding(8.dp), color = Color.White, fontSize = 12.sp)
                                                        }
                                                    }
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            pendingInstallPath = message.filePath
                                                            showVariantDialog = true
                                                        },
                                                        interactionSource = interactionSource,
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.InstallMobile,
                                                            contentDescription = "Install Build",
                                                            tint = if (isHovered) DropdownColors.green else DropdownColors.textPrimary,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderStyledLine(line: String) {
    if (line.startsWith("File:") && line.contains("/")) {
        val filePath = line.substringAfter("File:").trim()
        val fileInteraction = remember { MutableInteractionSource() }
        val isFileHovered by fileInteraction.collectIsHoveredAsState()

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📁 ", fontSize = 12.sp)
            Text(
                text = filePath,
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = if (isFileHovered) Color(0xFF93C5FD) else Color(0xFF60A5FA),
                textDecoration = if (isFileHovered) androidx.compose.ui.text.style.TextDecoration.Underline else androidx.compose.ui.text.style.TextDecoration.None,
                modifier = Modifier
                    .hoverable(fileInteraction)
                    .clickable {
                        try {
                            val file = File(filePath)
                            if (file.exists()) {
                                java.awt.Desktop.getDesktop().open(file.parentFile)
                            }
                        } catch (_: Exception) { }
                    }
            )
        }
    } else if (line.startsWith("ADB Command:") || line.startsWith("ADB:")) {
        val copyInteraction = remember { MutableInteractionSource() }
        val isCopyHovered by copyInteraction.collectIsHoveredAsState()
        var showCopied by remember { mutableStateOf(false) }

        LaunchedEffect(showCopied) {
            if (showCopied) {
                kotlinx.coroutines.delay(1500)
                showCopied = false
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val styledLine = buildStyledLine(line)
            Text(
                text = styledLine ?: buildAnnotatedString { append(line) },
                fontFamily = AppFontFamily,
                fontSize = 12.sp,
                softWrap = true,
                color = DropdownColors.textPrimary,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = if (showCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = "Copy ADB command",
                tint = when {
                    showCopied -> Color(0xFF22C55E)
                    isCopyHovered -> DropdownColors.textPrimary
                    else -> DropdownColors.textMuted
                },
                modifier = Modifier
                    .size(14.dp)
                    .hoverable(copyInteraction)
                    .clickable {
                        val command = line
                            .substringAfter("ADB Command:")
                            .substringAfter("ADB:")
                            .trim()
                        val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                        val selection = java.awt.datatransfer.StringSelection(command)
                        clipboard.setContents(selection, selection)
                        showCopied = true
                    }
            )
        }
    } else {
        val styledLine = buildStyledLine(line)
        Text(
            text = styledLine ?: buildAnnotatedString { append(line) },
            fontFamily = AppFontFamily,
            fontSize = 12.sp,
            softWrap = true,
            color = DropdownColors.textPrimary
        )
    }
}

@Composable
private fun buildStyledLine(line: String): androidx.compose.ui.text.AnnotatedString? {
    val boldWords = setOf("ScreenCopy", "WiFi", "Airplane Mode", "Screenshot", "Device")
    val hasMatch = styledWords.keys.any { line.contains(it, ignoreCase = true) } ||
            boldWords.any { line.contains(it, ignoreCase = true) }
    if (!hasMatch) return null

    return buildAnnotatedString {
        var remaining = line
        while (remaining.isNotEmpty()) {
            var earliestIndex = remaining.length
            var matchedWord = ""
            var wordColor: Color? = null
            var wordWeight: FontWeight? = null

            val allWords =
                boldWords.map { it to (null to FontWeight.Bold) } +
                        styledWords.map { it.key to it.value }

            for ((word, style) in allWords) {
                val index = remaining.indexOf(word, 0, ignoreCase = true)

                if (index in 0..<earliestIndex) {
                    earliestIndex = index
                    matchedWord = word
                    wordColor = style.first
                    wordWeight = style.second
                }
            }

            if (matchedWord.isEmpty()) {
                withStyle(SpanStyle(color = DropdownColors.textPrimary, fontWeight = FontWeight.Normal)) {
                    append(remaining)
                }
                break
            }
            if (earliestIndex > 0) {
                withStyle(SpanStyle(color = DropdownColors.textPrimary, fontWeight = FontWeight.Normal)) {
                    append(remaining.substring(0, earliestIndex))
                }
            }
            withStyle(
                SpanStyle(
                    color = wordColor ?: DropdownColors.textPrimary,
                    fontWeight = wordWeight ?: FontWeight.Normal
                )
            ) {
                append(matchedWord)
            }
            remaining = remaining.substring(earliestIndex + matchedWord.length)
        }
    }
}