plugins {
    id("fabric-loom") version "1.13-SNAPSHOT"
    id("maven-publish")
}

fun modProp(name: String): String =
    (findProperty(name) ?: rootProject.findProperty(name)) as String

val minecraftVersion = stonecutter.current.version

version = "${modProp("mod.version")}+$minecraftVersion"
group = modProp("mod.group")
base.archivesName = modProp("mod.archives_name")

val requiredJava = JavaVersion.VERSION_21

repositories {
    maven("https://maven.terraformersmc.com/releases/") { name = "TerraformersMC" }
    maven("https://maven.awakenedredstone.com") { name = "AwakenedRedstone" }
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:${modProp("deps.yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${modProp("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${modProp("deps.fabric_api")}")
    modCompileOnly("com.terraformersmc:modmenu:${modProp("deps.modmenu")}")
}

loom {
    enableModProvidedJavadoc = false
    splitEnvironmentSourceSets()

    mods {
        create("take-your-minestream") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets.getByName("client"))
        }
    }

    runConfigs.all {
        runDir = "../../run"
    }
}

java {
    withSourcesJar()
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava
}

tasks {
    processResources {
        val props = mapOf(
            "version" to modProp("mod.version"),
            "minecraft" to modProp("mod.mc_compat"),
            "fabric_loader_min" to modProp("deps.fabric_loader_min"),
        )
        inputs.properties(props)
        filesMatching("fabric.mod.json") { expand(props) }
    }

    withType<JavaCompile>().configureEach {
        options.release.set(21)
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(remapJar.get().archiveFile, remapSourcesJar.get().archiveFile)
        into(rootProject.layout.buildDirectory.dir("libs/${modProp("mod.version")}"))
        dependsOn("build")
    }
}

if (stonecutter.current.isActive) {
    rootProject.tasks.register("buildActive") {
        group = "project"
        dependsOn("buildAndCollect")
    }

    rootProject.tasks.register("runActive") {
        group = "project"
        dependsOn(tasks.named("runClient"))
    }
}
