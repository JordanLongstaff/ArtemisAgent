import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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

tasks.compileKotlin {
  compilerOptions {
    jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
    javaParameters = true
  }
}

tasks.test { useJUnitPlatform() }

ktfmt { kotlinLangStyle() }

dependencies {
  testImplementation(projects.ian.testing)
  testImplementation(projects.ian.udp)
  testImplementation(libs.bundles.konsist.common)
  testRuntimeOnly(libs.bundles.konsist.runtime)
}
