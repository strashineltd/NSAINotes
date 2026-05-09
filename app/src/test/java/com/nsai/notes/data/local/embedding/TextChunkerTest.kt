package com.nsai.notes.data.local.embedding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextChunkerTest {

    @Test
    fun `short text produces single chunk`() {
        val chunks = TextChunker.chunk("Hello world", noteId = 1)
        assertEquals(1, chunks.size)
        assertEquals("Hello world", chunks[0].text)
        assertEquals(1, chunks[0].noteId)
        assertEquals(0, chunks[0].index)
    }

    @Test
    fun `text longer than chunk size produces multiple chunks`() {
        val text = "a".repeat(1200)
        val chunks = TextChunker.chunk(text, noteId = 1)
        assertTrue(chunks.size >= 2)
        assertTrue(chunks[0].text.length <= TextChunker.CHUNK_SIZE + TextChunker.OVERLAP_SIZE)
    }

    @Test
    fun `empty text returns empty list`() {
        val chunks = TextChunker.chunk("", noteId = 1)
        assertEquals(0, chunks.size)
    }

    @Test
    fun `chunks have correct indices`() {
        val text = "a".repeat(1200)
        val chunks = TextChunker.chunk(text, noteId = 1)
        for (i in chunks.indices) {
            assertEquals(i, chunks[i].index)
        }
    }

    @Test
    fun `blank text returns empty list`() {
        val chunks = TextChunker.chunk("   \n  \n  ", noteId = 1)
        assertEquals(0, chunks.size)
    }
}
