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
import androidx.compose.material.icons.filled.SettingsSuggest
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.components.theme.DropdownColors
import ui.components.theme.AppFontFamily
import ui.components.theme.gradientColorsIcons

@Composable
fun dropDownEnv(
    selectedEnvSource: String,
    commandOptions: Map<String, String>,
    onCommandSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    hasSelectedEnv: Boolean = false
) {
    var dropdownWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    var isExpanded by remember { mutableStateOf(false) }

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
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SettingsSuggest,
                    contentDescription = "Environment",
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
                    text = "Select Environment",
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = DropdownColors.textPrimary
                )
                if (!hasSelectedEnv && commandOptions.isNotEmpty()) {
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
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = pulseAlpha))
                    )
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
                        .clickable(enabled = commandOptions.isNotEmpty()) {
                            isExpanded = true
                        }
                        .onGloballyPositioned { coordinates ->
                            dropdownWidth = with(density) {
                                coordinates.size.width.toDp()
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    Text(
                        text = selectedEnvSource,
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = when {
                            selectedEnvSource.startsWith("Select") -> DropdownColors.textPlaceholder
                            else -> DropdownColors.textPrimary
                        },
                        modifier = Modifier.weight(1f)
                    )
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
                                    text = "No environments available",
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = DropdownColors.textDim
                                )
                            }
                        } else {
                            commandOptions.forEach { (label, _) ->
                                val itemInteraction = remember { MutableInteractionSource() }
                                val itemHovered by itemInteraction.collectIsHoveredAsState()

                                DropdownMenuItem(
                                    onClick = {
                                        onCommandSelected(label)
                                        isExpanded = false
                                    },
                                    interactionSource = itemInteraction,
                                    modifier = Modifier
                                        .background(
                                            if (itemHovered) DropdownColors.rowHover else Color.Transparent
                                        )
                                ) {
                                    Text(
                                        text = label,
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = DropdownColors.textPrimary
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