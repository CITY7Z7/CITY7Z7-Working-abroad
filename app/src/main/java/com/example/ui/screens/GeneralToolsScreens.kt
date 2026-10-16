package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.database.entity.ArticleEntity
import com.example.data.database.entity.ArticleKind
import com.example.data.database.entity.VerificationStatus
import com.example.data.database.entity.UserContactEntity
import com.example.data.repository.CityRepository
import com.example.ui.components.BreadcrumbsHeader
import com.example.ui.components.MarkdownRenderer
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun SimpleArticleKindScreen(
    title: String,
    kind: ArticleKind,
    repository: CityRepository,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val countries by repository.allCountries.collectAsState(initial = emptyList())
    var selectedCountry by remember { mutableStateOf<String?>("ie") }
    val profile by repository.profile.collectAsState(initial = null)
    val articles by repository.getArticlesByKind(kind, selectedCountry).collectAsState(initial = emptyList())

    var showCreateDialog by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val text = context.contentResolver.openInputStream(it)?.use { s -> s.bufferedReader().readText() } ?: ""
                    val country = selectedCountry ?: "ie"
                    val newArticle = ArticleEntity(
                        id = "$country.${kind.name.lowercase()}.${UUID.randomUUID().toString().take(8)}",
                        countryId = country,
                        sectionId = null,
                        kind = kind,
                        titleRu = "Импорт (${if (kind == ArticleKind.GLOSSARY) "термины" else "фразы"})",
                        subtitle = "Загружено из файла",
                        bodyMd = text,
                        keywords = "$title $country",
                        applicableTo = "ANY",
                        sourceIds = "file_import",
                        verifiedAt = java.time.LocalDate.now().toString(),
                        status = VerificationStatus.VERIFIED,
                        archived = 0,
                        sortOrder = 50
                    )
                    repository.insertArticle(newArticle)
                    Toast.makeText(context, "Материал успешно импортирован", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Ошибка импорта: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BreadcrumbsHeader(
            breadcrumbs = listOf("Инструменты", title),
            onBack = onBack,
            actions = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { filePicker.launch("*/*") },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.testTag("import_file_button")
                    ) {
                        Icon(imageVector = Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Файл", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = { showCreateDialog = true },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.testTag("add_item_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            when (kind) {
                                ArticleKind.TEMPLATE -> "+ Новое письмо"
                                ArticleKind.PHRASE_SET -> "+ Новая фраза"
                                ArticleKind.GLOSSARY -> "+ Новый термин"
                                else -> "+ Добавить"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        BoxContainerCentered {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Dynamic country filter row from database
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    countries.forEach { c ->
                        FilterChip(
                            selected = selectedCountry == c.id,
                            onClick = { selectedCountry = c.id },
                            label = { Text("${c.flagEmoji} ${c.nameRu}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                    FilterChip(
                        selected = selectedCountry == null,
                        onClick = { selectedCountry = null },
                        label = { Text("🌍 Все страны") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                if (articles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Материалы для выбранной страны пока не добавлены",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Нажмите «+ Добавить», чтобы вставить нужный текст из интернета.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(articles, key = { it.id }) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.titleRu,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Copy button
                                        IconButton(
                                            onClick = {
                                                val p = profile
                                                val replacedBody = item.bodyMd
                                                    .replace("{{ФИО}}", p?.fullNameLatin?.ifBlank { p?.fullNameRu } ?: "Иван Иванов")
                                                    .replace("{{Телефон}}", p?.phone ?: "+353 00 000 0000")
                                                    .replace("{{Email}}", p?.email ?: "example@mail.com")
                                                    .replace("{{Профессия}}", p?.profession ?: "Специалист")

                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText(item.titleRu, replacedBody)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Текст скопирован с подстановкой профиля!", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Копировать",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                                    MarkdownRenderer(markdownText = item.bodyMd)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog for adding custom template / phrase / glossary
    if (showCreateDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newBody by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    when (kind) {
                        ArticleKind.TEMPLATE -> "Новый шаблон письма"
                        ArticleKind.PHRASE_SET -> "Новые фразы для разговорника"
                        ArticleKind.GLOSSARY -> "Новый термин"
                        else -> "Добавить материал"
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Вставьте или напишите текст. Можно использовать теги {{ФИО}}, {{Телефон}}, {{Email}} для автоподстановки из профиля.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Заголовок / Название") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newBody,
                        onValueChange = { newBody = it },
                        label = { Text("Текст (поддерживает Markdown)") },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank() && newBody.isNotBlank()) {
                            val country = selectedCountry ?: "ie"
                            val article = ArticleEntity(
                                id = "$country.${kind.name.lowercase()}.${UUID.randomUUID().toString().take(8)}",
                                countryId = country,
                                sectionId = "$country.general",
                                kind = kind,
                                titleRu = newTitle.trim(),
                                subtitle = "Добавлено пользователем",
                                bodyMd = newBody.trim(),
                                keywords = "${newTitle.trim()} $country",
                                applicableTo = "ANY",
                                sourceIds = "custom",
                                verifiedAt = java.time.LocalDate.now().toString(),
                                status = VerificationStatus.VERIFIED,
                                archived = 0,
                                sortOrder = 100
                            )
                            scope.launch {
                                repository.insertArticle(article)
                                showCreateDialog = false
                                Toast.makeText(context, "Сохранено!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = newTitle.isNotBlank() && newBody.isNotBlank()
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun ContactsScreen(
    repository: CityRepository,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val contacts by repository.getContacts(null).collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BreadcrumbsHeader(
            breadcrumbs = listOf("Инструменты", "Мои контакты"),
            onBack = onBack,
            actions = {
                Button(
                    onClick = { showAddDialog = true },
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Добавить", style = MaterialTheme.typography.labelSmall)
                }
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        BoxContainerCentered {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (contacts.isEmpty()) {
                    Text(
                        text = "Контакты не добавлены. Нажмите «Добавить», чтобы внести консульство, работодателя или врача.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(contacts, key = { it.id }) { contact ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = contact.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(text = contact.phone, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                                        if (contact.note.isNotBlank()) {
                                            Text(text = contact.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch { repository.deleteContact(contact.id) }
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var labelInput by remember { mutableStateOf("") }
        var phoneInput by remember { mutableStateOf("") }
        var noteInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Новый контакт") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = labelInput,
                        onValueChange = { labelInput = it },
                        label = { Text("Название (Консульство, Врач, Шеф)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Номер телефона") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Заметка (необязательно)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (labelInput.isNotBlank() && phoneInput.isNotBlank()) {
                            scope.launch {
                                repository.saveContact(
                                    UserContactEntity(
                                        id = UUID.randomUUID().toString(),
                                        label = labelInput.trim(),
                                        phone = phoneInput.trim(),
                                        note = noteInput.trim()
                                    )
                                )
                                showAddDialog = false
                            }
                        }
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun ShareLogScreen(
    repository: CityRepository,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val logs by repository.shareLogs.collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BreadcrumbsHeader(
            breadcrumbs = listOf("Инструменты", "Журнал отправок"),
            onBack = onBack
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        BoxContainerCentered {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Записано: подборка передана в системное меню отправки. Приложение не отправляет данные само.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (logs.isEmpty()) {
                    Text(
                        text = "Журнал пуст. Здесь будут отображаться факты передачи копий документов в системное окно «Поделиться».",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logs, key = { it.id }) { log ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = log.documentTitles,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Получатель: ${log.recipientLabel ?: "Не указан"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${dateFormat.format(Date(log.createdAt))} • ${if (log.watermarked) "С водяным знаком" else "Без знака"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch { repository.deleteShareLog(log.id) }
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
