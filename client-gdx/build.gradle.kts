plugins {
    kotlin("jvm") version "1.6.20"
    id("application")
}
application {
    mainClass.set("org.demoth.betelgeuse.DesktopLauncher")
}

val gdxVersion = "1.10.0"


dependencies {

    implementation(project(":common"))
    
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("org.springframework:spring-websocket:5.3.12")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
