package net.ririfa.fabricord.database

import dev.swiftstorm.akkaradb.engine.AkkDSL
import dev.swiftstorm.akkaradb.engine.PackedTable
import dev.swiftstorm.akkaradb.engine.StartupMode
import net.ririfa.fabricord.Fabricord

class Database {
    lateinit var db: PackedTable<FabricordDBTable>

    fun start() {
        db = AkkDSL.open(Fabricord.modDir, StartupMode.ULTRA_FAST, {
            m = 2
        })
    }
}