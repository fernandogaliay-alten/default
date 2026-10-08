package com.sazon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sazon.app.data.AssetSeedRecipeSource
import com.sazon.app.data.AssetTaxonomySource
import com.sazon.app.data.generator.FakeRecipeGenerator
import com.sazon.app.data.profile.ProfileDatabase
import com.sazon.app.data.profile.RoomProfileRepository
import com.sazon.app.data.suggestions.RoomSuggestionBatchRepository
import com.sazon.app.data.suggestions.SuggestionBatchDatabase
import com.sazon.app.domain.suggestions.SuggestionBatchService
import com.sazon.app.ui.SazonApp
import com.sazon.app.ui.theme.SazonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Composicion de dependencias a mano (sin DI): bases Room + repositorios + Taxonomia +
        // catalogo semilla + servicio cache-first del Lote -> raiz de la app.
        val profileDatabase = ProfileDatabase.build(applicationContext)
        val profileRepository = RoomProfileRepository(profileDatabase.profileDao())
        val taxonomy = AssetTaxonomySource(applicationContext).load()
        val seedRecipes = AssetSeedRecipeSource(applicationContext).load().recipes
        val suggestionBatchDatabase = SuggestionBatchDatabase.build(applicationContext)
        val suggestionBatchRepository =
            RoomSuggestionBatchRepository(suggestionBatchDatabase.suggestionBatchDao())
        val suggestionBatchService = SuggestionBatchService(
            generator = FakeRecipeGenerator(),
            repository = suggestionBatchRepository,
            taxonomy = taxonomy,
            seedRecipes = seedRecipes,
        )

        setContent {
            SazonTheme {
                SazonApp(
                    profileRepository = profileRepository,
                    taxonomy = taxonomy,
                    suggestionBatchRepository = suggestionBatchRepository,
                    suggestionBatchService = suggestionBatchService,
                    seedRecipes = seedRecipes,
                )
            }
        }
    }
}
