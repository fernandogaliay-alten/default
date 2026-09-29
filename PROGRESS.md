# Registro de progreso

## Estado verificado actual

- Raíz del repositorio: `C:\Users\fernando.galiay\OneDrive - ALTEN Group\Documents\Default Project`
- Camino estándar de arranque: `init.sh` con Git Bash (`bash init.sh` si `Git\bin` está en el PATH;
  si no, `& "C:\Program Files\Git\bin\bash.exe" init.sh`)
- Camino estándar de verificación: `./gradlew test` y `./gradlew lint`, ambos vía `init.sh`
- Siguiente feature lista: `recipe-domain-model`
- Bloqueo actual: ninguno
- Última verificación: 2026-09-29 (taxonomy-in-code aceptada de forma independiente)

## Registro de sesiones

### Sesión 001

- Fecha: 2026-09-22
- Objetivo: cerrar el discovery y crear el harness mínimo de arranque.
- Completado: documentos de discovery (`CONTEXT.md`, `docs/**`, `DESIGN.md`, ADRs) y los cuatro
  artefactos del harness (`AGENTS.md`, `init.sh`, `PROGRESS.md`, `feature_list.json`).
- Verificación ejecutada: `feature_list.json` validado como JSON; comprobados campos obligatorios,
  ids únicos, dependencias existentes, ausencia de ciclos y de auto-referencias; `init.sh` ejecutado
  con Git Bash en fase pre-bootstrap.
- Evidencia capturada: 26 features, 0 in_progress, 0 `passing` sin evidencia, 0 dependencias
  inexistentes; `init.sh` con código de salida 0.
- Ficheros actualizados: los cuatro artefactos del harness más los documentos de discovery.
- Riesgo o incógnita abierta: proveedor de IA sin decidir (pregunta 1); `bash` no está en el PATH
  del equipo y hay que invocarlo por ruta completa. `minSdk`/`targetSdk` 33 y paquete `com.sazon.app`
  decididos.
- Siguiente paso: ejecutar la feature `bootstrap-android-project`.

### Sesión 002

- Fecha: 2026-09-22
- Objetivo: ejecutar la feature `bootstrap-android-project` (esqueleto Android y gate real).
- Completado: proyecto Android creado con el asistente (Empty Activity, Compose, Kotlin DSL);
  `minSdk` 33, `targetSdk` 36 y `compileSdk` 36; `MainActivity` reducido a una `Surface` vacía;
  `strings.xml` con `Sazón`; `Theme.kt` sin el chequeo de SDK obsoleto; `init.sh` sustituido por el
  gate real; `.gitignore` con `.kotlin/`.
- Verificación ejecutada: `./gradlew test`, `./gradlew lint`, `./gradlew assembleDebug`, `init.sh`
  con Git Bash, e instalación y arranque en el emulador `Pixel_7a_API_33`.
- Evidencia capturada: `ExampleUnitTest` con 1 test y 0 fallos; lint con 0 errores y 18 warnings;
  APK de depuración de 28,2 MB; `init.sh` con código de salida 0; `topResumedActivity` en
  `com.sazon.app/.MainActivity` y buffer de crashes vacío. Captura de pantalla en
  `%TEMP%\opencode\sazon-arranque.png`.
- Ficheros actualizados: `app/build.gradle.kts`, `app/src/main/java/com/sazon/app/MainActivity.kt`,
  `app/src/main/java/com/sazon/app/ui/theme/Theme.kt`, `app/src/main/res/values/strings.xml`,
  `init.sh`, `AGENTS.md`, `docs/technical-discovery.md` y `docs/specs/bootstrap-android-project.md`.
- Riesgo o incógnita abierta: `OldTargetApi` (warning, no error) porque `targetSdk` 36 va por detrás
  del último API publicado (37); no se persigue. El tema usa color dinámico de Material You, que
  contradice la paleta de `DESIGN.md`; falta una feature de tokens de diseño que lo resuelva.
- Siguiente paso: validación independiente con `feature-validator`.

### Sesión 003

- Fecha: 2026-09-22
- Objetivo: cerrar decisiones abiertas tras el bootstrap.
- Completado: `targetSdk` fijado en 36 (se probó 33 y se revirtió); decidido desactivar el color
  dinámico de Material You por contradecir `DESIGN.md`; añadida la feature
  `design-tokens-and-theme` al plan, antes de la primera pantalla.
- Verificación ejecutada: `init.sh` con Git Bash tras el cambio de `targetSdk`.
- Evidencia capturada: ambas ejecuciones con `BUILD SUCCESSFUL`; lint con 0 errores y 18 warnings.
- Ficheros actualizados: `app/build.gradle.kts`, `docs/technical-discovery.md`,
  `docs/specs/bootstrap-android-project.md`, `feature_list.json`, `.gitignore` y `PROGRESS.md`.
- Riesgo o incógnita abierta: ninguno nuevo.
- Siguiente paso: validación independiente del bootstrap con `feature-validator`.

### Sesión 004

- Fecha: 2026-09-22
- Objetivo: crear la línea base del repositorio y validar el bootstrap de forma independiente.
- Completado: commit inicial `06459f8` (56 ficheros) más `66fc6da` (bit de ejecución de `gradlew`);
  creado `.gitattributes` porque `core.autocrlf=true` habría roto `gradlew` e `init.sh` en un clon;
  validación del bootstrap por un agente con contexto limpio.
- Verificación ejecutada: el validador re-ejecutó `init.sh`, `./gradlew test --rerun-tasks`,
  `./gradlew lint` y `./gradlew assembleDebug`, y reprodujo el escenario del SDK ausente exportando
  solo lo versionado a un directorio temporal.
- Evidencia capturada: veredicto **`accept`**; los cinco escenarios de aceptación comprobados;
  coherencia de `minSdk` 33 / `targetSdk` 36 / `compileSdk` 36 / `com.sazon.app` en los cinco
  documentos; sin artefactos generados versionados; `local.properties` ignorado.
- Ficheros actualizados: `feature_list.json` (la feature pasa a `accepted`), la spec (hallazgo 7) y
  este registro.
- Hallazgos del validador (todos de severidad baja, ninguno bloqueante): `.gitattributes` no estaba
  documentado en el alcance (ya corregido); la captura de pantalla no es legible por un modelo sin
  visión; quedan dependencias de test de plantilla sin ejercitar, que son valores por defecto del
  asistente.
- Riesgo o incógnita abierta: ninguno nuevo.
- Siguiente paso: especificar `taxonomy-in-code` con `feature-spec`.

### Sesión 005

- Fecha: 2026-09-29
- Objetivo: implementar `taxonomy-in-code` (la Taxonomía como pieza de código).
- Completado: `kotlinx.serialization` 1.9.0 añadido al catálogo de versiones; tarea `syncTaxonomy`
  que copia `docs/domain/taxonomy.json` a `app/src/main/assets/` (ignorado en git); modelos tipados y
  catálogo consultable por id en `domain.taxonomy` sin dependencias de Android; `AssetTaxonomySource`
  en `data`; test de integridad con dos casos negativos; borrado `ExampleUnitTest.kt`.
- Verificación ejecutada: `./gradlew test`, `./gradlew lint`, `./gradlew assembleDebug`, `init.sh`
  con Git Bash e inspección del contenido del APK.
- Evidencia capturada: `TaxonomyIntegrityTest` con **5 tests, 0 fallos**; lint con 0 errores y 20
  warnings; APK de 29 MB que contiene `assets/taxonomy.json` (22956 bytes); `init.sh` con código 0.
- Ficheros actualizados: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `.gitignore`,
  `docs/domain/taxonomy.json` (BOM eliminado), `docs/technical-discovery.md`,
  `docs/specs/taxonomy-in-code.md`, `feature_list.json` y este registro.
- Hallazgos relevantes: el JSON canónico tenía un **BOM de UTF-8** heredado de un `Set-Content` de
  PowerShell 5.1, y el parser lo rechazaba; lo detectó el test y sin él la app habría fallado al
  arrancar. AGP 9.3.3 rechaza `Provider` en el SourceSet API, así que la sincronización escribe
  directamente en `app/src/main/assets/` en lugar de en un directorio generado.
- Riesgo o incógnita abierta: ninguno nuevo.
- Siguiente paso: validación independiente de `taxonomy-in-code` con `feature-validator`.

### Sesión 006

- Fecha: 2026-09-29
- Objetivo: validar `taxonomy-in-code` de forma independiente y cerrar sus hallazgos.
- Completado: veredicto **`accept`** de un agente con contexto limpio, que re-ejecutó el gate, abrió
  el APK como ZIP, recalculó los tamaños del catálogo con su propio script y comparó el JSON byte a
  byte contra HEAD para descartar manipulación; `feature_list.json` pasa a `accepted`; corregida la
  contradicción del spec (una tarea citaba `assets.srcDir` y las casillas estaban sin marcar);
  anotada como deuda técnica la sincronización por nombre de tarea.
- Verificación ejecutada: `init.sh` tras los cambios de documentación y del build.
- Evidencia capturada: el validador confirmó 5 tests con 0 fallos, APK con `assets/taxonomy.json`, y
  que el BOM era un defecto real: HEAD empieza por `ef bb bf` y el árbol de trabajo por `7b 0a`.
- Ficheros actualizados: `feature_list.json`, `docs/specs/taxonomy-in-code.md`,
  `docs/risks-and-open-questions.md`, `app/build.gradle.kts` y este registro.
- Hallazgos del validador (5, todos de severidad baja): asset generado en el árbol de fuentes con
  enganche por nombre de tarea; checklists del spec sin marcar y una tarea desactualizada (corregido);
  `AssetTaxonomySource` sin test directo (aceptable, lo cubrirá la primera pantalla); `clean` no borra
  el asset; falta de salto de línea final (corregido).
- Riesgo o incógnita abierta: deuda técnica anotada en `docs/risks-and-open-questions.md`.
- Siguiente paso: especificar `recipe-domain-model` con `feature-spec`.
