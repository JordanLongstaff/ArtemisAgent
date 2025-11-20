import artemis.agent.gradle.configurePitest
import artemis.agent.gradle.configureTests

plugins {
    id("ian-library")
    id("fixtures")
    id("info.solidsoft.pitest")
}

configureTests()

configurePitest(rootPackage = "com.walkertribe.ian.util", threads = 2)

dependencies {
    api(libs.kotlin.stdlib)
    api(libs.kotlinx.io)

    implementation(libs.bundles.ian.util)

    testImplementation(platform(libs.kotest.bom))
    testImplementation(libs.bundles.ian.util.test)
    testRuntimeOnly(libs.bundles.ian.test.runtime)

    testFixturesImplementation(platform(libs.kotest.bom))
    testFixturesImplementation(libs.bundles.ian.util.test.fixtures)

    pitest(libs.bundles.arcmutate)
}
