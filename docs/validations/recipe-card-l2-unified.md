# Validación independiente — `recipe-card-l2-unified`

- Feature id: `recipe-card-l2-unified`
- Spec: `docs/specs/recipe-card-l2-unified.md`
- Rama/worktree: `feature/recipe-card-l2-unified`
  (`/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/recipe-card-l2-unified`)
- Rol: `validator` (contexto limpio; no implementa ni persiste `accepted`)
- Fecha: 2026-10-14
- Estado en `feature_list.json` al validar: `passing` (autoverificado por el implementer, sesión 115;
  sin commit/push/PR, trabajo en disco — esperado)
- Veredicto: **accept**

## Alcance validado

Feature de UI (`area: ui`, issue #129, depende de `recipe-card-typographic` y
`dish-artwork-as-card-background`, ambas `accepted`). Unifica las **dos** estructuras de `RecipeFicha`
en una sola (variante **L2** del ADR 0006): hueco **16:9** + **banda sólida** siempre, con el ajuste
"Generar imágenes del plato" encendido o apagado; sin imagen solo desaparece la capa de fondo.

1. `ui/home/RecipeFicha.kt`: se eliminan `TypographicHole` (16:10, con los ingredientes dentro del
   hueco) e `ImageBackgroundHole`; el hueco lo pinta un único `Hole` (16:9) con el contenido
   factorizado en `HoleHeading` (marca de agua por `watermarkVisible(layer)`, categoría y título por
   `headingOnImage(layer)`) y el filete de 6 px. Con `imageBackground == false` **no** se compone
   `RecipeArtwork` (capa sintética `TYPOGRAPHIC` → 0 lecturas de caché/semilla/generador); con `true`
   `RecipeArtwork` aporta la capa de fondo. `SolidBand` siempre debajo y reparte los ingredientes por
   `bandLayout`.
2. `ui/artwork/DishImageArtworkOverlay.kt` (puro): `enum FichaBandLayout { ONE_LINE, THREE_ROWS }` +
   `bandLayout(wide, compact)`.
3. `ui/theme/Spacing.kt`: retirada de la constante muerta `SazonComponents.recipeCardImageRatio`
   (16:10).
4. KDoc-only en `RecipeCard`, `ResultRecipeCard` y `FavoriteRecipeTile` (16:10 → 16:9).

Exención de capturas autorizada (issue #129, registrada en `DESIGN.md` líneas 241-259): **no** debe
existir `docs/design/concepts/recipe-card-l2-unified/`; la referencia son las capturas ya aceptadas de
`recipe-card-typographic` y `dish-artwork-as-card-background` (intactas).

## Verificación re-ejecutada (independiente)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (test + lint vía `scripts/gradle.sh`); salió `UP-TO-DATE`, se forzó rerun |
| Test JVM forzado | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | BUILD SUCCESSFUL, 5m44s, 34/34 tareas |
| Lint forzado | `bash scripts/gradle.sh lint --rerun-tasks` | BUILD SUCCESSFUL, 5m22s, 30/30 tareas |
| Parseo propio XML | `app/build/test-results/testDebugUnitTest/TEST-*.xml` | **97 suites / 733 tests / 0 fallos / 0 errores / 0 saltados** |
| Baseline en `HEAD` | `git grep -c '@Test'` | **97 ficheros / 731 anotaciones** → delta **+2 tests / 0 suites** (`DishImageArtworkOverlayTest` 5 → 7) |
| Suites vecinas | — | `RecipeBackgroundGateTest` 6, `RecipeCardFormatTest` 13, `RecipeMonogramTest` 7, `RecipeArtworkImagePrecedenceTest` 5 (verdes, sin cambios) |
| Lint SARIF | `app/build/reports/lint-results-debug.sarif` | **13 resultados / 0 errores / 0 bajo `app/src`** (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1 = preexistentes) |
| Verificador propio fuera del repo | `/tmp` (reflexión sobre el bytecode real de `app/build`) | **13/13 checks, 0 fallos** |

El verificador externo comprobó `HOLE_RATIO == 16f/9f` y `!= 16f/10f`, los cuatro vectores de
`bandLayout` (incl. `wide && compact` → `ONE_LINE`), el determinismo de `bandLayout`, la exclusión
mutua `headingOnImage ⇔ !watermarkVisible` en todas las capas y el alfa `1f/0f` coherente.

## Escenarios

- **E1** (apagado = L2 sin capa de fondo): un único `Hole` 16:9 + `HoleHeading` + `SolidBand`; con
  `imageBackground == false` **no** se compone `RecipeArtwork` → 0 lecturas de caché/semilla/generador. ✔
- **E2** (encendido + imagen): única diferencia = capa de fondo (`matchParentSize`, sin salto de
  layout) + tratamiento sobre imagen + marca de agua oculta; crossfade desde
  `DishImageArtworkPolicy.CROSSFADE_MILLIS`. ✔
- **E3** (encendido sin imagen ≡ apagado): capa `TYPOGRAPHIC` + alfa `0f`; gate cubierto por
  `RecipeBackgroundGateTest` intacto (null, códec roto, lento, cancelación propagada, tope agotado). ✔
- **E4** (ancha): 26 sp, banda `THREE_ROWS` con cantidad tabular + `FichaFoot`, descripción después de
  la banda. ✔
- **E5** (compacta): 16:9 + banda `ONE_LINE`, sin metadatos, corazón superpuesto intacto. ✔
- **E6** (regla pura): vectores + determinismo verificados sobre el bytecode. ✔
- **E7** (retirada del 16:10): sin rama ni constante; KDoc y aserción del test reconciliados. ✔
- **E8** (alcance/pureza/capturas): verificado por diff, greps y `git status`. ✔

El prototipo aceptado `docs/design/concepts/typographic-cards/prototipo-c3-imagenes.html` (`.filete`
dentro del `.art` 16:9, `.ingband` debajo con `.ing3`/`.foot`) confirma la fidelidad al L2 aceptado y
`DESIGN.md` (§Overview, §Components, OQ6 resuelta, frontmatter `ratio: "16:9 … L2 unificada"`) coincide
con lo implementado.

## Disciplina de alcance y seguridad

- `git diff --name-only origin/main` = los 9 ficheros esperados + `feature_list.json` + `PROGRESS.md`
  (+ la spec sin trackear); **sin** diff en `data/**`, `domain/**`, `assets/**`, `app/build.gradle.kts`,
  `gradle/**`, `init.sh`, `scripts/**`, `DESIGN.md` ni `docs/design/concepts/**`.
- Greps rehechos por el validador: `recipeCardImageRatio|16f / 10f` y `TypographicHole|ImageBackgroundHole`
  en `app/src` → **0**; 0 imports `android`/`androidx` en `DishImageArtworkOverlay.kt`; 0 hex en
  `ui/{home,artwork,results,favorites}`; `RecipeArtwork(` → definición + **2** consumidores.
- Capturas: `test ! -e docs/design/concepts/recipe-card-l2-unified` OK y
  `git status docs/design/concepts` limpio.
- Seguridad: sin secretos, sin red, sin auth, sin entrada de usuario, sin SQL/HTML, sin logs ni
  dependencias nuevas. El contrato de gasto se **preserva**: con el ajuste apagado el camino unificado
  no compone `RecipeArtwork` (0 lecturas de caché/semilla/generador y 0 cuota; el gate del interruptor y
  el tope por sesión, en `data/**`, sin diff).

## Hallazgos

**Critical / High / Medium: ninguno.**

Low/Info informativos no bloqueantes:

- **L1** — `docs/specs/recipe-card-l2-unified.md` §Incógnitas dice que el hueco 16:9 es "más alto" que
  el 16:10; a igual ancho es **más bajo** (las tarjetas crecen por la banda). El KDoc del implementer
  tiene los números correctos; no afecta al comportamiento.
- **L2** — el test del repo no aserta el determinismo de `bandLayout` (el validador lo comprobó por
  bytecode).
- **L3–L5** — frases heredadas de `HEAD` en `docs/adr/0006` (una entrada "pendiente de validación
  independiente" ya resuelta) y en `docs/technical-discovery.md`; y el framing "seis features" del ADR.
- **L6** — `verification[2]` de `feature_list.json` citaba la carpeta de capturas; reconciliada por el
  orquestador al persistir `accepted`.

## Cierre

Veredicto **`accept`**; el orquestador de `feature-flow` persistió `accepted` en `feature_list.json`
(sesión 116), registró el cierre en `PROGRESS.md` y crea el commit local de la rama. La publicación
(push + PR) queda para el orquestador de `github-flow`. Queda para la persona la **revisión manual** en
el emulador (no automatizable desde WSL): Inicio, Resultados y rejilla de Favoritos en claro y oscuro,
con el ajuste apagado y encendido; un defecto visual se corrige con una feature correctiva.
