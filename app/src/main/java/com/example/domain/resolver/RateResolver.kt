package com.example.domain.resolver

import com.example.data.database.entity.RateEntity
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object RateResolver {

    private val rateRegex = Regex("\\{\\{rate:([a-zA-Z0-9_.]+)\\}\\}")
    private val decimalFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.GERMANY))

    /**
     * Resolves {{rate:<key>}} tokens in Markdown text using the provided map of rates.
     */
    fun resolveRates(text: String, rates: Map<String, RateEntity>): String {
        return rateRegex.replace(text) { matchResult ->
            val key = matchResult.groupValues[1]
            val rate = rates[key]
            if (rate != null) {
                formatRateValue(rate)
            } else {
                "уточните на официальном сайте"
            }
        }
    }

    private fun formatRateValue(rate: RateEntity): String {
        val formattedNumber = decimalFormat.format(rate.value)
        return when (rate.unit) {
            "EUR/h" -> "$formattedNumber €/ч"
            "EUR/year" -> "$formattedNumber €/год"
            "EUR/month" -> "$formattedNumber €/мес"
            "percent" -> "${rate.value}%"
            "days" -> "${rate.value.toInt()} дн."
            "weeks" -> "${rate.value.toInt()} нед."
            "months" -> "${rate.value.toInt()} мес."
            else -> "$formattedNumber ${rate.unit}"
        }
    }
}
