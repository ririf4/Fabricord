package net.ririfa.fabricord.util

import net.ririfa.fabricord.database.tables.Group

sealed class GroupSearchResult {
    data class Found(val group: Group) : GroupSearchResult()
    object NotFound : GroupSearchResult()
    object MultipleFound : GroupSearchResult()
}
