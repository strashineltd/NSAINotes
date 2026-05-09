package com.nsai.notes.data.mapper

import com.nsai.notes.data.local.db.entity.TagEntity
import com.nsai.notes.domain.model.Tag
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagMapper @Inject constructor() {

    fun toDomain(entity: TagEntity): Tag {
        return Tag(
            id = entity.id,
            name = entity.name,
            color = entity.color,
            createdAt = entity.createdAt
        )
    }

    fun toEntity(domain: Tag): TagEntity {
        return TagEntity(
            id = domain.id,
            name = domain.name,
            color = domain.color,
            createdAt = domain.createdAt
        )
    }
}
