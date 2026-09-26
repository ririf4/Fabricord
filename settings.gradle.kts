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

fun safeInclude(name: String, path: String) {
    val dir = file(path)
    if (dir.exists()) {
        include(name)
        project(":$name").projectDir = dir
    }
}

safeInclude("26.1.2", "fabricord/versions/26.1.2")
safeInclude("1.21.11", "fabricord/versions/1.21.11")
safeInclude("1.21.8", "fabricord/versions/1.21.8")
safeInclude("1.21.5", "fabricord/versions/1.21.5")
safeInclude("1.20.4", "fabricord/versions/1.20.4")
safeInclude("1.20.2", "fabricord/versions/1.20.2")
safeInclude("1.20.1", "fabricord/versions/1.20.1")
safeInclude("1.19.4", "fabricord/versions/1.19.4")
safeInclude("1.19.2", "fabricord/versions/1.19.2")
safeInclude("1.19", "fabricord/versions/1.19")
safeInclude("1.18.2", "fabricord/versions/1.18.2")
safeInclude("1.17.1", "fabricord/versions/1.17.1")
safeInclude("1.16.5", "fabricord/versions/1.16.5")
safeInclude("1.15.2", "fabricord/versions/1.15.2")

rootProject.name = "fabricord"
