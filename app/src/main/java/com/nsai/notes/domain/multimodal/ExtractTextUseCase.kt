package com.nsai.notes.domain.multimodal

import android.graphics.Bitmap
import com.nsai.notes.data.local.multimodal.OCREngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtractTextUseCase @Inject constructor(private val ocrEngine: OCREngine) {
    suspend fun execute(bitmap: Bitmap): String = ocrEngine.recognize(bitmap).fullText
}
