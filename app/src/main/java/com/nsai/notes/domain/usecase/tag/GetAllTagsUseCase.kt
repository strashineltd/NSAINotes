package com.nsai.notes.domain.usecase.tag

import com.nsai.notes.domain.model.Tag
import com.nsai.notes.domain.repository.NoteRepository
import javax.inject.Inject

class GetAllTagsUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(): List<Tag> {
        return repository.getAllTags()
    }
}
