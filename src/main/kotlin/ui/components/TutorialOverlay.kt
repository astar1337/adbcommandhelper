package ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.components.theme.AppFontFamily
import ui.components.theme.DropdownColors

@Composable
fun tutorialOverlay(
    target: Rect,
    stepIndex: Int,
    stepCount: Int,
    text: String,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
    tooltipAtTop: Boolean
) {
    val isLastStep = stepIndex == stepCount - 1

    val pulseTransition = rememberInfiniteTransition()
    val pulseRadius by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        )
    )
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .background(Color.Black.copy(alpha = 0.7f))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(stepIndex) {
                    detectTapGestures { tap ->
                        if (target.contains(tap)) {
                            if (isLastStep) onFinish() else onNext()
                        }
                    }
                }
        ) {
            drawRoundRect(
                color = Color(0xFFFFEB3B).copy(alpha = pulseAlpha),
                topLeft = Offset(
                    target.left - pulseRadius - 4.dp.toPx(),
                    target.top - pulseRadius - 4.dp.toPx()
                ),
                size = Size(
                    target.width + (pulseRadius + 4.dp.toPx()) * 2,
                    target.height + (pulseRadius + 4.dp.toPx()) * 2
                ),
                cornerRadius = CornerRadius(12.dp.toPx() + pulseRadius),
                style = Stroke(width = 2.dp.toPx())
            )
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(target.left - 2.dp.toPx(), target.top - 2.dp.toPx()),
                size = Size(target.width + 4.dp.toPx(), target.height + 4.dp.toPx()),
                cornerRadius = CornerRadius(12.dp.toPx()),
                blendMode = BlendMode.Clear,
            )

            drawRoundRect(
                color = Color(0xFFFFEB3B),
                topLeft = Offset(target.left - 2.dp.toPx(), target.top - 2.dp.toPx()),
                size = Size(target.width + 4.dp.toPx(), target.height + 4.dp.toPx()),
                cornerRadius = CornerRadius(12.dp.toPx()),
                style = Stroke(width = 4.dp.toPx())
            )
        }

        Column(
            modifier = Modifier
                .align(if (tooltipAtTop) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .widthIn(max = 400.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DropdownColors.cardBg)
                .border(1.dp, DropdownColors.border, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DropdownColors.dialogHeaderBg)
                    .padding(16.dp, 12.dp, 16.dp, 10.dp)
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
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = Color(0xFFFFEB3B),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Tutorial",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DropdownColors.textPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFFEB3B).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFFFEB3B).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${stepIndex + 1} / $stepCount",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color(0xFFFFEB3B)
                        )
                    }
                }
            }

            Divider(color = DropdownColors.divider)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = text,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = DropdownColors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Divider(color = DropdownColors.divider)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DropdownColors.footerBg)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val skipInteraction = remember { MutableInteractionSource() }
                val isSkipHovered by skipInteraction.collectIsHoveredAsState()

                TextButton(
                    onClick = onSkip,
                    interactionSource = skipInteraction,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (isSkipHovered) DropdownColors.textSecondary else DropdownColors.textMuted
                    )
                ) {
                    Text(
                        "Skip",
                        fontFamily = AppFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                val nextInteraction = remember { MutableInteractionSource() }
                val isNextHovered by nextInteraction.collectIsHoveredAsState()

                Button(
                    onClick = { if (isLastStep) onFinish() else onNext() },
                    interactionSource = nextInteraction,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isNextHovered) Color(0xFFFDD835) else Color(0xFFFFEB3B),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp)
                ) {
                    Text(
                        if (isLastStep) "Got it!" else "Next",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isLastStep) Icons.Default.CheckCircle else Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}