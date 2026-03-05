package net.ririfa.fabricord

import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.ririfa.fabricord.command.CommandManager
import net.ririfa.fabricord.config.ConfigManager
import net.ririfa.fabricord.config.SendableEvent
import net.ririfa.fabricord.database.DataBase
import net.ririfa.fabricord.discord.DiscordBotManager
import net.ririfa.fabricord.discord.DiscordEmbed
import net.ririfa.fabricord.i18n.FMsgKey
import net.ririfa.fabricord.i18n.FMsgProvider
import net.ririfa.fabricord.util.Config
import net.ririfa.fabricord.util.FT
import net.ririfa.fabricord.util.Logger
import net.ririfa.langman.LangMan
import net.ririfa.langman.LangManBuilder
import net.ririfa.langman.TextFactory
import net.ririfa.langman.ext.yaml.YamlFileLoader
import org.apache.logging.log4j.LogManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class Fabricord : DedicatedServerModInitializer {
    companion object {
        const val MOD_ID = "fabricord"

        lateinit var server: MinecraftServer
        lateinit var langMan: LangMan<FMsgProvider, Text>
        lateinit var consoleAppender: ConsoleTrackerAppender

        val logger: Logger
            get() = LoggerFactory.getLogger(Fabricord::class.simpleName)
        val loader: FabricLoader = FabricLoader.getInstance()
        val serverDir: Path = loader.gameDir
        val modDir: Path = serverDir.resolve(MOD_ID)
        val langDir: Path = modDir.resolve("lang")
        val dbDir: Path = modDir.resolve("db")
        val threads: Int = Runtime.getRuntime().availableProcessors()
        val thread: ScheduledExecutorService = Executors.newScheduledThreadPool(
            (threads * 0.3)
                .toInt()
                .coerceAtLeast(2)
                .coerceAtMost(8)
        ) { r ->
            Thread(r, "Fabricord-Worker").apply {
                isDaemon = true
            }
        }
        val availableLang: List<String> = listOf(
            "en",
            "ja"
        )
    }

    override fun onInitializeServer() {
        langMan = LangManBuilder.new<FMsgProvider, Text>()
            .fromClass(Fabricord::class.java)
            .fromResource("/assets/$MOD_ID/lang/")
            .toPath(langDir)
            .withMessageKey(FMsgKey::class.java)
            .withType(YamlFileLoader { inputStream -> Yaml().load(inputStream) })
            .registerTextFactory(textFactory)
            .withLanguage(availableLang)
            .autoUpdateIfNeeded(true)
            .debug(true)
            .build()

        registerServerEvents()
        DataBase.initialize()
    }

    private fun registerServerEvents() {
        if (Config.consoleLogChannelID != null) {
            consoleAppender = ConsoleTrackerAppender("FabricordConsoleTracker")
            val rootLogger = LogManager.getRootLogger() as? org.apache.logging.log4j.core.Logger
            rootLogger?.addAppender(consoleAppender)
        }

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            CommandManager.registerAll(dispatcher)
        }

        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            Fabricord.server = server
            if (!ConfigManager.isErrorOccurred) {
                DiscordBotManager.start()
                DiscordEmbed.init()
            } else {
                Logger.warn("SERVER_STARTED: ConfigManager reported an error, skipping Discord bot start")
            }
        }

        ServerLifecycleEvents.SERVER_STOPPING.register {
            if (Config.consoleLogChannelID != null && consoleAppender.isInitialized) {
                val rootLogger = LogManager.getRootLogger() as? org.apache.logging.log4j.core.Logger
                rootLogger?.removeAppender(consoleAppender)
                consoleAppender.stop()
            }
            if (DiscordBotManager.isBotInitialized) DiscordBotManager.stop()
        }

        ServerLifecycleEvents.SERVER_STOPPED.register {
            thread.shutdown()
            thread.awaitTermination(3, TimeUnit.SECONDS)
            DataBase.terminate()
        }

        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            val player = handler.player
            val isOp = server.playerManager.isOperator(player.playerConfigEntry)

            FT {
                DataBase.insertPlayer(player.uuid, isOp)
                if (Config.willSends?.contains(SendableEvent.Join) == true && Config.logChannels != null && DiscordBotManager.isBotInitialized) {
                    DiscordEmbed.sendPlayerJoinEmbed(player)
                }
            }
        }

        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            val player = handler.player

            FT {
                if (Config.willSends?.contains(SendableEvent.Leave) == true && Config.logChannels != null && DiscordBotManager.isBotInitialized) {
                    DiscordEmbed.sendPlayerLeftEmbed(player)
                }
            }
        }
    }

    private val textFactory = object : TextFactory<Text> {
        override val clazz: Class<Text>
            get() = Text::class.java

        override fun invoke(text: String): Text = Text.literal(text)
    }
}