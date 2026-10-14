package ai.vatio.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Renders an agent's reply: paragraphs, headings, bullet and numbered lists,
 * quotes, rules and code blocks, with bold, italic, strikethrough, inline
 * code and links inside them. The rules follow Vatio's web widget, so a
 * reply reads the same in both.
 *
 * The content is untrusted. It only ever becomes styled text (nothing is
 * interpreted as HTML), and a link opens only when it is http(s), mailto or
 * tel; anything else stays plain text.
 */
@Composable
fun Markdown(text: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    val blocks = remember(text, colors, uriHandler) {
        val style = InlineStyle(
            code = SpanStyle(fontFamily = FontFamily.Monospace, background = colors.surfaceContainerHigh),
            link = TextLinkStyles(SpanStyle(color = colors.primary, textDecoration = TextDecoration.Underline)),
            open = uriHandler::openSafely,
        )
        parseBlocks(text, style)
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        blocks.forEach { MarkdownBlock(it) }
    }
}

@Composable
private fun MarkdownBlock(block: Block) {
    val typography = MaterialTheme.typography
    when (block) {
        is Block.Paragraph -> Text(block.text, style = typography.bodyLarge)
        is Block.Heading -> Text(
            block.text,
            style = if (block.level <= 2) typography.titleLarge else typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        is Block.Items -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            block.items.forEach { item ->
                Row(Modifier.padding(start = 20.dp * item.depth)) {
                    Text(item.marker, style = typography.bodyLarge, modifier = Modifier.widthIn(min = 24.dp))
                    Text(item.text, style = typography.bodyLarge, modifier = Modifier.weight(1f))
                }
            }
        }
        is Block.Quote -> Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp)),
            )
            Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                block.blocks.forEach { MarkdownBlock(it) }
            }
        }
        is Block.Code -> Text(
            block.code,
            style = typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            // Code keeps its lines; a long one scrolls sideways instead of wrapping.
            softWrap = false,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
        )
        Block.Rule -> HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

// Blocks

private sealed interface Block {
    class Paragraph(val text: AnnotatedString) : Block
    class Heading(val level: Int, val text: AnnotatedString) : Block
    class Items(val items: List<ListItem>) : Block
    class Quote(val blocks: List<Block>) : Block
    class Code(val code: String) : Block
    data object Rule : Block
}

private class ListItem(val marker: String, val depth: Int, val text: AnnotatedString)

private val LINE_BREAK = Regex("""\r\n?""")
private val FENCE = Regex("""^(\s*)(`{3,}|~{3,})\s*[^\s`]*\s*$""")
private val CLOSING_FENCE = Regex("""^\s*(`{3,}|~{3,})\s*$""")
private val HEADING = Regex("""^ {0,3}(#{1,6})\s+(.+?)\s*#*\s*$""")
private val RULE = Regex("""^ {0,3}([-*_])\s*(?:\1\s*){2,}$""")
private val QUOTE = Regex("""^ {0,3}>\s?""")
private val ITEM = Regex("""^(\s*)([-*+]|\d{1,9}[.)])\s+""")
private val WRAPPED = Regex("""^\s{2,}\S""")

private fun parseBlocks(source: String, style: InlineStyle): List<Block> {
    val lines = source.replace(LINE_BREAK, "\n").split("\n")
    val blocks = mutableListOf<Block>()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]
        val fence = FENCE.matchEntire(line)
        val heading = HEADING.matchEntire(line)

        when {
            fence != null -> {
                val margin = fence.groupValues[1].length
                val marker = fence.groupValues[2]
                val code = mutableListOf<String>()
                i++
                // An unclosed fence runs to the end of the message.
                while (i < lines.size && !closes(lines[i], marker)) {
                    code += lines[i].dropIndent(margin)
                    i++
                }
                i++
                blocks += Block.Code(code.joinToString("\n"))
            }

            heading != null -> {
                blocks += Block.Heading(heading.groupValues[1].length, inline(heading.groupValues[2], style))
                i++
            }

            RULE.matches(line) -> {
                blocks += Block.Rule
                i++
            }

            QUOTE.containsMatchIn(line) -> {
                val quoted = mutableListOf<String>()
                while (i < lines.size && QUOTE.containsMatchIn(lines[i])) {
                    quoted += lines[i].replaceFirst(QUOTE, "")
                    i++
                }
                blocks += Block.Quote(parseBlocks(quoted.joinToString("\n"), style))
            }

            ITEM.containsMatchIn(line) -> {
                val items = mutableListOf<ListItem>()
                val indents = ArrayDeque<Int>()
                // The last number at each depth: "1." on every line still
                // counts 1, 2, 3, as it does in HTML.
                val numbers = mutableMapOf<Int, Int>()

                while (i < lines.size && ITEM.containsMatchIn(lines[i])) {
                    val match = ITEM.find(lines[i])!!
                    val indent = match.groupValues[1].length
                    while (indents.isNotEmpty() && indent < indents.last()) indents.removeLast()
                    if (indents.isEmpty() || indent > indents.last()) indents.addLast(indent)
                    val depth = indents.size - 1
                    numbers.keys.removeAll { it > depth }

                    val text = mutableListOf(lines[i].substring(match.range.last + 1))
                    i++
                    // A wrapped item: indented text that is not the next item
                    // and not a fence the item introduces.
                    while (
                        i < lines.size && WRAPPED.containsMatchIn(lines[i]) &&
                        !ITEM.containsMatchIn(lines[i]) && !FENCE.matches(lines[i])
                    ) {
                        text += lines[i].trim()
                        i++
                    }

                    val bullet = match.groupValues[2]
                    val marker = if (bullet.first().isDigit()) {
                        val number = numbers[depth]?.plus(1) ?: bullet.dropLast(1).toInt()
                        numbers[depth] = number
                        "$number."
                    } else {
                        numbers.remove(depth)
                        BULLETS[depth.coerceAtMost(BULLETS.lastIndex)]
                    }
                    items += ListItem(marker, depth, inline(text.joinToString(" "), style))
                }
                blocks += Block.Items(items)
            }

            line.isBlank() -> i++

            else -> {
                // Nothing above claimed this line, so it is paragraph text
                // whatever it looks like.
                val paragraph = mutableListOf(line)
                i++
                while (i < lines.size && lines[i].isNotBlank() && !startsBlock(lines[i])) {
                    paragraph += lines[i]
                    i++
                }
                // Single line breaks are kept: agents write them on purpose.
                blocks += Block.Paragraph(inline(paragraph.joinToString("\n"), style))
            }
        }
    }
    return blocks
}

private val BULLETS = listOf("•", "◦", "▪")

private fun startsBlock(line: String): Boolean =
    FENCE.matches(line) || HEADING.matches(line) || RULE.matches(line) ||
        QUOTE.containsMatchIn(line) || ITEM.containsMatchIn(line)

// A fence closes with the same character, at least as many times.
private fun closes(line: String, opening: String): Boolean {
    val closing = CLOSING_FENCE.matchEntire(line)?.groupValues?.get(1) ?: return false
    return closing.first() == opening.first() && closing.length >= opening.length
}

// The fence's own indentation is the block's margin, not part of the code.
private fun String.dropIndent(width: Int): String {
    var n = 0
    while (n < width && n < length && (this[n] == ' ' || this[n] == '\t')) n++
    return substring(n)
}

// Inline

private class InlineStyle(val code: SpanStyle, val link: TextLinkStyles, val open: (String) -> Unit)

private class InlineRule(
    pattern: String,
    val isLink: Boolean = false,
    val render: AnnotatedString.Builder.(MatchResult, InlineStyle) -> Unit,
) {
    val regex = Regex(pattern)
}

private val BOLD = SpanStyle(fontWeight = FontWeight.Bold)
private val ITALIC = SpanStyle(fontStyle = FontStyle.Italic)
private val BOLD_ITALIC = SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
private val STRIKETHROUGH = SpanStyle(textDecoration = TextDecoration.LineThrough)

// In order of precedence when two match at the same place. Code spans and
// links come before emphasis, so `a * b` keeps its asterisks and a URL keeps
// its underscores.
private val INLINE_RULES = listOf(
    // A backslash keeps the next character literal: \* is an asterisk.
    InlineRule("""\\([\\`*_{}\[\]()#+\-.!~>|])""") { match, _ -> append(match.groupValues[1]) },
    InlineRule("""(`+)(.+?)\1""") { match, style ->
        withStyle(style.code) { append(match.groupValues[2]) }
    },
    InlineRule("""\[([^\]\n]*)]\(([^)\s]+)(?:\s+"[^"\n]*")?\)""", isLink = true) { match, style ->
        appendLink(url = match.groupValues[2], label = match.groupValues[1], style)
    },
    InlineRule("""(?<![^\s(])(?:https?://|www\.)[^\s<]+""", isLink = true) { match, style ->
        // Trailing punctuation belongs to the sentence, not to the link.
        val url = match.value.trimEnd { it in ".,;:!?)]}'\"" }
        appendLink(url = url, label = url, style)
        append(match.value.substring(url.length))
    },
    InlineRule("""\*\*\*([^\n*]+)\*\*\*""") { match, style ->
        withStyle(BOLD_ITALIC) { appendInline(match.groupValues[1], style) }
    },
    InlineRule("""\*\*([^\n*]+)\*\*""") { match, style ->
        withStyle(BOLD) { appendInline(match.groupValues[1], style) }
    },
    InlineRule("""(?<![\w_])__([^\n_]+)__(?!\w)""") { match, style ->
        withStyle(BOLD) { appendInline(match.groupValues[1], style) }
    },
    InlineRule("""(?<![\w*])\*([^\n*]+)\*(?!\w)""") { match, style ->
        withStyle(ITALIC) { appendInline(match.groupValues[1], style) }
    },
    InlineRule("""(?<![\w_])_([^\n_]+)_(?!\w)""") { match, style ->
        withStyle(ITALIC) { appendInline(match.groupValues[1], style) }
    },
    InlineRule("""~~([^\n~]+)~~""") { match, style ->
        withStyle(STRIKETHROUGH) { appendInline(match.groupValues[1], style) }
    },
)

private val RULES_WITHOUT_LINKS = INLINE_RULES.filterNot { it.isLink }

private fun inline(text: String, style: InlineStyle): AnnotatedString =
    buildAnnotatedString { appendInline(text, style) }

private fun AnnotatedString.Builder.appendInline(text: String, style: InlineStyle, links: Boolean = true) {
    val rules = if (links) INLINE_RULES else RULES_WITHOUT_LINKS
    var index = 0
    while (index < text.length) {
        // The rule that matches earliest wins; on a tie, the one listed first.
        val (rule, match) = rules
            .mapNotNull { rule -> rule.regex.find(text, index)?.let { rule to it } }
            .minByOrNull { (_, match) -> match.range.first }
            ?: break
        append(text, index, match.range.first)
        rule.render(this, match, style)
        index = match.range.last + 1
    }
    append(text, index, text.length)
}

private fun AnnotatedString.Builder.appendLink(url: String, label: String, style: InlineStyle) {
    val href = safeUrl(url)
    if (href == null) {
        appendInline(label, style)
        return
    }
    withLink(LinkAnnotation.Url(href, style.link) { style.open(href) }) {
        // No links inside a link's own label.
        appendInline(label.ifEmpty { href }, style, links = false)
    }
}

// Only places a person can safely be sent, as in the widget. javascript:,
// intent:, file: and relative paths stay plain text.
private fun safeUrl(url: String): String? {
    val value = url.trim()
    val safe = listOf("https://", "http://", "mailto:", "tel:").any { value.startsWith(it, ignoreCase = true) }
    return when {
        safe -> value
        value.startsWith("www.", ignoreCase = true) -> "https://$value"
        else -> null
    }
}
