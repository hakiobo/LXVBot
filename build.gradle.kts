import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.6.10"
    java
    id("com.github.johnrengelman.shadow") version "6.1.0"
    kotlin("plugin.serialization") version "1.6.10"
}

group = "me.huebnerj"
version = "0.4.0"

repositories {
    mavenCentral()
    jcenter()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    testImplementation(kotlin("test-testng"))
    implementation("dev.kord", "kord-core", "0.8.0-M8")
    implementation("org.slf4j", "slf4j-simple", "1.7.30")
    implementation("org.litote.kmongo", "kmongo-coroutine-serialization", "4.4.0")
}

application {
    mainClassName = "BotKt"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}