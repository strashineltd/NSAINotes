package com.nsai.notes.data.local.multimodal

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class OCRResult(val fullText: String, val blocks: List<TextBlock> = emptyList())
data class TextBlock(val text: String, val confidence: Float? = null)

@Singleton
class OCREngine @Inject constructor() {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(bitmap: Bitmap): OCRResult = withContext(Dispatchers.IO) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val result = recognizer.process(inputImage).await()
        OCRResult(fullText = result.text, blocks = result.textBlocks.map { TextBlock(it.text) })
    }
}
