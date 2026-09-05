import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ui.components.theme.appTheme
import utils.helpers.AppConfig
import views.mainScreen


@Composable
@Preview
fun app() {
    mainScreen()
}

fun main() {
    System.setProperty("apple.awt.application.name", "Android Helper")
    AppConfig.initialize()
    application {
        val windowState = rememberWindowState(
            size = DpSize(780.dp, 610.dp)
        )
        Window(
            onCloseRequest = ::exitApplication,
            title = "Android Helper",
            state = windowState,
        ) {
            appTheme {
                app()
            }
        }
    }
}