# Validación independiente — `recipe-card-typographic`

- Feature id: `recipe-card-typographic`
- Spec: `docs/specs/recipe-card-typographic.md`
- Rama/worktree: `feature/recipe-card-typographic`
  (`/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/recipe-card-typographic`)
- Rol: `validator` (contexto limpio; no implementa ni persiste `accepted`)
- Fecha: 2026-10-14
- Estado en `feature_list.json` al validar: `passing` (autoverificado por el implementer, sesión 104;
  sin commit/push/PR, trabajo en disco — esperado)
- Veredicto: **accept**

## Alcance validado

Segunda de las cinco features del ADR 0006 (`area: ui`, depende de
`card-typographic-tokens-and-design`, ya `accepted`). Convierte la tarjeta de receta en la **ficha
tipográfica** (dirección C3 + los tres ingredientes principales), sin ninguna foto:

1. **Lógica pura JVM** (`ui/home`): `RecipeCardFormat.categoryLine`/`mainIngredients`/`MainIngredient`/
   `matchBadge(score, compact)`/`ingredientsLine` y `RecipeMonogram.of(title)`.
2. **Ficha compartida** `ui/home/RecipeFicha.kt`: hueco 16:10 sin imagen, categoría en versalitas,
   título Manrope local, ingredientes en una línea (estrecho) o tres filas con `tnum` (ancho),
   metadatos, coincidencia en terracota, filete de 6px por cocina (`RecipeAccent`) y marca de agua del
   glifo al 12 %; cromo desde `LocalSazonCardColors`.
3. **Consumidores**: `RecipeCard` (Home), `ResultRecipeCard` (Resultados) y rejilla de
   `FavoriteRecipeTile`; la **lista** de Favoritos pasa a miniatura 96×96 de **monograma** con **regla
   terracota de 5px** (`primary`), no por cocina.
4. **Primitivas extraídas** a `ui/artwork/RecipeArtworkPrimitives.kt` (`RecipeGlyphArtwork`,
   `recipeAccentColors`, `recipeAccentBrush`); `RecipeArtwork` conserva firma y comportamiento y su
   único punto de uso queda en el Detalle (otra feature).

Capturas de referencia obligatorias revisadas (las 8 PNG existen):
`docs/design/concepts/recipe-card-typographic/{recipe-card,favoritos-lista,favoritos-rejilla}/`
(`screen.png`, `screen-dark.png`; `recipe-card` además `screen-resultados.png`/`screen-resultados-dark.png`)
más los `code*.html` y `DESIGN.md` de cada carpeta.

## Verificación re-ejecutada (independiente)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (test + lint vía `scripts/gradle.sh`; sin procesos largos, `init.sh` intacto) |
| Focal `ui.home` (forzado, mío) | `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.home.*" --rerun-tasks` | **BUILD SUCCESSFUL** (2m30s; 34/34 tareas) |
| Tests completo (forzado, mío) | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL** (3m22s; 34/34 tareas) |
| Parseo XML propio (fresco) | `app/build/test-results/testDebugUnitTest/TEST-*.xml` | **95 suites / 720 tests / 0 fallos / 0 errores / 0 saltados**; `RecipeCardFormatTest` **13**, `RecipeMonogramTest` **7** |
| Baseline en `HEAD` | `git grep "@Test"` en `HEAD` | **94 ficheros / 704 `@Test`** ⇒ delta **+1 suite / +16 tests** |
| Lint (forzado, mío) | `bash scripts/gradle.sh lint --rerun-tasks` | **BUILD SUCCESSFUL** (2m53s; 30/30 tareas) |
| SARIF fresco | `app/build/reports/lint-results-debug.sarif` | **0 errores**, 13 warnings de entorno; **0 bajo `app/src`** |
| Alcance | `git diff --name-only` | Expected File Changes + spec + `feature_list.json` (orquestador); sin diff en Detalle, `DESIGN.md`, `app/build.gradle.kts`, `gradle/**`, `init.sh`, `scripts/**`, `assets/**` |
| `RecipeArtwork(` | `grep -rn` en `app/src` | solo la definición + `ui/detail/RecipeDetailScreen.kt` (Detalle, otra feature) |
| Hex sueltos | `grep -rn "0xFF"` fuera de `ui/theme` | solo máscaras de bytes preexistentes (sin hex nuevos) |

## Desviaciones de spec evaluadas de forma independiente

1. **Monograma (Escenario 5) — correcta.** La redacción original ("dos primeras palabras
   significativas") daba `AP` para `Arroz con pollo criollo`, incompatible con el vector `AC` de las
   capturas. El validator reimplementó la regla por separado: `primera + última` palabra significativa
   reproduce **los cinco** vectores (`TP/AC/GA/CG/PT`); `dos primeras` falla (`AP`) y "2 letras de la
   primera palabra" también (`TO/AR/GA/CU/PA`). El cambio de spec a `primera + última` es la única
   lectura coherente con las capturas y está documentado. Aceptable.
2. **Rejilla de Favoritos (Escenario 3) — fiel.** El `code.html` aceptado de `favoritos-rejilla`
   renderiza cada celda con categoría + título 16px + una línea de ingredientes 10,5px + filete y
   **sin** fila de metadatos; la variante compacta de `RecipeFicha` reproduce exactamente eso (a 16:10 y
   ~172 dp con el título a 2 líneas no cabe). No decide **OQ6** y no encubre desviación.

## Coherencia de color (mandato: manda `DESIGN.md`)

Verificado en código y docs reconciliadas, todo en el mismo sentido:

- **Miniatura de la lista (96×96)**: `RecipeMonogramTile` → `surfaceVariant`, esquina media y **regla
  terracota 5px a la izquierda (`scheme.primary`)**; **no** por cocina.
- **Filete de 6px de las tarjetas**: `RecipeFicha` → `Brush.horizontalGradient(recipeAccentColors(...))`
  (**por cocina**, `RecipeAccent`).
- `feature_list.json` (`user_visible_behavior`), `docs/adr/0006` §6 y el README de capturas dicen
  **terracota 5px** + por-cocina en el filete de 6px. Sin contradicciones.

## Hallazgos

Sin hallazgos **Critical/High/Medium**. Ninguno bloquea la aceptación.

- **Low (no bloqueante)**: el KDoc de `SazonCardTokens` (`ui/theme/CardTokens.kt`) aún decía "en esta
  feature **no hay consumidores**" cuando ya hay dos (`RecipeFicha`, `FavoriteRecipeCard`). Corregido
  por el orquestador en el cierre (**solo comentario**, sin cambio de comportamiento): el validator lo
  autorizó como limpieza de la pasada de docs, sin exigir re-validación.
- **Low / Info (tensión captura ↔ DESIGN.md, resuelta a favor del contrato)**: en la tarjeta estrecha el
  prototipo/captura abrevia la fila de metadatos (`25 min · Fácil`) mientras `DESIGN.md` (`Components >
  RecipeCard`) y el Escenario 1 exigen `tiempo · dificultad · comensales`. La implementación sigue
  `DESIGN.md`/spec. Aviso para la revisión visual fina de la persona; si se quisiera el literal de la
  captura, sería una feature correctiva, no un bloqueo.
- **Info**: el bloque "Tus recetas/Guardadas" de la captura de Home se pinta con `RecipeCard` (ficha
  estrecha) y no con la fila de monograma del prototipo: **non-goal** explícito y preexistente (solo el
  carrusel estaba en alcance).
- **Info**: la narrativa de evidencia del implementer decía "sin diff en `feature_list.json`" (hoy sí
  está modificado, por el orquestador) y "dos máscaras `0xFF`" (hay 2 máscaras + 2 comparaciones de test
  preexistentes). Inmaterial.
- **Positivo**: se corrigió aquí el **I1** diferido de `card-typographic-tokens-and-design` (KDoc de
  `tintedSurface`: sRGB → Oklab), solo comentario, sin cambio de comportamiento.

## Seguridad

Sin hallazgos: diff de Compose + lógica pura JVM, sin red, sin I/O, sin secretos, sin permisos, sin
autenticación/autorización, sin datos de usuario nuevos, sin dependencias nuevas (`gradle/**` y
`app/build.gradle.kts` intactos) y sin `assets/` nuevos. `LocalSazonCardColors` mantiene default seguro
(`light`) para previews/tests.

## Documentación / harness

- `docs/technical-discovery.md`: actualizado (el párrafo de la tarjeta deja de ser "dirección pendiente";
  nombra `RecipeFicha`, la lógica pura y las capturas aportadas).
- `docs/adr/0006`: `Estado` (1.ª y 2.ª features) + **§6** reconciliado a terracota 5px + §7 ajustado.
- `DESIGN.md`: **intacto** (como exigía la spec). `docs/domain-model.md` y `AGENTS.md`: sin cambios.
- `init.sh`/`scripts/**`: intactos; el worktree sigue reiniciable por el camino estándar.
- `feature_list.json`: `passing` + evidencia al validar; `accepted` lo persiste el orquestador.
- Capturas obligatorias presentes y usadas como baseline; E2E/UI de emulador **no aplica** (coherente
  con `AGENTS.md`).

## Cierre

El **orquestador** de `feature-flow` persiste `accepted` en `feature_list.json` con la evidencia del
validador, registra la sesión en `PROGRESS.md` y crea el **commit local** en
`feature/recipe-card-typographic`. **Sin** push ni PR (los hace `github-flow`).
