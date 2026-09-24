package app.hullbeat.domain.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import app.hullbeat.HullBeatApp
import app.hullbeat.data.db.AppDatabase
import app.hullbeat.data.db.Attachment
import app.hullbeat.data.db.Vessel
import app.hullbeat.data.preferences.AppPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.ByteArray
import kotlin.text.Charsets

@Serializable
data class BackupManifest(
    val version: Int = 1,
    val appName: String = "HullBeat",
    val exportedAt: Long = System.currentTimeMillis(),
    val attachments: List<AttachmentEntry> = emptyList(),
    val vesselPhotos: List<VesselPhotoEntry> = emptyList(),
)

@Serializable
data class AttachmentEntry(
    val id: Long,
    val archivePath: String,
)

@Serializable
data class VesselPhotoEntry(
    val vesselId: Long,
    val archivePath: String,
)

object BackupManager {

    val SQLITE_HEADER: ByteArray = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
    val ZIP_MAGIC: ByteArray = byteArrayOf(0x50.toByte(), 0x4B.toByte(), 0x03.toByte(), 0x04.toByte())

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun isZipHeader(header: ByteArray): Boolean {
        if (header.size < 4) return false
        return header[0] == ZIP_MAGIC[0] &&
            header[1] == ZIP_MAGIC[1] &&
            header[2] == ZIP_MAGIC[2] &&
            header[3] == ZIP_MAGIC[3]
    }

    fun isSqliteHeader(header: ByteArray): Boolean {
        if (header.size < 16) return false
        return header.sliceArray(0 until 16).contentEquals(SQLITE_HEADER)
    }

    suspend fun createBackup(context: Context, db: AppDatabase, outputStream: OutputStream): Boolean {
        return try {
            // Only the main .db file goes into the archive, so everything still
            // sitting in the WAL must be folded into it first. A checkpoint
            // that could not finish does not throw: it returns busy = 1, and
            // the copy made after it would silently miss the newest rows.
            // Better no backup than one that looks complete and is not.
            val checkpointed = db.openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(FULL)")
                .use { it.moveToFirst() && it.getInt(0) == 0 }
            if (!checkpointed) return false

            val dbFile = context.getDatabasePath(AppDatabase.NAME)
            if (!dbFile.exists()) return false

            val attachments = try {
                db.attachmentDao().allAttachments()
            } catch (_: Exception) {
                emptyList()
            }

            val vessels = try {
                db.vesselDao().allActive()
            } catch (_: Exception) {
                emptyList()
            }

            val zipOut = ZipOutputStream(outputStream)

            // 1. Pack database file
            zipOut.putNextEntry(ZipEntry("database/hullbeat.db"))
            dbFile.inputStream().use { it.copyTo(zipOut) }
            zipOut.closeEntry()

            // 2. Pack attachments
            val attachmentEntries = mutableListOf<AttachmentEntry>()
            for (att in attachments) {
                if (att.fileUri.isBlank()) continue
                val archivePath = "attachments/${att.id}.bin"
                val stream = openUriStream(context, att.fileUri) ?: continue
                try {
                    zipOut.putNextEntry(ZipEntry(archivePath))
                    stream.use { it.copyTo(zipOut) }
                    zipOut.closeEntry()
                    attachmentEntries.add(AttachmentEntry(id = att.id, archivePath = archivePath))
                } catch (_: Exception) {
                }
            }

            // 3. Pack vessel photos
            val vesselEntries = mutableListOf<VesselPhotoEntry>()
            for (vessel in vessels) {
                val photoUri = vessel.photoUri
                if (photoUri.isNullOrBlank()) continue
                val archivePath = "vessels/${vessel.id}.bin"
                val stream = openUriStream(context, photoUri) ?: continue
                try {
                    zipOut.putNextEntry(ZipEntry(archivePath))
                    stream.use { it.copyTo(zipOut) }
                    zipOut.closeEntry()
                    vesselEntries.add(VesselPhotoEntry(vesselId = vessel.id, archivePath = archivePath))
                } catch (_: Exception) {
                }
            }

            // 4. Pack manifest
            val manifest = BackupManifest(
                version = 1,
                appName = "HullBeat",
                exportedAt = System.currentTimeMillis(),
                attachments = attachmentEntries,
                vesselPhotos = vesselEntries,
            )
            val manifestBytes = json.encodeToString(manifest).toByteArray(Charsets.UTF_8)
            zipOut.putNextEntry(ZipEntry("manifest.json"))
            zipOut.write(manifestBytes)
            zipOut.closeEntry()

            zipOut.finish()
            zipOut.flush()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun restoreBackup(context: Context, inputStream: InputStream): Boolean {
        val tempInputFile = File(context.cacheDir, "restore_temp_input")
        try {
            inputStream.use { input ->
                tempInputFile.outputStream().use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }

            if (tempInputFile.length() < 16) {
                tempInputFile.delete()
                return false
            }

            val header = ByteArray(16)
            tempInputFile.inputStream().use { it.read(header) }

            return when {
                isZipHeader(header) -> restoreZipBackup(context, tempInputFile)
                isSqliteHeader(header) -> restoreSqliteBackup(context, tempInputFile)
                else -> {
                    tempInputFile.delete()
                    false
                }
            }
        } catch (_: Exception) {
            tempInputFile.delete()
            return false
        }
    }

    private fun restoreSqliteBackup(context: Context, tempDbFile: File): Boolean {
        try {
            if (!verifyDatabaseIntegrity(tempDbFile)) {
                tempDbFile.delete()
                return false
            }

            // Checkpoint any pending WAL frames and convert journal mode to DELETE
            val checkpointDb = SQLiteDatabase.openDatabase(
                tempDbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE,
            )
            try {
                checkpointDb.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
                checkpointDb.execSQL("PRAGMA journal_mode = DELETE")
            } catch (_: Exception) {
            } finally {
                checkpointDb.close()
            }
            File(tempDbFile.path + "-wal").delete()
            File(tempDbFile.path + "-shm").delete()

            // Close existing Room database cleanly before overwriting database files
            (context.applicationContext as? HullBeatApp)?.closeAndResetDatabase()

            val targetDb = context.getDatabasePath(AppDatabase.NAME)
            targetDb.parentFile?.mkdirs()
            File(targetDb.path + "-wal").delete()
            File(targetDb.path + "-shm").delete()
            tempDbFile.copyTo(targetDb, overwrite = true)
            tempDbFile.delete()
            File(targetDb.path + "-wal").delete()
            File(targetDb.path + "-shm").delete()

            AppPreferences.setSelectedVesselId(context, null)
            return true
        } catch (_: Exception) {
            tempDbFile.delete()
            File(tempDbFile.path + "-wal").delete()
            File(tempDbFile.path + "-shm").delete()
            return false
        }
    }

    private fun restoreZipBackup(context: Context, zipFileToRestore: File): Boolean {
        val tempDbFile = File(context.cacheDir, "restore_temp.db")
        try {
            val zipFile = ZipFile(zipFileToRestore)
            val dbEntry = zipFile.getEntry("database/hullbeat.db")
                ?: zipFile.getEntry("database\\hullbeat.db")
                ?: zipFile.entries().asSequence().firstOrNull {
                    it.name.endsWith(".db", ignoreCase = true) && !it.isDirectory
                }
            if (dbEntry == null) {
                zipFile.close()
                zipFileToRestore.delete()
                return false
            }

            zipFile.getInputStream(dbEntry).use { inStream ->
                tempDbFile.outputStream().use { outStream ->
                    inStream.copyTo(outStream)
                    outStream.flush()
                }
            }

            if (!verifyDatabaseIntegrity(tempDbFile)) {
                zipFile.close()
                tempDbFile.delete()
                zipFileToRestore.delete()
                return false
            }

            val manifestEntry = zipFile.getEntry("manifest.json")
                ?: zipFile.getEntry("manifest.JSON")
            val manifest: BackupManifest? = if (manifestEntry != null) {
                runCatching {
                    zipFile.getInputStream(manifestEntry).bufferedReader(Charsets.UTF_8).use { reader ->
                        json.decodeFromString<BackupManifest>(reader.readText())
                    }
                }.getOrNull()
            } else null

            val attachmentsDir = File(context.filesDir, "attachments")
            attachmentsDir.mkdirs()

            val vesselsDir = File(context.filesDir, "vessels")
            vesselsDir.mkdirs()

            val extractedAttachments = mutableListOf<Pair<Long, File>>()
            val extractedVesselPhotos = mutableListOf<Pair<Long, File>>()

            if (manifest != null && manifest.attachments.isNotEmpty()) {
                for (attMeta in manifest.attachments) {
                    val entry = zipFile.getEntry(attMeta.archivePath)
                        ?: zipFile.getEntry(attMeta.archivePath.replace('\\', '/'))
                        ?: zipFile.getEntry(attMeta.archivePath.replace('/', '\\'))
                        ?: continue
                    val targetFile = File(attachmentsDir, "${attMeta.id}.bin")
                    zipFile.getInputStream(entry).use { inStream ->
                        targetFile.outputStream().use { outStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    extractedAttachments.add(attMeta.id to targetFile)
                }
            } else {
                val entries = zipFile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val normalizedName = entry.name.replace('\\', '/')
                    if (normalizedName.startsWith("attachments/") && !entry.isDirectory) {
                        val fileName = File(entry.name).name
                        val idPart = fileName.substringBefore(".")
                        val attId = idPart.toLongOrNull()
                        val targetFile = File(attachmentsDir, fileName)
                        zipFile.getInputStream(entry).use { inStream ->
                            targetFile.outputStream().use { outStream ->
                                inStream.copyTo(outStream)
                            }
                        }
                        if (attId != null) {
                            extractedAttachments.add(attId to targetFile)
                        }
                    }
                }
            }

            if (manifest != null && manifest.vesselPhotos.isNotEmpty()) {
                for (vesselMeta in manifest.vesselPhotos) {
                    val entry = zipFile.getEntry(vesselMeta.archivePath)
                        ?: zipFile.getEntry(vesselMeta.archivePath.replace('\\', '/'))
                        ?: zipFile.getEntry(vesselMeta.archivePath.replace('/', '\\'))
                        ?: continue
                    val targetFile = File(vesselsDir, "${vesselMeta.vesselId}.bin")
                    zipFile.getInputStream(entry).use { inStream ->
                        targetFile.outputStream().use { outStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    extractedVesselPhotos.add(vesselMeta.vesselId to targetFile)
                }
            } else {
                val entries = zipFile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val normalizedName = entry.name.replace('\\', '/')
                    if (normalizedName.startsWith("vessels/") && !entry.isDirectory) {
                        val fileName = File(entry.name).name
                        val idPart = fileName.substringBefore(".")
                        val vId = idPart.toLongOrNull()
                        val targetFile = File(vesselsDir, fileName)
                        zipFile.getInputStream(entry).use { inStream ->
                            targetFile.outputStream().use { outStream ->
                                inStream.copyTo(outStream)
                            }
                        }
                        if (vId != null) {
                            extractedVesselPhotos.add(vId to targetFile)
                        }
                    }
                }
            }

            zipFile.close()

            // Update photo & attachment file URIs in tempDbFile, then checkpoint and convert to journal_mode = DELETE
            val writeDb = SQLiteDatabase.openDatabase(
                tempDbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE,
            )
            try {
                for ((attId, localFile) in extractedAttachments) {
                    val uriString = Uri.fromFile(localFile).toString()
                    writeDb.execSQL(
                        "UPDATE attachment SET fileUri = ? WHERE id = ?",
                        arrayOf(uriString, attId.toString())
                    )
                }
                for ((vesselId, localFile) in extractedVesselPhotos) {
                    val uriString = Uri.fromFile(localFile).toString()
                    writeDb.execSQL(
                        "UPDATE vessel SET photoUri = ? WHERE id = ?",
                        arrayOf(uriString, vesselId.toString())
                    )
                }
                writeDb.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
                writeDb.execSQL("PRAGMA journal_mode = DELETE")
            } finally {
                writeDb.close()
            }
            File(tempDbFile.path + "-wal").delete()
            File(tempDbFile.path + "-shm").delete()

            // Close existing Room database cleanly before overwriting database files
            (context.applicationContext as? HullBeatApp)?.closeAndResetDatabase()

            val targetDb = context.getDatabasePath(AppDatabase.NAME)
            targetDb.parentFile?.mkdirs()
            File(targetDb.path + "-wal").delete()
            File(targetDb.path + "-shm").delete()
            tempDbFile.copyTo(targetDb, overwrite = true)
            tempDbFile.delete()
            File(targetDb.path + "-wal").delete()
            File(targetDb.path + "-shm").delete()
            zipFileToRestore.delete()

            // Clear selected vessel so app selects restored vessel
            AppPreferences.setSelectedVesselId(context, null)

            return true
        } catch (_: Exception) {
            tempDbFile.delete()
            File(tempDbFile.path + "-wal").delete()
            File(tempDbFile.path + "-shm").delete()
            zipFileToRestore.delete()
            return false
        }
    }

    fun verifyDatabaseIntegrity(dbFile: File): Boolean {
        if (!dbFile.exists() || dbFile.length() < 100) return false

        val header = ByteArray(16)
        return try {
            dbFile.inputStream().use { it.read(header) }
            if (!isSqliteHeader(header)) return false

            var isIntegrityOk = false
            val testDb = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
            val cursor = testDb.rawQuery("PRAGMA quick_check", null)
            if (cursor.moveToFirst()) {
                val result = cursor.getString(0)
                isIntegrityOk = result.equals("ok", ignoreCase = true)
            }
            cursor.close()
            testDb.close()
            isIntegrityOk
        } catch (_: Exception) {
            false
        }
    }

    private fun openUriStream(context: Context, uriString: String): InputStream? {
        return runCatching {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)
        }.getOrNull() ?: runCatching {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file" || uri.scheme == null) {
                val path = uri.path ?: uriString
                val file = File(path)
                if (file.exists() && file.isFile) file.inputStream() else null
            } else {
                null
            }
        }.getOrNull()
    }
}