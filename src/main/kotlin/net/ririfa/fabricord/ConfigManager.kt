package net.ririfa.fabricord

import net.ririfa.yacla.Yacla
import net.ririfa.yacla.annotation.Default
import net.ririfa.yacla.annotation.IfNullEvenRequired
import net.ririfa.yacla.annotation.Required
import net.ririfa.yacla.defaults.DefaultHandlers
import net.ririfa.yacla.loader.ConfigLoader
import net.ririfa.yacla.loader.ErrorHandlerWith
import net.ririfa.yacla.loader.util.ContextType
import net.ririfa.yacla.yaml.YamlParser
import org.jetbrains.annotations.Nullable
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
    }

    private fun loadConfig() {
        try {
            loader = Yacla.fileLoader<Config>()
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
        @Nullable
        @JvmField
        @IfNullEvenRequired(handler = LogChannelIDNullHandler::class)
        var logChannelID: String?,

        @JvmField
        @Default("false")
        var dontSendChatToDiscord: Boolean,
        @JvmField
        @Default("false")
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
    }

    class LogChannelIDNullHandler : ErrorHandlerWith<Any?> {
        override fun handle(
            fieldValue: Any?,
            configInstance: Any,
            context: Any?,
            contextType: ContextType
        ) {
            if (configInstance is Config) {
                configInstance.isLogChannelIDNotSet = true
            }
        }
    }
}
