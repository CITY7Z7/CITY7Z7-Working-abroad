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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
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
    val clipboardManager = LocalClipboardManager.current

    val countries by repository.allCountries.collectAsState(initial = emptyList())
    var selectedCountryId by remember { mutableStateOf("ie") }
    val sections by repository.getSectionsForCountry(selectedCountryId).collectAsState(initial = emptyList())
    var selectedSectionId by remember { mutableStateOf("") }

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

    // Preview and Collision Resolution States
    var parsedCandidates by remember { mutableStateOf<List<ArticleEntity>>(emptyList()) }
    var collisionCandidates by remember { mutableStateOf<List<ArticleEntity>>(emptyList()) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var showCollisionDialog by remember { mutableStateOf(false) }
    var collisionStrategy by remember { mutableStateOf("OVERWRITE") } // "OVERWRITE", "SKIP", "KEEP_BOTH"

    fun initiatePreview(rawContent: String) {
        if (rawContent.isBlank()) {
            Toast.makeText(context, "Текст для импорта пуст", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            isLoading = true
            statusMessage = "Разбор и проверка на коллизии..."
            try {
                val articles = withContext(Dispatchers.Default) {
                    if (rawContent.trim().startsWith("[") || rawContent.trim().startsWith("{")) {
                        KnowledgeParser.parseJson(rawContent, selectedCountryId)
                    } else {
                        KnowledgeParser.parseMarkdownOrText(rawContent, selectedCountryId, activeSection)
                    }
                }

                if (articles.isEmpty()) {
                    statusMessage = "Не удалось распознать подходящие статьи или заголовки."
                    Toast.makeText(context, "Статьи не распознаны", Toast.LENGTH_SHORT).show()
                } else {
                    // Check against existing database articles for collisions
                    val existing: List<ArticleEntity> = withContext(Dispatchers.IO) { repository.getAllActiveArticles() }
                    val collisions = articles.filter { cand ->
                        existing.any { it.id == cand.id || it.titleRu.equals(cand.titleRu, ignoreCase = true) }
                    }

                    parsedCandidates = articles
                    collisionCandidates = collisions
                    showPreviewDialog = true
                    statusMessage = "Найдено статей: ${articles.size}. Коллизий: ${collisions.size}"
                }
            } catch (e: Exception) {
                statusMessage = "Ошибка обработки: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // SAF File Picker (.md, .txt, .json)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                statusMessage = "Чтение файла..."
                try {
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).readText()
                        } ?: ""
                    }
                    initiatePreview(content)
                } catch (e: Exception) {
                    statusMessage = "Ошибка чтения файла: ${e.message}"
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
                    text = "Все справочники хранятся строго локально на устройстве (100% офлайн). " +
                            "Импортируйте статьи из файлов (.md, .txt, .json) или вставляйте из буфера обмена. " +
                            "Перед сохранением всегда доступен предпросмотр и диалог разрешения коллизий.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Country & Section selector
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Целевая страна для импорта:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
                                    label = { Text("${country.flagEmoji} ${country.nameRu}") },
                                    modifier = Modifier.testTag("import_country_${country.id}")
                                )
                            }
                        }

                        if (sections.isNotEmpty()) {
                            Text("Целевая рубрика:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
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
                            text = "Выберите локальный файл на устройстве. Приложение откроет предпросмотр распознанных статей с проверкой коллизий.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
                            enabled = !isLoading,
                            modifier = Modifier.testTag("pick_import_file_button")
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Выбрать файл и открыть предпросмотр")
                        }
                    }
                }

                // 2. Manual entry or paste from clipboard
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("2. Вставка из буфера и ручной ввод", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text ?: ""
                                    if (clip.isNotBlank()) {
                                        initiatePreview(clip)
                                    } else {
                                        Toast.makeText(context, "Буфер обмена пуст", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.testTag("bulk_paste_preview_button")
                            ) {
                                Icon(Icons.Default.Preview, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Парсить весь буфер", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Text(
                            text = "Вставьте текст статьи или конспекта. Вы можете заполнить поля по отдельности или вставить готовый блок текста.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = manualTitle,
                                onValueChange = { manualTitle = it },
                                label = { Text("Заголовок статьи") },
                                placeholder = { Text("Например: Правила продления IRP в Дублине") },
                                modifier = Modifier.weight(1f).testTag("manual_title_input"),
                                singleLine = true
                            )
                            OutlinedButton(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text ?: ""
                                    if (clip.isNotBlank()) {
                                        manualTitle = clip.lines().firstOrNull()?.trim()?.removePrefix("#")?.trim() ?: ""
                                        Toast.makeText(context, "Заголовок вставлен из буфера", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text("Вставить", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Текст статьи (Markdown):", style = MaterialTheme.typography.labelMedium)
                                OutlinedButton(
                                    onClick = {
                                        val clip = clipboardManager.getText()?.text ?: ""
                                        if (clip.isNotBlank()) {
                                            manualBody = if (manualBody.isBlank()) clip else "$manualBody\n\n$clip"
                                            Toast.makeText(context, "Текст вставлен из буфера обмена", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.testTag("paste_body_clipboard_button")
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Вставить из буфера", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            OutlinedTextField(
                                value = manualBody,
                                onValueChange = { manualBody = it },
                                placeholder = { Text("Вставьте текст статьи с разметкой...") },
                                modifier = Modifier.fillMaxWidth().testTag("manual_body_input"),
                                minLines = 4,
                                maxLines = 8
                            )
                        }

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

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    if (manualTitle.isNotBlank() && manualBody.isNotBlank()) {
                                        val candidate = ArticleEntity(
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
                                        parsedCandidates = listOf(candidate)
                                        scope.launch {
                                            val existing: List<ArticleEntity> = withContext(Dispatchers.IO) { repository.getAllActiveArticles() }
                                            val collisions = parsedCandidates.filter { cand ->
                                                existing.any { it.titleRu.equals(cand.titleRu, ignoreCase = true) }
                                            }
                                            collisionCandidates = collisions
                                            showPreviewDialog = true
                                        }
                                    }
                                },
                                enabled = !isLoading && manualTitle.isNotBlank() && manualBody.isNotBlank(),
                                modifier = Modifier.testTag("preview_manual_entry_button")
                            ) {
                                Icon(Icons.Default.Preview, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Открыть предпросмотр статьи")
                            }
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

    // ==========================================
    // ДИАЛОГ ПРЕДПРОСМОТРА ИМПОРТА
    // ==========================================
    if (showPreviewDialog && parsedCandidates.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Preview, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Предпросмотр импорта (${parsedCandidates.size} статей)")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (collisionCandidates.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth().testTag("collision_warning_banner")
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Внимание: обнаружено коллизий — ${collisionCandidates.size} шт. " +
                                            "Текущая стратегия: ${when (collisionStrategy) {
                                                "OVERWRITE" -> "Перезаписать"
                                                "SKIP" -> "Пропустить"
                                                else -> "Сохранить обе (копии)"
                                            }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showCollisionDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("open_collision_dialog_button")
                        ) {
                            Text("Настроить разрешение коллизий (${collisionCandidates.size})")
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Коллизий не обнаружено. Все ${parsedCandidates.size} записей новые.", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Text("Список материалов к добавлению:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(parsedCandidates) { item ->
                            val isCollision = collisionCandidates.any { it.titleRu.equals(item.titleRu, ignoreCase = true) || it.id == item.id }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCollision) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(item.titleRu, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        if (isCollision) {
                                            Text("КОЛЛИЗИЯ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text("НОВАЯ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Text(
                                        text = item.bodyMd.take(100).replace("\n", " ") + if (item.bodyMd.length > 100) "..." else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                val resolvedArticles = mutableListOf<ArticleEntity>()
                                val existing: List<ArticleEntity> = withContext(Dispatchers.IO) { repository.getAllActiveArticles() }

                                for (cand in parsedCandidates) {
                                    val matchedExisting = existing.firstOrNull { it.id == cand.id || it.titleRu.equals(cand.titleRu, ignoreCase = true) }
                                    if (matchedExisting != null) {
                                        when (collisionStrategy) {
                                            "OVERWRITE" -> {
                                                resolvedArticles.add(cand.copy(id = matchedExisting.id))
                                            }
                                            "SKIP" -> {
                                                // Do not include candidate
                                            }
                                            "KEEP_BOTH" -> {
                                                resolvedArticles.add(
                                                    cand.copy(
                                                        id = "${cand.id}_copy_${System.currentTimeMillis() % 1000}",
                                                        titleRu = "${cand.titleRu} (копия)"
                                                    )
                                                )
                                            }
                                        }
                                    } else {
                                        resolvedArticles.add(cand)
                                    }
                                }

                                if (resolvedArticles.isNotEmpty()) {
                                    repository.insertArticles(resolvedArticles)
                                    statusMessage = "Успешно импортировано ${resolvedArticles.size} материалов в базу данных!"
                                    Toast.makeText(context, "Импортировано статей: ${resolvedArticles.size}", Toast.LENGTH_LONG).show()
                                } else {
                                    statusMessage = "Все материалы были пропущены согласно правилу коллизий."
                                }
                                showPreviewDialog = false
                                parsedCandidates = emptyList()
                                collisionCandidates = emptyList()
                                manualTitle = ""
                                manualBody = ""
                            } catch (e: Exception) {
                                statusMessage = "Ошибка импорта: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_import_button")
                ) {
                    Text("Подтвердить импорт (${parsedCandidates.size})")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPreviewDialog = false
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    // ==========================================
    // ДИАЛОГ РАЗРЕШЕНИЯ КОЛЛИЗИЙ (COLLISION DIALOG)
    // ==========================================
    if (showCollisionDialog) {
        AlertDialog(
            onDismissRequest = { showCollisionDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Разрешение коллизий при импорте")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "В импортируемом материале обнаружены статьи, которые уже есть в вашей локальной базе (${collisionCandidates.size} совпадений). Выберите желаемое действие:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    FilterChip(
                        selected = collisionStrategy == "OVERWRITE",
                        onClick = { collisionStrategy = "OVERWRITE" },
                        label = { Text("1. Перезаписать существующие (Обновить)") },
                        modifier = Modifier.fillMaxWidth().testTag("strategy_overwrite")
                    )

                    FilterChip(
                        selected = collisionStrategy == "SKIP",
                        onClick = { collisionStrategy = "SKIP" },
                        label = { Text("2. Пропустить дубликаты (Не перезаписывать)") },
                        modifier = Modifier.fillMaxWidth().testTag("strategy_skip")
                    )

                    FilterChip(
                        selected = collisionStrategy == "KEEP_BOTH",
                        onClick = { collisionStrategy = "KEEP_BOTH" },
                        label = { Text("3. Сохранить обе записи (Создать копии)") },
                        modifier = Modifier.fillMaxWidth().testTag("strategy_keep_both")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCollisionDialog = false },
                    modifier = Modifier.testTag("apply_collision_strategy_button")
                ) {
                    Text("Применить")
                }
            }
        )
    }
}
