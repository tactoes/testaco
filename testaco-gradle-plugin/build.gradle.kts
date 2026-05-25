plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    id("maven-publish")
    id("com.github.hierynomus.license")
}

dependencies {
    implementation(project(":testaco-core"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.3")

    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation(gradleTestKit())
  implementation(kotlin("stdlib"))
}

gradlePlugin {
    plugins {
        create("testaco") {
            id = "org.testaco"
            implementationClass = "org.testaco.gradle.TestacoPlugin"
        }
    }
}

repositories {
  mavenCentral()
}

license {
  header = rootProject.file("LICENSE_HEADER")
  include("**/*.kt")
  mapping("kt", "SLASHSTAR_STYLE")
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
