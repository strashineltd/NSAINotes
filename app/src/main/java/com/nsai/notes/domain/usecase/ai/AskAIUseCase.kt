package com.nsai.notes.domain.usecase.ai

import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.model.ChatMessage
import com.nsai.notes.domain.model.Note
import com.nsai.notes.domain.repository.AIService
import com.nsai.notes.domain.repository.AIOptions
import com.nsai.notes.domain.repository.AIResponse
import com.nsai.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AskAIUseCase @Inject constructor(
    private val aiService: AIService,
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(
        question: String,
        provider: AIProvider,
        noteContext: Note? = null,
        mode: com.nsai.notes.domain.model.AIMode = com.nsai.notes.domain.model.AIMode.QUICK
    ): Result<AIResponse> {
        return runCatching {
            val messages = buildList {
                val systemPrompt = buildString {
                    append("你是一个AI笔记助手。你的任务是帮助用户处理笔记相关的任务。")
                    if (noteContext != null) {
                        append("\n\n以下是用户当前笔记的内容：\n---\n")
                        append("标题：${noteContext.title}\n")
                        append("内容：${noteContext.content}")
                        append("\n---\n请基于以上笔记内容回答用户的问题。")
                    }
                }
                add(ChatMessage(ChatMessage.Role.SYSTEM, systemPrompt))
                add(ChatMessage(ChatMessage.Role.USER, question))
            }

            aiService.chat(provider, messages, mode = mode)
        }
    }
}
