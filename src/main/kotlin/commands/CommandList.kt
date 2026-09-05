package commands

import utils.helpers.*
import utils.helpers.AppConfig.adbPath
import utils.helpers.findAdbPath

val appActivity = "com.vpn.android.pureb2b/com.vpn.SplashActivity"

fun moreSettings(deviceId: String): Map<String, String> {
    val adb = adbPath
    return mapOf(
        "Home" to "$adb -s $deviceId shell input keyevent KEYCODE_HOME",
        "Back" to "$adb -s $deviceId shell input keyevent KEYCODE_BACK",
        "Overview" to "$adb -s $deviceId shell input keyevent KEYCODE_APP_SWITCH",
        "OK" to "$adb -s $deviceId shell input keyevent 66",
    )
}

data class AppDefinition(
    val displayName: String,
    val prodPackage: String,
    val stagPackage: String,
    val activityPath: String,
    val iconRes: String
)

val appConfigs = mapOf(
    "KAYO" to AppDefinition(
        displayName = "KAYO",
        prodPackage = "au.com.kayosports",
        stagPackage = "au.com.kayosports.debug",
        activityPath = "com.dazn.splash.view.SplashScreenActivity",
        iconRes = "icons/kayoico.png"
    ),
    "BINGE" to AppDefinition(
        displayName = "BINGE",
        prodPackage = "au.com.streamotion.ares",
        stagPackage = "au.com.streamotion.ares.debug",
        activityPath = "com.dazn.splash.view.SplashScreenActivity",
        iconRes = "icons/bingeico.png"
    ),
    "DAZN" to AppDefinition(
        displayName = "DAZN",
        prodPackage = "com.dazn",
        stagPackage = "com.dazn.stag",
        activityPath = "com.dazn.splash.view.SplashScreenActivity",
        iconRes = "icons/daznlogo.jpg"
    )
)

val appList = appConfigs.mapValues { it.value.displayName }

fun envList(appKey: String): Map<String, String> {
    val config = appConfigs[appKey] ?: return emptyMap()
    return mapOf(
        "${Emoji.PROD_GREEN} PROD" to config.prodPackage,
        "${Emoji.STAGE_TEST} STAG" to config.stagPackage
    )
}

fun getActivityPath(appKey: String): String {
    if (appKey.isEmpty()) return ""
    return appConfigs[appKey]?.activityPath
        ?: error("Unknown app: $appKey")
}

data class Gateway(val name: String, val index: Int)

data class RegionGateway(
    val name: String,
    val index: Int,
    val url: String,
    val flagEmoji: String
)
enum class QuickActionType {
    COMMAND,
    AUTOMATION
}

val regions = listOf(
    Gateway("DAZNAustralia", 0),
    Gateway("DAZNFrance", 1),
    Gateway("DAZNMexico", 2),
    Gateway("DAZNNetherlands", 3),
    Gateway("DAZNPoland", 4),
    Gateway("DAZNSingapore", 5),
    Gateway("DAZNSouthKorea", 6),
    Gateway("DAZNSweden", 7),
    Gateway("DAZNArgentina", 8),
    Gateway("DAZNIndonesia", 9),
    Gateway("DAZNTaiwan", 10),
    Gateway("DAZNThailand", 11),
    Gateway("DAZNVietnam", 12),
    Gateway("DAZNSaudiArabia", 13),
    Gateway("DAZNUAE", 14),
    Gateway("DAZNMalaysia", 15),
    Gateway("DAZNBulgaria", 16),
    Gateway("DAZNAustria", 17),
    Gateway("DAZNFinland", 18),
    Gateway("DAZNNorway", 19),
    Gateway("DAZNBrazil", 20),
    Gateway("DAZNColombia", 21),
    Gateway("DAZNItaly", 22),
    Gateway("DAZNBelgium", 23),
    Gateway("DAZNCanada", 24),
    Gateway("DAZNChina", 25),
    Gateway("DAZNDenmark", 26),
    Gateway("DAZNGermany", 27),
    Gateway("DAZNIreland", 28),
    Gateway("DAZNSpain", 29),
    Gateway("DAZNUK", 30),
    Gateway("DAZNUSA", 31),
    Gateway("DAZNEgypt", 32),
    Gateway("DAZNSeychelles", 33),
    Gateway("DAZNPortugal", 34),
    Gateway("DAZNLuxembourg", 35),
    Gateway("DAZNNewZealand", 36),
    Gateway("DAZNSwitzerland", 37),
    Gateway("DAZNIndia01", 38),
    Gateway("DAZNJapan", 39),
    Gateway("DAZNAuTesting", 40)
)

data class RegionMarcoPolo(
    val name: String,
    val index: Int,
    val flagEmoji: String,
    val guid: String
)


data class CountryCode(
    val dialCode: String,
    val country: String,
    val flag: String
)

val countryCodes = listOf(
    CountryCode("+1", "United States", "🇺🇸"),
    CountryCode("+1", "Canada", "🇨🇦"),
    CountryCode("+44", "United Kingdom", "🇬🇧"),
    CountryCode("+49", "Germany", "🇩🇪"),
    CountryCode("+43", "Austria", "🇦🇹"),
    CountryCode("+41", "Switzerland", "🇨🇭"),
    CountryCode("+39", "Italy", "🇮🇹"),
    CountryCode("+34", "Spain", "🇪🇸"),
    CountryCode("+33", "France", "🇫🇷"),
    CountryCode("+31", "Netherlands", "🇳🇱"),
    CountryCode("+32", "Belgium", "🇧🇪"),
    CountryCode("+351", "Portugal", "🇵🇹"),
    CountryCode("+353", "Ireland", "🇮🇪"),
    CountryCode("+48", "Poland", "🇵🇱"),
    CountryCode("+46", "Sweden", "🇸🇪"),
    CountryCode("+47", "Norway", "🇳🇴"),
    CountryCode("+45", "Denmark", "🇩🇰"),
    CountryCode("+358", "Finland", "🇫🇮"),
    CountryCode("+55", "Brazil", "🇧🇷"),
    CountryCode("+54", "Argentina", "🇦🇷"),
    CountryCode("+57", "Colombia", "🇨🇴"),
    CountryCode("+56", "Chile", "🇨🇱"),
    CountryCode("+52", "Mexico", "🇲🇽"),
    CountryCode("+81", "Japan", "🇯🇵"),
    CountryCode("+82", "South Korea", "🇰🇷"),
    CountryCode("+86", "China", "🇨🇳"),
    CountryCode("+91", "India", "🇮🇳"),
    CountryCode("+65", "Singapore", "🇸🇬"),
    CountryCode("+60", "Malaysia", "🇲🇾"),
    CountryCode("+62", "Indonesia", "🇮🇩"),
    CountryCode("+66", "Thailand", "🇹🇭"),
    CountryCode("+84", "Vietnam", "🇻🇳"),
    CountryCode("+886", "Taiwan", "🇹🇼"),
    CountryCode("+971", "UAE", "🇦🇪"),
    CountryCode("+966", "Saudi Arabia", "🇸🇦"),
    CountryCode("+20", "Egypt", "🇪🇬"),
    CountryCode("+61", "Australia", "🇦🇺"),
    CountryCode("+64", "New Zealand", "🇳🇿"),
    CountryCode("+352", "Luxembourg", "🇱🇺"),
    CountryCode("+359", "Bulgaria", "🇧🇬"),
    CountryCode("+248", "Seychelles", "🇸🇨"),
    CountryCode("+7", "Russia", "🇷🇺"),
    CountryCode("+90", "Turkey", "🇹🇷"),
    CountryCode("+27", "South Africa", "🇿🇦"),
    CountryCode("+234", "Nigeria", "🇳🇬"),
    CountryCode("+63", "Philippines", "🇵🇭"),
    CountryCode("+92", "Pakistan", "🇵🇰"),
    CountryCode("+880", "Bangladesh", "🇧🇩"),
    CountryCode("+94", "Sri Lanka", "🇱🇰"),
)

fun String.toFlagEmoji(): String = when {
    contains("Australia") -> "🇦🇺"
    contains("Germany") -> "🇩🇪"
    contains("Austria") -> "🇦🇹"
    contains("Switzerland") -> "🇨🇭"
    contains("Canada") -> "🇨🇦"
    contains("Japan") -> "🇯🇵"
    contains("Italy") -> "🇮🇹"
    contains("USA") || contains("United States") -> "🇺🇸"
    contains("Spain") -> "🇪🇸"
    contains("Brazil") -> "🇧🇷"
    contains("France") && !contains("French") -> "🇫🇷"
    contains("UK") || contains("United Kingdom") -> "🇬🇧"
    contains("Belgium") -> "🇧🇪"
    contains("Portugal") -> "🇵🇹"
    contains("Netherlands") -> "🇳🇱"
    contains("Poland") -> "🇵🇱"
    contains("Mexico") -> "🇲🇽"
    contains("Argentina") -> "🇦🇷"
    contains("Colombia") -> "🇨🇴"
    contains("Chile") -> "🇨🇱"
    contains("Ireland") -> "🇮🇪"
    contains("Seychelles") -> "🇸🇨"
    contains("Singapore") -> "🇸🇬"
    contains("SouthKorea") || contains("South Korea") -> "🇰🇷"
    contains("Sweden") -> "🇸🇪"
    contains("Indonesia") -> "🇮🇩"
    contains("Taiwan") -> "🇹🇼"
    contains("Thailand") -> "🇹🇭"
    contains("Vietnam") -> "🇻🇳"
    contains("SaudiArabia") || contains("Saudi Arabia") -> "🇸🇦"
    contains("UAE") -> "🇦🇪"
    contains("Malaysia") -> "🇲🇾"
    contains("Bulgaria") -> "🇧🇬"
    contains("Finland") -> "🇫🇮"
    contains("Norway") -> "🇳🇴"
    contains("Denmark") -> "🇩🇰"
    contains("China") -> "🇨🇳"
    contains("Egypt") -> "🇪🇬"
    contains("Luxembourg") -> "🇱🇺"
    contains("NewZealand") || contains("New Zealand") -> "🇳🇿"
    contains("India") -> "🇮🇳"
    else -> "🌐"
}
data class RegionOption(val name: String, val index: Int, val guid: String)

val mpRegion = listOf(
    RegionOption("Argentina", 10,   "a3f1c2d4-1e2b-4a3c-8d5e-6f7a8b9c0d1e"),
    RegionOption("Austria", 14,     "b4e2d3f5-2f3c-4b4d-9e6f-7a8b9c0d1e2f"),
    RegionOption("Brazil", 31,      "c5f3e4a6-3a4d-4c5e-af7a-8b9c0d1e2f3a"),
    RegionOption("Canada", 40,      "d6a4f5b7-4b5e-4d6f-8a8b-9c0d1e2f3a4b"),
    RegionOption("Germany", 83,     "e7b5a6c8-5c6f-4e7a-9b9c-0d1e2f3a4b5c"),
    RegionOption("Italy", 110,      "f8c6b7d9-6d7a-4f8b-8c0d-1e2f3a4b5c6d"),
    RegionOption("Japan", 112,      "a9d7c8e0-7e8b-4a9c-ad1e-2f3a4b5c6d7e"),
    RegionOption("Spain", 210,      "b0e8d9f1-8f9c-4b0d-be2f-3a4b5c6d7e8f"),
    RegionOption("Switzerland", 216,"c1f9e0a2-9a0d-4c1e-8f3a-4b5c6d7e8f9a"),
    RegionOption("United States of America (the)", 237, "d2a0f1b3-0b1e-4d2f-9a4b-5c6d7e8f9a0b"),
    RegionOption("Belgium", 21,     "e3b1a2c4-1c2f-4e3a-8b5c-6d7e8f9a0b1c"),
    RegionOption("Chile", 44,       "f4c2b3d5-2d3a-4f4b-9c6d-7e8f9a0b1c2d"),
    RegionOption("Colombia", 48,    "a5d3c4e6-3e4b-4a5c-8d7e-8f9a0b1c2d3e"),
    RegionOption("France", 76,      "b6e4d5f7-4f5c-4b6d-ae8f-9a0b1c2d3e4f"),
    RegionOption("Ireland", 107,    "c7f5e6a8-5a6d-4c7e-bf9a-0b1c2d3e4f5a"),
    RegionOption("Mexico", 144,     "d8a6f7b9-6b7e-4d8f-8a0b-1c2d3e4f5a6b"),
    RegionOption("Netherlands (the)", 157, "e9b7a8c0-7c8f-4e9a-9b1c-2d3e4f5a6b7c"),
    RegionOption("Poland", 178,     "f0c8b9d1-8d9a-4f0b-ac2d-3e4f5a6b7c8d"),
    RegionOption("Portugal", 179,   "a1d9c0e2-9e0b-4a1c-bd3e-4f5a6b7c8d9e"),
    RegionOption("United Kingdom of Great Britain and Northern Ireland (the)", 235, "b2e0d1f3-0f1c-4b2d-8e4f-5a6b7c8d9e0f"),
    RegionOption("Seychelles", 199, "c3f1e2a4-1a2d-4c3e-9f5a-6b7c8d9e0f1a"),
)
object Emoji {
    const val CHECK = "\uD83D\uDD0D"
    const val LAUNCH = "\uD83D\uDE80"
    const val STOP = "\u26D4"
    const val MOBILE_PHONE_WITH_ARROW = "\uD83D\uDCF2"
    const val DELETE = "\uD83D\uDDD1\uFE0F"
    const val CLEAR_ALL = "\uD83E\uDDF9"
    const val CLEAR_CACHE = "\uD83E\uDDFC"
    const val POOP = "\uD83D\uDCA9"
    const val PUKE = "\uD83E\uDD2E"
    const val TRASH = "\uD83D\uDDD1"
    const val STAGE_TEST = "\uD83E\uDDEA"
    const val PROD_GREEN = "\uD83D\uDFE2"
    const val PHONE_MOBILE = "\uD83D\uDCF1"
    const val EMULATOR_COMPUTER = "\uD83D\uDDA5"
    const val NO_DEVICE = "\u274C"
    const val AUSTRALIA_FLAG = "\uD83C\uDDE6\uD83C\uDDFA"
    const val AU_KOALA = "\uD83D\uDC28"
    const val AU_KANGAROO = "\uD83E\uDD98"
    const val MOVIE_CLAPPER = "\uD83C\uDFAC"
    const val MOVIE_CAMERA = "\uD83D\uDCF9"
    const val MOVIE_POPCORN = "\uD83C\uDF7F"
    const val SPORT_TROPHY = "\uD83C\uDFC6"
    const val SPORT_BALL = "\u26BD"
    const val SPORT_MEDAL = "\uD83C\uDFC5"
    const val SCREENSHOT_CAMERA = "\uD83D\uDCF8"
    const val WIFI_OFF = "\uD83D"
    const val WIFI_ON = "\uD83D\uDCF6"
    const val SETTINGS_GEAR = "\u2699\uFE0F"
    const val RESTART = "\uD83D\uDD04"
    const val NERD_POINTING_UP = "\uD83E\uDD13\u261D\uFE0F"
    const val AIRPLANE = "\u2708\uFE0F"
    const val TURNED_OFF = "\u274C"
    const val CLIPBOARD = "\uD83D\uDCCB"
    const val PASTE_MEMO = "\uD83D\uDCDD"
    const val CHECK_GREEN = "\u2705"
    const val MOBILE_WITH_ARROW = "\uD83D\uDCF2"
    const val CLEAN_SOAP = "\uD83E\uDDFC"
    const val CLEAN_SPONGE = "\uD83E\uDDFD"
    const val PACKAGE_EMOJI = "\uD83D\uDCE6"

    data class OutPutResultsFormatted(
        val deviceChosen: List<AdbDeviceId>,
        val appChosen: String,
        val environmentChosen: Boolean,
        val commandChosen: String,
        val successMessage: String? = null
    )
}

enum class TutorialStep {
    DEVICE_DROPDOWN,
    APP_DROPDOWN,
    ENV_DROPDOWN,
    QUICK_ACTIONS,
    CONSOLE,
    HEADER_BUTTONS,
    DONE
}

fun commandOptions(
    deviceId: String,
    envPrefix: String,
    notifState: NotificationState,
    envName: String,
    appKey: String,
): List<Command> {
    if (appKey.isEmpty()) return emptyList()

    val adb = AppConfig.adbPath
    val sdk = getAndroidSdk(deviceId)
    val activityPath = getActivityPath(appKey)
    val commands = mutableListOf<Command>()

    commands += Command(
        id = "launch",
        labelParts = listOf(
            CommandLabel.Text("Launch $envName "),
            CommandLabel.AppIcon(appKey)
        ),
        adbCommand = "$adb -s $deviceId shell am start -n $envPrefix/$activityPath"
    )

    commands += Command(
        id = "force_stop",
        labelParts = listOf(
            CommandLabel.Text("Force Stop $envName "),
            CommandLabel.AppIcon(appKey)
        ),
        adbCommand = "$adb -s $deviceId shell am force-stop $envPrefix"
    )

    commands += Command(
        id = "delete_keep_data",
        labelParts = listOf(
            CommandLabel.Text("Delete $envName "),
            CommandLabel.AppIcon(appKey),
            CommandLabel.Text(" without deleting files")
        ),
        adbCommand = "$adb -s $deviceId shell pm uninstall -k $envPrefix"
    )

    commands += Command(
        id = "delete",
        labelParts = listOf(
            CommandLabel.Text("Delete $envName "),
            CommandLabel.AppIcon(appKey)
        ),
        adbCommand = "$adb -s $deviceId uninstall $envPrefix"
    )

    commands += Command(
        id = "marco_polo",
        labelParts = listOf(
            CommandLabel.Text("Delete $envName "),
            CommandLabel.AppIcon(appKey)
        ),
        adbCommand = "$adb -s $deviceId uninstall $envPrefix"
    )

    commands += Command(
        id = "clear_data",
        labelParts = listOf(
            CommandLabel.Text("Clear $envName "),
            CommandLabel.AppIcon(appKey),
            CommandLabel.Text(" Cache & Data")
        ),
        adbCommand = "$adb -s $deviceId shell pm clear $envPrefix"
    )

    commands += notificationCommand(
        deviceId = deviceId,
        appId = envPrefix,
        envName = envName,
        sdk = sdk,
        appKey = appKey,
        state = notifState
    )

    return commands
}

data class WorkflowResult(
    val workflowId: String,
    val workflowName: String,
    val branch: String,
    val fieldValues: Map<String, String>,
    val runId: Long? = null,
    val error: String? = null
)

data class WorkflowConfig(
    val id: String,
    val name: String,
    val description: String,
    val icon: String = "🚀",
    val branches: List<String> = listOf("main", "develop")
)

val workflowConfigs = listOf(
    WorkflowConfig(
        id = "apk_builder_mobile",
        name = "${Emoji.MOBILE_PHONE_WITH_ARROW} APK Builder Mobile",
        description = "Build and generate APK for mobile platforms",
        icon = "\uD83D\uDCE6",
        branches = listOf("main", "develop")
    )
)

data class WorkflowInput(
    val id: String,
    val description: String,
    val required: Boolean,
    val type: String,
    val default: String,
    val options: List<String>
)

fun formatWorkflowOutput(result: WorkflowResult): String {
    val brand = result.fieldValues["brand"] ?: ""
    val fields = result.fieldValues.entries.joinToString("\n") { (key, value) ->
        "  $key: $value"
    }

    if (result.error != null) {
        return buildString {
            appendLine("Workflow '${result.workflowName}' failed to trigger on branch: ${result.branch}")
            appendLine("Error: ${result.error}")
        }
    }

    return buildString {
        appendLine("Workflow '${result.workflowName}' triggered on branch: ${result.branch} and on brand: $brand")
        if (result.runId != null) {
            appendLine("Run ID: ${result.runId}")
            appendLine("Status: In Progress")
        }
        appendLine("Branch: ${result.branch}")
        appendLine("Parameters:")
        append(fields)
    }
}