plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm")

    id("com.github.johnrengelman.shadow") version "7.0.0"

    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

group = parent?.group ?: "net.fenstonsingel.fcbp"
version = parent?.version ?: "inapplicable"

/*
 * this is (seemingly) a weird hack to basically exclude
 * this subproject's dependencies from distribution dependencies of
 * the actual plugin while still being able to make a fat jar for
 * the javaagent and otherwise use them as standard dependencies
 * the hack (seems to) depend on how the intellij gradle plugin tasks
 * scan the "runtimeClasspath" configuration
 */
val javaagentImplementation: Configuration by configurations.creating
configurations.compileClasspath.configure { extendsFrom(javaagentImplementation) }
configurations.runtimeClasspath.configure { extendsFrom(javaagentImplementation) }

repositories {
    mavenCentral()
}

dependencies {
    javaagentImplementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.21")
    javaagentImplementation("org.ow2.asm:asm:9.2")
    javaagentImplementation("org.ow2.asm:asm-commons:9.2")
    javaagentImplementation("org.ow2.asm:asm-util:9.2")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.18.1")
}

detekt {
    config = files("../detekt-config.yml")
    buildUponDefaultConfig = true

    reports {
        html.enabled = false
        xml.enabled = false
        txt.enabled = false
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    jar {
        manifest {
            attributes["Premain-Class"] = "net.fenstonsingel.fcbp.instrumenter.PremainClass"
            attributes["Can-Retransform-Classes"] = "true"
        }
    }

    shadowJar {
        relocate("org.objectweb.asm", "net.fenstonsingel.fcbp.asm")
    }

    withType<io.gitlab.arturbosch.detekt.Detekt> {
        onlyIf { !project.hasProperty("ignoreDetekt") }
    }
}
