import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.7.20"
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
    kotlin("plugin.serialization") version "1.7.20"
}

group = "me.huebnerj"
version = "1.1.0"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    testImplementation(kotlin("test-testng"))
    implementation("dev.kord", "kord-core", "0.8.0-M17")
    implementation("org.slf4j", "slf4j-simple", "2.0.6")
    implementation("org.litote.kmongo", "kmongo-coroutine-serialization", "4.8.0")
}

application {
    mainClass.set("BotKt")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}   