package app.hullbeat.data.db

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
 * Every exported schema, migrated to the latest by the app's own SQL, must
 * come out as the latest schema: the same columns with the same types,
 * nullability and defaults, the same foreign keys and the same indices.
 * Those are what Room checks on open, so a gap here is a crash on the first
 * start after an update - or on restoring a backup an older build made,
 * which goes through the same chain.
 *
 * Only 3 -> 4 had a test before, and only for one table.
 */
class MigrationChainTest {

    private val schemaDir: File =
        listOf("schemas", "app/schemas").map { File(it, "app.hullbeat.data.db.AppDatabase") }
            .first { it.isDirectory }

    private val versions: List<Int> =
        schemaDir.listFiles()!!.mapNotNull { it.name.removeSuffix(".json").toIntOrNull() }.sorted()

    private val latest = versions.last()

    private fun schema(version: Int): JsonObject =
        Json.parseToJsonElement(File(schemaDir, "$version.json").readText())
            .jsonObject.getValue("database").jsonObject

    private fun open(version: Int): Connection {
        val conn = DriverManager.getConnection("jdbc:sqlite::memory:")
        val db = schema(version)
        conn.createStatement().use { st ->
            for (entity in db.getValue("entities").jsonArray.map { it.jsonObject }) {
                val table = entity.getValue("tableName").jsonPrimitive.content
                st.execute(entity.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                entity["indices"]?.jsonArray?.forEach { ix ->
                    st.execute(ix.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                }
            }
        }
        return conn
    }

    private fun migrate(conn: Connection, from: Int) {
        conn.createStatement().use { st ->
            for (v in from until latest) {
                AppDatabase.MIGRATION_STEPS_SQL.getValue(v).forEach(st::execute)
            }
        }
    }

    private fun Connection.rows(sql: String): List<List<String?>> =
        createStatement().use { st ->
            st.executeQuery(sql).use { rs ->
                val n = rs.metaData.columnCount
                buildList { while (rs.next()) add((1..n).map { rs.getString(it) }) }
            }
        }

    /** What Room compares on open, in an order-independent form. */
    private fun shape(conn: Connection): Map<String, List<Any>> {
        val tables = conn.rows(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'"
        ).map { it[0]!! }.sorted()
        return tables.associateWith { t ->
            // name, type, notnull, default
            val columns = conn.rows("PRAGMA table_info(`$t`)")
                .map { listOf(it[1], it[2]?.uppercase(), it[3], it[4]) }.sortedBy { it.toString() }
            // table, from, to, on update, on delete
            val keys = conn.rows("PRAGMA foreign_key_list(`$t`)")
                .map { listOf(it[2], it[3], it[4], it[5], it[6]) }.sortedBy { it.toString() }
            val indices = conn.rows("PRAGMA index_list(`$t`)")
                .filterNot { it[1]!!.startsWith("sqlite_autoindex") }
                .map { ix -> listOf(ix[1], ix[2]) + conn.rows("PRAGMA index_info(`${ix[1]}`)").map { it[2] } }
                .sortedBy { it.toString() }
            listOf(columns, keys, indices)
        }
    }

    @Test
    fun `there is a migration step for every version below the latest`() {
        assertEquals((versions.first() until latest).toList(), AppDatabase.MIGRATION_STEPS_SQL.keys.sorted())
    }

    @Test
    fun `every older schema migrates to exactly the latest one`() {
        val expected = open(latest).use { shape(it) }
        for (from in versions.dropLast(1)) {
            open(from).use { conn ->
                migrate(conn, from)
                assertEquals("migrating $from -> $latest", expected, shape(conn))
            }
        }
    }

    @Test
    fun `2 to 3 keeps one run per boat, checklist and day before the unique index`() {
        open(2).use { conn ->
            conn.createStatement().use { st ->
                st.execute("INSERT INTO checklist_run (vesselId, checklistCode, date, completedAt) VALUES (1, 'departure', '2026-06-01', 1)")
                st.execute("INSERT INTO checklist_run (vesselId, checklistCode, date, completedAt) VALUES (1, 'departure', '2026-06-01', 2)")
                st.execute("INSERT INTO checklist_run (vesselId, checklistCode, date, completedAt) VALUES (1, 'departure', '2026-06-02', 3)")
            }
            migrate(conn, 2)
            assertEquals(
                listOf(listOf("1", "2026-06-01"), listOf("3", "2026-06-02")),
                conn.rows("SELECT id, date FROM checklist_run ORDER BY id"),
            )
        }
    }

    @Test
    fun `1 to 2 keeps existing records, with no schedule`() {
        open(1).use { conn ->
            conn.createStatement().use { st ->
                st.execute(
                    "INSERT INTO service_record (componentId, date, precision, workTypes, description, createdAt) " +
                        "VALUES (7, '2026-05-01', 'DAY', 'SCHEDULED', 'Oil change', 0)"
                )
            }
            migrate(conn, 1)
            assertEquals(
                listOf(listOf("7", "Oil change", null)),
                conn.rows("SELECT componentId, description, scheduleId FROM service_record"),
            )
        }
    }
}
