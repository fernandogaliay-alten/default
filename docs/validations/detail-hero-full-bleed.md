# Validación independiente — `detail-hero-full-bleed`

- Feature id: `detail-hero-full-bleed`
- Spec: `docs/specs/detail-hero-full-bleed.md`
- Rama/worktree: `feature/detail-hero-full-bleed`
  (`/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/detail-hero-full-bleed`)
- Rol: `validator` (contexto limpio; no implementa ni persiste `accepted`)
- Fecha: 2026-10-14
- Estado en `feature_list.json` al validar: `passing` (autoverificado por el implementer, sesión 117;
  sin commit/push/PR, trabajo en disco — esperado)
- Veredicto: **accept**

## Alcance validado

Feature de UI (`area: ui`, issue #132, depende de `recipe-detail-typographic-header` y
`dish-artwork-as-card-background`, ambas `accepted`). El **hero** (nombre + ingredientes principales) y la
**tira de metadatos** del Detalle dejan de estar embutidos por el `padding` lateral de 16 dp y pasan a
**de borde a borde**, sin tocar su geometría interna.

Cambio mínimo en `app/src/main/java/com/sazon/app/ui/detail/RecipeDetailScreen.kt` (único fichero de
código): se retira `.padding(horizontal = SazonSpacing.md)` de la cadena del `Box` de `DetailHero` (antigua
línea 215) y del `modifier` de la `Surface` de `MetaStrip` (antigua línea 399), más 3 hunks de KDoc
(encabezado, `DetailHero`, `MetaStrip`). Nada más cambia: `clip(HeroBottomShape)` (esquinas inferiores de
24 dp), `background(recipeAccentBrush)`, `height(240)`, filete de 6 dp, `HERO_PADDING = 18`, marca de agua
de 190 dp al 12 % con sus offsets, `HeroTypography`, `surface` + `shadowElevation = 4.dp` y la fila interna
(`vertical = sm`, `horizontal = 4.dp`) con los cuatro `RecipeDetailFormat.metaEntries`.

Exención de capturas autorizada (issue #132, registrada en `DESIGN.md`): **no** debe existir
`docs/design/concepts/detail-hero-full-bleed/`; la dirección visual vigente es `DESIGN.md` → `Shapes`
(hero y tira de borde a borde, revisión 2026-10-14).

## Verificación re-ejecutada (independiente)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (`:app:testDebugUnitTest` ejecutado, `lint` OK, vía `scripts/gradle.sh`; sin procesos largos) |
| Parseo propio XML | `app/build/test-results/testDebugUnitTest/TEST-*.xml` | **97 suites / 733 tests / 0 fallos / 0 errores / 0 saltados** |
| Suites `ui.detail` | — | `RecipeDetailHeroFormatTest` 10, `RecipeDetailFormatTest` 4, `ServingScalerTest` 8, `RecipeDetailStateTest` 5, `IngredientFormatTest` 5 (verdes, sin diff) |
| Baseline en `HEAD` `804f009` | `git grep -c '@Test'` | **97 ficheros / 733 anotaciones** → delta **0 suites / 0 tests** |
| Lint SARIF | `app/build/reports/lint-results-debug.sarif` | **13 resultados / 0 errores / 0 bajo `app/src`** (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1 = preexistentes) |
| Segunda observación | XML escritos por otro `init.sh` concurrente | Mismos **97 / 733 / 0 / 0 / 0** |

## Escenarios

- **E1** (hero a borde a borde): padding lateral retirado (`grep -c 'horizontal = SazonSpacing.md'` = 0);
  `padding(horizontal` solo en cuerpo/pestañas/`DurationPill`; geometría interna intacta
  (`RecipeDetailHeroFormat` y su test `la_geometria_del_hero_es_contrato` sin diff). ✔
- **E2** (tira alineada): `HeroBottomShape` definido una vez y usado por hero y tira → esquinas inferiores
  alineadas en x; `surface` + sombra intactas. ✔
- **E3** (ajuste de imágenes on/off): `RecipeArtwork` sigue con `matchParentSize()` **dentro** del `Box`
  recortado → imagen + `scrim` heredan el clip a ancho completo sin tocar `ui/artwork`; apagado = hero
  tipográfico. ✔
- **E4** (controles flotantes): "Atrás" y corazón conservan `SazonSpacing.md`, tamaño y comportamiento,
  ahora íntegramente sobre el hero, con ≥48 dp de objetivo táctil. ✔
- **E5** (alcance/pureza/capturas): por diff, greps y `git status`. ✔

## Disciplina de alcance y seguridad

- `git diff --name-only` = `RecipeDetailScreen.kt` (+ `feature_list.json`, `PROGRESS.md`; spec sin
  trackear): **único** fichero de código. Sin diff en `RecipeDetailHeroFormat`/`RecipeDetailFormat`/
  `RecipeDetailState`/`ServingScaler`/`IngredientFormat`, `ui/artwork/**`, `ui/theme/**`,
  `ui/MainShell.kt`, `domain/**`, `data/**`, `assets/**`, build/lockfile, `init.sh`, `scripts/**`,
  `DESIGN.md`, el ADR 0006 ni `docs/design/concepts/**`; 0 ficheros de test en el diff; sin dependencias
  nuevas.
- Greps rehechos: `horizontal = SazonSpacing.md` → **0**; `padding(horizontal` sin coincidencias en
  `DetailHero` ni `MetaStrip`.
- Capturas: `test ! -e docs/design/concepts/detail-hero-full-bleed` OK y `git status docs/design/concepts`
  limpio.
- Seguridad (`references/security-checklist.md`): sin hallazgos. Cambio exclusivamente de layout Compose;
  sin secretos, red, auth, entrada de usuario, datos, dependencias ni ficheros.

## Hallazgos

**Critical / High / Medium: ninguno.**

Low no bloqueantes (reparación del orquestador/persona, no del implementer):

- **L1** — La evidencia (spec, `feature_list.json`, `PROGRESS.md`) afirmaba que las capturas aceptadas ya
  mostraban el hero a sangre; en realidad son **previas** a la revisión (renderizadas en `7f1e113` antes de
  que `DESIGN.md` se actualizara en `45203e2`) y aún tienen **inset de 16 px** por lado
  (`code.html` envuelve `.herowrap` en `<div style="padding:0 16px">`). **Corregido** en la spec,
  `feature_list.json` y `PROGRESS.md`; manda `DESIGN.md`. Re-renderizar esas capturas es una acción de
  artefacto de diseño de la persona.
- **L2** — `verification[2]` pedía capturas de una carpeta que no debe existir. **Reconciliado** con la
  exención #132 al persistir `accepted`.
- **L3** — `hs_err_pid50454.log` sin trackear y fuera de `.gitignore` (SIGSEGV del daemon de Gradle).
  **Eliminado** antes del commit.
- **L4** — Informativos preexistentes/fuera de alcance: deriva previa de `docs/technical-discovery.md`
  sobre los consumidores de `RecipeArtwork` y rationale (b) imprecisa de la spec.

## Incidencia de entorno (no atribuible a la feature)

Concurrencia de builds de Gradle sobre el mismo `build/` del worktree (varios `init.sh` solapados) más un
SIGSEGV previo del daemon provocaron fallos de caché incremental en las tiradas `--rerun-tasks`
(`Expected compiler error, but got exitCode=OK`, `Could not read file: .../R.jar`). Un `init.sh` posterior
volvió a compilar, testear (97/733 verde) y lintar con éxito, así que el estado quedó sano; **no** es una
regresión de la feature (el diff no toca ningún camino cubierto por tests). Recomendado: no solapar builds
en el mismo worktree y limpiar la caché incremental si reaparece.

## Cierre

Veredicto **`accept`**; el orquestador de `feature-flow` persiste `accepted` en `feature_list.json`
(sesión 117), registra el cierre en `PROGRESS.md` y crea el commit local de la rama. La publicación
(push + PR, con `Closes #132`) queda para el orquestador de `github-flow`. Queda para la persona la
**revisión manual** en el emulador (no automatizable desde WSL): claro/oscuro × ajuste de imágenes
encendido/apagado, comprobando borde a borde, esquinas inferiores de 24 limpias y alineadas, controles
legibles y pulsables y el resto del Detalle sin moverse, **contra `DESIGN.md`** (no contra las capturas
antiguas, ver L1); un defecto visual se corrige con una feature correctiva.
