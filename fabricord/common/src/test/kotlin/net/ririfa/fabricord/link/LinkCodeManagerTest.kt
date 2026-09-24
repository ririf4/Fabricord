package net.ririfa.fabricord.link

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LinkCodeManagerTest {
    @Test
    fun `code can only be consumed once`() {
        val manager = LinkCodeManager()
        val minecraftUuid = UUID.randomUUID()
        val code = manager.issue(minecraftUuid)

        assertEquals(minecraftUuid, manager.consume(code.lowercase()))
        assertNull(manager.consume(code))
    }
}
