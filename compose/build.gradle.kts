import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Chave da TheMealDB: lida do local.properties (que NÃO vai para o GitHub).
// Se não existir, usa "1", a chave pública de testes documentada pela própria TheMealDB,
// então o projeto roda sem nenhuma configuração extra.
val propriedadesLocais = Properties().apply {
    val arquivo = rootProject.file("local.properties")
    if (arquivo.exists()) arquivo.inputStream().use { load(it) }
}
val chaveTheMealDb: String = propriedadesLocais.getProperty("THEMEALDB_API_KEY")
    ?: System.getenv("THEMEALDB_API_KEY")
    ?: "1"

android {
    namespace = "br.com.caderno.receitas"
    compileSdk = 37

    defaultConfig {
        applicationId = "br.com.caderno.receitas"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "THEMEALDB_API_KEY", "\"$chaveTheMealDb\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        // Testes de interface Compose rodam na JVM com Robolectric (sem emulador).
        unitTests.isIncludeAndroidResources = true
    }
}

ksp {
    // Guarda o esquema do banco em compose/schemas (ajuda a revisar e migrar versões).
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Interface
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.adaptive.navigation3)
    debugImplementation(libs.compose.ui.tooling)

    // Estado e navegação
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.viewmodel.navigation3)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)

    // Dados locais
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)

    // Rede e imagens
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor3)

    // Testes (./gradlew :compose:testDebugUnitTest)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
