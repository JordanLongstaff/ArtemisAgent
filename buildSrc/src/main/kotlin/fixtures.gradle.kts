import artemis.agent.gradle.includeSourceSets
import com.android.build.gradle.internal.testFixtures.testFixturesFeatureName

plugins {
    id("java-test-fixtures")
    id("io.gitlab.arturbosch.detekt")
    id("org.jetbrains.kotlinx.kover")
}

detekt.includeSourceSets(testFixturesFeatureName)

kover.currentProject.sources.excludedSourceSets.add(testFixturesFeatureName)
