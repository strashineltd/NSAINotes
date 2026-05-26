package com.nsai.notes.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Int = 0xFF1976D2.toInt(),
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
