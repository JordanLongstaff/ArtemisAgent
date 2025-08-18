import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library")
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

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
        javaParameters = true
    }
}

tasks.test {
    jvmArgs("-Xmx2g", "-Xms1g", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:+UseParallelGC")
    useJUnitPlatform()
}

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

ktfmt { kotlinLangStyle() }

dependencies {
    compileOnly(projects.ian.annotations)

    api(projects.ian.enums)
    api(projects.ian.listener)
    api(projects.ian.packets)
    api(projects.ian.util)
    api(projects.ian.world)

    api(libs.kotlin.stdlib)

    ksp(projects.ian.processor)
    ksp(libs.ksp.koin)

    implementation(libs.bundles.ian)

    runtimeOnly(libs.kotlin.reflect)

    testImplementation(testFixtures(projects.ian.listener))
    testImplementation(testFixtures(projects.ian.packets))
    testImplementation(testFixtures(projects.ian.util))
    testImplementation(libs.bundles.ian.test)
    testRuntimeOnly(libs.bundles.ian.test.runtime)

    pitest(libs.bundles.arcmutate)
}

val pitestMutators: Set<String> by rootProject.extra
val pitestTimeoutFactor: BigDecimal by rootProject.extra

pitest {
    pitestVersion = libs.versions.pitest.asProvider()
    junit5PluginVersion = libs.versions.pitest.junit5
    verbose = true
    targetClasses = listOf("com.walkertribe.ian.*")
    threads = 2
    timeoutFactor = pitestTimeoutFactor
    outputFormats = listOf("HTML", "CSV")
    timestampedReports = false
    setWithHistory(true)
    mutators.addAll(pitestMutators)
}
