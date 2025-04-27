@file:Suppress("DuplicatedCode")

package net.ririfa.fabricord

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.ririfa.fabricord.annotations.Indexed
import net.ririfa.fabricord.database.tables.*
import net.ririfa.fabricord.translation.FabricordMessageKey
import net.ririfa.fabricord.translation.adapt
import net.ririfa.fabricord.util.*
import org.jetbrains.exposed.sql.and
import java.util.*
import kotlin.reflect.full.findAnnotation

object CommandManager {
    private val localChatToggled = mutableListOf<UUID>()

    val allGroupCommands = listOf(
        LCCommand,
        GroupCommands.GrpHelpCommand,
        GroupCommands.GrpCreateCommand,
        GroupCommands.GrpDeleteCommand,
        GroupCommands.GrpJoinCommand,
        GroupCommands.GrpLeaveCommand
    )

    fun registerAll(dispatcher: CommandDispatcher<ServerCommandSource>) {
        allGroupCommands.forEach { it.register(dispatcher) }
    }

    //TODO
    object GroupCommands {
        object GrpHelpCommand : C {
            override fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
                dispatcher.registerCommand("grp") {
                    then(
                        literal("help")
                            .executes { context ->
                                showHelpToPlayer(context.source, page = 1)
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

                                                val player = source.playerOrThrow
                                                val uuid = player.uuid

                                                val groupData = GroupData(
                                                    uuid = ShortUUID.generate(),
                                                    name = name,
                                                    owner = uuid,
                                                    members = listOf(uuid),
                                                    open = open
                                                )

                                                groupData.insert()
                                                val ac = mapOf("name" to name)
                                                source.sendFeedback({ player.adapt().getMessage(FabricordMessageKey.Command.Group.Create.Success, ac) }, false)
                                                1
                                            }
                                            .then(
                                                argument("players", EntityArgumentType.players())
                                                    .executes { context ->
                                                        val name = StringArgumentType.getString(context, "name")
                                                        val open = BoolArgumentType.getBool(context, "open")
                                                        val players = EntityArgumentType.getPlayers(context, "players")
                                                        val source = context.source

                                                        val player = source.playerOrThrow
                                                        val uuid = player.uuid

                                                        val memberUUIDs = players.map { it.uuid } + uuid

                                                        val groupData = GroupData(
                                                            uuid = ShortUUID.generate(),
                                                            name = name,
                                                            owner = uuid,
                                                            members = memberUUIDs.distinct(),
                                                            open = open
                                                        )

                                                        groupData.insert()
                                                        val ac = mapOf("name" to name, "size" to players.size + 1)
                                                        source.sendFeedback(
                                                            { player.adapt().getMessage(FabricordMessageKey.Command.Group.Create.SuccessWithMembers, ac) },
                                                            false
                                                        )
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
                                        val player = source.playerOrThrow
                                        val playerUUID = player.uuid

                                        val groupResult = DB<GroupSearchResult> {
                                            val byId = runCatching { ShortUUID.fromShortString(target) }
                                                .getOrNull()
                                                ?.let { Group.findById(it.toShortString()) }

                                            if (byId != null && byId.owner == playerUUID) {
                                                return@DB GroupSearchResult.Found(byId)
                                            }

                                            val matchedGroups = Group.find {
                                                (Groups.name eq target) and (Groups.owner eq playerUUID)
                                            }.toList()

                                            when {
                                                matchedGroups.isEmpty() -> GroupSearchResult.NotFound
                                                matchedGroups.size == 1 -> GroupSearchResult.Found(matchedGroups.first())
                                                else -> GroupSearchResult.MultipleFound
                                            }
                                        }

                                        when (groupResult) {
                                            is GroupSearchResult.Found -> {
                                                val group = groupResult.group
                                                group.deleteGroup()
                                                val ac = mapOf("target" to group.name)
                                                source.sendFeedback({ player.adapt().getMessage(FabricordMessageKey.Command.Group.Delete.Success, ac) }, false)
                                                1
                                            }

                                            GroupSearchResult.NotFound -> {
                                                val ac = mapOf("target" to target)
                                                source.sendError(player.adapt().getMessage(FabricordMessageKey.Command.Group.Delete.Failure, ac))
                                                0
                                            }

                                            GroupSearchResult.MultipleFound -> {
                                                source.sendError(player.adapt().getMessage(FabricordMessageKey.Command.Group.Delete.UseIDForDelete))
                                                0
                                            }
                                        }
                                    }
                            )
                    )
                }
            }
        }

        object GrpJoinCommand : C {
            override fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
                dispatcher.registerCommand("grp") {
                    then(
                        literal("join")
                            .then(
                                argument("target", StringArgumentType.string())
                                    .executes { context ->
                                        val target = StringArgumentType.getString(context, "target")
                                        val source = context.source
                                        val player = source.playerOrThrow
                                        val playerUUID = player.uuid

                                        val groupResult = DB<GroupSearchResult> {
                                            val byId = runCatching { ShortUUID.fromShortString(target) }
                                                .getOrNull()
                                                ?.let { Group.findById(it.toShortString()) }

                                            if (byId != null) {
                                                return@DB GroupSearchResult.Found(byId)
                                            }

                                            val matchedGroups = Group.find {
                                                (Groups.name eq target)
                                            }.toList()

                                            when {
                                                matchedGroups.isEmpty() -> GroupSearchResult.NotFound
                                                matchedGroups.size == 1 -> GroupSearchResult.Found(matchedGroups.first())
                                                else -> GroupSearchResult.MultipleFound
                                            }
                                        }

                                        when (groupResult) {
                                            is GroupSearchResult.Found -> {
                                                val group = groupResult.group
                                                if (!group.open) {
                                                    source.sendError(player.adapt().getMessage(FabricordMessageKey.Command.Group.Join.ClosedGroup))
                                                    return@executes 0
                                                }

                                                if (group.members.contains(playerUUID)) {
                                                    source.sendError(player.adapt().getMessage(FabricordMessageKey.Command.Group.Join.AlreadyJoined))
                                                    return@executes 0
                                                }

                                                group.addMember(playerUUID)
                                                val ac = mapOf("target" to group.name)
                                                source.sendFeedback({ player.adapt().getMessage(FabricordMessageKey.Command.Group.Join.Success, ac) }, false)
                                                1
                                            }

                                            GroupSearchResult.NotFound -> {
                                                val ac = mapOf("target" to target)
                                                source.sendError(player.adapt().getMessage(FabricordMessageKey.Command.Group.Join.Failure, ac))
                                                0
                                            }

                                            GroupSearchResult.MultipleFound -> {
                                                source.sendError(player.adapt().getMessage(FabricordMessageKey.Command.Group.Join.UseIDForJoin))
                                                0
                                            }
                                        }
                                    }
                            )
                    )
                }
            }
        }

        object GrpLeaveCommand : C {
            override fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
                dispatcher.registerCommand("grp") {
                    then(
                        literal("leave")
                            .then(
                                argument("target", StringArgumentType.string())
                                    .executes { context ->
                                        val target = StringArgumentType.getString(context, "target")
                                        val source = context.source
                                        val player = source.playerOrThrow
                                        val playerUUID = player.uuid

                                        val groupResult = DB<GroupSearchResult> {
                                            val byId = runCatching { ShortUUID.fromShortString(target) }
                                                .getOrNull()
                                                ?.let { Group.findById(it.toShortString()) }

                                            if (byId != null) {
                                                return@DB GroupSearchResult.Found(byId)
                                            }

                                            val matchedGroups = Group.find {
                                                (Groups.name eq target)
                                            }.toList()

                                            when {
                                                matchedGroups.isEmpty() -> GroupSearchResult.NotFound
                                                matchedGroups.size == 1 -> GroupSearchResult.Found(matchedGroups.first())
                                                else -> GroupSearchResult.MultipleFound
                                            }
                                        }

                                        when (groupResult) {
                                            is GroupSearchResult.Found -> {
                                                val group = groupResult.group

                                                if (!group.members.contains(playerUUID)) {
                                                    source.sendError(player.adapt().getMessage(FabricordMessageKey.Command.Group.Leave.NotMember))
                                                    return@executes 0
                                                }

                                                if (group.owner == playerUUID) {
                                                    source.sendError(player.adapt().getMessage(FabricordMessageKey.Command.Group.Leave.OwnerCannotLeave))
                                                    return@executes 0
                                                }

                                                group.removeMember(playerUUID)
                                                val ac = mapOf("target" to group.name)
                                                source.sendFeedback({ player.adapt().getMessage(FabricordMessageKey.Command.Group.Leave.Success, ac) }, false)
                                                1
                                            }

                                            GroupSearchResult.NotFound -> {
                                                val ac = mapOf("target" to target)
                                                source.sendError(player.adapt().getMessage(FabricordMessageKey.Command.Group.Leave.Failure, ac))
                                                0
                                            }

                                            GroupSearchResult.MultipleFound -> {
                                                source.sendError(player.adapt().getMessage(FabricordMessageKey.Command.Group.Leave.UseIDForLeave))
                                                0
                                            }
                                        }
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

    fun showHelpToPlayer(source: ServerCommandSource, page: Int) {
        val player = source.player ?: run {
            val player = (source.entity as ServerPlayerEntity).adapt()
            source.sendError(player.getMessage(FabricordMessageKey.Command.ThisCommandIsPlayerOnly))
            return
        }

        val commandHelpMap = FabricordMessageKey.Command.Help.Group::class
            .sealedSubclasses
            .sortedBy { it.findAnnotation<Indexed>()?.value }
            .associateWith { subclass ->
                val about = subclass.sealedSubclasses.find { it.simpleName == "About" }?.objectInstance
                val usage = subclass.sealedSubclasses.find { it.simpleName == "Usage" }?.objectInstance
                about to usage
            }

        val commandHelpPages = commandHelpMap.entries.chunked(3)
        val totalPages = commandHelpPages.size

        val pageIndex = (page - 1).coerceIn(0, totalPages - 1)
        val commandPage = commandHelpPages[pageIndex]

        val name = player.adapt().getMessage(FabricordMessageKey.Command.Page).string

        player.sendMessage(Text.of("§a=== $name (${pageIndex + 1}/$totalPages) ==="))

        commandPage.forEach { (command, pair) ->
            val aboutMessage = pair.first?.let { player.adapt().getMessage(it) }
            val usageMessage = pair.second?.let { player.adapt().getMessage(it) }

            player.sendMessage(Text.of("§b/${command.simpleName?.lowercase()}§r - ${aboutMessage?.string}"))
            player.sendMessage(Text.of("  §7Usage: ${usageMessage?.string}"))
            player.sendMessage(Text.of(""))
        }
    }
}

interface C {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>)
}