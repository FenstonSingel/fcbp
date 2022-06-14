rootProject.name = "fcbp-instrumenter"

pluginManagement {
    plugins {
        kotlin("jvm") version "1.5.31"
        kotlin("plugin.serialization") version "1.5.31"
        id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
    }
}

include(":shared")
project(":shared").projectDir = File("../fcbp-shared")
