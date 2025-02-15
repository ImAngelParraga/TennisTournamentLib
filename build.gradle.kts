plugins {
    java
    kotlin("jvm") version "2.0.21"
    `maven-publish`
}

group = "parraga.bros"
version = "0.0.2"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            group = "parraga.bros"
            version = "0.0.1"
            artifactId = "TennisTournamentLib"

            from(components["java"])
        }
    }
}