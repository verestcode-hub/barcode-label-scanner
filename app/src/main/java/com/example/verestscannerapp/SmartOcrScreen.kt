package com.example.verestscannerapp

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(angle)
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartOcrScreen(
    bitmap: Bitmap,
    viewModel: ScannerViewModel,
    onDismiss: () -> Unit,
    onBitmapCropped: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var currentBitmap by remember { mutableStateOf(bitmap) }
    var ocrLines by remember { mutableStateOf<List<ScannerViewModel.OcrTextLine>>(emptyList()) }
    var isAnalyzing by remember { mutableStateOf(true) }

    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(currentBitmap) {
        isAnalyzing = true
        dragStart = null
        dragEnd = null
        viewModel.performTextRecognitionWithBlocks(context, currentBitmap) { lines ->
            ocrLines = lines
            isAnalyzing = false
        }
    }

    val imageScale = if (canvasSize != IntSize.Zero && currentBitmap.width > 0 && currentBitmap.height > 0) {
        min(canvasSize.width.toFloat() / currentBitmap.width, canvasSize.height.toFloat() / currentBitmap.height)
    } else 1f

    val imageOffsetX = if (canvasSize != IntSize.Zero) {
        (canvasSize.width - currentBitmap.width * imageScale) / 2f
    } else 0f

    val imageOffsetY = if (canvasSize != IntSize.Zero) {
        (canvasSize.height - currentBitmap.height * imageScale) / 2f
    } else 0f

    val selectionRect = if (dragStart != null && dragEnd != null) {
        Rect(
            left = min(dragStart!!.x, dragEnd!!.x),
            top = min(dragStart!!.y, dragEnd!!.y),
            right = max(dragStart!!.x, dragEnd!!.x),
            bottom = max(dragStart!!.y, dragEnd!!.y)
        )
    } else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Виділіть текст", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Скасувати") }
                },
                actions = {
                    IconButton(onClick = { currentBitmap = rotateBitmap(currentBitmap, 90f) }) {
                        Icon(Icons.Default.Refresh, "Повернути")
                    }
                    Button(
                        onClick = {
                            val selectedLines = ocrLines.filter { line ->
                                val scaledRect = Rect(
                                    left = line.rect.left * imageScale + imageOffsetX,
                                    top = line.rect.top * imageScale + imageOffsetY,
                                    right = line.rect.right * imageScale + imageOffsetX,
                                    bottom = line.rect.bottom * imageScale + imageOffsetY
                                )
                                selectionRect?.overlaps(scaledRect) == true
                            }

                            if (selectedLines.isNotEmpty()) {
                                var minX = selectedLines.minOf { it.rect.left }
                                var minY = selectedLines.minOf { it.rect.top }
                                var maxX = selectedLines.maxOf { it.rect.right }
                                var maxY = selectedLines.maxOf { it.rect.bottom }

                                val boxWidth = maxX - minX
                                val boxHeight = maxY - minY


                                val padX = (boxWidth * 0.1f) + 100f
                                val padY = (boxHeight * 0.15f) + 100f

                                minX = max(0f, minX - padX)
                                minY = max(0f, minY - padY)
                                maxX = min(currentBitmap.width.toFloat(), maxX + padX)
                                maxY = min(currentBitmap.height.toFloat(), maxY + padY)

                                val cropWidth = (maxX - minX).toInt()
                                val cropHeight = (maxY - minY).toInt()

                                if (cropWidth > 0 && cropHeight > 0) {
                                    val cropped = Bitmap.createBitmap(
                                        currentBitmap,
                                        minX.toInt(),
                                        minY.toInt(),
                                        cropWidth,
                                        cropHeight
                                    )
                                    onBitmapCropped(cropped)
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Готово")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(Color.Black)) {
            if (isAnalyzing) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
            } else {
                val imageBitmap = remember(currentBitmap) { currentBitmap.asImageBitmap() }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { canvasSize = it.size }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset -> dragStart = offset; dragEnd = offset },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragEnd = dragEnd!! + dragAmount
                                },
                                onDragEnd = { }
                            )
                        }
                ) {
                    drawImage(
                        image = imageBitmap,
                        dstOffset = IntOffset(imageOffsetX.toInt(), imageOffsetY.toInt()),
                        dstSize = IntSize((imageBitmap.width * imageScale).toInt(), (imageBitmap.height * imageScale).toInt())
                    )

                    ocrLines.forEach { line ->
                        val scaledRect = Rect(
                            left = line.rect.left * imageScale + imageOffsetX,
                            top = line.rect.top * imageScale + imageOffsetY,
                            right = line.rect.right * imageScale + imageOffsetX,
                            bottom = line.rect.bottom * imageScale + imageOffsetY
                        )
                        val isSelected = selectionRect?.overlaps(scaledRect) == true
                        drawRect(
                            color = if (isSelected) Color(0xFF2196F3).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.2f),
                            topLeft = scaledRect.topLeft,
                            size = scaledRect.size
                        )
                    }

                    selectionRect?.let {
                        drawRect(color = Color(0xFF03A9F4).copy(alpha = 0.2f), topLeft = it.topLeft, size = it.size)
                        drawRect(color = Color(0xFF03A9F4), topLeft = it.topLeft, size = it.size, style = Stroke(width = 4f))
                    }
                }
            }
        }
    }
}