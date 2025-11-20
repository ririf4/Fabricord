@file:JvmName("Aliases")

package net.ririfa.fabricord

import net.ririfa.fabricord.config.ConfigManager
import net.ririfa.fabricord.config.FConfig
import org.slf4j.Logger
import java.nio.file.Path

val ModDir: Path by lazy { Fabricord.modDir }
val Logger: Logger by lazy { Fabricord.logger }
val Config: FConfig by lazy { ConfigManager.config }