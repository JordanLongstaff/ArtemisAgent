import artemis.agent.gradle.configure
import artemis.agent.gradle.configureTests
import artemis.agent.gradle.dependsOnKonsist

plugins {
    id("ian-library")
    alias(libs.plugins.kover)
    id("info.solidsoft.pitest")
}

configureTests()
pitest.configure(rootPackage = "com.walkertribe.ian.protocol", threads = 2)

dependsOnKonsist()

dependencies {
    implementation(libs.kotlinx.io)
    api(libs.bundles.ian.udp.api)

    testImplementation(libs.bundles.ian.udp.test)
    testRuntimeOnly(libs.bundles.ian.test.runtime)

    pitest(libs.bundles.arcmutate)
}
