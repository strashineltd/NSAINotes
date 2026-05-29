package com.nsai.notes.domain.usecase.tag

import com.nsai.notes.domain.model.Tag
import com.nsai.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllTagsUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<List<Tag>> {
        return repository.getAllTags()
    }
}
