# Feature Implementation Spec: Generador de recetas falso para desarrollo

## Source Feature

- `id`: `fake-recipe-generator`
- `area`: `domain`
- `depends_on`: `["recipe-domain-model"]` (satisfecho: `accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`; el cambio de estado lo hacen
  implementer/validador/orquestador.
- `source`: `feature_list.json`

## Goal

Introducir el **puerto `RecipeGenerator`** (el contrato de dominio para la IA, con sus tres
operaciones) y una implementación **falsa, determinista y sin red** (`FakeRecipeGenerator`) que lo
cumpla, de modo que toda la app pueda construirse y validarse **antes de elegir proveedor de IA** y
sin gastar red ni cuota.

El falso devuelve **recetas fijas** para las tres operaciones (sugerir, buscar, generar). Su salida
depende solo del propio falso (no de la entrada ni del azar), de forma que es reproducible en tests.
El conjunto fijo incluye a propósito **recetas que el validador deberá descartar** (un ingrediente
libre `"Pan"` que activa la restricción de gluten y una receta con carne que incumple patrones de
dieta restrictivos), para que `recipe-validator` y `generation-flow-with-validation` tengan material
de caso negativo.

Es lógica pura de dominio/implementación falsa: **sin Android, sin red, sin persistencia y sin UI**.

## Non-Goals

- **No** se implementa el proveedor real ni HTTP, ni claves, ni `BuildConfig` (`real-ai-provider`).
- **No** se implementa el Validador (`recipe-validator`): el falso devuelve recetas inseguras pero
  **no las filtra ni las descarta**. Aplicar el perfil y las restricciones es del Validador.
- **No** se implementa el catálogo de recetas semilla (`seed-recipe-catalog`): las fixtures del falso
  son **código de desarrollo**, no contenido empaquetado ni assets.
- **No** se implementa el constructor de prompt (`prompt-builder`).
- **No** hay persistencia, caché, inyección de dependencias, ViewModel ni UI
  (`suggestion-batch-cache`, `generation-flow-with-validation`, pantallas).
- **No** se define el modelo de errores/reintentos de la IA (pregunta 6 abierta): el puerto devuelve
  `List<Recipe>` y el falso **nunca falla**. El estado `ai_unavailable` se resolverá al integrar.
- **No** se define la UI del paso de ajustes del generador (`generator-settings-step`).
  `GenerationConstraints` es solo la costura mínima que el puerto necesita.
- **No** se calcula `matchScore` (derivado, no persistido; es de `results-screen`): el falso lo deja
  `null`.

## Job Story

Cuando construyo el Lote de sugerencias, la Búsqueda con IA, el flujo de generación y las pantallas
de resultados antes de elegir un proveedor de IA,
quiero una implementación que cumpla el puerto `RecipeGenerator` y devuelva recetas fijas y
deterministas sin red,
para poder construir y verificar el flujo completo offline y ejercer el caso negativo del Validador
sin coste ni dependencia externa.

## Users And Permissions

- **Desarrollador / agente**: instancia el falso en tests JVM y en builds de desarrollo; no necesita
  red, permisos ni emulador. No hay cuentas ni roles.
- **Usuario local**: no interactúa con esta feature (no hay UI); se beneficia de que la app funcione
  sin proveedor de IA.

## Acceptance Scenarios

### Scenario 1: sugerir devuelve datos fijos y deterministas

Given un `PreferenceProfile` y un `FakeRecipeGenerator`,
When se llama `suggestByPreferences(profile)` dos veces,
Then ambas llamadas devuelven listas `List<Recipe>` **no vacías** e **iguales** entre sí, y cada
receta tiene `source = RecipeSource.AI`.

### Scenario 2: buscar devuelve datos fijos y deterministas

Given un `PreferenceProfile` y dos consultas de texto distintas,
When se llama `searchByText(query, profile)`,
Then cada llamada devuelve una lista no vacía; la misma consulta repetida da una lista igual, y el
falso **ignora el texto** (devuelve sus recetas fijas), por lo que consultas distintas dan el mismo
conjunto fijo.

### Scenario 3: generar devuelve datos fijos y deterministas

Given una `List<RecipeIngredient>` de entrada y unos `GenerationConstraints`,
When se llama `generateFromIngredients(ingredients, constraints)` dos veces,
Then ambas devuelven listas no vacías e iguales; el falso **ignora ingredientes y ajustes** y
devuelve sus recetas fijas.

### Scenario 4: las recetas fijas son de esquema válido

Given cualquier `Recipe` devuelta por las tres operaciones,
When se serializa con `RecipeJson.encode` y se vuelve a parsear con `RecipeJson.parse`,
Then la receta recuperada es **igual** a la original (esquema `Recipe` válido y serializable).

### Scenario 5: el conjunto fijo incluye recetas que el Validador debe descartar

Given el conjunto de recetas fijas que devuelve el falso,
Then existe **al menos un disparador de Restricción dura** (un ingrediente **libre** con nombre
`"Pan"` e `ingredientId = null`, que el Validador casará por `keywords` de `gluten`) y **al menos un
disparador de Patrón de dieta** (un ingrediente con `id` real de la Taxonomía con `tags` `meat` o
`poultry`, p. ej. `pollo` o `ternera`), de modo que `recipe-validator` y
`generation-flow-with-validation` dispongan de material de caso negativo.

### Scenario 6: sin red y sin Android

Given el puerto y el falso,
When se compilan y se ejecutan sus tests,
Then corren en **JVM** sin emulador, ninguna clase de `domain.generator` ni del falso importa
`android.*`, no se añade ninguna dependencia HTTP y el falso **no hace ninguna llamada de red**.

## Repository Research

### Files Inspected

- `feature_list.json` — definición, verificación y nota de `fake-recipe-generator`; dependencia
  `recipe-domain-model` en `accepted`.
- `PROGRESS.md` — sesión 013: última aceptada `preference-signature`; camino de verificación.
- `CONTEXT.md` — glosario "Generador de recetas (RecipeGenerator)": puerto con tres operaciones.
- `docs/domain-model.md` — sección "Generador de recetas (RecipeGenerator)": firmas
  `suggestByPreferences(profile)`, `searchByText(query, profile)`, `generateFromIngredients(ingredients,
  constraints)`; todas pasan por el Validador; `ai`/`ai_unavailable`.
- `docs/build-brief.md` — Validation Plan ("se ejecutan con `FakeRecipeGenerator`") y paso de ajustes
  del generador (tiempo, tipo de comida, comensales, dificultad, restricciones, notas).
- `docs/technical-discovery.md` — `FakeRecipeGenerator` (datos fijos, sin red) es la implementación
  por defecto en desarrollo y tests; el dominio depende de la interfaz, no de un SDK.
- `docs/domain/taxonomy.md` y `docs/domain/taxonomy.json` — ids reales (`mealTypes`, `cuisines`,
  `difficulties`, `timeBuckets`, `ingredients`) y `keywords` de `hardRestrictions` (gluten incluye
  `pan`); `pollo`/`ternera` con `tags` `poultry`/`meat`.
- `docs/risks-and-open-questions.md` — pregunta 6 (errores/reintentos de la IA) abierta; riesgo "test
  negativo con `FakeRecipeGenerator`"; `RecipeGenerator` no se elevó a ADR.
- `docs/specs/recipe-domain-model.md` — modelo `Recipe` aceptado (`Recipe`, `RecipeIngredient`,
  `RecipeStep`, `Nutrition`, `RecipeSource`, `MeasureUnit`) y `RecipeJson`.
- `docs/specs/preference-signature.md` — patrón de spec y de tests de dominio puro.
- `docs/specs/taxonomy-in-code.md` — "El puerto `RecipeGenerator` y los ADRs fijan la inversión de
  dependencias como patrón del proyecto".
- `app/src/main/java/com/sazon/app/domain/recipe/Recipe.kt` y `RecipeJson.kt` — esquema y
  serialización reales.
- `app/src/main/java/com/sazon/app/domain/profile/UserProfile.kt`, `ProfileRepository.kt` — patrón de
  puerto en dominio que usa `suspend` + `Flow`.
- `app/src/main/java/com/sazon/app/domain/taxonomy/Taxonomy.kt` — catálogo de consulta por id.
- `app/src/main/java/com/sazon/app/data/AssetTaxonomySource.kt`,
  `data/profile/RoomProfileRepository.kt` — patrón de implementación en `data`.
- `app/src/test/java/com/sazon/app/domain/recipe/RecipeSerializationTest.kt`,
  `domain/profile/PreferenceSignatureTest.kt` — patrón de test JUnit 4, nombres en español con `_`,
  helpers `private fun`.
- `app/build.gradle.kts` — `kotlinx.serialization`, `kotlinx.coroutines.core` (main) y
  `kotlinx.coroutines.test` (test) ya presentes; **no hace falta tocar el build**.

### Existing Patterns To Follow

- **Inversión de dependencias**: el puerto (interfaz) vive en `domain`; las implementaciones viven en
  `data` (`ProfileRepository` → `RoomProfileRepository`; parseo de dominio → `AssetTaxonomySource`).
- Paquetes de dominio puros `com.sazon.app.domain.<área>`, **sin** `android.*`.
- Identificadores de código en inglés; comentarios y mensajes de dominio en español.
- `suspend` + corrutinas son el patrón de los puertos de dominio (`ProfileRepository`).
- Tests JUnit 4 en `app/src/test/...`, sin Robolectric cuando no se necesita Android; nombres en
  español con `_`.
- Verificación única: `./gradlew test` + `./gradlew lint` vía `init.sh`.
- Los `id` de la Taxonomía son `snake_case`, en inglés y estables.

### Current Gaps

- **No existe el puerto `RecipeGenerator`** en el código (solo en los documentos).
- **No existe ninguna implementación falsa** del generador.
- **No existe ningún test** que cubra las tres operaciones offline.
- No existe ningún modelo de entrada del generador (`GenerationConstraints`) ni el modelo de ajustes
  del paso 2 (eso será de `generator-settings-step`).

## Technical Approach

### 1. Puerto `RecipeGenerator` (dominio)

Nuevo paquete puro `com.sazon.app.domain.generator`:

```kotlin
interface RecipeGenerator {
    suspend fun suggestByPreferences(profile: PreferenceProfile): List<Recipe>
    suspend fun searchByText(query: String, profile: PreferenceProfile): List<Recipe>
    suspend fun generateFromIngredients(
        ingredients: List<RecipeIngredient>,
        constraints: GenerationConstraints,
    ): List<Recipe>
}
```

- Las tres operaciones son las de `docs/domain-model.md`, con los mismos nombres.
- `suspend` porque la implementación real hará E/S; el dominio ya depende de corrutinas.
- Devuelven `List<Recipe>` directamente. **No** se introduce un `Result`/sealed de error aquí: la
  pregunta 6 (errores/reintentos, `ai_unavailable`) sigue abierta y se resolverá al integrar el
  proveedor. El falso nunca falla.

### 2. `GenerationConstraints` (dominio)

`data class GenerationConstraints` en `com.sazon.app.domain.generator`, costura mínima del paso de
ajustes descrito en `docs/build-brief.md`:

- `profile: PreferenceProfile` — patrón de dieta, restricciones duras y preferencias de base.
- `servings: Int`
- `mealTypeId: String? = null`
- `timeBucketId: String? = null`
- `difficultyId: String? = null`
- `notes: String? = null`

Se empaqueta el `profile` dentro de `constraints` para respetar la firma documentada
`generateFromIngredients(ingredients, constraints)` de `docs/domain-model.md`. Es una costura
deliberadamente mínima: la UI que la rellena (`generator-settings-step`) es otra feature y podrá
extenderla.

### 3. `FakeRecipeGenerator` (implementación)

`class FakeRecipeGenerator : RecipeGenerator` en `com.sazon.app.data.generator`, coherente con el
patrón de implementaciones en `data`. Kotlin puro: sin Android, sin red, sin dependencias nuevas.

- **Fixtures fijas** en un `val` privado: varias `Recipe` con `source = RecipeSource.AI`,
  `matchScore = null` y `id` de relleno deterministas (p. ej. `fake-...`). Se construyen con el
  esquema real de `domain.recipe`.
- Las **tres operaciones devuelven recetas fijas ignorando su entrada**: la salida depende solo del
  falso. No se usa `Random`, ni reloj, ni `UUID`; las listas son inmutables.
- Los ingredientes usan `id` reales de la Taxonomía donde aplica (`tomate`, `arroz`, `pollo`…), y se
  incluye al menos un ingrediente **libre** (`ingredientId = null`) con nombre `"Pan"` para el caso
  negativo de gluten.
- El falso **no filtra** recetas inseguras: esa es la responsabilidad de `recipe-validator`.

### 4. Determinismo

La determinación es estructural: mismos valores de `data class`, sin estado mutable ni fuentes de
entropía. El test lo comprueba llamando dos veces y comparando con `assertEquals`, y encadenando
`RecipeJson.encode`/`parse` para validar el esquema.

## Expected File Changes

- `app/src/main/java/com/sazon/app/domain/generator/RecipeGenerator.kt` — **crear**; puerto con las
  tres operaciones.
- `app/src/main/java/com/sazon/app/domain/generator/GenerationConstraints.kt` — **crear**; modelo de
  entrada mínimo de `generateFromIngredients`.
- `app/src/main/java/com/sazon/app/data/generator/FakeRecipeGenerator.kt` — **crear**; implementación
  falsa determinista y sus fixtures fijas (seguras + inseguras).
- `app/src/test/java/com/sazon/app/data/generator/FakeRecipeGeneratorTest.kt` — **crear**;
  escenarios 1–6.
- `docs/technical-discovery.md` — **modificar**; anotar la ubicación del puerto
  (`domain.generator`) y del falso (`data.generator`), puros y sin Android.
- `docs/domain-model.md` — **modificar (mínimo)**; concretar junto a las tres operaciones que
  `constraints` es `GenerationConstraints`.
- `PROGRESS.md` y `feature_list.json` — **modificar al implementar**; estado y evidencia (no en esta
  sesión de planificación).

No se toca `app/build.gradle.kts` ni `libs.versions.toml` (serialización y corrutinas ya están), ni
`domain.recipe`, `domain.profile`, `data.profile/**` ni los assets.

## Visual Design Impact

- UI involved: **no**.
- Design source: no aplica.
- Screens or states affected: ninguna.
- New design artifact required: no.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio omite este fichero por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no introduce una regla MUST/MUST NOT nueva de alcance global; la
  pureza del dominio y la inversión de dependencias ya están documentadas.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas de trabajo no cambian.
- Other docs: `docs/technical-discovery.md` — update ligero (ubicación del puerto y del falso);
  `docs/domain-model.md` — update mínimo (`GenerationConstraints`); `PROGRESS.md` y
  `feature_list.json` — update al implementar.

## Implementation Plan

1. Crear `RecipeGenerator` y `GenerationConstraints` en `com.sazon.app.domain.generator`.
2. Crear `FakeRecipeGenerator` en `com.sazon.app.data.generator` con las fixtures fijas, incluidas
   las inseguras (ingrediente libre `"Pan"` y un ingrediente con `tags` de carne).
3. Escribir `FakeRecipeGeneratorTest` con los escenarios 1–6.
4. Ejecutar `./gradlew test` y `./gradlew lint` vía `init.sh`; corregir lo que salga.
5. Actualizar `docs/technical-discovery.md` y `docs/domain-model.md`.
6. Registrar evidencia y estado en `feature_list.json` y `PROGRESS.md` (lo hace el implementer).

## Implementation Tasks

- [x] Crear `com.sazon.app.domain.generator.RecipeGenerator` con las tres operaciones `suspend`.
- [x] Crear `GenerationConstraints` con `profile`, `servings` y los ajustes opcionales.
- [x] Crear `FakeRecipeGenerator : RecipeGenerator` en `data.generator`.
- [x] Definir fixtures fijas `source = AI`, `matchScore = null` e `id` deterministas.
- [x] Incluir el caso negativo: ingrediente libre `"Pan"` (gluten) y un ingrediente de carne
      (`pollo`/`ternera`).
- [x] Garantizar determinismo: sin `Random`, sin reloj, sin `UUID`; salida independiente de la entrada.
- [x] Escribir los tests de determinismo de las tres operaciones (escenarios 1–3).
- [x] Escribir el test de ida y vuelta `RecipeJson` de las fixtures (escenario 4).
- [x] Escribir el test del material de caso negativo (escenario 5).
- [x] Verificar que `domain.generator` y `data.generator` no importan `android.*` (escenario 6).
- [x] Ejecutar `init.sh` en verde y actualizar `docs/technical-discovery.md` y `docs/domain-model.md`.
- [x] Registrar evidencia y estado en `feature_list.json` y `PROGRESS.md`.

## Verification Plan

- `./gradlew test` → verde, con `FakeRecipeGeneratorTest` ejecutado y **0 fallos**. Para depurar:
  `./gradlew testDebugUnitTest --tests "com.sazon.app.data.generator.FakeRecipeGeneratorTest"`
  (en este proyecto `./gradlew test --tests` no acepta `--tests`: `test` es la tarea de ciclo de vida
  de AGP; ver `PROGRESS.md`, sesión 014).
- `./gradlew lint` → sin errores nuevos (los ~20 warnings preexistentes son aceptables).
- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → código 0.
- Inspección: ni `com.sazon.app.domain.generator` ni `data.generator` importan `android.*`; el build no
  gana dependencias HTTP; no hay llamadas de red.
- Comprobación semántica: llamar dos veces a cada operación devuelve listas iguales; las fixtures
  incluyen el ingrediente libre `"Pan"` y un ingrediente de carne.

Sobre `init.sh`: **no cambia**. Debe seguir ejecutando solo comprobaciones no bloqueantes
(`test` + `lint`), **no** arranca procesos largos y no necesita modos nuevos.

Cobertura E2E: **no aplica**. No hay suite E2E persistente y esta feature no introduce UI ni flujo
visible: las tres operaciones son comportamiento puro de JVM, que es exactamente lo que exige
`feature_list.json`. La prueba end-to-end manual del flujo completo llegará con
`generation-flow-with-validation`.

## Evidence To Capture

- Salida de `./gradlew test` con `FakeRecipeGeneratorTest`, número de tests y 0 fallos.
- Salida de `./gradlew lint` sin errores nuevos.
- Salida de `init.sh` con código 0.
- Demostración de determinismo: dos llamadas a cada operación devuelven listas iguales.
- Confirmación de que las fixtures incluyen el ingrediente libre `"Pan"` y un ingrediente de carne.
- Confirmación por inspección de que no hay imports de Android ni dependencias de red nuevas.
- Anotación en `feature_list.json` de la evidencia y cambio de estado a `passing` (al implementar).

## Decisions And Unknowns

### Decisiones fijadas

- **Puerto en `domain.generator`, falso en `data.generator`**: respeta la inversión de dependencias
  usada por `ProfileRepository`/`AssetTaxonomySource` y deja el dominio libre de código no productivo.
- **Tres operaciones `suspend`** con los nombres de `docs/domain-model.md`.
- **`GenerationConstraints` empaqueta el `profile`**: así `generateFromIngredients` conserva la firma
  de dos argumentos documentada y recibe todo el contexto (perfil + ajustes de la generación). Es una
  costura mínima; `generator-settings-step` posee su UI y podrá extenderla.
- **Salida fija e independiente de la entrada**: el enunciado pide "recetas fijas"; por eso el falso
  ignora perfil, consulta e ingredientes y es 100 % determinista.
- **`source = AI` y `matchScore = null`**: el generador representa a la IA; el score es derivado y no
  persistido (`results-screen`).
- **Modelo de error diferido** (pregunta 6): `List<Recipe>` y el falso nunca falla.
- **Las fixtures inseguras no se filtran**: filtrar es de `recipe-validator`; hasta entonces los
  llamantes no deben asumir seguridad.

### Unknowns

- **Pregunta 6 abierta** (errores/reintentos, `ai_unavailable`): el puerto puede necesitar un envoltorio
  (`Result`/sealed) al integrar el proveedor; se acepta como coste de no inventar el modelo ahora.
- **`id` de recetas `ai`**: `docs/domain-model.md` dice que la app asigna el `id` al persistir; el
  falso devuelve `id` de relleno deterministas y `suggestion-batch-cache` deberá reasignarlos.
- **Solape con el catálogo semilla**: las fixtures del falso y las recetas `seed` pueden coincidir en
  título; la deduplicación es responsabilidad de `suggestion-batch-cache`.
- **`GenerationConstraints` puede crecer** con `generator-settings-step`; la spec lo delimita como
  costura, no como modelo final.

## Validator Checklist

- [ ] La implementación se mantiene en el alcance: sin Validador, sin proveedor real/HTTP, sin
      persistencia/caché, sin UI, sin catálogo semilla y sin cálculo de `matchScore`.
- [ ] El puerto `RecipeGenerator` tiene exactamente las tres operaciones documentadas.
- [ ] `FakeRecipeGenerator` devuelve listas **no vacías y deterministas** para las tres operaciones.
- [ ] No hay `Random`, reloj, `UUID` ni estado mutable; la salida es independiente de la entrada.
- [ ] Todas las fixtures pasan la ida y vuelta de `RecipeJson` y tienen `source = AI`.
- [ ] El conjunto fijo incluye el disparador de Restricción dura (ingrediente libre `"Pan"`) y el
      disparador de Patrón de dieta (ingrediente de carne).
- [ ] Ni `domain.generator` ni `data.generator` importan `android.*`; los tests corren en JVM y no se
      añaden dependencias de red.
- [ ] `init.sh` queda en verde y `docs/technical-discovery.md` y `docs/domain-model.md` quedan
      coherentes con lo implementado.
- [ ] `feature_list.json` y `PROGRESS.md` quedan actualizados con evidencia verificable.
