package net.ririfa.fabricord.database

import dev.swiftstorm.akkaradb.engine.AkkDSL
import dev.swiftstorm.akkaradb.engine.PackedTable
import dev.swiftstorm.akkaradb.engine.StartupMode
import dev.swiftstorm.akkaradb.format.akk.parity.RSParityCoder
import net.ririfa.fabricord.util.DBDir
import java.util.*

object DataBase {
    lateinit var mcDiscordLink: PackedTable<MCDiscordLink, UUID>
    lateinit var fabricordPlayer: PackedTable<FabricordPlayer, UUID>

    fun initialize() {
        mcDiscordLink = AkkDSL.open(DBDir.resolve("linking"), StartupMode.ULTRA_FAST) {
            m = 2; parityCoder = RSParityCoder(2); debug = true
        }
        fabricordPlayer = AkkDSL.open(DBDir.resolve("players"), StartupMode.ULTRA_FAST) {
            m = 2; parityCoder = RSParityCoder(2); debug = true
        }
    }

    fun terminate() {
        mcDiscordLink.close()
    }

    fun linkUser(mc: UUID, discord: Long) {
        mcDiscordLink.put(MCDiscordLink(mcUUID = mc, discordId = discord))
    }

    fun getDiscordId(mc: UUID): Long? {
        return mcDiscordLink.get(mc)?.discordId
    }

    fun getMinecraftUUID(discord: Long): UUID? {
        return mcDiscordLink.firstOrNull { discordId == discord }?.mcUUID
    }

    @JvmStatic
    fun isUserLinked(mc: UUID): Boolean {
        return mcDiscordLink.get(mc) != null
    }

    fun insertPlayer(mc: UUID, isOp: Boolean) {
        fabricordPlayer.put(FabricordPlayer(mcUUID = mc, isOp = isOp))
    }

    fun isOp(mc: UUID): Boolean? {
        return fabricordPlayer.get(mc)?.isOp
    }
}