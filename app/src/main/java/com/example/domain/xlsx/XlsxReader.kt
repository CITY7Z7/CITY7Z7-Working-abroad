package com.example.domain.xlsx

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

object XlsxReader {

    private const val MAX_ROWS = 5000

    fun read(inputStream: InputStream): SpreadsheetData {
        val zipBytes = inputStream.readBytes()

        // 1. Read shared strings from xl/sharedStrings.xml
        val sharedStrings = readSharedStrings(zipBytes)

        // 2. Read sheet1.xml
        val rows = readSheet1(zipBytes, sharedStrings)

        if (rows.isEmpty()) {
            return SpreadsheetData(emptyList(), emptyList(), false, 0)
        }

        val headers = rows.first()
        val dataRows = if (rows.size > 1) rows.subList(1, rows.size) else emptyList()
        val isTruncated = rows.size >= MAX_ROWS

        return SpreadsheetData(
            headers = headers,
            rows = dataRows,
            isTruncated = isTruncated,
            totalRowCount = rows.size
        )
    }

    private fun readSharedStrings(zipBytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "xl/sharedStrings.xml") {
                    val parser = Xml.newPullParser().apply {
                        setInput(zis, "UTF-8")
                    }
                    var eventType = parser.eventType
                    var currentText = StringBuilder()
                    var inText = false

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                if (parser.name == "t") {
                                    inText = true
                                    currentText.clear()
                                }
                            }
                            XmlPullParser.TEXT -> {
                                if (inText) {
                                    currentText.append(parser.text)
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                if (parser.name == "t") {
                                    inText = false
                                } else if (parser.name == "si") {
                                    strings.add(currentText.toString())
                                    currentText.clear()
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                    break
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return strings
    }

    private fun readSheet1(zipBytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "xl/worksheets/sheet1.xml") {
                    val parser = Xml.newPullParser().apply {
                        setInput(zis, "UTF-8")
                    }
                    var eventType = parser.eventType
                    val currentRow = mutableListOf<String>()
                    var cellType = ""
                    var inValue = false
                    var cellValue = StringBuilder()

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                when (parser.name) {
                                    "row" -> currentRow.clear()
                                    "c" -> {
                                        cellType = parser.getAttributeValue(null, "t") ?: ""
                                        cellValue.clear()
                                    }
                                    "v" -> inValue = true
                                }
                            }
                            XmlPullParser.TEXT -> {
                                if (inValue) {
                                    cellValue.append(parser.text)
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                when (parser.name) {
                                    "v" -> inValue = false
                                    "c" -> {
                                        val rawVal = cellValue.toString().trim()
                                        val text = if (cellType == "s") {
                                            val sIdx = rawVal.toIntOrNull()
                                            if (sIdx != null && sIdx in sharedStrings.indices) sharedStrings[sIdx] else rawVal
                                        } else {
                                            rawVal
                                        }
                                        currentRow.add(text)
                                    }
                                    "row" -> {
                                        rows.add(currentRow.toList())
                                        if (rows.size >= MAX_ROWS) return rows
                                    }
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                    break
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return rows
    }
}
