package net.ririfa.fabricord

import net.ririfa.fabricord.discord.DiscordBridge
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.layout.PatternLayout

object ConsoleRelay {
    private var appender: FabricordConsoleAppender? = null

    fun start() {
        if (appender != null || Fabricord.config.consoleLogChannelID == null) return
        val root = LogManager.getRootLogger() as? org.apache.logging.log4j.core.Logger ?: return
        appender = FabricordConsoleAppender().also(root::addAppender)
    }

    fun stop() {
        val active = appender ?: return
        val root = LogManager.getRootLogger() as? org.apache.logging.log4j.core.Logger
        root?.removeAppender(active)
        active.stop()
        appender = null
    }
}

private class FabricordConsoleAppender : AbstractAppender(
    "FabricordConsoleRelay",
    null,
    PatternLayout.createDefaultLayout(),
    false,
    emptyArray(),
) {
    init {
        start()
    }

    override fun append(event: LogEvent) {
        if (event.loggerName.startsWith("net.dv8tion.jda")) return
        if (event.level !in setOf(Level.INFO, Level.WARN, Level.ERROR, Level.FATAL)) return
        val message = event.message.formattedMessage
            .replace("```", "` ` `")
            .replace(Regex("[\r\n]+"), " ")
        DiscordBridge.enqueueConsole("[${event.level}] ${message.take(1_700)}")
    }
}
