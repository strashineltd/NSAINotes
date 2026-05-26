package com.nsai.notes.presentation.notes.components

import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon

@Composable
fun MarkdownPreview(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val markwon = remember { Markwon.create(context) }
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                movementMethod = ScrollingMovementMethod()
                textSize = 16f
                setTextColor(textColor)
            }
        },
        update = { textView ->
            markwon.setMarkdown(textView, markdown)
        },
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    )
}
