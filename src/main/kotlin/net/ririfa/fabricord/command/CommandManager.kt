@file:Suppress("DuplicatedCode")

package net.ririfa.fabricord.command

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.server.command.ServerCommandSource
import java.util.*

object CommandManager {
    @JvmField
    val localChatToggled = mutableSetOf<UUID>()

    fun registerAll(dispatcher: CommandDispatcher<ServerCommandSource>) {
        Commands.entries.forEach { it.register(dispatcher) }
    }
}
