package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.events.guild.member.GuildMemberUpdateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.modals.Modal
import net.minecraft.network.DisconnectionInfo
import net.minecraft.server.BannedPlayerEntry
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.ririfa.fabricord.Fabricord
import net.ririfa.fabricord.command.LinkCommandAuthCodeManager
import net.ririfa.fabricord.config.LogChannelType
import net.ririfa.fabricord.database.DataBase
import net.ririfa.fabricord.i18n.FMsgKey
import net.ririfa.fabricord.util.Config
import net.ririfa.fabricord.util.FT
import net.ririfa.fabricord.util.LM
import net.ririfa.fabricord.util.Server
import java.awt.Color
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

class CompositeDiscordListener : ListenerAdapter() {
    private val mcidPattern = Regex("@([a-zA-Z0-9_]+)")
    private val uuidPattern = Regex("@\\{([0-9a-fA-F-]+)}")

    private val modalID = "FABRICORD_LINK_ACCOUNT_MODAL"

    private val logChannelIDs = Config.logChannels?.get(LogChannelType.Chat)
        ?: Config.logChannels?.get(LogChannelType.Default)
    private val consoleChannel = Config.consoleLogChannelID

    override fun onMessageReceived(event: MessageReceivedEvent) {
        when {
            logChannelIDs != null && event.channel.id in logChannelIDs -> {
                FT {
                    val (mentionedPlayers, foundUUID) = findMentionedPlayers(event.message.contentRaw)

                    if (mentionedPlayers.isEmpty()) {
                        DiscordMessageHandler.handleDiscordMessage(event)
                    } else {
                        DiscordMessageHandler.handleMentionedDiscordMessage(event, mentionedPlayers, foundUUID)
                    }
                }
            }

            consoleChannel != null && event.channel.id == consoleChannel -> {
                if (!event.author.isBot) {
                    val command = event.message.contentRaw
                    Server.execute {
                        Server.commandManager.parseAndExecute(Server.commandSource, command)
                    }
                }
            }
        }
    }

    override fun onGuildMemberUpdate(event: GuildMemberUpdateEvent) {
        if (Config.opSyncRoleIDs?.isNotEmpty() != true) return

        FT {
            OpSync.syncFromRoleChange(event.member.idLong, event.member.roles)
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        when (event.name) {
            "playerlist" -> handlePlayerList(event)
            "status" -> handleStatus(event)
            "link" -> handleLink(event)
            "kick" -> handleKick(event)
            "ban" -> handleBan(event)
            "pardon" -> handlePardon(event)
            "run" -> handleRun(event)
        }
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
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
        OpSync.syncOnLink(uuid)
        event.reply(LM.getMessage(FMsgKey.Discord.Modal.LINK.LinkedSuccessfully).string)
            .setEphemeral(true)
            .queue { hook ->
                hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS)
            }
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
            val lang = event.userLocale.locale

            val tps = getTPS()
            val mspt = getMSPT()

            fun label(key: FMsgKey) = LM.getMessage(key, lang = lang).string

            val embedBuilder = EmbedBuilder()
                .setTitle(label(FMsgKey.Discord.Embed.ServerStatus.Title))
                .setColor(tpsColor(tps))
                .setTimestamp(Instant.now())
                // Row 1
                .addField("TPS", "`${"%.2f".format(tps)}`", true)
                .addField("MSPT", "`${"%.2f".format(mspt)} ms`", true)
                .addField("Players", "`${getPlayerInfo()}`", true)
                // Row 2
                .addField(label(FMsgKey.Discord.Embed.ServerStatus.Description.MemoryUsage), "`${getMemoryUsage()}`", true)
                .addField(label(FMsgKey.Discord.Embed.ServerStatus.Description.Uptime), "`${getUptime()}`", true)
                .addField(label(FMsgKey.Discord.Embed.ServerStatus.Description.Version), "`${Server.version}`", true)
                // Row 3
                .addField(label(FMsgKey.Discord.Embed.ServerStatus.Description.WorldTime), "`${getWorldTime()}`", true)
                .addField(label(FMsgKey.Discord.Embed.ServerStatus.Description.LoadedChunks), "`${getLoadedChunks()}`", true)

            event.replyEmbeds(embedBuilder.build()).queue({ message ->
                FT(delay = 30000, arg = message) { msg ->
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

        val modal = Modal.create(modalID, LM.getMessage(FMsgKey.Discord.Modal.LINK.Title, lang = event.userLocale.locale).string)
            .addComponents(
                Label.of("Link Code", codeInput)
            )
            .build()

        event.replyModal(modal).queue()
    }

    private fun handleKick(event: SlashCommandInteractionEvent) {
        val executorDiscordUser = event.user
        val playerOptionString = event.getOption("player")?.asString!!
        val reasonOptionString = event.getOption("reason")?.asString

        val mcExecutor = DataBase.getMinecraftUUID(executorDiscordUser.idLong)?.let { uuid -> Server.playerManager.getPlayer(uuid) }
        val isOp = mcExecutor?.playerConfigEntry?.let { Server.playerManager.isOperator(it) }
            ?: DataBase.isOp(mcExecutor?.uuid ?: return)
            ?: run {
                event.reply(LM.getMessage(FMsgKey.Discord.Command.CannotGetPlayerPerm, lang = event.userLocale.locale).string)
                    .setEphemeral(true)
                    .queue { hook ->
                        hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS)
                    }
                return
            }

        val targetPlayer = Server.playerManager.getPlayer(playerOptionString) ?: run {
            event.reply("Player not found")
                .setEphemeral(true)
                .queue { hook -> hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS) }
            return
        }

        if (!isOp) {
            event.reply(LM.getMessage(FMsgKey.Discord.Command.Kick.NoPermission, lang = event.userLocale.locale).string)
                .setEphemeral(true)
                .queue { hook ->
                    hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS)
                }
            return
        }

        targetPlayer.networkHandler.disconnect(DisconnectionInfo(Text.of(reasonOptionString)))

        val ac = mapOf("player" to targetPlayer.name.string)

        event.reply(LM.getMessage(FMsgKey.Discord.Command.Kick.SentKickPacket, ac, lang = event.userLocale.locale).string)
            .setEphemeral(true)
            .queue { hook ->
                hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS)
            }
    }

    private fun handleBan(event: SlashCommandInteractionEvent) {
        val executorDiscordUser = event.user
        val playerName = event.getOption("player")!!.asString
        val reason = event.getOption("reason")?.asString ?: "Banned by an operator."
        val expireDays = event.getOption("expire_date")?.asInt

        val uuid = DataBase.getMinecraftUUID(executorDiscordUser.idLong) ?: run {
            event.reply(LM.getMessage(FMsgKey.Discord.Command.NoLinkedAccount, lang = event.userLocale.locale).string)
                .setEphemeral(true)
                .queue { hook -> hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS) }
            return
        }
        val player = Server.playerManager.getPlayer(uuid)

        val isOp = player?.playerConfigEntry?.let { Server.playerManager.isOperator(it) }
            ?: DataBase.isOp(uuid)
            ?: run {
                event.reply(LM.getMessage(FMsgKey.Discord.Command.CannotGetPlayerPerm, lang = event.userLocale.locale).string)
                    .setEphemeral(true)
                    .queue { hook -> hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS) }
                return
            }

        if (!isOp) {
            event.reply(LM.getMessage(FMsgKey.Discord.Command.Ban.NoPermission, lang = event.userLocale.locale).string)
                .setEphemeral(true)
                .queue { hook -> hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS) }
            return
        }

        val targetOnlinePlayer = Server.playerManager.getPlayer(playerName)
        val targetEntry = targetOnlinePlayer?.playerConfigEntry
            ?: Server.apiServices.comp_4407.findByName(playerName).orElse(null)
            ?: run {
                event.reply(LM.getMessage(FMsgKey.Discord.Command.PlayerNotFound, lang = event.userLocale.locale).string)
                    .setEphemeral(true)
                    .queue { hook -> hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS) }
                return
            }

        val created = Date()
        val source = executorDiscordUser.name
        val expiry =
            if (expireDays != null)
                Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(expireDays.toLong()))
            else
                null

        val entry = BannedPlayerEntry(targetEntry, created, source, expiry, reason)
        Server.playerManager.userBanList.add(entry)

        targetOnlinePlayer?.networkHandler?.disconnect(Text.literal(reason))

        val ac = mapOf("player" to (targetEntry.comp_4423 ?: playerName))
        event.reply(LM.getMessage(FMsgKey.Discord.Command.Ban.SendBanPacket, ac, lang = event.userLocale.locale).string)
            .setEphemeral(true)
            .queue { hook -> hook.deleteOriginal().queueAfter(7, TimeUnit.SECONDS) }
    }

    private fun handlePardon(event: SlashCommandInteractionEvent) {
        val executorDiscordUser = event.user
        val playerName = event.getOption("player")!!.asString

        val uuid = DataBase.getMinecraftUUID(executorDiscordUser.idLong) ?: run {
            event.reply(LM.getMessage(FMsgKey.Discord.Command.NoLinkedAccount, lang = event.userLocale.locale).string)
                .setEphemeral(true)
                .queue { hook -> hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS) }
            return
        }
        val mcExecutor = Server.playerManager.getPlayer(uuid)

        val isOp = mcExecutor?.playerConfigEntry?.let { Server.playerManager.isOperator(it) }
            ?: DataBase.isOp(uuid)
            ?: run {
                event.reply(LM.getMessage(FMsgKey.Discord.Command.CannotGetPlayerPerm, lang = event.userLocale.locale).string)
                    .setEphemeral(true)
                    .queue { hook -> hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS) }
                return
            }

        if (!isOp) {
            event.reply(LM.getMessage(FMsgKey.Discord.Command.Pardon.NoPermission, lang = event.userLocale.locale).string)
                .setEphemeral(true)
                .queue { hook -> hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS) }
            return
        }

        val banList = Server.playerManager.userBanList

        val bannedEntry = banList.values().firstOrNull { entry ->
            entry.key?.comp_4423.equals(playerName, ignoreCase = true)
        }

        if (bannedEntry == null) {
            event.reply("$playerName is not banned.")
                .setEphemeral(true)
                .queue { hook -> hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS) }
            return
        }

        banList.remove(bannedEntry.key)

        event.reply(
            LM.getMessage(
                FMsgKey.Discord.Command.Pardon.SentPardonPacket,
                mapOf("player" to playerName),
                lang = event.userLocale.locale
            ).string
        ).setEphemeral(false).queue()
    }

    private fun handleRun(event: SlashCommandInteractionEvent) {
        val executorDiscordUser = event.user
        val command = event.getOption("command")!!.asString

        val uuid = DataBase.getMinecraftUUID(executorDiscordUser.idLong) ?: run {
            event.reply(LM.getMessage(FMsgKey.Discord.Command.NoLinkedAccount, lang = event.userLocale.locale).string)
                .setEphemeral(true)
                .queue { hook -> hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS) }
            return
        }

        val mcExecutor = Server.playerManager.getPlayer(uuid)
        val isOp = mcExecutor?.playerConfigEntry?.let { Server.playerManager.isOperator(it) }
            ?: DataBase.isOp(uuid)
            ?: run {
                event.reply(LM.getMessage(FMsgKey.Discord.Command.CannotGetPlayerPerm, lang = event.userLocale.locale).string)
                    .setEphemeral(true)
                    .queue { hook -> hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS) }
                return
            }

        if (!isOp) {
            event.reply(LM.getMessage(FMsgKey.Discord.Command.Run.NoPermission, lang = event.userLocale.locale).string)
                .setEphemeral(true)
                .queue { hook -> hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS) }
            return
        }

        Server.execute {
            Server.commandManager.parseAndExecute(Server.commandSource, command)
        }

        val ac = mapOf("command" to command)
        event.reply(LM.getMessage(FMsgKey.Discord.Command.Run.Executed, ac, lang = event.userLocale.locale).string)
            .setEphemeral(true)
            .queue { hook -> hook.deleteOriginal().queueAfter(10, TimeUnit.SECONDS) }
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
        return "$usedMemory / $totalMemory MB (Max: $maxMemory MB)"
    }

    private fun getPlayerInfo(): String {
        val playerCount = Server.playerManager.playerList.size
        val maxPlayers = Server.playerManager.maxPlayerCount
        return "$playerCount / $maxPlayers"
    }

    private fun getUptime(): String {
        val uptimeMs = System.currentTimeMillis() - Fabricord.serverStartTime
        val totalSeconds = uptimeMs / 1000
        val days = totalSeconds / 86400
        val hours = (totalSeconds % 86400) / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}m"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m ${seconds}s"
        }
    }

    private fun getWorldTime(): String {
        val time = runCatching { Server.overworld.time % 24000 }.getOrDefault(0L)
        val label = when {
            time < 13000 -> "☀ Day"
            else -> "☽ Night"
        }
        return "$label ($time)"
    }

    private fun getLoadedChunks(): Int {
        return runCatching {
            Server.worlds.sumOf { world -> world.chunkManager.loadedChunkCount }
        }.getOrDefault(0)
    }

    private fun tpsColor(tps: Double): Color = when {
        tps >= 18.0 -> Color(0x57F287)
        tps >= 15.0 -> Color(0xFEE75C)
        else -> Color(0xED4245)
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