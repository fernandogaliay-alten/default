# Validación independiente — `dish-artwork-as-card-background`

- Feature id: `dish-artwork-as-card-background` (`area: integration`)
- Spec: `docs/specs/dish-artwork-as-card-background.md`
- Rama/worktree: `feature/dish-artwork-as-card-background`
  (`/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/dish-artwork-as-card-background`)
- Rol: `validator` (contexto limpio; no implementa, no commitea y **no** persiste `accepted`)
- Fecha: 2026-10-14
- Estado en `feature_list.json` al validar: `passing` (autoverificado por el implementer, sesión 109;
  trabajo **sin commitear**: `HEAD == origin/main`, 0 commits por delante, sin upstream; sin push ni PR —
  esperado en `feature-flow`)
- Veredicto: **accept**

## Alcance validado

Cuarta de las cinco features del ADR 0006 (variante **L2**), depende de `recipe-card-typographic` y
`recipe-detail-typographic-header` (ambas `accepted`). Convierte la imagen generada en **capa de fondo
opcional** del hueco de la tarjeta (16:9) y del hero (240), con la ficha tipográfica como capa base, el
`scrim` de contraste, la banda sólida en la tarjeta y el fundido ya existente.

1. **Lógica pura nueva/ajustada** (`ui/artwork`, sin Android ni Compose):
   `DishImageArtworkOverlay` (`HOLE_RATIO = 16f / 9f`, `headingOnImage`, `watermarkVisible`,
   `crossfadeAlpha` derivado de la precedencia) y `RecipeArtworkImagePrecedence` reducida a **dos**
   capas (`RecipeArtworkImageLayer { TYPOGRAPHIC, GENERATED }`; `layerOf`, `crossfadeTarget`,
   `originOf(generated, resolvedOrigin)` sin `LOCAL`).
2. **`RecipeArtwork` = proveedor de la capa de fondo**: firma `(recipe, modifier, glyphSize)` con
   defaults conservados (y `glyphSize` documentado como **reservado**), **slot de contenido aditivo**
   `content: @Composable (RecipeArtworkImageLayer) -> Unit = {}`, imagen `ContentScale.Crop` con
   `alpha = DishImageArtworkOverlay.crossfadeAlpha(layer)` + `tween(DishImageArtworkPolicy.CROSSFADE_MILLIS)`
   y el `scrim` con el **mismo** alfa encima; **sin** `LocalRecipePhotoSource`, **sin** degradado+glifo
   de respaldo y **sin** geometría propia (`matchParentSize`, la aporta el llamante).
3. **Tokens** nuevos en `ui/theme/CardImageOverlay.kt` (único sitio con hex): `scrimBrush(dark)` con las
   paradas del prototipo, `onImageText` `#FFFFFF`, `onImageVariant` blanco al 86 % (`0xDB` = 219/255)
   y `onImageHairline` blanco al 32 % (`0x52` = 82/255).
4. **Estructura L2** (`RecipeFicha.imageBackground: Boolean = false`): hueco 16:9 con base tipográfica +
   `RecipeArtwork` + contenido del slot (marca de agua solo sin imagen, categoría/título con el
   tratamiento de la capa) y **banda sólida** (tres filas con cantidad + metadatos; la celda `compact`
   conserva una línea y **sin** metadatos). Con `false`, `TypographicHole` (16:10) reproduce la ficha
   aceptada.
5. **Hero** (`RecipeDetailScreen`): `RecipeArtwork` como capa de fondo sobre la base
   `recipeAccentBrush` + marca de agua y tipografía con tratamiento sobre imagen; `HERO_HEIGHT_DP` = 240,
   `MetaStrip`, pestañas, escalador y barra inferior sin cambios.
6. **Reparto del flag**: `MainShell` (`dishImageSettings.state.enabled`, ya observado) → `HomeScreen`,
   `ResultsScreen`, `FavoritesScreen` y el Detalle; `RecipeCard`/`ResultRecipeCard`/`FavoriteRecipeTile` y
   `GeneratorFlow`/`AiSearchFlow` (hallazgo de alcance del implementer, aditivo con default `false`)
   reenvían.

## Verificación re-ejecutada (independiente, mía)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0**, pero `:app:testDebugUnitTest` y `:app:lintAnalyze*` salieron **UP-TO-DATE** ⇒ se forzaron las dos tiradas (abajo); **no** se aceptó como evidencia propia |
| Tests (forzado) | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL** en 3m 34s, **34/34 tareas `executed`**, exit 0 (log `/tmp/sazon-gradle-CEzfI7.log`) |
| Parseo XML propio (fresco, 13:41) | `app/build/test-results/testDebugUnitTest/TEST-*.xml` | **98 suites / 742 tests / 0 fallos / 0 errores / 0 saltados** |
| Suites de la feature (del XML fresco) | — | `DishImageArtworkOverlayTest` **5/5**, `RecipeBackgroundGateTest` **6/6**, `RecipeArtworkImagePrecedenceTest` **5/5** (antes 4) |
| Sin regresión en vecinos | — | `RecipeCardFormatTest` 13, `RecipeMonogramTest` 7, `RecipeDetailHeroFormatTest` 10, `RecipeDetailFormatTest` 4, `DishImageArtworkLoaderTest` 6, `RecipeArtworkStyleTest` 8, `AndroidRecipeImageDecoderTest` 2 — todos verdes |
| Baseline en `HEAD` | `git grep -c '@Test' HEAD -- 'app/src/test/**'` | **96 ficheros / 730 `@Test`** ⇒ en el árbol **+2 suites / +12 tests** (11 de los 2 ficheros nuevos —`git grep` no ve los no versionados— **+1** de la precedencia: 4 → 5). Coincide con lo declarado |
| Lint (forzado) | `bash scripts/gradle.sh lint --rerun-tasks` | **BUILD SUCCESSFUL** en 3m 12s, **30/30 tareas `executed`**, exit 0 (log `/tmp/sazon-gradle-nGPvo8.log`) |
| SARIF fresco (13:44) | `app/build/reports/lint-results-debug.sarif` | **13 resultados, 0 errores**: `GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1 (entorno); **0 bajo `app/src`** ⇒ sin warnings nuevos (idéntico a la baseline aceptada) |
| Arbitraje de estado | `git rev-parse HEAD` / `origin/main` / `rev-list --count` / `HEAD@{u}` | `HEAD == origin/main == 4ba6a7c`, **0 commits** por delante, **sin upstream** ⇒ el implementer **no** commiteó, **no** empujó y **no** abrió PR |
| Alcance | `git status --porcelain` + `git diff --name-only origin/main` | **21** ficheros: los 14 previstos de `app/src/main`, los 3 de test, las 5 docs, la spec (no versionada, del planner) y `feature_list.json`/`PROGRESS.md` (harness); **0** cambios en `app/build.gradle.kts`, `gradle/**`, `init.sh`, `scripts/**`, `app/src/main/assets/**`, `data/**`, `domain/**`, `app/src/androidTest/**`, `docs/design/concepts/**` ni `docs/domain/taxonomy.json` |
| Capturas obligatorias | `git status --porcelain docs/design/concepts` + `ls -la .../{tarjeta-con-imagen,detalle-con-imagen}/` | **sin cambios**; los **8** ficheros existen (`screen.png`, `screen-dark.png`, `code.html`, `DESIGN.md` ×2) y son de solo lectura |
| Pureza de la lógica nueva | `grep -n "import android\|import androidx" DishImageArtworkOverlay.kt` | **0** |
| Hex sueltos | `grep -rn "0xFF\|0x[0-9A-Fa-f]\{8\}"` en `ui/artwork`, `ui/home`, `ui/detail`, `ui/results`, `ui/favorites` | **0** (los valores viven en `ui/theme/CardImageOverlay.kt`) |
| Consumidores de `RecipeArtwork` | `grep -rn "RecipeArtwork(" app/src` | definición + **2** llamadas (`RecipeFicha.kt:262` hueco y `RecipeDetailScreen.kt:280` hero), como exige el escenario 7 |
| Cadena local | `grep -rn "LocalRecipePhotoSource" app/src` | solo `MainActivity.kt` (provider conservado, declarado) y su definición (`RecipePhotoSource.kt`) |
| Gate/tope intactos | `git diff --name-only origin/main -- …/data/image` | **0** ficheros ⇒ `DishImageSettings`, `DishImageGenerationBudget` y `GatedDishImageGenerator` sin tocar; `MainActivity.kt:218` sigue cableando `generatedIfEnabled { loader.imageFor(recipe) }` |
| Hunks acotados | `git diff -U0` de `RecipeDetailScreen.kt` y `FavoriteRecipeCard.kt` | solo imports/constantes/hero+`HeroForeground`/`HeroIngredients`/`Hairline` y la firma+reenvío de `FavoriteRecipeTile`; `RecipeMonogramTile`, `MetaStrip`, `DetailTabs`, `StepsTab`, `NotesTab`, `DetailBottomBar` y el `Saver` **sin tocar** |

## Escenarios de la spec contrastados

| Escenario | Comprobación independiente | Resultado |
|---|---|---|
| 1 — apagado: sin fondo y sin tocar caché/semilla/generador | `MainActivity` gate §(código leído) + `RecipeBackgroundGateTest.apagado_…` (0 invocaciones del resolutor, 0 decodificados, `layerOf(null) == TYPOGRAPHIC`, `crossfadeTarget == 0f`); doble protección: `MainShell` pasa `enabled` como `imageBackground`, así que con el ajuste apagado `RecipeArtwork` **ni se compone** | **Cumple**. El resolutor es la **única** entrada a memoria→disco→semilla→generador, así que "0 llamadas al resolutor" implica 0 en los tres niveles |
| 2 — encendido con imagen: fondo + `scrim` + fundido | Test (`GENERATED`, `crossfadeTarget == 1f`, `headingOnImage`, `watermarkVisible == false`) + código (`ContentScale.Crop`, `scrimBrush`, `tween(CROSSFADE_MILLIS)`) + capturas (`aspect-ratio:16/9`, `.scrim{linear-gradient(...)}`, `.ingband{padding:11px 13px 13px}`) | **Cumple**. Paradas del `scrim` idénticas al prototipo (claro `.42/.18@52%/.78`, oscuro `.5/.22/.82`) y banda a 11/13/13 dp |
| 3 — encendido sin imagen o códec roto | Test (resolutor `null` → `TYPOGRAPHIC` con marca de agua; `decode = { null }` → `null`) + `RecipeFicha.kt:268` (marca de agua solo si `watermarkVisible`) | **Cumple**: sin microcopy, sin error y sin hueco; nada se cachea (el loader solo cachea en éxito) |
| 4 — timeout / cancelación | Test 4b: `CancellationException` **se propaga** (relanzada por el loader y por `generateOrNull`/`seedOrNull`); test 4a: resolución que cae a `null` → `TYPOGRAPHIC` | **Cumple**, con matiz: el timeout **real** vive en `data.ai.image` (`DishImagePolicy.TIMEOUTS`) y no es alcanzable desde `ui.artwork`; el test cubre la degradación `null → TYPOGRAPHIC` que es el contrato de esta capa (ver Low-1). Ni un timeout ni una cancelación pueden tumbar la app: `CancellationException` cancela la corrutina de `produceState` y la capa queda en `TYPOGRAPHIC` |
| 5 — tope por sesión agotado | `RecipeBackgroundGateTest` (tope agotado en `GatedDishImageGenerator` → `null` con **0** llamadas al delegado) sobre el código aceptado y **sin diff** | **Cumple** (regresión del gate del tope, que esta feature no toca) |
| 6 — geometría invariante y fundido en un único sitio | `HOLE_RATIO == 16f/9f` (test + `aspectRatio(DishImageArtworkOverlay.HOLE_RATIO)`), `HERO_HEIGHT_DP` 240 (`RecipeDetailHeroFormatTest` 10/10 verde), alfa `0f/1f` y **única** constante de fundido `DishImageArtworkPolicy.CROSSFADE_MILLIS` (`grep`: `250` aparece solo en la política) | **Cumple**. Al aparecer la imagen solo cambia el alfa de la capa (mismo hueco, misma banda, misma tira): sin salto de layout |
| 7 — contrato de `RecipeArtwork` y consumidores | Firma `(recipe, modifier, glyphSize)` con defaults (inspección directa) + `content` aditivo con default + **2** consumidores + 0 geometría propia + 0 `LocalRecipePhotoSource` + 0 degradado/glifo de respaldo + sin dependencias nuevas | **Cumple**. La frase heredada de "los cinco puntos de uso" se interpreta como **compatibilidad de contrato** (§1 de la spec, decisión cerrada): los cinco llamantes históricos seguirían compilando, porque los tres parámetros conservan nombre y default |
| 8 — alcance, pureza y capturas | Los checks de la tabla anterior | **Cumple** |

## Hallazgos

Ningún **Critical**, **High** ni **Medium**. Nada bloquea la aceptación.

### Finding (Low-1): el caso "tarda" del test no ejercita un timeout real

- Severity: Low (no bloqueante)
- Evidence: `RecipeBackgroundGateTest.kt:99-112` — `una_resolucion_lenta_que_cae_a_null_es_tipografica`
  usa un resolutor falso con `delay(1_000)` que después devuelve `null`; bajo `runTest` el `delay` es
  **tiempo virtual** (se salta) y no hay ninguna aserción sobre la política de timeout. El único
  aserto útil es `null → TYPOGRAPHIC` con `resolver.calls == 1`.
- Why it matters: el nombre y el comentario ("el timeout de la politica se degrada aguas arriba a
  null") pueden leerse como si el timeout estuviera cubierto por test. El timeout real está en la capa
  de datos aceptada (`DevExpertDishImageGenerator` + `DishImagePolicy.TIMEOUTS`) y **no** es alcanzable
  desde `ui.artwork`; lo verificado aquí es la degradación de la capa, que es el contrato correcto.
- Required change: ninguna funcional; ajustar nombre/comentario para que no prometa más de lo que
  verifica, o añadir una aserción que documente que el límite superior es `DishImagePolicy.TIMEOUTS`
  (máx. 40 s, ya aceptado).
- Suggested implementation:
  1. Renombrar a `una_resolucion_que_cae_a_null_es_tipografica` (o añadir "(timeout aguas arriba)").
  2. Si se quiere trazabilidad, `assertTrue(DishImagePolicy.TIMEOUTS.…)`-style o un comentario con la
     constante de la capa de datos; **no** duplicar la constante.
- Verification after fix: `bash scripts/gradle.sh testDebugUnitTest --tests "*RecipeBackgroundGateTest"`.
- Sugerencia de encaje: no merece una feature correctiva por sí solo; puede resolverse en
  `retire-recipe-photos` (que ya toca `ui/artwork`/`ui/artwork` tests y el KDoc obsoleto, Low-3).

### Finding (Low-2): `CardImageOverlay.scrimStops` es API pública sin uso

- Severity: Low
- Evidence: `app/src/main/java/com/sazon/app/ui/theme/CardImageOverlay.kt:43-47`; `grep -rn "scrimStops"`
  → **0** usos (ni en `app/src/main` ni en `app/src/test`). Su KDoc dice "útil para inspección y tests".
- Why it matters: superficie pública muerta en un fichero de tokens que se declaró "el único sitio con
  los valores"; en el futuro puede divergir de `scrimBrush`.
- Required change: usarla desde un test (p. ej. afirmar las tres paradas claro/oscuro) o retirarla.
- Suggested implementation: en `DishImageArtworkOverlayTest` (o un `CardImageOverlayTest` en
  `ui/theme`), afirmar `scrimStops(false).size == 3` y las alfas `.42/.18/.78`.
- Verification after fix: `bash scripts/gradle.sh testDebugUnitTest --tests "*Artwork*"`.
- No bloquea: es pulido de mantenibilidad.

### Finding (Low-3): KDoc obsoleto en `DishImageArtworkLoader` (cadena local)

- Severity: Low
- Evidence: `app/src/main/java/com/sazon/app/ui/artwork/DishImageArtworkLoader.kt:27` ("la UI conserva
  la foto local"), `:29` ("degrada a la cadena local") y `:55` ("degrada a la cadena local"). El fichero
  **no** está en el diff (fuera del alcance declarado de la spec) y su comportamiento real no cambia.
- Why it matters: contradice el comportamiento implementado (la tarjeta y el hero ya no leen la cadena
  local); un agente futuro puede creer que sigue habiendo respaldo por foto.
- Required change: reconciliar el KDoc al retirar la cadena (`retire-recipe-photos`), que es quien borra
  `LocalRecipePhotoSource`/`RecipePhotoAssets`.
- Verification after fix: `grep -rn "foto local\|cadena local" app/src/main/java/com/sazon/app/ui/artwork`
  sin referencias a un respaldo inexistente.
- No bloquea: es documentación de código y la feature declara explícitamente que no toca ese fichero.

### Informational (sin acción en esta feature)

- **Info-1 — variantes no capturadas**: el hueco 16:9 + banda en la tarjeta **estrecha** (carrusel de
  Inicio) y en la celda **compacta** de Favoritos es una **extrapolación** de la captura ancha; OQ6 sigue
  abierta. Declarado en la spec, `PROGRESS.md` y `docs/risks-and-open-questions.md`. **Revisión visual
  de la persona** tras el merge; un defecto se corrige con una feature correctiva.
- **Info-2 — transitorio de 250 ms**: la visibilidad de la marca de agua y el tratamiento tipográfico
  cambian **por capa** (instantáneo) mientras el alfa del fondo se anima; durante el fundido el texto
  blanco se apoya en un `scrim` a medio entrar. Es exactamente lo que manda la spec (solo cambia el alfa
  de la capa de fondo y el tratamiento sigue a la capa) y no es medible en JVM; se anota para la revisión
  visual.
- **Info-3 — restos declarados hasta `retire-recipe-photos`**: el provider `LocalRecipePhotoSource` en
  `MainActivity` queda **sin lectores** y `data.image.DishImageOrigin.CATEGORY` **sin productor** (no se
  toca `data/**`: decisión 5 y §5 de la spec).
- **Info-4 — `RecipeArtworkDefaults.GlyphSize` (48.dp)** queda **reservado** por compatibilidad de
  contrato, con `@Suppress("UNUSED_PARAMETER")` y KDoc explícito (decisión 6 de la spec): no es código
  muerto accidental.
- **Info-5 — proxy de "0 lecturas"**: el test del apagado mide "0 llamadas al resolutor" (única entrada
  a memoria/disco/semilla/generador) y `loader.decodedCount == 0`, en vez de contar cada nivel por
  separado. El proxy es válido y suficiente.
- **Positivo**: el `init.sh` de este worktree devolvió el gate **UP-TO-DATE**; no se aceptó como evidencia
  propia: las tiradas forzadas (`--rerun-tasks`, 34/34 y 30/30 tareas `executed`), el parseo del XML y el
  del SARIF reproducen **exactamente** los conteos declarados (98/742/0/0/0 y 13 warnings de entorno con 0
  errores y 0 bajo `app/src`).

## Seguridad

**Sin hallazgos.** Revisión acotada al diff con `references/security-checklist.md`: no hay secretos,
claves, URLs ni credenciales nuevas (el diff es Compose + lógica pura JVM; el único `Log.d` de origen es
preexistente y no incluye bytes), no hay autenticación/autorización, ni entrada de usuario, ni consultas,
ni rutas de fichero, ni datos personales nuevos. La única operación con efectos externos —resolver/generar
la imagen— sigue pasando por la cadena aceptada (`data.image` → `data.ai.image`) **detrás del mismo
interruptor y del mismo tope por sesión**, cuya lógica y constantes quedan **sin diff**; el apagado sigue
cortando antes de cualquier lectura de caché, red o cuota (escenario 1 verificado). Sin dependencias
nuevas (`app/build.gradle.kts` y `gradle/**` intactos), así que no aplica `Dependency And Tooling Risk`.
Las imágenes son **decorativas** (`contentDescription = null`, la receta ya se anuncia) y no salen del
dispositivo.

## Documentación / harness

- `DESIGN.md`: tokens del `scrim` y del texto sobre imagen en el frontmatter (`recipe-card-image`), ratio
  L2 16:9, sección *Imagen de receta* reconciliada (capa base tipográfica, dos consumidores, cadena local
  sin lectores) y registro de las capturas `tarjeta-con-imagen`/`detalle-con-imagen`. Coherente con
  `docs/design/concepts/typographic-cards/` y con el ADR 0006.
- `docs/technical-discovery.md`: `RecipeArtwork` = proveedor de la capa de fondo, dos capas, tokens y
  decisión pura nuevas, degradación por `null`/timeout/tope y los tests JVM nuevos.
- `docs/adr/0006`: estado con la **cuarta** feature implementada (queda `retire-recipe-photos`), §7 y
  consecuencias reconciliados con los **dos** consumidores.
- `docs/domain/taxonomy.md` §6: glifo/acento vigentes, degradado base del hero y `CATEGORY` sin productor.
- `docs/risks-and-open-questions.md`: cadena local sin lectores y acabado de las variantes no capturadas.
- `docs/domain-model.md` y `AGENTS.md`: **sin cambios**, como declaraba la spec (sin entidades/estados
  nuevos y sin reglas de flujo nuevas). Sin documentos extra creados.
- `init.sh`/`scripts/**`: intactos; el worktree sigue reiniciable por el camino estándar (`bash init.sh`
  → exit 0). `feature_list.json`: `passing` con evidencia y **`verification` intacto** (`git diff` solo
  cambia `status` y `evidence`).
- E2E/UI automatizada: **no aplica** (no hay suite E2E persistente; `AGENTS.md` no automatiza UI de
  emulador). Queda para la persona la revisión visual en claro y oscuro de Inicio, Resultados, Favoritos
  (rejilla y lista) y el Detalle, con el ajuste apagado, encendido con imagen y encendido sin imagen
  (modo avión o tope agotado), prestando atención a Info-1 e Info-2.

## Cierre

El **orquestador** de `feature-flow` persiste `accepted` en `feature_list.json` con la evidencia del
validador, registra la sesión en `PROGRESS.md` y crea el **commit local** en
`feature/dish-artwork-as-card-background`. **Sin** push ni PR (los hace `github-flow`). Este validador
**no** ha commiteado, empujado ni abierto PR, y **no** ha tocado código de producto,
`feature_list.json` ni `PROGRESS.md`.
