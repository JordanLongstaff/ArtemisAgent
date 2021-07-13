import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("java-library")
  id("java-test-fixtures")
  id("kotlin")
  alias(libs.plugins.kover)
  id("info.solidsoft.pitest")
  alias(libs.plugins.detekt)
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

detekt {
  source.setFrom(file("src/main/kotlin"))
  config.setFrom(file("$rootDir/config/detekt/detekt.yml"))
}

dependencies {
  api(libs.kotlin.stdlib)
  implementation(libs.bundles.ian.util)
  testImplementation(libs.bundles.ian.util.test)
  testFixturesImplementation(libs.bundles.ian.util.test.fixtures)
  testRuntimeOnly(libs.bundles.ian.test.runtime)
}

kover {
  currentProject {
    sources {
      excludedSourceSets.add("testFixtures")
    }
  }
}

val pitestMutators: Set<String> by rootProject.extra

pitest {
  pitestVersion = libs.versions.pitest.asProvider()
  junit5PluginVersion = libs.versions.pitest.junit5
  verbose = true
  targetClasses = listOf("com.walkertribe.ian.util.*")
  threads = 2
  outputFormats = listOf("HTML", "CSV")
  timestampedReports = false
  setWithHistory(true)
  mutators.addAll(pitestMutators)
}
