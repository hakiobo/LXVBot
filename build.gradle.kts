import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.4.30"
}

application.mainClassName = "BotKt"

group = "me.huebnerj"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    testImplementation(kotlin("test-testng"))
    implementation("dev.kord:kord-core:0.7.0-SNAPSHOT")
    implementation("org.mongodb:mongodb-driver-sync:4.1.0")
    implementation("org.litote.kmongo:kmongo:4.1.2")
    implementation("org.slf4j:slf4j-simple:1.7.30")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "BotKt"
    }
    from(sourceSets.main.get().output)
//    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
    archiveFileName.set("LXV.jar")
}

