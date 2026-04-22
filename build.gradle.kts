plugins {
    java
    kotlin("jvm") version "2.0.21"
    `maven-publish`
}

group = "parraga.bros"
version = "0.0.3"

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
            version = "0.0.3"
            artifactId = "TennisTournamentLib"

            from(components["java"])
        }
    }
}