plugins { id("konsist-tests") }

dependencies {
    testImplementation(projects.ian.annotations)
    testImplementation(projects.ian.packets)

    testImplementation(libs.bundles.konsist.ian)
}
