package com.example.domain.search

object SearchQueryBuilder {

    /**
     * Builds a safe FTS4 query:
     * - trim, lowercase
     * - 'ё' -> 'е'
     * - remove special FTS characters: " * ( ) : ^ -
     * - append '*' to each token
     * - join with AND
     */
    fun buildFtsQuery(rawQuery: String): String {
        val normalized = rawQuery.trim().lowercase()
            .replace('ё', 'е')
            .replace('"', ' ')
            .replace('*', ' ')
            .replace('(', ' ')
            .replace(')', ' ')
            .replace(':', ' ')
            .replace('^', ' ')
            .replace('-', ' ')

        val tokens = normalized.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return ""

        val meaningful = tokens.filter { it.length > 1 }
        val finalTokens = if (meaningful.isNotEmpty()) meaningful else tokens

        return finalTokens.joinToString(" AND ") { "$it*" }
    }

    /**
     * Normalizes text for in-memory comparison / highlight.
     */
    fun normalizeText(text: String): String {
        return text.trim().lowercase().replace('ё', 'е')
    }

    /**
     * Extracts a snippet around the first match (up to 120 chars).
     */
    fun extractSnippet(fullText: String, rawQuery: String, maxLength: Int = 120): String {
        val queryNorm = normalizeText(rawQuery)
        val textNorm = normalizeText(fullText)
        val tokens = queryNorm.split("\\s+".toRegex()).filter { it.length > 1 }

        var firstIndex = -1
        for (token in tokens) {
            val idx = textNorm.indexOf(token)
            if (idx != -1 && (firstIndex == -1 || idx < firstIndex)) {
                firstIndex = idx
            }
        }

        if (firstIndex == -1) {
            return if (fullText.length <= maxLength) fullText else fullText.take(maxLength) + "…"
        }

        val start = (firstIndex - maxLength / 3).coerceAtLeast(0)
        val end = (start + maxLength).coerceAtMost(fullText.length)
        val snippet = fullText.substring(start, end).replace('\n', ' ')

        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < fullText.length) "…" else ""
        return "$prefix$snippet$suffix"
    }
}
