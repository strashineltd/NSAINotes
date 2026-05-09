package com.nsai.notes.presentation.files

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
data class AppFileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }
}

@Stable
data class FileListUiState(
    val files: List<AppFileItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val createName: String = "",
    val createIsFolder: Boolean = false,
    val renameTarget: AppFileItem? = null,
    val renameName: String = ""
)

sealed class FileListEvent {
    data object LoadFiles : FileListEvent()
    data class CreateItem(val name: String, val isFolder: Boolean) : FileListEvent()
    data class DeleteItem(val item: AppFileItem) : FileListEvent()
    data class RenameItem(val item: AppFileItem, val newName: String) : FileListEvent()
    data object ShowCreateFile : FileListEvent()
    data object ShowCreateFolder : FileListEvent()
    data object DismissDialog : FileListEvent()
    data class UpdateCreateName(val name: String) : FileListEvent()
    data object ConfirmCreate : FileListEvent()
    data class StartRename(val item: AppFileItem) : FileListEvent()
    data class UpdateRenameName(val name: String) : FileListEvent()
    data object ConfirmRename : FileListEvent()
}

@HiltViewModel
class FileListViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileListUiState())
    val uiState: StateFlow<FileListUiState> = _uiState.asStateFlow()

    private val appFilesDir: File
        get() = File(context.filesDir, "NSAI_Files").also { if (!it.exists()) it.mkdirs() }

    init { loadFiles() }

    fun onEvent(event: FileListEvent) {
        when (event) {
            FileListEvent.LoadFiles -> loadFiles()
            is FileListEvent.CreateItem -> createItem(event.name, event.isFolder)
            is FileListEvent.DeleteItem -> deleteItem(event.item)
            is FileListEvent.RenameItem -> renameItem(event.item, event.newName)
            FileListEvent.ShowCreateFile -> showCreateDialog(isFolder = false)
            FileListEvent.ShowCreateFolder -> showCreateDialog(isFolder = true)
            FileListEvent.DismissDialog -> dismissDialog()
            is FileListEvent.UpdateCreateName -> _uiState.value = _uiState.value.copy(createName = event.name)
            FileListEvent.ConfirmCreate -> confirmCreate()
            is FileListEvent.StartRename -> _uiState.value = _uiState.value.copy(renameTarget = event.item, renameName = event.item.name)
            is FileListEvent.UpdateRenameName -> _uiState.value = _uiState.value.copy(renameName = event.name)
            FileListEvent.ConfirmRename -> confirmRename()
        }
    }

    private fun loadFiles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val items = withContext(Dispatchers.IO) {
                    appFilesDir.listFiles()
                        ?.filter { !it.name.startsWith(".") }
                        ?.map { file ->
                            AppFileItem(
                                name = file.name,
                                path = file.absolutePath,
                                isDirectory = file.isDirectory,
                                size = if (file.isFile) file.length() else 0,
                                lastModified = file.lastModified()
                            )
                        }
                        ?.sortedWith(
                            compareByDescending<AppFileItem> { it.isDirectory }.thenBy { it.name.lowercase() }
                        ) ?: emptyList()
                }
                _uiState.value = _uiState.value.copy(files = items, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private fun showCreateDialog(isFolder: Boolean) {
        _uiState.value = _uiState.value.copy(
            showCreateDialog = true,
            createName = "",
            createIsFolder = isFolder
        )
    }

    private fun dismissDialog() {
        _uiState.value = _uiState.value.copy(
            showCreateDialog = false,
            createName = "",
            renameTarget = null,
            renameName = ""
        )
    }

    private fun confirmCreate() {
        val name = _uiState.value.createName.trim()
        if (name.isBlank()) return
        createItem(name, _uiState.value.createIsFolder)
    }

    private fun createItem(name: String, isFolder: Boolean) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val target = File(appFilesDir, name)
                    if (target.exists()) throw IllegalStateException("已存在同名${if (isFolder) "文件夹" else "文件"}")
                    if (isFolder) target.mkdirs() else target.createNewFile()
                }
                dismissDialog()
                loadFiles()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private fun deleteItem(item: AppFileItem) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val target = File(item.path)
                    if (item.isDirectory) target.deleteRecursively() else target.delete()
                }
                loadFiles()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private fun confirmRename() {
        val target = _uiState.value.renameTarget ?: return
        val newName = _uiState.value.renameName.trim()
        if (newName.isBlank() || newName == target.name) {
            dismissDialog()
            return
        }
        renameItem(target, newName)
    }

    private fun renameItem(item: AppFileItem, newName: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val source = File(item.path)
                    val target = File(source.parent, newName)
                    if (target.exists()) throw IllegalStateException("已存在同名${if (target.isDirectory) "文件夹" else "文件"}")
                    source.renameTo(target)
                }
                dismissDialog()
                loadFiles()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
