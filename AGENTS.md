# Instrucciones para agentes — Sazón

Este repositorio contiene **Sazón**, una app Android que convierte los ingredientes que el usuario ya
tiene en casa en recetas completas generadas con IA.

## Leer primero

- `CONTEXT.md` — vocabulario del dominio. Si un término no está aquí, no lo inventes.
- `docs/build-brief.md` — problema, alcance del MVP y no-objetivos.
- `docs/domain-model.md` — entidades, relaciones y estados.
- `docs/risks-and-open-questions.md` — riesgos e incógnitas sin resolver.

Leer solo cuando sea relevante para la tarea:

- `docs/technical-discovery.md` — stack, almacenamiento, integraciones y verificación.
- `docs/domain/taxonomy.md` — modelo de la Taxonomía y reglas del Validador.
- `docs/design/stitch-prompt.md` — pantallas y microcopy de referencia (material opcional de diseño).
- `docs/design/concepts/` — capturas de referencia aceptadas, una subcarpeta `<feature-id>/` por
  feature de UI; referencia visual opcional, no un requisito del flujo.
- `DESIGN.md` — dirección visual y tokens.
- `docs/adr/` — decisiones aceptadas. Consúltalas si algo parece contradecirlas.

## Flujo de arranque

Antes de escribir código:

1. Confirma el directorio de trabajo.
2. **Clon nuevo**: copia `local.properties.example` a `local.properties`
   (`cp local.properties.example local.properties`) y rellena `sdk.dir`, `opencode.apiKey` (chat de IA)
   y `devexpert.apiKey` (imagen de plato). El fichero no se versiona: las claves se pasan aparte y sin
   ellas la app degrada en silencio (ver "Notas de entorno").
3. Lee `PROGRESS.md` para conocer el estado verificado y el siguiente paso.
4. Lee `feature_list.json` y toma la primera feature lista sin terminar, en orden de lista.
5. Ejecuta el camino de arranque (ver "Notas de entorno" para el comando exacto).
6. Si la verificación base falla, arréglala antes de empezar trabajo nuevo.

## Reglas de trabajo

- Una feature por sesión/worktree. Pueden correr varias features en paralelo si cada una usa su
  propio worktree y su propia sesión (ver `github-flow`); dentro de una sesión no se mezclan features.
- No marques una feature como hecha solo porque hayas añadido código.
- Mantén los cambios dentro del alcance de la feature elegida.
- No cambies las reglas de verificación durante la implementación.
- **Diseño de UI opcional**: `DESIGN.md` es la fuente de verdad visual y las features de UI lo siguen.
  Las capturas de referencia en `docs/design/concepts/<feature-id>/` y los prompts de `docs/design/stitch/`
  son material opcional de apoyo; no son un requisito previo y su ausencia no bloquea el flujo.
- Actualiza los artefactos del repositorio en lugar de depender de resúmenes de chat.

## Acuerdos de trabajo con la persona

El flujo de entrega lo define `github-flow` (sobre `feature-flow`) y sus reglas mandan:

- El ciclo spec → implementación → validación es **autónomo**: los subagentes trabajan sin aprobación
  paso a paso. El validador independiente es la única puerta de calidad; solo un `accept` cierra la
  feature.
- El **commit** se crea automáticamente al `accept` (regla de `feature-flow`), en la rama
  `feature/<id>` del worktree de esa feature.
- Al `accept`, **el orquestador de `github-flow`** publica automáticamente: hace push de la rama
  `feature/<id>` y abre (o reutiliza) el **PR** contra la rama base, con **permiso de push
  permanente**. No se pide autorización por ejecución. Una sesión que ejecute `feature-flow` (incluida
  la sesión hija de un worktree) **nunca** hace push ni abre PR, ni siquiera teniendo el permiso: su
  salida es el commit local.
- El merge lo hace **solo el orquestador de `pr-flow`**, con **merge commit** (`gh pr merge <n> --merge`)
  y **solo** cuando el `reviewer` devuelve `accept`, no hay conflictos y no hay checks requeridos en
  rojo. Ningún otro flujo mergea: `github-flow` publica el PR y ahí termina. Nunca se usa `--squash`,
  `--rebase` ni `--auto`, y nunca se hace force-push.
- **Verificación de UI**: no se automatizan pruebas de UI (emulador). Las features de UI se validan por
  lógica/estado en JVM cuando sea posible y contra `DESIGN.md` (fuente de verdad visual); lo puramente
  visual lo revisa la persona tras el merge. Un defecto visual se
  corrige con una **feature correctiva** nueva, sin bloquear el flujo autónomo.
- Los pasos manuales fuera del repositorio (crear el proyecto con el asistente de Android Studio;
  opcionalmente, generar material de diseño en Stitch) los hace la persona.

## Reparto de roles (subagentes)

Cada fase del ciclo la ejecuta un subagente especializado; el agente principal orquesta y persiste la
aceptación.

- La especificación (`feature-spec`) la hace el subagente **`planner`**.
- La implementación (`feature-implementer`) la hace el subagente **`implementer`**.
- La validación (`feature-validator`) la hace el subagente **`validator`**.
- La revisión del PR (`pr-review`) la hace el subagente **`reviewer`**, orquestada por `pr-flow`.
- El agente principal **no** ejecuta esas skills directamente: delega en el subagente del rol. El
  flujo completo (`feature-flow`) lo orquesta el agente principal.
- Para varias features en paralelo sobre un repositorio GitHub, el orquestador usa la skill
  **`github-flow`**: crea issues, ramas y worktrees, lanza una sesión independiente por worktree
  (`bash scripts/launch-feature-session.sh <feature-id>`, que fija el cwd del worktree) y delega el
  ciclo de cada feature a `feature-flow`. La sesión se lanza **desacoplada** (`SAZON_DETACH=1`), sin
  bloquear una tool call durante todo el ciclo (30–60+ min): el helper retorna al instante y escribe
  su salida en `<worktree>/.feature-session.log` y el código de salida en
  `<worktree>/.feature-session.exit`. La **señal de fin** es esa sentinela (o, si se pierde, los
  artefactos: estado en `feature_list.json` + commit local), **no** el retorno de la llamada;
  `bash scripts/wait-feature-session.sh <feature-id>` espera de forma acotada. `github-flow`
  **nunca** hace merge.
- La revisión y el merge de los PRs abiertos los orquesta la skill **`pr-flow`**: lanza el subagente
  **`reviewer`**, delega las reparaciones en **`implementer`**, resuelve los conflictos como único
  integrador y mergea. Es el **único** flujo con permiso de merge.
- Atajos: `/spec [id]`, `/implement [id]`, `/validate [id]`, `/feature [id]` y `/review-pr [nº|id]`.
  El `id` es opcional; sin él, la skill correspondiente selecciona la siguiente feature (o, en
  `pr-flow`, todos los PRs abiertos).

## Repositorio y publicación (github-flow)

- Remoto: `origin` → `https://github.com/fernandogaliay-alten/default.git` (privado).
- Rama base: **`main`**; cada feature trabaja en `feature/<feature-id>`.
- Worktree por feature, **fuera** del checkout base, en `../<carpeta-del-checkout>-worktrees`
  (aquí `../Default-Project-worktrees`), creado **desde WSL**.
- Herramientas requeridas: **opencode CLI** (`opencode run`) y **`gh`** autenticado.
- **Quién publica**: solo el orquestador de `github-flow`, una vez, al `accept` (reutilizando el PR si
  ya existe). `feature-flow` termina en el commit local de la rama; nunca hace push ni PR.
- **Formato del PR** (obligatorio al publicar, lo redacta el orquestador; también si publica a mano):
  título **`[<feature-id>] <título de la feature>`** y, en el cuerpo, **una línea `Closes #<n>`** con la
  issue de la feature. La palabra clave debe ir **en inglés** (`Closes`/`Fixes`/`Resolves`): GitHub **no**
  reconoce `Cierra #<n>`, ni una mera referencia `Issue: #<n>`, así que la issue **no** se cerraría al
  mergear (pasó con la #77 / PR #79).
- **Integración de artefactos compartidos**: el orquestador es el único integrador. Antes de publicar,
  trae `origin/main` a la rama con un merge (`scripts/sync-worktree.sh <id>`; nunca rebase ni
  force-push) y resuelve los conflictos de estado/docs (`feature_list.json`, `PROGRESS.md`,
  `docs/technical-discovery.md`, …) manteniendo **ambas** aportaciones. Tras cada merge de un PR,
  re-sincroniza las ramas abiertas restantes igual.
- Modos de `github-flow`: `plan` (solo lectura), `execute` (crea issues/worktrees/sesiones y, al
  `accept`, **commitea, hace push y abre el PR** automáticamente), `publish` (republicación: push +
  PR) y `prune` (limpieza: elimina worktree y rama local de una feature **solo** si está `accepted`
  y su PR está mergeado; `execute` y `publish` lo ejecutan antes de trabajar). Identidades
  deterministas: rama `feature/<id>`, issue `[<id>] <título>`, PR `feature/<id>` → `main`. Nunca
  hace merge.

## Revisión y merge de PRs (pr-flow)

- `pr-flow` opera sobre los **PRs ya abiertos** por `github-flow`, no sobre ramas locales. Alcance por
  ejecución: un PR (número o feature id) o todos los PRs abiertos contra la rama base, uno a uno.
- Precondiciones: `gh` autenticado con permiso de merge y la base al día. Los PRs de un fork quedan
  fuera: un fork es un head en **otro repositorio** (`isCrossRepository=true`), no un dueño distinto; el
  head debe ser una rama `feature/<id>` de este repositorio.
- Ciclo por PR: sincronizar la base en la rama (`scripts/sync-worktree.sh <id>`), revisar con el
  subagente `reviewer`, reparar un `revise` con el subagente `implementer` (máx. 2 rondas) y mergear
  al `accept`.
- **Quién resuelve conflictos**: el orquestador de `pr-flow` (único integrador), manteniendo **ambas**
  aportaciones en estado/docs; si el conflicto exige criterio de código, se delega en `implementer`.
  El `reviewer` solo los **detecta** y los reporta.
- **Merge**: solo con `accept` + sin conflictos + sin checks requeridos en rojo, con **merge commit**
  (`gh pr merge <n> --merge`). Nunca `--squash`, `--rebase` ni `--auto`.
- Worktree del PR: se reutiliza el de `github-flow`; si falta,
  `scripts/pr-worktree.sh <nº-pr> [base-branch]` (imprime la ruta).
- Tras mergear, el modo `prune` de `github-flow` limpia el worktree/rama mergeados; nunca se fuerza.

## Artefactos obligatorios

- `feature_list.json` — fuente de verdad del estado de las features.
- `PROGRESS.md` — estado verificado y registro ligero de sesiones.
- `init.sh` — camino estándar de arranque y verificación.

## Ciclo de vida de una feature

```
not_started → in_progress → passing → accepted
                    ↓
                 blocked
```

- `passing`: implementado y **autoverificado** con evidencia registrada. No es aceptación final.
- `accepted`: un **validador independiente** devolvió `accept`. Lo persiste el orquestador, nunca
  quien implementó.
- `blocked`: no se puede continuar y el motivo queda registrado en `PROGRESS.md` o en la feature.
- Un vector de dependencias está satisfecho cuando apunta a una feature en `accepted`.
- Como máximo una feature puede estar en `in_progress` **por worktree/sesión**. Con `github-flow`
  pueden coexistir varias en paralelo, cada una en su rama `feature/<id>`.

## Definición de hecho

Una feature está hecha solo si se cumplen todas:

- el comportamiento objetivo está implementado,
- la verificación exigida se ejecutó de verdad,
- la evidencia queda registrada en `feature_list.json` o `PROGRESS.md`,
- el repositorio sigue siendo reiniciable desde el camino estándar,
- la documentación relevante está actualizada si cambió el comportamiento o las reglas.

## Fin de sesión

1. Actualiza `PROGRESS.md`.
2. Actualiza `feature_list.json`.
3. Registra riesgos o bloqueos sin resolver.
4. Deja el repositorio listo para que la siguiente sesión pueda verificar de inmediato.

## Notas de entorno

- Plataforma: **WSL2 (Ubuntu)**; el checkout vive en el sistema de ficheros de Windows
  (`/mnt/c/Users/fernando.galiay/Documents/Default-Project`). `init.sh` es un script bash normal
  (`bash init.sh` o `./init.sh`); ya no hace falta Git Bash ni PowerShell.
- El proyecto Android **está bootstrapeado**: `init.sh` ejecuta la verificación real
  (`./gradlew test` y `./gradlew lint`) y **no** arranca procesos largos.
- **Nunca ejecutes `./gradlew` directamente desde una sesión de agente**: usa `bash init.sh` (camino
  estándar) o el wrapper `bash scripts/gradle.sh <tareas>` para comprobaciones puntuales. Gradle
  arranca daemons (de Gradle y, sobre todo, de Kotlin al compilar) que heredan el stdout del comando;
  si alguno sobrevive, la herramienta de shell espera para siempre y la sesión queda colgada aunque el
  build ya haya impreso `BUILD SUCCESSFUL`. El wrapper manda la salida a un fichero y solo después la
  vuelca, así que es seguro en cualquier shell. Tampoco uses `./gradlew --stop`: es global y mata
  también los builds de otros worktrees que corren en paralelo (los daemons se apagan solos por
  inactividad).
- **Entorno de WSL**: `scripts/wsl-env.sh` —que sourcean `init.sh`, `scripts/gradle.sh` y
  `scripts/new-worktree.sh`— publica `JAVA_HOME` (JDK 21 de Linux en `~/.local/opt/jdk-21`),
  `ANDROID_HOME`/`ANDROID_SDK_ROOT` (SDK de Linux en `~/Android/Sdk`), `GRADLE_USER_HOME` en ext4
  (`~/.gradle`) y el `PATH` de `gh`. Fuera de WSL no hace nada, así que los mismos scripts sirven en
  ambos sistemas. No asumas que el `java` del PATH sirve para compilar: es el de Windows (JDK 11, vía
  interop).
- Si falta el JDK o el SDK, `bash scripts/wsl-bootstrap.sh` los instala (idempotente, **sin sudo**,
  todo bajo `$HOME`). El daemon que exige `gradle/gradle-daemon-jvm.properties` es **JDK 21**, el mismo
  del entorno, así que Gradle no aprovisiona otro.
- El **SDK de Windows no sirve** para compilar en WSL: solo trae binarios `.exe` (`aapt2.exe`,
  `adb.exe`, `zipalign.exe`) que Linux no puede ejecutar. Por eso hay un SDK de Linux. Android Studio
  en Windows sigue valiendo para editar y sincronizar, pero la verificación canónica corre en WSL.
- **`adb` y los dispositivos**: el emulador (y un móvil por USB) los ve el `adb` que corre en **Windows**.
  El `adb` de WSL está en otra red y no los ve, así que `bash scripts/gradle.sh installDebug` desde WSL
  no encuentra dispositivo útil: la comprobación visual/manual sigue haciéndose en Android Studio. Solo
  si se quiere instalar desde WSL hay que puentear `adb` (p. ej. invocando el `adb.exe` de Windows).
- `local.properties` (fuera de control de versiones) lo regenera Android Studio con la ruta del SDK de
  Windows y **solo** con `sdk.dir`: la plantilla versionada **sin secretos** `local.properties.example`
  dice qué propiedades hacen falta (`sdk.dir`, `opencode.apiKey` y `devexpert.apiKey`); cópiala en un clon
  nuevo y rellena las claves, que se pasan aparte porque no están en git. Sin `opencode.apiKey` el chat
  (generador, búsqueda y lote) degrada en silencio a **modo semilla** con `MISSING_KEY` en el log; sin
  `devexpert.apiKey` la imagen de plato cae a la **ficha tipográfica**. `wsl-env.sh` reescribe `sdk.dir` a
  la del SDK de Linux al detectar una ruta de Windows, así que no hay que mantenerlo a mano.
- La **caché de Gradle** (dependencias, distribuciones, JDKs aprovisionados) vive en ext4
  (`~/.gradle`): bajo `/mnt/c` (DrvFs) cada acceso cuesta órdenes de magnitud más. `wsl-env.sh` fuerza
  `GRADLE_USER_HOME` a ext4 y desactiva el file-watching (`-Dorg.gradle.vfs.watch=false`), que no es
  fiable en DrvFs.
- **Mezcla de rutas Windows/WSL**: si Gradle en WSL falla con
  `this and base files have different roots: C:\... and /mnt/c/...` (habitual tras compilar en Android
  Studio de Windows sobre el mismo `build/`), son cachés incrementales de KSP/Kotlin que guardaron
  rutas de Windows. Arreglo: borrar `app/build/kotlin`, `app/build/kspCaches`, `app/build/generated/ksp`
  y `.kotlin/sessions`, y reintentar `bash init.sh` (visto en la Sesión 131); no es un fallo de código.
- **Worktrees (`github-flow`)**: crea el worktree de una feature con
  `bash scripts/new-worktree.sh <feature-id>`, **desde WSL**. El script hace `git fetch origin main`
  antes de ramificar (para no partir de un `origin/main` obsoleto), crea la rama `feature/<feature-id>`
  en `../<checkout>-worktrees/<id>`, copia `local.properties` al worktree (ignorado en git, no viaja
  solo) y avisa por stderr si al `local.properties` resultante le falta `opencode.apiKey` (chat → modo
  semilla con `MISSING_KEY`) o `devexpert.apiKey` (imagen → ficha tipográfica), sin cambiar el código de
  salida; también avisa si la rama queda por detrás de la base. Sin `local.properties` ni `ANDROID_HOME`,
  `./init.sh` falla al no encontrar el SDK. No se hace `pull` de la base dentro del worktree.
- **Los worktrees se crean desde WSL, no desde Git Bash.** La metadata de git de un worktree guarda
  rutas absolutas con el formato del sistema que lo creó: uno creado en Windows aparece `prunable` en
  WSL (y `git status` dentro de él falla con `not a git repository`); uno creado en WSL deja de poder
  abrirse desde Git Bash. Reparar uno roto: reescribir con rutas `/mnt/c/...` el `.git` del worktree
  (`gitdir: ...`) y el back-link `.git/worktrees/<id>/gitdir` del checkout base.
- **Recarga del harness en caliente**: OpenCode (`v2`) descubre `.opencode/agent/`, `.opencode/command/`
  y `.opencode/skills/` al cargar la location, pero en este checkout (`/mnt/c`, DrvFs) su watcher de
  ficheros no dispara y los artefactos **nuevos no aparecen solos** (misma causa que el file-watching
  desactivado de Gradle). Tras añadir o editar agentes, comandos o skills, recarga la location:
  `opencode api post /api/location/reload -H "x-opencode-directory:$PWD"` (o reinicia el TUI;
  `opencode service restart` para el servicio compartido). Los ficheros sí son válidos: el problema es
  solo de descubrimiento.
- **Uso de herramientas en el harness (Code Mode)**: `shell`, `read`, `edit`, `grep`, `write`… se
  invocan **directamente**, nunca dentro de la herramienta `execute`. `execute` corre JavaScript en un
  runtime aislado (Code Mode) donde **solo** existen las herramientas de su catálogo (aquí, el
  namespace `opencode`: sesiones, worktrees, modelos, recursos MCP). Meter una orden de shell dentro de
  `execute` falla con `Unknown tool 'shell'`. El error se cuela sobre todo en las skills de
  orquestación pesada (`pr-flow`, `github-flow`), que encadenan muchos `git`/`gh`/`scripts/*.sh`: la
  tentación de agruparlos en `execute` es lo que lo provoca, no la skill en sí.
- **Herramientas nativas del harness (WSL)**: `opencode` ya se instala nativo (`~/.opencode/bin/opencode`,
  binario Linux; el segfault intermitente al cerrar era de la build de Windows) y
  `bash scripts/wsl-bootstrap.sh` instala `gh` nativo en `~/.local/bin/gh`, reutilizando la sesión de
  gh de Windows (si no, `gh auth login`). `node`/`npm` del `PATH` siguen siendo los de Windows por
  interop y **no** hacen falta para el harness.
- La verificación se juzga por **artefactos** (git, `feature_list`/`PROGRESS`, PR, logs de Gradle), no
  por el código de salida del CLI.
- `scripts/cleanup-worktree.sh` mantiene `git -c core.longpaths=true` solo por compatibilidad con el
  camino heredado de Windows (rutas > MAX_PATH al borrar `build/`/`.gradle/`); en WSL es inocuo.
- Al hacer el primer commit, marca los scripts como ejecutables para no perder el bit en otros
  entornos: `git add --chmod=+x init.sh scripts/*.sh`.
