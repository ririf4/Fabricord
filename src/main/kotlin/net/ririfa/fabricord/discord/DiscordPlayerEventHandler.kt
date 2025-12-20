package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.minecraft.server.network.ServerPlayerEntity
import net.ririfa.fabricord.database.DataBase
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

            val discordId = DataBase.getDiscordId(player.uuid) ?: return // ID should not-null value but just in case
            val guild = Config.logChannelID?.let { DiscordBotManager.jda?.getTextChannelById(it) }?.guild ?: return
            guild.retrieveMemberById(discordId).queue({ member ->
                val allowed = if (Config.useUserPermissionForMentions) {
                    resolveAllowedMentions(member)
                } else if (Config.blockAllMentions) {
                    emptySet()
                } else null

                if (allowed != null) data.setAllowedMentions(allowed)

                DiscordBotManager.webHook?.sendMessage(data.build())
                    ?.setUsername(player.name.string)
                    ?.setAvatarUrl("https://visage.surgeplay.com/face/256/${player.uuid}")
                    ?.queue()
            }, { error ->
                Logger.error("Failed to retrieve Discord member", error)
            })
        } catch (e: Exception) {
            Logger.error(LM.getMessage(FMsgKey.Discord.Bot.ErrorDuringSendingModernMessage).string, e)
        }
    }

    private fun resolveAllowedMentions(member: Member): Set<Message.MentionType> {
        val allowed = mutableSetOf<Message.MentionType>()

        if (member.hasPermission(Permission.MESSAGE_MENTION_EVERYONE))
            allowed += Message.MentionType.EVERYONE

        if (member.roles.isNotEmpty())
            allowed += Message.MentionType.ROLE

        allowed += Message.MentionType.USER

        return allowed
    }

}