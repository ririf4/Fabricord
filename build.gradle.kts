import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.fabric.loom) apply false
}

allprojects {
    repositories {
        mavenCentral()

        group = "net.ririfa"

        maven("https://repo.swiftstorm.dev/maven2/") {
            name = "SwiftStorm"
        }

        maven("https://maven.fabricmc.net") {
            name = "FabricMC"
        }
    }
}

subprojects {
    plugins.apply("kotlin")
    plugins.apply("net.fabricmc.fabric-loom")

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

    val deepVersion = "$major.$minor.$patch.r$currentRevision"

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

    tasks.named("compileKotlin") {
        finalizedBy(incrementRevision)
    }

    when (name) {
        "26.1.2" -> {
            val fullVersion = "$deepVersion+mc$name"

            version = fullVersion
            extra["deepVersion"] = fullVersion
        }
    }
}