import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    id("net.fabricmc.fabric-loom")
    alias(libs.plugins.shadow)
}

val shade = configurations.create("shade") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
configurations.runtimeOnly.get().extendsFrom(shade)

dependencies {
    minecraft("com.mojang:minecraft:26.1.2")
    implementation("net.fabricmc:fabric-loader:0.19.2")
    implementation("net.fabricmc.fabric-api:fabric-api:0.149.1+26.1.2")
    implementation("net.fabricmc:fabric-language-kotlin:1.13.10+kotlin.2.3.20")

    implementation(libs.jda)
    implementation(libs.sqlite.jdbc)
    implementation(libs.yacla.core)
    implementation(libs.yacla.yaml)
    implementation(libs.langman.core)
    implementation(libs.langman.yaml)
    implementation(libs.yaml)
    shade(libs.jda) { exclude(group = "net.java.dev.jna", module = "jna") }
    shade(libs.sqlite.jdbc)
    shade(libs.yacla.core)
    shade(libs.yacla.yaml)
    shade(libs.langman.core)
    shade(libs.langman.yaml)
    shade(libs.yaml)

    testImplementation(kotlin("test"))
}

tasks.withType<ProcessResources> {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

val shadowJarTask = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    configurations = listOf(shade)
    isZip64 = true

    relocate("net.dv8tion.jda", "net.ririfa.fabricord.shadowed.jda")

    exclude("net/dv8tion/jda/api/audio/**")
    exclude("net/dv8tion/jda/internal/audio/**")
    exclude("club/minnced/opus/**")
    exclude("com/sun/jna/**")
    exclude("kotlin/**")
    exclude("org/jetbrains/kotlin/**")
    exclude("kotlinx/**")
    exclude("org/slf4j/**")
    exclude("META-INF/*.kotlin_module")
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("plain")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<JavaExec>("runServer") {
    jvmArgs(
        "-Dfabric.gameMappingNamespace=official",
        "-Dfabric.runtimeMappingNamespace=official",
    )
}
