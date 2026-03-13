package net.ririfa.fabricord.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import net.minecraft.command.permission.Permission
import net.minecraft.command.permission.PermissionLevel
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.ririfa.fabricord.config.ConfigManager
import net.ririfa.fabricord.discord.DiscordBotManager
import net.ririfa.fabricord.i18n.FMsgKey
import net.ririfa.fabricord.i18n.adapt
import net.ririfa.fabricord.util.argument
import net.ririfa.fabricord.util.literal
import net.ririfa.fabricord.util.registerCommand
import net.ririfa.fabricord.util.toggle

enum class Commands(
    private val registerAction: (CommandDispatcher<ServerCommandSource>) -> Unit
) : C {

    LC({ dispatcher ->
        dispatcher.registerCommand("lc") {
            executes { ctx ->
                val player = ctx.source.player ?: return@executes 0

                val newState = CommandManager.localChatToggled.toggle(player.uuid)
                val stateKey = if (newState) FMsgKey.Command.LC.State.ON else FMsgKey.Command.LC.State.OFF

                val stateMsg = mapOf("state" to player.adapt().getMessage(stateKey))

                player.sendMessage(
                    player.adapt().getMessage(FMsgKey.Command.LC.SwitchedLocalChatState, stateMsg),
                    false
                )
                1
            }

            then(
                argument("message", greedyString())
                    .executes { ctx ->
                        val player = ctx.source.player ?: return@executes 0
                        val message = ctx.getArgument("message", String::class.java)

                        val text = Text.of("<${player.name.string}> $message")
                        player.server.playerManager.playerList.forEach {
                            it.sendMessage(text, false)
                        }
                        1
                    }
            )
        }
    }),

    LINK({ dispatcher ->
        dispatcher.registerCommand("link") {
            executes {
                val player = it.source.player ?: return@executes 0
                val code = LinkCommandAuthCodeManager.issueLinkCode(player.uuid)
                val ac = mapOf("code" to code)
                player.sendMessage(
                    player.adapt().getMessage(
                        FMsgKey.Command.LINK.IssuedCode,
                        ac
                    ),
                    false
                )
                1
            }
        }
    }),

    FABRICORD({ dispatcher ->
        dispatcher.registerCommand("fabricord") {
            requires { it.permissions.hasPermission(Permission.Level(PermissionLevel.fromLevel(2))) }
            then(
                literal("reload").executes { ctx ->
                    val source = ctx.source
                    val success = ConfigManager.reload()
                    if (success) {
                        DiscordBotManager.restart()
                        source.sendFeedback({ Text.literal("[Fabricord] Config reloaded.") }, false)
                    } else {
                        source.sendError(Text.literal("[Fabricord] Failed to reload config. Check logs for details."))
                    }
                    if (success) 1 else 0
                }
            )
        }
    });

    override fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        registerAction(dispatcher)
    }
}