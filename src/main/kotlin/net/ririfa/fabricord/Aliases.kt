@file:JvmName("Aliases")
@file:Suppress("FunctionName")

package net.ririfa.fabricord

import net.minecraft.server.MinecraftServer
import net.ririfa.fabricord.config.ConfigManager
import net.ririfa.fabricord.config.FConfig
import org.slf4j.Logger
import java.nio.file.Path
import java.util.concurrent.ScheduledExecutorService

val ModDir: Path by lazy { Fabricord.modDir }
val Logger: Logger by lazy { Fabricord.logger }
val Config: FConfig by lazy { ConfigManager.config }
val Server: MinecraftServer by lazy { Fabricord.server }

val T: ScheduledExecutorService by lazy { Fabricord.thread }

inline fun FT(crossinline task: () -> Unit) {
    FT<Unit>(null) { task() }
}

inline fun <reified Q> FT(arg: Q?, crossinline block: (Q?) -> Unit) {
    T.submit { block(arg) }
}

inline fun <reified Q> FT(delay: Long, period: Long, arg: Q?, crossinline block: (Q?) -> Unit) {
    T.scheduleAtFixedRate({ block(arg) }, delay, period, java.util.concurrent.TimeUnit.MILLISECONDS)
}
