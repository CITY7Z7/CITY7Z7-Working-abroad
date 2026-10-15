package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.database.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class NoteDao {

    @Query("SELECT * FROM note ORDER BY pinned DESC, updatedAt DESC")
    abstract fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE articleId = :articleId ORDER BY pinned DESC, updatedAt DESC")
    abstract fun getNotesForArticle(articleId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE id = :id LIMIT 1")
    abstract suspend fun getNoteById(id: Long): NoteEntity?

    @Query("SELECT * FROM note WHERE id = :id LIMIT 1")
    abstract fun observeNoteById(id: Long): Flow<NoteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertNoteInternal(note: NoteEntity): Long

    @Update
    protected abstract suspend fun updateNoteInternal(note: NoteEntity)

    @Query("INSERT INTO note_fts(docid, title, body) VALUES(:docid, :title, :body)")
    protected abstract suspend fun insertNoteFts(docid: Long, title: String, body: String)

    @Query("DELETE FROM note_fts WHERE docid = :docid")
    protected abstract suspend fun deleteNoteFts(docid: Long)

    @Query("DELETE FROM note WHERE id = :id")
    protected abstract suspend fun deleteNoteInternal(id: Long)

    @Transaction
    open suspend fun insertOrUpdateNote(note: NoteEntity): Long {
        val now = System.currentTimeMillis()
        val noteToSave = note.copy(updatedAt = now)
        val id = if (noteToSave.id == 0L) {
            insertNoteInternal(noteToSave.copy(createdAt = now))
        } else {
            updateNoteInternal(noteToSave)
            noteToSave.id
        }

        // FTS update: only non-private notes are indexed
        deleteNoteFts(id)
        if (!noteToSave.isPrivate && (noteToSave.title.isNotBlank() || noteToSave.body.isNotBlank())) {
            insertNoteFts(id, noteToSave.title, noteToSave.body)
        }
        return id
    }

    @Transaction
    open suspend fun deleteNote(id: Long) {
        deleteNoteFts(id)
        deleteNoteInternal(id)
    }

    @Query("""
        SELECT n.* FROM note n
        JOIN note_fts f ON n.id = f.docid
        WHERE note_fts MATCH :query AND n.isPrivate = 0
        ORDER BY n.pinned DESC, n.updatedAt DESC
    """)
    abstract suspend fun searchNotesFts(query: String): List<NoteEntity>
}
