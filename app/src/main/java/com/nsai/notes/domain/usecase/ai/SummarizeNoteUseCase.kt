package com.nsai.notes.domain.usecase.ai

import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.repository.AIService
import com.nsai.notes.domain.repository.AIOptions
import com.nsai.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SummarizeNoteUseCase @Inject constructor(
    private val aiService: AIService,
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(
        noteId: Long,
        provider: AIProvider
    ): Result<String> {
        return runCatching {
            val note = noteRepository.getNoteById(noteId).first()
                ?: throw IllegalStateException("笔记不存在")

            note.aiSummary?.let { return@runCatching it }

            val summary = aiService.summarize(provider, note.content)
            noteRepository.updateNote(note.copy(aiSummary = summary))

            summary
        }
    }
}
