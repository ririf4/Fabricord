package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.ririfa.fabricord.config.LogChannelType
import net.ririfa.fabricord.util.Config
import net.ririfa.fabricord.util.FT
import net.ririfa.fabricord.util.JDA
import net.ririfa.fabricord.util.Logger
import java.awt.Color
import java.util.concurrent.ConcurrentLinkedQueue

object DiscordEmbed {
    private val logQueue = ConcurrentLinkedQueue<Pair<String, MessageEmbed>>()

    fun init() {
        FT(period = 5000, newThread = true) {
            val (channelId, embed) = logQueue.poll() ?: return@FT

            val jda = JDA ?: return@FT
            val channel = jda.getTextChannelById(channelId) ?: return@FT

            try {
                channel.sendMessageEmbeds(embed).queue()
            } catch (e: Exception) {
                Logger.error("Failed to send embed: ${e.message}")
            }
        }
    }

    private fun sendEmbedToDiscord(color: Color, author: String? = null, imageUrl: String, channelIds: Set<String>?) {
        if (channelIds == null) return

        val embed = EmbedBuilder().apply {
            setColor(color)
            setAuthor(author, null, imageUrl)
        }.build()

        channelIds.forEach { channelId ->
            logQueue.add(channelId to embed)
        }
    }

    private fun sendEmbedToDiscordImmediately(color: Color, author: String? = null, imageUrl: String, channelIds: Set<String>?) {
        if (channelIds == null) return

        val embed = EmbedBuilder().apply {
            setColor(color)
            setAuthor(author, null, imageUrl)
        }.build()

        val jda = JDA ?: return

        channelIds.forEach { channelId ->
            val channel = jda.getTextChannelById(channelId) ?: return@forEach

            channel.sendMessageEmbeds(embed).queue(
                null,
                { e -> Logger.error("Failed to send embed to channel $channelId: ${e.message}") }
            )
        }
    }

    @JvmStatic
    fun sendPlayerJoinEmbed(player: ServerPlayerEntity) {
        Config.playerJoinMessage?.let { rawMessage ->
            val name = player.name.string
            val uuid = player.uuid.toString()
            val imageUrl = "https://visage.surgeplay.com/face/256/$uuid"
            val message = rawMessage.replace("%player%", name)
            val channelIds = Config.logChannels?.get(LogChannelType.Join)
                ?: Config.logChannels?.get(LogChannelType.Default)

            sendEmbedToDiscordImmediately(Color.GREEN, message, imageUrl, channelIds)
        }
    }


    @JvmStatic
    fun sendPlayerLeftEmbed(player: ServerPlayerEntity) {
        Config.playerLeaveMessage?.let { rawMessage ->
            val name = player.name.string
            val uuid = player.uuid.toString()
            val imageUrl = "https://visage.surgeplay.com/face/256/$uuid"
            val message = rawMessage.replace("%player%", name)
            val channelIds = Config.logChannels?.get(LogChannelType.Leave)
                ?: Config.logChannels?.get(LogChannelType.Default)

            sendEmbedToDiscordImmediately(Color.RED, message, imageUrl, channelIds)
        }
    }

    @JvmStatic
    fun sendPlayerDeathEmbed(player: ServerPlayerEntity, deathMessage: Text) {
        val uuid = player.uuid.toString()
        val imageUrl = "https://visage.surgeplay.com/face/256/$uuid"
        val channelIds = Config.logChannels?.get(LogChannelType.Death)
            ?: Config.logChannels?.get(LogChannelType.Default)
        sendEmbedToDiscord(Color.BLACK, deathMessage.string, imageUrl, channelIds)
    }

    @JvmStatic
    fun sendPlayerGrantCriterionEmbed(player: ServerPlayerEntity, criterion: String) {
        val name = player.name.string
        val uuid = player.uuid.toString()
        val imageUrl = "https://visage.surgeplay.com/face/256/$uuid"
        val channelIds = Config.logChannels?.get(LogChannelType.Advancement)
            ?: Config.logChannels?.get(LogChannelType.Default)
        sendEmbedToDiscord(Color.YELLOW, "$name has made the advancement $criterion", imageUrl, channelIds)
    }
}