package net.ririfa.fabricord.util

enum class MessageStyle(val id: String) {
    CLASSIC("classic"),
    MODERN("modern");

    companion object {
        fun of(raw: String?) =
            entries.firstOrNull { it.id == raw?.lowercase() } ?: CLASSIC
    }
}
