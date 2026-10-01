package net.ririfa.fabricord.config

import net.ririfa.fabricord.util.MessageStyle
import net.ririfa.yacla.annotation.*
import net.ririfa.yacla.loader.FieldLoader
import java.util.Locale

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

    @BlankToNull @JvmField val minecraftToDiscordMessageFormat: String? = null,
    @BlankToNull @JvmField val discordToMinecraftMessageFormat: String? = null,
    @BlankToNull @JvmField val playerDeathMessage: String? = null,
    @BlankToNull @JvmField val playerAdvancementMessage: String? = null,

    @Loader(DiscordCommandPermissionsLoader::class)
    @JvmField val discordCommandPermissions: Map<String, DiscordCommandPermission>? = null,
) {
    fun sendChat(): Boolean = willSends?.contains(SendableEvent.Chat) == true

    fun commandPermission(command: String): DiscordCommandPermission =
        discordCommandPermissions
            ?.entries
            ?.firstOrNull { (name, _) -> name.equals(command, ignoreCase = true) }
            ?.value
            ?: DiscordCommandPermission()
}

data class DiscordCommandPermission(
    val mode: DiscordCommandPermissionMode = DiscordCommandPermissionMode.LINKED_OPERATOR,
    val allowedRoleIDs: Set<String> = emptySet(),
    val allowedUserIDs: Set<String> = emptySet(),
)

enum class DiscordCommandPermissionMode {
    LINKED_OPERATOR,
    ALLOW_LIST,
    LINKED_OPERATOR_OR_ALLOW_LIST,
    EVERYONE,
    DISABLED,
}

class DiscordCommandPermissionsLoader : FieldLoader {
    override fun load(raw: Any?): Map<String, DiscordCommandPermission>? {
        val permissions = raw as? Map<*, *> ?: return null
        return permissions.mapNotNull { (rawCommand, rawPermission) ->
            val command = rawCommand?.toString()?.trim()?.takeIf(String::isNotEmpty) ?: return@mapNotNull null
            val fields = rawPermission as? Map<*, *> ?: return@mapNotNull null
            val modeName = fields.value("Mode")?.toString()?.trim()?.uppercase(Locale.ROOT)
            val mode = modeName
                ?.let { name -> DiscordCommandPermissionMode.entries.firstOrNull { it.name == name } }
                ?: DiscordCommandPermissionMode.LINKED_OPERATOR
            command.lowercase(Locale.ROOT) to DiscordCommandPermission(
                mode = mode,
                allowedRoleIDs = fields.stringSet("AllowedRoleIDs"),
                allowedUserIDs = fields.stringSet("AllowedUserIDs"),
            )
        }.toMap().takeIf(Map<*, *>::isNotEmpty)
    }

    private fun Map<*, *>.value(name: String): Any? =
        entries.firstOrNull { (key, _) -> key?.toString()?.equals(name, ignoreCase = true) == true }?.value

    private fun Map<*, *>.stringSet(name: String): Set<String> =
        (value(name) as? Iterable<*>)
            ?.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
            ?.toSet()
            .orEmpty()
}

class LogChannelsLoader : FieldLoader {
    override fun load(raw: Any?): Map<LogChannelType, Set<String>>? {
        val map = raw as? Map<*, *> ?: return null

        return map.mapNotNull { (key, value) ->
            val type = LogChannelType.entries.firstOrNull {
                it.name.equals(key.toString(), ignoreCase = true)
            }
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

class MessageStyleLoader : FieldLoader {
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
