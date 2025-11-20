import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.swiftstorm.akkaradb.plugin.akkara
import net.fabricmc.loom.task.RemapJarTask

plugins {
	alias(libs.plugins.kotlin)
	alias(libs.plugins.loom)
	alias(libs.plugins.akkara)
	alias(libs.plugins.shadowJar)
}

repositories {
	mavenCentral()
	maven("https://maven.fabricmc.net/") { name = "FabricMC" }
	maven("https://repo.swiftstorm.dev/maven2/") { name = "SwiftStorm Repository" }
}

val shade: Configuration by configurations.creating {
	extendsFrom(configurations.runtimeOnly.get())
}

dependencies {
	minecraft(libs.minecraft)
	mappings(libs.fabric.yarn)
	modImplementation(libs.fabric.api)
	modImplementation(libs.fabric.loader)
	modImplementation(libs.fabric.kotlin)

	modImplementation(libs.langman.core)
	modImplementation(libs.langman.yaml)
	modImplementation(libs.yacla.core)
	modImplementation(libs.yacla.yaml)
	modImplementation(libs.jda) {
		exclude(group = "net.java.dev.jna", module = "jna")
	}

	akkara(libs.versions.akkaradb.get())

	shade(libs.jda)
}

tasks.named<RemapJarTask>("remapJar") {
	finalizedBy("finalJar")
}

tasks.register<ShadowJar>("finalJar") {
	archiveClassifier.set("final")
	configurations = listOf(shade)
	isZip64 = true

	relocate("net.dv8tion.jda", "net.ririfa.shadowed.jda")
}