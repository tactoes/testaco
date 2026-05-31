plugins {
    kotlin("jvm") version "2.1.20" apply false
    id("maven-publish")
    id("signing")
}

// Centralized dependency versions
extra["jacksonVersion"] = "2.21.3"
extra["postgresqlDriverVersion"] = "42.7.11"
extra["testcontainersVersion"] = "1.21.4"
extra["kotestVersion"] = "6.1.11"

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

    // Publishing and signing for subprojects (commented out for now)
    /*
    plugins.apply("maven-publish")
    plugins.apply("signing")

    val javadocJar by tasks.registering(Jar::class) {
        archiveClassifier.set("javadoc")
    }

    val sourcesJar by tasks.registering(Jar::class) {
        archiveClassifier.set("sources")
        from((project.extensions.findByName("sourceSets") as? org.gradle.api.tasks.SourceSetContainer)?.named("main")?.get()?.allSource)
    }

    publishing {
        publications {
            create<org.gradle.api.publish.maven.MavenPublication>("mavenJava") {
                from(components["java"])
                artifact(sourcesJar.get())
                artifact(javadocJar.get())

                pom {
                    name.set("testaco")
                    description.set("Testaco library")
                    url.set("https://testaco.org")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    scm {
                        connection.set("scm:git:git@github.com:tactoes/testaco.git")
                        developerConnection.set("scm:git:git@github.com:tactoes/testaco.git")
                        url.set("https://github.com/tactoes/testaco")
                    }
                    developers {
                        developer {
                            id.set("geirhe")
                            name.set("Geir Hedemark")
                        }
                    }
                }
            }
        }

        repositories {
            maven {
                name = "OSSRH"
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME")
                    password = findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
                }
            }
        }
    }

    signing {
        val signingKey = findProperty("signing.key") as String? ?: System.getenv("GPG_PRIVATE_KEY")
        val signingPassword = findProperty("signing.password") as String? ?: System.getenv("GPG_PASSPHRASE")
        if (!signingKey.isNullOrBlank()) {
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(publishing.publications["mavenJava"])
        }
    }
    */
}

// Publishing configuration moved into subprojects block
