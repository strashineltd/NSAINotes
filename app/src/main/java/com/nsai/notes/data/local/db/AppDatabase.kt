package com.nsai.notes.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nsai.notes.data.local.db.dao.ConversationDao
import com.nsai.notes.data.local.db.dao.NoteDao
import com.nsai.notes.data.local.db.dao.TagDao
import com.nsai.notes.data.local.db.entity.ConversationEntity
import com.nsai.notes.data.local.db.entity.NoteEntity
import com.nsai.notes.data.local.db.entity.NoteTagEntity
import com.nsai.notes.data.local.db.entity.TagEntity
import com.nsai.notes.data.local.memory.MemoryDao
import com.nsai.notes.data.local.memory.MemoryEntity
import com.nsai.notes.data.local.vector.ChunkEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN is_private INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS conversations (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                messages_json TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """)
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS chunks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                note_id INTEGER NOT NULL,
                chunk_index INTEGER NOT NULL,
                content TEXT NOT NULL,
                embedding BLOB NOT NULL,
                created_at INTEGER NOT NULL
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chunks_note_id ON chunks(note_id)")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS memories (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                type TEXT NOT NULL,
                key TEXT NOT NULL,
                content TEXT NOT NULL,
                importance REAL NOT NULL DEFAULT 0.5,
                embedding BLOB NOT NULL,
                source_conversation_id INTEGER,
                access_count INTEGER NOT NULL DEFAULT 0,
                last_accessed_at INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                FOREIGN KEY (source_conversation_id) REFERENCES conversations(id)
            )
        """)
    }
}

@Database(
    entities = [
        NoteEntity::class,
        TagEntity::class,
        NoteTagEntity::class,
        ConversationEntity::class,
        ChunkEntity::class,
        MemoryEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun conversationDao(): ConversationDao
    abstract fun memoryDao(): MemoryDao
}
