package com.example.domain.importer

import com.example.data.database.entity.ArticleEntity
import com.example.data.database.entity.ArticleKind
import com.example.data.database.entity.VerificationStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.util.UUID

object KnowledgeParser {

    data class Frontmatter(
        val title: String? = null,
        val country: String? = null,
        val section: String? = null,
        val source: String? = null,
        val verified: String? = null
    )

    /**
     * Parses optional YAML-style frontmatter at the beginning of the file:
     * ---
     * Title: Example Title
     * Country: ie
     * Section: ie.tax
     * Source: https://revenue.ie
     * Verified: 2026-10-14
     * ---
     */
    fun extractFrontmatter(text: String): Pair<Frontmatter, String> {
        val trimmed = text.trimStart()
        if (!trimmed.startsWith("---")) {
            return Pair(Frontmatter(), text)
        }

        val secondDelimiter = trimmed.indexOf("\n---", startIndex = 3)
        if (secondDelimiter == -1) {
            return Pair(Frontmatter(), text)
        }

        val frontmatterContent = trimmed.substring(3, secondDelimiter)
        val bodyContent = trimmed.substring(secondDelimiter + 4).trimStart('\r', '\n')

        var title: String? = null
        var country: String? = null
        var section: String? = null
        var source: String? = null
        var verified: String? = null

        for (line in frontmatterContent.lines()) {
            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
                val key = line.substring(0, colonIndex).trim().lowercase()
                val value = line.substring(colonIndex + 1).trim().trim('"', '\'')
                when (key) {
                    "title" -> title = value
                    "country" -> country = value
                    "section" -> section = value
                    "source" -> source = value
                    "verified" -> verified = value
                }
            }
        }

        return Pair(
            Frontmatter(title, country, section, source, verified),
            bodyContent
        )
    }

    /**
     * Parses MD or TXT text into ArticleEntity items.
     * Extracts frontmatter metadata (Title, Country, Section, Source, Verified)
     * and splits remaining text by # and ## headings.
     */
    fun parseMarkdownOrText(
        text: String,
        defaultCountryId: String = "ie",
        defaultSectionId: String = "status"
    ): List<ArticleEntity> {
        val (frontmatter, content) = extractFrontmatter(text)

        val countryId = frontmatter.country?.ifBlank { defaultCountryId } ?: defaultCountryId
        val sectionId = frontmatter.section?.ifBlank { defaultSectionId } ?: defaultSectionId
        val fullSectionId = if (sectionId.contains(".")) sectionId else "$countryId.$sectionId"
        val defaultSource = frontmatter.source ?: "custom_import"
        val defaultVerified = frontmatter.verified ?: LocalDate.now().toString()

        val lines = content.lines()
        val articles = mutableListOf<ArticleEntity>()

        var currentTitle = frontmatter.title ?: ""
        var currentBody = StringBuilder()

        fun flushArticle() {
            val title = currentTitle.trim().ifBlank { "Загруженный материал" }
            val body = currentBody.toString().trim()
            if (body.isNotBlank()) {
                articles.add(
                    ArticleEntity(
                        id = "$countryId.custom.${UUID.randomUUID().toString().take(8)}",
                        countryId = countryId,
                        sectionId = fullSectionId,
                        kind = ArticleKind.GUIDE,
                        titleRu = title,
                        subtitle = "Импортировано пользователем",
                        bodyMd = body,
                        keywords = "$title $countryId",
                        applicableTo = "ANY",
                        sourceIds = defaultSource,
                        verifiedAt = defaultVerified,
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

            // Header split detection
            if (trimmed.startsWith("## ") || trimmed.startsWith("# ")) {
                if (currentBody.isNotBlank()) {
                    flushArticle()
                }
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
                val source = obj["source"]?.jsonPrimitive?.content ?: obj["sourceIds"]?.jsonPrimitive?.content ?: "custom_json"
                val verifiedAt = obj["verifiedAt"]?.jsonPrimitive?.content ?: LocalDate.now().toString()

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
                        sourceIds = source,
                        verifiedAt = verifiedAt,
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
