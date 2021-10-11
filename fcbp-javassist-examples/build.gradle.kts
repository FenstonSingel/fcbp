plugins {
    java
}

group = "net.fenstonsingel.fcbp"

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
}
