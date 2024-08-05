plugins {
    application
    kotlin("jvm") version "2.0.0"
    id("io.github.goooler.shadow") version "8.1.7"
}

group = "de.blazemcworld"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // minestom
    implementation("net.minestom:minestom-snapshots:1f34e60ea6")

    // minimessage
    implementation("net.kyori:adventure-text-minimessage:4.17.0")

    // database
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.4.1")
    implementation("org.jetbrains.exposed:exposed-core:0.52.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.52.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.52.0")
    implementation("org.jetbrains.exposed:exposed-json:0.52.0")

    // compression
    implementation("com.github.luben:zstd-jni:1.5.6-4")

    // logging
    implementation("io.github.oshai:kotlin-logging-jvm:6.0.9")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.23.1")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "de.blazemcworld.fireflow.FireFlowKt"
    }
}
application {
    mainClass.set("de.blazemcworld.fireflow.FireFlowKt")
    tasks.run.get().workingDir = File(rootProject.projectDir, ".run")
}
