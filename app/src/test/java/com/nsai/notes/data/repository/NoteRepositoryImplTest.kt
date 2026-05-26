package com.nsai.notes.data.repository

import com.nsai.notes.data.local.db.dao.NoteDao
import com.nsai.notes.data.local.db.dao.TagDao
import com.nsai.notes.data.local.security.KeyStoreManager
import com.nsai.notes.data.mapper.NoteMapper
import com.nsai.notes.data.mapper.TagMapper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class NoteRepositoryImplTest {

    private lateinit var noteDao: NoteDao
    private lateinit var tagDao: TagDao
    private lateinit var noteMapper: NoteMapper
    private lateinit var tagMapper: TagMapper
    private lateinit var keyStoreManager: KeyStoreManager
    private lateinit var repository: NoteRepositoryImpl

    @Before
    fun setup() {
        noteDao = mock()
        tagDao = mock()
        noteMapper = NoteMapper()
        tagMapper = TagMapper()
        keyStoreManager = mock()
        repository = NoteRepositoryImpl(noteDao, tagDao, noteMapper, tagMapper, keyStoreManager)
    }

    @Test
    fun `getAllNotes should return mapped notes`() = runTest {
        val entity = com.nsai.notes.data.local.db.entity.NoteEntity(
            id = 1, title = "Test", content = "Content"
        )
        `when`(noteDao.getAllNotes()).thenReturn(flowOf(listOf(entity)))
        `when`(noteDao.getTagsForNotes(org.mockito.ArgumentMatchers.anyList()))
            .thenReturn(emptyList())

        val notes = repository.getAllNotes().first()

        assertNotNull(notes)
        assertEquals(1, notes.size)
        assertEquals("Test", notes[0].title)
        assertEquals("Content", notes[0].content)
    }
}
