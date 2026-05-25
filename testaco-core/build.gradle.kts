plugins {
    kotlin("jvm")
    id("maven-publish")
    id("com.github.hierynomus.license")
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.3")

    compileOnly("org.postgresql:postgresql:42.7.11")

    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("org.testcontainers:postgresql:1.20.6")
    testImplementation("org.testcontainers:junit-jupiter:1.20.6")
    testImplementation("org.postgresql:postgresql:42.7.11")
  implementation(kotlin("stdlib-jdk8"))
}

repositories {
  mavenCentral()
}

license {
  header = rootProject.file("LICENSE_HEADER")
  include("**/*.kt")
  mapping("kt", "SLASHSTAR_STYLE")
}
kotlin {
  jvmToolchain(8)
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])
      pom {
        licenses {
          license {
            name.set("The Apache Software License, Version 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            distribution.set("repo")
          }
        }
      }
    }
  }
}
