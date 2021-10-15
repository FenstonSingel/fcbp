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
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }

    jar {
        manifest {
            attributes["Main-Class"] = "net.fenstonsingel.javassist.examples.code.JavassistExamples"
        }
    }
}
