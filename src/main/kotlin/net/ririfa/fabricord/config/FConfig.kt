package net.ririfa.fabricord.config

import net.ririfa.fabricord.util.Logger
import net.ririfa.fabricord.util.MessageStyle
import net.ririfa.yacla.loader.FieldLoader
import net.ririfa.yacla.schema.FieldDefBuilder
import net.ririfa.yacla.schema.YaclaSchema

data class FConfig(
    @JvmField
    val botToken: String?,
    @JvmField
    val logChannelID: String?,

    @JvmField
    var willSends: List<SendableEvent>?,

    @JvmField
    var botActivityMessage: String?,
    @JvmField
    var botActivityStatus: String?,
    @JvmField
    var botOnlineStatus: String?,

    @JvmField
    val serverStartMessage: String?,
    @JvmField
    val serverStopMessage: String?,

    @JvmField
    val playerJoinMessage: String?,
    @JvmField
    val playerLeaveMessage: String?,

    @JvmField
    var messageStyle: MessageStyle?,

    @JvmField
    val useUserPermissionsForMention: Boolean,
    @JvmField
    val blockAllMentions: Boolean,
    @JvmField
    val mentionBlockedUserID: Set<String>?,
    @JvmField
    val mentionBlockedRoleID: Set<String>?,

    @JvmField
    val consoleLogChannelID: String?,
) {
    fun sendChat(): Boolean = willSends?.contains(SendableEvent.Chat) == true
}

object FConfigSchema : YaclaSchema<FConfig> {
    override fun configure(def: FieldDefBuilder<FConfig>) {
        def.field(FConfig::botToken) {
            loader(BlankToNullLoader)
            validate {
                if (it == null) Logger.warn("Bot token is not set in config, Fabricord will not function properly.")
            }
        }
        def.field(FConfig::logChannelID) {
            loader(BlankToNullLoader)
            validate {
                if (it == null) Logger.warn("Log channel ID is not set in config, some features may not work properly.")
            }
        }
        def.field(FConfig::willSends) {
            loader(SendableEventListLoader)
        }
        def.field(FConfig::botActivityMessage) {
            loader(BlankToNullLoader)
        }
        def.field(FConfig::botActivityStatus) {
            loader(BlankToNullLoader)
        }
        def.field(FConfig::botOnlineStatus) {
            loader(BlankToNullLoader)
        }
        def.field(FConfig::serverStartMessage) {
            loader(BlankToNullLoader)
        }
        def.field(FConfig::serverStopMessage) {
            loader(BlankToNullLoader)
        }
        def.field(FConfig::playerJoinMessage) {
            loader(BlankToNullLoader)
        }
        def.field(FConfig::playerLeaveMessage) {
            loader(BlankToNullLoader)
        }
        def.field(FConfig::messageStyle) {
            loader(StringToMessageStyleLoader)
            default(MessageStyle.CLASSIC)
        }
        def.field(FConfig::useUserPermissionsForMention) {
            default(false)
        }
        def.field(FConfig::blockAllMentions) {
            default(false)
        }
        def.field(FConfig::mentionBlockedUserID) {
            loader(ListToSetLoader)
        }
        def.field(FConfig::mentionBlockedRoleID) {
            loader(ListToSetLoader)
        }
    }
}

object StringToMessageStyleLoader : FieldLoader {
    override fun load(raw: Any?): Any? {
        val str = (raw as? String)?.trim()
            ?: throw IllegalArgumentException("Expected a String for MessageStyle, got: ${raw?.javaClass?.name}")

        return try {
            MessageStyle.of(str)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid MessageStyle: '$str'", e)
        }
    }
}

object SendableEventListLoader : FieldLoader {
    override fun load(raw: Any?): Any {
        val rawList = raw as? List<*>
            ?: throw IllegalArgumentException("Expected a List for SendableEvent, got: ${raw?.javaClass?.name}")

        return rawList.map {
            val str = (it as? String)?.trim()
                ?: throw IllegalArgumentException("Expected string elements in SendableEvent list")

            try {
                SendableEvent.valueOf(str)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid SendableEvent: '$str'", e)
            }
        }
    }
}

object BlankToNullLoader : FieldLoader {
    override fun load(raw: Any?): Any? {
        return when (raw) {
            is String -> raw.ifBlank { null }
            else -> raw
        }
    }
}

object ListToSetLoader : FieldLoader {
    override fun load(raw: Any?): Any? {
        if (raw !is List<*>) return null
        return raw.mapNotNull { it?.toString() }.toSet()
    }
}

enum class SendableEvent {
    Chat, Advancement, Death, Join, Leave
}