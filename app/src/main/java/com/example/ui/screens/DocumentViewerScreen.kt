package com.example.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.BreadcrumbsHeader
import com.example.ui.viewmodel.DocumentHubViewModel
import kotlinx.coroutines.launch

@Composable
fun DocumentViewerScreen(
    documentId: String,
    viewModel: DocumentHubViewModel,
    onBack: () -> Unit,
    onShowModeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val document by viewModel.activeDocument.collectAsState()
    val decryptedBytes by viewModel.activeDecryptedBytes.collectAsState()
    val spreadsheetData by viewModel.activeSpreadsheetData.collectAsState()
    val pdfPageCount by viewModel.activePdfPageCount.collectAsState()

    var currentPdfPage by remember { mutableIntStateOf(0) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(documentId) {
        viewModel.selectDocument(documentId)
        currentPdfPage = 0
    }

    val doc = document

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("document_viewer_screen")
    ) {
        BreadcrumbsHeader(
            breadcrumbs = listOf("Документы", doc?.title ?: "Просмотр"),
            onBack = onBack,
            actions = {
                // Show Mode
                Button(
                    onClick = onShowModeClick,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.testTag("show_mode_button")
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Режим показа")
                }

                // Share
                Button(
                    onClick = { showShareDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.testTag("share_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Поделиться")
                }

                // Delete
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                }
            }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Content Area
        BoxContainerCentered {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (decryptedBytes == null && spreadsheetData == null && pdfPageCount == 0) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Расшифровка документа...", style = MaterialTheme.typography.bodyMedium)
                    }
                } else if (pdfPageCount > 0) {
                    // PDF Renderer View
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            IconButton(
                                onClick = { if (currentPdfPage > 0) currentPdfPage-- },
                                enabled = currentPdfPage > 0
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Предыдущая")
                            }
                            Text(
                                text = "Страница ${currentPdfPage + 1} из $pdfPageCount",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { if (currentPdfPage < pdfPageCount - 1) currentPdfPage++ },
                                enabled = currentPdfPage < pdfPageCount - 1
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Следующая")
                            }
                        }

                        val bitmap = remember(currentPdfPage, pdfPageCount) {
                            viewModel.renderPdfPage(currentPdfPage)
                        }

                        if (bitmap != null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                                    .verticalScroll(rememberScrollState()),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Страница PDF",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                } else if (spreadsheetData != null) {
                    // CSV or XLSX Table
                    val sheet = spreadsheetData!!
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (sheet.isTruncated) {
                            Text(
                                text = "⚠️ Показаны первые 5 000 строк из ${sheet.totalRowCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            LazyColumn {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(8.dp)
                                    ) {
                                        sheet.headers.forEach { header ->
                                            Text(
                                                text = header,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.width(140.dp).padding(horizontal = 4.dp),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                }
                                items(sheet.rows) { row ->
                                    Row(modifier = Modifier.padding(8.dp)) {
                                        row.forEach { cell ->
                                            Text(
                                                text = cell,
                                                modifier = Modifier.width(140.dp).padding(horizontal = 4.dp),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                } else if (decryptedBytes != null) {
                    // Image Viewer
                    val bitmap = remember(decryptedBytes) {
                        try {
                            BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes!!.size)
                        } catch (_: Exception) {
                            null
                        }
                    }

                    if (bitmap != null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                                .verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Изображение документа",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Формат файла поддерживается только для скачивания/шеринга", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    // Share Options Dialog
    if (showShareDialog && doc != null) {
        var recipientInput by remember { mutableStateOf("") }
        var watermarkEnabled by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Подготовка к отправке") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Отправляйте копии, а не оригиналы. Передавайте документы только тем, кому они нужны для конкретного шага.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = recipientInput,
                        onValueChange = { recipientInput = it },
                        label = { Text("Получатель (для водяного знака)") },
                        placeholder = { Text("например: Агентство X") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = watermarkEnabled,
                            onCheckedChange = { watermarkEnabled = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Наложить защитный водяной знак (рекомендуется)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showShareDialog = false
                        scope.launch {
                            val uri = viewModel.prepareShareUri(doc, recipientInput.ifBlank { null }, watermarkEnabled)
                            if (uri != null) {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = doc.mime
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    clipData = android.content.ClipData.newRawUri("Document", uri)
                                }
                                val chooser = Intent.createChooser(sendIntent, "Поделиться копией документа")
                                context.startActivity(chooser)
                            } else {
                                Toast.makeText(context, "Не удалось подготовить файл к отправке", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Поделиться")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Delete Confirmation
    if (showDeleteConfirm && doc != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить документ?") },
            text = { Text("Зашифрованный файл '${doc.title}' будет безвозвратно удален с устройства.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteDocument(doc.id) {
                            onBack()
                        }
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
