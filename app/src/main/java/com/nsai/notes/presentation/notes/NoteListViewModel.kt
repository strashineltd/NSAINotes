package com.nsai.notes.presentation.notes

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.nsai.notes.data.local.datastore.SettingsDataStore
import com.nsai.notes.data.local.db.paging.NotePagingSource
import com.nsai.notes.domain.model.Note
import com.nsai.notes.domain.repository.NoteRepository
import com.nsai.notes.domain.usecase.note.GetAllNotesUseCase
import com.nsai.notes.domain.usecase.note.SearchNotesUseCase
import com.nsai.notes.domain.usecase.note.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
data class NoteListUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false
)

sealed class NoteListEvent {
    data object LoadNotes : NoteListEvent()
    data class Search(val query: String) : NoteListEvent()
    data class ToggleFavorite(val noteId: Long) : NoteListEvent()
    data object ClearSearch : NoteListEvent()
}

@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val getAllNotesUseCase: GetAllNotesUseCase,
    private val searchNotesUseCase: SearchNotesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val noteRepository: NoteRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteListUiState())
    val uiState: StateFlow<NoteListUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init { loadNotes() }

    fun onEvent(event: NoteListEvent) {
        when (event) {
            NoteListEvent.LoadNotes -> loadNotes()
            is NoteListEvent.Search -> search(event.query)
            is NoteListEvent.ToggleFavorite -> toggleFavorite(event.noteId)
            NoteListEvent.ClearSearch -> clearSearch()
        }
    }

    private fun loadNotes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                getAllNotesUseCase().collectLatest { notes ->
                    _uiState.value = _uiState.value.copy(notes = notes, isLoading = false, error = null)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    // Paging support - use when note count exceeds threshold
    private val _pagedNotes = MutableStateFlow<PagingData<Note>>(PagingData.empty())
    // Paging not wired to UI yet - T11 waves into screen layer next

    private fun search(query: String) {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(searchQuery = query, isSearchActive = true)
        if (query.isBlank()) { clearSearch(); return }
        searchJob = viewModelScope.launch {
            try {
                searchNotesUseCase(query).collectLatest { notes ->
                    _uiState.value = _uiState.value.copy(notes = notes, isLoading = false, error = null)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private fun clearSearch() {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(searchQuery = "", isSearchActive = false)
        loadNotes()
    }

    private fun toggleFavorite(noteId: Long) {
        viewModelScope.launch { toggleFavoriteUseCase(noteId) }
    }

    fun togglePin(noteId: Long) {
        viewModelScope.launch { noteRepository.togglePin(noteId) }
    }

    fun renameNote(noteId: Long, title: String) {
        viewModelScope.launch { noteRepository.renameNote(noteId, title) }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch { noteRepository.softDeleteNote(noteId) }
    }

    suspend fun getPrivacyPin(): String = settingsDataStore.getPrivacyPin()

    suspend fun setPrivacyPin(pin: String) { settingsDataStore.setPrivacyPin(pin) }
}
