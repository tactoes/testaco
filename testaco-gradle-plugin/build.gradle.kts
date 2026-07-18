plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

dependencies {
    implementation(project(":testaco-core"))
    implementation(libs.jacksonDatabind)
    implementation(libs.jacksonModuleKotlin)

    testImplementation(libs.kotestRunner)
    testImplementation(libs.kotestAssertions)
    testImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        create("testaco") {
            id = "org.testaco"
            implementationClass = "org.testaco.gradle.TestacoPlugin"
        }
    }
}

