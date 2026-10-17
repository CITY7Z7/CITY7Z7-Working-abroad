package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "country")
@Serializable
data class CountryEntity(
    @PrimaryKey val id: String, // e.g. "ie", "de", "nl"
    val code: String,           // "IE", "DE"
    val nameRu: String,
    val nameLocal: String,
    val flagEmoji: String,
    val currencyCode: String,
    val currencySymbol: String,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "section",
    indices = [Index(value = ["countryId"])]
)
@Serializable
data class SectionEntity(
    @PrimaryKey val id: String, // e.g. "ie.status", "ie.work"
    val countryId: String?,      // null for global sections
    val slug: String,
    val titleRu: String,
    val iconKey: String,
    val sortOrder: Int = 0
)

enum class ArticleKind {
    GUIDE,
    SITUATION,
    PHRASE_SET,
    GLOSSARY,
    TEMPLATE,
    FACT_CARD
}

enum class VerificationStatus {
    VERIFIED,
    TO_VERIFY
}

@Entity(
    tableName = "article",
    indices = [
        Index(value = ["countryId"]),
        Index(value = ["sectionId"]),
        Index(value = ["kind"])
    ]
)
@Serializable
data class ArticleEntity(
    @PrimaryKey val id: String, // e.g. "ie.w1.pps"
    val countryId: String?,
    val sectionId: String?,
    val kind: ArticleKind,
    val titleRu: String,
    val subtitle: String = "",
    val bodyMd: String,
    val keywords: String = "",
    val applicableTo: String = "ANY", // "EU_EEA,THIRD_COUNTRY,TEMP_PROTECTION,STUDENT,ANY"
    val sourceIds: String = "",       // Comma separated source ids
    val verifiedAt: String = "2026-10-14",
    val status: VerificationStatus = VerificationStatus.VERIFIED,
    val archived: Int = 0,
    val sortOrder: Int = 0
)

@Entity(tableName = "article_fts")
@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)
data class ArticleFtsEntity(
    val titleRu: String,
    val keywords: String,
    val bodyMd: String
)

@Entity(tableName = "source")
@Serializable
data class SourceEntity(
    @PrimaryKey val id: String,
    val orgName: String,
    val urlText: String, // Text only, never auto-opened
    val checkedAt: String
)

@Entity(
    tableName = "rate",
    indices = [
        Index(value = ["countryId", "key", "validFrom"])
    ]
)
@Serializable
data class RateEntity(
    @PrimaryKey val id: String, // e.g. "ie.min_wage_adult"
    val countryId: String,
    val key: String,
    val value: Double,
    val unit: String,           // "EUR/h", "EUR/year", "percent"
    val validFrom: String,
    val validTo: String? = null,
    val sourceId: String,
    val status: VerificationStatus = VerificationStatus.VERIFIED
)

@Entity(
    tableName = "checklist",
    indices = [Index(value = ["countryId"])]
)
@Serializable
data class ChecklistEntity(
    @PrimaryKey val id: String, // e.g. "ie.w1"
    val countryId: String?,
    val titleRu: String,
    val applicableTo: String = "ANY",
    val sortOrder: Int = 0
)

@Entity(
    tableName = "checklist_item",
    indices = [Index(value = ["checklistId"])]
)
@Serializable
data class ChecklistItemEntity(
    @PrimaryKey val id: String, // e.g. "ie.w1.pps"
    val checklistId: String,
    val textRu: String,
    val textLocal: String? = null,
    val sortOrder: Int = 0
)

@Entity(tableName = "checklist_progress")
data class ChecklistProgressEntity(
    @PrimaryKey val itemId: String,
    val isDone: Boolean,
    val doneAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "user_contact",
    indices = [Index(value = ["countryId"])]
)
data class UserContactEntity(
    @PrimaryKey val id: String,
    val label: String,
    val phone: String,
    val note: String = "",
    val countryId: String? = null
)

@Entity(tableName = "setting")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "content_meta")
data class ContentMetaEntity(
    @PrimaryKey val key: String,
    val value: String
)

data class CanonicalDefaultSection(
    val slug: String,
    val titleRu: String,
    val iconKey: String,
    val sortOrder: Int
)

object CanonicalDefaultSections {
    val LIST = listOf(
        CanonicalDefaultSection("status", "Статус и разрешения", "badge", 1),
        CanonicalDefaultSection("work", "Работа и права", "work", 2),
        CanonicalDefaultSection("tax", "Налоги и зарплата", "payments", 3),
        CanonicalDefaultSection("housing", "Жильё и аренда", "home", 4),
        CanonicalDefaultSection("health", "Здоровье и медицина", "health_and_safety", 5)
    )
}
