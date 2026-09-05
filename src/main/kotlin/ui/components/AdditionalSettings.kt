package ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import commands.Emoji
import kotlinx.coroutines.launch
import ui.components.theme.AppFontFamily
import utils.helpers.AdbDevices.runCommandFromMap
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.ui.draw.clip
import ui.components.theme.DropdownColors
import utils.helpers.*
import ui.components.theme.MenuColors
import ui.components.theme.gradientColors
import utils.helpers.getDeviceName


@Composable
fun additionalSettings(
    modifier: Modifier = Modifier,
    onCommandExecuted: (String, String) -> Unit,
    deviceId: String,
    removeZipAfterInstall: Boolean,
    onRemoveZipToggle: (Boolean) -> Unit,
) {
    var isHoveringSettings by remember { mutableStateOf(false) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val iconColor by animateColorAsState(
        targetValue = if (isHoveringSettings) Color.White else Color(0xFF9CA3AF),
        animationSpec = tween(durationMillis = 300),
        label = "iconColor"
    )

    val borderAlpha by animateFloatAsState(
        targetValue = if (isHoveringSettings) 1f else 0.2f,
        animationSpec = tween(durationMillis = 300),
        label = "borderAlpha"
    )


    val commandOptions = remember(deviceId) {
        buildMap {
            put(
                "Change Accessory Permissions",
                "OPEN_ACCESSORY_SETTINGS"
            )
            put(
                "Delete ALL DAZN/FOXTEL applications on ${getDeviceName(deviceId)}",
                "DELETE_ALL_APPS"
            )
        }
    }

    Box(modifier = modifier) {
        IconButton(
            onClick = { isMenuExpanded = true },
            modifier = Modifier
                .size(48.dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            isHoveringSettings = when (event.type) {
                                PointerEventType.Enter -> true
                                PointerEventType.Exit -> false
                                else -> isHoveringSettings
                            }
                        }
                    }
                }
                .drawBehind {
                    val borderWidth = 3.dp.toPx()
                    val gradient = Brush.sweepGradient(gradientColors)
                    drawRoundRect(
                        brush = gradient,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                        alpha = borderAlpha,
                        style = Stroke(width = borderWidth)
                    )
                }
        ) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = "Settings",
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
        }

        if (isMenuExpanded) {
            Dialog(onDismissRequest = { isMenuExpanded = false }) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MenuColors.panelBg,
                    elevation = 16.dp,
                    modifier = Modifier
                        .width(500.dp)
                        .heightIn(max = 420.dp)
                ) {
                    Column {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MenuColors.headerBg)
                                .padding(20.dp, 16.dp, 20.dp, 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${Emoji.SETTINGS_GEAR} App Settings",
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MenuColors.textPrimary
                                )
                                IconButton(
                                    onClick = { isMenuExpanded = false },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = MenuColors.textMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Configure app options or run utility commands",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Light,
                                fontSize = 12.sp,
                                color = DropdownColors.textMuted
                            )
                            Spacer(Modifier.height(8.dp))
                    }

                        Divider(color = MenuColors.divider)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "OPTIONS",
                                fontFamily = AppFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MenuColors.textMuted,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                settingsCheckboxRow(
                                    label = "Remove zip after install",
                                    checked = removeZipAfterInstall,
                                    onCheckedChange = { onRemoveZipToggle(it) },
                                    modifier = Modifier.weight(1f)
                                )
                                settingsCheckboxRow(
                                    label = "Keyboard navigation",
                                    checked = false,
                                    onCheckedChange = { },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                settingsCheckboxRow(
                                    label = "Hide Delete app pop-up",
                                    checked = false,
                                    onCheckedChange = { },
                                    modifier = Modifier.weight(1f)
                                )
                                settingsCheckboxRow(
                                    label = "Remove Side-menu",
                                    checked = false,
                                    onCheckedChange = { },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(Modifier.height(4.dp))
                            Divider(color = MenuColors.divider)
                            Spacer(Modifier.height(4.dp))

                            Text(
                                "COMMANDS",
                                fontFamily = AppFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MenuColors.textMuted,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            commandOptions.entries.forEach { (commandLabel, command) ->
                                val isDanger = commandLabel.contains("Delete ALL", ignoreCase = true)

                                settingsCommandRow(
                                    label = commandLabel,
                                    isDanger = isDanger,
                                    onClick = {
                                        coroutineScope.launch {
                                            when (command) {
                                                "DELETE_ALL_APPS" -> {
                                                    onCommandExecuted("Scanning for installed apps...", "")
                                                    val result = deleteAllInstalledApps(deviceId)
                                                    val detailedOutput = buildString {
                                                        appendLine(result.summary)
                                                        appendLine()
                                                    }
                                                    onCommandExecuted(detailedOutput, "Delete All Apps - Complete")
                                                }

                                                "OPEN_ACCESSORY_SETTINGS" -> {
                                                    openAccessorySettings()
                                                }

                                                "LAUNCH_SCRCPY" -> {
                                                    try {
                                                        ProcessBuilder(AppConfig.scrcpyPath, "-s", deviceId)
                                                            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                                                            .redirectError(ProcessBuilder.Redirect.DISCARD)
                                                            .start()
                                                        onCommandExecuted("SCRCPY launched", "Launched SCRCPY for $deviceId")
                                                    } catch (e: Exception) {
                                                        val installUrl = "https://github.com/Genymobile/scrcpy/blob/master/doc/macos.md"
                                                        try {
                                                            if (java.awt.Desktop.isDesktopSupported()) {
                                                                java.awt.Desktop.getDesktop().browse(java.net.URI(installUrl))
                                                            }
                                                        } catch (_: Exception) { }
                                                        onCommandExecuted(
                                                            "Failed to launch SCRCPY: ${e.message}. Installation page opened in browser.",
                                                            "SCRCPY Error"
                                                        )
                                                    }
                                                }

                                                else -> {
                                                    val result = runCommandFromMap(command)
                                                    val output = if (result.isSuccess) {
                                                        result.rawOutput
                                                    } else {
                                                        result.errorMessage ?: "Command failed"
                                                    }
                                                    onCommandExecuted(output, commandLabel)
                                                }
                                            }
                                            isMenuExpanded = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun settingsCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHovered) MenuColors.rowHover else MenuColors.fieldBg)
            .border(1.dp, MenuColors.fieldBorder, RoundedCornerShape(8.dp))
            .hoverable(interaction)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            colors = CheckboxDefaults.colors(
                checkedColor = MenuColors.checkboxChecked,
                uncheckedColor = MenuColors.fieldBorder,
                checkmarkColor = Color.Black
            ),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Normal,
            color = MenuColors.textPrimary,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun settingsCommandRow(
    label: String,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isHovered && isDanger -> MenuColors.dangerRed.copy(alpha = 0.1f)
                    isHovered -> MenuColors.rowHover
                    else -> Color.Transparent
                }
            )
            .hoverable(interaction)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            label,
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = when {
                isDanger && isHovered -> MenuColors.dangerRed
                isDanger -> MenuColors.dangerRed.copy(alpha = 0.8f)
                isHovered -> MenuColors.textPrimary
                else -> MenuColors.textSecondary
            },
            modifier = Modifier.weight(1f)
        )
        Text(
            "›",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isHovered) MenuColors.textPrimary else MenuColors.textDim
        )
    }
}