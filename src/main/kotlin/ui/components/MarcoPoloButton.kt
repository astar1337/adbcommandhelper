package ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import commands.RegionMarcoPolo
import commands.mpRegion
import commands.toFlagEmoji
import kotlinx.coroutines.launch
import ui.components.theme.AppFontFamily
import ui.components.theme.DropdownColors
import kotlinx.coroutines.delay

private const val APP_RELOAD_DELAY_MS = 2_000L

@Composable
fun marcoPoloDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onRegionConnect: (guid: String, regionName: String) -> Unit,
    onRegionDisconnect: () -> Unit,
    currentGuid: String,
) {
    if (!showDialog) return

    var searchQuery by remember { mutableStateOf("") }
    var connectedGateway by remember { mutableStateOf<String?>(null) }
    var connectingGateway by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val regionOptions: List<RegionMarcoPolo> = mpRegion.map { region ->
        RegionMarcoPolo(
            name = region.name,
            index = region.index,
            flagEmoji = region.name.toFlagEmoji(),
            guid = region.guid
        )
    }

    LaunchedEffect(currentGuid, regionOptions) {
        val matchedRegion = regionOptions.find { it.guid == currentGuid }
        connectedGateway = matchedRegion?.name
    }

    val sortedGateways = remember(regionOptions, connectedGateway) {
        val connected = regionOptions.filter { it.name == connectedGateway }
        val others = regionOptions.filter { it.name != connectedGateway }
        connected + others
    }

    val filteredGateways = remember(searchQuery, sortedGateways) {
        if (searchQuery.isBlank()) sortedGateways
        else sortedGateways.filter {
            it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
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
                            text = "Marco Polo Integration",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = DropdownColors.textPrimary
                        )
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
                        text = "Select a region from the list to mock a location on your device",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 12.sp,
                        color = DropdownColors.textMuted
                    )

                    Spacer(Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Note: Clicking on \"Switch to\" button will reload the app",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                color = Color(0xFF92400E)
                            )
                        }
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
                                isLocked = (connectedGateway != null && connectedGateway != gateway.name) ||
                                        (connectingGateway != null && connectingGateway != gateway.name),
                                onConnect = {
                                    coroutineScope.launch {
                                        connectingGateway = gateway.name
                                        onRegionConnect(gateway.guid, gateway.name)
                                        delay(APP_RELOAD_DELAY_MS)
                                        connectedGateway = gateway.name
                                        connectingGateway = null
                                    }
                                },
                                onDisconnect = {
                                    coroutineScope.launch {
                                        connectingGateway = gateway.name
                                        onRegionDisconnect()
                                        delay(APP_RELOAD_DELAY_MS)
                                        connectedGateway = null
                                        connectingGateway = null
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
                        .background(DropdownColors.footerBg)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "If you want to see more regions added, contact the tool owner",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 11.sp,
                        color = DropdownColors.textDim,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    val closeInteraction = remember { MutableInteractionSource() }
                    val isCloseHovered by closeInteraction.collectIsHoveredAsState()

                    TextButton(
                        onClick = onDismiss,
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

@Composable
private fun regionMenuItem(
    gateway: RegionMarcoPolo,
    isConnected: Boolean,
    isConnecting: Boolean,
    isLocked: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isLocked) 0.45f else 1f)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isConnected -> DropdownColors.green.copy(alpha = 0.1f)
                    isHovered && !isLocked -> DropdownColors.rowHover
                    else -> Color.Transparent
                }
            )
            .border(
                1.dp,
                when {
                    isConnected -> DropdownColors.green.copy(alpha = 0.3f)
                    isHovered && !isLocked -> DropdownColors.fieldBorderHover
                    else -> Color.Transparent
                },
                RoundedCornerShape(8.dp)
            )
            .hoverable(interaction, enabled = !isLocked)
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
                    color = if (isConnected) DropdownColors.green else DropdownColors.textPrimary
                )
                Text(
                    text = "UUID: " + gateway.guid,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Light,
                    fontSize = 11.sp,
                    color = DropdownColors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isConnected) {
                    Text(
                        text = "Currently connected to: ${gateway.name}",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 11.sp,
                        color = DropdownColors.green,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        val buttonInteraction = remember { MutableInteractionSource() }
        val isButtonHovered by buttonInteraction.collectIsHoveredAsState()
        val isEnabled = !isConnecting && !isLocked

        Button(
            onClick = {
                if (isConnected) onDisconnect() else onConnect()
            },
            enabled = isEnabled,
            interactionSource = buttonInteraction,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = when {
                    isConnected -> DropdownColors.green
                    isButtonHovered -> DropdownColors.accentYellow
                    else -> DropdownColors.fieldBg
                },
                contentColor = when {
                    isConnected -> Color.White
                    isButtonHovered -> Color.Black
                    else -> DropdownColors.textPrimary
                },
                disabledBackgroundColor = if (isConnected) DropdownColors.green else DropdownColors.fieldBg,
                disabledContentColor = if (isConnected) Color.White else DropdownColors.textDim
            ),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(
                1.dp,
                when {
                    isConnected -> DropdownColors.green
                    isButtonHovered && isEnabled -> DropdownColors.accentYellow
                    else -> DropdownColors.fieldBorder
                }
            ),
            elevation = ButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                hoveredElevation = 0.dp
            ),
            modifier = Modifier.requiredSize(width = 104.dp, height = 32.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = DropdownColors.textMuted,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isConnected) "Disconnect" else "Switch to",
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