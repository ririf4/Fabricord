package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.minecraft.server.network.ServerPlayerEntity
import net.ririfa.fabricord.Config
import net.ririfa.fabricord.FT
import net.ririfa.fabricord.LM
import net.ririfa.fabricord.Logger
import net.ririfa.fabricord.translation.FabricordMessageKey

object DiscordPlayerEventHandler {
    fun handleMCMessage(player: ServerPlayerEntity, message: String) {
        Logger.debug("handleMCMessage called! player=${player.name.string}, message=$message")
        FT {
            when (Config.messageStyle) {
                "modern" -> modernStyle(player, message)
                "classic" -> classicStyle(player, message)
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

            if (Config.allowMentions == false) {
                data.setAllowedMentions(emptySet())
            }

            DiscordBotManager.webHook?.sendMessage(data.build())
                ?.setUsername(player.name.string)
                ?.setAvatarUrl("https://visage.surgeplay.com/face/256/${player.uuid}")
                ?.queue()

        } catch (e: Exception) {
            Logger.error(LM.getMessage(FabricordMessageKey.System.Discord.ErrorDuringSendingModernMessage), e)
        }
    }
}