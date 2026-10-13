# Validación independiente — `dish-image-domain`

- **Feature**: `dish-image-domain` (`area: domain`, fase 1 de `docs/ai-dish-images-proposal.md`)
- **Spec**: `docs/specs/dish-image-domain.md`
- **Rama / worktree**: `feature/dish-image-domain`, `/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/dish-image-domain`
- **Sesión**: 086 (agente validador con contexto limpio, independiente del implementer)
- **Estado previo**: `passing` (sesión 085, trabajo en disco sin commit; `HEAD = a0025b7`)
- **Veredicto**: **`accept`**

## Qué se re-ejecutó (sin fiarse del handoff)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | exit 0, pero todo `UP-TO-DATE` (config cache) → insuficiente por sí solo |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | exit 0, `BUILD SUCCESSFUL`, 34 tareas ejecutadas |
| Parseo propio de XML | `app/build/test-results/testDebugUnitTest/TEST-*.xml` | **68 suites / 496 tests / 0 fallos / 0 errores / 0 saltados**; nuevas: `DishImageCacheKeyTest` 8, `DishImagePromptTest` 7, `DishImageRequestTest` 5 = 20 |
| Baseline re-derivada | `git grep -c "@Test" HEAD -- app/src/test` | 65 ficheros / 476 anotaciones en `HEAD`; 476 + 20 = 496 ✓ |
| Lint | gate + parseo propio del SARIF | **0 errores, 13 warnings** preexistentes, **0 bajo `app/src`** |
| Pureza | grep de imports en `domain/image` | solo `Recipe`, `Taxonomy`, `java.text.Normalizer`, `java.util.Locale`; 0 `android`/`androidx`/`io.ktor`/`java.net`/`random`/`UUID`/`currentTimeMillis` |
| No-consumo del puerto | grep `DishImage` fuera de `domain/image` | 0 referencias externas, 0 implementaciones (fase 2 intacta) |
| Alcance | `git status` / `git diff --name-only` | solo los 9 ficheros previstos + spec, `feature_list.json` y `PROGRESS.md`; build/lockfile/`init.sh`/`scripts/**`/`data/**`/`ui/**`/dominio existente/Taxonomía/`seed-recipes.json`/`assets/**`/`DESIGN.md`/`docs/design/**` intactos; nada en `docs/design/concepts/` |
| Escenarios 1-9 | lectura de los tests + tirada forzada verde | todos cubiertos, incluidos los negativos |

Evidencia intermedia inspeccionada: la tirada focal de las 09:53 muestra `20 tests completed, 1 failed`
(aserción de pureza, `DishImagePromptTest.kt:134`) y otra verde a las 09:54; iteración TDD normal, sin
tests debilitados (el estado final pasa los greps independientes de pureza).

## Escenarios verificados

1. Prompt determinista, no vacío y con estilo fijo.
2. Sin título → el plato se describe por la etiqueta real de la categoría.
3. Receta incompleta (título/categoría/cocina vacíos) y `Taxonomy` vacía → prompt por defecto, sin lanzar.
4. Cocina desconocida/vacía → cláusula de cocina omitida (sin "desconocida").
5. `normalize` cubre mayúsculas, diacríticos, puntuación y espacios (`Salmón a la plancha` →
   `dish-image-v1-salmon-a-la-plancha`).
6. Clave estable y discriminante; el `id`/`source` no influyen cuando hay título.
7. Sin título → respaldo por `id` (`dish-image-v1-plato-seed-tortilla-patatas`) y respaldo final
   (`dish-image-v1-plato`), sin colisión ni clave vacía.
8. Frontera deliberada: normalización tipográfica, no semántica (`Arroz con pollo` ≠ `Arroz con pollo fácil`).
9. `DishImageRequest.of(taxonomy, recipe)` compone las tres piezas, `data class` con igualdad estructural
   y `recipeId = null` con `id` vacío.

Se confirmó además que la clave es independiente del idioma del dispositivo (`Locale.setDefault("tr-TR")`
→ `İSTANBUL` = `istanbul`, por el `Locale.ROOT` explícito).

## Hallazgos

**Critical / High / Medium: ninguno.**

- **L1 (Low, nota de endurecimiento, sin reparación obligatoria)** — `DishImagePrompt` solo colapsa
  espacios en el título; un título que acabe en coma (`"Tortilla,"`) produciría `… de Tortilla,, cerámica…`.
  Ningún escenario lo dispara y la plantilla es literalmente la que prescribe la spec. Sugerencia para la
  fase 2/4: `trim(',')` del plato o un test explícito del caso.
- **L2 (Low)** — `docs/adr/0004-recipe-photos-as-assets.md` no lleva puntero a 0005. No hay contradicción
  (la afirmación "sin red en runtime" sigue siendo cierta para la cadena local). Añadirlo cuando la fase 4
  inserte la generación delante.
- **L3 (Low)** — La spec (fichero del planner) declara una baseline "64 suites / 456 tests" que no coincide
  con `HEAD` de la rama (65 / 476, verificado). El implementer ya lo documentó; no bloquea.

## Seguridad (checklist de la feature)

Sin hallazgos ni notas bloqueantes: no hay secretos, ni red, ni Ktor, ni llamadas externas, ni datos
sensibles en logs o errores. El único dato derivado de la entrada es el slug de la clave, restringido a
`[a-z0-9-]` (sin `/`, `.` ni traversal), propiedad positiva para la fase 3 cuando se use como nombre de
fichero o columna; `recipeId` es solo trazabilidad y no interviene en la clave. Sin parsing de payloads y
sin dependencias nuevas.

## Documentación y harness

- `docs/adr/0005-ai-dish-images.md` existe con estado "Aceptado parcialmente (fase 1)", ajusta parcialmente
  el 0004 sin contradecirlo y enumera las fases 2-7 y la desviación consciente.
- `docs/technical-discovery.md` (puerto `domain.image` + tests JVM) y `docs/risks-and-open-questions.md`
  (cuota ~2 %/imagen, latencia ~4,2 s, almacenamiento; ADR 0005 en la lista) actualizados y coherentes con
  el código.
- `init.sh` ejecuta el gate real (`test` + `lint` vía `scripts/gradle.sh`), termina exit 0 en verde y sale
  ≠0 si un check falla; no arranca procesos largos.
- `PROGRESS.md` (sesión 085) y `feature_list.json` reflejaban el estado real.

## Conclusión

La feature cumple su spec, los escenarios (incluidos los negativos obligatorios) están cubiertos y en
verde, la disciplina de alcance y la pureza se sostienen y la documentación durable es coherente.
**Veredicto: `accept`.** Sin trabajo de reparación requerido. La verificación manual/E2E no aplica (la
feature no cambia comportamiento observable: sin UI, sin red, sin persistencia y sin consumidores del
puerto).
