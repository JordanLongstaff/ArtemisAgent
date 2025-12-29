plugins {
    id("java-test-fixtures")
    id("io.gitlab.arturbosch.detekt")
}

val testFixtures = "testFixtures"

detekt.source.from("src/$testFixtures/kotlin")
