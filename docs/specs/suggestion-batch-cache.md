# Feature Implementation Spec: Lote de sugerencias con política cache-first

## Source Feature

- `id`: `suggestion-batch-cache`
- `area`: `data`
- `depends_on`: `["preference-signature", "fake-recipe-generator", "seed-recipe-catalog"]`
  (los tres `accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`; el cambio de estado lo hacen
  implementer/validador/orquestador.
- `source`: `feature_list.json`

## Goal

Hacer que el **Lote de sugerencias** (`SuggestionBatch`) sea una **caché persistida y cache-first**:
al abrir la app el lote se lee de disco y solo se pide al Generador cuando de verdad hay que
refrescarlo —porque **cambió la firma de preferencias**, porque **caducó** (7 días) o porque el
usuario **lo pide**—. Sin IA disponible el lote **cae a solo semilla** y sigue siendo utilizable
offline. La decisión de frescura es **lógica pura** testeable en JVM; la persistencia sigue el patrón
Room ya establecido para el Perfil.

Esta feature entrega el **motor** (política + servicio + repositorio + persistencia) que
`home-offline-blocks` consumirá para poblar el carrusel de sugeridas sin conexión. No añade UI, no
cambia la Home y no conecta el proveedor real de IA.

## Non-Goals

- **No** hay UI, navegación ni render de la Home: el carrusel de sugeridas es `home-offline-blocks`.
- **No** se implementa la acción "Actualizar sugerencias" del Perfil (UI): `manual-suggestions-refresh`
  la expone; aquí solo se ofrece la costura `force` del servicio.
- **No** se conecta el proveedor real de IA: `real-ai-provider`. En desarrollo se usa
  `FakeRecipeGenerator` (ya `accepted`); el servicio depende del puerto `RecipeGenerator`.
- **No** se cachean los resultados de `searchByText` (Búsqueda con IA): su política está en
  `docs/domain-model.md` y la implementa `ai-search`.
- **No** se implementan Favoritos (`favorites`), "Abiertas hace poco" ni el contador del Resumen de
  actividad (`activity-summary`).
- **No** se calcula ni se reimplementa la Firma: se **consume** `PreferenceSignature.of(...)`. El lote
  guarda la firma como parte de sus metadatos (es su clave de caché), no en un almacén aparte.
- **No** se toca `ProfileDatabase`, `data.profile/**` ni `domain.profile/**`.
- **No** se añaden dependencias de build: Room, KSP, corrutinas y `kotlinx.serialization` ya están.
- **No** se introduce una migración de la base de datos del Perfil (ver Decisión "Base de datos
  propia").
- **No** se persiste un estado `fresh`/`stale` como columna: la frescura es **derivada** por la
  política.

## Job Story

Cuando abro la app con mi Perfil ya creado,
quiero ver sugerencias al instante desde la caché local y que la app solo gaste una llamada de IA
cuando mis preferencias han cambiado, el lote ha caducado o yo pido refrescarlo,
para tener siempre algo que cocinar sin conexión, sin llamadas repetidas y sin sorpresas de coste.

## Users And Permissions

- **Usuario local**: no interactúa con esta feature directamente (no hay UI); se beneficia de que la
  Home funcione offline y de que la cuota de IA no se malgaste.
- **Desarrollador / agente**: ejecuta y verifica la política y el servicio como lógica pura en JVM,
  sin red, sin emulador y sin permisos.
- **Sistema**: intenta generar solo cuando la política lo decide; ante fallo del generador degrada a
  modo semilla (modo esperado, no error).

## Acceptance Scenarios

### Scenario 1: mismo perfil y lote fresco no generan llamada (verificación exigida)

Given un lote cacheado con firma `S` y `generatedAt` dentro del TTL de 7 días,
When se asegura el lote con un Perfil cuya firma es `S`,
Then la decisión es **usar la caché**, el `RecipeGenerator` **no** se invoca y se devuelve el lote
cacheado sin reescribirlo.

### Scenario 2: firma distinta marca el lote como obsoleto (verificación exigida)

Given un lote cacheado con firma `S1`,
When se asegura el lote con un Perfil cuya firma es `S2` (`S1 != S2`),
Then la decisión es **generar** con motivo `SIGNATURE_CHANGED`, se invoca al `RecipeGenerator` y se
persiste/actualiza el lote con la firma nueva.

### Scenario 3: caducidad marca el lote como obsoleto (verificación exigida)

Given un lote cacheado con la firma actual pero `generatedAt` con **7 días o más** de antigüedad,
When se asegura el lote,
Then la decisión es **generar** con motivo `EXPIRED`.

### Scenario 4: frontera del TTL

Given un lote cacheado con la firma actual,
When `now - generatedAt` es exactamente `BATCH_TTL_MILLIS` o más, la decisión es `EXPIRED`;
When es un milisegundo menos, la decisión es **usar la caché**.

### Scenario 5: sin IA disponible el lote cae a solo semilla y sigue siendo utilizable (verificación exigida)

Given que hay que generar (lote inexistente, obsoleto o forzado) y que el `RecipeGenerator` **falla**
(lanza una excepción, p. ej. sin clave o sin red),
When se asegura el lote,
Then el servicio devuelve un lote **formado solo por recetas `seed`**, no vacío, y lo persiste; la
lectura posterior devuelve ese lote seed-only utilizable offline.

### Scenario 6: petición explícita del usuario refresca aunque esté fresco

Given un lote cacheado con la firma actual y dentro del TTL,
When se asegura el lote con `force = true`,
Then la decisión es **generar** con motivo `FORCED`.

### Scenario 7: lote inexistente se genera y se persiste

Given que no hay lote en disco,
When se asegura el lote,
Then la decisión es **generar** con motivo `MISSING` y, si el generador responde, el lote resultante
contiene recetas `seed` **y** recetas `ai` y queda persistido con `generatedAt = now`.

### Scenario 8: las recetas `ai` inseguras se descartan (caso negativo de seguridad)

Given que hay que generar y que el `RecipeGenerator` devuelve una receta `ai` que incumple una
Restricción dura o el Patrón de dieta del Perfil,
When el servicio compone el lote,
Then esa receta **no** entra en el lote (se descarta vía `RecipeValidator`), mientras que las recetas
`ai` válidas **sí** entran junto a todas las `seed`.

### Scenario 9: duplicados seed/ai se deduplican

Given recetas `seed` y una receta `ai` con el mismo título (normalizado: sin mayúsculas ni acentos),
When se compone el lote,
Then el lote conserva **una sola** receta para ese título (gana la `seed`).

### Scenario 10: ida y vuelta de persistencia

Given un `SuggestionBatch` con recetas `seed` y `ai` (con `nutrition`/`ingredientId` opcionales),
When se guarda y se relee con `SuggestionBatchRepository`,
Then el lote recuperado es **igual** al original, la tabla es de **una sola fila** (`COUNT = 1`) al
sobrescribir, `clear()` deja la lectura en `null` y `observe()` emite el cambio.

## Repository Research

### Files Inspected

- `feature_list.json` — definición, `depends_on`, verificación y nota ("caducidad del lote de 7 días,
  en un único sitio configurable") de `suggestion-batch-cache`; features vecinas
  (`home-offline-blocks`, `manual-suggestions-refresh`, `favorites`, `activity-summary`).
- `PROGRESS.md` — estado: última feature aceptada `onboarding-ingredients-safety-kitchen`; siguiente
  feature lista `suggestion-batch-cache`; camino de verificación `init.sh` (`./gradlew test` + `lint`).
- `CONTEXT.md` — glosario "Lote de sugerencias" (se guarda con la firma y una fecha),
  "Firma de preferencias", "Sugerencia", "Receta semilla".
- `docs/domain-model.md` — `SuggestionBatch` (`preferenceSignature`, `generatedAt`, `status`,
  `recipeIds[]`), ciclo `inexistente → … → fresh → stale → refresco`, "si la IA no está disponible el
  lote cae a solo semilla", Scenario 4 (cambio de preferencias), 5 (apertura sin conexión), 7 (cambio
  de identidad no invalida) y edge case de duplicados por título normalizado.
- `docs/technical-discovery.md` — "Lote de sugerencias y recetas `ai` | Room | Evita llamadas
  repetidas a la IA y da soporte offline"; "política cache-first"; dominio testeable en JVM sin Android.
- `docs/build-brief.md` — política cache-first (firma cambiada, caché caducada o petición explícita);
  Success Criteria "la app arranca y es usable sin proveedor de IA (modo semilla)".
- `docs/risks-and-open-questions.md` — "Caducidad del Lote de sugerencias (antes pregunta 3): **7
  días**, en un único sitio configurable"; pregunta abierta 1 (errores/reintentos de la IA).
- `docs/adr/0002-local-first-no-backend.md` — todo el estado en el dispositivo (Room); degradación
  elegante a modo semilla.
- `docs/adr/0003-ai-provider.md` — proveedor real; la app no depende de structured outputs.
- `app/src/main/java/com/sazon/app/domain/profile/{UserProfile,PreferenceSignature,ProfileRepository}.kt`
  — `PreferenceProfile`, `PreferenceSignature.of(preferences)` y el patrón del puerto de persistencia.
- `app/src/main/java/com/sazon/app/domain/generator/{RecipeGenerator,GenerationConstraints}.kt` — el
  puerto (`suggestByPreferences`) devuelve `List<Recipe>` sin envoltorio de error.
- `app/src/main/java/com/sazon/app/data/generator/FakeRecipeGenerator.kt` — implementación por defecto
  en desarrollo, determinista y sin red; incluye recetas que el Validador debe descartar.
- `app/src/main/java/com/sazon/app/domain/recipe/{Recipe,RecipeJson,SeedRecipeCatalog}.kt` — esquema
  serializable, `RecipeJson` (objeto de serialización) y el catálogo semilla.
- `app/src/main/java/com/sazon/app/data/AssetSeedRecipeSource.kt` y `AssetTaxonomySource.kt` — patrón
  de lectura de assets.
- `app/src/main/java/com/sazon/app/domain/validation/RecipeValidator.kt` — `validateRecipe(recipe,
  taxonomy, RecipeConstraints)` devuelve `Accepted`/`Discarded`; lógica pura ya `accepted`.
- `app/src/main/java/com/sazon/app/domain/taxonomy/Taxonomy.kt` — resolución de ids por colección.
- `app/src/main/java/com/sazon/app/data/profile/{ProfileDatabase,ProfileDao,ProfileEntity,
  ProfileMappers,RoomProfileRepository,ListStringConverter}.kt` — patrón Room completo (entidad,
  DAO, mapeo, repositorio, base de una fila).
- `app/src/main/java/com/sazon/app/{MainActivity.kt,ui/SazonApp.kt,ui/AppGate.kt,ui/HomePlaceholder.kt}`
  — composición de dependencias actual (el Perfil **no** se consume aún en MAIN; la Home es
  placeholder).
- `app/src/test/java/com/sazon/app/data/profile/RoomProfileRepositoryTest.kt` — patrón de test Room en
  JVM con Robolectric `@Config(sdk = [33])` y base en memoria.
- `app/src/test/java/com/sazon/app/domain/taxonomy/TaxonomyIntegrityTest.kt` y
  `domain/recipe/SeedRecipeCatalogTest.kt` — patrón de test que lee assets por propiedad de sistema.
- `app/build.gradle.kts` — propiedades de sistema de test ya disponibles (`taxonomy.asset.path`,
  `seed.recipes.asset.path`, `design.tokens.path`); `seed-recipes.json` en `src/main/assets/`.
- `gradle/libs.versions.toml` — Room 2.8.5, KSP, corrutinas y `kotlinx.serialization` ya presentes.

### Existing Patterns To Follow

- **Costura dominio / datos**: lógica pura en `com.sazon.app.domain.<área>` (sin `android.*`),
  persistencia en `com.sazon.app.data.<área>` sobre Room.
- **Base de datos de una fila** con `@PrimaryKey` `id = 1`, `INSERT OR REPLACE`, `Flow` para observar
  y `DELETE` para `clear` (patrón exacto de `ProfileDao`/`ProfileEntity`).
- **Mapeo entidad ↔ dominio** en un fichero `*Mappers.kt`; listas como **columna JSON** con
  `kotlinx.serialization` (patrón `ListStringConverter`, aquí con `List<Recipe>`).
- **Inyección de dependencias manual** desde la raíz de composición (sin Hilt/Koin).
- **Tests JUnit 4** en `app/src/test/...`, nombres en español con `_`, helpers `private fun`; Room con
  Robolectric y base en memoria; lógica pura sin Robolectric.
- **Reloj inyectable** para determinismo (ver `OnboardingProfileBuilder.build(..., now)`).
- Verificación única: `./gradlew test` + `./gradlew lint` vía `init.sh`; sin procesos largos.

### Current Gaps

- No existe ningún modelo, política, puerto, servicio ni persistencia del Lote de sugerencias.
- La Firma y el catálogo semilla existen pero **no están conectados** a nada.
- No hay ninguna base de datos ni entidad para el lote (solo `ProfileDatabase` con `user_profile`).
- No hay una costura que lea el catálogo semilla como `List<Recipe>` reutilizable por el servicio.

## Technical Approach

### 1. Dominio puro — `com.sazon.app.domain.suggestions`

Modelo e intención (sin Android, sin Room, sin corrutinas salvo `suspend` del servicio):

```
data class SuggestionBatch(
    val preferenceSignature: String,
    val generatedAt: Long,
    val recipes: List<Recipe>,
) {
    val recipeIds: List<String> get() = recipes.map { it.id }
}

enum class BatchRefreshReason { MISSING, SIGNATURE_CHANGED, EXPIRED, FORCED }

sealed interface BatchDecision {
    data object UseCached : BatchDecision
    data class Generate(val reason: BatchRefreshReason) : BatchDecision
}

interface SuggestionBatchRepository {
    suspend fun get(): SuggestionBatch?
    fun observe(): Flow<SuggestionBatch?>
    suspend fun save(batch: SuggestionBatch)
    suspend fun clear()
}
```

Política de frescura — **un único sitio configurable** para el TTL:

```
object SuggestionBatchPolicy {
    /** Caducidad del Lote en un único sitio (docs/risks-and-open-questions.md). */
    const val BATCH_TTL_MILLIS: Long = 7L * 24 * 60 * 60 * 1_000

    fun decide(
        cached: SuggestionBatch?,
        currentSignature: String,
        now: Long,
        force: Boolean = false,
    ): BatchDecision {
        if (cached == null) return BatchDecision.Generate(MISSING)
        if (force) return BatchDecision.Generate(FORCED)
        if (cached.preferenceSignature != currentSignature) {
            return BatchDecision.Generate(SIGNATURE_CHANGED)
        }
        val elapsed = now - cached.generatedAt
        if (elapsed >= BATCH_TTL_MILLIS) return BatchDecision.Generate(EXPIRED)
        return BatchDecision.UseCached
    }
}
```

`decide` es determinista y puro: recibe `cached`, la firma actual, `now` y `force`; no lee reloj ni
disco. La frescura (`fresh`/`stale` de `docs/domain-model.md`) **se deriva** de esta decisión y no se
persiste.

### 2. Servicio de dominio — `SuggestionBatchService`

Orquesta la política y el generador (puro y testeable en JVM, `suspend`, sin Android):

```
class SuggestionBatchService(
    private val generator: RecipeGenerator,
    private val repository: SuggestionBatchRepository,
    private val taxonomy: Taxonomy,
    private val seedRecipes: List<Recipe>,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun ensureBatch(
        profile: PreferenceProfile,
        force: Boolean = false,
    ): SuggestionBatch
}
```

Algoritmo de `ensureBatch`:

1. `signature = PreferenceSignature.of(profile)` (se **consume**, no se reimplementa).
2. `cached = repository.get()`.
3. Según `SuggestionBatchPolicy.decide(cached, signature, now(), force)`:
   - `UseCached` → devolver `cached` **sin** reescribir ni llamar al generador.
   - `Generate` → intentar generar (paso 4).
4. **Generación (best-effort)**:
   - Firmar las restricciones con `RecipeConstraints(profile.dietPatternId,
     profile.hardRestrictionIds.toSet())` y llamar a `generator.suggestByPreferences(profile)`.
   - Filtrar cada receta `ai` con `RecipeValidator.validateRecipe(...)`, conservando solo las
     `Accepted`. Las recetas `seed` no se validan en ejecución (contenido autorado y ya verificado por
     `seed-recipe-catalog`).
   - Componer `recipes = deduplicateByTitle(seedRecipes + safeAiRecipes)` (seed primero).
   - Persistir y devolver `SuggestionBatch(signature, now(), recipes)`.
   - Si el generador **falla**: no propagar; construir `SuggestionBatch(signature, now(),
     deduplicateByTitle(seedRecipes))`, persistirlo y devolverlo (modo semilla).
   - Manejo de error: capturar `Exception` (el puerto aún no tiene modelo de errores; pregunta abierta
     1) **re-lanzando `kotlinx.coroutines.CancellationException`** para no romper la cancelación. No
     usar `runCatching` a secas.

`deduplicateByTitle` es un helper puro: normaliza el `title` (minúsculas, sin acentos con
`java.text.Normalizer`, recortado) y conserva la **primera** aparición (seed antes que ai).

`ensureBatch` no pinta ni navega: devuelve el lote y, de paso, deja la caché lista para que la Home la
observe con `repository.observe()`.

### 3. Persistencia Room — `com.sazon.app.data.suggestions`

Base de datos **propia** (ver Decisión), de una sola fila, con las recetas serializadas como JSON:

```
@Entity(tableName = "suggestion_batch")
data class SuggestionBatchEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val preferenceSignature: String,
    val generatedAt: Long,
    val recipesJson: String,
) { companion object { const val SINGLETON_ID = 1 } }

@Dao interface SuggestionBatchDao {
    @Query("SELECT * FROM suggestion_batch WHERE id = 1") suspend fun get(): SuggestionBatchEntity?
    @Query("SELECT * FROM suggestion_batch WHERE id = 1") fun observe(): Flow<SuggestionBatchEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(entity: SuggestionBatchEntity)
    @Query("DELETE FROM suggestion_batch") suspend fun deleteAll()
}

@Database(entities = [SuggestionBatchEntity::class], version = 1, exportSchema = false)
abstract class SuggestionBatchDatabase : RoomDatabase() {
    abstract fun suggestionBatchDao(): SuggestionBatchDao
    companion object {
        fun build(context: Context): SuggestionBatchDatabase =
            Room.databaseBuilder(context, SuggestionBatchDatabase::class.java, "suggestion_batch.db")
                .build()
    }
}
```

`RoomSuggestionBatchRepository(dao)` implementa el puerto y delega `get`/`observe`/`save`/`clear`.
`SuggestionBatchMappers.kt` convierte entidad ↔ dominio y (de)serializa `List<Recipe>`.

Serialización de la lista: añadir a `RecipeJson` dos funciones **aditivas**
(`encodeList(recipes)` / `parseList(source)` sobre `ListSerializer(Recipe.serializer())`) para no
duplicar la configuración de `Json` (`ignoreUnknownKeys`, `encodeDefaults`) que ya vive ahí. No cambia
el comportamiento de `encode`/`parse` existentes.

### 4. Alcance de integración

Esta feature es de **datos**: no modifica `MainActivity`, `SazonApp` ni la Home. La composición de
`SuggestionBatchDatabase` + `RoomSuggestionBatchRepository` + `SuggestionBatchService` en la raíz de
la app y la lectura del lote en MAIN son de `home-offline-blocks`, igual que `profile-persistence` no
montó `ProfileDatabase` hasta que una feature de UI lo consumió. El servicio queda listo para esa
integración (constructor con dependencias explícitas) y para `manual-suggestions-refresh` (`force`).

## Expected File Changes

- `app/src/main/java/com/sazon/app/domain/suggestions/SuggestionBatch.kt` — **crear**; modelo,
  `BatchRefreshReason` y `BatchDecision`.
- `app/src/main/java/com/sazon/app/domain/suggestions/SuggestionBatchPolicy.kt` — **crear**; política
  pura y `BATCH_TTL_MILLIS` (único sitio del TTL).
- `app/src/main/java/com/sazon/app/domain/suggestions/SuggestionBatchRepository.kt` — **crear**; puerto
  `get`/`observe`/`save`/`clear`.
- `app/src/main/java/com/sazon/app/domain/suggestions/SuggestionBatchService.kt` — **crear**;
  orquestación cache-first, validación, dedupe y fallback a semilla.
- `app/src/main/java/com/sazon/app/domain/recipe/RecipeJson.kt` — **modificar (aditivo)**;
  `encodeList`/`parseList` para `List<Recipe>`.
- `app/src/main/java/com/sazon/app/data/suggestions/SuggestionBatchEntity.kt` — **crear**.
- `app/src/main/java/com/sazon/app/data/suggestions/SuggestionBatchDao.kt` — **crear**.
- `app/src/main/java/com/sazon/app/data/suggestions/SuggestionBatchDatabase.kt` — **crear**;
  `suggestion_batch.db`, versión 1.
- `app/src/main/java/com/sazon/app/data/suggestions/SuggestionBatchMappers.kt` — **crear**.
- `app/src/main/java/com/sazon/app/data/suggestions/RoomSuggestionBatchRepository.kt` — **crear**.
- `app/src/test/java/com/sazon/app/domain/suggestions/SuggestionBatchPolicyTest.kt` — **crear**;
  escenarios 1–6 (frescura, firma, caducidad, frontera, `force`).
- `app/src/test/java/com/sazon/app/domain/suggestions/SuggestionBatchServiceTest.kt` — **crear**;
  escenarios 1–9 con generador falso, repositorio en memoria y Taxonomía real por propiedad de
  sistema.
- `app/src/test/java/com/sazon/app/data/suggestions/RoomSuggestionBatchRepositoryTest.kt` — **crear**;
  escenario 10 (Robolectric, base en memoria).
- `docs/technical-discovery.md` — **modificar**; ubicar el Lote en `domain.suggestions` +
  `data.suggestions` (Room, base propia) y la política cache-first.
- `docs/domain-model.md` — **modificar (mínimo)**; el lote **persiste sus recetas** y su `status`
  `fresh`/`stale` es **derivado** por la política (no se guarda); TTL de 7 días.
- `docs/risks-and-open-questions.md` — **modificar (mínimo)**; la entrada resuelta de caducidad apunta
  a `SuggestionBatchPolicy.BATCH_TTL_MILLIS`.
- `PROGRESS.md` y `feature_list.json` — **modificar al implementar**; estado y evidencia (no en esta
  sesión de planificación).

**No se toca** `MainActivity`/`SazonApp`/`HomePlaceholder`, `ProfileDatabase`, `data.profile/**`,
`domain.profile/**`, `domain.generator/**`, `data.generator/**`, la Taxonomía, `seed-recipes.json`,
`app/build.gradle.kts` ni `gradle/libs.versions.toml`.

## Visual Design Impact

- UI involved: **no**.
- Design source: no aplica.
- Screens or states affected: ninguna (el lote se renderizará en `home-offline-blocks`).
- New design artifact required: no.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio omite este fichero por decisión del harness; la
  costura dominio/datos se documenta en `docs/technical-discovery.md`.
- `CONSTRAINTS.md`: **not needed** — la pureza del dominio y la separación dominio/datos ya están
  documentadas; esta feature no introduce una regla global nueva.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas de trabajo no cambian.
- Other docs: `docs/technical-discovery.md` — update (ubicación del Lote y política cache-first);
  `docs/domain-model.md` — update mínimo (recetas persistidas y `status` derivado);
  `docs/risks-and-open-questions.md` — update mínimo (cita del TTL al constante); `PROGRESS.md` y
  `feature_list.json` — update al implementar.

## Implementation Plan

1. Crear el modelo, los enums/sealed y la política pura en `domain.suggestions`
   (`SuggestionBatch`, `BatchRefreshReason`, `BatchDecision`, `SuggestionBatchPolicy`).
2. Crear el puerto `SuggestionBatchRepository` y el `SuggestionBatchService` (validación, dedupe y
   fallback a semilla con manejo correcto de cancelación).
3. Añadir `RecipeJson.encodeList`/`parseList`.
4. Crear la capa Room en `data.suggestions` (entidad, DAO, base propia, mappers, repositorio).
5. Escribir `SuggestionBatchPolicyTest` (escenarios 1–6), `SuggestionBatchServiceTest` (escenarios
   1–9) y `RoomSuggestionBatchRepositoryTest` (escenario 10).
6. Ejecutar `init.sh` (test + lint), corregir lo que salga y comprobar los recuentos.
7. Actualizar `docs/technical-discovery.md`, `docs/domain-model.md` y
   `docs/risks-and-open-questions.md`; registrar evidencia en `feature_list.json` y `PROGRESS.md`
   (lo hace el implementer).

## Implementation Tasks

- [ ] Crear `SuggestionBatch` + `BatchRefreshReason` + `BatchDecision` en `domain.suggestions`.
- [ ] Crear `SuggestionBatchPolicy.decide(...)` y `BATCH_TTL_MILLIS = 7 días` (único sitio).
- [ ] Crear el puerto `SuggestionBatchRepository` (`get`/`observe`/`save`/`clear`).
- [ ] Crear `SuggestionBatchService.ensureBatch(profile, force)` consumiendo `PreferenceSignature`.
- [ ] Filtrar las recetas `ai` con `RecipeValidator.validateRecipe` y mapear el Perfil a
      `RecipeConstraints`.
- [ ] Implementar `deduplicateByTitle` (seed primero).
- [ ] Implementar el fallback a solo semilla ante fallo del generador, sin tragarse
      `CancellationException`.
- [ ] Añadir `RecipeJson.encodeList`/`parseList`.
- [ ] Crear `SuggestionBatchEntity`, `SuggestionBatchDao`, `SuggestionBatchDatabase`,
      `SuggestionBatchMappers` y `RoomSuggestionBatchRepository`.
- [ ] Escribir `SuggestionBatchPolicyTest` con los escenarios 1–6 (incluida la frontera del TTL).
- [ ] Escribir `SuggestionBatchServiceTest` con los escenarios 1–9 (contador de llamadas al
      generador, generador que lanza, receta insegura y duplicado).
- [ ] Escribir `RoomSuggestionBatchRepositoryTest` con el escenario 10.
- [ ] Ejecutar `init.sh` en verde.
- [ ] Actualizar `docs/technical-discovery.md`, `docs/domain-model.md` y `docs/risks-and-open-questions.md`.

## Verification Plan

- `./gradlew test` → verde, con `SuggestionBatchPolicyTest`, `SuggestionBatchServiceTest` y
  `RoomSuggestionBatchRepositoryTest` ejecutados y **0 fallos**. Para depurar un test concreto:
  `./gradlew testDebugUnitTest --tests "com.sazon.app.domain.suggestions.SuggestionBatchServiceTest"`
  (en este proyecto `./gradlew test --tests` no acepta `--tests`; ver `PROGRESS.md`, sesión 014).
- Escenario 1: un generador falso que cuenta llamadas no se invoca con caché fresca y firma igual.
- Escenarios 2–4: la decisión es `Generate(SIGNATURE_CHANGED)` / `Generate(EXPIRED)` y `UseCached` un
  milisegundo antes del TTL.
- Escenario 5 (verificación exigida): con un generador que lanza, el lote resultante tiene **solo**
  recetas `source = seed`, no está vacío y `repository.get()` lo devuelve (utilizable offline).
- Escenario 8: una receta `ai` insegura no aparece en el lote y una segura sí.
- `./gradlew lint` → sin errores nuevos (los warnings preexistentes de versión/plantilla son
  aceptables).
- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → código 0.
- Inspección: `com.sazon.app.domain.suggestions` **no** importa `android.*`/`androidx.*`;
  `data.suggestions` usa Room; **no** cambian `app/build.gradle.kts`, `gradle/libs.versions.toml`,
  `ProfileDatabase` ni el código de UI.

Sobre `init.sh`: **no cambia**. Sigue ejecutando solo comprobaciones no bloqueantes (`test` + `lint`),
no arranca procesos largos y no necesita modos nuevos.

Cobertura E2E: **no aplica**. No hay suite E2E persistente y esta feature no introduce UI ni flujo
visible; el comportamiento observable (decisión cache-first, fallback a semilla, ida y vuelta de
persistencia) es lógica pura de JVM, que es lo que exige `feature_list.json`. La prueba manual
end-to-end llegará con `home-offline-blocks`.

## Evidence To Capture

- Salida de `./gradlew test` con las tres suites nuevas, número de tests y 0 fallos.
- Resultado del escenario 1: contador de llamadas al generador en 0 con caché fresca y firma igual.
- Resultado de los escenarios 2–4: motivos `SIGNATURE_CHANGED`/`EXPIRED` y frontera del TTL.
- Resultado del escenario 5: lote final solo con `source = seed`, no vacío y persistido.
- Resultado del escenario 8: la receta `ai` insegura no entra en el lote.
- Salida de `./gradlew lint` sin errores nuevos y `init.sh` con código 0.
- Confirmación por inspección de que `domain.suggestions` no importa Android y de que no se tocó
  `ProfileDatabase` ni el build.
- Anotación en `feature_list.json` de la evidencia y cambio de estado a `passing` (al implementar).

## Decisions And Unknowns

### Decisiones fijadas

- **El lote se persiste en Room (no en memoria)**, en una entidad/tabla propia. Lo fija
  `docs/technical-discovery.md` ("Lote de sugerencias y recetas `ai` | Room") y lo exige
  `home-offline-blocks` ("todo funciona sin conexión"): un lote solo en memoria desaparecería al
  cerrar la app y no daría soporte offline.
- **Base de datos propia `SuggestionBatchDatabase` (`suggestion_batch.db`, versión 1)** en lugar de
  añadir la entidad a `ProfileDatabase`. Motivo: añadir una entidad obliga a subir `ProfileDatabase` a
  la versión 2 y a una **migración escrita a mano** cuyo camino de upgrade **no se puede ejercitar en
  los tests JVM** (Room `inMemoryDatabaseBuilder` arranca en la versión actual, y `MigrationTestHelper`
  exigiría la dependencia nueva `room-testing`, prohibida). Una base propia mantiene la feature
  aditiva, no toca la persistencia aceptada del Perfil y no añade dependencias. Deuda anotada:
  consolidar en una única base de la app con migración probada es un refactor futuro.
- **La firma se consume, no se recalcula ni se persiste aparte**: el servicio llama a
  `PreferenceSignature.of(profile)` y guarda el `String` resultante como metadato del lote (su clave de
  caché). No hay almacén separado de firmas.
- **La frescura es derivada, no persistida**: `status` `fresh`/`stale` de `docs/domain-model.md` es el
  resultado de `SuggestionBatchPolicy.decide`. No se escribe una columna de estado, para no abrir una
  segunda fuente de verdad que pueda desincronizarse (mismo criterio con el que `AppGate` evitó el flag
  de DataStore).
- **TTL de 7 días en un único sitio**: `SuggestionBatchPolicy.BATCH_TTL_MILLIS`; la política usa
  `elapsed >= TTL` (a los 7 días exactos el lote ya está obsoleto).
- **Orden de decisión**: `null → MISSING`, `force → FORCED`, firma distinta → `SIGNATURE_CHANGED`,
  caducado → `EXPIRED`, si no `UseCached`.
- **El lote contiene sus recetas (`seed` + `ai`)**, coherente con "`SuggestionBatch` contiene 0..n
  `Recipe`" de `docs/domain-model.md` y con que un Favorito debe copiar la receta porque el lote puede
  refrescarse y perderla. Las recetas `seed` se incluyen en el snapshot aunque también estén en el APK:
  así la lectura es autocontenida y no necesita resolver ids contra varios orígenes.
- **Las recetas `ai` pasan por el `RecipeValidator`** antes de entrar en el lote (invariante de
  `docs/domain-model.md`: "todas pasan por el Validador"). `recipe-validator` no figura en
  `depends_on` porque no es una dependencia de build ni de orden del harness, sino un colaborador
  interno ya `accepted` que se reutiliza; sin él, Home podría mostrar sugerencias inseguras, y ninguna
  otra feature valida `suggestByPreferences`.
- **Fallback a solo semilla ante fallo del generador**: se persiste un lote seed-only con la firma
  actual y `now`, garantizando Home offline y seguridad (nunca se reutiliza un lote `ai` de una firma
  antigua, que podría incumplir una restricción recién añadida).
- **Dedupe por título normalizado**, seed primero, según el edge case de `docs/domain-model.md`.
- **Sin dependencias nuevas**; `Room`, `KSP`, corrutinas y `kotlinx.serialization` ya están.
- **Sin integración en la app**: la composición y el disparo "al abrir la app" los hace
  `home-offline-blocks`, siguiendo el precedente de `profile-persistence`.

### Unknowns

- **Modelo de errores del generador (pregunta abierta 1)**: el puerto no distingue "no disponible" de
  un fallo de programación; el servicio trata cualquier `Exception` como IA no disponible y degrada a
  semilla. Cuando `real-ai-provider` fije tipos de error, el servicio podría distinguirlos sin cambiar
  la política.
- **Reintento al volver la red**: un lote seed-only persistido con la firma actual no se reintenta
  hasta que cambie la firma, caduque o el usuario fuerce. `docs/domain-model.md` (Scenario 5) menciona
  refrescar "cuando vuelva la red"; si se quiere ese comportamiento automático, lo añadirá la feature
  que gestione conectividad. Aquí se prioriza proteger la cuota.
- **Reloj hacia atrás**: si `now < generatedAt` (cambio de hora del dispositivo), el cálculo de
  `elapsed` da negativo y el lote se considera fresco; no se añade una regla para este caso.
- **Tamaño de la base**: las recetas se guardan como JSON en una sola fila; con 12 semillas + hasta 6
  `ai` por lote el tamaño es despreciable, pero crecerá si el catálogo semilla crece mucho.
- **Consolidación de bases de datos**: dos ficheros Room (`sazon.db`, `suggestion_batch.db`);
  unificarlos con una migración probada queda como deuda futura (ver Decisión).

### Hallazgos de implementación (implementer)

- **APIs reales**: todas las firmas de la spec compilaron sin cambios
  (`PreferenceSignature.of`, `RecipeGenerator.suggestByPreferences`,
  `RecipeValidator.validateRecipe`, `Taxonomy`, `Recipe`). No se tocó ningún fichero de
  `domain.profile`/`data.profile`, la UI ni el build.
- **`generatedAt` capturado una sola vez**: la spec ilustraba `SuggestionBatch(signature, now(),
  recipes)`; la implementación lee `now()` una vez tras decidir y reutiliza ese instante para el lote
  generado o de fallback, para no depender de un reloj con granularidad fina. El comportamiento
  observable es idéntico (los tests fijan el reloj).
- **Adición de `RecipeJson`**: `encodeList`/`parseList` usan la misma `format` (`ignoreUnknownKeys`,
  `encodeDefaults`) vía `ListSerializer(Recipe.serializer())`; `encode`/`parse` no cambian.

## Validator Checklist

- [ ] La implementación se mantiene en `domain.suggestions` (puro) y `data.suggestions` (Room), sin UI
      y sin tocar `ProfileDatabase`, `data.profile/**`, `domain.profile/**` ni el build.
- [ ] `SuggestionBatchPolicy` es puro, usa `BATCH_TTL_MILLIS = 7 días` en un único sitio y devuelve los
      motivos `MISSING`/`SIGNATURE_CHANGED`/`EXPIRED`/`FORCED`; la frontera `>=` TTL está cubierta.
- [ ] Con caché fresca y misma firma **no** se llama al generador (escenario 1) y no se reescribe el
      lote.
- [ ] Firma distinta y caducidad marcan el lote como obsoleto (escenarios 2 y 3, verificación exigida).
- [ ] Sin IA disponible el lote resultante es **solo semilla**, no vacío y queda persistido/utilizable
      (escenario 5, verificación exigida).
- [ ] Las recetas `ai` inseguras se descartan con `RecipeValidator`; las `seed` y las `ai` válidas
      entran (escenario 8).
- [ ] La composición deduplica por título normalizado con la `seed` como ganadora (escenario 9).
- [ ] La ida y vuelta de persistencia es exacta, la tabla es de una sola fila, `clear()` deja `null` y
      `observe()` emite (escenario 10).
- [ ] El manejo de fallo del generador no se traga `CancellationException`.
- [ ] `domain.suggestions` no importa Android; no se añaden dependencias ni cambia
      `app/build.gradle.kts`/`gradle/libs.versions.toml`.
- [ ] `init.sh` queda en verde y `docs/technical-discovery.md`, `docs/domain-model.md`,
      `docs/risks-and-open-questions.md`, `PROGRESS.md` y `feature_list.json` quedan actualizados y
      coherentes.
