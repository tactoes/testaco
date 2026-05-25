plugins {
    kotlin("jvm") version "2.1.20" apply false
}

allprojects {
    group = "org.testaco"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(25)
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_23)
        }
    }

    tasks.withType<JavaCompile> {
        targetCompatibility = "23"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

