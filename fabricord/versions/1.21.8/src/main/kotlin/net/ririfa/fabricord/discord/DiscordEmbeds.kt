package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.EmbedBuilder
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.ririfa.fabricord.Fabricord
import net.ririfa.fabricord.config.LogChannelType
import java.awt.Color

object DiscordEmbeds {
    @JvmStatic
    fun sendJoin(player: ServerPlayer) {
        val message = Fabricord.config.playerJoinMessage
            ?.replace("%player%", player.name.string)
            ?: return
        sendPlayerEmbed(player, message, Color.GREEN, LogChannelType.Join)
    }

    @JvmStatic
    fun sendLeave(player: ServerPlayer) {
        val message = Fabricord.config.playerLeaveMessage
            ?.replace("%player%", player.name.string)
            ?: return
        sendPlayerEmbed(player, message, Color.RED, LogChannelType.Leave)
    }

    @JvmStatic
    fun sendDeath(player: ServerPlayer, message: Component) {
        sendPlayerEmbed(player, message.string, Color(0x2B2D31), LogChannelType.Death)
    }

    @JvmStatic
    fun sendAdvancement(player: ServerPlayer, title: String) {
        sendPlayerEmbed(
            player,
            "${player.name.string} has made the advancement $title",
            Color(0xFEE75C),
            LogChannelType.Advancement,
        )
    }

    private fun sendPlayerEmbed(
        player: ServerPlayer,
        message: String,
        color: Color,
        channelType: LogChannelType,
    ) {
        val embed = EmbedBuilder()
            .setColor(color)
            .setAuthor(
                message,
                null,
                "https://visage.surgeplay.com/face/256/${player.uuid}",
            )
            .build()
        DiscordBridge.sendEmbed(embed, DiscordBridge.channels(channelType))
    }
}
