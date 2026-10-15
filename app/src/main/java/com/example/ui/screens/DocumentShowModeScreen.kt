package com.example.ui.screens

import android.app.Activity
import android.graphics.BitmapFactory
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.watermark.WatermarkHelper
import com.example.ui.viewmodel.DocumentHubViewModel

@Composable
fun DocumentShowModeScreen(
    documentId: String,
    viewModel: DocumentHubViewModel,
    onExitShowMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val document by viewModel.activeDocument.collectAsState()
    val decryptedBytes by viewModel.activeDecryptedBytes.collectAsState()
    val pdfPageCount by viewModel.activePdfPageCount.collectAsState()

    var showWithWatermark by remember { mutableStateOf(false) }
    var holdSecondsRemaining by remember { mutableIntStateOf(2) }

    // Max brightness and keep screen on
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        val originalBrightness = window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window?.attributes = window?.attributes?.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        }
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            window?.attributes = window?.attributes?.apply {
                screenBrightness = originalBrightness
            }
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(documentId) {
        viewModel.selectDocument(documentId)
    }

    val doc = document

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("document_show_mode_screen")
    ) {
        // Document content (Full Screen, High Contrast)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (decryptedBytes != null) {
                val rawBitmap = remember(decryptedBytes) {
                    try {
                        BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes!!.size)
                    } catch (_: Exception) {
                        null
                    }
                }

                val displayBitmap = remember(rawBitmap, showWithWatermark) {
                    if (rawBitmap != null && showWithWatermark) {
                        WatermarkHelper.applyWatermarkToBitmap(rawBitmap, "КОПИЯ ДЛЯ ПРЕДЪЯВЛЕНИЯ")
                    } else {
                        rawBitmap
                    }
                }

                if (displayBitmap != null) {
                    Image(
                        bitmap = displayBitmap.asImageBitmap(),
                        contentDescription = "Оригинал документа",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                } else if (pdfPageCount > 0) {
                    val pageBmp = remember(showWithWatermark) {
                        val bmp = viewModel.renderPdfPage(0)
                        if (bmp != null && showWithWatermark) {
                            WatermarkHelper.applyWatermarkToBitmap(bmp, "КОПИЯ ДЛЯ ПРЕДЪЯВЛЕНИЯ")
                        } else {
                            bmp
                        }
                    }
                    if (pageBmp != null) {
                        Image(
                            bitmap = pageBmp.asImageBitmap(),
                            contentDescription = "PDF страница",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = "Загрузка документа...",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Floating Bottom Control Bar
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xDD1E1E1E)),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = doc?.title ?: "Режим показа",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (showWithWatermark) "С защитной меткой" else "Оригинал документа",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showWithWatermark = !showWithWatermark }
                    ) {
                        Text(if (showWithWatermark) "Оригинал" else "С меткой", color = Color.White)
                    }

                    // Long-press exit button (2 seconds required)
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.error, MaterialTheme.shapes.small)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        onExitShowMode()
                                    }
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("exit_show_mode_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Удерживайте 2с для выхода", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
