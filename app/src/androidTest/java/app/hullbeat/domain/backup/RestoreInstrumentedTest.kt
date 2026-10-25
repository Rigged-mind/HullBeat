package app.hullbeat.domain.backup

import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.hullbeat.HullBeatApp
import app.hullbeat.data.db.AppDatabase
import app.hullbeat.data.db.Attachment
import app.hullbeat.data.db.AttachmentKind
import app.hullbeat.data.db.ParentType
import app.hullbeat.data.db.Vessel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The whole restore on a real device: Android's SQLite, Room's migrations
 * and its schema check, the swap and the rollback - what the JVM tests
 * cannot reach.
 *
 * Everything happens in a database and a folder of the test's own. The
 * owner's database and photographs are fingerprinted before each test and
 * checked after it: if a restore ever reached them, the test fails. Run it
 * with `tools/emu.sh itest`, not `connectedAndroidTest`, which uninstalls
 * the app - and the owner's data with it - when it finishes.
 */
@RunWith(AndroidJUnit4::class)
class RestoreInstrumentedTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val app = context.applicationContext as HullBeatApp

    @get:Rule
    val migrations = MigrationTestHelper(instrumentation, AppDatabase::class.java)

    private val root = File(context.filesDir, "restore_test")
    private val media = File(root, "media")
    private val attachments = File(media, "attachments")
    private val dbName = "restore_test.db"
    private val sourceName = "restore_test_source.db"
    private val oldName = "restore_test_v1.db"

    private var open: AppDatabase? = null
    private fun db(): AppDatabase = open ?: app.buildDatabase(dbName).also { open = it }

    private val target = RestoreTarget(
        databaseFile = context.getDatabasePath(dbName),
        mediaRoot = media,
        scratchDir = File(root, "scratch"),
        closeDatabase = { open?.close(); open = null },
        openAndCheck = {
            db().openHelper.writableDatabase.query("SELECT COUNT(*) FROM vessel").use { it.moveToFirst() }
        },
        onRestored = {},
    )

    private lateinit var ownerBefore: List<String>

    /** Size and time of the owner's database files, and the names of their photographs. */
    private fun ownerFingerprint(): List<String> {
        val db = context.getDatabasePath(AppDatabase.NAME)
        val files = listOf("", "-wal", "-shm").map { File(db.path + it) }
            .map { "${it.name}:${it.exists()}:${it.length()}:${it.lastModified()}" }
        val photos = listOf("attachments", "vessels").flatMap { dir ->
            File(context.filesDir, dir).listFiles()?.map { "$dir/${it.name}:${it.length()}" }.orEmpty()
        }.sorted()
        return files + photos
    }

    @Before
    fun setUp() {
        assertNotEquals(AppDatabase.NAME, dbName)
        ownerBefore = ownerFingerprint()
        cleanUp()
        attachments.mkdirs()
        // The data a restore must replace, or leave alone when it refuses.
        seed(dbName, "Current", File(attachments, "current.bin"), "photo-current")
        target.closeDatabase()
    }

    @After
    fun tearDown() {
        target.closeDatabase()
        cleanUp()
        assertEquals("the owner's data changed", ownerBefore, ownerFingerprint())
    }

    private fun cleanUp() {
        for (name in listOf(dbName, sourceName, oldName)) {
            context.deleteDatabase(name)
            val path = context.getDatabasePath(name).path
            // .lck is MigrationTestHelper's lock file, which deleteDatabase leaves.
            File("$path.lck").delete()
            listOf("", "-wal", "-shm").forEach { File(path + it + ".pre-restore").delete() }
        }
        root.deleteRecursively()
    }

    private fun seed(name: String, vesselName: String, photo: File, photoText: String) = runBlocking {
        val db = if (name == dbName) db() else app.buildDatabase(name)
        val vesselId = db.vesselDao().insert(
            Vessel(name = vesselName, hullType = "monohull", engine = "diesel", drive = "shaft")
        )
        photo.parentFile?.mkdirs()
        photo.writeText(photoText)
        db.attachmentDao().insert(
            Attachment(
                parentType = ParentType.VESSEL,
                parentId = vesselId,
                kind = AttachmentKind.PHOTO,
                fileUri = Uri.fromFile(photo).toString(),
            )
        )
        if (name != dbName) db.close()
    }

    /** A real backup, made by createBackup from a database holding one boat named "Restored". */
    private fun realBackup(): ByteArray = runBlocking {
        seed(sourceName, "Restored", File(root, "source/photo.jpg"), "photo-restored")
        val source = app.buildDatabase(sourceName)
        val out = ByteArrayOutputStream()
        try {
            assertTrue(BackupManager.createBackup(context, source, out).success)
        } finally {
            source.close()
        }
        out.toByteArray()
    }

    /** The database file inside a backup, rewritten by [edit]. */
    private fun databaseFrom(backup: ByteArray, edit: (SQLiteDatabase) -> Unit = {}): ByteArray {
        val file = File(root, "edit.db").apply { parentFile?.mkdirs(); delete() }
        java.util.zip.ZipInputStream(ByteArrayInputStream(backup)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == "database/hullbeat.db") file.outputStream().use { zip.copyTo(it) }
            }
        }
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use(edit)
        return file.readBytes().also { file.delete() }
    }

    private fun restore(bytes: ByteArray) = BackupManager.restoreBackup(ByteArrayInputStream(bytes), target)

    private fun vesselNames(): List<String> = runBlocking { db().vesselDao().allActive().map { it.name } }

    private fun assertUntouched() {
        assertEquals(listOf("Current"), vesselNames())
        assertEquals("photo-current", File(attachments, "current.bin").readText())
        assertEquals(listOf("current.bin"), attachments.list()!!.sorted())
        val setAside = File(target.databaseFile.path + ".pre-restore")
        assertFalse("left a set-aside copy behind", setAside.exists())
    }

    @Test
    fun aRealBackupReplacesTheDataAndItsPhotographs() {
        assertEquals(RestoreResult.OK, restore(realBackup()))

        assertEquals(listOf("Restored"), vesselNames())
        val photo = runBlocking { db().attachmentDao().allAttachments() }.single()
        val file = File(Uri.parse(photo.fileUri).path!!)
        assertEquals(attachments.canonicalPath, file.parentFile!!.canonicalPath)
        assertEquals("photo-restored", file.readText())
        assertFalse("the previous photograph was not pruned", File(attachments, "current.bin").exists())
    }

    @Test
    fun anotherAppsDatabaseIsRefused() {
        val foreign = File(root, "foreign.db").apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(foreign, null).use {
            it.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY, body TEXT)")
            it.version = AppDatabase.IDENTITY_HASHES.keys.max()
        }
        assertEquals(RestoreResult.INVALID, restore(foreign.readBytes()))
        assertUntouched()
    }

    @Test
    fun aDatabaseFromANewerBuildIsToldApart() {
        val newer = databaseFrom(realBackup()) { it.version = AppDatabase.IDENTITY_HASHES.keys.max() + 1 }
        assertEquals(RestoreResult.NEWER_VERSION, restore(newer))
        assertUntouched()
    }

    /**
     * Passes every check before the swap - a HullBeat hash for the version it
     * claims - and fails Room's own: a version-4 database calling itself 3,
     * so the 3 -> 4 migration adds a column that is already there.
     */
    @Test
    fun whenRoomRefusesThePreviousDataComesBack() {
        val broken = databaseFrom(realBackup()) {
            it.version = 3
            it.execSQL("UPDATE room_master_table SET identity_hash = '${AppDatabase.IDENTITY_HASHES.getValue(3)}'")
        }
        // As a zip with a photograph, so the unpacked file must go again too.
        val zip = ByteArrayOutputStream()
        ZipOutputStream(zip).use { out ->
            out.putNextEntry(ZipEntry("database/hullbeat.db")); out.write(broken); out.closeEntry()
            out.putNextEntry(ZipEntry("attachments/1.bin")); out.write("photo-broken".toByteArray()); out.closeEntry()
        }
        assertEquals(RestoreResult.INVALID, restore(zip.toByteArray()))
        assertUntouched()
    }

    @Test
    fun aVersionOneDatabaseIsMigratedOnRestore() {
        migrations.createDatabase(oldName, 1).use { old ->
            old.execSQL(
                """
                INSERT INTO vessel (id, name, hullType, engine, drive, cooling, enginesCount, rigType,
                                    keelType, extras, storage, water, archived)
                VALUES (1, 'From v1', 'monohull', 'diesel', 'shaft', 'raw_water', 1, 'sloop',
                        'fin', '', 'afloat_year_round', 'salt', 0)
                """.trimIndent()
            )
        }
        val bytes = context.getDatabasePath(oldName).readBytes()

        assertEquals(RestoreResult.OK, restore(bytes))
        assertEquals(listOf("From v1"), vesselNames())
        assertEquals(
            AppDatabase.IDENTITY_HASHES.keys.max(),
            db().openHelper.readableDatabase.version,
        )
    }
}
