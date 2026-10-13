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

private const val DEVICE_PLACEHOLDER = "Select your source..."
private const val APP_PLACEHOLDER = "Select app..."
private const val NO_APPS_PLACEHOLDER = "No apps installed"
private const val ENV_PLACEHOLDER = "Select environment..."

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun toolbarTooltip(text: String, content: @Composable () -> Unit) {
    TooltipArea(
        tooltip = {
            Surface(
                color = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(6.dp),
                elevation = 4.dp
            ) {
                Text(
                    text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontFamily = AppFontFamily,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        },
        delayMillis = 250,
        content = content
    )
}

@Composable
fun mainScreen() {
    var selectedCommand by remember { mutableStateOf<Command?>(null) }
    var selectedHardwareSource by remember { mutableStateOf(DEVICE_PLACEHOLDER) }
    var selectedEnvSource by remember { mutableStateOf(ENV_PLACEHOLDER) }
    var selectedAppSource by remember { mutableStateOf(APP_PLACEHOLDER) }

    val notificationQueue = remember { mutableStateListOf<Notification>() }
    var activeNotification by remember { mutableStateOf<Notification?>(null) }
    var notificationTrigger by remember { mutableStateOf(0) }

    var hasSelectedDevice by remember { mutableStateOf(false) }
    var hasSelectedEnv by remember { mutableStateOf(false) }
    var hasSelectedApp by remember { mutableStateOf(false) }

    var devicesList by remember { mutableStateOf(emptyList<String>()) }

    var selectedDeviceId by remember { mutableStateOf("") }
    var previousDeviceCount by remember { mutableStateOf(-1) }

    val isDeviceSelected = (hasSelectedDevice || selectedDeviceId.isNotEmpty()) && devicesList.isNotEmpty()

    var selectedEnvValue by remember { mutableStateOf("") }

    var currentNotifState by remember { mutableStateOf(NotificationState.DISABLED) }
    var currentEnvList by remember { mutableStateOf(mapOf<String, String>()) }
    var refreshNotifCounter by remember { mutableStateOf(0) }
    var selectedAppKey by remember { mutableStateOf("") }

    var installedApps by remember { mutableStateOf(listOf<InstalledApp>()) }
    var availableAppList by remember { mutableStateOf(mapOf<String, String>()) }

    val tutorialTargets = remember { mutableStateMapOf<TutorialStep, Rect>() }
    val tutorialSteps = remember {
        TutorialStep.entries.filter { it != TutorialStep.DONE }
    }
    var currentStepIndex by remember { mutableStateOf(0) }
    var tutorialStep by remember { mutableStateOf<TutorialStep?>(null) }
    var removeZipAfterInstall by remember { mutableStateOf(false) }

    var showNavBar by remember { mutableStateOf(false) }

    var cachedDeviceName by remember { mutableStateOf("") }
    var isConsoleExpanded by remember { mutableStateOf(true) }
    var consoleMessages by remember { mutableStateOf(listOf<ConsoleInstallMessage>()) }
    var showClearMessage by remember { mutableStateOf(false) }

    //caching info
    var cachedSdk by remember { mutableStateOf(0) }
    var cachedDeviceType by remember { mutableStateOf(DeviceType.MOBILE) }

    fun resetEnv() {
        selectedEnvSource = ENV_PLACEHOLDER
        selectedEnvValue = ""
        hasSelectedEnv = false
    }

    fun clearAppSelection(placeholder: String = APP_PLACEHOLDER) {
        selectedAppKey = ""
        selectedAppSource = placeholder
        hasSelectedApp = false
        currentEnvList = emptyMap()
        resetEnv()
    }

    fun resetAppAndEnvState() {
        clearAppSelection()
        installedApps = emptyList()
        availableAppList = emptyMap()
        selectedCommand = null
    }

    fun resetDeviceSelection() {
        selectedDeviceId = ""
        selectedHardwareSource = DEVICE_PLACEHOLDER
        hasSelectedDevice = false
        cachedDeviceType = DeviceType.MOBILE
        cachedSdk = 0
        resetAppAndEnvState()
    }

    fun enqueueNotifications(vararg notifications: Notification) {
        if (notifications.isEmpty()) return
        if (activeNotification == null) {
            activeNotification = notifications.first()
            notificationTrigger++
            notificationQueue.addAll(notifications.drop(1))
        } else {
            notificationQueue.addAll(notifications)
        }
    }

    fun buildEnvList(app: InstalledApp): Map<String, String> {
        val envMap = mutableMapOf<String, String>()
        app.prodPackage?.let { envMap["PRODUCTION"] = it }
        app.betaPackage?.let { envMap["BETA"] = it }
        app.stagPackage?.let { envMap["STAGING"] = it }
        return envMap
    }

    fun selectApp(app: InstalledApp) {
        val envList = buildEnvList(app)
        selectedAppKey = app.key
        selectedAppSource = app.displayName
        hasSelectedApp = true
        currentEnvList = envList
        if (envList.size == 1) {
            val env = envList.entries.first()
            selectedEnvSource = env.key
            selectedEnvValue = env.value
            hasSelectedEnv = true
        } else {
            resetEnv()
        }
    }

    // Auto-select when exactly one device is connected
    LaunchedEffect(devicesList) {
        if (devicesList.size == 1 && selectedDeviceId.isEmpty()) {
            val singleDevice = devicesList.first()
            selectedHardwareSource = singleDevice
            selectedDeviceId = singleDevice
            hasSelectedDevice = true
        }
    }

    fun refreshEnvForSelectedApp() {
        val app = installedApps.find { it.key == selectedAppKey }
        if (app == null) {
            clearAppSelection(
                if (selectedAppSource == NO_APPS_PLACEHOLDER) NO_APPS_PLACEHOLDER else APP_PLACEHOLDER
            )
            return
        }

        val newEnvList = buildEnvList(app)
        currentEnvList = newEnvList

        val validEnv = newEnvList.entries.find { it.value == selectedEnvValue }
        when {
            newEnvList.size == 1 -> {
                val env = newEnvList.entries.first()
                selectedEnvSource = env.key
                selectedEnvValue = env.value
                hasSelectedEnv = true
            }

            selectedEnvValue.isNotEmpty() && validEnv != null -> {
                selectedEnvSource = validEnv.key
                hasSelectedEnv = true
            }

            else -> resetEnv()
        }
    }

    // Single place that syncs the env list with the selected app
    LaunchedEffect(installedApps, selectedAppKey) {
        refreshEnvForSelectedApp()
    }

    suspend fun refreshInstalledApps() {
        if (selectedDeviceId.isEmpty()) return

        val newInstalledApps = withContext(Dispatchers.IO) {
            AppDetection.getInstalledApps(selectedDeviceId, cachedDeviceType)
        }

        if (installedApps == newInstalledApps) return

        installedApps = newInstalledApps
        availableAppList = newInstalledApps.associate { it.key to it.displayName }

        val stillInstalled = newInstalledApps.find { it.key == selectedAppKey }

        when {
            stillInstalled != null -> {
                selectedAppSource = stillInstalled.displayName
                hasSelectedApp = true
            }

            newInstalledApps.size == 1 -> selectApp(newInstalledApps.first())

            newInstalledApps.isEmpty() -> clearAppSelection(NO_APPS_PLACEHOLDER)

            else -> clearAppSelection()
        }
        // Env list is refreshed by LaunchedEffect(installedApps, selectedAppKey)
    }

    LaunchedEffect(selectedDeviceId) {
        if (selectedDeviceId.isEmpty()) return@LaunchedEffect

        val info = DeviceInfoCache.fetchAndCache(selectedDeviceId)
        cachedDeviceName = info.name
        cachedSdk = info.sdk
        cachedDeviceType = info.deviceType

        refreshInstalledApps()

        while (true) {
            delay(1500)
            refreshInstalledApps()
        }
    }

    LaunchedEffect(selectedDeviceId, selectedEnvValue, refreshNotifCounter, cachedDeviceType, cachedSdk) {
        if (cachedDeviceType == DeviceType.TV) return@LaunchedEffect
        if (selectedDeviceId.isEmpty() || selectedEnvValue.isEmpty()) return@LaunchedEffect

        if (refreshNotifCounter > 0) delay(500)   // give pm grant/revoke time to apply
        currentNotifState = withContext(Dispatchers.IO) {
            getNotificationState(selectedDeviceId, selectedEnvValue, cachedSdk)
        }
    }

    // Reset selected command when device/env changes; otherwise keep the
    // notification command in sync with the current notification state.
    var lastSelection by remember { mutableStateOf(selectedDeviceId to selectedEnvValue) }
    LaunchedEffect(currentNotifState, selectedDeviceId, selectedEnvValue) {
        val selection = selectedDeviceId to selectedEnvValue
        if (selection != lastSelection) {
            lastSelection = selection
            selectedCommand = null
            return@LaunchedEffect
        }
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

    // Device polling (first scan runs immediately, off the UI thread)
    LaunchedEffect(Unit) {
        while (true) {
            val raw = withContext(Dispatchers.IO) { AdbDevices.getDevicesList() }
            AdbConnectionManager.syncWith(raw)
            val newDevices = AdbConnectionManager.visibleDevices(raw)
            devicesList = newDevices
            val currentCount = newDevices.size

            if (previousDeviceCount == 1 && currentCount > 1) {
                resetDeviceSelection()
            }

            if (selectedDeviceId.isNotEmpty() && selectedDeviceId !in newDevices) {
                DeviceInfoCache.invalidate(selectedDeviceId)
                AppDetection.forget(selectedDeviceId)
                resetDeviceSelection()
            }

            if (currentCount != previousDeviceCount && previousDeviceCount != -1) {
                if (currentCount < previousDeviceCount) {
                    val remaining = when (currentCount) {
                        0 -> Notification("No devices connected", NotificationType.ERROR)
                        1 -> Notification("1 device remaining", NotificationType.SUCCESS)
                        else -> Notification("$currentCount devices remaining", NotificationType.SUCCESS)
                    }
                    enqueueNotifications(
                        Notification("Device removed", NotificationType.WARNING),
                        remaining
                    )
                } else {
                    enqueueNotifications(
                        if (currentCount == 1) Notification("Device connected", NotificationType.SUCCESS)
                        else Notification("$currentCount devices connected", NotificationType.SUCCESS)
                    )
                }
            }
            previousDeviceCount = currentCount
            delay(1000)
        }
    }

    LaunchedEffect(notificationTrigger) {
        if (notificationTrigger > 0 && activeNotification != null) {
            delay(2500)
            activeNotification = null
            if (notificationQueue.isNotEmpty()) {
                delay(300)
                activeNotification = notificationQueue.removeAt(0)
                notificationTrigger++
            }
        }
    }

    val emptyStateText = when {
        showClearMessage -> "Console cleared"
        consoleMessages.isEmpty() -> "Awaiting output from console..."
        else -> ""
    }

    val hasAnyDevice = devicesList.isNotEmpty() || AdbConnectionManager.disconnected.isNotEmpty()

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
                            visible = isDeviceSelected,
                            enter = fadeIn(animationSpec = tween(300)),
                            exit = fadeOut(animationSpec = tween(300))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(Modifier.width(12.dp))
                                val tutorialInteraction = remember { MutableInteractionSource() }
                                val isTutorialHovered by tutorialInteraction.collectIsHoveredAsState()

                                IconButton(
                                    onClick = {
                                        currentStepIndex = 0
                                        tutorialStep = tutorialSteps.getOrNull(0)
                                    },
                                    interactionSource = tutorialInteraction,
                                    modifier = Modifier.size(20.dp)
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
                    }

                    AnimatedVisibility(
                        visible = isDeviceSelected,
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
                            toolbarTooltip("Run CI/CD Workflow") {
                                workflowButton(
                                    onWorkflowTriggered = { result ->
                                        val output = formatWorkflowOutput(result)
                                        consoleMessages = consoleMessages + ConsoleInstallMessage.Text(output)
                                        showClearMessage = false
                                    },
                                    onConsoleOutput = { newMessage ->
                                        consoleMessages = consoleMessages + newMessage
                                    },
                                    modifier = Modifier.height(48.dp)
                                )
                            }

                            toolbarTooltip("Select a VPN gateway") {
                                vpnButton(
                                    modifier = Modifier.height(48.dp),
                                    onCommandExecuted = { output, _ ->
                                        consoleMessages = consoleMessages + ConsoleInstallMessage.Text(output)
                                    },
                                    deviceId = selectedDeviceId,
                                    deviceName = cachedDeviceName,
                                    onCommandReplaced = { output, _ ->
                                        consoleMessages = consoleMessages.dropLast(1) + ConsoleInstallMessage.Text(output)
                                    }
                                )
                            }

                            toolbarTooltip("Install a build from apk or zip file") {
                                zipApkInstaller(
                                    deviceId = selectedDeviceId,
                                    enabled = isDeviceSelected,
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

                            toolbarTooltip("App Settings") {
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
                    targetState = hasAnyDevice,
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
                                Box(modifier = Modifier.weight(1f)) {
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
                                            enabled = isDeviceSelected && availableAppList.isNotEmpty(),
                                            selectedHardwareSource = selectedHardwareSource,
                                            hasSelectedDevice = hasSelectedDevice,
                                            hasSelectedApp = hasSelectedApp,
                                            hasSelectedEnv = hasSelectedEnv,
                                            commandOptions = availableAppList,
                                            selectedAppKey = selectedAppKey,
                                            selectedAppSource = selectedAppSource,
                                            selectedPackage = selectedEnvValue,
                                            onCommandSelected = { appKey ->
                                                installedApps.find { it.key == appKey }?.let { selectApp(it) }
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
                                    targetState = hasSelectedApp && currentEnvList.isNotEmpty(),
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
                                targetState = hasSelectedApp && hasSelectedEnv,
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
                                        isAppSelected = hasSelectedApp
                                    )
                                } else {
                                    val quickCommands = remember(
                                        selectedDeviceId, selectedEnvValue, selectedAppKey,
                                        selectedEnvSource, currentNotifState, cachedSdk, cachedDeviceType
                                    ) {
                                        commandOptions(
                                            deviceId = selectedDeviceId,
                                            envPrefix = selectedEnvValue,
                                            appKey = selectedAppKey,
                                            envName = selectedEnvSource,
                                            notifState = currentNotifState,
                                            sdk = cachedSdk,
                                            deviceType = cachedDeviceType
                                        )
                                    }
                                    commandDropdown(
                                        enabled = isDeviceSelected && hasSelectedApp && hasSelectedEnv,
                                        commandOptions = quickCommands,
                                        deviceType = cachedDeviceType,
                                        onCommandSelected = { selectedCommand = it },
                                        onCommandExecuted = { newMessage, _ ->
                                            consoleMessages = consoleMessages + newMessage
                                            showClearMessage = false
                                            refreshNotifCounter++
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
                                        onCommandReplaced = { newMessage, _ ->
                                            consoleMessages = consoleMessages.dropLast(1) + newMessage
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
                                enabled = isDeviceSelected,
                                deviceId = selectedDeviceId,
                                removeZipAfterInstall = removeZipAfterInstall,
                                onNewMessage = { newMessage ->
                                    showClearMessage = false
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
                    version = "3.0.1",
                    githubUrl = "https://github.com/astar1337/adbcommandhelper"
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
            visible = isDeviceSelected,
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
                showOverview = cachedDeviceType == DeviceType.MOBILE
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
                    onSkip = { tutorialStep = null },
                    onFinish = { tutorialStep = null },
                    tooltipAtTop = step == TutorialStep.CONSOLE || step == TutorialStep.QUICK_ACTIONS
                )
            }
        }
    }
}

