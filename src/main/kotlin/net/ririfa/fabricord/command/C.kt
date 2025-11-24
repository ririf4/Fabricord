package net.ririfa.fabricord.command

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.server.command.ServerCommandSource

interface C {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>)
}