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
    val logChannels: Map<LogChannelType, Set<String>>?,

    @JvmField
    var willSends: List<SendableEvent>?,

    @JvmField
    var botActivityMessage: String?,
    @JvmField
    var botActivityStatus: String?,
    @JvmField
    var botOnlineStatus: String?,

    @JvmField
    var serverStartMessage: String?,
    @JvmField
    var serverStopMessage: String?,

    @JvmField
    val playerJoinMessage: String?,
    @JvmField
    val playerLeaveMessage: String?,

    @JvmField
    var messageStyle: MessageStyle?,

    @JvmField
    val useUserPermissionForMentions: Boolean,
    @JvmField
    val blockAllMentions: Boolean,
    @JvmField
    val mentionBlockedUserIDs: Set<String>?,
    @JvmField
    val mentionBlockedRoleIDs: Set<String>?,

    //TODO: 機能追加
    @JvmField
    val disableDeathMessages: Boolean,
    @JvmField
    val disableAdvancementMessages: Boolean,

    @JvmField
    val ignoredAdvancements: Set<String>?,

    @JvmField
    val consoleLogChannelID: String?,
) {
    fun sendChat(): Boolean = willSends?.contains(SendableEvent.Chat) == true
}

object FConfigSchema : YaclaSchema<FConfig> {
    override fun configure(def: FieldDefBuilder<FConfig>) {
        def.field(FConfig::botToken)
            .loader(BlankToNullLoader)
            .ifNull { _, _ ->
                Logger.warn("Bot token is not set in config, Fabricord will not function properly.")
            }
        def.field(FConfig::logChannels)
            .loader(LogChannelsLoader)
        def.field(FConfig::consoleLogChannelID)
            .loader(BlankToNullLoader)
        def.field(FConfig::willSends)
            .loader(SendableEventListLoader)
        def.field(FConfig::botActivityMessage)
            .loader(BlankToNullLoader)
        def.field(FConfig::botActivityStatus)
            .loader(BlankToNullLoader)
        def.field(FConfig::botOnlineStatus)
            .loader(BlankToNullLoader)

        def.field(FConfig::serverStartMessage)
            .loader(BlankToNullLoaderWithDefault(":white_check_mark: **Server has started!**"))
        def.field(FConfig::serverStopMessage)
            .loader(BlankToNullLoaderWithDefault(":octagonal_sign: **Server has stopped!**"))
        def.field(FConfig::playerJoinMessage)
            .loader(BlankToNullLoaderWithDefault("%player% joined the server"))
        def.field(FConfig::playerLeaveMessage)
            .loader(BlankToNullLoaderWithDefault("%player% left the server"))

        def.field(FConfig::messageStyle)
            .loader(StringToMessageStyleLoader)
        def.field(FConfig::useUserPermissionForMentions)
            .default(false)
        def.field(FConfig::blockAllMentions)
            .default(false)
        def.field(FConfig::mentionBlockedUserIDs)
            .loader(ListToSetLoader())
        def.field(FConfig::ignoredAdvancements)
            .loader(ListToSetLoader())
        def.field(FConfig::mentionBlockedRoleIDs)
            .loader(ListToSetLoader())
    }
}

object LogChannelsLoader : FieldLoader {
    override fun load(raw: Any?): Map<LogChannelType, Set<String>>? {
        val map = raw as? Map<*, *> ?: return null

        return map.mapNotNull { (key, value) ->
            val type = LogChannelType.valueOf(key.toString())
            val channels = (value as? List<*>)
                ?.mapNotNull { it?.toString() }
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?.takeIf { it.isNotEmpty() } // 空のSetは除外
                ?: return@mapNotNull null

            type to channels
        }.toMap().takeIf { it.isNotEmpty() }
    }
}

object StringToMessageStyleLoader : FieldLoader {
    override fun load(raw: Any?): Any {
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

class BlankToNullLoaderWithDefault(val defaultValue: String) : FieldLoader {
    override fun load(raw: Any?): Any? {
        return when (raw) {
            is String -> {
                if (raw.isBlank()) {
                    null
                } else if (raw.uppercase() == "DEFAULT") {
                    defaultValue
                } else {
                    raw
                }
            }

            else -> raw
        }
    }
}

class ListToSetLoader(val emptyToNull: Boolean = true) : FieldLoader {
    override fun load(raw: Any?): Any? {
        if (raw !is List<*>) return null
        val set = raw.mapNotNull {
            it?.toString()?.takeIf { str ->
                if (emptyToNull) str.isNotBlank() else str.isNotEmpty()
            }
        }.toSet()
        return set.ifEmpty { null }
    }
}

enum class SendableEvent {
    Chat, Advancement, Death, Join, Leave
}

enum class LogChannelType {
    Default, Chat, Advancement, Death, Join, Leave, ServerStart, ServerStop
}