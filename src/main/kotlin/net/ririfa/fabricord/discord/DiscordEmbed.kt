package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
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

    private fun sendEmbedToDiscord(color: Color, author: String? = null, imageUrl: String, channelId: String? = Config.logChannelID) {
        if (channelId == null) return

        val embed = EmbedBuilder().apply {
            setColor(color)
            setAuthor(author, null, imageUrl)
        }.build()

        logQueue.add(channelId to embed)
    }

    private fun sendEmbedToDiscordImmediately(color: Color, author: String? = null, imageUrl: String, channelId: String? = Config.logChannelID) {
        if (channelId == null) return

        val embed = EmbedBuilder().apply {
            setColor(color)
            setAuthor(author, null, imageUrl)
        }.build()

        val jda = JDA ?: return
        val channel = jda.getTextChannelById(channelId) ?: return

        channel.sendMessageEmbeds(embed).queue(
            null,
            { e -> Logger.error("Failed to send embed: ${e.message}") }
        )
    }

    @JvmStatic
    fun sendPlayerJoinEmbed(player: ServerPlayerEntity) {
        val name = player.name.string
        val uuid = player.uuid.toString()
        val imageUrl = "https://visage.surgeplay.com/face/256/$uuid"
        val message = Config.playerJoinMessage?.replace("%player%", name)
        sendEmbedToDiscordImmediately(Color.GREEN, message, imageUrl)
    }

    @JvmStatic
    fun sendPlayerLeftEmbed(player: ServerPlayerEntity) {
        val name = player.name.string
        val uuid = player.uuid.toString()
        val imageUrl = "https://visage.surgeplay.com/face/256/$uuid"
        val message = Config.playerLeaveMessage?.replace("%player%", name)
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