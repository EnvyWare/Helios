import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "1.9.24"
  id("org.jetbrains.intellij") version "1.17.3"
  id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "uk.co.envyware.helios.idea"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
  maven("https://maven.envyware.co.uk/releases")
}

intellij {
  version.set("2023.3.7")
  plugins.set(listOf("com.intellij.java"))
}

dependencies {
  implementation("uk.co.envyware:helios:1.0-SNAPSHOT")
}

tasks {
  withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
  }
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
  }

  withType<ShadowJar> {
    archiveBaseName.set("Helios-Plugin-" + project.version)
    archiveVersion.set("")
    archiveClassifier.set("")
  }

  patchPluginXml {
    sinceBuild.set("232")
    untilBuild.set("242.*")
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }
}
