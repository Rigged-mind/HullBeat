package app.hullbeat.domain.backup

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.ByteArray
import kotlin.text.Charsets

class BackupManagerTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun testZipHeaderDetection() {
        val zipHeader = byteArrayOf(0x50.toByte(), 0x4B.toByte(), 0x03.toByte(), 0x04.toByte(), 0x00, 0x00)
        assertTrue(BackupManager.isZipHeader(zipHeader))

        val fakeHeader = byteArrayOf(0x00, 0x00, 0x00, 0x00)
        assertFalse(BackupManager.isZipHeader(fakeHeader))

        val shortHeader = byteArrayOf(0x50.toByte(), 0x4B.toByte())
        assertFalse(BackupManager.isZipHeader(shortHeader))

        assertFalse(BackupManager.isZipHeader(BackupManager.SQLITE_HEADER))
    }

    @Test
    fun testSqliteHeaderDetection() {
        assertTrue(BackupManager.isSqliteHeader(BackupManager.SQLITE_HEADER))

        val paddedSqliteHeader = BackupManager.SQLITE_HEADER + byteArrayOf(0x01, 0x02, 0x03)
        assertTrue(BackupManager.isSqliteHeader(paddedSqliteHeader))

        val zipHeader = byteArrayOf(0x50.toByte(), 0x4B.toByte(), 0x03.toByte(), 0x04.toByte(), 0x00, 0x00)
        assertFalse(BackupManager.isSqliteHeader(zipHeader))

        val shortHeader = "SQLite".toByteArray(Charsets.US_ASCII)
        assertFalse(BackupManager.isSqliteHeader(shortHeader))
    }

    @Test
    fun testManifestSerialization() {
        val manifest = BackupManifest(
            version = 1,
            appName = "HullBeat",
            exportedAt = 1726000000000L,
            attachments = listOf(
                AttachmentEntry(id = 101L, archivePath = "attachments/101.bin"),
                AttachmentEntry(id = 102L, archivePath = "attachments/102.bin"),
            ),
            vesselPhotos = listOf(
                VesselPhotoEntry(vesselId = 1L, archivePath = "vessels/1.bin"),
            ),
        )

        val jsonString = json.encodeToString(manifest)
        val decoded = json.decodeFromString<BackupManifest>(jsonString)

        assertEquals(1, decoded.version)
        assertEquals("HullBeat", decoded.appName)
        assertEquals(1726000000000L, decoded.exportedAt)
        assertEquals(2, decoded.attachments.size)
        assertEquals(101L, decoded.attachments[0].id)
        assertEquals("attachments/101.bin", decoded.attachments[0].archivePath)
        assertEquals(1, decoded.vesselPhotos.size)
        assertEquals(1L, decoded.vesselPhotos[0].vesselId)
        assertEquals("vessels/1.bin", decoded.vesselPhotos[0].archivePath)
    }

    @Test
    fun testZipPackagingAndStreamReading() {
        val outStream = ByteArrayOutputStream()
        val zipOut = ZipOutputStream(outStream)

        // Add dummy DB
        zipOut.putNextEntry(ZipEntry("database/hullbeat.db"))
        zipOut.write("dummy db content".toByteArray(Charsets.UTF_8))
        zipOut.closeEntry()

        // Add dummy attachment
        zipOut.putNextEntry(ZipEntry("attachments/42.bin"))
        zipOut.write("dummy image bytes".toByteArray(Charsets.UTF_8))
        zipOut.closeEntry()

        // Add manifest
        val manifest = BackupManifest(
            version = 1,
            appName = "HullBeat",
            exportedAt = 12345L,
            attachments = listOf(AttachmentEntry(42L, "attachments/42.bin")),
        )
        val manifestBytes = json.encodeToString(manifest).toByteArray(Charsets.UTF_8)
        zipOut.putNextEntry(ZipEntry("manifest.json"))
        zipOut.write(manifestBytes)
        zipOut.closeEntry()

        zipOut.finish()
        zipOut.flush()

        val zipBytes = outStream.toByteArray()
        assertTrue(BackupManager.isZipHeader(zipBytes))

        // Verify entries can be read back sequentially
        val zipIn = ZipInputStream(ByteArrayInputStream(zipBytes))
        val entryNames = mutableListOf<String>()
        var entry = zipIn.nextEntry
        while (entry != null) {
            entryNames.add(entry.name)
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }
        zipIn.close()

        assertTrue(entryNames.contains("database/hullbeat.db"))
        assertTrue(entryNames.contains("attachments/42.bin"))
        assertTrue(entryNames.contains("manifest.json"))
    }
}
