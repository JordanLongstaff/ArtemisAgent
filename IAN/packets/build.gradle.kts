import artemis.agent.gradle.configure
import artemis.agent.gradle.dependsOnKonsist
import artemis.agent.gradle.excludeTestFixtures
import artemis.agent.gradle.includeSourceSets
import com.android.build.gradle.internal.testFixtures.testFixturesFeatureName
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library")
    id("java-test-fixtures")
    id("kotlin")
    alias(libs.plugins.ksp)
    id("org.jetbrains.kotlinx.kover")
    id("info.solidsoft.pitest")
    alias(libs.plugins.ktfmt)
    id("io.gitlab.arturbosch.detekt")
    alias(libs.plugins.dependency.analysis)
}

val javaVersion: JavaVersion by rootProject.extra

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
        javaParameters = true
    }
}

tasks.test {
    jvmArgs("-Xmx4g", "-Xms1g", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:+UseParallelGC")
    useJUnitPlatform()
}

kover.excludeTestFixtures()

dependsOnKonsist()

ktfmt { kotlinLangStyle() }

detekt.includeSourceSets(testFixturesFeatureName)

dependencies {
    compileOnly(projects.ian.annotations)

    api(projects.ian.enums)
    api(projects.ian.listener)
    api(projects.ian.util)
    api(projects.ian.vesseldata)
    api(projects.ian.world)

    api(libs.bundles.ian.packets.api)

    ksp(projects.ian.processor)
    ksp(libs.ksp.koin)

    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.io)

    testImplementation(testFixtures(projects.ian.listener))
    testImplementation(testFixtures(projects.ian.vesseldata))
    testImplementation(testFixtures(projects.ian.world))

    testFixturesApi(projects.ian.enums)
    testFixturesApi(projects.ian.listener)
    testFixturesApi(projects.ian.util)
    testFixturesApi(projects.ian.vesseldata)
    testFixturesApi(projects.ian.world)

    testFixturesApi(libs.kotlinx.io)

    testFixturesImplementation(testFixtures(projects.ian.enums))
    testFixturesImplementation(testFixtures(projects.ian.listener))
    testFixturesImplementation(testFixtures(projects.ian.util))
    testFixturesImplementation(testFixtures(projects.ian.vesseldata))
    testFixturesImplementation(testFixtures(projects.ian.world))

    testFixturesImplementation(libs.bundles.ian.packets.test.fixtures)
    testImplementation(libs.bundles.ian.packets.test)
    testRuntimeOnly(libs.bundles.ian.test.runtime)

    pitest(libs.bundles.arcmutate)
}

pitest {
    jvmArgs = listOf("-Xmx8g", "-Xms1g", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:+UseParallelGC")
    configure(rootPackage = "com.walkertribe.ian.protocol", threads = 8)
}
