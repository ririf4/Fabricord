package net.ririfa.fabricord

import net.ririfa.fabricord.util.SendableEvent
import net.ririfa.yacla.Yacla
import net.ririfa.yacla.annotation.CustomLoader
import net.ririfa.yacla.annotation.Default
import net.ririfa.yacla.annotation.IfNullEvenRequired
import net.ririfa.yacla.annotation.Required
import net.ririfa.yacla.loader.ConfigLoaderBuilder
import net.ririfa.yacla.loader.ErrorHandlerWith
import net.ririfa.yacla.loader.FieldLoader
import net.ririfa.yacla.logger.impl.SLF4JYaclaLogger
import net.ririfa.yacla.yaml.YamlParser
import java.nio.file.Files
import java.nio.file.Path

object ConfigManager {
    private val configFile: Path =
        ModDir.resolve("config.yml")

    val loader: ConfigLoaderBuilder<Config> by lazy {
        Yacla.loader<Config>()
            .fromResource("/assets/fabricord/config.yml")
            .toFile(configFile)
            .parser(YamlParser())
            .autoUpdateIfOutdated(true)
            .withLogger(SLF4JYaclaLogger)
    }

    val config: Config by lazy {
        try {
            if (!Files.exists(ModDir)) {
                Files.createDirectories(ModDir)
            }

            loader.load().also { it.validate() }.config
        } catch (e: Exception) {
            Logger.error("Failed to initialize config: ${e.message}", e)
            isErrorOccurred = true

            Config(
                botToken = "dummy",
                logChannelID = null,
                willSends = emptyList(),
                botActivityMessage = "Minecraft",
                botActivityStatus = "playing",
                botOnlineStatus = "online",
                messageStyle = "classic",
                serverStartMessage = ":white_check_mark: **Server has started!**",
                serverStopMessage = ":octagonal_sign: **Server has stopped!**",
                playerJoinMessage = "%player% joined the server",
                playerLeaveMessage = "%player% left the server",
                useUserPermissionForMention = false,
                allowMentions = false,
                mentionBlockedUserID = emptySet(),
                mentionBlockedRoleID = emptySet(),
                enableConsoleLog = false,
                consoleLogChannelID = null
            ).apply {
                isLogChannelIDNotSet = true
            }
        }
    }

    var isErrorOccurred = false

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
        @CustomLoader(loader = SendableEventListLoader::class)
        var willSends: List<SendableEvent>?,
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
        @Default(":white_check_mark: **Server has started!**")
        var serverStartMessage: String,
        @JvmField
        @Default(":octagonal_sign: **Server has stopped!**")
        var serverStopMessage: String,
        @JvmField
        @Default("%player% joined the server")
        var playerJoinMessage: String,
        @JvmField
        @Default("%player% left the server")
        var playerLeaveMessage: String,

        @JvmField
        @Default("false")
        var useUserPermissionForMention: Boolean,
        @JvmField
        @Default("false")
        var allowMentions: Boolean,
        @JvmField
        @CustomLoader(loader = ListToSetLoader::class)
        var mentionBlockedUserID: Set<String>,
        @JvmField
        @CustomLoader(loader = ListToSetLoader::class)
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

        fun sendChat(): Boolean = willSends?.contains(SendableEvent.Chat) == true
    }

    class LogChannelIDNullHandler : ErrorHandlerWith {
        override fun handle(fieldValue: Any?, config: Any?) {
            (config as? Config)?.let { cfg ->
                cfg.isLogChannelIDNotSet = true
            }
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

    class ListToSetLoader : FieldLoader {
        override fun load(raw: Any?): Any {
            return when (raw) {
                is Collection<*> -> raw.toSet()
                null -> emptySet<Any>()
                else -> error("Expected a list for conversion to set, got: $raw")
            }
        }
    }
}
