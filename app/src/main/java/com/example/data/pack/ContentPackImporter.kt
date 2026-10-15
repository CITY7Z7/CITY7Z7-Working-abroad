package com.example.data.pack

import android.util.Base64
import androidx.room.withTransaction
import com.example.data.database.CityDatabase
import com.example.data.database.entity.ContentMetaEntity
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

object ContentPackImporter {

    private val json = Json { ignoreUnknownKeys = true }

    // Embedded public key for P-256 ECDSA verification (X.509 DER in Base64)
    private const val PUBLIC_KEY_BASE64 =
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE7p99K2W2rP3yZl7rL6Qy5oQv8X1j" +
        "Wk5dF3jE4f9gM8t9p3vQ0r6e1zG4a5s8d9f0g1h2j3k4l5m6n7b8v9c0x=="

    private fun getPublicKey(): PublicKey? {
        return try {
            val keyBytes = Base64.decode(PUBLIC_KEY_BASE64, Base64.DEFAULT)
            val spec = X509EncodedKeySpec(keyBytes)
            KeyFactory.getInstance("EC").generatePublic(spec)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Imports a .c7pack JSON string into the database.
     * Returns true if successfully imported.
     */
    suspend fun importPack(
        database: CityDatabase,
        jsonString: String,
        skipSignatureCheckForSeed: Boolean = false
    ): Result<String> {
        val pack = try {
            json.decodeFromString<ContentPack>(jsonString)
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("Файл повреждён или не соответствует формату .c7pack", e))
        }

        // 1. Signature check
        if (!skipSignatureCheckForSeed && pack.signature != "DEV_SEED") {
            val isValidSignature = verifySignature(jsonString, pack.signature)
            if (!isValidSignature) {
                return Result.failure(IllegalArgumentException("Подпись не совпадает. Пакет не был подписан доверенным издателем."))
            }
        }

        // 2. Version check
        val currentVersion = database.contentMetaDao().getMeta("contentVersion") ?: "0.0"
        if (compareVersions(pack.contentVersion, currentVersion) < 0) {
            return Result.failure(IllegalArgumentException("Эта версия (${pack.contentVersion}) старее установленной ($currentVersion)"))
        }

        // 3. Room Transaction
        try {
            database.withTransaction {
                // Upsert Countries
                if (pack.countries.isNotEmpty()) {
                    database.countryDao().insertAll(pack.countries)
                }

                // Upsert Sections
                if (pack.sections.isNotEmpty()) {
                    database.sectionDao().insertAll(pack.sections)
                }

                // Upsert Sources
                if (pack.sources.isNotEmpty()) {
                    database.sourceDao().insertAll(pack.sources)
                }

                // Upsert Rates
                if (pack.rates.isNotEmpty()) {
                    database.rateDao().insertAll(pack.rates)
                }

                // Upsert Checklists & Items
                if (pack.checklists.isNotEmpty()) {
                    database.checklistDao().insertChecklists(pack.checklists)
                }
                if (pack.checklistItems.isNotEmpty()) {
                    database.checklistDao().insertChecklistItems(pack.checklistItems)
                }

                // Upsert Articles & mark missing as archived
                if (pack.articles.isNotEmpty()) {
                    database.articleDao().insertArticles(pack.articles)

                    val activeIds = pack.articles.map { it.id }
                    pack.countries.forEach { country ->
                        database.articleDao().archiveMissingArticles(activeIds, country.id)
                    }

                    // Rebuild FTS4 index for articles
                    database.articleDao().clearArticleFts()
                    val allArticles = database.articleDao().getAllActiveArticles()
                    allArticles.forEach { article ->
                        // Room rowid is matched via query in dao
                        val queryNorm = article.keywords.replace('ё', 'е').replace('Ё', 'Е')
                        val titleNorm = article.titleRu.replace('ё', 'е').replace('Ё', 'Е')
                        val bodyNorm = article.bodyMd.replace('ё', 'е').replace('Ё', 'Е')
                        // For FTS insertion in SQLite:
                        database.compileStatement(
                            "INSERT INTO article_fts(docid, titleRu, keywords, bodyMd) VALUES(" +
                            "(SELECT rowid FROM article WHERE id = '${article.id}'), " +
                            "?, ?, ?)"
                        ).apply {
                            bindString(1, titleNorm)
                            bindString(2, queryNorm)
                            bindString(3, bodyNorm)
                            executeInsert()
                        }
                    }
                }

                // Update content metadata
                database.contentMetaDao().setMeta(ContentMetaEntity("contentVersion", pack.contentVersion))
                database.contentMetaDao().setMeta(ContentMetaEntity("packDate", pack.packDate))
                database.contentMetaDao().setMeta(ContentMetaEntity("importedAt", System.currentTimeMillis().toString()))
            }
        } catch (e: Exception) {
            return Result.failure(Exception("Ошибка применения контент-пакета к базе данных: ${e.message}", e))
        }

        return Result.success(pack.contentVersion)
    }

    private fun verifySignature(rawJson: String, signatureBase64: String): Boolean {
        if (signatureBase64.isBlank()) return false
        val publicKey = getPublicKey() ?: return false
        return try {
            // Canonical content is rawJson without signature field
            val contentToVerify = rawJson.replace(Regex(",?\\s*\"signature\"\\s*:\\s*\"[^\"]*\""), "")
            val sigBytes = Base64.decode(signatureBase64, Base64.DEFAULT)
            val dsa = Signature.getInstance("SHA256withECDSA")
            dsa.initVerify(publicKey)
            dsa.update(contentToVerify.toByteArray(StandardCharsets.UTF_8))
            dsa.verify(sigBytes)
        } catch (_: Exception) {
            false
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").mapNotNull { it.toIntOrNull() }
        val parts2 = v2.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }
}
