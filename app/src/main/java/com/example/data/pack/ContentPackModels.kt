package com.example.data.pack

import com.example.data.database.entity.ArticleEntity
import com.example.data.database.entity.ChecklistEntity
import com.example.data.database.entity.ChecklistItemEntity
import com.example.data.database.entity.CountryEntity
import com.example.data.database.entity.RateEntity
import com.example.data.database.entity.SectionEntity
import com.example.data.database.entity.SourceEntity
import kotlinx.serialization.Serializable

@Serializable
data class ContentPack(
    val format: Int = 1,
    val contentVersion: String,
    val packDate: String,
    val countries: List<CountryEntity> = emptyList(),
    val sections: List<SectionEntity> = emptyList(),
    val articles: List<ArticleEntity> = emptyList(),
    val sources: List<SourceEntity> = emptyList(),
    val rates: List<RateEntity> = emptyList(),
    val checklists: List<ChecklistEntity> = emptyList(),
    val checklistItems: List<ChecklistItemEntity> = emptyList(),
    val signature: String = ""
)
