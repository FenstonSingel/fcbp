rootProject.name = "fcbp"

pluginManagement {
    plugins {
        id("org.jetbrains.kotlin.jvm") version "1.5.21"
        id("io.gitlab.arturbosch.detekt") version "1.18.1"
        id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
    }
}

include(":instrumenter")
project(":instrumenter").projectDir = File("fcbp-instrumenter")
