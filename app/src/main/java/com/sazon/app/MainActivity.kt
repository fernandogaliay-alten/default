package com.sazon.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sazon.app.data.AssetSeedRecipeSource
import com.sazon.app.data.AssetTaxonomySource
import com.sazon.app.data.NetworkStatusSource
import com.sazon.app.data.activity.ActivitySummaryDatabase
import com.sazon.app.data.activity.RoomActivitySummaryRepository
import com.sazon.app.data.ai.AiHttpClientFactory
import com.sazon.app.data.ai.AiVerboseDiagnostics
import com.sazon.app.data.ai.DevExpertRecipeGenerator
import com.sazon.app.data.favorites.FavoritesDatabase
import com.sazon.app.data.favorites.RoomFavoriteRepository
import com.sazon.app.data.profile.ProfileDatabase
import com.sazon.app.data.profile.RoomProfileRepository
import com.sazon.app.data.suggestions.RoomSuggestionBatchRepository
import com.sazon.app.data.suggestions.SuggestionBatchDatabase
import com.sazon.app.domain.activity.ActivitySummaryService
import com.sazon.app.domain.favorites.FavoriteService
import com.sazon.app.domain.generation.GenerationService
import com.sazon.app.domain.search.AiSearchService
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
        val activitySummaryDatabase = ActivitySummaryDatabase.build(applicationContext)
        val activitySummaryRepository =
            RoomActivitySummaryRepository(activitySummaryDatabase.activitySummaryDao())
        val activitySummaryService = ActivitySummaryService(activitySummaryRepository)
        // Proveedor de IA real (DevExpert Inference, Ktor) tras el puerto RecipeGenerator. La clave
        // se inyecta en BuildConfig desde local.properties (no versionada); se crea tras cargar la
        // Taxonomia y lo comparten los tres servicios, igual que antes con el generador falso.
        val aiClient = AiHttpClientFactory.create()
        val generator = DevExpertRecipeGenerator(
            apiKey = BuildConfig.DEVEXPERT_API_KEY,
            taxonomy = taxonomy,
            client = aiClient,
            // Diagnostico verboso (detalle de descartes + JSON crudo) solo en debug; en release queda
            // apagado. El desglose `incoherent=` del resumen no depende de este interruptor.
            verboseDiagnostics = BuildConfig.DEBUG,
            onEvent = { event ->
                // Diagnostico local: nunca incluye la clave ni la cabecera Authorization. En el camino
                // por defecto solo longitudes del contenido, recuentos de recetas y el resumen de
                // motivos; en modo depuracion el evento trae ademas el detalle y el crudo, y el
                // render de abajo los emite troceados.
                val causes =
                    if (event.incoherentByCause.isEmpty()) "" else "incoherent=${event.incoherentByCause} "
                Log.d(
                    "SazonAi",
                    "${event.operation} ${event.outcome} attempt=${event.attempt} " +
                        "status=${event.httpStatus} ${event.durationMillis}ms " +
                        "format=${event.responseFormat} " +
                        "content=${event.contentChars}c/${event.contentBytes}b " +
                        "received=${event.received} accepted=${event.accepted} " +
                        "discards=${event.discardedByReason} " + causes + "${event.detail}",
                )
                // Una linea por descarte y el crudo troceado; solo si el evento los trae (el modo lo
                // decide el generador, no el log).
                AiVerboseDiagnostics.discardLines(event.discards).forEach { Log.d("SazonAi", it) }
                AiVerboseDiagnostics.rawLines(event.rawContent).forEach { Log.d("SazonAi", it) }
            },
        )
        val suggestionBatchService = SuggestionBatchService(
            generator = generator,
            repository = suggestionBatchRepository,
            taxonomy = taxonomy,
            seedRecipes = seedRecipes,
            // Costura aditiva: cada receta ai aceptada en un refresco del Lote suma al Resumen.
            onRecipesGenerated = { recipes -> activitySummaryService.recordAcceptedRecipes(recipes) },
        )
        val generationService = GenerationService(
            generator = generator,
            taxonomy = taxonomy,
            // Costura aditiva: cada receta ai aceptada por el Generador suma al Resumen.
            onRecipesGenerated = { recipes -> activitySummaryService.recordAcceptedRecipes(recipes) },
        )
        val aiSearchService = AiSearchService(
            generator = generator,
            taxonomy = taxonomy,
            // Costura aditiva: cada receta ai aceptada por la Busqueda con IA suma al Resumen.
            onRecipesGenerated = { recipes -> activitySummaryService.recordAcceptedRecipes(recipes) },
        )
        // Unica dependencia externa de la Home: la conectividad del bloque 2 (Busqueda con IA).
        val networkStatusSource = NetworkStatusSource(applicationContext)
        val favoritesDatabase = FavoritesDatabase.build(applicationContext)
        val favoriteRepository = RoomFavoriteRepository(favoritesDatabase.favoriteDao())
        val favoriteService = FavoriteService(favoriteRepository)

        setContent {
            SazonTheme {
                SazonApp(
                    profileRepository = profileRepository,
                    taxonomy = taxonomy,
                    suggestionBatchRepository = suggestionBatchRepository,
                    suggestionBatchService = suggestionBatchService,
                    generationService = generationService,
                    favoriteRepository = favoriteRepository,
                    favoriteService = favoriteService,
                    activitySummaryRepository = activitySummaryRepository,
                    aiSearchService = aiSearchService,
                    networkStatusSource = networkStatusSource,
                    seedRecipes = seedRecipes,
                )
            }
        }
    }
}
