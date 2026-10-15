package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.database.entity.ArticleEntity
import com.example.data.database.entity.CountryEntity
import com.example.data.database.entity.RateEntity
import com.example.data.database.entity.VerificationStatus
import com.example.ui.components.BreadcrumbsHeader
import com.example.ui.components.MarkdownRenderer
import com.example.ui.viewmodel.DocumentViewModel

@Composable
fun ArticleDetailScreen(
    articleId: String,
    country: CountryEntity?,
    rates: List<RateEntity>,
    documentViewModel: DocumentViewModel,
    onBack: (() -> Unit)?,
    onCreateNoteFromArticle: (articleId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeArticle by documentViewModel.activeArticle.collectAsState()
    val resolvedBody by documentViewModel.resolvedArticleBody.collectAsState()

    LaunchedEffect(articleId, rates) {
        documentViewModel.loadArticle(articleId, rates)
    }

    val article = activeArticle ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("article_detail_screen")
    ) {
        // Breadcrumbs & Action Bar
        BreadcrumbsHeader(
            breadcrumbs = listOfNotNull(
                country?.nameRu ?: "Справочник",
                article.titleRu
            ),
            onBack = onBack,
            actions = {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Article Text", "${article.titleRu}\n\n$resolvedBody"))
                        Toast.makeText(context, "Статья скопирована в буфер", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("copy_article_button")
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Копировать", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = { onCreateNoteFromArticle(article.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("create_note_button")
                ) {
                    Icon(imageVector = Icons.Default.EditNote, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("В заметку", style = MaterialTheme.typography.labelSmall)
                }
            }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Main Content (max 760dp width, centered)
        BoxContainerCentered {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Block
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = article.titleRu,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (article.subtitle.isNotBlank()) {
                        Text(
                            text = article.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Verification Status Banner
                if (article.status == VerificationStatus.TO_VERIFY) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️ Некоторые данные в этой статье требуют уточнения в официальных источниках.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Markdown Rendered Body
                MarkdownRenderer(markdownText = resolvedBody)

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                // Bottom Metadata & Legal Disclaimer
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Проверено: ${article.verifiedAt}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (article.sourceIds.isNotBlank()) {
                        Text(
                            text = "Источники: ${article.sourceIds}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Справочная информация, а не юридическая или налоговая консультация. Сверяйте с официальными ведомствами.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun BoxContainerCentered(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
        ) {
            content()
        }
    }
}
