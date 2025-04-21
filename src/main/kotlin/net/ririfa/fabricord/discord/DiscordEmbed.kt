package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.ririfa.fabricord.*
import net.ririfa.fabricord.translation.FabricordMessageKey
import java.awt.Color
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

object DiscordEmbed {
    private val logQueue = ConcurrentLinkedQueue<Pair<String, MessageEmbed>>()

    fun init() {
        FT(period = 500, unit = TimeUnit.MILLISECONDS, newThread = true) {
            val (channelId, embed) = logQueue.poll() ?: return@FT

            val channel = JDA?.getTextChannelById(channelId)
            if (channel != null) {
                try {
                    channel.sendMessageEmbeds(embed).queue()
                } catch (e: Exception) {
                    Logger.error("Failed to send embed: ${e.message}")
                }
            } else {
                Logger.warn("Discord channel not found: $channelId")
            }
        }
    }

    private fun sendEmbedToDiscord(color: Color, author: String? = null, imageUrl: String, channelId: String? = Config.logChannelID) {
        if (channelId == null || channelId.isBlank()) {
            Logger.error(LM.getMessage(FabricordMessageKey.Discord.Config.LogChannelIDIsBlank))
            return
        }

        val embed = EmbedBuilder().apply {
            setColor(color)
            setAuthor(author, null, imageUrl)
        }.build()

        logQueue.add(channelId to embed)
    }

    private fun sendEmbedToDiscordImmediately(color: Color, author: String? = null, imageUrl: String, channelId: String? = Config.logChannelID) {
        if (channelId == null) return
        if (channelId.isBlank()) {
            Logger.error(LM.getMessage(FabricordMessageKey.Discord.Config.LogChannelIDIsBlank))
            return
        }

        val embed = EmbedBuilder().apply {
            setColor(color)
            setAuthor(author, null, imageUrl)
        }.build()

        JDA?.getTextChannelById(channelId)?.sendMessageEmbeds(embed)?.queue()
    }

    @JvmStatic
    fun sendPlayerJoinEmbed(player: ServerPlayerEntity) {
        val name = player.name.string
        val uuid = player.uuid.toString()
        val imageUrl = "https://visage.surgeplay.com/face/256/$uuid"
        val message = Config.playerJoinMessage!!.replace("%player%", name)
        sendEmbedToDiscordImmediately(Color.GREEN, message, imageUrl)
    }

    @JvmStatic
    fun sendPlayerLeftEmbed(player: ServerPlayerEntity) {
        val name = player.name.string
        val uuid = player.uuid.toString()
        val imageUrl = "https://visage.surgeplay.com/face/256/$uuid"
        val message = Config.playerLeaveMessage!!.replace("%player%", name)
        sendEmbedToDiscordImmediately(Color.RED, message, imageUrl)
    }

    @JvmStatic
    fun sendPlayerDeathEmbed(player: ServerPlayerEntity, deathMessage: Text) {
        val uuid = player.uuid.toString()
        val imageUrl = "https://visage.surgeplay.com/face/256/$uuid"
        sendEmbedToDiscord(Color.BLACK, deathMessage.string, imageUrl)
    }

    @JvmStatic
    fun sendPlayerGrantCriterionEmbed(player: ServerPlayerEntity, criterion: String) {
        val name = player.name.string
        val uuid = player.uuid.toString()
        val imageUrl = "https://visage.surgeplay.com/face/256/$uuid"
        sendEmbedToDiscord(Color.YELLOW, "$name has made the advancement $criterion", imageUrl)
    }
}