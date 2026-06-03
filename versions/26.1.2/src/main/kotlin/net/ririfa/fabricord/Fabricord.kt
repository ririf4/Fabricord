package net.ririfa.fabricord

import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.chat.Component
import net.ririfa.fabricord.i18n.FMsgKey
import net.ririfa.fabricord.i18n.FMsgProvider
import net.ririfa.langman.LangMan
import net.ririfa.langman.LangManBuilder
import net.ririfa.langman.TextFactory
import net.ririfa.langman.ext.yaml.YamlFileLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.nio.file.Path

val Logger: Logger by lazy { Fabricord.logger }

class Fabricord : DedicatedServerModInitializer {
    companion object {
        const val MOD_ID = "fabricord"

        lateinit var langMan: LangMan<FMsgProvider, Component>

        val logger: Logger
            get() = LoggerFactory.getLogger(Fabricord::class.simpleName)
        val loader: FabricLoader = FabricLoader.getInstance()
        val serverDir: Path = loader.gameDir
        val modDir: Path = serverDir.resolve(MOD_ID)
        val langDir: Path = modDir.resolve("lang")
        val dbDir: Path = modDir.resolve("db")

        val availableLang: List<String> = listOf(
            "en",
            "ja",
            "de",
            "fr"
        )
    }

    override fun onInitializeServer() {
        langMan = LangManBuilder.new<FMsgProvider, Component>()
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
    }

    private fun registerEvents() {

    }

    private val textFactory = object : TextFactory<Component> {
        override val clazz: Class<Component>
            get() = Component::class.java

        override fun invoke(text: String): Component = Component.literal(text)
    }
}