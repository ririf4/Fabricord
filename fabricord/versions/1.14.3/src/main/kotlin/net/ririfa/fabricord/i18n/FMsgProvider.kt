package net.ririfa.fabricord.i18n

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.ririfa.langman.def.MessageProviderDefault

class FMsgProvider(
    val player: ServerPlayerEntity
) : MessageProviderDefault<FMsgProvider, Text>(Text::class.java, FMsgKey::class.java) {
    override fun getLanguage(): String {
        return PlayerLanguageCache.get(player).substringBefore("_")
    }
}

fun ServerPlayerEntity.adapt(): FMsgProvider {
    return FMsgProvider(this)
}
