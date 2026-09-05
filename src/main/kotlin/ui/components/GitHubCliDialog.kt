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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ui.components.theme.AppFontFamily
import ui.components.theme.DropdownColors

enum class GitHubAuthError {
    CLI_NOT_INSTALLED,
    NOT_LOGGED_IN,
    TOKEN_INVALID
}

@Composable
fun gitHubCliDialog(
    error: GitHubAuthError,
    isChecking: Boolean = false,
    onRecheck: () -> Unit = {},
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DropdownColors.cardBg,
            elevation = 16.dp,
            modifier = Modifier
                .width(500.dp)
                .wrapContentHeight()
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFEB3B),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = when (error) {
                                    GitHubAuthError.CLI_NOT_INSTALLED -> "GitHub CLI Not Found"
                                    GitHubAuthError.NOT_LOGGED_IN -> "GitHub Authentication Required"
                                    GitHubAuthError.TOKEN_INVALID -> "GitHub Token Invalid"
                                },
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
                        text = when (error) {
                            GitHubAuthError.CLI_NOT_INSTALLED -> "GitHub CLI (gh) is required to trigger workflows"
                            GitHubAuthError.NOT_LOGGED_IN -> "You need to authenticate with GitHub first"
                            GitHubAuthError.TOKEN_INVALID -> "Your GitHub token has expired or is invalid"
                        },
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
                    when (error) {
                        GitHubAuthError.CLI_NOT_INSTALLED -> {
                            Text(
                                "Install GitHub CLI using Homebrew:",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = DropdownColors.textSecondary
                            )

                            commandBlock("brew install gh")

                            Text(
                                "Then authenticate:",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = DropdownColors.textSecondary
                            )

                            commandBlock("gh auth login")

                            Text(
                                "Follow the prompts to authenticate with your GitHub account. Select HTTPS and authenticate via browser.",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Light,
                                fontSize = 11.sp,
                                color = DropdownColors.textMuted,
                                lineHeight = 16.sp
                            )
                        }

                        GitHubAuthError.NOT_LOGGED_IN -> {
                            Text(
                                "Run the following command in your terminal:",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = DropdownColors.textSecondary
                            )

                            commandBlock("gh auth login")

                            Text(
                                "Select the following options when prompted:",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = DropdownColors.textSecondary
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                authStep("1", "Account: GitHub.com")
                                authStep("2", "Protocol: HTTPS")
                                authStep("3", "Authenticate: Login with a web browser")
                                authStep("4", "Copy the one-time code and press Enter")
                                authStep("5", "Complete authentication in your browser")
                            }
                        }

                        GitHubAuthError.TOKEN_INVALID -> {
                            Text(
                                "Your token may have expired. Re-authenticate:",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = DropdownColors.textSecondary
                            )

                            commandBlock("gh auth logout")
                            commandBlock("gh auth login")

                            Text(
                                "This will clear your existing token and prompt you to authenticate again.",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Light,
                                fontSize = 11.sp,
                                color = DropdownColors.textMuted,
                                lineHeight = 16.sp
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFEB3B).copy(alpha = 0.1f))
                            .border(1.dp, Color(0xFFFFEB3B).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFFFEB3B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Once complete, press Re-check to verify without restarting the app.",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            color = Color(0xFFFFEB3B)
                        )
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

                    Spacer(Modifier.width(8.dp))

                    val recheckInteraction = remember { MutableInteractionSource() }
                    val isRecheckHovered by recheckInteraction.collectIsHoveredAsState()

                    Button(
                        onClick = onRecheck,
                        enabled = !isChecking,
                        interactionSource = recheckInteraction,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (isRecheckHovered) DropdownColors.accentYellow
                            else DropdownColors.fieldBg,
                            contentColor = if (isRecheckHovered) Color.Black
                            else DropdownColors.textPrimary,
                            disabledBackgroundColor = DropdownColors.fieldBg,
                            disabledContentColor = DropdownColors.textDim
                        ),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isRecheckHovered) DropdownColors.accentYellow
                            else DropdownColors.fieldBorder
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
                            if (isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = DropdownColors.textMuted,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Re-check",
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
        }
    }
}

@Composable
private fun commandBlock(command: String) {
    val copyInteraction = remember { MutableInteractionSource() }
    val isCopyHovered by copyInteraction.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0A1015))
            .border(1.dp, DropdownColors.fieldBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$ $command",
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = Color(0xFF22C55E)
        )

        IconButton(
            onClick = {
                val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                val selection = java.awt.datatransfer.StringSelection(command)
                clipboard.setContents(selection, selection)
            },
            interactionSource = copyInteraction,
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = if (isCopyHovered) DropdownColors.textPrimary else DropdownColors.textMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun authStep(number: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFFFFEB3B).copy(alpha = 0.15f))
                .border(1.dp, Color(0xFFFFEB3B).copy(alpha = 0.3f), RoundedCornerShape(9.dp))
                .wrapContentSize(Alignment.Center)
        ) {
            Text(
                text = number,
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = Color(0xFFFFEB3B),
                lineHeight = 9.sp
            )
        }
        Text(
            text = text,
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 12.sp,
            color = DropdownColors.textMuted
        )
    }
}