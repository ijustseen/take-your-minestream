pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.architectury.dev/")
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9"
}

// Fabric Loom и Architectury Loom нельзя remap’ить в одном прогоне.
// Сборка JAR: ./gradlew buildAndCollectFabric|Forge|NeoForge (изолированный -Pmod.loaders).
val modLoaders = providers.gradleProperty("mod.loaders")
    .orElse("fabric,forge,neoforge")
    .get()
    .split(',')
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .toSet()

val wantFabric = "fabric" in modLoaders
val wantForge = "forge" in modLoaders
val wantNeoForge = "neoforge" in modLoaders

stonecutter {
    kotlinController = true
    create(rootProject) {
        if (wantFabric) {
            branch("fabric") {
                fun add(version: String) {
                    val buildscript = if (sc.eval(version, ">= 26.3")) {
                        "build-fabric-26.3.gradle.kts"
                    } else if (sc.eval(version, ">= 26.1")) {
                        "build-fabric-unobfuscated.gradle.kts"
                    } else if (sc.eval(version, "< 1.21")) {
                        "build-fabric-1.20.1.gradle.kts"
                    } else {
                        "build-fabric.gradle.kts"
                    }
                    version(version).buildscript(buildscript)
                }
                for (v in listOf("1.20.1", "1.21", "1.21.1", "1.21.4", "1.21.8", "1.21.10", "1.21.11", "26.1", "26.2", "26.3")) {
                    add(v)
                }
            }
        } else if (wantForge || wantNeoForge) {
            // Якорь vcs/active без Fabric Loom — иначе FML-версия станет active и скомпилит сырой core/
            branch("fabric") {
                version("1.21.8").buildscript("build-fabric-stub.gradle.kts")
            }
        }

        if (wantNeoForge) {
            branch("neoforge") {
                version("1.21.1").buildscript("build-neoforge.gradle.kts")
            }
        }
        if (wantForge) {
            branch("forge") {
                version("1.20.1").buildscript("build-forge.gradle.kts")
            }
        }

        vcsVersion = "1.21.8"
    }
}

rootProject.name = "take-your-stream-chat"
