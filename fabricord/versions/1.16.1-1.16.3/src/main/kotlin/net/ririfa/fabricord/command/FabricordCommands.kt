package net.ririfa.fabricord.command

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.arguments.MessageArgument
import net.minecraft.network.chat.TextComponent
import net.ririfa.fabricord.Fabricord
import net.ririfa.fabricord.discord.DiscordBridge
import net.ririfa.fabricord.i18n.FMsgKey
import net.ririfa.fabricord.i18n.adapt
import java.util.*
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
                    player.sendMessage(
                        provider.getMessage(
                            FMsgKey.Command.LC.SwitchedLocalChatState,
                            mapOf("state" to provider.getMessage(stateKey)),
                        ), player.uuid
                    )
                    1
                }
                .then(
                    argument("message", MessageArgument.message())
                        .executes { context ->
                            val player = context.source.playerOrException
                            val message = MessageArgument.getMessage(context, "message")
                            Fabricord.server.playerList.broadcastMessage(
                                TextComponent("<${player.name.string}> ").append(message),
                                net.minecraft.network.chat.ChatType.SYSTEM,
                                player.uuid
                            )
                            1
                        }
                )
        )

        dispatcher.register(
            literal("link").executes { context ->
                val player = context.source.playerOrException
                val code = Fabricord.linkCodes.issue(player.uuid)
                player.sendMessage(
                    player.adapt().getMessage(FMsgKey.Command.LINK.IssuedCode, mapOf("code" to code)),
                    player.uuid
                )
                1
            }
        )

        dispatcher.register(
            literal("unlink").executes { context ->
                val player = context.source.playerOrException
                val key = if (DiscordBridge.unlinkMinecraftAccount(player.uuid)) {
                    FMsgKey.Command.UNLINK.Unlinked
                } else {
                    FMsgKey.Command.UNLINK.NotLinked
                }
                player.sendMessage(player.adapt().getMessage(key), player.uuid)
                1
            }
        )

        dispatcher.register(
            literal("fabricord")
                .requires { source ->
                    source.hasPermission(2)
                }
                .then(
                    literal("reload").executes { context ->
                        val success = Fabricord.configManager.reload()
                        if (success) {
                            DiscordBridge.restart()
                            context.source.sendSuccess(TextComponent("[Fabricord] Configuration reloaded."), false)
                            1
                        } else {
                            context.source.sendFailure(TextComponent("[Fabricord] Configuration reload failed."))
                            0
                        }
                    }
                )
        )
    }
}
