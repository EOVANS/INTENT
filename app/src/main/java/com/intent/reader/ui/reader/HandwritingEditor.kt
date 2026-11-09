package com.intent.reader.ui.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.intent.reader.R
import com.intent.reader.notes.Handwriting
import com.intent.reader.notes.InkPoint
import com.intent.reader.notes.InkStroke
import com.intent.reader.ui.theme.IntentTheme

@Composable
fun HandwritingPreview(drawing: Handwriting, modifier: Modifier = Modifier) {
    Canvas(modifier.background(Color.White)) {
        val scale = minOf(size.width / drawing.width, size.height / drawing.height)
        drawing.strokes.forEach { stroke ->
            val points = stroke.points
            if (points.size == 1) drawCircle(Color.Black, radius = 2f * scale, center = Offset(points[0].x * scale, points[0].y * scale))
            for (i in 1 until points.size) drawLine(Color.Black, Offset(points[i-1].x * scale, points[i-1].y * scale), Offset(points[i].x * scale, points[i].y * scale), strokeWidth = 3f * scale, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun HandwritingEditor(initial: Handwriting?, onCancel: () -> Unit, onDone: (Handwriting?) -> Unit, onDraft: (Handwriting?) -> Unit) {
    val width = initial?.width ?: 1000f
    var height by remember { mutableFloatStateOf(initial?.height ?: 1600f) }
    var strokes by remember { mutableStateOf(initial?.strokes.orEmpty()) }
    var undo by remember { mutableStateOf(emptyList<List<InkStroke>>()) }
    var redo by remember { mutableStateOf(emptyList<List<InkStroke>>()) }
    var current by remember { mutableStateOf(emptyList<InkPoint>()) }
    var fingerDraw by remember { mutableStateOf(false) }
    var eraser by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val c = IntentTheme.colors
    fun change(next: List<InkStroke>) {
        if (next != strokes) { undo = (undo + listOf(strokes)).takeLast(100); strokes = next; redo = emptyList() }
    }
    LaunchedEffect(strokes, height) { onDraft(if (strokes.isEmpty()) null else Handwriting(width = width, height = height, strokes = strokes)) }
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.close)) }
                TextButton(onClick = { onDone(if (strokes.isEmpty()) null else Handwriting(width = width, height = height, strokes = strokes)) }) { Text(stringResource(R.string.notes_save)) }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
                TextButton(enabled = undo.isNotEmpty(), onClick = { redo = redo + listOf(strokes); strokes = undo.last(); undo = undo.dropLast(1) }) { Text(stringResource(R.string.ink_undo)) }
                TextButton(enabled = redo.isNotEmpty(), onClick = { undo = undo + listOf(strokes); strokes = redo.last(); redo = redo.dropLast(1) }) { Text(stringResource(R.string.ink_redo)) }
                TextButton(onClick = { eraser = !eraser }) { Text(stringResource(if (eraser) R.string.ink_pen else R.string.ink_eraser)) }
                TextButton(enabled = height < 10000f, onClick = { height = (height + 800f).coerceAtMost(10000f) }) { Text(stringResource(R.string.ink_extend)) }
                TextButton(onClick = { fingerDraw = !fingerDraw }) { Text(stringResource(if (fingerDraw) R.string.ink_finger_on else R.string.ink_finger_off)) }
                TextButton(enabled = strokes.isNotEmpty(), onClick = { confirmClear = true }) { Text(stringResource(R.string.ink_clear)) }
            }
            Text(stringResource(R.string.ink_hint), color = c.textSecondary, modifier = Modifier.padding(horizontal = 16.dp))
            Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp)) {
                Canvas(Modifier.fillMaxWidth().aspectRatio(width / height).background(Color.White)
                    .pointerInput(fingerDraw, width, height, eraser) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (down.type == PointerType.Stylus || down.type == PointerType.Eraser || (fingerDraw && down.type == PointerType.Touch)) {
                                val sx = width / size.width.toFloat()
                                val sy = height / size.height.toFloat()
                                fun point(p: Offset) = InkPoint((p.x * sx).coerceIn(0f, width), (p.y * sy).coerceIn(0f, height))
                                val points = mutableListOf(point(down.position))
                                val eraseGesture = eraser || down.type == PointerType.Eraser
                                current = points.toList()
                                down.consume()
                                var cancelled = false
                                do {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change == null) { cancelled = true; break }
                                    if (change.pressed) { points.add(point(change.position)); current = points.toList(); change.consume() }
                                } while (change.pressed)
                                if (!cancelled) {
                                    if (eraseGesture) {
                                        val radius = 24f
                                        val removed = strokes.filter { stroke -> stroke.points.any { p -> points.any { q -> (p.x-q.x)*(p.x-q.x)+(p.y-q.y)*(p.y-q.y) <= radius*radius } } }
                                        if (removed.isNotEmpty()) change(strokes.filterNot { it in removed })
                                    } else { change(strokes + InkStroke(points.toList())) }
                                }
                                current = emptyList()
                            }
                        }
                    }) {
                    val scaleX = size.width / width
                    val scaleY = size.height / height
                    (strokes.map { it.points } + listOf(current)).forEach { points ->
                        if (points.size == 1) drawCircle(Color.Black, radius = 2f * scaleX, center = Offset(points[0].x * scaleX, points[0].y * scaleY))
                        for (i in 1 until points.size) drawLine(Color.Black, Offset(points[i-1].x * scaleX, points[i-1].y * scaleY), Offset(points[i].x * scaleX, points[i].y * scaleY), strokeWidth = 3f * scaleX, cap = StrokeCap.Round)
                    }
                }
            }
        }
    }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false }, title = { Text(stringResource(R.string.ink_clear_confirm)) }, confirmButton = {
        TextButton(onClick = { change(emptyList()); confirmClear = false }) { Text(stringResource(R.string.ink_clear)) }
    }, dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.close)) } })
}
