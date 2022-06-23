rootProject.name = "fcbp"

include(":instrumenter")
project(":instrumenter").projectDir = File("fcbp-instrumenter")
include(":shared")
project(":shared").projectDir = File("fcbp-shared")
