package com.nsai.notes.presentation.ai

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nsai.notes.data.local.datastore.SettingsDataStore
import com.nsai.notes.data.remote.search.SearchResult
import com.nsai.notes.data.remote.search.WebSearchService
import com.nsai.notes.domain.model.AIMode
import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.model.ChatMessage
import com.nsai.notes.domain.model.Conversation
import com.nsai.notes.domain.model.Note
import com.nsai.notes.domain.agent.ReActLoop
import com.nsai.notes.domain.rag.RetrieveContextUseCase
import com.nsai.notes.domain.repository.AIService
import com.nsai.notes.domain.repository.ConversationRepository
import com.nsai.notes.domain.repository.NoteRepository
import com.nsai.notes.domain.usecase.ai.AskAIUseCase
import com.nsai.notes.domain.usecase.note.GetAllNotesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

val GREETING_MESSAGE = ChatMessage(ChatMessage.Role.ASSISTANT,
    "你好！我是AI笔记助手。\n⚡快速模式 | 🧠思考模式 | 🎨图片生成\n📝AI写文档 | 🤖AI Agent\n选择模式开始体验！"
)

@Stable
data class AIHomeUiState(
    val recentNotes: List<Note> = emptyList(),
    val messages: List<ChatMessage> = listOf(GREETING_MESSAGE),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val currentMode: AIMode = AIMode.QUICK,
    val selectedProvider: AIProvider = AIProvider.DEEPSEEK,
    val imagePrompt: String = "",
    val generatedImage: String? = null,
    val error: String? = null,
    val isAgentMode: Boolean = false,
    val isDocGenMode: Boolean = false,
    val isWebSearchMode: Boolean = false,
    val isRagMode: Boolean = false,
    val conversationHistory: List<Conversation> = emptyList(),
    val currentConversationId: Long = 0L,
    val showHistory: Boolean = false,
    val searchResults: List<SearchResult> = emptyList(),
    val searchEngine: String = "GOOGLE",
    val searchEngineCustomUrl: String = "",
    val bookmarks: List<SettingsDataStore.Bookmark> = emptyList(),
    val searchHistory: List<String> = emptyList()
)

sealed class AIHomeEvent {
    data object LoadNotes : AIHomeEvent()
    data class UpdateInput(val text: String) : AIHomeEvent()
    data object SendMessage : AIHomeEvent()
    data class SelectMode(val mode: AIMode) : AIHomeEvent()
    data class SelectProvider(val provider: AIProvider) : AIHomeEvent()
    data class UpdateImagePrompt(val prompt: String) : AIHomeEvent()
    data object GenerateImage : AIHomeEvent()
    data object ToggleAgentMode : AIHomeEvent()
    data object ToggleDocGenMode : AIHomeEvent()
    data object ToggleWebSearch : AIHomeEvent()
    data object ToggleRagMode : AIHomeEvent()
    data object ClearError : AIHomeEvent()
    data object LoadHistory : AIHomeEvent()
    data object NewConversation : AIHomeEvent()
    data class LoadConversation(val conversation: Conversation) : AIHomeEvent()
    data class DeleteConversation(val id: Long) : AIHomeEvent()
    data object ToggleHistory : AIHomeEvent()
    data class AddBookmark(val title: String, val url: String) : AIHomeEvent()
    data class RemoveBookmark(val url: String) : AIHomeEvent()
    data class AddSearchHistory(val query: String) : AIHomeEvent()
    data object ClearSearchHistory : AIHomeEvent()
    data class SetSearchEngine(val engine: String) : AIHomeEvent()
    data class SetSearchEngineCustomUrl(val url: String) : AIHomeEvent()
}

@HiltViewModel
class AIHomeViewModel @Inject constructor(
    private val getAllNotesUseCase: GetAllNotesUseCase,
    private val askAIUseCase: AskAIUseCase,
    private val aiService: AIService,
    private val noteRepository: NoteRepository,
    private val settingsDataStore: SettingsDataStore,
    private val conversationRepository: ConversationRepository,
    private val webSearchService: WebSearchService,
    private val retrieveContextUseCase: RetrieveContextUseCase,
    private val reActLoop: ReActLoop
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIHomeUiState())
    val uiState: StateFlow<AIHomeUiState> = _uiState.asStateFlow()

    init {
        loadNotes()
        loadSelectedProvider()
        loadHistory()
        loadSearchEngine()
        loadBookmarksAndHistory()
    }

    private fun loadSearchEngine() {
        viewModelScope.launch {
            settingsDataStore.searchEngineFlow.collect { engine ->
                _uiState.value = _uiState.value.copy(searchEngine = engine)
            }
        }
        viewModelScope.launch {
            settingsDataStore.searchEngineCustomUrlFlow.collect { url ->
                _uiState.value = _uiState.value.copy(searchEngineCustomUrl = url)
            }
        }
    }

    private fun loadSelectedProvider() {
        viewModelScope.launch {
            settingsDataStore.selectedProvider.first()?.let { provider ->
                _uiState.value = _uiState.value.copy(selectedProvider = provider)
            }
        }
    }

    suspend fun getSearchEngine(): String = settingsDataStore.getSearchEngine()

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

    fun onEvent(event: AIHomeEvent) {
        when (event) {
            AIHomeEvent.LoadNotes -> loadNotes()
            is AIHomeEvent.UpdateInput -> _uiState.value = _uiState.value.copy(inputText = event.text)
            AIHomeEvent.SendMessage -> sendMessage()
            is AIHomeEvent.SelectMode -> selectMode(event.mode)
            is AIHomeEvent.SelectProvider -> _uiState.value = _uiState.value.copy(selectedProvider = event.provider)
            is AIHomeEvent.UpdateImagePrompt -> _uiState.value = _uiState.value.copy(imagePrompt = event.prompt)
            AIHomeEvent.GenerateImage -> generateImage()
            AIHomeEvent.ToggleAgentMode -> toggleAgent()
            AIHomeEvent.ToggleDocGenMode -> toggleDocGen()
            AIHomeEvent.ToggleWebSearch -> _uiState.value = _uiState.value.copy(
                isWebSearchMode = !_uiState.value.isWebSearchMode,
                isAgentMode = false, isDocGenMode = false
            )
            AIHomeEvent.ToggleRagMode -> _uiState.value = _uiState.value.copy(
                isRagMode = !_uiState.value.isRagMode,
                isWebSearchMode = false, isAgentMode = false, isDocGenMode = false
            )
            AIHomeEvent.ClearError -> _uiState.value = _uiState.value.copy(error = null)
            AIHomeEvent.LoadHistory -> loadHistory()
            AIHomeEvent.NewConversation -> newConversation()
            is AIHomeEvent.LoadConversation -> loadConversation(event.conversation)
            is AIHomeEvent.DeleteConversation -> deleteConversation(event.id)
            AIHomeEvent.ToggleHistory -> _uiState.value = _uiState.value.copy(showHistory = !_uiState.value.showHistory)
            is AIHomeEvent.AddBookmark -> addBookmark(event.title, event.url)
            is AIHomeEvent.RemoveBookmark -> removeBookmark(event.url)
            is AIHomeEvent.AddSearchHistory -> addSearchHistory(event.query)
            AIHomeEvent.ClearSearchHistory -> clearSearchHistory()
            is AIHomeEvent.SetSearchEngine -> setSearchEngine(event.engine)
            is AIHomeEvent.SetSearchEngineCustomUrl -> setSearchEngineCustomUrl(event.url)
        }
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

    private fun setSearchEngine(engine: String) {
        _uiState.value = _uiState.value.copy(searchEngine = engine)
        viewModelScope.launch { settingsDataStore.setSearchEngine(engine) }
    }

    private fun setSearchEngineCustomUrl(url: String) {
        _uiState.value = _uiState.value.copy(searchEngineCustomUrl = url)
        viewModelScope.launch { settingsDataStore.setSearchEngineCustomUrl(url) }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            conversationRepository.getAll().collect { list ->
                _uiState.value = _uiState.value.copy(conversationHistory = list)
            }
        }
    }

    private fun newConversation() {
        _uiState.value = _uiState.value.copy(
            messages = listOf(GREETING_MESSAGE),
            currentConversationId = 0L,
            showHistory = false
        )
    }

    private fun loadConversation(conversation: Conversation) {
        _uiState.value = _uiState.value.copy(
            messages = conversation.messages.ifEmpty { listOf(GREETING_MESSAGE) },
            currentConversationId = conversation.id,
            showHistory = false
        )
    }

    private fun deleteConversation(id: Long) {
        viewModelScope.launch {
            conversationRepository.delete(id)
            if (_uiState.value.currentConversationId == id) {
                newConversation()
            }
        }
    }

    private fun saveCurrentConversation() {
        val state = _uiState.value
        val messages = state.messages
        if (messages.size <= 1) return // don't save empty conversations
        val title = messages.firstOrNull { it.role == ChatMessage.Role.USER }?.content?.take(40) ?: "新对话"
        viewModelScope.launch {
            val conv = Conversation(
                id = state.currentConversationId,
                title = title,
                messages = messages
            )
            val newId = conversationRepository.save(conv)
            _uiState.value = _uiState.value.copy(currentConversationId = newId)
        }
    }

    private fun loadNotes() {
        viewModelScope.launch { try { _uiState.value = _uiState.value.copy(recentNotes = getAllNotesUseCase().first().take(10)) } catch (_: Exception) {} }
    }

    private fun selectMode(mode: AIMode) {
        _uiState.value = _uiState.value.copy(currentMode = mode, imagePrompt = "", generatedImage = null, isAgentMode = false, isDocGenMode = false)
    }

    private fun toggleAgent() {
        _uiState.value = _uiState.value.copy(isAgentMode = !_uiState.value.isAgentMode, isDocGenMode = false, currentMode = AIMode.QUICK)
        if (_uiState.value.isAgentMode) {
            _uiState.value = _uiState.value.copy(
                messages = listOf(ChatMessage(ChatMessage.Role.ASSISTANT, "🤖 AI Agent 已激活！\n\n你可以命令我：\n• 「创建笔记 标题XXX 内容XXX」\n• 「搜索包含XXX的笔记」\n• 「列出所有笔记」\n• 「帮我记备忘 标题XXX」"))
            )
        }
    }

    private fun toggleDocGen() {
        _uiState.value = _uiState.value.copy(isDocGenMode = !_uiState.value.isDocGenMode, isAgentMode = false, currentMode = AIMode.QUICK)
        if (_uiState.value.isDocGenMode) {
            _uiState.value = _uiState.value.copy(
                messages = listOf(ChatMessage(ChatMessage.Role.ASSISTANT, "📝 AI文档生成器已激活！\n\n请描述你需要的文档：\n• 「写一份AI人工智能介绍」\n• 「生成一份周报模板」\n• 「帮我写读书笔记 书名叫XXX」"))
            )
        }
    }

    private fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        val searchPrefix = if (_uiState.value.isWebSearchMode)
            "[联网搜索模式] 请基于你的知识尽可能搜索并提供最新信息来回答：\n" else ""
        val augmentedText = searchPrefix + text
        val userMsg = ChatMessage(ChatMessage.Role.USER, augmentedText)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMsg, inputText = "",
            isLoading = true, error = null, searchResults = emptyList()
        )

        // Perform web search if in search mode
        if (_uiState.value.isWebSearchMode) {
            viewModelScope.launch {
                val results = webSearchService.search(text)
                _uiState.value = _uiState.value.copy(searchResults = results)
            }
        }

        viewModelScope.launch {
            try {
                if (_uiState.value.isAgentMode) {
                    try {
                        val result = reActLoop.execute(text, _uiState.value.selectedProvider)
                        _uiState.value = _uiState.value.copy(
                            messages = _uiState.value.messages + ChatMessage(ChatMessage.Role.ASSISTANT, result),
                            isLoading = false
                        )
                        saveCurrentConversation()
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Agent执行失败")
                    }
                    return@launch
                } else if (_uiState.value.isDocGenMode) {
                    generateDocument(text)
                } else {
                    val finalQuestion = if (_uiState.value.isRagMode) {
                        val context = retrieveContextUseCase.retrieve(text)
                        if (context.isNotBlank()) "[知识库上下文]\n$context\n\n[用户问题]\n$text" else text
                    } else text
                    val result = askAIUseCase(
                        question = finalQuestion,
                        provider = _uiState.value.selectedProvider,
                        mode = _uiState.value.currentMode
                    )
                    result.fold(
                        onSuccess = { response ->
                            _uiState.value = _uiState.value.copy(
                                messages = _uiState.value.messages + ChatMessage(
                                    ChatMessage.Role.ASSISTANT, response.content,
                                    reasoningContent = response.reasoning,
                                    isThinking = _uiState.value.currentMode == AIMode.THINK
                                ), isLoading = false)
                            saveCurrentConversation()
                        },
                        onFailure = { e ->
                            _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "请求失败")
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "请求失败")
            }
        }
    }

    private fun executeAgentCommand(cmd: String) {
        viewModelScope.launch {
            try {
                val result = when {
                    Regex("(创建|新建)(笔记|备忘)").containsMatchIn(cmd) -> executeCreateNote(cmd)
                    Regex("(搜索|查找)\\p{IsHan}").containsMatchIn(cmd) -> executeSearch(cmd)
                    Regex("(列出|显示|查看).*(笔记|所有)").containsMatchIn(cmd) -> executeListNotes()
                    else -> "未识别的命令，请尝试：\n• 创建笔记 标题XXX 内容XXX\n• 搜索XXX\n• 列出所有笔记"
                }
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + ChatMessage(ChatMessage.Role.ASSISTANT, result), isLoading = false)
                saveCurrentConversation()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "执行失败")
            }
        }
    }

    private suspend fun executeCreateNote(cmd: String): String {
        val title = extractField(cmd, "标题")
        val content = if (cmd.contains("内容")) extractField(cmd, "内容") else cmd.substringAfterLast("内容").trim()
        if (title.isBlank() || content.isBlank()) return "请使用格式：创建笔记 标题XXX 内容XXX"
        noteRepository.createNote(Note(title = title, content = content))
        loadNotes()
        return "已创建笔记「$title」"
    }

    private suspend fun executeSearch(cmd: String): String {
        val keyword = cmd.substringAfter("搜索").substringBefore("的笔记").substringBefore("的")
            .ifBlank { cmd.substringAfter("查找").trim() }.trim()
        if (keyword.isBlank()) return "请输入要搜索的关键词"
        val notes = noteRepository.searchNotes(keyword).first()
        return if (notes.isEmpty()) "未找到包含「$keyword」的笔记"
        else "找到${notes.size}篇笔记：\n${notes.take(5).joinToString("\n") { "• ${it.title}" }}"
    }

    private suspend fun executeListNotes(): String {
        val notes = noteRepository.getAllNotes().first()
        return if (notes.isEmpty()) "还没有笔记"
        else "共${notes.size}篇笔记：\n${notes.take(10).joinToString("\n") { "• ${it.title}" }}"
    }

    private fun extractField(cmd: String, fieldName: String): String {
        val after = cmd.substringAfter(fieldName)
        return after.removePrefix(":").removePrefix("：").trim()
            .split("内容", "：", ":", "\n").firstOrNull()?.trim() ?: after.take(100).trim()
    }

    private fun generateDocument(topic: String) {
        viewModelScope.launch {
            try {
                val msgs = listOf(
                    ChatMessage(ChatMessage.Role.SYSTEM, "你是一个专业的文档生成助手。根据用户描述生成一篇完整的文档，格式整洁，包含标题和章节。"),
                    ChatMessage(ChatMessage.Role.USER, "请生成以下文档：$topic")
                )
                val response = aiService.chat(_uiState.value.selectedProvider, msgs + _uiState.value.messages.takeLast(2))
                val content = response.content
                noteRepository.createNote(Note(title = topic.take(50), content = content))
                loadNotes()
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + ChatMessage(ChatMessage.Role.ASSISTANT, "已生成并保存文档：「${topic.take(50)}」\n\n$content"), isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "生成失败")
            }
        }
    }

    private fun generateImage() {
        val prompt = _uiState.value.imagePrompt.trim()
        if (prompt.isBlank()) return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null, generatedImage = null)
        viewModelScope.launch {
            try {
                if (!_uiState.value.selectedProvider.supportsImage) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "当前模型不支持，请切换GLM 5.1")
                    return@launch
                }
                val response = aiService.generateImage(_uiState.value.selectedProvider, prompt)
                _uiState.value = _uiState.value.copy(isLoading = false, generatedImage = response.content)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "生成失败")
            }
        }
    }
}
