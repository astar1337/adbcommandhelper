package ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ui.components.theme.AppFontFamily
import ui.components.theme.DropdownColors
import commands.countryCodes
import utils.helpers.Environment

enum class DialCodeState {
    EMPTY,      // Just "+"
    TYPING,     // Partial match, could become valid
    VALID,      // Exact match to a known country code
    TOO_LONG,   // Valid prefix but extra digits
    INVALID     // No match at all
}

data class DialCodeResult(
    val state: DialCodeState,
    val flag: String = "🌐",
    val country: String = ""
)

fun validateDialCode(input: String): DialCodeResult {
    if (input.isBlank() || input == "+") return DialCodeResult(DialCodeState.EMPTY)

    val cleaned = if (input.startsWith("+")) input else "+$input"

    val exactMatch = countryCodes.firstOrNull { it.dialCode == cleaned }
    if (exactMatch != null) return DialCodeResult(
        state = DialCodeState.VALID,
        flag = exactMatch.flag,
        country = exactMatch.country
    )

    val couldBePartial = countryCodes.any { it.dialCode.startsWith(cleaned) }
    if (couldBePartial) return DialCodeResult(DialCodeState.TYPING)

    val sorted = countryCodes.sortedByDescending { it.dialCode.length }
    val prefixMatch = sorted.firstOrNull { cleaned.startsWith(it.dialCode) }
    if (prefixMatch != null) return DialCodeResult(
        state = DialCodeState.TOO_LONG,
        flag = prefixMatch.flag,
        country = prefixMatch.country
    )
    return DialCodeResult(DialCodeState.INVALID)
}
@Composable
fun clearPhoneDialog(
    showDialog: Boolean,
    environment: Environment,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onClearPhone: (fullNumber: String, environment: Environment) -> Unit
) {
    if (!showDialog) return

    var phoneInput by remember { mutableStateOf("") }
    var countryCodeInput by remember { mutableStateOf("+") }

    val envLabel = when (environment) {
        Environment.STAGING -> "STAG"
        Environment.PRODUCTION -> "PROD"
    }

    val envColor = when (environment) {
        Environment.STAGING -> Color(0xFFFFEB3B)
        Environment.PRODUCTION -> Color(0xFF22C55E)
    }

    val dialCodeResult = remember(countryCodeInput) {
        validateDialCode(countryCodeInput)
    }

    val fullNumber = remember(countryCodeInput, phoneInput) {
        val code = countryCodeInput.trim()
        val number = phoneInput.trim().replace(" ", "")
        if (code.length >= 2 && number.isNotEmpty()) "$code $number" else ""
    }

    val isValid = remember(fullNumber, dialCodeResult) {
        fullNumber.length >= 8 &&
                countryCodeInput.length >= 2 &&
                phoneInput.isNotBlank() &&
                dialCodeResult.state == DialCodeState.VALID
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DropdownColors.cardBg,
            elevation = 16.dp,
            modifier = Modifier.width(440.dp)
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
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = envColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Clear $envLabel Phone Number",
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
                        text = "Enter the phone number to clear from the $envLabel account",
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DropdownColors.fieldBg)
                                .border(
                                    1.dp,
                                    when (dialCodeResult.state) {
                                        DialCodeState.INVALID, DialCodeState.TOO_LONG -> Color(0xFFEF4444).copy(alpha = 0.5f)
                                        DialCodeState.VALID -> envColor.copy(alpha = 0.5f)
                                        else -> DropdownColors.fieldBorder
                                    },
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when (dialCodeResult.state) {
                                DialCodeState.INVALID, DialCodeState.TOO_LONG -> {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Invalid code",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                else -> {
                                    Text(
                                        text = dialCodeResult.flag,
                                        fontSize = 24.sp
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = countryCodeInput,
                            onValueChange = { newValue ->
                                val filtered = newValue.filter { it == '+' || it.isDigit() }
                                val digits = filtered.filter { it.isDigit() }
                                val result = "+$digits"
                                if (result.length <= 5) {
                                    countryCodeInput = result
                                }
                            },
                            placeholder = {
                                Text(
                                    "+44",
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Light,
                                    fontSize = 14.sp,
                                    color = DropdownColors.textDim
                                )
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = AppFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DropdownColors.textPrimary
                            ),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                backgroundColor = DropdownColors.fieldBg,
                                focusedBorderColor = envColor,
                                unfocusedBorderColor = DropdownColors.fieldBorder,
                                cursorColor = DropdownColors.textPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.width(80.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(DropdownColors.divider)
                        )
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { newValue ->
                                val filtered = newValue.filter { it.isDigit() || it == ' ' }
                                if (filtered.length <= 15) {
                                    phoneInput = filtered
                                }
                            },
                            placeholder = {
                                Text(
                                    "7911 123456",
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Light,
                                    fontSize = 14.sp,
                                    color = DropdownColors.textDim
                                )
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = AppFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = DropdownColors.textPrimary
                            ),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                backgroundColor = DropdownColors.fieldBg,
                                focusedBorderColor = envColor,
                                unfocusedBorderColor = DropdownColors.fieldBorder,
                                cursorColor = DropdownColors.textPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (countryCodeInput.length > 1) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            when (dialCodeResult.state) {
                                DialCodeState.VALID -> {
                                    Text(dialCodeResult.flag, fontSize = 12.sp)
                                    Text(
                                        text = dialCodeResult.country,
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 11.sp,
                                        color = DropdownColors.textMuted
                                    )
                                }
                                DialCodeState.TYPING -> {
                                    Text("🌐", fontSize = 12.sp)
                                    Text(
                                        text = "Typing...",
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 11.sp,
                                        color = DropdownColors.textDim
                                    )
                                }
                                DialCodeState.TOO_LONG -> {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Too many digits — check country code",
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 11.sp,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                                DialCodeState.INVALID -> {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Invalid country code",
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 11.sp,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                                DialCodeState.EMPTY -> { /* nothing */ }
                            }
                        }
                    }

                    if (fullNumber.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DropdownColors.fieldBg)
                                .border(1.dp, DropdownColors.fieldBorder, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Full number:",
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Light,
                                    fontSize = 11.sp,
                                    color = DropdownColors.textMuted
                                )
                                Text(
                                    text = fullNumber,
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = DropdownColors.textPrimary
                                )
                            }
                        }
                    }
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
                        onClick = onBack,
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

                    val clearInteraction = remember { MutableInteractionSource() }
                    val isClearHovered by clearInteraction.collectIsHoveredAsState()

                    Button(
                        onClick = { onClearPhone(fullNumber, environment) },
                        enabled = isValid,
                        interactionSource = clearInteraction,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (isClearHovered) envColor.copy(alpha = 0.9f) else envColor,
                            contentColor = Color.Black,
                            disabledBackgroundColor = envColor.copy(alpha = 0.3f),
                            disabledContentColor = Color.Black.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (isValid) envColor else envColor.copy(alpha = 0.3f)),
                        elevation = ButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            hoveredElevation = 0.dp
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Clear Phone",
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