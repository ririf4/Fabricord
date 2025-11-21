package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Webhook
import net.dv8tion.jda.api.requests.GatewayIntent
import net.ririfa.fabricord.Config
import net.ririfa.fabricord.FT
import net.ririfa.fabricord.Logger
import java.time.Duration

object DiscordBotManager {
    lateinit var jda: JDA

    private val intents = GatewayIntent.MESSAGE_CONTENT

    private var webHook: Webhook? = null

    fun strat() {
        FT {
            jda = JDABuilder.createDefault(Config.botToken)
                .setAutoReconnect(true)
                .addEventListeners(CompositeDiscordListener())
                .enableIntents(intents)
                .build()
                .awaitReady()

            setupWebhook()
        }
    }

    fun stop() {
        jda.awaitShutdown(Duration.ofSeconds(15L))
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
            val channel = jda.getTextChannelById(channelId) ?: return@FT
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

    private fun setupWebhook() {
        val textChannel = jda.getTextChannelById(Config.logChannelID ?: return)
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
}