package com.intent.reader.ui.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.intent.reader.R
import com.intent.reader.notes.Handwriting
import com.intent.reader.notes.InkPoint
import com.intent.reader.notes.InkStroke

@Composable
private fun InkTool(icon: ImageVector, description: String, enabled: Boolean = true, selected: Boolean = false, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)
        .background(if (selected) Color.White.copy(alpha = 0.22f) else Color.Transparent, RoundedCornerShape(12.dp))) {
        Icon(icon, contentDescription = description, tint = Color.White.copy(alpha = if (enabled) 1f else 0.38f))
    }
}

@Composable
fun HandwritingPreview(drawing: Handwriting, modifier: Modifier = Modifier) {
    Canvas(modifier.background(Color.White)) {
        val scale = minOf(size.width / drawing.width, size.height / drawing.height)
        drawing.strokes.forEach { stroke ->
            val points = stroke.points
            val color = Color(stroke.color.toInt())
            if (points.size == 1) drawCircle(color, radius = 2f * scale, center = Offset(points[0].x * scale, points[0].y * scale))
            for (i in 1 until points.size) drawLine(color, Offset(points[i-1].x * scale, points[i-1].y * scale), Offset(points[i].x * scale, points[i].y * scale), strokeWidth = 3f * scale, cap = StrokeCap.Round)
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
    var penColor by remember { mutableLongStateOf(0xFF000000L) }
    var confirmClear by remember { mutableStateOf(false) }
    val toolbar = Color(0xFF355F99)
    fun change(next: List<InkStroke>) {
        if (next != strokes) { undo = (undo + listOf(strokes)).takeLast(100); strokes = next; redo = emptyList() }
    }
    LaunchedEffect(strokes, height) { onDraft(if (strokes.isEmpty()) null else Handwriting(width = width, height = height, strokes = strokes)) }
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Color.White).navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().background(toolbar).statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                InkTool(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.close), onClick = onCancel)
                Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                InkTool(Icons.Filled.Edit, stringResource(R.string.ink_pen), selected = !eraser, onClick = { eraser = false })
                InkTool(Icons.Filled.Backspace, stringResource(R.string.ink_eraser), selected = eraser, onClick = { eraser = true })
                listOf(0xFF000000L to R.string.ink_black, 0xFF1565C0L to R.string.ink_blue, 0xFFD32F2FL to R.string.ink_red, 0xFF2E7D32L to R.string.ink_green).forEach { (color, label) ->
                    val description = stringResource(label)
                    IconButton(onClick = { penColor = color; eraser = false }, modifier = Modifier.size(40.dp).semantics { contentDescription = description }) {
                        Box(Modifier.size(22.dp).background(Color(color.toInt()), CircleShape).border(if (penColor == color && !eraser) 3.dp else 1.dp, Color.White, CircleShape))
                    }
                }
                InkTool(Icons.Filled.PanTool, stringResource(if (fingerDraw) R.string.ink_finger_on else R.string.ink_finger_off), selected = fingerDraw, onClick = { fingerDraw = !fingerDraw })
                InkTool(Icons.Filled.Add, stringResource(R.string.ink_extend), enabled = height < 10000f, onClick = { height = (height + 800f).coerceAtMost(10000f) })
                InkTool(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.ink_undo), enabled = undo.isNotEmpty(), onClick = { redo = redo + listOf(strokes); strokes = undo.last(); undo = undo.dropLast(1) })
                InkTool(Icons.AutoMirrored.Filled.Redo, stringResource(R.string.ink_redo), enabled = redo.isNotEmpty(), onClick = { undo = undo + listOf(strokes); strokes = redo.last(); redo = redo.dropLast(1) })
                InkTool(Icons.Filled.Delete, stringResource(R.string.ink_clear), enabled = strokes.isNotEmpty(), onClick = { confirmClear = true })
                }
                InkTool(Icons.Filled.Check, stringResource(R.string.notes_save), onClick = { onDone(if (strokes.isEmpty()) null else Handwriting(width = width, height = height, strokes = strokes)) })
            }
            Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                Canvas(Modifier.fillMaxWidth().aspectRatio(width / height).background(Color.White)
                    .pointerInput(fingerDraw, width, height, eraser, penColor) {
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
                                    } else { change(strokes + InkStroke(points.toList(), penColor)) }
                                }
                                current = emptyList()
                            }
                        }
                    }) {
                    val scaleX = size.width / width
                    val scaleY = size.height / height
                    (strokes + listOfNotNull(current.takeIf { it.isNotEmpty() }?.let { InkStroke(it, penColor) })).forEach { stroke ->
                        val points = stroke.points
                        val color = Color(stroke.color.toInt())
                        if (points.size == 1) drawCircle(color, radius = 2f * scaleX, center = Offset(points[0].x * scaleX, points[0].y * scaleY))
                        for (i in 1 until points.size) drawLine(color, Offset(points[i-1].x * scaleX, points[i-1].y * scaleY), Offset(points[i].x * scaleX, points[i].y * scaleY), strokeWidth = 3f * scaleX, cap = StrokeCap.Round)
                    }
                }
            }
        }
    }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false }, title = { Text(stringResource(R.string.ink_clear_confirm)) }, confirmButton = {
        TextButton(onClick = { change(emptyList()); confirmClear = false }) { Text(stringResource(R.string.ink_clear)) }
    }, dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.close)) } })
}
