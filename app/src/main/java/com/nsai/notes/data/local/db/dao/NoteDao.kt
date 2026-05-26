package com.nsai.notes.data.local.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nsai.notes.data.local.db.entity.NoteEntity
import com.nsai.notes.data.local.db.entity.NoteTagEntity
import com.nsai.notes.data.local.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
@Query("SELECT * FROM notes WHERE deleted_at IS NOT NULL AND deleted_at < :cutoff")
suspend fun getSoftDeletedBefore(cutoff: Long): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE deleted_at IS NULL ORDER BY pinned DESC, updated_at DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id AND deleted_at IS NULL")
    fun getNoteById(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteByIdAny(id: Long): NoteEntity?

    @Query("""
        SELECT n.* FROM notes n
        INNER JOIN note_tags nt ON n.id = nt.note_id
        WHERE nt.tag_id = :tagId AND n.deleted_at IS NULL
        ORDER BY n.updated_at DESC
    """)
    fun getNotesByTag(tagId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE is_favorite = 1 AND deleted_at IS NULL ORDER BY updated_at DESC")
    fun getFavoriteNotes(): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes 
        WHERE deleted_at IS NULL 
        AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY updated_at DESC
    """)
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("UPDATE notes SET deleted_at = :timestamp WHERE id = :noteId")
    suspend fun softDeleteNote(noteId: Long, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun permanentlyDeleteNote(note: NoteEntity)

    @Query("UPDATE notes SET is_favorite = CASE WHEN is_favorite = 1 THEN 0 ELSE 1 END WHERE id = :noteId")
    suspend fun toggleFavorite(noteId: Long)

    @Query("UPDATE notes SET pinned = CASE WHEN pinned = 1 THEN 0 ELSE 1 END WHERE id = :noteId")
    suspend fun togglePin(noteId: Long)

    @Query("UPDATE notes SET title = :title WHERE id = :noteId")
    suspend fun renameNote(noteId: Long, title: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTag(noteTag: NoteTagEntity)

    @Query("DELETE FROM note_tags WHERE note_id = :noteId AND tag_id = :tagId")
    suspend fun removeTagFromNote(noteId: Long, tagId: Long)

    @Query("SELECT tag_id FROM note_tags WHERE note_id = :noteId")
    suspend fun getTagIdsForNote(noteId: Long): List<Long>

    @Query("DELETE FROM note_tags WHERE note_id = :noteId")
    suspend fun removeAllTagsFromNote(noteId: Long)

    @Query("SELECT t.* FROM tags t INNER JOIN note_tags nt ON t.id = nt.tag_id WHERE nt.note_id = :noteId")
    suspend fun getTagsForNote(noteId: Long): List<TagEntity>

    @Query("SELECT nt.note_id, t.id, t.name, t.color, t.created_at FROM tags t INNER JOIN note_tags nt ON t.id = nt.tag_id WHERE nt.note_id IN (:noteIds)")
    suspend fun getTagsForNotes(noteIds: List<Long>): List<NoteTagResult>

    data class NoteTagResult(
        @ColumnInfo(name = "note_id") val noteId: Long,
        val id: Long,
        val name: String,
        val color: Int,
        @ColumnInfo(name = "created_at") val createdAt: Long
    ) {
        fun toTagEntity() = TagEntity(id = id, name = name, color = color, createdAt = createdAt)
    }

    @Transaction
    suspend fun insertNoteWithTags(note: NoteEntity, tagIds: List<Long>): Long {
        val id = insertNote(note)
        tagIds.forEach { insertNoteTag(NoteTagEntity(id, it)) }
        return id
    }

    @Transaction
    suspend fun updateNoteWithTags(note: NoteEntity, tagIds: List<Long>) {
        updateNote(note)
        removeAllTagsFromNote(note.id)
        tagIds.forEach { insertNoteTag(NoteTagEntity(note.id, it)) }
    }

    @Transaction
    suspend fun updateNoteTags(noteId: Long, tagIds: List<Long>) {
        removeAllTagsFromNote(noteId)
        tagIds.forEach { tagId ->
            insertNoteTag(NoteTagEntity(noteId, tagId))
        }
    }

// === Paging queries (added by Wave 2 T6) ===
@Query("SELECT * FROM notes WHERE deleted_at IS NULL ORDER BY pinned DESC, updated_at DESC LIMIT :limit OFFSET :offset")
suspend fun getNotesPaged(limit: Int, offset: Int): List<NoteEntity>

@Query("SELECT COUNT(*) FROM notes WHERE deleted_at IS NULL")
suspend fun getNotesCount(): Int

@Query("SELECT * FROM notes WHERE is_favorite = 1 AND deleted_at IS NULL ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
suspend fun getFavoritesPaged(limit: Int, offset: Int): List<NoteEntity>

@Query("SELECT COUNT(*) FROM notes WHERE is_favorite = 1 AND deleted_at IS NULL")
suspend fun getFavoritesCount(): Int

@Query("SELECT * FROM notes WHERE deleted_at IS NULL AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
suspend fun searchNotesPaged(query: String, limit: Int, offset: Int): List<NoteEntity>

@Query("SELECT COUNT(*) FROM notes WHERE deleted_at IS NULL AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')")
suspend fun searchNotesCount(query: String): Int

@Query("SELECT n.* FROM notes n INNER JOIN note_tags nt ON n.id = nt.note_id WHERE nt.tag_id = :tagId AND n.deleted_at IS NULL ORDER BY n.updated_at DESC LIMIT :limit OFFSET :offset")
suspend fun getNotesByTagPaged(tagId: Long, limit: Int, offset: Int): List<NoteEntity>

@Query("SELECT COUNT(*) FROM notes n INNER JOIN note_tags nt ON n.id = nt.note_id WHERE nt.tag_id = :tagId AND n.deleted_at IS NULL")
suspend fun getNotesByTagCount(tagId: Long): Int

@Query("SELECT * FROM notes WHERE deleted_at IS NULL ORDER BY id")
suspend fun getAllNotesRaw(): List<NoteEntity>
}

