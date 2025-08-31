import artemis.agent.gradle.configure
import artemis.agent.gradle.configureTests
import artemis.agent.gradle.dependsOnKonsist

plugins {
    id("ian-library")
    id("fixtures")
    id("info.solidsoft.pitest")
}

configureTests()
pitest.configure(rootPackage = "com.walkertribe.ian.iface", threads = 2)

dependsOnKonsist()

dependencies {
    api(libs.kotlin.stdlib)
    api(libs.kotlinx.coroutines)

    implementation(libs.kotlin.reflect)

    testImplementation(libs.bundles.ian.listener.test)
    testFixturesImplementation(libs.kotlin.reflect)
    testRuntimeOnly(libs.bundles.ian.test.runtime)

    pitest(libs.bundles.arcmutate)
}
