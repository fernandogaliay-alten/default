package com.sazon.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asImageBitmap
import com.sazon.app.data.AssetDishImageSeedSource
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
import com.sazon.app.data.image.DishImageOrigin
import com.sazon.app.data.image.DishImageOriginSink
import com.sazon.app.data.image.DishImageResolver
import com.sazon.app.data.image.DishImageSettings
import com.sazon.app.data.image.DishVisualDescriptorStore
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
import com.sazon.app.domain.recipe.Recipe
import com.sazon.app.domain.search.AiSearchService
import com.sazon.app.domain.suggestions.SuggestionBatchService
import com.sazon.app.ui.SazonApp
import com.sazon.app.ui.artwork.AndroidRecipeImageDecoder
import com.sazon.app.ui.artwork.DishImageArtworkLoader
import com.sazon.app.ui.artwork.DishImageOriginDiagnostics
import com.sazon.app.ui.artwork.GeneratedRecipeArtworkSource
import com.sazon.app.ui.artwork.LocalDishImageOriginDiagnostics
import com.sazon.app.ui.artwork.LocalGeneratedRecipeArtworkSource
import com.sazon.app.ui.artwork.LocalRecipePhotoSource
import com.sazon.app.ui.artwork.NoDishImageOriginDiagnostics
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
        // Almacen de sesion del descriptor visual (fase 5): el generador lo alimenta con el mapa del
        // decode y el `requestFor` de la imagen lo consulta por `id`. En memoria (se vacia al recrear
        // la Activity, degradando a fase 1) y compartido por Lote, Generador y Busqueda, porque los
        // tres usan el mismo generador.
        val dishVisualDescriptors = DishVisualDescriptorStore()
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
            // Costura del descriptor visual (fase 5): cada decode exitoso registra por `id` el
            // descriptor que el modelo emitio en la misma llamada, de modo que el `requestFor` de la
            // imagen pueda compartir clave por `dishKey` sin cambiar el puerto `RecipeGenerator`.
            onDescriptors = dishVisualDescriptors::record,
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
        // Sumidero de sesion del **origen resuelto** de la imagen (solo diagnostico): el resolutor lo
        // alimenta al servir cada imagen y el diagnostico de `ui.artwork` lo consulta al pintar. En
        // memoria, efimero por sesion y sin persistencia.
        val dishImageOrigins = DishImageOriginSink()
        // Set semilla (fase 7): los platos frecuentes del demo viajan preparados en el APK y se
        // resuelven por la clave canonica del plato; ganan al generador (0 cuota, 0 red, 0 espera) y
        // quedan detras del mismo gate del interruptor. Best-effort: un asset ausente degrada a la
        // cadena de la fase 3 sin error ni UI.
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
            seed = AssetDishImageSeedSource(applicationContext),
            // Costura aditiva de diagnostico: registra el origen (SEED/GENERATED) que resolvio cada
            // receta, para que el log de debug pueda distinguir una generacion real de un acierto del
            // set semilla. No cambia bytes ni la cadena.
            onResolved = { request, origin -> dishImageOrigins.record(request.recipeId, origin) },
        )
        val dishImageLoader = DishImageArtworkLoader(
            // Con descriptor registrado, la clave/prompt salen del `dishKey`/`imagePrompt` del modelo
            // (dos variantes del mismo plato comparten imagen); sin descriptor, la composicion es
            // exactamente la de la fase 1 (respaldo por titulo).
            requestFor = { recipe ->
                DishImageRequest.of(taxonomy, recipe, dishVisualDescriptors.descriptorFor(recipe.id))
            },
            resolve = dishImageResolver::imageFor,
            decode = AndroidRecipeImageDecoder::decode,
        )
        // Diagnostico del origen de la imagen: **solo** en depuracion. En release queda el default
        // no-op y no se emite ninguna linea. No interviene en el pintado, la cache, el tope ni la red.
        val dishImageDiagnostics: DishImageOriginDiagnostics =
            if (BuildConfig.DEBUG) {
                object : DishImageOriginDiagnostics {
                    override fun resolvedOriginFor(recipe: Recipe): DishImageOrigin? =
                        dishImageOrigins.originFor(recipe.id)

                    override fun onPainted(recipe: Recipe, origin: DishImageOrigin) {
                        Log.d("SazonDishImage", "origin=$origin id=${recipe.id} title=${recipe.title}")
                    }
                }
            } else {
                NoDishImageOriginDiagnostics
            }

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
                    // Solo con `BuildConfig.DEBUG` hay diagnostico real; en release, no-op.
                    LocalDishImageOriginDiagnostics provides dishImageDiagnostics,
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
