package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Message.MentionType
import net.dv8tion.jda.api.entities.Webhook
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent
import net.ririfa.fabricord.Config
import net.ririfa.fabricord.FT
import net.ririfa.fabricord.LM
import net.ririfa.fabricord.Logger
import net.ririfa.fabricord.translation.FabricordMessageKey
import net.ririfa.fabricord.util.error
import net.ririfa.fabricord.util.warn
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.security.auth.login.LoginException

object DiscordBotManager {
    var jda: JDA? = null
    var webHook: Webhook? = null

    @JvmField
    var isBotInitialized = false

    private val intents = GatewayIntent.MESSAGE_CONTENT

    fun start() {
        FT {
            try {
                Config.botToken?.let { tk ->
                    jda = JDABuilder.createDefault(tk)
                        .addEventListeners(CompositeDiscordListener())
                        .setStatus(onlineStatus())
                        .setActivity(activity())
                        .setAutoReconnect(true)
                        .enableIntents(intents)
                        .build()
                        .awaitReady()
                } ?: return@FT

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
        ConsoleLogBufferFlusher.enqueue(message)
    }
}