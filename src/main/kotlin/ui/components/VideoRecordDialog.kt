package ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ui.components.theme.AppFontFamily
import ui.components.theme.DropdownColors

@Composable
fun videoRecordDialog(
    onDismiss: () -> Unit,
    onStartRecording: (noSound: Boolean, showTouches: Boolean, fileName: String) -> Unit,
    onStopRecording: () -> Unit,
    isRecording: Boolean = false
) {
    var noSound by remember { mutableStateOf(false) }
    var showTouches by remember { mutableStateOf(false) }
    var fileName by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition()
    val recordingAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Dialog(onDismissRequest = { if (!isRecording) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DropdownColors.cardBg,
            elevation = 16.dp,
            modifier = Modifier
                .width(400.dp)
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
                            if (isRecording) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444).copy(alpha = recordingAlpha))
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = DropdownColors.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = if (isRecording) "Recording" else "Screen Recording",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isRecording) Color(0xFFEF4444) else DropdownColors.textPrimary
                            )
                        }
                        if (!isRecording) {
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
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isRecording) {
                            val displayName = fileName.ifEmpty { "screen_recording" }
                            "Saving to Desktop/$displayName.mp4"
                        } else "Record your device screen using scrcpy",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 12.sp,
                        color = if (isRecording) Color(0xFFEF4444).copy(alpha = 0.7f) else DropdownColors.textMuted
                    )
                }

                Divider(color = DropdownColors.divider)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                                .border(2.dp, Color(0xFFEF4444).copy(alpha = recordingAlpha), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444).copy(alpha = recordingAlpha))
                            )
                        }

                        Text(
                            text = "Recording...",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFEF4444).copy(alpha = recordingAlpha)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(DropdownColors.fieldBg)
                                .border(2.dp, DropdownColors.fieldBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = DropdownColors.textMuted,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        OutlinedTextField(
                            value = fileName,
                            onValueChange = { fileName = it },
                            placeholder = {
                                Text(
                                    "DAZN_signup_bug_video",
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Light,
                                    fontSize = 13.sp,
                                    color = DropdownColors.textDim
                                )
                            },
                            label = {
                                Text(
                                    "File name",
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Light,
                                    fontSize = 11.sp,
                                    color = DropdownColors.textMuted
                                )
                            },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = AppFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = DropdownColors.textPrimary
                            ),
                            trailingIcon = {
                                Text(
                                    ".mp4",
                                    fontFamily = AppFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Light,
                                    color = DropdownColors.textMuted,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            },
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                backgroundColor = DropdownColors.fieldBg,
                                focusedBorderColor = Color(0xFFEF4444),
                                unfocusedBorderColor = DropdownColors.fieldBorder,
                                cursorColor = DropdownColors.textPrimary,
                                focusedLabelColor = Color(0xFFEF4444),
                                unfocusedLabelColor = DropdownColors.textMuted
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val noSoundInteraction = remember { MutableInteractionSource() }
                            val isNoSoundHovered by noSoundInteraction.collectIsHoveredAsState()

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isNoSoundHovered) DropdownColors.rowHover else DropdownColors.fieldBg)
                                    .border(1.dp, DropdownColors.fieldBorder, RoundedCornerShape(8.dp))
                                    .hoverable(noSoundInteraction)
                                    .clickable { noSound = !noSound }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Checkbox(
                                    checked = noSound,
                                    onCheckedChange = { noSound = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFFFFEB3B),
                                        uncheckedColor = DropdownColors.fieldBorder,
                                        checkmarkColor = Color.Black
                                    ),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "No audio",
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = DropdownColors.textPrimary
                                )
                            }

                            val touchesInteraction = remember { MutableInteractionSource() }
                            val isTouchesHovered by touchesInteraction.collectIsHoveredAsState()

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isTouchesHovered) DropdownColors.rowHover else DropdownColors.fieldBg)
                                    .border(1.dp, DropdownColors.fieldBorder, RoundedCornerShape(8.dp))
                                    .hoverable(touchesInteraction)
                                    .clickable { showTouches = !showTouches }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Checkbox(
                                    checked = showTouches,
                                    onCheckedChange = { showTouches = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFFFFEB3B),
                                        uncheckedColor = DropdownColors.fieldBorder,
                                        checkmarkColor = Color.Black
                                    ),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Show touches",
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = DropdownColors.textPrimary
                                )
                            }
                        }
                    }
                }

                Divider(color = DropdownColors.divider)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DropdownColors.footerBg)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = if (isRecording) Arrangement.Center else Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isRecording) {
                        val stopInteraction = remember { MutableInteractionSource() }
                        val isStopHovered by stopInteraction.collectIsHoveredAsState()

                        Button(
                            onClick = onStopRecording,
                            interactionSource = stopInteraction,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (isStopHovered) Color(0xFFDC2626) else Color(0xFFEF4444),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Stop Recording",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        val recordInteraction = remember { MutableInteractionSource() }
                        val isRecordHovered by recordInteraction.collectIsHoveredAsState()

                        Button(
                            onClick = {
                                val sanitized = fileName.trim().ifEmpty { "screen_recording" }
                                onStartRecording(noSound, showTouches, sanitized)
                            },
                            interactionSource = recordInteraction,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (isRecordHovered) Color(0xFFDC2626) else Color(0xFFEF4444),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Start Recording",
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