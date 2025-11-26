package net.ririfa.fabricord.util

object CodeGenerator {
    private val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private val RANDOM = java.util.concurrent.ThreadLocalRandom.current()

    fun generate(length: Int = 6): String =
        buildString(length) {
            repeat(length) {
                append(chars[RANDOM.nextInt(chars.length)])
            }
        }
}
