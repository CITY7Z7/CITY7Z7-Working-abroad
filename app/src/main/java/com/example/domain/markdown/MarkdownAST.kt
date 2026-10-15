package com.example.domain.markdown

sealed interface MarkdownNode {
    data class Heading(val level: Int, val text: String) : MarkdownNode
    data class Paragraph(val inlines: List<InlineNode>) : MarkdownNode
    data class ListItem(val isOrdered: Boolean, val index: Int, val inlines: List<InlineNode>) : MarkdownNode
    data class ChecklistItem(val isChecked: Boolean, val originalLineIndex: Int, val inlines: List<InlineNode>) : MarkdownNode
    data class Blockquote(val text: String) : MarkdownNode
    data class CodeBlock(val language: String, val code: String) : MarkdownNode
    data class Table(val headers: List<String>, val alignments: List<TableAlignment>, val rows: List<List<String>>) : MarkdownNode
    data object Divider : MarkdownNode
}

enum class TableAlignment {
    LEFT,
    CENTER,
    RIGHT
}

sealed interface InlineNode {
    data class Text(val content: String) : InlineNode
    data class Bold(val content: String) : InlineNode
    data class Italic(val content: String) : InlineNode
    data class InlineCode(val content: String) : InlineNode
    data class Strikethrough(val content: String) : InlineNode
    data class LinkAsText(val label: String, val url: String) : InlineNode
}
