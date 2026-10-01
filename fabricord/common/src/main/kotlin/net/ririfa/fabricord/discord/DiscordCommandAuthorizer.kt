package net.ririfa.fabricord.discord

import net.ririfa.fabricord.config.DiscordCommandPermission
import net.ririfa.fabricord.config.DiscordCommandPermissionMode

data class DiscordMemberIdentity(
    val userId: String,
    val roleIds: Set<String>,
)

object DiscordCommandAuthorizer {
    fun isAuthorized(
        permission: DiscordCommandPermission,
        member: DiscordMemberIdentity,
        linkedOperator: Boolean,
    ): Boolean {
        val allowListed = member.userId in permission.allowedUserIDs ||
            member.roleIds.any(permission.allowedRoleIDs::contains)

        return when (permission.mode) {
            DiscordCommandPermissionMode.LINKED_OPERATOR -> linkedOperator
            DiscordCommandPermissionMode.ALLOW_LIST -> allowListed
            DiscordCommandPermissionMode.LINKED_OPERATOR_OR_ALLOW_LIST -> linkedOperator || allowListed
            DiscordCommandPermissionMode.EVERYONE -> true
            DiscordCommandPermissionMode.DISABLED -> false
        }
    }
}
