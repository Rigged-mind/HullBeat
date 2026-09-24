package app.hullbeat.domain.importer

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlin.ByteArray

object CsvSniffer {

    data class SniffResult(
        val delimiter: Char,
        val charset: Charset,
        val headers: List<String>,
        val sampleRows: List<List<String>>,
        val allRows: List<List<String>>,
    )

    private val CANDIDATE_DELIMITERS = charArrayOf(',', ';', '\t', '|')

    fun sniffAndParse(inputStream: InputStream): SniffResult {
        val bytes = inputStream.readBytes()
        val charset = detectCharset(bytes)
        var text = String(bytes, charset)
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1)
        }
        val delimiter = detectDelimiter(text)
        val allRows = parse(text, delimiter)
        val headers = allRows.firstOrNull() ?: emptyList()
        val dataRows = if (allRows.size > 1) allRows.subList(1, allRows.size) else emptyList()
        val sampleRows = dataRows.take(20)

        return SniffResult(
            delimiter = delimiter,
            charset = charset,
            headers = headers,
            sampleRows = sampleRows,
            allRows = dataRows,
        )
    }

    fun parse(text: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        val len = text.length

        while (i < len) {
            val c = text[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < len && text[i + 1] == '"') {
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == delimiter && !inQuotes -> {
                    currentRow.add(currentField.toString().trim())
                    currentField.setLength(0)
                }
                (c == '\r' || c == '\n') && !inQuotes -> {
                    if (c == '\r' && i + 1 < len && text[i + 1] == '\n') {
                        i++
                    }
                    currentRow.add(currentField.toString().trim())
                    currentField.setLength(0)
                    if (currentRow.any { it.isNotEmpty() }) {
                        rows.add(currentRow.toList())
                    }
                    currentRow.clear()
                }
                else -> {
                    currentField.append(c)
                }
            }
            i++
        }
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString().trim())
            if (currentRow.any { it.isNotEmpty() }) {
                rows.add(currentRow.toList())
            }
        }
        return rows
    }

    fun detectDelimiter(text: String): Char {
        val sampleLines = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(10)
            .toList()

        if (sampleLines.isEmpty()) return ','

        var bestDelimiter = ','
        var bestScore = -1

        for (delim in CANDIDATE_DELIMITERS) {
            val counts = sampleLines.map { countDelimiter(it, delim) }
            val firstCount = counts.firstOrNull() ?: 0
            if (firstCount > 0 && counts.all { it == firstCount }) {
                val score = firstCount * 100
                if (score > bestScore) {
                    bestScore = score
                    bestDelimiter = delim
                }
            } else if (firstCount > 0) {
                val score = counts.sum()
                if (score > bestScore) {
                    bestScore = score
                    bestDelimiter = delim
                }
            }
        }
        return bestDelimiter
    }

    private fun countDelimiter(line: String, delimiter: Char): Int {
        var count = 0
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == delimiter && !inQuotes) {
                count++
            }
            i++
        }
        return count
    }

    private fun detectCharset(bytes: ByteArray): Charset {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return StandardCharsets.UTF_8
        }
        return try {
            val decoder = StandardCharsets.UTF_8.newDecoder()
            decoder.decode(ByteBuffer.wrap(bytes))
            StandardCharsets.UTF_8
        } catch (_: Exception) {
            try {
                Charset.forName("windows-1251")
            } catch (_: Exception) {
                StandardCharsets.ISO_8859_1
            }
        }
    }
}