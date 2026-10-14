pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // The Vatio SDK is built by JitPack from github.com/vatio-ai/android.
        maven("https://jitpack.io")
    }
}

rootProject.name = "vatio-chat-android"
include(":app")
