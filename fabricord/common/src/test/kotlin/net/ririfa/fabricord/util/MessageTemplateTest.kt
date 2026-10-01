package net.ririfa.fabricord.util

import net.ririfa.fabricord.discord.ServerSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageTemplateTest {
    @Test
    fun `renders modern and legacy placeholders case insensitively`() {
        val rendered = MessageTemplate.render(
            "{PLAYER}: %message% ({missing})",
            mapOf("player" to "Alex", "MESSAGE" to "Hello"),
        )

        assertEquals("Alex: Hello ({missing})", rendered)
    }

    @Test
    fun `provides stable server placeholders`() {
        val values = MessageTemplate.serverValues(
            ServerSnapshot(
                playerCount = 3,
                maxPlayers = 20,
                mspt = 50.0,
                version = "1.21.11",
                worldTime = 6000,
                loadedChunks = 42,
                uptimeMillis = 90_000,
            )
        )

        assertEquals("3/20 20.00 50.00 0h 1m", MessageTemplate.render(
            "{count}/{max} {tps} {mspt} {uptime}",
            values,
        ))
    }
}
