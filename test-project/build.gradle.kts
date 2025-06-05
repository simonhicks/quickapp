plugins {
    kotlin("jvm")
    id("quickapp.kotlin.plugin") version (rootProject.extra["pluginVersion"] as String)
}

repositories {
    mavenLocal()  // For our plugin
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":runtime"))
    
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
} 