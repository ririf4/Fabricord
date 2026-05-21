plugins {
    alias(libs.plugins.kotlin.jvm)
}

allprojects {
    plugins.apply("kotlin")

    repositories {
        mavenCentral()
        maven("https://repo.swiftstorm.dev/maven2/") { name = "SwiftStorm" }
        maven("https://maven.fabricmc.net") { name = "FabricMC" }
    }
}