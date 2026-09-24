package net.ririfa.fabricord.database

import java.nio.file.Files
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountLinkRepositoryTest {
    @Test
    fun `link is bidirectional and one to one`() {
        val directory = Files.createTempDirectory("fabricord-db-test")
        val repository = AccountLinkRepository(directory.resolve("test.db"))
        val firstMinecraftUser = UUID.randomUUID()
        val secondMinecraftUser = UUID.randomUUID()

        try {
            repository.initialize()
            repository.link(firstMinecraftUser, 10L)

            assertEquals(10L, repository.findDiscordId(firstMinecraftUser))
            assertEquals(firstMinecraftUser, repository.findMinecraftUuid(10L))

            repository.link(secondMinecraftUser, 10L)

            assertNull(repository.findDiscordId(firstMinecraftUser))
            assertEquals(secondMinecraftUser, repository.findMinecraftUuid(10L))
            assertTrue(repository.unlinkDiscord(10L))
            assertFalse(repository.isLinked(secondMinecraftUser))
        } finally {
            repository.close()
            directory.toFile().deleteRecursively()
        }
    }
}
