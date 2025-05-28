package net.ririfa.fabricord.database.tables

import net.ririfa.fabricord.database.DataManager.cache
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import java.util.*

data class DMLinkData(
    val discordId: Long,
    val minecraftUUID: UUID
)

object DiscordMinecraftLinks : IdTable<UUID>("discord_minecraft_links") {
    override val id = uuid("minecraft_uuid").entityId()
    val discordId = long("discord_id")

    override val primaryKey = PrimaryKey(id, name = "pk_discord_minecraft_link")
}

class DiscordMinecraftLink(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<DiscordMinecraftLink>(DiscordMinecraftLinks) {
        @JvmStatic
        fun from(data: DMLinkData): DiscordMinecraftLink {
            val existing = cache.get(data.minecraftUUID)
            if (existing != null) return existing

            val newLink = new(data.minecraftUUID) {
                discordId = data.discordId
            }
            cache.put(data.minecraftUUID, newLink)
            return newLink
        }


        @JvmStatic
        fun isUserLinked(minecraftUUID: UUID): Boolean {
            return cache.get(minecraftUUID) != null
        }
    }

    var discordId by DiscordMinecraftLinks.discordId

    fun toData(): DMLinkData {
        return DMLinkData(discordId, id.value)
    }
}