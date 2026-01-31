package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.minecraft.server.network.ServerPlayerEntity
import net.ririfa.fabricord.config.LogChannelType
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
        DiscordBotManager.sendToDiscord(formattedMessage, Config.logChannels?.get(LogChannelType.Chat))
    }

    private fun modernStyle(player: ServerPlayerEntity, message: String) {
        try {
            val channelIds = Config.logChannels?.get(LogChannelType.Chat) ?: return

            val data = MessageCreateBuilder()
                .setContent(message)

            when {
                Config.useUserPermissionForMentions -> {
                    // Check user permissions (async)
                    val discordId = DataBase.getDiscordId(player.uuid) ?: return

                    channelIds.forEach { channelId ->
                        val guild = DiscordBotManager.jda?.getTextChannelById(channelId)?.guild ?: return@forEach

                        guild.retrieveMemberById(discordId).queue({ member ->
                            val allowed = resolveAllowedMentions(member)
                            data.setAllowedMentions(allowed)
                            sendWebhookMessage(player, data.build(), channelId)
                        }, { error ->
                            Logger.error("Failed to retrieve Discord member for channel $channelId", error)
                        })
                    }
                }

                Config.blockAllMentions -> {
                    // Block all mentions
                    data.setAllowedMentions(emptySet())
                    channelIds.forEach { channelId ->
                        sendWebhookMessage(player, data.build(), channelId)
                    }
                }

                else -> {
                    // Use default Discord behavior (no mention restrictions)
                    channelIds.forEach { channelId ->
                        sendWebhookMessage(player, data.build(), channelId)
                    }
                }
            }

        } catch (e: Exception) {
            Logger.error(LM.getMessage(FMsgKey.Discord.Bot.ErrorDuringSendingModernMessage).string, e)
        }
    }

    private fun sendWebhookMessage(player: ServerPlayerEntity, message: MessageCreateData, channelId: String) {
        DiscordBotManager.webHooks[channelId]?.sendMessage(message)
            ?.setUsername(player.name.string)
            ?.setAvatarUrl("https://visage.surgeplay.com/face/256/${player.uuid}")
            ?.queue()
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