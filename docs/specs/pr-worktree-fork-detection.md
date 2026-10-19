# Feature Implementation Spec: `pr-worktree.sh` no confunde la organización dueña con un fork

## Source Feature

- `id`: `pr-worktree-fork-detection`
- `area`: `platform`
- `depends_on`: `[]`
- `status`: `not_started` (al planificar)
- `source`: `feature_list.json:1877-1891` (título, `user_visible_behavior`, las **tres** verificaciones y las
  `notes` con el alcance acordado). Correctiva detectada al mergear el **PR #167** desde el orquestador
  `pr-flow`: el helper abortó y el worktree se creó a mano con la misma lógica.

## Goal

`scripts/pr-worktree.sh <n>` debe clasificar un PR como fork **por el repositorio donde vive la rama head**,
no por el dueño de ese repositorio. Hoy compara `headRepositoryOwner.login` con el usuario autenticado de
`gh` (`scripts/pr-worktree.sh:58-62`); en un repositorio de organización el dueño
(`fernandogaliay-alten`) nunca coincide con el usuario (`FernandoGaliay`), así que **todo** PR legítimo de
este repositorio se rechaza como fork y `pr-flow` no puede seguir su camino estándar.

El arreglo es una sola condición: usar `isCrossRepository` de `gh pr view` (verdadero solo cuando el head
vive en otro repositorio) y eliminar la consulta a `gh api user`. Un PR de este repositorio, aunque su rama
viva bajo la organización dueña, prepara/reutiliza su worktree; un PR cuyo head pertenece a otro
repositorio sigue rechazándose con exit 1. No cambia la app ni el resto del harness.

## Non-Goals

- **No** se soportan PRs de fork: se siguen rechazando con exit 1 (solo cambia el criterio de detección).
- **No** se tocan `init.sh` ni otros helpers (`new-worktree.sh`, `sync-worktree.sh`, `cleanup-worktree.sh`,
  `gradle.sh`, `wsl-env.sh`), ni la app, `app/build.gradle.kts`, `gradle/libs.versions.toml`, el manifiesto,
  los permisos o `local.properties`.
- **No** se añade un test de scripts versionado ni CI: el repo no tiene arnés de tests para `scripts/` y la
  verificación se hace con un stub efímero de `gh` en `/tmp` (evidencia reproducible, sin ampliar el
  repositorio).
- **No** cambia el contrato de salida: stdout sigue siendo **solo** la ruta del worktree (última línea),
  los diagnósticos van a stderr y se conservan los códigos 0 (listo), 1 (no se puede) y 2 (uso).
- **No** cambia el comportamiento posterior al filtro: `fetch`, creación/reutilización del worktree,
  `local.properties`, resolución de rutas y mensajes `state`/`draft`.
- **No** se valida el estado del PR (`OPEN`/`MERGED`), el draft, ni la rama `feature/<id>`; eso ya era así y
  queda fuera.

## Job Story

When ejecuto `pr-flow` sobre un PR de este repositorio cuya rama head vive bajo la organización dueña,
I want que `scripts/pr-worktree.sh <n>` prepare el worktree sin clasificarlo como fork,
so I can revisar, reparar y mergear el PR por el camino estándar, sin crear worktrees a mano.

## Users And Permissions

- **Orquestador `pr-flow`** (agente): llama al helper con el número de PR y la rama base; necesita exit 0 y
  la ruta por stdout. Usa `gh` ya autenticado (precondición de `pr-flow`).
- **Persona mantenedora**: puede invocar el helper a mano con el mismo contrato.
- Sin permisos nuevos ni cambios de autenticación. Un token con acceso al repositorio basta: el arreglo
  **retira** la llamada a `gh api user`, que no era necesaria.
- Los PRs de fork siguen fuera del flujo: se rechazan antes de crear nada.

## Acceptance Scenarios

### Scenario 1: PR de este repositorio en una organización (positiva, reproducibilidad determinista)

Given un stub de `gh` que responde a `gh pr view` con `headRefName=feature/demo`,
`isCrossRepository=false` y `headRepository.nameWithOwner=fernandogaliay-alten/default`, mientras el
usuario autenticado sería `FernandoGaliay` (dueño distinto),
When se ejecuta `scripts/pr-worktree.sh 42 main`,
Then **no** aborta con «viene de un fork», termina con exit 0 e imprime la ruta del worktree creado; y el
stub registra **solo** llamadas `pr view` (ninguna `gh api user`).

### Scenario 2: PR real de este repositorio (positiva, verificación exigida)

Given un PR de este repositorio con `isCrossRepository=false` (hoy el **#167**:
`headRepository.nameWithOwner=fernandogaliay-alten/default`,
`headRefName=feature/cooking-timer-notification-step-label`),
When se ejecuta `WORKTREES_ROOT=<tmp> bash scripts/pr-worktree.sh 167 main`,
Then termina con exit 0 e imprime `<tmp>/cooking-timer-notification-step-label` (worktree creado o
reutilizado), sin mensaje de fork.

### Scenario 3: fork real en otro repositorio (negativa, verificación exigida)

Given un stub de `gh` con `isCrossRepository=true` y `headRepository.nameWithOwner` de **otro** repositorio,
When se ejecuta el helper con ese PR,
Then termina con exit 1, escribe en stderr un mensaje que contiene «viene de un fork» y **no** crea el
directorio del worktree ni la rama.

### Scenario 4: fork bajo el mismo dueño que el usuario (negativa que el código viejo dejaba pasar)

Given un stub de `gh` con `isCrossRepository=true` y `headRepository.nameWithOwner=FernandoGaliay/default`
(es decir, `headRepositoryOwner.login` **sí** coincide con el usuario autenticado),
When se ejecuta el helper,
Then termina con exit 1 con el mensaje de fork: la decisión no depende del dueño sino del repositorio.

### Scenario 5: no hay regresión en el resto del contrato

Given un worktree ya existente para la rama head, o una invocación por `feature-id` (sin `gh`),
When se ejecuta el helper,
Then reutiliza el worktree e imprime su ruta con exit 0 (o resuelve la rama como antes), y se conservan los
códigos 2 (sin argumentos) y 1 (rama sin prefijo / sin ref), el mensaje
`PR #<n>: head=<rama> state=<estado> draft=<bool>` y la copia de `local.properties`.

## Repository Research

### Files Inspected

- `scripts/pr-worktree.sh` (106 líneas) — objeto del cambio. Consulta
  `headRefName,headRepositoryOwner,state,isDraft` (líneas 46-48), compara `owner != me` con `gh api user`
  (58-62) y después resuelve el worktree (71-106).
- `feature_list.json:1877-1891` — alcance, verificaciones y notas; confirma que el criterio correcto es
  «si la rama head vive en el mismo repositorio» y que #167 tiene `isCrossRepository=false`.
- `.opencode/skills/pr-flow/SKILL.md:60` — regla duplicada y **incorrecta**: «If the PR head is a fork
  branch (different `headRepositoryOwner`)». Debe alinearse o el orquestador seguirá saltándose PRs
  legítimos aunque el script esté arreglado.
- `AGENTS.md:128-129` — dice «el head debe ser una rama `feature/<id>` de este repositorio»; correcto en
  intención, conviene fijar que fork = otro repositorio, no otro dueño.
- `PROGRESS.md:6416-6430` (Sesión 131, alta de `pr-flow`) — el helper se validó solo con `bash -n`; no hay
  tests de scripts en el repo.
- `scripts/wsl-env.sh:60-103` — el script sourceado añade `$HOME/.local/bin` al PATH si falta; un stub de
  `gh` debe quedar **por delante** de ese directorio (riesgo de verificación, ver abajo).
- `gh` instalado (`~/.local/bin/gh`, autenticado como `FernandoGaliay`): se comprobó que
  `gh pr view --json` **no** admite `baseRepository`, sí `isCrossRepository`, `headRepository` y
  `headRepositoryOwner`; para #167 devuelve `isCrossRepository=false` y
  `headRepository.nameWithOwner=fernandogaliay-alten/default`.

### Existing Patterns To Follow

- Bash estricto (`set -euo pipefail`), mensajes de diagnóstico a stderr, stdout reservado a la ruta.
- Lectura de `gh` en una sola llamada `gh pr view ... --json ... --jq ... | @tsv` y `IFS=$'\t' read`.
- Códigos de salida documentados en la cabecera del script (0/1/2) y comentarios en español sin tildes.
- Los scripts no tienen tests; la comprobación estática del repo es `bash -n` y el gate global `bash init.sh`.

### Current Gaps

- No hay arnés versionado de tests para scripts; la reproducibilidad se resuelve con el stub descrito en
  `Verification Plan`.
- `gh pr view --json` no expone `baseRepository`, así que no se puede comparar head/base en una sola
  llamada; `isCrossRepository` es el dato equivalente y ya está disponible.
- No hay PRs abiertos ahora mismo (`gh pr list --state open` vacío); el caso real se ejerce con #167
  (merged), que el script acepta porque solo resuelve la rama head.

## Technical Approach

1. **Una sola llamada a `gh`.** Cambiar el `--json`/`--jq` a:
   `headRefName,state,isDraft,isCrossRepository,headRepository` con
   `[.headRefName, .state, (.isDraft|tostring), (.isCrossRepository|tostring), (.headRepository.nameWithOwner // "-")] | @tsv`.
   Orden deliberado: los cuatro campos que gobiernan el flujo van primero y el dato diagnóstico
   (`headRepository`) al final; así un `headRepository` nulo (fork borrado) **no** desplaza columnas al
   hacer `read` con IFS tab (bash colapsa campos vacíos intermedios).
2. **Parseo:** `IFS=$'\t' read -r branch state draft cross head_repo <<< "$info"`. Mantener la guarda actual
   de `branch` vacía → exit 1.
3. **Condición de fork:** rechazar **solo** si `[ "$cross" = "true" ]`. Mensaje:
   `PR #${target} viene de un fork (${head_repo}); pr-flow no lo soporta todavia` (stderr) y exit 1. Un
   valor vacío o ausente **no** rechaza (comportamiento permisivo actual); un fallo real de `gh` ya cae en
   «no se encontro el PR».
4. **Eliminar** la llamada `me="$(gh api user ...)"` y la comparación con `headRepositoryOwner`; se puede
   quitar también `headRepositoryOwner` del `--json` porque ya no se usa.
5. **Cabecera:** actualizar los comentarios de las líneas 23 y 41 para decir que el rechazo es por head en
   **otro repositorio**, no por dueño distinto.
6. **Nada más cambia:** resolución de `repo_root`, `worktrees_root`, `dir_name`, reutilización, `fetch`,
   `worktree add`, copia de `local.properties`, stdout/stderr y exit codes.
7. **Documentación durable:** corregir la regla de fork en `.opencode/skills/pr-flow/SKILL.md:60` y fijar la
   definición en `AGENTS.md:128-129`.

No se añade ninguna heurística alternativa (comparar dueños, parsear la URL de `origin`, segunda llamada a
`gh repo view`): `isCrossRepository` es exactamente «el head vive en otro repositorio» y evita más red y
más modos de fallo.

## Expected File Changes

- `scripts/pr-worktree.sh` — modificar; sustituir la detección por `isCrossRepository`, eliminar `gh api
  user`, ajustar cabecera. Único cambio de código.
- `.opencode/skills/pr-flow/SKILL.md` — modificar **solo** la viñeta de fork (línea 60): «head en otro
  repositorio (`isCrossRepository=true`)», no «different `headRepositoryOwner`».
- `AGENTS.md` — modificar **solo** las líneas 128-129 para fijar que un fork es un head en otro repositorio.
- `docs/specs/pr-worktree-fork-detection.md` — crear (este documento).
- `PROGRESS.md` — actualizar por el implementer (sesión, verificación y evidencia).
- `feature_list.json` — actualizar por el implementer: `status: passing` y `evidence` con los resultados.

No se tocan: `init.sh`, el resto de `scripts/`, `app/**`, `build.gradle.kts`, `gradle/libs.versions.toml`,
manifiesto, permisos, `docs/technical-discovery.md`, `docs/domain-model.md`, `docs/risks-and-open-questions.md`
ni `DESIGN.md`.

## Visual Design Impact

- UI involved: no.
- Design source: not applicable (`DESIGN.md` no aplica).
- Screens or states affected: ninguno.
- New design artifact required: no.

## Durable Documentation Impact

- `AGENTS.md`: **update** — la sección de `pr-flow` debe definir fork por repositorio, no por dueño, para
  que futuras sesiones no reintroduzcan la comparación con el usuario.
- `.opencode/skills/pr-flow/SKILL.md`: **update** — contiene la regla incorrecta que causó el
  comportamiento; sin este cambio el orquestador seguiría descartando PRs legítimos.
- `docs/technical-discovery.md`: **not needed** — no cambia el stack, almacenamiento, integraciones ni el
  camino de verificación de la app; el helper es harness.
- `docs/domain-model.md`: **not needed** — sin entidades, relaciones ni estados de dominio.
- `docs/risks-and-open-questions.md`: **not needed** — es un defecto corregido, no una incógnita abierta.
- `DESIGN.md` / `docs/design/**`: **not needed** — sin UI.

## Implementation Plan

1. Leer el script completo y localizar el bloque `if [[ "$target" =~ ^[0-9]+$ ]]`.
2. Cambiar el `--json`/`--jq`, el `read` y la condición de fork según `Technical Approach`; borrar
   `gh api user` y su variable.
3. Actualizar los comentarios de cabecera del script.
4. Corregir la viñeta de fork en `.opencode/skills/pr-flow/SKILL.md` y la nota de `AGENTS.md`.
5. Ejecutar `bash -n` y el arnés de stub (positivo/negativo, incluido el fork con el mismo dueño).
6. Ejecutar el caso real con el PR #167 en un `WORKTREES_ROOT` temporal y limpiar worktree/rama temporales.
7. Ejecutar `bash init.sh` (test + lint) y registrar evidencia en `PROGRESS.md` y `feature_list.json`.

## Implementation Tasks

- [ ] Sustituir la consulta por `headRefName,state,isDraft,isCrossRepository,headRepository` con el `jq`
      indicado (head repo al final, `// "-"`).
- [ ] Parsear `branch state draft cross head_repo` y rechazar solo `cross = "true"` con el mensaje de fork.
- [ ] Eliminar `gh api user`, la variable `me` y el uso de `headRepositoryOwner` en la decisión.
- [ ] Actualizar los comentarios de cabecera (líneas 23 y 41) sin cambiar el contrato 0/1/2.
- [ ] Corregir `.opencode/skills/pr-flow/SKILL.md:60` y `AGENTS.md:128-129`.
- [ ] Pasar `bash -n scripts/pr-worktree.sh`.
- [ ] Ejecutar el arnés de stub: negativos (otro repo; mismo dueño) y positivos (org; reutilización).
- [ ] Ejecutar el caso real `WORKTREES_ROOT=<tmp> bash scripts/pr-worktree.sh 167 main` y limpiar.
- [ ] Ejecutar `bash init.sh` y anotar la evidencia.
- [ ] Actualizar `PROGRESS.md` y `feature_list.json` (estado `passing`, no `accepted`).

## Verification Plan

### 1. Sintaxis y gate del repo

```bash
bash -n scripts/pr-worktree.sh        # exit 0
bash init.sh                          # exit 0: ./gradlew test + ./gradlew lint, sin procesos largos
```

`init.sh` **no** se modifica y debe seguir ejecutando el gate no bloqueante; no arranca servicios ni
emuladores. No hay dependencias nuevas.

### 2. Arnés determinista con stub de `gh` (positivo y negativos)

Ejecutar desde la raíz del checkout de la feature (`repo` se resuelve con `git rev-parse --show-toplevel`).
El stub **solo** responde `pr view`; cualquier otra llamada (`gh api user`) falla con exit 97 y queda en el
log. `PATH` incluye `$HOME/.local/bin` **después** del stub porque `scripts/wsl-env.sh` lo antepondría si
faltara.

```bash
set -uo pipefail
repo="$(git rev-parse --show-toplevel)"
scratch="$(mktemp -d /tmp/sazon-pr-worktree.XXXXXX)"
mkdir -p "$scratch/bin"
git init -q -b main "$scratch/repo"
git -C "$scratch/repo" -c user.name=t -c user.email=t@example.com commit -q --allow-empty -m init
git -C "$scratch/repo" branch feature/demo

cat > "$scratch/bin/gh" <<'GH'
#!/usr/bin/env bash
printf '%s\n' "$*" >> "${GH_STUB_LOG:?GH_STUB_LOG}"
if [ "${1:-}" = "pr" ] && [ "${2:-}" = "view" ]; then
  printf '%s\t%s\t%s\t%s\t%s\n' \
    "${GH_STUB_BRANCH:?}" "${GH_STUB_STATE:-OPEN}" "${GH_STUB_DRAFT:-false}" \
    "${GH_STUB_CROSS:?}" "${GH_STUB_HEAD_REPO:--}"
  exit 0
fi
echo "llamada inesperada a gh: $*" >&2
exit 97
GH
chmod +x "$scratch/bin/gh"

run_case() { # <isCrossRepository> <headRepository.nameWithOwner>
  ( cd "$scratch/repo" && \
    PATH="$scratch/bin:$HOME/.local/bin:$PATH" \
    GH_STUB_LOG="$scratch/calls.log" WORKTREES_ROOT="$scratch/worktrees" \
    GH_STUB_BRANCH=feature/demo GH_STUB_CROSS="$1" GH_STUB_HEAD_REPO="$2" \
    bash "$repo/scripts/pr-worktree.sh" 42 main )
}

# Negativo 1: fork real en otro repositorio.
run_case true otra-org/default >"$scratch/out1" 2>"$scratch/err1"; s1=$?
# Negativo 2: fork cuyo dueño SÍ es el usuario autenticado (el código viejo lo dejaba pasar).
run_case true FernandoGaliay/default >"$scratch/out2" 2>"$scratch/err2"; s2=$?

[ "$s1" = 1 ] && [ ! -s "$scratch/out1" ] && grep -q 'viene de un fork' "$scratch/err1"
[ "$s2" = 1 ] && [ ! -s "$scratch/out2" ] && grep -q 'viene de un fork' "$scratch/err2"
[ ! -e "$scratch/worktrees/demo" ]   # los negativos no crean worktree ni rama

# Positivo: este repositorio, dueño = organización distinta del usuario.
run_case false fernandogaliay-alten/default >"$scratch/out3" 2>"$scratch/err3"; s3=$?
# Positivo repetido: reutiliza el worktree.
run_case false fernandogaliay-alten/default >"$scratch/out4" 2>"$scratch/err4"; s4=$?

[ "$s3" = 0 ] && [ "$(cat "$scratch/out3")" = "$scratch/worktrees/demo" ] && [ -d "$scratch/worktrees/demo" ]
[ "$s4" = 0 ] && grep -q 'worktree ya existe' "$scratch/err4"
! grep -q 'api user' "$scratch/calls.log"
rm -rf "$scratch"
```

Resultado esperado: `s1=1`, `s2=1`, `s3=0`, `s4=0`; el directorio `demo` no existe tras los negativos
(ejecutarlos **antes** del positivo) y sí tras el positivo; el log no contiene `api user`.

### 3. Caso real (verificación exigida)

```bash
gh pr view 167 --json isCrossRepository,headRefName,headRepository,state \
  --jq '[.isCrossRepository,.headRefName,.headRepository.nameWithOwner,.state]|@tsv'
# false  feature/cooking-timer-notification-step-label  fernandogaliay-alten/default  MERGED

had_branch=0
git show-ref --verify --quiet refs/heads/feature/cooking-timer-notification-step-label && had_branch=1
real_root="$(mktemp -d /tmp/sazon-pr167.XXXXXX)"
WORKTREES_ROOT="$real_root" bash scripts/pr-worktree.sh 167 main   # exit 0; imprime <real_root>/cooking-timer-notification-step-label
git worktree remove --force "$real_root/cooking-timer-notification-step-label"
[ "$had_branch" = 1 ] || git branch -D feature/cooking-timer-notification-step-label
rm -rf "$real_root"
```

Si #167 ya no tuviera ref remoto, usar cualquier otro PR de este repositorio con
`isCrossRepository=false`; lo que se verifica es la clasificación, no el estado del PR. Dejar el checkout sin
worktrees ni ramas temporales.

### 4. Inspección de alcance

```bash
git diff --stat    # solo scripts/pr-worktree.sh, .opencode/skills/pr-flow/SKILL.md, AGENTS.md y docs de estado
git diff -- scripts/pr-worktree.sh   # revisar que solo cambia el bloque de gh y los comentarios
```

No hay E2E: el repo no tiene arnés E2E (la UI se valida en JVM), y esta feature no toca flujos de
usuario, autenticación ni rutas de la app.

## Evidence To Capture

En `feature_list.json` (`evidence`) y `PROGRESS.md`:

- `bash -n scripts/pr-worktree.sh` → exit 0.
- `bash init.sh` → exit 0 (test + lint), sin dependencias ni cambios en `init.sh`.
- Salida del arnés: `s1=1` (fork otro repo), `s2=1` (fork mismo dueño), `s3=0` y ruta impresa,
  `s4=0` (worktree ya existe), log sin `api user`, y ausencia/presencia de `$scratch/worktrees/demo` en el
  orden correcto.
- Caso real: `gh pr view 167` con `isCrossRepository=false`, comando del helper, exit 0 y ruta temporal
  impresa; limpieza realizada.
- `git diff --stat` con los ficheros previstos y **sin** cambios en el resto de scripts, app, build,
  lockfile, manifiesto ni permisos.
- Fecha, rama y cualquier desviación (p. ej. `gh` no disponible) con su motivo.

## Validator Checklist

- [ ] La implementación se limita a `scripts/pr-worktree.sh` más los docs indicados; no toca la app, el
      build, `init.sh` ni otros scripts.
- [ ] Los escenarios 1-5 pasan: el positivo de este repositorio no se clasifica como fork y los dos
      negativos (otro repositorio; fork con el mismo dueño) abortan con exit 1 sin crear worktree.
- [ ] La decisión usa `isCrossRepository` y ya **no** existe `gh api user` ni comparación con
      `headRepositoryOwner`.
- [ ] Se conservan stdout (solo ruta), stderr (diagnósticos), exit codes 0/1/2, reutilización de worktree y
      copia de `local.properties`.
- [ ] La evidencia de `feature_list.json`/`PROGRESS.md` es real, fresca y reproducible con los comandos de
      esta spec.
- [ ] `.opencode/skills/pr-flow/SKILL.md` y `AGENTS.md` ya no definen fork por dueño.
- [ ] No hay comportamiento de producto extra, features vecinas ni dependencias nuevas.

## Open Questions / Unknowns

- Ninguna bloqueante. `isCrossRepository` está disponible en el `gh` instalado y en las versiones recientes
  documentadas; si una instalación muy antigua no lo admitiera, `gh pr view --json` fallaría y el script
  caería en «no se encontro el PR» (fallo visible, no una clasificación errónea). No se añade fallback.
