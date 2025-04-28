package net.ririfa.fabricord

import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.ririfa.fabricord.discord.DiscordBotManager
import net.ririfa.fabricord.discord.DiscordEmbed
import net.ririfa.fabricord.translation.FabricordMessageKey
import net.ririfa.fabricord.translation.FabricordMessageProvider
import net.ririfa.fabricord.util.isOlderVersion
import net.ririfa.langman.InitType
import net.ririfa.langman.LangMan
import org.apache.logging.log4j.LogManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import kotlin.use

class Fabricord : DedicatedServerModInitializer {
    companion object {
        const val MOD_ID = "fabricord"

        lateinit var server: MinecraftServer
        lateinit var langMan: LangMan<FabricordMessageProvider, Text>
        var consoleAppender: ConsoleTrackerAppender? = null

        val logger: Logger
            get() = LoggerFactory.getLogger(Fabricord::class.simpleName)
        val loader: FabricLoader = FabricLoader.getInstance()
        val serverDir: Path = loader.gameDir
        val modDir: Path = serverDir.resolve(MOD_ID)
        val langDir: Path = modDir.resolve("lang")
        val logDir: Path = modDir.resolve("logs")

        val thread: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
        val availableLang = listOf<String>("en", "ja")
    }

    override fun onInitializeServer() {
        if (Files.notExists(logDir)) Files.createDirectories(logDir)
        LanguageAutoUpdater.checkForUpdatesAndExtract()
        langMan = LangMan.createNew(
            { Text.of(it) },
            FabricordMessageKey::class,
            false
        )
        langMan.init(
            InitType.YAML,
            langDir.toFile(),
            availableLang
        )
        ConfigManager.init()
        registerServerEvents()
    }

    private fun registerServerEvents() {
        Logger.debug("registerServerEvents: start")

        if (Config.enableConsoleLog == true && Config.consoleLogChannelID != null) {
            Logger.debug("registerServerEvents: enabling ConsoleTrackerAppender (console log enabled, channel ID set)")
            consoleAppender = ConsoleTrackerAppender("FabricordConsoleTracker")
            val rootLogger = LogManager.getRootLogger() as org.apache.logging.log4j.core.Logger
            rootLogger.addAppender(consoleAppender!!)
            Logger.debug("registerServerEvents: ConsoleTrackerAppender attached")
        } else {
            Logger.debug("registerServerEvents: console log not enabled or channel ID not set, skipping ConsoleTrackerAppender")
        }

        Logger.debug("registerServerEvents: registering commands")
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            Logger.debug("registerServerEvents: CommandRegistrationCallback triggered, registering commands")
            CommandManager.registerAll(dispatcher)
            Logger.debug("registerServerEvents: Command registration completed")
        }

        Logger.debug("registerServerEvents: registering SERVER_STARTED event")
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            Logger.debug("SERVER_STARTED: server instance received")
            Fabricord.server = server
            if (!(ConfigManager.isErrorOccurred)) {
                Logger.debug("SERVER_STARTED: ConfigManager reports no error, starting Discord bot")
                DiscordBotManager.start()
            } else {
                Logger.warn("SERVER_STARTED: ConfigManager reported an error, skipping Discord bot start")
            }
            DiscordEmbed.init()
            Logger.debug("SERVER_STARTED: DiscordEmbed initialized")
        }

        Logger.debug("registerServerEvents: registering SERVER_STOPPING event")
        ServerLifecycleEvents.SERVER_STOPPING.register { server ->
            Logger.debug("SERVER_STOPPING: shutting down Discord bot (if initialized)")
            if (DiscordBotManager.isBotInitialized) {
                DiscordBotManager.stop()
                Logger.debug("SERVER_STOPPING: Discord bot stopped")
            } else {
                Logger.debug("SERVER_STOPPING: Discord bot was not initialized, skipping stop")
            }
            thread.shutdown()
            Logger.debug("SERVER_STOPPING: ScheduledExecutorService shut down")
        }

        if (!Config.isLogChannelIDNotSet) {
            Logger.debug("registerServerEvents: logChannelID is set, registering JOIN and DISCONNECT events")
            ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
                val player = handler.player
                Logger.debug("JOIN: player {} ({}) joined", player.name.string, player.uuid)

                if (DiscordBotManager.isBotInitialized) {
                    Logger.debug("JOIN: Discord bot initialized, sending join embed")
                    FT {
                        DiscordEmbed.sendPlayerJoinEmbed(player)
                        Logger.debug("JOIN: join embed sent for ${player.name.string}")
                    }
                } else {
                    Logger.debug("JOIN: Discord bot not initialized, skipping join embed")
                }
            }

            ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
                val player = handler.player
                Logger.debug("DISCONNECT: player {} ({}) disconnected", player.name.string, player.uuid)

                if (DiscordBotManager.isBotInitialized) {
                    Logger.debug("DISCONNECT: Discord bot initialized, sending leave embed")
                    FT {
                        DiscordEmbed.sendPlayerLeftEmbed(player)
                        Logger.debug("DISCONNECT: leave embed sent for ${player.name.string}")
                    }
                } else {
                    Logger.debug("DISCONNECT: Discord bot not initialized, skipping leave embed")
                }
            }

            // Replaced by control in Mixin.
//            ServerMessageEvents.CHAT_MESSAGE.register { message, sender, params ->
//                if (DiscordBotManager.isBotInitialized && Config.dontSendChatToDiscord == false) {
//                    val uuid = sender.uuid
//
//                    if (uuid in localChatToggled) return@register
//
//                    val content = message.content.string
//                    handleMCMessage(sender, content)
//                }
//            }
        } else {
            Logger.debug("registerServerEvents: logChannelID is not set, skipping JOIN and DISCONNECT event registration")
        }

        Logger.debug("registerServerEvents: finished")
    }

    object LanguageAutoUpdater {
        private val yaml = Yaml()
        private const val DEFAULT_VERSION = "1.0.0"

        fun checkForUpdatesAndExtract() {
            Logger.debug("LanguageAutoUpdater: Starting language update check")
            try {
                if (!Files.exists(langDir)) {
                    Logger.debug("LanguageAutoUpdater: lang directory does not exist, creating and extracting all")
                    Files.createDirectories(langDir)
                    extractLangFiles(langDir)
                    return
                }

                val latestVersions = getLatestVersionsFromJar()
                if (latestVersions == null) {
                    Logger.warn("LanguageAutoUpdater: latestVersions not found (langversion.info missing?), skipping update check")
                    return
                }

                Logger.debug("LanguageAutoUpdater: latestVersions = {}", latestVersions)

                val needsUpdate = Files.list(langDir).use { files ->
                    files.toList().filter { it.toString().endsWith(".yml") }.any { file ->
                        val langKey = file.fileName.toString().removeSuffix(".yml")
                        val latestVersion = latestVersions[langKey] ?: DEFAULT_VERSION
                        val currentVersion = getVersionFromYaml(file) ?: DEFAULT_VERSION
                        Logger.debug("LanguageAutoUpdater: checking $langKey → current=$currentVersion, latest=$latestVersion")
                        isOlderVersion(currentVersion, latestVersion)
                    }
                }

                if (needsUpdate) {
                    Logger.info("LanguageAutoUpdater: update needed, extracting language files")
                    extractLangFiles(langDir)
                } else {
                    Logger.debug("LanguageAutoUpdater: all language files up to date, no extraction needed")
                }
            } catch (e: Exception) {
                Logger.error("LanguageAutoUpdater: Failed to check for language file updates", e)
            }
        }

        private fun extractLangFiles(targetDir: Path) {
            Logger.debug("LanguageAutoUpdater: extracting language files to {}", targetDir)
            try {
                val langPath = "assets/${MOD_ID}/lang/"
                val classLoader = Fabricord::class.java.classLoader

                availableLang.forEach { lang ->
                    val fileName = "$lang.yml"
                    val fullPath = "$langPath$fileName"

                    val inputStream: InputStream? = classLoader.getResourceAsStream(fullPath)
                        ?: run {
                            val fallbackPath = Path.of("build/resources/main/$fullPath")
                            if (Files.exists(fallbackPath)) {
                                Logger.warn("LanguageAutoUpdater: Using fallback language file: $fallbackPath")
                                Files.newInputStream(fallbackPath)
                            } else {
                                Logger.warn("LanguageAutoUpdater: Language file not found: $fullPath (also missing in build/resources/main)")
                                null
                            }
                        }

                    if (inputStream == null) {
                        Logger.debug("LanguageAutoUpdater: Skipping $fileName (no available source)")
                        return@forEach
                    }

                    val targetFile = targetDir.resolve(fileName)
                    inputStream.use { input ->
                        Files.copy(input, targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                    }
                    Logger.info("LanguageAutoUpdater: Extracted language file: $fileName")
                }
            } catch (e: Exception) {
                Logger.error("LanguageAutoUpdater: Failed to extract language files", e)
            }
        }

        private fun getVersionFromYaml(file: Path): String? {
            return try {
                Files.newBufferedReader(file).use { reader ->
                    val data = yaml.load<Map<String, Any>>(reader)
                    val version = data["version"] as? String
                    Logger.debug("LanguageAutoUpdater: read version from {} = {}", file.fileName, version)
                    version
                }
            } catch (e: Exception) {
                Logger.warn("LanguageAutoUpdater: Failed to read version from ${file.fileName}", e)
                null
            }
        }

        private fun getLatestVersionsFromJar(): Map<String, String>? {
            return try {
                val classLoader = this::class.java.classLoader
                val resourceUrl = classLoader.getResource("assets/${MOD_ID}/lang/langversion.info") ?: return null
                resourceUrl.openStream().use { inputStream ->
                    val data: Map<String, Any> = yaml.load(inputStream)
                    @Suppress("UNCHECKED_CAST")
                    val latest = data["latest"] as? Map<String, String>
                    Logger.debug("LanguageAutoUpdater: loaded langversion.info successfully")
                    latest
                }
            } catch (e: Exception) {
                Logger.error("LanguageAutoUpdater: Failed to read langversion.info", e)
                null
            }
        }
    }
}