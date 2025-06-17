package net.ririfa.fabricord

import net.fabricmc.loader.api.FabricLoader
import net.ririfa.fabricord.util.SendableEvent
import net.ririfa.yacla.Yacla.loader
import net.ririfa.yacla.defaults.DefaultHandlers
import net.ririfa.yacla.yaml.YamlParser
import java.nio.file.Files
import java.nio.file.Path

/**
 * Manages loading and access to the Fabricord configuration via a safe lazy property.
 */
object ConfigManager {
    private val configFile: Path =
        FabricLoader.getInstance().configDir.resolve("config.yml")

    @JvmStatic
    var isErrorOccurred: Boolean = false
        private set

    @JvmStatic
    val isLogChannelIDNotSet: Boolean
        get() = _config.logChannelID.isNullOrBlank()

    // The configuration is loaded once, on first access, and errors propagate.
    private val _config: Config by lazy {
        try {

            // Ensure config directory exists
            val dir = configFile.parent
            if (!Files.exists(dir)) Files.createDirectories(dir)

            // Register default handlers before loading
            registerDefaultHandlers()

            // Build and load the config
            val cfgLoader = loader<Config>()
                .fromResource("/assets/fabricord/config.yml")
                .toFile(configFile)
                .parser(YamlParser())
                .autoUpdateIfOutdated(true)
                .load()
                .also {
                    it.validate()
                    it.nullCheck()
                }
            cfgLoader.config
        } catch (e: Exception) {
            Logger.debug("Failed to load config.yml", e)
            Config()
        }
    }

    /**
     * Call at server startup to eagerly load and validate the configuration.
     * Will throw on any loading or validation error.
     */
    fun init() {
        // Force evaluation now so failures surface immediately
        try {
            _config
        } catch (e: Exception) {
            isErrorOccurred = true
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Returns the loaded configuration. init() should be called first.
     */
    fun getConfig(): Config = _config

    /**
     * Registers any custom default handlers for Yacla.
     */
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

    /**
     * The data structure for Fabricord's configuration
     */
    data class Config(
        val botToken: String? = "",
        val logChannelID: String? = "",
        val willSends: List<SendableEvent> = emptyList(),
        val botActivityMessage: String = "Minecraft",
        val botActivityStatus: String = "playing",
        val botOnlineStatus: String = "online",
        val messageStyle: String = "classic",
        val serverStartMessage: String = "Server has started!",
        val serverStopMessage: String = "Server has stopped!",
        val playerJoinMessage: String = "%player% joined the game",
        val playerLeaveMessage: String = "%player% left the game",
        val useUserPermissionForMention: Boolean = false,
        val allowMentions: Boolean = true,
        val mentionBlockedUserID: Set<String> = emptySet(),
        val mentionBlockedRoleID: Set<String> = emptySet(),
        val enableConsoleLog: Boolean = false,
        val consoleLogChannelID: String? = null,
    ) {
        /**
         * If this true, [net.ririfa.fabricord.discord.DiscordBotManager.sendToDiscord] will not do anything
         */
        fun sendChat(): Boolean = willSends.contains(SendableEvent.Chat)
    }

}