@file:Suppress("unused")

package net.ririfa.fabricord

import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.MessageType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.MinecraftServer
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.ririfa.fabricord.command.FabricordCommands
import net.ririfa.fabricord.command.LocalChat
import net.ririfa.fabricord.config.ConfigManager
import net.ririfa.fabricord.config.FConfig
import net.ririfa.fabricord.config.SendableEvent
import net.ririfa.fabricord.database.AccountLinkRepository
import net.ririfa.fabricord.discord.DiscordBridge
import net.ririfa.fabricord.discord.DiscordEmbeds
import net.ririfa.fabricord.discord.MinecraftDiscordPlatform
import net.ririfa.fabricord.discord.OpSync
import net.ririfa.fabricord.i18n.FMsgKey
import net.ririfa.fabricord.i18n.FMsgProvider
import net.ririfa.fabricord.i18n.adapt
import net.ririfa.fabricord.link.LinkCodeManager
import net.ririfa.langman.LangMan
import net.ririfa.langman.LangManBuilder
import net.ririfa.langman.TextFactory
import net.ririfa.langman.ext.yaml.YamlFileLoader
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.yaml.snakeyaml.Yaml
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

val Logger: Logger
    get() = Fabricord.logger

class Fabricord : DedicatedServerModInitializer {
    companion object {
        const val MOD_ID = "fabricord"

        val logger: Logger = LogManager.getLogger(Fabricord::class.java)
        val loader: FabricLoader = FabricLoader.getInstance()
        val serverDir: Path = loader.gameDir
        val modDir: Path = serverDir.resolve(MOD_ID)
        val langDir: Path = modDir.resolve("lang")
        val dbDir: Path = modDir.resolve("db")

        val configManager = ConfigManager(modDir)
        val config: FConfig
            get() = configManager.config
        val accountLinks = AccountLinkRepository(dbDir.resolve("fabricord.db"))
        val linkCodes = LinkCodeManager()
        val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(2) { task ->
            Thread(task, "Fabricord-Worker").apply { isDaemon = true }
        }

        lateinit var server: MinecraftServer
            private set
        lateinit var langMan: LangMan<FMsgProvider, Text>
            private set

        @Volatile
        var serverStartTime: Long = 0L
            private set

        fun handleChat(player: ServerPlayerEntity, message: String): Boolean {
            if (
                DiscordBridge.isRunning &&
                config.sendChat() &&
                config.useUserPermissionForMentions &&
                !accountLinks.isLinked(player.uuid)
            ) {
                player.sendChatMessage(FMsgKey.Chat.LinkDiscordAccountFirst.t(player.adapt()), MessageType.SYSTEM)
                return false
            }
            if (config.sendChat() && player.uuid !in LocalChat.players) {
                DiscordBridge.sendMinecraftChat(player.uuid, player.name.asString(), message)
            }
            return true
        }
    }

    override fun onInitializeServer() {
        langMan = LangManBuilder.new<FMsgProvider, Text>()
            .fromClass(Fabricord::class.java)
            .fromResource("/assets/$MOD_ID/lang/")
            .toPath(langDir)
            .withMessageKey(FMsgKey::class.java)
            .withType(YamlFileLoader { input -> Yaml().load(input) })
            .registerTextFactory(textFactory)
            .withLanguage(listOf("en", "ja", "de", "fr"))
            .autoUpdateIfNeeded(true)
            .build()

        config
        accountLinks.initialize()
        DiscordBridge.installPlatform(MinecraftDiscordPlatform)
        registerEvents()
    }

    private fun registerEvents() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            FabricordCommands.register(dispatcher)
        }

        ServerLifecycleEvents.SERVER_STARTED.register { runningServer ->
            server = runningServer
            serverStartTime = System.currentTimeMillis()
            if (!configManager.isErrorOccurred && config.botToken != null) {
                DiscordBridge.start()
                ConsoleRelay.start()
            }
        }

        ServerLifecycleEvents.SERVER_STOPPING.register {
            ConsoleRelay.stop()
            DiscordBridge.stop()
        }

        ServerLifecycleEvents.SERVER_STOPPED.register {
            accountLinks.close()
            executor.shutdown()
            executor.awaitTermination(3, TimeUnit.SECONDS)
        }

        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            val player = handler.player
            if (config.willSends?.contains(SendableEvent.Join) == true) {
                DiscordEmbeds.sendJoin(player)
            }
            OpSync.syncOnJoin(player.uuid)
        }

        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            if (config.willSends?.contains(SendableEvent.Leave) == true) {
                DiscordEmbeds.sendLeave(handler.player)
            }
        }

    }

    private val textFactory = object : TextFactory<Text> {
        override val clazz: Class<Text> = Text::class.java
        override fun invoke(text: String): Text = LiteralText(text)
    }
}
