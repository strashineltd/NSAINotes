package com.nsai.notes.domain.usecase.note

import com.nsai.notes.domain.repository.NoteRepository
import javax.inject.Inject

class DeleteNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(noteId: Long): Result<Unit> {
        return runCatching {
            repository.softDeleteNote(noteId)
        }
    }
}
