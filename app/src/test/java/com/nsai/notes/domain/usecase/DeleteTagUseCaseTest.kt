package com.nsai.notes.domain.usecase

import com.nsai.notes.domain.repository.NoteRepository
import com.nsai.notes.domain.usecase.tag.DeleteTagUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class DeleteTagUseCaseTest {

    private lateinit var repository: NoteRepository
    private lateinit var useCase: DeleteTagUseCase

    @Before
    fun setup() {
        repository = mock()
        useCase = DeleteTagUseCase(repository)
    }

    @Test
    fun `delete tag should succeed`() = runTest {
        val result = useCase(tagId = 1L)

        assertTrue(result.isSuccess)
        verify(repository).deleteTag(1L)
    }
}
