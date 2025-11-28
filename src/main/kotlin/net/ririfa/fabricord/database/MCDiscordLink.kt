package net.ririfa.fabricord.database

import dev.swiftstorm.akkaradb.engine.Id
import java.util.*

data class MCDiscordLink(@Id val mcUUID: UUID, val discordId: Long)