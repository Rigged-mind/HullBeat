package app.hullbeat.data.db

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.sql.DriverManager

/**
 * The backfill run on every database open, against the service_record table
 * exactly as the exported Room schema creates it.
 */
class RecordCurrencyBackfillTest {

    private fun serviceRecordCreateSql(): String {
        val schema = listOf("schemas", "app/schemas").map { File(it) }.first { it.isDirectory }
        val latest = File(schema, "app.hullbeat.data.db.AppDatabase")
            .listFiles { f -> f.extension == "json" }!!
            .maxBy { it.nameWithoutExtension.toInt() }
            .readText()
        // The table's entry in the exported schema; parsed by hand so the
        // test needs nothing beyond JUnit and the driver.
        val entry = latest.substringAfter("\"tableName\": \"service_record\"")
        val sql = entry.substringAfter("\"createSql\": \"").substringBefore("\",")
        return sql.replace("\${TABLE_NAME}", "service_record")
    }

    @Test
    fun `fills the current currency into costs that have none, and nothing else`() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { conn ->
            conn.createStatement().use { it.execute(serviceRecordCreateSql()) }
            conn.createStatement().use {
                it.execute(
                    """
                    INSERT INTO service_record
                        (id, componentId, date, precision, workTypes, description, cost, currency, createdAt)
                    VALUES
                        (1, 1, '2025-05-01', 'DAY', 'SCHEDULED', 'old cost', 243023.0, NULL, 0),
                        (2, 1, '2026-05-01', 'DAY', 'SCHEDULED', 'paid in euro', 450.0, 'EUR', 0),
                        (3, 1, '2026-06-01', 'DAY', 'SCHEDULED', 'no cost', NULL, NULL, 0)
                    """.trimIndent()
                )
            }

            conn.prepareStatement(AppDatabase.BACKFILL_RECORD_CURRENCY_SQL).use {
                it.setString(1, "UAH")
                it.executeUpdate()
            }
            // A second open after the owner switched to euro changes nothing.
            conn.prepareStatement(AppDatabase.BACKFILL_RECORD_CURRENCY_SQL).use {
                it.setString(1, "EUR")
                assertEquals(0, it.executeUpdate())
            }

            val rows = conn.createStatement().use { st ->
                st.executeQuery("SELECT id, currency FROM service_record ORDER BY id").use { rs ->
                    buildList { while (rs.next()) add(rs.getLong(1) to rs.getString(2)) }
                }
            }
            assertEquals(listOf(1L to "UAH", 2L to "EUR", 3L to null), rows)
        }
    }
}
