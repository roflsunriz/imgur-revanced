rootProject.name = "imgur-revanced"

pluginManagement {
    if (file("temp/revanced-patches-gradle-plugin-reference").isDirectory) {
        includeBuild("temp/revanced-patches-gradle-plugin-reference")
    }

    repositories {
        maven {
            name = "localReVancedDevelopment"
            url = uri(file("temp/maven"))
        }
        gradlePluginPortal()
        google()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/revanced/registry")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                    ?: System.getenv("GITHUB_ACTOR")
                    ?: "unused"
                password = providers.gradleProperty("gpr.key").orNull
                    ?: System.getenv("GITHUB_TOKEN")
                    ?: "unused"
            }
        }
    }
}

plugins {
    id("app.revanced.patches") version "1.0.0-dev.11"
}

dependencyResolutionManagement {
    repositories {
        maven {
            name = "localReVancedDevelopment"
            url = uri(file("temp/maven"))
        }
    }
}

settings {
    extensions {
        defaultNamespace = "app.revanced.extension.imgur"
    }
}
