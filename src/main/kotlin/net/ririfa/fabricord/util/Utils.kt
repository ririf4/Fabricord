package net.ririfa.fabricord.util

import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent

fun ServerPlayerEntity.playSoundToPlayerMaster(soundEvent: SoundEvent, f: Float, g: Float) {
    this.networkHandler.sendPacket(
        PlaySoundS2CPacket(
            Registries.SOUND_EVENT.getEntry(soundEvent),
            SoundCategory.MASTER,
            this.x,
            this.y,
            this.z,
            f,
            g,
            this.random.nextLong()
        )
    )
}

fun replaceUUIDsWithMCIDs(message: String, players: List<ServerPlayerEntity>): Pair<String, List<ServerPlayerEntity>> {
    var updatedMessage = message
    val mentionedPlayers = mutableListOf<ServerPlayerEntity>()
    val uuidPattern = Regex("@\\{([0-9a-fA-F-]+)}")
    val matches = uuidPattern.findAll(message)

    matches.forEach { match ->
        val uuidStr = match.groupValues[1]
        val player = players.find { it.uuid.toString() == uuidStr }
        player?.let {
            updatedMessage = updatedMessage.replace(match.value, "@${it.name.string}")
            mentionedPlayers.add(it)
        }
    }
    return Pair(updatedMessage, mentionedPlayers)
}