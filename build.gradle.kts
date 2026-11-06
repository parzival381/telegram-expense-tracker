plugins {
    kotlin("jvm") version "2.4.21"
    application
}

group = "dev.portfolio"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.2")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.40.0")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("dev.portfolio.expensetracker.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
