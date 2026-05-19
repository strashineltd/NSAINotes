package com.nsai.notes.domain.multimodal

import android.graphics.Bitmap
import android.util.Base64
import com.nsai.notes.data.local.multimodal.OCREngine
import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.model.ChatMessage
import com.nsai.notes.domain.repository.AIOptions
import com.nsai.notes.domain.repository.AIService
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DescribeImageUseCase @Inject constructor(
    private val ocrEngine: OCREngine,
    private val aiService: AIService
) {
    suspend fun execute(bitmap: Bitmap, provider: AIProvider): String {
        val ocrText = ocrEngine.recognize(bitmap).fullText
        val base64Image = compressToBase64(bitmap)
        val prompt = buildString {
            append("请详细描述这张图片")
            if (ocrText.isNotBlank()) append("。图片中的文字内容如下：\n$ocrText")
        }
        val response = aiService.chat(provider = provider, messages = listOf(ChatMessage(ChatMessage.Role.USER, prompt)), options = AIOptions(temperature = 0.7f, maxTokens = 1024, imageData = base64Image))
        return response.content
    }

    private fun compressToBase64(bitmap: Bitmap, maxSize: Int = 1024): String {
        val scaled = if (bitmap.width > maxSize || bitmap.height > maxSize) {
            val ratio = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else bitmap
        val os = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, os)
        val result = Base64.encodeToString(os.toByteArray(), Base64.NO_WRAP)
        if (scaled != bitmap) scaled.recycle()
        return result
    }
}
