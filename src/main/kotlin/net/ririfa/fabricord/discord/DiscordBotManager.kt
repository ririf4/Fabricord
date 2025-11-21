package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Webhook
import net.dv8tion.jda.api.requests.GatewayIntent
import net.ririfa.fabricord.Config
import net.ririfa.fabricord.FT
import net.ririfa.fabricord.Logger

object DiscordBotManager {
    lateinit var jda: JDA

    private val intents = GatewayIntent.MESSAGE_CONTENT

    @PublishedApi
    internal var webHook: Webhook? = null

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