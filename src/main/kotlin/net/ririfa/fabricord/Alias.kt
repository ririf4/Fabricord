package net.ririfa.fabricord

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.ririfa.fabricord.discord.DiscordBotManager
import net.ririfa.fabricord.translation.FabricordMessageProvider
import net.ririfa.langman.LangMan
import org.slf4j.Logger
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val JDA by lazy { DiscordBotManager.jda }
val LM: LangMan<FabricordMessageProvider, Text> by lazy { Fabricord.langMan }

@JvmField
val Logger: Logger = Fabricord.logger
val Server: MinecraftServer by lazy { Fabricord.server }
val Loader: FabricLoader = Fabricord.loader
val ServerDir: Path = Fabricord.serverDir
val ModDir: Path = Fabricord.modDir
val Config: ConfigManager.Config by lazy { ConfigManager.config }
val T = Fabricord.thread

@Suppress("FunctionName")
inline fun <reified Q> FT(
	delay: Long = 0,
	period: Long = -1,
	unit: TimeUnit = TimeUnit.MILLISECONDS,
	newThread: Boolean = false,
	argument: Q? = null,
	crossinline task: (Q?) -> Unit
) {
	val executor = if (newThread) Executors.newSingleThreadScheduledExecutor() else T

	if (period > 0) {
		executor.scheduleAtFixedRate({ task(argument) }, delay, period, unit)
	} else {
		executor.schedule({ task(argument) }, delay, unit)
	}

	if (newThread && period <= 0) {
		executor.shutdown()
	}
}

@Suppress("FunctionName")
inline fun FT(
	delay: Long = 0,
	period: Long = -1,
	unit: TimeUnit = TimeUnit.MILLISECONDS,
	newThread: Boolean = false,
	crossinline task: () -> Unit
) {
	FT<Unit>(delay, period, unit, newThread, null) { task() }
}