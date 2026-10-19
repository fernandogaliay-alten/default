# Validación independiente — `ai-keys-onboarding-guard`

- **Feature**: `ai-keys-onboarding-guard` (`area: platform`; `depends_on: []`).
- **Spec**: `docs/specs/ai-keys-onboarding-guard.md`
- **Rama / worktree**: `feature/ai-keys-onboarding-guard`,
  `/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/ai-keys-onboarding-guard` (WSL, nativo).
- **Sesión**: validación independiente (subagente `validator`, contexto limpio, sin fiarse del handoff del
  implementer) en la **sesión 148**; el número lo fija el orquestador al persistir el veredicto.
- **Estado previo**: `passing` (implementer, sesión 147; trabajo en disco; `HEAD f5e28fe`, **sin commit,
  sin push, sin PR**).
- **Veredicto**: **`accept`**

## Alcance validado

Correctiva de configuración del harness (sin cambios en la app):

- `local.properties.example` — **nuevo**, rastreado y sin secretos: `sdk.dir=`, `opencode.apiKey=` y
  `devexpert.apiKey=` con valores **vacíos** y comentarios (qué hace cada clave, que `local.properties` no
  se versiona y que las claves se pasan aparte).
- `scripts/new-worktree.sh` — helper `sazon_props_value` (grep anclado, última aparición, tolerante a CRLF,
  recorta espacios, `|| true` bajo `set -euo pipefail`, **nunca** imprime valores) y **dos avisos
  independientes por stderr** dentro de la rama con `local.properties`, tras `local.properties copiado a ...`:
  chat (`opencode.apiKey` → `MISSING_KEY`, modo semilla) e imagen (`devexpert.apiKey` → ficha tipográfica).
  Una propiedad presente pero vacía cuenta como ausente. Cabecera actualizada.
- `AGENTS.md` y `docs/technical-discovery.md` — paso al clonar, dónde viven las claves y qué degrada.

## Verificación re-ejecutada por el validador (números reales)

| Comprobación | Comando | Resultado |
|---|---|---|
| Sintaxis | `bash -n scripts/new-worktree.sh` | **exit 0** |
| Gate estándar | `bash init.sh` | **exit 0**; `:app:testDebugUnitTest` `UP-TO-DATE`, `:app:lint` ejecutado (30/30) ⇒ no vale como fresca: forzado |
| Tests **forzados** | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | **exit 0**, `BUILD SUCCESSFUL` 4m11s, **34/34 tareas ejecutadas**; XML frescos **117 suites / 967 tests / 0 fallos / 0 errores / 0 saltados** |
| Lint **forzado** | `bash scripts/gradle.sh lint --rerun-tasks` | **exit 0**, `BUILD SUCCESSFUL` 3m15s, **30/30 tareas**; SARIF **13 hallazgos, todos warning / 0 errores / 0 bajo `app/src`** (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1) = baseline de `HEAD` |
| Arnés propio (spec §2 + bordes) | repo git desechable en `/tmp`, `SAZON_WSL_LOCAL_PROPS=0`, `base-sha` como 2º argumento | **34/34 checks PASS** |
| **Discriminación** | mismo arnés contra `git show HEAD:scripts/new-worktree.sh` | el aserto exigido (`opencode.apiKey`/`MISSING_KEY`) **falla contra `HEAD`** ⇒ el arnés no pasa en vacío |
| Arnés de la spec **tal cual** | `bash` del snippet de §2 | exit **1**, y con `bash -x` se confirma que **solo** falla la última aserción `[ -f .../demo-con_claves/local.properties ]`; todas las anteriores pasan |
| Arnés con `con_claves` al final | mismo snippet, casos reordenados | `ARNES OK: 4 casos, avisos de clave donde toca`, **exit 0** |
| Contrato stdout/stderr/rc | diff contra `HEAD` normalizado, en positivo/negativo/sin fichero | stdout **byte a byte idéntico**; stderr idéntico salvo **exactamente** las 2 líneas `AVISO` nuevas del negativo; exit 0 conservado, sin argumentos → **2**, sha inválido → **128** igual que `HEAD` |
| Ramas previas | sin `local.properties` ni `ANDROID_HOME`; rama `ANDROID_HOME`; worktree existente | aviso previo intacto; rama `ANDROID_HOME` sin avisos de clave; `worktree ya existe` exit 0 (con y sin `local.properties`) |
| Bordes | presente-vacía, solo-espacios, línea comentada `#opencode.apiKey=`, última aparición gana, CRLF | comportamiento spec-correcto |
| Fugas | búsqueda en todos los `.out`/`.err` | `dummy-chat`/`dummy-img`/`crlf-chat`/`primero`/`segundo` **ausentes** |
| Plantilla | `git ls-files --error-unmatch`, `git check-ignore -v --no-index`, `grep` de valores | rastreada (exit 0, modo `100644`); **no** ignorada (exit 1); 3 propiedades vacías / 0 con valor / 0 patrones `oc_sk_`/`sk-*`; contenido idéntico al bloque de la spec |
| Alcance | `git diff --name-only HEAD`, `git status` | solo `local.properties.example`, `scripts/new-worktree.sh`, `AGENTS.md`, `docs/technical-discovery.md`, `feature_list.json`, `PROGRESS.md` y la spec; **sin** cambios en `app/**`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, manifiesto, permisos, `init.sh`, `.gitignore`, el resto de `scripts/**` ni `local.properties` (mtime 14:25, anterior a las ediciones 14:27; sin `sdk.dir` de Windows) |
| Docs durables | inspección | `AGENTS.md` (paso 2 de *Flujo de arranque*, viñeta de `local.properties`, viñeta de worktrees) y `docs/technical-discovery.md` (fila `| Claves de IA |` + *Deployment and Operations*) coherentes con el comportamiento observado |

## Escenarios de la spec verificados

1. **Scenario 1** — la plantilla existe, está rastreada, no está ignorada y no contiene ningún valor de
   clave (solo nombres y valores vacíos con comentarios).
2. **Scenario 2** — `local.properties` sin `opencode.apiKey` ⇒ exit 0, worktree creado, stdout sin cambios y
   aviso por stderr que nombra `opencode.apiKey` y `MISSING_KEY`.
3. **Scenario 3** — con las dos claves ⇒ exit 0 y **ningún** aviso de clave; `local.properties` se copia.
4. **Scenario 4 (borde)** — `opencode.apiKey=` presente y vacía ⇒ se trata como ausente y se emite el aviso.
5. **Scenario 5 (borde)** — solo falta `devexpert.apiKey` ⇒ sin aviso de chat y con aviso de imagen.
6. **Scenario 6** — sin `local.properties` ni `ANDROID_HOME`, rama `ANDROID_HOME`, worktree existente,
   stdout y códigos 0/2: sin regresión.
7. **Scenario 7** — `AGENTS.md` y `docs/technical-discovery.md` dicen dónde viven las claves y qué degrada.

## Hallazgos

- **Bloqueantes / mayores**: ninguno.
- **Informativos, sin reparación**:
  - **F1** (spec): el snippet del arnés de `docs/specs/ai-keys-onboarding-guard.md:350-387` no es copiable
    tal cual porque la última aserción `[ -f .../demo-con_claves/local.properties ]` se evalúa después de
    que el último `run_case` haga `rm -rf "$scratch/worktrees"`. Es un artefacto de **orden** del propio
    snippet: no debilita ninguna aserción, el arnés **sí discrimina** (contra `HEAD` falla en el aserto
    exigido) y la copia del fichero se comprobó aparte (`COPY_OK=1`). Ya documentado por el implementer en
    la nota de la spec; mejora opcional: mover `con_claves` al final.
  - **F2** (proceso): la spec estaba sin trackear; el orquestador la incluye en el commit de aceptación.
  - **F3** (estilo aceptado): la clave del `grep` va sin escapar (`opencode.apiKey`; el `.` casa cualquier
    carácter en ERE) y `sazon_props_value` imprimiría el valor si se invocara fuera de `$(...)`. Inocuo:
    claves fijas del script, nunca se invoca fuera de la sustitución de comando y no hay fuga.
  - **F4** (alcance acordado): `sdk.dir` se documenta pero no se valida (su ausencia ya la cubre el aviso de
    `ANDROID_HOME`/Gradle).

## Seguridad

Sin hallazgos relevantes ni bloqueantes: la plantilla versionada está **vacía** (0 patrones de clave real en
plantilla, diff, spec, `PROGRESS.md` y `feature_list.json`); `local.properties` sigue ignorado
(`.gitignore:5`) y fuera del commit; los avisos nombran **solo la propiedad** ausente y nunca su valor
(verificado que ninguna cadena de prueba aparece en stdout/stderr); no hay `cat` del fichero, ni `eval`, ni
interpolación de datos no confiables (el único input es la clave fija del script); sin dependencias nuevas,
permisos, red ni cambios en `app/**`; el aviso es informativo y no altera los códigos de salida, así que no
puede romper el flujo autónomo de `github-flow`/`feature-flow`.

## Persistent Durable Documentation

`AGENTS.md` (política repo-wide correcta) y `docs/technical-discovery.md` (límite durable de *Data and
Storage* / *Deployment and Operations*) actualizados y sin contradicciones; no se crearon docs
innecesarias. `init.sh` **sin tocar** (sigue ejecutando el gate real) y no hay regresión del arnés. E2E no
aplicable: el repo no tiene arnés E2E persistente, esta feature no cambia flujos de usuario ni rutas de la
app, y el equivalente reproducible es el arnés bash sobre el script real.

## Persistencia

`feature_list.json` pasa a **`accepted`** con la evidencia de esta validación independiente y el commit
local es `feat: complete ai-keys-onboarding-guard` en la rama `feature/ai-keys-onboarding-guard` (**sin**
push ni PR; publica `github-flow`).
