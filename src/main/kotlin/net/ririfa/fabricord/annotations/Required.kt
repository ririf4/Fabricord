package net.ririfa.fabricord.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY)
annotation class Required(val soft: Boolean = false, val named: String)