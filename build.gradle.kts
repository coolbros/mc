plugins {
    java
}

group = "com.agast.minecraft"
version = "1.0.0"

val paperApiVersion = providers.gradleProperty("paperApiVersion").get()
val pluginApiVersion = providers.gradleProperty("pluginApiVersion").get()
val javaVersion = providers.gradleProperty("javaVersion").map { it.toInt() }.get()

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand(
                "version" to project.version,
                "apiVersion" to pluginApiVersion
            )
        }
    }

    jar {
        archiveBaseName.set("smp-worker-bots")
    }
}
