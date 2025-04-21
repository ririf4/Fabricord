package net.ririfa.fabricord.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Indexed(val value: Int)