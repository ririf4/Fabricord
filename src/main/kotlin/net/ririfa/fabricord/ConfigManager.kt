package net.ririfa.fabricord

import net.ririfa.fabricord.util.SendableEvent
import net.ririfa.yacla.Yacla
import net.ririfa.yacla.annotation.CustomLoader
import net.ririfa.yacla.annotation.Default
import net.ririfa.yacla.annotation.IfNullEvenRequired
import net.ririfa.yacla.annotation.Required
import net.ririfa.yacla.defaults.DefaultHandlers
import net.ririfa.yacla.loader.ConfigLoader
import net.ririfa.yacla.loader.ErrorHandlerWith
import net.ririfa.yacla.loader.FieldLoader
import net.ririfa.yacla.yaml.YamlParser
import java.nio.file.Files
import java.nio.file.Path

object ConfigManager {
    lateinit var loader: ConfigLoader<Config>
    lateinit var config: Config
    var isErrorOccurred = false

    private val configFile: Path = ModDir.resolve("config.yml")

    fun init() {
        createDirectoryIfNeeded()
        registerDefaultHandlers()
        loadConfig()
    }

    private fun createDirectoryIfNeeded() {
        if (!Files.exists(ModDir)) {
            Files.createDirectories(ModDir)
        }
    }

    private fun registerDefaultHandlers() {
        DefaultHandlers.register(Set::class.java) { raw, _ ->
            if (raw == "toEmptySet") emptySet<Any>()
            else throw IllegalArgumentException("Unsupported default value '$raw' for Set")
        }

        DefaultHandlers.register(List::class.java) { raw, _ ->
            if (raw == "toEmptyList") emptyList<Any>()
            else throw IllegalArgumentException("Unsupported default value '$raw' for List")
        }
    }

    private fun loadConfig() {
        try {
            loader = Yacla.loader<Config>()
                .fromResource("/assets/fabricord/config.yml")
                .toFile(configFile)
                .parser(YamlParser())
                .autoUpdateIfOutdated(true)
                .load()
                .also {
                    it.validate()
                    it.nullCheck()
                }

            config = loader.config
        } catch (e: Exception) {
            Logger.error("Failed to initialize config: ${e.message}", e)
            isErrorOccurred = true
        }
    }

    // >================================================< \\
    data class Config(
        @Required
        @JvmField
        val botToken: String?,
        @Required(soft = true)
        @JvmField
        @IfNullEvenRequired(handler = LogChannelIDNullHandler::class)
        var logChannelID: String?,

        @JvmField
        @Default("toEmptyList")
        @CustomLoader(loader = SendableEventListLoader::class)
        var willSends: List<SendableEvent>,
        @JvmField
        @Default("Minecraft")
        var botActivityMessage: String,
        @JvmField
        @Default("playing")
        var botActivityStatus: String,
        @JvmField
        @Default("online")
        var botOnlineStatus: String,
        @JvmField
        @Default("classic")
        var messageStyle: String,

        @JvmField
        var serverStartMessage: String,
        @JvmField
        var serverStopMessage: String,
        @JvmField
        var playerJoinMessage: String,
        @JvmField
        var playerLeaveMessage: String,

        @JvmField
        @Default("false")
        var useUserPermissionForMention: Boolean,
        @JvmField
        @Default("false")
        var allowMentions: Boolean,
        @JvmField
        @Default("toEmptySet")
        var mentionBlockedUserID: Set<String>,
        @JvmField
        @Default("toEmptySet")
        var mentionBlockedRoleID: Set<String>,

        @JvmField
        @Default("false")
        var enableConsoleLog: Boolean,
        @JvmField
        var consoleLogChannelID: String?,
    ) {
        /**
         * If this true, [net.ririfa.fabricord.discord.DiscordBotManager.sendToDiscord] will not do anything
         */
        @JvmField
        var isLogChannelIDNotSet = false

        fun sendChat(): Boolean = willSends.contains(SendableEvent.Chat)
    }

    class LogChannelIDNullHandler : ErrorHandlerWith {
        override fun handle(
            fieldValue: Any?
        ) {
            config.isLogChannelIDNotSet = true
        }
    }

    class SendableEventListLoader : FieldLoader {
        override fun load(raw: Any?): Any {
            val rawList = raw as? List<*>
                ?: throw IllegalArgumentException("Expected a List for SendableEvent, got: ${raw?.javaClass?.name}")

            return rawList.map {
                val str = (it as? String)?.trim()
                    ?: throw IllegalArgumentException("Expected string elements in SendableEvent list")

                try {
                    SendableEvent.valueOf(str)
                } catch (e: IllegalArgumentException) {
                    throw IllegalArgumentException("Invalid SendableEvent: '$str'", e)
                }
            }
        }
    }
}
