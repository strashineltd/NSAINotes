package com.nsai.notes.domain.usecase.tag

import com.nsai.notes.domain.model.Tag
import com.nsai.notes.domain.repository.NoteRepository
import javax.inject.Inject

class CreateTagUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(name: String, color: Int = 0xFF1976D2.toInt()): Result<Long> {
        return runCatching {
            if (name.isBlank()) throw IllegalArgumentException("标签名称不能为空")
            if (name.length > 20) throw IllegalArgumentException("标签名称不能超过20个字符")

            val tag = Tag(
                name = name.trim(),
                color = color
            )
            repository.createTag(tag)
        }
    }
}
