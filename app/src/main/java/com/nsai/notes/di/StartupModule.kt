package com.nsai.notes.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object StartupModule {
    const val FRAME_MONITOR_DELAY_MS = 500L
    const val RESOURCE_MANAGER_DELAY_MS = 1000L
}
