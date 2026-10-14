# Registro de progreso

## Estado verificado actual

- Raíz del repositorio: `/mnt/c/Users/fernando.galiay/Documents/Default-Project` (WSL2) =
  `C:\Users\fernando.galiay\Documents\Default-Project` (Windows)
- Camino estándar de arranque: **`bash init.sh` en WSL** (nativo; ya no hace falta Git Bash). El
  entorno de Linux (JDK 21, SDK de Android, `gh`) lo publica `scripts/wsl-env.sh`, que cargan
  `init.sh`, `scripts/gradle.sh` y `scripts/new-worktree.sh`; `scripts/wsl-bootstrap.sh` lo instala
  desde cero, sin `sudo`. Ver Sesión 059.
- Camino estándar de verificación: `./gradlew test` y `./gradlew lint`, ambos vía `init.sh` (que
  enruta la salida por `scripts/gradle.sh` para no colgar la shell del agente; ver Sesión 056)
- Flujo de entrega: `github-flow` (worktrees + issues/PR) sobre `feature-flow`; rama base `main` y
  remoto `origin` → `https://github.com/fernandogaliay-alten/default.git` (privado)
- Últimas features aceptadas: `fake-recipe-generator`, `recipe-validator`, `prompt-builder`,
  `seed-recipe-catalog`, `design-tokens-and-theme`, `onboarding-shell-and-identity`,
  `onboarding-diet-style-cuisines`,   `onboarding-ingredients-safety-kitchen`, `suggestion-batch-cache`,
  `home-offline-blocks`, `local-result-filters`, `generator-ingredients-step`, `generator-settings-step`,
  `profile-screen`, `generation-flow-with-validation`, `manual-suggestions-refresh`, `results-screen`,
  `recipe-detail-screen`, `favorites`, `activity-summary`, `ai-search` y `real-ai-provider` (validador independiente, veredicto `accept`). El onboarding queda **completo**
  (7 pasos) y persiste el Perfil al cerrar; el Generador ya ejecuta el corte vertical principal del MVP
  (generar -> validar -> mostrar solo aceptadas, con carga cancelable) y monta la pantalla de Resultados
  real (fórmula de `matchScore`, estados vacío/error con "Reintentar", subtítulo de búsqueda); la pestaña
  Perfil es real (identidad + preferencias editables reutilizando los editores del onboarding) y permite
  forzar el refresco del Lote, además del nuevo Resumen de actividad (recetas generadas persistidas +
  favoritas derivadas); el Detalle de receta ya muestra ingredientes con cantidades, ajusta las
  raciones (escalado con redondeo por unidad), recorre los pasos y consulta tips/nutrición opcional; la
  pestaña Favoritos es real (guardar/quitar, persistencia local de una copia de la Receta y consulta sin
  conexión, con estados poblado/vacío). El proveedor de IA real ya está conectado tras el puerto
  `RecipeGenerator` (DevExpert Inference, Ktor + OkHttp aislado en `data.ai`, política de
  timeouts/reintentos y degradación a modo semilla), con lo que el corte vertical principal del MVP queda
  completo de punta a punta.
- Dirección de diseño vigente (2026-10-13, sesión 101): la imagen de una receta es una **ficha
  tipográfica** (ADR 0006) y la foto es **capa de fondo opcional** supeditada al ajuste de imágenes por
  IA; se retira la cadena local de fotos (se conserva `default.<ext>` como ancla de estilo). Cinco
  features nuevas en `feature_list.json`, todas `not_started`, y sus capturas de referencia ya **aportadas**
  (2026-10-14, renderizadas de los prototipos locales con Chrome headless; desviación de la política de
  capturas autorizada por la persona).
- Primera feature del ADR 0006 implementada y autoverificada (sesión 103): **`card-typographic-tokens-and-design`**
  queda **`passing`** en su worktree `feature/card-typographic-tokens-and-design` (tokens de tarjeta en
  `ui/theme`, contrato `SazonCardTokens`/`SazonCardColors`/`LocalSazonCardColors` con el tinte cálido y
  tests JVM de contraste WCAG; **ninguna** pantalla la consume todavía) a la espera de **validación
  independiente**. Las otras cuatro features del ADR 0006 siguen `not_started`.
- Cuarta feature del ADR 0006 implementada y autoverificada (sesión 109): **`dish-artwork-as-card-background`**
  (`area: integration`) queda **`passing`** en su worktree `feature/dish-artwork-as-card-background`
  (imagen generada como capa de fondo opcional del hueco de la tarjeta —16:9— y del hero —240—, con
  `scrim` de contraste y banda sólida; `RecipeArtwork` pasa a ser el **proveedor de la capa de fondo** con
  la ficha tipográfica como capa base y **dos** consumidores; precedencia reducida a dos capas; tokens del
  `scrim`/texto sobre imagen en `ui/theme/CardImageOverlay`; el interruptor y el tope por sesión se
  respetan sin cambios) a la espera de **validación independiente**. Segunda y tercera
  (`recipe-card-typographic` y `recipe-detail-typographic-header`) ya `accepted`; queda **`not_started`**
  la quinta, `retire-recipe-photos` (depende de esta).
- Siguiente feature en orden de lista: **ninguna**; `real-ai-provider` fue la última feature del plan y
  quedó `accepted` en la sesión 067 (validación independiente, rama `feature/real-ai-provider`). El commit
  local lo hace el orquestador de `feature-flow` y la publicación queda para `github-flow`. Después se
  añadió la **correctiva** `ai-provider-timeouts` (issue #68), que quedó `accepted` en la sesión 069
  (validación independiente, veredicto `accept`, rama `feature/ai-provider-timeouts`, sesión 068 la había
  dejado `passing` implementada y autoverificada). No hay features `not_started` ni `in_progress`. El plan
  de features del MVP sigue **cerrado**. Después se añadió una segunda **correctiva** de
  `real-ai-provider`, `ai-recipe-decode` (issue #70): implementada y autoverificada (`passing`) en la
  sesión 070 y **`accepted`** en la sesión 071 (validación independiente, veredicto `accept`, rama
  `feature/ai-recipe-decode`). El adaptador real ya decodifica la **forma real** del contrato §4 (sin
  `id` ni `source`), completa `id` (`ai-<sha256>` determinista) y `source = ai` antes de aplicar
  `RecipeValidator.validateRecipe`; `Recipe.kt`/`RecipeValidator.kt` intactos. No hay features
  `not_started`, `in_progress` ni `passing`: la tercera **correctiva** de `real-ai-provider`,
  `ai-call-diagnostics` (issue #72), quedó **`accepted`** en la sesión 073 (validación independiente,
  veredicto `accept`, rama `feature/ai-call-diagnostics`). El plan del MVP sigue **cerrado**; la
  ampliación **`cooking-mode`** (issue #76) quedó **`accepted`** en la sesión 078 (ver abajo). Después se
  añadió la **correctiva** de `ai-recipe-decode` **`ai-quantity-tolerance`** (tolerar `quantity` nula
  en el wire de la IA, ingredientes "al gusto"): implementada y autoverificada (**`passing`**) en la
  sesión 081 y **`accepted`** en la sesión 082 (validación independiente, veredicto `accept`, rama
  `feature/ai-quantity-tolerance`). **No hay features `not_started`, `in_progress`, `passing` ni
  `blocked`**: todas las entradas de `feature_list.json` están `accepted`. El resto del plan sigue
  **cerrado**. Después se dio de alta la **fase 1** de `docs/ai-dish-images-proposal.md` como feature
  **`dish-image-domain`** (`area: domain`; dependencias `recipe-domain-model` y `taxonomy-in-code`, ya
  `accepted`): implementada y autoverificada (**`passing`**) en la **sesión 085** en su worktree
  (`feature/dish-image-domain`, sin commit/push/PR) — política pura en `com.sazon.app.domain.image`
  (puerto `DishImageGenerator` + `DishImageRequest`, prompt `DishImagePrompt` y clave canónica
  `DishImageCacheKey`) con 20 tests JVM nuevos, ADR 0005 (**aceptado parcialmente**) y **sin** adaptador,
  red, caché, UI, assets ni dependencias nuevas; quedó **`accepted`** en la **sesión 086**. La **fase 2**,
  **`dish-image-generator`** (`area: integration`, depende de `dish-image-domain`), es la **única**
  entrada del plan con trabajo reciente (WIP = 1 por worktree): implementada y autoverificada
  (**`passing`**) en la **sesión 089** y **`accepted`** en la **sesión 090** (validación independiente,
  veredicto `accept`, rama `feature/dish-image-generator`, sin commit/push/PR) — el adaptador
  real `com.sazon.app.data.ai.image.DevExpertDishImageGenerator` (Ktor, `POST /images/generations` con
  alias `image` y `size 1024x1024`, decodificación tolerante de `data[0].b64_json` y reducción local a
  WebP q80 de 960 px, `null` como fallo, **un solo intento** y sin `Log`) con 22 tests JVM nuevos (uno de
  ellos Robolectric en modo gráfico nativo para el códec real); sigue **sin caché, sin UI y sin
  consumidores**. Las fases 3-7 de la propuesta (`dish-image-cache`, `dish-image-progressive-ui`,
  `dish-visual-descriptor`, `dish-style-reference` y `dish-image-seed`) ya están dadas de alta en
  `feature_list.json` como `not_started`.
- Capturas de UI (política del PR #37): **backfill pendiente**; 11 features de UI `accepted` y 18
  pantallas por exportar de Stitch (ver sesión 046 y `docs/design/concepts/<feature-id>/README.md`).
  `results-screen`, `recipe-detail-screen`, `favorites`, `activity-summary` y `ai-search` ya aportan sus
  capturas (`docs/design/concepts/results-screen/resultados/`,
  `docs/design/concepts/recipe-detail-screen/detalle-receta/`,
  `docs/design/concepts/favorites/favoritos/`,
  `docs/design/concepts/activity-summary/perfil-resumen/` y
  `docs/design/concepts/ai-search/busqueda-ia/`).
- Capturas de UI — cambio de política del harness (2026-10-11, sesión 055): en `github-flow`, una
  feature de UI sin capturas ya **no** bloquea sin crear nada ni ensucia el checkout base. `execute`
  **provisiona** la feature dentro de su propio worktree (issue, rama `feature/<id>` y worktree) y deja
  allí `docs/design/concepts/<feature-id>/` con un `README.md` (pantallas y ficheros esperados) y un
  `stitch-prompt.md` (prompt listo para pegar en Stitch, derivado de `docs/design/stitch/`), **sin**
  lanzar sesión. La feature queda `blocked`/`provisioned` hasta que la persona coloca las capturas en el
  worktree y re-ejecuta `github-flow execute <feature-id>`.
- Propuesta post-MVP documentada (no aceptada, no planificada, fuera del MVP):
  `docs/backend-proposal.md` — backend Kotlin + Ktor (opción B) para cuenta, sincronización
  offline-first y proxy de IA, con módulo de dominio compartido. No altera el estado de ninguna feature
  ni el MVP local-first; queda para retomar en una fase posterior.
- Última feature **`accepted`**: **`favorites-sort-controls`** (issue #85, `area: ui`), cerrada en la
  sesión 082 con validación independiente (veredicto `accept`, rama `feature/favorites-sort-controls`; la
  sesión 081 la dejó `passing`). Es una **correctiva de `favorites`**: el chip de orden por defecto pasa
  de "Añadidas recientemente" a **"Recientes"** (dejaba de recortarse) y los dos chips de texto
  "Lista"/"Rejilla" se sustituyen por el **toggle compacto icónico** de la captura aceptada
  (`docs/design/concepts/favorites/favoritos/code.html` líneas 89-97): píldora `surface` con dos botones
  circulares de 28dp solo con icono (activo `primary`/`onPrimary`, inactivo sin relleno con
  `onSurfaceVariant`), con los glifos `ListGlyph`/`GridGlyph` dibujados con `Canvas` (el repo no tiene
  `material-icons`). Solo cambia el literal del label de `FavoritesSort.RECENT` (key/entries/fromKey/
  `Saver` intactos) más la capa Compose; sin lógica nueva, sin dependencias y sin tocar dominio,
  persistencia, `ui.theme` ni el resto de la pantalla. La desviación de capturas está autorizada
  (issue #85, misma clase que #77) y registrada en `DESIGN.md` (microcopy "Recientes" + toggle icónico);
  **no** se crea `docs/design/concepts/favorites-sort-controls/`. Detalle en
  `docs/validations/favorites-sort-controls.md`. WIP = 0. También quedó **`accepted`** la correctiva
  **`recipe-photos`** (issue #83, `area: ui`, correctiva de `recipe-artwork`), cerrada en la sesión 082
  con validación independiente (veredicto `accept`, rama `feature/recipe-photos`): las tarjetas y el
  Detalle muestran **fotos de plato** como **assets offline** versionados (31 ficheros: 12 semillas + 18
  categorías + `default`; 5,81 MiB), con cadena de respaldo pura `seed → category → default → null`
  (`data.RecipePhotoAssets`), una costura `RecipePhotoSource` (`LocalRecipePhotoSource` +
  `AssetRecipePhotoSource` con `AssetManager`/`BitmapFactory` en `Dispatchers.IO` y LRU 16) cableada en
  `MainActivity`, y `RecipeArtwork` con foto (`Image(ContentScale.Crop)`) dejando el degradado + glifo
  como último recurso (firma/geometría intactas). ADR nuevo `docs/adr/0004-recipe-photos-as-assets.md`;
  sin dependencias nuevas. Antes,
- **`recipe-artwork`** (issue #77, `area: ui`), cerrada en la sesión 078
  con validación independiente (veredicto `accept`, rama `feature/recipe-artwork`; la sesión 077 la dejó
  `passing`). Es la primera feature del plan posterior al MVP ya cerrado. Sustituye las **4 copias** del
  placeholder de receta
  (degradado melocotón→oliva + `PlateGlyph` duplicado a mano) por un **único componente**
  `ui.artwork.RecipeArtwork` con mapeo **puro** (`RecipeArtworkStyle`: glifo por `categoryId` —18
  categorías reales—, acento del degradado por `cuisineId` —10 cocinas reales—) y **fallback total**
  (id nulo/vacío/desconocido → plato + degradado por defecto). Sin dependencias nuevas, sin assets, sin
  hex sueltos (roles del tema) y con la geometría aceptada intacta (16:10, 96x96, alto 96, hero 240;
  glifos 48/56/40/40/72 dp). La desviación de capturas está autorizada (issue #77) y registrada en
  `DESIGN.md`; **no** se crea `docs/design/concepts/recipe-artwork/`. WIP = 0. Antes:
  la cuarta **correctiva** de `real-ai-provider`, `ai-response-format` (issue #74, `area: integration`,
  no UI), quedó **`accepted`** en la sesión 076 (validación independiente, veredicto `accept`, rama
  `feature/ai-response-format`; la sesión 074 la dejó `passing` y la 075 la reparó tras un `revise`).
  Envía `response_format {"type":"json_object"}` en el alias `chat`, con un interruptor
  (`AiProviderPolicy.RESPONSE_FORMAT_ENABLED`), un **fallback one-shot** sin `response_format` solo ante
  400/422 (respetando `MAX_ATTEMPTS = 2`) y observabilidad del modo/fallback por
  `AiCallEvent`/`Log.d("SazonAi")`; `RecipeValidator` sigue intacto como puerta dura. Antes,
  `ai-call-diagnostics` (issue #72) quedó `accepted` en la sesión 073. `ai-recipe-decode` (issue #70) en
  la 071 y `ai-provider-timeouts` (issue #68) en la 069. El resto del plan sigue cerrado. WIP = 0.
  Después se añadió la ampliación de UI **`cooking-mode`** (issue #76, "Modo cocina con temporizadores"),
  que quedó `blocked`/provisioned por falta de capturas, se reanudó cuando la persona dejó
  `docs/design/concepts/cooking-mode/modo-cocina/` (`screen.png`, `code.html`, `DESIGN.md`), y en la
  **sesión 077** el implementer la dejó **`passing`** (implementada y autoverificada) hasta que la
  **sesión 078** la cerró **`accepted`** con validación independiente (rama `feature/cooking-mode`).
  El Modo cocina es ya real: lógica pura `ui.cooking` (máquina del temporizador con `tick`, paso actual,
  navegación y marcado) + `CookingModeScreen` como overlay encima del Detalle, cableando el
  `onStartCooking` que antes era un no-op; sin notificación de sistema ni persistencia (Non-Goals),
  sin dependencias nuevas.
- Feature más reciente — `recipe-photos` (issue #83, `area: ui`): **`passing`** (implementada y
  autoverificada en la sesión 081, rama `feature/recipe-photos`; **pendiente de validación
  independiente**). Las tarjetas y el Detalle muestran **fotos de plato** empaquetadas como **assets
  offline** (31 fotos: 12 `seed/`, 18 `category/cat-`, 1 `default.jpg`; 6.092.271 B ≈ 5,9 MiB ≤ 8 MB) con
  cadena de respaldo determinista (`data.RecipePhotoAssets`: foto propia → categoría → `default` →
  `null`) y `RecipeArtwork` con foto (`Image(ContentScale.Crop)`) y el degradado + glifo de
  `recipe-artwork` como **último recurso**. Costura Android `data.AssetRecipePhotoSource`
  (`AssetManager` + `BitmapFactory`, `Dispatchers.IO`, LRU 16) y seam Compose
  `ui.artwork.RecipePhotoSource` (`LocalRecipePhotoSource`, default nulo) provisto en `MainActivity`; sin
  dependencias nuevas y sin red en runtime. La ampliación del MVP queda registrada en
  `docs/build-brief.md`, `DESIGN.md`, `docs/technical-discovery.md`,
  `docs/risks-and-open-questions.md` y `docs/domain/taxonomy.md` §6, con ADR nuevo
  `docs/adr/0004-recipe-photos-as-assets.md`; `scripts/fetch-recipe-photos.sh` es la herramienta de una
  sola vez **fuera** de `init.sh`. WIP = 1 (es la única entrada no aceptada).
- Cuarta **correctiva** de `real-ai-provider`, `ai-response-format` (issue #74): implementada y
  autoverificada (`passing`) en la sesión 074 por el implementer, rama `feature/ai-response-format`.
  La validación independiente devolvió **`revise`** (Finding 1 Medium: el fallback podía añadir un
  **tercer** intento HTTP) y el implementer la **reparó** en la sesión 075 (sigue `passing`,
  **pendiente de re-validación**). Acota un cambio en `data.ai` + el formato del `Log.d` de
  `MainActivity`: envía `response_format {"type":"json_object"}` en el alias `chat` (no `json_schema`,
  no `chat-pro`), con **fallback one-shot** ante `400`/`422` sin la clave **y solo si queda presupuesto
  de intentos** (nunca supera `MAX_ATTEMPTS = 2`), interruptor
  `AiProviderPolicy.RESPONSE_FORMAT_ENABLED` (por defecto `true`) con costura de constructor,
  observabilidad del modo/fallback sobre `AiCallEvent` (`responseFormat` + `FALLBACK`; `format=…` en el
  log) y `@EncodeDefault(NEVER)` para **nunca** emitir `"response_format":null` pese a
  `encodeDefaults = true`. `Recipe.kt`/`RecipeValidator.kt` intactos (siguen siendo la puerta dura).
- Quinta **correctiva** de `real-ai-provider`, `ai-verbose-diagnostics` (`area: integration`, no UI):
  implementada y autoverificada (`passing`) en la sesión 079 y **`accepted`** en la sesión 080 (validación
  independiente, veredicto `accept`, rama `feature/ai-verbose-diagnostics`). Desglosa `INCOHERENT` en sus
  dos causas del Validador (regla 4a, deriva de tiempos → `TIME_DRIFT`; 4b, cobertura de alérgenos →
  `ALLERGEN_COVERAGE`) siempre activo (`AiIncoherentCause` + `AiCallEvent.incoherentByCause`, `incoherent={…}`
  en el log, sin tocar `DiscardReason`/`RecipeValidator`) y añade un modo de depuración ocultable
  (`verboseDiagnostics`, activo solo en debug vía `BuildConfig.DEBUG`) con el detalle de cada descarte
  (`discard[i/n]`) y el JSON crudo troceado (`raw[i/n]` vía `AiVerboseDiagnostics`, ≤3000 char/trozo),
  apagado por defecto y sin exponer nunca la clave ni `Authorization`. El plan del MVP sigue **cerrado**.
- Correctiva de `activity-summary`, `profile-stats-heading` (issue #86, `area: ui`): implementada y
  autoverificada (`passing`) en la sesión 083 y **`accepted`** en la sesión 084 (validación independiente,
  veredicto `accept`, rama `feature/profile-stats-heading`). El titular visible del bloque de cifras del
  Perfil pasa de "Resumen" a **"Estadísticas"** (solo microcopy; `ActivitySummarySection.kt` + el KDoc de
  `ProfileScreen.kt`) para explicitar que las dos tarjetas ("recetas generadas" y "favoritas") son
  estadística informativa y **no navegable**; sin cambios de números, layout, tokens ni orden y sin añadir
  `clickable`/`onClick`. La desviación de capturas está autorizada (issue #86, misma clase que
  `recipe-artwork` #77) y registrada en `DESIGN.md`; **no** se crea
  `docs/design/concepts/profile-stats-heading/`. WIP = 0.
- Última feature **`accepted`** — **`home-recently-opened-group`** (issue #91, `area: ui`, correctiva de
  `home-offline-blocks`, pedida por la persona): implementada y autoverificada (**`passing`**) en la
  sesión 085 y **`accepted`** en la sesión 086 (validación independiente, veredicto `accept`, rama
  `feature/home-recently-opened-group`; ver `docs/validations/home-recently-opened-group.md`). El
  bloque "Tus recetas" de la Home **solo pinta los grupos con contenido** (hoy, "Guardadas") y su
  `HomeRecipesGroup` ya no pinta ni título ni carrusel sin recetas; la costura muerta
  `HomeRecipes.recentlyOpened` (sin productor: `MainShell` la rellenaba con `emptyList()`) se **retira**
  y `HomeRecipesTest` queda en 7 casos (net −1). `DESIGN.md` registra la estructura vigente y la
  desviación de capturas (issue #91, misma clase que #77/#85/#86); no se crea
  `docs/design/concepts/home-recently-opened-group/`. Sin dependencias nuevas. WIP = 0.
- Bloqueo actual: ninguno. Queda la **verificación manual** de la persona en el emulador de Android
  Studio (Windows): con clave válida, generar/buscar y comprobar en logcat (`SazonAi`) el detalle
  enriquecido por llamada (`content=…c/…b`, `received=… accepted=…`, `discards={…}`) y el modo de
  formato (`format=JSON_OBJECT`/`NONE`, y la línea `FALLBACK` si el proveedor rechaza `json_object`);
  con un lote sin recetas aceptadas, que el log indique el motivo; con `RESPONSE_FORMAT_ENABLED=false`
  el comportamiento es idéntico al actual (sin `response_format`); con la clave ausente o inválida,
  modo semilla sin fallar. No es automatizable desde WSL (el `adb` de WSL no ve el emulador). Para
  `recipe-artwork` queda también la **revisión manual** de emulador (Home, Resultados, Favoritos
  lista+rejilla y Detalle, en claro y oscuro) del acabado de los 18 glifos y los 11 degradados, contra
  el registro de `DESIGN.md`; un defecto visual se corrige con una feature correctiva. Con la
  misma limitación queda la **verificación manual** de `cooking-mode`: abrir una receta, pulsar
  "Empezar a cocinar" y recorrer los pasos con "Siguiente"/"Anterior"/"Terminar", con el temporizador
  (Iniciar/Pausar/Reanudar/Reiniciar y el aviso "¡Listo!" al llegar a cero). Con la misma limitación
  queda la **verificación manual** de `recipe-photos`: abrir Home, Resultados, Favoritos (lista y
  rejilla) y Detalle en claro y oscuro y confirmar que todas las tarjetas muestran foto, que una receta
  generada por IA muestra la de su categoría, que dos recetas de la misma categoría con fotos propias no
  se confunden y que (prueba destructiva opcional, quitando un asset) aparece el glifo en vez de un hueco.
- Última feature **`accepted`**: **`dish-image-domain`** (`area: domain`, fase 1 de
  `docs/ai-dish-images-proposal.md`), cerrada en la **sesión 086** con validación independiente (veredicto
  `accept`, rama `feature/dish-image-domain`; la sesión 085 la dejó `passing`). Deja fijada en el dominio,
  **sin red**, la política de las imágenes de plato generadas: puerto
  `domain.image.DishImageGenerator` (`suspend fun generate(DishImageRequest): ByteArray?`, `null` = no
  disponible, sin excepción y con la cancelación propagada) + `DishImageRequest.of(taxonomy, recipe)` como
  único punto de composición, prompt puro `DishImagePrompt` (plantilla
  `<prefijo> <plato>[, <cocina>], <coletilla>`; degradación `título → etiqueta real de categoría →
  plato casero`; cláusula de cocina omitida si el id no existe) y clave canónica versionada
  `DishImageCacheKey` (`dish-image-v1-<slug>` del **nombre del plato**, normalización NFD + `\p{Mn}+` +
  `locale` ROOT + alfabeto `[a-z0-9-]`, con respaldo por `id` y respaldo final `dish-image-v1-plato`).
  Lógica pura en `com.sazon.app.domain.image`, testeada en JVM (20 tests nuevos: `DishImageCacheKeyTest`
  8, `DishImagePromptTest` 7, `DishImageRequestTest` 5); **sin** adaptador real, caché, persistencia, UI,
  assets ni dependencias nuevas (fases 2-7, no-goals). ADR nuevo
  `docs/adr/0005-ai-dish-images.md` ("Aceptado parcialmente (fase 1)"), que registra la desviación
  consciente (normalización tipográfica, **no** semántica) y ajusta parcialmente el ADR 0004;
  `docs/technical-discovery.md` y `docs/risks-and-open-questions.md` actualizados. Detalle en
  `docs/validations/dish-image-domain.md`. WIP = 0. Con esto, **toda** entrada de `feature_list.json`
  queda `accepted`: el plan (MVP + ampliaciones + correctivas) sigue **cerrado** y no hay features
  `not_started`, `in_progress`, `passing` ni `blocked`.
- Última verificación (2026-10-13, `dish-image-domain`, validator, sesión 086): **veredicto `accept`
  independiente** (agente con contexto limpio). Re-ejecutó `bash init.sh` (exit 0, pero todo `UP-TO-DATE`
  por la config cache del implementer) y, para no fiarse, forzó
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (BUILD SUCCESSFUL, 34 tareas ejecutadas),
  parseando por su cuenta los XML frescos (**68 suites / 496 tests / 0 fallos / 0 errores / 0 saltados**;
  `DishImageCacheKeyTest` 8, `DishImagePromptTest` 7, `DishImageRequestTest` 5 = 20 nuevos) y re-derivó la
  baseline en `HEAD` (65 ficheros con `@Test` / 476 anotaciones; 476 + 20 = 496). Lint (SARIF): **0 errores,
  13 warnings** preexistentes, **0 bajo `app/src`**. Verificó por greps propios la **pureza** (`domain.image`
  solo importa `Recipe`, `Taxonomy`, `java.text.Normalizer` y `java.util.Locale`; 0
  `android`/`androidx`/`io.ktor`/`java.net`/`random`/`UUID`/`currentTimeMillis`), que el puerto sigue **sin
  implementación** ni consumidores (**0** referencias a `DishImage` fuera de `domain.image`), la disciplina
  de alcance (solo los 9 ficheros previstos + la spec, `feature_list.json` y `PROGRESS.md`;
  build/lockfile/`init.sh`/`scripts/**`/`data/**`/`ui/**`/dominio ya existente/Taxonomía/
  `seed-recipes.json`/`assets/**`/`DESIGN.md`/`docs/design/**` intactos; **nada** en
  `docs/design/concepts/`) y los 9 escenarios contra los tests, incluidos los negativos (receta vacía,
  `categoryId` desconocido, `Taxonomy` vacía, cocina vacía/inexistente sin marcador, respaldo por `id` sin
  colisión, frontera tipográfica del escenario 8, composición + igualdad estructural + `recipeId = null` y
  clave estable con `Locale.setDefault("tr-TR")`). Sin hallazgos Critical/High/Medium; 3 Low informativos no
  bloqueantes (L1: un título terminado en coma produciría una coma doble, caso no exigido por la plantilla
  de la spec, anotado para la fase 2/4; L2: ADR 0004 sin puntero a 0005, sin contradicción; L3: la baseline
  "64 suites / 456 tests" de la spec es una medición previa a `HEAD`, ya documentada por el implementer).
  Detalle en `docs/validations/dish-image-domain.md`.
- Verificación anterior (2026-10-13, `recipe-photos`, validator, sesión 082): **veredicto `accept`
  independiente** (agente con contexto limpio). Re-ejecutó `bash init.sh` (exit 0) y, al verlo
  `UP-TO-DATE`, forzó `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (BUILD SUCCESSFUL 2m42s) y
  `bash scripts/gradle.sh lint --rerun-tasks` (2m40s); parseó por su cuenta los XML (**65 suites / 467
  tests / 0 fallos / 0 errores / 0 saltados**; `RecipePhotoAssetsTest` 11, `RecipeArtworkStyleTest` 8) y el
  informe de lint (**0 errores, 13 warnings** preexistentes, 0 bajo `app/src`). Confirmó `assembleDebug` +
  inspección del APK (**31 assets** `img/recipes/**`, 6.092.271 B = 5,81 MiB ≤ 8 MB) y recomputó el mapeo
  contra la Taxonomía y el catálogo semilla reales (12 semillas → foto propia, 18 categorías → foto propia,
  `default` → `default.jpg`; `null`/`""`/`"  "`/desconocido → `default`; nada existe → `null`),
  contrastando por **md5** los 31 ficheros descargados de los `src` con los empaquetados (**31/31**
  idénticos, JPEG 1376×768). Sin hallazgos Critical/High/Medium; 5 Low informativos no bloqueantes (KDoc
  de `AssetRecipePhotoSource` dice ≤960 px cuando los assets son 1376×768; el test fija `.jpg` literal en 4
  aserciones; `docs/domain/taxonomy.json` conserva `imageResolution` `.webp` fuera de alcance;
  `docs/technical-discovery.md` ~línea 504 cita solo ADR 0001-0003; el seam Android sin test unitario).
  Detalle en `docs/validations/recipe-photos.md`.
  Para `ai-quantity-tolerance`
  queda también la **prueba manual** de emulador de la persona: con `devexpert.apiKey` válida, buscar
  *"arroz con pollo"* y comprobar que las recetas con ingredientes "al gusto" (p. ej. *perejil fresco*)
  ya **no** caen por `SCHEMA` en el diagnóstico (`SazonAi`) y llegan a Resultados, con "Al gusto" en el
  Detalle; con la clave ausente o vaciada, modo semilla sin fallar (no automatizable desde WSL).
- Última verificación (2026-10-13, `ai-quantity-tolerance`, validator, sesión 082): **veredicto `accept`
  independiente** (agente con contexto limpio). Re-ejecutó `bash init.sh` (exit 0, aunque la primera
  pasada salió `UP-TO-DATE`) y forzó `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (**34/34
  tareas**); parseó por su cuenta los XML (**64 suites / 465 tests / 0 fallos / 0 errores / 0 saltados**;
  `data.ai` = 85) y el informe de lint (**0 errores, 13 warnings** preexistentes, 0 bajo `app/src`).
  Verificador independiente **fuera del repo** (`/tmp`, 28/28 checks contra las clases reales de
  `app/build`): Scenarios 1-7 (nula+`unit` null y nula+`unit` → normaliza a `0.0`/`unit null` y la `unit`
  del wire se descarta; campo ausente idéntico; `200 g`/`1 ud` intactos; lote mixto `accepted==1
  received==2 {SCHEMA:1}`; Validador **no** relajado: `mealTypeId` inexistente → `SCHEMA`, deriva >5 min →
  `INCOHERENT`; `source==AI`, `id` prefijo `ai-`, `matchScore` null), la causa raíz por diferencial real,
  `javap` sobre el bytecode (`AiIngredientDraft.getQuantity()` → `java.lang.Double` anulable vs
  `RecipeIngredient.getQuantity()` → `double` primitivo) y la **estabilidad de ids** (recomputó a mano el
  hash de canonicalización anterior y obtuvo el mismo `ai-<sha256>`; ningún id existente cambia).
  Confirmó que `RecipeValidator` no lee `quantity` (la puerta dura no puede debilitarse), la disciplina de
  alcance, la no-fuga y que el implementer no hizo commit/push/PR. Sin hallazgos Critical/High/Medium; 6
  Low informativos no bloqueantes. Detalle en `docs/validations/ai-quantity-tolerance.md`.
- Verificación anterior (2026-10-13, `ai-quantity-tolerance`, implementer, sesión 081): **trabajo en disco,
  sin commit/push/PR**. `bash init.sh` exit 0 (test + lint vía `scripts/gradle.sh`, sin procesos largos)
  y, antes, tirada focal `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"`
  (BUILD SUCCESSFUL). Parseo propio de los XML del gate: **64 suites / 465 tests / 0 fallos / 0 errores /
  0 saltados** (456 preexistentes + 9 nuevos de `data.ai`: `AiRecipePayloadTest` 22 = 14+8,
  `AiRecipeIdsTest` 7 = 6+1; `data.ai` total 85 = 76+9) y del informe de lint: **0 errores, 13 warnings
  preexistentes** (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2,
  OldTargetApi 1), **0 bajo `app/src`**. Reproducción independiente de la causa raíz con un fichero de
  test temporal (ya eliminado): el wire `{"name":"Perejil fresco","quantity":null,...}` decodificado con
  el tipo de dominio `RecipeIngredient` (no anulable) lanza
  `JsonDecodingException: Failed to parse literal 'null' as a double value at path: $.quantity`
  (`runCatching` → `SCHEMA`, receta entera perdida), mientras con `AiIngredientDraft` (anulable) decodifica
  y `toRecipeIngredient()` normaliza a `RecipeIngredient("Perejil fresco", 0.0, unit=null, ...)`.
  `git diff --stat` solo con lo previsto (2 ficheros de `main` en `data.ai`, 2 tests, 3 docs, la spec y
  `feature_list.json`); `Recipe.kt`/`RecipeValidator.kt`/`AiRecipePayload.kt`/`domain/**`/`ui/**`/build/
  Taxonomía/`seed-recipes.json`/`init.sh` intactos; sin dependencias nuevas; sin capturas (no es UI).
  Docs actualizadas (`docs/technical-discovery.md`, `docs/domain/taxonomy.md` §4, `docs/domain-model.md`).
- Verificación anterior (2026-10-12, `recipe-artwork`, validator, sesión 078): **veredicto `accept`
  independiente** (agente con contexto limpio). Re-ejecutó `bash init.sh` (exit 0) y, al verlo
  `UP-TO-DATE`, forzó `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (34/34 tareas) y
  `bash scripts/gradle.sh lint --rerun-tasks` (30/30 tareas); parseó por su cuenta los XML (**60 suites /
  413 tests / 0 fallos / 0 errores / 0 saltados**; `RecipeArtworkStyleTest` 8) y el informe de lint
  (**0 errores, 13 warnings** preexistentes, 0 bajo `app/src`). Recomputó el mapeo desde el código contra
  la Taxonomía real (18 `recipeCategories` → 18 `RecipeGlyph` distintos == `RecipeGlyph.entries`; 10
  `cuisines` → 10 `RecipeAccent` distintos + `DEFAULT` == `RecipeAccent.entries`; `else`/`other` →
  `PLATE`/`DEFAULT`), confirmó pureza de `ui.artwork`, sin hex sueltos, **0 `PlateGlyph`** en `app/src`,
  5 puntos de llamada con geometría/tamaños preservados, disciplina de alcance (domain/Taxonomía/seed/
  Room/build/`init.sh` intactos, sin dependencias) y que el implementer no hizo commit/push/PR. Sin
  hallazgos Critical/High/Medium; 2 Low informativos no bloqueantes (redacción "vertical" del degradado
  cuando `Brush.linearGradient` es diagonal, idéntico al placeholder aceptado antes; Scenario 6 solo por
  inspección, coherente con AGENTS.md). Detalle en `docs/validations/recipe-artwork.md`.
- Verificación anterior (2026-10-12, `recipe-artwork`, implementer, sesión 077): **trabajo en disco, sin
  commit**. `bash init.sh` exit 0 (`./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`, sin
  procesos largos) y, antes, tirada focal
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.artwork.*"` (BUILD SUCCESSFUL);
  parseo propio de los XML: **60 suites / 413 tests / 0 fallos / 0 errores / 0 saltados** (405 previos +
  8 nuevos de `RecipeArtworkStyleTest`) y del informe de lint: **0 errores, 13 warnings preexistentes**
  (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), **0 bajo
  `app/src`**. `RecipeArtworkStyleTest` itera la Taxonomía real vía
  `System.getProperty("taxonomy.asset.path")`: 18 `recipeCategories` → 18 `RecipeGlyph` distintos
  (== `RecipeGlyph.entries`) y 10 `cuisines` → 10 `RecipeAccent` distintos (+`DEFAULT` ==
  `RecipeAccent.entries`), más determinismo, fallback total (`null`/`""`/`"  "`/desconocido →
  `PLATE`+`DEFAULT`, también con una `Recipe` de ids vacíos) y caso negativo. `grep`: **0 apariciones de
  `PlateGlyph`** en `app/src`; el degradado del placeholder (`primaryContainer` →
  `secondaryContainer`) solo vive en `RecipeArtwork.kt` (el otro `Brush.linearGradient` es el hero
  preexistente de `HomeScreen`, ajeno a la feature: finding anotado en la spec). `RecipeArtworkStyle.kt`
  solo importa `domain.recipe.Recipe` (sin Compose/Android) y `ui/artwork` no tiene hex sueltos.
  Geometría y tamaños preservados (16:10, 96x96, alto 96, hero 240; glifos 48/56/40/40/72 dp); `git
  diff --stat` solo con los ficheros previstos y sin tocar build/lockfile/dominio/Taxonomía/
  `seed-recipes.json`/Room/`ui.theme`/`init.sh`. `DESIGN.md` y `docs/technical-discovery.md`
  actualizados; **no** se crea `docs/design/concepts/recipe-artwork/` (desviación autorizada, issue #77).
- Última verificación (2026-10-12, `cooking-mode`, implementer, sesión 077): **trabajo en disco, sin commit**.
  `bash init.sh` exit 0 (test + lint vía `scripts/gradle.sh`, sin procesos largos); tirada focal
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.cooking.*"` en verde (**29 tests /
  0 fallos**). Parseo propio de los XML del gate: **62 suites / 434 tests / 0 fallos / 0 errores / 0
  saltados** (59 suites / 405 tests previos + 29 nuevos: `CookingTimerStateTest` 11,
  `CookingModeStateTest` 10, `CookingModeFormatTest` 8) y del informe de lint (0 errores, 13 warnings
  preexistentes, 0 bajo `app/src`). `ui.cooking` puro (sin Compose/Android; solo `domain.recipe` y
  `Locale`) + `CookingModeScreen` (única capa Compose); `MainShell` es el único fichero existente
  modificado (overlay `cookingRecipe` encima del Detalle y `onStartCooking` cableado); build/lockfile,
  `domain/**`, `data/**`, Taxonomía, `seed-recipes.json`, `ui/detail/**`, `MainNavigationState.kt`,
  `ServingsStepper.kt`, Room/Perfil, `scripts/**` e `init.sh` intactos; sin dependencias nuevas. Docs:
  `docs/technical-discovery.md`, `DESIGN.md` (`cooking-mode`: `modo-cocina`) y `docs/build-brief.md`
  (ampliación del MVP, issue #76).
- Verificación anterior (2026-10-12, `ai-response-format`, implementer, **reparación del Finding 1**):
  **trabajo en disco, sin commit**. La rama de fallback de `DevExpertRecipeGenerator` ahora exige
  `attempt < AiProviderPolicy.MAX_ATTEMPTS` y se quitó el `!usedFallback` redundante (`includeFormat`
  ya lo implica): un `503`/`IOException` en el 1.er intento (reintento de transporte) seguido de
  `400`/`422` en el 2.º ya **no** genera un 3.er intento HTTP (`calls=2`, `UNAVAILABLE/HTTP_ERROR` con
  el formato del intento, sin evento `FALLBACK`). Test de regresión nuevo
  `DevExpertRecipeGeneratorTest.el_fallback_no_anade_un_tercer_intento` (cubre `503→400` y
  `IOException→422`; los tests del fallback aislado siguen intactos). Tirada focal
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"` en verde (**62 tests / 0
  fallos**, +1); `bash init.sh` exit 0 (test + lint vía `scripts/gradle.sh`); parseo propio de los XML:
  **59 suites / 405 tests / 0 fallos / 0 errores / 0 saltados** (404 previos + 1 nuevo) y del informe de
  lint (0 errores, 13 warnings preexistentes, 0 bajo `app/src`). KDoc del generador y dos docs durables
  (`docs/risks-and-open-questions.md`, `docs/technical-discovery.md`) matizadas para ceñir el fallback
  al presupuesto; sin cambios fuera de `DevExpertRecipeGenerator.kt`, su test y esas docs.
- Verificación anterior (2026-10-12, `ai-response-format`, implementer, sesión 074): **trabajo en disco, sin commit**.
  `bash init.sh` exit 0 (test + lint vía `scripts/gradle.sh`); tirada focal
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"` en verde (61 tests / 0
  fallos). Parseo propio de los XML del gate: **59 suites / 404 tests / 0 fallos / 0 errores / 0
  saltados** (393 preexistentes + 11 nuevos de `data.ai`: `AiProviderPolicyTest` 12,
  `AiRecipeIdsTest` 6, `AiRecipePayloadTest` 11, `DevExpertRecipeGeneratorTest` 30, `OpenAiChatTest` 2)
  y del informe de lint (0 errores, 13 warnings preexistentes, 0 bajo `app/src`, 0 nuevos por
  `@EncodeDefault`/`@OptIn`). `git diff --stat` solo con lo previsto; `Recipe.kt`, `RecipeValidator.kt`,
  `AiRecipePayload.kt`, `AiDecodeResult.kt`, `AiHttpClientFactory.kt`, `domain/**`, `ui/**`, Room,
  Taxonomía, `seed-recipes.json`, build, `init.sh`, `scripts/**`, `DESIGN.md` y `docs/design/**`
  intactos; sin dependencias nuevas; sin capturas (no es UI).
- Verificación anterior (2026-10-12, `ai-call-diagnostics`, validator): veredicto **`accept`** independiente
  tras re-ejecutar `bash init.sh` (exit 0, test + lint vía `scripts/gradle.sh`), forzar
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (34/34 tareas) y
  `bash scripts/gradle.sh lint --rerun-tasks` (30/30 tareas), con parseo propio de los XML
  (**58 suites / 393 tests / 0 fallos/errores/saltados**; 386 preexistentes + 7 nuevos de `data.ai`:
  `AiProviderPolicyTest` 10, `AiRecipeIdsTest` 6, `AiRecipePayloadTest` 11,
  `DevExpertRecipeGeneratorTest` 23) y del informe de lint (0 errores, 10 warnings, todos en el
  build/version catalog, 0 bajo `app/src`). Reprodujo la **regresión por aritmética**: con los valores
  viejos (90/120/180) fallan `SOCKET > peor caso`, `SOCKET ≥ 2×` el peor del alias `chat` y el margen de
  reintento; con 120/150/240 las tres son ciertas. Detalle en `docs/validations/ai-call-diagnostics.md`.
- Verificación anterior (2026-10-12, `ai-recipe-decode`, validator): veredicto **`accept`** independiente
  tras re-ejecutar `bash init.sh` (exit 0, test + lint vía `scripts/gradle.sh`), forzar
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (34/34 tareas) y
  `bash scripts/gradle.sh lint --rerun-tasks` (30/30 tareas), con parseo propio de los XML
  (**58 suites / 386 tests / 0 fallos/errores/saltados**; 376 preexistentes + 10 nuevos de `data.ai`:
  `AiProviderPolicyTest` 10, `AiRecipeIdsTest` 6, `AiRecipePayloadTest` 8,
  `DevExpertRecipeGeneratorTest` 19) y del informe de lint (0 errores, 13 warnings preexistentes, 0 bajo
  `app/src`). Reprodujo el bug y el arreglo de forma independiente con un verificador Java fuera del
  repo (16/16): el mismo JSON literal sin `id`/`source` por el camino de `main` acepta 0 / descarta 1
  como SCHEMA, mientras `AiRecipePayload.decode` acepta 1. Detalle en
  `docs/validations/ai-recipe-decode.md`.
- Verificación anterior (2026-10-12, `ai-provider-timeouts`, validator): veredicto **`accept`** independiente
  tras re-ejecutar `bash init.sh` (exit 0, test + lint vía `scripts/gradle.sh`), forzar
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (34/34 tareas) y
  `bash scripts/gradle.sh lint --rerun-tasks` (30/30 tareas), con parseo propio de los XML
  (**57 suites / 376 tests / 0 fallos/errores/saltados**; 371 preexistentes + 5 nuevos de `data.ai`:
  `AiProviderPolicyTest` 10, `AiRecipePayloadTest` 5, `DevExpertRecipeGeneratorTest` 18) y del informe
  de lint (0 errores, 13 warnings preexistentes, 0 bajo `app/src`). Comprobó con `javap` sobre los jars
  reales la jerarquía de excepciones y la ausencia de otro techo de timeout. Detalle en
  `docs/validations/ai-provider-timeouts.md`.
- Verificación anterior (2026-10-12, `real-ai-provider`, validator): veredicto **`accept`** independiente
  tras re-ejecutar `bash init.sh` (exit 0, test + lint vía `scripts/gradle.sh`),
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` y
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*" checkDebugAarMetadata`,
  con parseo propio de los XML (**57 suites / 371 tests / 0 fallos/errores/saltados**; 28 nuevos de
  `data.ai`: `AiProviderPolicyTest` 8, `AiRecipePayloadTest` 5, `DevExpertRecipeGeneratorTest` 15) y del
  informe de lint (0 errores, 13 warnings preexistentes, 0 en ficheros de la feature). Detalle en
  `docs/validations/real-ai-provider.md`.
- Última feature **`accepted`** — **`dish-image-cache`** (`area: data`, fase 3 de `docs/ai-dish-images-proposal.md`):
  cerrada en la **sesión 091** con validación independiente (veredicto **`accept`**, rama
  `feature/dish-image-cache`; la sesión 091 la dejó `passing` tras el implementer). Es la
  caché de la imagen de plato en `com.sazon.app.data.image`: ficheros WebP en `cacheDir/dish-images` con
  nombre = clave canónica (`<cacheKey>.webp`), tope de **80 MiB con evicción LRU**, LRU de bytes en
  memoria (16), escritura atómica y un `DishImageResolver` que orquesta memoria → disco → generador →
  persistir con `Mutex` por clave. Gate: `bash init.sh` exit 0, **77 suites / 551 tests /
  0 fallos / 0 errores / 0 saltados** (baseline 72/517; delta +5 suites / +34 tests), lint 0 errores y 13
  warnings preexistentes (ninguno bajo `app/src`). Sin Room (decisión de la persona: ficheros), sin UI,
  sin cableado y sin consumidores: la cadena de `recipe-photos` sigue siendo la única resolución en runtime
  hasta la fase 4. Detalle en `docs/validations/dish-image-cache.md`. WIP = 0. La **fase 4**
  (`dish-image-progressive-ui`, `area: ui`) es la siguiente del plan y exige capturas de referencia antes
  de implementar.
- Última feature **`accepted`** — **`dish-image-progressive-ui`** (fase 4, **mitad de integración** de
  `docs/ai-dish-images-proposal.md`, issue #98, `area: integration`): cerrada en la **sesión 092** con
  validación independiente (veredicto `accept`, rama `feature/dish-image-progressive-ui`; la sesión 092
  la dejó `passing`).
- Última feature **`accepted`**: **`dish-image-settings-toggle`** (fase 4, **mitad de UI** de
  `docs/ai-dish-images-proposal.md`, issue #101, `area: ui`), cerrada en la **sesión 093** con
  validación independiente (veredicto `accept`, rama `feature/dish-image-settings-toggle`; la sesión 093
  la dejó `passing`). Aporta el **interruptor** de generación en Perfil ("Imágenes del plato"), el **tope
  por sesión** (12) impuesto por un decorador del puerto y el microcopy puro del tope; en memoria
  (sesión = proceso, sin persistencia), **sin pantalla nueva**, sin dependencias y con **desviación de
  capturas autorizada** registrada en `DESIGN.md` (issue #101, misma clase que #77/#85/#86/#91/#98; **no**
  se crea `docs/design/concepts/dish-image-settings-toggle/`). Gate: `bash init.sh` exit 0, **84 suites /
  593 tests / 0 fallos / 0 errores / 0 saltados** (baseline en `HEAD` 81/562; delta +3 suites / +31 tests),
  lint 0 errores y 13 warnings preexistentes (ninguno bajo `app/src`). Con esto, la **fase 4** de la
  propuesta de imágenes de plato queda **completa** (integración + ajuste de usuario). WIP = 0. Detalle en
  `docs/validations/dish-image-settings-toggle.md`. Queda pendiente la **prueba manual en emulador** de la
  persona tras el merge (apagar el interruptor deja de generar imágenes nuevas, encender las reanuda, el
  tope baja al generar y vuelve a 12 al reiniciar la app), no automatizable desde WSL. Las fases 5-7
  (`dish-visual-descriptor`, `dish-style-reference`, `dish-image-seed`) siguen `not_started`.
- Última feature **`accepted`** — **`dish-visual-descriptor`** (fase 5, variante B de
  `docs/ai-dish-images-proposal.md` §4.1, `area: integration`): cerrada en la **sesión 094** con
  validación independiente (veredicto **`accept`**, rama `feature/dish-visual-descriptor`; la sesión 094
  la dejó `passing` tras el implementer; **sin commit/push/PR del implementer**, el commit lo crea el
  orquestador de `feature-flow` y publica `github-flow`). Fija en el **contrato con la IA** y en la
  **política pura** el descriptor visual **opcional** del plato: el wire puede traer `dishKey` (plato
  canónico = clave de caché compartida) e `imagePrompt` (descripción visual del plato terminado),
  tolerante a tipos inválidos (un `dishKey: 123` u objeto/array **nunca** descarta la receta como
  `SCHEMA`), con valor puro nuevo `com.sazon.app.domain.image.DishVisualDescriptor` (`of`/`isEmpty`/`NONE`),
  parámetros **opcionales con default** en `DishImageCacheKey.of`, `DishImagePrompt.build` y
  `DishImageRequest.of`, `descriptors` en `AiDecodeResult` y los campos en el `OUTPUT_CONTRACT` de
  `PromptBuilder`. `Recipe.kt`, `RecipeValidator.kt` y `AiRecipeIds.kt` **no** cambian (el descriptor es
  cosmético, no se valida ni entra en el `id`) y `DishImageCacheKey.VERSION` **no** sube. **Sin entrega en
  runtime** (non-goal): `MainActivity` sigue componiendo `DishImageRequest.of(taxonomy, recipe)` sin
  descriptor, así que la app sigue con el respaldo por título; se recomienda una feature posterior
  `dish-visual-descriptor-wiring`. Docs durables: §4 y §6 de `docs/domain/taxonomy.md`, ADR 0005
  (**fases 1-5**), `docs/technical-discovery.md` y `docs/risks-and-open-questions.md`. Gate: `bash init.sh`
  exit 0; **86 suites / 619 tests / 0 fallos / 0 errores / 0 saltados** (baseline en `HEAD` 84/593; delta
  +2 suites / +26 tests), lint 0 errores y 13 warnings preexistentes (ninguno bajo `app/src`). Sin capturas
  (`docs/design/concepts/dish-visual-descriptor/` **no** existe). Detalle en
  `docs/validations/dish-visual-descriptor.md`. **WIP = 0.** En el plan quedan las fases 6-7
  (`dish-style-reference`, `dish-image-seed`, `not_started`).
- Última feature **`accepted`** — **`dish-style-reference`** (fase 6, variante C de
  `docs/ai-dish-images-proposal.md` §4.2, `area: integration`): cerrada en la **sesión 095** con
  validación independiente (veredicto **`accept`**, rama `feature/dish-style-reference`; la sesión 095
  la dejó `passing` tras el implementer). Aporta el **ajuste de estilo con referencia** en
  `/images/edits`: el adaptador `DevExpertDishImageGenerator` envía una petición multipart
  (`model=image-edit`, `prompt`, `size=1024x1024`, parte `image` con los bytes de una **foto de
  referencia aceptada como asset versionado** `img/recipes/default.jpg`) y **degrada a texto→imagen**
  (`/images/generations`, fase 2) solo ante rechazo definitivo (`400/404/405/413/415/422`) o `2xx`
  inutilizable; `null` sin respaldo ante transporte/timeout y `401/403/429/5xx`; sin tercer intento;
  `CancellationException` relanzada; interruptor `DishImagePolicy.STYLE_REFERENCE_ENABLED` (por
  defecto `true`). Valor + política pura nuevos (`DishImageStyleReference`,
  `DishImageStyleReferencePolicy`) y cargador Android *best-effort* (`AssetDishImageStyleReference`);
  `domain.image`, `data.image`, `ui/**`, los assets y `RecipePhotoAssets.kt` sin cambios;
  `DishImageCacheKey.VERSION` sigue `dish-image-v1`; sin dependencias nuevas; **sin capturas** (no es
  UI, `docs/design/concepts/dish-style-reference/` no se crea). Gate: `bash init.sh` exit 0, **88
  suites / 643 tests / 0 fallos / 0 errores / 0 saltados** (baseline 86/619; delta +2 suites / +24
  tests), lint 0 errores y 13 warnings preexistentes (ninguno bajo `app/src`). Detalle en
  `docs/validations/dish-style-reference.md`. **WIP = 0.** En el plan queda la fase 7
  (`dish-image-seed`, `not_started`) y, como seguimiento no bloqueante previo,
  `dish-visual-descriptor-wiring` (`not_started`). Queda la **prueba real opcional** (~2 % de cuota,
  comparar con la variante A) y la revisión visual de la persona, no automatizables desde WSL.
- Última feature **`accepted`** — **`dish-image-seed`** (fase 7, la **última** de
  `docs/ai-dish-images-proposal.md` §5, `area: data`, depende de `dish-image-domain`): cerrada en la
  **sesión 096** con validación independiente (veredicto **`accept`**, rama `feature/dish-image-seed`; la
  sesión 096 la dejó `passing` tras el implementer y el validator la aceptó; el commit lo crea el
  orquestador de `feature-flow` y publica `github-flow`, **sin** commit/push/PR del implementer). Los
  **6 platos frecuentes del demo**
  viajan **preparados una vez** y versionados como fotos WebP de 960 px en
  `app/src/main/assets/img/dishes/seed/<clave canónica>.webp` (6 × ~73 KiB = 439 KiB ≤ 2 MiB) y se
  resuelven por la **clave canónica del plato** (`DishImageCacheKey`, p. ej.
  `dish-image-v1-arroz-con-pollo-criollo`) en un nivel **nuevo** de `DishImageResolver` **entre el disco
  y el generador** (memoria → disco → semilla → generador → persistir): el acierto de semilla se cachea
  en **memoria** y se devuelve **sin** escribir en `cacheDir` y **sin** invocar al generador (0 cuota,
  0 red, 0 espera: **ni la primera vez**), detrás del mismo gate del interruptor de la fase 4 y **sin**
  consumir el tope por sesión. Mapa puro `data.DishImageSeedAssets` (ROOT `img/dishes/seed`, orden
  `webp → jpg → png`, saneado `[a-z0-9-]+`, `exists` inyectado) + seam `data.DishImageSeedSource` +
  costura Android `data.AssetDishImageSeedSource` (único fichero nuevo con `android.*`; `null` sin
  lanzar) y **una línea** en `MainActivity`; `app/build.gradle.kts` publica
  `dish.image.seed.assets.dir` a los tests. `scripts/prepare-dish-image-seed.sh` es la herramienta de
  **una sola vez** (fuera de `init.sh` y de runtime; modo proveedor `/images/edits` con respaldo
  acotado **por estado** a `/images/generations`, o `--source <dir>` **sin cuota**; idempotente,
  *magic bytes*, WebP q80 ≤960 px, inventario, ≤2 MiB y `exit ≠ 0` ante cualquier desvío).
  `DishImageCacheKey.VERSION` sigue `dish-image-v1`; dominio, `data.ai.image`, la caché, `ui/**`, la
  cadena de `recipe-photos`, la Taxonomía, `seed-recipes.json` e `init.sh` **sin cambios**; sin
  dependencias nuevas y **sin** `docs/design/concepts/dish-image-seed/` (no es UI). Gate: `bash init.sh`
  exit 0, **90 suites / 660 tests / 0 fallos / 0 errores / 0 saltados** (baseline en `HEAD` 88/643;
  delta **+2 suites / +17 tests**), lint 0 errores y 13 warnings preexistentes (ninguno bajo `app/src`),
  APK de debug con los 6 assets. Detalle en `docs/validations/dish-image-seed.md`. **WIP = 0.** Con esto
  la **lista de fases de la propuesta queda completa** (1-7); como seguimiento no bloqueante queda
  `dish-visual-descriptor-wiring` y la corrección de redacción del **Low** de la validación en
  `docs/technical-discovery.md`.
- Coste documentado (preparación, **una sola vez**): el modo proveedor gastó **6 generaciones** (~2 %
  de cuota cada una, ~12 % de la cuota semanal) al empaquetar el set; en runtime esos platos **no**
  gastan cuota ni esperan red. El mismo script acepta `--source <dir>` para prepararlo sin gastar
  cuota.
- Feature **aceptada**: **`dish-visual-descriptor-wiring`** (`area: integration`, `depends_on:
  dish-visual-descriptor`), la **entrega en runtime** del descriptor visual de la fase 5: queda
  **`accepted`** (implementada y autoverificada en la **sesión 097** y validada de forma independiente
  por el `validator` en la **sesión 097**, veredicto **`accept`**, rama
  `feature/dish-visual-descriptor-wiring`, **sin commit/push/PR del implementer**; el commit local lo
  crea el orquestador de `feature-flow` y la publicación queda para `github-flow`). Conecta
  `AiDecodeResult.descriptors` con el único punto de composición de
  la imagen: costura aditiva `onDescriptors` (default no-op) en `DevExpertRecipeGenerator`, almacén de
  sesión **puro JDK en memoria** `data.image.DishVisualDescriptorStore` (`record`/`descriptorFor`) y
  `MainActivity` componiendo `DishImageRequest.of(taxonomy, recipe, descriptorFor(recipe.id))`, de modo
  que el `dishKey` del modelo es la clave canónica compartida y el `imagePrompt` el prompt; sin
  descriptor (semilla, restaurado, `id` desconocido) la composición es **exactamente** la de la fase 1.
  Sin persistencia (efímero por sesión), sin UI/capturas, sin dependencias nuevas y sin tocar `Recipe`,
  `RecipeValidator` ni `AiRecipeIds`; `DishImageCacheKey.VERSION` sigue `dish-image-v1`. Gate: `bash
  init.sh` exit 0, **92 suites / 673 tests / 0 fallos / 0 errores / 0 saltados** (baseline en `HEAD`
  90/660; delta +2 suites / +13 tests), lint 0 errores y 13 warnings preexistentes (ninguno bajo
  `app/src`). Detalle de la validación en
  `docs/validations/dish-visual-descriptor-wiring.md`.
- Última feature **`accepted`** — **`home-suggestions-personalized`** (issue #110, `area: ui`, correctiva
  de `home-offline-blocks` pedida por la persona): implementada y autoverificada (**`passing`**) en la
  sesión 098 y **`accepted`** en la sesión 099 (validación independiente, veredicto `accept`, rama
  `feature/home-suggestions-personalized`; el commit local lo crea el orquestador de `feature-flow` y la
  publicación queda para `github-flow`, **sin** commit/push/PR del implementer). El carrusel "Sugeridas
  para ti" ya **no** consume el Lote tal cual: `com.sazon.app.ui.home.HomeSuggestions.visible(batchRecipes,
  seedRecipes, preferences, taxonomy)` (puro, `MAX_VISIBLE = 6`) hace `batchRecipes ?: seedRecipes` →
  **filtro de seguridad** `RecipeValidator.validateRecipe(...) is Accepted` (patrón de dieta +
  restricciones duras, aplicado a semilla y a `ai`) → **ranking** por `MatchScore.of(...)`
  (`sortedByDescending`, estable en empates, score calculado una sola vez) → `take(6)`; **sin**
  `copy(matchScore = …)`, así que las tarjetas conservan su `matchScore` original (hoy `null`) y la
  píldora de `RecipeCard` **no** se pinta (**sin cambio visual**: mismo título "Sugeridas para ti" +
  microcopy "basado en tus gustos", mismas tarjetas e imágenes). El **respaldo de semilla offline**
  (`null`) pasa por el **mismo camino** (filtro → orden → tope) y un Lote resuelto **vacío**
  (`emptyList()`) sigue vacío (no cae a semilla). Cableado en `ui/MainShell.kt` con
  `remember(batch?.recipes, seedRecipes, profile.preferences, taxonomy)`. Exención de capturas autorizada
  (issue #110, misma clase que #77/#85/#86/#91/#98): **no** se crea
  `docs/design/concepts/home-suggestions-personalized/`; `DESIGN.md` registra la excepción y el
  comportamiento vigente. Deuda de seguridad registrada (no se arregla aquí): el modo degradado de
  Resultados (`ResultsStatus.Unavailable`) sigue puntuando/ordenando **sin** filtrar; los fixtures
  incoherentes de `ResultsStatusTest` impiden filtrar sin vaciarlo → correctiva recomendada
  `results-seed-safety-filter` en `docs/risks-and-open-questions.md`. Gate: `bash init.sh` exit 0, **92
  suites / 683 tests / 0 fallos / 0 errores / 0 saltados** (baseline re-derivada en `HEAD` 92/673; delta
  real **+10 tests / 0 suites**; `HomeSuggestionsTest` 3 → 13), lint 0 errores y 13 warnings preexistentes
  (ninguno bajo `app/src`). Sin dependencias nuevas, sin tocar build/dominio/persistencia/`ui/results`.
  Detalle en `docs/validations/home-suggestions-personalized.md`. **WIP = 0**: todas las entradas de
  `feature_list.json` vuelven a estar `accepted`; queda `seed-catalog-50` (`not_started`, depende de esta).

- Última feature **`accepted`** — **`dish-image-prompt-v2`** (`area: integration`, issue #111, correctiva
  pedida por la persona tras probar imágenes que no cuadran y una repetida tres veces): implementada y
  autoverificada (**`passing`**) en la **sesión 098** por el implementer y cerrada **`accepted`** en la
  **sesión 098** con validación independiente (veredicto **`accept`**, rama
  `feature/dish-image-prompt-v2`; **sin** commit/push/PR del implementer: el commit local lo crea el
  orquestador de `feature-flow` y publica `github-flow`). Es la **primera** aplicación de la regla durable del ADR 0005 (subir
  `DishImageCacheKey.VERSION` al cambiar la plantilla): (1) `DishImagePrompt` v2 lleva **siempre** el
  título como identidad + el `imagePrompt` del descriptor como **detalle** (ya no lo sustituye) +
  ingredientes clave (≤5, no despensa, distintos y en orden) + `COMPOSITION` + `STYLE_SUFFIX` sin
  negativos + `NEGATIVES` reforzados, con segmentos no vacíos unidos por `", "` y colapso de comas
  repetidas; (2) `DishImageCacheKey.VERSION = "dish-image-v2"` con el **renombrado** (`git mv`, bytes
  idénticos, **sin cuota**) de los 6 assets semilla a `dish-image-v2-*.webp` y `EXPECTED_SEED_KEYS` del
  script en v2 (espejo del prompt acotado a título+cocina+composición+estilo+negativos, con la deriva
  anotada); (3) el contrato de `PromptBuilder` pide un `imagePrompt` más rico; (4) **diagnóstico** del
  origen resuelto (`data.image.DishImageOrigin` + `DishImageOriginSink`, `onResolved` aditivo con origen
  recordado por clave, `RecipeArtworkImagePrecedence.originOf` y el seam
  `ui.artwork.DishImageOriginDiagnostics`) emitido desde `MainActivity` **solo** con `BuildConfig.DEBUG`
  (`Log.d("SazonDishImage", …)`), **sin** cambiar el pintado, la caché, el tope ni la red. Gate:
  `bash init.sh` exit 0; **93 suites / 687 tests / 0 fallos / 0 errores / 0 saltados** (baseline en
  `HEAD` 92/673; delta **+1 suite / +14 tests**), lint 0 errores y 13 warnings preexistentes (ninguno
  bajo `app/src`); sin dependencias nuevas y sin capturas (`docs/design/concepts/dish-image-prompt-v2/`
  **no** existe). Docs durables: ADR 0005, `docs/technical-discovery.md`, §4/§6 de
  `docs/domain/taxonomy.md` y `docs/risks-and-open-questions.md`. Validación independiente (`validator`,
  sesión 098, contexto limpio): veredicto **`accept`**; re-ejecutó `bash init.sh` (exit 0) y forzó
  `testDebugUnitTest --rerun-tasks` (5m13s) y `lint --rerun-tasks` (4m31s), parseó sus propios XML
  (**93 suites / 687 tests / 0 fallos / 0 errores / 0 saltados**) y el SARIF (0 errores, 13 warnings
  preexistentes, 0 bajo `app/src`), re-derivó la baseline (92/673) y confirmó la invalidación (0
  `dish-image-v1`), los 6 assets con md5 idénticos, el diagnóstico solo debug y la disciplina de alcance.
  Sin hallazgos Critical/High/Medium; 2 Low informativos no bloqueantes. Detalle en
  `docs/validations/dish-image-prompt-v2.md`. **WIP = 0** (ninguna entrada no aceptada). Queda la
  **prueba manual** de la persona tras el merge (logcat `SazonDishImage` con
  `GENERATED`/`SEED`/`CATEGORY`) y el **pendiente separado** del tope de 12 por sesión / manejo del
  `429`.
- Última feature **`accepted`** — **`dish-artwork-as-card-background`** (`area: integration`, **cuarta**
  feature del ADR 0006, variante **L2**): implementada y autoverificada (**`passing`**) en la **sesión 109**
  y **`accepted`** en la **sesión 110** (validación independiente, veredicto **`accept`**, rama
  `feature/dish-artwork-as-card-background`; el commit local lo crea el orquestador de `feature-flow` y la
  publicación queda para `github-flow`, **sin** commit/push/PR del implementer). La imagen generada pasa a
  **capa de fondo opcional** del hueco de la tarjeta y del hero del Detalle: `RecipeArtwork` deja de ser
  "la imagen de la receta" y pasa a ser el **proveedor de la capa de fondo** (imagen con `ContentScale.Crop`
  y alfa animado + `scrim` de contraste encima de la imagen y debajo de la tipografía; firma
  `(recipe, modifier, glyphSize)`, geometría y `glyphSize` reservados, slot de contenido aditivo) y deja de
  leer `LocalRecipePhotoSource` y de pintar el degradado/glifo de respaldo; `RecipeArtworkImagePrecedence`
  queda en **dos** capas (`TYPOGRAPHIC`/`GENERATED`, `DishImageOrigin.CATEGORY` sin productor) y aparecen la
  lógica pura `ui/artwork/DishImageArtworkOverlay.kt` (`HOLE_RATIO = 16f/9f`, `headingOnImage`,
  `watermarkVisible`, `crossfadeAlpha`) y el único hogar de tokens `ui/theme/CardImageOverlay.kt`
  (`scrim` claro/oscuro y texto sobre imagen). `RecipeFicha` gana `imageBackground: Boolean = false`
  (`true` = hueco **16:9** + `RecipeArtwork` + **banda sólida** con los tres ingredientes con cantidad y
  metadatos; `false` = ficha aceptada idéntica) y el flag se reparte desde
  `MainShell`→`HomeScreen`/`FavoritesScreen` y, por un **hallazgo de alcance** registrado en la spec,
  también por `GeneratorFlow`/`AiSearchFlow` hasta `ResultsScreen`; `DetailHero` usa la misma capa con
  `HERO_HEIGHT_DP = 240` y la tira de metadatos intactas. Apagado = **0** consultas (ni caché, ni set
  semilla, ni generador) y fallo/timeout/cancelación/tope agotado → ficha tipográfica silenciosa; el
  **interruptor** y el **tope por sesión** (`dish-image-settings-toggle`) no se tocan y el fundido sigue en
  `DishImageArtworkPolicy.CROSSFADE_MILLIS` (250). Gate: `bash init.sh` exit 0, **98 suites / 742 tests /
  0 fallos / 0 errores / 0 saltados** (baseline re-derivada en `HEAD` 96/730; delta **+2 suites / +12
  tests**: `DishImageArtworkOverlayTest` 5, `RecipeBackgroundGateTest` 6, `RecipeArtworkImagePrecedenceTest`
  5), lint 0 errores y 13 warnings preexistentes (ninguno bajo `app/src`); capturas obligatorias
  (8 ficheros de `docs/design/concepts/dish-artwork-as-card-background/{tarjeta-con-imagen,detalle-con-imagen}/`)
  aportadas por la persona, de solo lectura y sin modificar; sin dependencias nuevas y sin diff en
  `domain/**`, `data/**`, `assets/**`, build/lockfile, `init.sh` ni `scripts/**`. Docs durables: `DESIGN.md`,
  `docs/technical-discovery.md`, `docs/adr/0006` (cuarta feature implementada), `docs/domain/taxonomy.md` §6
  y `docs/risks-and-open-questions.md`. Detalle en
  `docs/validations/dish-artwork-as-card-background.md`. **WIP = 0.** Nueva incógnita abierta (no
  bloqueante): el **transitorio** de 250 ms cambia el tratamiento y la marca de agua de forma instantánea
  mientras el alfa anima; la revisión visual de la persona (variantes estrecha y compacta, extrapoladas sin
  captura, y contraste del título sobre una foto arbitraria) sigue pendiente tras el merge. La quinta feature
  del ADR 0006, **`retire-recipe-photos`** (`area: data`), queda desbloqueada y es la siguiente del plan.

## Registro de sesiones

> Las sesiones **001–045** están archivadas en [`docs/progress/sessions-001-045.md`](docs/progress/sessions-001-045.md).
> Aquí se conserva el estado actual y las sesiones recientes (046–…).

### Sesión 046

- Fecha: 2026-10-09
- Objetivo: **chore de harness** (no es feature de producto): **Fase 1** del backfill de capturas de UI
  exigidas por la política del PR #37, aplicada a las 11 features de `area: ui` ya `accepted` (que se
  implementaron antes de que existiera la política). Rama `chore/ui-captures-backfill`.
- Completado:
  - Convención de guardado actualizada en `docs/design/concepts/README.md`: **una subcarpeta por
    pantalla** (no por componente) con los **tres ficheros del export de Stitch** (`.png`, `.html` y
    `design.md`, con los nombres que dé Stitch).
  - **11 `docs/design/concepts/<feature-id>/README.md`** nuevos (la "petición" de cada feature) con la
    lista exacta de pantallas esperadas y una línea de qué es cada una: `design-tokens-and-theme`
    (`design-system`); `onboarding-shell-and-identity` (`bienvenida`);
    `onboarding-diet-style-cuisines` (`dieta`, `estilo`, `cocinas`);
    `onboarding-ingredients-safety-kitchen` (`ingredientes`, `alergias`, `tu-cocina`);
    `home-offline-blocks` (`home`, `home-sin-conexion`); `local-result-filters`
    (`filtros-tus-recetas`); `generator-ingredients-step` (`generador-ingredientes`);
    `generator-settings-step` (`generador-ajustes`); `generation-flow-with-validation`
    (`generador-cargando`, `resultados-preview`); `profile-screen` (`perfil`, `perfil-editar`);
    `manual-suggestions-refresh` (`perfil-sugerencias`). Total: **18 pantallas**.
  - `DESIGN.md`: sección "Generated Concept Images" registra el backfill y las pantallas por feature
    (la petición concreta vive en cada `README.md`).
  - **11 specs**: la sección "Visual Design Impact" nombra sus capturas y el impacto en
    `docs/design/concepts/` pasa de `not needed` a `update` (backfill).
- Verificación ejecutada: cambio **solo de documentación** (ningún fichero Kotlin/recursos/gradle), por
  lo que el gate `init.sh` no aplica; revisión manual del árbol `docs/design/concepts/`, de los 11
  `README.md` y de `git status` (24 ficheros, +358/−40).
- Ficheros creados/cambiados: `docs/design/concepts/README.md`, los 11
  `docs/design/concepts/<feature-id>/README.md` (nuevos), `DESIGN.md`, los 11 `docs/specs/<feature>.md`
  y este registro. `feature_list.json` **sin cambios** (ninguna feature cambia de estado).
- Hallazgos relevantes: las 11 features de UI aceptadas no tienen capturas porque son anteriores al
  PR #37; sus specs incluso decían `docs/design/concepts/: not needed`. El backfill no reabre features
  aceptadas: solo deja la petición y, cuando lleguen los exports, se hará la **Fase 2** de
  reconciliación (UI implementada vs captura) **una a una**, abriendo una feature correctiva solo si
  aparece una desviación visual real.
- Riesgo o incógnita abierta: la Fase 2 no puede empezar hasta que la persona exporte las 18 pantallas
  desde Stitch; no hay bloqueo para el resto del plan (`results-screen` sigue siendo la siguiente
  feature de producto).
- Siguiente paso: commit + push + PR del chore (publicación por `github-flow`). Después, **Fase 2**:
  reconciliar feature por feature según lleguen los exports.

### Sesión 047

- Fecha: 2026-10-09
- Objetivo: **chore de harness**: trocear el prompt de Stitch **por pantalla** para que Stitch no lo
  trunque y genere todas las pantallas (el prompt completo `docs/design/stitch-prompt.md` se truncaba).
- Completado: nueva carpeta `docs/design/stitch/` con `README.md` (guía de uso + mapa parte → carpeta de
  `docs/design/concepts/`) y **21 partes** listas para copiar/pegar de una en una en la misma
  conversación de Stitch: `00-contexto.md` (producto, lenguaje visual, navegación, fuera de alcance y
  calidad, se pega primero), `01-design-system`, 7 pasos de onboarding, `home`, `home-sin-conexion`,
  `home-busqueda-ia`, generador (ingredientes, ajustes, cargando), resultados, detalle, favoritos,
  perfil, perfil-editar y perfil-sugerencias. Referencias actualizadas para apuntar al prompt troceado
  (`docs/design/concepts/README.md` y cabecera de `docs/design/stitch-prompt.md`, que sigue siendo la
  fuente completa de referencia).
- Verificación ejecutada: cambio **solo de documentación** (ningún fichero Kotlin/recursos/gradle), por
  lo que el gate `init.sh` no aplica; revisión manual de los 22 ficheros y de `git status`.
- Ficheros creados/cambiados: los 22 ficheros de `docs/design/stitch/`, `docs/design/concepts/README.md`,
  `docs/design/stitch-prompt.md` y este registro.
- Hallazgos relevantes: el prompt largo se trunca; pegarlo en partes pequeñas y en la **misma** sesión
  (no sesiones distintas) es lo que permite generar todo. La hoja "Design System" es una parte propia.
- Riesgo o incógnita abierta: ninguno nuevo. Las partes de Resultados, Detalle, Favoritos y búsqueda IA
  corresponden a features aún `not_started` (igual que en el prompt original).
- Siguiente paso: la persona pega las partes en Stitch en orden y guarda cada export en
  `docs/design/concepts/<feature-id>/<pantalla>/`; después, **Fase 2** de reconciliación una a una.

### Sesión 048

- Fecha: 2026-10-09
- Objetivo: ejecutar `feature-flow` para la feature `results-screen` (única seleccionada), en modo
  hasta-aceptación. Solo planner/implementer/validator; **sin push ni PR** (publica `github-flow`).
- Completado:
  - Selección: `results-screen` (`area: ui`, `depends_on: generation-flow-with-validation` ya
    `accepted`), sin spec previa. Rol 1 ejecutado: **planner** (`feature-spec`).
  - Spec creada: `docs/specs/results-screen.md` (514 líneas). Fija la fórmula de `matchScore`
    (derivada, en memoria, no persistida; media ponderada de cobertura/gustados/cocina/tiempo/
    dificultad menos penalización por evitados; ejemplo dorado 86/76), los estados poblado/vacío/
    `Unavailable` (con "Reintentar") y el subtítulo de búsqueda. Sustituye y elimina
    `GeneratorResultsPreview`.
  - **Gate de UI**: la spec es de UI y exige capturas de referencia en
    `docs/design/concepts/results-screen/` antes de implementar. No existen. Se crea la carpeta con
    `docs/design/concepts/results-screen/README.md` (petición) y se **detiene** el flujo, marcando la
    feature `blocked` (regla de `feature-flow` y `AGENTS.md`): no se lanza el `implementer`.
- Verificación ejecutada: ninguna de código (no hay implementación). Revisión de artefactos: la spec
  nombra las cuatro pantallas con sus tres ficheros cada una; `docs/design/concepts/results-screen/`
  solo contiene el `README.md` de petición; `git status` sin cambios fuera de la spec, el README y el
  harness.
- Ficheros creados/cambiados: `docs/specs/results-screen.md` (planner),
  `docs/design/concepts/results-screen/README.md` (nuevo), `feature_list.json` (`results-screen` a
  `blocked`) y este registro.
- Hallazgos relevantes: el planner (rol limitado a `docs/specs/`) nombra las capturas pero no crea la
  carpeta; esa petición la materializa el orquestador, como exige el gate de UI. El bloqueo es de
  entrada, no técnico: falta que la persona exporte de Stitch las cuatro pantallas.
- Riesgo o incógnita abierta: **bloqueada** `results-screen` hasta que existan las capturas de
  `docs/design/concepts/results-screen/{resultados,resultados-busqueda,resultados-vacio,resultados-error}/`
  (cada una con `.png`, `.html` y `design.md`). Sigue pendiente además la Fase 2 de reconciliación de
  las 11 features de UI ya aceptadas (sesión 046).
- Siguiente paso: la persona pega en Stitch la parte `resultados` (más los estados vacío/error) y deja
  los exports en `docs/design/concepts/results-screen/`; entonces se desbloquea la feature y el
  orquestador lanza el `implementer`.

### Sesión 049

- Fecha: 2026-10-09
- Objetivo: implementar `results-screen` (pantalla de Resultados real) según
  `docs/specs/results-screen.md`. Sesión reanudada: el trabajo ya existía en disco sin commitear y sin
  los artefactos de harness/docs; esta sesión lo revisa, lo reconcilia con la spec, completa lo que
  faltaba y verifica. El gate de capturas de UI (que bloqueaba la sesión 048) queda satisfecho: existe
  `docs/design/concepts/results-screen/resultados/` con sus tres ficheros.
- Completado: `com.sazon.app.ui.results` como lógica **pura** testeable en JVM más capa Compose delgada.
  `MatchScore.of(...)` fija la fórmula **derivada y no persistida** de `matchScore` (media ponderada
  `Σ(w·f)/Σw` de cobertura/`W_COVERAGE`, gustados/`W_LIKED`, cocina/`W_CUISINE`, tiempo/`W_TIME` y
  dificultad/`W_DIFFICULTY`, menos `DISLIKE_PENALTY` por evitado presente; ejemplo dorado 86/76; sin
  `Random` ni reloj). `ResultsStatus.from(...)` puntúa las aceptadas con `copy(matchScore = ...)` y las
  ordena por relevancia (`Content`/`Empty`/`Unavailable`); `ResultsHeader`/`ResultsOrigin`/`ResultsHeaders.of`
  formatean el encabezado (Generador con singular/plural y franja, o Búsqueda con la consulta entre
  comillas). Compose: `ResultsScreen` (`Scaffold`: topBar "Atrás" + título/subtítulo, bottomBar "Cambiar
  ingredientes", estados poblado/vacío/degradado con "Reintentar") y `ResultRecipeCard` (degradado+glifo
  16:10, título, descripción, meta tabular y píldora de coincidencia; sin corazón ni click). Integración:
  `MainShell` pasa `seedRecipes` a `GeneratorFlow`, que en `result != null` monta `ResultsScreen` con
  `onBack` (→ ajustes), `onChangeIngredients` (→ ingredientes) y `onRetry` (incrementa el token); se
  elimina `GeneratorResultsPreview.kt`.
- Verificación ejecutada: `init.sh` con Git Bash (exit 0, `./gradlew test` + `./gradlew lint`, sin
  procesos largos); antes, `./gradlew testDebugUnitTest --tests "com.sazon.app.ui.results.*"` con parseo
  propio de los XML. Inspección de pureza, del fichero eliminado, de que no cambian build/lockfile/
  dominio/`data`/Taxonomía/`seed-recipes.json`/`MainNavigationState.kt`/`init.sh` y de que no hay hex
  sueltos.
- Evidencia capturada: **255 tests en 39 suites, 0 fallos/errores/saltados** (234 preexistentes + 21
  nuevos: `MatchScoreTest` 8, `ResultsHeaderTest` 7, `ResultsStatusTest` 6); lint 0 errores y 13 warnings
  preexistentes (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2,
  OldTargetApi 1), ninguno en ficheros de la feature; `init.sh` con código 0; `GeneratorResultsPreview.kt`
  eliminado y sin referencias en `app/src`; `MatchScore`/`ResultsStatus`/`ResultsHeader` solo importan
  `domain.*`/`java.text.Normalizer`; sin dependencias nuevas ni cambios en el build, el lockfile, el
  dominio, `data/**`, la Taxonomía, `seed-recipes.json` ni `init.sh`.
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/ui/results/{MatchScore,ResultsStatus,ResultsHeader,ResultsScreen,ResultRecipeCard}.kt`,
  `app/src/test/java/com/sazon/app/ui/results/{MatchScoreTest,ResultsStatusTest,ResultsHeaderTest}.kt`,
  `app/src/main/java/com/sazon/app/ui/generator/GeneratorFlow.kt` y `app/src/main/java/com/sazon/app/ui/MainShell.kt`
  (integración), `GeneratorResultsPreview.kt` (eliminado), `docs/technical-discovery.md`,
  `docs/domain-model.md`, `docs/risks-and-open-questions.md`, `DESIGN.md` (`results-screen/resultados`),
  `feature_list.json` (`results-screen` a `passing`) y este registro.
- Hallazgos relevantes: la fórmula y los estados ya estaban implementados y en verde; no hubo que
  corregir código, solo completar los artefactos de harness/docs y verificar. `./gradlew testDebugUnitTest
  --tests "...ui.results.*"` deja el daemon vivo (el `trap` de `init.sh` lo evita); se paró con `./gradlew --stop`.
- Riesgo o incógnita abierta: falta la **revisión manual** de emulador que `AGENTS.md` reserva a la
  persona (estado vacío; error con "Reintentar"; "Cambiar ingredientes" conservando el borrador; acabado
  contra `docs/design/concepts/results-screen/resultados/`). Sigue pendiente además la Fase 2 de
  reconciliación de capturas de features de UI ya aceptadas (sesión 046).
- Siguiente paso: validación independiente de `results-screen` con `feature-validator`; después, commit
  local (lo hace el orquestador de `feature-flow`) y publicación por `github-flow`.

### Sesión 050

- Fecha: 2026-10-10
- Objetivo: reanudar e implementar `recipe-detail-screen` (Detalle de receta con raciones ajustables)
  según `docs/specs/recipe-detail-screen.md`. Una ejecución previa se interrumpió y dejó el trabajo en
  disco sin commitear y sin los artefactos de harness/docs; esta sesión lo revisa contra la spec,
  completa lo que faltaba (el gate de UI ya estaba satisfecho con las capturas de
  `docs/design/concepts/recipe-detail-screen/detalle-receta/`), corrige una desviación del test de
  escalado y lo autoverifica sin rehacer lo ya válido.
- Completado: lógica **pura** en `com.sazon.app.ui.detail` testeable en JVM (sin Compose ni Android):
  `ServingScaler.scaledIngredients(recipe, targetServings)` con **redondeo por unidad** (no lineal),
  `GRAM_STEP = 5` (`g`/`ml`), `UNIT_STEP = 0.5` (`ud`/`cda`/`cdta`), half-up (`Math.round`), mínimo el
  paso si `quantity > 0`, `unit = null` ("al gusto") **no se escala**, `factor == 1` intacto y sin
  división por cero (`servings.coerceAtLeast(1)`); `IngredientFormat.quantityLabel` (`"½"`, `"4½ ud"`,
  `"300 g"`, `"Al gusto"`, nunca `"0.5"`); `RecipeDetailFormat.metaEntries` (Tiempo/Dificultad real con
  respaldo al `id`/Comensales/Calorías con `"—"` sin nutrición, sin `matchScore`) y `ADJUSTED_CAPTION`;
  `RecipeDetailTab` + `RecipeDetailState` (`initial`/`setServings` con `coerceIn`/`selectTab`, inmutable,
  sin guardar el `Recipe`). Capa Compose **delgada** `RecipeDetailScreen` (héroe degradado + "Atrás" y
  corazón; tarjeta superpuesta con meta; pestañas Ingredientes/Pasos/Notas; `ServingsStepper`
  reutilizado **sin cambios** + caption; filas con checkbox y cantidad en cifras tabulares; timeline con
  píldora ámbar; tips + rejilla de nutrición opcional; barra inferior "Guardar"/"Empezar a cocinar";
  `RecipeDetailStateSaver`). Integración: `RecipeCard`/`ResultRecipeCard` ganan `onClick` opcional;
  `HomeScreen`/`ResultsScreen`/`GeneratorFlow` reciben `onOpenRecipe`; `MainShell` hospeda el detalle
  como **overlay** (`var detailRecipe by remember`) encima de la Home/Generador **sin desmontarlos**;
  `isSaved = false`, `onToggleSave = {}`, `onStartCooking = {}` como costuras no-op de
  `favorites`/modo cocina. Tests JVM con los vectores dorados (escenarios 1-4) y casos negativos.
  Sin dependencias nuevas; no se tocan `app/build.gradle.kts`, `gradle/libs.versions.toml`, `domain/**`,
  `data/**`, la Taxonomía, `seed-recipes.json`, `ui.preferences/ServingsStepper.kt`,
  `ui.navigation/MainNavigationState.kt` ni `init.sh`.
- Verificación ejecutada: `& "C:\Program Files\Git\bin\bash.exe" init.sh` (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos); parseo propio de los XML de resultados
  (`test-results/testDebugUnitTest/TEST-*.xml`) y del informe `lint-results-debug.xml`; inspección de
  imports de la lógica pura (sin Compose/Android) y de `git status` (solo los ficheros previstos).
- Evidencia capturada: **43 suites, 277 tests, 0 fallos/errores/saltados** (22 nuevos:
  `ServingScalerTest` 8, `IngredientFormatTest` 5, `RecipeDetailStateTest` 5, `RecipeDetailFormatTest` 4;
  255 preexistentes en verde); lint 0 errores y 13 warnings preexistentes (GradleDependency 7,
  NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), 0 en ficheros de la feature;
  `init.sh` con código 0; `ServingScaler`/`IngredientFormat`/`RecipeDetailFormat`/`RecipeDetailState`
  sin imports Compose/Android; `ServingsStepper`, `MainNavigationState`, el build/lockfile, el dominio,
  `data/**`, la Taxonomía, `seed-recipes.json` e `init.sh` sin cambios.
- Ficheros creados/cambiados:
  `app/src/main/java/com/sazon/app/ui/detail/{ServingScaler,IngredientFormat,RecipeDetailFormat,
  RecipeDetailState,RecipeDetailScreen}.kt` (nuevos),
  `app/src/test/java/com/sazon/app/ui/detail/{ServingScalerTest,IngredientFormatTest,
  RecipeDetailFormatTest,RecipeDetailStateTest}.kt` (nuevos),
  `app/src/main/java/com/sazon/app/ui/{MainShell.kt, generator/GeneratorFlow.kt, home/HomeScreen.kt,
  home/RecipeCard.kt, results/ResultRecipeCard.kt, results/ResultsScreen.kt}` (modificados),
  `docs/technical-discovery.md`, `docs/domain-model.md`, `DESIGN.md`, `feature_list.json` y este
  registro.
- Hallazgos relevantes: la inspección inicial detectó un **defecto real** en
  `ServingScalerTest.una_receta_sin_raciones_no_divide_por_cero`: con `recipe.servings = 0` y
  `targetServings = 2` el factor es `2` (por `coerceAtLeast(1)`), así que `Patata` queda en `1200 g` y no
  en `600 g` como asertaba el test original; corregido el vector (y añadido `Huevo = 12 ud`) para
  alinearlo con la fórmula de la spec. El test pasó a verde en la siguiente ejecución. El resto de la
  lógica y la capa Compose ya eran correctas.
- Riesgo o incógnita abierta: quedan para revisión manual de la persona tras el merge la prueba de
  emulador exigida (abrir una receta desde Resultados y desde la Home, cambiar las raciones y ver las
  cantidades actualizarse, recorrer las tres pestañas y "Atrás" sin perder resultado/borrador) y el
  acabado visual contra `docs/design/concepts/recipe-detail-screen/detalle-receta/`; un defecto visual se
  corrige con feature correctiva. Rotar con el detalle abierto lo cierra (`detailRecipe` vive en
  `remember`), como documenta la spec (Unknown). Se mantiene la Fase 2 de reconciliación de capturas de
  las features de UI previas (sesión 046).
- Siguiente paso: validación independiente de `recipe-detail-screen` con `feature-validator`; después,
  commit local de la rama (lo hace el orquestador de `feature-flow`) y publicación por `github-flow`. La
  siguiente feature en orden de lista es `favorites` (espera a que `recipe-detail-screen` quede
  `accepted`).

### Sesión 051

- Fecha: 2026-10-10
- Objetivo: validar `recipe-detail-screen` de forma independiente y persistir la aceptación. Ejecutada
  por el orquestador de `feature-flow` en la rama `feature/recipe-detail-screen` (worktree), solo
  hasta-aceptación; sin push ni PR (publica `github-flow`).
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la validación independiente. Sin cambios de código tras la
  validación.
- Verificación ejecutada: el validador re-ejecutó `init.sh` con Git Bash (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos) y, al ver la primera pasada en `UP-TO-DATE`, forzó
  `./gradlew testDebugUnitTest --rerun-tasks` (32/32 tareas) y `./gradlew lint --rerun-tasks`; parseó
  por su cuenta los XML de resultados y el informe `lint-results-debug.xml`, e inspeccionó el código.
- Evidencia capturada: 43 suites, 277 tests, 0 fallos/errores/saltados (22 nuevos: `ServingScalerTest`
  8, `IngredientFormatTest` 5, `RecipeDetailStateTest` 5, `RecipeDetailFormatTest` 4; 255 preexistentes
  en verde); lint 0 errores y 13 warnings preexistentes, 0 en ficheros de `ui.detail`. El validador
  recalculó a mano el escalado contra `seed-recipes.json` real (`seed-tortilla-patatas`, `servings` 4):
  4→2 da `300 g`/`3 ud`/`½ ud`/`75 ml` y `Sal` sin escalar; 4→3 da `450 g`/`4½ ud`/`1 ud`/`115 ml`;
  factor 1 devuelve el original intacto; `unit = null` nunca escala; `servings = 0` no divide por cero;
  determinista. Confirmó pureza de `ui.detail` (sin Compose/Android salvo `RecipeDetailScreen`, sin hex
  sueltos), `ServingsStepper` reutilizado sin cambios, el detalle como overlay en `MainShell` (no un
  `PushedScreen` nuevo) con las costuras no-op, la captura obligatoria `detalle-receta/`
  (`screen.png`/`code.html`/`DESIGN.md`) presente y seguida, y disciplina de alcance (build/lockfile,
  dominio, `data/**`, la Taxonomía, `seed-recipes.json`, `MainNavigationState.kt` e `init.sh` intactos;
  sin dependencias nuevas). Confirmó además que el implementer no hizo commit/push/PR.
- Ficheros actualizados: `feature_list.json` y este registro.
- Hallazgos del validador (2 Low informativos, no bloqueantes): el `README.md` de capturas repite la
  plantilla con `design.md` en minúscula mientras el fichero real es `DESIGN.md` (inocuo en Windows,
  heredado de la plantilla); `RecipeDetailFormatTest.la_caption_del_ajustador_es_estable` es una
  aserción trivial (la cobertura real está en el resto del test). Sin hallazgos Critical/High/Medium.
- Riesgo o incógnita abierta: ninguno nuevo. Quedan para revisión manual de la persona tras el merge la
  prueba de emulador exigida (abrir una receta desde Resultados y desde la Home, cambiar las raciones y
  ver las cantidades actualizarse, recorrer las tres pestañas y "Atrás" sin perder resultado/borrador) y
  el acabado visual contra `docs/design/concepts/recipe-detail-screen/detalle-receta/`; un defecto visual
  se corrige con una feature correctiva. Se mantiene la Fase 2 de reconciliación de capturas de las
  features de UI previas (sesión 046).
- Siguiente paso: commit local de la rama `feature/recipe-detail-screen` (lo hace el orquestador de
  `feature-flow`); la publicación queda para `github-flow`. Después, `favorites` (ya con sus dos
  dependencias `accepted`).

### Sesión 052

- Fecha: 2026-10-10
- Objetivo: implementar `favorites` (Favoritos disponibles sin conexión) según
  `docs/specs/favorites.md`, en la rama `feature/favorites` (worktree); solo hasta implementación y
  autoverificación (`passing`), sin commit/push/PR (los hace el orquestador).
- Completado: dominio puro `com.sazon.app.domain.favorites` (`Favorite` con **copia** de la `Recipe` +
  `savedAt`, el puerto `FavoriteRepository` y `FavoriteService.toggle` con reloj inyectable que
  normaliza `matchScore` a `null`); persistencia Room en `com.sazon.app.data.favorites` sobre base
  **propia** `favorites.db` (version 1, tabla **multi-fila** `favorite_recipe` con la receta como
  columna JSON vía `RecipeJson`, `upsert` idempotente por `recipeId` y orden `savedAt` desc); lógica de
  presentación pura `com.sazon.app.ui.favorites` (`FavoritesSort`, `FavoritesViewMode`,
  `FavoritesList.visible`/`countLabel`) y capa Compose delgada `FavoritesScreen` (cabecera "Mis recetas
  guardadas" + conteo, buscador, chips de orden terracota, toggle lista/rejilla oliva, nota
  "Disponibles sin conexión", estado vacío con marcador y "Explorar recetas", `Saver` de
  `rememberSaveable`) y `FavoriteRecipeCard`/`FavoriteRecipeTile` (miniatura 96x96 degradada, meta en
  cifras tabulares y corazón; swipe "Eliminar" vía `SwipeToDismissBox` de Material 3). Integración:
  `MainActivity` compone `FavoritesDatabase`/repositorio/servicio, `SazonApp` las pasa a `MainShell`,
  éste observa los favoritos para la pestaña, alimenta `HomeRecipes.saved` (bloque "Guardadas") y
  cablea `isSaved`/`onToggleSave` del Detalle; `ui/SectionPlaceholder.kt` eliminado. Sin Resumen de
  actividad, sin "Abiertas hace poco", sin modo cocina, sin Búsqueda con IA ni proveedor real, sin
  dependencias nuevas.
- Verificación ejecutada: `& "C:\Program Files\Git\bin\bash.exe" init.sh` (exit 0, `./gradlew test` +
  `./gradlew lint`, sin procesos largos); se parsearon por separado los XML de resultados y el informe
  `lint-results-debug.xml`.
- Evidencia capturada: **46 suites, 301 tests, 0 fallos/errores/saltados**; 24 nuevos:
  `FavoriteServiceTest` 6, `FavoritesListTest` 9, `RoomFavoriteRepositoryTest` 9 (277 preexistentes en
  verde). Lint 0 errores y 13 warnings preexistentes (GradleDependency 7, NewerVersionAvailable 3,
  AndroidGradlePluginVersion 2, OldTargetApi 1), ninguno con localización bajo `app/src`. Inspección:
  `domain.favorites` y la lógica pura de `ui.favorites` sin imports `android.*`/`androidx.*`;
  `SectionPlaceholder` sin referencias en `app/src`; `git diff --stat` sin cambios en
  `app/build.gradle.kts`, `gradle/libs.versions.toml`, el dominio/Lote/Perfil, las bases ajenas, la
  Taxonomía ni `seed-recipes.json`.
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/domain/favorites/{Favorite,FavoriteRepository,FavoriteService}.kt`,
  `app/src/main/java/com/sazon/app/data/favorites/{FavoriteEntity,FavoriteDao,FavoritesDatabase,FavoritesMappers,RoomFavoriteRepository}.kt`,
  `app/src/main/java/com/sazon/app/ui/favorites/{FavoritesSort,FavoritesViewMode,FavoritesList,FavoritesScreen,FavoriteRecipeCard}.kt`,
  `app/src/test/java/com/sazon/app/domain/favorites/FavoriteServiceTest.kt`,
  `app/src/test/java/com/sazon/app/ui/favorites/FavoritesListTest.kt`,
  `app/src/test/java/com/sazon/app/data/favorites/RoomFavoriteRepositoryTest.kt`,
  `MainActivity.kt`, `ui/SazonApp.kt`, `ui/MainShell.kt` (modificados), `ui/SectionPlaceholder.kt`
  (eliminado), `docs/technical-discovery.md`, `DESIGN.md`, `feature_list.json` y este registro.
- Hallazgo de entorno: la primera ejecución del test JVM (R8/`testDebugUnitTest`) abortó con un
  `EXCEPTION_ACCESS_VIOLATION` en un hilo GC de Corretto 21 (`-Xmx512m`, `jvm.dll`), sin relación con el
  código nuevo (la compilación Kotlin de main y test terminó sin errores); la re-ejecución de `init.sh`
  fue en verde. Se registra como flake de entorno.
- Riesgo o incógnita abierta: ninguno nuevo. Quedan para revisión manual de la persona tras el merge
  las pruebas exigidas (guardar una receta generada por IA y verla tras reiniciar sin conexión; el
  estado vacío con "Explorar recetas" llevando a Inicio) y el acabado visual (lista/rejilla, chips,
  corazón, swipe "Eliminar", claro/oscuro) contra `docs/design/concepts/favorites/favoritos/`; un
  defecto visual se corrige con una feature correctiva.
- Siguiente paso: validación independiente de `favorites` con `feature-validator`.

### Sesión 053

- Fecha: 2026-10-10
- Objetivo: validar `favorites` de forma independiente y persistir la aceptación.
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la aceptación independiente. Commit local de la rama
  `feature/favorites` (lo hace el orquestador de `feature-flow`); la publicación queda para
  `github-flow`. Sin cambios de código tras la validación.
- Verificación ejecutada: el validador re-ejecutó `init.sh` con Git Bash (exit 0) y, al ver test/lint en
  `UP-TO-DATE`, forzó `./gradlew testDebugUnitTest --rerun-tasks` (32/32 tareas) y
  `./gradlew lint --rerun-tasks` (28/28 tareas); parseó por su cuenta los XML de resultados y el informe
  de lint.
- Evidencia capturada: 46 suites, 301 tests, 0 fallos/errores/saltados (`FavoriteServiceTest` 6,
  `FavoritesListTest` 9, `RoomFavoriteRepositoryTest` 9); lint 0 errores y 13 warnings preexistentes, 0
  con localización bajo `app/src`. Confirmó los escenarios 1-2 y 10 (copia con `savedAt=now`,
  `matchScore` normalizado a `null`, toggle idempotente, dos filas coexisten, `remove` selectivo,
  `clear`, `observe`, orden `savedAt` desc) y 5-6 (labels, tres órdenes, búsqueda por título insensible a
  mayúsculas/acentos, caso negativo por descripción, sin coincidencias vacío, `fromKey` con fallback);
  3/4/7/8/9 por inspección de la integración. Pureza de `domain.favorites` y de la lógica pura de
  `ui.favorites`, base Room propia `favorites.db` v1 con `@Query` parametrizado, disciplina de alcance
  (build/lockfile/dominio/Lote/Perfil/bases ajenas/Taxonomía/`seed-recipes.json` intactos, sin
  dependencias nuevas) y capturas obligatorias presentes
  (`docs/design/concepts/favorites/favoritos/{screen.png,code.html,DESIGN.md}` + `README.md`).
  Seguridad: sin secretos, sin red, sin construcción de SQL por concatenación.
- Ficheros actualizados: `feature_list.json` y este registro.
- Hallazgos del validador (3 Low, informativos, ninguno bloqueante): `README.md` de capturas con
  `design.md` en minúscula heredado de la plantilla; microcopy aditivo "No hay favoritas con esta
  búsqueda." fuera de la spec pero coherente; `Validator Checklist` de la spec sin marcar.
- Riesgo o incógnita abierta: ninguno nuevo. Quedan para revisión manual de la persona tras el merge las
  dos pruebas exigidas (guardar una receta generada por IA y verla tras reiniciar sin conexión; el estado
  vacío con "Explorar recetas" llevando a Inicio) y el acabado visual contra
  `docs/design/concepts/favorites/favoritos/`; un defecto visual se corrige con una feature correctiva.
- Siguiente paso: `activity-summary` (depende de `profile-screen` y `favorites`, ambas `accepted`),
  empezando por `planner`/`feature-spec`.

### Sesión 054

- Fecha: 2026-10-11
- Objetivo: analizar y documentar (sin implementar) la propuesta de backend post-MVP para cuenta,
  sincronización y proxy de IA.
- Completado: creado `docs/backend-proposal.md` con la opción B (**Kotlin + Ktor + PostgreSQL**),
  alternativas consideradas, alcance por servicios, superficie HTTP propuesta, modelo de datos,
  autenticación/seguridad, estrategia de sincronización offline-first (Room como verdad local,
  servidor como réplica, LWW + tombstones + WorkManager), módulo de dominio compartido (KMP) para
  reutilizar `RecipeValidator`/`PromptBuilder`/`PreferenceSignature`, despliegue y operaciones, plan por
  fases (backlog propuesto), impacto en el harness (ADR 0004 sugerido), riesgos, preguntas abiertas y un
  checklist de retomada.
- Verificación ejecutada: ninguna de código (cambio solo documental). No se ha tocado
  `feature_list.json`, `init.sh`, el build ni el código de la app; `docs/backend-proposal.md` no se cita
  como decisión aceptada.
- Evidencia capturada: documento versionado en `docs/backend-proposal.md`; referencia añadida en el
  estado de este registro; ninguna feature cambia de estado (WIP = 0).
- Ficheros actualizados: `docs/backend-proposal.md` (nuevo) y este registro.
- Hallazgo relevante: el backend **revierte** el no-objetivo permanente de `build-brief.md` y el
  `ADR 0002`; construirlo exige un ADR nuevo que ajuste el 0002 y actualizar discovery (recogido en
  §13 del documento). La propuesta explota que el dominio ya es puro y que la app define puertos
  (`ProfileRepository`, `FavoriteRepository`, `SuggestionBatchRepository`, `RecipeGenerator`), por lo
  que la integración es, sobre todo, adaptadores detrás de esos puertos más un motor de sync.
- Riesgo o incógnita abierta: sin resolver a propósito (el desarrollo no se inicia ahora). Preguntas
  abiertas en §15 del documento (identidad email/password vs social, qué se sincroniza, hosting).
- Siguiente paso: sin cambios en el plan del MVP; si se decide construir el backend, empezar por la
  fase 1 (`shared-domain-module`) y por el ADR 0004, según el checklist de §16 del documento.

### Sesión 055

- Fecha: 2026-10-11
- Objetivo: cambiar el paso de capturas de UI del flujo `github-flow` para que, cuando falten, se
  **provisione la feature dentro del worktree** en lugar de bloquear sin crear nada (que ensuciaba la
  base y obligaba a commitear/pushear `main`).
- Completado:
  - `.opencode/skills/github-flow/SKILL.md`: nueva Hard Rule de provisioning; preconditions #5 y párrafo
    de fallo ajustados; nuevo artefacto determinista (carpeta de petición **dentro del worktree**);
    `Mode: execute` reordenado (fija `base_sha` antes del gate) con paso explícito de *UI-captures
    provisioning* que crea worktree/rama + `README.md` + `stitch-prompt.md` y **no** lanza sesión; nueva
    sección `## UI Captures Provisioning`; `Blocked → Resume` actualizado (nada en la base, sin copias al
    reanudar); `Output Report` con estado `provisioned` y `session: none`.
  - `.opencode/skills/feature-flow/SKILL.md`: el UI gate reutiliza el material ya dejado en el worktree y
    genera también el `stitch-prompt.md` si falta.
  - `.opencode/skills/github-flow/references/evals.md`: nueva *Eval 6* (contrato del provisioning en
    modo `plan`, cero efectos).
  - `AGENTS.md`: política de capturas de UI actualizada (worktree igualmente creado, base sin ensuciar).
- Verificación ejecutada: ninguna de código (cambio solo documental del harness). No se tocó
  `feature_list.json`, `init.sh`, el build ni el código de la app; no se ejecutó `init.sh`/Gradle porque
  el cambio no altera el camino de arranque.
- Evidencia capturada: `git diff` de los cuatro ficheros; el flujo resultante documentado arriba en
  "Estado verificado actual". Ninguna feature cambia de estado (WIP = 0).
- Ficheros actualizados: `.opencode/skills/github-flow/SKILL.md`,
  `.opencode/skills/github-flow/references/evals.md`, `.opencode/skills/feature-flow/SKILL.md`,
  `AGENTS.md` y este registro.
- Riesgo o incógnita abierta: el `stitch-prompt.md` es una copia derivada de `docs/design/stitch/`
  (fuente de verdad); si las pantallas cambian, hay que regenerarlo. La generación del prompt depende del
  mapeo feature→pantalla, que se toma de la spec o de `feature_list.json`.
- Siguiente paso: sin cambios en el plan del MVP; `activity-summary` sigue siendo la siguiente candidata
  (`planner`/`feature-spec`) y, si le faltaran capturas, ahora se provisionará su worktree en vez de
  bloquear en la base.

### Sesión 056

- Fecha: 2026-10-11
- Objetivo: eliminar el cuelgue de las sesiones de agente al ejecutar Gradle (incidencia real: la
  sesión hija de `activity-summary` quedó congelada con su último comando de Gradle ya terminado).
- Diagnóstico: el subagente `implementer` lanzó `./gradlew testDebugUnitTest …` **a mano** (sin pasar
  por `init.sh`). El build imprimió `BUILD SUCCESSFUL` a las 19:45, pero el daemon de Kotlin (hijo del
  daemon de Gradle) heredó el stdout del comando y sobrevivió al build: el pipe nunca llegó a EOF, la
  herramienta `bash` de opencode quedó en estado `running` durante ~26 min y el proceso `opencode run`
  dejó de escribir log (19:44:29). Se desbloqueó matando el proceso hijo; el worktree conserva el WIP.
- Completado:
  - `scripts/gradle.sh` (nuevo, ejecutable): ejecuta `./gradlew "$@"` con la salida redirigida a un
    fichero y la vuelca al terminar, devolviendo el código de salida de Gradle. Los daemons heredan el
    fichero, no el pipe del llamante, así que no pueden colgar la shell. **No** llama a `--stop`.
  - `init.sh`: usa el wrapper para `test` y `lint` y elimina su `trap './gradlew --stop' EXIT`.
  - `AGENTS.md`: regla en "Notas de entorno" (nunca `./gradlew` directo; wrapper para comprobaciones
    puntuales; nunca `--stop`).
  - `.opencode/skills/feature-implementer/SKILL.md` (Self-Verify) y `.opencode/skills/feature-validator/SKILL.md`
    (Gradle daemon hygiene): usan el wrapper en vez de la receta `trap … --stop`.
- Hallazgo extra: `./gradlew --stop` es **global** y mata también los builds de otros worktrees. Se
  verificó en directo: el gate de esta chore falló con `Gradle build daemon has been stopped: stop
  command received` porque el `init.sh` (con el trap) de la otra feature corrió a la vez. Por eso se
  elimina el trap: el redirigido a fichero ya evita el cuelgue y los worktrees paralelos dejan de
  matarse entre sí.
- Verificación ejecutada:
  1. `bash init.sh` en el worktree de `chore/gradle-daemon-hang`: `./gradlew test` → `BUILD
     SUCCESSFUL`, `./gradlew lint` → `BUILD SUCCESSFUL`, exit 0, **con otro build corriendo en
     paralelo** (ya no se interrumpen).
  2. Prueba decisiva del wrapper sin redirección externa: `bash scripts/gradle.sh compileDebugKotlin
     --rerun-tasks` → exit 0 y la herramienta devolvió, con el daemon de Kotlin **vivo** (1 GB)
     sujetando solo el fichero de log. Es el caso que antes colgaba la sesión.
- Evidencia capturada: logs de las dos ejecuciones anteriores; `git diff` de los seis ficheros.
- Ficheros actualizados: `scripts/gradle.sh` (nuevo), `init.sh`, `AGENTS.md`,
  `.opencode/skills/feature-implementer/SKILL.md`, `.opencode/skills/feature-validator/SKILL.md` y este
  registro. No se toca `feature_list.json`, el código de la app ni el build (WIP = 0).
- Riesgo o incógnita abierta: los daemons de Gradle ya no se paran al terminar (se apagan solos por
  inactividad, ~3 h); si se acumulan, limpiarlos a mano con `./gradlew --stop` **sin builds en vuelo**.
  Las ramas de features abiertas conservan el `init.sh` con el trap hasta que se mergeen.
- Siguiente paso: sin cambios en el plan del MVP. `activity-summary` sigue en curso con la sesión
  relanzada; su WIP rompía 4 tests de `SuggestionBatchServiceTest` (`./gradlew test` fallaba) y esa
  sesión debe corregirlos antes de que el validador pueda aceptar.

### Sesión 057

- Fecha: 2026-10-11
- Objetivo: retomar y cerrar `activity-summary` (Resumen de actividad en el Perfil) sobre el WIP que
  dejó la sesión interrumpida por el cuelgue de Gradle (sesión 056), corregir los 4 tests rotos y
  dejar el repositorio listo para validación independiente.
- Completado:
  - **Corregido el WIP roto**: el orden de los parámetros de `SuggestionBatchService` dejaba
    `onRecipesGenerated` **después** del reloj, de modo que el trailing lambda `{ now }` de los tests
    existentes se ligaba al callback en vez de al reloj (4 fallos en `SuggestionBatchServiceTest`). Se
    movió el callback **antes** de `now: () -> Long` (el reloj sigue siendo el último parámetro), así la
    costura es de verdad aditiva y los tests existentes quedan intactos.
  - Reutilizado el resto del WIP en disco: `domain.activity` (puerto + `ActivitySummaryService`),
    `data.activity` (base propia `activity_summary.db` v1, una fila, `increment` atómico), el callback
    `onRecipesGenerated` en `SuggestionBatchService`/`GenerationService` (default no-op; la rama
    `UseCached` no lo invoca), `ActivitySummarySection`, y el cableado `ProfileScreen`/`MainShell`/
    `SazonApp`/`MainActivity` (`favoritesCount = favorites.size`).
  - Documentación durable: registrado `domain.activity`/`data.activity`, el callback y la sección
    "Resumen" en `docs/technical-discovery.md` (y corregida la fila de la tabla: las favoritas se
    derivan de la lista observada, no de un `COUNT`).
  - Hallazgo de documentación: `DESIGN.md` no tenía registrada la captura `activity-summary:
    perfil-resumen`; se añadió la línea a "Capturas aceptadas" (el resto de `DESIGN.md` no cambia) y se
    anotó el hallazgo en `docs/specs/activity-summary.md`.
- Verificación ejecutada:
  1. `bash scripts/gradle.sh testDebugUnitTest --tests "…SuggestionBatchServiceTest"` → `BUILD
     SUCCESSFUL` (reprodujo y confirmó el arreglo del WIP roto).
  2. `bash init.sh` con Git Bash → `./gradlew test` y `./gradlew lint`, ambos `BUILD SUCCESSFUL`,
     exit 0, sin procesos largos. Parseo propio de los XML de resultados y del informe de lint.
- Evidencia capturada: 49 suites, **316 tests/0 fallos/errores/saltados** (301 preexistentes + 15
  nuevos: `ActivitySummaryServiceTest` 6, `RoomActivitySummaryRepositoryTest` 5,
  `SuggestionBatchActivityRecordingTest` 4); lint 0 errores y 13 warnings preexistentes
  (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), 0 en
  ficheros de la feature; `domain.activity` sin imports `android.*`/`androidx.*`;
  `app/build.gradle.kts`, `gradle/libs.versions.toml`, `data.favorites/**`, `data.profile/**` y
  `data.suggestions/**` sin cambios; sin dependencias nuevas.
- Ficheros creados (WIP reutilizado): los siete de `domain.activity`/`data.activity`/
  `ActivitySummarySection` y los tres tests nuevos (`ActivitySummaryServiceTest`,
  `RoomActivitySummaryRepositoryTest`, `SuggestionBatchActivityRecordingTest`), más el corte de diseño
  `docs/design/stitch/21-perfil-resumen.md` y la captura
  `docs/design/concepts/activity-summary/perfil-resumen/`. Modificados: `SuggestionBatchService.kt`,
  `GenerationService.kt`, `ProfileScreen.kt`, `MainShell.kt`, `SazonApp.kt`, `MainActivity.kt`,
  `docs/technical-discovery.md`, `DESIGN.md`, `docs/design/stitch/README.md`,
  `docs/specs/activity-summary.md`, `feature_list.json` y este registro.
- Riesgo o incógnita abierta: ninguno nuevo. Queda para la persona la verificación manual de emulador
  (favoritas = recetas guardadas; el contador sube al generar y no baja al refrescar ni al reabrir) y el
  acabado visual contra la captura, conforme a AGENTS.md (no se automatiza UI de emulador). La costura
  documentada: cuando se implemente `ai-search`, deberá reportar sus recetas `ai` aceptadas por el mismo
  `onRecipesGenerated`/`ActivitySummaryService.recordAcceptedRecipes` (follow-up, no en esta feature).
- Siguiente paso: validación independiente de `activity-summary` con `feature-validator`.

### Sesión 058

- Fecha: 2026-10-11
- Objetivo: validar `activity-summary` de forma independiente y persistir la aceptación.
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la aceptación independiente. Sin cambios de código tras la
  validación (solo estado/evidencia y este registro). Commit local en la rama `feature/activity-summary`
  (la publicación push + PR la hace `github-flow`, no esta sesión).
- Verificación ejecutada: el validador re-ejecutó `bash init.sh` con Git Bash (exit 0, `./gradlew test` +
  `./gradlew lint` vía `scripts/gradle.sh`, sin procesos largos), `./gradlew testDebugUnitTest
  --rerun-tasks` (32/32 tareas) y `./gradlew lint --rerun-tasks`, y parseó por su cuenta los XML de
  resultados y el informe de lint; inspeccionó pureza de dominio, tokens/UI, alcance/build y capturas.
- Evidencia capturada: **49 suites, 316 tests, 0 fallos/errores/saltados** (`ActivitySummaryServiceTest`
  6, `RoomActivitySummaryRepositoryTest` 5, `SuggestionBatchActivityRecordingTest` 4; regresión
  `SuggestionBatchServiceTest` 9/9 y `GenerationServiceTest` 6/6); lint 0 errores y 13 warnings
  preexistentes (0 en ficheros de la feature); `domain.activity` sin imports `android.*`/`androidx.*`;
  `app/build.gradle.kts`, `gradle/libs.versions.toml`, `data.favorites/**`, `data.profile/**` y
  `data.suggestions/**` sin cambios; capturas `docs/design/concepts/activity-summary/perfil-resumen/`
  presentes (`screen.png`, `code.html`, `DESIGN.md`) y registradas en `DESIGN.md`; escenarios 3, 4, 6 y 7
  cubiertos por tests JVM y la regla "solo `source == ai`, solo suma" confirmada en
  `ActivitySummaryService.recordAcceptedRecipes` (reforzada en `RoomActivitySummaryRepository`).
- Hallazgos del validador (3 Low/informativos, ninguno bloqueante): `onRecipesGenerated` se invoca tras
  `repository.save(batch)` en `SuggestionBatchService` (si la escritura del contador lanzara,
  `ensureBatch` propagaría el error con el Lote ya guardado; robustez menor); checklists de la spec sin
  marcar (patrón heredado del repo); nota de harness del worktree (`.git` referencia ruta WSL y `git`
  desnudo falló de forma intermitente desde Git Bash, resuelto con `GIT_DIR`/`GIT_WORK_TREE`).
- Riesgo o incógnita abierta: ninguno nuevo. Queda para la persona la verificación manual de emulador
  (favoritas = recetas guardadas; el contador sube al generar y no baja al refrescar ni al reabrir) y el
  acabado visual contra la captura, conforme a AGENTS.md (no se automatiza UI de emulador). La costura
  documentada: cuando se implemente `ai-search`, deberá reportar sus recetas `ai` aceptadas por el mismo
  `onRecipesGenerated`/`ActivitySummaryService.recordAcceptedRecipes` (follow-up).
- Siguiente paso: commit local de la rama `feature/activity-summary` (lo hace el orquestador de
  `feature-flow`); la publicación queda para `github-flow`. Después, especificar `ai-search` con
  `feature-spec`.
### Sesión 059

- Fecha: 2026-10-11
- Objetivo: dejar **todo el flujo corriendo nativo en WSL**. El repositorio vivía en `/mnt/c` pero se
  trabajaba con Git Bash y el `java` de Windows: `bash init.sh` en WSL fallaba con
  `ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH`.
- Completado:
  - `scripts/wsl-env.sh` (nuevo, se *sourcea*): publica `JAVA_HOME` (JDK 21 de Linux),
    `ANDROID_HOME`/`ANDROID_SDK_ROOT` (SDK de Linux), `GRADLE_USER_HOME` en ext4 y el `PATH` de `gh`;
    desactiva el file-watching de Gradle (`-Dorg.gradle.vfs.watch=false`, no fiable en DrvFs) y
    normaliza `local.properties` cuando trae una ruta del SDK de Windows. Fuera de WSL no hace nada.
  - `scripts/wsl-bootstrap.sh` (nuevo, idempotente, **sin sudo**): instala JDK 21 (Temurin,
    `~/.local/opt/jdk-21`), el SDK de Android de Linux (`~/Android/Sdk`: cmdline-tools 19.0,
    `platforms;android-36.1`, `build-tools;36.0.0`, `platform-tools`), `gh` nativo y reutiliza la sesión
    de `gh` de Windows (su token está en el keyring, no en `hosts.yml`: `gh.exe auth token` →
    `gh auth login --with-token`) y la distribución de Gradle ya descargada en Windows.
  - `init.sh`, `scripts/gradle.sh` y `scripts/new-worktree.sh` sourcean `wsl-env.sh`.
  - Worktrees: reparada la metadata de `activity-summary` y `chore-gradle-daemon-hang`, que guardaba
    rutas `C:/...` creadas por Git Bash (en WSL salían `prunable` y `git status` dentro fallaba con
    `not a git repository`). Ciclo completo probado desde WSL con un id desechable:
    `scripts/new-worktree.sh` (crea, ramifica y siembra `local.properties` con la ruta Linux) y
    `scripts/cleanup-worktree.sh` (elimina worktree y rama).
  - Causa confirmada por evidencia ajena: la Sesión 058 ya anotó como incidencia de harness que el
    worktree fallase **desde el otro sistema** (resuelto a mano con `GIT_DIR`/`GIT_WORK_TREE`); esta
    sesión corrige la raíz (la metadata del worktree guarda rutas absolutas en el formato del sistema
    que lo creó) y deja la regla escrita en `AGENTS.md`.
  - Harness nativo: `gh` en `~/.local/bin/gh`, helper de credenciales de git apuntando al `gh` nativo
    (antes invocaba al `gh.exe` de Windows por interop) y bloque de entorno en `~/.profile` para las
    shells interactivas. `opencode` ya era el binario Linux (`~/.opencode/bin/opencode`, v2.0.26), pero
    en shells **no** interactivas el PATH resolvía al shim npm de Windows (el alias de `~/.bashrc` solo
    aplica a las interactivas, y esa build de Bun es la que se colgaba al cerrar): `wsl-bootstrap.sh`
    deja un enlace en `~/.local/bin/opencode`, que va primero en el PATH, así que resuelve nativo
    siempre.
  - Documentación: `AGENTS.md` ("Notas de entorno" reescritas para WSL: plataforma, JDK/SDK, caché de
    Gradle, regla de worktrees desde WSL), `docs/technical-discovery.md` (`Testing and Verification` y
    `Deployment and Operations`) y `.opencode/skills/github-flow/SKILL.md` (la sesión se lanza en WSL).
- Verificación ejecutada:
  1. `bash scripts/wsl-bootstrap.sh` dos veces (instalación y re-ejecución idempotente, sin descargas
     nuevas).
  2. Sonda de SDK: con `sdk.dir` = ruta de Windows (inexistente en WSL) y `ANDROID_HOME` al SDK de
     Linux, `:app:processDebugResources` → `BUILD SUCCESSFUL` (AGP avisa de `sdk.dir` no válido y cae a
     `ANDROID_HOME`); con `wsl-env.sh` cargado, `local.properties` queda en la ruta Linux y el aviso
     desaparece.
  3. `bash init.sh` en WSL (camino estándar, con `local.properties` apuntando todavía al SDK de
     Windows): `./gradlew test` → `BUILD SUCCESSFUL` en 11m31s (**301 tests, 0 fallos, 0 errores, 0
     omitidos**) y `./gradlew lint` → `BUILD SUCCESSFUL` en 2m20s; exit 0. Primera pasada, con la
     descarga inicial de dependencias de Maven: **13m52s**. `local.properties` quedó apuntado a
     `/home/fernandogaliay/Android/Sdk` durante la propia ejecución. Segunda pasada en caliente:
     **8,7 s** en total (test y lint `UP-TO-DATE`, caché de configuración reutilizada).
  4. Reescritura de `local.properties`: probada con la forma escapada de Android Studio (`sdk.dir=C\:\\...`)
     y sin escapar; conserva otras claves (`ia.apiKey`), es idempotente con una ruta Linux ya correcta y
     no crea el fichero si no existe.
  5. `gh auth status`, `gh repo view` y `gh issue list` nativos en WSL; `git ls-remote origin` autentica
     con el helper nativo (repo privado).
  6. Ciclo de worktree de punta a punta (crear, abrir y eliminar) y `git worktree list` con los tres
     worktrees operativos desde WSL.
  7. Post-merge (tras integrar `origin/main` `0de0719`, con el código de `activity-summary`): `bash
     init.sh` → exit 0 en 2m29s (`test` `BUILD SUCCESSFUL` en 1m29s y `lint` en 59s), con **49 suites /
     316 tests / 0 fallos / 0 errores / 0 omitidos** y 0 errores de lint: las mismas 316 que registraron
     las Sesiones 057 y 058 en Windows, lo que confirma que WSL reproduce la verificación.
  8. Resolución de herramientas: `command -v java/gh/adb/opencode` → binarios nativos en shells de login
     y no interactivas (`opencode v2.0.26`, no el shim de Windows `1.18.35`).
- Evidencia capturada: salida de todos los comandos anteriores y `git worktree list` reconstruido.
- Ficheros actualizados: `scripts/wsl-env.sh` y `scripts/wsl-bootstrap.sh` (nuevos), `init.sh`,
  `scripts/gradle.sh`, `scripts/new-worktree.sh`, `AGENTS.md`, `docs/technical-discovery.md`,
  `.opencode/skills/github-flow/SKILL.md` y este registro. No se toca `feature_list.json` ni el código
  de la app (WIP = 0 en esta rama).
- Riesgo o incógnita abierta: (a) la metadata de un worktree guarda rutas absolutas en el formato del
  sistema que lo creó, así que **se crean desde WSL** (ya documentado; el que se cree desde Git Bash
  aparecerá `prunable` en WSL); (b) si Android Studio en Windows resincroniza el proyecto, reescribe
  `local.properties` con la ruta de Windows y WSL la corrige en el siguiente build; (c) quedan restos de
  la etapa Windows: el directorio vacío `Documents\Default Project-worktrees` y el worktree
  `chore-gradle-daemon-hang` (PR #53 ya mergeado: candidato a `github-flow prune`, que es idempotente);
  (d) el `adb` de WSL no ve el emulador (ni un móvil por USB) que corre en Windows, así que
  `installDebug` desde WSL no encuentra dispositivo: la comprobación visual/manual sigue en Android
  Studio (documentado en `AGENTS.md`).
- Siguiente paso: sin cambios en el plan del MVP. `activity-summary` quedó **aceptada** (Sesión 058) y
  **mergeada** (PR #56) mientras corría esta sesión, con su worktree ya limpiado; la siguiente
  candidata es `ai-search` (`feature-spec`). Esta rama integra `origin/main` (`0de0719`) conservando
  ambas aportaciones en `PROGRESS.md`.

### Sesión 060

- Fecha: 2026-10-12
- Objetivo: implementar `ai-search` (bloque 2 de la Home: Búsqueda con IA y sus estados) según
  `docs/specs/ai-search.md`, en el worktree de la rama `feature/ai-search`.
- Completado:
  - **Dominio puro** en `com.sazon.app.domain.search`: `AiSearchService.search(query, profile)` (normaliza
    con `SearchMemo.key`, sirve el memo, llama a `RecipeGenerator.searchByText`, valida **receta a receta**
    con `RecipeValidator.validateRecipe`, re-lanza `CancellationException`, degrada a `aiUnavailable` y
    **no** memoiza el fallo, reporta aceptadas por `onRecipesGenerated`) y `SearchMemo` (memo de sesión en
    memoria, clave `trim().lowercase()`).
  - **Lógica/estado puro** en `com.sazon.app.ui.search`: `AiSearchQuery` (normalize/canSubmit),
    `AiSearchCopy` (microcopy único), `AiSearchProgress` (cabecera + tres sublíneas + línea de prompt) y
    `AiSearchOutcome.stageOf` (Results/Blocked/NoResults/Failure; el bloqueo por Perfil lo decide el
    **motivo del descarte** `DIET_PATTERN`/`HARD_RESTRICTION`, no la consulta).
  - **Costura de conectividad**: `com.sazon.app.data.NetworkStatusSource` (sobre `ConnectivityManager`,
    `NET_CAPABILITY_INTERNET` + `NET_CAPABILITY_VALIDATED`) y permiso `ACCESS_NETWORK_STATE` en el
    manifiesto. Sin dependencias nuevas.
  - **Capa Compose delgada**: `AiSearchBar` (bloque 2 de la Home, píldora + destello + envío; deshabilitada
    y con `OFFLINE_NOTE` sin conexión), `AiSearchFlow` (host de `PushedScreen.SEARCH`: carga reutilizando
    `GeneratorGeneratingScreen` con `cancelLabel` aditivo y `ResultsScreen` con `ResultsOrigin.Search`, y
    los estados de `AiSearchStates`) y `AiSearchStates` (bloqueado en ámbar `tertiaryContainer`, sin
    resultados y fallo, cada uno con "Atrás" y su acción).
  - **Integración sin DI**: `PushedScreen.SEARCH`; `HomeScreen` monta `AiSearchBar` con la conectividad y
    la consulta; `MainShell` guarda la consulta en `rememberSaveable`, observa la conectividad y enruta
    `SEARCH` a `AiSearchFlow` con los callbacks; `SazonApp`/`MainActivity` componen `AiSearchService` y
    `NetworkStatusSource`.
  - **Tests JVM nuevos** (27): `AiSearchServiceTest` 9, `SearchMemoTest` 5, `AiSearchOutcomeTest` 6,
    `AiSearchQueryTest` 4 y `AiSearchProgressTest` 3.
- Verificación ejecutada: `bash init.sh` (exit 0, `./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`, sin procesos largos); `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks`
  (32/32 tareas) y `bash scripts/gradle.sh lint`, con parseo propio de los XML de resultados y del
  informe de lint.
- Evidencia capturada: **54 suites, 343 tests, 0 fallos/errores/saltados** (316 preexistentes en verde);
  `AiSearchServiceTest` 9 (`accepted` vacío con dieta contraria + `DIET_PATTERN`; el falso real descarta
  el pollo por dieta; gluten → `HARD_RESTRICTION`; fallo → `aiUnavailable` sin propagar; cancelación
  re-lanzada; consulta en blanco no llama; memo no repite y el fallo no se memoiza; `onRecipesGenerated`
  solo con aceptadas); lint **0 errores, 13 warnings** preexistentes y **0 en ficheros de la feature**
  (se corrigió 1 Hint `AutoboxingStateCreation` en `AiSearchFlow.kt` con `mutableIntStateOf`).
  Inspección: `domain.search` y la lógica pura de `ui.search` sin imports `android.*`/`androidx.*`;
  `ui.results` intacto; sin cambios en `app/build.gradle.kts`, `gradle/libs.versions.toml`, dominio
  aceptado, `data/**` (salvo `NetworkStatusSource`), la Taxonomía, `seed-recipes.json` ni `init.sh`; sin
  hex sueltos ni iconos.
- Ficheros creados: `app/src/main/java/com/sazon/app/domain/search/{AiSearchService,SearchMemo}.kt`,
  `app/src/main/java/com/sazon/app/ui/search/{AiSearchQuery,AiSearchCopy,AiSearchProgress,AiSearchOutcome,AiSearchBar,AiSearchFlow,AiSearchStates}.kt`,
  `app/src/main/java/com/sazon/app/data/NetworkStatusSource.kt` y los cinco tests en
  `app/src/test/java/com/sazon/app/{domain,ui}/search/`.
- Ficheros modificados: `AndroidManifest.xml` (permiso `ACCESS_NETWORK_STATE`), `MainActivity.kt`,
  `ui/SazonApp.kt`, `ui/MainShell.kt`, `ui/home/HomeScreen.kt`, `ui/navigation/MainNavigationState.kt`
  (`PushedScreen.SEARCH`), `ui/generator/GeneratorGeneratingScreen.kt` (`cancelLabel` aditivo con default
  `"Cancelar"`), `feature_list.json` y este registro.
- Docs actualizados: `docs/technical-discovery.md` (ubicación de `domain.search`/`ui.search`, estados y
  costura `NetworkStatusSource` + permiso), `docs/domain-model.md` (dónde vive la orquestación de la
  Búsqueda con IA y que el memo es de sesión) y `DESIGN.md` (captura aceptada `ai-search: busqueda-ia`).
- Hallazgo (spec actualizada): el **Scenario 2** es inconsistente con el `FakeRecipeGenerator` real — un
  Perfil vegano solo descarta "Pollo al ajillo" (las otras tres fixtures son vegetales), así que
  `accepted` no queda vacío y el estado sería Resultados, no Bloqueado. La verificación exigida ("una
  consulta que contradice el patrón de dieta devuelve cero recetas") se cubre con un generador cuya salida
  es **toda** de dieta contraria; además se comprueba con el falso real que el pollo cae por
  `DIET_PATTERN`. Anotado en `docs/specs/ai-search.md` (Unknowns/Findings).
- Riesgo o incógnita abierta: el acabado visual (píldora + destello, ámbar del bloqueo, claro/oscuro) y
  las pruebas manuales de emulador (barra sin conexión; carga cancelable; resultados con subtítulo;
  bloqueo → Perfil) quedan para revisión de la persona tras el merge (AGENTS.md no automatiza UI de
  emulador). Se mantiene la pregunta 1 abierta (errores/reintentos de la IA) hasta `real-ai-provider`.
- Siguiente paso: validación independiente de `ai-search` con `feature-validator`; el commit local de la
  rama lo hace el orquestador de `feature-flow` y la publicación queda para `github-flow`.

### Sesión 061

- Fecha: 2026-10-12
- Objetivo: validar `ai-search` de forma independiente y persistir la aceptación, en el worktree de la
  rama `feature/ai-search`.
- Completado: veredicto **`accept`** de un agente con contexto limpio (rol `validator`,
  `feature-validator`); `feature_list.json` pasa a `accepted` con evidencia que nombra la validación
  independiente; registrado el detalle en `docs/validations/ai-search.md`. Sin cambios de código tras
  la validación.
- Verificación ejecutada: el validador re-ejecutó `bash init.sh` (exit 0, test + lint vía
  `scripts/gradle.sh`, sin procesos largos), `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks`
  (32/32 tareas) y parseó por su cuenta los XML de resultados y el informe de lint; inspeccionó pureza
  de `domain.search`/`ui.search`, el alcance del diff y `ui.results`.
- Evidencia capturada: **54 suites, 343 tests, 0 fallos/errores/saltados** (27 nuevos de `ai-search`,
  316 preexistentes en verde), reproducidos exactamente respecto al implementer; lint **0 errores, 13
  warnings** preexistentes y **0 en ficheros de la feature**; `ui.results` intacto; `data/**` solo el
  nuevo `NetworkStatusSource`; `app/build.gradle.kts`, `gradle/libs.versions.toml`, dominio aceptado,
  la Taxonomía, `seed-recipes.json` e `init.sh` sin cambios.
- Hallazgos del validador (4 Low/informativos, ninguno bloqueante): (1) el **Scenario 2** es imposible
  con el `FakeRecipeGenerator` real (un Perfil vegano solo descarta "Pollo al ajillo"; `accepted` no
  queda vacío y el estado real sería Resultados, no Bloqueado); lo acepta como limitación de la fixture
  de desarrollo, no de la lógica, porque la verificación exigida ("cero recetas" con dieta contraria)
  se cubre con un test negativo genuino, el bloqueo por `DIET_PATTERN`/`HARD_RESTRICTION` se cubre en
  `AiSearchOutcomeTest` y la inconsistencia está trazada en la spec/PROGRESS/evidencia; (2)
  `AiSearchProgress.subLines(query)` no usa el parámetro (microcopy estable por diseño); (3) el memo se
  indexa solo por consulta normalizada, como fija la spec; (4) rotar la página vuelve a carga, mismo
  criterio que el Generador y declarado como *Unknown*. Sin hallazgos Critical/High/Medium; sin
  acciones de seguridad requeridas.
- Riesgo o incógnita abierta: el estado Bloqueado no es demostrable en smoke manual con el fake actual
  (un camino manual demostrable exigiría una feature correctiva de fixture, fuera de alcance). Se
  mantiene la revisión manual de la persona (barra sin conexión, carga cancelable, subtítulo de
  resultados, acabado frente a las capturas) y la pregunta 1 abierta hasta `real-ai-provider`.
- Siguiente paso: `feature_list.json` queda en `accepted` y el orquestador de `feature-flow` crea el
  **commit local** de la rama `feature/ai-search` (nunca push ni PR: la publicación es del orquestador
  de `github-flow`). Después, la siguiente candidata es `real-ai-provider`.

### Sesión 062

- Fecha: 2026-10-12
- Objetivo: chore de mantenimiento del harness y de tests, tras analizar por qué `ai-search` tardó más de
  lo esperado. Dos frentes: (a) el CLI `opencode` v2 ya no soporta `--dir`; (b) los tests de `observe()`
  de los repositorios Room usaban un margen de 5 s que a veces se agota con builds de Gradle en paralelo.
- Completado:
  - `.opencode/skills/github-flow/SKILL.md`, `.opencode/skills/github-flow/references/evals.md` y
    `AGENTS.md`: el arranque de la sesión de una feature pasa a ser `opencode run` **con el worktree como
    directorio de trabajo** (`cd`/cwd), porque `opencode` v2 no tiene `--dir`; nota de
    `NODE_OPTIONS=--dns-result-order=ipv4first` ante DNS IPv6 y de `-m <provider/model>` solo si hace
    falta override.
  - `.opencode/skills/feature-implementer/SKILL.md` (Self-Verify): preferir tiradas enfocadas y un único
    `bash init.sh` final (cada gate completo cuesta ~1.5–2.5 min).
  - Estabilizados los cuatro tests Room de `observe()` (`RoomActivitySummaryRepositoryTest`,
    `RoomFavoriteRepositoryTest`, `RoomProfileRepositoryTest`, `RoomSuggestionBatchRepositoryTest`): el
    margen de espera de las emisiones del Flow de Room pasa de 5 s a 30 s (con comentario), sin cambiar el
    comportamiento verificado.
- Verificación ejecutada: `bash init.sh` en el worktree → exit 0 (`./gradlew test` BUILD SUCCESSFUL en
  1m52s; `./gradlew lint` BUILD SUCCESSFUL en 1m41s, 0 errores), y una tirada enfocada
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` de las cuatro clases (28 tests, 0 fallos).
- Evidencia capturada: salida de los comandos anteriores; `git diff --stat` (4 ficheros de test, +16/-8;
  4 ficheros de docs/skills).
- Ficheros actualizados: `.opencode/skills/github-flow/SKILL.md`,
  `.opencode/skills/github-flow/references/evals.md`, `.opencode/skills/feature-implementer/SKILL.md`,
  `AGENTS.md`, los cuatro `Room*RepositoryTest.kt` y este registro. No se toca `feature_list.json` ni el
  código de la app (WIP = 0 en esta rama).
- Riesgo o incógnita abierta: el arreglo del flake es un margen mayor, no una eliminación determinista de
  la carrera (sería inyectar un executor de test en Room y usar reloj virtual); si reapareciera, se hace
  una correctiva. El PR #60 (`ai-search`) se mergeó mientras corría esta sesión, así que esta entrada se
  renumeró a 062 al integrar `origin/main`.
- Siguiente paso: sin cambios en el plan del MVP. Siguiente candidata: `real-ai-provider`.

### Sesión 063

- Fecha: 2026-10-12
- Objetivo: saneado del harness tras la revisión de coherencia (deriva de plantilla, referencias
  obsoletas y huecos de tooling).
- Completado:
  - A2: sustituidas en las skills las referencias a ficheros inexistentes de stack ajeno
    (`docs/product-brief.md`, `docs/user-and-access-model.md`, `docs/mvp-scope.md`, `ARCHITECTURE.md`,
    `CONSTRAINTS.md`, `pnpm`, `bootstrap-nextjs-shell`) por los reales de este repo.
  - A3: comando del gate unificado a `bash init.sh` (feature-flow ya no dice `CI=true ./init.sh`).
  - A4: corregido el ejemplo de capturas de `AGENTS.md` y el id `perfil-screen` → `profile-screen` en
    `evals`.
  - A6: `docs/design/concepts/README.md` documenta el provisioning en el worktree; el mapa de Stitch
    deja de marcar `ai-search/` como "futuro".
  - D3: nuevo `scripts/launch-feature-session.sh` (centraliza cwd del worktree, modelo,
    `NODE_OPTIONS=--dns-result-order=ipv4first` y título); `github-flow` y `AGENTS.md` lo referencian.
  - D4: `new-worktree.sh`/`sync-worktree.sh`/`cleanup-worktree.sh` aceptan `chore/<id>`; y los cuatro
    scripts resuelven la raíz por el git common dir (bug: ejecutados desde un worktree apuntaban al
    worktree en vez de al checkout base).
  - D2 (descartado): unir `test lint` en una sola invocación de Gradle **falla** por validación de
    dependencia implícita (`lint` lee `app/src/main/assets`, salida de `syncTaxonomy`). Se mantiene el
    `init.sh` con dos invocaciones; arreglarlo exige tocar el build (fuera de este chore).
- Verificación ejecutada: `bash init.sh` exit 0 (`./gradlew test` 1m42s + `./gradlew lint`); `bash -n`
  de todos los scripts; dry-runs de `cleanup-worktree.sh` con `chore/gradle-daemon-hang`, `ai-search` y
  `real-ai-provider` (resuelven dir/rama correctos).
- Evidencia capturada: salida de los comandos anteriores; `git diff --stat`.
- Ficheros actualizados: skills (`feature-flow`, `feature-spec`, `feature-implementer`,
  `feature-validator` + `references/`, `github-flow`), `AGENTS.md`, `docs/design/concepts/README.md`,
  `docs/design/stitch/README.md`,
  `scripts/{new-worktree,sync-worktree,cleanup-worktree,launch-feature-session}.sh` y este registro. No
  se toca `init.sh` (D2 revertido), `feature_list.json` ni el código de la app.
- Riesgo o incógnita abierta: el worktree huérfano `chore-gradle-daemon-hang` (PR #53 mergeado) ya es
  limpiable con el script mejorado, pero no se ha borrado en esta sesión. D1 (tamaño de contexto de
  `PROGRESS.md`/`feature_list.json`) queda para un chore aparte.
- Siguiente paso: merge del PR #61 y de este chore; después, D1 (aligerar contexto) y `real-ai-provider`.

### Sesión 064

- Fecha: 2026-10-12
- Objetivo: aligerar el contexto de arranque (D1, punto 1): archivar las sesiones antiguas de
  `PROGRESS.md`.
- Completado: movidas las sesiones **001–045** a `docs/progress/sessions-001-045.md`; `PROGRESS.md`
  conserva el bloque "Estado verificado actual" y las sesiones **046–063** (todas las referenciadas por
  el estado actual) más un puntero al archivo. `PROGRESS.md` pasa de **206 KB a ~70 KB** (−66 %).
- Verificación ejecutada: ninguna de código (cambio solo documental). Comprobado por script: el archivo
  conserva íntegras las sesiones 001–045, `PROGRESS.md` conserva 046–063 y el estado actual, y no hay
  referencias a sesiones ≤045 dentro de `PROGRESS.md`.
- Ficheros actualizados: `PROGRESS.md` y `docs/progress/sessions-001-045.md` (nuevo). No se toca
  `feature_list.json` ni el código de la app.
- Riesgo o incógnita abierta: algunas specs (`docs/specs/*.md`) citan sesiones archivadas (001, 006,
  010, 013, 014, 024, 033); la referencia sigue siendo localizable por el puntero de `PROGRESS.md` y la
  cabecera del archivo. El punto 2 de D1 (evidencia de `feature_list.json`) queda pendiente.
- Siguiente paso: merge de #61/#62/#63; después, D1 punto 2 (si se decide) y `real-ai-provider`.

### Sesión 065

- Fecha: 2026-10-12
- Objetivo: que `github-flow` acepte una ruta externa con los exports de Stitch y los importe al
  worktree, sin copiarlos a mano a `docs/design/concepts/`.
- Completado:
  - Nuevo `scripts/import-stitch-captures.sh <feature-id> <ruta-origen> [--screen <pantalla>]`: importa
    exports (subcarpetas por pantalla, o ficheros sueltos de una pantalla), acepta rutas WSL y Windows,
    valida que cada pantalla esperada tenga png + html + md y devuelve 0 (ok) / 1 (faltan ficheros) /
    2 (uso, origen o worktree).
  - `github-flow` (skill + evals): input opcional `captures_path` para `execute`; en el provisioning, si
    el import valida, resuelve el bloqueo y **lanza la sesión en la misma ejecución**; nueva Eval 7.
  - `AGENTS.md` y `docs/design/concepts/README.md` documentan
    `github-flow execute <feature-id> --captures <ruta>`.
- Verificación ejecutada: `bash -n` de todos los scripts; pruebas del importador (subcarpetas; ficheros
  sueltos con `--screen` y inferidos del README; fichero faltante -> rc 1; ruta Windows inexistente ->
  rc 2).
- Ficheros actualizados: `scripts/import-stitch-captures.sh` (nuevo),
  `.opencode/skills/github-flow/SKILL.md`, `.opencode/skills/github-flow/references/evals.md`,
  `AGENTS.md`, `docs/design/concepts/README.md` y este registro. No se toca `feature_list.json` ni el
  código de la app.
- Riesgo o incógnita abierta: el importador no borra ficheros del destino que no estén en el origen
  (solo copia); si una pantalla cambia de nombre, conviene limpiar la carpeta destino.
- Siguiente paso: `real-ai-provider`.

### Sesión 066

- Fecha: 2026-10-12
- Objetivo: implementar **`real-ai-provider`** (worktree
  `/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/real-ai-provider`, rama
  `feature/real-ai-provider`) según `docs/specs/real-ai-provider.md`: conectar el proveedor real
  (DevExpert Inference, API compatible con OpenAI) **detrás** del puerto `RecipeGenerator`, con Ktor
  Client (engine OkHttp) + `kotlinx.serialization`, política de timeouts/reintentos/degradación en un
  único sitio y verificación JVM con `MockEngine`. Sin commit ni push (los hace el orquestador al
  `accept`).
- Completado:
  - Build: Ktor 3.6.0 en el version catalog (5 librerías), dependencias en `app/build.gradle.kts`
    (`implementation` ×4 + `ktor-client-mock` de test), `buildConfig = true` y
    `buildConfigField("String", "DEVEXPERT_API_KEY", …)` leyendo `local.properties` en tiempo de
    configuración (valor `""` si falta; nunca se imprime), y permiso `INTERNET` en el manifiesto.
  - `com.sazon.app.data.ai`: `AiProviderPolicy` (constantes y decisiones puras), `AiDiagnostics`
    (`AiUnavailableReason`, `AiUnavailableException`, `AiCallOutcome`, `AiOperation`, `AiCallEvent`),
    `OpenAiChat` (DTOs del envelope), `AiRecipePayload` (decode tolerante, valla markdown, parseo
    elemento a elemento con restricciones **neutras**), `AiHttpClientFactory` (OkHttp/Mock,
    `ContentNegotiation` + `HttpTimeout`, `expectSuccess = false`) y `DevExpertRecipeGenerator`
    (3 operaciones, prompt con `PromptBuilder`, deadline total, 1 reintento con backoff/`Retry-After`,
    `CancellationException` siempre relanzada, clave ausente → `MISSING_KEY` sin petición, 200 ilegible
    → lista vacía).
  - `MainActivity`: se sustituye `FakeRecipeGenerator()` por `AiHttpClientFactory.create()` +
    `DevExpertRecipeGenerator(BuildConfig.DEVEXPERT_API_KEY, taxonomy, client, onEvent = Log)`.
  - Tests nuevos en JVM (`MockEngine`, reloj inyectado, clave ficticia `test-key`):
    `AiProviderPolicyTest` (8), `AiRecipePayloadTest` (5) y `DevExpertRecipeGeneratorTest` (15,
    escenarios 1–10 de la spec, incluidas la degradación con `GenerationService` y `AiSearchService`
    reales). **28 tests nuevos.**
- Verificación ejecutada: `bash init.sh` **exit 0**; tirada focal
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"` en verde. Parseo propio
  de los XML: **371 tests/0 fallos/errores/saltados** en 57 suites (343 preexistentes + 28 nuevos) y
  del informe de lint: **0 errores, 13 warnings preexistentes**, ninguno en ficheros de la feature.
  `:app:checkDebugAarMetadata` en verde.
- Hallazgo/desviación mínima registrada: Ktor 3.6.0 arrastra OkHttp **5.5.0**, cuyo artefacto Android
  declara `minCompileSdk=37` y rompía `checkDebugAarMetadata` con el `compileSdk` del repo (API 36.1).
  `app/build.gradle.kts` **fuerza OkHttp 5.4.0** (la última cuyo artefacto Android acepta
  `compileSdk 36`) con `resolutionStrategy.force` y la versión en `gradle/libs.versions.toml`; no se
  cambia el engine ni `compileSdk`. Documentado en `docs/technical-discovery.md` ("Constraints") y
  anotado en la spec (Unknowns). Segundo hallazgo anotado en la spec: cuando lo que impide el reintento
  es el **deadline** (y no el número de intentos), el motivo es `TIMEOUT`.
- Ficheros cambiados: `gradle/libs.versions.toml`, `app/build.gradle.kts`,
  `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/sazon/app/MainActivity.kt`,
  `app/src/main/java/com/sazon/app/data/ai/*` (6 ficheros nuevos),
  `app/src/test/java/com/sazon/app/data/ai/*` (3 suites nuevas), `docs/domain-model.md`,
  `docs/risks-and-open-questions.md` (pregunta 1 → "Resueltas"; tabla sin filas + nota),
  `docs/technical-discovery.md` (Candidate Stack, clave, `data.ai`, Integrations, Observability,
  Testing, Constraints), `docs/specs/real-ai-provider.md` (hallazgos) y este registro +
  `feature_list.json`. **No** se toca `domain/**`, `data/generator/FakeRecipeGenerator.kt`, `ui/**`
  (salvo el cableado de `MainActivity`), las bases Room, la Taxonomía, `seed-recipes.json`,
  `DESIGN.md`, `docs/design/**` ni `init.sh`.
- Riesgo o incógnita abierta: **verificación manual pendiente** (la persona, emulador de Android
  Studio en Windows; el `adb` de WSL no lo ve): (1) clave válida → recetas reales que pasan el
  Validador; (2) clave ausente/inválida → modo semilla sin fallar (cambiar `local.properties` exige
  reconstruir). Riesgos de producto ya asumidos en `docs/risks-and-open-questions.md` (clave en el APK,
  cuota semanal, latencia/creatividad del modelo).
- Siguiente paso: **validación independiente** de `real-ai-provider` (validator); después, el
  orquestador de `feature-flow`/`github-flow` hará el commit y publicará el PR.

### Sesión 067

- Fecha: 2026-10-12
- Objetivo: validar `real-ai-provider` de forma independiente y persistir la aceptación, en el worktree
  de la rama `feature/real-ai-provider` (ejecutada por el orquestador de `feature-flow`; solo
  hasta-aceptación, **sin push ni PR**: publica `github-flow`).
- Completado: veredicto **`accept`** de un agente con contexto limpio (rol `validator`,
  `feature-validator`); `feature_list.json` pasa a `accepted` con evidencia que nombra la validación
  independiente; registrado el detalle en `docs/validations/real-ai-provider.md`. Sin cambios de código
  tras la validación.
- Verificación ejecutada: el validador re-ejecutó `bash init.sh` (exit 0, test + lint vía
  `scripts/gradle.sh`, sin procesos largos), `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` y
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*" checkDebugAarMetadata`, y
  parseó por su cuenta los XML de resultados y el informe de lint; inspeccionó la política de
  reintentos, el decode tolerante, la degradación por clave ausente, la disciplina de alcance (solo
  `data.ai` importa Ktor) y la ausencia de fuga de la clave.
- Evidencia capturada: **57 suites, 371 tests, 0 fallos/errores/saltados** (28 nuevos de `data.ai`,
  reproducidos exactamente respecto al implementer; 343 preexistentes en verde); lint **0 errores, 13
  warnings** preexistentes y **0 en ficheros de la feature**; `checkDebugAarMetadata` en verde con el
  pin de OkHttp; `domain/**`, `FakeRecipeGenerator`, `ui/**` (salvo el cableado de `MainActivity`), las
  bases Room, la Taxonomía, `seed-recipes.json`, `DESIGN.md`, `docs/design/**` e `init.sh` intactos.
- Hallazgos del validador (3 Low/informativos, ninguno bloqueante): la ruta real del engine OkHttp no se
  cubre en JVM (solo con `MockEngine`) y la verificación E2E es manual, por lo que el pin a OkHttp 5.4.0
  no queda ejercitado automáticamente; `parseContent` usa `runCatching` síncrono (sin impacto práctico);
  `AiCallEvent.detail` para fallos HTTP es solo `HTTP <status>` (cosmético). Acepta como correcta y bien
  documentada la desviación del pin de OkHttp. Sin hallazgos Critical/High/Medium; sin acciones de
  seguridad requeridas (el valor real de `devexpert.apiKey` no aparece en el árbol versionado, logs,
  mensajes de error, tests ni evidencia; `local.properties` sigue ignorado).
- Riesgo o incógnita abierta: la **verificación end-to-end real** es **manual** (la persona, emulador de
  Android Studio en Windows; el `adb` de WSL no lo ve): (1) con `devexpert.apiKey` válida, generar
  recetas reales que pasan el Validador; (2) con la clave ausente o inválida, modo semilla sin fallar
  (cambiar `local.properties` exige reconstruir). Riesgos de producto ya asumidos en
  `docs/risks-and-open-questions.md` (clave en el APK, cuota semanal, latencia/creatividad del modelo).
  Se mantiene el backfill de capturas de UI de features anteriores (sesión 046).
- Siguiente paso: **commit local** de la rama `feature/real-ai-provider` (lo hace el orquestador de
  `feature-flow`); la publicación (push + PR) queda para `github-flow`, que nunca hace merge. Con esta
  feature, el plan de features del MVP queda **cerrado** (0 `not_started`, 0 `passing`).

### Sesión 068

- Fecha: 2026-10-12
- Objetivo: implementar la correctiva `ai-provider-timeouts` (issue #68) según
  `docs/specs/ai-provider-timeouts.md`, en la rama `feature/ai-provider-timeouts` (worktree); solo hasta
  implementación y autoverificación (`passing`), **sin commit/push/PR** (los hace el orquestador).
- Completado:
  - `AiProviderPolicy` (único sitio de la política): `SOCKET_TIMEOUT_MILLIS` 30 s → **90 s**,
    `REQUEST_TIMEOUT_MILLIS` 40 s → **120 s**, `TOTAL_DEADLINE_MILLIS` 60 s → **180 s** (KDoc con la
    latencia medida: HTTP 200 en 28,0 s / 6.194 tokens, issue #68); `CONNECT_TIMEOUT_MILLIS` 10 s,
    `MAX_ATTEMPTS` 2, `RETRY_BACKOFF_MILLIS` 500 ms y `RETRY_AFTER_MAX_MILLIS` 5 s **sin cambios**.
    `attemptRequestTimeoutMillis` (recorte `min(120 s, restante)`) tampoco cambia de lógica.
  - `AiProviderPolicy.transportFailureReason(cause)` nueva y **pura**: `HttpRequestTimeoutException`,
    `java.net.SocketTimeoutException` e `InterruptedIOException` → `TIMEOUT`; `IOException` genérico
    (incluido el `ConnectTimeoutException` de Ktor, que es un `java.net.ConnectException`) → `NETWORK`;
    cualquier otra causa, incluido `null` → `UNEXPECTED`. Los **dos** bloques de `catch` de `sendAttempt`
    (petición y `bodyAsText()`) pasan por ella, capturando `SocketTimeoutException` **antes** del
    `IOException` genérico y relanzando `CancellationException` primero.
  - KDoc de `AiUnavailableReason.TIMEOUT` (incluye timeout de socket) y `NETWORK` (fallo que no es
    timeout); comentario de `throwDeadlineExhausted` referido a la constante.
  - `AiProviderPolicyTest`: corregida la única aserción que codificaba el valor viejo
    (`attemptRequestTimeoutMillis(REQUEST + 1)`), frontera del recorte (`REQUEST`/`REQUEST ± 1`,
    `TOTAL_DEADLINE_MILLIS`, 0, negativo), test de **holgura** (`REAL_LATENCY_MILLIS = 28_000L`: socket
    ≥ 2×, intento ≥ 3×, margen de reintento, connect 10 s) y test de **clasificación** del fallo de
    transporte (positivos/negativos/nulo).
  - `DevExpertRecipeGeneratorTest`: timeout de socket en los dos intentos → 2 intentos,
    `AiUnavailableException(TIMEOUT)` y evento final `UNAVAILABLE` con `detail = "TIMEOUT"`; timeout de
    socket y luego 200 → 2 intentos, recetas, evento `RETRY` con `detail = "TIMEOUT"` y backoff fijo de
    500 ms; intento lento (35 s de reloj inyectado) → 1 llamada y recetas.
  - Docs durables: `docs/technical-discovery.md` (valores nuevos + clasificación en el párrafo de
    `data.ai`, "Integrations" y "Testing and Verification"), `docs/risks-and-open-questions.md` (bullet
    "Errores y reintentos de la IA" en *Resueltas*) y `docs/specs/real-ai-provider.md` (nota de
    resolución en el Unknown "Latencia real de 6 recetas"; el resto de la spec histórica no se reescribe).
- Verificación ejecutada:
  1. Tirada focal `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"` →
     `BUILD SUCCESSFUL`; parseo propio de los XML: **3 suites / 33 tests / 0 fallos / 0 errores /
     0 saltados** (`AiProviderPolicyTest` 10, `AiRecipePayloadTest` 5, `DevExpertRecipeGeneratorTest` 18).
  2. Gate `bash init.sh` → exit 0 (test + lint vía `scripts/gradle.sh`, sin procesos largos); parseo
     propio de los XML: **57 suites / 376 tests / 0 fallos / 0 errores / 0 saltados** (371 preexistentes
     + 5 nuevos, ninguno eliminado); `lint-results-debug.xml`: **0 errores y 13 warnings** preexistentes
     (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), **0**
     con localización bajo `app/src`.
  3. Prueba de **regresión real** (temporal, revertida): con los valores viejos (socket 30 s) el test de
     holgura **falla** en su primera aserción ("El socket timeout debe ser al menos 2x la latencia
     medida"); con los nuevos queda verde. En `main`, un `SocketTimeoutException` caía en el `catch`
     genérico de `IOException` y se etiquetaba `NETWORK` (hoy `TIMEOUT`).
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/data/ai/AiProviderPolicy.kt`,
  `.../DevExpertRecipeGenerator.kt`, `.../AiDiagnostics.kt` (modificados),
  `app/src/test/java/com/sazon/app/data/ai/AiProviderPolicyTest.kt` y `.../DevExpertRecipeGeneratorTest.kt`
  (modificados), `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`,
  `docs/specs/real-ai-provider.md`, `feature_list.json` (`ai-provider-timeouts` a `passing`) y este
  registro.
- Hallazgos relevantes: el `git grep 30_000/40_000/60_000` en `data.ai` deja **dos** coincidencias, ambas
  argumentos `remainingMillis` de `canRetry` en el test preexistente (`60_000L`), ninguna constante de
  timeout; se dejan intactas para no alterar tests aceptados. `AiHttpClientFactory.kt` sigue leyendo las
  constantes (no tiene literales propios), así que no se toca.
- Riesgo o incógnita abierta: la **verificación end-to-end real** sigue siendo **manual** (la persona,
  emulador de Android Studio en Windows; el `adb` de WSL no lo ve): (1) con `devexpert.apiKey` válida, el
  Generador/Home/Búsqueda devuelven recetas reales (~28 s, carga cancelable) sin degradar a modo semilla;
  (2) con la clave ausente, modo semilla sin fallar. La latencia real puede ser mayor y variable (28,0 s
  es **una** medición): si aparecen operaciones > 90 s, la degradación sigue siendo honesta (`TIMEOUT`) y
  la calibración es revisable en el mismo sitio. Se mantiene el backfill de capturas de UI (sesión 046).
- Siguiente paso: **validación independiente** de `ai-provider-timeouts` con `feature-validator`
  (la feature queda `passing`, **no** `accepted`); después, commit local de la rama (orquestador de
  `feature-flow`) y publicación por `github-flow`.

### Sesión 069

- Fecha: 2026-10-12
- Objetivo: validar `ai-provider-timeouts` de forma independiente y persistir la aceptación. Ejecutada
  por el orquestador de `feature-flow` en la rama `feature/ai-provider-timeouts` (worktree), solo
  hasta-aceptación; **sin push ni PR** (publica `github-flow`).
- Completado: veredicto **`accept`** de un agente con contexto limpio (rol `validator`);
  `feature_list.json` pasa a `accepted` con evidencia que nombra la validación independiente; se añade
  `docs/validations/ai-provider-timeouts.md`. Sin cambios de código tras la validación (solo
  estado/evidencia/registro).
- Verificación ejecutada: el validador re-ejecutó `bash init.sh` (exit 0, test + lint vía
  `scripts/gradle.sh`); al ver test/lint en `UP-TO-DATE` forzó
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (34/34 tareas) y
  `bash scripts/gradle.sh lint --rerun-tasks` (30/30 tareas), y parseó por su cuenta los XML de
  resultados y el informe de lint. Comprobó con `javap` sobre los jars reales de la caché de Gradle la
  jerarquía de excepciones (`HttpRequestTimeoutException extends java.io.IOException`;
  `ConnectTimeoutException extends java.net.ConnectException`; el engine OkHttp mapea
  `SocketTimeoutException` al typealias de Ktor) y que no hay `withTimeout` ni otro `*_MILLIS` que acote
  al proveedor.
- Evidencia capturada: **57 suites, 376 tests, 0 fallos/errores/saltados** (371 preexistentes + 5 nuevos
  de `data.ai`: `AiProviderPolicyTest` 10, `AiRecipePayloadTest` 5, `DevExpertRecipeGeneratorTest` 18);
  lint 0 errores y 13 warnings preexistentes, 0 bajo `app/src`. Escenarios 1-5 de la spec confirmados
  (holgura con `REAL_LATENCY_MILLIS = 28_000L`; frontera del recorte `REQUEST`/`REQUEST±1`/
  `TOTAL_DEADLINE`/`0`/negativo; tabla de clasificación positiva/negativa/`null` con socket timeout →
  `TIMEOUT` y ya no `NETWORK`; socket+200 → `RETRY`/`detail = "TIMEOUT"` con backoff fijo 500 ms;
  regresión de los 28 escenarios aceptados). Alcance: solo `data.ai`, sus tests y las tres docs;
  `AiHttpClientFactory.kt`, `domain/**`, `ui/**`, `data/generator/**`, Room, assets, build/gradle,
  manifiesto, `init.sh`, `scripts/**`, `DESIGN.md` y `docs/design/**` intactos; sin dependencias nuevas.
  Seguridad: sin clave/token en el diff; `local.properties` ignorado; tests con la clave ficticia
  `test-key`.
- Hallazgos del validador (4 Low/informativos, ninguno bloqueante): redacción imprecisa de la spec sobre
  qué aserciones fallan con los valores viejos (fallan 1, 3 y 4; la 2 se cumple con 40 ≥ 30, y la prueba
  de regresión sigue siendo válida); el test de "intento lento" usa reloj inyectado y no ejercita el
  plugin `HttpTimeout` real (el discriminador viejo-vs-nuevo es el test de holgura y los de socket
  timeout); `docs/specs/real-ai-provider.md` conserva 30/40/60 s en sus secciones históricas a propósito;
  la spec `docs/specs/ai-provider-timeouts.md` estaba sin commitear y el commit de aceptación la incluye.
  Sin hallazgos Critical/High/Medium.
- Riesgo o incógnita abierta: ninguno nuevo. Queda la verificación **manual** de la persona en el
  emulador de Android Studio (Windows; el `adb` de WSL no lo ve): con clave válida, el
  Generador/Home/Búsqueda devuelven recetas reales (~28 s) sin modo semilla y logcat con
  `detail = "TIMEOUT"` (nunca `NETWORK`) si aún se agota; con clave ausente, modo semilla sin fallar.
  Se mantiene el backfill de capturas de UI de features previas (sesión 046).
- Siguiente paso: commit local de la rama `feature/ai-provider-timeouts` (lo hace el orquestador de
  `feature-flow`); la publicación queda para `github-flow`. No hay más features pendientes en el plan.

### Sesión 070

- Fecha: 2026-10-12
- Objetivo: implementar la correctiva `ai-recipe-decode` (issue #70) según
  `docs/specs/ai-recipe-decode.md`, en la rama `feature/ai-recipe-decode` (worktree); solo hasta
  implementación y autoverificación (`passing`), **sin commit/push/PR** (los hace el orquestador).
- Completado:
  - **Causa raíz corregida**: `AiRecipePayload.decode` delegaba en `RecipeValidator.validateResponse`,
    que deserializa cada elemento con `Recipe.serializer()`; `Recipe.id`/`source` son **obligatorios** y
    el contrato §4 **no los pide**, así que toda receta caía como `SCHEMA` y la operación quedaba EMPTY.
  - **Capa anti-corrupción en `data.ai`** (sin tocar el dominio):
    - `AiRecipeDraft` (nuevo): DTO del **wire** del contrato §4 (sin `id`/`source`/`matchScore`) que
      reutiliza `RecipeIngredient`/`RecipeStep`/`Nutrition`; `toRecipe(id)` fija `source = RecipeSource.AI`
      y `matchScore = null`. Con `ignoreUnknownKeys = true`, un `id`/`source`/`matchScore` del modelo se
      ignora (los posee la app).
    - `AiRecipeIds` (nuevo): id **determinista** = `ai-` + SHA-256 hex minúscula de la cadena canónica
      (prefijo `ai-recipe-v1`, separadores `U+001F`/`U+001E`), **sin `Random`, reloj ni `UUID`**; un
      duplicado exacto en la misma llamada se desambigua con `-2`/`-3`… (determinista por orden).
    - `AiRecipePayload.decode` reescrito: `content` nulo/blanco → vacío; valla markdown (se conserva);
      parseo tolerante del root `{"recipes":[...]}`; elemento a elemento con `AiRecipeDraft` (el que no
      encaja se salta = descarte SCHEMA sin tumbar el resto); la app completa `id`/`source` **antes** de
      `RecipeValidator.validateRecipe(recipe, taxonomy, AiProviderPolicy.NEUTRAL_CONSTRAINTS)`; nunca
      lanza. KDoc actualizado.
  - **Tests con la forma REAL del contrato** (JSON **literal** sin `id`/`source`; nunca
    `RecipeJson.encode`, que era el punto ciego de #70): `AiRecipePayloadTest` reescrito (8) con los
    escenarios 1, 2, 4, 5 y 6; `AiRecipeIdsTest` nuevo (6) con el escenario 3; y
    `DevExpertRecipeGeneratorTest` ampliado (19) con el escenario 7 (regresión end-to-end con
    `GenerationService` real). Los 18 tests previos de esa suite se conservan intactos (siguen usando su
    envelope con `RecipeJson.encode`, que ejercita el camino con `ingredientId` para dieta/restricciones).
  - **Docs durables**: `docs/technical-discovery.md` (párrafo de `data.ai` + *Integrations* + *Testing and
    Verification*), `docs/domain-model.md` (drift del `id` corregido: la app asigna `id` y `source = ai`
    **al decodificar** la respuesta, no "al persistir") y `docs/domain/taxonomy.md` §4 (una línea: `id`,
    `source` y `matchScore` los pone la app; el modelo no debe enviarlos).
- Verificación ejecutada:
  1. Reproducción del bug con un **test temporal** (creado y borrado en esta sesión): sobre el mismo JSON
     literal sin `id`/`source`, el camino viejo (`RecipeValidator.validateResponse`, el de `main`) acepta
     **0** y descarta 1 como `SCHEMA`, mientras `AiRecipePayload.decode` acepta **1**. `BUILD SUCCESSFUL`.
  2. Tirada focal `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"` →
     `BUILD SUCCESSFUL`; parseo propio de los XML: **4 suites / 43 tests / 0 fallos / 0 errores /
     0 saltados** (`AiProviderPolicyTest` 10, `AiRecipeIdsTest` 6, `AiRecipePayloadTest` 8,
     `DevExpertRecipeGeneratorTest` 19).
  3. Gate `bash init.sh` → **exit 0** (test + lint vía `scripts/gradle.sh`, sin procesos largos); parseo
     propio de los XML: **58 suites / 386 tests / 0 fallos / 0 errores / 0 saltados** (376 preexistentes +
     10 nuevos, ninguno eliminado); `lint-results-debug.xml`: **0 errores y 13 warnings** preexistentes
     (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), **0**
     con localización bajo `app/src`.
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/data/ai/{AiRecipeDraft,AiRecipeIds}.kt`
  (nuevos), `.../AiRecipePayload.kt` (modificado), `app/src/test/java/com/sazon/app/data/ai/{AiRecipeIdsTest}.kt`
  (nuevo), `.../{AiRecipePayloadTest,DevExpertRecipeGeneratorTest}.kt` (modificados),
  `docs/technical-discovery.md`, `docs/domain-model.md`, `docs/domain/taxonomy.md`,
  `docs/specs/ai-recipe-decode.md` (hallazgo), `feature_list.json` (`ai-recipe-decode` a `passing`) y este
  registro. **No** se toca `Recipe.kt`, `RecipeValidator.kt`, `RecipeJson.kt`, `AiProviderPolicy.kt`,
  `AiDiagnostics.kt`, `OpenAiChat.kt`, `AiHttpClientFactory.kt`, `DevExpertRecipeGenerator.kt`,
  `domain/**`, `data/generator/**`, `ui/**`, las bases Room, `app/src/main/assets/**`,
  `app/build.gradle.kts`, `gradle/libs.versions.toml`, `AndroidManifest.xml`, `init.sh`, `scripts/**`,
  `DESIGN.md` ni `docs/design/**`; sin dependencias nuevas; sin capturas de UI (no es feature de UI).
- Inspección de decisiones: `git grep "validateResponse" app/src/main` deja solo la **definición** en
  `RecipeValidator.kt` y dos comentarios KDoc (`GenerationService`, `AiSearchService`): sin llamador real
  (decisión documentada; el método no se modifica y sigue cubierto por `RecipeValidatorTest`). El esquema
  **no** se debilita: `id`/`source` siguen obligatorios en `Recipe`.
- Hallazgo de spec anotado: la lista de campos de `AiRecipeIds.canonical` **omitía `tips`**, pero el
  *Unknown* de la spec afirma que "un cambio de contenido (p. ej. tips) cambia el `id`". Se incluyen los
  `tips` en la cadena canónica (de lo contrario, recetas que solo difieren en los trucos compartirían
  `id` entre llamadas) y el test `AiRecipeIdsTest.contenido_distinto_produce_id_distinto` lo cubre.
- Riesgo o incógnita abierta: la **verificación manual** sigue siendo de la persona (emulador de Android
  Studio en Windows; el `adb` de WSL no lo ve): con `devexpert.apiKey` válida, la búsqueda *"arroz con
  pollo"* y el Generador muestran recetas reales que pasan el Validador (logcat `SUCCESS` en vez de
  `EMPTY`); con la clave vaciada o ausente, modo semilla sin fallar. Sigue vigente el *Unknown* de
  robustez del modelo fuera del contrato §4 (un campo obligatorio ausente/mal tipado descarta esa receta;
  no tumba el lote) y el de estabilidad de ids (un cambio de contenido cambia el `id`; el Favorito
  conserva su copia). Se mantiene el backfill de capturas de UI (sesión 046).
- Siguiente paso: **validación independiente** de `ai-recipe-decode` con `feature-validator` (la feature
  queda `passing`, **no** `accepted`); después, commit local de la rama `feature/ai-recipe-decode`
  (orquestador de `feature-flow`) y publicación por `github-flow`.

### Sesión 071

- Fecha: 2026-10-12
- Objetivo: validar `ai-recipe-decode` (issue #70) de forma independiente y persistir la aceptación.
  Ejecutada por el orquestador de `feature-flow` en la rama `feature/ai-recipe-decode` (worktree), solo
  hasta-aceptación; **sin push ni PR** (publica `github-flow`).
- Completado: veredicto **`accept`** de un agente con contexto limpio (rol `validator`); `feature_list.json`
  pasa a `accepted` con evidencia que nombra la validación independiente; se añade
  `docs/validations/ai-recipe-decode.md`. Sin cambios de código tras la validación (solo
  estado/evidencia/registro).
- Verificación ejecutada: el validador re-ejecutó `bash init.sh` (exit 0, test + lint vía
  `scripts/gradle.sh`); al ver test/lint en `UP-TO-DATE` forzó
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (34/34 tareas, 2m39s) y
  `bash scripts/gradle.sh lint --rerun-tasks` (30/30 tareas, 2m43s), y parseó por su cuenta los XML de
  resultados y el informe de lint. Reprodujo además el bug y el arreglo con un verificador Java propio
  fuera del repo (`/tmp/opencode/sazon-verify/`, JDK 21 sobre las clases compiladas + `taxonomy.json`
  real; ningún fichero del repo tocado), con **16/16 comprobaciones**.
- Evidencia capturada: **58 suites, 386 tests, 0 fallos/errores/saltados** (376 preexistentes + 10 nuevos
  de `data.ai`: `AiProviderPolicyTest` 10, `AiRecipeIdsTest` 6, `AiRecipePayloadTest` 8,
  `DevExpertRecipeGeneratorTest` 19); lint 0 errores y 13 warnings preexistentes, 0 bajo `app/src`.
  Escenarios 1-7 confirmados: forma real del contrato → recetas válidas con `source = AI`, id `ai-<sha256>`
  y `matchScore = null`; id único con duplicado literal (`-2`/`-3`) y determinista por contenido; reglas
  del Validador vivas (ids de Taxonomía inexistentes → `SCHEMA`; deriva de tiempos → `INCOHERENT`; un
  elemento roto no tumba el lote); tolerancia sin lanzar; y regresión end-to-end con `GenerationService`
  real. **Discriminador de #70** verificado de forma independiente: el mismo JSON literal sin `id`/`source`
  por el camino de `main` (`RecipeValidator.validateResponse`) da `accepted 0` / 1 descarte `SCHEMA`,
  mientras `AiRecipePayload.decode` da 1 receta. Alcance: solo `data.ai` (+2 nuevos), sus 3 tests, 3 docs
  y harness; `Recipe.kt`, `RecipeValidator.kt`, `RecipeJson.kt`, `AiProviderPolicy.kt`, `AiDiagnostics.kt`,
  `OpenAiChat.kt`, `AiHttpClientFactory.kt`, `DevExpertRecipeGenerator.kt`, `domain/**`, `ui/**`,
  `data/generator/**`, Room, assets, build/gradle, manifiesto, `init.sh`, `scripts/**`, `DESIGN.md` y
  `docs/design/**` intactos; sin dependencias nuevas. Seguridad: sin claves/secretos en el diff;
  `local.properties` ignorado; tests con `test-key`; el `id` es un hash one-way sin PII.
- Hallazgos del validador (3 Low + varios informativos, ninguno bloqueante): (1) la cadena canónica del
  `id` omite `RecipeStep.description` (dos recetas que solo difieren en la prosa de un paso comparten `id`
  base entre llamadas; impacto bajo, cumple la lista de campos de la spec; sugerencia de correctiva
  futura con vector dorado); (2) la frase añadida a `docs/domain/taxonomy.md` §4 sobre `matchScore`
  convive con el prompt real que todavía lo pide (drift preexistente, `PromptBuilder` fuera de alcance);
  (3) falta vector dorado del hash y un test de que un `id`/`source` del modelo se ignora (verificado
  externamente por el validador). Sin hallazgos Critical/High/Medium.
- Riesgo o incógnita abierta: la **verificación manual** sigue siendo de la persona (emulador de Android
  Studio en Windows; el `adb` de WSL no lo ve): con `devexpert.apiKey` válida, la búsqueda *"arroz con
  pollo"* y el Generador muestran recetas reales que pasan el Validador (logcat `SUCCESS` en vez de
  `EMPTY`); con la clave vaciada o ausente, modo semilla sin fallar. Se mantiene el backfill de capturas
  de UI de features previas (sesión 046).
- Siguiente paso: commit local de la rama `feature/ai-recipe-decode` (lo hace el orquestador de
  `feature-flow`); la publicación (push + PR) queda para `github-flow`, que nunca hace merge. No hay más
  features pendientes en el plan.

### Sesión 072

- Fecha: 2026-10-12
- Objetivo: implementar la correctiva `ai-call-diagnostics` (issue #72) según
  `docs/specs/ai-call-diagnostics.md`, en la rama `feature/ai-call-diagnostics` (worktree); solo hasta
  implementación y autoverificación (`passing`), **sin commit/push/PR** (los hace el orquestador).
- Completado:
  - `AiProviderPolicy` (único sitio de la política): `SOCKET_TIMEOUT_MILLIS` **90 s → 120 s**,
    `REQUEST_TIMEOUT_MILLIS` **120 s → 150 s**, `TOTAL_DEADLINE_MILLIS` **180 s → 240 s**;
    `CONNECT_TIMEOUT_MILLIS` (10 s), `MAX_ATTEMPTS` (2), `RETRY_BACKOFF_MILLIS` (500 ms) y
    `RETRY_AFTER_MAX_MILLIS` (5 s) **intactos**, y `attemptRequestTimeoutMillis`/`canRetry`/`Retry-After`
    sin cambio de lógica. KDoc con la evidencia multi-modelo (peor caso `mimo-v2.6-pro` 115,8 s; alias
    diario `chat` 28,0/33,1/42,1/46,6 s; `chat`+`json_object` 85,3 s; `chat-pro`+`json_schema` 97,0 s;
    `deepseek-v4-flash` 52,6 s).
  - **Diagnóstico de `EMPTY`**: `AiDecodeResult.kt` nuevo (`internal enum AiContentState`
    BLANK/ILLEGIBLE/RECIPES + `internal data class AiDecodeResult` con `accepted`, `received`,
    `discardedByReason`, `contentChars`, `contentBytes`, `contentState`). `AiRecipePayload.decode` deja
    de devolver `List<Recipe>` y devuelve el resultado: cuenta `received`/`accepted`, cuenta los
    descartes **por motivo** (solo motivos presentes, en orden de `DiscardReason`, perdiendo el `detail`
    a propósito) y calcula longitudes UTF-16/UTF-8 (`0/0` si el contenido está en blanco); sigue **sin
    lanzar** y con `constraints` por defecto `NEUTRAL_CONSTRAINTS` (el proveedor **no** pre-filtra: el
    estado Bloqueado de `ai-search` se conserva).
  - `AiDiagnostics.kt`: `AiCallEvent` gana **5 campos aditivos** con default (`contentBytes`,
    `contentChars`, `received`, `accepted` = `null`; `discardedByReason` = `emptyMap()`);
    `DevExpertRecipeGenerator.report(...)` los rellena desde el resultado del decode (`null`/vacío
    cuando no hubo decode: clave ausente, `RETRY`, `UNAVAILABLE`, deadline). `MainActivity`:
    `Log.d("SazonAi", …)` pasa a mostrar `content=…c/…b`, `received=…`, `accepted=…` y `discards={…}`.
    **Nunca** clave, `Authorization`, cuerpos ni títulos: solo longitudes, recuentos y motivos.
  - Tests (7 nuevos): `AiProviderPolicyTest` recalibrado (`REAL_LATENCY_MILLIS = 115_800L` +
    `CHAT_WORST_LATENCY_MILLIS = 46_600L`, invariantes y márgenes de Scenario 1, frontera del recorte);
    `AiRecipePayloadTest` 11 (adaptado a `.accepted` + resumen con los **cuatro** `DiscardReason`
    representables con restricciones no neutras y en orden del enum + `contentState` con longitudes
    UTF-16 vs UTF-8 + tolerancia/ids/reglas/`matchScore`); `DevExpertRecipeGeneratorTest` 23
    (escenario 2 `EMPTY` por vacío/ilegible con longitudes y 0/0, escenario 3 lote parcial con
    `{SCHEMA:2, INCOHERENT:1}`, escenario 4 todas descartadas → `EMPTY` con motivos sin lanzar y
    escenario 5 no-fuga de clave/cabecera/cuerpos/títulos).
  - Docs durables: `docs/technical-discovery.md` (párrafo de `data.ai` con 120/150/240 y el decode
    tipado, *Integrations*, *Observability* con los campos nuevos y el detalle de `EMPTY`, *Testing and
    Verification* con el peor caso y los tests del evento), `docs/risks-and-open-questions.md` (bullet
    "Errores y reintentos de la IA"), y notas de *Unknown* en `docs/specs/real-ai-provider.md` (latencia
    y `AiCallEvent`) y `docs/specs/ai-provider-timeouts.md` (latencia y "el motivo no se registraba").
- Verificación ejecutada:
  1. Tirada focal `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"` →
     `BUILD SUCCESSFUL`; **50 tests / 0 fallos** (`AiProviderPolicyTest` 10, `AiRecipeIdsTest` 6,
     `AiRecipePayloadTest` 11, `DevExpertRecipeGeneratorTest` 23).
  2. Prueba de **regresión real** (temporal, revertida): con los valores viejos (90/120/180) el test de
     holgura **falla** en su primera aserción ("El socket timeout debe superar el peor caso medido
     (mimo-v2.6-pro, 115,8 s)"; `90_000 > 115_800` es falso); con los nuevos queda verde.
  3. Gate `bash init.sh` → **exit 0** (test + lint vía `scripts/gradle.sh`, sin procesos largos);
     parseo propio de los XML: **58 suites / 393 tests / 0 fallos / 0 errores / 0 saltados** (386
     preexistentes + 7 nuevos, ninguno eliminado); `lint-results-debug.xml`: **0 errores** y **7
     warnings** preexistentes (GradleDependency 5, NewerVersionAvailable 1, OldTargetApi 1), todos en el
     build/version catalog y **0 bajo `app/src`** (la diferencia frente a los 13 históricos es del
     chequeo de versiones disponibles de lint, que depende de su caché de red; ningún warning nuevo).
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/data/ai/AiDecodeResult.kt` (nuevo),
  `.../AiProviderPolicy.kt`, `.../AiRecipePayload.kt`, `.../AiDiagnostics.kt`,
  `.../DevExpertRecipeGenerator.kt` y `app/src/main/java/com/sazon/app/MainActivity.kt` (log),
  `app/src/test/java/com/sazon/app/data/ai/{AiProviderPolicyTest,AiRecipePayloadTest,DevExpertRecipeGeneratorTest}.kt`,
  las cuatro docs y este registro + `feature_list.json`. **No** se toca `Recipe.kt`,
  `RecipeValidator.kt`, `AiRecipeDraft.kt`, `AiRecipeIds.kt`, `AiHttpClientFactory.kt`, `OpenAiChat.kt`,
  `domain/**`, `data/generator/**`, `ui/**`, las bases Room, `app/src/main/assets/**`,
  `app/build.gradle.kts`, `gradle/libs.versions.toml`, `AndroidManifest.xml`, `init.sh`, `scripts/**`,
  `DESIGN.md` ni `docs/design/**`; sin dependencias nuevas; sin capturas de UI (no es feature de UI).
- Hallazgo relevante (spec actualizada): el `internal` que la spec proponía para
  `AiDecodeResult`/`AiContentState` **no compila** con `AiRecipePayload.decode` público
  (`'public' function exposes its 'internal' return type`). Resuelto con el criterio que la spec pide:
  los dos tipos siguen `internal` y `decode` pasa a `internal fun` (el objeto `AiRecipePayload` sigue
  público); los tests JVM del mismo módulo lo usan por los *friend paths*, igual que ya hacían con
  `AiRecipeIds`/`AiRecipeDraft`. Anotado en "Hallazgos de la sesión de implementación" de la spec.
- Riesgo o incógnita abierta: la **verificación manual** sigue siendo de la persona (emulador de Android
  Studio en Windows; el `adb` de WSL no lo ve): con `devexpert.apiKey` válida, logcat (`SazonAi`) debe
  mostrar por llamada estado, intento, duración, `content=…c/…b`, `received=… accepted=…` y
  `discards={…}`, con las recetas reales llegando a Resultados; con un lote sin recetas aceptadas el log
  debe indicar el motivo; con la clave ausente o vaciada sigue el modo semilla. Sigue vigente el margen
  estrecho con `mimo-v2.6-pro` (115,8 s vs socket 120 s: otra medición por encima degradará como
  `TIMEOUT` honesto, revisable en el mismo sitio) y el `Unknown` de que `DIET_PATTERN`/`HARD_RESTRICTION`
  no son alcanzables en producción (restricciones neutras). Se mantiene el backfill de capturas de UI
  (sesión 046).
- Siguiente paso: **validación independiente** de `ai-call-diagnostics` con `feature-validator` (la
  feature queda `passing`, **no** `accepted`); después, commit local de la rama
  `feature/ai-call-diagnostics` (orquestador de `feature-flow`) y publicación por `github-flow`.

### Sesión 073

- Fecha: 2026-10-12
- Objetivo: validar `ai-call-diagnostics` de forma independiente y persistir la aceptación. Ejecutada
  por el orquestador de `feature-flow` en la rama `feature/ai-call-diagnostics` (worktree), solo
  hasta-aceptación; **sin push ni PR** (la publicación es de `github-flow`).
- Completado: veredicto **`accept`** de un agente con contexto limpio (rol `validator`); `feature_list.json`
  pasa a `accepted` con la evidencia de la validación independiente; sin cambios de código tras la
  validación. Commit local de la rama `feature/ai-call-diagnostics` (lo hace el orquestador).
- Verificación ejecutada: el validador re-ejecutó `bash init.sh` (exit 0, test + lint vía
  `scripts/gradle.sh`, sin procesos largos) y, al ver la primera pasada en `UP-TO-DATE`, forzó
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (34/34 tareas) y
  `bash scripts/gradle.sh lint --rerun-tasks` (30/30 tareas), con **parseo propio** de los XML y del
  informe de lint.
- Evidencia capturada: **58 suites / 393 tests / 0 fallos/errores/saltados** (386 preexistentes + 7
  nuevos de `data.ai`: `AiProviderPolicyTest` 10, `AiRecipeIdsTest` 6, `AiRecipePayloadTest` 11,
  `DevExpertRecipeGeneratorTest` 23; regresión `RecipeValidatorTest` intacta); lint 0 errores y 10
  warnings (todos en el build/version catalog, **0 bajo `app/src`**). Reprodujo la **regresión de forma
  independiente por aritmética**: con los valores viejos (90/120/180) fallan `SOCKET > peor caso`
  (`90_000 > 115_800` falso), `SOCKET ≥ 2×` el peor del alias `chat` (`90_000 ≥ 2×46_600` falso) y el
  margen de reintento (`180_000-115_800-500 ≥ 115_800` falso); con 120/150/240 las tres son ciertas.
  Confirmó los escenarios 1-6 (holgura con `REAL_LATENCY_MILLIS` = 115_800 y frontera del recorte;
  `EMPTY` por vacío/ilegible con `received=0`/`accepted=0` y longitudes `0/0` vs `>0` y 1 sola llamada;
  lote parcial `received=4 accepted=1 {SCHEMA:2, INCOHERENT:1}`; los **cuatro** `DiscardReason`
  representables con restricciones no neutras y en orden del enum; todas descartadas → `EMPTY` con
  motivos sin lanzar; no-fuga: ningún `toString()` de evento contiene `test-key`, `Bearer`/`Authorization`,
  el cuerpo del modelo ni títulos), que `AiProviderPolicy` sigue siendo el **único** sitio de la política
  (connect 10 s, `MAX_ATTEMPTS` 2, backoff 500 ms, `Retry-After` ≤ 5 s y
  `attemptRequestTimeoutMillis`/`canRetry` sin cambios), que `decode` no lanza y usa restricciones neutras
  por defecto (el estado Bloqueado de `ai-search` se conserva) y que `response_format` queda fuera.
  Disciplina de alcance verificada (solo `data.ai` + el `Log.d` de `MainActivity` + 4 docs; `Recipe.kt`,
  `RecipeValidator.kt`, `AiRecipeDraft.kt`, `AiRecipeIds.kt`, `AiHttpClientFactory.kt`, `OpenAiChat.kt`,
  `domain/**`, `ui/**`, Room, Taxonomía, assets, build/version catalog, `AndroidManifest.xml`, `init.sh`,
  `scripts/**`, `DESIGN.md` y `docs/design/**` intactos; sin dependencias nuevas; sin capturas de UI) y
  seguridad (sin claves/secretos en el diff; `local.properties` ignorado; tests con `test-key`; el evento
  no expone clave, `Authorization`, cuerpos ni títulos). Detalle en
  `docs/validations/ai-call-diagnostics.md`.
- Hallazgos del validador (3 Low informativos, ninguno bloqueante): `contentState` (BLANK vs ILLEGIBLE)
  no viaja en el evento (la distinción va por las longitudes `0/0` vs `>0`); el número de warnings de
  lint oscila con su caché de red (siempre 0 bajo `app/src`); `AiRecipePayload.decode` pasó de `public`
  a `internal` (con `AiDecodeResult`/`AiContentState`) para no exponer un tipo `internal` desde una API
  pública, sin llamadores fuera del módulo. Sin hallazgos Critical/High/Medium.
- Ficheros actualizados: `feature_list.json` (feature a `accepted` con la evidencia del validador),
  `docs/validations/ai-call-diagnostics.md` (nuevo) y este registro. Sin cambios de código tras la
  validación.
- Riesgo o incógnita abierta: ninguno nuevo. Queda para la persona la **verificación manual** en el
  emulador de Android Studio (Windows; el `adb` de WSL no lo ve): con `devexpert.apiKey` válida, logcat
  (`SazonAi`) muestra por llamada estado, intento, duración, `content=…c/…b`, `received=… accepted=…` y
  `discards={…}`, y las recetas reales llegan a Resultados; con un lote sin recetas aceptadas el log
  indica el motivo; con la clave ausente o vaciada sigue el modo semilla. Se mantiene el margen estrecho
  con `mimo-v2.6-pro` (115,8 s vs socket 120 s) y el `Unknown` de que `DIET_PATTERN`/`HARD_RESTRICTION`
  no son alcanzables en producción (restricciones neutras). Se mantiene el backfill de capturas de UI
  (sesión 046).
- Siguiente paso: **commit local** de la rama `feature/ai-call-diagnostics` (lo hace el orquestador de
  `feature-flow`); la publicación (push + PR, sin merge) queda para `github-flow`. No hay más features
  del plan del MVP; si aparece un defecto visual/manual, se corrige con una feature correctiva nueva.

### Sesión 074

- Fecha: 2026-10-12
- Objetivo: implementar la cuarta **correctiva** de `real-ai-provider`, `ai-response-format` (issue #74),
  según `docs/specs/ai-response-format.md`, en la rama `feature/ai-response-format` (worktree); solo
  hasta implementación y autoverificación (`passing`), sin commit/push/PR (los hace el orquestador).
  `area: integration`, **no** es UI (sin capturas).
- Completado:
  - `AiProviderPolicy`: `RESPONSE_FORMAT_ENABLED = true`, `RESPONSE_FORMAT_JSON_OBJECT = "json_object"` y
    `requiresResponseFormatFallback(status) = status == 400 || status == 422`. **Sin** tocar
    `isRetryableStatus`, timeouts (120/150/240), `MAX_ATTEMPTS`/backoff/`Retry-After`.
  - `OpenAiChat`: `ResponseFormat` (+ `JSON_OBJECT`) y `ChatCompletionRequest.responseFormat` con
    `@SerialName("response_format") @EncodeDefault(EncodeDefault.Mode.NEVER)` y default `null`; `@OptIn`
    a nivel de fichero. La KDoc deja de afirmar que no se envía `response_format`.
  - `AiDiagnostics`: `enum AiResponseFormat { JSON_OBJECT, NONE }`, `AiCallOutcome.FALLBACK` y
    `AiCallEvent.responseFormat: AiResponseFormat = NONE` (aditivo con default).
  - `DevExpertRecipeGenerator`: costura `responseFormatEnabled` por constructor (default de la política);
    `sendAttempt(prompt, remaining, includeFormat)` monta `ResponseFormat.JSON_OBJECT` solo si
    `includeFormat`; rama de fallback (400/422, **una vez** por operación, sin backoff, `attempt += 1`)
    antes de la lógica de transporte; `formatOf(includeFormat)` en todas las llamadas a `report`.
  - `MainActivity`: `format=${event.responseFormat}` en el `Log.d("SazonAi", …)` (único cambio fuera de
    `data.ai`).
  - Tests: `OpenAiChatTest` (nuevo, Scenario 3) y casos de formato/fallback/observabilidad en
    `DevExpertRecipeGeneratorTest` (Scenarios 1, 2, 4, 5, 6 y 7; el antiguo `nunca_reintenta_un_4xx_no_reintentable`
    se acota a 401/403/404 porque 400/422 ahora disparan el fallback) y `requiresResponseFormatFallback`/
    interruptor por defecto en `AiProviderPolicyTest`.
- Verificación ejecutada: tirada focal `bash scripts/gradle.sh testDebugUnitTest --tests
  "com.sazon.app.data.ai.*"` (BUILD SUCCESSFUL) y gate estándar `bash init.sh` (exit 0, test + lint vía
  `scripts/gradle.sh`, sin procesos largos); parseo propio de los XML y del informe de lint.
- Evidencia capturada: **61 tests / 0 fallos en `data.ai`** (50 preexistentes + 11 nuevos); gate
  **59 suites / 404 tests / 0 fallos / 0 errores / 0 saltados** (393 preexistentes + 11 nuevos); lint
  **0 errores y 13 warnings preexistentes** (GradleDependency 7, NewerVersionAvailable 3,
  AndroidGradlePluginVersion 2, OldTargetApi 1), **0 bajo `app/src`** y 0 nuevos por
  `@EncodeDefault`/`@OptIn`; `git diff --stat` solo con lo previsto y `grep response_format` restringido
  a `data.ai` (código y tests) más la KDoc del fallback.
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/data/ai/{AiProviderPolicy,OpenAiChat,
  AiDiagnostics,DevExpertRecipeGenerator}.kt` y `app/src/main/java/com/sazon/app/MainActivity.kt`
  (modificados); `app/src/test/java/com/sazon/app/data/ai/{AiProviderPolicyTest,DevExpertRecipeGeneratorTest}.kt`
  (modificados) y `app/src/test/java/com/sazon/app/data/ai/OpenAiChatTest.kt` (nuevo); `docs/technical-discovery.md`,
  `docs/risks-and-open-questions.md`, `docs/adr/0003-ai-provider.md`, `docs/specs/real-ai-provider.md`,
  `docs/specs/ai-call-diagnostics.md`, `feature_list.json` y este registro.
- Hallazgos relevantes: el test `nunca_reintenta_un_4xx_no_reintentable` agrupaba 400/422 con los 4xx
  no reintentables; con el formato activo pasan a 2 llamadas (fallback), así que se acotó a 401/403/404 y
  el comportamiento nuevo se cubre con tests propios. La primera pasada falló una aserción ingenua
  (`body.contains("null")`) porque el **prompt** enumera la unidad `null`: se sustituyó por el parseo
  preciso de `response_format` (`OpenAiChatTest` cubre el negativo de `"response_format":null`).
- Riesgo o incógnita abierta: se mantienen los `Unknown` de la spec (latencia de `json_object` 85,3 s
  frente a `chat` 28,0–46,6 s; un 400/422 no relacionado con el formato también dispararía el fallback,
  coste de una petición extra con rastro `FALLBACK`; `FALLBACK` no se reintenta si el 2.º intento también
  da 400/422). Queda la verificación **manual** de emulador de la persona (logcat `format=…`/`FALLBACK`;
  interruptor apagado idéntico; clave ausente → modo semilla), no automatizable desde WSL. Se mantiene el
  backfill de capturas de UI (sesión 046).
- Siguiente paso: validación independiente de `ai-response-format` con `feature-validator`; después,
  commit local de la rama (lo hace el orquestador de `feature-flow`) y publicación por `github-flow`.

### Sesión 075

- Fecha: 2026-10-12
- Objetivo: **reparar** `ai-response-format` (issue #74) tras el veredicto **`revise`** del validador
  independiente (`docs/validations/ai-response-format.md`, Finding 1 **Medium**): el fallback de formato
  podía añadir un **tercer** intento HTTP y romper la invariante `MAX_ATTEMPTS = 2`. Solo hasta
  implementación y autoverificación (`passing`), sin commit/push/PR.
- Diagnóstico: en `DevExpertRecipeGenerator.execute`, la condición del fallback
  (`includeFormat && requiresResponseFormatFallback(status) && !usedFallback`) **no** consultaba el
  presupuesto de intentos. Con `503 → 400` (o `IOException → 422`), el primer intento consumía el
  reintento de transporte (`attempt = 2`) y el `400/422` del segundo disparaba igualmente el fallback
  (`attempt` pasaba a 3) → `calls = 3`, `formats = [json_object, json_object, null]`.
- Reparación aplicada:
  - La rama `AttemptOutcome.HttpFailure` añade la guarda `attempt < AiProviderPolicy.MAX_ATTEMPTS` a la
    condición del fallback y se elimina `!usedFallback` (redundante: `includeFormat` ya lo implica).
  - Un `400/422` sin presupuesto cae en la rama actual → `UNAVAILABLE`/`HTTP_ERROR` con el formato del
    intento (`JSON_OBJECT`), **sin** tercera petición.
  - KDoc del generador precisada (el fallback solo consume el 2.º intento si queda presupuesto).
  - Docs durables matizadas para que el enunciado sea coherente con la cota: `docs/risks-and-open-questions.md`
    y `docs/technical-discovery.md` (`MAX_ATTEMPTS = 2`).
- Test de regresión nuevo: `DevExpertRecipeGeneratorTest.el_fallback_no_anade_un_tercer_intento` —
  `503 → 400` y `IOException → 422` ⇒ exactamente **2** llamadas, sin evento `FALLBACK`, evento `RETRY`
  de transporte presente, `UNAVAILABLE`/`HTTP_ERROR` y evento final con `responseFormat = JSON_OBJECT`.
  Los tests del fallback aislado (`400/422` en el 1.er intento ⇒ 2 llamadas, la 2.ª sin la clave) siguen
  intactos y en verde.
- Verificación ejecutada: `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"`
  (BUILD SUCCESSFUL) y gate estándar `bash init.sh` (exit 0, test + lint vía `scripts/gradle.sh`, sin
  procesos largos); parseo propio de los XML y del informe de lint.
- Evidencia capturada: **62 tests / 0 fallos en `data.ai`** (61 previos + 1 nuevo;
  `DevExpertRecipeGeneratorTest` 31); gate **59 suites / 405 tests / 0 fallos / 0 errores / 0 saltados**
  (404 previos + 1 nuevo); lint **0 errores y 13 warnings preexistentes** (GradleDependency 7,
  NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), **0 bajo `app/src`**.
- Ficheros cambiados: `app/src/main/java/com/sazon/app/data/ai/DevExpertRecipeGenerator.kt` y
  `app/src/test/java/com/sazon/app/data/ai/DevExpertRecipeGeneratorTest.kt`, más
  `docs/risks-and-open-questions.md` y `docs/technical-discovery.md`; `feature_list.json` y este registro.
  Sin tocar `isRetryableStatus`, timeouts 120/150/240, `MAX_ATTEMPTS`, backoff, `Retry-After`,
  `Recipe.kt`/`RecipeValidator.kt`, `AiRecipePayload`/`AiDecodeResult`/`AiHttpClientFactory`, `ui/**`,
  Room, Taxonomía, build, dependencias, `init.sh`, `scripts/**`, `DESIGN.md` ni `docs/design/**`.
- Hallazgos relevantes: el camino mixto (reintento de transporte + `400/422`) no estaba cubierto por
  ningún test; el nuevo caso lo fija. La interpretación elegida es la de la spec (el fallback **consume**
  el 2.º intento, no lo añade), no la alternativa de ampliar la spec a 3 peticiones.
- Siguiente paso: **re-validación independiente** de `ai-response-format` con `feature-validator`
  (puede ser focal: una condición + un test); después, commit local de la rama (lo hace el orquestador de
  `feature-flow`) y publicación por `github-flow`. Se mantiene la verificación **manual** de emulador de
  la persona (no automatizable desde WSL) y el backfill de capturas de UI (sesión 046).

### Sesión 076

- Fecha: 2026-10-12
- Objetivo: validar `ai-response-format` (correctiva de `real-ai-provider`, issue #74) de forma
  independiente y persistir la aceptación, en el worktree de la rama `feature/ai-response-format`;
  modo hasta-aceptación, sin push ni PR (publica `github-flow`).
- Completado: veredicto **`accept`** de un agente con contexto limpio (rol `validator`,
  `feature-validator`). La primera validación había devuelto **`revise`** (Finding 1 **Medium**: el
  fallback podía añadir un tercer intento HTTP en secuencia mixta), se reparó en la sesión 075
  (`attempt < AiProviderPolicy.MAX_ATTEMPTS` + test de regresión) y la re-validación confirmó el arreglo.
  `feature_list.json` pasa a `accepted` con la evidencia del validador independiente; detalle en
  `docs/validations/ai-response-format.md`. Sin cambios de código tras la validación.
- Verificación ejecutada: el validador re-ejecutó `bash init.sh` (exit 0, test + lint vía
  `scripts/gradle.sh`, sin procesos largos) y, al ver UP-TO-DATE, forzó
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (34/34 tareas) y
  `bash scripts/gradle.sh lint --rerun-tasks` (30/30 tareas); parseó por su cuenta los XML y el informe
  de lint, e **independientemente reprodujo el Fix** con una sonda propia en copia scratch (7/7).
- Evidencia capturada: **59 suites / 405 tests / 0 fallos/errores/saltados** (62 en `data.ai`:
  `AiProviderPolicyTest` 12, `AiRecipeIdsTest` 6, `AiRecipePayloadTest` 11, `DevExpertRecipeGeneratorTest`
  31, `OpenAiChatTest` 2); lint **0 errores, 13 warnings** preexistentes, **0 bajo `app/src`**. La sonda
  del validador: mixto `503→400` y `IOException→422` ⇒ `calls=2` sin `FALLBACK` y `HTTP_ERROR` (el bug
  `calls=3` ya no reproduce); fallback aislado `400/422` ⇒ `calls=2` con `FALLBACK`+`SUCCESS`; persistente
  `400` y `fallback_luego_429` ⇒ `calls=2` (sin tercera); transporte `429` persistente ⇒ `calls=2` ambas
  con formato y `RATE_LIMITED`. Scenarios 1-8 confirmados; `Recipe.kt`, `RecipeValidator.kt`,
  `AiRecipePayload.kt`, `AiDecodeResult.kt`, `AiHttpClientFactory.kt`, `domain/**`, `ui/**`, Room,
  Taxonomía, `seed-recipes.json`, build, `init.sh`, `scripts/**`, `DESIGN.md` y `docs/design/**`
  intactos; sin dependencias nuevas; sin capturas (no es UI). Sin hallazgos Critical/High/Medium
  abiertos; 2 Low informativos no bloqueantes.
- Ficheros actualizados: `feature_list.json` (feature a `accepted` con la evidencia del validador),
  `docs/validations/ai-response-format.md` y este registro. Sin cambios de código.
- Riesgo o incógnita abierta: se mantienen los `Unknown` de la spec (latencia de `json_object` 85,3 s
  frente a `chat` 28,0–46,6 s; un 400/422 no relacionado con el formato dispara igualmente el fallback,
  con rastro `FALLBACK`; `FALLBACK` no se reintenta si el 2.º intento también da 400/422). Queda la
  verificación **manual** de emulador de la persona (logcat `format=…`/`FALLBACK` con recetas reales;
  interruptor apagado → comportamiento idéntico; clave ausente o vaciada → modo semilla), no
  automatizable desde WSL. Se mantiene el backfill de capturas de UI (sesión 046).
- Siguiente paso: **commit local** de la rama `feature/ai-response-format` (lo hace el orquestador de
  `feature-flow`); la publicación (push + PR, sin merge) queda para `github-flow`. No hay más features
  del plan del MVP; el plan sigue **cerrado**.
### Sesión 077

- Fecha: 2026-10-12
- Objetivo: implementar `cooking-mode` (Modo cocina: pasos a pantalla completa con temporizadores,
  ampliación del MVP issue #76) segun `docs/specs/cooking-mode.md`, en la rama `feature/cooking-mode`
  (worktree); solo hasta implementación y autoverificación (`passing`), sin commit/push/PR (los hace el
  orquestador). Ejecutada por el subagente `implementer` (`feature-implementer`).
- Completado:
  - **Gate de UI satisfecho**: las capturas obligatorias existen en
    `docs/design/concepts/cooking-mode/modo-cocina/` (`screen.png`, `code.html`, `DESIGN.md`) mas la
    petición (`README.md`) y el prompt (`stitch-prompt.md`); la implementación sigue esa dirección
    (Stitch no generó una variante por estado del temporizador: los cuatro se derivan de la misma
    captura).
  - Lógica **pura** en `com.sazon.app.ui.cooking` (sin Compose ni Android; testeable en JVM):
    `CookingTimerState` (enum `CookingTimerStatus` + máquina `of/start/pause/resume/reset/tick`: el
    tiempo solo entra por `tick(deltaMillis)`, descuenta únicamente En marcha, acota a cero y pasa a
    Terminado sin seguir contando; transiciones inválidas y `delta <= 0` son **no-op**; `of(minutes)`
    con `minutes <= 0` deja `hasDuration = false` y la UI oculta el bloque); `CookingModeState`
    (`initial`, `next`/`previous` que acotan a `0..totalSteps-1` y **reinician** el temporizador al
    paso nuevo, `toggleCompleted` por `RecipeStep.order` **independiente** del índice y del
    temporizador, `withTimer`; **no** guarda la `Recipe` y usa `getOrNull` para no lanzar);
    `CookingModeFormat` (`progressLabel` "Paso 2 de 5", `progressFraction` acotada (0f sin pasos),
    `durationLabel` "10 min", `countdown` mm:ss en `Locale.ROOT` con clamp a 0 y `FINISHED_NOTICE`
    "¡Listo!").
  - Capa Compose **delgada** `CookingModeScreen`: `Scaffold` (fondo crema + barra inferior blanca),
    cabecera con "Atrás" circular, "Progreso"/"Paso X de N" en cifras tabulares y barra fina terracota;
    título de la receta (una línea, elipsis) y tarjeta del paso con círculo numerado terracota y
    tachado/atenuado `0.45` si está hecho; bloque del temporizador solo si `hasDuration`, con acento
    miel (`tertiaryContainer`/`tertiary`): píldora `durationLabel`, cuenta atrás `countdown` en cifras
    tabulares, y los cuatro estados (Inactivo "Iniciar" · En marcha "Pausar" con pulso sutil · Pausado
    "Reanudar" + "Reiniciar" · Terminado "00:00" + aviso **"¡Listo!"** miel/oliva, **nunca** `error`, +
    "Reiniciar"); fila "Marcar paso como hecho"; barra inferior `OutlinedButton` "Anterior"
    (deshabilitado en el primero) + primario terracota "Siguiente" (→ "Terminar" en el último);
    `CookingModeStateSaver` (`rememberSaveable`) y un `LaunchedEffect(state.timer.status)` que solo
    alimenta `tick(1000)` (el reloj real queda fuera de la lógica pura). Guarda defensiva para receta
    sin pasos (mensaje + barra "Atrás"/"Terminar").
  - Integración: `MainShell` gana `var cookingRecipe by remember` (segundo overlay **encima** del
    Detalle, sin desmontarlo) y cablea `onStartCooking = { cookingRecipe = recipe }`; "Atrás"/"Terminar"
    vuelven al Detalle intacto. **Sin** cambios en la firma de `RecipeDetailScreen`.
- Verificación ejecutada: `bash init.sh` -> **exit 0** (`./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`, sin procesos largos); tirada focal
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.cooking.*"` -> `BUILD SUCCESSFUL`
  (3 suites / 29 tests / 0 fallos); parseo propio de los XML de resultados
  (`app/build/test-results/testDebugUnitTest/TEST-*.xml`) y del informe `lint-results-debug.xml`;
  inspección de importaciones de la lógica pura y de `git status`.
- Evidencia capturada: **62 suites / 434 tests / 0 fallos / 0 errores / 0 saltados** (59 suites / 405
  tests previos + **29 nuevos**: `CookingTimerStateTest` 11, `CookingModeStateTest` 10,
  `CookingModeFormatTest` 8); lint **0 errores** y 13 warnings preexistentes (GradleDependency 7,
  NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), **0 con localización bajo
  `app/src`**; `init.sh` con código 0. `CookingTimerState`/`CookingModeState`/`CookingModeFormat` solo
  importan `domain.recipe`/`java.util.Locale`; `git status` limitado a los ficheros previstos
  (`ui/cooking/**` nuevos, `MainShell.kt` modificado, docs).
- Ficheros creados/cambiados:
  `app/src/main/java/com/sazon/app/ui/cooking/{CookingTimerState,CookingModeState,CookingModeFormat,
  CookingModeScreen}.kt` y
  `app/src/test/java/com/sazon/app/ui/cooking/{CookingTimerStateTest,CookingModeStateTest,
  CookingModeFormatTest}.kt` (nuevos), `app/src/main/java/com/sazon/app/ui/MainShell.kt` (integracion),
  `docs/technical-discovery.md`, `DESIGN.md`, `docs/build-brief.md`, `feature_list.json`
  (`cooking-mode` a `passing` con la evidencia) y este registro.
- Hallazgos relevantes: la spec fija la lógica pura y el reparto con la capa Compose, así que la
  implementación no necesitó desviarse; el único ajuste fue derivar los cuatro estados del temporizador
  de la captura única (Stitch no generó variantes por estado) y traducir a microcopy de app los textos
  de la matriz de estados, que son notas de diseño; el pulso de "En marcha" se implementó con
  `rememberInfiniteTransition` (dirección de la captura, detalle visual). Nota para el validador: el
  `<` del botón "Anterior" y el zócalo del detalle no se tocan; la tarjeta del paso no reproduce el
  rótulo decorativo "Técnica principal" de la captura porque `RecipeStep` no tiene ese dato (solo
  `order`/`title`/`description`/`minutes`), y la spec define la tarjeta como círculo + título + cuerpo.
- Riesgo o incógnita abierta: se mantienen los `Unknown` de la spec (rotar con el modo abierto lo
  cierra, como el Detalle; `delay(1000)` acumula deriva bajo carga; la etiqueta fina de los controles
  del temporizador en Terminado queda como detalle visual). Queda la **verificación manual** de la
  persona en el emulador (abrir el modo desde "Empezar a cocinar", recorrer los pasos, temporizador
  Iniciar/Pausar/Reanudar/Reiniciar, aviso "¡Listo!" y acabado claro/oscuro contra
  `docs/design/concepts/cooking-mode/modo-cocina/`); no es automatizable desde WSL y un defecto visual
  se corrige con feature correctiva. Se mantiene el backfill de capturas de UI de la sesión 046.
- Siguiente paso: **validación independiente** de `cooking-mode` con `feature-validator`; después,
  commit local de la rama `feature/cooking-mode` (lo hace el orquestador de `feature-flow`) y
  publicación (push + PR, sin merge) por `github-flow`.
- Nota de integración: la sesión 077 la ejecutaron en paralelo dos ramas (una por feature); se conservan las **dos** entradas.
- Objetivo: implementar y autoverificar `recipe-artwork` (placeholder de receta por categoría y cocina,
  issue #77, `area: ui`) según `docs/specs/recipe-artwork.md`, en la rama `feature/recipe-artwork`
  (worktree); solo hasta implementación y autoverificación (`passing`), sin commit/push/PR (los hace el
  orquestador).
- Completado: paquete nuevo `com.sazon.app.ui.artwork` con **mapeo puro** y **un** componente Compose.
  `RecipeArtworkStyle` (sin Compose/Android, solo `domain.recipe.Recipe`) fija `RecipeGlyph` (18
  miembros ↔ 18 `recipeCategories` reales, `PLATE` = `other` **y** fallback), `RecipeAccent` (10
  acentos ↔ 10 `cuisines` + `DEFAULT`), `RecipeStyle` y `glyphFor`/`accentFor`/`of(recipe)` con
  **fallback total** (`null`/vacío/desconocido → `PLATE` + `DEFAULT`; nunca vacío ni excepción).
  `RecipeArtwork(recipe, modifier, glyphSize = 48.dp)` pinta el bloque con `Brush.linearGradient` de
  **roles del tema** (`primaryContainer`/`secondaryContainer`/`tertiaryContainer`/`surfaceVariant`) y
  un `RecipeGlyphCanvas` privado con los 18 glifos de línea (trazo 2 dp, coordenadas normalizadas,
  `PLATE` = los dos círculos concéntricos de siempre). `RecipeArtworkDefaults.GlyphSize` es el 48 dp
  de la tarjeta. Se sustituyen las **4 copias** y se eliminan los `PlateGlyph` privados:
  `RecipeCard` (16:10, 48 dp), `ResultRecipeCard` (16:10, 56), `FavoriteRecipeCard` (miniatura 96x96
  con `clip(SazonShapes.small)` y bloque de la rejilla de alto 96, 40; se **conservan**
  `HeartGlyph`/`HeartButton`/`DismissBackground` y el corazón superpuesto) y `DetailHero` (240 dp,
  72 dp; `DetailHero` ahora recibe `recipe` y "Atrás"/corazón siguen siendo hermanos). Se limpian los
  imports huérfanos (`Canvas`/`Brush`/`Color`/`Stroke`/`Alignment`/`size`/`Sazon*Container` según el
  fichero). Sin dependencias nuevas, sin assets y sin hex sueltos. `RecipeArtworkStyleTest` itera la
  **Taxonomía real**.
- Verificación ejecutada: tirada focal
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.artwork.*"` (BUILD SUCCESSFUL) y
  gate estándar `bash init.sh` (exit 0, `./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`, sin
  procesos largos); parseo propio de los XML de `app/build/test-results/testDebugUnitTest/` y del
  informe `app/build/reports/lint-results-debug.xml`; inspección de pureza, de ausencia de hex y de
  `git diff --stat`.
- Evidencia capturada: **60 suites / 413 tests / 0 fallos / 0 errores / 0 saltados** (405 previos + 8
  nuevos de `RecipeArtworkStyleTest`: `cada_categoria_real_de_la_taxonomia_tiene_su_glifo_sin_duplicados`,
  `cada_cocina_real_de_la_taxonomia_tiene_su_acento_sin_duplicados`, `el_mapeo_es_determinista`,
  `un_id_nulo_vacio_o_desconocido_cae_al_fallback_de_plato_y_degradado_por_defecto`,
  `el_fallback_reproduce_el_estilo_de_la_categoria_other`,
  `un_id_desconocido_no_devuelve_el_estilo_de_ninguna_categoria_o_cocina_concreta`,
  `el_estilo_de_una_receta_sale_de_su_categoria_y_su_cocina`,
  `una_receta_sin_clasificar_no_lanza_y_usa_el_fallback`). El test lee
  `docs/domain/taxonomy.json` por `System.getProperty("taxonomy.asset.path")` (mismo patrón que
  `TaxonomyIntegrityTest`) y exige 18 categorías → 18 glifos distintos (`== RecipeGlyph.entries`) y 10
  cocinas → 10 acentos distintos (`+ DEFAULT == RecipeAccent.entries`), determinismo, fallback total y
  caso negativo (un id desconocido no comparte glifo con ninguna categoría concreta salvo `other` —el
  propio fallback— ni acento con ninguna cocina real). Lint **0 errores y 13 warnings preexistentes**
  (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), **0 bajo
  `app/src`**. `grep`: **0 apariciones de `PlateGlyph`** en `app/src`; el degradado del placeholder
  (`primaryContainer` → `secondaryContainer`) solo vive en `RecipeArtwork.kt`. `RecipeArtworkStyle.kt`
  sin Compose/Android; `ui/artwork` sin hex sueltos; geometría y tamaños intactos (16:10, 96x96, alto
  96, hero 240; 48/56/40/40/72 dp); `git diff --stat` solo con los ficheros previstos.
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/ui/artwork/{RecipeArtworkStyle,RecipeArtwork}.kt`
  y `app/src/test/java/com/sazon/app/ui/artwork/RecipeArtworkStyleTest.kt` (nuevos);
  `app/src/main/java/com/sazon/app/ui/{home/RecipeCard.kt, results/ResultRecipeCard.kt,
  favorites/FavoriteRecipeCard.kt, detail/RecipeDetailScreen.kt}` (modificados); `DESIGN.md`,
  `docs/technical-discovery.md`, `docs/specs/recipe-artwork.md` (finding del grep), `feature_list.json`
  y este registro. **No** se toca `app/build.gradle.kts`, `gradle/libs.versions.toml`, `domain/**`,
  `data/**`, la Taxonomía, `seed-recipes.json`, `ui.theme/**`, `MainShell.kt`, `SazonApp.kt`,
  `MainActivity.kt` ni `init.sh`; **no** se crea `docs/design/concepts/recipe-artwork/` (desviación
  autorizada en el issue #77).
- Hallazgos relevantes: (1) Kotlin **anida** los comentarios de bloque, así que `assets/img/recipes/**`
  dentro de un KDoc abre un comentario anidado y rompe la compilación ("Unclosed comment"): se escribió
  sin el `**`. (2) `Recipe.categoryId`/`cuisineId` **no son anulables**, así que el caso "sin
  clasificar" real que cubre el test es un id **vacío o desconocido** (el mapeo puro sí acepta `null`
  por si un futuro campo se anula). (3) El "grep de `Brush.linearGradient` solo en `RecipeArtwork.kt`"
  de la spec es impreciso: `HomeScreen` ya usaba `Brush.linearGradient` (hero `primary` →
  `inversePrimary`, ajeno a esta feature); el degradado **del placeholder** sí queda solo en
  `RecipeArtwork.kt`. Anotado en la spec.
- Riesgo o incógnita abierta: se mantienen los `Unknown` de la spec (fidelidad fina de los 18 glifos
  line-art y contraste del glifo `onSurfaceVariant` sobre los 11 degradados). Queda para revisión manual
  de la persona en el emulador (Home, Resultados, Favoritos lista+rejilla y Detalle, en claro y oscuro)
  el acabado contra el registro de `DESIGN.md`; un defecto visual se corrige con una feature correctiva.
  No se ha hecho commit ni push. Se mantiene el backfill de capturas de UI de las features previas
  (sesión 046).
- Siguiente paso: **validación independiente** de `recipe-artwork` con `feature-validator`; después,
  commit local de la rama (lo hace el orquestador de `feature-flow`) y publicación por `github-flow`.

### Sesión 078

- Fecha: 2026-10-12
- Objetivo: validar `cooking-mode` de forma independiente y persistir la aceptación. Ejecutada por el
  orquestador de `feature-flow` en la rama `feature/cooking-mode` (worktree), solo hasta-aceptación;
  **sin push ni PR** (publica `github-flow`).
- Completado: veredicto **`accept`** de un agente con contexto limpio (rol `validator`); `feature_list.json`
  pasa a `accepted` con evidencia que nombra la validación independiente. Sin cambios de código tras la
  validación. Commit local `feat: complete cooking-mode` (lo hace el orquestador).
- Verificación ejecutada: el validador re-ejecutó `bash init.sh` (exit 0; test/lint `UP-TO-DATE` en la
  primera pasada) y forzó `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (30/30 tareas) y
  `bash scripts/gradle.sh lint --rerun-tasks` (30/30 tareas); parseó por su cuenta los XML de resultados
  y el informe `lint-results-debug.xml`, e inspeccionó el código y las capturas.
- Evidencia capturada: **62 suites / 434 tests / 0 fallos / 0 errores / 0 saltados** (29 en `ui.cooking`:
  `CookingTimerStateTest` 11, `CookingModeStateTest` 10, `CookingModeFormatTest` 8); lint **0 errores** y
  13 warnings preexistentes, **0 bajo `app/src`**. El validador confirmó los Scenarios 1-7 contra el
  artefacto (overlay de `MainShell` cableando `onStartCooking`; `next`/`previous` con reinicio del
  temporizador y no-op en los extremos; cuenta atrás determinista por `tick(1000)` hasta `FINISHED`/`0` y
  aviso "¡Listo!" en `secondaryContainer` nunca `error`; pausa congela y reanudar continúa; marcado
  idempotente e independiente con tachado + `alpha 0.45`; `hasDuration` oculta el bloque; receta sin
  pasos sin crash), la pureza de `ui.cooking`, el gate de capturas satisfecho (mtime 22:34 anterior a los
  fuentes) y la disciplina de alcance (build/lockfile, dominio, `data/**`, Taxonomía, `seed-recipes.json`,
  `ui/detail/**`, `MainNavigationState.kt`, `ServingsStepper.kt`, Room/Perfil, `scripts/**` e `init.sh`
  intactos; sin dependencias nuevas, sin iconos ni permisos). Sin hallazgos Critical/High/Medium; 3 Low
  informativos no bloqueantes (el título de la receta no aparece en la guarda defensiva de la receta sin
  pasos —ruta prácticamente inalcanzable—; el export etiqueta el estado Terminado también como
  "Temporizador completado", pulido visual; `Implementation Tasks`/`Validator Checklist` de la spec sin
  marcar). Sin hallazgos de seguridad.
- Ficheros actualizados: `feature_list.json` (`cooking-mode` a `accepted`) y este registro.
- Riesgo o incógnita abierta: ninguno nuevo. Queda la **verificación manual** de la persona en el
  emulador (abrir el modo desde "Empezar a cocinar", recorrer los pasos, temporizador
  Iniciar/Pausar/Reanudar/Reiniciar, aviso "¡Listo!" y acabado claro/oscuro contra
  `docs/design/concepts/cooking-mode/modo-cocina/`); un defecto visual se corrige con feature correctiva.
  Se mantiene el backfill de capturas de UI de la sesión 046.
- Siguiente paso: commit local de la rama `feature/cooking-mode` (lo hace el orquestador de
  `feature-flow`); la **publicación** (push + PR contra `main`, sin merge) queda para `github-flow`. No
  hay más features del plan del MVP; el plan sigue **cerrado**.
- Nota de integración: la sesión 078 corrió en paralelo para dos features; se conservan las **dos** entradas de aceptación.
- Objetivo: validar `recipe-artwork` de forma independiente y persistir la aceptación. Ejecutada por el
  orquestador de `feature-flow` en la rama `feature/recipe-artwork` (worktree), solo hasta-aceptación;
  **sin push ni PR** (publica `github-flow`).
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la validación independiente; creado `docs/validations/recipe-artwork.md`.
  Sin cambios de código tras la validación.
- Verificación ejecutada: el validador re-ejecutó `bash init.sh` (exit 0) y, al verlo `UP-TO-DATE`, forzó
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (34/34 tareas) y
  `bash scripts/gradle.sh lint --rerun-tasks` (30/30 tareas); parseó por su cuenta los XML de resultados y
  el informe `lint-results-debug.xml`, e inspeccionó el código y el diff.
- Evidencia capturada: **60 suites / 413 tests / 0 fallos / 0 errores / 0 saltados**
  (`RecipeArtworkStyleTest` 8; 405 preexistentes en verde) y lint **0 errores, 13 warnings** preexistentes
  (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), 0 bajo
  `app/src`. El validador **recomputó** el mapeo contra la Taxonomía real: 18 `recipeCategories` → 18
  `RecipeGlyph` distintos (`== RecipeGlyph.entries`) y 10 `cuisines` → 10 `RecipeAccent` distintos
  (+`DEFAULT` `== RecipeAccent.entries`), `else`/`other` → `PLATE`/`DEFAULT`; confirmó la pureza de
  `ui.artwork` (sin Compose/Android), la ausencia de hex sueltos, **0 `PlateGlyph`** en `app/src`, el
  degradado del placeholder solo en `RecipeArtwork.kt`, los 5 puntos de llamada con geometría/tamaños
  preservados (16:10, 96x96, alto 96, hero 240; glifos 48/56/40/40/72 dp), que `DESIGN.md` y
  `docs/technical-discovery.md` quedan actualizados, la exención de capturas (issue #77) tratada como
  autorizada, la disciplina de alcance y que el implementer no hizo commit/push/PR.
- Ficheros actualizados por el orquestador: `feature_list.json` (`recipe-artwork` a `accepted` +
  evidencia), `docs/validations/recipe-artwork.md` (nuevo) y este registro. Commit local de la rama
  `feature/recipe-artwork`; la publicación queda para `github-flow`.
- Hallazgos del validador (2 Low informativos, no bloqueantes): la redacción "vertical" del degradado
  cuando `Brush.linearGradient` es diagonal (idéntico al placeholder aceptado antes, sin regresión);
  Scenario 6 (roles del tema por claro/oscuro) es solo por inspección, coherente con el Verification Plan
  y con AGENTS.md (no se automatiza UI de emulador). Sin hallazgos Critical/High/Medium.
- Riesgo o incógnita abierta: se mantienen los `Unknown` de la spec (fidelidad fina de los 18 glifos
  line-art y contraste del glifo `onSurfaceVariant` sobre los 11 degradados). Queda para revisión manual de
  la persona en el emulador (Home, Resultados, Favoritos lista+rejilla y Detalle, en claro y oscuro) el
  acabado contra el registro de `DESIGN.md`; un defecto visual se corrige con una feature correctiva. Se
  mantiene el backfill de capturas de UI de las features previas (sesión 046).
- Siguiente paso: commit local de la rama `feature/recipe-artwork` (lo hace el orquestador de
  `feature-flow`); la publicación queda para `github-flow`. Después, `recipe-artwork` era la única feature
  de este plan; el plan del MVP sigue cerrado y no hay más features `not_started`.

### Sesión 079

- Fecha: 2026-10-12
- Objetivo: implementar `ai-verbose-diagnostics` (correctiva de `ai-call-diagnostics`) en el worktree de
  la rama `feature/ai-verbose-diagnostics`: (A+B) desglosar `INCOHERENT` en sus dos causas del Validador
  y (D) modo de depuración (interruptor, detalle de descartes con título y JSON crudo troceado). Solo
  hasta implementación y autoverificación (`passing`), sin commit/push/PR.
- Completado:
  - **Desglose siempre activo**: `AiIncoherentCause { TIME_DRIFT, ALLERGEN_COVERAGE }` (diagnóstico en
    `data.ai`, **no** se toca `DiscardReason`) y `AiCallEvent.incoherentByCause`; `AiRecipePayload.decode`
    clasifica cada descarte `INCOHERENT` recomputando **solo** la regla 4a con
    `RecipeValidator.MAX_STEP_TOTAL_DRIFT_MINUTES` (si no es 4a es 4b: el Validador devuelve el primer
    fallo y evalúa 4a antes que 4b).
  - **Modo depuración**: `DevExpertRecipeGenerator` gana `verboseDiagnostics: Boolean = false`;
    `decode(verbose)` rellena `AiDecodeResult.discards` (`AiDiscardDetail`: motivo, causa y título del
    draft o del primitivo `title` del JSON bruto) solo en verbose; el generador añade
    `rawContent = outcome.content` solo en verbose. `MainActivity` usa
    `verboseDiagnostics = BuildConfig.DEBUG` y añade `incoherent={…}` (solo si hay causas) más las líneas
    verbosas; el modo lo decide un único sitio (el generador) y el log es renderizador.
  - **Helper puro** `AiVerboseDiagnostics`: `MAX_CHUNK_CHARS = 3_000`, `rawLines` (trocea sin cortar
    pares suplentes; `null`/vacío → vacío) y `discardLines` (motivo/causa/título saneado; `(sin titulo)`).
- Verificación ejecutada: tirada focal
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"` (BUILD SUCCESSFUL) y gate
  estándar `bash init.sh` (exit 0, test + lint vía `scripts/gradle.sh`, sin procesos largos); parseo
  propio de los XML y del informe de lint.
- Evidencia capturada: **76 tests / 0 fallos en `data.ai`** (62 previos + 14 nuevos: `AiRecipePayloadTest`
  14, `AiVerboseDiagnosticsTest` 7, `DevExpertRecipeGeneratorTest` 35, más los intactos `AiProviderPolicyTest`
  12, `AiRecipeIdsTest` 6 y `OpenAiChatTest` 2); gate **60 suites / 419 tests / 0 fallos / 0 errores / 0
  saltados** (405 previos + 14 nuevos); lint **0 errores y 13 warnings preexistentes** (GradleDependency 7,
  NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), **0 bajo `app/src`**.
  Scenario 1: lote con una 4a y una 4b → `discardedByReason {INCOHERENT: 2}` e `incoherentByCause
  {TIME_DRIFT=1, ALLERGEN_COVERAGE=1}` en orden del enum, sin títulos. Scenario 2: coherencia con
  `RecipeValidator.validateRecipe` (solo 4a → TIME_DRIFT; solo 4b → ALLERGEN_COVERAGE; ambas → TIME_DRIFT;
  ninguna → `Accepted` con el desglose vacío). Scenario 3: por defecto `discards` vacío, `rawContent` null
  y `toString()` sin títulos ni cuerpo. Scenario 4: con el interruptor activo y un 200 de ~14 KB,
  `rawContent == content` del `MockEngine`, `discards` con título/causa y el render reconstruye el crudo
  por concatenación. Scenario 5: ningún evento ni línea lleva `test-key`/`Bearer`/`Authorization` en
  ningún modo.
- Ficheros cambiados: `app/src/main/java/com/sazon/app/data/ai/{AiDiagnostics,AiDecodeResult,
  AiRecipePayload,DevExpertRecipeGenerator}.kt`, `app/src/main/java/com/sazon/app/data/ai/AiVerboseDiagnostics.kt`
  (nuevo), `app/src/main/java/com/sazon/app/MainActivity.kt`, `app/src/test/java/com/sazon/app/data/ai/
  {AiRecipePayloadTest,DevExpertRecipeGeneratorTest}.kt`, `app/src/test/java/com/sazon/app/data/ai/
  AiVerboseDiagnosticsTest.kt` (nuevo), `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`,
  `docs/adr/0003-ai-provider.md`, `feature_list.json` y este registro. Sin tocar `Recipe.kt`,
  `RecipeValidator.kt` (`DiscardReason`/`RecipeValidation`/`DiscardedRecipe` intactos), `AiRecipeDraft.kt`,
  `AiRecipeIds.kt`, `AiProviderPolicy.kt`, `AiHttpClientFactory.kt`, `OpenAiChat.kt`, `domain/**`, `ui/**`,
  `data/generator/**`, Room, Taxonomía, `seed-recipes.json`, `app/build.gradle.kts`,
  `gradle/libs.versions.toml`, `AndroidManifest.xml`, `init.sh`, `scripts/**`, `DESIGN.md` ni
  `docs/design/**`; sin dependencias nuevas.
- Hallazgos relevantes: la primera pasada de la batería de coherencia falló por un error del propio test
  (decodificaba una receta suelta en vez del envelope `{recipes:[…]}`); corregido. El desglose **no** lleva
  texto de recetas por sí mismo (el título solo viaja con el interruptor). `incoherentByCause` es el único
  campo nuevo siempre presente en el evento (vacío si no hay `INCOHERENT`), así que con un lote sin
  `INCOHERENT` la línea `SazonAi` es la actual (interpretación de «idéntico al actual» de la spec).
- Riesgo o incógnita abierta: se mantienen los `Unknown` de la spec (duplicación mínima de la 4a, mitigada
  reutilizando la constante y con el test de coherencia; volumen de log en debug, ~5 líneas de ~3 KB por
  llamada con posible rate-limit de Logcat; `rawContent` puede ser `null`/vacío en un 200 ilegible). Queda
  la verificación **manual** de emulador de la persona (logcat con `incoherent={…}` y, en debug,
  `discard[i/n]`/`raw[i/n]`; release → formato anterior; clave ausente → modo semilla), no automatizable
  desde WSL. El modo verboso queda trazado como **deuda temporal** (debug-only) en
  `docs/risks-and-open-questions.md` y con nota en `docs/adr/0003-ai-provider.md`.
- Siguiente paso: **validación independiente** de `ai-verbose-diagnostics` con `feature-validator`;
  después, commit local de la rama (lo hace el orquestador de `feature-flow`) y publicación por
  `github-flow`. Se mantiene el backfill de capturas de UI (sesión 046).

### Sesión 080

- Fecha: 2026-10-12
- Objetivo: validar `ai-verbose-diagnostics` de forma independiente y persistir la aceptación. Ejecutada
  por el orquestador de `feature-flow` en la rama `feature/ai-verbose-diagnostics` (worktree), solo
  hasta-aceptación; sin push ni PR (publica `github-flow`).
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con la evidencia del validador independiente y se crea
  `docs/validations/ai-verbose-diagnostics.md`. Sin cambios de código tras la validación.
- Verificación ejecutada: el validador re-ejecutó `bash init.sh` (exit 0; test + lint vía `scripts/gradle.sh`)
  y, al ver la primera pasada en `UP-TO-DATE`, forzó `bash scripts/gradle.sh testDebugUnitTest --tests
  "com.sazon.app.data.ai.*" --rerun-tasks` (34/34 tareas) y `bash scripts/gradle.sh lint --rerun-tasks`
  (30/30 tareas); parseó por su cuenta los XML de resultados y el informe de lint.
- Evidencia capturada: gate **60 suites / 419 tests / 0 fallos / 0 errores / 0 saltados** (405 previos + 14
  nuevos); rerun focal fresco del paquete `data.ai`: **76/76** (`AiRecipePayloadTest` 14,
  `AiVerboseDiagnosticsTest` 7, `DevExpertRecipeGeneratorTest` 35, más `AiProviderPolicyTest` 12,
  `AiRecipeIdsTest` 6 y `OpenAiChatTest` 2); lint **0 errores y 13 warnings preexistentes**, 0 bajo
  `app/src`. Confirmó los Scenarios 1-6 (desglose 4a/4b y su coherencia con `RecipeValidator.validateRecipe`;
  con el interruptor apagado sin detalle ni crudo y la línea `SazonAi` byte a byte con un lote sin
  `INCOHERENT`; con el interruptor activo `rawContent == content` del `MockEngine` y render reconstruible;
  no-fuga de `test-key`/`Bearer`/`Authorization` en ambos modos; regresión: los 62 tests previos de `data.ai`
  intactos). Scenario 7 (manual en emulador) pendiente por diseño. Alcance confirmado
  (`RecipeValidator.kt`/`DiscardReason`, `Recipe.kt`, `AiProviderPolicy.kt`, `AiHttpClientFactory.kt`,
  `OpenAiChat.kt`, `domain/**`, `ui/**`, Room, assets, build, `init.sh`, `scripts/**`, `DESIGN.md` y
  `docs/design/**` intactos; sin dependencias nuevas; sin capturas, no es UI).
- Hallazgos del validador (3 Low informativos, ninguno bloqueante): la igualdad byte a byte del prefijo
  `incoherent=` vive en `MainActivity` sin test JVM (el helper puro sí está probado);
  `AiVerboseDiagnostics.chunks` con `maxChars == 1` podría partir un par suplente (no alcanzable en
  producción, que usa 3000); el registro de validación aún no existía (persistido en esta sesión). Sin
  hallazgos Critical/High/Medium.
- Ficheros actualizados: `feature_list.json` (feature a `accepted` con la evidencia del validador),
  `docs/validations/ai-verbose-diagnostics.md` (nuevo) y este registro. Sin cambios de código.
- Riesgo o incógnita abierta: se mantienen los `Unknown` de la spec (duplicación mínima de la 4a mitigada
  con la constante y el test de coherencia; volumen de log en debug, ~5 líneas de ~3 KB por llamada con
  posible rate-limit de Logcat; `rawContent` puede ser null/vacío en un 200 ilegible). Queda la verificación
  **manual** de emulador de la persona (logcat con `incoherent={…}` y, en debug, `discard[i/n]`/`raw[i/n]`;
  release → formato anterior; clave ausente → modo semilla), no automatizable desde WSL. El modo verboso
  queda trazado como deuda temporal (debug-only) en `docs/risks-and-open-questions.md` y con nota en
  `docs/adr/0003-ai-provider.md`. Se mantiene el backfill de capturas de UI (sesión 046).
- Siguiente paso: **commit local** de la rama `feature/ai-verbose-diagnostics` (lo hace el orquestador de
  `feature-flow`); la publicación (push + PR, sin merge) queda para `github-flow`. No hay más features del
  plan del MVP; el plan sigue **cerrado**.

### Sesión 081

- Fecha: 2026-10-13
- Objetivo: implementar `favorites-sort-controls` (correctiva de `favorites`) en el worktree de la rama
  `feature/favorites-sort-controls`: acortar el chip de orden por defecto a "Recientes" (dejaba de
  recortarse) y sustituir los dos chips de texto "Lista"/"Rejilla" por el toggle icónico de la captura
  aceptada. Solo hasta implementación y autoverificación (`passing`), sin commit/push/PR.
- Completado:
  - **Microcopy** (`FavoritesSort.kt`): `RECENT("Añadidas recientemente", "recent")` →
    `RECENT("Recientes", "recent")`; solo el literal. `key`, entries, `fromKey` (desconocido → `RECENT`)
    y la semántica de orden (`savedAt` desc) intactos, así que `FavoritesSortSaver` y los tests siguen
    valiendo.
  - **Toggle icónico** (`FavoritesScreen.kt`): se elimina `ViewModeButton` (dos `FilterChip` de texto) y
    nace `ViewModeToggle` — `Surface(shape = SazonPillShape, color = surface, shadowElevation = 2.dp)`
    con relleno interior de 2dp y dos `ViewModeIconButton` circulares de 28dp
    (`size(28.dp).clip(CircleShape).background(...)`) seleccionables. Activo = `primary`/`onPrimary`;
    inactivo = sin relleno (`Color.Transparent`) con `onSurfaceVariant`. Accesible:
    `Row(selectableGroup())` + `selectable(role = Role.RadioButton)` + `semantics { contentDescription }`
    ("Lista"/"Rejilla"). Glifos privados `ListGlyph` (rectángulo redondeado + línea a 1/3, `view_agenda`)
    y `GridGlyph` (cuatro cuadrados redondeados 2×2, `grid_view`) dibujados con `Canvas`, patrón de
    `CloudOffGlyph`/`BookmarkGlyph`; el repo no tiene `material-icons-*` y no se añaden dependencias.
    `FavoritesSortRow` conserva el `LazyRow(Modifier.weight(1f))` con los tres `FilterChip` de orden.
  - **`DESIGN.md`**: registra el microcopy vigente "Recientes" (Core Screen 6 + bullet `FilterChip`), el
    toggle lista/rejilla icónico (28dp, activo terracota) y la **excepción de capturas** autorizada de
    `favorites-sort-controls` (issue #85; no se crea `docs/design/concepts/favorites-sort-controls/`),
    en la misma línea que la de `recipe-artwork` (#77).
- Verificación ejecutada: `bash init.sh` en WSL (test + lint vía `scripts/gradle.sh`, sin procesos largos)
  con **exit 0**; parseo propio de los XML de resultados y del informe de lint; greps de alcance.
- Evidencia capturada: gate **64 suites / 456 tests / 0 fallos / 0 errores / 0 saltados**
  (`FavoritesListTest` 9, `FavoriteServiceTest` 6, `RoomFavoriteRepositoryTest` 9 siguen verdes porque
  usan enums/keys, no `.label`); lint **0 errores y 13 warnings preexistentes** (GradleDependency 7,
  NewerVersionAvailable 3, AndroidGradlePluginVersion 2, OldTargetApi 1), **0** en
  `app/src/main/java/com/sazon/app/ui/favorites`. Greps: **0** referencias a `ViewModeButton`, **0**
  literales de texto "Lista"/"Rejilla" como etiqueta de botón (los únicos literales son las
  `contentDescription` exigidas por el Scenario 6) y **0** hex sueltos en `ui.favorites`. `git diff
  --name-only` solo lista `DESIGN.md`, `FavoritesScreen.kt` y `FavoritesSort.kt`; `app/build.gradle.kts`,
  `gradle/libs.versions.toml` e `init.sh` intactos, y sin tocar dominio, persistencia, `ui.theme`,
  `FavoriteRecipeCard.kt`, `FavoritesList.kt`, `FavoritesViewMode.kt`, `MainShell`/`SazonApp`/
  `MainActivity` ni las capturas `docs/design/concepts/favorites/**`.
- Ficheros cambiados: `app/src/main/java/com/sazon/app/ui/favorites/FavoritesSort.kt`,
  `app/src/main/java/com/sazon/app/ui/favorites/FavoritesScreen.kt`, `DESIGN.md`, `feature_list.json` y
  este registro.
- Hallazgos relevantes: el export de la captura aceptada todavía rotula el chip como "Añadidas
  recientemente"; se deja intacto a propósito (es el export histórico) y el microcopy vigente queda
  registrado en `DESIGN.md`. La `contentDescription` "Lista"/"Rejilla" es la única aparición de esos
  literales en el código y es **obligatoria** (Scenario 6): no es texto de botón.
- Riesgo o incógnita abierta: se mantienen los `Unknown` de la spec (recorte con tipografía muy grande;
  objetivo táctil de 28dp por debajo de los 48dp de `DESIGN.md`, igual que los `FilterChip` de la fila;
  fidelidad fina de los glifos, que no tiene test). La verificación **manual** en emulador (chip
  "Recientes" entero y toggle icónico en claro/oscuro) la hace la persona tras el merge; AGENTS.md no
  automatiza UI de emulador y un defecto visual se corrige con una feature correctiva.
- Siguiente paso: **validación independiente** de `favorites-sort-controls` con `feature-validator`;
  después, commit local de la rama (orquestador de `feature-flow`) y publicación por `github-flow`.
- Nota de integración: la sesión 081 la ejecutaron en paralelo tres ramas (una por feature,
  `favorites-sort-controls`, `recipe-photos` y `ai-quantity-tolerance`); se conservan las **tres**
  entradas.
- Objetivo: implementar y **autoverificar** `recipe-photos` (issue #83, `area: ui`) en su worktree/rama
  `feature/recipe-photos`, siguiendo `docs/specs/recipe-photos.md`; sin commit/push/PR (los hace el
  orquestador de `github-flow` al `accept`).
- Completado: las cinco piezas de la spec. (1) `scripts/fetch-recipe-photos.sh` (bash, idempotente,
  `--force`) extrae los `src` de las láminas versionadas (semillas **por posición** con el comentario
  `<!-- NN. <clave> -->`; categorías por `alt`), descarga con `=s0`, elige extensión por *magic bytes* y
  exige 12/19 claves exactas (exit ≠ 0 si falta algo). (2) `data/RecipePhotoAssets.kt` (cadena **pura**
  `seed → category → default → null`, con `ROOT`/`DEFAULT_KEY`/`EXTENSIONS` y las 18 categorías reales).
  (3) `data/AssetRecipePhotoSource.kt` (`AssetManager` + `BitmapFactory` en `Dispatchers.IO`, LRU 16,
  `null` sin lanzar, devuelve `Bitmap`). (4) `ui/artwork/RecipePhotoSource.kt` (`fun interface` +
  `LocalRecipePhotoSource` con default nulo). (5) `RecipeArtwork` con foto (`produceState` +
  `Image(ContentScale.Crop)`) y el degradado + glifo como último recurso (firma/geometría intactas);
  `MainActivity` provee el seam con `CompositionLocalProvider` y `app/build.gradle.kts` solo gana
  `systemProperty("recipe.photos.assets.dir", …)`.
- Evidencia capturada (real): assets **31 ficheros / 6.092.271 B (5.949 KiB) ≤ 8 MB** en
  `app/src/main/assets/img/recipes/` (12 `seed/`, 18 `category/`, 1 `default.jpg`), versionados
  (`git check-ignore`: no ignorados). Sin `cwebp`/ImageMagick en la máquina → el script **conserva el
  original** y lo registra (paso 4 de la spec); re-ejecución idempotente con **exit 0**
  (`ya empaquetado, se omite` × 31).
- Verificación ejecutada: `bash init.sh` **exit 0** (test: BUILD SUCCESSFUL en 52 s; lint: BUILD
  SUCCESSFUL en 1 m 13 s), parseo propio de los XML de resultado y del informe de lint. Gate **65 suites /
  467 tests / 0 fallos / 0 errores / 0 saltados** (la rama parte de un `origin/main` más nuevo que la
  sesión 080, que medía 419; se añaden los 11 de `RecipePhotoAssetsTest`); `RecipePhotoAssetsTest`
  **11/0/0/0** y `RecipeArtworkStyleTest` **8/0/0/0** (sin cambios). Lint **0 errores / 13 warnings**
  preexistentes (versiones en `gradle/libs.versions.toml`, `gradle-wrapper.properties` y
  `app/build.gradle.kts`), **0 bajo `app/src`**. `bash scripts/gradle.sh assembleDebug` BUILD SUCCESSFUL y
  `unzip -l app-debug.apk` → **31 assets** `assets/img/recipes/**` (5,81 MiB). Grep: `RecipeArtwork` en 4
  ficheros / 5 puntos de llamada, **0 `PlateGlyph`**, **0 `BitmapFactory`/`AssetManager`** fuera de
  `data/AssetRecipePhotoSource.kt`; `RecipeArtworkStyle.kt`, `domain/**`, `gradle/libs.versions.toml`,
  `ui.theme/**`, `docs/domain/taxonomy.json`, `seed-recipes.json`, `init.sh` y `scripts/gradle.sh`
  intactos.
- Docs durables: ADR nuevo `docs/adr/0004-recipe-photos-as-assets.md`; `DESIGN.md` (Components → Imagen
  de receta, registro de las capturas `recipe-photos: fotos-semillas/fotos-categorias` y OQ4 revisada),
  `docs/technical-discovery.md` (fila Iconos e imágenes, párrafo de la imagen de receta, Constraint de
  tamaño del APK y tests JVM nuevos), `docs/build-brief.md` (ampliación del MVP en `Non-Goals`),
  `docs/risks-and-open-questions.md` (licencias/precisión, riesgo de tamaño/decodificación y ADR 0004) y
  `docs/domain/taxonomy.md` §6 (convención real `img/recipes/{seed,category}/…` + `default`).
- Hallazgos / desviaciones menores: el snippet de la spec usaba `exists = { File(assetsDir, it).isFile }`,
  pero `recipe.photos.assets.dir` apunta a `.../assets/img/recipes` mientras los candidatos incluyen la
  raíz `img/recipes`; el test usa un helper `photoFile()` que resta `RecipePhotoAssets.ROOT` (documentado
  en el KDoc del test) y la lógica de resolución no cambia. El script conserva JPEG porque no hay
  herramienta de conversión (permitido por el paso 4 de la spec); migrar a WebP no requiere cambios de
  Kotlin (`EXTENSIONS` los prueba en orden).
- Riesgo o incógnita abierta: se mantienen los `Unknown` de la spec (jank/memoria al hacer scroll con
  muchas tarjetas, mitigado con `Dispatchers.IO` + LRU 16; fidelidad genérica de la foto de categoría para
  recetas de IA; procedencia de las fotos registrada en el ADR 0004). Queda la **verificación manual** de
  emulador de la persona (Home, Resultados, Favoritos lista+rejilla y Detalle, claro y oscuro; receta IA
  → foto de categoría; prueba destructiva opcional → glifo), no automatizable desde WSL.
- Siguiente paso: **validación independiente** de `recipe-photos` con `feature-validator`; después, commit
  local de la rama (lo hace el orquestador de `feature-flow`) y publicación (push + PR, sin merge) por
  `github-flow`.
- Objetivo: implementar `ai-quantity-tolerance` (correctiva de `ai-recipe-decode`): tolerar el
  ingrediente "al gusto" del wire del modelo (`quantity` nula o ausente) en la capa anti-corrupción
  `data.ai` y normalizarlo a `0.0` + `unit = null`, según `docs/specs/ai-quantity-tolerance.md`, en la
  rama `feature/ai-quantity-tolerance` (worktree); solo hasta implementación y autoverificación
  (`passing`), sin commit/push/PR (los hace el orquestador).
- Completado:
  - `AiIngredientDraft` (`@Serializable internal data class`) en `data.ai`: `name` obligatorio y
    `quantity: Double? = null`, `unit: MeasureUnit? = null`, `isPantry = false`, `ingredientId = null`
    (defaults tolerantes), con `AL_GUSTO_QUANTITY: Double = 0.0` como fuente única y
    `AiIngredientDraft.toRecipeIngredient()` con el contrato de normalización: `quantity == null`
    (nula **o** ausente) → "Al gusto" (`quantity = 0.0`, `unit = null`, descartando la `unit` del wire);
    `quantity != null` → mapeo **1:1** intacto.
  - `AiRecipeDraft.ingredients` pasa a `List<AiIngredientDraft>` y `toRecipe(id)` mapea
    `ingredients.map { it.toRecipeIngredient() }`; KDoc actualizado (el dominio exige `quantity: Double`
    no anulable, por eso no se reutiliza `RecipeIngredient` en el wire).
  - `AiRecipeIds.canonical` canonicaliza el ingrediente **ya normalizado**; `PREFIX` y
    `ALGORITHM_VERSION` sin cambios (el par cantidad/unidad de un ingrediente numérico es idéntico al de
    antes, así que **ningún id existente cambia**; las variantes nula/ausente/`0.0`+`null` comparten id).
  - Tests JVM nuevos (Taxonomía real, cuerpos **literales** del contrato §4, sin `RecipeJson.encode`):
    `AiRecipePayloadTest` escenarios 1-7 (`quantity`+`unit` nulas → aceptada y "Al gusto" comprobando
    `IngredientFormat.quantityLabel(0.0, null)`; `unit` no nula con `quantity` nula → unidad descartada;
    campo `quantity` ausente → mismo resultado; `quantity` numérica intacta; lote mixto "al gusto" + rota
    → `{SCHEMA:1}` y la buena aceptada; `quantity` nula con otro campo inválido → sigue descartada por
    `SCHEMA`/`INCOHERENT`; `source`/`id`/`matchScore` de la app) + mapeo directo del DTO, y
    `AiRecipeIdsTest.la_cantidad_nula_la_ausente_y_la_normalizada_comparten_id`; helper `draft(...)`
    adaptado a `List<AiIngredientDraft>`.
  - Hallazgo de spec: la spec pedía `AL_GUSTO_QUANTITY = 0.0D` (sufijo de Java, **inválido** en Kotlin:
    `Unresolved reference 'D'`); implementado como `const val ... : Double = 0.0` y anotado en
    `docs/specs/ai-quantity-tolerance.md`.
- Verificación ejecutada: `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.*"`
  (verde) y `bash init.sh` (exit 0, `./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`, sin
  procesos largos); parseo propio de los XML de resultados y del informe `lint-results-debug.xml`;
  reproducción independiente de la causa raíz con un test temporal (eliminado después); inspección de
  `git diff --stat` y del alcance.
- Evidencia capturada: **64 suites / 465 tests / 0 fallos / 0 errores / 0 saltados** (456 preexistentes
  + 9 nuevos de `data.ai`: `AiRecipePayloadTest` 22, `AiRecipeIdsTest` 7) y lint **0 errores y 13
  warnings preexistentes**, 0 bajo `app/src`. Reproducción: el wire con `"quantity":null` por el camino
  viejo (`RecipeIngredient` no anulable) lanza
  `JsonDecodingException: Failed to parse literal 'null' as a double value at path: $.quantity` →
  `SCHEMA` (receta entera perdida); con `AiIngredientDraft` decodifica y normaliza a
  `RecipeIngredient("Perejil fresco", 0.0, unit=null, ...)`.
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/data/ai/AiRecipeDraft.kt` y
  `AiRecipeIds.kt`, `app/src/test/java/com/sazon/app/data/ai/AiRecipePayloadTest.kt` y `AiRecipeIdsTest.kt`,
  `docs/technical-discovery.md`, `docs/domain/taxonomy.md`, `docs/domain-model.md`,
  `docs/specs/ai-quantity-tolerance.md` (nota), `feature_list.json` (`ai-quantity-tolerance` a `passing`
  con la evidencia) y este registro. Sin cambios en el dominio aceptado (`Recipe.kt`,
  `RecipeValidator.kt`, `RecipeJson.kt`), `AiRecipePayload.kt`, `AiDecodeResult.kt`,
  `AiProviderPolicy.kt`, `OpenAiChat.kt`, `AiHttpClientFactory.kt`, `DevExpertRecipeGenerator.kt`,
  `domain/**`, `ui/**`, Room, assets, build, `init.sh`, `scripts/**`, `DESIGN.md` ni `docs/design/**`;
  sin dependencias nuevas.
- Riesgo o incógnita abierta: se mantienen los `Unknown` de la spec (otras causas de `SCHEMA` en el
  ingrediente fuera del contrato §4; la **deriva de tiempos** `INCOHERENT`/`TIME_DRIFT` —el curry que
  cae por 34 vs 40 min— queda **fuera de alcance**; `name` ausente/vacío sigue siendo `SCHEMA`). Queda
  la **prueba manual** de emulador de la persona (con clave válida, *"arroz con pollo"*: las recetas
  "al gusto" ya no caen por `SCHEMA`, llegan a Resultados y el Detalle pinta "Al gusto"; con la clave
  ausente, modo semilla), no automatizable desde WSL.
- Siguiente paso: **validación independiente** de `ai-quantity-tolerance` con `feature-validator`;
  después, commit local de la rama (lo hace el orquestador de `feature-flow`) y publicación por
  `github-flow`.

### Sesión 082

- Fecha: 2026-10-13
- Objetivo: validar `favorites-sort-controls` (correctiva de `favorites`) de forma independiente y
  persistir la aceptación. Ejecutada por el orquestador de `feature-flow` en la rama
  `feature/favorites-sort-controls` (worktree), solo hasta-aceptación; **sin push ni PR** (publica
  `github-flow`).
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con evidencia que nombra la validación independiente. Sin cambios de código tras la
  validación.
- Verificación ejecutada: el validador re-ejecutó `bash init.sh` (exit 0, test + lint vía
  `scripts/gradle.sh`) y, al verlo `UP-TO-DATE` (no lo tomó como evidencia), forzó
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (34/34 tareas) y `lint --rerun-tasks` (30/30
  tareas); parseó por su cuenta los XML de resultados y el informe `lint-results-debug.xml`, e inspeccionó
  el diff real.
- Evidencia capturada: **64 suites / 456 tests / 0 fallos / 0 errores / 0 saltados** (`FavoritesListTest`
  9, `FavoriteServiceTest` 6, `RoomFavoriteRepositoryTest` 9 verdes); lint **0 errores y 13 warnings
  preexistentes** (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2,
  OldTargetApi 1), **todos** en build/version catalog y **0** bajo `app/src`. Reprodujo los números del
  implementer. Confirmó los Scenarios 1-7 inspeccionables: `FavoritesSort.RECENT.label == "Recientes"`
  con `key`/entries/`fromKey`/`Saver` intactos; `ViewModeToggle` (`Surface(SazonPillShape, surface,
  shadowElevation = 2.dp)` + `Row(selectableGroup())` con dos botones circulares de 28dp
  `selectable(role = Role.RadioButton)` y `contentDescription` "Lista"/"Rejilla"; activo
  `primary`/`onPrimary`, inactivo `Color.Transparent`/`onSurfaceVariant`); `ListGlyph`/`GridGlyph` con
  `Canvas` (sin `material-icons`); **0** `ViewModeButton`, **0** literales "Lista"/"Rejilla" como texto de
  botón (solo las 2 `contentDescription`) y **0** hex sueltos en `ui/favorites`; disciplina de alcance
  (build/lockfile/`init.sh`/dominio/`data`/`ui.theme`/tarjetas/`MainShell`/`SazonApp`/`MainActivity`/
  capturas/stitch intactos, sin dependencias nuevas) y que el implementer **no** hizo commit/push/PR.
  Confirmó además la **exención de capturas** (issue #85) registrada en la spec, en `feature_list.json` y
  en `DESIGN.md`, y que **no** existe ni se crea `docs/design/concepts/favorites-sort-controls/`.
- Ficheros actualizados: `feature_list.json` (feature a `accepted` con la evidencia del validador),
  `docs/validations/favorites-sort-controls.md` (nuevo) y este registro. Sin cambios de código.
- Hallazgos del validador (3 Low informativos, no bloqueantes): los 28dp del toggle frente al objetivo
  táctil de 48dp de `DESIGN.md` (se sigue la captura aceptada; `Unknown` de la spec, revisión de la
  persona); el acabado visual puro (recorte del chip, fidelidad de los glifos, claro/oscuro) queda para la
  persona (`AGENTS.md` no automatiza UI de emulador); la rama va 3 commits por detrás de `origin/main`, que
  `github-flow` sincroniza antes de publicar. Sin hallazgos Critical/High/Medium.
- Riesgo o incógnita abierta: ninguno nuevo; se mantienen los `Unknown` de la spec. Queda la verificación
  **manual** de emulador de la persona (chip "Recientes" entero y sin recorte, toggle icónico en claro y
  oscuro) contra la captura aceptada `docs/design/concepts/favorites/favoritos/` y el registro de
  `DESIGN.md`; un defecto visual se corrige con una feature correctiva.
- Siguiente paso: **commit local** de la rama `feature/favorites-sort-controls` (lo hace el orquestador de
  `feature-flow`); la publicación queda para `github-flow` (`sync-worktree.sh` + push + PR
  `[favorites-sort-controls] …` con `Closes #85`). Tras esta feature, la siguiente correctiva pendiente es
  `profile-stats-heading`.
- Nota de integración: la sesión 082 corrió en paralelo para tres features (`favorites-sort-controls`,
  `recipe-photos` y `ai-quantity-tolerance`); se conservan las **tres** entradas de aceptación.
- Objetivo: validar `recipe-photos` de forma independiente y persistir la aceptación. Ejecutada por el
  orquestador de `feature-flow` en la rama `feature/recipe-photos` (worktree), solo hasta-aceptación; **sin
  push ni PR** (publica `github-flow`).
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a
  `accepted` con la evidencia del validador independiente y se crea
  `docs/validations/recipe-photos.md`. Sin cambios de código tras la validación. Commit local de la rama
  (lo hace el orquestador de `feature-flow`).
- Verificación ejecutada: el validador re-ejecutó `bash init.sh` (exit 0) y, al verlo `UP-TO-DATE`, forzó
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (BUILD SUCCESSFUL 2m42s) y
  `bash scripts/gradle.sh lint --rerun-tasks` (2m40s); parseó por su cuenta los XML y el informe de lint.
- Evidencia capturada: gate **65 suites / 467 tests / 0 fallos / 0 errores / 0 saltados**
  (`RecipePhotoAssetsTest` 11, `RecipeArtworkStyleTest` 8); lint **0 errores / 13 warnings** preexistentes,
  0 bajo `app/src`; `assembleDebug` + inspección del APK con **31 assets** `img/recipes/**`
  (6.092.271 B = 5,81 MiB ≤ 8 MB). Recomputó el mapeo contra la Taxonomía y el catálogo semilla reales y
  contrastó por **md5** los 31 ficheros descargados de los `src` con los empaquetados (**31/31**
  idénticos, JPEG 1376×768). Confirmó Scenarios 1-10, pureza de la cadena, geometría/firma de
  `RecipeArtwork` intactas, **0 `PlateGlyph`**, sin dependencias nuevas (`libs.versions.toml` sin diff),
  disciplina de alcance (`domain/**`, `ui.theme/**`, `docs/domain/taxonomy.json`, `seed-recipes.json`,
  `init.sh`, `scripts/gradle.sh` intactos), docs/ADR coherentes, gate de capturas satisfecho y que el
  implementer no hizo commit/push/PR.
- Hallazgos del validador (5 Low informativos, ninguno bloqueante): el KDoc de `AssetRecipePhotoSource`
  dice ≤960 px cuando los assets son 1376×768 (≈4 MiB por bitmap, techo LRU-16 ≈64,5 MiB; riesgo ya
  registrado); `RecipePhotoAssetsTest` fija `.jpg` literal en 4 aserciones pese a la afirmación de que
  migrar a WebP no requiere cambios; `docs/domain/taxonomy.json` conserva `imageResolution` `.webp` (fuera
  de alcance por la spec); `docs/technical-discovery.md` ~línea 504 lista solo los ADR 0001-0003; el seam
  Android `AssetRecipePhotoSource` no tiene test unitario (la cadena pura sí, como pedía la spec). Sin
  hallazgos Critical/High/Medium.
- Ficheros actualizados: `feature_list.json` (feature a `accepted` con la evidencia del validador),
  `docs/validations/recipe-photos.md` (nuevo) y este registro. Sin cambios de código.
- Riesgo o incógnita abierta: se mantienen los `Unknown` de la spec (jank/memoria al hacer scroll,
  mitigado con `Dispatchers.IO` + LRU 16; fidelidad genérica de la foto de categoría para recetas de IA;
  procedencia de las fotos en el ADR 0004). Queda la **verificación manual** de emulador de la persona
  (Home, Resultados, Favoritos lista+rejilla y Detalle, claro y oscuro; receta IA → foto de categoría;
  prueba destructiva opcional → glifo), no automatizable desde WSL. La rama está 4 commits por detrás de
  `origin/main` → el orquestador de `github-flow` debe `scripts/sync-worktree.sh recipe-photos` antes de
  publicar.
- Siguiente paso: **commit local** de la rama `feature/recipe-photos` (lo hace el orquestador de
  `feature-flow`); la publicación (push + PR con `Closes #83`, sin merge) queda para `github-flow`.
- Objetivo: validar `ai-quantity-tolerance` de forma independiente y persistir la aceptación. Ejecutada
  por el orquestador de `feature-flow` en la rama `feature/ai-quantity-tolerance` (worktree), solo
  hasta-aceptación; **sin push ni PR** (publica `github-flow`).
- Completado: veredicto **`accept`** de un agente con contexto limpio (skill `feature-validator`, agente
  distinto del implementer); `feature_list.json` pasa a `accepted` con evidencia que nombra la validación
  independiente y se persiste `docs/validations/ai-quantity-tolerance.md`. Sin cambios de código tras la
  validación.
- Verificación ejecutada: el validador re-ejecutó `bash init.sh` (exit 0; primera pasada `UP-TO-DATE`, que
  descartó como evidencia débil) y forzó `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks`
  (**34/34 tareas**, `BUILD SUCCESSFUL in 3m48s`); parseó por su cuenta los XML y el informe
  `lint-results-debug.xml`, y montó un **verificador independiente fuera del repo** (`/tmp/opencode/aqt`)
  compilado contra las clases reales de `app/build`.
- Evidencia capturada: **64 suites / 465 tests / 0 fallos / 0 errores / 0 saltados** (`data.ai` = 85:
  `AiProviderPolicyTest` 12, `AiRecipeIdsTest` 7, `AiRecipePayloadTest` 22, `AiVerboseDiagnosticsTest` 7,
  `DevExpertRecipeGeneratorTest` 35, `OpenAiChatTest` 2), reproduciendo los números del implementer; lint
  **0 errores y 13 warnings** preexistentes, 0 bajo `app/src`. Verificador externo **28/28**: Scenarios
  1-7 (nula+`unit` → normaliza a `0.0`/`unit null` y descarta la `unit` del wire; campo ausente idéntico;
  `200 g`/`1 ud` intactos; lote mixto `accepted==1 received==2 {SCHEMA:1}`; Validador no relajado:
  `mealTypeId` inexistente → `SCHEMA` y deriva >5 min → `INCOHERENT`; `source==AI`, `id` `ai-`,
  `matchScore` null), la causa raíz por diferencial real (`RecipeIngredient` lanza
  `JsonDecodingException` con `quantity:null`; `AiIngredientDraft` decodifica y normaliza) y la
  **estabilidad de ids** (recomputó a mano el hash de canonicalización **anterior** y obtuvo el mismo
  `ai-<sha256>`, así que ningún id ya existente cambia). `javap` sobre el bytecode confirmó
  `AiIngredientDraft.getQuantity(): java.lang.Double` (anulable) vs `RecipeIngredient.getQuantity():
  double` (primitivo): el dominio está intacto. `RecipeValidator` **no lee `quantity`**, así que tolerar
  la cantidad no puede debilitar la puerta dura.
- Ficheros actualizados por el orquestador: `feature_list.json` (`ai-quantity-tolerance` a `accepted` con
  la evidencia de la validación independiente), `docs/validations/ai-quantity-tolerance.md` (nuevo) y este
  registro.
- Hallazgos del validador (6 Low informativos, ninguno bloqueante): `Validator Checklist` de la spec sin
  marcar (convención heredada); `AiRecipePayloadTest` importa `ui.detail.IngredientFormat` (pura y pedida
  por la spec); colapso de representaciones del wire (`null`, `0.0`+`null`, `null`+`unit`) comparten
  ingrediente e id, es el comportamiento especificado; sin vector dorado del hash `ai-<sha256>` (gap
  preexistente de `ai-recipe-decode`); artefactos de validación/spec que persistió el orquestador;
  Scenario 8 manual. Sin hallazgos Critical/High/Medium.
- Riesgo o incógnita abierta: ninguno nuevo. Queda la **verificación manual** de la persona en el
  emulador de Android Studio (Windows), no automatizable desde WSL: con `devexpert.apiKey` válida, buscar
  *"arroz con pollo"* y comprobar que las recetas con ingredientes "al gusto" (p. ej. *perejil fresco*) ya
  no caen por `SCHEMA` en el diagnóstico (`SazonAi`) y llegan a Resultados, con "Al gusto" en el Detalle;
  con la clave ausente o vaciada, modo semilla sin fallar.
- Siguiente paso: **commit local** de la rama `feature/ai-quantity-tolerance` (lo hace el orquestador de
  `feature-flow`); la publicación (push + PR, `Closes #<n>`, sin merge) queda para `github-flow`. Con
  esta aceptación **no queda ninguna feature pendiente** en `feature_list.json` (todas `accepted`).

### Sesión 083

- Fecha: 2026-10-13
- Objetivo: implementar `profile-stats-heading` (correctiva de `activity-summary`, `area: ui`): cambiar el
  titular visible del bloque de cifras del Perfil de `"Resumen"` a `"Estadísticas"`, según
  `docs/specs/profile-stats-heading.md`, en la rama `feature/profile-stats-heading` (worktree); solo
  hasta implementación y autoverificación (`passing`), **sin commit/push/PR** (los hace el orquestador).
- Completado:
  - `ui/profile/ActivitySummarySection.kt`: literal del titular `text = "Resumen"` → `text = "Estadísticas"`
    (línea 40) y su KDoc (línea 20) actualizado; el resto del `@Composable` (encabezado `titleMedium`/
    `onBackground`, `Row`, `ActivityStatCard`, `tnum`, tokens `SazonSpacing`/`SazonShapes`) **intacto**.
  - `ui/profile/ProfileScreen.kt`: **solo** el KDoc (línea 37) que citaba la sección como `"Resumen"` pasa a
    `"Estadísticas"`; sin cambios de firma, layout ni orden de items (la sección sigue entre
    `ProfileIdentityEditor` y `ProfileSuggestionsSection`).
  - `DESIGN.md`: párrafo de excepción **issue #86** añadido en `Generated Concept Images` inmediatamente
    después del de `recipe-artwork` (#77), que documenta la **desviación de microcopy** ("Estadísticas" en
    vez de "Resumen", que es lo que dicen la captura aceptada y `docs/design/stitch/21-perfil-resumen.md`) y
    la **exención de capturas** (no se crea `docs/design/concepts/profile-stats-heading/`); Core Screens 7
    cita el titular visible **"Estadísticas"** manteniendo el significado (tarjetas informativas **no
    navegables**). `DESIGN.md` queda internamente coherente (la única mención a "Resumen" es la del propio
    párrafo de desviación, al explicar la diferencia con la captura/prompt).
  - **Sin tests nuevos** (el titular es un literal de UI; no hay lógica JVM que cubrir y la infraestructura
    de Compose UI del repo es instrumentada, fuera de `init.sh` y no ejecutable desde WSL). Sin
    dependencias nuevas; `app/build.gradle.kts` y `gradle/libs.versions.toml` intactos; ficheros en **UTF-8
    sin BOM**.
- Verificación ejecutada: `bash init.sh` (exit 0; `./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`, sin procesos largos) y parseo propio de los XML de `testDebugUnitTest` y del informe
  `lint-results-debug.sarif`; greps de inspección; `git diff --stat`; comprobación de BOM.
- Evidencia capturada: gate **64 suites / 456 tests / 0 fallos / 0 errores / 0 saltados** (sin tests
  nuevos) y lint **0 errores y 13 warnings preexistentes** (GradleDependency 7, NewerVersionAvailable 3,
  AndroidGradlePluginVersion 2, OldTargetApi 1), **0 bajo `app/src`**. Greps: **0** resultados de `"Resumen"`
  entrecomillado en `app/src/main/java/com/sazon/app/ui`; en `ActivitySummarySection.kt` el literal y su KDoc
  dicen "Estadísticas" (líneas 20 y 40); **0** resultados de `clickable`/`onClick` en
  `ActivitySummarySection.kt` (`ActivityStatCard` sigue siendo `Surface` + `Column` + `Text`). Ficheros UTF-8
  sin BOM (`head -c 3` → `pac`).
- Alcance: `git diff --stat` solo con `ActivitySummarySection.kt`, `ProfileScreen.kt` (solo KDoc), `DESIGN.md`
  y `feature_list.json`. Intactos `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`,
  `scripts/**`, `ui/theme/**`, `domain/**`, `data/**`, `docs/specs/activity-summary.md` (su Non-Goal "No se
  añade navegación" sigue vigente sin editar), `docs/design/concepts/**` y `docs/design/stitch/**`. **No** se
  crea `docs/design/concepts/profile-stats-heading/` (exención autorizada por la persona, issue #86).
- Riesgo o incógnita abierta: la **verificación manual** en el emulador (Scenarios 1-3: titular
  "Estadísticas", tarjetas sin navegación, claro/oscuro y contadores poblados/0-0, contra la captura
  aceptada `activity-summary/perfil-resumen`) la revisa la persona conforme a AGENTS.md; no es automatizable
  desde WSL. Un problema de acabado se corrige con una feature correctiva. `docs/design/stitch/21-perfil-resumen.md`
  y la captura seguirán diciendo "Resumen" (fuera de alcance), cubierto por la desviación registrada.
- Siguiente paso: validación independiente de `profile-stats-heading` (`passing`); después, **commit local**
  de la rama (orquestador de `feature-flow`) y publicación (push + PR, sin merge) por `github-flow`.

### Sesión 084

- Fecha: 2026-10-13
- Objetivo: validar de forma independiente `profile-stats-heading` y persistir la aceptación. Ejecutada por
  el orquestador de `feature-flow` en la rama `feature/profile-stats-heading` (worktree); solo
  hasta-aceptación, **sin push ni PR** (publica `github-flow`).
- Completado: veredicto **`accept`** de un agente con contexto limpio; `feature_list.json` pasa a `accepted`
  con evidencia que nombra la validación independiente. Sin cambios de código tras la validación.
- Verificación ejecutada: el validador re-ejecutó `bash init.sh` (exit 0) y, al verlo `UP-TO-DATE`, forzó
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (64 suites / 456 tests) y
  `bash scripts/gradle.sh lint --rerun-tasks`; parseó por su cuenta los XML de resultados y el informe
  `lint-results-debug.sarif`.
- Evidencia capturada: **64 suites / 456 tests / 0 fallos / 0 errores / 0 saltados** (sin tests nuevos,
  coincidente con la evidencia del implementer) y lint **13 warnings / 0 errores / 0 bajo `app/src`**.
  Confirmó el literal exacto `"Estadísticas"` **a nivel de bytes** (UTF-8 sin BOM), **0** resultados de
  `"Resumen"` entrecomillado en `app/src/main/java/com/sazon/app/ui`, **0** `clickable`/`onClick` en
  `ActivitySummarySection.kt` (`ActivityStatCard` sigue `Surface` + `Column` + `Text`), ausencia de
  `docs/design/concepts/profile-stats-heading/` (exención autorizada), `docs/specs/activity-summary.md` sin
  editar con su Non-Goal "No se añade navegación" vigente, y ausencia de cambios en
  `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**` y `app/src/androidTest/**`;
  identificadores de dominio intactos ("Resumen de actividad", `domain.activity`, `data.activity`,
  `activity_summary.db`, costuras de `GenerationService`/`AiSearchService`/`SuggestionBatchService`). Sin
  hallazgos Critical/High/Medium; 3 Low informativos no bloqueantes (revisión visual de emulador pendiente
  de la persona; `DESIGN.md` ya solo menciona "Resumen" en el párrafo de desviación; captura/prompt de
  Stitch siguen diciendo "Resumen", cubiertos por la desviación autorizada).
- Ficheros actualizados: `feature_list.json` y este registro.
- Riesgo o incógnita abierta: la **verificación manual** en el emulador (Scenarios 1-3: titular
  "Estadísticas", tarjetas sin navegación, claro/oscuro y contadores poblados/0-0, contra la captura
  aceptada `activity-summary/perfil-resumen`) la revisa la persona tras el merge, conforme a AGENTS.md; un
  defecto visual se corrige con una feature correctiva. `docs/design/stitch/21-perfil-resumen.md` y la
  captura siguen diciendo "Resumen" (fuera de alcance, cubierto por la desviación registrada).
- Siguiente paso: **commit local** de la rama `feature/profile-stats-heading` (lo hace el orquestador de
  `feature-flow`); la publicación (push + PR) queda para `github-flow`.

### Sesión 085

- Fecha: 2026-10-13
- Objetivo: implementar `dish-image-domain` (fase 1 de `docs/ai-dish-images-proposal.md`, `area: domain`),
  especificada en `docs/specs/dish-image-domain.md`: puerto `DishImageGenerator` + `DishImageRequest`,
  prompt puro `DishImagePrompt` y clave canónica `DishImageCacheKey` en el dominio, **sin red**. Ejecutada
  por el subagente `implementer` en la rama `feature/dish-image-domain` (worktree); solo hasta
  implementación y autoverificación (`passing`), **sin commit/push/PR** (los hace el orquestador).
- Completado:
  - `app/src/main/java/com/sazon/app/domain/image/DishImageCacheKey.kt` (**nuevo**): `object` puro con
    `VERSION = "dish-image-v1"`, `FALLBACK_KEY = "plato"`, `normalize(dishName)` (colapso de espacios,
    `NFD` + `\p{Mn}+`, `lowercase(Locale.ROOT)` explícito, todo fuera de `[a-z0-9]` → `-`, colapso y
    recorte de guiones), `of(dishName)` y `of(recipe)` (título → respaldo por `id`
    `dish-image-v1-plato-<slug-id>` → respaldo por defecto `dish-image-v1-plato`).
  - `app/src/main/java/com/sazon/app/domain/image/DishImagePrompt.kt` (**nuevo**): `object` puro con
    `PROMPT_PREFIX`, `DEFAULT_DISH` y `STYLE_SUFFIX` y la plantilla única
    `<prefijo> <plato>[, <cocina>], <coletilla>`; degradación `título → etiqueta real de categoryId →
    plato casero` y **omisión** de la cláusula de cocina cuando el id no existe o está vacío.
  - `app/src/main/java/com/sazon/app/domain/image/DishImageGenerator.kt` (**nuevo**): `DishImageRequest`
    (`data class` con `cacheKey`, `prompt` y `recipeId` opcional) y su `of(taxonomy, recipe)` como
    **único punto de composición**; puerto `interface DishImageGenerator { suspend fun
    generate(request): ByteArray? }` con `null` = imagen no disponible (**sin** excepción; la
    cancelación se propaga). **Sin** implementación en `app/src/main` (fase 2).
  - `app/src/test/java/com/sazon/app/domain/image/{DishImageCacheKeyTest,DishImagePromptTest,DishImageRequestTest}.kt`
    (**nuevos**): 20 tests JVM que cubren los 9 escenarios de la spec (determinismo con dos `Taxonomy`
    parseadas aparte, etiqueta real de cocina, degradación por categoría y por defecto con receta vacía /
    categoría desconocida / Taxonomía vacía, cocina vacía/desconocida sin marcador, clave de las cuatro
    variantes del mismo nombre, alfabeto `[a-z0-9-]` sin guiones sobrantes, estabilidad, discriminación,
    respaldo por `id` sin colisión, frontera tipográfica **no** semántica, clave independiente del
    `locale`, composición/igualdad estructural de la petición y `recipeId = null` con `id` vacío), más la
    pureza por lectura de los tres fuentes (0 imports de `android`/`androidx`/`io.ktor`/`java.net`/
    `kotlin.random`/`java.util.Random`/`UUID`, 0 `currentTimeMillis`/`Math.random`).
  - `docs/adr/0005-ai-dish-images.md` (**nuevo**): decisión (puerto en el dominio, prompt determinista,
    clave canónica versionada del **nombre del plato**, cadena de `recipe-photos` como respaldo), estado
    **"Aceptado parcialmente (fase 1)"**, ajuste parcial del ADR 0004 y las fases 2-7 enumeradas como
    pendientes; registra la **desviación consciente** frente a la lectura amplia de la propuesta §5
    (normalización tipográfica, no semántica).
  - `docs/technical-discovery.md` (**modificado**): nuevo bullet del puerto `domain.image` (fase 1, puro,
    **sin** adaptador/caché/UI y con la cadena de assets como **única** resolución hoy) en Integrations, y
    bullet de los tests JVM de la política en Testing and Verification.
  - `docs/risks-and-open-questions.md` (**modificado**): fila de riesgo de cuota/latencia/almacenamiento
    por imagen (~2 % de cuota y ~4,2 s; se materializa en las fases 2-3) y ADR 0005 en la lista.
  - **Sin dependencias nuevas**: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`,
    `scripts/**`, `data/**`, `ui/**`, `Recipe.kt`, `RecipeValidator.kt`, `PromptBuilder.kt`, la Taxonomía,
    `seed-recipes.json`, `assets/**`, `DESIGN.md` y `docs/design/**` **intactos**; **no** se crea nada en
    `docs/design/concepts/` (el gate de UI no aplica a una feature de dominio).
- Verificación ejecutada: tirada focal `bash scripts/gradle.sh testDebugUnitTest --tests
  "com.sazon.app.domain.image.*"` (BUILD SUCCESSFUL) y gate estándar `bash init.sh` (**exit 0**;
  `./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`, sin procesos largos); parseo propio de los
  XML de `testDebugUnitTest` y del `lint-results-debug.sarif`; greps de inspección de pureza y alcance;
  `git diff --name-only`.
- Evidencia capturada: gate **68 suites / 496 tests / 0 fallos / 0 errores / 0 saltados**, con las tres
  suites nuevas (`DishImageCacheKeyTest` 8, `DishImagePromptTest` 7, `DishImageRequestTest` 5 = **20**
  tests). Baseline del worktree antes del cambio: **476** métodos `@Test` en **65** ficheros rastreados
  (`git grep -c "@Test"` sobre `HEAD`), es decir **+3 suites / +20 tests** exactos; el "64 suites / 456
  tests" del handoff corresponde a una medición previa al estado de `HEAD` de esta rama. Lint: **0 errores
  y 13 warnings preexistentes** (`GradleDependency` 7, `NewerVersionAvailable` 3,
  `AndroidGradlePluginVersion` 2, `OldTargetApi` 1), **0** bajo `app/src` (todos en `app/build.gradle.kts`,
  `gradle/libs.versions.toml` y `gradle/wrapper/gradle-wrapper.properties`). Inspección: **0** resultados
  de `import android|import androidx|io.ktor|java.net|kotlin.random|java.util.Random|UUID|
  currentTimeMillis` bajo `app/src/main/java/com/sazon/app/domain/image`; **0** implementaciones de
  `DishImageGenerator` en `app/src/main`. `git status` limitado a los 6 ficheros nuevos (3 de código, 3 de
  test) + `docs/adr/0005-ai-dish-images.md` + `docs/specs/dish-image-domain.md` (del planner) + las 3
  modificaciones de docs/estado (`docs/technical-discovery.md`, `docs/risks-and-open-questions.md`,
  `feature_list.json`) y este `PROGRESS.md`.
- Alcance: se cumple la spec al pie de la letra; los escenarios 1-9 están cubiertos por test y los
  no-goals respetados (sin adaptador, Ktor, `/images/*`, `b64_json`, WebP, caché, Room, UI ni cambios en
  `RecipeArtwork`/`recipe-photos`).
- Riesgo o incógnita abierta: el **puerto queda sin implementación** (código no ejecutado) por diseño; el
  prompt depende de las **etiquetas** de la Taxonomía (editar una etiqueta de cocina/categoría cambia el
  prompt pero **no** la clave, así que una imagen cacheada podría no reflejar la etiqueta nueva: se mitiga
  subiendo `VERSION`); los títulos no latinos o solo símbolos caen al respaldo por `id`/por defecto
  (cubierto por test); las preguntas §11 de la propuesta (Room vs ficheros, tamaño y tope de disco, set
  semilla, interruptor, entrada de la variante C) siguen abiertas y **no** se deciden aquí.
- Siguiente paso: validación independiente de `dish-image-domain` (`passing`); después, **commit local** de
  la rama (orquestador de `feature-flow`) y publicación (push + PR contra `main`, sin merge) por
  github-flow.

### Sesión 086

- Fecha: 2026-10-13
- Objetivo: **validación independiente** de `dish-image-domain` (issue #90, `area: domain`, fase 1 de
  `docs/ai-dish-images-proposal.md`) según `docs/specs/dish-image-domain.md`, en modo hasta-aceptación de
  `feature-flow`. La sesión 085 la dejó `passing` (implementada y autoverificada, sin commit). Orquestada
  por el agente principal y ejecutada por el subagente `validator` (contexto limpio); **sin push ni PR**
  (los hace `github-flow`).
- Completado:
  - **Veredicto `accept`** del validador independiente: 9/9 escenarios cubiertos (incluidos los negativos
    obligatorios), alcance respetado, pureza sostenida y documentación durable coherente. Sin hallazgos
    Critical/High/Medium; 3 Low informativos no bloqueantes (L1: un título terminado en coma produciría una
    coma doble, caso no exigido por la plantilla de la spec, anotado para la fase 2/4; L2: el ADR 0004 no
    lleva puntero al 0005, sin contradicción; L3: la baseline "64 suites / 456 tests" de la spec es una
    medición previa a `HEAD`, ya documentada por el implementer).
  - **Acceptance persistida** por el orquestador: `feature_list.json` (`dish-image-domain` de `passing` a
    `accepted`, con la evidencia de la validación) y este `PROGRESS.md`; nuevo registro
    `docs/validations/dish-image-domain.md`.
  - **Commit local** creado en la rama `feature/dish-image-domain` (`feat: complete dish-image-domain`),
    con solo los cambios de la feature aceptada. **Sin** push y **sin** PR: publica el orquestador de
    `github-flow` (push de la rama + PR `[dish-image-domain] …` contra `main`, con `Closes #90`).
- Verificación ejecutada: re-ejecución independiente del gate por el validador — `bash init.sh` (exit 0,
  pero todo `UP-TO-DATE`) más `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (BUILD SUCCESSFUL, 34
  tareas), con parseo propio de los XML **68 suites / 496 tests / 0 fallos / 0 errores / 0 saltados** y de
  `lint-results-debug.sarif` (**0 errores, 13 warnings** preexistentes, 0 bajo `app/src`); greps propios de
  pureza, de ausencia de consumidores/implementaciones del puerto y de alcance.
- Ficheros creados/cambiados: `docs/validations/dish-image-domain.md` (nuevo), `feature_list.json`
  (`dish-image-domain` a `accepted` + evidencia) y este `PROGRESS.md`. El código y los tests ya estaban en
  disco desde la sesión 085 (3 fuentes y 3 tests en `com.sazon.app.domain.image`, ADR 0005,
  `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`) y entran en este mismo commit.
- Hallazgos relevantes: al ver el gate `UP-TO-DATE`, el validador forzó la re-ejecución
  (`--rerun-tasks`) en vez de dar por buenos los resultados cacheados; la baseline del worktree antes del
  cambio (65 ficheros / 476 `@Test` en `HEAD`) confirma el delta exacto (+3 suites / +20 tests) y desmiente
  la cifra "64 / 456" de la spec, que era una medición previa. No hay trabajo de reparación requerido.
- Riesgo o incógnita abierta: ninguno nuevo. Siguen las incógnitas de la fase 1 (puerto sin implementación
  por diseño; el prompt depende de las etiquetas de la Taxonomía mientras la clave no; títulos no latinos
  caen al respaldo) y las preguntas §11 de la propuesta para las fases 2-7. La **verificación manual/E2E no
  aplica**: la feature no cambia comportamiento observable (sin UI, sin red, sin persistencia y sin
  consumidores del puerto).
- Siguiente paso: publicar la rama `feature/dish-image-domain` (push + PR `[dish-image-domain] …` con
  `Closes #90`), que lo hace el orquestador de `github-flow`; no hay siguiente feature (`feature_list.json`
  queda **entero** `accepted`, el plan sigue cerrado).

### Sesión 087

- Fecha: 2026-10-13
- Objetivo: implementar y autoverificar `home-recently-opened-group` (issue #91, correctiva de
  `home-offline-blocks`) según `docs/specs/home-recently-opened-group.md`. Ejecutada por el subagente
  **implementer** en la rama `feature/home-recently-opened-group` (worktree); **sin commit, push ni PR**
  (el commit lo hace el orquestador de `feature-flow`; la publicación es de `github-flow`).
- Completado:
  - **Costura pura** (`ui/home/HomeRecipes.kt`): se retira `recentlyOpened` (muerta: `MainShell` la
    alimentaba con `emptyList()` literal y ninguna feature registra `lastOpenedAt`); `isEmpty =
    saved.isEmpty()` y `filteredBy` acota solo `saved`; KDoc actualizado.
  - **Capa Compose** (`ui/home/HomeScreen.kt`): `RecipesSection` usa `allRecipes = recipes.saved`, ya
    **no** invoca el grupo "Abiertas hace poco" e invoca "Guardadas" sin condicionar su no-vacuidad en
    el call site (el guard vive en el componente); `HomeRecipesGroup` arranca con `if (recipes.isEmpty())
    return` y KDoc "nunca un grupo sin contenido" (ese era el defecto: el título se pintaba siempre).
  - **Integración** (`ui/MainShell.kt`): `HomeRecipes(saved = favorites.map { it.recipe })`, sin el
    literal `recentlyOpened = emptyList()`.
  - **Tests** (`app/src/test/.../HomeRecipesTest.kt`): se elimina
    `con_una_receta_abierta_hace_poco_el_bloque_no_esta_vacio` y se adaptan los **7** restantes al modelo
    de un grupo (`sin_guardadas_el_bloque_esta_vacio`, `con_una_receta_guardada_el_bloque_no_esta_vacio`,
    `filtered_by_categoria_filtra_las_guardadas`, frontera 30 de "Rápido", "Todas" conserva, sin
    coincidencias → `isEmpty`, `filteredBy` no muta el original). Net −1 test; ninguno pasa en vacío.
  - **Docs**: `DESIGN.md` (Core Screen 2 = "Tus recetas" solo lista grupos con contenido + párrafo de
    excepción de capturas de `home-recently-opened-group`, **issue #91**) y `docs/technical-discovery.md`
    (una frase de coherencia: `HomeRecipes` conserva un único grupo `saved` y la Home solo pinta grupos
    con contenido).
- Verificación ejecutada: tirada focal `bash scripts/gradle.sh testDebugUnitTest --tests
  "com.sazon.app.ui.home.*"` (BUILD SUCCESSFUL, 2m22s) y el gate completo **`bash init.sh` → exit 0**
  (`./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`, sin procesos largos). Parseo propio de
  los XML (`app/build/test-results/testDebugUnitTest/TEST-*.xml`) y del informe de lint.
- Evidencia capturada: **65 suites / 475 tests / 0 fallos / 0 errores / 0 saltados**; `HomeRecipesTest`
  **7** tests (8 → 7: cabecera HEAD del fichero con 8 `@Test`, ahora 7 → **net −1**; el resto de ficheros
  de test no cambia, así que la cabecera de este worktree era 476 tests). Lint: **0 errores, 13 warnings
  preexistentes** (GradleDependency 7, NewerVersionAvailable 3, AndroidGradlePluginVersion 2,
  OldTargetApi 1), **0 localizados bajo `app/src`**. Greps de inspección: `grep -rn "recentlyOpened"
  app/src` → **0**; `grep -rn "Abiertas hace poco" app/src` → **0**; `MainShell.HomeRecipes` ya solo lleva
  `saved`; `test -d docs/design/concepts/home-recently-opened-group` → **no existe**. `git diff --stat`
  solo con los 6 ficheros previstos (3 de `ui/home`+`MainShell`, el test, `DESIGN.md` y
  `docs/technical-discovery.md`); `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`,
  `scripts/**`, `ui.theme`, `ui.filters`, `domain/**`, `data/**` y las specs aceptadas
  (`home-offline-blocks`, `local-result-filters`, `favorites`) **sin diff**; ficheros tocados en UTF-8
  **sin BOM**; sin dependencias ni infra de tests nueva; sin commit/push/PR.
- Honestidad de cobertura: el no-pintado del grupo vacío vive en `HomeRecipesGroup` (Compose) y **no** es
  testeable en JVM; la infra de Compose UI del repo es **instrumentada** (`androidTest`, fuera de
  `init.sh` y no ejecutable desde WSL) y esta feature **no** la toca. Queda cubierto por inspección + la
  prueba manual exigida en el emulador (Scenarios 1-3 y 6, claro/oscuro), que revisa la persona.
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/ui/home/HomeRecipes.kt`,
  `app/src/main/java/com/sazon/app/ui/home/HomeScreen.kt`,
  `app/src/main/java/com/sazon/app/ui/MainShell.kt`,
  `app/src/test/java/com/sazon/app/ui/home/HomeRecipesTest.kt`, `DESIGN.md`,
  `docs/technical-discovery.md`, `feature_list.json` (`home-recently-opened-group` a `passing`) y este
  registro. Nada nuevo (la spec es del planner).
- Hallazgos relevantes: la referencia de conteo que citaba la spec (sesión 084: 64 suites / 456 tests →
  ≈455) era de **otro estado del repositorio** (el checkout base en esa fecha); en **este worktree** el
  gate observado es 65 suites / **475** tests (476 en HEAD tras retirar el caso). La spec ya lo anticipaba
  ("la evidencia histórica muestra conteos distintos por worktree; el implementer registra el
  observado"). El número de issue (#91) sí estaba disponible vía `gh`, así que el párrafo de `DESIGN.md`
  lo cita como los vecinos. Nota de tensión ya documentada: `docs/design/stitch/prompt` y las capturas
  pendientes de la Home **sí** describen "Abiertas hace poco"; no se editan (fuera de alcance) y la
  diferencia queda cubierta por el registro en `DESIGN.md`.
- Riesgo o incógnita abierta: la **verificación manual** de la persona en el emulador de Windows con al
  menos una receta guardada ("Tus recetas" muestra "Guardadas" y **no** "Abiertas hace poco"), sin
  recetas (solo título + "Cuando guardes una receta aparecerá aquí.") y con un filtro sin coincidencias
  ("No hay recetas con este filtro." y ningún grupo vacío), en claro y oscuro; no es automatizable desde
  WSL. Un defecto visual se corrige con una feature correctiva. El grupo "Abiertas hace poco" volverá
  cuando exista una feature que registre `lastOpenedAt` (reintroducirá la costura y su microcopy).
- Siguiente paso: validación independiente de `home-recently-opened-group` (`passing`); después, **commit
  local** de la rama (orquestador de `feature-flow`) y publicación (push + PR, sin merge) por
  github-flow.

### Sesión 088

- Fecha: 2026-10-13
- Objetivo: validar de forma independiente `home-recently-opened-group` y persistir la aceptación.
  Ejecutada por el orquestador de `feature-flow` en la rama `feature/home-recently-opened-group`
  (worktree); solo hasta-aceptación, **sin push ni PR** (publica `github-flow`).
- Completado: veredicto **`accept`** de un agente con contexto limpio (rol `validator`,
  `feature-validator`); `feature_list.json` pasa a `accepted` con evidencia que nombra la validación
  independiente; registrado el detalle en `docs/validations/home-recently-opened-group.md`. Sin cambios
  de código tras la validación (solo estado/evidencia y este registro).
- Verificación ejecutada: el validador re-ejecutó `bash init.sh` (exit 0; al verlo `UP-TO-DATE` no lo
  tomó como evidencia) y forzó `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (**34/34 tareas**,
  BUILD SUCCESSFUL) y `bash scripts/gradle.sh lint --rerun-tasks` (**30/30 tareas**); parseó por su cuenta
  los XML y el informe de lint, e inspeccionó el código real.
- Evidencia capturada: **65 suites / 475 tests / 0 fallos / 0 errores / 0 saltados**; `HomeRecipesTest`
  **7** tests verdes (ninguno vacío), reproduciendo el número del implementer; lint **0 errores, 13
  warnings** preexistentes (0 bajo `app/src`). Conformidad confirmada por inspección: `HomeRecipesGroup`
  arranca con `if (recipes.isEmpty()) return`; `RecipesSection` usa `allRecipes = recipes.saved`, ya no
  invoca "Abiertas hace poco" y llama a "Guardadas" sin condicionar su no-vacuidad en el call site;
  `HomeRecipes` con un único grupo `saved`; `MainShell` sin `recentlyOpened`; greps `recentlyOpened` y
  "Abiertas hace poco" en `app/src` → **0**; `test -d docs/design/concepts/home-recently-opened-group` →
  **no existe** (exención autorizada #91); `DESIGN.md` registra la estructura vigente y el párrafo de
  excepción (#91, tras #86). Disciplina de alcance intacta (sin diff en build/lockfile/`init.sh`/
  `scripts/**`/`ui.theme`/`ui.filters`/dominio/`data/**`/specs aceptadas; sin dependencias nuevas; UTF-8
  sin BOM). Confirmó que el implementer no hizo commit/push/PR (HEAD == `origin/main`, sin upstream ni
  PR).
- Hallazgos del validador (Low/informativos, ninguno bloqueante): el guard de no-pintado vive en Compose y
  **no** es testeable en JVM (declarado con honestidad en spec y evidencia; la infra Compose UI del repo
  es `androidTest`, fuera de `init.sh`); quedan referencias históricas a "Abiertas hace poco" en
  `CONTEXT.md`, `docs/domain-model.md`, `docs/design/stitch*/**`, las capturas pendientes de la Home,
  `docs/backend-proposal.md` y specs aceptadas (vocabulario/intención histórica, non-goal declarado;
  manda `DESIGN.md`); la verificación manual de emulador queda pendiente de la persona. Seguridad: sin
  hallazgos (diff solo UI Compose local). Sin hallazgos Critical/High/Medium.
- Riesgo o incógnita abierta: la **verificación manual** de la persona en el emulador de Windows con al
  menos una receta guardada ("Tus recetas" muestra "Guardadas" y **no** "Abiertas hace poco"), sin recetas
  (solo título + "Cuando guardes una receta aparecerá aquí.") y con un filtro sin coincidencias ("No hay
  recetas con este filtro." y ningún grupo vacío), en claro y oscuro; no es automatizable desde WSL. Un
  defecto visual se corrige con una feature correctiva. El grupo "Abiertas hace poco" volverá cuando exista
  una feature que registre `lastOpenedAt`.
- Siguiente paso: **commit local** de la rama `feature/home-recently-opened-group` (lo hace el orquestador
  de `feature-flow`, incluyendo la spec y `docs/validations/home-recently-opened-group.md`); la
  publicación (push + PR con `Closes #91`, sin merge) queda para `github-flow`. En orden de lista, la
  siguiente feature candidata es `dish-image-domain` (`not_started`, dependencias
  `recipe-domain-model`/`taxonomy-in-code` ya `accepted`).

### Sesión 089

- Fecha: 2026-10-13
- Objetivo: implementar y autoverificar `dish-image-generator` (fase 2 de
  `docs/ai-dish-images-proposal.md` y `docs/adr/0005-ai-dish-images.md`), la **única** feature no
  `accepted` del plan. Ejecutada por el subagente `implementer` en la rama
  `feature/dish-image-generator` (worktree); **sin commit, push ni PR** (los hace el orquestador).
- Completado: el **adaptador real** del puerto `com.sazon.app.domain.image.DishImageGenerator` en el
  paquete nuevo `com.sazon.app.data.ai.image` (6 ficheros):
  - `DishImagePolicy` — política **pura** en un único sitio: endpoint (`/images/generations` sobre
    `BASE_URL`), alias `image`, `size 1024x1024`, `TARGET_WIDTH_PX = 960`, `WEBP_QUALITY = 80`,
    `MAX_ATTEMPTS = 1`, `TIMEOUTS` propios (connect 10 s / socket 30 s / request 40 s, más cortos que
    los de chat) y `scaledSize` (solo reduce, proporcional, mínimo 1 px).
  - `OpenAiImages` — DTOs `@Serializable` del wire (`model`, `prompt`, `size`; `data[].b64_json`).
  - `DishImagePayload` — decodificación **tolerante** de `data[0].b64_json` a bytes
    (`java.util.Base64`); cualquier cosa que no encaje → `null` sin lanzar.
  - `DishImageReducer` (`fun interface`) + `AndroidDishImageReducer` — decodifica con `BitmapFactory`,
    reduce con `scaledSize` (sin agrandar) y **re-codifica siempre** a WebP q80 con
    `Bitmap.CompressFormat.WEBP_LOSSY` + `Bitmap.scale` de core-ktx; recicla bitmaps y devuelve `null`
    si falla. **Único** fichero de `data.ai.image` que importa `android.graphics`.
  - `DevExpertDishImageGenerator` — transporte Ktor: clave y `HttpClient` por constructor, timeout por
    petición desde `TIMEOUTS`, una sola petición; clave en blanco → `null` **sin** petición; no-2xx,
    timeout, fallo de transporte, cuerpo ilegible o `b64_json` inválido → `null` **sin lanzar**;
    `CancellationException` **relanzada**; sin `Log`. `request.prompt` viaja tal cual y
    `request.cacheKey` **no** interviene en la red (identidad de la fase 3).
- Tests nuevos (4 ficheros, **22 tests JVM**): `DishImagePolicyTest` (7), `DishImagePayloadTest` (3),
  `DevExpertDishImageGeneratorTest` (7, `MockEngine` + reductor falso en JVM puro) y
  `AndroidDishImageReducerTest` (5, Robolectric `@GraphicsMode(NATIVE)` + `@Config(sdk = [33])`, con el
  camino extremo a extremo `b64_json → WebP 960`).
- Verificación ejecutada: tirada focal `bash scripts/gradle.sh testDebugUnitTest --tests
  "com.sazon.app.data.ai.image.*"` (BUILD SUCCESSFUL, 52s) y el gate canónico **`bash init.sh` → exit 0**
  (`./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`, en invocaciones separadas; sin red, sin
  procesos largos y sin tocar `init.sh`). Parseo propio de los XML y del informe de lint.
- Evidencia capturada: gate completo **72 suites / 517 tests / 0 fallos / 0 errores / 0 saltados**
  (delta vs HEAD = **+4 suites / +22 tests**, los ficheros nuevos; baseline en HEAD re-derivado
  **68 suites / 495 tests** — la spec citaba 496 y las suites coinciden, desviación de 1 en el conteo de
  tests). Lint: **0 errores, 13 warnings preexistentes, 0 bajo `app/src`** (se corrigió un `UseKtx`
  propio con la extensión `Bitmap.scale` de core-ktx, ya presente). Inspección: `import android` solo en
  `AndroidDishImageReducer.kt`; `Recipe|Taxonomy|DishImagePrompt|DishImageCacheKey` → **0**; `Log.` →
  **0**; `test -d docs/design/concepts/dish-image-generator` → **no existe**. `git status` solo con los
  6 ficheros nuevos de `main`, los 4 de test, `docs/specs/dish-image-generator.md` (del planner), los 3
  docs actualizados, `feature_list.json` y este registro; `app/build.gradle.kts`,
  `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `domain/**`, `ui/**`, assets, `DESIGN.md` y
  `docs/design/**` **sin diff**; sin dependencias nuevas; sin commit/push/PR.
- Docs actualizados: `docs/technical-discovery.md` (nuevo bullet del adaptador real `data.ai.image` de
  la fase 2 y el bullet del puerto pasa a "fases 1-2"), `docs/risks-and-open-questions.md` (la fila de
  cuota/latencia pasa a "el transporte ya existe desde la fase 2; se materializa al consumirse en las
  fases 3-4") y `docs/adr/0005-ai-dish-images.md` (estado **Aceptado parcialmente (fases 1-2)**,
  alcance de la fase 2 y consecuencias con las fases 3-7 pendientes).
- Hallazgo relevante: el **runtime gráfico nativo de Robolectric arranca en WSL** (`@GraphicsMode(NATIVE)`
  con `@Config(sdk = [33])`, Robolectric 4.17), así que el códec Android real (`BitmapFactory` +
  `compress(WEBP_LOSSY)`) se ejercita de verdad en JVM con imágenes generadas dentro del test (no se
  versiona ningún binario). No fue necesario declarar ningún bloqueo por este motivo.
- Riesgo o incógnita abierta: la **prueba real opcional** contra el proveedor (verificación #3, ~2 % de
  cuota) **no** se ejecutó (no es puerta de aceptación y las verificaciones #1 y #2 están en verde); la
  primera generación real se hará al consumir el puerto en las fases 3-4. La evidencia del códec cubre
  geometría y formato, no coincidencia píxel a píxel ni la calidad 80 (no verificable de forma fiable).
- Siguiente paso: **validación independiente** de `dish-image-generator` (`passing`) por el subagente
  `validator`; después, commit local de la rama (orquestador de `feature-flow`) y publicación (push + PR,
  sin merge) por `github-flow`.

### Sesión 090

- Fecha: 2026-10-13
- Objetivo: ejecutar `feature-flow` para `dish-image-generator` en modo hasta-aceptación (planner →
  implementer → validator) en el worktree `feature/dish-image-generator`. **Sin push ni PR** (publica el
  orquestador de `github-flow`).
- Completado:
  - Selección: `dish-image-generator` (`area: integration`, `depends_on: ["dish-image-domain"]` ya
    `accepted`), sin spec previa. Rol 1: **planner** (`feature-spec`) → `docs/specs/dish-image-generator.md`
    (nueva, 592 líneas): adaptador Ktor del puerto `domain.image`, política `DishImagePolicy` (endpoint,
    alias `image`, `1024x1024`, 960 px WebP q80, timeouts propios 10/30/40 s, un solo intento), costura
    `DishImageReducer`/`AndroidDishImageReducer`, 10 escenarios, no-goals (sin caché/UI/consumidores) y
    checklist. **Sin gate de capturas** (no es `area: ui`).
  - Rol 2: **implementer** (`feature-implementer`); dejó la feature `passing` (sesión 089).
  - Rol 3: **validator** (`feature-validator`), independiente y con contexto limpio.
  - **Veredicto `accept`**: 72 suites / **517 tests** / 0 fallos / 0 errores / 0 saltados (delta exacto
    +4 suites / +22 tests sobre la baseline re-derivada en `HEAD`, 68/495), lint 0 errores / 13 warnings
    preexistentes / 0 bajo `app/src`, escenarios 1-10 verificados (incluido el códec Android real con
    Robolectric `@GraphicsMode(NATIVE)` en WSL) y disciplina de alcance intacta. Sin hallazgos
    Critical/High/Medium; 4 Low informativos no bloqueantes (`MAX_ATTEMPTS` declarativa; base64 estricto
    sin diagnóstico; `catch(Exception)` no cubre `OutOfMemoryError`; baseline 496 vs 495). Detalle en
    `docs/validations/dish-image-generator.md`.
  - **Persistencia de la aceptación**: `feature_list.json` (`dish-image-generator` → `accepted` con la
    evidencia de la validación independiente) y este registro.
- Verificación ejecutada: `bash init.sh` → **exit 0** en el arranque (antes de planificar) y re-ejecutado
  por el implementer y el validador; el validador forzó `scripts/gradle.sh testDebugUnitTest
  --rerun-tasks` (34/34 tareas) y `lint --rerun-tasks`. Nota de entorno: el primer `lint --rerun-tasks`
  del validador murió con **SIGSEGV del daemon de Gradle** (Temurin 21); el reintento fue verde (no se usó
  `--stop`).
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/data/ai/image/{DishImagePolicy,OpenAiImages,DishImagePayload,DishImageReducer,AndroidDishImageReducer,DevExpertDishImageGenerator}.kt`,
  los 4 tests en `app/src/test/java/com/sazon/app/data/ai/image/`, `docs/specs/dish-image-generator.md`,
  `docs/validations/dish-image-generator.md`, `docs/technical-discovery.md`,
  `docs/risks-and-open-questions.md`, `docs/adr/0005-ai-dish-images.md`, `feature_list.json` y este
  registro. Build/lockfile/`init.sh`/`scripts/**`/`domain/**`/`ui/**`/assets/`DESIGN.md`/`docs/design/**`
  intactos; sin dependencias nuevas.
- Hallazgos relevantes: el runtime gráfico nativo de Robolectric **sí** arranca en WSL, así que el códec
  Android real se ejercita en JVM sin emulador (no hubo bloqueo). La clave solo entra por constructor y
  viaja en `Authorization`; el adaptador no registra nada.
- Riesgo o incógnita abierta: la **prueba real opcional** contra el proveedor (~2 % de cuota) **no** se
  ejecutó (no es puerta de aceptación); la primera generación real se hará en la fase 4. Los hallazgos
  Low quedan como endurecimiento para las fases 3-4.
- Commit local de la aceptación en la rama `feature/dish-image-generator` (orquestador de `feature-flow`);
  **sin push ni PR**.
- Siguiente paso: **ninguna feature** en esta sesión; `github-flow` publica la rama (push + PR con
  `Closes`, sin merge). La siguiente feature del plan es la **fase 3**, `dish-image-cache`
  (`not_started`), y requeriría una nueva ejecución de `github-flow execute dish-image-cache`.

### Sesión 091

- Fecha: 2026-10-13
- Objetivo: ejecutar `feature-flow` para `dish-image-cache` (fase 3 de `docs/ai-dish-images-proposal.md`)
  en modo hasta-aceptación (planner → implementer → validator) en el worktree
  `feature/dish-image-cache`. **Sin push ni PR** (publica el orquestador de `github-flow`). `area: data`;
  **sin gate de capturas** (no es UI).
- Completado (rol 1, **planner**): spec `docs/specs/dish-image-cache.md` (620 líneas) con 13 escenarios,
  no-goals (sin UI/cableado, sin Room, sin códec, sin consumidores, sin dependencias), plan de
  verificación (JVM + Robolectric solo para `cacheDir`) y checklist del validador. Fija las decisiones de
  la persona (2026-10-13): **ficheros en `cacheDir/dish-images`, no Room**; clave canónica = nombre de
  fichero; tope de disco 80 MiB con evicción LRU; LRU de memoria de **bytes** (no de bitmaps).
- Completado (rol 2, **implementer**): la feature queda **`passing`**. Cinco ficheros nuevos en
  `com.sazon.app.data.image` — `DishImageCachePolicy` (constantes, `fileNameFor` y `evictionVictims`
  pura, con desempate por nombre), `FileDishImageCache` (raíz `File` y `now` inyectables, escritura
  atómica temporal+`renameTo`, toque de recencia al leer, evicción LRU con tope inyectable, **nunca
  lanza** por I/O), `MemoryDishImageCache` (LRU de bytes, `accessOrder = true`, tope 16, `synchronized`),
  `DishImageResolver` (memoria → disco → generador → persistir, `Mutex` por clave, `CancellationException`
  relanzada, persistencia *best-effort*) y `AndroidDishImageCache.root(context)` (único fichero con
  `android.*`) — más 5 tests JVM (Robolectric `@Config(sdk = [33])` solo en la costura de `cacheDir`).
- Verificación ejecutada: tirada focal `bash scripts/gradle.sh testDebugUnitTest --tests
  "com.sazon.app.data.image.*"` → BUILD SUCCESSFUL (5 suites / 34 tests) y **`bash init.sh` exit 0**
  (`./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`, sin procesos largos, sin red, sin cuota y
  sin tocar `init.sh`). Parseo propio de los XML: **77 suites / 551 tests / 0 fallos / 0 errores /
  0 saltados** (baseline en `HEAD` 72/517 re-derivada; delta +5 suites / +34 tests: `DishImageCachePolicyTest`
  7, `FileDishImageCacheTest` 8, `MemoryDishImageCacheTest` 5, `DishImageResolverTest` 12,
  `AndroidDishImageCacheTest` 2). Lint: **0 errores y 12 warnings** preexistentes, todos en el
  build/version catalog, **0 bajo `app/src`** (la spec citaba 13; los avisos de "versión nueva disponible"
  dependen de la metadata del momento — desviación anotada, no bloqueante).
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/data/image/{DishImageCachePolicy,FileDishImageCache,MemoryDishImageCache,DishImageResolver,AndroidDishImageCache}.kt`,
  los 5 tests en `app/src/test/java/com/sazon/app/data/image/`, `docs/specs/dish-image-cache.md` (del
  planner), `docs/technical-discovery.md`, `docs/adr/0005-ai-dish-images.md`,
  `docs/risks-and-open-questions.md`, `feature_list.json` y este registro. `app/build.gradle.kts`,
  `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `domain/**` (incluido `domain/image`),
  `data/ai/image/**`, `data/RecipePhotoAssets.kt`, `data/AssetRecipePhotoSource.kt`, `ui/**`,
  `assets/**`, `DESIGN.md` y `docs/design/**` intactos; sin dependencias nuevas y **nada** en
  `docs/design/concepts/`.
- Hallazgos relevantes: (a) la **escritura atómica** deja el fichero servible solo cuando está completo
  (temporal en la misma raíz + `renameTo` + limpieza del temporal); (b) la evicción es una **LRU real**
  porque `read` toca `lastModified` (si el sistema de ficheros no lo soportara, degrada a FIFO, aceptado);
  (c) la **deduplicación por clave** (`Mutex` por `cacheKey`) evita pagar dos veces la misma imagen en
  peticiones simultáneas (p. ej. tarjeta y hero); (d) la LRU de memoria guarda **bytes**, no bitmaps:
  mantiene `data.image` sin `android.graphics` y deja la decodificación a la fase 4.
- Riesgo o incógnita abierta (registrados, no bloqueantes): un cambio de `DishImageCacheKey.VERSION` deja
  huérfanos los `dish-image-v1-*.webp` hasta que la LRU los evicte (acotado por el tope); `cacheDir` puede
  vaciarlo el sistema (la caché es *best-effort*); un fichero corrupto se sirve tal cual (la caché no
  decodifica, lo degrada la fase 4); la primera imagen de cada plato sigue costando red y cuota (lo ataca
  el set semilla, fase 7).
- Completado (rol 3, **validator**, contexto limpio): veredicto **`accept`** independiente. Re-ejecutó
  `bash init.sh` (exit 0; forzó la pasada que salió `UP-TO-DATE`), `bash scripts/gradle.sh
  testDebugUnitTest --rerun-tasks` (parseo propio de los XML: **77 suites / 551 tests / 0 fallos /
  0 errores / 0 saltados**; focal 5 suites / 34 tests) y `bash scripts/gradle.sh lint --rerun-tasks`
  (0 errores, 13 warnings preexistentes, **0 bajo `app/src`**); re-derivó la baseline en `HEAD` con
  `git grep -c '@Test'` (72 ficheros / 517 tests → delta exacto +5 suites / +34 tests) y confirmó los
  escenarios 1-13 (tests no vacuos; evicción con tope inyectado y confinada a su raíz/extensión; LRU de
  memoria; fallos → `null` sin persistir ni dejar temporales; `CancellationException` relanzada;
  *single-flight* por clave; Robolectric bajo `cacheDir` y `filesDir` intacto), la pureza (`import android`
  solo en `AndroidDishImageCache.kt`; 0 `androidx.`/`Recipe`/`Taxonomy`/`DishImageCacheKey`/`Log.`/
  `filesDir`/`getDatabasePath`/`graphics`), los non-goals (sin UI/cableado/Room/códec/`android.graphics`/
  `dishKey`/set semilla/dependencias nuevas) y el alcance del diff. Sin hallazgos Critical/High/Medium;
  5 Low informativos no bloqueantes (Mutex por clave sin poda; `.tmp` huérfano de *crash* no medido ni
  evictado; invariante de alfabeto de `cacheKey` a validar al cablear en fase 4; evicción solo en
  `write`; el rerun de lint da 13 warnings —la spec citaba 13, el implementer 12—, sin cambio material).
  Detalle en `docs/validations/dish-image-cache.md`.
- Verificación de la aceptación: la feature pasa a **`accepted`** en `feature_list.json` con la evidencia
  del validator. Commit local de la aceptación por el orquestador de `feature-flow` en la rama
  `feature/dish-image-cache`; **sin push ni PR**.
- Ficheros creados/cambiados (cierre): `docs/validations/dish-image-cache.md` (nuevo), `feature_list.json`
  (estado `accepted` + evidencia del validator), este registro y el commit local.
- Siguiente paso: **ninguna feature** en esta sesión; `github-flow` publica la rama (push + PR con
  `Closes`, sin merge). La siguiente feature del plan es la **fase 4**, `dish-image-progressive-ui`
  (`area: ui`), que **requiere capturas de referencia** en
  `docs/design/concepts/dish-image-progressive-ui/` antes de implementar y se reanudaría con
  `github-flow execute dish-image-progressive-ui`.

### Sesión 092

- Fecha: 2026-10-13
- Objetivo: ejecutar `feature-flow` para `dish-image-progressive-ui` (fase 4, **mitad de integración** de
  `docs/ai-dish-images-proposal.md`) en modo hasta-aceptación (planner → implementer → validator) en el
  worktree `feature/dish-image-progressive-ui`. **Sin push ni PR** (publica el orquestador de
  `github-flow`). `area: integration`; **desviación de capturas autorizada** por el orquestador (misma
  clase que #77/#85/#86/#91), por lo que **no** se crea `docs/design/concepts/dish-image-progressive-ui/`.
- Completado (rol 1, **planner**): spec `docs/specs/dish-image-progressive-ui.md` (639 líneas) con 8
  escenarios, no-goals (sin UI nueva, sin interruptor/tope, sin tocar `data/**`/`domain/**`, sin
  dependencias) y checklist del validador. Reconciliación del harness (orquestador, pre-implementación):
  el `verification` de la entrada citaba "tope de generaciones por sesión respetado" (atribuido al
  hermano `dish-image-settings-toggle`); se sustituye por "límite de concurrencia (2) respetado".
- Completado (rol 2, **implementer**): la feature queda **`passing`**. Seis ficheros nuevos en
  `com.sazon.app.ui.artwork` — `GeneratedRecipeArtworkSource` (seam `fun interface` +
  `NoGeneratedRecipeArtworkSource` + `LocalGeneratedRecipeArtworkSource`, default no-op),
  `RecipeArtworkImagePrecedence` (precedencia `GENERATED`/`LOCAL`/`FALLBACK` y objetivo del crossfade,
  puros y genéricos), `DishImageArtworkPolicy` (concurrencia **2**, crossfade **250 ms**, LRU de
  decodificadas **16**), `DishImageArtworkLoader<T>` (LRU por clave canónica + `Semaphore(2)` +
  degradación `null` sin cachear + `CancellationException` propagada), `RecipeImageDecoder` y
  `AndroidRecipeImageDecoder` (`BitmapFactory`, **único** `android.graphics` de `ui.artwork`) — más
  `RecipeArtwork` reescrito por **capas** (capa base siempre + capa generada con alfa animado; firma
  `(recipe, modifier, glyphSize)` y geometría intactas) y el cableado de `MainActivity` (generador real
  + resolutor de la fase 3 + loader + decodificador, reutilizando el `aiClient` compartido).
- Verificación ejecutada: tirada focal `bash scripts/gradle.sh testDebugUnitTest --tests
  "com.sazon.app.ui.artwork.*"` → BUILD SUCCESSFUL (5 suites / 19 tests) y **`bash init.sh` exit 0**
  (`./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`, sin procesos largos, sin red, sin cuota y
  sin tocar `init.sh`). Parseo propio de los XML: **81 suites / 562 tests / 0 fallos / 0 errores /
  0 saltados** (baseline en `HEAD` 77/551 re-derivada; delta **+4 suites / +11 tests**:
  `RecipeArtworkImagePrecedenceTest` 2, `DishImageArtworkLoaderTest` 6, `GeneratedRecipeArtworkSourceTest`
  1, `AndroidRecipeImageDecoderTest` 2). Lint (SARIF): **0 errores y 13 warnings** preexistentes, todos en
  el build/version catalog, **0 bajo `app/src`**.
- Ficheros creados/cambiados: los 6 ficheros nuevos de `ui/artwork`, los 4 tests nuevos en
  `app/src/test/java/com/sazon/app/ui/artwork/`, `RecipeArtwork.kt`, `MainActivity.kt`,
  `docs/specs/dish-image-progressive-ui.md` (del planner), `DESIGN.md`, `docs/technical-discovery.md`,
  `docs/adr/0005-ai-dish-images.md`, `docs/domain/taxonomy.md`, `docs/risks-and-open-questions.md`,
  `feature_list.json` y este registro. Los **5 puntos de uso**, `RecipePhotoSource.kt`, `data/**`,
  `domain/**`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`,
  `app/src/androidTest/**`, `AndroidManifest.xml`, `assets/**`, `seed-recipes.json` y las specs aceptadas
  **intactos**; sin dependencias nuevas y **nada** en `docs/design/concepts/`.
- Hallazgos relevantes: (a) la **capa base** (foto local o degradado + glifo) se pinta siempre y la
  generada se **superpone** con alfa animado: no hay hueco ni frame en blanco, ni siquiera con la caché
  fría; (b) en la **primera composición** `animateFloatAsState` arranca en el objetivo, así que un acierto
  de caché se pinta directamente; (c) el **LRU por clave canónica** (`DishImageCacheKey`, nombre de plato)
  evita volver a resolver/decodificar en el scroll de ida y vuelta, y el `Semaphore(2)` acota las
  resoluciones en vuelo; (d) el `null`/array vacío/códec roto **no se cachean** (se reintenta) y la
  cancelación se propaga.
- Riesgo o incógnita abierta (registrados, no bloqueantes): sin el hermano `dish-image-settings-toggle`
  **no hay tope por sesión ni interruptor**, así que el gasto de cuota queda acotado solo por la
  concurrencia (2), la clave compartida y la caché persistente; la cadena literal genera también para las
  12 recetas semilla (candidato a un refinamiento barato, no aplicado para no desviarse del plan); una
  cancelación a mitad de petición puede "perder" una generación ya enviada (cuota gastada sin disco);
  `cacheDir` puede vaciarlo el sistema (se regenera y vuelve a gastar cuota).
- Rol 3 (**validator**): **pendiente** en esta sesión al cierre del implementer (el validador
  independiente es la siguiente puerta; la feature queda `passing`, no `accepted`).
- Siguiente paso: validación independiente de `dish-image-progressive-ui` (o el ciclo que decida el
  orquestador). Después, la **fase 4, mitad de UI** (`dish-image-settings-toggle`, `area: ui`) es la
  siguiente feature del plan (interruptor + tope por sesión) y **sí** requiere capturas o desviación
  autorizada registrada en `DESIGN.md`. `github-flow` publica la rama (push + PR con `Closes`, sin merge).

### Sesión 093

- Fecha: 2026-10-13
- Objetivo: ejecutar `feature-implementer` para `dish-image-settings-toggle` (fase 4, **mitad de UI** de
  `docs/ai-dish-images-proposal.md`: **interruptor** de generación + **tope por sesión**) en el worktree
  `feature/dish-image-settings-toggle`. **Sin commit, push ni PR** (el commit lo hace el orquestador de
  `feature-flow`; publica `github-flow`). `area: ui`; **desviación de capturas autorizada** por la persona
  (2026-10-13, misma clase que #77/#85/#86/#91/#98): **no** se crea
  `docs/design/concepts/dish-image-settings-toggle/` ni su `README.md`.
- Completado (rol **implementer**): la feature queda **`passing`** (validación independiente pendiente).
  El ajuste se gobierna en **dos puntos** (decisión cerrada del spec): (a) el proveedor del seam en
  `MainActivity` gatea el **interruptor** (`DishImageSettings.generatedIfEnabled`: apagado = `null` sin
  leer caché ni generar, solo foto local) y (b) el generador real va envuelto por
  `GatedDishImageGenerator` (decorador del puerto), que **reserva** una unidad **antes** de invocar al
  delegado y devuelve `null` **sin invocarlo** si el interruptor está apagado o el tope agotado.
- Ficheros creados: `data/image/DishImageGenerationBudget.kt` (contador atómico con reserva estricta,
  tope inyectable) y `data/image/DishImageSettings.kt` (`DishImageSettingsPolicy` con
  `DEFAULT_ENABLED = true` y `MAX_GENERATIONS_PER_SESSION = 12` en un único sitio, patrón
  `AiProviderPolicy`; `DishImageSettingsState` con `remaining`/`exhausted` derivados; `DishImageSettings`
  con **interruptor** + `state: StateFlow` + `generatedIfEnabled` genérico; y `GatedDishImageGenerator`);
  `ui/profile/DishImageSettingsCopy.kt` (microcopy puro y determinista: singular/plural, tope alcanzado y
  apagado) y `ui/profile/DishImageSettingsSection.kt` (Compose delgada: título `titleMedium` + fila
  `toggleable(role = Role.Switch)` con label `bodyLarge`, resumen `bodyMedium`/`onSurfaceVariant`, helper
  `labelSmall`, `SazonSpacing.md`, ≥48dp y `Switch(onCheckedChange = null)`; sin iconos ni hex).
  Tests: `DishImageGenerationBudgetTest` 7, `DishImageSettingsTest` 16, `DishImageSettingsCopyTest` 8.
- Ficheros modificados: `MainActivity.kt` (crea `DishImageSettings` por proceso, envuelve el generador
  real y gatea el proveedor del seam), `ui/SazonApp.kt` y `ui/MainShell.kt` (reparto por parámetro;
  `MainShell` observa `state.collectAsState()` y pasa estado + callback a `ProfileScreen`),
  `ui/profile/ProfileScreen.kt` (2 parámetros nuevos + el `item` de la sección **al final** del
  `LazyColumn`, para no reordenar ninguna sección aceptada) y los 4 docs durables.
- Verificación ejecutada: `bash init.sh` → **exit 0** (`./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`; sin procesos largos, sin red, sin cuota, sin tocar `init.sh`). Parseo propio de los
  XML (`app/build/test-results/testDebugUnitTest/TEST-*.xml`): **84 suites / 593 tests / 0 fallos /
  0 errores / 0 saltados**. Baseline en `HEAD` (`c0cb069`) **re-derivada** por grep propio: 81 suites /
  562 tests → delta **+3 suites / +31 tests** (`DishImageGenerationBudgetTest` 7,
  `DishImageSettingsTest` 16, `DishImageSettingsCopyTest` 8). Tiradas focales: `--tests
  "com.sazon.app.data.image.*"` → **7 suites / 57 tests** (las 5 aceptadas de la fase 3 + las 2 nuevas) y
  `--tests "com.sazon.app.ui.profile.*"` → **3 suites / 28 tests** (`DishImageSettingsCopyTest` 8 +
  `ProfileEditStateTest` 9 + `SuggestionsRefreshFormatTest` 11), ambas `BUILD SUCCESSFUL`. Lint (SARIF
  `app/build/reports/lint-results-debug.sarif`): **0 errores** y **13 warnings** preexistentes
  (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1),
  **0 bajo `app/src`**.
- Inspección de pureza y alcance: los 2 ficheros nuevos de `data.image` **no** importan `android.*` /
  `androidx.*` (solo `AtomicInteger` y `kotlinx.coroutines.flow` + `domain.image`); `ui/artwork/**` **sin
  diff** y `RecipeArtwork` conserva la firma `(recipe, modifier, glyphSize)` con los **5 puntos de uso**
  intactos; sin cambios en `data/image` aceptado, `data/ai/image/**`, `domain/**`, `ui/theme/**`,
  `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `AndroidManifest.xml`,
  `assets/**` ni `seed-recipes.json`; sin dependencias nuevas; `Switch`/`toggleable` solo en la sección
  nueva y **0 hex** sueltos; `docs/design/concepts/dish-image-settings-toggle/` **no existe**.
- Hallazgos relevantes: (a) el tope **no** se consume con los aciertos de memoria/disco (el decorador
  está en el borde de generación, por debajo del resolutor de la fase 3); (b) **apagar/encender no
  reinicia** el tope y el interruptor apagado **no** reserva; (c) la reserva precede a la llamada, así
  que un **fallo** del proveedor y una generación **cancelada** consumen la unidad (semántica
  conservadora, documentada en el ADR 0005 y en riesgos) y la `CancellationException` **se propaga**;
  (d) el decorador recibe el `DishImageSettings` completo para que el estado observable y el tope
  aplicado sean la **misma** fuente.
- Riesgo o incógnita abierta (registrados, no bloqueantes): el valor del tope (**12**) es una primera
  estimación y se cambia en `DishImageSettingsPolicy` (un único sitio); una sesión sin red puede agotar
  el tope con fallos rápidos hasta reiniciar (guardarraíl conservador; reembolsar fallos sería una
  correctiva); el microcopy exacto y el acabado del `Switch` los revisa la persona en el emulador
  (defecto visual → feature correctiva, sin bloquear el flujo autónomo).
- Rol 3 (**validator**): **`accept`** independiente (agente con contexto limpio). Re-ejecutó `bash init.sh`
  (exit 0, `UP-TO-DATE`) y forzó `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (34/34 tareas) y
  `bash scripts/gradle.sh lint --rerun-tasks` (30/30 tareas); parseó por su cuenta los XML
  (**84 suites / 593 tests / 0 fallos / 0 errores / 0 saltados**) y el SARIF (**0 errores, 13 warnings**
  preexistentes, 0 bajo `app/src`), re-derivó la baseline en `HEAD` `c0cb069` (81 suites / 562 tests ⇒
  +3 suites / +31 tests) y verificó por greps propios la pureza de los nuevos `data.image`, la constante
  única `MAX_GENERATIONS_PER_SESSION`, el alcance (`ui/artwork/**` sin diff, firma y 5 puntos de uso
  intactos, sin dependencias), las capturas (carpeta inexistente; `DESIGN.md` con el párrafo #101) y la no
  publicación (0 commits sobre `origin/main`, sin rama remota ni PR; issue #101 `OPEN`). Sin hallazgos
  Critical/High/Medium; 5 Low informativos no bloqueantes. Detalle en
  `docs/validations/dish-image-settings-toggle.md`.
- Persistencia del `accept` (orquestador, mismo worktree): `feature_list.json` pasa a **`accepted`** con la
  evidencia del validator y `PROGRESS.md` actualizado; commit local en `feature/dish-image-settings-toggle`
  (**sin push ni PR**; publica `github-flow`).
- Siguiente paso: queda la **prueba manual en emulador** de la persona tras el merge y, en el plan, las
  fases 5-7 (`dish-visual-descriptor`, `dish-style-reference`, `dish-image-seed`, `not_started`). Nota de
  entorno: durante esta sesión un `--rerun-tasks` hizo **crashear el daemon de Gradle** (JVM, 25.º build
  del mismo daemon) y dejó un `hs_err_pid*.log` en la raíz (retirado); era del daemon (heap/uptime), no del
  código, porque el `bash init.sh` y las tiradas focales ya habían pasado en verde. No se usó
  `./gradlew --stop` (AGENTS.md, Notas de entorno).

### Sesión 094

- Fecha: 2026-10-13
- Objetivo: ejecutar `feature-implementer` para `dish-visual-descriptor` (fase 5, **variante B** de
  `docs/ai-dish-images-proposal.md` §4.1: descriptor visual opcional del plato en el contrato de la IA)
  en el worktree `feature/dish-visual-descriptor`. **Sin commit, push ni PR** (el commit lo hace el
  orquestador de `feature-flow`; publica `github-flow`). `area: integration`; **gate de capturas no
  aplica** (**no** se crea `docs/design/concepts/dish-visual-descriptor/`).
- Completado (rol **implementer**): la feature queda **`passing`** (validación independiente pendiente).
  Valor puro nuevo `domain.image.DishVisualDescriptor` (`dishKey`/`imagePrompt`, `of`/`isEmpty`/`NONE`:
  colapsa espacios, conserva el `dishKey` tal cual y lo descarta si no deja ningún carácter `[a-z0-9]`,
  descarta `imagePrompt` en blanco); parámetros **opcionales con default** en `DishImageCacheKey.of`
  (el `dishKey` normalizado gana al título), `DishImagePrompt.build` (precedencia `imagePrompt → título →
  categoría → DEFAULT_DISH`, conservando prefijo/cocina/`STYLE_SUFFIX`) y `DishImageRequest.of`;
  `AiRecipeDraft` gana `dishKey`/`imagePrompt` como `JsonElement?` tolerantes + `descriptor()`;
  `AiDecodeResult.descriptors` (por `id` **generado**, solo recetas aceptadas) rellenado en
  `AiRecipePayload.decode`; y `OUTPUT_CONTRACT` de `PromptBuilder` con los dos campos opcionales + su
  instrucción. `Recipe.kt`, `RecipeValidator.kt` y `AiRecipeIds.kt` **no** cambian (el descriptor no se
  valida, no se persiste y no entra en el `id`); `DishImageCacheKey.VERSION` **no** sube.
- Ficheros creados: `app/src/main/java/com/sazon/app/domain/image/DishVisualDescriptor.kt`,
  `app/src/test/java/com/sazon/app/domain/image/DishVisualDescriptorTest.kt` (7 tests) y
  `app/src/test/java/com/sazon/app/data/ai/DishVisualDescriptorWireTest.kt` (7 tests).
- Ficheros modificados: `domain/image/{DishImageCacheKey,DishImagePrompt,DishImageGenerator}.kt`,
  `data/ai/{AiRecipeDraft,AiDecodeResult,AiRecipePayload}.kt`,
  `domain/prompt/PromptBuilder.kt`; tests ampliados `DishImageCacheKeyTest` (8→12),
  `DishImagePromptTest` (7→11), `DishImageRequestTest` (5→7), `PromptBuilderTest` (10→12); y los 4 docs
  durables (`docs/domain/taxonomy.md` §4 y §6, `docs/adr/0005-ai-dish-images.md`,
  `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`).
- Verificación ejecutada: `bash init.sh` → **exit 0** (`./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`; sin procesos largos, sin red, sin cuota, sin tocar `init.sh`). Tirada focal
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.domain.image.*" --tests
  "com.sazon.app.data.ai.DishVisualDescriptorWireTest" --tests "com.sazon.app.domain.prompt.PromptBuilderTest"`
  → `BUILD SUCCESSFUL`; tirada completa forzada `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks`
  → `BUILD SUCCESSFUL` (34/34 tareas ejecutadas). Parseo propio de los XML frescos
  (`app/build/test-results/testDebugUnitTest/TEST-*.xml`): **86 suites / 619 tests / 0 fallos / 0 errores /
  0 saltados**. Baseline en `HEAD` re-derivada (`git grep -c '@Test' -- 'app/src/test/**'`) = **593**;
  delta **+2 suites / +26 tests** (`DishVisualDescriptorTest` 7, `DishVisualDescriptorWireTest` 7,
  `DishImageCacheKeyTest` +4, `DishImagePromptTest` +4, `DishImageRequestTest` +2, `PromptBuilderTest` +2).
  Lint (SARIF `app/build/reports/lint-results-debug.sarif`): **0 errores** y **13 warnings**
  preexistentes (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2,
  `OldTargetApi` 1), **0 bajo `app/src`**.
- Vectores concretos comprobados: dos títulos distintos con el mismo `dishKey` comparten
  `dish-image-v1-arroz-con-pollo` (frente a sus claves por título distintas); clave y prompt con
  descriptor `null`/`NONE`/solo `dishKey` idénticos a la fase 1; respuestas con `"dishKey": 123`, objeto,
  array o `"🍲"` **aceptadas** sin `SCHEMA` y sin descriptor; descriptor parcial conserva el campo
  utilizable; `id` idéntico con y sin descriptor.
- Inspección de pureza y alcance: `domain.image` solo importa `domain.recipe`/`domain.taxonomy` y
  `java.text.Normalizer`/`java.util.Locale` (0 `android.*`, `androidx.*`, `io.ktor`, `java.net`,
  `Random`/`UUID`/`currentTimeMillis`); `DishVisualDescriptor.kt` **no importa nada**; `git diff
  --name-only` = lista prevista (7 `main`, 4 tests ampliados + 3 nuevos, 4 docs, `feature_list.json`);
  `Recipe.kt`, `RecipeValidator.kt`, `AiRecipeIds.kt`, `domain/validation/**`, `data/image/**`,
  `data/ai/image/**`, `ui/**`, `MainActivity.kt`, build, `init.sh`, `scripts/**`, Taxonomía,
  `seed-recipes.json` y `assets/**` **sin diff**; sin dependencias nuevas; nada creado en
  `docs/design/concepts/`.
- Hallazgos relevantes: (a) el parseo tolerante (`JsonElement?` + `JsonPrimitive.isString`) es
  imprescindible: con `val dishKey: String?` un `"dishKey": 123` contaría la receta como `SCHEMA`, justo
  lo que el contrato prohíbe; (b) el descriptor **no** entra en `AiRecipeIds.canonical`, así que los
  `id` `ai-<sha256>` no cambian (Favoritos/`isSaved` intactos); (c) `VERSION` no sube porque ni la
  normalización ni la plantilla cambian, solo la **fuente** del plato cuando el modelo lo emite.
- Riesgo o incógnita abierta (registrados, no bloqueantes): un `dishKey` demasiado grueso (`"pollo"`)
  haría que platos distintos compartan imagen (el contrato pide especificidad; el descriptor no se valida
  y una imagen equivocada es peor que una duplicada, anotado en `docs/risks-and-open-questions.md`);
  **entrega en runtime y persistencia del descriptor pendientes** (`AiDecodeResult.descriptors` no tiene
  consumidor: `MainActivity` sigue componiendo sin descriptor; se recomienda una feature de integración
  `dish-visual-descriptor-wiring`); al entregarlo, un plato antes cacheado por título pasará a pedirse por
  `dishKey` → un fallo de caché y una generación extra la primera vez.
- Siguiente paso: **validación independiente** de esta feature (rol `validator`, contexto limpio) y, si
  sale `accept`, el commit local del orquestador de `feature-flow`. En el plan, las fases 6-7
  (`dish-style-reference`, `dish-image-seed`, `not_started`).
- Validación independiente (rol **validator**, misma sesión 094, contexto limpio): veredicto
  **`accept`**. Re-ejecutó `bash init.sh` (exit 0; la pasada de test salió `UP-TO-DATE`, así que forzó
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` —34/34 tareas— y
  `bash scripts/gradle.sh lint --rerun-tasks`), parseó por su cuenta los XML (**86 suites / 619 tests / 0
  fallos / 0 errores / 0 saltados**) y el SARIF (**13 warnings, 0 errores, 0 bajo `app/src`**), y
  re-derivó la baseline en `HEAD` (**593** `@Test` ⇒ delta +26). Confirmó los escenarios 1-10 y los
  negativos clave (sin descriptor degrada exactamente a la fase 1; `dishKey` numérico/objeto/array/emoji/en
  blanco **nunca** descarta la receta como `SCHEMA`; `id` idéntico con y sin descriptor; `DishImageRequest`
  sin campos nuevos por reflexión; `VERSION` sigue `dish-image-v1` con independencia de `Locale`), la
  pureza de `domain.image`, la disciplina de alcance (`git diff` = lista prevista; prohibidos sin diff;
  `docs/design/concepts/dish-visual-descriptor/` **no** existe) y que no hubo commit/push/PR. Sin
  hallazgos Critical/High/Medium; 3 Low informativos no bloqueantes. Detalle en
  `docs/validations/dish-visual-descriptor.md`.
- Siguiente paso (orquestador): persistir la aceptación (hecho: `dish-visual-descriptor` pasa a
  **`accepted`** en `feature_list.json`) y crear el **commit local** (`feat: complete
  dish-visual-descriptor`); **sin push ni PR** (publica `github-flow`). En el plan, las fases 6-7
  (`dish-style-reference`, `dish-image-seed`, `not_started`); seguimiento no bloqueante recomendado: feature
  de integración `dish-visual-descriptor-wiring` para entregar el descriptor al runtime (hoy
  `AiDecodeResult.descriptors` no tiene consumidor).

### Sesión 095

- Fecha: 2026-10-13
- Objetivo: ejecutar `feature-implementer` para `dish-style-reference` (fase 6, **variante C** de
  `docs/ai-dish-images-proposal.md` §4.2: ajuste de estilo con referencia en `/images/edits`) en el
  worktree `feature/dish-style-reference`. **Sin commit, push ni PR** (el commit lo hace el orquestador
  de `feature-flow`; publica `github-flow`). `area: integration`; **gate de capturas no aplica** (**no**
  se crea `docs/design/concepts/dish-style-reference/`).
- Completado (rol **implementer**): la feature queda **`passing`** (validación independiente pendiente).
  `DishImagePolicy` gana `IMAGES_EDITS_PATH`, `EDIT_MODEL_ALIAS = "image-edit"`,
  `EDIT_FALLBACK_STATUSES = 400/404/405/413/415/422`, `editsUrl()`, `editFailureRequiresTextFallback` y
  el interruptor `STYLE_REFERENCE_ENABLED = true` (patrón `AiProviderPolicy.RESPONSE_FORMAT_ENABLED`).
  Nuevos: `DishImageStyleReference` (bytes + `mediaType` + `fileName`), `DishImageStyleReferencePolicy`
  (pura: `ASSET_BASE = img/recipes/default`, reutiliza `RecipePhotoAssets.EXTENSIONS`, `resolve`,
  `mediaTypeFor`, `fileNameFor`) y `AssetDishImageStyleReference` (lectura *best-effort* del asset,
  `null` sin lanzar; único fichero Android nuevo junto al reductor). `OpenAiImages.kt` añade
  `EDIT_REFERENCE_PART = "image"` y `editsFormData(...)` (multipart: `model`, `prompt`, `size`, `image`).
  `DevExpertDishImageGenerator` gana `styleReference`/`styleReferenceEnabled` y el flujo edits→texto:
  una petición multipart a `/images/edits`, respaldo por texto **solo** ante rechazo definitivo o carga
  `2xx` inutilizable, `null` sin respaldo ante transporte/timeout y `401/403/429/5xx`,
  `CancellationException` relanzada en ambas fases, y el camino de texto de la fase 2 extraído a un
  privado reutilizado. `MainActivity` carga la referencia y la pasa al adaptador (best-effort; `null` =
  fase 2). `domain.image`, `data.image`, `ui/**`, los assets y `RecipePhotoAssets.kt` **no** cambian;
  `DishImageCacheKey.VERSION` sigue `dish-image-v1`; sin dependencias nuevas (multipart en
  `ktor-client-core`).
- Ficheros creados: `app/src/main/java/com/sazon/app/data/ai/image/DishImageStyleReference.kt`,
  `app/src/main/java/com/sazon/app/data/ai/image/AssetDishImageStyleReference.kt`,
  `app/src/test/java/com/sazon/app/data/ai/image/DishImageStyleReferenceTest.kt` (7 tests) y
  `app/src/test/java/com/sazon/app/data/ai/image/AssetDishImageStyleReferenceTest.kt` (2 tests,
  Robolectric `@Config(sdk = [33])`, lee el asset real `default.jpg`).
- Ficheros modificados: `data/ai/image/{DishImagePolicy,OpenAiImages,DevExpertDishImageGenerator}.kt`,
  `MainActivity.kt`; tests ampliados `DishImagePolicyTest` (7→9) y `DevExpertDishImageGeneratorTest`
  (7→20); y los 3 docs durables (`docs/adr/0005-ai-dish-images.md` — fases 1-6 —,
  `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`).
- Verificación ejecutada: `bash init.sh` → **exit 0** (`./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`; sin procesos largos, sin red, sin cuota, sin tocar `init.sh`). Tirada focal
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.ai.image.*" --rerun-tasks` →
  `BUILD SUCCESSFUL` (6 suites / 46 tests). Parseo propio de los XML frescos
  (`app/build/test-results/testDebugUnitTest/TEST-*.xml`): **88 suites / 643 tests / 0 fallos / 0 errores
  / 0 saltados**. Baseline re-derivada en este `HEAD` antes de tocar tests (**86 suites / 619 tests**);
  delta **+2 suites / +24 tests** (`DishImagePolicyTest` +2, `DevExpertDishImageGeneratorTest` +13,
  `DishImageStyleReferenceTest` 7 nueva, `AssetDishImageStyleReferenceTest` 2 nueva). Lint (SARIF
  `app/build/reports/lint-results-debug.sarif`): **0 errores** y **13 warnings** preexistentes
  (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1),
  **0 bajo `app/src`**.
- Vectores concretos comprobados: sin referencia (o con el interruptor apagado o bytes vacíos) = **una**
  petición a `/images/generations` con el cuerpo de la fase 2 (`model=image`, `prompt`, `size=1024x1024`);
  con referencia = **una** petición multipart a `/images/edits` (URL, `POST`, `Authorization: Bearer`,
  `Content-Type` multipart con boundary, y en el cuerpo exactamente cuatro partes: `model=image-edit`,
  `prompt` idéntico, `size=1024x1024` y `image` con `name`/`filename="dish-style-reference.jpg"`/
  `Content-Type: image/jpeg` y los bytes de la referencia byte a byte; sin `cacheKey`/`recipeId`);
  respaldo con **exactamente dos** peticiones ante `400/404/405/413/415/422` y ante `200` inutilizable
  (JSON inválido, `{}`, `data` vacía, `b64_json` vacío/base64 inválido o reductor que devuelve `null`);
  `null` con **una** petición ante `SocketTimeoutException`/`HttpRequestTimeoutException`/`IOException` y
  ante `401/403/408/429/500`; sin cadena de un tercer intento; `apiKey` en blanco + referencia = **0**
  peticiones; `CancellationException` relanzada en la fase de ajuste y en el respaldo.
- Inspección de alcance y pureza: `import android` solo en `AndroidDishImageReducer.kt` (códec,
  `android.graphics`) y `AssetDishImageStyleReference.kt` (`android.content.Context`); **0** `Log`,
  `filesDir`, `getDatabasePath`; `git status` = solo los ficheros previstos (4 `main` + 1 `MainActivity`,
  2 tests ampliados + 2 nuevos, 3 docs, `feature_list.json`, `PROGRESS.md`); `domain/image/**`,
  `data/image/**`, `ui/**`, `app/src/main/assets/**`, build, `init.sh`, `scripts/**`, `AndroidManifest.xml`
  y `RecipePhotoAssets.kt` **sin diff**; `DishImageCacheKey.VERSION` sigue `dish-image-v1`;
  `docs/design/concepts/dish-style-reference` **no** existe; sin dependencias nuevas.
- Hallazgo (no bloqueante, transparencia): el grep del plan `"Recipe|Taxonomy|DishImagePrompt|
  DishImageCacheKey" → 0` delata 4 coincidencias de `Recipe` que son la **convención de assets
  `RecipePhotoAssets`** (`import`, `ASSET_BASE = "${RecipePhotoAssets.ROOT}/...")`, `EXTENSIONS =` y una
  referencia en KDoc) — precisamente la **reutilización que el propio plan manda** en su Apartado 3 —;
  **no** hay ningún tipo del dominio `Recipe`/`Taxonomy`, ni `DishImagePrompt` ni `DishImageCacheKey`.
  Se registra porque la expectativa "0" del plan es imprecisa con su propio diseño.
- Riesgos/incógnitas registrados (no bloqueantes): soporte real de `/images/edits` por el proveedor
  (alias `image-edit`, multipart, `1024x1024`, `b64_json`) no verificado en código, mitigado por el
  respaldo por texto, el interruptor `STYLE_REFERENCE_ENABLED` y la prueba real **opcional** (~2 % de
  cuota, **no ejecutada**: no es puerta de aceptación); si el proveedor rechazara siempre el ajuste, cada
  imagen costaría una llamada desechada (≤2 por `generate`, una unidad del tope por sesión); estilo mixto
  transitorio de la caché mientras la LRU no evicta las imágenes previas (Decisión 3, `VERSION` no sube).
- Completado (rol **validator**, contexto limpio): la feature queda **`accepted`**. Veredicto
  **`accept`**. Re-ejecutó `bash init.sh` (exit 0; al salir `UP-TO-DATE` forzó
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` 4m09s y `bash scripts/gradle.sh lint
  --rerun-tasks` 3m14s, ambos `BUILD SUCCESSFUL`) y parseó por su cuenta los XML (**88 suites / 643
  tests / 0 fallos / 0 errores / 0 saltados**) y el SARIF (**0 errores, 13 warnings** preexistentes, 0
  bajo `app/src`); re-derivó la baseline (**86 suites / 619 tests**) ⇒ **+2 suites / +24 tests**.
  Confirmó los 13 escenarios, el contrato `/images/edits` (multipart de 4 partes, respaldo con
  exactamente 2 peticiones, `null` sin respaldo ante transporte/timeout y `401/403/429/5xx`, sin tercer
  intento, clave en blanco = 0 peticiones, `CancellationException` relanzada), la referencia como asset
  versionado `img/recipes/default.jpg` sin diff de assets, la pureza/alcance y la ausencia de
  commit/push/PR. Sin hallazgos Critical/High/Medium; **1 Low** informativo no bloqueante (el grep de
  alcance de la spec `Recipe|… → 0` es contradictorio con su propio diseño que manda reutilizar
  `RecipePhotoAssets`; imprecisión de redacción de la spec, no del código). Detalle en
  `docs/validations/dish-style-reference.md`.
- Aceptación persistida por el orquestador de `feature-flow`: `feature_list.json` pasa a `accepted` con
  la evidencia del validador y se crea el commit local de la rama `feature/dish-style-reference`
  (**sin push ni PR**; publica `github-flow`).
- Siguiente paso: en el plan queda la fase 7 (`dish-image-seed`) y, como seguimiento no bloqueante
  previo, `dish-visual-descriptor-wiring`. La publicación del PR de `dish-style-reference` la hace el
  orquestador de `github-flow`.

### Sesión 096

- Fecha: 2026-10-13
- Objetivo: ejecutar `feature-implementer` para `dish-image-seed` (fase **7**, la **última** de
  `docs/ai-dish-images-proposal.md` §5: set semilla de imágenes de platos frecuentes) en el worktree
  `feature/dish-image-seed`. **Sin commit, push ni PR** (el commit lo hace el orquestador de
  `feature-flow`; publica `github-flow`). `area: data`; **gate de capturas no aplica** (**no** se crea
  `docs/design/concepts/dish-image-seed/`). Modo del script de preparación: **proveedor** (el indicado
  por la spec por defecto).
- Completado (rol **implementer**): la feature queda **`passing`** (validación independiente pendiente).
  `DishImageResolver` gana un parámetro **nuevo al final con default** (`seed: DishImageSeedSource? =
  null`) y un nivel nuevo entre el disco y el generador (**memoria → disco → semilla → generador →
  persistir**): el acierto de semilla se guarda en **memoria** y se devuelve **sin** escribir en disco y
  **sin** invocar al generador; el **disco gana a la semilla**; `null`/array vacío/excepción de la fuente
  degradan a `null` y `CancellationException` se relanza; con `seed = null` el comportamiento es el de la
  fase 3. Nuevos: `data.DishImageSeedAssets` (mapa **puro** clave→asset: `ROOT = "img/dishes/seed"`,
  `EXTENSIONS` de `RecipePhotoAssets`, `SAFE_KEY = [a-z0-9-]+`, `isSafeKey`, `path`, `resolve(cacheKey,
  exists, root = ROOT)` con `exists` inyectado), `data.DishImageSeedSource` (seam `fun interface`,
  `suspend bytesFor`) y `data.AssetDishImageSeedSource` (costura Android: `Dispatchers.IO`, `null` sin
  lanzar; **único** fichero nuevo con `android.*`). `MainActivity` gana una línea
  (`seed = AssetDishImageSeedSource(applicationContext)`) y `app/build.gradle.kts` publica
  `dish.image.seed.assets.dir` a los tests JVM (mismo patrón que `recipe.photos.assets.dir`).
  `scripts/prepare-dish-image-seed.sh` (**nuevo**, herramienta de una sola vez, **fuera** de `init.sh` y
  de runtime): lista autoritativa ordenada de 6 claves canónicas con plato y cocina, prompt **espejo** de
  `DishImagePrompt` con la etiqueta de cocina leída de `docs/domain/taxonomy.json`, modo proveedor
  (`POST /images/edits` multipart con `model=image-edit`, la referencia neutra
  `img/recipes/default.<ext>`, `size=1024x1024`; respaldo **acotado por estado** a
  `/images/generations` solo ante `400/404/405/413/415/422` o `2xx` inutilizable; transporte/timeout y
  `401/403/429/5xx` fallan el plato **sin** respaldo), modo `--source <dir>` (**sin cuota**), *magic
  bytes*, conversión a WebP q80 ≤960 px cuando hay herramienta, inventario y presupuesto **≤ 2 MiB** con
  `exit ≠ 0` ante cualquier desvío.
- Ficheros creados: `app/src/main/java/com/sazon/app/data/{DishImageSeedAssets,DishImageSeedSource,
  AssetDishImageSeedSource}.kt`, `scripts/prepare-dish-image-seed.sh`,
  `app/src/main/assets/img/dishes/seed/*.webp` (6), `app/src/test/java/com/sazon/app/data/
  {DishImageSeedAssetsTest,AssetDishImageSeedSourceTest}.kt` y este registro de sesión.
- Ficheros modificados: `data/image/DishImageResolver.kt`, `MainActivity.kt` (una línea + import),
  `app/build.gradle.kts` (directorio + `systemProperty`), `DishImageResolverTest.kt` (12 → 19 tests) y los
  4 docs durables (`docs/adr/0005-ai-dish-images.md` — fases 1-7, lista de fases **completa** —,
  `docs/technical-discovery.md` — fila nueva en Data and Storage + bullet de la fase 7 + bullet de tests
  —, `docs/domain/taxonomy.md` §6 y `docs/risks-and-open-questions.md`).
- Preparación ejecutada **una sola vez** (modo proveedor, con la clave de `local.properties` que nunca se
  imprime): 6 peticiones OK a `/images/edits` (sin respaldos), conversión con `cwebp` a WebP q80 960×960.
  **Coste: ~12 % de la cuota semanal** (6 × ~2 %), gastado una vez; en runtime esos platos no gastan cuota
  ni esperan red.
- Verificación ejecutada: `bash init.sh` → **exit 0** (`./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`; sin procesos largos, sin red, sin cuota, sin tocar `init.sh`). Tirada focal
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.DishImageSeedAssetsTest" --tests
  "com.sazon.app.data.AssetDishImageSeedSourceTest" --tests "com.sazon.app.data.image.*"` →
  `BUILD SUCCESSFUL` (**9 suites / 74 tests / 0 fallos**). Parseo propio de los XML frescos
  (`app/build/test-results/testDebugUnitTest/TEST-*.xml`): **90 suites / 660 tests / 0 fallos / 0 errores
  / 0 saltados**. Baseline re-derivada en este `HEAD` antes de tocar tests (**88 suites / 643 tests** =
  88 ficheros con `@Test` / 643 anotaciones, la misma última baseline aceptada); delta **+2 suites / +17
  tests** (`DishImageSeedAssetsTest` 6 nueva, `AssetDishImageSeedSourceTest` 4 nueva,
  `DishImageResolverTest` 12 → 19). Lint (SARIF `app/build/reports/lint-results-debug.sarif`):
  **0 errores** y **13 warnings** preexistentes (`GradleDependency` 7, `NewerVersionAvailable` 3,
  `AndroidGradlePluginVersion` 2, `OldTargetApi` 1), **0 bajo `app/src`**.
- Assets e inventario: 6 ficheros `assets/img/dishes/seed/<clave canónica>.webp` (WebP, 960×960,
  `webpinfo`), total **449.844 B (439 KiB ≤ 2 MiB)**, ninguno vacío, **no** ignorados por git
  (`git check-ignore` → no) y presentes en el APK de debug (`bash scripts/gradle.sh assembleDebug` +
  `unzip -l`: 6 entradas `assets/img/dishes/seed/*.webp`). Revisión visual de las 6 fotos: cada una
  representa su plato con la dirección ya aceptada (cerámica, luz natural, estilo editorial).
- Vectores concretos comprobados (JVM): `DishImageCacheKey.of("Arroz con pollo criollo") ==
  "dish-image-v1-arroz-con-pollo-criollo"` resuelve `img/dishes/seed/<clave>.<ext>` existente y no vacío;
  orden `webp → jpg → png`; claves inseguras (`""`, `"   "`, mayúsculas, `../etc/passwd`, `dish/images`,
  NUL…) → `path`/`resolve` `null` (nunca una ruta fuera de `ROOT`); clave ajena al set → `null`; semilla
  → **0** llamadas al generador y **0** ficheros en la raíz de la caché; la segunda resolución lee el
  asset **1** sola vez; el disco gana a la semilla; `seed = null` reproduce la fase 3; `null`/vacío/
  excepción degradan y `CancellationException` se relanza; Robolectric (`@Config(sdk = [33])`): asset real
  con cabecera de imagen, clave desconocida/insegura y `root` inexistente → `null` sin lanzar.
- Script (escenario 11): segunda pasada **sin** `--force` → omite los 6 (`= <clave> (ya empaquetado, se
  omite)`, exit 0, sin red ni cuota); `--source` con un directorio **incompleto** → `exit 1` **sin
  escribir nada** (md5 de los 6 assets intactos); `--source` con el set completo → `exit 0` y bytes
  idénticos; un asset **ajeno** a la lista → `exit 1`; `grep "prepare-dish-image-seed" init.sh` → **0**.
- Inspección de alcance y pureza: `git status`/`git diff` solo con los ficheros previstos (3 nuevos en
  `data/`, `DishImageResolver.kt`, `MainActivity.kt`, `app/build.gradle.kts`, 2 tests nuevos + 1
  ampliado, 4 docs, `feature_list.json`, `PROGRESS.md`, la spec, el script y los assets);
  `domain/image/**`, `data/ai/image/**`, la caché de la fase 3, `ui/**`, `data/RecipePhotoAssets.kt`,
  `data/AssetRecipePhotoSource.kt`, `init.sh`, `scripts/fetch-recipe-photos.sh`, `gradle/libs.versions.toml`,
  `AndroidManifest.xml`, `docs/domain/taxonomy.json`, `seed-recipes.json`, `DESIGN.md` y `docs/design/**`
  **sin diff**; `import android` en `data` solo en las costuras ya existentes + `AssetDishImageSeedSource`
  (en `data/image` sigue **solo** `AndroidDishImageCache.kt`); `test -d docs/design/concepts/dish-image-seed`
  → **no existe**; `DishImageCacheKey.VERSION` sigue `dish-image-v1`; **sin** dependencias nuevas.
- Hallazgo (no bloqueante, transparencia): el grep del plan `"prepare-dish-image-seed en app/src" → 0` da
  **1** coincidencia, que es la **mención del script en el KDoc** de `DishImageSeedAssets` (mismo patrón ya
  aceptado en `RecipePhotoAssets.kt`, que cita `fetch-recipe-photos.sh`); **no** hay ninguna invocación en
  runtime.
- Riesgos/incógnitas registrados (no bloqueantes): la **lista concreta** de platos frecuentes (hoy 6) y su
  calidad visual se revisan en la prueba manual de la persona (se corrige con `--force` o una feature
  correctiva); el **espejo en bash** del prompt y del wire (deriva cosmética, acotada por la regla de
  `VERSION`: subirla cambia las claves y obliga a **volver a preparar el set**); un plato del set cuya
  clave ya tuviera copia generada en disco se sirve del disco (decisión 2, cubierta por test); con el
  interruptor apagado **no** se ven las semilla (semántica aceptada de la fase 4).
- Completado (rol **validator**, contexto limpio): **veredicto `accept`** (sesión 096). Re-ejecutó
  `bash init.sh` (exit 0; al salir `testDebugUnitTest`/`lint` `UP-TO-DATE` forzó
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` — 3m52s, 34 tareas — y
  `bash scripts/gradle.sh lint --rerun-tasks` — 3m01s, 30 tareas —, ambos `BUILD SUCCESSFUL`) y parseó
  por su cuenta los XML frescos (**90 suites / 660 tests / 0 fallos / 0 errores / 0 saltados**) y el
  SARIF (**0 errores, 13 warnings** preexistentes, **0 bajo `app/src`**); re-derivó la baseline
  (`git grep -c '@Test' HEAD` = 88 ficheros / 643) ⇒ **+2 suites / +17 tests**. Confirmó los 12
  escenarios, los 6 assets WebP reales (960×960, 449.844 B, md5 del APK == md5 del árbol, no ignorados,
  sin duplicar una foto existente), el nivel semilla (0 llamadas/0 ficheros en disco, acierto de memoria,
  disco gana a semilla, `seed = null` = fase 3, `CancellationException` relanzada) **detrás del mismo
  gate** (no consume tope) y la disciplina de alcance/pureza (`domain/**`, `data/ai/image/**`, la caché
  de la fase 3, `ui/**`, la cadena de `recipe-photos`, `init.sh`, el version catalog, el manifest, la
  Taxonomía, `seed-recipes.json`, `DESIGN.md` y `docs/design/**` sin diff; sin dependencias nuevas;
  `docs/design/concepts/dish-image-seed/` **no existe**; `VERSION` sigue `dish-image-v1`; único fichero
  nuevo con `android.*` = `AssetDishImageSeedSource.kt`). **Sin** hallazgos Critical/High/Medium; **1 Low
  informativo no bloqueante** (una frase de `docs/technical-discovery.md` dice que el script falla "sin
  empaquetar nada" cuando escribe incrementalmente y falla después en la validación; el contrato de la
  spec se cumple — exit ≠ 0 y **nunca** parcial silencioso —; anotado para corregir la redacción).
  Detalle en `docs/validations/dish-image-seed.md`. El orquestador de `feature-flow` **persiste la
  aceptación** (`feature_list.json` → `accepted` con la evidencia del validador y este registro) y crea
  el **commit local**; **sin** push ni PR (publica `github-flow`).
- Siguiente paso: el orquestador de `github-flow` publica la feature (`Closes #<n>`, **sin** merge). La
  fase 7 cierra el plan de `docs/ai-dish-images-proposal.md` (fases 1-7 **completas**). Seguimientos no
  bloqueantes: (a) corregir la frase del hallazgo Low en `docs/technical-discovery.md`; (b) la prueba
  manual de la persona en emulador (abrir un plato del set sin red y comprobar su foto propia, y que con
  "Imágenes del plato" apagado se ve la foto local); (c) `dish-visual-descriptor-wiring` (entrega en
  runtime del descriptor de la fase 5).

### Sesión 097

- Fecha: 2026-10-13
- Objetivo: ejecutar `feature-implementer` para **`dish-visual-descriptor-wiring`** (`area: integration`,
  `depends_on: dish-visual-descriptor` ya `accepted`): la **entrega en runtime** del descriptor visual
  de la fase 5, en el worktree `feature/dish-visual-descriptor-wiring`. **Sin commit, push ni PR** (el
  commit lo hace el orquestador de `feature-flow`; publica `github-flow`). **Gate de capturas no aplica**
  (**no** se crea `docs/design/concepts/dish-visual-descriptor-wiring/`).
- Completado (rol **implementer**): la feature queda **`passing`** (validación independiente pendiente).
  - **Almacén de sesión** (`data/image/DishVisualDescriptorStore.kt`, **nuevo**, puro JDK: solo
    `ConcurrentHashMap` y `DishVisualDescriptor`; **0** `import android`): `record(Map)` ignora ids en
    blanco y descriptores `isEmpty`/`NONE`, con el mismo `id` **gana el último**; `descriptorFor(id)`
    devuelve `null` para `id` nulo/en blanco/desconocido; sin estado compartido entre instancias ni
    persistencia (efímero: se pierde al recrear la `Activity`).
  - **Costura del adaptador** (`DevExpertRecipeGenerator.kt`): parámetro **nuevo al final con default**
    `onDescriptors: (Map<String, DishVisualDescriptor>) -> Unit = {}`, invocado con
    `result.descriptors` tras `report(...)` y antes de `return result.accepted` **solo** en la rama
    `AttemptOutcome.Success` (una vez por decode exitoso, nunca en fallo/degradación/cancelación). El
    tipo del callback es público, así que `AiDecodeResult` (`internal`) no entra en la firma.
  - **Cableado** (`MainActivity.kt`): `val dishVisualDescriptors = DishVisualDescriptorStore()` antes
    del generador, `onDescriptors = dishVisualDescriptors::record` (el generador es compartido por
    Lote/Generador/Búsqueda) y `requestFor = { DishImageRequest.of(taxonomy, recipe,
    dishVisualDescriptors.descriptorFor(recipe.id)) }`.
- Ficheros creados: `app/src/main/java/com/sazon/app/data/image/DishVisualDescriptorStore.kt`,
  `app/src/test/java/com/sazon/app/data/image/DishVisualDescriptorStoreTest.kt` y
  `app/src/test/java/com/sazon/app/data/ai/DishVisualDescriptorWiringTest.kt`.
- Ficheros modificados: `data/ai/DevExpertRecipeGenerator.kt`, `MainActivity.kt`,
  `DevExpertRecipeGeneratorTest.kt` (3 tests nuevos; helper `provider` con `onDescriptors`), los 3 docs
  durables (`docs/technical-discovery.md`, `docs/adr/0005-ai-dish-images.md`,
  `docs/risks-and-open-questions.md`), `feature_list.json` y este `PROGRESS.md`.
- Verificación ejecutada: `bash init.sh` → **exit 0** (`./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`; sin procesos largos, sin red, sin cuota, sin tocar `init.sh`). Tirada focal
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.image.*" --tests
  "com.sazon.app.data.ai.*"` → `BUILD SUCCESSFUL` (**22 suites / 215 tests / 0 fallos**). Parseo propio
  de los XML frescos (`app/build/test-results/testDebugUnitTest/TEST-*.xml`): **92 suites / 673 tests /
  0 fallos / 0 errores / 0 saltados**. Baseline re-derivada en este `HEAD` antes de tocar tests (**90
  suites / 660 tests** = 90 ficheros con `@Test` / 660 anotaciones); delta **+2 suites / +13 tests**
  (`DishVisualDescriptorStoreTest` 5, `DishVisualDescriptorWiringTest` 5, `DevExpertRecipeGeneratorTest`
  35 → 38). Lint (SARIF): **0 errores** y **13 warnings** preexistentes (`GradleDependency` 7,
  `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1), **0 bajo `app/src`**.
- Escenarios 1-6 cubiertos por test: (1) `onDescriptors` con el mapa del decode por `id` generado,
  vacío sin descriptor y nunca en fallo/401, 200 sin recetas (mapa vacío) ni cancelación, con default
  no-op (los tests existentes no cambian); (2) decode → `record` → `DishImageRequest.of(taxonomy,
  recipe, descriptorFor(recipe.id))` con `cacheKey == DishImageCacheKey.of(recipe, descriptor)`
  (`dish-image-v1-arroz-con-pollo-criollo`) y `prompt == DishImagePrompt.build(...)` con el
  `imagePrompt`; (3) `"Arroz con pollo"` y `"Arroz con pollo fácil"` con el **mismo**
  `dishKey = "arroz-con-pollo"` → ambas `dish-image-v1-arroz-con-pollo`, frente a la clave por título
  de la segunda (`dish-image-v1-arroz-con-pollo-facil`); (4) sin descriptor (semilla/restaurada/`id` en
  blanco o nulo) la composición es **exactamente** `DishImageRequest.of(taxonomy, recipe)`; (5) el
  almacén ignora entradas vacías y el último gana; (6) alcance verificado por inspección.
- Inspección de pureza y alcance: `grep -c "import android"` en el almacén → **0** (solo
  `DishVisualDescriptor` y `ConcurrentHashMap`); `git status`/`git diff` solo con los ficheros
  previstos; `Recipe.kt`, `RecipeValidator.kt`, `AiRecipeIds.kt`, `AiRecipeDraft.kt`,
  `AiRecipePayload.kt`, `AiDecodeResult.kt`, `domain/image/**`, `data/ai/image/**`, `data/image/**`
  (salvo el almacén nuevo), `ui/**`, `SazonApp.kt`, `MainShell.kt`, `app/build.gradle.kts`,
  `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `docs/domain/taxonomy.json`,
  `seed-recipes.json`, `DESIGN.md` y `docs/design/**` **sin diff**;
  `test -d docs/design/concepts/dish-visual-descriptor-wiring` → **no existe**;
  `DishImageCacheKey.VERSION` sigue `dish-image-v1`; **sin** dependencias nuevas.
- Docs durables actualizadas: `docs/technical-discovery.md` (el descriptor **ya tiene consumidor en
  runtime**: `onDescriptors` + `DishVisualDescriptorStore` + `descriptorFor(recipe.id)`; bullet de
  tests nuevos; sustituidas las frases "nadie lo consume todavía"), `docs/adr/0005-ai-dish-images.md`
  (cerrada la "entrega en runtime pendiente"; sigue **Aceptado parcialmente (fases 1-7)**; el descriptor
  sigue sin persistirse ni validarse y `VERSION` sigue `dish-image-v1`) y
  `docs/risks-and-open-questions.md` (fila del `dishKey`: entregada en runtime + **limitación viva**:
  efímero por sesión, una receta restaurada del Lote o un Favorito se resuelve por título hasta la
  siguiente llamada de IA; consecuencia de caché ya aplicable; ficha del ADR 0005 actualizada).
- Riesgos/incógnitas registrados (no bloqueantes): el descriptor es **efímero por sesión** (no se
  persiste); **fallo de caché al cambiar de clave** (título → `dishKey`): una generación extra la
  primera vez y una entrada antigua como lastre hasta que la LRU la evicte; **carrera benigna** (una
  petición compuesta por título antes de que el decode registre el descriptor); un `dishKey` grueso
  sigue siendo riesgo aceptado.
- Validación independiente (rol **validator**, **sesión 097**, contexto limpio): veredicto **`accept`**.
  Re-ejecutó `bash init.sh` (**exit 0**; al salir `test` `UP-TO-DATE` forzó
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` → **34/34 tareas**, 5m11s,
  `BUILD SUCCESSFUL`, y `bash scripts/gradle.sh lint --rerun-tasks` → **30/30 tareas**, 4m51s,
  `BUILD SUCCESSFUL`) y parseó por su cuenta los XML frescos (**92 suites / 673 tests / 0 fallos /
  0 errores / 0 saltados**) y el SARIF (**13 warnings, 0 errores, 0 bajo `app/src`**); re-derivó la
  baseline en `HEAD` (**90 ficheros / 660 `@Test`**) ⇒ delta **+2 suites / +13 tests**. Confirmó los
  escenarios 1-6, la pureza del almacén (**0** `import android`), la disciplina de alcance (`git diff`
  = lista prevista; `Recipe.kt`, `RecipeValidator.kt`, `AiRecipeIds.kt`, `AiRecipeDraft.kt`,
  `AiRecipePayload.kt`, `AiDecodeResult.kt`, `domain/image/**`, `data/ai/image/**`, `data/image/**`
  salvo el almacén, `ui/**`, build, version catalog, `init.sh`, `scripts/**`, Taxonomía y
  `seed-recipes.json` sin diff; `VERSION` sigue `dish-image-v1`;
  `docs/design/concepts/dish-visual-descriptor-wiring` **no existe**) y que **no** hay commit/push/PR
  (`HEAD == origin/main` `c038690`, 0 commits por delante, sin rama remota ni PR). **Sin** hallazgos
  Critical/High/Medium; **2 Low** informativos no bloqueantes (el KDoc de `AiDecodeResult.kt:44` aún
  dice que nadie consume el descriptor —corregirlo está **prohibido** por esta spec—; el bullet de capa
  `data.image` de `docs/technical-discovery.md` no menciona el almacén). Detalle en
  `docs/validations/dish-visual-descriptor-wiring.md`.
- Siguiente paso: el orquestador de `feature-flow` **persiste la aceptación**
  (`feature_list.json` → `accepted` con la evidencia del validador y este registro) y crea el **commit
  local**; **sin** push ni PR (publica `github-flow`). Prueba manual opcional de la persona (no es
  puerta, no automatizable desde WSL): en el emulador, generar dos variantes del mismo plato y comprobar
  que la segunda no vuelve a llamar a `/images/generations` (misma clave).

### Sesión 098

- Fecha: 2026-10-13
- Objetivo: ejecutar `feature-implementer` para **`home-suggestions-personalized`** (`area: ui`,
  `depends_on: home-offline-blocks`, `suggestion-batch-cache` y `results-screen`, las tres `accepted`),
  la **correctiva** del carrusel "Sugeridas para ti", en el worktree
  `feature/home-suggestions-personalized`. **Sin commit, push ni PR** (el commit lo hace el orquestador
  de `feature-flow`; publica `github-flow`). **Gate de capturas exento por autorización de la persona**
  (mismo diseño visual; precedentes #77/#85/#86/#91/#98): **no** se crea
  `docs/design/concepts/home-suggestions-personalized/`.
- Completado (rol **implementer**): la feature queda **`passing`** (validación independiente pendiente).
  El carrusel pinta **como máximo 6** recetas **seguras** y **ordenadas por relevancia con el Perfil**.
  - `ui/home/HomeSuggestions.visible(batchRecipes, seedRecipes, preferences, taxonomy)` (logica **pura**,
    sin Compose/Android) con `MAX_VISIBLE = 6`: `batchRecipes ?: seedRecipes` → `RecipeConstraints`
    inline (`dietPatternId` + `hardRestrictionIds.toSet()`, mismo mapeo de 2 lineas que
    `SuggestionBatchService`) → **filtro** `RecipeValidator.validateRecipe(...) is Accepted` (semilla y
    `ai`; se conserva la receta original, no la normalizada) → **ranking** decorado por `MatchScore.of`
    (`sortedByDescending`, estable en empates, score calculado una sola vez) → `take(MAX_VISIBLE)`.
    **Sin** `copy(matchScore = ...)`: se devuelven las mismas instancias, asi que `RecipeCard` no pinta
    su pildora y el aspecto no cambia. Un Lote resuelto **vacio** sigue vacio (no cae a semilla). KDoc
    actualizado.
  - Cableado en `ui/MainShell.kt`: `remember(batch?.recipes, seedRecipes, profile.preferences, taxonomy)`
    envolviendo la llamada (`import androidx.compose.runtime.remember`, ya presente); `HomeScreen` y su
    firma no cambian.
- Ficheros modificados: `app/src/main/java/com/sazon/app/ui/home/HomeSuggestions.kt`,
  `app/src/main/java/com/sazon/app/ui/MainShell.kt`,
  `app/src/test/java/com/sazon/app/ui/home/HomeSuggestionsTest.kt`, `docs/technical-discovery.md`,
  `docs/risks-and-open-questions.md`, `DESIGN.md`, `feature_list.json` y este `PROGRESS.md`.
- Verificación ejecutada: `bash init.sh` → **exit 0** (`./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`; sin procesos largos, sin tocar `init.sh`). Parseo propio de los XML frescos
  (`app/build/test-results/testDebugUnitTest/TEST-*.xml`): **92 suites / 683 tests / 0 fallos /
  0 errores / 0 saltados**. Baseline re-derivada en este `HEAD`
  (`git grep -c "@Test" HEAD -- 'app/src/test/**'` = **92 ficheros / 673 anotaciones**) ⇒ delta
  **+10 tests** (mismo numero de suites; `HomeSuggestionsTest` 3 → 13). Lint (SARIF): **0 errores** y
  **13 warnings** preexistentes (`GradleDependency` 7, `NewerVersionAvailable` 3,
  `AndroidGradlePluginVersion` 2, `OldTargetApi` 1), **0 bajo `app/src`**.
- Escenarios cubiertos por test (`HomeSuggestionsTest`, 13): (1) tope — 8 seguras → 6 conservando el
  orden de origen, 4 → 4 sin recorte; (2) orden por `MatchScore` descendente con el input en orden
  inverso y **empates estables** (los tres puntuan igual); (3) **caso negativo obligatorio** — con
  `dietPatternId = "vegetarian"` se descartan la **semilla** y la **`ai`** con `pollo` mientras la
  segura sigue, y con `hardRestrictionIds = ["gluten"]` se descarta el "Pan" libre (`ingredientId =
  null`); en ambos el **control positivo** con perfil `omnivore` sin restricciones **sí** muestra las
  descartadas (no pasa en vacío); (4) Lote `emptyList()` → vacío; (5) respaldo `null` con el catálogo
  semilla real → 6, y con `gluten` → ≤ 6 sin `seed-pasta-tomate` ni ninguna de `SEMILLAS_CON_GLUTEN`;
  (6) `dislikedIngredientIds` **penaliza y no descarta** (ambas aparecen, la penalizada despues); (7)
  **sin materialización** — la receta devuelta es la **misma instancia** (`assertSame`) con
  `matchScore == null`.
- Inspección de pureza y alcance: `ui.home.HomeSuggestions` **0** imports de Compose/Android y **usa**
  `MatchScore` y `RecipeValidator` (grep); `git diff --stat` solo con los ficheros previstos (código +
  3 docs durables + harness); `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`,
  `scripts/**`, `domain/**`, `data/**`, `ui/results/**`, `ui/home/HomeScreen.kt`, `RecipeCard.kt`, la
  Taxonomía, `seed-recipes.json` y `docs/design/**` **sin diff**;
  `test -d docs/design/concepts/home-suggestions-personalized` → **no existe**; **sin** dependencias
  nuevas.
- Docs durables actualizadas: `docs/technical-discovery.md` (el carrusel ya **no** "consume el Lote tal
  cual": filtro por `RecipeValidator` + orden por `MatchScore` sin materializar + tope 6, tambien en el
  respaldo de semilla), `docs/risks-and-open-questions.md` (**deuda de seguridad** del modo degradado de
  Resultados: `ResultsStatus.Unavailable` sigue puntuando/ordenando **sin filtrar**; los fixtures
  incoherentes de `ResultsStatusTest` impiden filtrar sin vaciarlo → correctiva recomendada p. ej.
  `results-seed-safety-filter`) y `DESIGN.md` (excepcion de capturas de la feature + comportamiento
  vigente del carrusel ≤ 6 sin pildora, y nota en "Core Screens").
- Incógnitas resueltas: (1) el filtro de seguridad se aplica **solo en la Home** (no se toca
  `ResultsStatus.Unavailable`; deuda registrada); (2) el respaldo de semilla offline (`null`) pasa por el
  **mismo camino** (filtro → orden → tope) y un Lote resuelto vacío **no** cae a semilla.
- Riesgos/incógnitas registrados (no bloqueantes): nº de seguras < 6 con perfiles restrictivos (p. ej.
  vegano + gluten) → carrusel de 4-5 tarjetas (comportamiento correcto "como máximo 6"; `seed-catalog-50`
  dará variedad); coste del re-validado en recomposición mitigado con `remember`; heurística/pesos de
  `MatchScore` y empates revisables con correctiva sin cambiar el contrato.
- Pendiente para la persona (no automatizable, AGENTS.md no automatiza UI): prueba manual en emulador —
  con Lote resuelto y con respaldo de semilla offline, el carrusel muestra ≤ 6 tarjetas acordes al Perfil
  (sin alérgenos ni patrón de dieta incumplidos) y las mejor puntuadas primero, sin cambio de aspecto
  (título "Sugeridas para ti" + "basado en tus gustos", tarjetas e imágenes iguales, sin píldora), en
  claro y oscuro.
- Siguiente paso: validación independiente (`feature-validator`) sobre el worktree; después el
  orquestador de `feature-flow` persiste la aceptación y crea el commit local (**sin** push ni PR;
  publica `github-flow`).

### Sesión 099

- Fecha: 2026-10-13
- Objetivo: **orquestación `feature-flow`** de `home-suggestions-personalized` (issue #110, `area: ui`):
  implementación → validación independiente → **aceptación** → **commit local** (sin push ni PR; publica
  `github-flow`).
- Completado:
  - El **implementer** (sesión 098, subagente con contexto limpio) dejó la feature **`passing`**:
    `HomeSuggestions.visible(...)` puro con `MAX_VISIBLE = 6` (filtro por `RecipeValidator` → ranking por
    `MatchScore` sin materializar → `take`), cableado en `MainShell` con `remember`, `HomeSuggestionsTest`
    de 3 a 13 casos, y docs durables (`docs/technical-discovery.md`, `docs/risks-and-open-questions.md`,
    `DESIGN.md`). Sin commit/push/PR.
  - El **validator** (sesión 099, subagente con contexto limpio) re-ejecutó el gate y devolvió **`accept`**:
    `bash init.sh` → exit 0 en la segunda pasada (la primera falló por un **flake de DrvFs**, no por un
    test) con `testDebugUnitTest --rerun-tasks` (34/34 tareas) y `lint --rerun-tasks` (30/30 tareas);
    parseo propio de los XML = **92 suites / 683 tests / 0 fallos / 0 errores / 0 saltados**
    (`HomeSuggestionsTest` 13) y del SARIF = **0 errores, 13 warnings preexistentes**, 0 bajo `app/src`.
    Resolvió la discrepancia de baseline a favor del implementer (`HEAD` = 92 ficheros / 673 anotaciones
    `@Test` → delta **+10 tests / 0 suites**; el "90/660" de la spec era previo a
    `dish-visual-descriptor-wiring`) y verificó los 7 escenarios (negativos no vacíos, `emptyList() !=
    null`, re-derivación del set real de semillas con gluten, pureza, alcance, exención de capturas y la
    deuda de Resultados **sin** arreglarse fuera de alcance). Sin hallazgos Critical/High/Medium; 3 Low
    informativos no bloqueantes.
  - **Aceptación persistida** por el orquestador: `feature_list.json` → `accepted` (+ evidencia de la
    validación independiente) y registro en `docs/validations/home-suggestions-personalized.md`.
  - **L1 (Low)** del validador corregido aquí: el resumen "Estado verificado actual" ya refleja la
    feature `accepted` con 92/683 y su prueba manual pendiente.
- Verificación ejecutada: la del validador independiente (arriba); el orquestador **no** re-implementó ni
  re-validó (regla de `feature-flow`) y solo persiste la aceptación y crea el commit local.
- Ficheros: código de la feature (sesión 098) + `docs/validations/home-suggestions-personalized.md`
  (nuevo) + `feature_list.json` y `PROGRESS.md` (aceptación).
- Pendiente para la persona (no automatizable, AGENTS.md no automatiza UI): prueba manual en emulador —
  con Lote resuelto y con respaldo de semilla offline, el carrusel muestra ≤ 6 tarjetas acordes al Perfil
  (sin alérgenos ni patrón de dieta incumplidos) y las mejor puntuadas primero, sin cambio de aspecto, en
  claro y oscuro.
- Siguiente paso: **`github-flow`** publica la rama `feature/home-suggestions-personalized` y abre/reutiliza
  su PR (título `[home-suggestions-personalized] …`, cuerpo con `Closes #110`). Después queda
  `seed-catalog-50` (`not_started`, `area: data`, depende de esta) — con capturas/exención a confirmar.

### Sesión 100

- Fecha: 2026-10-13
- Objetivo: implementar `dish-image-prompt-v2` (issue #111, `area: integration`) según
  `docs/specs/dish-image-prompt-v2.md`: prompt de imagen v2 + bump de `DishImageCacheKey.VERSION` con el
  renombrado de los assets semilla + diagnóstico del origen (solo debug). Rama
  `feature/dish-image-prompt-v2`; solo hasta implementación y autoverificación (`passing`), **sin**
  commit/push/PR (los hace el orquestador de `feature-flow` / `github-flow`).
- Completado:
  - **`DishImagePrompt` v2** (`domain.image`): la identidad es **siempre** el título (degradación
    `título → etiqueta real de categoría → plato casero`, sin cambios); el `imagePrompt` del descriptor
    pasa a ser el **detalle** (cambio deliberado respecto a la fase 5, que lo usaba como sustituto);
    constantes nuevas `COMPOSITION` (`una ración completa del plato en primer plano, sobre fondo neutro`),
    `STYLE_SUFFIX` (sin la parte de negativos) y `NEGATIVES` (`sin texto, sin manos, sin personas, sin
    cubiertos ni marcas de agua, sin collage ni varios platos, sin fondo recargado`), más
    `MAX_KEY_INGREDIENTS = 5`; **ingredientes clave** = hasta 5 nombres no-despensa, distintos y en el
    orden de la receta, unidos por `", "`; el ensamblado une segmentos no vacíos con `", "` y colapsa
    las rachas de comas (nunca `,,` ni espacios dobles). Nota de interpretación: la spec ilustra la
    plantilla con `"<identidad>[, <cocina>]: <detalle>, …"` pero su regla normativa de ensamblado une
    segmentos no vacíos con `", "`; se implementó la regla (sin `:`) y se documenta aquí por
    transparencia.
  - **Invalidación**: `DishImageCacheKey.VERSION = "dish-image-v2"` (KDoc actualizado); `git mv` de los 6
    assets `app/src/main/assets/img/dishes/seed/dish-image-v1-*.webp` → `dish-image-v2-*.webp` (bytes
    **idénticos**, md5 sin cambios: **sin** red y **sin** cuota; no se regenera nada); `EXPECTED_SEED_KEYS`
    del script en v2 y su espejo del prompt actualizado a la plantilla v2 (título+cocina+`COMPOSITION`+
    `STYLE_SUFFIX`+`NEGATIVES`) con un comentario que acota la deriva conocida (no puede reproducir las
    cláusulas de `imagePrompt`/ingredientes).
  - **Contrato de `PromptBuilder`**: la instrucción del `imagePrompt` pide una descripción visual breve,
    de una línea, del plato terminado (ingredientes principales y aspecto —color, textura, guarnición—,
    sin texto ni manos ni personas); `OUTPUT_CONTRACT`, ids y determinismo intactos. KDoc de
    `DishVisualDescriptor` corregido (el `imagePrompt` es el detalle, no un sustituto del título).
  - **Diagnóstico del origen** (puro y sin comportamiento): `data.image.DishImageOrigin` (enum
    `GENERATED`/`SEED`/`CATEGORY`/`GLYPH`, con `CATEGORY` agrupando la cadena local) y
    `data.image.DishImageOriginSink` (JDK puro en memoria, ignora ids en blanco, gana el último);
    `DishImageResolver` gana `onResolved: (DishImageRequest, DishImageOrigin) -> Unit = { _, _ -> }`
    (parámetro nuevo al final con default) y recuerda por clave el origen con el que pobló la memoria
    (un acierto de memoria sobre bytes semilla sigue reportando `SEED`; el default asume `GENERATED`);
    `RecipeArtworkImagePrecedence.originOf(local, generated, resolvedOrigin)` clasifica la capa que gana y
    `RecipeArtwork` la notifica por `ui.artwork.DishImageOriginDiagnostics` (interface + no-op +
    `LocalDishImageOriginDiagnostics`) como **llamada lateral** (ninguna rama de pintado la lee);
    `MainActivity` cablea el sumidero, el `onResolved` y el proveedor **solo** con `BuildConfig.DEBUG`
    (`Log.d("SazonDishImage", "origin=… id=… title=…")`); en release queda el no-op y no emite nada.
- Verificación ejecutada:
  1. Tirada focal `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.domain.image.*"
     --tests "com.sazon.app.domain.prompt.PromptBuilderTest" --tests "com.sazon.app.data.image.*"
     --tests "com.sazon.app.data.DishImageSeedAssetsTest"
     --tests "com.sazon.app.data.ai.DishVisualDescriptorWiringTest" --tests "com.sazon.app.ui.artwork.*"
     --rerun-tasks` → `BUILD SUCCESSFUL` (**21 suites / 162 tests / 0 fallos / 0 errores / 0 saltados**).
  2. `bash init.sh` → **exit 0** (`./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`, sin
     procesos largos, sin red y sin cuota), con parseo propio de los XML y del informe de lint.
- Evidencia capturada: **93 suites / 687 tests / 0 fallos / 0 errores / 0 saltados** (baseline en `HEAD`
  92/673 de la sesión 097; delta **+1 suite / +14 tests**: `DishImageOriginSinkTest` 4 nueva,
  `DishImagePromptTest` 11→13, `DishImageCacheKeyTest` 12→13, `DishImageResolverTest` 19→24,
  `RecipeArtworkImagePrecedenceTest` 2→4). Lint (SARIF): **0 errores** y **13 warnings** preexistentes
  (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1),
  **0 bajo `app/src`**. Vectores: prompt del escenario 1 (literal exacto), escenario 2 con título **y**
  `imagePrompt`, tope/orden/distintos/sin-despensa y omisión sin comas sueltas, `STYLE_SUFFIX` una vez,
  `VERSION == "dish-image-v2"` y `of("Arroz con pollo criollo") == "dish-image-v2-arroz-con-pollo-criollo"`,
  `md5` de los 6 assets idénticos antes/después del `git mv` (449.844 B = 439 KiB ≤ 2 MiB),
  `originOf(...)` en las 4 capas, sumidero y `onResolved` (`SEED`/`GENERATED`/fallo sin reporte/default
  no-op). Inspección: `grep import android` en `domain/image` → **0** (y 0 `io.ktor`/`java.net`/`Log`);
  en `data/image` solo `AndroidDishImageCache.kt`; `grep -rn "dish-image-v1" app/src/main app/src/test
  docs/adr docs/technical-discovery.md docs/domain/taxonomy.md docs/risks-and-open-questions.md scripts`
  → **0**; `test -d docs/design/concepts/dish-image-prompt-v2` → **no existe**; `git diff --name-only` =
  la lista de "Expected File Changes" (más la spec, `feature_list.json`, `PROGRESS.md` y los 4 docs
  durables); build/lockfile/`init.sh`/`scripts/**` (salvo el espejo)/`seed-recipes.json`/Taxonomía/
  `DESIGN.md`/`docs/design/**` sin diff; sin dependencias nuevas; **sin** commit/push/PR.
- Ficheros creados/cambiados: `data/image/{DishImageOrigin,DishImageOriginSink}.kt` y
  `ui/artwork/DishImageOriginDiagnostics.kt` (nuevos); `domain/image/DishImagePrompt.kt`,
  `domain/image/DishImageCacheKey.kt`, `domain/image/DishVisualDescriptor.kt`,
  `domain/prompt/PromptBuilder.kt`, `data/image/DishImageResolver.kt`,
  `ui/artwork/RecipeArtworkImagePrecedence.kt`, `ui/artwork/RecipeArtwork.kt`, `MainActivity.kt`,
  `data/DishImageSeedAssets.kt`, `data/DishImageSeedSource.kt`, `data/image/DishImageCachePolicy.kt`
  (modificados); `scripts/prepare-dish-image-seed.sh`; los 6 assets renombrados;
  `app/src/test/**` (11 ficheros actualizados + `DishImageOriginSinkTest` nuevo); `docs/adr/0005`,
  `docs/technical-discovery.md`, `docs/domain/taxonomy.md`, `docs/risks-and-open-questions.md`,
  `feature_list.json` (`dish-image-prompt-v2` → `passing`) y este registro.
- Hallazgos relevantes: la regla normativa de ensamblado de la spec (unir segmentos no vacíos con
  `", "` y colapsar comas) convive con una plantilla ilustrativa que muestra `": "` tras la cláusula de
  identidad; se siguió la regla (ver nota arriba). El test de invalidez v1→v2 se expresa sin literales
  `dish-image-v1` para cumplir el grep del plan de verificación (la versión es el primer segmento).
- Riesgo o incógnita abierta: la longitud del prompt crece (acotada); el proveedor puede seguir
  devolviendo imágenes poco apetitosas (mejora la probabilidad, no la garantiza); la invalidación
  regenera **una vez** cada plato ya cacheado (~2 % de cuota, acotado por la LRU y el tope por sesión);
  el espejo del script sigue siendo bash duplicado y acotado; `CATEGORY` no distingue propia/categoría/
  `default` (haría falta abrir `RecipePhotoSource`, feature propia); queda **pendiente separado** el tope
  de 12 por sesión / manejo del `429`.
- Validación independiente (misma sesión 100, rol `validator`, contexto limpio): **veredicto `accept`**.
  Re-ejecutó `bash init.sh` (exit 0; test/lint `UP-TO-DATE` por la config cache, así que forzó
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` 5m13s/34 tareas y
  `bash scripts/gradle.sh lint --rerun-tasks` 4m31s/30 tareas, ambos `BUILD SUCCESSFUL`), parseó sus
  propios XML (**93 suites / 687 tests / 0 fallos / 0 errores / 0 saltados**) y el SARIF (**0 errores**,
  13 warnings preexistentes, **0 bajo `app/src`**); re-derivó la baseline en `HEAD` (92 ficheros con
  `@Test` / 673 ⇒ +1 suite / +14 tests) y verificó los 12 escenarios, la invalidación (0
  `dish-image-v1`), los 6 assets renombrados (md5 idénticos, 449 844 B ≤ 2 MiB), el diagnóstico
  `GENERATED`/`SEED`/`CATEGORY`/`GLYPH` solo bajo `BuildConfig.DEBUG` (release no-op, sin datos
  sensibles), la pureza de `domain.image`/`data.image` y la disciplina de alcance. Sin hallazgos
  Critical/High/Medium; 2 Low informativos no bloqueantes (L1: plantilla ilustrativa de la spec con
  `": "` frente a la regla normativa `", "` que sí se implementó y exigen los escenarios; L2: el eje
  `ui.artwork` → `data.image` es decisión explícita de la spec). Registro en
  `docs/validations/dish-image-prompt-v2.md`.
- Siguiente paso: el orquestador de `feature-flow` persiste la aceptación (`feature_list.json` →
  `accepted`) y crea el **commit local** en `feature/dish-image-prompt-v2`; la publicación (push + PR con
  `Closes #111`) la hace `github-flow`. No queda ninguna feature `not_started` dependiente de esta: el
  plan sigue cerrado salvo `home-suggestions-personalized` y `seed-catalog-50` (no relacionadas).

### Sesión 101

- Fecha: 2026-10-13
- Objetivo: **formalizar la dirección de diseño "tarjeta tipográfica"** (agente principal, rol
  orquestador; sin subagente de rol). Nace de la revisión por la persona de los problemas de las fotos
  reales: (1) dependencia de la API de imágenes con cuota y (2) imágenes preestablecidas imprecisas (la
  receta de IA caía en la foto de su categoría). Se bocetaron alternativas en prototipos HTML locales y
  la persona eligió la **tarjeta C3 "carta de restaurante"** con los tres ingredientes principales, el
  oscuro **B + C** y la imagen **L2**.
- Completado:
  - **`docs/adr/0006-typographic-recipe-cards.md`** (nuevo): la imagen de una receta es una ficha
    tipográfica; la foto (generada, opt-in) pasa a **capa de fondo** (L2); se retira la cadena local de
    fotos; tokens de tarjeta en oscuro (`darkCardSurface` `#3A2E26` + `darkCardOutline` `#7D6A5C`,
    **3,26:1**, frente al 1,15:1 anterior, por debajo del 3:1 de WCAG 1.4.11); monograma en Favoritos;
    `RecipeArtwork` pasa a proveedor de la capa de fondo. Ajusta los ADR 0004 y 0005.
  - **`DESIGN.md`** como fuente de verdad: Overview, Product Feel, tarjeta de colores (2 filas nuevas),
    frontmatter (tokens y componentes de tarjeta), `Components` (`RecipeCard`, `RecipeListCard`,
    "Imagen de receta" con la variante L2 y la marca de agua), Core Screens 4/5/6, Accessibility,
    Do's and Don'ts, OQ4 revisada y OQ6/7/8 nuevas, y el registro de que **faltan las capturas** de esta
    dirección (desviación de la política pendiente de autorización).
  - **`feature_list.json`**: cinco features nuevas con `depends_on` y verificación —
    `card-typographic-tokens-and-design` (ui, **blocked**), `recipe-card-typographic` (ui, **blocked**),
    `recipe-detail-typographic-header` (ui, **blocked**), `dish-artwork-as-card-background` (integration,
    **blocked**) y `retire-recipe-photos` (data, `not_started`). Total: 56 features.
  - **Prototipos versionados** en `docs/design/concepts/typographic-cards/` (+ `design.md`): 
    `prototipo-c3.html` (cinco pantallas en claro y oscuro + hoja de componentes + par OFF/ON),
    `prototipo-c3-imagenes.html` (opciones de contraste A/B/C y variantes L1/L2) y las dos
    `comparativa-*.html`. Es la **referencia** de la que saldrán las capturas por pantalla.
  - **Peticiones de capturas** (`README.md` con la lista exacta de pantallas) en
    `docs/design/concepts/{card-typographic-tokens-and-design,recipe-card-typographic,recipe-detail-typographic-header,dish-artwork-as-card-background}/`.
  - **Docs derivados** (subagente `general`, contexto propio): `docs/build-brief.md`,
    `docs/domain/taxonomy.md` §6, `docs/technical-discovery.md`, `docs/risks-and-open-questions.md` y las
    líneas de "Estado" de `docs/adr/0004` y `docs/adr/0005`.
  - Prototipos de trabajo sin versionar en `preview/` (fuera del repo, no se commitean).
- Ficheros creados/cambiados: `docs/adr/0006-typographic-recipe-cards.md`,
  `docs/design/concepts/typographic-cards/{design.md,*.html}` y las cuatro carpetas de petición de
  capturas (nuevos); `DESIGN.md`, `feature_list.json`, `PROGRESS.md` y los docs derivados del subagente
  (modificados). **Sin** tocar `app/**`, `scripts/**`, `init.sh` ni `gradle*`; sin dependencias nuevas.
- **Bloqueo registrado**: las cuatro features de UI quedan `blocked` porque **faltan las capturas de
  referencia** y el origen previsto (prototipos locales en vez de export de Stitch) es una **desviación
  de la política pendiente de autorización explícita de la persona**. Al autorizarla, se capturan los
  prototipos (claro y oscuro) en las carpetas de cada feature y las features pasan a `not_started`.
- Riesgo o incógnita abierta: se renuncia al atractivo apetitoso de la fotografía como protagonista
  (registro editorial, reversible); el Detalle pierde el recurso del solape de la tarjeta sobre la foto;
  OQ6 (¿carrusel a 3:2 para tener las tres filas siempre?), OQ7 (¿se limpia también el set semilla de
  imágenes `img/dishes/seed/`?) y OQ8 (¿basta la marca de agua del glifo al 12 %?).
- Hallazgos del subagente y cierre (agente principal): el subagente detectó cuatro incoherencias en los
  docs y **todas quedan resueltas**: (1) `docs/domain/taxonomy.md` §3 — el campo `recipeCategories[].image`
  y el invariante "todo `recipeCategories[].id` tiene imagen local" quedan marcados como retirados por
  ADR 0006 (anotados, se limpian con `retire-recipe-photos`); (2) `docs/technical-discovery.md` (Testing)
  — el bloque de `RecipePhotoAssetsTest` y `recipe.photos.assets.dir` se marca como "describe el código
  vigente hasta `retire-recipe-photos`"; (3) el **ancla de estilo** de `dish-style-reference`
  (`img/recipes/default.<ext>`) deja de existir con la retirada: queda como **decisión abierta** en
  `DESIGN.md` OQ7 y en la línea de estado de ADR 0005 (¿conservar ese solo asset, ~136 KB y no visible en
  ninguna pantalla, o generar solo por texto→imagen?); (4) la frase de ADR 0005 "se mantiene para la
  cadena local" se matiza a "mientras la cadena local exista (retirada por ADR 0006)".
- Autorizaciones de la persona (misma sesión 101, tras revisar la formalización): (1) **la desviación de
  capturas queda autorizada** — las capturas de referencia se toman de los prototipos locales
  (`docs/design/concepts/typographic-cards/`), en claro y oscuro, y se dejan en la carpeta de cada
  feature; las cuatro features de UI pasan de `blocked` a `not_started` y **no se implementan hasta que
  esas capturas existan** (lo exige el planner y lo impide `github-flow`); (2) **se conserva
  `img/recipes/default.<ext>`** (~136 KB) como ancla de estilo de `dish-style-reference` (ADR 0005,
  fase 6), sin mostrarse en ninguna pantalla: salen del APK los otros **30 de los 31 assets (~6,2 MiB)**.
  Ambos ajustes se propagaron a `DESIGN.md` (registro de capturas y OQ7), `docs/adr/0004`, `0005` y
  `0006`, `docs/build-brief.md`, `docs/risks-and-open-questions.md`, `docs/technical-discovery.md`,
  `docs/domain/taxonomy.md`, `docs/design/concepts/README.md`, los cuatro `README.md` de capturas, el
  `design.md` de `typographic-cards/` y las cinco entradas de `feature_list.json`.
- Siguiente paso: aportar las capturas (claro y oscuro) en las carpetas preparadas de cada feature y, con
  ellas dentro, `github-flow execute card-typographic-tokens-and-design` para arrancar la cadena por el
  planner. **No** se ha hecho commit, push ni PR: el árbol de trabajo queda con los cambios en disco para
  revisión.

### Sesión 102

- Fecha: 2026-10-14
- Objetivo: **capturar los prototipos** de la dirección tipográfica (ADR 0006) y depositarlos como
  capturas de referencia versionadas en la carpeta de cada feature. Desviación de la política de capturas
  (origen: prototipos locales en vez de export de Stitch) ya autorizada por la persona en la sesión 101.
- Completado:
  - **Pipeline reproducible**: se extrajeron del prototipo `docs/design/concepts/typographic-cards/`
    (`prototipo-c3.html`) el `<style>` y cada bloque `<div class="app">` y se generó un `code.html`
    autónomo por pantalla (390×844, sin marco de dispositivo, con `?mode=dark` para la variante oscura).
    Render con **Chrome headless de Windows por interop desde WSL**
    (`--headless=new --force-device-scale-factor=2 --window-size=390,844 --virtual-time-budget=4000`), que
    da PNG de **780×1688**. Sin dependencias nuevas en el proyecto: el navegador es el del sistema.
  - **16 capturas en 7 pantallas** (claro y oscuro), cada carpeta con su `code.html` y su `DESIGN.md`:
    `card-typographic-tokens-and-design/tarjeta-oscura/` (2);
    `recipe-card-typographic/recipe-card/` (4: Inicio y Resultados, claro y oscuro),
    `.../favoritos-lista/` (2), `.../favoritos-rejilla/` (2);
    `recipe-detail-typographic-header/detalle-receta/` (2);
    `dish-artwork-as-card-background/tarjeta-con-imagen/` (2) y `.../detalle-con-imagen/` (2).
  - **Dos pantallas extra** que el prototipo no tenía como pantalla: el **par OFF/ON** del ajuste de
    imágenes (tarjeta tipográfica vs imagen como capa de fondo L2) y el **Detalle con la imagen de fondo**
    (construido desde la pantalla de Detalle). En las dos, la foto de muestra es un asset real del APK
    (`img/recipes/category/cat-rice.jpg`), advertido en su `DESIGN.md`.
- **Defectos detectados por las capturas y corregidos** (para eso sirven): (1) `.ing3` no era una clase
  global en el CSS, así que en el hero del Detalle los tres ingredientes salían **en una línea** y
  empujaban el texto contra el botón de atrás → `.ing3` pasa a global en los dos prototipos y se
  re-renderiza; (2) en la variante **L2** los tokens de "texto sobre imagen" (`--text` en blanco) se
  aplicaban a **toda** la tarjeta, de modo que la **banda sólida de ingredientes quedaba en blanco sobre
  blanco** → esos tokens se acotan al área del arte (`.ncard.onimg .art`), con lo que la banda vuelve a
  usar los tokens normales de tarjeta; (3) la **regla de 5 px** de la miniatura de Favoritos era
  terracota y `DESIGN.md` decía "color de la cocina": a 5 px los pares suaves de `RecipeAccent` no se
  perciben, así que se corrige la redacción de `DESIGN.md` a **terracota** (la variación por cocina vive
  en el filete de 6 px de las tarjetas).
- Ficheros creados/cambiados: los 16 `.png`, 8 `code*.html` y 7 `DESIGN.md` nuevos en
  `docs/design/concepts/`; `DESIGN.md` (registro de capturas aportadas + regla de la miniatura),
  `feature_list.json` (las cuatro entradas de UI citan las capturas aportadas), los cuatro `README.md` de
  capturas, `docs/design/concepts/README.md`, `docs/design/concepts/typographic-cards/design.md` y los dos
  prototipos versionados (`.ing3` global y tokens de la variante L2 acotados al arte). **Sin** tocar
  `app/**`, `scripts/**`, `init.sh` ni `gradle*`; sin dependencias nuevas.
- Nota: los prototipos de trabajo de `preview/` (sin versionar) quedan **superados**: la referencia
  versionada es `docs/design/concepts/typographic-cards/`.
- Siguiente paso: la formalización viaja en la rama **`docs/typographic-cards-formalization`** con el **PR #114**
  contra `main` (el agente **no** mergea: lo mergea la persona; permiso de push permanente). Con el
  PR mergeado, arranca `github-flow execute card-typographic-tokens-and-design`. La limpieza previa de
  `execute` borrará el worktree y la rama de `home-suggestions-personalized` (PR #112 ya mergeado).

### Sesión 103

- Fecha: 2026-10-14
- Objetivo: **implementar** (rol `implementer`, contexto limpio, worktree
  `feature/card-typographic-tokens-and-design`) la primera de las cinco features del ADR 0006,
  **`card-typographic-tokens-and-design`** (`area: ui`), según
  `docs/specs/card-typographic-tokens-and-design.md`. No es orquestación: no hay commit/push/PR.
- Completado:
  - **Tokens de color** en `ui/theme/Color.kt`: `SazonDarkCardSurface = Color(0xFF3A2E26)` y
    `SazonDarkCardOutline = Color(0xFF7D6A5C)` en el bloque de paleta de `DESIGN.md`, con KDoc que cita
    el ADR 0006 y su ámbito (tarjeta). `LightColorScheme`/`DarkColorScheme` **intactos**: `darkSurface`
    sigue `#312720` y el navegador, las hojas y el resto de pantallas no cambian.
  - **Contrato de tarjeta** nuevo en `ui/theme/CardTokens.kt`: `SazonCardColors` (`surface`,
    `boundary`, `boundaryWidth`), `SazonCardTokens` (`light` = `SazonSurface` + transparente + `0.dp`;
    `dark` = `#3A2E26` + `#7D6A5C` + `1.dp`; `of(dark)`), `TINT_ALPHA = 0.14f` (banda documentada
    12–16 %) y `tintedSurface(surface, accent, alpha)` **puro y determinista** (`lerp` de Compose, sin
    Android ni reloj), más `LocalSazonCardColors` (`staticCompositionLocalOf`, default la variante
    clara). El borde sustituye a la sombra como elevación en oscuro.
  - **Cableado** en `Theme.kt`: `SazonTheme` envuelve el `content` de `MaterialTheme` en
    `CompositionLocalProvider(LocalSazonCardColors provides SazonCardTokens.of(darkTheme))`.
  - **Tests JVM**: `DesignTokensTest` pasa de 6 a **8** casos (frontmatter ↔ `Color.kt` de los dos
    tokens de tarjeta; `darkSurface` intacto en frontmatter y esquema, con los tokens de tarjeta
    distintos de él y la tarjeta clara igual a la `surface` del tema) y **`CardTokensTest`** nuevo con
    **5** casos (escenarios 2-5 de la spec) y helpers privados `relativeLuminance`/`contrastRatio`
    (fórmula WCAG) y comparación de hex con ±1 por canal.
  - **Docs durables**: `docs/technical-discovery.md` (el párrafo del Tema gana el contrato de tarjeta y
    el de Testing nombra `CardTokensTest` y el `DesignTokensTest` ampliado) y
    `docs/adr/0006-typographic-recipe-cards.md` (`Estado`: esta feature implementada; las otras cuatro
    siguen pendientes). `DESIGN.md` **no** se toca (ya declaraba tokens, tinte y 3,26:1).
- Ratios medidos (fórmula WCAG, test `CardTokensTest`): borde `#7D6A5C` vs `darkBackground` `#241C16`
  = **3,263 ≥ 3,0** (ADR: 3,26); negativo no vacuo: `darkSurface` `#312720` = **1,151** y relleno nuevo
  `#3A2E26` = **1,276**, ambos **< 3,0** (el límite es el **borde**, no el relleno); tinte a 14 % de los
  cuatro roles de acento oscuro (`primaryContainer` `#6E2C15`, `secondaryContainer` `#2F3D1E`,
  `tertiaryContainer` `#4A3413`, `surfaceVariant` `#3D3129`): relleno teñido vs fondo **1,28–1,31**
  (< 2,0) y relleno vs borde **2,49–2,56** (≥ 2,0). Vectores del prototipo C3 comprobados:
  `primaryContainer` al 12 % → `#402E24` y `secondaryContainer` al 16 % → `#383025`.
- Verificación ejecutada: baseline derivada en `HEAD` con `bash scripts/gradle.sh testDebugUnitTest`
  = **93 suites / 697 tests / 0 fallos / 0 errores / 0 saltados**; tirada focal
  `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.theme.*" --rerun-tasks` →
  `BUILD SUCCESSFUL` (2m36s, 34 tareas; `CardTokensTest` 5/5 y `DesignTokensTest` 8/8); gate completo
  **`bash init.sh` → exit 0** (test + lint; `lint` `BUILD SUCCESSFUL` 2m12s). Parseo propio de
  `app/build/test-results/testDebugUnitTest/TEST-*.xml`: **94 suites / 704 tests / 0 fallos / 0 errores
  / 0 saltados** (delta **+1 suite / +7 tests**). SARIF
  `app/build/reports/lint-results-debug.sarif`: **0 errores**, 13 warnings preexistentes de entorno
  (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1) y
  **0 bajo `app/src`**.
- Disciplina de alcance (grep): `grep -rn "3A2E26\|7D6A5C" app/src` solo aparece en `ui/theme`
  (`Color.kt`, el KDoc de `CardTokens.kt` y `CardTokensTest.kt`); **0** consumidores de
  `LocalSazonCardColors`/`SazonCardTokens`/`tintedSurface` fuera de `ui/theme` (deliberado: los tokens
  nacen para `recipe-card-typographic`, como `domain.image` en su día); `git diff --name-only` = los
  `Expected File Changes` (más la spec del planner, `feature_list.json` y `PROGRESS.md`); **sin** diff en
  pantallas, `ui/artwork/**`, `app/build.gradle.kts`, `gradle/**`, `init.sh`, `scripts/**`, `DESIGN.md`
  ni `docs/design/**`; sin dependencias nuevas. Capturas obligatorias
  `docs/design/concepts/card-typographic-tokens-and-design/tarjeta-oscura/{screen.png,screen-dark.png}`
  revisadas (superficie `#3A2E26` + borde `#7D6A5C` visibles, sin sombra, tinte cálido sutil) y el
  contrato de tokens las reproduce.
- Ficheros creados/cambiados: `ui/theme/CardTokens.kt` y
  `app/src/test/java/com/sazon/app/ui/theme/CardTokensTest.kt` (nuevos); `ui/theme/Color.kt`,
  `ui/theme/Theme.kt`, `app/src/test/java/com/sazon/app/ui/theme/DesignTokensTest.kt`,
  `docs/technical-discovery.md` y `docs/adr/0006-typographic-recipe-cards.md` (modificados);
  `feature_list.json` (`card-typographic-tokens-and-design` → **`passing`** con evidencia) y este
  registro. **Sin** commit, push ni PR: el trabajo queda en disco para el validador independiente.
- Hallazgos: el compilador de Kotlin atrapó un *shadowing* real — `fun of(dark: Boolean)` con las
  propiedades `light`/`dark` homónimas devolvía `Any`; se resuelve con `this.dark` (el parámetro
  conserva el nombre `dark` del contrato de la spec). El 3:1 lo da el **borde**, no el relleno (ya
  recogido en la spec): un test que exigiera ≥ 3:1 al relleno fallaría; el test lo documenta como caso
  negativo no vacuo. `surfaceVariant` no se tiñe (es el mismo color que la superficie), así que su
  vector de relleno es idéntico al relleno sin tinte.
- Siguiente paso: **validación independiente** (`feature-validator`, contexto limpio) de
  `docs/specs/card-typographic-tokens-and-design.md`; si devuelve `accept`, el orquestador de
  `feature-flow` persiste `accepted` y crea el **commit local** en
  `feature/card-typographic-tokens-and-design`, y `github-flow` publica (push + PR). Después podrá
  arrancar `recipe-card-typographic`, que consume estos tokens.
- Validación independiente (misma sesión 103, rol `validator`, contexto limpio): **veredicto `accept`**.
  Re-ejecutó `bash init.sh` (exit 0) y forzó `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks`
  (`BUILD SUCCESSFUL` 3m41s, 34/34 tareas) y `bash scripts/gradle.sh lint --rerun-tasks`
  (`BUILD SUCCESSFUL` 3m1s, 30/30 tareas); parseó sus propios XML (**94 suites / 704 tests / 0 fallos /
  0 errores / 0 saltados**) y el SARIF (**0 errores**, 13 warnings de entorno, **0 bajo `app/src`**);
  re-derivó la baseline en `HEAD` (93 ficheros con `@Test` / 697 ⇒ +1 suite / +7 tests) y recalculó los
  ratios WCAG de forma independiente (borde **3,263** ≥ 3,0; `darkSurface` `#312720` **1,151** y relleno
  `#3A2E26` **1,276**, ambos < 3,0 como negativo no vacuo; tinte 14 % vs fondo < 2,0 y vs borde ≥ 2,0).
  Confirmó los 5 escenarios, la ausencia de hex sueltos y de consumidores fuera de `ui/theme`,
  `darkSurface`/esquemas intactos, las capturas obligatorias, la disciplina de alcance y que el
  implementer no hizo commit/push/PR. Sin hallazgos Critical/High/Medium; **1 Low** no bloqueante (el
  KDoc de `tintedSurface` dice "por componente en sRGB" cuando el `lerp` de Compose interpola en
  **Oklab**; el comportamiento cumple la spec, corrección diferida a `recipe-card-typographic`) y
  1 Info (los rangos de ratio de la evidencia son orientativos en sRGB; las cotas duras sí están
  aseveradas). Nota preexistente no imputable: `DESIGN.md` "nunca bordes duros" vs el borde de tarjeta
  en oscuro, a reconciliar al aplicar los tokens. Sin hallazgos de seguridad. Detalle en
  `docs/validations/card-typographic-tokens-and-design.md`.
- Cierre (agente principal, orquestador de `feature-flow`): aceptación **persistida** en
  `feature_list.json` (`card-typographic-tokens-and-design` → `accepted` con la evidencia del validador)
  y **commit local** `feat: complete card-typographic-tokens-and-design` en
  `feature/card-typographic-tokens-and-design`. **Sin** push ni PR (los hace `github-flow`).
  `recipe-card-typographic` queda como siguiente feature del ADR 0006 (consume estos tokens) y sigue
  `not_started`.

### Sesión 104

- Feature **`recipe-card-typographic`** (`area: ui`, segunda de las cinco del ADR 0006, depende de
  `card-typographic-tokens-and-design`, ya `accepted`): planificada, implementada, autoverificada y
  **`accepted`** en esta misma sesión con validación independiente (veredicto `accept`, rama
  `feature/recipe-card-typographic`). Ciclo completo de `feature-flow` en el worktree de la feature,
  **sin** commit/push/PR del implementer; el commit local lo crea el orquestador y la publicación queda
  para `github-flow`.
- **Gate UI**: capturas obligatorias ya aportadas y revisadas en
  `docs/design/concepts/recipe-card-typographic/{recipe-card,favoritos-lista,favoritos-rejilla}/`
  (`screen.png`/`screen-dark.png`; `recipe-card` además `screen-resultados.png`/`screen-resultados-dark.png`).
- La tarjeta de receta es ya una **ficha tipográfica sin foto** en Inicio, Resultados y Favoritos:
  **lógica pura JVM** `ui.home.RecipeCardFormat` (`categoryLine`, `mainIngredients`/`MainIngredient` con
  `isPantry` excluido, orden de declaración y completado hasta 3 con despensa; `quantityLabel` vía
  `ui.detail.IngredientFormat`; `matchBadge(score, compact)`; `ingredientsLine`) y `ui.home.RecipeMonogram`
  (`of(title)`: conectores saltados, 1–2 iniciales, `null` si vacío). **Contenido compartido**
  `ui.home.RecipeFicha` (hueco 16:10 sin imagen, categoría en versalitas, título **Manrope local** —
  el tema solo overridea 4 estilos y `titleMedium`/`titleSmall` caen a Roboto —, ingredientes en **una
  línea** cuando es estrecha o en **tres filas con cifras tabulares** cuando es ancha, fila de metadatos,
  coincidencia en terracota, **filete de 6px** con `RecipeAccent` por cocina y **marca de agua** del
  glifo al 12 %), consumido por `RecipeCard` (Home), `ResultRecipeCard` (Resultados) y la rejilla de
  `FavoriteRecipeTile`; la **lista** de Favoritos pasa a **miniatura 96×96 de monograma** sobre
  `surfaceVariant` con la **regla terracota de 5px** a la izquierda. Cromo desde `LocalSazonCardColors`
  (oscuro `#3A2E26` + borde `#7D6A5C` 1 dp en lugar de sombra + superficie teñida; claro `#FFFFFF` sin
  borde). Primitivas extraídas a `ui/artwork/RecipeArtworkPrimitives.kt` (`RecipeGlyphArtwork`,
  `recipeAccentColors`, `recipeAccentBrush`); `RecipeArtwork` conserva firma y comportamiento y su único
  punto de uso es el Detalle (otra feature). Sin assets nuevos, sin dependencias nuevas, sin tocar el
  Detalle ni `DESIGN.md`.
- **Nota de coherencia resuelta** (mandato de la persona; manda `DESIGN.md`): la regla de 5px de la
  miniatura de Favoritos es **terracota** (`primary`) y la variación por cocina vive en el **filete de
  6px** de las tarjetas. Redacciones previas reconciliadas: `user_visible_behavior` de esta entrada en
  `feature_list.json` (orquestador), `docs/adr/0006` §6 y el `README.md` de capturas (implementer).
- **Gate**: `bash init.sh` exit 0 (test + lint vía `scripts/gradle.sh`, sin procesos largos). Tests
  **95 suites / 720 tests / 0 fallos / 0 errores / 0 saltados** (baseline derivada en `HEAD` 94/704;
  delta **+1 suite / +16 tests**: `RecipeCardFormatTest` 4→13, `RecipeMonogramTest` 7 nuevos). Lint
  SARIF: 0 errores, 13 warnings preexistentes de entorno y **0 bajo `app/src`**. El validador re-ejecutó
  el gate y forzó `--rerun-tasks` (test y lint) obteniendo los mismos conteos.
- **Dos desviaciones de spec evaluadas por el validador**: (1) el Escenario 5 se reconcilió a
  **primera + última palabra significativa**, la única lectura que reproduce los cinco vectores de las
  capturas (`TP/AC/GA/CG/PT`; "dos primeras" daba `AP` en `Arroz con pollo criollo`); (2) la celda de
  rejilla a 16:10 omite la fila de metadatos por ser fiel al `code.html` de `favoritos-rejilla` (subir a
  3:2 es **OQ6**, que sigue **abierto**). Hallazgos: ninguno Critical/High/Medium; 1 Low (KDoc de
  `SazonCardTokens` decía "no hay consumidores"; corregido por el orquestador, **solo comentario**) y
  1 Low/Info (la captura estrecha abrevia la fila de metadatos y se sigue `DESIGN.md`) + 3 Info. Se
  corrigió aquí el **I1** diferido del KDoc de `tintedSurface` (sRGB→Oklab). Detalle en
  `docs/validations/recipe-card-typographic.md`.
- **Pendiente para la persona**: revisión visual fina en el emulador (Home, Resultados, Favoritos lista
  y rejilla, en claro y oscuro) del filete por cocina, la marca de agua, la tipografía Manrope y el
  monograma terracota; no automatizable desde WSL.
- **WIP = 1 → 0** al cerrar. Siguiente feature del ADR 0006 en orden de lista:
  **`recipe-detail-typographic-header`** (`not_started`, `depends_on: recipe-card-typographic`) → toca
  `feature-spec` (planner) y exige capturas de referencia antes de implementar.

### Sesión 105

- Fecha: 2026-10-14
- Objetivo: **implementar** (rol `implementer`, contexto limpio, worktree
  `feature/recipe-detail-typographic-header`) la **tercera** de las cinco features del ADR 0006,
  **`recipe-detail-typographic-header`** (`area: ui`, `depends_on: recipe-card-typographic` ya
  `accepted`), según `docs/specs/recipe-detail-typographic-header.md`. No es orquestación: **sin**
  commit/push/PR (el commit lo crea el orquestador de `feature-flow` y publica `github-flow`).
- Completado:
  - **Lógica pura nueva** `ui/detail/RecipeDetailHeroFormat.kt`: constantes de geometría
    (`HERO_HEIGHT_DP = 240`, `FILLET_HEIGHT_DP = 6`, `WATERMARK_DP = 190`, `WATERMARK_ALPHA = 0.12f`,
    `MAIN_INGREDIENTS = RecipeCardFormat.MAIN_INGREDIENTS`), `data class HeroContent(category, title,
    ingredients: List<MainIngredient>)` y `of(taxonomy, recipe)` compuesto por
    `RecipeCardFormat.categoryLine` + `RecipeCardFormat.mainIngredients` (una sola regla de
    "categoría · cocina" y de ingredientes principales, con despensa fuera, orden de declaración, tope 3
    y respaldo al `id`). Sin Compose ni Android.
  - **Hero tipográfico** en `ui/detail/RecipeDetailScreen.kt`: `Box` de 240 dp a sangre con margen
    lateral 16 dp y `clip` de esquinas **inferiores** a 24 dp, relleno
    `recipeAccentBrush(RecipeArtworkStyle.of(recipe).accent)`, marca de agua `RecipeGlyphArtwork` de
    190 dp tintada `onSurfaceVariant` al 12 % desbordando por la esquina inferior derecha (offset
    +24/+6, recortada por el `clip`), `Column` alineada al pie con categoría en versalitas (11 sp, bold,
    tracking 0.14 em), título Manrope Bold 29/31 sp (-0.025 em, 2 líneas con elipsis) y las **tres filas**
    de ingrediente (nombre 12,5 sp `onSurfaceVariant` + cantidad 12,5 sp Medium tabular `onSurface`,
    hairline `outline` bajo cada fila incluida la última) y **filete de 6 dp**
    (`Brush.horizontalGradient(recipeAccentColors(accent))`) en el borde inferior. Tipografías locales
    (`HeroTypography` desde `SazonFontFamilies`): el tema solo overridea 4 estilos.
  - **Eliminados** `DetailCard` y el `Column(offset(y = -SazonSpacing.lg))`: el `MetaStrip` es ahora un
    **bloque propio** (sin solape) con `Surface` `surface`, esquinas inferiores 24 dp, margen lateral
    16 dp, sombra suave 4 dp, **cuatro columnas** `weight(1f)` centradas con hairline vertical `outline`
    (no tras la última), etiqueta en versalitas (9,5 sp, tracking 0.1 em, `onSurfaceVariant`) y valor en
    **Manrope Bold 14 sp tabular** (`onSurface`), alimentado por `RecipeDetailFormat.metaEntries`
    (**sin tocar** sus cuatro valores ni sus tests).
  - **La descripción de la receta deja de pintarse** (Scenario 8, consecuencia declarada de la spec):
    vivía en la tarjeta retirada.
  - **Intactos**: "Atrás" (píldora) y el corazón/guardar (48 dp) flotando sobre el hero; pestañas,
    checklist con `ServingScaler`, `ServingsStepper`, timeline, notas/nutrición, `DetailBottomBar`,
    `RecipeDetailState`/`RecipeDetailStateSaver`, `RecipeDetailFormat`, `IngredientFormat`,
    `ui/artwork/**` (solo se **consumen** `RecipeGlyphArtwork`/`recipeAccentBrush`/`recipeAccentColors`/
    `RecipeArtworkStyle`), `ui/home/**`, `MainShell`, `ui/theme/**`, dominio, datos, build/gradle,
    `init.sh` y `scripts/**`.
  - **Docs durables**: `docs/technical-discovery.md` (párrafo del Detalle: hero tipográfico 240 dp,
    degradado de cocina, marca de agua 12 %, filete 6 px, tira como bloque propio, `RecipeDetailHeroFormat`
    entre la lógica pura y que el Detalle **deja de consumir** `RecipeArtwork`), `docs/adr/0006`
    (`Estado`: tercera feature implementada —y `recipe-card-typographic` marcada `accepted`—; §7 y
    "Consecuencias": el hero del Detalle ya no usa `RecipeArtwork`, que queda **sin consumidores** hasta
    `dish-artwork-as-card-background`) y `DESIGN.md` → `Shapes` (la viñeta de la tarjeta superpuesta del
    Detalle se sustituye por el apilado **hero + tira de metadatos**). `docs/domain-model.md`,
    `AGENTS.md`, `docs/risks-and-open-questions.md` y `docs/design/concepts/**`: **sin cambios** (las
    capturas se leen, no se editan).
- Verificación ejecutada:
  1. Tirada focal `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.detail.*"
     --rerun-tasks` → `BUILD SUCCESSFUL` (2m25s, 34/34 tareas): `RecipeDetailHeroFormatTest` 10/10 y los
     cuatro tests previos de `ui.detail` verdes **sin diff**.
  2. Gate estándar **`bash init.sh` → exit 0** (`./gradlew test` + `./gradlew lint` vía
     `scripts/gradle.sh`; sin procesos largos, sin red y **sin** tocar `init.sh`).
- Evidencia capturada:
  - Parseo propio de `app/build/test-results/testDebugUnitTest/TEST-*.xml`: **96 suites / 730 tests /
    0 fallos / 0 errores / 0 saltados**. Baseline re-derivada en `HEAD` (`git grep -c '@Test'`) = **95
    ficheros / 720 anotaciones** ⇒ delta **+1 suite / +10 tests** (`RecipeDetailHeroFormatTest` 10). Los
    cuatro tests previos de `ui.detail` siguen intactos (`ServingScalerTest` 8, `IngredientFormatTest` 5,
    `RecipeDetailFormatTest` 4, `RecipeDetailStateTest` 5).
  - Vectores JVM comprobados (Taxonomía real): categoría `"Arroz · Latina"` (`rice`/`latin`) y
    `"Huevos y desayunos · Española"` (`egg_breakfast`/`spanish`); ingredientes `Pollo 400 g / Arroz 300 g
    / Zanahoria 1 ud` y la tortilla semilla `Patata 600 g / Huevo 6 ud / Cebolla 1 ud` (aceite y sal,
    despensa, fuera); `"Al gusto"` con `unit = null`; negativos **no vacíos** (receta vacía → lista vacía
    sin excepción; 2 ingredientes → 2; solo despensa → se completa a 3; 4 ingredientes → 3; ids
    desconocidos → `"futurista · marte"`; determinismo por igualdad estructural de `HeroContent`) y
    geometría `240/6/190/0,12/3` con `MAIN_INGREDIENTS == RecipeCardFormat.MAIN_INGREDIENTS`.
  - Lint (`app/build/reports/lint-results-debug.sarif`): **0 errores**, los **13 warnings** de entorno
    preexistentes (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2,
    `OldTargetApi` 1) y **0 bajo `app/src`**.
  - Disciplina de alcance por inspección: `git diff --name-only` = los `Expected File Changes`
    (`RecipeDetailHeroFormat.kt` y su test nuevos; `RecipeDetailScreen.kt`; `docs/technical-discovery.md`;
    `docs/adr/0006`; `DESIGN.md`) más la spec del planner y este harness; `grep -rn "RecipeArtwork(\|
    RecipePhotoSource" app/src/main/java/com/sazon/app/ui/detail` → **0**; `grep -rn "RecipeArtwork("
    app/src/main/java/com/sazon/app/ui` → solo la **definición** (`RecipeArtwork` queda **sin
    consumidores**); sin `DetailCard` ni `offset(y = (-` en `RecipeDetailScreen.kt`; `grep -rn "0xFF" ...
    | grep -v ui/theme` → solo las **dos máscaras de bytes preexistentes**; `test -f` de
    `docs/design/concepts/recipe-detail-typographic-header/detalle-receta/{screen,screen-dark}.png` →
    **OK**; sin diff en `app/build.gradle.kts`, `gradle/**`, `init.sh`, `scripts/**`, `assets/**`,
    `docs/domain/taxonomy.json`, `seed-recipes.json`, `ui/theme/**`, `ui/artwork/**` ni
    `docs/design/**`; sin dependencias nuevas.
- Ficheros creados/cambiados: `app/src/main/java/com/sazon/app/ui/detail/RecipeDetailHeroFormat.kt` y
  `app/src/test/java/com/sazon/app/ui/detail/RecipeDetailHeroFormatTest.kt` (nuevos);
  `app/src/main/java/com/sazon/app/ui/detail/RecipeDetailScreen.kt`, `docs/technical-discovery.md`,
  `docs/adr/0006-typographic-recipe-cards.md`, `DESIGN.md` y la spec (modificados);
  `feature_list.json` (`recipe-detail-typographic-header` → **`passing`** con evidencia) y este registro.
  **Sin** commit, push ni PR.
- Hallazgos:
  - El grep de alcance de la spec (`"RecipeArtwork\|RecipePhotoSource"` → 0) **chocaba con su propio
    Approach**, que exige `recipeAccentBrush(RecipeArtworkStyle.of(recipe).accent)`: `ui/detail` sí
    importa `RecipeArtworkStyle` (mapeo **puro** glifo/acento). Se reconciliaron las tres líneas de
    inspección del plan y del checklist a **`RecipeArtwork(`** (la llamada a la imagen) y se dejó
    constancia en la spec ("Hallazgo de implementación"). Lo que la spec pedía de verdad —el hero sin
    foto y `RecipeArtwork` sin consumidores— se cumple.
  - Desviaciones visuales **declaradas** por la spec y respetadas: la tira conserva `"480 kcal"` (el
    `code.html` dibuja `480`), los controles "Atrás"/guardar se mantienen a ≥48 dp (la captura los dibuja
    a 34 px) y las pestañas/stepper/checklist **no** se rediseñan aunque la captura los muestre con otros
    contenedores (`DESIGN.md` manda).
- Riesgo o incógnita abierta: ninguno nuevo. Queda para la persona la **revisión visual en el emulador**
  (abrir una receta semilla y una de IA, en claro y oscuro: confirmar hero sin foto, marca de agua,
  filete, tira con los cuatro valores y comportamiento idéntico del stepper, las pestañas y la barra
  inferior), no automatizable desde WSL; un defecto visual se corrige con feature correctiva. La
  desviación de contraste del hero en oscuro (~1,5:1 contra `darkBackground`) queda como está declarada
  en la spec, no bloqueante. **OQ7** (set semilla en el hero) sigue abierto: el hero es tipográfico.
- Siguiente paso: **validación independiente** (`feature-validator`, contexto limpio) de
  `docs/specs/recipe-detail-typographic-header.md`; si devuelve `accept`, el orquestador de
  `feature-flow` persiste `accepted` y crea el **commit local** en
  `feature/recipe-detail-typographic-header`, y `github-flow` publica (push + PR). Después podrá
  arrancar `dish-artwork-as-card-background` (L2), que recupera `RecipeArtwork` como capa de fondo y
  exige capturas ya aportadas.

### Sesión 106

- Fecha: 2026-10-14
- Objetivo: **validar de forma independiente** (rol `validator`, contexto limpio) y **persistir la
  aceptación** (orquestador de `feature-flow`) de **`recipe-detail-typographic-header`** (tercera
  feature del ADR 0006) en el worktree `feature/recipe-detail-typographic-header`. **Sin** push ni PR
  (los hace `github-flow`).
- Veredicto del validador independiente: **`accept`**. No se fio de la evidencia del implementer:
  `bash init.sh` salió exit 0 pero con `:app:testDebugUnitTest` **UP-TO-DATE**, así que forzó
  `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (**BUILD SUCCESSFUL** 3m45s, 34/34 tareas
  ejecutadas) y `bash scripts/gradle.sh lint --rerun-tasks` (**BUILD SUCCESSFUL** 3m17s, 30/30) y
  parseó por su cuenta los XML frescos: **96 suites / 730 tests / 0 fallos / 0 errores / 0 saltados**
  (`RecipeDetailHeroFormatTest` 10/10; `ServingScalerTest` 8, `IngredientFormatTest` 5,
  `RecipeDetailFormatTest` 4 —los cuatro valores de la tira—, `RecipeDetailStateTest` 5) y el SARIF
  (**0 errores**, 13 warnings de entorno, **0 bajo `app/src`**). Re-derivó la baseline en `HEAD`
  (`git grep -c '@Test'` → 95 ficheros / 720 anotaciones) ⇒ delta **+1 suite / +10 tests**.
- Contraste de los **8 escenarios** contra código + capturas + tests (hero 240 dp sin foto, esquinas
  inferiores 24 dp, marca de agua 190 dp @12 %, filete 6 dp, tira de 4 valores, sin `DetailCard` ni
  `offset`, descripción no pintada) y **disciplina de alcance** (sin diff en build/gradle, `init.sh`,
  `scripts/**`, `assets/**`, `ui/theme/**`, `ui/artwork/**`, `ui/home/**`, `MainShell.kt`, dominio ni
  datos; **0 llamadas** a `RecipeArtwork(` en `ui/**`; capturas versionadas sin modificar; sin
  dependencias nuevas). Sin hallazgos **Critical/High/Medium**; 2 **Low** informativos no bloqueantes
  (`HeroBottomShape` duplica el literal `24.dp` de `SazonShapes.medium`; la captura pinta
  `DE LA COCINA LATINA` y el código `ARROZ · LATINA` según `RecipeCardFormat.categoryLine`, aviso para
  la revisión visual) e informativos (título 700 vs 800 del prototipo, mismo precedente aceptado de
  `RecipeFicha`; dependencia mutua `ui.detail ⇄ ui.home` mandada por la spec). Detalle en
  `docs/validations/recipe-detail-typographic-header.md`.
- Persistencia de la aceptación (orquestador): `feature_list.json` →
  `recipe-detail-typographic-header` **`accepted`** con la evidencia del validador; este registro de
  sesión. **Commit local** en `feature/recipe-detail-typographic-header` (Conventional Commit, solo los
  ficheros de la feature + harness/docs); **sin** push ni PR.
- Ficheros de la feature aceptada: `app/src/main/java/com/sazon/app/ui/detail/RecipeDetailHeroFormat.kt`
  y `app/src/test/java/com/sazon/app/ui/detail/RecipeDetailHeroFormatTest.kt` (nuevos);
  `app/src/main/java/com/sazon/app/ui/detail/RecipeDetailScreen.kt`, `docs/technical-discovery.md`,
  `docs/adr/0006-typographic-recipe-cards.md`, `DESIGN.md`, la spec, `docs/validations/…`,
  `feature_list.json` y `PROGRESS.md`.
- Queda para la persona la **revisión visual en el emulador** (claro y oscuro: hero sin foto, marca de
  agua, filete, tira con los cuatro valores y stepper/pestañas/barra inferior igual que antes), no
  automatizable desde WSL; un defecto visual se corrige con una feature correctiva. **WIP = 0** para
  esta feature.
- Siguiente paso: `github-flow` publica (push + PR) esta rama; después podrá arrancar
  `dish-artwork-as-card-background` (L2), que recupera `RecipeArtwork` como capa de fondo y exige
  capturas ya aportadas.

### Sesión 107

- Fecha: 2026-10-14
- Objetivo: cerrar las **mejoras del harness** detectadas al comparar con la skill original de
  `github-flow`: permisos de los agentes, arranque del lanzador y aviso de fin. Rama
  `chore/harness-launch-and-agent-permissions`; PR propio (no es una feature).
- Completado:
  - **Permisos de los agentes V1 → V2** (`permissions:`, `shell:`, `subagent:` en `planner`,
    `implementer` y `validator`). Con la forma V1, la guarda `git push*` (y `rm -rf*`,
    `git reset --hard*`, `git clean*`) **se ignoraba en silencio**: nunca estuvo activa. El `edit: deny`
    sí funcionaba porque se llama igual en los dos dialectos, y eso despistaba. Verificado con sesiones
    reales: el `implementer` queda **bloqueado** en `git push --dry-run` y el `planner` no puede escribir
    fuera de `docs/specs/`.
  - **Lanzador sin espera**: `scripts/launch-feature-session.sh` crea la sesión en el servidor y devuelve
    el control (**0,69 s** medidos). Antes esperaba el ciclo completo (30-90 min) y el harness lo
    abandonaba a la hora con `Tool execution interrupted`, dejando al orquestador a ciegas mientras el
    flujo seguía vivo. Deja el id en `<worktrees-root>/.sazon-sessions/<feature-id>`.
  - **Ruleset mínimo al crear la sesión**: pasar `{action:"*",resource:"*",effect:"allow"}` **anula las
    guardas `deny` de los agentes**; el lanzador pasa solo `external_directory: allow` (necesario porque
    Gradle lee fuera del worktree, p. ej. `~/.gradle`).
  - **Aviso de fin**: `POST /api/experimental/session/<id>/wait` devuelve en **0,117 s** sobre una sesión
    inactiva; lanzado en segundo plano del harness (sin timeout, con aviso al terminar) permite publicar
    en cuanto el flujo acaba. Documentado en la skill de `github-flow` (dos frases factuales) y en
    `AGENTS.md` (una línea).
- Ficheros: `.opencode/agent/{planner,implementer,validator}.md`,
  `.opencode/skills/github-flow/SKILL.md`, `scripts/launch-feature-session.sh`, `AGENTS.md` y este
  registro. **Sin** tocar `app/**`, `init.sh` ni la build; sin dependencias nuevas.
- Nota: la corrección de permisos aplica a las sesiones que arranquen en un worktree que **ya contenga**
  el cambio; los flujos lanzados antes siguen con los ficheros antiguos.
- Integración: merge de `origin/main` (`7e4f1c3`) conservando **ambas** aportaciones de `PROGRESS.md`
  (sesión 105 de `recipe-detail-typographic-header` y esta, renumerada a 107).

### Sesión 108

- Fecha: 2026-10-14
- Cierre de la publicación: la persona **mergeó** el PR #120 (`recipe-detail-typographic-header`,
  merge commit `7e4f1c3`) y la issue #119 quedó **cerrada**. El `feature-flow` de la sesión 106 dejó el
  commit local (`a50fd90`); la publicación (push + PR) la hizo el orquestador de `github-flow`, **sin**
  merge por su parte.
- Limpieza (`github-flow prune`): se retiraron el worktree y la rama local de
  `recipe-card-typographic` (PR #118) y de `recipe-detail-typographic-header` (PR #120), ambas
  `accepted`, con PR mergeado y rama contenida en `origin/main`.
- Estado de features: **`seed-catalog-50`** pasa a **`blocked`** (borrador) a petición de la persona
  (2026-10-14): es contenido, no solo código, y necesita su material (38 recetas nuevas + 38 fotos
  propias) y la confirmación de la exención de capturas antes de planificarse. Sus dependencias
  (`home-suggestions-personalized`, `recipe-photos`) ya están `accepted`; todavía no tiene spec.
  Registrado en `feature_list.json` (rama `chore/seed-catalog-50-blocked`).

### Sesión 109

- Fecha: 2026-10-14
- Objetivo: implementar `dish-artwork-as-card-background` (`area: integration`, cuarta feature del
  ADR 0006, variante **L2**) según `docs/specs/dish-artwork-as-card-background.md`, en la rama
  `feature/dish-artwork-as-card-background` (worktree); solo hasta implementación y autoverificación
  (`passing`), **sin commit/push/PR** (los hace el orquestador). Gate de UI satisfecho: las capturas
  obligatorias `docs/design/concepts/dish-artwork-as-card-background/{tarjeta-con-imagen,detalle-con-imagen}/`
  ya existían y son de solo lectura.
- Completado:
  - `ui/artwork/RecipeArtwork.kt` reconducido a **proveedor de la capa de fondo**: pinta la imagen
    generada (`ContentScale.Crop`) con `alpha` animado (`animateFloatAsState` + `tween(CROSSFADE_MILLIS)`) y
    el **`scrim`** de contraste con el mismo alfa **encima** de la imagen y **debajo** de la tipografía;
    firma `(recipe, modifier, glyphSize)` y geometría conservadas (`glyphSize` reservado), **slot de
    contenido aditivo** que recibe la capa resuelta, **sin** `LocalRecipePhotoSource` ni degradado/glifo de
    respaldo. Diagnóstico de origen intacto (llamada lateral).
  - `ui/artwork/RecipeArtworkImagePrecedence.kt` reducida a **dos capas** (`TYPOGRAPHIC`/`GENERATED`);
    `layerOf(generated)`, `crossfadeTarget(generated)` y `originOf(generated, resolvedOrigin)` (sin `LOCAL`;
    `CATEGORY` queda sin productor).
  - `ui/artwork/DishImageArtworkOverlay.kt` **nuevo** (puro, sin Compose ni Android): `HOLE_RATIO = 16f/9f`,
    `headingOnImage`, `watermarkVisible` y `crossfadeAlpha` derivado de la precedencia.
  - `ui/theme/CardImageOverlay.kt` **nuevo**: único sitio con el `scrim` claro/oscuro
    (`rgba(20,12,8,.42/.18/.78)` / `rgba(10,6,4,.5/.22/.82)`) y el texto sobre imagen (`#FFFFFF`, blanco al
    86 % y hairline blanco al 32 %).
  - `RecipeFicha` gana `imageBackground: Boolean = false`; con `true` usa la estructura **L2**: hueco
    **16:9** (base tipográfica + `RecipeArtwork` + contenido del slot) y **banda sólida** con los
    ingredientes con cantidad (tres filas; la celda `compact` conserva una línea y sin metadatos). Con
    `false` la ficha aceptada **idéntica**. `RecipeCard`/`ResultRecipeCard`/`FavoriteRecipeTile` reenvían el
    flag.
  - `DetailHero` pinta `RecipeArtwork` como capa de fondo y usa el tratamiento sobre imagen; `HERO_HEIGHT_DP`
    = 240 intacta; la tira de metadatos no cambia.
  - `HomeScreen`/`ResultsScreen`/`FavoritesScreen` reciben y reenvían el flag; `MainShell` reparte
    `dishImageSettings.state.enabled`. **Hallazgo de alcance** (registrado en la spec): `ResultsScreen` no lo
    monta `MainShell` directamente, sino `GeneratorFlow` y `AiSearchFlow`, así que el flag los atraviesa con
    un parámetro aditivo (default `false`); sin ellos Resultados se quedaría siempre en `false`.
  - Tests JVM nuevos: `DishImageArtworkOverlayTest` 5, `RecipeBackgroundGateTest` 6 (escenarios 1-5:
    apagado → `null` con 0 invocaciones del resolutor; encendido con imagen → `GENERATED` alfa `1f`; sin
    imagen/códec roto/lento → `TYPOGRAPHIC`; `CancellationException` propagada; tope agotado → `null` sin
    invocar al delegado) y `RecipeArtworkImagePrecedenceTest` actualizado a dos capas (5).
  - Docs durables: `DESIGN.md` (frontmatter `recipe-card-image` con los tokens del `scrim` y del texto sobre
    imagen; sección *Imagen de receta* reconciliada a L2 y a la capa base tipográfica, cadena local sin
    lectores), `docs/technical-discovery.md`, `docs/adr/0006` (cuarta feature implementada),
    `docs/domain/taxonomy.md` §6 y `docs/risks-and-open-questions.md`.
- Verificación ejecutada: baselines re-derivadas en `HEAD` (`git grep -c '@Test'` → 96 ficheros / 730
  anotaciones). Tiradas focales `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.artwork.*"`
  y `--tests "com.sazon.app.ui.*"` → `BUILD SUCCESSFUL`. Gate `bash init.sh` → **exit 0** (test 1m41s +
  lint 2m01s, vía `scripts/gradle.sh`, sin procesos largos; `init.sh` intacto). Parseo propio de
  `app/build/test-results/testDebugUnitTest/TEST-*.xml`: **98 suites / 742 tests / 0 fallos / 0 errores /
  0 saltados** (delta **+2 suites / +12 tests**). SARIF `app/build/reports/lint-results-debug.sarif`:
  **0 errores** y **13 warnings** preexistentes de entorno (GradleDependency 7, NewerVersionAvailable 3,
  AndroidGradlePluginVersion 2, OldTargetApi 1), **0 bajo `app/src`**.
- Ficheros creados/cambiados: `ui/artwork/{RecipeArtwork.kt,RecipeArtworkImagePrecedence.kt}` (modificados),
  `ui/artwork/DishImageArtworkOverlay.kt` (nuevo), `ui/theme/CardImageOverlay.kt` (nuevo),
  `ui/home/{RecipeFicha,RecipeCard,HomeScreen}.kt`, `ui/results/{ResultRecipeCard,ResultsScreen}.kt`,
  `ui/favorites/{FavoriteRecipeCard,FavoritesScreen}.kt`, `ui/detail/RecipeDetailScreen.kt`,
  `ui/MainShell.kt`, `ui/generator/GeneratorFlow.kt`, `ui/search/AiSearchFlow.kt`,
  `app/src/test/java/com/sazon/app/ui/artwork/{DishImageArtworkOverlayTest.kt,RecipeBackgroundGateTest.kt}`
  (nuevos) y `RecipeArtworkImagePrecedenceTest.kt` (modificado), más `DESIGN.md`,
  `docs/technical-discovery.md`, `docs/adr/0006`, `docs/domain/taxonomy.md`,
  `docs/risks-and-open-questions.md`, la spec (hallazgo), `feature_list.json` y este registro.
- Inspección: `DishImageArtworkOverlay.kt` sin imports `android`/`androidx`; **0 hex** en `ui/artwork` (viven
  en `ui/theme/CardImageOverlay.kt`); `RecipeArtwork(` = definición + **2** consumidores (hueco y hero);
  `LocalRecipePhotoSource` solo en `MainActivity.kt` (provider conservado) y `RecipePhotoSource.kt`;
  capturas de `docs/design/concepts/dish-artwork-as-card-background/**` **sin modificar**; sin diff en
  `domain/**`, `data/**` (ajuste/tope), `assets/**`, `app/build.gradle.kts`, `gradle/**`, `init.sh` ni
  `scripts/**`; sin dependencias nuevas.
- Riesgo/incógnita abierta: **revisión visual de la persona** (emulador, tras el merge) de la tarjeta
  **estrecha** del carrusel y la **compacta** de la rejilla con la banda L2 (extrapolación: las capturas solo
  cubren la ancha de Resultados y el hero) y del contraste del título sobre una foto arbitraria (el `scrim`
  no es medible en JVM). OQ6 (¿sube el carrusel a 3:2?) sigue abierta. Un defecto visual se corrige con una
  feature correctiva.
- Siguiente paso: validación independiente de `dish-artwork-as-card-background` con `feature-validator`;
  después, commit local (lo hace el orquestador de `feature-flow`) y publicación por `github-flow`. La
  quinta feature del ADR 0006 (`retire-recipe-photos`) espera a que ésta quede `accepted`.

### Sesión 110

- Fecha: 2026-10-14
- Objetivo: cerrar `dish-artwork-as-card-background` (`area: integration`, cuarta feature del ADR 0006,
  variante **L2**) con validación independiente y commit local, en el worktree
  `feature/dish-artwork-as-card-background`. Solo `feature-flow`: **sin push y sin PR** (publica
  `github-flow`).
- Completado:
  - Validación independiente (`validator`, contexto limpio, sin reutilizar la evidencia del implementer):
    veredicto **`accept`**. Re-ejecutó `bash init.sh` (exit 0, pero `testDebugUnitTest`/`lint` en
    `UP-TO-DATE`) y forzó `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` (BUILD SUCCESSFUL 3m34s,
    34/34 tareas `executed`) y `bash scripts/gradle.sh lint --rerun-tasks` (BUILD SUCCESSFUL 3m12s, 30/30);
    parseó por su cuenta los XML frescos (**98 suites / 742 tests / 0 fallos / 0 errores / 0 saltados**;
    `DishImageArtworkOverlayTest` 5/5, `RecipeBackgroundGateTest` 6/6, `RecipeArtworkImagePrecedenceTest`
    5/5) y el SARIF (13 resultados, **0 errores**, **0 bajo `app/src`**). Re-derivó la baseline en `HEAD`
    (`git grep -c '@Test' HEAD` = 96 ficheros / 730 anotaciones) ⇒ delta **+2 suites / +12 tests**.
  - Contraste de los 8 escenarios contra código y tests (apagado → `null` con **0** llamadas al resolutor y
    `RecipeArtwork` ni se compone; encendido con imagen → `GENERATED`/alfa `1f`/`Crop`/`scrim`; sin imagen,
    códec roto, lento, cancelado o tope agotado → `TYPOGRAPHIC` silencioso con `CancellationException`
    propagada; hueco **16:9** y hero 240 **sin salto**; fundido desde la única constante
    `DishImageArtworkPolicy.CROSSFADE_MILLIS` 250; firma `(recipe, modifier, glyphSize)` con `content`
    aditivo y **2** consumidores), la pureza (0 imports `android`/`androidx` en `DishImageArtworkOverlay`;
    **0 hex** en `ui/{artwork,home,detail,results,favorites}`, solo en `ui/theme/CardImageOverlay.kt`) y la
    disciplina de alcance (0 diff en `domain/**`, `data/**` con interruptor y tope intactos, `assets/**`,
    build/gradle, `init.sh`, `scripts/**`, `docs/design/concepts/**`, `taxonomy.json`; las **8 capturas**
    intactas; sin dependencias nuevas). Confirmó que el implementer **no** hizo commit/push/PR
    (`HEAD == origin/main == 4ba6a7c`).
  - La frase heredada de `verification` sobre los "cinco puntos de uso" se interpretó como **compatibilidad
    de contrato** (§1 de la spec) y se cumple con los 2 consumidores nuevos.
  - Sin hallazgos **Critical/High/Medium**; 3 **Low** informativos no bloqueantes (el test de resolución
    lenta usa tiempo virtual y solo verifica `null → TYPOGRAPHIC`; `CardImageOverlay.scrimStops` es API
    pública sin uso; KDoc obsoleto de `DishImageArtworkLoader.kt` que cita la cadena local) e informativos
    (variantes estrecha/compacta extrapoladas sin captura con OQ6 abierta, transitorio de 250 ms,
    `LocalRecipePhotoSource` sin lectores y `DishImageOrigin.CATEGORY` sin productor hasta
    `retire-recipe-photos`). Sin hallazgos de seguridad (mismo interruptor y tope con 0 diff; apagado corta
    antes de caché, red y cuota). Detalle en `docs/validations/dish-artwork-as-card-background.md`.
  - Aceptación persistida por el orquestador: `feature_list.json` → `accepted` con la evidencia del
    validator, bullet de estado y este registro en `PROGRESS.md`, y **commit local**
    (`feat: complete dish-artwork-as-card-background`) en la rama
    `feature/dish-artwork-as-card-background`.
- Verificación ejecutada: la del validador independiente (arriba), más `git status`/`git diff` del
  orquestador para aislar los ficheros de la feature antes del commit.
- Ficheros creados/cambiados: los de la sesión 109 (`ui/artwork/{RecipeArtwork,RecipeArtworkImagePrecedence,DishImageArtworkOverlay}.kt`,
  `ui/theme/CardImageOverlay.kt`, `ui/home/{RecipeFicha,RecipeCard,HomeScreen}.kt`,
  `ui/results/{ResultRecipeCard,ResultsScreen}.kt`, `ui/favorites/{FavoriteRecipeCard,FavoritesScreen}.kt`,
  `ui/detail/RecipeDetailScreen.kt`, `ui/MainShell.kt`, `ui/generator/GeneratorFlow.kt`,
  `ui/search/AiSearchFlow.kt`, los 3 tests de `ui/artwork`, `DESIGN.md`, `docs/technical-discovery.md`,
  `docs/adr/0006`, `docs/domain/taxonomy.md`, `docs/risks-and-open-questions.md`), más
  `docs/specs/dish-artwork-as-card-background.md`, `docs/validations/dish-artwork-as-card-background.md`,
  `feature_list.json` y este registro. Capturas sin cambios.
- Hallazgos relevantes: `accept` en la primera validación (sin ciclo `revise`); los 3 hallazgos Low no
  exigen re-validación y encajan en `retire-recipe-photos`.
- Riesgo o incógnita abierta: **revisión manual de la persona** en el emulador (tras el merge) de Inicio,
  Resultados, Favoritos (rejilla y lista) y Detalle en los tres estados (apagado / encendido con imagen /
  encendido sin imagen por modo avión o tope agotado), con atención a las variantes **estrecha** y
  **compacta** y al contraste del título durante el fundido; no automatizable desde WSL (`adb` de WSL no ve
  el emulador). OQ6 (¿sube el carrusel a 3:2?) sigue abierta.
- Siguiente paso: el orquestador de `github-flow` publica (push de `feature/dish-artwork-as-card-background`
  + PR contra `main`, reutilizando la issue si existe, con `Closes #<n>`). Después, la siguiente feature del
  plan es **`retire-recipe-photos`** (`area: data`, ya desbloqueada): rol **planner** (spec).
