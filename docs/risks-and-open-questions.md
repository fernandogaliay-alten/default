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
- **Proveedor/modelo de IA** (antes pregunta 1): **dos proveedores** desde la correctiva
  `chat-provider-opencode-go` (2026-10-14, issue #141, por agotamiento de la cuota semanal de
  DevExpert). El **chat** va a **OpenCode Go** (base `https://opencode.ai/zen/go/v1`, `POST
  /chat/completions`, `Bearer OPENCODE_API_KEY`, modelo **literal** `deepseek-v4.1-flash` y cabeceras
  obligatorias `x-opencode-session` + `User-Agent`) y la **imagen de plato** sigue en **DevExpert
  Inference** (base **propia** `https://inference.devexpert.io/v1` en `DishImagePolicy`, alias
  `image`/`image-edit`, `Bearer DEVEXPERT_API_KEY`). La app usa HTTP directo + parsing
  tolerante del `RecipeValidator`; desde `ai-response-format` (issue #74) pide
  `response_format: {"type":"json_object"}` (forma) con fallback one-shot ante `400`/`422` e interruptor
  en `AiProviderPolicy`, sin depender de `json_schema`/structured outputs forzados (siguen descartados).
  Ver `docs/adr/0003-ai-provider.md`.
- **Riesgo nuevo — cuota compartida y monitorización de abuso de OpenCode Go** (2026-10-14, issue #141):
  Go está pensado para **agentes de programación** y **monitoriza el abuso**; su cuota (límites por
  **5 h/semana/mes**) la comparte el chat de la app **con el harness de agentes de este repo**, así que
  una sesión intensa de agentes puede agotar la del chat. Además, el endpoint **Console** de OpenCode
  (`https://opencode.ai/inference/...`) responde `402 Insufficient account funds` (sin saldo), por lo
  que **no** es alternativa. Mitigación: el chat **degrada a modo semilla** sin fallar cuando la cuota o
  la clave fallan, y la política cache-first del Lote reduce las llamadas. Riesgo **no bloqueante**
  (registrado; una cuota agotada no rompe la app, solo devuelve recetas semilla).
- **Riesgo nuevo — id de modelo fijado** (2026-10-14, issue #141): OpenCode Go **no expone alias**, así
  que el chat fija `deepseek-v4.1-flash` (`AiProviderPolicy.MODEL_ID`) en un único sitio; si el
  proveedor lo renombra, el chat degrada a modo semilla hasta cambiar esa constante (cambio de una
  línea, sin fallo de la app). Igual de acotado: el **valor** exacto de `x-opencode-session` está
  verificado como **presencia** (sin ella `400 MissingSessionID`), no como formato; la constante
  (`"sazon-android"`) vive en un único sitio y cambiarla es una línea.
- **Licencias y precisión de las fotos de receta** (era la pregunta 2 de la tabla): resuelta como **no
  bloqueante** y, después, **resuelta por retirada** (ADR 0006). La app es personal y no se publica, así
  que la elección de assets es libre: los iconos van vía Material Symbols (incluido en Material 3). Las
  **fotos de plato** (`recipe-photos`) eran **generadas por nosotros** con el prompt propio del repo
  (`docs/design/concepts/recipe-photos/stitch-prompt.md`) a partir de las láminas de Stitch versionadas en
  `docs/design/concepts/recipe-photos/{fotos-semillas,fotos-categorias}/`, y se empaquetaban como assets
  offline; pero la **foto de categoría no corresponde al plato** de las recetas `ai-*` (síntoma de
  `dish-image-prompt-v2`), así que con `docs/adr/0006-typographic-recipe-cards.md` la **cadena local de
  fotos se retiró** con la feature `retire-recipe-photos` (2026-10-14) y la imagen de receta pasa a ser una **ficha
  tipográfica** (la foto, si existe por IA, es **capa de fondo**). Ver `docs/design/concepts/README.md`,
  `docs/adr/0004-recipe-photos-as-assets.md` (ajustado) y `docs/adr/0006-typographic-recipe-cards.md`.

## Later / Not MVP

Evolutivos aceptados, a planificar después del MVP:

- Lista de la compra.
- Modo cocina con temporizadores reales.
- Compartir / exportar recetas.
- Multi-idioma.
- ~~Notificaciones y recordatorios.~~ **Enmendado de forma acotada (2026-10-15)**: entra **solo** el
  aviso de fin del temporizador del Modo cocina (`cooking-timer-alarm`, ADR 0007) como notificación de
  sistema con sonido y vibración, más la **cuenta atrás visible** del mismo temporizador en una
  notificación persistente de un servicio en primer plano y la **reanudación del paso** desde la
  notificación (`cooking-timer-foreground-service`, ADR 0008). Los **recordatorios programados por el
  usuario** y las **notificaciones de sugerencias/recetas** siguen fuera del MVP.
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
| Las 31 fotos de receta engordan el APK o se decodifican de forma cara al hacer scroll | — | — | **Resuelto por retirada** (ADR 0006, aplicada por `retire-recipe-photos` el 2026-10-14): **30 de los 31 assets (5.954.912 B ≈ 5,68 MiB)** —se conserva `img/recipes/default.jpg` (137.359 B) como ancla de estilo, sin mostrarse— y la cadena local (`RecipePhotoAssets`, `AssetRecipePhotoSource`, el seam `RecipePhotoSource`) **salieron del APK y del runtime**, con lo que desaparece la decodificación de esas fotos al hacer scroll. Ya no aplica el presupuesto de **≤8 MB** de `assets/img/recipes/` (queda `default.jpg`, 137.359 B) |
| Generar la imagen de cada plato gasta cuota de IA (**~2 % por imagen**), añade latencia (**~4,2 s**) y hace crecer el almacenamiento | Media | Medio | El **transporte** (fase 2, `dish-image-generator`), la **caché** (fase 3, `dish-image-cache`), el **control de gasto** (fase 4, `dish-image-settings-toggle`, 2026-10-13) y el **ajuste con referencia** (fase 6, `dish-style-reference`, 2026-10-13) ya existen (ADR 0005): adaptador `data.ai.image` con `POST /images/generations` a `1024x1024`, reducción local a WebP 960 q80 y **un solo intento** (sin reintento), y caché `data.image` de ficheros en `cacheDir/dish-images` con la clave canónica por **nombre de plato** (una imagen se paga una vez), **tope de disco de 80 MiB con evicción LRU** y LRU de bytes en memoria. El crecimiento de almacenamiento queda por tanto **acotado** (mitigación implementada); el **gasto de cuota** es **opt-in** desde el ADR 0006 y, desde `dish-image-settings-default-off` (2026-10-14), es además el **default del arranque** (`DEFAULT_ENABLED = false`: el interruptor **aparece apagado**, así que no se paga cuota ni se espera latencia hasta que la persona lo encienda): hay **consumidor** en runtime (fase 4, `dish-image-progressive-ui` + `dish-image-settings-toggle`), pero **solo** se paga cuota con el interruptor encendido, con las mitigaciones ya implementadas (clave compartida por nombre de plato, caché persistente con evicción LRU y tope de 80 MiB, LRU de bytes en memoria, **límite de 2 generaciones concurrentes**, **cancelación al salir de pantalla o hacer scroll**, **tope de generaciones por sesión** y **interruptor de usuario** —desde el **ADR 0006** el gasto pasa a ser **opt-in**: **solo** con el interruptor "Generar imágenes del plato" encendido se paga cuota y se espera latencia; apagado se muestra **solo la ficha tipográfica**, sin leer caché, sin red y sin cuota; desde `dish-image-settings-default-off` (2026-10-14) ese apagado es el **estado inicial** del interruptor; desde `dish-image-settings-persistence` (2026-10-15) ese **estado inicial** y la elección del usuario **se recuerdan** entre reinicios —el interruptor se persiste en un bit detrás de una costura pura y se restaura antes del primer frame—, mientras el **tope por sesión** sigue reiniciándose a 12 en cada proceso—) y, desde la **fase 7** **set semilla** de 6 platos frecuentes del demo empaquetado como assets versionados (`assets/img/dishes/seed/<cacheKey>.<ext>`) que el resolutor resuelve por la **clave canónica** del plato **entre el disco y el generador**: esos platos muestran su foto propia **sin red y sin gastar cuota** (ni la primera vez) y su coste es de **preparación, una sola vez** (~2 % de cuota por plato, en un script fuera de `init.sh` y del gate del interruptor, **evitable** con el modo `--source` del script). La **fase 6** matiza el coste: el ajuste con referencia (`/images/edits`) usa la misma política de un intento, y solo un **rechazo definitivo** del proveedor (`400/404/405/413/415/422`) o una carga inutilizable dispara el respaldo por texto, con **como mucho dos llamadas** (una desechada) para **una** unidad del tope por sesión; un fallo de transporte o `401/403/429/5xx` **no** respalda. Es acotado, silencioso y **reversible con `DishImagePolicy.STYLE_REFERENCE_ENABLED = false`** (rollback de una línea si el proveedor no soportara `image-edit`). Semántica conservadora documentada: un fallo del proveedor consume la unidad (es donde se paga) y apagar/encender el interruptor **no** reinicia el tope |
| El `dishKey` que emita el modelo puede ser **demasiado grueso** (`"pollo"`, `"arroz"`) y hacer que platos distintos **compartan imagen** | Media | Medio | El contrato de la fase 5 (`PromptBuilder` + `docs/domain/taxonomy.md` §4) pide un `dishKey` **breve y específico del plato terminado**, nunca un ingrediente ni la categoría, y la clave es **exacta** (sin fusión difusa). El descriptor es **cosmético y no validado** (no puede descartar la receta), así que un incumplimiento solo degrada la imagen; si se materializa, la salida es reforzar la instrucción del contrato con una feature correctiva. **Entrega en runtime entregada** (`dish-visual-descriptor-wiring`, 2026-10-13): el descriptor del decode llega al `requestFor` (`onDescriptors` + `DishVisualDescriptorStore` + `descriptorFor(recipe.id)`), así que **ya** afecta a la clave y comparte imagen entre variantes del mismo `dishKey`. Limitación viva: el descriptor es **efímero por sesión** (no se persiste; una receta restaurada del Lote o un Favorito se resuelve **por título** hasta la siguiente llamada de IA). Consecuencia de caché conocida y aceptada: un plato antes cacheado por título (`dish-image-v2-arroz-con-pollo-facil`) pasa a pedirse por `dishKey` (`dish-image-v2-arroz-con-pollo`) → **un fallo de caché y una generación extra** la primera vez, y la entrada antigua queda como lastre hasta que la LRU la evicte |
| El prompt de imagen no describe bien el plato (imágenes que no cuadran) o no se puede saber de dónde salió la imagen que se repite | Media | Medio | **Mitigado** por `dish-image-prompt-v2` (2026-10-13, ADR 0005): la plantilla v2 lleva **siempre** el título como identidad, el detalle del descriptor, los **ingredientes clave** (≤5, no despensa), composición (ración completa, primer plano, fondo neutro) y **negativos reforzados**; el contrato de `PromptBuilder` pide un `imagePrompt` más rico. El cambio de plantilla **invalida** la caché de disco (bump a `dish-image-v2`) y **renombra** los 6 assets semilla (mismos bytes, **coste cero en cuota**; no se regenera nada), de modo que cada plato ya cacheado se regenera **una vez** (~2 % de cuota, acotado por la LRU y el tope por sesión) al pedirse con la clave nueva. Además, el **fallback a la foto de categoría** (y el acierto del set semilla) pasa a ser **observable en debug**: el log `SazonDishImage` emite el origen resuelto (`GENERATED`/`SEED`/`GLYPH`; el valor `CATEGORY` se retiró con `retire-recipe-photos`), lo que permite distinguir un fallo de generación (timeout/`429`/tope por sesión) de un acierto de categoría. Queda **pendiente separado** revisar el tope de 12 por sesión y el manejo del `429` (otra correctiva); la mejora del prompt aumenta la probabilidad de acierto, **no** la garantiza |
| El **modo degradado de Resultados** (`ResultsStatus.Unavailable`) muestra las recetas semilla **sin pasar el filtro de seguridad**, incumpliendo el Success Criterion de `docs/build-brief.md` ("ninguna receta mostrada incumple una Restricción dura marcada") | Media | Alto | **Deuda de seguridad conocida** (resolución 1 de `docs/specs/home-suggestions-personalized.md`): el carrusel "Sugeridas para ti" **ya** filtra (restricciones duras + patrón de dieta) y acota a 6 desde `home-suggestions-personalized` (2026-10-13), pero `ResultsStatus.Unavailable` sigue **puntuando y ordenando sin filtrar**. La evidencia de `ResultsStatusTest` explica por qué no se toca aquí: sus fixtures (`recetaAlta`/`recetaBaja`) tienen `steps = emptyList()` con `totalMinutes = 25/90`, **incoherentes** para la regla 4 del Validador, así que filtrar hoy **vaciaría** la lista degradada y rompería un contrato aceptado. Salida: **feature correctiva propia** (p. ej. `results-seed-safety-filter`) que decida si el modo degradado debe filtrar y **rehaga los fixtures** de `ResultsStatusTest`; no se resuelve en `home-suggestions-personalized` para no alterar en silencio `results-screen` (aceptada). |
| La retirada de las fotos renuncia al **atractivo apetitoso** y la ficha tipográfica puede resultar fría o menos convincente | Media | Medio | **Riesgo de producto consciente** (ADR 0006): la dirección pasa a un **registro editorial** (jerarquía tipográfica, color cálido y claridad de la ficha: qué plato es y qué lleva). Es **reversible**: volver a las fotos es un `revert`; el acabado se revisa en el emulador y, si no convence, se corrige con una **feature correctiva** |
| La dirección tipográfica (ADR 0006) se decidió sobre **prototipos HTML locales**, no sobre export de Stitch, y las capturas `.png` por pantalla están **pendientes de autorización** | Media | Medio | **Desviación de la política de capturas** documentada en `DESIGN.md` y `docs/design/concepts/README.md`: los prototipos viven versionados en `docs/design/concepts/typographic-cards/`. La persona **autorizó** la desviación (2026-10-13) y **aportó** las capturas `.png` (2026-10-14, renderizadas de los prototipos con Chrome headless) para las cuatro features de esa dirección: `card-typographic-tokens-and-design` (`tarjeta-oscura`), `recipe-card-typographic` (`recipe-card`, `favoritos-lista`, `favoritos-rejilla`), `recipe-detail-typographic-header` (`detalle-receta`) y `dish-artwork-as-card-background` (`tarjeta-con-imagen`, `detalle-con-imagen`). **Queda para revisión visual de la persona** (emulador, tras el merge) el acabado de las variantes **no capturadas**: la tarjeta **estrecha** del carrusel y la **compacta** de la rejilla con la banda sólida L2 son una **extrapolación** de la captura ancha (OQ6 sigue abierta: ¿sube el carrusel a 3:2?); el contraste del título sobre una foto arbitraria depende del `scrim` y no es medible en JVM |
| El **icono del lanzador y su splash** los resuelve y los cachea el launcher, y no todos re-resuelven la variante nocturna ni invalidan su caché de iconos al cambiar el tema | Media | Bajo | El contrato verificable (`LauncherIconTest`/`SplashThemeTest`, `app-launcher-icon`) ata las tres capas del icono, su paleta y los atributos del splash a `DESIGN.md`, y **el acabado lo revisa la persona en el emulador** (cajón/recientes/Ajustes en claro y oscuro, cuadrado y redondo, con iconos tematizados, y el splash de arranque); si en algún launcher no se ve el **fondo oscuro**, la corrección es una **feature correctiva** (p. ej. fondo único) sin reabrir la feature aceptada |
| El aviso de fin del temporizador no llega porque el usuario **denegó `POST_NOTIFICATIONS`** o el sistema **no concede la alarma exacta** | Media | Medio | **Degradación silenciosa** (ADR 0007): con el permiso denegado `CookingNotifications.notifyFinish` no publica nada y **no** lanza, y el Modo cocina, la cuenta atrás y el "¡Listo!" en pantalla siguen **intactos** (desde la correctiva `cooking-timer-alarm-permission-context` el permiso se pide **en contexto**, al arrancar el temporizador de un paso y tras un rationale, sin gatear ninguna pantalla **ni el temporizador**: denegar o descartar el rationale no retrasa la cuenta atrás ni la alarma del aviso). Si `canScheduleExactAlarms()` es `false`, `AndroidCookingAlarm` **degrada** a `setAndAllowWhileIdle` (inexacta: el aviso puede llegar tarde, pero llega) y nunca hay crash (`runCatching` en todo el adaptador). El caso real (Doze, batería agresiva del fabricante, pantalla bloqueada) se comprueba en la **prueba manual** del emulador, no automatizable según AGENTS.md. **Riesgo adicional corregido** (correctiva `cooking-timer-finish-notification`, 2026-10-16): con la app en **primer plano** el aviso **no siempre llegaba** porque `CookingAlarmPolicy` devolvía `Cancel` en `RUNNING → FINISHED` y ese `Cancel` cancelaba la alarma (y paraba el servicio) antes de que disparase `AlarmManager` (carrera no determinista entre el `tick` de la composición y el disparo del sistema); desde la correctiva esa arista devuelve `None`, la alarma se deja viva y el aviso se publica **siempre**, sin duplicar (mismo id `1001`). **Productor primario cambiado** (correctiva `cooking-timer-notification-reliability`, 2026-10-16): el aviso puntual lo publica el **servicio en primer plano** en el instante exacto y la alarma queda como **respaldo** (el `delay` del proceso vivo no depende de que el fabricante difiera/coalesca la alarma); con dos publicadores, "una sola notificación y una sola alerta por fin" lo garantiza el **sello de fin** (`CookingFinishPublication`), no el publicador único |
| Tocar la notificación del aviso **no devuelve al paso** si el sistema mató la actividad o el proceso | Media | Bajo | **Resuelto por `cooking-timer-foreground-service`** (2026-10-15, ADR 0008): la **sesión del Modo cocina** se persiste en una fila (`cooking_session.db`: receta, paso, marcado, temporizador e **instante de fin**) y el toque de la notificación (persistente o de fin) la **reanuda** en el paso exacto con la cuenta atrás recalculada —En marcha con el tiempo restante o Terminado si el instante ya pasó—, también con el proceso muerto (el `Intent` de contenido lleva **solo** el booleano `EXTRA_RESUME_COOKING`). La reanudación es **explícita**: un arranque normal (lanzador) **no** abre el Modo cocina, así que una sesión antigua no secuestra el arranque. Con la actividad viva se sigue reutilizando (`NEW_TASK or SINGLE_TOP`). **Cerrado del todo** con la correctiva `cooking-timer-finish-notification` (2026-10-16): una sesión persistida en **Terminado** también reabre el paso desde el aviso —antes `isResumable` exigía En marcha/Pausado, el hallazgo **Low L1** de la validación de `cooking-timer-foreground-service`—, con `remaining = 0` y sin rearmar alarma ni servicio; la puerta pasa a `CookingSession.reopensFromNotification` |
| El **servicio en primer plano** de la cuenta atrás depende de permisos y del tipo `specialUse` (API 34+) y de que el sistema no mate el proceso | Media | Bajo | **Degradación silenciosa** (ADR 0005/0008): `FOREGROUND_SERVICE` y `FOREGROUND_SERVICE_SPECIAL_USE` son permisos **normales** (sin diálogo, concedidos al instalar) y el tipo declarado es `specialUse` con su `<property>` (no hay tipo específico de temporizador y `shortService` está limitado a ~3 min). Si `startForeground` no se puede completar, el servicio se apaga **sin lanzar** y el Modo cocina, la cuenta atrás en pantalla y el "¡Listo!" siguen intactos. El servicio es `START_NOT_STICKY` y `stopWithTask` queda por defecto: si el sistema mata el proceso o el usuario quita la tarea de recientes, **la cuenta atrás desaparece** pero el "¡Listo!" sigue llegando (alarma) y el toque devuelve al paso (sesión persistida). No se añade `RECEIVE_BOOT_COMPLETED`: un reinicio deja la sesión obsoleta (sin notificación que la reanude) |
| El canal `cooking_timer` es de **importancia alta** y sus atributos son inmutables: la notificación **persistente** podría alertar (sonar/vibrar) la primera vez que se publica | Baja | Bajo | Se publica con `setOnlyAlertOnce(true)` + `setSilent(true)` (sin sonido ni vibración propias y sin repetir alerta al refrescarse) y **no** se puede bajar la importancia de un canal existente. Cosmético: se comprueba en la prueba **manual** y, si molesta, el remedio es una **feature correctiva** con un canal propio de importancia baja (sin tocar el aceptado). En la misma prueba se revisa el **orden** de las dos notificaciones mientras coexisten (persistente `1000` y aviso `1001`) |
| El cronómetro descendente de la notificación persistente lo **pinta el sistema**: una capa del fabricante (pantalla de bloqueo/OEM) podría no renderizarlo | Baja | Bajo | `setUsesChronometer(true)` + `setChronometerCountDown(true)` es el mecanismo estándar y no depende del proceso (cero despertares por segundo); si alguna capa no lo pintara, no habría cuenta atrás visible y el remedio sería una **feature correctiva** (bucle de texto por segundo o `RemoteViews`). Se comprueba en la prueba **manual** del emulador, no automatizable según AGENTS.md. Deriva conocida y aceptada: el cronómetro se expresa en hora de pared (`when`), así que un cambio de reloj mientras corre puede desviar lo mostrado respecto al fin real (que mide el reloj monotónico y la alarma); desde `cooking-timer-notification-reliability` (2026-10-16) el `when` se **recalcula al publicar** (`currentTimeMillis() + (endAt - elapsedRealtime())`), así que no se hereda un valor obsoleto, el servicio **no pinta** la persistente si el instante de fin ya pasó (nunca se ve la cuenta atrás en negativo) y la retirada en el instante exacto la aseguran el propio servicio y, si está congelado/muerto, el receptor de respaldo (parar servicio + `cancel(1000)`) |
| El aviso de fin puede **llegar tarde** (o llegar el de un **paso anterior** durante el siguiente) si el fabricante difiere/coalesce la alarma exacta o el proceso estaba muerto | Media | Medio | **Mitigado** por `cooking-timer-notification-reliability` (2026-10-16): el aviso puntual lo publica el **servicio en primer plano** en el instante exacto (su `delay` corre en el proceso vivo, que además pinta la cuenta atrás) y la alarma queda como **respaldo** para la muerte/congelación del proceso. Con **dos** publicadores, la idempotencia la da el **sello de fin** (hora de pared, calculado una vez y llevado en los dos `Intent`, publicado como `when` de la `1001`) más la decisión pura `CookingFinishPublication`: mismo sello ya mostrado ⇒ **no** se republica (una sola notificación y **una sola** alerta), sello más viejo que la **ventana de frescura** de **60 s** ⇒ es el aviso de un paso anterior y se **descarta**. Precio asumido y documentado: una alarma que el fabricante retrase **más** de la ventana con el proceso muerto se pierde (se revisa en la prueba manual). El retraso de 10-15 s de la **primera** cuenta atrás tras un arranque en frío **no** se puede reproducir desde WSL: la mitigación incluida es desacoplar el arranque del trabajo de sistema de los efectos de Compose (un único punto por gesto, con una sola lectura del reloj), y la **prueba manual** en el dispositivo ColorOS decide; si persiste, es una feature correctiva |
| Con la app en **primer plano**, el canal `cooking_timer` de importancia alta puede sonar/vibrar (heads-up) **además** del "¡Listo!" que ya se ve en pantalla al llegar a cero | Baja | Bajo | **Trade-off asumido** por la Opción A de `cooking-timer-finish-notification` (2026-10-16): la alarma sigue siendo el **único** productor del aviso y se deja viva al entrar en Terminado, así que en primer plano pueden coincidir el heads-up del sistema y el "¡Listo!" de la pantalla. **No hay duplicado de alerta**: desde `cooking-timer-notification-reliability` (2026-10-16) el productor primario es el **servicio** (publica al vencer) y la alarma, al disparar con el **mismo sello**, no republica; el id fijo `1001` evita además dos notificaciones. El sonido/vibración los aplica el sistema **una vez** por fin. No se mitiga aquí (bajar la importancia exigiría un canal nuevo, descartado en el ADR 0008); se revisa en la prueba **manual** y, si molesta, se corrige con una **feature correctiva** (canal propio de importancia baja) |
| **Rotar** la pantalla o volver a la app por el lanzador cierra el Modo cocina | Baja | Bajo | Limitación **preexistente y fuera de alcance**: `MainShell` hospeda el Modo cocina con `remember` (la `Recipe` no es serializable) y el `onDispose` cancela alarma y servicio. La correctiva `cooking-timer-finish-notification` (2026-10-16) **solo** garantiza que una rotación u otro desmontaje **no borra** el aviso `1001` ya mostrado (el `onDispose` emite únicamente `CookingAlarmPolicy.onExit()`/`Cancel`, que no retira notificaciones mostradas, decisión 9 del ADR 0007) ni la sesión persistida. Conservar o reabrir el modo al rotar sería una **feature correctiva** nueva |


## Research Tasks

1. Comparar la salida estructurada de los proveedores candidatos (esquema forzado, longitud máxima,
   tratamiento de `enum`).
2. Medir latencia y coste reales de generar 4–6 recetas con el contrato de la Taxonomía.
3. Elegir set de iconos de ingredientes (**no bloqueante**: app personal y no publicada; se prioriza
   comodidad, p. ej. Material Symbols o Noto Emoji).
4. Elegir origen de las fotos de receta — **resuelto** (`recipe-photos`, issue #83) y después
   **revertido** por `docs/adr/0006-typographic-recipe-cards.md`: las fotos generadas por nosotros y
   empaquetadas como assets offline (12 semillas + 18 categorías + 1 por defecto, ~6,4 MiB; se conserva la
   neutra `default.<ext>` como ancla de estilo) **se retiran**
   con `retire-recipe-photos`, porque la foto de categoría no corresponde al plato; la imagen de receta es
   **tipográfica**. Ver `docs/adr/0004-recipe-photos-as-assets.md` (ajustado).
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
- `docs/adr/0006-typographic-recipe-cards.md` — la **imagen base de una receta es una ficha
  tipográfica** (categoría, título y los tres ingredientes principales con cantidad + filete de cocina) y
  **no una foto por defecto**; la foto (generada por IA, **opt-in**) pasa a ser **capa de fondo** del hueco
  (variante **L2**), se **retiró la cadena local de fotos** (`recipe-photos`; 30 de los 31 assets,
  **5.954.912 B ≈ 5,68 MiB**, ya aplicado por `retire-recipe-photos` el 2026-10-14; se conserva
  `img/recipes/default.jpg`, 137.359 B, como ancla de estilo), la tarjeta gana **dos tokens en oscuro** (`darkCardSurface` `#3A2E26` /
  `darkCardOutline` `#7D6A5C`, 3,26:1) y `ui.artwork.RecipeArtwork` pasa a ser el **proveedor de la capa de
  fondo**. Ajusta los ADR **0004** y **0005**.
- `docs/adr/0007-cooking-timer-alarm.md` — el fin del temporizador del Modo cocina avisa con una
  **alarma exacta** (`AlarmManager`, `ELAPSED_REALTIME_WAKEUP`, `USE_EXACT_ALARM` con degradación
  inexacta) y una **notificación propia** (canal `cooking_timer` de importancia alta con sonido y
  vibración, id fijo, contenido **genérico** sin datos de la receta), disparada por un **receptor** de
  manifiesto `exported="false"`; la decisión del aviso es **pura** (`CookingAlarmPolicy`, instantes
  inyectados) y el permiso `POST_NOTIFICATIONS` se pide **en contexto**, al arrancar el temporizador de
  un paso y tras un rationale (correctiva `cooking-timer-alarm-permission-context`, 2026-10-15; **antes**
  se pedía una vez por proceso al arrancar — enmienda a la decisión 11 y alternativa 9 del ADR). **No** se
  persiste el Modo cocina ni se introduce un `foreground service`. Enmienda acotada del Non-Goal
  "Notificaciones y recordatorios" de `docs/build-brief.md`. Las alternativas 3 (`foreground service`) y
  4 (persistir el Modo cocina) quedan **revisadas/adoptadas** por el ADR 0008.
- `docs/adr/0008-cooking-timer-foreground-service.md` — la **cuenta atrás** del temporizador del Modo
  cocina se ve fuera de la app en una **notificación persistente** (id `1000`) de un **servicio en primer
  plano** de tipo `specialUse` que **pinta el sistema** (cronómetro descendente: cero despertares por
  segundo); el servicio es un **pintor** y la alarma one-shot del ADR 0007 sigue siendo la **única**
  dueña del "¡Listo!"; la **sesión del Modo cocina** (una fila en `cooking_session.db`, base propia) se
  persiste para que el toque de la notificación devuelva al **paso exacto** incluso con el proceso muerto,
  y la reanudación es **explícita** (`EXTRA_RESUME_COOKING`, nunca al abrir la app). Enmienda acotada del
  Non-Goal "Notificaciones y recordatorios" de `docs/build-brief.md`.

`RecipeGenerator` como interfaz con implementación falsa **no se elevó a ADR**: introducir esa
abstracción más tarde sería un refactor de una sola capa y el patrón es estándar. Queda documentado
en `docs/technical-discovery.md`.
