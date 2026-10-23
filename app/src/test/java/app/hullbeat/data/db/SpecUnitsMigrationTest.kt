package app.hullbeat.data.db

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/**
 * MIGRATION_3_4 against the component table exactly as the exported Room
 * schemas create it: version 3 migrated must have the columns version 4 has,
 * and a row written before keeps its values.
 */
class SpecUnitsMigrationTest {

    private fun componentCreateSql(version: Int): String {
        val schema = listOf("schemas", "app/schemas").map { File(it) }.first { it.isDirectory }
        val text = File(schema, "app.hullbeat.data.db.AppDatabase/$version.json").readText()
        val entry = text.substringAfter("\"tableName\": \"component\"")
        val sql = entry.substringAfter("\"createSql\": \"").substringBefore("\",")
        return sql.replace("\${TABLE_NAME}", "component").replace("\\\"", "\"")
    }

    private fun columns(conn: Connection): List<String> =
        conn.createStatement().use { st ->
            st.executeQuery("PRAGMA table_info(component)").use { rs ->
                buildList { while (rs.next()) add(rs.getString("name")) }
            }
        }

    @Test
    fun `version 3 migrated has the columns of version 4 and keeps its rows`() {
        val expected = DriverManager.getConnection("jdbc:sqlite::memory:").use { conn ->
            conn.createStatement().use { it.execute(componentCreateSql(4)) }
            columns(conn)
        }
        DriverManager.getConnection("jdbc:sqlite::memory:").use { conn ->
            conn.createStatement().use { st ->
                st.execute(componentCreateSql(3))
                st.execute(
                    """INSERT INTO component (vesselId, categoryCode, name, criticality, specJson, isCustom, archived, sortOrder)
                       VALUES (1, 'fuel', 'Fuel tank', 'MED', '{"Capacity (L)":"40"}', 0, 0, 0)"""
                )
                st.execute(AppDatabase.MIGRATION_3_4_SQL)
            }
            assertEquals(expected.sorted(), columns(conn).sorted())
            conn.createStatement().use { st ->
                st.executeQuery("SELECT specJson, specUnitsJson FROM component").use { rs ->
                    rs.next()
                    assertEquals("""{"Capacity (L)":"40"}""", rs.getString(1))
                    assertEquals(null, rs.getString(2))
                }
            }
        }
    }
}
