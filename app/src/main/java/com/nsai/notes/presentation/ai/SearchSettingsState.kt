package com.nsai.notes.presentation.ai

import androidx.compose.runtime.Stable
import com.nsai.notes.data.local.datastore.SettingsDataStore
import com.nsai.notes.domain.model.SearchEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Stable
data class SearchSettingsState(
    val searchEngine: String = SearchEngine.BING.name,
    val searchEngineCustomUrl: String = "",
    val bookmarks: List<SettingsDataStore.Bookmark> = emptyList(),
    val searchHistory: List<String> = emptyList()
)

/**
 * Shared delegate for search engine, bookmark, and search history state.
 * Injected into ViewModels that need search-related UI state.
 */
@Singleton
class SearchSettingsDelegate @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {
    private val _state = MutableStateFlow(SearchSettingsState())
    val state: StateFlow<SearchSettingsState> = _state.asStateFlow()

    fun load(scope: CoroutineScope) {
        scope.launch {
            val engine = settingsDataStore.getSearchEngine()
            val customUrl = settingsDataStore.getSearchEngineCustomUrl()
            val bookmarks = settingsDataStore.getBookmarks()
            val history = settingsDataStore.getSearchHistory()
            _state.value = _state.value.copy(
                searchEngine = engine,
                searchEngineCustomUrl = customUrl,
                bookmarks = bookmarks,
                searchHistory = history
            )
        }
    }

    fun setSearchEngine(scope: CoroutineScope, engine: String) {
        _state.value = _state.value.copy(searchEngine = engine)
        scope.launch { settingsDataStore.setSearchEngine(engine) }
    }

    fun setSearchEngineCustomUrl(scope: CoroutineScope, url: String) {
        _state.value = _state.value.copy(searchEngineCustomUrl = url)
        scope.launch { settingsDataStore.setSearchEngineCustomUrl(url) }
    }

    fun addBookmark(scope: CoroutineScope, title: String, url: String) {
        scope.launch {
            settingsDataStore.addBookmark(title, url)
            _state.value = _state.value.copy(bookmarks = settingsDataStore.getBookmarks())
        }
    }

    fun removeBookmark(scope: CoroutineScope, url: String) {
        scope.launch {
            settingsDataStore.removeBookmark(url)
            _state.value = _state.value.copy(bookmarks = settingsDataStore.getBookmarks())
        }
    }

    fun addSearchHistory(scope: CoroutineScope, query: String) {
        scope.launch {
            settingsDataStore.addSearchHistory(query)
            _state.value = _state.value.copy(searchHistory = settingsDataStore.getSearchHistory())
        }
    }

    fun clearSearchHistory(scope: CoroutineScope) {
        scope.launch {
            settingsDataStore.clearSearchHistory()
            _state.value = _state.value.copy(searchHistory = emptyList())
        }
    }
}
