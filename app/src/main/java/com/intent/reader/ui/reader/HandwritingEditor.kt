package com.intent.reader.ui.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
    val description = stringResource(R.string.ink_note_preview)
    Canvas(modifier.clip(RoundedCornerShape(16.dp)).background(Color(drawing.backgroundColor.toInt()))
        .semantics { contentDescription = description }) {
        val scale = minOf(size.width / drawing.width, size.height / drawing.height)
        val origin = Offset((size.width - drawing.width * scale) / 2f, (size.height - drawing.height * scale) / 2f)
        drawing.strokes.forEach { stroke ->
            val points = stroke.points
            val color = Color(stroke.color.toInt())
            if (points.size == 1) drawCircle(color, radius = 2f * scale, center = origin + Offset(points[0].x * scale, points[0].y * scale))
            for (i in 1 until points.size) drawLine(color, origin + Offset(points[i-1].x * scale, points[i-1].y * scale), origin + Offset(points[i].x * scale, points[i].y * scale), strokeWidth = 3f * scale, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun HandwritingEditor(initial: Handwriting?, onCancel: () -> Unit, onDone: (Handwriting?) -> Unit, onDraft: (Handwriting?) -> Unit) {
    var width by remember { mutableFloatStateOf(initial?.width ?: 0f) }
    var height by remember { mutableFloatStateOf(initial?.height ?: 0f) }
    var strokes by remember { mutableStateOf(initial?.strokes.orEmpty()) }
    var undo by remember { mutableStateOf(emptyList<List<InkStroke>>()) }
    var redo by remember { mutableStateOf(emptyList<List<InkStroke>>()) }
    var current by remember { mutableStateOf(emptyList<InkPoint>()) }
    var fingerDraw by remember { mutableStateOf(false) }
    var eraser by remember { mutableStateOf(false) }
    var penColor by remember { mutableLongStateOf(0xFF000000L) }
    var backgroundColor by remember { mutableLongStateOf(initial?.backgroundColor ?: 0xFFFFFFFFL) }
    var showPenColors by remember { mutableStateOf(false) }
    var showBackground by remember { mutableStateOf(false) }
    var showHandHelp by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val toolbar = Color(0xFF355F99)
    fun change(next: List<InkStroke>) {
        if (next != strokes) { undo = (undo + listOf(strokes)).takeLast(100); strokes = next; redo = emptyList() }
    }
    fun snapshot() = if (width <= 0f || height <= 0f || (strokes.isEmpty() && backgroundColor == 0xFFFFFFFFL)) null
        else Handwriting(width = width, height = height, strokes = strokes, backgroundColor = backgroundColor)
    LaunchedEffect(strokes, width, height, backgroundColor) { onDraft(snapshot()) }
    val eraserIcon = ImageVector.vectorResource(R.drawable.ic_ink_eraser)
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val view = LocalView.current
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.statusBars())
            // This is the dialog's window; the activity's system bar policy is untouched.
            onDispose { }
        }
        Column(Modifier.fillMaxSize().background(Color(backgroundColor.toInt())).navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().background(toolbar).windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                InkTool(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.close), onClick = onCancel)
                Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                InkTool(Icons.Filled.Edit, stringResource(R.string.ink_pen), selected = !eraser, onClick = { eraser = false; showPenColors = !showPenColors })
                InkTool(eraserIcon, stringResource(R.string.ink_eraser), selected = eraser, onClick = { eraser = true; showPenColors = false })
                InkTool(Icons.Filled.PanTool, stringResource(if (fingerDraw) R.string.ink_finger_on else R.string.ink_finger_off), selected = fingerDraw, onClick = { fingerDraw = !fingerDraw; showPenColors = false; showHandHelp = true })
                InkTool(Icons.AutoMirrored.Filled.Redo, stringResource(R.string.ink_redo), enabled = redo.isNotEmpty(), onClick = { undo = undo + listOf(strokes); strokes = redo.last(); redo = redo.dropLast(1) })
                InkTool(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.ink_undo), enabled = undo.isNotEmpty(), onClick = { redo = redo + listOf(strokes); strokes = undo.last(); undo = undo.dropLast(1) })
                InkTool(Icons.Filled.Delete, stringResource(R.string.ink_clear), enabled = strokes.isNotEmpty(), onClick = { confirmClear = true })
                InkTool(Icons.Filled.FormatColorFill, stringResource(R.string.ink_background), onClick = { showPenColors = false; showBackground = true })
                }
                InkTool(Icons.Filled.Check, stringResource(R.string.notes_save), onClick = { onDone(snapshot()) })
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val density = LocalDensity.current
                val availableWidth = with(density) { maxWidth.toPx() }
                val availableHeight = with(density) { maxHeight.toPx() }
                // A fresh canvas takes the viewport size once and keeps that logical size;
                // both new and existing drawings are then scaled down to stay on screen.
                LaunchedEffect(availableWidth, availableHeight) {
                    if (width <= 0f || height <= 0f) { width = availableWidth; height = availableHeight }
                }
                val logicalWidth = width.takeIf { it > 0f } ?: availableWidth
                val logicalHeight = height.takeIf { it > 0f } ?: availableHeight
                val fit = minOf(1f, availableWidth / logicalWidth, availableHeight / logicalHeight)
                Box(Modifier.size(with(density) { (logicalWidth * fit).toDp() }, with(density) { (logicalHeight * fit).toDp() })
                    .align(Alignment.Center)) {
                Canvas(Modifier.matchParentSize()
                    .pointerInput(fingerDraw, width, height, eraser, penColor) {
                        awaitEachGesture {
                            if (width <= 0f || height <= 0f) return@awaitEachGesture
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (down.type == PointerType.Stylus || down.type == PointerType.Eraser || (fingerDraw && down.type == PointerType.Touch)) {
                                // Overlaying the palette avoids moving the canvas under the pen when it closes.
                                showPenColors = false
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
                if (showPenColors) Surface(modifier = Modifier.align(Alignment.TopCenter).padding(8.dp), color = toolbar,
                    shape = RoundedCornerShape(20.dp), shadowElevation = 4.dp) {
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(4.dp)) {
                        listOf(0xFF000000L to R.string.ink_black, 0xFF1565C0L to R.string.ink_blue, 0xFFD32F2FL to R.string.ink_red,
                            0xFF2E7D32L to R.string.ink_green, 0xFFFFFFFFL to R.string.ink_white).forEach { (color, label) ->
                            val description = stringResource(label)
                            IconButton(onClick = { penColor = color; eraser = false }, modifier = Modifier.size(48.dp).semantics { contentDescription = description }) {
                                Box(Modifier.size(24.dp).background(Color(color.toInt()), CircleShape)
                                    .border(if (penColor == color) 3.dp else 1.dp, Color.White, CircleShape))
                            }
                        }
                    }
                }
            }
        }
        if (showHandHelp) AlertDialog(onDismissRequest = { showHandHelp = false },
            title = { Text(stringResource(if (fingerDraw) R.string.ink_finger_on else R.string.ink_finger_off)) },
            text = { Text(stringResource(if (fingerDraw) R.string.ink_hand_enabled_help else R.string.ink_hand_disabled_help)) },
            confirmButton = { TextButton(onClick = { showHandHelp = false }) { Text(stringResource(R.string.ink_understood)) } })
        if (showBackground) BackgroundColorDialog(backgroundColor, onDismiss = { showBackground = false }, onSelect = { backgroundColor = it; showBackground = false })
        if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false }, title = { Text(stringResource(R.string.ink_clear_confirm)) }, confirmButton = {
            TextButton(onClick = { change(emptyList()); confirmClear = false }) { Text(stringResource(R.string.ink_clear)) }
        }, dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.close)) } })
    }
}

/** Paper and muted tones that keep black ink readable. */
private val InkBackgroundPalette = listOf(
    0xFFFFFFFFL, 0xFFFFF8E1L, 0xFFFFF3E0L, 0xFFFCE4ECL, 0xFFF3E5F5L,
    0xFFE8EAF6L, 0xFFE3F2FDL, 0xFFE0F2F1L, 0xFFE8F5E9L, 0xFFF1F8E9L,
    0xFFFFF9C4L, 0xFFFFE0B2L, 0xFFFFCDD2L, 0xFFE1BEE7L, 0xFFBBDEFBL,
    0xFFB2DFDBL, 0xFFC8E6C9L, 0xFFCFD8DCL, 0xFF9E9E9EL, 0xFF616161L,
    0xFF303134L, 0xFF202124L, 0xFF121212L, 0xFF000000L,
)

@Composable
private fun BackgroundColorDialog(initial: Long, onDismiss: () -> Unit, onSelect: (Long) -> Unit) {
    var selected by remember { mutableLongStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.ink_background)) }, text = {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InkBackgroundPalette.chunked(6).forEach { rowColors ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    rowColors.forEach { preset ->
                        val picked = selected == preset
                        Box(Modifier.size(40.dp).clip(CircleShape).background(Color(preset.toInt()))
                            .border(if (picked) 3.dp else 1.dp,
                                if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable { selected = preset }
                            .semantics { contentDescription = "#%06X".format(preset and 0xFFFFFFL) })
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = { onSelect(selected) }) { Text(stringResource(R.string.notes_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } })
}
