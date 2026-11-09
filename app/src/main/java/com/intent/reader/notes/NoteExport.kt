package com.intent.reader.notes

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

/** A share archive is a readable snapshot, not an importable backup. */
object NoteExport {
    fun writeZip(file: File, items: List<NoteExportItem>) {
        require(items.isNotEmpty())
        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("notes.md"))
            val body = buildString {
                items.groupBy { it.note.articleId }.values.forEachIndexed { index, group ->
                    if (index > 0) append("\n\n---\n\n")
                    append(NoteMarkdown.encodeArticle(group.map { it.note }))
                    group.filter { it.handwriting != null }.forEach { item ->
                        append("\n> ").append(item.note.quote.replace('\n', ' ')).append("\n")
                        append("\n![手写笔记](handwriting/").append(item.note.id).append(".png)\n")
                    }
                }
            }
            zip.write(body.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            items.forEach { item ->
                val drawing = item.handwriting ?: return@forEach
                val name = item.note.id
                zip.putNextEntry(ZipEntry("handwriting/$name.json"))
                zip.write(HandwritingFormat.encode(drawing))
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("handwriting/$name.png"))
                render(drawing, zip)
                zip.closeEntry()
            }
        }
    }

    private fun render(drawing: Handwriting, output: java.io.OutputStream) {
        val scale = min(1f, 2048f / maxOf(drawing.width, drawing.height))
        val bitmap = Bitmap.createBitmap((drawing.width * scale).roundToInt().coerceAtLeast(1), (drawing.height * scale).roundToInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            canvas.drawColor(drawing.backgroundColor.toInt())
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; strokeWidth = 2.5f * scale; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; style = Paint.Style.STROKE }
            drawing.strokes.forEach { stroke ->
                paint.color = stroke.color.toInt()
                val points = stroke.points
                if (points.size == 1) canvas.drawPoint(points[0].x * scale, points[0].y * scale, paint)
                else for (i in 1 until points.size) canvas.drawLine(points[i - 1].x * scale, points[i - 1].y * scale, points[i].x * scale, points[i].y * scale, paint)
            }
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        } finally { bitmap.recycle() }
    }
}
