package com.intent.reader.notes

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Coordinates use a fixed logical canvas, independent of screen density. */
@Serializable
data class InkPoint(val x: Float, val y: Float)

@Serializable
data class InkStroke(val points: List<InkPoint>)

@Serializable
data class Handwriting(val version: Int = 1, val width: Float, val height: Float, val strokes: List<InkStroke>)

/** Reject malformed or unsupported data instead of treating it as an empty drawing. */
object HandwritingFormat {
    const val MAX_BYTES = 4 * 1024 * 1024
    private const val MAX_STROKES = 10_000
    private const val MAX_POINTS = 200_000
    private val json = Json { ignoreUnknownKeys = false; encodeDefaults = true }

    fun encode(drawing: Handwriting): ByteArray {
        validate(drawing)
        return json.encodeToString(drawing).toByteArray(Charsets.UTF_8).also { require(it.size <= MAX_BYTES) }
    }

    fun decode(bytes: ByteArray): Handwriting {
        require(bytes.size <= MAX_BYTES)
        return json.decodeFromString<Handwriting>(bytes.toString(Charsets.UTF_8)).also(::validate)
    }

    private fun validate(drawing: Handwriting) {
        require(drawing.version == 1)
        require(drawing.width.isFinite() && drawing.width in 1f..100_000f)
        require(drawing.height.isFinite() && drawing.height in 1f..100_000f)
        require(drawing.strokes.size <= MAX_STROKES)
        var total = 0L
        for (stroke in drawing.strokes) {
            require(stroke.points.isNotEmpty())
            total += stroke.points.size
            require(total <= MAX_POINTS)
            for (point in stroke.points) {
                require(point.x.isFinite() && point.x in 0f..drawing.width)
                require(point.y.isFinite() && point.y in 0f..drawing.height)
            }
        }
    }
}
