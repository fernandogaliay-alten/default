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
- `docs/design/stitch-prompt.md` — pantallas y microcopy de referencia.
- `DESIGN.md` — dirección visual y tokens.
- `docs/adr/` — decisiones aceptadas. Consúltalas si algo parece contradecirlas.

## Flujo de arranque

Antes de escribir código:

1. Confirma el directorio de trabajo.
2. Lee `PROGRESS.md` para conocer el estado verificado y el siguiente paso.
3. Lee `feature_list.json` y toma la primera feature lista sin terminar, en orden de lista.
4. Ejecuta el camino de arranque (ver "Notas de entorno" para el comando exacto en Windows).
5. Si la verificación base falla, arréglala antes de empezar trabajo nuevo.

## Reglas de trabajo

- Una feature por sesión/worktree. Pueden correr varias features en paralelo si cada una usa su
  propio worktree y su propia sesión (ver `github-flow`); dentro de una sesión no se mezclan features.
- No marques una feature como hecha solo porque hayas añadido código.
- Mantén los cambios dentro del alcance de la feature elegida.
- No cambies las reglas de verificación durante la implementación.
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
- El agente **nunca hace merge** ni activa auto-merge: el merge del PR lo hace la persona.
- Los pasos manuales fuera del repositorio (crear el proyecto con el asistente de Android Studio,
  regenerar las pantallas en Stitch) los hace la persona.

## Reparto de roles (subagentes)

Cada fase del ciclo la ejecuta un subagente especializado; el agente principal orquesta y persiste la
aceptación.

- La especificación (`feature-spec`) la hace el subagente **`planner`**.
- La implementación (`feature-implementer`) la hace el subagente **`implementer`**.
- La validación (`feature-validator`) la hace el subagente **`validator`**.
- El agente principal **no** ejecuta esas skills directamente: delega en el subagente del rol. El
  flujo completo (`feature-flow`) lo orquesta el agente principal.
- Para varias features en paralelo sobre un repositorio GitHub, el orquestador usa la skill
  **`github-flow`**: crea issues, ramas y worktrees, lanza una sesión independiente por worktree
  (`opencode run --dir <worktree>`) y delega el ciclo de cada feature a `feature-flow`.
  `github-flow` **nunca** hace merge.
- Atajos: `/spec [id]`, `/implement [id]`, `/validate [id]` y `/feature [id]`. El `id` es opcional;
  sin él, la skill correspondiente selecciona la siguiente feature.

## Repositorio y publicación (github-flow)

- Remoto: `origin` → `https://github.com/fernandogaliay-alten/default.git` (privado).
- Rama base: **`main`**; cada feature trabaja en `feature/<feature-id>`.
- Worktree por feature, **fuera** del checkout base, en `../<carpeta-del-checkout>-worktrees`
  (aquí `..\Default Project-worktrees`).
- Herramientas requeridas: **opencode CLI** (`opencode run`) y **`gh`** autenticado.
- **Quién publica**: solo el orquestador de `github-flow`, una vez, al `accept` (reutilizando el PR si
  ya existe). `feature-flow` termina en el commit local de la rama; nunca hace push ni PR.
- **Integración de artefactos compartidos**: el orquestador es el único integrador. Antes de publicar,
  trae `origin/main` a la rama con un merge (`scripts/sync-worktree.sh <id>`; nunca rebase ni
  force-push) y resuelve los conflictos de estado/docs (`feature_list.json`, `PROGRESS.md`,
  `docs/technical-discovery.md`, …) manteniendo **ambas** aportaciones. Tras cada merge de un PR,
  re-sincroniza las ramas abiertas restantes igual.
- Modos de `github-flow`: `plan` (solo lectura), `execute` (crea issues/worktrees/sesiones y, al
  `accept`, **commitea, hace push y abre el PR** automáticamente) y `publish` (republicación: push +
  PR). Identidades deterministas: rama `feature/<id>`, issue `[<id>] <título>`, PR `feature/<id>` →
  `main`. Nunca hace merge.

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

- Plataforma: **Windows**. `init.sh` es un script bash, no un ejecutable de PowerShell. Se invoca con
  `bash init.sh` si `C:\Program Files\Git\bin` está en el PATH; si no, con la ruta completa:

  ```powershell
  & "C:\Program Files\Git\bin\bash.exe" init.sh
  ```

  En este equipo `Git\bin` se añadió al PATH de sistema, pero las sesiones abiertas antes del cambio
  no lo ven.
- El proyecto Android **está bootstrapeado**: `init.sh` ejecuta la verificación real
  (`./gradlew test` y `./gradlew lint`) y **no** arranca procesos largos.
- `java` en el PATH es **JDK 11**, insuficiente para Android Gradle Plugin. El wrapper arranca con
  `JAVA_HOME` (el JBR de Android Studio, **JDK 17**) y el daemon corre con un **JDK 21** que Gradle
  aprovisiona solo, según `gradle/gradle-daemon-jvm.properties`. No asumas que el `java` del PATH
  sirve para compilar.
- El SDK de Android está en `%LOCALAPPDATA%\Android\Sdk` y se localiza vía `local.properties`
  (`sdk.dir`), que está fuera de control de versiones. `ANDROID_HOME` está definido a nivel de
  usuario como respaldo para los worktrees.
- **Worktrees (`github-flow`)**: crea el worktree de una feature con
  `bash scripts/new-worktree.sh <feature-id>`. El script hace `git fetch origin main` antes de
  ramificar (para no partir de un `origin/main` obsoleto), crea la rama `feature/<feature-id>` en
  `../<checkout>-worktrees/<id>`, copia `local.properties` al worktree (ignorado en git, no viaja
  solo) y avisa si la rama queda por detrás de la base. Sin `local.properties` ni `ANDROID_HOME`,
  `./init.sh` falla al no encontrar el SDK. No se hace `pull` de la base dentro del worktree.
- **Monitoreo en vivo (TUI)**: arranca el backend compartido una vez con `bash scripts/serve.sh`
  (`opencode serve --port 4096`) y observa las sesiones con
  `opencode attach http://localhost:4096` en Windows Terminal. El orquestador lanza cada feature con
  `opencode run --attach http://localhost:4096 ...` para que su progreso aparezca en esa TUI.
- Al hacer el primer commit, marca los scripts como ejecutables para no perder el bit en otros
  entornos: `git add --chmod=+x init.sh scripts/new-worktree.sh`.
