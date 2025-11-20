package net.ririfa.fabricord

import net.ririfa.fabricord.util.SendableEvent
import net.ririfa.yacla.Yacla
import net.ririfa.yacla.loader.ConfigLoaderBuilder
import net.ririfa.yacla.loader.ErrorHandlerWith
import net.ririfa.yacla.loader.FieldLoader
import net.ririfa.yacla.logger.impl.SLF4JYaclaLogger
import net.ririfa.yacla.schema.FieldDefBuilder
import net.ririfa.yacla.schema.YaclaSchema
import net.ririfa.yacla.yaml.YamlParser
import java.nio.file.Files
import java.nio.file.Path

object ConfigManager {
    private val configFile: Path =
        ModDir.resolve("config.yml")

    val loader: ConfigLoaderBuilder<FConfig> by lazy {
        Yacla.loader<FConfig>()
            .fromResource("/assets/fabricord/config.yml")
            .toFile(configFile)
            .parser(YamlParser())
            .autoUpdateIfOutdated(true)
            .withLogger(SLF4JYaclaLogger)
    }

    val config: FConfig by lazy {
        try {
            if (!Files.exists(ModDir)) {
                Files.createDirectories(ModDir)
            }

            loader.load().also { it.validate() }.config
        } catch (e: Exception) {
            Logger.error("Failed to initialize config: ${e.message}", e)
            isErrorOccurred = true

            FConfig(
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
    data class FConfig(
        @JvmField
        val botToken: String?,
        @JvmField
        var logChannelID: String?,

        @JvmField
        var willSends: List<SendableEvent>?,
        @JvmField
        var botActivityMessage: String,
        @JvmField
        var botActivityStatus: String,
        @JvmField
        var botOnlineStatus: String,
        @JvmField
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
        var useUserPermissionForMention: Boolean,
        @JvmField
        var allowMentions: Boolean,
        @JvmField
        var mentionBlockedUserID: Set<String>,
        @JvmField
        var mentionBlockedRoleID: Set<String>,

        @JvmField
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

    object ConfigSchema : YaclaSchema<FConfig> {
        override fun configure(def: FieldDefBuilder<FConfig>) {
            def.field(FConfig::botToken) {
                required()
            }
            def.field(FConfig::logChannelID) {
                required(soft = true)
                ifNull(LogChannelIDNullHandler::class)
            }
            def.field(FConfig::willSends) {
                loader(SendableEventListLoader())
            }
            def.field(FConfig::mentionBlockedUserID) {
                loader(ListToSetLoader())
            }
            def.field(FConfig::mentionBlockedRoleID) {
                loader(ListToSetLoader())
            }
            def.field(FConfig::botActivityMessage) {
                default("Minecraft")
            }
            def.field(FConfig::botActivityStatus) {
                default("playing")
            }
            def.field(FConfig::botOnlineStatus) {
                default("online")
            }
            def.field(FConfig::messageStyle) {
                default("classic")
            }
            def.field(FConfig::serverStartMessage) {
                default(":white_check_mark: **Server has started!**")
            }
            def.field(FConfig::serverStopMessage) {
                default(":octagonal_sign: **Server has stopped!**")
            }
            def.field(FConfig::useUserPermissionForMention) {
                default(false)
            }
            def.field(FConfig::allowMentions) {
                default(false)
            }
        }
    }

    class LogChannelIDNullHandler : ErrorHandlerWith {
        override fun handle(fieldValue: Any?, config: Any?) {
            (config as? FConfig)?.let { cfg ->
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