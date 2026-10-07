# Feature Implementation Spec: Constructor de prompt desde el perfil

## Source Feature

- `id`: `prompt-builder`
- `area`: `domain`
- `depends_on`: `["taxonomy-in-code", "preference-signature"]` (ambas en `accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`.
- `source`: `feature_list.json`

## Goal

Componer, como **lógica pura de dominio**, el prompt que la app enviará a la IA a partir del Perfil
de preferencias del usuario, traduciendo los `*Id` seleccionados a las **etiquetas y `promptHint`** de
la Taxonomía y exigiendo una **salida estructurada** con `ids` de la Taxonomía para todo lo
clasificable (contrato de `docs/domain/taxonomy.md` §4).

Es la pieza que traduce "lo que el usuario eligió" al lenguaje del modelo. Sus dos garantías son de
seguridad: el **patrón de dieta** y las **restricciones duras** aparecen **siempre** en el prompt, y
los campos clasificables (`categoryId`, `mealTypeId`, `cuisineId`, `difficultyId`,
`declaredAllergens`) deben salir como `id` de la Taxonomía, nunca como texto libre. Pide **6 recetas**
por generación (decisión ya resuelta en `docs/risks-and-open-questions.md`).

Vive en `com.sazon.app.domain.prompt`, sin Android, sin red, sin persistencia y sin UI, para poder
testearse en JVM con `./gradlew test`.

## Non-Goals

- **No** llama a la IA ni implementa `RecipeGenerator` ni un proveedor real (`real-ai-provider`).
- **No** parsea ni valida la respuesta de la IA (eso es `recipe-validator`).
- **No** implementa los flujos `suggestByPreferences` / `searchByText` / `generateFromIngredients`:
  solo compone el texto; quien lo invoca es el proveedor real.
- **No** elige la forma HTTP ni el rol de los mensajes del proveedor: devuelve el prompt ya separado
  en `system` y `user`; el transporte es de `real-ai-provider`.
- **No** persiste, no cachea y **no** calcula ni incluye la `preferenceSignature`: la firma es la
  clave de caché del Lote, no parte del prompt.
- **No** hace UI ni navegación.
- **No** cambia la Taxonomía (`taxonomy-in-code`) ni `PreferenceProfile`; solo los consume.
- **No** añade dependencias nuevas.

## Job Story

Cuando la app necesite pedir recetas a la IA para un perfil,
quiero componer un prompt que exprese el patrón de dieta y las restricciones duras con las etiquetas y
los `id` de la Taxonomía y un contrato de salida estructurado,
para que el modelo devuelva solo opciones clasificables y no pueda ignorar la seguridad del usuario.

## Users And Permissions

- **Usuario local**: no interactúa con esta feature (no hay UI). Se beneficia de que sus preferencias
  y, sobre todo, sus restricciones duras se comuniquen al modelo.
- **Desarrollador / agente**: invoca el constructor desde tests y (más tarde) desde el proveedor real,
  sin permisos, sin red y sin emulador.

## Acceptance Scenarios

### Scenario 1: dieta y restricciones duras aparecen siempre (verificación exigida)

Given la Taxonomía real y un `PreferenceProfile(dietPatternId = "vegetarian",
hardRestrictionIds = ["gluten", "lactose"])` con `defaultServings`,
When se construye el prompt,
Then el prompt **contiene** el `id` y la `label` del patrón de dieta y el `id` y la `label` de **cada**
restricción dura.

### Scenario 2: sin restricciones duras, la sección sigue presente

Given un `PreferenceProfile` con `hardRestrictionIds = []`,
When se construye el prompt,
Then el prompt **sigue conteniendo** la sección de restricciones duras, indicando explícitamente que
no hay ninguna (la sección de seguridad nunca desaparece).

### Scenario 3: pide 6 recetas y el contrato de salida de §4

Given un `PromptRequest` válido,
When se construye el prompt,
Then el prompt pide exactamente `PromptBuilder.RECIPES_PER_GENERATION` (= 6) recetas y contiene las
claves del envelope `{ "recipes": [...] }` (`recipes`, `title`, `description`, `categoryId`,
`mealTypeId`, `cuisineId`, `totalMinutes`, `prepMinutes`, `cookMinutes`, `difficultyId`, `servings`,
`ingredients`, `steps`, `tips`, `nutrition`, `declaredAllergens`).

### Scenario 4: el prompt enumera los `id` válidos (nada clasificable en texto libre)

Given la Taxonomía real,
When se construye el prompt,
Then **todos** los `id` de `recipeCategories`, `mealTypes`, `cuisines`, `difficulties` y
`hardRestrictions` aparecen en el prompt, y este instruye a usar **solo** esos `id` en los campos
clasificables.

### Scenario 5: los `promptHint` de dieta y preferencias blandas se inyectan

Given `dietPatternId = "flexitarian"` y `softPreferenceIds = ["real_fooding", "economical"]`,
When se construye el prompt,
Then se incluye el `promptHint` del patrón (`prioriza recetas de base vegetal…`) y el `promptHint` de
cada preferencia blanda.

### Scenario 6: contexto opcional (ajustes, ingredientes libres y notas)

Given un `PromptRequest` con `mealTypeId = "dinner"`, `timeBucketId = "lte_30"`,
`difficultyId = "easy"`, `notes = "sin picante"`, e `ingredients` con un ingrediente de Taxonomía
(`ingredientId = "tomate"`) y un ingrediente libre (`ingredientId = null`, `name = "Pan"`),
When se construye el prompt,
Then el prompt menciona la label de la comida, el límite de `maxMinutes` de la franja, la label de la
dificultad, el texto de `notes`, la label `Tomate` del ingrediente resuelto y el nombre `Pan` del
ingrediente libre.

### Scenario 7: ids desconocidos no rompen ni se inventan

Given un `PromptRequest` con `dietPatternId`/`hardRestrictionIds`/`mealTypeId` que **no** existen en la
Taxonomía,
When se construye el prompt,
Then **no** se lanza excepción y esos ids **no** se traducen a etiquetas inventadas (se omiten).

### Scenario 8: determinismo

Given dos `PromptRequest` iguales,
When se construyen sendos prompts,
Then `system` y `user` son **idénticos** (misma entrada → mismo texto).

## Repository Research

### Files Inspected

- `feature_list.json` — definición y verificación de `prompt-builder`; dependencias `taxonomy-in-code`
  y `preference-signature` en `accepted`.
- `docs/domain/taxonomy.md` §1 (regla de oro: la IA solo devuelve ids), §4 (contrato de la respuesta),
  §5 (validación posterior) y §7 (ingrediente libre).
- `docs/domain/taxonomy.json` — valores reales: `dietPatterns` (con `promptHint` en `flexitarian`),
  `hardRestrictions` (11, `keywords`), `softPreferences` (7, `promptHint`), `mealTypes`, `cuisines`,
  `timeBuckets` (`maxMinutes`, `null` en `gt_60`), `difficulties` (`level`), `recipeCategories`
  (18, `fallback: "other"`).
- `app/src/main/java/com/sazon/app/domain/taxonomy/Taxonomy.kt` — colecciones y consultas por id
  (`dietPattern`, `restriction`, `softPreference`, `mealType`, `cuisine`, `timeBucket`, `difficulty`,
  `ingredient`) y campos de cada modelo (`label`, `promptHint`, `maxMinutes`, `level`).
- `app/src/main/java/com/sazon/app/domain/profile/UserProfile.kt` — campos exactos de
  `PreferenceProfile` (`dietPatternId`, `hardRestrictionIds`, `softPreferenceIds`, `cuisineIds`,
  `preferredIngredientIds`, `dislikedIngredientIds`, `difficultyId`, `timeBucketId`,
  `defaultServings`, `updatedAt`).
- `app/src/main/java/com/sazon/app/domain/generator/RecipeGenerator.kt` y `GenerationConstraints.kt` —
  las tres operaciones y los ajustes que el futuro proveedor mapea a `PromptRequest`.
- `app/src/main/java/com/sazon/app/domain/validation/RecipeValidator.kt` — referencia de estilo para
  lógica pura que consume la Taxonomía (no se modifica).
- `app/src/test/java/com/sazon/app/domain/taxonomy/TaxonomyIntegrityTest.kt` — patrón de carga de la
  Taxonomía real por `System.getProperty("taxonomy.asset.path")` y nombres de test en español con `_`.
- `app/build.gradle.kts` — todas las tareas `Test` reciben `taxonomy.asset.path` y dependen de
  `syncTaxonomy`; JUnit ya está. **No hay que tocar el build.**
- `docs/technical-discovery.md` — el dominio es JVM-testeable; `PromptBuilder` figura entre los tests
  obligatorios.
- `docs/risks-and-open-questions.md` — nº de recetas por generación = **6** (resuelta); no quedan
  preguntas que bloqueen esta feature.

### Existing Patterns To Follow

- Paquete de dominio puro `com.sazon.app.domain.<área>`, **sin** `android.*`; tests JUnit 4 en JVM.
- Funciones puras que reciben la `Taxonomy` como parámetro (patrón de `RecipeValidator`); el
  constructor **no** carga assets ni usa coroutines.
- Identificadores de código en inglés; textos y comentarios de dominio en español.
- Verificación única: `./gradlew test` + `./gradlew lint` vía `init.sh`.

### Current Gaps

- No existe `com.sazon.app.domain.prompt` ni ninguna clase de composición de prompt (grep sin
  resultados).
- El contrato de §4 solo está descrito en Markdown; nadie lo materializa en un texto de prompt.
- No hay traducción centralizada `PreferenceProfile` → texto para la IA.

## Technical Approach

### 1. Dónde vive y pureza

Nuevo paquete `com.sazon.app.domain.prompt`. Prohibido importar `android.*`. Solo depende de
`domain.profile` (`PreferenceProfile`) y `domain.taxonomy` (`Taxonomy`), más la stdlib de Kotlin.
Todo el paquete es testeable en JVM.

### 2. API pública

```kotlin
package com.sazon.app.domain.prompt

data class PromptIngredient(
    val name: String,
    val ingredientId: String? = null, // null = ingrediente libre (solo nombre)
)

data class PromptRequest(
    val preferences: PreferenceProfile,
    val servings: Int = preferences.defaultServings,
    val ingredients: List<PromptIngredient> = emptyList(),
    val mealTypeId: String? = null,
    val timeBucketId: String? = null,
    val difficultyId: String? = null,
    val query: String? = null,        // texto libre de la Busqueda con IA
    val notes: String? = null,        // texto libre del paso de ajustes
)

data class BuiltPrompt(
    val system: String, // rol + reglas + contrato §4 + ids validos (depende solo de la Taxonomia)
    val user: String,   // perfil y contexto concretos de esta peticion
)

object PromptBuilder {
    const val RECIPES_PER_GENERATION = 6

    fun build(taxonomy: Taxonomy, request: PromptRequest): BuiltPrompt
}
```

`query` y `notes` son texto libre del **usuario**, no clasificable: la regla "nada clasificable en
texto libre" aplica a la **salida** del modelo para `categoryId`, `mealTypeId`, `cuisineId`,
`difficultyId` y `declaredAllergens`.

### 3. Reparto `system` / `user`

- **`system`** (depende solo de la Taxonomía, cacheable por el llamador): rol de asistente de cocina en
  español, regla "responde **solo** con el JSON pedido", el **contrato de §4** como esqueleto con
  todos sus campos, el conjunto de unidades cerrado (`g`, `ml`, `ud`, `cda`, `cdta` o `null`) y la
  **enumeración de ids válidos**: cada `recipeCategories`, `mealTypes`, `cuisines`, `difficulties` y
  `hardRestrictions` como `id — label`. Incluye la instrucción de usar solo esos `id` en los campos
  clasificables, la obligación de respetar el patrón de dieta y las restricciones duras, y el número
  `RECIPES_PER_GENERATION`. Enumera también las despensa asumida (sal, aceite, pimienta, agua).
- **`user`** (depende de la petición): el Perfil traducido y el contexto opcional.

### 4. Contenido obligatorio de `user`

En este orden (encabezados fijos para que el test sea estable):

1. **`Patrón de dieta:`** siempre. `id — label` y, si existe, su `promptHint`.
2. **`Restricciones duras:`** siempre. `id — label` de cada una, o la palabra `Ninguna` si la lista
   está vacía. Es la sección de seguridad: nunca se omite.
3. **Preferencias blandas:** `id — label` y su `promptHint` de cada una (solo ranking).
4. **Cocinas favoritas:** labels.
5. **Ingredientes que me gustan / que evito:** labels de `preferredIngredientIds` /
   `dislikedIngredientIds`.
6. **Ajustes:** dificultad (`label`), franja de tiempo (`label` y, si `maxMinutes != null`, el límite
   en minutos; si es `null`, "sin límite máximo") y `servings` comensales.
7. **Ingredientes disponibles:** si `ingredients` no está vacío, cada uno por su `label` de Taxonomía
   cuando `ingredientId` resuelve, o por su `name` cuando es libre.
8. **Búsqueda / notas:** si `query`/`notes` no son nulos, se incluyen tal cual.
9. La petición explícita de las recetas.

### 5. Resolución de ids (sin inventar)

Cada `*Id` se resuelve contra la `Taxonomy`. Si no resuelve, **se omite** y no se lanza excepción: la
Taxonomía es la fuente única y la UI solo ofrece ids válidos, así que un id desconocido solo puede
venir de datos corruptos. Es coherente con el Validador (que ignora restricciones desconocidas) y con
la regla de no inventar etiquetas. Riesgo residual anotado en Decisiones.

### 6. Determinismo

Sin `Random`, sin reloj y sin estado mutable. El orden de las listas del perfil se preserva tal cual
llega; la misma entrada produce siempre el mismo `system`/`user`.

### 7. Modo de fallo

El constructor **nunca** lanza por datos de entrada: `PreferenceProfile` con ids desconocidos,
listas vacías o `query`/`notes` nulos se degradan a un prompt válido. No registra log ni persiste.

## Expected File Changes

- `app/src/main/java/com/sazon/app/domain/prompt/PromptBuilder.kt` — **crear**; `PromptIngredient`,
  `PromptRequest`, `BuiltPrompt`, `PromptBuilder` y la composición de `system`/`user`.
- `app/src/test/java/com/sazon/app/domain/prompt/PromptBuilderTest.kt` — **crear**; escenarios 1–8
  sobre la Taxonomía real cargada con `System.getProperty("taxonomy.asset.path")`.
- `docs/technical-discovery.md` — **modificar**; anotar que el Constructor de prompt vive en
  `domain.prompt` (`PromptBuilder`), es puro y consume el Perfil de preferencias y la Taxonomía.
- `docs/domain-model.md` — **not needed**; el contrato de la respuesta ya vive en
  `docs/domain/taxonomy.md` §4. Solo si el implementador añade un concepto de dominio nuevo.
- `docs/risks-and-open-questions.md` — **not needed**; el nº de recetas (6) ya está resuelto y no hay
  preguntas abiertas que afecten a esta feature.
- `app/build.gradle.kts` / `gradle/libs.versions.toml` — **no se tocan** (JUnit y
  `taxonomy.asset.path` ya están).
- `PROGRESS.md` y `feature_list.json` — **modificar al implementar** (evidencia y estado); no en esta
  sesión de planificación.

## Visual Design Impact

- UI involved: **no**.
- Design source: no aplica.
- Screens or states affected: ninguna.
- New design artifact required: no.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio omite este fichero por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — "la IA solo devuelve ids de la Taxonomía" y "la validación es la
  garantía de seguridad" ya viven en `docs/domain/taxonomy.md` §1 y §5.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas no cambian.
- Other docs: `docs/technical-discovery.md` — update ligero (ubicación del Constructor de prompt);
  `PROGRESS.md` y `feature_list.json` — update al implementar.

## Implementation Plan

1. Crear los tipos `PromptIngredient`, `PromptRequest` y `BuiltPrompt` en `domain.prompt`.
2. Componer `system`: rol, contrato §4, unidades, enumeración de ids válidos y regla "solo esos ids" +
   patrón de dieta/restricciones + 6 recetas.
3. Componer `user`: traducir el Perfil y el contexto opcional, con las secciones fijas de dieta y
   restricciones duras.
4. Escribir `PromptBuilderTest` con los escenarios 1–8, cargando la Taxonomía real por
   `taxonomy.asset.path`.
5. Ejecutar `init.sh`, corregir lo que salga y actualizar `technical-discovery.md`.
6. Registrar evidencia y estado en `feature_list.json` y `PROGRESS.md` (lo hace el implementer).

## Implementation Tasks

- [ ] Crear `PromptIngredient`, `PromptRequest` y `BuiltPrompt` en `com.sazon.app.domain.prompt`.
- [ ] Implementar `PromptBuilder.build(taxonomy, request)` y la constante
      `RECIPES_PER_GENERATION = 6`.
- [ ] Componer `system` con el contrato de §4 y la enumeración de **todos** los ids clasificables.
- [ ] Incluir **siempre** el patrón de dieta y la sección de restricciones duras en `user`.
- [ ] Traducir preferencias blandas, cocinas, ingredientes preferidos/evitados, dificultad, franja y
      comensales a labels/hints.
- [ ] Añadir el contexto opcional (ingredientes libres/de Taxonomía, `query`, `notes`).
- [ ] Omitir ids desconocidos sin lanzar excepción.
- [ ] Escribir `PromptBuilderTest` con los escenarios 1–8 (incluido el perfil sin restricciones).
- [ ] Verificar que `domain.prompt` no importa `android.*`.
- [ ] Ejecutar `init.sh` en verde y actualizar `docs/technical-discovery.md`.

## Verification Plan

- `./gradlew test` → verde, con `PromptBuilderTest` ejecutado y **0 fallos**. Debe incluir
  exactamente lo que exige `feature_list.json`: comparar el prompt de un perfil de ejemplo y
  comprobar que las **restricciones duras** y el **patrón de dieta** aparecen siempre.
- `./gradlew lint` → sin errores nuevos (los warnings preexistentes son aceptables).
- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → código 0 (`test` + `lint`), sin procesos largos.
- Inspección: `com.sazon.app.domain.prompt` no importa `android.*` ni `androidx.*`; no se tocan
  `PreferenceProfile`, la Taxonomía ni el build.

Cobertura E2E: **no aplica**. No hay suite E2E persistente y la feature es lógica pura sin
comportamiento de UI ni de red por sí sola; el test JVM es el nivel correcto y es lo que exige
`feature_list.json`.

`init.sh` **no cambia**: sigue ejecutando el gate no bloqueante (`test` + `lint`) y no arranca
procesos largos.

## Evidence To Capture

- Salida de `./gradlew test` con `PromptBuilderTest`, número de tests y 0 fallos.
- Salida de `./gradlew lint` sin errores nuevos.
- Salida de `init.sh` con código 0.
- Confirmación por inspección de que `domain.prompt` no importa Android y que no se tocó el build.
- Confirmación de que el prompt de ejemplo contiene el patrón de dieta y las restricciones duras, y
  que sin restricciones la sección sigue presente indicando que no hay ninguna.
- Anotación en `feature_list.json` de la evidencia y cambio de estado a `passing`.

## Decisions And Unknowns

### Decisiones fijadas

- **`BuiltPrompt(system, user)`**: separar el bloque estático (rol + contrato + ids válidos, derivado
  solo de la Taxonomía) del bloque por petición (Perfil + contexto) facilita el test y deja el
  transporte del proveedor en `real-ai-provider`.
- **Dieta y restricciones duras siempre presentes**: son seguridad. Aunque las listas estén vacías, la
  sección existe y lo dice ("Ninguna"); nunca se omite silenciosamente.
- **Ids traducidos a `id — label`**: la salida del modelo usa `id`; el `label` ayuda al modelo a elegir
  bien. Los `promptHint` de dieta/soft preferences se inyectan como instrucciones, no como datos.
- **6 recetas**: `RECIPES_PER_GENERATION = 6`, constante pública y testeable, alineada con
  `docs/risks-and-open-questions.md`.
- **Unidades cerradas** (`g`, `ml`, `ud`, `cda`, `cdta` o `null`): se recuerdan en `system`, coherente
  con `recipe-domain-model`.
- **Ids desconocidos se omiten** (nunca se lanzan ni se inventan etiquetas). La Taxonomía es la fuente
  única; un id desconocido solo puede venir de datos corruptos y el Validador tampoco impone lo que no
  describe. Riesgo residual: una restricción dura corrupta no se comunicaría al modelo; aceptable
  porque la UI solo ofrece ids válidos.
- **Sin firma en el prompt**: `preferenceSignature` es clave de caché del Lote, no contenido del
  prompt. La dependencia con `preference-signature` es de cadena de features, no de datos.
- **Sin dependencias nuevas**: solo stdlib de Kotlin + los paquetes de dominio existentes.
- **Microcopy flexible, tokens fijos**: el implementador redacta libremente, pero debe mantener los
  encabezados `Patrón de dieta:` y `Restricciones duras:` y la palabra `Ninguna` para que el test sea
  estable.

### Unknowns

Ninguna ambigüedad de producto. Las preguntas 1 (errores/reintentos de la IA) y 2 (licencias) de
`docs/risks-and-open-questions.md` siguen abiertas pero **no** afectan a esta feature: aquí no hay red
ni assets.

## Validator Checklist

- [ ] La implementación se mantiene en `com.sazon.app.domain.prompt`, es lógica pura y no importa
      `android.*` ni `androidx.*`.
- [ ] El prompt de un perfil de ejemplo incluye **siempre** el patrón de dieta y las restricciones
      duras (y la sección sigue presente, con "Ninguna", si no hay restricciones).
- [ ] El prompt pide exactamente 6 recetas y contiene el contrato de `docs/domain/taxonomy.md` §4.
- [ ] El prompt enumera los ids válidos de `recipeCategories`, `mealTypes`, `cuisines`,
      `difficulties` y `hardRestrictions` e instruye a no usar texto libre en los campos
      clasificables.
- [ ] Los `promptHint` de dieta y preferencias blandas se inyectan cuando existen.
- [ ] El contexto opcional (ajustes, ingredientes libres/de Taxonomía, `query`, `notes`) se refleja en
      el prompt.
- [ ] Los escenarios 1–8 pasan; los ids desconocidos no lanzan y no se inventan etiquetas.
- [ ] No se añaden dependencias ni se modifican `PreferenceProfile`, la Taxonomía ni el build.
- [ ] `docs/technical-discovery.md` queda coherente; `feature_list.json` y `PROGRESS.md` se actualizan
      con evidencia verificable.
- [ ] No se ha colado trabajo de `recipe-validator`, `real-ai-provider` ni `ai-search`.
