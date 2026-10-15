package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.normalizer.PasteMode
import com.example.domain.normalizer.PasteNormalizer
import com.example.domain.normalizer.PasteResult
import com.example.ui.components.BreadcrumbsHeader
import com.example.ui.components.MarkdownRenderer
import com.example.ui.theme.EditorTextStyle
import com.example.ui.viewmodel.DocumentViewModel
import kotlinx.coroutines.launch

@Composable
fun NoteEditorScreen(
    noteId: Long?,
    documentViewModel: DocumentViewModel,
    onBack: (() -> Unit)?,
    onNoteDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val editorState by documentViewModel.editorState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pasteResultToShow by remember { mutableStateOf<PasteResult?>(null) }
    var selectedPasteMode by remember { mutableStateOf(PasteMode.CLEANED) }

    // Android Zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val imgMd = "\n\n![Изображение]($uri)\n\n"
            val updated = if (editorState.body.isBlank()) imgMd.trimStart() else editorState.body + imgMd
            documentViewModel.onBodyChange(updated)
            Toast.makeText(context, "Изображение добавлено в заметку", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(noteId) {
        if (noteId != null && noteId != 0L) {
            documentViewModel.loadNote(noteId)
        } else {
            documentViewModel.initNewNote()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("note_editor_screen")
    ) {
        // 1. Top Action Bar with all mandatory actions
        BreadcrumbsHeader(
            breadcrumbs = listOf("Заметки", if (editorState.isPrivate) "Приватная заметка" else editorState.title.ifBlank { "Без названия" }),
            onBack = onBack,
            actions = {
                // Save button
                Button(
                    onClick = {
                        documentViewModel.saveCurrentNoteImmediately()
                        Toast.makeText(context, "Заметка сохранена", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.testTag("save_note_button")
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Сохранить", style = MaterialTheme.typography.labelSmall)
                }

                // Pin toggle
                IconButton(
                    onClick = { documentViewModel.togglePinned() },
                    modifier = Modifier.testTag("pin_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Закрепить",
                        tint = if (editorState.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Lock Read-Only toggle
                IconButton(
                    onClick = {
                        documentViewModel.toggleLocked()
                        val msg = if (!editorState.isLocked) "Заметка заблокирована от изменений" else "Заметка разблокирована"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("lock_button")
                ) {
                    Icon(
                        imageVector = if (editorState.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Заблокировать от изменений",
                        tint = if (editorState.isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Preview toggle
                OutlinedButton(
                    onClick = { documentViewModel.togglePreviewMode() },
                    modifier = Modifier.testTag("preview_toggle_button")
                ) {
                    Icon(
                        imageVector = if (editorState.isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (editorState.isPreviewMode) "Редактор" else "Просмотр", style = MaterialTheme.typography.labelSmall)
                }

                // Copy Text
                IconButton(
                    onClick = {
                        val full = buildString {
                            if (editorState.title.isNotBlank()) append(editorState.title).append("\n\n")
                            append(editorState.body)
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Заметка", full)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Текст заметки скопирован", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("copy_text_button")
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Копировать текст", tint = MaterialTheme.colorScheme.primary)
                }

                // Delete
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.testTag("delete_note_button")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                }
            }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // 2. Status Bar: Lock warning or Save status & Char count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (editorState.isLocked) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (editorState.isLocked) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Заблокировано от изменений (только чтение)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = editorState.saveStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (editorState.charLimitExceeded) {
                    Text(
                        text = "Превышен лимит 200 000 символов!",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${editorState.charCount} симв.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 3. Markdown Formatting Toolbar (when not in preview mode and not locked)
        if (!editorState.isPreviewMode && !editorState.isLocked) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            MarkdownToolbar(
                onInsertText = { snippet ->
                    val appended = if (editorState.body.isBlank()) snippet else "${editorState.body}\n$snippet"
                    documentViewModel.onBodyChange(appended)
                },
                onPickImage = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onSmartPaste = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val item = clipboard.primaryClip?.getItemAt(0)
                    val text = item?.text?.toString() ?: ""
                    if (text.isNotBlank()) {
                        pasteResultToShow = PasteNormalizer.process(text, selectedPasteMode)
                    } else {
                        Toast.makeText(context, "Буфер обмена пуст", Toast.LENGTH_SHORT).show()
                    }
                },
                onUndo = { documentViewModel.undo() },
                canUndo = editorState.canUndo
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // 4. Editor Body or Preview
        BoxContainerCentered {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                if (!editorState.isPreviewMode) {
                    // Title Input
                    BasicTextField(
                        value = editorState.title,
                        onValueChange = { documentViewModel.onTitleChange(it) },
                        enabled = !editorState.isLocked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("note_title_input"),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (editorState.title.isEmpty()) {
                                Text(
                                    text = "Заголовок заметки...",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Body Monospace Input
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        BasicTextField(
                            value = editorState.body,
                            onValueChange = { documentViewModel.onBodyChange(it) },
                            enabled = !editorState.isLocked,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("note_body_input"),
                            textStyle = EditorTextStyle.copy(color = MaterialTheme.colorScheme.onBackground),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                if (editorState.body.isEmpty()) {
                                    Text(
                                        text = "Текст в формате Markdown (поддерживаются заголовки, списки, чек-листы - [ ], картинки ![alt](url), таблицы, цитаты)...",
                                        style = EditorTextStyle.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                } else {
                    // Preview Mode with Interactive Checklists and Markdown
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (editorState.title.isNotBlank()) {
                            Text(
                                text = editorState.title,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        }

                        MarkdownRenderer(
                            markdownText = editorState.body,
                            onCheckboxClick = { lineIndex, currentChecked ->
                                if (!editorState.isLocked) {
                                    documentViewModel.toggleCheckboxInPreview(lineIndex, currentChecked)
                                }
                            }
                        )
                    }
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState)
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить заметку?") },
            text = { Text("Заметка будет удалена безвозвратно. Вы сможете отменить удаление в течение 5 секунд через уведомление.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        documentViewModel.deleteCurrentNote {
                            onNoteDeleted()
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Заметка удалена",
                                    actionLabel = "Отмена",
                                    duration = androidx.compose.material3.SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    documentViewModel.restoreRecentlyDeleted()
                                }
                            }
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

    // Smart Paste Normalizer Dialog
    pasteResultToShow?.let { pasteResult ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val rawClip = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""

        AlertDialog(
            onDismissRequest = { pasteResultToShow = null },
            title = { Text("Умная вставка из буфера") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (pasteResult.hasSensitiveData) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Text(
                                text = pasteResult.sensitiveDataWarning ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Text("Выберите режим нормализации:", style = MaterialTheme.typography.labelMedium)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedPasteMode == PasteMode.CLEANED,
                            onClick = {
                                selectedPasteMode = PasteMode.CLEANED
                                pasteResultToShow = PasteNormalizer.process(rawClip, PasteMode.CLEANED)
                            }
                        )
                        Text("Очищенный (по умолчанию)", style = MaterialTheme.typography.bodySmall)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedPasteMode == PasteMode.RAW,
                            onClick = {
                                selectedPasteMode = PasteMode.RAW
                                pasteResultToShow = PasteNormalizer.process(rawClip, PasteMode.RAW)
                            }
                        )
                        Text("Как есть", style = MaterialTheme.typography.bodySmall)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedPasteMode == PasteMode.QUOTE,
                            onClick = {
                                selectedPasteMode = PasteMode.QUOTE
                                pasteResultToShow = PasteNormalizer.process(rawClip, PasteMode.QUOTE)
                            }
                        )
                        Text("Цитатой (> )", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val appended = if (editorState.body.isBlank()) {
                            pasteResult.processedText
                        } else {
                            "${editorState.body}\n\n${pasteResult.processedText}"
                        }
                        documentViewModel.onBodyChange(appended)
                        pasteResultToShow = null
                    }
                ) {
                    Text("Вставить")
                }
            },
            dismissButton = {
                TextButton(onClick = { pasteResultToShow = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun MarkdownToolbar(
    onInsertText: (String) -> Unit,
    onPickImage: () -> Unit,
    onSmartPaste: () -> Unit,
    onUndo: () -> Unit,
    canUndo: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo
            IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Отмена", modifier = Modifier.size(18.dp))
            }

            // Headings
            ToolbarButton(label = "H1") { onInsertText("# Заголовок 1") }
            ToolbarButton(label = "H2") { onInsertText("## Заголовок 2") }
            ToolbarButton(label = "H3") { onInsertText("### Заголовок 3") }

            // Bold
            IconButton(onClick = { onInsertText("**жирный текст**") }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.FormatBold, contentDescription = "Жирный", modifier = Modifier.size(18.dp))
            }

            // Italic
            IconButton(onClick = { onInsertText("*курсивный текст*") }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.FormatItalic, contentDescription = "Курсив", modifier = Modifier.size(18.dp))
            }

            // Checklist
            IconButton(onClick = { onInsertText("- [ ] Новый пункт") }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Checklist, contentDescription = "Чек-лист", modifier = Modifier.size(18.dp))
            }

            // Bullet list
            IconButton(onClick = { onInsertText("- Пункт списка") }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.FormatListBulleted, contentDescription = "Список", modifier = Modifier.size(18.dp))
            }

            // Numbered list
            IconButton(onClick = { onInsertText("1. Первый пункт") }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.FormatListNumbered, contentDescription = "Нумерованный", modifier = Modifier.size(18.dp))
            }

            // Quote
            IconButton(onClick = { onInsertText("> Важная цитата") }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.FormatQuote, contentDescription = "Цитата", modifier = Modifier.size(18.dp))
            }

            // Table
            IconButton(
                onClick = {
                    onInsertText(
                        "| Параметр | Значение |\n|---|---|\n| Ставка | 14.15 € |\n| Налог | 20% |"
                    )
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.TableChart, contentDescription = "Таблица", modifier = Modifier.size(18.dp))
            }

            // Divider
            IconButton(onClick = { onInsertText("---") }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.HorizontalRule, contentDescription = "Разделитель", modifier = Modifier.size(18.dp))
            }

            // Image Picker
            AssistChip(
                onClick = onPickImage,
                label = { Text("Картинка", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )

            // Smart Paste
            IconButton(onClick = onSmartPaste, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ContentPaste, contentDescription = "Вставить", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ToolbarButton(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.height(28.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
        }
    }
}
