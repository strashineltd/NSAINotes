package com.nsai.notes.domain.usecase.note

import com.nsai.notes.domain.model.Note
import com.nsai.notes.domain.model.Tag
import com.nsai.notes.domain.repository.NoteRepository
import javax.inject.Inject

class CreateNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(
        title: String,
        content: String,
        tags: List<Tag> = emptyList(),
        isPrivate: Boolean = false
    ): Result<Long> {
        return runCatching {
            if (title.isBlank()) throw IllegalArgumentException("标题不能为空")
            if (content.isBlank()) throw IllegalArgumentException("内容不能为空")

            val note = Note(
                title = title.trim(),
                content = content.trim(),
                tags = tags,
                isPrivate = isPrivate
            )
            repository.createNote(note)
        }
    }
}
