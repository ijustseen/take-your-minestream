plugins {
    id("dev.architectury.loom") version "1.9-SNAPSHOT"
    id("maven-publish")
    id("me.modmuss50.mod-publish-plugin") version "2.2.1"
}

fun modProp(name: String): String =
    (findProperty(name) ?: rootProject.findProperty(name)) as String

/** Fabric Loom + Architectury Loom в одном прогоне ломают remapJar — FML только изолированно. */
val selectedLoaders: Set<String> =
    ((findProperty("mod.loaders") as? String) ?: "fabric,forge,neoforge")
        .split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()
val fmlBuildAllowed = "fabric" !in selectedLoaders

val minecraftVersion = stonecutter.current.version

version = "${modProp("mod.version")}+$minecraftVersion-forge"
group = modProp("mod.group")
base.archivesName = modProp("mod.archives_name")

val requiredJava = JavaVersion.VERSION_17

repositories {
    maven("https://maven.minecraftforge.net/")
    maven("https://maven.architectury.dev/")
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.layered {
        mappings("net.fabricmc:yarn:${modProp("deps.yarn_mappings")}:v2")
        mappings("dev.architectury:yarn-mappings-patch-forge:${modProp("deps.yarn_patch_forge")}")
    })
    "forge"("net.minecraftforge:forge:${modProp("deps.forge")}")
}

sourceSets {
    named("main") {
        resources.srcDir("src/client/resources")
    }
    create("client") {
        compileClasspath += named("main").get().compileClasspath + named("main").get().output
        runtimeClasspath += named("main").get().runtimeClasspath + named("main").get().output
    }
}

dependencies {
    "clientImplementation"(sourceSets.main.get().output)
}

loom {
    silentMojangMappingsLicense()

    forge {
        mixinConfigs("take-your-stream-chat.client.mixins.json")
    }

    mods {
        // Имя = FML modId. Оба source set — иначе classes без assets или наоборот.
        create("take_your_stream_chat") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets.getByName("client"))
        }
    }

    runConfigs.all {
        runDir = "../../../run-forge"
    }
}

tasks.named<Jar>("jar") {
    // Только classes: resources уже в main, client после sync дублирует pack.mcmeta/assets
    from(sourceSets["client"].output.classesDirs)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Forge в dev часто монтирует client output без main/resources → нет lang/textures.
// ПОСЛЕ processClientResources. Без META-INF: иначе второй mods.toml → лишний ModFile.
tasks.register<Copy>("syncMainResourcesToClient") {
    dependsOn("processResources", "processClientResources")
    from(layout.buildDirectory.dir("resources/main")) {
        exclude("META-INF/**")
        exclude("fabric.mod.json")
    }
    into(layout.buildDirectory.dir("resources/client"))
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    doLast {
        delete(layout.buildDirectory.dir("resources/client/META-INF"))
        delete(layout.buildDirectory.file("resources/client/fabric.mod.json"))
    }
}

tasks.named("clientClasses") {
    dependsOn("syncMainResourcesToClient")
}

tasks.matching { it.name.startsWith("run") || it.name == "configureClientLaunch" }.configureEach {
    dependsOn("clientClasses")
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
            "java_min" to modProp("deps.java_min"),
            "forge" to modProp("deps.forge"),
        )
        inputs.properties(props)
        filesMatching("META-INF/mods.toml") { expand(props) }
    }

    withType<JavaCompile>().configureEach {
        options.release.set(17)
        dependsOn("stonecutterGenerate")
    }

    register<Copy>("collectJars") {
        group = "build"
        onlyIf { fmlBuildAllowed }
        from(remapJar.get().archiveFile, remapSourcesJar.get().archiveFile)
        into(rootProject.layout.buildDirectory.dir("libs/${modProp("mod.version")}/forge"))
        dependsOn("build")
    }
}

tasks.named("remapJar") { onlyIf { fmlBuildAllowed } }
tasks.named("remapSourcesJar") { onlyIf { fmlBuildAllowed } }

apply(from = rootProject.file("gradle/publish-mods.gradle"))
