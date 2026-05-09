package com.nsai.notes.domain.usecase

import com.nsai.notes.domain.model.Note
import com.nsai.notes.domain.repository.NoteRepository
import com.nsai.notes.domain.usecase.note.SearchNotesUseCase
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class SearchNotesUseCaseTest {

    private lateinit var repository: NoteRepository
    private lateinit var useCase: SearchNotesUseCase

    @Before
    fun setup() {
        repository = mock()
        useCase = SearchNotesUseCase(repository)
    }

    @Test
    fun `search should return matching notes`() = runTest {
        val notes = listOf(
            Note(id = 1, title = "AI Notes", content = "About AI"),
            Note(id = 2, title = "Shopping List", content = "Buy milk")
        )
        `when`(repository.searchNotes("AI")).thenReturn(flowOf(listOf(notes[0])))

        val result = useCase("AI")

        result.collect { list ->
            assertEquals(1, list.size)
            assertEquals("AI Notes", list[0].title)
        }
    }

    @Test
    fun `search with empty query should return all notes`() = runTest {
        val notes = listOf(
            Note(id = 1, title = "Note 1", content = "Content 1"),
            Note(id = 2, title = "Note 2", content = "Content 2")
        )
        `when`(repository.searchNotes("")).thenReturn(flowOf(notes))

        val result = useCase("")

        result.collect { list ->
            assertEquals(2, list.size)
        }
    }
}
