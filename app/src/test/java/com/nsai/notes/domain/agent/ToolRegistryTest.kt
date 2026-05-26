package com.nsai.notes.domain.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ToolRegistryTest {
    @Test fun `registry finds tool by name`() {
        val tool = FakeTool()
        assertEquals(tool, ToolRegistry(setOf(tool)).get("fake_tool"))
    }
    @Test fun `registry returns null for unknown`() {
        assertNull(ToolRegistry(emptySet()).get("unknown"))
    }
    @Test fun `all tools returns list`() {
        val t1 = FakeTool(); val t2 = FakeTool("other")
        assertEquals(2, ToolRegistry(setOf(t1, t2)).all().size)
    }
    private class FakeTool(override val name: String = "fake_tool") : AgentTool {
        override val displayName = "Fake"; override val description = "test tool"
        override val parameters = emptyList<ToolParameter>(); override val isDestructive = false
        override suspend fun execute(params: Map<String, String>) = ToolResult(true, "ok")
    }
}
