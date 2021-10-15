plugins {
    java
}

group = "net.fenstonsingel.fcbp"

repositories {
    mavenCentral()
}

dependencies {}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
}
