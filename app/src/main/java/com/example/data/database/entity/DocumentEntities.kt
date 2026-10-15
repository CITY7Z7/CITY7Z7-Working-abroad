package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class DocumentCategory(val titleRu: String) {
    ID("Удостоверения личности"),
    WORK("Разрешения на работу"),
    EDUCATION("Образование"),
    OTHER("Прочее")
}

enum class DocumentType(val titleRu: String, val defaultCategory: DocumentCategory) {
    PASSPORT("Паспорт", DocumentCategory.ID),
    ID_CARD("ID-карта", DocumentCategory.ID),
    DRIVING_LICENCE("Водительское удостоверение", DocumentCategory.ID),
    VISA("Виза", DocumentCategory.WORK),
    WORK_PERMIT("Разрешение на работу", DocumentCategory.WORK),
    IRP("Карта IRP", DocumentCategory.WORK),
    HEALTH_INSURANCE("Медицинская страховка", DocumentCategory.OTHER),
    EDUCATION("Диплом / Сертификат", DocumentCategory.EDUCATION),
    OTHER("Другой документ", DocumentCategory.OTHER)
}

@Entity(tableName = "person_profile")
data class PersonProfileEntity(
    @PrimaryKey val id: Int = 1,
    val fullNameRu: String = "",
    val fullNameLatin: String = "",
    val phone: String = "",
    val email: String = "",
    val countryResidenceId: String? = null,
    val countryTargetId: String? = null,
    val profession: String = "",
    val about: String = "",
    val photoEncPath: String? = null,
    val watermarkCustomText: String = "",
    val defaultVCardFieldsJson: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "document",
    indices = [
        Index(value = ["expiresAt"]),
        Index(value = ["category"]),
        Index(value = ["isPinned"])
    ]
)
data class DocumentEntity(
    @PrimaryKey val id: String, // UUID
    val category: DocumentCategory,
    val type: DocumentType,
    val title: String,
    val filePath: String,       // Relative or file name inside filesDir/docs/
    val mime: String,
    val sizeBytes: Long,
    val expiresAt: Long?,       // EpochDay, null = without expiration
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val replacesId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tag")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "document_tag",
    primaryKeys = ["documentId", "tagId"],
    indices = [Index(value = ["tagId"])]
)
data class DocumentTagCrossRef(
    val documentId: String,
    val tagId: Long
)

@Entity(
    tableName = "document_alert",
    primaryKeys = ["documentId", "threshold"]
)
data class DocumentAlertEntity(
    val documentId: String,
    val threshold: Int, // e.g. 90, 60, 30, 0
    val notifiedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "share_log")
data class ShareLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val recipientLabel: String? = null,
    val documentTitles: String, // JSON array string or comma-separated titles
    val watermarked: Boolean = true
)
