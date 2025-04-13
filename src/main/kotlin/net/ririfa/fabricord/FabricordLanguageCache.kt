package net.ririfa.fabricord

import net.minecraft.server.network.ServerPlayerEntity
import java.util.UUID

object FabricordLanguageCache {
    private val playerLangs = mutableMapOf<UUID, String>()

    @JvmStatic
    fun set(player: ServerPlayerEntity, lang: String) {
        playerLangs[player.uuid] = lang
    }

    fun get(player: ServerPlayerEntity): String {
        return playerLangs[player.uuid] ?: "en"
    }
}
