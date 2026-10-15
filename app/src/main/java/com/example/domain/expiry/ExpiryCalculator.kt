package com.example.domain.expiry

import java.time.LocalDate

enum class ExpiryUrgency {
    NO_EXPIRY,
    VALID,
    ATTENTION_90,
    WARNING_60,
    CRITICAL_30,
    EXPIRED_TODAY,
    EXPIRED
}

data class ExpiryStatus(
    val daysLeft: Long?,
    val urgency: ExpiryUrgency,
    val displayLabel: String,
    val requiresNotification: Boolean,
    val thresholdCrossed: Int? // 90, 60, 30, 0 or null
)

object ExpiryCalculator {

    fun calculate(expiresAtEpochDay: Long?, today: LocalDate = LocalDate.now()): ExpiryStatus {
        if (expiresAtEpochDay == null) {
            return ExpiryStatus(
                daysLeft = null,
                urgency = ExpiryUrgency.NO_EXPIRY,
                displayLabel = "Бессрочно",
                requiresNotification = false,
                thresholdCrossed = null
            )
        }

        val todayEpochDay = today.toEpochDay()
        val daysLeft = expiresAtEpochDay - todayEpochDay

        return when {
            daysLeft < 0 -> ExpiryStatus(
                daysLeft = daysLeft,
                urgency = ExpiryUrgency.EXPIRED,
                displayLabel = "Истёк (${-daysLeft} дн. назад)",
                requiresNotification = false,
                thresholdCrossed = 0
            )
            daysLeft == 0L -> ExpiryStatus(
                daysLeft = 0,
                urgency = ExpiryUrgency.EXPIRED_TODAY,
                displayLabel = "Истекает сегодня",
                requiresNotification = true,
                thresholdCrossed = 0
            )
            daysLeft <= 30 -> ExpiryStatus(
                daysLeft = daysLeft,
                urgency = ExpiryUrgency.CRITICAL_30,
                displayLabel = "Истекает через $daysLeft дн.",
                requiresNotification = true,
                thresholdCrossed = 30
            )
            daysLeft <= 60 -> ExpiryStatus(
                daysLeft = daysLeft,
                urgency = ExpiryUrgency.WARNING_60,
                displayLabel = "Осталось $daysLeft дн.",
                requiresNotification = true,
                thresholdCrossed = 60
            )
            daysLeft <= 90 -> ExpiryStatus(
                daysLeft = daysLeft,
                urgency = ExpiryUrgency.ATTENTION_90,
                displayLabel = "Действителен, $daysLeft дн.",
                requiresNotification = true,
                thresholdCrossed = 90
            )
            else -> ExpiryStatus(
                daysLeft = daysLeft,
                urgency = ExpiryUrgency.VALID,
                displayLabel = "Действителен, $daysLeft дн.",
                requiresNotification = false,
                thresholdCrossed = null
            )
        }
    }
}
