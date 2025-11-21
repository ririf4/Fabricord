package net.ririfa.fabricord.config

import net.ririfa.yacla.loader.FieldLoader
import net.ririfa.yacla.schema.FieldDefBuilder
import net.ririfa.yacla.schema.YaclaSchema

data class FConfig(
    val botToken: String?,
    val logChannelID: String?,
    val blockAllMentions: Boolean,
    val mentionBlockedUserID: Set<String>?,
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