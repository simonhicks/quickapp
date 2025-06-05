buildscript {
    extra["kotlinVersion"] = "1.9.23"
    extra["pluginVersion"] = "1.0-SNAPSHOT"
}

plugins {
    kotlin("jvm") version "${project.extra["kotlinVersion"]}" apply false
}

allprojects {
    group = "com.quickappcursor"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java")

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}

tasks.register("publish") {
    group = "publishing"
    description = "Publishes all artifacts (placeholder)"
    // TODO: Implement actual publishing logic
} 