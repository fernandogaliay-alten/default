# Validación independiente — `detail-hero-square-bottom`

- Feature id: `detail-hero-square-bottom`
- Spec: `docs/specs/detail-hero-square-bottom.md`
- Rama/worktree: `feature/detail-hero-square-bottom`
  (`/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/detail-hero-square-bottom`)
- Rol: `validator` (contexto limpio; no implementa ni persiste `accepted`)
- Fecha: 2026-10-14
- Estado en `feature_list.json` al validar: `passing` (autoverificado por el implementer, sesión 124;
  sin commit/push/PR, trabajo en disco — esperado)
- Baseline: `HEAD` `7661f6e` (`git grep -c '@Test' -- 'app/src/test/**'` → **98 ficheros / 737
  anotaciones**)
- Veredicto: **accept**

## Alcance validado

Feature correctiva de UI (`area: ui`, depende de `detail-hero-full-bleed`, `accepted`). El **hero** del
Detalle deja de redondear sus esquinas inferiores: pasa de `.clip(HeroBottomShape)` a
`.clip(RectangleShape)` (recorte **rectangular**, conservando el recorte) para encajar a ras con la tira
de metadatos, que **conserva** sus 24 dp inferiores y su sombra. La constante superviviente se renombra
`HeroBottomShape` → `MetaStripBottomShape` (mismo valor, usada **solo** por la tira).

Cambio mínimo en `app/src/main/java/com/sazon/app/ui/detail/RecipeDetailScreen.kt` (**único** fichero de
código): un import nuevo (`RectangleShape`), el recorte del `DetailHero` (línea 229), el rename de la
constante (línea 85, uso en `MetaStrip` línea 413) y 3 hunks de KDoc (encabezado, `DetailHero`,
`MetaStrip`). `git diff --stat` del código: 1 fichero, 33 inserciones / 19 borrados. `DESIGN.md` se
reconcilia en **solo** dos puntos (`Shapes` y `Core Screens` 5). Nada más cambia.

## Verificación re-ejecutada (independiente)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (`:app:testDebugUnitTest` + `lint` vía `scripts/gradle.sh`; sin procesos largos). Ojo: la tirada salió mayormente `UP-TO-DATE` (config cache reused), por lo que **no** se consideró evidencia suficiente |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (previo `rm -rf app/build/test-results/testDebugUnitTest`) | **BUILD SUCCESSFUL**, 34/34 tareas ejecutadas |
| Parseo propio XML | `app/build/test-results/testDebugUnitTest/TEST-*.xml` | **98 suites / 737 tests / 0 fallos / 0 errores / 0 saltados** |
| Suites `ui.detail` | — | `RecipeDetailHeroFormatTest` 10 (incl. `la_geometria_del_hero_es_contrato`), `RecipeDetailFormatTest` 4, `ServingScalerTest` 8, `RecipeDetailStateTest` 5, `IngredientFormatTest` 5 = **5 suites / 32 tests verdes** |
| Baseline | `git grep -c '@Test' HEAD -- 'app/src/test/**/*.kt'` vs working tree | **98 / 737** en ambos → delta **0 suites / 0 tests**; `git diff --name-only HEAD -- app/src/test` vacío |
| Lint forzado | `bash scripts/gradle.sh lint --rerun-tasks` | **BUILD SUCCESSFUL**, 30/30 tareas ejecutadas |
| Lint SARIF | `app/build/reports/lint-results-debug.sarif` | **13 resultados / 0 errores / 0 bajo `app/src`** (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1 = preexistentes) |

## Escenarios (contra el diff y los greps)

- **E1** (cabecera recta y a ras con la tira): el único `clip(` del fichero es el del `DetailHero`
  (línea 229) y es `.clip(RectangleShape)`; `HeroBottomShape` → **0** apariciones; `height(240)`,
  `fillMaxWidth()` y el orden `fillMaxWidth → recorte → background → height` intactos; el borde superior
  de la tira era y sigue recto. ✔
- **E2** (el hero sigue recortando marca de agua y filete): el recorte se **conserva** (solo cambia de
  forma); `WATERMARK_OFFSET_X/Y = 24/6`, marca de agua 190 dp al 12 % y filete de 6 px siguen siendo
  hijos del `Box` recortado. Quitar el `clip` (defecto) no ocurrió. ✔
- **E3** (la tira no cambia): `RoundedCornerShape`/`bottomStart`/`bottomEnd` aparecen **solo** en la
  definición de `MetaStripBottomShape` (24 dp, línea 85); `shape = MetaStripBottomShape`, `color =
  scheme.surface`, `shadowElevation = 4.dp`, fila interna (`vertical = SazonSpacing.sm`, `horizontal =
  4.dp`) y los 4 `RecipeDetailFormat.metaEntries` con sus hairlines intactos. ✔
- **E4** (nada más se mueve): `RecipeDetailHeroFormat`/`RecipeDetailFormat`/`RecipeDetailState`/
  `ServingScaler`/`IngredientFormat` y sus tests **sin diff**; sin tests nuevos; `git status` limpio en el
  resto. ✔
- **E5** (ajuste de imágenes on/off): `RecipeArtwork` sigue como hija `matchParentSize()` **dentro** del
  `Box` recortado → imagen + `scrim` heredan el clip rectangular a ancho completo sin tocar `ui/artwork`;
  apagado = hero tipográfico con marca de agua. ✔
- **E6** (claro/oscuro): el recorte no depende de roles de color; la tira conserva sus roles. Revisión
  final de acabado de la persona en el emulador (no automatizable desde WSL). ✔ por inspección

## Disciplina de alcance y seguridad

- `git diff --name-only` (working tree vs `HEAD`): `DESIGN.md`,
  `app/src/main/java/com/sazon/app/ui/detail/RecipeDetailScreen.kt`, `PROGRESS.md`,
  `docs/specs/detail-hero-square-bottom.md`, `feature_list.json`. **Un solo** fichero de código; el resto
  son estado/docs (los persiste el flujo). Sin diff en `RecipeDetailHeroFormat`/`RecipeDetailFormat`/
  `RecipeDetailState`/`ServingScaler`/`IngredientFormat`, `ui/artwork/**`, `ui/theme/**`,
  `ui/MainShell.kt`, `domain/**`, `data/**`, `assets/**`, `app/build.gradle.kts`, `gradle/**`/lockfile,
  `init.sh`, `scripts/**`, el ADR 0006 ni `docs/design/concepts/**`; **0** ficheros de test en el diff;
  sin dependencias nuevas.
- La spec solo tiene cambios esperados del implementer (tareas marcadas `[x]` + sección "Estado de
  implementación"; ningún cambio de contrato).
- **Capturas**: `test ! -e docs/design/concepts/detail-hero-square-bottom` → **OK**; `git status
  docs/design/concepts` limpio (no se crea ni edita nada).
- **Docs durables**: `DESIGN.md` reconciliado en `Shapes` (líneas 326-333: el hero termina en **bordes
  rectos**; la tira **conserva** sus 24 dp y la sombra) y `Core Screens` 5 (líneas ~506-511); ninguna
  afirmación viva atribuye 24 dp al hero (la mención de 24 dp se atribuye correctamente a la tira).
  `grep "esquina" docs/technical-discovery.md` → **0** (no requería cambio); `docs/domain-model.md`,
  `AGENTS.md`, ADR 0006 y `docs/risks-and-open-questions.md` no requerían cambio (coherente con la spec).
- **Seguridad** (`references/security-checklist.md`): sin hallazgos. Cambio exclusivamente de layout
  Compose (`clip` de una `Box`); sin secretos, red, auth, entrada de usuario, datos personales,
  persistencia, dependencias ni ficheros.

## Hallazgos

**Critical / High / Medium: ninguno.** El resumen del implementer resiste el contraste independiente:
todos los greps, el diff, los recuentos de tests (delta 0) y el lint coinciden exactamente.

Low / informativos no bloqueantes (no requieren reparación para aceptar):

- **L1** — `DESIGN.md` línea 394 ("no cambian esquinas ni tamaños", sección de la imagen de fondo,
  `recipe-photos`) es una redacción genérica sobre la **imagen** (que no cambia forma ni tamaño); no
  nombra 24 dp ni el hero. Preidentificado en los Unknowns de la spec; **informativo**, sin deriva que
  contradiga la dirección vigente. No se toca.
- **L2** — `docs/design/concepts/recipe-detail-screen/detalle-receta/code.html` sigue diciendo "esquinas
  inferiores a 24px" del Detalle antiguo: material histórico previo a ADR 0006 y a
  `detail-hero-full-bleed`, **no** fuente de verdad (mismo criterio que la sesión 122). Fuera de alcance.
- **L3** — Incidencia de **entorno** durante mi validación (no atribuible a la feature): el primer
  `lint --rerun-tasks` provocó un **SIGSEGV de la JVM** (`G1ParScanThreadState::trim_queue_to_threshold`,
  `hs_err_pid70749.log`) en un daemon de Gradle ("7 busy and 3 stopped Daemons" por builds concurrentes de
  otros worktrees). El reintento de `lint --rerun-tasks` terminó **BUILD SUCCESSFUL** (30/30). El
  `hs_err_pid70749.log` que generó mi tirada se **eliminó**; `git status` quedó con solo los ficheros
  esperados. Recomendado: no solapar builds de Gradle sobre el mismo worktree.

## Cierre

Veredicto **`accept`**. El orquestador de `feature-flow` debe persistir `accepted` en
`feature_list.json`, registrar el cierre en `PROGRESS.md` y crear el commit local de la rama
`feature/detail-hero-square-bottom`; la publicación (push + PR, con `Closes #<n>`) queda para el
orquestador de `github-flow`. Queda para la persona la **revisión manual** en el emulador (no
automatizable desde WSL): abrir el Detalle desde Inicio y Resultados, en claro/oscuro y con el ajuste de
imágenes apagado/encendido, comprobando (a) hero con borde inferior recto y a ras con la tira (sin hueco
de fondo), (b) tira con su borde inferior redondeado y su sombra, (c) marca de agua y filete dentro del
hero, (d) "Atrás" y corazón legibles y pulsables, (e) pestañas/comensales/barra sin moverse, **contra
`DESIGN.md`**; un defecto visual se corrige con una feature correctiva.
