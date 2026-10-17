package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.database.entity.ArticleEntity
import com.example.data.database.entity.ArticleKind
import com.example.data.database.entity.ChecklistEntity
import com.example.data.database.entity.ChecklistItemEntity
import com.example.data.database.entity.ChecklistProgressEntity
import com.example.data.database.entity.ContentMetaEntity
import com.example.data.database.entity.CountryEntity
import com.example.data.database.entity.RateEntity
import com.example.data.database.entity.SectionEntity
import com.example.data.database.entity.SettingEntity
import com.example.data.database.entity.SourceEntity
import com.example.data.database.entity.UserContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CountryDao {
    @Query("SELECT * FROM country ORDER BY sortOrder ASC")
    fun getAllCountries(): Flow<List<CountryEntity>>

    @Query("SELECT * FROM country WHERE id = :id LIMIT 1")
    suspend fun getCountryById(id: String): CountryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(countries: List<CountryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCountry(country: CountryEntity)

    @Query("DELETE FROM country WHERE id = :countryId")
    suspend fun deleteCountry(countryId: String)
}

@Dao
interface SectionDao {
    @Query("SELECT * FROM section WHERE countryId = :countryId OR countryId IS NULL ORDER BY sortOrder ASC")
    fun getSectionsForCountry(countryId: String): Flow<List<SectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sections: List<SectionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: SectionEntity)

    @Query("DELETE FROM section WHERE countryId = :countryId")
    suspend fun deleteSectionsForCountry(countryId: String)
}

@Dao
interface ArticleDao {
    @Query("SELECT * FROM article WHERE id = :id AND archived = 0 LIMIT 1")
    suspend fun getArticleById(id: String): ArticleEntity?

    @Query("SELECT * FROM article WHERE id = :id LIMIT 1")
    fun observeArticleById(id: String): Flow<ArticleEntity?>

    @Query("SELECT * FROM article WHERE countryId = :countryId AND sectionId = :sectionId AND archived = 0 ORDER BY sortOrder ASC")
    fun getArticlesBySection(countryId: String, sectionId: String): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM article WHERE kind = :kind AND (:countryId IS NULL OR countryId = :countryId OR countryId IS NULL) AND archived = 0 ORDER BY sortOrder ASC")
    fun getArticlesByKind(kind: ArticleKind, countryId: String? = null): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM article WHERE countryId = :countryId AND kind = 'FACT_CARD' AND archived = 0 LIMIT 1")
    fun getFactCardForCountry(countryId: String): Flow<ArticleEntity?>

    @Query("""
        SELECT a.* FROM article a
        JOIN article_fts f ON a.rowid = f.docid
        WHERE article_fts MATCH :query AND a.archived = 0
        LIMIT 50
    """)
    suspend fun searchArticlesFts(query: String): List<ArticleEntity>

    @Query("SELECT * FROM article WHERE archived = 0")
    suspend fun getAllActiveArticles(): List<ArticleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: ArticleEntity)

    @Query("INSERT INTO article_fts(docid, titleRu, keywords, bodyMd) VALUES(:docid, :titleRu, :keywords, :bodyMd)")
    suspend fun insertArticleFts(docid: Long, titleRu: String, keywords: String, bodyMd: String)

    @Query("DELETE FROM article_fts")
    suspend fun clearArticleFts()

    @Query("UPDATE article SET archived = 1 WHERE id NOT IN (:activeIds) AND countryId = :countryId")
    suspend fun archiveMissingArticles(activeIds: List<String>, countryId: String)

    @Query("DELETE FROM article WHERE id = :id")
    suspend fun deleteArticle(id: String)

    @Query("DELETE FROM article WHERE countryId = :countryId")
    suspend fun deleteArticlesForCountry(countryId: String)
}

@Dao
interface SourceDao {
    @Query("SELECT * FROM source WHERE id IN (:ids)")
    suspend fun getSourcesByIds(ids: List<String>): List<SourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sources: List<SourceEntity>)
}

@Dao
interface RateDao {
    @Query("SELECT * FROM rate WHERE countryId = :countryId AND key = :key AND validFrom <= :currentDate ORDER BY validFrom DESC LIMIT 1")
    suspend fun getLatestRate(countryId: String, key: String, currentDate: String): RateEntity?

    @Query("SELECT * FROM rate WHERE countryId = :countryId ORDER BY key ASC, validFrom DESC")
    fun getRatesForCountry(countryId: String): Flow<List<RateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rates: List<RateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: RateEntity)

    @Query("DELETE FROM rate WHERE countryId = :countryId")
    suspend fun deleteRatesForCountry(countryId: String)
}

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist WHERE countryId = :countryId OR countryId IS NULL ORDER BY sortOrder ASC")
    fun getChecklistsForCountry(countryId: String): Flow<List<ChecklistEntity>>

    @Query("SELECT * FROM checklist WHERE id = :id LIMIT 1")
    suspend fun getChecklistById(id: String): ChecklistEntity?

    @Query("SELECT * FROM checklist_item WHERE checklistId = :checklistId ORDER BY sortOrder ASC")
    fun getItemsForChecklist(checklistId: String): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM checklist_progress")
    fun getAllProgress(): Flow<List<ChecklistProgressEntity>>

    @Query("SELECT * FROM checklist_progress WHERE itemId = :itemId LIMIT 1")
    suspend fun getProgressForItem(itemId: String): ChecklistProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setProgress(progress: ChecklistProgressEntity)

    @Query("DELETE FROM checklist_progress WHERE itemId IN (SELECT id FROM checklist_item WHERE checklistId = :checklistId)")
    suspend fun resetChecklistProgress(checklistId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklists(checklists: List<ChecklistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItems(items: List<ChecklistItemEntity>)
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM user_contact WHERE countryId = :countryId OR countryId IS NULL ORDER BY label ASC")
    fun getContacts(countryId: String?): Flow<List<UserContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: UserContactEntity)

    @Query("DELETE FROM user_contact WHERE id = :id")
    suspend fun deleteContact(id: String)
}

@Dao
interface SettingDao {
    @Query("SELECT value FROM setting WHERE key = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Query("SELECT value FROM setting WHERE key = :key LIMIT 1")
    fun observeSetting(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: SettingEntity)
}

@Dao
interface ContentMetaDao {
    @Query("SELECT value FROM content_meta WHERE key = :key LIMIT 1")
    suspend fun getMeta(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setMeta(meta: ContentMetaEntity)
}
