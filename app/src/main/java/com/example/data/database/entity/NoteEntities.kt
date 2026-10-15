package com.example.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.PrimaryKey

@Entity(tableName = "note")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val isPrivate: Boolean = false,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val cipher: ByteArray? = null,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val iv: ByteArray? = null,
    val articleId: String? = null,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NoteEntity

        if (id != other.id) return false
        if (title != other.title) return false
        if (body != other.body) return false
        if (isPrivate != other.isPrivate) return false
        if (cipher != null) {
            if (other.cipher == null) return false
            if (!cipher.contentEquals(other.cipher)) return false
        } else if (other.cipher != null) return false
        if (iv != null) {
            if (other.iv == null) return false
            if (!iv.contentEquals(other.iv)) return false
        } else if (other.iv != null) return false
        if (articleId != other.articleId) return false
        if (pinned != other.pinned) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + body.hashCode()
        result = 31 * result + isPrivate.hashCode()
        result = 31 * result + (cipher?.contentHashCode() ?: 0)
        result = 31 * result + (iv?.contentHashCode() ?: 0)
        result = 31 * result + (articleId?.hashCode() ?: 0)
        result = 31 * result + pinned.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}

@Entity(tableName = "note_fts")
@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)
data class NoteFtsEntity(
    val title: String,
    val body: String
)
