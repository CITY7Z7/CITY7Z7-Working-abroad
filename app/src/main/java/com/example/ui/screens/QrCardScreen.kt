package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.expiry.ExpiryCalculator
import com.example.domain.vcard.VCardBuilder
import com.example.ui.components.BreadcrumbsHeader
import com.example.ui.components.QrCodeCanvas
import com.example.ui.viewmodel.DocumentHubViewModel
import com.example.ui.viewmodel.QrCardMode

@Composable
fun QrCardScreen(
    viewModel: DocumentHubViewModel,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(QrCardMode.VCARD) }
    var includePhone by remember { mutableStateOf(true) }
    var includeEmail by remember { mutableStateOf(true) }
    var includeProfession by remember { mutableStateOf(true) }

    val profile by viewModel.profile.collectAsState()
    val allDocs by viewModel.allDocuments.collectAsState()

    // Build embedded content based on mode
    val qrContent = remember(mode, profile, allDocs, includePhone, includeEmail, includeProfession) {
        val p = profile
        when (mode) {
            QrCardMode.VCARD -> {
                val contact = VCardBuilder.ContactData(
                    fullNameLatin = p?.fullNameLatin?.ifBlank { p.fullNameRu } ?: "User",
                    profession = if (includeProfession) p?.profession ?: "" else "",
                    phone = if (includePhone) p?.phone ?: "" else "",
                    email = if (includeEmail) p?.email ?: "" else "",
                    country = p?.countryResidenceId ?: ""
                )
                VCardBuilder.buildVCard3(contact)
            }
            QrCardMode.RESUME -> {
                buildString {
                    append("РЕЗЮМЕ\n")
                    append("Имя: ${p?.fullNameLatin?.ifBlank { p?.fullNameRu } ?: "Специалист"}\n")
                    if (includeProfession && !p?.profession.isNullOrBlank()) append("Профессия: ${p?.profession}\n")
                    if (includePhone && !p?.phone.isNullOrBlank()) append("Тел: ${p?.phone}\n")
                    if (includeEmail && !p?.email.isNullOrBlank()) append("Email: ${p?.email}\n")
                    if (!p?.about.isNullOrBlank()) append("О себе: ${p?.about}\n")
                }.take(600)
            }
            QrCardMode.DOCUMENTS_STATUS -> {
                buildString {
                    append("СТАТУС ДОКУМЕНТОВ (${p?.fullNameLatin ?: "Соискатель"}):\n")
                    allDocs.take(8).forEach { doc ->
                        val status = ExpiryCalculator.calculate(doc.expiresAt)
                        append("• ${doc.title}: ${status.displayLabel}\n")
                    }
                }.take(800)
            }
        }
    }

    val matrix = remember(qrContent) {
        viewModel.generateQrMatrix(qrContent, 0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("qr_card_screen")
    ) {
        BreadcrumbsHeader(
            breadcrumbs = listOf("Инструменты", "Офлайн QR-визитка"),
            onBack = onBack
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        BoxContainerCentered {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mode Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterChip(
                        selected = mode == QrCardMode.VCARD,
                        onClick = { mode = QrCardMode.VCARD },
                        label = { Text("Визитка vCard") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = mode == QrCardMode.RESUME,
                        onClick = { mode = QrCardMode.RESUME },
                        label = { Text("Краткое резюме") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = mode == QrCardMode.DOCUMENTS_STATUS,
                        onClick = { mode = QrCardMode.DOCUMENTS_STATUS },
                        label = { Text("Статус документов") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                // QR Code Matrix Canvas
                QrCodeCanvas(matrix = matrix)

                Text(
                    text = "Наведите камеру смартфона для быстрого считывания без интернета",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Privacy Toggles Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Настройки приватности данных в QR-коде",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        PrivacyToggleRow(
                            label = "Включать номер телефона",
                            checked = includePhone,
                            onCheckedChange = { includePhone = it }
                        )

                        PrivacyToggleRow(
                            label = "Включать адрес электронной почты",
                            checked = includeEmail,
                            onCheckedChange = { includeEmail = it }
                        )

                        PrivacyToggleRow(
                            label = "Включать профессию и должность",
                            checked = includeProfession,
                            onCheckedChange = { includeProfession = it }
                        )

                        Text(
                            text = "🔒 Паспортные номера, PPS, IBAN и домашний адрес исключены по умолчанию для предотвращения кражи личных данных.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
