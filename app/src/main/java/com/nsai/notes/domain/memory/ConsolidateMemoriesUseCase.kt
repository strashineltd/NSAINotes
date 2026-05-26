package com.nsai.notes.domain.memory

import com.nsai.notes.data.local.memory.MemoryDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsolidateMemoriesUseCase @Inject constructor(
    private val memoryDao: MemoryDao
) {
    suspend fun consolidate() {
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        memoryDao.deleteStale(cutoff, minImportance = 0.3f)
    }
}
