package com.nsai.notes.domain.usecase.note

import com.nsai.notes.domain.model.Note
import com.nsai.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchNotesUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(query: String): Flow<List<Note>> {
        return repository.searchNotes(query.trim())
    }
}
