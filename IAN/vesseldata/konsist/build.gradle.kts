import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("java-library")
  id("kotlin")
  alias(libs.plugins.detekt)
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

tasks.test { useJUnitPlatform() }

ktfmt { kotlinLangStyle() }

dependencies {
  testImplementation(projects.ian.testing)
  testImplementation(libs.bundles.konsist.common)
  testImplementation(libs.bundles.konsist.vesseldata)
  testRuntimeOnly(libs.bundles.konsist.runtime)
}
