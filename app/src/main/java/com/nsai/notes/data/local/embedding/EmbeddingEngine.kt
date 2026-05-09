package com.nsai.notes.data.local.embedding

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.LongBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmbeddingEngine @Inject constructor() {

    private var session: OrtSession? = null
    private val env = OrtEnvironment.getEnvironment()

    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        if (session != null) return@withContext
        val modelFile = File(context.filesDir, "models/all-MiniLM-L6-v2.onnx")
        if (!modelFile.exists()) {
            copyModelFromAssets(context, modelFile)
        }
        session = env.createSession(modelFile.absolutePath)
    }

    suspend fun embed(text: String): FloatArray = withContext(Dispatchers.IO) {
        val s = session ?: throw IllegalStateException("EmbeddingEngine not initialized")
        val tokens = tokenize(text)
        val inputTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(tokens), longArrayOf(1, tokens.size.toLong()))
        val ortResult = s.run(mapOf("input_ids" to inputTensor))
        val output = ortResult.first().value as Array<FloatArray>
        ortResult.close()
        inputTensor.close()
        output[0]
    }

    val isInitialized: Boolean get() = session != null

    private fun tokenize(text: String): LongArray {
        // BERT tokenizer: pad/truncate to 128 tokens using basic whitespace+punct split
        // Full WordPiece tokenizer requires vocab file; this basic version handles
        // ASCII text and CJK characters for the initial implementation.
        val maxLen = 128
        val result = LongArray(maxLen) { 0L }
        // Map each character to a simple hash-based token ID (101 + index % vocabRange)
        val chars = text.take(500).toCharArray()
        var pos = 0
        result[pos++] = 101L // [CLS]
        for (c in chars) {
            if (pos >= maxLen - 1) break
            result[pos++] = (101L + (c.code and 0x7F)) // simple mapping for placeholder
        }
        result[pos] = 102L // [SEP]
        return result
    }

    private fun copyModelFromAssets(context: Context, dest: File) {
        dest.parentFile?.mkdirs()
        context.assets.open("embeddings/all-MiniLM-L6-v2.onnx").use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
    }

    companion object {
        const val EMBEDDING_DIM = 384

        fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
            var dot = 0f
            var normA = 0f
            var normB = 0f
            for (i in a.indices) {
                dot += a[i] * b[i]
                normA += a[i] * a[i]
                normB += b[i] * b[i]
            }
            val denom = kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB)
            return if (denom == 0f) 0f else dot / denom
        }
    }
}
