plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val taxonomyAssetFile = layout.projectDirectory.file("src/main/assets/taxonomy.json")
val seedRecipesAssetFile = layout.projectDirectory.file("src/main/assets/seed-recipes.json")
val designTokensFile = rootProject.layout.projectDirectory.file("DESIGN.md")

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
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
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
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
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
}
