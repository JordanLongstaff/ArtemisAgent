plugins { id("ian-library") }

tasks.test { useJUnitPlatform() }

dependencies {
    testImplementation(libs.bundles.konsist.common)
    testRuntimeOnly(libs.bundles.konsist.runtime)
}
