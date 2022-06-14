rootProject.name = "fcbp-shared"

pluginManagement {
    plugins {
        kotlin("jvm") version "1.5.31"
        kotlin("plugin.serialization") version "1.5.31"
        id("io.gitlab.arturbosch.detekt") version "1.18.1"
        id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
    }
}