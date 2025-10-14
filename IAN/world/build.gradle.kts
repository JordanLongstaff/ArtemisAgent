import artemis.agent.gradle.configure
import artemis.agent.gradle.configureTests
import artemis.agent.gradle.dependsOnKonsist

plugins {
    id("ian-library")
    id("fixtures")
    alias(libs.plugins.ksp)
    id("info.solidsoft.pitest")
}

configureTests()

pitest.configure(rootPackage = "com.walkertribe.ian.world", threads = 2)

dependsOnKonsist()

dependencies {
    api(projects.ian.enums)
    api(projects.ian.listener)
    api(projects.ian.util)
    api(projects.ian.vesseldata)
    api(libs.kotlin.stdlib)

    ksp(projects.ian.processor)

    testFixturesApi(projects.ian.listener)
    testFixturesApi(projects.ian.util)
    testFixturesImplementation(libs.bundles.ian.world.test.fixtures)

    testImplementation(testFixtures(projects.ian.util))
    testImplementation(testFixtures(projects.ian.vesseldata))
    testImplementation(libs.bundles.ian.world.test)
    testRuntimeOnly(libs.bundles.ian.test.runtime)

    pitest(libs.bundles.arcmutate)

    pitest(testFixtures(projects.ian.util))
    pitest(testFixtures(projects.ian.vesseldata))
}
