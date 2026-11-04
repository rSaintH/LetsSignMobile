package com.letssign.mobile.ui.signer

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Tela de captura de assinatura manuscrita (desenho livre com o dedo),
 * usada pelo Signatário para assinar cada campo posicionado no documento.
 */
@Composable
fun SignaturePadDialog(
    fieldLabel: String,
    cacheDir: File,
    onDismiss: () -> Unit,
    onConfirm: (imagePath: String) -> Unit
) {
    val strokes = remember { mutableStateOf(listOf<List<Offset>>()) }
    val currentStroke = remember { mutableStateOf(listOf<Offset>()) }
    var canvasSizePx by remember { mutableStateOf(IntSize.Zero) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assinar: $fieldLabel") },
        text = {
            Column {
                Text("Desenhe sua assinatura no campo abaixo.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .border(1.dp, Color.Gray)
                        .background(Color.White)
                        .onSizeChanged { canvasSizePx = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset -> currentStroke.value = listOf(offset) },
                                onDragEnd = {
                                    strokes.value = strokes.value + listOf(currentStroke.value)
                                    currentStroke.value = emptyList()
                                },
                                onDrag = { change, _ ->
                                    currentStroke.value = currentStroke.value + change.position
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                        val allStrokes = strokes.value + listOf(currentStroke.value)
                        allStrokes.forEach { points ->
                            if (points.size > 1) {
                                val path = Path()
                                path.moveTo(points.first().x, points.first().y)
                                points.drop(1).forEach { path.lineTo(it.x, it.y) }
                                drawPath(path, color = Color(0xFF1C1B3A), style = Stroke(width = 6f))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = {
                        strokes.value = emptyList()
                        currentStroke.value = emptyList()
                    }) { Text("Limpar") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val allStrokes = strokes.value
                    if (allStrokes.isNotEmpty() && canvasSizePx.width > 0) {
                        val path = buildSignatureBitmap(allStrokes, canvasSizePx.width, canvasSizePx.height)
                        val file = File(cacheDir, "sig_${UUID.randomUUID()}.png")
                        FileOutputStream(file).use { out -> path.compress(Bitmap.CompressFormat.PNG, 100, out) }
                        onConfirm(file.absolutePath)
                    }
                }
            ) { Text("Confirmar assinatura") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private fun buildSignatureBitmap(strokes: List<List<Offset>>, widthPx: Int, heightPx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(AndroidColor.TRANSPARENT)
    val paint = Paint().apply {
        color = AndroidColor.rgb(28, 27, 58)
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    strokes.forEach { points ->
        if (points.size > 1) {
            val path = AndroidPath()
            path.moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { path.lineTo(it.x, it.y) }
            canvas.drawPath(path, paint)
        }
    }
    return bitmap
}
