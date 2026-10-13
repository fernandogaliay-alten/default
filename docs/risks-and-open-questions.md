# Risks and Open Questions

## Blocking Next Phase

Ninguno. El harness (`AGENTS.md`, `init.sh`, `PROGRESS.md`, `feature_list.json`) y las features de
dominio, persistencia y UI pueden derivarse ya. **No quedan preguntas de implementación abiertas**: la
última (la 1, errores y reintentos de la IA) la resolvió `real-ai-provider`.

## Implementation-Time Questions

Los números son **posicionales**. Al resolver una pregunta y retirarla de la tabla, el resto se
renumera y hay que actualizar las citas que existan en otros documentos del repositorio.

| # | Pregunta | Impacto | Cuándo decidir |
|---|---|---|---|

Nota: **no quedan preguntas de implementación abiertas**. La última —la **1** (errores y reintentos
de la IA)— la resolvió `real-ai-provider`.

Nota de renumeración: se han resuelto el proveedor de IA (antes 1), el nº de recetas por generación
(antes 2), la caducidad del lote (antes 3), `matchScore` (antes 4), el formato de las recetas semilla
(antes 7) y, por último, los errores y reintentos de la IA (la 1). Las antiguas 5 y 6 pasaron a ser 1
y 2 en su momento. Las specs históricas conservan las citas con la numeración de su momento; los
números eran **posicionales** y no se renumeran retroactivamente.

### Resueltas

- **Errores y reintentos de la IA** (era la pregunta 1): resuelta en `real-ai-provider`, con los
  timeouts recalibrados por `ai-provider-timeouts` y, después, por `ai-call-diagnostics` (issue #72)
  contra la latencia real de **varios** modelos. La política es fija y vive en un **único** sitio,
  `com.sazon.app.data.ai.AiProviderPolicy`: connect 10 s, socket 120 s, timeout por intento 150 s y
  deadline total 240 s (calibrados contra el peor caso medido: `mimo-v2.6-pro` en 115,8 s; el alias
  diario `chat` mide 28,0–46,6 s y `chat-pro`+`json_schema` 97,0 s);
  máximo **1 reintento** (2 intentos) solo ante timeout/`IOException`/408/429/5xx, con backoff fijo de
  500 ms y `Retry-After` respetado solo si ≤ 5 s; **nunca** se reintentan 400/401/403/404/422. Con el
  `response_format` activo, un `400`/`422` **con presupuesto de intentos** (es decir, en el 1.er intento)
  dispara el **fallback** de formato: un único reintento sin la
  clave, camino **aparte** del transporte (`isRetryableStatus` no cambia), sin backoff y consumiendo el
  2.º intento (`ai-response-format`, issue #74). El `400`/`422` del 2.º intento ya no tiene presupuesto:
  degrada a `HTTP_ERROR` sin una 3.ª petición (la operación nunca supera `MAX_ATTEMPTS = 2`). El fallo
  de transporte se clasifica por su causa: timeout de socket o de petición → `TIMEOUT` (el modelo tardó
  en responder), conexión/red → `NETWORK`. Un 200 ilegible o vacío **no** lanza: devuelve
  las recetas decodificables (posiblemente vacías) y la pantalla muestra su estado vacío con
  "Reintentar". El diagnóstico local (`AiCallEvent` + `Log.d("SazonAi", …)`) explica cada llamada con
  estado, intento, duración, el modo de formato (`format=JSON_OBJECT`/`NONE`) y si saltó el fallback
  (`FALLBACK`), longitudes del contenido (`content=…c/…b`), `received`/`accepted` y el
  resumen de motivos de descarte (`discards={…}`), sin exponer nunca la clave ni los cuerpos. El
  resumen se **desglosa** además en `incoherent={TIME_DRIFT=…, ALLERGEN_COVERAGE=…}` para decir cuál
  de las dos reglas de coherencia del Validador falló (4a deriva de tiempos vs. 4b `declaredAllergens`
  incompletos; correctiva `ai-verbose-diagnostics`); el segmento solo aparece si hay causas. Cuota
  agotada (429 sostenido) y clave ausente degradan a **modo semilla** sin
  microcopy específico de cuota. Ver `docs/specs/real-ai-provider.md`,
  `docs/specs/ai-provider-timeouts.md`, `docs/specs/ai-call-diagnostics.md` y
  `docs/specs/ai-verbose-diagnostics.md`.
- **Política de trazas** (antes pregunta 5, la de trazas): resuelta en `recipe-validator`. Se aplica
  tal cual la política conservadora de `docs/domain/taxonomy.md` §7: la Taxonomía ya marca los
  posibles trazas con el alérgeno y su keyword (p. ej. `avena` con `gluten`). El validador **no**
  añade una capa de trazas: si la keyword aparece en el nombre normalizado, descarta. La decisión fina
  de negocio vive en `docs/domain/taxonomy.json`; cambiarla es cambiar keywords.
- **Resolución de un ingrediente libre (`pan`) contra las keywords** (antes pregunta 7, la de
  ingrediente libre): resuelta en `recipe-validator`. Un ingrediente libre se resuelve **solo** por
  `keywords`, nunca contra la lista `ingredients` (`docs/domain/taxonomy.md` §7): se normaliza el
  `name` (minúsculas, sin acentos) y se busca contención de cada keyword de las restricciones activas.
  Un `ingredientId` desconocido se degrada a libre y sigue validándose por keywords.
- **Unidades permitidas** (antes pregunta 4): resuelta en `recipe-domain-model`. Conjunto cerrado
  `MeasureUnit`: `g`, `ml`, `ud`, `cda`, `cdta`; `unit` es opcional (`null` = "al gusto"). Ver
  `docs/specs/recipe-domain-model.md`.
- **Nutrición obligatoria u opcional** (antes pregunta 5): resuelta en `recipe-domain-model` como
  **opcional en el esquema** (`Nutrition?`); el prompt y el generador pedirán que se incluya siempre
  que se pueda. Ver `docs/specs/recipe-domain-model.md`.
- **Nº de recetas por generación** (antes pregunta 2): **6** recetas por petición (compromiso
  coste/latencia/UI). El contrato del prompt (`prompt-builder`) pedirá ese número.
- **Caducidad del Lote de sugerencias** (antes pregunta 3): **7 días**, en un único sitio configurable:
  `SuggestionBatchPolicy.BATCH_TTL_MILLIS` en `com.sazon.app.domain.suggestions` (`docs/specs/suggestion-batch-cache.md`).
- **Cálculo de `matchScore`** (antes pregunta 4): lo calcula **la app** (derivado, **no persistido**);
  la app lo recalcula al mostrar resultados. La fórmula vive en `com.sazon.app.ui.results.MatchScore`
  (`results-screen`, ver `docs/specs/results-screen.md`).
- **Formato de las recetas semilla** (antes pregunta 7): **JSON en assets**
  (`app/src/main/assets/`), cargado por la app y validado por test. Empezar con **12** recetas.
- **Licencia de las fuentes Manrope/Inter** (assets tipográficos de `design-tokens-and-theme`): ambas
  se empaquetan bajo **SIL Open Font License 1.1**; el texto completo queda en
  `docs/licenses/manrope-OFL.txt` y `docs/licenses/inter-OFL.txt`. Los `.ttf` de
  `app/src/main/res/font/` son instancias estáticas de los pesos usados (Manrope 600/700, Inter
  400/500) generadas con `fontTools` a partir de las fuentes variables oficiales de Google Fonts, no
  descargables por red en tiempo de ejecución.
- **Proveedor/modelo de IA** (antes pregunta 1): **DevExpert Inference**, API compatible con OpenAI
  (base `https://inference.devexpert.io/v1`, `POST /chat/completions`, `Bearer DEVEXPERT_API_KEY`).
  Alias `chat` (diario) y `chat-pro` (avanzado); cuota semanal. La app usa HTTP directo + parsing
  tolerante del `RecipeValidator`; desde `ai-response-format` (issue #74) pide
  `response_format: {"type":"json_object"}` (forma) con fallback one-shot ante `400`/`422` e interruptor
  en `AiProviderPolicy`, sin depender de `json_schema`/structured outputs forzados (siguen descartados).
  Ver `docs/adr/0003-ai-provider.md`.
- **Licencias de iconos y fotos de receta** (era la pregunta 2 de la tabla): resuelta como **no
  bloqueante**. La app es personal y no se publica, así que la elección de assets es libre. Los iconos
  van vía Material Symbols (incluido en Material 3) y las **fotos de plato** (`recipe-photos`) son
  **generadas por nosotros** con el prompt propio del repo
  (`docs/design/concepts/recipe-photos/stitch-prompt.md`) a partir de las láminas de Stitch versionadas
  en `docs/design/concepts/recipe-photos/{fotos-semillas,fotos-categorias}/`, y se empaquetan como assets
  offline. El placeholder dibujado (degradado) sigue existiendo como último recurso. Ver
  `docs/design/concepts/README.md` y `docs/adr/0004-recipe-photos-as-assets.md`.

## Later / Not MVP

Evolutivos aceptados, a planificar después del MVP:

- Lista de la compra.
- Modo cocina con temporizadores reales.
- Compartir / exportar recetas.
- Multi-idioma.
- Notificaciones y recordatorios.
- Editor de recetas propias.

## Assumptions

- Un único **Usuario local** por dispositivo; no hay cuentas, roles ni compartición.
- No hay backend en el MVP; el dispositivo habla directamente con la API de IA.
- Idioma único: español.
- El usuario dispone de red al menos de forma ocasional; la app debe funcionar sin ella.
- La Taxonomía es suficientemente estable como para empaquetarse con la app.
- El proveedor (DevExpert Inference) **no** garantiza **salida estructurada** (JSON Schema): el
  contrato del prompt pide JSON y el `RecipeValidator` parsea de forma tolerante (ver ADR 0003).
- La clave de IA en `BuildConfig` es aceptable porque el APK no se distribuye.

## Risks

| Riesgo | Probabilidad | Impacto | Mitigación |
|---|---|---|---|
| La IA devuelve una receta que incumple una Restricción dura | Media | Alto | Validador obligatorio + test negativo con `FakeRecipeGenerator` |
| La clave de IA se filtra al distribuir el APK | Baja (demo) | Alto | Documentado como solo-demo; `local.properties` fuera de control de versiones |
| Cuota o coste del proveedor se dispara | Media | Medio | Política cache-first, modo semilla, límite de recetas por petición |
| El modelo devuelve JSON inválido o `*Id` desconocidos | Media | Medio | Validación de esquema + fallback a `other` + log del motivo |
| Deriva entre la Taxonomía de la UI y la del prompt | Baja | Alto | Fuente única + test de integridad de la Taxonomía |
| El catálogo de recetas semilla no se autora a tiempo (es contenido, no código) | Media | Medio | Empezar con 12 recetas; el flujo `ai` no depende de él |
| Sobrecarga de alcance por los 6 evolutivos | Alta | Medio | Están explícitamente fuera del MVP |
| La verificación depende del emulador y es lenta | Media | Bajo | Mantener el dominio testeable en JVM sin Android |
| El flujo se rompe si el proveedor cambia de formato | Media | Medio | Interfaz `RecipeGenerator` + mapping aislado en la implementación |
| El onboarding de 7 pasos añade fricción y abandono | Media | Medio | Pasos cortos, una decisión por pantalla, progreso visible, preferencias de estilo opcionales |
| Diagnóstico verboso de IA (debug-only) deja contenido crudo y títulos en el log | Media | Bajo | **Deuda temporal**: activo solo en debug (`BuildConfig.DEBUG`), apagado en release, sin persistencia; criterio de retirada: cuando esté entendida la causa de `INCOHERENT` y ajustado el prompt, se retira el interruptor (`ai-verbose-diagnostics`); coste: volumen de log (~5 líneas de ~3 KB por llamada más una por descarte, posible rate-limit de Logcat) |
| La Búsqueda con IA es el único punto de Home que exige red y rompe la sensación offline del resto | Media | Medio | Degradar solo ese bloque con aviso claro; el resto de Home intacto |
| La Búsqueda con IA se percibe como "rota" cuando el Perfil bloquea lo que el usuario escribe | Media | Medio | Estado vacío que explica el motivo y enlaza a editar el Perfil |
| El Perfil duplica pantallas del onboarding y divergen | Media | Alto | Regla "un editor, dos entradas": reutilizar los mismos componentes |
| La sincronización de la Taxonomía escribe en el árbol de fuentes y se engancha por nombre de tarea (`merge*Assets`) | Baja | Bajo | `syncTaxonomy` corre en cada build y el asset está ignorado en git; es deuda técnica anotada por el validador, no un fallo |
| Las 31 fotos de receta engordan el APK o se decodifican de forma cara al hacer scroll | Media | Medio | Presupuesto **≤8 MB** para `assets/img/recipes/` (hoy ~5,9 MiB), formato WebP a ≤960 px cuando hay herramienta, decodificación en `Dispatchers.IO` y caché LRU de 16 bitmaps; si molesta al desplazar, la salida es una feature correctiva (p. ej. Coil, con su ADR) |
| Generar la imagen de cada plato gasta cuota de IA (**~2 % por imagen**), añade latencia (**~4,2 s**) y hace crecer el almacenamiento | Media | Medio | El **transporte** (fase 2, `dish-image-generator`), la **caché** (fase 3, `dish-image-cache`), el **control de gasto** (fase 4, `dish-image-settings-toggle`, 2026-10-13) y el **ajuste con referencia** (fase 6, `dish-style-reference`, 2026-10-13) ya existen (ADR 0005): adaptador `data.ai.image` con `POST /images/generations` a `1024x1024`, reducción local a WebP 960 q80 y **un solo intento** (sin reintento), y caché `data.image` de ficheros en `cacheDir/dish-images` con la clave canónica por **nombre de plato** (una imagen se paga una vez), **tope de disco de 80 MiB con evicción LRU** y LRU de bytes en memoria. El crecimiento de almacenamiento queda por tanto **acotado** (mitigación implementada); el **gasto de cuota se materializa ya** con la fase 4 (`dish-image-progressive-ui` + `dish-image-settings-toggle`), al haber **consumidor** en runtime: mitigaciones implementadas (clave compartida por nombre de plato, caché persistente con evicción LRU y tope de 80 MiB, LRU de bytes en memoria, **límite de 2 generaciones concurrentes**, **cancelación al salir de pantalla o hacer scroll**, **tope de generaciones por sesión** y **interruptor de usuario** que deja solo la foto local) y, desde la **fase 7** (`dish-image-seed`, 2026-10-13), un **set semilla** de 6 platos frecuentes del demo empaquetado como assets versionados (`assets/img/dishes/seed/<cacheKey>.<ext>`) que el resolutor resuelve por la **clave canónica** del plato **entre el disco y el generador**: esos platos muestran su foto propia **sin red y sin gastar cuota** (ni la primera vez) y su coste es de **preparación, una sola vez** (~2 % de cuota por plato, en un script fuera de `init.sh` y del gate del interruptor, **evitable** con el modo `--source` del script). La **fase 6** matiza el coste: el ajuste con referencia (`/images/edits`) usa la misma política de un intento, y solo un **rechazo definitivo** del proveedor (`400/404/405/413/415/422`) o una carga inutilizable dispara el respaldo por texto, con **como mucho dos llamadas** (una desechada) para **una** unidad del tope por sesión; un fallo de transporte o `401/403/429/5xx` **no** respalda. Es acotado, silencioso y **reversible con `DishImagePolicy.STYLE_REFERENCE_ENABLED = false`** (rollback de una línea si el proveedor no soportara `image-edit`). Semántica conservadora documentada: un fallo del proveedor consume la unidad (es donde se paga) y apagar/encender el interruptor **no** reinicia el tope |
| El `dishKey` que emita el modelo puede ser **demasiado grueso** (`"pollo"`, `"arroz"`) y hacer que platos distintos **compartan imagen** | Media | Medio | El contrato de la fase 5 (`PromptBuilder` + `docs/domain/taxonomy.md` §4) pide un `dishKey` **breve y específico del plato terminado**, nunca un ingrediente ni la categoría, y la clave es **exacta** (sin fusión difusa). El descriptor es **cosmético y no validado** (no puede descartar la receta), así que un incumplimiento solo degrada la imagen; si se materializa, la salida es reforzar la instrucción del contrato con una feature correctiva. **Entrega en runtime entregada** (`dish-visual-descriptor-wiring`, 2026-10-13): el descriptor del decode llega al `requestFor` (`onDescriptors` + `DishVisualDescriptorStore` + `descriptorFor(recipe.id)`), así que **ya** afecta a la clave y comparte imagen entre variantes del mismo `dishKey`. Limitación viva: el descriptor es **efímero por sesión** (no se persiste; una receta restaurada del Lote o un Favorito se resuelve **por título** hasta la siguiente llamada de IA). Consecuencia de caché conocida y aceptada: un plato antes cacheado por título (`dish-image-v1-arroz-con-pollo-facil`) pasa a pedirse por `dishKey` (`dish-image-v1-arroz-con-pollo`) → **un fallo de caché y una generación extra** la primera vez, y la entrada antigua queda como lastre hasta que la LRU la evicte |


## Research Tasks

1. Comparar la salida estructurada de los proveedores candidatos (esquema forzado, longitud máxima,
   tratamiento de `enum`).
2. Medir latencia y coste reales de generar 4–6 recetas con el contrato de la Taxonomía.
3. Elegir set de iconos de ingredientes (**no bloqueante**: app personal y no publicada; se prioriza
   comodidad, p. ej. Material Symbols o Noto Emoji).
4. Elegir origen de las fotos de receta — **resuelto** (`recipe-photos`, issue #83): fotos generadas por
   nosotros con el prompt del repo, empaquetadas como assets offline (12 semillas + 18 categorías + 1 por
   defecto). Ver `docs/adr/0004-recipe-photos-as-assets.md`.
5. Validar el contrato del prompt contra un modelo real antes de fijarlo como definitivo.
6. Regenerar las pantallas de Stitch a partir de `DESIGN.md` (las actuales quedan obsoletas).

## ADRs

- `docs/adr/0001-taxonomy-in-code.md` — la Taxonomía vive en código y es la única fuente de verdad.
- `docs/adr/0002-local-first-no-backend.md` — todo el estado en el dispositivo; la clave de IA viaja
  en el APK.
- `docs/adr/0003-ai-provider.md` — proveedor de IA: DevExpert Inference (API compatible con OpenAI).
- `docs/adr/0004-recipe-photos-as-assets.md` — las fotos de plato viajan empaquetadas como assets
  offline (convención, cadena de respaldo y descarte de Coil/red).
- `docs/adr/0005-ai-dish-images.md` — las imágenes de plato generadas por IA se piden por un puerto de
  dominio (`DishImageGenerator`) con prompt determinista y clave canónica versionada del nombre del
  plato; ajusta parcialmente el 0004 (la cadena de assets pasa a respaldo). Estado: **aceptado
  parcialmente (fases 1-7)**; el adaptador real (fase 2), la caché de disco + memoria (fase 3), la
  fase 4 **completa** (costura de UI con crossfade + interruptor y tope por sesión), la **fase 5**
  (contrato del descriptor `dishKey`/`imagePrompt` + política pura, **ya entregado en runtime** por
  `dish-visual-descriptor-wiring`), la
  **fase 6** (ajuste con referencia de estilo versionada, `/images/edits`, con respaldo por texto) y la
  **fase 7** (set semilla de platos frecuentes: assets versionados resueltos por la clave canónica
  delante del generador) ya existen; la fase 4 **tiene consumidor** en runtime. Con la fase 7, la
  **lista de fases de la propuesta queda completa**; no queda ningún pendiente de entrega de la
  propuesta (el descriptor es **efímero por sesión**, ya que no se persiste).

`RecipeGenerator` como interfaz con implementación falsa **no se elevó a ADR**: introducir esa
abstracción más tarde sería un refactor de una sola capa y el patrón es estándar. Queda documentado
en `docs/technical-discovery.md`.
