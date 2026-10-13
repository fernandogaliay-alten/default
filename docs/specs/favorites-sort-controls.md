# Feature Implementation Spec: Controles de orden compactos en Favoritos

## Source Feature

- `id`: `favorites-sort-controls`
- `area`: `ui`
- `depends_on`: `["favorites"]` (`accepted`; esta correctiva **no** reabre ni replanifica esa feature)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`; el cambio de estado lo hacen
  implementer/validador/orquestador.
- `source`: `feature_list.json` + issue **#85**, que es el brief autoritativo.
- Verificación exigida (de la feature): `./gradlew test` (vía `bash init.sh`) sigue en verde **sin**
  lógica nueva que testear en JVM; prueba manual del chip "Recientes" sin recorte y del toggle icónico;
  `DESIGN.md` registra la desviación de microcopy y el toggle.
- **Desviación de capturas autorizada por la persona** (issue #85, misma clase que #77): **no** se crea
  `docs/design/concepts/favorites-sort-controls/`. Ver `Visual Design Impact`.

## Goal

Corregir el acabado de la fila de controles de la pestaña **Favoritos** (`MainTab.FAVORITES`) con dos
cambios acotados y sin tocar estado ni lógica:

1. El chip de orden por defecto (`FavoritesSort.RECENT`) deja de leerse **"Añadidas recientemente"** y
   pasa a leerse **"Recientes"**, de forma que quepa entero y **no aparezca recortado**.
2. Los dos chips de texto **"Lista"/"Rejilla"** se sustituyen por un **toggle compacto de dos botones
   circulares solo con icono** dentro de una píldora (activo relleno en terracota), tal como muestra la
   captura aceptada `docs/design/concepts/favorites/favoritos/code.html` (líneas 89-97).

El resto de la pantalla (buscador, chips "Tiempo"/"Dificultad", nota "Disponibles sin conexión",
tarjetas, rejilla, estado vacío) **no cambia**. Los iconos se dibujan con `Canvas` de Compose (el repo
**no** tiene `material-icons-*`), siguiendo el patrón de glifos del propio fichero (`CloudOffGlyph`,
`BookmarkGlyph`); **sin dependencias nuevas**.

## Non-Goals

- **No** se cambia la **lógica de orden ni de presentación**: `FavoritesList.visible`/`countLabel`,
  las `key` de `FavoritesSort`/`FavoritesViewMode`, `fromKey` y los `Saver` quedan con su
  comportamiento actual. Solo cambia el **literal** del label de `RECENT`.
- **No** se toca `FavoriteRecipeCard`/`FavoriteRecipeTile`, el buscador, la nota offline, la cabecera,
  el conteo ni el estado vacío.
- **No** se toca el dominio (`domain.favorites`), la persistencia (`data.favorites`, `favorites.db`),
  `FavoriteService`, `MainShell`/`SazonApp`/`MainActivity` ni la integración.
- **No** se añaden dependencias: ni `material-icons-*`, ni imágenes, ni librería de iconos.
  `app/build.gradle.kts` y `gradle/libs.versions.toml` **intactos**.
- **No** se crea `docs/design/concepts/favorites-sort-controls/` ni su `README.md` (capturas exentas,
  issue #85).
- **No** se implementan las otras correctivas pendientes (`profile-stats-heading`, `recipe-photos`) ni
  ningún rediseño de la pantalla Favoritos.

## Job Story

Cuando abro la pestaña Favoritos para buscar entre mis recetas guardadas,
quiero leer el criterio de orden activo completo ("Recientes") y cambiar entre lista y rejilla con un
control de iconos compacto,
para que la fila de controles se vea intencionada y sin texto cortado, igual que el diseño aceptado.

## Users And Permissions

- **Usuario local**: único dueño del dispositivo (ADR 0002). Ve el chip de orden, lo cambia y alterna
  lista/rejilla; no hay cuentas ni roles.
- **Desarrollador / agente**: ejecuta el gate JVM (`init.sh`) y verifica por inspección/grep que no
  queda ningún chip de texto "Lista"/"Rejilla" ni dependencia nueva. El acabado fino (recorte del chip,
  tamaño del toggle, claro/oscuro) lo revisa la persona en el emulador (AGENTS.md no automatiza UI).
- **Sistema**: no hay red, estado nuevo, ni persistencia; todo es local y determinista.

## Acceptance Scenarios

### Scenario 1: el chip por defecto se lee "Recientes" y no se recorta (verificación exigida)

Given favoritos persistidos y la pestaña Favoritos abierta (el orden por defecto es `FavoritesSort.RECENT`),
When se pinta la fila de chips de orden,
Then el primer chip muestra el texto **"Recientes"** completo (sin elipsis ni recorte) y sigue siendo el
chip activo; pulsarlo mantiene el mismo criterio de orden (`savedAt` descendente).

### Scenario 2: toggle lista/rejilla icónico (verificación exigida)

Given la fila de controles con el modo `LIST` activo,
When se pinta el selector de vista,
Then aparece una **píldora** (fondo de rol `surface`) con **dos botones circulares solo con icono**:
el de lista **activo** (círculo relleno en `primary` con el icono en `onPrimary`) y el de rejilla
**inactivo** (sin relleno, icono en `onSurfaceVariant`); **no** hay ningún texto "Lista" ni "Rejilla".

### Scenario 3: el toggle cambia el modo y conserva el comportamiento

Given el toggle icónico visible,
When el usuario pulsa el botón de rejilla,
Then el modo pasa a `GRID` (la lista se muestra en rejilla de dos columnas), el botón de rejilla queda
activo (relleno terracota) y el de lista inactivo; al pulsar el de lista vuelve a `LIST`. El modo se
sigue restaurando con el `Saver` existente (`FavoritesViewMode.fromKey`), sin cambios de estado.

### Scenario 4: el resto de la pantalla no cambia

Given la pestaña Favoritos poblada,
When se aplica la feature,
Then el buscador ("Buscar por título"), los chips "Tiempo"/"Dificultad", la nota "Disponibles sin
conexión", las tarjetas con corazón, la rejilla y el estado vacío con "Explorar recetas" se ven y se
comportan igual que antes; **no** se añade ni se quita ningún otro elemento.

### Scenario 5: claro y oscuro por roles del tema, sin hex sueltos

Given `SazonTheme` en modo claro y en oscuro,
When se pinta la píldora y sus botones,
Then los colores salen de **roles del tema** (`surface`, `primary`, `onPrimary`, `onSurfaceVariant`) —
nunca de hex propios— y en oscuro la píldora usa el `surface` oscuro con el botón activo terracota.

### Scenario 6: accesibilidad del toggle

Given los dos botones solo con icono,
When un lector de pantalla enfoca el toggle,
Then cada botón tiene una descripción ("Lista" / "Rejilla") y su estado seleccionado se anuncia (grupo
seleccionable con rol de radio), de modo que el estado no se transmite **solo** por color
(`DESIGN.md`, Accessibility Baseline).

### Scenario 7: sin lógica nueva y sin dependencias (verificación exigida)

Given el gate estándar,
When corre `bash init.sh`,
Then `./gradlew test` sigue en verde **con las mismas suites** (no hay lógica pura nueva que testear:
`FavoritesSort`/`FavoritesList`/`FavoritesViewMode` conservan su semántica) y `./gradlew lint` no añade
errores ni warnings nuevos en ficheros de la feature; `app/build.gradle.kts` y
`gradle/libs.versions.toml` permanecen intactos.

## Repository Research

### Files Inspected

- `feature_list.json` — entry `favorites-sort-controls` (área `ui`, `depends_on: ["favorites"]`,
  verificación exigida y nota con la causa raíz, la solución acordada y la desviación de capturas);
  entries vecinas (`favorites` `accepted`, `profile-stats-heading`, `recipe-photos`).
- `PROGRESS.md` — `favorites` aceptada en la sesión 053 con sus capturas; camino `init.sh`
  (`./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`); regla de no automatizar UI de
  emulador; política de capturas.
- `AGENTS.md` — UI verificada por lógica/estado en JVM cuando sea posible; capturas obligatorias salvo
  desviación autorizada; una feature por sesión; nunca `./gradlew` directo.
- Issue **#85** — brief autoritativo (causa raíz, solución acordada, desviación de capturas).
- `app/src/main/java/com/sazon/app/ui/favorites/FavoritesScreen.kt` — `FavoritesSortRow` (líneas
  180-215): `Row { LazyRow(Modifier.weight(1f)) { 3 FilterChip } ; Spacer ; ViewModeButton("Lista") ;
  ViewModeButton("Rejilla") }`. Los glifos `CloudOffGlyph` (nube tachada, `Stroke(1.5.dp)`) y
  `BookmarkGlyph` (`Path` relleno) son el patrón de "dibujar iconos con Compose" del propio fichero.
- `app/src/main/java/com/sazon/app/ui/favorites/FavoritesSort.kt` — `RECENT("Añadidas recientemente",
  "recent")`, `TIME`, `DIFFICULTY`, `fromKey` (desconocido → `RECENT`).
- `app/src/main/java/com/sazon/app/ui/favorites/FavoritesViewMode.kt` — `LIST("list")`,
  `GRID("grid")`, `fromKey` (desconocido → `LIST`).
- `app/src/main/java/com/sazon/app/ui/favorites/FavoritesList.kt` — `visible`/`countLabel` (no cambian).
- `app/src/main/java/com/sazon/app/ui/favorites/FavoriteRecipeCard.kt` — tarjeta/tile y corazón (no
  cambian; solo el toggle de arriba).
- `app/src/test/java/com/sazon/app/ui/favorites/FavoritesListTest.kt` — usa `FavoritesSort.RECENT` y
  `FavoritesViewMode.fromKey(mode.key)` como **enums/keys**, **no** afirma ningún `.label`: cambiar el
  literal del label **no** rompe tests (verificado por grep en `app/src/test`).
- `docs/specs/favorites.md` — spec de la feature base: describe la fila de chips y el toggle
  lista/rejilla; su Scenario 6 nombra el chip "Añadidas recientemente" (queda superado por esta
  correctiva) y su Validator Checklist es el estilo a seguir.
- `docs/specs/recipe-artwork.md` — precedente de **correctiva con desviación de capturas autorizada**:
  estructura de spec, registro explícito de la exención y exigencia de que `DESIGN.md` documente el
  alcance visual (modelo directo para el `Visual Design Impact` de esta spec).
- `docs/design/concepts/favorites/favoritos/code.html` — **captura aceptada** (claro y oscuro):
  - líneas 74-98 (claro) y 263-286 (oscuro): fila de chips + toggle; el chip activo es terracota con
    texto blanco y los otros dos en píldora `surface`; el toggle es una píldora `surface` (claro) /
    `#241C16` (oscuro) con **dos botones `w-7 h-7` (28px) redondos**: activo `bg-[#D9552B] text-white`
    (`view_agenda`), inactivo `text-[#8C7E74]` (`grid_view`), icono de 16px.
  - líneas 78-81 y 266-269: el export todavía escribe "Añadidas recientemente" (microcopy que esta
    feature acorta; ver `Visual Design Impact`).
- `docs/design/concepts/favorites/README.md` — ficha de la petición de captura de Favoritos (histórico).
- `DESIGN.md` — Core Screen 6 (Favoritos: "búsqueda, orden, lista, estado vacío con nota"); Components
  (**FilterChip**: "fila desplazable; seleccionado relleno de terracota"); Colors ("el terracota se
  reserva para **una** acción principal por pantalla"); Shapes ("Chips y botones: píldora completa");
  Accessibility Baseline (objetivos ≥48dp; "no depender solo del color"); `Generated Concept Images`
  (captura aceptada `favorites/favoritos` y la excepción de capturas documentada de `recipe-artwork`).
- `docs/design/stitch/17-favoritos.md` y `docs/design/stitch-prompt.md` — prompt de generación (hoy
  dice "Añadidas recientemente" y "two-button" toggle): es el **prompt histórico**, no la fuente de
  verdad.
- `docs/technical-discovery.md` — `ui.favorites` como presentación de Favoritos y la integración en
  `MainShell` (no cambia).
- `CONTEXT.md` — vocabulario de dominio (ningún término nuevo; "orden"/"Filtros" ya definidos).

### Existing Patterns To Follow

- **Capa Compose delgada**: `FavoritesScreen.kt` es UI pura; la lógica de orden presentación vive en
  `FavoritesList`/`FavoritesSort`/`FavoritesViewMode` (sin Compose). Esta correctiva **solo** toca la
  capa Compose y un literal.
- **Glifos dibujados con Compose**: `CloudOffGlyph`/`BookmarkGlyph` en el mismo fichero
  (`Canvas` + `Path`/`Stroke`, coordenadas normalizadas a `size`); el repo **no** tiene
  `material-icons-*` (ya verificado por `onboarding-shell-and-identity`).
- **Tokens del tema, nunca hex sueltos** (`ui.theme`): `MaterialTheme.colorScheme.*`; `SazonPillShape`
  para píldoras/chips.
- **Estado en `rememberSaveable` + `Saver`** (`FavoritesSortSaver`/`FavoritesViewModeSaver`) ya existe;
  se reutiliza sin cambios.
- **Tests JUnit 4** en `app/src/test/...`, nombres en español con `_`; aquí **no** se añade lógica pura.
- `init.sh` es el gate no bloqueante (`./gradlew test` + `./gradlew lint`), sin procesos largos.

### Current Gaps

- El label `RECENT("Añadidas recientemente")` es más ancho que el viewport del `LazyRow(weight(1f))`
  porque comparte línea con dos chips de texto, de modo que el chip por defecto sale **recortado**.
- El selector lista/rejilla son dos `FilterChip` de **texto** ("Lista"/"Rejilla"), que ocupan ancho y
  **no** coinciden con la captura aceptada (que ya usa un toggle de dos iconos).
- No existe ningún glifo de lista/rejilla en Compose en el repositorio.

## Technical Approach

Estrategia: **dos ediciones puntuales en `ui.favorites` + registro en `DESIGN.md`**, sin estado nuevo,
sin dependencias y sin tocar el dominio ni la persistencia.

### 1. Microcopy del chip por defecto — `FavoritesSort.kt`

```kotlin
enum class FavoritesSort(val label: String, val key: String) {
    RECENT("Recientes", "recent"),   // antes: "Añadidas recientemente"
    TIME("Tiempo", "time"),
    DIFFICULTY("Dificultad", "difficulty");
    // fromKey sin cambios (clave desconocida -> RECENT)
}
```

Solo cambia el **literal**; `key`, el orden de los entries, `fromKey` y la semántica de orden
(`FavoritesList.visible` con `savedAt` desc) permanecen idénticos. `FavoritesSortSaver` guarda por `key`,
así que un estado restaurado no depende del label.

### 2. Toggle lista/rejilla icónico — `FavoritesScreen.kt`

Sustituir los dos `ViewModeButton` de texto por una única función `ViewModeToggle`:

- Contenedor: píldora con rol **`surface`** (blanco en claro; `surface` oscuro en oscuro) y una sombra
  muy suave (p. ej. `Surface(shape = SazonPillShape, color = colorScheme.surface, shadowElevation =
  2.dp)`), con relleno interior ~2dp (`DESIGN.md`: chips/botones en píldora, sombras suaves).
- Dos botones **circulares de 28dp** (`Modifier.size(28.dp).clip(CircleShape)`), sin texto:
  - **activo**: fondo `primary` (terracota) y glifo teñido con `onPrimary`;
  - **inactivo**: sin relleno (se ve el `surface` de la píldora) y glifo teñido con `onSurfaceVariant`.
- Cada botón es seleccionable de forma accesible: `Row(Modifier.selectableGroup())` con
  `Modifier.selectable(selected = …, onClick = …, role = Role.RadioButton)` y descripción "Lista" /
  "Rejilla" (Scenario 6).
- Glifos nuevos, **privados** en `FavoritesScreen.kt`, con el patrón de `CloudOffGlyph`/`BookmarkGlyph`
  (`Canvas`, coordenadas normalizadas a `size`, trazo ~1.5–2dp), tamaño ~16dp dentro del círculo de 28dp:
  - `ListGlyph` — rectángulo redondeado vertical con una línea horizontal a ~1/3 de la altura (la forma
    del `view_agenda` de la captura);
  - `GridGlyph` — cuatro cuadrados redondeados en 2×2 (la forma del `grid_view` de la captura).
  La fidelidad **fina** de cada trazo la revisa la persona (Unknown).
- `FavoritesSortRow` conserva el `LazyRow(Modifier.weight(1f))` con los tres `FilterChip` de orden:
  con "Recientes" y un toggle más estrecho, el primer chip cabe entero en el viewport (fin del recorte).
  No se introduce `horizontalScroll` alternativo ni se cambia la disposición de la fila.

### 3. Documentación visual — `DESIGN.md`

- Registrar la **desviación de microcopy** ("Recientes" en lugar de "Añadidas recientemente" como label
  del orden por defecto de Favoritos) y el **toggle lista/rejilla icónico** (píldora `surface` con dos
  botones circulares de 28dp, activo en terracota) como **alcance de esta feature**, y anotar la
  excepción de capturas autorizada (issue #85, sin `docs/design/concepts/favorites-sort-controls/`),
  en la misma línea que la excepción ya documentada de `recipe-artwork`.
- Actualizar el bullet **FilterChip**/Components para mencionar el toggle icónico de Favoritos (o añadir
  un bullet propio), manteniendo `DESIGN.md` como fuente de verdad.

## Expected File Changes

**Modificar**

- `app/src/main/java/com/sazon/app/ui/favorites/FavoritesSort.kt` — label `RECENT` → `"Recientes"`
  (sin otros cambios).
- `app/src/main/java/com/sazon/app/ui/favorites/FavoritesScreen.kt` — sustituir `ViewModeButton`
  ("Lista"/"Rejilla") por `ViewModeToggle` (píldora + dos botones circulares icónicos) y añadir los
  glifos privados `ListGlyph`/`GridGlyph`; limpiar imports huérfanos si los hubiera.
- `DESIGN.md` — registrar el microcopy "Recientes" y el toggle icónico + la excepción de capturas de
  `favorites-sort-controls`.
- `PROGRESS.md` y `feature_list.json` — **actualizar al implementar** (estado y evidencia), no ahora.

**No se toca**: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `domain/**`, `data/**`,
`FavoriteRecipeCard.kt`, `FavoritesList.kt`, `FavoritesViewMode.kt`, `MainShell.kt`, `SazonApp.kt`,
`MainActivity.kt`, `ui.theme/**`, las capturas de `docs/design/concepts/favorites/**`, los prompts de
`docs/design/stitch*/**`, `init.sh`.

## Visual Design Impact

- UI involved: **sí** (fila de controles de la pestaña Favoritos).
- Design source: **captura aceptada** `docs/design/concepts/favorites/favoritos/` (claro y oscuro) —
  concretamente `code.html` líneas 89-97 (y 277-285 en oscuro) para el toggle icónico — más `DESIGN.md`
  (FilterChip, Colors, Shapes, Accessibility Baseline). Ante contradicción, manda `DESIGN.md`.
- Screens or states affected: pestaña Favoritos, fila de orden (chip activo + "Tiempo"/"Dificultad") y
  selector lista/rejilla; claro y oscuro; poblada y vacía (el estado vacío no muestra la fila).
- New design artifact required: **no**.
- **Capturas de UI — exención autorizada por la persona (issue #85; misma clase que #77 y ya anotada en
  la entry `favorites-sort-controls` de `feature_list.json`)**: esta feature **no** genera capturas y
  **no** se crea `docs/design/concepts/favorites-sort-controls/` ni su `README.md`. Motivo: el toggle
  icónico **ya** está en la captura aceptada de Favoritos, y el único cambio de microcopy ("Recientes")
  es un acortamiento del label existente. La dirección visual aplicable es
  `docs/design/concepts/favorites/favoritos/` + el `DESIGN.md` actualizado.
- **`DESIGN.md` debe registrar, como parte del alcance**, (a) el microcopy **"Recientes"** como label
  del orden por defecto de Favoritos y (b) el **toggle lista/rejilla icónico** (píldora `surface` con
  dos botones circulares de 28dp, activo terracota). Es contra ese registro contra lo que se revisa el
  acabado en el emulador (Scenario 1-3).
- Nota de tensión (Unknown): el toggle de la captura tiene círculos de **28dp**, por debajo del objetivo
  táctil de 48dp de `DESIGN.md` (igual que los `FilterChip` actuales de la fila). Se sigue la captura;
  la persona lo revisa y, si lo considera un defecto, se corrige con una feature correctiva.
- Un defecto visual se corrige con una **feature correctiva** (regla de AGENTS.md), no bloquea el flujo.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace ninguna regla MUST/MUST NOT de nivel repo.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas de trabajo no cambian.
- `docs/technical-discovery.md`: **not needed** — no cambia el stack, el almacenamiento, la
  integración ni el camino de verificación; solo el acabado de una fila de UI.
- `docs/domain-model.md`: **not needed** — no cambian entidades, relaciones ni estados.
- `DESIGN.md` y `docs/design/concepts/`: **update** — registrar el microcopy "Recientes", el toggle
  icónico y la excepción de capturas de `favorites-sort-controls` (no se crea carpeta de capturas).
- `docs/design/concepts/favorites/README.md` y los prompts `docs/design/stitch*/`: **not needed** — son
  la petición y el prompt **históricos** que produjeron la captura aceptada; reescribirlos
  distorsionaría el registro del export.
- `docs/specs/favorites.md`: **not needed** — es la spec de la feature base ya `accepted`; su Scenario 6
  queda superado por esta correctiva, que es la que documenta el nuevo microcopy.
- `PROGRESS.md` y `feature_list.json`: **update al implementar** (estado y evidencia), no en esta sesión.

## Implementation Plan

1. Cambiar el label de `FavoritesSort.RECENT` a `"Recientes"` en `FavoritesSort.kt`.
2. Crear `ViewModeToggle` en `FavoritesScreen.kt` (píldora `surface` + dos botones circulares de 28dp
   con selección accesible) y los glifos privados `ListGlyph`/`GridGlyph` con `Canvas`.
3. Cablear `FavoritesSortRow` al nuevo `ViewModeToggle` y eliminar `ViewModeButton` (y los imports que
   queden sin uso).
4. Actualizar `DESIGN.md` (microcopy + toggle + excepción de capturas).
5. Ejecutar `bash init.sh` en verde y registrar la evidencia.

## Implementation Tasks

- [ ] `FavoritesSort.RECENT` → label `"Recientes"` (sin tocar `key`/`TIME`/`DIFFICULTY`/`fromKey`).
- [ ] Añadir `ListGlyph` y `GridGlyph` (privados, `Canvas`, patrón `CloudOffGlyph`/`BookmarkGlyph`).
- [ ] Crear `ViewModeToggle` (píldora `surface` + sombra suave; dos círculos de 28dp; activo
      `primary`/`onPrimary`, inactivo `onSurfaceVariant`; `selectable` con rol de radio y descripción).
- [ ] Sustituir en `FavoritesSortRow` los dos `ViewModeButton` por `ViewModeToggle`; borrar
      `ViewModeButton` y los imports huérfanos.
- [ ] Verificar por grep que no quedan los literales de texto "Lista"/"Rejilla" como botones ni
      referencias a `ViewModeButton`.
- [ ] Actualizar `DESIGN.md` (microcopy "Recientes", toggle icónico, excepción de capturas #85).
- [ ] Ejecutar `bash init.sh` en verde y anotar la evidencia en `feature_list.json`/`PROGRESS.md`.

## Verification Plan

- `bash init.sh` (vía `scripts/gradle.sh`) → **esperado**: exit 0 (`./gradlew test` + `./gradlew lint`),
  sin procesos largos. Es el gate no bloqueante; **no cambia** y no necesita modos nuevos.
- `./gradlew test` (vía `init.sh`) → **esperado**: **las mismas suites, todas en verde**; **no** hay
  lógica pura nueva que testear (`FavoritesSort`/`FavoritesList`/`FavoritesViewMode` conservan su
  semántica y `FavoritesListTest` sigue pasando porque usa enums/keys, no `.label`). Si el implementer
  cree necesario cubrir el label, un `assertEquals("Recientes", FavoritesSort.RECENT.label)` es
  **opcional** y no altera el contrato; no se exige.
- `./gradlew lint` (vía `init.sh`) → **esperado**: 0 errores y **sin warnings nuevos** en ficheros de la
  feature (los preexistentes se mantienen).
- **Inspección/grep**:
  - `FavoritesSort.kt`: solo cambia el literal del label; `key`, entries y `fromKey` intactos.
  - `FavoritesScreen.kt`: el selector de vista es iconográfico; **0** apariciones de los literales de
    texto "Lista"/"Rejilla" como etiquetas de botón y **0** referencias a `ViewModeButton`.
  - `app/build.gradle.kts` y `gradle/libs.versions.toml` **intactos** (sin `material-icons-*`); sin hex
    sueltos en el código nuevo (solo roles de `MaterialTheme.colorScheme`); sin cambios en
    `FavoriteRecipeCard.kt`, `FavoritesList.kt`, `FavoritesViewMode.kt`, `domain/**`, `data/**`,
    `MainShell.kt`, `SazonApp.kt`, `MainActivity.kt` ni `init.sh`; `git diff --stat` solo con lo previsto.
- **Manual (emulador, lo revisa la persona; AGENTS.md no automatiza UI)** — **verificación exigida**:
  - Abrir Favoritos con favoritos y comprobar que el chip **"Recientes"** se ve **entero** (sin
    recorte) y sigue activo; que "Tiempo" y "Dificultad" funcionan.
  - Comprobar el **toggle icónico**: píldora con dos botones circulares; el activo relleno terracota;
    pulsar cada uno alterna lista/rejilla; revisar **claro y oscuro**.
  - Confirmar que el resto de la pantalla (buscador, nota offline, tarjetas, rejilla, estado vacío) no
    cambió, contra la captura `docs/design/concepts/favorites/favoritos/`.

Cobertura E2E: **no aplica**. No existe suite E2E persistente y AGENTS.md prohíbe automatizar UI de
emulador; esta correctiva no introduce lógica pura nueva, así que el gate JVM solo comprueba que nada se
rompe y el acabado lo revisa la persona contra la captura aceptada.

Sobre `init.sh`: **no cambia**; sigue ejecutando solo comprobaciones no bloqueantes (`test` + `lint`) y
no arranca procesos largos.

## Evidence To Capture

- Salida de `./gradlew test` (vía `init.sh`): mismas suites, **0 fallos/errores/saltados** y el total.
- Salida de `lint`: 0 errores y **sin** warnings nuevos en ficheros de la feature; `init.sh` con
  código 0.
- Grep: sin literales "Lista"/"Rejilla" como botones y sin `ViewModeButton`; sin dependencias nuevas;
  sin hex sueltos; `git diff --stat` solo con los ficheros previstos.
- `DESIGN.md` con el microcopy "Recientes", el toggle icónico y la excepción de capturas (#85)
  registrados.
- Resultado manual en emulador (descripción; captura opcional): chip "Recientes" completo y toggle
  icónico funcionando en claro y oscuro, contra la captura `favorites/favoritos`.
- Anotación en `feature_list.json` de la evidencia y del estado `passing` (al implementar).

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **El label pasa a `"Recientes"`** y es la única edición de "lógica": el chip sigue siendo el orden por
  defecto (`key = "recent"`, `savedAt` desc) y `fromKey` no cambia, así que los `Saver` y los tests
  existentes siguen valiendo.
- **El toggle es puramente visual**: reutiliza el estado `viewMode` y el `Saver` ya existentes; no se
  duplica estado ni se introduce `FavoritesViewMode` nuevo.
- **Los iconos se dibujan con `Canvas`** (patrón `CloudOffGlyph`/`BookmarkGlyph` del mismo fichero)
  porque el repositorio no tiene `material-icons-*` y esta correctiva no añade dependencias.
- **Colores por roles del tema**: píldora `surface`, activo `primary`/`onPrimary`, inactivo
  `onSurfaceVariant` — mapea el `#D9552B`/`#8C7E74`/`white` de la captura y funciona en claro y oscuro
  sin hex sueltos.
- **Tamaño del círculo 28dp y glifo ~16dp**, como la captura (`w-7 h-7`, icono de 16px).
- **Se conserva el `LazyRow(Modifier.weight(1f))`** de los chips de orden: el recorte se corrige por el
  label más corto y un selector más estrecho, sin rehacer el layout de la fila.
- **Accesibilidad sin depender del color**: `selectable` con rol de radio, grupo seleccionable y
  descripciones "Lista"/"Rejilla" (Accessibility Baseline de `DESIGN.md`).
- **`DESIGN.md` es el único registro visual** de esta correctiva (microcopy + toggle + excepción de
  capturas), como en `recipe-artwork`; no se crea carpeta de capturas.
- **Sin dependencias nuevas y `init.sh` sin cambios.**

### Unknowns

- **Recorte exacto en pantallas pequeñas / tipografía ampliada**: la corrección se valida visualmente en
  el emulador; el label corto debe caber, pero con "fuente muy grande" (`DESIGN.md`, Responsive
  Baseline) el primer chip podría seguir apretado. Es revisión de la persona; un defecto se corrige con
  feature correctiva.
- **Objetivo táctil de 28dp vs 48dp de `DESIGN.md`**: se sigue la captura aceptada; si la persona lo
  considera un problema de ergonomía, se corrige con una feature correctiva (afecta también a los
  `FilterChip` de la fila, ya por debajo de 48dp hoy).
- **Fidelidad fina de los glifos `ListGlyph`/`GridGlyph`**: son line-art dibujado a mano con Compose y
  no tienen test; el parecido con `view_agenda`/`grid_view` lo revisa la persona.
- **Microcopy "Añadidas recientemente" en artefactos históricos** (`docs/design/stitch*/`, la captura
  `code.html` y `docs/specs/favorites.md`): se dejan intactos a propósito (registran lo que se pidió y
  generó); `DESIGN.md` es el registro vivo del microcopy vigente.

## Validator Checklist

- [ ] La implementación se queda en `FavoritesSort.kt` (literales del label) y `FavoritesScreen.kt`
      (toggle icónico) + `DESIGN.md`; sin rediseñar el resto de Favoritos ni tocar otras correctivas.
- [ ] El chip por defecto se lee **"Recientes"** y sigue activo por defecto sin recorte (Scenario 1,
      verificación exigida).
- [ ] El selector lista/rejilla es una **píldora `surface` con dos botones circulares solo con icono**
      (activo relleno terracota), **sin** texto "Lista"/"Rejilla"; alterna `LIST`/`GRID` con el estado y
      el `Saver` existentes (Scenarios 2 y 3).
- [ ] El resto de la pantalla (buscador, "Tiempo"/"Dificultad", nota offline, tarjetas, rejilla, estado
      vacío) no cambia (Scenario 4).
- [ ] Colores por roles del tema (sin hex sueltos) y correctos en claro y oscuro (Scenario 5).
- [ ] El toggle es accesible: descripciones "Lista"/"Rejilla" y estado seleccionado anunciado, sin
      depender solo del color (Scenario 6).
- [ ] **No** hay lógica pura nueva ni cambio de semántica en `FavoritesSort`/`FavoritesList`/
      `FavoritesViewMode`; `./gradlew test` sigue en verde con las mismas suites (Scenario 7).
- [ ] Sin dependencias nuevas (`app/build.gradle.kts` y `gradle/libs.versions.toml` intactos); sin tocar
      dominio, persistencia, `MainShell`/`SazonApp`/`MainActivity` ni `init.sh`.
- [ ] Para UI: `Visual Design Impact` registra la **exención de capturas** (#85), **no** se crea
      `docs/design/concepts/favorites-sort-controls/`, y `DESIGN.md` registra el microcopy "Recientes" y
      el toggle icónico como alcance (verificación exigida).
- [ ] `init.sh` en verde y evidencia en `feature_list.json`/`PROGRESS.md`; `DESIGN.md` coherente.
