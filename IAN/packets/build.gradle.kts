import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-library")
    id("java-test-fixtures")
    id("kotlin")
    alias(libs.plugins.kover)
    id("info.solidsoft.pitest")
    alias(libs.plugins.detekt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dependency.analysis)
}

val javaVersion: JavaVersion by rootProject.extra

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

tasks.compileKotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
        javaParameters = true
    }
}

tasks.test {
    jvmArgs("-Xmx4g", "-Xms1g", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:+UseParallelGC")
    useJUnitPlatform()
}

tasks.assemble.dependsOn(":IAN:packets:konsist:test")

detekt {
    source.setFrom(file("src/main/kotlin"))
    config.setFrom(file("$rootDir/config/detekt/detekt.yml"))
}

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

kover {
    currentProject {
        sources {
            excludedSourceSets.add("testFixtures")
        }
    }
}

val pitestMutators: Set<String> by rootProject.extra
val pitestTimeoutFactor: BigDecimal by rootProject.extra

pitest {
    pitestVersion = libs.versions.pitest.asProvider()
    junit5PluginVersion = libs.versions.pitest.junit5
    verbose = true
    targetClasses = listOf("com.walkertribe.ian.protocol.*")
    threads = 8
    timeoutFactor = pitestTimeoutFactor
    outputFormats = listOf("HTML", "CSV")
    timestampedReports = false
    setWithHistory(true)
    mutators.addAll(pitestMutators)
    jvmArgs = listOf("-Xmx8g", "-Xms1g", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:+UseParallelGC")
}
