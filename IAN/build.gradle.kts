import artemis.agent.gradle.configure
import artemis.agent.gradle.configureTests
import com.android.build.gradle.internal.tasks.factory.dependsOn

plugins {
    id("ian-library")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kover)
    id("info.solidsoft.pitest")
}

configureTests()

pitest.configure(rootPackage = "com.walkertribe.ian", threads = 2)

val konsistCollect by
    tasks.registering {
        group = "build"
        description = "Runs all Konsist unit tests of all subprojects."
    }

allprojects
    .filter { it.path.contains("konsist") }
    .forEach { project ->
        project.tasks.whenTaskAdded {
            if (name == "test") {
                konsistCollect.dependsOn(path)
            }
        }
    }

tasks.assemble.dependsOn(konsistCollect)

dependencies {
    compileOnly(projects.ian.annotations)

    api(projects.ian.enums)
    api(projects.ian.listener)
    api(projects.ian.packets)
    api(projects.ian.util)
    api(projects.ian.world)

    api(libs.kotlin.stdlib)

    ksp(projects.ian.processor)

    implementation(libs.bundles.ian)

    runtimeOnly(libs.kotlin.reflect)

    testImplementation(testFixtures(projects.ian.listener))
    testImplementation(testFixtures(projects.ian.packets))
    testImplementation(testFixtures(projects.ian.util))
    testImplementation(libs.bundles.ian.test)
    testRuntimeOnly(libs.bundles.ian.test.runtime)

    pitest(libs.bundles.arcmutate)

    pitest(testFixtures(projects.ian.listener))
    pitest(testFixtures(projects.ian.packets))
    pitest(testFixtures(projects.ian.util))
}
