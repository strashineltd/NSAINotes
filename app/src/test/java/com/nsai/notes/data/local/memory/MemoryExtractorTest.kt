package com.nsai.notes.data.local.memory

import com.google.gson.Gson
import com.nsai.notes.domain.memory.MemoryType
import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryExtractorTest {
    private val extractor = MemoryExtractor(Gson())

    @Test fun `parse AI response extracts memories from JSON`() {
        val json = """{"memories":[{"type":"PREFERENCE","key":"回答风格","content":"用户喜欢简洁"},{"type":"FACT","key":"职业","content":"Android开发者"}]}"""
        val memories = extractor.parseMemoriesJson(json, sourceConversationId = 1)
        assertEquals(2, memories.size)
        assertEquals(MemoryType.PREFERENCE, memories[0].type)
        assertEquals(1L, memories[0].sourceConversationId)
    }
    @Test fun `parse empty returns empty`() {
        assertEquals(0, extractor.parseMemoriesJson("", 1).size)
    }
    @Test fun `parse malformed returns empty`() {
        assertEquals(0, extractor.parseMemoriesJson("{bad json", 1).size)
    }
}
