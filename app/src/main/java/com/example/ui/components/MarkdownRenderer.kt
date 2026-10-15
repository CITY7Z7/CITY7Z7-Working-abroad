package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.markdown.InlineNode
import com.example.domain.markdown.MarkdownNode
import com.example.domain.markdown.MarkdownParser
import com.example.domain.markdown.TableAlignment

@Composable
fun MarkdownRenderer(
    markdownText: String,
    modifier: Modifier = Modifier,
    onCheckboxClick: ((lineIndex: Int, currentChecked: Boolean) -> Unit)? = null
) {
    val nodes = MarkdownParser.parse(markdownText)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        nodes.forEach { node ->
            when (node) {
                is MarkdownNode.Heading -> {
                    val style = when (node.level) {
                        1 -> MaterialTheme.typography.headlineLarge
                        2 -> MaterialTheme.typography.headlineMedium
                        else -> MaterialTheme.typography.headlineSmall
                    }
                    Text(
                        text = node.text,
                        style = style,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                is MarkdownNode.Paragraph -> {
                    Text(
                        text = buildAnnotatedInlines(node.inlines),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                is MarkdownNode.ListItem -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        val prefix = if (node.isOrdered) "${node.index}. " else "• "
                        Text(
                            text = prefix,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = buildAnnotatedInlines(node.inlines),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                is MarkdownNode.ChecklistItem -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = node.isChecked,
                            onCheckedChange = {
                                onCheckboxClick?.invoke(node.originalLineIndex, node.isChecked)
                            },
                            enabled = onCheckboxClick != null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = buildAnnotatedInlines(node.inlines),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                is MarkdownNode.Blockquote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(36.dp)
                                .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = node.text,
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is MarkdownNode.CodeBlock -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.shapes.small
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                            .padding(12.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = node.code,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                is MarkdownNode.Table -> {
                    TableBlock(node)
                }
                MarkdownNode.Divider -> {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TableBlock(table: MarkdownNode.Table) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
            .horizontalScroll(scrollState)
            .padding(4.dp)
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 8.dp)
            ) {
                table.headers.forEachIndexed { idx, header ->
                    val align = table.alignments.getOrElse(idx) { TableAlignment.LEFT }
                    Text(
                        text = header,
                        modifier = Modifier
                            .width(160.dp)
                            .padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = when (align) {
                            TableAlignment.LEFT -> TextAlign.Start
                            TableAlignment.CENTER -> TextAlign.Center
                            TableAlignment.RIGHT -> TextAlign.End
                        }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // Data Rows
            table.rows.forEach { row ->
                Row(modifier = Modifier.padding(vertical = 8.dp)) {
                    row.forEachIndexed { idx, cell ->
                        val align = table.alignments.getOrElse(idx) { TableAlignment.LEFT }
                        Text(
                            text = cell,
                            modifier = Modifier
                                .width(160.dp)
                                .padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = when (align) {
                                TableAlignment.LEFT -> TextAlign.Start
                                TableAlignment.CENTER -> TextAlign.Center
                                TableAlignment.RIGHT -> TextAlign.End
                            }
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun buildAnnotatedInlines(inlines: List<InlineNode>) = buildAnnotatedString {
    inlines.forEach { inline ->
        when (inline) {
            is InlineNode.Text -> append(inline.content)
            is InlineNode.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(inline.content)
            }
            is InlineNode.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(inline.content)
            }
            is InlineNode.InlineCode -> withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                append(" ${inline.content} ")
            }
            is InlineNode.Strikethrough -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                append(inline.content)
            }
            is InlineNode.LinkAsText -> {
                // Links are rendered as plain non-clickable text: "label (url)"
                append("${inline.label} (${inline.url})")
            }
        }
    }
}
