package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.entities.Role
import net.minecraft.server.network.ServerPlayerEntity
import net.ririfa.fabricord.Fabricord
import net.ririfa.fabricord.Logger
import java.util.UUID

object OpSync {
    fun syncOnJoin(player: ServerPlayerEntity) {
        val roleIds = Fabricord.config.opSyncRoleIDs?.takeIf { it.isNotEmpty() } ?: return
        val discordId = Fabricord.accountLinks.findDiscordId(player.uuid) ?: return
        val guild = DiscordBridge.jda?.guilds?.firstOrNull() ?: return

        guild.retrieveMemberById(discordId).queue(
            { member -> apply(player.uuid, member.roles.any { it.id in roleIds }) },
            { error -> Logger.warn("Could not synchronize OP status for Discord user $discordId", error) },
        )
    }

    fun syncFromRoleChange(discordId: Long, roles: List<Role>) {
        val roleIds = Fabricord.config.opSyncRoleIDs?.takeIf { it.isNotEmpty() } ?: return
        val minecraftUuid = Fabricord.accountLinks.findMinecraftUuid(discordId) ?: return
        apply(minecraftUuid, roles.any { it.id in roleIds })
    }

    fun syncOnLink(minecraftUuid: UUID) {
        Fabricord.server.execute {
            Fabricord.server.playerManager.getPlayer(minecraftUuid)?.let(::syncOnJoin)
        }
    }

    private fun apply(minecraftUuid: UUID, shouldBeOperator: Boolean) {
        Fabricord.server.execute {
            val player = Fabricord.server.playerManager.getPlayer(minecraftUuid) ?: return@execute
            val profile = player.gameProfile
            val isOperator = Fabricord.server.playerManager.isOperator(profile)
            when {
                shouldBeOperator && !isOperator -> Fabricord.server.playerManager.addToOperators(profile)
                !shouldBeOperator && isOperator -> Fabricord.server.playerManager.removeFromOperators(profile)
            }
        }
    }
}
