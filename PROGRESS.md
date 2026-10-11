# Registro de progreso

## Estado verificado actual

- Raíz del repositorio: `C:\Users\fernando.galiay\Documents\Default Project`
- Camino estándar de arranque: `init.sh` con Git Bash (`bash init.sh` si `Git\bin` está en el PATH;
  si no, `& "C:\Program Files\Git\bin\bash.exe" init.sh`)
- Camino estándar de verificación: `./gradlew test` y `./gradlew lint`, ambos vía `init.sh`
- Flujo de entrega: `github-flow` (worktrees + issues/PR) sobre `feature-flow`; rama base `main` y
  remoto `origin` → `https://github.com/fernandogaliay-alten/default.git` (privado)
- Últimas features aceptadas: `fake-recipe-generator`, `recipe-validator`, `prompt-builder`,
  `seed-recipe-catalog`, `design-tokens-and-theme`, `onboarding-shell-and-identity`,
  `onboarding-diet-style-cuisines`,   `onboarding-ingredients-safety-kitchen`, `suggestion-batch-cache`,
  `home-offline-blocks`, `local-result-filters`, `generator-ingredients-step`, `generator-settings-step`,
  `profile-screen`, `generation-flow-with-validation`, `manual-suggestions-refresh`, `results-screen`,
  `recipe-detail-screen` y `favorites` (validador independiente, veredicto `accept`). El onboarding queda **completo**
  (7 pasos) y persiste el Perfil al cerrar; el Generador ya ejecuta el corte vertical principal del MVP
  (generar -> validar -> mostrar solo aceptadas, con carga cancelable) y monta la pantalla de Resultados
  real (fórmula de `matchScore`, estados vacío/error con "Reintentar", subtítulo de búsqueda); la pestaña
  Perfil es real (identidad + preferencias editables reutilizando los editores del onboarding) y permite
  forzar el refresco del Lote; el Detalle de receta ya muestra ingredientes con cantidades, ajusta las
  raciones (escalado con redondeo por unidad), recorre los pasos y consulta tips/nutrición opcional; la
  pestaña Favoritos es real (guardar/quitar, persistencia local de una copia de la Receta y consulta sin
  conexión, con estados poblado/vacío).
- Siguiente feature en orden de lista: `favorites` quedó `accepted`, de modo que `activity-summary`
  (depende de `profile-screen` y `favorites`) es la siguiente candidata.
- Capturas de UI (política del PR #37): **backfill pendiente**; 11 features de UI `accepted` y 18
  pantallas por exportar de Stitch (ver sesión 046 y `docs/design/concepts/<feature-id>/README.md`).
  `results-screen`, `recipe-detail-screen` y `favorites` ya aportan sus capturas
  (`docs/design/concepts/results-screen/resultados/`,
  `docs/design/concepts/recipe-detail-screen/detalle-receta/` y
  `docs/design/concepts/favorites/favoritos/`).
- Propuesta post-MVP documentada (no aceptada, no planificada, fuera del MVP):
  `docs/backend-proposal.md` — backend Kotlin + Ktor (opción B) para cuenta, sincronización
  offline-first y proxy de IA, con módulo de dominio compartido. No altera el estado de ninguna feature
  ni el MVP local-first; queda para retomar en una fase posterior.
- Feature en curso: ninguna. WIP = 0.
- Bloqueo actual: ninguno
- Última verificación (2026-10-10, `favorites`, validador independiente): veredicto `accept`.
  `init.sh` con Git Bash exit 0; `./gradlew testDebugUnitTest --rerun-tasks` (32/32 tareas) y
  `./gradlew lint --rerun-tasks` (28/28 tareas); **301 tests/0 fallos/errores/saltados** en 46 suites
  (277 preexistentes + 24 nuevos de
  `FavoriteServiceTest`/`FavoritesListTest`/`RoomFavoriteRepositoryTest`); lint 0 errores y 13 warnings
  preexistentes (0 en ficheros de la feature).

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

### Sesión 007

- Fecha: 2026-09-29
- Objetivo: implementar `recipe-domain-model` (esquema único de Receta para `seed` y `ai`).
- Completado: paquete `com.sazon.app.domain.recipe` con `Recipe`, `RecipeIngredient`, `RecipeStep`,
  `Nutrition`, `RecipeSource` y `MeasureUnit`; `RecipeJson` con `ignoreUnknownKeys` y
  `encodeDefaults`; `RecipeSerializationTest` con cuatro casos. Resueltas las preguntas 4 (unidades:
  `MeasureUnit` `g`/`ml`/`ud`/`cda`/`cdta`, `unit` nullable) y 5 (nutrición opcional en el esquema,
  siempre pedida en el prompt) en `docs/risks-and-open-questions.md`, con renumeración del resto.
- Verificación ejecutada: `./gradlew test`, `./gradlew lint` e `init.sh` con Git Bash.
- Evidencia capturada: `RecipeSerializationTest` con **4 tests, 0 fallos** (9 tests en total); lint
  con 0 errores y sin warnings nuevos; `init.sh` con código 0; `com.sazon.app.domain.recipe` sin
  imports de Android.
- Ficheros actualizados: `app/src/main/java/com/sazon/app/domain/recipe/Recipe.kt`,
  `.../RecipeJson.kt`, `app/src/test/java/com/sazon/app/domain/recipe/RecipeSerializationTest.kt`,
  `docs/risks-and-open-questions.md`, `docs/technical-discovery.md`,
  `docs/specs/recipe-domain-model.md`, `feature_list.json` y este registro.
- Hallazgos relevantes: la renumeración de preguntas obligó a corregir citas en `feature_list.json`
  (semilla: 11→9) y `technical-discovery.md`. Una aserción del propio test estaba mal contada
  (esperaba 1 ingrediente libre y hay 2); no era un fallo del modelo. La pregunta 4 antigua
  (`matchScore`) sigue abierta: dirección provisional "lo calcula la app, derivado y no persistido",
  registrada solo en la spec.
- Riesgo o incógnita abierta: `matchScore` sin cerrar formalmente (se decide antes de `results-screen`).
- Siguiente paso: validación independiente de `recipe-domain-model` con `feature-validator`.

### Sesión 008

- Fecha: 2026-09-29
- Objetivo: validar `recipe-domain-model` de forma independiente y persistir la aceptación.
- Completado: veredicto **`accept`** de un agente con contexto limpio. Re-ejecutó `init.sh` (exit 0),
  `./gradlew test --rerun-tasks` (9 tests, 0 fallos) y `lint` (0 errores, 20 warnings preexistentes);
  confirmó el esquema contra los documentos, el ámbito y la ausencia de imports de Android. Cerrados
  los dos hallazgos Low en la spec (cita `parse(json): Recipe` y numeración antigua de preguntas).
- Verificación ejecutada: el gate estándar y los tests forzados por el validador; sin cambios de
  código tras la validación (solo documentación).
- Evidencia capturada: `feature_list.json` pasa a `accepted`; el validador confirmó 9 tests/0 fallos,
  `MeasureUnit` con los cinco ids, `matchScore` como `Int?` de cable y sin tocar el build.
- Ficheros actualizados: `feature_list.json`, `docs/specs/recipe-domain-model.md` y este registro.
- Hallazgos del validador (2, ambos Low, no bloqueantes): numeración antigua de preguntas dentro de la
  spec (aclarada) y typo `parse(json): String` (corregido a `Recipe`).
- Riesgo o incógnita abierta: la pregunta 4 de riesgos (antigua 6, `matchScore`) sigue abierta.
- Siguiente paso: especificar `profile-persistence` con `feature-spec`.

### Sesión 009

- Fecha: 2026-10-01
- Objetivo: implementar `profile-persistence` (capa mínima de persistencia del Perfil de usuario y de
  preferencias en Room).
- Completado: dominio puro `com.sazon.app.domain.profile` (`UserProfile`, `PreferenceProfile`,
  `ProfileName` con recorte y 1–40, y el puerto `ProfileRepository` con `get`/`observe`/`save`/
  `clear`); capa `com.sazon.app.data.profile` (`ProfileEntity` de una fila `user_profile` con PK
  `id = 1`, `ProfileDao` con `Flow`/`INSERT OR REPLACE`/`DELETE`, `ListStringConverter` JSON con
  `kotlinx.serialization`, `ProfileMappers`, `RoomProfileRepository` y `ProfileDatabase` con
  `version = 1`/`exportSchema = false` y fábrica `build(context)`); `RoomProfileRepositoryTest` con
  los seis escenarios y dos de borde. Sin UI, sin firma, sin DataStore y sin migraciones (no-goals).
- Verificación ejecutada: `./gradlew test`, `./gradlew lint` e `init.sh` con Git Bash; inspección de
  imports de `domain.profile`.
- Evidencia capturada: `RoomProfileRepositoryTest` con **9 tests, 0 fallos** (18 tests en total, 0
  fallos), en JVM sobre Robolectric `@Config(sdk = [33])` y base en memoria; lint con 0 errores y los
  mismos 20 warnings preexistentes (sin nuevos); `init.sh` con código 0; `domain.profile` sin imports
  de `android.*`. Versiones fijadas: KSP `2.2.10-2.0.2`, Room `2.8.5`, Robolectric `4.17`,
  `androidx.test:core` `1.7.0`, kotlinx-coroutines `1.11.0`.
- Ficheros cambiados: `gradle/libs.versions.toml`, `gradle.properties`, `build.gradle.kts`,
  `app/build.gradle.kts`, los cuatro ficheros de `domain/profile`, los seis de `data/profile`,
  `app/src/test/.../data/profile/RoomProfileRepositoryTest.kt`, `docs/technical-discovery.md`,
  `docs/specs/profile-persistence.md`, `feature_list.json` y este registro.
- Hallazgos relevantes: AGP 9 usa Kotlin integrado y bloquea el DSL `kotlin.sourceSets` que KSP
  necesita; se añadió `android.disallowKotlinSourceSets=false` en `gradle.properties` (solución
  documentada por AGP, con un warning experimental, no bloqueante). Se actualizó el spec con este
  hallazgo y con las versiones elegidas.
- Riesgo o incógnita abierta: ninguno nuevo. La única deuda anotada es el warning experimental de AGP
  por la excepción de KSP.
- Siguiente paso: validación independiente de `profile-persistence` con `feature-validator`.

### Sesión 010

- Fecha: 2026-10-02
- Objetivo: validar `profile-persistence` de forma independiente y persistir la aceptación.
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted`; desbloqueada `preference-signature` (su dependencia ya está aceptada).
- Verificación ejecutada: el validador re-ejecutó `init.sh` con Git Bash (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos), `./gradlew test --rerun-tasks` y
  `./gradlew lint --rerun-tasks`.
- Evidencia capturada: 18 tests, 0 fallos (`RoomProfileRepositoryTest` 9, `RecipeSerializationTest` 4,
  `TaxonomyIntegrityTest` 5); lint 0 errores y los mismos 20 warnings preexistentes (sin nuevos);
  `domain.profile` sin imports `android.*`/`androidx.*`; alcance y no-goals respetados (sin UI, sin
  firma, sin DataStore, sin migraciones, sin `ProfileDatabase` en `MainActivity`); escenarios 1-6 más
  `clear()` cubiertos.
- Ficheros actualizados: `feature_list.json` y este registro.
- Hallazgos del validador (4, todos Low o informativos, ninguno bloqueante): estado de git mezcla la
  feature ya aceptada `recipe-domain-model` sin commitear (no es de esta feature); `AGENTS.md`
  modificado con la sección "Reparto de roles (subagentes)" y no declarado en la sesión 009 (es
  harness, ajeno a la feature, pendiente de trazar/commitear aparte); `ProfileDatabase` usa el nombre
  de fichero genérico `sazon.db` (deuda ya anotada); `ListStringConverter.toList` no tolera JSON
  malformado (solo con BD corrupta, aceptable en v1); perfil en Room sin cifrado (coherente con
  ADR 0002).
- Riesgo o incógnita abierta: ninguno nuevo.
- Siguiente paso: especificar `preference-signature` con `feature-spec`.

### Sesión 011

- Fecha: 2026-10-05
- Objetivo: habilitar la entrega concurrente por worktrees y PR con la skill `github-flow`.
- Completado: instalada la skill `github-flow` (y su `references/evals.md`) en `~/.agents/skills/`;
  consolidada la base del repositorio en el commit `65c7d7c`; rama renombrada `master` → `main`;
  creado el remoto `origin` y publicado `main` en el repo privado `fernandogaliay-alten/default`;
  añadido permiso `external_directory` para `...\Documents\*-worktrees\**` en la config global.
- Herramientas instaladas: **opencode CLI** 1.18.34 (npm `opencode-ai`; ExecutionPolicy `CurrentUser`
  → `RemoteSigned` para invocar el shim) y **gh** 2.102.0 autenticado como `fernandogaliay-alten`
  (scopes `repo`, `read:org`, `gist`, `workflow`).
- Bootstrap del SDK para worktrees: `ANDROID_HOME` definido a nivel de usuario
  (`%LOCALAPPDATA%\Android\Sdk`), `scripts/new-worktree.sh` creado (crea/reutiliza el worktree y
  copia `local.properties`, que está ignorado en git y no viaja solo) y documentado en `AGENTS.md`.
- Ficheros actualizados: `AGENTS.md` (acuerdos de trabajo alineados con `github-flow`: ciclo autónomo,
  commit al `accept`, push + PR solo con permiso de push, nunca merge; sección "Repositorio y
  publicación"; regla `in_progress` por worktree; notas de entorno de worktrees), `scripts/new-worktree.sh`
  y este registro.
- Verificación ejecutada: `git status` limpio; push a `origin/main` OK; `gh auth status` y
  `gh repo view` (privado, default `main`); `opencode --version` y `gh --version`; `bash -n` del script.
- Riesgo o incógnita abierta: los artefactos compartidos (`feature_list.json`, `PROGRESS.md`) los
  edita cada worktree en su rama; al mergear varios PRs en paralelo habrá conflictos que la persona
  debe resolver. Pendiente de probar `github-flow` en modo `plan`.
- Siguiente paso: reiniciar opencode y lanzar `github-flow` en modo `plan` para `preference-signature`.

### Sesión 012

- Fecha: 2026-10-05
- Objetivo: implementar `preference-signature` (firma de preferencias determinista).
- Completado: objeto puro `com.sazon.app.domain.profile.PreferenceSignature` con
  `of(preferences: PreferenceProfile): String`; cadena canónica con listas tratadas como conjuntos
  (`distinct().sorted()`), orden de campos fijo, separador U+001F y prefijo de versión `v1`; hash
  SHA-256 en hex minúscula con `java.security.MessageDigest`. Excluye la identidad por construcción
  (recibe `PreferenceProfile`, no `UserProfile`) y también `updatedAt`. `PreferenceSignatureTest` con
  los escenarios 1–7. Sin persistir la firma, sin integrarla en el Lote y sin UI (no-goals).
- Verificación ejecutada: `init.sh` con Git Bash, `./gradlew test` y `./gradlew lint`; inspección de
  imports de `domain.profile`.
- Evidencia capturada: `PreferenceSignatureTest` con **8 tests, 0 fallos** (26 tests en total, 0
  fallos); lint con 0 errores y los mismos 20 warnings preexistentes (sin nuevos); `init.sh` con
  código 0; `domain.profile` sin imports de Android; vector dorado
  `256cbcb3fae4784e9f89ab408d690a8ad184f36435fc276ca25e9b9b42197a8c`.
- Ficheros actualizados: `app/src/main/java/com/sazon/app/domain/profile/PreferenceSignature.kt`,
  `app/src/test/java/com/sazon/app/domain/profile/PreferenceSignatureTest.kt`,
  `docs/specs/preference-signature.md`, `docs/technical-discovery.md`, `docs/domain-model.md`,
  `feature_list.json` y este registro.
- Hallazgos relevantes: ninguno. El vector dorado previsto coincidió con el SHA-256 real producido,
  así que no hubo que corregirlo.
- Riesgo o incógnita abierta: ninguno nuevo.
- Siguiente paso: validación independiente de `preference-signature` con `feature-validator`.

### Sesión 013

- Fecha: 2026-10-05
- Objetivo: validar `preference-signature` de forma independiente y persistir la aceptación.
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la aceptación independiente.
- Verificación ejecutada: el validador re-ejecutó `init.sh` con Git Bash (exit 0),
  `./gradlew testDebugUnitTest --rerun-tasks` y `./gradlew lint --rerun-tasks`.
- Evidencia capturada: 26 tests, 0 fallos (`PreferenceSignatureTest` 8, `RoomProfileRepositoryTest` 9,
  `RecipeSerializationTest` 4, `TaxonomyIntegrityTest` 5); lint 0 errores y sin warnings nuevos en
  ficheros de la feature; el validador recalculó el vector dorado por su cuenta con SHA-256 de .NET y
  coincidió exactamente (`256cbcb3...197a8c`); escenarios 1–7 cubiertos, una comprobación por campo,
  identidad y `updatedAt` excluidos, listas como conjuntos, alcance/no-goals respetados y `of(...)`
  recibe `PreferenceProfile` (sin sobrecarga con `UserProfile`).
- Ficheros actualizados: `feature_list.json` y este registro.
- Hallazgos del validador (1 Low, no bloqueante): el recuento de warnings de lint pasó de 20 a 21 por
  deriva de comprobaciones de versión de dependencias dependientes de red/tiempo; ninguno es de los
  ficheros de la feature. Se mantiene la redacción "sin warnings nuevos".
- Riesgo o incógnita abierta: ninguno nuevo.
- Siguiente paso: especificar `recipe-validator` con `feature-spec`.

### Sesión 014

- Fecha: 2026-10-05

- Objetivo: implementar `recipe-validator` (validador que descarta recetas inseguras).
- Completado: paquete puro `com.sazon.app.domain.validation` con `RecipeValidator`,
  `RecipeConstraints`, `DiscardReason`, `DiscardedRecipe`, `RecipeValidation`, `ValidationOutcome` y
  el helper interno `IngredientText` (normalización sin acentos con `java.text.Normalizer` y matching
  por keywords conservador). El parseo del envelope `{ "recipes": [...] }` es propio (un `Json` nuevo,
  no se toca `RecipeJson`): raíz/`recipes` malformados → `Malformed`; cada elemento se decodifica por
  separado dentro de `runCatching`, de modo que una receta rota se descarta con `SCHEMA` sin tumbar el
  lote. `validateRecipe` aplica las reglas 1–4 de `docs/domain/taxonomy.md` §5 en orden: esquema/ids
  (ids desconocidos sin fallback → `SCHEMA`; `categoryId` → `"other"`; `ingredientId` desconocido →
  libre), dieta por `excludedTags`, restricciones duras por `allergens` curados + keywords sobre el
  nombre normalizado, y coherencia (`abs(sum(steps) - totalMinutes) > 5` y `declaredAllergens` debe
  cubrir los alérgenos curados de los ingredientes resueltos). Sin Android, sin red, sin persistencia
  y sin log (no-goals). Resueltas las preguntas 5 (trazas) y 7 (ingrediente libre) en
  `docs/risks-and-open-questions.md`, renumeradas las restantes (6→5, 8→6, 9→7) y corregida la cita de
  `docs/technical-discovery.md`.
- Verificación ejecutada: `./gradlew test`, `./gradlew lint` e `init.sh` con Git Bash; inspección de
  imports de `domain.validation`.
- Evidencia capturada: `RecipeValidatorTest` con **12 tests, 0 fallos** (38 tests en total, 0 fallos);
  cubre los escenarios 1–11 más un extra de `ingredientId` desconocido degradado a libre. Lint con 0
  errores y 21 warnings preexistentes (GradleDependency 7, UnusedResources 7, NewerVersionAvailable 3,
  AndroidGradlePluginVersion 2, OldTargetApi 1, RedundantLabel 1), ninguno en ficheros de la feature;
  `init.sh` con código 0; `com.sazon.app.domain.validation` sin imports `android.*`/`androidx.*`.
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/domain/validation/RecipeValidator.kt`,
  `.../IngredientText.kt`, `app/src/test/java/com/sazon/app/domain/validation/RecipeValidatorTest.kt`,
  `docs/risks-and-open-questions.md`, `docs/technical-discovery.md`, `feature_list.json` y este
  registro. No se tocaron `app/build.gradle.kts` ni `gradle/libs.versions.toml`.
- Hallazgos relevantes: ninguno de comportamiento; los dos descartes/conversiones esperados
  (`categoryId` → `other`, `ingredientId` desconocido → libre) se documentan como decisiones de la
  spec.
- Riesgo o incógnita abierta: ninguno nuevo. Queda anotado el riesgo residual ya descrito en la spec:
  un ingrediente libre que sea violación de dieta (p. ej. "pollo" sin `ingredientId`) no se detecta
  como dieta, solo por keywords de restricciones duras.
- Siguiente paso: validación independiente de `recipe-validator` con `feature-validator`.

### Sesión 015

- Fecha: 2026-10-05

- Objetivo: implementar `fake-recipe-generator` (puerto `RecipeGenerator` + implementación falsa
  determinista y sin red) según `docs/specs/fake-recipe-generator.md`.
- Completado: puerto puro `com.sazon.app.domain.generator.RecipeGenerator` con las tres operaciones
  `suspend` (`suggestByPreferences`, `searchByText`, `generateFromIngredients`) y `GenerationConstraints`
  (`profile`, `servings` + `mealTypeId`/`timeBucketId`/`difficultyId`/`notes` opcionales);
  `com.sazon.app.data.generator.FakeRecipeGenerator` con 4 fixtures fijas (`source = AI`,
  `matchScore = null`, ids `fake-*`) que ignora la entrada y no usa `Random`/reloj/`UUID`; incluye el
  caso negativo: ingrediente libre `"Pan"` (`ingredientId = null`, gluten) y `"Pollo"` (id real, tag
  `poultry`); `FakeRecipeGeneratorTest` con los escenarios 1–6. Sin Validador, sin proveedor real/HTTP,
  sin persistencia/caché/DI/UI, sin cálculo de `matchScore` (no-goals).
- Verificación ejecutada: `./gradlew testDebugUnitTest --tests "...FakeRecipeGeneratorTest"` (enfocado),
  y el gate estándar `init.sh` con Git Bash (`./gradlew test` + `./gradlew lint`); inspección de imports
  y de que no cambian `app/build.gradle.kts` ni `gradle/libs.versions.toml`.
- Evidencia capturada: `FakeRecipeGeneratorTest` con **6 tests, 0 fallos** (**32 tests en total, 0
  fallos**: RoomProfileRepositoryTest 9, PreferenceSignatureTest 8, TaxonomyIntegrityTest 5,
  RecipeSerializationTest 4); lint 0 errores y 21 warnings preexistentes, ninguno en ficheros de la
  feature; `init.sh` con código 0; ni `domain.generator` ni `data.generator` importan
  `android.*`/`androidx.*`/`java.net`/HTTP; sin dependencias nuevas.
- Ficheros cambiados: `app/src/main/java/com/sazon/app/domain/generator/RecipeGenerator.kt`,
  `.../domain/generator/GenerationConstraints.kt`,
  `app/src/main/java/com/sazon/app/data/generator/FakeRecipeGenerator.kt`,
  `app/src/test/java/com/sazon/app/data/generator/FakeRecipeGeneratorTest.kt`,
  `docs/technical-discovery.md`, `docs/domain-model.md`, `feature_list.json` y este registro.
- Hallazgos relevantes: `./gradlew test --tests` no funciona en este proyecto (el atajo `test` es la
  tarea de ciclo de vida de AGP y no acepta `--tests`); el filtrado enfocado se hace con
  `testDebugUnitTest --tests`. La spec cita `./gradlew test --tests`; se registra como desviación menor,
  sin impacto en el gate (`init.sh` ejecuta `./gradlew test`).
- Riesgo o incógnita abierta: ninguno nuevo. La pregunta 6 (errores/reintentos, `ai_unavailable`) sigue
  abierta y el puerto devuelve `List<Recipe>` sin envoltorio de error, como fija la spec.
- Siguiente paso: validación independiente de `fake-recipe-generator` con `feature-validator`.


### Sesión 016

- Fecha: 2026-10-05

- Objetivo: validar `recipe-validator` de forma independiente y persistir la aceptación.
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la aceptación independiente; registrado el detalle en
  `docs/validations/recipe-validator.md`.
- Verificación ejecutada: el validador re-ejecutó `init.sh` con Git Bash (exit 0),
  `./gradlew test --rerun-tasks` y `./gradlew lint --rerun-tasks`; inspección de pureza y de las
  citas de numeración.
- Evidencia capturada: 38 tests, 0 fallos (`RecipeValidatorTest` 12, `RoomProfileRepositoryTest` 9,
  `PreferenceSignatureTest` 8, `RecipeSerializationTest` 4, `TaxonomyIntegrityTest` 5); lint 0
  errores y sin issues en ficheros de la feature; `domain.validation` sin imports `android.*`; build
  intacto (`app/build.gradle.kts` y `gradle/libs.versions.toml` sin cambios); escenarios 1–11 y orden
  de reglas §5 confirmados; malformada nunca lanza; preguntas 5 y 7 resueltas con renumeración y
  citas coherentes.
- Ficheros actualizados: `feature_list.json`, `docs/validations/recipe-validator.md` y este registro.
- Hallazgos del validador (3 Low, ninguno bloqueante): dos entradas de `risks-and-open-questions.md`
  comparten la etiqueta "(antes pregunta 5)" pero se distinguen por contexto; `FALLBACK_CATEGORY_ID`
  fijado en código en vez de leerse de `recipeCategories.fallback` (el modelo no lo expone); riesgo
  residual ya documentado de ingrediente libre que viola dieta solo detectado por keywords.
- Riesgo o incógnita abierta: ninguno nuevo.
- Siguiente paso: especificar `prompt-builder` con `feature-spec`.

### Sesión 017

- Fecha: 2026-10-05

- Objetivo: validar `fake-recipe-generator` de forma independiente y persistir la aceptación.
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la aceptación independiente; commit y publicación de la rama.
- Verificación ejecutada: el validador re-ejecutó `init.sh` con Git Bash (exit 0, test + lint, sin
  procesos largos), `./gradlew testDebugUnitTest --rerun-tasks` y `lint --rerun-tasks`; parseó los XML
  de resultados y de lint por su cuenta.
- Evidencia capturada: 32 tests, 0 fallos/errores/saltados (`FakeRecipeGeneratorTest` 6,
  `RoomProfileRepositoryTest` 9, `PreferenceSignatureTest` 8, `TaxonomyIntegrityTest` 5,
  `RecipeSerializationTest` 4); lint 21 warnings, 0 errores, ninguno en ficheros de la feature; el
  validador comprobó que el puerto tiene exactamente las tres operaciones `suspend`, que el falso es
  determinista e independiente de la entrada (sin `Random`/reloj/`UUID`/estado mutable), que las
  fixtures incluyen el caso negativo (`Pan` libre con `ingredientId=null` y `pollo` con tag
  `poultry`), que los cuatro `id` de fixture resuelven en la Taxonomía, la ida y vuelta `RecipeJson`,
  la ausencia de imports `android.*`/`java.net`/HTTP y de dependencias nuevas, y el respeto de
  alcance/no-goals.
- Ficheros actualizados: `feature_list.json` y este registro.
- Hallazgos del validador (2 Low, informativos, no bloqueantes): las casillas del `Validator Checklist`
  de la spec quedan sin marcar (codifican las propias comprobaciones del validador); el test de guardia
  Android/red escanea texto fuente y resuelve la raíz vía `settings.gradle.kts`, algo frágil pero
  correcto hoy. El recuento de warnings de lint (20→21) sigue la deriva ya anotada en la sesión 013.
- Riesgo o incógnita abierta: ninguno nuevo. La pregunta 6 (errores/reintentos, `ai_unavailable`) sigue
  abierta y el puerto devuelve `List<Recipe>` sin envoltorio de error, como fija la spec.
- Siguiente paso: especificar `recipe-validator` con `feature-spec`.

### Sesión 018

- Fecha: 2026-10-07
- Objetivo: reanudar e implementar `prompt-builder` (constructor de prompt desde el Perfil) según
  `docs/specs/prompt-builder.md`. Una ejecución previa se interrumpió por un corte del proveedor y dejó
  el trabajo sin commitear; esta sesión lo retoma sin rehacer lo ya válido.
- Completado: paquete puro `com.sazon.app.domain.prompt` con `PromptIngredient`, `PromptRequest`,
  `BuiltPrompt` y `PromptBuilder` (`build(taxonomy, request)` y `RECIPES_PER_GENERATION = 6`). El
  bloque `system` (dependiente solo de la Taxonomía) fija el rol, la regla de responder solo con el
  JSON pedido, el contrato de `docs/domain/taxonomy.md` sección 4, las unidades cerradas
  (`g`/`ml`/`ud`/`cda`/`cdta`/`null`), la despensa asumida y la enumeración `id — label` de
  `recipeCategories`/`mealTypes`/`cuisines`/`difficulties`/`hardRestrictions` con la instrucción de no
  usar texto libre en los campos clasificables. El bloque `user` traduce el Perfil y el contexto
  opcional con las secciones fijas de seguridad (`Patrón de dieta:` y `Restricciones duras:`, esta
  última con `Ninguna` si la lista está vacía), inyecta los `promptHint` de dieta y preferencias
  blandas, y refleja cocinas, ingredientes preferidos/evitados, ajustes (comida, dificultad, franja con
  `maxMinutes` o "sin límite máximo", comensales), ingredientes disponibles (de Taxonomía o libres) y
  `query`/`notes`. Los ids desconocidos se omiten sin lanzar ni inventar etiquetas; sin `Random`, reloj
  ni estado mutable (determinista). `PromptBuilderTest` con los escenarios 1–8 (10 tests). Sin UI, sin
  red, sin persistencia y sin firma (no-goals de la spec).
- Verificación ejecutada: `& "C:\Program Files\Git\bin\bash.exe" init.sh` (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos), `./gradlew testDebugUnitTest --rerun-tasks` con parseo propio
  de los XML de resultados, y parseo del informe de lint.
- Evidencia capturada: 54 tests, 0 fallos/errores/saltados (`PromptBuilderTest` 10,
  `RecipeValidatorTest` 12, `RoomProfileRepositoryTest` 9, `PreferenceSignatureTest` 8,
  `FakeRecipeGeneratorTest` 6, `TaxonomyIntegrityTest` 5, `RecipeSerializationTest` 4); lint 0 errores
  y 21 warnings preexistentes, ninguno en ficheros de la feature; `init.sh` con código 0;
  `com.sazon.app.domain.prompt` sin imports `android.*`/`androidx.*`/`java.net` y con dependencias solo
  de `domain.profile` y `domain.taxonomy`.
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/domain/prompt/PromptBuilder.kt`,
  `app/src/test/java/com/sazon/app/domain/prompt/PromptBuilderTest.kt`, `docs/specs/prompt-builder.md`
  (spec del planner, ya presente), `docs/technical-discovery.md`, `feature_list.json` y este registro.
  No se tocaron `PreferenceProfile`, la Taxonomía, `app/build.gradle.kts` ni `gradle/libs.versions.toml`.
- Hallazgos relevantes: ninguno de comportamiento. En la Taxonomía real `flexitarian` aporta el
  `promptHint` usado por el escenario 5 y la franja `gt_60` tiene `maxMinutes = null` (cubierto por el
  test 6b). El trabajo previo ya estaba completo y en verde; solo se verificó y se registró la evidencia.
- Riesgo o incógnita abierta: ninguno nuevo. Riesgo residual ya descrito en la spec: una restricción
  dura corrupta (id inexistente) no se comunicaría al modelo porque se omite; aceptable porque la UI
  solo ofrece ids válidos.
- Siguiente paso: validación independiente de `prompt-builder` con `feature-validator`.

### Sesión 019

- Fecha: 2026-10-07
- Objetivo: validar `prompt-builder` de forma independiente y persistir la aceptación.
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la aceptación independiente. Sin cambios de código tras la
  validación.
- Verificación ejecutada: el validador re-ejecutó `init.sh` con Git Bash (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos), `./gradlew testDebugUnitTest --rerun-tasks` (32/32 tareas) y
  parseó por su cuenta los XML de resultados y el informe de lint; releyó la Taxonomía real y la spec.
- Evidencia capturada: 54 tests, 0 fallos/errores/saltados (`PromptBuilderTest` 10,
  `RecipeValidatorTest` 12, `RoomProfileRepositoryTest` 9, `PreferenceSignatureTest` 8,
  `FakeRecipeGeneratorTest` 6, `TaxonomyIntegrityTest` 5, `RecipeSerializationTest` 4); lint 0 errores y
  21 warnings preexistentes, ninguno en ficheros de la feature; `domain.prompt` sin imports
  `android.*`/`androidx.*`/`java.net` y sin `Random`/reloj/`UUID`; build y dependencias intactos;
  escenarios 1–8 y contrato de `docs/domain/taxonomy.md` §4 confirmados; sección de seguridad con
  `Ninguna` cuando no hay restricciones duras.
- Hallazgos del validador (2 Low, informativos, ninguno bloqueante): deriva de documentación en
  `docs/domain/taxonomy.md` §4, que aún lista `matchScore` aunque la app lo deriva y el prompt no lo
  pide; robustez de dos aserciones del test (coincidencia por subcadena en `escenario_4` y microcopy
  exacto), correctas hoy y heredadas del patrón de features previas.
- Riesgo o incógnita abierta: ninguno nuevo. Se mantiene el riesgo residual documentado en la spec
  (una restricción dura corrupta no se comunicaría al modelo).
- Siguiente paso: commit local de la rama `feature/prompt-builder` (lo hace el orquestador de
  `feature-flow`); la publicación queda para `github-flow`. Después, especificar `seed-recipe-catalog`
  con `feature-spec`.

### Sesión 020

- Fecha: 2026-10-07
- Objetivo: implementar `seed-recipe-catalog` (contenido + costura de lectura + test de integridad) según
  `docs/specs/seed-recipe-catalog.md`. Sesión reanudada tras una interrupción de la ejecución previa
  (stall del stream del proveedor); el trabajo en disco ya existía sin commitear y solo faltaban los
  artefactos de harness/docs.
- Completado: `app/src/main/assets/seed-recipes.json` con **12 recetas semilla** autoradas (envelope
  `{ "recipes": [...] }`, `source = seed`, `matchScore` omitido, ids `seed-*`); parser puro
  `com.sazon.app.domain.recipe.SeedRecipeCatalog` + `SeedRecipeJson` (sin Android); lector
  `com.sazon.app.data.AssetSeedRecipeSource(context).load()`; propiedad de sistema
  `seed.recipes.asset.path` en `app/build.gradle.kts` (nada más); `SeedRecipeCatalogTest` (escenarios
  1 y 3–6, con dos casos negativos de JSON mutado) y `AssetSeedRecipeSourceTest` (escenario 2,
  Robolectric). Sin UI, sin red, sin persistencia, sin `RecipeValidator` en ejecución y sin
  `matchScore` (no-goals). No se tocó `.gitignore` (el asset es fuente versionada, no generada).
- Verificación ejecutada: `& "C:\Program Files\Git\bin\bash.exe" init.sh` (exit 0),
  `./gradlew testDebugUnitTest --rerun-tasks` con parseo de los XML de resultados,
  `./gradlew lint` (dentro de `init.sh`), `./gradlew assembleDebug` e inspección del contenido del APK;
  `git check-ignore` sobre el asset e inspección de imports de `domain.recipe`.
- Evidencia capturada: **51 tests, 0 fallos/errores/saltados** (`SeedRecipeCatalogTest` 6,
  `AssetSeedRecipeSourceTest` 1, `RecipeValidatorTest` 12, `RoomProfileRepositoryTest` 9,
  `PreferenceSignatureTest` 8, `TaxonomyIntegrityTest` 5, `FakeRecipeGeneratorTest` 6,
  `RecipeSerializationTest` 4); lint 0 errores y 21 warnings preexistentes, ninguno en ficheros de la
  feature; `init.sh` con código 0; `app-debug.apk` (29,45 MB) contiene `assets/seed-recipes.json`
  (20710 bytes sin comprimir); `seed-recipes.json` **no** está ignorado en `.gitignore` y aparece como
  fichero nuevo versionable en `git status`; `com.sazon.app.domain.recipe` sin imports `android.*`; sin
  dependencias nuevas.
- Ficheros creados/cambiados: `app/src/main/assets/seed-recipes.json`,
  `app/src/main/java/com/sazon/app/domain/recipe/SeedRecipeCatalog.kt`,
  `app/src/main/java/com/sazon/app/data/AssetSeedRecipeSource.kt`,
  `app/src/test/java/com/sazon/app/domain/recipe/SeedRecipeCatalogTest.kt`,
  `app/src/test/java/com/sazon/app/data/AssetSeedRecipeSourceTest.kt`, `app/build.gradle.kts`,
  `docs/technical-discovery.md`, `feature_list.json` y este registro. La spec
  `docs/specs/seed-recipe-catalog.md` se mantiene sin cambios (no hubo desviaciones de contrato).
- Hallazgos relevantes: ninguno de comportamiento. La interrupción previa no dejó trabajo a medias en
  código; el contenido de las 12 recetas se validó íntegro contra la Taxonomía (referencias,
  alérgenos declarados y coherencia de tiempos) sin correcciones.
- Riesgo o incógnita abierta: ninguno nuevo. Se mantiene el riesgo de calendario ya registrado (las
  recetas semilla son contenido, no código) y el crecimiento del catálogo queda como contenido (el test
  exige `>= 12`).
- Siguiente paso: validación independiente de `seed-recipe-catalog` con `feature-validator`.

### Sesión 021

- Fecha: 2026-10-07
- Objetivo: validar `seed-recipe-catalog` de forma independiente y persistir la aceptación.
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la aceptación independiente. Sin cambios de código tras la
  validación.
- Verificación ejecutada: el validador re-ejecutó `init.sh` con Git Bash (exit 0), `./gradlew
  testDebugUnitTest --rerun-tasks` (32/32 tareas), `./gradlew assembleDebug`, parseó por su cuenta los
  XML de tests y el informe de lint, e inspeccionó el APK como ZIP; re-verificó los escenarios 1–6 con
  su propio parser de JSON y PowerShell contra `docs/domain/taxonomy.json`.
- Evidencia capturada: 51 tests, 0 fallos/errores/saltados (`SeedRecipeCatalogTest` 6,
  `AssetSeedRecipeSourceTest` 1, `RecipeValidatorTest` 12, `RoomProfileRepositoryTest` 9,
  `PreferenceSignatureTest` 8, `TaxonomyIntegrityTest` 5, `FakeRecipeGeneratorTest` 6,
  `RecipeSerializationTest` 4); lint 0 errores y 21 warnings preexistentes, 0 en ficheros de la feature;
  el validador confirmó con su propio parser 12 recetas, ids únicos `seed-*`, `source=seed`,
  `matchScore=null`, todas las referencias resueltas, `declaredAllergens ⊆ hardRestrictions`,
  `servings` en 1..12, coherencia de tiempos y cobertura de alérgenos, con 0 violaciones; el caso
  negativo falla de verdad (mutar `categoryId` → 1 violación; quitar `gluten` → 2 violaciones);
  `assets/seed-recipes.json` (20710 bytes) está en el APK y no está ignorado en git; `domain.recipe` sin
  imports Android.
- Hallazgos del validador (2 Low, informativos, ninguno bloqueante): el test de "apta vegana" usa un
  `ANIMAL_TAGS` fijo en vez de derivarlo de `dietPatterns[vegan].excludedTags` (hoy idénticos); el
  helper `violations(...)` es solo de test, coherente con la decisión de la spec de no pasar el catálogo
  por el `RecipeValidator` en ejecución.
- Riesgo o incógnita abierta: ninguno nuevo. Se mantiene el riesgo de calendario (el catálogo es
  contenido) y el crecimiento del catálogo (el test exige `>= 12`).
- Siguiente paso: commit local de la rama `feature/seed-recipe-catalog` (lo hace el orquestador de
  `feature-flow`); la publicación queda para `github-flow`. Después, `suggestion-batch-cache` (depende
  de `preference-signature`, `fake-recipe-generator` y `seed-recipe-catalog`, todas ya `accepted`) o las
  features de UI que consume el catálogo.

### Sesión 022

- Fecha: 2026-10-07
- Objetivo: implementar `design-tokens-and-theme` (tokens de `DESIGN.md` + tema claro/oscuro sin color
  dinámico) según `docs/specs/design-tokens-and-theme.md`.
- Completado: paquete `com.sazon.app.ui.theme` como única fuente de verdad visual. `Color.kt`
  reescrito con la paleta de `DESIGN.md` y `LightColorScheme`/`DarkColorScheme` con **todos** los roles
  sobrescritos (sin purple/pink por defecto; modo oscuro anclado a `darkBackground`/`darkSurface` con
  acentos terracota/oliva y texto crema). `Type.kt` con `SazonFontFamilies` (Manrope/Inter) y
  `SazonTypography` (display 32/700, h1 28/600, body 16/400, caption 12/500). `Shape.kt` (`SazonShapes`
  16/24 + `SazonPillShape`) y `Spacing.kt` (`SazonSpacing` 12/16/24 y `SazonComponents`). `Theme.kt`
  reescrito **sin** `dynamicColor`/`LocalContext` y cableando color, tipografía y formas. Sistema
  alineado: `values/colors.xml` (solo `sazon_window_background` #FFF8F0), `values-night/colors.xml`
  (#241C16), `themes.xml` y `values-night/themes.xml` (`windowBackground` + `forceDarkAllowed=false`),
  `android:label` quitado de la actividad y `Surface` de `MainActivity` pintada con
  `colorScheme.background`. Añadida la propiedad de sistema `design.tokens.path` en
  `app/build.gradle.kts` y `DesignTokensTest` (JVM, 6 tests, con casos negativos de deriva de `primary`
  y de paleta de plantilla). Las fuentes OFL se obtuvieron de las variables oficiales de Google Fonts y
  se instanciaron a estáticas con `fontTools` (`manrope_semibold/bold`, `inter_regular/medium`);
  licencias en `docs/licenses/` y regla `*.ttf binary` en `.gitattributes`.
- Verificación ejecutada: `& "C:\Program Files\Git\bin\bash.exe" init.sh` (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos); antes, `./gradlew testDebugUnitTest --tests
  "com.sazon.app.ui.theme.DesignTokensTest"` (enfocado) y parseo propio de los XML de resultados y del
  informe `lint-results-debug.xml`.
- Evidencia capturada: **67 tests, 0 fallos/errores/saltados** (`DesignTokensTest` 6,
  `RecipeValidatorTest` 12, `PromptBuilderTest` 10, `RoomProfileRepositoryTest` 9,
  `PreferenceSignatureTest` 8, `FakeRecipeGeneratorTest` 6, `SeedRecipeCatalogTest` 6,
  `TaxonomyIntegrityTest` 5, `RecipeSerializationTest` 4, `AssetSeedRecipeSourceTest` 1); lint 0 errores
  y **13 warnings** (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2,
  OldTargetApi 1): desaparecen los 7 `UnusedResources` de colores de plantilla y el `RedundantLabel`
  (21→13), sin issues en ficheros de la feature; `init.sh` con código 0. Inspección: no queda ninguna
  ruta de color dinámico ni referencia a purple/pink/teal en `app/src/main`; las cuatro fuentes `.ttf`
  están en `res/font/` y `values-night/` existe con `colors.xml` y `themes.xml`.
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/ui/theme/{Color,Type,Theme,Shape,Spacing}.kt`,
  `app/src/main/res/font/{manrope_semibold,manrope_bold,inter_regular,inter_medium}.ttf`,
  `app/src/main/res/values/colors.xml`, `app/src/main/res/values-night/{colors,themes}.xml`,
  `app/src/main/res/values/themes.xml`, `app/src/main/AndroidManifest.xml`,
  `app/src/main/java/com/sazon/app/MainActivity.kt`, `app/build.gradle.kts`,
  `app/src/test/java/com/sazon/app/ui/theme/DesignTokensTest.kt`, `docs/licenses/{manrope,inter}-OFL.txt`,
  `.gitattributes`, `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`,
  `docs/specs/design-tokens-and-theme.md` (hallazgos), `feature_list.json` y este registro.
- Hallazgos relevantes: la spec cita un "riesgo 1" y un plan de respaldo inexistentes; se resolvió sin
  respaldo instanciando las fuentes variables oficiales con `fontTools`. `colors.text` se mapea a
  `onBackground` y `onSurface`. Se añadió `SazonOnAccent` explícito y `*.ttf binary` en `.gitattributes`.
  La primera compilación del test falló por usos de API de Compose (`toArgb` necesita import,
  `topStart` es `CornerSize` no `Dp`, `TextStyle.fontWeight` es nullable); corregido en el test, sin
  tocar el código de main.
- Riesgo o incógnita abierta: ninguno nuevo. Los roles oscuros derivados (`*Container`, `inverse*`,
  `surfaceVariant`, `outlineVariant`) y el repaso visual del modo oscuro/contraste quedan para revisión
  manual de la persona tras el merge (la spec lo asume; un defecto visual se corrige con feature
  correctiva). Quedan sin ejecutar las dos pruebas manuales de emulador (fondo de pantalla y modo
  oscuro), que AGENTS.md reserva a la persona.
- Siguiente paso: validación independiente de `design-tokens-and-theme` con `feature-validator`.

### Sesión 023

- Fecha: 2026-10-07
- Objetivo: validar `design-tokens-and-theme` de forma independiente y persistir la aceptación.
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la aceptación independiente. Sin cambios de código tras la
  validación.
- Verificación ejecutada: el validador re-ejecutó `init.sh` con Git Bash (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos), `./gradlew testDebugUnitTest --rerun-tasks` (32/32 tareas) y
  `./gradlew lint --rerun-tasks`; parseó por su cuenta los XML de resultados y el informe de lint, e
  inspeccionó los recursos y las fuentes directamente.
- Evidencia capturada: 67 tests, 0 fallos/errores/saltados (`DesignTokensTest` 6,
  `RecipeValidatorTest` 12, `PromptBuilderTest` 10, `RoomProfileRepositoryTest` 9,
  `PreferenceSignatureTest` 8, `FakeRecipeGeneratorTest` 6, `SeedRecipeCatalogTest` 6,
  `TaxonomyIntegrityTest` 5, `RecipeSerializationTest` 4, `AssetSeedRecipeSourceTest` 1); lint 0 errores
  y 13 warnings (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2,
  OldTargetApi 1), sin issues en ficheros de la feature y con la desaparición de los 7 `UnusedResources`
  de plantilla y del `RedundantLabel`; el validador comprobó los escenarios 1, 2, 4 y 5, que el caso
  negativo falla de verdad, la ausencia de rutas `dynamicColor`/`LocalContext` y de purple/pink/teal,
  que los `.ttf` no están ignorados por git y llevan OFL real, y la disciplina de alcance.
- Ficheros actualizados: `feature_list.json` y este registro.
- Hallazgos del validador (4 Low, informativos, ninguno bloqueante): nombres internos `nameID` obsoletos
  en los `.ttf` pese a `OS/2.usWeightClass` correcto (el render es correcto); cobertura parcial de roles
  en la comprobación negativa de plantilla; precisión de un enunciado de evidencia; `Validator Checklist`
  de la spec sin marcar.
- Riesgo o incógnita abierta: ninguno nuevo. Quedan para revisión manual de la persona tras el merge el
  repaso visual del modo oscuro/contraste y las dos pruebas de emulador (fondo de pantalla y modo
  oscuro); un defecto visual se corrige con feature correctiva.
- Siguiente paso: commit local de la rama `feature/design-tokens-and-theme` (lo hace el orquestador de
  `feature-flow`); la publicación queda para `github-flow`. Después, `onboarding-shell-and-identity`
  (depende de `profile-persistence` y `taxonomy-in-code`, ya `accepted`) o `suggestion-batch-cache`.

### Sesión 024

- Fecha: 2026-10-08
- Objetivo: reanudar e implementar `onboarding-shell-and-identity` (armazón de onboarding + paso 1 de
  identidad + puerta de primer arranque) según `docs/specs/onboarding-shell-and-identity.md`. La
  ejecución previa dejó el trabajo sin commitear; esta sesión lo revisa, completa lo que faltaba
  (documentación) y lo autoverifica sin rehacer lo ya válido.
- Completado: `OnboardingStep` (7 pasos en orden con `title`/`nextLabel`, `TOTAL_STEPS = 7`),
  `OnboardingProgress` (máquina de pasos pura con `stepNumber`, `step`, `canGoBack`, `isLastStep`,
  `next()`, `back()` sin salirse de rango y `progressLabel = "Paso N de 7"`), `AvatarInitials.of`
  (máx. 2 iniciales en mayúsculas; `null` si el nombre queda vacío) y `OnboardingIdentityState`
  (`normalizedName`/`isNameValid`/`canProceed` vía `ProfileName`, `initials` vía `AvatarInitials` y
  `initial(taxonomy)` con el primer `avatarColors`, `terracotta`). Puerta pura `AppDestination` +
  `AppGate.decide(profile)`. Capa Compose: `OnboardingScaffold` (progreso con puntos, "Atrás" y
  primario terracota anclado), `AvatarPreview` (iniciales o icono genérico sobre el color), 
  `OnboardingIdentityScreen` (microcopy del `stitch-prompt`, campo obligatorio, 8 muestras y vista
  previa), `OnboardingFlow` (`rememberSaveable`, placeholders 2–7, costura `onFinished`),
  `SazonApp` + `HomePlaceholder` y `MainActivity` cableado con `ProfileDatabase` +
  `RoomProfileRepository` + `AssetTaxonomySource`. Sin persistir Perfil, sin DataStore, sin
  `navigation-compose` ni DI, sin Home real ni Perfil editable (no-goals).
- Verificación ejecutada: `& "C:\Program Files\Git\bin\bash.exe" init.sh` (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos); `./gradlew testDebugUnitTest --rerun-tasks` con parseo propio
  de los XML de resultados; parseo y agrupación del informe `lint-results-debug.xml`; comprobación de
  la ausencia de `material-icons` en `:app:dependencies --configuration debugRuntimeClasspath`.
- Evidencia capturada: **85 tests, 0 fallos/errores/saltados**; 18 nuevos de esta feature
  (`OnboardingProgressTest` 5, `AvatarInitialsTest` 5, `OnboardingIdentityStateTest` 6,
  `AppGateTest` 2) más los 67 preexistentes; lint 0 errores y 13 warnings preexistentes
  (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), 0 en
  ficheros de la feature; `init.sh` con código 0; `app/build.gradle.kts` y
  `gradle/libs.versions.toml` sin cambios; la lógica pura de `ui.onboarding`/`ui.AppGate` no importa
  Compose ni Android y corre en JVM.
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/ui/onboarding/{OnboardingStep,
  OnboardingProgress, AvatarInitials, OnboardingIdentityState, OnboardingScaffold,
  OnboardingIdentityScreen, AvatarPreview, OnboardingFlow}.kt`,
  `app/src/main/java/com/sazon/app/ui/{AppGate, SazonApp, HomePlaceholder}.kt`,
  `app/src/main/java/com/sazon/app/MainActivity.kt`,
  `app/src/test/java/com/sazon/app/ui/AppGateTest.kt`,
  `app/src/test/java/com/sazon/app/ui/onboarding/{OnboardingProgressTest, AvatarInitialsTest,
  OnboardingIdentityStateTest}.kt`, `docs/technical-discovery.md`,
  `docs/specs/onboarding-shell-and-identity.md` (hallazgo), `feature_list.json` y este registro.
- Hallazgos relevantes: el proyecto no arrastra `material-icons-core` (verificado en el classpath de
  runtime) y la spec prohíbe añadir dependencias, así que `AvatarPreview` dibuja el icono genérico de
  persona con primitivas de `Canvas` en vez de `Icons.Default.Person`; documentado como hallazgo en la
  spec. `SazonApp` usa `produceState` en vez de `collectAsState(initial = null)`, equivalente.
- Riesgo o incógnita abierta: quedan las dos pruebas manuales de emulador ("Empezar" deshabilitado sin
  nombre; "Paso 1 de 7" y "Atrás" no disponible), que AGENTS.md reserva a la persona tras el merge. La
  reanudación de un onboarding interrumpido sigue fuera de alcance (requiere persistir el paso).
- Siguiente paso: validación independiente de `onboarding-shell-and-identity` con `feature-validator`.
  Después, commit local de la rama (lo hace el orquestador de `feature-flow`) y publicación por
  `github-flow`.

### Sesión 025

- Fecha: 2026-10-08
- Objetivo: validar `onboarding-shell-and-identity` de forma independiente y persistir la aceptación.
  Sesión reanudada tras una interrupción de la ejecución previa (la feature quedó implementada sin
  commitear); el worktree se sincronizó con `origin/main` antes de continuar.
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la validación independiente. Sin cambios de código tras la
  validación.
- Verificación ejecutada: el validador re-ejecutó `init.sh` con Git Bash (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos), `./gradlew testDebugUnitTest --rerun-tasks` (32/32 tareas) y
  `./gradlew lint --rerun-tasks`; parseó por su cuenta los XML de resultados y el informe de lint, e
  inspeccionó el código y la microcopy contra `docs/design/stitch-prompt.md`.
- Evidencia capturada: 85 tests, 0 fallos/errores/saltados (18 nuevos: `OnboardingProgressTest` 5,
  `AvatarInitialsTest` 5, `OnboardingIdentityStateTest` 6, `AppGateTest` 2); lint 0 errores y 13
  warnings preexistentes (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2,
  OldTargetApi 1), 0 en ficheros de la feature; escenarios 1–6 y `Validator Checklist` en PASS;
  disciplina de alcance confirmada (sin pasos reales 2–7, sin persistir Perfil, sin Home real, sin
  barra/FAB, sin DataStore, sin `navigation-compose` ni DI); `app/build.gradle.kts` y
  `gradle/libs.versions.toml` sin cambios; lógica pura de `ui.onboarding`/`ui.AppGate` sin imports
  Android/Compose; decisiones documentadas verificadas (puerta por presencia de Perfil, navegación
  mínima, `AvatarPreview` con `Canvas`, `SazonApp` con `produceState`).
- Ficheros actualizados: `feature_list.json` y este registro.
- Hallazgos del validador (2 Low + 2 informativos, ninguno bloqueante): el fallback defensivo del `hex`
  es inconsistente entre las muestras de color (`OnboardingIdentityScreen`) y `AvatarPreview`, hoy no
  alcanzable (la Taxonomía valida el formato) y anotado para una feature correctiva; el
  `Validator Checklist` de la spec queda sin marcar (por diseño, codifica las comprobaciones del
  validador); la carga del asset en el hilo principal sigue como *Unknown* de la spec; el trabajo sin
  commitear es el comportamiento esperado de `feature-flow`.
- Riesgo o incógnita abierta: ninguno nuevo. Quedan para revisión manual de la persona tras el merge
  las dos pruebas de emulador ("Empezar" deshabilitado sin nombre; "Paso 1 de 7" y "Atrás" no
  disponible) y el acabado visual (muestras, contraste, claro/oscuro); un defecto visual se corrige
  con una feature correctiva.
- Siguiente paso: commit local de la rama `feature/onboarding-shell-and-identity` y publicación por
  `github-flow` (push + PR). Después, `onboarding-diet-style-cuisines` o `suggestion-batch-cache`.

### Sesión 026

- Fecha: 2026-10-08
- Objetivo: implementar `onboarding-diet-style-cuisines` (pasos 2–4 reales del onboarding: dieta,
  estilo y cocinas) según `docs/specs/onboarding-diet-style-cuisines.md`, sobre el armazón aceptado en
  `onboarding-shell-and-identity`.
- Completado: borrador **puro** `com.sazon.app.ui.onboarding.OnboardingPreferencesDraft`
  (`dietPatternId` single-select con `initial(taxonomy)` = primer `dietPatterns`, `softPreferenceIds`
  y `cuisineIds` como conjuntos sin duplicados, `selectDietPattern`, `toggleSoftPreference`,
  `toggleCuisine`, `clearSoftPreferences` e `isStyleNone` = lista vacía). Tres editores reutilizables
  y **sin estado** en `com.sazon.app.ui.preferences`: `DietPatternEditor` (tarjetas 24dp con
  `label`+`description`), `SoftPreferencesEditor` (chips píldora + helper "Opcional..." + chip
  "Ninguno" seleccionado cuando la lista está vacía) y `CuisinesEditor` (rejilla de 2 columnas de
  tiles, objetivo táctil ≥64dp). Cableado en `OnboardingFlow`: borrador con `rememberSaveable` y
  `OnboardingPreferencesDraftSaver` (Saver en la capa Compose, formato
  `[dietPatternId, softCount, ...soft, ...cuisines]`), enrutado de `DIET`/`STYLE`/`CUISINES` a los
  editores con las listas de la Taxonomía, enunciado del paso en el flujo (los editores no conocen el
  onboarding), y pasos 5–7 como marcadores; `primaryEnabled` sigue siendo `true` en los pasos 2–4 y
  `onFinished` sigue sin efecto. `OnboardingPreferencesDraftTest` con los escenarios 1–8 y casos
  negativos. Sin dependencias, sin persistir Perfil, sin DataStore, sin iconos, sin navegación ni DI
  (no-goals).
- Verificación ejecutada: `& "C:\Program Files\Git\bin\bash.exe" init.sh` (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos); parseo propio de los XML de resultados y del informe
  `lint-results-debug.xml`; inspección de imports y de que no cambian `app/build.gradle.kts` ni
  `gradle/libs.versions.toml`.
- Evidencia capturada: **93 tests, 0 fallos/errores/saltados** (8 nuevos:
  `OnboardingPreferencesDraftTest` 8; el resto son los 85 preexistentes); lint 0 errores y 13 warnings
  preexistentes (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2,
  OldTargetApi 1), 0 en ficheros de la feature; `init.sh` con código 0; `OnboardingPreferencesDraft`
  sin imports de Compose/Android (solo `domain.taxonomy`); los editores no usan `remember`/
  `mutableState`; sin hex sueltos ni iconos.
- Ficheros creados/cambiados:
  `app/src/main/java/com/sazon/app/ui/onboarding/OnboardingPreferencesDraft.kt`,
  `app/src/main/java/com/sazon/app/ui/preferences/{DietPatternEditor,SoftPreferencesEditor,
  CuisinesEditor}.kt`, `app/src/main/java/com/sazon/app/ui/onboarding/OnboardingFlow.kt`,
  `app/src/test/java/com/sazon/app/ui/onboarding/OnboardingPreferencesDraftTest.kt`,
  `docs/technical-discovery.md`, `feature_list.json` y este registro. No se tocaron
  `app/build.gradle.kts` ni `gradle/libs.versions.toml`.
- Hallazgos relevantes: ninguno de comportamiento. El enunciado del paso (`step.title`) se pinta en
  `OnboardingFlow` para mantener los editores agnósticos del onboarding (reutilizables por Perfil).
- Riesgo o incógnita abierta: la persistencia entre reinicios queda fuera de alcance por diseño (la
  verifica `onboarding-ingredients-safety-kitchen`); el borrador sobrevive a cambios de
  configuración/proceso dentro de la sesión. Queda para revisión manual de la persona el acabado
  visual de los pasos 2–4 (tarjetas, chips, rejilla, claro/oscuro) y que las opciones coincidan con la
  Taxonomía conservando la selección al ir y volver (AGENTS.md no automatiza UI de emulador).
- Siguiente paso: validación independiente de `onboarding-diet-style-cuisines` con
  `feature-validator`; después, commit local (orquestador de `feature-flow`) y publicación por
  `github-flow`.

### Sesión 027

- Fecha: 2026-10-08
- Objetivo: validar `onboarding-diet-style-cuisines` de forma independiente y persistir la aceptación.
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la validación independiente. Sin cambios de código tras la
  validación.
- Verificación ejecutada: el validador re-ejecutó `init.sh` con Git Bash (exit 0), `./gradlew
  testDebugUnitTest --rerun-tasks` (32/32 tareas) y `./gradlew lint --rerun-tasks` (28/28 tareas);
  parseó por su cuenta los XML de resultados y el informe de lint, e inspeccionó el código (opciones
  desde la Taxonomía, editores sin estado, ausencia de hex/iconos/persistencia).
- Evidencia capturada: 93 tests, 0 fallos/errores/saltados (nuevo: `OnboardingPreferencesDraftTest` 8;
  85 preexistentes); lint 0 errores y 13 warnings preexistentes (GradleDependency 7,
  NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), 0 en ficheros de la feature;
  escenarios 1–8 y `Validator Checklist` en PASS; opciones exactamente de
  `dietPatterns`/`softPreferences`/`cuisines` sin listas duplicadas ni labels hardcodeados; editores
  sin estado en `ui.preferences` reutilizables por `profile-screen`; sin persistencia; build y lockfile
  intactos; `OnboardingStep`/`OnboardingScaffold`/`SazonApp`/`AppGate`/`MainActivity` no tocados.
- Ficheros actualizados: `feature_list.json` y este registro.
- Hallazgos del validador (3 Low informativos, ninguno bloqueante): la retención del `OnboardingPreferencesDraftSaver`
  no tiene test de round-trip (correcto por inspección; la retención Compose es revisión manual); los
  conteos 5/7+1/10 no se asertan en el test porque se garantizan estructuralmente (Taxonomía + su
  `TaxonomyIntegrityTest`); `Validator Checklist` de la spec sin marcar.
- Riesgo o incógnita abierta: ninguno nuevo. Queda para revisión manual de la persona tras el merge el
  acabado visual de los pasos 2–4 y la persistencia entre reinicios (fuera de alcance por diseño; la
  verifica `onboarding-ingredients-safety-kitchen`).
- Siguiente paso: commit local de la rama `feature/onboarding-diet-style-cuisines` y publicación por
  `github-flow` (push + PR). Después, `onboarding-ingredients-safety-kitchen` o `suggestion-batch-cache`.

### Sesión 028

- Fecha: 2026-10-08
- Objetivo: implementar `onboarding-ingredients-safety-kitchen` (pasos 5–7 reales del onboarding y
  cierre que persiste el `UserProfile`) según `docs/specs/onboarding-ingredients-safety-kitchen.md`,
  la feature que cierra el onboarding.
- Completado:
  - Borrador **puro** `OnboardingPreferencesDraft` ampliado con `preferredIngredientIds`,
    `dislikedIngredientIds`, `hardRestrictionIds`, `difficultyId`, `timeBucketId` y `defaultServings`;
    operaciones inmutables (`copy`) con **exclusión mutua** de ingredientes (`togglePreferredIngredient`/
    `toggleDislikedIngredient`), restricciones con `toggleHardRestriction`/`clearHardRestrictions`/
    `hasNoRestrictions`, y cocina con `selectDifficulty`/`selectTimeBucket`/`setServings(limits)`
    (recorte a `min..max`); `initial(taxonomy)` añade `easy`, `lte_15` y `servings.default` (2).
  - Constructor **puro** `OnboardingProfileBuilder.build(displayName, avatarColorId, draft, now)`:
    valida/normaliza con `ProfileName`, inyecta `now` en `createdAt`/`updatedAt` y copia todos los
    campos a `PreferenceProfile`; nombre inválido → `IllegalArgumentException`.
  - Filtro **puro** `IngredientsFilter.matching(ingredients, query)`: subcadena por `label`, sin
    distinguir mayúsculas ni acentos; consulta en blanco = todas.
  - Cinco editores **reutilizables y sin estado** en `ui.preferences`: `IngredientsEditor` (buscador +
    grupos "Me gusta" terracota y "Evito" oliva con helper y nota de compartición), `HardRestrictionsEditor`
    (banner ámbar + chips multi-select + "Ninguna"; rol `tertiary`, nunca error/rojo),
    `DifficultyEditor`, `TimeBucketEditor` y `ServingsStepper` (`− value +` con cifras tabulares y
    límites deshabilitados).
  - `OnboardingFlow`: Saver ampliado a todos los campos (cada lista como `[conteo, ...elementos]`),
    enrutado de `INGREDIENTS`/`RESTRICTIONS`/`KITCHEN` y `onFinished` cambiado a
    `(UserProfile) -> Unit`; el Perfil se construye con `System.currentTimeMillis()`.
  - `SazonApp`: `rememberCoroutineScope` + `ProfileRepository.save(profile)` en el `onFinished`; la
    observación existente hace que `AppGate.decide` pase a `MAIN` y el onboarding desaparezca.
  - Tests JVM con casos negativos: `OnboardingIngredientsDraftTest` (escenarios 1–9),
    `OnboardingProfileBuilderTest` (escenarios 10 y 12) e `IngredientsFilterTest`. Sin dependencias
    nuevas: no se tocan `app/build.gradle.kts` ni `gradle/libs.versions.toml`.
- Verificación ejecutada: `& "C:\Program Files\Git\bin\bash.exe" init.sh` (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos); parseo propio de los XML de resultados y del informe
  `lint-results-debug.xml`; inspección de imports de la lógica pura, de la ausencia de estado en los
  editores, de que `HardRestrictionsEditor` no usa el rol `error` y de que el build/lockfile no cambian.
- Evidencia capturada: **110 tests, 0 fallos/errores/saltados** (17 nuevos:
  `OnboardingIngredientsDraftTest` 9, `OnboardingProfileBuilderTest` 4, `IngredientsFilterTest` 4; 93
  preexistentes, todos en verde, incluidos `OnboardingPreferencesDraftTest`, `OnboardingIdentityStateTest`,
  `OnboardingProgressTest` y `RoomProfileRepositoryTest`); lint 0 errores y 13 warnings preexistentes
  (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), 0 en
  ficheros de la feature; `init.sh` con código 0; `OnboardingPreferencesDraft` y
  `OnboardingProfileBuilder` sin imports Compose/Android; editores sin `remember`/`mutableState`.
- Ficheros creados/cambiados:
  `app/src/main/java/com/sazon/app/ui/onboarding/{OnboardingPreferencesDraft,OnboardingFlow}.kt`
  (modificados), `app/src/main/java/com/sazon/app/ui/onboarding/OnboardingProfileBuilder.kt` (nuevo),
  `app/src/main/java/com/sazon/app/ui/preferences/{IngredientsEditor,HardRestrictionsEditor,
  DifficultyEditor,TimeBucketEditor,ServingsStepper,IngredientsFilter}.kt` (nuevos),
  `app/src/main/java/com/sazon/app/ui/SazonApp.kt` (modificado),
  `app/src/test/java/com/sazon/app/ui/onboarding/{OnboardingIngredientsDraftTest,
  OnboardingProfileBuilderTest}.kt` y `app/src/test/java/com/sazon/app/ui/preferences/IngredientsFilterTest.kt`
  (nuevos), `docs/technical-discovery.md`, `feature_list.json` y este registro. No se tocaron
  `app/build.gradle.kts` ni `gradle/libs.versions.toml`.
- Hallazgos relevantes: ninguno de comportamiento; el contrato de la spec compiló con las APIs reales
  del repositorio (`ProfileName.normalize`/`isValid`, `ProfileRepository.save`, las firmas de la
  Taxonomía y `PreferenceProfile`), sin adaptaciones. `primaryEnabled` se mantiene ligado a la validez
  del nombre del paso 1 para todo el flujo, de modo que un nombre inválido deja el primario
  deshabilitado también en el último paso (escenario 12), coherente con el borrador.
- Riesgo o incógnita abierta: quedan para revisión manual de la persona tras el merge las tres pruebas
  de emulador (elegir en "Me gusta" quita de "Evito" y el buscador filtra; chips de alérgenos en ámbar
  y nunca en rojo; el onboarding no reaparece al reabrir) y el acabado visual de los pasos 5–7; un
  defecto visual se corrige con una feature correctiva. El round-trip del `Saver` ampliado no se testea
  en JVM (vive en la capa Compose); se verifica por inspección y retención manual, como en los pasos 2–4.
- Siguiente paso: validación independiente de `onboarding-ingredients-safety-kitchen` con
  `feature-validator`; después, commit local (orquestador de `feature-flow`) y publicación por
  `github-flow`. La siguiente feature sin empezar es `suggestion-batch-cache`.

### Sesión 029

- Fecha: 2026-10-08
- Objetivo: validar `onboarding-ingredients-safety-kitchen` de forma independiente y persistir la
  aceptación.
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la validación independiente. Sin cambios de código tras la
  validación. Con esta feature el onboarding de 7 pasos queda **completo**.
- Verificación ejecutada: el validador re-ejecutó `init.sh` con Git Bash (exit 0), `./gradlew
  testDebugUnitTest --rerun-tasks` (32/32 tareas) y `./gradlew lint --rerun-tasks` (28/28 tareas);
  parseó por su cuenta los XML de resultados y el informe de lint, e inspeccionó el código (exclusión
  mutua, ámbar sin rojo, recorte del stepper, cierre que persiste, pureza de la lógica).
- Evidencia capturada: 110 tests, 0 fallos/errores/saltados (nuevos: `OnboardingIngredientsDraftTest`
  9, `OnboardingProfileBuilderTest` 4, `IngredientsFilterTest` 4; 93 preexistentes); lint 0 errores y
  13 warnings preexistentes (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2,
  OldTargetApi 1), 0 en ficheros de la feature; escenarios 1–12 y `Validator Checklist` en PASS;
  exclusión mutua "Me gusta"/"Evito" con intersección vacía; restricciones con "Ninguna" sin residuo;
  chips de alérgenos en `tertiary`/`tertiaryContainer` y nunca `error`; stepper recortado a 1..12;
  opciones exactamente de la Taxonomía (95 ingredientes, 11 restricciones, 3 dificultades, 5 franjas,
  `servings` 1..12 default 2); cierre que persiste el `UserProfile` y pasa `AppGate` a `MAIN`; build y
  lockfile intactos.
- Ficheros actualizados: `feature_list.json` y este registro.
- Hallazgos del validador (2 Low + 2 informativos, ninguno bloqueante): `ServingsStepper` usa
  `Arrangement.spacedBy(16.dp)` en vez de `SazonSpacing.md`; comentario obsoleto en `OnboardingStep.kt`
  (los pasos 2–7 ya no son marcadores); el round-trip del `Saver` ampliado no se testea en JVM (capa
  Compose, manual); densidad visual del paso 5 (95 chips en dos grupos), mitigada por el buscador.
- Riesgo o incógnita abierta: ninguno nuevo. Quedan para revisión manual de la persona tras el merge
  las tres pruebas de emulador y el acabado visual de los pasos 5–7.
- Siguiente paso: commit local de la rama `feature/onboarding-ingredients-safety-kitchen` y publicación
  por `github-flow` (push + PR). Después, `suggestion-batch-cache`.

### Sesión 030

- Fecha: 2026-10-08
- Objetivo: implementar `suggestion-batch-cache` (Lote de sugerencias como caché persistida y
  cache-first) según `docs/specs/suggestion-batch-cache.md`.
- Completado: paquete puro `com.sazon.app.domain.suggestions` con `SuggestionBatch`
  (`preferenceSignature`, `generatedAt`, recetas y `recipeIds`), `BatchRefreshReason`, `BatchDecision`,
  `SuggestionBatchPolicy.decide(...)` (con `BATCH_TTL_MILLIS` = 7 días en un único sitio y frontera
  `elapsed >= TTL`) y el puerto `SuggestionBatchRepository` (`get`/`observe`/`save`/`clear`).
  `SuggestionBatchService.ensureBatch(profile, force)` es cache-first: consume
  `PreferenceSignature.of(profile)`, consulta la política y solo genera cuando toca; al generar,
  mapea el Perfil a `RecipeConstraints`, filtra las recetas `ai` con `RecipeValidator.validateRecipe`
  (aceptadas entran, descartadas no), deduplica por título normalizado (minúsculas, sin acentos con
  `Normalizer`, `trim`; gana la `seed`) y persiste. Si el generador **falla**, degrada a un lote
  seed-only persistido sin propagar, re-lanzando `CancellationException` (no `runCatching`). Añadido
  aditivo a `RecipeJson`: `encodeList`/`parseList` con `ListSerializer(Recipe.serializer())` reusando
  la `format`. Capa `com.sazon.app.data.suggestions` sobre Room en base **propia**
  (`SuggestionBatchDatabase`, `suggestion_batch.db`, versión 1, una fila `SINGLETON_ID = 1` con
  `INSERT OR REPLACE`, `Flow` para observar y `DELETE` para `clear`): `SuggestionBatchEntity`,
  `SuggestionBatchDao`, `SuggestionBatchMappers` (entidad ↔ dominio y (de)serialización de la lista) y
  `RoomSuggestionBatchRepository`. Sin UI y sin composición en la app (la monta `home-offline-blocks`),
  sin dependencias nuevas y sin tocar `ProfileDatabase`, `data.profile/**`, `domain.profile/**`,
  `domain.generator/**`, la Taxonomía, `app/build.gradle.kts`, `gradle/libs.versions.toml` ni la UI.
- Verificación ejecutada: `& "C:\Program Files\Git\bin\bash.exe" init.sh` (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos); parseo propio de los XML de resultados
  (`test-results/testDebugUnitTest/TEST-*.xml`) y del informe `lint-results-debug.xml`; inspección de
  imports de `domain.suggestions` (sin `android.*`/`androidx.*`) y de `git status` (solo cambia
  `RecipeJson.kt`; el resto son ficheros nuevos).
- Evidencia capturada: **132 tests, 0 fallos/errores/saltados** en 21 suites (22 nuevos:
  `SuggestionBatchPolicyTest` 8, `SuggestionBatchServiceTest` 9 y
  `RoomSuggestionBatchRepositoryTest` 5; 110 preexistentes en verde); lint 0 errores y 13 warnings
  preexistentes (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2,
  OldTargetApi 1), 0 en ficheros de la feature; `init.sh` con código 0; `domain.suggestions` sin
  imports Android; `git diff --stat` solo lista `RecipeJson.kt` (+10, aditivo) y `feature_list.json`;
  el bundle de la base (`sazon.db` del Perfil) y el build quedan intactos.
  Cobertura de escenarios: 1 (firma igual y fresco → 0 llamadas al generador y 0 `save`), 2
  (`SIGNATURE_CHANGED`, firma nueva persistida), 3 (`EXPIRED`), 4 (frontera: un milisegundo antes
  `UseCached`, en el TTL exacto `EXPIRED`, en política y en servicio), 5 (generador que lanza → lote
  solo `seed`, no vacío y persistido), 6 (`force` refresca aunque esté fresco), 7 (`MISSING` genera y
  persiste `seed` + `ai` con `generatedAt = now`), 8 (receta `ai` con "Pan" libre descartada por
  restricción de gluten; la `ai` segura entra junto a las semillas), 9 (duplicado seed/ai por título
  normalizado, gana la `seed`), 10 (ida y vuelta exacta con `nutrition`/`ingredientId` opcionales,
  una sola fila al sobrescribir, `clear()` → `null` y `observe()` emite).
- Ficheros creados/cambiados:
  `app/src/main/java/com/sazon/app/domain/suggestions/{SuggestionBatch,SuggestionBatchPolicy,
  SuggestionBatchRepository,SuggestionBatchService}.kt` (nuevos),
  `app/src/main/java/com/sazon/app/domain/recipe/RecipeJson.kt` (modificado, aditivo),
  `app/src/main/java/com/sazon/app/data/suggestions/{SuggestionBatchEntity,SuggestionBatchDao,
  SuggestionBatchDatabase,SuggestionBatchMappers,RoomSuggestionBatchRepository}.kt` (nuevos),
  `app/src/test/java/com/sazon/app/domain/suggestions/{SuggestionBatchPolicyTest,
  SuggestionBatchServiceTest}.kt`, `app/src/test/java/com/sazon/app/data/suggestions/
  RoomSuggestionBatchRepositoryTest.kt` (nuevos), `docs/technical-discovery.md`,
  `docs/domain-model.md`, `docs/risks-and-open-questions.md`, `docs/specs/suggestion-batch-cache.md`
  (hallazgos), `feature_list.json` y este registro.
- Hallazgos relevantes: las firmas de la spec compilaron con las APIs reales
  (`PreferenceSignature.of`, `RecipeGenerator.suggestByPreferences`,
  `RecipeValidator.validateRecipe`, `Taxonomy`, `Recipe`) sin adaptaciones de contrato. `generatedAt`
  se captura una sola vez tras decidir (la spec ilustraba `now()` dos veces); comportamiento
  observable idéntico con reloj fijo. `RecipeJson.encodeList/parseList` es aditivo y no cambia
  `encode`/`parse`.
- Riesgo o incógnita abierta: ninguno nuevo. Se mantiene la pregunta abierta 1 (modelo de errores del
  generador: el servicio trata cualquier `Exception` como IA no disponible) y el unknown de la spec
  (un lote seed-only persistido con la firma actual no se reintenta hasta que cambie la firma, caduque
  o el usuario fuerce); la deuda de consolidar `sazon.db` + `suggestion_batch.db` con una migración
  probada sigue anotada.
- Siguiente paso: validación independiente de `suggestion-batch-cache` con `feature-validator`;
  después, commit local (orquestador de `feature-flow`) y publicación por `github-flow`. La siguiente
  feature sin empezar es `home-offline-blocks`.

### Sesión 031

- Fecha: 2026-10-08
- Objetivo: validar `suggestion-batch-cache` de forma independiente y persistir la aceptación.
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la validación independiente. Sin cambios de código tras la
  validación. Queda desbloqueada `home-offline-blocks`.
- Verificación ejecutada: el validador re-ejecutó `init.sh` con Git Bash (exit 0), `./gradlew
  testDebugUnitTest --rerun-tasks` (32/32 tareas) y `./gradlew lint --rerun-tasks` (28/28 tareas);
  parseó por su cuenta los XML de resultados y el informe de lint, e inspeccionó el código (pureza,
  TTL único, frontera `>=`, cancelación, dedupe, base propia, alcance data-only).
- Evidencia capturada: 132 tests, 0 fallos/errores/saltados (nuevos: `SuggestionBatchPolicyTest` 8,
  `SuggestionBatchServiceTest` 9, `RoomSuggestionBatchRepositoryTest` 5; 110 preexistentes); lint 0
  errores y 13 warnings preexistentes, 0 en ficheros de la feature; escenarios 1–10 y `Validator
  Checklist` en PASS; escenario 1 sin llamada al generador ni `save`; `SIGNATURE_CHANGED`/`EXPIRED` y
  frontera del TTL en política y servicio; fallback a solo semilla no vacío y persistido; descarte de
  la `ai` insegura por `RecipeValidator` conservando la segura; dedupe con la seed ganadora;
  round-trip con una fila, `clear()` → `null` y `observe()` emite; `CancellationException` re-lanzada;
  dominio puro sin Android; sin cambios en build/`ProfileDatabase`/`data.profile`/UI.
- Ficheros actualizados: `feature_list.json` y este registro.
- Hallazgos del validador (2 Low + 2 informativos, ninguno bloqueante): redacción imprecisa del
  `git diff --stat` en una entrada de evidencia; aserción de `observe()` con carrera teórica
  (patrón heredado); `catch (Exception)` mapeado a "IA no disponible" como decisión explícita de la
  spec; `Validator Checklist` de la spec sin marcar.
- Riesgo o incógnita abierta: ninguno nuevo. Se mantienen la pregunta abierta 1 (modelo de errores del
  generador) y la deuda de consolidar `sazon.db` + `suggestion_batch.db` con migración probada.
- Siguiente paso: commit local de la rama `feature/suggestion-batch-cache` y publicación por
  `github-flow` (push + PR). Después, `home-offline-blocks`.

### Sesión 032

- Fecha: 2026-10-08
- Objetivo: implementar `home-offline-blocks` (Home real con sus cuatro bloques locales y armazón
  principal con barra inferior + FAB) según `docs/specs/home-offline-blocks.md`, y compone por primera
  vez el Lote de sugerencias en la app.
- Completado:
  - Lógica/estado **puro** en `com.sazon.app.ui.home`: `HomeGreeting.text(name, hour)` (bandas
    5..11/12..20/21..4 y fallback `"Buenas tardes"` sin nombre), `HomeIdentity.from(profile, taxonomy)`
    (hex del `avatarColorId` con respaldo al primer `avatarColors`; iniciales vía `AvatarInitials`),
    `HomeSuggestions.visible(batchRecipes, seedRecipes)` (`?:`, respaldo inmediato a semilla) y
    `HomeRecipes(saved, recentlyOpened)` con `isEmpty`; y `RecipeCardFormat.metaLine`/`matchBadge`.
  - Navegación **pura** en `com.sazon.app.ui.navigation`: `MainTab` (Inicio · Favoritos · Perfil),
    `PushedScreen` (GENERATOR) y `MainNavigationState` (`selectTab`/`push`/`pop` inmutables).
  - Capa Compose delgada: `HomeScreen` (bloques 1, 3, 4 y 5 en `LazyColumn`), `RecipeCard` (bloque
    degradado melocotón→oliva + glifo de plato con `Canvas`, `metaLine` y píldora de coincidencia
    opcional), `SectionPlaceholder` (Favoritos/Perfil/Generador con botón atrás) y `MainShell`
    (`Scaffold` con `NavigationBar` de 80dp y FAB "＋ Crear receta"; glifos casa/marca/persona
    dibujados con Compose; `Saver` de `MainNavigationState` para `rememberSaveable`).
  - Integración en la app: `MainActivity` monta `SuggestionBatchDatabase` +
    `RoomSuggestionBatchRepository` + `SuggestionBatchService(FakeRecipeGenerator())` + la Taxonomía y
    el catálogo semilla; `SazonApp` observa el `UserProfile` completo (con estado de carga propio para
    no pintar el onboarding antes de resolver) y monta `MainShell`. `MainShell` consume el Lote tal
    cual: `observe()` alimenta el carrusel y `ensureBatch` se llama una vez al entrar (cache-first),
    con `HomeSuggestions` de respaldo inmediato; la Home se pinta poblada sin pantalla de carga.
  - `ui/HomePlaceholder.kt` eliminado. Sin dependencias nuevas, sin `navigation-compose`, sin iconos ni
    imágenes externas; `domain.suggestions`/`data.suggestions` intactos.
- Verificación ejecutada: `& "C:\Program Files\Git\bin\bash.exe" init.sh` (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos); parseo propio de los XML de resultados y agrupación del
  informe `lint-results-debug.xml`; inspección de imports de la lógica pura y de que no cambian
  `app/build.gradle.kts` ni `gradle/libs.versions.toml`.
- Evidencia capturada: **159 tests, 0 fallos/errores/saltados** en 27 suites (27 nuevos:
  `HomeGreetingTest` 7, `MainNavigationStateTest` 6, `HomeIdentityTest` 4, `RecipeCardFormatTest` 4,
  `HomeRecipesTest` 3, `HomeSuggestionsTest` 3; 132 preexistentes en verde); lint 0 errores y 13
  warnings preexistentes (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2,
  OldTargetApi 1), 0 en ficheros de la feature; `init.sh` con código 0; `ui.navigation` sin imports
  Compose/Android y los cinco ficheros puros de `ui.home` tampoco (Compose solo en `HomeScreen`,
  `RecipeCard`, `MainShell` y `SectionPlaceholder`).
- Ficheros creados/cambiados:
  `app/src/main/java/com/sazon/app/ui/home/{HomeGreeting,HomeIdentity,HomeSuggestions,HomeRecipes,
  RecipeCardFormat,HomeScreen,RecipeCard}.kt` (nuevos),
  `app/src/main/java/com/sazon/app/ui/navigation/MainNavigationState.kt` (nuevo),
  `app/src/main/java/com/sazon/app/ui/{MainShell,SectionPlaceholder}.kt` (nuevos),
  `app/src/main/java/com/sazon/app/{MainActivity,SazonApp}.kt` (modificados),
  `app/src/main/java/com/sazon/app/ui/HomePlaceholder.kt` (eliminado),
  `app/src/test/java/com/sazon/app/ui/home/{HomeGreetingTest,HomeIdentityTest,HomeSuggestionsTest,
  HomeRecipesTest,RecipeCardFormatTest}.kt`,
  `app/src/test/java/com/sazon/app/ui/navigation/MainNavigationStateTest.kt` (nuevos),
  `docs/technical-discovery.md`, `feature_list.json` y este registro. No se tocaron
  `app/build.gradle.kts` ni `gradle/libs.versions.toml`.
- Hallazgos relevantes: para distinguir "Perfil aún cargando" de "sin Perfil" (la spec decía "mientras
  es `null` pinta el fondo"), `SazonApp` usa un estado privado `ProfileObservation` con `Loading` /
  `Loaded(profile)`: sin él, un `null` inicial mostraría el onboarding antes de resolver el Perfil.
  `MainShell` (no `HomeScreen`) es quien observa el Lote y llama a `ensureBatch`, manteniendo
  `HomeScreen` sin estado y con la firma de la spec (`profile, taxonomy, suggestions, recipes,
  onOpenGenerator`); el comportamiento observable es el descrito en la spec. En el bloque "Tus
  recetas" se pintan siempre los títulos de grupo "Guardadas" y "Abiertas hace poco" y, si
  `isEmpty`, la línea de estado vacío, para cumplir el escenario 5 literalmente (la spec sugería que un
  grupo vacío no pinta nada; el escenario pide ver las dos etiquetas). Correcciones de compilación:
  `${band(hour)}` en el template de `HomeGreeting` y la factoría `RoundRect(rect=…, topLeft=…)` frente
  al constructor con `topLeftCornerRadius`.
- Riesgo o incógnita abierta: quedan para revisión manual de la persona tras el merge el acabado
  visual (degradado y glifo de las tarjetas, barra activa/inactiva, densidad del carrusel, claro/oscuro)
  y las dos pruebas manuales de emulador sin red (bloques 1, 3, 4 y 5 se ven y navegan; Home poblada de
  inmediato sin pantalla de carga). Un defecto visual se corrige con una feature correctiva. La deuda de
  consolidar `sazon.db` + `suggestion_batch.db` con migración probada sigue anotada.
- Siguiente paso: validación independiente de `home-offline-blocks` con `feature-validator`; después,
  commit local de la rama (orquestador de `feature-flow`) y publicación por `github-flow`.

### Sesión 033

- Fecha: 2026-10-08
- Objetivo: validar `home-offline-blocks` de forma independiente y persistir la aceptación.
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la validación independiente. Sin cambios de código tras la
  validación. Quedan desbloqueadas `local-result-filters`, `generator-ingredients-step` y
  `profile-screen`.
- Verificación ejecutada: el validador re-ejecutó `init.sh` con Git Bash (exit 0), `./gradlew
  testDebugUnitTest --rerun-tasks` (32/32 tareas) y `./gradlew lint --rerun-tasks` (28/28 tareas);
  parseó por su cuenta los XML de resultados e informes de lint e inspeccionó el código (pureza,
  integración del Lote, alcance y las tres desviaciones documentadas).
- Evidencia capturada: 159 tests, 0 fallos/errores/saltados (nuevos: `HomeGreetingTest` 7,
  `MainNavigationStateTest` 6, `HomeIdentityTest` 4, `RecipeCardFormatTest` 4, `HomeRecipesTest` 3,
  `HomeSuggestionsTest` 3; 132 preexistentes); lint 0 errores y 13 warnings preexistentes
  (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), 0 en
  ficheros de la feature; escenarios 1-8 y `Validator Checklist` en PASS; `domain.suggestions`/
  `data.suggestions`, `ProfileDatabase`, `data.profile/**`, `domain.profile/**` y el build intactos;
  `ui/home`/`ui/navigation` puros sin imports Android/Compose; `HomePlaceholder.kt` eliminado sin
  referencias colgantes.
- Ficheros actualizados: `feature_list.json` y este registro.
- Hallazgos del validador (4 Low informativos, ninguno bloqueante): el paréntesis del plan de
  verificación de `HomeSuggestionsTest` en la spec ("la lista vacía no sustituye a las semillas") es
  ambiguo frente al contrato `?:` (la implementación sigue el contrato y lo documenta); el
  `Validator Checklist` de la spec queda sin marcar (por diseño); los escenarios 4/6/7 y el estilo
  activo/inactivo se verifican por inspección (AGENTS.md no automatiza UI); la hora se captura una vez
  con `remember`, intencional.
- Riesgo o incógnita abierta: ninguno nuevo. Quedan para revisión manual de la persona tras el merge
  el acabado visual (degradado/glifo, barra activa/inactiva, densidad del carrusel, claro/oscuro) y las
  dos pruebas de emulador sin red; un defecto visual se corrige con una feature correctiva. La deuda de
  consolidar `sazon.db` + `suggestion_batch.db` con migración probada sigue anotada.
- Siguiente paso: commit local de la rama `feature/home-offline-blocks` (lo hace el orquestador de
  `feature-flow`); la publicación queda para `github-flow`. Después, `local-result-filters`,
  `generator-ingredients-step` o `profile-screen`.

### Sesión 034

- Fecha: 2026-10-09
- Objetivo: implementar `local-result-filters` (filtrado local, sin red, de las recetas del dispositivo
  en el bloque "Tus recetas": categoría + derivados "Rápido"/"Vegetariano") según
  `docs/specs/local-result-filters.md`. Sesión reanudada: la ejecución previa dejó el trabajo sin
  commitear; esta sesión lo revisa, completa lo que faltaba (documentación) y lo autoverifica sin
  rehacer lo ya válido.
- Completado:
  - Lógica **pura** en `com.sazon.app.ui.filters`: `LocalResultFilter` (selección única `All`,
    `Category(categoryId)`, `Fast` y `Vegetarian`, cada una con `key` estable) y el `object`
    `LocalResultFilters` con `visible(recipes, filter, taxonomy)` (los cuatro predicados, orden
    preservado), `vegetarianTags(taxonomy)` (derivado de `dietPatterns[vegetarian].excludedTags` con
    respaldo constante `meat`/`poultry`/`fish`/`seafood`), `categoriesIn(recipes, taxonomy)` (categorías
    presentes, sin duplicados, en orden de `taxonomy.recipeCategories`, ignorando ids desconocidos),
    `label(...)`, `isDerived(...)`, `fromKey(...)` y `FAST_MAX_MINUTES = 30`. Sin imports de
    Compose/Android (solo `domain.recipe`/`domain.taxonomy`), testeable en JVM.
  - Fila de chips `ResultFilterRow` (`FilterChip` de Material 3, sin dependencias nuevas): "Todas"
    primero, una chip por categoría presente, y "Rápido"/"Vegetariano" agrupados al final (tras un
    separador) y con el acento **oliva** (`secondary`) al seleccionarse, frente al terracota
    (`primary`) de las categorías; etiquetas vía `LocalResultFilters.label`, tokens del tema sin hex
    sueltos.
  - Integración en la Home: `HomeRecipes.filteredBy(filter, taxonomy)` (puro) acota `saved` y
    `recentlyOpened` de forma independiente; `HomeScreen` guarda la selección con
    `rememberSaveable(stateSaver = LocalResultFilterSaver)` (Saver en la capa Compose sobre `key`/
    `fromKey`) y `RecipesSection` construye las categorías presentes, pinta la fila y, si el filtrado
    queda vacío, la línea "No hay recetas con este filtro."; si el bloque **sin filtrar** llega vacío
    solo pinta la línea de usuario nuevo (sin chips).
  - Tests JVM: `LocalResultFilterTest` (escenarios 1-5 y claves, con frontera 30/31 y caso negativo de
    tags animales) y `HomeRecipesTest` ampliado (casos de `filteredBy`). Sin Favoritos, sin buscador por
    texto, sin red, sin persistencia y sin tocar `Recipe`/la Taxonomía/`seed-recipes.json`/
    `domain.suggestions`; sin dependencias nuevas.
  - `docs/technical-discovery.md` ampliado con la ubicación y garantías de `ui.filters` y su aplicación
    al bloque "Tus recetas".
- Verificación ejecutada: `& "C:\Program Files\Git\bin\bash.exe" init.sh` (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos); parseo propio de los XML de resultados
  (`test-results/testDebugUnitTest/TEST-*.xml`) y agrupación del informe `lint-results-debug.xml`;
  inspección de imports de la lógica pura, de que no cambian `app/build.gradle.kts` ni
  `gradle/libs.versions.toml` y de que no se tocan `Recipe`, la Taxonomía, `seed-recipes.json` ni
  `domain.suggestions`/`data.suggestions`.
- Evidencia capturada: **172 tests, 0 fallos/errores/saltados** en 28 suites (13 nuevos:
  `LocalResultFilterTest` 8 y `HomeRecipesTest` 8 — de 3 a 8, +5 de `filteredBy`; 159 preexistentes en
  verde); lint 0 errores y 13 warnings preexistentes (GradleDependency 7, NewerVersionAvailable 3,
  AndroidGradlePluginVersion 2, OldTargetApi 1), 0 en ficheros de la feature; `init.sh` con código 0;
  `LocalResultFilter` sin imports Compose/Android; `HomeRecipes.filteredBy` puro; sin dependencias
  nuevas ni cambios en el build/lockfile ni en el dominio/catálogo.
- Ficheros creados/cambiados:
  `app/src/main/java/com/sazon/app/ui/filters/{LocalResultFilter,ResultFilterRow}.kt` (nuevos),
  `app/src/test/java/com/sazon/app/ui/filters/LocalResultFilterTest.kt` (nuevo),
  `app/src/main/java/com/sazon/app/ui/home/{HomeRecipes,HomeScreen}.kt` (modificados),
  `app/src/test/java/com/sazon/app/ui/home/HomeRecipesTest.kt` (modificado),
  `docs/technical-discovery.md`, `feature_list.json` y este registro. No se tocaron
  `app/build.gradle.kts` ni `gradle/libs.versions.toml`.
- Hallazgos relevantes: ninguno de comportamiento; las firmas de la spec compilaron con las APIs reales
  del repositorio (`Taxonomy.ingredient`/`recipeCategory`/`recipeCategories`, `Recipe.categoryId`/
  `totalMinutes`, `dietPattern("vegetarian")?.excludedTags`). `fromKey` degrada una clave desconocida a
  `All` (defensivo ante un estado restaurado corrupto). El bloque "Tus recetas" sigue llegando vacío
  desde `MainShell` (costura de `home-offline-blocks`; la llenarán `favorites`/`recipe-detail-screen`),
  así que hoy solo se observa el estado de usuario nuevo; el filtro puro es lo verificado en JVM ahora.
- Riesgo o incógnita abierta: se mantienen los residuales de la spec: la detección es por **tags, no
  por keywords** (un ingrediente libre que sea violación de dieta no se excluye); el microcopy "No hay
  recetas con este filtro." es provisional; la distinción visual exacta de los derivados (oliva +
  agrupación) y el efecto del bloque poblado quedan para revisión manual de la persona tras el merge
  (AGENTS.md no automatiza UI de emulador). La secuencia (bloque vacío hoy) se anota sin reordenar
  `feature_list.json`.
- Siguiente paso: validación independiente de `local-result-filters` con `feature-validator`; después,
  commit local de la rama (orquestador de `feature-flow`) y publicación por `github-flow`.

### Sesión 035

- Fecha: 2026-10-09
- Objetivo: validar `local-result-filters` de forma independiente y persistir la aceptación.
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la validación independiente. Sin cambios de código tras la
  validación. Quedan desbloqueadas las features que dependían de esta.
- Verificación ejecutada: el validador re-ejecutó `init.sh` con Git Bash (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos) y `./gradlew testDebugUnitTest --rerun-tasks`; parseó por su
  cuenta los XML de resultados y el informe de lint, e inspeccionó el código (pureza, predicados,
  ausencia de hex sueltos, alcance y build/lockfile intactos).
- Evidencia capturada: 172 tests, 0 fallos/errores/saltados en 28 suites (13 nuevos:
  `LocalResultFilterTest` 8 y `HomeRecipesTest` 8 — de 3 a 8, +5 de `filteredBy`; 159 preexistentes en
  verde, sin regresiones); lint 0 errores y 13 warnings preexistentes (GradleDependency 7,
  NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), 0 en ficheros de la feature;
  escenarios exigidos 3 (frontera `<= 30`) y 4 (tags `meat`/`poultry`/`fish`/`seafood`, caso negativo
  que no pasa en vacío) confirmados; `All` preserva orden, `Category` usa `categoryId` y categoría
  inexistente da vacío; `LocalResultFilter` sin imports Compose/Android y `ResultFilterRow` la única
  capa Compose; `app/build.gradle.kts` y `gradle/libs.versions.toml` sin cambios.
- Ficheros actualizados: `feature_list.json` y este registro.
- Hallazgos del validador (4 Low + 1 informativo, ninguno bloqueante): el nombre del test del escenario
  6 está desalineado con la spec (cubre claves/labels/isDerived, no la fila de chips); una aserción
  redundante en el escenario 4; `FALLBACK_VEGETARIAN_TAGS` podría ser `private`; un flake transitorio de
  resultados de test en la primera pasada de `init.sh` tras el `./gradlew --stop` del trap de salida
  (la segunda pasada pasó en verde; anotado como robustez de harness); el `Validator Checklist` de la
  spec queda sin marcar (por diseño).
- Riesgo o incógnita abierta: ninguno nuevo. Se mantienen los residuales de la spec: detección por
  **tags, no keywords** (ingrediente libre no se excluye); microcopy "No hay recetas con este filtro."
  provisional; la distinción visual exacta de los derivados y el efecto del bloque poblado quedan para
  revisión manual de la persona tras el merge; hoy la costura `HomeRecipes` llega vacía desde
  `MainShell`.
- Siguiente paso: commit local de la rama `feature/local-result-filters` (lo hace el orquestador de
  `feature-flow`); la publicación queda para `github-flow`. Después, `generator-ingredients-step`.

### Sesión 036

- Fecha: 2026-10-09
- Objetivo: implementar `generator-ingredients-step` (paso 1 real del Generador, "¿Qué tienes?") según
  `docs/specs/generator-ingredients-step.md`, sustituyendo el `SectionPlaceholder` de
  `PushedScreen.GENERATOR`.
- Completado:
  - Logica/estado **puro** en `com.sazon.app.ui.generator`: `GeneratorIngredient` (ingrediente anadido:
    de Taxonomia si `ingredientId != null`, libre si `null`), `GeneratorIngredientsState`
    (`typeQuery`, `toggleTaxonomy` con dedupe por `ingredientId` y alternancia, `addFree` con dedupe por
    nombre normalizado `trim`+minusculas+sin acentos y vaciado de la query, `remove`, `count`,
    `isEmpty`, `canContinue` y `countLabel` con singular/plural) y `GeneratorStep` (`INGREDIENTS`
    "¿Qué tienes?"/"Continuar" y `SETTINGS` "Ajusta tu receta"/"Generar recetas" con `next`/`previous`/
    `isLast`). Sin imports de Compose/Android (solo `domain.taxonomy` y `java.text.Normalizer`).
  - Capa Compose delgada: `GeneratorIngredientsScreen` (helper, `OutlinedTextField` pildora con "Añadir"
    en el `trailingIcon` deshabilitado con texto en blanco, chips de los anadidos con "×", contador con
    cifras tabulares y nube **colapsable** "Ingredientes sugeridos" desde `taxonomy.ingredients`
    filtrada con `IngredientsFilter.matching`, chip seleccionado si su `id` esta entre los anadidos) y
    `GeneratorFlow` (`Scaffold` con barra superior "Atras" + titulo del paso y primario anclado,
    deshabilitado con `!canContinue` en el paso 1; `Saver` de `rememberSaveable` para el paso y el
    borrador; paso 2 como marcador). Sin iconos: "×" y "Atras" son texto.
  - Integracion en `MainShell`: `PushedScreen.GENERATOR` monta `GeneratorFlow(taxonomy, onClose = pop)`;
    `SectionPlaceholder` sigue existiendo para Favoritos/Perfil.
  - Tests JVM con casos negativos: `GeneratorIngredientsStateTest` (escenarios 1-6) y
    `GeneratorStepTest` (escenario 7 y microcopy). Sin dependencias nuevas: no se tocan
    `app/build.gradle.kts` ni `gradle/libs.versions.toml`.
- Verificación ejecutada: `./gradlew testDebugUnitTest --tests "com.sazon.app.ui.generator.*"`
  (enfocado, BUILD SUCCESSFUL), `& "C:\Program Files\Git\bin\bash.exe" init.sh` (exit 0, `./gradlew test`
  + `./gradlew lint`, sin procesos largos); parseo propio de los XML de resultados y del informe
  `lint-results-debug.xml`; inspeccion de imports de la logica pura, de la reutilizacion sin cambios de
  `IngredientsFilter`, de la ausencia de cambios en el build/lockfile y de `git status`.
- Evidencia capturada: **189 tests, 0 fallos/errores/saltados** en 30 suites (17 nuevos:
  `GeneratorIngredientsStateTest` 12 y `GeneratorStepTest` 5; 172 preexistentes en verde); lint 0 errores
  y 13 warnings preexistentes (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2,
  OldTargetApi 1), 0 en ficheros de la feature; `init.sh` con código 0; `GeneratorIngredient`/
  `GeneratorIngredientsState`/`GeneratorStep` sin imports Compose/Android; `app/build.gradle.kts` y
  `gradle/libs.versions.toml` sin cambios.
- Ficheros creados/cambiados:
  `app/src/main/java/com/sazon/app/ui/generator/{GeneratorIngredient,GeneratorIngredientsState,
  GeneratorStep,GeneratorIngredientsScreen,GeneratorFlow}.kt` (nuevos),
  `app/src/main/java/com/sazon/app/ui/MainShell.kt` (modificado),
  `app/src/test/java/com/sazon/app/ui/generator/{GeneratorIngredientsStateTest,GeneratorStepTest}.kt`
  (nuevos), `docs/technical-discovery.md`, `feature_list.json` y este registro. No se tocaron
  `app/build.gradle.kts` ni `gradle/libs.versions.toml`.
- Hallazgos relevantes: ninguno de comportamiento; las firmas de la spec compilaron con las APIs reales
  (`Ingredient`, `Taxonomy.ingredients`, `IngredientsFilter.matching`, tokens del tema) sin adaptaciones.
  El `Saver` del borrador usa el formato `[query, count, name, id]...` de la spec y el paso se guarda por
  nombre de enum (`GeneratorStepSaver`). El `Scaffold` de `GeneratorFlow` vive dentro del `Box` de
  `MainShell` que ya aplica `windowInsetsPadding(safeDrawing)`, de modo que no hay doble inset.
- Riesgo o incógnita abierta: quedan para revision manual de la persona tras el merge el acabado visual
  (chips, densidad de la nube, claro/oscuro) y la prueba manual de emulador exigida ("Continuar"
  deshabilitado con cero ingredientes, contador al anadir/quitar y "×"); un defecto visual se corrige con
  una feature correctiva. El round-trip del `Saver` no se testea en JVM (capa Compose; retencion manual),
  como en features previas. El microcopy del placeholder del input libre es provisional.
- Siguiente paso: validacion independiente de `generator-ingredients-step` con `feature-validator`;
  despues, commit local de la rama `feature/generator-ingredients-step` (orquestador de `feature-flow`) y
  publicacion por `github-flow`. La siguiente feature en lista es `generator-settings-step`.

### Sesión 037

- Fecha: 2026-10-09
- Objetivo: validar `generator-ingredients-step` de forma independiente y persistir la aceptación.
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la validación independiente. Sin cambios de código tras la
  validación. Queda desbloqueada `generator-settings-step`.
- Verificación ejecutada: el validador re-ejecutó `init.sh` con Git Bash (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos) y `./gradlew testDebugUnitTest --rerun-tasks` (32/32 tareas);
  parseó por su cuenta los XML de resultados y el informe `lint-results-debug.xml`, e inspeccionó el
  código (pureza, dedupe, integración en `MainShell`, alcance y build/lockfile intactos).
- Evidencia capturada: 30 suites, 189 tests, 0 fallos/errores/saltados (17 nuevos:
  `GeneratorIngredientsStateTest` 12 y `GeneratorStepTest` 5; 172 preexistentes en verde); lint 0 errores
  y 13 warnings preexistentes (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2,
  OldTargetApi 1), 0 en ficheros de la feature; escenarios 1–8 confirmados; `ui.generator` sin imports
  Compose/Android; `MainShell` sustituye el `SectionPlaceholder` del Generador por `GeneratorFlow`;
  `app/build.gradle.kts`, `gradle/libs.versions.toml`, `build.gradle.kts` y `gradle.properties` sin
  cambios; `IngredientsFilter`, `domain/**`, `data/**`, la Taxonomía y `seed-recipes.json` intactos.
- Ficheros actualizados: `feature_list.json` y este registro.
- Hallazgos del validador (4 Low informativos, ninguno bloqueante): un duplicado cruzado de libre a
  Taxonomía no queda cubierto por el contrato literal de la spec (si se añade el libre "Tomate" y luego
  el chip "Tomate", se crean dos chips homónimos; candidato a feature correctiva); el round-trip del
  `Saver` no se testea en JVM (capa Compose, retención manual); posible doble inset del `Scaffold` de
  `GeneratorFlow` dentro del `Box` de `MainShell`, visual; `Validator Checklist` de la spec sin marcar
  (por diseño).
- Riesgo o incógnita abierta: ninguno nuevo. Quedan para revisión manual de la persona tras el merge el
  acabado visual (chips, densidad de la nube, claro/oscuro) y la prueba manual de emulador exigida
  ("Continuar" deshabilitado con cero ingredientes y el contador se actualiza al añadir y quitar); un
  defecto visual se corrige con una feature correctiva.
- Siguiente paso: commit local de la rama `feature/generator-ingredients-step` (lo hace el orquestador de
  `feature-flow`); la publicación queda para `github-flow`. Después, `generator-settings-step`.

### Sesión 038

- Fecha: 2026-10-09
- Objetivo: implementar `profile-screen` (pestaña Perfil con identidad y preferencias editables) según
  `docs/specs/profile-screen.md`, sustituyendo el `SectionPlaceholder("Perfil")`. Feature elegida por
  el orquestador fuera del orden de lista (aprovechando que la rama `feature/profile-screen` ya existe y
  su spec está lista).
- Completado:
  - `OnboardingPreferencesDraft` ampliado **aditivo** con `from(preferences: PreferenceProfile)` y
    `toPreferences(updatedAt: Long)`: el Perfil usa el **mismo** motor de selección que el onboarding.
  - Estado de edición **puro** `com.sazon.app.ui.profile.ProfileEditState` (identidad + borrador) con
    `from(profile, taxonomy)`, `withDisplayName`/`withAvatarColorId`/`withPreferencesDraft`,
    `isNameValid` y `toProfile(base, now)`. Sin imports Compose/Android. `toProfile` conserva
    `base.preferences` tal cual (mismo `updatedAt`) si el borrador no cambió, de modo que editar solo la
    identidad **no** cambia la firma ni invalida el Lote; si cambia una preferencia, reconstruye
    `PreferenceProfile` con `updatedAt = now`; conserva `createdAt` y normaliza el nombre
    (`ProfileName`); nombre inválido → `IllegalArgumentException`.
  - Capa Compose delgada: `ProfileScreen` (LazyColumn, secciones "Mis preferencias", "Alergias e
    intolerancias" y "Mi cocina", estado `rememberSaveable` + `ProfileEditStateSaver`,
    `LaunchedEffect(profile)` y guardado por `onSaveProfile`), `ProfileIdentityEditor` (reutiliza
    `AvatarPreview`, campo + helper + 8 muestras de `avatarColors` con anillo terracota y "Guardar"
    deshabilitado con nombre inválido), `ProfileSectionRow` (resumen con labels de la Taxonomía y
    chevron) y `ProfileEditorSheet` (hoja inferior `ModalBottomSheet` + enum `ProfileSection` que
    envuelve **sin cambios** los ocho editores de `ui.preferences`).
  - Integración: `MainShell` recibe `onSaveProfile` y enruta `MainTab.PROFILE -> ProfileScreen`
    (Favoritos sigue con `SectionPlaceholder`); `SazonApp` guarda con `ProfileRepository.save` en un
    `rememberCoroutineScope`; la observación existente recompone el armazón. Sin tocar dominio, Room,
    la Firma, la Taxonomía, `app/build.gradle.kts` ni `gradle/libs.versions.toml`; sin dependencias
    nuevas (la hoja usa `ModalBottomSheet` de Material 3, con `@OptIn(ExperimentalMaterial3Api::class)`).
  - Test JVM `ProfileEditStateTest` (9 tests) con los escenarios 2-4 y 6-10 más la semántica del 8, con
    casos negativos que no pasan en vacío.
- Verificación ejecutada: `./gradlew testDebugUnitTest --tests "com.sazon.app.ui.profile.*"` (enfocado,
  BUILD SUCCESSFUL) y `bash init.sh` (exit 0, `./gradlew test` + `./gradlew lint`, sin procesos largos);
  parseo propio de los XML de resultados y del informe `lint-results-debug.xml`; inspección de imports
  de `ProfileEditState`, de `git status` (solo cambian los ficheros previstos) y de que
  `ui.preferences`, `app/build.gradle.kts` y `gradle/libs.versions.toml` no cambian.
- Evidencia capturada: **198 tests, 0 fallos/errores/saltados** en 31 suites (9 nuevos:
  `ProfileEditStateTest` 9; 189 preexistentes en verde); lint 0 errores y 13 warnings preexistentes
  (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), 0 en
  ficheros de la feature; `init.sh` con código 0; `ProfileEditState` sin imports Compose/Android;
  escenarios 2-4 y 6-10 verificados con firma (`PreferenceSignature`) real.
- Ficheros creados/cambiados:
  `app/src/main/java/com/sazon/app/ui/profile/{ProfileEditState,ProfileScreen,ProfileIdentityEditor,
  ProfileEditorSheet,ProfileSectionRow}.kt` (nuevos),
  `app/src/main/java/com/sazon/app/ui/onboarding/OnboardingPreferencesDraft.kt` (aditivo),
  `app/src/main/java/com/sazon/app/ui/{MainShell,SazonApp}.kt` (modificados),
  `app/src/test/java/com/sazon/app/ui/profile/ProfileEditStateTest.kt` (nuevo),
  `docs/technical-discovery.md`, `docs/specs/profile-screen.md` (spec del planner, ya presente),
  `feature_list.json` y este registro.
- Hallazgos relevantes: la spec no incluye callbacks para entrar/salir del modo edición de identidad;
  `ProfileIdentityEditor` añade `onEdit`/`onCancel` (necesarios para la UX del "Editar"/"Cancelar" de
  `DESIGN.md`/`stitch-prompt`) sin cambiar los contratos puros. `ModalBottomSheet` sigue requiriendo
  `@OptIn(ExperimentalMaterial3Api::class)` en el BOM actual (2026.02.01). La spec sugería
  `ProfileEditorSheet(section, state, ...)`; se respeta esa firma.
- Riesgo o incógnita abierta: quedan para revisión manual de la persona tras el merge el acabado visual
  (hojas inferiores, chips, muestras, contraste, claro/oscuro) y las dos pruebas manuales exigidas
  (editar una preferencia desde el Perfil se refleja igual que en el onboarding; cambiar nombre/avatar no
  invalida el Lote); un defecto visual se corrige con feature correctiva. El round-trip del
  `ProfileEditStateSaver` no se testea en JVM (capa Compose; retención manual), como en features previas.
  La sección "Resumen de actividad", "Actualizar sugerencias", los ajustes y "Borrar mis datos" quedan
  fuera de alcance por diseño (features propias).
- Siguiente paso: validación independiente de `profile-screen` con `feature-validator`; después, commit
  local de la rama `feature/profile-screen` (orquestador de `feature-flow`) y publicación por
  `github-flow`.

### Sesión 039

- Fecha: 2026-10-09
- Objetivo: implementar `generator-settings-step` (paso 2 real del Generador, "Ajusta tu receta") según
  `docs/specs/generator-settings-step.md`. Sesión reanudada: la ejecución previa dejó el trabajo sin
  commitear (estado + pantalla + test nuevos; `GeneratorFlow`, `MainShell` y `docs/technical-discovery.md`
  modificados); esta sesión lo revisa, confirma que está completo y correcto, y lo autoverifica sin rehacer
  lo ya válido.
- Completado:
  - Lógica/estado **puro** en `com.sazon.app.ui.generator.GeneratorSettingsState` (`servings`, `mealTypeId`
    opcional, `timeBucketId`, `difficultyId`, `hardRestrictionIds`, `notes`), con `initial(preferences,
    limits)` que **precarga** comensales (`defaultServings` recortado a `taxonomy.servings`), franja,
    dificultad y alergias del Perfil (el tipo de comida arranca en `null` y las notas en blanco),
    `setServings` (recorta con `coerceIn`), `selectMealType` (single-select **opcional**: el segundo toque
    lo deja en `null`), `selectTimeBucket`/`selectDifficulty` (single-select que reemplazan),
    `toggleHardRestriction` (conjunto, sin duplicados), `clearHardRestrictions`/`hasNoRestrictions` y
    `typeNotes`. Sin imports Compose/Android (solo `domain.profile` y `domain.taxonomy`).
  - Capa Compose **delgada** `GeneratorSettingsScreen`: seis tarjetas `Surface` (`SazonShapes.large`,
    interior `SazonSpacing.sm`) con "Tiempo disponible" (`TimeBucketEditor`), "Tipo de comida" (fila local
    de `FilterChip` sobre `taxonomy.mealTypes`, incluido **Postre**; no hay editor previo), "Comensales"
    (`ServingsStepper`), "Dificultad" (`DifficultyEditor`), "Alergias a evitar"
    (`HardRestrictionsEditor` en **ámbar**, nunca rojo, con la caption "Ninguna declarada" y el
    `TextButton` "Editar en mi perfil") y "¿Algo más que deba saber?" (`OutlinedTextField` multilínea).
    Sin iconos ni dependencias nuevas.
  - `GeneratorFlow` recibe ahora `preferences: PreferenceProfile` y `onEditProfile: () -> Unit`, guarda el
    borrador de ajustes con `rememberSaveable` + `GeneratorSettingsStateSaver` (formato
    `[servings, mealTypeId.orEmpty(), timeBucketId, difficultyId, count, …restrictionIds, notes]`) y monta
    `GeneratorSettingsScreen` en `GeneratorStep.SETTINGS`; **eliminado** `GeneratorSettingsPlaceholder`
    (sin referencias colgantes). "Generar recetas" sigue `enabled` y sin efecto (`step.next() == null`).
  - `MainShell` pasa `preferences = profile.preferences` y
    `onEditProfile = { navigation = navigation.pop().selectTab(MainTab.PROFILE) }` a `GeneratorFlow`.
  - `docs/technical-discovery.md` ampliado con la ubicación y las garantías del paso de ajustes.
  - Tests JVM con casos negativos: `GeneratorSettingsStateTest` (escenarios 1-5 y 7). Sin dependencias
    nuevas; no se tocan `app/build.gradle.kts`, `gradle/libs.versions.toml`, `domain/**`, `data/**`, la
    Taxonomía, `ui/preferences/**` ni `init.sh`.
- Verificación ejecutada: `"/c/Program Files/Git/bin/bash.exe" init.sh` (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos); `./gradlew testDebugUnitTest --rerun-tasks` (32/32 tareas) con
  parseo propio de los XML de resultados; parseo del informe `lint-results-debug.xml`; inspección de
  imports de la lógica pura, de la reutilización sin cambios de los editores, de la ausencia de cambios en
  el build/lockfile/dominio y de `git status`.
- Evidencia capturada: **200 tests, 0 fallos/errores/saltados** en 31 suites (11 nuevos:
  `GeneratorSettingsStateTest`; 189 preexistentes en verde); lint 0 errores y 13 warnings preexistentes
  (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), 0 en
  ficheros de la feature; `init.sh` con código 0; `GeneratorSettingsState` sin imports Compose/Android;
  `TimeBucketEditor`/`DifficultyEditor`/`ServingsStepper`/`HardRestrictionsEditor` reutilizados sin
  modificarlos; `app/build.gradle.kts`, `gradle/libs.versions.toml`, `domain/**` y `data/**` sin cambios;
  `init.sh` sin cambios.
- Ficheros creados/cambiados:
  `app/src/main/java/com/sazon/app/ui/generator/GeneratorSettingsState.kt` (nuevo),
  `app/src/main/java/com/sazon/app/ui/generator/GeneratorSettingsScreen.kt` (nuevo),
  `app/src/main/java/com/sazon/app/ui/generator/GeneratorFlow.kt` (modificado),
  `app/src/main/java/com/sazon/app/ui/MainShell.kt` (modificado),
  `app/src/test/java/com/sazon/app/ui/generator/GeneratorSettingsStateTest.kt` (nuevo),
  `docs/technical-discovery.md`, `feature_list.json` y este registro.
- Hallazgos relevantes: ninguno de comportamiento; las firmas de la spec compilaron con las APIs reales
  (`PreferenceProfile`, `Taxonomy.mealTypes`/`servings`, los editores de `ui.preferences`) sin
  adaptaciones. El escenario 6 (navegación a Perfil en `MainShell`) y el 8 (conservación del borrador con
  `rememberSaveable` + `Saver`) son de la capa Compose y se verifican por inspección y prueba manual.
- Riesgo o incógnita abierta: quedan para revisión manual de la persona tras el merge las dos pruebas de
  emulador exigidas (el stepper aparece con los comensales habituales del perfil; los cinco tipos de comida
  incluyen Postre) y el acabado visual (tarjetas, ámbar de las alergias, densidad de seis tarjetas,
  claro/oscuro); un defecto visual se corrige con una feature correctiva. El round-trip del `Saver` no se
  testea en JVM (capa Compose; retención manual), como en features previas.
- Siguiente paso: validación independiente de `generator-settings-step` con `feature-validator`; después,
  commit local de la rama `feature/generator-settings-step` (orquestador de `feature-flow`) y publicación
  por `github-flow`. La siguiente feature en lista es `generation-flow-with-validation`.

### Sesión 040

- Fecha: 2026-10-09
- Objetivo: validar `profile-screen` de forma independiente y persistir la aceptación.
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la validación independiente. Sin cambios de código tras la
  validación. Queda desbloqueada `activity-summary` (a la espera de `favorites`) y avanzada la ruta de
  `manual-suggestions-refresh`.
- Verificación ejecutada: el validador re-ejecutó `init.sh` con Git Bash (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos); al ver la primera pasada en `UP-TO-DATE`, forzó
  `./gradlew testDebugUnitTest --rerun-tasks` (32/32 tareas) y `./gradlew lint --rerun-tasks` (28/28
  tareas); parseó por su cuenta los XML de resultados y el informe `lint-results-debug.xml`, e
  inspeccionó el código (pureza, reutilización de editores, integración en `MainShell`, alcance y
  build/lockfile intactos).
- Evidencia capturada: 31 suites, 198 tests, 0 fallos/errores/saltados (9 nuevos: `ProfileEditStateTest`
  9; 189 preexistentes en verde); lint 0 errores y 13 warnings preexistentes, 0 referencias a `ui/profile`.
  Escenarios 2-4 y 6-10 confirmados con casos negativos reales; verificación exigida #2: `PreferenceSignature.of`
  igual al cambiar solo nombre o avatar y distinta al cambiar una preferencia, y `MainShell.kt:84` mantiene
  `LaunchedEffect(profile.preferences)` de modo que la identidad sola no relanza `ensureBatch`; verificación
  exigida #1: mismo motor `OnboardingPreferencesDraft` que el onboarding. `ProfileEditState` sin imports
  Compose/Android; `ui.preferences/**` sin cambios; `OnboardingPreferencesDraft` aditivo; sin tocar dominio,
  Room, Firma, Taxonomía, assets, `app/build.gradle.kts` ni `gradle/libs.versions.toml`.
- Ficheros actualizados: `feature_list.json` y este registro.
- Hallazgos del validador (3 Low informativos, ninguno bloqueante): un nombre sin confirmar en el editor de
  identidad puede arrastrarse a un guardado de preferencias (borde de UX, candidato a feature correctiva);
  `ProfileEditStateSaver.restore` no es defensivo ante un estado guardado corrupto (patrón heredado);
  KDoc obsoleto en `SectionPlaceholder.kt` que aún cita Perfil/Generador como placeholders.
- Riesgo o incógnita abierta: ninguno nuevo. Quedan para revisión manual de la persona tras el merge el
  acabado visual (hojas inferiores, chips, muestras, contraste, claro/oscuro) y las dos pruebas manuales
  exigidas (una preferencia editada desde el Perfil se refleja como en el onboarding; cambiar nombre/avatar
  no invalida el Lote); un defecto visual se corrige con feature correctiva.
- Siguiente paso: commit local de la rama `feature/profile-screen` (lo hace el orquestador de
  `feature-flow`); la publicación queda para `github-flow`. Después, `generator-settings-step`.

### Sesión 041

- Fecha: 2026-10-09
- Objetivo: validar `generator-settings-step` de forma independiente y persistir la aceptación.
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la validación independiente; registrado el detalle en
  `docs/validations/generator-settings-step.md`. Sin cambios de código tras la validación. Queda
  desbloqueada `generation-flow-with-validation` (sus otras dependencias ya están `accepted`).
- Verificación ejecutada: el validador re-ejecutó `init.sh` con Git Bash (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos), `./gradlew testDebugUnitTest --rerun-tasks` (32/32 tareas,
  `TEST_EXIT=0`) y `./gradlew lint --rerun-tasks` (28/28 tareas, `LINT_EXIT=0`); parseó por su cuenta los
  XML de resultados y el informe `lint-results-debug.xml`, e inspeccionó el código (pureza, reutilización
  de editores, integración en `MainShell`, eliminación del placeholder, alcance y build/lockfile intactos).
- Evidencia capturada: 31 suites, 200 tests, 0 fallos/errores/saltados (11 nuevos:
  `GeneratorSettingsStateTest`; 189 preexistentes en verde); lint 0 errores y 13 warnings preexistentes
  (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), 0 en
  ficheros de la feature; escenarios 1–8 confirmados; `GeneratorSettingsState` sin imports Compose/Android;
  `TimeBucketEditor`/`DifficultyEditor`/`ServingsStepper`/`HardRestrictionsEditor` reutilizados sin
  modificarlos; `app/build.gradle.kts`, `gradle/libs.versions.toml`, `domain/**`, `data/**` y la Taxonomía
  intactos.
- Ficheros actualizados: `feature_list.json`, `docs/validations/generator-settings-step.md` y este
  registro.
- Hallazgos del validador (3 Low + 3 informativos, ninguno bloqueante): los escenarios 6 y 8 viven en la
  capa Compose y no son automatizables por AGENTS.md (verificados por inspección y prueba manual); el
  escenario 2 fija el orden literal de `mealTypes`; el escenario 4 usa folds en vez de aserción explícita
  de los extremos; microcopy de títulos de tarjeta hardcodeado por spec; `Validator Checklist` de la spec
  sin marcar; `docs/validations/generator-settings-step.md` creado por el orquestador.
- Riesgo o incógnita abierta: ninguno nuevo. Quedan para revisión manual de la persona tras el merge las
  dos pruebas de emulador exigidas (el stepper aparece con los comensales habituales del perfil; los cinco
  tipos de comida incluyen Postre) y el acabado visual (tarjetas, ámbar de alergias, densidad, claro/oscuro);
  un defecto visual se corrige con una feature correctiva.
- Siguiente paso: commit local de la rama `feature/generator-settings-step` (lo hace el orquestador de
  `feature-flow`); la publicación queda para `github-flow`. Después, `generation-flow-with-validation`.

### Sesión 042

- Fecha: 2026-10-09
- Objetivo: implementar `generation-flow-with-validation` (corte vertical principal del MVP: al pulsar
  "Generar recetas", pedir recetas al generador, descartar las inseguras con el `RecipeValidator` y
  mostrar solo las aceptadas, con un estado de carga cancelable) según
  `docs/specs/generation-flow-with-validation.md`.
- Completado:
  - Orquestación **pura** en `com.sazon.app.domain.generation`: `GenerationResult` (`accepted`,
    `discarded`, `aiUnavailable`) y `GenerationService(generator, taxonomy).generate(ingredients,
    constraints)`, que construye las `RecipeConstraints` con el `profile` **efectivo** de las
    `GenerationConstraints`, llama a `RecipeGenerator.generateFromIngredients`, valida **receta a
    receta** con `RecipeValidator.validateRecipe` (no usa `validateResponse`), re-lanza
    `CancellationException` y convierte cualquier otra `Exception` en `aiUnavailable = true` con listas
    vacías, sin propagar. Sin imports Compose/Android.
  - Mapeo y microcopy **puros** en `com.sazon.app.ui.generator`: `GeneratorRequestFactory.ingredients`
    (`quantity = 0.0`, `unit = null`, `isPantry = false`, conservando `name`/`ingredientId`) y
    `GeneratorRequestFactory.constraints` (Perfil efectivo con `hardRestrictionIds` del paso de ajustes,
    notas en blanco → `null`); `GeneratorProgressMessages` (cabecera fija + sublíneas rotatorias desde
    la Taxonomía y los ajustes).
  - Capa Compose **delgada**: `GeneratorGeneratingScreen` (animación terracota dibujada con `Canvas`,
    titular Manrope, sublíneas rotatorias con `LaunchedEffect`+`delay` y "Cancelar" secundario) y
    `GeneratorResultsPreview` (seam mínimo: **solo** `accepted` con título + `RecipeCardFormat.metaLine`,
    aviso ámbar `tertiaryContainer` si `aiUnavailable` y estado vacío; acciones "Cambiar ingredientes" y
    "Atrás").
  - `GeneratorFlow` recibe `generationService`, guarda `result`/`generationId` en `remember` (no
    `rememberSaveable`), dispara la generación desde "Generar recetas" en `SETTINGS`, respeta
    `MIN_VISIBLE_MILLIS` (~900 ms), cancela poniendo el token a `null` y pinta por *stage* (preview →
    generación → asistente). `MainShell`, `SazonApp` y `MainActivity` encadenan
    `GenerationService(generator = FakeRecipeGenerator(), taxonomy = taxonomy)` (sin DI).
  - Tests JVM con casos negativos: `GenerationServiceTest` (escenarios 1-4, 6-7, incluido el **caso
    negativo obligatorio**), `GeneratorRequestFactoryTest` y `GeneratorProgressMessagesTest`. Sin
    dependencias nuevas; no se tocan `app/build.gradle.kts`, `gradle/libs.versions.toml`,
    `domain.generator` (`GenerationConstraints`), `domain.validation`, `domain.prompt`,
    `domain.suggestions`, `data/**`, la Taxonomía, `seed-recipes.json` ni `init.sh`.
- Verificación ejecutada: `& "C:\Program Files\Git\bin\bash.exe" init.sh` (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos); `./gradlew testDebugUnitTest --tests ...` enfocado (BUILD
  SUCCESSFUL); parseo propio de los XML de resultados y del informe `lint-results-debug.xml`;
  inspección de imports de la lógica pura y de `git status` (solo cambian los ficheros previstos).
- Evidencia capturada: **223 tests, 0 fallos/errores/saltados** en 35 suites (14 nuevos:
  `GenerationServiceTest` 6, `GeneratorRequestFactoryTest` 4, `GeneratorProgressMessagesTest` 4; 209
  preexistentes en verde); lint 0 errores y 13 warnings preexistentes (GradleDependency 7,
  NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), 0 en ficheros de la feature;
  `init.sh` con código 0; `domain.generation` y `GeneratorRequestFactory`/`GeneratorProgressMessages`
  sin imports Compose/Android. Corregido un `AutoboxingStateCreation` (Hint) usando `mutableIntStateOf`
  para volver a los 13 warnings preexistentes.
- Ficheros creados/cambiados:
  `app/src/main/java/com/sazon/app/domain/generation/{GenerationResult,GenerationService}.kt` (nuevos),
  `app/src/main/java/com/sazon/app/ui/generator/{GeneratorRequestFactory,GeneratorProgressMessages,
  GeneratorGeneratingScreen,GeneratorResultsPreview}.kt` (nuevos),
  `app/src/main/java/com/sazon/app/ui/generator/GeneratorFlow.kt` (modificado),
  `app/src/main/java/com/sazon/app/ui/{MainShell,SazonApp}.kt` y `MainActivity.kt` (modificados),
  `app/src/test/java/com/sazon/app/domain/generation/GenerationServiceTest.kt`,
  `app/src/test/java/com/sazon/app/ui/generator/{GeneratorRequestFactoryTest,
  GeneratorProgressMessagesTest}.kt` (nuevos), `docs/technical-discovery.md`, `feature_list.json` y
  este registro.
- Hallazgos relevantes: ninguno de comportamiento; las firmas de la spec compilaron con las APIs reales
  (`RecipeGenerator`, `GenerationConstraints`, `RecipeValidator`, `Taxonomy.timeBucket`, los tokens del
  tema) sin adaptaciones. El fixture "Pollo al ajillo" se descarta por `INCOHERENT` (sus pasos suman 25
  min frente a `totalMinutes = 35`), tal y como la spec ya documentó: en el camino feliz entran tres
  recetas, no cuatro. Se creó un único `FakeRecipeGenerator` compartido por el Lote y la generación en
  `MainActivity`. El resultado no se persiste ni se ordena (no-goals).
- Riesgo o incógnita abierta: quedan para revisión manual de la persona tras el merge el acabado visual
  (animación, ritmo de las sublíneas, densidad del preview, claro/oscuro) y las dos pruebas de emulador
  exigidas ("Cancelar" responde volviendo a los ajustes; sin Cancelar solo se listan recetas aceptadas y
  el "Bocadillo de tomate" del falso no aparece con gluten); un defecto visual se corrige con una feature
  correctiva. Rotar durante la generación o el preview vuelve a los ajustes (el resultado vive en
  `remember`), como documenta la spec.
- Siguiente paso: validación independiente de `generation-flow-with-validation` con `feature-validator`;
  después, commit local de la rama (orquestador de `feature-flow`) y publicación por `github-flow`. La
  siguiente feature en lista es `results-screen`.

### Sesión 043

- Fecha: 2026-10-09
- Objetivo: validar `generation-flow-with-validation` de forma independiente y persistir la aceptación.
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la aceptación independiente; registrado el detalle en
  `docs/validations/generation-flow-with-validation.md`. Sin cambios de código tras la validación.
- Verificación ejecutada: el validador re-ejecutó `init.sh` con Git Bash (exit 0, `./gradlew test` +
  `./gradlew lint`), `./gradlew testDebugUnitTest --rerun-tasks --no-daemon` (32/32 tareas) y
  `./gradlew lint --rerun-tasks` (28/28 tareas); parseó por su cuenta los XML de resultados y el informe
  de lint, e inspeccionó el diff completo.
- Evidencia capturada: 35 suites, **223 tests, 0 fallos/errores/saltados** (`GenerationServiceTest` 6,
  `GeneratorRequestFactoryTest` 4, `GeneratorProgressMessagesTest` 4 y 209 preexistentes); lint 0 errores
  y 13 warnings preexistentes, 0 en ficheros de la feature; el validador confirmó el caso negativo
  obligatorio (Pan libre con gluten → "Bocadillo de tomate" descartado con `HARD_RESTRICTION` y nunca en
  `accepted`, con `.single{}` que no pasa en vacío), los escenarios 1/3/4/6/7 (Pollo al ajillo por
  `INCOHERENT` y por `DIET_PATTERN` vegetarian; restricciones del Perfil efectivo en ambos sentidos;
  `CancellationException` re-lanzada antes del `catch(Exception)`; generador que lanza → `aiUnavailable`
  sin propagar), la pureza de `domain.generation` y de
  `GeneratorRequestFactory`/`GeneratorProgressMessages` (sin imports Android/Compose), el seam
  `GeneratorResultsPreview` que pinta solo `accepted` sin pre-empting `results-screen`
  (`PushedScreen.RESULTS` sin añadir) y la disciplina de alcance (sin cambios en build/lockfile,
  `domain.generator`, `domain.validation`, `domain.recipe`, `domain.prompt`, `domain.suggestions`,
  `data/**`, la Taxonomía, `seed-recipes.json` ni `init.sh`).
- Ficheros actualizados: `feature_list.json`, `docs/validations/generation-flow-with-validation.md` y este
  registro.
- Hallazgos del validador (3 Low, informativos, ninguno bloqueante): cualquier `Exception` se mapea a
  `aiUnavailable` (decisión de la spec y pregunta abierta 1); el `Validator Checklist` de la spec queda
  sin marcar; los escenarios 5 (Cancelar) y los *stages* Compose no se testean en JVM (para revisión
  manual, según AGENTS.md). Nota de entorno: una pasada forzada murió con
  `Gradle build daemon has been stopped` (probable `--stop` de otro worktree); la re-ejecución con
  `--no-daemon` quedó en verde, sin relación con la feature.
- Riesgo o incógnita abierta: ninguno nuevo. Quedan para revisión manual de la persona tras el merge el
  acabado visual (animación, ritmo de las sublíneas, densidad del preview, claro/oscuro) y las dos pruebas
  de emulador exigidas ("Cancelar" responde volviendo a los ajustes; sin Cancelar solo se listan recetas
  aceptadas y el "Bocadillo de tomate" del falso no aparece con gluten); un defecto visual se corrige con
  una feature correctiva. Rotar durante la generación o el preview vuelve a los ajustes (el resultado vive
  en `remember`), como documenta la spec.
- Siguiente paso: commit local de la rama `feature/generation-flow-with-validation` (lo hace el
  orquestador de `feature-flow`); la publicación queda para `github-flow`. Después, especificar
  `results-screen` (su dependencia ya está `accepted`) con `feature-spec`.


### Sesión 044

- Objetivo: implementar `manual-suggestions-refresh` (selección explícita del orquestador, fuera del
  orden de lista) y dejarla autoverificada para validación independiente.
- Completado: sección **"Sugerencias"** en la pestaña Perfil (insertada tras la cabecera de identidad y
  antes de "Mis preferencias") con la antigüedad del Lote observado y un botón "Actualizar sugerencias".
  Formateador puro `SuggestionsRefreshFormat` (sin Compose/Android) + sección Compose delgada
  `ProfileSuggestionsSection`. `ProfileScreen` recibe `suggestionsGeneratedAt`, `isRefreshingSuggestions`
  y `onRefreshSuggestions`; `MainShell` aporta `rememberCoroutineScope` + un boolean "en vuelo" no
  persistido y cablea `SuggestionBatchService.ensureBatch(profile.preferences, force = true)` sin tocar
  `ProfileRepository`. El refresco no cambia preferencias/identidad/borrador y reusa el mismo Lote
  observado, de modo que la fecha y el carrusel de la Home se recomponen solos. `docs/technical-discovery.md`
  actualizado.
- Verificación ejecutada: `./gradlew testDebugUnitTest --tests "com.sazon.app.ui.profile.*"` (verde) y
  el gate estándar `& "C:\Program Files\Git\bin\bash.exe" init.sh` (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos). Test XML y `lint-results-debug.xml` parseados a mano.
- Evidencia capturada: 33 suites, **220 tests, 0 fallos/errores/saltados** (11 nuevos
  `SuggestionsRefreshFormatTest`; 209 preexistentes en verde). Cubre escenarios 3-5 y límites: respaldo
  sin Lote, hoy / hace 1 / 2 / 3 / 7 días, `1 día - 1 ms` -> hoy, reloj retrasado -> hoy sin negativos,
  `generatedAt` más reciente -> etiqueta más corta, microcopy estable; casos negativos con
  `assertNotEquals`. lint: BUILD SUCCESSFUL, 0 errores y 13 warnings preexistentes
  (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), 0 en
  ficheros de la feature. Inspección: `SuggestionsRefreshFormat` sin ningún import; `force = true` en
  `MainShell.kt:154-157`; sin `ProfileRepository.save` en el refresco; `domain.suggestions`/`data.suggestions`/
  `domain.profile`/`data.profile`/`ProfileDatabase`/Taxonomía/`seed-recipes.json`/`SazonApp`/`Home`/
  `Generator`/`app/build.gradle.kts`/`gradle/libs.versions.toml` intactos; sin dependencias nuevas.
- Ficheros creados/cambiados:
  `app/src/main/java/com/sazon/app/ui/profile/SuggestionsRefreshFormat.kt` (nuevo),
  `app/src/main/java/com/sazon/app/ui/profile/ProfileSuggestionsSection.kt` (nuevo),
  `app/src/test/java/com/sazon/app/ui/profile/SuggestionsRefreshFormatTest.kt` (nuevo),
  `app/src/main/java/com/sazon/app/ui/profile/ProfileScreen.kt` (modificado),
  `app/src/main/java/com/sazon/app/ui/MainShell.kt` (modificado),
  `docs/technical-discovery.md`, `feature_list.json` y este registro.
- Hallazgos relevantes: la primera pasada de `init.sh` falló en `lint` con "Gradle build daemon has been
  stopped: stop command received"; fue un parón del daemon compartido por otra sesión/worktree (el `trap`
  de su `init.sh` ejecuta `./gradlew --stop`), no un fallo de la feature: la segunda pasada terminó con
  exit 0. Se mantiene la incógnita ya registrada en la spec sobre la concurrencia entre el refresco
  manual y el `ensureBatch` de entrada (no se añade sincronización; el botón deshabilitado evita solo el
  doble toque manual).
- Riesgo o incógnita abierta: quedan para revisión manual de la persona tras el merge la prueba de
  emulador exigida (pulsar "Actualizar sugerencias" renueva el Lote sin cambiar ninguna preferencia y
  actualiza la fecha mostrada) y el acabado visual (estado "Actualizando…" deshabilitado, sin Lote la
  fecha cae al respaldo); un defecto visual se corrige con feature correctiva. El estado "en vuelo" no
  sobrevive a un cambio de configuración (estado de UI, como `editingIdentity`).
- Siguiente paso: validación independiente de `manual-suggestions-refresh` con `feature-validator`;
  después, commit local de la rama `feature/manual-suggestions-refresh` (orquestador de `feature-flow`)
  y publicación por `github-flow`. La siguiente feature en orden de lista sigue siendo
  `generation-flow-with-validation`.


### Sesión 045

- Objetivo: validar `manual-suggestions-refresh` de forma independiente y persistir la aceptación.
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la validación independiente. Sin cambios de código tras la
  validación.
- Verificación ejecutada: el validador re-ejecutó `init.sh` con Git Bash (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos), `./gradlew testDebugUnitTest --rerun-tasks` (32/32 tareas) y
  `./gradlew lint --rerun-tasks` (28/28 tareas; el primer intento de lint falló por
  "Gradle build daemon has been stopped" de otro worktree y el reintento pasó); parseó por su cuenta
  los XML de resultados y el informe de lint, e inspeccionó el código.
- Evidencia capturada: 33 suites, **220 tests, 0 fallos/errores/saltados** (`SuggestionsRefreshFormatTest`
  11); lint 0 errores y 13 warnings preexistentes, 0 en ficheros de la feature; escenarios 1–7 y
  `Validator Checklist` en PASS; verificación exigida #1 (`MainShell.kt:154-156` llama a
  `ensureBatch(profile.preferences, force = true)`; el de entrada queda sin `force` y sin
  `ProfileRepository.save` en el refresco) y #2 (la fecha sale de `batch?.generatedAt` del Lote
  observado, sin segunda fuente de verdad); antigüedad en JVM con límites `1 día - 1 ms` → hoy y reloj
  retrasado → hoy sin negativos; botón `enabled = !isRefreshing` con guard de doble disparo; formateador
  sin imports; dominio/persistencia/`ProfileDatabase`/Taxonomía/`seed-recipes.json`/`SazonApp`/Home/
  Generator/build intactos; sin dependencias nuevas.
- Ficheros actualizados: `feature_list.json` y este registro.
- Hallazgos del validador (Low informativos, ninguno bloqueante): los checkboxes de `Implementation
  Tasks`/`Validator Checklist` de la spec quedan sin marcar (convención: el validador no edita ficheros);
  una línea larga cosmética en `docs/technical-discovery.md`; se mantienen las incógnitas ya documentadas
  (estado "en vuelo" no persistido, concurrencia con el `ensureBatch` de entrada y `now` capturado una
  vez).
- Riesgo o incógnita abierta: ninguno nuevo. Quedan para revisión manual de la persona tras el merge la
  prueba de emulador exigida (pulsar "Actualizar sugerencias" renueva el Lote sin cambiar ninguna
  preferencia y actualiza la fecha mostrada) y el acabado visual; un defecto visual se corrige con una
  feature correctiva.
- Siguiente paso: commit local de la rama `feature/manual-suggestions-refresh` (orquestador de
  `feature-flow`); la publicación queda para `github-flow`. La siguiente feature en orden de lista sigue
  siendo `generation-flow-with-validation`.

### Sesión 046

- Fecha: 2026-10-09
- Objetivo: **chore de harness** (no es feature de producto): **Fase 1** del backfill de capturas de UI
  exigidas por la política del PR #37, aplicada a las 11 features de `area: ui` ya `accepted` (que se
  implementaron antes de que existiera la política). Rama `chore/ui-captures-backfill`.
- Completado:
  - Convención de guardado actualizada en `docs/design/concepts/README.md`: **una subcarpeta por
    pantalla** (no por componente) con los **tres ficheros del export de Stitch** (`.png`, `.html` y
    `design.md`, con los nombres que dé Stitch).
  - **11 `docs/design/concepts/<feature-id>/README.md`** nuevos (la "petición" de cada feature) con la
    lista exacta de pantallas esperadas y una línea de qué es cada una: `design-tokens-and-theme`
    (`design-system`); `onboarding-shell-and-identity` (`bienvenida`);
    `onboarding-diet-style-cuisines` (`dieta`, `estilo`, `cocinas`);
    `onboarding-ingredients-safety-kitchen` (`ingredientes`, `alergias`, `tu-cocina`);
    `home-offline-blocks` (`home`, `home-sin-conexion`); `local-result-filters`
    (`filtros-tus-recetas`); `generator-ingredients-step` (`generador-ingredientes`);
    `generator-settings-step` (`generador-ajustes`); `generation-flow-with-validation`
    (`generador-cargando`, `resultados-preview`); `profile-screen` (`perfil`, `perfil-editar`);
    `manual-suggestions-refresh` (`perfil-sugerencias`). Total: **18 pantallas**.
  - `DESIGN.md`: sección "Generated Concept Images" registra el backfill y las pantallas por feature
    (la petición concreta vive en cada `README.md`).
  - **11 specs**: la sección "Visual Design Impact" nombra sus capturas y el impacto en
    `docs/design/concepts/` pasa de `not needed` a `update` (backfill).
- Verificación ejecutada: cambio **solo de documentación** (ningún fichero Kotlin/recursos/gradle), por
  lo que el gate `init.sh` no aplica; revisión manual del árbol `docs/design/concepts/`, de los 11
  `README.md` y de `git status` (24 ficheros, +358/−40).
- Ficheros creados/cambiados: `docs/design/concepts/README.md`, los 11
  `docs/design/concepts/<feature-id>/README.md` (nuevos), `DESIGN.md`, los 11 `docs/specs/<feature>.md`
  y este registro. `feature_list.json` **sin cambios** (ninguna feature cambia de estado).
- Hallazgos relevantes: las 11 features de UI aceptadas no tienen capturas porque son anteriores al
  PR #37; sus specs incluso decían `docs/design/concepts/: not needed`. El backfill no reabre features
  aceptadas: solo deja la petición y, cuando lleguen los exports, se hará la **Fase 2** de
  reconciliación (UI implementada vs captura) **una a una**, abriendo una feature correctiva solo si
  aparece una desviación visual real.
- Riesgo o incógnita abierta: la Fase 2 no puede empezar hasta que la persona exporte las 18 pantallas
  desde Stitch; no hay bloqueo para el resto del plan (`results-screen` sigue siendo la siguiente
  feature de producto).
- Siguiente paso: commit + push + PR del chore (publicación por `github-flow`). Después, **Fase 2**:
  reconciliar feature por feature según lleguen los exports.

### Sesión 047

- Fecha: 2026-10-09
- Objetivo: **chore de harness**: trocear el prompt de Stitch **por pantalla** para que Stitch no lo
  trunque y genere todas las pantallas (el prompt completo `docs/design/stitch-prompt.md` se truncaba).
- Completado: nueva carpeta `docs/design/stitch/` con `README.md` (guía de uso + mapa parte → carpeta de
  `docs/design/concepts/`) y **21 partes** listas para copiar/pegar de una en una en la misma
  conversación de Stitch: `00-contexto.md` (producto, lenguaje visual, navegación, fuera de alcance y
  calidad, se pega primero), `01-design-system`, 7 pasos de onboarding, `home`, `home-sin-conexion`,
  `home-busqueda-ia`, generador (ingredientes, ajustes, cargando), resultados, detalle, favoritos,
  perfil, perfil-editar y perfil-sugerencias. Referencias actualizadas para apuntar al prompt troceado
  (`docs/design/concepts/README.md` y cabecera de `docs/design/stitch-prompt.md`, que sigue siendo la
  fuente completa de referencia).
- Verificación ejecutada: cambio **solo de documentación** (ningún fichero Kotlin/recursos/gradle), por
  lo que el gate `init.sh` no aplica; revisión manual de los 22 ficheros y de `git status`.
- Ficheros creados/cambiados: los 22 ficheros de `docs/design/stitch/`, `docs/design/concepts/README.md`,
  `docs/design/stitch-prompt.md` y este registro.
- Hallazgos relevantes: el prompt largo se trunca; pegarlo en partes pequeñas y en la **misma** sesión
  (no sesiones distintas) es lo que permite generar todo. La hoja "Design System" es una parte propia.
- Riesgo o incógnita abierta: ninguno nuevo. Las partes de Resultados, Detalle, Favoritos y búsqueda IA
  corresponden a features aún `not_started` (igual que en el prompt original).
- Siguiente paso: la persona pega las partes en Stitch en orden y guarda cada export en
  `docs/design/concepts/<feature-id>/<pantalla>/`; después, **Fase 2** de reconciliación una a una.

### Sesión 048

- Fecha: 2026-10-09
- Objetivo: ejecutar `feature-flow` para la feature `results-screen` (única seleccionada), en modo
  hasta-aceptación. Solo planner/implementer/validator; **sin push ni PR** (publica `github-flow`).
- Completado:
  - Selección: `results-screen` (`area: ui`, `depends_on: generation-flow-with-validation` ya
    `accepted`), sin spec previa. Rol 1 ejecutado: **planner** (`feature-spec`).
  - Spec creada: `docs/specs/results-screen.md` (514 líneas). Fija la fórmula de `matchScore`
    (derivada, en memoria, no persistida; media ponderada de cobertura/gustados/cocina/tiempo/
    dificultad menos penalización por evitados; ejemplo dorado 86/76), los estados poblado/vacío/
    `Unavailable` (con "Reintentar") y el subtítulo de búsqueda. Sustituye y elimina
    `GeneratorResultsPreview`.
  - **Gate de UI**: la spec es de UI y exige capturas de referencia en
    `docs/design/concepts/results-screen/` antes de implementar. No existen. Se crea la carpeta con
    `docs/design/concepts/results-screen/README.md` (petición) y se **detiene** el flujo, marcando la
    feature `blocked` (regla de `feature-flow` y `AGENTS.md`): no se lanza el `implementer`.
- Verificación ejecutada: ninguna de código (no hay implementación). Revisión de artefactos: la spec
  nombra las cuatro pantallas con sus tres ficheros cada una; `docs/design/concepts/results-screen/`
  solo contiene el `README.md` de petición; `git status` sin cambios fuera de la spec, el README y el
  harness.
- Ficheros creados/cambiados: `docs/specs/results-screen.md` (planner),
  `docs/design/concepts/results-screen/README.md` (nuevo), `feature_list.json` (`results-screen` a
  `blocked`) y este registro.
- Hallazgos relevantes: el planner (rol limitado a `docs/specs/`) nombra las capturas pero no crea la
  carpeta; esa petición la materializa el orquestador, como exige el gate de UI. El bloqueo es de
  entrada, no técnico: falta que la persona exporte de Stitch las cuatro pantallas.
- Riesgo o incógnita abierta: **bloqueada** `results-screen` hasta que existan las capturas de
  `docs/design/concepts/results-screen/{resultados,resultados-busqueda,resultados-vacio,resultados-error}/`
  (cada una con `.png`, `.html` y `design.md`). Sigue pendiente además la Fase 2 de reconciliación de
  las 11 features de UI ya aceptadas (sesión 046).
- Siguiente paso: la persona pega en Stitch la parte `resultados` (más los estados vacío/error) y deja
  los exports en `docs/design/concepts/results-screen/`; entonces se desbloquea la feature y el
  orquestador lanza el `implementer`.

### Sesión 049

- Fecha: 2026-10-09
- Objetivo: implementar `results-screen` (pantalla de Resultados real) según
  `docs/specs/results-screen.md`. Sesión reanudada: el trabajo ya existía en disco sin commitear y sin
  los artefactos de harness/docs; esta sesión lo revisa, lo reconcilia con la spec, completa lo que
  faltaba y verifica. El gate de capturas de UI (que bloqueaba la sesión 048) queda satisfecho: existe
  `docs/design/concepts/results-screen/resultados/` con sus tres ficheros.
- Completado: `com.sazon.app.ui.results` como lógica **pura** testeable en JVM más capa Compose delgada.
  `MatchScore.of(...)` fija la fórmula **derivada y no persistida** de `matchScore` (media ponderada
  `Σ(w·f)/Σw` de cobertura/`W_COVERAGE`, gustados/`W_LIKED`, cocina/`W_CUISINE`, tiempo/`W_TIME` y
  dificultad/`W_DIFFICULTY`, menos `DISLIKE_PENALTY` por evitado presente; ejemplo dorado 86/76; sin
  `Random` ni reloj). `ResultsStatus.from(...)` puntúa las aceptadas con `copy(matchScore = ...)` y las
  ordena por relevancia (`Content`/`Empty`/`Unavailable`); `ResultsHeader`/`ResultsOrigin`/`ResultsHeaders.of`
  formatean el encabezado (Generador con singular/plural y franja, o Búsqueda con la consulta entre
  comillas). Compose: `ResultsScreen` (`Scaffold`: topBar "Atrás" + título/subtítulo, bottomBar "Cambiar
  ingredientes", estados poblado/vacío/degradado con "Reintentar") y `ResultRecipeCard` (degradado+glifo
  16:10, título, descripción, meta tabular y píldora de coincidencia; sin corazón ni click). Integración:
  `MainShell` pasa `seedRecipes` a `GeneratorFlow`, que en `result != null` monta `ResultsScreen` con
  `onBack` (→ ajustes), `onChangeIngredients` (→ ingredientes) y `onRetry` (incrementa el token); se
  elimina `GeneratorResultsPreview.kt`.
- Verificación ejecutada: `init.sh` con Git Bash (exit 0, `./gradlew test` + `./gradlew lint`, sin
  procesos largos); antes, `./gradlew testDebugUnitTest --tests "com.sazon.app.ui.results.*"` con parseo
  propio de los XML. Inspección de pureza, del fichero eliminado, de que no cambian build/lockfile/
  dominio/`data`/Taxonomía/`seed-recipes.json`/`MainNavigationState.kt`/`init.sh` y de que no hay hex
  sueltos.
- Evidencia capturada: **255 tests en 39 suites, 0 fallos/errores/saltados** (234 preexistentes + 21
  nuevos: `MatchScoreTest` 8, `ResultsHeaderTest` 7, `ResultsStatusTest` 6); lint 0 errores y 13 warnings
  preexistentes (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2,
  OldTargetApi 1), ninguno en ficheros de la feature; `init.sh` con código 0; `GeneratorResultsPreview.kt`
  eliminado y sin referencias en `app/src`; `MatchScore`/`ResultsStatus`/`ResultsHeader` solo importan
  `domain.*`/`java.text.Normalizer`; sin dependencias nuevas ni cambios en el build, el lockfile, el
  dominio, `data/**`, la Taxonomía, `seed-recipes.json` ni `init.sh`.
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/ui/results/{MatchScore,ResultsStatus,ResultsHeader,ResultsScreen,ResultRecipeCard}.kt`,
  `app/src/test/java/com/sazon/app/ui/results/{MatchScoreTest,ResultsStatusTest,ResultsHeaderTest}.kt`,
  `app/src/main/java/com/sazon/app/ui/generator/GeneratorFlow.kt` y `app/src/main/java/com/sazon/app/ui/MainShell.kt`
  (integración), `GeneratorResultsPreview.kt` (eliminado), `docs/technical-discovery.md`,
  `docs/domain-model.md`, `docs/risks-and-open-questions.md`, `DESIGN.md` (`results-screen/resultados`),
  `feature_list.json` (`results-screen` a `passing`) y este registro.
- Hallazgos relevantes: la fórmula y los estados ya estaban implementados y en verde; no hubo que
  corregir código, solo completar los artefactos de harness/docs y verificar. `./gradlew testDebugUnitTest
  --tests "...ui.results.*"` deja el daemon vivo (el `trap` de `init.sh` lo evita); se paró con `./gradlew --stop`.
- Riesgo o incógnita abierta: falta la **revisión manual** de emulador que `AGENTS.md` reserva a la
  persona (estado vacío; error con "Reintentar"; "Cambiar ingredientes" conservando el borrador; acabado
  contra `docs/design/concepts/results-screen/resultados/`). Sigue pendiente además la Fase 2 de
  reconciliación de capturas de features de UI ya aceptadas (sesión 046).
- Siguiente paso: validación independiente de `results-screen` con `feature-validator`; después, commit
  local (lo hace el orquestador de `feature-flow`) y publicación por `github-flow`.

### Sesión 050

- Fecha: 2026-10-10
- Objetivo: reanudar e implementar `recipe-detail-screen` (Detalle de receta con raciones ajustables)
  según `docs/specs/recipe-detail-screen.md`. Una ejecución previa se interrumpió y dejó el trabajo en
  disco sin commitear y sin los artefactos de harness/docs; esta sesión lo revisa contra la spec,
  completa lo que faltaba (el gate de UI ya estaba satisfecho con las capturas de
  `docs/design/concepts/recipe-detail-screen/detalle-receta/`), corrige una desviación del test de
  escalado y lo autoverifica sin rehacer lo ya válido.
- Completado: lógica **pura** en `com.sazon.app.ui.detail` testeable en JVM (sin Compose ni Android):
  `ServingScaler.scaledIngredients(recipe, targetServings)` con **redondeo por unidad** (no lineal),
  `GRAM_STEP = 5` (`g`/`ml`), `UNIT_STEP = 0.5` (`ud`/`cda`/`cdta`), half-up (`Math.round`), mínimo el
  paso si `quantity > 0`, `unit = null` ("al gusto") **no se escala**, `factor == 1` intacto y sin
  división por cero (`servings.coerceAtLeast(1)`); `IngredientFormat.quantityLabel` (`"½"`, `"4½ ud"`,
  `"300 g"`, `"Al gusto"`, nunca `"0.5"`); `RecipeDetailFormat.metaEntries` (Tiempo/Dificultad real con
  respaldo al `id`/Comensales/Calorías con `"—"` sin nutrición, sin `matchScore`) y `ADJUSTED_CAPTION`;
  `RecipeDetailTab` + `RecipeDetailState` (`initial`/`setServings` con `coerceIn`/`selectTab`, inmutable,
  sin guardar el `Recipe`). Capa Compose **delgada** `RecipeDetailScreen` (héroe degradado + "Atrás" y
  corazón; tarjeta superpuesta con meta; pestañas Ingredientes/Pasos/Notas; `ServingsStepper`
  reutilizado **sin cambios** + caption; filas con checkbox y cantidad en cifras tabulares; timeline con
  píldora ámbar; tips + rejilla de nutrición opcional; barra inferior "Guardar"/"Empezar a cocinar";
  `RecipeDetailStateSaver`). Integración: `RecipeCard`/`ResultRecipeCard` ganan `onClick` opcional;
  `HomeScreen`/`ResultsScreen`/`GeneratorFlow` reciben `onOpenRecipe`; `MainShell` hospeda el detalle
  como **overlay** (`var detailRecipe by remember`) encima de la Home/Generador **sin desmontarlos**;
  `isSaved = false`, `onToggleSave = {}`, `onStartCooking = {}` como costuras no-op de
  `favorites`/modo cocina. Tests JVM con los vectores dorados (escenarios 1-4) y casos negativos.
  Sin dependencias nuevas; no se tocan `app/build.gradle.kts`, `gradle/libs.versions.toml`, `domain/**`,
  `data/**`, la Taxonomía, `seed-recipes.json`, `ui.preferences/ServingsStepper.kt`,
  `ui.navigation/MainNavigationState.kt` ni `init.sh`.
- Verificación ejecutada: `& "C:\Program Files\Git\bin\bash.exe" init.sh` (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos); parseo propio de los XML de resultados
  (`test-results/testDebugUnitTest/TEST-*.xml`) y del informe `lint-results-debug.xml`; inspección de
  imports de la lógica pura (sin Compose/Android) y de `git status` (solo los ficheros previstos).
- Evidencia capturada: **43 suites, 277 tests, 0 fallos/errores/saltados** (22 nuevos:
  `ServingScalerTest` 8, `IngredientFormatTest` 5, `RecipeDetailStateTest` 5, `RecipeDetailFormatTest` 4;
  255 preexistentes en verde); lint 0 errores y 13 warnings preexistentes (GradleDependency 7,
  NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), 0 en ficheros de la feature;
  `init.sh` con código 0; `ServingScaler`/`IngredientFormat`/`RecipeDetailFormat`/`RecipeDetailState`
  sin imports Compose/Android; `ServingsStepper`, `MainNavigationState`, el build/lockfile, el dominio,
  `data/**`, la Taxonomía, `seed-recipes.json` e `init.sh` sin cambios.
- Ficheros creados/cambiados:
  `app/src/main/java/com/sazon/app/ui/detail/{ServingScaler,IngredientFormat,RecipeDetailFormat,
  RecipeDetailState,RecipeDetailScreen}.kt` (nuevos),
  `app/src/test/java/com/sazon/app/ui/detail/{ServingScalerTest,IngredientFormatTest,
  RecipeDetailFormatTest,RecipeDetailStateTest}.kt` (nuevos),
  `app/src/main/java/com/sazon/app/ui/{MainShell.kt, generator/GeneratorFlow.kt, home/HomeScreen.kt,
  home/RecipeCard.kt, results/ResultRecipeCard.kt, results/ResultsScreen.kt}` (modificados),
  `docs/technical-discovery.md`, `docs/domain-model.md`, `DESIGN.md`, `feature_list.json` y este
  registro.
- Hallazgos relevantes: la inspección inicial detectó un **defecto real** en
  `ServingScalerTest.una_receta_sin_raciones_no_divide_por_cero`: con `recipe.servings = 0` y
  `targetServings = 2` el factor es `2` (por `coerceAtLeast(1)`), así que `Patata` queda en `1200 g` y no
  en `600 g` como asertaba el test original; corregido el vector (y añadido `Huevo = 12 ud`) para
  alinearlo con la fórmula de la spec. El test pasó a verde en la siguiente ejecución. El resto de la
  lógica y la capa Compose ya eran correctas.
- Riesgo o incógnita abierta: quedan para revisión manual de la persona tras el merge la prueba de
  emulador exigida (abrir una receta desde Resultados y desde la Home, cambiar las raciones y ver las
  cantidades actualizarse, recorrer las tres pestañas y "Atrás" sin perder resultado/borrador) y el
  acabado visual contra `docs/design/concepts/recipe-detail-screen/detalle-receta/`; un defecto visual se
  corrige con feature correctiva. Rotar con el detalle abierto lo cierra (`detailRecipe` vive en
  `remember`), como documenta la spec (Unknown). Se mantiene la Fase 2 de reconciliación de capturas de
  las features de UI previas (sesión 046).
- Siguiente paso: validación independiente de `recipe-detail-screen` con `feature-validator`; después,
  commit local de la rama (lo hace el orquestador de `feature-flow`) y publicación por `github-flow`. La
  siguiente feature en orden de lista es `favorites` (espera a que `recipe-detail-screen` quede
  `accepted`).

### Sesión 051

- Fecha: 2026-10-10
- Objetivo: validar `recipe-detail-screen` de forma independiente y persistir la aceptación. Ejecutada
  por el orquestador de `feature-flow` en la rama `feature/recipe-detail-screen` (worktree), solo
  hasta-aceptación; sin push ni PR (publica `github-flow`).
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la validación independiente. Sin cambios de código tras la
  validación.
- Verificación ejecutada: el validador re-ejecutó `init.sh` con Git Bash (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos) y, al ver la primera pasada en `UP-TO-DATE`, forzó
  `./gradlew testDebugUnitTest --rerun-tasks` (32/32 tareas) y `./gradlew lint --rerun-tasks`; parseó
  por su cuenta los XML de resultados y el informe `lint-results-debug.xml`, e inspeccionó el código.
- Evidencia capturada: 43 suites, 277 tests, 0 fallos/errores/saltados (22 nuevos: `ServingScalerTest`
  8, `IngredientFormatTest` 5, `RecipeDetailStateTest` 5, `RecipeDetailFormatTest` 4; 255 preexistentes
  en verde); lint 0 errores y 13 warnings preexistentes, 0 en ficheros de `ui.detail`. El validador
  recalculó a mano el escalado contra `seed-recipes.json` real (`seed-tortilla-patatas`, `servings` 4):
  4→2 da `300 g`/`3 ud`/`½ ud`/`75 ml` y `Sal` sin escalar; 4→3 da `450 g`/`4½ ud`/`1 ud`/`115 ml`;
  factor 1 devuelve el original intacto; `unit = null` nunca escala; `servings = 0` no divide por cero;
  determinista. Confirmó pureza de `ui.detail` (sin Compose/Android salvo `RecipeDetailScreen`, sin hex
  sueltos), `ServingsStepper` reutilizado sin cambios, el detalle como overlay en `MainShell` (no un
  `PushedScreen` nuevo) con las costuras no-op, la captura obligatoria `detalle-receta/`
  (`screen.png`/`code.html`/`DESIGN.md`) presente y seguida, y disciplina de alcance (build/lockfile,
  dominio, `data/**`, la Taxonomía, `seed-recipes.json`, `MainNavigationState.kt` e `init.sh` intactos;
  sin dependencias nuevas). Confirmó además que el implementer no hizo commit/push/PR.
- Ficheros actualizados: `feature_list.json` y este registro.
- Hallazgos del validador (2 Low informativos, no bloqueantes): el `README.md` de capturas repite la
  plantilla con `design.md` en minúscula mientras el fichero real es `DESIGN.md` (inocuo en Windows,
  heredado de la plantilla); `RecipeDetailFormatTest.la_caption_del_ajustador_es_estable` es una
  aserción trivial (la cobertura real está en el resto del test). Sin hallazgos Critical/High/Medium.
- Riesgo o incógnita abierta: ninguno nuevo. Quedan para revisión manual de la persona tras el merge la
  prueba de emulador exigida (abrir una receta desde Resultados y desde la Home, cambiar las raciones y
  ver las cantidades actualizarse, recorrer las tres pestañas y "Atrás" sin perder resultado/borrador) y
  el acabado visual contra `docs/design/concepts/recipe-detail-screen/detalle-receta/`; un defecto visual
  se corrige con una feature correctiva. Se mantiene la Fase 2 de reconciliación de capturas de las
  features de UI previas (sesión 046).
- Siguiente paso: commit local de la rama `feature/recipe-detail-screen` (lo hace el orquestador de
  `feature-flow`); la publicación queda para `github-flow`. Después, `favorites` (ya con sus dos
  dependencias `accepted`).

### Sesión 052

- Fecha: 2026-10-10
- Objetivo: implementar `favorites` (Favoritos disponibles sin conexión) según
  `docs/specs/favorites.md`, en la rama `feature/favorites` (worktree); solo hasta implementación y
  autoverificación (`passing`), sin commit/push/PR (los hace el orquestador).
- Completado: dominio puro `com.sazon.app.domain.favorites` (`Favorite` con **copia** de la `Recipe` +
  `savedAt`, el puerto `FavoriteRepository` y `FavoriteService.toggle` con reloj inyectable que
  normaliza `matchScore` a `null`); persistencia Room en `com.sazon.app.data.favorites` sobre base
  **propia** `favorites.db` (version 1, tabla **multi-fila** `favorite_recipe` con la receta como
  columna JSON vía `RecipeJson`, `upsert` idempotente por `recipeId` y orden `savedAt` desc); lógica de
  presentación pura `com.sazon.app.ui.favorites` (`FavoritesSort`, `FavoritesViewMode`,
  `FavoritesList.visible`/`countLabel`) y capa Compose delgada `FavoritesScreen` (cabecera "Mis recetas
  guardadas" + conteo, buscador, chips de orden terracota, toggle lista/rejilla oliva, nota
  "Disponibles sin conexión", estado vacío con marcador y "Explorar recetas", `Saver` de
  `rememberSaveable`) y `FavoriteRecipeCard`/`FavoriteRecipeTile` (miniatura 96x96 degradada, meta en
  cifras tabulares y corazón; swipe "Eliminar" vía `SwipeToDismissBox` de Material 3). Integración:
  `MainActivity` compone `FavoritesDatabase`/repositorio/servicio, `SazonApp` las pasa a `MainShell`,
  éste observa los favoritos para la pestaña, alimenta `HomeRecipes.saved` (bloque "Guardadas") y
  cablea `isSaved`/`onToggleSave` del Detalle; `ui/SectionPlaceholder.kt` eliminado. Sin Resumen de
  actividad, sin "Abiertas hace poco", sin modo cocina, sin Búsqueda con IA ni proveedor real, sin
  dependencias nuevas.
- Verificación ejecutada: `& "C:\Program Files\Git\bin\bash.exe" init.sh` (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos); se parsearon por separado los XML de resultados y el informe
  `lint-results-debug.xml`.
- Evidencia capturada: **46 suites, 301 tests, 0 fallos/errores/saltados**; 24 nuevos:
  `FavoriteServiceTest` 6, `FavoritesListTest` 9, `RoomFavoriteRepositoryTest` 9 (277 preexistentes en
  verde). Lint 0 errores y 13 warnings preexistentes (GradleDependency 7, NewerVersionAvailable 3,
  AndroidGradlePluginVersion 2, OldTargetApi 1), ninguno con localización bajo `app/src`. Inspección:
  `domain.favorites` y la lógica pura de `ui.favorites` sin imports `android.*`/`androidx.*`;
  `SectionPlaceholder` sin referencias en `app/src`; `git diff --stat` sin cambios en
  `app/build.gradle.kts`, `gradle/libs.versions.toml`, el dominio/Lote/Perfil, las bases ajenas, la
  Taxonomía ni `seed-recipes.json`.
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/domain/favorites/{Favorite,FavoriteRepository,FavoriteService}.kt`,
  `app/src/main/java/com/sazon/app/data/favorites/{FavoriteEntity,FavoriteDao,FavoritesDatabase,FavoritesMappers,RoomFavoriteRepository}.kt`,
  `app/src/main/java/com/sazon/app/ui/favorites/{FavoritesSort,FavoritesViewMode,FavoritesList,FavoritesScreen,FavoriteRecipeCard}.kt`,
  `app/src/test/java/com/sazon/app/domain/favorites/FavoriteServiceTest.kt`,
  `app/src/test/java/com/sazon/app/ui/favorites/FavoritesListTest.kt`,
  `app/src/test/java/com/sazon/app/data/favorites/RoomFavoriteRepositoryTest.kt`,
  `MainActivity.kt`, `ui/SazonApp.kt`, `ui/MainShell.kt` (modificados), `ui/SectionPlaceholder.kt`
  (eliminado), `docs/technical-discovery.md`, `DESIGN.md`, `feature_list.json` y este registro.
- Hallazgo de entorno: la primera ejecución del test JVM (R8/`testDebugUnitTest`) abortó con un
  `EXCEPTION_ACCESS_VIOLATION` en un hilo GC de Corretto 21 (`-Xmx512m`, `jvm.dll`), sin relación con el
  código nuevo (la compilación Kotlin de main y test terminó sin errores); la re-ejecución de `init.sh`
  fue en verde. Se registra como flake de entorno.
- Riesgo o incógnita abierta: ninguno nuevo. Quedan para revisión manual de la persona tras el merge
  las pruebas exigidas (guardar una receta generada por IA y verla tras reiniciar sin conexión; el
  estado vacío con "Explorar recetas" llevando a Inicio) y el acabado visual (lista/rejilla, chips,
  corazón, swipe "Eliminar", claro/oscuro) contra `docs/design/concepts/favorites/favoritos/`; un
  defecto visual se corrige con una feature correctiva.
- Siguiente paso: validación independiente de `favorites` con `feature-validator`.

### Sesión 053

- Fecha: 2026-10-10
- Objetivo: validar `favorites` de forma independiente y persistir la aceptación.
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la aceptación independiente. Commit local de la rama
  `feature/favorites` (lo hace el orquestador de `feature-flow`); la publicación queda para
  `github-flow`. Sin cambios de código tras la validación.
- Verificación ejecutada: el validador re-ejecutó `init.sh` con Git Bash (exit 0) y, al ver test/lint en
  `UP-TO-DATE`, forzó `./gradlew testDebugUnitTest --rerun-tasks` (32/32 tareas) y
  `./gradlew lint --rerun-tasks` (28/28 tareas); parseó por su cuenta los XML de resultados y el informe
  de lint.
- Evidencia capturada: 46 suites, 301 tests, 0 fallos/errores/saltados (`FavoriteServiceTest` 6,
  `FavoritesListTest` 9, `RoomFavoriteRepositoryTest` 9); lint 0 errores y 13 warnings preexistentes, 0
  con localización bajo `app/src`. Confirmó los escenarios 1-2 y 10 (copia con `savedAt=now`,
  `matchScore` normalizado a `null`, toggle idempotente, dos filas coexisten, `remove` selectivo,
  `clear`, `observe`, orden `savedAt` desc) y 5-6 (labels, tres órdenes, búsqueda por título insensible a
  mayúsculas/acentos, caso negativo por descripción, sin coincidencias vacío, `fromKey` con fallback);
  3/4/7/8/9 por inspección de la integración. Pureza de `domain.favorites` y de la lógica pura de
  `ui.favorites`, base Room propia `favorites.db` v1 con `@Query` parametrizado, disciplina de alcance
  (build/lockfile/dominio/Lote/Perfil/bases ajenas/Taxonomía/`seed-recipes.json` intactos, sin
  dependencias nuevas) y capturas obligatorias presentes
  (`docs/design/concepts/favorites/favoritos/{screen.png,code.html,DESIGN.md}` + `README.md`).
  Seguridad: sin secretos, sin red, sin construcción de SQL por concatenación.
- Ficheros actualizados: `feature_list.json` y este registro.
- Hallazgos del validador (3 Low, informativos, ninguno bloqueante): `README.md` de capturas con
  `design.md` en minúscula heredado de la plantilla; microcopy aditivo "No hay favoritas con esta
  búsqueda." fuera de la spec pero coherente; `Validator Checklist` de la spec sin marcar.
- Riesgo o incógnita abierta: ninguno nuevo. Quedan para revisión manual de la persona tras el merge las
  dos pruebas exigidas (guardar una receta generada por IA y verla tras reiniciar sin conexión; el estado
  vacío con "Explorar recetas" llevando a Inicio) y el acabado visual contra
  `docs/design/concepts/favorites/favoritos/`; un defecto visual se corrige con una feature correctiva.
- Siguiente paso: `activity-summary` (depende de `profile-screen` y `favorites`, ambas `accepted`),
  empezando por `planner`/`feature-spec`.

### Sesión 054

- Fecha: 2026-10-11
- Objetivo: analizar y documentar (sin implementar) la propuesta de backend post-MVP para cuenta,
  sincronización y proxy de IA.
- Completado: creado `docs/backend-proposal.md` con la opción B (**Kotlin + Ktor + PostgreSQL**),
  alternativas consideradas, alcance por servicios, superficie HTTP propuesta, modelo de datos,
  autenticación/seguridad, estrategia de sincronización offline-first (Room como verdad local,
  servidor como réplica, LWW + tombstones + WorkManager), módulo de dominio compartido (KMP) para
  reutilizar `RecipeValidator`/`PromptBuilder`/`PreferenceSignature`, despliegue y operaciones, plan por
  fases (backlog propuesto), impacto en el harness (ADR 0004 sugerido), riesgos, preguntas abiertas y un
  checklist de retomada.
- Verificación ejecutada: ninguna de código (cambio solo documental). No se ha tocado
  `feature_list.json`, `init.sh`, el build ni el código de la app; `docs/backend-proposal.md` no se cita
  como decisión aceptada.
- Evidencia capturada: documento versionado en `docs/backend-proposal.md`; referencia añadida en el
  estado de este registro; ninguna feature cambia de estado (WIP = 0).
- Ficheros actualizados: `docs/backend-proposal.md` (nuevo) y este registro.
- Hallazgo relevante: el backend **revierte** el no-objetivo permanente de `build-brief.md` y el
  `ADR 0002`; construirlo exige un ADR nuevo que ajuste el 0002 y actualizar discovery (recogido en
  §13 del documento). La propuesta explota que el dominio ya es puro y que la app define puertos
  (`ProfileRepository`, `FavoriteRepository`, `SuggestionBatchRepository`, `RecipeGenerator`), por lo
  que la integración es, sobre todo, adaptadores detrás de esos puertos más un motor de sync.
- Riesgo o incógnita abierta: sin resolver a propósito (el desarrollo no se inicia ahora). Preguntas
  abiertas en §15 del documento (identidad email/password vs social, qué se sincroniza, hosting).
- Siguiente paso: sin cambios en el plan del MVP; si se decide construir el backend, empezar por la
  fase 1 (`shared-domain-module`) y por el ADR 0004, según el checklist de §16 del documento.
