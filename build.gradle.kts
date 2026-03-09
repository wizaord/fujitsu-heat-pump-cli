import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion: String by project
val cliktVersion: String by project
val coroutinesVersion: String by project
val serializationVersion: String by project
val junitVersion: String by project
val assertjVersion: String by project

plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    application
}

group = "com.airstage"
version = "1.0.0"

repositories {
    mavenCentral()
}

// Source set séparé pour les tests d'intégration
val integrationTest: SourceSet by sourceSets.creating {
    kotlin.srcDir("src/integrationTest/kotlin")
    compileClasspath += sourceSets["main"].output + sourceSets["main"].compileClasspath
    runtimeClasspath += output + compileClasspath
}

val integrationTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations["implementation"])
}

val integrationTestRuntimeOnly: Configuration by configurations.getting

dependencies {
    // Client HTTP async
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // CLI
    implementation("com.github.ajalt.clikt:clikt:$cliktVersion")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    // JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")

    // Tests d'intégration
    integrationTestImplementation("io.ktor:ktor-server-core:$ktorVersion")
    integrationTestImplementation("io.ktor:ktor-server-cio:$ktorVersion")
    integrationTestImplementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    integrationTestImplementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    integrationTestImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    integrationTestImplementation("org.assertj:assertj-core:$assertjVersion")
    integrationTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    integrationTestRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter(tasks.test)
}

application {
    mainClass.set("com.airstage.MainKt")
}
