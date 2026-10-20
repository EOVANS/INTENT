package com.intent.reader.domain

/** Card styles A-E per design doc section 5. */
enum class CardStyle {
    /** Big image + white text area below */
    A,
    /** Full-bleed image + gradient scrim title */
    B,
    /** Horizontal card: text left, thumbnail right */
    C,
    /** Text-only horizontal card (no image) */
    D,
    /** Same-source daily aggregate */
    E;

    companion object {
        fun from(raw: String?): CardStyle? = entries.firstOrNull { it.name == raw }
    }
}
