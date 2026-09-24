package net.ririfa.fabricord.config

import net.ririfa.fabricord.Logger
import net.ririfa.yacla.Yacla
import net.ririfa.yacla.loader.ConfigLoaderBuilder
import net.ririfa.yacla.logger.impl.SLF4JYaclaLogger
import net.ririfa.yacla.yaml.YamlParser
import java.nio.file.Files
import java.nio.file.Path

class ConfigManager(
    val modDir: Path
) {
    private val configFile: Path = modDir.resolve("config.yml")

    @Volatile
    var isErrorOccurred: Boolean = false

    private val loader: ConfigLoaderBuilder<FConfig>
        get() = Yacla.loader<FConfig>()
            .fromResource("/assets/fabricord/config/languages/")
            .defaultLanguage("en")
            .pull(configFile)
            .parser(YamlParser())
            .autoUpdateIfOutdated(true)
            .withLogger(SLF4JYaclaLogger)

    @Volatile
    private var _config: FConfig? = null

    val config: FConfig
        get() = _config ?: loadConfig().also { _config = it }

    private fun loadConfig(): FConfig {
        return try {
            if (!Files.exists(modDir)) {
                Files.createDirectories(modDir)
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
