package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.database.CityDatabase
import com.example.data.database.entity.DocumentAlertEntity
import com.example.domain.expiry.ExpiryCalculator
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class ExpiryCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "city7z7_expiry_check_worker"
        const val CHANNEL_ID = "document_expiry_channel"
        const val NOTIFICATION_ID = 7001

        fun scheduleDaily(context: Context) {
            val request = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(24, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        val database = CityDatabase.getInstance(context)
        val today = LocalDate.now()
        val maxEpochDay = today.plusDays(90).toEpochDay()
        val expiringDocs = database.documentDao().getExpiringDocumentsList(maxEpochDay)

        var notificationNeeded = false

        for (doc in expiringDocs) {
            val status = ExpiryCalculator.calculate(doc.expiresAt, today)
            val threshold = status.thresholdCrossed

            if (threshold != null) {
                val existingAlerts = database.documentDao().getAlertsForDocument(doc.id)
                val alreadyNotified = existingAlerts.any { it.threshold == threshold }

                if (!alreadyNotified) {
                    database.documentDao().insertAlert(
                        DocumentAlertEntity(
                            documentId = doc.id,
                            threshold = threshold,
                            notifiedAt = System.currentTimeMillis()
                        )
                    )
                    notificationNeeded = true
                }
            }
        }

        if (notificationNeeded) {
            showNotification()
        }

        return Result.success()
    }

    private fun showNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Сроки документов",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Локальные напоминания о приближении срока действия документов"
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Public version shown on lock screen (zero private leaks)
        val publicNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("CITY7Z7")
            .setContentText("Есть документ, срок которого скоро истекает. Откройте приложение")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Сроки действия документов")
            .setContentText("Один или несколько документов требуют вашего внимания. Проверьте актуальность.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
