import com.wildfire.ATtoCTConverter
import com.wildfire.OptimizePng
import com.wildfire.ValidateJson

plugins {
    id("dev.kikugie.stonecutter")
    id("net.neoforged.moddev") version "2.0.143" apply false
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT" apply false
    id("idea")
}

idea {
    module {
        // Tell IDEA to always download sources/javadoc artifacts from Maven.
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

stonecutter active "26.3"

tasks.named<Wrapper>("wrapper") {
    //Define wrapper values here so as to not have to always do so when updating gradlew.properties
    gradleVersion = "9.7.1"
    distributionSha256Sum = "92c1a136d76b5017732a66d2e0a648ebff00dd3687d8bff0d0047a1bd904fdf2"
    //Note: We use the all distribution to make it easier to modify build scripts by being able to view the javadocs
    distributionType = Wrapper.DistributionType.ALL
}

tasks.register("generatePackageInfos") {
    description = "Generates package-info files for any packages that are missing them"
    dependsOn(stonecutter.tasks.named("generatePackageInfos") { branch.id.isNotEmpty() })
}

tasks.register("runData") {
    description = "Run data generation for all loaders and versions"
}

tasks.register<OptimizePng>("optimizePng") {
    inputFiles.from(fileTree(layout.projectDirectory) {
        include("*/src/main/resources/**/*.png")
    })
}

tasks.register<ValidateJson>("validateJson") {
    val modId: String = stonecutter.properties["mod_id"]
    inputs.property("modId", modId)
    criticalFiles.from(
        "fabric/src/main/resources/fabric.mod.json",
        "common/src/main/resources/${modId}.mixins.json",
        "fabric/src/main/resources/${modId}.fabric.mixins.json",
        "neoforge/src/main/resources/${modId}.neo.mixins.json"
    )
    rootTranslation.set(layout.projectDirectory.file(
        "fabric/versions/${stonecutter.current?.project}/src/main/generated/assets/${modId}/lang/en_us.json"
    ))
    translationFiles.from(fileTree(layout.projectDirectory) {
        include("*/versions/*/src/main/generated/assets/${modId}/lang/*.json")
        include("common/src/main/resources/assets/${modId}/lang/*.json")
    })
    nonExhaustiveLocales.set(setOf(
        //Only generates the relevant overrides as missing lang entries fall back to en_us
        "en_au",
        "en_ca",
        "en_gb",
        //Exact matches such as "N" does not generate the duplicate file
        "en_ud"
    ))
}

tasks.register<ATtoCTConverter>("convertATtoCT") {
    atPath = layout.projectDirectory.file("common/src/main/resources/META-INF/accesstransformer.cfg")
    ctPath = layout.projectDirectory.file("fabric/src/main/resources/${stonecutter.properties["mod_id"] as String}.classtweaker")
}

tasks.register("publishMods") {
    description = "Publish mod to both platforms, for both loaders, and all versions"
    group = "publishing"
    dependsOn(stonecutter.tasks.named("publishMods") { branch.id == "fabric" || branch.id == "neoforge" })
}
