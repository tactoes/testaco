plugins {
    kotlin("jvm")
}

dependencies {
    implementation(libs.jacksonDatabind)
    implementation(libs.jacksonModuleKotlin)

    compileOnly(libs.postgresql)

    testImplementation(libs.kotestRunner)
    testImplementation(libs.kotestAssertions)
    testImplementation(libs.testcontainersPostgresql)
    testImplementation(libs.testcontainersJunit)
    testImplementation(libs.postgresql)
}

