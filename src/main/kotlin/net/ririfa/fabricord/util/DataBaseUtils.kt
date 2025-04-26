@file:Suppress("FunctionName")

package net.ririfa.fabricord.util

import net.ririfa.fabricord.DataManager
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction

fun DBAll(block: Transaction.() -> Unit) {
    val list = listOf(
        DataManager.db,
        DataManager.memDb,
    )

    list.forEach {
        transaction(it) { block() }
    }
}

fun <T> DB(block: Transaction.() -> T): T {
    return transaction(DataManager.memDb, block)
}
