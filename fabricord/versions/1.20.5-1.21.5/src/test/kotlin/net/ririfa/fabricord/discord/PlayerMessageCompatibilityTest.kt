package net.ririfa.fabricord.discord

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerMessageCompatibilityTest {
    @Test
    fun `player messages use the overload supported by Minecraft 1_21_1`() {
        // The one-argument method_64398 from 1.21.5 is absent in 1.21.1.
        val classes = listOf(
            "net/ririfa/fabricord/discord/MinecraftDiscordPlatform",
            "net/ririfa/fabricord/command/FabricordCommands",
            "net/ririfa/fabricord/Fabricord",
        )
        classes.forEach { className ->
            val calls = mutableListOf<String>()
            val stream = requireNotNull(javaClass.classLoader.getResourceAsStream(
                "$className.class",
            ))
            stream.use {
                ClassReader(it).accept(object : ClassVisitor(Opcodes.ASM9) {
                    override fun visitMethod(
                        access: Int,
                        name: String,
                        descriptor: String,
                        signature: String?,
                        exceptions: Array<out String>?,
                    ): MethodVisitor? {
                        return object : MethodVisitor(Opcodes.ASM9) {
                            override fun visitMethodInsn(
                                opcode: Int,
                                owner: String,
                                name: String,
                                descriptor: String,
                                isInterface: Boolean,
                            ) {
                                if (owner == "net/minecraft/server/level/ServerPlayer" && name == "sendSystemMessage") {
                                    calls += descriptor
                                }
                            }
                        }
                    }
                }, ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
            }
            assertEquals(
                setOf("(Lnet/minecraft/network/chat/Component;Z)V"),
                calls.toSet(),
                "$className must use the overload supported by Minecraft 1.21.1",
            )
        }
    }
}
