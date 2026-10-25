package app.hullbeat.domain.backup

import app.hullbeat.data.db.AppDatabase
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A restore replaces the owner's whole database, so it must take only one of
 * ours. Each case below builds the database the way the device would hold it
 * and reads back the same two values the restore reads.
 */
class BackupSchemaTest {

    private val schemaDir: File =
        listOf("schemas", "app/schemas").map { File(it, "app.hullbeat.data.db.AppDatabase") }
            .first { it.isDirectory }

    private fun schema(version: Int): JsonObject =
        Json.parseToJsonElement(File(schemaDir, "$version.json").readText())
            .jsonObject.getValue("database").jsonObject

    private val versions: List<Int> =
        schemaDir.listFiles()!!.mapNotNull { it.name.removeSuffix(".json").toIntOrNull() }.sorted()

    /** A HullBeat database at [version], as Room creates it: tables, setup queries, user_version. */
    private fun hullbeat(version: Int): Connection {
        val conn = DriverManager.getConnection("jdbc:sqlite::memory:")
        val db = schema(version)
        conn.createStatement().use { st ->
            for (entity in db.getValue("entities").jsonArray.map { it.jsonObject }) {
                val table = entity.getValue("tableName").jsonPrimitive.content
                st.execute(entity.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            }
            db.getValue("setupQueries").jsonArray.forEach { st.execute(it.jsonPrimitive.content) }
            st.execute("PRAGMA user_version = $version")
        }
        return conn
    }

    private fun verdictOf(conn: Connection): SchemaVerdict {
        val version = conn.createStatement().use { st ->
            st.executeQuery("PRAGMA user_version").use { it.next(); it.getInt(1) }
        }
        val hash = runCatching {
            conn.createStatement().use { st ->
                st.executeQuery("SELECT identity_hash FROM room_master_table WHERE id = 42")
                    .use { if (it.next()) it.getString(1) else null }
            }
        }.getOrNull()
        return BackupManager.schemaVerdict(version, hash)
    }

    @Test
    fun everyExportedSchemaHasItsHashInTheApp() {
        val exported = versions.associateWith {
            schema(it).getValue("identityHash").jsonPrimitive.content
        }
        assertEquals(exported, AppDatabase.IDENTITY_HASHES)
    }

    @Test
    fun aHullBeatDatabaseOfEveryVersionIsTaken() {
        for (v in versions) {
            hullbeat(v).use { assertEquals("version $v", SchemaVerdict.HULLBEAT, verdictOf(it)) }
        }
    }

    @Test
    fun anotherAppsDatabaseIsRefused() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { conn ->
            conn.createStatement().use { st ->
                st.execute("CREATE TABLE notes (id INTEGER PRIMARY KEY, body TEXT)")
            }
            assertEquals(SchemaVerdict.FOREIGN, verdictOf(conn))
            // Its own user_version happening to match ours changes nothing.
            conn.createStatement().use { it.execute("PRAGMA user_version = ${versions.last()}") }
            assertEquals(SchemaVerdict.FOREIGN, verdictOf(conn))
        }
    }

    @Test
    fun anotherRoomDatabaseIsRefused() {
        hullbeat(versions.last()).use { conn ->
            conn.createStatement().use {
                it.execute("UPDATE room_master_table SET identity_hash = '0123456789abcdef0123456789abcdef'")
            }
            assertEquals(SchemaVerdict.FOREIGN, verdictOf(conn))
        }
    }

    @Test
    fun aDatabaseFromANewerBuildIsToldApart() {
        hullbeat(versions.last()).use { conn ->
            conn.createStatement().use { it.execute("PRAGMA user_version = ${versions.last() + 1}") }
            assertEquals(SchemaVerdict.NEWER_VERSION, verdictOf(conn))
        }
    }
}
