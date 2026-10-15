package com.example.data.backup

import android.content.Context
import com.example.data.crypto.KeystoreHelper
import com.example.data.database.CityDatabase
import com.example.data.database.entity.ChecklistProgressEntity
import com.example.data.database.entity.DocumentEntity
import com.example.data.database.entity.NoteEntity
import com.example.data.database.entity.PersonProfileEntity
import com.example.data.database.entity.SettingEntity
import com.example.data.database.entity.ShareLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupManager {

    private const val MAGIC_HEADER = "C7BAK1\n"
    private const val ITERATIONS = 600_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH = 16
    private const val NONCE_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class DatabaseBackupData(
        val notes: List<NoteBackupItem> = emptyList(),
        val profile: ProfileBackupItem? = null,
        val documents: List<DocumentBackupItem> = emptyList(),
        val settings: List<SettingBackupItem> = emptyList(),
        val checklistProgress: List<ChecklistProgressBackupItem> = emptyList(),
        val shareLogs: List<ShareLogBackupItem> = emptyList()
    )

    @Serializable
    data class NoteBackupItem(
        val id: Long,
        val title: String,
        val body: String,
        val isPrivate: Boolean,
        val cipherBase64: String? = null,
        val ivBase64: String? = null,
        val articleId: String? = null,
        val pinned: Boolean,
        val createdAt: Long,
        val updatedAt: Long
    )

    @Serializable
    data class ProfileBackupItem(
        val fullNameRu: String,
        val fullNameLatin: String,
        val phone: String,
        val email: String,
        val countryResidenceId: String?,
        val countryTargetId: String?,
        val profession: String,
        val about: String,
        val watermarkCustomText: String,
        val defaultVCardFieldsJson: String
    )

    @Serializable
    data class DocumentBackupItem(
        val id: String,
        val category: String,
        val type: String,
        val title: String,
        val filePath: String,
        val mime: String,
        val sizeBytes: Long,
        val expiresAt: Long?,
        val isPinned: Boolean,
        val replacesId: String?,
        val createdAt: Long,
        val updatedAt: Long
    )

    @Serializable
    data class SettingBackupItem(val key: String, val value: String)

    @Serializable
    data class ChecklistProgressBackupItem(val itemId: String, val isDone: Boolean, val doneAt: Long)

    @Serializable
    data class ShareLogBackupItem(val id: Long, val createdAt: Long, val recipientLabel: String?, val documentTitles: String, val watermarked: Boolean)

    suspend fun createBackup(
        context: Context,
        database: CityDatabase,
        password: String,
        outputStream: OutputStream
    ) = withContext(Dispatchers.IO) {
        if (password.length < 8) {
            throw IllegalArgumentException("Пароль должен содержать не менее 8 символов")
        }

        // 1. Gather all data
        val notes = database.noteDao().getAllNotes().first().map {
            NoteBackupItem(
                id = it.id,
                title = it.title,
                body = it.body,
                isPrivate = it.isPrivate,
                cipherBase64 = it.cipher?.let { c -> android.util.Base64.encodeToString(c, android.util.Base64.NO_WRAP) },
                ivBase64 = it.iv?.let { iv -> android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP) },
                articleId = it.articleId,
                pinned = it.pinned,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }

        val profile = database.profileDao().getProfile()?.let {
            ProfileBackupItem(
                fullNameRu = it.fullNameRu,
                fullNameLatin = it.fullNameLatin,
                phone = it.phone,
                email = it.email,
                countryResidenceId = it.countryResidenceId,
                countryTargetId = it.countryTargetId,
                profession = it.profession,
                about = it.about,
                watermarkCustomText = it.watermarkCustomText,
                defaultVCardFieldsJson = it.defaultVCardFieldsJson
            )
        }

        val documents = database.documentDao().getAllActiveDocuments().first()
        val docBackupItems = documents.map {
            DocumentBackupItem(
                id = it.id,
                category = it.category.name,
                type = it.type.name,
                title = it.title,
                filePath = it.filePath,
                mime = it.mime,
                sizeBytes = it.sizeBytes,
                expiresAt = it.expiresAt,
                isPinned = it.isPinned,
                replacesId = it.replacesId,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }

        val progress = database.checklistDao().getAllProgress().first().map {
            ChecklistProgressBackupItem(it.itemId, it.isDone, it.doneAt)
        }

        val logs = database.shareLogDao().getAllLogs().first().map {
            ShareLogBackupItem(it.id, it.createdAt, it.recipientLabel, it.documentTitles, it.watermarked)
        }

        val backupData = DatabaseBackupData(
            notes = notes,
            profile = profile,
            documents = docBackupItems,
            checklistProgress = progress,
            shareLogs = logs
        )

        val jsonString = json.encodeToString(DatabaseBackupData.serializer(), backupData)

        // 2. Build in-memory zip archive with json + decrypted files
        val zipBytes = ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zos ->
                // Entry 1: data.json
                zos.putNextEntry(ZipEntry("data.json"))
                zos.write(jsonString.toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()

                // Entry 2..N: document files
                val docsDir = File(context.filesDir, "docs")
                documents.forEach { doc ->
                    val file = File(docsDir, doc.filePath)
                    if (file.exists()) {
                        try {
                            val decrypted = KeystoreHelper.decryptFileToBytes(file)
                            zos.putNextEntry(ZipEntry("docs/${doc.filePath}"))
                            zos.write(decrypted)
                            zos.closeEntry()
                        } catch (_: Exception) {
                            // Skip corrupted file
                        }
                    }
                }
            }
            baos.toByteArray()
        }

        // 3. Derive key via PBKDF2
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
        val nonce = ByteArray(NONCE_LENGTH).also { random.nextBytes(it) }

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val secretKeyBytes = factory.generateSecret(spec).encoded
        val secretKey = SecretKeySpec(secretKeyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, nonce))
        val cipherBytes = cipher.doFinal(zipBytes)

        // 4. Write: MAGIC_HEADER (bytes) + salt (16) + nonce (12) + cipherBytes
        outputStream.write(MAGIC_HEADER.toByteArray(StandardCharsets.UTF_8))
        outputStream.write(salt)
        outputStream.write(nonce)
        outputStream.write(cipherBytes)
        outputStream.flush()
    }

    suspend fun restoreBackup(
        context: Context,
        database: CityDatabase,
        password: String,
        inputStream: InputStream
    ) = withContext(Dispatchers.IO) {
        val allBytes = inputStream.readBytes()

        // 1. Verify header
        val headerBytes = MAGIC_HEADER.toByteArray(StandardCharsets.UTF_8)
        if (allBytes.size < headerBytes.size + SALT_LENGTH + NONCE_LENGTH + 16) {
            throw IllegalArgumentException("Файл резервной копии повреждён или имеет неверный формат")
        }

        for (i in headerBytes.indices) {
            if (allBytes[i] != headerBytes[i]) {
                throw IllegalArgumentException("Неверная сигнатура файла резервной копии")
            }
        }

        var offset = headerBytes.size
        val salt = allBytes.copyOfRange(offset, offset + SALT_LENGTH)
        offset += SALT_LENGTH
        val nonce = allBytes.copyOfRange(offset, offset + NONCE_LENGTH)
        offset += NONCE_LENGTH
        val cipherBytes = allBytes.copyOfRange(offset, allBytes.size)

        // 2. Derive key
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val secretKeyBytes = factory.generateSecret(spec).encoded
        val secretKey = SecretKeySpec(secretKeyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, nonce))

        val plainZipBytes = try {
            cipher.doFinal(cipherBytes)
        } catch (e: Exception) {
            throw IllegalArgumentException("Неверный пароль или повреждённые данные копии", e)
        }

        // 3. Unpack zip and restore
        var backupData: DatabaseBackupData? = null
        val restoredFiles = mutableMapOf<String, ByteArray>()

        ZipInputStream(plainZipBytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "data.json") {
                    val jsonStr = String(zis.readBytes(), StandardCharsets.UTF_8)
                    backupData = json.decodeFromString(DatabaseBackupData.serializer(), jsonStr)
                } else if (entry.name.startsWith("docs/")) {
                    val fileName = entry.name.removePrefix("docs/")
                    restoredFiles[fileName] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val data = backupData ?: throw IllegalArgumentException("В резервной копии отсутствуют системные данные")

        // 4. Write restored files to filesDir/docs/ encrypted with device keystore
        val docsDir = File(context.filesDir, "docs").apply { mkdirs() }
        restoredFiles.forEach { (fileName, bytes) ->
            val target = File(docsDir, fileName)
            KeystoreHelper.encryptStreamToFile(bytes.inputStream(), target)
        }

        // 5. Restore DB entries
        // Notes
        data.notes.forEach { n ->
            val cipherBytes = n.cipherBase64?.let { android.util.Base64.decode(it, android.util.Base64.DEFAULT) }
            val ivBytes = n.ivBase64?.let { android.util.Base64.decode(it, android.util.Base64.DEFAULT) }
            database.noteDao().insertOrUpdateNote(
                NoteEntity(
                    id = n.id,
                    title = n.title,
                    body = n.body,
                    isPrivate = n.isPrivate,
                    cipher = cipherBytes,
                    iv = ivBytes,
                    articleId = n.articleId,
                    pinned = n.pinned,
                    createdAt = n.createdAt,
                    updatedAt = n.updatedAt
                )
            )
        }

        // Profile
        data.profile?.let { p ->
            database.profileDao().insertOrUpdateProfile(
                PersonProfileEntity(
                    id = 1,
                    fullNameRu = p.fullNameRu,
                    fullNameLatin = p.fullNameLatin,
                    phone = p.phone,
                    email = p.email,
                    countryResidenceId = p.countryResidenceId,
                    countryTargetId = p.countryTargetId,
                    profession = p.profession,
                    about = p.about,
                    watermarkCustomText = p.watermarkCustomText,
                    defaultVCardFieldsJson = p.defaultVCardFieldsJson
                )
            )
        }

        // Documents
        data.documents.forEach { d ->
            database.documentDao().insertDocument(
                DocumentEntity(
                    id = d.id,
                    category = try { com.example.data.database.entity.DocumentCategory.valueOf(d.category) } catch (_: Exception) { com.example.data.database.entity.DocumentCategory.OTHER },
                    type = try { com.example.data.database.entity.DocumentType.valueOf(d.type) } catch (_: Exception) { com.example.data.database.entity.DocumentType.OTHER },
                    title = d.title,
                    filePath = d.filePath,
                    mime = d.mime,
                    sizeBytes = d.sizeBytes,
                    expiresAt = d.expiresAt,
                    isPinned = d.isPinned,
                    replacesId = d.replacesId,
                    createdAt = d.createdAt,
                    updatedAt = d.updatedAt
                )
            )
        }

        // Progress
        data.checklistProgress.forEach { cp ->
            database.checklistDao().setProgress(
                ChecklistProgressEntity(cp.itemId, cp.isDone, cp.doneAt)
            )
        }
    }
}
