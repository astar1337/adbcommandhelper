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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ui.components.theme.AppFontFamily
import ui.components.theme.DropdownColors

@Composable
fun maestroLoginDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onRunTest: (email: String, password: String) -> Unit
) {
    if (!showDialog) return

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

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
                                imageVector = Icons.Default.Login,
                                contentDescription = null,
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Maestro Login Test",
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
                        text = "Enter credentials to run the login flow",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 12.sp,
                        color = DropdownColors.textMuted
                    )
                }

                Divider(color = DropdownColors.divider)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.1f))
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Please make sure that the account you're using is still valid.",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = Color(0xFFF59E0B)
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 4.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Email",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = DropdownColors.textSecondary
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                emailError = false
                            },
                            placeholder = {
                                Text(
                                    "user@dazn.com",
                                    fontFamily = AppFontFamily,
                                    fontSize = 13.sp,
                                    color = DropdownColors.textMuted
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = if (emailError) Color(0xFFEF4444) else DropdownColors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            isError = emailError,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = DropdownColors.textPrimary,
                                backgroundColor = DropdownColors.fieldBg,
                                focusedBorderColor = Color(0xFFA855F7),
                                unfocusedBorderColor = DropdownColors.fieldBorder,
                                errorBorderColor = Color(0xFFEF4444),
                                cursorColor = Color(0xFFA855F7)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = AppFontFamily,
                                fontSize = 13.sp
                            )
                        )
                        if (emailError) {
                            Text(
                                text = "Email is required",
                                fontFamily = AppFontFamily,
                                fontSize = 11.sp,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Password",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = DropdownColors.textSecondary
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                passwordError = false
                            },
                            placeholder = {
                                Text(
                                    "Enter password",
                                    fontFamily = AppFontFamily,
                                    fontSize = 13.sp,
                                    color = DropdownColors.textMuted
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (passwordError) Color(0xFFEF4444) else DropdownColors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = DropdownColors.textMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            isError = passwordError,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = DropdownColors.textPrimary,
                                backgroundColor = DropdownColors.fieldBg,
                                focusedBorderColor = Color(0xFFA855F7),
                                unfocusedBorderColor = DropdownColors.fieldBorder,
                                errorBorderColor = Color(0xFFEF4444),
                                cursorColor = Color(0xFFA855F7)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = AppFontFamily,
                                fontSize = 13.sp
                            )
                        )
                        if (passwordError) {
                            Text(
                                text = "Password is required",
                                fontFamily = AppFontFamily,
                                fontSize = 11.sp,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }

                Divider(color = DropdownColors.divider)

                // Footer
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
                        onClick = onDismiss,
                        interactionSource = cancelInteraction,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isCancelHovered) DropdownColors.textSecondary else DropdownColors.textMuted
                        )
                    ) {
                        Text(
                            "Go back",
                            fontFamily = AppFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    val runInteraction = remember { MutableInteractionSource() }
                    val isRunHovered by runInteraction.collectIsHoveredAsState()

                    Button(
                        onClick = {
                            emailError = email.isBlank()
                            passwordError = password.isBlank()

                            if (!emailError && !passwordError) {
                                onRunTest(email, password)
                            }
                        },
                        interactionSource = runInteraction,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (isRunHovered) Color(0xFF9333EA) else Color(0xFFA855F7),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        elevation = ButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            hoveredElevation = 0.dp
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Run Test",
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