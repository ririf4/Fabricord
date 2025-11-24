package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.minecraft.server.network.ServerPlayerEntity
import net.ririfa.fabricord.i18n.FMsgKey
import net.ririfa.fabricord.util.*

object DiscordPlayerEventHandler {
    fun handleMCMessage(player: ServerPlayerEntity, message: String) {
        FT {
            when (Config.messageStyle) {
                MessageStyle.MODERN -> modernStyle(player, message)
                MessageStyle.CLASSIC -> classicStyle(player, message)
                else -> classicStyle(player, message)
            }
        }
    }

    private fun classicStyle(player: ServerPlayerEntity, message: String) {
        val mcId = player.name.string
        val formattedMessage = "$mcId » $message"
        DiscordBotManager.sendToDiscord(formattedMessage)
    }

    private fun modernStyle(player: ServerPlayerEntity, message: String) {
        try {
            val data = MessageCreateBuilder()
                .setContent(message)

            if (Config.blockAllMentions) data.setAllowedMentions(emptySet())

            DiscordBotManager.webHook?.sendMessage(data.build())
                ?.setUsername(player.name.string)
                ?.setAvatarUrl("https://visage.surgeplay.com/face/256/${player.uuid}")
                ?.queue()

        } catch (e: Exception) {
            Logger.error(LM.getMessage(FMsgKey.Discord.Bot.ErrorDuringSendingModernMessage).string, e)
        }
    }
}