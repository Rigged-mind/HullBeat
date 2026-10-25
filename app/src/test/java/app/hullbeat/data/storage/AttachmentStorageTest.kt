package app.hullbeat.data.storage

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentStorageTest {

    private val root: File = Files.createTempDirectory("files").toFile()
    private val attachments = File(root, "attachments").apply { mkdirs() }

    private fun file(dir: File, name: String) = File(dir, name).apply {
        parentFile?.mkdirs()
        writeText("x")
    }

    @Test
    fun aFileInTheFolderIsInside() {
        assertTrue(AttachmentStorage.isFileInside(file(attachments, "1.bin"), attachments))
        assertTrue(AttachmentStorage.isFileInside(file(File(attachments, "sub"), "2.bin"), attachments))
    }

    @Test
    fun aSiblingFolderThatSharesTheNameIsNot() {
        val sibling = file(File(root, "attachments_old"), "1.bin")
        assertFalse(AttachmentStorage.isFileInside(sibling, attachments))
    }

    @Test
    fun climbingOutWithDotsIsNot() {
        val outside = file(root, "prefs.xml")
        assertFalse(AttachmentStorage.isFileInside(File(attachments, "../prefs.xml"), attachments))
        assertTrue(outside.exists())
    }

    @Test
    fun theFolderItselfAndMissingFilesAreNot() {
        assertFalse(AttachmentStorage.isFileInside(attachments, attachments))
        assertFalse(AttachmentStorage.isFileInside(File(attachments, "gone.bin"), attachments))
    }
}
