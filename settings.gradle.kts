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

rootProject.name = "CadernoDeReceitas"

// Etapa 1 (Parcial): telas em XML com Views, navegação por Intent explícita e dados simulados
include(":parcial")
// Etapa 2: app completo em Jetpack Compose
include(":compose")
