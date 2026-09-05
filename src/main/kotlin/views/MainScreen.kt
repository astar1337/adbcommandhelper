package views

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import commands.*
import ui.components.*
import ui.components.theme.AppFontFamily
import kotlinx.coroutines.delay
import utils.helpers.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import commands.formatWorkflowOutput
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material.icons.filled.Info
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ui.components.theme.DropdownColors.contentBackgroundColor
import ui.components.theme.DropdownColors.headerBackgroundColor


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun mainScreen() {
    var selectedCommand by remember { mutableStateOf<Command?>(null) }
    val initialDevices = remember { AdbDevices.getDevicesList() }
    var selectedHardwareSource by remember { mutableStateOf(if (initialDevices.size == 1) initialDevices.first() else "Select your source...") }
    var selectedEnvSource by remember { mutableStateOf("Select environment...") }
    var selectedAppSource by remember { mutableStateOf("Select app...") }

    val notificationQueue = remember { mutableStateListOf<Notification>() }
    var activeNotification by remember { mutableStateOf<Notification?>(null) }
    var notificationTrigger by remember { mutableStateOf(0) }

    var hasSelectedDevice by remember { mutableStateOf(initialDevices.size == 1) }
    var hasSelectedEnv by remember { mutableStateOf(false) }
    var hasSelectedApp by remember { mutableStateOf(false) }

    var devicesList by remember { mutableStateOf(initialDevices) }
    val refreshTrigger by remember { mutableStateOf(0) }

    var selectedDeviceId by remember { mutableStateOf(if (initialDevices.size == 1) initialDevices.first() else "") }
    //var outputHistory by remember { mutableStateOf(mutableListOf<String>()) }
    var previousDeviceCount by remember { mutableStateOf(-1) }

    val isDeviceSelected = (hasSelectedDevice || selectedDeviceId.isNotEmpty()) && devicesList.isNotEmpty()

    val isAppSelected = hasSelectedApp
    val isEnvSelected = hasSelectedEnv

    var selectedEnvValue by remember { mutableStateOf("") }
    var selectedAppValue by remember { mutableStateOf("") }

    var currentNotifState by remember { mutableStateOf(NotificationState.DISABLED) }
    var currentEnvList by remember { mutableStateOf(mapOf<String, String>()) }
    var refreshNotifCounter by remember { mutableStateOf(0) }
    var selectedAppKey by remember { mutableStateOf("") }

    var installedApps by remember { mutableStateOf(listOf<InstalledApp>()) }
    var availableAppList by remember { mutableStateOf(mapOf<String, String>()) }

    val tutorialTargets = remember { mutableStateMapOf<TutorialStep, Rect>() }
    var showTutorial by remember { mutableStateOf(false) }
    val tutorialSteps = remember {
        TutorialStep.entries.filter { it != TutorialStep.DONE }
    }
    var currentStepIndex by remember { mutableStateOf(0) }
    var tutorialStep by remember {
        mutableStateOf(if (showTutorial) tutorialSteps.getOrNull(0) else null)
    }
    var removeZipAfterInstall by remember { mutableStateOf(false) }

    var showNavBar by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    var isWorkflowDialogOpen by remember { mutableStateOf(false) }
    var cachedDeviceName by remember { mutableStateOf("") }
    var cachedSdk by remember { mutableStateOf(0) }

    var isConsoleExpanded by remember { mutableStateOf(true) }
    var outputColor by remember { mutableStateOf<Color?>(null) }
    var consoleMessages by remember { mutableStateOf(listOf<ConsoleInstallMessage>()) }
    var showClearMessage by remember { mutableStateOf(false) }

    fun resetAppAndEnvState() {
        selectedAppKey = ""
        selectedAppSource = "Select app..."
        selectedAppValue = ""
        hasSelectedApp = false

        installedApps = emptyList()
        availableAppList = emptyMap()

        currentEnvList = emptyMap()
        selectedEnvSource = "Select environment..."
        selectedEnvValue = ""
        hasSelectedEnv = false

        selectedCommand = null
    }

    LaunchedEffect(devicesList, selectedHardwareSource) {
        if (devicesList.size == 1 && selectedDeviceId.isEmpty()) {
            val singleDevice = devicesList.first()
            selectedHardwareSource = singleDevice
            selectedDeviceId = singleDevice
            hasSelectedDevice = true
        }
    }

    fun buildEnvList(app: InstalledApp): Map<String, String> {
        val config = appConfigs[app.key] ?: return emptyMap()
        val envMap = mutableMapOf<String, String>()

        if (app.hasProd) {
            envMap["PRODUCTION"] = config.prodPackage
        }
        if (app.hasStaging) {
            envMap["STAGING"] = config.stagPackage
        }
        return envMap
    }

    fun refreshEnvForSelectedApp() {
        val app = installedApps.find { it.key == selectedAppKey }

        if (app == null) {
            selectedAppKey = ""
            selectedAppSource = "Select app..."
            hasSelectedApp = false
            currentEnvList = emptyMap()
            selectedEnvSource = "Select environment..."
            selectedEnvValue = ""
            hasSelectedEnv = false
            return
        }

        val newEnvList = buildEnvList(app)

        currentEnvList = newEnvList

        if (selectedEnvValue.isNotEmpty() && selectedEnvValue !in newEnvList.values) {
            selectedEnvSource = "Select environment..."
            selectedEnvValue = ""
            hasSelectedEnv = false
        }

        when {
            newEnvList.size == 1 -> {
                val env = newEnvList.entries.first()
                selectedEnvSource = env.key
                selectedEnvValue = env.value
                hasSelectedEnv = true
            }

            selectedEnvValue.isNotEmpty() && selectedEnvValue in newEnvList.values -> {
                val validEnv = newEnvList.entries.find { it.value == selectedEnvValue }
                if (validEnv != null) {
                    selectedEnvSource = validEnv.key
                    hasSelectedEnv = true
                }
            }

            else -> {
                selectedEnvSource = "Select environment..."
                selectedEnvValue = ""
                hasSelectedEnv = false
            }
        }
    }

    LaunchedEffect(installedApps, selectedAppKey) {
        refreshEnvForSelectedApp()
    }

    suspend fun refreshInstalledApps() {
        if (selectedDeviceId.isEmpty()) return

        val newInstalledApps = withContext(Dispatchers.IO) {
            AppDetection.getInstalledApps(selectedDeviceId)
        }

        if (installedApps == newInstalledApps) return

        installedApps = newInstalledApps
        availableAppList = newInstalledApps.associate { it.key to it.displayName }

        val stillInstalled = newInstalledApps.find { it.key == selectedAppKey }

        when {
            stillInstalled != null -> {
                selectedAppSource = stillInstalled.displayName
                hasSelectedApp = true
                currentEnvList = buildEnvList(stillInstalled)
            }

            newInstalledApps.size == 1 -> {
                val app = newInstalledApps.first()
                selectedAppKey = app.key
                selectedAppSource = app.displayName
                selectedAppValue = app.key.lowercase()
                hasSelectedApp = true
                val envList = buildEnvList(app)
                currentEnvList = envList

                if (envList.size == 1) {
                    val env = envList.entries.first()
                    selectedEnvSource = env.key
                    selectedEnvValue = env.value
                    hasSelectedEnv = true
                }
            }

            newInstalledApps.isEmpty() -> {
                selectedAppKey = ""
                selectedAppSource = "No apps installed"
                selectedAppValue = ""
                hasSelectedApp = false
                currentEnvList = emptyMap()
                selectedEnvSource = "Select environment..."
                selectedEnvValue = ""
                hasSelectedEnv = false
            }

            else -> {
                selectedAppKey = ""
                selectedAppSource = "Select app..."
                selectedAppValue = ""
                hasSelectedApp = false
                currentEnvList = emptyMap()
                selectedEnvSource = "Select environment..."
                selectedEnvValue = ""
                hasSelectedEnv = false
            }
        }
        refreshEnvForSelectedApp()
    }

    LaunchedEffect(selectedDeviceId) {
        if (selectedDeviceId.isEmpty()) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            DeviceInfoCache.fetchAndCache(selectedDeviceId)
        }
        cachedDeviceName = DeviceInfoCache.getNameSync(selectedDeviceId)
        cachedSdk = DeviceInfoCache.getSdkSync(selectedDeviceId)

        refreshInstalledApps()

        while (true) {
            delay(5000)
            refreshInstalledApps()
        }
    }

    LaunchedEffect(selectedDeviceId, selectedEnvValue, refreshNotifCounter) {
        if (selectedDeviceId.isNotEmpty() && selectedEnvValue.isNotEmpty()) {
            if (refreshNotifCounter > 0) delay(1500)
            currentNotifState = withContext(Dispatchers.IO) {
                getNotificationState(selectedDeviceId, selectedEnvValue)
            }
        }
    }

    LaunchedEffect(currentNotifState, selectedDeviceId, selectedEnvValue) {
        if (
            selectedCommand?.isNotificationPermissionCommand == true &&
            selectedDeviceId.isNotEmpty() &&
            selectedEnvValue.isNotEmpty()
        ) {
            selectedCommand = notificationCommand(
                deviceId = selectedDeviceId,
                appId = selectedEnvValue,
                appKey = selectedAppKey,
                envName = selectedEnvSource,
                sdk = cachedSdk,
                state = currentNotifState
            )
        }
    }

    LaunchedEffect(refreshTrigger) {
        while (true) {
            delay(1000)
            val newDevices = withContext(Dispatchers.IO) {
                AdbDevices.getDevicesList()
            }
            devicesList = newDevices
            val currentCount = newDevices.size

            if (previousDeviceCount == 1 && currentCount > 1) {
                selectedDeviceId = ""
                selectedHardwareSource = "Select a device..."
                hasSelectedDevice = false
                resetAppAndEnvState()
            }

            if (selectedDeviceId.isNotEmpty() && selectedDeviceId !in newDevices) {
                DeviceInfoCache.invalidate(selectedDeviceId)
                selectedDeviceId = ""
                selectedHardwareSource = "Select your source..."
                hasSelectedDevice = false
                resetAppAndEnvState()
            }

            if (currentCount != previousDeviceCount && previousDeviceCount != -1) {
                when {
                    currentCount < previousDeviceCount -> {
                        val notifications = mutableListOf(
                            Notification("Device removed", NotificationType.WARNING)
                        )

                        when (currentCount) {
                            0 -> notifications.add(Notification("No devices connected", NotificationType.ERROR))
                            1 -> notifications.add(Notification("1 device remaining", NotificationType.SUCCESS))
                            else -> notifications.add(Notification("$currentCount devices remaining", NotificationType.SUCCESS))
                        }

                        if (activeNotification == null) {
                            activeNotification = notifications.first()
                            notificationTrigger++
                            if (notifications.size > 1) {
                                notificationQueue.add(notifications[1])
                            }
                        } else {
                            notificationQueue.addAll(notifications)
                        }
                    }

                    currentCount >= previousDeviceCount -> {
                        val notification = if (currentCount == 1) {
                            Notification("Device connected", NotificationType.SUCCESS)
                        } else {
                            Notification("$currentCount devices connected", NotificationType.SUCCESS)
                        }

                        if (activeNotification == null) {
                            activeNotification = notification
                            notificationTrigger++
                        } else {
                            notificationQueue.add(notification)
                        }
                    }
                }
            }
            previousDeviceCount = currentCount
        }
    }

    LaunchedEffect(notificationTrigger) {
        if (notificationTrigger > 0 && activeNotification != null) {
            delay(2500)
            activeNotification = null
            if (notificationQueue.isNotEmpty()) {
                delay(300)
                activeNotification = notificationQueue.removeFirst()
                notificationTrigger++
            }
        }
    }

    LaunchedEffect(selectedDeviceId, selectedEnvValue) {
        if (selectedDeviceId.isNotEmpty() || selectedEnvValue.isNotEmpty()) {
            selectedCommand = null
        }
    }

    LaunchedEffect(showClearMessage) {
        if (showClearMessage) {
            delay(1000)
            showClearMessage = false
        }
    }
    val emptyStateText = when {
        showClearMessage -> "Console cleared"
        consoleMessages.isEmpty() -> "Awaiting output from console..."
        else -> ""
    }
    LaunchedEffect(showClearMessage) {
        if (showClearMessage) {
            delay(2000)
            showClearMessage = false
        }
    }
    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = contentBackgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = headerBackgroundColor,
                elevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 88.dp)
                        .padding(horizontal = 30.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Image(
                            painter = painterResource("icons/daznlogo.png"),
                            contentDescription = "Logo",
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = "Android Helper",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = Color.White
                        )

                        AnimatedVisibility(
                            visible = isDeviceSelected && devicesList.isNotEmpty(),
                            enter = fadeIn(animationSpec = tween(300)),
                            exit = fadeOut(animationSpec = tween(300))
                        ) {
                            Spacer(Modifier.width(12.dp))
                            val tutorialInteraction = remember { MutableInteractionSource() }
                            val isTutorialHovered by tutorialInteraction.collectIsHoveredAsState()

                            IconButton(
                                onClick = {
                                    showTutorial = true
                                    currentStepIndex = 0
                                    tutorialStep = tutorialSteps.getOrNull(0)
                                },
                                interactionSource = tutorialInteraction,
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Start tutorial",
                                    tint = if (isTutorialHovered) Color(0xFFFFEB3B) else Color(0xFF8899AA),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isDeviceSelected && devicesList.isNotEmpty(),
                        enter = fadeIn(animationSpec = tween(750)),
                        exit = fadeOut(animationSpec = tween(750))
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.onGloballyPositioned { coords ->
                                tutorialTargets[TutorialStep.HEADER_BUTTONS] =
                                    coords.boundsInWindow()
                            }
                        ) {
                            TooltipArea(
                                tooltip = {
                                    Surface(
                                        color = Color(0xFF1A1A1A),
                                        shape = RoundedCornerShape(6.dp),
                                        elevation = 4.dp
                                    ) {
                                        Text(
                                            "Run CI/CD Workflow",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            fontFamily = AppFontFamily,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
                                },
                                delayMillis = 250
                            ) {
                                workflowButton(
                                    onWorkflowTriggered = { result ->
                                        val output = formatWorkflowOutput(result)
                                        consoleMessages = consoleMessages + ConsoleInstallMessage.Text(output)
                                        showClearMessage = false
                                        isWorkflowDialogOpen = false
                                    },
                                    onConsoleOutput = { newMessage ->
                                        consoleMessages = consoleMessages + newMessage
                                    },
                                    modifier = Modifier.height(48.dp)
                                )
                            }

                            TooltipArea(
                                tooltip = {
                                    Surface(
                                        color = Color(0xFF1A1A1A),
                                        shape = RoundedCornerShape(6.dp),
                                        elevation = 4.dp
                                    ) {
                                        Text(
                                            "Select a VPN gateway",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            fontFamily = AppFontFamily,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
                                },
                                delayMillis = 250
                            ) {
                                vpnButton(
                                    modifier = Modifier.height(48.dp),
                                    onCommandExecuted = { output, color ->
                                        consoleMessages = consoleMessages + ConsoleInstallMessage.Text(output)
                                        outputColor = color
                                    },
                                    deviceId = selectedDeviceId,
                                    deviceName = cachedDeviceName,
                                    onCommandReplaced = { output, color ->
                                        consoleMessages = if (consoleMessages.isNotEmpty()) {
                                            consoleMessages.dropLast(1) + ConsoleInstallMessage.Text(output)
                                        } else {
                                            listOf(ConsoleInstallMessage.Text(output))
                                        }
                                        outputColor = color
                                    }
                                )
                            }

                            TooltipArea(
                                tooltip = {
                                    Surface(
                                        color = Color(0xFF1A1A1A),
                                        shape = RoundedCornerShape(6.dp),
                                        elevation = 4.dp
                                    ) {
                                        Text(
                                            "Install a build from apk or zip file",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            fontFamily = AppFontFamily,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
                                },
                                delayMillis = 250
                            ) {
                                zipApkInstaller(
                                    deviceId = selectedDeviceId,
                                    enabled = isDeviceSelected && devicesList.isNotEmpty(),
                                    removeZipAfterInstall = removeZipAfterInstall,
                                    onInstallStarted = { newMessage ->
                                        consoleMessages = consoleMessages + newMessage
                                    },
                                    onInstallCompleted = { newMessage ->
                                        consoleMessages = consoleMessages + newMessage
                                    },
                                    modifier = Modifier.height(48.dp)
                                )
                            }

                            TooltipArea(
                                tooltip = {
                                    Surface(
                                        color = Color(0xFF1A1A1A),
                                        shape = RoundedCornerShape(6.dp),
                                        elevation = 4.dp
                                    ) {
                                        Text(
                                            "App Settings",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            fontFamily = AppFontFamily,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
                                },
                                delayMillis = 250
                            ) {
                                additionalSettings(
                                    deviceId = selectedDeviceId,
                                    removeZipAfterInstall = removeZipAfterInstall,
                                    onRemoveZipToggle = { removeZipAfterInstall = it },
                                    onCommandExecuted = { output, commandLabel ->
                                        val result = if (output.isEmpty()) {
                                            "$commandLabel ran successfully"
                                        } else {
                                            "$commandLabel\n$output"
                                        }
                                        consoleMessages = consoleMessages + ConsoleInstallMessage.Text(result)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Divider(
                color = Color(0xFFFFEB3B),
                thickness = 2.dp
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Crossfade(
                    targetState = devicesList.isNotEmpty(),
                    animationSpec = tween(200),
                    modifier = Modifier.weight(1f),
                ) { deviceConnected ->
                    if (!deviceConnected) {
                        emptyDeviceState(
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Crossfade(
                                    targetState = devicesList.isNotEmpty(),
                                    animationSpec = tween(200),
                                    modifier = Modifier.weight(1f)
                                ) { hasDevices ->
                                    if (!hasDevices) {
                                        skeletonDropdown(
                                            modifier = Modifier.fillMaxWidth(),
                                            message = "No devices connected"
                                        )
                                    } else {
                                        deviceSourceDropDown(
                                            selectedHardwareSource = selectedHardwareSource,
                                            commandOptions = devicesList,
                                            onCommandSelected = { deviceId ->
                                                selectedHardwareSource = deviceId
                                                selectedDeviceId = deviceId
                                                hasSelectedDevice = true
                                            },
                                            onCommandExecuted = { output ->
                                                consoleMessages = consoleMessages + ConsoleInstallMessage.Text(output)
                                                showClearMessage = false
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                                .onGloballyPositioned { coords ->
                                                    tutorialTargets[TutorialStep.DEVICE_DROPDOWN] =
                                                        coords.boundsInWindow()
                                                },
                                            elementId = "device_dropdown"
                                        )
                                    }
                                }

                                Crossfade(
                                    targetState = availableAppList.isNotEmpty(),
                                    animationSpec = tween(200),
                                    modifier = Modifier.weight(1f)
                                ) { hasApps ->
                                    if (!hasApps) {
                                        skeletonDropdown(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onGloballyPositioned { coords ->
                                                    tutorialTargets[TutorialStep.APP_DROPDOWN] =
                                                        coords.boundsInWindow()
                                                },
                                            message = "No apps or devices"
                                        )
                                    } else {
                                        appSelectDropDown(
                                            enabled = isDeviceSelected && devicesList.isNotEmpty() && availableAppList.isNotEmpty(),
                                            selectedHardwareSource = selectedHardwareSource,
                                            selectedEnvSource = selectedEnvSource,
                                            hasSelectedDevice = hasSelectedDevice,
                                            hasSelectedApp = hasSelectedApp,
                                            hasSelectedEnv = hasSelectedEnv,
                                            commandOptions = availableAppList,
                                            selectedAppKey = selectedAppKey,
                                            selectedAppSource = selectedAppSource,
                                            onCommandSelected = { appKey ->
                                                val app = installedApps.find { it.key == appKey }
                                                if (app != null) {
                                                    selectedAppKey = appKey
                                                    selectedAppSource = app.displayName
                                                    selectedAppValue = appKey.lowercase()
                                                    hasSelectedApp = true
                                                    if (currentEnvList.size == 1) {
                                                        val envEntry = currentEnvList.entries.first()
                                                        selectedEnvSource = envEntry.key
                                                        selectedEnvValue = envEntry.value
                                                        hasSelectedEnv = true
                                                    } else {
                                                        selectedEnvSource = "Select environment..."
                                                        selectedEnvValue = ""
                                                        hasSelectedEnv = false
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onGloballyPositioned { coords ->
                                                    tutorialTargets[TutorialStep.APP_DROPDOWN] = coords.boundsInWindow()
                                                },
                                        )
                                    }
                                }

                                Crossfade(
                                    targetState = isAppSelected && currentEnvList.isNotEmpty(),
                                    animationSpec = tween(200),
                                    modifier = Modifier.weight(1f)
                                ) { hasEnv ->
                                    if (!hasEnv) {
                                        skeletonDropdown(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onGloballyPositioned { coords ->
                                                    tutorialTargets[TutorialStep.ENV_DROPDOWN] =
                                                        coords.boundsInWindow()
                                                }
                                        )
                                    } else {
                                        dropDownEnv(
                                            selectedEnvSource = selectedEnvSource,
                                            commandOptions = currentEnvList,
                                            hasSelectedEnv = hasSelectedEnv,
                                            onCommandSelected = { envLabel ->
                                                selectedEnvSource = envLabel
                                                selectedEnvValue = currentEnvList[envLabel] ?: ""
                                                hasSelectedEnv = true
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onGloballyPositioned { coords ->
                                                    tutorialTargets[TutorialStep.ENV_DROPDOWN] =
                                                        coords.boundsInWindow()
                                                },
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Crossfade(
                                targetState = isAppSelected && isEnvSelected,
                                animationSpec = tween(300)
                            ) { ready ->
                                if (!ready) {
                                    skeletonQuickActions(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onGloballyPositioned { coords ->
                                                tutorialTargets[TutorialStep.QUICK_ACTIONS] =
                                                    coords.boundsInWindow()
                                            },
                                        isAppSelected = isAppSelected
                                    )
                                } else {
                                    commandDropdown(
                                        enabled = isDeviceSelected && devicesList.isNotEmpty() && isAppSelected && isEnvSelected,
                                        commandOptions = commandOptions(
                                            deviceId = selectedDeviceId,
                                            envPrefix = selectedEnvValue,
                                            appKey = selectedAppKey,
                                            envName = selectedEnvSource,
                                            notifState = currentNotifState,

                                        ),
                                        onCommandSelected = { selectedCommand = it },
                                        onCommandExecuted = { newMessage,color ->
                                            consoleMessages = consoleMessages + newMessage
                                            showClearMessage = false
                                            refreshNotifCounter++
                                            outputColor = color
                                        },
                                        selectedDeviceId = selectedDeviceId,
                                        selectedAppSource = selectedAppSource,
                                        selectedEnvSource = selectedEnvSource,
                                        notifState = currentNotifState,
                                        selectedEnvValue = selectedEnvValue,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onGloballyPositioned { coords ->
                                                tutorialTargets[TutorialStep.QUICK_ACTIONS] =
                                                    coords.boundsInWindow()
                                            },
                                        onCommandReplaced = { newMessage, color ->
                                            consoleMessages = if (consoleMessages.isNotEmpty()) {
                                                consoleMessages.dropLast(1) + newMessage
                                            } else {
                                                listOf(newMessage)
                                            }
                                            outputColor = color
                                        },
                                        selectedAppKey = selectedAppKey,
                                        )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            outputConsole(
                                messages = consoleMessages,
                                onClear = {
                                    consoleMessages = emptyList()
                                    showClearMessage = true
                                },
                                emptyStateMessage = emptyStateText,
                                isCleared = showClearMessage,
                                enabled = isDeviceSelected && devicesList.isNotEmpty(),
                                deviceId = selectedDeviceId,
                                removeZipAfterInstall = removeZipAfterInstall,
                                onNewMessage = { newMessage ->
                                    if (showClearMessage) {
                                        showClearMessage = false
                                    }
                                    consoleMessages = consoleMessages + newMessage
                                },
                                onExpandChanged = { isConsoleExpanded = it },
                                modifier = Modifier
                                    .then(
                                        if (isConsoleExpanded) Modifier.weight(1f)
                                        else Modifier
                                    )
                                    .onGloballyPositioned { coords ->
                                        tutorialTargets[TutorialStep.CONSOLE] =
                                            coords.boundsInWindow()
                                    },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                appFooter(
                    modifier = Modifier.height(18.dp),
                    version = "2.0.4",
                    githubUrl = "www.github.com"
                )

            }
        }


        notificationHost(
            notification = activeNotification,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(24.dp)
        )

        AnimatedVisibility(
            visible = isDeviceSelected && devicesList.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
        ) {
            sideNavigationBar(
                isNavBarVisible = showNavBar,
                deviceId = selectedDeviceId,
                onBackClick = {},
                onHomeClick = {},
                onOKClick = {},
                onToggleClick = { showNavBar = !showNavBar },
                showToggleButton = true,
            )
        }

        tutorialStep?.let { step ->
            val target = tutorialTargets[step]

            if (target != null) {
                tutorialOverlay(
                    target = target,
                    text = when (step) {
                        TutorialStep.DEVICE_DROPDOWN -> "Select your connected Android device from this dropdown. The app automatically detects devices plugged in via USB or connected wirelessly."
                        TutorialStep.APP_DROPDOWN -> "Choose which application to work with. Apps are detected automatically from your device. If no apps are installed, this will show a placeholder."
                        TutorialStep.ENV_DROPDOWN -> "Select the environment — Production or Staging. This determines which app variant your commands target."
                        TutorialStep.QUICK_ACTIONS -> "Quick action buttons for the most common ADB commands. Launch, force stop, clear data, delete, and toggle notifications with a single click."
                        TutorialStep.CONSOLE -> "All command output appears here. Expand to see full details, copy ADB commands, and click file links to open them."
                        TutorialStep.HEADER_BUTTONS -> "Toolbar buttons: trigger CI/CD workflows, connect to VPN gateways, install APK/ZIP builds, and access device settings."
                        else -> ""
                    },
                    onNext = {
                        currentStepIndex++
                        tutorialStep = tutorialSteps.getOrNull(currentStepIndex)
                    },
                    stepIndex = currentStepIndex,
                    stepCount = tutorialSteps.size,
                    onSkip = {
                        showTutorial = false
                        tutorialStep = null
                        coroutineScope.launch {
                            delay(200)
                        }
                    },
                    onFinish = {
                        showTutorial = false
                        tutorialStep = null
                        coroutineScope.launch {
                            delay(200)
                        }
                    },
                    tooltipAtTop = step == TutorialStep.CONSOLE || step == TutorialStep.QUICK_ACTIONS
                )
            }
        }
    }
}