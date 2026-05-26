package com.nsai.notes.presentation.files

import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.compose.runtime.Stable
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nsai.notes.data.local.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@Stable
data class AppFileItem(val name: String, val path: String, val isDirectory: Boolean, val size: Long, val lastModified: Long) {
    val formattedDate: String get() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(lastModified))
}

@Stable
data class FileListUiState(
    val files: List<AppFileItem> = emptyList(), val isLoading: Boolean = true, val error: String? = null,
    val showCreateDialog: Boolean = false, val createName: String = "", val createIsFolder: Boolean = false,
    val renameTarget: AppFileItem? = null, val renameName: String = "",
    val canGoUp: Boolean = false, val currentPath: String = "/",
    val openFileIntent: Intent? = null, val showSystemFiles: Boolean = false,
    val deleteConfirmTarget: AppFileItem? = null,
    val isPrivateUnlocked: Boolean = false, val showPinDialog: Boolean = false, val pinError: Boolean = false
)

sealed class FileListEvent {
    data object LoadFiles : FileListEvent()
    data class CreateItem(val name: String, val isFolder: Boolean) : FileListEvent()
    data class DeleteItem(val item: AppFileItem) : FileListEvent()
    data class RenameItem(val item: AppFileItem, val newName: String) : FileListEvent()
    data object ShowCreateFile : FileListEvent()
    data object ShowCreateFolder : FileListEvent()
    data class ConfirmDelete(val item: AppFileItem) : FileListEvent()
    data class TogglePrivate(val item: AppFileItem) : FileListEvent()
    data object ShowUnlockPin : FileListEvent()
    data object LockPrivate : FileListEvent()
    data class VerifyPin(val pin: String) : FileListEvent()
    data object DismissDialog : FileListEvent()
    data class UpdateCreateName(val name: String) : FileListEvent()
    data object ConfirmCreate : FileListEvent()
    data class StartRename(val item: AppFileItem) : FileListEvent()
    data class UpdateRenameName(val name: String) : FileListEvent()
    data object ConfirmRename : FileListEvent()
    data class OpenFolder(val item: AppFileItem) : FileListEvent()
    data class OpenFile(val item: AppFileItem) : FileListEvent()
    data object NavigateUp : FileListEvent()
    data object ToggleSystemView : FileListEvent()
}

@HiltViewModel
class FileListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(FileListUiState())
    val uiState: StateFlow<FileListUiState> = _uiState.asStateFlow()

    private val userFilesDir: File get() = File(context.filesDir, "NSAI_Files").also { if (!it.exists()) it.mkdirs() }
    private val systemRootDir: File get() = context.filesDir.parentFile ?: context.filesDir
    private val privateDir: File get() = File(userFilesDir, ".private").also { if (!it.exists()) it.mkdirs() }
    private val showSystemFiles get() = _uiState.value.showSystemFiles
    private val rootDir: File get() = if (showSystemFiles) systemRootDir else userFilesDir
    private var currentDir: File = userFilesDir

    init { loadFiles() }

    fun onEvent(event: FileListEvent) {
        when (event) {
            FileListEvent.LoadFiles -> loadFiles()
            is FileListEvent.CreateItem -> createItem(event.name, event.isFolder)
            is FileListEvent.DeleteItem -> _uiState.value = _uiState.value.copy(deleteConfirmTarget = event.item)
            is FileListEvent.ConfirmDelete -> deleteItem(event.item)
            is FileListEvent.RenameItem -> renameItem(event.item, event.newName)
            FileListEvent.ShowCreateFile -> showCreateDialog(isFolder = false)
            FileListEvent.ShowCreateFolder -> showCreateDialog(isFolder = true)
            FileListEvent.DismissDialog -> dismissDialog()
            is FileListEvent.UpdateCreateName -> _uiState.value = _uiState.value.copy(createName = event.name)
            FileListEvent.ConfirmCreate -> confirmCreate()
            is FileListEvent.StartRename -> _uiState.value = _uiState.value.copy(renameTarget = event.item, renameName = event.item.name)
            is FileListEvent.UpdateRenameName -> _uiState.value = _uiState.value.copy(renameName = event.name)
            FileListEvent.ConfirmRename -> confirmRename()
            FileListEvent.ToggleSystemView -> { currentDir = if (!showSystemFiles) systemRootDir else userFilesDir; _uiState.value = _uiState.value.copy(showSystemFiles = !showSystemFiles); loadFiles() }
            is FileListEvent.OpenFolder -> { currentDir = File(event.item.path); loadFiles() }
            is FileListEvent.OpenFile -> { val f = File(event.item.path); val ext = MimeTypeMap.getFileExtensionFromUrl(f.name); val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"; try { val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f); _uiState.value = _uiState.value.copy(openFileIntent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, mime); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }) } catch (_: Exception) { _uiState.value = _uiState.value.copy(error = "无法打开") } }
            FileListEvent.NavigateUp -> { if (currentDir != rootDir) { currentDir = currentDir.parentFile ?: rootDir; loadFiles() } }
            FileListEvent.LockPrivate -> { _uiState.value = _uiState.value.copy(isPrivateUnlocked = false); loadFiles() }
            FileListEvent.ShowUnlockPin -> _uiState.value = _uiState.value.copy(showPinDialog = true)
            is FileListEvent.VerifyPin -> verifyPin(event.pin)
            is FileListEvent.TogglePrivate -> togglePrivacy(event.item)
        }
    }

    private fun loadFiles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val targetDir = currentDir
                val baseForPath = if (showSystemFiles) systemRootDir.absolutePath else userFilesDir.absolutePath
                val showHidden = showSystemFiles
                val privateUnlocked = _uiState.value.isPrivateUnlocked
                val items = withContext(Dispatchers.IO) {
                    targetDir.listFiles()?.filter {
                        if (it.name == privateDir.name && !privateUnlocked) false else showHidden || !it.name.startsWith(".")
                    }?.map { AppFileItem(it.name, it.absolutePath, it.isDirectory, if (it.isFile) it.length() else 0, it.lastModified()) }
                        ?.sortedWith(compareByDescending<AppFileItem> { it.isDirectory }.thenBy { it.name.lowercase() }) ?: emptyList()
                }
                _uiState.value = _uiState.value.copy(files = items, isLoading = false, error = null,
                    canGoUp = currentDir != rootDir,
                    currentPath = if (showSystemFiles) currentDir.absolutePath else currentDir.absolutePath.removePrefix(baseForPath).ifEmpty { "/" })
            } catch (e: Exception) { _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) }
        }
    }

    private fun verifyPin(pin: String) { viewModelScope.launch { val saved = settingsDataStore.getPrivacyPin(); _uiState.value = if (saved.isEmpty() || pin == saved) { if (saved.isEmpty()) settingsDataStore.setPrivacyPin(pin); currentDir = userFilesDir; _uiState.value.copy(isPrivateUnlocked = true, showPinDialog = false, pinError = false) } else _uiState.value.copy(pinError = true); loadFiles() } }

    private fun togglePrivacy(item: AppFileItem) { viewModelScope.launch { try { withContext(Dispatchers.IO) { val s = File(item.path); val isPrivate = s.absolutePath.contains("/${privateDir.name}/"); val d = if (isPrivate) File(userFilesDir, s.name) else File(privateDir, s.name); s.renameTo(d) }; loadFiles() } catch (e: Exception) { _uiState.value = _uiState.value.copy(error = e.message) } } }

    private fun dismissDialog() { _uiState.value = _uiState.value.copy(showCreateDialog = false, createName = "", renameTarget = null, renameName = "", openFileIntent = null, deleteConfirmTarget = null, showPinDialog = false, pinError = false) }
    private fun confirmCreate() { val n = _uiState.value.createName.trim(); if (n.isBlank()) return; createItem(n, _uiState.value.createIsFolder) }
    private fun showCreateDialog(isFolder: Boolean) { _uiState.value = _uiState.value.copy(showCreateDialog = true, createName = "", createIsFolder = isFolder) }
    private fun createItem(name: String, isFolder: Boolean) { viewModelScope.launch { try { withContext(Dispatchers.IO) { val t = File(currentDir, name); if (t.exists()) throw IllegalStateException("已存在"); if (isFolder) t.mkdirs() else t.createNewFile() }; dismissDialog(); loadFiles() } catch (e: Exception) { _uiState.value = _uiState.value.copy(error = e.message) } } }
    private fun deleteItem(item: AppFileItem) { viewModelScope.launch { try { withContext(Dispatchers.IO) { val t = File(item.path); if (item.isDirectory) t.deleteRecursively() else t.delete() }; loadFiles() } catch (e: Exception) { _uiState.value = _uiState.value.copy(error = e.message) } } }
    private fun confirmRename() { val t = _uiState.value.renameTarget ?: return; val n = _uiState.value.renameName.trim(); if (n.isBlank() || n == t.name) { dismissDialog(); return }; renameItem(t, n) }
    private fun renameItem(item: AppFileItem, newName: String) { viewModelScope.launch { try { withContext(Dispatchers.IO) { val s = File(item.path); val t = File(s.parent, newName); if (t.exists()) throw IllegalStateException("已存在"); s.renameTo(t) }; dismissDialog(); loadFiles() } catch (e: Exception) { _uiState.value = _uiState.value.copy(error = e.message) } } }
}
