package ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import commands.RegionGateway
import commands.regions
import commands.toFlagEmoji
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ui.components.theme.AppFontFamily
import ui.components.theme.DropdownColors
import ui.components.theme.DropdownColors.green
import ui.components.theme.MenuColors.dangerRed
import ui.components.theme.gradientColors
import utils.helpers.CancellableProcess
import utils.helpers.connectGateway
import utils.helpers.disconnectVPN
import utils.helpers.waveFill


@Composable
fun vpnButton(
    modifier: Modifier = Modifier,
    deviceId: String,
    deviceName: String = "",
    enabled: Boolean = true,
    onCommandExecuted: (String, Color?) -> Unit,
    onCommandReplaced: (String, Color?) -> Unit,
) {
    var isHoveringSettings by remember { mutableStateOf(false) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var connectedGateway by remember { mutableStateOf<String?>(null) }
    var connectingGateway by remember { mutableStateOf<String?>(null) }
    var disconnectingGateway by remember { mutableStateOf<String?>(null) }
    var connectingJob by remember { mutableStateOf<Job?>(null) }

    var showPureDomeDialog by remember { mutableStateOf(false) }
    val pureDomeGate = rememberPureDomeGate()

    val coroutineScope = rememberCoroutineScope()
    val maestroGate = rememberMaestroGate()
    var showMaestroDialog by remember { mutableStateOf(false) }

    val saturation by animateFloatAsState(
        targetValue = if (isHoveringSettings) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "saturation"
    )

    val imageColorMatrix = remember(saturation) {
        ColorFilter.colorMatrix(
            ColorMatrix().apply {
                setToSaturation(saturation)
            }
        )
    }

    val borderAlpha by animateFloatAsState(
        targetValue = if (isHoveringSettings) 1f else 0.2f,
        animationSpec = tween(durationMillis = 300),
        label = "borderAlpha"
    )

    val regionGateways: List<RegionGateway> = remember {
        regions.map { gateway ->
            RegionGateway(
                name = gateway.name,
                index = gateway.index,
                url = "dazn-${gateway.name.lowercase()}.puredomegateways.com",
                flagEmoji = gateway.name.toFlagEmoji()
            )
        }
    }

    val sortedGateways = remember(regionGateways, connectedGateway) {
        val connected = regionGateways.filter { it.name == connectedGateway }
        val others = regionGateways.filter { it.name != connectedGateway }
        connected + others
    }

    val filteredGateways = remember(searchQuery, sortedGateways) {
        if (searchQuery.isBlank()) {
            sortedGateways
        } else {
            sortedGateways.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.url.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(modifier = modifier) {
        IconButton(
            onClick = {
                maestroGate.ensureInstalled(
                    onMissing = {
                        showMaestroDialog  = true
                        onCommandExecuted(
                            "Maestro CLI not found — install it to use VPN region switching ⚠️",
                            Color(0xFFF59E0B)
                        )
                    }
                ) {
                    pureDomeGate.ensureInstalled(
                        deviceId = deviceId,
                        onMissing = {
                            showPureDomeDialog = true
                            onCommandExecuted(
                                "PureDome app NOT installed on $deviceName ⚠️",
                                Color(0xFFF59E0B)
                            )
                        }
                    ) {
                        isMenuExpanded = true
                        searchQuery = ""
                    }
                }
            },
            enabled = enabled,
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

                    drawCircle(
                        brush = gradient,
                        radius = size.minDimension / 2,
                        alpha = borderAlpha,
                        style = Stroke(width = borderWidth)
                    )
                }
        ) {
            Image(
                painter = painterResource("icons/puredome.png"),
                contentDescription = "VPN Settings",
                colorFilter = imageColorMatrix,
                modifier = Modifier
                    .size(28.dp)
                    .alpha(if (isHoveringSettings) 1f else 0.6f)
            )
        }

        if (isMenuExpanded) {
            Dialog(
                onDismissRequest = { isMenuExpanded = false }
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DropdownColors.cardBg,
                    elevation = 16.dp,
                    modifier = Modifier
                        .width(500.dp)
                        .heightIn(max = 450.dp)
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
                                    text = "Select VPN Region",
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = DropdownColors.textPrimary
                                )
                                IconButton(
                                    onClick = { isMenuExpanded = false },
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
                                text = "Connect to a PureDome VPN gateway",
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
                                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
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
                                    text = "Make sure you are logged in to your PureDome VPN app.",
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = Color(0xFFF59E0B)
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        "Search regions...",
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Light,
                                        fontSize = 13.sp,
                                        color = DropdownColors.textDim
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = DropdownColors.textMuted
                                    )
                                },
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = DropdownColors.textPrimary,
                                    backgroundColor = DropdownColors.fieldBg,
                                    cursorColor = DropdownColors.accentYellow,
                                    focusedBorderColor = DropdownColors.accentYellow,
                                    unfocusedBorderColor = DropdownColors.fieldBorder
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        Divider(color = DropdownColors.divider)

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (filteredGateways.isEmpty()) {
                                item {
                                    Text(
                                        text = "No regions found",
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Light,
                                        fontSize = 13.sp,
                                        color = DropdownColors.textDim,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                items(filteredGateways) { gateway ->
                                    regionMenuItem(
                                        gateway = gateway,
                                        isConnected = connectedGateway == gateway.name,
                                        isConnecting = connectingGateway == gateway.name,
                                        isDisconnecting = disconnectingGateway == gateway.name,
                                        isBusy = connectingGateway != null || disconnectingGateway != null,
                                        onConnect = {
                                            connectingJob = coroutineScope.launch {
                                                connectingGateway = gateway.name
                                                onCommandExecuted(
                                                    "VPN: Connecting to ${gateway.name}...",
                                                    Color(0xFFA855F7)
                                                )

                                                try {
                                                    val result = withContext(Dispatchers.IO) {
                                                        connectGateway(gateway.name)
                                                    }

                                                    when {
                                                        result.success -> {
                                                            connectedGateway = gateway.name
                                                            onCommandReplaced(
                                                                "${result.output}\nVPN: Connected to ${gateway.name} ✅",
                                                                green
                                                            )
                                                        }

                                                        CancellableProcess.wasCancelled() -> {
                                                            onCommandReplaced(
                                                                "VPN: Connection to ${gateway.name} cancelled 🛑",
                                                                Color(0xFFF59E0B)
                                                            )
                                                        }

                                                        else -> {
                                                            onCommandReplaced(
                                                                "${result.output}\nVPN: Failed to connect to ${gateway.name} ❌",
                                                                dangerRed
                                                            )
                                                        }
                                                    }
                                                } catch (e: CancellationException) {
                                                    onCommandReplaced(
                                                        "VPN: Connection to ${gateway.name} cancelled 🛑",
                                                        Color(0xFFF59E0B)
                                                    )
                                                    throw e
                                                } finally {
                                                    connectingGateway = null
                                                    connectingJob = null
                                                }
                                            }
                                        },
                                        onCancel = {
                                            connectingJob?.cancel()
                                        },
                                        onDisconnect = {
                                            coroutineScope.launch {
                                                disconnectingGateway = gateway.name
                                                try {
                                                    withContext(Dispatchers.IO) {
                                                        disconnectVPN(deviceId)
                                                    }
                                                    connectedGateway = null
                                                    onCommandExecuted(
                                                        "VPN: ${gateway.name} disconnected. You can now connect to a different region.",
                                                        green
                                                    )
                                                } finally {
                                                    disconnectingGateway = null
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }

                        Divider(color = DropdownColors.divider)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .background(DropdownColors.footerBg)
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            val closeInteraction = remember { MutableInteractionSource() }
                            val isCloseHovered by closeInteraction.collectIsHoveredAsState()

                            TextButton(
                                onClick = { isMenuExpanded = false },
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
        pureDomeMissingDialog(
            showDialog = showPureDomeDialog,
            deviceId = deviceId,
            deviceName = deviceName,
            isChecking = pureDomeGate.isChecking,
            onRecheck = {
                pureDomeGate.ensureInstalled(
                    deviceId = deviceId,
                    onMissing = {
                        showPureDomeDialog = true
                        onCommandExecuted(
                            "PureDome app NOT installed on ${deviceName.ifBlank { deviceId }} ⚠️",
                            Color(0xFFF59E0B)
                        )
                    }
                ) {
                    showPureDomeDialog = false
                    isMenuExpanded = true
                    searchQuery = ""
                }
            },
            onDismiss = { showPureDomeDialog = false }
        )

        if (maestroGate.isChecking) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp).align(Alignment.Center),
                color = DropdownColors.accentYellow,
                strokeWidth = 2.dp
            )
        }
        if (showMaestroDialog) {
            maestroNotInstalledDialog(
                onRecheck = {
                    maestroGate.ensureInstalled(
                        onMissing = { }
                    ) {
                        showMaestroDialog = false
                        pureDomeGate.ensureInstalled(
                            deviceId = deviceId,
                            onMissing = { showPureDomeDialog = true }
                        ) {
                            isMenuExpanded = true
                            searchQuery = ""
                        }
                    }
                },
                onDismiss = { showMaestroDialog = false }
            )
        }


    }

}

@Composable
private fun regionMenuItem(
    gateway: RegionGateway,
    isConnected: Boolean,
    isConnecting: Boolean,
    isDisconnecting: Boolean,
    isBusy: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onCancel: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isConnected -> green.copy(alpha = 0.1f)
                    isHovered -> DropdownColors.rowHover
                    else -> Color.Transparent
                }
            )
            .border(
                1.dp,
                when {
                    isConnected -> green.copy(alpha = 0.3f)
                    isHovered -> DropdownColors.fieldBorderHover
                    else -> Color.Transparent
                },
                RoundedCornerShape(8.dp)
            )
            .hoverable(interaction)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = gateway.flagEmoji,
                fontSize = 22.sp,
                modifier = Modifier.padding(end = 12.dp)
            )

            Column {
                Text(
                    text = gateway.name,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isConnected) green else DropdownColors.textPrimary
                )
                Text(
                    text = gateway.url,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Light,
                    fontSize = 11.sp,
                    color = DropdownColors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        val buttonInteraction = remember { MutableInteractionSource() }
        val isButtonHovered by buttonInteraction.collectIsHoveredAsState()
        val showSpinner = isConnecting || isDisconnecting
        val showCancel = isConnecting && isButtonHovered
        val buttonShape = RoundedCornerShape(6.dp)

        Button(
            onClick = {
                when {
                    isConnecting -> onCancel()
                    isDisconnecting -> Unit
                    isConnected -> onDisconnect()
                    else -> onConnect()
                }
            },
            enabled = isConnecting || !isBusy,
            interactionSource = buttonInteraction,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = when {
                    showCancel -> Color.Transparent
                    showSpinner -> DropdownColors.fieldBg
                    isConnected -> green
                    isButtonHovered -> DropdownColors.accentYellow
                    else -> DropdownColors.fieldBg
                },
                contentColor = when {
                    showCancel -> Color.White
                    isConnected -> Color.White
                    isButtonHovered -> Color.Black
                    else -> DropdownColors.textPrimary
                },
                disabledBackgroundColor = DropdownColors.fieldBg,
                disabledContentColor = DropdownColors.textDim
            ),
            shape = buttonShape,
            border = BorderStroke(
                1.dp,
                when {
                    showCancel -> dangerRed
                    showSpinner -> DropdownColors.fieldBorder
                    isConnected -> green
                    isButtonHovered -> DropdownColors.accentYellow
                    else -> DropdownColors.fieldBorder
                }
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
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        waveFill(
                            active = showCancel,
                            baseColor = dangerRed,
                            shape = buttonShape
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    showCancel -> Text(
                        text = "Cancel",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false
                    )

                    showSpinner -> CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = DropdownColors.textMuted,
                        strokeWidth = 2.dp
                    )

                    else -> Text(
                        text = if (isConnected) "Disconnect" else "Connect",
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