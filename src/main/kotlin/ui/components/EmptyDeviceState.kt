package ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.components.theme.AppFontFamily
import ui.components.theme.DropdownColors

@Composable
fun emptyDeviceState(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(70.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .drawBehind {
                            val strokeWidth = 3.dp.toPx()
                            val radius = size.minDimension / 2 - strokeWidth / 2

                            drawCircle(
                                color = Color(0xFF243038),
                                radius = radius,
                                style = Stroke(width = 1.dp.toPx())
                            )

                            drawCircle(
                                color = Color(0xFF243038),
                                radius = radius * 0.65f,
                                style = Stroke(width = 1.dp.toPx())
                            )

                            rotate(radarRotation) {
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        0f to Color.Transparent,
                                        0.15f to Color(0xFFFFEB3B).copy(alpha = pulseAlpha),
                                        0.3f to Color.Transparent
                                    ),
                                    startAngle = 0f,
                                    sweepAngle = 120f,
                                    useCenter = true,
                                    size = androidx.compose.ui.geometry.Size(
                                        radius * 2,
                                        radius * 2
                                    ),
                                    topLeft = Offset(
                                        center.x - radius,
                                        center.y - radius
                                    )
                                )

                                drawArc(
                                    brush = Brush.sweepGradient(
                                        0f to Color.Transparent,
                                        0.1f to Color(0xFFFFEB3B).copy(alpha = pulseAlpha * 0.8f),
                                        0.2f to Color.Transparent
                                    ),
                                    startAngle = 0f,
                                    sweepAngle = 60f,
                                    useCenter = false,
                                    style = Stroke(
                                        width = strokeWidth,
                                        cap = StrokeCap.Round
                                    ),
                                    size = androidx.compose.ui.geometry.Size(
                                        radius * 2,
                                        radius * 2
                                    ),
                                    topLeft = Offset(
                                        center.x - radius,
                                        center.y - radius
                                    )
                                )
                            }

                            drawCircle(
                                color = Color(0xFFFFEB3B).copy(alpha = pulseAlpha * 0.5f),
                                radius = 3.dp.toPx()
                            )
                        }
                )

                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = DropdownColors.textMuted,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "Waiting for device...",
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = DropdownColors.textPrimary
            )

            Text(
                text = "Connect an Android device",
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Light,
                fontSize = 12.sp,
                color = DropdownColors.textMuted
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .drawBehind {
                        val centerX = size.width / 2
                        val topY = 0f
                        val midY = size.height * 0.5f
                        val bottomY = size.height
                        val leftX = size.width * 0.25f
                        val rightX = size.width * 0.75f
                        val lineColor = Color(0xFF3A4A54)
                        val lineWidth = 1.5.dp.toPx()
                        val dashEffect = PathEffect.dashPathEffect(
                            floatArrayOf(6.dp.toPx(), 4.dp.toPx()),
                            0f
                        )

                        drawLine(
                            color = lineColor,
                            start = Offset(centerX, topY),
                            end = Offset(centerX, midY),
                            strokeWidth = lineWidth,
                            pathEffect = dashEffect
                        )

                        drawLine(
                            color = lineColor,
                            start = Offset(leftX, midY),
                            end = Offset(rightX, midY),
                            strokeWidth = lineWidth,
                            pathEffect = dashEffect
                        )

                        drawLine(
                            color = lineColor,
                            start = Offset(leftX, midY),
                            end = Offset(leftX, bottomY),
                            strokeWidth = lineWidth,
                            pathEffect = dashEffect
                        )

                        drawLine(
                            color = lineColor,
                            start = Offset(rightX, midY),
                            end = Offset(rightX, bottomY),
                            strokeWidth = lineWidth,
                            pathEffect = dashEffect
                        )

                        val arrowSize = 4.dp.toPx()

                        drawLine(
                            color = lineColor,
                            start = Offset(leftX - arrowSize, bottomY - arrowSize),
                            end = Offset(leftX, bottomY),
                            strokeWidth = lineWidth
                        )
                        drawLine(
                            color = lineColor,
                            start = Offset(leftX + arrowSize, bottomY - arrowSize),
                            end = Offset(leftX, bottomY),
                            strokeWidth = lineWidth
                        )

                        drawLine(
                            color = lineColor,
                            start = Offset(rightX - arrowSize, bottomY - arrowSize),
                            end = Offset(rightX, bottomY),
                            strokeWidth = lineWidth
                        )
                        drawLine(
                            color = lineColor,
                            start = Offset(rightX + arrowSize, bottomY - arrowSize),
                            end = Offset(rightX, bottomY),
                            strokeWidth = lineWidth
                        )
                    }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DropdownColors.fieldBg)
                        .border(1.dp, DropdownColors.fieldBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Usb,
                            contentDescription = null,
                            tint = Color(0xFFFFEB3B),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "USB Connection",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = DropdownColors.textPrimary
                        )
                    }

                    instructionStep("1", "Enable Developer Options on your device")
                    instructionStep("2", "Enable USB Debugging in Developer Options")
                    instructionStep("3", "Connect device via USB cable")
                    instructionStep("4", "Tap 'Allow' on the USB debugging prompt")
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DropdownColors.fieldBg)
                        .border(1.dp, DropdownColors.fieldBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "WiFi (Android 11+)",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = DropdownColors.textPrimary
                        )
                    }

                    instructionStep("1", "Enable Wireless Debugging in Developer Options")
                    instructionStep("2", "Tap 'Pair device with pairing code'")
                    instructionStep("3", "Run: adb pair <ip>:<port>")
                    instructionStep("4", "Enter the pairing code shown on device")
                    instructionStep("5", "Run: adb connect <ip>:<port>")
                }
            }
        }
    }
}

@Composable
private fun instructionStep(
    number: String,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFEB3B).copy(alpha = 0.15f))
                .border(1.dp, Color(0xFFFFEB3B).copy(alpha = 0.3f), CircleShape)
                .wrapContentSize(Alignment.Center)
        ) {
            Text(
                text = number,
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                color = Color(0xFFFFEB3B),
                lineHeight = 8.sp
            )
        }
        Text(
            text = text,
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 10.sp,
            color = DropdownColors.textMuted,
            lineHeight = 13.sp
        )
    }
}