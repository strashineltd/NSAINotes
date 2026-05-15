package com.nsai.notes.presentation.notes

import com.nsai.notes.domain.model.Note
import com.nsai.notes.domain.model.Tag
import com.nsai.notes.domain.usecase.note.CreateNoteUseCase
import com.nsai.notes.domain.usecase.note.GetNoteUseCase
import com.nsai.notes.domain.usecase.note.UpdateNoteUseCase
import com.nsai.notes.domain.usecase.tag.GetAllTagsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class NoteEditViewModelTest {

    private lateinit var getNoteUseCase: GetNoteUseCase
    private lateinit var createNoteUseCase: CreateNoteUseCase
    private lateinit var updateNoteUseCase: UpdateNoteUseCase
    private lateinit var getAllTagsUseCase: GetAllTagsUseCase
    private lateinit var viewModel: NoteEditViewModel

    @Before
    fun setup() {
        getNoteUseCase = mock()
        createNoteUseCase = mock()
        updateNoteUseCase = mock()
        getAllTagsUseCase = mock()
        val extractTextUseCase = mock(com.nsai.notes.domain.multimodal.ExtractTextUseCase::class.java)
        val describeImageUseCase = mock(com.nsai.notes.domain.multimodal.DescribeImageUseCase::class.java)
        val settingsDataStore = mock(com.nsai.notes.data.local.datastore.SettingsDataStore::class.java)
        val indexNotesUseCase = dagger.Lazy { mock(com.nsai.notes.domain.rag.IndexNotesUseCase::class.java) }
        viewModel = NoteEditViewModel(
            getNoteUseCase, createNoteUseCase, updateNoteUseCase, getAllTagsUseCase,
            extractTextUseCase, describeImageUseCase, settingsDataStore, indexNotesUseCase
        )
    }

    @Test
    fun `updateTitle should update state`() {
        viewModel.onEvent(NoteEditEvent.UpdateTitle("Hello"))
        assertEquals("Hello", viewModel.uiState.value.title)
    }

    @Test
    fun `updateContent should update state`() {
        viewModel.onEvent(NoteEditEvent.UpdateContent("World"))
        assertEquals("World", viewModel.uiState.value.content)
    }

    @Test
    fun `togglePreview should switch state`() {
        assertTrue(!viewModel.uiState.value.isPreview)
        viewModel.onEvent(NoteEditEvent.TogglePreview)
        assertTrue(viewModel.uiState.value.isPreview)
    }

    @Test
    fun `togglePrivate should switch state`() {
        assertTrue(!viewModel.uiState.value.isPrivate)
        viewModel.onEvent(NoteEditEvent.TogglePrivate)
        assertTrue(viewModel.uiState.value.isPrivate)
    }

    @Test
    fun `toggleTag should add and remove tags`() {
        val tag = Tag(id = 1, name = "Work")
        viewModel.onEvent(NoteEditEvent.ToggleTag(tag))
        assertTrue(viewModel.uiState.value.selectedTags.any { it.id == 1L })

        viewModel.onEvent(NoteEditEvent.ToggleTag(tag))
        assertTrue(viewModel.uiState.value.selectedTags.none { it.id == 1L })
    }

    @Test
    fun `loadNote should populate fields`() = runTest {
        val note = Note(id = 1, title = "Test", content = "Content", tags = emptyList())
        `when`(getNoteUseCase(1L)).thenReturn(MutableStateFlow(note))
        `when`(getAllTagsUseCase()).thenReturn(emptyList())

        viewModel.onEvent(NoteEditEvent.LoadNote(1L))

        assertEquals("Test", viewModel.uiState.value.title)
        assertEquals("Content", viewModel.uiState.value.content)
    }

    @Test
    fun `saveAndReturn new note should create`() = runTest {
        viewModel.onEvent(NoteEditEvent.UpdateTitle("New Title"))
        viewModel.onEvent(NoteEditEvent.UpdateContent("New Content"))
        `when`(createNoteUseCase(any(), any(), any()))
            .thenReturn(Result.success(1L))

        val result = viewModel.saveAndReturn()
        assertEquals(1L, result)
    }
}
