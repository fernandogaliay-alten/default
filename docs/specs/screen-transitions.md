# Feature Implementation Spec: Transiciones de movimiento entre pantallas, pasos y contenido

## Source Feature

- `id`: `screen-transitions`
- `area`: `ui`
- `depends_on`: `["design-tokens-and-theme", "onboarding-ingredients-safety-kitchen", "generation-flow-with-validation", "recipe-detail-screen", "cooking-mode", "ai-search"]` — los seis `accepted`.
- `status`: `not_started` en `feature_list.json` a la hora de planificar. Esta spec es el contrato
  planner → implementer → validator. El planner **no** modifica `feature_list.json` ni `PROGRESS.md`.
- `source`: `feature_list.json` (entrada `screen-transitions`, nota: "Ampliación de UI pedida por la
  persona (2026-10-15): 'faltan transiciones y queda visualmente mal'").
- Verificación exigida (resumen): `bash init.sh` en verde **sin dependencias nuevas**; test JVM de la
  política de movimiento contra `DESIGN.md`; test JVM del selector de dirección (adelante/atrás/sin
  movimiento); el comportamiento funcional no cambia (mismos estados, callbacks y destinos; los tests
  existentes siguen verdes sin cambiar aserciones); prueba manual de suavidad por la persona.

## Goal

Sustituir los **cortes secos** de hoy por un **único lenguaje de movimiento, sobrio y coherente**
(fundido + deslizamiento corto) en los cuatro frentes que pidió la persona: (1) navegación principal
(cambio de pestaña y páginas empujadas), (2) overlays (entrada/salida del Detalle y del Modo cocina),
(3) pasos del onboarding y del Generador, y (4) contenido al aparecer (listas, resultados y tarjetas).

Hoy **toda** la navegación es composición directa: `when (navigation.pushed)` / `when (navigation.tab)`
en `MainShell.kt`, los bloques `detailRecipe?.let { }` y `cookingRecipe?.let { }` como overlays, y
`when` de paso en `OnboardingFlow.kt` y `GeneratorFlow.kt`. Los únicos micro-efectos existentes son
aislados (crossfade de imagen de 250 ms, spinner del Generador y pulso del temporizador) y **no se
tocan**.

La pieza central es, como en `design-tokens-and-theme`, **tokens + política pura testeable en JVM**:
los valores del lenguaje de movimiento viven en `com.sazon.app.ui.theme.SazonMotion` (declarados en
`DESIGN.md`, que hoy **no** tiene sección de movimiento), y el selector de dirección
(`adelante` / `atrás` / `sin movimiento`) en `com.sazon.app.ui.motion.MotionPolicy`. La capa Compose
queda reducida a **una** primitiva delgada (`MotionContent`, sobre `AnimatedContent`) que los
puntos de aplicación usan sin reimplementar transiciones.

**Sin dependencias nuevas:** `androidx.compose.animation:animation` ya está en el classpath de
compilación a través de `material3` (ver `Repository Research`).

## Non-Goals

- **No** cambia ningún estado, callback, destino ni microcopy: mismos `MainNavigationState`,
  `MainTab`, `PushedScreen`, `OnboardingProgress`, `GeneratorStep`, `ResultsStatus`/`ResultsHeader`,
  `AiSearchStage` y `CookingModeState`. Los tests existentes deben seguir verdes **sin cambios de
  aserciones**.
- **No** se animan las **pestañas Ingredientes/Pasos/Notas** del Detalle, ni las **hojas del Perfil**
  (`ModalBottomSheet`, que ya anima Material 3), ni el paso **Onboarding → Home** de la puerta
  `AppGate` (`SazonApp`): el brief no los pide y mantenerlos fuera acota la superficie.
- **No** se tocan los micro-efectos ya aceptados: crossfade de la imagen de plato
  (`DishImageArtworkPolicy.CROSSFADE_MILLIS` = 250 ms), animación del `GeneratorGeneratingScreen` ni
  el pulso del temporizador de `CookingModeScreen`.
- **No** se toca ningún token **visual** existente (`Color.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`,
  `CardTokens.kt`, `CardImageOverlay.kt`) ni `DESIGN.md` fuera de lo indicado (bloque `motion:` del
  frontmatter + sección nueva + nota de capturas).
- **No** se implementa navegación nueva, `navigation-compose`, DI, iconos, imágenes ni persistencia.
- **No** se toca `domain/**`, `data/**`, la Taxonomía, `seed-recipes.json`, `assets/**`, Room,
  `init.sh`, `scripts/**`, `app/build.gradle.kts` ni `gradle/libs.versions.toml`.
- **No** se añade manejo propio de "quitar animaciones" del sistema: basta con la escala de duración
  que Compose respeta (ver Unknowns).

## Job Story

Cuando me muevo por Sazón (cambio de pestaña, abro el Generador, entro al Detalle, avanzo un paso del
onboarding o aparecen mis resultados),
quiero que la vista se desplace con un movimiento suave y consistente,
para que la app se sienta tranquila y bien acabada en lugar de dar cortes secos.

## Users And Permissions

- **Usuario local**: percibe el movimiento; no configura nada y no cambia ningún permiso.
- **Desarrollador / agente**: fija los tokens y la política, cablea la primitiva en los puntos de
  aplicación y cubre en JVM los tokens y el selector; el acabado lo revisa la persona en el emulador
  (AGENTS.md no automatiza UI de emulador).
- **Sistema**: sin red, sin permisos y sin almacenamiento.

## Acceptance Scenarios

### Scenario 1: cambio de pestaña (adelante y atrás)

Given la Home en la pestaña Inicio,
When el usuario toca "Favoritos" (adelante) o, desde Perfil, vuelve a "Inicio" (atrás),
Then el contenido nuevo entra con fundido y un deslizamiento horizontal de 24 dp **desde la derecha**
si el índice de `MainTab` crece y **desde la izquierda** si decrece, la capa entrante se pinta
**encima**, y la barra inferior y el FAB siguen presentes sin duplicarse ni parpadear.

### Scenario 2: abrir y cerrar una página empujada

Given la Home,
When se pulsa "＋ Crear receta" (o se envía la Búsqueda con IA) y después "Atrás"/"Cancelar",
Then entrar empuja `PushedScreen.GENERATOR`/`SEARCH` (entra desde la derecha) y salir la saca
(entra desde la izquierda, `MotionDirection.BACKWARD`), sin desmontar ni reasignar
`navigation`/`searchQuery` y sin tocar el borrador del Generador.

### Scenario 3: entrar y salir del Detalle y del Modo cocina

Given una lista de recetas visible,
When el usuario abre una receta (Detalle) y desde ahí "Empezar a cocinar" (Modo cocina), y luego
cierra cada uno,
Then cada overlay entra **desde abajo** (deslizamiento vertical de 24 dp + fundido) y sale hacia
abajo, el Detalle sigue encima de la pantalla anterior sin desmontarla, el Modo cocina sigue encima
del Detalle, y al cerrarlos se vuelve al estado intacto.

### Scenario 4: pasos del onboarding (adelante y atrás)

Given el onboarding en el paso 3 de 7,
When el usuario pulsa el primario ("Siguiente") o "Atrás",
Then el cuerpo del paso entra deslizando horizontalmente según la dirección del índice
(índice creciente → desde la derecha; decreciente → desde la izquierda), el progreso "Paso N de 7" y
el primario anclado no se mueven, y en el paso 1 (sin "Atrás") no hay salto de layout.

### Scenario 5: pasos y estados del Generador

Given el Generador en el paso 1 ("¿Qué tienes?"),
When se pasa al paso 2, se pulsa "Generar recetas" (estado de carga) y llegan los resultados,
Then el cambio de paso desliza en horizontal con la dirección del índice, y los **estados**
(edición → carga → resultados) se funden con deslizamiento vertical, manteniendo intactos el token de
generación, el `LaunchedEffect` que llama a `GenerationService` y los borradores con su `Saver`.

### Scenario 6: aparición de contenido (listas, resultados y tarjetas)

Given una pantalla cuyo contenido aparece o cambia (cuerpo de Resultados por estado, cuerpo de
Favoritos vacío↔poblado, bloques con tarjetas de la Home),
When el contenido aparece o cambia de identidad,
Then se revela con fundido y subida de 24 dp **sin pantalla de carga nueva**, sin reordenar, sin
perder el scroll de los bloques que no cambian y sin alterar ningún estado de la pantalla.

### Scenario 7: los tokens de movimiento coinciden con DESIGN.md (verificación exigida)

Given el bloque `motion:` del frontmatter de `DESIGN.md`,
When el test JVM compara `SazonMotion` con esos valores,
Then `duration`, `slide`, `easing` y `enterOnTop` coinciden **exactamente**; y con una copia mutada
del documento (p. ej. `duration: 999ms`) la comparación **falla**, de modo que el test no pasa en
vacío.

### Scenario 8: selector de dirección (verificación exigida)

Given el destino anterior y el siguiente,
When se consulta `MotionPolicy` para pestañas, páginas empujadas y pasos (onboarding y Generador),
Then devuelve `FORWARD` cuando el avance es hacia adelante, `BACKWARD` cuando retrocede y `NONE`
cuando el destino no cambia, y los desplazamientos son `+24`/`-24`/`0` dp coherentes con la dirección
(con casos negativos que no pasan en vacío).

### Scenario 9: el comportamiento funcional no cambia (regresión)

Given el árbol actual,
When se ejecuta el gate,
Then `bash init.sh` termina con código 0, las 98 suites / 739 tests existentes siguen verdes **sin
cambiar ninguna aserción**, `lint` no añade warnings, y `app/build.gradle.kts` /
`gradle/libs.versions.toml` / `init.sh` / `domain/**` / `data/**` quedan intactos.

## Repository Research

### Files Inspected

- `feature_list.json` (entrada `screen-transitions`, líneas 1569-1585) — brief autoritativo: los cuatro
  frentes, el enfoque (fundido + deslizamiento corto, tokens en `ui/theme`, política pura, sin
  dependencias) y los no-goals.
- `DESIGN.md` — **no existe hoy sección ni token de movimiento**; sí el carácter "tranquilo" y
  "Táctil, sin adornos" (`Product Feel`), las formas/espaciado (`rounded`, `spacing`) y la política de
  capturas (§`Generated Concept Images`, incluidas las **excepciones de capturas** de las correctivas).
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — `when (navigation.pushed)` (Scaffold con
  `MainBottomBar` + FAB, `PushedScreen.GENERATOR` y `PushedScreen.SEARCH`) y `when (navigation.tab)`;
  los overlays son `detailRecipe?.let { RecipeDetailScreen(...) }` y
  `cookingRecipe?.let { CookingModeScreen(...) }`; el `LaunchedEffect(profile.preferences) { ensureBatch }`
  vive **fuera** de esos `when` (los efectos secundarios no se mueven).
- `app/src/main/java/com/sazon/app/ui/navigation/MainNavigationState.kt` — `MainTab` (HOME/FAVORITES/
  PROFILE, con `ordinal`) y `PushedScreen` (GENERATOR/SEARCH); lógica pura sin Compose.
- `app/src/main/java/com/sazon/app/ui/onboarding/OnboardingProgress.kt` y `OnboardingStep` —
  `stepIndex` base cero y `step` (enum); `OnboardingFlow.kt` pinta el cuerpo con `when (progress.step)`
  dentro de `OnboardingScaffold` (que tiene `verticalScroll` y un `Saver` de estado).
- `app/src/main/java/com/sazon/app/ui/generator/GeneratorFlow.kt` — `when` de paso
  (`GeneratorStep.INGREDIENTS`/`SETTINGS`), y un `when` de **stage** (`result != null` / `generationId != null`
  / edición) con `rememberSaveable` + `Saver` por paso y `LaunchedEffect(generationId)`.
- `app/src/main/java/com/sazon/app/ui/generator/GeneratorStep.kt` — enum de 2 pasos con `next()`/`previous()`.
- `app/src/main/java/com/sazon/app/ui/search/AiSearchFlow.kt` — stages por `AiSearchOutcome.stageOf`
  (carga / `Results` / `Blocked` / `NoResults` / `Failure`).
- `app/src/main/java/com/sazon/app/ui/results/ResultsScreen.kt` — cuerpo `when (status)`
  (`Content` con `LazyColumn` de `ResultRecipeCard`, `Empty`, `Unavailable` con/sin semillas);
  `Scaffold` de tamaño completo.
- `app/src/main/java/com/sazon/app/ui/favorites/FavoritesScreen.kt` — `LazyColumn` raíz
  (`fillMaxSize`) con cabecera, buscador, chips y cuerpo vacío↔poblado.
- `app/src/main/java/com/sazon/app/ui/home/HomeScreen.kt` — `LazyColumn` con `item { }`; los bloques con
  tarjetas son "Sugeridas para ti" (carrusel `LazyRow` de `RecipeCard`) y "Tus recetas"
  (`HomeRecipes.saved`).
- `app/src/main/java/com/sazon/app/ui/detail/RecipeDetailScreen.kt` y `ui/cooking/CookingModeScreen.kt`
  — overlays de tamaño completo con `Scaffold` propio; no cambian (las hojas y pestañas quedan fuera).
- `app/src/test/java/com/sazon/app/ui/theme/DesignTokensTest.kt` — patrón de test JVM que lee
  `DESIGN.md` por la propiedad de sistema `design.tokens.path` y compara tokens, con **caso negativo**
  (copia mutada) y `frontmatterSectionMap(...)` **privado** en ese fichero.
- `app/build.gradle.kts` — `systemProperty("design.tokens.path", …)` ya existe y sirve tal cual;
  no hay ninguna dependencia de `androidx.compose.animation`.
- `gradle/libs.versions.toml` + metadatos de módulo de la caché de Gradle — Compose BOM `2026.02.01`;
  `androidx.compose.material3:material3-android:1.4.0` declara `foundation` en su variante **api** y
  `androidx.compose.foundation:foundation-android:1.10.4` declara `androidx.compose.animation:animation`
  en su variante **api** (`animation-android:1.10.4` está resuelto en la caché): `AnimatedContent`,
  `fadeIn/fadeOut`, `slideInHorizontally/slideOutHorizontally`, `slideInVertically/slideOutVertically`,
  `tween`, `FastOutSlowInEasing` y `ContentTransform` están disponibles **en compilación** sin declarar
  nada.
- `init.sh` — gate no bloqueante (`./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`); no
  necesita cambios. Baseline del worktree: **98 ficheros de test / 739 `@Test`** (`git grep`).

### Existing Patterns To Follow

- **Tokens en `ui.theme`, lógica pura en `ui.<capacidad>`, Compose delgado**: `SazonSpacing`/
  `SazonShapes` (`ui.theme`) + `ui.artwork`/`ui.cooking`/`ui.filters`.
- **Test JVM que compara con `DESIGN.md`** por `design.tokens.path`, con caso negativo que falla si el
  token no se compara de verdad (patrón `DesignTokensTest`/`TaxonomyIntegrityTest`).
- **Nombres de test en español**, un escenario por test, con casos negativos.
- **Documentación y comentarios en español**, identificadores en inglés, paquetes por capa.
- **Sin dependencias nuevas** y `init.sh` sin cambios; verificación = `bash init.sh` + inspección +
  prueba manual de la persona (AGENTS.md no automatiza UI de emulador).

### Current Gaps

- No existe ningún token, política ni primitiva de movimiento: `SazonMotion`/`MotionPolicy`/
  `MotionContent` son nuevos y `DESIGN.md` no tiene sección `Motion`.
- `DesignTokensTest` no lee el bloque `motion:` (y su parser de frontmatter es privado, no
  reutilizable tal cual por un test nuevo).
- Los cuatro frentes son `when`/`?.let` directos: no hay ninguna transición que reutilizar.

## Technical Approach

### 1. Tokens de movimiento — `com.sazon.app.ui.theme.SazonMotion` (`ui/theme/Motion.kt`, nuevo)

Objeto **puro** (solo `Int`/`String`/`Boolean`: sin imports de Compose ni Android, para que su test sea
JVM puro, como la política):

```kotlin
object SazonMotion {
    const val DURATION_MILLIS = 240      // toda transición (una sola duración)
    const val SLIDE_OFFSET_DP = 24       // desplazamiento del fundido, en dp
    const val EASING = "standard"        // la capa Compose lo mapea a FastOutSlowInEasing
    const val ENTER_ON_TOP = true        // orden de capas: la capa entrante se pinta encima
}
```

Estos cuatro valores se declaran **también** en el frontmatter de `DESIGN.md` (fuente de verdad
visual, bloque nuevo tras `components:`):

```yaml
motion:
  duration: 240ms
  slide: 24px
  easing: standard
  enterOnTop: true
```

### 2. Selector de dirección — `com.sazon.app.ui.motion.MotionPolicy` (`ui/motion/MotionPolicy.kt`, nuevo)

Lógica **pura** (sin Compose/Android; solo depende de `ui.navigation`, que también es puro):

```kotlin
enum class MotionDirection { FORWARD, BACKWARD, NONE }

object MotionPolicy {
    fun directionOf(fromIndex: Int, toIndex: Int): MotionDirection   // índice creciente/decreciente/igual
    fun tabChange(from: MainTab, to: MainTab): MotionDirection       // ordinal de MainTab
    fun pageChange(from: PushedScreen?, to: PushedScreen?): MotionDirection
    fun enterOffsetDp(direction: MotionDirection): Int               // +24 / -24 / 0
    fun exitOffsetDp(direction: MotionDirection): Int                // -enter (el opuesto)
}
```

Reglas fijadas (contrato):

| Cambio | `FORWARD` | `BACKWARD` | `NONE` |
|---|---|---|---|
| Pestaña (`tabChange`) | ordinal creciente: Inicio → Favoritos → Perfil | ordinal decreciente | misma pestaña |
| Página (`pageChange`) | `null → X` (empujar) y `X → Y` (cambio de página) | `X → null` (sacar) | mismo destino |
| Pasos (`directionOf`) | índice creciente | índice decreciente | mismo índice |

`NONE` = **sin desplazamiento** (solo fundido); con el mismo destino `AnimatedContent` no ejecuta
ninguna transición.

### 3. Primitiva Compose — `com.sazon.app.ui.motion.MotionContent` (`ui/motion/MotionContent.kt`, nuevo)

**Única** primitiva del lenguaje (los cuatro frentes la reutilizan; no se usan `Crossfade` ni
`AnimatedVisibility` para no fragmentar el movimiento):

```kotlin
enum class MotionAxis { HORIZONTAL, VERTICAL }

@Composable
fun <T> MotionContent(
    targetState: T,
    modifier: Modifier = Modifier,
    axis: MotionAxis = MotionAxis.HORIZONTAL,
    direction: (initial: T, target: T) -> MotionDirection = { _, _ -> MotionDirection.NONE },
    content: @Composable (T) -> Unit,
)
```

- Envuelve `AnimatedContent` con `transitionSpec` construido desde los tokens:
  - `HORIZONTAL`: `fadeIn/fadeOut(tween(DURATION_MILLIS, FastOutSlowInEasing))` +
    `slideInHorizontally`/`slideOutHorizontally` con `MotionPolicy.enterOffsetDp/exitOffsetDp` (captura
    `LocalDensity` en el cuerpo del composable y convierte los tokens a px dentro del `transitionSpec`).
    `FORWARD` → entra desde la derecha y la saliente se va a la izquierda; `BACKWARD` al revés.
  - `VERTICAL`: fundido + `slideInVertically`/`slideOutVertically` con `+SLIDE_OFFSET_DP` (entra
    **desde abajo** y sale hacia abajo); `direction` se ignora (documentado).
- `ContentTransform(enter, exit, targetContentZIndex = if (SazonMotion.ENTER_ON_TOP) 1f else -1f,
  sizeTransform = null)`: `sizeTransform = null` evita recorte y saltos de layout (las superficies son
  de tamaño fijo o viven en un contenedor con scroll).
- `label = "sazon-motion"`.

### 4. Puntos de aplicación (fijos; ningún otro)

| Fichero | `targetState` | Eje | Dirección |
|---|---|---|---|
| `MainShell.kt` — contenido de la pestaña | `navigation.tab` | H | `MotionPolicy.tabChange` |
| `MainShell.kt` — destino de nivel superior | `navigation.pushed` | H | `MotionPolicy.pageChange` |
| `MainShell.kt` — Detalle | `detailRecipe` (`Recipe?`) | V | — |
| `MainShell.kt` — Modo cocina | `cookingRecipe` (`Recipe?`) | V | — |
| `OnboardingFlow.kt` — cuerpo del paso | `progress.stepIndex` | H | `MotionPolicy.directionOf` |
| `GeneratorFlow.kt` — paso del asistente | `step` (`GeneratorStep.ordinal`) | H | `MotionPolicy.directionOf` |
| `GeneratorFlow.kt` — stage | clave local `EDIT`/`GENERATING`/`RESULTS` | V | — |
| `AiSearchFlow.kt` — stage | clave local `LOADING`/`RESULTS`/`BLOCKED`/`NO_RESULTS`/`FAILURE` | V | — |
| `ResultsScreen.kt` — cuerpo por estado | clave local `CONTENT`/`EMPTY`/`UNAVAILABLE_SEEDS`/`UNAVAILABLE` | V | — |
| `FavoritesScreen.kt` — cuerpo | `favorites.isEmpty()` (`EMPTY`/`POPULATED`) | V | — |
| `HomeScreen.kt` — bloques con tarjetas | ids visibles de "Sugeridas" y de "Tus recetas" | V | — |

Detalles de integración obligatorios:

- Las claves de stage/estado se derivan **localmente** en la capa Compose (función privada o `val`),
  sin tocar `ResultsStatus`, `AiSearchStage`, `GeneratorStep` ni ningún fichero puro aceptado.
- Los **efectos secundarios no se mueven**: `LaunchedEffect(profile.preferences) { ensureBatch }`,
  `LaunchedEffect(generationId)`, `LaunchedEffect(query, searchId)` y los `Saver` siguen donde están.
  `MotionContent` envuelve **solo** el pintado.
- `OnboardingScaffold` **no cambia** (el progreso, "Atrás" y el primario quedan estáticos); el
  `MotionContent` se coloca dentro de su slot `content`.
- El `Saver` de `MainNavigationState` y los `rememberSaveable` con `Saver` por paso se mantienen tal
  cual (si aparece un choque de claves de `rememberSaveable` bajo `AnimatedContent`, la mitigación es
  `contentKey = { it }` explícito y, si hace falta, `rememberSaveableStateHolder`; ver Unknowns).

### 5. `DESIGN.md` — sección nueva `## Motion`

Además del bloque `motion:` del frontmatter, se añade `## Motion` (tras `## Shapes`) con: el lenguaje
único (fundido + deslizamiento corto de 24 dp, 240 ms, `FastOutSlowInEasing`), la regla de dirección
(adelante = desde la derecha/arriba, atrás = al revés), la regla de capas (entra encima), **dónde**
aplica (los cuatro frentes), **qué no** se anima (pestañas y hojas del Detalle/Perfil, crossfade de
imagen de 250 ms, spinner y pulso del temporizador), la nota de accesibilidad (se respeta la escala de
animaciones del sistema) y la **excepción de capturas** en el estilo ya usado por el repo: no se crea
`docs/design/concepts/screen-transitions/` porque el movimiento **no es capturable** en un `.png`
estático; la dirección visual aplicable es esta sección y un defecto se corrige con una feature
correctiva.

## Expected File Changes

**Crear**

- `app/src/main/java/com/sazon/app/ui/theme/Motion.kt` — `SazonMotion` (tokens puros).
- `app/src/main/java/com/sazon/app/ui/motion/MotionPolicy.kt` — `MotionDirection` + `MotionPolicy`.
- `app/src/main/java/com/sazon/app/ui/motion/MotionContent.kt` — `MotionAxis` + `MotionContent`.
- `app/src/test/java/com/sazon/app/ui/theme/MotionTokensTest.kt` — escenario 7 (tokens vs `DESIGN.md` +
  copia mutada).
- `app/src/test/java/com/sazon/app/ui/motion/MotionPolicyTest.kt` — escenario 8 (selector + offsets).
- `app/src/test/java/com/sazon/app/ui/theme/DesignFrontmatter.kt` — **helper de test** con
  `frontmatterSectionMap(markdown, section)` extraído **tal cual** de `DesignTokensTest` (mismo código,
  visibilidad `internal`), para que los dos tests compartan el parser.

**Modificar**

- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — los cuatro puntos de navegación/overlay.
- `app/src/main/java/com/sazon/app/ui/onboarding/OnboardingFlow.kt` — cuerpo del paso.
- `app/src/main/java/com/sazon/app/ui/generator/GeneratorFlow.kt` — paso + stage.
- `app/src/main/java/com/sazon/app/ui/search/AiSearchFlow.kt` — stage.
- `app/src/main/java/com/sazon/app/ui/results/ResultsScreen.kt` — cuerpo por estado.
- `app/src/main/java/com/sazon/app/ui/favorites/FavoritesScreen.kt` — cuerpo vacío↔poblado.
- `app/src/main/java/com/sazon/app/ui/home/HomeScreen.kt` — los dos bloques con tarjetas.
- `app/src/test/java/com/sazon/app/ui/theme/DesignTokensTest.kt` — solo pasa a usar el helper extraído
  (**sin cambiar ninguna aserción**).
- `DESIGN.md` — bloque `motion:` + sección `## Motion` + excepción de capturas.
- `docs/technical-discovery.md` — ubicar `SazonMotion`/`MotionPolicy`/`MotionContent`, el hecho de que
  `androidx.compose.animation` llega por `material3` (sin dependencia declarada) y la verificación JVM.
- `PROGRESS.md` / `feature_list.json` — los actualiza el implementer/validador, **no** esta sesión.

**No se toca** `app/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `init.sh`,
`scripts/**`, `domain/**`, `data/**`, la Taxonomía, `seed-recipes.json`, `assets/**`, `SazonApp.kt`,
`ui/navigation/MainNavigationState.kt`, `ui/theme/{Color,Type,Shape,Spacing,CardTokens,CardImageOverlay}.kt`,
`ui/detail/**`, `ui/cooking/**`, `ui/results/{ResultsStatus,ResultsHeader,ResultRecipeCard}.kt`,
`ui/preferences/**`, `ui/profile/**`, `ui/artwork/**` ni `docs/design/**`.

## Visual Design Impact

- UI involved: **sí** (movimiento en claro y oscuro; ninguna pantalla ni layout nuevos).
- Design source: `DESIGN.md` — **fuente de verdad**; esta feature **crea** la sección `Motion` y su
  bloque `motion:` en el frontmatter, y el test JVM la mantiene sincronizada.
- Screens or states affected: navegación principal (pestañas y páginas empujadas), overlays (Detalle y
  Modo cocina), pasos del onboarding y del Generador (incluidos los stages carga→resultados) y la
  aparición de contenido (Resultados, Favoritos, bloques de la Home). No cambian colores, tipografía,
  formas ni espaciado.
- Capturas: **no requiere capturas estáticas** (el movimiento no es capturable en un `.png`) y **no se
  crea** `docs/design/concepts/screen-transitions/`; la desviación se registra en `DESIGN.md` citando
  `screen-transitions` (misma clase de excepción que `recipe-artwork` #77, `favorites-sort-controls`
  #85, `profile-stats-heading` #86, `home-recently-opened-group` #91 o `dish-image-settings-toggle`
  #101). El acabado se revisa en el emulador tras el merge y un defecto se corrige con feature
  correctiva.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — no existe en el repositorio (decisión del harness).
- `CONSTRAINTS.md`: **not needed** — no existe; no nace ninguna regla MUST/MUST NOT de nivel repo.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas no cambian.
- `docs/technical-discovery.md`: **update** — ubicación de los tokens/política/primitiva, la
  procedencia transitiva de `androidx.compose.animation` (sin dependencia declarada) y la verificación
  JVM del movimiento.
- `docs/domain-model.md`: **not needed** — no hay entidad, relación ni estado de dominio nuevos.
- `DESIGN.md`: **update** — bloque `motion:` del frontmatter, sección `## Motion` y nota de excepción
  de capturas (hoy no existe ninguna sección de movimiento).
- `docs/build-brief.md`: **not needed** — no es una ampliación de alcance del MVP (ningún Non-Goal del
  brief menciona transiciones) y no cambia el comportamiento de producto; solo el acabado visual.
- `docs/risks-and-open-questions.md`: **not needed** — no hay pregunta de producto nueva; los unknowns
  de esta feature son de implementación y quedan en la sección "Decisions And Unknowns" de esta spec.
- `docs/specs/screen-transitions.md`: esta spec (planner).

## Implementation Plan

1. Crear `SazonMotion` (tokens puros) y declarar el bloque `motion:` en `DESIGN.md`.
2. Extraer `frontmatterSectionMap` a `DesignFrontmatter.kt`, dejar `DesignTokensTest` verde sin tocar
   aserciones y escribir `MotionTokensTest` (tokens + caso negativo).
3. Crear `MotionPolicy` (+ `MotionDirection`) y `MotionPolicyTest` con vectores dorados y casos
   negativos.
4. Crear `MotionContent` (`AnimatedContent` + tokens + `MotionAxis` + `sizeTransform = null`).
5. Cablear los cuatro frentes en los 7 ficheros de UI, dejando los efectos secundarios y los `Saver`
   donde están.
6. Ejecutar `bash init.sh` en verde y comprobar que no cambian aserciones existentes.
7. Actualizar `DESIGN.md` (sección `## Motion` + excepción de capturas) y `docs/technical-discovery.md`.

## Implementation Tasks

- [ ] Crear `ui/theme/Motion.kt` con `SazonMotion` (240 ms, 24 dp, `standard`, `enterOnTop = true`).
- [ ] Añadir el bloque `motion:` al frontmatter de `DESIGN.md`.
- [ ] Extraer `frontmatterSectionMap` a `ui/theme/DesignFrontmatter.kt` y dejarlo compartido.
- [ ] Escribir `MotionTokensTest`: coincidencia exacta con `DESIGN.md` + copia mutada que **falla** +
      valores con sentido (duración > 0, desplazamiento > 0).
- [ ] Crear `ui/motion/MotionPolicy.kt` y escribir `MotionPolicyTest` (pestañas, páginas, pasos y
      offsets, con casos negativos: `FORWARD != BACKWARD`, `NONE` = 0).
- [ ] Crear `ui/motion/MotionContent.kt` (`MotionAxis`, `AnimatedContent`, `ContentTransform` con
      `targetContentZIndex` y `sizeTransform = null`).
- [ ] `MainShell.kt`: pestañas (`tabChange`), página empujada (`pageChange`) y los dos overlays
      (vertical) sin mover `LaunchedEffect`/`Saver`.
- [ ] `OnboardingFlow.kt`: envolver el cuerpo del paso por `progress.stepIndex` sin tocar
      `OnboardingScaffold`.
- [ ] `GeneratorFlow.kt`: paso (horizontal) y stage edición/carga/resultados (vertical) sin tocar el
      `LaunchedEffect`, los borradores ni el token de generación.
- [ ] `AiSearchFlow.kt`: stage por clave local (vertical) sin tocar la búsqueda ni el memo.
- [ ] `ResultsScreen.kt`: cuerpo por clave de estado (vertical); mismas acciones y mismo
      `ResultsStatus`.
- [ ] `FavoritesScreen.kt`: cuerpo vacío↔poblado (vertical) conservando buscador, orden y view mode.
- [ ] `HomeScreen.kt`: bloques "Sugeridas para ti" y "Tus recetas" por ids visibles (vertical).
- [ ] `bash init.sh` en verde, 0 warnings nuevos y sin cambios de aserciones existentes.
- [ ] Actualizar `DESIGN.md` (sección `## Motion` + excepción de capturas) y `docs/technical-discovery.md`.

## Verification Plan

- `bash init.sh` → **esperado**: código 0, sin procesos largos, `./gradlew test` + `./gradlew lint` en
  verde; **98 suites / 739 tests** preexistentes verdes sin cambios de aserciones + 2 suites / ~12
  tests nuevos (`MotionTokensTest`, `MotionPolicyTest`).
- Tirada focal: `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.motion.*" --tests "com.sazon.app.ui.theme.MotionTokensTest"`
  → **esperado**: `BUILD SUCCESSFUL` con 0 fallos. `MOTION_TOKENS` **caso negativo obligatorio**:
  `duration: 999ms` en la copia mutada hace fallar la comparación (el test no pasa en vacío).
- `./gradlew lint` (vía `init.sh`) → **esperado**: 0 errores y los **13 warnings preexistentes**, sin
  warnings nuevos bajo `app/src` (ojo con `UnusedMaterial3ScaffoldPaddingParameter`/API experimental: no
  se espera invocar API experimental).
- Inspección (obligatoria):
  - `SazonMotion` y `MotionPolicy` **sin** imports de Compose/Android y sin reloj/aleatoriedad.
  - `app/build.gradle.kts` y `gradle/libs.versions.toml` **intactos** (no se declara
    `androidx.compose.animation`); `init.sh` intacto.
  - `git diff --stat` limitado a los ficheros de `Expected File Changes`; `domain/**`, `data/**`,
    Taxonomía, `seed-recipes.json`, `assets/**` y los tokens visuales intactos.
  - El **comportamiento funcional** no cambia: mismas firmas de composables salvo la envoltura, mismos
    callbacks y destinos; `ensureBatch`/`LaunchedEffect` siguen fuera de `MotionContent`; crossfade de
    imagen (250 ms), spinner y pulso intactos.
  - Un solo lenguaje: no se introduce ninguna duración/desplazamiento literal fuera de `SazonMotion`
    (los 250 ms del crossfade de imagen son la excepción documentada y preexistente).
- Manual (persona, emulador, tras el merge; AGENTS.md no automatiza UI de emulador): en **claro y
  oscuro**, comprobar que cada transición se ve suave y coherente, **sin corte, sin parpadeo y sin
  salto de layout**: cambio de pestaña (ida y vuelta), abrir/cerrar Generador y Búsqueda con IA,
  entrar/salir del Detalle y del Modo cocina, avanzar/retroceder pasos del onboarding y del Generador,
  edición→carga→resultados y aparición de listas/tarjetas en Resultados, Favoritos y Home. Con "quitar
  animaciones" activado, la app no debe quedarse atascada (las transiciones se acortan a corte).
- Cobertura E2E: **no aplica**. No existe suite E2E persistente y AGENTS.md prohíbe automatizar pruebas
  de UI de emulador; el movimiento se cubre por tokens + selector en JVM y por la revisión manual.
- `init.sh`: **no cambia**; sigue ejecutando solo comprobaciones no bloqueantes, no arranca emulador ni
  servidor y puede seguir imprimiendo los comandos manuales.

## Evidence To Capture

- Salida de `bash init.sh` (código 0) y del total de tests: suites/tests, 0 fallos/errores/saltados, con
  el delta frente a la baseline **98 suites / 739 tests**.
- Salida de los tests nuevos (`MotionTokensTest`, `MotionPolicyTest`) y del **caso negativo** de tokens.
- Salida de `lint` (0 errores, 13 warnings preexistentes, 0 bajo `app/src`).
- Inspección: `SazonMotion`/`MotionPolicy` puros; `app/build.gradle.kts`, `gradle/libs.versions.toml`,
  `init.sh`, `domain/**`, `data/**`, Taxonomía, `seed-recipes.json` y `assets/**` intactos; ninguna
  aserción existente modificada; los cuatro frentes cableados con `MotionContent`.
- `DESIGN.md` (bloque `motion:` + sección `## Motion` + excepción de capturas) y
  `docs/technical-discovery.md` actualizados.
- Resultado manual de la persona (descripción; captura/vídeo opcional) de las transiciones en claro y
  oscuro, comparado con `DESIGN.md` → `Motion`.
- Anotación en `feature_list.json`/`PROGRESS.md` del estado y la evidencia (al implementar/validar).

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **Un solo lenguaje**: 240 ms, 24 dp, `FastOutSlowInEasing`, capa entrante encima. El brief pide
  "fundido con un deslizamiento sutil" y "una transición suave y coherente"; una única duración y una
  única distancia evitan el movimiento decorativo y son directamente comparables contra `DESIGN.md`.
- **Tokens puros en `ui.theme` (`SazonMotion`) y política pura en `ui.motion`**: replica el patrón
  aceptado de `design-tokens-and-theme` (tokens + test JVM contra `DESIGN.md`) y el de "lógica pura +
  Compose delgado" de `ui.cooking`/`ui.filters`, de modo que el acuerdo de diseño sea verificable sin
  emulador.
- **`AnimatedContent` como primitiva única** (fade + slide, `sizeTransform = null`): una sola
  implementación para contenido que cambia de identidad, **incluido `null ↔ valor`** (los overlays), que
  resuelve el problema clásico de la salida (la capa saliente sigue compuesta durante la animación).
  `sizeTransform = null` evita recorte y saltos de layout.
- **`androidx.compose.animation` no se declara**: se comprueba en los metadatos de módulo de la caché de
  Gradle que `material3 1.4.0 → foundation 1.10.4 → animation 1.10.4` es una dependencia **api**, así
  que está en el classpath de compilación (el brief lo exige explícitamente: sin dependencias nuevas).
- **Los efectos secundarios no se animan**: `ensureBatch`, la generación, la búsqueda y los `Saver`
  siguen fuera de `MotionContent`; envolver contenido en una transición **no** puede duplicar llamadas
  (durante la transición ambos contenidos están compuestos, pero solo el pintado).
- **Pestañas del Detalle, hojas del Perfil y `AppGate` fuera**: el brief enumera los cuatro frentes y
  Material 3 ya anima sus propias hojas; mantenerlos fuera acota el diff sobre pantallas aceptadas.
- **`DESIGN.md` gana la sección y el test la vigila**: el repositorio exige que `DESIGN.md` sea la
  fuente de verdad visual y hoy no tiene nada de movimiento; la excepción de capturas se registra como
  manda la política vigente (no hay captura estática posible).
- **`init.sh` sin cambios y sin dependencias nuevas**; el gate es el mismo `test` + `lint`.

### Unknowns

- **`rememberSaveable` bajo `AnimatedContent`**: durante la transición ambos destinos están compuestos y
  conviven `rememberSaveable` con `Saver` (Generador, Onboarding, Favoritos). No se espera choque de
  claves (los subárboles tienen claves de composición distintas), pero si apareciera, la mitigación es
  `contentKey = { it }` explícito y, en último término, un `rememberSaveableStateHolder`. **No** cambia
  el comportamiento ya aceptado de "empujar una página pierde el borrador de la pestaña".
- **Altura variable durante la transición**: en los bloques de la Home (hijos de un `LazyColumn`) y en
  el cuerpo de Favoritos, la capa saliente y la entrante pueden tener alturas distintas; con
  `sizeTransform = null` el contenedor toma el tamaño destino y el desbordamiento de 240 ms puede
  recortarse. Es aceptable y se revisa en el emulador; si molesta, se retira ese punto concreto sin
  salir de la feature.
- **"Quitar animaciones" del sistema**: no se añade código propio; Compose aplica la escala de duración
  del sistema a las animaciones de `Transition`. Se comprueba a mano en el emulador.
- **Rotación**: los overlays (`detailRecipe`/`cookingRecipe`) viven en `remember`, así que rotar sigue
  cerrándolos **sin** animación de salida (comportamiento actual, ya documentado en sus specs).
- **Sabor del movimiento** (240 ms / 24 dp en mano): lo juzga la persona tras el merge; afinar los
  valores es cambiar cuatro tokens y un `DESIGN.md`, sin tocar la política ni los puntos de aplicación.
- **`AnimatedContent` y estado en vuelo**: al abrir el Generador durante la transición quedan
  compuestos el Scaffold (pestañas) y el Generador; no hay efectos nuevos, pero conviene comprobar en el
  emulador que el FAB y la barra inferior no parpadean.

## Validator Checklist

- [ ] Los cuatro frentes están cableados con la **única** primitiva (`MotionContent`) en los puntos de
      aplicación listados, sin duraciones ni desplazamientos literales fuera de `SazonMotion`.
- [ ] Escenario 7: `MotionTokensTest` compara `duration`/`slide`/`easing`/`enterOnTop` con el frontmatter
      de `DESIGN.md` y **falla** con la copia mutada (no pasa en vacío).
- [ ] Escenario 8: `MotionPolicyTest` cubre `FORWARD`/`BACKWARD`/`NONE` para pestañas, páginas y pasos,
      más los offsets, con casos negativos.
- [ ] Escenario 9: `bash init.sh` en verde, **0 aserciones existentes modificadas**, lint sin warnings
      nuevos, y `app/build.gradle.kts` / `gradle/libs.versions.toml` / `init.sh` / `domain/**` /
      `data/**` / Taxonomía / `seed-recipes.json` / `assets/**` intactos (`androidx.compose.animation`
      **no** declarado).
- [ ] `SazonMotion` y `MotionPolicy` son puros (sin imports de Compose/Android) y deterministas.
- [ ] Los efectos secundarios (`ensureBatch`, generación, búsqueda) y los `Saver` siguen fuera de las
      transiciones; el crossfade de imagen (250 ms), el spinner y el pulso quedan intactos.
- [ ] `DESIGN.md` gana el bloque `motion:` y la sección `## Motion`, y registra la excepción de capturas
      citando `screen-transitions`; no se crea `docs/design/concepts/screen-transitions/`.
- [ ] `docs/technical-discovery.md` actualizado; `feature_list.json` y `PROGRESS.md` actualizados con
      evidencia por el implementer/validador (no por el planner).
- [ ] Manual (persona): en claro y oscuro no hay corte, parpadeo ni salto de layout en ninguno de los
      cuatro frentes.
