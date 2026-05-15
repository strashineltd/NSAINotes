package com.nsai.notes.di

import com.nsai.notes.data.local.embedding.EmbeddingEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AIEngineModule {

    @Provides
    @Singleton
    fun provideEmbeddingEngine(): EmbeddingEngine = EmbeddingEngine()
}
