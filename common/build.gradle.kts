plugins {
    kotlin("jvm") version "1.6.20"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
}


dependencies {
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("com.fasterxml.jackson.core:jackson-annotations:2.13.2")
    api("io.swagger:swagger-annotations:1.6.6")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}