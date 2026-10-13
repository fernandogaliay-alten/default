# Validación independiente — `dish-image-generator`

- **Feature**: `dish-image-generator` (`area: integration`, fase 2 de `docs/ai-dish-images-proposal.md`)
- **Spec**: `docs/specs/dish-image-generator.md`
- **Rama / worktree**: `feature/dish-image-generator`, `/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/dish-image-generator`
- **Sesión**: 090 (agente validador con contexto limpio, independiente del implementer)
- **Estado previo**: `passing` (sesión 089, trabajo en disco; `HEAD = 245620d` = `origin/main`, sin upstream)
- **Veredicto**: **`accept`**

## Qué se re-ejecutó (sin fiarse del handoff)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0**, pero todo `UP-TO-DATE` (config cache reutilizada) → insuficiente por sí solo |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | exit 0, `BUILD SUCCESSFUL` en 3m17s, **34/34 tareas ejecutadas** |
| Tirada focal | `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.image.*" --rerun-tasks` | exit 0, `BUILD SUCCESSFUL` en 2m43s, **4 suites / 22 tests / 0 fallos / 0 errores / 0 saltados** |
| Parseo propio de XML | `app/build/test-results/testDebugUnitTest/TEST-*.xml` | **72 suites / 517 tests / 0 fallos / 0 errores / 0 saltados**; nuevas: `DishImagePolicyTest` 7, `DishImagePayloadTest` 3, `DevExpertDishImageGeneratorTest` 7, `AndroidDishImageReducerTest` 5 |
| Baseline re-derivada | `git ls-tree` + `git grep -c '@Test' HEAD -- app/src/test` | `HEAD`: **68 suites / 495 anotaciones**; árbol: 72 suites / 517 anotaciones ⇒ delta **+4 suites / +22 tests**, cuadra exacto con el run |
| Lint | `bash scripts/gradle.sh lint --rerun-tasks` + parseo propio del SARIF | exit 0, **0 errores, 13 warnings** preexistentes (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), **0 bajo `app/src`** |
| Pureza | grep de imports en `data/ai/image` | `import android` **solo** en `AndroidDishImageReducer.kt` (`Bitmap`, `BitmapFactory` + `androidx.core.graphics.scale`); `Recipe\|Taxonomy\|DishImagePrompt\|DishImageCacheKey` → **0**; `Log.` → **0** |
| Sin consumidores | grep `DevExpertDishImageGenerator\|DishImageReducer\|DishImageGenerator\|DishImageRequest` en `app/src` | **0** referencias fuera de `domain.image` y de `data.ai.image` (el puerto sigue sin cablear; sin tocar `MainActivity`/`BuildConfig`) |
| Sin secretos | grep de claves/`BuildConfig`/`local.properties` en los ficheros nuevos | solo `apiKey = "test-key"` en tests; sin `Log`, sin clave en cuerpo (aserción del test) ni en el árbol |
| Alcance | `git status --porcelain -uall` | solo los 6 fuentes nuevos, los 4 tests, `docs/specs/dish-image-generator.md` (del planner), los 3 docs actualizados, `feature_list.json` y `PROGRESS.md`; `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `domain/**`, `ui/**`, assets, `DESIGN.md` y `docs/design/**` **sin diff**; `docs/design/concepts/dish-image-generator` **no existe** (el gate de capturas no aplica) |

Nota de entorno (no afecta al veredicto): el primer `lint --rerun-tasks` murió con un **SIGSEGV del daemon de
Gradle** (Temurin 21, `NodeHash::hash_find_insert`; dejó `hs_err_pid1223.log` en el worktree, retirado por el
validador). El reintento terminó `BUILD SUCCESSFUL` y el informe SARIF es el parcheado arriba.

## Escenarios verificados (contra el código y los tests reales)

1. **Contrato exacto**: el test de camino feliz comprueba URL `https://inference.devexpert.io/v1/images/generations`,
   método POST, `Authorization: Bearer test-key`, `Content-Type` JSON en el `OutgoingContent` y cuerpo con
   **exactamente** `model`/`prompt`/`size` (`image`, el prompt de la petición, `1024x1024`); **1** petición.
2. **`b64_json` → WebP 960**: Robolectric `@GraphicsMode(NATIVE)` + `@Config(sdk=[33])` decodifica de verdad
   (`BitmapFactory`) y comprime `WEBP_LOSSY`; 1024×1024 → WebP (`RIFF`…`WEBP`) de 960×960; `WEBP_QUALITY = 80`
   se fija por constante (la calidad no es verificable de forma fiable; ya declarado como incógnita en la spec).
3. **Nunca agranda**: `scaledSize` pura (1024→960, 960→960, 400×300→400×300, mínimo 1 px) y el códec real
   re-codifica a WebP conservando 400×300.
4. **Error HTTP**: 400/401/429/500 → `null` sin lanzar y **1 sola** petición (sin reintento).
5. **Timeout/transporte**: `SocketTimeoutException`, `HttpRequestTimeoutException` e `IOException` → `null`, 1
   petición; además un timeout inyectado (50/50/100 ms) con respuesta de 1 s → `null` (si el `timeout { }` por
   petición se ignorara, el test fallaría por recibir bytes válidos: la costura se prueba de verdad).
6. **Sin clave**: `""` y `"   "` → `null` y **0** peticiones.
7. **`200` malformado**: cuerpo nulo/`""`/blank, no-JSON, `{}`, `{"data":[]}`, `{"data":[{}]}`,
   `b64_json` vacío/blank/base64 inválida, `data` string u objeto → `null` sin lanzar (y por inspección, sin
   llegar al reductor: `decode` devuelve `null` antes de `reducer.reduceToWebp`).
8. **Bytes no imagen**: `0x01..`, array vacío y texto → `null` con el reductor Android real.
9. **Cancelación**: petición en vuelo + `cancelAndJoin` → `CancellationException` **relanzada** (no se convierte
   en `null`); el test no pasaría en vacío (comprueba la excepción capturada, no solo el estado del job).
10. **Transporte puro**: greps independientes (sin dominio, `android.graphics` aislado, sin `Log`, sin
    `BuildConfig`) + test que comprueba que `cacheKey`/`recipeId` **no** viajan en el cuerpo y que el `prompt`
    viaja tal cual.

## Hallazgos

**Critical / High / Medium: ninguno.** La feature cumple la spec, incluidos los cuatro escenarios negativos
obligatorios, el contrato `null` (con cancelación propagada) y la disciplina de alcance.

- **L1 (Low, informativo)** — `DishImagePolicy.MAX_ATTEMPTS` es una constante **declarativa**: el adaptador no
  la referencia (no hay bucle de reintento; la petición única está en el camino lineal) y solo la usan el KDoc
  y `DishImagePolicyTest`. El comportamiento "un solo intento" se sostiene por construcción, no por la
  constante; conviene no leerla como una política ejecutable en las fases 3-4.
- **L2 (Low, endurecimiento para fases 3-4)** — `DishImagePayload` usa `Base64.getDecoder()` (decoder básico,
  estricto con `\n`/espacios) y el adaptador no registra nada. Si el proveedor cambiara el formato del payload
  (base64 envuelto en líneas, `url` en vez de `b64_json`, cambio de tamaño mínimo), **todas** las imágenes
  degradarían a `null` en silencio y sin traza. Es el contrato pedido por la spec (escenario 7) y la prueba real
  opcional #3 no se ejecutó (permitido por la spec); se anota como el punto a vigilar cuando la fase 4 consuma
  el puerto.
- **L3 (Low, teórico)** — `AndroidDishImageReducer` decodifica con `BitmapFactory.decodeByteArray` sin acotar
  tamaño (`inSampleSize`/`inJustDecodeBounds`) y captura `Exception`, no `OutOfMemoryError`: un payload anómalo
  del gateway podría romper el contrato "nunca lanza". No es explotable en esta rebanada (los bytes vienen por
  TLS del proveedor de confianza con la clave en la cabecera) y queda como nota de endurecimiento.
- **L4 (Low, informativo)** — La spec cifraba la baseline del gate en "68 suites / 496 tests"; el validador
  re-deriva **495** anotaciones `@Test` en `HEAD` (68 suites). El implementer ya reportó la desviación de 1 y el
  delta real (+4 suites / +22 tests = 72/517) cuadra exacto con el run; la spec permitía re-derivar el número.

## Seguridad (checklist de la feature)

Sin hallazgos ni notas bloqueantes:

- **Secretos**: no hay claves en el diff ni en el árbol; los tests usan `test-key`; la clave entra por
  constructor y **solo** viaja en la cabecera `Authorization` (no en el cuerpo, sin `Log` y sin telemetría, así
  que no puede filtrarse por un log).
- **Auth/side effects**: `apiKey` en blanco → `null` **antes** de cualquier petición (0 peticiones, verificado).
- **Entrada externa**: el payload del proveedor se parsea de forma tolerante y nunca lanza; no hay rutas,
  nombres de fichero ni persistencia en esta fase (sin traversal ni escritura en disco).
- **Servicios externos / SSRF**: la URL se construye desde la constante de confianza `AiProviderPolicy.BASE_URL`
  (nada viene del usuario); timeouts acotados (10/30/40 s) y un único intento, así que no hay amplificación de
  gasto de cuota.
- **Dependencias**: ninguna nueva (`app/build.gradle.kts` y `gradle/libs.versions.toml` intactos; se reutiliza
  `androidx.core.graphics.scale` de core-ktx ya presente en lugar de código propio, lo que eliminó un warning
  `UseKtx`).
- **Nota**: ver L3 (cota de decodificación) y L2 (ausencia de diagnóstico) como endurecimiento futuro.

## Documentación y harness

- `docs/adr/0005-ai-dish-images.md`: estado **"Aceptado parcialmente (fases 1-2)"**, alcance de la fase 2
  (transporte + códec, `null` como fallo, sin reintento, WebP 960 q80, clave solo en cabecera, sin consumidores)
  y fases 3-7 pendientes; coherente con el código.
- `docs/technical-discovery.md`: nuevo bullet del adaptador real `data.ai.image` (Ktor aislado en `data.ai`,
  política propia, un intento, reducción local, sin caché/UI/consumidores) y el bullet del puerto pasa a
  "fases 1-2"; coherente.
- `docs/risks-and-open-questions.md`: la fila de cuota/latencia pasa de "aún no se genera nada" a "el transporte
  ya existe desde la fase 2 y se materializa al consumirse en las fases 3-4", que es exactamente el estado real.
- `domain-model.md` / `docs/domain/taxonomy.md` / `AGENTS.md` / `DESIGN.md`: correctamente **no** tocados (no
  cambian entidades, resolución en runtime, flujo ni UI).
- `init.sh`: sigue ejecutando el gate real (`test` + `lint` vía `scripts/gradle.sh`), exit 0 en verde, sin
  procesos largos y sin modificaciones.
- `PROGRESS.md` (sesión 089) y `feature_list.json` (evidencia + `passing`) reflejan el estado real, y el
  implementer **no** hizo commit, push ni PR (`HEAD == origin/main == 245620d`, sin upstream; todo el trabajo
  está en disco).

## Conclusión

El adaptador real del puerto `DishImageGenerator` está implementado como transporte + códec puros, con los 10
escenarios cubiertos (incluidos los negativos obligatorios y la cancelación propagada), el contrato exacto de la
petición, la reducción WebP 960 verificada con el códec Android real en modo gráfico nativo, disciplina de
alcance intacta (sin caché, UI, consumidores, dependencias ni cambios en build/`init.sh`/dominio) y
documentación durable coherente. **Veredicto: `accept`**, sin trabajo de reparación requerido. La verificación
E2E no aplica (no hay suite persistente, nadie consume el puerto y la feature no cambia comportamiento
observable) y la prueba real contra el proveedor sigue siendo opcional y no ejecutada (consume ~2 % de cuota
semanal); se hará al consumir el puerto en las fases 3-4.

**Siguiente acción (orquestador)**: commit local de la rama `feature/dish-image-generator` (incluyendo la spec y
este informe) y publicación (push + PR con `Closes #<n>`, sin merge). El orquestador es quien persiste el
estado `accepted`.
