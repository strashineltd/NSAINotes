package com.nsai.notes.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nsai.notes.domain.model.AIProvider
import com.nsai.notes.domain.model.AIProviderConfig
import com.nsai.notes.domain.model.MCPServer
import com.nsai.notes.domain.model.SkillPlugin
import kotlinx.coroutines.flow.Flow
import android.util.Log
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.nsai.notes.data.local.security.KeyStoreManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val keyStore: KeyStoreManager
) {

    private val mutex = Mutex()
    private object Keys {
        val SELECTED_PROVIDER = stringPreferencesKey("selected_ai_provider")
        fun apiKeyKey(provider: String) = stringPreferencesKey("api_key_$provider")
        fun baseUrlKey(provider: String) = stringPreferencesKey("base_url_$provider")
        fun enabledKey(provider: String) = stringPreferencesKey("enabled_$provider")
        val THEME_MODE = intPreferencesKey("theme_mode")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val PRIVACY_ACCEPTED = booleanPreferencesKey("privacy_accepted")
        val PRIVACY_PIN = stringPreferencesKey("privacy_pin")
        val MCP_SERVERS = stringPreferencesKey("mcp_servers_json")
        val SKILL_PLUGINS = stringPreferencesKey("skill_plugins_json")
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val SEARCH_ENGINE_CUSTOM_URL = stringPreferencesKey("search_engine_custom_url")
        val SEARCH_HISTORY = stringPreferencesKey("search_history_json")
        val BOOKMARKS = stringPreferencesKey("bookmarks_json")
    }

    private val gson = Gson()

    // --- MCP Server storage ---
    val mcpServers: Flow<List<MCPServer>> = dataStore.data.map { prefs ->
        val json = prefs[Keys.MCP_SERVERS] ?: return@map emptyList()
        try { gson.fromJson(json, Array<MCPServer>::class.java).toList() }
        catch (_: com.google.gson.JsonSyntaxException) { emptyList() }
    }

    suspend fun saveMCPServers(servers: List<MCPServer>) {
        dataStore.edit { prefs ->
            prefs[Keys.MCP_SERVERS] = gson.toJson(servers)
        }
    }

    suspend fun addMCPServer(server: MCPServer) = mutex.withLock {
        val servers = mcpServers.first().toMutableList()
        servers.add(server)
        saveMCPServers(servers)
    }

    suspend fun updateMCPServer(server: MCPServer) = mutex.withLock {
        val servers = mcpServers.first().map { if (it.id == server.id) server else it }
        saveMCPServers(servers)
    }

    suspend fun deleteMCPServer(id: String) = mutex.withLock {
        val servers = mcpServers.first().filter { it.id != id }
        saveMCPServers(servers)
    }

    // --- Skill Plugin storage ---
    val skillPlugins: Flow<List<SkillPlugin>> = dataStore.data.map { prefs ->
        val json = prefs[Keys.SKILL_PLUGINS] ?: return@map emptyList()
        try { gson.fromJson(json, Array<SkillPlugin>::class.java).toList() }
        catch (_: com.google.gson.JsonSyntaxException) { emptyList() }
    }

    suspend fun saveSkillPlugins(skills: List<SkillPlugin>) {
        dataStore.edit { prefs ->
            prefs[Keys.SKILL_PLUGINS] = gson.toJson(skills)
        }
    }

    suspend fun addSkillPlugin(skill: SkillPlugin) = mutex.withLock {
        val skills = skillPlugins.first().toMutableList()
        skills.add(skill)
        saveSkillPlugins(skills)
    }

    suspend fun updateSkillPlugin(skill: SkillPlugin) = mutex.withLock {
        val skills = skillPlugins.first().map { if (it.id == skill.id) skill else it }
        saveSkillPlugins(skills)
    }

    suspend fun deleteSkillPlugin(id: String) = mutex.withLock {
        val skills = skillPlugins.first().filter { it.id != id }
        saveSkillPlugins(skills)
    }

    val selectedProvider: Flow<AIProvider?> = dataStore.data.map { prefs ->
        prefs[Keys.SELECTED_PROVIDER]?.let { name ->
            try { AIProvider.valueOf(name) } catch (_: IllegalArgumentException) { null }
        }
    }

    suspend fun getSelectedProvider(): AIProvider? = selectedProvider.first()

    suspend fun setSelectedProvider(provider: AIProvider) {
        dataStore.edit { prefs -> prefs[Keys.SELECTED_PROVIDER] = provider.name }
    }

    suspend fun saveProviderConfig(config: AIProviderConfig) {
        dataStore.edit { prefs ->
            val key = config.provider.name
            prefs[Keys.apiKeyKey(key)] = if (config.apiKey.isNotEmpty()) keyStore.encryptToString(config.apiKey) else ""
            prefs[Keys.baseUrlKey(key)] = config.baseUrl
            prefs[Keys.enabledKey(key)] = if (config.isEnabled) "true" else "false"
        }
    }

    suspend fun getProviderConfig(provider: AIProvider): AIProviderConfig {
        val prefs = dataStore.data.first()
        val key = provider.name
        val encryptedKey = prefs[Keys.apiKeyKey(key)] ?: ""
        val apiKey = if (encryptedKey.isNotEmpty()) {
            runCatching { keyStore.decryptFromString(encryptedKey) }
                .onFailure { Log.e("SettingsDataStore", "Failed to decrypt API key for ${provider.name}", it) }
                .getOrElse { "" }
        } else ""
        return AIProviderConfig(
            provider = provider,
            apiKey = apiKey,
            baseUrl = prefs[Keys.baseUrlKey(key)] ?: provider.defaultBaseUrl,
            isEnabled = prefs[Keys.enabledKey(key)] == "true"
        )
    }

    suspend fun isApiKeyConfigured(provider: AIProvider): Boolean {
        val key = provider.name
        val prefs = dataStore.data.first()
        return !prefs[Keys.apiKeyKey(key)].isNullOrEmpty()
    }

    fun getAllProviderConfigs(): Flow<List<AIProviderConfig>> {
        return dataStore.data.map { prefs ->
            AIProvider.entries.map { provider ->
                val key = provider.name
                val hasKey = !prefs[Keys.apiKeyKey(key)].isNullOrEmpty()
                AIProviderConfig(
                    provider = provider,
                    apiKey = if (hasKey) PLACEHOLDER_API_KEY else "",
                    baseUrl = prefs[Keys.baseUrlKey(key)] ?: provider.defaultBaseUrl,
                    isEnabled = prefs[Keys.enabledKey(key)] == "true"
                )
            }
        }
    }

    companion object {
        const val PLACEHOLDER_API_KEY = "••••••••"
    }

    val themeMode: Flow<Int> = dataStore.data.map { prefs -> prefs[Keys.THEME_MODE] ?: 0 }

    suspend fun setThemeMode(mode: Int) {
        dataStore.edit { prefs -> prefs[Keys.THEME_MODE] = mode }
    }

    val fontScale: Flow<Float> = dataStore.data.map { prefs -> prefs[Keys.FONT_SCALE] ?: 1.0f }

    suspend fun setFontScale(scale: Float) {
        dataStore.edit { prefs -> prefs[Keys.FONT_SCALE] = scale }
    }

    suspend fun isPrivacyAccepted(): Boolean {
        return dataStore.data.first()[Keys.PRIVACY_ACCEPTED] ?: false
    }

    suspend fun acceptPrivacy() {
        dataStore.edit { prefs -> prefs[Keys.PRIVACY_ACCEPTED] = true }
    }

    suspend fun getPrivacyPin(): String {
        val encrypted = dataStore.data.first()[Keys.PRIVACY_PIN] ?: ""
        return if (encrypted.isNotEmpty()) {
            runCatching { keyStore.decryptFromString(encrypted) }
                .onFailure { Log.e("SettingsDataStore", "Failed to decrypt privacy PIN", it) }
                .getOrElse { "" }
        } else ""
    }

    suspend fun setPrivacyPin(pin: String) {
        dataStore.edit { prefs -> prefs[Keys.PRIVACY_PIN] = if (pin.isNotEmpty()) keyStore.encryptToString(pin) else "" }
    }

    // --- Search Engine ---
    val searchEngineFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.SEARCH_ENGINE] ?: "BAIDU"
    }

    val searchEngineCustomUrlFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.SEARCH_ENGINE_CUSTOM_URL] ?: ""
    }

    suspend fun getSearchEngine(): String = dataStore.data.first()[Keys.SEARCH_ENGINE] ?: "BAIDU"

    suspend fun setSearchEngine(engine: String) {
        dataStore.edit { prefs -> prefs[Keys.SEARCH_ENGINE] = engine }
    }

    suspend fun getSearchEngineCustomUrl(): String =
        dataStore.data.first()[Keys.SEARCH_ENGINE_CUSTOM_URL] ?: ""

    suspend fun setSearchEngineCustomUrl(url: String) {
        dataStore.edit { prefs -> prefs[Keys.SEARCH_ENGINE_CUSTOM_URL] = url }
    }

    // --- Search History ---
    suspend fun getSearchHistory(): List<String> {
        val json = dataStore.data.first()[Keys.SEARCH_HISTORY] ?: return emptyList()
        return try { gson.fromJson(json, Array<String>::class.java).toList() } catch (_: com.google.gson.JsonSyntaxException) { emptyList() }
    }

    suspend fun addSearchHistory(query: String) {
        val history = getSearchHistory().toMutableList()
        history.remove(query)
        history.add(0, query)
        dataStore.edit { prefs -> prefs[Keys.SEARCH_HISTORY] = gson.toJson(history.take(20)) }
    }

    suspend fun clearSearchHistory() {
        dataStore.edit { prefs -> prefs.remove(Keys.SEARCH_HISTORY) }
    }

    // --- Bookmarks ---
    data class Bookmark(val title: String, val url: String)

    suspend fun getBookmarks(): List<Bookmark> {
        val json = dataStore.data.first()[Keys.BOOKMARKS] ?: return emptyList()
        return try { gson.fromJson(json, Array<Bookmark>::class.java).toList() } catch (_: com.google.gson.JsonSyntaxException) { emptyList() }
    }

    suspend fun addBookmark(title: String, url: String) {
        val bookmarks = getBookmarks().toMutableList()
        bookmarks.removeAll { it.url == url }
        bookmarks.add(0, Bookmark(title, url))
        dataStore.edit { prefs -> prefs[Keys.BOOKMARKS] = gson.toJson(bookmarks) }
    }

    suspend fun removeBookmark(url: String) {
        val bookmarks = getBookmarks().filter { it.url != url }
        dataStore.edit { prefs -> prefs[Keys.BOOKMARKS] = gson.toJson(bookmarks) }
    }

    suspend fun clearProviderConfig(provider: AIProvider) {
        dataStore.edit { prefs ->
            val key = provider.name
            prefs.remove(Keys.apiKeyKey(key))
            prefs.remove(Keys.baseUrlKey(key))
            prefs.remove(Keys.enabledKey(key))
        }
    }
}
