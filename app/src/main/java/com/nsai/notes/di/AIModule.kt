package com.nsai.notes.di

import com.nsai.notes.data.remote.ai.AIProviderAdapter
import com.nsai.notes.data.remote.ai.BaseAIAdapter
import com.nsai.notes.data.remote.ai.DeepSeekAdapter
import com.nsai.notes.data.remote.ai.GLMAdapter
import com.nsai.notes.data.remote.ai.KimiAdapter
import com.nsai.notes.data.remote.ai.MiMoAdapter
import com.nsai.notes.data.remote.ai.MiniMaxAdapter
import com.nsai.notes.data.remote.ai.CustomAIAdapter
import com.nsai.notes.data.remote.ai.QwenAdapter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AIModule {

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindDeepSeekAdapter(adapter: DeepSeekAdapter): AIProviderAdapter

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindKimiAdapter(adapter: KimiAdapter): AIProviderAdapter

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindGLMAdapter(adapter: GLMAdapter): AIProviderAdapter

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindMiniMaxAdapter(adapter: MiniMaxAdapter): AIProviderAdapter

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindQwenAdapter(adapter: QwenAdapter): AIProviderAdapter

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindMiMoAdapter(adapter: MiMoAdapter): AIProviderAdapter

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindCustomAIAdapter(adapter: CustomAIAdapter): AIProviderAdapter
}
