package com.sazon.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asImageBitmap
import com.sazon.app.data.AssetRecipePhotoSource
import com.sazon.app.data.AssetSeedRecipeSource
import com.sazon.app.data.AssetTaxonomySource
import com.sazon.app.data.NetworkStatusSource
import com.sazon.app.data.activity.ActivitySummaryDatabase
import com.sazon.app.data.activity.RoomActivitySummaryRepository
import com.sazon.app.data.ai.AiHttpClientFactory
import com.sazon.app.data.ai.AiVerboseDiagnostics
import com.sazon.app.data.ai.DevExpertRecipeGenerator
import com.sazon.app.data.ai.image.AssetDishImageStyleReference
import com.sazon.app.data.ai.image.DevExpertDishImageGenerator
import com.sazon.app.data.favorites.FavoritesDatabase
import com.sazon.app.data.favorites.RoomFavoriteRepository
import com.sazon.app.data.image.AndroidDishImageCache
import com.sazon.app.data.image.DishImageResolver
import com.sazon.app.data.image.DishImageSettings
import com.sazon.app.data.image.FileDishImageCache
import com.sazon.app.data.image.GatedDishImageGenerator
import com.sazon.app.data.image.MemoryDishImageCache
import com.sazon.app.data.profile.ProfileDatabase
import com.sazon.app.data.profile.RoomProfileRepository
import com.sazon.app.data.suggestions.RoomSuggestionBatchRepository
import com.sazon.app.data.suggestions.SuggestionBatchDatabase
import com.sazon.app.domain.activity.ActivitySummaryService
import com.sazon.app.domain.favorites.FavoriteService
import com.sazon.app.domain.generation.GenerationService
import com.sazon.app.domain.image.DishImageRequest
import com.sazon.app.domain.search.AiSearchService
import com.sazon.app.domain.suggestions.SuggestionBatchService
import com.sazon.app.ui.SazonApp
import com.sazon.app.ui.artwork.AndroidRecipeImageDecoder
import com.sazon.app.ui.artwork.DishImageArtworkLoader
import com.sazon.app.ui.artwork.GeneratedRecipeArtworkSource
import com.sazon.app.ui.artwork.LocalGeneratedRecipeArtworkSource
import com.sazon.app.ui.artwork.LocalRecipePhotoSource
import com.sazon.app.ui.artwork.RecipePhotoSource
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
        // Fotos de plato empaquetadas como assets offline; se inyectan en el arbol de composicion por
        // el seam `LocalRecipePhotoSource` (composicion explicita, sin DI, como el resto).
        val recipePhotoSource = AssetRecipePhotoSource(applicationContext)
        // Imagen de plato generada (fases 2-4): reutiliza el `aiClient` compartido (los timeouts de
        // imagen se aplican por peticion, ADR 0005; no se crea un segundo motor HTTP) y la cache de la
        // fase 3. Se inyecta por `LocalGeneratedRecipeArtworkSource` como un nivel por delante de la
        // cadena local; sin red ni I/O en el arranque (la cache solo se toca al resolver).
        // El ajuste de sesion (interruptor + tope por sesion) se gobierna en DOS puntos: el decorador
        // impone el tope en el borde de generacion (donde se gasta cuota) y el proveedor del seam
        // gatea el interruptor (apagado = no se pide nada: ni cache ni generacion). Uno por proceso =
        // una sesion; no se persiste.
        val dishImageSettings = DishImageSettings()
        // Referencia de estilo (fase 6): el asset neutro y versionado de `recipe-photos`
        // (`img/recipes/default.<ext>`) viaja en el APK; no hay subida del usuario. Best-effort: `null`
        // (asset ausente o ilegible) deja la app exactamente en el comportamiento de la fase 2 (una
        // peticion por texto por imagen), sin error ni UI.
        val dishImageStyleReference = AssetDishImageStyleReference(applicationContext).load()
        val dishImageResolver = DishImageResolver(
            disk = FileDishImageCache(AndroidDishImageCache.root(applicationContext)),
            memory = MemoryDishImageCache(),
            generator = GatedDishImageGenerator(
                delegate = DevExpertDishImageGenerator(
                    apiKey = BuildConfig.DEVEXPERT_API_KEY,
                    client = aiClient,
                    styleReference = dishImageStyleReference,
                ),
                settings = dishImageSettings,
            ),
        )
        val dishImageLoader = DishImageArtworkLoader(
            requestFor = { recipe -> DishImageRequest.of(taxonomy, recipe) },
            resolve = dishImageResolver::imageFor,
            decode = AndroidRecipeImageDecoder::decode,
        )

        setContent {
            SazonTheme {
                CompositionLocalProvider(
                    LocalRecipePhotoSource provides RecipePhotoSource { recipe ->
                        recipePhotoSource.bitmapFor(recipe)?.asImageBitmap()
                    },
                    LocalGeneratedRecipeArtworkSource provides GeneratedRecipeArtworkSource { recipe ->
                        // Interruptor apagado = no se pide nada (ni cache ni generacion): solo la foto
                        // local. El tope lo impone el decorador en el borde de generacion.
                        dishImageSettings.generatedIfEnabled { dishImageLoader.imageFor(recipe) }
                    },
                ) {
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
                        dishImageSettings = dishImageSettings,
                        seedRecipes = seedRecipes,
                    )
                }
            }
        }
    }
}
