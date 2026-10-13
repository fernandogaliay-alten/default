# Feature Implementation Spec: Puerto y prompt determinista de imagen de plato

## Source Feature

- `id`: `dish-image-domain`
- `area`: `domain`
- `depends_on`: `["recipe-domain-model", "taxonomy-in-code"]` (satisfechas: ambas `accepted`)
- `status`: `not_started` (en el momento de planificar)
- `source`: `feature_list.json`

## Goal

Dejar fijado, **en el dominio y sin red**, el contrato con el que la app pedirá y cacheará la imagen
generada de un plato: un **puerto** (`DishImageGenerator`) que una fase posterior implementará contra
el proveedor de IA, un **constructor de prompt puro** (`DishImagePrompt`) que traduce una `Recipe` a un
texto de imagen con estilo fijo, y una **clave canónica** (`DishImageCacheKey`) derivada del nombre del
plato, determinista y estable, que será la clave de caché (disco y memoria) para que dos veces el
mismo plato compartan imagen.

Es la **fase 1** de `docs/ai-dish-images-proposal.md`: todo el valor de esta entrega es que la política
(prompt, clave y firma del puerto) quede escrita y **verificada en JVM**, de modo que las fases 2
(adaptador real), 3 (caché) y 4 (UI progresiva) solo tengan que implementar transporte, persistencia y
pintado, sin volver a decidir nada. Nada de esto cambia hoy el comportamiento de la app: nadie consume
todavía el puerto.

## Non-Goals

- **No** se implementa el adaptador real (`data.ai.image.DevExpertDishImageGenerator`), ni Ktor, ni
  `/images/generations`, ni `/images/edits`, ni la decodificación de `b64_json`, ni la reducción a WebP
  (fase 2: `dish-image-generator`). Sin red y sin claves.
- **No** hay caché: ni memoria (LRU), ni ficheros en `cacheDir`, ni Room, ni evicción, ni tope de disco
  (fase 3: `dish-image-cache`).
- **No** hay UI ni cambios en `ui.artwork.RecipeArtwork`: ni carga progresiva, ni shimmer, ni fade, ni
  guardarraíles de sesión (fase 4: `dish-image-progressive-ui`, `area: ui`, con capturas obligatorias).
- **No** se toca la cadena local de `recipe-photos` (`RecipePhotoAssets`, `AssetRecipePhotoSource`,
  `RecipeArtwork`): sigue resolviendo todo hoy y **seguirá siendo el respaldo** en las fases posteriores.
- **No** se añade ningún campo al contrato de la IA ni al wire (`dishKey`/`imagePrompt` de la variante B
  y el `image-edit` de la variante C son las fases 5 y 6).
- **No** se modela el error de generación con una jerarquía de excepciones nueva (ver
  "Decisions And Unknowns").
- **No** se añaden dependencias, ni se toca `app/build.gradle.kts`, `gradle/libs.versions.toml`,
  `init.sh`, la Taxonomía, `seed-recipes.json` ni `Recipe`/`RecipeValidator`/`PromptBuilder`.
- **No** se empaquetan assets ni un set semilla de imágenes (fase 7).

## Job Story

Cuando conecte la generación real de imágenes, su caché y su pintado progresivo,
quiero disponer ya de la firma del puerto, del prompt y de la clave canónica en el dominio,
para no redecidir la política visual ni la identidad de caché en cada fase, y para poder probarlas en
JVM sin emulador, sin red y sin gastar cuota.

## Users And Permissions

- **Desarrollador / agente**: puede construir el prompt, la clave y la petición desde una `Recipe` y la
  Taxonomía, sin permisos, sin red y sin almacenamiento.
- **Usuario local**: **no interactúa** en esta fase; no hay pantalla, ni cambio visual, ni tráfico de
  red. Su efecto se verá en las fases 2–4.

## Acceptance Scenarios

### Scenario 1: el prompt es determinista, no vacío y con estilo fijo

Given una `Recipe` con `title = "Arroz con pollo criollo"`, `cuisineId = "spanish"` y
`categoryId = "rice"`, y la Taxonomía real,
When se construye el prompt dos veces, incluso con dos instancias de `Taxonomy` parseadas por
separado,
Then las dos cadenas son **idénticas**, no están en blanco, incluyen el título, la etiqueta de la
cocina de la Taxonomía (`Española`) y la coletilla de estilo completa, y nunca contienen espacios
dobles ni comas duplicadas.

### Scenario 2: sin título, el plato se describe por su categoría

Given una `Recipe` con `title = ""` y `categoryId = "dessert"` (categoría real),
When se construye el prompt,
Then el prompt describe el plato con la etiqueta de la categoría (`Postres`) en lugar de quedar
vacío.

### Scenario 3: receta incompleta → prompt por defecto, sin lanzar (caso negativo obligatorio)

Given una `Recipe` con `title = ""`, `categoryId = ""`, `cuisineId = ""`, ingredientes y pasos
vacíos,
When se construye el prompt,
Then **no** lanza, el resultado **no** está en blanco y contiene el plato por defecto
(`DishImagePrompt.DEFAULT_DISH`) y la coletilla de estilo. Mismo resultado con un `categoryId`
desconocido (`"no-existe"`) y con una `Taxonomy` vacía (todas las colecciones vacías).

### Scenario 4: la cocina desconocida no ensucia el prompt

Given una `Recipe` con cocina `""`, `"no-existe"` o una Taxonomía sin cocinas,
When se construye el prompt,
Then no aparece ninguna cláusula de cocina (ni un marcador de "desconocida") y el prompt sigue siendo
válido, determinista y no vacío.

### Scenario 5: la clave canónica normaliza el nombre del plato

Given los nombres `"Arroz con Pollo Criollo"`, `"  arroz  con   pollo criollo "`,
`"ARROZ CON POLLO CRIOLLO"` y `"Arroz con pollo criollo!"`,
When se calcula la clave canónica,
Then los cuatro producen **la misma** clave, con la forma `dish-image-v1-<slug>` donde `<slug>` solo
contiene `[a-z0-9-]`, sin diacríticos (p. ej. `"Salmón a la plancha"` → `dish-image-v1-salmon-a-la-plancha`)
y sin guiones sobrantes.

### Scenario 6: la clave es estable y discriminante

Given dos `Recipe` con el mismo título (`"Arroz con pollo"`) en llamadas distintas y una `Recipe`
con otro título (`"Tortilla de patatas"`),
When se calculan sus claves,
Then las dos primeros son **iguales entre sí** e iguales a `DishImageCacheKey.of("Arroz con pollo")`,
la tercera es distinta, y el `id`/`source` de la receta **no** influyen en la clave cuando hay
título.

### Scenario 7: sin nombre de plato, la clave no colisiona

Given una `Recipe` sin título cuyo `id` es `"seed-tortilla-patatas"` y otra `Recipe` sin título y sin
`id`,
When se calculan sus claves,
Then la primera es distinta de la segunda (respaldo por `id`, p. ej.
`dish-image-v1-plato-seed-tortilla-patatas`) y la segunda cae a la clave por defecto
(`dish-image-v1-plato`); **ninguna** clave queda vacía y las dos recetas sin nombre **no** comparten
clave.

### Scenario 8: normalización tipográfica, no semántica (frontera deliberada)

Given `"Arroz con pollo"` y `"Arroz con pollo fácil"` (y `"Receta de arroz con pollo"`),
When se calculan sus claves,
Then producen claves **distintas**: la normalización solo cubre mayúsculas, diacríticos, puntuación y
espacios, y **no** fusiona variantes semánticas (ver "Decisions And Unknowns", desviación consciente
de la lectura amplia de `docs/ai-dish-images-proposal.md` §5).

### Scenario 9: la petición compone las tres piezas

Given la Taxonomía real y una `Recipe`,
When se construye `DishImageRequest.of(taxonomy, recipe)`,
Then `prompt` coincide con `DishImagePrompt.build(taxonomy, recipe)`, `cacheKey` coincide con
`DishImageCacheKey.of(recipe)`, `recipeId` es el `id` de la receta (o `null` si está vacío), y dos
construcciones sucesivas son iguales (`data class` con igualdad estructural); el dominio **no** llama
a ninguna implementación del puerto.

## Repository Research

### Files Inspected

- `feature_list.json` (entrada `dish-image-domain`, líneas 886-899) — metadata, verificación exigida y
  no-goals.
- `docs/ai-dish-images-proposal.md` — §2 (piezas A/C/B), §4 (puerto/prompt/clave y descriptor), §4.2
  (plantilla y coletilla de estilo), §5 (clave = `dishKey`, no `recipe.id`), §8 (riesgos), §9
  (impacto documental), §10 (fases) y §12 (checklist de arranque).
- `app/src/main/java/com/sazon/app/domain/recipe/Recipe.kt` — campos disponibles: `title`,
  `categoryId`, `cuisineId`, `id`, `source`.
- `app/src/main/java/com/sazon/app/domain/taxonomy/Taxonomy.kt` — `cuisine(id)`, `recipeCategory(id)`,
  `ingredient(id)`: únicos accesos necesarios; etiquetas en español (`Española`, `Mediterránea`,
  `Arroz`, `Postres`).
- `app/src/main/java/com/sazon/app/domain/generator/RecipeGenerator.kt` — patrón de **puerto de
  dominio** (`interface` + `suspend`, sin Android ni Ktor) y su KDoc: el modelo de errores no se
  inventa antes de que exista el adaptador.
- `app/src/main/java/com/sazon/app/domain/prompt/PromptBuilder.kt` — patrón de **objeto puro** que
  traduce Taxonomía + datos a texto determinista, sin estado, sin coroutines y sin lanzar por datos de
  entrada.
- `app/src/main/java/com/sazon/app/domain/validation/IngredientText.kt` y
  `app/src/main/java/com/sazon/app/ui/results/MatchScore.kt` — normalización de texto ya usada en el
  repo: `Normalizer.normalize(..., NFD)` + `Regex("\\p{Mn}+")` + minúsculas.
- `app/src/main/java/com/sazon/app/data/ai/AiRecipeIds.kt` — patrón de canonicalización con
  `MessageDigest`, separadores de control y **versión de algoritmo** explícita.
- `app/src/main/java/com/sazon/app/data/RecipePhotoAssets.kt` — patrón de lógica pura de imagen con
  contrato `null` ("no hay imagen") y sin lanzar; **no** se modifica.
- `app/src/main/java/com/sazon/app/domain/search/SearchMemo.kt` — clave de memo por consulta
  (`trim().lowercase()`); concepto **distinto** de la clave de imagen.
- `app/src/test/java/com/sazon/app/domain/prompt/PromptBuilderTest.kt` y
  `app/src/test/java/com/sazon/app/domain/taxonomy/TaxonomyIntegrityTest.kt` — patrón de test JUnit 4
  en JVM que lee la **Taxonomía real** por `System.getProperty("taxonomy.asset.path")`.
- `app/build.gradle.kts` (líneas 124-130) — todas las tareas `Test` reciben `taxonomy.asset.path` y
  `seed.recipes.asset.path`; **no hace falta tocar el build**.
- `docs/domain/taxonomy.md` §6 — estrategia visual vigente (la imagen se resuelve por categoría); la
  resolución **no** cambia en esta fase.
- `docs/adr/0004-recipe-photos-as-assets.md` — afirma "sin red en tiempo de ejecución" para las fotos
  empaquetadas; la afirmación sigue siendo cierta hoy y se matizará en la fase 2 (ver ADR 0005).
- `docs/technical-discovery.md` §Integrations / §Testing and Verification — dónde se documentan los
  puertos de dominio y las políticas puras.
- `docs/risks-and-open-questions.md` — tabla de riesgos (cuota de IA) y lista de ADRs.
- `docs/specs/recipe-domain-model.md` y `docs/specs/taxonomy-in-code.md` — estructura y nivel de
  detalle de una spec de dominio ya aceptada.

### Existing Patterns To Follow

- Paquete de dominio puro `com.sazon.app.domain.<área>`, **sin** `android.*`, testeable en JVM.
- Objeto sin estado para políticas puras (`PromptBuilder`, `RecipePhotoAssets`) con KDoc extenso en
  español e identificadores en inglés.
- Puertos de dominio como `interface` con operaciones `suspend`; el adaptador vive en `data`/`data.ai`.
- Versión explícita del algoritmo cuando una cadena canónica es persistible (`AiRecipeIds`).
- Tests JUnit 4 en `app/src/test/java/com/sazon/app/domain/...`, nombres en español con `_`, Taxonomía
  real por `taxonomy.asset.path`.
- Verificación canónica: `bash init.sh` (`./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`); **nunca** `./gradlew` directo.

### Current Gaps

- **No existe `com.sazon.app.domain.image`**: hoy la única lógica de imagen es `data.RecipePhotoAssets`
  (assets offline), que no sabe nada de generación ni de claves de caché.
- **No existe ninguna noción de "nombre canónico de plato"**: `Recipe.id` es la identidad de una
  receta, no la de un plato; hoy dos recetas del mismo plato no comparten nada.
- **No hay prompt de imagen**: `PromptBuilder` construye el prompt de **texto** de la IA; el prompt
  visual (§4.2 de la propuesta) no existe en código.
- **El puerto no tiene implementación** y la fase 1 no la añade: la verificación es de contrato
  (compilación, inspección y tests de la lógica pura), no de comportamiento en runtime.

## Technical Approach

### 1. Dónde vive

Nuevo paquete `app/src/main/java/com/sazon/app/domain/image/`, puro Kotlin/JDK (sin Android, sin
Compose, sin red, sin Ktor, sin coroutines más allá de la firma `suspend` del puerto), con tres
ficheros. Es el mismo patrón que `domain.prompt` y `domain.generator`.

### 2. Puerto: `DishImageGenerator.kt`

```kotlin
/** Petición de imagen de un plato, ya resuelta: el adaptador no necesita conocer la Taxonomía. */
data class DishImageRequest(
    val cacheKey: String,
    val prompt: String,
    val recipeId: String? = null,
) {
    companion object {
        fun of(taxonomy: Taxonomy, recipe: Recipe): DishImageRequest
    }
}

interface DishImageGenerator {
    /** Imagen ya codificada lista para cachear; `null` = no disponible (degradación silenciosa). */
    suspend fun generate(request: DishImageRequest): ByteArray?
}
```

Reglas del contrato:

- El puerto está **keyed por `DishImageRequest`** (propuesta §4): recibe el prompt ya compuesto y la
  clave canónica, de modo que el adaptador de la fase 2 es transporte puro y **no** conoce `Recipe`
  ni la Taxonomía.
- `of(taxonomy, recipe)` es el **único punto de composición**: `prompt = DishImagePrompt.build(...)`,
  `cacheKey = DishImageCacheKey.of(recipe)`, `recipeId = recipe.id.takeIf { it.isNotBlank() }`.
- `ByteArray?`: `null` y bytes ya codificados. El **formato** (WebP reducido) y su extensión los decide
  la fase 2 (adaptador) / fase 3 (caché, que ya prueba extensiones por *magic bytes* como
  `RecipePhotoAssets.EXTENSIONS`); el dominio no modela codecs.
- Fallo de generación = `null`, **no** excepción: la app conserva la cadena local actual y nunca
  muestra un error por imagen (propuesta §6.5). La cancelación de la corrutina **sí** se propaga
  (`CancellationException`), porque la fase 4 cancela al salir de pantalla o al hacer scroll.

### 3. Prompt puro: `DishImagePrompt.kt`

`object` sin estado (patrón `PromptBuilder`):

```kotlin
object DishImagePrompt {
    const val PROMPT_PREFIX = "Fotografía gastronómica de"
    const val DEFAULT_DISH = "plato casero"
    const val STYLE_SUFFIX = "cerámica artesanal, luz natural suave, estilo editorial gastronómico, sin texto ni manos"

    fun build(taxonomy: Taxonomy, recipe: Recipe): String
}
```

Plantilla resultante (una sola forma, siempre la misma):

```
<PROMPT_PREFIX> <plato>[, <cocina>], <STYLE_SUFFIX>
```

- **`<plato>`** = `recipe.title` con espacios colapsados y recortados; si queda vacío,
  `taxonomy.recipeCategory(recipe.categoryId)?.label`; si tampoco hay, `DEFAULT_DISH`.
- **`<cocina>`** = `taxonomy.cuisine(recipe.cuisineId)?.label` **solo** si existe y no está en blanco;
  en cualquier otro caso la cláusula se **omite** (nunca "desconocida").
- **Caso (y minúsculas) del título se conservan**: los diacríticos y las mayúsculas ayudan al modelo
  de imagen. La forma normalizada (sin acentos, minúsculas) es exclusiva de la **clave** de caché.
- Sin `Random`, sin reloj, sin `Locale` por defecto y sin estado: misma entrada → misma cadena; el
  resultado **nunca** está en blanco y **siempre** contiene `STYLE_SUFFIX` exactamente una vez.
- No lanza por datos de entrada (título/categoría/cocina vacíos, ids desconocidos, `Taxonomy` vacía).

### 4. Clave canónica: `DishImageCacheKey.kt`

`object` puro. Dos niveles: normalizador tipográfico y clave versionada.

```kotlin
object DishImageCacheKey {
    const val VERSION = "dish-image-v1"       // se sube cuando cambien normalización o prompt
    const val FALLBACK_KEY = "plato"

    fun normalize(dishName: String): String   // slug puro, sin versión
    fun of(dishName: String): String          // VERSION + "-" + slug (o FALLBACK_KEY)
    fun of(recipe: Recipe): String            // título → respaldo por id → FALLBACK_KEY
}
```

Reglas de `normalize` (deterministas, sin lanzar, entrada arbitraria):

1. `trim()` y colapso de espacios.
2. `Normalizer.normalize(raw, NFD)` y eliminación de `\p{Mn}+` (diacríticos) — patrón ya usado en
   `IngredientText`/`MatchScore`.
3. `lowercase(Locale.ROOT)` (**explícito**: el idioma del dispositivo no puede cambiar la clave).
4. Todo carácter fuera de `[a-z0-9]` → `-` (puntuación, apóstrofos, símbolos, emoji).
5. Colapso de guiones repetidos y recorte de guiones en los extremos; si no queda nada, `""`.

Reglas de `of`:

| Entrada | Salida |
|---|---|
| `title` con slug no vacío | `dish-image-v1-<slug-título>` |
| `title` vacío, `id` con slug no vacío | `dish-image-v1-plato-<slug-id>` |
| ni título ni `id` útiles | `dish-image-v1-plato` |

- La clave **no** incluye `categoryId`/`cuisineId`/`source`: el criterio de compartición es el
  **nombre del plato** (propuesta §5); añadir clasificación partiría el mismo plato en imágenes
  distintas cuando el modelo clasifique distinto.
- La clave **sí** excluye `recipe.id` cuando hay título (propuesta §5): dos recetas del mismo plato
  comparten imagen; el `id` solo entra como **respaldo sin colisión** cuando no hay nombre (una receta
  sin título no debe heredar la imagen de otra).
- Alfabeto `[a-z0-9-]` y versión como primer segmento: la clave es apta como **nombre de fichero**
  (fase 3) y como columna en Room, y subir `VERSION` es el único mecanismo de invalidación (regla
  durable, ver ADR 0005).
- Determinista y estable en el tiempo: sin reloj, sin `hashCode`/identidad, sin orden de colecciones.

### 5. Modo de fallo

Coherente con el resto del dominio: **no lanzar por datos de entrada**. Datos incompletos degradan a
prompt por defecto y a clave por defecto; nunca a excepción, nunca a cadena vacía. Como no hay red, no
hay tiempo de espera ni reintentos que modelar en esta fase.

### 6. Frontera con las fases posteriores (no implementar aquí)

- `prompt`/`cacheKey` son el punto de entrada estable del puerto: la fase 2 (adaptador) los consume
  tal cual y la variante B (fase 5) solo cambiará **quién los rellena**, no la firma.
- Fase 3 mapea `cacheKey` → fichero en disco; fase 4 inserta la imagen generada **delante** de la
  cadena de `recipe-photos` (que pasa a respaldo). Ninguna de las dos se toca aquí.

## Expected File Changes

- `app/src/main/java/com/sazon/app/domain/image/DishImageGenerator.kt` — crear; puerto, `DishImageRequest`
  y su `of(taxonomy, recipe)`.
- `app/src/main/java/com/sazon/app/domain/image/DishImagePrompt.kt` — crear; prompt puro y constantes.
- `app/src/main/java/com/sazon/app/domain/image/DishImageCacheKey.kt` — crear; normalizador y clave
  canónica versionada.
- `app/src/test/java/com/sazon/app/domain/image/DishImagePromptTest.kt` — crear; escenarios 1-4 + casos
  negativos (receta vacía, `Taxonomy` vacía) + determinismo con la Taxonomía real.
- `app/src/test/java/com/sazon/app/domain/image/DishImageCacheKeyTest.kt` — crear; escenarios 5-8
  (normalización, estabilidad, respaldos y frontera tipográfica).
- `app/src/test/java/com/sazon/app/domain/image/DishImageRequestTest.kt` — crear; escenario 9
  (composición, igualdad estructural y `recipeId` vacío → `null`).
- `docs/technical-discovery.md` — modificar; registrar el paquete `domain.image` (puerto + prompt +
  clave), su pureza y que la cadena local sigue intacta (adaptador, caché y UI pendientes).
- `docs/risks-and-open-questions.md` — modificar; añadir el riesgo de **cuota/latencia/coste por
  imagen** (se activa en la fase 2) y listar el ADR 0005.
- `docs/adr/0005-ai-dish-images.md` — crear; decisión de las imágenes de plato generadas (puerto,
  prompt y clave canónica; ajuste parcial de `0004`).
- `PROGRESS.md` y `feature_list.json` — modificar al implementar/validar (estado, evidencia); no los
  toca el planificador.

**No se toca**: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`,
`app/src/main/androidTest/**`, `domain/recipe/**`, `domain/taxonomy/**`, `domain/validation/**`,
`domain/prompt/**`, `domain/generator/**`, `data/**`, `ui/**`, `MainActivity`, la Taxonomía
(`docs/domain/taxonomy.json`/`.md`), `seed-recipes.json`, `assets/**`, `DESIGN.md`,
`docs/design/**` y las specs ya aceptadas.

## Visual Design Impact

- UI involved: **no** (`area: domain`; no hay pantalla, ni estado visual, ni componente Compose).
- Design source: no aplica.
- Screens or states affected: ninguna.
- New design artifact required: no.
- **El gate de capturas de UI no aplica**: es una feature de dominio sin comportamiento visual. **No**
  se crea `docs/design/concepts/dish-image-domain/` ni ninguna otra carpeta en `docs/design/concepts/`.
  Las capturas de referencia son obligatorias solo para la fase 4 (`dish-image-progressive-ui`).

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — nueva frontera durable: el dominio gana el puerto
  `DishImageGenerator` y las políticas puras de prompt y clave de imagen; dejar explícito que hoy no
  hay adaptador, ni caché, ni red, y que la cadena de `recipe-photos` sigue siendo la única resolución.
- `docs/domain-model.md`: **not needed** — no cambian entidades, relaciones ni estados; la imagen no es
  un campo de `Recipe` (alternativa 3 descartada en ADR 0004) y no se introduce ninguna clave
  persistida todavía.
- `docs/domain/taxonomy.md` §6: **not needed** en esta fase — la resolución de la imagen no cambia; se
  actualizará en la fase 4, cuando la cadena gane el primer nivel.
- `AGENTS.md`: **not needed** — no cambian flujo, arranque ni reglas de trabajo.
- `docs/adr/0005-ai-dish-images.md`: **create** — registra la decisión (puerto en dominio, prompt
  determinista, clave canónica versionada, cadena local como respaldo) y **ajusta parcialmente** ADR
  0004 (la afirmación "sin red en runtime" sigue siendo cierta para la cadena de assets, que pasa a
  respaldo cuando exista generación). Estado **"Aceptado parcialmente (fase 1)"**, con lo pendiente
  (adaptador, caché, UI, variantes B/C) enumerado como fases 2-7.
- `docs/risks-and-open-questions.md`: **update** — riesgo de cuota (≈2 % por imagen), latencia (≈4 s) y
  crecimiento de almacenamiento, con la mitigación ya prevista (clave canónica + caché persistente +
  tope por sesión) y la nota de que se materializa en la fase 2; añadir el ADR 0005 a la lista.
- `DESIGN.md`: **not needed** — sin UI ni microcopy nuevos; `STYLE_SUFFIX` es texto de prompt, no
  dirección visual de pantalla.

## Implementation Plan

1. Escribir el test de la clave (`DishImageCacheKeyTest`) con los vectores de los escenarios 5-8 y los
   casos límite del normalizador, y hacerlo pasar con `DishImageCacheKey`.
2. Escribir el test del prompt (`DishImagePromptTest`) con la Taxonomía real por `taxonomy.asset.path`,
   incluyendo los casos negativos (receta vacía, `Taxonomy` vacía, cocina desconocida), y hacerlo pasar
   con `DishImagePrompt`.
3. Escribir el puerto y `DishImageRequest` (con `of`) y su test de composición/igualdad.
4. Inspeccionar pureza (sin `android.*`, sin red, sin estado) y que ningún fichero fuera del alcance ha
   cambiado.
5. Crear el ADR 0005 y actualizar `docs/technical-discovery.md` y `docs/risks-and-open-questions.md`.
6. Ejecutar `bash init.sh` en verde, registrar evidencia y dejar la feature `passing` (la validación
   independiente es de otro rol).

## Implementation Tasks

- [ ] Crear `DishImageCacheKey` con `normalize`, `of(dishName)` y `of(recipe)`, `VERSION` y
      `FALLBACK_KEY`.
- [ ] Usar `java.text.Normalizer` (NFD + `\p{Mn}+`) y `lowercase(Locale.ROOT)` en la normalización,
      siguiendo `IngredientText`.
- [ ] Crear `DishImagePrompt` con `PROMPT_PREFIX`, `DEFAULT_DISH` y `STYLE_SUFFIX` y la plantilla fija.
- [ ] Implementar la degradación `título → etiqueta de categoría → DEFAULT_DISH` y la omisión de la
      cláusula de cocina desconocida/vacía.
- [ ] Crear `DishImageGenerator` (puerto `suspend`, `ByteArray?`) y `DishImageRequest` con
      `of(taxonomy, recipe)`.
- [ ] Escribir `DishImageCacheKeyTest` (escenarios 5-8 + normalizador vacío/puntuación/emoji/acentos).
- [ ] Escribir `DishImagePromptTest` (escenarios 1-4 + determinismo con dos `Taxonomy` + casos
      negativos).
- [ ] Escribir `DishImageRequestTest` (escenario 9; `id` vacío → `recipeId = null`).
- [ ] Verificar por inspección que `domain/image` no importa Android ni red y que no hay estado global.
- [ ] Crear `docs/adr/0005-ai-dish-images.md` y actualizar `docs/technical-discovery.md` y
      `docs/risks-and-open-questions.md`.
- [ ] Ejecutar `bash init.sh` (test + lint) en verde y registrar la evidencia.
- [ ] Actualizar `feature_list.json` (a `passing` con evidencia) y `PROGRESS.md`.

## Verification Plan

- `bash init.sh` → **exit 0**. Ejecuta el gate no bloqueante estándar (`./gradlew test` + `./gradlew
  lint` vía `scripts/gradle.sh`), **no** arranca procesos largos, **no** usa red y **no** cambia
  `init.sh` (esta feature no lo modifica).
- `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.domain.image.*"` → `BUILD
  SUCCESSFUL` (tirada focal antes del gate completo).
- Parseo propio de los XML (`app/build/test-results/testDebugUnitTest/TEST-*.xml`): **0 fallos, 0
  errores, 0 saltados**; la baseline actual es **64 suites / 456 tests** y el total debe subir con los
  tests nuevos de `domain.image` (prompt, clave y petición).
- `bash scripts/gradle.sh lint` → **0 errores** (los 13 warnings de `GradleDependency`,
  `NewerVersionAvailable`, `AndroidGradlePluginVersion` y `OldTargetApi` son preexistentes, todos en el
  build/version catalog, **ninguno** bajo `app/src`).
- Inspección de pureza y alcance:
  - `grep -rn "import android" app/src/main/java/com/sazon/app/domain/image` → 0 resultados; tampoco
    `io.ktor`, `kotlinx.coroutines.launch` ni `System.currentTimeMillis`/`Random`/`UUID`.
  - `git diff --name-only` solo con los 9 ficheros previstos (3 de código, 3 de test, 3 de docs) más
    `feature_list.json`/`PROGRESS.md`; `app/build.gradle.kts`, `gradle/libs.versions.toml`,
    `init.sh`, `scripts/**`, `data/**`, `ui/**`, `Recipe.kt`, `RecipeValidator.kt`, `PromptBuilder.kt`,
    la Taxonomía y `seed-recipes.json` intactos.
- Cobertura E2E: **no aplica**. No hay suite E2E persistente, la feature no cambia comportamiento
  observable (nadie consume el puerto; no hay UI, red ni persistencia) y la verificación canónica del
  repo para lógica pura es el test JVM (`AGENTS.md`: la UI se valida por lógica/estado en JVM y, para
  features visuales, contra las capturas obligatorias — aquí no hay UI).
- Nota operativa para quien implemente: usar `bash init.sh` o `bash scripts/gradle.sh <tarea>`; **nunca**
  `./gradlew` directo ni `./gradlew --stop` (ver `AGENTS.md`, Notas de entorno).

## Evidence To Capture

- Salida de `bash init.sh` con código 0 (y del focal `--tests "com.sazon.app.domain.image.*"`).
- Conteo parseado del XML del gate (suites/tests, 0 fallos/errores/saltados) con los tests nuevos
  nombrados (`DishImagePromptTest`, `DishImageCacheKeyTest`, `DishImageRequestTest`).
- Informe de lint (0 errores, 13 warnings preexistentes, 0 bajo `app/src`).
- Resultado de los greps de inspección (0 imports de Android/red; ficheros del alcance).
- Vectores concretos comprobados en los tests: clave de `"Arroz con Pollo Criollo"` y de
  `"Salmón a la plancha"`, prompt por defecto de la receta incompleta (contiene `DEFAULT_DISH` y
  `STYLE_SUFFIX`) y prompt con la etiqueta real de la Taxonomía.
- Anotación en `feature_list.json` (evidencia + `passing`) y en `PROGRESS.md`.
- Confirmación de que el implementer **no** hizo commit, push ni PR (los hace el orquestador).

## Decisions And Unknowns

Decisiones cerradas por esta spec (el implementer no las reabre):

1. **`null` como contrato de fallo, sin jerarquía de excepciones nueva.** `generate` devuelve
   `ByteArray?`; `null` = "no disponible → se conserva la cadena local". Motivos: mismo contrato que
   `RecipePhotoAssets.resolve` (que ya devuelve `null`), ninguna fase necesita distinguir "falló" de
   "no se intentó" para decidir el pintado, y `RecipeGenerator` fija el precedente de no inventar el
   modelo de errores antes de que exista adaptador. La cancelación **sí** se propaga. *Alternativa
   descartada*: tipo valor `DishImage(bytes, mimeType)` + `DishImageUnavailableException` (añade tipos
   que la fase 2 tendría que redecidir).
2. **Normalización tipográfica, no semántica.** `"Arroz con pollo"` y `"Arroz con pollo fácil"`
   producen claves distintas. Esto **estrecha** la lectura amplia de `docs/ai-dish-images-proposal.md`
   §5 (que esperaba que compartieran imagen) y sigue su §4.1 ("clave exacta, sin fusión difusa"):
   preferimos una imagen redundante (coste de cuota) a una imagen **equivocada** (colisión). La
   fusión semántica es responsabilidad de la fase 5 (`dish-visual-descriptor`: el `dishKey` lo emite
   el modelo, ya normalizado conceptualmente). Debe quedar registrado en el ADR 0005.
3. **Clave versionada (`dish-image-v1`) y con alfabeto de fichero.** Subir `VERSION` es el **único**
   mecanismo de invalidación de la caché cuando cambien la normalización o la plantilla del prompt; es
   la misma disciplina que `AiRecipeIds.ALGORITHM_VERSION`. Cualquier cambio de `STYLE_SUFFIX` o del
   orden de las cláusulas del prompt **obliga** a subir la versión (la caché de la fase 3 se indexa por
   clave, no por prompt).
4. **Respaldo por `id` cuando falta el título.** Evita que dos recetas sin nombre compartan imagen; el
   coste (no compartir imagen en un caso degenerado) se acepta.
5. **El prompt conserva mayúsculas y diacríticos; la clave no.** El prompt es texto para un modelo de
   imagen (los acentos y la forma natural ayudan); la clave es un identificador de máquina.
6. **ADR 0005 se crea en esta fase, con estado "Aceptado parcialmente (fase 1)".** La decisión de
   arquitectura (puerto en dominio + clave canónica + cadena local como respaldo) es la que habilita
   las fases 2-7 y deja de estar solo en una propuesta no aceptada; lo pendiente se enumera como
   pendiente. Si la persona prefiere posponer el ADR a la fase 2, esta spec es la única que cambia
   (basta con marcar el impacto documental como `not needed` aquí y crear el ADR allí).

Incógnitas y riesgos de implementación (no bloquean, quedan anotados):

- **Títulos no latinos o solo símbolos** (p. ej. un título en CJK o `"🍲"`) producen slug vacío y caen
  al respaldo por `id`. Aceptado y cubierto por test; si alguna vez importa, se resuelve en la fase 5
  con el `dishKey` del modelo.
- **El prompt depende de etiquetas de la Taxonomía** (cocina y categoría): editar una etiqueta cambia
  el prompt pero **no** la clave, así que una imagen ya cacheada podría no reflejar la etiqueta nueva.
  Impacto bajo (las etiquetas cambian muy rara vez) y mitigable subiendo `VERSION` si molesta.
- **El puerto queda sin implementación** (código no ejecutado) en esta fase: es deliberado y el
  precedente es `RecipeGenerator` antes de `real-ai-provider`; ningún test debe intentar sustituir al
  adaptador real.
- **La firma del puerto es la apuesta de mayor coste** de la fase: si la fase 2 necesitara más contexto
  (p. ej. tamaño objetivo, semilla de estilo o la foto de referencia de la variante C), habría que
  ampliar `DishImageRequest`. Por eso el request es un `data class` con defaults (ampliarlo es
  aditivo) y no una función con cinco parámetros.
- **Preguntas de la propuesta que siguen abiertas** (§11): persistencia de la caché (Room vs ficheros),
  tamaño objetivo y tope de disco, set semilla, interruptor de usuario, y si la variante C entra desde
  el principio. Ninguna se decide aquí.

## Validator Checklist

- [ ] La implementación se mantiene en el alcance: **sin** adaptador, red, Ktor, caché, persistencia, UI,
      assets, dependencias nuevas ni cambios en `Recipe`/`RecipeValidator`/`PromptBuilder`/build/`init.sh`.
- [ ] Los 9 escenarios pasan, incluidos los negativos (receta vacía, `Taxonomy` vacía, cocina
      desconocida) y el escenario 8 (frontera tipográfica, con la desviación razonada).
- [ ] `DishImagePrompt` es determinista (misma entrada → misma cadena, incluso con dos instancias de
      `Taxonomy`), nunca devuelve una cadena en blanco y siempre contiene `STYLE_SUFFIX` una sola vez.
- [ ] La degradación del prompt es exactamente `título → etiqueta de categoría real → DEFAULT_DISH`, y la
      cláusula de cocina se **omite** cuando el id no existe.
- [ ] `DishImageCacheKey.normalize` usa NFD + `\p{Mn}+` + `lowercase(Locale.ROOT)` y devuelve solo
      `[a-z0-9-]` (sin `-` en los extremos ni repetidos); las claves son estables entre llamadas y no
      dependen de `locale`/reloj/`Random`.
- [ ] La clave no incluye `categoryId`/`cuisineId`/`source` ni `recipe.id` cuando hay título; el respaldo
      por `id` y el respaldo final (`dish-image-v1-plato`) están cubiertos por test.
- [ ] `DishImageRequest.of(taxonomy, recipe)` es el único punto de composición y `recipeId` es `null`
      cuando el `id` está vacío.
- [ ] `com.sazon.app.domain.image` no importa `android.*`, ni Ktor, ni red; los tests corren en JVM con
      la Taxonomía real (`taxonomy.asset.path`) y **no** añaden dependencias.
- [ ] El ADR 0005 existe con su estado acotado y ajusta parcialmente el 0004 sin contradecirlo;
      `docs/technical-discovery.md` y `docs/risks-and-open-questions.md` están actualizados y coherentes.
- [ ] No se ha creado nada en `docs/design/concepts/` (el gate de UI no aplica a esta feature).
- [ ] `feature_list.json` y `PROGRESS.md` reflejan el estado y la evidencia; el implementer no hizo
      commit, push ni PR.
