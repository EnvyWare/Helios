import org.gradle.authentication.http.BasicAuthentication;

plugins {
    id("java")
    id("maven-publish")
}

group = "uk.co.envyware"
version = "1.4-SNAPSHOT"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

repositories {
    mavenCentral()
}

dependencies {

}

publishing {
    publications {
        repositories {
            maven {
                name = "envyware"
                url = uri("https://maven.envyware.co.uk/releases")
                credentials(PasswordCredentials::class)
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }

        create<MavenPublication>("envyware") {
            groupId = project.group.toString()
            artifactId = "helios"
            version = project.version.toString()

            from(components["java"])
        }
    }
}