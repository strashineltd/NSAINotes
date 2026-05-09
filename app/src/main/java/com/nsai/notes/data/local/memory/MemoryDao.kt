package com.nsai.notes.data.local.memory

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: MemoryEntity): Long

    @Update
    suspend fun update(memory: MemoryEntity)

    @Query("SELECT * FROM memories ORDER BY importance DESC, last_accessed_at DESC")
    suspend fun getAll(): List<MemoryEntity>

    @Query("SELECT * FROM memories ORDER BY importance DESC, last_accessed_at DESC")
    fun getAllFlow(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getById(id: Long): MemoryEntity?

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM memories WHERE last_accessed_at < :cutoff AND importance < :minImportance")
    suspend fun deleteStale(cutoff: Long, minImportance: Float = 0.3f)

    @Query("SELECT * FROM memories WHERE embedding IS NOT NULL")
    suspend fun getAllWithEmbedding(): List<MemoryEntity>

    @Query("UPDATE memories SET access_count = access_count + 1, last_accessed_at = :timestamp WHERE id = :id")
    suspend fun incrementAccess(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM memories WHERE type = :type ORDER BY importance DESC")
    suspend fun getByType(type: String): List<MemoryEntity>
}
