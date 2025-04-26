package net.ririfa.fabricord

import net.ririfa.fabricord.tables.Groups
import net.ririfa.fabricord.util.DBAll
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import java.io.File

object DataManager {
    lateinit var db: Database
    lateinit var memDb: Database

    fun start(): Boolean {
        return try {
            // database.mv.db
            val dbFile = File(ModDir.toFile(), "database").absoluteFile.path
            val fileDbUrl = "jdbc:h2:$dbFile;AUTO_SERVER=TRUE"
            val memoryDbUrl = "jdbc:h2:mem:bulletinboard;DB_CLOSE_DELAY=-1"

            db = Database.connect(fileDbUrl, driver = "org.h2.Driver", user = "sa", password = "")
            memDb = Database.connect(memoryDbUrl, driver = "org.h2.Driver", user = "sa", password = "")

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
            memDb.connector().close()
        } catch (e: Exception) {
            Logger.warn("Failed to close database connections: ${e.message}")
        }
    }

    private fun createRequiredTables() {
        DBAll {
            SchemaUtils.create(Groups)
        }
    }
}