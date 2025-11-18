package net.ririfa.fabricord.database

import java.util.*

data class FabricordDBTable(
    val dmLinkTable: DMLinkData
)

data class DMLinkData(
    val discordId: Long,
    val minecraftUUID: UUID
)