package com.nsai.notes.data.local.db.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.nsai.notes.data.local.db.dao.NoteDao
import com.nsai.notes.data.local.security.KeyStoreManager
import com.nsai.notes.data.mapper.NoteMapper
import com.nsai.notes.data.mapper.TagMapper
import com.nsai.notes.domain.model.Note

private const val ENCRYPTED_PREFIX = "AES256:"

class NotePagingSource(
    private val noteDao: NoteDao,
    private val noteMapper: NoteMapper,
    private val tagMapper: TagMapper,
    private val keyStoreManager: KeyStoreManager
) : PagingSource<Int, Note>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Note> {
        return try {
            val page = params.key ?: 0
            val pageSize = params.loadSize
            val offset = page * pageSize

            val entities = noteDao.getNotesPaged(pageSize, offset)
            val count = noteDao.getNotesCount()

            val tagsMap = noteDao.getTagsForNotes(entities.map { it.id })
                .groupBy({ it.noteId }, { it.toTagEntity().let { tagMapper.toDomain(it) } })

            val notes = entities.map { entity ->
                decryptNote(noteMapper.toDomain(entity, tagsMap[entity.id] ?: emptyList()))
            }

            val nextKey = if (offset + pageSize < count) page + 1 else null
            LoadResult.Page(notes, prevKey = if (page > 0) page - 1 else null, nextKey = nextKey)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private fun decryptNote(note: Note): Note {
        if (!note.isPrivate) return note
        return note.copy(
            title = decryptField(note.title),
            content = decryptField(note.content)
        )
    }

    private fun decryptField(value: String): String {
        if (!value.startsWith(ENCRYPTED_PREFIX)) return value
        val encoded = value.removePrefix(ENCRYPTED_PREFIX)
        return runCatching { keyStoreManager.decryptFromString(encoded) }.getOrElse { "[解密失败]" }
    }

    override fun getRefreshKey(state: PagingState<Int, Note>): Int? {
        return state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }
    }
}
