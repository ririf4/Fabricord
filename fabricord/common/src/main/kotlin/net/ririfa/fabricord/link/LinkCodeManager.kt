package net.ririfa.fabricord.link

import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class LinkCodeManager(
    private val lifetime: Duration = Duration.ofMinutes(5),
    private val clock: Clock = Clock.systemUTC(),
) {
    private data class PendingLink(val minecraftUuid: UUID, val expiresAt: Instant)

    private val random = SecureRandom()
    private val byCode = ConcurrentHashMap<String, PendingLink>()
    private val byUuid = ConcurrentHashMap<UUID, String>()

    fun issue(minecraftUuid: UUID): String {
        cleanupExpired()
        byUuid.remove(minecraftUuid)?.let(byCode::remove)

        val code = generateSequence(::newCode).first { candidate ->
            byCode.putIfAbsent(
                candidate,
                PendingLink(minecraftUuid, clock.instant().plus(lifetime))
            ) == null
        }
        byUuid[minecraftUuid] = code
        return code
    }

    fun consume(rawCode: String): UUID? {
        val code = rawCode.trim().uppercase()
        val pending = byCode.remove(code) ?: return null
        byUuid.remove(pending.minecraftUuid, code)
        return pending.minecraftUuid.takeIf { pending.expiresAt.isAfter(clock.instant()) }
    }

    private fun cleanupExpired() {
        val now = clock.instant()
        byCode.entries.removeIf { (code, pending) ->
            if (pending.expiresAt.isAfter(now)) {
                false
            } else {
                byUuid.remove(pending.minecraftUuid, code)
                true
            }
        }
    }

    private fun newCode(): String = buildString(6) {
        repeat(6) { append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]) }
    }

    private companion object {
        const val CODE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    }
}
