package utils.helpers
import config.AppConfig
import java.net.HttpURLConnection
import java.net.URL

enum class Environment {
    PRODUCTION, STAGING
}
data class PhoneRemovalResult(
    val success: Boolean,
    val responseCode: Int,
    val message: String
)

fun removePhoneNumberCurl(
    phoneNumber: String,
    environment: Environment = Environment.STAGING
): PhoneRemovalResult {
    val url: String
    val apiKey: String

    when (environment) {
        Environment.PRODUCTION -> {
            url = "https://user-prod.ar.indazn.com/v1/users/remove-phone-number"
            apiKey = AppConfig.PROD_API_KEY
        }
        Environment.STAGING -> {
            url = "https://user-stage-eu-central-1.ar.dazn-stage.com/v1/users/remove-phone-number"
            apiKey = AppConfig.STAG_API_KEY
        }
    }

    val jsonBody = """{"phoneNumber": "$phoneNumber"}"""

    return try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "PUT"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("x-api-key", apiKey)
        connection.doOutput = true
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        connection.outputStream.use { os ->
            os.write(jsonBody.toByteArray())
        }

        val responseCode = connection.responseCode
        val responseBody = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().readText()
        } else {
            connection.errorStream?.bufferedReader()?.readText() ?: "No error body"
        }

        connection.disconnect()

        PhoneRemovalResult(
            success = responseCode in 200..299,
            responseCode = responseCode,
            message = responseBody
        )
    } catch (e: Exception) {
        PhoneRemovalResult(
            success = false,
            responseCode = -1,
            message = e.message ?: "Unknown error"
        )
    }
}