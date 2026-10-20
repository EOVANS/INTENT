package com.intent.reader.domain

import java.security.MessageDigest

/**
 * Deterministic pseudo-random card style assignment (doc 5.6):
 * - image-less articles -> D
 * - same-source same-day aggregation is handled upstream -> E
 * - the rest hash (guid + date) into [A, B, C] with balance fix-ups:
 *   no 3 identical in a row; ratio roughly A:B:C = 2:1:2 (B is visually heavy).
 * Same day + same guid always yields the same style, so the feed never jumps.
 */
object CardStyleSelector {

    private val weightedPool = listOf(
        CardStyle.A, CardStyle.A,
        CardStyle.B,
        CardStyle.C, CardStyle.C,
    )

    fun hashOf(guid: String, dateKey: String): Int {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest((guid + "|" + dateKey).toByteArray(Charsets.UTF_8))
        return ((bytes[0].toInt() and 0xFF) or
            (bytes[1].toInt() and 0xFF shl 8) or
            (bytes[2].toInt() and 0xFF shl 16) or
            (bytes[3].toInt() and 0xFF shl 24))
    }

    /**
     * @param hasImage per-article image availability
     * @param disabled user-disabled styles removed from the candidate pool
     */
    fun assignStyles(
        guids: List<String>,
        dateKey: String,
        hasImage: List<Boolean>,
        disabled: Set<String> = emptySet(),
    ): List<CardStyle> {
        val pool = if (disabled.isEmpty()) weightedPool
        else weightedPool.filter { it.name !in disabled }.ifEmpty { listOf(CardStyle.C) }

        val result = mutableListOf<CardStyle>()
        for (i in guids.indices) {
            if (!hasImage[i]) {
                result.add(CardStyle.D)
                continue
            }
            val h = hashOf(guids[i], dateKey)
            var style = pool[((h % pool.size) + pool.size) % pool.size]
            // Balance: avoid three identical styles in a row
            if (result.size >= 2 &&
                result[result.size - 1] == style &&
                result[result.size - 2] == style
            ) {
                val alt = pool[(((h ushr 8) % pool.size) + pool.size) % pool.size]
                style = if (alt != style) alt else pool.firstOrNull { it != style } ?: style
            }
            result.add(style)
        }
        return result
    }
}
