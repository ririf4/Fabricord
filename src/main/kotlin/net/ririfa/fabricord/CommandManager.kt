@file:Suppress("DuplicatedCode")

package net.ririfa.fabricord

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.ririfa.fabricord.translation.FabricordMessageKey
import net.ririfa.fabricord.translation.adapt
import net.ririfa.fabricord.util.argument
import net.ririfa.fabricord.util.registerCommand
import java.util.*

object CommandManager {
    @JvmField
    val localChatToggled = mutableSetOf<UUID>()

    val allGroupCommands = listOf(
        LCCommand
    )

    fun registerAll(dispatcher: CommandDispatcher<ServerCommandSource>) {
        allGroupCommands.forEach { it.register(dispatcher) }
    }

    object LCCommand : C {
        override fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
            dispatcher.registerCommand("lc") {
                executes { context ->
                    val player = context.source.player ?: return@executes 0
                    val uuid = player.uuid
                    val newState = uuid !in localChatToggled

                    if (newState) {
                        localChatToggled.add(uuid)
                    } else {
                        localChatToggled.remove(uuid)
                    }

                    val stateMSG = if (newState) "ON" else "OFF"
                    player.sendMessage(
                        player.adapt().getMessage(FabricordMessageKey.Command.LC.SwitchedLocalChatState, stateMSG),
                        false
                    )

                    1
                }
                then(
                    argument("message", greedyString())
                        .executes { context ->
                            val player = context.source.player ?: return@executes 0
                            val message = context.getArgument("message", String::class.java)

                            player.server.playerManager.playerList.forEach {
                                it.sendMessage(Text.of("<${player.name.string}> $message"), false)
                            }

                            1
                        }
                )
            }
        }
    }

}

interface C {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>)
}