package net.ririfa.fabricord.discord

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.ririfa.fabricord.Fabricord
import net.ririfa.fabricord.i18n.FMsgKey
import java.util.UUID

object MinecraftDiscordPlatform : DiscordPlatform {
    override fun execute(task: () -> Unit) = Fabricord.server.execute(task)

    override fun onlinePlayers(): List<MinecraftPlayerRef> =
        Fabricord.server.playerList.players.map { MinecraftPlayerRef(it.uuid, it.name.string) }

    override fun broadcastDiscordMessage(prefix: String, message: String, mentionedPlayers: Set<UUID>) {
        Fabricord.server.playerList.players.forEach { player ->
            val component = Component.literal(prefix).withStyle(ChatFormatting.AQUA)
                .append(
                    Component.literal(message).withStyle(
                        if (player.uuid in mentionedPlayers) ChatFormatting.BOLD else ChatFormatting.WHITE
                    )
                )
            player.sendSystemMessage(component)
            if (player.uuid in mentionedPlayers) {
                player.playSound(SoundEvents.NOTE_BLOCK_PLING, 2.0f, 2.0f)
            }
        }
    }

    override fun snapshot(): ServerSnapshot {
        val server = Fabricord.server
        return ServerSnapshot(
            playerCount = server.playerList.playerCount,
            maxPlayers = server.playerList.maxPlayers,
            mspt = server.tickTimes.average() / 1_000_000.0,
            version = server.serverVersion,
            worldTime = server.overworld().dayTime % 24_000,
            loadedChunks = server.allLevels.sumOf { it.chunkSource.loadedChunksCount },
            uptimeMillis = System.currentTimeMillis() - Fabricord.serverStartTime,
        )
    }

    override fun isOperator(minecraftUuid: UUID): Boolean? = runCatching {
        val profile = Fabricord.server.profileCache?.get(minecraftUuid)?.orElse(null) ?: return null
        Fabricord.server.playerList.isOp(profile)
    }.getOrNull()

    override fun findPlayerName(minecraftUuid: UUID): String? =
        Fabricord.server.playerList.getPlayer(minecraftUuid)?.name?.string

    override fun playerTargetExists(command: String, playerName: String): Boolean = when (command) {
        "kick" -> Fabricord.server.playerList.getPlayerByName(playerName) != null
        "ban" -> Fabricord.server.profileCache?.get(playerName)?.isPresent == true
        else -> true
    }

    override fun executeServerCommand(command: String) {
        Fabricord.server.commands.performPrefixedCommand(Fabricord.server.createCommandSourceStack(), command)
    }

    override fun setOperator(minecraftUuid: UUID, shouldBeOperator: Boolean) {
        val player = Fabricord.server.playerList.getPlayer(minecraftUuid) ?: return
        val profile = player.gameProfile
        val isOperator = Fabricord.server.playerList.isOp(profile)
        when {
            shouldBeOperator && !isOperator -> Fabricord.server.playerList.op(profile)
            !shouldBeOperator && isOperator -> Fabricord.server.playerList.deop(profile)
        }
    }

    override fun message(key: FMsgKey, language: String, arguments: Map<String, String>): String =
        Fabricord.langMan.getMessage(key, argsComplete = arguments, lang = language).string
}
