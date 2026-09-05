package ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.AddLocation
import androidx.compose.material.icons.rounded.DirectionsRun
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import commands.Emoji
import kotlinx.coroutines.launch
import ui.components.theme.AppFontFamily
import ui.components.theme.DropdownColors
import ui.components.theme.gradientColorsIcons
import utils.helpers.AdbDevices.runCommandFromMap
import androidx.compose.ui.window.Dialog
import commands.QuickActionType
import commands.mpRegion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import utils.helpers.*
import utils.helpers.Environment
import utils.helpers.ConsoleInstallMessage
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import kotlinx.coroutines.Job

data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val commandId: String? = null,
    val actionType: QuickActionType = QuickActionType.COMMAND
)

private val quickActions = listOf(
    QuickAction("Launch", Icons.Default.RocketLaunch, "launch"),
    QuickAction("Force Stop", Icons.Default.Block, "force_stop"),
    QuickAction("Clear Data", Icons.Default.CleaningServices, "clear_data"),
    QuickAction("Delete", Icons.Default.Delete, "delete"),
    QuickAction("Notifications", Icons.Default.Notifications, "notifications"),
    QuickAction("Marco Polo", Icons.Rounded.AddLocation, "marco_polo"),
    QuickAction(
        label = "Automation",
        icon = Icons.Rounded.DirectionsRun,
        commandId = null,
        actionType = QuickActionType.AUTOMATION
    ),
)

fun findCommandByAction(action: QuickAction, commands: List<Command>): Command? {
    if (action.commandId == null) return null
    return commands.find { it.id == action.commandId }
}

@Composable
fun commandDropdown(
    commandOptions: List<Command>,
    onCommandSelected: (Command) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCommandExecuted: (ConsoleInstallMessage, Color?) -> Unit,
    onCommandReplaced: (ConsoleInstallMessage, Color?) -> Unit,
    selectedDeviceId: String,
    selectedAppSource: String,
    selectedAppKey: String,
    selectedEnvSource: String,
    notifState: NotificationState = NotificationState.DISABLED,
    selectedEnvValue: String,

) {
    val scope = rememberCoroutineScope()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var pendingDeleteCommand by remember { mutableStateOf<Command?>(null) }

    var showMarcoPoloDialog by remember { mutableStateOf(false) }
    var storedOriginalGuid by remember { mutableStateOf("") }
    var currentDeviceGuid by remember { mutableStateOf("") }
    var showAutomationDialog by remember { mutableStateOf(false) }
    var showClearPhoneDialog by remember { mutableStateOf(false) }
    var clearPhoneEnvironment by remember { mutableStateOf(Environment.STAGING) }

    //maestro
    var showMaestroLoginDialog by remember { mutableStateOf(false) }

    //app key for maestro
    val maestroApp = remember(selectedAppKey) {
        MaestroApp.fromKey(selectedAppKey) ?: MaestroApp.DAZN
    }
    //automation progress
    var runningAutomationId by remember { mutableStateOf<String?>(null) }
    var automationJob by remember { mutableStateOf<Job?>(null) }


    fun executeCommand(command: Command) {
        scope.launch {
            val result = runCommandFromMap(command.adbCommand)

            val noisePrefixes = listOf(
                "Starting:", "Warning:", "Note:", "Success",
                "adb:", "package:", "SCRCPY launched"
            )

            val cleanResult = if (result.isSuccess) {
                val meaningful = result.rawOutput
                    .lines()
                    .filterNot { line ->
                        val t = line.trim()
                        t.isEmpty() || noisePrefixes.any { t.startsWith(it) }
                    }
                    .joinToString("\n")
                    .trim()

                if (meaningful.isEmpty())
                    "Command executed successfully"
                else
                    "Success"
            } else {
                "Error: ${result.errorMessage ?: "Command failed ${Emoji.NO_DEVICE}"}"
            }

            val formattedMessage = FormatOutput(
                device = selectedDeviceId,
                selectedApp = selectedAppSource,
                environment = selectedEnvSource,
                commandLabel = command.displayLabel,
                commandExecuted = command.adbCommand,
                result = cleanResult,
                appNumber = getAppVersion(selectedDeviceId, selectedEnvValue)
            ).toConsoleMessage()

            onCommandExecuted(formattedMessage,null)
        }
    }
    fun startAutomation(optionId: String, launcher: () -> Job) {
        if (runningAutomationId != null) return
        runningAutomationId = optionId
        val job = launcher()
        automationJob = job
        scope.launch {
            job.join()
            runningAutomationId = null
            automationJob = null
        }
    }
    val isStagingEnv = remember(selectedEnvSource) {
        selectedEnvSource.equals("STAGING", ignoreCase = true)
    }

    marcoPoloDialog(
        showDialog = showMarcoPoloDialog,
        onDismiss = { showMarcoPoloDialog = false },
        currentGuid = currentDeviceGuid,
        onRegionConnect = { guid, regionName ->
            val adb = AppConfig.adbPath
            val guidCommand = Command(
                id = "guid",
                labelParts = listOf(
                    CommandLabel.Text("Marco Polo → $regionName")
                ),
                adbCommand = "$adb -s $selectedDeviceId shell am start -a android.intent.action.VIEW -n $selectedEnvValue/com.dazn.splash.view.SplashScreenActivity -e DEVICE_GUID $guid"
            )
            onCommandSelected(guidCommand)
            executeCommand(guidCommand)
        },
        onRegionDisconnect = {
            val adb = AppConfig.adbPath
            val clearCommand = Command(
                id = "marco_polo_clear",
                labelParts = listOf(
                    CommandLabel.Text("Marco Polo → Clearing data...")
                ),
                adbCommand = "$adb -s $selectedDeviceId shell pm clear $selectedEnvValue"
            )

            val launchCommand = Command(
                id = "marco_polo_relaunch",
                labelParts = listOf(
                    CommandLabel.Text("Marco Polo → Reloading app")
                ),
                adbCommand = "$adb -s $selectedDeviceId shell am start -n $selectedEnvValue/com.dazn.splash.view.SplashScreenActivity"
            )
            executeCommand(clearCommand)
            executeCommand(launchCommand)
            onCommandSelected(launchCommand)
        }
    )

    automationDialog(
        showDialog = showAutomationDialog,
        isStaging = isStagingEnv,
        runningOptionId = runningAutomationId,
        onDismiss = { showAutomationDialog = false },
        onCancelRun = { automationJob?.cancel() },
        onOptionSelected = { option ->
            when (option.id) {
                "login" -> {
                    showAutomationDialog = false
                    showMaestroLoginDialog = true
                }

                "logout" -> startAutomation("logout") {
                    runMaestroFlow(
                        scope = scope,
                        flowFileName = "logout",
                        label = "Logout",
                        app = maestroApp,
                        appId = selectedEnvValue,
                        onCommandExecuted = onCommandExecuted,
                        onCommandReplaced = onCommandReplaced
                    )
                }

                "signup_freemium" -> startAutomation("signup_freemium") {
                    runMaestroFlow(
                        scope = scope,
                        flowFileName = "create_account",
                        label = "Create Account",
                        app = maestroApp,
                        appId = selectedEnvValue,
                        onCommandExecuted = onCommandExecuted,
                        onCommandReplaced = onCommandReplaced
                    )
                }

                "clear_stag_phone" -> {
                    showAutomationDialog = false
                    clearPhoneEnvironment = Environment.STAGING
                    showClearPhoneDialog = true
                }

                "clear_prod_phone" -> {
                    showAutomationDialog = false
                    clearPhoneEnvironment = Environment.PRODUCTION
                    showClearPhoneDialog = true
                }
            }
        }
    )
    clearPhoneDialog(
        showDialog = showClearPhoneDialog,
        environment = clearPhoneEnvironment,
        onDismiss = { showClearPhoneDialog = false },
        onBack = {
            showClearPhoneDialog = false
            showAutomationDialog = true
        },
        onClearPhone = { fullNumber, env ->
            showClearPhoneDialog = false
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    removePhoneNumberCurl(
                        phoneNumber = fullNumber,
                        environment = env
                    )
                }
                val envLabel = if (env == Environment.STAGING) "STAG" else "PROD"

                if (result.success) {
                    onCommandExecuted(ConsoleInstallMessage.Text("Phone number $fullNumber cleared from $envLabel account (${result.responseCode})"), null)
                } else {
                    onCommandExecuted(ConsoleInstallMessage.Text("Failed to clear $fullNumber from $envLabel account — ${result.responseCode}: Number not found"), null)
                }

            }
        }
    )
    maestroLoginDialog(
        showDialog = showMaestroLoginDialog,
        onDismiss = {
            showMaestroLoginDialog = false
            showAutomationDialog = true
        },
        onRunTest = { email, password ->
            showMaestroLoginDialog = false
            showAutomationDialog = true
            val maestroLines = mutableListOf<String>()
            maestroLines.add("Maestro: Running login flow...")
            onCommandExecuted(ConsoleInstallMessage.Text(maestroLines.joinToString("\n")), Color(0xFFA855F7))
            startAutomation("login") {
                runMaestroFlow(
                    scope = scope,
                    flowFileName = "login_valid",
                    label = "Login",
                    app = maestroApp,
                    appId = selectedEnvValue,
                    email = email,
                    password = password,
                    onCommandExecuted = onCommandExecuted,
                    onCommandReplaced = onCommandReplaced
                )
            }
        }
    )

    fun getNotifLabel(): String {
        return when (notifState) {
            NotificationState.ENABLED -> "Notifs ON"
            NotificationState.DISABLED -> "Notifs OFF"
        }
    }

    fun getNotifStatusColor(): Color {
        return when (notifState) {
            NotificationState.ENABLED -> Color(0xFF22C55E)
            NotificationState.DISABLED -> Color(0xFFEF4444)
        }
    }

    Card(
        modifier = modifier,
        backgroundColor = DropdownColors.cardBg,
        border = BorderStroke(1.dp, DropdownColors.border),
        shape = RoundedCornerShape(12.dp),

    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Checklist,
                    contentDescription = null,
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
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "App Quick Actions",
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (enabled) DropdownColors.textPrimary else DropdownColors.textDim
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                quickActions.forEach { action ->
                    val command = findCommandByAction(action, commandOptions)
                    val isNotifAction = action.commandId == "notifications"
                    val isDeleteAction = action.commandId == "delete"
                    val isMarcoPolo = action.commandId == "marco_polo"
                    val isAutomation = action.actionType == QuickActionType.AUTOMATION
                    val isAvailable = when {
                        isAutomation -> enabled
                        else -> enabled && command != null
                    }

                    quickActionButton(
                        label = if (isNotifAction) getNotifLabel() else action.label,
                        icon = action.icon,
                        enabled = isAvailable,
                        statusColor = if (isNotifAction) getNotifStatusColor() else null,
                        textColor = when {
                            isMarcoPolo -> Color(0xFF3B82F6)
                            isAutomation -> Color(0xFFA855F7)
                            else -> null
                        },
                        onClick = {
                            when {
                                isAutomation -> {
                                    showAutomationDialog = true
                                }
                                else -> {
                                    command?.let {
                                        when {
                                            isDeleteAction -> {
                                                pendingDeleteCommand = it
                                                showDeleteConfirmDialog = true
                                            }
                                            isMarcoPolo -> {
                                                scope.launch {
                                                    val fetchedGuid = withContext(Dispatchers.IO) {
                                                        getAppGUID(selectedDeviceId, selectedEnvValue)
                                                    }
                                                    currentDeviceGuid = fetchedGuid
                                                    val isAlreadyRegionGuid = mpRegion.any { region -> region.guid == fetchedGuid }
                                                    if (!isAlreadyRegionGuid) {
                                                        storedOriginalGuid = fetchedGuid
                                                    }
                                                    showMarcoPoloDialog = true
                                                }
                                            }
                                            else -> {
                                                onCommandSelected(it)
                                                executeCommand(it)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (showDeleteConfirmDialog && pendingDeleteCommand != null) {
                    Dialog(onDismissRequest = {
                        showDeleteConfirmDialog = false
                        pendingDeleteCommand = null
                    }) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DropdownColors.cardBg,
                            elevation = 16.dp,
                            modifier = Modifier.width(400.dp)
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
                                            text = "Confirm Delete",
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = DropdownColors.textPrimary
                                        )
                                        IconButton(
                                            onClick = {
                                                showDeleteConfirmDialog = false
                                                pendingDeleteCommand = null
                                            },
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
                                        text = "This action cannot be undone",
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Light,
                                        fontSize = 12.sp,
                                        color = DropdownColors.textMuted
                                    )
                                }

                                Divider(color = DropdownColors.divider)

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "Are you sure you want to delete $selectedEnvSource $selectedAppSource?",
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = DropdownColors.textSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }

                                Divider(color = DropdownColors.divider)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DropdownColors.footerBg)
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val cancelInteraction = remember { MutableInteractionSource() }
                                    val isCancelHovered by cancelInteraction.collectIsHoveredAsState()

                                    TextButton(
                                        onClick = {
                                            showDeleteConfirmDialog = false
                                            pendingDeleteCommand = null
                                        },
                                        interactionSource = cancelInteraction,
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = if (isCancelHovered) DropdownColors.textSecondary else DropdownColors.textMuted
                                        )
                                    ) {
                                        Text(
                                            "Cancel",
                                            fontFamily = AppFontFamily,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    val deleteInteraction = remember { MutableInteractionSource() }
                                    val isDeleteHovered by deleteInteraction.collectIsHoveredAsState()

                                    Button(
                                        onClick = {
                                            pendingDeleteCommand?.let {
                                                onCommandSelected(it)
                                                executeCommand(it)
                                            }
                                            showDeleteConfirmDialog = false
                                            pendingDeleteCommand = null
                                        },
                                        interactionSource = deleteInteraction,
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = if (isDeleteHovered) Color(0xFFDC2626) else Color(0xFFEF4444),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isDeleteHovered) Color(0xFFDC2626) else Color(0xFFEF4444)
                                        ),
                                        elevation = ButtonDefaults.elevation(
                                            defaultElevation = 0.dp,
                                            pressedElevation = 0.dp,
                                            hoveredElevation = 0.dp
                                        ),
                                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Delete",
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
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

@Composable
private fun quickActionButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    statusColor: Color? = null,
    onClick: () -> Unit,
    textColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    !enabled -> Color.Transparent
                    isHovered -> DropdownColors.rowHover
                    else -> DropdownColors.fieldBg
                }
            )
            .border(
                1.dp,
                when {
                    !enabled -> DropdownColors.fieldBorder.copy(alpha = 0.3f)
                    isHovered -> DropdownColors.fieldBorderHover
                    else -> DropdownColors.fieldBorder
                },
                RoundedCornerShape(8.dp)
            )
            .then(if (enabled) Modifier.hoverable(interaction) else Modifier)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = when {
                !enabled -> DropdownColors.textDim.copy(alpha = 0.4f)
                isHovered -> DropdownColors.textPrimary
                else -> DropdownColors.textMuted
            },
            modifier = Modifier.size(18.dp)
        )

        Text(
            text = label,
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = when {
                statusColor != null && enabled -> statusColor
                textColor != null && enabled -> textColor
                !enabled -> DropdownColors.textDim.copy(alpha = 0.4f)
                isHovered -> DropdownColors.textPrimary
                else -> DropdownColors.textMuted
            }
        )
    }
}
data class AutomationOption(
    val label: String,
    val icon: ImageVector,
    val description: String,
    val id: String,
    val flowFile: String? = null,
    val prodOnly: Boolean = false,
    val usesMaestro: Boolean = true

)
private val automationOptions = listOf(
    AutomationOption(
        label = "Log In",
        icon = Icons.Default.Login,
        description = "Log in to the app with your credentials",
        id = "login",
        prodOnly = true

    ),
    AutomationOption(
        label = "Log Out",
        icon = Icons.Default.Logout,
        description = "Log out of the app",
        id = "logout",
        prodOnly = true
    ),
    AutomationOption(
        label = "Clear STAG Phone number",
        icon = Icons.Default.PhoneAndroid,
        description = "Clear phone number from a staging account",
        id = "clear_stag_phone",
        usesMaestro = false
    ),
    AutomationOption(
        label = "Clear PROD Phone number",
        icon = Icons.Default.PhoneAndroid,
        description = "Clear phone number from a production account",
        id = "clear_prod_phone",
        usesMaestro = false

    ),
    AutomationOption(
        label = "Sign-up FREEMIUM",
        icon = Icons.Default.PersonAdd,
        description = "Create a new freemium account",
        id = "signup_freemium",
        prodOnly = true
    ),
)

@Composable
fun automationDialog(
    showDialog: Boolean,
    isStaging: Boolean = false,
    runningOptionId: String? = null,
    onDismiss: () -> Unit,
    onCancelRun: () -> Unit,
    onOptionSelected: (AutomationOption) -> Unit
) {
    if (!showDialog) return

    val infiniteTransition = rememberInfiniteTransition()
    val spinnerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DropdownColors.cardBg,
            elevation = 16.dp,
            modifier = Modifier.width(420.dp)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.DirectionsRun,
                                contentDescription = null,
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Maestro Automation",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = DropdownColors.textPrimary
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
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
                        text = when {
                            runningOptionId != null -> "Flow running — press ✕ to cancel"
                            isStaging -> "UI test flows are restricted to production builds"
                            else -> "Select an automation to run"
                        },
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 12.sp,
                        color = when {
                            runningOptionId != null -> DropdownColors.orange
                            isStaging -> DropdownColors.textMuted
                            else -> DropdownColors.textMuted
                        }
                    )
                }

                Divider(color = DropdownColors.divider)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    automationOptions.forEach { option ->
                        val interaction = remember { MutableInteractionSource() }
                        val isHovered by interaction.collectIsHoveredAsState()

                        val isRunning = runningOptionId == option.id
                        val isEnvBlocked = option.prodOnly && isStaging
                        val isLocked = isEnvBlocked ||
                                (option.usesMaestro && runningOptionId != null && !isRunning)
                        val isInteractive = !isLocked && !isRunning

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (isLocked) 0.4f else 1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isHovered && isInteractive) DropdownColors.rowHover
                                    else Color.Transparent
                                )
                                .hoverable(interaction, enabled = isInteractive)
                                .clickable(enabled = isInteractive) { onOptionSelected(option) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = null,
                                    tint = if (isHovered && isInteractive) Color(0xFFA855F7)
                                    else DropdownColors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = option.label,
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isHovered && isInteractive) DropdownColors.textPrimary
                                        else DropdownColors.textSecondary
                                    )
                                    Text(
                                        text = when {
                                            isRunning -> "Running…"
                                            isEnvBlocked -> "Not available in the staging environment"
                                            else -> option.description
                                        },
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Light,
                                        fontSize = 11.sp,
                                        color = if (isRunning) DropdownColors.orange
                                        else DropdownColors.textMuted
                                    )
                                }
                            }

                            when {
                                isEnvBlocked -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(DropdownColors.textMuted.copy(alpha = 0.15f))
                                            .border(
                                                1.dp,
                                                DropdownColors.textMuted.copy(alpha = 0.3f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "PROD ONLY",
                                            fontFamily = AppFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.sp,
                                            color = DropdownColors.textMuted,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }

                                isRunning -> {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(20.dp)) {
                                            CircularProgressIndicator(
                                                progress = 0.7f,
                                                color = DropdownColors.orange,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .rotate(spinnerRotation)
                                            )
                                        }

                                        val cancelInteraction = remember { MutableInteractionSource() }
                                        val isCancelHovered by cancelInteraction.collectIsHoveredAsState()

                                        IconButton(
                                            onClick = onCancelRun,
                                            interactionSource = cancelInteraction,
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Cancel run",
                                                tint = if (isCancelHovered) DropdownColors.required
                                                else DropdownColors.textMuted,
                                                modifier = Modifier.size(14.dp)
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
}