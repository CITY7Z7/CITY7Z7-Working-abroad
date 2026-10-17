package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.database.entity.CountryEntity
import com.example.data.database.entity.RateEntity

@Composable
fun KeyFiguresCard(
    country: CountryEntity?,
    rates: List<RateEntity>,
    modifier: Modifier = Modifier,
    onEditRate: ((RateEntity) -> Unit)? = null
) {
    if (country == null) return

    // Find the adult minimum wage (not youth rates 18/19/u18)
    val adultWageRate = rates.firstOrNull { it.key == "${country.id}.min_wage_adult" && (it.validTo == null || it.validTo >= "2026-01-01") }
        ?: rates.firstOrNull { it.key == "${country.id}.min_wage_adult" }
        ?: rates.firstOrNull { it.key.contains("min_wage_adult") && it.validTo == null }
        ?: rates.firstOrNull { it.key.contains("min_wage") && !it.key.contains("18") && !it.key.contains("19") && !it.key.contains("u18") }

    val minWageText = if (adultWageRate != null && adultWageRate.value > 0.0) {
        val verifiedSuffix = if (adultWageRate.id == "ie.min_wage_adult_2027") {
            " (по Budget 2027)"
        } else if (adultWageRate.status.name == "TO_VERIFY") {
            " (предварительно)"
        } else {
            " (пров. ${adultWageRate.validFrom})"
        }
        "${adultWageRate.value} ${adultWageRate.unit}$verifiedSuffix"
    } else if (adultWageRate != null) {
        "Не задана"
    } else {
        "По коллективным договорам"
    }

    val workIdLabel = when (country.id) {
        "ie" -> "PPS Number"
        "de" -> "Steuer-ID / Anmeldung"
        "nl" -> "BSN (Burgerservicenummer)"
        else -> "Налоговый номер"
    }

    val taxOrgLabel = when (country.id) {
        "ie" -> "Revenue (revenue.ie)"
        "de" -> "Finanzamt (finanzamt.de)"
        "nl" -> "Belastingdienst (belastingdienst.nl)"
        else -> "Налоговая служба"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("key_figures_card"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Ключевые цифры: ${country.nameRu} ${country.flagEmoji}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            FigureRow(label = "Валюта:", value = "${country.currencyCode} (${country.currencySymbol})")
            FigureRow(label = "Минималка:", value = minWageText)
            FigureRow(label = "Рабочий ID:", value = workIdLabel)
            FigureRow(label = "Налоговая:", value = taxOrgLabel)
        }
    }
}

@Composable
private fun FigureRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
