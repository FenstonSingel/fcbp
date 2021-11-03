plugins {
    kotlin("jvm") version "1.5.31"

    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "net.fenstonsingel"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.31")
    implementation("org.javassist:javassist:3.28.0-GA")
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    jar {
        manifest {
            attributes["Premain-Class"] = "net.fenstonsingel.javassist.examples.javaagent.PremainClass"
        }
    }
}
