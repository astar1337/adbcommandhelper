package ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import commands.moreSettings
import kotlinx.coroutines.launch
import ui.components.theme.AppFontFamily
import utils.helpers.AdbDevices.runCommandFromMap
import kotlin.math.roundToInt

@Composable
fun sideNavigationBar(
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    deviceId: String,
    onOKClick: () -> Unit,
    onToggleClick: () -> Unit,
    showToggleButton: Boolean = true,
    isNavBarVisible: Boolean = false
) {
    val density = LocalDensity.current
    var offsetX by remember { mutableStateOf(with(density) { (-50).dp.toPx() }) }
    var offsetY by remember { mutableStateOf(360f) }
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var containerWidth by remember { mutableStateOf(0f) }
    var containerHeight by remember { mutableStateOf(0f) }

    LaunchedEffect(isDragging) {
        if (isDragging && isNavBarVisible) {
            onToggleClick()
        }
    }

    val expandProgress by animateFloatAsState(
        targetValue = if (isNavBarVisible) 1f else 0f,
        animationSpec = tween(250)
    )

    val rotation by animateFloatAsState(
        targetValue = if (isNavBarVisible) 45f else 0f,
        animationSpec = tween(200)
    )

    val buttonSpacing = 62f

    if (showToggleButton) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    containerWidth = coordinates.size.width.toFloat()
                    containerHeight = coordinates.size.height.toFloat()
                }


        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .padding(end = 16.dp)
                    .size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                if (expandProgress > 0.01f) {
                    navActionButton(
                        label = "Back",
                        labelPosition = LabelPosition.LEFT,
                        icon = Icons.Default.KeyboardDoubleArrowLeft,
                        xOffset = -(buttonSpacing + 5f) * expandProgress,
                        yOffset = 0f,
                        onClick = {
                            coroutineScope.launch {
                                val command = moreSettings(deviceId)["Back"] ?: return@launch
                                runCommandFromMap(command)
                                onBackClick()
                            }
                        }
                    )

                    navActionButton(
                        icon = Icons.Default.Home,
                        xOffset = 0f,
                        label = "Home",
                        yOffset = -buttonSpacing * expandProgress,
                        onClick = {
                            coroutineScope.launch {
                                val command = moreSettings(deviceId)["Home"] ?: return@launch
                                runCommandFromMap(command)
                                onHomeClick()
                            }
                        }
                    )

                    navActionButton(
                        icon = Icons.Default.ViewCarousel,
                        xOffset = 0f,
                        labelPosition = LabelPosition.BELOW,
                        label = "Overview",
                        yOffset = buttonSpacing * expandProgress,
                        onClick = {
                            coroutineScope.launch {
                                val command = moreSettings(deviceId)["Overview"] ?: return@launch
                                runCommandFromMap(command)
                                onHomeClick()
                            }
                        }
                    )

                    navActionButton(
                        icon = Icons.Default.Check,
                        xOffset = buttonSpacing * expandProgress,
                        labelPosition = LabelPosition.RIGHT,
                        label = "OK",
                        yOffset = 0f,
                        onClick = {
                            coroutineScope.launch {
                                val command = moreSettings(deviceId)["OK"] ?: return@launch
                                runCommandFromMap(command)
                                onOKClick()
                            }
                        }
                    )
                }

                val centerInteraction = remember { MutableInteractionSource() }
                val isCenterHovered by centerInteraction.collectIsHoveredAsState()

                val dragScale by animateFloatAsState(
                    targetValue = if (isDragging) 1.15f else 1f,
                    animationSpec = tween(200)
                )

                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .graphicsLayer {
                                scaleX = dragScale
                                scaleY = dragScale
                            }
                            .hoverable(centerInteraction)
                            .pointerInput(containerWidth, containerHeight) {
                                val padding = 80.dp.toPx()
                                val buttonSize = 44.dp.toPx()
                                val defaultCenterX = containerWidth - with(density) { 16.dp.toPx() } - with(density) { 80.dp.toPx() }
                                val defaultCenterY = containerHeight / 2

                                detectDragGestures(
                                    onDragStart = { isDragging = true },
                                    onDragEnd = { isDragging = false },
                                    onDragCancel = { isDragging = false },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val newX = offsetX + dragAmount.x
                                        val newY = offsetY + dragAmount.y

                                        val minX = -(defaultCenterX - padding)
                                        val maxX = (containerWidth - defaultCenterX - buttonSize - padding).coerceAtLeast(0f)
                                        val minY = -(defaultCenterY - padding)
                                        val maxY = containerHeight - defaultCenterY - padding

                                        offsetX = newX.coerceIn(minX, maxX)
                                        offsetY = newY.coerceIn(minY, maxY)
                                    }
                                )
                            }
                            .clickable(
                                interactionSource = centerInteraction,
                                indication = null,
                                onClick = { onToggleClick() }
                            ),
                        shape = CircleShape,
                        elevation = if (isDragging) 16.dp else 8.dp,
                        color = when {
                            isDragging -> Color(0xFFFDD835)
                            isCenterHovered -> Color(0xFFFDD835)
                            else -> Color(0xFFFFEB3B)
                        }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (isDragging) Icons.Default.OpenWith else Icons.Default.Add,
                                contentDescription = "Toggle navigation",
                                tint = Color.Black,
                                modifier = Modifier
                                    .size(24.dp)
                                    .rotate(if (isDragging) 0f else rotation)
                            )
                        }
                    }
                }
            }
        }
    }
}
enum class LabelPosition {
    ABOVE, BELOW, LEFT, RIGHT
}

@Composable
private fun BoxScope.navActionButton(
    icon: ImageVector,
    label: String,
    xOffset: Float,
    yOffset: Float,
    labelPosition: LabelPosition = LabelPosition.ABOVE,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.7f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isPressed -> Color(0xFFFFEB3B)
            isHovered -> Color(0xFF1A2830)
            else -> Color(0xFF0C161C)
        },
        animationSpec = tween(150)
    )

    val iconColor by animateColorAsState(
        targetValue = when {
            isPressed -> Color.Black
            isHovered -> Color(0xFFFFEB3B)
            else -> Color(0xFF8899AA)
        },
        animationSpec = tween(150)
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isPressed -> Color(0xFFFFEB3B)
            isHovered -> Color(0xFFFFEB3B)
            else -> Color.White.copy(alpha = 0.3f)
        },
        animationSpec = tween(150)
    )

    val labelColor by animateColorAsState(
        targetValue = when {
            isPressed -> Color(0xFFFFEB3B)
            isHovered -> Color(0xFFFFEB3B)
            else -> Color(0xFF8899AA)
        },
        animationSpec = tween(150)
    )

    val isHorizontal = labelPosition == LabelPosition.LEFT || labelPosition == LabelPosition.RIGHT

    val content: @Composable () -> Unit = {
        // Label
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFF0C161C).copy(alpha = 0.9f),
            border = BorderStroke(1.dp, borderColor),
            elevation = 4.dp
        ) {
            Text(
                text = label,
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = labelColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }

    val iconContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .size(36.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(backgroundColor)
                .border(3.dp, borderColor, CircleShape)
                .hoverable(interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = {
                        isPressed = true
                        onClick()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    if (isHorizontal) {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset(xOffset.dp.roundToPx(), yOffset.dp.roundToPx()) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (labelPosition == LabelPosition.LEFT) {
                content()
                iconContent()
            } else {
                iconContent()
                content()
            }
        }
    } else {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset(xOffset.dp.roundToPx(), yOffset.dp.roundToPx()) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (labelPosition == LabelPosition.ABOVE) {
                content()
                iconContent()
            } else {
                iconContent()
                content()
            }
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(200)
            isPressed = false
        }
    }
}