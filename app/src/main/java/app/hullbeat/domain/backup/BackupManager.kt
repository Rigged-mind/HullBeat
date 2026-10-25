package app.hullbeat.domain.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import app.hullbeat.HullBeatApp
import app.hullbeat.data.db.AppDatabase
import app.hullbeat.data.db.Attachment
import app.hullbeat.data.db.Vessel
import app.hullbeat.data.preferences.AppPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
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

/**
 * Result of a backup operation.
 *
 * [success] is false when the checkpoint did not flush the WAL or when an
 * unrecoverable IO error occurred. [skippedFiles] counts attachments and vessel
 * photos whose URIs existed in the database but could not be read at backup
 * time — the database entry is still present, but the file itself is gone.
 * A positive count means the archive looks complete from the outside but will
 * have broken attachment links after a restore.
 */
data class BackupResult(val success: Boolean, val skippedFiles: Int = 0)

/**
 * What a restore came to. Anything but [OK] leaves the working database and
 * its photographs as they were. [NEWER_VERSION] is told apart from
 * [INVALID] because the owner can fix it: by updating the app.
 */
enum class RestoreResult { OK, INVALID, NEWER_VERSION, NO_SPACE }

/**
 * Where a restore puts the database and photographs, and how it has Room
 * check the result. The app restores into [forApp]; an instrumented test
 * passes its own, so running it never touches the owner's data.
 */
class RestoreTarget(
    val databaseFile: File,
    /** Holds the attachments/ and vessels/ folders. */
    val mediaRoot: File,
    val scratchDir: File,
    /** Closes any open connection before the file under it is replaced. */
    val closeDatabase: () -> Unit,
    /** Opens [databaseFile] through Room and queries it; throws if Room refuses it. */
    val openAndCheck: () -> Unit,
    /** Runs once a restore has gone through. */
    val onRestored: () -> Unit,
) {
    companion object {
        fun forApp(context: Context): RestoreTarget {
            val app = context.applicationContext as HullBeatApp
            return RestoreTarget(
                databaseFile = context.getDatabasePath(AppDatabase.NAME),
                mediaRoot = context.filesDir,
                scratchDir = context.cacheDir,
                closeDatabase = { app.closeAndResetDatabase() },
                openAndCheck = {
                    app.database.openHelper.writableDatabase
                        .query("SELECT COUNT(*) FROM vessel").use { it.moveToFirst() }
                },
                // So the app picks a boat from the restored data.
                onRestored = { AppPreferences.setSelectedVesselId(context, null) },
            )
        }
    }
}

/** What the database inside a backup turned out to be. */
internal enum class SchemaVerdict { HULLBEAT, NEWER_VERSION, FOREIGN }

object BackupManager {

    private const val TAG = "BackupManager"

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

    suspend fun createBackup(context: Context, db: AppDatabase, outputStream: OutputStream): BackupResult {
        return try {
            // Only the main .db file goes into the archive, so everything still
            // sitting in the WAL must be folded into it first. A checkpoint
            // that could not finish does not throw: it returns busy = 1, and
            // the copy made after it would silently miss the newest rows.
            // Better no backup than one that looks complete and is not.
            val checkpointed = db.openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(FULL)")
                .use { it.moveToFirst() && it.getInt(0) == 0 }
            if (!checkpointed) return BackupResult(success = false)

            val dbFile = context.getDatabasePath(db.openHelper.databaseName ?: AppDatabase.NAME)
            if (!dbFile.exists()) return BackupResult(success = false)

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
            var skippedFiles = 0

            // 1. Pack database file
            zipOut.putNextEntry(ZipEntry("database/hullbeat.db"))
            dbFile.inputStream().use { it.copyTo(zipOut) }
            zipOut.closeEntry()

            // 2. Pack attachments
            val attachmentEntries = mutableListOf<AttachmentEntry>()
            for (att in attachments) {
                if (att.fileUri.isBlank()) continue
                val archivePath = "attachments/${att.id}.bin"
                val stream = openUriStream(context, att.fileUri)
                if (stream == null) {
                    // URI in the DB but file unreachable — archive will be incomplete.
                    skippedFiles++
                    continue
                }
                try {
                    zipOut.putNextEntry(ZipEntry(archivePath))
                    stream.use { it.copyTo(zipOut) }
                    zipOut.closeEntry()
                    attachmentEntries.add(AttachmentEntry(id = att.id, archivePath = archivePath))
                } catch (_: Exception) {
                    skippedFiles++
                }
            }

            // 3. Pack vessel photos
            val vesselEntries = mutableListOf<VesselPhotoEntry>()
            for (vessel in vessels) {
                val photoUri = vessel.photoUri
                if (photoUri.isNullOrBlank()) continue
                val archivePath = "vessels/${vessel.id}.bin"
                val stream = openUriStream(context, photoUri)
                if (stream == null) {
                    skippedFiles++
                    continue
                }
                try {
                    zipOut.putNextEntry(ZipEntry(archivePath))
                    stream.use { it.copyTo(zipOut) }
                    zipOut.closeEntry()
                    vesselEntries.add(VesselPhotoEntry(vesselId = vessel.id, archivePath = archivePath))
                } catch (_: Exception) {
                    skippedFiles++
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
            BackupResult(success = true, skippedFiles = skippedFiles)
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            BackupResult(success = false)
        }
    }

    fun restoreBackup(context: Context, inputStream: InputStream): RestoreResult =
        restoreBackup(inputStream, RestoreTarget.forApp(context))

    fun restoreBackup(inputStream: InputStream, target: RestoreTarget): RestoreResult {
        target.scratchDir.mkdirs()
        val tempInputFile = File(target.scratchDir, "restore_temp_input")
        try {
            val budget = SpaceBudget(target.scratchDir)
            inputStream.use { input ->
                tempInputFile.outputStream().use { output ->
                    input.copyWithin(output, budget)
                    output.flush()
                }
            }

            if (tempInputFile.length() < 16) {
                tempInputFile.delete()
                return RestoreResult.INVALID
            }

            val header = ByteArray(16)
            tempInputFile.inputStream().use { it.read(header) }

            return when {
                isZipHeader(header) -> restoreZipBackup(target, tempInputFile)
                isSqliteHeader(header) -> restoreSqliteBackup(target, tempInputFile)
                else -> {
                    Log.w(TAG, "Restore: neither a zip nor an SQLite file")
                    tempInputFile.delete()
                    RestoreResult.INVALID
                }
            }
        } catch (e: NoSpaceException) {
            Log.w(TAG, "Restore: not enough space to read the file", e)
            tempInputFile.delete()
            return RestoreResult.NO_SPACE
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed while reading the file", e)
            tempInputFile.delete()
            return RestoreResult.INVALID
        }
    }

    private fun restoreSqliteBackup(target: RestoreTarget, tempDbFile: File): RestoreResult {
        try {
            if (!verifyDatabaseIntegrity(tempDbFile)) return RestoreResult.INVALID
            readSchemaVerdict(tempDbFile).refusal()?.let { return it }

            // Checkpoint any pending WAL frames and convert journal mode to DELETE
            val checkpointDb = SQLiteDatabase.openDatabase(
                tempDbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE,
            )
            try {
                checkpointDb.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
                checkpointDb.rawQuery("PRAGMA journal_mode = DELETE", null).use { it.moveToFirst() }
            } catch (_: Exception) {
            } finally {
                checkpointDb.close()
            }
            File(tempDbFile.path + "-wal").delete()
            File(tempDbFile.path + "-shm").delete()

            if (!swapInDatabase(target, tempDbFile)) return RestoreResult.INVALID
            pruneUnreferencedMedia(target)

            target.onRestored()
            return RestoreResult.OK
        } catch (e: Exception) {
            Log.e(TAG, "Restore of an SQLite file failed", e)
            return RestoreResult.INVALID
        } finally {
            DB_FILE_SUFFIXES.forEach { File(tempDbFile.path + it).delete() }
        }
    }

    private fun restoreZipBackup(target: RestoreTarget, zipFileToRestore: File): RestoreResult {
        val tempDbFile = File(target.scratchDir, "restore_temp.db")
        // Photographs are unpacked into the live folders, but under names no
        // file there has yet: nothing the working data uses is overwritten
        // before the restore is known to have worked, and if it has not, they
        // are deleted again.
        val extracted = mutableListOf<File>()
        var result = RestoreResult.INVALID
        try {
            result = ZipFile(zipFileToRestore).use { zipFile ->
                restoreFromZip(target, zipFile, tempDbFile, extracted)
            }
        } catch (e: NoSpaceException) {
            Log.w(TAG, "Restore: not enough space to unpack the backup", e)
            result = RestoreResult.NO_SPACE
        } catch (e: Exception) {
            Log.e(TAG, "Restore of a zip backup failed", e)
        } finally {
            DB_FILE_SUFFIXES.forEach { File(tempDbFile.path + it).delete() }
            zipFileToRestore.delete()
            if (result != RestoreResult.OK) extracted.forEach { it.delete() }
        }
        return result
    }

    private fun restoreFromZip(
        target: RestoreTarget,
        zipFile: ZipFile,
        tempDbFile: File,
        extracted: MutableList<File>,
    ): RestoreResult {
        if (zipFile.size() > MAX_ZIP_ENTRIES) return RestoreResult.INVALID
        val dbEntry = zipFile.getEntry("database/hullbeat.db")
            ?: zipFile.getEntry("database\\hullbeat.db")
            ?: zipFile.entries().asSequence().firstOrNull {
                it.name.endsWith(".db", ignoreCase = true) && !it.isDirectory
            }
            ?: return RestoreResult.INVALID

        val budget = SpaceBudget(target.mediaRoot)
        zipFile.getInputStream(dbEntry).use { inStream ->
            tempDbFile.outputStream().use { outStream ->
                inStream.copyWithin(outStream, budget)
                outStream.flush()
            }
        }

        if (!verifyDatabaseIntegrity(tempDbFile)) return RestoreResult.INVALID
        readSchemaVerdict(tempDbFile).refusal()?.let { return it }

        val manifestEntry = zipFile.getEntry("manifest.json")
            ?: zipFile.getEntry("manifest.JSON")
        // Read whole before parsing, so a bounded read, not readText(): an
        // archive the owner picked can hold a manifest of any size.
        val manifestBytes = manifestEntry?.let { entry ->
            zipFile.getInputStream(entry).use { readAtMost(it, MAX_MANIFEST_BYTES) }
                ?: run {
                    Log.w(TAG, "Restore: refused, manifest.json over $MAX_MANIFEST_BYTES bytes")
                    return RestoreResult.INVALID
                }
        }
        val manifest: BackupManifest? = manifestBytes?.let { bytes ->
            runCatching {
                json.decodeFromString<BackupManifest>(bytes.toString(Charsets.UTF_8))
            }.getOrNull()
        }

        val attachmentsDir = File(target.mediaRoot, "attachments")
        attachmentsDir.mkdirs()

        val vesselsDir = File(target.mediaRoot, "vessels")
        vesselsDir.mkdirs()

        val prefix = "r${System.currentTimeMillis()}_"
        fun unpack(entry: ZipEntry, dir: File, name: String): File {
            val targetFile = File(dir, prefix + name)
            extracted += targetFile
            zipFile.getInputStream(entry).use { inStream ->
                targetFile.outputStream().use { outStream ->
                    inStream.copyWithin(outStream, budget)
                }
            }
            return targetFile
        }

        val extractedAttachments = mutableListOf<Pair<Long, File>>()
        val extractedVesselPhotos = mutableListOf<Pair<Long, File>>()

        if (manifest != null && manifest.attachments.isNotEmpty()) {
            for (attMeta in manifest.attachments) {
                val entry = zipFile.getEntry(attMeta.archivePath)
                    ?: zipFile.getEntry(attMeta.archivePath.replace('\\', '/'))
                    ?: zipFile.getEntry(attMeta.archivePath.replace('/', '\\'))
                    ?: continue
                extractedAttachments.add(attMeta.id to unpack(entry, attachmentsDir, "${attMeta.id}.bin"))
            }
        } else {
            for (entry in zipFile.entries()) {
                val normalizedName = entry.name.replace('\\', '/')
                if (normalizedName.startsWith("attachments/") && !entry.isDirectory) {
                    val fileName = File(entry.name).name
                    val attId = fileName.substringBefore(".").toLongOrNull()
                    val targetFile = unpack(entry, attachmentsDir, fileName)
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
                extractedVesselPhotos.add(vesselMeta.vesselId to unpack(entry, vesselsDir, "${vesselMeta.vesselId}.bin"))
            }
        } else {
            for (entry in zipFile.entries()) {
                val normalizedName = entry.name.replace('\\', '/')
                if (normalizedName.startsWith("vessels/") && !entry.isDirectory) {
                    val fileName = File(entry.name).name
                    val vId = fileName.substringBefore(".").toLongOrNull()
                    val targetFile = unpack(entry, vesselsDir, fileName)
                    if (vId != null) {
                        extractedVesselPhotos.add(vId to targetFile)
                    }
                }
            }
        }

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
            // rawQuery, not execSQL: this PRAGMA answers with a row, and
            // Android's execSQL throws on any statement that returns one.
            // It did, here, outside any try - so every .zip restore failed.
            writeDb.rawQuery("PRAGMA journal_mode = DELETE", null).use { it.moveToFirst() }
        } finally {
            writeDb.close()
        }
        File(tempDbFile.path + "-wal").delete()
        File(tempDbFile.path + "-shm").delete()

        if (!swapInDatabase(target, tempDbFile)) return RestoreResult.INVALID
        pruneUnreferencedMedia(target)

        target.onRestored()

        return RestoreResult.OK
    }

    /** The main database file and the two SQLite keeps beside it. */
    private val DB_FILE_SUFFIXES = listOf("", "-wal", "-shm")

    /**
     * Puts [restoredDb] where the working database is and has Room open it,
     * which runs the migrations an older backup needs and checks the result
     * against the schema this build expects. The working database is set
     * aside first, not overwritten, and goes back if Room refuses: a backup
     * that passed every check before this can still fail here, and that must
     * cost the owner nothing.
     */
    private fun swapInDatabase(target: RestoreTarget, restoredDb: File): Boolean {
        val dbFile = target.databaseFile
        dbFile.parentFile?.mkdirs()
        val live = DB_FILE_SUFFIXES.map { File(dbFile.path + it) }
        val setAside = live.map { File(it.path + ".pre-restore") }

        target.closeDatabase()
        setAside.forEach { it.delete() }
        val moved = mutableListOf<Int>()
        for (i in live.indices) {
            if (!live[i].exists()) continue
            if (!live[i].renameTo(setAside[i])) {
                Log.e(TAG, "Restore: could not set ${live[i].name} aside, nothing replaced")
                moved.forEach { setAside[it].renameTo(live[it]) }
                return false
            }
            moved += i
        }

        return try {
            if (!restoredDb.renameTo(dbFile)) restoredDb.copyTo(dbFile, overwrite = true)
            // A query, not just an open: Room migrates and validates on the
            // first use of the connection, and that is the check that counts.
            target.openAndCheck()
            setAside.forEach { it.delete() }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Restore: Room refused the restored database, the previous one is back", e)
            target.closeDatabase()
            live.forEach { it.delete() }
            moved.forEach { setAside[it].renameTo(live[it]) }
            false
        }
    }

    /**
     * The folders the app copies attachments and vessel photos into. Nothing
     * else writes there, and only `attachment.fileUri` and `vessel.photoUri`
     * point into them.
     */
    private val MEDIA_DIRS = listOf("attachments", "vessels")

    /**
     * After a restore, deletes the files in [MEDIA_DIRS] that the restored
     * database no longer points to: the previous data's photographs, which
     * nothing can open any more and which otherwise pile up with every
     * restore. If the references cannot be read, nothing is deleted - a
     * cleanup must never cost a photograph that is still in use.
     */
    private fun pruneUnreferencedMedia(target: RestoreTarget) {
        val uris = try {
            val db = SQLiteDatabase.openDatabase(target.databaseFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            try {
                val list = mutableListOf<String?>()
                db.rawQuery("SELECT fileUri FROM attachment", null).use { c -> while (c.moveToNext()) list += c.getString(0) }
                db.rawQuery("SELECT photoUri FROM vessel", null).use { c -> while (c.moveToNext()) list += c.getString(0) }
                list
            } finally {
                db.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Restore: references unreadable, media left as it was", e)
            return
        }
        val referenced = referencedPaths(uris)
        var deleted = 0
        for (dir in MEDIA_DIRS) {
            val files = File(target.mediaRoot, dir).listFiles()?.filter { it.isFile } ?: continue
            for (file in filesToPrune(files, referenced)) {
                if (file.delete()) deleted++
            }
        }
        if (deleted > 0) Log.i(TAG, "Restore: removed $deleted media file(s) the restored data does not use")
    }

    /**
     * Canonical paths of the local files among [uris]; content:// and blanks
     * are not ours. A uri that does not parse is read as a bare path rather
     * than dropped: dropping it would delete a file that is still in use.
     */
    internal fun referencedPaths(uris: List<String?>): Set<String> =
        uris.mapNotNull { uri ->
            if (uri.isNullOrBlank()) return@mapNotNull null
            val path = runCatching {
                val parsed = java.net.URI(uri)
                when (parsed.scheme) {
                    "file" -> parsed.path
                    null -> uri
                    else -> return@mapNotNull null
                }
            }.getOrElse { uri.removePrefix("file://") }
            runCatching { File(path).canonicalPath }.getOrNull()
        }.toSet()

    internal fun filesToPrune(files: List<File>, referenced: Set<String>): List<File> =
        files.filter { runCatching { it.canonicalPath }.getOrNull()?.let { p -> p !in referenced } ?: false }

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
            if (!isIntegrityOk) Log.w(TAG, "Restore: quick_check did not return ok")
            isIntegrityOk
        } catch (e: Exception) {
            Log.e(TAG, "Restore: the database in the backup could not be opened", e)
            false
        }
    }

    /**
     * Whether a database is one of ours, judged by the two things Room itself
     * writes: `PRAGMA user_version` and the identity hash in
     * `room_master_table`. quick_check only says the file is sound SQLite -
     * any app's database passes it, and Room, handed one, either fails on
     * every start or takes version 0 for a fresh install and opens it empty.
     */
    internal fun schemaVerdict(userVersion: Int, identityHash: String?): SchemaVerdict = when {
        identityHash == null -> SchemaVerdict.FOREIGN
        userVersion > AppDatabase.IDENTITY_HASHES.keys.max() -> SchemaVerdict.NEWER_VERSION
        AppDatabase.IDENTITY_HASHES[userVersion] == identityHash -> SchemaVerdict.HULLBEAT
        else -> SchemaVerdict.FOREIGN
    }

    private fun readSchemaVerdict(dbFile: File): SchemaVerdict = try {
        SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            val hash = runCatching {
                db.rawQuery("SELECT identity_hash FROM room_master_table WHERE id = 42", null)
                    .use { c -> if (c.moveToFirst()) c.getString(0) else null }
            }.getOrNull()
            schemaVerdict(db.version, hash).also {
                if (it != SchemaVerdict.HULLBEAT) Log.w(TAG, "Restore: refused, version ${db.version}, $it")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Restore: the database in the backup could not be read", e)
        SchemaVerdict.FOREIGN
    }

    private fun SchemaVerdict.refusal(): RestoreResult? = when (this) {
        SchemaVerdict.HULLBEAT -> null
        SchemaVerdict.NEWER_VERSION -> RestoreResult.NEWER_VERSION
        SchemaVerdict.FOREIGN -> RestoreResult.INVALID
    }

    /** Left free on the device by a restore, so it never fills the phone. */
    private const val RESTORE_FREE_SPACE_RESERVE = 64L * 1024 * 1024

    /** Far more entries than any real backup has; an archive with more is not ours. */
    private const val MAX_ZIP_ENTRIES = 50_000

    /**
     * Twice what a manifest listing [MAX_ZIP_ENTRIES] files takes (about 92
     * bytes each); a bigger one is not ours.
     */
    private const val MAX_MANIFEST_BYTES = 8L * 1024 * 1024

    /** All of [input], or null if it holds more than [limit] bytes. */
    internal fun readAtMost(input: InputStream, limit: Long): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            total += n
            if (total > limit) return null
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    private class NoSpaceException : IOException("not enough space to restore")

    /**
     * Bytes a restore may still write. Counted while copying, not taken from
     * the sizes the archive states about itself, which can be false.
     */
    private class SpaceBudget(dir: File) {
        private var remaining = dir.usableSpace - RESTORE_FREE_SPACE_RESERVE

        fun take(bytes: Int) {
            remaining -= bytes
            if (remaining < 0) throw NoSpaceException()
        }
    }

    private fun InputStream.copyWithin(out: OutputStream, budget: SpaceBudget) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val n = read(buffer)
            if (n < 0) break
            budget.take(n)
            out.write(buffer, 0, n)
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