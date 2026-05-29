package com.nsai.notes.presentation.tags

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nsai.notes.domain.model.Tag
import com.nsai.notes.domain.usecase.tag.CreateTagUseCase
import com.nsai.notes.domain.usecase.tag.DeleteTagUseCase
import com.nsai.notes.domain.usecase.tag.GetAllTagsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
data class TagManageUiState(
    val tags: List<Tag> = emptyList(),
    val newTagName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class TagManageEvent {
    data object LoadTags : TagManageEvent()
    data class UpdateNewTagName(val name: String) : TagManageEvent()
    data object CreateTag : TagManageEvent()
    data class DeleteTag(val tagId: Long) : TagManageEvent()
}

@HiltViewModel
class TagManageViewModel @Inject constructor(
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val createTagUseCase: CreateTagUseCase,
    private val deleteTagUseCase: DeleteTagUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagManageUiState())
    val uiState: StateFlow<TagManageUiState> = _uiState.asStateFlow()

    init {
        loadTags()
    }

    fun onEvent(event: TagManageEvent) {
        when (event) {
            TagManageEvent.LoadTags -> loadTags()
            is TagManageEvent.UpdateNewTagName -> updateName(event.name)
            TagManageEvent.CreateTag -> createTag()
            is TagManageEvent.DeleteTag -> deleteTag(event.tagId)
        }
    }

    private fun loadTags() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                getAllTagsUseCase().collect { tags ->
                    _uiState.value = _uiState.value.copy(tags = tags, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(newTagName = name)
    }

    private fun createTag() {
        val name = _uiState.value.newTagName.trim()
        if (name.isBlank()) return

        viewModelScope.launch {
            createTagUseCase(name).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(newTagName = "")
                    loadTags()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            )
        }
    }

    private fun deleteTag(tagId: Long) {
        viewModelScope.launch {
            deleteTagUseCase(tagId).fold(
                onSuccess = { loadTags() },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            )
        }
    }
}
