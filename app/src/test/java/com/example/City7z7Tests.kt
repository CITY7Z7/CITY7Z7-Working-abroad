package com.example

import com.example.domain.calculator.NetCalculatorIE
import com.example.domain.calculator.ReverseCalculator
import com.example.domain.expiry.ExpiryCalculator
import com.example.domain.expiry.ExpiryUrgency
import com.example.domain.markdown.MarkdownNode
import com.example.domain.markdown.MarkdownParser
import com.example.domain.normalizer.PasteMode
import com.example.domain.normalizer.PasteNormalizer
import com.example.domain.search.SearchQueryBuilder
import com.example.domain.vcard.VCardBuilder
import com.example.ui.theme.oklch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class City7z7Tests {

    // --- 1. SearchQueryBuilder Tests ---
    @Test
    fun testSearchQueryBuilder_normalization() {
        val query = SearchQueryBuilder.buildFtsQuery("  Налог  и  жильё  ")
        assertEquals("налог* AND жилье*", query)
    }

    @Test
    fun testSearchQueryBuilder_stripsSpecialChars() {
        val query = SearchQueryBuilder.buildFtsQuery("\"PPS-номер\": ^тест*")
        assertEquals("pps* AND номер* AND тест*", query)
    }

    @Test
    fun testSearchQueryBuilder_snippet() {
        val full = "В Ирландии для начала работы требуется оформить PPS номер в службе MyWelfare."
        val snippet = SearchQueryBuilder.extractSnippet(full, "PPS", 40)
        assertTrue(snippet.contains("PPS"))
    }

    // --- 2. NetCalculatorIE Benchmark Tests (Section 7.1 & 9.8) ---
    @Test
    fun testNetCalculator_2027_minWage() {
        // IE, 2027, 14.94 €/h, 39h: gross 30 298 € -> net ≈ 26 734 €/year (≈ 2 228 €/month), within 1%
        val breakdown = NetCalculatorIE.calculate(
            hourlyRate = 14.94,
            weeklyHours = 39.0,
            year = 2027
        )

        val expectedGross = 30298.0
        val expectedNetYear = 26734.0
        val expectedNetMonth = 2228.0

        assertEquals(expectedGross, breakdown.grossYear, expectedGross * 0.01)
        assertEquals(expectedNetYear, breakdown.netYear, expectedNetYear * 0.01)
        assertEquals(expectedNetMonth, breakdown.netMonth, expectedNetMonth * 0.01)
    }

    @Test
    fun testNetCalculator_2027_higherBracket() {
        // IE, 2027, gross 60 000 € -> net ≈ 45 601 €/year (≈ 3 800 €/month), within 1%
        val breakdown = NetCalculatorIE.calculateFromGross(60000.0, year = 2027)

        val expectedNetYear = 45601.0
        val expectedNetMonth = 3800.0

        assertEquals(expectedNetYear, breakdown.netYear, expectedNetYear * 0.01)
        assertEquals(expectedNetMonth, breakdown.netMonth, expectedNetMonth * 0.01)
    }

    // --- 3. ReverseCalculator Tests ---
    @Test
    fun testReverseCalculator_convergence() {
        val targetNet = 26734.0
        val result = ReverseCalculator.findHourlyRateForNet(
            targetNetYear = targetNet,
            weeklyHours = 39.0,
            year = 2027,
            minimumWage = 14.94
        )

        // Must converge within 50 iterations
        assertTrue(result.iterations <= 50)
        // Rate should be near 14.94
        assertEquals(14.94, result.requiredHourlyRate, 0.1)
        assertFalse(result.isBelowMinimumWage)
    }

    @Test
    fun testReverseCalculator_belowMinimumWageAlert() {
        val result = ReverseCalculator.findHourlyRateForNet(
            targetNetYear = 10000.0,
            weeklyHours = 39.0,
            year = 2027,
            minimumWage = 14.94
        )
        assertTrue(result.isBelowMinimumWage)
    }

    // --- 4. PasteNormalizer Tests ---
    @Test
    fun testPasteNormalizer_cleanBullets() {
        val raw = "• Пункт 1\n– Пункт 2\n· Пункт 3\n1) Пункт 4"
        val result = PasteNormalizer.process(raw, PasteMode.CLEANED)
        assertTrue(result.processedText.contains("- Пункт 1"))
        assertTrue(result.processedText.contains("- Пункт 2"))
        assertTrue(result.processedText.contains("1. Пункт 4"))
    }

    @Test
    fun testPasteNormalizer_detectSensitivePPSandIBAN() {
        val withPps = "Мой налоговый номер 1234567T для работы"
        val resPps = PasteNormalizer.process(withPps)
        assertTrue(resPps.hasSensitiveData)
        assertNotNull(resPps.sensitiveDataWarning)

        val withIban = "Переведите на IE29AIBK93115212345678"
        val resIban = PasteNormalizer.process(withIban)
        assertTrue(resIban.hasSensitiveData)
    }

    // --- 5. MarkdownParser Tests ---
    @Test
    fun testMarkdownParser_checklists() {
        val md = "- [ ] Не сделано\n- [x] Сделано"
        val nodes = MarkdownParser.parse(md)
        val checkItems = nodes.filterIsInstance<MarkdownNode.ChecklistItem>()
        assertEquals(2, checkItems.size)
        assertFalse(checkItems[0].isChecked)
        assertTrue(checkItems[1].isChecked)
    }

    @Test
    fun testMarkdownParser_tables() {
        val md = """
            | Страна | Минималка |
            | :--- | ---: |
            | Ирландия | 14.94 € |
            | Германия | 14.60 € |
        """.trimIndent()

        val nodes = MarkdownParser.parse(md)
        val table = nodes.filterIsInstance<MarkdownNode.Table>().firstOrNull()
        assertNotNull(table)
        assertEquals(2, table!!.headers.size)
        assertEquals(2, table.rows.size)
        assertEquals("Ирландия", table.rows[0][0])
    }

    // --- 6. ExpiryCalculator Tests ---
    @Test
    fun testExpiryCalculator_thresholds() {
        val today = LocalDate.of(2027, 2, 28) // Leap year test around Feb
        val docExpiresIn80Days = today.plusDays(80).toEpochDay()
        val status80 = ExpiryCalculator.calculate(docExpiresIn80Days, today)
        assertEquals(ExpiryUrgency.ATTENTION_90, status80.urgency)
        assertEquals(90, status80.thresholdCrossed)

        val docExpiresIn25Days = today.plusDays(25).toEpochDay()
        val status25 = ExpiryCalculator.calculate(docExpiresIn25Days, today)
        assertEquals(ExpiryUrgency.CRITICAL_30, status25.urgency)

        val docExpired = today.minusDays(5).toEpochDay()
        val statusExpired = ExpiryCalculator.calculate(docExpired, today)
        assertEquals(ExpiryUrgency.EXPIRED, statusExpired.urgency)
    }

    // --- 7. VCardBuilder Tests ---
    @Test
    fun testVCardBuilder_validVCard3() {
        val vcard = VCardBuilder.buildVCard3(
            VCardBuilder.ContactData(
                fullNameLatin = "Ivan Ivanov",
                profession = "Software Engineer",
                phone = "+353871234567",
                email = "ivan@example.com",
                country = "Ireland"
            )
        )
        assertTrue(vcard.startsWith("BEGIN:VCARD\r\nVERSION:3.0"))
        assertTrue(vcard.contains("FN:Ivan Ivanov"))
        assertTrue(vcard.contains("TEL;TYPE=CELL:+353871234567"))
        assertTrue(vcard.endsWith("END:VCARD\r\n"))
        assertTrue(vcard.toByteArray().size <= 1000)
    }

    // --- 8. OklchConverter Tests ---
    @Test
    fun testOklch_returnsValidColor() {
        val color = oklch(0.26, 0.03, 243.97)
        assertTrue(color.red in 0f..1f)
        assertTrue(color.green in 0f..1f)
        assertTrue(color.blue in 0f..1f)
    }
}
