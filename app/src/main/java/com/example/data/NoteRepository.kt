package com.example.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    val allNotesIncludingDeleted: Flow<List<Note>> = noteDao.getAllNotesIncludingDeleted()

    fun searchNotes(query: String): Flow<List<Note>> {
        return noteDao.searchNotes(query.trim())
    }

    fun getNoteById(id: Long): Flow<Note?> {
        return noteDao.getNoteById(id)
    }

    suspend fun getNoteByIdDirect(id: Long): Note? {
        return noteDao.getNoteByIdDirect(id)
    }

    suspend fun insertNote(note: Note): Long {
        return noteDao.insertNote(note)
    }

    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
    }

    suspend fun deleteNoteById(id: Long) {
        noteDao.deleteNoteById(id)
    }

    suspend fun emptyTrash() {
        noteDao.emptyTrash()
    }

    suspend fun togglePin(id: Long, currentPinState: Boolean) {
        noteDao.setPinned(id = id, isPinned = !currentPinState)
    }

    suspend fun setPinned(id: Long, isPinned: Boolean) {
        noteDao.setPinned(id = id, isPinned = isPinned)
    }

    suspend fun setLocked(id: Long, isLocked: Boolean) {
        noteDao.setLocked(id = id, isLocked = isLocked)
    }

    suspend fun moveToTrash(id: Long) {
        noteDao.moveToTrash(id = id)
    }

    suspend fun restoreFromTrash(id: Long) {
        noteDao.restoreFromTrash(id = id)
    }
}
