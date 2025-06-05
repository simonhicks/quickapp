plugins {
    kotlin("jvm")
}

val kotlinVersion = rootProject.extra["kotlinVersion"] as String

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
} 