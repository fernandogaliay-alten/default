# Feature Implementation Spec: Descriptor visual del plato en el contrato de la IA

## Source Feature

- `id`: `dish-visual-descriptor`
- `area`: `integration`
- `depends_on`: `["dish-image-generator"]` (satisfecha: `accepted`, sesión 090)
- `status`: `not_started` (en el momento de planificar)
- `source`: `feature_list.json` (entrada líneas 1049-1061) + `docs/ai-dish-images-proposal.md` §4.1
  (variante B) + `docs/adr/0005-ai-dish-images.md` (fase 5 pendiente)
- Verificación exigida (textual de la feature):
  1. `./gradlew test` cubre el campo opcional `dishKey`/`imagePrompt` **en el parseo de la respuesta**:
     presente, ausente (degradación) y formato inválido; **`RecipeValidator` no cambia**.
  2. La **clave canónica** (`domain.image.DishImageCacheKey`) usa `dishKey` cuando existe y mantiene el
     respaldo por título/id cuando falta; el **prompt de imagen** (`domain.image.DishImagePrompt`) usa
     `imagePrompt` cuando existe.
  3. El **contrato §4** de `docs/domain/taxonomy.md` documenta el campo opcional.

## Goal

Fijar, en el **contrato con la IA** y en la **política pura de imagen**, la variante **B** de la
propuesta: el mismo modelo que genera la receta puede emitir dos campos **opcionales** por receta,
`dishKey` (el plato canónico, ya pensado como clave de caché compartida) e `imagePrompt` (la
descripción visual del plato terminado). Hoy la frontera tipográfica del ADR 0005 (decisión 2 /
alternativa 2) hace que `"arroz con pollo"` y `"arroz con pollo fácil"` paguen **dos** imágenes;
`dishKey` es la pieza que permite que el mismo plato comparta clave, tal y como el propio ADR dejó
escrito como responsabilidad de esta fase.

`Recipe` y `RecipeValidator` **no cambian** (el descriptor es cosmético y **no** se valida): la app
sigue resolviendo la imagen por el título y la Taxonomía exactamente como hasta ahora **siempre que el
modelo no emita el descriptor** (degradación). Ganchos puros que la fase 3 y la fase 4 ya consumen
(`DishImageCacheKey`, `DishImagePrompt`, `DishImageRequest`) pasan a aceptar el descriptor como
**parámetro opcional con default**, sin cambiar la firma de nadie ni el puerto.

Sin llamada extra, sin red, sin cuota, sin UI y sin persistencia.

## Non-Goals

- **No** se entrega el descriptor al punto de composición de la petición de imagen en runtime
  (`MainActivity` → `DishImageArtworkLoader.requestFor`): hoy nadie pasa descriptor, así que la app
  sigue usando el respaldo por título/id. Es deliberado y **la verificación de esta feature es de
  contrato y de política pura en JVM** (ver "Decisions And Unknowns", Decisión 1).
- **No** se toca `Recipe.kt` ni `RecipeValidator.kt` (ni `domain/validation/**`). El descriptor **no**
  se valida, **no** se persiste y **no** es un campo de `Recipe`.
- **No** se modifica `AiRecipeIds.kt`: los `id` `ai-<sha256>` **no** cambian al añadir el descriptor
  (un cambio de `id` rompería Favoritos y `isSaved`; ver Decisión 5).
- **No** se toca el puerto `DishImageGenerator`, ni `data/ai/image/**`, ni `data/image/**`
  (`DishImageResolver`, caches, `DishImageSettings`), ni sus tests: el descriptor se consume **al
  componer** la petición, no en el transporte.
- **No** hay UI, ni microcopy, ni capturas: `area: integration`, sin comportamiento visual.
- **No** se implementan las fases 6 (`dish-style-reference`, `image-edit`) ni 7 (`dish-image-seed`), ni
  el autochequeo con visión de la propuesta §7.4.
- **No** hay dependencias nuevas ni cambios en `app/build.gradle.kts`, `gradle/libs.versions.toml`,
  `init.sh`, `scripts/**`, assets, Taxonomía (`docs/domain/taxonomy.json`) ni `seed-recipes.json`.
- **No** se cambia la plantilla del prompt (las cláusulas y su orden) ni la normalización de la clave:
  **`DishImageCacheKey.VERSION` no se sube** (ver Decisión 3 y Decisión 7).

## Job Story

Cuando la IA me propone un plato que ya conozco con otro nombre,
quiero que el propio modelo pueda indicar el plato canónico y una descripción visual en la **misma**
llamada que genera la receta,
para que la imagen del plato se comparta entre variantes y se describa mejor sin gastar una llamada ni
cuota extra — y si el modelo no lo indica, que todo siga funcionando como hasta ahora.

## Users And Permissions

- **Desarrollador / agente**: puede construir la clave, el prompt y la petición con o sin descriptor y
  probarlo en JVM; sin permisos, sin red y sin almacenamiento.
- **Usuario local**: **no interactúa** en esta rebanada (ninguna pantalla ni microcopy cambia). Su
  efecto visible llega con la entrega en runtime del descriptor (fuera de alcance aquí).

## Acceptance Scenarios

### Scenario 1: el contrato pide los campos opcionales

Given el prompt de sistema que compone `PromptBuilder.build(taxonomy, request)`,
When se inspecciona el texto,
Then incluye `dishKey` y `imagePrompt` en el esqueleto del contrato de salida y una instrucción que los
describe como **opcionales** y pide que `dishKey` sea **específico del plato terminado** (no de un
ingrediente ni de la categoría); el resto del contrato de §4 sigue presente y el prompt continúa siendo
determinista.

### Scenario 2: el descriptor presente se extrae y no altera la receta

Given una respuesta con una receta válida del contrato que además trae
`"dishKey": "arroz-con-pollo-criollo"` e `"imagePrompt": "arroz amarillo con pollo desmenuzado,
guisantes, zanahoria y aceitunas"`,
When se decodifica con `AiRecipePayload.decode(...)`,
Then la receta aparece en `accepted` exactamente como sin el descriptor, y el descriptor está en
`result.descriptors[receta.id]` con `dishKey` e `imagePrompt` no nulos.

### Scenario 3: sin descriptor, degradación y sin cambios

Given la misma respuesta **sin** `dishKey` ni `imagePrompt` (o con ambos `null`/vacíos/en blanco),
When se decodifica,
Then `result.descriptors` está **vacío**, `accepted`/`discardedByReason` son idénticos a los de una
respuesta sin esos campos y la clave y el prompt que se derivan de la receta son **exactamente** los de
hoy (título → respaldo por `id` → `plato`; prompt por título/categoría/cocina).

### Scenario 4: formato inválido → se ignora, nunca descarta la receta (caso negativo)

Given respuestas donde `dishKey` y/o `imagePrompt` llegan con un tipo JSON no textual (número, objeto,
array) o como cadena en blanco o con un `dishKey` que no deja ningún carácter `[a-z0-9]` (`"🍲"`,
`"..."`),
When se decodifica,
Then la receta sigue en `accepted` (**nunca** `SCHEMA` por un campo cosmético), el descriptor no
aparece en `result.descriptors` (o solo con el campo utilizable) y el resto de la respuesta se comporta
igual.

### Scenario 5: la clave canónica usa `dishKey` cuando existe

Given una `Recipe` con `title = "Arroz con pollo fácil"` y un descriptor con
`dishKey = "arroz-con-pollo"`,
When se calcula `DishImageCacheKey.of(recipe, descriptor)`,
Then la clave es `dish-image-v1-arroz-con-pollo` (el `dishKey` gana al título) y **coincide** con la de
otra `Recipe` de título distinto que traiga el mismo `dishKey` (la frontera tipográfica de la fase 1 se
resuelve cuando el modelo emite el descriptor).

### Scenario 6: la clave mantiene su respaldo cuando falta el descriptor

Given la misma `Recipe` con `descriptor = null`, con `DishVisualDescriptor.NONE` o con un descriptor
cuyo `dishKey` no es utilizable,
When se calcula la clave,
Then el resultado es **idéntico** al de la fase 1 (`dish-image-v1-<slug-título>`; sin título, respaldo
por `id`; sin ninguno, `dish-image-v1-plato`), con el mismo alfabeto `[a-z0-9-]` y la misma
independencia del `locale` del dispositivo.

### Scenario 7: el prompt de imagen usa `imagePrompt` cuando existe

Given la Taxonomía real, una `Recipe` y un descriptor con un `imagePrompt` utilizable,
When se construye `DishImagePrompt.build(taxonomy, recipe, descriptor)`,
Then el prompt contiene `imagePrompt` (con espacios colapsados) como descripción del plato, conserva el
prefijo (`Fotografía gastronómica de`), la cláusula de cocina de la receta y `STYLE_SUFFIX`
**exactamente una vez**, y no queda en blanco.

### Scenario 8: sin `imagePrompt`, el prompt es el de la fase 1 (sin regresión)

Given la misma receta con `descriptor = null`, con `DishVisualDescriptor.NONE` o con un descriptor que
solo trae `dishKey`,
When se construye el prompt,
Then la cadena es **idéntica** a `DishImagePrompt.build(taxonomy, recipe)` de hoy (degradación a
título → etiqueta de categoría real → `plato casero`).

### Scenario 9: la petición compone ambas piezas

Given la Taxonomía real, una `Recipe` y un descriptor,
When se construye `DishImageRequest.of(taxonomy, recipe, descriptor)`,
Then `cacheKey` coincide con `DishImageCacheKey.of(recipe, descriptor)`, `prompt` coincide con
`DishImagePrompt.build(taxonomy, recipe, descriptor)` y `recipeId` no cambia; con `descriptor`
omitido, el resultado es el de la fase 1 y el `data class` **no** gana ningún campo nuevo (el adaptador
sigue siendo transporte puro).

### Scenario 10: los `id` de las recetas no cambian

Given dos respuestas idénticas salvo que una trae el descriptor,
When se calculan sus `id` con `AiRecipeIds.idFor`,
Then el `id` es el **mismo** en ambos casos: el descriptor queda **fuera** de la cadena canónica del
`id` (los `id` ya decodificables no cambian).

## Repository Research

### Files Inspected

- `feature_list.json` (entrada `dish-visual-descriptor`, líneas 1049-1061) — metadata, verificación y
  no-goals.
- `docs/ai-dish-images-proposal.md` §2 (variante B), §4.1 (contrato del descriptor), §5 (clave =
  `dishKey`, no `recipe.id`), §6, §8 y §10 (fase 5) — el diseño de origen.
- `docs/adr/0005-ai-dish-images.md` — decisiones 2 y 4 (frontera tipográfica; clave versionada) y
  "Pendiente: fases 5-7"; el ADR ya atribuye a la fase 5 la igualdad conceptual.
- `app/src/main/java/com/sazon/app/domain/image/DishImageCacheKey.kt` — `normalize`, `of(dishName)` y
  `of(recipe)`: **reutilizables** tal cual para el `dishKey` (misma normalización tipográfica).
- `app/src/main/java/com/sazon/app/domain/image/DishImagePrompt.kt` — plantilla fija
  `<PREFIX> <plato>[, <cocina>], <STYLE_SUFFIX>` y degradación del `<plato>`.
- `app/src/main/java/com/sazon/app/domain/image/DishImageGenerator.kt` — `DishImageRequest` y su
  `of(taxonomy, recipe)`, único punto de composición; el puerto no se toca.
- `app/src/main/java/com/sazon/app/data/ai/AiRecipeDraft.kt` — **capa anti-corrupción del wire**
  (`AiIngredientDraft` normaliza "al gusto" para que una rareza del wire no descarte la receta): es el
  precedente exacto de cómo tolerar un campo opcional.
- `app/src/main/java/com/sazon/app/data/ai/AiRecipePayload.kt` — `decode` elemento a elemento
  (`runCatching` por elemento → `SCHEMA`), `draft.toRecipe(id = AiRecipeIds.idFor(...))` y después
  `RecipeValidator.validateRecipe`; aquí se extrae el descriptor.
- `app/src/main/java/com/sazon/app/data/ai/AiDecodeResult.kt` — tipo `internal` del resultado del
  decode; es el único sitio observable del parseo (va a `AiCallEvent` sin exponer contenido).
- `app/src/main/java/com/sazon/app/data/ai/AiRecipeIds.kt` — `canonical(draft)` enumera campos
  explícitos (añadir campos al draft **no** cambia el `id`); incluye `tips` a propósito.
- `app/src/main/java/com/sazon/app/domain/prompt/PromptBuilder.kt` — `OUTPUT_CONTRACT` y la enumeración
  de ids: el prompt de sistema es la copia operativa de §4.
- `app/src/test/java/com/sazon/app/domain/prompt/PromptBuilderTest.kt` — `CLAVES_DEL_CONTRATO` se
  comprueba con `contains`: añadir claves al contrato **no** rompe el test existente.
- `app/src/test/java/com/sazon/app/data/ai/AiRecipePayloadTest.kt` y `AiRecipeIdsTest.kt` — estilo de
  los tests del wire (payloads JSON en crudo) y de la estabilidad del `id`.
- `app/src/test/java/com/sazon/app/domain/image/{DishImageCacheKeyTest,DishImagePromptTest,DishImageRequestTest}.kt`
  — los 20 tests JVM que esta feature extiende (los tres ficheros existen y se pueden ampliar).
- `docs/domain/taxonomy.md` §4 (contrato con la IA) y §6 (estrategia visual) — dónde documentar el
  campo opcional.
- `docs/technical-discovery.md` (bullets de `domain.image` y tests JVM) y
  `docs/risks-and-open-questions.md` (fila de cuota/latencia) — los docs durables que se actualizan.
- `app/src/main/java/com/sazon/app/MainActivity.kt` (líneas 69-150) — el punto donde se compondría la
  petición; **no se toca** (ver Non-Goals y Decisión 1).

### Existing Patterns To Follow

- Logica pura en `domain.image` (JDK, sin Android, sin red, sin estado, sin reloj): mismo patrón que
  `DishImageCacheKey`/`DishImagePrompt`.
- Anti-corrupción del wire en `data.ai`: un campo opcional **nunca** puede tumbar la receta
  (precedente: `AiIngredientDraft` y "al gusto").
- Parámetros **opcionales con default** para ampliar sin romper llamantes (`DishImageRequest` ya se
  diseñó "aditivo" según el ADR, decisión 1).
- Tests JUnit 4 en JVM con la Taxonomía real por `taxonomy.asset.path`; nombres en español con `_`.
- Verificación canónica: `bash init.sh` (`./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`);
  **nunca** `./gradlew` directo.

### Current Gaps

- No existe ningún tipo que represente el descriptor del modelo (`dishKey` + `imagePrompt`), ni el
  wire lo parsea: hoy `AiRecipeDraft` **no** declara esos campos y `ignoreUnknownKeys` los tira.
- `PromptBuilder` **no** pide los campos: el modelo no los puede emitir aunque quiera.
- §4 de `docs/domain/taxonomy.md` no los documenta.
- Nadie compone todavía un `DishImageRequest` con descriptor (entrega en runtime: fuera de alcance).

## Technical Approach

### 1. Valor puro: `domain.image.DishVisualDescriptor` (nuevo)

```kotlin
data class DishVisualDescriptor(
    val dishKey: String? = null,      // plato canónico tal y como lo emite el modelo
    val imagePrompt: String? = null,  // descripción visual del plato terminado
) {
    val isEmpty: Boolean get() = dishKey == null && imagePrompt == null
    companion object {
        val NONE = DishVisualDescriptor()
        fun of(dishKey: String?, imagePrompt: String?): DishVisualDescriptor
    }
}
```

Reglas de `of` (puro, no lanza, entrada arbitraria):

- Colapsa espacios en blanco y recorta cada campo (reutilizar el criterio de
  `DishImagePrompt.singleLine`).
- `dishKey` se conserva **tal cual** (mayúsculas/diacríticos incluidos) porque quien canonicaliza es
  `DishImageCacheKey.normalize`; solo se descarta a `null` si tras normalizar **no queda ningún
  carácter** `[a-z0-9]` (`"🍲"`, `"..."`, `"  "`).
- `imagePrompt` se descarta a `null` si queda vacío tras el colapso.
- El resultado puede tener los dos campos `null` (`isEmpty`); el draft lo convierte en `null` para no
  poblar mapas vacíos.

### 2. Clave: `DishImageCacheKey.of(recipe, descriptor = null)`

- Si `descriptor?.dishKey` es utilizable → `of(dishKey)` (la **misma** función `normalize` de la fase
  1: `VERSION + "-" + slug`), de modo que `"Arroz con Pollo!!!"` y `"arroz-con-pollo"` dan la misma
  clave.
- En cualquier otro caso, **exactamente** el comportamiento actual (`título → respaldo por id →
  plato`). `of(dishName)` y las constantes no cambian; `VERSION` **no** se sube.

### 3. Prompt: `DishImagePrompt.build(taxonomy, recipe, descriptor = null)`

- La plantilla y las cláusulas **no** cambian:
  `<PROMPT_PREFIX> <plato>[, <cocina>], <STYLE_SUFFIX>`.
- Solo cambia la fuente del `<plato>`, con la precedencia:
  `descriptor.imagePrompt` → `title` → etiqueta de categoría real → `DEFAULT_DISH`.
- La cláusula de cocina y `STYLE_SUFFIX` se mantienen siempre (el estilo de la casa sigue en un único
  sitio y el resultado nunca queda en blanco).

### 4. Petición: `DishImageRequest.of(taxonomy, recipe, descriptor = null)`

- Único punto de composición; pasa el descriptor a la clave y al prompt; `recipeId` no cambia.
- `DishImageRequest` **no** gana campos: el descriptor no es información de transporte (el adaptador
  sigue usando `prompt` y `cacheKey` tal cual).

### 5. Wire tolerante: `AiRecipeDraft` + `AiDecodeResult` + `AiRecipePayload`

- `AiRecipeDraft` gana `val dishKey: JsonElement? = null` e `val imagePrompt: JsonElement? = null`
  (tolerante: **un tipo JSON equivocado no puede descartar la receta como `SCHEMA`**, mismo espíritu que
  `AiIngredientDraft`) y un `fun descriptor(): DishVisualDescriptor?` que convierte a texto **solo** los
  `JsonPrimitive` de tipo string y delega la limpieza en `DishVisualDescriptor.of`, devolviendo `null`
  si queda vacío.
- `AiDecodeResult` gana `val descriptors: Map<String, DishVisualDescriptor> = emptyMap()` (último
  parámetro, con default para no romper construcciones existentes): clave = `id` **generado** de la
  receta, valor = descriptor no vacío, **solo de recetas `accepted`**, en el orden de la respuesta.
- `AiRecipePayload.decode` recoge los descriptores en un mapa local y lo pasa en el resultado: dentro
  de la rama `Accepted`, `draft.descriptor()?.let { descriptors[result.recipe.id] = it }` (el `id` es el
  **generado** por `AiRecipeIds.idFor`, no el de origen). Un descarte no aporta descriptor. No cambia la
  validación ni el reparto `accepted`/`discardedByReason`.
- `AiRecipeIds.canonical` se deja **intacto**: el descriptor no entra en la cadena del `id`.

### 6. Contrato: `PromptBuilder` + §4

- `OUTPUT_CONTRACT` añade `"dishKey"` e `"imagePrompt"` al esqueleto y una línea que los describe como
  **opcionales** (plato terminado; `dishKey` breve y específico, no un ingrediente ni la categoría;
  `imagePrompt` una descripción visual breve) e indica que, si no se envían, la app usará el título y
  la taxonomía. El bloque estable sigue dependiendo solo de la Taxonomía y el prompt sigue siendo
  determinista.
- `docs/domain/taxonomy.md` §4 documenta los dos campos opcionales (formato recomendado, que la app los
  normaliza/no los valida, que **no** forman parte de `Recipe` y que su ausencia degrada).

### 7. Modo de fallo

Ninguna ruta nueva lanza por datos de entrada: descriptor ausente, vacío, de otro tipo o no
canonicalizable degradan al comportamiento de la fase 1 (clave por título/id, prompt por
título/categoría). La receta **nunca** se descarta por el descriptor.

## Expected File Changes

- `app/src/main/java/com/sazon/app/domain/image/DishVisualDescriptor.kt` — **crear**; valor puro + `of`.
- `app/src/main/java/com/sazon/app/domain/image/DishImageCacheKey.kt` — **modificar**; overload
  `of(recipe, descriptor = null)`.
- `app/src/main/java/com/sazon/app/domain/image/DishImagePrompt.kt` — **modificar**; parámetro opcional
  y precedencia del `<plato>`.
- `app/src/main/java/com/sazon/app/domain/image/DishImageGenerator.kt` — **modificar**; solo
  `DishImageRequest.of(taxonomy, recipe, descriptor = null)` (el puerto no cambia).
- `app/src/main/java/com/sazon/app/data/ai/AiRecipeDraft.kt` — **modificar**; campos opcionales
  tolerantes + `descriptor()`.
- `app/src/main/java/com/sazon/app/data/ai/AiDecodeResult.kt` — **modificar**; `descriptors`.
- `app/src/main/java/com/sazon/app/data/ai/AiRecipePayload.kt` — **modificar**; extracción por receta
  aceptada.
- `app/src/main/java/com/sazon/app/domain/prompt/PromptBuilder.kt` — **modificar**; contrato + línea de
  instrucción.
- `app/src/test/java/com/sazon/app/domain/image/DishVisualDescriptorTest.kt` — **crear**; escenarios
  3-8 (limpieza, `isEmpty`, degradación).
- `app/src/test/java/com/sazon/app/domain/image/DishImageCacheKeyTest.kt` — **modificar**; escenarios
  5-6.
- `app/src/test/java/com/sazon/app/domain/image/DishImagePromptTest.kt` — **modificar**; escenarios 7-8.
- `app/src/test/java/com/sazon/app/domain/image/DishImageRequestTest.kt` — **modificar**; escenario 9.
- `app/src/test/java/com/sazon/app/data/ai/DishVisualDescriptorWireTest.kt` — **crear**; escenarios 2-4
  y 10 vía `AiRecipePayload.decode` (acceso a tipos `internal` por *friend paths*, como
  `AiRecipePayloadTest`).
- `app/src/test/java/com/sazon/app/domain/prompt/PromptBuilderTest.kt` — **modificar**; escenario 1
  (añadir `dishKey`/`imagePrompt` a `CLAVES_DEL_CONTRATO` + aserción de la instrucción "opcional").
- `docs/domain/taxonomy.md` — **modificar**; §4 documenta el campo opcional.
- `docs/adr/0005-ai-dish-images.md` — **modificar**; estado "fases 1-5" + alcance de la fase 5.
- `docs/technical-discovery.md` — **modificar**; descriptor en `domain.image` y su parseo en `data.ai`.
- `docs/risks-and-open-questions.md` — **modificar**; riesgo de `dishKey` grueso y nota de entrega
  pendiente.
- `PROGRESS.md` y `feature_list.json` — **modificar** al implementar/validar; los toca el implementer,
  **no** el planner.

**No se toca**: `Recipe.kt`, `RecipeValidator.kt`, `domain/validation/**`, `AiRecipeIds.kt`,
`data/ai/image/**`, `data/image/**`, `ui/**`, `MainActivity.kt`, `SazonApp.kt`, `MainShell.kt`,
`app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `docs/domain/taxonomy.json`,
`seed-recipes.json`, assets, `DESIGN.md`, `docs/design/**` y las specs ya aceptadas.

## Visual Design Impact

- UI involved: **no** (`area: integration`; no hay pantalla, estado visual ni componente Compose, y
  `imagePrompt` es texto de prompt para un modelo de imagen, no dirección visual de pantalla).
- Design source: no aplica.
- Screens or states affected: ninguna.
- New design artifact required: no.
- **El gate de capturas no aplica**: **no** se crea `docs/design/concepts/dish-visual-descriptor/` ni
  ninguna otra carpeta bajo `docs/design/concepts/`. Las capturas obligatorias son solo de features
  `area: ui`; esta no lo es (mismo criterio que `dish-image-domain`, `dish-image-cache` y
  `dish-image-generator`).

## Durable Documentation Impact

- `docs/domain/taxonomy.md` §4: **update** — documenta `dishKey`/`imagePrompt` como campos **opcionales**
  del contrato (formato recomendado, no validados, no parte de `Recipe`, degradación si faltan).
  Requisito literal de la verificación de la feature.
- `docs/adr/0005-ai-dish-images.md`: **update** — pasa a "Aceptado parcialmente (fases 1-5)", añade el
  alcance de la fase 5 (contrato + política; descriptor aún **sin entrega en runtime**) y anota la
  consecuencia de versionado.
- `docs/technical-discovery.md`: **update** — el descriptor vive en `domain.image` (puro), `data.ai` lo
  parsea de forma tolerante y lo expone en el resultado del decode; hoy **sin** consumidor en runtime.
- `docs/risks-and-open-questions.md`: **update** — riesgo de `dishKey` demasiado grueso (colisión de
  imagen) y nota de que la entrega en runtime / persistencia del descriptor queda pendiente.
- `docs/domain-model.md`: **not needed** — no cambian entidades, relaciones ni estados; el descriptor
  no se persiste, no es campo de `Recipe` y no crea estado nuevo.
- `AGENTS.md`: **not needed** — no cambian flujo, arranque ni reglas de trabajo.
- `DESIGN.md`: **not needed** — sin UI ni microcopy nuevos.
- `docs/ai-dish-images-proposal.md`: **not needed** — sigue siendo una propuesta; §4.1 ya describe la
  variante B tal cual.

## Implementation Plan

1. Escribir `DishVisualDescriptor` con `of`/`isEmpty`/`NONE` y su test (limpieza y degradación).
2. Extender `DishImageCacheKey` (overload con descriptor) y ampliar su test con el escenario 5 (el
   `dishKey` gana y une variantes) y el 6 (respaldo intacto).
3. Extender `DishImagePrompt` y `DishImageRequest` con el parámetro opcional y ampliar sus tests
   (precedencia, sin regresión, composición).
4. Añadir los campos tolerantes a `AiRecipeDraft` + `descriptor()`; añadir `descriptors` a
   `AiDecodeResult` y rellenarlo en `AiRecipePayload.decode`.
5. Escribir `DishVisualDescriptorWireTest` (presente / ausente / formato inválido / no altera el
   `accepted` / el `id` no cambia).
6. Añadir los campos opcionales al `OUTPUT_CONTRACT` de `PromptBuilder` y ampliar `PromptBuilderTest`.
7. Actualizar §4 de `docs/domain/taxonomy.md`, el ADR 0005, `technical-discovery.md` y
   `risks-and-open-questions.md`.
8. Inspeccionar pureza y alcance (`git diff --name-only` = lista prevista; `Recipe.kt`,
   `RecipeValidator.kt` y `AiRecipeIds.kt` sin diff), ejecutar `bash init.sh` en verde, registrar
   evidencia y dejar la feature `passing` (la validación independiente es de otro rol).

## Implementation Tasks

- [ ] Crear `domain/image/DishVisualDescriptor.kt` (data class + `NONE` + `of` + `isEmpty`).
- [ ] Colapsar espacios en cada campo y descartar `dishKey` normalizado a vacío y `imagePrompt` vacío.
- [ ] Añadir `DishImageCacheKey.of(recipe, descriptor = null)` sin tocar `normalize`, `of(dishName)` ni
      `VERSION`.
- [ ] Añadir el parámetro opcional a `DishImagePrompt.build` con la precedencia
      `imagePrompt → título → categoría → DEFAULT_DISH`, conservando cocina y `STYLE_SUFFIX`.
- [ ] Añadir el parámetro opcional a `DishImageRequest.of` sin añadir campos a `DishImageRequest`.
- [ ] Añadir `dishKey`/`imagePrompt` (`JsonElement?`) a `AiRecipeDraft` + `descriptor()` sin tocar
      `toRecipe` ni `AiRecipeIds.canonical`.
- [ ] Añadir `descriptors` a `AiDecodeResult` (último parámetro con default) y rellenarlo en
      `AiRecipePayload.decode` solo con recetas `accepted` y descriptores no vacíos.
- [ ] Ampliar `DishVisualDescriptorTest`, `DishImageCacheKeyTest`, `DishImagePromptTest`,
      `DishImageRequestTest` y crear `DishVisualDescriptorWireTest`.
- [ ] Añadir los campos opcionales y su instrucción al `OUTPUT_CONTRACT` de `PromptBuilder` y ampliar
      `PromptBuilderTest` (contrato + "opcional").
- [ ] Actualizar §4 de `docs/domain/taxonomy.md` y los tres docs durables (ADR 0005,
      `technical-discovery.md`, `risks-and-open-questions.md`).
- [ ] Verificar por inspección que `Recipe.kt`, `RecipeValidator.kt` y `AiRecipeIds.kt` no cambian y que
      no se crea nada bajo `docs/design/concepts/`.
- [ ] Ejecutar `bash init.sh` en verde, registrar la evidencia y actualizar `feature_list.json` /
      `PROGRESS.md`.

## Verification Plan

- `bash init.sh` → **exit 0**. Ejecuta el gate no bloqueante estándar (`./gradlew test` +
  `./gradlew lint` vía `scripts/gradle.sh`), **no** arranca procesos largos, **no** usa red y **no**
  cambia `init.sh` (esta feature no lo modifica).
- Focal: `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.domain.image.*"
  --tests "com.sazon.app.data.ai.DishVisualDescriptorWireTest"
  --tests "com.sazon.app.domain.prompt.PromptBuilderTest" --rerun-tasks` → `BUILD SUCCESSFUL`.
- Parseo propio de los XML (`app/build/test-results/testDebugUnitTest/TEST-*.xml`): **0 fallos, 0
  errores, 0 saltados**. Baseline en HEAD de este worktree: **593 anotaciones `@Test`** (re-derivable
  con `git grep -c '@Test' -- 'app/src/test/**'`); el total debe subir con los tests nuevos y el delta
  debe coincidir con las tareas del plan.
- `bash scripts/gradle.sh lint` → **0 errores** y los **13 warnings preexistentes** (`GradleDependency`,
  `NewerVersionAvailable`, `AndroidGradlePluginVersion`, `OldTargetApi`), **ninguno** bajo `app/src`.
- Inspección de pureza y alcance:
  - `grep -rn "import android" app/src/main/java/com/sazon/app/domain/image` → 0; tampoco red
    (`io.ktor`, `java.net`) en `domain.image` ni en `DishVisualDescriptor`.
  - `git diff --name-only` = la lista de "Expected File Changes" (sin `Recipe.kt`, `RecipeValidator.kt`,
    `AiRecipeIds.kt`, `data/image/**`, `data/ai/image/**`, `ui/**`, `MainActivity.kt`, build, `init.sh`).
  - `docs/design/concepts/dish-visual-descriptor` **no** existe.
- Cobertura E2E: **no aplica**. El repo no tiene suite E2E persistente y esta feature no cambia ningún
  flujo observable (no hay UI, red, ni consumidor del descriptor en runtime): la verificación canónica
  es el test JVM/Robolectric, conforme a `AGENTS.md`.
- Nota operativa: usar `bash init.sh` o `bash scripts/gradle.sh <tarea>`; **nunca** `./gradlew` directo
  ni `./gradlew --stop` (ver `AGENTS.md`, Notas de entorno).

## Evidence To Capture

- Salida de `bash init.sh` con código 0 y de la tirada focal.
- Conteo parseado de los XML (suites/tests, 0 fallos/errores/saltados) con los tests nuevos nombrados
  (`DishVisualDescriptorTest`, `DishVisualDescriptorWireTest` y los ampliados).
- Informe de lint (0 errores, 13 warnings preexistentes, 0 bajo `app/src`).
- Vectores concretos: clave de dos títulos distintos con el mismo `dishKey` (`dish-image-v1-arroz-con-pollo`
  en ambos) frente a sus claves por título; clave y prompt con descriptor `null`/`NONE` idénticos a la
  fase 1; `accepted` de una respuesta con `"dishKey": 123` y con `"🍲"` (sin `SCHEMA`); `id` idéntico con
  y sin descriptor.
- Resultado de los greps/diff de inspección y confirmación de que **no** se creó nada en
  `docs/design/concepts/`.
- Anotación en `feature_list.json` (evidencia + `passing`) y `PROGRESS.md`, y confirmación de que el
  implementer **no** hizo commit, push ni PR (los hace el orquestador).

## Decisions And Unknowns

Decisiones cerradas por esta spec (el implementer no las reabre):

1. **Esta rebanada es de contrato + política pura; la entrega del descriptor al runtime es un
   non-goal.** `AiDecodeResult.descriptors` es el **transportador** documentado (clave = `id` de la
   receta), pero **nadie** lo consume todavía: `MainActivity` compone `DishImageRequest.of(taxonomy,
   recipe)` sin descriptor, así que la app sigue con el respaldo por título. Motivos: (a) la
   verificación de la feature es de parseo y de política, no de cableado; (b) entregarlo exige decidir
   un canal nuevo (un mapa `recipeId → descriptor` por sesión alimentado por una costura del generador,
   al estilo de `onRecipesGenerated`) y su propia cobertura, que merece su propia entrada en
   `feature_list.json`; (c) mantiene esta rebanada verificable **solo** en JVM y sin cuota. *Recomendación
   al orquestador*: dar de alta una feature de integración posterior (p. ej.
   `dish-visual-descriptor-wiring`) que conecte `AiDecodeResult.descriptors` con
   `DishImageArtworkLoader.requestFor`; el argumento `descriptor` de `DishImageRequest.of` ya existe para
   eso. *Alternativa descartada*: añadir el mapa a `Recipe` o cambiar el puerto `RecipeGenerator`
   (reitera el descarte del ADR 0004, alternativa 4, y rompe `FakeRecipeGenerator`/`GenerationService`).
2. **Parseo tolerante con `JsonElement?`.** Un campo cosmético **no** puede descartar la receta: con
   `val dishKey: String?` un `"dishKey": 123` haría fallar el decode del elemento y lo contaría como
   `SCHEMA`, justo lo que el contrato prohíbe. Es el mismo espíritu que `AiIngredientDraft` ("al gusto"
   no tumba la receta). Solo se aceptan `JsonPrimitive` de tipo string.
3. **`VERSION` no se sube.** La normalización y la **forma** de la plantilla (prefijo, cláusulas en el
   mismo orden, `STYLE_SUFFIX`) no cambian: solo cambia de dónde sale el `<plato>`/el slug cuando hay
   descriptor. Subir `VERSION` invalidaría además claves válidas de la fase 1 sin necesidad. La regla
   durable del ADR sigue vigente para futuros cambios de plantilla o de normalización.
4. **`imagePrompt` sustituye solo la cláusula del plato.** Prefijo, cláusula de cocina y `STYLE_SUFFIX`
   se conservan: el estilo de la casa vive en un único sitio (ADR 0005, decisión 3) y el resultado
   nunca queda en blanco. *Alternativa descartada*: que `imagePrompt` sea el prompt **entero** (perdería
   la coletilla de estilo y la homogeneidad visual).
5. **El descriptor queda fuera de `AiRecipeIds.canonical`.** Los `id` `ai-<sha256>` son la identidad de
   Favoritos/`isSaved`; un cambio de `id` al estrenar el descriptor rompería favoritos ya guardados y la
   memoización. Dos respuestas que solo difieren en el descriptor comparten `id` (misma receta a ojos de
   la app).
6. **Nada del descriptor se persiste ni se valida.** No entra en `Recipe` ni en `RecipeValidator`: es
   información cosmética del modelo, y la puerta dura (reglas 1-4) no debe cambiar.
7. **Consecuencia de caché conocida y aceptada**: si en el futuro se entrega el descriptor, un plato
   antes cacheado por título (`dish-image-v1-arroz-con-pollo-facil`) pasará a pedirse por `dishKey`
   (`dish-image-v1-arroz-con-pollo`) → **fallo de caché y una generación extra** la primera vez, y la
   entrada antigua queda como lastre hasta que la LRU la evicte. Es el precio de que el mismo plato
   comparta imagen y es el objetivo de la fase; queda anotado en `docs/risks-and-open-questions.md`.

Incógnitas y riesgos de implementación (no bloquean, quedan anotados):

- **`dishKey` demasiado grueso** (`"pollo"`, `"arroz"`) → colisiones de imagen entre platos distintos
  (riesgo ya identificado en la propuesta §8). Mitigación: el contrato pide especificidad del plato
  terminado y la clave es **exacta** (sin fusión difusa), pero si el modelo incumple, una imagen
  equivocada es peor que una duplicada; el riesgo se registra en `docs/risks-and-open-questions.md`.
- **El modelo puede no emitir el descriptor** (o emitirlo de forma inconsistente entre llamadas):
  degradación exacta a la fase 1, sin error. Con descriptor, el prompt/clave **no** son deterministas
  entre respuestas (el texto lo produce el modelo); la función sí es determinista **dada** una entrada.
- **`imagePrompt` de longitud arbitraria**: no se impone tope; el colapso de espacios evita basura, pero
  un texto muy largo encarecería el prompt de imagen. Se deja como observación, sin constante inventada.
- **Un `imagePrompt` distinto con el mismo `dishKey`** solo afecta a la **primera** generación (después
  manda la caché por clave). Es coherente con el diseño (clave = identidad, prompt = primera petición).
- **Entrega en runtime y persistencia del descriptor** quedan pendientes (Decisión 1); hasta entonces
  el valor de la fase 5 en la app es nulo, aunque el contrato y la política ya estén fijados y probados.

## Validator Checklist

- [ ] El alcance se respeta: **sin** entrega en runtime, UI, persistencia, red, adaptador, caché,
      assets, dependencias nuevas ni cambios en `Recipe.kt`, `RecipeValidator.kt`, `AiRecipeIds.kt`,
      `data/image/**`, `data/ai/image/**`, `ui/**`, `MainActivity.kt`, build o `init.sh`.
- [ ] Los 10 escenarios pasan, incluidos los negativos (sin descriptor, formato inválido → nunca
      `SCHEMA`, `dishKey` no canonicalizable) y el escenario 10 (el `id` no cambia).
- [ ] El parseo es **tolerante**: un `dishKey`/`imagePrompt` con tipo JSON no textual, en blanco o no
      canonicalizable **no** descarta la receta ni altera `accepted`/`discardedByReason`.
- [ ] `DishImageCacheKey.of(recipe, descriptor)` usa el `dishKey` normalizado cuando existe y, si no,
      reproduce **exactamente** el comportamiento de la fase 1; `VERSION` sigue siendo
      `dish-image-v1` y el alfabeto `[a-z0-9-]` se mantiene.
- [ ] `DishImagePrompt.build(taxonomy, recipe, descriptor)` usa `imagePrompt` como cláusula del plato
      conservando prefijo, cocina y `STYLE_SUFFIX` (exactamente una vez) y es idéntico al de la fase 1
      sin descriptor.
- [ ] `DishImageRequest` **no** gana campos y `DishImageRequest.of` sigue siendo el único punto de
      composición (con el nuevo parámetro opcional).
- [ ] `PromptBuilder` pide los campos como **opcionales** y específicos del plato terminado, y §4 de
      `docs/domain/taxonomy.md` los documenta (requisito literal de la verificación).
- [ ] `com.sazon.app.domain.image` no importa Android ni red; los tests corren en JVM con la Taxonomía
      real y **no** añaden dependencias.
- [ ] El ADR 0005 refleja el estado (fases 1-5) y el alcance real, y los otros tres docs durables están
      actualizados y coherentes.
- [ ] No se ha creado nada en `docs/design/concepts/` (el gate de UI no aplica).
- [ ] `feature_list.json` y `PROGRESS.md` reflejan el estado y la evidencia; el implementer no hizo
      commit, push ni PR.
