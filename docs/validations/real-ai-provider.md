# Validation Record: real-ai-provider

- Feature: `real-ai-provider`
- Spec: `docs/specs/real-ai-provider.md`
- Date: 2026-10-12
- Validator: agente con contexto limpio (rol `validator`, `feature-validator`)
- Verdict: **accept**

## Evidence re-executed independently

- `bash init.sh` (WSL, vía `scripts/gradle.sh`) → exit 0 (`./gradlew test` + `./gradlew lint`, sin
  procesos largos).
- `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` → BUILD SUCCESSFUL.
- Parseo propio de `app/build/test-results/testDebugUnitTest/*.xml` → **57 suites, 371 tests, 0
  fallos/errores/saltados**; 28 nuevos de `data.ai` (`AiProviderPolicyTest` 8, `AiRecipePayloadTest`
  5, `DevExpertRecipeGeneratorTest` 15) + 343 preexistentes en verde. Los números coinciden
  exactamente con los del implementer (reproducidos, no aceptados de su resumen).
- `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*" checkDebugAarMetadata`
  → exit 0 (3 suites / 28 tests / 0 fallos; el pin de OkHttp deja `checkDebugAarMetadata` en verde).
- Parseo propio de `lint-results-debug.xml` → **0 errores, 13 warnings** preexistentes
  (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1);
  **0 en ficheros `data/ai`**.

## Checks on the critical points

- **Política (`AiProviderPolicy`)** — único sitio con las constantes: connect 10 s / socket 30 s /
  request 40 s / deadline 60 s / `MAX_ATTEMPTS = 2` / backoff 500 ms / `Retry-After` ≤ 5 s.
  `isRetryableStatus` = 408, 429 y 5xx, excluyendo 400/401/403/404/422 (casos negativos:
  200/201/302/4xx/499). `retryDelayMillis` cubre 2→2000, 5→5000, 0→0, 30/`abc`/`-1`/fecha
  HTTP/`null`→500. `attemptRequestTimeoutMillis` recorta `min(40 s, restante)`; `canRetry` en la
  frontera del deadline (`501`→true, `500`→false, `attempt=2`→false).
- **200 ilegible o vacío NO lanza** — `AiRecipePayload.decode`: `null`/blanco/valla markdown/`choices`
  vacío/HTML/no-JSON → lista vacía vía `RecipeValidator.validateResponse` elemento a elemento con
  restricciones neutras (`Malformed` → vacío). Cinco cuerpos probados, cada uno con 1 sola llamada.
- **`CancellationException` siempre relanzada** — `catch (CancellationException) { throw }` antes de
  cada `catch` en `sendAttempt`; test de cancelación en vuelo confirma la propagación.
- **Clave ausente → modo semilla sin HTTP** — `apiKey.isBlank()` lanza `MISSING_KEY` con 0 peticiones;
  con el `GenerationService` real → `aiUnavailable = true`.
- **El proveedor no pre-filtra por dieta/restricciones** — verificado en `RecipeValidator`: con
  `dietPatternId = ""` (`dietPattern("") == null`) la regla de dieta es no-op y `hardRestrictionIds`
  vacío deja la regla de restricciones sin candidatos, así que llegan el pollo (`poultry`) y el "Pan"
  (gluten); con el `AiSearchService` real se producen `DIET_PATTERN`/`HARD_RESTRICTION` y el estado
  **Bloqueado** sigue siendo alcanzable.
- **Desviación OkHttp (aceptada y documentada)** — Ktor 3.6.0 arrastra OkHttp 5.5.0 (`minCompileSdk
  = 37`), que rompía `checkDebugAarMetadata` con el `compileSdk` del repo (API 36.1); se fuerza
  OkHttp **5.4.0** con `resolutionStrategy.force` (`configurations.configureEach`, solo el pin de
  `okhttp`), sin cambiar el engine ni `compileSdk`. Documentado en `app/build.gradle.kts`,
  `gradle/libs.versions.toml`, `docs/technical-discovery.md` ("Constraints") y la spec (Unknowns).
  No oculta cambios de build fuera de alcance.
- **Docs durables** — `docs/risks-and-open-questions.md`: la pregunta abierta 1 está en "Resueltas"
  con la política fija citando la spec, y la tabla "Implementation-Time Questions" queda sin filas con
  nota explícita; "Blocking Next Phase" coherente. `docs/technical-discovery.md` y
  `docs/domain-model.md` quedan alineados.
- **Visual Design Impact** — `area: integration`; no se piden ni hacen falta capturas (no existe
  `docs/design/concepts/real-ai-provider/`).

## Security

Sin hallazgos.

- **Secretos:** el valor real de `devexpert.apiKey` no aparece en el árbol versionado, logs, mensajes
  de error, tests ni evidencia; `local.properties` sigue ignorado por git; los tests usan la clave
  ficticia `test-key`.
- **Exposición de datos:** `AiCallEvent` y los mensajes de `AiUnavailableException` no incluyen la
  clave, la cabecera `Authorization` ni los cuerpos.
- **Servicios externos:** endpoint desde constante de confianza, reintentos/timeouts acotados y
  `expectSuccess = false` con inspección explícita del estado HTTP.
- **Dependencias:** Ktor necesario y aislado en `data.ai`; pin de OkHttp justificado. Sin cambios de
  autenticación (el riesgo de clave en el APK ya está asumido en ADR 0002 y esta feature no lo altera).

## Findings by severity

- **Critical:** ninguno.
- **High:** ninguno.
- **Medium:** ninguno.
- **Low / informativos (no bloquean):**
  1. El downgrade forzado a OkHttp 5.4.0 deja la ruta real del engine OkHttp sin cobertura automática
     (los tests JVM usan `MockEngine`); mitigado porque la verificación end-to-end es **manual** y
     queda anotada como tal.
  2. `parseContent` usa `runCatching` (podría tragarse un `CancellationException` en teoría); aquí es
     síncrono y sin suspensión, sin impacto práctico.
  3. `AiCallEvent.detail` para fallos HTTP es solo `HTTP <status>`; el diagnóstico del intento se
     conserva en `attempt` (cosmético).

## Manual verification pending (person, not automated)

Con el emulador de Android Studio en **Windows** (el `adb` de WSL no lo ve):

1. Con `devexpert.apiKey` válida en `local.properties`: `bash scripts/gradle.sh assembleDebug` y
   ejecutar; desde el Generador (y la Home / la Búsqueda con IA) comprobar que llegan recetas reales
   que pasan el Validador.
2. Con la clave vaciada o ausente: la app entra en **modo semilla** sin fallar (cambiar
   `local.properties` exige reconstruir).
