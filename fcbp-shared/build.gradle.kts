plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")

    id("org.jlleitschuh.gradle.ktlint")
}

group = parent?.group ?: "net.fenstonsingel.fcbp"
version = parent?.version ?: "inapplicable"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }
}
