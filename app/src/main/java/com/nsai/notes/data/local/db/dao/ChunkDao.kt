package com.nsai.notes.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nsai.notes.data.local.vector.ChunkEntity

@Dao
interface ChunkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chunk: ChunkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(chunks: List<ChunkEntity>)

    @Query("DELETE FROM chunks WHERE note_id = :noteId")
    suspend fun deleteByNoteId(noteId: Long)

    @Query("DELETE FROM chunks")
    suspend fun deleteAll()

    @Query("SELECT * FROM chunks ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 2000): List<ChunkEntity>

    @Query("SELECT * FROM chunks WHERE note_id = :noteId ORDER BY chunk_index ASC")
    suspend fun getByNoteId(noteId: Long): List<ChunkEntity>

    @Query("SELECT COUNT(*) FROM chunks")
    suspend fun count(): Int
}
