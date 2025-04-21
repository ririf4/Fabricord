package net.ririfa.fabricord.util

import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.ririfa.fabricord.Fabricord
import net.ririfa.fabricord.Logger
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

fun isOlderVersion(current: String, latest: String): Boolean {
    val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
    val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }

    val maxLength = maxOf(currentParts.size, latestParts.size)
    val paddedCurrent = currentParts + List(maxLength - currentParts.size) { 0 }
    val paddedLatest = latestParts + List(maxLength - latestParts.size) { 0 }

    return (0 until maxLength).any { paddedCurrent[it] < paddedLatest[it] }
}

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

fun String.toBooleanOrNull(): Boolean? {
    return when (this.trim().lowercase()) {
        "true", "1", "t" -> true
        "false", "0", "f" -> false
        else -> null
    }
}

fun copyResourceToFile(resourcePath: String, outputPath: Path) {
    val fullPath = "/$resourcePath"
    val inputStream: InputStream? = Fabricord::class.java.getResourceAsStream(fullPath)
    if (inputStream == null) {
        Logger.error("Resource $fullPath not found in Jar")
        return
    }
    Files.copy(inputStream, outputPath)
    Logger.info("Copied resource $fullPath to $outputPath")
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