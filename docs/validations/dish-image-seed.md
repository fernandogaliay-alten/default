# Validación independiente — `dish-image-seed`

- Feature id: `dish-image-seed`
- Spec: `docs/specs/dish-image-seed.md`
- Rama/worktree: `feature/dish-image-seed`
  (`/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/dish-image-seed`)
- Rol: `validator` (contexto limpio; no implementa ni acepta en ficheros)
- Fecha: 2026-10-13
- Estado en `feature_list.json` al validar: `passing` (autoverificado por el implementer, sesión 096)
- Veredicto: **accept**

## Alcance validado

Fase 7 (última) de `docs/ai-dish-images-proposal.md` §5, `area: data`. Los platos frecuentes del demo
viajan preparados **una vez** y **versionados** como assets WebP
(`app/src/main/assets/img/dishes/seed/<cacheKey>.<ext>`) y se resuelven por la **clave canónica** del
plato (`DishImageCacheKey`) en un nivel nuevo de `DishImageResolver` **entre el disco y el generador**
(`memoria → disco → semilla → generador → persistir`). Sin red, sin cuota y sin esperar generación; la
semilla se cachea en memoria y **no** se escribe en `cacheDir`. No es UI: el gate de capturas
`docs/design/concepts/<feature-id>/` no aplica y **no** existe.

## Verificación re-ejecutada (independiente)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (test + lint vía `scripts/gradle.sh`). `testDebugUnitTest` y `lint` salieron `UP-TO-DATE`; se forzó rerun. |
| Tests (forzado) | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL** (3m 52s; 34 tareas ejecutadas). |
| Lint (forzado) | `bash scripts/gradle.sh lint --rerun-tasks` | **BUILD SUCCESSFUL** (3m 01s; 30 tareas ejecutadas); SARIF regenerado. |
| Parseo XML propio | `app/build/test-results/testDebugUnitTest/TEST-*.xml` (frescos) | **90 suites / 660 tests / 0 fallos / 0 errores / 0 saltados**. |
| Parseo SARIF propio | `app/build/reports/lint-results-debug.sarif` | **0 errores**; 13 warnings preexistentes (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1), **0 bajo `app/src`**. |
| APK | `bash scripts/gradle.sh assembleDebug` + `unzip -l` + extracción | 6 entradas `assets/img/dishes/seed/*.webp` (**Stored**, sin comprimir); el `md5` de cada entrada coincide con el del fichero fuente del árbol. |

Suites focales reportadas por el XML: `DishImageSeedAssetsTest` 6, `AssetDishImageSeedSourceTest` 4,
`DishImageResolverTest` 19.

### Baseline y delta re-derivados (independientes)

`git grep -c '@Test' HEAD -- app/src/test` → **88 ficheros / 643 anotaciones** (la misma última baseline
aceptada, sesión 095). Árbol de trabajo: **90 suites / 660 tests** → delta **+2 suites / +17 tests**,
exactamente `DishImageSeedAssetsTest` (6, nueva) + `AssetDishImageSeedSourceTest` (4, nueva) +
`DishImageResolverTest` (12 → 19, +7). 643 + 17 = 660.

## Escenarios de la spec

- **1** (clave canónica → asset): el test deriva cada clave con `DishImageCacheKey.of(título)` **real**
  (no hardcodeada): `DishImageCacheKey.of("Arroz con pollo criollo") ==
  "dish-image-v1-arroz-con-pollo-criollo"` resuelve `img/dishes/seed/dish-image-v1-arroz-con-pollo-criollo.webp`
  existente y no vacío; extensión probada en orden `webp → jpg → png`.
- **2/3** (semilla gana al generador, 2ª resolución no relee): bytes del asset con **0** llamadas al
  generador y **0** ficheros en la raíz de la caché; la fuente semilla se lee **una sola vez**.
- **4** (precedencia `memoria → disco → semilla`): con copia en disco se sirve del disco sin leer la
  semilla; sin copia, del asset sin escribir en disco.
- **5** (fuera del set): `DishImageCacheKey.of("Plato que no existe")` → `null` y la cadena sigue igual
  que la fase 3 (no es comodín).
- **6** (fallo silencioso): `null`/`[]`/`IllegalStateException` degradan sin romper;
  `CancellationException` se **relanza**.
- **7** (`seed = null`): comportamiento idéntico al de la fase 3; sus tests siguen verdes.
- **8** (saneado): `""`, espacios, mayúsculas, `..`, `/`, NUL → `null`, nunca una ruta fuera de `ROOT`.
- **9** (costura Android, Robolectric `@Config(sdk = [33])`): asset real → bytes no vacíos con cabecera
  real (WebP/JPEG/PNG); clave fuera del set / clave insegura / `root` inexistente → `null` sin lanzar.
- **10** (versionado y empaquetado): **6 WebP reales 960×960, 449.844 B (439 KiB ≤ 2 MiB)**, ninguno
  vacío, **no** ignorados por git y presentes en el APK.
- **11** (script de una sola vez e idempotente): 0 referencias en `init.sh`; segunda pasada sin `--force`
  omite los 6 (`= <clave> (ya empaquetado, se omite)`), exit 0, md5 intactos y sin cuota; `--source`
  incompleto → exit ≠ 0 sin escribir nada; asset ajeno / vacío / no-imagen → exit ≠ 0.
- **12** (alcance y pureza): solo los ficheros previstos cambiados; sin dependencias nuevas;
  `docs/design/concepts/dish-image-seed/` no existe; único fichero nuevo con `android.*` =
  `AssetDishImageSeedSource.kt`; `DishImageCacheKey.VERSION` sigue `dish-image-v1`.

### Extra comprobado

- La semilla queda **detrás del mismo gate** del interruptor (`generatedIfEnabled { … }` en
  `MainActivity`) y **no** pasa por `GatedDishImageGenerator`: con "Imágenes del plato" apagado se ve la
  foto local y **no** se consume tope de sesión.
- Los 6 títulos del set **no** colisionan con las 12 recetas semilla (su foto propia por `id` no se
  pisa): decisión 8 respetada.

## Hallazgos

**Critical / High / Medium: ninguno.**

- **Low (no bloqueante, precisión de documentación).** El bullet de la fase 7 en
  `docs/technical-discovery.md` afirma que el script, ante un desvío, sale `!= 0` **sin** empaquetar
  nada. En realidad el script escribe **incrementalmente** y falla después en la validación del
  inventario/presupuesto: en el sandbox quedan los ficheros escritos (fallo **ruidoso** con `ERROR` +
  inventario + exit ≠ 0, pero no atómico). El contrato de la spec se cumple (**nunca** un parcial
  silencioso). Corrección sugerida: reescribir la frase para decir que deja el inventario a la vista y
  que hay que regenerar/limpiar con `--force`. Anotado como follow-up de redacción; no bloquea el
  veredicto.

- **Informativo (no es hallazgo).** El grep del plan `prepare-dish-image-seed en app/src → 0` da 1
  coincidencia: la cita del script en el KDoc de `DishImageSeedAssets` (mismo patrón aceptado de
  `RecipePhotoAssets.kt` citando `fetch-recipe-photos.sh`), sin invocación en runtime. Las líneas
  `Failed to create image decoder with message 'unimplemented'` de Robolectric son preexistentes y no
  producen fallos (0/0/0). Con el descriptor de la fase 5 sin cablear, la clave del plato sale del
  **título** de la receta: la prueba manual de la persona debe confirmar que los títulos reales del demo
  coinciden con los 6 del set.

## Seguridad

Sin hallazgos: saneado de la clave (`[a-z0-9-]+`) como defensa en profundidad (nunca ruta fuera de
`img/dishes/seed/`), assets de solo lectura, `null` sin lanzar, sin red ni permisos nuevos en runtime,
sin persistencia en `cacheDir`, sin `Log`/secretos, la clave del proveedor se lee de `local.properties`
(no versionado) y **nunca** se imprime (`Authorization: Bearer` no se refleja en stdout/errores), y el
script no es invocado por ningún código de runtime ni por `init.sh`.

## Documentación / harness

`init.sh` intacto (sigue ejecutando el gate real). ADR 0005 → "Aceptado parcialmente (fases 1-7)";
`docs/technical-discovery.md` (fila *Data and Storage* + bullets de fase 7 y tests);
`docs/domain/taxonomy.md` §6 (nivel semilla); `docs/risks-and-open-questions.md` (coste de preparación
una vez, ~2 %/plato, evitable con `--source`); `domain-model.md`, la propuesta, `DESIGN.md`,
`build-brief.md`, `AGENTS.md` y `.gitattributes` correctamente **no** tocados. `feature_list.json` con
evidencia completa y WIP = 0. Sin `docs/design/concepts/dish-image-seed/`.

## Siguiente acción requerida

1. El **orquestador** commitea la aceptación (rama `feature/dish-image-seed`) y `github-flow` publica
   (`Closes #<n>`, sin merge). El validador no modifica ficheros.
2. No bloqueante: corregir la frase del hallazgo Low en `docs/technical-discovery.md`.
3. No bloqueante (persona, tras el merge): abrir un plato del set sin red y comprobar su foto propia y
   que con "Imágenes del plato" apagado se ve la foto local.
