package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Webhook
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.requests.GatewayIntent
import net.ririfa.fabricord.i18n.FMsgKey
import net.ririfa.fabricord.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
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
    var webHooks: MutableMap<String, Webhook> = mutableMapOf()

    fun start() {
        val token = Config.botToken ?: return
        FT {
            try {
                jda = JDABuilder.createDefault(token)
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
                    Commands.slash("status", "Get the status of the server"),
                    Commands.slash("link", "Link your Discord account to a Minecraft account"),
                    Commands.slash("kick", "Kick a player from the Minecraft server")
                        .addOptions(
                            OptionData(OptionType.STRING, "player", "The player to kick", true),
                            OptionData(OptionType.STRING, "reason", "The reason for kicking the player", false)
                        ),
                    Commands.slash("ban", "Ban a player from the Minecraft server")
                        .addOptions(
                            OptionData(OptionType.STRING, "player", "The player to ban", true),
                            OptionData(OptionType.STRING, "reason", "The reason for banning the player", false),
                            OptionData(OptionType.INTEGER, "expire_date", "When the ban will lifted (format: 20250101 for Jan 1, 2025)", false)
                        ),
                    Commands.slash("pardon", "Unban a player from the Minecraft server")
                        .addOptions(
                            OptionData(OptionType.STRING, "player", "The player to unban", true)
                        )
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

        try {
            val bot = jda
            if (bot != null) {
                val name = runCatching { bot.selfUser.name }.getOrNull() ?: "?"

                bot.shutdown()

                if (!bot.awaitShutdown(5000, TimeUnit.MILLISECONDS)) {
                    Logger.warn(LM.getMessage(FMsgKey.Discord.Bot.TimedOutForStoppingBot).string)
                    bot.shutdownNow()
                    bot.awaitShutdown(3, TimeUnit.SECONDS)
                }

                Logger.info(
                    LM.getMessage(FMsgKey.Discord.Bot.BotNowOffline, name).string
                )
            }
        } catch (e: Exception) {
            Logger.error(
                LM.getMessage(FMsgKey.Discord.Bot.CannotStopBot).string,
                e
            )
        } finally {
            shutdownExecutor.shutdown()
            shutdownExecutor.awaitTermination(3, TimeUnit.SECONDS)
        }
    }


    fun sendToDiscord(message: String) {
        val channelIds = Config.logChannelIDs ?: return

        FT {
            val blockAll = Config.blockAllMentions

            val blockedUserIds = Config.mentionBlockedUserIDs.orEmpty()
            val blockedRoleIds = Config.mentionBlockedRoleIDs.orEmpty()

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
            channelIds.forEach { channelId ->
                val channel = jda?.getTextChannelById(channelId) ?: return@forEach
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
    }

    fun sendToDiscordConsole(message: String) {
        ConsoleLogBufferFlusher.enqueue(message)
    }

    private fun setupWebhook() {
        Config.logChannelIDs?.forEach { channelId ->
            val textChannel = jda?.getTextChannelById(channelId) ?: return@forEach
            textChannel.retrieveWebhooks().queue({ webhooks ->
                val webHook = webhooks.firstOrNull { it.name == "Fabricord" }

                if (webHook == null) {
                    textChannel.createWebhook("Fabricord").queue { created ->
                        webHooks[channelId] = created
                        Logger.info("Created webhook: ${created.name} in channel $channelId")
                    }
                } else {
                    webHooks[channelId] = webHook
                    Logger.info("Using existing webhook: ${webHook.name} in channel $channelId")
                }
            }, { error ->
                Logger.warn("Could not retrieve webhook for channel $channelId: ${error.message}")
            })
        }
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