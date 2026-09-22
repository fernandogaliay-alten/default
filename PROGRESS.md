# Registro de progreso

## Estado verificado actual

- Raíz del repositorio: `C:\Users\fernando.galiay\OneDrive - ALTEN Group\Documents\Default Project`
- Camino estándar de arranque: `init.sh` con Git Bash (`bash init.sh` si `Git\bin` está en el PATH;
  si no, `& "C:\Program Files\Git\bin\bash.exe" init.sh`)
- Camino estándar de verificación: `./gradlew test` y `./gradlew lint`, ambos vía `init.sh`
- Siguiente feature lista: `taxonomy-in-code`
- Bloqueo actual: ninguno
- Última verificación: 2026-09-22 (bootstrap validado de forma independiente: veredicto `accept`)

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
