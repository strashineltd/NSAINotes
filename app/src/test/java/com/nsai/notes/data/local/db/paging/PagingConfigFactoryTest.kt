package com.nsai.notes.data.local.db.paging

import androidx.paging.PagingConfig
import org.junit.Test
import org.junit.Assert.*

class PagingConfigFactoryTest {
    @Test
    fun `default config has correct pageSize`() {
        val config = PagingConfigFactory.defaultConfig
        assertEquals(30, config.pageSize)
    }

    @Test
    fun `default config disables placeholders`() {
        val config = PagingConfigFactory.defaultConfig
        assertFalse(config.enablePlaceholders)
    }
}

