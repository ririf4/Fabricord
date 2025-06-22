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
import net.ririfa.langman.LangMan
import net.ririfa.langman.LangManBuilder
import net.ririfa.langman.TextFactory
import net.ririfa.langman.ext.yaml.YamlFileLoader
import net.ririfa.shaded.snakeyaml.org_yaml_snakeyaml.Yaml
import org.apache.logging.log4j.LogManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

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

        val thread: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
        val availableLang = listOf<String>("en", "ja")
    }

    override fun onInitializeServer() {
        langMan = LangManBuilder.new<FabricordMessageProvider, Text>()
            .fromResource("/assets/$MOD_ID/lang/")
            .toPath(langDir)
            .withMessageKey(FabricordMessageKey::class.java)
            .withType(YamlFileLoader { inputStream -> Yaml().load(inputStream) })
            .registerTextFactory(object : TextFactory<Text> {
                override val clazz: Class<Text>
                    get() = Text::class.java

                override fun invoke(text: String): Text = Text.literal(text)
            })
            .withLanguage(availableLang)
            .autoUpdateIfNeeded(true)
            .debug(true)
            .build()
        ConfigManager
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
                        Logger.debug("JOIN: join embeds sent for {}", player.name.string)
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
        } else {
            Logger.debug("registerServerEvents: logChannelID is not set, skipping JOIN and DISCONNECT event registration")
        }

        Logger.debug("registerServerEvents: finished")
    }
}