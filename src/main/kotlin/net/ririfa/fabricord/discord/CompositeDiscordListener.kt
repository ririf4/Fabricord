package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.modals.Modal
import net.minecraft.server.network.ServerPlayerEntity
import net.ririfa.fabricord.command.LinkCommandAuthCodeManager
import net.ririfa.fabricord.database.DataBase
import net.ririfa.fabricord.i18n.FMsgKey
import net.ririfa.fabricord.util.Config
import net.ririfa.fabricord.util.FT
import net.ririfa.fabricord.util.LM
import net.ririfa.fabricord.util.Server
import java.awt.Color
import java.util.*

class CompositeDiscordListener : ListenerAdapter() {
    private val mcidPattern = Regex("@([a-zA-Z0-9_]+)")
    private val uuidPattern = Regex("@\\{([0-9a-fA-F-]+)}")

    private val modalID = "FABRICORD_LINK_ACCOUNT_MODAL"

    private val logChannelID = Config.logChannelID
    private val consoleChannel = Config.consoleLogChannelID

    override fun onMessageReceived(event: MessageReceivedEvent) {
        when (event.channel.id) {
            logChannelID -> {
                FT {
                    val (mentionedPlayers, foundUUID) = findMentionedPlayers(event.message.contentRaw)

                    if (mentionedPlayers.isEmpty()) {
                        DiscordMessageHandler.handleDiscordMessage(event)
                    } else {
                        DiscordMessageHandler.handleMentionedDiscordMessage(event, mentionedPlayers, foundUUID)
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
            "link" -> handleLink(event)
        }
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        onModal(event)
    }

    private fun handlePlayerList(event: SlashCommandInteractionEvent) {
        FT {
            val discordUserLang = event.userLocale.locale
            val onlinePlayers = Server.playerManager.playerList
            val playerCount = onlinePlayers.size
            val ac = mapOf("playerCount" to playerCount.toString())

            val embedBuilder = EmbedBuilder()
                .setTitle(LM.getMessage(FMsgKey.Discord.Embed.PlayerList.Title, lang = discordUserLang).string)
                .setColor(Color.GREEN)
                .setDescription(
                    LM.getMessage(
                        FMsgKey.Discord.Embed.PlayerList.Description, argsComplete = ac, lang = discordUserLang
                    ).string
                )

            if (playerCount > 0) {
                val playerList = onlinePlayers.joinToString(separator = "\n") { player -> player.name.string }
                embedBuilder.setDescription(embedBuilder.descriptionBuilder.append(playerList).toString())
            } else {
                embedBuilder.setDescription(
                    LM.getMessage(FMsgKey.Discord.Embed.PlayerList.ThereAreNoPlayersOnline, lang = discordUserLang).string
                )
            }

            event.replyEmbeds(embedBuilder.build()).queue({ message ->
                FT(delay = 10000, arg = message) { msg ->
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

            val memUsage = LM.getMessage(FMsgKey.Discord.Embed.ServerStatus.Description.MemoryUsage, lang = discordUserLang)

            val embedBuilder = EmbedBuilder()
                .setTitle(LM.getMessage(FMsgKey.Discord.Embed.ServerStatus.Title, lang = discordUserLang).string)
                .setColor(Color.BLUE)
                .setDescription(
                    "**TPS:** `${"%.2f".format(tps)}`\n" +
                            "**MSPT:** `${"%.2f".format(mspt)}` ms\n" +
                            "**Players:** `${playerInfo}`\n" +
                            "**$memUsage:** `${memoryUsage}`"
                )

            event.replyEmbeds(embedBuilder.build()).queue({ message ->
                FT(delay = 10000, arg = message) { msg ->
                    msg?.deleteOriginal()?.queue({}, {})
                }
            }, {})
        }
    }

    private fun handleLink(event: SlashCommandInteractionEvent) {
        val codeInput = TextInput.create("code", TextInputStyle.SHORT)
            .setMinLength(6)
            .setMaxLength(6)
            .setRequired(true)
            .build()

        val modal = Modal.create(modalID, LM.getMessage(FMsgKey.Discord.Modal.LINK.Title).string)
            .addComponents(
                Label.of("Link Code", codeInput)
            )
            .build()

        event.replyModal(modal).queue()
    }

    private fun onModal(event: ModalInteractionEvent) {
        if (event.modalId != modalID) return

        val code = event.getValue("code")?.asString
        if (code == null) {
            event.reply(LM.getMessage(FMsgKey.Discord.Modal.LINK.Invalid).string)
                .setEphemeral(true)
                .queue()
            return
        }

        val normalized = code.trim().uppercase()

        val uuid = LinkCommandAuthCodeManager.consume(normalized)
        if (uuid == null) {
            event.reply("This code is invalid or has expired.").setEphemeral(true).queue()
            return
        }

        DataBase.linkUser(uuid, event.user.idLong)
        event.reply(LM.getMessage(FMsgKey.Discord.Modal.LINK.LinkedSuccessfully).string)
            .setEphemeral(true)
            .queue()
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

    private fun findMentionedPlayers(messageContent: String): Pair<List<ServerPlayerEntity>, Boolean> {
        val players = Server.playerManager.playerList

        // index maps
        val nameMap = players.associateBy { it.name.string.lowercase() }
        val uuidMap = players.associateBy { it.uuid.toString() }

        val lower = messageContent.lowercase()
        var foundUUID = false

        val mentioned = mutableSetOf<ServerPlayerEntity>()

        // match by MCID
        for (match in mcidPattern.findAll(lower)) {
            val name = match.groupValues[1]
            nameMap[name]?.let { mentioned += it }
        }

        // match by UUID
        for (match in uuidPattern.findAll(messageContent)) {
            val uuid = match.groupValues[1]
            uuidMap[uuid]?.let {
                mentioned += it
                foundUUID = true
            }
        }

        return mentioned.toList() to foundUUID
    }
}