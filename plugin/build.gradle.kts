plugins {
    id("java-gradle-plugin")
    id("org.jetbrains.kotlin.kapt")
    id("maven-publish")
}

group = "quickapp"
version = rootProject.extra["pluginVersion"] as String

// Read kotlinVersion from root project
val kotlinVersion = rootProject.extra["kotlinVersion"] as String

repositories {
    mavenCentral()
    gradlePluginPortal()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
    
    // Keep minimal test dependencies for FilenameCleaner test
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Register the compiler plugin ID and entry point
gradlePlugin {
    plugins {
        create("quickAppPlugin") {
            id = "quickapp.kotlin.plugin"
            implementationClass = "quickapp.plugin.QuickAppGradlePlugin"
            displayName = "QuickApp Kotlin Plugin"
            description = "A Kotlin compiler plugin for QuickApp"
            tags.set(listOf("kotlin", "compiler-plugin"))
        }
    }
}

// Configure publishing
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "plugin"
            version = project.version.toString()
        }
    }
}

// Make the compiler plugin available to other modules
tasks.jar {
    manifest {
        attributes(mapOf(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Plugin-Version" to project.version,
            "Kotlin-Version" to kotlinVersion,
            "Kotlin-Compiler-Plugin-Id" to "quickapp.kotlin.plugin",
            "Kotlin-Compiler-Plugin-Version" to project.version
        ))
    }
} 
