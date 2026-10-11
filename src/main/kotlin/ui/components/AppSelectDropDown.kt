package ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import commands.Emoji
import commands.Emoji.PACKAGE_EMOJI
import commands.appConfigs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ui.components.theme.AppFontFamily
import ui.components.theme.DropdownColors
import ui.components.theme.gradientColorsIcons
import utils.helpers.*

private data class AppInfo(
    val guid: String = "",
    val version: String = "",
    val installTime: String = "",
    val size: String = ""
)

private const val LOADING_TEXT = "Loading..."

private fun copyToClipboard(text: String) {
    if (text.isBlank() || text == LOADING_TEXT) return
    val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
    val selection = java.awt.datatransfer.StringSelection(text)
    clipboard.setContents(selection, selection)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun appSelectDropDown(
    selectedAppSource: String,
    commandOptions: Map<String, String>,
    onCommandSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedHardwareSource: String,
    selectedEnvSource: String = "",
    hasSelectedDevice: Boolean = false,
    selectedAppKey: String = "",
    hasSelectedApp: Boolean = false,
    hasSelectedEnv: Boolean = false,
    selectedPackage: String = "",
) {
    var dropdownWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    var isExpanded by remember { mutableStateOf(false) }
    var showAppInfoDialog by remember { mutableStateOf(false) }

    var appInfo by remember { mutableStateOf<AppInfo?>(null) }

    // Load app details once per open (and again if device/package changes while open)
    LaunchedEffect(showAppInfoDialog, selectedHardwareSource, selectedPackage) {
        if (!showAppInfoDialog || selectedPackage.isEmpty()) return@LaunchedEffect
        appInfo = null
        appInfo = withContext(Dispatchers.IO) {
            try {
                AppInfo(
                    guid = getAppGUID(selectedHardwareSource, selectedPackage),
                    version = getAppVersion(selectedHardwareSource, selectedPackage),
                    installTime = getAppInstallTime(selectedHardwareSource, selectedPackage),
                    size = getAppSizes(selectedHardwareSource, selectedPackage)
                )
            } catch (e: Exception) {
                println("Failed to load app info: ${e.message}")
                AppInfo()
            }
        }
    }

    // Close the dialog if the selected app is no longer installed or the device is gone
    LaunchedEffect(selectedHardwareSource, selectedAppKey, commandOptions) {
        if (showAppInfoDialog) {
            val stillValid = selectedAppKey in commandOptions &&
                    selectedHardwareSource.isNotEmpty() &&
                    !selectedHardwareSource.startsWith("Select", ignoreCase = true)
            if (!stillValid) showAppInfoDialog = false
        }
    }

    Card(
        modifier = modifier,
        elevation = 8.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = DropdownColors.cardBg,
        border = BorderStroke(1.dp, DropdownColors.border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = "Applications",
                        tint = Color.White,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.sweepGradient(gradientColorsIcons),
                                    blendMode = BlendMode.SrcAtop
                                )
                            }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Select Application",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = DropdownColors.textPrimary
                    )
                    if (!hasSelectedApp && commandOptions.isNotEmpty()) {
                        Spacer(Modifier.width(4.dp))
                        val pulseTransition = rememberInfiniteTransition()
                        val pulseAlpha by pulseTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 0.3f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444).copy(alpha = pulseAlpha))
                        )
                    }
                    if (commandOptions.isNotEmpty() && hasSelectedDevice && hasSelectedApp && hasSelectedEnv) {
                        Spacer(modifier = Modifier.width(6.dp))
                        TooltipArea(
                            tooltip = {
                                Surface(
                                    color = Color(0xFF1A1A1A),
                                    shape = RoundedCornerShape(6.dp),
                                    elevation = 4.dp
                                ) {
                                    Text(
                                        "App Information",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontFamily = AppFontFamily,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            },
                            delayMillis = 500
                        ) {
                            appHeaderIconButton(
                                icon = Icons.Default.Quiz,
                                onClick = { showAppInfoDialog = true }
                            )
                        }
                    }
                }
            }

            Box {
                val triggerInteraction = remember { MutableInteractionSource() }
                val isTriggerHovered by triggerInteraction.collectIsHoveredAsState()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DropdownColors.fieldBg)
                        .border(
                            1.dp,
                            if (isTriggerHovered) DropdownColors.fieldBorderHover else DropdownColors.fieldBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .hoverable(triggerInteraction)
                        .clickable(enabled = enabled && commandOptions.isNotEmpty()) {
                            isExpanded = true
                        }
                        .onGloballyPositioned { coordinates ->
                            dropdownWidth = with(density) {
                                coordinates.size.width.toDp()
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                        .heightIn(min = 25.dp)
                ) {
                    if (selectedAppKey.isNotEmpty()) {
                        appInlineLabel(
                            appKey = selectedAppKey,
                            textColor = DropdownColors.textPrimary,
                            enabled = enabled,
                        )
                    } else {
                        Text(
                            text = selectedAppSource,
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = DropdownColors.textPlaceholder
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "\u21C5",
                        fontSize = 12.sp,
                        color = DropdownColors.textMuted
                    )
                }

                MaterialTheme(
                    colors = MaterialTheme.colors.copy(surface = DropdownColors.dropdownBg)
                ) {
                    DropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false },
                        modifier = Modifier
                            .width(dropdownWidth)
                            .border(1.dp, DropdownColors.dropdownBorder, RoundedCornerShape(8.dp))
                    ) {
                        if (commandOptions.isEmpty()) {
                            DropdownMenuItem(onClick = {}) {
                                Text(
                                    text = "No Apps installed",
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = DropdownColors.textDim
                                )
                            }
                        } else {
                            commandOptions.forEach { (key, _) ->
                                key(key) {
                                    val itemInteraction = remember { MutableInteractionSource() }
                                    val itemHovered by itemInteraction.collectIsHoveredAsState()

                                    DropdownMenuItem(
                                        onClick = {
                                            onCommandSelected(key)
                                            isExpanded = false
                                        },
                                        interactionSource = itemInteraction,
                                        modifier = Modifier
                                            .background(
                                                if (itemHovered) DropdownColors.rowHover else Color.Transparent
                                            )
                                    ) {
                                        appInlineLabel(
                                            appKey = key,
                                            textColor = DropdownColors.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAppInfoDialog) {
        Dialog(onDismissRequest = { showAppInfoDialog = false }) {
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
                                text = "Application Information $PACKAGE_EMOJI",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = DropdownColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { showAppInfoDialog = false },
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
                            "Installed application details and version info",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Light,
                            fontSize = 12.sp,
                            color = DropdownColors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Divider(color = DropdownColors.divider)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        val scrollState = rememberScrollState()
                        val info = appInfo

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(start = 20.dp, end = 28.dp, top = 20.dp, bottom = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.size(60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val iconRes = appConfigs[selectedAppKey]?.iconRes
                                if (iconRes != null) {
                                    Image(
                                        painter = painterResource(iconRes),
                                        contentDescription = selectedAppSource,
                                        modifier = Modifier.size(60.dp)
                                    )
                                } else {
                                    Text(
                                        text = Emoji.NO_DEVICE,
                                        fontSize = 36.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (selectedEnvSource.isNotBlank() && !selectedEnvSource.startsWith("Select", ignoreCase = true)) {
                                styledAppInfoItem(
                                    label = "Environment",
                                    value = selectedEnvSource
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            styledAppInfoItem(
                                label = "Package Name",
                                value = selectedPackage,
                                onCopy = { copyToClipboard(selectedPackage) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            styledAppInfoItem(
                                label = "Device GUID",
                                value = info?.guid ?: LOADING_TEXT,
                                onCopy = { info?.guid?.let { copyToClipboard(it) } }
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            styledAppInfoItem(
                                label = "Version and Version Code",
                                value = info?.version ?: LOADING_TEXT
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            styledAppInfoItem(
                                label = "First Install Time",
                                value = info?.installTime ?: LOADING_TEXT
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            styledAppInfoItem(
                                label = "Application Size (Approx.)",
                                value = info?.size ?: LOADING_TEXT
                            )
                        }

                        VerticalScrollbar(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(end = 4.dp, top = 4.dp, bottom = 4.dp),
                            adapter = rememberScrollbarAdapter(scrollState),
                            style = ScrollbarStyle(
                                minimalHeight = 40.dp,
                                thickness = 6.dp,
                                shape = RoundedCornerShape(3.dp),
                                hoverDurationMillis = 300,
                                unhoverColor = Color(0xFF556677),
                                hoverColor = Color(0xFF8899AA)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun appHeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()

    IconButton(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier.size(24.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Application Information",
            tint = if (isHovered) DropdownColors.iconHover else DropdownColors.iconDefault,
            modifier = Modifier.size(16.dp)
        )
    }
}