package com.example

import android.app.Application
import com.example.data.database.CityDatabase
import com.example.data.pack.ContentPackImporter
import com.example.data.repository.CityRepository
import com.example.worker.ExpiryCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.InputStreamReader

class CityApplication : Application() {

    lateinit var database: CityDatabase
        private set

    lateinit var repository: CityRepository
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Manual DI Container
        database = CityDatabase.getInstance(this)
        repository = CityRepository(this, database)

        // Seed initial content on first launch
        applicationScope.launch {
            try {
                val currentVersion = database.contentMetaDao().getMeta("contentVersion")
                if (currentVersion == null) {
                    val seedStream = assets.open("seed/content_seed.json")
                    val seedJson = InputStreamReader(seedStream, Charsets.UTF_8).use { it.readText() }
                    ContentPackImporter.importPack(
                        database = database,
                        jsonString = seedJson,
                        skipSignatureCheckForSeed = true
                    )
                }
            } catch (_: Exception) {
            }

            // Schedule daily document expiration checks
            try {
                ExpiryCheckWorker.scheduleDaily(this@CityApplication)
            } catch (_: Exception) {
            }
        }
    }
}
