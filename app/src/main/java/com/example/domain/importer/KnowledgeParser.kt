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
import java.net.HttpURLConnection
import java.net.URL
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

    /**
     * Fetches web page by URL and converts HTML into readable Markdown text.
     */
    fun fetchAndParseWebUrl(
        urlString: String,
        countryId: String,
        sectionId: String
    ): Result<ArticleEntity> {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Android; Mobile; rv:100.0) Gecko/100.0 Firefox/100.0"
            )

            if (connection.responseCode !in 200..299) {
                return Result.failure(Exception("Сервер вернул ошибку: ${connection.responseCode}"))
            }

            val html = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }

            // Extract <title>
            val titleMatch = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(html)
            val rawTitle = titleMatch?.groupValues?.get(1)?.trim() ?: "Материал из сети"
            val cleanTitle = cleanHtmlEntities(rawTitle).take(120)

            // Convert basic HTML content to Markdown
            val cleanMarkdown = htmlToMarkdown(html, urlString)

            val fullSectionId = if (sectionId.contains(".")) sectionId else "$countryId.$sectionId"
            val article = ArticleEntity(
                id = "$countryId.web.${UUID.randomUUID().toString().take(8)}",
                countryId = countryId,
                sectionId = fullSectionId,
                kind = ArticleKind.GUIDE,
                titleRu = cleanTitle,
                subtitle = "Сайт: ${url.host}",
                bodyMd = cleanMarkdown,
                keywords = "$cleanTitle ${url.host}",
                applicableTo = "ANY",
                sourceIds = url.host,
                verifiedAt = java.time.LocalDate.now().toString(),
                status = VerificationStatus.VERIFIED,
                archived = 0,
                sortOrder = 100
            )

            Result.success(article)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun htmlToMarkdown(html: String, sourceUrl: String): String {
        // Remove scripts, styles, heads, nav, footer
        var text = html
            .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<nav[\\s\\S]*?</nav>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<footer[\\s\\S]*?</footer>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<header[\\s\\S]*?</header>", RegexOption.IGNORE_CASE), "")

        // Convert headings
        text = text.replace(Regex("<h1[^>]*>(.*?)</h1>", RegexOption.IGNORE_CASE), "\n# $1\n")
        text = text.replace(Regex("<h2[^>]*>(.*?)</h2>", RegexOption.IGNORE_CASE), "\n## $1\n")
        text = text.replace(Regex("<h3[^>]*>(.*?)</h3>", RegexOption.IGNORE_CASE), "\n### $1\n")

        // Paragraphs & breaks
        text = text.replace(Regex("<p[^>]*>(.*?)</p>", RegexOption.IGNORE_CASE), "\n$1\n")
        text = text.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")

        // Lists
        text = text.replace(Regex("<li[^>]*>(.*?)</li>", RegexOption.IGNORE_CASE), "\n- $1")

        // Remove all other remaining HTML tags
        text = text.replace(Regex("<[^>]+>"), "")

        // Decode entities
        text = cleanHtmlEntities(text)

        // Clean up excessive blank lines
        val cleanedLines = text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n\n")

        return "### Источник: [$sourceUrl]($sourceUrl)\n\n$cleanedLines"
    }

    private fun cleanHtmlEntities(text: String): String {
        return text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace("&laquo;", "«")
            .replace("&raquo;", "»")
    }
}
