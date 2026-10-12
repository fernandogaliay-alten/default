# Feature Implementation Spec: Titular "Estadísticas" en el resumen del Perfil

## Source Feature

- `id`: `profile-stats-heading`
- `area`: `ui`
- `depends_on`: `["activity-summary"]` (`accepted`, sesión 058)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`; el cambio de estado lo hacen
  implementer/validador/orquestador.
- `source`: `feature_list.json` + issue **#86**, que es el brief autoritativo.
- Verificación exigida (de la feature): `bash init.sh` sigue en verde; **no** hay lógica nueva que
  testear en JVM (es un literal de UI); prueba manual en emulador de que el bloque de cifras se titula
  "Estadísticas" y las tarjetas siguen sin navegación; `DESIGN.md` registra la desviación de microcopy.
- **Desviación de capturas autorizada por la persona** (issue #86, misma clase que la excepción de
  `recipe-artwork` del issue #77): esta feature **no** genera capturas nuevas ni carpeta
  `docs/design/concepts/profile-stats-heading/`. Ver `Visual Design Impact`.

## Goal

Cambiar el **titular visible** de la sección de estadísticas del Perfil: el literal `"Resumen"` de
`ActivitySummarySection` pasa a **`"Estadísticas"`**, para que quede claro que las dos tarjetas
("recetas generadas" y "favoritas") son **estadística informativa y no navegable** (un usuario leyó
"2 favoritas" como un acceso).

El cambio es **solo microcopy**: no se toca ningún número, contador, derivación, layout, token ni
comportamiento. La sección sigue siendo la misma `ActivitySummarySection` **entre la cabecera de
identidad y la sección "Sugerencias"**, con las mismas dos tarjetas `Surface` no interactivas. El
Non-Goal **"No se añade navegación"** de `docs/specs/activity-summary.md` sigue **intacto**.

## Non-Goals

- **No** se añade navegación, `clickable`, `onClick`, enlace ni destino alguno a las tarjetas: siguen
  siendo informativas (Non-Goal de `activity-summary`, vigente).
- **No** se cambia ningún número, etiqueta de tarjeta ("recetas generadas" / "favoritas"), layout,
  orden de secciones, tipografía, token ni estado (0/0 incluido).
- **No** se renombran identificadores de dominio/código: "Resumen de actividad" (`CONTEXT.md`),
  `ActivitySummarySection`, `domain.activity.**`, `data.activity.**`, `activity_summary.db` y los
  comentarios de costura de `SuggestionBatchService`/`GenerationService`/`AiSearchService`
  **se quedan** como están (el término de dominio no es el titular de pantalla).
- **No** se crea `docs/design/concepts/profile-stats-heading/` (ni `README.md`): capturas exentas por
  autorización de la persona.
- **No** se toca `docs/specs/activity-summary.md` (su Non-Goal sigue siendo cierto tal cual).
- **No** se añaden dependencias, tests de UI ni infraestructura nueva: `app/build.gradle.kts` y
  `gradle/libs.versions.toml` quedan **intactos**.
- **No** se toca el Perfil más allá de este titular (identidad, preferencias, alergias, Sugerencias,
  ajustes).

## Job Story

Cuando miro mi Perfil y veo dos cifras bajo un titular,
quiero que ese titular diga que son **mis estadísticas**,
para entender de un vistazo que son informativas y no un acceso a otra pantalla.

## Users And Permissions

- **Usuario local**: único dueño del dispositivo (ADR 0002); lee las cifras. No hay cuentas ni roles.
- **Desarrollador / agente**: cambia el literal, mantiene la verdad de los KDoc y registra la
  desviación en `DESIGN.md`. Verifica el gate (`bash init.sh`) y la ausencia de interacción por
  inspección; **no** hay lógica nueva que cubrir en JVM.
- **Persona**: revisa el acabado en el emulador (AGENTS.md no automatiza UI).

## Acceptance Scenarios

### Scenario 1: el titular es "Estadísticas" (verificación exigida)

Given la pestaña Perfil con la sección de actividad,
When se abre el Perfil,
Then el bloque de las dos tarjetas se titula exactamente **"Estadísticas"** (y ya **no** dice
"Resumen"), en claro y en oscuro, y tanto con contadores poblados ("24"/"12") como en la variante
0/0.

### Scenario 2: las tarjetas siguen sin navegación (verificación exigida)

Given el bloque de cifras ya titulado "Estadísticas",
When el usuario pulsa cualquiera de las dos tarjetas,
Then **no ocurre nada**: no hay `clickable`/`onClick`, ni cambio de pestaña, ni navegación, ni
feedback de pulsación; las tarjetas siguen siendo `Surface` + `Column` de solo texto y su etiqueta
("recetas generadas" / "favoritas") no cambia.

### Scenario 3: nada más del Perfil cambia

Given el Perfil antes del cambio,
When se aplica la feature,
Then solo cambia el literal del titular (más los KDoc que lo citan y el registro en `DESIGN.md`):
identidad, preferencias, alergias, "Sugerencias", posición de la sección, tokens, tamaños, cifras
tabulares y el orden identidad → **estadísticas** → Sugerencias → Mis preferencias permanecen
idénticos.

### Scenario 4: `DESIGN.md` registra la desviación (verificación exigida)

Given que la captura aceptada `activity-summary/perfil-resumen` y el prompt
`docs/design/stitch/21-perfil-resumen.md` titulan la sección como "Resumen",
When se implementa,
Then `DESIGN.md` registra la **desviación de microcopy** ("Estadísticas" en vez de "Resumen") y la
**exención de capturas** de `profile-stats-heading`, sin crear `docs/design/concepts/profile-stats-heading/`.

## Repository Research

### Files Inspected

- `feature_list.json` — entry `profile-stats-heading` (verificación exigida y nota de la desviación
  autorizada) y su predecesora `activity-summary`; vecina correctiva `favorites-sort-controls`.
- `PROGRESS.md` — `activity-summary` **`accepted`** (sesión 058); política de capturas y regla "un
  defecto visual se corrige con una feature correctiva".
- `AGENTS.md` — capturas obligatorias de UI salvo autorización; UI no se automatiza (lógica/estado en
  JVM + revisión manual de la persona); una feature por sesión; nunca `./gradlew` directo.
- `CONTEXT.md` — "Resumen de actividad" es el término de **dominio** (Datos: recetas generadas y
  favoritas); no es el titular de pantalla.
- `docs/build-brief.md` — MVP slice 8: el Perfil incluye el Resumen de actividad (recetas generadas y
  favoritas).
- `docs/specs/activity-summary.md` — Non-Goal "**No** se añade navegación"; describe
  `ActivitySummarySection(recipesGeneratedCount, favoritesCount)` con encabezado y dos tarjetas.
- `app/src/main/java/com/sazon/app/ui/profile/ActivitySummarySection.kt` — **único** sitio con el
  literal: línea 40 `text = "Resumen",` (el KDoc de línea 20 también lo cita).
- `app/src/main/java/com/sazon/app/ui/profile/ProfileScreen.kt` — inserta
  `ActivitySummarySection` como item entre `ProfileIdentityEditor` y `ProfileSuggestionsSection`
  (líneas 111-116); su KDoc (línea 37) cita la sección como `"Resumen"`. `SectionHeading` es un
  helper privado distinto, usado por "Mis preferencias"/"Alergias e intolerancias"/"Mi cocina".
- `app/src/main/java/com/sazon/app/ui/MainShell.kt`, `ui/SazonApp.kt`, `MainActivity.kt` — aportan los
  dos números; **no** contienen el literal (no se tocan).
- `DESIGN.md` — Core Screens 7 ("Resumen de actividad (recetas generadas y favoritas)"), sección
  `Generated Concept Images` con la lista de capturas aceptadas (`activity-summary: perfil-resumen`) y
  el párrafo de **excepción de `recipe-artwork`** (issue #77), plantilla de redacción de esta
  desviación.
- `docs/design/concepts/activity-summary/perfil-resumen/{screen.png,code.html,DESIGN.md}` y su
  `README.md` — la captura aceptada titula la sección `"Resumen"` (p. ej. `code.html` línea 153);
  `docs/design/stitch/21-perfil-resumen.md` línea 4 pide `Section title "Resumen"`.
- `docs/design/concepts/README.md` — política: capturas obligatorias salvo exención autorizada;
  `DESIGN.md` manda ante contradicciones.
- `app/src/test/**` — **no existe** ningún test que afirme el literal del titular (grep de `"Resumen"`
  en `app/src/test`: 0 coincidencias; las menciones a "Resumen" del código son KDoc/dominio, no
  aserciones de microcopy). No hay ningún test JVM de Compose.
- `app/build.gradle.kts` / `gradle/libs.versions.toml` / `app/src/androidTest/**` — la infraestructura
  de Compose UI (`androidx-compose-ui-test-junit4`, `createComposeRule`) está declarada **solo** para
  `androidTest` (instrumentado) y el único fichero es el `ExampleInstrumentedTest` de plantilla; el
  gate `bash init.sh` **no** ejecuta `connectedAndroidTest` y AGENTS.md **no** automatiza UI de
  emulador (el `adb` de WSL no ve el emulador).

### Existing Patterns To Follow

- **Capa Compose delgada**: los literales visibles viven en los `@Composable`; el dominio y los datos
  no saben de microcopy.
- **Términos de dominio en español y estables**: se cambia el titular, **no** el vocabulario
  (`CONTEXT.md` "Resumen de actividad" sigue siendo el término de dominio).
- **Tokens del tema, nunca hex sueltos** (`ui.theme`): el titular ya usa
  `titleMedium`/`onBackground`; no se toca.
- **Acentos UTF-8** ya presentes en recursos de UI (`ProfileScreen`: "Alimentación", "Alergias e
  intolerancias"); el fichero debe seguir en **UTF-8 sin BOM** (lección del incidente de BOM en
  `docs/domain/taxonomy.json`, detectado por `TaxonomyIntegrityTest`).
- **`bash init.sh` es el gate no bloqueante** (`./gradlew test` + `./gradlew lint`), sin procesos
  largos y sin cambios de modo.
- **Un defecto visual se corrige con una feature correctiva nueva** (AGENTS.md), no se reabre esta.

### Current Gaps

- `ActivitySummarySection.kt` línea 40 titula la sección `"Resumen"`, que no explicita que las tarjetas
  son estadística informativa.
- El KDoc de `ActivitySummarySection` (línea 20) y el de `ProfileScreen` (línea 37) citan la sección
  como `"Resumen"`.
- `DESIGN.md` **no** recoge todavía la desviación de microcopy ni la exención de capturas de
  `profile-stats-heading`.

## Technical Approach

Cambio puntual de microcopy, sin capas nuevas ni dependencias:

1. En `ui/profile/ActivitySummarySection.kt`, sustituir el literal de la línea 40
   (`text = "Resumen",` → `text = "Estadísticas",`). El resto del `@Composable` (encabezado
   `titleMedium`/`onBackground`, `Row`, `ActivityStatCard`, `tnum`, tokens) **no cambia**.
2. Actualizar los **KDoc** que citan el titular para que no mientan: `ActivitySummarySection.kt`
   (línea ~20: `Seccion **"Estadísticas"** ...`) y `ProfileScreen.kt` (línea ~37: seccion
   **"Estadísticas"**). Se permite dejar intactas las menciones al **término de dominio** "Resumen de
   actividad" (`domain.activity`, `data.activity`, costuras de reporte), porque no son microcopy.
3. **No** añadir test: el literal es un texto de UI y no hay nada que cubrir en JVM; la única
   infraestructura de Compose UI del repo es **instrumentada** (`androidTest`, fuera del gate `init.sh`
   y no ejecutable desde WSL), así que un test del titular no sería evidencia útil y añadirlo queda
   fuera de alcance.
4. Registrar la desviación en `DESIGN.md` (ver `Visual Design Impact`):
   - **añadir** un párrafo de excepción en `Generated Concept Images`, inmediatamente después del
     párrafo de excepción de `recipe-artwork` (mismo formato/tono), que diga: `profile-stats-heading`
     **no** genera capturas nuevas ni carpeta `docs/design/concepts/profile-stats-heading/`; la
     dirección visual aplicable es la captura aceptada `activity-summary: perfil-resumen`; el titular
     visible del bloque de cifras del Perfil es **"Estadísticas"** (la captura y el prompt
     `docs/design/stitch/21-perfil-resumen.md` dicen "Resumen") para explicitar que las dos tarjetas
     son estadística informativa y **no navegable**; el resto de la sección no cambia.
   - **ajustar** la frase de Core Screens 7 para que cite el titular visible **"Estadísticas"**
     (manteniendo el significado: tarjetas informativas no navegables), de modo que `DESIGN.md` sea
     internamente coherente. Es el único otro retoque admitido en `DESIGN.md`.
5. Ejecutar el gate (`bash init.sh`) y registrar evidencia.

## Expected File Changes

**Crear**

- Ninguno. (No se crea spec de terceros, ni tests, ni carpeta de capturas.)

**Modificar**

- `app/src/main/java/com/sazon/app/ui/profile/ActivitySummarySection.kt` — literal del titular
  `"Resumen"` → `"Estadísticas"` y su KDoc.
- `app/src/main/java/com/sazon/app/ui/profile/ProfileScreen.kt` — solo el KDoc que cita la sección
  (línea ~37). **Sin** cambios de firma, layout ni orden de items.
- `DESIGN.md` — párrafo de desviación (microcopy + exención de capturas) y ajuste de Core Screens 7.
- `PROGRESS.md` y `feature_list.json` — **actualizar al implementar** (estado y evidencia), no ahora.

**No se toca**: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`,
`ui/theme/**`, `ui/MainShell.kt`, `ui/SazonApp.kt`, `MainActivity.kt`, `domain.activity/**`,
`data.activity/**`, `domain.suggestions/**`, `domain.generation/**`, `domain.search/**`,
`docs/specs/activity-summary.md`, `docs/design/concepts/**` (ni crear
`docs/design/concepts/profile-stats-heading/`) y `docs/design/stitch/**`.

## Visual Design Impact

- UI involved: **sí** (una cadena visible en la pestaña Perfil).
- Design source: **`DESIGN.md`** (Core Screens 7 y tokens) es la fuente de verdad; dirección visual
  aplicable = **captura aceptada** `docs/design/concepts/activity-summary/perfil-resumen/`
  (`screen.png`, `code.html`, `DESIGN.md`) + prompt-corte `docs/design/stitch/21-perfil-resumen.md`.
- Screens or states affected: pestaña Perfil, titular del bloque de cifras; claro y oscuro; variante
  de contadores 0/0.
- New design artifact required: **no**.
- **Capturas de UI — exención autorizada por la persona (issue #86, misma clase que la excepción de
  `recipe-artwork` del issue #77)**: esta feature **no** genera capturas nuevas y **no** se crea
  `docs/design/concepts/profile-stats-heading/` ni su `README.md`. Motivo: es un cambio de
  **microcopy de una sola cadena**; el layout, los tokens y las tarjetas ya están cubiertos por la
  captura aceptada de `activity-summary`, y el propio issue pide registrar la desviación en
  `DESIGN.md`.
- **Desviación explícita (dirección visual asumida)**: la captura aceptada y el prompt de Stitch
  titulan la sección **"Resumen"**; implementamos **"Estadísticas"**. `DESIGN.md` manda ante
  contradicciones y es donde queda registrada la desviación (Scenario 4). El validador **no** debe
  tratar ese desacuerdo como defecto: está autorizado y documentado.
- **`UI captures (required)`**: **ninguna nueva**. Las capturas de la dirección visual son las ya
  versionadas de `activity-summary` (`docs/design/concepts/activity-summary/perfil-resumen/`), que la
  spec **no** exige duplicar.
- Sin cambios de tokens, hex, tipografía, tamaño, espaciado ni iconos; sin estados nuevos.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace ninguna regla MUST/MUST NOT de nivel repo.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas de trabajo no cambian.
- `docs/technical-discovery.md`: **not needed** — no cambia stack, almacenamiento, integraciones ni
  camino de verificación; el titular de UI ya está descrito en `DESIGN.md`.
- `docs/domain-model.md`: **not needed** — "Resumen de actividad"/`ActivitySummary` no cambian de
  nombre ni de semántica.
- `docs/specs/activity-summary.md`: **not needed** — el Non-Goal "no se añade navegación" sigue
  vigente sin edición.
- `DESIGN.md`: **update** — registrar la desviación de microcopy ("Estadísticas" en vez de "Resumen")
  y la exención de capturas de `profile-stats-heading`, en el párrafo de excepción de
  `Generated Concept Images`, y citar el titular visible en Core Screens 7.
- `docs/design/concepts/`: **no change** — capturas exentas por decisión de la persona (issue #86).
- `PROGRESS.md` y `feature_list.json`: **update al implementar** (estado y evidencia), no en esta
  sesión.

## Implementation Plan

1. Cambiar el literal del titular en `ActivitySummarySection.kt` a `"Estadísticas"`.
2. Actualizar los KDoc que citan el titular (`ActivitySummarySection.kt`, `ProfileScreen.kt`).
3. Registrar la desviación y la exención en `DESIGN.md` (párrafo de excepción + Core Screens 7).
4. Verificar por grep que no queda ningún `"Resumen"` de microcopy del bloque de cifras y que no hay
   `clickable`/`onClick` en la sección.
5. Ejecutar `bash init.sh` en verde y anotar la evidencia en `feature_list.json`/`PROGRESS.md`.

## Implementation Tasks

- [x] Sustituir `text = "Resumen"` por `text = "Estadísticas"` en `ActivitySummarySection.kt` (línea
      ~40); nada más del `@Composable` cambia.
- [x] Actualizar el KDoc de `ActivitySummarySection.kt` y el de `ProfileScreen.kt` (línea ~37) que
      citan la sección como `"Resumen"`.
- [x] Registrar en `DESIGN.md` el párrafo de excepción (microcopy + sin capturas nuevas) y citar
      `"Estadísticas"` en Core Screens 7.
- [x] Comprobar por inspección/grep: no queda `"Resumen"` de microcopy en la sección; `Surface`/
      `Column` de las tarjetas sin `clickable`/`onClick`; sin cambios en `app/build.gradle.kts`,
      `gradle/libs.versions.toml`, dominio, `data/**` ni `docs/design/concepts/**`.
- [x] Ejecutar `bash init.sh` en verde y anotar la evidencia (test/lint + resultado manual).

## Verification Plan

- `bash init.sh` (vía `scripts/gradle.sh`) → **esperado**: exit 0 (`./gradlew test` + `./gradlew lint`),
  sin procesos largos; **no** cambia y no necesita modos nuevos. Debe seguir en verde **sin ningún test
  nuevo** (conteo exacto = el que reporte el gate; se registra el observado).
- **Honestidad sobre la cobertura automática**: el titular es un **literal de UI** y no hay lógica
  nueva que testear en JVM. La infraestructura de Compose UI del repo es **instrumentada**
  (`androidTestImplementation(libs.androidx.compose.ui.test.junit4)`, `app/src/androidTest/`), no la
  ejecuta `bash init.sh` y AGENTS.md prohíbe automatizar UI de emulador; añadir ahí un test del titular
  quedaría fuera del gate y sin forma de correrlo desde WSL. Por tanto el gate automático se limita a
  que **nada se rompe**; el titular concreto **no** queda cubierto por un test JVM y esta feature no
  añade infraestructura ni dependencias. No se admite como evidencia "un test nuevo" que no exista.
- `./gradlew lint` (vía `init.sh`) → **esperado**: 0 errores y sin warnings nuevos (los preexistentes se
  mantienen).
- **Inspección/grep** (evidencia objetiva del cambio):
  - `grep -rn '"Resumen"' app/src/main/java/com/sazon/app/ui` → **0** resultados.
  - `grep -rn 'Estadísticas' app/src/main/java/com/sazon/app/ui/profile/ActivitySummarySection.kt` → 1
    (el literal).
  - En `ActivitySummarySection.kt`: `ActivityStatCard` sigue siendo `Surface` + `Column` + `Text`, sin
    `Modifier.clickable` ni `onClick`; el fichero sigue en UTF-8 sin BOM.
  - `git diff --stat` limitado a los 4 ficheros previstos (+ `feature_list.json`/`PROGRESS.md` al
    implementar).
- **Manual (emulador, lo revisa la persona; AGENTS.md no automatiza UI ni es posible desde WSL)**:
  - Perfil: el bloque de cifras se titula **"Estadísticas"** y las tarjetas siguen sin navegación
    (pulsarlas no hace nada) — Scenario 1 y 2 (verificación exigida).
  - Revisar en claro y oscuro y con contadores poblados y 0/0: el resto del Perfil (identidad,
    preferencias, alergias, "Sugerencias") no cambia — Scenario 3.
  - Contra la captura aceptada `docs/design/concepts/activity-summary/perfil-resumen/`: todo igual
    **salvo** el titular, que es la desviación autorizada y registrada en `DESIGN.md`.
- Cobertura E2E: **no aplica**. No existe suite E2E persistente, AGENTS.md prohíbe automatizar UI de
  emulador y el cambio no toca autenticación, autorización, routing ni flujos de API.

## Evidence To Capture

- Salida de `bash init.sh` con código 0 y los conteos de `./gradlew test` (mismo nº de tests que antes,
  0 fallos, **sin tests nuevos**) y `./gradlew lint` (0 errores, sin warnings nuevos).
- Salidas de los grep del plan de verificación (0 `"Resumen"` en `ui`; 1 `"Estadísticas"`; sin
  `clickable`/`onClick` en la sección).
- `git diff --stat` dentro del alcance previsto.
- Diff de `DESIGN.md` con el párrafo de desviación/exención y Core Screens 7 citando "Estadísticas".
- Resultado manual en emulador (descripción): titular "Estadísticas" y tarjetas no navegables, en claro
  y oscuro y con 0/0.
- Anotación en `feature_list.json` de la evidencia y cambio de estado a `passing` (al implementar).

## Validator Checklist

- [ ] El único cambio de comportamiento es el titular: literal exacto **"Estadísticas"** en
      `ActivitySummarySection`; sin cambios de números, etiquetas, layout, tokens, orden ni estados
      (Scenarios 1 y 3).
- [ ] Las tarjetas siguen **sin navegación** (sin `clickable`/`onClick`/destino) y el Non-Goal "No se
      añade navegación" de `docs/specs/activity-summary.md` sigue vigente y sin editar (Scenario 2).
- [ ] `DESIGN.md` registra la desviación de microcopy y la exención de capturas de
      `profile-stats-heading`, y **no** existe `docs/design/concepts/profile-stats-heading/`
      (Scenario 4).
- [ ] No se han creado tests nuevos que aparenten cubrir el literal, ni se ha añadido infraestructura
      de Compose UI; `app/build.gradle.kts` y `gradle/libs.versions.toml` intactos.
- [ ] `bash init.sh` en verde (test + lint), sin romper ninguna suite existente y sin warnings nuevos.
- [ ] El alcance se limita a `ActivitySummarySection.kt` (+ el KDoc de `ProfileScreen.kt`) y a
      `DESIGN.md`; no se renombran identificadores de dominio ni se toca
      `docs/specs/activity-summary.md`, `docs/design/concepts/**` ni `docs/design/stitch/**`.
- [ ] La implementación sigue la dirección visual aplicable (captura aceptada de `activity-summary` +
      `DESIGN.md`) y la desviación autorizada del titular no se marca como defecto.
- [ ] `feature_list.json` y `PROGRESS.md` quedan actualizados con evidencia verificable.

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **"Estadísticas" en vez de "Resumen"**: es lo que pide el issue #86; explicita que las dos tarjetas
  son estadística informativa y evita que "favoritas" se lea como un acceso.
- **El término de dominio no se renombra**: "Resumen de actividad" (`CONTEXT.md`) sigue siendo el
  vocabulario de `domain.activity`/`data.activity`; cambiarlo sería *vocabulary drift* y alcance ajeno.
- **Sin navegación y sin reinterpretar el layout**: el malentendido se resuelve con microcopy, no
  añadiendo interacción (Non-Goal de `activity-summary`).
- **`DESIGN.md` como registro**: es la fuente de verdad y donde se anota la desviación respecto a la
  captura y al prompt de Stitch (Scenario 4), para que un validador futuro no la lea como defecto.

### Unknowns

- **Revisión visual fina**: el titular usa el mismo `titleMedium`/`onBackground` que antes y las demás
  secciones del Perfil; aun así, el encaje visual final lo revisa la persona en emulador (AGENTS.md no
  automatiza UI). Un problema de acabado se corrige con una **feature correctiva** nueva.
- **Prompt de Stitch desactualizado**: `docs/design/stitch/21-perfil-resumen.md` y la captura seguirán
  diciendo "Resumen"; **no** se editan (fuera de alcance) y la diferencia queda cubierta por la
  desviación registrada en `DESIGN.md`.
- **Conteo de tests de referencia**: la evidencia de `PROGRESS.md` muestra conteos distintos entre
  sesiones/worktrees; el implementer debe registrar el **observado** en su gate, no un número
  heredado.
