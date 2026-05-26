package com.nsai.notes.data.local.embedding

data class ChunkInput(
    val noteId: Long,
    val index: Int,
    val text: String
)

object TextChunker {

    const val CHUNK_SIZE = 500
    const val OVERLAP_SIZE = 50
    private const val MIN_CHUNK_SIZE = 50

    fun chunk(text: String, noteId: Long): List<ChunkInput> {
        val cleaned = text.trim()
        if (cleaned.isBlank()) return emptyList()
        if (cleaned.length <= CHUNK_SIZE) {
            return listOf(ChunkInput(noteId, 0, cleaned))
        }

        val chunks = mutableListOf<ChunkInput>()
        var start = 0
        while (start < cleaned.length) {
            var end = (start + CHUNK_SIZE).coerceAtMost(cleaned.length)
            if (end < cleaned.length) {
                val breakPoint = findBreakPoint(cleaned, start, end)
                if (breakPoint > start + MIN_CHUNK_SIZE) {
                    end = breakPoint
                }
            }
            val chunkText = cleaned.substring(start, end).trim()
            if (chunkText.isNotBlank()) {
                chunks.add(ChunkInput(noteId, chunks.size, chunkText))
            }
            start = end - OVERLAP_SIZE
            if (start >= cleaned.length) break
        }
        return chunks
    }

    private fun findBreakPoint(text: String, start: Int, end: Int): Int {
        val searchStart = (end - OVERLAP_SIZE).coerceAtLeast(start)
        val paragraphBreak = text.indexOf("\n\n", searchStart)
        if (paragraphBreak in (searchStart + 1) until end) return paragraphBreak + 2
        val sentenceBreak = text.indexOf("。", searchStart)  // Chinese period 。
        if (sentenceBreak in (searchStart + 1) until end) return sentenceBreak + 1
        val englishSentenceBreak = text.indexOf(". ", searchStart)
        if (englishSentenceBreak in (searchStart + 1) until end) return englishSentenceBreak + 2
        return end
    }
}
