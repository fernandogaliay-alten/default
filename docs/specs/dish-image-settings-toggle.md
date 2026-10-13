# Feature Implementation Spec: Ajuste de imagen de plato (interruptor y tope por sesión)

## Source Feature

- `id`: `dish-image-settings-toggle`
- `area`: `ui` (la única parte **con UI nueva** de la fase 4; el swap/progresión fueron de
  `dish-image-progressive-ui`, `area: integration`)
- `depends_on`: `["dish-image-progressive-ui"]` (satisfecha: `accepted`, sesión 092)
- `status`: `not_started` (en el momento de planificar)
- `source`: `feature_list.json` (entrada líneas 1021-1033) + `docs/ai-dish-images-proposal.md` §6 y §10
- Verificación exigida (textual de la feature): `./gradlew test` cubre el estado en JVM (interruptor
  apagado = no se pide ninguna imagen; encendido = se permite; tope por sesión respetado, patrón
  `AiProviderPolicy`); prueba manual en emulador de que apagar deja de generar y encender reanuda;
  desviación de capturas autorizada registrada en `DESIGN.md`.
- **Desviación de capturas autorizada por la persona (2026-10-13)**: **no** se crea
  `docs/design/concepts/dish-image-settings-toggle/`. Ver `Visual Design Impact`.

## Goal

Dar al usuario **control del gasto de cuota** de la imagen de plato generada (fase 4 de
`docs/ai-dish-images-proposal.md` y `docs/adr/0005-ai-dish-images.md`) mediante dos piezas que la
reconciliación del split dejó atribuidas a esta feature (`dish-image-progressive-ui` solo dejó el
swap/progresión y el límite de concurrencia 2):

1. Un **interruptor** visible en la pantalla **Perfil** ("Imágenes del plato") que activa o desactiva
   por completo la generación. Con el interruptor **apagado**, la app usa **solo la foto local**: el
   proveedor del seam `LocalGeneratedRecipeArtworkSource` devuelve `null` **antes** de resolver, así
   que no se lee caché, no se generan imágenes nuevas y el usuario ve la cadena de `recipe-photos`
   como hoy.
2. Un **tope de generaciones por sesión** (una unidad por invocación del generador real, es decir por
   petición al proveedor), con la constante en **un único sitio** (patrón `AiProviderPolicy`), su
   **imposición** en el borde de generación (decorador del puerto `DishImageGenerator` cableado en
   `MainActivity`) y su **visualización** junto al interruptor ("quedan N de 12 en esta sesión").

"Sesión" = **vida del proceso** de la app: `DishImageSettings` se crea una vez en `MainActivity`; al
morir el proceso el contador vuelve a cero. El interruptor y el tope son **por sesión (en memoria)**,
no persistidos (ver "Decisions And Unknowns", Decisión 2). El punto de inyección de esta mitad es
**el proveedor del seam en `MainActivity`**, y ni `RecipeArtwork` ni sus 5 puntos de uso cambian.

## Scope Reconciliation (split de la fase 4, 2026-10-13)

`docs/specs/dish-image-progressive-ui.md` dejó escrito el reparto y este spec lo **ejecuta** (decisión
cerrada, no reabrir):

- **Ya hecho (aceptado, sesión 092)**: costura `GeneratedRecipeArtworkSource`, precedencia + crossfade,
  decodificación + LRU de bitmaps, concurrencia 2 y su **propio** `verification` ("límite de
  concurrencia (2) respetado").
- **Aquí (`dish-image-settings-toggle`)**: el **interruptor** de usuario, el **tope por sesión**
  (mecanismo + constante + imposición estricta + visualización) y su **estado en JVM**. La
  reconciliación del harness (2026-10-13, pre-implementación) retiró de la entrada del hermano la
  frase "tope de generaciones por sesión respetado" y la sustituyó por el límite de concurrencia; por
  tanto **este** spec es el contrato único del tope y del interruptor.
- `feature_list.json` **no se modifica** (el planner no lo toca): el `verification` de esta feature ya
  dice exactamente lo que se implementa.

## Non-Goals

- **No hay pantalla, ruta ni pestaña nuevas**: se reutiliza la pantalla **Perfil**
  (`ui/profile/ProfileScreen.kt`, `MainTab.PROFILE`) con el estilo de sección ya existente
  (título `titleMedium` + fila + helper). **No** se implementan las secciones "Ajustes"
  (Notificaciones, Idioma, Sobre la app) que `docs/specs/profile-screen.md` dejó fuera de alcance.
- **No se persiste nada**: ni el interruptor ni el tope (sesión = proceso). Nada de Room/DataStore ni
  campos nuevos en `UserProfile`/`ProfileRepository` (ver Decisión 2).
- **No se toca la costura aceptada**: `RecipeArtwork` conserva **firma y geometría**, los **5 puntos
  de uso** no cambian, y no se modifican `GeneratedRecipeArtworkSource`, `DishImageArtworkLoader`,
  `DishImageArtworkPolicy`, `RecipeArtworkImagePrecedence`, `RecipeImageDecoder`,
  `AndroidRecipeImageDecoder` ni `RecipePhotoSource`.
- **No se modifica ningún fichero aceptado de `data.image`/`data.ai.image`/`domain.image`**:
  `DishImageResolver`, `DishImageCachePolicy`, `FileDishImageCache`, `MemoryDishImageCache`,
  `AndroidDishImageCache`, `DevExpertDishImageGenerator`, `DishImagePolicy`, el puerto y
  `DishImageRequest`/`DishImageCacheKey` quedan **intactos** (el tope se **envuelve**, no se edita).
- **No hay re-resolución al cambiar el interruptor**: las tarjetas ya pintadas no se vuelven a pedir
  (no hay recarga global ni invalidación de `produceState`); el ajuste se aplica a las **peticiones
  nuevas** (nueva tarjeta, scroll, nueva navegación). Ver Decisión 5.
- **No hay reset manual** del tope en la UI (el usuario solo lo **ve**); no hay tope por receta,
  por origen (`seed`/`ai`), por día ni persistido.
- **No** se implementan las fases 5 (`dish-visual-descriptor`), 6 (`dish-style-reference`) ni 7
  (`dish-image-seed`), ni el autochequeo con visión de la propuesta §7.4, ni un aviso/shimmer de
  "generando imagen" (el estado visual no cambia).
- **No hay dependencias nuevas** ni cambios en `app/build.gradle.kts`, `gradle/libs.versions.toml`,
  `init.sh`, `scripts/**`, `AndroidManifest.xml`, assets ni Taxonomía. Sin telemetría ni `Log`.
- **No se toca** `domain/**`, `data/image/DishImageResolver.kt` y compañía, `data/**` ya aceptado,
  `ui/artwork/**`, `ui/theme/**`, `docs/design/concepts/**` ni las specs ya aceptadas.

## Job Story

Cuando estoy en los ajustes de mi Perfil y me preocupa el consumo de cuota de las imágenes,
quiero poder apagar la generación de imágenes de plato (o encenderla) y ver cuántas quedan en esta
sesión,
para controlar el gasto sin perder la foto local de cada receta ni ver errores.

## Users And Permissions

- **Usuario local** (único dueño del dispositivo, ADR 0002): abre la pestaña **Perfil**, ve la sección
  "Imágenes del plato", apaga/enciende el interruptor y lee el tope restante de su sesión. No hay
  cuentas ni roles.
- **Desarrollador / agente**: ejercita la lógica en **JVM** con generadores/resolutores falsos, sin
  red, sin clave, sin cuota y sin emulador.
- **Persona**: revisa el acabado y el comportamiento en el emulador tras el merge (AGENTS.md no
  automatiza UI de emulador); contra el registro de `DESIGN.md`.

## Acceptance Scenarios

### Scenario 1: interruptor apagado → no se pide ninguna imagen (verificación exigida)

Given un `DishImageSettings` con el interruptor **apagado** y un productor contador (delegado que
cuenta invocaciones) detrás del seam,
When el usuario navega/compone tarjetas (se llama a la costura en el punto de inyección),
Then la costura devuelve `null` **sin invocar al productor** (0 peticiones: no se lee caché, no se
genera) y `RecipeArtwork` pinta la foto local (o el degradado + glifo), sin error de UI.

### Scenario 2: interruptor encendido → se permite generar (verificación exigida)

Given el interruptor **encendido** y presupuesto disponible,
When llega una petición de imagen,
Then la costura **sí** produce (el productor se invoca una vez) y se aplica el tope: con el
presupuesto agotado, el decorador devuelve `null` sin invocar al delegado.

### Scenario 3: tope por sesión respetado (verificación exigida)

Given un tope inyectado pequeño (p. ej. `N = 2`) y un generador falso que cuenta invocaciones y
devuelve bytes,
When se piden imágenes generadas hasta agotarlo y una más,
Then las primeras `N` invocan al generador (una unidad por petición al proveedor), la `N+1` devuelve
`null` **sin invocar** al delegado (la UI conserva la foto local), y `used`/`remaining`/`exhausted`
reflejan el consumo. La constante `MAX_GENERATIONS_PER_SESSION` vive en un único sitio.

### Scenario 4: el interruptor apagado no consume tope, y no puede reiniciarlo

Given un presupuesto intacto, `When` el interruptor está apagado y se piden generaciones, `Then`
`used` sigue en **0** (no hay reserva); y dado un tope ya agotado (`used == max`), `When` el usuario
apaga y vuelve a encender el interruptor, `Then` `exhausted` sigue siendo `true` (apagar/encender
**no** es un reset del tope).

### Scenario 5: estado observable para la UI (determinista)

Given un `DishImageSettings`,
When cambian el interruptor y el consumo,
Then `state` (un `StateFlow`) emite el estado correspondiente con `enabled`, `used`, `max`,
`remaining = (max - used).coerceAtLeast(0)` y `exhausted = used >= max`; el valor inicial es
`enabled = true`, `used = 0`, `max = 12` (política por defecto).

### Scenario 6: un acierto de caché no consume tope

Given un `DishImageResolver` real con memoria/disco falsos y el generador envuelto por el decorador,
When se resuelve **dos veces** la misma clave canónica y una clave nueva,
Then el generador se invoca **una** vez (el acierto de memoria/disco no pasa por el decorador) y
`used` es exactamente 1.

### Scenario 7: microcopy del tope (puro y determinista)

Given el estado del ajuste,
When se formatea el resumen visible,
Then con el interruptor activado y `remaining = 12` el texto indica que quedan 12 de 12; con
`remaining = 1` usa el singular ("Queda 1 de 12"); con `remaining = 0` indica que el tope de la
sesión está alcanzado (12 de 12) y que se reanudará al reiniciar la app; con el interruptor apagado
indica que se usa solo la foto local. La misma entrada produce siempre la misma cadena.

### Scenario 8: fallo y cancelación del generador (degradación silenciosa)

Given el decorador envolviendo un delegado que lanza,
When el delegado lanza una excepción de red/`null`, `Then` la costura devuelve `null` sin romper la
UI (contrato del ADR 0005); `When` el delegado lanza `CancellationException`, `Then` esta **se
propaga** (no se convierte en `null`). Semántica conservadora documentada: una reserva previa a una
generación cancelada **sí** queda consumida (es lo que se paga).

### Scenario 9: alcance y pureza (inspección)

Given la implementación terminada,
When se inspecciona el diff,
Then `RecipeArtwork` conserva la firma `(recipe, modifier, glyphSize)` y el resto de `ui/artwork`
está **sin diff**; los **5 puntos de uso** no cambian; `data.image` solo **gana** ficheros nuevos
(ningún fichero aceptado cambia) y esos ficheros nuevos no importan `android.*`/`androidx.*`; el
único fichero Compose nuevo es la sección de Perfil; no hay dependencias nuevas; y **no** existe
`docs/design/concepts/dish-image-settings-toggle/`.

## Repository Research

### Files Inspected

- `feature_list.json` — entradas `dish-image-settings-toggle` (líneas 1021-1033) y
  `dish-image-progressive-ui` (992-1019): metadata, `verification` y la nota del split.
- `PROGRESS.md` — estado verificado (última aceptación: `dish-image-progressive-ui`, sesión 092;
  WIP = 0), camino de arranque `bash init.sh` y la regla de no automatizar UI de emulador.
- `docs/ai-dish-images-proposal.md` — §6 ("Guardarraíles de presupuesto: tope de generaciones por
  sesión e interruptor … mismo patrón que `AiProviderPolicy.RESPONSE_FORMAT_ENABLED`"), §8 y §10
  (fase 4) y §11 pregunta 5 ("¿Se expone un ajuste al usuario…?").
- `docs/adr/0005-ai-dish-images.md` — estado "Aceptado parcialmente (fases 1-4)"; líneas 10-11 y 174:
  el interruptor y el tope quedan **pendientes** y apuntan a **esta** feature.
- `docs/specs/dish-image-progressive-ui.md` — su "Scope Reconciliation" y su spec 7 (el mecanismo del
  tope, que este spec ahora materializa).
- `docs/specs/dish-image-cache.md`, `docs/specs/dish-image-generator.md`,
  `docs/specs/dish-image-domain.md` — contratos aceptados de fases 1-3 que **no** se tocan.
- `app/src/main/java/com/sazon/app/MainActivity.kt` (líneas 121-151) — **el punto de inyección**:
  `DishImageResolver` + `DishImageArtworkLoader` + `CompositionLocalProvider` con
  `LocalGeneratedRecipeArtworkSource provides GeneratedRecipeArtworkSource { recipe ->
  dishImageLoader.imageFor(recipe) }`.
- `app/src/main/java/com/sazon/app/ui/artwork/GeneratedRecipeArtworkSource.kt` — `fun interface`
  `suspend fun generatedFor(recipe): ImageBitmap?` + default no-op + `LocalGeneratedRecipeArtworkSource`
  (`staticCompositionLocalOf`).
- `app/src/main/java/com/sazon/app/ui/artwork/RecipeArtwork.kt` — dos `produceState` (claves
  `recipe.id`/`recipe.categoryId`), crossfade y capa base siempre pintada. **No se modifica.**
- `app/src/main/java/com/sazon/app/ui/artwork/DishImageArtworkPolicy.kt` y
  `RecipeArtworkImagePrecedence.kt` — patrón de **política en un único sitio** y de **helper puro y
  genérico** `<T : Any>` (testeable con `String`, sin Android) que replica el gate del interruptor.
- `app/src/main/java/com/sazon/app/data/ai/AiProviderPolicy.kt` — patrón `object` con constantes
  (`RESPONSE_FORMAT_ENABLED`) citado explícitamente por la propuesta §6.
- `app/src/main/java/com/sazon/app/data/image/DishImageResolver.kt` — orden **memoria → disco →
  generador → persistir**, `Mutex` por clave, `CancellationException` relanzada, fallos → `null`.
- `app/src/main/java/com/sazon/app/data/ai/image/DevExpertDishImageGenerator.kt` — adaptador real ya
  instanciado en `MainActivity` (se **envuelve**, no se edita).
- `app/src/main/java/com/sazon/app/ui/profile/ProfileScreen.kt` — **la superficie de ajustes
  reutilizada**: `LazyColumn` con `SectionHeading` (`titleMedium`) + `ProfileSectionRow` +
  secciones "Estadísticas"/"Sugerencias"/"Mis preferencias"/"Alergias e intolerancias"/"Mi cocina".
- `app/src/main/java/com/sazon/app/ui/profile/ProfileSectionRow.kt` y `ProfileSuggestionsSection.kt`
  — estilo de fila/sección: label `bodyLarge`, resumen `bodyMedium`/`onSurfaceVariant`, helper
  `labelSmall`, `SazonSpacing.md` de ritmo vertical, objetivo táctil ≥48dp.
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` (líneas 116-135 y 196-219) — el host **observa**
  fuentes (`collectAsState`) y pasa **valores** a las pantallas; ahí vive el `ProfileScreen(...)`.
- `app/src/main/java/com/sazon/app/ui/SazonApp.kt` — composición explícita (sin DI): todo entra por
  parámetro desde `MainActivity`.
- `app/src/main/java/com/sazon/app/ui/theme/Spacing.kt` — `SazonSpacing.sm/md/lg`,
  `SazonComponents.buttonHeight = 52.dp`.
- `app/build.gradle.kts` (líneas 89-118) — `testImplementation` tiene `junit`, `kotlinx-coroutines-test`,
  `ktor-client-mock`, `robolectric` y `androidx.test:core`; **no** hay test de Compose en JVM (por eso
  la lógica testeable se extrae de `@Composable`). Sin dependencias nuevas.
- `DESIGN.md` — "Generated Concept Images" (excepciones #77/#85/#86/#91/#98, el formato exacto a
  replicar), "Core Screens 7 Perfil", "Components → Imagen de receta" y "Accessibility Baseline".
- `docs/technical-discovery.md` (líneas 580-597 y 682-688) y `docs/risks-and-open-questions.md`
  (línea 144: mitigaciones **pendientes** = tope por sesión + interruptor → esta feature).
- `CONTEXT.md` — "Pantalla Perfil"/"Perfil de preferencias": ningún término nuevo (el ajuste no es una
  preferencia culinaria ni entra en la Firma de preferencias).

### Existing Patterns To Follow

- **Política en un único sitio, pura y determinista**, con constantes y topes **inyectables**
  (`AiProviderPolicy`, `DishImagePolicy`, `DishImageCachePolicy`, `DishImageArtworkPolicy`).
- **Decorador sobre el puerto del dominio** para gobernar el generador sin editar ni el puerto ni el
  adaptador real (`DishImageResolver` ya compone un `DishImageGenerator` por constructor).
- **Helper puro y genérico `<T : Any>`** para que la decisión se pruebe en JVM sin Android/Compose
  (`RecipeArtworkImagePrecedence`).
- **Estado observable como `Flow`** con `collectAsState` en la capa Compose (`NetworkStatusSource`,
  repositorios Room) — evita meter Compose en la lógica pura.
- **Capa Compose delgada** sobre microcopy puro (`SuggestionsRefreshFormat`, `RecipeCardFormat`,
  `ProfileEditState`): los textos visibles salen de un objeto puro testeado en JVM.
- **Composición explícita (sin DI)**: `MainActivity` construye y `SazonApp`/`MainShell` reparten por
  parámetro; los `CompositionLocal` se reservan a los seams de imagen.
- **Test JUnit 4** en `app/src/test/java/com/sazon/app/...`, nombres en español con `_`, sin red ni
  emulador; `runBlocking`/`kotlinx-coroutines-test`.
- **Verificación canónica** `bash init.sh` (`./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`); **nunca** `./gradlew` directo ni `./gradlew --stop`.

### Current Gaps

- **No existe ningún interruptor ni contador de generaciones**: en `data/image` y `ui/artwork` las
  únicas apariciones de "tope" son las del **disco** (80 MiB) y del **LRU** de bitmaps
  (`DishImageCachePolicy`, `DishImageArtworkPolicy`), ajenas al número de generaciones; no hay
  `budget`/interruptor/ajuste ni punto de extensión. El seam
  `LocalGeneratedRecipeArtworkSource` se provee con una lambda **sin ninguna condición** (`MainActivity`
  líneas 148-150), así que hoy la generación siempre está activa.
- **No hay persistencia ligera de preferencias fuera de Room**: no hay `SharedPreferences` ni
  `DataStore` en `app/src` (único uso de persistencia: Room: Perfil, favoritos, Lote, actividad), lo
  que hace que "persistir el interruptor" implique tocar dominio + Room (fuera de alcance).
- **No existe la sección de ajuste en Perfil**: `ProfileScreen` termina en "Mi cocina" y el KDoc dice
  que los "Ajustes" (Notificaciones, Idioma, Sobre la app) quedan para features propias.

## Technical Approach

### 1. Estado de sesión puro y observable — `data/image/DishImageSettings.kt` (nuevo)

Dos ficheros **nuevos** en `com.sazon.app.data.image` (lógica pura, sin Android ni Compose; el tope se
**envuelve**, no se edita nada aceptado):

```kotlin
// data/image/DishImageGenerationBudget.kt (puro)
class DishImageGenerationBudget(
    private val maxGenerations: Int = DishImageSettingsPolicy.MAX_GENERATIONS_PER_SESSION,
) {
    private val counter = AtomicInteger(0)          // thread-safe: la concurrencia del loader es 2
    val used: Int get() = counter.get()
    val remaining: Int get() = (maxGenerations - counter.get()).coerceAtLeast(0)
    val exhausted: Boolean get() = counter.get() >= maxGenerations
    /** Reserva una unidad; `false` si el tope está agotado (el llamante NO invoca al generador). */
    fun tryAcquire(): Boolean // CAS: nunca rebasa el tope ni con dos generaciones simultáneas
}
```

```kotlin
// data/image/DishImageSettings.kt (puro)
object DishImageSettingsPolicy {
    /** Estado inicial del interruptor: la generación viene activada (comportamiento actual). */
    const val DEFAULT_ENABLED = true
    /** Tope de generaciones por sesión; en un único sitio (patrón AiProviderPolicy). */
    const val MAX_GENERATIONS_PER_SESSION = 12
}

data class DishImageSettingsState(val enabled: Boolean, val used: Int, val max: Int) {
    val remaining: Int get() = (max - used).coerceAtLeast(0)
    val exhausted: Boolean get() = used >= max
}

class DishImageSettings(                       // uno por proceso = una sesión
    enabled: Boolean = DishImageSettingsPolicy.DEFAULT_ENABLED,
    private val budget: DishImageGenerationBudget = DishImageGenerationBudget(),
) {
    val state: StateFlow<DishImageSettingsState>          // lo que pinta Ajustes
    fun setEnabled(value: Boolean)                        // el interruptor
    /** Reserva una generación: `false` si el interruptor está apagado o el tope agotado. */
    fun tryAcquireGeneration(): Boolean
    /** Gate del interruptor en el punto de inyección: apagado NO produce (no se pide imagen). */
    suspend fun <T : Any> generatedIfEnabled(produce: suspend () -> T?): T?
}

/** Decorador del puerto: `null` sin invocar al delegado si el interruptor está apagado o el tope agotado. */
class GatedDishImageGenerator(
    private val delegate: DishImageGenerator,
    private val settings: DishImageSettings,
) : DishImageGenerator {
    override suspend fun generate(request: DishImageRequest): ByteArray? =
        if (settings.tryAcquireGeneration()) delegate.generate(request) else null
}
```

Comportamiento **vinculante**: una **unidad por invocación del generador** (intento = petición al
proveedor); `tryAcquireGeneration()` **no** reserva con el interruptor apagado (cortocircuito); el
decorador **no** captura excepciones (la `CancellationException` se propaga; la reserva previa ya
consumida es la semántica conservadora aceptada); los aciertos de memoria/disco **no** pasan por el
decorador y **no** consumen tope; el tope por defecto (12) es ajustable y cubre dos pantallas de
Resultados (6 + 6) sin exponer la cuota semanal.

### 2. Gate del interruptor en el punto de inyección — `MainActivity.kt` (modificar)

```kotlin
val dishImageSettings = DishImageSettings()          // uno por proceso = una sesión
val dishImageResolver = DishImageResolver(
    disk = FileDishImageCache(AndroidDishImageCache.root(applicationContext)),
    memory = MemoryDishImageCache(),
    generator = GatedDishImageGenerator(
        delegate = DevExpertDishImageGenerator(BuildConfig.DEVEXPERT_API_KEY, aiClient),
        settings = dishImageSettings,
    ),
)
val dishImageLoader = DishImageArtworkLoader(/* igual que hoy */)

CompositionLocalProvider(
    LocalRecipePhotoSource provides /* igual que hoy */,
    LocalGeneratedRecipeArtworkSource provides GeneratedRecipeArtworkSource { recipe ->
        // Interruptor apagado = no se pide nada (ni caché ni generación): solo la foto local.
        dishImageSettings.generatedIfEnabled { dishImageLoader.imageFor(recipe) }
    },
) { SazonApp(..., dishImageSettings = dishImageSettings) }
```

Sin red ni I/O en el arranque (igual que hoy). El interruptor **no** fuerza una re-resolución de las
tarjetas ya pintadas: se aplica a las peticiones nuevas (Decisión 5).

### 3. Reparto por parámetro — `SazonApp.kt` y `MainShell.kt` (modificar)

`SazonApp(..., dishImageSettings: DishImageSettings)` → `MainShell(..., dishImageSettings:
DishImageSettings)`; `MainShell` **observa** (`val imageGeneration by dishImageSettings.state
.collectAsState()`, patrón de `networkStatusSource`/`favoriteRepository`) y pasa a `ProfileScreen`
el estado y el callback:

```kotlin
MainTab.PROFILE -> ProfileScreen(
    /* … parámetros actuales … */
    imageGeneration = imageGeneration,
    onImageGenerationChange = { dishImageSettings.setEnabled(it) },
)
```

Se descarta un `CompositionLocal` nuevo para el ajuste: el repo lo reserva a los seams de imagen y
`SazonApp`/`MainShell` ya reparten todo por parámetro (composición explícita, sin DI).

### 4. Sección de ajuste en Perfil — `ui/profile` (2 ficheros nuevos + 1 modificado)

- `ui/profile/DishImageSettingsCopy.kt` (**nuevo, puro**): microcopy en un único sitio, testeado en
  JVM. Textos iniciales (la persona los revisa en el emulador; se registran en `DESIGN.md`):
  - `SECTION_TITLE = "Imágenes del plato"`, `TOGGLE_LABEL = "Generar imágenes del plato"`.
  - `HELPER_TEXT = "Las imágenes nuevas se generan con IA y consumen cuota; las que ya existen se
    reutilizan."`
  - `summary(state)`: activado y `remaining > 1` → `"Quedan N de 12 imágenes en esta sesión."`;
    `remaining == 1` → `"Queda 1 de 12 imágenes en esta sesión."`; `remaining == 0` → `"Tope de esta
    sesión alcanzado (12 de 12). Se reanudará al reiniciar la app."`; apagado → `"Desactivado: se usa
    solo la foto local."`
- `ui/profile/DishImageSettingsSection.kt` (**nuevo, Compose delgado**): `Text` de título
  (`typography.titleMedium`, como `SectionHeading`/`ProfileSuggestionsSection`) + una `Row`
  `fillMaxWidth().toggleable(value = enabled, role = Role.Switch, onValueChange = onToggle)
  .padding(vertical = SazonSpacing.md)` con `Column(weight(1f))` (label `bodyLarge`; resumen
  `bodyMedium`/`onSurfaceVariant`; helper `labelSmall`/`onSurfaceVariant`) y
  `Switch(checked = enabled, onCheckedChange = null)` (estado izado por la fila, accesible). Solo
  **roles del tema** y `SazonSpacing`: sin hex, sin iconos y sin dependencias nuevas (no hay
  `material-icons`; `Switch` es del `material3` ya presente). Objetivo táctil ≥48dp.
- `ui/profile/ProfileScreen.kt` (**modificar**): dos parámetros nuevos (`imageGeneration:
  DishImageSettingsState`, `onImageGenerationChange: (Boolean) -> Unit`) y un `item` al **final** del
  `LazyColumn` (tras "Mi cocina") con `DishImageSettingsSection`. Se elige el final a propósito: es un
  ajuste de app (gasto de cuota), no una preferencia culinaria, y así **no se reordena** ninguna
  sección aceptada (que la captura de Perfil y `DESIGN.md` describen).

## Expected File Changes

- `app/src/main/java/com/sazon/app/data/image/DishImageGenerationBudget.kt` — **crear**; contador
  atómico del tope (`used`/`remaining`/`exhausted`/`tryAcquire`), puro.
- `app/src/main/java/com/sazon/app/data/image/DishImageSettings.kt` — **crear**; política
  (`DEFAULT_ENABLED`, `MAX_GENERATIONS_PER_SESSION`), `DishImageSettingsState`, `DishImageSettings`
  (interruptor + `state` observable + `generatedIfEnabled`) y `GatedDishImageGenerator` (decorador del
  puerto).
- `app/src/main/java/com/sazon/app/MainActivity.kt` — **modificar**; crear el `DishImageSettings`,
  envolver el generador real con el decorador y gatear el proveedor del seam.
- `app/src/main/java/com/sazon/app/ui/SazonApp.kt` — **modificar**; parámetro `dishImageSettings`.
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — **modificar**; observa el estado y se lo pasa a
  `ProfileScreen` con el callback.
- `app/src/main/java/com/sazon/app/ui/profile/ProfileScreen.kt` — **modificar**; 2 parámetros nuevos y
  el `item` de la sección.
- `app/src/main/java/com/sazon/app/ui/profile/DishImageSettingsSection.kt` — **crear**; sección Compose
  delgada (interruptor + resumen).
- `app/src/main/java/com/sazon/app/ui/profile/DishImageSettingsCopy.kt` — **crear**; microcopy puro.
- `app/src/test/java/com/sazon/app/data/image/DishImageGenerationBudgetTest.kt` — **crear**;
  Scenarios 3 y 4 (contador y tope).
- `app/src/test/java/com/sazon/app/data/image/DishImageSettingsTest.kt` — **crear**; Scenarios 1, 2, 4,
  5, 6 y 8 (interruptor, no-consumo, acierto de caché, cancelación, decorador).
- `app/src/test/java/com/sazon/app/ui/profile/DishImageSettingsCopyTest.kt` — **crear**; Scenario 7.
- `DESIGN.md` — **modificar**; excepción de capturas (formato #77/#85/#86/#91/#98) + Core Screen 7 y/o
  "Components" con la sección "Imágenes del plato" (interruptor + tope por sesión visible).
- `docs/technical-discovery.md` — **modificar**; la cadena en runtime gana el gate del interruptor y
  el tope por sesión; tests JVM nuevos.
- `docs/adr/0005-ai-dish-images.md` — **modificar**; fase 4 **completa** (integración + ajuste de
  usuario): dejar de listar interruptor/tope como pendientes.
- `docs/risks-and-open-questions.md` — **modificar**; la fila de cuota pasa a "mitigación
  implementada" (tope por sesión + interruptor) y el pendiente queda en el set semilla (fase 7).
- `PROGRESS.md` y `feature_list.json` — **actualizar al implementar/validar** (estado y evidencia);
  **no** los toca el planner.

**No se toca**: `app/src/main/java/com/sazon/app/ui/artwork/**` (ni `RecipeArtwork` ni los 5 puntos de
uso), `domain/**`, los ficheros aceptados de `data/image/**` y `data/ai/image/**`, `ui/theme/**`,
`app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `app/src/androidTest/**`,
`AndroidManifest.xml`, la Taxonomía, `seed-recipes.json`, `assets/**`, `docs/design/concepts/**` y las
specs aceptadas.

## Visual Design Impact

- UI involved: **sí** (nueva sección de ajuste en la pantalla Perfil), pero **sin pantalla nueva**.
- Design source: `DESIGN.md` (fuente de verdad de tokens y reglas: typography, `SazonSpacing`,
  Accessibility Baseline) + el **estilo de sección ya aceptado** de la pantalla Perfil (captura
  aceptada `docs/design/concepts/activity-summary/perfil-resumen/`, sección "Estadísticas", y el
  patrón de `ProfileSuggestionsSection`). Backfill pendiente de referencia: `profile-screen`
  (`perfil`, `perfil-editar`) y `manual-suggestions-refresh` (`perfil-sugerencias`), ver
  `docs/design/concepts/profile-screen/README.md`.
- Screens or states affected: pestaña **Perfil** (solo se **añade** una sección al final); estados de
  la sección: interruptor **activado con tope disponible**, **activado con tope alcanzado** y
  **desactivado** (y claro/oscuro por roles del tema).
- New design artifact required: **no**.
- **Capturas de UI — DESVIACIÓN AUTORIZADA por la persona (2026-10-13; ya anotada en la entrada
  `dish-image-settings-toggle` de `feature_list.json`, misma clase que #77/#85/#86/#91/#98): no se crea
  `docs/design/concepts/dish-image-settings-toggle/` ni su `README.md`.** Motivo: la feature **no**
  introduce pantalla, navegación, layout, geometría ni estilo nuevos; **reutiliza el estilo de ajustes
  existente** de la pantalla Perfil (título `titleMedium`, fila label+resumen, tokens del tema) y solo
  añade un `Switch` de `material3` y una línea de microcopy con el tope. La dirección visual aplicable
  es `DESIGN.md` + la captura aceptada del Perfil (`activity-summary/perfil-resumen`) + el backfill
  pendiente `profile-screen`, contra las que la persona revisa el acabado en el emulador tras el merge.
- **Obligatorio**: el implementer registra en `DESIGN.md → "Generated Concept Images"` un párrafo de
  excepción con el **mismo formato** que las de #77/#85/#86/#91/#98 (feature + issue + motivo +
  dirección visual aplicable + "no se crea carpeta"), usando el **número de issue que `github-flow`
  haya asignado a esta feature** (el hermano fue #98), y describe la sección en Core Screens 7
  (Perfil) y/o en "Components": sección **"Imágenes del plato"** con interruptor
  ("Generar imágenes del plato") y tope por sesión visible ("Quedan N de 12…"), sustituyendo el
  genérico "y ajustes" por este contenido real.
- Nota de gate: la desviación está autorizada, así que el gate de capturas queda cubierto por el
  registro en `DESIGN.md`; el `planner` **no** crea carpetas fuera de `docs/specs/` y comprueba que
  `docs/design/concepts/dish-image-settings-toggle/` **no existe**.

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — la cadena de imagen en runtime (líneas 580-597) gana el
  **gate del interruptor** en el proveedor del seam y el **tope de generación por sesión** (decorador
  del puerto, `data.image`); bullet de tests JVM nuevos (sección de tests, junto a los de `ui.artwork`).
- `docs/adr/0005-ai-dish-images.md`: **update** — la fase 4 queda **completa** (integración +
  ajuste de usuario); se retiran de "pendientes" el interruptor y el tope por sesión y se documenta
  la semántica (una unidad por petición al proveedor, sesión = proceso, sin persistencia).
- `DESIGN.md`: **update** — excepción de capturas (desviación documentada) + registro de la sección
  "Imágenes del plato" y su microcopy.
- `docs/risks-and-open-questions.md`: **update** — la mitigación del gasto de cuota pasa a
  **implementada** (caché + concurrencia 2 + cancelación + **tope por sesión** + **interruptor**); el
  único pendiente asociado sigue siendo el set semilla (fase 7).
- `docs/domain-model.md`: **not needed** — no hay entidades, relaciones ni estados nuevos: el ajuste
  **no** es un campo de `UserProfile` ni entra en la Firma de preferencias (Decisión 2).
- `AGENTS.md`: **not needed** — no cambian flujo, arranque ni reglas de trabajo.
- `docs/design/concepts/`: **not needed** — desviación autorizada (arriba); no se crea carpeta.
- `docs/specs/profile-screen.md`: **not needed** — spec aceptada; su Non-Goal sobre "Ajustes"
  (Notificaciones/Idioma/Sobre) sigue vigente y esta feature **no** lo implementa.
- `docs/ai-dish-images-proposal.md`: **not needed** — documento de propuesta; `feature_list.json` y el
  ADR son la fuente de verdad del estado.

## Implementation Plan

1. Crear `DishImageGenerationBudget` (contador atómico, tope inyectable) y su test.
2. Crear `DishImageSettings` + `DishImageSettingsState` + `DishImageSettingsPolicy` +
   `GatedDishImageGenerator` (puros) y su test (interruptor, no-consumo, tope, acierto de caché,
   cancelación).
3. Crear `DishImageSettingsCopy` (microcopy puro) y su test; crear `DishImageSettingsSection` (Compose
   delgado) con `Switch` + `toggleable(role = Role.Switch)` reutilizando tokens/ritmo del Perfil.
4. Añadir a `ProfileScreen` los dos parámetros y el `item` de la sección al final del `LazyColumn`.
5. Pasar el `DishImageSettings` por `SazonApp` y `MainShell` (observar con `collectAsState`) y
   cablearlo en `MainActivity`: decorador del generador + gate del proveedor del seam.
6. Inspeccionar pureza/alcance (greps del Verification Plan) y que los 5 puntos de uso y `ui/artwork`
   están sin diff.
7. Actualizar `DESIGN.md` (excepción + sección), `docs/technical-discovery.md`, el ADR 0005 y
   `docs/risks-and-open-questions.md`.
8. Ejecutar `bash init.sh` en verde, registrar evidencia y dejar la feature `passing` (la validación
   independiente es de otro rol).

## Implementation Tasks

- [ ] Crear `data/image/DishImageGenerationBudget.kt` (`MAX_GENERATIONS_PER_SESSION` inyectable,
      `used`/`remaining`/`exhausted`, `tryAcquire()` atómico) y `DishImageGenerationBudgetTest`.
- [ ] Crear `data/image/DishImageSettings.kt`: `DishImageSettingsPolicy` (`DEFAULT_ENABLED = true`,
      `MAX_GENERATIONS_PER_SESSION = 12`), `DishImageSettingsState`, `DishImageSettings`
      (`state: StateFlow`, `setEnabled`, `tryAcquireGeneration`, `generatedIfEnabled` genérico) y
      `GatedDishImageGenerator` (decorador del puerto, cancelación propagada).
- [ ] Escribir `DishImageSettingsTest` (interruptor apagado = 0 peticiones, encendido = 1; apagado no
      consume tope; apagar/encender no resetea; decorador no invoca al delegado con el tope agotado;
      estado observable; acierto de caché con `DishImageResolver` real + fakes no consume tope;
      `CancellationException` propagada) — casos negativos que no pasan en vacío.
- [ ] Crear `ui/profile/DishImageSettingsCopy.kt` + `DishImageSettingsCopyTest` (singular/plural,
      tope alcanzado, desactivado, determinismo).
- [ ] Crear `ui/profile/DishImageSettingsSection.kt` (título `titleMedium`; fila
      `toggleable(role = Role.Switch)`; `Switch(onCheckedChange = null)`; label `bodyLarge`; resumen
      `bodyMedium`/`onSurfaceVariant`; helper `labelSmall`; `SazonSpacing.md`; ≥48dp; sin hex).
- [ ] Añadir a `ProfileScreen` `imageGeneration: DishImageSettingsState` y
      `onImageGenerationChange: (Boolean) -> Unit` + el `item` al final del `LazyColumn`.
- [ ] `SazonApp`: parámetro `dishImageSettings`; `MainShell`: `state.collectAsState()` y pasar estado +
      callback a `ProfileScreen`.
- [ ] `MainActivity`: `DishImageSettings()` por proceso, `GatedDishImageGenerator` envolviendo
      `DevExpertDishImageGenerator`, y el proveedor del seam gateado con `generatedIfEnabled`.
- [ ] Verificar por inspección: firma/geometría de `RecipeArtwork` y los 5 puntos de uso; `ui/artwork`
      sin diff; ficheros nuevos de `data.image` sin `android.*`/`androidx.*`; sin dependencias nuevas;
      `docs/design/concepts/dish-image-settings-toggle/` no existe.
- [ ] Actualizar `DESIGN.md`, `docs/technical-discovery.md`, `docs/adr/0005-ai-dish-images.md` y
      `docs/risks-and-open-questions.md`.
- [ ] Ejecutar `bash init.sh` (test + lint) en verde, registrar la evidencia y actualizar
      `feature_list.json` (a `passing`, **sin tocar su `verification`**) y `PROGRESS.md`.

## Verification Plan

- `bash init.sh` → **exit 0**. Ejecuta el gate no bloqueante estándar (`./gradlew test` + `./gradlew
  lint` vía `scripts/gradle.sh`), **no** arranca procesos largos y **no** modifica `init.sh`.
- Tiradas focales:
  - `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.image.*"` →
    `BUILD SUCCESSFUL` (las 5 suites aceptadas de la fase 3 + las 2 nuevas, sin regresión).
  - `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.profile.*"` →
    `BUILD SUCCESSFUL` (`DishImageSettingsCopyTest` + las existentes `ProfileEditStateTest`,
    `SuggestionsRefreshFormatTest`).
- Parseo propio de los XML (`app/build/test-results/testDebugUnitTest/TEST-*.xml`): **0 fallos, 0
  errores, 0 saltados**. Baseline de referencia: **81 suites / 562 tests** (validación de
  `dish-image-progressive-ui`, sesión 092); el implementer **re-deriva** el número exacto en su HEAD
  (`git ls-tree` + `git grep -c '@Test'`) y confirma el delta: **+3 suites**
  (`DishImageGenerationBudgetTest`, `DishImageSettingsTest`, `DishImageSettingsCopyTest`) y
  **≈ +18-19 tests**.
- `bash scripts/gradle.sh lint` → **0 errores**; sin warnings nuevos bajo `app/src` (los
  `GradleDependency`/`NewerVersionAvailable`/`AndroidGradlePluginVersion`/`OldTargetApi` son
  preexistentes y su recuento varía con la metadata de versiones del momento).
- Mapeo explícito del `verification` de la feature:
  - "el interruptor apagado desactiva la generación (no se pide ninguna imagen)" →
    `DishImageSettingsTest` Scenario 1 (productor contador → 0 invocaciones).
  - "encendido la permite" → `DishImageSettingsTest` Scenario 2 (1 invocación).
  - "el tope por sesión se respeta (patrón `AiProviderPolicy`)" → `DishImageGenerationBudgetTest` +
    `DishImageSettingsTest` (Scenarios 3, 4 y 6: decorador que no invoca al delegado agotado,
    apagar/encender no resetea, acierto de caché sin consumo).
  - "desviación de capturas autorizada … registrada en `DESIGN.md`" → Scenario 9 (inspección: no
    existe la carpeta y el párrafo está en `DESIGN.md`).
  - "prueba manual en emulador" → prueba de la persona tras el merge (abajo).
- Inspección de pureza y alcance:
  - `grep -rn "import android\|import androidx" app/src/main/java/com/sazon/app/data/image/DishImageGenerationBudget.kt
    app/src/main/java/com/sazon/app/data/image/DishImageSettings.kt` → **0** (puros; solo `AtomicInteger`
    de `java.util.concurrent` y `kotlinx.coroutines.flow`).
  - `git diff --name-only` solo con los ficheros previstos; **0** cambios en `ui/artwork/**`,
    `data/image/DishImageResolver.kt` y los demás aceptados de `data/image`, `data/ai/image/**`,
    `domain/**`, `ui/theme/**`, build/lockfile, `init.sh` y `scripts/**`.
  - `grep -rn "RecipeArtwork(" app/src/main/java` → 5 puntos de uso con la misma geometría; firma
    intacta.
  - `test -d docs/design/concepts/dish-image-settings-toggle` → **no existe**; `DESIGN.md` con el
    párrafo de excepción con el formato de #98.
  - `grep -rn "Switch\|toggleable" app/src/main/java/com/sazon/app/ui/profile` → solo en la sección
    nueva; **0** hex sueltos (solo `MaterialTheme.colorScheme.*`).
- Cobertura **E2E**: **no aplica**. El repo no tiene suite E2E persistente y AGENTS.md no automatiza
  UI de emulador; la cobertura canónica es el test JVM (todo el estado en `data.image`) y la revisión
  visual/manual de la persona.
- **Prueba manual (persona, tras el merge; no automatizable desde WSL)**: en el emulador, con clave
  válida, abrir varias pantallas con recetas para que se generen imágenes y (a) leer en Perfil → "Imágenes del
  plato" el tope restante ("Quedan N de 12…"), comprobando que **baja** al aparecer imágenes nuevas;
  (b) **apagar** el interruptor y verificar que las recetas nuevas/el scroll ya **no** generan (se
  mantiene la foto local y el proveedor no recibe peticiones); (c) **encender** y comprobar que se
  reanuda; (d) agotar el tope (o inyectar uno pequeño si se quiere una prueba rápida) y verificar que
  al llegar a 0 las recetas nuevas se quedan con la foto local y el texto lo anuncia; (e) reiniciar la
  app y ver el contador de nuevo en 12 (sesión = proceso); (f) revisar **claro y oscuro** el
  interruptor y el resumen, contra `DESIGN.md` y la captura aceptada del Perfil.
- Nota operativa: usar `bash init.sh` o `bash scripts/gradle.sh <tarea>`; **nunca** `./gradlew`
  directo ni `./gradlew --stop` (AGENTS.md, Notas de entorno).

## Evidence To Capture

- Salida de `bash init.sh` con código 0 y de las tiradas focales (`data.image.*`, `ui.profile.*`).
- Conteo parseado del XML (suites/tests, 0 fallos/errores/saltados) con las 3 suites nuevas nombradas
  y la baseline re-derivada en HEAD.
- Informe de lint (0 errores, ninguno nuevo bajo `app/src`).
- Vectores concretos de los tests: `enabled = false` → `generatedIfEnabled` devuelve `null` con **0**
  invocaciones del productor y `used == 0`; `enabled = true` → **1**; con `max = 2`, `tryAcquire()` →
  `true,true,false` y `used == 2`, `remaining == 0`, `exhausted == true`; con `max = 0` ninguna reserva
  pasa; apagar/encender tras agotar → sigue `exhausted`; `state.value` pasa de
  `(true,0,12)` a `(true,1,12)`; resolución doble de la misma clave → generador invocado **1** vez y
  `used == 1`; `CancellationException` propagada.
- Resultado de los greps de inspección (pureza de los nuevos ficheros de `data.image`, `ui/artwork` sin
  diff, 5 puntos de uso intactos, sin dependencias nuevas, `docs/design/concepts/dish-image-settings-toggle/`
  inexistente) y del `git diff --stat`.
- Confirmación de que el implementer **no** hizo commit, push ni PR (los hace el orquestador).

## Decisions And Unknowns

### Decisiones cerradas (el implementer no las reabre)

1. **El ajuste se gobierna en dos puntos, no en uno**: (a) el **interruptor** se comprueba en el
   **punto de inyección** (proveedor de `LocalGeneratedRecipeArtworkSource` en `MainActivity`), porque
   apagado debe impedir **también** servir una imagen generada ya cacheada ("con la generación apagada,
   la app usa solo la foto local"); (b) el **tope** se impone en el **borde de generación**
   (`GatedDishImageGenerator` sobre el puerto), porque es donde se puede gastar cuota y porque los
   aciertos de memoria/disco **no** deben consumir tope. Un único punto no puede cumplir ambas cosas.
2. **El interruptor y el tope son de sesión (en memoria), no persistidos**. Justificación: el
   `user_visible_behavior` habla de "el tope por sesión"; la sesión es la **vida del proceso** (igual
   que la reconciliación del hermano); el repo **no** tiene persistencia ligera de preferencias
   (ni `SharedPreferences` ni `DataStore`: solo Room) y persistir exigiría un campo nuevo en
   `UserProfile` + migración de Room, que es **otro** alcance (dominio + datos) y no lo pide la
   feature (`area: ui`, inyección en `MainActivity`). Consecuencia documentada: al reiniciar la app el
   interruptor vuelve a **activado** (comportamiento actual) y el contador a 12. Si la persona quiere
   persistencia, se abre una **feature correctiva** (dominio + Room); no se mezcla aquí.
3. **Constantes en un único sitio** (patrón `AiProviderPolicy`): `DishImageSettingsPolicy`
   (`DEFAULT_ENABLED = true`, `MAX_GENERATIONS_PER_SESSION = 12`). El tope por defecto (12) es una
   primera estimación (cubre dos pantallas de Resultados) y se cambia en ese único punto.
4. **Semántica de consumo**: una unidad por **invocación del generador** (intento = petición al
   proveedor), reserva estricta **antes** de invocar y `null` sin invocar al delegado cuando está
   agotado. Un **fallo** del proveedor consume la unidad (es donde se paga) y una generación
   **cancelada** también (la reserva precede a la llamada). Se documenta como guardarraíl de
   presupuesto; el refinamiento (reembolsar fallos) queda como posible correctiva.
5. **El interruptor no fuerza re-resolución ni cancelación** de las tarjetas ya pintadas: el
   `produceState` de `RecipeArtwork` está claveado por `recipe.id`/`categoryId`, así que un cambio de
   ajuste se aplica a las **peticiones nuevas** (nueva tarjeta, scroll, navegación). Es lo que pide la
   verificación ("apagar deja de generar imágenes **nuevas**"); recargar el árbol al cambiar el ajuste
   sería un comportamiento nuevo, no pedido, y tocar `RecipeArtwork` (prohibido).
6. **El decorador se llama `GatedDishImageGenerator` y recibe el `DishImageSettings`** (no un
   `DishImageGenerationBudget` suelto como esbozaba `docs/specs/dish-image-progressive-ui.md` §7): así
   el estado **observable** que pinta Ajustes y el tope **aplicado** por el decorador son la **misma**
   fuente (una reserva re-emite el estado) y no hay dos contadores que puedan divergir. El contador
   atómico sigue aislado y testeado en `DishImageGenerationBudget`.
7. **Sin reset manual y sin tope visible "ajustable"**: el usuario **ve** el tope (lo pide el
   `user_visible_behavior`) pero no lo cambia ni lo reinicia desde la UI; el reset es el reinicio del
   proceso. `reset()` **no** se añade (evitar API muerta) y apagar/encender **no** reinicia el tope
   (si lo hiciera, el tope sería trivialmente esquivable).
8. **La sección vive al final del Perfil** y con el estilo de sección existente (`titleMedium` +
   fila + helper), con un `Switch` de `material3` y `toggleable(role = Role.Switch)` para que el estado
   no dependa **solo** del color (Accessibility Baseline). Sin iconos (el repo no tiene
   `material-icons`) y sin dependencias nuevas.
9. **Degradación silenciosa**: interruptor apagado, tope agotado o fallo → **foto local**, nunca error
   de UI ni hueco (contrato del ADR 0005); la `CancellationException` se propaga (no se traga).
10. **Sin dependencias nuevas y `init.sh` sin cambios**: la lógica testeable se extrae de
    `@Composable` (no hay test de Compose en `testImplementation`) y el microcopy sale de un objeto
    puro.

### Unknowns y riesgos (no bloquean, quedan anotados)

- **Valor del tope (12)**: primera estimación; si en la demo se queda corto o largo, se ajusta en
  `DishImageSettingsPolicy` (un único sitio) sin tocar la costura.
- **Sesión sin red agota el tope con fallos rápidos**: con el interruptor encendido y sin red, cada
  intento consume unidad; al volver la red puede no quedar presupuesto hasta reiniciar. Aceptado
  (guardarraíl conservador) y documentado en el ADR 0005/riesgos; el refinamiento es una correctiva.
- **El interruptor no "vacía" imágenes ya pintadas**: al apagarlo, las tarjetas ya compuestas que ya
  mostraban la generada la mantienen hasta recomponerse/destruirse. Es coherente con la Decisión 5 y
  con la prueba manual ("deja de generar imágenes nuevas").
- **Microcopy exacto y acabado del `Switch`** (tamaño, espaciado, claro/oscuro, tipografía ampliada):
  lo revisa la persona en el emulador contra `DESIGN.md`; un defecto se corrige con una **feature
  correctiva** (no bloquea el flujo autónomo).
- **Número de issue para el párrafo de `DESIGN.md`**: lo asigna `github-flow` al provisionar la
  feature (el hermano fue #98). Si no estuviera disponible en la sesión del implementer, se usa el
  número que figure en la branch/issue del worktree; el formato del párrafo es el de #98.
- **Los tests de concurrencia del contador** deben ser deterministas (N reservas con tope `k < N` y
  aserción de exactamente `k` éxitos), no basados en reposos ni en temporizadores.
- **`state: StateFlow` obliga a no bloquear** en la capa UI: el `Switch` dispara `setEnabled` (no
  suspendente) y el consumo lo notifica el propio decorador al reservar; no hay polling ni reloj.

## Validator Checklist

- [ ] Alcance: solo `data/image/{DishImageGenerationBudget,DishImageSettings}.kt` (nuevos),
      `MainActivity.kt`, `ui/SazonApp.kt`, `ui/MainShell.kt`, `ui/profile/{ProfileScreen,DishImageSettingsSection,DishImageSettingsCopy}.kt`
      (2 nuevos + 1 modificado), los 3 tests nuevos y las 4 docs previstas; **sin** tocar
      `ui/artwork/**`, los 5 puntos de uso, los ficheros aceptados de `data/image`/`data/ai/image`,
      `domain/**`, `ui/theme/**`, build, lockfile, `init.sh` ni dependencias.
- [ ] Interruptor **apagado** = **0 peticiones** de imagen (no se lee caché ni se genera) y se usa
      solo la foto local; **encendido** = se permite (Scenarios 1 y 2, verificación exigida).
- [ ] Tope **por sesión** respetado: constante en un único sitio (patrón `AiProviderPolicy`), reserva
      atómica, `null` **sin invocar** al delegado cuando está agotado, aciertos de caché **sin**
      consumo, apagar/encender **no** lo reinicia (Scenarios 3, 4 y 6).
- [ ] El estado observable (`enabled`/`used`/`remaining`/`max`/`exhausted`) alimenta la sección de
      Perfil y el resumen visible es puro y determinista, con singular/plural correcto (Scenarios 5
      y 7).
- [ ] Degradación silenciosa ante fallo/`null` y `CancellationException` propagada (Scenario 8);
      ninguna excepción de imagen llega a la UI.
- [ ] `RecipeArtwork` conserva la firma `(recipe, modifier, glyphSize)` y la geometría aceptada; el
      resto de `ui/artwork` y los 5 puntos de uso están **sin diff** (Scenario 9).
- [ ] Los ficheros nuevos de `data.image` son **puros** (sin `android.*`/`androidx.*`, sin reloj ni
      azar) y testeables en JVM; sin dependencias nuevas.
- [ ] La sección de Perfil reutiliza tokens/estilo existentes (roles del tema, `SazonSpacing`,
      tipografía, ≥48dp, `role = Role.Switch`, sin depender solo del color) y **no** crea pantalla ni
      navegación (Visual Design Impact).
- [ ] `docs/design/concepts/dish-image-settings-toggle/` **no** existe y `DESIGN.md` registra la
      excepción de capturas con el formato de #77/#85/#86/#91/#98 más la sección "Imágenes del plato"
      (verificación exigida).
- [ ] `docs/technical-discovery.md`, `docs/adr/0005-ai-dish-images.md` y
      `docs/risks-and-open-questions.md` están actualizados y coherentes (fase 4 completa; mitigación
      de cuota implementada; pendiente solo el set semilla).
- [ ] Escenarios 1-9 cubiertos por tests JVM (incluidos los negativos: apagado, tope agotado,
      cancelación, apagado+encendido) o justificados por inspección/prueba manual según AGENTS.md.
- [ ] `feature_list.json` y `PROGRESS.md` reflejan estado y evidencia; el `verification` de la feature
      queda **intacto** y el implementer no hizo commit, push ni PR.
