package com.nsai.notes.presentation.ai

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nsai.notes.data.local.datastore.SettingsDataStore
import com.nsai.notes.data.remote.search.SearchResult
import com.nsai.notes.data.remote.search.WebSearchService
import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.model.AIMode
import com.nsai.notes.domain.model.ChatMessage
import com.nsai.notes.domain.usecase.ai.AskAIUseCase
import com.nsai.notes.domain.usecase.ai.SummarizeNoteUseCase
import com.nsai.notes.domain.usecase.note.GetNoteUseCase
import com.nsai.notes.domain.repository.AIService
import com.nsai.notes.domain.repository.NoteRepository
import com.nsai.notes.domain.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
data class AIChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val selectedProvider: AIProvider = AIProvider.DEEPSEEK,
    val summary: String? = null,
    val isSummarizing: Boolean = false,
    val error: String? = null,
    val searchEngine: String = "BING",
    val searchEngineCustomUrl: String = "",
    val bookmarks: List<SettingsDataStore.Bookmark> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val currentMode: AIMode = AIMode.QUICK,
    val isWebSearchMode: Boolean = false,
    val searchResults: List<SearchResult> = emptyList(),
    val imagePrompt: String = "",
    val generatedImage: String? = null,
    val currentImageSize: String = "1024x1024"
)

sealed class AIChatEvent {
    data class LoadNote(val noteId: Long) : AIChatEvent()
    data class UpdateInput(val text: String) : AIChatEvent()
    data object SendMessage : AIChatEvent()
    data object Summarize : AIChatEvent()
    data class SelectProvider(val provider: AIProvider) : AIChatEvent()
    data class SelectMode(val mode: AIMode) : AIChatEvent()
    data class UpdateImagePrompt(val prompt: String) : AIChatEvent()
    data object GenerateImage : AIChatEvent()
    data class SelectImageSize(val size: String) : AIChatEvent()
    data object ToggleWebSearch : AIChatEvent()
    data object ClearError : AIChatEvent()
    data object DismissSummary : AIChatEvent()
    data class SetSearchEngine(val engine: String) : AIChatEvent()
    data class SetSearchEngineCustomUrl(val url: String) : AIChatEvent()
    data class AddBookmark(val title: String, val url: String) : AIChatEvent()
    data class RemoveBookmark(val url: String) : AIChatEvent()
    data class AddSearchHistory(val query: String) : AIChatEvent()
    data object ClearSearchHistory : AIChatEvent()
    data class AppendToNote(val text: String) : AIChatEvent()
}

@HiltViewModel
class AIChatViewModel @Inject constructor(
    private val askAIUseCase: AskAIUseCase,
    private val summarizeNoteUseCase: SummarizeNoteUseCase,
    private val getNoteUseCase: GetNoteUseCase,
    private val noteRepository: NoteRepository,
    private val settingsDataStore: SettingsDataStore,
    private val webSearchService: WebSearchService,
    private val aiService: AIService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIChatUiState())
    val uiState: StateFlow<AIChatUiState> = _uiState.asStateFlow()

    private var noteId: Long? = null
    private var sendJob: Job? = null

    init {
        loadSelectedProvider()
        loadSearchEngine()
        loadBookmarksAndHistory()
    }

    private fun loadSelectedProvider() {
        viewModelScope.launch {
            settingsDataStore.selectedProvider.first()?.let { provider ->
                _uiState.value = _uiState.value.copy(selectedProvider = provider)
            }
        }
    }

    private fun loadSearchEngine() {
        viewModelScope.launch {
            val engine = settingsDataStore.getSearchEngine()
            val customUrl = settingsDataStore.getSearchEngineCustomUrl()
            _uiState.value = _uiState.value.copy(
                searchEngine = engine,
                searchEngineCustomUrl = customUrl
            )
        }
    }

    private fun loadBookmarksAndHistory() {
        viewModelScope.launch {
            val bookmarks = settingsDataStore.getBookmarks()
            val history = settingsDataStore.getSearchHistory()
            _uiState.value = _uiState.value.copy(
                bookmarks = bookmarks,
                searchHistory = history
            )
        }
    }

    fun onEvent(event: AIChatEvent) {
        when (event) {
            is AIChatEvent.LoadNote -> loadNote(event.noteId)
            is AIChatEvent.UpdateInput -> updateInput(event.text)
            AIChatEvent.SendMessage -> sendMessage()
            AIChatEvent.Summarize -> summarize()
            is AIChatEvent.SelectProvider -> selectProvider(event.provider)
            AIChatEvent.ClearError -> clearError()
            AIChatEvent.DismissSummary -> _uiState.value = _uiState.value.copy(summary = null, isSummarizing = false)
            is AIChatEvent.SetSearchEngine -> setSearchEngine(event.engine)
            is AIChatEvent.SetSearchEngineCustomUrl -> setSearchEngineCustomUrl(event.url)
            is AIChatEvent.AddBookmark -> addBookmark(event.title, event.url)
            is AIChatEvent.RemoveBookmark -> removeBookmark(event.url)
            is AIChatEvent.AddSearchHistory -> addSearchHistory(event.query)
            AIChatEvent.ClearSearchHistory -> clearSearchHistory()
            is AIChatEvent.AppendToNote -> appendToNote(event.text)
            is AIChatEvent.SelectMode -> selectMode(event.mode)
            AIChatEvent.ToggleWebSearch -> toggleWebSearch()
            is AIChatEvent.UpdateImagePrompt -> updateImagePrompt(event.prompt)
            AIChatEvent.GenerateImage -> generateImage()
            is AIChatEvent.SelectImageSize -> selectImageSize(event.size)
        }
    }

    private fun selectMode(mode: AIMode) {
        _uiState.value = _uiState.value.copy(currentMode = mode)
    }

    private fun updateImagePrompt(prompt: String) {
        _uiState.value = _uiState.value.copy(imagePrompt = prompt)
    }

    private fun selectImageSize(size: String) {
        _uiState.value = _uiState.value.copy(currentImageSize = size)
    }

    private fun generateImage() {
        val prompt = _uiState.value.imagePrompt.trim()
        if (prompt.isBlank()) return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null, generatedImage = null)
        sendJob?.cancel()
        sendJob = viewModelScope.launch {
            try {
                if (!_uiState.value.selectedProvider.supportsImage) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "当前模型不支持图片生成")
                    return@launch
                }
                val response = aiService.generateImage(_uiState.value.selectedProvider, prompt)
                _uiState.value = _uiState.value.copy(isLoading = false, generatedImage = response.content)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "生成失败")
            }
        }
    }

    private fun toggleWebSearch() {
        val wasActive = _uiState.value.isWebSearchMode
        _uiState.value = _uiState.value.copy(
            isWebSearchMode = !wasActive,
            searchResults = if (wasActive) emptyList() else _uiState.value.searchResults
        )
    }

    private fun appendToNote(text: String) {
        val id = noteId ?: return
        viewModelScope.launch {
            val note = getNoteUseCase(id).first() ?: return@launch
            noteRepository.updateNote(note.copy(
                content = if (note.content.isNotBlank()) "${note.content}\n\n$text" else text,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    private fun setSearchEngine(engine: String) {
        _uiState.value = _uiState.value.copy(searchEngine = engine)
        viewModelScope.launch { settingsDataStore.setSearchEngine(engine) }
    }

    private fun setSearchEngineCustomUrl(url: String) {
        _uiState.value = _uiState.value.copy(searchEngineCustomUrl = url)
        viewModelScope.launch { settingsDataStore.setSearchEngineCustomUrl(url) }
    }

    private fun addBookmark(title: String, url: String) {
        viewModelScope.launch {
            settingsDataStore.addBookmark(title, url)
            _uiState.value = _uiState.value.copy(bookmarks = settingsDataStore.getBookmarks())
        }
    }

    private fun removeBookmark(url: String) {
        viewModelScope.launch {
            settingsDataStore.removeBookmark(url)
            _uiState.value = _uiState.value.copy(bookmarks = settingsDataStore.getBookmarks())
        }
    }

    private fun addSearchHistory(query: String) {
        viewModelScope.launch {
            settingsDataStore.addSearchHistory(query)
            _uiState.value = _uiState.value.copy(searchHistory = settingsDataStore.getSearchHistory())
        }
    }

    private fun clearSearchHistory() {
        viewModelScope.launch {
            settingsDataStore.clearSearchHistory()
            _uiState.value = _uiState.value.copy(searchHistory = emptyList())
        }
    }

    suspend fun getEnabledProviders(): List<AIProvider> {
        return settingsDataStore.getAllProviderConfigs().first()
            .filter { it.isEnabled }.map { it.provider }
    }

    private fun loadNote(id: Long) {
        noteId = id
        viewModelScope.launch {
            val note = runCatching { getNoteUseCase(id).first() }.getOrNull()
            if (note != null) {
                _uiState.value = _uiState.value.copy(
                    messages = listOf(
                        ChatMessage(
                            role = ChatMessage.Role.ASSISTANT,
                            content = "你好！我是AI笔记助手。关于「${note.title}」这篇笔记，有什么我可以帮你的吗？"
                        )
                    )
                )
            }
        }
    }

    private fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    private fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        sendJob?.cancel()

        val userMessage = ChatMessage(role = ChatMessage.Role.USER, content = text)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            inputText = "",
            isLoading = true,
            error = null
        )

        sendJob = viewModelScope.launch {
            try {
                // Perform web search first if in search mode
            var searchContext = ""
            if (_uiState.value.isWebSearchMode) {
                val sResults = webSearchService.search(text, com.nsai.notes.domain.model.SearchEngine.fromName(_uiState.value.searchEngine))
                _uiState.value = _uiState.value.copy(searchResults = sResults)
                if (sResults.isNotEmpty()) {
                    searchContext = buildString {
                        appendLine("以下是联网搜索的结果，请基于这些信息回答用户问题：")
                        sResults.take(5).forEachIndexed { i, r ->
                            appendLine("${i + 1}. ${r.title}")
                            appendLine("   ${r.snippet.take(200)}")
                        }
                        appendLine()
                    }
                }
            }

            val finalQuestion = if (searchContext.isNotBlank()) "$searchContext\n[用户问题]\n$text" else text
            val note = noteId?.let { runCatching { getNoteUseCase(it).first() }.getOrNull() }
            val result = askAIUseCase(
                question = finalQuestion,
                provider = _uiState.value.selectedProvider,
                noteContext = note,
                mode = _uiState.value.currentMode
            )
            result.fold(
                onSuccess = { response ->
                    val assistantMessage = ChatMessage(
                        role = ChatMessage.Role.ASSISTANT,
                        content = response.content,
                        reasoningContent = response.reasoning,
                        isThinking = _uiState.value.currentMode == AIMode.THINK
                    )
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + assistantMessage,
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "AI请求失败"
                    )
                }
            )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "AI请求失败"
                )
            }
        }
    }

    private fun summarize() {
        val id = noteId ?: return
        _uiState.value = _uiState.value.copy(isSummarizing = true, error = null)

        viewModelScope.launch {
            val result = summarizeNoteUseCase(id, _uiState.value.selectedProvider)
            result.fold(
                onSuccess = { summary ->
                    _uiState.value = _uiState.value.copy(
                        summary = summary,
                        isSummarizing = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSummarizing = false,
                        error = e.message ?: "摘要生成失败"
                    )
                }
            )
        }
    }

    private fun selectProvider(provider: AIProvider) {
        _uiState.value = _uiState.value.copy(selectedProvider = provider)
        viewModelScope.launch { settingsDataStore.setSelectedProvider(provider) }
    }

    private fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
