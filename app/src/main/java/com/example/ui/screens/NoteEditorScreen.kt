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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
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

    // TextFieldValue state for tracking selection and cursor position
    var bodyTextFieldValue by remember { mutableStateOf(TextFieldValue(editorState.body)) }

    // Synchronize external changes (e.g. initial load, undo, new note)
    LaunchedEffect(editorState.body) {
        if (bodyTextFieldValue.text != editorState.body) {
            val currentSel = bodyTextFieldValue.selection
            val safeStart = currentSel.start.coerceIn(0, editorState.body.length)
            val safeEnd = currentSel.end.coerceIn(0, editorState.body.length)
            bodyTextFieldValue = TextFieldValue(
                text = editorState.body,
                selection = TextRange(safeStart, safeEnd)
            )
        }
    }

    fun insertSnippet(snippet: String) {
        val text = bodyTextFieldValue.text
        val selection = bodyTextFieldValue.selection
        val start = minOf(selection.start, selection.end)
        val end = maxOf(selection.start, selection.end)
        val newText = text.substring(0, start) + snippet + text.substring(end)
        val newCursor = start + snippet.length
        bodyTextFieldValue = TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
        documentViewModel.onBodyChange(newText)
    }

    fun formatSelection(prefix: String, suffix: String = prefix, defaultPlaceholder: String = "") {
        val text = bodyTextFieldValue.text
        val selection = bodyTextFieldValue.selection
        val start = minOf(selection.start, selection.end)
        val end = maxOf(selection.start, selection.end)

        if (start != end) {
            val selected = text.substring(start, end)
            val replacement = prefix + selected + suffix
            val newText = text.substring(0, start) + replacement + text.substring(end)
            val newCursor = start + replacement.length
            bodyTextFieldValue = TextFieldValue(
                text = newText,
                selection = TextRange(newCursor)
            )
            documentViewModel.onBodyChange(newText)
        } else {
            val placeholder = defaultPlaceholder
            val insertion = prefix + placeholder + suffix
            val newText = text.substring(0, start) + insertion + text.substring(start)
            val newSelection = if (placeholder.isNotEmpty()) {
                TextRange(start + prefix.length, start + prefix.length + placeholder.length)
            } else {
                TextRange(start + insertion.length)
            }
            bodyTextFieldValue = TextFieldValue(
                text = newText,
                selection = newSelection
            )
            documentViewModel.onBodyChange(newText)
        }
    }

    fun formatLines(prefix: String, defaultPlaceholder: String = "") {
        val text = bodyTextFieldValue.text
        val selection = bodyTextFieldValue.selection
        val start = minOf(selection.start, selection.end)
        val end = maxOf(selection.start, selection.end)

        if (start != end) {
            val selected = text.substring(start, end)
            val formatted = selected.lines().joinToString("\n") { line ->
                if (line.startsWith(prefix)) line else "$prefix$line"
            }
            val newText = text.substring(0, start) + formatted + text.substring(end)
            bodyTextFieldValue = TextFieldValue(
                text = newText,
                selection = TextRange(start + formatted.length)
            )
            documentViewModel.onBodyChange(newText)
        } else {
            val lastNewline = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0))
            val lineStart = if (lastNewline == -1) 0 else lastNewline + 1
            val currentLine = if (lineStart < text.length) {
                val nextNewline = text.indexOf('\n', lineStart)
                if (nextNewline == -1) text.substring(lineStart) else text.substring(lineStart, nextNewline)
            } else ""

            if (currentLine.isBlank()) {
                val insertion = "$prefix$defaultPlaceholder"
                val newText = text.substring(0, lineStart) + insertion + text.substring(lineStart)
                val newCursor = lineStart + insertion.length
                bodyTextFieldValue = TextFieldValue(
                    text = newText,
                    selection = TextRange(newCursor)
                )
                documentViewModel.onBodyChange(newText)
            } else {
                val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
                val newCursor = (start + prefix.length).coerceAtMost(newText.length)
                bodyTextFieldValue = TextFieldValue(
                    text = newText,
                    selection = TextRange(newCursor)
                )
                documentViewModel.onBodyChange(newText)
            }
        }
    }

    // Android Zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val imgMd = "\n\n![Изображение]($uri)\n\n"
            insertSnippet(imgMd)
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
        // 1. Compact, responsive Top Action Bar
        BreadcrumbsHeader(
            breadcrumbs = listOf("Заметки", if (editorState.isPrivate) "Приватная заметка" else editorState.title.ifBlank { "Без названия" }),
            onBack = onBack,
            actions = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    // Save button
                    Button(
                        onClick = {
                            documentViewModel.saveCurrentNoteImmediately()
                            Toast.makeText(context, "Заметка сохранена", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("save_note_button")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Сохранить", style = MaterialTheme.typography.labelSmall)
                    }

                    // Pin toggle
                    IconButton(
                        onClick = { documentViewModel.togglePinned() },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("pin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Закрепить",
                            tint = if (editorState.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Lock Read-Only toggle
                    IconButton(
                        onClick = {
                            documentViewModel.toggleLocked()
                            val msg = if (!editorState.isLocked) "Заметка заблокирована от изменений" else "Заметка разблокирована"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("lock_button")
                    ) {
                        Icon(
                            imageVector = if (editorState.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Заблокировать от изменений",
                            tint = if (editorState.isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Preview toggle
                    OutlinedButton(
                        onClick = { documentViewModel.togglePreviewMode() },
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("preview_toggle_button")
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
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("copy_text_button")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Копировать текст", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }

                    // Delete
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_note_button")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // 2. Status Bar: Lock warning or Save status & Char count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (editorState.isLocked) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 4.dp),
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
                onFormatSelection = { prefix, suffix, defaultPlaceholder ->
                    formatSelection(prefix, suffix, defaultPlaceholder)
                },
                onFormatLines = { prefix, defaultPlaceholder ->
                    formatLines(prefix, defaultPlaceholder)
                },
                onInsertSnippet = { snippet ->
                    insertSnippet(snippet)
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

        // 4. Editor Body or Preview (strictly weighted so it always fills remaining screen height)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                if (!editorState.isPreviewMode) {
                    // Title Input
                    BasicTextField(
                        value = editorState.title,
                        onValueChange = { documentViewModel.onTitleChange(it) },
                        enabled = !editorState.isLocked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
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
                    Spacer(modifier = Modifier.height(8.dp))

                    // Body Monospace Input with Selection Support
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        BasicTextField(
                            value = bodyTextFieldValue,
                            onValueChange = { newValue ->
                                bodyTextFieldValue = newValue
                                if (newValue.text != editorState.body) {
                                    documentViewModel.onBodyChange(newValue.text)
                                }
                            },
                            enabled = !editorState.isLocked,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("note_body_input"),
                            textStyle = EditorTextStyle.copy(color = MaterialTheme.colorScheme.onBackground),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                if (bodyTextFieldValue.text.isEmpty()) {
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

                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val rawClip = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""

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
                        insertSnippet(pasteResult.processedText)
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
    onFormatSelection: (prefix: String, suffix: String, defaultPlaceholder: String) -> Unit,
    onFormatLines: (prefix: String, defaultPlaceholder: String) -> Unit,
    onInsertSnippet: (String) -> Unit,
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
            IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(34.dp)) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Отмена", modifier = Modifier.size(18.dp))
            }

            // Headings
            ToolbarButton(label = "H1") { onFormatLines("# ", "Заголовок 1") }
            ToolbarButton(label = "H2") { onFormatLines("## ", "Заголовок 2") }
            ToolbarButton(label = "H3") { onFormatLines("### ", "Заголовок 3") }

            // Bold
            IconButton(onClick = { onFormatSelection("**", "**", "жирный текст") }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.FormatBold, contentDescription = "Жирный", modifier = Modifier.size(18.dp))
            }

            // Italic
            IconButton(onClick = { onFormatSelection("*", "*", "курсивный текст") }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.FormatItalic, contentDescription = "Курсив", modifier = Modifier.size(18.dp))
            }

            // Checklist
            IconButton(onClick = { onFormatLines("- [ ] ", "Новый пункт") }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Checklist, contentDescription = "Чек-лист", modifier = Modifier.size(18.dp))
            }

            // Bullet list
            IconButton(onClick = { onFormatLines("- ", "Пункт списка") }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.FormatListBulleted, contentDescription = "Список", modifier = Modifier.size(18.dp))
            }

            // Numbered list
            IconButton(onClick = { onFormatLines("1. ", "Первый пункт") }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.FormatListNumbered, contentDescription = "Нумерованный", modifier = Modifier.size(18.dp))
            }

            // Quote
            IconButton(onClick = { onFormatLines("> ", "Важная цитата") }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.FormatQuote, contentDescription = "Цитата", modifier = Modifier.size(18.dp))
            }

            // Table
            IconButton(
                onClick = {
                    onInsertSnippet(
                        "\n| Параметр | Значение |\n|---|---|\n| Ставка | 14.15 € |\n| Налог | 20% |\n"
                    )
                },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Default.TableChart, contentDescription = "Таблица", modifier = Modifier.size(18.dp))
            }

            // Divider
            IconButton(onClick = { onInsertSnippet("\n---\n") }, modifier = Modifier.size(34.dp)) {
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
            IconButton(onClick = onSmartPaste, modifier = Modifier.size(34.dp)) {
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
