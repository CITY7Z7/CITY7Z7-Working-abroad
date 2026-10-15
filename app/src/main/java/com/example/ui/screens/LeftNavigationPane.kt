package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.data.database.entity.PersonProfileEntity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val factCard by mainViewModel.currentCountryFactCard.collectAsState()
    val rates by mainViewModel.currentCountryRates.collectAsState()
    val week1Progress by mainViewModel.week1Progress.collectAsState()
    val expiringDocs by mainViewModel.expiringDocuments.collectAsState()
    val profile by mainViewModel.profile.collectAsState()
    val recentNotes by mainViewModel.sidebarNotes.collectAsState()
    val searchQuery by mainViewModel.searchQuery.collectAsState()

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
            onValueChange = { mainViewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_field"),
            placeholder = {
                Text(
                    text = "PPS, налог, аренда, 112… (Ctrl+K)",
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

        Spacer(modifier = Modifier.height(8.dp))

        // Scrollable Body
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 2. Country Selector Chips
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
                        onClick = { mainViewModel.selectCountry(country.id) },
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

            // 3. Key Figures Card
            KeyFiguresCard(country = selectedCountry, rates = rates)

            // 4. Profile & Document Hub Entry
            ProfileSummaryBlock(
                profile = profile,
                onProfileClick = { mainViewModel.select(Selection.Profile) },
                onDocumentsClick = { mainViewModel.select(Selection.Documents()) }
            )

            // 5. Expiring Documents Block ("Требуют внимания")
            if (expiringDocs.isNotEmpty()) {
                AttentionNeededBlock(
                    expiringDocs = expiringDocs,
                    onDocClick = { mainViewModel.select(Selection.Document(it.id)) }
                )
            }

            // 6. "Сегодня" Block: Week 1 Progress
            Week1ProgressBlock(
                completed = week1Progress.first,
                total = week1Progress.second,
                onClick = { mainViewModel.select(Selection.Checklist("ie.w1")) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // 7. Country Sections
            Text(
                text = "Разделы: ${selectedCountry?.nameRu ?: ""}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            sections.forEach { section ->
                NavigationItemRow(
                    title = section.titleRu,
                    isSelected = false,
                    onClick = {
                        // Navigate to section or first article
                        mainViewModel.select(Selection.Country(selectedCountryId))
                    }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // 8. General Tools
            Text(
                text = "Общие инструменты",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            ToolKind.entries.forEach { tool ->
                val isSelected = (currentSelection as? Selection.Tool)?.kind == tool
                NavigationItemRow(
                    title = tool.titleRu,
                    isSelected = isSelected,
                    onClick = { mainViewModel.select(Selection.Tool(tool)) }
                )
            }

            NavigationItemRow(
                title = "QR-визитка (Офлайн)",
                isSelected = currentSelection is Selection.QrCard,
                icon = Icons.Default.QrCode,
                onClick = { mainViewModel.select(Selection.QrCard) }
            )

            NavigationItemRow(
                title = "Журнал отправок",
                isSelected = currentSelection is Selection.ShareLog,
                icon = Icons.Default.Folder,
                onClick = { mainViewModel.select(Selection.ShareLog) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // 9. Notes Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Заметки",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = { mainViewModel.select(Selection.NewNote) },
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Новая заметка", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Создать", style = MaterialTheme.typography.labelSmall)
                }
            }

            recentNotes.take(5).forEach { note ->
                val isSelected = (currentSelection as? Selection.Note)?.id == note.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { mainViewModel.select(Selection.Note(note.id)) }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (note.isPrivate) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Приватная", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (note.isPrivate) "Приватная заметка" else note.title.ifBlank { "Без названия" },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 10. Fixed SOS Button (Only red element)
        Button(
            onClick = { mainViewModel.select(Selection.Sos) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("sos_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = Color.White
            ),
            shape = MaterialTheme.shapes.small
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "SOS Экстренно",
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ЭКСТРЕННО / SOS (112)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
            )
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
                val displayProf = if (profile != null && profile.profession.isNotBlank()) profile.profession else "Документ-хаб"
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
                modifier = Modifier.fillMaxWidth().height(36.dp),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Документ-хаб", style = MaterialTheme.typography.labelSmall)
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
                    text = "Сегодня: Первые 7 дней",
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
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
