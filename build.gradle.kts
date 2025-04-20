import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.loom)
    alias(libs.plugins.shadowJar)
}

repositories {
    mavenCentral { name="Maven Central" }
    maven("https://repo.ririfa.net/maven2/") { name="RiriFa Repository" }
    maven("https://maven.fabricmc.net/") { name="FabricMC" }
}

dependencies {
    minecraft(libs.minecraft)
    mappings(libs.fabric.yarn)
    modImplementation(libs.bundles.fabrics)
    modImplementation(libs.jda) {
        exclude(group = "net.java.dev.jna", module = "jna")
    }
    modImplementation(libs.bundles.fabricord)
}

tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(project.configurations.getByName("runtimeClasspath"))

    relocate("net.dv8tion.jda", "net.ririfa.shadowed.jda")
    relocate("net.java.dev.jna", "net.ririfa.shadowed.jna")
    relocate("org.yaml.snakeyaml", "net.ririfa.shadowed.yaml")
    relocate("org.jetbrains.exposed", "net.ririfa.shadowed.exposed")

    archiveClassifier.set("")
}

tasks.named<RemapJarTask>("remapJar") {
    dependsOn("shadowJar")
    val shadowJar = tasks.named<ShadowJar>("shadowJar").get()
    inputs.file(shadowJar.archiveFile)
    from({ shadowJar.outputs.files })
}
