@file:Suppress("FunctionName")

package net.ririfa.fabricord.util

import net.ririfa.fabricord.database.DataManager
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction

fun <T> DB(block: Transaction.() -> T): T {
    return transaction(DataManager.db, block)
}