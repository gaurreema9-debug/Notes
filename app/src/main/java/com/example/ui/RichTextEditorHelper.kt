package com.example.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

enum class EditorTextAlign {
    LEFT,
    CENTER,
    RIGHT;

    fun toComposeTextAlign(): TextAlign = when (this) {
        LEFT -> TextAlign.Start
        CENTER -> TextAlign.Center
        RIGHT -> TextAlign.End
    }

    fun next(): EditorTextAlign = when (this) {
        LEFT -> CENTER
        CENTER -> RIGHT
        RIGHT -> LEFT
    }
}

enum class InlineStyleType {
    BOLD,
    ITALIC,
    UNDERLINE
}

data class InlineStyleRange(
    val start: Int,
    val end: Int,
    val type: InlineStyleType
)

data class RichTextFormatting(
    val textAlign: EditorTextAlign = EditorTextAlign.LEFT,
    val spans: List<InlineStyleRange> = emptyList()
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("align", textAlign.name)
        val arr = JSONArray()
        for (span in spans) {
            if (span.end > span.start) {
                val obj = JSONObject()
                obj.put("s", span.start)
                obj.put("e", span.end)
                obj.put("t", span.type.name)
                arr.put(obj)
            }
        }
        root.put("spans", arr)
        return root.toString()
    }

    companion object {
        fun fromJson(json: String?): RichTextFormatting {
            if (json.isNullOrBlank()) return RichTextFormatting()
            return try {
                val root = JSONObject(json)
                val alignStr = root.optString("align", EditorTextAlign.LEFT.name)
                val align = try {
                    EditorTextAlign.valueOf(alignStr)
                } catch (e: Exception) {
                    EditorTextAlign.LEFT
                }
                val spansList = mutableListOf<InlineStyleRange>()
                val arr = root.optJSONArray("spans")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val s = obj.optInt("s", 0)
                        val e = obj.optInt("e", 0)
                        val tStr = obj.optString("t", "")
                        val type = try {
                            InlineStyleType.valueOf(tStr)
                        } catch (ex: Exception) {
                            null
                        }
                        if (type != null && e > s && s >= 0) {
                            spansList.add(InlineStyleRange(s, e, type))
                        }
                    }
                }
                RichTextFormatting(textAlign = align, spans = normalizeSpans(spansList))
            } catch (e: Exception) {
                RichTextFormatting()
            }
        }

        fun normalizeSpans(spans: List<InlineStyleRange>, maxLen: Int = Int.MAX_VALUE): List<InlineStyleRange> {
            val result = mutableListOf<InlineStyleRange>()
            for (type in InlineStyleType.entries) {
                val ofType = spans
                    .map {
                        InlineStyleRange(
                            start = it.start.coerceIn(0, maxLen),
                            end = it.end.coerceIn(0, maxLen),
                            type = it.type
                        )
                    }
                    .filter { it.type == type && it.end > it.start }
                    .sortedBy { it.start }

                for (span in ofType) {
                    if (result.isEmpty() || result.last().type != type || result.last().end < span.start) {
                        result.add(span)
                    } else {
                        val last = result.removeAt(result.lastIndex)
                        result.add(last.copy(end = max(last.end, span.end)))
                    }
                }
            }
            return result
        }
    }
}

data class EditorSnapshot(
    val title: String,
    val content: String,
    val selectionStart: Int,
    val selectionEnd: Int,
    val formatting: RichTextFormatting,
    val activeTypingStyles: Set<InlineStyleType> = emptySet()
)

class RichTextVisualTransformation(
    private val formatting: RichTextFormatting
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val builder = AnnotatedString.Builder(raw)
        for (span in formatting.spans) {
            val s = span.start.coerceIn(0, raw.length)
            val e = span.end.coerceIn(0, raw.length)
            if (e > s) {
                val style = when (span.type) {
                    InlineStyleType.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                    InlineStyleType.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                    InlineStyleType.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
                }
                builder.addStyle(style, s, e)
            }
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

object RichTextEditorHelper {

    fun isStyleActiveInSelection(
        formatting: RichTextFormatting,
        selection: TextRange,
        activeTypingStyles: Set<InlineStyleType>,
        type: InlineStyleType
    ): Boolean {
        val selMin = min(selection.start, selection.end)
        val selMax = max(selection.start, selection.end)
        if (selMin == selMax) {
            return activeTypingStyles.contains(type)
        }
        // Check if every character in [selMin, selMax) is covered by spans of `type`
        val relevant = formatting.spans.filter { it.type == type && it.end > selMin && it.start < selMax }
        if (relevant.isEmpty()) return false
        var coveredUpTo = selMin
        for (span in relevant.sortedBy { it.start }) {
            if (span.start > coveredUpTo) return false
            coveredUpTo = max(coveredUpTo, span.end)
        }
        return coveredUpTo >= selMax
    }

    fun toggleInlineStyle(
        formatting: RichTextFormatting,
        selection: TextRange,
        activeTypingStyles: Set<InlineStyleType>,
        textLength: Int,
        type: InlineStyleType
    ): Pair<RichTextFormatting, Set<InlineStyleType>> {
        val selMin = min(selection.start, selection.end).coerceIn(0, textLength)
        val selMax = max(selection.start, selection.end).coerceIn(0, textLength)

        if (selMin == selMax) {
            val newTyping = activeTypingStyles.toMutableSet()
            if (newTyping.contains(type)) {
                newTyping.remove(type)
            } else {
                newTyping.add(type)
            }
            return formatting to newTyping
        }

        val currentlyActive = isStyleActiveInSelection(formatting, selection, activeTypingStyles, type)
        val updatedSpans = mutableListOf<InlineStyleRange>()

        for (span in formatting.spans) {
            if (span.type != type) {
                updatedSpans.add(span)
            } else {
                if (currentlyActive) {
                    // Remove style in [selMin, selMax)
                    if (span.end <= selMin || span.start >= selMax) {
                        updatedSpans.add(span)
                    } else {
                        if (span.start < selMin) {
                            updatedSpans.add(InlineStyleRange(span.start, selMin, type))
                        }
                        if (span.end > selMax) {
                            updatedSpans.add(InlineStyleRange(selMax, span.end, type))
                        }
                    }
                } else {
                    updatedSpans.add(span)
                }
            }
        }

        if (!currentlyActive) {
            updatedSpans.add(InlineStyleRange(selMin, selMax, type))
        }

        val normalized = RichTextFormatting.normalizeSpans(updatedSpans, textLength)
        val newTyping = activeTypingStyles.toMutableSet().apply {
            if (currentlyActive) remove(type) else add(type)
        }
        return formatting.copy(spans = normalized) to newTyping
    }

    /**
     * Adjusts inline spans when text changes from `oldText` to `newText`,
     * and applies `activeTypingStyles` to newly inserted characters.
     */
    fun adjustSpansOnTextChange(
        oldText: String,
        newText: String,
        oldFormatting: RichTextFormatting,
        activeTypingStyles: Set<InlineStyleType>
    ): RichTextFormatting {
        if (oldText == newText) return oldFormatting

        var prefixLen = 0
        val minLen = min(oldText.length, newText.length)
        while (prefixLen < minLen && oldText[prefixLen] == newText[prefixLen]) {
            prefixLen++
        }

        var oldSuffixIdx = oldText.length
        var newSuffixIdx = newText.length
        while (oldSuffixIdx > prefixLen && newSuffixIdx > prefixLen &&
            oldText[oldSuffixIdx - 1] == newText[newSuffixIdx - 1]
        ) {
            oldSuffixIdx--
            newSuffixIdx--
        }

        val removedLen = oldSuffixIdx - prefixLen
        val insertedLen = newSuffixIdx - prefixLen
        val delta = insertedLen - removedLen

        val adjusted = mutableListOf<InlineStyleRange>()
        for (span in oldFormatting.spans) {
            val newStart = when {
                span.start <= prefixLen -> span.start
                span.start >= oldSuffixIdx -> span.start + delta
                else -> prefixLen
            }
            val newEnd = when {
                span.end <= prefixLen -> span.end
                span.end >= oldSuffixIdx -> span.end + delta
                else -> prefixLen
            }
            if (newEnd > newStart) {
                adjusted.add(InlineStyleRange(newStart, newEnd, span.type))
            }
        }

        if (insertedLen > 0) {
            for (style in activeTypingStyles) {
                adjusted.add(InlineStyleRange(prefixLen, prefixLen + insertedLen, style))
            }
        }

        return oldFormatting.copy(
            spans = RichTextFormatting.normalizeSpans(adjusted, newText.length)
        )
    }

    private val bulletPrefixRegex = Regex("^•\\s")
    private val numberedPrefixRegex = Regex("^\\d+\\.\\s")

    fun isBulletListActive(text: String, selection: TextRange): Boolean {
        val lines = getSelectedLineRanges(text, selection)
        if (lines.isEmpty()) return false
        return lines.all { (lineStart, lineEnd) ->
            val line = text.substring(lineStart, lineEnd)
            bulletPrefixRegex.containsMatchIn(line)
        }
    }

    fun isNumberedListActive(text: String, selection: TextRange): Boolean {
        val lines = getSelectedLineRanges(text, selection)
        if (lines.isEmpty()) return false
        return lines.all { (lineStart, lineEnd) ->
            val line = text.substring(lineStart, lineEnd)
            numberedPrefixRegex.containsMatchIn(line)
        }
    }

    fun toggleBulletList(
        value: TextFieldValue,
        formatting: RichTextFormatting,
        activeTypingStyles: Set<InlineStyleType>
    ): Pair<TextFieldValue, RichTextFormatting> {
        val text = value.text
        val selection = value.selection
        val lineRanges = getSelectedLineRanges(text, selection)
        val allHaveBullet = lineRanges.isNotEmpty() && lineRanges.all { (s, e) ->
            bulletPrefixRegex.containsMatchIn(text.substring(s, e))
        }

        val sb = StringBuilder()
        var lastIdx = 0
        var selectionStartShift = 0
        var selectionEndShift = 0
        val selMin = min(selection.start, selection.end)
        val selMax = max(selection.start, selection.end)

        for ((lineStart, lineEnd) in lineRanges) {
            sb.append(text.substring(lastIdx, lineStart))
            val line = text.substring(lineStart, lineEnd)
            val newLine = when {
                allHaveBullet -> line.replaceFirst(bulletPrefixRegex, "")
                numberedPrefixRegex.containsMatchIn(line) ->
                    "• " + line.replaceFirst(numberedPrefixRegex, "")
                else -> "• $line"
            }
            val diff = newLine.length - line.length
            if (lineStart <= selMin) {
                selectionStartShift += diff
            }
            if (lineStart <= selMax) {
                selectionEndShift += diff
            }
            sb.append(newLine)
            lastIdx = lineEnd
        }
        sb.append(text.substring(lastIdx))

        val newText = sb.toString()
        val newStart = (selMin + selectionStartShift).coerceIn(0, newText.length)
        val newEnd = (selMax + selectionEndShift).coerceIn(0, newText.length)
        val newFormatting = adjustSpansOnTextChange(text, newText, formatting, activeTypingStyles)
        return TextFieldValue(
            text = newText,
            selection = TextRange(newStart, newEnd)
        ) to newFormatting
    }

    fun toggleNumberedList(
        value: TextFieldValue,
        formatting: RichTextFormatting,
        activeTypingStyles: Set<InlineStyleType>
    ): Pair<TextFieldValue, RichTextFormatting> {
        val text = value.text
        val selection = value.selection
        val lineRanges = getSelectedLineRanges(text, selection)
        val allHaveNumbers = lineRanges.isNotEmpty() && lineRanges.all { (s, e) ->
            numberedPrefixRegex.containsMatchIn(text.substring(s, e))
        }

        val sb = StringBuilder()
        var lastIdx = 0
        var selectionStartShift = 0
        var selectionEndShift = 0
        val selMin = min(selection.start, selection.end)
        val selMax = max(selection.start, selection.end)

        lineRanges.forEachIndexed { index, (lineStart, lineEnd) ->
            sb.append(text.substring(lastIdx, lineStart))
            val line = text.substring(lineStart, lineEnd)
            val numberPrefix = "${index + 1}. "
            val newLine = when {
                allHaveNumbers -> line.replaceFirst(numberedPrefixRegex, "")
                bulletPrefixRegex.containsMatchIn(line) ->
                    numberPrefix + line.replaceFirst(bulletPrefixRegex, "")
                else -> numberPrefix + line
            }
            val diff = newLine.length - line.length
            if (lineStart <= selMin) {
                selectionStartShift += diff
            }
            if (lineStart <= selMax) {
                selectionEndShift += diff
            }
            sb.append(newLine)
            lastIdx = lineEnd
        }
        sb.append(text.substring(lastIdx))

        val newText = sb.toString()
        val newStart = (selMin + selectionStartShift).coerceIn(0, newText.length)
        val newEnd = (selMax + selectionEndShift).coerceIn(0, newText.length)
        val newFormatting = adjustSpansOnTextChange(text, newText, formatting, activeTypingStyles)
        return TextFieldValue(
            text = newText,
            selection = TextRange(newStart, newEnd)
        ) to newFormatting
    }

    /**
     * Auto-continues bullet or numbered list when the user presses Enter (`\n`).
     */
    fun handleAutoListContinuation(
        oldValue: TextFieldValue,
        newValue: TextFieldValue
    ): TextFieldValue {
        val oldText = oldValue.text
        val newText = newValue.text
        if (newText.length != oldText.length + 1) return newValue
        val cursor = newValue.selection.start
        if (cursor <= 0 || newText[cursor - 1] != '\n') return newValue

        val newlineIndex = cursor - 1
        // Find start of previous line
        val prevLineStart = newText.lastIndexOf('\n', newlineIndex - 1).let { if (it == -1) 0 else it + 1 }
        val prevLine = newText.substring(prevLineStart, newlineIndex)

        if (bulletPrefixRegex.containsMatchIn(prevLine)) {
            // If user pressed enter on an empty bullet line "• ", remove the bullet
            if (prevLine.trim() == "•") {
                val cleaned = newText.removeRange(prevLineStart, cursor)
                return TextFieldValue(
                    text = cleaned,
                    selection = TextRange(prevLineStart)
                )
            }
            val insertion = "• "
            val updated = newText.substring(0, cursor) + insertion + newText.substring(cursor)
            return TextFieldValue(
                text = updated,
                selection = TextRange(cursor + insertion.length)
            )
        }

        val numMatch = numberedPrefixRegex.find(prevLine)
        if (numMatch != null) {
            val prefixStr = numMatch.value
            val currentNum = prefixStr.substringBefore(".").trim().toIntOrNull() ?: 1
            if (prevLine.trim() == "${currentNum}.") {
                val cleaned = newText.removeRange(prevLineStart, cursor)
                return TextFieldValue(
                    text = cleaned,
                    selection = TextRange(prevLineStart)
                )
            }
            val nextPrefix = "${currentNum + 1}. "
            val updated = newText.substring(0, cursor) + nextPrefix + newText.substring(cursor)
            return TextFieldValue(
                text = updated,
                selection = TextRange(cursor + nextPrefix.length)
            )
        }

        return newValue
    }

    private fun getSelectedLineRanges(text: String, selection: TextRange): List<Pair<Int, Int>> {
        if (text.isEmpty()) return listOf(0 to 0)
        val selMin = min(selection.start, selection.end).coerceIn(0, text.length)
        val selMax = max(selection.start, selection.end).coerceIn(0, text.length)

        val firstLineStart = text.lastIndexOf('\n', (selMin - 1).coerceAtLeast(0)).let {
            if (selMin == 0 || it == -1) 0 else it + 1
        }
        val lastLineEnd = text.indexOf('\n', selMax).let {
            if (it == -1) text.length else it
        }

        val ranges = mutableListOf<Pair<Int, Int>>()
        var currentStart = firstLineStart
        while (currentStart <= lastLineEnd) {
            val nextNewline = text.indexOf('\n', currentStart)
            val lineEnd = if (nextNewline == -1 || nextNewline > lastLineEnd) lastLineEnd else nextNewline
            ranges.add(currentStart to lineEnd)
            if (nextNewline == -1 || nextNewline >= lastLineEnd) break
            currentStart = nextNewline + 1
        }
        return ranges
    }
}
