package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
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
        // Top Action Bar
        BreadcrumbsHeader(
            breadcrumbs = listOf("Заметки", if (editorState.isPrivate) "Приватная заметка" else editorState.title.ifBlank { "Без названия" }),
            onBack = onBack,
            actions = {
                // Undo
                IconButton(
                    onClick = { documentViewModel.undo() },
                    enabled = editorState.canUndo,
                    modifier = Modifier.testTag("undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Отмена",
                        tint = if (editorState.canUndo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }

                // Smart Paste from clipboard
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val item = clipboard.primaryClip?.getItemAt(0)
                        val text = item?.text?.toString() ?: ""
                        if (text.isNotBlank()) {
                            pasteResultToShow = PasteNormalizer.process(text, selectedPasteMode)
                        } else {
                            Toast.makeText(context, "Буфер обмена пуст", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("paste_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Вставить из буфера",
                        tint = MaterialTheme.colorScheme.primary
                    )
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

                // Private toggle (AES-256)
                IconButton(
                    onClick = { documentViewModel.togglePrivate() },
                    modifier = Modifier.testTag("private_button")
                ) {
                    Icon(
                        imageVector = if (editorState.isPrivate) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Приватная заметка",
                        tint = if (editorState.isPrivate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Preview toggle
                OutlinedButton(
                    onClick = { documentViewModel.togglePreviewMode() },
                    modifier = Modifier.testTag("preview_toggle_button")
                ) {
                    Icon(
                        imageVector = if (editorState.isPreviewMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (editorState.isPreviewMode) "Редактор" else "Просмотр")
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

        // Status Row (Save status, char counter)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = editorState.saveStatus,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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

        // Editor Body or Preview
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
                    Spacer(modifier = Modifier.height(12.dp))

                    // Body Monospace Input
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        BasicTextField(
                            value = editorState.body,
                            onValueChange = { documentViewModel.onBodyChange(it) },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("note_body_input"),
                            textStyle = EditorTextStyle.copy(color = MaterialTheme.colorScheme.onBackground),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                if (editorState.body.isEmpty()) {
                                    Text(
                                        text = "Текст в формате Markdown (поддерживаются заголовки, списки, чек-листы - [ ], таблицы, цитаты)...",
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
                    // Preview Mode with Interactive Checklists
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
                                documentViewModel.toggleCheckboxInPreview(lineIndex, currentChecked)
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
            text = { Text("Заметка будет удалена. Вы сможете отменить удаление в течение 5 секунд.") },
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
