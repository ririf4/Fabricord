package net.ririfa.fabricord

import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

class Fabricord : DedicatedServerModInitializer {
    companion object {
        const val MOD_ID = "fabricord"

        val logger: Logger
            get() = LoggerFactory.getLogger(Fabricord::class.simpleName)
        val loader: FabricLoader = FabricLoader.getInstance()
        val serverDir: Path = loader.gameDir
        val modDir: Path = serverDir.resolve(MOD_ID)
    }

    override fun onInitializeServer() {

    }
}