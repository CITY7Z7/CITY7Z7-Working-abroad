package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.database.dao.ArticleDao
import com.example.data.database.dao.ChecklistDao
import com.example.data.database.dao.ContactDao
import com.example.data.database.dao.ContentMetaDao
import com.example.data.database.dao.CountryDao
import com.example.data.database.dao.DocumentDao
import com.example.data.database.dao.NoteDao
import com.example.data.database.dao.ProfileDao
import com.example.data.database.dao.RateDao
import com.example.data.database.dao.SectionDao
import com.example.data.database.dao.SettingDao
import com.example.data.database.dao.ShareLogDao
import com.example.data.database.dao.SourceDao
import com.example.data.database.dao.TagDao
import com.example.data.database.entity.ArticleEntity
import com.example.data.database.entity.ArticleFtsEntity
import com.example.data.database.entity.ChecklistEntity
import com.example.data.database.entity.ChecklistItemEntity
import com.example.data.database.entity.ChecklistProgressEntity
import com.example.data.database.entity.ContentMetaEntity
import com.example.data.database.entity.CountryEntity
import com.example.data.database.entity.DocumentAlertEntity
import com.example.data.database.entity.DocumentEntity
import com.example.data.database.entity.DocumentTagCrossRef
import com.example.data.database.entity.NoteEntity
import com.example.data.database.entity.NoteFtsEntity
import com.example.data.database.entity.PersonProfileEntity
import com.example.data.database.entity.RateEntity
import com.example.data.database.entity.SectionEntity
import com.example.data.database.entity.SettingEntity
import com.example.data.database.entity.ShareLogEntity
import com.example.data.database.entity.SourceEntity
import com.example.data.database.entity.TagEntity
import com.example.data.database.entity.UserContactEntity

@Database(
    entities = [
        CountryEntity::class,
        SectionEntity::class,
        ArticleEntity::class,
        ArticleFtsEntity::class,
        SourceEntity::class,
        RateEntity::class,
        ChecklistEntity::class,
        ChecklistItemEntity::class,
        ChecklistProgressEntity::class,
        NoteEntity::class,
        NoteFtsEntity::class,
        UserContactEntity::class,
        SettingEntity::class,
        ContentMetaEntity::class,
        PersonProfileEntity::class,
        DocumentEntity::class,
        TagEntity::class,
        DocumentTagCrossRef::class,
        DocumentAlertEntity::class,
        ShareLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class CityDatabase : RoomDatabase() {

    abstract fun countryDao(): CountryDao
    abstract fun sectionDao(): SectionDao
    abstract fun articleDao(): ArticleDao
    abstract fun sourceDao(): SourceDao
    abstract fun rateDao(): RateDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun noteDao(): NoteDao
    abstract fun contactDao(): ContactDao
    abstract fun settingDao(): SettingDao
    abstract fun contentMetaDao(): ContentMetaDao
    abstract fun documentDao(): DocumentDao
    abstract fun profileDao(): ProfileDao
    abstract fun shareLogDao(): ShareLogDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: CityDatabase? = null

        fun getInstance(context: Context): CityDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CityDatabase::class.java,
                    "city7z7_database.db"
                )
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
