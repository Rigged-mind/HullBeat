package app.hullbeat.data.storage

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object AttachmentStorage {
    private const val TAG = "AttachmentStorage"

    /**
     * Copies the content from [sourceUri] into the app's internal storage
     * under [subDir] (e.g. "attachments" or "vessels").
     *
     * Returns the file:// [Uri] of the copied file, or null on failure.
     * If [sourceUri] already points to an existing file in that internal directory,
     * it is returned as-is to avoid duplicate copying.
     */
    fun copyToInternalStorage(
        context: Context,
        sourceUri: Uri,
        subDir: String,
        prefix: String = "img_",
    ): Uri? {
        val targetDir = File(context.filesDir, subDir)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        // If it is already an internal file inside targetDir, do not duplicate it.
        if (sourceUri.scheme == "file" || sourceUri.scheme == null) {
            val path = sourceUri.path
            if (path != null) {
                val existingFile = File(path)
                val canonicalTarget = runCatching { targetDir.canonicalPath }.getOrNull()
                val canonicalFile = runCatching { existingFile.canonicalPath }.getOrNull()
                if (canonicalTarget != null && canonicalFile != null && canonicalFile.startsWith(canonicalTarget) && existingFile.exists()) {
                    return Uri.fromFile(existingFile)
                }
            }
        }

        val ext = getExtension(context, sourceUri) ?: "jpg"
        val fileName = "${prefix}${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$ext"
        val destFile = File(targetDir, fileName)

        return try {
            openInputStream(context, sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: run {
                Log.w(TAG, "Cannot open input stream for $sourceUri")
                return null
            }
            Uri.fromFile(destFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy image from $sourceUri to ${destFile.absolutePath}", e)
            if (destFile.exists()) {
                destFile.delete()
            }
            null
        }
    }

    /**
     * Safely deletes a file from internal storage if it resides within [Context.getFilesDir].
     */
    fun deleteInternalFile(context: Context, uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return false
        val path = uri.path ?: return false
        val file = File(path)

        return try {
            val canonicalFilesDir = context.filesDir.canonicalPath
            val canonicalFile = file.canonicalPath
            if (canonicalFile.startsWith(canonicalFilesDir) && file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete internal file $uriString", e)
            false
        }
    }

    /**
     * Opens an [InputStream] for [uri], directly opening [FileInputStream] for file://
     * schemes before falling back to [android.content.ContentResolver.openInputStream].
     */
    fun openInputStream(context: Context, uri: Uri): InputStream? {
        if (uri.scheme == "file" || uri.scheme == null) {
            val path = uri.path
            if (path != null) {
                val file = File(path)
                if (file.exists()) {
                    return runCatching { FileInputStream(file) }.getOrNull()
                }
            }
        }
        return runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
    }

    private fun getExtension(context: Context, uri: Uri): String? {
        val mimeType = if (uri.scheme == "content") {
            runCatching { context.contentResolver.getType(uri) }.getOrNull()
        } else {
            val path = uri.path ?: return null
            val ext = MimeTypeMap.getFileExtensionFromUrl(path)
            if (ext.isNotEmpty()) return ext
            null
        }
        return mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
    }
}
