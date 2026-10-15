package com.example.domain.watermark

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object WatermarkHelper {

    const val DEFAULT_LEGAL_NOTICE =
        "These electronic copies of documents are not intended for banking transactions. They are for employment purposes only."

    /**
     * Builds watermarking text, respecting the 120 characters limit.
     */
    fun buildWatermarkText(
        customText: String? = null,
        recipient: String? = null,
        date: LocalDate = LocalDate.now()
    ): String {
        if (!customText.isNullOrBlank()) {
            return customText.take(120)
        }

        val monthYearFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("ru"))
        val monthYear = date.format(monthYearFormatter)

        val fullText = if (!recipient.isNullOrBlank()) {
            "Только для трудоустройства: $recipient, $monthYear"
        } else {
            DEFAULT_LEGAL_NOTICE
        }

        return fullText.take(120)
    }

    /**
     * Applies diagonal repeating watermark over a bitmap copy.
     */
    fun applyWatermarkToBitmap(original: Bitmap, text: String): Bitmap {
        val result = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            alpha = (255 * 0.38f).toInt() // 38% opacity
            textSize = (result.width / 24f).coerceAtLeast(28f)
            style = Paint.Style.FILL
            isFakeBoldText = true
        }

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val textWidth = bounds.width().toFloat()
        val textHeight = bounds.height().toFloat()

        val stepX = (textWidth * 1.4f).coerceAtLeast(300f)
        val stepY = (textHeight * 3.5f).coerceAtLeast(150f)

        canvas.save()
        // Rotate 35 degrees diagonally
        canvas.rotate(-35f, result.width / 2f, result.height / 2f)

        // Draw grid over expanded diagonal space
        val diagonal = kotlin.math.hypot(result.width.toDouble(), result.height.toDouble()).toFloat()
        var y = -diagonal
        while (y < diagonal * 1.5f) {
            var x = -diagonal
            while (x < diagonal * 1.5f) {
                canvas.drawText(text, x, y, paint)
                x += stepX
            }
            y += stepY
        }

        canvas.restore()
        return result
    }

    /**
     * Saves watermarked bitmap to a file in cache directory.
     */
    fun saveWatermarkedBitmap(bitmap: Bitmap, targetFile: File, format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG) {
        targetFile.parentFile?.mkdirs()
        FileOutputStream(targetFile).use { fos ->
            bitmap.compress(format, 88, fos)
        }
    }
}
