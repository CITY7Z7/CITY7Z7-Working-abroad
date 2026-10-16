package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entity.PersonProfileEntity
import com.example.domain.expiry.ExpiryCalculator
import com.example.domain.expiry.ExpiryUrgency
import com.example.ui.components.KeyFiguresCard
import com.example.ui.navigation.Selection
import com.example.ui.navigation.ToolKind
import com.example.ui.viewmodel.MainViewModel

@Composable
fun LeftNavigationPane(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentSelection by mainViewModel.currentSelection.collectAsState()
    val countries by mainViewModel.countries.collectAsState()
    val selectedCountryId by mainViewModel.selectedCountryId.collectAsState()
    val sections by mainViewModel.currentCountrySections.collectAsState()
    val rates by mainViewModel.currentCountryRates.collectAsState()
    val week1Progress by mainViewModel.week1Progress.collectAsState()
    val expiringDocs by mainViewModel.expiringDocuments.collectAsState()
    val profile by mainViewModel.profile.collectAsState()
    val recentNotes by mainViewModel.sidebarNotes.collectAsState()
    val searchQuery by mainViewModel.searchQuery.collectAsState()

    var isToolsExpanded by rememberSaveable { mutableStateOf(false) }

    val selectedCountry = countries.firstOrNull { it.id == selectedCountryId }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("left_navigation_pane")
    ) {
        // 1. Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { query ->
                mainViewModel.updateSearchQuery(query)
                if (query.isNotBlank()) {
                    mainViewModel.select(Selection.Search(query))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_field"),
            placeholder = {
                Text(
                    text = "Поиск (статьи, заметки, термины)...",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Поиск",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Scrollable Navigation List (Order: 1. Заметки -> 2. Общие инструменты -> 3. Разделы стран)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ==========================================
            // 1-й БЛОК: ЗАМЕТКИ (главный раздел по ТЗ)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📝 ЗАМЕТКИ",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = { mainViewModel.select(Selection.NewNote) },
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Создать", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Создать", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                    }
                }
            }

            NavigationItemRow(
                title = "Все заметки (Сборник)",
                isSelected = currentSelection is Selection.Home || currentSelection is Selection.NotesHub,
                onClick = { mainViewModel.select(Selection.NotesHub) }
            )

            // Recent 5 notes
            recentNotes.take(5).forEach { note ->
                val isSelected = (currentSelection as? Selection.Note)?.id == note.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { mainViewModel.select(Selection.Note(note.id)) }
                        .padding(vertical = 5.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (note.pinned) {
                        Icon(imageVector = Icons.Default.PushPin, contentDescription = "Закреплено", modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (note.isLocked) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Заблокировано", modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (note.isPrivate) "🔒 Приватная" else note.title.ifBlank { "Без названия" },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            // ==========================================
            // 2-й БЛОК: ОБЩИЕ ИНСТРУМЕНТЫ (Сворачиваемая папка, по умолчанию свёрнута)
            // ==========================================
            Surface(
                onClick = { isToolsExpanded = !isToolsExpanded },
                shape = MaterialTheme.shapes.small,
                color = if (isToolsExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tools_folder_toggle")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isToolsExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                            contentDescription = "Папка инструментов",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "🛠 ОБЩИЕ ИНСТРУМЕНТЫ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "10",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isToolsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isToolsExpanded) "Свернуть" else "Развернуть",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isToolsExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 1. Калькулятор налогов
                    NavigationItemRow(
                        title = ToolKind.CALCULATOR.titleRu,
                        isSelected = (currentSelection as? Selection.Tool)?.kind == ToolKind.CALCULATOR,
                        icon = Icons.Default.Calculate,
                        onClick = { mainViewModel.select(Selection.Tool(ToolKind.CALCULATOR)) }
                    )

                    // 2. Разговорник
                    NavigationItemRow(
                        title = ToolKind.PHRASES.titleRu,
                        isSelected = (currentSelection as? Selection.Tool)?.kind == ToolKind.PHRASES,
                        icon = Icons.Default.Translate,
                        onClick = { mainViewModel.select(Selection.Tool(ToolKind.PHRASES)) }
                    )

                    // 3. Словарь терминов
                    NavigationItemRow(
                        title = ToolKind.GLOSSARY.titleRu,
                        isSelected = (currentSelection as? Selection.Tool)?.kind == ToolKind.GLOSSARY,
                        icon = Icons.Default.MenuBook,
                        onClick = { mainViewModel.select(Selection.Tool(ToolKind.GLOSSARY)) }
                    )

                    // 4. Шаблоны писем
                    NavigationItemRow(
                        title = ToolKind.TEMPLATES.titleRu,
                        isSelected = (currentSelection as? Selection.Tool)?.kind == ToolKind.TEMPLATES,
                        icon = Icons.Default.Email,
                        onClick = { mainViewModel.select(Selection.Tool(ToolKind.TEMPLATES)) }
                    )

                    // 5. Что делать, если...
                    NavigationItemRow(
                        title = ToolKind.SITUATIONS.titleRu,
                        isSelected = (currentSelection as? Selection.Tool)?.kind == ToolKind.SITUATIONS,
                        icon = Icons.Default.HelpOutline,
                        onClick = { mainViewModel.select(Selection.Tool(ToolKind.SITUATIONS)) }
                    )

                    // 6. Мои контакты
                    NavigationItemRow(
                        title = ToolKind.CONTACTS.titleRu,
                        isSelected = (currentSelection as? Selection.Tool)?.kind == ToolKind.CONTACTS,
                        icon = Icons.Default.Contacts,
                        onClick = { mainViewModel.select(Selection.Tool(ToolKind.CONTACTS)) }
                    )

                    // 7. Настройки
                    NavigationItemRow(
                        title = ToolKind.SETTINGS.titleRu,
                        isSelected = (currentSelection as? Selection.Tool)?.kind == ToolKind.SETTINGS,
                        icon = Icons.Default.Settings,
                        onClick = { mainViewModel.select(Selection.Tool(ToolKind.SETTINGS)) }
                    )

                    // 8. Импорт базы знаний
                    NavigationItemRow(
                        title = "Импорт базы знаний",
                        isSelected = currentSelection is Selection.KnowledgeImport,
                        icon = Icons.Default.FileOpen,
                        onClick = { mainViewModel.select(Selection.KnowledgeImport) }
                    )

                    // 9. QR-визитка (Быстрая)
                    NavigationItemRow(
                        title = "QR-визитка (Быстрая)",
                        isSelected = currentSelection is Selection.QrCard,
                        icon = Icons.Default.QrCode,
                        onClick = { mainViewModel.select(Selection.QrCard) }
                    )

                    // 10. Журнал отправок
                    NavigationItemRow(
                        title = "Журнал отправок",
                        isSelected = currentSelection is Selection.ShareLog,
                        icon = Icons.Default.Folder,
                        onClick = { mainViewModel.select(Selection.ShareLog) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            // ==========================================
            // 3-й БЛОК: РАЗДЕЛЫ СТРАН
            // ==========================================
            Text(
                text = "🌍 СПРАВОЧНИКИ ПО СТРАНАМ",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Country Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                countries.forEach { country ->
                    val isSelected = country.id == selectedCountryId
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            mainViewModel.selectCountry(country.id)
                            mainViewModel.select(Selection.Country(country.id))
                        },
                        label = {
                            Text(
                                text = "${country.flagEmoji} ${country.nameRu}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Key Figures Card
            KeyFiguresCard(country = selectedCountry, rates = rates)

            // Country Sections List
            Text(
                text = "Разделы: ${selectedCountry?.nameRu ?: ""}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            sections.forEach { section ->
                val isSelected = (currentSelection as? Selection.SectionArticles)?.sectionId == section.id
                NavigationItemRow(
                    title = section.titleRu,
                    isSelected = isSelected,
                    onClick = {
                        mainViewModel.select(
                            Selection.SectionArticles(
                                countryId = selectedCountryId,
                                sectionId = section.id,
                                sectionTitle = section.titleRu
                            )
                        )
                    }
                )
            }

            // Week 1 Progress (if Ireland)
            if (selectedCountryId == "ie") {
                Week1ProgressBlock(
                    completed = week1Progress.first,
                    total = week1Progress.second,
                    onClick = { mainViewModel.select(Selection.Checklist("ie.w1")) }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            // ==========================================
            // 4-й БЛОК: ПРОФИЛЬ И ДОКУМЕНТ-ХАБ (ZERO-TRUST)
            // ==========================================
            ProfileSummaryBlock(
                profile = profile,
                onProfileClick = { mainViewModel.select(Selection.Profile) },
                onDocumentsClick = { mainViewModel.select(Selection.Documents()) }
            )

            if (expiringDocs.isNotEmpty()) {
                AttentionNeededBlock(
                    expiringDocs = expiringDocs,
                    onDocClick = { mainViewModel.select(Selection.Document(it.id)) }
                )
            }
        }
    }
}

@Composable
private fun ProfileSummaryBlock(
    profile: PersonProfileEntity?,
    onProfileClick: () -> Unit,
    onDocumentsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProfileClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Профиль",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val displayName = if (profile != null && profile.fullNameRu.isNotBlank()) profile.fullNameRu else "Мой профиль"
                val displayProf = if (profile != null && profile.profession.isNotBlank()) profile.profession else "Документ-хаб (Защищено)"
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = displayProf,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDocumentsClick,
                modifier = Modifier.fillMaxWidth().height(34.dp),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Документ-хаб (AES-256)", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun AttentionNeededBlock(
    expiringDocs: List<com.example.data.database.entity.DocumentEntity>,
    onDocClick: (com.example.data.database.entity.DocumentEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Внимание",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Требуют внимания",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            expiringDocs.take(3).forEach { doc ->
                val status = ExpiryCalculator.calculate(doc.expiresAt)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDocClick(doc) }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = doc.title,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = status.displayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (status.urgency == ExpiryUrgency.EXPIRED || status.urgency == ExpiryUrgency.EXPIRED_TODAY)
                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun Week1ProgressBlock(
    completed: Int,
    total: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Чек-лист: Первые 7 дней",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$completed из $total",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            val fraction = if (total > 0) completed.toFloat() / total.toFloat() else 0f
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun NavigationItemRow(
    title: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
