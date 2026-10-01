package net.ririfa.fabricord.discord

import net.minecraft.ChatFormat
import net.minecraft.network.chat.ChatMessageType
import net.minecraft.network.chat.TextComponent
import net.minecraft.server.world.ServerChunkManager
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.ririfa.fabricord.Fabricord
import net.ririfa.fabricord.i18n.FMsgKey
import java.util.UUID

object MinecraftDiscordPlatform : DiscordPlatform {
    override fun execute(task: () -> Unit) = Fabricord.server.execute(task)

    override fun onlinePlayers(): List<MinecraftPlayerRef> =
        Fabricord.server.playerManager.playerList.map { MinecraftPlayerRef(it.uuid, it.name.string) }

    override fun broadcastDiscordMessage(prefix: String, message: String, mentionedPlayers: Set<UUID>) {
        Fabricord.server.playerManager.playerList.forEach { player ->
            val component = TextComponent(prefix).applyFormat(ChatFormat.AQUA)
                .append(
                    TextComponent(message).applyFormat(
                        if (player.uuid in mentionedPlayers) ChatFormat.BOLD else ChatFormat.WHITE
                    )
                )
            player.sendChatMessage(component, ChatMessageType.SYSTEM)
            if (player.uuid in mentionedPlayers) {
                player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 2.0f, 2.0f)
            }
        }
    }

    override fun snapshot(): ServerSnapshot {
        val server = Fabricord.server
        return ServerSnapshot(
            playerCount = server.playerManager.getCurrentPlayerCount(),
            maxPlayers = server.playerManager.getMaxPlayerCount(),
            mspt = server.lastTickLengths.average() / 1_000_000.0,
            version = server.version,
            worldTime = server.getWorld(net.minecraft.world.dimension.DimensionType.OVERWORLD)?.timeOfDay?.rem(24_000) ?: 0L,
            loadedChunks = server.worlds.sumOf { (it.chunkManager as ServerChunkManager).getLoadedChunkCount() },
            uptimeMillis = System.currentTimeMillis() - Fabricord.serverStartTime,
        )
    }

    override fun isOperator(minecraftUuid: UUID): Boolean? = runCatching {
        val profile = Fabricord.server.userCache?.getByUuid(minecraftUuid) ?: return null
        Fabricord.server.playerManager.isOperator(profile)
    }.getOrNull()

    override fun findPlayerName(minecraftUuid: UUID): String? =
        Fabricord.server.playerManager.getPlayer(minecraftUuid)?.name?.string

    override fun playerTargetExists(command: String, playerName: String): Boolean = when (command) {
        "kick" -> Fabricord.server.playerManager.getPlayer(playerName) != null
        "ban" -> Fabricord.server.userCache?.findByName(playerName) != null
        else -> true
    }

    override fun executeServerCommand(command: String) {
        Fabricord.server.commandManager.execute(Fabricord.server.commandSource, command)
    }

    override fun setOperator(minecraftUuid: UUID, shouldBeOperator: Boolean) {
        val player = Fabricord.server.playerManager.getPlayer(minecraftUuid) ?: return
        val profile = player.gameProfile
        val isOperator = Fabricord.server.playerManager.isOperator(profile)
        when {
            shouldBeOperator && !isOperator -> Fabricord.server.playerManager.addToOperators(profile)
            !shouldBeOperator && isOperator -> Fabricord.server.playerManager.removeFromOperators(profile)
        }
    }

    override fun message(key: FMsgKey, language: String, arguments: Map<String, String>): String =
        Fabricord.langMan.getMessage(key, argsComplete = arguments, lang = language).string
}
