# Feature Implementation Spec: Detalle de receta con raciones ajustables

## Source Feature

- `id`: `recipe-detail-screen`
- `area`: `ui`
- `depends_on`: `["results-screen"]` (`accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`.
- `source`: `feature_list.json`
- Nota de la feature: "Nutrición opcional y unidades (`MeasureUnit`: `g`/`ml`/`ud`/`cda`/`cdta`,
  unidad nullable) resueltas en `recipe-domain-model`." Verificación exigida: `./gradlew test` cubre el
  **escalado de raciones**, incluido el **redondeo de cantidades no lineales**; prueba manual: cambiar
  las raciones **actualiza las cantidades mostradas**.
- Capturas obligatorias (gate de UI) **ya presentes**: ver `Visual Design Impact`.

## Goal

Entregar la **pantalla de Detalle de receta** real: el usuario abre una receta (desde Resultados o
desde la Home), ve el **hero**, la tarjeta superpuesta con título/descripción/meta, las pestañas
**Ingredientes · Pasos · Notas**, **ajusta las raciones** con un stepper `− N +` de forma que las
**cantidades se recalculan y redondean por unidad**, recorre los **pasos con su duración** y consulta
**tips y nutrición básica opcional**.

La pieza central es el **escalado de raciones** como lógica **pura y testeable en JVM**
(`ServingScaler`), con **redondeo por unidad** (no lineal): gramos/mililitros a múltiplos de 5,
`ud`/`cda`/`cdta` a medios, y la unidad `null` ("al gusto") **no se escala**. Se acompaña de estado
puro (`RecipeDetailState`, `RecipeDetailTab`), un formateador de cantidades (`IngredientFormat`) y un
formateador de meta (`RecipeDetailFormat`), más una capa Compose delgada y **reutilizable**
(`RecipeDetailScreen`).

## Non-Goals

- **No** se implementa **Favoritos** (`favorites`): el botón flotante de corazón y el "Guardar" de la
  barra inferior se pintan como **costura** (`isSaved`/`onToggleSave`) con efecto **no-op** en esta
  feature; persistir/consultar favoritos es de `favorites`.
- **No** se implementa el **modo cocina** con temporizadores: "Empezar a cocinar" es una **costura
  no-op** (`onStartCooking`); el build-brief lo deja explícitamente fuera del MVP.
- **No** se implementa "Añadir a la lista de la compra" (excluido por la captura y por el build-brief:
  lista de la compra fuera del MVP).
- **No** se persiste nada: ni favoritos, ni el estado del detalle, ni "recetas abiertas hace poco"
  (esta última es de `favorites`/Home).
- **No** se implementa la **Búsqueda con IA** (`ai-search`) ni el **proveedor real**
  (`real-ai-provider`): sin red.
- **No** se implementa la pestaña **Favoritos** (`SectionPlaceholder` intacto) ni se toca su contenido.
- **No** se calcula nutrición (solo se **muestra** la opcional ya presente); sin `matchScore` nuevo.
- **No** se toca `domain.recipe`, `domain.validation`, `domain.generation`, `domain.generator`,
  `domain.prompt`, `domain.suggestions`, `domain.profile`, `data/**`, la Taxonomía, `seed-recipes.json`,
  Room/Perfil ni `init.sh`.
- **No** se añaden `navigation-compose`, iconos (`material-icons-*`), imágenes (Coil) ni DI; **sin
  dependencias nuevas** (`app/build.gradle.kts` y `gradle/libs.versions.toml` intactos).

## Job Story

Cuando he abierto una receta que me interesa,
quiero ver sus ingredientes con cantidades, ajustar para cuántos comensales cocino y seguir sus pasos
con sus tiempos,
para cocinarla con las cantidades correctas sin hacer cuentas a mano, y de paso leer los tips y la
nutrición.

## Users And Permissions

- **Usuario local**: dueño único del dispositivo. Abre el detalle desde una tarjeta de Resultados o de
  la Home; ajusta raciones, cambia de pestaña y vuelve con "Atrás".
- **Desarrollador / agente**: cubre en JVM el escalado y su redondeo no lineal, el formato de
  cantidades, el estado y los formateos; el acabado visual lo revisa la persona en el emulador
  (AGENTS.md no automatiza UI).
- **Sistema**: no hay red ni persistencia; la receta ya viene validada y con sus unidades resueltas.

## Acceptance Scenarios

### Scenario 1: escalado de raciones (verificación exigida)

Given la receta semilla `seed-tortilla-patatas` (`servings = 4`) con `Patata 600 g`, `Huevo 6 ud`,
`Cebolla 1 ud`, `Aceite de oliva 150 ml` y `Sal 1` (`unit = null`),
When se abre el detalle y se ajusta a **2 comensales** (factor `0.5`),
Then las cantidades mostradas son `Patata 300 g`, `Huevo 3 ud`, `Cebolla ½ ud`, `Aceite de oliva 75 ml`
y `Sal → "Al gusto"` (sin escalar); `matchScore` no interviene.

### Scenario 2: redondeo de cantidades no lineales por unidad (verificación exigida)

Given la misma receta a **3 comensales** (factor `0.75`),
When se recalculan las cantidades,
Then se aplica el **paso de redondeo de cada unidad**: `Patata 600 g → 450 g` (múltiplo de 5),
`Huevo 6 ud → 4½ ud` (medios), `Cebolla 1 ud → 1 ud` (`0,75` redondea a `1` con paso `0,5`),
`Aceite 150 ml → 115 ml` (`112,5` redondea a `115` con paso `5`); el redondeo **no es lineal** (no es
`cantidad × factor` sin más) y es **determinista**.

### Scenario 3: a las raciones originales, cantidades intactas

Given cualquier receta,
When `servings` coincide con `recipe.servings` (factor `1`),
Then se devuelven **exactamente** las cantidades originales (sin redondear), incluidos valores como
`0,5 ud`.

### Scenario 4: unidad `null` ("al gusto") no se escala

Given un ingrediente con `unit = null` (p. ej. `Sal`, `Orégano`, `Pimienta`),
When se escalan las raciones (cualquier objetivo),
Then su `quantity` **no cambia** y su etiqueta de cantidad es `"Al gusto"`; nunca aparece una cantidad
numérica para esa unidad.

### Scenario 5: el ajustador de comensales acota y actualiza (prueba manual)

Given el detalle abierto,
When el usuario pulsa `−`/`+` en el stepper "Comensales",
Then el valor se acota a `taxonomy.servings` (`1..12`) y **las cantidades mostradas cambian** en
consecuencia (`ServingScaler.scaledIngredients(recipe, nuevoValor)`), con la caption "Cantidades
ajustadas automáticamente" visible.

### Scenario 6: pestañas Ingredientes · Pasos · Notas

Given el detalle abierto,
When el usuario cambia de pestaña,
Then se muestran: **Ingredientes** (ajustador + filas nombre/cantidad), **Pasos** (timeline vertical
numerado, cada paso con título, descripción y píldora ámbar `"N min"` con `step.minutes`) y **Notas**
(los `tips` y, si existe, la tarjeta de nutrición); el estado de pestaña activa es único.

### Scenario 7: nutrición opcional (con y sin datos)

Given una receta con `nutrition != null`,
When se abre la pestaña Notas,
Then aparece la rejilla compacta `Calorías · Proteínas · Carbohidratos · Grasas` con `kcal`/`protein`/
`carbs`/`fat`;
Given una receta con `nutrition == null`,
When se abre la pestaña Notas,
Then solo se muestran los `tips` (o una línea discreta de "sin información nutricional"), **sin**
inventar cifras y sin error.

### Scenario 8: abrir el detalle desde Resultados y volver sin perder el resultado

Given la lista de Resultados tras una generación,
When el usuario toca una tarjeta,
Then se abre el detalle a pantalla completa sobre el resultado; al pulsar "Atrás", se vuelve a la
lista de Resultados **con el resultado y el borrador del Generador intactos** (no se remonta
`GeneratorFlow` ni se pierde `result`).

### Scenario 9: abrir el detalle desde la Home

Given una tarjeta del carrusel "Sugeridas para ti" o de "Tus recetas" (cuando tenga datos),
When el usuario la toca,
Then se abre el mismo `RecipeDetailScreen` a pantalla completa sobre la Home; "Atrás" vuelve a la Home
sin perder la pestaña ni el Lote.

## Repository Research

### Files Inspected

- `feature_list.json` — `recipe-detail-screen` (`area: ui`, `depends_on: [results-screen]`) y vecinas
  `results-screen` (accepted), `favorites`, `ai-search`; verificación y nota de unidades/nutrición.
- `PROGRESS.md` — última aceptada `results-screen`; camino `init.sh` (`./gradlew test` + `lint`); regla
  de no automatizar UI de emulador; capturas por pantalla (política PR #37).
- `AGENTS.md` — verificación de UI por lógica/estado en JVM cuando sea posible; capturas obligatorias
  antes de implementar; una feature por sesión.
- `CONTEXT.md` — "Receta (`Recipe`)", "Ingrediente de despensa (pantry)", "Comensales habituales".
- `docs/build-brief.md` — MVP slice 6 "Detalle con ingredientes y cantidades, raciones escalables,
  pasos con duración y nutrición básica"; modo cocina y lista de la compra **fuera** del MVP.
- `docs/domain-model.md` — `Recipe`/`RecipeIngredient` (`quantity: Double`, `unit: MeasureUnit?`,
  `isPantry`, `ingredientId`), `RecipeStep` (`order`, `title`, `description`, `minutes`), `Nutrition`
  opcional, `tips`; **edge case "Escalado de raciones: no es lineal (especias, sal) → se redondea por
  unidad y se avisa"**.
- `docs/risks-and-open-questions.md` — unidades resueltas: `MeasureUnit` `g`/`ml`/`ud`/`cda`/`cdta`,
  `null` = "al gusto"; nutrición **opcional** en el esquema.
- `docs/technical-discovery.md` — `ui.results` (Estado/Cabecera/card sin click), `GeneratorFlow` monta
  `ResultsScreen` como *stage* de resultado; "escalado de raciones" listado en verificación JVM.
- `DESIGN.md` — dirección visual: superficies superpuestas (tarjeta blanca sobre la foto), pestañas,
  stepper, timeline con píldora ámbar, cifras tabulares, objetivos ≥48dp, una acción principal
  terracota, capturas por pantalla en `docs/design/concepts/<feature-id>/<pantalla>/`.
- `docs/design/stitch-prompt.md` sección E y `docs/design/stitch/16-detalle-receta.md` — microcopy y
  layout exactos del Detalle (hero + botones flotantes, tarjeta solapada con meta de 4 pares, pestañas
  "Ingredientes · Pasos · Notas", ajustador de comensales, filas con checkbox y cantidad, timeline de
  pasos con píldora ámbar, notas + nutrición, barra inferior "Guardar" + "Empezar a cocinar").
- `docs/design/concepts/recipe-detail-screen/README.md` y `detalle-receta/` — **capturas obligatorias**
  ya presentes (ver `Visual Design Impact`).
- `docs/specs/results-screen.md` — patrón "lógica pura en `ui.results` + Compose delgado"; tarjeta
  **sin corazón ni click** ("el Detalle es de `recipe-detail-screen`").
- `docs/specs/home-offline-blocks.md` — "abrir una receta desde el carrusel o 'Tus recetas' es de
  `recipe-detail-screen`"; las tarjetas `RecipeCard` hoy no navegan.
- `docs/specs/recipe-domain-model.md` — `quantity: Double` "para permitir el escalado de raciones
  futuro"; `unit` enum cerrado "para facilitar el redondeo por unidad".
- `app/src/main/java/com/sazon/app/domain/recipe/Recipe.kt` — modelo exacto (`Recipe`, `RecipeIngredient`,
  `MeasureUnit`, `RecipeStep`, `Nutrition`).
- `app/src/main/java/com/sazon/app/domain/taxonomy/Taxonomy.kt` — `Servings(min, max, default)`,
  `difficulty(id)`, `serving` (`taxonomy.servings`).
- `app/src/main/assets/seed-recipes.json` — datos reales: `g`/`ml` (15..800), `ud` (0,5..8), `cda`
  (`Miel 1 cda`), y `null` ("al gusto": sal, especias, hierbas; siempre `quantity = 1`).
- `app/src/main/java/com/sazon/app/ui/results/{ResultsScreen,ResultRecipeCard,ResultsStatus}.kt` —
  pantalla y tarjeta a reutilizar/enganchar; `ui.results` puro sin Compose.
- `app/src/main/java/com/sazon/app/ui/generator/GeneratorFlow.kt` — host del *stage* de Resultados;
  `result`/`generationId` en `remember`; a enganchar para abrir el detalle.
- `app/src/main/java/com/sazon/app/ui/home/{HomeScreen,RecipeCard,RecipeCardFormat}.kt` — tarjetas de la
  Home (carrusel y grupos) y su formato de meta; a enganchar su click.
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — armazón con barra/FAB y `when(pushed)`; punto
  natural para **hospedar** el detalle sin librería de navegación.
- `app/src/main/java/com/sazon/app/ui/preferences/ServingsStepper.kt` — stepper reutilizable (`− N +`,
  cifras tabulares, límites de `Taxonomy`); se reutiliza **sin cambios**.
- `app/src/main/java/com/sazon/app/ui/theme/{Spacing,Shape,Color,Type}.kt` — tokens (`SazonSpacing`,
  `SazonShapes`, `SazonPillShape`, `SazonComponents.buttonHeight`, `SazonPrimaryContainer`,
  `SazonSecondaryContainer`).

### Existing Patterns To Follow

- **Lógica pura + capa Compose delgada**: los objetos de `ui.detail` (`ServingScaler`,
  `IngredientFormat`, `RecipeDetailFormat`, `RecipeDetailState`) no importan Compose/Android y se testean
  en JVM; los `@Composable` solo pintan y delegan.
- **Reutilización**: `ServingsStepper` (sin cambios), `RecipeCardFormat`/tokens de tema, el degradado
  melocotón→oliva + glifo de plato de `RecipeCard`/`ResultRecipeCard`.
- **Sin librería de navegación**: la navegación es estado puro; el detalle se hospeda con un `remember`
  + overlay (patrón de *stage* de `GeneratorFlow`/`MainShell`).
- **Tests JUnit 4** en `app/src/test/...`, nombres en español con `_` y casos negativos que fallarían si
  la lógica no hiciera nada.
- **Sin imágenes ni iconos externos**: glifos y bloques de imagen con primitivas Compose (decisión ya
  documentada para `AvatarPreview`/`RecipeCard`).
- **Sin dependencias nuevas**; `init.sh` sin cambios.

### Current Gaps

- No existe ningún Detalle: no hay pantalla, ni escalado de raciones, ni timeline de pasos, ni pestañas.
- Las tarjetas (`ResultRecipeCard`, `RecipeCard`) **no son clicables**: no hay forma de abrir una receta.
- No hay formateo de cantidades con unidad ni redondeo por unidad.
- El estado de pestaña activa y de comensales del detalle no existe.

## Technical Approach

Estrategia: **lógica pura en `com.sazon.app.ui.detail` + capa Compose delgada y reutilizable**, sin tocar
el dominio aceptado ni añadir dependencias.

### 1. `ServingScaler` — escalado con redondeo por unidad (pura)

`object ServingScaler` en `com.sazon.app.ui.detail`:

```kotlin
object ServingScaler {
    const val GRAM_STEP = 5.0       // g y ml
    const val UNIT_STEP = 0.5       // ud, cda, cdta

    fun scaledIngredients(recipe: Recipe, targetServings: Int): List<RecipeIngredient>
    fun scaleQuantity(quantity: Double, unit: MeasureUnit?, factor: Double): Double
}
```

- `factor = targetServings.toDouble() / recipe.servings.coerceAtLeast(1)`.
- **Si `factor == 1.0`** (raciones originales): se devuelven las cantidades **intactas** (sin redondear).
- Si no, por unidad:

  | `MeasureUnit` | Paso de redondeo | Mínimo si `quantity > 0` | Ejemplo |
  |---|---|---|---|
  | `GRAM` (`g`) | `5` | `5` | `600 g × 0,5 = 300 g` |
  | `MILLILITER` (`ml`) | `5` | `5` | `150 ml × 0,75 = 112,5 → 115 ml` |
  | `UNIT` (`ud`) | `0,5` | `0,5` | `6 ud × 0,75 = 4,5 → 4½ ud` |
  | `TABLESPOON` (`cda`) | `0,5` | `0,5` | `1 cda × 0,5 = 0,5 → ½ cda` |
  | `TEASPOON` (`cdta`) | `0,5` | `0,5` | (idem) |
  | `null` ("al gusto") | — **no se escala** | — | `Sal 1 → quantity 1` intacta |

- `roundToStep(value, step) = Math.round(value / step) * step` (redondeo **half-up**); `quantity == 0,0`
  se conserva `0,0`. Determinista, sin reloj ni `Random`; **no persiste** nada.
- Los textos ("½", "Al gusto") los resuelve `IngredientFormat`; el escalador solo devuelve números.

### 2. `IngredientFormat` — etiqueta de cantidad (pura)

`object IngredientFormat`:

- `fun quantityLabel(quantity: Double, unit: MeasureUnit?): String`
  - `unit == null` → `"Al gusto"`.
  - mitades → `"½"`, `"1½"`, `"2½"` (nunca `"0.5"`), el resto enteros.
  - sufijo de unidad: `g`, `ml`, `ud`, `cda`, `cdta` → `"300 g"`, `"4½ ud"`, `"½ cda"`.

### 3. `RecipeDetailFormat` — meta y captions (pura)

`object RecipeDetailFormat`:

- `data class Meta(val label: String, val value: String)` y
  `fun metaEntries(taxonomy: Taxonomy, recipe: Recipe): List<Meta>` → los cuatro pares del hero:
  `Tiempo` (`"${totalMinutes} min"`), `Dificultad` (`taxonomy.difficulty(id)?.label ?: id`),
  `Comensales` (`recipe.servings.toString()`), `Calorías` (`nutrition?.let { "${it.kcal} kcal" } ?: "—"`).
- `const val ADJUSTED_CAPTION = "Cantidades ajustadas automáticamente"`.

### 4. `RecipeDetailState` / `RecipeDetailTab` — estado (pure)

```kotlin
enum class RecipeDetailTab(val label: String) { INGREDIENTS("Ingredientes"), STEPS("Pasos"), NOTES("Notas") }

data class RecipeDetailState(val servings: Int, val selectedTab: RecipeDetailTab = RecipeDetailTab.INGREDIENTS) {
    fun setServings(value: Int, limits: Servings): RecipeDetailState
    fun selectTab(tab: RecipeDetailTab): RecipeDetailState
    companion object { fun initial(recipe: Recipe, limits: Servings): RecipeDetailState }
}
```

- `initial` arranca en `recipe.servings.coerceIn(limits.min, limits.max)` y pestaña `INGREDIENTS`.
- `setServings` recorta con `coerceIn` a `limits` (no sale de `1..12`); `selectTab` reemplaza.
- **No** guarda el `Recipe` (para que el estado sea serializable y reutilizable): el escalado es
  `ServingScaler.scaledIngredients(recipe, state.servings)`.

### 5. Capa Compose — `com.sazon.app.ui.detail`

- `RecipeDetailScreen(recipe, taxonomy, isSaved, onToggleSave, onStartCooking, onBack, modifier)`:
  - `Scaffold` sobre `colorScheme.background`.
  - **Hero**: bloque de degradado melocotón→oliva (reutilizar el degradado/glifo de
    `RecipeCard`/`ResultRecipeCard`) con el botón "Atrás" (texto) y el **corazón/guardar flotante**
    (`isSaved`/`onToggleSave`, costura no-op).
  - **Tarjeta superpuesta** blanca (`SazonShapes.large`): título (`headlineLarge`, Manrope),
    descripción (`bodyLarge`) y `metaEntries` en cuatro pares icono+valor (label caption + valor en
    cifras tabulares `"tnum"`).
  - **Fila de pestañas** fija (`RecipeDetailTab.entries`; activo terracota `FilterChip`/`SegmentedButton`).
  - Cuerpo por pestaña:
    - `INGREDIENTS`: label "Comensales" + `ServingsStepper(taxonomy.servings, state.servings, onChange)`
      reutilizado **sin cambios** + caption `ADJUSTED_CAPTION`; lista de filas con checkbox local
      (visual), nombre y `IngredientFormat.quantityLabel(...)` **alineada a la derecha** en cifras
      tabulares, separadas por hairline (`colorScheme.outlineVariant`).
    - `STEPS`: timeline vertical con círculos numerados (terracota) y línea; cada paso con título
      (`titleSmall`), descripción (`bodyLarge`) y píldora **ámbar** (`tertiaryContainer`, nunca rojo)
      `"${step.minutes} min"`.
    - `NOTES`: `tips` como lista; si `nutrition != null`, rejilla compacta
      `Calorías/Proteínas/Carbohidratos/Grasas`.
  - **Barra inferior** fija (`White`/`surface`): `OutlinedButton "Guardar"` (`onToggleSave`) + primario
    terracota `"Empezar a cocinar"` (`onStartCooking`), ambos pills `SazonPillShape` a
    `SazonComponents.buttonHeight`.
  - `servings` y `selectedTab` con `rememberSaveable` + `RecipeDetailStateSaver` (patrón de
    `GeneratorFlow`); el `Recipe` es un parámetro (no se guarda).

### 6. Integración — abrir el detalle sin perder estado

- `MainShell` hospeda el detalle como **overlay**: añade
  `var detailRecipe by remember { mutableStateOf<Recipe?>(null) }` y envuelve el contenido en un `Box`;
  tras el `when(pushed)` pinta `detailRecipe?.let { RecipeDetailScreen(recipe = it, ... onBack = { detailRecipe = null }) }`.
  Así el detalle queda **encima** de la Home o del Generador **sin desmontarlos** (el `result`/borrador
  del Generador se conserva).
- `MainShell` pasa `onOpenRecipe = { detailRecipe = it }` a `HomeScreen` y a `GeneratorFlow`.
- `HomeScreen` recibe `onOpenRecipe: (Recipe) -> Unit` y lo pasa a `RecipeCard`; `RecipeCard` gana
  `onClick: (() -> Unit)? = null` (tarjeta clicable).
- `GeneratorFlow` recibe `onOpenRecipe` y lo pasa a `ResultsScreen`; `ResultsScreen` gana
  `onOpenRecipe: (Recipe) -> Unit` y lo pasa a `ResultRecipeCard`, que gana `onClick: (() -> Unit)? = null`.
- `isSaved = false`, `onToggleSave = {}`, `onStartCooking = {}` (costuras documentadas de `favorites` y
  modo cocina).

## Expected File Changes

**Crear**

- `app/src/main/java/com/sazon/app/ui/detail/ServingScaler.kt` — escalado puro con redondeo por unidad.
- `app/src/main/java/com/sazon/app/ui/detail/IngredientFormat.kt` — etiqueta de cantidad/unidad.
- `app/src/main/java/com/sazon/app/ui/detail/RecipeDetailFormat.kt` — meta (4 pares) y captions.
- `app/src/main/java/com/sazon/app/ui/detail/RecipeDetailState.kt` — `RecipeDetailTab` + estado.
- `app/src/main/java/com/sazon/app/ui/detail/RecipeDetailScreen.kt` — pantalla delgada (hero, tarjeta,
  pestañas, timeline, notas, barra inferior, `Saver`).
- `app/src/test/java/com/sazon/app/ui/detail/ServingScalerTest.kt` — escenarios 1, 2, 3, 4.
- `app/src/test/java/com/sazon/app/ui/detail/IngredientFormatTest.kt` — formateo (½, "Al gusto").
- `app/src/test/java/com/sazon/app/ui/detail/RecipeDetailStateTest.kt` — escenarios 5, 6 y límites.
- `app/src/test/java/com/sazon/app/ui/detail/RecipeDetailFormatTest.kt` — meta con y sin nutrición.

**Modificar**

- `app/src/main/java/com/sazon/app/ui/results/ResultsScreen.kt` — `onOpenRecipe` y paso a la tarjeta.
- `app/src/main/java/com/sazon/app/ui/results/ResultRecipeCard.kt` — `onClick` opcional.
- `app/src/main/java/com/sazon/app/ui/generator/GeneratorFlow.kt` — recibir/pasar `onOpenRecipe`.
- `app/src/main/java/com/sazon/app/ui/home/HomeScreen.kt` — `onOpenRecipe` y paso a las tarjetas.
- `app/src/main/java/com/sazon/app/ui/home/RecipeCard.kt` — `onClick` opcional.
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — hospedar el detalle (overlay) y cablear
  `onOpenRecipe`; costuras `isSaved`/`onToggleSave`/`onStartCooking`.
- `docs/technical-discovery.md` — ubicar `ui.detail` (escalado con redondeo por unidad, pestañas, host).
- `docs/domain-model.md` — apuntar el escalado de raciones a `com.sazon.app.ui.detail.ServingScaler`.
- `DESIGN.md` — registrar `recipe-detail-screen` (`detalle-receta`) como captura aceptada.

**No se toca** `app/build.gradle.kts`, `gradle/libs.versions.toml`, `domain.recipe`,
`domain.validation`, `domain.generation`, `domain.generator`, `domain.prompt`, `domain.suggestions`,
`domain.profile`, `data/**`, `ui.navigation/MainNavigationState.kt`, `ui.preferences/ServingsStepper.kt`,
la Taxonomía, `seed-recipes.json` ni `init.sh`.

## Visual Design Impact

- UI involved: **sí**.
- Design source: `DESIGN.md` + `docs/design/stitch-prompt.md` sección E + `docs/design/stitch/16-detalle-receta.md`.
- Screens or states affected: Detalle poblado (hero + tarjeta solapada + meta), pestaña Ingredientes
  (ajustador + cantidades), pestaña Pasos (timeline con píldoras ámbar), pestaña Notas (tips +
  nutrición), barra inferior fija, y la variante claro/oscuro. El estado "sin nutrición" y el detalle
  abierto sobre la Home/Resultados usan la misma dirección.
- Tokens: `SazonTheme` (claro/oscuro), `SazonSpacing`, `SazonShapes`, `SazonPillShape`,
  `SazonComponents.buttonHeight`, `MaterialTheme.colorScheme.{primary, onPrimary, tertiaryContainer,
  onTertiaryContainer, onSurfaceVariant, outlineVariant}`; cifras tabulares (`"tnum"`) en cantidades,
  comensales y nutrición; objetivos ≥48dp; una única acción principal terracota por pantalla.
- **Capturas de referencia (obligatorias, ya presentes).** Existe una única captura de Stitch del
  Detalle en `docs/design/concepts/recipe-detail-screen/detalle-receta/`:
  - `docs/design/concepts/recipe-detail-screen/detalle-receta/screen.png` (export de la pantalla),
  - `docs/design/concepts/recipe-detail-screen/detalle-receta/code.html` (código del export),
  - `docs/design/concepts/recipe-detail-screen/detalle-receta/DESIGN.md` (notas del sistema del export).
  - Petición/ficha: `docs/design/concepts/recipe-detail-screen/README.md`.
  Stitch no generó variantes por pestaña/estado: las tres pestañas y el estado sin nutrición se
  implementan a partir de esta misma dirección. Ante cualquier contradicción, manda `DESIGN.md`. Un
  defecto visual se corrige con feature correctiva.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace ninguna regla MUST/MUST NOT de nivel repo.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas no cambian.
- `docs/technical-discovery.md`: **update** — ubicar `ui.detail` (escalado con redondeo por unidad,
  pestañas, host overlay en `MainShell`).
- `docs/domain-model.md`: **update** — el escalado de raciones (edge case ya descrito) vive en
  `com.sazon.app.ui.detail.ServingScaler`; sin cambios de entidades.
- `DESIGN.md` y `docs/design/concepts/`: **update** — registrar la captura aceptada `detalle-receta` de
  `recipe-detail-screen`.
- `PROGRESS.md` y `feature_list.json`: **update al implementar** (estado y evidencia), no en esta sesión.

## Implementation Plan

1. Crear `ServingScaler`, `IngredientFormat`, `RecipeDetailFormat` y `RecipeDetailState` (puros).
2. Escribir sus tests JVM con los vectores dorados (escenarios 1–4) y casos negativos.
3. Crear `RecipeDetailScreen` (hero, tarjeta+meta, pestañas, timeline, notas, barra inferior, `Saver`)
   reutilizando `ServingsStepper` y los tokens.
4. Añadir `onClick` a `RecipeCard`/`ResultRecipeCard` y `onOpenRecipe` a `HomeScreen`/`ResultsScreen`/
   `GeneratorFlow`.
5. Hospedar el detalle como overlay en `MainShell` y cablear las costuras.
6. Ejecutar `init.sh` en verde y actualizar `docs/technical-discovery.md`/`docs/domain-model.md`.

## Implementation Tasks

- [ ] Crear `ServingScaler.scaledIngredients(recipe, targetServings)` con `GRAM_STEP = 5`,
      `UNIT_STEP = 0.5`, half-up y unidad `null` sin escalar.
- [ ] Escribir `ServingScalerTest`: tortilla 4→2 y 4→3 (vectores de los escenarios 1 y 2), factor 1
      exacto (escenario 3), `null` sin escalar (escenario 4), `recipe.servings = 0` no divide por cero,
      recorte por límites.
- [ ] Crear `IngredientFormat.quantityLabel` (½/1½, `g`/`ml`/`ud`/`cda`/`cdta`, "Al gusto").
- [ ] Escribir `IngredientFormatTest` (mitades, enteros, "Al gusto", caso negativo: nunca `"0.5"`).
- [ ] Crear `RecipeDetailFormat.metaEntries`/`ADJUSTED_CAPTION` y `RecipeDetailFormatTest` (con y sin
      nutrición; dificultad real y respaldo al id).
- [ ] Crear `RecipeDetailTab`/`RecipeDetailState` (`initial`/`setServings` con `coerceIn`/`selectTab`).
- [ ] Escribir `RecipeDetailStateTest` (arranque en `recipe.servings` acotado, stepper 1..12, cambio de
      pestaña, inmutabilidad, caso negativo fuera de rango).
- [ ] Crear `RecipeDetailScreen` (hero, tarjeta+meta, pestañas, timeline ámbar, notas+nutrición, barra
      "Guardar"/"Empezar a cocinar", `RecipeDetailStateSaver`).
- [ ] Añadir `onClick` a `ResultRecipeCard`/`RecipeCard` y `onOpenRecipe` a `ResultsScreen`/
      `HomeScreen`/`GeneratorFlow`.
- [ ] Hospedar el detalle en `MainShell` (overlay `Box`) y cablear las costuras no-op.
- [ ] Ejecutar `init.sh` en verde y actualizar `docs/technical-discovery.md`/`docs/domain-model.md`.

## Verification Plan

- `./gradlew test` (vía `init.sh`) → **esperado**: suites nuevas en verde sin romper las existentes:
  - `ServingScalerTest` (**verificación exigida**): tortilla 4→2 (`300 g`, `3 ud`, `½ ud`, `75 ml`,
    `Sal` sin escalar) y 4→3 (`450 g`, `4½ ud`, `1 ud`, `115 ml`); factor 1 intacto (incluido `0,5 ud`),
    `null` intacto, determinismo, sin división por cero.
  - `IngredientFormatTest`: `½`/`1½`, `"300 g"`, `"4½ ud"`, `"½ cda"`, `"Al gusto"`; nunca `"0.5"`.
  - `RecipeDetailStateTest`: `initial` acota `recipe.servings` a `1..12`; `setServings` recorta; cambio
    de pestaña único; operaciones inmutables.
  - `RecipeDetailFormatTest`: cuatro pares con dificultad real y respaldo al id; `Calorías = "—"` sin
    nutrición.
- `./gradlew lint` (vía `init.sh`) → **esperado**: 0 errores y sin warnings nuevos en ficheros de la
  feature (los 13 preexistentes se mantienen).
- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → **esperado**: código 0, sin procesos largos.
- Inspección: `ui.detail` puro sin imports Compose/Android en `ServingScaler`/`IngredientFormat`/
  `RecipeDetailFormat`/`RecipeDetailState`; `ServingsStepper` reutilizado sin cambios; sin cambios en
  `app/build.gradle.kts`, `gradle/libs.versions.toml`, dominio, `data/**`, la Taxonomía ni
  `seed-recipes.json`; sin hex sueltos ni iconos externos.
- Manual (emulador, lo revisa la persona; AGENTS.md no automatiza UI):
  - Abrir una receta desde Resultados (y desde la Home) y **cambiar las raciones: las cantidades
    mostradas se actualizan** (incluido el redondeo de `ud`/`ml`); `Sal`/"Al gusto" no cambia.
  - Recorrer las pestañas Ingredientes/Pasos/Notas; ver la píldora ámbar de duración por paso y la
    nutrición (o su ausencia) en Notas.
  - "Atrás" vuelve a Resultados/Home **sin perder el resultado ni el borrador**; "Guardar" y "Empezar a
    cocinar" están presentes (costuras, sin efecto en esta feature); el acabado se compara con la
    captura `detalle-receta`.

Cobertura E2E: **no aplica**. No existe suite E2E persistente y AGENTS.md prohíbe automatizar UI de
emulador; el escalado y los formateos se cubren en JVM y el acabado lo revisa la persona.

Sobre `init.sh`: **no cambia**; sigue ejecutando solo comprobaciones no bloqueantes (`test` + `lint`),
no arranca procesos largos ni necesita modos nuevos.

## Evidence To Capture

- Salida de `./gradlew test` con `ServingScalerTest`, `IngredientFormatTest`,
  `RecipeDetailFormatTest` y `RecipeDetailStateTest`, nº de tests y 0 fallos, y el total.
- Salida de `lint` con 0 errores y sin warnings nuevos en ficheros de la feature.
- `init.sh` con código 0.
- Inspección: `ui.detail` puro sin imports Compose/Android; `ServingsStepper` sin cambios; sin cambios
  en `app/build.gradle.kts`, `gradle/libs.versions.toml`, dominio, `data/**`, la Taxonomía ni
  `seed-recipes.json`.
- Resultado manual en emulador (descripción; captura opcional) del escalado de raciones y de las tres
  pestañas, comparado con la captura `detalle-receta`.
- Anotación en `feature_list.json` de la evidencia y del estado `passing` (al implementar).

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **Redondeo por unidad (verificación exigida)**: `g`/`ml` a múltiplos de `5`, `ud`/`cda`/`cdta` a
  medios, `null` ("al gusto") **no se escala**; half-up; factor 1 devuelve el original intacto. Es un
  heurístico de presentación (el dominio ya describe el escalado no lineal como edge case), revisable
  con feature correctiva; no es una regla de seguridad.
- **`isPantry` no decide el escalado**: manda la **unidad** (p. ej. `Aceite de oliva` `isPantry = true`
  con `150 ml` sí se escala; `Sal` `isPantry = true` con `unit = null` no). Coincide con los datos
  semilla.
- **Detalle hospedado como overlay en `MainShell`** (no `PushedScreen.DETAIL`): permite abrirlo desde la
  Home **y** desde Resultados sin librería de navegación y **sin desmontar** `GeneratorFlow` (se
  conservan `result`/borrador), igual que `results-screen` evitó un destino nuevo para conservar el
  estado.
- **`RecipeDetailState` sin el `Recipe`**: el estado memorizado es `servings` + pestaña (serializable
  con un `Saver`); el `Recipe` es un parámetro y el escalado se computa con `ServingScaler`. Mantiene la
  lógica pura y testeable sin serializar `Recipe`.
- **Card click**: `ResultRecipeCard`/`RecipeCard` ganan `onClick` opcional para cumplir lo que
  `results-screen` y `home-offline-blocks` delegaron explícitamente a esta feature.
- **Corazón/"Guardar" y "Empezar a cocinar" como costuras no-op**: `favorites` y el modo cocina son
  features/no-goals propios; se pintan según la captura pero no persisten ni cocinan (precedente: el
  "Generar recetas" no-op de `generator-settings-step`).
- **Sin `matchScore` en el detalle**: se muestra la receta tal cual (con su meta); el detalle no
  repuntúa.
- **Sin dependencias nuevas** y `init.sh` sin cambios.

### Unknowns

- **Valores exactos de redondeo**: los pasos `5` (g/ml) y `0,5` (ud/cda/cdta) son una elección
  documentada, ajustable como detalle visual/sensación sin cambiar el contrato puro (editar
  `ServingScaler`).
- **`nutrition` ausente en muchas recetas del generador**: la pestaña Notas muestra el estado sin
  nutrición; afinar el microcopy es detalle visual.
- **Process death mostrando el detalle**: `detailRecipe` vive en `remember` (`Recipe` no es
  serializable), así que rotar cierra el detalle (como el `result` de Resultados). No se resuelve aquí.
- **Repaso visual** de hero, tarjeta solapada, timeline y contraste claro/oscuro: queda para la persona
  tras el merge; un defecto visual se corrige con feature correctiva.
- **Abrir detalle desde la pestaña Favoritos**: es de `favorites` (hoy `SectionPlaceholder`); esta
  feature solo engancha Resultados y Home.

## Validator Checklist

- [ ] `ServingScaler` es puro y determinista; el escalado cubre el **redondeo no lineal por unidad**
      (escenarios 1 y 2, **verificación exigida**) y la unidad `null` no se escala (escenario 4).
- [ ] A raciones originales las cantidades quedan **intactas** (escenario 3) y nunca hay división por
      cero (`recipe.servings = 0`).
- [ ] El ajustador de comensales acota a `taxonomy.servings` (`1..12`) y **cambia las cantidades
      mostradas** con la caption "Cantidades ajustadas automáticamente" (escenario 5; prueba manual).
- [ ] Las pestañas Ingredientes · Pasos · Notas se comportan como se describe, con timeline y píldoras
      ámbar de duración; la nutrición es **opcional** y sin datos no inventa cifras (escenarios 6 y 7).
- [ ] Abrir el detalle desde Resultados (y Home) y volver con "Atrás" **no pierde** el resultado ni el
      borrador del Generador (escenarios 8 y 9).
- [ ] `ui.detail` puro está cubierto en JVM con casos negativos; `ServingsStepper` reutilizado sin
      cambios; "Guardar"/"Empezar a cocinar" quedan como costuras documentadas.
- [ ] `domain/**`, `data/**`, la Taxonomía, `seed-recipes.json`, `MainNavigationState.kt`, el build y
      `init.sh` quedan intactos; sin dependencias nuevas, iconos ni imágenes externas.
- [ ] Para UI: `Visual Design Impact` nombra las capturas de
      `docs/design/concepts/recipe-detail-screen/detalle-receta/` (`screen.png`, `code.html`,
      `DESIGN.md`), que **existen** antes de implementar, y la implementación las sigue.
- [ ] `init.sh` en verde y evidencia en `feature_list.json`/`PROGRESS.md`; `docs/technical-discovery.md`
      y `docs/domain-model.md` quedan coherentes.
