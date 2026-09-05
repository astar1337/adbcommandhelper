package ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.components.theme.AppFontFamily
import ui.components.theme.DropdownColors

@Composable
fun skeletonQuickActions(
    modifier: Modifier = Modifier,
    isAppSelected: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition()
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val pulseTransition = rememberInfiniteTransition()
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1E2A32),
            Color(0xFF2A3A44),
            Color(0xFF1E2A32)
        ),
        start = Offset(shimmerOffset * 300f, 0f),
        end = Offset(shimmerOffset * 300f + 300f, 0f)
    )

    val promptText = if (isAppSelected) {
        "Select an environment to continue"
    } else {
        "Select a DAZN or FOXTEL app to continue"
    }

    Card(
        modifier = modifier.height(125.dp),
        elevation = 8.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = DropdownColors.cardBg,
        border = BorderStroke(1.dp, DropdownColors.border)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Skeleton background
            Column(
                modifier = Modifier.padding(16.dp).alpha(0.4f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(6) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(shimmerBrush)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.alpha(pulseAlpha)
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = Color(0xFF8899AA),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = promptText,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 24.sp,
                    color = Color(0xFF8899AA)
                )
            }
        }
    }
}