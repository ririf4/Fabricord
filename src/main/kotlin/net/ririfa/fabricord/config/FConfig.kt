package net.ririfa.fabricord.config

import net.ririfa.yacla.schema.FieldDefBuilder
import net.ririfa.yacla.schema.YaclaSchema

data class FConfig(
    val botToken: String?,
    val logChannelID: String?,
)

object FConfigSchema : YaclaSchema<FConfig> {
    override fun configure(def: FieldDefBuilder<FConfig>) {
        def.field(FConfig::botToken) {
            default(null)
        }
        def.field(FConfig::logChannelID) {
            default(null)
        }
    }
}