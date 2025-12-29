plugins {
    id("java-test-fixtures")
    id("io.gitlab.arturbosch.detekt")
    id("org.jetbrains.kotlinx.kover")
}

val testFixtures = "testFixtures"

detekt.source.from("src/$testFixtures/kotlin")

kover.currentProject.sources.excludedSourceSets.add(testFixtures)
