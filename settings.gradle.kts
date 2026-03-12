pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        flatDir {
            dirs("libs")
        }
        google()
        mavenCentral()
        maven(url = "https://api.xposed.info")
    }
}

plugins {
    id("com.android.settings") version ("8.13.0")
}

android {
    compileSdk = 36
    minSdk = 27
}

rootProject.name = "Shutify"
include(":app")
include(":stub")
