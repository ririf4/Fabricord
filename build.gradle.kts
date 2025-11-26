import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.swiftstorm.akkaradb.plugin.akkara
import net.fabricmc.loom.task.RemapJarTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
	// === Minecraft and Fabric ===
	minecraft(libs.minecraft)
	mappings(libs.fabric.yarn)
	modImplementation(libs.fabric.api)
	modImplementation(libs.fabric.loader)
	modImplementation(libs.fabric.kotlin)

	// === RiriFa Libs ===
	modImplementation(libs.langman.core)
	modImplementation(libs.langman.yaml)
	modImplementation(libs.yacla.core)
	modImplementation(libs.yacla.yaml)
	modImplementation(libs.cask)
	akkara(libs.versions.akkaradb.get(), "modImplementation")

	// === Other Libs ===
	modImplementation(libs.snakeyaml)
	modImplementation(libs.jda)

	// === Shadowed Libs ===
	shade(libs.jda) {
		exclude(group = "net.java.dev.jna", module = "jna")
	}
}

loom {
	accessWidenerPath = file("src/main/resources/fabricord.accesswidener")
}

java {
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
	jvmToolchain {
		languageVersion.set(JavaLanguageVersion.of(21))
	}
}

tasks.withType<KotlinCompile> {
	compilerOptions {
		jvmTarget.set(JvmTarget.JVM_21)
	}
}

tasks.withType<JavaCompile> {
	options.release.set(21)
}

tasks.named<RemapJarTask>("remapJar") {
	finalizedBy("finalJar")
}

tasks.register<ShadowJar>("finalJar") {
	archiveClassifier.set("final")
	configurations = listOf(shade)
	isZip64 = true

	doFirst {
		val remapped = tasks.named<RemapJarTask>("remapJar").get().archiveFile.get().asFile
		from(zipTree(remapped))
	}

	relocate("net.dv8tion.jda", "net.ririfa.shadowed.jda")

	exclude("net/dv8tion/jda/api/audio/**")
	exclude("net/dv8tion/jda/internal/audio/**")
	exclude("tomp2p/**")
	exclude("com/sun/jna/**")
	exclude("kotlin/**")
	exclude("org/jetbrains/kotlin/**")
	exclude("kotlinx/**")
	exclude("club/minnced/opus/**")
	exclude("com/fasterxml/jackson/**")
	exclude("org/apache/commons/**")
	exclude("org/slf4j/**")
}