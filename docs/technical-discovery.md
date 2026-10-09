# Technical Discovery

## Product Surface

App **Android nativa**, orientación retrato, un único usuario local. Sin web, sin escritorio, sin
backend propio. El diseño de referencia está en `DESIGN.md`.

## Candidate Stack

- Kotlin + Jetpack Compose + Material 3.
- Android: **`minSdk` 33** (Android 13), **`targetSdk` 36** y **`compileSdk` 36**. Decisión de curso:
  no se soportan versiones anteriores a Android 13.
- Toolchain del bootstrap: Gradle 9.5.0 (wrapper), AGP 9.3.3, Kotlin 2.2.10, Compose BOM 2026.02.01.
- `applicationId` / paquete base: **`com.sazon.app`**. Barato de cambiar ahora; caro tras publicar.
- Room para persistencia local; DataStore para flags de aplicación (p. ej. "onboarding completado").
- KSP para procesar las anotaciones de Room. Con el Kotlin integrado de AGP 9, KSP necesita la
  excepción `android.disallowKotlinSourceSets=false` en `gradle.properties` para registrar sus fuentes
  generadas; sin ella, la configuración falla.
- Coroutines + Flow para asincronía.
- Cliente HTTP (Retrofit o Ktor) + `kotlinx.serialization` contra la API compatible con OpenAI de
  **DevExpert Inference** (proveedor decidido; ver `docs/adr/0003-ai-provider.md`).
- Inyección de dependencias ligera. En un proyecto de curso, manual o Koin es suficiente; Hilt añade
  ceremonia.
- Tests: JUnit en JVM. El dominio no debe depender de Android para poder testarse sin emulador.

## Data and Storage

| Qué | Dónde | Por qué |
|---|---|---|
| Taxonomía | `app/src/main/assets/taxonomy.json`, generado por la tarea `syncTaxonomy` desde `docs/domain/taxonomy.json`, y parseado a objetos tipados al arrancar | Fuente única editable y artefacto empaquetado, sin base de datos |
| Recetas semilla | **JSON fuente versionado** en `app/src/main/assets/seed-recipes.json`, cargado por la app y validado por test (decisión: riesgos, antes pregunta 7) | Contenido empaquetado, siempre disponible offline, editable sin recompilar |
| Perfil de usuario (identidad) | Room (1 fila, compartida con preferencias) | Nombre y color de avatar |
| Perfil de preferencias | Room (1 fila); el flag de onboarding en DataStore queda **diferido** (la puerta usa la presencia de Perfil, ver más abajo) | Estado mutable del usuario; fuente de la firma |
| Resumen de actividad | Contador `recipesGeneratedCount` en Room + COUNT de Favoritos | El contador se persiste; el conteo de favoritos se deriva |
| Favoritos | Room | Disponibilidad offline garantizada |
| Lote de sugerencias y recetas `ai` | Room, base propia `suggestion_batch.db` (`data.suggestions`); política y servicio puros en `domain.suggestions` | Evita llamadas repetidas a la IA y da soporte offline |
| Iconos e imágenes de receta | Assets locales (`assets/icons/**`, `assets/img/recipes/**`) | Determinista, offline, sin coste |
| Clave de IA | `BuildConfig`, desde `local.properties` | Decisión de demo; ver riesgos |

La Taxonomía **no** se persiste en base de datos: se empaqueta con la app y es idéntica entre
versiones. Los `id` nunca se renombran.

`docs/domain/taxonomy.json` es la única fuente editable. La tarea de Gradle `syncTaxonomy` la copia a
`app/src/main/assets/`, que está ignorado en git. El parseo vive en `com.sazon.app.domain.taxonomy`
(`TaxonomyJson.parse`, sin dependencias de Android) y la lectura del asset en `com.sazon.app.data`
(`AssetTaxonomySource`), de modo que los tests corren en JVM sin emulador. Ver
`docs/specs/taxonomy-in-code.md`.

El esquema de una receta vive en `com.sazon.app.domain.recipe` (`Recipe`, `RecipeIngredient`,
`RecipeStep`, `Nutrition`, `RecipeSource` y `MeasureUnit`), un único modelo serializable con
`kotlinx.serialization` para recetas `seed` y `ai`, también sin dependencias de Android. Ver
`docs/specs/recipe-domain-model.md`.

El Catálogo de recetas semilla vive en `app/src/main/assets/seed-recipes.json` (12 recetas, envelope
`{ "recipes": [...] }`). A diferencia de la Taxonomía, este JSON es **fuente versionada**, no generado:
no tiene gemelo en `docs/` ni tarea `sync`, y `.gitignore` no lo ignora. El parser puro vive en
`com.sazon.app.domain.recipe` (`SeedRecipeCatalog` + `SeedRecipeJson.parse`, sin Android) y la lectura
del asset en `com.sazon.app.data` (`AssetSeedRecipeSource`), mismo patrón que la Taxonomía. La
integridad y coherencia del contenido se verifican en JVM con `SeedRecipeCatalogTest` (referencias a la
Taxonomía, alérgenos, tiempos y dos casos negativos con JSON mutado) y `AssetSeedRecipeSourceTest`
(Robolectric) confirma que el asset se empaqueta y se carga por nombre. Ver
`docs/specs/seed-recipe-catalog.md`.

El Validador de recetas vive en `com.sazon.app.domain.validation` (`RecipeValidator`,
`RecipeConstraints`, `DiscardReason`, `DiscardedRecipe`, `RecipeValidation`, `ValidationOutcome` y el
helper interno `IngredientText`). Es lógica pura sin Android: consume el modelo de Receta y la
Taxonomía, parsea el envelope `{ "recipes": [...] }` elemento a elemento (sin lanzar ante una
respuesta mala) y aplica las reglas 1–4 de `docs/domain/taxonomy.md` §5 en orden, devolviendo las
recetas aceptadas y el motivo de cada descarte (no registra log ni persiste: eso es del llamador).
Testeado en JVM (`RecipeValidatorTest`). Ver `docs/specs/recipe-validator.md`.

La persistencia del Perfil de usuario (identidad + preferencias) vive en
`com.sazon.app.data.profile` sobre Room 2.8.5: una sola fila (`user_profile`) con `INSERT OR REPLACE`
y una columna JSON por lista (conversor con `kotlinx.serialization`). El dominio puro
`com.sazon.app.domain.profile` define `UserProfile`, `PreferenceProfile`, `ProfileName` y el puerto
`ProfileRepository` (`get`/`observe`/`save`/`clear`). El test corre en JVM con Robolectric y base de
datos en memoria, sin emulador. Ver `docs/specs/profile-persistence.md`.

La Firma de preferencias vive en `com.sazon.app.domain.profile.PreferenceSignature.of(preferences)`:
un hash SHA-256 determinista sobre una cadena canonica (listas tratadas como conjuntos) que **excluye**
la identidad (nombre y color de avatar) y `updatedAt`. Logica pura sin Android, testeada en JVM. El
Lote de sugerencias la **consume** (`SuggestionBatchService`) y la guarda como metadato de la cache
(su clave), sin almacen separado de firmas. Ver `docs/specs/preference-signature.md`.

El Lote de sugerencias vive en `com.sazon.app.domain.suggestions` como logica pura
(`SuggestionBatch`, `BatchRefreshReason`, `BatchDecision`, `SuggestionBatchPolicy` con
`BATCH_TTL_MILLIS` de 7 dias en un unico sitio, el puerto `SuggestionBatchRepository` y
`SuggestionBatchService.ensureBatch`). El servicio es **cache-first**: consume
`PreferenceSignature.of(profile)` y solo llama al Generador cuando la politica decide
`MISSING`/`SIGNATURE_CHANGED`/`EXPIRED`/`FORCED`; filtra las recetas `ai` con `RecipeValidator`,
deduplica por titulo normalizado (gana la `seed`) y, si el Generador falla, degrada a un lote
seed-only persistido (modo esperado, sin tragarse `CancellationException`). La persistencia esta en
`com.sazon.app.data.suggestions` sobre Room, en una base **propia** (`suggestion_batch.db`, version 1,
una fila con las recetas como columna JSON via `RecipeJson.encodeList`/`parseList`) para no migrar la
base aceptada del Perfil. Testeado en JVM (`SuggestionBatchPolicyTest`, `SuggestionBatchServiceTest` y
`RoomSuggestionBatchRepositoryTest`). La composicion **si** ocurre ya en la app: la realiza
`home-offline-blocks`. Ver `docs/specs/suggestion-batch-cache.md`.

La **Home real y el armazon principal** viven en `com.sazon.app.ui`: `MainShell` es un `Scaffold` con
la **barra de navegacion inferior** de tres destinos (Inicio · Favoritos · Perfil, `MainTab`) y el
**FAB** "＋ Crear receta", y enruta el contenido por pestana (`ui.home.HomeScreen` para Inicio y
`SectionPlaceholder` para Favoritos/Perfil/Generador). `HomeScreen` pinta los cuatro bloques **locales**
de `DESIGN.md` (el bloque 2, Búsqueda con IA, es de `ai-search`): cabecera con saludo por franja
horaria y avatar, tarjeta-heroe al Generador, carrusel "Sugeridas para ti" y bloque "Tus recetas" con su
estado vacio. La logica/estado de la Home es **pura** y testeable en JVM en `com.sazon.app.ui.home`
(`HomeGreeting`, `HomeIdentity`, `HomeSuggestions`, `HomeRecipes`, `RecipeCardFormat`) y en
`com.sazon.app.ui.navigation` (`MainTab`, `PushedScreen`, `MainNavigationState`, navegacion sin
libreria: el `Saver` de `rememberSaveable` vive en la capa Compose). `MainActivity` compone a mano
(sin DI) `SuggestionBatchDatabase` + `RoomSuggestionBatchRepository` + `SuggestionBatchService`
(`FakeRecipeGenerator`, sin red) + la Taxonomia y el catalogo semilla; `SazonApp` observa el
`UserProfile` completo y monta `MainShell`. `MainShell` **consume el Lote tal cual**:
`SuggestionBatchRepository.observe()` alimenta el carrusel y `SuggestionBatchService.ensureBatch` se
llama una vez al entrar (politica cache-first ya aceptada); mientras el Lote no se resuelve, la Home
pinta las recetas semilla empaquetadas (`HomeSuggestions.visible`) y nunca muestra una pantalla de
carga. La Home es **offline**: solo la futura Búsqueda con IA necesita red. Ver
`docs/specs/home-offline-blocks.md`.

El **filtrado local de "Tus recetas"** vive en `com.sazon.app.ui.filters`: `LocalResultFilter` (seleccion
**unica**: `All`, `Category(categoryId)`, `Fast` y `Vegetarian`, cada una con una `key` estable para
`rememberSaveable`) y `LocalResultFilters`, un `object` **puro** (sin Compose/Android, testeado en JVM)
con `visible(recipes, filter, taxonomy)`, `vegetarianTags(taxonomy)`, `categoriesIn(recipes, taxonomy)`,
`label(...)`, `isDerived(...)` y `fromKey(...)`. "Rápido" y "Vegetariano" son filtros **derivados**, no
categorias: "Rápido" selecciona `totalMinutes <= FAST_MAX_MINUTES` (30) y "Vegetariano" excluye recetas
con algun ingrediente resuelto cuyos tags esten en `dietPatterns[vegetarian].excludedTags`
(`meat`/`poultry`/`fish`/`seafood`, con respaldo constante defensivo); la deteccion es por **tags, no
por keywords**, asi que un ingrediente libre no se excluye. `ResultFilterRow` es la fila de chips
(`FilterChip` de Material 3, sin dependencias nuevas) con "Todas" primero, una chip por categoria
**presente** en las recetas (orden de `taxonomy.recipeCategories`), y "Rápido"/"Vegetariano" agrupados
al final y con el acento oliva (`secondary`) cuando estan seleccionados, para distinguirlos de las
categorias (terracota/`primary`). Se aplica al bloque "Tus recetas": `HomeRecipes.filteredBy(filter,
taxonomy)` acota cada grupo de forma independiente y `HomeScreen` guarda la seleccion con
`rememberSaveable` + `Saver` (clave/`fromKey`), pinta la fila y el estado "sin resultados para este
filtro"; si el bloque llega **vacio** solo pinta la linea de usuario nuevo (sin chips). La logica es
**reutilizable** por `favorites`. Ver `docs/specs/local-result-filters.md`.

El **paso de ingredientes del Generador** vive en `com.sazon.app.ui.generator`. La logica/estado es
**pura** y testeable en JVM: `GeneratorIngredient` (ingrediente anadido: de la Taxonomia si
`ingredientId != null`, **libre** si `null`), `GeneratorIngredientsState` (`typeQuery`, `toggleTaxonomy`,
`addFree`, `remove`, `count`, `canContinue` y `countLabel` con singular/plural) y `GeneratorStep`
(`INGREDIENTS` "¿Qué tienes?" y `SETTINGS` "Ajusta tu receta", con `next`/`previous`/`isLast`). Las
reglas: dedupe de Taxonomia por `ingredientId` (el segundo toque quita), dedupe de libres por **nombre
normalizado** (`trim` + minusculas + sin acentos) tambien contra los ya anadidos, y un texto en blanco
como **no-op**; `IngredientsFilter` (ya en `ui.preferences`) se reutiliza sin cambios para la nube de
sugeridos, que sale siempre de `taxonomy.ingredients` (la UI no diverge de la Taxonomia). La capa Compose
es `GeneratorFlow` (armazon del asistente dentro de `PushedScreen.GENERATOR`: barra superior con "Atras"
y el titulo del paso, primario anclado y `Saver` de `rememberSaveable` para el paso y el borrador, que
asi se conserva al ir y volver del paso 2) y `GeneratorIngredientsScreen` (input libre con "Añadir",
chips de los anadidos con "×", contador con cifras tabulares y nube **colapsable** "Ingredientes
sugeridos", sin iconos: "×" y "Atras" son texto). No se persiste el borrador en disco (asistente
efimero). `MainShell` monta `GeneratorFlow` en el destino `GENERATOR` en lugar del `SectionPlaceholder`
(que se sigue usando para Favoritos/Perfil); el paso 2 es un **marcador** hasta
`generator-settings-step`. Ver `docs/specs/generator-ingredients-step.md`.

El Constructor de prompt vive en `com.sazon.app.domain.prompt.PromptBuilder.build(taxonomy, request)`:
traduce el Perfil de preferencias y los ajustes opcionales a un `BuiltPrompt(system, user)` que exige
el contrato de `docs/domain/taxonomy.md` seccion 4 con `id` de la Taxonomia y garantiza que el patron
de dieta y las restricciones duras aparecen **siempre**; pide 6 recetas por generacion
(`RECIPES_PER_GENERATION`). Es logica pura sin Android, red, persistencia ni coroutines, y no incluye
la Firma de preferencias (es clave de cache del Lote, no contenido del prompt). Testeado en JVM
(`PromptBuilderTest`). La implementacion real del proveedor lo consumira. Ver
`docs/specs/prompt-builder.md`.

El Tema de la app vive en `com.sazon.app.ui.theme` y es la unica fuente de verdad visual,
materializada desde `DESIGN.md`: `Color.kt` (paleta + `LightColorScheme`/`DarkColorScheme` con todos
los roles sobrescritos, sin purple/pink por defecto), `Type.kt` (`SazonTypography` + familias
Manrope/Inter), `Shape.kt` (formas 16/24 + pildora) y `Spacing.kt` (espaciado 12/16/24 y metricas de
componentes). `SazonTheme` elige la variante clara/oscura con `isSystemInDarkTheme()` y **no** usa
color dinamico de Material You: ninguna ruta lee el fondo de pantalla, por lo que los colores no
cambian con el. El **modo oscuro entra en el MVP** (pregunta abierta 3 de `DESIGN.md`, resuelta). Las
fuentes OFL Manrope/Inter se empaquetan en `app/src/main/res/font/` (instancias estaticas de los pesos
600/700 y 400/500, generadas con `fontTools` de las fuentes variables oficiales; licencias en
`docs/licenses/`) y no se descargan por red. El fondo de ventana del sistema se alinea con
`background`/`darkBackground` (`values/colors.xml`, `values-night/colors.xml`, `themes.xml` y
`values-night/themes.xml`) y fija `android:forceDarkAllowed=false`. `DesignTokensTest` (JVM) compara
los tokens expuestos con el frontmatter YAML de `DESIGN.md` (con caso negativo) leyendo la ruta por la
propiedad de sistema `design.tokens.path`. Ver `docs/specs/design-tokens-and-theme.md`.

El punto de entrada real de la app vive en `com.sazon.app.ui`: `SazonApp` compone la raíz y la
**puerta de primer arranque**, que en esta feature es la **presencia de Perfil** en Room
(`ProfileRepository.observe()` → `AppGate.decide`): sin Perfil se muestra el onboarding obligatorio y
no saltable; con Perfil, el armazon principal (`MainShell`, ver mas arriba). **No** se usa el flag de DataStore (diferido sin segunda fuente de verdad que
pueda desincronizarse) ni una librería de navegación: el cambio de destino es un estado mínimo
(`AppDestination`), adoptable más tarde sin cambiar los contratos puros. El onboarding entrega el
**armazón reutilizable** (`OnboardingScaffold`: progreso "Paso N de 7" con puntos, "Atrás" y primario
anclado) y los **7 pasos reales** (paso 1 identidad; pasos 2–7 dieta, estilo, cocinas, ingredientes,
restricciones duras y cocina), con lógica pura en `com.sazon.app.ui.onboarding`
(`OnboardingStep`, `OnboardingProgress`, `AvatarInitials`, `OnboardingIdentityState`,
`OnboardingPreferencesDraft`, `OnboardingProfileBuilder`) testeada en JVM. El borrador vive en
`rememberSaveable` y al cerrar se construye y persiste el Perfil. `MainActivity` monta
`ProfileDatabase.build(applicationContext)` → `RoomProfileRepository` + `AssetTaxonomySource` +
`AssetSeedRecipeSource` + `SuggestionBatchDatabase.build(applicationContext)` →
`RoomSuggestionBatchRepository` + `SuggestionBatchService(FakeRecipeGenerator(), …)` y compone
`SazonTheme { SazonApp(...) }`. `SazonApp` observa el `UserProfile` completo (distingue la carga de la
ausencia de Perfil) y monta `MainShell`. Ver `docs/specs/onboarding-shell-and-identity.md` y
`docs/specs/home-offline-blocks.md`.

El borrador en memoria de los pasos 2–7 vive en
`com.sazon.app.ui.onboarding.OnboardingPreferencesDraft`, una data class **pura** sin imports de
Compose ni Android (se testea en JVM): `dietPatternId` (single-select, por defecto el primer
`dietPatterns` de la Taxonomía, `omnivore`), `softPreferenceIds`, `cuisineIds`,
`preferredIngredientIds` y `dislikedIngredientIds` (listas con semántica de conjunto, sin duplicados),
`hardRestrictionIds` (multi-select con "Ninguna" = lista vacía) y `difficultyId`, `timeBucketId` y
`defaultServings` (por defecto el primer `difficulties`/`timeBuckets` y `servings.default` de la
Taxonomía). Las operaciones son inmutables (`copy`): `selectDietPattern`,
`toggleSoftPreference`/`clearSoftPreferences`/`isStyleNone`, `toggleCuisine`,
`togglePreferredIngredient`/`toggleDislikedIngredient` (**exclusión mutua**: elegir un ingrediente en
un grupo lo quita del otro, de modo que la intersección nunca se solapa), `toggleHardRestriction`/
`clearHardRestrictions`/`hasNoRestrictions`, `selectDifficulty`, `selectTimeBucket` y
`setServings(limits)` (recorta al rango `min..max`). `OnboardingFlow` lo guarda con `rememberSaveable`
(Saver en la capa Compose, ampliado a todos los campos) y enruta los pasos 2–7 a los editores. Los
editores son **componentes reutilizables y sin estado** en `com.sazon.app.ui.preferences`
(`DietPatternEditor`, `SoftPreferencesEditor` con chip "Ninguno", `CuisinesEditor` en rejilla,
`IngredientsEditor` con buscador y dos grupos "Me gusta" (terracota) y "Evito" (oliva),
`HardRestrictionsEditor` con banner ámbar y chip "Ninguna", `DifficultyEditor`, `TimeBucketEditor` y
`ServingsStepper`), reciben las listas de la Taxonomía y `value` + callbacks, y no conocen el
onboarding ni el Perfil: materializan la regla "un editor, dos entradas" que `profile-screen`
reutilizará. `IngredientsFilter.matching(ingredients, query)` (puro) filtra por `label` sin distinguir
mayúsculas ni acentos. Los chips de alérgenos usan el rol **tertiary** (ámbar) y **nunca** el rol de
error/rojo; el stepper muestra cifras tabulares y deshabilita `−`/`+` en los límites. No renderizan
iconos (el repositorio no tiene assets ni dependencia de iconos) y usan solo tokens del tema.

El cierre del onboarding es **puro** en `OnboardingProfileBuilder.build(displayName, avatarColorId,
draft, now)`: valida y normaliza el nombre con `ProfileName` (mismo contrato que
`RoomProfileRepository.save`), inyecta el instante del cierre (`createdAt`/`updatedAt`) y copia todos
los campos del borrador a `PreferenceProfile`; un nombre inválido lanza `IllegalArgumentException` y no
se escribe ninguna fila. `OnboardingFlow` cambia `onFinished` a `(UserProfile) -> Unit` y construye el
Perfil al pulsar "Empezar a cocinar"; `SazonApp` lo persiste con `ProfileRepository.save` en un
`rememberCoroutineScope`, de modo que la observación existente hace que `AppGate.decide` pase a `MAIN`
y el onboarding no vuelva a aparecer. Ver `docs/specs/onboarding-ingredients-safety-kitchen.md`.

Decisiones de arquitectura que afectan a este documento: `docs/adr/0001-taxonomy-in-code.md`,
`docs/adr/0002-local-first-no-backend.md` y `docs/adr/0003-ai-provider.md`.

## Integrations

- **API de IA**: única integración externa. Proveedor decidido: **DevExpert Inference**, API
  compatible con OpenAI (base `https://inference.devexpert.io/v1`, `POST /chat/completions`, auth
  `Bearer DEVEXPERT_API_KEY`). Alias `chat` (diario) y `chat-pro` (avanzado); el modelo real puede
  cambiar sin cambiar el alias. Cuota **semanal** por clave. Ver `docs/adr/0003-ai-provider.md`.
- **Salida estructurada**: la compatibilidad con OpenAI **no** garantiza `json_schema`/structured
  outputs. El contrato del prompt pide JSON y el `RecipeValidator` parsea de forma tolerante (sin
  lanzar), por lo que la app **no** depende de salida estructurada forzada.
- El dominio depende de la interfaz `RecipeGenerator` (tres operaciones: sugerir por preferencias,
  buscar por texto y generar desde ingredientes), **no** de un SDK concreto. La implementación real
  es lo último que se conecta. El puerto vive en `com.sazon.app.domain.generator` (puro, sin Android)
  junto a `GenerationConstraints`, la costura minima de `generateFromIngredients`.
- `FakeRecipeGenerator` (datos fijos, sin red) es la implementación por defecto en desarrollo y
  tests, y permite construir y validar toda la app antes de elegir proveedor. Vive en
  `com.sazon.app.data.generator` (Kotlin puro, sin Android ni red) y sus fixtures incluyen, a
  proposito, recetas que el Validador debe descartar: un ingrediente libre "Pan" (gluten) y un
  ingrediente de carne/ave con id real de la Taxonomia.
- **Búsqueda con IA**: no se persiste un caché consulta→resultado; se memoiza en memoria por sesión
  para evitar llamadas repetidas al volver atrás. Las recetas devueltas se guardan como recetas `ai`
  en la caché normal.
- No hay analítica, ni telemetría remota, ni backend.

## Authentication and Authorization

No aplica: no hay cuentas, roles, ni recursos compartidos. Todos los datos pertenecen al usuario del
dispositivo y no salen de él, salvo la petición de generación a la API de IA.

## Deployment and Operations

- Distribución como APK de depuración para el curso. **No** Play Store en el MVP.
- La clave de IA se inyecta en tiempo de build desde `local.properties` (que no se versiona).
- Sin CI obligatoria. El harness usa `init.sh` localmente.
- Operación: la asume el propio estudiante; no hay entorno de producción.

## Testing and Verification

- **Tests unitarios de dominio en JVM** (obligatorios): integridad de la Taxonomía, `PromptBuilder`,
  parsing y validación de la respuesta, descarte por Restricciones duras, firma de preferencias y
  política de caché, escalado de raciones.
- `FakeRecipeGenerator` (determinista, datos fijos, sin red) disponible para el flujo completo sin
  red, incluidas recetas inseguras para el test negativo del Validador.
- **Prueba manual end-to-end** en emulador (ver `docs/build-brief.md` → Validation Plan).
- `init.sh` debe ejecutar los tests de forma reproducible y ser el punto de entrada de verificación
  del harness.

## Observability

- Sin telemetría. El diagnóstico se hace con **log local de eventos de IA**: proveedor, código de
  respuesta, duración y **motivo de descarte del validador**.
- El motivo del descarte es la señal más valiosa del producto: indica si el problema es el prompt o
  el modelo.

## Constraints

- **Sin backend**: la clave de IA viaja en el APK → solo válido para demo.
- **Coste y cuota**: DevExpert Inference tiene cuota **semanal** por clave (al 100 % se rechazan las
  peticiones hasta el reset) → la política cache-first y el modo semilla protegen el coste.
- **Red intermitente**: solo la Búsqueda con IA necesita red; el Lote de sugerencias, "Tus recetas" y
  Favoritos deben funcionar sin conexión.
- **Tamaño del APK**: las imágenes locales de receta suman; conviene formato WebP y resolución
  moderada.
- **Tiempo de curso**: el MVP debe priorizar el corte vertical sobre la amplitud de features.
- **Complejidad de build**: Kotlin/Compose conlleva Gradle, SDK y emulador; es el mayor coste de
  arranque del proyecto.
