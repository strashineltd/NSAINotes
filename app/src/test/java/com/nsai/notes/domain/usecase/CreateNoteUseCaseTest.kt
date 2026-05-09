package com.nsai.notes.domain.usecase

import com.nsai.notes.domain.model.Note
import com.nsai.notes.domain.repository.NoteRepository
import com.nsai.notes.domain.usecase.note.CreateNoteUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class CreateNoteUseCaseTest {

    private lateinit var repository: NoteRepository
    private lateinit var useCase: CreateNoteUseCase

    @Before
    fun setup() {
        repository = mock()
        useCase = CreateNoteUseCase(repository)
    }

    @Test
    fun `create note with valid title and content should succeed`() = runTest {
        `when`(repository.createNote(any<Note>())).thenReturn(1L)

        val result = useCase(title = "Test Title", content = "Test Content")

        assertTrue(result.isSuccess)
        assertEquals(1L, result.getOrNull())
        verify(repository).createNote(any<Note>())
    }

    @Test
    fun `create note with empty title should fail`() = runTest {
        val result = useCase(title = "", content = "Content")

        assertTrue(result.isFailure)
        assertEquals("标题不能为空", result.exceptionOrNull()?.message)
    }

    @Test
    fun `create note with empty content should fail`() = runTest {
        val result = useCase(title = "Title", content = "")

        assertTrue(result.isFailure)
        assertEquals("内容不能为空", result.exceptionOrNull()?.message)
    }
}
