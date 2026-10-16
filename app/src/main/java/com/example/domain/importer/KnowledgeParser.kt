package com.example.domain.importer

import com.example.data.database.entity.ArticleEntity
import com.example.data.database.entity.ArticleKind
import com.example.data.database.entity.VerificationStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.UUID

object KnowledgeParser {

    /**
     * Parses MD or TXT text into ArticleEntity items.
     * Recognizes top-level # and ## headers as article boundaries or processes whole text.
     */
    fun parseMarkdownOrText(
        text: String,
        defaultCountryId: String = "ie",
        defaultSectionId: String = "status"
    ): List<ArticleEntity> {
        val lines = text.lines()
        val articles = mutableListOf<ArticleEntity>()

        var currentCountryId = defaultCountryId
        var currentSectionId = defaultSectionId
        var currentTitle = ""
        var currentBody = StringBuilder()

        fun flushArticle() {
            val title = currentTitle.trim().ifBlank { "Загруженный материал" }
            val body = currentBody.toString().trim()
            if (body.isNotBlank()) {
                val fullSectionId = if (currentSectionId.contains(".")) currentSectionId else "$currentCountryId.$currentSectionId"
                articles.add(
                    ArticleEntity(
                        id = "$currentCountryId.custom.${UUID.randomUUID().toString().take(8)}",
                        countryId = currentCountryId,
                        sectionId = fullSectionId,
                        kind = ArticleKind.GUIDE,
                        titleRu = title,
                        subtitle = "Импортировано пользователем",
                        bodyMd = body,
                        keywords = "$title $currentCountryId",
                        applicableTo = "ANY",
                        sourceIds = "custom_import",
                        verifiedAt = java.time.LocalDate.now().toString(),
                        status = VerificationStatus.VERIFIED,
                        archived = 0,
                        sortOrder = 100
                    )
                )
            }
            currentTitle = ""
            currentBody = StringBuilder()
        }

        for (line in lines) {
            val trimmed = line.trim()

            // Country switch detection
            if (trimmed.equals("# Ирландия", ignoreCase = true) || trimmed.equals("--- Ирландия ---", ignoreCase = true)) {
                flushArticle()
                currentCountryId = "ie"
                continue
            }
            if (trimmed.equals("# Германия", ignoreCase = true) || trimmed.equals("--- Германия ---", ignoreCase = true)) {
                flushArticle()
                currentCountryId = "de"
                continue
            }
            if (trimmed.equals("# Нидерланды", ignoreCase = true) || trimmed.equals("--- Нидерланды ---", ignoreCase = true)) {
                flushArticle()
                currentCountryId = "nl"
                continue
            }

            // Section detection
            if (trimmed.startsWith("## ") || trimmed.startsWith("# ")) {
                flushArticle()
                currentTitle = trimmed.removePrefix("## ").removePrefix("# ").trim()
                continue
            }

            currentBody.append(line).append("\n")
        }

        flushArticle()
        return articles
    }

    /**
     * Parses JSON array containing article objects.
     */
    fun parseJson(jsonString: String, defaultCountryId: String = "ie"): List<ArticleEntity> {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val element = json.parseToJsonElement(jsonString)
            val list = mutableListOf<ArticleEntity>()

            val array = if (element is kotlinx.serialization.json.JsonArray) {
                element.jsonArray
            } else if (element.jsonObject.containsKey("articles")) {
                element.jsonObject["articles"]!!.jsonArray
            } else {
                return emptyList()
            }

            for (item in array) {
                val obj = item.jsonObject
                val country = obj["countryId"]?.jsonPrimitive?.content ?: defaultCountryId
                val section = obj["sectionId"]?.jsonPrimitive?.content ?: "$country.general"
                val title = obj["titleRu"]?.jsonPrimitive?.content ?: obj["title"]?.jsonPrimitive?.content ?: "Без названия"
                val body = obj["bodyMd"]?.jsonPrimitive?.content ?: obj["body"]?.jsonPrimitive?.content ?: ""

                list.add(
                    ArticleEntity(
                        id = obj["id"]?.jsonPrimitive?.content ?: "$country.json.${UUID.randomUUID().toString().take(8)}",
                        countryId = country,
                        sectionId = section,
                        kind = ArticleKind.GUIDE,
                        titleRu = title,
                        subtitle = obj["subtitle"]?.jsonPrimitive?.content ?: "Импорт JSON",
                        bodyMd = body,
                        keywords = obj["keywords"]?.jsonPrimitive?.content ?: title,
                        applicableTo = "ANY",
                        sourceIds = "custom_json",
                        verifiedAt = java.time.LocalDate.now().toString(),
                        status = VerificationStatus.VERIFIED,
                        archived = 0,
                        sortOrder = 100
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}

