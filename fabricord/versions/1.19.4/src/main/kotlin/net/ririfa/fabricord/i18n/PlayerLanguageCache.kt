package net.ririfa.fabricord.i18n

import net.minecraft.server.level.ServerPlayer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object PlayerLanguageCache {
    private val languages = ConcurrentHashMap<UUID, String>()

    @JvmStatic
    fun set(player: ServerPlayer, language: String) {
        languages[player.uuid] = language
    }

    fun get(player: ServerPlayer): String {
        return languages[player.uuid] ?: "en"
    }
}
