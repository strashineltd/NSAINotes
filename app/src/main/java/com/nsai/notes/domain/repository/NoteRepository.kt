package com.nsai.notes.domain.repository

import com.nsai.notes.domain.model.Note
import com.nsai.notes.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getAllNotes(): Flow<List<Note>>
    fun getNoteById(id: Long): Flow<Note?>
    fun getNotesByTag(tagId: Long): Flow<List<Note>>
    fun getFavoriteNotes(): Flow<List<Note>>
    fun searchNotes(query: String): Flow<List<Note>>
    suspend fun createNote(note: Note): Long
    suspend fun updateNote(note: Note)
    suspend fun softDeleteNote(noteId: Long)
    suspend fun permanentlyDeleteNote(noteId: Long)
    suspend fun toggleFavorite(noteId: Long)
    suspend fun togglePin(noteId: Long)
    suspend fun renameNote(noteId: Long, title: String)

    suspend fun createTag(tag: Tag): Long
    fun getAllTags(): Flow<List<Tag>>
    suspend fun deleteTag(tagId: Long)
    suspend fun addTagToNote(noteId: Long, tagId: Long)
    suspend fun removeTagFromNote(noteId: Long, tagId: Long)
}
