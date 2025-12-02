package net.ririfa.fabricord.database

import dev.swiftstorm.akkaradb.engine.AkkDSL
import dev.swiftstorm.akkaradb.engine.PackedTable
import dev.swiftstorm.akkaradb.engine.StartupMode
import dev.swiftstorm.akkaradb.format.akk.parity.RSParityCoder
import net.ririfa.fabricord.util.DBDir
import java.util.*

object DataBase {
    val mcDiscordLink: PackedTable<MCDiscordLink, UUID> by lazy {
        AkkDSL.open(DBDir, StartupMode.ULTRA_FAST) {
            m = 2; parityCoder = RSParityCoder(2)
        }
    }

    fun linkUser(mc: UUID, discord: Long) {
        mcDiscordLink.put(MCDiscordLink(mcUUID = mc, discordId = discord))
    }

    fun getDiscordId(mc: UUID): Long? {
        return mcDiscordLink.get(mc)?.discordId
    }

    fun getMinecraftUUID(discord: Long): UUID? {
        return mcDiscordLink.firstOrNull {
            discordId == discord
        }?.mcUUID
    }

    @JvmStatic
    fun isUserLinked(mc: UUID): Boolean {
        return mcDiscordLink.get(mc) != null
    }
}