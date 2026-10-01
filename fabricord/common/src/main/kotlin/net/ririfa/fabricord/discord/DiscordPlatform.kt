package net.ririfa.fabricord.discord

import net.ririfa.fabricord.i18n.FMsgKey
import java.util.UUID

data class MinecraftPlayerRef(
    val uuid: UUID,
    val name: String,
)

data class ServerSnapshot(
    val playerCount: Int,
    val maxPlayers: Int,
    val mspt: Double,
    val version: String,
    val worldTime: Long,
    val loadedChunks: Int,
    val uptimeMillis: Long,
) {
    val tps: Double = if (mspt <= 0.0) 20.0 else minOf(20.0, 1000.0 / mspt)
}

interface DiscordPlatform {
    fun execute(task: () -> Unit)
    fun onlinePlayers(): List<MinecraftPlayerRef>
    fun broadcastDiscordMessage(prefix: String, message: String, mentionedPlayers: Set<UUID>)
    fun snapshot(): ServerSnapshot
    fun isOperator(minecraftUuid: UUID): Boolean?
    fun findPlayerName(minecraftUuid: UUID): String?
    fun playerTargetExists(command: String, playerName: String): Boolean
    fun executeServerCommand(command: String)
    fun setOperator(minecraftUuid: UUID, shouldBeOperator: Boolean)
    fun message(key: FMsgKey, language: String, arguments: Map<String, String> = emptyMap()): String
}
