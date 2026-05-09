package com.nsai.notes.data.local.db.worker

import org.junit.Test
import org.junit.Assert.*

class NSAIWorkersTest {
    @Test
    fun `worker name constants correct`() {
        assertEquals("nsai_data_cleanup", NSAIWorkers.DATA_CLEANUP)
        assertEquals("nsai_search_history_cleanup", NSAIWorkers.SEARCH_HISTORY_CLEANUP)
    }
}

