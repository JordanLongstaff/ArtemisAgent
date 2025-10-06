import artemis.agent.gradle.configure
import artemis.agent.gradle.configureTests
import artemis.agent.gradle.dependsOnKonsist

plugins {
    id("ian-library")
    id("fixtures")
    id("info.solidsoft.pitest")
}

configureTests()

pitest.configure(rootPackage = "com.walkertribe.ian.vesseldata", threads = 2)

dependsOnKonsist()

dependencies {
    api(projects.ian.enums)
    api(projects.ian.util)
    api(libs.kotlin.stdlib)

    implementation(libs.bundles.ian.vesseldata)

    testFixturesImplementation(projects.ian.enums)
    testFixturesImplementation(libs.bundles.ian.vesseldata.test.fixtures)

    testImplementation(libs.bundles.ian.vesseldata.test)
    testRuntimeOnly(libs.bundles.ian.test.runtime)

    pitest(projects.ian.enums)
    pitest(projects.ian.util)
    pitest(libs.bundles.arcmutate)
}
