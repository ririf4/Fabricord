package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.entities.Role
import net.minecraft.server.network.ServerPlayerEntity
import net.ririfa.fabricord.database.DataBase
import net.ririfa.fabricord.util.Config
import net.ririfa.fabricord.util.Logger
import net.ririfa.fabricord.util.Server
import java.util.*

object OpSync {

    /**
     * Syncs OP status for a player who just joined Minecraft.
     * Looks up their linked Discord account and checks for opSyncRoles.
     */
    fun syncOnJoin(player: ServerPlayerEntity) {
        val opSyncRoleIDs = Config.opSyncRoleIDs?.takeIf { it.isNotEmpty() } ?: return
        if (!DiscordBotManager.isBotInitialized) return

        val discordId = DataBase.getDiscordId(player.uuid) ?: return
        val jda = DiscordBotManager.jda ?: return
        val guild = jda.guilds.firstOrNull() ?: return

        guild.retrieveMemberById(discordId).queue({ member ->
            val hasRole = member.roles.any { it.id in opSyncRoleIDs }
            applyOpSync(player.name.string, hasRole)
        }, { error ->
            Logger.warn("OpSync: Failed to retrieve Discord member $discordId: ${error.message}")
        })
    }

    /**
     * Syncs OP status when a Discord member's roles change.
     * Only applies immediately if the player is currently online.
     * Offline players will be synced on their next login.
     */
    fun syncFromRoleChange(discordId: Long, currentRoles: List<Role>) {
        val opSyncRoleIDs = Config.opSyncRoleIDs?.takeIf { it.isNotEmpty() } ?: return
        val uuid = DataBase.getMinecraftUUID(discordId) ?: return
        val player = Server.playerManager.getPlayer(uuid) ?: return  // offline → synced on next login

        val hasRole = currentRoles.any { it.id in opSyncRoleIDs }
        applyOpSync(player.name.string, hasRole)
    }

    /**
     * Syncs OP status right after an account link is completed.
     * Reuses syncOnJoin if the player is currently online.
     */
    fun syncOnLink(mcUUID: UUID) {
        if (Config.opSyncRoleIDs?.isNotEmpty() != true) return
        val player = Server.playerManager.getPlayer(mcUUID) ?: return
        syncOnJoin(player)
    }

    private fun applyOpSync(playerName: String, hasRole: Boolean) {
        val command = if (hasRole) "op $playerName" else "deop $playerName"
        Server.execute {
            Server.commandManager.parseAndExecute(Server.commandSource, command)
        }
    }
}
