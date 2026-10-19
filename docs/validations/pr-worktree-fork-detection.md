# Validación independiente — `pr-worktree-fork-detection`

- **Feature**: `pr-worktree-fork-detection` (`area: platform`; `depends_on: []`).
- **Spec**: `docs/specs/pr-worktree-fork-detection.md`
- **Rama / worktree**: `feature/pr-worktree-fork-detection`,
  `/home/fergaliay/proyectos/default-worktrees/pr-worktree-fork-detection` (WSL, nativo).
- **Sesión**: validación independiente (subagente `validator`, contexto limpio, sin fiarse del handoff
  del implementer); el número de sesión lo fija el orquestador al persistir el veredicto.
- **Estado previo**: `passing` (implementer, sesión 145; trabajo en disco; `HEAD == 285dc8e` — alta de la
  correctiva —, **sin commit, sin push, sin PR**).
- **Veredicto**: **`accept`**

## Alcance validado

Correctiva de una sola condición en el helper de `pr-flow`:

- `scripts/pr-worktree.sh`: el fork se clasifica por el **repositorio** donde vive la rama head
  (`isCrossRepository`), no por su dueño; se elimina `gh api user`, la variable `me` y la comparación con
  `headRepositoryOwner`.
- `.opencode/skills/pr-flow/SKILL.md` y `AGENTS.md`: la regla de fork queda alineada (head en **otro
  repositorio**, el dueño es irrelevante en una organización).

## Verificación re-ejecutada por el validador (números reales)

| Comprobación | Comando | Resultado |
|---|---|---|
| Sintaxis | `bash -n scripts/pr-worktree.sh` | **exit 0** |
| Arnés determinista (spec §2, literal) | stub efímero de `gh` en `/tmp` | **s1=1** (fork `otra-org/default`) y **s2=1** (fork `FernandoGaliay/default`, mismo dueño que el usuario), ambos con stdout vacío, «viene de un fork» en stderr y **sin** crear `worktrees/demo`; **s3=0** con `<scratch>/worktrees/demo` impreso y directorio creado; **s4=0** con «worktree ya existe» |
| Llamadas a `gh` | log del stub | **4 llamadas, todas `pr view`**; **0 `api user`** (el stub habría salido 97 con cualquier otra) |
| Caso real (verificación exigida) | `gh pr view 167 --json isCrossRepository,headRefName,headRepository,state` | `false`, `feature/cooking-timer-notification-step-label`, `fernandogaliay-alten/default`, `MERGED` |
| Caso real (helper) | `WORKTREES_ROOT=<tmp> bash scripts/pr-worktree.sh 167 main` | **exit 0**, stdout `<tmp>/cooking-timer-notification-step-label`, stderr `PR #167: head=… state=MERGED draft=false` + `local.properties copiado`, **sin** aviso de fork; limpieza (`git worktree remove --force` + `git branch -D`) deja `git worktree list` y ramas en la baseline |
| Contrato | sin argumentos / por feature-id / ref ausente | **exit 2** con uso; feature-id con ref **exit 0** + ruta y mensaje `PR #42: head=feature/demo state=OPEN draft=false`; feature-id sin ref **exit 1** «no existe ref local ni remoto» |
| Gate estándar | `bash init.sh` | **exit 0**, pero `:app:testDebugUnitTest` **`UP-TO-DATE`** ⇒ no vale como evidencia fresca; forzado |
| Tests **forzados** | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | **exit 0**, `BUILD SUCCESSFUL` 52s, **34/34 tareas ejecutadas**; XML frescos **117 suites / 967 tests / 0 fallos / 0 errores / 0 saltados** |
| Lint **forzado** | `bash scripts/gradle.sh lint --rerun-tasks` | **exit 0**, `BUILD SUCCESSFUL` 45s, **30/30 tareas**; txt **0 errores / 13 warnings**; SARIF **13 resultados**, todos warning (`GradleDependency`, `NewerVersionAvailable`, `AndroidGradlePluginVersion`, `OldTargetApi`), preexistentes y ajenos al diff |
| Alcance | `git diff --stat`, `git status --porcelain`, `git diff --check`, `git ls-files -s` | Diff limitado a `scripts/pr-worktree.sh`, `.opencode/skills/pr-flow/SKILL.md`, `AGENTS.md`, `feature_list.json`, `PROGRESS.md` + spec sin trackear; **sin** cambios en `init.sh`, otros `scripts/`, `app/**`, build/lockfile, manifiesto ni permisos; modo `100755` intacto; sin errores de espacios |
| Deriva de reglas | `grep` en `.opencode/**`, `AGENTS.md`, `docs/**` | No queda `headRepositoryOwner` ni «fork por dueño» fuera de la spec (histórico) |

## Escenarios de la spec verificados

1. **Scenario 1 — organización dueña ≠ usuario autenticado (positivo determinista).** Con
   `isCrossRepository=false` y `headRepository.nameWithOwner=fernandogaliay-alten/default` el helper **no**
   aborta, sale 0 e imprime la ruta; el log del stub demuestra que no se consulta el usuario.
2. **Scenario 2 — PR real de este repositorio (verificación exigida).** #167 (`isCrossRepository=false`)
   prepara el worktree con exit 0 y sin mensaje de fork.
3. **Scenario 3 — fork real en otro repositorio (negativa exigida).** Exit 1, «viene de un fork» en stderr,
   stdout vacío y sin worktree ni rama creados.
4. **Scenario 4 — fork con el mismo dueño que el usuario.** También exit 1: el código viejo lo dejaba pasar
   y ahora la decisión depende del repositorio, no del dueño.
5. **Scenario 5 — sin regresión en el contrato.** Reutilización del worktree, invocación por feature-id,
   códigos 0/1/2, mensaje `state`/`draft`, copia de `local.properties` y stdout reservado a la ruta.

## Hallazgos

- **Bloqueantes / mayores**: ninguno.
- **Informativos, sin reparación**:
  - **F1** (estilo): el comentario nuevo de `scripts/pr-worktree.sh:42` mezcla ASCII y `dueño` (ñ); el script
    corre limpio (`bash -n` y arnés) y otros scripts del repo ya usan no-ASCII.
  - **F2** (proceso): la spec estaba sin trackear; el orquestador debe incluirla en el commit de aceptación.
  - **F3** (limitación aceptada): con un `gh` muy antiguo sin `isCrossRepository`, `gh pr view --json` falla
    y el helper cae en «no se encontro el PR» (fallo visible, no una clasificación errónea); documentado en
    la spec y sin fallback, tal como se acordó.

## Seguridad

Sin hallazgos. El cambio **retira** una llamada autenticada (`gh api user`) y no necesita scope de usuario;
los valores de `gh` se consumen con un único `IFS=$'\t' read` y se usan entrecomillados, `head_repo` solo va
a stderr (nunca a una ruta ni a `eval`); se conservan `set -euo pipefail`, el manejo de fallo visible y
stdout reservado a la ruta.

## Persistencia

`feature_list.json` pasa a **`accepted`** con la evidencia de esta validación independiente y el commit local
es `feat: complete pr-worktree-fork-detection` en la rama `feature/pr-worktree-fork-detection` (**sin**
push ni PR; publica `github-flow`).
