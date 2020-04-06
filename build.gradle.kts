import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.71"
    id("com.diffplug.gradle.spotless") version "3.25.0"
}

group = "io.fu"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("io.netty:netty-all:4.1.48.Final")
    implementation("io.netty:netty-tcnative:2.0.30.Final")

    implementation("org.dyn4j:dyn4j:3.4.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    format("misc") {
        target("*.gradle", "*.md", ".gitgnore")

        trimTrailingWhitespace()
        indentWithTabs()
        endWithNewline()
    }
    kotlin {
        ktlint()
    }
}
