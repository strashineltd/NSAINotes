package com.nsai.notes.domain.usecase.note

import com.nsai.notes.domain.model.Note
import com.nsai.notes.domain.repository.NoteRepository
import javax.inject.Inject

class UpdateNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note): Result<Unit> {
        return runCatching {
            if (note.title.isBlank()) throw IllegalArgumentException("标题不能为空")
            if (note.content.isBlank()) throw IllegalArgumentException("内容不能为空")

            repository.updateNote(
                note.copy(
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
