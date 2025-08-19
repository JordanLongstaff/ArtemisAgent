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
  testImplementation(projects.ian.world)
  testCompileOnly(projects.ian.annotations)

  testImplementation(libs.bundles.konsist.common)
  testImplementation(libs.bundles.konsist.ian)
  testRuntimeOnly(libs.bundles.konsist.runtime)
}
