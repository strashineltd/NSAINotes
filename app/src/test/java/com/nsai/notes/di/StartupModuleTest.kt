package com.nsai.notes.di
import org.junit.Test
import org.junit.Assert.*
class StartupModuleTest {
    @Test fun `frame delay 500ms`() { assertEquals(500L, StartupModule.FRAME_MONITOR_DELAY_MS) }
    @Test fun `resource delay 1000ms`() { assertEquals(1000L, StartupModule.RESOURCE_MANAGER_DELAY_MS) }
}

