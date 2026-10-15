package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.CityRepository
import com.example.domain.importer.KnowledgeParser
import com.example.ui.components.BreadcrumbsHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets

@Composable
fun KnowledgeImportScreen(
    repository: CityRepository,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedCountry by remember { mutableStateOf("ie") }
    var selectedSection by remember { mutableStateOf("status") }
    var customUrlInput by remember { mutableStateOf("") }
    var directFileUrlInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // SAF File Picker
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                statusMessage = "Чтение и парсинг файла..."
                try {
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).readText()
                        } ?: ""
                    }

                    val articles = withContext(Dispatchers.Default) {
                        if (content.trim().startsWith("[") || content.trim().startsWith("{")) {
                            KnowledgeParser.parseJson(content, selectedCountry)
                        } else {
                            KnowledgeParser.parseMarkdownOrText(content, selectedCountry, selectedSection)
                        }
                    }

                    if (articles.isNotEmpty()) {
                        repository.insertArticles(articles)
                        statusMessage = "Успешно добавлено ${articles.size} материалов в базу данных!"
                        Toast.makeText(context, "Импортировано статей: ${articles.size}", Toast.LENGTH_LONG).show()
                    } else {
                        statusMessage = "В файле не найдено подходящих статей или разделов."
                    }
                } catch (e: Exception) {
                    statusMessage = "Ошибка импорта: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("knowledge_import_screen")
    ) {
        BreadcrumbsHeader(
            breadcrumbs = listOf("Инструменты", "Импорт и веб-парсер знаний"),
            onBack = onBack
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        BoxContainerCentered {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Обновление базы знаний и справочников",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Вы можете загружать текстовые файлы, Markdown-документы или парсить информацию с официальных сайтов прямо в рубрики приложения.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Country target selector
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Целевая страна для добавления:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = selectedCountry == "ie",
                                onClick = { selectedCountry = "ie" },
                                label = { Text("🇮🇪 Ирландия") }
                            )
                            FilterChip(
                                selected = selectedCountry == "de",
                                onClick = { selectedCountry = "de" },
                                label = { Text("🇩🇪 Германия") }
                            )
                            FilterChip(
                                selected = selectedCountry == "nl",
                                onClick = { selectedCountry = "nl" },
                                label = { Text("🇳🇱 Нидерланды") }
                            )
                        }

                        Text("Рубрика:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = selectedSection == "status",
                                onClick = { selectedSection = "status" },
                                label = { Text("Статус и визы") }
                            )
                            FilterChip(
                                selected = selectedSection == "work",
                                onClick = { selectedSection = "work" },
                                label = { Text("Работа и права") }
                            )
                            FilterChip(
                                selected = selectedSection == "tax",
                                onClick = { selectedSection = "tax" },
                                label = { Text("Налоги") }
                            )
                            FilterChip(
                                selected = selectedSection == "housing",
                                onClick = { selectedSection = "housing" },
                                label = { Text("Жильё") }
                            )
                        }
                    }
                }

                // 1. Local File Import
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("1. Загрузка файла (.md, .txt, .json)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Выберите файл на устройстве. Программа автоматически разберёт текст по заголовкам и сохранит статьи в базу.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
                            enabled = !isLoading
                        ) {
                            Text("Выбрать файл на устройстве")
                        }
                    }
                }

                // 2. Web URL Parser
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("2. Встроенный веб-парсер по ссылке", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Введите адрес веб-страницы или выберите официальный источник. Текст страницы будет автоматически очищен от рекламы и сохранён в выбранный раздел страны.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Quick buttons
                        Text("Быстрые ссылки официальных порталов:", style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    customUrlInput = "https://www.citizensinformation.ie/en/moving-country/moving-to-ireland/"
                                    selectedCountry = "ie"
                                    selectedSection = "status"
                                }
                            ) {
                                Text("CitizensInfo (Ирландия)", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    customUrlInput = "https://www.make-it-in-germany.com/en/visa-residence/types"
                                    selectedCountry = "de"
                                    selectedSection = "work"
                                }
                            ) {
                                Text("MakeItInGermany", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        OutlinedTextField(
                            value = customUrlInput,
                            onValueChange = { customUrlInput = it },
                            label = { Text("URL-адрес веб-страницы") },
                            placeholder = { Text("https://example.com/guide") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (customUrlInput.isNotBlank()) {
                                    scope.launch {
                                        isLoading = true
                                        statusMessage = "Загрузка и парсинг веб-страницы..."
                                        val result = withContext(Dispatchers.IO) {
                                            KnowledgeParser.fetchAndParseWebUrl(
                                                customUrlInput.trim(),
                                                selectedCountry,
                                                selectedSection
                                            )
                                        }
                                        if (result.isSuccess) {
                                            val article = result.getOrThrow()
                                            repository.insertArticle(article)
                                            statusMessage = "Материал «${article.titleRu}» успешно сохранен в базу!"
                                            Toast.makeText(context, "Статья сохранена!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            statusMessage = "Не удалось загрузить страницу: ${result.exceptionOrNull()?.message}"
                                        }
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading && customUrlInput.isNotBlank()
                        ) {
                            Text("Спарсить и сохранить в базу")
                        }
                    }
                }

                // 3. Status info
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(statusMessage ?: "Обработка...", style = MaterialTheme.typography.bodyMedium)
                    }
                } else if (statusMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(
                            text = statusMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}
