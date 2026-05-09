package com.nsai.notes.data.local.db.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.nsai.notes.data.local.db.dao.NoteDao
import com.nsai.notes.data.mapper.NoteMapper
import com.nsai.notes.data.mapper.TagMapper
import com.nsai.notes.domain.model.Note

class NotePagingSource(
    private val noteDao: NoteDao,
    private val noteMapper: NoteMapper,
    private val tagMapper: TagMapper
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

            val notes = entities.map { noteMapper.toDomain(it, tagsMap[it.id] ?: emptyList()) }

            val nextKey = if (offset + pageSize < count) page + 1 else null
            LoadResult.Page(notes, prevKey = if (page > 0) page - 1 else null, nextKey = nextKey)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Note>): Int? {
        return state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }
    }
}
