package net.ririfa.fabricord.config

import kotlin.test.Test
import kotlin.test.assertEquals

class DiscordCommandPermissionsLoaderTest {
    @Test
    fun `loads permission maps case insensitively`() {
        val loaded = DiscordCommandPermissionsLoader().load(
            mapOf(
                "Run" to mapOf(
                    "mode" to "linked_operator_or_allow_list",
                    "allowedRoleIds" to listOf("10", "20"),
                    "AllowedUserIDs" to listOf("30"),
                )
            )
        )

        assertEquals(
            DiscordCommandPermission(
                mode = DiscordCommandPermissionMode.LINKED_OPERATOR_OR_ALLOW_LIST,
                allowedRoleIDs = setOf("10", "20"),
                allowedUserIDs = setOf("30"),
            ),
            loaded?.get("run"),
        )
    }
}
