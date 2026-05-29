package com.nsai.notes.data.repository

import com.nsai.notes.data.local.db.dao.NoteDao
import com.nsai.notes.data.local.db.dao.TagDao
import com.nsai.notes.data.local.db.entity.NoteTagEntity
import com.nsai.notes.data.local.security.KeyStoreManager
import com.nsai.notes.data.mapper.NoteMapper
import com.nsai.notes.data.mapper.TagMapper
import com.nsai.notes.domain.model.Note
import com.nsai.notes.domain.model.Tag
import com.nsai.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val ENCRYPTED_PREFIX = "AES256:"

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val tagDao: TagDao,
    private val noteMapper: NoteMapper,
    private val tagMapper: TagMapper,
    private val keyStoreManager: KeyStoreManager
) : NoteRepository {

    override fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes().map { entities ->
            val tagsMap = if (entities.isEmpty()) emptyMap()
            else noteDao.getTagsForNotes(entities.map { it.id })
                .groupBy({ it.noteId }, { it.toTagEntity().let { tagMapper.toDomain(it) } })
            entities.map { entity ->
                decryptNote(noteMapper.toDomain(entity, tagsMap[entity.id] ?: emptyList()))
            }
        }
    }

    override fun getNoteById(id: Long): Flow<Note?> {
        return noteDao.getNoteById(id).map { entity ->
            entity?.let {
                val tags = noteDao.getTagsForNote(it.id).map { e -> tagMapper.toDomain(e) }
                decryptNote(noteMapper.toDomain(it, tags))
            }
        }
    }

    override fun getNotesByTag(tagId: Long): Flow<List<Note>> {
        return noteDao.getNotesByTag(tagId).map { entities ->
            val tagsMap = if (entities.isEmpty()) emptyMap()
            else noteDao.getTagsForNotes(entities.map { it.id })
                .groupBy({ it.noteId }, { it.toTagEntity().let { tagMapper.toDomain(it) } })
            entities.map { entity ->
                decryptNote(noteMapper.toDomain(entity, tagsMap[entity.id] ?: emptyList()))
            }
        }
    }

    override fun getFavoriteNotes(): Flow<List<Note>> {
        return noteDao.getFavoriteNotes().map { entities ->
            val tagsMap = if (entities.isEmpty()) emptyMap()
            else noteDao.getTagsForNotes(entities.map { it.id })
                .groupBy({ it.noteId }, { it.toTagEntity().let { tagMapper.toDomain(it) } })
            entities.map { entity ->
                decryptNote(noteMapper.toDomain(entity, tagsMap[entity.id] ?: emptyList()))
            }
        }
    }

    override fun searchNotes(query: String): Flow<List<Note>> {
        return noteDao.searchNotes(query).map { entities ->
            val tagsMap = if (entities.isEmpty()) emptyMap()
            else noteDao.getTagsForNotes(entities.map { it.id })
                .groupBy({ it.noteId }, { it.toTagEntity().let { tagMapper.toDomain(it) } })
            entities.map { entity ->
                decryptNote(noteMapper.toDomain(entity, tagsMap[entity.id] ?: emptyList()))
            }
        }
    }

    override suspend fun createNote(note: Note): Long {
        val entity = noteMapper.toEntity(encryptNote(note))
        val id = noteDao.insertNoteWithTags(entity, note.tags.map { it.id })
        return id
    }

    override suspend fun updateNote(note: Note) {
        val entity = noteMapper.toEntity(encryptNote(note))
        noteDao.updateNoteWithTags(entity, note.tags.map { it.id })
    }

    override suspend fun softDeleteNote(noteId: Long) {
        noteDao.softDeleteNote(noteId)
    }

    override suspend fun permanentlyDeleteNote(noteId: Long) {
        noteDao.getNoteByIdAny(noteId)?.let { entity ->
            noteDao.permanentlyDeleteNote(entity)
        }
    }

    override suspend fun toggleFavorite(noteId: Long) {
        noteDao.toggleFavorite(noteId)
    }

    override suspend fun togglePin(noteId: Long) {
        noteDao.togglePin(noteId)
    }

    override suspend fun renameNote(noteId: Long, title: String) {
        noteDao.renameNote(noteId, title)
    }

    override suspend fun createTag(tag: Tag): Long {
        val entity = tagMapper.toEntity(tag)
        return tagDao.insertTag(entity)
    }

    override fun getAllTags(): Flow<List<Tag>> {
        return tagDao.getAllTags().map { entities -> entities.map { tagMapper.toDomain(it) } }
    }

    override suspend fun deleteTag(tagId: Long) {
        tagDao.deleteTagById(tagId)
    }

    override suspend fun addTagToNote(noteId: Long, tagId: Long) {
        noteDao.insertNoteTag(NoteTagEntity(noteId, tagId))
    }

    override suspend fun removeTagFromNote(noteId: Long, tagId: Long) {
        noteDao.removeTagFromNote(noteId, tagId)
    }

    private fun encryptNote(note: Note): Note {
        if (!note.isPrivate) return note
        if (note.title.startsWith(ENCRYPTED_PREFIX)) return note
        val encTitle = keyStoreManager.encryptToString(note.title)
        val encContent = keyStoreManager.encryptToString(note.content)
        return note.copy(
            title = ENCRYPTED_PREFIX + encTitle,
            content = ENCRYPTED_PREFIX + encContent
        )
    }

    private fun decryptNote(note: Note): Note {
        if (!note.isPrivate) return note
        val decryptedTitle = decryptField(note.title)
        val decryptedContent = decryptField(note.content)
        return note.copy(title = decryptedTitle, content = decryptedContent)
    }

    private fun decryptField(value: String): String {
        if (!value.startsWith(ENCRYPTED_PREFIX)) return value
        val encoded = value.removePrefix(ENCRYPTED_PREFIX)
        return runCatching { keyStoreManager.decryptFromString(encoded) }.getOrElse { DECRYPT_FAILED }
    }

    companion object {
        const val DECRYPT_FAILED = "[解密失败]"
    }
}
