package net.ririfa.fabricord.config

import net.ririfa.fabricord.Logger
import net.ririfa.fabricord.ModDir
import net.ririfa.yacla.Yacla
import net.ririfa.yacla.loader.ConfigLoaderBuilder
import net.ririfa.yacla.logger.impl.SLF4JYaclaLogger
import net.ririfa.yacla.yaml.YamlParser
import java.nio.file.Files
import java.nio.file.Path

object ConfigManager {
    private val configFile: Path = ModDir.resolve("config.yml")

    val loader: ConfigLoaderBuilder<FConfig> by lazy {
        Yacla.loader<FConfig>()
            .fromResource("/assets/fabricord/config.yml")
            .toFile(configFile)
            .parser(YamlParser())
            .schema(FConfigSchema)
            .autoUpdateIfOutdated(true)
            .withLogger(SLF4JYaclaLogger)
    }

    val config: FConfig by lazy {
        try {
            if (!Files.exists(ModDir)) {
                Files.createDirectories(ModDir)
            }

            return@lazy loader.load().also { it.validate() }.config
        } catch (e: Exception) {
            Logger.error("Failed to initialize config: ${e.message}", e)

            return@lazy Yacla.fillByDefault<FConfig>(FConfigSchema)
        }
    }
}