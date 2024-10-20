plugins {
    java
    kotlin("jvm") version "2.0.20"
    `maven-publish`
}

group = "parraga.bros"
version = "1.0"

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
            version = "1.0"
            artifactId = "TennisTournamentLib"

            from(components["java"])
        }
    }
}