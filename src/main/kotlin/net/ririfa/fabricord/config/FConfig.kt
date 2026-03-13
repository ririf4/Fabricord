package net.ririfa.fabricord.config

import net.ririfa.fabricord.util.MessageStyle
import net.ririfa.yacla.annotation.*
import net.ririfa.yacla.loader.FieldLoader

data class FConfig(
    @BlankToNull
    @Warn("Bot token is not set in config, Fabricord will not function properly.")
    @JvmField val botToken: String? = null,

    @Loader(LogChannelsLoader::class)
    @JvmField val logChannels: Map<LogChannelType, Set<String>>? = null,

    @EnumList
    @JvmField val willSends: List<SendableEvent>? = null,

    @BlankToNull @JvmField val botActivityMessage: String? = null,
    @BlankToNull @JvmField val botActivityStatus: String? = null,
    @BlankToNull @JvmField val botOnlineStatus: String? = null,

    @BlankToNull @JvmField val serverStartMessage: String? = null,
    @BlankToNull @JvmField val serverStopMessage: String? = null,

    @BlankToNull @JvmField val playerJoinMessage: String? = null,
    @BlankToNull @JvmField val playerLeaveMessage: String? = null,

    @Loader(MessageStyleLoader::class)
    @JvmField val messageStyle: MessageStyle? = null,

    @JvmField val useUserPermissionForMentions: Boolean = false,
    @JvmField val blockAllMentions: Boolean = false,

    @SetOf @JvmField val mentionBlockedUserIDs: Set<String>? = null,
    @SetOf @JvmField val mentionBlockedRoleIDs: Set<String>? = null,

    @JvmField val disableDeathMessages: Boolean = false,
    @JvmField val disableAdvancementMessages: Boolean = false,

    @SetOf @JvmField val ignoredAdvancements: Set<String>? = null,

    @BlankToNull @JvmField val consoleLogChannelID: String? = null,

    @BlankToNull @JvmField val playerCountActivityFormat: String? = null,

    @SetOf @JvmField val opSyncRoleIDs: Set<String>? = null,
) {
    fun sendChat(): Boolean = willSends?.contains(SendableEvent.Chat) == true
}

object LogChannelsLoader : FieldLoader {
    override fun load(raw: Any?): Map<LogChannelType, Set<String>>? {
        val map = raw as? Map<*, *> ?: return null

        return map.mapNotNull { (key, value) ->
            val type = runCatching { LogChannelType.valueOf(key.toString()) }.getOrNull()
                ?: return@mapNotNull null
            val channels = (value as? List<*>)
                ?.mapNotNull { it?.toString() }
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?.takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null

            type to channels
        }.toMap().takeIf { it.isNotEmpty() }
    }
}

object MessageStyleLoader : FieldLoader {
    override fun load(raw: Any?): Any? {
        val str = (raw as? String)?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return MessageStyle.of(str)
    }
}

enum class SendableEvent {
    Chat, Advancement, Death, Join, Leave
}

enum class LogChannelType {
    Default, Chat, Advancement, Death, Join, Leave, ServerStart, ServerStop
}
