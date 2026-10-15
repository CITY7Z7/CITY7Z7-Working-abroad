package com.example.domain.xlsx

import java.io.InputStream

data class SpreadsheetData(
    val headers: List<String>,
    val rows: List<List<String>>,
    val isTruncated: Boolean,
    val totalRowCount: Int
)

object CsvReader {

    private const val MAX_ROWS = 5000

    fun read(inputStream: InputStream): SpreadsheetData {
        val lines = inputStream.bufferedReader().readLines()
        if (lines.isEmpty()) {
            return SpreadsheetData(emptyList(), emptyList(), false, 0)
        }

        // Detect delimiter from first line (comma vs semicolon vs tab)
        val firstLine = lines.first()
        val delimiter = when {
            firstLine.count { it == ';' } > firstLine.count { it == ',' } -> ';'
            firstLine.count { it == '\t' } > firstLine.count { it == ',' } -> '\t'
            else -> ','
        }

        val parsedRows = mutableListOf<List<String>>()
        var rowCount = 0
        var isTruncated = false

        for (line in lines) {
            if (rowCount >= MAX_ROWS) {
                isTruncated = true
                break
            }
            parsedRows.add(parseCsvLine(line, delimiter))
            rowCount++
        }

        val headers = parsedRows.firstOrNull() ?: emptyList()
        val dataRows = if (parsedRows.size > 1) parsedRows.subList(1, parsedRows.size) else emptyList()

        return SpreadsheetData(
            headers = headers,
            rows = dataRows,
            isTruncated = isTruncated,
            totalRowCount = lines.size
        )
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val cells = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == delimiter && !inQuotes) {
                cells.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        cells.add(sb.toString().trim())
        return cells
    }
}
