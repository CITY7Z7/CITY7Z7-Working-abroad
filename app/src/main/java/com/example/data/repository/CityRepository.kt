package com.example.data.repository

import android.content.Context
import com.example.data.crypto.KeystoreHelper
import com.example.data.database.CityDatabase
import com.example.data.database.entity.ArticleEntity
import com.example.data.database.entity.ArticleKind
import com.example.data.database.entity.ChecklistEntity
import com.example.data.database.entity.ChecklistItemEntity
import com.example.data.database.entity.ChecklistProgressEntity
import com.example.data.database.entity.CountryEntity
import com.example.data.database.entity.DocumentCategory
import com.example.data.database.entity.DocumentEntity
import com.example.data.database.entity.DocumentType
import com.example.data.database.entity.NoteEntity
import com.example.data.database.entity.PersonProfileEntity
import com.example.data.database.entity.RateEntity
import com.example.data.database.entity.SectionEntity
import com.example.data.database.entity.SettingEntity
import com.example.data.database.entity.ShareLogEntity
import com.example.data.database.entity.UserContactEntity
import com.example.data.database.entity.VerificationStatus
import com.example.domain.search.SearchQueryBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.time.LocalDate
import java.util.UUID

data class SearchResults(
    val articles: List<ArticleEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList()
)

class CityRepository(
    private val context: Context,
    private val database: CityDatabase
) {

    // --- Countries & Sections ---
    val allCountries: Flow<List<CountryEntity>> = database.countryDao().getAllCountries()

    suspend fun getCountryById(id: String): CountryEntity? = database.countryDao().getCountryById(id)

    fun getSectionsForCountry(countryId: String): Flow<List<SectionEntity>> =
        database.sectionDao().getSectionsForCountry(countryId)

    // --- Articles & Content ---
    fun getArticlesBySection(countryId: String, sectionId: String): Flow<List<ArticleEntity>> =
        database.articleDao().getArticlesBySection(countryId, sectionId)

    fun getArticlesByKind(kind: ArticleKind, countryId: String? = null): Flow<List<ArticleEntity>> =
        database.articleDao().getArticlesByKind(kind, countryId)

    fun getFactCardForCountry(countryId: String): Flow<ArticleEntity?> =
        database.articleDao().getFactCardForCountry(countryId)

    suspend fun getArticleById(id: String): ArticleEntity? = database.articleDao().getArticleById(id)

    fun observeArticleById(id: String): Flow<ArticleEntity?> = database.articleDao().observeArticleById(id)

    suspend fun insertArticles(articles: List<ArticleEntity>) = database.articleDao().insertArticles(articles)

    suspend fun insertArticle(article: ArticleEntity) = database.articleDao().insertArticle(article)

    suspend fun getAllActiveArticles(): List<ArticleEntity> = database.articleDao().getAllActiveArticles()

    suspend fun deleteArticle(articleId: String) = withContext(Dispatchers.IO) {
        database.articleDao().deleteArticle(articleId)
    }

    suspend fun addCountry(
        nameRu: String,
        flagEmoji: String,
        language: String,
        currencyCode: String,
        currencySymbol: String
    ): CountryEntity = withContext(Dispatchers.IO) {
        val baseSlug = nameRu.lowercase().replace(" ", "_").filter { it in 'a'..'z' || it in '0'..'9' }.take(6)
        val slug = if (baseSlug.isNotBlank()) baseSlug else "c_" + UUID.randomUUID().toString().take(4)
        val country = CountryEntity(
            id = slug,
            code = slug.uppercase().take(2),
            nameRu = nameRu.trim(),
            nameLocal = language.trim(),
            flagEmoji = flagEmoji.trim().ifBlank { "🌐" },
            currencyCode = currencyCode.trim().ifBlank { "EUR" },
            currencySymbol = currencySymbol.trim().ifBlank { "€" },
            sortOrder = 50
        )
        database.countryDao().insertCountry(country)

        // 5 default sections according to docs/PRINCIPLES.md
        val defaultSections = listOf(
            SectionEntity(id = "$slug.status", countryId = slug, slug = "status", titleRu = "Статус и разрешения", iconKey = "badge", sortOrder = 1),
            SectionEntity(id = "$slug.work", countryId = slug, slug = "work", titleRu = "Работа и права", iconKey = "work", sortOrder = 2),
            SectionEntity(id = "$slug.tax", countryId = slug, slug = "tax", titleRu = "Налоги и зарплата", iconKey = "payments", sortOrder = 3),
            SectionEntity(id = "$slug.housing", countryId = slug, slug = "housing", titleRu = "Жильё и аренда", iconKey = "home", sortOrder = 4),
            SectionEntity(id = "$slug.health", countryId = slug, slug = "health", titleRu = "Здоровье и медицина", iconKey = "health_and_safety", sortOrder = 5)
        )
        database.sectionDao().insertAll(defaultSections)

        // Initial empty glossary and phrasebook for this country
        val initialGlossary = ArticleEntity(
            id = "$slug.glossary",
            countryId = slug,
            sectionId = null,
            kind = ArticleKind.GLOSSARY,
            titleRu = "Словарь терминов (${nameRu.trim()})",
            subtitle = "Ключевые термины и сокращения",
            bodyMd = "# Словарь терминов\n\nСловарь пуст. Добавьте первый термин через «+ Новый термин» или загрузите файл.",
            keywords = "словарь термины $slug",
            applicableTo = "ANY",
            sourceIds = "",
            verifiedAt = LocalDate.now().toString(),
            status = VerificationStatus.VERIFIED,
            sortOrder = 1
        )
        val initialPhrases = ArticleEntity(
            id = "$slug.phrases",
            countryId = slug,
            sectionId = null,
            kind = ArticleKind.PHRASE_SET,
            titleRu = "Разговорник (${nameRu.trim()})",
            subtitle = "Фразы для работы, жилья, врача",
            bodyMd = "# Разговорник\n\nРазговорник пуст. Добавьте первую фразу через «+ Новая фраза» или загрузите файл.",
            keywords = "разговорник фразы $slug",
            applicableTo = "ANY",
            sourceIds = "",
            verifiedAt = LocalDate.now().toString(),
            status = VerificationStatus.VERIFIED,
            sortOrder = 2
        )
        database.articleDao().insertArticle(initialGlossary)
        database.articleDao().insertArticle(initialPhrases)

        // Key figures initial rate
        val defaultRate = RateEntity(
            id = "$slug.min_wage_adult",
            countryId = slug,
            key = "$slug.min_wage_adult",
            value = 0.0,
            unit = "${country.currencySymbol}/ч",
            validFrom = LocalDate.now().toString(),
            sourceId = "manual_entry",
            status = VerificationStatus.VERIFIED
        )
        database.rateDao().insertRate(defaultRate)

        country
    }

    suspend fun deleteCountry(countryId: String) = withContext(Dispatchers.IO) {
        if (countryId.equals("ie", ignoreCase = true)) {
            // Ireland is the primary foundational country in the system and cannot be deleted
            return@withContext
        }
        database.articleDao().deleteArticlesForCountry(countryId)
        database.sectionDao().deleteSectionsForCountry(countryId)
        database.rateDao().deleteRatesForCountry(countryId)
        database.countryDao().deleteCountry(countryId)
    }

    suspend fun saveRate(rate: RateEntity) = withContext(Dispatchers.IO) {
        database.rateDao().insertRate(rate)
    }

    fun getRatesForCountry(countryId: String): Flow<List<RateEntity>> =
        database.rateDao().getRatesForCountry(countryId)

    suspend fun getLatestRate(countryId: String, key: String): RateEntity? {
        val today = LocalDate.now().toString()
        return database.rateDao().getLatestRate(countryId, key, today)
    }

    // --- Checklists ---
    fun getChecklistsForCountry(countryId: String): Flow<List<ChecklistEntity>> =
        database.checklistDao().getChecklistsForCountry(countryId)

    fun getItemsForChecklist(checklistId: String): Flow<List<ChecklistItemEntity>> =
        database.checklistDao().getItemsForChecklist(checklistId)

    val allChecklistProgress: Flow<List<ChecklistProgressEntity>> =
        database.checklistDao().getAllProgress()

    suspend fun toggleChecklistItem(itemId: String, isDone: Boolean) {
        database.checklistDao().setProgress(ChecklistProgressEntity(itemId, isDone))
    }

    suspend fun resetChecklist(checklistId: String) {
        database.checklistDao().resetChecklistProgress(checklistId)
    }

    // --- Notes ---
    val allNotes: Flow<List<NoteEntity>> = database.noteDao().getAllNotes()

    fun getNotesForArticle(articleId: String): Flow<List<NoteEntity>> =
        database.noteDao().getNotesForArticle(articleId)

    suspend fun getNoteById(id: Long): NoteEntity? = database.noteDao().getNoteById(id)

    fun observeNoteById(id: Long): Flow<NoteEntity?> = database.noteDao().observeNoteById(id)

    suspend fun saveNote(note: NoteEntity): Long = database.noteDao().insertOrUpdateNote(note)

    suspend fun deleteNote(id: Long) = database.noteDao().deleteNote(id)

    // --- Search ---
    suspend fun search(rawQuery: String): SearchResults = withContext(Dispatchers.IO) {
        val ftsQuery = SearchQueryBuilder.buildFtsQuery(rawQuery)
        if (ftsQuery.isBlank()) {
            return@withContext SearchResults()
        }

        val articles = try {
            database.articleDao().searchArticlesFts(ftsQuery)
        } catch (_: Exception) {
            emptyList()
        }

        val notes = try {
            database.noteDao().searchNotesFts(ftsQuery)
        } catch (_: Exception) {
            emptyList()
        }

        SearchResults(articles, notes)
    }

    // --- Contacts & SOS ---
    fun getContacts(countryId: String?): Flow<List<UserContactEntity>> =
        database.contactDao().getContacts(countryId)

    suspend fun saveContact(contact: UserContactEntity) = database.contactDao().insertContact(contact)

    suspend fun deleteContact(id: String) = database.contactDao().deleteContact(id)

    // --- Documents & Profile ---
    val allActiveDocuments: Flow<List<DocumentEntity>> = database.documentDao().getAllActiveDocuments()
    val pinnedDocuments: Flow<List<DocumentEntity>> = database.documentDao().getPinnedDocuments()
    val recentDocuments: Flow<List<DocumentEntity>> = database.documentDao().getRecentDocuments(5)

    fun getDocumentsByCategory(category: DocumentCategory): Flow<List<DocumentEntity>> =
        database.documentDao().getDocumentsByCategory(category)

    fun getExpiringDocuments(maxDays: Long = 90): Flow<List<DocumentEntity>> {
        val maxEpochDay = LocalDate.now().plusDays(maxDays).toEpochDay()
        return database.documentDao().getExpiringDocuments(maxEpochDay)
    }

    suspend fun getDocumentById(id: String): DocumentEntity? = database.documentDao().getDocumentById(id)

    fun observeDocumentById(id: String): Flow<DocumentEntity?> = database.documentDao().observeDocumentById(id)

    suspend fun importDocumentFile(
        title: String,
        category: DocumentCategory,
        type: DocumentType,
        mime: String,
        expiresAt: Long?,
        inputStream: InputStream
    ): DocumentEntity = withContext(Dispatchers.IO) {
        val uuid = UUID.randomUUID().toString()
        val fileName = "$uuid.bin"
        val docsDir = File(context.filesDir, "docs").apply { mkdirs() }
        val targetFile = File(docsDir, fileName)

        // Encrypt with Keystore AES-256-GCM
        KeystoreHelper.encryptStreamToFile(inputStream, targetFile)

        val doc = DocumentEntity(
            id = uuid,
            category = category,
            type = type,
            title = title,
            filePath = fileName,
            mime = mime,
            sizeBytes = targetFile.length(),
            expiresAt = expiresAt
        )
        database.documentDao().insertDocument(doc)
        doc
    }

    suspend fun getDecryptedDocumentBytes(document: DocumentEntity): ByteArray = withContext(Dispatchers.IO) {
        val file = File(File(context.filesDir, "docs"), document.filePath)
        if (!file.exists()) {
            throw IllegalStateException("Файл документа не найден на устройстве")
        }
        KeystoreHelper.decryptFileToBytes(file)
    }

    suspend fun deleteDocument(id: String) = withContext(Dispatchers.IO) {
        val doc = database.documentDao().getDocumentById(id)
        if (doc != null) {
            val file = File(File(context.filesDir, "docs"), doc.filePath)
            if (file.exists()) file.delete()
            database.documentDao().deleteDocument(id)
        }
    }

    suspend fun updateDocumentPinned(id: String, pinned: Boolean) {
        val doc = database.documentDao().getDocumentById(id) ?: return
        database.documentDao().insertDocument(doc.copy(isPinned = pinned, updatedAt = System.currentTimeMillis()))
    }

    // Profile
    val profile: Flow<PersonProfileEntity?> = database.profileDao().observeProfile()

    suspend fun saveProfile(profile: PersonProfileEntity) {
        database.profileDao().insertOrUpdateProfile(profile.copy(updatedAt = System.currentTimeMillis()))
    }

    // Share Logs
    val shareLogs: Flow<List<ShareLogEntity>> = database.shareLogDao().getAllLogs()

    suspend fun logShare(recipient: String?, documentTitles: List<String>, watermarked: Boolean) {
        val titlesString = documentTitles.joinToString(", ")
        database.shareLogDao().insertLog(
            ShareLogEntity(
                recipientLabel = recipient,
                documentTitles = titlesString,
                watermarked = watermarked
            )
        )
    }

    suspend fun deleteShareLog(id: Long) = database.shareLogDao().deleteLog(id)

    // Settings
    fun observeSetting(key: String): Flow<String?> = database.settingDao().observeSetting(key)

    suspend fun getSetting(key: String): String? = database.settingDao().getSetting(key)

    suspend fun setSetting(key: String, value: String) {
        database.settingDao().setSetting(SettingEntity(key, value))
    }

    suspend fun clearAllUserData() = withContext(Dispatchers.IO) {
        database.noteDao().getAllNotes() // clean
        database.documentDao().clearAllDocuments()
        database.profileDao().clearProfile()
        database.shareLogDao().clearAllLogs()

        // Clear files in filesDir/docs and cacheDir
        File(context.filesDir, "docs").deleteRecursively()
        File(context.cacheDir, "share").deleteRecursively()
    }
}
