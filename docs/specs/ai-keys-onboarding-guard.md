# Feature Implementation Spec: Las claves de IA no se pierden al clonar el repo o al crear un worktree

## Source Feature

- `id`: `ai-keys-onboarding-guard`
- `area`: `platform`
- `depends_on`: `[]`
- `status`: `not_started` (al planificar; última entrada de `feature_list.json`)
- `source`: `feature_list.json:1898-1913` (título, `user_visible_behavior`, las **tres** verificaciones y
  las `notes` con el alcance acordado). Correctiva pedida por la persona (2026-10-19) tras probar la
  Búsqueda con IA en una instalación hecha desde otro PC.

## Goal

Que las dos claves de IA que la app necesita en tiempo de build (`opencode.apiKey` para el chat y
`devexpert.apiKey` para la imagen de plato) queden **explícitas y verificables** en cualquier equipo,
en vez de descubrirse por un `MISSING_KEY` silencioso. Hoy `local.properties` está ignorado en git
(`.gitignore:5`), Android Studio genera uno nuevo con **solo** `sdk.dir` y nada en el repo dice qué
claves hacen falta; `scripts/new-worktree.sh` copia el fichero al worktree pero solo avisa si **no
existe**.

El resultado es: una plantilla **versionada y sin secretos** (`local.properties.example`) con los
nombres de las propiedades y valores vacíos; un **aviso por stderr** en `scripts/new-worktree.sh`
cuando el `local.properties` resultante no define `opencode.apiKey` (o `devexpert.apiKey`), nombrando
la consecuencia real (modo semilla / ficha tipográfica); y la documentación durable (`AGENTS.md`,
`docs/technical-discovery.md`) diciendo dónde viven las claves y qué degrada si faltan.

**No cambia el comportamiento de la app**: la degradación silenciosa se mantiene tal cual; lo que
cambia es que el fallo de configuración deja de ser invisible en el arranque del harness.

## Non-Goals

- **No** se crean, mueven, cifran ni publican secretos: la plantilla lleva valores **vacíos**. Las
  claves siguen fuera de git y se pasan aparte.
- **No** se toca `local.properties` (sigue ignorado y fuera de control de versiones).
- **No** se toca la app: `DevExpertRecipeGenerator` y su guarda `if (apiKey.isBlank())` (que emite
  `UNAVAILABLE`/`MISSING_KEY` con `attempt=0`) siguen **exactamente** igual, ni `MainActivity`, ni el
  modo semilla, ni los diagnósticos `SazonAi`.
- **No** se toca `app/build.gradle.kts` (`getProperty(..., "")`), ni `gradle/libs.versions.toml`, ni
  el manifiesto, ni permisos, ni `init.sh`.
- **No** se toca el resto de scripts (`wsl-env.sh`, `gradle.sh`, `pr-worktree.sh`,
  `sync-worktree.sh`, `cleanup-worktree.sh`, `wsl-bootstrap.sh`, `launch-feature-session.sh`,
  `wait-feature-session.sh`): el único script modificado es `new-worktree.sh`.
- **No** se rellena automáticamente ninguna clave (ni desde el entorno, ni con prompts interactivos):
  el script **avisa**, no pide, no escribe y no bloquea.
- **No** cambia el contrato de `new-worktree.sh`: mismos mensajes de stdout, mismos códigos de salida
  (0/1/2) y misma creación/reutilización de worktree; los avisos nuevos van **a stderr**.
- **No** se añade arnés de tests versionado para `scripts/`, ni CI, ni hooks de git, ni
  `.gitattributes`, ni dependencias nuevas.

## Job Story

When clono el repositorio en otra máquina (o creo un worktree) y la app me devuelve
`SEARCH_BY_TEXT UNAVAILABLE ... MISSING_KEY` sin ninguna petición HTTP,
I want que el repo me diga qué claves de IA hay que poner en `local.properties` y que el helper de
worktrees me avise si falta alguna,
so I can arreglar la configuración en un minuto en vez de diagnosticar un error críptico en logcat.

## Users And Permissions

- **Persona mantenedora / estudiante**: clona el repo o crea un worktree; necesita la plantilla y el
  aviso. No necesita permisos nuevos; las claves las aporta ella aparte (canal fuera del repo).
- **Agentes del harness** (`github-flow`, `feature-flow`): invocan `scripts/new-worktree.sh`; el aviso
  por stderr es informativo y **no** cambia el código de salida, así que no rompe el flujo autónomo.
- **Nadie lee las claves**: ni el script ni su aviso imprimen valores; solo el **nombre** de la
  propiedad ausente.

## Acceptance Scenarios

### Scenario 1: la plantilla versionada existe y no lleva secretos (positiva)

Given un clon limpio del repositorio, sin `local.properties`,
When se inspecciona el repo,
Then existe `local.properties.example` **rastreado por git**, con `sdk.dir`, `opencode.apiKey` y
`devexpert.apiKey` con **valores vacíos**, comentarios que explican para qué sirve cada clave y que
`local.properties` no se versiona; y el fichero no contiene ningún valor de clave real.

### Scenario 2: worktree sin `opencode.apiKey` (negativa, verificación exigida)

Given un checkout cuyo `local.properties` define `sdk.dir` pero **no** define `opencode.apiKey`,
When se ejecuta `scripts/new-worktree.sh <id> <sha>`,
Then el script termina con **exit 0**, crea el worktree, **no** cambia su stdout y escribe en
**stderr** un aviso que nombra `opencode.apiKey` y la consecuencia (`MISSING_KEY`, modo semilla),
señalando dónde rellenarla y la existencia de `local.properties.example`.

### Scenario 3: worktree con las claves presentes (positiva, verificación exigida)

Given un checkout cuyo `local.properties` define `opencode.apiKey` y `devexpert.apiKey` con valores no
vacíos,
When se ejecuta `scripts/new-worktree.sh <id> <sha>`,
Then el script termina con exit 0 y **no** emite ningún aviso de clave ausente (ni por `opencode.apiKey`
ni por `devexpert.apiKey`), y `local.properties` se copia al worktree como hasta ahora.

### Scenario 4: propiedad presente pero vacía (borde)

Given un `local.properties` que contiene la línea `opencode.apiKey=` (sin valor) — el caso exacto que
produce `BuildConfig.OPENCODE_API_KEY == ""` y por tanto `MISSING_KEY` —,
When se ejecuta el helper,
Then se trata como **ausente**: el aviso del escenario 2 se emite igual (una presencia sin valor no
silencia el aviso).

### Scenario 5: solo falta la clave de la imagen (borde, aviso específico)

Given un `local.properties` con `opencode.apiKey` rellena pero sin `devexpert.apiKey`,
When se ejecuta el helper,
Then **no** hay aviso del chat y **sí** uno que nombra `devexpert.apiKey` y su consecuencia (la imagen
de plato cae a la ficha tipográfica): cada clave se evalúa por separado.

### Scenario 6: sin regresión en el contrato del script

Given un checkout **sin** `local.properties` y sin `ANDROID_HOME`, o un worktree que ya existe,
When se ejecuta el helper,
Then se conservan el aviso actual (`no hay local.properties ni ANDROID_HOME; el build puede fallar`), la
rama `ANDROID_HOME`, la reutilización del worktree, el mensaje `local.properties copiado a <ruta>`,
las líneas `worktree: <ruta>` / `branch: ...` de stdout, el aviso de desincronización respecto a la base
y los códigos de salida 0 (éxito) y 2 (sin argumentos).

### Scenario 7: la documentación dice dónde viven las claves y qué degrada

Given un clon nuevo en otra máquina,
When la persona lee `AGENTS.md` y `docs/technical-discovery.md`,
Then encuentra el paso obligatorio al clonar (crear `local.properties` con `sdk.dir` + las dos claves,
que se pasan aparte porque no están en git) y qué se degrada si falta cada clave (chat → modo semilla
con `MISSING_KEY`; imagen → ficha tipográfica).

## Repository Research

### Files Inspected

- `feature_list.json:1898-1913` — entrada de la feature: las tres verificaciones (gate en verde +
  plantilla versionada sin secretos + prueba reproducible con y sin `opencode.apiKey`) y las `notes`
  con el alcance acordado.
- `.gitignore:5` — `local.properties` (patrón sin comodines: **no** captura `local.properties.example`;
  verificado con `git check-ignore -v --no-index local.properties.example` → **exit 1**, no ignorado).
- `scripts/new-worktree.sh` (86 líneas) — objeto del cambio: líneas 67-75 son el bloque de
  `local.properties` (copia / `ANDROID_HOME` / aviso) y 77-86 los mensajes finales y el aviso de
  desincronización. Cabecera 8-18 con el uso y las variables de entorno.
- `app/build.gradle.kts:19-28` y `54-66` — las dos claves se leen de `local.properties` con
  `getProperty("devexpert.apiKey", "")` (línea 27) / `getProperty("opencode.apiKey", "")` (28) y se
  inyectan como `BuildConfig.DEVEXPERT_API_KEY` (57) / `BuildConfig.OPENCODE_API_KEY` (62); valor `""`
  si falta.
- `app/src/main/java/com/sazon/app/data/ai/DevExpertRecipeGenerator.kt:130-147` — la guarda
  `if (apiKey.isBlank())` reporta `AiCallOutcome.UNAVAILABLE` con `attempt = 0`, `httpStatus = null` y
  `detail = MISSING_KEY`, y lanza `AiUnavailableException(MISSING_KEY, "Falta la clave del proveedor de
  IA (opencode.apiKey); se degrada a modo semilla.")`. Es el origen del log reportado, **no** la API.
- `scripts/wsl-env.sh:37-57, 101` — normaliza `sdk.dir` en `$SAZON_REPO_ROOT/local.properties` (solo si
  detecta ruta de Windows) y respeta `SAZON_WSL_LOCAL_PROPS=0` para no tocar nada; publica
  `ANDROID_HOME` si existe `~/Android/Sdk`. Importa para que el arnés **no** mute el
  `local.properties` real.
- `AGENTS.md:23-31` (flujo de arranque), `213-215` (`local.properties` lo regenera Android Studio) y
  `225-230` (worktrees: copia de `local.properties` y aviso solo si falta el fichero).
- `docs/technical-discovery.md:53` (fila `| Claves de IA |` de *Data and Storage*) y `1054-1059`
  (*Deployment and Operations*, «La clave de IA se inyecta en tiempo de build desde `local.properties`
  (que no se versiona)»). También `1251` (worktrees y `wsl-bootstrap.sh`).
- `docs/risks-and-open-questions.md:148, 155` — la clave en `BuildConfig` ya está documentada como
  aceptable en demo y `local.properties` fuera de control de versiones.
- `PROGRESS.md` (últimas sesiones 145/146) y `docs/specs/pr-worktree-fork-detection.md` — formato de
  spec reciente y patrón de verificación con arnés determinista en `/tmp`.

### Existing Patterns To Follow

- Bash estricto (`set -euo pipefail`), diagnósticos a **stderr**, comentarios y mensajes en español y
  **sin tildes** (`AVISO: ...`, `no se pudo leer ...`).
- Los scripts no tienen tests: la verificación es `bash -n` + arnés efímero en `/tmp` (patrón de
  `docs/specs/pr-worktree-fork-detection.md`) + el gate `bash init.sh`.
- Un único `local.properties` en la raíz del checkout base es la fuente; el worktree recibe una
  **copia**. `wsl-env.sh` ya tiene el precedente de respetar `SAZON_WSL_LOCAL_PROPS=0`.
- Los patrones de propiedad se leen con `grep -E` (mismo estilo que `wsl-env.sh:48`).

### Current Gaps

- No existe ninguna plantilla ni documentación del **contenido** que debe tener `local.properties`
  (hoy solo se documenta que existe y que lo regenera Android Studio).
- `scripts/new-worktree.sh` no distingue «fichero copiado y completo» de «fichero copiado sin claves»:
  el único aviso es por ausencia del fichero (línea 74).
- No hay arnés de tests para `scripts/`: la reproducibilidad se resuelve con el arnés de la sección
  `Verification Plan`, sin ampliar el repositorio.
- `shellcheck` **no** está instalado en el entorno: la comprobación estática disponible es `bash -n`.

## Technical Approach

1. **Plantilla versionada `local.properties.example`** en la **raíz** del repo (junto a
   `local.properties`): solo nombres de propiedades con valor vacío y comentarios. Contenido exacto
   esperado (los comentarios pueden pulirse, los nombres y los valores vacíos **no** cambian):

   ```properties
   ## Plantilla de local.properties. NO contiene secretos: copiala y rellena los valores.
   ##   cp local.properties.example local.properties
   ## local.properties esta ignorado en git (las claves de IA no se versionan y no viajan
   ## con el clon): pasalas aparte a cada maquina.

   ## Ruta del SDK de Android. La escribe Android Studio; en WSL, scripts/wsl-env.sh la
   ## normaliza al SDK de Linux (~/Android/Sdk) al arrancar Gradle.
   sdk.dir=

   ## Clave del chat (OpenCode Go): generador, Busqueda con IA y lote de sugerencias.
   ## Si falta o esta vacia, la app degrada en silencio a modo semilla y el log muestra MISSING_KEY.
   opencode.apiKey=

   ## Clave de la imagen de plato (DevExpert). Si falta o esta vacia, la tarjeta cae a la
   ## ficha tipografica (sin imagen generada).
   devexpert.apiKey=
   ```

   Debe quedar **rastreado** (`git add local.properties.example`); el patrón `local.properties` de
   `.gitignore:5` no lo captura (ya verificado). No se añade ninguna regla a `.gitignore`.

2. **Aviso en `scripts/new-worktree.sh`, dentro del bloque `local.properties` (líneas 67-75)**, justo
   después del `echo "local.properties copiado a ..."`, para que se evalúe el fichero **resultante**
   (tanto si el worktree se acaba de crear como si se reutiliza, porque la copia ocurre en ambos casos):

   - Helper **puro y silencioso** que devuelve el valor sin imprimirlo nunca, robusto bajo
     `set -euo pipefail` (un `grep` sin coincidencia debe llevar `|| true` o ir en un `if`), sin `cat`
     del fichero:

     ```bash
     # Valor de una propiedad de un local.properties. Vacio si no existe o esta en blanco.
     # No imprime nunca el contenido del fichero. Ultima aparicion gana (regla de .properties).
     sazon_props_value() { # <fichero> <propiedad>
       local file="$1" key="$2" line=""
       line="$(grep -E "^[[:space:]]*${key}[[:space:]]*=" "$file" 2>/dev/null | tail -n 1 || true)"
       line="${line#*=}"
       printf '%s' "${line%$'\r'}" | sed -E 's/^[[:space:]]+//; s/[[:space:]]+$//'
     }
     ```

     El anclaje `^[[:space:]]*` deja fuera las líneas comentadas (`#opencode.apiKey=...`); `${line#*=}`
     toma lo que sigue al **primer** `=`; `${line%$'\r'}` tolera CRLF (Android Studio en Windows) y el
     `sed` recorta espacios. Valor vacío ⇒ propiedad **ausente** (mismo efecto que
     `getProperty(..., "")`). El `.` de la clave se pasa tal cual (en ERE casaría cualquier carácter,
     inocuo en un `local.properties`; si se quiere exactitud literal, usar `opencode\.apiKey`).
   - Dos avisos independientes, uno por clave, **solo** si el valor está vacío. Tokens exigidos para
     que la verificación sea determinista: el del chat contiene `opencode.apiKey` y `MISSING_KEY`; el de
     la imagen contiene `devexpert.apiKey`. Texto sugerido:
     `AVISO: ${worktree_path}/local.properties no define opencode.apiKey; el chat de IA (generador, busqueda y lote) caera en MISSING_KEY y la app usara el modo semilla. Rellena la clave en ${repo_root}/local.properties (ver local.properties.example).`
     `AVISO: ${worktree_path}/local.properties no define devexpert.apiKey; la imagen de plato generada caera a la ficha tipografica. Rellena la clave en ${repo_root}/local.properties (ver local.properties.example).`
   - **Nada más cambia:** los avisos van a stderr; stdout conserva sus líneas y su orden; no hay exit
     distinto de 0; no se pide entrada por stdin; no se escribe en ningún fichero; no se imprime ningún
     valor de clave. La rama `elif [ -n "${ANDROID_HOME:-}" ]` y la rama `else` (sin fichero) quedan
     **sin tocar**.
   - Comentario de cabecera (líneas 8-9) actualizado: además de sembrar `local.properties`, el script
     avisa si le faltan las claves de IA.

3. **Documentación durable**:
   - `AGENTS.md`: paso explícito en `Flujo de arranque` (23-31) para un clon nuevo — copiar
     `local.properties.example` a `local.properties` y rellenar `sdk.dir`, `opencode.apiKey` y
     `devexpert.apiKey`, recordando que las claves no están en git y se pasan aparte; ampliar la viñeta
     de `local.properties` (213-215) y la de worktrees (225-230) con el aviso nuevo.
   - `docs/technical-discovery.md`: fila `| Claves de IA |` (53) con la plantilla versionada
     `local.properties.example` (sin secretos) y el detalle de la degradación; viñeta de
     *Deployment and Operations* (1054-1059) con el paso manual al clonar y el aviso del helper.

## Expected File Changes

- `local.properties.example` — **crear** (raíz del repo); plantilla sin secretos, versionada.
- `scripts/new-worktree.sh` — **modificar**; helper de lectura + dos avisos en el bloque
  `local.properties` + comentario de cabecera. Único cambio de código.
- `AGENTS.md` — **modificar**; paso al clonar y precisión en las viñetas de `local.properties` y de
  worktrees.
- `docs/technical-discovery.md` — **modificar**; fila de claves de IA y viñeta de despliegue.
- `docs/specs/ai-keys-onboarding-guard.md` — crear (este documento).
- `PROGRESS.md` — actualizar por el implementer (sesión, verificación y evidencia).
- `feature_list.json` — actualizar por el implementer: `status: passing` y `evidence` con los
  resultados (nunca `accepted`).

No se tocan: `app/**`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, el manifiesto, permisos,
`init.sh`, el resto de `scripts/`, `local.properties`, `docs/domain-model.md`,
`docs/risks-and-open-questions.md`, `DESIGN.md` ni `docs/design/**`.

## Visual Design Impact

- UI involved: no.
- Design source: not applicable (`DESIGN.md` no aplica).
- Screens or states affected: ninguno (la app y su modo semilla quedan intactos).
- New design artifact required: no.

## Durable Documentation Impact

- `AGENTS.md`: **update** — cambia el camino de arranque en un clon nuevo (una regla operativa
  repo-wide) y el comportamiento del helper de worktrees.
- `docs/technical-discovery.md`: **update** — dónde viven las claves y qué degrada si faltan es un
  límite durable de *Data and Storage* / *Deployment and Operations*.
- `docs/domain-model.md`: **not needed** — sin entidades, relaciones ni estados de dominio.
- `docs/risks-and-open-questions.md`: **not needed** — la degradación silenciosa no cambia y el riesgo
  de la clave en `BuildConfig` ya está documentado; esta correctiva reduce el fallo de configuración,
  no añade una incógnita.
- `CONTEXT.md` / `DESIGN.md` / `docs/design/**`: **not needed** — sin vocabulario, UI ni visuales
  nuevos.
- `docs/adr/`: **not needed** — no hay decisión de arquitectura nueva; el patrón «clave en
  `local.properties` fuera de git» ya está decidido.

## Implementation Plan

1. Crear `local.properties.example` en la raíz con el contenido del `Technical Approach` y añadirlo a
   git (`git add local.properties.example`); comprobar que no está ignorado.
2. Abrir `scripts/new-worktree.sh` y añadir el helper `sazon_props_value` (antes del bloque de
   `local.properties`, junto a las funciones/comentarios del script) y los dos avisos dentro del `if
   [ -f "${repo_root}/local.properties" ]`, tras el `echo` de la copia.
3. Actualizar el comentario de cabecera del script (qué hace con las claves).
4. Ejecutar `bash -n scripts/new-worktree.sh` y el arnés determinista de la sección `Verification Plan`
   (positivo, negativo y bordes).
5. Actualizar `AGENTS.md` (flujo de arranque + viñetas) y `docs/technical-discovery.md` (fila de
   claves + despliegue).
6. Ejecutar `bash init.sh` (test + lint) y registrar la evidencia real en `PROGRESS.md` y
   `feature_list.json` (estado `passing`).

## Implementation Tasks

- [ ] Crear `local.properties.example` sin secretos (3 propiedades, valores vacíos, comentarios) y
      dejarlo **rastreado**.
- [ ] Añadir el helper `sazon_props_value` robusto bajo `set -euo pipefail` (sin imprimir valores).
- [ ] Emitir el aviso de `opencode.apiKey` (con `MISSING_KEY`) cuando el valor está vacío.
- [ ] Emitir el aviso de `devexpert.apiKey` (ficha tipográfica) cuando el valor está vacío.
- [ ] Verificar que stdout, códigos de salida y ramas `ANDROID_HOME`/sin fichero quedan intactos.
- [ ] Actualizar el comentario de cabecera de `scripts/new-worktree.sh`.
- [ ] Pasar `bash -n scripts/new-worktree.sh`.
- [ ] Ejecutar el arnés determinista: negativo (sin `opencode.apiKey`), positivo (con las dos), borde
      (valor vacío) y borde (solo falta `devexpert.apiKey`).
- [ ] Actualizar `AGENTS.md` y `docs/technical-discovery.md`.
- [ ] Ejecutar `bash init.sh` y anotar la evidencia.
- [ ] Actualizar `PROGRESS.md` y `feature_list.json` (estado `passing`, no `accepted`).

## Verification Plan

### 1. Sintaxis y gate del repo

```bash
bash -n scripts/new-worktree.sh     # exit 0
bash init.sh                        # exit 0: ./gradlew test + ./gradlew lint, sin procesos largos
```

`init.sh` **no** se modifica: sigue ejecutando el gate no bloqueante en WSL y **no** arranca servicios
ni emuladores. No hay dependencias nuevas. `shellcheck` no está instalado en el entorno: `bash -n` es
la comprobación estática disponible.

### 2. Arnés determinista (verificación exigida: con y sin `opencode.apiKey`)

Se ejecuta el script **real** del checkout de la feature sobre un repositorio git desechable en `/tmp`,
sin SDK ni Gradle. Dos precauciones: `SAZON_WSL_LOCAL_PROPS=0` para que `wsl-env.sh` **no** reescriba el
`local.properties` real, y el `base-sha` como **segundo argumento** para que el script no haga `fetch`.

```bash
set -uo pipefail
repo="$(git rev-parse --show-toplevel)"
scratch="$(mktemp -d /tmp/sazon-ai-keys.XXXXXX)"
git init -q -b main "$scratch/repo"
git -C "$scratch/repo" -c user.name=t -c user.email=t@example.com commit -q --allow-empty -m init
sha="$(git -C "$scratch/repo" rev-parse HEAD)"

run_case() { # <nombre> <contenido de local.properties>
  rm -rf "$scratch/worktrees"
  printf '%b' "$2" > "$scratch/repo/local.properties"
  ( cd "$scratch/repo" && \
    SAZON_WSL_LOCAL_PROPS=0 WORKTREES_ROOT="$scratch/worktrees" \
    bash "$repo/scripts/new-worktree.sh" "demo-$1" "$sha" ) \
    >"$scratch/$1.out" 2>"$scratch/$1.err"
  echo "$?" > "$scratch/$1.rc"
}

# 1) Negativo: solo sdk.dir -> avisa del chat (y tambien de la imagen, que no esta).
run_case sin_chat 'sdk.dir=/home/u/Android/Sdk\n'
# 2) Borde: propiedad presente y vacia -> mismo aviso del chat, aislado.
run_case vacia 'sdk.dir=/home/u/Android/Sdk\ndevexpert.apiKey=dummy-img\nopencode.apiKey=\n'
# 3) Positivo: las dos claves -> sin avisos de clave.
run_case con_claves 'sdk.dir=/home/u/Android/Sdk\nopencode.apiKey=dummy-chat\ndevexpert.apiKey=dummy-img\n'
# 4) Borde: solo falta la de la imagen.
run_case sin_imagen 'sdk.dir=/home/u/Android/Sdk\nopencode.apiKey=dummy-chat\n'

# Contrato y contenidos (set -e: el primer aserto que falle aborta con exit != 0):
set -e
[ "$(cat "$scratch"/sin_chat.rc)" = 0 ] && [ "$(cat "$scratch"/vacia.rc)" = 0 ]
[ "$(cat "$scratch"/con_claves.rc)" = 0 ] && [ "$(cat "$scratch"/sin_imagen.rc)" = 0 ]
grep -q 'opencode.apiKey'   "$scratch/sin_chat.err" && grep -q 'MISSING_KEY' "$scratch/sin_chat.err"
grep -q 'devexpert.apiKey'  "$scratch/sin_chat.err"   # caso 1: tambien falta la de la imagen
grep -q 'opencode.apiKey'   "$scratch/vacia.err"    && grep -q 'MISSING_KEY' "$scratch/vacia.err"
! grep -q 'devexpert.apiKey' "$scratch/vacia.err"     # borde aislado: no avisa de la imagen
! grep -q 'opencode.apiKey\|devexpert.apiKey' "$scratch/con_claves.err"  # positivo: ni un aviso
grep -q 'devexpert.apiKey'  "$scratch/sin_imagen.err"             # borde: aviso de imagen
! grep -q 'opencode.apiKey' "$scratch/sin_imagen.err"             # ...y ninguno del chat
# Sin fugas de valores y sin cambio de stdout:
! grep -q 'dummy-chat\|dummy-img' "$scratch"/*.err "$scratch"/*.out
grep -q 'local.properties copiado a ' "$scratch/con_claves.out"
grep -q 'worktree: ' "$scratch/con_claves.out"
[ -f "$scratch/worktrees/demo-con_claves/local.properties" ]
rm -rf "$scratch"
echo "ARNES OK: 4 casos, avisos de clave donde toca"
```

> **Hallazgo del implementer (2026-10-19)**: tal cual está escrito, el aserto final
> `[ -f "$scratch/worktrees/demo-con_claves/local.properties" ]` falla porque el último
> `run_case` (`sin_imagen`) empieza con `rm -rf "$scratch/worktrees"` y borra el worktree de
> `con_claves`. No es un fallo de la implementación: ejecutando **los mismos cuatro casos con
> `run_case con_claves` al final** (todos los demás asertos idénticos) el arnés imprime
> `ARNES OK` y sale 0, y la copia del `local.properties` se comprobó también justo después del
> caso positivo. La evidencia de `PROGRESS.md` registra esa tirada.

Resultado esperado: los cuatro casos con exit 0; avisos exactamente donde toca (chat en 1 y 2, imagen
solo en 4, ninguno en 3); ningún valor de clave en stdout/stderr; stdout y copia de `local.properties`
intactos. Los `AVISO` de `wsl-env.sh` sobre el entorno (JDK/SDK ausentes) son ajenos: por eso el caso
positivo se comprueba por el **nombre de la propiedad** y no por la palabra `AVISO`. Las líneas
`Preparing worktree (...)` (stderr) y `HEAD is now at ...` (stdout) las imprime `git worktree add`, no
el script: las comprobaciones se hacen contra las líneas propias (`local.properties copiado a ...`,
`worktree: ...`).

Estado **antes** de implementar el aviso (comprobado al planificar): los cuatro casos ya devuelven
exit 0 y crean el worktree, y el arnés **falla** (exit 1) en el primer aserto que exige el aviso de
`opencode.apiKey`/`MISSING_KEY`; es decir, el arnés discrimina el cambio y no pasa en vacío. Con el
aviso implementado debe imprimir `ARNES OK` y salir 0. La ejecución no toca el `local.properties` real
(`SAZON_WSL_LOCAL_PROPS=0`; `git status` limpio para él).

### 3. Plantilla y contrato del script (inspecciones)

```bash
git ls-files --error-unmatch local.properties.example                 # versionado
git check-ignore -v --no-index local.properties.example; echo $?      # 1 (NO ignorado)
grep -nE '^(sdk\.dir|opencode\.apiKey|devexpert\.apiKey)=$' local.properties.example  # 3 aciertos
grep -cE '^(sdk\.dir|opencode\.apiKey|devexpert\.apiKey)=.+' local.properties.example # 0
grep -cE 'oc_sk_|sk-[A-Za-z0-9]' local.properties.example                             # 0
git diff -- scripts/new-worktree.sh   # solo el helper, los dos avisos y la cabecera
```

Caso 6 (sin regresión) se ejercita con `env -u ANDROID_HOME -u ANDROID_SDK_ROOT
SAZON_WSL_ANDROID_SDK=/nonexistent SAZON_WSL_LOCAL_PROPS=0`: sin `local.properties` en el repo
desechable debe seguir imprimiéndose `AVISO: no hay local.properties ni ANDROID_HOME; el build puede
fallar`, y una segunda ejecución debe decir `worktree ya existe` con exit 0 (los `AVISO` de `wsl-env.sh`
sobre el SDK son esperados y ajenos).

### 4. Alcance

```bash
git diff --stat   # local.properties.example (nuevo), scripts/new-worktree.sh, AGENTS.md,
                  # docs/technical-discovery.md y los ficheros de estado
```

No hay E2E: el repo **no** tiene arnés E2E (la UI se verifica por lógica/estado en JVM y contra
`DESIGN.md`, `AGENTS.md`), y esta feature no cambia ningún flujo de usuario, autenticación ni ruta de
la app; el equivalente reproducible es el arnés bash de la sección 2, que ejercita el script real.

## Evidence To Capture

En `feature_list.json` (`evidence`) y `PROGRESS.md`:

- `bash -n scripts/new-worktree.sh` → exit 0 y `bash init.sh` → exit 0 (test + lint; si
  `testDebugUnitTest` sale `UP-TO-DATE`, forzar con `bash scripts/gradle.sh testDebugUnitTest
  --rerun-tasks` para tener evidencia fresca), sin dependencias nuevas ni cambios en `init.sh`.
- Salida del arnés: los cuatro códigos de retorno, presencia/ausencia de `AVISO`/`MISSING_KEY`/
  `devexpert.apiKey` por caso, ausencia de `dummy-chat`/`dummy-img` en stdout y stderr, y la copia
  efectiva del fichero.
- Inspecciones de la plantilla: `git ls-files` (rastreada), `git check-ignore` exit 1 (no ignorada),
  3 propiedades con valor vacío y 0 valores no vacíos.
- `git diff --stat` con los ficheros previstos y **sin** cambios en `app/**`, `init.sh`, el resto de
  `scripts/`, build/lockfile, manifiesto, permisos ni `local.properties`.
- Fecha, rama y cualquier desviación (p. ej. `ANDROID_HOME` presente en el entorno) con su motivo.

## Validator Checklist

- [ ] La implementación se limita a `local.properties.example`, `scripts/new-worktree.sh` y los docs
      indicados; no toca la app, el build, `init.sh`, `local.properties` ni otros scripts.
- [ ] `local.properties.example` está rastreado, **no** está ignorado y no contiene ningún valor de
      clave (solo nombres y valores vacíos).
- [ ] Los escenarios 2, 3, 4 y 5 pasan: aviso con `MISSING_KEY` si falta o está vacía
      `opencode.apiKey`, aviso específico si falta `devexpert.apiKey`, y **ningún** aviso de clave con
      las dos rellenas.
- [ ] El aviso va **solo** a stderr, contiene el nombre de la propiedad y su consecuencia, no imprime
      ningún valor de clave y **no** cambia el código de salida (0) ni la creación/reutilización del
      worktree.
- [ ] Se conservan los avisos y ramas previos (sin `local.properties` ni `ANDROID_HOME`; `ANDROID_HOME`;
      `worktree ya existe`), stdout y los códigos 0/2.
- [ ] Escenario 7: `AGENTS.md` tiene el paso al clonar y `docs/technical-discovery.md` dice dónde viven
      las claves y qué degrada si faltan.
- [ ] La evidencia de `feature_list.json`/`PROGRESS.md` es real, fresca y reproducible con los comandos
      de esta spec; el estado máximo fijado por el implementer es `passing`.
- [ ] No hay comportamiento de producto extra, features vecinas ni dependencias nuevas.

## Open Questions / Unknowns

- Ninguna bloqueante. Decisión fijada aquí: una propiedad **presente pero vacía** cuenta como ausente
  (es exactamente el caso que produce `BuildConfig.OPENCODE_API_KEY == ""` y el `MISSING_KEY`
  reportado); si la persona prefiriera distinguir «no está» de «está vacía», sería un cambio de
  microcopy del aviso, no de comportamiento.
- El aviso **no** bloquea ni rellena nada: si en el futuro se quisiera fallar el arranque sin claves,
  sería otra decisión (y otra feature), porque rompería el modo semilla por diseño.
- `sdk.dir` se documenta en la plantilla pero **no** se valida en el script: su ausencia ya se cubre con
  el aviso existente de `ANDROID_HOME`/Gradle y validarlo excedería el alcance acordado.
- El aviso solo actúa en el camino de `new-worktree.sh`; un clon sin worktree se cubre por la plantilla
  y por `AGENTS.md`, no por el script (acordado con la persona).
