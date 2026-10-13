import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val taxonomyAssetFile = layout.projectDirectory.file("src/main/assets/taxonomy.json")
val seedRecipesAssetFile = layout.projectDirectory.file("src/main/assets/seed-recipes.json")
val designTokensFile = rootProject.layout.projectDirectory.file("DESIGN.md")
// Directorio de las fotos de plato empaquetadas (assets offline, fuente versionada); los tests JVM
// comprueban que cada receta semilla, cada categoria y el caso por defecto resuelven un fichero real.
val recipePhotosDir = layout.projectDirectory.dir("src/main/assets/img/recipes")
// Directorio del set semilla de imagenes de plato (fase 7, assets versionados); el test JVM deriva la
// clave canonica de cada plato de la lista (`DishImageCacheKey.of(titulo)`) y comprueba que resuelve un
// fichero real, no vacio y con la extension esperada.
val dishImageSeedDir = layout.projectDirectory.dir("src/main/assets/img/dishes/seed")

// Clave del proveedor de IA leida en tiempo de configuracion desde local.properties (que NO se
// versiona). Nunca se imprime su valor ni la ruta; si falta el fichero o la propiedad, queda "".
val localProperties = Properties().apply {
    rootProject.layout.projectDirectory.file("local.properties")
        .asFile.takeIf { it.exists() }
        ?.inputStream()?.use { load(it) }
}
val devexpertApiKey: String = localProperties.getProperty("devexpert.apiKey", "")

val syncTaxonomy by tasks.registering(Copy::class) {
    description = "Copia la Taxonomia canonica al directorio de assets de la aplicacion."
    from(rootProject.layout.projectDirectory.file("docs/domain/taxonomy.json"))
    into(layout.projectDirectory.dir("src/main/assets"))
}

android {
    namespace = "com.sazon.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.sazon.app"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Clave del proveedor de IA inyectada en BuildConfig; valor vacio si falta local.properties.
        buildConfigField(
            "String",
            "DEVEXPERT_API_KEY",
            "\"${devexpertApiKey.replace("\\", "\\\\").replace("\"", "\\\"")}\"",
        )
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// Ktor 3.6.0 (engine OkHttp) arrastra OkHttp 5.5.0, cuyo artefacto Android (`okhttp-android`)
// declara `minCompileSdk=37` y rompe la verificacion del AAR con el `compileSdk` del repo
// (API 36.1). Se fija OkHttp a 5.4.0, la ultima version cuyo artefacto Android acepta
// `compileSdk 36`: es la minima desviacion posible y no cambia ni el engine ni el compileSdk.
configurations.configureEach {
    resolutionStrategy {
        force("com.squareup.okhttp3:okhttp:${libs.versions.okhttp.get()}")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    dependsOn(syncTaxonomy)
}

tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    dependsOn(syncTaxonomy)
    systemProperty("taxonomy.asset.path", taxonomyAssetFile.asFile.absolutePath)
    systemProperty("seed.recipes.asset.path", seedRecipesAssetFile.asFile.absolutePath)
    systemProperty("design.tokens.path", designTokensFile.asFile.absolutePath)
    systemProperty("recipe.photos.assets.dir", recipePhotosDir.asFile.absolutePath)
    systemProperty("dish.image.seed.assets.dir", dishImageSeedDir.asFile.absolutePath)
}
