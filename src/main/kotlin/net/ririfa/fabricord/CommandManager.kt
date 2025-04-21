package net.ririfa.fabricord

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.ririfa.fabricord.translation.FabricordMessageKey
import net.ririfa.fabricord.translation.adapt
import net.ririfa.fabricord.util.argument
import net.ririfa.fabricord.util.literal
import net.ririfa.fabricord.util.registerCommand
import java.util.*

//TODO
object CommandManager {
    private val localChatToggled = mutableListOf<UUID>()

    val allGroupCommands = listOf(
        LCCommand,
        GroupCommands.GrpHelpCommand,
        GroupCommands.GrpCreateCommand,
        GroupCommands.GrpDeleteCommand
    )

    fun registerAll(dispatcher: CommandDispatcher<ServerCommandSource>) {
        allGroupCommands.forEach { it.register(dispatcher) }
    }

    object GroupCommands {
        object GrpHelpCommand : C {
            override fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
                dispatcher.registerCommand("grp") {
                    then(
                        literal("help")
                            .executes { context ->
                                //showHelpToPlayer(context.source, page = 1)
                                1
                            }
                    )
                }
            }
        }

        object GrpCreateCommand : C {
            override fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
                dispatcher.registerCommand("grp") {
                    then(
                        literal("create")
                            .then(
                                argument("name", StringArgumentType.string())
                                    .then(
                                        argument("open", BoolArgumentType.bool())
                                            .executes { context ->
                                                val name = StringArgumentType.getString(context, "name")
                                                val open = BoolArgumentType.getBool(context, "open")
                                                val source = context.source

                                                // プレイヤー指定なし
                                                // 実際のグループ作成処理


                                                1
                                            }
                                            .then(
                                                argument("players", EntityArgumentType.players())
                                                    .executes { context ->
                                                        val name = StringArgumentType.getString(context, "name")
                                                        val open = BoolArgumentType.getBool(context, "open")
                                                        val players = EntityArgumentType.getPlayers(context, "players")
                                                        val source = context.source

                                                        // プレイヤー指定ありバージョン


                                                        1
                                                    }
                                            )
                                    )
                            )
                    )
                }
            }
        }

        object GrpDeleteCommand : C {
            override fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
                dispatcher.registerCommand("grp") {
                    then(
                        literal("delete")
                            .then(
                                argument("target", StringArgumentType.string())
                                    .executes { context ->
                                        val target = StringArgumentType.getString(context, "target")
                                        val source = context.source

                                        // グループ削除処理


                                        1
                                    }
                            )
                    )
                }
            }
        }
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