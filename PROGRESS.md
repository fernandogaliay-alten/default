# Registro de progreso

## Estado verificado actual

- Raíz del repositorio: `C:\Users\fernando.galiay\Documents\Default Project`
- Camino estándar de arranque: `init.sh` con Git Bash (`bash init.sh` si `Git\bin` está en el PATH;
  si no, `& "C:\Program Files\Git\bin\bash.exe" init.sh`)
- Camino estándar de verificación: `./gradlew test` y `./gradlew lint`, ambos vía `init.sh`
- Flujo de entrega: `github-flow` (worktrees + issues/PR) sobre `feature-flow`; rama base `main` y
  remoto `origin` → `https://github.com/fernandogaliay-alten/default.git` (privado)
- Últimas features aceptadas: `fake-recipe-generator` y `recipe-validator` (validador independiente,
  veredicto `accept`)
- Siguiente feature lista: `prompt-builder` (dependencias `taxonomy-in-code` y `preference-signature`
  ya `accepted`)
- Bloqueo actual: ninguno
- Última verificación: 2026-10-05 (`recipe-validator`: `init.sh` exit 0, 38 tests/0 fallos;
  `fake-recipe-generator`: 32 tests/0 fallos; lint 0 errores y 21 warnings preexistentes, sin nuevos)

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
