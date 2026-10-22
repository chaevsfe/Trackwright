import java.util.zip.ZipFile

plugins {
    `java-library`
    id("net.neoforged.moddev") version "2.0.141"
}

group = property("maven_group") as String
version = "${property("mod_version")}+neoforge-mc${property("mc_line")}"

base {
    archivesName.set(property("archives_base_name") as String)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://maven.createmod.net")
    maven("https://mvn.devos.one/snapshots")
    maven("https://api.modrinth.com/maven") {
        content { includeGroup("maven.modrinth") }
    }
}

val mainSourceSet: SourceSet = sourceSets.main.get()
val clientSourceSet: SourceSet = sourceSets.create("client")
clientSourceSet.compileClasspath += mainSourceSet.output
clientSourceSet.runtimeClasspath += mainSourceSet.output

configurations.named(clientSourceSet.implementationConfigurationName) { extendsFrom(configurations.implementation.get()) }
configurations.named(clientSourceSet.compileOnlyConfigurationName) { extendsFrom(configurations.compileOnly.get()) }
configurations.named(clientSourceSet.runtimeOnlyConfigurationName) { extendsFrom(configurations.runtimeOnly.get()) }

val serverPort = property("server_port") as String
val rconPort = property("rcon_port") as String
val rconPassword = property("rcon_password") as String
val snrDependency = "maven.modrinth:create-steam-n-rails-1.21.1:${property("snr_version")}"
val metadataExpansions = mapOf(
    "version" to project.version.toString(),
    "mod_id" to property("mod_id") as String,
    "mod_name" to property("mod_name") as String,
    "mod_license" to property("mod_license") as String,
    "mod_authors" to property("mod_authors") as String,
    "mod_credits" to property("mod_credits") as String,
    "mod_description" to property("mod_description") as String,
    "mod_url" to property("mod_url") as String,
    "mod_issues" to property("mod_issues") as String,
    "loader_version_range" to property("loader_version_range") as String,
    "minecraft_version_range" to property("minecraft_version_range") as String,
    "neo_version_range" to property("neo_version_range") as String,
    "create_version_range" to property("create_version_range") as String,
)

val withSnr = providers.gradleProperty("withSnr").map { it != "false" }.getOrElse(false)
val mixinDebug = providers.gradleProperty("mixinDebug").map { it != "false" }.getOrElse(false)

val writeServerSettings = tasks.register("writeServerSettings") {
    description = "Writes eula.txt and the dev server's port, RCON and online-mode settings into run-server."
    group = "mod development"
    val directory = layout.projectDirectory.dir("run-server").asFile
    val settings = linkedMapOf(
        "server-port" to serverPort,
        "enable-rcon" to "true",
        "rcon.port" to rconPort,
        "rcon.password" to rconPassword,
        "online-mode" to "false",
    )
    doLast {
        directory.mkdirs()
        File(directory, "eula.txt").writeText("eula=true\n")
        val file = File(directory, "server.properties")
        val kept = if (file.exists()) file.readLines().filter { line -> settings.keys.none { line.startsWith("$it=") } } else emptyList()
        file.writeText((kept + settings.map { (key, value) -> "$key=$value" }).joinToString("\n", postfix = "\n"))
    }
}

neoForge {
    version = property("neo_version") as String

    parchment {
        mappingsVersion.set(property("parchment_mappings_version") as String)
        minecraftVersion.set(property("parchment_minecraft_version") as String)
    }

    runs {
        register("client") {
            client()
            gameDirectory.set(file("run-client"))
            sourceSet.set(clientSourceSet)
        }
        register("clientJoin") {
            client()
            gameDirectory.set(file("run-client"))
            sourceSet.set(clientSourceSet)
            programArgument("--quickPlayMultiplayer")
            programArgument("127.0.0.1:$serverPort")
        }
        register("server") {
            server()
            gameDirectory.set(file("run-server"))
            programArgument("--nogui")
            taskBefore(writeServerSettings)
        }
        configureEach {
            logLevel.set(org.slf4j.event.Level.INFO)
            if (mixinDebug) {
                systemProperty("mixin.debug.verbose", "true")
                systemProperty("mixin.debug.export", "true")
            }
        }
    }

    mods {
        register(property("mod_id") as String) {
            sourceSet(mainSourceSet)
            sourceSet(clientSourceSet)
        }
    }
}

neoForge.addModdingDependenciesTo(clientSourceSet)

dependencies {
    implementation("com.simibubi.create:create-${property("minecraft_version")}:${property("create_version")}:slim") { isTransitive = false }
    implementation("net.createmod.ponder:ponder-neoforge:${property("ponder_version")}+mc${property("minecraft_version")}")
    compileOnly("dev.engine-room.flywheel:flywheel-neoforge-api-${property("minecraft_version")}:${property("flywheel_version")}")
    runtimeOnly("dev.engine-room.flywheel:flywheel-neoforge-${property("minecraft_version")}:${property("flywheel_version")}")
    implementation("com.tterrag.registrate:Registrate:${property("registrate_version")}")
    compileOnly("org.jspecify:jspecify:${property("jspecify_version")}")
    if (withSnr) {
        runtimeOnly(snrDependency) { isTransitive = false }
    }
}

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    inputs.properties(metadataExpansions)
    expand(metadataExpansions)
    from("src/main/templates")
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}
mainSourceSet.resources.srcDir(generateModMetadata)
neoForge.ideSyncTask(generateModMetadata)

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "10000"))
}

tasks.withType<AbstractCopyTask>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.jar {
    from(clientSourceSet.output)
    from("LICENSE")
    from("NOTICE")
}

tasks.named<Jar>("sourcesJar") {
    from(clientSourceSet.allSource)
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

val checkTranslations = tasks.register("checkTranslations") {
    description = "Fails if the sources use a translation key that no language file defines, or if one of our keys is never used."
    group = "verification"
    val sources = fileTree("src") { include("**/*.java") }
    val ownLang = file("src/main/resources/assets/trackwright/lang/en_us.json")
    val compileClasspath = configurations.named("compileClasspath")
    inputs.files(sources)
    inputs.file(ownLang)
    doLast {
        val slurper = groovy.json.JsonSlurper()
        @Suppress("UNCHECKED_CAST")
        val own = (slurper.parse(ownLang) as Map<String, Any>).keys
        val createJar = compileClasspath.get().files.firstOrNull { it.name.startsWith("create-1.21.1-") && it.name.endsWith(".jar") }
            ?: throw GradleException("Create is not on the compile classpath")
        val createKeys = ZipFile(createJar).use { zip ->
            val entry = zip.getEntry("assets/create/lang/en_us.json")
                ?: throw GradleException("Create ships no assets/create/lang/en_us.json")
            @Suppress("UNCHECKED_CAST")
            (zip.getInputStream(entry).use { slurper.parse(it) } as Map<String, Any>).keys
        }
        val createPrefix = "create.track."
        val literal = Regex("\"((?:create|trackwright)(?:\\.[a-z0-9_]+)+)\"")
        val category = Regex("\"(key\\.category\\.trackwright(?:\\.[a-z0-9_]+)+)\"")
        val used = sources.files.flatMap { source ->
            val text = source.readText()
            literal.findAll(text).map { it.groupValues[1] }.toList() +
                category.findAll(text).map { it.groupValues[1] }.toList()
        }.toSortedSet()
        val missing = used.filter { key ->
            if (key.startsWith("trackwright.")) key !in own && (createPrefix + key) !in own
            else if (key.startsWith("key.")) key !in own
            else key !in own && key !in createKeys
        }
        val unused = own.filter { key -> key !in used && !(key.startsWith(createPrefix) && key.removePrefix(createPrefix) in used) }
        if (missing.isNotEmpty() || unused.isNotEmpty()) {
            throw GradleException(
                "Translation check failed:\n" +
                    missing.joinToString("") { "  missing: $it\n" } +
                    unused.joinToString("") { "  unused: $it\n" }
            )
        }
        logger.lifecycle("checkTranslations: ${used.size} keys used, ${own.size} own keys, all defined and used")
    }
}

tasks.named("check") {
    dependsOn(checkJarContainment)
    dependsOn(checkTranslations)
}
