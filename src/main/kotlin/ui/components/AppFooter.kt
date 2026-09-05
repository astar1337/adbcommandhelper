package ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.components.theme.AppFontFamily
import ui.components.theme.DropdownColors
import java.awt.Desktop
import java.net.URI

@Composable
fun appFooter(
    version: String = "",
    githubUrl: String = "",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Version $version",
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = DropdownColors.textDim
        )

        if (githubUrl.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .offset(y = 3.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        try {
                            if (Desktop.isDesktopSupported()) {
                                Desktop.getDesktop().browse(URI(githubUrl))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = "GitHub",
                    tint = DropdownColors.textDim,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}