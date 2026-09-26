package net.ririfa.fabricord.command

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.arguments.MessageArgument
import net.minecraft.network.chat.Component
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
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            literal("lc")
                .executes { context ->
                    val player = context.source.playerOrException
                    val enabled = if (LocalChat.players.remove(player.uuid)) {
                        false
                    } else {
                        LocalChat.players.add(player.uuid)
                        true
                    }
                    val provider = player.adapt()
                    val stateKey = if (enabled) FMsgKey.Command.LC.State.ON else FMsgKey.Command.LC.State.OFF
                    player.sendSystemMessage(
                        provider.getMessage(
                            FMsgKey.Command.LC.SwitchedLocalChatState,
                            mapOf("state" to provider.getMessage(stateKey)),
                        )
                    )
                    1
                }
                .then(
                    argument("message", MessageArgument.message())
                        .executes { context ->
                            val player = context.source.playerOrException
                            val message = MessageArgument.getMessage(context, "message")
                            Fabricord.server.playerList.broadcastSystemMessage(
                                Component.literal("<${player.name.string}> ").append(message),
                                false
                            )
                            1
                        }
                )
        )

        dispatcher.register(
            literal("link").executes { context ->
                val player = context.source.playerOrException
                val code = Fabricord.linkCodes.issue(player.uuid)
                player.sendSystemMessage(
                    player.adapt().getMessage(FMsgKey.Command.LINK.IssuedCode, mapOf("code" to code))
                )
                1
            }
        )

        dispatcher.register(
            literal("fabricord")
                .requires { source ->
                    source.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS)
                }
                .then(
                    literal("reload").executes { context ->
                        val success = Fabricord.configManager.reload()
                        if (success) {
                            DiscordBridge.restart()
                            context.source.sendSuccess(Component.literal("[Fabricord] Configuration reloaded."), false)
                            1
                        } else {
                            context.source.sendFailure(Component.literal("[Fabricord] Configuration reload failed."))
                            0
                        }
                    }
                )
        )
    }
}
