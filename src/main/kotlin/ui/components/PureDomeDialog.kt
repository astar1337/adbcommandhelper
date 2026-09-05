package ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ui.components.theme.AppFontFamily
import ui.components.theme.DropdownColors
import utils.helpers.AppDetection

@Composable
fun pureDomeMissingDialog(
    showDialog: Boolean,
    deviceId: String,
    deviceName: String,
    isChecking: Boolean,
    onRecheck: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!showDialog) return

    val deviceLabel = deviceName.ifBlank { deviceId }.ifBlank { "No device selected" }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DropdownColors.cardBg,
            elevation = 16.dp,
            modifier = Modifier
                .width(500.dp)
                .heightIn(max = 500.dp)
        ) {
            Column {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DropdownColors.headerBg)
                        .padding(20.dp, 16.dp, 20.dp, 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PureDome App Required",
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
                        text = if (deviceName.isNotBlank())
                            "The PureDome VPN app was not found on $deviceName"
                        else
                            "The PureDome VPN app was not found on the selected device",
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
                            .background(Color(0xFFF59E0B).copy(alpha = 0.1f))
                            .border(
                                1.dp,
                                Color(0xFFF59E0B).copy(alpha = 0.3f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "VPN region switching is unavailable until the app is installed.",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            color = Color(0xFFF59E0B)
                        )
                    }
                }

                Divider(color = DropdownColors.divider)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    detailRow(
                        label = "Target device",
                        value = deviceLabel,
                    )
                    detailRow(label = "Missing package", value = AppDetection.PUREDOME_PACKAGE)

                    Text(
                        text = "How to fix",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = DropdownColors.textPrimary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        stepRow(1, "Install the PureDome B2B APK on the device.")
                        stepRow(2, "Open the app and log in.")
                        stepRow(3, "Press Re-check to verify.")
                    }
                }

                Divider(color = DropdownColors.divider)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DropdownColors.footerBg)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
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

                    Spacer(Modifier.width(8.dp))

                    val recheckInteraction = remember { MutableInteractionSource() }
                    val isRecheckHovered by recheckInteraction.collectIsHoveredAsState()

                    Button(
                        onClick = onRecheck,
                        enabled = !isChecking && deviceId.isNotEmpty(),
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
                            .requiredSize(width = 104.dp, height = 32.dp)
                            .pointerHoverIcon(PointerIcon.Hand),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = DropdownColors.textMuted,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Re-check",
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
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
private fun detailRow(label: String, value: String, caption: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 11.sp,
            color = DropdownColors.textMuted
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(DropdownColors.fieldBg)
                .border(1.dp, DropdownColors.fieldBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = DropdownColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (caption != null) {
                Text(
                    text = caption,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Light,
                    fontSize = 10.sp,
                    color = DropdownColors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun stepRow(number: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$number.",
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = DropdownColors.accentYellow
        )
        Text(
            text = text,
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 11.sp,
            color = DropdownColors.textMuted
        )
    }
}