package net.ririfa.fabricord.database.tables

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
            return findById(data.minecraftUUID) ?: new(data.minecraftUUID) {
                discordId = data.discordId
            }
        }

        @JvmStatic
        fun isUserLinked(minecraftUUID: UUID): Boolean {
            return findById(minecraftUUID) != null
        }
    }

    var discordId by DiscordMinecraftLinks.discordId

    fun toData(): DMLinkData {
        return DMLinkData(discordId, id.value)
    }
}