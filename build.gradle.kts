import dev.swiftstorm.akkaradb.plugin.akkara

plugins {
	alias(libs.plugins.kotlin)
	alias(libs.plugins.loom)
	alias(libs.plugins.akkara)
}

repositories {
	mavenCentral()
	maven("https://maven.fabricmc.net/") { name = "FabricMC" }
	maven("https://repo.swiftstorm.dev/maven2/") { name = "SwiftStorm Repository" }
}

dependencies {
	minecraft(libs.minecraft)
	mappings(libs.fabric.yarn)
	modImplementation(libs.fabric.api)
	modImplementation(libs.fabric.loader)
	modImplementation(libs.fabric.kotlin)

	akkara("0.0.1+rc.3")
}