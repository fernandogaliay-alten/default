# Feature Implementation Spec: Validador que descarta recetas inseguras

## Source Feature

- `id`: `recipe-validator`
- `area`: `domain`
- `depends_on`: `["recipe-domain-model"]` (satisfecho: `accepted`; `taxonomy-in-code` llega por
  transitividad y también está `accepted`)
- `status`: `not_started` (en el momento de planificar)
- `source`: `feature_list.json`

## Goal

Hoy la app ya sabe representar una Receta (`domain.recipe`) y leer la Taxonomía
(`domain.taxonomy`), pero **nada impide que una respuesta de IA muestre gluten a un celíaco**: no
existe la capa obligatoria de validación que describe `docs/domain/taxonomy.md` §5. Esta feature
entrega el **Validador de recetas** como lógica pura de dominio: recibe la respuesta cruda de la IA
(`{ "recipes": [...] }`), la Taxonomía y las restricciones del usuario; aplica las reglas 1–4 de §5;
y devuelve las recetas aceptadas junto con **el motivo** de cada descarte. Es la capa que garantiza
la seguridad y donde vive la calidad del producto.

Se mantiene en `com.sazon.app.domain.validation`, sin Android, sin UI, sin red y sin persistencia,
para que corra en JVM con `./gradlew test`.

## Non-Goals

- **No** se conecta al `RecipeGenerator`, al flujo de generación (`generation-flow-with-validation`)
  ni a la Búsqueda con IA (`ai-search`). Aquí solo existe el validador y sus tipos.
- **No** se implementa ranking ni `matchScore` (regla 5 de §5 y `results-screen`). Las Preferencias
  blandas **no** descartan y quedan fuera.
- **No** se persiste ni se escribe log: el validador **devuelve** los motivos; el registro local
  (observabilidad de `technical-discovery.md`) lo hace el llamador.
- **No** se construye el prompt (`prompt-builder`) ni se elige proveedor.
- **No** se validan las invariantes de la Taxonomía (ya lo cubre `taxonomy-in-code`).
- **No** se mapea `PreferenceProfile` → restricciones de validación; ese pegamento es de la feature
  de UI que consuma el validador.
- **No** se resuelve el idioma/labels de la receta ni sus assets: eso es de la UI.

## Job Story

Cuando recibo recetas generadas por la IA,
quiero descartar de forma verificable las que incumplen la seguridad o el patrón de dieta del
usuario, y saber por qué se descartó cada una,
para no mostrar nunca una receta insegura y poder diagnosticar si el fallo es del prompt o del
modelo.

## Users And Permissions

- **Desarrollador / agente**: invoca el validador desde código y tests, sin permisos, red ni
  emulador.
- **Usuario local**: no interactúa con esta feature. Su efecto visible (una receta insegura que no
  aparece) llegará con las features de UI.

## Acceptance Scenarios

### Scenario 1: respuesta segura y coherente

Given la Taxonomía real, unas `RecipeConstraints(dietPatternId = "omnivore", hardRestrictionIds = [])`
y una respuesta `{ "recipes": [receta] }` cuya receta es coherente (pasos suman lo que `totalMinutes`)
y declara todos sus alérgenos,
When se llama a `RecipeValidator.validateResponse(...)`,
Then el resultado es `ValidationOutcome.ValidationCompleted` con esa receta en `accepted` y
`discarded` vacío.

### Scenario 2: respuesta malformada (no parsea)

Given una respuesta que no es un JSON con la forma `{ "recipes": [...] }` (p. ej. `"esto no es json"`),
When se valida,
Then el resultado es `ValidationOutcome.Malformed` con un detalle, y **no** se lanza excepción.

### Scenario 3: `*Id` de clasificación desconocido

Given una receta con `difficultyId = "imposible"` (id que no existe en la Taxonomía),
When se valida,
Then se descarta con `DiscardReason.SCHEMA` y el detalle nombra el campo y el id.

### Scenario 4: `categoryId` desconocido usa el fallback declarado

Given una receta con `categoryId = "no-existe"` (la Taxonomía declara `other` como fallback) y el
resto correcto,
When se valida,
Then la receta se acepta con `categoryId = "other"` (copia normalizada), sin descarte.

### Scenario 5: dieta incumplida

Given `dietPatternId = "vegetarian"` y una receta con un ingrediente `ingredientId = "pollo"` (tag
`poultry`, excluido por la dieta),
When se valida,
Then se descarta con `DiscardReason.DIET_PATTERN` y el detalle nombra el ingrediente y el tag.

### Scenario 6: alérgeno detectado por keywords en un ingrediente libre

Given `hardRestrictionIds = ["gluten"]` y una receta con un ingrediente libre
(`ingredientId = null`, `name = "pan"`),
When se valida,
Then se descarta con `DiscardReason.HARD_RESTRICTION` y el detalle cita el alérgeno `gluten` y el
nombre del ingrediente. La resolución es por `keywords`, **nunca** por la lista `ingredients`.

### Scenario 7: alérgeno por ingrediente de la Taxonomía

Given `hardRestrictionIds = ["gluten"]` y un ingrediente con `ingredientId = "pasta"` y
`name = "Espaguetis"` (el nombre no contiene la keyword `pasta`),
When se valida,
Then se descarta con `DiscardReason.HARD_RESTRICTION` porque los `allergens` del ingrediente de la
Taxonomía incluyen `gluten`.

### Scenario 8: normalización de acentos y mayúsculas

Given `hardRestrictionIds = ["sesame"]` y un ingrediente libre `name = "SÉSAMO tostado"`,
When se valida,
Then se descarta con `DiscardReason.HARD_RESTRICTION` (mayúsculas y acentos no impiden el match).

### Scenario 9: incoherencia de tiempos

Given una receta con `totalMinutes = 25` cuyos pasos suman 10 minutos,
When se valida,
Then se descarta con `DiscardReason.INCOHERENT` y el detalle muestra ambas cifras.

### Scenario 10: alérgeno omitido en `declaredAllergens`

Given un usuario **sin** restricciones y una receta con `ingredientId = "leche"` (alergeno `lactose`
según la Taxonomía) y `declaredAllergens = []`,
When se valida,
Then se descarta con `DiscardReason.INCOHERENT`.

### Scenario 11: una receta inválida no tumba el lote

Given una respuesta con dos recetas, una correcta y otra a la que le falta un campo obligatorio,
When se valida,
Then el resultado es `ValidationCompleted` con la correcta en `accepted` y la otra en `discarded`
con `DiscardReason.SCHEMA`.

## Repository Research

### Files Inspected

- `app/src/main/java/com/sazon/app/domain/recipe/Recipe.kt` — `Recipe`, `RecipeIngredient`,
  `RecipeStep`, `MeasureUnit`, `RecipeSource`; campos y opcionales que valida el validador.
- `app/src/main/java/com/sazon/app/domain/recipe/RecipeJson.kt` — `Json { ignoreUnknownKeys;
  encodeDefaults }`; se usa en los tests para fabricar recetas.
- `app/src/main/java/com/sazon/app/domain/taxonomy/Taxonomy.kt` — `Taxonomy` con búsqueda por id
  (`dietPattern`, `restriction`, `ingredient`) y modelos `DietPattern.excludedTags`,
  `HardRestriction.keywords`, `Ingredient.allergens`, `Ingredient.tags`. Entrada principal del
  validador.
- `app/src/main/java/com/sazon/app/domain/taxonomy/TaxonomyJson.kt` — patrón de parser puro con
  `kotlinx.serialization` y `ignoreUnknownKeys`.
- `app/src/test/java/com/sazon/app/domain/taxonomy/TaxonomyIntegrityTest.kt` — patrón de test JUnit 4
  en JVM; carga el JSON real por la propiedad de sistema `taxonomy.asset.path`.
- `app/src/test/java/com/sazon/app/domain/recipe/RecipeSerializationTest.kt` — patrón de fixtures y
  nombres de test en español con `_`.
- `app/build.gradle.kts` — `kotlinx.serialization` y JUnit ya en dependencias; todas las tareas
  `Test` reciben `systemProperty("taxonomy.asset.path", ...)` y dependen de `syncTaxonomy`. **No hay
  que tocar el build.**
- `docs/domain/taxonomy.md` §4 y §5 — contrato de la respuesta y reglas del validador (1–5).
- `docs/domain-model.md` — `Validador de recetas`, estado `discarded(schema | diet_pattern |
  hard_restriction | incoherent)`, escenario 3 y caso borde del ingrediente libre.
- `docs/technical-discovery.md` — "el motivo de descarte es la señal más valiosa"; dominio sin
  Android, testeable en JVM.
- `docs/risks-and-open-questions.md` — preguntas 5 (trazas) y 7 (ingrediente libre) "antes del
  validador".

### Existing Patterns To Follow

- Paquete de dominio puro `com.sazon.app.domain.<área>`, sin `android.*`; tests en JVM.
- Objetos con lógica pura y datos `@Serializable` cuando aplica; textos y comentarios en español,
  identificadores en inglés.
- Tests JUnit 4 en `app/src/test/java/com/sazon/app/domain/...` con nombres en español y `_`.
- Verificación con `./gradlew test` y `./gradlew lint` vía `init.sh`.
- Fallar rápido en infraestructura (Taxonomía), pero el validador **no debe lanzar** ante una
  respuesta de IA mala: devuelve motivo.

### Current Gaps

- **No existe el paquete `domain.validation`** ni ninguna clase de validación (grep sin resultados).
- La respuesta envuelta `{ "recipes": [...] }` no se parsea en ninguna parte:
  `recipe-domain-model` lo dejó explícitamente fuera y es de esta feature.
- No existe normalización de texto (sin acentos, minúsculas) para cruzar `keywords`.
- Las preguntas 5 (trazas) y 7 (ingrediente libre) siguen abiertas y bloquean conceptualmente al
  validador; esta feature las resuelve.

## Technical Approach

### 1. Dónde vive y pureza

Nuevo paquete `com.sazon.app.domain.validation`. Prohibido importar `android.*`. La normalización de
acentos usa `java.text.Normalizer` (JDK; también disponible en Android, no es API de Android). Todo
el paquete es testeable en JVM.

### 2. API pública

```kotlin
data class RecipeConstraints(
    val dietPatternId: String,
    val hardRestrictionIds: Set<String>,
)

enum class DiscardReason { SCHEMA, DIET_PATTERN, HARD_RESTRICTION, INCOHERENT }

data class DiscardedRecipe(
    val reason: DiscardReason,
    val detail: String,        // motivo legible para diagnostico
    val title: String? = null, // null si la receta no llego a decodificarse
)

sealed interface RecipeValidation {
    data class Accepted(val recipe: Recipe) : RecipeValidation
    data class Discarded(val reason: DiscardReason, val detail: String) : RecipeValidation
}

sealed interface ValidationOutcome {
    data class Malformed(val detail: String) : ValidationOutcome
    data class ValidationCompleted(
        val accepted: List<Recipe>,
        val discarded: List<DiscardedRecipe>,
    ) : ValidationOutcome
}

object RecipeValidator {
    const val MAX_STEP_TOTAL_DRIFT_MINUTES = 5

    fun validateResponse(
        rawResponse: String,
        taxonomy: Taxonomy,
        constraints: RecipeConstraints,
    ): ValidationOutcome

    fun validateRecipe(
        recipe: Recipe,
        taxonomy: Taxonomy,
        constraints: RecipeConstraints,
    ): RecipeValidation
}
```

`validateResponse` parsea y delega en `validateRecipe` por cada receta. `RecipeConstraints` se
mantiene desacoplado de `PreferenceProfile` a propósito: el Perfil y los ajustes por generación se
mapean a estas dos listas en la capa que use el validador.

### 3. Parseo de la respuesta envuelta

El validador es dueño del parseo de `{ "recipes": [...] }` (lo cedió `recipe-domain-model`). Con un
`Json { ignoreUnknownKeys = true }` propio (no se toca `RecipeJson`, que solo (de)serializa una
`Recipe` individual):

- Parsear la raíz a `JsonObject` y leer `recipes` como `JsonArray`. Si la raíz no es objeto, falta
  `recipes` o no es array → `ValidationOutcome.Malformed`.
- Decodificar **cada elemento por separado** con `decodeFromJsonElement(Recipe.serializer(), ...)`
  dentro de `try/catch`. Un elemento que no decodifica se convierte en
  `DiscardedRecipe(SCHEMA, ...)` sin tumbar el lote (Scenario 11). Una excepción de
  `kotlinx.serialization` **nunca** escapa del validador.

### 4. Reglas por receta, en orden (taxonomy.md §5)

Para cada `Recipe` decodificada, se evalúa en este orden y se devuelve **el primer** incumplimiento:

1. **Esquema / ids** (`SCHEMA`):
   - `categoryId` desconocido → se **normaliza a `"other"`** (fallback declarado en
     `recipeCategories.fallback`) y no descarta (Scenario 4).
   - `mealTypeId`, `cuisineId` o `difficultyId` desconocidos → descarte `SCHEMA` (no tienen fallback
     declarado) (Scenario 3).
   - `ingredientId` desconocido → se normaliza a `null` (ingrediente libre). **No** descarta: el
     nombre sigue sujeto a la detección por keywords y la seguridad se preserva. Ver Decisiones.
   - Entradas de `declaredAllergens` que no sean restricciones de la Taxonomía → se ignoran
     (no afectan a la seguridad).
2. **Dieta** (`DIET_PATTERN`): sea `dietPattern = taxonomy.dietPattern(constraints.dietPatternId)`.
   Si no es `null`, ningún ingrediente resuelto por `ingredientId` puede tener un `tag` en
   `dietPattern.excludedTags` (Scenario 5). Los ingredientes libres no aportan tags (ver
   Decisiones).
3. **Restricciones duras** (`HARD_RESTRICTION`): para cada ingrediente se reúnen los ids de
   restricción **candidatos**: (a) los `allergens` del ingrediente de la Taxonomía si
   `ingredientId` resuelve, y (b) las restricciones cuyas `keywords` aparezcan en el `name`
   normalizado (esto cubre el ingrediente libre). Si algún candidato está en
   `constraints.hardRestrictionIds` → descarte citando alérgeno e ingrediente (Scenarios 6, 7, 8).
   Las restricciones cuyos ids no existan en la Taxonomía se ignoran (dato corrupto: no se puede
   imponer lo que no se describe).
4. **Coherencia** (`INCOHERENT`):
   - `abs(sum(steps.minutes) - totalMinutes) > RecipeValidator.MAX_STEP_TOTAL_DRIFT_MINUTES` →
     descarte con ambas cifras en el detalle (Scenario 9).
   - Los `allergens` (ids de restricción) de los ingredientes resueltos por `ingredientId` deben
     estar **todos** en `declaredAllergens`; si falta alguno → descarte (Scenario 10).

Si no hay incumplimiento, `Accepted(recipe)` con la copia normalizada (`categoryId = "other"`,
`ingredientId = null` donde proceda).

### 5. Normalización y matching de keywords

Helper interno `IngredientText`:

- `normalize(raw)`: `trim`, minúsculas, descomposición Unicode (`Normalizer.normalize(s, NFD)`),
  eliminación de marcas diacríticas (`\p{Mn}`), colapso de espacios.
- `matchesAny(text, keywords)`: contiene (substring) alguna keyword normalizada. Se elige
  contención deliberadamente **conservadora**: los keywords ya son raíces ("pan", "trigo",
  "nata") y prefiero un falso positivo (descarto y registro) a un falso negativo (sirvo gluten).
  Documentar el trade-off; es deuda aceptable del MVP.

### 6. Modo de fallo

- Respuesta ilegible → `Malformed`, nunca excepción.
- Receta ilegible dentro de un lote legible → descarte `SCHEMA`, el resto sigue.
- Constraints con ids desconocidos → se ignoran esos ids; sin excepción.
- El validador **no** registra log ni persiste; solo devuelve `reason` + `detail`.

## Expected File Changes

- `app/src/main/java/com/sazon/app/domain/validation/RecipeValidator.kt` — crear; `RecipeValidator`,
  `RecipeConstraints`, `DiscardReason`, `DiscardedRecipe`, `RecipeValidation`, `ValidationOutcome` y
  el parseo del envelope.
- `app/src/main/java/com/sazon/app/domain/validation/IngredientText.kt` — crear; normalización y
  matching por keywords (interno del paquete).
- `app/src/test/java/com/sazon/app/domain/validation/RecipeValidatorTest.kt` — crear; escenarios 1–11
  sobre la Taxonomía real cargada con `System.getProperty("taxonomy.asset.path")`.
- `docs/risks-and-open-questions.md` — modificar; resolver las preguntas 5 (trazas) y 7 (ingrediente
  libre) y renumerar el resto, corrigiendo citas afectadas.
- `docs/technical-discovery.md` — modificar; anotar dónde vive el validador y que consume la
  Taxonomía y el modelo de Receta.
- `docs/domain-model.md` — **no needed**; el Validador y sus estados ya están descritos. Solo si el
  implementador cambia nombres de estado, actualizar para mantener coherencia.
- `app/build.gradle.kts` / `gradle/libs.versions.toml` — **no se tocan**: serialización y JUnit ya
  están, y `taxonomy.asset.path` ya se inyecta a todos los tests.

## Visual Design Impact

- UI involved: **no**.
- Design source: no aplica.
- Screens or states affected: ninguna.
- New design artifact required: no.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio omite este fichero por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — "el dominio no depende de Android" y "la validación es pura" ya
  están en `docs/technical-discovery.md`.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas no cambian.
- Other docs: `docs/technical-discovery.md` — update (ubicación del validador);
  `docs/risks-and-open-questions.md` — update (preguntas 5 y 7 resueltas y numeración);
  `PROGRESS.md` y `feature_list.json` — update con estado y evidencia al implementar.

## Implementation Plan

1. Crear `IngredientText` con `normalize` y `matchesAny`.
2. Crear los tipos de la API (`RecipeConstraints`, `DiscardReason`, `DiscardedRecipe`,
   `RecipeValidation`, `ValidationOutcome`) y el parseo del envelope en `RecipeValidator.kt`.
3. Implementar `validateRecipe` con las reglas 1–4 en orden y la normalización de ids con fallback.
4. Escribir `RecipeValidatorTest` con los escenarios 1–11, cargando la Taxonomía real por
   `taxonomy.asset.path`.
5. Resolver las preguntas 5 y 7 en `docs/risks-and-open-questions.md` (y renumerar citas).
6. Ejecutar `init.sh`, corregir lo que salga y actualizar `technical-discovery.md`, `PROGRESS.md` y
   `feature_list.json`.

## Implementation Tasks

- [x] Crear el helper interno `IngredientText` (normalización sin acentos + matching por keywords).
- [x] Definir `RecipeConstraints`, `DiscardReason`, `DiscardedRecipe`, `RecipeValidation` y
      `ValidationOutcome`.
- [x] Parsear el envelope `{ "recipes": [...] }` elemento a elemento, sin lanzar excepciones.
- [x] Implementar la regla de esquema: `categoryId` → `other`, otros ids de clasificación →
      `SCHEMA`, `ingredientId` desconocido → libre.
- [x] Implementar la regla de dieta por `excludedTags` sobre ingredientes resueltos.
- [x] Implementar la regla de alérgenos por keywords sobre todos los ingredientes.
- [x] Implementar la coherencia de tiempos (`MAX_STEP_TOTAL_DRIFT_MINUTES = 5`) y de
      `declaredAllergens`.
- [x] Escribir los escenarios 1–11, incluido el test de `pan` con gluten y el de acentos.
- [x] Verificar que `domain.validation` no importa `android.*`.
- [x] Resolver las preguntas 5 y 7 en `docs/risks-and-open-questions.md` y ajustar la numeración.
- [x] Ejecutar `init.sh` en verde.
- [x] Actualizar `docs/technical-discovery.md`, `PROGRESS.md` y `feature_list.json`.

## Verification Plan

- `./gradlew test` → verde, con `RecipeValidatorTest` ejecutado y 0 fallos. Debe incluir los cinco
  casos que exige `feature_list.json` (esquema inválido, id desconocido, dieta incumplida, alérgeno
  por keywords en ingrediente libre, incoherencia de tiempos) más el caso `pan` → gluten.
- `./gradlew lint` → sin errores nuevos.
- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → código 0 (`./gradlew test` + `./gradlew lint`),
  sin procesos largos.
- Inspección: `com.sazon.app.domain.validation` no importa `android.*`.

Cobertura E2E: **no aplica**. No hay suite E2E persistente y la feature no cambia comportamiento
observable por sí sola (sin UI ni flujo). El test de JVM es el nivel correcto y es lo que exige
`feature_list.json`.

`init.sh` no cambia: sigue ejecutando el gate no bloqueante (`test` + `lint`) y no arranca procesos
largos.

## Evidence To Capture

- Salida de `./gradlew test` con el nombre y el número de tests de `RecipeValidatorTest` y 0 fallos.
- Salida de `./gradlew lint` sin errores nuevos.
- Salida de `init.sh` con código 0.
- Confirmación por inspección de que `domain.validation` no importa Android.
- Las preguntas 5 y 7 movidas a "Resueltas" en `docs/risks-and-open-questions.md`.
- Anotación en `feature_list.json` de la evidencia y el cambio de estado a `passing`.

## Decisions And Unknowns

Decisiones cerradas por esta feature (el implementador las persiste en
`docs/risks-and-open-questions.md`):

- **Pregunta 5 (trazas) — resuelta, conservadora.** La política por defecto de `taxonomy.md` §7 se
  aplica tal cual: la Taxonomía ya marca los posibles trazas con el alérgeno y su keyword (p. ej.
  `avena` con `gluten`). El validador no añade una capa de trazas: si la keyword aparece, descarta.
  La decisión fina de negocio sigue viviendo en `taxonomy.json`; cambiarla es cambiar keywords.
- **Pregunta 7 (ingrediente libre vs keywords) — resuelta.** Un ingrediente libre se resuelve
  **solo** por keywords, nunca contra la lista `ingredients` (`taxonomy.md` §7): se normaliza el
  `name` (minúsculas, sin acentos) y se busca contención de cada keyword de las restricciones
  activas.

Decisiones de diseño de esta spec:

- **Tolerancia de tiempos:** `MAX_STEP_TOTAL_DRIFT_MINUTES = 5`, constante pública y testeable. El
  modelo estima tiempos y los pasos a veces omiten el prep; una tolerancia fija y registrada es
  preferible a una fórmula inventada. Revisable si en la práctica descarta de más.
- **Coherencia de `declaredAllergens`:** se compara contra los `allergens` **curados** de los
  ingredientes resueltos por `ingredientId` (más cualquier match de restricciones activas, que ya
  descartó antes). Así se detecta que el modelo olvida declarar un alérgeno conocido aunque el
  usuario no sea alérgico, sin inventar matching sobre texto libre para todas las restricciones.
- **Ingrediente libre y dieta:** un ingrediente sin `ingredientId` no aporta `tags`, así que no
  puede evaluarse contra `excludedTags`. Es coherente con `taxonomy.md` §7 ("nunca por la lista
  `ingredients`") y con la regla de oro §1 (la IA devuelve ids para todo lo clasificable). Riesgo
  residual anotado: un libre "pollo" no se detectaría como violación de dieta; se detectaría si
  llegara con `ingredientId`.
- **Ingrediente con `ingredientId` desconocido:** se degrada a libre y se sigue validando por
  keywords, en lugar de descartar. Motivo: evita tirar recetas seguras por un id alucinado sin
  perder la seguridad (el nombre sigue cruzándose).
- **`categoryId` desconocido:** se normaliza a `other`, único id con fallback declarado en la
  Taxonomía (`recipeCategories.fallback`). `mealTypeId`, `cuisineId` y `difficultyId` desconocidos
  descartan con `SCHEMA`.

## Validator Checklist

- [ ] La implementación se mantiene en el alcance: sin UI, sin generador, sin prompt, sin
      persistencia, sin red y sin ranking/`matchScore`.
- [ ] Los escenarios 1–11 pasan, incluido `pan` → gluten, la normalización de acentos y la receta
      inválida que no tumba el lote.
- [ ] Se respetan las reglas 1–4 de `docs/domain/taxonomy.md` §5, en ese orden.
- [ ] Una respuesta malformada **no** lanza excepción; devuelve `Malformed`.
- [ ] `com.sazon.app.domain.validation` **no** importa `android.*` y los tests corren en JVM.
- [ ] No se han tocado `app/build.gradle.kts` ni `libs.versions.toml`.
- [ ] Las preguntas 5 y 7 quedan resueltas en `docs/risks-and-open-questions.md` de forma coherente
      con el comportamiento, y la numeración y sus citas se han ajustado.
- [ ] `feature_list.json`, `PROGRESS.md` y `docs/technical-discovery.md` están actualizados y
      coherentes.
