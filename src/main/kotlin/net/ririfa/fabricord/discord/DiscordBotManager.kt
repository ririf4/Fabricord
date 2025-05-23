package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Message.MentionType
import net.dv8tion.jda.api.entities.Webhook
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent
import net.minecraft.server.network.ServerPlayerEntity
import net.ririfa.fabricord.*
import net.ririfa.fabricord.translation.FabricordMessageKey
import net.ririfa.fabricord.util.error
import net.ririfa.fabricord.util.warn
import java.awt.Color
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.security.auth.login.LoginException

object DiscordBotManager {
    var jda: JDA? = null
    var webHook: Webhook? = null

    @JvmField
    var isBotInitialized = false

    private val intents = GatewayIntent.MESSAGE_CONTENT
    private val logQueue = ConcurrentLinkedQueue<String>()
    private val mcidPattern = Regex("@([a-zA-Z0-9_]+)")
    private val uuidPattern = Regex("@\\{([0-9a-fA-F-]+)}")

    init {
        FT(delay = 0, period = 5000, unit = TimeUnit.MILLISECONDS, newThread = true) {
            flushLogQueue()
        }
    }

    fun start() {
        FT {
            try {
                jda = JDABuilder.createDefault(Config.botToken)
                    .addEventListeners(discordListener)
                    .setStatus(onlineStatus())
                    .setActivity(activity())
                    .setAutoReconnect(true)
                    .enableIntents(intents)
                    .build()
                    .awaitReady()

                val textChannel = jda?.getTextChannelById(Config.logChannelID ?: return@FT)
                textChannel?.retrieveWebhooks()?.queue({ webhooks ->
                    webHook = webhooks.firstOrNull { it.name == "Fabricord" }

                    if (webHook == null) {
                        textChannel.createWebhook("Fabricord").queue { created ->
                            webHook = created
                            Logger.info("Created webhook: ${created.name}")
                        }
                    } else {
                        Logger.info("Using existing webhook: ${webHook?.name}")
                    }
                }, { error ->
                    Logger.warn("Could not retrieve webhook: ${error.message}")
                })

                jda?.updateCommands()?.addCommands(
                    Commands.slash("playerlist", "Get a list of online players"),
                    Commands.slash("status", "Get the status of the server")
                )?.queue()

                isBotInitialized = true
                Logger.info(LM.getMessage(FabricordMessageKey.Discord.Bot.BotNowOnline, jda?.selfUser?.name ?: "Bot").string)
                sendToDiscord(Config.serverStartMessage)
            } catch (e: LoginException) {
                Logger.error(LM.getMessage(FabricordMessageKey.Discord.Bot.CannotLoginToBot), e)
            } catch (e: Exception) {
                Logger.error(LM.getMessage(FabricordMessageKey.Discord.Bot.CannotStartBot), e)
            }
        }
    }

    fun stop() {
        sendToDiscord(Config.serverStopMessage)

        try {
            isBotInitialized = false

            jda?.let { instance ->
                val shutdownFuture = CompletableFuture.runAsync {
                    try {
                        instance.shutdown()
                    } catch (e: Exception) {
                        Logger.error("Error during JDA shutdown: ", e)
                    }
                }

                try {
                    shutdownFuture.get(7500, TimeUnit.MILLISECONDS)
                    Logger.info(LM.getMessage(FabricordMessageKey.Discord.Bot.BotNowOffline, instance.selfUser.name).string)
                } catch (_: TimeoutException) {
                    Logger.warn(LM.getMessage(FabricordMessageKey.Discord.Bot.TimedOutForStoppingBot))
                    instance.shutdownNow()
                }
            }
        } catch (e: Exception) {
            Logger.error(LM.getMessage(FabricordMessageKey.Discord.Bot.CannotStopBot), e)
            e.printStackTrace()
        }
    }

    private val discordListener = object : ListenerAdapter() {
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
                            Server.commandManager.executeWithPrefix(Server.commandSource, command)
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
    }

    private fun onlineStatus(): OnlineStatus {
        return when (Config.botOnlineStatus.lowercase()) {
            "online" -> OnlineStatus.ONLINE
            "idle" -> OnlineStatus.IDLE
            "dnd" -> OnlineStatus.DO_NOT_DISTURB
            "invisible" -> OnlineStatus.INVISIBLE
            else -> OnlineStatus.ONLINE
        }
    }

    private fun activity(): Activity {
        val activityStatus = Config.botActivityStatus
        val activityMessage = Config.botActivityMessage

        return when (activityStatus.lowercase()) {
            "playing" -> Activity.playing(activityMessage)
            "watching" -> Activity.watching(activityMessage)
            "listening" -> Activity.listening(activityMessage)
            "competing" -> Activity.competing(activityMessage)
            else -> Activity.playing(activityMessage)
        }
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

    private fun flushLogQueue() {
        if (logQueue.isEmpty()) return

        val messages = mutableListOf<String>()
        var msg: String?

        while (logQueue.poll().also { msg = it } != null) {
            messages.add(msg!!)
        }

        val combinedMessage = messages.joinToString("\n")

        Config.consoleLogChannelID?.let { channelId ->
            jda?.getTextChannelById(channelId)?.sendMessage(combinedMessage)
                ?.setAllowedMentions(emptySet())
                ?.queue()
        }
    }

    fun sendToDiscord(message: String) {
        if (Config.isLogChannelIDNotSet) return
        FT {
            Config.logChannelID?.let { channelId ->
                val blockedUserIds = Config.mentionBlockedUserID
                val blockedRoleIds = Config.mentionBlockedRoleID

                val mentionedUserIds = "<@!?([0-9]+)>".toRegex()
                    .findAll(message)
                    .map { it.groupValues[1] }
                    .toSet()

                val mentionedRoleIds = "<@&([0-9]+)>".toRegex()
                    .findAll(message)
                    .map { it.groupValues[1] }
                    .toSet()

                val allowedMentionUserIds = mentionedUserIds.filterNot { it in blockedUserIds }
                val allowedMentionRoleIds = mentionedRoleIds.filterNot { it in blockedRoleIds }

                val sanitizedMessage = message
                    .replace(Regex("<@!?(${blockedUserIds.joinToString("|")})>"), "@\u200Buser")
                    .replace(Regex("<@&(${blockedRoleIds.joinToString("|")})>"), "@\u200Brole")

                val messageAction = jda?.getTextChannelById(channelId)?.sendMessage(sanitizedMessage)

                if (Config.allowMentions == false) {
                    messageAction?.setAllowedMentions(emptySet())
                } else {
                    messageAction
                        ?.setAllowedMentions(listOf(MentionType.USER, MentionType.ROLE))
                        ?.mentionUsers(*allowedMentionUserIds.toTypedArray())
                        ?.mentionRoles(*allowedMentionRoleIds.toTypedArray())
                }

                messageAction?.queue()
            }
        }
    }

    fun sendToDiscordConsole(message: String) {
        logQueue.add(message)
    }
}