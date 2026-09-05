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
import androidx.compose.ui.input.key.*
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



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun deviceSourceDropDown(
    selectedHardwareSource: String,
    commandOptions: List<String>,
    onCommandSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    elementId: String = "device_dropdown",
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

    val deviceNameCache = remember { mutableStateMapOf<String, String>() }

    fun formatDeviceDisplay(deviceId: String): String {
        if (deviceId.startsWith("Select", ignoreCase = true)) return deviceId
        if (deviceId.contains("No devices", ignoreCase = true)) return deviceId

        val emoji = if (deviceId.startsWith("emulator", ignoreCase = true)) EMULATOR_COMPUTER else PHONE_MOBILE
        val deviceName = deviceNameCache.getOrPut(deviceId) { getDeviceName(deviceId) }
        val sdkRelease = getAndroidSdkRelease(deviceId)

        return if (deviceName.isNotEmpty() && deviceName != deviceId) {
            "($sdkRelease) $emoji $deviceName"
        } else {
            "($sdkRelease) $emoji $deviceId"
        }
    }
    val displayText = when {
        commandOptions.size == 1 -> commandOptions.first()
        selectedHardwareSource.startsWith("Select") -> "Select a device..."
        else -> selectedHardwareSource
    }
    LaunchedEffect(showDeviceMenu, selectedHardwareSource) {
        if (showDeviceMenu && selectedHardwareSource.isNotEmpty() && !selectedHardwareSource.startsWith("Select")) {
            isDeviceMenuLoading = true
            kotlinx.coroutines.delay(200)
            val wifi = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                getWifiState(selectedHardwareSource)
            }
            val airplane = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                getAirplaneModeState(selectedHardwareSource)
            }
            isWifiOn = wifi
            isAirplaneModeOn = airplane
            isDeviceMenuLoading = false
        }
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
                                        "Device Information",
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
                                        deviceMenuItem(
                                            label = if (isWifiOn) "Turn OFF WiFi" else "Turn ON WiFi",
                                            statusColor = if (isWifiOn) Color(0xFFEF4444) else Color(0xFF22C55E),
                                            onClick = {
                                                coroutineScope.launch {
                                                    val action = if (isWifiOn) "disabled" else "enabled"
                                                    val cmd = "$adbPath  -s $selectedHardwareSource shell svc wifi ${if (isWifiOn) "disable" else "enable"}"
                                                    AdbDevices.runCommandFromMap(cmd)
                                                    isWifiOn = !isWifiOn
                                                    onCommandExecuted("WiFi $action on ${getDeviceName(selectedHardwareSource)} at ${java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}")
                                                }
                                            }
                                        )
                                        deviceMenuItem(
                                            label = if (isAirplaneModeOn) "Turn OFF Airplane Mode" else "Turn ON Airplane Mode",
                                            statusColor = if (isAirplaneModeOn) Color(0xFFEF4444) else Color(0xFF22C55E),
                                            onClick = {
                                                coroutineScope.launch {
                                                    val action = if (isAirplaneModeOn) "disabled" else "enabled"
                                                    val cmd = "$adbPath -s $selectedHardwareSource shell cmd connectivity airplane-mode ${if (isAirplaneModeOn) "disable" else "enable"}"
                                                    AdbDevices.runCommandFromMap(cmd)
                                                    isAirplaneModeOn = !isAirplaneModeOn
                                                    onCommandExecuted("Airplane Mode $action on ${getDeviceName(selectedHardwareSource)} at ${java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}")
                                                }
                                            }
                                        )
                                        deviceMenuItem(
                                            label = "Open Settings",
                                            onClick = {
                                                coroutineScope.launch {
                                                    AdbDevices.runCommandFromMap("$adbPath -s $selectedHardwareSource shell am start -a android.settings.SETTINGS")
                                                    showDeviceMenu = false
                                                }
                                            }
                                        )
                                        deviceMenuItem(
                                            label = "Open Network Settings",
                                            onClick = {
                                                coroutineScope.launch {
                                                    AdbDevices.runCommandFromMap("$adbPath -s $selectedHardwareSource shell am start -a android.settings.WIRELESS_SETTINGS")
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
                                                if (!VideoRecorder.isScrcpyInstalled()) {
                                                    showDeviceMenu = false
                                                    showScrcpyInstallDialog = true
                                                } else {
                                                    try {
                                                        val pb = ProcessBuilder(AppConfig.scrcpyPath, "-s", selectedHardwareSource)
                                                        val env = pb.environment()
                                                        val adbDir = java.io.File(adbPath).parent ?: ""
                                                        val scrcpyDir = java.io.File(AppConfig.scrcpyPath).parent ?: ""
                                                        env["PATH"] = listOf(adbDir, scrcpyDir, "/usr/local/bin", "/opt/homebrew/bin", "/usr/bin")
                                                            .filter { it.isNotEmpty() }
                                                            .joinToString(":") + ":" + (env["PATH"] ?: "")

                                                        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD)
                                                            .redirectError(ProcessBuilder.Redirect.DISCARD)
                                                            .start()

                                                        onCommandExecuted("ScreenCopy launched on ${getDeviceName(selectedHardwareSource)} at ${java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}")
                                                    } catch (e: Exception) {
                                                        onCommandExecuted("Couldn't launch ScreenCopy: ${e.message}")
                                                    }
                                                    showDeviceMenu = false
                                                }
                                            }
                                        )
                                        deviceMenuItem(
                                            label = "Take Screenshot",
                                            onClick = {
                                                coroutineScope.launch {
                                                    takeScreenshotToClipboard(selectedHardwareSource)
                                                    showDeviceMenu = false
                                                    onCommandExecuted("Screenshot captured to clipboard from ${getDeviceName(selectedHardwareSource)} at ${java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}")
                                                }
                                            }
                                        )
                                        deviceMenuItem(
                                            label = "Take Video via ScreenCopy",
                                            onClick = {
                                                showDeviceMenu = false
                                                showVideoDialog = true
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
                                                    AdbDevices.runCommandFromMap("$adbPath -s $selectedHardwareSource reboot")
                                                    showDeviceMenu = false
                                                    onCommandExecuted("Device ${getDeviceName(selectedHardwareSource)} restarted at ${java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}")
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
                        .clickable(enabled = commandOptions.isNotEmpty()) {
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
                            val itemInteraction = remember { MutableInteractionSource() }
                            val itemHovered by itemInteraction.collectIsHoveredAsState()
                            val isSelected = deviceId == selectedHardwareSource

                            DropdownMenuItem(
                                onClick = {
                                    onCommandSelected(deviceId)
                                    isExpanded = false
                                },
                                interactionSource = itemInteraction,
                                modifier = Modifier
                                    .background(
                                        when {
                                            itemHovered -> DropdownColors.rowHover
                                            else -> Color.Transparent
                                        }
                                    )
                            ) {
                                Text(
                                    text = formatDeviceDisplay(deviceId),
                                    fontFamily = AppFontFamily,
                                    fontWeight = if (isSelected) FontWeight.Normal else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = DropdownColors.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val deviceIcon = if (selectedHardwareSource.startsWith("emulator", ignoreCase = true)) {
        Icons.Default.Computer
    } else {
        Icons.Default.PhoneAndroid
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

                            deviceInfoItem(
                                label = "Device Name",
                                value = getDeviceName(selectedHardwareSource).ifEmpty { "Not available" },
                                onCopy = {
                                    val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                                    val selection = java.awt.datatransfer.StringSelection(getDeviceName(selectedHardwareSource))
                                    clipboard.setContents(selection, selection)
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            deviceInfoItem(
                                label = "Android Version",
                                value = "Version: ${getAndroidSdkRelease(selectedHardwareSource)} | API level: ${getAndroidSdk(selectedHardwareSource)}",
                                onCopy = {
                                    val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                                    val selection = java.awt.datatransfer.StringSelection("Version: ${getAndroidSdkRelease(selectedHardwareSource)} | API: ${getAndroidSdk(selectedHardwareSource)}")
                                    clipboard.setContents(selection, selection)
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            deviceInfoItem(
                                label = "Device Model",
                                value = getDeviceModel(selectedHardwareSource).ifEmpty { "Not available" },

                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            deviceInfoItem(
                                label = "Device ID",
                                value = selectedHardwareSource,
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            deviceInfoItem(
                                label = "IPv4 Address",
                                value = getDeviceWifiIp(selectedHardwareSource).ifEmpty { "Not available" },
                                onCopy = {
                                    val ip = getDeviceWifiIp(selectedHardwareSource)
                                    if (ip.isNotEmpty()) {
                                        val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                                        val selection = java.awt.datatransfer.StringSelection(ip)
                                        clipboard.setContents(selection, selection)
                                    }
                                }
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
