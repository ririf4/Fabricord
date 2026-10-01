package net.ririfa.fabricord.util

import net.ririfa.fabricord.discord.ServerSnapshot
import java.util.Locale

object MessageTemplate {
    private val placeholder = Regex("\\{([A-Za-z0-9_.-]+)}|%([A-Za-z0-9_.-]+)%")

    fun render(template: String, values: Map<String, Any?>): String {
        val normalized = values.mapKeys { (key, _) -> key.lowercase(Locale.ROOT) }
        return placeholder.replace(template) { match ->
            val key = (match.groups[1]?.value ?: match.groups[2]?.value)
                ?.lowercase(Locale.ROOT)
                ?: return@replace match.value
            normalized[key]?.toString() ?: match.value
        }
    }

    fun serverValues(snapshot: ServerSnapshot): Map<String, Any> = mapOf(
        "count" to snapshot.playerCount,
        "max" to snapshot.maxPlayers,
        "tps" to String.format(Locale.ROOT, "%.2f", snapshot.tps),
        "mspt" to String.format(Locale.ROOT, "%.2f", snapshot.mspt),
        "uptime" to formatDuration(snapshot.uptimeMillis),
        "version" to snapshot.version,
        "world_time" to snapshot.worldTime,
        "loaded_chunks" to snapshot.loadedChunks,
    )

    fun formatDuration(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val days = totalSeconds / 86_400
        val hours = totalSeconds % 86_400 / 3_600
        val minutes = totalSeconds % 3_600 / 60
        return if (days > 0) "${days}d ${hours}h ${minutes}m" else "${hours}h ${minutes}m"
    }
}
