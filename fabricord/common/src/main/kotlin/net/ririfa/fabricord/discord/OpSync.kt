package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.entities.Role
import net.ririfa.fabricord.Fabricord
import net.ririfa.fabricord.Logger
import java.util.UUID

object OpSync {
    fun syncOnJoin(minecraftUuid: UUID) {
        val roleIds = Fabricord.config.opSyncRoleIDs?.takeIf { it.isNotEmpty() } ?: return
        val discordId = Fabricord.accountLinks.findDiscordId(minecraftUuid) ?: return
        val guild = DiscordBridge.jda?.guilds?.firstOrNull() ?: return

        guild.retrieveMemberById(discordId).queue(
            { member -> apply(minecraftUuid, member.roles.any { it.id in roleIds }) },
            { error -> Logger.warn("Could not synchronize OP status for Discord user $discordId", error) },
        )
    }

    fun syncFromRoleChange(discordId: Long, roles: List<Role>) {
        val roleIds = Fabricord.config.opSyncRoleIDs?.takeIf { it.isNotEmpty() } ?: return
        val minecraftUuid = Fabricord.accountLinks.findMinecraftUuid(discordId) ?: return
        apply(minecraftUuid, roles.any { it.id in roleIds })
    }

    fun syncOnLink(minecraftUuid: UUID) {
        syncOnJoin(minecraftUuid)
    }

    private fun apply(minecraftUuid: UUID, shouldBeOperator: Boolean) {
        DiscordBridge.platform.execute {
            DiscordBridge.platform.setOperator(minecraftUuid, shouldBeOperator)
        }
    }
}
