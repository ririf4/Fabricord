package net.ririfa.fabricord.database

import net.ririfa.fabricord.Logger
import net.ririfa.fabricord.ModDir
import net.ririfa.fabricord.database.tables.DiscordMinecraftLinks
import net.ririfa.fabricord.util.DB
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import java.io.File

object DataManager {
    lateinit var db: Database

    fun start(): Boolean {
        return try {
            val dbFile = File(ModDir.toFile(), "database").absoluteFile.path
            val fileDbUrl = "jdbc:h2:$dbFile;AUTO_SERVER=TRUE"

            db = Database.connect(fileDbUrl, driver = "org.h2.Driver", user = "sa", password = "")

            createRequiredTables()
            true
        } catch (e: Exception) {
            Logger.error("Failed to start H2 database: ${e.message}")
            false
        }
    }

    fun stop() {
        try {
            db.connector().close()
        } catch (e: Exception) {
            Logger.warn("Failed to close database connection: ${e.message}")
        }
    }

    private fun createRequiredTables() {
        DB {
            SchemaUtils.create(DiscordMinecraftLinks)
        }
    }
}
