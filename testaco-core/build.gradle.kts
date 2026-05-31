plugins {
    kotlin("jvm")
}

val jacksonVersion = "2.21.3"
val postgresqlDriverVersion = "42.7.11"
val testcontainersVersion = "1.21.4"
dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    compileOnly("org.postgresql:postgresql:$postgresqlDriverVersion")

    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("org.postgresql:postgresql:$postgresqlDriverVersion")
}

