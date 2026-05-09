package com.nsai.notes.data.mapper

import com.nsai.notes.data.local.db.entity.NoteEntity
import com.nsai.notes.domain.model.Note
import com.nsai.notes.domain.model.Tag
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteMapper @Inject constructor() {

    fun toDomain(entity: NoteEntity, tags: List<Tag> = emptyList()): Note {
        return Note(
            id = entity.id,
            title = entity.title,
            content = entity.content,
            tags = tags,
            isFavorite = entity.isFavorite,
            aiSummary = entity.aiSummary,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            deletedAt = entity.deletedAt,
            isPrivate = entity.isPrivate,
            pinned = entity.pinned
        )
    }

    fun toEntity(domain: Note): NoteEntity {
        return NoteEntity(
            id = domain.id,
            title = domain.title,
            content = domain.content,
            isFavorite = domain.isFavorite,
            aiSummary = domain.aiSummary,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            deletedAt = domain.deletedAt,
            isPrivate = domain.isPrivate,
            pinned = domain.pinned
        )
    }
}
