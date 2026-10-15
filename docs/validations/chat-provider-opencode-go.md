# Validación independiente — `chat-provider-opencode-go`

- **Feature**: `chat-provider-opencode-go` (`area: integration`, correctiva de `real-ai-provider`, issue #141)
- **Spec**: `docs/specs/chat-provider-opencode-go.md`
- **Rama / worktree**: `feature/chat-provider-opencode-go`,
  `/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/chat-provider-opencode-go`
- **Sesión**: 127 (agente validador con contexto limpio, independiente del implementer)
- **Estado previo**: `passing` (sesión 126, trabajo en disco sin commit; `HEAD = 5b2de53`)
- **Veredicto**: **`accept`**

## Qué se re-ejecutó (sin fiarse del handoff)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | exit 0 (`./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`) |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | exit 0, `BUILD SUCCESSFUL`, 34/34 tareas |
| Parseo propio de XML | `app/build/test-results/testDebugUnitTest/TEST-*.xml` | **98 suites / 739 tests / 0 fallos / 0 errores / 0 saltados** |
| Baseline re-derivada | `git grep -c "@Test" HEAD -- app/src/test` | 98 ficheros / 738 anotaciones en `HEAD` (5b2de53) ⇒ **delta +1 test / 0 suites** (`AiProviderPolicyTest` 12→13) |
| Lint | SARIF `app/build/reports/lint-results-debug.sarif` | **0 errores, 13 warnings** preexistentes, **0 bajo `app/src`** |
| Alcance | `git diff --name-only` | exactamente los ficheros previstos por la spec + la spec sin trackear |
| Claves | `git check-ignore local.properties` / diff | `local.properties` ignorado y ausente; solo nombres de propiedad/campo; tests con `test-key` |

Nota de entorno (no defecto de código ni del harness): el primer `init.sh` del validador chocó con un
`lint --rerun-tasks` residual de un intento previo que compilaba el **mismo** worktree en DrvFs
(`NoSuchFileException` en `app/build/intermediates`). Tras esperar a que terminara, `init.sh` pasó
limpio. No se usó `./gradlew --stop`.

## Escenarios verificados (en código y en tests, no solo por grep)

1. **Chat → OpenCode Go** — `AiProviderPolicy.BASE_URL = "https://opencode.ai/zen/go/v1"`,
   `endpointUrl() = .../chat/completions`, `MODEL_ID = "deepseek-v4.1-flash"`; `sendAttempt` añade
   `x-opencode-session` (`SESSION_ID`) y `User-Agent` **por petición**. El camino feliz asevera URL,
   `POST`, `Bearer`, cabecera de sesión, `User-Agent` y modelo. ✓
2. **`MISSING_KEY` degrada sin red** — el test asevera `MISSING_KEY`, **0 peticiones HTTP** y que el
   mensaje cita `opencode.apiKey` y **no** `devexpert`. ✓
3. **Imagen en DevExpert y desacoplada** — `DishImagePolicy` gana su `BASE_URL` propia;
   `endpointUrl()`/`editsUrl()` la usan; aserción negativa
   `assertNotEquals(AiProviderPolicy.BASE_URL, DishImagePolicy.BASE_URL)`; las peticiones de imagen
   (generación y ajuste) aseveran `assertNull` de la cabecera de sesión. ✓
4. **Dos claves con consumidores separados** — `build.gradle.kts` lee `opencode.apiKey` →
   `OPENCODE_API_KEY` y conserva `DEVEXPERT_API_KEY`; `MainActivity` usa `OPENCODE_API_KEY` en el chat y
   `DEVEXPERT_API_KEY` en la imagen; `""` + degradación en silencio si falta la propiedad. ✓
5. **Comportamiento aceptado intacto** — el diff no-comentario de `AiProviderPolicy` es solo `BASE_URL`,
   `MODEL_ALIAS`→`MODEL_ID` y tres constantes nuevas; timeouts (10/120/150/240 s), `MAX_ATTEMPTS = 2`,
   backoff, `Retry-After`, `RESPONSE_FORMAT_ENABLED`, fallback, diagnóstico y modo semilla sin cambios y
   verdes. ✓

## Hallazgos

**Critical / High / Medium: ninguno.**

- **L1 (Low, informativo, no bloqueante)** — en `DevExpertDishImageGeneratorTest` la aserción negativa
  del `User-Agent` solo falla si coincide con el del chat; la garantía fuerte es el `assertNull` de
  `x-opencode-session`, presente en ambas rutas. Cosmético.

**Residual (documentado, no bloqueante)**: verificación manual en emulador pendiente de la persona
(chat HTTP 200 en logcat `SazonAi`; modo semilla con la clave vaciada; imagen sigue generándose en
DevExpert) y las incógnitas ya anotadas en la spec (valor exacto de `x-opencode-session` —verificado solo
su presencia— y aceptación de `response_format: json_object` por Go, cubierta por el fallback ya
aceptado).

## Seguridad

Sin hallazgos relevantes. Ninguna clave en el diff ni en ningún fichero versionado (solo nombres de
propiedad/constante); `local.properties` sigue ignorado; `SESSION_ID` es una constante no sensible y se
asevera distinta de (y sin contener) la clave; las cabeceras del proveedor viajan **por petición**, así
que no pueden filtrarse al cliente de imagen compartido; las URLs de salida son las dos constantes de
confianza; reintentos/timeouts acotados. Sin cambios de dependencias ni de tooling.

## Documentación / harness

Los 5 documentos durables actualizados de forma coherente con el código (`technical-discovery.md`,
`domain-model.md`, `risks-and-open-questions.md`, ADR `0003`, ADR `0005`); `AGENTS.md`/`CONTEXT.md`/
`DESIGN.md`/Taxonomía intactos según la spec. `init.sh` ejecuta el gate real y sale no-cero al fallar.
`feature_list.json` queda `accepted` con la evidencia y `PROGRESS.md` actualizado. Harness coherente y
reiniciable.

## Seguimiento requerido

Ninguno bloqueante. El orquestador persiste `chat-provider-opencode-go` → **`accepted`** y crea el
commit local (la publicación es de `github-flow`). Único pendiente: la verificación manual en emulador
de la persona, ya registrada en la evidencia.
