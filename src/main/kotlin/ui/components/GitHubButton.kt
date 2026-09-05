package ui.components

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import commands.WorkflowConfig
import commands.WorkflowResult
import commands.workflowConfigs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ui.components.theme.AppFontFamily
import ui.components.theme.gradientColors
import utils.helpers.*
import ui.components.theme.DropdownColors
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import commands.WorkflowInput
import utils.helpers.formatDate
import utils.helpers.GitHubClient



@Composable
fun workflowButton(
    workflows: List<WorkflowConfig> = workflowConfigs,
    onWorkflowTriggered: (WorkflowResult) -> Unit = {},
    onConsoleOutput: (ConsoleInstallMessage) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var isOpen by remember { mutableStateOf(false) }
    val selectedWorkflow = workflows.first()
    var selectedBranch by remember { mutableStateOf("") }
    var fieldValues by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isRunning by remember { mutableStateOf(false) }
    var isHovering by remember { mutableStateOf(false) }

    var showAuthError by remember { mutableStateOf<GitHubAuthError?>(null) }
    var isCheckingAuth by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var isBranchHistoryLoading by remember { mutableStateOf(false) }
    var branchRunHistory by remember { mutableStateOf<List<BranchRunHistoryItem>>(emptyList()) }
    var hasFetchedBranchHistory by remember { mutableStateOf(false) }
    var branchHistoryCache by remember { mutableStateOf<Map<String, List<BranchRunHistoryItem>>>(emptyMap()) }
    var branchCache by remember { mutableStateOf<List<GitHubBranch>>(emptyList()) }


    var branches by remember { mutableStateOf<List<GitHubBranch>>(emptyList()) }
    var isBranchesLoading by remember { mutableStateOf(false) }
    var branchSearchQuery by remember { mutableStateOf("") }
    var authToken by remember { mutableStateOf<String?>(null) }

    var selectedTab by remember { mutableStateOf(0) }

    var isDownloading by remember { mutableStateOf<Long?>(null) }
    var downloadProgress by remember { mutableStateOf("") }
    var downloadFailed by remember { mutableStateOf<Long?>(null) }
    var lastDownloadArtifact by remember { mutableStateOf<GitHubArtifact?>(null) }

    val trackedRunsFlow by WorkflowMonitor.runsFlow.collectAsState()
    var trackedRuns by remember { mutableStateOf(WorkflowMonitor.getTrackedRuns()) }

    var cancellingRunId by remember { mutableStateOf<Long?>(null) }
    var retriggeringRunId by remember { mutableStateOf<Long?>(null) }
    var latestRunId by remember { mutableStateOf<Long?>(null) }
    var dynamicInputs by remember { mutableStateOf<List<WorkflowInput>>(emptyList()) }
    var isInputsLoading by remember { mutableStateOf(false) }
    var branchHistoryJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    var isLinkVisible by remember { mutableStateOf(false) }


    LaunchedEffect(trackedRunsFlow) {
        trackedRuns = trackedRunsFlow
    }

    LaunchedEffect(Unit) {
        WorkflowMonitor.setOnStatusChanged { updatedRun ->
            trackedRuns = WorkflowMonitor.getTrackedRuns()
            val statusMsg = when (updatedRun.status) {
                WorkflowRunStatus.COMPLETED -> "Result: Workflow completed successfully on branch: ${updatedRun.branch} (${updatedRun.brand})"
                WorkflowRunStatus.FAILED -> "Result: Workflow failed on branch: ${updatedRun.branch} (${updatedRun.brand})"
                WorkflowRunStatus.CANCELLED -> "Result: Workflow cancelled on branch: ${updatedRun.branch} (${updatedRun.brand})"
                else -> return@setOnStatusChanged
            }
            onConsoleOutput(ConsoleInstallMessage.Text(statusMsg))
        }
    }

    LaunchedEffect(isOpen) {
        if (isOpen) {
            trackedRuns = WorkflowMonitor.getTrackedRuns()
        }
    }
    LaunchedEffect(selectedTab, selectedBranch) {
        val token = authToken
        if (selectedTab == 2 && selectedBranch.isNotEmpty() && token != null) {
            val cached = branchHistoryCache[selectedBranch]
            if (cached != null) {
                branchRunHistory = cached
                hasFetchedBranchHistory = true
                isBranchHistoryLoading = false
                branchHistoryJob?.cancel()
                branchHistoryJob = coroutineScope.launch {
                    val result = GitHubClient.fetchBranchRunHistory(token, selectedBranch)
                    branchRunHistory = result
                    branchHistoryCache = branchHistoryCache + (selectedBranch to result)
                    branchHistoryJob = null
                }
            } else {
                isBranchHistoryLoading = true
                branchHistoryJob?.cancel()
                branchHistoryJob = coroutineScope.launch {
                    val startTime = System.currentTimeMillis()
                    val result = GitHubClient.fetchBranchRunHistory(token, selectedBranch)
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed < 1500) delay(1500 - elapsed)
                    branchRunHistory = result
                    branchHistoryCache = branchHistoryCache + (selectedBranch to result)
                    isBranchHistoryLoading = false
                    hasFetchedBranchHistory = true
                    branchHistoryJob = null
                }
            }
        }
    }
    LaunchedEffect(selectedBranch) {
        if (branchHistoryCache[selectedBranch] == null) {
            hasFetchedBranchHistory = false
            branchRunHistory = emptyList()
        } else {
            branchRunHistory = branchHistoryCache[selectedBranch]!!
            hasFetchedBranchHistory = true
        }
        branchHistoryJob?.cancel()
        branchHistoryJob = null
    }

    LaunchedEffect(isOpen) {
        if (isOpen) {
            if (authToken != null) {
                if (branchCache.isNotEmpty()) {
                    branches = branchCache
                    branchSearchQuery = ""
                    if (selectedBranch.isEmpty() && branchCache.isNotEmpty()) {
                        selectedBranch = branchCache.firstOrNull { it.name == "main" }?.name
                            ?: branchCache.firstOrNull { it.name == "develop" }?.name
                                    ?: branchCache.first().name
                    }
                    isBranchesLoading = false
                } else {
                    isBranchesLoading = true
                    branches = emptyList()
                    branchSearchQuery = ""
                    val fetchedBranches = withContext(Dispatchers.IO) {
                        GitHubClient.fetchBranches(authToken!!)
                    }
                    branches = fetchedBranches
                    branchCache = fetchedBranches
                    if (selectedBranch.isEmpty() && fetchedBranches.isNotEmpty()) {
                        selectedBranch = fetchedBranches.firstOrNull { it.name == "main" }?.name
                            ?: fetchedBranches.firstOrNull { it.name == "develop" }?.name
                                    ?: fetchedBranches.first().name
                    }
                    isBranchesLoading = false
                }
            }
        }
    }

    val filteredBranches = remember(branchSearchQuery, branches) {
        when {
            branchSearchQuery.isBlank() -> branches
            else -> {
                val exactMatch = branches.filter {
                    it.name.equals(branchSearchQuery, ignoreCase = true)
                }
                exactMatch.ifEmpty {
                    branches.filter {
                        it.name.contains(branchSearchQuery, ignoreCase = true)
                    }
                }
            }
        }
    }

    LaunchedEffect(selectedBranch) {
        if (selectedBranch.isNotEmpty() && authToken != null) {
            isInputsLoading = true
            val inputs = GitHubClient.fetchWorkflowInputs(authToken!!, selectedBranch)
            dynamicInputs = inputs
            fieldValues = inputs.associate { it.id to it.default }
            isInputsLoading = false
        }
    }

    val allRequiredFilled = remember(fieldValues, dynamicInputs, selectedBranch) {
        selectedBranch.isNotEmpty() && dynamicInputs
            .filter { it.required }
            .all { (fieldValues[it.id] ?: "").isNotBlank() }
    }

    val borderAlpha by animateFloatAsState(
        targetValue = if (isHovering) 1f else 0.2f,
        animationSpec = tween(durationMillis = 300),
        label = "borderAlpha"
    )
    val infiniteTransition = rememberInfiniteTransition()
    val spinnerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val hasActiveRuns = trackedRuns.any {
        it.status == WorkflowRunStatus.QUEUED || it.status == WorkflowRunStatus.IN_PROGRESS
    }

    fun runAuthCheck(onReady: (String) -> Unit) {
        if (isCheckingAuth) return
        coroutineScope.launch {
            isCheckingAuth = true
            try {
                GitHubClient.clearTokenCache()
                val ghInstalled = withContext(Dispatchers.IO) { GitHubClient.isGhCliInstalled() }
                if (!ghInstalled) {
                    showAuthError = GitHubAuthError.CLI_NOT_INSTALLED
                    return@launch
                }
                val token = withContext(Dispatchers.IO) { GitHubClient.getAuthToken() }
                if (token == null) {
                    showAuthError = GitHubAuthError.NOT_LOGGED_IN
                    return@launch
                }
                val user = GitHubClient.validateToken(token)
                if (user == null) {
                    showAuthError = GitHubAuthError.TOKEN_INVALID
                    return@launch
                }
                onReady(token)
            } catch (e: Exception) {
                showAuthError = GitHubAuthError.TOKEN_INVALID
            } finally {
                isCheckingAuth = false
            }
        }
    }
    Box(modifier = modifier) {
        IconButton(
            onClick = {
                if (!isRunning) {
                    if (isOpen) {
                        isOpen = false
                    } else {
                        runAuthCheck { token ->
                            authToken = token
                            isOpen = true
                            isBranchesLoading = true
                        }
                    }
                }
            },
            modifier = Modifier
                .size(48.dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            isHovering = when (event.type) {
                                PointerEventType.Enter -> true
                                PointerEventType.Exit -> false
                                else -> isHovering
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
                    if (hasActiveRuns) {
                        val spinnerWidth = 2.dp.toPx()
                        val spinnerRadius = size.minDimension / 2 + 2.dp.toPx()
                        drawArc(
                            color = Color(0xFFFFEB3B),
                            startAngle = spinnerRotation,
                            sweepAngle = 90f,
                            useCenter = false,
                            style = Stroke(width = spinnerWidth, cap = StrokeCap.Round),
                            size = androidx.compose.ui.geometry.Size(spinnerRadius * 2, spinnerRadius * 2),
                            topLeft = androidx.compose.ui.geometry.Offset(
                                center.x - spinnerRadius,
                                center.y - spinnerRadius
                            )
                        )
                    }
                }
        ) {
            Image(
                painter = painterResource("icons/github.png"),
                contentDescription = "Run workflow",
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier
                    .size(28.dp)
                    .alpha(if (isHovering) 1f else 0.4f)
            )
        }

        if (isOpen) {
            Dialog(onDismissRequest = { if (!isRunning) isOpen = false }) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DropdownColors.panelBg,
                    elevation = 16.dp,
                    modifier = Modifier
                        .width(500.dp)
                        .heightIn(max = 520.dp)
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
                                    selectedWorkflow.name,
                                    fontFamily = AppFontFamily,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DropdownColors.textPrimary
                                )
                                IconButton(
                                    onClick = { isOpen = false },
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
                                selectedWorkflow.description,
                                fontFamily = AppFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Light,
                                color = DropdownColors.textMuted,
                                lineHeight = 16.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                wfTab("Run New Workflow", selectedTab == 0) { selectedTab = 0 }
                                wfTab("Branch Run History", selectedTab == 2) { selectedTab = 2 }
                                wfTab(
                                    "Session History${if (trackedRuns.isNotEmpty()) " (${trackedRuns.size})" else ""}",
                                    selectedTab == 1
                                ) { selectedTab = 1 }
                            }
                        }

                        Divider(color = DropdownColors.divider)

                        when (selectedTab) {
                            0 -> {
                                if (isBranchesLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {

                                            Box(modifier = Modifier.size(32.dp)) {
                                                CircularProgressIndicator(
                                                    color = DropdownColors.textDim,
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .rotate(spinnerRotation)
                                                )
                                            }
                                            Text(
                                                "Loading branches...",
                                                fontFamily = AppFontFamily,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 18.sp,
                                                color = DropdownColors.textDim
                                            )
                                            Text(
                                                "This may take a few seconds...",
                                                fontFamily = AppFontFamily,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 11.sp,
                                                color = DropdownColors.textDim
                                            )
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .verticalScroll(rememberScrollState())
                                            .padding(horizontal = 20.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            wfFieldLabel("Use workflow from branch", required = true)

                                            val refreshInteraction = remember { MutableInteractionSource() }
                                            val isRefreshHovered by refreshInteraction.collectIsHoveredAsState()
                                            var isRefreshing by remember { mutableStateOf(false) }

                                            val refreshRotation by animateFloatAsState(
                                                targetValue = if (isRefreshing) 360f else 0f,
                                                animationSpec = tween(600)
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(
                                                        when {
                                                            isRefreshing -> DropdownColors.accentYellow.copy(alpha = 0.15f)
                                                            isRefreshHovered -> DropdownColors.green.copy(alpha = 0.08f)
                                                            else -> DropdownColors.green.copy(alpha = 0.15f)
                                                        }
                                                    )
                                                    .border(
                                                        1.dp,
                                                        when {
                                                            isRefreshing -> DropdownColors.accentYellow.copy(alpha = 0.3f)
                                                            isRefreshHovered -> DropdownColors.green.copy(alpha = 0.2f)
                                                            else -> DropdownColors.green.copy(alpha = 0.3f)
                                                        },
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .hoverable(refreshInteraction)
                                                    .clickable(enabled = !isRefreshing && authToken != null) {
                                                        isRefreshing = true
                                                        isLinkVisible = false
                                                        coroutineScope.launch {
                                                            isBranchesLoading = true
                                                            val fetchedBranches = withContext(Dispatchers.IO) {
                                                                GitHubClient.fetchBranches(authToken!!)
                                                            }
                                                            branches = fetchedBranches
                                                            branchCache = fetchedBranches
                                                            isBranchesLoading = false
                                                            isRefreshing = false
                                                        }
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Refresh,
                                                        contentDescription = "Refresh branches",
                                                        tint = when {
                                                            isRefreshing -> DropdownColors.accentYellow
                                                            isRefreshHovered -> DropdownColors.green
                                                            else -> DropdownColors.green
                                                        },
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .rotate(refreshRotation)

                                                    )
                                                    Text(
                                                        text = if (isRefreshing) "Refreshing..." else "Refresh branches",
                                                        fontFamily = AppFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = when {
                                                            isRefreshing -> DropdownColors.accentYellow
                                                            isRefreshHovered -> DropdownColors.green.copy(alpha = 0.8f)
                                                            else -> DropdownColors.green
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedTextField(
                                                value = branchSearchQuery,
                                                onValueChange = { branchSearchQuery = it },
                                                placeholder = {
                                                    Text(
                                                        selectedBranch.ifEmpty { "Search branches..." },
                                                        fontFamily = AppFontFamily,
                                                        fontWeight = FontWeight.Light,
                                                        fontSize = 13.sp,
                                                        color = DropdownColors.textDim,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Search,
                                                        contentDescription = "Search",
                                                        tint = DropdownColors.textMuted,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                trailingIcon = {
                                                    if (branchSearchQuery.isNotEmpty()) {
                                                        IconButton(
                                                            onClick = { branchSearchQuery = "" },
                                                            modifier = Modifier.size(18.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Clear,
                                                                contentDescription = "Clear",
                                                                tint = DropdownColors.textMuted,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                },
                                                singleLine = true,
                                                textStyle = TextStyle(
                                                    fontFamily = AppFontFamily,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = DropdownColors.textPrimary
                                                ),
                                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                                    backgroundColor = DropdownColors.fieldBg,
                                                    focusedBorderColor = DropdownColors.accentYellow,
                                                    unfocusedBorderColor = DropdownColors.fieldBorder,
                                                    cursorColor = DropdownColors.textPrimary
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth(),

                                            )

                                            if (selectedBranch.isNotEmpty()) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.padding(start = 4.dp)
                                                ) {
                                                    Text("\uD83C\uDF3F", fontSize = 12.sp)
                                                    Text(
                                                        "Selected: $selectedBranch",
                                                        fontFamily = AppFontFamily,
                                                        fontWeight = FontWeight.Normal,
                                                        fontSize = 11.sp,
                                                        color = DropdownColors.green
                                                    )
                                                }
                                            }

                                            if (branchSearchQuery.isNotEmpty()) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(max = 150.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(DropdownColors.fieldBg)
                                                        .border(1.dp, DropdownColors.fieldBorder, RoundedCornerShape(8.dp))
                                                ) {
                                                    if (filteredBranches.isEmpty()) {
                                                        Text(
                                                            "No branches found",
                                                            fontFamily = AppFontFamily,
                                                            fontWeight = FontWeight.Light,
                                                            fontSize = 12.sp,
                                                            color = DropdownColors.textDim,
                                                            modifier = Modifier.padding(12.dp)
                                                        )
                                                    } else {
                                                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                                            filteredBranches.forEach { branch ->
                                                                val itemInteraction = remember { MutableInteractionSource() }
                                                                val itemHovered by itemInteraction.collectIsHoveredAsState()
                                                                val isSelected = branch.name == selectedBranch

                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .background(
                                                                            when {
                                                                                isSelected -> DropdownColors.selectedHighlight
                                                                                itemHovered -> DropdownColors.hoverHighlight
                                                                                else -> Color.Transparent
                                                                            }
                                                                        )
                                                                        .hoverable(itemInteraction)
                                                                        .clickable {
                                                                            selectedBranch = branch.name
                                                                            branchSearchQuery = ""
                                                                        }
                                                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                                                ) {
                                                                    Text("\uD83C\uDF3F", fontSize = 11.sp, modifier = Modifier.padding(end = 8.dp))
                                                                    Text(
                                                                        branch.name,
                                                                        fontFamily = AppFontFamily,
                                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                        fontSize = 12.sp,
                                                                        color = if (isSelected) DropdownColors.green else DropdownColors.textPrimary
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Divider(color = DropdownColors.divider, modifier = Modifier.padding(vertical = 2.dp))

                                        if (isInputsLoading) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CircularProgressIndicator(
                                                    color = DropdownColors.textMuted,
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    "Loading workflow inputs for $selectedBranch...",
                                                    fontFamily = AppFontFamily,
                                                    fontWeight = FontWeight.Light,
                                                    fontSize = 12.sp,
                                                    color = DropdownColors.textMuted
                                                )
                                            }
                                        } else if (dynamicInputs.isEmpty() && selectedBranch.isNotEmpty()) {
                                            Text(
                                                "No inputs found for this branch. Branch may have been deleted.",
                                                fontFamily = AppFontFamily,
                                                fontWeight = FontWeight.Light,
                                                fontSize = 12.sp,
                                                color = DropdownColors.textDim,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        } else {
                                            dynamicInputs.forEach { input ->
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    wfFieldLabel(input.description, input.required)

                                                    val currentValue = fieldValues[input.id] ?: input.default

                                                    when {
                                                        input.type == "choice" && input.options.isNotEmpty() -> {
                                                            wfStyledDropdown(
                                                                value = currentValue,
                                                                options = input.options,
                                                                onSelected = { idx ->
                                                                    fieldValues = fieldValues.toMutableMap().apply {
                                                                        put(input.id, input.options[idx])
                                                                    }
                                                                }
                                                            )
                                                        }
                                                        input.type == "boolean" -> {
                                                            wfStyledDropdown(
                                                                value = currentValue,
                                                                options = listOf("true", "false"),
                                                                onSelected = { idx ->
                                                                    fieldValues = fieldValues.toMutableMap().apply {
                                                                        put(input.id, if (idx == 0) "true" else "false")
                                                                    }
                                                                }
                                                            )
                                                        }
                                                        else -> {
                                                            wfStyledTextField(
                                                                value = currentValue,
                                                                placeholder = input.description.ifEmpty { "Enter value..." },
                                                                onValueChanged = { newVal ->
                                                                    fieldValues = fieldValues.toMutableMap().apply {
                                                                        put(input.id, newVal)
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                }
                                Divider(color = DropdownColors.divider)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DropdownColors.footerBg)
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (latestRunId != null && isLinkVisible) {
                                        val linkInteraction = remember { MutableInteractionSource() }
                                        val isLinkHovered by linkInteraction.collectIsHoveredAsState()

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .hoverable(linkInteraction)
                                                .clickable {
                                                    try {
                                                        java.awt.Desktop.getDesktop().browse(
                                                            java.net.URI("https://github.com/getndazn/android-dazn-app/actions/runs/$latestRunId")
                                                        )
                                                    } catch (_: Exception) { }
                                                }
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "Open job in browser",
                                                fontFamily = AppFontFamily,
                                                fontWeight = FontWeight.Light,
                                                fontSize = 11.sp,
                                                color = if (isLinkHovered) DropdownColors.accentYellow else DropdownColors.textMuted
                                            )
                                            Icon(
                                                imageVector = Icons.Default.OpenInNew,
                                                contentDescription = "Open in GitHub",
                                                tint = if (isLinkHovered) DropdownColors.accentYellow else DropdownColors.textMuted,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(Modifier.width(1.dp))
                                    }

                                    val runInteraction = remember { MutableInteractionSource() }
                                    val isRunHovered by runInteraction.collectIsHoveredAsState()
                                    val canRun = allRequiredFilled && !isRunning && !isBranchesLoading && !isInputsLoading

                                    Button(
                                        onClick = {
                                            if (canRun && authToken != null) {
                                                isRunning = true
                                                coroutineScope.launch {
                                                    val inputs = fieldValues.toMutableMap()
                                                    val success = GitHubClient.triggerWorkflow(
                                                        token = authToken!!,
                                                        branch = selectedBranch,
                                                        inputs = inputs
                                                    )
                                                    if (success) {
                                                        val latestRun = GitHubClient.getLatestRun(authToken!!, selectedBranch)
                                                        if (latestRun != null) {
                                                            latestRunId = latestRun.id
                                                            isLinkVisible = true
                                                        }
                                                        branchHistoryCache = branchHistoryCache - selectedBranch
                                                        hasFetchedBranchHistory = false
                                                        onWorkflowTriggered(
                                                            WorkflowResult(
                                                                workflowId = selectedWorkflow.id,
                                                                workflowName = selectedWorkflow.name,
                                                                branch = selectedBranch,
                                                                fieldValues = fieldValues,
                                                                runId = latestRun?.id
                                                            )
                                                        )
                                                        if (latestRun != null) {
                                                            WorkflowMonitor.trackRun(
                                                                runId = latestRun.id,
                                                                branch = selectedBranch,
                                                                brand = fieldValues["brand"] ?: "",
                                                                store = fieldValues["store"] ?: "",
                                                                appSealing = fieldValues["enable-app-sealing"] ?: "true"
                                                            )
                                                            trackedRuns = WorkflowMonitor.getTrackedRuns()
                                                        }
                                                    } else {
                                                        onWorkflowTriggered(
                                                            WorkflowResult(
                                                                workflowId = selectedWorkflow.id,
                                                                workflowName = selectedWorkflow.name,
                                                                branch = selectedBranch,
                                                                fieldValues = fieldValues,
                                                                error = "Failed to trigger workflow"
                                                            )
                                                        )
                                                    }
                                                    isRunning = false
                                                }
                                            }
                                        },
                                        enabled = canRun,
                                        interactionSource = runInteraction,
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = if (isRunHovered) DropdownColors.greenHover else DropdownColors.green,
                                            contentColor = Color.White,
                                            disabledBackgroundColor = DropdownColors.greenDisabled,
                                            disabledContentColor = Color.White.copy(alpha = 0.4f)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                                    ) {
                                        if (isRunning) {
                                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Triggering...", fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                        } else {
                                            Text("▶", fontSize = 11.sp)
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                "Run workflow",
                                                fontFamily = AppFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }

                            1 -> {
                                if (trackedRuns.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.History, contentDescription = null, tint = DropdownColors.textDim, modifier = Modifier.size(56.dp))
                                            Text("No workflow runs triggered yet", fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, color = DropdownColors.textDim)
                                            Text("Trigger a workflow to see it appear here", fontFamily = AppFontFamily, fontWeight = FontWeight.Light, fontSize = 11.sp, color = DropdownColors.textDim)
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        trackedRuns.forEach { run ->
                                            wfRunHistoryItem(
                                                run = run,
                                                isDownloading = isDownloading == run.runId,
                                                downloadFailed = downloadFailed == run.runId,
                                                isCancelling = cancellingRunId == run.runId,
                                                downloadProgress = if (isDownloading == run.runId || downloadFailed == run.runId) downloadProgress else "",
                                                isRetriggering = retriggeringRunId == run.runId,
                                                onDownload = { artifact ->
                                                    if (authToken != null) {
                                                        isDownloading = run.runId
                                                        downloadFailed = null
                                                        lastDownloadArtifact = artifact
                                                        GitHubClient.resetDownloadCancel()
                                                        coroutineScope.launch {
                                                            onConsoleOutput(ConsoleInstallMessage.Text("Download started: ${artifact.name}"))
                                                            val path = GitHubClient.downloadArtifact(
                                                                token = authToken!!,
                                                                artifactId = artifact.id,
                                                                fileName = artifact.name,
                                                                onProgress = { progress -> downloadProgress = progress }
                                                            )
                                                            isDownloading = null
                                                            if (path != null) {
                                                                downloadProgress = ""
                                                                downloadFailed = null
                                                                lastDownloadArtifact = null
                                                                onConsoleOutput(
                                                                    ConsoleInstallMessage.InstallAction(
                                                                        preText = "Result: Download completed successfully\nFile: ",
                                                                        filePath = path
                                                                    )
                                                                )
                                                            } else {
                                                                downloadFailed = run.runId
                                                                onConsoleOutput(ConsoleInstallMessage.Text("Result: Download failed — $downloadProgress")) }
                                                        }
                                                    }
                                                },
                                                onRetryDownload = {
                                                    if (authToken != null && lastDownloadArtifact != null) {
                                                        isDownloading = run.runId
                                                        downloadFailed = null
                                                        GitHubClient.resetDownloadCancel()
                                                        coroutineScope.launch {
                                                            onConsoleOutput(ConsoleInstallMessage.Text("Retrying download: ${lastDownloadArtifact!!.name}"))
                                                            val path = GitHubClient.downloadArtifact(
                                                                token = authToken!!,
                                                                artifactId = lastDownloadArtifact!!.id,
                                                                fileName = lastDownloadArtifact!!.name,
                                                                onProgress = { progress -> downloadProgress = progress }
                                                            )
                                                            isDownloading = null
                                                            if (path != null) {
                                                                downloadProgress = ""
                                                                downloadFailed = null
                                                                lastDownloadArtifact = null
                                                                onConsoleOutput(
                                                                    ConsoleInstallMessage.InstallAction(
                                                                        preText = "Result: Download completed successfully\nFile: ",
                                                                        filePath = path
                                                                    )
                                                                )
                                                            } else {
                                                                downloadFailed = run.runId
                                                                onConsoleOutput(ConsoleInstallMessage.Text("Result: Retry download failed — $downloadProgress"))
                                                            }
                                                        }
                                                    }
                                                },
                                                onCancelDownload = {
                                                    GitHubClient.cancelDownload()
                                                    isDownloading = null
                                                    downloadFailed = run.runId
                                                    downloadProgress = "Download cancelled"
                                                    onConsoleOutput(ConsoleInstallMessage.Text("Download cancelled by user manually"))
                                                },
                                                onCancelRun = {
                                                    if (authToken != null) {
                                                        cancellingRunId = run.runId
                                                        coroutineScope.launch {
                                                            val cancelled = GitHubClient.cancelWorkflowRun(authToken!!, run.runId)
                                                            if (cancelled) {
                                                                onConsoleOutput(ConsoleInstallMessage.Text("Workflow cancellation requested for run ${run.runId}"))


                                                                repeat(20) {
                                                                    delay(5000)
                                                                    val status = GitHubClient.getRunStatus(authToken!!, run.runId)
                                                                    if (status != null && status.status == "completed") {
                                                                        WorkflowMonitor.forceRefresh()
                                                                        trackedRuns = WorkflowMonitor.getTrackedRuns()
                                                                        cancellingRunId = null
                                                                        return@launch
                                                                    }
                                                                }
                                                                cancellingRunId = null
                                                            } else {
                                                                onConsoleOutput(ConsoleInstallMessage.Text("Failed to cancel workflow run ${run.runId}"))
                                                                cancellingRunId = null
                                                            }
                                                        }
                                                    }
                                                },
                                                onRerun = {
                                                    if (authToken != null) {
                                                        retriggeringRunId = run.runId
                                                        coroutineScope.launch {
                                                            val rerun = GitHubClient.rerunWorkflow(authToken!!, run.runId)
                                                            if (rerun) {
                                                                onConsoleOutput(ConsoleInstallMessage.Text("Workflow re-run triggered for branch: ${run.branch}"))
                                                                var newRun: GitHubWorkflowRun? = null
                                                                repeat(10) {
                                                                    delay(3000)
                                                                    val latest = GitHubClient.getRunStatus(authToken!!, run.runId)
                                                                    if (latest != null && latest.status == "in_progress") {
                                                                        newRun = latest
                                                                        return@repeat
                                                                    }
                                                                }

                                                                if (newRun != null) {
                                                                    WorkflowMonitor.updateRun(
                                                                        run.copy(status = WorkflowRunStatus.IN_PROGRESS)
                                                                    )
                                                                    WorkflowMonitor.restartMonitoring()
                                                                    trackedRuns = WorkflowMonitor.getTrackedRuns()
                                                                } else {
                                                                    onConsoleOutput(ConsoleInstallMessage.Text("Re-run started but could not confirm status"))
                                                                }
                                                            } else {
                                                                onConsoleOutput(ConsoleInstallMessage.Text("Failed to re-run workflow"))

                                                            }
                                                            retriggeringRunId = null
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            2 -> {
                                if ((isBranchesLoading && branchRunHistory.isEmpty()) || isBranchHistoryLoading || (!hasFetchedBranchHistory && selectedBranch.isNotEmpty())) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(modifier = Modifier.size(32.dp)) {
                                                CircularProgressIndicator(
                                                    color = DropdownColors.textDim,
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .rotate(spinnerRotation)
                                                )
                                            }
                                            if (isBranchesLoading) {
                                                Text(
                                                    "Loading branches...",
                                                    fontFamily = AppFontFamily,
                                                    fontWeight = FontWeight.Normal,
                                                    fontSize = 18.sp,
                                                    color = DropdownColors.textDim
                                                )
                                                Text(
                                                    "Waiting for branch data to be fetched",
                                                    fontFamily = AppFontFamily,
                                                    fontWeight = FontWeight.Normal,
                                                    fontSize = 11.sp,
                                                    color = DropdownColors.textDim
                                                )
                                            } else {
                                                Text(
                                                    "Loading...",
                                                    fontFamily = AppFontFamily,
                                                    fontWeight = FontWeight.Normal,
                                                    fontSize = 20.sp,
                                                    color = DropdownColors.textDim
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        "Fetching last 10 runs for branch:",
                                                        fontFamily = AppFontFamily,
                                                        fontWeight = FontWeight.Normal,
                                                        fontSize = 16.sp,
                                                        color = DropdownColors.textDim
                                                    )
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .widthIn(max = 300.dp)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(DropdownColors.green.copy(alpha = 0.15f))
                                                            .border(1.dp, DropdownColors.green.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = selectedBranch,
                                                            fontFamily = AppFontFamily,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 16.sp,
                                                            color = DropdownColors.green,
                                                            letterSpacing = 1.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (branchRunHistory.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = null,
                                                tint = DropdownColors.textDim,
                                                modifier = Modifier.size(56.dp)
                                            )
                                            Text(
                                                "No workflows runs available yet from this branch",
                                                fontFamily = AppFontFamily,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 16.sp,
                                                color = DropdownColors.textDim
                                            )
                                            Text(
                                                "Select a different branch or run a new workflow",
                                                fontFamily = AppFontFamily,
                                                fontWeight = FontWeight.Light,
                                                fontSize = 11.sp,
                                                color = DropdownColors.textDim
                                            )
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .verticalScroll(rememberScrollState())
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        branchRunHistory.forEach { run ->
                                            val interaction = remember { MutableInteractionSource() }
                                            val isHovered by interaction.collectIsHoveredAsState()

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isHovered) DropdownColors.hoverHighlight else DropdownColors.fieldBg)
                                                    .border(1.dp, DropdownColors.fieldBorder, RoundedCornerShape(8.dp))
                                                    .hoverable(interaction)
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(
                                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    ) {
                                                        Text(
                                                            run.displayTitle,
                                                            fontFamily = AppFontFamily,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = DropdownColors.textPrimary,
                                                            maxLines = 1,
                                                            modifier = Modifier.weight(1f, fill = false)
                                                        )

                                                        val linkInteraction = remember { MutableInteractionSource() }
                                                        val isLinkHovered by linkInteraction.collectIsHoveredAsState()

                                                        Text(
                                                            text = "#${run.runNumber}",
                                                            fontFamily = AppFontFamily,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = DropdownColors.textDim,
                                                            maxLines = 1
                                                        )

                                                        IconButton(
                                                            onClick = {
                                                                try {
                                                                    java.awt.Desktop.getDesktop().browse(
                                                                        java.net.URI(run.htmlUrl)
                                                                    )
                                                                } catch (_: Exception) { }
                                                            },
                                                            interactionSource = linkInteraction,
                                                            modifier = Modifier.size(16.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.OpenInNew,
                                                                contentDescription = "Open in GitHub",
                                                                tint = if (isLinkHovered) DropdownColors.accentYellow else DropdownColors.textDim,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                        }
                                                    }

                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = buildAnnotatedString {
                                                                withStyle(SpanStyle(fontWeight = FontWeight.Light, color = DropdownColors.textDim)) {
                                                                    append("Triggered on ")
                                                                }
                                                                withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = DropdownColors.textPrimary)) {
                                                                    append(formatDate(run.createdAt).replace("T", " ").replace("Z", "").take(17))
                                                                }
                                                                withStyle(SpanStyle(fontWeight = FontWeight.Light, color = DropdownColors.textDim)) {
                                                                    append(" by ")
                                                                }
                                                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                                                                    append(run.actor.login)
                                                                }
                                                            },
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            fontFamily = AppFontFamily,
                                                            fontSize = 10.sp,
                                                            modifier = Modifier.weight(1f, fill = false)
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(DropdownColors.green.copy(alpha = 0.15f))
                                                                .border(1.dp, DropdownColors.green.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                                .widthIn(max = 120.dp)
                                                        ) {
                                                            Text(
                                                                text = run.headBranch,
                                                                fontFamily = AppFontFamily,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 8.sp,
                                                                color = DropdownColors.green,
                                                                letterSpacing = 0.5.sp,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }

                                                        val (statusText, statusColor) = when (run.getStatus()) {
                                                            WorkflowRunStatus.COMPLETED -> "SUCCESS" to DropdownColors.green
                                                            WorkflowRunStatus.FAILED -> "FAILED" to DropdownColors.required
                                                            WorkflowRunStatus.CANCELLED -> "CANCELLED" to DropdownColors.textMuted
                                                            WorkflowRunStatus.IN_PROGRESS -> "IN PROGRESS" to DropdownColors.orange
                                                            WorkflowRunStatus.QUEUED -> "QUEUED" to DropdownColors.accentYellow
                                                            WorkflowRunStatus.UNKNOWN -> "UNKNOWN" to DropdownColors.textDim
                                                        }

                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(statusColor.copy(alpha = 0.15f))
                                                                .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                statusText,
                                                                fontFamily = AppFontFamily,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 8.sp,
                                                                color = statusColor,
                                                                letterSpacing = 0.5.sp
                                                            )
                                                        }
                                                    }
                                                    if (isDownloading == run.runId || downloadFailed == run.runId) {
                                                        Text(
                                                            downloadProgress,
                                                            fontFamily = AppFontFamily,
                                                            fontWeight = FontWeight.Light,
                                                            fontSize = 10.sp,
                                                            color = if (downloadFailed == run.runId) DropdownColors.required else DropdownColors.accentYellow
                                                        )
                                                    }
                                                }
                                                if (run.getStatus() == WorkflowRunStatus.COMPLETED) {
                                                    if (isDownloading == run.runId) {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            CircularProgressIndicator(
                                                                color = DropdownColors.accentYellow,
                                                                strokeWidth = 2.dp,
                                                                modifier = Modifier.size(18.dp)
                                                            )

                                                            val cancelInteraction = remember { MutableInteractionSource() }
                                                            val isCancelHovered by cancelInteraction.collectIsHoveredAsState()

                                                            IconButton(
                                                                onClick = {
                                                                    GitHubClient.cancelDownload()
                                                                    isDownloading = null
                                                                    downloadFailed = run.runId
                                                                    downloadProgress = "Download cancelled"
                                                                    onConsoleOutput(ConsoleInstallMessage.Text("Download cancelled by user manually"))
                                                                },
                                                                interactionSource = cancelInteraction,
                                                                modifier = Modifier.size(20.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Close,
                                                                    contentDescription = "Cancel download",
                                                                    tint = if (isCancelHovered) DropdownColors.required else DropdownColors.textMuted,
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                            }
                                                        }
                                                    } else if (downloadFailed == run.runId) {
                                                        val retryInteraction = remember { MutableInteractionSource() }
                                                        val isRetryHovered by retryInteraction.collectIsHoveredAsState()

                                                        IconButton(
                                                            onClick = {
                                                                if (authToken != null && lastDownloadArtifact != null) {
                                                                    isDownloading = run.runId
                                                                    downloadFailed = null
                                                                    GitHubClient.resetDownloadCancel()
                                                                    coroutineScope.launch {
                                                                        onConsoleOutput(ConsoleInstallMessage.Text("Retrying download: ${lastDownloadArtifact!!.name}"))
                                                                        val path = GitHubClient.downloadArtifact(
                                                                            token = authToken!!,
                                                                            artifactId = lastDownloadArtifact!!.id,
                                                                            fileName = lastDownloadArtifact!!.name,
                                                                            onProgress = { progress -> downloadProgress = progress }
                                                                        )
                                                                        isDownloading = null
                                                                        if (path != null) {
                                                                            downloadProgress = ""
                                                                            downloadFailed = null
                                                                            lastDownloadArtifact = null
                                                                            onConsoleOutput(
                                                                                ConsoleInstallMessage.InstallAction(
                                                                                    preText = "Result: Download completed successfully",
                                                                                    filePath = path
                                                                                )
                                                                            )
                                                                        } else {
                                                                            downloadFailed = run.runId
                                                                            onConsoleOutput(ConsoleInstallMessage.Text("Result: Retry download failed — $downloadProgress"))
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            interactionSource = retryInteraction,
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Refresh,
                                                                contentDescription = "Retry download",
                                                                tint = if (isRetryHovered) DropdownColors.accentYellow else DropdownColors.required,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    } else {
                                                        val dlInteraction = remember { MutableInteractionSource() }
                                                        val isDlHovered by dlInteraction.collectIsHoveredAsState()

                                                        IconButton(
                                                            onClick = {
                                                                if (authToken != null) {
                                                                    coroutineScope.launch {
                                                                        isDownloading = run.runId
                                                                        downloadFailed = null
                                                                        onConsoleOutput(ConsoleInstallMessage.Text("Fetching artifacts for run ${run.runId}..."))

                                                                        val artifacts = GitHubClient.getArtifacts(authToken!!, run.runId)
                                                                        val apkArtifact = artifacts.firstOrNull { it.name.startsWith("APKS") }

                                                                        if (apkArtifact != null) {
                                                                            lastDownloadArtifact = apkArtifact
                                                                            GitHubClient.resetDownloadCancel()
                                                                            onConsoleOutput(ConsoleInstallMessage.Text("Download started: ${apkArtifact.name}"))

                                                                            val path = GitHubClient.downloadArtifact(
                                                                                token = authToken!!,
                                                                                artifactId = apkArtifact.id,
                                                                                fileName = apkArtifact.name,
                                                                                onProgress = { progress -> downloadProgress = progress }
                                                                            )

                                                                            isDownloading = null
                                                                            if (path != null) {
                                                                                downloadProgress = ""
                                                                                downloadFailed = null
                                                                                onConsoleOutput(ConsoleInstallMessage.InstallAction(
                                                                                        preText = "Result: Download completed successfully",
                                                                                        filePath = path
                                                                                    )
                                                                                )
                                                                            } else {
                                                                                downloadFailed = run.runId
                                                                                onConsoleOutput(ConsoleInstallMessage.Text("Result: Download failed — $downloadProgress"))
                                                                            }
                                                                        } else {
                                                                            isDownloading = null
                                                                            onConsoleOutput(ConsoleInstallMessage.Text("No APKS artifact found for this run"))
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            interactionSource = dlInteraction,
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Download,
                                                                contentDescription = "Download",
                                                                tint = if (isDlHovered) DropdownColors.green else DropdownColors.textPrimary,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                } else if (run.getStatus() == WorkflowRunStatus.FAILED) {
                                                    Icon(
                                                        imageVector = Icons.Default.Error,
                                                        contentDescription = "Failed",
                                                        tint = DropdownColors.required,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                } else if (run.getStatus() == WorkflowRunStatus.CANCELLED) {
                                                    Icon(
                                                        imageVector = Icons.Default.Cancel,
                                                        contentDescription = "Cancelled",
                                                        tint = DropdownColors.textMuted,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                } else if (run.getStatus() == WorkflowRunStatus.IN_PROGRESS || run.getStatus() == WorkflowRunStatus.QUEUED) {
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

        showAuthError?.let { error ->
            gitHubCliDialog(
                error = error,
                isChecking = isCheckingAuth,
                onRecheck = {
                    runAuthCheck { token ->
                        authToken = token
                        showAuthError = null
                        isOpen = true
                        isBranchesLoading = true
                    }
                },
                onDismiss = { showAuthError = null }
            )
        }

        if (isCheckingAuth && showAuthError == null) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp).align(Alignment.Center),
                color = DropdownColors.accentYellow,
                strokeWidth = 2.dp
            )
        }

        if (isCheckingAuth && showAuthError == null) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp).align(Alignment.Center),
                color = DropdownColors.accentYellow,
                strokeWidth = 2.dp
            )
        }
    }
}
@Composable
fun wfRunHistoryItem(
    run: TrackedRun,
    isDownloading: Boolean,
    downloadFailed: Boolean,
    downloadProgress: String,
    onDownload: (GitHubArtifact) -> Unit,
    onRetryDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onRerun: () -> Unit,
    onCancelRun: () -> Unit,
    isCancelling: Boolean,
    isRetriggering: Boolean
) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()
    val infiniteTransition = rememberInfiniteTransition()
    val spinnerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHovered) DropdownColors.hoverHighlight else DropdownColors.fieldBg)
            .border(1.dp, DropdownColors.fieldBorder, RoundedCornerShape(8.dp))
            .hoverable(interaction)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "\uD83C\uDF3F",
                    fontSize = 11.sp
                )
                Text(
                    run.branch,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = DropdownColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val linkInteraction = remember { MutableInteractionSource() }
                val isLinkHovered by linkInteraction.collectIsHoveredAsState()

                IconButton(
                    onClick = {
                        try {
                            java.awt.Desktop.getDesktop().browse(
                                java.net.URI("https://github.com/getndazn/android-dazn-app/actions/runs/${run.runId}")
                            )
                        } catch (_: Exception) { }
                    },
                    interactionSource = linkInteraction,
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open in GitHub",
                        tint = if (isLinkHovered) DropdownColors.accentYellow else DropdownColors.textDim,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (run.brand.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DropdownColors.textMuted.copy(alpha = 0.15f))
                            .border(1.dp, DropdownColors.textMuted.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            run.brand,
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            color = DropdownColors.textMuted,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                if (run.store.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DropdownColors.textMuted.copy(alpha = 0.15f))
                            .border(1.dp, DropdownColors.textMuted.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            run.store,
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            color = DropdownColors.textMuted,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                if (run.appSealing.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (run.appSealing == "true")
                                    DropdownColors.green.copy(alpha = 0.15f)
                                else
                                    DropdownColors.textMuted.copy(alpha = 0.15f)
                            )
                            .border(
                                1.dp,
                                if (run.appSealing == "true")
                                    DropdownColors.green.copy(alpha = 0.3f)
                                else
                                    DropdownColors.textMuted.copy(alpha = 0.3f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Sealing: ${run.appSealing}",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            color = if (run.appSealing == "true") DropdownColors.green else DropdownColors.textMuted,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(DropdownColors.textDim.copy(alpha = 0.15f))
                        .border(1.dp, DropdownColors.textDim.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "Triggered at ${run.triggeredAt}",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        color = DropdownColors.textDim,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            if (isDownloading || downloadFailed) {
                Text(
                    downloadProgress,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Light,
                    fontSize = 10.sp,
                    color = if (downloadFailed) DropdownColors.required else DropdownColors.accentYellow
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (run.status) {
                WorkflowRunStatus.QUEUED, WorkflowRunStatus.IN_PROGRESS -> {
                    if (isCancelling) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DropdownColors.accentYellow.copy(alpha = 0.2f))
                                .border(1.dp, DropdownColors.accentYellow.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 1.5.dp,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    "Cancelling workflow...",
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                    else {
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
                                    contentDescription = "Cancel workflow",
                                    tint = if (isCancelHovered) DropdownColors.required else DropdownColors.textMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                WorkflowRunStatus.COMPLETED -> {
                    if (isDownloading) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = DropdownColors.accentYellow,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )

                            val cancelInteraction = remember { MutableInteractionSource() }
                            val isCancelHovered by cancelInteraction.collectIsHoveredAsState()

                            IconButton(
                                onClick = onCancelDownload,
                                interactionSource = cancelInteraction,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel download",
                                    tint = if (isCancelHovered) DropdownColors.required else DropdownColors.textMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    } else if (downloadFailed) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val retryInteraction = remember { MutableInteractionSource() }
                            val isRetryHovered by retryInteraction.collectIsHoveredAsState()

                            IconButton(
                                onClick = onRetryDownload,
                                interactionSource = retryInteraction,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry download",
                                    tint = if (isRetryHovered) DropdownColors.accentYellow else DropdownColors.required,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (run.artifacts.isNotEmpty()) {
                                val dlInteraction = remember { MutableInteractionSource() }
                                val isDlHovered by dlInteraction.collectIsHoveredAsState()

                                IconButton(
                                    onClick = { onDownload(run.artifacts.first()) },
                                    interactionSource = dlInteraction,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Download again",
                                        tint = if (isDlHovered) DropdownColors.green else DropdownColors.textMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    } else if (run.artifacts.isNotEmpty()) {
                        val dlInteraction = remember { MutableInteractionSource() }
                        val isDlHovered by dlInteraction.collectIsHoveredAsState()

                        IconButton(
                            onClick = { onDownload(run.artifacts.first()) },
                            interactionSource = dlInteraction,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download",
                                tint = if (isDlHovered) DropdownColors.green else DropdownColors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = DropdownColors.green,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                WorkflowRunStatus.FAILED -> {
                    if (isRetriggering) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DropdownColors.accentYellow.copy(alpha = 0.2f))
                                .border(1.dp, DropdownColors.accentYellow.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 1.5.dp,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    "Re-triggering",
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Failed",
                                tint = DropdownColors.required,
                                modifier = Modifier.size(20.dp)
                            )

                            val rerunInteraction = remember { MutableInteractionSource() }
                            val isRerunHovered by rerunInteraction.collectIsHoveredAsState()

                            IconButton(
                                onClick = onRerun,
                                interactionSource = rerunInteraction,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = "Re-run workflow",
                                    tint = if (isRerunHovered) DropdownColors.accentYellow else DropdownColors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                WorkflowRunStatus.CANCELLED -> {
                    if (isRetriggering) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DropdownColors.accentYellow.copy(alpha = 0.2f))
                                .border(1.dp, DropdownColors.accentYellow.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 1.5.dp,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    "Re-triggering",
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DropdownColors.textMuted.copy(alpha = 0.15f))
                                    .border(1.dp, DropdownColors.textMuted.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = "Cancelled",
                                        tint = DropdownColors.textMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        "CANCELLED",
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp,
                                        color = DropdownColors.textMuted,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            val rerunInteraction = remember { MutableInteractionSource() }
                            val isRerunHovered by rerunInteraction.collectIsHoveredAsState()

                            IconButton(
                                onClick = onRerun,
                                interactionSource = rerunInteraction,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = "Re-run workflow",
                                    tint = if (isRerunHovered) DropdownColors.accentYellow else DropdownColors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                WorkflowRunStatus.UNKNOWN -> {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Unknown",
                        tint = DropdownColors.textDim,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun wfFieldLabel(text: String, required: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            fontFamily = AppFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = DropdownColors.textSecondary
        )
        if (required) {
            Text(
                "*",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DropdownColors.required
            )
        }
    }
}

@Composable
private fun wfStyledDropdown(
    value: String,
    options: List<String>,
    onSelected: (Int) -> Unit,
    leadingIcon: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(DropdownColors.fieldBg)
                .border(
                    1.dp,
                    if (isHovered) DropdownColors.fieldBorderHover else DropdownColors.fieldBorder,
                    RoundedCornerShape(8.dp)
                )
                .hoverable(interaction)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            if (leadingIcon != null) {
                Text(leadingIcon, fontSize = 13.sp)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                value,
                fontFamily = AppFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = DropdownColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text("\u21C5", fontSize = 12.sp, color = DropdownColors.textMuted)
        }

        MaterialTheme(colors = MaterialTheme.colors.copy(surface = DropdownColors.fieldBg)) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.border(1.dp, DropdownColors.fieldBorder, RoundedCornerShape(8.dp))
            ) {
                options.forEachIndexed { index, option ->
                    val itemInteraction = remember { MutableInteractionSource() }
                    val itemHovered by itemInteraction.collectIsHoveredAsState()
                    val isCurrentValue = option == value

                    DropdownMenuItem(
                        onClick = {
                            onSelected(index)
                            expanded = false
                        },
                        interactionSource = itemInteraction,
                        modifier = Modifier.background(
                            when {
                                isCurrentValue -> DropdownColors.selectedHighlight
                                itemHovered -> DropdownColors.hoverHighlight
                                else -> Color.Transparent
                            }
                        )
                    ) {
                        Text(
                            option,
                            fontFamily = AppFontFamily,
                            fontSize = 13.sp,
                            color = if (isCurrentValue) DropdownColors.green else DropdownColors.textPrimary,
                            fontWeight = if (isCurrentValue) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun wfStyledTextField(
    value: String,
    placeholder: String,
    onValueChanged: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChanged,
        placeholder = {
            Text(
                placeholder,
                fontFamily = AppFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Light,
                color = DropdownColors.textDim
            )
        },
        singleLine = true,
        textStyle = TextStyle(
            fontFamily = AppFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = DropdownColors.textPrimary
        ),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            backgroundColor = DropdownColors.fieldBg,
            focusedBorderColor = DropdownColors.focusBorder,
            unfocusedBorderColor = DropdownColors.fieldBorder,
            cursorColor = DropdownColors.textPrimary
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    )
}
@Composable
private fun wfTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .hoverable(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                fontFamily = AppFontFamily,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp,
                color = when {
                    isSelected -> DropdownColors.textPrimary
                    isHovered -> DropdownColors.textSecondary
                    else -> DropdownColors.textMuted
                }
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        if (isSelected) DropdownColors.accentYellow else Color.Transparent
                    )
            )
        }
    }
}

