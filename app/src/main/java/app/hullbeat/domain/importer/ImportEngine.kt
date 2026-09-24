package app.hullbeat.domain.importer

import androidx.room.withTransaction
import app.hullbeat.data.db.AppDatabase
import app.hullbeat.data.db.Component
import app.hullbeat.data.db.Criticality
import app.hullbeat.data.db.DatePrecision
import app.hullbeat.data.db.ImportRow
import app.hullbeat.data.db.ImportSession
import app.hullbeat.data.db.Meter
import app.hullbeat.data.db.MeterReading
import app.hullbeat.data.db.MeterUnit
import app.hullbeat.data.db.ReadingSource
import app.hullbeat.data.db.ServiceRecord
import app.hullbeat.data.db.WorkType
import java.io.InputStream
import java.security.MessageDigest
import java.time.LocalDate
import kotlin.ByteArray
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

object ImportEngine {

    data class ImportPreview(
        val fileName: String,
        val fileHash: String,
        val delimiter: Char,
        val headers: List<String>,
        val suggestedMapping: Map<Int, TargetField>,
        val sampleRows: List<List<String>>,
        val totalRowCount: Int,
        val allRows: List<List<String>>,
    )

    data class ImportResult(
        val sessionId: Long,
        val rowCount: Int,
        val importedRecords: Int,
    )

    fun createPreview(inputStream: InputStream, fileName: String): ImportPreview {
        val sniff = CsvSniffer.sniffAndParse(inputStream)
        val suggested = ColumnMapper.suggestMapping(sniff.headers, sniff.sampleRows)
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(fileName.toByteArray())
            .joinToString("") { "%02x".format(it) }

        return ImportPreview(
            fileName = fileName,
            fileHash = hash,
            delimiter = sniff.delimiter,
            headers = sniff.headers,
            suggestedMapping = suggested,
            sampleRows = sniff.sampleRows,
            totalRowCount = sniff.allRows.size,
            allRows = sniff.allRows,
        )
    }

    suspend fun commitImport(
        vesselId: Long,
        preview: ImportPreview,
        mapping: Map<Int, TargetField>,
        db: AppDatabase,
    ): ImportResult = db.withTransaction {
        var existingComponents = db.componentDao().forVessel(vesselId)
        if (existingComponents.isEmpty()) {
            val defaultCompId = db.componentDao().insert(
                Component(
                    vesselId = vesselId,
                    categoryCode = "general",
                    name = "General Maintenance",
                    customName = null,
                    criticality = Criticality.MED,
                )
            )
            existingComponents = db.componentDao().forVessel(vesselId)
        }

        val mappingJson = mapping.entries.joinToString(
            prefix = "{",
            postfix = "}",
            separator = ",",
            transform = { "\"${it.key}\":\"${it.value.name}\"" }
        )

        val sessionId = db.importDao().insertSession(
            ImportSession(
                fileName = preview.fileName,
                fileHash = preview.fileHash,
                format = "CSV",
                layout = "FLAT",
                mappingJson = mappingJson,
                sheetCount = 1,
                rowCount = preview.totalRowCount,
                status = "COMMITTED",
            )
        )

        val importRows = preview.allRows.mapIndexed { index, row ->
            // Through the serializer: a cell holding a backslash, a tab or a
            // line break from a quoted CSV field is ordinary data, and hand-
            // escaping only the quote stored it as JSON nothing could read back.
            val rowJson = JsonArray(row.map { JsonPrimitive(it) }).toString()
            ImportRow(
                sessionId = sessionId,
                sheet = "Sheet1",
                rowIndex = index + 1,
                rawJson = rowJson,
                parseStatus = "OK",
            )
        }
        db.importDao().insertRows(importRows)

        var importedCount = 0

        for ((rowIndex, row) in preview.allRows.withIndex()) {
            var datePair = Pair(LocalDate.now(), DatePrecision.DAY)
            var description = ""
            var workType = WorkType.SCHEDULED
            var hours: Double? = null
            var cost: Double? = null
            var performedBy: String? = null
            var notes: String? = null
            var targetComponentId: Long? = null

            for ((colIndex, cell) in row.withIndex()) {
                val field = mapping[colIndex] ?: TargetField.IGNORE
                val text = cell.trim()
                if (text.isEmpty()) continue

                when (field) {
                    TargetField.DATE -> {
                        val parsed = ColumnMapper.parseDate(text)
                        if (parsed != null) {
                            datePair = parsed
                        }
                    }
                    TargetField.DESCRIPTION -> {
                        if (description.isEmpty()) {
                            description = text
                        } else {
                            description = "$description $text"
                        }
                    }
                    TargetField.WORK_TYPE -> {
                        val upper = text.uppercase()
                        workType = WorkType.entries.firstOrNull { upper.contains(it.name) } ?: WorkType.SCHEDULED
                    }
                    TargetField.HOURS -> {
                        val parsedHours = ColumnMapper.parseHours(text)
                        if (parsedHours != null) {
                            hours = parsedHours
                        }
                    }
                    TargetField.COST -> {
                        val parsedCost = ColumnMapper.parseCost(text)
                        if (parsedCost != null) {
                            cost = parsedCost
                        }
                    }
                    TargetField.PERFORMED_BY -> {
                        performedBy = text
                    }
                    TargetField.NOTES -> {
                        notes = if (notes == null) text else "$notes; $text"
                    }
                    TargetField.COMPONENT -> {
                        val match = existingComponents.firstOrNull { comp ->
                            comp.name.equals(text, ignoreCase = true) ||
                                (comp.customName != null && comp.customName.equals(text, ignoreCase = true)) ||
                                (comp.catalogCode != null && comp.catalogCode.equals(text, ignoreCase = true))
                        }
                        if (match != null) {
                            targetComponentId = match.id
                        }
                    }
                    TargetField.IGNORE -> {}
                }
            }

            if (description.isEmpty()) {
                description = row.filter { it.isNotBlank() }.take(3).joinToString(" · ")
            }
            if (description.isEmpty()) {
                description = "Imported record #${rowIndex + 1}"
            }

            val finalComponentId = targetComponentId ?: existingComponents.first().id

            var meterReadingId: Long? = null
            if (hours != null && hours > 0.0) {
                val meters = db.meterDao().forComponent(finalComponentId)
                val meterId = meters.firstOrNull()?.id ?: db.meterDao().insertMeter(
                    Meter(componentId = finalComponentId, unit = MeterUnit.HOURS)
                )
                meterReadingId = db.meterDao().insertReading(
                    MeterReading(
                        meterId = meterId,
                        date = datePair.first,
                        precision = datePair.second,
                        value = hours,
                        source = ReadingSource.IMPORT,
                    )
                )
            }

            val record = ServiceRecord(
                componentId = finalComponentId,
                scheduleId = null,
                date = datePair.first,
                precision = datePair.second,
                workTypes = workType.name,
                description = description,
                cost = cost,
                currency = null,
                meterReadingId = meterReadingId,
                performedBy = performedBy,
                notes = notes,
                importSessionId = sessionId,
                importSheet = "Sheet1",
                importRowIndex = rowIndex + 1,
            )
            db.serviceRecordDao().insert(record)
            importedCount++
        }

        ImportResult(
            sessionId = sessionId,
            rowCount = preview.totalRowCount,
            importedRecords = importedCount,
        )
    }

    suspend fun rollbackSession(sessionId: Long, db: AppDatabase) = db.withTransaction {
        db.serviceRecordDao().deleteByImportSession(sessionId)
        db.importDao().deleteRowsForSession(sessionId)
        db.importDao().deleteSession(sessionId)
    }
}