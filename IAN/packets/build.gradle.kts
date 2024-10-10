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
    compileOnly(project(":IAN:annotations"))

    api(project(":IAN:enums"))
    api(project(":IAN:listener"))
    api(project(":IAN:util"))
    api(project(":IAN:vesseldata"))
    api(project(":IAN:world"))

    api(libs.bundles.ian.packets.api)

    ksp(project(":IAN:processor"))
    ksp(libs.ksp.koin)

    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.io)

    testImplementation(testFixtures(project(":IAN:listener")))
    testImplementation(testFixtures(project(":IAN:vesseldata")))
    testImplementation(testFixtures(project(":IAN:world")))

    testFixturesApi(project(":IAN:enums"))
    testFixturesApi(project(":IAN:listener"))
    testFixturesApi(project(":IAN:util"))
    testFixturesApi(project(":IAN:vesseldata"))
    testFixturesApi(project(":IAN:world"))

    testFixturesApi(libs.kotlinx.io)

    testFixturesImplementation(testFixtures(project(":IAN:enums")))
    testFixturesImplementation(testFixtures(project(":IAN:listener")))
    testFixturesImplementation(testFixtures(project(":IAN:util")))
    testFixturesImplementation(testFixtures(project(":IAN:vesseldata")))
    testFixturesImplementation(testFixtures(project(":IAN:world")))

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
