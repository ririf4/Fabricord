package net.ririfa.fabricord.config

import net.ririfa.yacla.loader.FieldLoader
import net.ririfa.yacla.schema.FieldDefBuilder
import net.ririfa.yacla.schema.YaclaSchema

data class FConfig(
    @JvmField
    val botToken: String?,
    @JvmField
    val logChannelID: String?,
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
    val blockAllMentions: Boolean,
    @JvmField
    val mentionBlockedUserID: Set<String>?,
    @JvmField
    val mentionBlockedRoleID: Set<String>?,
)

object FConfigSchema : YaclaSchema<FConfig> {
    override fun configure(def: FieldDefBuilder<FConfig>) {
        def.field(FConfig::botToken) {
            default(null)
        }
        def.field(FConfig::logChannelID) {
            default(null)
        }
        def.field(FConfig::botActivityMessage) {
            default(null)
        }
        def.field(FConfig::botActivityStatus) {
            default(null)
        }
        def.field(FConfig::botOnlineStatus) {
            default(null)
        }
        def.field(FConfig::serverStartMessage) {
            default(null)
        }
        def.field(FConfig::serverStopMessage) {
            default(null)
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

object ListToSetLoader : FieldLoader {
    override fun load(raw: Any?): Any? {
        if (raw !is List<*>) return null
        return raw.mapNotNull { it?.toString() }.toSet()
    }
}