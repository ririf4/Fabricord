package net.ririfa.fabricord.database.tables

import net.ririfa.fabricord.database.SyncManager
import net.ririfa.fabricord.util.DB
import net.ririfa.fabricord.util.ShortUUID
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

data class GroupData(
    val uuid: ShortUUID,
    val name: String,
    val owner: UUID,
    val members: List<UUID>,
    val open: Boolean
)

// --- Tables ---

object Groups : IdTable<String>("groups") {
    override val id = varchar("uuid", 22).entityId()
    override val primaryKey = PrimaryKey(id)
    val name = text("name")
    val owner = uuid("owner")
    val open = bool("open")
}

object GroupMembers : Table("group_members") {
    val groupId = varchar("group_uuid", 22).references(Groups.id)
    val memberId = uuid("member_uuid")
    override val primaryKey = PrimaryKey(groupId, memberId)
}

// --- DAO ---

class Group(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, Group>(Groups) {
        fun from(data: GroupData): Group {
            val group = new(data.uuid.toShortString()) {
                name = data.name
                owner = data.owner
                open = data.open
            }
            data.members.forEach { member ->
                GroupMembers.insert {
                    it[groupId] = data.uuid.toShortString()
                    it[memberId] = member
                }
            }
            return group
        }
    }

    var name by Groups.name
    var owner by Groups.owner

    val members: List<UUID>
        get() = GroupMembers.selectAll()
            .where { GroupMembers.groupId eq id.value }
            .map { it[GroupMembers.memberId] }

    var open by Groups.open

    fun toData(): GroupData = GroupData(
        uuid = ShortUUID.fromShortString(id.value),
        name = name,
        owner = owner,
        members = members,
        open = open
    )

    fun addMember(member: UUID) {
        GroupMembers.insert {
            it[groupId] = id.value
            it[memberId] = member
        }
    }

    fun removeMember(member: UUID) {
        GroupMembers.deleteWhere {
            (groupId eq id.value) and (memberId eq member)
        }
    }
}

// --- Extensions ---

fun GroupData.insert(): Group {
    return DB {
        require(Group.findById(this@insert.uuid.toShortString()) == null) {
            "Group with UUID ${this@insert.uuid} already exists"
        }
        val group = Group.from(this@insert)
        SyncManager.markDirty(this@insert.uuid)
        group
    }
}

fun GroupData.update(): Boolean {
    return DB {
        val entity = Group.findById(this@update.uuid.toShortString()) ?: return@DB false
        entity.name = this@update.name
        entity.owner = this@update.owner
        entity.open = this@update.open

        GroupMembers.deleteWhere { groupId eq this@update.uuid.toShortString() }
        this@update.members.forEach { member ->
            GroupMembers.insert {
                it[groupId] = this@update.uuid.toShortString()
                it[memberId] = member
            }
        }
        SyncManager.markDirty(this@update.uuid)
        true
    }
}

fun Group.deleteGroup(): Boolean {
    return DB {
        GroupMembers.deleteWhere { groupId eq this@deleteGroup.id.value }
        this@deleteGroup.delete()
        SyncManager.markDirty(ShortUUID.fromShortString(this@deleteGroup.id.value))
        true
    }
}

fun ShortUUID.getGroup(): GroupData? {
    return DB {
        Group.findById(this@getGroup.toShortString())?.toData()
    }
}
