package utils.helpers

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import commands.appConfigs
import ui.components.theme.AppFontFamily

@Composable
fun appInlineLabel(
    appKey: String,
    textColor: Color = MaterialTheme.colors.onSurface,
    iconSize: Dp = 25.dp,
    enabled: Boolean = true,
    fontSize: TextUnit = 13.sp,
    ) {
    val app = appConfigs[appKey]

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        app?.iconRes?.let { icon ->
            Image(
                painter = painterResource(icon),
                contentDescription = app.displayName,
                modifier = Modifier
                    .size(iconSize)
                    .alpha(if (enabled) 1f else 0.4f)
            )
            Spacer(Modifier.width(8.dp))
        }

        Text(
            text = app?.displayName ?: appKey,
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Normal,
            color = textColor,
            fontSize = fontSize
            )
    }
}