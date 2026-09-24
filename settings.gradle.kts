pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.swiftstorm.dev/maven2/") { name = "SwiftStorm Repository" }
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
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

rootProject.name = "fabricord"
