import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    id("net.fabricmc.fabric-loom-remap")
    alias(libs.plugins.shadow)
}

val shade = configurations.create("shade") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
configurations.runtimeOnly.get().extendsFrom(shade)

dependencies {
    minecraft("com.mojang:minecraft:1.16.5")
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:0.19.5")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.42.0+1.16")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.13.10+kotlin.2.3.20")

    implementation(libs.jda) {
        exclude(group = "org.slf4j")
    }
    implementation(libs.sqlite.jdbc)
    implementation(libs.yacla.core)
    implementation(libs.yacla.yaml)
    implementation(libs.langman.core)
    implementation(libs.langman.yaml)
    implementation(libs.yaml)
    shade(libs.jda) {
        exclude(group = "net.java.dev.jna", module = "jna")
        exclude(group = "org.slf4j")
    }
    shade(libs.sqlite.jdbc)
    shade(libs.yacla.core)
    shade(libs.yacla.yaml)
    shade(libs.langman.core)
    shade(libs.langman.yaml)
    shade(libs.yaml)
    shade("org.slf4j:slf4j-api:2.0.17")
    shade("org.slf4j:slf4j-simple:2.0.17")

    testImplementation(kotlin("test"))
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

tasks.withType<ProcessResources> {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

val shadowJarTask = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("dev-shadow")
    configurations = listOf(shade)
    isZip64 = true

    relocate("net.dv8tion.jda", "net.ririfa.fabricord.shadowed.jda")
    relocate("org.slf4j", "net.ririfa.fabricord.shadowed.slf4j")

    exclude("club/minnced/opus/**")
    exclude("com/sun/jna/**")
    exclude("kotlin/**")
    exclude("org/jetbrains/kotlin/**")
    exclude("kotlinx/**")
    exclude("META-INF/*.kotlin_module")
}

tasks.named<RemapJarTask>("remapJar") {
    dependsOn(shadowJarTask)
    inputFile.set(shadowJarTask.flatMap { it.archiveFile })
    archiveClassifier.set("")
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("plain")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
