package net.ririfa.fabricord

import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

class Fabricord : DedicatedServerModInitializer {
    companion object {
        const val MOD_ID = "fabricord"

        lateinit var server: MinecraftServer

        val logger: Logger
            get() = LoggerFactory.getLogger(Fabricord::class.simpleName)
        val loader: FabricLoader = FabricLoader.getInstance()
        val serverDir: Path = loader.gameDir
        val modDir: Path = serverDir.resolve(MOD_ID)
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
    }

    override fun onInitializeServer() {

    }
}