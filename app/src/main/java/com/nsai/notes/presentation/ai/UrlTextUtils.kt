package com.nsai.notes.presentation.ai

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration

private val urlRegex = Regex("""(https?://[^\s]+|www\.[^\s]+\.[^\s]+)""")

fun buildUrlAnnotatedText(text: String) = buildAnnotatedString {
    var last = 0
    urlRegex.findAll(text).forEach { m ->
        append(text.substring(last, m.range.first))
        pushStringAnnotation("URL", m.value)
        pushStyle(SpanStyle(color = Color(0xFF1A73E8), textDecoration = TextDecoration.Underline))
        append(m.value)
        pop()
        pop()
        last = m.range.last + 1
    }
    append(text.substring(last))
}

fun getUrlAt(text: String, offset: Int): String? =
    buildUrlAnnotatedText(text).getStringAnnotations("URL", offset, offset).firstOrNull()?.item
