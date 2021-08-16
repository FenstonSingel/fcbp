plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm")
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

group = parent!!.group
version = parent!!.version

/*
 * this is (seemingly) a weird hack to basically exclude
 * this subproject's dependencies from distribution dependencies of
 * the actual plugin while still being able to make a fat jar for
 * the javaagent and otherwise use them as standard dependencies
 * the hack (seems to) depend on how the intellij gradle plugin tasks
 * scan the "runtimeClasspath" configuration
 */
val javaagentImplementation: Configuration by configurations.creating
configurations.compileClasspath.configure {
    extendsFrom(javaagentImplementation)
}
configurations.runtimeClasspath.configure {
    extendsFrom(javaagentImplementation)
}

repositories {
    mavenCentral()
}
dependencies {
    javaagentImplementation(kotlin("stdlib"))
    javaagentImplementation(files("libs/fs-asm-9.2.jar"))
    javaagentImplementation(files("libs/fs-asm-commons-9.2.jar"))
    javaagentImplementation(files("libs/fs-asm-tree-9.2.jar"))
    javaagentImplementation(files("libs/fs-asm-analysis-9.2.jar"))
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.17.1")
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

tasks.jar {
    manifest {
        attributes["Premain-Class"] = "net.fenstonsingel.fcbp.instrumenter.PremainClass"
    }
    val sourcePaths = javaagentImplementation.map { classpathFile ->
        if (classpathFile.isDirectory)
            classpathFile
        else
            zipTree(classpathFile).matching { exclude("**/module-info.class") }
    }
    from(sourcePaths)
}
