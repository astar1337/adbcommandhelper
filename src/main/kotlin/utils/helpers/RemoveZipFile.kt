package utils.helpers

import java.awt.Desktop
import java.io.File

fun moveZipToTrash(filePath: String, permanentlyDelete: Boolean): Boolean  {
    val downloadsDir = File(System.getProperty("user.home"), "Downloads")
    val fileName = File(filePath)

    println("Checking directory: ${downloadsDir.absolutePath}")

    if (!fileName.exists()) {
        println("File not found: $filePath")
        return false
    }

    return if (permanentlyDelete) {
        println("Permanently deleting: ${fileName.name}")
        fileName.delete()
    } else {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)) {
            try {
                println("Moving to Trash: ${fileName.name}")
                Desktop.getDesktop().moveToTrash(fileName)
                true
            } catch (e: Exception) {
                println("Moving to trash failed, deleting the ZIP instead: ${e.message}")
                fileName.delete()
            }
        } else {
            fileName.delete()
        }
    }
}
