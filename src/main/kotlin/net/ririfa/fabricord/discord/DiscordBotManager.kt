package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import net.ririfa.fabricord.Config

object DiscordBotManager {
    lateinit var jda: JDA

    private val intents = GatewayIntent.MESSAGE_CONTENT

    fun strat() {
        jda = JDABuilder.createDefault(Config.botToken)
            .setAutoReconnect(true)
            .addEventListeners(CompositeDiscordListener())
            .enableIntents(intents)
            .build()

        jda.awaitReady()

    }
}