plugins {
    java
    kotlin("jvm") version "2.0.20"
    `maven-publish`
}

group = "parraga.bros"
version = "1.0-SNAPSHOT"

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
            groupId = "parraga.bros"
            artifactId = "TennisTournamentLib"
            version = "1.0-SNAPSHOT"

            from(components["java"])
        }
    }
}