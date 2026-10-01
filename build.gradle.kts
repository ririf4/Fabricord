import com.modrinth.minotaur.ModrinthExtension
import com.modrinth.minotaur.dependencies.DependencyType
import com.modrinth.minotaur.dependencies.ModDependency
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.util.Properties
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

val lib = the<LibrariesForLibs>()
val modrinthToken = providers.environmentVariable("MODRINTH_TOKEN")
    .orElse(providers.gradleProperty("modrinthToken"))
val excludedSqliteNativeTargets = listOf(
    "FreeBSD",
    "Linux/arm",
    "Linux/armv6",
    "Linux/armv7",
    "Linux/ppc64",
    "Linux/riscv64",
    "Linux/x86",
    "Linux-Musl/x86",
    "Windows/armv7",
    "Windows/x86",
)

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.minotaur) apply false
    alias(libs.plugins.shadow) apply false
}

val versionPropertiesFile = rootProject.file("version.properties")
check(versionPropertiesFile.exists()) { "Missing version file: $versionPropertiesFile" }

val versionProperties = Properties()
versionPropertiesFile.reader().use { versionProperties.load(it) }

val generation = requireNotNull(versionProperties.getProperty("generation")?.toIntOrNull()) {
    "Missing or invalid generation in $versionPropertiesFile"
}
check(generation > 0) { "generation must be positive in $versionPropertiesFile" }

fun gitOutput(vararg arguments: String): String =
    providers.exec {
        commandLine("git", *arguments)
    }.standardOutput.asText.get().trim()

val gitRevision = gitOutput("rev-list", "--count", "HEAD").toInt()
val gitSha = gitOutput("rev-parse", "--short=8", "HEAD").lowercase()
val releaseYear = gitOutput("show", "-s", "--format=%cd", "--date=format:%Y", "HEAD").toInt()
check(releaseYear in 1000..9999) { "Git commit year must use four digits" }

val modrinthGameVersions = mapOf(
    "1.14-1.14.2" to listOf("1.14", "1.14.1", "1.14.2"),
    "1.14.3" to listOf("1.14.3"),
    "1.14.4" to listOf("1.14.4"),
    "1.15-1.15.2" to listOf("1.15", "1.15.1", "1.15.2"),
    "1.16" to listOf("1.16"),
    "1.16.1-1.16.3" to listOf("1.16.1", "1.16.2", "1.16.3"),
    "1.16.4-1.16.5" to listOf("1.16.4", "1.16.5"),
    "1.17-1.17.1" to listOf("1.17", "1.17.1"),
    "1.18-1.18.2" to listOf("1.18", "1.18.1", "1.18.2"),
    "1.19" to listOf("1.19"),
    "1.19.1-1.19.2" to listOf("1.19.1", "1.19.2"),
    "1.19.3-1.19.4" to listOf("1.19.3", "1.19.4"),
    "1.20-1.20.1" to listOf("1.20", "1.20.1"),
    "1.20.2" to listOf("1.20.2"),
    "1.20.3-1.20.4" to listOf("1.20.3", "1.20.4"),
    "1.20.5-1.21.5" to listOf("1.20.5", "1.20.6", "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5"),
    "1.21.6-1.21.8" to listOf("1.21.6", "1.21.7", "1.21.8"),
    "1.21.9-1.21.11" to listOf("1.21.9", "1.21.10", "1.21.11"),
    "26.1-26.1.2" to listOf("26.1", "26.1.1", "26.1.2"),
)

val validateModrinthCredentials = tasks.register("validateModrinthCredentials") {
    group = "publishing"
    description = "Verify that Modrinth credentials are available."

    doLast {
        check(!modrinthToken.orNull.isNullOrBlank()) {
            "Set MODRINTH_TOKEN or modrinthToken in the user Gradle properties before publishing."
        }
        val repositoryStatus = providers.exec {
            commandLine("git", "status", "--porcelain", "--untracked-files=normal")
        }.standardOutput.asText.get().trim()
        check(repositoryStatus.isEmpty()) {
            "The repository must be clean before publishing so the Git SHA identifies the released sources."
        }
    }
}

val prepareModrinthPublication = tasks.register("prepareModrinthPublication") {
    group = "publishing"
    description = "Build and test every compatibility module before publishing."
    dependsOn(validateModrinthCredentials)
}

val publishModrinth = tasks.register("publishModrinth") {
    group = "publishing"
    description = "Build and publish every compatibility module to Modrinth."
    dependsOn(prepareModrinthPublication)
}

tasks.register<Delete>("clean") {
    delete(layout.projectDirectory.dir("dist"))
}

allprojects {
    group = "net.ririfa"
}

subprojects {
    configurations.configureEach {
        exclude(group = "club.minnced", module = "opus-java")
        exclude(group = "com.google.crypto.tink", module = "tink")
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        extensions.configure<KotlinJvmProjectExtension>("kotlin") {
            sourceSets.named("main") {
                kotlin.srcDir(rootProject.file("fabricord/common/src/main/kotlin"))
            }
            sourceSets.named("test") {
                kotlin.srcDir(rootProject.file("fabricord/common/src/test/kotlin"))
            }
        }

        extensions.configure<SourceSetContainer>("sourceSets") {
            named("main") {
                java.srcDir(rootProject.file("fabricord/common/src/main/java"))
                resources.srcDir(rootProject.file("fabricord/common/src/main/resources"))
            }
        }

        dependencies {
            add("compileOnly", lib.yacla.core)
            add("compileOnly", lib.yacla.yaml)
            add("compileOnly", lib.langman.core)
            add("compileOnly", lib.langman.yaml)
            add("compileOnly", lib.yaml)
        }
    }

    val versionKey = "version.$name"
    val versionParts = requireNotNull(
        versionProperties.getProperty(versionKey)
            ?.trim()
            ?.let { Regex("(\\d+)\\.(\\d+)").matchEntire(it) },
    ) {
        "Missing or invalid $versionKey in $versionPropertiesFile; expected Minor.Patch (for example, 1.0)"
    }
    val minor = versionParts.groupValues[1].toInt()
    val patch = versionParts.groupValues[2].toInt()
    check(minor >= 1 && patch >= 0) {
        "$versionKey must have Minor >= 1 and Patch >= 0"
    }
    val publicVersion = "$releaseYear.$minor.$patch"
    requireNotNull(modrinthGameVersions[name]) {
        "Missing Modrinth game versions for $name"
    }
    val minecraftVersionRange = name
    val preRelease = versionProperties.getProperty("preRelease.$name")
        ?.trim()
        ?.takeIf(String::isNotEmpty)
    if (preRelease != null) {
        check(preRelease.matches(Regex("[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*"))) {
            "preRelease.$name contains invalid identifiers"
        }
    }

    val shortYear = releaseYear.toString().takeLast(2)
    val preReleaseSuffix = preRelease?.let { "-$it" }.orEmpty()
    val deepVersion = "$generation$shortYear.$minor.$patch.r$gitRevision" +
        "$preReleaseSuffix+env$minecraftVersionRange.sha$gitSha"
    version = publicVersion

    fun configureReleaseJar(taskName: String) {
        val releaseJar = tasks.named<AbstractArchiveTask>(taskName) {
            archiveFileName.set("Fabricord-$deepVersion.jar")
        }
        val copyReleaseJar = tasks.register<Copy>("copyReleaseJar") {
            group = "distribution"
            description = "Copy the release JAR to the root distribution directory."
            dependsOn(releaseJar)
            from(releaseJar.flatMap { it.archiveFile })
            into(rootProject.layout.projectDirectory.dir("dist"))
        }

        val buildTask = tasks.named("build") {
            dependsOn(copyReleaseJar)
            mustRunAfter(validateModrinthCredentials)
        }
        prepareModrinthPublication.configure {
            dependsOn(buildTask)
        }

        pluginManager.apply("com.modrinth.minotaur")
        extensions.configure<ModrinthExtension>("modrinth") {
            token.set(modrinthToken)
            projectId.set("fabricord")
            versionNumber.set(deepVersion)
            versionName.set("Fabricord $publicVersion for Minecraft $minecraftVersionRange")
            versionType.set("release")
            changelog.set(
                "For the full changelog, see the " +
                    "[GitHub Releases page](https://github.com/ririf4/Fabricord/releases).",
            )
            file.set(releaseJar.flatMap { it.archiveFile })
            gameVersions.set(requireNotNull(modrinthGameVersions[project.name]))
            loaders.set(listOf("fabric"))
            environment.set("dedicated_server_only")
            detectLoaders.set(false)
            debugMode.set(
                providers.gradleProperty("modrinthDebug")
                    .map(String::toBoolean)
                    .orElse(false),
            )
            dependencies.set(
                listOf(
                    ModDependency("fabric-api", DependencyType.REQUIRED),
                    ModDependency("fabric-language-kotlin", DependencyType.REQUIRED),
                ),
            )
        }

        val modrinthTask = tasks.named("modrinth") {
            dependsOn(validateModrinthCredentials)
            dependsOn(releaseJar)
            mustRunAfter(prepareModrinthPublication)
        }
        publishModrinth.configure {
            dependsOn(modrinthTask)
        }
    }

    pluginManager.withPlugin("com.gradleup.shadow") {
        tasks.withType<ShadowJar>().configureEach {
            excludedSqliteNativeTargets.forEach { target ->
                exclude("org/sqlite/native/$target/**")
            }
        }
        if (pluginManager.hasPlugin("net.fabricmc.fabric-loom")) {
            configureReleaseJar("shadowJar")
        }
    }
    pluginManager.withPlugin("net.fabricmc.fabric-loom-remap") {
        configureReleaseJar("remapJar")
    }
}
