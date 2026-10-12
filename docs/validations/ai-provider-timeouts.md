# Validación independiente — `ai-provider-timeouts`

- Fecha: 2026-10-12
- Feature: `ai-provider-timeouts` (correctiva de `real-ai-provider`, issue #68) · `area: integration`
- Spec: `docs/specs/ai-provider-timeouts.md`
- Rol: `validator` (agente con contexto limpio, skill `feature-validator`)
- Rama / worktree: `feature/ai-provider-timeouts`
- Veredicto: **`accept`**

## Alcance validado

- `AiProviderPolicy`: `SOCKET_TIMEOUT_MILLIS` 30 s → **90 s**, `REQUEST_TIMEOUT_MILLIS` 40 s → **120 s**,
  `TOTAL_DEADLINE_MILLIS` 60 s → **180 s**; `CONNECT_TIMEOUT_MILLIS` 10 s, `MAX_ATTEMPTS = 2`, backoff
  500 ms y `Retry-After ≤ 5 s` **sin cambios**.
- `AiProviderPolicy.transportFailureReason(cause)` pura: `HttpRequestTimeoutException` /
  `java.net.SocketTimeoutException` / `InterruptedIOException` → `TIMEOUT`; `IOException` genérico
  (incluye el `ConnectTimeoutException` de Ktor, que es `java.net.ConnectException`) → `NETWORK`; resto,
  incluido `null` → `UNEXPECTED`. Los dos `catch` de `sendAttempt` (petición y `bodyAsText()`) pasan por
  ella; `CancellationException` se relanza primero.
- KDoc de `AiUnavailableReason.TIMEOUT`/`NETWORK` actualizado.

## Verificación re-ejecutada por el validador

| Comprobación | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | exit 0; `test` y `lint` vía `scripts/gradle.sh` (primera pasada `UP-TO-DATE`) |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | BUILD SUCCESSFUL, 34/34 tareas |
| Lint forzado | `bash scripts/gradle.sh lint --rerun-tasks` | BUILD SUCCESSFUL, 30/30 tareas |
| XML (parseo propio) | `app/build/test-results/testDebugUnitTest/*.xml` | **57 suites / 376 tests / 0 fallos / 0 errores / 0 saltados** (371 previos + 5 nuevos) |
| `data.ai` | mismo XML | 33 tests: `AiProviderPolicyTest` 10, `AiRecipePayloadTest` 5, `DevExpertRecipeGeneratorTest` 18 |
| Lint | `app/build/reports/lint-results-debug.xml` | **0 errores, 13 warnings** preexistentes; 0 bajo `app/src` |

## Comprobaciones adicionales

- Jerarquía real de excepciones vía `javap` sobre los jars de `~/.gradle/caches`:
  `HttpRequestTimeoutException extends java.io.IOException`; `ConnectTimeoutException extends
  java.net.ConnectException`; el engine OkHttp mapea `SocketTimeoutException` al typealias de Ktor.
- Sin `withTimeout` ni otro `*_MILLIS` que acote al proveedor en `app/src/main`.
- Escenarios 1-5 de la spec cubiertos (holgura con `REAL_LATENCY_MILLIS = 28_000L`, frontera del recorte,
  tabla de clasificación positiva/negativa/null, socket timeout → `TIMEOUT` y ya no `NETWORK`,
  socket+200 → `RETRY`/`detail = TIMEOUT` con backoff fijo, regresión de los 28 escenarios aceptados).
- Alcance: `git diff main --name-only` solo con los ficheros previstos; `AiHttpClientFactory.kt`,
  `domain/**`, `ui/**`, `data/generator/**`, Room, assets, build/gradle, manifiesto, `init.sh`,
  `scripts/**`, `DESIGN.md` y `docs/design/**` intactos; sin dependencias nuevas.
- Seguridad: sin clave/token en el diff; `local.properties` ignorado; tests con la clave ficticia
  `test-key`.

## Hallazgos

Sin hallazgos Critical/High/Medium. 4 Low/informativos no bloqueantes:

1. La spec (`docs/specs/ai-provider-timeouts.md:81`) dice que "las tres primeras aserciones fallan" con los
   valores viejos; en realidad fallan 1, 3 y 4 (la 2, `REQUEST ≥ SOCKET`, se cumple con 40 ≥ 30). La
   prueba de regresión sigue siendo válida porque la aserción 1 falla. Nit de redacción.
2. `un_intento_lento_dentro_de_la_politica_no_degrada` usa `MockEngine` + reloj inyectado, así que no
   ejercita el plugin `HttpTimeout` contra tiempo real; el discriminador real viejo-vs-nuevo es
   `la_politica_admite_con_holgura_la_latencia_real_medida` (y los tests de socket timeout).
3. `docs/specs/real-ai-provider.md` conserva 30/40/60 s en sus secciones históricas, de forma
   intencionada (la spec no se reescribe; se anota su Unknown).
4. `docs/specs/ai-provider-timeouts.md` estaba sin commitear; el commit de aceptación lo incluye.

## Pendiente (persona, no automatizable desde WSL)

Prueba manual en el emulador de Android Studio (Windows): con `devexpert.apiKey` válida, el
Generador/Home/Búsqueda devuelven recetas reales (~28 s) sin degradar a modo semilla y logcat con
`detail = TIMEOUT` (nunca `NETWORK`) si aún se agota; con la clave ausente, la app sigue en modo semilla
sin fallar.
