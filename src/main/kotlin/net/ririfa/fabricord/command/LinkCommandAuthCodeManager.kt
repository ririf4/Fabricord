package net.ririfa.fabricord.command

import net.ririfa.cask.cask
import net.ririfa.cask.ttlMinutes
import net.ririfa.fabricord.util.CodeGenerator
import java.util.*

object LinkCommandAuthCodeManager {
    val UtS = cask<UUID, String> {
        ttlMinutes = 5L  // 5 minutes
        shareGcExecutor(true)
    }

    val StU = cask<String, UUID> {
        ttlMinutes = 5 * 60L  // 5 minutes
        shareGcExecutor(true)
        onEvict { _, uuid ->
            uuid?.let { UtS.invalidate(it) }
        }
    }

    fun issueLinkCode(uuid: UUID): String {
        UtS.get(uuid)?.let { _ ->
            UtS.invalidate(uuid)
        }

        var code: String
        do {
            code = CodeGenerator.generate()
        } while (StU.get(code) != null)

        UtS.put(uuid, code)
        StU.put(code, uuid)

        return code
    }

    fun consume(code: String): UUID? {
        val uuid = StU.get(code) ?: return null
        StU.invalidate(code)
        UtS.invalidate(uuid)
        return uuid
    }

    fun peekUuid(code: String): UUID? =
        StU.get(code)

    fun peekCode(uuid: UUID): String? =
        UtS.get(uuid)
}