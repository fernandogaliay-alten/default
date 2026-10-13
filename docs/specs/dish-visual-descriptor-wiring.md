# Feature Implementation Spec: Cablear el descriptor visual del plato en runtime

## Source Feature

- `id`: `dish-visual-descriptor-wiring`
- `area`: `integration`
- `depends_on`: `["dish-visual-descriptor"]` (satisfecha: `accepted`, sesión 094)
- `status`: `not_started` (en el momento de planificar)
- `source`: `feature_list.json` (entrada líneas 1076-1090) + seguimiento recomendado por la spec
  `docs/specs/dish-visual-descriptor.md` (Decisión 1) y `docs/adr/0005-ai-dish-images.md` (fase 5,
  "entrega en runtime pendiente").
- Verificación exigida (textual de la feature):
  1. `./gradlew test` cubre que el descriptor que devuelve el decode (`AiDecodeResult.descriptors`) se
     propaga hasta `DishImageRequest.of` (`cacheKey` y `prompt`) y que, sin descriptor, la composición
     es **exactamente** la de la fase 1.
  2. `MainActivity` compone `DishImageRequest.of(taxonomy, recipe, descriptor)` con el descriptor de la
     respuesta; dos variantes del mismo `dishKey` comparten clave canónica (test JVM).
  3. Sin cambios en `Recipe`, `RecipeValidator` ni `AiRecipeIds` (los `id` `ai-<sha256>` no cambian),
     sin capturas y sin dependencias nuevas.

## Goal

Cerrar la **entrega en runtime** que la fase 5 (`dish-visual-descriptor`) dejó declarada como
non-goal: conectar el descriptor visual que el propio modelo emite **en la misma llamada** de la
receta (`AiDecodeResult.descriptors`, hoy sin consumidor) con el **único punto de composición** de la
imagen del plato (`DishImageRequest.of(taxonomy, recipe, descriptor)`), que ya lo acepta.

Hoy `MainActivity` compone `DishImageRequest.of(taxonomy, recipe)` sin descriptor, así que la clave
sale del título y `"arroz con pollo"` y `"arroz con pollo fácil"` **pagan dos imágenes**. Al cablear el
descriptor, el `dishKey` pasa a ser la **clave canónica compartida** y el `imagePrompt` el texto del
prompt, de forma que dos variantes del mismo plato reutilizan la misma imagen sin gastar cuota extra.

La entrega necesita un **canal** desde el decode (que vive dentro del adaptador real
`DevExpertRecipeGenerator`, en `data.ai`) hasta el `requestFor` de `MainActivity`, que solo recibe la
`Recipe`. Se usa una **costura aditiva del generador** (un callback, como `onEvent`/`onRecipesGenerated`)
más un **almacén en memoria por sesión** indexado por `id` de receta, consultado al componer la
petición. Sin cambio de puerto, sin campo nuevo en `Recipe`, sin persistencia, sin red, sin UI y sin
cuota.

## Non-Goals

- **No** se cambia el puerto `RecipeGenerator` ni se hace que devuelva el descriptor: se mantienen
  `suggestByPreferences`/`searchByText`/`generateFromIngredients` con `List<Recipe>` y
  `FakeRecipeGenerator`/`GenerationService`/`SuggestionBatchService`/`AiSearchService` intactos. El
  canal es la costura del adaptador.
- **No** se toca `Recipe.kt`, `RecipeValidator.kt` ni `AiRecipeIds.kt` (ni `domain/validation/**`): el
  descriptor sigue siendo cosmético, no se valida, no entra en el `id` y no es un campo de `Recipe`.
- **No** se persiste el descriptor: sin Room, sin ficheros, sin cambios en `SuggestionBatchDatabase`,
  `ProfileDatabase`, `FavoritesDatabase` ni `ActivitySummaryDatabase`. El almacén es **en memoria**.
- **No** se toca la política pura de imagen ya aceptada: `DishVisualDescriptor`, `DishImageCacheKey`,
  `DishImagePrompt`, `DishImageRequest`/`DishImageGenerator` y sus tests quedan **como están** (ya
  aceptan el descriptor); **no** se sube `DishImageCacheKey.VERSION`.
- **No** se toca el parseo del wire: `AiRecipeDraft`, `AiRecipePayload` y `AiDecodeResult` ya
  transportan el descriptor y no cambian.
- **No** se toca la resolución ni el pintado: `DishImageResolver`, las cachés, `GatedDishImageGenerator`,
  `DishImageSettings`, `ui.artwork.*` (`DishImageArtworkLoader`, `GeneratedRecipeArtworkSource`,
  `RecipeArtwork`) quedan intactos.
- **No** hay UI, microcopy, captura ni cambio visual: `area: integration` (cambia la clave/prompt, no
  el layout). **El gate de capturas no aplica** y **no** se crea
  `docs/design/concepts/dish-visual-descriptor-wiring/`.
- **No** hay dependencias nuevas ni cambios en `app/build.gradle.kts`, `gradle/libs.versions.toml`,
  `init.sh`, `scripts/**`, assets, `docs/domain/taxonomy.json` ni `seed-recipes.json`.
- **No** se implementan fases nuevas de `docs/ai-dish-images-proposal.md` (6 y 7 ya están `accepted`).

## Job Story

Cuando la IA me propone dos variantes del mismo plato en la misma sesión,
quiero que la imagen del plato se comparta entre ellas,
para no pagar dos veces la misma imagen y ver el mismo plato con la misma foto — y si el modelo no
emite el descriptor, que todo funcione exactamente como antes.

## Users And Permissions

- **Desarrollador / agente**: puede alimentar el almacén con un `Map` de descriptores y componer la
  petición con o sin descriptor, probándolo en JVM con la Taxonomía real; sin permisos, sin red, sin
  cuota y sin almacenamiento en disco.
- **Usuario local**: **no** interacciona en esta rebanada (ninguna pantalla ni microcopy cambia). El
  efecto es el **gasto** (no se paga dos veces el mismo plato) y la **coherencia** de la imagen.

## Acceptance Scenarios

### Scenario 1: el generador expone el descriptor del decode (costura del adaptador)

Given un `DevExpertRecipeGenerator` con un callback `onDescriptors` de grabación,
When una operación (`generateFromIngredients`/`searchByText`/`suggestByPreferences`) recibe una
respuesta con una receta aceptada que trae `dishKey`/`imagePrompt`,
Then `onDescriptors` se invoca **una vez** con un mapa de una entrada cuya clave es el `id`
**generado** de la receta aceptada y cuyo valor es el descriptor; con una respuesta sin descriptor el
mapa es **vacío**; en fallo, degradación o cancelación **no** se invoca. El parámetro tiene default y
los constructores/tests existentes no cambian.

### Scenario 2: el descriptor llega a `DishImageRequest.of` (propagación)

Given el `Map<String, DishVisualDescriptor>` que devuelve `AiRecipePayload.decode`,
When se registra en el almacén (`record`) y se compone
`DishImageRequest.of(taxonomy, recipe, store.descriptorFor(recipe.id))`,
Then `cacheKey` coincide con `DishImageCacheKey.of(recipe, descriptor)` (clave por `dishKey`) y
`prompt` coincide con `DishImagePrompt.build(taxonomy, recipe, descriptor)` (usa `imagePrompt`); y
`MainActivity` compone exactamente así en `requestFor`.

### Scenario 3: dos variantes del mismo `dishKey` comparten la clave canónica

Given un decode con dos recetas aceptadas de títulos distintos (`"Arroz con pollo"` y `"Arroz con
pollo fácil"`) y el **mismo** `dishKey = "arroz-con-pollo"`,
When ambas se registran y se componen vía el almacén,
Then las dos `cacheKey` son **idénticas** (`dish-image-v1-arroz-con-pollo`) y distintas de sus claves
por título; una sola clave compartida implica una sola entrada de caché/resolución para el mismo plato.

### Scenario 4: sin descriptor, composición exactamente la de la fase 1 (caso negativo)

Given el almacén vacío, o una receta sin registrar (semilla del catálogo, receta restaurada del Lote
o un Favorito), o un `recipe.id` en blanco/`null`,
When se compone `DishImageRequest.of(taxonomy, recipe, store.descriptorFor(recipe.id))`,
Then `descriptorFor` devuelve `null` y el resultado es **exactamente** `DishImageRequest.of(taxonomy,
recipe)` de la fase 1 (título → respaldo por `id` → `plato`; prompt por título/categoría/cocina), sin
regresión en los tests de la fase 1.

### Scenario 5: el almacén es tolerante, por sesión y efímero

Given un almacén recién creado y operaciones `record(...)`,
When se registran ids en blanco o descriptores vacíos (`DishVisualDescriptor.NONE`),
Then se ignoran (no crean entradas); con el mismo `id` registrado dos veces **gana el último**;
`descriptorFor(null)`/`descriptorFor("  ")`/id desconocido devuelven `null`; dos almacenes distintos
no comparten estado y nada se escribe en disco (efímero, se pierde al recrear la `Activity`).

### Scenario 6: alcance y pureza (inspección)

Given el diff de la feature,
Then `Recipe.kt`, `RecipeValidator.kt`, `AiRecipeIds.kt`, `AiRecipeDraft.kt`, `AiRecipePayload.kt`,
`AiDecodeResult.kt`, `domain/image/**`, `data/image/{DishImageResolver,FileDishImageCache,
MemoryDishImageCache,DishImageCachePolicy,AndroidDishImageCache,DishImageSettings,DishImageSettingsPolicy,
DishImageGenerationBudget,GatedDishImageGenerator}.kt`, `data/ai/image/**`, `ui/**`, `SazonApp.kt`,
`MainShell.kt`, build, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, assets, Taxonomía y
`seed-recipes.json` **no** cambian; `DishImageCacheKey.VERSION` sigue `dish-image-v1`; **no** existe
`docs/design/concepts/dish-visual-descriptor-wiring/`; no hay dependencias nuevas.

## Repository Research

### Files Inspected

- `feature_list.json` (entrada `dish-visual-descriptor-wiring`, líneas 1076-1090) — metadata,
  verificación y no-goals.
- `docs/specs/dish-visual-descriptor.md` (Decisión 1 y Non-Goals) — recomienda esta feature y
  describe el canal pendiente (`recipeId → descriptor` por sesión, "al estilo de `onRecipesGenerated`").
- `docs/adr/0005-ai-dish-images.md` (fase 5 y "Pendiente: entrega en runtime", líneas 146-168 y
  316-330) — el descriptor ya se fija en contrato/política; su entrega en runtime es lo que falta.
- `app/src/main/java/com/sazon/app/domain/image/DishImageGenerator.kt` —
  `DishImageRequest.of(taxonomy, recipe, descriptor = null)`: **único punto de composición**; ya acepta
  el descriptor. No se toca.
- `app/src/main/java/com/sazon/app/domain/image/DishVisualDescriptor.kt` — valor puro `of`/`isEmpty`/`NONE`.
- `app/src/main/java/com/sazon/app/data/ai/DevExpertRecipeGenerator.kt` — el adaptador real: decodifica
  con `AiRecipePayload.decode(...)` en la rama `AttemptOutcome.Success`, usa `result.accepted` y
  **descarta** `result.descriptors`. Constructor aditivo (patrón `onEvent`/`onRecipesGenerated`).
- `app/src/main/java/com/sazon/app/data/ai/AiDecodeResult.kt` — `descriptors: Map<String,
  DishVisualDescriptor>` (clave = `id` generado, solo recetas aceptadas). Es `internal`.
- `app/src/main/java/com/sazon/app/data/ai/AiRecipePayload.kt` (líneas 113-158) — rellena
  `descriptors` en el decode; **no** cambia.
- `app/src/main/java/com/sazon/app/MainActivity.kt` (líneas 70-163) — compone el generador, el
  `DishImageResolver` y `dishImageLoader` con `requestFor = { recipe -> DishImageRequest.of(taxonomy,
  recipe) }` (línea 160). Único punto de cableado.
- `app/src/main/java/com/sazon/app/data/image/DishImageResolver.kt` — resuelve por `cacheKey`
  (memoria → disco → semilla → generador → persistir): la clave compartida se traduce en una sola imagen.
- `app/src/main/java/com/sazon/app/data/image/DishImageSettings.kt` y `MemoryDishImageCache.kt` —
  precedentes de **estado de sesión en memoria** en `data.image` (modelo para el almacén nuevo).
- `app/src/main/java/com/sazon/app/domain/generation/GenerationService.kt` (líneas 34-40) — patrón de
  costura aditiva `onRecipesGenerated: suspend (List<Recipe>) -> Unit = {}`.
- `app/src/test/java/com/sazon/app/data/ai/DishVisualDescriptorWireTest.kt` — estilo de test del
  descriptor en el wire (`AiRecipePayload.decode`, JSON literal del contrato, friend paths).
- `app/src/test/java/com/sazon/app/data/ai/DevExpertRecipeGeneratorTest.kt` — `MockEngine` y reloj
  inyectado; base para el escenario de la costura.
- `docs/technical-discovery.md` (bullets de `domain.image` 532-556, `data.image` 593-608 y el parseo
  del descriptor 686-690; tests 738-757) — marca `domain.image` como "puro … y **sin estado**" (por eso
  el almacén va en `data.image`) y contiene las frases "nadie lo consume todavía" que hay que actualizar.
- `docs/domain/taxonomy.md` §4 (descriptor opcional, líneas 130-145) y §6 (clave por `dishKey`,
  líneas 182-185) — ya documentan el descriptor y su clave.
- `docs/risks-and-open-questions.md` (línea 145 y "ADRs" 169-179) — "entrega en runtime pendiente" y la
  consecuencia de caché al entregarla.

### Existing Patterns To Follow

- **Costura aditiva con default** en adaptadores/servicios: `onEvent` (data.ai),
  `onRecipesGenerated` (domain.generation) y los parámetros con default de `DishImageResolver`
  (`seed`). No rompe constructores ni tests existentes.
- **Estado de sesión en memoria en `data.image`** (`DishImageSettings`, `MemoryDishImageCache`): el
  almacén de descriptores vive ahí, no en `domain.image` (documentado como **sin estado**).
- **Clave canónica como identidad**: la deduplicación de imagen ya ocurre por `cacheKey` en
  `DishImageResolver` (Mutex por clave, memoria, disco); compartir la clave basta para no repetir gasto.
- **Test JVM sin emulador** con la Taxonomía real por `taxonomy.asset.path`; nombres en español con `_`.
- **Verificación canónica**: `bash init.sh` (`./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`); **nunca** `./gradlew` directo.

### Current Gaps

- `DevExpertRecipeGenerator` **descarta** `result.descriptors` (solo devuelve `result.accepted`): no
  hay forma de que el runtime vea el descriptor.
- `MainActivity` compone `DishImageRequest.of(taxonomy, recipe)` **sin** descriptor (línea 160), así que
  `dishKey`/`imagePrompt` no tienen efecto y dos variantes del mismo plato pagan dos imágenes.
- No existe ningún almacén `recipeId → DishVisualDescriptor` ni forma de consultarlo al componer.

## Technical Approach

### 1. Almacén de sesión: `data.image.DishVisualDescriptorStore` (nuevo)

Clase **pura** (solo JDK: `java.util.concurrent.ConcurrentHashMap`; sin Android, sin Compose, sin red,
sin reloj), en memoria, por sesión:

```kotlin
class DishVisualDescriptorStore {
    fun record(descriptors: Map<String, DishVisualDescriptor>)   // ignora ids en blanco y vacíos
    fun descriptorFor(recipeId: String?): DishVisualDescriptor?  // null si no hay/está en blanco
}
```

Reglas: `record` ignora claves en blanco y descriptores `isEmpty`/`NONE`, y con el mismo `id` **gana el
último**; `descriptorFor` devuelve `null` para `id` `null`/en blanco/desconocido; dos instancias no
comparten estado; **no** persiste nada. Se elige `data.image` (estado de sesión, como
`DishImageSettings`/`MemoryDishImageCache`) porque `domain.image` está documentado como **sin estado** y
debe seguir siéndolo.

### 2. Costura en el adaptador: `DevExpertRecipeGenerator.onDescriptors`

Parámetro **nuevo al final con default** (aditivo, como `onEvent`):

```kotlin
private val onDescriptors: (Map<String, DishVisualDescriptor>) -> Unit = {},
```

En la rama `AttemptOutcome.Success`, tras `report(...)` y antes del `return result.accepted`:

```kotlin
onDescriptors(result.descriptors)
```

Así se invoca **una vez por decode exitoso** (mapa posiblemente vacío) con la **misma** clave (`id`
generado) que usa el resto de la app, y **no** se invoca en fallo/degradación/cancelación. El tipo del
callback es público (`DishVisualDescriptor` es de dominio); `AiDecodeResult` (`internal`) **no** entra en
la firma.

### 3. Cableado en `MainActivity`

- Crear el almacén **antes** del generador: `val dishVisualDescriptors = DishVisualDescriptorStore()`.
- Pasar `onDescriptors = dishVisualDescriptors::record` al `DevExpertRecipeGenerator` (junto a
  `onEvent`, del que ya dispone). El generador es **compartido** por el Lote, el Generador y la Búsqueda,
  así que los descriptores de **cualquier** receta `ai` aceptada en la sesión quedan registrados.
- Componer la petición con el descriptor del almacén:

```kotlin
requestFor = { recipe -> DishImageRequest.of(taxonomy, recipe, dishVisualDescriptors.descriptorFor(recipe.id)) },
```

Sin más cambios: `DishImageArtworkLoader`, el seam de artwork y el resolutor se quedan igual.

### 4. Modo de fallo y degradación

Ninguna ruta nueva lanza: sin descriptor (semilla, restaurado, id desconocido) el comportamiento es el
de la fase 1. La **cancelación** se propaga (no se toca). Un `dishKey` grueso sigue siendo riesgo
aceptado (imagen compartida entre platos distintos); un `imagePrompt` distinto con la misma clave solo
afecta a la **primera** generación (después manda la caché por clave).

### 5. Consecuencias conocidas

- **Efímero**: al recrear la `Activity` (o abrir la app) el almacén arranca vacío; una receta del Lote
  restaurado o un Favorito **no** tiene descriptor hasta la siguiente llamada de IA → se compone por
  título (fase 1). Es coherente con "el descriptor no se persiste" (fase 5) y acota el diff.
- **Fallo de caché al cambiar de clave** (ya anotado en fase 5): un plato cacheado por título
  (`dish-image-v1-arroz-con-pollo-facil`) pasa a pedirse por `dishKey`
  (`dish-image-v1-arroz-con-pollo`) → una generación extra la primera vez, y la entrada antigua queda
  como lastre hasta que la LRU la evicte. Es el objetivo de la feature (mismo plato, misma imagen).

## Expected File Changes

- `app/src/main/java/com/sazon/app/data/image/DishVisualDescriptorStore.kt` — **crear**; almacén puro
  de sesión.
- `app/src/main/java/com/sazon/app/data/ai/DevExpertRecipeGenerator.kt` — **modificar**; nuevo parámetro
  `onDescriptors` e invocación en la rama de éxito.
- `app/src/main/java/com/sazon/app/MainActivity.kt` — **modificar**; crear el almacén, pasar
  `onDescriptors` y componer `DishImageRequest.of(taxonomy, recipe, descriptorFor(recipe.id))`.
- `app/src/test/java/com/sazon/app/data/image/DishVisualDescriptorStoreTest.kt` — **crear**; escenarios
  5 (tolerancia, último gana, sesión).
- `app/src/test/java/com/sazon/app/data/ai/DishVisualDescriptorWiringTest.kt` — **crear**; escenarios
  2-4 (decode → almacén → `DishImageRequest.of`: propagación, clave compartida, degradación a fase 1).
- `app/src/test/java/com/sazon/app/data/ai/DevExpertRecipeGeneratorTest.kt` — **modificar**; escenario 1
  (la costura recibe el mapa del decode; default no-op sin regresión).
- `docs/technical-discovery.md`, `docs/adr/0005-ai-dish-images.md`,
  `docs/risks-and-open-questions.md` — **modificar** (ver "Durable Documentation Impact").
- `PROGRESS.md` y `feature_list.json` — **modificar** al implementar/validar; los toca el implementer,
  **no** el planner.

**No se toca**: `Recipe.kt`, `RecipeValidator.kt`, `domain/validation/**`, `AiRecipeIds.kt`,
`AiRecipeDraft.kt`, `AiRecipePayload.kt`, `AiDecodeResult.kt`, `domain/image/**`,
`data/image/{DishImageResolver,FileDishImageCache,MemoryDishImageCache,DishImageCachePolicy,
AndroidDishImageCache,DishImageSettings,DishImageSettingsPolicy,DishImageGenerationBudget,
GatedDishImageGenerator}.kt`, `data/ai/image/**`, `data/generator/**`, `ui/**`, `SazonApp.kt`,
`MainShell.kt`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`,
`docs/domain/taxonomy.json`, `seed-recipes.json`, assets, `DESIGN.md`, `docs/design/**` y las specs ya
aceptadas.

## Visual Design Impact

- UI involved: **no** (`area: integration`; cambia la clave/prompt de la imagen generada, no el layout,
  ni un estado visual, ni microcopy).
- Design source: no aplica.
- Screens or states affected: ninguna.
- New design artifact required: no.
- **El gate de capturas no aplica**: **no** se crea `docs/design/concepts/dish-visual-descriptor-wiring/`
  ni ninguna otra carpeta bajo `docs/design/concepts/` (mismo criterio que `dish-image-domain`,
  `dish-image-generator`, `dish-image-cache`, `dish-visual-descriptor`, `dish-style-reference` y
  `dish-image-seed`).

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — el descriptor **ya tiene consumidor** en runtime: el
  adaptador expone `onDescriptors` (mapa por `id` generado), `data.image.DishVisualDescriptorStore` lo
  guarda por sesión y `MainActivity` compone `DishImageRequest.of(taxonomy, recipe,
  descriptorFor(recipe.id))`; sustituir las frases "nadie lo consume todavía" (líneas ~686-690) y anotar
  el almacén y los tests nuevos (sección de tests JVM ~738-757).
- `docs/adr/0005-ai-dish-images.md`: **update** — cerrar la "Pendiente: entrega en runtime" (líneas
  165-168 y 316-330): la fase 5 queda **entregada en runtime** por `dish-visual-descriptor-wiring`,
  manteniendo el estado "Aceptado parcialmente (fases 1-7)", que el descriptor sigue sin persistirse ni
  validarse y que `VERSION` sigue `dish-image-v1`.
- `docs/risks-and-open-questions.md`: **update** — la fila del `dishKey` (línea 145) deja de decir
  "entrega en runtime pendiente" (pasa a entregada), se explicita que la **consecuencia de caché** ya
  aplica y se anota la limitación nueva (el descriptor es **efímero por sesión**: una receta restaurada
  del Lote o un Favorito se resuelve por título hasta la siguiente llamada de IA). Actualizar la ficha
  del ADR 0005 en "ADRs" (líneas 169-179).
- `docs/domain/taxonomy.md` §4/§6: **not needed** — ya documentan los campos opcionales y que la clave
  usa el `dishKey` "cuando existe"; el cableado no cambia el contrato ni la Taxonomía.
- `docs/domain-model.md`: **not needed** — no cambian entidades, relaciones ni estados; el descriptor no
  se persiste ni es campo de `Recipe`.
- `AGENTS.md`, `DESIGN.md`: **not needed** — no cambian flujo, arranque, reglas de trabajo ni la
  dirección visual.
- `docs/ai-dish-images-proposal.md`: **not needed** — sigue siendo una propuesta; esta feature es un
  seguimiento de la fase 5, no una fase nueva.

## Implementation Plan

1. Crear `data/image/DishVisualDescriptorStore.kt` (puro, en memoria) y su test (escenario 5).
2. Añadir el parámetro `onDescriptors` (default `{}`) a `DevExpertRecipeGenerator` y llamarlo con
   `result.descriptors` en la rama de éxito; ampliar `DevExpertRecipeGeneratorTest` (escenario 1).
3. Escribir `DishVisualDescriptorWiringTest` (escenarios 2-4: decode → `record` →
   `DishImageRequest.of`; dos variantes comparten clave; degradación exacta a fase 1).
4. Cablear en `MainActivity`: crear el almacén, `onDescriptors = dishVisualDescriptors::record` y
   `requestFor = { recipe -> DishImageRequest.of(taxonomy, recipe,
   dishVisualDescriptors.descriptorFor(recipe.id)) }`.
5. Actualizar `docs/technical-discovery.md`, el ADR 0005 y `docs/risks-and-open-questions.md`.
6. Inspeccionar pureza y alcance (`git diff --name-only` = lista prevista; `Recipe.kt`,
   `RecipeValidator.kt` y `AiRecipeIds.kt` sin diff; nada en `docs/design/concepts/`), ejecutar
   `bash init.sh` en verde, registrar evidencia y dejar la feature `passing` (la validación
   independiente es de otro rol).

## Implementation Tasks

- [ ] Crear `data/image/DishVisualDescriptorStore.kt`: `record(Map)` y `descriptorFor(String?)` sobre
      `ConcurrentHashMap`, sin Android ni persistencia.
- [ ] Ignorar ids en blanco y descriptores vacíos; último gana; `descriptorFor` `null` para id
      nulo/en blanco/desconocido.
- [ ] Añadir `onDescriptors: (Map<String, DishVisualDescriptor>) -> Unit = {}` al final del constructor
      de `DevExpertRecipeGenerator` e invocarlo con `result.descriptors` tras `report(...)` en la rama
      `AttemptOutcome.Success` (nada en fallo/degradación/cancelación).
- [ ] Cablear en `MainActivity`: almacén antes del generador, `onDescriptors`, y el descriptor en
      `requestFor`.
- [ ] Crear `DishVisualDescriptorStoreTest` (escenario 5) y `DishVisualDescriptorWiringTest`
      (escenarios 2-4) y ampliar `DevExpertRecipeGeneratorTest` (escenario 1).
- [ ] Actualizar `docs/technical-discovery.md`, `docs/adr/0005-ai-dish-images.md` y
      `docs/risks-and-open-questions.md`.
- [ ] Verificar por inspección que `Recipe.kt`, `RecipeValidator.kt`, `AiRecipeIds.kt`,
      `domain/image/**`, `data/ai/image/**`, `data/image/**` (salvo el nuevo almacén), `ui/**`, build,
      `init.sh` y `scripts/**` no cambian y que **no** se crea nada bajo `docs/design/concepts/`.
- [ ] Ejecutar `bash init.sh` en verde, registrar la evidencia y actualizar `feature_list.json` /
      `PROGRESS.md`.

## Verification Plan

- `bash init.sh` → **exit 0**. Ejecuta el gate no bloqueante estándar (`./gradlew test` +
  `./gradlew lint` vía `scripts/gradle.sh`), **no** arranca procesos largos, **no** usa red y **no**
  cambia `init.sh` (esta feature no lo modifica).
- Focal: `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.image.*"
  --tests "com.sazon.app.data.ai.*" --rerun-tasks` → `BUILD SUCCESSFUL`.
- Parseo propio de los XML (`app/build/test-results/testDebugUnitTest/TEST-*.xml`): **0 fallos, 0
  errores, 0 saltados**. Baseline en HEAD de este worktree: **90 suites / 660 tests** (re-derivable con
  `git grep -c '@Test' -- 'app/src/test/**'` = **660** anotaciones); el delta debe coincidir con los
  tests nuevos (`DishVisualDescriptorStoreTest`, `DishVisualDescriptorWiringTest`, los añadidos a
  `DevExpertRecipeGeneratorTest`).
- `bash scripts/gradle.sh lint` → **0 errores** y los **13 warnings preexistentes** (`GradleDependency`,
  `NewerVersionAvailable`, `AndroidGradlePluginVersion`, `OldTargetApi`), **ninguno** bajo `app/src`.
- Inspección de pureza y alcance:
  - `grep -rn "import android" app/src/main/java/com/sazon/app/data/image/DishVisualDescriptorStore.kt`
    → 0; el almacén solo usa el JDK y `domain.image.DishVisualDescriptor`.
  - `git diff --name-only` = la lista de "Expected File Changes" (sin `Recipe.kt`, `RecipeValidator.kt`,
    `AiRecipeIds.kt`, `AiRecipeDraft.kt`, `AiRecipePayload.kt`, `AiDecodeResult.kt`, `domain/image/**`,
    `data/ai/image/**`, `data/image/**` salvo el almacén nuevo, `ui/**`, build, `init.sh`,
    `scripts/**`).
  - `DishImageCacheKey.VERSION` sigue `dish-image-v1` y `test -d
    docs/design/concepts/dish-visual-descriptor-wiring` → no existe.
- Cobertura E2E: **no aplica**. El repo no tiene suite E2E persistente y el efecto de la feature se
  verifica en JVM a nivel de clave/composición (una clave compartida ⇒ una sola resolución, ya cubierto
  por los tests de la fase 3); el acabado puramente visual no cambia. Conforme a `AGENTS.md`, la
  verificación canónica es el test JVM/Robolectric.
- Nota operativa: usar `bash init.sh` o `bash scripts/gradle.sh <tarea>`; **nunca** `./gradlew` directo
  ni `./gradlew --stop` (ver `AGENTS.md`, Notas de entorno).
- **Prueba manual opcional (persona, no es puerta)**: en el emulador, generar dos variantes del mismo
  plato y comprobar en logcat/`SazonAi`/red que la segunda no vuelve a llamar a `/images/generations`
  (misma clave). No es automatizable desde WSL.

## Evidence To Capture

- Salida de `bash init.sh` con código 0 y de la tirada focal.
- Conteo parseado de los XML (suites/tests, 0 fallos/errores/saltados) con los tests nuevos nombrados
  (`DishVisualDescriptorStoreTest`, `DishVisualDescriptorWiringTest` y los ampliados).
- Informe de lint (0 errores, 13 warnings preexistentes, 0 bajo `app/src`).
- Vectores concretos: dos títulos con el mismo `dishKey = "arroz-con-pollo"` →
  `dish-image-v1-arroz-con-pollo` en ambos (frente a sus claves por título distintas); `prompt` con el
  `imagePrompt` como cláusula del plato; `cacheKey`/`prompt` idénticos a la fase 1 sin descriptor (y con
  receta semilla/restaurada); almacén: id en blanco y `NONE` ignorados, último gana.
- Resultado de los greps/diff de inspección y confirmación de que **no** se creó nada en
  `docs/design/concepts/` y de que el implementer **no** hizo commit, push ni PR.
- Anotación en `feature_list.json` (evidencia + `passing`) y `PROGRESS.md`.

## Decisions And Unknowns

Decisiones cerradas por esta spec (el implementer no las reabre):

1. **El canal es una costura aditiva del adaptador + un almacén en memoria**, no el puerto ni un campo
   de `Recipe`. El decode vive dentro de `DevExpertRecipeGenerator`, así que es el único sitio que ve
   `AiDecodeResult.descriptors`; un callback con default (patrón `onEvent`/`onRecipesGenerated`) es el
   cambio mínimo que no rompe a nadie. *Alternativas descartadas*: cambiar `RecipeGenerator` para que
   devuelva el descriptor (rompe `FakeRecipeGenerator`/`GenerationService`/`SuggestionBatchService`/
   `AiSearchService` y es el descarte del ADR 0004, alternativa 4) y añadir el descriptor a `Recipe`
   (campo duplicado en el dominio + migración; descartado).
2. **El almacén vive en `data.image`** (estado de sesión, como `DishImageSettings`/`MemoryDishImageCache`)
   y es puro JDK, no Android. *Alternativa descartada*: `domain.image`, que `docs/technical-discovery.md`
   documenta explícitamente como **sin estado**.
3. **El descriptor es efímero y por sesión**: no se persiste (fase 5) ni se escribe con el Lote ni con
   los Favoritos; una receta restaurada se resuelve por título hasta la siguiente llamada de IA.
   *Alternativa descartada*: persistir los descriptores en `SuggestionBatchDatabase`/`FavoritesDatabase`
   (esquema, migraciones y alcance mucho mayor, con un beneficio marginal).
4. **`onDescriptors` se invoca una vez por decode exitoso** con `result.descriptors` (posiblemente
   vacío) y **nunca** en fallo/degradación/cancelación. El default es un no-op para no cambiar
   constructores ni tests.
5. **`DishImageCacheKey.VERSION` no se sube**: la normalización y la plantilla no cambian; el `dishKey`
   solo cambia **de dónde sale** el plato/slug al componer. Subir la versión invalidaría claves válidas
   sin necesidad (regla durable del ADR 0005).
6. **La clave del almacén es el `id` generado de la receta** (el mismo que usan Favoritos/`isSaved`).
   Como `AiRecipeIds` excluye el descriptor, dos recetas de contenido idéntico comparten `id`; con el
   mismo `id` registrado dos veces **gana el último** (información cosmética, sin efecto).
7. **Sin persistencia ni `ViewModel`**: el almacén vive en `MainActivity.onCreate`; recrear la
   `Activity` lo vacía (degradación a fase 1 hasta la siguiente llamada de IA). Evita dependencias y
   estado global; la alternativa (scope de aplicación) queda para una correctiva si molesta.

Incógnitas y riesgos de implementación (no bloquean, quedan anotados):

- **El modelo puede no emitir el descriptor** (o emitirlo inconsistentemente): degradación exacta a la
  fase 1, sin error. Con descriptor, la clave/prompt no son deterministas entre respuestas (los produce
  el modelo); la composición sí es determinista **dada** la entrada.
- **`dishKey` demasiado grueso** → colisiones de imagen entre platos distintos (riesgo ya registrado en
  `docs/risks-and-open-questions.md`; el contrato pide especificidad y la clave es exacta).
- **Fallo de caché al cambiar de clave** (título → `dishKey`): una generación extra la primera vez y una
  entrada antigua como lastre hasta la LRU (ya anotado en fase 5; ahora se materializa).
- **`imagePrompt` de longitud arbitraria**: no se impone tope (observación de la fase 5, sin constante
  inventada).
- **Carrera benigna**: una petición compuesta por título (antes de que el decode registre el descriptor)
  puede cachear una imagen con la clave antigua; tras registrarse el descriptor, la clave nueva puede
  generar otra. Acotado a la primera apertura y aceptable (mismo espíritu que la consecuencia de caché).

## Validator Checklist

- [ ] El alcance se respeta: **sin** UI, capturas, persistencia, red, cuota, dependencias nuevas ni
      cambios en `Recipe.kt`, `RecipeValidator.kt`, `AiRecipeIds.kt`, `AiRecipeDraft.kt`,
      `AiRecipePayload.kt`, `AiDecodeResult.kt`, `domain/image/**`, `data/ai/image/**`, `data/image/**`
      (salvo el almacén nuevo), `ui/**`, build o `init.sh`.
- [ ] Los escenarios 1-6 pasan, incluidos los negativos (sin descriptor ⇒ **exactamente** la fase 1;
      id en blanco/`NONE` ignorados; nada en fallo/cancelación).
- [ ] El descriptor del decode (`AiDecodeResult.descriptors`) se propaga hasta `DishImageRequest.of`
      (`cacheKey` = `DishImageCacheKey.of(recipe, descriptor)`, `prompt` con `imagePrompt`) y
      `MainActivity` compone con `descriptorFor(recipe.id)` (verificado por test JVM + inspección).
- [ ] Dos variantes del mismo `dishKey` comparten `cacheKey` (`dish-image-v1-<slug-dishKey>`) y el
      caso sin descriptor no introduce regresión (`DishImageCacheKey.VERSION` sigue `dish-image-v1`).
- [ ] `onDescriptors` es aditivo con default, se invoca una sola vez por decode exitoso con el mapa del
      decode y **no** se invoca en fallo/degradación/cancelación.
- [ ] El almacén es puro (sin Android/red), en memoria y por sesión; `record` tolera entradas vacías y
      el último gana; `descriptorFor` devuelve `null` para id nulo/en blanco/desconocido.
- [ ] `com.sazon.app.data.image.DishVisualDescriptorStore` no importa Android ni red y los tests corren
      en JVM con la Taxonomía real sin añadir dependencias.
- [ ] Los tres docs durables (`technical-discovery.md`, ADR 0005, `risks-and-open-questions.md`) están
      actualizados y coherentes (el descriptor ya tiene consumidor en runtime, con su limitación).
- [ ] No se ha creado nada en `docs/design/concepts/` (el gate de UI no aplica).
- [ ] `feature_list.json` y `PROGRESS.md` reflejan el estado y la evidencia; el implementer no hizo
      commit, push ni PR.
