import artemis.agent.gradle.configure
import artemis.agent.gradle.dependsOnKonsist
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library")
    id("kotlin")
    fixtures
    id("info.solidsoft.pitest")
    alias(libs.plugins.ktfmt)
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

dependsOnKonsist()

ktfmt { kotlinLangStyle() }

dependencies {
    api(libs.kotlin.stdlib)
    api(libs.kotlinx.coroutines)

    implementation(libs.kotlin.reflect)

    testImplementation(libs.bundles.ian.listener.test)
    testFixturesImplementation(libs.kotlin.reflect)
    testRuntimeOnly(libs.bundles.ian.test.runtime)

    pitest(libs.bundles.arcmutate)
}

pitest.configure(rootPackage = "com.walkertribe.ian.iface", threads = 2)
