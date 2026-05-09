package com.nsai.notes.domain.model

import androidx.compose.runtime.Stable

@Stable
data class Note(
    val id: Long = 0,
    val title: String,
    val content: String,
    val tags: List<Tag> = emptyList(),
    val isFavorite: Boolean = false,
    val aiSummary: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val isPrivate: Boolean = false,
    val pinned: Boolean = false
) {
    val isDeleted: Boolean get() = deletedAt != null
}
