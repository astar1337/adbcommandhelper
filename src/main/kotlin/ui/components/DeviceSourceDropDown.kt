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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import commands.Emoji.EMULATOR_COMPUTER
import commands.Emoji.PHONE_MOBILE
import ui.components.theme.AppFontFamily
import utils.helpers.*
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import ui.components.theme.gradientColorsIcons
import ui.components.theme.DropdownColors
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import utils.helpers.AdbDevices
import utils.helpers.getAirplaneModeState
import utils.helpers.getWifiState
import utils.helpers.takeScreenshotToClipboard
import utils.helpers.AppConfig.adbPath
import commands.Emoji.TV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun deviceSourceDropDown(
    selectedHardwareSource: String,
    commandOptions: List<String>,
    onCommandSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    onCommandExecuted: (String) -> Unit = {},
) {
    var dropdownWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    var isExpanded by remember { mutableStateOf(false) }
    var showDeviceInfoDialog by remember { mutableStateOf(false) }
    var hasFocus by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    var showDeviceMenu by remember { mutableStateOf(false) }
    var isWifiOn by remember { mutableStateOf(false) }
    var isAirplaneModeOn by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var isDeviceMenuLoading by remember { mutableStateOf(false) }
    var showVideoDialog by remember { mutableStateOf(false) }
    var isVideoRecording by remember { mutableStateOf(false) }
    var showScrcpyInstallDialog by remember { mutableStateOf(false) }

    val deviceInfoMap = remember { mutableStateMapOf<String, DeviceInfoCache.DeviceInfo>() }
    var deviceIp by remember { mutableStateOf("") }

    val isSelectedTv = deviceInfoMap[selectedHardwareSource]?.deviceType == DeviceType.TV

    var actionSerial by remember { mutableStateOf<String?>(null) }
    var action by remember { mutableStateOf(ConnectionAction.NONE) }

    LaunchedEffect(commandOptions) {
        // drop devices that are no longer connected
        deviceInfoMap.keys.retainAll(commandOptions.toSet())

        commandOptions
            .filterNot { it.startsWith("Select", true) || it.contains("No devices", true) }
            .filter { it !in deviceInfoMap }
            .forEach { id ->
                val info = DeviceInfoCache.fetchAndCache(id)
                if (info.isValid) deviceInfoMap[id] = info
            }
    }
    LaunchedEffect(showDeviceInfoDialog, selectedHardwareSource) {
        if (showDeviceInfoDialog) {
            deviceIp = "Loading..."
            deviceIp = withContext(Dispatchers.IO) { getDeviceWifiIp(selectedHardwareSource) }
        }
    }
    val disconnectedDevices = AdbConnectionManager.disconnected
        .filterNot { it.serial in commandOptions }

    fun snapshotOf(serial: String): DisconnectedDevice {
        val info = deviceInfoMap[serial]
        return DisconnectedDevice(
            serial = serial,
            label = info?.name?.ifBlank { null } ?: serial,
            sdkRelease = info?.sdkRelease.orEmpty(),
            deviceType = info?.deviceType ?: DeviceType.MOBILE
        )
    }


    fun deviceLabel(deviceId: String): String =
        deviceInfoMap[deviceId]?.name?.ifEmpty { null } ?: deviceId

    fun formatDeviceDisplay(deviceId: String): String {
        if (deviceId.startsWith("Select", ignoreCase = true)) return deviceId
        if (deviceId.contains("No devices", ignoreCase = true)) return deviceId

        val info = deviceInfoMap[deviceId] ?: return deviceId

        val emoji = when {
            info.deviceType == DeviceType.TV -> TV
            deviceId.startsWith("emulator", ignoreCase = true) -> EMULATOR_COMPUTER
            else -> PHONE_MOBILE
        }
        return "(AND ${info.sdkRelease}) $emoji ${deviceLabel(deviceId)}"
    }

    val displayText = when {
        commandOptions.size == 1 -> commandOptions.first()
        selectedHardwareSource.startsWith("Select") -> "Select a device..."
        else -> selectedHardwareSource
    }
    LaunchedEffect(showDeviceMenu, selectedHardwareSource, isSelectedTv) {
        if (!showDeviceMenu || selectedHardwareSource.isEmpty() || selectedHardwareSource.startsWith("Select")) {
            return@LaunchedEffect
        }
        if (isSelectedTv) {
            isDeviceMenuLoading = false
            return@LaunchedEffect
        }

        isDeviceMenuLoading = true
        kotlinx.coroutines.delay(200)
        val wifi = withContext(Dispatchers.IO) { getWifiState(selectedHardwareSource) }
        val airplane = withContext(Dispatchers.IO) { getAirplaneModeState(selectedHardwareSource) }
        isWifiOn = wifi
        isAirplaneModeOn = airplane
        isDeviceMenuLoading = false
    }

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
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = "Device",
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
                        text = "Select your device",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = DropdownColors.textPrimary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (commandOptions.isNotEmpty() && !selectedHardwareSource.startsWith("Select")) {
                        TooltipArea(
                            tooltip = {
                                Surface(
                                    color = Color(0xFF1A1A1A),
                                    shape = RoundedCornerShape(6.dp),
                                    elevation = 4.dp
                                ) {
                                    Text(
                                        text = "Device Information ${if (deviceInfoMap[selectedHardwareSource]?.deviceType == DeviceType.TV) TV else PHONE_MOBILE}",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontFamily = AppFontFamily,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            },
                            delayMillis = 500
                        ) {
                            deviceHeaderIconButton(
                                icon = Icons.Default.PermDeviceInformation,
                                contentDescription = "Device Information",
                                onClick = { showDeviceInfoDialog = true }
                            )
                        }

                        Box {
                            TooltipArea(
                                tooltip = {
                                    Surface(
                                        color = Color(0xFF1A1A1A),
                                        shape = RoundedCornerShape(6.dp),
                                        elevation = 4.dp
                                    ) {
                                        Text(
                                            "Device Options",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            fontFamily = AppFontFamily,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
                                },
                                delayMillis = 500
                            ) {
                                deviceHeaderIconButton(
                                    icon = Icons.Default.Menu,
                                    contentDescription = "Device menu",
                                    onClick = { showDeviceMenu = true }
                                )
                            }

                            MaterialTheme(
                                colors = MaterialTheme.colors.copy(surface = DropdownColors.dropdownBg)
                            ) {
                                DropdownMenu(
                                    expanded = showDeviceMenu,
                                    onDismissRequest = { showDeviceMenu = false },
                                    modifier = Modifier
                                        .width(300.dp)
                                        .border(1.dp, DropdownColors.dropdownBorder, RoundedCornerShape(8.dp))
                                ) {
                                    Text(
                                        "DEVICE OPTIONS",
                                        fontFamily = AppFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DropdownColors.textMuted,
                                        letterSpacing = 0.8.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                    Divider(
                                        color = DropdownColors.divider,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )

                                    if (isDeviceMenuLoading) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                color = DropdownColors.textMuted,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                "Checking device state...",
                                                fontFamily = AppFontFamily,
                                                fontWeight = FontWeight.Light,
                                                fontSize = 12.sp,
                                                color = DropdownColors.textMuted
                                            )
                                        }
                                    } else {
                                        if (!isSelectedTv) {
                                            deviceMenuItem(
                                                label = if (isWifiOn) "Turn OFF WiFi" else "Turn ON WiFi",
                                                statusColor = if (isWifiOn) Color(0xFFEF4444) else Color(0xFF22C55E),
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val action = if (isWifiOn) "disabled" else "enabled"
                                                        val cmd =
                                                            "$adbPath  -s $selectedHardwareSource shell svc wifi ${if (isWifiOn) "disable" else "enable"}"
                                                        AdbDevices.runCommandFromMap(cmd)
                                                        isWifiOn = !isWifiOn
                                                        onCommandExecuted(
                                                            "WiFi $action on ${
                                                                deviceLabel(
                                                                    selectedHardwareSource
                                                                )
                                                            } at ${
                                                                java.time.LocalDateTime.now().format(
                                                                    java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                                                                )
                                                            }"
                                                        )
                                                    }
                                                }
                                            )

                                            deviceMenuItem(
                                                label = if (isAirplaneModeOn) "Turn OFF Airplane Mode" else "Turn ON Airplane Mode",
                                                statusColor = if (isAirplaneModeOn) Color(0xFFEF4444) else Color(
                                                    0xFF22C55E
                                                ),
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val action = if (isAirplaneModeOn) "disabled" else "enabled"
                                                        val cmd =
                                                            "$adbPath -s $selectedHardwareSource shell cmd connectivity airplane-mode ${if (isAirplaneModeOn) "disable" else "enable"}"
                                                        AdbDevices.runCommandFromMap(cmd)
                                                        isAirplaneModeOn = !isAirplaneModeOn
                                                        onCommandExecuted(
                                                            "Airplane Mode $action on ${
                                                                deviceLabel(
                                                                    selectedHardwareSource
                                                                )
                                                            } at ${
                                                                java.time.LocalDateTime.now().format(
                                                                    java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                                                                )
                                                            }"
                                                        )
                                                    }
                                                }
                                            )
                                            deviceMenuItem(
                                                label = "Open Network Settings",
                                                onClick = {
                                                    coroutineScope.launch {
                                                        withContext(Dispatchers.IO) {
                                                            AdbDevices.runCommandFromMap("$adbPath -s $selectedHardwareSource shell am start -a android.settings.WIRELESS_SETTINGS")
                                                        }
                                                        showDeviceMenu = false
                                                    }
                                                }
                                            )
                                        }
                                            deviceMenuItem(
                                                label = "Open Settings",
                                                onClick = {
                                                    coroutineScope.launch {
                                                        withContext(Dispatchers.IO) {
                                                            AdbDevices.runCommandFromMap("$adbPath -s $selectedHardwareSource shell am start -a android.settings.SETTINGS")
                                                        }
                                                        showDeviceMenu = false
                                                    }
                                                }
                                            )
                                            Divider(
                                                color = DropdownColors.divider,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                            deviceMenuItem(
                                                label = "Open ScreenCopy",
                                                onClick = {
                                                    coroutineScope.launch {
                                                        if (!VideoRecorder.isScrcpyInstalled()) {
                                                            showDeviceMenu = false
                                                            showScrcpyInstallDialog = true
                                                        } else {
                                                            try {
                                                                val pb = ProcessBuilder(
                                                                    AppConfig.scrcpyPath,
                                                                    "-s",
                                                                    selectedHardwareSource
                                                                )
                                                                val env = pb.environment()
                                                                val adbDir = java.io.File(adbPath).parent ?: ""
                                                                val scrcpyDir =
                                                                    java.io.File(AppConfig.scrcpyPath).parent ?: ""
                                                                env["PATH"] = listOf(
                                                                    adbDir,
                                                                    scrcpyDir,
                                                                    "/usr/local/bin",
                                                                    "/opt/homebrew/bin",
                                                                    "/usr/bin"
                                                                )
                                                                    .filter { it.isNotEmpty() }
                                                                    .joinToString(":") + ":" + (env["PATH"] ?: "")

                                                                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD)
                                                                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                                                                    .start()

                                                                onCommandExecuted(
                                                                    "ScreenCopy launched on ${
                                                                        deviceLabel(
                                                                            selectedHardwareSource
                                                                        )
                                                                    } at ${
                                                                        java.time.LocalDateTime.now().format(
                                                                            java.time.format.DateTimeFormatter.ofPattern(
                                                                                "HH:mm"
                                                                            )
                                                                        )
                                                                    }"
                                                                )
                                                            } catch (e: Exception) {
                                                                onCommandExecuted("Couldn't launch ScreenCopy: ${e.message}")
                                                            }
                                                            showDeviceMenu = false
                                                        }
                                                    }
                                                }
                                            )
                                            deviceMenuItem(
                                                label = "Take Screenshot",
                                                onClick = {
                                                    coroutineScope.launch {
                                                        withContext(Dispatchers.IO) { takeScreenshotToClipboard(selectedHardwareSource) }
                                                        showDeviceMenu = false
                                                        onCommandExecuted("Screenshot captured to clipboard from ${deviceLabel(selectedHardwareSource)} at ${java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}")
                                                    }
                                                }
                                            )
                                            deviceMenuItem(
                                                label = "Take Video via ScreenCopy",
                                                onClick = {
                                                    coroutineScope.launch {
                                                        showDeviceMenu = false
                                                        showVideoDialog = true
                                                    }
                                                }
                                            )

                                            Divider(
                                                color = DropdownColors.divider,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                            )

                                            deviceMenuItem(
                                                label = "Restart Device",
                                                isDanger = true,
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val label = deviceLabel(selectedHardwareSource)   // capture before the device drops off
                                                        withContext(Dispatchers.IO) {
                                                            AdbDevices.runCommandFromMap("$adbPath -s $selectedHardwareSource reboot")
                                                        }
                                                        showDeviceMenu = false
                                                        onCommandExecuted("Device $label restarted at ${java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}")
                                                    }
                                                }
                                            )
                                    }
                                }
                            }
                        }
                    }
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
                            width = when {
                                hasFocus -> 2.dp
                                else -> 1.dp
                            },
                            color = when {
                                hasFocus -> DropdownColors.focusBorder
                                isTriggerHovered -> DropdownColors.fieldBorderHover
                                else -> DropdownColors.fieldBorder
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .hoverable(triggerInteraction)
                        .clickable(enabled = commandOptions.isNotEmpty() || disconnectedDevices.isNotEmpty()) {
                            isExpanded = true
                        }
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            hasFocus = focusState.hasFocus
                        }
                        .onGloballyPositioned { coordinates ->
                            dropdownWidth = with(density) {
                                coordinates.size.width.toDp()
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    Text(
                        text = if (displayText.startsWith("Select")) {
                            displayText
                        } else {
                            formatDeviceDisplay(displayText)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = when {
                            commandOptions.isEmpty() -> DropdownColors.textDim
                            displayText.startsWith("Select") -> DropdownColors.textPlaceholder
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
                        commandOptions.forEach { deviceId ->
                            key(deviceId) {
                                val itemInteraction = remember { MutableInteractionSource() }
                                val itemHovered by itemInteraction.collectIsHoveredAsState()

                                DropdownMenuItem(
                                    onClick = {
                                        onCommandSelected(deviceId)
                                        isExpanded = false
                                    },
                                    interactionSource = itemInteraction,
                                    modifier = Modifier.background(if (itemHovered) DropdownColors.rowHover else Color.Transparent)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formatDeviceDisplay(deviceId),
                                            fontFamily = AppFontFamily,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = DropdownColors.textPrimary,
                                            modifier = Modifier.weight(1f)
                                        )

                                        val busy = actionSerial == deviceId && action == ConnectionAction.DISCONNECTING

                                        connectionActionButton(
                                            icon = Icons.Default.LinkOff,
                                            description = "Disconnect",
                                            busy = busy,
                                            enabled = action == ConnectionAction.NONE,
                                            tooltip = "Disconnect from ADB",
                                            hoverColor = Color(0xFFEF4444),
                                            onClick = {
                                                val snapshot = snapshotOf(deviceId)
                                                actionSerial = deviceId
                                                action = ConnectionAction.DISCONNECTING
                                                coroutineScope.launch {
                                                    val mode = AdbConnectionManager.disconnect(snapshot)
                                                    action = ConnectionAction.NONE
                                                    actionSerial = null
                                                    onCommandExecuted(
                                                        when (mode) {
                                                            DisconnectMode.ADB -> "Disconnected ${snapshot.label} from ADB"
                                                            DisconnectMode.DETACHED -> "Detached ${snapshot.label} (still attached to adb, hidden from Android Helper)"
                                                        }
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        AnimatedVisibility(
                            visible = disconnectedDevices.isNotEmpty(),
                            enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                            exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
                        ) {
                            Column(modifier = Modifier.animateContentSize()) {
                                if (commandOptions.isNotEmpty()) {
                                    Divider(
                                        color = DropdownColors.divider,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    "DISCONNECTED",
                                    fontFamily = AppFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DropdownColors.textMuted,
                                    letterSpacing = 0.8.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )

                                disconnectedDevices.forEach { device ->
                                    key(device.serial) {
                                        val busy = actionSerial == device.serial && action == ConnectionAction.RECONNECTING

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                                .alpha(if (busy) 1f else 0.55f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val emoji = when {
                                                device.deviceType == DeviceType.TV -> TV
                                                device.serial.startsWith("emulator", true) -> EMULATOR_COMPUTER
                                                else -> PHONE_MOBILE
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "(${device.sdkRelease}) $emoji ${device.label}",
                                                    fontFamily = AppFontFamily,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = DropdownColors.textMuted
                                                )
                                                Text(
                                                    text = if (device.mode == DisconnectMode.ADB) "Disconnected from ADB"
                                                    else "Hidden · still attached to adb",
                                                    fontFamily = AppFontFamily,
                                                    fontWeight = FontWeight.Light,
                                                    fontSize = 10.sp,
                                                    color = DropdownColors.textDim
                                                )
                                            }

                                            connectionActionButton(
                                                icon = Icons.Default.Refresh,
                                                description = "Reconnect",
                                                busy = busy,
                                                enabled = action == ConnectionAction.NONE,
                                                tooltip = "Reconnect via ADB",
                                                hoverColor = Color(0xFF22C55E),
                                                spin = true,
                                                onClick = {
                                                    actionSerial = device.serial
                                                    action = ConnectionAction.RECONNECTING
                                                    coroutineScope.launch {
                                                        val result = AdbConnectionManager.reconnect(device)
                                                        action = ConnectionAction.NONE
                                                        actionSerial = null
                                                        onCommandExecuted(
                                                            result.fold(
                                                                { "Reconnected ${device.label}" },
                                                                { "Couldn't reconnect ${device.label}: ${it.message}" }
                                                            )
                                                        )
                                                    }
                                                }
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
    }

    val deviceIcon = when {
        deviceInfoMap[selectedHardwareSource]?.deviceType == DeviceType.TV -> Icons.Default.Tv
        selectedHardwareSource.startsWith("emulator", ignoreCase = true) -> Icons.Default.Computer
        else -> Icons.Default.PhoneAndroid
    }

    LaunchedEffect(selectedHardwareSource, commandOptions) {
        if (showDeviceInfoDialog) {
            val isDeviceAvailable = commandOptions.contains(selectedHardwareSource) &&
                    !selectedHardwareSource.startsWith("Select", ignoreCase = true)
            if (!isDeviceAvailable) {
                showDeviceInfoDialog = false
            }
        }
    }

    if (showDeviceInfoDialog) {
        Dialog(onDismissRequest = { showDeviceInfoDialog = false }) {
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
                            .background(DropdownColors.dialogHeaderBg)
                            .padding(20.dp, 16.dp, 20.dp, 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Device Information $PHONE_MOBILE",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = DropdownColors.textPrimary
                            )
                            IconButton(
                                onClick = { showDeviceInfoDialog = false },
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
                            "Connected device details and network info",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Light,
                            fontSize = 12.sp,
                            color = DropdownColors.textMuted
                        )
                    }

                    Divider(color = DropdownColors.divider)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        val scrollState = rememberScrollState()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(start = 20.dp, end = 28.dp, top = 20.dp, bottom = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = deviceIcon,
                                contentDescription = null,
                                tint = DropdownColors.textPrimary,
                                modifier = Modifier.size(48.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            val info = deviceInfoMap[selectedHardwareSource]
                            val name = deviceLabel(selectedHardwareSource)
                            val versionText = "Version: ${info?.sdkRelease.orEmpty()} | API level: ${info?.sdk ?: 0}"
                            val ipCopyable = deviceIp.isNotEmpty() && deviceIp != "Loading..." &&
                                    !deviceIp.startsWith("Not") && deviceIp != "N/A"

                            fun copy(text: String) {
                                val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                                val selection = java.awt.datatransfer.StringSelection(text)
                                clipboard.setContents(selection, selection)
                            }

                            deviceInfoItem(label = "Device Name", value = name, onCopy = { copy(name) })
                            Spacer(modifier = Modifier.height(10.dp))

                            deviceInfoItem(label = "Android Version", value = versionText, onCopy = { copy(versionText) })
                            Spacer(modifier = Modifier.height(10.dp))

                            deviceInfoItem(
                                label = "Device Type",
                                value = if (info?.deviceType == DeviceType.TV) "Android TV" else "Mobile"
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            deviceInfoItem(label = "Device Model", value = info?.model.orEmpty())
                            Spacer(modifier = Modifier.height(10.dp))

                            deviceInfoItem(label = "Device ID", value = selectedHardwareSource)
                            Spacer(modifier = Modifier.height(10.dp))

                            deviceInfoItem(
                                label = "IPv4 Address",
                                value = deviceIp,
                                onCopy = { if (ipCopyable) copy(deviceIp) }
                            )
                        }
                        VerticalScrollbar(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(end = 4.dp, top = 4.dp, bottom = 4.dp),
                            adapter = rememberScrollbarAdapter(scrollState),
                            style = ScrollbarStyle(
                                minimalHeight = 40.dp,
                                thickness = 6.dp,
                                shape = RoundedCornerShape(3.dp),
                                hoverDurationMillis = 300,
                                unhoverColor = Color(0xFF556677),
                                hoverColor = Color(0xFF8899AA)
                            )
                        )
                    }
                }
            }
        }
    }
    if (showVideoDialog) {
        videoRecordDialog(
            onDismiss = {
                if (isVideoRecording) {
                    VideoRecorder.stopRecording()
                }
                showVideoDialog = false
                isVideoRecording = false
            },
            onStartRecording = { noSound, showTouches, fileName ->
                if (!VideoRecorder.isScrcpyInstalled()) {
                    showVideoDialog = false
                    showScrcpyInstallDialog = true
                } else {
                    val started = VideoRecorder.startRecording(
                        deviceId = selectedHardwareSource,
                        fileName = fileName,
                        noSound = noSound,
                        showTouches = showTouches
                    )
                    isVideoRecording = started
                    if (started) {
                        val options = buildList {
                            if (noSound) add("No audio")
                            if (showTouches) add("Show touches")
                        }
                        val optionsText = if (options.isNotEmpty()) {
                            " — ${options.joinToString(", ")}"
                        } else ""
                        onCommandExecuted("Recording started: $fileName.mp4$optionsText")
                    } else {
                        onCommandExecuted("Failed to start recording")
                    }
                }
            },
            onStopRecording = {
                val savedFileName = VideoRecorder.getLastFileName()
                VideoRecorder.stopRecording()
                isVideoRecording = false
                showVideoDialog = false
                val desktop = System.getProperty("user.home") + "/Desktop"
                val filePath = "$desktop/$savedFileName.mp4"
                onCommandExecuted("Result: Video saved successfully\nFile: $filePath")
            },
            isRecording = isVideoRecording
        )
    }
    if (showScrcpyInstallDialog) {
        scrcpyInstallDialog(
            onDismiss = { showScrcpyInstallDialog = false }
        )
    }
}
@Composable
private fun deviceMenuItem(
    label: String,
    isDanger: Boolean = false,
    statusColor: Color? = null,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()

    DropdownMenuItem(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier
            .background(
                when {
                    isHovered && isDanger -> Color(0xFFEF4444).copy(alpha = 0.1f)
                    isHovered -> DropdownColors.rowHover
                    else -> Color.Transparent
                }
            )

    ) {
        Text(
            label,
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = when {
                statusColor != null -> statusColor
                isDanger && isHovered -> Color(0xFFEF4444)
                isDanger -> Color(0xFFEF4444).copy(alpha = 0.8f)
                isHovered -> DropdownColors.textPrimary
                else -> DropdownColors.textSecondary
            }

        )
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun connectionActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    busy: Boolean,
    enabled: Boolean,
    tooltip: String,
    hoverColor: Color,
    spin: Boolean = false,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()

    val infinite = rememberInfiniteTransition()
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart)
    )

    TooltipArea(
        tooltip = {
            Surface(color = Color(0xFF1A1A1A), shape = RoundedCornerShape(6.dp), elevation = 4.dp) {
                Text(
                    tooltip,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    fontFamily = AppFontFamily,
                    fontSize = 11.sp,
                    color = Color.White
                )
            }
        },
        delayMillis = 400
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled && !busy,
            interactionSource = interaction,
            modifier = Modifier.size(24.dp)
        ) {
            Crossfade(targetState = busy, animationSpec = tween(150)) { isBusy ->
                if (isBusy) {
                    CircularProgressIndicator(
                        progress = 0.75f,
                        color = hoverColor,
                        strokeWidth = 1.5.dp,
                        modifier = Modifier.size(14.dp).rotate(rotation)
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = description,
                        tint = when {
                            !enabled -> DropdownColors.textDim.copy(alpha = 0.35f)
                            isHovered -> hoverColor
                            else -> DropdownColors.textMuted
                        },
                        modifier = Modifier.size(14.dp).then(
                            if (spin && isHovered) Modifier.rotate(rotation) else Modifier
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun deviceHeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()
    var isPressed by remember { mutableStateOf(false) }

    IconButton(
        onClick = {
            isPressed = true
            onClick()
        },
        interactionSource = interaction,
        modifier = Modifier.size(24.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = when {
                isHovered -> Color.White
                else -> Color(0xFF8899AA)
            },
            modifier = Modifier.size(16.dp)
        )
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(150)
            isPressed = false
        }
    }

}
