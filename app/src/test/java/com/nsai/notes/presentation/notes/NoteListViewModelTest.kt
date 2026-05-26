package com.nsai.notes.presentation.notes

import com.nsai.notes.data.local.datastore.SettingsDataStore
import com.nsai.notes.domain.model.Note
import com.nsai.notes.domain.repository.NoteRepository
import com.nsai.notes.domain.usecase.note.GetAllNotesUseCase
import com.nsai.notes.domain.usecase.note.SearchNotesUseCase
import com.nsai.notes.domain.usecase.note.ToggleFavoriteUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class NoteListViewModelTest {

    private lateinit var getAllNotesUseCase: GetAllNotesUseCase
    private lateinit var searchNotesUseCase: SearchNotesUseCase
    private lateinit var toggleFavoriteUseCase: ToggleFavoriteUseCase
    private lateinit var noteRepository: NoteRepository
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var viewModel: NoteListViewModel

    @Before
    fun setup() {
        getAllNotesUseCase = mock()
        searchNotesUseCase = mock()
        toggleFavoriteUseCase = mock()
        noteRepository = mock()
        settingsDataStore = mock()
        viewModel = NoteListViewModel(
            getAllNotesUseCase, searchNotesUseCase, toggleFavoriteUseCase,
            noteRepository, settingsDataStore
        )
    }

    @Test
    fun `init should load notes`() = runTest {
        val notes = listOf(
            Note(id = 1, title = "Test", content = "Content"),
            Note(id = 2, title = "Another", content = "More")
        )
        `when`(getAllNotesUseCase()).thenReturn(flowOf(notes))

        val state = viewModel.uiState.first()
        assertEquals(2, state.notes.size)
        assertFalse(state.isLoading)
    }

    @Test
    fun `search should set isSearchActive`() = runTest {
        val notes = emptyList<Note>()
        `when`(getAllNotesUseCase()).thenReturn(flowOf(notes))
        viewModel.uiState.first() // wait for load

        viewModel.onEvent(NoteListEvent.Search("test"))

        assertTrue(viewModel.uiState.value.isSearchActive)
        assertEquals("test", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun `clearSearch should reset state`() = runTest {
        val notes = emptyList<Note>()
        `when`(getAllNotesUseCase()).thenReturn(flowOf(notes))
        viewModel.uiState.first()

        viewModel.onEvent(NoteListEvent.Search("test"))
        viewModel.onEvent(NoteListEvent.ClearSearch)

        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertFalse(state.isSearchActive)
    }

    @Test
    fun `toggleFavorite should call use case`() = runTest {
        val notes = emptyList<Note>()
        `when`(getAllNotesUseCase()).thenReturn(flowOf(notes))
        viewModel.uiState.first()

        viewModel.onEvent(NoteListEvent.ToggleFavorite(1L))
        verify(toggleFavoriteUseCase).invoke(1L)
    }
}
