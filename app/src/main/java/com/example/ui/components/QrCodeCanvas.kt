package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkQrBackground
import com.example.ui.theme.DarkQrModule

@Composable
fun QrCodeCanvas(
    matrix: Array<BooleanArray>?,
    modifier: Modifier = Modifier
) {
    if (matrix == null || matrix.isEmpty()) return

    val numRows = matrix.size
    val numCols = matrix[0].size

    Box(
        modifier = modifier
            .fillMaxWidth(0.7f)
            .aspectRatio(1f)
            .background(DarkQrBackground, MaterialTheme.shapes.medium)
            .padding(16.dp)
            .testTag("qr_code_canvas"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            val cellWidth = size.width / numCols
            val cellHeight = size.height / numRows

            for (y in 0 until numRows) {
                for (x in 0 until numCols) {
                    if (matrix[y][x]) {
                        drawRect(
                            color = DarkQrModule,
                            topLeft = Offset(x * cellWidth, y * cellHeight),
                            size = Size(cellWidth + 0.5f, cellHeight + 0.5f)
                        )
                    }
                }
            }
        }
    }
}
