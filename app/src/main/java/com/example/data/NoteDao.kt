package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE is_deleted = 0 ORDER BY is_pinned DESC, updated_at DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes ORDER BY is_pinned DESC, updated_at DESC")
    fun getAllNotesIncludingDeleted(): Flow<List<Note>>

    @Query("""
        SELECT * FROM notes 
        WHERE is_deleted = 0 AND is_locked = 0 AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY is_pinned DESC, updated_at DESC
    """)
    fun searchNotes(query: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getNoteById(id: Long): Flow<Note?>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteByIdDirect(id: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("DELETE FROM notes WHERE is_deleted = 1")
    suspend fun emptyTrash()

    @Query("UPDATE notes SET is_pinned = :isPinned, updated_at = :updatedAt WHERE id = :id")
    suspend fun setPinned(id: Long, isPinned: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET is_locked = :isLocked, updated_at = :updatedAt WHERE id = :id")
    suspend fun setLocked(id: Long, isLocked: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET is_deleted = 1, deleted_at = :deletedAt, is_pinned = 0 WHERE id = :id")
    suspend fun moveToTrash(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET is_deleted = 0, deleted_at = NULL, updated_at = :updatedAt WHERE id = :id")
    suspend fun restoreFromTrash(id: Long, updatedAt: Long = System.currentTimeMillis())
}
