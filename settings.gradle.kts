@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.swiftstorm.dev/maven2/") { name = "SwiftStorm Repository" }
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
    }
}

plugins {
    id("net.fabricmc.fabric-loom-repositories") version "1.16-SNAPSHOT"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.PREFER_SETTINGS

    repositories {
        val central = mavenCentral()

        exclusiveContent {
            forRepositories(central)
            filter {
                includeGroup("org.lwjgl")
            }
        }

        maven("https://repo.swiftstorm.dev/maven2/")
        maven("https://maven.fabricmc.net/")
    }
}

val compatibilityModules = listOf(
    "26.1-26.1.2",
    "1.21.9-1.21.11",
    "1.21.6-1.21.8",
    "1.20.5-1.21.5",
    "1.20.3-1.20.4",
    "1.20.2",
    "1.20-1.20.1",
    "1.19.3-1.19.4",
    "1.19.1-1.19.2",
    "1.19",
    "1.18-1.18.2",
    "1.17-1.17.1",
    "1.16.4-1.16.5",
    "1.16.1-1.16.3",
    "1.16",
    "1.15-1.15.2",
    "1.14.4",
    "1.14.3",
    "1.14-1.14.2",
)

compatibilityModules.forEach { name ->
    val dir = file("fabricord/versions/$name")
    check(dir.isDirectory) { "Missing compatibility module directory: $dir" }
    include(name)
    project(":$name").projectDir = dir
}

rootProject.name = "fabricord"
