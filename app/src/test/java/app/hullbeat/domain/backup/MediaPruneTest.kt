package app.hullbeat.domain.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Which media files a restore removes: only those the restored database no
 * longer points to, and never one it does, however its uri is written.
 */
class MediaPruneTest {

    private fun dir(): File = Files.createTempDirectory("media").toFile()

    @Test
    fun `files the restored data does not use are pruned, used ones kept`() {
        val d = dir()
        val used = File(d, "1.bin").apply { writeText("a") }
        val orphan = File(d, "img_old.jpg").apply { writeText("b") }
        val referenced = BackupManager.referencedPaths(listOf(used.toURI().toString(), null, ""))
        assertEquals(listOf(orphan), BackupManager.filesToPrune(listOf(used, orphan), referenced))
    }

    @Test
    fun `a bare path, an encoded space and an unparseable uri still count as used`() {
        val d = dir()
        val bare = File(d, "bare.jpg").apply { writeText("a") }
        val spaced = File(d, "with space.jpg").apply { writeText("b") }
        val odd = File(d, "odd file.jpg").apply { writeText("c") }
        val referenced = BackupManager.referencedPaths(
            listOf(
                bare.absolutePath,
                spaced.toURI().toString(),                         // file:/.../with%20space.jpg
                "file://" + odd.absolutePath.replace('\\', '/'),   // unencoded space: not a valid URI
            )
        )
        assertTrue(BackupManager.filesToPrune(listOf(bare, spaced, odd), referenced).isEmpty())
    }

    @Test
    fun `content uris are not ours and do not protect anything`() {
        val d = dir()
        val f = File(d, "1.bin").apply { writeText("a") }
        val referenced = BackupManager.referencedPaths(listOf("content://media/external/images/1"))
        assertTrue(referenced.isEmpty())
        assertEquals(listOf(f), BackupManager.filesToPrune(listOf(f), referenced))
    }
}
