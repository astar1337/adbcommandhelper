package ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.components.theme.AppFontFamily
import ui.components.theme.DropdownColors
import ui.components.theme.DropdownColors.textDim

@Composable
fun deviceInfoItem(
    label: String,
    value: String,
    onCopy: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = DropdownColors.textMuted,
            textAlign = TextAlign.Center
        )

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
                text = value.ifEmpty { "Not available" },
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = textDim,
                softWrap = true,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            if (onCopy != null) {
                Spacer(Modifier.width(8.dp))

                val copyInteraction = remember { MutableInteractionSource() }
                val isCopyHovered by copyInteraction.collectIsHoveredAsState()

                IconButton(
                    onClick = onCopy,
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
    }
}