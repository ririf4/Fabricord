package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.EmbedBuilder
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.network.chat.Component
import net.ririfa.fabricord.Fabricord
import net.ririfa.fabricord.config.LogChannelType
import net.ririfa.fabricord.util.MessageTemplate
import java.awt.Color

object DiscordEmbeds {
    @JvmStatic
    fun sendJoin(player: ServerPlayerEntity) {
        val message = Fabricord.config.playerJoinMessage
            ?.let { MessageTemplate.render(it, playerValues(player)) }
            ?: return
        sendPlayerEmbed(player, message, Color.GREEN, LogChannelType.Join)
    }

    @JvmStatic
    fun sendLeave(player: ServerPlayerEntity) {
        val message = Fabricord.config.playerLeaveMessage
            ?.let { MessageTemplate.render(it, playerValues(player)) }
            ?: return
        sendPlayerEmbed(player, message, Color.RED, LogChannelType.Leave)
    }

    @JvmStatic
    fun sendDeath(player: ServerPlayerEntity, message: Component) {
        val rendered = MessageTemplate.render(
            Fabricord.config.playerDeathMessage ?: "{message}",
            playerValues(player) + ("message" to message.string),
        )
        sendPlayerEmbed(player, rendered, Color(0x2B2D31), LogChannelType.Death)
    }

    @JvmStatic
    fun sendAdvancement(player: ServerPlayerEntity, title: String) {
        val message = MessageTemplate.render(
            Fabricord.config.playerAdvancementMessage ?: "{player} has made the advancement {advancement}",
            playerValues(player) + ("advancement" to title),
        )
        sendPlayerEmbed(
            player,
            message,
            Color(0xFEE75C),
            LogChannelType.Advancement,
        )
    }

    private fun playerValues(player: ServerPlayerEntity): Map<String, Any> =
        MessageTemplate.serverValues(DiscordBridge.platform.snapshot()) + mapOf(
            "player" to player.name.string,
            "uuid" to player.uuid,
        )

    private fun sendPlayerEmbed(
        player: ServerPlayerEntity,
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
