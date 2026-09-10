/*
 * Wildfire's Female Gender Mod is a female gender mod created for Minecraft.
 * Copyright (C) 2023-present WildfireRomeo
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import dev.kikugie.stonecutter.data.deserialization.SCList
import gradle.kotlin.dsl.accessors._04dc8d3a90e2cfd35b03275ab207a42a.main
import gradle.kotlin.dsl.accessors._04dc8d3a90e2cfd35b03275ab207a42a.publishing
import gradle.kotlin.dsl.accessors._04dc8d3a90e2cfd35b03275ab207a42a.sourceSets
import neoforge.GeneratePackageInfos

plugins {
    `java-library`
    `maven-publish`
}

fun SCList.asListedElements(): String {
    return joinToString(", ")
}

fun SCList.asTomlList(): String {
    return joinToString("\", \"")
}

val modName: String = stonecutterBuild.properties["mod_name"]
val modVersion: String = stonecutterBuild.properties["mod_version"]
val javaVersion: String = stonecutterBuild.properties["java.version"]

base {
    archivesName.set(modName.replace(' ', '-'))
}

version = "${loader}-${modVersion}+mc${stonecutterBuild.current.project}"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
    withSourcesJar()
    withJavadocJar()
}

val generatePackageInfos = tasks.register<GeneratePackageInfos>("generatePackageInfos")

sourceSets.register("datagen") {
    //Datagen has no input resources
    resources.setSrcDirs(listOf<String>())
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().runtimeClasspath
}

sourceSets.configureEach {
    generatePackageInfos.configure { files.from(java.srcDirTrees) }
    listOf(
        compileClasspathConfigurationName,
        runtimeClasspathConfigurationName
    ).forEach { variant ->
        configurations.named(variant) {
            attributes {
                attribute(loaderAttribute, loader)
            }
        }
    }
}

tasks.named("test").configure {//Ensure validateJson has to be run in order for build to pass
    dependsOn(rootProject.tasks.named("validateJson"))
}

//For common and Neo, ensure that the minecraft artifacts occur after stonecutter generates stuff
tasks.matching { it.name == "createMinecraftArtifacts" }.configureEach {
    dependsOn("stonecutterGenerate")
}

val licenseFile = rootProject.layout.projectDirectory.file("LICENSE")
tasks.withType<Jar>().configureEach {
    inputs.property("name", modName)
    from(licenseFile) {
        rename("LICENSE", "LICENSE_${modName.replace(' ', '_')}")
    }
}

tasks.withType<Javadoc>().configureEach {
    val opts = options as StandardJavadocDocletOptions
    opts.encoding = "UTF-8"
    opts.addBooleanOption("-no-fonts", true)
    opts.tags = (opts.tags ?: mutableListOf()).apply {
        add("apiNote:a:API Note:")
        add("implSpec:a:Implementation Requirements:")
        add("implNote:a:Implementation Note:")
    }
}

tasks.named<Jar>("jar") {
    var authors = stonecutterBuild.properties.raw("authors").asList().asListedElements()
    manifest.attributes(mapOf(
        "Specification-Title" to modName,
        "Specification-Vendor" to authors,
        "Specification-Version" to archiveVersion.get(),
        "Implementation-Title" to modName,
        "Implementation-Vendor" to authors,
        "Implementation-Version" to archiveVersion.get(),
        "Built-On-Minecraft" to stonecutterBuild.current.version
    ))
    inputs.property("name", modName)
    inputs.property("authors", authors)
    inputs.property("version", archiveVersion.get())
    //TODO: Is this needed? Or does how SC does things not require it
    //inputs.property("sc_version", stonecutterBuild.current.version)
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(":common:${stonecutterBuild.current.project}:stonecutterGenerate")
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(":common:${stonecutterBuild.current.project}:stonecutterGenerate")
    val authors = stonecutterBuild.properties.raw("authors").asList()
    val contributors = stonecutterBuild.properties.raw("contributors").asList()
    val expandProps = mapOf(
        "version" to modVersion,
        "minecraft_version" to stonecutterBuild.current.version,
        "major_minecraft_version" to stonecutterBuild.current.project,
        "minecraft_version_range" to stonecutterBuild.properties["meta.supported_minecraft_versions"],
        "fabric_version" to stonecutterBuild.properties["dependencies.fabric_api"],
        "fabric_loader_version" to stonecutterBuild.properties["dependencies.fabric_loader_version"],
        "mod_name" to modName,
        "mod_id" to stonecutterBuild.properties["mod_id"],
        "license" to stonecutterBuild.properties["mod_license"],
        "source_code" to stonecutterBuild.properties["source_code"],
        "issue_tracker" to stonecutterBuild.properties["issue_tracker"],
        "description" to stonecutterBuild.properties["mod_description"],
        "authors" to authors.asListedElements(),
        "authors_list" to authors.asTomlList(),
        "contributors" to contributors.asListedElements(),
        "contributors_list" to contributors.asTomlList(),
        // may be omitted during snapshot cycles
        "neoforge_version" to (stonecutterBuild.properties.getOrNull("dependencies.min_neo_version") ?: ""),
        "java_version" to javaVersion,
    )
    inputs.properties(expandProps)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand(expandProps)
    }

    val jsonExpandProps = expandProps.mapValues { (_, value) -> value.replace("\n", "\\n") }
    filesMatching(listOf("fabric.mod.json", "*.mixins.json")) {
        expand(jsonExpandProps)
    }
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            artifactId = base.archivesName.get()
            from(components["java"])
        }
    }
}

listOf("apiElements", "runtimeElements", "sourcesElements", "javadocElements").forEach { variant ->
    configurations.named(variant) {
        attributes {
            attribute(loaderAttribute, loader)
        }
    }
}

stonecutterBuild.replacements {
    string(stonecutterBuild.current.parsed >= "26.2") {
        replace("setScreen(", "gui.setScreen(")
        replace("getToastManager(", "gui.toastManager(")
        replace("getTabList(", "hud.getTabList(")
        replace(".screen ", ".gui.screen() ")
    }
    string(stonecutterBuild.current.parsed >= "26.3-pre-2") {
        replace("com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService", "com.mojang.authlib.services.MinecraftServicesSessionService")
        replace("YggdrasilMinecraftSessionService", "MinecraftServicesSessionService")
        replace("HttpAuthenticationService", "HttpDiscoveryService")
    }
    string(stonecutterBuild.current.parsed >= "26.2", "!entity_types") {
        replace("EntityType", "EntityTypes")
    }
    string(stonecutterBuild.current.parsed >= "26.2", "color_as_rgb") {
        replace("TextColor.fromRgb(0xFFAA00)", "TextColor.GOLD")
        replace("TextColor.fromRgb(0xFF55FF)", "TextColor.LIGHT_PURPLE")
        replace("TextColor.fromRgb(0xFFFFFF)", "TextColor.WHITE")
    }
    regex(stonecutterBuild.current.parsed >= "26.2", "!named_text_color") {
        replace("withStyle\\(net\\.minecraft\\.ChatFormatting\\.([A-Z_]+)", "withColor(TextColor.$1",
            "withColor\\(TextColor\\.([A-Z_]+)", "withStyle(net.minecraft.ChatFormatting.$1")
        replace("net\\.minecraft\\.ChatFormatting\\.([A-Z_]+)", "TextColor.$1",
            "TextColor\\.([A-Z_]+)", "net.minecraft.ChatFormatting.$1")
    }
}
