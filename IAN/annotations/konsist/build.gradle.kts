plugins { id("konsist-tests") }

dependencies {
    testCompileOnly(projects.ian.annotations)
    testImplementation(libs.bundles.konsist.ian)
}
