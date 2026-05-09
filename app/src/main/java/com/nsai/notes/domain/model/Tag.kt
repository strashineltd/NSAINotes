package com.nsai.notes.domain.model

import androidx.compose.runtime.Stable

@Stable
data class Tag(
    val id: Long = 0,
    val name: String,
    val color: Int = 0xFF1976D2.toInt(),
    val createdAt: Long = System.currentTimeMillis()
)
