package net.ririfa.fabricord.util

inline fun <reified T> MutableSet<T>.toggle(element: T): Boolean =
    if (add(element)) true else remove(element).let { false }
