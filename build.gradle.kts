import java.util.Properties
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

val lib = the<LibrariesForLibs>()

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.shadow) apply false
}

allprojects {
    group = "net.ririfa"
}

subprojects {
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

    val propertiesFile = file("version.properties")

    if (!propertiesFile.exists()) {
        propertiesFile.createNewFile()

        val props = Properties()

        props["revision"] = "1"

        propertiesFile.writer().use { props.store(it, "Version Properties") }
    }

    val versionProps = Properties()

    propertiesFile.reader().use { versionProps.load(it) }

    val currentRevision = versionProps.getProperty("revision")
            ?.toIntOrNull()
            ?: 1

    val major = "2026"
    val minor = "1"
    val patch = "1"

    val publicVersion = "$major.$minor.$patch"

    val deepVersion = "$major.$minor.$patch-r$currentRevision"

    extra["publicVersion"] = publicVersion
    extra["deepVersion"] = deepVersion
    extra["revision"] = currentRevision

    version = deepVersion

    val incrementRevision = tasks.register("incrementRevision") {
        group = "versioning"
        description = "Increment revision for project $path"

        doLast {
            val latestProps = Properties()

            propertiesFile.reader().use { latestProps.load(it) }

            val latestRevision = latestProps.getProperty("revision")
                ?.toIntOrNull()
                ?: 1

            val newRevision = latestRevision + 1

            latestProps["revision"] = newRevision.toString()
            propertiesFile.writer().use { latestProps.store(it, "Version Properties") }

            println("[$path] Revision incremented -> r$newRevision")
        }
    }

    tasks.matching { it.name == "compileKotlin" }.configureEach {
        finalizedBy(incrementRevision)
    }

    when (name) {
        "26.1.2", "1.21.11", "1.21.8", "1.21.5", "1.20.4", "1.20.2", "1.20.1", "1.19.4", "1.19.2", "1.19", "1.18.2", "1.17.1", "1.16.5", "1.15.2" -> {
            val fullVersion = "$deepVersion+mc$name"

            version = fullVersion
            extra["deepVersion"] = fullVersion
        }
    }
}
