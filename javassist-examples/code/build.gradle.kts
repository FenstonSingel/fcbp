plugins {
    java
}

group = "net.fenstonsingel"

repositories {
    mavenCentral()
}

dependencies {}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    jar {
        manifest {
            attributes["Main-Class"] = "net.fenstonsingel.javassist.examples.code.JavassistExamples"
        }
    }
}
