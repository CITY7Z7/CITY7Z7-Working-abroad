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
    modifier: Modifier = Modifier
) {
    if (country == null) return

    val minWageRate = rates.firstOrNull { it.key.contains("min_wage") }
    val minWageText = if (minWageRate != null) {
        val verifiedTag = if (minWageRate.status.name == "TO_VERIFY") " [уточнить]" else ""
        "${minWageRate.value} €/ч$verifiedTag"
    } else {
        "По коллективным договорам"
    }

    val workIdLabel = when (country.id) {
        "ie" -> "PPS Number"
        "de" -> "Steuer-ID / Anmeldung"
        "nl" -> "BSN (Burgerservicenummer)"
        "be" -> "Numéro national (коммуна)"
        "lu" -> "Matricule (коммуна / CNS)"
        "fi" -> "Henkilötunnus (DVV)"
        "no" -> "D-nummer / Fødselsnummer"
        else -> "Налоговый номер"
    }

    val taxOrgLabel = when (country.id) {
        "ie" -> "Revenue (revenue.ie)"
        "de" -> "Finanzamt (finanzamt.de)"
        "nl" -> "Belastingdienst (belastingdienst.nl)"
        "be" -> "SPF Finances (finances.belgium.be)"
        "lu" -> "ACD (impotsdirects.public.lu)"
        "fi" -> "Vero (vero.fi)"
        "no" -> "Skatteetaten (skatteetaten.no)"
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
