import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-library")
    id("java-test-fixtures")
    id("kotlin")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kover)
    id("info.solidsoft.pitest")
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.detekt)
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
    jvmArgs("-Xmx2g", "-Xms1g", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:+UseParallelGC")
    useJUnitPlatform()
}

tasks.assemble.dependsOn(":IAN:world:konsist:test")

ktfmt {
    kotlinLangStyle()
}

detekt {
    source.setFrom(file("src/main/kotlin"))
    config.setFrom(file("$rootDir/config/detekt/detekt.yml"))
}

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
    targetClasses = listOf("com.walkertribe.ian.world.*")
    threads = 2
    timeoutFactor = pitestTimeoutFactor
    outputFormats = listOf("HTML", "CSV")
    timestampedReports = false
    setWithHistory(true)
    mutators.addAll(pitestMutators)
}
