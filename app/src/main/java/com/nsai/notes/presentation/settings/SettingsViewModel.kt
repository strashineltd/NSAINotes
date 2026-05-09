package com.nsai.notes.presentation.settings

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nsai.notes.BuildConfig
import com.nsai.notes.data.local.datastore.SettingsDataStore
import com.nsai.notes.data.remote.NoUpdateException
import com.nsai.notes.data.remote.UpdateChecker
import com.nsai.notes.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
data class SettingsUiState(
    val appVersion: String = BuildConfig.VERSION_NAME,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontScale: Float = 1.0f,
    val updateDialog: UpdateDialogState = UpdateDialogState.Hidden
)

@Stable
sealed class UpdateDialogState {
    data object Hidden : UpdateDialogState()
    data object Checking : UpdateDialogState()
    data class Available(val version: String, val url: String, val notes: String = "") : UpdateDialogState()
    data object UpToDate : UpdateDialogState()
    data class Error(val message: String) : UpdateDialogState()
    data object PrivacyPolicy : UpdateDialogState()
}

sealed class SettingsEvent {
    data class SetThemeMode(val mode: ThemeMode) : SettingsEvent()
    data class SetFontScale(val scale: Float) : SettingsEvent()
    data object CheckUpdate : SettingsEvent()
    data object ShowPrivacyPolicy : SettingsEvent()
    data object DismissUpdateDialog : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val updateChecker: UpdateChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadThemeMode()
        loadFontScale()
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetThemeMode -> setThemeMode(event.mode)
            is SettingsEvent.SetFontScale -> setFontScale(event.scale)
            SettingsEvent.CheckUpdate -> checkUpdate()
            SettingsEvent.ShowPrivacyPolicy -> showPrivacyPolicy()
            SettingsEvent.DismissUpdateDialog -> dismissUpdateDialog()
        }
    }

    private fun loadFontScale() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(fontScale = settingsDataStore.fontScale.first())
        }
    }

    private fun setFontScale(scale: Float) {
        viewModelScope.launch {
            settingsDataStore.setFontScale(scale)
            _uiState.value = _uiState.value.copy(fontScale = scale)
        }
    }

    private fun showPrivacyPolicy() {
        _uiState.value = _uiState.value.copy(updateDialog = UpdateDialogState.PrivacyPolicy)
    }

    private fun loadThemeMode() {
        viewModelScope.launch {
            val mode = settingsDataStore.themeMode.first()
            _uiState.value = _uiState.value.copy(themeMode = ThemeMode.fromValue(mode))
        }
    }

    private fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode.value)
            _uiState.value = _uiState.value.copy(themeMode = mode)
        }
    }

    private fun checkUpdate() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(updateDialog = UpdateDialogState.Checking)
            val result = updateChecker.check(_uiState.value.appVersion)
            _uiState.value = _uiState.value.copy(
                updateDialog = result.fold(
                    onSuccess = { info ->
                        UpdateDialogState.Available(info.version, info.downloadUrl, info.notes)
                    },
                    onFailure = { e ->
                        when (e) {
                            is NoUpdateException -> UpdateDialogState.UpToDate
                            is java.net.UnknownHostException -> UpdateDialogState.Error("无网络连接，请检查网络")
                            is java.net.SocketTimeoutException -> UpdateDialogState.Error("连接超时，请稍后重试")
                            else -> UpdateDialogState.Error(e.message ?: "检查失败")
                        }
                    }
                )
            )
        }
    }

    private fun dismissUpdateDialog() {
        _uiState.value = _uiState.value.copy(updateDialog = UpdateDialogState.Hidden)
    }
}
