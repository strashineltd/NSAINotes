package com.nsai.notes.presentation.notes

import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nsai.notes.domain.model.Note
import com.nsai.notes.domain.model.Tag
import com.nsai.notes.domain.usecase.note.CreateNoteUseCase
import com.nsai.notes.domain.usecase.note.GetNoteUseCase
import com.nsai.notes.domain.usecase.note.UpdateNoteUseCase
import com.nsai.notes.domain.usecase.tag.GetAllTagsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import com.nsai.notes.domain.multimodal.DescribeImageUseCase
import com.nsai.notes.domain.multimodal.ExtractTextUseCase
import com.nsai.notes.domain.model.AIProvider
import javax.inject.Inject

@Stable
data class NoteEditUiState(
    val title: String = "",
    val content: String = "",
    val tags: List<Tag> = emptyList(),
    val selectedTags: List<Tag> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isPreview: Boolean = false,
    val isPrivate: Boolean = false,
    val error: String? = null,
    val savedNoteId: Long? = null
)

sealed class NoteEditEvent {
    data class LoadNote(val noteId: Long) : NoteEditEvent()
    data class UpdateTitle(val title: String) : NoteEditEvent()
    data class UpdateContent(val content: String) : NoteEditEvent()
    data object TogglePreview : NoteEditEvent()
    data object Save : NoteEditEvent()
    data class InsertText(val text: String) : NoteEditEvent()
    data class ToggleTag(val tag: Tag) : NoteEditEvent()
    data object LoadTags : NoteEditEvent()
    data object TogglePrivate : NoteEditEvent()
    data class CaptureImage(val bitmap: Bitmap) : NoteEditEvent()
    data class DescribeImage(val bitmap: Bitmap) : NoteEditEvent()
}

@HiltViewModel
class NoteEditViewModel @Inject constructor(
    private val getNoteUseCase: GetNoteUseCase,
    private val createNoteUseCase: CreateNoteUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val extractTextUseCase: ExtractTextUseCase,
    private val describeImageUseCase: DescribeImageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditUiState())
    val uiState: StateFlow<NoteEditUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null
    private var ocrJob: Job? = null
    private var describeImageJob: Job? = null
    private var currentNoteId: Long? = null

    suspend fun saveAndReturn(): Long? {
        val state = _uiState.value
        val noteId = currentNoteId
        return if (noteId != null) {
            if (state.title.isBlank() && state.content.isBlank()) return noteId
            updateNoteUseCase(
                Note(id = noteId, title = state.title, content = state.content,
                    tags = state.selectedTags, isPrivate = state.isPrivate)
            ).getOrNull()
            noteId
        } else {
            if (state.title.isBlank() && state.content.isBlank()) return null
            val newId = createNoteUseCase(
                title = state.title.ifBlank { "无标题" },
                content = state.content.ifBlank { "无内容" },
                tags = state.selectedTags,
                isPrivate = state.isPrivate
            ).getOrNull() ?: return null
            currentNoteId = newId
            _uiState.value = _uiState.value.copy(savedNoteId = newId)
            newId
        }
    }

    fun onEvent(event: NoteEditEvent) {
        when (event) {
            is NoteEditEvent.LoadNote -> loadNote(event.noteId)
            is NoteEditEvent.UpdateTitle -> updateTitle(event.title)
            is NoteEditEvent.UpdateContent -> updateContent(event.content)
            NoteEditEvent.TogglePreview -> togglePreview()
            NoteEditEvent.Save -> save()
            is NoteEditEvent.InsertText -> insertText(event.text)
            is NoteEditEvent.ToggleTag -> toggleTag(event.tag)
            NoteEditEvent.LoadTags -> loadTags()
            NoteEditEvent.TogglePrivate -> togglePrivate()
            is NoteEditEvent.CaptureImage -> extractTextFromImage(event.bitmap)
            is NoteEditEvent.DescribeImage -> describeImage(event.bitmap)
        }
    }

    private fun togglePrivate() {
        _uiState.value = _uiState.value.copy(isPrivate = !_uiState.value.isPrivate)
    }

    private fun loadNote(noteId: Long) {
        currentNoteId = noteId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            getNoteUseCase(noteId).first { note ->
                note?.let {
                    _uiState.value = _uiState.value.copy(
                        title = it.title,
                        content = it.content,
                        selectedTags = it.tags,
                        isPrivate = it.isPrivate,
                        isLoading = false
                    )
                } ?: run {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                true
            }
        }
        loadTags()
    }

    private fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
        scheduleAutoSave()
    }

    private fun updateContent(content: String) {
        _uiState.value = _uiState.value.copy(content = content)
        scheduleAutoSave()
    }

    private fun togglePreview() {
        _uiState.value = _uiState.value.copy(isPreview = !_uiState.value.isPreview)
    }

    private fun insertText(text: String) {
        val newContent = _uiState.value.content + text
        _uiState.value = _uiState.value.copy(content = newContent)
    }

    private fun toggleTag(tag: Tag) {
        val current = _uiState.value.selectedTags.toMutableList()
        if (current.any { it.id == tag.id }) {
            current.removeAll { it.id == tag.id }
        } else {
            current.add(tag)
        }
        _uiState.value = _uiState.value.copy(selectedTags = current)
    }

    private fun loadTags() {
        viewModelScope.launch {
            try {
                getAllTagsUseCase().collect { tags ->
                    _uiState.value = _uiState.value.copy(tags = tags)
                }
            } catch (e: Exception) { Log.w("NoteEditVM", "Failed to load tags", e) }
        }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(2000)
            save()
        }
    }

    private fun extractTextFromImage(bitmap: Bitmap) {
        ocrJob?.cancel()
        ocrJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val text = extractTextUseCase.execute(bitmap)
                val currentContent = _uiState.value.content
                val newContent = if (currentContent.isNotEmpty()) "$currentContent\n$text" else text
                _uiState.value = _uiState.value.copy(content = newContent, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }
    }

    private fun describeImage(bitmap: Bitmap) {
        describeImageJob?.cancel()
        describeImageJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val description = describeImageUseCase.execute(bitmap, AIProvider.GLM)
                val currentContent = _uiState.value.content
                val newContent = if (currentContent.isNotEmpty()) "$currentContent\n\n[图片描述]: $description" else description
                _uiState.value = _uiState.value.copy(content = newContent, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }
    }

    private fun save() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                val state = _uiState.value
                val noteId = currentNoteId
                if (noteId != null) {
                    updateNoteUseCase(
                        Note(
                            id = noteId,
                            title = state.title,
                            content = state.content,
                            tags = state.selectedTags,
                            isPrivate = state.isPrivate
                        )
                    ).getOrThrow()
                } else {
                    val newId = createNoteUseCase(
                        title = state.title,
                        content = state.content,
                        tags = state.selectedTags,
                        isPrivate = state.isPrivate
                    ).getOrThrow()
                    currentNoteId = newId
                    _uiState.value = _uiState.value.copy(savedNoteId = newId)
                }
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "保存失败"
                )
            }
        }
    }
}
