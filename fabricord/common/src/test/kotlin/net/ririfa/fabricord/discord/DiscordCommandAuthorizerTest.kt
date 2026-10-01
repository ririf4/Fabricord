package net.ririfa.fabricord.discord

import net.ririfa.fabricord.config.DiscordCommandPermission
import net.ririfa.fabricord.config.DiscordCommandPermissionMode
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiscordCommandAuthorizerTest {
    private val member = DiscordMemberIdentity("100", setOf("200"))

    @Test
    fun `linked operator mode only accepts linked operators`() {
        val permission = DiscordCommandPermission(DiscordCommandPermissionMode.LINKED_OPERATOR)

        assertTrue(DiscordCommandAuthorizer.isAuthorized(permission, member, linkedOperator = true))
        assertFalse(DiscordCommandAuthorizer.isAuthorized(permission, member, linkedOperator = false))
    }

    @Test
    fun `allow list accepts either configured users or roles`() {
        val users = DiscordCommandPermission(
            mode = DiscordCommandPermissionMode.ALLOW_LIST,
            allowedUserIDs = setOf("100"),
        )
        val roles = DiscordCommandPermission(
            mode = DiscordCommandPermissionMode.ALLOW_LIST,
            allowedRoleIDs = setOf("200"),
        )
        val empty = DiscordCommandPermission(DiscordCommandPermissionMode.ALLOW_LIST)

        assertTrue(DiscordCommandAuthorizer.isAuthorized(users, member, linkedOperator = false))
        assertTrue(DiscordCommandAuthorizer.isAuthorized(roles, member, linkedOperator = false))
        assertFalse(DiscordCommandAuthorizer.isAuthorized(empty, member, linkedOperator = true))
    }

    @Test
    fun `combined public and disabled modes are explicit`() {
        val combined = DiscordCommandPermission(
            mode = DiscordCommandPermissionMode.LINKED_OPERATOR_OR_ALLOW_LIST,
            allowedRoleIDs = setOf("200"),
        )
        val everyone = DiscordCommandPermission(DiscordCommandPermissionMode.EVERYONE)
        val disabled = DiscordCommandPermission(DiscordCommandPermissionMode.DISABLED)

        assertTrue(DiscordCommandAuthorizer.isAuthorized(combined, member, linkedOperator = false))
        assertTrue(DiscordCommandAuthorizer.isAuthorized(everyone, member, linkedOperator = false))
        assertFalse(DiscordCommandAuthorizer.isAuthorized(disabled, member, linkedOperator = true))
    }
}
