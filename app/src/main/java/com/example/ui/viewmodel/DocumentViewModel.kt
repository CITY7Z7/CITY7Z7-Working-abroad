package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.crypto.KeystoreHelper
import com.example.data.database.entity.ArticleEntity
import com.example.data.database.entity.NoteEntity
import com.example.data.database.entity.RateEntity
import com.example.data.repository.CityRepository
import com.example.domain.resolver.RateResolver
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

data class NoteEditorState(
    val noteId: Long = 0,
    val title: String = "",
    val body: String = "",
    val isPrivate: Boolean = false,
    val isPinned: Boolean = false,
    val articleId: String? = null,
    val isPreviewMode: Boolean = false,
    val saveStatus: String = "Сохранено",
    val canUndo: Boolean = false,
    val isLocked: Boolean = false,
    val charCount: Int = 0,
    val charLimitExceeded: Boolean = false
)

class DocumentViewModel(
    private val repository: CityRepository
) : ViewModel() {

    private val _editorState = MutableStateFlow(NoteEditorState())
    val editorState: StateFlow<NoteEditorState> = _editorState.asStateFlow()

    private val undoHistory = mutableListOf<Pair<String, String>>() // (title, body)
    private var autoSaveJob: Job? = null
    private var recentlyDeletedNote: NoteEntity? = null

    // --- Active Article State ---
    private val _activeArticle = MutableStateFlow<ArticleEntity?>(null)
    val activeArticle: StateFlow<ArticleEntity?> = _activeArticle.asStateFlow()

    private val _resolvedArticleBody = MutableStateFlow("")
    val resolvedArticleBody: StateFlow<String> = _resolvedArticleBody.asStateFlow()

    fun loadArticle(articleId: String, rates: List<RateEntity>) {
        viewModelScope.launch {
            val article = repository.getArticleById(articleId)
            _activeArticle.value = article
            if (article != null) {
                val rateMap = rates.associateBy { it.key }
                _resolvedArticleBody.value = RateResolver.resolveRates(article.bodyMd, rateMap)
            } else {
                _resolvedArticleBody.value = ""
            }
        }
    }

    // --- Note Loading & Editing ---
    fun initNewNote(articleId: String? = null) {
        autoSaveJob?.cancel()
        undoHistory.clear()
        _editorState.value = NoteEditorState(
            noteId = 0,
            title = "",
            body = "",
            isPrivate = false,
            isPinned = false,
            articleId = articleId,
            isPreviewMode = false,
            saveStatus = "Новая заметка"
        )
    }

    fun loadNote(noteId: Long) {
        viewModelScope.launch {
            val note = repository.getNoteById(noteId) ?: return@launch
            autoSaveJob?.cancel()
            undoHistory.clear()

            var title = note.title
            var body = note.body
            var isLocked = false

            if (note.isPrivate && note.cipher != null && note.iv != null) {
                try {
                    val decryptedBytes = KeystoreHelper.decryptBytes(note.cipher, note.iv)
                    val decryptedJson = String(decryptedBytes, StandardCharsets.UTF_8)
                    val parts = decryptedJson.split("\n---C7BODY---\n", limit = 2)
                    title = parts.getOrElse(0) { "" }
                    body = parts.getOrElse(1) { "" }
                } catch (_: Exception) {
                    isLocked = true
                }
            }

            _editorState.value = NoteEditorState(
                noteId = note.id,
                title = title,
                body = body,
                isPrivate = note.isPrivate,
                isPinned = note.pinned,
                articleId = note.articleId,
                isPreviewMode = false,
                saveStatus = "Сохранено",
                isLocked = isLocked,
                charCount = body.length,
                charLimitExceeded = body.length > 200_000
            )
        }
    }

    fun onTitleChange(newTitle: String) {
        pushUndo()
        _editorState.value = _editorState.value.copy(
            title = newTitle,
            saveStatus = "Сохранение...",
            canUndo = undoHistory.isNotEmpty()
        )
        scheduleAutoSave()
    }

    fun onBodyChange(newBody: String) {
        pushUndo()
        val limitExceeded = newBody.length > 200_000
        _editorState.value = _editorState.value.copy(
            body = newBody,
            saveStatus = "Сохранение...",
            charCount = newBody.length,
            charLimitExceeded = limitExceeded,
            canUndo = undoHistory.isNotEmpty()
        )
        scheduleAutoSave()
    }

    fun togglePreviewMode() {
        _editorState.value = _editorState.value.copy(isPreviewMode = !_editorState.value.isPreviewMode)
    }

    fun togglePinned() {
        val newPinned = !_editorState.value.isPinned
        _editorState.value = _editorState.value.copy(isPinned = newPinned)
        saveCurrentNoteImmediately()
    }

    fun togglePrivate() {
        val newPrivate = !_editorState.value.isPrivate
        _editorState.value = _editorState.value.copy(isPrivate = newPrivate)
        saveCurrentNoteImmediately()
    }

    fun toggleCheckboxInPreview(originalLineIndex: Int, currentChecked: Boolean) {
        val lines = _editorState.value.body.lines().toMutableList()
        if (originalLineIndex in lines.indices) {
            val targetLine = lines[originalLineIndex]
            val updatedLine = if (currentChecked) {
                targetLine.replace(Regex("\\[[xX]\\]"), "[ ]")
            } else {
                targetLine.replace(Regex("\\[ \\]"), "[x]")
            }
            lines[originalLineIndex] = updatedLine
            val newBody = lines.joinToString("\n")
            onBodyChange(newBody)
        }
    }

    fun undo() {
        if (undoHistory.isNotEmpty()) {
            val (lastTitle, lastBody) = undoHistory.removeAt(undoHistory.size - 1)
            _editorState.value = _editorState.value.copy(
                title = lastTitle,
                body = lastBody,
                canUndo = undoHistory.isNotEmpty(),
                saveStatus = "Сохранение..."
            )
            scheduleAutoSave()
        }
    }

    private fun pushUndo() {
        if (undoHistory.size >= 50) {
            undoHistory.removeAt(0)
        }
        undoHistory.add(Pair(_editorState.value.title, _editorState.value.body))
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1000)
            saveCurrentNoteImmediately()
        }
    }

    fun saveCurrentNoteImmediately() {
        autoSaveJob?.cancel()
        val state = _editorState.value
        if (state.title.isBlank() && state.body.isBlank()) return

        viewModelScope.launch {
            var finalTitle = state.title
            var finalBody = state.body
            var cipher: ByteArray? = null
            var iv: ByteArray? = null

            if (state.isPrivate) {
                val combined = "${state.title}\n---C7BODY---\n${state.body}".toByteArray(StandardCharsets.UTF_8)
                val encrypted = KeystoreHelper.encryptBytes(combined)
                cipher = encrypted.first
                iv = encrypted.second
                finalTitle = ""
                finalBody = ""
            }

            val note = NoteEntity(
                id = state.noteId,
                title = finalTitle,
                body = finalBody,
                isPrivate = state.isPrivate,
                cipher = cipher,
                iv = iv,
                articleId = state.articleId,
                pinned = state.isPinned
            )

            val newId = repository.saveNote(note)
            _editorState.value = _editorState.value.copy(
                noteId = newId,
                saveStatus = "Сохранено"
            )
        }
    }

    fun deleteCurrentNote(onDeleted: () -> Unit) {
        val id = _editorState.value.noteId
        if (id != 0L) {
            viewModelScope.launch {
                recentlyDeletedNote = repository.getNoteById(id)
                repository.deleteNote(id)
                onDeleted()
            }
        }
    }

    fun restoreRecentlyDeleted() {
        recentlyDeletedNote?.let { note ->
            viewModelScope.launch {
                repository.saveNote(note)
                recentlyDeletedNote = null
            }
        }
    }

    class Factory(private val repository: CityRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DocumentViewModel(repository) as T
        }
    }
}
