package com.nsai.notes.domain.model

import androidx.compose.runtime.Stable

@Stable
enum class ThemeMode(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2);

    companion object {
        fun fromValue(value: Int): ThemeMode =
            entries.find { it.value == value } ?: SYSTEM
    }
}
