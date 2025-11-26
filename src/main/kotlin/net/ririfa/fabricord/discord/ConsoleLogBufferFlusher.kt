package net.ririfa.fabricord.discord

import net.ririfa.fabricord.util.Config
import net.ririfa.fabricord.util.FT
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Buffers console log messages and periodically flushes them to Discord.
 * Large output is automatically chunked to satisfy Discord's 2000-char limit.
 */
object ConsoleLogBufferFlusher {
    private const val DISCORD_LIMIT = 2000
    private val logQueue = ConcurrentLinkedQueue<String>()

    fun startFlusher() {
        FT(
            delay = 0,
            period = 5000,
            newThread = true
        ) {
            runCatching { flush() }.onFailure { it.printStackTrace() }
        }
    }

    fun enqueue(message: String) {
        logQueue.add(message)
    }

    /**
     * Flushes queued log messages to Discord.
     * Messages are batched and chunked to fit Discord's message size limits.
     */
    private fun flush() {
        if (logQueue.isEmpty()) return

        val jda = DiscordBotManager.jda
        val channelId = Config.consoleLogChannelID ?: return
        val channel = jda.getTextChannelById(channelId) ?: return

        // Gather current queue snapshot without holding the queue for too long
        val messages = mutableListOf<String>()
        while (true) {
            val msg = logQueue.poll() ?: break
            messages.add(msg)
        }
        if (messages.isEmpty()) return

        // Build chunks that stay under Discord's 2000-char limit
        var current = StringBuilder()
        val chunks = mutableListOf<String>()

        for (line in messages) {
            if (current.length + line.length + 1 > DISCORD_LIMIT) {
                chunks.add(current.toString())
                current = StringBuilder()
            }
            if (current.isNotEmpty()) current.append('\n')
            current.append(line)
        }
        if (current.isNotEmpty()) chunks.add(current.toString())

        // Send all chunks sequentially
        for (chunk in chunks) {
            channel.sendMessage(chunk)
                .setAllowedMentions(emptySet())
                .queue()
        }
    }
}
