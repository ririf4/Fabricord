import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.google.gson.GsonBuilder
import net.fabricmc.loom.task.RemapJarTask

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.loom)
    alias(libs.plugins.shadowJar)
}

group = "net.ririfa"
version = "5.0.0"
description = "A modern message style like DiscordSRV will be reproduced as a Fabric version mod."

val authors = listOf(
    "RiriFa"
)
val contact = mapOf(
    "sources" to "https://github.com/ririf4/Fabricord",
    "issues" to "https://github.com/ririf4/Fabricord/issues"
)
val license = "Apache-2.0"

repositories {
    mavenCentral { name="Maven Central" }
    maven("https://repo.ririfa.net/maven2/") { name="RiriFa Repository" }
    maven("https://maven.fabricmc.net/") { name="FabricMC" }
}

val shade: Configuration by configurations.creating

dependencies {
    minecraft(libs.minecraft)
    mappings(libs.fabric.yarn)
    modImplementation(libs.bundles.fabrics)
    modImplementation(libs.bundles.fabricord) {
        exclude(group = "net.java.dev.jna", module = "jna")
    }

    shade(libs.bundles.fabricord) {
        exclude(group = "net.java.dev.jna", module = "jna")
    }
}

val minecraftVersionCompatibility = "1.21.5"
val fabricLoaderVersionCompatibility = ""
val fabricLanguageKotlinVersionCompatibility = ""

fun generateFabricModJson(): String {
    val json = mapOf(
        "schemaVersion" to 1,
        "id" to rootProject.name.lowercase(),
        "version" to version,
        "name" to rootProject.name,
        "description" to description,
        "authors" to authors,
        "contact" to contact,
        "license" to license,
        "environment" to "server",
        "entrypoints" to mapOf(
            "server" to listOf(
                mapOf(
                    "adapter" to "kotlin",
                    "value" to "net.ririfa.fabricord.Fabricord"
                )
            )
        ),
        "depends" to mapOf(
            "fabricloader" to fabricLoaderVersionCompatibility,
            "minecraft" to minecraftVersionCompatibility,
            "java" to "21",
            "fabric-api" to "*",
            "fabric-language-kotlin" to fabricLanguageKotlinVersionCompatibility
        )
    )
    return GsonBuilder().setPrettyPrinting().create().toJson(json)
}

tasks.named<RemapJarTask>("remapJar") {
    dependsOn("clean")
    mustRunAfter("clean")
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

    val jsonText = generateFabricModJson()
    from(resources.text.fromString(jsonText)) {
        into("")
        rename { "fabric.mod.json" }
    }

    relocate("net.dv8tion.jda", "net.ririfa.shadowed.jda")
    relocate("net.java.dev.jna", "net.ririfa.shadowed.jna")
    relocate("org.yaml.snakeyaml", "net.ririfa.shadowed.yaml")
    relocate("org.jetbrains.exposed", "net.ririfa.shadowed.exposed")

    exclude("net/dv8tion/jda/api/audio/**")
    exclude("net/dv8tion/jda/internal/audio/**")
    exclude("tomp2p/**")
    exclude("com/sun/jna/**")
    exclude("kotlin/**")
    exclude("org/jetbrains/kotlin/**")
    exclude("kotlinx/**")
    exclude("club/minnced/opus/**")
}