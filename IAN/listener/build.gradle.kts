import artemis.agent.gradle.configure
import artemis.agent.gradle.configureTests
import artemis.agent.gradle.dependsOnKonsist

plugins {
    id("ian-library")
    id("fixtures")
    id("info.solidsoft.pitest")
}

val byteBuddyAgent: Configuration by configurations.creating

configureTests()

tasks.test { jvmArgs("-javaagent:${byteBuddyAgent.asPath}") }

pitest.configure(rootPackage = "com.walkertribe.ian.iface", threads = 2)

dependsOnKonsist()

dependencies {
    api(libs.kotlin.stdlib)
    api(libs.kotlinx.coroutines)

    implementation(libs.kotlin.reflect)

    testImplementation(libs.bundles.ian.listener.test)
    testFixturesImplementation(libs.kotlin.reflect)
    testRuntimeOnly(libs.bundles.ian.test.runtime)

    byteBuddyAgent(libs.byte.buddy.agent)

    pitest(libs.bundles.arcmutate)
}
