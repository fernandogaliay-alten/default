# Feature Implementation Spec: El interruptor "Generar imágenes del plato" aparece desactivado por defecto

## Source Feature

- `id`: `dish-image-settings-default-off`
- `area`: `ui` (el ajuste es visible en el Perfil), aunque el área técnica del cambio sea `data.image`
- `depends_on`: `["dish-image-settings-toggle"]` (satisfecha: `accepted`, sesión 093)
- `status`: `not_started` (en el momento de planificar)
- `source`: `feature_list.json` (entrada líneas 1473-1487)
- Petición de la persona (2026-10-14): la generación de imágenes venía **activada** por defecto
  (`DishImageSettingsPolicy.DEFAULT_ENABLED = true`) y gasta cuota de IA; quiere que el interruptor
  aparezca **desactivado** por defecto (opt-in), coherente con la mitigación de cuota de
  `docs/risks-and-open-questions.md` y con el ADR 0006.
- Verificación exigida (textual de la feature): estado inicial apagado (`DEFAULT_ENABLED = false`) →
  sin leer caché, sin generar y sin consumir tope, con el resumen del Perfil indicando que las recetas
  se muestran sin imagen; encender restaura el comportamiento actual (apagado = 0 peticiones;
  encendido = 1 y el tope se respeta) y los tests JVM que asumían el default encendido se actualizan;
  `bash init.sh` verde; sin dependencias nuevas.

## Goal

Cambiar el **estado inicial** del ajuste de imagen de plato para que, al abrir la app, el interruptor
"Generar imágenes del plato" esté **apagado** y la generación por IA sea **opt-in**: hasta que la
persona lo encienda, no se lee caché, no se genera nada, no se consume cuota ni tope, y las recetas se
muestran con la ficha/hero tipográfico (ADR 0006). Encender el interruptor restaura exactamente el
comportamiento actual (`dish-image-settings-toggle`).

El cambio es **de una constante** (`DishImageSettingsPolicy.DEFAULT_ENABLED`: `true` → `false`) más el
KDoc que la cita y los tests JVM que asumían el default encendido. **El resto del mecanismo no
cambia**: gate del interruptor en el proveedor del seam de `MainActivity`, `GatedDishImageGenerator`,
tope por sesión de 12, `DishImageSettingsCopy`, la sección `DishImageSettingsSection` del Perfil y el
reparto `imageBackground` de `MainShell`. Esto **no** es "otra feature de persistencia": el ajuste
sigue siendo **de sesión** (vida del proceso, sin `UserProfile` ni Room).

## Non-Goals

- **No se persiste nada**: ni el interruptor ni el tope. Siguen en memoria (Decisión 2 de
  `docs/specs/dish-image-settings-toggle.md`); perseguir "recordar el opt-in entre reinicios" exigiría
  dominio + Room y sería **otra** feature (correctiva), no esta.
- **No cambia ningún mecanismo aceptado**: `generatedIfEnabled`, `GatedDishImageGenerator`,
  `DishImageGenerationBudget`, `DishImageSettingsState`, `setEnabled`, `tryAcquireGeneration`,
  `MAX_GENERATIONS_PER_SESSION = 12`, `DishImageSettingsCopy` y `DishImageSettingsSection` quedan
  **intactos** (firma y comportamiento).
- **No se toca `MainActivity`, `SazonApp`, `MainShell` ni `ProfileScreen`**: el `DishImageSettings()`
  por proceso ya existe (`MainActivity` línea 145) y adopta el nuevo default sin edición; `MainShell`
  reparte `imageBackground = imageGeneration.enabled` (que pasa a `false` en el arranque, ya coherente
  con el default `false` de las tarjetas/hero) y `ProfileScreen` pinta `DishImageSettingsCopy.summary`,
  que ya devuelve `DISABLED_SUMMARY` con el interruptor apagado.
- **No cambian textos/microcopy ni el diseño visual**: `DISABLED_SUMMARY` y el resto de `Copy` ya
  existen; `DESIGN.md` solo **registra** que el arranque es apagado.
- **No hay pantalla nueva, ni navegación, ni geometría, ni tokens nuevos**; no se añaden dependencias
  ni se toca `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**` ni assets.
- **No se toca el resto de `data.image`/`data.ai.image`/`domain.image`/`ui.artwork`**: no se modifica
  ningún fichero aceptado salvo `DishImageSettings.kt` (la constante y su KDoc).
- **No se reintenta ni re-generan las tarjetas ya pintadas**: el cambio es de estado inicial; la
  re-resolución al encender sigue la Decisión 5 de la spec del hermano.

## Job Story

Cuando abro la app y no quiero gastar cuota de IA sin haberlo decidido,
quiero que el interruptor "Generar imágenes del plato" empiece apagado y ver en el Perfil que las
recetas se muestran sin imagen,
para que la generación por IA (y su coste) sea una decisión **explícita** mía y no algo por defecto.

## Users And Permissions

- **Usuario local** (único dueño del dispositivo, ADR 0002): abre la app y ve la ficha tipográfica;
  entra en **Perfil → "Imágenes del plato"** y ve el interruptor apagado y el resumen
  `"Desactivado: las recetas se muestran sin imagen."`; si lo enciende, vuelve el mecanismo actual
  (imagen de fondo + tope por sesión).
- **Desarrollador / agente**: comprueba el default y el no-consumo en **JVM**, sin red, sin clave, sin
  cuota y sin emulador.
- **Persona**: revisa en el emulador tras el merge que el arranque sale apagado y que encender reanuda;
  `DESIGN.md` es la fuente de verdad visual (AGENTS.md no automatiza UI de emulador).

## Acceptance Scenarios

### Scenario 1: el estado inicial es apagado y no pide imágenes (verificación exigida)

Given un `DishImageSettings()` construido **sin** argumentos (default de la política) y un productor
contador detrás del gate,
When se arranca (se crea el ajuste) y se pide una imagen a través de `generatedIfEnabled`,
Then `state.value == DishImageSettingsState(enabled = false, used = 0, max = 12)`,
`generatedIfEnabled { ... }` devuelve `null` con **0** invocaciones del productor (no se lee caché, no
se genera) y `tryAcquireGeneration()` devuelve `false` con `used == 0` (no consume tope).

### Scenario 2: el Perfil arranca mostrando el resumen desactivado

Given el estado inicial de Scenario 1,
When se formatea el resumen visible (`DishImageSettingsCopy.summary`),
Then devuelve `DishImageSettingsCopy.DISABLED_SUMMARY` ("Desactivado: las recetas se muestran sin
imagen.") y el contador de sesión sigue en **12 de 12** sin consumo.

### Scenario 3: encender restaura el comportamiento actual (verificación exigida)

Given un `DishImageSettings()` con el default apagado,
When se llama a `setEnabled(true)`,
Then `generatedIfEnabled { ... }` **sí** produce (1 invocación) y el tope por sesión vuelve a
imponerse igual que hoy (con `max = 2`: dos reservas `true`, la tercera `false` sin invocar al
delegado); apagar/encender **no** reinicia un tope agotado.

### Scenario 4: la constante vive en un único sitio

Given `DishImageSettingsPolicy`,
When se inspecciona,
Then `DEFAULT_ENABLED == false` y `MAX_GENERATIONS_PER_SESSION == 12` (sin literales duplicados): el
arranque apagado se cambia en ese único punto.

### Scenario 5: el resto del mecanismo no cambia (inspección)

Given la implementación terminada,
When se inspecciona el diff,
Then solo `data/image/DishImageSettings.kt` cambia en `app/src/main` (constante + KDoc), **no** hay
diffs en `MainActivity.kt`, `ui/SazonApp.kt`, `ui/MainShell.kt`, `ui/profile/**`, `ui/artwork/**`,
`data/image/DishImageSettingsCopy`/`DishImageGenerationBudget`, `data/ai/image/**`, `domain/**`,
`ui/theme/**`, build/lockfile, `init.sh` ni `scripts/**`; y no hay dependencias nuevas.

## Repository Research

### Files Inspected

- `feature_list.json` — entrada `dish-image-settings-default-off` (líneas 1473-1487: `area: ui`,
  `depends_on: ["dish-image-settings-toggle"]`, `status: not_started`, `verification`, `notes`) y
  entrada `dish-image-settings-toggle` (líneas 1021-1035, hoy `accepted`).
- `app/src/main/java/com/sazon/app/data/image/DishImageSettings.kt` — **el único fichero de
  `app/src/main` que cambia**: `DishImageSettingsPolicy.DEFAULT_ENABLED = true` (línea 19) con el KDoc
  "la generacion viene activada (comportamiento actual)" (línea 18); `DishImageSettings` (líneas
  68-106) toma `enabled: Boolean = DishImageSettingsPolicy.DEFAULT_ENABLED` (línea 69); el KDoc de la
  clase cita la constante (línea 53). `DishImageSettingsState`, `setEnabled`, `tryAcquireGeneration`,
  `generatedIfEnabled` y `GatedDishImageGenerator` no cambian.
- `app/src/test/java/com/sazon/app/data/image/DishImageSettingsTest.kt` — 16 tests; **11** construyen
  `DishImageSettings()`/`DishImageSettings(budget = …)` sin `enabled` y asumen el default encendido
  (líneas 67, 97, 115, 124, 135, 154, 192, 213, 230, 245, 265), más la aserción
  `assertTrue(DishImageSettingsPolicy.DEFAULT_ENABLED)` (línea 185). Detalle exacto en
  `Technical Approach` §2.
- `app/src/test/java/com/sazon/app/ui/artwork/RecipeBackgroundGateTest.kt` — **6** construcciones, todas
  con `enabled` **explícito** (líneas 37, 53, 74, 105, 118, 134); **no** depende del default → sin
  cambios.
- `app/src/test/java/com/sazon/app/ui/profile/DishImageSettingsCopyTest.kt` — **8** tests que reciben
  `DishImageSettingsState` construido a mano (no tocan la política) → sin cambios.
- `app/src/test/java/com/sazon/app/data/image/DishImageGenerationBudgetTest.kt` — no usa
  `DishImageSettings` ni la política → sin cambios.
- `app/src/main/java/com/sazon/app/MainActivity.kt` (líneas 141-145 y 208) — `DishImageSettings()` por
  proceso (línea 145) y el proveedor del seam `dishImageSettings.generatedIfEnabled { … }` (línea 208);
  adopta el nuevo default **sin edición**.
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` (líneas 146-147, 241) —
  `val imageBackground = imageGeneration.enabled` y `onImageGenerationChange = { dishImageSettings.setEnabled(it) }`:
  con el nuevo default, el arranque reparte `imageBackground = false`, ya coherente con el default
  `false` de `RecipeCard`/`RecipeFicha`/`RecipeDetailScreen` (ADR 0006).
- `app/src/main/java/com/sazon/app/ui/profile/DishImageSettingsCopy.kt` (líneas 26 y 36-42) —
  `DISABLED_SUMMARY` y `summary(state)` ya cubren `!state.enabled` (manda el interruptor aunque quede
  tope) → el resumen de arranque sale correcto sin tocar nada.
- `app/src/main/java/com/sazon/app/ui/profile/DishImageSettingsSection.kt` y `ProfileScreen.kt` — la
  sección/estados ya existen; el `Switch` refleja `state.enabled` → aparece apagado en el arranque.
- `app/src/test/java/com/sazon/app/ui/artwork/` y `ui/profile/` — baseline de suites para el delta.
- `DESIGN.md` — "Imágenes del plato" (líneas 450-459), Core Screen 7 Perfil (líneas 512-519) y
  "Cuándo hay imagen" (líneas 365-368): registran el interruptor pero **no** que el arranque sea
  apagado (y el ADR 0006 ya dice "sin red ni cuota por defecto", línea 120).
- `docs/adr/0005-ai-dish-images.md` (líneas 157-174) — cita literal `DEFAULT_ENABLED = true` (línea
  169).
- `docs/technical-discovery.md` (líneas 785-797, y 801) — cadena en runtime y "al reiniciar vuelve a
  activado" (líneas 793-795).
- `docs/risks-and-open-questions.md` (línea 147) — el gasto queda "de facto opt-in" desde el ADR
  0006, pero el arranque seguía pagando si el usuario no tocaba nada.
- `docs/adr/0006-typographic-recipe-cards.md` (líneas 74, 86-89, 120) — el interruptor se conserva y
  "sin red ni cuota por defecto".
- `docs/specs/dish-image-settings-toggle.md` — spec aceptada del mecanismo (Decisiones 1-10); este
  spec la respeta y solo invierte el default.

### Existing Patterns To Follow

- **Política en un único sitio** (`AiProviderPolicy`, `DishImageCachePolicy`, `DishImagePolicy`): el
  default del ajuste se cambia en `DishImageSettingsPolicy`, no en llamantes.
- **Composición explícita sin DI**: `MainActivity` construye el ajuste y `SazonApp`/`MainShell`
  reparten por parámetro; no hay que editar el cableado para cambiar el default.
- **Microcopy puro y determinista** (`DishImageSettingsCopy`): el resumen visible se deriva solo del
  `DishImageSettingsState`; con `enabled = false` ya sale `DISABLED_SUMMARY`.
- **Test JUnit 4** en `app/src/test/java/com/sazon/app/...`, nombres en español con `_`, sin red ni
  emulador.
- **Verificación canónica** `bash init.sh` (`./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`); **nunca** `./gradlew` directo ni `./gradlew --stop`.

### Current Gaps

- El KDoc de `DishImageSettingsPolicy.DEFAULT_ENABLED` (línea 18) describe el comportamiento **al
  revés** ("la generacion viene activada (comportamiento actual)"): hay que reescribirlo a opt-in.
- Los tests JVM del hermano **asumen** el default encendido: sin actualizarlos, el gate se pone rojo
  con el nuevo default (no hay ningún test que fije el arranque apagado).
- `DESIGN.md`, el ADR 0005, `docs/technical-discovery.md` y `docs/risks-and-open-questions.md`
  describen el arranque activado/activo: quedan **desfasados** tras el cambio.

## Technical Approach

### 1. La constante y su KDoc — `data/image/DishImageSettings.kt` (modificar)

```kotlin
object DishImageSettingsPolicy {
    /**
     * Estado inicial del interruptor: la generacion viene **apagada** (opt-in). Hasta que el usuario
     * lo encienda no se lee cache, no se genera ni se consume tope (ADR 0006: sin red ni cuota por
     * defecto); encenderlo restaura el comportamiento de `dish-image-settings-toggle`.
     */
    const val DEFAULT_ENABLED = false
    const val MAX_GENERATIONS_PER_SESSION = 12   // sin cambios
}
```

Ajustar el KDoc de la clase `DishImageSettings` (líneas 49-67) si procede para dejar claro que el
arranque es **apagado/opt-in** (sigue citando la constante, no un literal). No cambia ninguna firma ni
el cuerpo de `setEnabled`/`tryAcquireGeneration`/`generatedIfEnabled`/`GatedDishImageGenerator`.

**Consecuencia vinculante** (se propaga sola, sin editar llamantes): `MainActivity` línea 145 crea
`DishImageSettings()` → `enabled = false`; `generatedIfEnabled` devuelve `null` sin leer caché ni
generar; `tryAcquireGeneration()` devuelve `false` sin tocar el contador (tope en 12);
`MainShell` reparte `imageBackground = false` (ficha/hero tipográficos); `ProfileScreen` pinta
`DishImageSettingsCopy.DISABLED_SUMMARY`.

### 2. Tests JVM a actualizar — `data/image/DishImageSettingsTest.kt` (modificar)

Lista **exacta** verificada en el fichero. **11** tests construyen el ajuste sin `enabled` y se rompen
con el default apagado; hay que hacerlos **explícitos** (añadir `enabled = true`) para que sigan
probando el mismo mecanismo encendido:

1. `apagar_el_interruptor_tras_producir_corta_la_siguiente_peticion` (L66-76, `DishImageSettings()` en
   L67) → `DishImageSettings(enabled = true)`.
2. `apagar_y_encender_no_reinicia_el_tope_agotado` (L95-109, `DishImageSettings(budget = …)` en
   L97-99) → añadir `enabled = true` (el test asevera que la primera reserva pasa).
3. `una_reserva_re_emite_el_estado_con_el_consumo_actualizado` (L122-131, L124) → `enabled = true`.
4. `el_estado_es_un_state_flow_observable` (L133-148, L135) → `enabled = true` (espera primera emisión
   `(true, 0, 12)` y consumo con `enabled`).
5. `el_decorador_agotado_no_invoca_al_delegado` (L152-169, L154) → `enabled = true`.
6. `un_acierto_de_cache_no_consume_tope` (L190-208, L192) → `enabled = true`.
7. `un_acierto_de_disco_tampoco_consume_tope` (L210-224, L213) → `enabled = true`.
8. `un_fallo_del_proveedor_degrada_a_null_y_consume_la_unidad` (L228-241, L230) → `enabled = true`.
9. `la_cancelacion_del_generador_se_propaga_y_consume_la_unidad` (L243-261, L245) → `enabled = true`.
10. `el_decorador_no_convierte_la_cancelacion_en_null` (L263-280, L265) → `enabled = true`.

Además, **invertir** el test del estado inicial y `DEFAULT_ENABLED`:

11. `el_estado_inicial_es_activado_sin_consumo_y_con_el_tope_de_la_politica` (L113-120): renombrar a
    `el_estado_inicial_es_apagado_sin_consumo_y_con_el_tope_de_la_politica` y esperar
    `DishImageSettingsState(enabled = false, used = 0, max = 12)` (y `remaining = 12`,
    `exhausted = false`).
12. `la_constante_del_tope_vive_en_un_sitio` (L182-186): la aserción
    `assertTrue(DishImageSettingsPolicy.DEFAULT_ENABLED)` pasa a
    `assertFalse(DishImageSettingsPolicy.DEFAULT_ENABLED)`; `MAX_GENERATIONS_PER_SESSION == 12`
    intacta.

Los tests que **ya** usan `enabled` explícito (`interruptor_apagado_no_pide_ninguna_imagen_ni_consume_tope`
L36, `interruptor_encendido_permite_producir_una_vez` L52, `el_interruptor_apagado_no_consume_tope_ni_reserva`
L81, `el_decorador_apagado_no_invoca_al_delegado` L172) **no** cambian.

**Test nuevo recomendado** (cubre el `verification` "el estado inicial … sin leer caché, sin generar y
sin consumir tope" de forma explícita y autoexplicativa, sin depender del rename del punto 11):
`el_estado_inicial_es_apagado_y_no_pide_imagenes` — con `DishImageSettings()` (sin argumentos) y un
productor contador: `state.value == (false, 0, 12)`, `generatedIfEnabled { … }` devuelve `null` con
**0** invocaciones y `tryAcquireGeneration()` devuelve `false` con `used == 0`. Es un caso negativo
que **no** pasa en vacío (con el default actual `true` fallaría en las tres aserciones).

### 3. Docs durables (registro del opt-in)

Ver `Durable Documentation Impact`. El implementer registra que el interruptor aparece **desactivado**
al arrancar, siguiendo estos puntos:

- `DESIGN.md`: en la sección "Imágenes del plato" (líneas 450-459) y/o Core Screen 7 (líneas 512-519),
  dejar escrito que el ajuste **aparece desactivado por defecto** (opt-in) y que el resumen de arranque
  es "Desactivado: las recetas se muestran sin imagen."; ajustar "Cuándo hay imagen" (líneas 365-368)
  si conviene aclarar que el arranque es sin imagen.
- `docs/adr/0005-ai-dish-images.md` (línea 169): `DEFAULT_ENABLED = false` + una línea de por qué
  (opt-in: sin cuota hasta que el usuario lo encienda).
- `docs/technical-discovery.md` (líneas 793-795): `DEFAULT_ENABLED = false` y "al reiniciar vuelve a
  **apagado**".
- `docs/risks-and-open-questions.md` (línea 147): reflejar que el opt-in es ahora el **default** del
  arranque (no solo "de facto"): apagado no se paga cuota ni se espera latencia.

## Expected File Changes

- `app/src/main/java/com/sazon/app/data/image/DishImageSettings.kt` — **modificar**; `DEFAULT_ENABLED`
  `true` → `false` en `DishImageSettingsPolicy` + KDoc de la política (y, si procede, del ajuste) a
  opt-in. Nada más del fichero cambia.
- `app/src/test/java/com/sazon/app/data/image/DishImageSettingsTest.kt` — **modificar**; los 11 tests
  listados pasan a `enabled = true` explícito, el test del estado inicial se invierte (apagado) y la
  aserción de `DEFAULT_ENABLED` pasa a `false`; añadir
  `el_estado_inicial_es_apagado_y_no_pide_imagenes`.
- `DESIGN.md` — **modificar**; registrar que el interruptor aparece desactivado al arrancar (sección
  "Imágenes del plato" / Core Screen 7 / "Cuándo hay imagen").
- `docs/adr/0005-ai-dish-images.md` — **modificar**; `DEFAULT_ENABLED = false` (opt-in).
- `docs/technical-discovery.md` — **modificar**; constante y "al reiniciar vuelve a apagado".
- `docs/risks-and-open-questions.md` — **modificar**; el opt-in pasa a ser el default de arranque.
- `PROGRESS.md` y `feature_list.json` — **actualizar al implementar/validar** (estado y evidencia);
  **no** los toca el planner.

**No se toca** (verificado sin dependencia del default): `MainActivity.kt`, `ui/SazonApp.kt`,
`ui/MainShell.kt`, `ui/profile/ProfileScreen.kt`, `ui/profile/DishImageSettingsSection.kt`,
`ui/profile/DishImageSettingsCopy.kt`, `ui/artwork/**`, los demás ficheros aceptados de `data/image/**`
y `data/ai/image/**`, `domain/**`, `ui/theme/**`, `RecipeBackgroundGateTest.kt`,
`DishImageSettingsCopyTest.kt`, `DishImageGenerationBudgetTest.kt`, `app/build.gradle.kts`,
`gradle/libs.versions.toml`, `init.sh`, `scripts/**`, assets y la Taxonomía.

## Visual Design Impact

- UI involved: **sí**, pero **sin pantalla, layout, geometría ni tokens nuevos**: solo cambia el
  **estado inicial** del interruptor y del resumen ya existentes en la pantalla Perfil.
- Design source: `DESIGN.md` es la fuente de verdad visual (typography, `SazonSpacing`, Accessibility
  Baseline) y el estilo de sección ya aceptado del Perfil
  (`docs/design/concepts/activity-summary/perfil-resumen/`, sección "Estadísticas", y el patrón de
  `ProfileSuggestionsSection`).
- Screens or states affected: pestaña **Perfil → "Imágenes del plato"**: el estado de arranque pasa a
  ser **interruptor apagado + resumen desactivado**; encender sigue mostrando los estados de tope
  disponible/reanudado de hoy. Claro y oscuro por roles del tema (sin cambios de estilo).
- New design artifact required: **no** — la feature no crea pantalla ni cambia el diseño: reutiliza la
  sección aceptada y el microcopy existentes.

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — `DEFAULT_ENABLED = false` en la cadena en runtime
  (líneas 793-795) y "al reiniciar vuelve a **apagado**" (opt-in).
- `docs/adr/0005-ai-dish-images.md`: **update** — la constante de la fase 4 pasa a `false` y se
  documenta el opt-in por defecto (sin cambiar el resto de la semántica del ajuste).
- `DESIGN.md`: **update** — registrar que el interruptor aparece **desactivado** al arrancar y que el
  resumen inicial es el desactivado.
- `docs/risks-and-open-questions.md`: **update** — el opt-in del gasto de cuota pasa a ser el default
  de arranque (no solo "de facto").
- `docs/domain-model.md`: **not needed** — no hay entidades, relaciones ni estados nuevos; el ajuste no
  entra en `UserProfile` ni en la Firma de preferencias.
- `AGENTS.md`: **not needed** — no cambian flujo, arranque ni reglas de trabajo.
- `docs/adr/0006-typographic-recipe-cards.md`: **not needed** — ya dice "sin red ni cuota por defecto";
  esta feature lo hace literal sin contradecirlo.
- `docs/specs/dish-image-settings-toggle.md`: **not needed** — spec aceptada; su mecanismo se respeta
  (solo se invierte el default, documentado aquí).

## Implementation Plan

1. Editar `DishImageSettingsPolicy.DEFAULT_ENABLED` a `false` y reescribir su KDoc a opt-in; revisar el
   KDoc de `DishImageSettings` para que el arranque quede claro. No tocar ninguna firma.
2. Actualizar `DishImageSettingsTest`: añadir `enabled = true` a los 11 tests listados, invertir el test
   del estado inicial (apagado) y la aserción de `DEFAULT_ENABLED` (`assertFalse`); añadir el test nuevo
   del default apagado y sin peticiones.
3. Ejecutar la verificación focal de `data.image.*` y `ui.profile.*` para confirmar que no hay
   regresión fuera de lo previsto.
4. Actualizar `DESIGN.md`, `docs/adr/0005-ai-dish-images.md`, `docs/technical-discovery.md` y
   `docs/risks-and-open-questions.md`.
5. Inspeccionar el diff (`git diff --name-only`) para confirmar que **solo** cambian
   `data/image/DishImageSettings.kt`, su test y las 4 docs (más estado/evidencia).
6. Ejecutar `bash init.sh` en verde, registrar la evidencia y dejar la feature `passing` (la validación
   independiente es de otro rol; **sin** commit, push ni PR, los hace el orquestador).

## Implementation Tasks

- [ ] `DishImageSettingsPolicy.DEFAULT_ENABLED = false` + KDoc de la política (y del ajuste) a opt-in;
      `MAX_GENERATIONS_PER_SESSION = 12` **sin cambios**.
- [ ] `DishImageSettingsTest`: `enabled = true` explícito en `apagar_el_interruptor_tras_producir_corta_la_siguiente_peticion`,
      `apagar_y_encender_no_reinicia_el_tope_agotado`,
      `una_reserva_re_emite_el_estado_con_el_consumo_actualizado`,
      `el_estado_es_un_state_flow_observable`, `el_decorador_agotado_no_invoca_al_delegado`,
      `un_acierto_de_cache_no_consume_tope`, `un_acierto_de_disco_tampoco_consume_tope`,
      `un_fallo_del_proveedor_degrada_a_null_y_consume_la_unidad`,
      `la_cancelacion_del_generador_se_propaga_y_consume_la_unidad` y
      `el_decorador_no_convierte_la_cancelacion_en_null`.
- [ ] `DishImageSettingsTest`: invertir `el_estado_inicial_es_activado…` a
      `el_estado_inicial_es_apagado_sin_consumo_y_con_el_tope_de_la_politica` (espera `(false, 0, 12)`)
      y cambiar `assertTrue(DEFAULT_ENABLED)` → `assertFalse` en `la_constante_del_tope_vive_en_un_sitio`.
- [ ] Añadir `el_estado_inicial_es_apagado_y_no_pide_imagenes` (default apagado → `null` con 0
      invocaciones, `tryAcquireGeneration()` `false`, `used == 0`).
- [ ] Verificar por inspección que `RecipeBackgroundGateTest`, `DishImageSettingsCopyTest` y
      `DishImageGenerationBudgetTest` quedan **sin diff** y que `MainActivity`/`MainShell`/`ProfileScreen`
      no cambian.
- [ ] Actualizar `DESIGN.md`, `docs/adr/0005-ai-dish-images.md`, `docs/technical-discovery.md` y
      `docs/risks-and-open-questions.md`.
- [ ] Ejecutar `bash init.sh` (test + lint) en verde, registrar la evidencia y actualizar
      `feature_list.json` (a `passing`, **sin tocar su `verification`**) y `PROGRESS.md`.

## Verification Plan

- `bash init.sh` → **exit 0**. Ejecuta el gate no bloqueante estándar (`./gradlew test` + `./gradlew
  lint` vía `scripts/gradle.sh`), **no** arranca procesos largos y **no** modifica `init.sh`.
- Tiradas focales:
  - `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.image.*"` →
    `BUILD SUCCESSFUL` (las 7 suites de la fase 3/4 sin regresión + el caso nuevo).
  - `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.profile.*"` y
    `--tests "com.sazon.app.ui.artwork.*"` → `BUILD SUCCESSFUL` (sin cambios en esos tests).
- Parseo propio de los XML (`app/build/test-results/testDebugUnitTest/TEST-*.xml`): **0 fallos, 0
  errores, 0 saltados**. Baseline de referencia: **84 suites / 593 tests** (validación de
  `dish-image-settings-toggle`, sesión 093); el implementer **re-deriva** el número exacto en su HEAD
  (`git ls-tree` + `git grep -c '@Test'`) y confirma el delta: **+1 test** (el caso nuevo del default) y
  **0 suites nuevas**.
- `bash scripts/gradle.sh lint` → **0 errores**; sin warnings nuevos bajo `app/src` (los
  `GradleDependency`/`NewerVersionAvailable`/`AndroidGradlePluginVersion`/`OldTargetApi` son
  preexistentes y su recuento varía con la metadata de versiones del momento).
- Mapeo explícito del `verification` de la feature:
  - "El estado inicial del ajuste es apagado (`DEFAULT_ENABLED = false`): con el interruptor apagado no
    se lee caché, no se genera ni se consume tope" → Scenario 1 / tests
    `el_estado_inicial_es_apagado_sin_consumo_y_con_el_tope_de_la_politica` +
    `el_estado_inicial_es_apagado_y_no_pide_imagenes` + `assertFalse(DEFAULT_ENABLED)`.
  - "el resumen visible del Perfil indica que las recetas se muestran sin imagen" → Scenario 2 (puro):
    `DishImageSettingsCopy.summary(state(enabled = false, …)) == DISABLED_SUMMARY` (ya cubierto por
    `DishImageSettingsCopyTest.con_el_interruptor_apagado_…`, sin cambios).
  - "Encender el interruptor sigue habilitando la generación y el tope … apagado = 0 peticiones;
    encendido = 1 y el tope se respeta" → Scenario 3 (los 11 tests con `enabled = true` explícito).
  - "los tests JVM que asumían el default encendido se actualizan" → diff del test (§2).
  - "`bash init.sh` verde; sin dependencias nuevas" → gate completo + `git diff` sin cambios de build.
- Inspección de alcance:
  - `git diff --name-only` **solo** con `data/image/DishImageSettings.kt`, su test y las 4 docs
    previstas; **0** cambios en `MainActivity.kt`, `ui/**`, los demás ficheros aceptados de
    `data/image`/`data/ai/image`, `domain/**`, build, lockfile, `init.sh` y `scripts/**`.
  - `grep -rn "DEFAULT_ENABLED = true" app/src` → **0**; `grep -rn "activado\|activada" DESIGN.md
    docs/adr/0005-ai-dish-images.md docs/technical-discovery.md` → sin descripciones del arranque
    activado.
- Cobertura **E2E**: **no aplica**. El repo no tiene suite E2E persistente y AGENTS.md no automatiza UI
  de emulador; la cobertura canónica es el test JVM (el default y el no-consumo viven en `data.image`)
  y la revisión visual/manual de la persona contra `DESIGN.md`.
- **Prueba manual (persona, tras el merge; no automatizable desde WSL)**: en el emulador, abrir la app
  con clave válida y (a) comprobar que las recetas salen con la ficha/hero tipográficos **sin** pedir
  imágenes y que Perfil → "Imágenes del plato" muestra el interruptor **apagado** con "Desactivado: las
  recetas se muestran sin imagen." y el contador en 12 de 12; (b) **encender** y verificar que se
  reanuda (aparecen imágenes y baja el contador); (c) reiniciar la app y comprobar que vuelve a
  apagado y a 12 (sesión = proceso); (d) revisar claro y oscuro el interruptor/resumen contra
  `DESIGN.md`.
- Nota operativa: usar `bash init.sh` o `bash scripts/gradle.sh <tarea>`; **nunca** `./gradlew`
  directo ni `./gradlew --stop` (AGENTS.md, Notas de entorno).

## Evidence To Capture

- Salida de `bash init.sh` con código 0 y de las tiradas focales (`data.image.*`, `ui.profile.*`,
  `ui.artwork.*`).
- Conteo parseado del XML (suites/tests, 0 fallos/errores/saltados) con la baseline re-derivada en HEAD
  y el delta (≈ +1 test, 0 suites nuevas).
- Informe de lint (0 errores, ninguno nuevo bajo `app/src`).
- Vectores concretos del default: `DishImageSettings()` → `state.value == (false, 0, 12)`,
  `generatedIfEnabled { … }` → `null` con **0** invocaciones del productor y `used == 0`;
  `tryAcquireGeneration()` → `false` sin consumo; `setEnabled(true)` → **1** invocación y el tope se
  impone (con `max = 2`: `true, true, false` sin invocar al delegado agotado); `assertFalse(
  DishImageSettingsPolicy.DEFAULT_ENABLED)` y `MAX_GENERATIONS_PER_SESSION == 12`.
- Resultado de los greps de alcance (0 cambios fuera de lo previsto; 0 `DEFAULT_ENABLED = true`) y del
  `git diff --stat`.
- Confirmación de que el implementer **no** hizo commit, push ni PR (los hace el orquestador).

## Decisions And Unknowns

### Decisiones cerradas (el implementer no las reabre)

1. **El cambio es una constante**: `DishImageSettingsPolicy.DEFAULT_ENABLED` pasa de `true` a `false`
   en `data/image/DishImageSettings.kt`. **No** se toca ninguna firma ni el resto del mecanismo; el
   nuevo default se propaga solo porque `DishImageSettings()` (el que construye `MainActivity`) lee la
   constante y `MainShell`/`ProfileScreen` ya derivan de `state`/`summary`.
2. **Sigue siendo de sesión (sin persistencia)**: el opt-in **no** se recuerda entre reinicios; al
   reiniciar, el interruptor vuelve a **apagado** (nuevo default) y el contador a 12. Persistirlo
   exigiría dominio + Room (otra feature). Es coherente con la Decisión 2 de
   `docs/specs/dish-image-settings-toggle.md`.
3. **`MainActivity` no se edita**: su `DishImageSettings()` ya adopta el default de la política; el
   cableado (gate del seam + decorador del tope) es el mismo.
4. **El resumen y el reparto `imageBackground` no se tocan**: `DishImageSettingsCopy.summary` ya
   devuelve `DISABLED_SUMMARY` con `enabled = false`, y `MainShell` reparte `imageBackground = false`
   en el arranque, ya coherente con el default `false` de las tarjetas/hero (ADR 0006).
5. **Los tests con `enabled` explícito no cambian**: `RecipeBackgroundGateTest` (6 construcciones,
   todas explícitas), `DishImageSettingsCopyTest` (estado construido a mano) y
   `DishImageGenerationBudgetTest` no dependen del default.
6. **Los tests del hermano que asumían el default encendido se hacen explícitos** (`enabled = true`) en
   vez de borrarse o relajarse: el mecanismo (apagado corta, encendido produce, tope se respeta,
   cancelación propaga) debe seguir cubierto con el interruptor encendido.
7. **El test del estado inicial se invierte** (no se duplica a ciegas): el nombre y la aserción pasan a
   "apagado", y se añade un caso explícito del arranque sin peticiones para anclar el `verification`.
8. **Sin dependencias nuevas, sin `init.sh` ni build**: el cambio es de lógica pura ya presente; el
   microcopy y la sección de Perfil no se tocan.
9. **El área sigue siendo `ui`** porque el efecto visible es el ajuste del Perfil, aunque el fichero
   editado viva en `data.image`.

### Unknowns y riesgos (no bloquean, quedan anotados)

- **La constante sigue viva en un único sitio**: el KDoc de `DishImageSettings` cita la constante (no
  un literal), así que el implementer solo debe reescribir el KDoc de la **política**; si al revisar el
  KDoc de la clase quedara frase que implique "activado por defecto", se ajusta sin cambiar código.
- **Delta de tests**: la lista de 11 tests + 1 aserción se verificó sobre el fichero actual; si el
  implementer encuentra el fichero con otras líneas (otra rama/worktree), re-deriva la lista por
  `grep -n "DishImageSettings("` y actualiza **todos** los que no pasen `enabled` explícito.
- **Otros textos de docs que dicen "activado"**: se reconcilian las 4 docs previstas; si aparece otro
  punto que afirme el arranque activado (p. ej. un comentario en `MainActivity`), se ajusta solo si es
  una afirmación de comportamiento (no código).
- **Acabado visual del arranque** (interruptor apagado y resumen): lo revisa la persona en el
  emulador contra `DESIGN.md`; un defecto se corrige con una **feature correctiva** (no bloquea el
  flujo autónomo).
- **Tope de sesión seguía siendo 12 con el interruptor apagado**: el contador arranca en 12 y no
  consume (Scenario 1); al encender, el tope se aplica desde ahí (comportamiento actual).

## Validator Checklist

- [ ] Alcance: solo `data/image/DishImageSettings.kt` (constante + KDoc), su test y las 4 docs
  previstas; **sin** tocar `MainActivity.kt`, `ui/**`, los demás aceptados de
  `data/image`/`data/ai/image`, `domain/**`, build, lockfile, `init.sh` ni dependencias.
- [ ] `DishImageSettingsPolicy.DEFAULT_ENABLED == false` y `MAX_GENERATIONS_PER_SESSION == 12`
  (constantes en un único sitio); el KDoc de la política ya **no** dice que la generación viene
  activada.
- [ ] `DishImageSettings()` (default) → `state.value == (false, 0, 12)`; `generatedIfEnabled` devuelve
  `null` con **0** invocaciones (no se lee caché ni se genera) y `tryAcquireGeneration()` es `false`
  sin consumo de tope (Scenario 1, verificación exigida).
- [ ] El Perfil de arranque muestra el interruptor apagado y el resumen
  `DishImageSettingsCopy.DISABLED_SUMMARY` con el tope en 12 de 12 (Scenario 2).
- [ ] Encender (`setEnabled(true)`) restaura el comportamiento: 1 invocación y tope por sesión
  respetado; apagado = 0 peticiones; apagar/encender no reinicia el tope (Scenario 3).
- [ ] Los **11** tests que asumían el default encendido pasan con `enabled = true` explícito y la
  aserción `assertTrue(DEFAULT_ENABLED)` pasa a `assertFalse`; el test del estado inicial se invierte y
  existe un caso explícito del default apagado sin peticiones.
- [ ] `RecipeBackgroundGateTest`, `DishImageSettingsCopyTest` y `DishImageGenerationBudgetTest` están
  **sin diff** (no dependen del default).
- [ ] `bash init.sh` (test + lint) verde; **0 fallos/errores/saltados**; sin dependencias nuevas.
- [ ] `DESIGN.md`, `docs/adr/0005-ai-dish-images.md`, `docs/technical-discovery.md` y
  `docs/risks-and-open-questions.md` registran el arranque **apagado/opt-in** y son coherentes con el
  código.
- [ ] `feature_list.json` y `PROGRESS.md` reflejan estado y evidencia; el `verification` de la feature
  queda **intacto** y el implementer no hizo commit, push ni PR.
