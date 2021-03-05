import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.4.30"
    java
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "me.huebnerj"
version = "0.1.1"

repositories {
    mavenCentral()
    jcenter()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    testImplementation(kotlin("test-testng"))
    implementation("dev.kord", "kord-core", "0.7.0-SNAPSHOT")
    implementation("org.slf4j", "slf4j-simple", "1.7.30")
    implementation("org.litote.kmongo", "kmongo-coroutine", "4.2.4")
}

application {
    mainClassName = "BotKt"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}