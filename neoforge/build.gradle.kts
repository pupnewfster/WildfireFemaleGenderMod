plugins {
    id("multiloader-loader")
    id("net.neoforged.moddev")
    id("me.modmuss50.mod-publish-plugin")
}

val dataSourceSet = sourceSets.named("datagen")
val runMain = sourceSets.named("runMain")
val runData = sourceSets.named("runData")

neoForge {
    enable {
        version = sc.properties["dependencies.neo_version"]
        enabledSourceSets = sourceSets // All source sets use Minecraft code
    }

    val common = sc.tree["common"]!!.project
    val at = common.file("src/main/resources/META-INF/accesstransformer.cfg")
    //Use the corresponding common project's StoneCutter to process the path, so that it points at the identical absolute path
    // and MDG is able to more reliably re-use the recompiled minecraft
    val commonProject = sc.node.sibling("common")!!.project
    accessTransformers.from(commonProject.sc.process(at, "build/dev.at").absolutePath)
    //Note: We don't bother validating ATs as that is already done in the common branch
    //validateAccessTransformers = true

    val modId : String = sc.properties["mod_id"]

    val mod = mods.register(modId) {
        modSourceSets.add(sourceSets.main)
    }
    val dataMod = mods.register("${modId}_data") {
        modSourceSets.set(mod.get().modSourceSets)
        modSourceSets.add(dataSourceSet)
    }

    runs {
        configureEach {
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
            ideName = "NeoForge ${name.replaceFirstChar(Char::titlecase)} ($path)"
            gameDirectory = sc.branch.project.layout.projectDirectory.dir("run")

            val forceAnsi = providers.gradleProperty("forge_force_ansi")
            if (forceAnsi.isPresent) {
                // Force ANSI if declared as a Gradle property, as the auto detection
                // doesn't detect IntelliJ properly or Eclipse's ANSI console plugin.
                systemProperties.put("terminal.ansi", forceAnsi.get())
            }

            sourceSet = runMain
            loadedMods.add(mod.get())
        }

        register("client") {
            client()
            devLogin = providers.gradleProperty("mc_devlogin")
                    .map(String::toBoolean)
                    .getOrElse(false)
        }
        register("clientAlt") {
            client()
            programArguments.addAll("--username", "AltDev")
            devLogin = false
        }

        register("data") {
            clientData()
            loadedMods.empty()
            loadedMods.add(dataMod.get())
            sourceSet = runData

            programArguments.addAll("--all", "--output", file("src/generated/resources").absolutePath,
                "--mod", modId, "--existing", file("src/main/resources").absolutePath,
                "--existing", common.file("src/main/resources").absolutePath
            )
        }

        register("server") {
            server()
        }
    }
}

rootProject.tasks.named("runData").configure {
    dependsOn(tasks.named("runData"))
}

sourceSets.configureEach {
    configurations.named(getTaskName(null, "jarJar")) {
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
    }
    curseforge {
        from(cfOps, basePublishingOps)
    }
}
