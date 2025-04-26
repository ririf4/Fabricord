package net.ririfa.fabricord.tables

import net.ririfa.fabricord.util.DB
import net.ririfa.fabricord.util.ShortUUID
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

data class GroupData(
    val uuid: String,
)

object Groups : IdTable<String>("players") {
    override val id = varchar("uuid", 22).entityId()

}

class Player(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, Player>(Groups) {
        fun from(data: GroupData): Player {
            return new(data.uuid) {

            }
        }
    }


    fun to(): GroupData = GroupData(
        uuid = id.value,

        )
}

fun GroupData.insert(): Player {
    return DB {
        Player.from(this@insert)
    }
}

fun GroupData.update(): Boolean {
    return DB {
        val entity = Player.findById(this@update.uuid) ?: return@DB false

        true
    }
}

fun Player.deletePlayer(): Boolean {
    this.delete()
    return true
}

fun ShortUUID.getPlayer(): GroupData? {
    return DB {
        Player.findById(this@getPlayer.toShortString())?.to()
    }
}
