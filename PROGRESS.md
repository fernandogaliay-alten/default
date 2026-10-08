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
  `onboarding-diet-style-cuisines` y `onboarding-ingredients-safety-kitchen` (validador independiente,
  veredicto `accept`). El onboarding queda **completo** (7 pasos) y persiste el Perfil al cerrar.
- Siguiente feature lista en orden de lista con dependencias satisfechas (sin empezar):
  `suggestion-batch-cache` (dependencias `preference-signature`, `fake-recipe-generator` y
  `seed-recipe-catalog`, ya `accepted`)
- Feature en curso: ninguna (0 `in_progress`, 0 `passing`)
- Bloqueo actual: ninguno
- Última verificación: 2026-10-08 (`onboarding-ingredients-safety-kitchen`: `init.sh` exit 0, 110
  tests/0 fallos, 17 nuevos — `OnboardingIngredientsDraftTest` 9, `OnboardingProfileBuilderTest` 4 e
  `IngredientsFilterTest` 4; lint 0 errores y 13 warnings preexistentes, ninguno en ficheros de la
  feature; validación independiente `accept`)

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

