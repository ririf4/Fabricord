package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Webhook
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent
import net.ririfa.fabricord.i18n.FMsgKey
import net.ririfa.fabricord.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.security.auth.login.LoginException

object DiscordBotManager {
    internal var jda: JDA? = null

    private val intents = GatewayIntent.MESSAGE_CONTENT
    private val shutdownExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "Fabricord-JDA-Shutdown").apply {
            isDaemon = true
        }
    }

    @JvmField
    var isBotInitialized: Boolean = false

    @JvmField
    var webHook: Webhook? = null

    fun start() {
        FT {
            try {
                jda = JDABuilder.createDefault(Config.botToken)
                    .setAutoReconnect(true)
                    .setActivity(activity())
                    .setStatus(onlineStatus() ?: OnlineStatus.ONLINE)
                    .addEventListeners(CompositeDiscordListener())
                    .enableIntents(intents)
                    .build()
                    .awaitReady()

                setupWebhook()

                jda?.updateCommands()?.addCommands(
                    Commands.slash("playerlist", "Get a list of online players"),
                    Commands.slash("status", "Get the status of the server")
                )?.queue()

                Config.serverStartMessage?.let { sendToDiscord(it) }

                val c = mapOf("botName" to jda?.selfUser?.name)
                Logger.info(LM.getMessage(FMsgKey.Discord.Bot.BotNowOnline, c))
                isBotInitialized = true
            } catch (e: LoginException) {
                Logger.error(LM.getMessage(FMsgKey.Discord.Bot.CannotLoginToBot), e)
            } catch (e: Exception) {
                Logger.error(LM.getMessage(FMsgKey.Discord.Bot.CannotStartBot), e)
            }
        }
    }

    fun stop() {
        Config.serverStopMessage?.let { sendToDiscord(it) }

        runCatching {
            val shutdownFuture = CompletableFuture.runAsync({
                runCatching { jda?.shutdown() }
                    .onFailure { e -> Logger.error("Error during JDA shutdown: ", e) }
            }, shutdownExecutor)

            try {
                shutdownFuture.get(7500, TimeUnit.MILLISECONDS)
                val name = runCatching { jda?.selfUser?.name }.getOrNull() ?: "?"
                Logger.info(LM.getMessage(FMsgKey.Discord.Bot.BotNowOffline, name).string)
            } catch (_: TimeoutException) {
                Logger.warn(LM.getMessage(FMsgKey.Discord.Bot.TimedOutForStoppingBot).string)
                jda?.shutdownNow()
                Logger.warn("Forced immediate shutdown for JDA")
            }
        }.onFailure { e ->
            Logger.error(LM.getMessage(FMsgKey.Discord.Bot.CannotStopBot).string, e)
        }
    }

    fun sendToDiscord(message: String) {
        val channelId = Config.logChannelID ?: return

        FT {
            val blockAll = Config.blockAllMentions

            val blockedUserIds = Config.mentionBlockedUserID.orEmpty()
            val blockedRoleIds = Config.mentionBlockedRoleID.orEmpty()

            // --- Extract mentions ---------------------------------------
            val userMentionRegex = "<@!?([0-9]+)>".toRegex()
            val roleMentionRegex = "<@&([0-9]+)>".toRegex()

            val mentionedUserIds = userMentionRegex
                .findAll(message)
                .map { it.groupValues[1] }
                .toSet()

            val mentionedRoleIds = roleMentionRegex
                .findAll(message)
                .map { it.groupValues[1] }
                .toSet()

            // user/role allowed after filtering block list
            val allowedUserIds = mentionedUserIds.filter { it !in blockedUserIds }
            val allowedRoleIds = mentionedRoleIds.filter { it !in blockedRoleIds }

            // --- Sanitize the message -----------------------------------
            // Replace blocked only (visible but harmless)
            var sanitizedMessage = message

            if (blockedUserIds.isNotEmpty()) {
                val blockedUserPattern = Regex("<@!?(${blockedUserIds.joinToString("|")})>")
                sanitizedMessage = sanitizedMessage.replace(blockedUserPattern, "@\u200Buser")
            }

            if (blockedRoleIds.isNotEmpty()) {
                val blockedRolePattern = Regex("<@&(${blockedRoleIds.joinToString("|")})>")
                sanitizedMessage = sanitizedMessage.replace(blockedRolePattern, "@\u200Brole")
            }

            // --- Build Discord message ----------------------------------
            val channel = jda?.getTextChannelById(channelId) ?: return@FT
            val action = channel.sendMessage(sanitizedMessage)

            if (blockAll) {
                // Completely block mentions
                action.setAllowedMentions(emptySet())
            } else {
                // Allow mentions except blocked ones
                action.setAllowedMentions(
                    listOf(
                        Message.MentionType.USER,
                        Message.MentionType.ROLE
                    )
                )
                action.mentionUsers(*allowedUserIds.toTypedArray())
                action.mentionRoles(*allowedRoleIds.toTypedArray())
            }

            action.queue()
        }
    }

    fun sendToDiscordConsole(message: String) {
        ConsoleLogBufferFlusher.enqueue(message)
    }

    private fun setupWebhook() {
        val textChannel = jda?.getTextChannelById(Config.logChannelID ?: return)
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
    }

    private fun activity(): Activity? {
        val activityMessage = Config.botActivityMessage ?: return null
        val activityStatus = Config.botActivityStatus?.lowercase() ?: return null

        return when (activityStatus) {
            "playing" -> Activity.playing(activityMessage)
            "listening" -> Activity.listening(activityMessage)
            "watching" -> Activity.watching(activityMessage)
            "competing" -> Activity.competing(activityMessage)
            else -> null
        }
    }

    private fun onlineStatus(): OnlineStatus? {
        val onlineStatus = Config.botOnlineStatus?.lowercase() ?: return null

        return when (onlineStatus) {
            "online" -> OnlineStatus.ONLINE
            "idle" -> OnlineStatus.IDLE
            "dnd" -> OnlineStatus.DO_NOT_DISTURB
            "invisible" -> OnlineStatus.INVISIBLE
            else -> null
        }
    }
}