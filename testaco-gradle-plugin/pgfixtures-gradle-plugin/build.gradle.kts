plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

dependencies {
    implementation(project(":testaco-core"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.3")

    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        create("testaco") {
            id = "org.testaco"
            implementationClass = "org.testaco.gradle.PgFixturesPlugin"
        }
    }
}

