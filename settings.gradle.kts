pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "QuickAppCursor"

include("runtime")
include("plugin")
include("cli")
include("test-project") 