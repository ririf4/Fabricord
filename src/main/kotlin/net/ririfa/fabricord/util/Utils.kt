package net.ririfa.fabricord.util

import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.text.Text
import org.slf4j.Logger

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

fun Logger.info(text: Text, vararg throwable: Throwable?) {
    this.info(text.string, *throwable)
}

fun Logger.warn(text: Text, vararg throwable: Throwable?) {
    this.warn(text.string, *throwable)
}

fun Logger.error(text: Text, vararg throwable: Throwable?) {
    this.error(text.string, *throwable)
}

//fun Logger.logIfDebug(text: Text, vararg throwable: Throwable?) {
//    if (FConfig.isDebug) {
//        this.debug(text.string, *throwable)
//    }
//}