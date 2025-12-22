import artemis.agent.gradle.configure
import artemis.agent.gradle.configureTests

plugins {
    id("ian-library")
    id("fixtures")
    id("info.solidsoft.pitest")
}

configureTests()

pitest.configure(rootPackage = "com.walkertribe.ian.grid", threads = 2)

dependencies {
    api(projects.ian.enums)
    api(projects.ian.util)
    api(libs.kotlin.stdlib)

    implementation(libs.okio)

    testFixturesApi(projects.ian.enums)
    testFixturesImplementation(platform(libs.kotest.bom))
    testFixturesImplementation(libs.bundles.ian.grid.test.fixtures)

    testImplementation(platform(libs.kotest.bom))
    testImplementation(libs.bundles.ian.vesseldata.test)
    testRuntimeOnly(libs.bundles.ian.test.runtime)

    pitest(libs.bundles.arcmutate)

    pitest(projects.ian.enums)
    pitest(projects.ian.util)
}
