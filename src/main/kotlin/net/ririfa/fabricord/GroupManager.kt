package net.ririfa.fabricord

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.ririfa.fabricord.annotations.Indexed
import net.ririfa.fabricord.translation.FabricordMessageKey
import net.ririfa.fabricord.translation.adapt
import net.ririfa.fabricord.util.ShortUUID
import net.ririfa.fabricord.util.argument
import net.ririfa.fabricord.util.literal
import net.ririfa.fabricord.util.registerCommand
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.*
import kotlin.reflect.full.findAnnotation

object GroupManager {
	lateinit var db: Database

	val dbFile: String = File(Fabricord.modDir.toFile(), "database").absoluteFile.path
	val dbUrl = "jdbc:h2:$dbFile;AUTO_SERVER=TRUE"
	val groupTouchTimestamps = mutableMapOf<ShortUUID, Long>()

	fun init() {
		db = Database.connect(dbUrl, driver = "org.h2.Driver", user = "fabricord", password = "")

		transaction(db) {
			SchemaUtils.create(
				Tables.Group
			)
		}

		DB {
			Tables.Group
				.selectAll()
				.forEach {
					val group = Group(
						id = ShortUUID.fromShortString(it[Tables.Group.id]),
						name = it[Tables.Group.name],
						members = it[Tables.Group.members].split(",").map(UUID::fromString)
					)
					idGroupMap[group.id] = group
				}
		}

	}

	@JvmField
	val playerInGroupedChat: MutableMap<UUID, ShortUUID> = mutableMapOf()
	@JvmField
	val idGroupMap: MutableMap<ShortUUID, Group> = mutableMapOf()

	val allGroupCommands = listOf(
		Commands.GrpHelpCommand,
		Commands.GrpCreateCommand,
		Commands.GrpDeleteCommand
	)

	fun registerAll(dispatcher: CommandDispatcher<ServerCommandSource>) {
		allGroupCommands.forEach { it.register(dispatcher) }
	}

	@JvmStatic
	fun getGroupById(id: ShortUUID): Group? {
		return idGroupMap[id] ?: DB {
			Tables.Group
				.selectAll()
				.where { Tables.Group.id eq id.toShortString() }
				.singleOrNull()
				?.let {
					val g = Group(
						id = ShortUUID.fromShortString(it[Tables.Group.id]),
						name = it[Tables.Group.name],
						members = it[Tables.Group.members].split(",").map(UUID::fromString)
					)
					idGroupMap[g.id] = g
					g
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

	interface C {
		fun register(dispatcher: CommandDispatcher<ServerCommandSource>)
	}

	object Tables {
		object Group : Table("group") {
			val id = text("id")
			val name = text("name")
			val members = text("members")

			override val primaryKey = PrimaryKey(id)
		}
	}

	object Commands {
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
					then(literal("create")
						.then(argument("name", StringArgumentType.string())
							.then(argument("open", BoolArgumentType.bool())
								.executes { context ->
									val name = StringArgumentType.getString(context, "name")
									val open = BoolArgumentType.getBool(context, "open")
									val source = context.source

									// プレイヤー指定なしバージョン
									// 実際のグループ作成処理ここに書く


									1
								}
								.then(argument("players", EntityArgumentType.players())
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
					then(literal("delete")
						.then(argument("target", StringArgumentType.string())
							.executes { context ->
								val target = StringArgumentType.getString(context, "target")
								val source = context.source

								// グループ削除処理をここに


								1
							}
						)
					)
				}
			}
		}

	}

	data class Group(
		@JvmField
		val id: ShortUUID,
		@JvmField
		val name: String,
		@JvmField
		val members: List<UUID>
	)
}

fun <T> DB(block: () -> T): T {
	return transaction(GroupManager.db) { block() }
}