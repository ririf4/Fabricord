package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.Webhook
import net.dv8tion.jda.api.events.guild.member.GuildMemberUpdateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.exceptions.InvalidTokenException
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.modals.Modal
import net.dv8tion.jda.api.requests.GatewayIntent
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.ririfa.fabricord.Fabricord
import net.ririfa.fabricord.Logger
import net.ririfa.fabricord.config.LogChannelType
import net.ririfa.fabricord.i18n.FMsgKey
import net.ririfa.fabricord.util.MessageStyle
import java.awt.Color
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object DiscordBridge {
    internal var jda: JDA? = null
        private set
    private val webhooks = mutableMapOf<String, Webhook>()
    private val consoleQueue = ConcurrentLinkedQueue<String>()
    private var activityTask: ScheduledFuture<*>? = null
    private var consoleTask: ScheduledFuture<*>? = null

    @Volatile
    var isRunning: Boolean = false
        private set

    fun start() {
        if (isRunning) return
        val token = Fabricord.config.botToken ?: return

        Fabricord.executor.execute {
            runCatching {
                val intents = buildList {
                    add(GatewayIntent.MESSAGE_CONTENT)
                    if (!Fabricord.config.opSyncRoleIDs.isNullOrEmpty()) add(GatewayIntent.GUILD_MEMBERS)
                }
                jda = JDABuilder.createDefault(token)
                    .enableIntents(intents)
                    .setAutoReconnect(true)
                    .setActivity(configuredActivity())
                    .setStatus(configuredStatus())
                    .addEventListeners(Listener())
                    .build()
                    .awaitReady()

                registerSlashCommands()
                setupWebhooks()
                isRunning = true

                Fabricord.config.serverStartMessage?.let {
                    sendText(it, channels(LogChannelType.ServerStart))
                }
                Fabricord.config.playerCountActivityFormat?.let { format ->
                    activityTask = Fabricord.executor.scheduleAtFixedRate(
                        { updatePlayerCountActivity(format) },
                        0,
                        30,
                        TimeUnit.SECONDS,
                    )
                }
                if (Fabricord.config.consoleLogChannelID != null) {
                    consoleTask = Fabricord.executor.scheduleAtFixedRate(
                        ::flushConsole,
                        5,
                        5,
                        TimeUnit.SECONDS,
                    )
                }
                Logger.info(
                    defaultMessage(
                        FMsgKey.Discord.Bot.BotNowOnline,
                        mapOf("botName" to (jda?.selfUser?.name ?: "Fabricord")),
                    )
                )
            }.onFailure { error ->
                val loginFailed = generateSequence(error) { it.cause }
                    .any { it is InvalidTokenException }
                val key = if (loginFailed) {
                    FMsgKey.Discord.Bot.CannotLoginToBot
                } else {
                    FMsgKey.Discord.Bot.CannotStartBot
                }
                Logger.error(defaultMessage(key), error)
                isRunning = false
                jda = null
            }
        }
    }

    fun restart() {
        stop()
        if (Fabricord.config.botToken != null) start()
    }

    fun stop() {
        activityTask?.cancel(false)
        activityTask = null
        consoleTask?.cancel(false)
        consoleTask = null
        val bot = jda ?: return

        Fabricord.config.serverStopMessage?.let {
            sendText(it, channels(LogChannelType.ServerStop))
        }
        val botName = runCatching { bot.selfUser.name }.getOrDefault("?")
        runCatching {
            bot.shutdown()
            if (!bot.awaitShutdown(5, TimeUnit.SECONDS)) {
                Logger.warn(defaultMessage(FMsgKey.Discord.Bot.TimedOutForStoppingBot))
                bot.shutdownNow()
            }
        }.onSuccess {
            Logger.info(
                defaultMessage(
                    FMsgKey.Discord.Bot.BotNowOffline,
                    mapOf("botName" to botName),
                )
            )
        }.onFailure {
            Logger.warn(defaultMessage(FMsgKey.Discord.Bot.CannotStopBot), it)
        }
        webhooks.clear()
        jda = null
        isRunning = false
    }

    fun channels(type: LogChannelType): Set<String>? =
        Fabricord.config.logChannels?.get(type)
            ?: Fabricord.config.logChannels?.get(LogChannelType.Default)

    fun sendMinecraftChat(player: ServerPlayer, message: String) {
        if (!isRunning) return
        val channelIds = channels(LogChannelType.Chat) ?: return
        if (Fabricord.config.messageStyle == MessageStyle.MODERN) {
            sendWebhookChat(player, message, channelIds)
        } else {
            sendText("${player.name.string} » $message", channelIds)
        }
    }

    fun sendText(message: String, channelIds: Set<String>?) {
        val bot = jda ?: return
        channelIds ?: return
        val sanitized = sanitizeBlockedMentions(message)
        channelIds.forEach { channelId ->
            bot.getTextChannelById(channelId)?.sendMessage(sanitized)
                ?.setAllowedMentions(
                    if (Fabricord.config.blockAllMentions) emptySet()
                    else setOf(Message.MentionType.USER, Message.MentionType.ROLE)
                )
                ?.queue({}, { error -> Logger.warn("Could not send a Discord message", error) })
        }
    }

    fun sendEmbed(embed: MessageEmbed, channelIds: Set<String>?) {
        val bot = jda ?: return
        channelIds?.forEach { channelId ->
            bot.getTextChannelById(channelId)?.sendMessageEmbeds(embed)?.queue(
                {},
                { error -> Logger.warn("Could not send a Discord embed", error) },
            )
        }
    }

    fun enqueueConsole(message: String) {
        if (isRunning && Fabricord.config.consoleLogChannelID != null) {
            consoleQueue.add(message)
        }
    }

    private fun flushConsole() {
        val channelId = Fabricord.config.consoleLogChannelID ?: return
        val channel = jda?.getTextChannelById(channelId) ?: return
        val chunks = mutableListOf<String>()
        var current = StringBuilder()
        while (true) {
            val line = consoleQueue.poll() ?: break
            if (current.length + line.length + 1 > 1_900 && current.isNotEmpty()) {
                chunks += current.toString()
                current = StringBuilder()
            }
            if (current.isNotEmpty()) current.append('\n')
            current.append(line.take(1_900))
        }
        if (current.isNotEmpty()) chunks += current.toString()
        chunks.forEach { chunk ->
            channel.sendMessage("```\n$chunk\n```").setAllowedMentions(emptySet()).queue()
        }
    }

    private fun sendWebhookChat(player: ServerPlayer, content: String, channelIds: Set<String>) {
        runCatching {
            sendWebhookChatUnsafe(player, content, channelIds)
        }.onFailure { error ->
            Logger.error(
                defaultMessage(FMsgKey.Discord.Bot.ErrorDuringSendingModernMessage),
                error,
            )
        }
    }

    private fun sendWebhookChatUnsafe(player: ServerPlayer, content: String, channelIds: Set<String>) {
        channelIds.forEach { channelId ->
            val webhook = webhooks[channelId]
            if (webhook == null) {
                sendText("${player.name.string} » $content", setOf(channelId))
                return@forEach
            }

            val action = webhook.sendMessage(sanitizeBlockedMentions(content))
                .setUsername(player.name.string)
                .setAvatarUrl("https://visage.surgeplay.com/face/256/${player.uuid}")
            fun send() = action.queue(
                {},
                { error ->
                    Logger.error(
                        defaultMessage(FMsgKey.Discord.Bot.ErrorDuringSendingModernMessage),
                        error,
                    )
                },
            )

            if (Fabricord.config.blockAllMentions) {
                action.setAllowedMentions(emptySet())
                send()
                return@forEach
            }

            if (!Fabricord.config.useUserPermissionForMentions) {
                send()
                return@forEach
            }

            val discordId = Fabricord.accountLinks.findDiscordId(player.uuid)
            val guild = jda?.getTextChannelById(channelId)?.guild
            if (discordId == null || guild == null) {
                action.setAllowedMentions(emptySet())
                send()
                return@forEach
            }

            guild.retrieveMemberById(discordId).queue(
                { member ->
                    val allowed = mutableSetOf(Message.MentionType.USER)
                    if (member.roles.isNotEmpty()) allowed += Message.MentionType.ROLE
                    if (member.hasPermission(net.dv8tion.jda.api.Permission.MESSAGE_MENTION_EVERYONE)) {
                        allowed += Message.MentionType.EVERYONE
                    }
                    action.setAllowedMentions(allowed)
                    send()
                },
                {
                    action.setAllowedMentions(emptySet())
                    send()
                },
            )
        }
    }

    private fun setupWebhooks() {
        Fabricord.config.logChannels.orEmpty().values.flatten().toSet().forEach { channelId ->
            val channel = jda?.getTextChannelById(channelId) ?: return@forEach
            channel.retrieveWebhooks().queue(
                { existing ->
                    existing.firstOrNull { it.name == "Fabricord" }?.let { webhooks[channelId] = it }
                        ?: channel.createWebhook("Fabricord").queue { webhooks[channelId] = it }
                },
                { error -> Logger.warn("Could not prepare a webhook in channel $channelId", error) },
            )
        }
    }

    private fun registerSlashCommands() {
        jda?.updateCommands()?.addCommands(
            Commands.slash("playerlist", "Show online Minecraft players"),
            Commands.slash("status", "Show the Minecraft server status"),
            Commands.slash("link", "Link this Discord account to Minecraft"),
            Commands.slash("kick", "Kick a Minecraft player")
                .addOptions(
                    OptionData(OptionType.STRING, "player", "Minecraft player name", true),
                    OptionData(OptionType.STRING, "reason", "Kick reason", false),
                ),
            Commands.slash("ban", "Ban a Minecraft player")
                .addOptions(
                    OptionData(OptionType.STRING, "player", "Minecraft player name", true),
                    OptionData(OptionType.STRING, "reason", "Ban reason", false),
                ),
            Commands.slash("pardon", "Pardon a Minecraft player")
                .addOption(OptionType.STRING, "player", "Minecraft player name", true),
            Commands.slash("run", "Run a Minecraft server command")
                .addOption(OptionType.STRING, "command", "Command without the leading slash", true),
        )?.queue()
    }

    private fun configuredActivity(): Activity? {
        val message = Fabricord.config.botActivityMessage ?: return null
        return activity(Fabricord.config.botActivityStatus, message)
    }

    private fun configuredStatus(): OnlineStatus = when (Fabricord.config.botOnlineStatus?.lowercase()) {
        "idle" -> OnlineStatus.IDLE
        "dnd" -> OnlineStatus.DO_NOT_DISTURB
        "invisible" -> OnlineStatus.INVISIBLE
        else -> OnlineStatus.ONLINE
    }

    private fun activity(type: String?, message: String): Activity = when (type?.lowercase()) {
        "listening" -> Activity.listening(message)
        "watching" -> Activity.watching(message)
        "competing" -> Activity.competing(message)
        else -> Activity.playing(message)
    }

    private fun updatePlayerCountActivity(format: String) {
        if (!isRunning) return
        val players = Fabricord.server.playerList
        val message = format
            .replace("{count}", players.playerCount.toString())
            .replace("{max}", players.maxPlayers.toString())
        jda?.presence?.activity = activity(Fabricord.config.botActivityStatus, message)
    }

    private fun sanitizeBlockedMentions(raw: String): String {
        var result = raw
        Fabricord.config.mentionBlockedUserIDs.orEmpty().forEach { id ->
            result = result.replace(Regex("<@!?${Regex.escape(id)}>"), "@\u200Buser")
        }
        Fabricord.config.mentionBlockedRoleIDs.orEmpty().forEach { id ->
            result = result.replace(Regex("<@&${Regex.escape(id)}>"), "@\u200Brole")
        }
        return result
    }

    private class Listener : ListenerAdapter() {
        override fun onMessageReceived(event: MessageReceivedEvent) {
            if (event.author.isBot) return
            val chatChannels = channels(LogChannelType.Chat).orEmpty()
            when {
                event.channel.id in chatChannels -> sendDiscordChatToMinecraft(event)
                event.channel.id == Fabricord.config.consoleLogChannelID && isLinkedOperator(event.author.idLong) == true -> {
                    val command = event.message.contentRaw.trim().removePrefix("/")
                    if (command.isNotEmpty()) executeServerCommand(command)
                }
            }
        }

        override fun onGuildMemberUpdate(event: GuildMemberUpdateEvent) {
            OpSync.syncFromRoleChange(event.member.idLong, event.member.roles)
        }

        override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
            when (event.name) {
                "playerlist" -> playerList(event)
                "status" -> status(event)
                "link" -> link(event)
                "kick", "ban", "pardon", "run" -> administrativeCommand(event)
            }
        }

        override fun onModalInteraction(event: ModalInteractionEvent) {
            if (event.modalId != LINK_MODAL_ID) return
            val minecraftUuid = event.getValue("code")?.asString?.let(Fabricord.linkCodes::consume)
            if (minecraftUuid == null) {
                event.reply(message(FMsgKey.Discord.Modal.LINK.Invalid, event.userLocale.locale))
                    .setEphemeral(true)
                    .queue()
                return
            }
            Fabricord.accountLinks.link(minecraftUuid, event.user.idLong)
            OpSync.syncOnLink(minecraftUuid)
            event.reply(message(FMsgKey.Discord.Modal.LINK.LinkedSuccessfully, event.userLocale.locale))
                .setEphemeral(true)
                .queue()
        }

        private fun sendDiscordChatToMinecraft(event: MessageReceivedEvent) {
            val memberName = event.member?.effectiveName ?: event.author.effectiveName
            val roleName = event.member?.roles?.maxByOrNull { it.position }?.name
            val prefix = buildString {
                append("[Discord")
                if (roleName != null) append(" | ").append(roleName)
                append("] ").append(memberName).append(" » ")
            }
            val raw = event.message.contentDisplay
            val mentioned = Fabricord.server.playerList.players.filter { player ->
                raw.contains("@${player.name.string}", ignoreCase = true) ||
                    raw.contains("@{${player.uuid}}", ignoreCase = true)
            }.toSet()
            val normalized = Fabricord.server.playerList.players.fold(raw) { message, player ->
                message.replace("@{${player.uuid}}", "@${player.name.string}", ignoreCase = true)
            }

            Fabricord.server.execute {
                Fabricord.server.playerList.players.forEach { player ->
                    val message = Component.literal(prefix).withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(normalized).withStyle(if (player in mentioned) ChatFormatting.BOLD else ChatFormatting.WHITE))
                    player.sendSystemMessage(message)
                    if (player in mentioned) {
                        player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 2.0f, 2.0f)
                    }
                }
            }
        }

        private fun playerList(event: SlashCommandInteractionEvent) {
            val players = Fabricord.server.playerList.players
            val description = if (players.isEmpty()) {
                message(FMsgKey.Discord.Embed.PlayerList.ThereAreNoPlayersOnline, event.userLocale.locale)
            } else {
                val heading = Fabricord.langMan.getMessage(
                    FMsgKey.Discord.Embed.PlayerList.Description,
                    argsComplete = mapOf("playerCount" to players.size.toString()),
                    lang = event.userLocale.locale,
                ).string
                "$heading\n${players.joinToString("\n") { it.name.string }}"
            }
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle(message(FMsgKey.Discord.Embed.PlayerList.Title, event.userLocale.locale))
                    .setDescription(description)
                    .setColor(Color(0x57F287))
                    .build()
            ).queue()
        }

        private fun status(event: SlashCommandInteractionEvent) {
            val runtime = Runtime.getRuntime()
            val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
            val maxMemory = runtime.maxMemory() / 1024 / 1024
            val mspt = Fabricord.server.tickTimes.average() / 1_000_000.0
            val tps = if (mspt <= 0.0) 20.0 else minOf(20.0, 1000.0 / mspt)
            val uptime = System.currentTimeMillis() - Fabricord.serverStartTime
            val loadedChunks = Fabricord.server.allLevels.sumOf { it.chunkSource.loadedChunksCount }
            val players = Fabricord.server.playerList
            val worldTime = Fabricord.server.overworld().dayTime % 24_000
            val worldPeriod = if (worldTime < 13_000) {
                FMsgKey.Discord.Embed.ServerStatus.Description.Day
            } else {
                FMsgKey.Discord.Embed.ServerStatus.Description.Night
            }
            val worldTimeDisplay = "${message(worldPeriod, event.userLocale.locale)} ($worldTime)"

            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle(message(FMsgKey.Discord.Embed.ServerStatus.Title, event.userLocale.locale))
                    .setColor(if (tps >= 18) Color(0x57F287) else if (tps >= 15) Color(0xFEE75C) else Color(0xED4245))
                    .setTimestamp(Instant.now())
                    .addField("TPS", "%.2f".format(tps), true)
                    .addField("MSPT", "%.2f ms".format(mspt), true)
                    .addField("Players", "${players.playerCount}/${players.maxPlayers}", true)
                    .addField(message(FMsgKey.Discord.Embed.ServerStatus.Description.MemoryUsage, event.userLocale.locale), "$usedMemory/$maxMemory MB", true)
                    .addField(message(FMsgKey.Discord.Embed.ServerStatus.Description.Uptime, event.userLocale.locale), formatDuration(uptime), true)
                    .addField(message(FMsgKey.Discord.Embed.ServerStatus.Description.Version, event.userLocale.locale), Fabricord.server.serverVersion, true)
                    .addField(message(FMsgKey.Discord.Embed.ServerStatus.Description.WorldTime, event.userLocale.locale), worldTimeDisplay, true)
                    .addField(message(FMsgKey.Discord.Embed.ServerStatus.Description.LoadedChunks, event.userLocale.locale), loadedChunks.toString(), true)
                    .build()
            ).queue()
        }

        private fun link(event: SlashCommandInteractionEvent) {
            val input = TextInput.create("code", TextInputStyle.SHORT)
                .setMinLength(6)
                .setMaxLength(6)
                .setRequired(true)
                .build()
            val modal = Modal.create(
                LINK_MODAL_ID,
                message(FMsgKey.Discord.Modal.LINK.Title, event.userLocale.locale),
            )
                .addComponents(Label.of("Link code", input))
                .build()
            event.replyModal(modal).queue()
        }

        private fun administrativeCommand(event: SlashCommandInteractionEvent) {
            if (Fabricord.accountLinks.findMinecraftUuid(event.user.idLong) == null) {
                event.reply(message(FMsgKey.Discord.Command.NoLinkedAccount, event.userLocale.locale))
                    .setEphemeral(true)
                    .queue()
                return
            }
            val isOperator = isLinkedOperator(event.user.idLong)
            if (isOperator == null) {
                event.reply(message(FMsgKey.Discord.Command.CannotGetPlayerPerm, event.userLocale.locale))
                    .setEphemeral(true)
                    .queue()
                return
            }
            if (!isOperator) {
                event.reply(noPermissionMessage(event))
                    .setEphemeral(true)
                    .queue()
                return
            }

            val command = when (event.name) {
                "kick" -> playerCommand(event, "kick", "reason")
                "ban" -> playerCommand(event, "ban", "reason")
                "pardon" -> playerCommand(event, "pardon", null)
                "run" -> event.getOption("command")?.asString?.trim()?.removePrefix("/")
                else -> null
            }
            if (command.isNullOrBlank()) {
                event.reply("Invalid command arguments.").setEphemeral(true).queue()
                return
            }
            val playerName = event.getOption("player")?.asString
            if (playerName != null && !playerTargetExists(event.name, playerName)) {
                event.reply(message(FMsgKey.Discord.Command.PlayerNotFound, event.userLocale.locale))
                    .setEphemeral(true)
                    .queue()
                return
            }

            executeServerCommand(command)
            val successKey = when (event.name) {
                "kick" -> FMsgKey.Discord.Command.Kick.SentKickPacket
                "ban" -> FMsgKey.Discord.Command.Ban.SentBanPacket
                "pardon" -> FMsgKey.Discord.Command.Pardon.SentPardonPacket
                else -> FMsgKey.Discord.Command.Run.Executed
            }
            val argumentName = if (event.name == "run") "command" else "player"
            val argumentValue = event.getOption(argumentName)?.asString ?: command
            event.reply(
                Fabricord.langMan.getMessage(
                    successKey,
                    argsComplete = mapOf(argumentName to argumentValue),
                    lang = event.userLocale.locale,
                ).string
            ).setEphemeral(true).queue()
        }

        private fun playerCommand(event: SlashCommandInteractionEvent, command: String, reasonOption: String?): String? {
            val player = event.getOption("player")?.asString?.trim() ?: return null
            if (!PLAYER_NAME.matches(player)) return null
            val reason = reasonOption?.let { event.getOption(it)?.asString }
                ?.replace(Regex("[\r\n]"), " ")
                ?.take(200)
                ?.trim()
            return listOfNotNull(command, player, reason?.takeIf { it.isNotEmpty() }).joinToString(" ")
        }

        private fun noPermissionMessage(event: SlashCommandInteractionEvent): String {
            val key = when (event.name) {
                "kick" -> FMsgKey.Discord.Command.Kick.NoPermission
                "ban" -> FMsgKey.Discord.Command.Ban.NoPermission
                "pardon" -> FMsgKey.Discord.Command.Pardon.NoPermission
                else -> FMsgKey.Discord.Command.Run.NoPermission
            }
            return message(key, event.userLocale.locale)
        }
    }

    private fun isLinkedOperator(discordUserId: Long): Boolean? {
        val uuid = Fabricord.accountLinks.findMinecraftUuid(discordUserId) ?: return false
        return runCatching {
            val profile = Fabricord.server.profileCache?.get(uuid)?.orElse(null) ?: return null
            Fabricord.server.playerList.isOp(profile)
        }.getOrNull()
    }

    private fun playerTargetExists(command: String, playerName: String): Boolean = when (command) {
        "kick" -> Fabricord.server.playerList.getPlayerByName(playerName) != null
        "ban" -> Fabricord.server.profileCache?.get(playerName)?.isPresent == true
        else -> true
    }

    private fun executeServerCommand(command: String) {
        Fabricord.server.execute {
            Fabricord.server.commands.performPrefixedCommand(
                Fabricord.server.createCommandSourceStack(),
                command,
            )
        }
    }

    private fun formatDuration(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val days = totalSeconds / 86_400
        val hours = totalSeconds % 86_400 / 3_600
        val minutes = totalSeconds % 3_600 / 60
        return if (days > 0) "${days}d ${hours}h ${minutes}m" else "${hours}h ${minutes}m"
    }

    private fun message(key: FMsgKey, language: String): String =
        Fabricord.langMan.getMessage(key, lang = language).string

    private fun defaultMessage(key: FMsgKey, arguments: Map<String, String> = emptyMap()): String =
        Fabricord.langMan.getMessage(key, argsComplete = arguments).string

    private const val LINK_MODAL_ID = "fabricord-link-account"
    private val PLAYER_NAME = Regex("[A-Za-z0-9_]{1,16}")
}
