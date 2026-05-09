package com.nsai.notes.domain.usecase.note

import com.nsai.notes.domain.model.Note
import com.nsai.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(noteId: Long): Flow<Note?> {
        return repository.getNoteById(noteId)
    }
}
