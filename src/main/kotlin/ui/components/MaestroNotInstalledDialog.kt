package ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import ui.components.theme.AppFontFamily
import ui.components.theme.DropdownColors
import java.awt.Desktop
import java.net.URI
import androidx.compose.foundation.text.selection.SelectionContainer

private val warningAmber = Color(0xFFF59E0B)
private val infoBlue = Color(0xFF60A5FA)

private const val MAESTRO_DOCS_URL =
    "https://docs.maestro.dev/getting-started/installing-maestro"

private const val CMD_INSTALL_SCRIPT =
    "curl -fsSL \"https://get.maestro.mobile.dev\" | bash"
private const val CMD_INSTALL_BREW =
    "brew tap mobile-dev-inc/tap && brew install maestro"
private const val CMD_PATH_EXPORT =
    "echo 'export PATH=\"\$PATH\":\"\$HOME/.maestro/bin\"' >> ~/.zshrc && source ~/.zshrc"
private const val CMD_VERIFY =
    "maestro --version"

@Composable
fun maestroNotInstalledDialog(
    isRechecking: Boolean = false,
    onRecheck: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DropdownColors.cardBg,
            elevation = 16.dp,
            modifier = Modifier
                .width(560.dp)
                .heightIn(max = 620.dp)
        ) {
            Column {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DropdownColors.headerBg)
                        .padding(20.dp, 16.dp, 20.dp, 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Maestro Not Installed",
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
                        text = "The Maestro CLI could not be found on this machine",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 12.sp,
                        color = DropdownColors.textMuted
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(warningAmber.copy(alpha = 0.1f))
                            .border(1.dp, warningAmber.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = warningAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "This feature requires Maestro. Install it below, then re-check.",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            color = warningAmber
                        )
                    }
                }

                Divider(color = DropdownColors.divider)
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    installStep(
                        number = "1",
                        title = "Install Maestro",
                        description = "Run the official install script in Terminal.",
                        command = CMD_INSTALL_SCRIPT
                    )

                    installStep(
                        number = "2",
                        title = "Or install with Homebrew",
                        description = "Prefer brew? Use this instead of step 1.",
                        command = CMD_INSTALL_BREW
                    )

                    installStep(
                        number = "3",
                        title = "Add Maestro to your PATH",
                        description = "Only needed if the install script did not do it for you.",
                        command = CMD_PATH_EXPORT
                    )

                    installStep(
                        number = "4",
                        title = "Verify the installation",
                        description = "You should see a version number printed.",
                        command = CMD_VERIFY
                    )

                    // Requirements / note callout
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(infoBlue.copy(alpha = 0.08f))
                            .border(1.dp, infoBlue.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = infoBlue,
                            modifier = Modifier.size(16.dp).padding(top = 1.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Requirements",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = infoBlue
                            )
                            Text(
                                text = "• Java 11 or newer must be installed (java -version)\n" +
                                        "• Xcode command line tools for iOS simulators\n" +
                                        "• Android platform-tools (adb) for Android devices\n" +
                                        "• Restart this app after installing so the new PATH is picked up",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Light,
                                fontSize = 11.sp,
                                color = DropdownColors.textMuted
                            )
                        }
                    }
                }

                Divider(color = DropdownColors.divider)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DropdownColors.footerBg)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val docsInteraction = remember { MutableInteractionSource() }
                    val isDocsHovered by docsInteraction.collectIsHoveredAsState()

                    TextButton(
                        onClick = { openInBrowser(MAESTRO_DOCS_URL) },
                        interactionSource = docsInteraction,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isDocsHovered) DropdownColors.accentYellow
                            else DropdownColors.textMuted
                        )
                    ) {
                        Text(
                            text = "Open Maestro Docs ↗",
                            fontFamily = AppFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val closeInteraction = remember { MutableInteractionSource() }
                        val isCloseHovered by closeInteraction.collectIsHoveredAsState()

                        TextButton(
                            onClick = onDismiss,
                            interactionSource = closeInteraction,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (isCloseHovered) DropdownColors.textSecondary
                                else DropdownColors.textMuted
                            )
                        ) {
                            Text(
                                "Close",
                                fontFamily = AppFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        val recheckInteraction = remember { MutableInteractionSource() }
                        val isRecheckHovered by recheckInteraction.collectIsHoveredAsState()

                        Button(
                            onClick = onRecheck,
                            enabled = !isRechecking,
                            interactionSource = recheckInteraction,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (isRecheckHovered) DropdownColors.accentYellow
                                else DropdownColors.fieldBg,
                                contentColor = if (isRecheckHovered) Color.Black
                                else DropdownColors.textPrimary,
                                disabledBackgroundColor = DropdownColors.fieldBg,
                                disabledContentColor = DropdownColors.textDim
                            ),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isRecheckHovered) DropdownColors.accentYellow
                                else DropdownColors.fieldBorder
                            ),
                            elevation = ButtonDefaults.elevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp,
                                hoveredElevation = 0.dp
                            ),
                            modifier = Modifier
                                .height(32.dp)
                                .widthIn(min = 110.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            if (isRechecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = DropdownColors.textMuted,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Re-check",
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
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
private fun installStep(
    number: String,
    title: String,
    description: String,
    command: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DropdownColors.accentYellow.copy(alpha = 0.15f))
                    .border(
                        1.dp,
                        DropdownColors.accentYellow.copy(alpha = 0.4f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = DropdownColors.accentYellow
                )
            }

            Column {
                Text(
                    text = title,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = DropdownColors.textPrimary
                )
                Text(
                    text = description,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Light,
                    fontSize = 11.sp,
                    color = DropdownColors.textMuted
                )
            }
        }

        commandBlock(command = command)
    }
}

@Composable
private fun commandBlock(command: String) {
    val clipboard = LocalClipboardManager.current
    val rowInteraction = remember { MutableInteractionSource() }
    val isRowHovered by rowInteraction.collectIsHoveredAsState()

    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DropdownColors.fieldBg)
            .border(
                1.dp,
                if (isRowHovered) DropdownColors.fieldBorderHover else DropdownColors.fieldBorder,
                RoundedCornerShape(8.dp)
            )
            .hoverable(rowInteraction)
            .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SelectionContainer(modifier = Modifier.weight(1f)) {
            Text(
                text = command,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = DropdownColors.textSecondary
            )
        }

        val copyInteraction = remember { MutableInteractionSource() }
        val isCopyHovered by copyInteraction.collectIsHoveredAsState()

        TextButton(
            onClick = {
                clipboard.setText(AnnotatedString(command))
                copied = true
            },
            interactionSource = copyInteraction,
            colors = ButtonDefaults.textButtonColors(
                contentColor = when {
                    copied -> DropdownColors.green
                    isCopyHovered -> DropdownColors.accentYellow
                    else -> DropdownColors.textMuted
                }
            ),
            modifier = Modifier.height(28.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
        ) {
            if (copied) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = if (copied) "Copied" else "Copy",
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

private fun openInBrowser(url: String) {
    try {
        if (Desktop.isDesktopSupported() &&
            Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)
        ) {
            Desktop.getDesktop().browse(URI(url))
        } else {
            ProcessBuilder("open", url).start()
        }
    } catch (_: Throwable) {
    }
}