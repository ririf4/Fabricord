package net.ririfa.fabricord

import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
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
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

class Fabricord : DedicatedServerModInitializer {
    companion object {
        const val MOD_ID = "fabricord"

        lateinit var server: MinecraftServer
        lateinit var langMan: LangMan<FMsgProvider, Text>

        val logger: Logger
            get() = LoggerFactory.getLogger(Fabricord::class.simpleName)
        val loader: FabricLoader = FabricLoader.getInstance()
        val serverDir: Path = loader.gameDir
        val modDir: Path = serverDir.resolve(MOD_ID)
        val langDir: Path = modDir.resolve("lang")
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
            .fromResource("/assets/$MOD_ID/lang/")
            .toPath(langDir)
            .withMessageKey(FMsgKey::class.java)
            .withType(YamlFileLoader { inputStream -> Yaml().load(inputStream) })
            .registerTextFactory(object : TextFactory<Text> {
                override val clazz: Class<Text>
                    get() = Text::class.java

                override fun invoke(text: String): Text = Text.literal(text)
            })
            .withLanguage(availableLang)
            .autoUpdateIfNeeded(true)
            .debug(true)
            .build()

        registerServerEvents()
    }

    private fun registerServerEvents() {

    }
}