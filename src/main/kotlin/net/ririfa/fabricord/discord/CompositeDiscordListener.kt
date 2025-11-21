package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.minecraft.server.network.ServerPlayerEntity
import net.ririfa.fabricord.Config
import net.ririfa.fabricord.FT
import net.ririfa.fabricord.Server

class CompositeDiscordListener : ListenerAdapter() {
    private val mcidPattern = Regex("@([a-zA-Z0-9_]+)")
    private val uuidPattern = Regex("@\\{([0-9a-fA-F-]+)}")

    private val logChannelID = Config.logChannelID

    override fun onMessageReceived(event: MessageReceivedEvent) {
        when (event.channel.id) {
            logChannelID -> {
                FT {
                    val (mentionedPlayers, foundUUID) = findMentionedPlayers(event.message.contentRaw)

                }
            }
        }
    }

    private fun findMentionedPlayers(messageContent: String): Pair<List<ServerPlayerEntity>, Boolean> {
        val players = Server.playerManager.playerList

        // index maps
        val nameMap = players.associateBy { it.name.string.lowercase() }
        val uuidMap = players.associateBy { it.uuid.toString() }

        val lower = messageContent.lowercase()
        var foundUUID = false

        val mentioned = mutableSetOf<ServerPlayerEntity>()

        // match by MCID
        for (match in mcidPattern.findAll(lower)) {
            val name = match.groupValues[1]
            nameMap[name]?.let { mentioned += it }
        }

        // match by UUID
        for (match in uuidPattern.findAll(messageContent)) {
            val uuid = match.groupValues[1]
            uuidMap[uuid]?.let {
                mentioned += it
                foundUUID = true
            }
        }

        return mentioned.toList() to foundUUID
    }
}