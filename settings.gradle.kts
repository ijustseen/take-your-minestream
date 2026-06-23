pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9"
}

stonecutter {
    kotlinController = true
    shared {
        fun mc(vararg versions: String) {
            for (version in versions) {
                val buildscript = if (sc.eval(version, ">= 26.1")) {
                    "build-unobfuscated.gradle.kts"
                } else {
                    "build-obfuscated.gradle.kts"
                }
                version(version, version).buildscript(buildscript)
            }
        }
        mc("1.21", "1.21.1", "1.21.4", "1.21.8", "1.21.10", "1.21.11", "26.1")
        vcsVersion = "1.21.8"
    }
    create(rootProject)
}

rootProject.name = "take-your-stream-chat"
