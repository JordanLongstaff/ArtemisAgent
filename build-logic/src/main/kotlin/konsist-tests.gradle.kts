plugins { id("ian-library") }

tasks.test { useJUnitPlatform() }

dependencies {
    testImplementation(platform(libs.kotest.bom))
    testImplementation(libs.bundles.konsist.common)
    testRuntimeOnly(libs.bundles.konsist.runtime)
}
