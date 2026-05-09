package com.nsai.notes.di

import com.nsai.notes.data.local.db.AppDatabase
import com.nsai.notes.data.local.embedding.EmbeddingEngine
import com.nsai.notes.data.local.vector.VectorStore
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

    @Provides
    @Singleton
    fun provideVectorStore(db: AppDatabase): VectorStore = VectorStore(db)
}
