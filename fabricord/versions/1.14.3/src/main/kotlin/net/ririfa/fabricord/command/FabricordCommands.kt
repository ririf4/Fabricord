package net.ririfa.fabricord.command

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.command.arguments.MessageArgumentType
import net.minecraft.network.MessageType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.LiteralText
import net.ririfa.fabricord.Fabricord
import net.ririfa.fabricord.discord.DiscordBridge
import net.ririfa.fabricord.i18n.FMsgKey
import net.ririfa.fabricord.i18n.adapt
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object LocalChat {
    val players: MutableSet<UUID> = ConcurrentHashMap.newKeySet()
}

object FabricordCommands {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            literal("lc")
                .executes { context ->
                    val player = context.source.player
                    val enabled = if (LocalChat.players.remove(player.uuid)) {
                        false
                    } else {
                        LocalChat.players.add(player.uuid)
                        true
                    }
                    val provider = player.adapt()
                    val stateKey = if (enabled) FMsgKey.Command.LC.State.ON else FMsgKey.Command.LC.State.OFF
                    player.sendChatMessage(
                        provider.getMessage(
                            FMsgKey.Command.LC.SwitchedLocalChatState,
                            mapOf("state" to provider.getMessage(stateKey)),
                        ), MessageType.SYSTEM
                    )
                    1
                }
                .then(
                    argument("message", MessageArgumentType.message())
                        .executes { context ->
                            val player = context.source.player
                            val message = MessageArgumentType.getMessage(context, "message")
                            Fabricord.server.playerManager.broadcastChatMessage(
                                LiteralText("<${player.name.asString()}> ").append(message),
                                false,
                            )
                            1
                        }
                )
        )

        dispatcher.register(
            literal("link").executes { context ->
                val player = context.source.player
                val code = Fabricord.linkCodes.issue(player.uuid)
                player.sendChatMessage(
                    player.adapt().getMessage(FMsgKey.Command.LINK.IssuedCode, mapOf("code" to code)),
                    MessageType.SYSTEM
                )
                1
            }
        )

        dispatcher.register(
            literal("fabricord")
                .requires { source ->
                    source.hasPermissionLevel(2)
                }
                .then(
                    literal("reload").executes { context ->
                        val success = Fabricord.configManager.reload()
                        if (success) {
                            DiscordBridge.restart()
                            context.source.sendFeedback(LiteralText("[Fabricord] Configuration reloaded."), false)
                            1
                        } else {
                            context.source.sendError(LiteralText("[Fabricord] Configuration reload failed."))
                            0
                        }
                    }
                )
        )
    }
}
