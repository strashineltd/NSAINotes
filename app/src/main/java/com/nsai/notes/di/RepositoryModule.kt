package com.nsai.notes.di

import com.nsai.notes.data.repository.AIServiceImpl
import com.nsai.notes.data.repository.ConversationRepositoryImpl
import com.nsai.notes.data.repository.NoteRepositoryImpl
import com.nsai.notes.domain.repository.AIService
import com.nsai.notes.domain.repository.ConversationRepository
import com.nsai.notes.domain.repository.NoteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository

    @Binds
    @Singleton
    abstract fun bindAIService(impl: AIServiceImpl): AIService

    @Binds
    @Singleton
    abstract fun bindConversationRepository(impl: ConversationRepositoryImpl): ConversationRepository
}
