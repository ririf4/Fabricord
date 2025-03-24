package net.ririfa.fabricord.util

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.minecraft.server.command.ServerCommandSource

fun literal(name: String): LiteralArgumentBuilder<ServerCommandSource> =
	LiteralArgumentBuilder.literal(name)

fun <T> argument(name: String, type: com.mojang.brigadier.arguments.ArgumentType<T>): RequiredArgumentBuilder<ServerCommandSource, T> =
	RequiredArgumentBuilder.argument(name, type)

fun CommandDispatcher<ServerCommandSource>.registerCommand(name: String, block: LiteralArgumentBuilder<ServerCommandSource>.() -> Unit) {
	this.register(literal(name).apply(block))
}
