package net.ririfa.fabricord.database

import net.ririfa.fabricord.database.tables.Group
import net.ririfa.fabricord.database.tables.GroupMembers
import net.ririfa.fabricord.util.DB
import net.ririfa.fabricord.util.DBFile
import net.ririfa.fabricord.util.ShortUUID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object SyncManager {
    private val dirtyGroups = mutableSetOf<ShortUUID>()
    private val syncExecutor = Executors.newSingleThreadScheduledExecutor()

    init {
        syncExecutor.scheduleAtFixedRate(::syncDirtyGroups, 0, 5, TimeUnit.SECONDS)
    }

    fun markDirty(groupId: ShortUUID) {
        synchronized(dirtyGroups) {
            dirtyGroups.add(groupId)
        }
    }

    fun forceSyncAll() {
        val allIds = DB {
            Group.all().map { ShortUUID.fromShortString(it.id.value) }
        }
        synchronized(dirtyGroups) {
            dirtyGroups.addAll(allIds)
        }
        syncDirtyGroups()
    }

    private fun syncDirtyGroups() {
        val targets = synchronized(dirtyGroups) {
            val copy = dirtyGroups.toSet()
            dirtyGroups.clear()
            copy
        }
        if (targets.isEmpty()) return

        targets.forEach { uuid ->
            val data = DB {
                Group.findById(uuid.toShortString())?.toData()
            }

            DBFile {
                if (data == null) {
                    GroupMembers.deleteWhere { groupId eq uuid.toShortString() }
                    Group.findById(uuid.toShortString())?.delete()
                } else {
                    Group.findById(uuid.toShortString())?.apply {
                        name = data.name
                        owner = data.owner
                        open = data.open

                        GroupMembers.deleteWhere { groupId eq id.value }
                        data.members.forEach { member ->
                            GroupMembers.insert {
                                it[groupId] = data.uuid.toShortString()
                                it[memberId] = member
                            }
                        }
                    } ?: Group.from(data)
                }
            }
        }
    }
}
