@file:JvmName("Aliases")
@file:Suppress("FunctionName")

package net.ririfa.fabricord.util

import net.dv8tion.jda.api.JDA
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.ririfa.fabricord.Fabricord
import net.ririfa.fabricord.config.ConfigManager
import net.ririfa.fabricord.config.FConfig
import net.ririfa.fabricord.discord.DiscordBotManager
import net.ririfa.fabricord.i18n.FMsgProvider
import net.ririfa.langman.LangMan
import org.slf4j.Logger
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

val JDA: JDA? by lazy { DiscordBotManager.jda }
val ModDir: Path by lazy { Fabricord.Companion.modDir }
val Logger: Logger by lazy { Fabricord.Companion.logger }
val Config: FConfig by lazy { ConfigManager.config }
val DBDir: Path by lazy { Fabricord.Companion.dbDir }
val Server: MinecraftServer by lazy { Fabricord.Companion.server }
val LM: LangMan<FMsgProvider, Text> by lazy { Fabricord.Companion.langMan }

val T: ScheduledExecutorService by lazy { Fabricord.Companion.thread }

fun Logger.info(message: Text, cause: Throwable? = null) {
    if (cause != null) {
        this.info(message.string, cause)
    } else {
        this.info(message.string)
    }
}

fun Logger.warn(message: Text, cause: Throwable? = null) {
    if (cause != null) {
        this.warn(message.string, cause)
    } else {
        this.warn(message.string)
    }
}

fun Logger.error(message: Text, cause: Throwable? = null) {
    if (cause != null) {
        this.error(message.string, cause)
    } else {
        this.error(message.string)
    }
}

@Suppress("FunctionName")
inline fun <reified Q> FT(
    delay: Long = 0,
    period: Long = -1,
    unit: TimeUnit = TimeUnit.MILLISECONDS,
    newThread: Boolean = false,
    arg: Q? = null,
    crossinline task: (Q?) -> Unit
): ScheduledFuture<*> {
    val executor = if (newThread) Executors.newSingleThreadScheduledExecutor() else T

    val future: ScheduledFuture<*> =
        if (period > 0) {
            executor.scheduleAtFixedRate({ task(arg) }, delay, period, unit)
        } else {
            executor.schedule({
                try {
                    task(arg)
                } finally {
                    if (newThread) executor.shutdown()
                }
            }, delay, unit)
        }

    return future
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
