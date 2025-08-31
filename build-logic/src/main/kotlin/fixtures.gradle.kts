import com.android.build.gradle.internal.testFixtures.testFixturesFeatureName

plugins {
    id("java-test-fixtures")
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

detekt.source.from("src/$testFixturesFeatureName/kotlin")

kover.currentProject.sources.excludedSourceSets.add(testFixturesFeatureName)
