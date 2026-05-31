plugins {
    kotlin("jvm")
}

val jacksonVersion: String by rootProject.extra
val postgresqlDriverVersion: String by rootProject.extra
val testcontainersVersion: String by rootProject.extra
val kotestVersion: String by rootProject.extra

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    compileOnly("org.postgresql:postgresql:$postgresqlDriverVersion")

    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("org.postgresql:postgresql:$postgresqlDriverVersion")
}

