# Feature Implementation Spec: El interruptor "Generar imágenes del plato" recuerda su estado entre reinicios

## Source Feature

- `id`: `dish-image-settings-persistence`
- `area`: `ui` (el efecto visible es el ajuste del Perfil), aunque el cambio técnico vive en
  `data.image` + el punto de composición
- `depends_on`: `["dish-image-settings-toggle", "dish-image-settings-default-off"]` — **ambas
  satisfechas** (`accepted`, sesiones 093 y 124)
- `status`: `not_started` (en el momento de planificar)
- `source`: `feature_list.json` (entrada líneas 1537-1554)
- Verificación exigida (textual de la feature):
  1. "Test JVM: guardar el estado encendido y releerlo devuelve encendido; guardar apagado y
     releerlo devuelve apagado; sin estado guardado se usa el default de la politica (apagado)"
  2. "Test JVM: al arrancar con el estado persistido encendido el gate del interruptor pide
     imagenes (invoca al productor); con el estado persistido apagado no las pide (0 invocaciones)"
  3. "Test JVM: el tope de generaciones por sesion no se persiste; arranca en 12 aunque el estado
     guardado este encendido"
  4. "'bash init.sh' sigue en verde y sin dependencias nuevas"
- Decisión de la persona (2026-10-15, en `notes` de la entrada): se persiste **solo** el estado
  encendido/apagado del interruptor; el **tope** sigue siendo por sesión; el estado persistido es el
  que decide si se llama a la API de imágenes; la **semántica del gate no cambia** (apagado = no se
  llama a la API ni se lee caché/semilla; encendido = cadena normal de resolución).

## Goal

Que el **estado del interruptor** "Generar imágenes del plato" sobreviva al reinicio de la app: si
el usuario lo enciende y cierra la app, al volver a abrirla sigue encendido y la app pide imágenes
por la cadena ya aceptada; si lo apaga, sigue apagado y no pide nada (ni API, ni caché, ni semilla).

La persistencia es **de un solo bit** y se resuelve con una **costura pura nueva**
(`DishImageSettingsStore`) más un **adaptador `SharedPreferences`** detrás de ella. Se **restaura de
forma síncrona** en el punto de composición (`MainActivity`, antes de `setContent`), de modo que el
**primer frame** ya usa el estado guardado (sin parpadeo ni una primera decisión equivocada del
gate). Nada más cambia: el **tope por sesión** (12) sigue arrancando en 12, `generatedIfEnabled`,
`GatedDishImageGenerator`, la cadena de resolución, la caché, la UI del Perfil y la semántica del
gate quedan **intactos**.

## Non-Goals

- **No se persiste el tope** (ni `used`, ni el contador, ni "cuántas quedan"): `DishImageGenerationBudget`
  sigue siendo **de sesión** (vida del proceso) y arranca en `MAX_GENERATIONS_PER_SESSION = 12`.
  La costura nueva expone **solo** un booleano (garantía estructural, no solo una promesa).
- **No se toca `UserProfile` ni Room**: el ajuste **no** entra en `UserProfile`/`ProfileRepository` ni
  en la **Firma de preferencias** (`PreferenceSignature`), así que **no** fuerza refresco del Lote.
  Tampoco se añade tabla/entidad/DAO/base de datos nueva.
- **No se toca la cadena de resolución ni el gate**: `DishImageResolver`, `FileDishImageCache`,
  `MemoryDishImageCache`, `AndroidDishImageCache`, `AssetDishImageSeedSource`, `DishImagePayload`,
  `DevExpertDishImageGenerator`, el puerto `DishImageGenerator`, `DishImageRequest`, `DishImageCacheKey`,
  `DishImageArtworkLoader`, `DishImageArtworkPolicy`, `ui/artwork/**` (incluido `RecipeArtwork` y sus
  puntos de uso), `GatedDishImageGenerator` y `DishImageSettings.generatedIfEnabled` quedan **sin diff**.
  La semántica **apagado = `null` sin leer caché, semilla ni API** no cambia (decisión de la persona).
- **No se toca la UI**: `DishImageSettingsSection`, `DishImageSettingsCopy`, `ProfileScreen`,
  `MainShell`, `SazonApp` y `ui/theme/**` quedan **sin diff** (ya derivan de `DishImageSettingsState`);
  no hay pantalla, layout, geometría, microcopy ni estados nuevos.
- **No cambia el default de arranque**: `DishImageSettingsPolicy.DEFAULT_ENABLED = false` sigue igual:
  sin estado guardado (primer arranque, datos borrados, instalación nueva) la app arranca **apagada**.
- **No hay dependencias nuevas**: nada de `androidx.datastore` ni de Room nuevo; no se toca
  `app/build.gradle.kts`, `gradle/libs.versions.toml`, `AndroidManifest.xml`, `init.sh`, `scripts/**`.
- **No se persiste nada más**: ni el descriptor visual (`DishVisualDescriptorStore`), ni el sumidero de
  origen (`DishImageOriginSink`), ni la caché de imágenes (que ya tiene su propia política de ficheros).
- **No hay migración ni acción de usuario nueva**: no hay "restaurar ajustes", ni limpiar caché, ni
  reset del tope; el ajuste solo se escribe al mover el interruptor.
- No se tocan las specs aceptadas (`docs/specs/dish-image-settings-toggle.md`,
  `docs/specs/dish-image-settings-default-off.md`): son contratos históricos; su Decisión 2
  (interruptor de sesión, sin persistencia) queda **superada** por esta feature, y eso se registra en
  el ADR 0005 y en `docs/technical-discovery.md`.

## Job Story

Cuando enciendo (o apago) el interruptor "Generar imágenes del plato" en mi Perfil y vuelvo a abrir
la app más tarde,
quiero que la app recuerde mi última elección,
para no tener que volver a configurarla cada vez y para que el gasto de cuota siga siendo mi
decisión explícita (sin sorpresas en ningún sentido).

## Users And Permissions

- **Usuario local** (único dueño del dispositivo, ADR 0002): mueve el interruptor en **Perfil →
  "Imágenes del plato"**; tras reiniciar la app ve el interruptor en el último estado elegido y, con
  él encendido, el resumen del **tope de la sesión** en 12 de 12 recién arrancado.
- **Desarrollador / agente**: ejercita guardar/releer, el arranque restaurado y el no-persistir el
  tope en **JVM** (costura pura + un test Robolectric del adaptador), sin red, sin clave, sin cuota y
  sin emulador.
- **Persona**: revisa en el emulador tras el merge que encender/apagar sobrevive al reinicio y que el
  acabado visual no cambia (no hay nada visual nuevo que revisar); `DESIGN.md` es la fuente de verdad
  visual (AGENTS.md no automatiza UI de emulador).

## Acceptance Scenarios

### Scenario 1: encendido sobrevive al reinicio y pide imágenes (verificación exigida 1 y 2)

Given un store con `true` guardado (el usuario encendió el interruptor y el proceso murió),
When se arranca (`DishImageSettings.restored(store)`) y se pide una imagen por
`generatedIfEnabled { … }`,
Then `state.value == DishImageSettingsState(enabled = true, used = 0, max = 12)` y el productor se
invoca **1** vez (la cadena normal de resolución queda habilitada).

### Scenario 2: apagado sobrevive al reinicio y no pide nada (verificación exigida 1 y 2)

Given un store con `false` guardado,
When se arranca y se pide una imagen,
Then `state.value.enabled == false`, `generatedIfEnabled` devuelve `null` con **0** invocaciones del
productor (no se lee caché, ni semilla, ni API) y `tryAcquireGeneration()` devuelve `false` sin
consumir tope.

### Scenario 3: sin estado guardado se usa el default de la política (verificación exigida 1)

Given un store **vacío** (`loadEnabled() == null`),
When se arranca,
Then el interruptor queda **apagado** (`DEFAULT_ENABLED = false`, opt-in) y el contador en 12 de 12,
sin escribir nada en el store.

### Scenario 4: guardar y releer el estado (verificación exigida 1)

Given un store compartido entre dos instancias y un ajuste arrancado,
When se llama a `setEnabled(true)` y después se construye un `DishImageSettings` nuevo sobre el
mismo store, Then el nuevo ajuste lee `enabled = true`; When se llama a `setEnabled(false)` y se
relee, Then lee `enabled = false`. El valor persistido es siempre el **último** movido por el
usuario.

### Scenario 5: el tope por sesión no se persiste (verificación exigida 3)

Given un store con `true` guardado y un ajuste que ya consumió unidades en una sesión anterior
(proceso muerto),
When se arranca de nuevo,
Then `used == 0`, `max == 12`, `remaining == 12` y `exhausted == false` **aunque** el estado
guardado esté encendido; y la costura del store **no** ofrece ninguna operación de tope (solo
`loadEnabled`/`saveEnabled`), de modo que el tope no puede persistirse por construcción.

### Scenario 6: solo el interruptor se escribe, y solo al moverlo

Given un store que cuenta escrituras,
When se llama a `setEnabled(true)` y a `tryAcquireGeneration()`,
Then el store recibe **1** escritura (`true`) y el consumo de tope **no** genera ninguna escritura;
When después se llama a `setEnabled(false)`, Then el store recibe la escritura `false` (2 en total).

### Scenario 7: degradación silenciosa del almacenamiento

Given un store cuyo `loadEnabled()` lanza (o cuyo valor guardado está corrupto: el adaptador lee un
tipo distinto de `Boolean`),
When se arranca, Then el ajuste cae al default de la política (**apagado**) sin crash ni error de
UI. Given un store cuyo `saveEnabled()` lanza, When el usuario mueve el interruptor, Then el estado
**en memoria** cambia igual (la sesión se comporta como hoy) y la excepción **no** llega a la UI.

### Scenario 8: alcance, pureza y semántica intacta (inspección)

Given la implementación terminada,
When se inspecciona el diff,
Then solo cambian `data/image/DishImageSettings.kt`, los 2 ficheros nuevos de persistencia en
`data/image` (costura pura + adaptador), `MainActivity.kt` (1 línea de composición + comentario), los
2 tests nuevos y las 4 docs previstas; **no** hay diff en `domain/**`, `ui/**` (incluidas `ui/profile/**`
y `ui/artwork/**`), en los ficheros aceptados de `data/image`/`data/ai/image`, en
`DishImageGenerationBudget.kt`/`DishImageSettingsCopy.kt`, build/lockfile, `AndroidManifest.xml`,
`init.sh` ni `scripts/**`; los ficheros nuevos puros no importan `android.*`/`androidx.*` (solo el
adaptador importa `android.content`); `DishImageSettingsPolicy.DEFAULT_ENABLED == false` y
`MAX_GENERATIONS_PER_SESSION == 12` siguen igual; y no hay dependencias nuevas.

## Repository Research

### Files Inspected

- `feature_list.json` (líneas 1537-1554) — entrada de la feature: `area: ui`,
  `depends_on: ["dish-image-settings-toggle", "dish-image-settings-default-off"]`, `status:
  not_started`, `verification` (4 puntos, arriba) y `notes` (alcance exacto y no-goals).
- `app/src/main/java/com/sazon/app/data/image/DishImageSettings.kt` — **el fichero que cambia**:
  `DishImageSettingsPolicy` (`DEFAULT_ENABLED = false`, `MAX_GENERATIONS_PER_SESSION = 12`),
  `DishImageSettingsState`, `DishImageSettings(enabled, budget)` (`state`, `setEnabled`,
  `tryAcquireGeneration`, `generatedIfEnabled`) y `GatedDishImageGenerator`. El KDoc dice
  explícitamente "**No** se persiste (Decisión 2 del spec)": queda **desfasado** con esta feature.
- `app/src/main/java/com/sazon/app/data/image/DishImageGenerationBudget.kt` — contador atómico del
  tope, con `maxGenerations` inyectable; **no se toca** (permite el caso `max = 2` en tests).
- `app/src/main/java/com/sazon/app/MainActivity.kt` (línea 148 y comentario de las líneas 137-147) —
  **único punto de composición**: `val dishImageSettings = DishImageSettings()` (un ajuste por
  proceso) y el proveedor del seam gateado con
  `dishImageSettings.generatedIfEnabled { dishImageLoader.imageFor(recipe) }` (~línea 208). El
  comentario dice "no se persiste" y hay que actualizarlo.
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` (líneas 113, 146-147, 241) — observa
  `dishImageSettings.state` con `collectAsState()`, reparte `imageBackground = imageGeneration.enabled`
  y el callback `onImageGenerationChange = { dishImageSettings.setEnabled(it) }`. **No se toca**: con
  el estado restaurado, reparto y UI salen correctos solos.
- `app/src/main/java/com/sazon/app/ui/SazonApp.kt` (línea 52) — pasa el ajuste por parámetro
  (composición explícita, sin DI). **No se toca.**
- `app/src/main/java/com/sazon/app/ui/profile/{DishImageSettingsSection,DishImageSettingsCopy,ProfileScreen}.kt`
  — la sección refleja `state.enabled` y el resumen se deriva de `DishImageSettingsState`
  (`DISABLED_SUMMARY` con el interruptor apagado; textos del tope "…de 12 imágenes en esta sesión",
  que siguen siendo ciertos porque el tope **sigue siendo** de sesión). **No se tocan.**
- `app/src/test/java/com/sazon/app/data/image/DishImageSettingsTest.kt` — 17 `@Test`, **16**
  construcciones del ajuste (`DishImageSettings()` / `DishImageSettings(enabled = …, budget = …)`): con
  el diseño de este spec (param `enabled: Boolean` con default + factory `restored(store)`) **ninguna
  se rompe ni cambia**. Baseline de suite para el delta. (Total de construcciones en `app/src`: 23 =
  1 `MainActivity` + 16 aquí + 6 en `RecipeBackgroundGateTest`.)
- `app/src/test/java/com/sazon/app/ui/artwork/RecipeBackgroundGateTest.kt`,
  `app/src/test/java/com/sazon/app/data/image/DishImageGenerationBudgetTest.kt`,
  `app/src/test/java/com/sazon/app/ui/profile/DishImageSettingsCopyTest.kt` — usan `enabled` explícito
  o estados construidos a mano: **sin diff**.
- `app/build.gradle.kts` (líneas 89-118) — `testImplementation` trae `junit`,
  `kotlinx-coroutines-test`, `ktor-client-mock`, **`robolectric`** y `androidx.test:core`; **hay**
  precedente de tests Robolectric en JVM (`RoomActivitySummaryRepositoryTest`,
  `AssetDishImageStyleReferenceTest`, `AndroidDishImageReducerTest`: `@RunWith(RobolectricTestRunner::class)`,
  `@Config(sdk = [33])`, `ApplicationProvider.getApplicationContext()`). Sin dependencias nuevas.
- `app/src/main/java/com/sazon/app/data/{profile,favorites,activity,suggestions}/*Database.kt` —
  patrón de persistencia del repo: **Room**, una base **propia** por dominio (`sazon.db`,
  `favorites.db`, `activity_summary.db`, `suggestion_batch.db`, versión 1, `exportSchema = false`).
- `app/src/test/java/com/sazon/app/data/activity/RoomActivitySummaryRepositoryTest.kt` — patrón de
  test de persistencia (Robolectric + base en memoria/propia).
- `grep -rn "SharedPreferences\|androidx.datastore\|DataStore" app/src app/build.gradle.kts
  gradle/libs.versions.toml` → **0 usos** (única mención textual: un KDoc de `ui/AppGate.kt` que
  explica que la puerta de primer arranque **no** usa un flag de DataStore). El repo **no tiene**
  persistencia ligera de flags hoy.
- `docs/technical-discovery.md` (línea 15, 79-103, 813-825) — límites durables: "Room para
  persistencia local; DataStore para flags de aplicación … (diferido)"; y la sección de la cadena de
  imagen, que hoy dice "El ajuste es **de sesión** (vida del proceso, sin persistir) y **opt-in**: al
  reiniciar vuelve a **apagado** … y el contador a 12" (queda **desfasada**).
- `docs/adr/0005-ai-dish-images.md` (líneas ~176-190) — "(en memoria; sesión = vida del proceso,
  **sin** persistencia: no toca `UserProfile` ni Room)" (queda **desfasada** en la parte del
  interruptor).
- `docs/specs/dish-image-settings-toggle.md` (Decisión 2 y Decisión 6) y
  `docs/specs/dish-image-settings-default-off.md` (Non-Goals: "No se persiste nada … sería otra
  feature (correctiva)") — contratos aceptados que **esta** feature materializa; no se editan.
- `DESIGN.md` (líneas 365-369 "Cuándo hay imagen", 450-466 sección "Imágenes del plato", 512-527 Core
  Screen 7) — registra el interruptor y que "arranca apagado"; hay que matizar que **el primer
  arranque** es apagado y que después se recuerda la última elección.
- `docs/risks-and-open-questions.md` (línea 165) — fila del gasto de cuota: "opt-in … el interruptor
  **aparece apagado**"; conviene registrar que la elección **se recuerda**.
- `app/src/main/AndroidManifest.xml` (`allowBackup="true"` con reglas por defecto en
  `res/xml/{backup_rules,data_extraction_rules}.xml` sin `include`/`exclude`) — el fichero de
  preferencias puede entrar en el backup/restauración de Android como el resto de datos de la app
  (Room incluido). Es un booleano de ajuste: comportamiento benigno y **no** requiere tocar el
  manifiesto (Non-Goal).

### Existing Patterns To Follow

- **Costura con default no-op** para no ensuciar el dominio ni obligar a dependencias:
  `LocalGeneratedRecipeArtworkSource`, `LocalDishImageOriginDiagnostics` /
  `NoDishImageOriginDiagnostics`.
- **Adaptador Android detrás de una interfaz** en la misma capa que ya mezcla puro y Android:
  `data.image` tiene ficheros puros (`DishImageSettings`, `DishImageOriginSink`) y adaptadores Android
  (`AndroidDishImageCache`, `AndroidRecipeImageDecoder`).
- **Composición explícita sin DI**: `MainActivity` construye y `SazonApp`/`MainShell` reparten por
  parámetro; el ajuste ya se construye ahí una vez por proceso.
- **Política/constantes en un único sitio** (`AiProviderPolicy`, `DishImagePolicy`,
  `DishImageSettingsPolicy`) — el default de arranque **no** se duplica.
- **Estado observable como `Flow`**; la UI derivada no cambia si el estado inicial cambia.
- **Degradación silenciosa** (ADR 0005): un fallo de almacenamiento nunca rompe la UI ni produce
  error visible.
- **Test JUnit 4** en `app/src/test/java/com/sazon/app/...` (nombres en español con `_`), **sin red ni
  emulador**; Robolectric solo cuando hace falta Android real (`@Config(sdk = [33])`).
- **Verificación canónica** `bash init.sh` (`./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`); **nunca** `./gradlew` directo ni `./gradlew --stop`.

### Current Gaps

- **El ajuste no se persiste**: `DishImageSettings` se crea en cada `onCreate` y su `state` inicial es
  siempre `DEFAULT_ENABLED` (apagado). Al reiniciar, el usuario pierde su elección.
- **No existe ninguna persistencia ligera de flags en el repo** (0 `SharedPreferences`, 0 DataStore):
  hoy la única persistencia es Room por dominio, reservada a estado relacional; introducir un flag de
  aplicación es una capacidad **nueva** que este spec acota a un único bit.
- **No existe costura de lectura/escritura del interruptor**: `DishImageSettings` no tiene ningún
  punto por donde recibir un estado previo ni notificar un cambio.
- **KDoc y docs desfasados**: `DishImageSettings` ("No se persiste"), `MainActivity` ("no se
  persiste"), `docs/technical-discovery.md` ("al reiniciar vuelve a apagado"), el ADR 0005
  ("sin persistencia") y `DESIGN.md` ("arranca apagado" sin matizar el primer arranque).

## Technical Approach

### 1. Costura pura nueva — `data/image/DishImageSettingsStore.kt` (nuevo)

Interfaz **síncrona** y **pura** (sin `android.*`, sin coroutines): el arranque necesita el valor
**antes** del primer frame, así que una API suspendente obligaría a `runBlocking` en el punto de
composición o a una corrección asíncrona posterior (parpadeo del interruptor y una primera decisión
del gate potencialmente equivocada).

```kotlin
// data/image/DishImageSettingsStore.kt (puro)
/**
 * Costura de persistencia del **estado del interruptor** de imagenes del plato. Un solo bit: la
 * implementacion puede ser SharedPreferences (app) o una falsa en memoria (tests).
 *
 * Contrato: **no** lanza (un fallo se degrada a `null` / no-op) y **no** expone nada del tope por
 * sesion, de modo que el tope no puede persistirse por construccion.
 */
interface DishImageSettingsStore {
    /** Estado guardado del interruptor, o `null` si nunca se guardo o el valor no es legible. */
    fun loadEnabled(): Boolean?
    /** Guarda el estado del interruptor (mejor esfuerzo; un fallo no interrumpe la UI). */
    fun saveEnabled(enabled: Boolean)
}

/** Default seguro: sin estado guardado y sin efecto (tests, previews y composiciones sin ajuste). */
object NoOpDishImageSettingsStore : DishImageSettingsStore {
    override fun loadEnabled(): Boolean? = null
    override fun saveEnabled(enabled: Boolean) = Unit
}
```

### 2. Adaptador Android — `data/image/AndroidDishImageSettingsStore.kt` (nuevo)

```kotlin
// data/image/AndroidDishImageSettingsStore.kt (unico fichero nuevo de persistencia con Android)
class AndroidDishImageSettingsStore(context: Context) : DishImageSettingsStore {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)   // "dish_image_settings"

    override fun loadEnabled(): Boolean? =
        runCatching { if (prefs.contains(KEY_ENABLED)) prefs.getBoolean(KEY_ENABLED, false) else null }
            .getOrNull()      // valor corrupto (tipo distinto) o fichero ilegible -> null -> default

    override fun saveEnabled(enabled: Boolean) {
        runCatching { prefs.edit().putBoolean(KEY_ENABLED, enabled).apply() }
    }

    private companion object {
        const val PREFS_NAME = "dish_image_settings"
        const val KEY_ENABLED = "enabled"
    }
}
```

Detalles vinculantes: **`apply()`** (nunca `commit()`: la escritura es un bit en una acción explícita
del usuario, y `commit()` dispararía el warning `ApplySharedPref` de lint bajo `app/src`, prohibido);
`MODE_PRIVATE` (dato no sensible, sin permisos nuevos); fichero y clave son **detalle de
implementación** (no vocabulario de dominio) y **sin migración**: ausencia de valor = default.

### 3. Restaurar y persistir — `data/image/DishImageSettings.kt` (modificar)

Se conserva la firma pública actual y se añade **un** parámetro al final con default no-op, más una
**factoría estática** para el arranque real (así la restauración es código de producción testeable en
JVM y no una línea suelta en `MainActivity`):

```kotlin
class DishImageSettings(
    enabled: Boolean = DishImageSettingsPolicy.DEFAULT_ENABLED,
    private val budget: DishImageGenerationBudget = DishImageGenerationBudget(),
    private val store: DishImageSettingsStore = NoOpDishImageSettingsStore,
) {
    companion object {
        /**
         * Arranque real: restaura el estado guardado (o el default de la politica, `false`, si no hay
         * nada) y persiste cada movimiento del interruptor. Sincrono: el primer frame ya lo usa.
         */
        fun restored(store: DishImageSettingsStore): DishImageSettings =
            DishImageSettings(
                enabled = runCatching { store.loadEnabled() }.getOrNull()
                    ?: DishImageSettingsPolicy.DEFAULT_ENABLED,
                store = store,
            )
    }

    fun setEnabled(value: Boolean) {
        _state.update { it.copy(enabled = value) }
        runCatching { store.saveEnabled(value) }   // mejor esfuerzo: un fallo no rompe la UI
    }

    // tryAcquireGeneration() y generatedIfEnabled(): **sin cambios** (no escriben ni leen el store).
}
```

Comportamiento **vinculante**:
- El store **solo** se toca en `restored(...)` (lectura) y en `setEnabled(...)` (escritura): la
  reserva del tope y el gate **no** lo tocan.
- `DEFAULT_ENABLED = false` **no cambia**: sin valor guardado, apagado.
- `runCatching` se justifica porque las dos llamadas son **no suspendentes** (no hay
  `CancellationException` que tragarse) y el contrato del repo es degradación silenciosa (ADR 0005);
  un fallo de lectura cae al default y un fallo de escritura deja la sesión funcionando.
- `DishImageSettingsState`, `tryAcquireGeneration`, `generatedIfEnabled` y
  `GatedDishImageGenerator` quedan **intactos**; el KDoc pierde la frase "**No** se persiste" y
  distingue: interruptor **persistido**, tope **de sesión**.

### 4. Punto de composición — `MainActivity.kt` (modificar, mínimo)

```kotlin
// Interruptor: su estado se RESTAURA de SharedPreferences (un bit) y se persiste al moverlo; el
// TOPE sigue siendo por sesion (un proceso = una sesion) y nunca se persiste.
val dishImageSettings = DishImageSettings.restored(AndroidDishImageSettingsStore(applicationContext))
```

El resto de `MainActivity` (decorador del tope, `generatedIfEnabled` en el proveedor del seam,
reparto por `SazonApp`) **no** cambia; se actualiza el comentario que hoy dice "no se persiste".

### 5. Tests nuevos

- `app/src/test/java/com/sazon/app/data/image/DishImageSettingsPersistenceTest.kt` (**nuevo, puro**):
  factoría de test `RecordingDishImageSettingsStore` (mapa en memoria + contador de escrituras, al
  estilo del `CountingDishImageGenerator` local del test hermano) para Scenarios 1-7.
- `app/src/test/java/com/sazon/app/data/image/AndroidDishImageSettingsStoreTest.kt` (**nuevo,
  Robolectric**): round-trip real del adaptador (guardar→releer con una **segunda** instancia del
  store sobre el mismo `Context`), store virgen → `null` y valor corrupto → `null`.

`DishImageSettingsTest` **no cambia** (sus construcciones `DishImageSettings()`/`(enabled = …, budget
= …)` mantienen significado).

### 6. Docs durables

`docs/technical-discovery.md` (límites de almacenamiento + cadena en runtime), `docs/adr/0005-ai-dish-images.md`
(la Decisión de "sin persistencia" del interruptor pasa a "persistido con SharedPreferences tras una
costura pura; el tope sigue por sesión"), `DESIGN.md` (primer arranque apagado + estado recordado) y
`docs/risks-and-open-questions.md` (el opt-in recordado). Detalle en `Durable Documentation Impact`.

## Expected File Changes

- `app/src/main/java/com/sazon/app/data/image/DishImageSettingsStore.kt` — **crear**; costura pura
  `DishImageSettingsStore` (load/save del booleano) + `NoOpDishImageSettingsStore`.
- `app/src/main/java/com/sazon/app/data/image/AndroidDishImageSettingsStore.kt` — **crear**; adaptador
  `SharedPreferences` (`dish_image_settings` / `enabled`, `MODE_PRIVATE`, `apply()`, lecturas/escrituras
  blindadas).
- `app/src/main/java/com/sazon/app/data/image/DishImageSettings.kt` — **modificar**; parámetro `store`
  (default no-op), factoría `restored(store)`, persistir en `setEnabled`, KDoc (interruptor persistido
  / tope de sesión). **Sin** cambios en `state`, `tryAcquireGeneration`, `generatedIfEnabled`,
  `GatedDishImageGenerator`, `DishImageSettingsPolicy` ni `DishImageSettingsState`.
- `app/src/main/java/com/sazon/app/MainActivity.kt` — **modificar**; línea de composición
  (`DishImageSettings.restored(AndroidDishImageSettingsStore(applicationContext))`) + comentario.
- `app/src/test/java/com/sazon/app/data/image/DishImageSettingsPersistenceTest.kt` — **crear**;
  Scenarios 1-7 (puro, con store falso que cuenta escrituras).
- `app/src/test/java/com/sazon/app/data/image/AndroidDishImageSettingsStoreTest.kt` — **crear**;
  Robolectric: round-trip de `SharedPreferences`, virgen → `null`, valor corrupto → `null`.
- `docs/technical-discovery.md` — **modificar**; límites de almacenamiento (SharedPreferences para el
  flag del interruptor) y cadena en runtime (arranque restaurado; tope por sesión).
- `docs/adr/0005-ai-dish-images.md` — **modificar**; el interruptor se persiste (un bit, costura pura)
  y el tope sigue siendo de sesión.
- `DESIGN.md` — **modificar**; matizar "arranca apagado" → "primer arranque apagado; después recuerda
  la última elección" (sección "Imágenes del plato", "Cuándo hay imagen" y Core Screen 7).
- `docs/risks-and-open-questions.md` — **modificar**; el opt-in del gasto se **recuerda** entre
  reinicios (el tope, no).
- `PROGRESS.md` y `feature_list.json` — **actualizar al implementar/validar** (estado y evidencia);
  **no** los toca el planner.

**No se toca**: `domain/**`, `ui/**` (`MainShell`, `SazonApp`, `ProfileScreen`,
`DishImageSettingsSection`, `DishImageSettingsCopy`, `ui/artwork/**`, `ui/theme/**`),
`data/image/DishImageGenerationBudget.kt`, los aceptados de `data/image`/`data/ai/image` (`DishImageResolver`,
cachés, `DishVisualDescriptorStore`, `DishImageOriginSink`, `DevExpertDishImageGenerator`,
`AndroidDishImageCache`), `data/profile/**`, `data/favorites/**`, `data/activity/**`,
`data/suggestions/**`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `AndroidManifest.xml`,
`app/src/main/res/xml/**`, `init.sh`, `scripts/**`, assets, Taxonomía, `seed-recipes.json`,
`docs/design/concepts/**` y las specs aceptadas.

## Visual Design Impact

- UI involved: **sí** (el interruptor del Perfil arranca con el estado recordado), pero **sin cambio
  visual alguno**: no hay pantalla, layout, geometría, token, microcopy ni estado nuevos.
- Design source: `DESIGN.md` es la fuente de verdad visual (sección "Imágenes del plato", typography,
  `SazonSpacing`, Accessibility Baseline) con el estilo de sección ya aceptado del Perfil.
- Screens or states affected: pestaña **Perfil → "Imágenes del plato"**: el estado de arranque pasa a
  ser "el último elegido por el usuario" en vez de "siempre apagado"; los estados existentes
  (encendido con tope disponible / tope alcanzado / desactivado) no cambian ni se añaden.
- New design artifact required: **no** — no se crea `docs/design/concepts/dish-image-settings-persistence/`
  (misma clase de desviación autorizada que #77/#85/#86/#91/#98/#101, ya registrada para el ajuste en
  `DESIGN.md`).
- **Obligatorio**: el implementer matiza en `DESIGN.md` que el **primer arranque** (sin estado
  guardado) es apagado (opt-in) y que después se recuerda la última elección, dejando claro que el
  **tope** sigue siendo de sesión (el microcopy "Se reanudará al reiniciar la app" sigue vigente).

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — (a) límites de almacenamiento (línea ~15 y tabla): el
  flag del interruptor de imágenes de plato se persiste con **SharedPreferences** detrás de una costura
  pura, sin dependencias nuevas (DataStore sigue diferido: añadirlo sería una dependencia nueva); Room
  se reserva a estado relacional; (b) cadena en runtime (líneas ~813-825): el interruptor se
  **restaura** al arrancar y `DEFAULT_ENABLED = false` aplica solo cuando no hay estado guardado; el
  **tope** sigue siendo de sesión y arranca en 12.
- `docs/adr/0005-ai-dish-images.md`: **update** — la fase 4/ajuste deja de declarar "sin persistencia"
  para el interruptor: se persiste **un bit** por una costura pura + `SharedPreferences` (sin Room, sin
  `UserProfile`, sin dependencias nuevas) y el **tope por sesión** se mantiene sin persistir; la
  Decisión 2 de `docs/specs/dish-image-settings-toggle.md` queda superada en esa parte.
- `DESIGN.md`: **update** — matizar el arranque (primer arranque apagado / después, estado recordado)
  en "Cuándo hay imagen", "Imágenes del plato" y Core Screen 7.
- `docs/risks-and-open-questions.md`: **update** — la fila del gasto de cuota registra que el opt-in se
  **recuerda** entre reinicios (y que el tope, no).
- `docs/domain-model.md`: **not needed** — no hay entidades, relaciones ni estados nuevos; el ajuste
  **no** entra en `UserProfile` ni en la Firma de preferencias (sigue fuera del dominio).
- `AGENTS.md`: **not needed** — no cambian flujo, arranque ni reglas de trabajo.
- `CONTEXT.md`: **not needed** — no aparece vocabulario de dominio nuevo (el ajuste no es una
  preferencia culinaria).
- `docs/design/concepts/`: **not needed** — desviación de capturas ya autorizada para el ajuste; no se
  crea carpeta nueva.
- `docs/specs/dish-image-settings-toggle.md` y `docs/specs/dish-image-settings-default-off.md`: **not
  needed** — specs aceptadas (contratos históricos); la supersesión de su Decisión 2 se registra aquí y
  en el ADR 0005.

## Implementation Plan

1. Crear `DishImageSettingsStore.kt` (interfaz síncrona pura + `NoOpDishImageSettingsStore`).
2. Crear `AndroidDishImageSettingsStore.kt` (SharedPreferences `dish_image_settings`/`enabled`,
   `MODE_PRIVATE`, `apply()`, lectura/escritura blindadas).
3. Modificar `DishImageSettings.kt`: parámetro `store` con default no-op, factoría `restored(store)`,
   persistencia en `setEnabled`, KDoc al día. No tocar `state`/`tryAcquireGeneration`/`generatedIfEnabled`/
   `GatedDishImageGenerator`/la política.
4. Escribir `DishImageSettingsPersistenceTest` (Scenarios 1-7 con un store falso que cuenta
   escrituras) y `AndroidDishImageSettingsStoreTest` (Robolectric).
5. Cablear `MainActivity` (`DishImageSettings.restored(AndroidDishImageSettingsStore(applicationContext))`)
   y actualizar su comentario.
6. Verificar por inspección: `ui/**`, `domain/**`, los aceptados de `data/image`/`data/ai/image`,
   `DishImageGenerationBudget`, build/lockfile/manifest/`init.sh`/`scripts` sin diff; ficheros nuevos
   puros sin `android.*`/`androidx.*`; sin dependencias nuevas.
7. Actualizar `docs/technical-discovery.md`, el ADR 0005, `DESIGN.md` y `docs/risks-and-open-questions.md`.
8. Ejecutar `bash init.sh` en verde, registrar evidencia y dejar la feature `passing` (la validación
   independiente es de otro rol; **sin** commit, push ni PR, que los hace el orquestador).

## Implementation Tasks

- [ ] Crear `data/image/DishImageSettingsStore.kt`: `interface DishImageSettingsStore`
      (`loadEnabled(): Boolean?`, `saveEnabled(Boolean)`, contrato "no lanza") + `NoOpDishImageSettingsStore`.
- [ ] Crear `data/image/AndroidDishImageSettingsStore.kt`: `SharedPreferences` privado
      (`dish_image_settings` / `enabled`), `loadEnabled()` con `contains`+`runCatching` (corrupto →
      `null`), `saveEnabled()` con `apply()` y `runCatching`.
- [ ] `DishImageSettings.kt`: añadir `store: DishImageSettingsStore = NoOpDishImageSettingsStore`,
      factoría `restored(store)` (guardado → default de la política), `setEnabled` persiste el bit,
      KDoc actualizado (interruptor persistido / tope de sesión); resto del fichero **sin cambios**.
- [ ] `DishImageSettingsPersistenceTest` (puro): guardar/releer encendido y apagado; sin estado
      guardado → `(false, 0, 12)`; arranque con `true` → 1 invocación del productor y con `false`/vacío
      → 0; tope no persistido (`used = 0`, `max = 12` con `true` guardado); `setEnabled` escribe 1 vez y
      la reserva de tope 0 veces; store que lanza en `load`/`save` → default / sin crash.
- [ ] `AndroidDishImageSettingsStoreTest` (Robolectric `@Config(sdk = [33])`): round-trip con una
      segunda instancia sobre el mismo `Context`; store virgen → `null`; valor de tipo distinto → `null`.
- [ ] `MainActivity`: usar `DishImageSettings.restored(AndroidDishImageSettingsStore(applicationContext))`
      y actualizar el comentario del bloque (el interruptor se persiste; el tope no).
- [ ] Verificar por inspección: sin diff en `domain/**`, `ui/**`, aceptados de `data/**`,
      `DishImageGenerationBudget.kt`, build/lockfile/`AndroidManifest.xml`/`init.sh`/`scripts/**`;
      ficheros nuevos puros sin `android.*`/`androidx.*`; `DEFAULT_ENABLED == false` y
      `MAX_GENERATIONS_PER_SESSION == 12`; sin dependencias nuevas.
- [ ] Actualizar `docs/technical-discovery.md`, `docs/adr/0005-ai-dish-images.md`, `DESIGN.md` y
      `docs/risks-and-open-questions.md`.
- [ ] Ejecutar `bash init.sh` (test + lint) en verde, registrar la evidencia y actualizar
      `feature_list.json` (a `passing`, **sin tocar su `verification`**) y `PROGRESS.md`.

## Verification Plan

- `bash init.sh` → **exit 0**. Ejecuta el gate no bloqueante estándar (`./gradlew test` + `./gradlew
  lint` vía `scripts/gradle.sh`), **no** arranca procesos largos y **no** modifica `init.sh`.
- Tiradas focales:
  - `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.image.*"` →
    `BUILD SUCCESSFUL` (las suites aceptadas de `data.image` sin regresión + las 2 nuevas).
  - `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.artwork.*" --tests
    "com.sazon.app.ui.profile.*"` → `BUILD SUCCESSFUL` (sin cambios en esos tests).
- Parseo propio de los XML (`app/build/test-results/testDebugUnitTest/TEST-*.xml`): **0 fallos, 0
  errores, 0 saltados**. Baseline re-derivable en `HEAD` `89ed694` (**98 suites / 739 tests**; `git
  grep -c '@Test'` = 98 ficheros / 739 anotaciones); delta esperado **+2 suites / ≈ +10 tests**,
  **0 suites modificadas**.
- `bash scripts/gradle.sh lint` → **0 errores**; **0 warnings nuevos bajo `app/src`** (en particular,
  sin `ApplySharedPref`: el adaptador usa `apply()`).
- Mapeo explícito del `verification` de la feature:
  - punto 1 (guardar/releer encendido y apagado; sin estado guardado → default apagado) → Scenario 4
    (`guardar_y_releer_*`) + Scenario 3 (`sin_estado_guardado_*`) + el test Robolectric de round-trip.
  - punto 2 (arranque con persistido encendido → invoca al productor; apagado → 0 invocaciones) →
    Scenario 1 (`el_arranque_con_estado_persistido_encendido_pide_imagenes`) y Scenario 2
    (`..._apagado_no_pide_imagenes`) contando invocaciones con un productor contador.
  - punto 3 (el tope no se persiste; arranca en 12 aunque el estado guardado esté encendido) →
    Scenario 5 (`el_tope_no_se_persiste_y_arranca_en_12`, con `true` guardado y una sesión previa con
    consumo) + Scenario 6 (la reserva no escribe en el store) + inclusión del propio test en `data.image.*`.
  - punto 4 (`bash init.sh` verde y sin dependencias nuevas) → gate completo + `git diff` sin cambios
    en `app/build.gradle.kts`/`gradle/libs.versions.toml`.
- Inspección de sesgo/alcance:
  - `git diff --name-only` **solo** con los 2 ficheros nuevos de `data.image` (1 puro + 1 adaptador),
    `data/image/DishImageSettings.kt`, `MainActivity.kt`, los 2 tests nuevos y las 4 docs previstas.
  - `grep -rn "import android\|import androidx" app/src/main/java/com/sazon/app/data/image/DishImageSettingsStore.kt`
    → **0**; en `AndroidDishImageSettingsStore.kt` → solo `android.content.Context/SharedPreferences`.
  - `grep -rn "commit()" app/src/main/java` → **0**; `grep -rn "loadEnabled\|saveEnabled" app/src/main`
    → solo la costura, el adaptador, `DishImageSettings` y `MainActivity`.
  - `grep -rn "DEFAULT_ENABLED" app/src/main` → `DishImageSettingsPolicy` (`false`) + `DishImageSettings`;
    `MAX_GENERATIONS_PER_SESSION == 12` intacto.
  - `grep -rn "SharedPreferences" app/src/main` → solo `AndroidDishImageSettingsStore.kt` (y el KDoc de
    `AppGate.kt`, preexistente).
  - `test -d docs/design/concepts/dish-image-settings-persistence` → **no existe**.
- Cobertura **E2E**: **no aplica**. El repo no tiene suite E2E persistente y AGENTS.md no automatiza UI
  de emulador; la cobertura canónica es el test JVM (todo el estado y la costura viven en `data.image`)
  más la revisión manual de la persona contra `DESIGN.md`.
- **Prueba manual (persona, tras el merge; no automatizable desde WSL)**: en el emulador, (a) con la
  app recién instalada (o datos borrados) comprobar que arranca **apagado** y con 12 de 12; (b)
  **encender**, cerrar la app del todo (recents/kill) y reabrir → sigue **encendido**, con el contador
  en **12** y las imágenes generándose de nuevo; (c) **apagar**, cerrar y reabrir → sigue **apagado**
  y sin pedir imágenes (ficha tipográfica, sin red ni cuota); (d) revisar claro y oscuro que el
  interruptor y el resumen no cambian de aspecto.
- Nota operativa: usar `bash init.sh` o `bash scripts/gradle.sh <tarea>`; **nunca** `./gradlew` directo
  ni `./gradlew --stop` (AGENTS.md, Notas de entorno).

## Evidence To Capture

- Salida de `bash init.sh` con código 0 y de las tiradas focales (`data.image.*`, `ui.artwork.*`,
  `ui.profile.*`).
- Conteo parseado del XML (suites/tests, 0 fallos/errores/saltados) con la baseline re-derivada en HEAD
  y el delta (+2 suites / ≈ +10 tests, 0 suites modificadas).
- Informe de lint (0 errores, ninguno nuevo bajo `app/src`).
- Vectores concretos: store con `true` → `state.value == (true, 0, 12)` y productor invocado **1** vez;
  store con `false` y store vacío → **0** invocaciones y `(false, 0, 12)`; `setEnabled(true)` →
  `loadEnabled()` de una segunda instancia `true`; `setEnabled(false)` → `false`; `true` guardado +
  proceso nuevo → `used == 0`, `remaining == 12`; `saveEnabled` invocado **1** vez por movimiento y
  **0** por reserva de tope; store que lanza → default apagado sin excepción.
- Resultado de los greps de alcance/pureza (sin diff fuera de lo previsto; `0` `commit()`;
  `SharedPreferences` solo en el adaptador; `DEFAULT_ENABLED = false` y `MAX_GENERATIONS_PER_SESSION = 12`;
  carpeta de capturas de esta feature inexistente) y del `git diff --stat`.
- Confirmación de que el implementer **no** hizo commit, push ni PR (los hace el orquestador).

## Decisions And Unknowns

### Decisiones cerradas (el implementer no las reabre)

1. **Mecanismo: `SharedPreferences` detrás de una costura pura** (no Room, no DataStore, no
   `UserProfile`). Justificación contra los patrones del repo:
   - **Sin dependencias nuevas**: `SharedPreferences` es API del framework. `androidx.datastore` (la
     opción que `docs/technical-discovery.md` línea 15 reserva a "flags de aplicación", hoy diferida)
     sería una dependencia **nueva**, prohibida por la feature.
   - **Un solo bit no justifica Room**: Room en este repo es por **dominio con estado relacional**
     (`sazon.db`, `favorites.db`, `activity_summary.db`, `suggestion_batch.db`); abrir una base nueva
     con entidad+DAO+KSP para un booleano añade superficie, un fichero de base y deuda de versiones sin
     beneficio.
   - **El arranque es síncrono**: `MainActivity.onCreate` construye el ajuste **antes** de `setContent`
     y el gate decide en el primer frame. Una lectura síncrona (SharedPreferences) restaura el estado
     sin más; Room obligaría a `runBlocking` (I/O en el hilo principal) o a una corrección asíncrona
     posterior al primer frame (parpadeo del interruptor y primera decisión del gate potencialmente
     equivocada).
   - **Pureza y testabilidad**: la costura es Kotlin puro sin `android.*`; el adaptador es el único
     fichero con `android.content`; el round-trip se prueba en JVM con una costura falsa y con
     Robolectric (precedente ya existente en el repo).
   - **`UserProfile` está excluido de raíz**: sus campos entran en `PreferenceSignature` y forzarían
     refresco del Lote (lo dice la propia entrada de la feature).
2. **Solo el interruptor se persiste**: un único booleano. La costura **no** expone nada del tope; el
   tope sigue siendo de sesión (`MAX_GENERATIONS_PER_SESSION = 12`) y arranca intacto en cada proceso.
3. **Restauración síncrona en el punto de composición**: `DishImageSettings.restored(store)` se llama
   **una** vez en `MainActivity`; el estado guardado decide el primer frame. No hay estado intermedio
   "apagado y luego encendido".
4. **`DEFAULT_ENABLED = false` no cambia**: sin valor guardado (primer arranque, datos borrados,
   instalación nueva) la app arranca **apagada** (opt-in de `dish-image-settings-default-off`, ADR 0006);
   lo que cambia es que **si hay** valor guardado, manda el guardado.
5. **Escritura solo en `setEnabled`**: mover el interruptor persiste el bit; reservar tope, resolver o
   pintar **no** escriben. `apply()` (no `commit()`): sin I/O bloqueante en el hilo principal y sin el
   warning `ApplySharedPref` de lint.
6. **Degradación silenciosa** (ADR 0005): un fallo de lectura cae al default de la política; un fallo de
   escritura no interrumpe la UI y la sesión sigue con el valor elegido. Un valor guardado con tipo
   distinto (`ClassCastException` del framework) se trata como "sin estado guardado".
7. **Semántica del gate intacta** (decisión de la persona): no se toca `generatedIfEnabled`,
   `GatedDishImageGenerator`, `DishImageResolver` ni la cadena (memoria → disco → semilla → generador);
   apagado = `null` **sin** leer caché, semilla ni API.
8. **Firma pública preservada**: `enabled: Boolean = DEFAULT_ENABLED` se mantiene (así las 23
   construcciones actuales en `app/src`, 16 de ellas en `DishImageSettingsTest` y 6 en
   `RecipeBackgroundGateTest`, no cambian de sentido) y la restauración va en la factoría
   `restored(store)`, que es código de producción testeable en JVM. El default del parámetro `store` es
   el **no-op** (mismo patrón que `NoDishImageOriginDiagnostics` / `LocalGeneratedRecipeArtworkSource`):
   no hay una composición real "sin persistencia" que pueda olvidarse porque el **único** punto de
   composición es `MainActivity`, y el Scenario 8 lo fija por inspección
   (`grep -rn "loadEnabled\|saveEnabled" app/src/main` → solo la costura, el adaptador,
   `DishImageSettings` y `MainActivity`).
9. **Sin cambios de UI ni de microcopy**: la sección del Perfil, el resumen y el reparto
   `imageBackground` ya derivan de `DishImageSettingsState`; el resumen del tope ("Se reanudará al
   reiniciar la app") sigue siendo correcto porque el tope **sí** se reinicia.
10. **Sin `init.sh`, build, lockfile ni manifest**: la capacidad nueva no requiere permisos ni
    dependencias.

### Unknowns y riesgos (no bloquean, quedan anotados)

- **Primer acceso a `SharedPreferences` en el hilo principal**: el framework carga el fichero (un bit)
  de forma síncrona en el primer `getSharedPreferences`; es el idioma estándar de Android y el fichero
  es mínimo. Si en el futuro se quisiera I/O de arranque cero, la costura permite cambiar el adaptador
  (p. ej. DataStore) sin tocar el gate ni la UI — pero requeriría una dependencia nueva.
- **Visibilidad de `apply()` en Robolectric**: el test del adaptador debe releer con una **segunda
  instancia del store sobre el mismo `Context`** (mismo `SharedPreferences` cacheado), no con otro
  contexto/factory. Si el harness mostrara asincronía, la salida correcta **no** es `commit()` (lint
  `ApplySharedPref` bajo `app/src`, prohibido por la verificación) sino ajustar el test al mismo
  contexto.
- **Clave/fichero de preferencias**: `dish_image_settings` / `enabled` son detalle interno (no
  vocabulario de dominio); si en el futuro se necesita compartir con otro ajuste, se renombra en el
  adaptador sin migración (ausencia = default).
- **Backup/restauración de Android**: con `allowBackup="true"` y reglas por defecto, el fichero de
  preferencias puede restaurarse en otro dispositivo (igual que las bases Room). Es un booleano de
  ajuste: benigno y **no** justifica tocar el manifiesto (Non-Goal).
- **Multi-proceso**: la app es mono-proceso; una instancia del ajuste por proceso (como hoy). No hay
  carrera entre procesos que resolver.
- **`runCatching` en `setEnabled`**: solo envuelve una llamada **no suspendente** a
  `SharedPreferences`; el KDoc debe dejar claro que no oculta cancelación (no hay suspensión) y que la
  semántica es "mejor esfuerzo".
- **Baseline de tests**: 98 suites / 739 tests en `HEAD` `89ed694`; el implementer **re-deriva** el
  número en su rama (`git grep -c '@Test'` + `git ls-tree`) y confirma el delta (+2 suites, ≈ +10
  tests, 0 suites modificadas).
- **Acabado visual**: nada nuevo que revisar; si en la prueba manual la persona notara un defecto de
  interacción con el estado restaurado, se corrige con una **feature correctiva** (no bloquea el flujo
  autónomo).

## Validator Checklist

- [ ] Alcance: solo `data/image/DishImageSettingsStore.kt`, `data/image/AndroidDishImageSettingsStore.kt`
      (nuevos), `data/image/DishImageSettings.kt`, `MainActivity.kt`, los 2 tests nuevos y las 4 docs
      previstas; **sin** tocar `domain/**`, `ui/**`, los aceptados de `data/image`/`data/ai/image`,
      `DishImageGenerationBudget.kt`, `DishImageSettingsCopy.kt`, build, lockfile, `AndroidManifest.xml`,
      `init.sh`, `scripts/**` ni dependencias.
- [ ] Guardar/releer: encendido → encendido; apagado → apagado; sin estado guardado → default de la
      política (**apagado**) (Scenarios 3 y 4; verificación exigida 1).
- [ ] Arranque restaurado: con `true` guardado el gate **invoca** al productor (1 vez); con `false` o
      vacío **no** lo invoca (0) (Scenarios 1 y 2; verificación exigida 2).
- [ ] Tope **no** persistido: con `true` guardado, `used == 0`/`max == 12`/`remaining == 12`; la costura
      solo expone el booleano y la reserva no escribe (Scenarios 5 y 6; verificación exigida 3).
- [ ] Semántica del gate intacta: `generatedIfEnabled`, `GatedDishImageGenerator` y la cadena
      (memoria→disco→semilla→generador) sin diff; apagado no lee caché/semilla/API.
- [ ] Degradación silenciosa: `loadEnabled()` que lanza o valor corrupto → apagado sin crash;
      `saveEnabled()` que lanza → sin crash y estado en memoria correcto (Scenario 7).
- [ ] Ficheros nuevos puros sin `android.*`/`androidx.*` (solo el adaptador importa `android.content`);
      `apply()` y no `commit()`; sin dependencias nuevas (`app/build.gradle.kts`/`libs.versions.toml`
      sin diff).
- [ ] `DEFAULT_ENABLED == false` y `MAX_GENERATIONS_PER_SESSION == 12` siguen vigentes; sin cambios en
      `DishImageSettingsState`, `tryAcquireGeneration`, `generatedIfEnabled` ni `GatedDishImageGenerator`.
- [ ] `bash init.sh` (test + lint) verde; **0 fallos/errores/saltados**; 0 warnings nuevos bajo
      `app/src`; baseline y delta registrados con la evidencia.
- [ ] `docs/technical-discovery.md`, `docs/adr/0005-ai-dish-images.md`, `DESIGN.md` y
      `docs/risks-and-open-questions.md` están actualizados y coherentes (interruptor persistido, tope
      por sesión, primer arranque apagado), y **no** contradicen el código ni las specs aceptadas.
- [ ] No existe `docs/design/concepts/dish-image-settings-persistence/`; no se toca el microcopy ni el
      layout del Perfil.
- [ ] Escenarios 1-8 cubiertos por tests JVM (incluidos los negativos: store vacío, store que lanza,
      valor corrupto, tope no persistido) o justificados por inspección/prueba manual según AGENTS.md.
- [ ] `feature_list.json` y `PROGRESS.md` reflejan estado y evidencia; el `verification` de la feature
      queda **intacto** y el implementer no hizo commit, push ni PR.
