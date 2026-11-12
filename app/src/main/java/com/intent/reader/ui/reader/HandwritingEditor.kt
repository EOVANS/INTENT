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
import androidx.compose.material.icons.filled.LineWeight
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
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun HandwritingPreview(drawing: Handwriting, modifier: Modifier = Modifier, anchorText: String = "") {
    val description = stringResource(R.string.ink_note_preview)
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(Color(drawing.backgroundColor.toInt()))
        .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        // The highlighted sentence sits under the ink, exactly as it does in the editor.
        if (anchorText.isNotBlank()) Text(
            text = anchorText,
            color = Color(0xFF3A3A3A),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        )
        Canvas(Modifier.matchParentSize()) {
        val points = drawing.strokes.flatMap { it.points }
        if (points.isEmpty()) return@Canvas
        // Fit the ink itself instead of the whole canvas, so a preview never shows the
        // empty margins that surrounded the writing on the original page.
        val minX = points.minOf { it.x }
        val minY = points.minOf { it.y }
        val inkWidth = (points.maxOf { it.x } - minX).coerceAtLeast(1f)
        val inkHeight = (points.maxOf { it.y } - minY).coerceAtLeast(1f)
        val scale = minOf(size.width / inkWidth, size.height / inkHeight) * 0.92f
        val origin = Offset(
            (size.width - inkWidth * scale) / 2f - minX * scale,
            (size.height - inkHeight * scale) / 2f - minY * scale,
        )
        drawing.strokes.forEach { stroke ->
            val strokePoints = stroke.points
            val color = Color(stroke.color.toInt())
            if (strokePoints.size == 1) drawCircle(color, radius = (stroke.width / 2f * scale).coerceAtLeast(1f), center = origin + Offset(strokePoints[0].x * scale, strokePoints[0].y * scale))
            for (i in 1 until strokePoints.size) drawLine(color, origin + Offset(strokePoints[i-1].x * scale, strokePoints[i-1].y * scale), origin + Offset(strokePoints[i].x * scale, strokePoints[i].y * scale), strokeWidth = stroke.width * scale, cap = StrokeCap.Round)
        }
        }
    }
}

@Composable
fun HandwritingEditor(
    initial: Handwriting?,
    anchorText: String = "",
    onCancel: () -> Unit,
    onDone: (Handwriting?) -> Unit,
    onDraft: (Handwriting?) -> Unit,
) {
    var width by remember { mutableFloatStateOf(initial?.width ?: 0f) }
    var height by remember { mutableFloatStateOf(initial?.height ?: 0f) }
    var strokes by remember { mutableStateOf(initial?.strokes.orEmpty()) }
    var undo by remember { mutableStateOf(emptyList<List<InkStroke>>()) }
    var redo by remember { mutableStateOf(emptyList<List<InkStroke>>()) }
    var current by remember { mutableStateOf(emptyList<InkPoint>()) }
    var fingerDraw by remember { mutableStateOf(false) }
    var eraser by remember { mutableStateOf(false) }
    var penColor by remember { mutableLongStateOf(0xFF000000L) }
    var penWidth by remember { mutableFloatStateOf(4f) }
    var backgroundColor by remember { mutableLongStateOf(initial?.backgroundColor ?: 0xFFFFFFFFL) }
    var showPenColors by remember { mutableStateOf(false) }
    var showPenWidth by remember { mutableStateOf(false) }
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
    val localized = LocalContext.current
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val view = LocalView.current
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            // Some large screens still wrap the dialog content by default, which left the
            // canvas as a small island in the middle. Pin the window to the whole display.
            window?.setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            )
            // The status bar is deliberately left untouched: hiding it here changed the
            // window insets of the note dialog underneath, so the dialog jumped and
            // re-centred the moment this editor closed.
            onDispose { }
        }
        Column(Modifier.fillMaxSize().background(Color(backgroundColor.toInt())).statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().background(toolbar).windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                InkTool(Icons.AutoMirrored.Filled.ArrowBack, localized.getString(R.string.close), onClick = onCancel)
                Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                InkTool(Icons.Filled.Edit, localized.getString(R.string.ink_pen), selected = !eraser, onClick = { eraser = false; showPenColors = !showPenColors })
                InkTool(eraserIcon, localized.getString(R.string.ink_eraser), selected = eraser, onClick = { eraser = true; showPenColors = false })
                HorizontalDivider(Modifier.width(1.dp).height(24.dp), color = Color.White.copy(alpha = 0.25f))
                InkTool(Icons.Filled.LineWeight, localized.getString(R.string.ink_width), selected = showPenWidth, onClick = {
                    eraser = false; showPenColors = false; showPenWidth = !showPenWidth
                })
                InkTool(Icons.Filled.PanTool, localized.getString(if (fingerDraw) R.string.ink_finger_on else R.string.ink_finger_off), selected = fingerDraw, onClick = { fingerDraw = !fingerDraw; showPenColors = false; showPenWidth = false; showHandHelp = true })
                InkTool(Icons.AutoMirrored.Filled.Redo, localized.getString(R.string.ink_redo), enabled = redo.isNotEmpty(), onClick = { undo = undo + listOf(strokes); strokes = redo.last(); redo = redo.dropLast(1) })
                InkTool(Icons.AutoMirrored.Filled.Undo, localized.getString(R.string.ink_undo), enabled = undo.isNotEmpty(), onClick = { redo = redo + listOf(strokes); strokes = undo.last(); undo = undo.dropLast(1) })
                InkTool(Icons.Filled.Delete, localized.getString(R.string.ink_clear), enabled = strokes.isNotEmpty(), onClick = { confirmClear = true })
                InkTool(Icons.Filled.FormatColorFill, localized.getString(R.string.ink_background), onClick = { showPenColors = false; showBackground = true })
                }
                InkTool(Icons.Filled.Check, localized.getString(R.string.notes_save), onClick = { onDone(snapshot()) })
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val density = LocalDensity.current
                val availableWidth = with(density) { maxWidth.toPx() }
                val availableHeight = with(density) { maxHeight.toPx() }
                // The canvas always occupies the whole visible area: it cannot grow past
                // the screen and no dead margin surrounds the ink. A drawing made on the
                // old scrollable canvas is re-fitted here the first time it is opened.
                LaunchedEffect(availableWidth, availableHeight) {
                    if (availableWidth <= 0f || availableHeight <= 0f) return@LaunchedEffect
                    if (width <= 0f || height <= 0f) {
                        width = availableWidth
                        height = availableHeight
                    } else if (width != availableWidth || height != availableHeight) {
                        val scale = minOf(availableWidth / width, availableHeight / height)
                        val dx = (availableWidth - width * scale) / 2f
                        val dy = (availableHeight - height * scale) / 2f
                        if (strokes.isNotEmpty()) strokes = strokes.map { stroke ->
                            stroke.copy(
                                points = stroke.points.map { InkPoint(it.x * scale + dx, it.y * scale + dy) },
                                width = (stroke.width * scale).coerceIn(0.5f, 40f),
                            )
                        }
                        width = availableWidth
                        height = availableHeight
                    }
                }
                // The highlighted sentence stays exactly in the middle of the page, under
                // the ink, so the annotation can be written on top of the words themselves.
                if (anchorText.isNotBlank()) Text(
                    text = anchorText,
                    color = Color(0xFF3A3A3A),
                    fontSize = 18.sp,
                    lineHeight = 27.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 12,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 32.dp),
                )
                Canvas(Modifier.fillMaxSize()
                    .pointerInput(fingerDraw, width, height, eraser, penColor, penWidth) {
                        awaitEachGesture {
                            if (width <= 0f || height <= 0f) return@awaitEachGesture
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (down.type == PointerType.Stylus || down.type == PointerType.Eraser || (fingerDraw && down.type == PointerType.Touch)) {
                                // Overlaying the palette avoids moving the canvas under the pen when it closes.
                                showPenColors = false
                                showPenWidth = false
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
                                    } else { change(strokes + InkStroke(points.toList(), penColor, penWidth)) }
                                }
                                current = emptyList()
                            }
                        }
                    }) {
                    if (width <= 0f || height <= 0f) return@Canvas
                    val scaleX = size.width / width
                    val scaleY = size.height / height
                    (strokes + listOfNotNull(current.takeIf { it.isNotEmpty() }?.let { InkStroke(it, penColor, penWidth) })).forEach { stroke ->
                        val points = stroke.points
                        val color = Color(stroke.color.toInt())
                        val strokeWidth = stroke.width * scaleX
                        if (points.size == 1) drawCircle(color, radius = (strokeWidth / 2f).coerceAtLeast(1f), center = Offset(points[0].x * scaleX, points[0].y * scaleY))
                        for (i in 1 until points.size) drawLine(color, Offset(points[i-1].x * scaleX, points[i-1].y * scaleY), Offset(points[i].x * scaleX, points[i].y * scaleY), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                    }
                }
                if (showPenColors) Surface(modifier = Modifier.align(Alignment.TopCenter).padding(8.dp), color = toolbar,
                    shape = RoundedCornerShape(20.dp), shadowElevation = 4.dp) {
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(4.dp)) {
                        listOf(0xFF000000L to R.string.ink_black, 0xFF1565C0L to R.string.ink_blue, 0xFFD32F2FL to R.string.ink_red,
                            0xFF2E7D32L to R.string.ink_green, 0xFFFFFFFFL to R.string.ink_white).forEach { (color, label) ->
                            val description = localized.getString(label)
                            IconButton(onClick = { penColor = color; eraser = false }, modifier = Modifier.size(48.dp).semantics { contentDescription = description }) {
                                Box(Modifier.size(24.dp).background(Color(color.toInt()), CircleShape)
                                    .border(if (penColor == color) 3.dp else 1.dp, Color.White, CircleShape))
                            }
                        }
                    }
                }
                if (showPenWidth) Surface(modifier = Modifier.align(Alignment.TopCenter).padding(8.dp), color = toolbar,
                    shape = RoundedCornerShape(20.dp), shadowElevation = 4.dp) {
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        InkWidthPalette.forEach { option ->
                            val description = localized.getString(option.label)
                            val picked = penWidth == option.width
                            IconButton(onClick = { penWidth = option.width; eraser = false },
                                modifier = Modifier.size(48.dp).semantics { contentDescription = description }
                                    .background(if (picked) Color.White.copy(alpha = 0.22f) else Color.Transparent, RoundedCornerShape(12.dp))) {
                                Box(Modifier.width(28.dp).height(option.width.dp.coerceAtMost(20.dp))
                                    .background(Color.White, RoundedCornerShape(50)))
                            }
                        }
                    }
                }
            }
        }
        if (showHandHelp) AlertDialog(onDismissRequest = { showHandHelp = false },
            title = { Text(localized.getString(if (fingerDraw) R.string.ink_finger_on else R.string.ink_finger_off)) },
            text = { Text(localized.getString(if (fingerDraw) R.string.ink_hand_enabled_help else R.string.ink_hand_disabled_help)) },
            confirmButton = { TextButton(onClick = { showHandHelp = false }) { Text(localized.getString(R.string.ink_understood)) } })
        if (showBackground) BackgroundColorDialog(localized, backgroundColor, onDismiss = { showBackground = false }, onSelect = { backgroundColor = it })
        if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false }, title = { Text(localized.getString(R.string.ink_clear_confirm)) }, confirmButton = {
            TextButton(onClick = { change(emptyList()); confirmClear = false }) { Text(localized.getString(R.string.ink_clear)) }
        }, dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(localized.getString(R.string.close)) } })
    }
}

/** Pen widths in canvas units, shown as thicker bars in the width popup. */
private data class InkWidth(val width: Float, val label: Int)

private val InkWidthPalette = listOf(
    InkWidth(2f, R.string.ink_width_thin),
    InkWidth(4f, R.string.ink_width_medium),
    InkWidth(8f, R.string.ink_width_thick),
    InkWidth(14f, R.string.ink_width_extra),
)

/** Paper and muted tones that keep black ink readable. */
private val InkBackgroundPalette = listOf(
    0xFFFFFFFFL, 0xFFFFF8E1L, 0xFFFFF3E0L, 0xFFFCE4ECL, 0xFFF3E5F5L,
    0xFFE8EAF6L, 0xFFE3F2FDL, 0xFFE0F2F1L, 0xFFE8F5E9L, 0xFFF1F8E9L,
    0xFFFFF9C4L, 0xFFFFE0B2L, 0xFFFFCDD2L, 0xFFE1BEE7L, 0xFFBBDEFBL,
    0xFFB2DFDBL, 0xFFC8E6C9L, 0xFFCFD8DCL, 0xFF9E9E9EL, 0xFF616161L,
    0xFF303134L, 0xFF202124L, 0xFF121212L, 0xFF000000L,
)

@Composable
private fun BackgroundColorDialog(localized: android.content.Context, initial: Long, onDismiss: () -> Unit, onSelect: (Long) -> Unit) {
    var selected by remember { mutableLongStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(localized.getString(R.string.ink_background)) }, text = {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InkBackgroundPalette.chunked(6).forEach { rowColors ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    rowColors.forEach { preset ->
                        val picked = selected == preset
                        Box(Modifier.size(40.dp).clip(CircleShape).background(Color(preset.toInt()))
                            .border(if (picked) 3.dp else 1.dp,
                                if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            // Every tap applies at once, so the canvas updates behind the dialog.
                            .clickable { selected = preset; onSelect(preset) }
                            .semantics { contentDescription = "#%06X".format(preset and 0xFFFFFFL) })
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text(localized.getString(R.string.ink_confirm)) } })
}
