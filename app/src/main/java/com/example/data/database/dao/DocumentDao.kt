package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.database.entity.DocumentAlertEntity
import com.example.data.database.entity.DocumentCategory
import com.example.data.database.entity.DocumentEntity
import com.example.data.database.entity.PersonProfileEntity
import com.example.data.database.entity.ShareLogEntity
import com.example.data.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM document WHERE isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllActiveDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM document WHERE category = :category AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getDocumentsByCategory(category: DocumentCategory): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM document WHERE isPinned = 1 AND isArchived = 0 ORDER BY updatedAt DESC LIMIT 3")
    fun getPinnedDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM document WHERE isArchived = 0 ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentDocuments(limit: Int = 5): Flow<List<DocumentEntity>>

    @Query("""
        SELECT * FROM document 
        WHERE isArchived = 0 AND expiresAt IS NOT NULL AND expiresAt <= :maxEpochDay 
        ORDER BY expiresAt ASC
    """)
    fun getExpiringDocuments(maxEpochDay: Long): Flow<List<DocumentEntity>>

    @Query("""
        SELECT * FROM document 
        WHERE isArchived = 0 AND expiresAt IS NOT NULL AND expiresAt <= :maxEpochDay 
        ORDER BY expiresAt ASC
    """)
    suspend fun getExpiringDocumentsList(maxEpochDay: Long): List<DocumentEntity>

    @Query("SELECT * FROM document WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: String): DocumentEntity?

    @Query("SELECT * FROM document WHERE id = :id LIMIT 1")
    fun observeDocumentById(id: String): Flow<DocumentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Query("UPDATE document SET isArchived = 1, updatedAt = :now WHERE id = :id")
    suspend fun archiveDocument(id: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM document WHERE id = :id")
    suspend fun deleteDocument(id: String)

    @Query("DELETE FROM document")
    suspend fun clearAllDocuments()

    // Alerts
    @Query("SELECT * FROM document_alert WHERE documentId = :documentId")
    suspend fun getAlertsForDocument(documentId: String): List<DocumentAlertEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: DocumentAlertEntity)

    @Query("DELETE FROM document_alert WHERE documentId = :documentId")
    suspend fun clearAlertsForDocument(documentId: String)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM person_profile WHERE id = 1 LIMIT 1")
    fun observeProfile(): Flow<PersonProfileEntity?>

    @Query("SELECT * FROM person_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): PersonProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: PersonProfileEntity)

    @Query("DELETE FROM person_profile")
    suspend fun clearProfile()
}

@Dao
interface ShareLogDao {
    @Query("SELECT * FROM share_log ORDER BY createdAt DESC")
    fun getAllLogs(): Flow<List<ShareLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ShareLogEntity): Long

    @Query("DELETE FROM share_log WHERE id = :id")
    suspend fun deleteLog(id: Long)

    @Query("DELETE FROM share_log")
    suspend fun clearAllLogs()
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tag ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT * FROM tag WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?
}
