package ui.components.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

private val LightColors = lightColors(
        primary = Color.Black,
        secondary = Color(0xFFFF9800),
        background = Color.White,
        surface = Color.White,
    )

@Composable
fun appTheme(content: @Composable () -> Unit) {
        MaterialTheme(
            colors = LightColors,
            shapes = MaterialTheme.shapes,
            content = content
        )
    }
object MenuColors {
    val panelBg = Color(0xFF0C161C)          // body — your header color
    val headerBg = Color(0xFF142028)         // lighter shade for "Device Settings" header
    val footerBg = Color(0xFF0C161C)         // same as body
    val border = Color(0xFF1E2A32)           // subtle border
    val fieldBg = Color(0xFF142028)          // fields match header shade
    val fieldBorder = Color(0xFF243038)      // field borders
    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFE0E0E0)
    val textMuted = Color(0xFF6B8090)
    val textDim = Color(0xFF4A6070)
    val divider = Color(0xFF1E2A32)
    val rowHover = Color(0xFF182630)
    val dangerRed = Color(0xFFEF4444)
    val checkboxChecked = Color(0xFFFFEB3B)

}
val gradientColors = listOf(
    Color(0xFFFFEB3B),
    Color(0xFFFDD835),
    Color(0xFFFFC107),
    Color(0xFFFF9800),
    Color(0xFFFF7043),
    Color(0xFFFF5252),
    Color(0xFFE91E63),
    Color(0xFFD81B60),
    Color(0xFFAB47BC),
    Color(0xFF9C27B0),
    Color(0xFFBA68C8),
    Color(0xFFE91E63),
    Color(0xFFFF5252),
    Color(0xFFFF7043),
    Color(0xFFFF9800),
    Color(0xFFFFC107),
    Color(0xFFFDD835),
    Color(0xFFFFEB3B)
)
val gradientColorsIcons = listOf(
    Color(0xFFFFEB3B),
    Color(0xFFFF9800),
    Color(0xFFE91E63),
    Color(0xFF9C27B0)
)


object DropdownColors {
    val cardBg = Color(0xFF0C161C)
    val headerBg = Color(0xFF142028)
    val border = Color(0xFF1E2A32)
    val fieldBg = Color(0xFF142028)
    val fieldBorder = Color(0xFF243038)
    val fieldBorderHover = Color(0xFF2E3E48)
    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFE0E0E0)
    val textMuted = Color(0xFF6B8090)
    val textDim = Color(0xFF4A6070)
    val textPlaceholder = Color(0xFF4A6070)
    val accentYellow = Color(0xFFFFEB3B)
    val divider = Color(0xFF1E2A32)
    val rowHover = Color(0xFF1A2830)
    val selectedHighlight = Color(0x1A22C55E)
    val focusBorder = Color(0xFFFFEB3B)
    val iconDefault = Color(0xFF6B8090)
    val iconHover = Color(0xFFFFFFFF)
    val dropdownBg = Color(0xFF142028)
    val dropdownBorder = Color(0xFF243038)
    val dialogHeaderBg = Color(0xFF142028)
    val panelBg = Color(0xFF0C161C)        // darker body (main content area)// lighter gray (select workflow area)
    val footerBg = Color(0xFF0C161C)        // same as body
    val required = Color(0xFFEF4444)         // red asterisk
    val green = Color(0xFF22C55E)            // green accent
    val greenHover = Color(0xFF16A34A)       // green hover
    val greenDisabled = Color(0x6622C55E)    // green disabled
    val hoverHighlight = Color(0x15FFFFFF)   // hover on items
    val orange = Color(0xFFFF9800)

    val headerBackgroundColor = Color(0xFF0C161C)
    val contentBackgroundColor = Color(0xFF142028)

}

val success = Color(0xFF22C55E) to FontWeight.Bold
val error = Color(0xFFEF4444) to FontWeight.Bold
val warning = Color(0xFFF59E0B) to FontWeight.Bold

val styledWords = buildMap {
    listOf(
        "Launched", "captured", "connected", "enabled",
        "Cleared", "200", "COMPLETED", "PASSED",
        "disconnected successfully", "Successfully installed", "Granted","successfully"
    ).forEach { put(it, success) }

    listOf(
        "disconnected", "disabled", "failed", "couldn't",
        "number not found", "404:", "FAILED", "FAIL", "Force stopped", "Deleted", "Revoked"
    ).forEach { put(it, error) }

    listOf(
        "restarted", "WARNED", "CANCELLED", "RUNNING", "Installing"
    ).forEach { put(it, warning) }
}