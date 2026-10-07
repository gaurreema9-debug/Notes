package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.Note
import com.example.data.NoteDao
import com.example.data.NoteRepository
import com.example.data.NoteViewMode
import com.example.data.ThemePreferences
import com.example.ui.EditorTextAlign
import com.example.ui.InlineStyleType
import com.example.ui.NotesViewModel
import com.example.ui.RichTextEditorHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var noteDao: NoteDao
    private lateinit var repository: NoteRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        noteDao = database.noteDao()
        repository = NoteRepository(noteDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun appNameStringIsNotes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Notes", appName)
    }

    @Test
    fun viewModePersistenceAndLabelsTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("सूची दृश्य", context.getString(R.string.list_view_label))
        assertEquals("कार्ड दृश्य", context.getString(R.string.card_view_label))
        assertEquals("हाल ही में डिलीट किए गए", context.getString(R.string.recently_deleted_option))
        assertEquals("लॉक नोट", context.getString(R.string.locked_notes_option))

        val prefs1 = ThemePreferences(context)
        assertEquals(NoteViewMode.LIST, prefs1.viewMode.value)

        prefs1.setViewMode(NoteViewMode.CARD)
        assertEquals(NoteViewMode.CARD, prefs1.viewMode.value)

        // Re-instantiate to simulate reopening the app
        val prefs2 = ThemePreferences(context)
        assertEquals(NoteViewMode.CARD, prefs2.viewMode.value)

        prefs2.setViewMode(NoteViewMode.LIST)
        val prefs3 = ThemePreferences(context)
        assertEquals(NoteViewMode.LIST, prefs3.viewMode.value)
    }

    @Test
    fun editorUndoRedoAndFormattingFlowTest() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = ThemePreferences(app)
        val vm = NotesViewModel(app, repository, prefs)

        vm.openNewNote()
        assertFalse(vm.editorState.value.canUndo)
        assertFalse(vm.editorState.value.canRedo)

        // 1. Write text in steps
        vm.updateEditorContentValue(TextFieldValue("Hello", TextRange(5)))
        assertTrue(vm.editorState.value.canUndo)
        assertFalse(vm.editorState.value.canRedo)

        vm.updateEditorContentValue(TextFieldValue("Hello World", TextRange(11)))
        assertEquals("Hello World", vm.editorState.value.content)

        // 2. Apply Bold formatting to "Hello" (0..5)
        vm.updateEditorContentValue(TextFieldValue("Hello World", TextRange(0, 5)))
        vm.toggleInlineStyle(InlineStyleType.BOLD)
        assertTrue(
            RichTextEditorHelper.isStyleActiveInSelection(
                vm.editorState.value.formatting,
                TextRange(0, 5),
                emptySet(),
                InlineStyleType.BOLD
            )
        )

        // 3. Apply Italic and Underline to "World" (6..11)
        vm.updateEditorContentValue(TextFieldValue("Hello World", TextRange(6, 11)))
        vm.toggleInlineStyle(InlineStyleType.ITALIC)
        vm.toggleInlineStyle(InlineStyleType.UNDERLINE)
        assertTrue(
            RichTextEditorHelper.isStyleActiveInSelection(
                vm.editorState.value.formatting,
                TextRange(6, 11),
                emptySet(),
                InlineStyleType.ITALIC
            )
        )
        assertTrue(
            RichTextEditorHelper.isStyleActiveInSelection(
                vm.editorState.value.formatting,
                TextRange(6, 11),
                emptySet(),
                InlineStyleType.UNDERLINE
            )
        )

        // 4. Undo Underline, Undo Italic, Undo Bold, Undo "Hello World", Undo "Hello"
        vm.undoEditor() // Undo Underline
        assertTrue(vm.editorState.value.canRedo)
        assertFalse(
            RichTextEditorHelper.isStyleActiveInSelection(
                vm.editorState.value.formatting,
                TextRange(6, 11),
                emptySet(),
                InlineStyleType.UNDERLINE
            )
        )

        vm.undoEditor() // Undo Italic
        vm.undoEditor() // Undo Bold
        assertEquals("Hello World", vm.editorState.value.content)

        vm.undoEditor() // Undo "Hello World" -> back to "Hello"
        assertEquals("Hello", vm.editorState.value.content)

        // 5. Redo back to "Hello World"
        vm.redoEditor()
        assertEquals("Hello World", vm.editorState.value.content)

        // 6. Writing new text after Undo clears Redo history
        vm.updateEditorContentValue(TextFieldValue("Hello World!", TextRange(12)))
        assertFalse(vm.editorState.value.canRedo)

        // 7. Bullet List & Numbered List
        vm.toggleBulletList()
        assertEquals("• Hello World!", vm.editorState.value.content)

        vm.toggleNumberedList()
        assertEquals("1. Hello World!", vm.editorState.value.content)

        // 8. Text Alignment
        vm.setTextAlignment(EditorTextAlign.CENTER)
        assertEquals(EditorTextAlign.CENTER, vm.editorState.value.formatting.textAlign)

        // 9. Select All, Copy, Cut, Paste
        vm.selectAllContent()
        assertEquals(TextRange(0, vm.editorState.value.content.length), vm.editorState.value.contentSelection)

        val copied = vm.copySelectedContent()
        assertEquals("1. Hello World!", copied)

        val cut = vm.cutSelectedContent()
        assertEquals("1. Hello World!", cut)
        assertEquals("", vm.editorState.value.content)

        vm.pasteIntoContent(cut)
        assertEquals("1. Hello World!", vm.editorState.value.content)
    }

    @Test
    fun pinLockAndRecentlyDeletedFlowTest() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = ThemePreferences(context)

        // Set 4-digit PIN
        prefs.setLockPin("1234")
        assertTrue(prefs.verifyLockPin("1234"))
        assertFalse(prefs.verifyLockPin("0000"))

        // Insert a note
        val noteId = repository.insertNote(
            Note(
                title = "Secret Banking Note",
                content = "Account code: 9876"
            )
        )

        // Lock the note
        repository.setLocked(noteId, true)
        val lockedNote = repository.getNoteByIdDirect(noteId)
        assertNotNull(lockedNote)
        assertTrue(lockedNote!!.isLocked)

        // Move to Recently Deleted (Trash)
        repository.moveToTrash(noteId)
        val activeNotes = repository.allNotes.first()
        assertTrue(activeNotes.isEmpty())

        val allIncludingDeleted = repository.allNotesIncludingDeleted.first()
        assertEquals(1, allIncludingDeleted.size)
        assertTrue(allIncludingDeleted[0].isDeleted)

        // Restore from Recently Deleted
        repository.restoreFromTrash(noteId)
        val restoredActive = repository.allNotes.first()
        assertEquals(1, restoredActive.size)
        assertFalse(restoredActive[0].isDeleted)

        // Permanently delete
        repository.deleteNoteById(noteId)
        assertNull(repository.getNoteByIdDirect(noteId))
    }

    @Test
    fun createSaveEditSearchPinDeleteFlow() = runBlocking {
        // 1. Initial State: Empty
        val initialNotes = repository.allNotes.first()
        assertTrue(initialNotes.isEmpty())

        // 2. Create and Save Note 1
        val note1 = Note(
            title = "Grocery List",
            content = "Milk, Eggs, Bread, Apples",
            isPinned = false
        )
        val note1Id = repository.insertNote(note1)
        assertTrue(note1Id > 0)

        // 3. Create and Save Note 2
        val note2 = Note(
            title = "Project Ideas",
            content = "Build an offline notes app with Room and Compose",
            isPinned = false
        )
        val note2Id = repository.insertNote(note2)
        assertTrue(note2Id > 0)

        // Verify 2 notes exist
        val notesAfterInsert = repository.allNotes.first()
        assertEquals(2, notesAfterInsert.size)

        // 4. Pin Note 2
        repository.setPinned(note2Id, true)
        val pinnedNotesList = repository.allNotes.first()
        assertEquals(note2Id, pinnedNotesList[0].id)
        assertTrue(pinnedNotesList[0].isPinned)
        assertFalse(pinnedNotesList[1].isPinned)

        // 5. Search notes
        val searchResults = repository.searchNotes("offline").first()
        assertEquals(1, searchResults.size)
        assertEquals("Project Ideas", searchResults[0].title)

        val noResults = repository.searchNotes("nonexistent text").first()
        assertEquals(0, noResults.size)

        // 6. Edit Note 1
        val updatedNote1 = Note(
            id = note1Id,
            title = "Grocery List - Updated",
            content = "Milk, Eggs, Bread, Apples, Honey",
            isPinned = false
        )
        repository.updateNote(updatedNote1)

        val retrievedNote1 = repository.getNoteByIdDirect(note1Id)
        assertNotNull(retrievedNote1)
        assertEquals("Grocery List - Updated", retrievedNote1?.title)
        assertEquals("Milk, Eggs, Bread, Apples, Honey", retrievedNote1?.content)

        // 7. Unpin Note 2
        repository.setPinned(note2Id, false)
        val retrievedNote2 = repository.getNoteByIdDirect(note2Id)
        assertFalse(retrievedNote2?.isPinned == true)

        // 8. Delete Note 1
        repository.deleteNoteById(note1Id)
        val retrievedDeleted = repository.getNoteByIdDirect(note1Id)
        assertNull(retrievedDeleted)

        val finalNotes = repository.allNotes.first()
        assertEquals(1, finalNotes.size)
        assertEquals(note2Id, finalNotes[0].id)
    }
}
