pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.essential.gg/repository/maven-releases")
        maven("https://repo.polyfrost.org/releases")
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "gg.essential.loom" -> useModule("gg.essential:architectury-loom:${requested.version}")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.6.0")
}


rootProject.name = "Bazaar-Utils"
