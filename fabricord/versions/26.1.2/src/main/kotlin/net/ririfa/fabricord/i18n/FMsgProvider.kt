package net.ririfa.fabricord.i18n

import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.ririfa.langman.def.MessageProviderDefault

class FMsgProvider(
    val player: ServerPlayer
) : MessageProviderDefault<FMsgProvider, Component>(Component::class.java, FMsgKey::class.java) {
    override fun getLanguage(): String {
        return player.clientInformation().language.split("_")[0]
    }
}

fun ServerPlayer.adapt(): FMsgProvider {
    return FMsgProvider(this)
}