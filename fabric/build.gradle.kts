plugins {
    id("multiloader-loader")
    // plugin versions are defined in stonecutter.gradle.kts
    id("net.fabricmc.fabric-loom")
    id("me.modmuss50.mod-publish-plugin")
}

repositories {
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1") { name = "DevAuth" }
    maven("https://maven.terraformersmc.com/") { name = "Terraformers" }
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    implementation("net.fabricmc:fabric-loader:${sc.properties["dependencies.fabric_loader_version"] as String}")

    implementation(platform("net.fabricmc.fabric-api:fabric-api-bom:${sc.properties["dependencies.fabric_api"] as String}+${sc.current.project}"))
    implementation("net.fabricmc.fabric-api:fabric-data-generation-api-v1")
    implementation("net.fabricmc.fabric-api:fabric-networking-api-v1")
    implementation("net.fabricmc.fabric-api:fabric-key-mapping-api-v1")
    implementation("net.fabricmc.fabric-api:fabric-lifecycle-events-v1")
    implementation("net.fabricmc.fabric-api:fabric-command-api-v2")
    implementation("net.fabricmc.fabric-api:fabric-rendering-v1")
    implementation("net.fabricmc.fabric-api:fabric-resource-loader-v1")
    runtimeOnly("net.fabricmc.fabric-api:fabric-registry-sync-v0")

    // Allow logging into an actual Minecraft account in a dev env
    // See https://github.com/DJtheRedstoner/DevAuth
    localRuntime("me.djtheredstoner:DevAuth-fabric:1.2.2")

    val modmenu: String = sc.properties["dependencies.modmenu"]
    compileOnly("com.terraformersmc:modmenu:${modmenu}")
    if(sc.properties["debug.load_modmenu"]) {
        localRuntime("com.terraformersmc:modmenu:${modmenu}")
        localRuntime("net.fabricmc.fabric-api:fabric-screen-api-v1")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

val dataSourceSet = sourceSets.named("datagen") {
    compileClasspath += sourceSets.main.get().compileClasspath
}
configurations.datagenCompileClasspath.configure {
    extendsFrom(configurations.compileClasspath.get())
}
configurations.datagenRuntimeClasspath.configure {
    extendsFrom(configurations.runtimeClasspath.get())
}

loom {
    decompilers {
        named("vineflower") {
            options.put("mark-corresponding-synthetics", "1")
        }
    }

    val modId : String = sc.properties["mod_id"]
    mods {
        create(modId) {
            sourceSet("runMain")
        }
        create("${modId}_data") {
            sourceSet("runData")
            modFiles.forEach { println("File: ${it.absolutePath}") }
        }
    }

    runConfigs.configureEach {
        displayName = runtimeEnvironment.map { "Fabric ${it.replaceFirstChar(Char::uppercase)}" }
        generateRunConfig = true
        ideConfigFolder = "Fabric"
        // by default loom will use versions/*/run for the run dir, so instead tell it to use the
        // run dir in the project root directory
        runDirectory = sc.branch.project.layout.projectDirectory.dir("run")
        if (name == "datagen") {
            // always disable DevAuth in datagen, even if it's enabled in the DevAuth config/other similar means
            jvmArguments.add("-Ddevauth.enabled=false")
            sourceSet.set("runData")
        } else {
            // Enable DCEVM when using JBR
            if(javaToolchains.launcherFor(java.toolchain).map { it.metadata.vendor }.getOrElse("").contains("JetBrains")) {
                jvmArguments.addAll("-XX:+AllowEnhancedClassRedefinition")
            }
            sourceSet.set("runMain")
        }
    }

    //TODO: Can we have the convertATtoCT task run automatically for configuration? For building we can ensure the file is at least up to date
    val aw = sc.branch.project.file("src/main/resources/${modId}.classtweaker")
    if (aw.exists()) {
        accessWidenerPath = sc.process(aw, "build/dev.ct")
    } else {
        println("No class tweaker file present. Please run validateAccessWidener")
    }
}

//TODO - Fabric: Re-evaluate these task configurations
val convertATs = rootProject.tasks.named("convertATtoCT")

tasks.named("validateAccessWidener").configure {
    //Ensure that the CT file is up to date when trying to validate access wideners
    dependsOn(convertATs)
}
tasks.named("sourcesJar").configure {//Ensure AWs are validated for the source jar, and are updated if necessary
    dependsOn(tasks.named("validateAccessWidener"))
}
tasks.named<ProcessResources>("processResources").configure {
    mustRunAfter(convertATs)
    exclude("META-INF/accesstransformer.cfg")
}
tasks.named("stonecutterPrepare").configure {
    mustRunAfter(convertATs)
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}

rootProject.tasks.named("runData").configure {
    dependsOn(tasks.named("runDatagen"))
}

listOf("includeInternal", "modCompileClasspath").forEach { variant ->
    configurations.named(variant) {
        attributes {
            attribute(loaderAttribute, loader)
        }
    }
}

publishMods {
    dryRun = performDryRun
    modrinth {
        from(modrinthOps, basePublishingOps)
        additionalFile(tasks.sourcesJar.flatMap { it.archiveFile }) { type.set(SOURCES_JAR) }
        additionalFile(tasks.javadocJar.flatMap { it.archiveFile }) { type.set(JAVADOC_JAR) }
        requires("fabric-api")
    }
    curseforge {
        from(cfOps, basePublishingOps)
        requires("fabric-api")
    }
}
