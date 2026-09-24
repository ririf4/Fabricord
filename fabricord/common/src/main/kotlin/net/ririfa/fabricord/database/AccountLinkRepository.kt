@file:Suppress("SqlResolve", "SqlNoDataSourceInspection")

package net.ririfa.fabricord.database

import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID

data class AccountLink(
    val minecraftUuid: UUID,
    val discordUserId: Long,
)

class AccountLinkRepository(private val databaseFile: Path) : AutoCloseable {
    private val lock = Any()
    private var connection: Connection? = null

    fun initialize() = synchronized(lock) {
        if (connection != null) return@synchronized

        Files.createDirectories(databaseFile.parent)
        Class.forName("org.sqlite.JDBC")
        connection = DriverManager.getConnection("jdbc:sqlite:${databaseFile.toAbsolutePath()}").also { db ->
            db.createStatement().use { statement ->
                statement.execute("PRAGMA journal_mode = WAL")
                statement.execute("PRAGMA synchronous = NORMAL")
                statement.execute("PRAGMA busy_timeout = 5000")
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS account_links (
                        minecraft_uuid TEXT PRIMARY KEY NOT NULL,
                        discord_user_id INTEGER NOT NULL UNIQUE
                    )
                    """.trimIndent()
                )
            }
        }
    }

    fun link(minecraftUuid: UUID, discordUserId: Long) = synchronized(lock) {
        val db = requireConnection()
        val previousAutoCommit = db.autoCommit
        db.autoCommit = false
        try {
            db.prepareStatement(
                "DELETE FROM account_links WHERE minecraft_uuid = ? OR discord_user_id = ?"
            ).use { statement ->
                statement.setString(1, minecraftUuid.toString())
                statement.setLong(2, discordUserId)
                statement.executeUpdate()
            }
            db.prepareStatement(
                "INSERT INTO account_links (minecraft_uuid, discord_user_id) VALUES (?, ?)"
            ).use { statement ->
                statement.setString(1, minecraftUuid.toString())
                statement.setLong(2, discordUserId)
                statement.executeUpdate()
            }
            db.commit()
        } catch (error: Exception) {
            db.rollback()
            throw error
        } finally {
            db.autoCommit = previousAutoCommit
        }
    }

    fun unlinkMinecraft(minecraftUuid: UUID): Boolean = synchronized(lock) {
        requireConnection().prepareStatement(
            "DELETE FROM account_links WHERE minecraft_uuid = ?"
        ).use { statement ->
            statement.setString(1, minecraftUuid.toString())
            statement.executeUpdate() > 0
        }
    }

    fun unlinkDiscord(discordUserId: Long): Boolean = synchronized(lock) {
        requireConnection().prepareStatement(
            "DELETE FROM account_links WHERE discord_user_id = ?"
        ).use { statement ->
            statement.setLong(1, discordUserId)
            statement.executeUpdate() > 0
        }
    }

    fun findDiscordId(minecraftUuid: UUID): Long? = synchronized(lock) {
        requireConnection().prepareStatement(
            "SELECT discord_user_id FROM account_links WHERE minecraft_uuid = ?"
        ).use { statement ->
            statement.setString(1, minecraftUuid.toString())
            statement.executeQuery().use { result ->
                if (result.next()) result.getLong("discord_user_id") else null
            }
        }
    }

    fun findMinecraftUuid(discordUserId: Long): UUID? = synchronized(lock) {
        requireConnection().prepareStatement(
            "SELECT minecraft_uuid FROM account_links WHERE discord_user_id = ?"
        ).use { statement ->
            statement.setLong(1, discordUserId)
            statement.executeQuery().use { result ->
                if (result.next()) UUID.fromString(result.getString("minecraft_uuid")) else null
            }
        }
    }

    fun isLinked(minecraftUuid: UUID): Boolean = findDiscordId(minecraftUuid) != null

    override fun close() = synchronized(lock) {
        connection?.close()
        connection = null
    }

    private fun requireConnection(): Connection =
        checkNotNull(connection) { "AccountLinkRepository has not been initialized" }
}
