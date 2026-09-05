package ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ui.components.theme.AppFontFamily
import utils.helpers.Notification
import utils.helpers.NotificationType


@Composable
fun notificationHost(
    notification: Notification?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = notification != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        notification?.let {
            Card(
                elevation = 10.dp,
                shape = MaterialTheme.shapes.medium,
                backgroundColor = when (it.type) {
                    NotificationType.SUCCESS -> Color(0xFF4CAF50)
                    NotificationType.ERROR -> Color(0xFFE53935)
                    NotificationType.WARNING ->Color(0xFFFFC107)
                }
            ) {
                Text(
                    text = it.message,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
