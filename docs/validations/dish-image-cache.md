# Validación independiente — `dish-image-cache`

- **Feature**: `dish-image-cache` (`area: data`, fase 3 de `docs/ai-dish-images-proposal.md`)
- **Spec**: `docs/specs/dish-image-cache.md`
- **Rama / worktree**: `feature/dish-image-cache`, `/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/dish-image-cache`
- **Sesión**: 091 (agente validador con contexto limpio, independiente del implementer)
- **Estado previo**: `passing` (misma sesión, trabajo en disco; `HEAD = 9b67788` = `origin/main`, sin commit)
- **Veredicto**: **`accept`**

## Qué se re-ejecutó (sin fiarse del handoff)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (test + lint vía `scripts/gradle.sh`, sin procesos largos); la primera pasada salió `UP-TO-DATE` y se forzó |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | exit 0; **77 suites / 551 tests / 0 fallos / 0 errores / 0 saltados** (parseo propio de los XML) |
| Tirada focal | `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.image.*"` | exit 0; **5 suites / 34 tests / 0 fallos / 0 errores / 0 saltados** |
| Baseline re-derivada | `git grep -c '@Test' HEAD -- app/src/test` | `HEAD`: **72 ficheros / 517 tests** ⇒ delta exacto **+5 suites / +34 tests**, cuadra con el run |
| Lint | `bash scripts/gradle.sh lint --rerun-tasks` + parseo del informe | exit 0; **0 errores, 13 warnings** preexistentes (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1), **0 bajo `app/src`** |
| Pureza | grep de imports en `data/image` | `import android` **solo** en `AndroidDishImageCache.kt`; `androidx\.\|Recipe\|Taxonomy\|DishImagePrompt\|DishImageCacheKey\|Log\.\|filesDir\|getDatabasePath\|graphics` → **0** |
| Alcance | `git status --porcelain -uall` | solo los 5 `main` + 5 `test` de `data/image`, `docs/specs/dish-image-cache.md`, 3 docs y `feature_list.json`/`PROGRESS.md`; `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `domain/**`, `data/ai/image/**`, cadena `recipe-photos`, `ui/**`, `assets/**`, `DESIGN.md` y `docs/design/**` **sin diff**; `docs/design/concepts/dish-image-cache` **no existe** (el gate de capturas no aplica: `area: data`) |

Nota informativa: mi rerun de `lint` da **13** warnings (la spec citaba 13; el implementer reportó 12 con nota
honesta de desviación porque los avisos de "versión nueva disponible" dependen de la metadata del momento).
La afirmación material —0 errores, ninguno en `app/src`, ninguno causado por la feature— se sostiene.

## Escenarios verificados (contra el código y los tests reales)

1. **Reutilización por clave (aciertos)**: la segunda petición con la misma clave canónica invoca al
   generador **una sola vez**; en disco queda **un único** fichero. No es vacuo (contadores exactos del
   generador y bytes distintos para detectar falsos aciertos).
2. **Sin red desde disco**: con memoria fría (resolutor nuevo) se sirve del disco sin generar; con generador
   `null` (sin red) se sirve del disco y una clave sin copia devuelve `null` **sin crear fichero**.
3. **Títulos equivalentes**: `DishImageCacheKey.of('Arroz con Pollo Criollo') == of('  arroz  con   pollo criollo ')`
   → un solo fichero y una generación por clave distinta.
4. **Formato/nombre**: el fichero es `File(root, '<cacheKey>.webp')` con bytes **idénticos** a los del generador
   y cabecera `RIFF…WEBP` (la caché **no** recodifica).
5. **Escritura atómica**: temporal en la misma raíz + `renameTo` + limpieza; el fichero solo es servible
   completo.
6. **Evicción con tope inyectado**: con 300 B y tres ficheros con `lastModified` crecientes, escribir un cuarto
   borra el más antiguo; el recién escrito y los más recientes sobreviven y `totalBytes() <= 300`. La evicción
   **no sale de su raíz** ni toca otra extensión (un `.webp` y un `.db` en el directorio padre y un `notas.txt`
   dentro quedan intactos) y solo borra `<clave>.webp`.
7. **LRU real**: `read` toca `lastModified` (si el sistema no lo soportara, degrada a FIFO, aceptado).
8. **LRU de memoria**: tope 2 descarta la menos usada recientemente (un `get` rejuvenece) y una clave
   descartada de memoria se sirve del disco con el contador del generador intacto.
9. **Fallos**: generador `null`, array vacío o `IllegalStateException` → `imageFor` devuelve `null` sin lanzar,
   sin entrada de memoria y sin fichero ni temporales residuales.
10. **Cancelación**: `CancellationException` se relanza (no se convierte en `null`).
11. **Single-flight**: dos peticiones concurrentes de la misma clave pagan **una** generación (`Mutex` por
    clave) y con claves distintas, una por clave.
12. **I/O que nunca lanza**: `write` devuelve `false` y `read` `null` con una raíz no utilizable; si no se
    puede persistir, `imageFor` devuelve igualmente los bytes generados (*best-effort*).
13. **Costura Android** (Robolectric `@Config(sdk = [33])`, JVM sin emulador): `AndroidDishImageCache.root(context)
    == File(context.cacheDir, 'dish-images')` y una resolución real escribe bajo `cacheDir`, **nunca** bajo
    `filesDir` (`filesDir/dish-images` no existe tras la resolución).

## Non-goals respetados

Sin UI ni cableado (la cadena de `recipe-photos` sigue siendo la única resolución en runtime; la fase 4
inserta este nivel delante), sin Room, sin códec ni `android.graphics`, sin `dishKey`/`imagePrompt` en el
wire, sin set semilla, sin dependencias nuevas y sin tocar `app/build.gradle.kts`/`gradle/libs.versions.toml`/
`init.sh`/`scripts/**`/`domain.image`/`data.ai.image`.

## Evaluación de seguridad

Sin hallazgos bloqueantes ni de riesgo alto. No hay red, ni claves, ni `INTERNET`, ni almacenamiento externo;
la raíz es `cacheDir` privado de la app (excluye explícitamente `filesDir`/Room); sin `Log` ni telemetría;
contrato de fallo `null`/no-excepción con cancelación propagada (coherente con ADR 0005); escritura atómica y
evicción confinada a su subdirectorio y extensión.

## Hallazgos por severidad

**Critical / High / Medium: ninguno.**

**Low (no bloquean la aceptación; candidatos a endurecimiento en fase 4):**

1. `DishImageResolver.locks` crea una `Mutex` por `cacheKey` y nunca la elimina (cota natural = nº de platos
   vistos en el proceso). Inocuo hoy; conviene podar si en fase 4 las claves dejaran de ser un conjunto pequeño.
2. `entries()`/`evictionVictims` solo cuentan `<clave>.webp`: un `.tmp` huérfano de un *crash* no se mide ni se
   evicta (acotado; `cacheDir` es descartable).
3. `cacheKey` se usa **literal** como nombre de fichero. Es seguro por construcción (`DishImageCacheKey` garantiza
   alfabeto `[a-z0-9-]` + `dish-image-v1-`), pero `DishImageRequest` es `data class` público: una petición
   fabricada a mano con separadores podría escapar de `root`. Sin ruta de explotación hoy; validar/documentar el
   invariante al cablear en fase 4.
4. La evicción solo se dispara en `write`; ficheros introducidos por otra vía se *reapean* en la siguiente
   escritura (irrelevante hoy; relevante para el set semilla de fase 7).
5. Informativo: el rerun de `lint` da 13 warnings (la spec citaba 13; el implementer 12), sin cambio material.

## Documentación y harness

- `docs/technical-discovery.md`: fila nueva en la tabla **Data and Storage** + bullet de la fase 3 + tests JVM
  de `data.image`; mantiene explícito que nadie consume la caché.
- `docs/adr/0005-ai-dish-images.md`: estado **"Aceptado parcialmente (fases 1-3)"**, alcance de la fase 3 y
  alternativas descartadas (Room vs ficheros —resuelve §11.2— y LRU de bitmaps vs de bytes).
- `docs/risks-and-open-questions.md`: la mitigación de crecimiento de almacenamiento pasa a **implementada**
  (tope + evicción); el gasto de cuota sigue sin materializarse hasta la fase 4.
- `AGENTS.md`, `DESIGN.md`, `docs/domain-model.md`, `docs/domain/taxonomy.md`: correctamente **no** tocados.
- `feature_list.json` (estado `passing` con evidencia) y `PROGRESS.md` (sesión 091) coherentes; reinicio desde
  el camino estándar reproducido (`bash init.sh` verde). Sin commits/push/PR.
