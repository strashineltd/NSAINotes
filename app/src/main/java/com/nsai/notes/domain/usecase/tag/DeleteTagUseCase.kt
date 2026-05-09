package com.nsai.notes.domain.usecase.tag

import com.nsai.notes.domain.repository.NoteRepository
import javax.inject.Inject

class DeleteTagUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(tagId: Long): Result<Unit> {
        return runCatching {
            repository.deleteTag(tagId)
        }
    }
}
