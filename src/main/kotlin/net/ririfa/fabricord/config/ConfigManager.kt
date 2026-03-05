package net.ririfa.fabricord.config

import net.ririfa.fabricord.util.Logger
import net.ririfa.fabricord.util.ModDir
import net.ririfa.yacla.Yacla
import net.ririfa.yacla.loader.ConfigLoaderBuilder
import net.ririfa.yacla.logger.impl.SLF4JYaclaLogger
import net.ririfa.yacla.yaml.YamlParser
import java.nio.file.Files
import java.nio.file.Path

object ConfigManager {
    private val configFile: Path = ModDir.resolve("config.yml")

    var isErrorOccurred: Boolean = false

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

            return@lazy loader.load().config
        } catch (e: Exception) {
            Logger.error("Failed to initialize config: ${e.message}", e)
            isErrorOccurred = true
            return@lazy Yacla.fillByDefault<FConfig>()
        }
    }
}
