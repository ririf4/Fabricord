@file:Suppress("unused")

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
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.modals.Modal
import net.dv8tion.jda.api.requests.GatewayIntent
import net.ririfa.fabricord.Fabricord
import net.ririfa.fabricord.Logger
import net.ririfa.fabricord.config.LogChannelType
import net.ririfa.fabricord.i18n.FMsgKey
import net.ririfa.fabricord.util.MessageTemplate
import net.ririfa.fabricord.util.MessageStyle
import java.awt.Color
import java.time.Instant
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object DiscordBridge {
    lateinit var platform: DiscordPlatform
        private set
    internal var jda: JDA? = null
        private set
    private val webhooks = ConcurrentHashMap<String, Webhook>()
    private val consoleQueue = ConcurrentLinkedQueue<String>()
    private var activityTask: ScheduledFuture<*>? = null
    private var consoleTask: ScheduledFuture<*>? = null

    @Volatile
    var isRunning: Boolean = false
        private set

    fun installPlatform(platform: DiscordPlatform) {
        check(!isRunning) { "Cannot replace the Discord platform while the bridge is running" }
        this.platform = platform
    }

    fun start() {
        if (isRunning) return
        val token = Fabricord.config.botToken ?: return

        Fabricord.executor.execute {
            runCatching {
                val intents = buildList {
                    add(GatewayIntent.MESSAGE_CONTENT)
                    if (
                        !Fabricord.config.opSyncRoleIDs.isNullOrEmpty() ||
                        Fabricord.config.discordCommandPermissions.orEmpty().values.any { it.allowedRoleIDs.isNotEmpty() }
                    ) {
                        add(GatewayIntent.GUILD_MEMBERS)
                    }
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

                Fabricord.config.botActivityMessage?.let { format ->
                    platform.execute {
                        jda?.presence?.activity = activity(
                            Fabricord.config.botActivityStatus,
                            MessageTemplate.render(format, serverValues()),
                        )
                    }
                }

                Fabricord.config.serverStartMessage?.let {
                    platform.execute {
                        sendText(renderServerTemplate(it), channels(LogChannelType.ServerStart))
                    }
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
            sendText(renderServerTemplate(it), channels(LogChannelType.ServerStop))
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

    fun unlinkMinecraftAccount(minecraftUuid: UUID): Boolean {
        val removed = Fabricord.accountLinks.unlinkMinecraft(minecraftUuid)
        if (removed && !Fabricord.config.opSyncRoleIDs.isNullOrEmpty()) {
            platform.execute { platform.setOperator(minecraftUuid, false) }
        }
        return removed
    }

    private fun unlinkDiscordAccount(discordUserId: Long): Boolean {
        val minecraftUuid = Fabricord.accountLinks.findMinecraftUuid(discordUserId)
        val removed = Fabricord.accountLinks.unlinkDiscord(discordUserId)
        if (removed && minecraftUuid != null && !Fabricord.config.opSyncRoleIDs.isNullOrEmpty()) {
            platform.execute { platform.setOperator(minecraftUuid, false) }
        }
        return removed
    }

    fun sendMinecraftChat(playerUuid: UUID, playerName: String, message: String) {
        if (!isRunning) return
        val channelIds = channels(LogChannelType.Chat) ?: return
        val player = MinecraftPlayerRef(playerUuid, playerName)
        val values = serverValues() + mapOf(
            "player" to playerName,
            "uuid" to playerUuid,
            "message" to message,
        )
        val configured = Fabricord.config.minecraftToDiscordMessageFormat
        if (Fabricord.config.messageStyle == MessageStyle.MODERN) {
            sendWebhookChat(player, MessageTemplate.render(configured ?: "{message}", values), channelIds)
        } else {
            sendText(MessageTemplate.render(configured ?: "{player} » {message}", values), channelIds)
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

    private fun sendWebhookChat(player: MinecraftPlayerRef, content: String, channelIds: Set<String>) {
        runCatching {
            sendWebhookChatUnsafe(player, content, channelIds)
        }.onFailure { error ->
            Logger.error(
                defaultMessage(FMsgKey.Discord.Bot.ErrorDuringSendingModernMessage),
                error,
            )
        }
    }

    private fun sendWebhookChatUnsafe(player: MinecraftPlayerRef, content: String, channelIds: Set<String>) {
        channelIds.forEach { channelId ->
            val webhook = webhooks[channelId]
            if (webhook == null) {
                sendText("${player.name} » $content", setOf(channelId))
                return@forEach
            }

            val action = webhook.sendMessage(sanitizeBlockedMentions(content))
                .setUsername(player.name)
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
            Commands.slash("unlink", "Unlink a Discord account from Minecraft")
                .addOption(OptionType.USER, "user", "Account to unlink (administrators only)", false),
            Commands.slash("whois", "Show the Minecraft account linked to a Discord account")
                .addOption(OptionType.USER, "user", "Account to look up (administrators only)", false),
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

    private fun configuredStatus(): OnlineStatus = when (
        Fabricord.config.botOnlineStatus?.trim()?.lowercase(Locale.ROOT)
    ) {
        "idle" -> OnlineStatus.IDLE
        "dnd", "do_not_disturb", "do-not-disturb" -> OnlineStatus.DO_NOT_DISTURB
        "invisible" -> OnlineStatus.INVISIBLE
        else -> OnlineStatus.ONLINE
    }

    private fun activity(type: String?, message: String): Activity = when (type?.lowercase(Locale.ROOT)) {
        "listening" -> Activity.listening(message)
        "watching" -> Activity.watching(message)
        "competing" -> Activity.competing(message)
        else -> Activity.playing(message)
    }

    private fun updatePlayerCountActivity(format: String) {
        if (!isRunning) return
        platform.execute {
            if (!isRunning) return@execute
            val message = MessageTemplate.render(format, serverValues())
            jda?.presence?.activity = activity(Fabricord.config.botActivityStatus, message)
        }
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
                event.channel.id == Fabricord.config.consoleLogChannelID -> executeConsoleCommand(event)
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
                "unlink" -> unlink(event)
                "whois" -> whois(event)
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
            val displacedAccounts = Fabricord.accountLinks.link(minecraftUuid, event.user.idLong)
            if (!Fabricord.config.opSyncRoleIDs.isNullOrEmpty()) {
                platform.execute {
                    displacedAccounts.forEach { platform.setOperator(it, false) }
                }
            }
            OpSync.syncOnLink(minecraftUuid)
            event.reply(message(FMsgKey.Discord.Modal.LINK.LinkedSuccessfully, event.userLocale.locale))
                .setEphemeral(true)
                .queue()
        }

        private fun sendDiscordChatToMinecraft(event: MessageReceivedEvent) {
            val memberName = event.member?.effectiveName ?: event.author.effectiveName
            val roleName = event.member?.roles?.maxByOrNull { it.position }?.name
            val raw = event.message.contentDisplay

            platform.execute {
                val players = platform.onlinePlayers()
                val mentioned = players.filter { player ->
                    raw.contains("@${player.name}", ignoreCase = true) ||
                        raw.contains("@{${player.uuid}}", ignoreCase = true)
                }.mapTo(mutableSetOf(), MinecraftPlayerRef::uuid)
                val normalized = players.fold(raw) { message, player ->
                    message.replace("@{${player.uuid}}", "@${player.name}", ignoreCase = true)
                }
                val template = Fabricord.config.discordToMinecraftMessageFormat
                    ?: "[Discord{role_prefix}] {user} » {message}"
                val rendered = MessageTemplate.render(
                    template,
                    serverValues() + mapOf(
                        "user" to memberName,
                        "role" to (roleName ?: ""),
                        "role_prefix" to roleName?.let { " | $it" }.orEmpty(),
                        "message" to normalized,
                    ),
                )
                val messageStart = rendered.indexOf(normalized).takeIf { it >= 0 } ?: rendered.length
                platform.broadcastDiscordMessage(
                    rendered.substring(0, messageStart),
                    rendered.substring(messageStart),
                    mentioned,
                )
            }
        }

        private fun playerList(event: SlashCommandInteractionEvent) {
            event.deferReply().queue { hook ->
                platform.execute {
                    val players = platform.onlinePlayers()
                    val description = if (players.isEmpty()) {
                        message(FMsgKey.Discord.Embed.PlayerList.ThereAreNoPlayersOnline, event.userLocale.locale)
                    } else {
                        val heading = platform.message(
                            FMsgKey.Discord.Embed.PlayerList.Description,
                            event.userLocale.locale,
                            mapOf("playerCount" to players.size.toString()),
                        )
                        "$heading\n${players.joinToString("\n") { it.name }}"
                    }
                    hook.editOriginalEmbeds(
                        EmbedBuilder()
                            .setTitle(message(FMsgKey.Discord.Embed.PlayerList.Title, event.userLocale.locale))
                            .setDescription(description)
                            .setColor(Color(0x57F287))
                            .build()
                    ).queue()
                }
            }
        }

        private fun status(event: SlashCommandInteractionEvent) {
            event.deferReply().queue { hook ->
                platform.execute {
                    val runtime = Runtime.getRuntime()
                    val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
                    val maxMemory = runtime.maxMemory() / 1024 / 1024
                    val snapshot = platform.snapshot()
                    val worldPeriod = if (snapshot.worldTime < 13_000) {
                        FMsgKey.Discord.Embed.ServerStatus.Description.Day
                    } else {
                        FMsgKey.Discord.Embed.ServerStatus.Description.Night
                    }
                    val worldTimeDisplay = "${message(worldPeriod, event.userLocale.locale)} (${snapshot.worldTime})"

                    hook.editOriginalEmbeds(
                        EmbedBuilder()
                            .setTitle(message(FMsgKey.Discord.Embed.ServerStatus.Title, event.userLocale.locale))
                            .setColor(if (snapshot.tps >= 18) Color(0x57F287) else if (snapshot.tps >= 15) Color(0xFEE75C) else Color(0xED4245))
                            .setTimestamp(Instant.now())
                            .addField("TPS", String.format(Locale.ROOT, "%.2f", snapshot.tps), true)
                            .addField("MSPT", String.format(Locale.ROOT, "%.2f ms", snapshot.mspt), true)
                            .addField("Players", "${snapshot.playerCount}/${snapshot.maxPlayers}", true)
                            .addField(message(FMsgKey.Discord.Embed.ServerStatus.Description.MemoryUsage, event.userLocale.locale), "$usedMemory/$maxMemory MB", true)
                            .addField(message(FMsgKey.Discord.Embed.ServerStatus.Description.Uptime, event.userLocale.locale), MessageTemplate.formatDuration(snapshot.uptimeMillis), true)
                            .addField(message(FMsgKey.Discord.Embed.ServerStatus.Description.Version, event.userLocale.locale), snapshot.version, true)
                            .addField(message(FMsgKey.Discord.Embed.ServerStatus.Description.WorldTime, event.userLocale.locale), worldTimeDisplay, true)
                            .addField(message(FMsgKey.Discord.Embed.ServerStatus.Description.LoadedChunks, event.userLocale.locale), snapshot.loadedChunks.toString(), true)
                            .build()
                    ).queue()
                }
            }
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

        private fun unlink(event: SlashCommandInteractionEvent) {
            val target = event.getOption("user")?.asUser ?: event.user
            if (target.idLong != event.user.idLong && !isAuthorized(event, "accounts")) {
                event.reply(message(FMsgKey.Discord.Command.Account.NoPermission, event.userLocale.locale))
                    .setEphemeral(true)
                    .queue()
                return
            }

            val removed = unlinkDiscordAccount(target.idLong)
            val key = if (removed) {
                FMsgKey.Discord.Command.Account.Unlinked
            } else {
                FMsgKey.Discord.Command.Account.NotLinked
            }
            event.reply(
                platform.message(
                    key,
                    event.userLocale.locale,
                    mapOf("user" to target.effectiveName),
                )
            ).setEphemeral(true).queue()
        }

        private fun whois(event: SlashCommandInteractionEvent) {
            val target = event.getOption("user")?.asUser ?: event.user
            if (target.idLong != event.user.idLong && !isAuthorized(event, "accounts")) {
                event.reply(message(FMsgKey.Discord.Command.Account.NoPermission, event.userLocale.locale))
                    .setEphemeral(true)
                    .queue()
                return
            }

            platform.execute {
                val minecraftUuid = Fabricord.accountLinks.findMinecraftUuid(target.idLong)
                if (minecraftUuid == null) {
                    event.reply(
                        platform.message(
                            FMsgKey.Discord.Command.Account.NotLinked,
                            event.userLocale.locale,
                            mapOf("user" to target.effectiveName),
                        )
                    ).setEphemeral(true).queue()
                    return@execute
                }
                val playerName = platform.findPlayerName(minecraftUuid) ?: minecraftUuid.toString()
                event.reply(
                    platform.message(
                        FMsgKey.Discord.Command.Account.LinkedAs,
                        event.userLocale.locale,
                        mapOf(
                            "user" to target.effectiveName,
                            "player" to playerName,
                            "uuid" to minecraftUuid.toString(),
                        ),
                    )
                ).setEphemeral(true).queue()
            }
        }

        private fun administrativeCommand(event: SlashCommandInteractionEvent) {
            event.deferReply(true).queue { hook ->
                platform.execute {
                    administrativeCommandOnServer(event, hook)
                }
            }
        }

        private fun administrativeCommandOnServer(
            event: SlashCommandInteractionEvent,
            hook: InteractionHook,
        ) {
            if (!isAuthorized(event, event.name)) {
                hook.editOriginal(noPermissionMessage(event)).queue()
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
                hook.editOriginal("Invalid command arguments.").queue()
                return
            }
            val playerName = event.getOption("player")?.asString
            if (playerName != null && !platform.playerTargetExists(event.name, playerName)) {
                hook.editOriginal(message(FMsgKey.Discord.Command.PlayerNotFound, event.userLocale.locale)).queue()
                return
            }

            platform.executeServerCommand(command)
            val successKey = when (event.name) {
                "kick" -> FMsgKey.Discord.Command.Kick.SentKickPacket
                "ban" -> FMsgKey.Discord.Command.Ban.SentBanPacket
                "pardon" -> FMsgKey.Discord.Command.Pardon.SentPardonPacket
                else -> FMsgKey.Discord.Command.Run.Executed
            }
            val argumentName = if (event.name == "run") "command" else "player"
            val argumentValue = event.getOption(argumentName)?.asString ?: command
            hook.editOriginal(
                platform.message(
                    successKey,
                    event.userLocale.locale,
                    mapOf(argumentName to argumentValue),
                )
            ).queue()
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

    private fun executeConsoleCommand(event: MessageReceivedEvent) {
        val command = event.message.contentRaw.trim().removePrefix("/")
        if (command.isEmpty()) return
        platform.execute {
            if (isAuthorized(event.author.id, event.member?.roles?.mapTo(mutableSetOf()) { it.id }.orEmpty(), "console")) {
                platform.executeServerCommand(command)
            }
        }
    }

    private fun isLinkedOperator(discordUserId: Long): Boolean? {
        val uuid = Fabricord.accountLinks.findMinecraftUuid(discordUserId) ?: return false
        return platform.isOperator(uuid)
    }

    private fun isAuthorized(event: SlashCommandInteractionEvent, command: String): Boolean =
        isAuthorized(
            event.user.id,
            event.member?.roles?.mapTo(mutableSetOf()) { it.id }.orEmpty(),
            command,
        )

    private fun isAuthorized(userId: String, roleIds: Set<String>, command: String): Boolean {
        val permission = Fabricord.config.commandPermission(command)
        return DiscordCommandAuthorizer.isAuthorized(
            permission,
            DiscordMemberIdentity(userId, roleIds),
            isLinkedOperator(userId.toLong()) == true,
        )
    }

    private fun message(key: FMsgKey, language: String): String =
        platform.message(key, language)

    private fun defaultMessage(key: FMsgKey, arguments: Map<String, String> = emptyMap()): String =
        platform.message(key, "en", arguments)

    private fun serverValues(): Map<String, Any> = MessageTemplate.serverValues(platform.snapshot())

    private fun renderServerTemplate(template: String): String =
        MessageTemplate.render(template, serverValues())

    private const val LINK_MODAL_ID = "fabricord-link-account"
    private val PLAYER_NAME = Regex("[A-Za-z0-9_]{1,16}")
}
