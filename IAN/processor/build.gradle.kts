plugins { id("ian-library") }

dependencies {
    api(libs.kotlin.stdlib)
    api(libs.koin.annotations)
    api(libs.ksp.api)
    api(libs.kotlinpoet)

    implementation(projects.ian.annotations)
    implementation(projects.ian.listener)
    implementation(projects.ian.util)
    implementation(libs.kotlinpoet.ksp)

    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.shared)
    testImplementation(libs.kotest.framework.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.core)
    testImplementation(libs.mockk.dsl)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}