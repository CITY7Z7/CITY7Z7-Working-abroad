package com.example.domain.normalizer

enum class PasteMode {
    RAW,
    CLEANED,
    QUOTE
}

data class PasteResult(
    val processedText: String,
    val hasSensitiveData: Boolean = false,
    val sensitiveDataWarning: String? = null,
    val isTooLong: Boolean = false,
    val detectedEntities: List<String> = emptyList()
)

object PasteNormalizer {

    private val ZERO_WIDTH_REGEX = Regex("[\\u200B\\u200C\\u200D\\uFEFF]")
    private val PHONE_REGEX = Regex("(?:\\+?\\d[\\d\\-\\s()]{7,15}\\d)")
    private val EMAIL_REGEX = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    private val EIRCODE_REGEX = Regex("\\b[A-Z]\\d{2}\\s?[A-Z0-9]{4}\\b", RegexOption.IGNORE_CASE)
    private val PPS_REGEX = Regex("\\b\\d{7}[A-Z]{1,2}\\b")
    private val IBAN_IE_REGEX = Regex("\\bIE\\d{2}[A-Z0-9]{18}\\b")

    fun process(rawText: String, mode: PasteMode = PasteMode.CLEANED): PasteResult {
        val isTooLong = rawText.length > 20_000
        val detectedEntities = mutableListOf<String>()

        // 1. Detect sensitive data (PPS or Irish IBAN)
        val hasPps = PPS_REGEX.containsMatchIn(rawText)
        val hasIban = IBAN_IE_REGEX.containsMatchIn(rawText)
        val hasSensitive = hasPps || hasIban

        val warning = if (hasSensitive) {
            val sensitiveTypes = mutableListOf<String>()
            if (hasPps) sensitiveTypes.add("PPS-номер")
            if (hasIban) sensitiveTypes.add("IBAN банковского счёта")
            "В тексте обнаружены конфиденциальные данные (${sensitiveTypes.joinToString(", ")}). Рекомендуется сохранить их в приватную заметку!"
        } else null

        // Detect contact entities
        if (PHONE_REGEX.containsMatchIn(rawText)) detectedEntities.add("Телефон")
        if (EMAIL_REGEX.containsMatchIn(rawText)) detectedEntities.add("Email")
        if (EIRCODE_REGEX.containsMatchIn(rawText)) detectedEntities.add("Eircode")

        val resultText = when (mode) {
            PasteMode.RAW -> rawText
            PasteMode.CLEANED -> cleanText(rawText)
            PasteMode.QUOTE -> {
                val cleaned = cleanText(rawText)
                cleaned.lines().joinToString("\n") { "> $it" }
            }
        }

        return PasteResult(
            processedText = resultText,
            hasSensitiveData = hasSensitive,
            sensitiveDataWarning = warning,
            isTooLong = isTooLong,
            detectedEntities = detectedEntities
        )
    }

    private fun cleanText(text: String): String {
        // CRLF -> LF
        var t = text.replace("\r\n", "\n").replace("\r", "\n")

        // Strip zero-width chars
        t = t.replace(ZERO_WIDTH_REGEX, "")

        // Normalize bullets •, –, · -> -
        t = t.replace(Regex("^[ \\t]*[•–·][ \\t]+", RegexOption.MULTILINE), "- ")

        // Normalize numbered lists 1) -> 1.
        t = t.replace(Regex("^[ \\t]*(\\d+)\\)[ \\t]+", RegexOption.MULTILINE), "$1. ")

        // Trim trailing whitespaces on each line
        val lines = t.lines().map { it.trimEnd() }

        // Max 2 consecutive empty lines
        val cleanedLines = mutableListOf<String>()
        var emptyCount = 0
        for (line in lines) {
            if (line.isBlank()) {
                emptyCount++
                if (emptyCount <= 2) {
                    cleanedLines.add("")
                }
            } else {
                emptyCount = 0
                cleanedLines.add(line)
            }
        }

        return cleanedLines.joinToString("\n").trim()
    }
}
