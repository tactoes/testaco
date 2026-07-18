plugins {
    kotlin("jvm")
}

dependencies {
    implementation(libs.jacksonDatabind)
    implementation(libs.jacksonModuleKotlin)
    implementation(libs.slf4jApi)
    implementation(libs.logbackCore)
    implementation(libs.logbackClassic)

    compileOnly(libs.postgresql)

    testImplementation(libs.kotestRunner)
    testImplementation(libs.kotestAssertions)
    testImplementation(libs.testcontainersPostgresql)
    testImplementation(libs.testcontainersJunit)
    testImplementation(libs.postgresql)
}

