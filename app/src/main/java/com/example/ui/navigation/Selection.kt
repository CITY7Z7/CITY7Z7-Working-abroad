package com.example.ui.navigation

import com.example.data.database.entity.DocumentCategory

enum class ToolKind(val titleRu: String) {
    CALCULATOR("Калькулятор налогов"),
    PHRASES("Разговорник"),
    GLOSSARY("Словарь терминов"),
    TEMPLATES("Шаблоны писем"),
    SITUATIONS("Что делать, если..."),
    CONTACTS("Мои контакты"),
    SETTINGS("Настройки")
}

sealed interface Selection {
    data object Home : Selection
    data object NavigationMenu : Selection
    data class Country(val id: String) : Selection
    data class Article(val id: String) : Selection
    data class Note(val id: Long) : Selection
    data object NewNote : Selection
    data class Checklist(val id: String) : Selection
    data class Tool(val kind: ToolKind) : Selection
    data object Sos : Selection
    data class Search(val query: String) : Selection

    data object Profile : Selection
    data object NotesHub : Selection
    data class SectionArticles(val countryId: String, val sectionId: String, val sectionTitle: String) : Selection
    data object KnowledgeImport : Selection
    data class Documents(val category: DocumentCategory? = null) : Selection
    data class Document(val id: String) : Selection
    data class ShowMode(val documentId: String) : Selection
    data class ShareBundle(val documentIds: List<String>) : Selection
    data object QrCard : Selection
    data object ShareLog : Selection
}
