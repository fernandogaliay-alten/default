# Feature Implementation Spec: Tolerar cantidad nula en el wire de la IA (ingredientes al gusto)

## Source Feature

- `id`: `ai-quantity-tolerance` · `area`: `integration` · `depends_on`: `["ai-recipe-decode"]`
  (ya `accepted`, sesión 071) · `status`: `not_started` · `source`: `feature_list.json`
- **No es feature de UI**: sin pantallas, estados, microcopy ni capturas. El comportamiento visible ya
  existe (Resultados y Detalle); lo que cambia es que una receta con ingredientes "al gusto" deja de
  descartarse entera y **vuelve a llegar** a esas pantallas.
- Esta spec es el contrato planner → implementer → validator. El planner **no** modifica
  `feature_list.json` ni `PROGRESS.md`; el estado (`in_progress` → `passing` → `accepted`) lo mueven
  implementer/validador/orquestador.
- Verificación exigida (textual): (1) `./gradlew test` (vía `bash init.sh`) cubre que un ingrediente del
  wire con `quantity` **null** (con `unit` null, con `unit` no nulo o con el campo **ausente**) **no**
  tumba la receta: se normaliza a "Al gusto" (`unit` null) y la receta se valida y acepta; una
  `quantity` **numérica se conserva intacta**; un lote con una receta "al gusto" y una rota sigue
  aceptando la buena; (2) prueba **manual** en emulador: con clave válida, buscar *"arroz con pollo"* y
  comprobar que las recetas con ingredientes "al gusto" (p. ej. *perejil fresco*) ya no caen por
  `SCHEMA` y llegan a Resultados; el Detalle muestra "Al gusto" en esos ingredientes.

**Contexto (correctiva de `ai-recipe-decode`, pedida por la persona tras
`ai-verbose-diagnostics`).** Causa raíz confirmada con el JSON crudo del modelo: `RecipeIngredient`
de `domain.recipe` declara `quantity: Double` **no anulable** (y `AiRecipeDraft` lo reutiliza tal cual
en `ingredients: List<RecipeIngredient>`), mientras el modelo emite ingredientes **"al gusto"** con
`"quantity": null` y `"unit": null` (p. ej. *perejil fresco*). `AiRecipePayload.decode` decodifica cada
elemento con `json.decodeFromJsonElement(AiRecipeDraft.serializer(), element)`: al no poder mapear un
`quantity` nulo a un `Double` no anulable, ese elemento se salta y suma un `DiscardReason.SCHEMA`, de
modo que la **receta completa** se pierde aunque el resto de campos fuese válido. Evidencia: de 6
recetas recibidas, las 3 con `quantity` nulo (español, mediterráneo, risotto) cayeron por `SCHEMA` y las
3 sin él (curry, asiático, mexicano) pasaron; los ids de Taxonomía usados eran todos válidos y el
catálogo semilla no usa ninguna `quantity` nula.

## Goal

Que el adaptador real (`com.sazon.app.data.ai`) **tolere el ingrediente "al gusto"** del wire del
modelo y lo normalice a un `RecipeIngredient` de dominio válido, de forma que un ingrediente con
`quantity` nula o ausente **no descarte la receta entera**. El cambio vive en la capa anti-corrupción
`data.ai` (`AiRecipeDraft`): un DTO de ingrediente del wire con `quantity` anulable (campos
tolerantes) que mapea a `RecipeIngredient`; cuando `quantity` es nulo, se normaliza a **"Al gusto"**
(`quantity = 0.0`, `unit = null`).

La frontera de dominio **no se debilita**: `RecipeIngredient.quantity` sigue siendo `Double` no
anulable en `domain.recipe`, y `Recipe.kt`, `RecipeValidator.kt` y la UI **(Detalle/`IngredientFormat`,
`ServingScaler`)** **no se tocan**. La razón de que `0.0 + unit = null` sea suficiente es que la app ya
tiene la semántica aceptada de "al gusto": `IngredientFormat.quantityLabel` pinta `"Al gusto"` siempre
que `unit == null` (ignora la cantidad) y `ServingScaler.scaleQuantity` **no escala** cuando
`unit == null`; `MatchScore` ignora `quantity`/`unit` y `RecipeValidator` nunca lee `quantity`
(verificado por inspección). Resultado: con clave válida, las recetas con ingredientes "al gusto"
vuelven a mostrarse en Resultados y el Detalle ya las pinta como "Al gusto".

## Non-Goals

- **No** se cambia el dominio aceptado: `Recipe.kt` (`RecipeIngredient.quantity` sigue `Double` no
  anulable), `RecipeValidator.kt`, `RecipeJson.kt`, `domain/**` quedan **intactos**.
- **No** se toca la UI (`ui/**`), en particular `ui/detail/IngredientFormat.kt`,
  `ui/detail/ServingScaler.kt` ni `RecipeDetailScreen`: el render de "Al gusto" ya está aceptado.
- **No** se cambia la política de ids (`AiRecipeIds.PREFIX`/`ALGORITHM_VERSION`); regenerar el mismo
  contenido sigue conservando el `id` (ver *Decisiones*: la canonicalización pasa por el mapeo ya
  normalizado, sin alterar los ids de las recetas que ya se decodificaban).
- **No** se toca la **deriva de tiempos** (`INCOHERENT`/`TIME_DRIFT`) ni
  `RecipeValidator.MAX_STEP_TOTAL_DRIFT_MINUTES`; el curry que cae por deriva es **fuera de alcance**
  (feature aparte si se pide).
- **No** se relajan otras reglas del Validador: la receta normalizada sigue pasando por
  `RecipeValidator.validateRecipe` con `AiProviderPolicy.NEUTRAL_CONSTRAINTS`.
- **No** se cambia el prompt (`PromptBuilder`) ni se pide al modelo cantidades concretas; el contrato
  §4 sigue siendo el mismo (el modelo puede seguir marcando "al gusto").
- **No** se cambia la petición ni la política de red (`AiProviderPolicy`, `OpenAiChat`,
  `AiHttpClientFactory`), ni el diagnóstico (`AiDiagnostics`, `AiDecodeResult`, `AiVerboseDiagnostics`).
- **No** se añaden dependencias ni cambios de build (`app/build.gradle.kts`,
  `gradle/libs.versions.toml`, `AndroidManifest.xml`, `init.sh`, `scripts/**`, `DESIGN.md`,
  `docs/design/**` no se tocan).
- **No** se reescriben las specs históricas (`ai-recipe-decode.md`, `real-ai-provider.md`,
  `ai-call-diagnostics.md`); como mucho, una nota de hallazgo si el implementer lo considera útil.

## Job Story

Como usuario local con clave válida,
quiero que una receta que el modelo devuelve con algún ingrediente "al gusto" (`quantity` nula) se
acepte y se me muestre, en lugar de desaparecer entera por un detalle de formato,
para no perder recetas reales (p. ej. un risotto con *perejil fresco* "al gusto") en el Generador y en
la Búsqueda con IA.

## Users And Permissions

- **Usuario local**: dueño único del dispositivo (ADR 0002). No cambia lo que puede hacer; recupera
  recetas que antes se descartaban. Puede seguir cancelando la carga.
- **Desarrollador / agente**: verifica en **JVM** (sin red ni clave real) con cuerpos **literales** del
  contrato §4 que contienen `"quantity": null`, `unit` no nulo con `quantity` nula y `quantity`
  **ausente**, más el caso numérico intacto y el lote mixto con una receta rota.
- **Sistema**: `AiRecipePayload.decode` sigue siendo **puro y tolerante** (nunca lanza; un elemento
  roto no tumba el lote) y sigue aplicando las reglas del Validador con restricciones neutras.

## Acceptance Scenarios

### Scenario 1: `quantity` nula con `unit` nula → receta aceptada como "Al gusto"

Given un `content` = `{"recipes":[<receta válida del contrato §4>]}` donde un ingrediente trae
`{"name":"Perejil fresco","quantity":null,"unit":null,"isPantry":true,...,"ingredientId":"perejil"}` y
la Taxonomía real,
When se llama a `AiRecipePayload.decode(content, taxonomy)`,
Then la receta aparece en `accepted`, y ese ingrediente tiene `quantity == 0.0` y `unit == null`
(equivalente a "Al gusto"); `IngredientFormat.quantityLabel(0.0, null) == "Al gusto"`.

### Scenario 2: `quantity` nula con `unit` **no** nula → también "Al gusto"

Given un ingrediente del wire con `"quantity": null` y `"unit": "g"`,
When se decodifica,
Then el ingrediente normalizado queda `quantity == 0.0` y `unit == null` (la `unit` del wire se
**descarta**: con cantidad desconocida, la unidad no significa nada) y la receta se acepta.

### Scenario 3: `quantity` **ausente** → mismo resultado que `null`

Given un ingrediente del wire **sin** el campo `quantity` (y sin `unit`),
When se decodifica,
Then se normaliza igual que en el Scenario 1 (`quantity == 0.0`, `unit == null`) y la receta se acepta.

### Scenario 4: `quantity` numérica se conserva intacta

Given un ingrediente del wire con una `quantity` numérica (p. ej. `"quantity": 200, "unit": "g"`),
When se decodifica,
Then el ingrediente sale con `quantity == 200.0` y `unit == MeasureUnit.GRAM` **sin cambios** (misma
semántica que antes de esta feature).

### Scenario 5: verificación exigida en lote mixto (una "al gusto" y una rota)

Given un `content` con una receta válida que incluye un ingrediente "al gusto" y **otra** receta rota
(p. ej. `{"title":"Rota"}`),
When se decodifica,
Then la receta "al gusto" aparece en `accepted`, `received == 2` y `discardedByReason == {SCHEMA: 1}`
(el elemento roto sigue contando y no tumba a la buena).

### Scenario 6: la normalización **no** salta al Validador

Given un ingrediente con `quantity` nula dentro de una receta con otro campo inválido (p. ej.
`mealTypeId` inexistente o deriva de tiempos > 5 min),
When se decodifica,
Then esa receta se **descarta** por `SCHEMA`/`INCOHERENT` como siempre: tolerar la cantidad **no**
relaja ninguna regla del Validador ni pre-filtra por dieta/restricciones (restricciones neutras).

### Scenario 7: `id`/`source`/`matchScore` siguen siendo de la app

Given una receta con ingredientes "al gusto" decodificada con éxito,
When se inspecciona,
Then la receta lleva `source == RecipeSource.AI`, `id` con prefijo `ai-` (`AiRecipeIds`) y
`matchScore == null`, exactamente como las demás recetas `ai`; la canonicalización del `id` usa el
ingrediente **ya normalizado** (así `"quantity":null`, `quantity` ausente y `quantity: 0.0`+`unit:null`
comparten `id`), y los ids de las recetas que ya se decodificaban **no cambian**.

### Scenario 8: prueba manual en emulador (la persona; no automatizable desde WSL)

Given `devexpert.apiKey` válida en `local.properties` y el emulador de Android Studio en **Windows**,
When se busca *"arroz con pollo"* (o se genera desde el Generador),
Then las recetas con ingredientes "al gusto" (español, mediterráneo, risotto con *perejil fresco*) ya
**no** caen por `SCHEMA` y llegan a Resultados; al abrir el Detalle, esos ingredientes muestran
"Al gusto". Con la clave ausente o vaciada, la app sigue en **modo semilla** sin fallar.

## Repository Research

### Files Inspected

- `feature_list.json` — entrada `ai-quantity-tolerance` (causa raíz y alcance acordado) y
  `ai-recipe-decode` (correctiva padre, `accepted`).
- `docs/specs/ai-recipe-decode.md` — convenciones de esta familia de correctivas y los escenarios ya
  aceptados del decode (tolerancia, `id`/`source`, reglas del Validador, `matchScore`).
- `docs/domain/taxonomy.md` §4 (contrato JSON: el ingrediente es
  `{ "name", "quantity", "unit", "isPantry" }`; `id`/`source`/`matchScore` los pone la app) y §5
  (reglas 1–4 del Validador).
- `docs/technical-discovery.md` — párrafos de `data.ai` (el decode "DTO del wire `AiRecipeDraft` …
  completa `id`/`source` … `RecipeValidator.validateRecipe` con restricciones neutras") y de
  "Testing and Verification", que habrá que matizar con la tolerancia del ingrediente.
- `docs/domain-model.md` §`RecipeIngredient` (`name`, `quantity`, `unit`, `isPantry`, `ingredientId`) y
  la nota de escalado (`unit = null` "al gusto" no se escala).
- `app/src/main/java/com/sazon/app/data/ai/AiRecipeDraft.kt` — **sitio del cambio**: el ingrediente se
  declara como `List<RecipeIngredient>` (dominio, `quantity` no anulable) y `toRecipe(id)` lo mapea 1:1.
- `app/src/main/java/com/sazon/app/data/ai/AiRecipePayload.kt` — `decode` decodifica elemento a
  elemento con `AiRecipeDraft.serializer()`; si falla, salta el elemento y suma `SCHEMA`. **No cambia**
  (su contraste `runCatching` ya tolera el fallo del ingrediente mal tipado una vez el DTO lo acepte).
- `app/src/main/java/com/sazon/app/data/ai/AiRecipeIds.kt` — `canonical(draft)` recorre
  `draft.ingredients` (hoy `RecipeIngredient`) para la cadena canónica del `id`. **Cambio mínimo** por
  el tipo nuevo.
- `app/src/main/java/com/sazon/app/domain/recipe/Recipe.kt` — `RecipeIngredient.quantity: Double`
  (no anulable); `MeasureUnit?` ya es nullable con default `null`. **No cambia**.
- `app/src/main/java/com/sazon/app/domain/validation/RecipeValidator.kt` — reglas 1–4: **nunca lee
  `quantity`** (solo `name`/`ingredientId` para alérgenos y `steps.minutes`/`totalMinutes`). Es la costura
  que se reutiliza. **No cambia**.
- `app/src/main/java/com/sazon/app/ui/detail/IngredientFormat.kt` — `quantityLabel` devuelve
  `"Al gusto"` cuando `unit == null` (ignora la cantidad): justifica normalizar a `0.0` + `unit = null`.
- `app/src/main/java/com/sazon/app/ui/detail/ServingScaler.kt` — `scaleQuantity` devuelve la cantidad
  sin tocar cuando `unit == null`: no escala "al gusto". **No cambia**.
- `app/src/main/java/com/sazon/app/ui/results/MatchScore.kt` — documenta que ignora
  `quantity`/`unit`/`isPantry`. **No cambia**.
- `app/src/test/java/com/sazon/app/data/ai/AiRecipePayloadTest.kt` — fixtures **literales** del
  contrato (nunca `RecipeJson.encode`), con helpers `draftJson(...)`/envelope: donde se añaden los
  escenarios de cantidad nula/ausente. **Cambia** (tests).
- `app/src/test/java/com/sazon/app/data/ai/AiRecipeIdsTest.kt` — construye `AiRecipeDraft` con
  `ingredients: List<RecipeIngredient>`: **debe adaptarse** al tipo nuevo del wire.
- `app/src/test/java/com/sazon/app/data/ai/DevExpertRecipeGeneratorTest.kt` — regresión de proveedor
  (MockEngine) y usos de `AiRecipeDraft`; no debe romperse.
- `init.sh` y `scripts/gradle.sh` — `bash init.sh` = `./gradlew test` + `./gradlew lint`; sin modos
  nuevos.

### Existing Patterns To Follow

- **Capa anti-corrupción en `data.ai`**: el DTO del wire traduce el formato del proveedor al modelo de
  dominio; el dominio no conoce el wire (ya lo hacen `AiRecipeDraft`/`AiRecipePayload`/`AiRecipeIds`).
- **DTO tolerante con defaults**: `AiRecipeDraft` ya usa `tips = emptyList()`,
  `nutrition = null`, `declaredAllergens = emptyList()` y `ignoreUnknownKeys = true`; el DTO del
  ingrediente sigue la misma disciplina (`isPantry = false`, `ingredientId = null`, `quantity = null`,
  `unit = null`).
- **Normalización en el mapeo wire → dominio**, no en el dominio: la UI y el Validador ya están
  aceptados y no deben cambiar.
- **Tests JUnit 4 en JVM**, Taxonomía real por `System.getProperty("taxonomy.asset.path")`, cuerpos
  **literales** del contrato §4 y casos negativos que no pasan en vacío; nombres en español con `_`.
- **Comandos**: `bash init.sh` como gate; tiradas focales con
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"`. **Nunca** `./gradlew`
  directo ni `--stop` (AGENTS.md).

### Current Gaps

- No existe una representación del ingrediente del **wire** con `quantity` anulable: el DTO reutiliza
  el tipo de **dominio**, y eso hace que `"quantity": null` (o ausente) rompa el decode y descarte la
  receta completa como `SCHEMA` (bug real de la prueba *"arroz con pollo"*).
- No hay tests con un ingrediente `quantity: null`/ausente/`unit` no nula: el punto ciego actual.
- El contrato §4 no explicita que un ingrediente pueda venir "al gusto" con `quantity` nula; el
  comportamiento real del modelo lo demuestra.

## Technical Approach

Estrategia: **DTO de ingrediente tolerante en la capa anti-corrupción `data.ai`**, sin tocar el
dominio. El wire del modelo se decodifica a un DTO que admite `quantity` nula y se **normaliza** al
mapear a `RecipeIngredient`.

### 1. DTO del wire del ingrediente — dentro de `AiRecipeDraft.kt`

```kotlin
@Serializable
internal data class AiIngredientDraft(
    val name: String,
    val quantity: Double? = null,      // null = "al gusto"; tambien cubre el campo ausente
    val unit: MeasureUnit? = null,
    val isPantry: Boolean = false,
    val ingredientId: String? = null,
)
```

- **Todos** los campos menos `name` tienen default tolerante; `quantity` es **anulable** (el arreglo
  clave).
- `fun AiIngredientDraft.toRecipeIngredient(): RecipeIngredient` con el **contrato de normalización
  exacto**:
  - `quantity == null` (null **o** ausente) → `RecipeIngredient(name = name, quantity = AL_GUSTO_QUANTITY,
    unit = null, isPantry = isPantry, ingredientId = ingredientId)`: se fuerza `unit = null` (aunque el
    wire traiga `"unit":"g"`) porque sin cantidad la unidad no significa nada; el par
    `(0.0, null)` es exactamente lo que la app pinta como **"Al gusto"** y **no escala**.
  - `quantity != null` → mapeo 1:1 (`quantity` numérica y `unit` del wire se conservan **intactas**).
- `AL_GUSTO_QUANTITY: Double = 0.0` se declara como constante con nombre (una sola fuente) junto al
  DTO. *(Nota de implementación: la spec decía `0.0D`, sufijo de Java que **no** existe en Kotlin —el
  compilador da `Unresolved reference 'D'`—; se usa `0.0` con el tipo `Double` explícito.)*

### 2. `AiRecipeDraft` pasa a usar el DTO del wire

- `val ingredients: List<AiIngredientDraft>` (antes `List<RecipeIngredient>`).
- `toRecipe(id)` mapea `ingredients = ingredients.map { it.toRecipeIngredient() }`; el resto de campos
  (que ya reutilizan `RecipeStep`/`Nutrition`) **no cambian**.
- KDoc actualizado: la nota "reutiliza `RecipeIngredient`" deja de ser cierta para ingredientes; se
  explica el motivo (el dominio tiene `quantity` no anulable y el wire legítimamente trae "al gusto").

### 3. `AiRecipeIds.canonical` sobre el ingrediente normalizado

- La cadena canónica del `id` sigue incluyendo el ingrediente en orden, pero lee el **ingrediente ya
  normalizado** (`it.toRecipeIngredient()`) en lugar de los campos crudos del wire. Consecuencia:
  `"quantity":null`, `quantity` ausente y `quantity: 0.0, unit: null` producen el **mismo `id`**
  (misma receta a ojos de la app) y **ningún id existente cambia** (para los ingredientes ya
  decodificables, `quantity` y `unit` valen lo mismo que antes → misma cadena canónica → mismo hash).
  **No** se sube `ALGORITHM_VERSION`.

### 4. `AiRecipePayload.decode` — sin cambios

El `runCatching { json.decodeFromJsonElement(AiRecipeDraft.serializer(), element) }` ya tolera el fallo
del ingrediente: con el DTO anulable, un `"quantity": null` **deja de fallar** y la receta entra
normalizada al Validador. No se retoca `decode` ni `AiDecodeResult` (el elemento que siga siendo
irrecuperable se cuenta como `SCHEMA` igual que hoy).

## Expected File Changes

**Modificar (main)**

- `app/src/main/java/com/sazon/app/data/ai/AiRecipeDraft.kt` — nuevo `AiIngredientDraft`,
  `toRecipeIngredient()` (contrato de normalización), `ingredients: List<AiIngredientDraft>`, mapeo en
  `toRecipe` y KDoc.
- `app/src/main/java/com/sazon/app/data/ai/AiRecipeIds.kt` — `canonical` canonicaliza el ingrediente
  **normalizado**; sin cambiar prefijo ni `ALGORITHM_VERSION`. (Cambio mínimo de compilación por el
  tipo nuevo.)

**Modificar (tests)**

- `app/src/test/java/com/sazon/app/data/ai/AiRecipePayloadTest.kt` — escenarios 1–7: ingrediente con
  `"quantity":null`+`unit`:null; `quantity` null con `unit` no nula; `quantity` **ausente**; `quantity`
  numérica intacta; lote mixto "al gusto" + rota (la buena sobrevive); normalización que **no** salta
  al Validador; `id`/`source`/`matchScore` correctos.
- `app/src/test/java/com/sazon/app/data/ai/AiRecipeIdsTest.kt` — adaptar el helper `draft(...)` a
  `List<AiIngredientDraft>`; añadir el caso de que `quantity:null` y `quantity` ausente comparten `id`.
- `app/src/test/java/com/sazon/app/data/ai/DevExpertRecipeGeneratorTest.kt` — solo si su helper de
  `AiRecipeDraft`/fixtures lo requiere para compilar; **sin** cambios de comportamiento esperados
  (regresión en verde).

**Modificar (docs)** — ver *Durable Documentation Impact*.

**No se toca**: `Recipe.kt`, `RecipeValidator.kt`, `RecipeJson.kt`, `AiRecipePayload.kt`,
`AiDecodeResult.kt`, `AiDiagnostics.kt`, `AiVerboseDiagnostics.kt`, `AiProviderPolicy.kt`,
`OpenAiChat.kt`, `AiHttpClientFactory.kt`, `DevExpertRecipeGenerator.kt`, `domain/**`, `ui/**`,
`data/generator/**`, las bases Room, `app/src/main/assets/**`, `app/build.gradle.kts`,
`gradle/libs.versions.toml`, `AndroidManifest.xml`, `init.sh`, `scripts/**`, `DESIGN.md`,
`docs/design/**`.

## Visual Design Impact

- UI involved: **no** (`area: integration`). No hay pantalla, estado, microcopy, token ni acabado
  nuevos. El comportamiento visible ya aceptado (Resultados, Detalle) pasa a recibir recetas que antes
  se descartaban; el render "Al gusto" del Detalle **ya existe** (`IngredientFormat.quantityLabel`)
  y esta feature solo hace que el ingrediente llegue con `unit = null`.
- Design source: not applicable.
- Screens or states affected: ninguno nuevo (Detalle sigue mostrando "Al gusto"); sin cambio de
  geometría, tokens ni microcopy.
- New design artifact required: **no** — `docs/design/concepts/ai-quantity-tolerance/` **no** es
  necesario y la feature **no** se bloquea por capturas. No se piden capturas de Stitch.

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — matizar el párrafo de `data.ai`: el ingrediente del wire
  se decodifica con un DTO tolerante (`AiIngredientDraft`), y una `quantity` nula/ausente se normaliza a
  "Al gusto" (`quantity = 0.0`, `unit = null`) antes de validar; añadir la nota en "Testing and
  Verification".
- `docs/domain/taxonomy.md` §4: **update (mínimo)** — aclarar que un ingrediente puede venir "al
  gusto" (`"quantity": null`, `"unit": null` o ausentes) y que la app lo normaliza; el modelo sigue sin
  enviar `id`/`source`/`matchScore`.
- `docs/domain-model.md`: **update (mínimo, opcional)** — una línea en `RecipeIngredient` de que
  `quantity` sigue siendo no anulable en dominio y que el "al gusto" del wire se normaliza a
  `0.0` + `unit = null` en el adaptador; coherente con el escalado ya documentado.
- `docs/risks-and-open-questions.md`: **not needed** — no nace pregunta abierta. (El contract drift
  "ingrediente al gusto" queda resuelto en `taxonomy.md` §4.)
- `AGENTS.md`: **not needed** — flujo, arranque y reglas no cambian. `DESIGN.md` y `docs/design/**`:
  **not needed**. `CONSTRAINTS.md`/`ARCHITECTURE.md`: no aplican (el repo los omite).
- `PROGRESS.md` y `feature_list.json`: **update al implementar/validar**, no en esta sesión.

## Implementation Plan

1. Añadir `AiIngredientDraft` (con `quantity` anulable y defaults tolerantes) y
   `toRecipeIngredient()` con el contrato de normalización (`AL_GUSTO_QUANTITY = 0.0`, `unit = null`).
2. Cambiar `AiRecipeDraft.ingredients` al DTO del wire y mapear en `toRecipe`; actualizar KDoc.
3. Ajustar `AiRecipeIds.canonical` para canonicalizar el ingrediente ya normalizado.
4. Añadir los escenarios 1–7 a `AiRecipePayloadTest` (fixtures literales con `quantity` null/ausente/
   no nula) y adaptar/añadir el caso de id estable en `AiRecipeIdsTest`.
5. `bash init.sh` en verde y tirada focal `com.sazon.app.data.ai.*`; actualizar
   `docs/technical-discovery.md` (y `taxonomy.md` §4 / `domain-model.md` si procede); el implementer
   registra estado y evidencia en `feature_list.json`/`PROGRESS.md`.

## Implementation Tasks

- [ ] `AiIngredientDraft` (`@Serializable internal data class`) con `name` obligatorio y
      `quantity: Double? = null`, `unit: MeasureUnit? = null`, `isPantry: Boolean = false`,
      `ingredientId: String? = null`.
- [ ] `AiIngredientDraft.toRecipeIngredient()`: `quantity == null` → `quantity = AL_GUSTO_QUANTITY (0.0)`
      y `unit = null` (descarta la `unit` del wire); `quantity != null` → mapeo 1:1 intacto.
- [ ] `AiRecipeDraft.ingredients: List<AiIngredientDraft>` + `toRecipe` mapeando
      `ingredients.map { it.toRecipeIngredient() }`; KDoc actualizado.
- [ ] `AiRecipeIds.canonical` canonicaliza el ingrediente normalizado; sin cambiar `PREFIX` ni
      `ALGORITHM_VERSION`.
- [ ] `AiRecipePayloadTest`: `quantity` null + `unit` null → aceptada/"Al gusto"; `quantity` null +
      `unit` no nula → `unit` descartada; `quantity` ausente → igual; `quantity` numérica intacta; lote
      mixto "al gusto" + rota (`received=2`, `{SCHEMA:1}`, la buena aceptada); receta con `quantity`
      nula pero otro campo inválido → sigue descartada por el Validador.
- [ ] `AiRecipeIdsTest`: adaptar el helper del draft al DTO del wire; caso `quantity:null` y `quantity`
      ausente → mismo `id`; el resto de casos de determinismo/unicidad siguen verdes.
- [ ] `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"` y `bash init.sh` en
      verde (0 errores de lint y sin warnings nuevos en ficheros de la feature).
- [ ] Inspección de alcance: `git diff --stat` solo con lo previsto; `Recipe.kt` y `RecipeValidator.kt`
      intactos (grep de `quantity` en dominio sin cambios).
- [ ] Docs durables (`technical-discovery.md`, `taxonomy.md` §4, `domain-model.md` si procede).

## Verification Plan

- **Automática (la que decide):** `bash init.sh` (exit 0) y, para tiradas focales,
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"`. **Nunca** `./gradlew`
  directo ni `--stop` (AGENTS.md).
- `AiRecipePayloadTest` — cuerpos **literales** del contrato §4 (nunca `RecipeJson.encode`):
  - Scenario 1: ingrediente `{"name":"Perejil fresco","quantity":null,"unit":null,...}` → la receta
    sale en `accepted` y ese ingrediente queda `quantity == 0.0`, `unit == null`; comprobar además
    `IngredientFormat.quantityLabel(0.0, null) == "Al gusto"`.
  - Scenario 2: `"quantity":null,"unit":"g"` → ingrediente `quantity == 0.0`, `unit == null`.
  - Scenario 3: ingrediente **sin** `quantity` ni `unit` → mismo resultado que el Scenario 1.
  - Scenario 4: `"quantity":200,"unit":"g"` → `quantity == 200.0`, `unit == GRAM` (intacto).
  - Scenario 5: lote "al gusto" + `{"title":"Rota"}` → `accepted.size == 1`, `received == 2`,
    `discardedByReason == {SCHEMA: 1}`.
  - Scenario 6: ingrediente "al gusto" en una receta con `mealTypeId` inexistente (o deriva > 5 min) →
    la receta **no** aparece en `accepted` y sí en `discardedByReason` con el motivo esperado.
  - Scenario 7: `source == RecipeSource.AI`, `id.startsWith("ai-")`, `matchScore == null`.
- `AiRecipeIdsTest` — determinismo y unicidad preservados; añadir que `quantity:null` y `quantity`
  ausente dan el **mismo `id`**.
- Lint: `bash init.sh` → 0 errores y sin warnings nuevos (los 13 preexistentes se mantienen).
- Inspección: `git diff --stat` limitado a lo previsto; `Recipe.kt` (`quantity: Double`),
  `RecipeValidator.kt`, `AiRecipePayload.kt`, `AiDecodeResult.kt`, `AiProviderPolicy.kt`,
  `DevExpertRecipeGenerator.kt`, `domain/**`, `ui/**`, build, Taxonomía, `seed-recipes.json` e
  `init.sh` sin cambios; sin dependencias nuevas.
- **Manual (la persona, emulador de Android Studio en Windows; no automatizable desde WSL):** con
  `devexpert.apiKey` válida, `bash scripts/gradle.sh assembleDebug` y ejecutar; buscar
  *"arroz con pollo"* → las recetas con *perejil fresco* "al gusto" ya **no** caen por `SCHEMA` (logcat
  `SUCCESS`/`received=6 accepted>3` en vez de `SCHEMA` en `discards`), llegan a Resultados y el Detalle
  pinta "Al gusto" en esos ingredientes; con la clave vaciada → modo semilla sin fallar.
- **Cobertura E2E:** no aplica (no hay suite E2E persistente; el `MockEngine` cubre el transporte y el
  decode en JVM; lo visual/manual lo revisa la persona). `init.sh` **no** cambia.

## Evidence To Capture

- `bash init.sh` (exit 0) y tirada focal `com.sazon.app.data.ai.*` con recuento de tests y 0 fallos
  (esperado: 76 preexistentes de `data.ai` + los nuevos, sin eliminar ninguno).
- Nombres y resultado de los tests nuevos: `quantity` nula + `unit` nula; `quantity` nula + `unit` no
  nula; `quantity` ausente; `quantity` numérica intacta; lote mixto (`received=2`, `{SCHEMA:1}`, la
  buena aceptada); descarte por el Validador pese a la cantidad nula; `id`/`source`/`matchScore`; id
  estable entre `quantity:null` y `quantity` ausente.
- Reproducción del bug (contexto): el mismo JSON literal con `"quantity":null` por el camino actual
  daría `accepted` vacío y `{SCHEMA:1}`; tras el cambio, `accepted == 1`.
- `git diff --stat` de lo tocado y confirmación de que `Recipe.kt`, `RecipeValidator.kt`,
  `AiRecipePayload.kt`, `domain/**`, `ui/**`, build, Taxonomía, `seed-recipes.json` e `init.sh` no
  cambian; sin dependencias nuevas.
- Resultado **manual** del emulador (clave válida → las recetas "al gusto" llegan a Resultados; Detalle
  con "Al gusto") y la línea de diagnóstico (`SazonAi`) correspondiente.
- Docs actualizadas (`technical-discovery.md`, `taxonomy.md` §4, `domain-model.md` si procede) y
  anotación de estado/evidencia en `feature_list.json` y `PROGRESS.md`.

## Decisions And Unknowns

### Decisiones

- **Normalizar en el adaptador (`data.ai`), no en el dominio.** `RecipeIngredient.quantity` sigue siendo
  `Double` no anulable; el "al gusto" del wire se traduce a `quantity = 0.0` + `unit = null` en el mapeo.
  Evita debilitar el esquema aceptado y evita tocar el Validador y la UI.
- **`0.0` + `unit = null` como representación de "Al gusto".** La app ya define esa semántica:
  `IngredientFormat.quantityLabel` devuelve `"Al gusto"` con `unit == null` (ignora la cantidad) y
  `ServingScaler` no escala con `unit == null`. Es la opción con **cero cambios de UI**.
- **La `unit` del wire se descarta cuando `quantity` es nula** (Scenario 2): sin cantidad, la unidad no
  es interpretable; forzar `unit = null` garantiza el render correcto y evita un `"0 g"` engañoso.
- **DTO del wire separado (`AiIngredientDraft`) en vez de hacer anulable el dominio o un serializer
  custom**: es explícito sobre qué admite el wire, mantiene el dominio intacto y es testeable sin
  plumbing de JSON.
- **Canonicalización del `id` sobre el ingrediente normalizado**: hace que las variantes de wire
  (`null`/ausente/`0.0`+`null`) compartan `id` (misma receta a ojos de la app) y **no** cambia ningún id
  ya existente; no se sube `ALGORITHM_VERSION`.
- **Sin cambios de UI ni de build**: el Detalle ya pinta "Al gusto"; no hacen falta dependencias ni
  capturas.

### Unknowns

- **Otras causas de `SCHEMA` en el ingrediente** (p. ej. una `unit` con un valor fuera de
  `MeasureUnit` no listado en el contrato) siguen descartando la receta; esta feature solo cubre
  `quantity` nula/ausente y `unit` presente con cantidad nula. Es el *Unknown* ya vigente de
  "robustez del modelo fuera del contrato §4" en `ai-recipe-decode.md`, no se mitiga aquí.
- **Deriva de tiempos (`INCOHERENT`/`TIME_DRIFT`)**: el curry que cae por 34 vs 40 min **no** se
  aborda (fuera de alcance explícito); si se pide, es feature aparte.
- **Ingrediente `name` ausente o vacío**: sigue siendo `SCHEMA` (el DTO lo mantiene obligatorio); no se
  relaja.
- **Preferencia de representación del modelo**: si el modelo deja de emitir `quantity` nula y usa
  siempre `0`/unidad, la normalización no molesta (es idempotente para valores numéricos).

## Validator Checklist

- [ ] Un ingrediente del wire con `quantity` **null** (con `unit` null o no nula) y **ausente** **no**
      descarta la receta: se normaliza a `quantity == 0.0`, `unit == null` y la receta se acepta.
- [ ] Una `quantity` **numérica se conserva intacta** (`quantity` y `unit` sin cambios).
- [ ] Un lote con una receta "al gusto" y otra rota sigue aceptando la buena (`received` correcto,
      `{SCHEMA:1}`).
- [ ] La normalización **no** salta al Validador: una receta con `quantity` nula y otro campo inválido
      se descarta con el motivo correcto; el proveedor sigue con restricciones neutras.
- [ ] `source == AI`, `id` (`ai-`) único/determinista y `matchScore == null` siguen completándose; los
      ids de las recetas ya decodificables **no** cambian.
- [ ] `Recipe.kt`/`RecipeIngredient` (`quantity: Double` no anulable) y `RecipeValidator.kt`
      **intactos**; `AiRecipePayload.kt`, `AiDecodeResult.kt`, `AiProviderPolicy.kt`,
      `DevExpertRecipeGenerator.kt`, `ui/**` sin cambios.
- [ ] Tests JVM con cuerpos **literales** del contrato §4 (no `RecipeJson.encode`); casos negativos que
      no pasan en vacío.
- [ ] `bash init.sh` en verde (test + lint, 0 errores y sin warnings nuevos) y regresión de los 76
      tests de `data.ai` (`AiProviderPolicyTest`, `AiRecipeIdsTest`, `AiRecipePayloadTest`,
      `AiVerboseDiagnosticsTest`, `DevExpertRecipeGeneratorTest`, `OpenAiChatTest`) y de
      `RecipeValidatorTest`.
- [ ] Alcance: solo `data.ai` (2 ficheros de main + sus tests) y las docs; `domain/**`, `ui/**`, build,
      Taxonomía, `seed-recipes.json`, `init.sh`, `DESIGN.md` y `docs/design/**` sin cambios; sin
      dependencias nuevas.
- [ ] `Visual Design Impact` sin capturas (no hay UI) y la verificación **manual** en emulador anotada
      como tal (las recetas "al gusto" llegan a Resultados; Detalle con "Al gusto"), no automatizable
      desde WSL.
- [ ] Docs actualizadas (`technical-discovery.md`, `taxonomy.md` §4 y, si procede, `domain-model.md`).
- [ ] `feature_list.json` y `PROGRESS.md` actualizados por implementer/validador/orquestador.
