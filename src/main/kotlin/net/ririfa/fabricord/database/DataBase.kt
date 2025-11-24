package net.ririfa.fabricord.database

import dev.swiftstorm.akkaradb.engine.AkkDSL
import dev.swiftstorm.akkaradb.engine.PackedTable
import dev.swiftstorm.akkaradb.engine.StartupMode
import dev.swiftstorm.akkaradb.format.akk.parity.RSParityCoder
import net.ririfa.fabricord.util.DBDir
import java.util.*

object DataBase {
    val dataBase: PackedTable<FTable> by lazy {
        AkkDSL.open<FTable>(DBDir, StartupMode.ULTRA_FAST) {
            m = 2; parityCoder = RSParityCoder(2)
        }
    }

    fun isUserLinked(mcUUID: UUID): Boolean {
        return dataBase.exists {
            mcDiscordLink.mcUUID == mcUUID
        }
    }
}