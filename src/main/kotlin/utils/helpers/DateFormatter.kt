package utils.helpers

fun formatDate(raw: String): String {
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val cleaned = raw.replace("T", " ").replace("Z", "").take(16)
    val parts = cleaned.split("-", limit = 3)
    if (parts.size >= 3) {
        val monthIndex = parts[1].toIntOrNull()
        if (monthIndex != null && monthIndex in 1..12) {
            val monthName = months[monthIndex - 1]
            return "${parts[0]}-$monthName-${parts[2]}"
        }
    }
    return cleaned
}