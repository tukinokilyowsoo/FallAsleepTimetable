package io.github.fallasleeptimetable.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 极简 Markdown 渲染：只为应用内的法律文本服务。
 *
 * 支持标题、段落、列表、表格、代码块、分隔线、粗体、行内代码与链接。
 * 不引入第三方 Markdown 库，避免为了三个对话框增加依赖。
 */
private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class Bullet(val text: String) : MarkdownBlock
    data class Code(val text: String) : MarkdownBlock
    data class Table(val rows: List<List<String>>) : MarkdownBlock
    data object Rule : MarkdownBlock
    data object Blank : MarkdownBlock
}

@Composable
fun MarkdownDocument(
    text: String,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(text) { parseMarkdown(text) }
    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.titleMedium
                        2 -> MaterialTheme.typography.titleSmall
                        else -> MaterialTheme.typography.labelLarge
                    }
                    Text(
                        text = inlineMarkdown(block.text),
                        style = style.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                }

                is MarkdownBlock.Paragraph -> Text(
                    text = inlineMarkdown(block.text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                is MarkdownBlock.Bullet -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = inlineMarkdown(block.text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                is MarkdownBlock.Code -> Text(
                    text = block.text,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(8.dp),
                )

                is MarkdownBlock.Table -> Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    block.rows.forEachIndexed { index, row ->
                        Text(
                            text = inlineMarkdown(row.joinToString("  ·  ")),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (index == 0) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                },
                            ),
                            color = if (index == 0) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }

                MarkdownBlock.Rule -> HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                )

                MarkdownBlock.Blank -> Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

private fun parseMarkdown(text: String): List<MarkdownBlock> {
    val lines = text
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .split('\n')
    val blocks = mutableListOf<MarkdownBlock>()
    val heading = Regex("^(#{1,6})\\s+(.*)$")
    val rule = Regex("^\\s*([-*_])\\1{2,}\\s*$")
    val bullet = Regex("^\\s*[-*+]\\s+(.*)$")
    val tableSeparator = Regex("^:?-{2,}:?$")

    var index = 0
    while (index < lines.size) {
        val line = lines[index]
        val trimmed = line.trim()
        when {
            trimmed.isEmpty() -> {
                blocks += MarkdownBlock.Blank
                index++
            }

            trimmed.startsWith("```") -> {
                val code = mutableListOf<String>()
                index++
                while (index < lines.size && !lines[index].trim().startsWith("```")) {
                    code += lines[index]
                    index++
                }
                if (index < lines.size) index++
                blocks += MarkdownBlock.Code(code.joinToString("\n"))
            }

            heading.matches(line) -> {
                val match = heading.find(line)!!
                blocks += MarkdownBlock.Heading(
                    level = match.groupValues[1].length,
                    text = match.groupValues[2].trim(),
                )
                index++
            }

            rule.matches(line) -> {
                blocks += MarkdownBlock.Rule
                index++
            }

            trimmed.startsWith("|") -> {
                val rows = mutableListOf<List<String>>()
                while (index < lines.size) {
                    val rowLine = lines[index].trim()
                    if (!rowLine.startsWith("|")) break
                    val cells = rowLine.trim('|').split('|').map { it.trim() }
                    if (!cells.all { tableSeparator.matches(it) }) {
                        rows += cells
                    }
                    index++
                }
                if (rows.isNotEmpty()) blocks += MarkdownBlock.Table(rows)
            }

            bullet.matches(line) -> {
                blocks += MarkdownBlock.Bullet(bullet.find(line)!!.groupValues[1].trim())
                index++
            }

            else -> {
                blocks += MarkdownBlock.Paragraph(trimmed)
                index++
            }
        }
    }
    return blocks
}

private fun inlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var index = 0
    while (index < text.length) {
        when {
            text.startsWith("**", index) -> {
                val end = text.indexOf("**", index + 2)
                if (end > index + 2) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(text.substring(index + 2, end))
                    pop()
                    index = end + 2
                } else {
                    append("**")
                    index += 2
                }
            }

            text[index] == '`' -> {
                val end = text.indexOf('`', index + 1)
                if (end > index + 1) {
                    pushStyle(SpanStyle(fontFamily = FontFamily.Monospace))
                    append(text.substring(index + 1, end))
                    pop()
                    index = end + 1
                } else {
                    append('`')
                    index++
                }
            }

            text[index] == '[' -> {
                val close = text.indexOf(']', index + 1)
                val openParen = if (close >= 0) text.indexOf('(', close + 1) else -1
                val closeParen = if (openParen >= 0) text.indexOf(')', openParen + 1) else -1
                if (close > index && openParen == close + 1 && closeParen > openParen) {
                    append(text.substring(index + 1, close))
                    append(" (")
                    append(text.substring(openParen + 1, closeParen))
                    append(")")
                    index = closeParen + 1
                } else {
                    append(text[index])
                    index++
                }
            }

            else -> {
                append(text[index])
                index++
            }
        }
    }
}
