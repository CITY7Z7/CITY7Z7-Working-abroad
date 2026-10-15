package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.database.entity.DocumentCategory
import com.example.data.database.entity.DocumentEntity
import com.example.data.database.entity.DocumentType
import com.example.domain.expiry.ExpiryCalculator
import com.example.domain.expiry.ExpiryUrgency
import com.example.ui.components.BreadcrumbsHeader
import com.example.ui.viewmodel.DocumentHubViewModel
import java.time.LocalDate

@Composable
fun DocumentHubScreen(
    viewModel: DocumentHubViewModel,
    onBack: (() -> Unit)?,
    onDocumentClick: (documentId: String) -> Unit,
    onShowModeClick: (documentId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allDocs by viewModel.allDocuments.collectAsState()
    val pinnedDocs by viewModel.pinnedDocuments.collectAsState()
    var selectedCategory by remember { mutableStateOf<DocumentCategory?>(null) }

    // SAF Document Picker
    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val contentResolver = context.contentResolver
                var title = "Документ"
                var size = 0L

                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) title = cursor.getString(nameIdx)
                        if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
                    }
                }

                if (size > 25 * 1024 * 1024) {
                    Toast.makeText(context, "Размер файла превышает лимит 25 МБ", Toast.LENGTH_LONG).show()
                    return@rememberLauncherForActivityResult
                }

                val mime = contentResolver.getType(uri) ?: "application/octet-stream"
                val guessedType = guessDocumentType(title, mime)
                val guessedCategory = guessedType.defaultCategory

                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    viewModel.importDocument(
                        title = title,
                        category = guessedCategory,
                        type = guessedType,
                        mime = mime,
                        expiresAt = null,
                        inputStream = inputStream,
                        onSuccess = { doc ->
                            Toast.makeText(context, "Документ зашифрован и сохранён", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка импорта: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val filteredDocs = allDocs.filter { doc ->
        selectedCategory == null || doc.category == selectedCategory
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("document_hub_screen")
    ) {
        BreadcrumbsHeader(
            breadcrumbs = listOf("Документ-хаб", selectedCategory?.titleRu ?: "Все документы"),
            onBack = onBack,
            actions = {
                Button(
                    onClick = {
                        safLauncher.launch(arrayOf(
                            "application/pdf",
                            "image/*",
                            "text/csv",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        ))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("add_document_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Импорт (SAF)")
                }
            }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        BoxContainerCentered {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Category Filter Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("Все (${allDocs.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                    items(DocumentCategory.entries) { cat ->
                        val count = allDocs.count { it.category == cat }
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text("${cat.titleRu} ($count)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                // Documents List
                if (filteredDocs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Нет сохранённых документов",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    safLauncher.launch(arrayOf("application/pdf", "image/*", "text/csv", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                }
                            ) {
                                Text("Добавить первый документ")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredDocs, key = { it.id }) { doc ->
                            DocumentCardItem(
                                document = doc,
                                onClick = { onDocumentClick(doc.id) },
                                onShowModeClick = { onShowModeClick(doc.id) },
                                onTogglePinned = { viewModel.togglePinned(doc.id, doc.isPinned) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentCardItem(
    document: DocumentEntity,
    onClick: () -> Unit,
    onShowModeClick: () -> Unit,
    onTogglePinned: () -> Unit
) {
    val expiryStatus = remember(document.expiresAt) {
        ExpiryCalculator.calculate(document.expiresAt)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("document_card_${document.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${document.type.titleRu} • ${document.category.titleRu}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Expiry Tag
                Text(
                    text = expiryStatus.displayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (expiryStatus.urgency) {
                        ExpiryUrgency.EXPIRED, ExpiryUrgency.EXPIRED_TODAY -> MaterialTheme.colorScheme.error
                        ExpiryUrgency.CRITICAL_30, ExpiryUrgency.WARNING_60 -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.Medium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTogglePinned) {
                    Icon(
                        imageVector = if (document.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Закрепить",
                        tint = if (document.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onShowModeClick,
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Показ", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private fun guessDocumentType(name: String, mime: String): DocumentType {
    val lower = name.lowercase()
    return when {
        lower.contains("passport") || lower.contains("паспорт") -> DocumentType.PASSPORT
        lower.contains("visa") || lower.contains("виза") -> DocumentType.VISA
        lower.contains("permit") || lower.contains("разрешение") -> DocumentType.WORK_PERMIT
        lower.contains("irp") -> DocumentType.IRP
        lower.contains("insurance") || lower.contains("страховка") -> DocumentType.HEALTH_INSURANCE
        lower.contains("diploma") || lower.contains("диплом") || lower.contains("сертификат") -> DocumentType.EDUCATION
        lower.contains("driving") || lower.contains("водительск") || lower.contains("права") -> DocumentType.DRIVING_LICENCE
        lower.contains("id") -> DocumentType.ID_CARD
        else -> DocumentType.OTHER
    }
}
