plugins {
    kotlin("jvm") version "2.2.21"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.kotest:kotest-runner-junit5-jvm:6.1.3")
    testImplementation("io.kotest:kotest-assertions-core:6.1.3")
    testImplementation("io.kotest:kotest-framework-datatest:6.1.3")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}