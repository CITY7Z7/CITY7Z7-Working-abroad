package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.data.database.entity.ArticleEntity
import com.example.data.database.entity.ArticleKind
import com.example.data.database.entity.VerificationStatus
import com.example.data.repository.CityRepository
import com.example.domain.importer.KnowledgeParser
import com.example.ui.components.BreadcrumbsHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.UUID

@Composable
fun KnowledgeImportScreen(
    repository: CityRepository,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val countries by repository.allCountries.collectAsState(initial = emptyList())
    var selectedCountryId by remember { mutableStateOf("ie") }

    val sections by repository.getSectionsForCountry(selectedCountryId).collectAsState(initial = emptyList())
    var selectedSectionId by remember { mutableStateOf("") }

    // Update selected section if empty or country changes
    val activeSection = if (selectedSectionId.isNotBlank() && sections.any { it.id == selectedSectionId }) {
        selectedSectionId
    } else {
        sections.firstOrNull()?.id ?: "$selectedCountryId.status"
    }

    var manualTitle by remember { mutableStateOf("") }
    var manualBody by remember { mutableStateOf("") }
    var manualSourceUrl by remember { mutableStateOf("") }
    var manualVerifiedDate by remember { mutableStateOf(LocalDate.now().toString()) }

    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // SAF File Picker (.md, .txt, .json)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                statusMessage = "Чтение и парсинг локального файла..."
                try {
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).readText()
                        } ?: ""
                    }

                    val articles = withContext(Dispatchers.Default) {
                        if (content.trim().startsWith("[") || content.trim().startsWith("{")) {
                            KnowledgeParser.parseJson(content, selectedCountryId)
                        } else {
                            KnowledgeParser.parseMarkdownOrText(content, selectedCountryId, activeSection)
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
            breadcrumbs = listOf("Инструменты", "Импорт базы знаний"),
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
                    text = "Автономное пополнение базы знаний",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "В соответствии с принципами CITY7Z7, все справочники хранятся строго локально на устройстве. " +
                            "Вы можете импортировать статьи из файлов (.md, .txt, .json) или внести проверенную информацию вручную " +
                            "с обязательным указанием первоисточника и даты проверки.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Country & Section selector from Database
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Страна (из базы данных):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            countries.forEach { country ->
                                FilterChip(
                                    selected = selectedCountryId == country.id,
                                    onClick = {
                                        selectedCountryId = country.id
                                        selectedSectionId = ""
                                    },
                                    label = { Text("${country.flagEmoji} ${country.nameRu}") }
                                )
                            }
                        }

                        if (sections.isNotEmpty()) {
                            Text("Рубрика (из базы данных):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                sections.forEach { section ->
                                    FilterChip(
                                        selected = activeSection == section.id,
                                        onClick = { selectedSectionId = section.id },
                                        label = { Text(section.titleRu) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. Local File Import (.md, .txt, .json)
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
                            text = "Выберите локальный файл со структурированными конспектами или JSON-базой. Приложение разберёт заголовки и сохранит записи в базу Room.",
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

                // 2. Manual entry or paste
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("2. Ручной ввод или вставка из буфера", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Вставьте подготовленный текст статьи. Каждая статья сохраняется с обязательной ссылкой на первоисточник и датой верификации.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = manualTitle,
                            onValueChange = { manualTitle = it },
                            label = { Text("Заголовок статьи") },
                            placeholder = { Text("Например: Правила продления IRP в Дублине") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = manualBody,
                            onValueChange = { manualBody = it },
                            label = { Text("Текст статьи (Markdown)") },
                            placeholder = { Text("Вставьте текст статьи с разметкой...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 8
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = manualSourceUrl,
                                onValueChange = { manualSourceUrl = it },
                                label = { Text("Источник (URL / документ)") },
                                placeholder = { Text("https://www.irishimmigration.ie/...") },
                                modifier = Modifier.weight(1.5f),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = manualVerifiedDate,
                                onValueChange = { manualVerifiedDate = it },
                                label = { Text("Проверено") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Button(
                            onClick = {
                                if (manualTitle.isNotBlank() && manualBody.isNotBlank()) {
                                    scope.launch {
                                        isLoading = true
                                        statusMessage = "Сохранение статьи в базу..."
                                        try {
                                            val article = ArticleEntity(
                                                id = "$selectedCountryId.custom.${UUID.randomUUID().toString().take(8)}",
                                                countryId = selectedCountryId,
                                                sectionId = activeSection,
                                                kind = ArticleKind.GUIDE,
                                                titleRu = manualTitle.trim(),
                                                subtitle = "Внесено пользователем",
                                                bodyMd = manualBody.trim(),
                                                keywords = "${manualTitle.trim()} $selectedCountryId",
                                                applicableTo = "ANY",
                                                sourceIds = manualSourceUrl.trim().ifBlank { "manual_entry" },
                                                verifiedAt = manualVerifiedDate.trim().ifBlank { LocalDate.now().toString() },
                                                status = VerificationStatus.VERIFIED,
                                                archived = 0,
                                                sortOrder = 100
                                            )
                                            repository.insertArticle(article)
                                            manualTitle = ""
                                            manualBody = ""
                                            statusMessage = "Материал «${article.titleRu}» успешно добавлен в рубрику!"
                                            Toast.makeText(context, "Статья сохранена!", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            statusMessage = "Ошибка сохранения: ${e.message}"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            },
                            enabled = !isLoading && manualTitle.isNotBlank() && manualBody.isNotBlank()
                        ) {
                            Text("Сохранить статью в справочник")
                        }
                    }
                }

                // Status info
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
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
