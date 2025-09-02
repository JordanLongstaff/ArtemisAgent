import artemis.agent.gradle.configure
import artemis.agent.gradle.configureTests

plugins {
    id("ian-library")
    id("fixtures")
    id("info.solidsoft.pitest")
}

configureTests()

pitest.configure(rootPackage = "com.walkertribe.ian.util", threads = 2)

dependencies {
    api(libs.kotlin.stdlib)
    api(libs.kotlinx.io)

    implementation(libs.bundles.ian.util)

    testImplementation(libs.bundles.ian.util.test)
    testFixturesImplementation(libs.bundles.ian.util.test.fixtures)
    testRuntimeOnly(libs.bundles.ian.test.runtime)

    pitest(libs.bundles.arcmutate)
}
