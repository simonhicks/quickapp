package quickapp.plugin

/**
 * Cleans a filename by:
 * - stripping non-alphanumeric characters
 * - converting to lowercase
 * - removing file extension
 */
fun cleanFilename(fileName: String): String {
    // Remove file extension first by splitting on last dot
    val nameWithoutExtension = fileName.substringBeforeLast(".", fileName)
    
    // Keep only alphanumeric characters and convert to lowercase
    return nameWithoutExtension
        .filter { it.isLetterOrDigit() }
        .lowercase()
} 