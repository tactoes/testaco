plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

val jacksonVersion: String by rootProject.extra
val kotestVersion: String by rootProject.extra

dependencies {
    implementation(project(":testaco-core"))
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
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

