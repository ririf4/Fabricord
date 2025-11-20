package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.minecraft.server.network.ServerPlayerEntity
import net.ririfa.fabricord.Config
import net.ririfa.fabricord.FT
import net.ririfa.fabricord.LM
import net.ririfa.fabricord.Server
import net.ririfa.fabricord.discord.DiscordBotManager.jda
import net.ririfa.fabricord.i18n.FabricordMessageKey
import java.awt.Color
import java.util.*
import java.util.concurrent.TimeUnit

class CompositeDiscordListener : ListenerAdapter() {
    private val mcidPattern = Regex("@([a-zA-Z0-9_]+)")
    private val uuidPattern = Regex("@\\{([0-9a-fA-F-]+)}")

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val logChannel = Config.logChannelID?.let { jda?.getTextChannelById(it) }
        val consoleChannel = Config.consoleLogChannelID?.let { jda?.getTextChannelById(it) }

        when (event.channel) {
            logChannel -> {
                FT {
                    val (mentionedPlayers, foundUUID) = findMentionedPlayers(
                        event.message.contentRaw,
                        Server.playerManager.playerList
                    )
                    if (mentionedPlayers.isNotEmpty()) {
                        DiscordMessageHandler.handleMentionedDiscordMessage(event, mentionedPlayers, foundUUID)
                    } else {
                        DiscordMessageHandler.handleDiscordMessage(event)
                    }
                }
            }

            consoleChannel -> {
                if (!event.author.isBot) {
                    val command = event.message.contentRaw
                    Server.execute {
                        Server.commandManager.parseAndExecute(Server.commandSource, command)
                    }
                }
            }
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        when (event.name) {
            "playerlist" -> handlePlayerList(event)
            "status" -> handleStatus(event)
        }
    }

    private fun handlePlayerList(event: SlashCommandInteractionEvent) {
        FT {
            val discordUserLang = event.userLocale.locale
            val onlinePlayers = Server.playerManager.playerList
            val playerCount = onlinePlayers.size
            val ac = mapOf("playerCount" to playerCount.toString())

            val embedBuilder = EmbedBuilder()
                .setTitle(LM.getMessage(FabricordMessageKey.Discord.Embed.PlayerList.Title, lang = discordUserLang).string)
                .setColor(Color.GREEN)
                .setDescription(
                    LM.getMessage(
                        FabricordMessageKey.Discord.Embed.PlayerList.Description, argsComplete = ac, lang = discordUserLang
                    ).string
                )

            if (playerCount > 0) {
                val playerList = onlinePlayers.joinToString(separator = "\n") { player -> player.name.string }
                embedBuilder.setDescription(embedBuilder.descriptionBuilder.append(playerList).toString())
            } else {
                embedBuilder.setDescription(
                    LM.getMessage(FabricordMessageKey.Discord.Embed.PlayerList.ThereAreNoPlayersOnline, lang = discordUserLang).string
                )
            }

            event.replyEmbeds(embedBuilder.build()).queue({ message ->
                FT(delay = 10000, unit = TimeUnit.MILLISECONDS, argument = message) { msg ->
                    msg?.deleteOriginal()?.queue({}, {})
                }
            }, {})
        }
    }

    private fun handleStatus(event: SlashCommandInteractionEvent) {
        FT {
            val discordUserLang = event.userLocale.locale

            val tps = getTPS()
            val mspt = getMSPT()
            val memoryUsage = getMemoryUsage()
            val playerInfo = getPlayerInfo()

            val memUsage = LM.getMessage(FabricordMessageKey.Discord.Embed.ServerStatus.Description.MemoryUsage, lang = discordUserLang)

            val embedBuilder = EmbedBuilder()
                .setTitle(LM.getMessage(FabricordMessageKey.Discord.Embed.ServerStatus.Title, lang = discordUserLang).string)
                .setColor(Color.BLUE)
                .setDescription(
                    "**TPS:** `${"%.2f".format(tps)}`\n" +
                            "**MSPT:** `${"%.2f".format(mspt)}` ms\n" +
                            "**Players:** `${playerInfo}`\n" +
                            "**$memUsage:** `${memoryUsage}`"
                )

            event.replyEmbeds(embedBuilder.build()).queue({ message ->
                FT(delay = 10000, unit = TimeUnit.MILLISECONDS, argument = message) { msg ->
                    msg?.deleteOriginal()?.queue({}, {})
                }
            }, {})
        }
    }

    private fun getTPS(): Double {
        val tickTimes = Server.tickTimes
        val avgTickTime = Arrays.stream(tickTimes).average().orElse(0.0) / 1_000_000.0
        return 20.0.coerceAtMost(1000.0 / avgTickTime)
    }

    private fun getMSPT(): Double {
        return Server.averageTickTime.toDouble()
    }

    private fun getMemoryUsage(): String {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory() / (1024 * 1024)
        val freeMemory = runtime.freeMemory() / (1024 * 1024)
        val maxMemory = runtime.maxMemory() / (1024 * 1024)
        val usedMemory = totalMemory - freeMemory

        return "$usedMemory MB / $totalMemory MB (Max: $maxMemory MB)"
    }

    private fun getPlayerInfo(): String {
        val playerCount = Server.playerManager.playerList.size
        val maxPlayers = Server.playerManager.maxPlayerCount
        return "$playerCount / $maxPlayers"
    }

    private fun findMentionedPlayers(messageContent: String, players: List<ServerPlayerEntity>): Pair<List<ServerPlayerEntity>, Boolean> {
        var foundUUID = false

        val mentionedPlayers = (mcidPattern.findAll(messageContent.lowercase()).mapNotNull { match ->
            players.find { it.name.string.lowercase() == match.groupValues[1] }
        } + uuidPattern.findAll(messageContent).mapNotNull { match ->
            players.find { it.uuid.toString() == match.groupValues[1] }?.also { foundUUID = true }
        }).toSet().toList()

        return mentionedPlayers to foundUUID
    }
}