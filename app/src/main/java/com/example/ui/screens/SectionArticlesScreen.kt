package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entity.ArticleEntity
import com.example.data.database.entity.ArticleKind
import com.example.data.database.entity.VerificationStatus
import com.example.data.repository.CityRepository
import com.example.ui.components.BreadcrumbsHeader
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

@Composable
fun SectionArticlesScreen(
    countryId: String,
    sectionId: String,
    sectionTitle: String,
    repository: CityRepository,
    onArticleClick: (String) -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val articles by repository.getArticlesBySection(countryId, sectionId).collectAsState(initial = emptyList())

    var showArticleDialog by remember { mutableStateOf(false) }
    var editingArticle by remember { mutableStateOf<ArticleEntity?>(null) }
    var articleToDelete by remember { mutableStateOf<ArticleEntity?>(null) }

    var dialogTitle by remember { mutableStateOf("") }
    var dialogBody by remember { mutableStateOf("") }
    var dialogSourceUrl by remember { mutableStateOf("") }
    var dialogVerifiedAt by remember { mutableStateOf("") }

    fun openCreateDialog() {
        editingArticle = null
        dialogTitle = ""
        dialogBody = ""
        dialogSourceUrl = ""
        dialogVerifiedAt = LocalDate.now().toString()
        showArticleDialog = true
    }

    fun openEditDialog(article: ArticleEntity) {
        editingArticle = article
        dialogTitle = article.titleRu
        dialogBody = article.bodyMd
        dialogSourceUrl = article.sourceIds
        dialogVerifiedAt = article.verifiedAt
        showArticleDialog = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("section_articles_screen")
    ) {
        BreadcrumbsHeader(
            breadcrumbs = listOf("Разделы", sectionTitle),
            onBack = onBack,
            actions = {
                Button(
                    onClick = { openCreateDialog() },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.testTag("add_article_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Новая статья", style = MaterialTheme.typography.labelSmall)
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
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Статьи и инструкции рубрики. Каждая статья привязана к этой стране и рубрике.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

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
                                text = "В этой рубрике пока нет статей",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Нажмите «+ Новая статья», чтобы написать статью в Markdown или вставить текст.",
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(articles, key = { it.id }) { article ->
                            ArticleCardItem(
                                article = article,
                                onClick = { onArticleClick(article.id) },
                                onEdit = { openEditDialog(article) },
                                onDelete = { articleToDelete = article }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog for Adding or Editing an Article
    if (showArticleDialog) {
        val isEditing = editingArticle != null
        AlertDialog(
            onDismissRequest = { showArticleDialog = false },
            title = { Text(if (isEditing) "Редактировать статью" else "Новая статья") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = dialogTitle,
                        onValueChange = { dialogTitle = it },
                        label = { Text("Название статьи *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("article_title_input")
                    )

                    OutlinedTextField(
                        value = dialogBody,
                        onValueChange = { dialogBody = it },
                        label = { Text("Текст статьи (Markdown) *") },
                        minLines = 6,
                        maxLines = 14,
                        modifier = Modifier.fillMaxWidth().testTag("article_body_input")
                    )

                    OutlinedTextField(
                        value = dialogSourceUrl,
                        onValueChange = { dialogSourceUrl = it },
                        label = { Text("Источник (URL или орган власти)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("article_source_input")
                    )

                    OutlinedTextField(
                        value = dialogVerifiedAt,
                        onValueChange = { dialogVerifiedAt = it },
                        label = { Text("Дата «проверено» (ГГГГ-ММ-ДД)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("article_verified_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dialogTitle.isNotBlank() && dialogBody.isNotBlank()) {
                            scope.launch {
                                val targetArticle = if (isEditing) {
                                    editingArticle!!.copy(
                                        titleRu = dialogTitle.trim(),
                                        bodyMd = dialogBody.trim(),
                                        sourceIds = dialogSourceUrl.trim(),
                                        verifiedAt = dialogVerifiedAt.trim().ifBlank { LocalDate.now().toString() }
                                    )
                                } else {
                                    val safeSectionPart = sectionId.substringAfterLast(".").ifBlank { "sec" }
                                    ArticleEntity(
                                        id = "$countryId.$safeSectionPart.${UUID.randomUUID().toString().take(8)}",
                                        countryId = countryId,
                                        sectionId = sectionId,
                                        kind = ArticleKind.GUIDE,
                                        titleRu = dialogTitle.trim(),
                                        subtitle = "Пользовательская статья",
                                        bodyMd = dialogBody.trim(),
                                        keywords = "${dialogTitle.trim()} $countryId",
                                        applicableTo = "ANY",
                                        sourceIds = dialogSourceUrl.trim(),
                                        verifiedAt = dialogVerifiedAt.trim().ifBlank { LocalDate.now().toString() },
                                        status = VerificationStatus.VERIFIED,
                                        sortOrder = 50
                                    )
                                }
                                repository.insertArticle(targetArticle)
                                showArticleDialog = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_save_article")
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showArticleDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Delete Article Confirmation Dialog
    if (articleToDelete != null) {
        val toDelete = articleToDelete!!
        AlertDialog(
            onDismissRequest = { articleToDelete = null },
            title = { Text("Удалить статью?") },
            text = { Text("Вы действительно хотите удалить статью «${toDelete.titleRu}»? Это действие необратимо.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.deleteArticle(toDelete.id)
                            articleToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_article")
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { articleToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun ArticleCardItem(
    article: ArticleEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("article_card_${article.id}"),
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
                imageVector = Icons.Default.Article,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.titleRu,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (article.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = article.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Проверено: ${article.verifiedAt}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (article.sourceIds.isNotBlank()) {
                        Text(
                            text = "• Источник: ${article.sourceIds}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp).testTag("edit_article_${article.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Редактировать",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).testTag("delete_article_${article.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Открыть",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
