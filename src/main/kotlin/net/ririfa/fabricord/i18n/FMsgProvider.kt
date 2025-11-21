package net.ririfa.fabricord.i18n

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.ririfa.langman.def.MessageProviderDefault

class FMsgProvider(val player: ServerPlayerEntity) : MessageProviderDefault<FMsgProvider, Text>(Text::class.java, FMsgKey::class.java) {
    override fun getLanguage(): String {
        // https://maven.fabricmc.net/docs/yarn-1.21.4+build.8/net/minecraft/network/packet/c2s/common/SyncedClientOptions.html
        // comp_1951 -> Language (from this doc)
        // en_US -> en
        return player.clientOptions.comp_1951.split("_")[0]
    }
}

fun ServerPlayerEntity.adapt(): FMsgProvider {
    return FMsgProvider(this)
}