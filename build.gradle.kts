import java.util.zip.ZipFile

plugins {
    id("net.fabricmc.fabric-loom") version "1.16-SNAPSHOT"
}

group = property("maven_group") as String
version = "${property("mod_version")}+fly-mc${property("mc_line")}"

base {
    archivesName.set(property("archives_base_name") as String)
}

loom {
    splitEnvironmentSourceSets()
    mods {
        create("trackwright") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets["client"])
        }
    }
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://api.modrinth.com/maven") {
        content { includeGroup("maven.modrinth") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    implementation("maven.modrinth:create-fly:${property("create_fabric_version")}")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.withType<AbstractCopyTask>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.processResources {
    val expansions = mapOf(
        "version" to project.version.toString(),
        "minecraft_dependency_version" to project.property("minecraft_dependency_version") as String,
        "fabric_loader_version" to project.property("fabric_loader_version") as String,
        "fabric_api_version_range" to project.property("fabric_api_version_range") as String,
        "create_fabric_version_range" to project.property("create_fabric_version_range") as String,
    )
    expansions.forEach { (key, value) -> inputs.property(key, value) }
    filesMatching("fabric.mod.json") { expand(expansions) }
}

tasks.jar {
    from("LICENSE")
    from("NOTICE")
}

tasks.named<Jar>("sourcesJar") {
    from("LICENSE")
    from("NOTICE")
}

val jarContainmentPrefix = "dev/chaevsfe/trackwright/"

val checkJarContainment = tasks.register("checkJarContainment") {
    description = "Fails if the mod jar or the sources jar carries a class, a source file or a nested jar outside this mod's own namespace."
    group = "verification"
    val archives = files(
        tasks.named<Jar>("jar").flatMap { it.archiveFile },
        tasks.named<Jar>("sourcesJar").flatMap { it.archiveFile }
    )
    dependsOn(tasks.named("jar"), tasks.named("sourcesJar"))
    inputs.files(archives)
    val prefix = jarContainmentPrefix
    doLast {
        val offenders = mutableListOf<String>()
        var own = 0
        archives.forEach { archive ->
            ZipFile(archive).use { zip ->
                for (name in zip.entries().toList().map { it.name }) {
                    if (name.endsWith(".jar")) {
                        offenders += archive.name + " -> nested jar " + name
                        continue
                    }
                    if (!name.endsWith(".class") && !name.endsWith(".java")) continue
                    if (name.startsWith(prefix)) {
                        own++
                        continue
                    }
                    offenders += archive.name + " -> " + name
                }
            }
        }
        if (offenders.isNotEmpty()) {
            throw GradleException(
                "Trackwright must ship nothing outside " + prefix + "; found " + offenders.size +
                    " foreign entries:\n" + offenders.joinToString("\n") { "  " + it }
            )
        }
        logger.lifecycle("checkJarContainment: $own own class/source entries, 0 foreign, 0 nested jars")
    }
}

tasks.named("check") {
    dependsOn(checkJarContainment)
}
