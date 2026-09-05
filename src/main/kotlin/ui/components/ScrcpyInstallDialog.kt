package ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ui.components.theme.AppFontFamily
import ui.components.theme.DropdownColors

@Composable
fun scrcpyInstallDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DropdownColors.cardBg,
            elevation = 16.dp,
            modifier = Modifier
                .width(500.dp)
                .wrapContentHeight()
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFEB3B),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "scrcpy Not Found",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = DropdownColors.textPrimary
                            )
                        }
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
                        text = "scrcpy is required for screen mirroring and recording",
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Install scrcpy using Homebrew:",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = DropdownColors.textSecondary
                    )

                    scrcpyCommandBlock("brew install scrcpy")

                    Text(
                        "Or install via the official repository:",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = DropdownColors.textSecondary
                    )

                    val linkInteraction = remember { MutableInteractionSource() }
                    val isLinkHovered by linkInteraction.collectIsHoveredAsState()

                    Text(
                        text = "github.com/Genymobile/scrcpy",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = if (isLinkHovered) Color(0xFF93C5FD) else Color(0xFF60A5FA),
                        modifier = Modifier
                            .hoverable(linkInteraction)
                            .clickable {
                                try {
                                    java.awt.Desktop.getDesktop().browse(
                                        java.net.URI("https://github.com/Genymobile/scrcpy")
                                    )
                                } catch (_: Exception) { }
                            }
                    )

                    Text(
                        "After installation, restart the app and try again.",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 11.sp,
                        color = DropdownColors.textMuted,
                        lineHeight = 16.sp
                    )
                }

                Divider(color = DropdownColors.divider)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DropdownColors.footerBg)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    val closeInteraction = remember { MutableInteractionSource() }
                    val isCloseHovered by closeInteraction.collectIsHoveredAsState()

                    TextButton(
                        onClick = onDismiss,
                        interactionSource = closeInteraction,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isCloseHovered) DropdownColors.textSecondary else DropdownColors.textMuted
                        )
                    ) {
                        Text(
                            "Close",
                            fontFamily = AppFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun scrcpyCommandBlock(command: String) {
    val copyInteraction = remember { MutableInteractionSource() }
    val isCopyHovered by copyInteraction.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0A1015))
            .border(1.dp, DropdownColors.fieldBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$ $command",
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = Color(0xFF22C55E)
        )

        IconButton(
            onClick = {
                val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                val selection = java.awt.datatransfer.StringSelection(command)
                clipboard.setContents(selection, selection)
            },
            interactionSource = copyInteraction,
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = if (isCopyHovered) DropdownColors.textPrimary else DropdownColors.textMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}