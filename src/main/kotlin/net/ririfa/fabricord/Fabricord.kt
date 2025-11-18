package net.ririfa.fabricord

import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

class Fabricord : DedicatedServerModInitializer {
    companion object {
        const val MOD_ID = "fabricord"

//        lateinit var langMan: LangMan<FabricordMessageProvider, Text>
//        var consoleAppender: ConsoleTrackerAppender? = null

        val logger: Logger
            get() = LoggerFactory.getLogger(Fabricord::class.simpleName)
        val loader: FabricLoader = FabricLoader.getInstance()
        val serverDir: Path = loader.gameDir
        val modDir: Path = serverDir.resolve(MOD_ID)
        val langDir: Path = modDir.resolve("lang")

//        val thread: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
//        val availableLang = listOf<String>("en", "ja")
    }

    override fun onInitializeServer() {

    }
}