package app.hullbeat.data.db

import androidx.room.withTransaction

/**
 * Undo one past spreadsheet import: its records, its raw rows, the session.
 *
 * The CSV importer itself is gone (2026-10-25): it could only read flat CSV,
 * and a file with no node or date column put every row on the boat's first
 * node, dated today - which then counted as that node's last service. What
 * owners imported before that stays in the database and in Settings, and
 * this is the one way to take a bad import back out in a single step.
 */
suspend fun AppDatabase.rollbackImportSession(sessionId: Long) = withTransaction {
    serviceRecordDao().deleteByImportSession(sessionId)
    importDao().deleteRowsForSession(sessionId)
    importDao().deleteSession(sessionId)
}
