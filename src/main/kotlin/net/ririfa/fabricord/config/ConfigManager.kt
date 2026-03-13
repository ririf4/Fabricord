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

    @Volatile
    var isErrorOccurred: Boolean = false

    private val loader: ConfigLoaderBuilder<FConfig>
        get() = Yacla.loader<FConfig>()
            .fromResource("/assets/fabricord/config.yml")
            .toFile(configFile)
            .parser(YamlParser())
            .autoUpdateIfOutdated(true)
            .withLogger(SLF4JYaclaLogger)

    @Volatile
    private var _config: FConfig? = null

    val config: FConfig
        get() = _config ?: loadConfig().also { _config = it }

    private fun loadConfig(): FConfig {
        return try {
            if (!Files.exists(ModDir)) {
                Files.createDirectories(ModDir)
            }
            loader.load().config
        } catch (e: Exception) {
            Logger.error("Failed to initialize config: ${e.message}", e)
            isErrorOccurred = true
            Yacla.fillByDefault<FConfig>()
        }
    }

    fun reload(): Boolean {
        isErrorOccurred = false
        _config = loadConfig()
        return !isErrorOccurred
    }
}
