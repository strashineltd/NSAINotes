package com.nsai.notes.di

import android.content.Context
import androidx.room.Room
import com.nsai.notes.data.local.db.AppDatabase
import com.nsai.notes.data.local.db.MIGRATION_1_2
import com.nsai.notes.data.local.db.MIGRATION_2_3
import com.nsai.notes.data.local.db.MIGRATION_3_4
import com.nsai.notes.data.local.db.MIGRATION_4_5
import com.nsai.notes.data.local.db.MIGRATION_5_6
import com.nsai.notes.data.local.db.MIGRATION_6_7
import com.nsai.notes.data.local.db.dao.ConversationDao
import com.nsai.notes.data.local.db.dao.ChunkDao
import com.nsai.notes.data.local.db.dao.NoteDao
import com.nsai.notes.data.local.db.dao.TagDao
import com.nsai.notes.data.local.memory.MemoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "nsai_notes.db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideNoteDao(database: AppDatabase): NoteDao = database.noteDao()

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao = database.tagDao()

    @Provides
    fun provideConversationDao(database: AppDatabase): ConversationDao = database.conversationDao()

    @Provides
    fun provideMemoryDao(database: AppDatabase): MemoryDao = database.memoryDao()

    @Provides
    fun provideChunkDao(database: AppDatabase): ChunkDao = database.chunkDao()
}
