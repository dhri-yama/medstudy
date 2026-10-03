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
    }
}

rootProject.name = "MedStudy"
include(":app")
include(":core:model")
include(":core:data")
include(":core:database")
include(":core:designsystem")
include(":feature:categories")
include(":feature:practice")
include(":feature:progress")
