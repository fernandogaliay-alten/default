# Feature Implementation Spec: Quitar el grupo vacío "Abiertas hace poco" de la Home

## Source Feature

- `id`: `home-recently-opened-group`
- `area`: `ui`
- `depends_on`: `["home-offline-blocks"]` (`accepted`, sesión 033; esta correctiva **no** reabre ni
  replanifica esa feature)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`; el cambio de estado lo hacen
  implementer/validador/orquestador.
- `source`: `feature_list.json` (+ nota de la feature con la desviación de capturas autorizada por la
  persona, 2026-10-13). **No** hay número de issue en el repositorio: la issue la crea `github-flow`
  con la rama `feature/home-recently-opened-group`; no inventarlo.
- Verificación exigida (de la feature): `./gradlew test` (vía `bash init.sh`) sigue en verde y la
  costura ajustada queda cubierta en JVM (p. ej. que "Tus recetas" no tenga ningún grupo sin recetas
  que pintar, **o** el estado vacío cuando no hay ninguna); prueba manual en emulador de que con al
  menos una receta guardada "Tus recetas" muestra "Guardadas" y **no** "Abiertas hace poco";
  `DESIGN.md` registra que el bloque solo lista grupos con contenido y la desviación autorizada.
- **Desviación de capturas autorizada por la persona**: esta feature **no** genera capturas nuevas ni
  carpeta `docs/design/concepts/home-recently-opened-group/` (misma clase que `recipe-artwork` #77,
  `favorites-sort-controls` #85 y `profile-stats-heading` #86). Ver `Visual Design Impact`.

## Goal

Corregir el bloque 5 de la Home ("Tus recetas"): hoy pinta **siempre** el subtítulo de grupo
`"Abiertas hace poco"` aunque su lista venga vacía —lo que ocurre **siempre**, porque `MainShell`
alimenta `HomeRecipes.recentlyOpened` con `emptyList()` hardcodeado—, así que el usuario ve un título
sin nada debajo en cuanto hay al menos una receta guardada (si no hay ninguna gana el estado vacío
global y el defecto queda tapado).

Tras la feature, "Tus recetas" **solo pinta los grupos con contenido** (hoy, "Guardadas", alimentado
por los Favoritos), mantiene la fila de filtros locales, la línea "No hay recetas con este filtro." y
el estado vacío global `"Cuando guardes una receta aparecerá aquí."`. Como la costura
`HomeRecipes.recentlyOpened` está **muerta** (sin productor: ninguna feature registra `lastOpenedAt`),
se **retira** en vez de conservar código sin uso, ajustando `HomeRecipesTest`.

## Non-Goals

- **No** se implementa "Abiertas hace poco": ninguna feature (ni ninguna de las pendientes) registra
  `lastOpenedAt`; no se añade repositorio, persistencia, campo ni dato. La costura se retirará y
  volverá la primera feature que registre `lastOpenedAt`.
- **No** se toca "Guardadas" ni su fuente de datos (los Favoritos, `FavoriteRepository`,
  `data.favorites`), ni el filtro local (`ui.filters`, `LocalResultFilter`, `ResultFilterRow`), ni el
  estado vacío global, ni "No hay recetas con este filtro.".
- **No** se toca el resto de la Home: cabecera/saludo/avatar, buscador con IA (bloque 2), tarjeta-héroe,
  carrusel "Sugeridas para ti", barra inferior, FAB, `RecipeCard`, `RecipeArtwork`.
- **No** se rediseña el bloque ni se renombra `HomeRecipes`: solo se le quita el grupo muerto.
- **No** se editan specs aceptadas (`docs/specs/home-offline-blocks.md`,
  `docs/specs/local-result-filters.md`, `docs/specs/favorites.md`): son el registro histórico; su
  Scenario 5 / menciones a `recentlyOpened` quedan **superados** por esta correctiva.
- **No** se crea `docs/design/concepts/home-recently-opened-group/` ni su `README.md` (capturas
  exentas) y **no** se tocan `docs/design/concepts/**` ni `docs/design/stitch*/**`.
- **No** se añaden dependencias ni infraestructura de tests: `app/build.gradle.kts`,
  `gradle/libs.versions.toml`, `init.sh` y `scripts/**` quedan **intactos**. No se añaden tests de UI
  instrumentados (`androidTest` está fuera del gate y no es ejecutable desde WSL).

## Job Story

Cuando abro la Home y tengo recetas guardadas,
quiero ver bajo "Tus recetas" solo los grupos que tienen recetas,
para no leer un subtítulo ("Abiertas hace poco") que no lleva a ninguna parte.

## Users And Permissions

- **Usuario local**: único dueño del dispositivo (ADR 0002); ve el bloque "Tus recetas" en la Home. No
  hay cuentas ni roles.
- **Desarrollador / agente**: retira la costura muerta, hace que el grupo no se pinte sin contenido,
  ajusta `HomeRecipesTest` y registra la desviación en `DESIGN.md`. Verifica el gate (`bash init.sh`),
  los greps de inspección y la coherencia documental.
- **Persona**: revisa en el emulador el bloque "Tus recetas" (con y sin guardadas, claro y oscuro); un
  defecto visual se corrige con una feature correctiva (AGENTS.md no automatiza UI).

## Acceptance Scenarios

### Scenario 1: con recetas guardadas, solo se pinta "Guardadas" (verificación exigida)

Given un Perfil en `MAIN` con al menos un Favorito persistido,
When se pinta el bloque "Tus recetas" de la Home,
Then se ven el título "Tus recetas", la fila de filtros locales y el grupo **"Guardadas"** con su
carrusel, y **no** aparece en ningún caso el subtítulo **"Abiertas hace poco"** (ni un carrusel vacío
debajo).

### Scenario 2: sin ninguna receta se mantiene el estado vacío (verificación exigida, JVM)

Given que no hay ningún Favorito,
When se pinta el bloque "Tus recetas",
Then solo se ven el título y la línea `"Cuando guardes una receta aparecerá aquí."`; **no** se pinta
ningún grupo ni la fila de filtros (`HomeRecipes.isEmpty == true`).

### Scenario 3: filtro local sin coincidencias

Given guardadas que no encajan en el filtro seleccionado,
When se pinta el bloque,
Then se ve la fila de filtros y la línea `"No hay recetas con este filtro."`, y **no** se pinta el
título "Guardadas" sin contenido (el grupo vacío no pinta nada).

### Scenario 4: la costura ajustada queda cubierta en JVM (verificación exigida)

Given `HomeRecipes` sin el grupo muerto,
When corre `./gradlew test` (vía `bash init.sh`),
Then `HomeRecipesTest` cubre `isEmpty` (vacío sin guardadas; no vacío con guardadas) y `filteredBy`
(categoría, frontera de "Rápido" en 30, "Todas" y sin coincidencias) sobre `saved`, sin ninguna
referencia a `recentlyOpened` en `app/src`.

### Scenario 5: `DESIGN.md` registra la estructura vigente y la desviación (verificación exigida)

Given `DESIGN.md` describe hoy el bloque con **dos** grupos ("Guardadas" y "Abiertas hace poco"),
When se implementa,
Then `DESIGN.md` registra que el bloque **solo lista grupos con contenido** (hoy, "Guardadas") y la
**exención de capturas** de `home-recently-opened-group`, sin crear
`docs/design/concepts/home-recently-opened-group/`.

### Scenario 6: nada más de la Home cambia

Given la Home actual,
When se aplica la feature,
Then cabecera/saludo/avatar, buscador con IA, tarjeta-héroe, "Sugeridas para ti", tarjetas, barra
inferior y FAB se ven y se comportan igual; "Guardadas" sigue saliendo de los Favoritos y el filtro
local, el estado vacío y "No hay recetas con este filtro." no cambian.

## Repository Research

### Files Inspected

- `feature_list.json` — entry `home-recently-opened-group` (verificación exigida y nota con el motivo,
  el alcance previsto y la desviación autorizada); `home-offline-blocks` `accepted` (sesión 033) y
  vecinas. **Inventario**: la única otra feature `not_started` es `dish-image-domain` (imágenes de
  plato, sin relación) → **ninguna feature pendiente** va a poblar "Abiertas hace poco".
- `PROGRESS.md` — sesión 084: gate **64 suites / 456 tests / 0 fallos** y lint **0 errores / 13
  warnings** preexistentes (0 bajo `app/src`); camino `bash init.sh` (`./gradlew test` + `./gradlew
  lint` vía `scripts/gradle.sh`); regla de no automatizar UI de emulador.
- `AGENTS.md` — UI verificada por lógica/estado en JVM cuando sea posible + capturas obligatorias salvo
  desviación autorizada; un defecto visual se corrige con feature correctiva; nunca `./gradlew` directo.
- `app/src/main/java/com/sazon/app/ui/home/HomeScreen.kt` — **causa raíz**:
  - `RecipesSection` (líneas 217-273): `allRecipes = recipes.saved + recipes.recentlyOpened` (225);
    con `!recipes.isEmpty` pinta `ResultFilterRow` y, si `!filtered.isEmpty`, llama a **los dos**
    grupos: `HomeRecipesGroup("Guardadas", filtered.saved)` (258-263) y
    `HomeRecipesGroup("Abiertas hace poco", filtered.recentlyOpened)` (264-269).
  - `HomeRecipesGroup` (279-310): el `Text(title)` se pinta **siempre** (290-294); el `LazyRow` solo
    si `recipes.isNotEmpty()` (295-308). Su KDoc (275-278) declara "muestra su titulo siempre". Es
    justo el defecto reportado: título sin contenido.
- `app/src/main/java/com/sazon/app/ui/home/HomeRecipes.kt` — costura pura (19-30): `saved`,
  `recentlyOpened` (con `emptyList()` por defecto), `isEmpty = saved.isEmpty() && recentlyOpened.isEmpty()`
  y `filteredBy(filter, taxonomy)` que acota **ambos** grupos. Su KDoc (8-18) ya documentaba que ambas
  listas llegan vacías y que `favorites`/`recipe-detail-screen` las llenarían.
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — **único consumidor**: líneas 173-176 pasan
  `HomeRecipes(saved = favorites.map { it.recipe }, recentlyOpened = emptyList())`. El grupo
  "abiertas" es un literal `emptyList()`: **no hay productor** de `recentlyOpened` en el repositorio.
- `app/src/test/java/com/sazon/app/ui/home/HomeRecipesTest.kt` — **8 tests**; los **8** mencionan
  `recentlyOpened` (unas veces como argumento, otras como aserción: líneas 26, 31, 38, 50/56, 66/75,
  82/88, 95, 107), incluido el caso que hay que **eliminar**,
  `con_una_receta_abierta_hace_poco_el_bloque_no_esta_vacio` (37-41), y los 5 casos de `filteredBy`
  "por grupo" (43-57, 59-76, 78-89, 91-101 y 103-113) que hay que adaptar a un único grupo.
- `docs/specs/favorites.md` (líneas 33-34 y 539-540) — **evidencia de que la costura está muerta**:
  "no hay ninguna feature que registre `lastOpenedAt`; el grupo de la Home sigue recibiendo
  `emptyList()` y queda documentado" (Unknown explícito de esa feature, ya `accepted`).
- `docs/specs/recipe-detail-screen.md` (línea 41) — Non-Goal: **no** se persiste "recetas abiertas hace
  poco" (`lastOpenedAt`); feature `accepted`.
- `docs/domain-model.md` (líneas 69-70) — la persistencia de una receta `ai` "guarda `generatedAt` y
  `lastOpenedAt`" y `lastOpenedAt` alimenta el grupo "Abiertas hace poco": intención de **dominio**,
  hoy no implementada (ninguna feature la registra).
- `CONTEXT.md` (líneas 106-109) — el término de dominio "Bloque 'Tus recetas'" agrupa favoritas
  ("Guardadas") y últimas abiertas ("Abiertas hace poco"): **vocabulario**, no implementación.
- `DESIGN.md` — Core Screen 2 (283-286) describe Home con el bloque "Tus recetas" y **dos grupos**
  ("Guardadas" y "Abiertas hace poco"); `Generated Concept Images` (97-161) con la lista de capturas
  aceptadas, el **backfill pendiente** de `home-offline-blocks: home, home-sin-conexion` (130) y los
  párrafos de excepción de `recipe-artwork` (#77), `favorites-sort-controls` (#85) y
  `profile-stats-heading` (#86) — plantilla exacta de la redacción de esta desviación.
- `docs/design/concepts/home-offline-blocks/README.md` (18-25) — petición de capturas de la Home
  (`home/`, `home-sin-conexion/`), todavía **pendiente** de exportar.
- `docs/design/concepts/README.md` — política de capturas (obligatorias salvo exención autorizada;
  manda `DESIGN.md`).
- `docs/technical-discovery.md` (102-121, 134, 151-154) — ubica la Home (`ui.home`), la costura
  `HomeRecipes.saved` (bloque "Guardadas") y `HomeRecipes.filteredBy` "acota **cada grupo** de forma
  independiente".
- `docs/specs/local-result-filters.md` (24-40, 237-243, 390-399) — spec aceptada que documenta
  `filteredBy` sobre `saved` y `recentlyOpened` (queda superada en ese punto).
- `app/build.gradle.kts` (108-116) y `app/src/androidTest/` — la única infra de Compose UI
  (`androidx-compose-ui-test-junit4`, `createComposeRule`) es **instrumentada** (`androidTest`), y el
  único fichero es el `ExampleInstrumentedTest` de plantilla; `bash init.sh` **no** ejecuta
  `connectedAndroidTest` y AGENTS.md no automatiza UI de emulador. **Robolectric sí está en
  `testImplementation`**, pero sin `ui-test-junit4` en `testImplementation` no hay Compose en JVM (y
  añadirlo queda fuera de alcance).

### Existing Patterns To Follow

- **Lógica pura + capa Compose delgada**: la costura del bloque (`HomeRecipes`) es pura y se testea en
  JVM; los `@Composable` solo pintan.
- **Costura explícita, sin código sin uso**: `favorites` eliminó `ui/SectionPlaceholder.kt` y
  `home-offline-blocks` eliminó `ui/HomePlaceholder.kt` al quedarse sin referencias; mismo criterio
  aquí para `recentlyOpened`.
- **Tokens del tema, nunca hex sueltos**; los literales de UI viven en los `@Composable`
  (`"Guardadas"`, el estado vacío, "No hay recetas con este filtro.").
- **Correctivas que no reescriben la historia**: las specs aceptadas se dejan intactas; el nuevo
  comportamiento se documenta en la spec de la correctiva y en `DESIGN.md`
  (`favorites-sort-controls`, `profile-stats-heading`).
- **Tests JUnit 4** en `app/src/test/...`, nombres en español con `_`, casos negativos que no pasan en
  vacío; la Taxonomía real se lee por la propiedad de sistema `taxonomy.asset.path`.
- `bash init.sh` es el gate no bloqueante (`./gradlew test` + `./gradlew lint`), sin procesos largos y
  sin cambios de modo.

### Current Gaps

- `HomeRecipesGroup` pinta el título con independencia de que la lista esté vacía, y `RecipesSection`
  lo invoca para "Abiertas hace poco" siempre que el bloque tenga alguna receta.
- `HomeRecipes.recentlyOpened` **no tiene productor**: `MainShell` lo rellena con `emptyList()`, y
  `favorites` + `recipe-detail-screen` (ambas `accepted`) dejaron explícito que **no** registran
  `lastOpenedAt`.
- `DESIGN.md` describe el bloque con dos grupos, sin registrar que la implementación solo muestra los
  grupos con contenido.

## Technical Approach

Estrategia: **retirar la costura muerta + hacer que el grupo no se pinte sin contenido + registrar la
desviación en `DESIGN.md`**. Sin capas nuevas, sin estado nuevo, sin dependencias.

### 1. Costura pura — `ui/home/HomeRecipes.kt`

```kotlin
data class HomeRecipes(val saved: List<Recipe> = emptyList()) {
    val isEmpty: Boolean get() = saved.isEmpty()

    fun filteredBy(filter: LocalResultFilter, taxonomy: Taxonomy): HomeRecipes =
        copy(saved = LocalResultFilters.visible(saved, filter, taxonomy))
}
```

- Se **elimina** `recentlyOpened` (y su rama en `isEmpty` y en `filteredBy`) por estar muerta.
- `saved` conserva nombre y semántica ("Guardadas", alimentadas por los Favoritos).
- KDoc actualizado: la costura del bloque solo lleva las guardadas; "Abiertas hace poco" volverá con la
  feature que registre `lastOpenedAt`.

### 2. Capa Compose — `ui/home/HomeScreen.kt`

- `RecipesSection`:
  - `val allRecipes = recipes.saved` (para `LocalResultFilters.categoriesIn`).
  - Se elimina la llamada a `HomeRecipesGroup("Abiertas hace poco", ...)`.
  - El grupo "Guardadas" se pinta **sin** decidir su no-vacuidad en el call site; la decisión vive en el
    grupo, de modo que el guard es **alcanzable** hoy (con un filtro que deje 0 guardadas se pinta la
    línea "No hay recetas con este filtro." y el grupo no pinta nada):

    ```kotlin
    if (recipes.isEmpty) {
        Text("Cuando guardes una receta aparecerá aquí.", ...)
    } else {
        ResultFilterRow(...)
        HomeRecipesGroup(title = "Guardadas", recipes = filtered.saved, taxonomy, onOpenRecipe)
        if (filtered.isEmpty) {
            Text("No hay recetas con este filtro.", ...)
        }
    }
    ```
- `HomeRecipesGroup`: deja de pintar el título cuando no hay recetas — un grupo sin contenido **no se
  pinta** (invariante del componente, que es donde vivía el defecto):

  ```kotlin
  if (recipes.isEmpty()) return   // equivalente: envolver todo en if (recipes.isNotEmpty()) { ... }
  Column(...) { Text(title, ...); LazyRow { items(recipes) { RecipeCard(...) } } }
  ```

  (El early return o el `if (recipes.isNotEmpty()) { ... }` que envuelva el `Column` son equivalentes;
  el implementer elige el que prefiera, **sin** dejar el `Text(title)` fuera de la condición.)
  KDoc: "nunca pinta un grupo sin recetas; con recetas, título + carrusel horizontal".
- Sin cambios de tokens, espaciado, tipografía ni microcopy (`"Guardadas"`,
  `"Cuando guardes una receta aparecerá aquí."` y `"No hay recetas con este filtro."` se mantienen
  literales).

### 3. Integración — `ui/MainShell.kt`

- Líneas 173-176: `HomeRecipes(saved = favorites.map { it.recipe })` (se elimina la línea
  `recentlyOpened = emptyList()`). Nada más cambia.

### 4. Tests — `app/src/test/java/com/sazon/app/ui/home/HomeRecipesTest.kt`

- Ajustar al modelo de un solo grupo: renombrar el caso del estado vacío, **eliminar**
  `con_una_receta_abierta_hace_poco_el_bloque_no_esta_vacio` y adaptar los casos de `filteredBy` a
  `saved` (categoría, frontera 30 de "Rápido", "Todas" conserva, sin coincidencias → `isEmpty`,
  `filteredBy` no muta el original). **Net −1 test**; no añadir tests que pasen en vacío.

### 5. Documentación visual — `DESIGN.md`

- Core Screen 2: sustituir "y dos grupos: 'Guardadas' y 'Abiertas hace poco'" por la estructura
  vigente: el bloque **solo lista los grupos con contenido** (hoy, "Guardadas"; "Abiertas hace poco"
  volverá cuando exista una feature que registre `lastOpenedAt`), manteniendo filtros locales y el
  estado vacío `"Cuando guardes una receta aparecerá aquí."`.
- `Generated Concept Images`: añadir, **después** del párrafo de excepción de `profile-stats-heading`
  (#86), un párrafo de excepción con el formato/tono de los vecinos para `home-recently-opened-group`:
  no genera capturas nuevas ni carpeta `docs/design/concepts/home-recently-opened-group/`; es una
  correctiva que **quita** un grupo vacío; la dirección visual aplicable es el **backfill pendiente**
  de `home-offline-blocks` (`home`, `home-sin-conexion`, ver
  `docs/design/concepts/home-offline-blocks/README.md`) más Core Screen 2 y `Components`. Citar el
  feature id (y el número de issue de la feature si está disponible en el contexto de la rama/`gh`; no
  inventarlo).

## Expected File Changes

**Crear**

- Ninguno. (No se crean specs de terceros, ni tests de UI, ni carpeta de capturas; tampoco
  `docs/design/concepts/home-recently-opened-group/`.)

**Modificar**

- `app/src/main/java/com/sazon/app/ui/home/HomeRecipes.kt` — retirar `recentlyOpened`; `isEmpty` y
  `filteredBy` sobre `saved`; KDoc.
- `app/src/main/java/com/sazon/app/ui/home/HomeScreen.kt` — `RecipesSection` (sin el grupo "Abiertas
  hace poco", `allRecipes = recipes.saved`) y `HomeRecipesGroup` (no pinta nada sin recetas) + KDoc.
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — quitar el literal `recentlyOpened = emptyList()`
  de `HomeRecipes(...)`.
- `app/src/test/java/com/sazon/app/ui/home/HomeRecipesTest.kt` — ajustar los casos al modelo de un
  grupo (net −1 test).
- `DESIGN.md` — Core Screen 2 + párrafo de excepción de capturas de `home-recently-opened-group`.
- `docs/technical-discovery.md` — coherencia mínima: la Home pinta solo los grupos con contenido y
  `HomeRecipes` conserva un único grupo (`saved`) (una frase, cerca de las líneas 134/151-154).
- `PROGRESS.md` y `feature_list.json` — **actualizar al implementar** (estado y evidencia), no ahora.

**No se toca**: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`,
`ui/theme/**`, `ui/filters/**`, `ui/favorites/**`, `ui/profile/**`, `ui/search/**`, `ui/detail/**`,
`ui/artwork/**`, `domain/**`, `data/**`, `MainActivity.kt`, `ui/SazonApp.kt`,
`docs/specs/home-offline-blocks.md`, `docs/specs/local-result-filters.md`, `docs/specs/favorites.md`,
`docs/design/concepts/**`, `docs/design/stitch/**`, `CONTEXT.md`, `docs/domain-model.md`.

## Visual Design Impact

- UI involved: **sí** (bloque "Tus recetas" de la Home; una etiqueta de grupo que desaparece).
- Design source: **`DESIGN.md`** (Core Screen 2, `Components` y `Generated Concept Images`) es la
  fuente de verdad; dirección visual aplicable de la Home: el **backfill pendiente** de
  `home-offline-blocks` (`docs/design/concepts/home-offline-blocks/README.md`, pantallas `home` y
  `home-sin-conexion`, todavía sin exportar) más el prompt histórico `docs/design/stitch-prompt.md`
  sección B. Ante contradicción, manda `DESIGN.md`.
- Screens or states affected: Home → bloque "Tus recetas" **poblado** (con guardadas) y **vacío**
  (usuario nuevo); claro y oscuro; con y sin filtro aplicado.
- New design artifact required: **no**.
- **Capturas de UI — exención autorizada por la persona (misma clase que #77/#85/#86, anotada en la
  entry de `feature_list.json`)**: esta feature **no** genera capturas y **no** se crea
  `docs/design/concepts/home-recently-opened-group/` ni su `README.md`. Motivo: es una correctiva que
  **elimina** un grupo vacío (no introduce pantalla, layout, token ni estado nuevo); la estructura y el
  microcopy vigentes se registran en `DESIGN.md`.
- **`DESIGN.md` debe registrar, como parte del alcance** (Scenario 5): el bloque "Tus recetas" **solo
  lista grupos con contenido** y la exención de capturas de esta correctiva. Es contra ese registro
  contra lo que se revisa el acabado en el emulador.
- Nota de tensión (Unknown): el prompt histórico y las capturas pendientes de la Home **sí** describen
  "Abiertas hace poco"; no se editan (fuera de alcance) y la diferencia queda cubierta por el registro
  en `DESIGN.md`.
- Un defecto visual se corrige con una **feature correctiva** nueva (AGENTS.md), sin bloquear el flujo.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace ninguna regla MUST/MUST NOT de nivel repo.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas de trabajo no cambian.
- `docs/technical-discovery.md`: **update** (mínimo) — la Home pinta solo los grupos con contenido y
  `HomeRecipes` conserva un único grupo (`saved`); la costura `recentlyOpened` ya no existe.
- `docs/domain-model.md`: **not needed** — no cambian entidades, relaciones ni estados; `lastOpenedAt`
  (líneas 69-70) sigue siendo una intención de dominio **no implementada**, y el seam volverá con la
  feature que lo registre.
- `CONTEXT.md`: **not needed** — "Bloque 'Tus recetas'" sigue siendo el término de dominio (agrupa las
  recetas que ya están en el dispositivo); no se renombra vocabulario.
- `DESIGN.md`: **update** — registrar que el bloque solo lista grupos con contenido y la excepción de
  capturas de `home-recently-opened-group` (Core Screen 2 + `Generated Concept Images`).
- `docs/design/concepts/`: **no change** — capturas exentas por decisión de la persona; **no** se crea
  la carpeta de esta feature ni se tocan las peticiones/backfills existentes.
- `docs/specs/home-offline-blocks.md`, `docs/specs/local-result-filters.md`: **not needed** — specs de
  features `accepted`; su Scenario 5 / menciones a `recentlyOpened` quedan superados por esta
  correctiva, que es la que documenta el comportamiento nuevo.
- `PROGRESS.md` y `feature_list.json`: **update al implementar** (estado y evidencia), no en esta
  sesión de planificación.

## Implementation Plan

1. Retirar `recentlyOpened` de `HomeRecipes` y ajustar `isEmpty`/`filteredBy`/KDoc.
2. Ajustar `HomeRecipesTest` al modelo de un grupo (net −1 test) y ejecutarlo.
3. En `HomeScreen`, quitar el grupo "Abiertas hace poco", usar `recipes.saved` para las categorías y
   hacer que `HomeRecipesGroup` no pinte nada sin recetas (KDoc incluido).
4. En `MainShell`, quitar `recentlyOpened = emptyList()`.
5. Actualizar `DESIGN.md` (Core Screen 2 + párrafo de excepción) y `docs/technical-discovery.md`.
6. Ejecutar `bash init.sh` en verde, correr los greps de inspección y registrar la evidencia en
   `feature_list.json`/`PROGRESS.md`.

## Implementation Tasks

- [ ] `HomeRecipes`: eliminar `recentlyOpened`; `isEmpty = saved.isEmpty()`; `filteredBy` solo filtra
      `saved`; KDoc actualizado.
- [ ] `HomeRecipesTest`: eliminar el caso de "abierta hace poco" y adaptar los **7 restantes** al modelo
      de un grupo (sin tests vacíos).
- [ ] `HomeScreen.RecipesSection`: `allRecipes = recipes.saved`; sin la llamada a "Abiertas hace poco";
      grupo "Guardadas" invocado sin condicionar su no-vacuidad en el call site.
- [ ] `HomeScreen.HomeRecipesGroup`: `if (recipes.isEmpty()) return` y KDoc "nunca un grupo sin
      contenido".
- [ ] `MainShell`: `HomeRecipes(saved = favorites.map { it.recipe })`.
- [ ] `DESIGN.md`: Core Screen 2 (solo grupos con contenido) + párrafo de excepción de capturas.
- [ ] `docs/technical-discovery.md`: una frase de coherencia sobre el bloque y la costura.
- [ ] Greps de inspección (ver Verification Plan) y `bash init.sh` en verde.
- [ ] Registrar evidencia y estado en `feature_list.json`/`PROGRESS.md`.

## Verification Plan

- `bash init.sh` (vía `scripts/gradle.sh`) → **esperado**: exit 0 (`./gradlew test` + `./gradlew lint`),
  sin procesos largos; **no cambia** y no necesita modos nuevos.
- `./gradlew test` (vía `init.sh`) → **esperado**: todas las suites verdes; `HomeRecipesTest` con los
  casos ajustados (referencia de la sesión 084 en este worktree: 64 suites / 456 tests; este cambio
  retira 1 caso → **esperado ≈455**; el implementer registra el **observado**, no un número heredado).
  - `sin_guardadas_el_bloque_esta_vacio`: `HomeRecipes().isEmpty` y `HomeRecipes(saved = emptyList())`
    `isEmpty`.
  - `con_una_receta_guardada_el_bloque_no_esta_vacio`: `isEmpty == false`.
  - `filtered_by_*`: categoría filtra `saved`; "Rápido" incluye 30 y excluye 31; "Todas" conserva;
    sin coincidencias → `isEmpty`; `filteredBy` no muta el original.
  - **Honestidad sobre la cobertura**: el guard de pintado vive en `HomeRecipesGroup` (Compose) y **no**
    es testeable en JVM; la infra de Compose UI del repo es **instrumentada** (`androidTest`, fuera de
    `init.sh` y no ejecutable desde WSL) y esta feature no la toca. El gate automático cubre la costura
    pura (`HomeRecipes`: estado vacío y `filteredBy`); el no-pintado del grupo vacío se verifica por
    inspección + prueba manual. No se admite como evidencia "un test nuevo" que no exista.
- `./gradlew lint` (vía `init.sh`) → **esperado**: 0 errores y sin warnings nuevos en ficheros de la
  feature.
- **Inspección/grep** (evidencia objetiva):
  - `grep -rn "recentlyOpened" app/src` → **0** resultados.
  - `grep -rn "Abiertas hace poco" app/src` → **0** resultados (sigue solo en specs/prompts
    históricos y en `CONTEXT.md`/`DESIGN.md` donde se documenta).
  - `grep -n "HomeRecipes(" app/src/main/java/com/sazon/app/ui/MainShell.kt` → sin el argumento
    `recentlyOpened`.
  - `test -d docs/design/concepts/home-recently-opened-group` → **no existe**.
  - `git diff --stat` limitado a los ficheros previstos (+ `feature_list.json`/`PROGRESS.md`).
  - `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh` y `scripts/**` sin diff.
  - Ficheros nuevos/editados en **UTF-8 sin BOM** (lección del BOM de `taxonomy.json`).
- **Manual (emulador, lo revisa la persona; AGENTS.md no automatiza UI)** — **verificación exigida**:
  - Con al menos una receta guardada, "Tus recetas" muestra "Guardadas" y **no** "Abiertas hace poco"
    (Scenario 1).
  - Sin recetas guardadas, solo el título y "Cuando guardes una receta aparecerá aquí." (Scenario 2).
  - Con un filtro sin coincidencias, "No hay recetas con este filtro." y ningún grupo vacío
    (Scenario 3).
  - Revisar claro y oscuro y confirmar que el resto de la Home no cambió (Scenario 6).

Cobertura E2E: **no aplica**. No existe suite E2E persistente; AGENTS.md prohíbe automatizar UI de
emulador y esta feature no toca autenticación, autorización, routing ni flujos de API.

Sobre `init.sh`: **no cambia**; sigue ejecutando solo comprobaciones no bloqueantes (`test` + `lint`),
no arranca procesos largos y no necesita modos nuevos.

## Evidence To Capture

- Salida de `bash init.sh` con código 0 y los conteos de `./gradlew test` (suites/tests/0 fallos, con
  `HomeRecipesTest` ajustado) y `./gradlew lint` (0 errores, sin warnings nuevos en la feature).
- Salidas de los greps: 0 `recentlyOpened` y 0 `"Abiertas hace poco"` en `app/src`; ausencia de
  `docs/design/concepts/home-recently-opened-group/`.
- `git diff --stat` dentro del alcance previsto; `app/build.gradle.kts`, `gradle/libs.versions.toml`,
  `init.sh` y `scripts/**` intactos.
- Diff de `DESIGN.md` (Core Screen 2 + párrafo de excepción de capturas) y de
  `docs/technical-discovery.md`.
- Resultado manual en emulador (descripción; captura opcional): "Guardadas" sin "Abiertas hace poco",
  estado vacío y filtro sin coincidencias, en claro y oscuro.
- Anotación en `feature_list.json` de la evidencia y cambio de estado a `passing` (al implementar).

## Validator Checklist

- [ ] El bloque "Tus recetas" **no pinta ningún grupo sin contenido**: con guardadas solo "Guardadas";
  sin recetas, solo el estado vacío; con filtro sin coincidencias, la línea correspondiente
  (Scenarios 1-3).
- [ ] `HomeRecipes` ya **no** tiene `recentlyOpened`; no queda ninguna referencia a él en `app/src`, y
  `HomeRecipesTest` está ajustado (net −1 test) sin tests que pasen en vacío (Scenario 4).
- [ ] `DESIGN.md` registra que el bloque solo lista grupos con contenido y la exención de capturas de
  `home-recently-opened-group`; **no** existe `docs/design/concepts/home-recently-opened-group/`
  (Scenario 5).
- [ ] "Guardadas" sigue saliendo de los Favoritos y el filtro local, el estado vacío y la línea de
  filtro sin coincidencias no cambian; cabecera, héroe, sugerencias, buscador, barra y FAB intactos
  (Scenario 6).
- [ ] No se han creado spec/tests de UI ni infraestructura nueva; `app/build.gradle.kts`,
  `gradle/libs.versions.toml`, `init.sh` y `scripts/**` intactos.
- [ ] No se editan `docs/specs/home-offline-blocks.md`, `docs/specs/local-result-filters.md` ni
  `docs/specs/favorites.md` (specs aceptadas, superadas en ese punto por esta correctiva).
- [ ] `bash init.sh` en verde (test + lint), sin romper ninguna suite existente y sin warnings nuevos.
- [ ] `feature_list.json` y `PROGRESS.md` quedan actualizados con evidencia verificable.

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **La costura `recentlyOpened` está muerta y se retira**: el único consumidor (`MainShell`) lo
  alimenta con `emptyList()` literal y **no hay productor** — `favorites` (`docs/specs/favorites.md`
  33-34, 539-540) y `recipe-detail-screen` (línea 41) declararon explícitamente que **no** registran
  `lastOpenedAt`, y la única otra feature `not_started` (`dish-image-domain`) no tiene relación. El
  criterio del repositorio es no conservar código sin uso (`SectionPlaceholder`, `HomePlaceholder`).
- **El invariante vive en `HomeRecipesGroup`**: es el componente que tenía el defecto (título siempre);
  con `if (recipes.isEmpty()) return` ningún grupo se pinta sin contenido. En `RecipesSection` el grupo
  "Guardadas" se invoca sin condicionar su no-vacuidad en el call site, para que ese guard sea
  **alcanzable hoy** (filtro sin coincidencias) y no código defensivo inerte.
- **No se implementa "Abiertas hace poco"**: implementarlo exigiría registrar `lastOpenedAt` (dominio +
  persistencia), fuera del alcance de una correctiva; la spec lo deja como no-goal y documenta el
  retorno de la costura con esa feature.
- **`HomeRecipes` se conserva como costura del bloque** (con `saved`): sigue siendo el tipo que alimenta
  el bloque desde `MainShell` y donde vive la lógica pura (`isEmpty`, `filteredBy`); solo se le quita el
  grupo muerto.
- **`DESIGN.md` es el registro visual**: la captura/prompt históricos de la Home describen
  "Abiertas hace poco" y el backfill de `home-offline-blocks` sigue pendiente; la desviación (bloque
  solo con grupos con contenido, sin capturas nuevas) queda registrada en `DESIGN.md`, que manda.
- **Sin dependencias nuevas, sin infra de tests nueva y `init.sh` sin cambios.**

### Unknowns

- **Número de issue de la feature**: no está en el repositorio (lo crea `github-flow`). El párrafo de
  `DESIGN.md` cita el feature id y, si el número está disponible en el contexto de la rama/`gh`, se
  añade como en los párrafos vecinos; **no** se inventa.
- **Cobertura del no-pintado**: el guard es de Compose y no se puede cubrir en JVM sin añadir
  infraestructura (prohibida por el alcance); la evidencia es inspección + prueba manual. Un fallo de
  acabado se corrige con feature correctiva.
- **Retorno de "Abiertas hace poco"**: cuando una feature registre `lastOpenedAt`, habrá que reintroducir
  el grupo (y su seam) y volver a decidir su microcopy; `CONTEXT.md`/`docs/domain-model.md` conservan
  la intención de dominio, así que no hay pérdida de información.
- **Conteo de tests de referencia**: la evidencia histórica muestra conteos distintos por worktree; el
  implementer registra el **observado** en su gate (referencia local: 456 → ≈455 por el caso retirado).
