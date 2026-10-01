package net.ririfa.fabricord.i18n

import net.minecraft.server.network.ServerPlayerEntity
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object PlayerLanguageCache {
    private val languages = ConcurrentHashMap<UUID, String>()

    @JvmStatic
    fun set(player: ServerPlayerEntity, language: String) {
        languages[player.uuid] = language
    }

    fun get(player: ServerPlayerEntity): String {
        return languages[player.uuid] ?: "en"
    }
}
