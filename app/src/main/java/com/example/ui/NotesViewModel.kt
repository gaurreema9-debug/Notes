package com.example.ui

import android.app.Application
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Note
import com.example.data.NoteRepository
import com.example.data.NoteSection
import com.example.data.NoteViewMode
import com.example.data.ThemeMode
import com.example.data.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Home : Screen
    data class Editor(val noteId: Long? = null) : Screen
}

data class EditorState(
    val noteId: Long? = null,
    val title: String = "",
    val content: String = "",
    val contentSelection: TextRange = TextRange.Zero,
    val formatting: RichTextFormatting = RichTextFormatting(),
    val activeTypingStyles: Set<InlineStyleType> = emptySet(),
    val undoStack: List<EditorSnapshot> = emptyList(),
    val redoStack: List<EditorSnapshot> = emptyList(),
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val hasUnsavedChanges: Boolean = false
) {
    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun toSnapshot(): EditorSnapshot = EditorSnapshot(
        title = title,
        content = content,
        selectionStart = contentSelection.start,
        selectionEnd = contentSelection.end,
        formatting = formatting,
        activeTypingStyles = activeTypingStyles
    )
}

enum class UnlockAction {
    OPEN_NOTE,
    UNLOCK_NOTE
}

sealed interface PinDialogState {
    data class SetPin(
        val noteToLock: Note,
        val errorMessage: String? = null
    ) : PinDialogState

    data class UnlockNote(
        val note: Note,
        val unlockAction: UnlockAction = UnlockAction.OPEN_NOTE,
        val errorMessage: String? = null
    ) : PinDialogState
}

class NotesViewModel(
    application: Application,
    private val repository: NoteRepository,
    private val themePreferences: ThemePreferences
) : AndroidViewModel(application) {

    val themeMode: StateFlow<ThemeMode> = themePreferences.themeMode
    val viewMode: StateFlow<NoteViewMode> = themePreferences.viewMode
    val lockPin: StateFlow<String?> = themePreferences.lockPin

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSection = MutableStateFlow(NoteSection.ALL)
    val selectedSection: StateFlow<NoteSection> = _selectedSection.asStateFlow()

    private val _pinDialogState = MutableStateFlow<PinDialogState?>(null)
    val pinDialogState: StateFlow<PinDialogState?> = _pinDialogState.asStateFlow()

    val notes: StateFlow<List<Note>> = combine(
        repository.allNotesIncludingDeleted,
        _searchQuery,
        _selectedSection
    ) { allNotes, query, section ->
        val bySection = when (section) {
            NoteSection.ALL -> allNotes.filter { !it.isDeleted }
            NoteSection.LOCKED -> allNotes.filter { !it.isDeleted && it.isLocked }
            NoteSection.TRASH -> allNotes.filter { it.isDeleted }
        }

        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            bySection
        } else {
            // Never expose locked note content in search results without PIN
            bySection.filter { note ->
                !note.isLocked && (
                    note.title.contains(trimmedQuery, ignoreCase = true) ||
                        note.content.contains(trimmedQuery, ignoreCase = true)
                    )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _editorState = MutableStateFlow(EditorState())
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    private val _showAboutDialog = MutableStateFlow(false)
    val showAboutDialog: StateFlow<Boolean> = _showAboutDialog.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun setThemeMode(mode: ThemeMode) {
        themePreferences.setThemeMode(mode)
    }

    fun setViewMode(mode: NoteViewMode) {
        themePreferences.setViewMode(mode)
    }

    fun setShowAboutDialog(show: Boolean) {
        _showAboutDialog.value = show
    }

    fun setShowDeleteDialog(show: Boolean) {
        _showDeleteDialog.value = show
    }

    fun selectSection(section: NoteSection) {
        _selectedSection.value = section
    }

    fun dismissPinDialog() {
        _pinDialogState.value = null
    }

    fun onLockNoteRequested(note: Note) {
        if (!note.isLocked) {
            val existingPin = lockPin.value
            if (existingPin == null || existingPin.length != 4) {
                _pinDialogState.value = PinDialogState.SetPin(noteToLock = note)
            } else {
                viewModelScope.launch {
                    repository.setLocked(note.id, true)
                    _statusMessage.value = "🔒 Note Locked"
                }
            }
        } else {
            // Require 4-digit PIN before unlocking a locked note
            _pinDialogState.value = PinDialogState.UnlockNote(
                note = note,
                unlockAction = UnlockAction.UNLOCK_NOTE
            )
        }
    }

    fun onSetFourDigitPin(pin: String, confirmPin: String, noteToLock: Note) {
        val cleanedPin = pin.trim()
        val cleanedConfirm = confirmPin.trim()

        if (cleanedPin.length != 4 || !cleanedPin.all { it.isDigit() }) {
            _pinDialogState.value = PinDialogState.SetPin(
                noteToLock = noteToLock,
                errorMessage = "कृपया केवल 4 अंकों का PIN दर्ज करें।"
            )
            return
        }

        if (cleanedPin != cleanedConfirm) {
            _pinDialogState.value = PinDialogState.SetPin(
                noteToLock = noteToLock,
                errorMessage = "दोनों PIN समान नहीं हैं, कृपया दोबारा प्रयास करें।"
            )
            return
        }

        themePreferences.setLockPin(cleanedPin)
        _pinDialogState.value = null
        viewModelScope.launch {
            repository.setLocked(noteToLock.id, true)
            _statusMessage.value = "🔒 Note Locked"
        }
    }

    fun onUnlockPinSubmitted(inputPin: String, note: Note, unlockAction: UnlockAction) {
        val cleaned = inputPin.trim()
        if (cleaned.length == 4 && themePreferences.verifyLockPin(cleaned)) {
            _pinDialogState.value = null
            when (unlockAction) {
                UnlockAction.OPEN_NOTE -> {
                    openNoteInternal(note)
                }
                UnlockAction.UNLOCK_NOTE -> {
                    viewModelScope.launch {
                        repository.setLocked(note.id, false)
                        _statusMessage.value = "🔓 Note Unlocked"
                    }
                }
            }
        } else {
            _pinDialogState.value = PinDialogState.UnlockNote(
                note = note,
                unlockAction = unlockAction,
                errorMessage = "गलत PIN, कृपया दोबारा प्रयास करें।"
            )
        }
    }

    fun openNewNote() {
        _editorState.value = EditorState(
            noteId = null,
            title = "",
            content = "",
            contentSelection = TextRange.Zero,
            formatting = RichTextFormatting(),
            activeTypingStyles = emptySet(),
            undoStack = emptyList(),
            redoStack = emptyList(),
            isPinned = false,
            isLocked = false,
            isDeleted = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            hasUnsavedChanges = false
        )
        _currentScreen.value = Screen.Editor(null)
    }

    fun openNote(note: Note) {
        if (note.isLocked) {
            _pinDialogState.value = PinDialogState.UnlockNote(
                note = note,
                unlockAction = UnlockAction.OPEN_NOTE
            )
            return
        }
        openNoteInternal(note)
    }

    private fun openNoteInternal(note: Note) {
        val parsedFormatting = RichTextFormatting.fromJson(note.formattingJson)
        _editorState.value = EditorState(
            noteId = note.id,
            title = note.title,
            content = note.content,
            contentSelection = TextRange(note.content.length),
            formatting = parsedFormatting,
            activeTypingStyles = emptySet(),
            undoStack = emptyList(),
            redoStack = emptyList(),
            isPinned = note.isPinned,
            isLocked = note.isLocked,
            isDeleted = note.isDeleted,
            createdAt = note.createdAt,
            updatedAt = note.updatedAt,
            hasUnsavedChanges = false
        )
        _currentScreen.value = Screen.Editor(note.id)
    }

    private fun pushUndoSnapshot(current: EditorState): List<EditorSnapshot> {
        val snapshot = current.toSnapshot()
        val last = current.undoStack.lastOrNull()
        if (last != null &&
            last.title == snapshot.title &&
            last.content == snapshot.content &&
            last.formatting == snapshot.formatting
        ) {
            return current.undoStack
        }
        return (current.undoStack + snapshot).takeLast(MAX_UNDO_HISTORY)
    }

    fun updateEditorTitle(title: String) {
        val current = _editorState.value
        if (current.title != title) {
            _editorState.value = current.copy(
                title = title,
                undoStack = pushUndoSnapshot(current),
                redoStack = emptyList(),
                hasUnsavedChanges = true
            )
        }
    }

    fun updateEditorContent(content: String) {
        val current = _editorState.value
        if (current.content != content) {
            val updatedFormatting = RichTextEditorHelper.adjustSpansOnTextChange(
                oldText = current.content,
                newText = content,
                oldFormatting = current.formatting,
                activeTypingStyles = current.activeTypingStyles
            )
            _editorState.value = current.copy(
                content = content,
                contentSelection = TextRange(content.length),
                formatting = updatedFormatting,
                undoStack = pushUndoSnapshot(current),
                redoStack = emptyList(),
                hasUnsavedChanges = true
            )
        }
    }

    fun updateEditorContentValue(newValue: TextFieldValue) {
        val current = _editorState.value
        val textChanged = current.content != newValue.text
        if (!textChanged) {
            if (current.contentSelection != newValue.selection) {
                // Sync typing styles when cursor moves without selection
                val inferredStyles = inferStylesAtCursor(current.formatting, newValue.selection, newValue.text.length)
                _editorState.value = current.copy(
                    contentSelection = newValue.selection,
                    activeTypingStyles = inferredStyles
                )
            }
            return
        }

        val oldTfv = TextFieldValue(text = current.content, selection = current.contentSelection)
        val autoContinuedTfv = RichTextEditorHelper.handleAutoListContinuation(oldTfv, newValue)
        val updatedFormatting = RichTextEditorHelper.adjustSpansOnTextChange(
            oldText = current.content,
            newText = autoContinuedTfv.text,
            oldFormatting = current.formatting,
            activeTypingStyles = current.activeTypingStyles
        )

        _editorState.value = current.copy(
            content = autoContinuedTfv.text,
            contentSelection = autoContinuedTfv.selection,
            formatting = updatedFormatting,
            undoStack = pushUndoSnapshot(current),
            redoStack = emptyList(),
            hasUnsavedChanges = true
        )
    }

    private fun inferStylesAtCursor(
        formatting: RichTextFormatting,
        selection: TextRange,
        textLength: Int
    ): Set<InlineStyleType> {
        val set = mutableSetOf<InlineStyleType>()
        if (selection.collapsed) {
            val pos = selection.start.coerceIn(0, textLength)
            if (pos > 0) {
                for (span in formatting.spans) {
                    if (pos > span.start && pos <= span.end) {
                        set.add(span.type)
                    }
                }
            }
        } else {
            for (type in InlineStyleType.entries) {
                if (RichTextEditorHelper.isStyleActiveInSelection(formatting, selection, emptySet(), type)) {
                    set.add(type)
                }
            }
        }
        return set
    }

    fun undoEditor() {
        val current = _editorState.value
        if (current.undoStack.isEmpty()) return
        val previous = current.undoStack.last()
        val remainingUndo = current.undoStack.dropLast(1)
        val newRedo = (current.redoStack + current.toSnapshot()).takeLast(MAX_UNDO_HISTORY)

        val safeStart = previous.selectionStart.coerceIn(0, previous.content.length)
        val safeEnd = previous.selectionEnd.coerceIn(0, previous.content.length)

        _editorState.value = current.copy(
            title = previous.title,
            content = previous.content,
            contentSelection = TextRange(safeStart, safeEnd),
            formatting = previous.formatting,
            activeTypingStyles = previous.activeTypingStyles,
            undoStack = remainingUndo,
            redoStack = newRedo,
            hasUnsavedChanges = true
        )
    }

    fun redoEditor() {
        val current = _editorState.value
        if (current.redoStack.isEmpty()) return
        val next = current.redoStack.last()
        val remainingRedo = current.redoStack.dropLast(1)
        val newUndo = (current.undoStack + current.toSnapshot()).takeLast(MAX_UNDO_HISTORY)

        val safeStart = next.selectionStart.coerceIn(0, next.content.length)
        val safeEnd = next.selectionEnd.coerceIn(0, next.content.length)

        _editorState.value = current.copy(
            title = next.title,
            content = next.content,
            contentSelection = TextRange(safeStart, safeEnd),
            formatting = next.formatting,
            activeTypingStyles = next.activeTypingStyles,
            undoStack = newUndo,
            redoStack = remainingRedo,
            hasUnsavedChanges = true
        )
    }

    fun toggleInlineStyle(type: InlineStyleType) {
        val current = _editorState.value
        val (newFormatting, newTypingStyles) = RichTextEditorHelper.toggleInlineStyle(
            formatting = current.formatting,
            selection = current.contentSelection,
            activeTypingStyles = current.activeTypingStyles,
            textLength = current.content.length,
            type = type
        )
        val formattingChanged = newFormatting != current.formatting
        _editorState.value = current.copy(
            formatting = newFormatting,
            activeTypingStyles = newTypingStyles,
            undoStack = if (formattingChanged) pushUndoSnapshot(current) else current.undoStack,
            redoStack = if (formattingChanged) emptyList() else current.redoStack,
            hasUnsavedChanges = if (formattingChanged) true else current.hasUnsavedChanges
        )
    }

    fun toggleBulletList() {
        val current = _editorState.value
        val currentTfv = TextFieldValue(text = current.content, selection = current.contentSelection)
        val (newTfv, newFormatting) = RichTextEditorHelper.toggleBulletList(
            value = currentTfv,
            formatting = current.formatting,
            activeTypingStyles = current.activeTypingStyles
        )
        _editorState.value = current.copy(
            content = newTfv.text,
            contentSelection = newTfv.selection,
            formatting = newFormatting,
            undoStack = pushUndoSnapshot(current),
            redoStack = emptyList(),
            hasUnsavedChanges = true
        )
    }

    fun toggleNumberedList() {
        val current = _editorState.value
        val currentTfv = TextFieldValue(text = current.content, selection = current.contentSelection)
        val (newTfv, newFormatting) = RichTextEditorHelper.toggleNumberedList(
            value = currentTfv,
            formatting = current.formatting,
            activeTypingStyles = current.activeTypingStyles
        )
        _editorState.value = current.copy(
            content = newTfv.text,
            contentSelection = newTfv.selection,
            formatting = newFormatting,
            undoStack = pushUndoSnapshot(current),
            redoStack = emptyList(),
            hasUnsavedChanges = true
        )
    }

    fun cycleTextAlignment() {
        val current = _editorState.value
        val nextAlign = current.formatting.textAlign.next()
        setTextAlignment(nextAlign)
    }

    fun setTextAlignment(align: EditorTextAlign) {
        val current = _editorState.value
        if (current.formatting.textAlign == align) return
        _editorState.value = current.copy(
            formatting = current.formatting.copy(textAlign = align),
            undoStack = pushUndoSnapshot(current),
            redoStack = emptyList(),
            hasUnsavedChanges = true
        )
    }

    fun selectAllContent() {
        val current = _editorState.value
        _editorState.value = current.copy(
            contentSelection = TextRange(0, current.content.length)
        )
    }

    fun cutSelectedContent(): String {
        val current = _editorState.value
        val sel = current.contentSelection
        val start = minOf(sel.start, sel.end).coerceIn(0, current.content.length)
        val end = maxOf(sel.start, sel.end).coerceIn(0, current.content.length)
        if (start == end) {
            // If nothing is selected, cut the entire content if non-empty
            if (current.content.isEmpty()) return ""
            val cutText = current.content
            _editorState.value = current.copy(
                content = "",
                contentSelection = TextRange.Zero,
                formatting = current.formatting.copy(spans = emptyList()),
                undoStack = pushUndoSnapshot(current),
                redoStack = emptyList(),
                hasUnsavedChanges = true
            )
            _statusMessage.value = "Cut to clipboard"
            return cutText
        }
        val selectedText = current.content.substring(start, end)
        val newText = current.content.removeRange(start, end)
        val newFormatting = RichTextEditorHelper.adjustSpansOnTextChange(
            oldText = current.content,
            newText = newText,
            oldFormatting = current.formatting,
            activeTypingStyles = current.activeTypingStyles
        )
        _editorState.value = current.copy(
            content = newText,
            contentSelection = TextRange(start),
            formatting = newFormatting,
            undoStack = pushUndoSnapshot(current),
            redoStack = emptyList(),
            hasUnsavedChanges = true
        )
        _statusMessage.value = "Cut to clipboard"
        return selectedText
    }

    fun copySelectedContent(): String {
        val current = _editorState.value
        val sel = current.contentSelection
        val start = minOf(sel.start, sel.end).coerceIn(0, current.content.length)
        val end = maxOf(sel.start, sel.end).coerceIn(0, current.content.length)
        val textToCopy = if (start != end) {
            current.content.substring(start, end)
        } else {
            current.content
        }
        if (textToCopy.isNotEmpty()) {
            _statusMessage.value = "Copied to clipboard"
        }
        return textToCopy
    }

    fun pasteIntoContent(pastedText: String) {
        if (pastedText.isEmpty()) return
        val current = _editorState.value
        val sel = current.contentSelection
        val start = minOf(sel.start, sel.end).coerceIn(0, current.content.length)
        val end = maxOf(sel.start, sel.end).coerceIn(0, current.content.length)
        val newText = current.content.replaceRange(start, end, pastedText)
        val newCursor = (start + pastedText.length).coerceIn(0, newText.length)
        val newFormatting = RichTextEditorHelper.adjustSpansOnTextChange(
            oldText = current.content,
            newText = newText,
            oldFormatting = current.formatting,
            activeTypingStyles = current.activeTypingStyles
        )
        _editorState.value = current.copy(
            content = newText,
            contentSelection = TextRange(newCursor),
            formatting = newFormatting,
            undoStack = pushUndoSnapshot(current),
            redoStack = emptyList(),
            hasUnsavedChanges = true
        )
    }

    fun toggleEditorPin() {
        val current = _editorState.value
        val newPin = !current.isPinned
        _editorState.value = current.copy(
            isPinned = newPin,
            hasUnsavedChanges = true
        )
        if (current.noteId != null) {
            viewModelScope.launch {
                repository.setPinned(current.noteId, newPin)
            }
        }
    }

    fun saveNote(showFeedback: Boolean = true): Boolean {
        val state = _editorState.value
        val titleTrimmed = state.title.trim()
        val contentTrimmed = state.content.trim()

        if (titleTrimmed.isEmpty() && contentTrimmed.isEmpty()) {
            if (showFeedback) {
                _statusMessage.value = "Cannot save empty note"
            }
            return false
        }

        val now = System.currentTimeMillis()
        val formattingJson = state.formatting.toJson()
        viewModelScope.launch {
            if (state.noteId == null) {
                val newNote = Note(
                    title = titleTrimmed,
                    content = state.content,
                    formattingJson = formattingJson,
                    isPinned = state.isPinned,
                    isLocked = state.isLocked,
                    isDeleted = false,
                    createdAt = now,
                    updatedAt = now
                )
                val newId = repository.insertNote(newNote)
                _editorState.value = state.copy(
                    noteId = newId,
                    title = titleTrimmed,
                    updatedAt = now,
                    hasUnsavedChanges = false
                )
            } else {
                val updatedNote = Note(
                    id = state.noteId,
                    title = titleTrimmed,
                    content = state.content,
                    formattingJson = formattingJson,
                    isPinned = state.isPinned,
                    isLocked = state.isLocked,
                    isDeleted = state.isDeleted,
                    createdAt = state.createdAt,
                    updatedAt = now
                )
                repository.updateNote(updatedNote)
                _editorState.value = state.copy(
                    title = titleTrimmed,
                    updatedAt = now,
                    hasUnsavedChanges = false
                )
            }
            if (showFeedback) {
                _statusMessage.value = "Note saved"
            }
        }
        return true
    }

    fun deleteCurrentNote() {
        val current = _editorState.value
        val id = current.noteId
        if (id != null) {
            viewModelScope.launch {
                if (current.isDeleted) {
                    repository.deleteNoteById(id)
                    _statusMessage.value = "Note permanently deleted"
                } else {
                    repository.moveToTrash(id)
                    _statusMessage.value = "हाल ही में डिलीट किए गए में भेजा गया"
                }
                _showDeleteDialog.value = false
                _currentScreen.value = Screen.Home
            }
        } else {
            _showDeleteDialog.value = false
            _currentScreen.value = Screen.Home
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            repository.togglePin(note.id, note.isPinned)
        }
    }

    fun moveNoteToTrash(note: Note) {
        viewModelScope.launch {
            repository.moveToTrash(note.id)
            _statusMessage.value = "हाल ही में डिलीट किए गए में भेजा गया"
        }
    }

    fun restoreNote(note: Note) {
        viewModelScope.launch {
            repository.restoreFromTrash(note.id)
            _statusMessage.value = "Note Restored"
        }
    }

    fun permanentlyDeleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNoteById(note.id)
            _statusMessage.value = "Note Permanently Deleted"
        }
    }

    fun deleteNoteDirectly(note: Note) {
        viewModelScope.launch {
            repository.deleteNoteById(note.id)
            _statusMessage.value = "Note deleted"
        }
    }

    fun onBackFromEditor() {
        val state = _editorState.value
        val titleTrimmed = state.title.trim()
        val contentTrimmed = state.content.trim()

        if ((titleTrimmed.isNotEmpty() || contentTrimmed.isNotEmpty()) && !state.isDeleted) {
            if (state.hasUnsavedChanges) {
                val now = System.currentTimeMillis()
                val formattingJson = state.formatting.toJson()
                viewModelScope.launch {
                    if (state.noteId == null) {
                        repository.insertNote(
                            Note(
                                title = titleTrimmed,
                                content = state.content,
                                formattingJson = formattingJson,
                                isPinned = state.isPinned,
                                isLocked = state.isLocked,
                                isDeleted = false,
                                createdAt = now,
                                updatedAt = now
                            )
                        )
                    } else {
                        repository.updateNote(
                            Note(
                                id = state.noteId,
                                title = titleTrimmed,
                                content = state.content,
                                formattingJson = formattingJson,
                                isPinned = state.isPinned,
                                isLocked = state.isLocked,
                                isDeleted = state.isDeleted,
                                createdAt = state.createdAt,
                                updatedAt = now
                            )
                        )
                    }
                }
            }
        }
        _currentScreen.value = Screen.Home
    }

    companion object {
        private const val MAX_UNDO_HISTORY = 100

        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getDatabase(application)
                    val repository = NoteRepository(db.noteDao())
                    val themePrefs = ThemePreferences(application)
                    return NotesViewModel(application, repository, themePrefs) as T
                }
            }
    }
}
