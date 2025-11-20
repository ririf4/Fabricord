package net.ririfa.fabricord.discord

import net.ririfa.fabricord.Config
import net.ririfa.fabricord.FT
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

object ConsoleLogBufferFlusher {
    private val logQueue = ConcurrentLinkedQueue<String>()

    fun startFlusher() {
        FT(delay = 0, period = 5000, unit = TimeUnit.MILLISECONDS, newThread = true) {
            flush()
        }
    }

    fun enqueue(message: String) = logQueue.add(message)

    fun flush() {
        if (logQueue.isEmpty()) return
        val jda = DiscordBotManager.jda ?: return
        val channelID = Config.consoleLogChannelID ?: return
        val channel = jda.getTextChannelById(channelID)

        val messages = mutableListOf<String>()

        while (true) {
            val msg = logQueue.poll() ?: break
            messages.add(msg)
        }

        val combinedMessage = messages.joinToString("\n")

        channel?.sendMessage(combinedMessage)
            ?.setAllowedMentions(emptySet())
            ?.queue()
    }
}