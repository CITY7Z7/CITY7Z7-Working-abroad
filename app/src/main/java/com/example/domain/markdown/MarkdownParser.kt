package com.example.domain.markdown

object MarkdownParser {

    fun parse(text: String): List<MarkdownNode> {
        val lines = text.lines()
        val nodes = mutableListOf<MarkdownNode>()
        var i = 0

        while (i < lines.size) {
            val rawLine = lines[i]
            val line = rawLine.trim()

            // 1. Empty lines
            if (line.isEmpty()) {
                i++
                continue
            }

            // 2. Fenced Code Blocks (```)
            if (line.startsWith("```")) {
                val lang = line.removePrefix("```").trim()
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                if (i < lines.size) i++ // Skip closing ```
                nodes.add(MarkdownNode.CodeBlock(lang, codeLines.joinToString("\n")))
                continue
            }

            // 3. Thematic break / Divider (---, ***, ___)
            if (line == "---" || line == "***" || line == "___") {
                nodes.add(MarkdownNode.Divider)
                i++
                continue
            }

            // 3.5 Images (![alt](url))
            val imageMatch = Regex("^!\\[(.*?)\\]\\((.*?)\\)$").matchEntire(line)
            if (imageMatch != null) {
                nodes.add(MarkdownNode.Image(alt = imageMatch.groupValues[1], url = imageMatch.groupValues[2]))
                i++
                continue
            }

            // 4. Headings (#)
            if (line.startsWith("#")) {
                var level = 0
                while (level < line.length && line[level] == '#') {
                    level++
                }
                if (level in 1..6 && (level == line.length || line[level] == ' ')) {
                    val headingText = line.substring(level).trim()
                    nodes.add(MarkdownNode.Heading(level, headingText))
                    i++
                    continue
                }
            }

            // 5. Blockquotes (>)
            if (line.startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    quoteLines.add(lines[i].trim().removePrefix(">").trim())
                    i++
                }
                nodes.add(MarkdownNode.Blockquote(quoteLines.joinToString("\n")))
                continue
            }

            // 6. Checklists (- [ ] or - [x])
            val checkMatch = Regex("^[\\-*]\\s*\\[([ xX])\\]\\s*(.*)$").matchEntire(line)
            if (checkMatch != null) {
                val isChecked = checkMatch.groupValues[1].equals("x", ignoreCase = true)
                val itemText = checkMatch.groupValues[2]
                nodes.add(MarkdownNode.ChecklistItem(isChecked, i, parseInlines(itemText)))
                i++
                continue
            }

            // 7. GFM Tables (| col1 | col2 |)
            if (line.startsWith("|") && line.endsWith("|") && i + 1 < lines.size) {
                val nextLine = lines[i + 1].trim()
                if (nextLine.startsWith("|") && nextLine.endsWith("|") && nextLine.contains("-")) {
                    val headerCols = line.split("|").map { it.trim() }.filterIndexed { idx, _ -> idx != 0 && idx != line.split("|").size - 1 }
                    val alignCols = nextLine.split("|").map { it.trim() }.filterIndexed { idx, _ -> idx != 0 && idx != nextLine.split("|").size - 1 }

                    val alignments = alignCols.map { col ->
                        when {
                            col.startsWith(":") && col.endsWith(":") -> TableAlignment.CENTER
                            col.endsWith(":") -> TableAlignment.RIGHT
                            else -> TableAlignment.LEFT
                        }
                    }

                    val tableRows = mutableListOf<List<String>>()
                    i += 2 // Skip header & alignment
                    while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                        val rowCols = lines[i].split("|").map { it.trim() }
                            .filterIndexed { idx, _ -> idx != 0 && idx != lines[i].split("|").size - 1 }
                        tableRows.add(rowCols)
                        i++
                    }

                    nodes.add(MarkdownNode.Table(headerCols, alignments, tableRows))
                    continue
                }
            }

            // 8. Unordered Lists (-, *)
            if (line.startsWith("- ") || line.startsWith("* ")) {
                val content = line.substring(2).trim()
                nodes.add(MarkdownNode.ListItem(isOrdered = false, index = 0, parseInlines(content)))
                i++
                continue
            }

            // 9. Ordered Lists (1.)
            val orderedMatch = Regex("^(\\d+)\\.\\s+(.*)$").matchEntire(line)
            if (orderedMatch != null) {
                val index = orderedMatch.groupValues[1].toIntOrNull() ?: 1
                val content = orderedMatch.groupValues[2]
                nodes.add(MarkdownNode.ListItem(isOrdered = true, index = index, parseInlines(content)))
                i++
                continue
            }

            // 10. Normal Paragraph
            val paraLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().isNotEmpty() &&
                !lines[i].trim().startsWith("#") &&
                !lines[i].trim().startsWith("```") &&
                !lines[i].trim().startsWith(">") &&
                !lines[i].trim().startsWith("- ") &&
                !lines[i].trim().startsWith("* ") &&
                !Regex("^(\\d+)\\.\\s+").containsMatchIn(lines[i].trim()) &&
                lines[i].trim() != "---"
            ) {
                paraLines.add(lines[i].trim())
                i++
            }
            nodes.add(MarkdownNode.Paragraph(parseInlines(paraLines.joinToString(" "))))
        }

        return nodes
    }

    /**
     * Parses inline formatting: **bold**, *italic*, `code`, ~~strikethrough~~, [label](url).
     * Linear scanning approach.
     */
    fun parseInlines(text: String): List<InlineNode> {
        val result = mutableListOf<InlineNode>()
        var idx = 0
        val len = text.length
        val currentText = StringBuilder()

        fun flushText() {
            if (currentText.isNotEmpty()) {
                result.add(InlineNode.Text(currentText.toString()))
                currentText.clear()
            }
        }

        while (idx < len) {
            // Inline code: `code`
            if (text[idx] == '`') {
                val end = text.indexOf('`', idx + 1)
                if (end != -1) {
                    flushText()
                    result.add(InlineNode.InlineCode(text.substring(idx + 1, end)))
                    idx = end + 1
                    continue
                }
            }

            // Bold: **text**
            if (idx + 1 < len && text[idx] == '*' && text[idx + 1] == '*') {
                val end = text.indexOf("**", idx + 2)
                if (end != -1) {
                    flushText()
                    result.add(InlineNode.Bold(text.substring(idx + 2, end)))
                    idx = end + 2
                    continue
                }
            }

            // Italic: *text*
            if (text[idx] == '*') {
                val end = text.indexOf('*', idx + 1)
                if (end != -1 && end > idx + 1) {
                    flushText()
                    result.add(InlineNode.Italic(text.substring(idx + 1, end)))
                    idx = end + 1
                    continue
                }
            }

            // Strikethrough: ~~text~~
            if (idx + 1 < len && text[idx] == '~' && text[idx + 1] == '~') {
                val end = text.indexOf("~~", idx + 2)
                if (end != -1) {
                    flushText()
                    result.add(InlineNode.Strikethrough(text.substring(idx + 2, end)))
                    idx = end + 2
                    continue
                }
            }

            // Link: [label](url) -> label (url)
            if (text[idx] == '[') {
                val labelEnd = text.indexOf(']', idx + 1)
                if (labelEnd != -1 && labelEnd + 1 < len && text[labelEnd + 1] == '(') {
                    val urlEnd = text.indexOf(')', labelEnd + 2)
                    if (urlEnd != -1) {
                        flushText()
                        val label = text.substring(idx + 1, labelEnd)
                        val url = text.substring(labelEnd + 2, urlEnd)
                        result.add(InlineNode.LinkAsText(label, url))
                        idx = urlEnd + 1
                        continue
                    }
                }
            }

            currentText.append(text[idx])
            idx++
        }

        flushText()
        return result
    }
}
