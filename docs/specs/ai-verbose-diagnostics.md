# Feature Implementation Spec: Diagnóstico fino de descartes y respuesta cruda de la IA (modo depuración)

## Source Feature

- `id`: `ai-verbose-diagnostics` · `area`: `integration` · `depends_on`: `["ai-call-diagnostics"]`
  (`accepted`, sesión 073) · `status`: `not_started` · `source`: `feature_list.json` (issue por crear; el identificador lo asigna `github-flow`).
- **No es feature de UI**: `area: integration`; sin pantallas, estados, microcopy ni capturas. No usa
  `docs/design/concepts/` y **no** se bloquea por Stitch.
- Esta spec es el contrato planner → implementer → validator. El planner **no** modifica
  `feature_list.json` ni `PROGRESS.md`; el estado (`in_progress` → `passing` → `accepted`) lo mueven
  implementer/validador/orquestador.
- Verificación exigida (textual): (1) `./gradlew test` (vía `bash init.sh`) cubre que el **resumen de
  descartes distingue las dos causas de `INCOHERENT`** (deriva de tiempos vs. `declaredAllergens`
  incompletos) y que **con el modo de depuración activado** el evento incluye el **detalle del
  descarte** y el **contenido crudo del modelo**, mientras que **con el modo apagado** el evento es
  idéntico al actual y ningún evento contiene la clave ni la cabecera `Authorization`; (2) prueba
  **manual** en emulador: con clave válida, buscar `arroz con pollo` y ver en logcat (`SazonAi`) el
  desglose de motivos; con el modo activado, el detalle de cada descarte y el JSON crudo (troceado si
  supera el límite de línea); con el modo apagado, el log vuelve al formato anterior.

**Contexto (correctiva de `ai-call-diagnostics`).** El proveedor real ya está conectado, el
decode es tipado (`AiDecodeResult`) y el diagnóstico explica cada llamada. Pero una prueba real de
búsqueda con `arroz con pollo` (2026-10-12) mostró
`received=6 accepted=2 discards={INCOHERENT=4}` **sin poder saber por qué**: `INCOHERENT` agrupa **dos**
reglas distintas del Validador (`RecipeValidator.kt`):

- **4a deriva de tiempos**: `abs(steps.sumOf { it.minutes } - totalMinutes) > 5`
  (`RecipeValidator.MAX_STEP_TOTAL_DRIFT_MINUTES`).
- **4b alergenos declarados**: `declaredAllergens` no cubre los alérgenos curados de los ingredientes
  resueltos.

Además no hay forma de ver el **detalle** de cada descarte ni el **JSON crudo** que devolvió el modelo,
que es lo que permitiría depurar el prompt sin reproducir a ciegas.

## Goal

Dos entregables acotados, ambos sobre el diagnóstico local de `com.sazon.app.data.ai` (más el formato
del `Log.d` de `MainActivity`), **sin** cambiar el Validador ni el dominio:

1. **(A+B, siempre activo) Desglosar `INCOHERENT` en sus dos causas.** El decode calcula un **contador
   legible** `Map<AiIncoherentCause, Int>` (`TIME_DRIFT` = 4a, `ALLERGEN_COVERAGE` = 4b) y lo expone en
   el evento (`AiCallEvent.incoherentByCause`) y en la línea `SazonAi`. Solo cuenta y clasifica; **no**
   expone texto de recetas.
2. **(D, modo depuración, ocultable) Detalle de descartes + JSON crudo.** Un interruptor
   (`verboseDiagnostics`, **apagado por defecto**; la producción lo activa **solo en debug** vía
   `BuildConfig.DEBUG`) hace que el evento y el `Log.d` añadan: (i) el **detalle de cada descarte**
   (motivo + causa + **título**) y (ii) el **contenido crudo del modelo** (`choices[0].message.content`),
   **troceado** para no superar el límite de línea de Logcat. Con el interruptor apagado no se emite ni
   título ni cuerpo crudo.

**Nunca** se registra la clave (`BuildConfig.DEVEXPERT_API_KEY`) ni la cabecera `Authorization`, en
ningún modo. El comportamiento hacia el usuario (solo recetas validadas) no cambia; cambia el log local.

## Non-Goals

- **No** se cambia la **semántica** del Validador (reglas, orden ni decisión aceptar/descartar): sigue
  siendo la **puerta dura**. No se toca `RecipeValidator.validateRecipe` ni el enum `DiscardReason`
  (el desglose es un **diagnóstico** en `data.ai`, no un motivo nuevo de descarte).
- **No** se toca `Recipe.kt`, el wire del contrato §4 (`docs/domain/taxonomy.md`), el puerto
  `RecipeGenerator`, el `PromptBuilder`, `AiProviderPolicy` (timeouts/reintentos/
  `RESPONSE_FORMAT_ENABLED`), `OpenAiChat`, `AiHttpClientFactory` ni `response_format`.
- **No** se cambia el proveedor ni el modelo (`chat` sigue siendo el alias diario).
- **No** hay telemetría ni persistencia del diagnóstico, ni se muestra en la UI; el evento sigue siendo
  local y de solo lectura en Logcat.
- **No** se toca el filtrado por Perfil de `AiSearchService`, ni `GenerationService`/`SuggestionBatchService`,
  ni las bases Room, la Taxonomía, `seed-recipes.json`, `app/build.gradle.kts`,
  `gradle/libs.versions.toml`, `AndroidManifest.xml`, `init.sh`, `scripts/**`, `DESIGN.md`,
  `docs/design/**`.
- **No** se añaden dependencias ni se toca el log de otras rutas.

## Job Story

Como desarrollador que depura con clave real en Android Studio,
quiero saber, para cada lote descartado, **cuál** de las dos reglas de coherencia falló y poder ver
temporalmente **el detalle de cada descarte y el JSON crudo** del modelo,
para ajustar el prompt o el modelo sin reproducir a ciegas y sin filtrar la clave ni la cabecera.

## Users And Permissions

- **Usuario local**: dueño único del dispositivo (ADR 0002). No cambia nada de lo que ve.
- **Desarrollador / agente**: verifica en **JVM** (clave ficticia `test-key`, `MockEngine`, reloj
  inyectado): el desglose `INCOHERENT` (4a/4b), el interruptor encendido/apagado, el troceado y la
  **no-fuga** de clave/`Authorization`. Nunca usa red real ni clave real.
- **Sistema**: `AiCallEvent` sigue emitiéndose por la costura `onEvent` (default no-op) y cableado a
  `Log.d("SazonAi", …)`; el puerto `RecipeGenerator` y la degradación a modo semilla no cambian.

## Acceptance Scenarios

### Scenario 1: el resumen distingue las dos causas de `INCOHERENT` (JVM, siempre activo)

Given un lote del contrato §4 con una receta que falla **4a** (los `steps` suman > 5 min de diferencia
con `totalMinutes`) y otra que falla **4b** (usa un ingrediente con alérgeno curado que no está en
`declaredAllergens`),
When se invoca la operación con `onEvent` capturado (interruptor de depuración **apagado**),
Then el evento final lleva `discardedByReason = {INCOHERENT: 2}` **y**
`incoherentByCause = {TIME_DRIFT: 1, ALLERGEN_COVERAGE: 1}` (solo causas presentes, en orden del enum),
sin ningún título ni texto de receta. La línea `SazonAi` incluye `incoherent={TIME_DRIFT=1, ALLERGEN_COVERAGE=1}`.

### Scenario 2: la clasificación es coherente con el Validador (JVM, regresión del criterio)

Given recetas que (i) fallan solo 4a, (ii) fallan solo 4b, (iii) fallan **ambas** y (iv) no fallan ninguna,
When se comparan `RecipeValidator.validateRecipe(...)` con el clasificador del decode,
Then (i) → `TIME_DRIFT`, (ii) → `ALLERGEN_COVERAGE`, (iii) → `TIME_DRIFT` (4a se evalúa **antes** que 4b,
igual que el Validador) y (iv) → `Accepted` con `incoherentByCause` vacío. Ningún descarte `INCOHERENT`
del Validador se queda sin causa, y ninguna receta sin `INCOHERENT` recibe causa.

### Scenario 3: modo depuración apagado ⇒ nada de detalle ni de cuerpo crudo (JVM)

Given el interruptor `verboseDiagnostics = false` (por defecto) y un lote con descartes,
When se invoca la operación con `onEvent` capturado,
Then `event.discards` está **vacío**, `event.rawContent` es **null** y `toString()` del evento **no**
contiene el `content` del modelo ni ningún título de receta. Con un lote **sin** `INCOHERENT`, la línea
`SazonAi` es **byte a byte** la actual (`… discards={…} <detail>`), sin `incoherent=`.

### Scenario 4: modo depuración activado ⇒ detalle y JSON crudo troceado (JVM)

Given `verboseDiagnostics = true` y un `200` con contenido real (varios KB) y recetas descartadas,
When se invoca la operación y `MainActivity` renderiza el evento,
Then `event.discards` lleva, por cada descarte, `reason`, `cause` (si es `INCOHERENT`) y **título**
(`null`/`(sin titulo)` si el elemento no se decodificó), y `event.rawContent` es el `content` del modelo;
`MainActivity` emite una línea por descarte y **trocea** el crudo en líneas `raw[i/n]` cada una por
debajo del límite de Logcat, de modo que concatenar los trozos reconstruye el contenido original.

### Scenario 5: nunca la clave ni `Authorization` (JVM)

Given cualquier evento/line de cualquier modo (clave ficticia `test-key`, cuerpos reconocibles),
When se inspeccionan `toString()` de todos los eventos y las líneas de `AiVerboseDiagnostics`,
Then **ninguno** contiene `test-key`, `Bearer` ni `Authorization`; el crudo del modelo solo aparece en
modo depuración (es el `content` de la respuesta, no la cabecera).

### Scenario 6: regresión (JVM)

Given los **62** tests actuales de `data.ai` (`AiProviderPolicyTest` 12, `AiRecipeIdsTest` 6,
`AiRecipePayloadTest` 11, `DevExpertRecipeGeneratorTest` 31, `OpenAiChatTest` 2),
When corre `com.sazon.app.data.ai.*` y `RecipeValidatorTest`,
Then siguen verdes salvo los cambios mecánicos (campos nuevos con default y la decodificación con
`verbose`): timeouts 120/150/240, `MAX_ATTEMPTS`/backoff/`Retry-After`, `response_format`/fallback,
decode del contrato, ids deterministas y reglas del Validador intactos.

### Scenario 7: prueba manual en emulador (la persona; no automatizable desde WSL)

Given `devexpert.apiKey` válida y el emulador de Android Studio en **Windows** (build **debug**),
When se busca `arroz con pollo`,
Then logcat (`SazonAi`) muestra el desglose `incoherent={…}`, y en modo depuración el detalle de cada
descarte y el JSON crudo troceado; con el modo apagado (release) el log vuelve al formato anterior.
Clave ausente o vaciada → modo semilla sin fallar.

## Repository Research

### Files Inspected

- `feature_list.json` — entrada `ai-verbose-diagnostics` (alcance acordado por la persona; issue por crear).
- `docs/specs/ai-call-diagnostics.md` y `docs/specs/ai-response-format.md` — plantilla de correctiva;
  estado vigente de `AiDecodeResult`, `AiCallEvent`, `report(...)`, el interruptor por constructor
  (`responseFormatEnabled`) y las convenciones de tests.
- `app/src/main/java/com/sazon/app/data/ai/AiRecipePayload.kt` — `decode(content, taxonomy, constraints)`
  devuelve `AiDecodeResult`; en `is RecipeValidation.Discarded -> discardedCounts.increment(result.reason)`
  (hoy el `detail` y el título se pierden a propósito).
- `app/src/main/java/com/sazon/app/data/ai/AiDecodeResult.kt` — `AiDecodeResult`/`AiContentState`
  (`internal`); campos `accepted`/`received`/`discardedByReason`/longitudes/`contentState`.
- `app/src/main/java/com/sazon/app/data/ai/AiDiagnostics.kt` — `AiCallEvent` (11 campos con default),
  `AiCallOutcome`, `AiResponseFormat`, `AiOperation`; KDoc con la garantía «nunca clave/`Authorization`/cuerpos».
- `app/src/main/java/com/sazon/app/data/ai/DevExpertRecipeGenerator.kt` — `execute` (rama
  `AttemptOutcome.Success -> AiRecipePayload.decode(...) -> report(...)`), `sendAttempt` (devuelve
  `Success(status, content)` con el `content` del modelo), `report(...)` y el patrón de costura por
  constructor (`responseFormatEnabled: Boolean = AiProviderPolicy.RESPONSE_FORMAT_ENABLED`).
- `app/src/main/java/com/sazon/app/data/ai/AiProviderPolicy.kt` — política en un único sitio (timeouts
  120/150/240, `MAX_ATTEMPTS`, `RESPONSE_FORMAT_ENABLED`, `NEUTRAL_CONSTRAINTS`); referencia de estilo.
- `app/src/main/java/com/sazon/app/domain/validation/RecipeValidator.kt` — **regla 4a** (líneas ~174-182,
  `MAX_STEP_TOTAL_DRIFT_MINUTES = 5`) y **regla 4b** (líneas ~184-196), ambas `DiscardReason.INCOHERENT`;
  `Discarded(reason, detail)`, `DiscardedRecipe(reason, detail, title)`; `titleOf(element)`.
- `app/src/main/java/com/sazon/app/MainActivity.kt` (líneas ~50-68) — `onEvent` → `Log.d("SazonAi", …)`
  con `format=`, `content=…c/…b`, `received=…`, `accepted=…`, `discards={…}`; inyecta
  `BuildConfig.DEVEXPERT_API_KEY`.
- `app/build.gradle.kts` — `buildFeatures { buildConfig = true }` y `buildConfigField(DEVEXPERT_API_KEY)`;
  no hay `buildConfigField` de diagnóstico (no hará falta: se usa `BuildConfig.DEBUG`).
- `app/src/main/java/com/sazon/app/domain/search/AiSearchService.kt`,
  `domain/generation/GenerationService.kt`, `ui/search/AiSearchOutcome.kt` — **llamadores** que ya
  construyen su `discarded` con el Perfil efectivo y deciden el estado Bloqueado por `reason`; **no** se
  tocan (el desglose es del proveedor).
- `app/src/test/java/com/sazon/app/data/ai/{AiRecipePayloadTest,DevExpertRecipeGeneratorTest,AiProviderPolicyTest}.kt`
  — convenciones: `runTest`, `MockAi` (contador + `lastRequest` + `bodyText()`), clave `test-key`,
  `MockEngine`, reloj inyectado, nombres en español con `_`, casos negativos y `contractRecipe(...)`
  (fixture del contrato §4 con `totalMinutes`/`stepMinutes` parametrizables).
- `docs/technical-discovery.md` — párrafo de `data.ai` (~355-378) y «Observability» (~551-571).
- `docs/risks-and-open-questions.md` (~28) — bullet «Errores y reintentos de la IA».
- `docs/adr/0003-ai-provider.md` — proveedor/clave; punto natural para trazar el modo verboso temporal.
- `docs/domain/taxonomy.md` §5 — reglas 1-4 del Validador (4 = coherencia, con sus dos partes).

### Existing Patterns To Follow

- **Política/costura por constructor con default**: `verboseDiagnostics: Boolean = false` (mismo patrón
  que `model`/`responseFormatEnabled`), para que producción lea el interruptor y los tests lo inyecten.
- **Campos aditivos con default** en `AiCallEvent` y enums de diagnóstico en `data.ai`
  (`AiResponseFormat` es el precedente): no rompen construcciones ni tests.
- **Longitudes/recuentos, nunca cuerpos** como base; el cuerpo crudo queda **detrás** del interruptor.
- **Lógica pura testeable en JVM + capa Android delgada**: el troceado y el formateo de líneas van en un
  objeto puro de `data.ai`; `MainActivity` solo itera y llama a `Log.d`.
- **Motivo = enum aceptado** (`DiscardReason`): el desglose es un `Map` sobre un enum **nuevo de
  diagnóstico**, no un valor nuevo de `DiscardReason`.
- **Tests con `MockEngine` y clave ficticia**; la no-fuga se prueba explícitamente.

### Current Gaps

- `INCOHERENT` es un único valor: el log no distingue 4a de 4b (el bug reportado).
- El decode descarta el `detail` y el título a propósito y no retiene la causa.
- `AiCallEvent` no lleva causa de incoherencia, ni detalle de descartes, ni contenido crudo.
- No existe interruptor de diagnóstico verboso ni helper de troceado para Logcat.
- El planner **no** ejecutó `bash init.sh` (no hay cambios de código en esta sesión); el implementer
  parte del verde de la sesión 076 (405 tests totales, 62 en `data.ai`) y corre el gate él mismo.

## Technical Approach

### 1. Desglose de `INCOHERENT` (siempre activo) — `data.ai`

Nuevo enum **público** en `AiDiagnostics.kt` (público porque aparece en el `AiCallEvent` público, como
`AiResponseFormat`):

```kotlin
/** Causa de un descarte [DiscardReason.INCOHERENT]: la regla 4a o la 4b del Validador. */
enum class AiIncoherentCause { TIME_DRIFT, ALLERGEN_COVERAGE }
```

`AiRecipePayload.decode` clasifica cada descarte `INCOHERENT` con una función pura **en `data.ai`** que
**no** toca el Validador:

```kotlin
/**
 * Regla 4a del Validador (deriva de tiempos) sobre la receta ya completada. Reutiliza la constante
 * publica del Validador para no duplicar el umbral. Como el Validador devuelve el PRIMER fallo y la
 * 4a se evalua antes que la 4b, un INCOHERENT que no sea 4a es necesariamente 4b.
 */
private fun incoherentCause(recipe: Recipe): AiIncoherentCause =
    if (abs(recipe.steps.sumOf { it.minutes } - recipe.totalMinutes) >
        RecipeValidator.MAX_STEP_TOTAL_DRIFT_MINUTES
    ) {
        AiIncoherentCause.TIME_DRIFT
    } else {
        AiIncoherentCause.ALLERGEN_COVERAGE // 4b
    }
```

`AiDecodeResult` gana un campo `incoherentByCause: Map<AiIncoherentCause, Int>` (solo causas presentes,
en orden de `AiIncoherentCause`, igual que `discardedByReason` usa el orden de `DiscardReason`).
`AiCallEvent` gana `incoherentByCause: Map<AiIncoherentCause, Int> = emptyMap()`. El `report(...)` ya
recibe el `decodeResult`: solo hay que propagar el campo.

> **Decisión sobre «idéntico al actual» (léase con el validador).** El texto de verificación contrasta
> «con el modo de depuración activado el evento incluye el detalle y el crudo» con «con el modo apagado
> el evento es idéntico al actual». Se interpreta que **«idéntico al actual» describe el payload del
> modo depuración** (sin títulos ni cuerpo crudo), porque el desglose de `INCOHERENT` (**A+B**) es un
> entregable **aparte y siempre activo** (así lo pide la entrada: resolver `discards={INCOHERENT=4}` sin
> poder saber por qué, en producción). Consecuencia verificable: **con el interruptor apagado y un lote
> sin `INCOHERENT`, la línea `SazonAi` es byte a byte la actual**; y con `INCOHERENT` solo se **añade**
> el segmento `incoherent={…}`. El único campo nuevo siempre presente en el evento es
> `incoherentByCause` (vacío cuando no hay `INCOHERENT`).

### 2. Modo depuración — interruptor y payload verboso

- **Interruptor**: nuevo parámetro de constructor
  `verboseDiagnostics: Boolean = false` en `DevExpertRecipeGenerator`. **Apagado por defecto**
  (release-safe y tests). `MainActivity` lo activa **solo en debug**: `verboseDiagnostics = BuildConfig.DEBUG`
  (no requiere tocar `app/build.gradle.kts`; `buildConfig` ya está habilitado).
- **Detalle de descartes** (solo en modo depuración): nuevo tipo **público** de diagnóstico en
  `AiDiagnostics.kt`:

```kotlin
data class AiDiscardDetail(
    val reason: DiscardReason,
    val cause: AiIncoherentCause? = null, // solo si reason == INCOHERENT
    val title: String? = null,            // null si el elemento no llego a decodificarse
)
```

`AiRecipePayload.decode` gana `verbose: Boolean = false`; cuando es `true` **y** hay descartes, rellena
`AiDecodeResult.discards: List<AiDiscardDetail>` (título tomado del draft decodificado o, si el elemento
es ilegible, del primitivo `title` del JSON bruto — mismo criterio que `RecipeValidator.titleOf`). Con
`verbose = false`, `discards` es `emptyList()`: **en producción no se retiene ningún título**.
- **Contenido crudo**: `AiCallEvent` gana `rawContent: String? = null`. `execute` rellena
  `report(..., rawContent = if (verboseDiagnostics) outcome.content else null)`; `decode` **no** lo
  duplica (el `content` ya está en la rama `AttemptOutcome.Success`).
- **Evento**: `AiCallEvent` gana, con default, `discards: List<AiDiscardDetail> = emptyList()` y
  `rawContent: String? = null`. La KDoc matiza la garantía: en el camino por defecto **nunca** clave,
  `Authorization`, cuerpo ni títulos; **en modo depuración** el evento lleva el `content` del modelo y
  los títulos, y **aun así nunca** la clave ni la cabecera.

### 3. Troceado y render en el log — `data.ai` puro + `MainActivity` delgado

Nuevo objeto puro (p. ej. `app/src/main/java/com/sazon/app/data/ai/AiVerboseDiagnostics.kt`), testeable
en JVM, sin Android:

```kotlin
object AiVerboseDiagnostics {
    /** Logcat trunca ~4000 char; 3_000 deja margen a la etiqueta y al prefijo `raw[i/n] `. */
    const val MAX_CHUNK_CHARS = 3_000

    /** `raw[i/n] <trozo>` por cada trozo; nunca corta un par suplente. Vacio/null -> lista vacia. */
    fun rawLines(rawContent: String?, maxChars: Int = MAX_CHUNK_CHARS): List<String>

    /** `discard[i/n] INCOHERENT/TIME_DRIFT title="…"` (o `title=(sin titulo)`); sanea newlines. */
    fun discardLines(discards: List<AiDiscardDetail>): List<String>
}
```

`MainActivity` mantiene la línea base **como está** y añade, de forma condicional, el segmento
`incoherent=` (solo si hay causas) y las líneas verbosas (solo si el evento las trae). Forma
equivalente, sin cambiar el orden actual de los campos:

```kotlin
val causes = if (event.incoherentByCause.isEmpty()) "" else "incoherent=${event.incoherentByCause} "
Log.d(
    "SazonAi",
    "${event.operation} ${event.outcome} attempt=${event.attempt} " +
        "status=${event.httpStatus} ${event.durationMillis}ms " +
        "format=${event.responseFormat} " +
        "content=${event.contentChars}c/${event.contentBytes}b " +
        "received=${event.received} accepted=${event.accepted} " +
        "discards=${event.discardedByReason} " + causes + "${event.detail}",
)
// Solo si el evento trae payload verboso (el generador ya lo decidio):
AiVerboseDiagnostics.discardLines(event.discards).forEach { Log.d("SazonAi", it) }
AiVerboseDiagnostics.rawLines(event.rawContent).forEach { Log.d("SazonAi", it) }
```

Con `incoherentByCause` vacío (lote sin `INCOHERENT`) la línea base es exactamente la de hoy.

El modo lo decide **un solo sitio** (el generador, por el constructor); `MainActivity` es un renderizador:
si el evento no trae `discards`/`rawContent`, no emite líneas extra.

## Expected File Changes

**Modificar (código)**

- `app/src/main/java/com/sazon/app/data/ai/AiDiagnostics.kt` — `AiIncoherentCause`, `AiDiscardDetail` y
  3 campos con default en `AiCallEvent` (`incoherentByCause`, `discards`, `rawContent`); KDoc.
- `app/src/main/java/com/sazon/app/data/ai/AiDecodeResult.kt` — `incoherentByCause` y `discards`.
- `app/src/main/java/com/sazon/app/data/ai/AiRecipePayload.kt` — firma `decode(..., verbose: Boolean = false)`,
  clasificador `incoherentCause(recipe)`, recuento de causas y lista de descartes (solo verbose).
- `app/src/main/java/com/sazon/app/data/ai/DevExpertRecipeGenerator.kt` — costura
  `verboseDiagnostics: Boolean = false`; `decode(outcome.content, taxonomy, verbose = verboseDiagnostics)`;
  `report(..., rawContent = …)`; propagar `incoherentByCause`/`discards`.
- `app/src/main/java/com/sazon/app/MainActivity.kt` — `verboseDiagnostics = BuildConfig.DEBUG` y línea
  `SazonAi` con `incoherent={…}` + líneas verbosas vía `AiVerboseDiagnostics`.

**Crear**

- `app/src/main/java/com/sazon/app/data/ai/AiVerboseDiagnostics.kt` — troceado y formateo puros.
- `app/src/test/java/com/sazon/app/data/ai/AiVerboseDiagnosticsTest.kt` — tests del helper.

**Modificar (tests)**

- `app/src/test/java/com/sazon/app/data/ai/AiRecipePayloadTest.kt` — desglose 4a/4b (Scenario 1),
  coherencia con `RecipeValidator.validateRecipe` (Scenario 2) y `discards` con/sin `verbose`.
- `app/src/test/java/com/sazon/app/data/ai/DevExpertRecipeGeneratorTest.kt` — evento con
  `incoherentByCause` (Scenario 1), interruptor apagado sin detalle/crudo (Scenario 3), encendido con
  detalle + crudo (Scenario 4) y no-fuga ampliada (Scenario 5).

**No se toca**: `Recipe.kt`, `RecipeValidator.kt`, `RecipeConstraints`/`DiscardReason`/`DiscardedRecipe`,
`AiRecipeDraft.kt`, `AiRecipeIds.kt`, `AiProviderPolicy.kt`, `OpenAiChat.kt`, `AiHttpClientFactory.kt`,
`domain/**`, `ui/**`, `data/generator/**`, las bases Room, `app/src/main/assets/**`,
`app/build.gradle.kts`, `gradle/libs.versions.toml`, `AndroidManifest.xml`, `init.sh`, `scripts/**`,
`DESIGN.md`, `docs/design/**`.

## Visual Design Impact

- UI involved: **no** (`area: integration`). No hay pantalla, estado, microcopy, token ni acabado
  nuevos; el único cambio fuera de `data.ai` es el **log** de `MainActivity`.
- Design source: not applicable. Screens or states affected: ninguno.
- New design artifact required: **no**. `docs/design/concepts/ai-verbose-diagnostics/` **no** es
  necesario y la feature **no** se bloquea por capturas de Stitch.

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — párrafo de `data.ai` (~355-378): el decode además
  desglosa `INCOHERENT` en `AiIncoherentCause` y, en modo depuración, retiene el detalle y el crudo;
  «Observability» (~551-571): `AiCallEvent` con `incoherentByCause`, el interruptor `verboseDiagnostics`
  (debug-only, temporal) con `discards`/`rawContent` y el troceado; la garantía de no-fuga se matiza
  (en depuración el cuerpo del modelo sí aparece; la clave y `Authorization` nunca).
- `docs/risks-and-open-questions.md`: **update** — nuevo bullet de **deuda temporal**: «Diagnóstico
  verboso de IA» activo solo en debug, **apagado en release**, con contenido crudo y títulos; criterio de
  retirada (una vez entendida la causa de `INCOHERENT`/ajustado el prompt) y su coste (volumen de log).
  Matizar el bullet «Errores y reintentos de la IA» con el desglose 4a/4b.
- `docs/adr/0003-ai-provider.md`: **update mínimo (nota)** — el diagnóstico verboso es una ayuda de
  desarrollo **temporal** y debug-only; no cambia la decisión de proveedor ni el contrato.
- `docs/domain-model.md`, `docs/domain/taxonomy.md`: **not needed** — no cambian entidades, estados ni
  reglas; el desglose es un diagnóstico en `data.ai` (la regla 4 ya documenta sus dos partes).
- `AGENTS.md`, `DESIGN.md`, `docs/design/**`: **not needed** — no cambian flujo de agentes, arranque ni
  dirección visual.
- `PROGRESS.md` y `feature_list.json`: **update al implementar/validar**, no en esta sesión.

## Implementation Plan

1. `AiDiagnostics`: `AiIncoherentCause`, `AiDiscardDetail` y los 3 campos con default en `AiCallEvent`.
2. `AiDecodeResult`: `incoherentByCause` y `discards`.
3. `AiRecipePayload.decode`: parámetro `verbose`, clasificador `incoherentCause`, recuento de causas y
   construcción de `discards` (título del draft o del `title` bruto) solo si `verbose`.
4. `DevExpertRecipeGenerator`: costura `verboseDiagnostics`; pasar `verbose` al decode; propagar
   `rawContent` (`outcome.content` solo si verbose), `incoherentByCause` y `discards` en `report`.
5. `AiVerboseDiagnostics`: `MAX_CHUNK_CHARS`, `rawLines` (troceado sin cortar pares suplentes) y
   `discardLines` (sanea newlines).
6. `MainActivity`: `verboseDiagnostics = BuildConfig.DEBUG`; `incoherent={…}` condicional + líneas
   verbosas si el evento las trae.
7. Tests (helper + decode + generador) y `bash scripts/gradle.sh testDebugUnitTest --tests
   "com.sazon.app.data.ai.*"` + `bash init.sh` en verde; actualizar las tres docs.

## Implementation Tasks

- [ ] `AiDiagnostics`: `enum AiIncoherentCause { TIME_DRIFT, ALLERGEN_COVERAGE }`, `data class
      AiDiscardDetail(reason, cause = null, title = null)` y en `AiCallEvent`
      `incoherentByCause: Map<AiIncoherentCause, Int> = emptyMap()`, `discards: List<AiDiscardDetail> =
      emptyList()`, `rawContent: String? = null`; KDoc actualizada (garantía matizada).
- [ ] `AiDecodeResult`: `incoherentByCause: Map<AiIncoherentCause, Int>` y `discards: List<AiDiscardDetail>`.
- [ ] `AiRecipePayload.decode(content, taxonomy, constraints = NEUTRAL, verbose: Boolean = false)`:
      por cada `Discarded(INCOHERENT)` incrementa `incoherentCause(recipe)` (regla 4a primero; si no,
      `ALLERGEN_COVERAGE`); si `verbose`, añade `AiDiscardDetail(reason, cause, title)`; `discards` vacío
      cuando `verbose = false`; sigue **sin lanzar** y con restricciones neutras por defecto.
- [ ] `DevExpertRecipeGenerator`: `verboseDiagnostics: Boolean = false`; `decode(..., verbose = verboseDiagnostics)`;
      `report(decodeResult, rawContent = if (verboseDiagnostics) outcome.content else null, …)` y
      propagación de `incoherentByCause`/`discards`. Timeouts, reintentos, fallback y `isRetryableStatus`
      **intactos**.
- [ ] `AiVerboseDiagnostics` (nuevo, puro): `MAX_CHUNK_CHARS = 3_000`, `rawLines` (trocea; no corta un
      par suplente; `null` → vacío) y `discardLines` (título saneado; `(sin titulo)` si falta).
- [ ] `MainActivity`: `verboseDiagnostics = BuildConfig.DEBUG`; línea base con `incoherent={…}` solo si
      no está vacío; líneas `discard[i/n]`/`raw[i/n]` si el evento las trae (`Log.d("SazonAi", …)`).
- [ ] `AiVerboseDiagnosticsTest` (nuevo): reconstrucción por concatenación, cada línea ≤ límite, índices
      `[i/n]`, sin cortar pares suplentes, `null`→sin líneas, newlines saneados y sin fuga de
      `test-key`/`Bearer`/`Authorization`.
- [ ] `AiRecipePayloadTest`: 4a/4b con `incoherentByCause` (Scenario 1), coherencia con
      `RecipeValidator.validateRecipe` para (solo 4a / solo 4b / ambas / ninguna) (Scenario 2) y
      `discards` con/sin `verbose`.
- [ ] `DevExpertRecipeGeneratorTest`: evento con `incoherentByCause` sin depuración (Scenario 1);
      apagado ⇒ `discards` vacío y `rawContent` null, y sin título/crudo en `toString()` (Scenario 3);
      encendido ⇒ detalle con título y `rawContent` igual al `content` del modelo (Scenario 4);
      no-fuga de clave/`Authorization` en ambos modos (Scenario 5).
- [ ] `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"` y `bash init.sh` en
      verde (0 errores de lint y sin warnings nuevos en ficheros de la feature).
- [ ] Inspección de alcance: `git diff --stat` solo con lo previsto; `Recipe.kt`, `RecipeValidator.kt`,
      `AiProviderPolicy.kt`, `AiHttpClientFactory.kt`, `OpenAiChat.kt`, `domain/**`, `ui/**`, build,
      Taxonomía, `seed-recipes.json` e `init.sh` intactos; sin dependencias nuevas.
- [ ] Docs durables (`technical-discovery.md`, `risks-and-open-questions.md`, nota en
      `adr/0003-ai-provider.md`).

## Verification Plan

- **Automática (la que decide):** `bash init.sh` (exit 0) y, para tiradas focales,
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"`. **Nunca** `./gradlew`
  directo ni `--stop` (AGENTS.md).
- `AiRecipePayloadTest` — Scenario 1: lote con una receta 4a y una 4b →
  `discardedByReason = {INCOHERENT: 2}` y `incoherentByCause = {TIME_DRIFT: 1, ALLERGEN_COVERAGE: 1}`.
  Scenario 2: batería (solo 4a / solo 4b / ambas / ninguna) contrastada con
  `RecipeValidator.validateRecipe` (ambas → `TIME_DRIFT`); orden del map determinista. `discards` no
  vacío con `verbose = true` (con títulos) y vacío con `verbose = false`.
- `DevExpertRecipeGeneratorTest` — Scenario 1: cuerpo con `INCOHERENT` → evento `incoherentByCause`;
  Scenario 3: por defecto `discards` vacío y `rawContent` null; `toString()` sin el `content` ni títulos;
  Scenario 4: con `verboseDiagnostics = true`, `discards` con `title` y `rawContent` = al `content`
  servido por el `MockEngine`; Scenario 5: ningún `toString()` ni línea contiene `test-key`/`Bearer`/
  `Authorization`.
- `AiVerboseDiagnosticsTest` — troceado y formateo puros: `joinToString("")` de los trozos == contenido
  original; longitud de cada línea ≤ `MAX_CHUNK_CHARS + prefijo`; `null` → lista vacía; newlines/títulos
  saneados; sin fuga de clave/`Authorization`.
- Regresión: los **62** tests de `data.ai` siguen contando y en verde (los nuevos suman);
  `RecipeValidatorTest` intacto; timeouts 120/150/240, `MAX_ATTEMPTS`/backoff/`Retry-After` y
  `response_format`/fallback sin cambios.
- **Lint:** `bash init.sh` → 0 errores y sin warnings nuevos (los preexistentes del build se mantienen).
- **Inspección:** `git diff --stat` limitado a lo previsto; `grep` de `discards`/`rawContent`/
  `incoherentByCause` solo en `data.ai` y `MainActivity`; `RecipeValidator.kt`/`DiscardReason` intactos;
  `AiHttpClientFactory.kt`/`AiProviderPolicy.kt` sin cambios.
- **Cobertura E2E:** no aplica (no hay suite E2E persistente; `MockEngine` cubre el wiring y el log en
  JVM, y lo manual lo revisa la persona). `init.sh` **no** cambia.
- **Manual (la persona, emulador de Android Studio en Windows; no automatizable desde WSL):** build
  **debug**; buscar `arroz con pollo`; ver en logcat el desglose `incoherent={…}`, el detalle por
  descarte (`discard[i/n]`) y el crudo troceado (`raw[i/n]`); con un build **release** (o
  `verboseDiagnostics = false`) confirmar que el log vuelve al formato anterior; clave ausente → modo
  semilla.

## Evidence To Capture

- `bash init.sh` (exit 0) y tirada focal `com.sazon.app.data.ai.*` con recuento de tests y 0 fallos
  (esperado: 62 preexistentes en `data.ai` más los nuevos, sin eliminar ninguno).
- Nombres y resultado de los tests nuevos: desglose 4a/4b, coherencia con el Validador (ambas →
  `TIME_DRIFT`), interruptor apagado sin detalle/crudo, encendido con detalle + crudo, troceado
  reconstruible y no-fuga de clave/`Authorization`.
- `git diff --stat` de lo tocado y confirmación de que `Recipe.kt`, `RecipeValidator.kt`,
  `AiProviderPolicy.kt`, `AiHttpClientFactory.kt`, `OpenAiChat.kt`, `domain/**`, `ui/**`, build,
  Taxonomía, `seed-recipes.json`, `init.sh` y `DESIGN.md` no cambian.
- Resultado **manual** del emulador (logcat `SazonAi` con `incoherent={…}`, `discard[i/n]` y `raw[i/n]`
  en debug; formato anterior en release; clave ausente → modo semilla).
- Docs actualizadas y anotación de estado/evidencia en `feature_list.json` y `PROGRESS.md`.

## Decisions And Unknowns

### Decisiones

- **El desglose 4a/4b es siempre activo y no toca el Validador.** El problema reportado
  (`discards={INCOHERENT=4}` sin motivo) tiene que resolverse en el diagnóstico de producción, no solo
  en un modo de depuración; por eso `incoherentByCause` es un campo base (vacío si no hay `INCOHERENT`,
  con lo que el log queda byte a byte igual en ese caso). Se evita cambiar `DiscardReason` (vocabulario
  de dominio) o la decisión del Validador (sigue siendo la puerta dura).
- **Clasificación reutilizando el orden del Validador**: como `validateRecipe` devuelve el **primer**
  fallo y la 4a se evalúa antes que la 4b, un `INCOHERENT` que no sea deriva de tiempos es 4b; solo se
  recomputa la 4a reutilizando la constante pública `MAX_STEP_TOTAL_DRIFT_MINUTES`. Se fija con un test
  de coherencia frente a `RecipeValidator.validateRecipe` (Scenario 2). Alternativa considerada y
  **descartada por alcance**: exponer la causa de forma estructurada desde `RecipeValidator` (tocaría
  el dominio aceptado).
- **Interruptor por constructor (`verboseDiagnostics: Boolean = false`) + `BuildConfig.DEBUG` en
  `MainActivity`**: apagado por defecto (release-safe y tests), activo solo en debug (como sugiere la
  entrada), sin tocar `app/build.gradle.kts` y sin acoplar `AiProviderPolicy` (que es puro y JVM) a
  `BuildConfig`.
- **Crudo = `choices[0].message.content`** (no el envelope HTTP completo): es lo que el decode ya
  recibe y lo que pidió la persona. `decode` no lo duplica; el generador lo pone en el evento solo en
  modo depuración.
- **Troceado en un objeto puro** (`AiVerboseDiagnostics`) en lugar de dentro de `Log.d`: es testeable en
  JVM (reconstrucción, límite y saneado) y deja `MainActivity` como capa delgada.
- **El modo lo decide el generador, no el log**: `MainActivity` emite líneas verbosas solo si el evento
  las trae; así el interruptor vive en un único sitio.

### Unknowns

- **Duplicación mínima de la regla 4a**: si el Validador cambiara su umbral o la evaluación de la 4a, el
  clasificador podría desviarse; se mitiga reutilizando su constante y con el test de coherencia. Si en
  el futuro se prefiere una única fuente, sería una feature sobre el dominio.
- **Volumen de log en modo depuración**: 15 KB medidos → ~5 líneas de ~3 KB por llamada, más una línea
  por descarte; Logcat puede dar rate-limit con muchas llamadas. Es una ayuda **temporal** (debug-only) y
  se traza en riesgos.
- **Títulos/`detail` en modo depuración**: el título es texto del modelo y se registra **solo** con el
  interruptor; en el camino por defecto no se retiene. La clave y `Authorization` nunca aparecen (no
  forman parte del `content`).
- **Interpretación de «idéntico al actual»**: documentada arriba; el campo base siempre presente es
  `incoherentByCause` (vacío cuando no hay `INCOHERENT`). Si la persona quiere el modo apagado
  literalmente sin ningún campo nuevo, se resolvería gateando también el desglose (cambio trivial, pero
  dejaría el bug reportado sin explicar en producción).
- **`content` en blanco/ilegible** (ya conocido): `rawContent` puede ser `null`/vacío; el helper no
  emite líneas con `null`. Distinguirlos exigiría registrar el cuerpo HTTP crudo, hoy fuera de alcance.

## Validator Checklist

- [ ] El resumen **distingue** las dos causas de `INCOHERENT` (`TIME_DRIFT` = 4a, `ALLERGEN_COVERAGE` =
      4b) en el evento y en el log, **sin** exponer texto de recetas, y con el interruptor de depuración
      **apagado**.
- [ ] La clasificación es coherente con `RecipeValidator.validateRecipe` (solo 4a / solo 4b / ambas →
      `TIME_DRIFT` / ninguna → aceptada) y el orden del map es determinista.
- [ ] Con `verboseDiagnostics = false` (por defecto): `discards` vacío, `rawContent` null y `toString()`
      del evento **sin** el contenido del modelo ni títulos; con un lote sin `INCOHERENT`, la línea
      `SazonAi` es la actual.
- [ ] Con `verboseDiagnostics = true`: el evento lleva el detalle por descarte (con título) y el
      `content` del modelo; `MainActivity` lo trocea en líneas `raw[i/n]` reconstruibles y por debajo del
      límite de Logcat.
- [ ] **Ningún** evento ni línea contiene la clave ni `Authorization`/`Bearer`, en ningún modo.
- [ ] **El Validador y el dominio quedan intactos**: `RecipeValidator.kt`, `DiscardReason`,
      `RecipeValidation`/`DiscardedRecipe`, `Recipe.kt`; el desglose es un diagnóstico de `data.ai`.
- [ ] Alcance: solo `data.ai` (código + sus tests), `MainActivity` (wiring/log) y las tres docs;
      `AiProviderPolicy.kt`, `AiHttpClientFactory.kt`, `OpenAiChat.kt`, `domain/**`, `ui/**`, Room,
      Taxonomía, `seed-recipes.json`, build, `init.sh`, `scripts/**`, `DESIGN.md` y `docs/design/**`
      intactos; sin dependencias nuevas.
- [ ] `bash init.sh` en verde (test + lint, 0 errores y sin warnings nuevos) y regresión de los 62 tests
      de `data.ai` y de `RecipeValidatorTest`.
- [ ] `Visual Design Impact` sin capturas (no hay UI) y la verificación **manual** en emulador anotada
      como tal (logcat con `incoherent={…}` y, en debug, `discard[i/n]`/`raw[i/n]`; release → formato
      anterior; clave ausente → modo semilla), no automatizable desde WSL.
- [ ] El modo verboso queda trazado como **temporal** (debug-only, apagado en release) en
      `docs/risks-and-open-questions.md` (y nota en `adr/0003-ai-provider.md`).
- [ ] `feature_list.json` y `PROGRESS.md` actualizados por implementer/validador/orquestador.
