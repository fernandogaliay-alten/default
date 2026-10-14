# ADR 0005: Las imágenes de plato generadas por IA se piden por un puerto de dominio con prompt y clave canónica

## Estado

**Aceptado parcialmente (fases 1-7)** — features `dish-image-domain` (fase 1), `dish-image-generator`
(fase 2), `dish-image-cache` (fase 3), `dish-image-progressive-ui` (fase 4, mitad de integración),
`dish-image-settings-toggle` (fase 4, mitad de ajuste de usuario), `dish-visual-descriptor` (fase 5),
`dish-style-reference` (fase 6) y `dish-image-seed` (fase 7), más el seguimiento de integración
`dish-visual-descriptor-wiring` (entrega en runtime de la fase 5) y la correctiva `dish-image-prompt-v2`
(prompt de imagen v2 + diagnóstico del origen), 2026-10-13. Fija la decisión de
arquitectura y de política (puerto, prompt determinista y clave canónica), implementa el **adaptador
real** de transporte + códec y la **caché** de disco + memoria, **conecta** el nivel generado por
delante de la cadena local con una progresión con crossfade, añade el **control de gasto** (interruptor
de usuario + tope de generaciones por sesión), fija en el **contrato con la IA** el descriptor visual
opcional (`dishKey`/`imagePrompt`) que alimenta esa misma política y **lo entrega en runtime** (el
almacén de sesión y el `requestFor` lo consumen), añade al adaptador el **ajuste con
una referencia de estilo versionada** (`/images/edits`, variante C) con respaldo por texto y, en la
fase 7, **empaqueta preparado de antemano** el set semilla de platos frecuentes resuelto por la clave
canónica **delante del generador**. Con eso la **fase 4 queda completa**, la **fase 5** entrega contrato
+ política pura + entrega en runtime, la **fase 6** entrega la capa de transporte del anclaje visual y
la **fase 7** la
tercera capa de la caché (assets semilla), de modo que la **lista de fases de la propuesta queda
completa**. Ajusta **parcialmente** `docs/adr/0004-recipe-photos-as-assets.md`: la cadena de assets
sigue siendo válida y offline, pero pasa a ser el **respaldo** de la imagen generada, ya conectada en
runtime; además, desde la fase 6 la foto por defecto de esa cadena (`img/recipes/default.<ext>`) es
también la **referencia de estilo** del ajuste. Su afirmación "sin red en tiempo de ejecución" se
mantiene mientras la cadena local exista (**retirada por ADR 0006**). Se **conserva** el asset neutro
`img/recipes/default.<ext>` como **ancla de estilo** de la fase 6 (OQ7 de `DESIGN.md`, resuelta el
2026-10-13): no se muestra en ninguna pantalla.

**Ajustado por `docs/adr/0006-typographic-recipe-cards.md`** (2026-10-13): la imagen generada **pasa a ser
capa de fondo** del hueco (deja de ser "la imagen de la receta"), supeditada al ajuste del usuario; el
**interruptor** ("Generar imágenes del plato") y el **tope por sesión** se mantienen. La **cadena local**
de fotos que le servía de respaldo **se retiró** con `retire-recipe-photos` (2026-10-14), así que la
**capa base** pasa a ser la **ficha tipográfica**.

**Retirada aplicada (2026-10-14, `retire-recipe-photos`)**: la cadena local **ya no existe** y la cadena
generada (memoria → disco → semilla → generador) es la **única** fuente de imagen de una receta; sin
imagen, la base es la ficha tipográfica (ADR 0006). El valor `CATEGORY` sale del vocabulario de
diagnóstico `data.image.DishImageOrigin`, que queda `GENERATED`/`SEED`/`GLYPH`. Los 30 assets de
`img/recipes/{seed,category}/**` (**5.954.912 B ≈ 5,68 MiB**) y `scripts/fetch-recipe-photos.sh` salieron
del APK y del repo; se **conserva** el asset neutro `img/recipes/default.jpg` (**137.359 B**) como
**ancla de estilo** de la fase 6 (OQ7 de `DESIGN.md`, resuelta el 2026-10-13): no se muestra en ninguna
pantalla. No cambian el puerto, el transporte, la caché, el tope, el interruptor ni la clave canónica
(`DishImageCacheKey.VERSION` sigue `dish-image-v2`).

## Contexto

`recipe-photos` (ADR 0004) resuelve la imagen con 31 fotos empaquetadas, pero las **18 de categoría son
genéricas**: una receta `ai` nueva (*arroz con pollo criollo*) cae en la foto genérica de su categoría y
la imagen no representa el plato concreto.

El proveedor de IA real (ADR 0003) expone generación de imágenes en el mismo gateway compatible con
OpenAI. La prueba manual de la propuesta (§3) midió: `1024x1024` soportado (`512` responde 400),
~**4,2 s** de latencia, respuesta en **`b64_json`** (sin URL utilizable), coste orientativo de
**~2 % de la cuota semanal por imagen** y correspondencia correcta con el plato.

La app es local-first (ADR 0002) y su verificación canónica corre en JVM sin emulador, sin red y sin
gastar cuota. El valor de la fase 1 es dejar la **política** escrita y probada antes de gastar nada, de
modo que las fases siguientes solo aporten transporte, persistencia y pintado.

## Decisión

1. **Puerto en el dominio, keyed por un request ya resuelto.** `com.sazon.app.domain.image.DishImageGenerator`
   es una interfaz `suspend fun generate(request: DishImageRequest): ByteArray?`. El adaptador real
   (`data.ai.image`, fase 2) es **transporte puro**: no conoce `Recipe` ni la Taxonomía, porque
   `DishImageRequest.of(taxonomy, recipe)` es el **único** punto de composición (`prompt`, `cacheKey`,
   `recipeId`). El request es un `data class` con defaults para que ampliarlo (tamaño objetivo, semilla
   de estilo, foto de referencia) sea **aditivo**.
2. **Fallo = `null`, no excepción.** Mismo contrato que `RecipePhotoAssets.resolve`: la app conserva la
   cadena local y nunca muestra un error por imagen (degradación silenciosa, propuesta §6.5). La
   **cancelación** de la corrutina sí se propaga: la fase 4 cancela al salir de pantalla o al hacer
   scroll. No se modela el formato ni los codecs en el dominio: WebP reducido y su extensión los
   deciden el adaptador (fase 2) y la caché (fase 3).
3. **Prompt puro y determinista** (`DishImagePrompt`, patrón `PromptBuilder`): una sola plantilla,
   `<prefijo> <identidad>[, <cocina>][, <detalle>][, <ingredientes clave>], <COMPOSITION>,
   <STYLE_SUFFIX>, <NEGATIVES>`, con `Fotografía gastronómica de`, `plato casero` como último recurso,
   `COMPOSITION` (`una ración completa del plato en primer plano, sobre fondo neutro`), `STYLE_SUFFIX`
   (`cerámica artesanal, luz natural suave, estilo editorial gastronómico`) y `NEGATIVES` (`sin texto,
   sin manos, sin personas, sin cubiertos ni marcas de agua, sin collage ni varios platos, sin fondo
   recargado`) en un único lugar. La **identidad** es **siempre** el título de la receta y degrada
   `título → etiqueta de categoría real → plato casero`; el `imagePrompt` del descriptor es el
   **detalle**, no un sustituto del título (desde `dish-image-prompt-v2`); los **ingredientes clave**
   (hasta `MAX_KEY_INGREDIENTS = 5`, no despensa, distintos y en el orden de la receta) y la
   cláusula de cocina se **omiten** cuando no aportan (nunca un marcador de "desconocida"). Los
   segmentos no vacíos se unen con `", "` y se colapsan las rachas de comas. Conserva
   mayúsculas y diacríticos: es texto para un modelo de imagen.
4. **Clave canónica versionada del *nombre del plato*** (`DishImageCacheKey`), no del `recipe.id` ni de
   la clasificación: dos recetas del mismo plato (búsquedas distintas, regeneraciones) **comparten
   imagen** y no repiten cuota. Normalización **tipográfica** (recorte y colapso de espacios, `NFD` y
   eliminación de `\p{Mn}+`, `lowercase(Locale.ROOT)` explícito, todo fuera de `[a-z0-9]` → `-`,
   colapso y recorte de guiones), con formato `dish-image-v2-<slug>`, apto como nombre de fichero y
   como columna de Room. `recipe.id` entra **solo como respaldo** cuando no hay título
   (`dish-image-v2-plato-<slug-id>`) para que dos recetas sin nombre no compartan imagen, y si tampoco
   hay `id` la clave cae a `dish-image-v2-plato`. **Subir `VERSION` es el único mecanismo de
   invalidación** cuando cambien la normalización o la plantilla del prompt: la caché se indexa por
   clave, no por prompt. Misma disciplina que `AiRecipeIds.ALGORITHM_VERSION`. `VERSION` pasó a
   `dish-image-v2` con `dish-image-prompt-v2` (cambio de plantilla); es la **primera** vez que la regla
   se aplica.
5. **La cadena local pasa a respaldo, no se retira.** La imagen generada (memoria → disco) se insertará
   **delante** de la cadena de `recipe-photos` en la fase 4. Sin red, fallo o timeout, la app sigue
   pintando exactamente lo de hoy. Sin dependencias nuevas en ninguna fase de esta política.

Alcance de la **fase 1** (feature `dish-image-domain`): solo la política pura y sus tests JVM. **No** hay
adaptador, ni red, ni Ktor, ni caché (memoria o disco), ni Room, ni UI, ni assets semilla, ni campos
nuevos en el contrato de la IA. Nadie consume todavía el puerto.

Alcance de la **fase 2** (feature `dish-image-generator`): el **adaptador real** en
`com.sazon.app.data.ai.image` (`DevExpertDishImageGenerator`), **transporte + códec puros**. `POST
/images/generations` con el alias `image` y `size 1024x1024` (`512` responde 400), decodificación
tolerante de `data[0].b64_json` y **reducción local** a WebP q80 de **960 px** de ancho (nunca
agranda). La política vive en `DishImagePolicy` (un único sitio): endpoint sobre la base del proveedor,
timeouts propios **más cortos** que los de chat (connect 10 s / socket 30 s / request 40 s) y **un solo
intento** (`MAX_ATTEMPTS = 1`): con ~2 % de cuota por imagen, reintentar duplicaría el gasto sin cambiar
la experiencia. Fallo = `null` **sin excepción y sin reintento**; la cancelación se propaga. El
adaptador **no** conoce el modelo de recetas ni la Taxonomía (usa `request.prompt`; `request.cacheKey`
es la identidad de la fase 3 y **no** viaja en la petición), no registra nada (sin `Log`) y **nadie lo
consume todavía**. **No** hay caché, ni UI, ni persistencia, ni cambios en el dominio, ni dependencias
nuevas. Ver `docs/specs/dish-image-generator.md`.

Alcance de la **fase 3** (feature `dish-image-cache`): la **caché** en `com.sazon.app.data.image`, capa
de datos sin UI ni consumidores. `DishImageResolver.imageFor(request)` orquesta **memoria → disco →
generador → persistir** con una `Mutex` **por clave** (dos peticiones simultáneas del mismo plato no
pagan dos veces la imagen) y contrato de fallo coherente con esta decisión (`null` = no disponible,
**nunca** excepción; la **cancelación** se propaga; la persistencia es *best-effort*: si el disco falla,
se devuelven igualmente los bytes generados). `FileDishImageCache` guarda los bytes **tal cual** (WebP
de la fase 2, **sin códec**: no importa `android.graphics`) como **ficheros en `cacheDir/dish-images`
cuyo nombre es la clave canónica** (`<cacheKey>.webp`), con **escritura atómica** (temporal en la misma
raíz + `renameTo`), **tope de 80 MiB** y **evicción LRU** por `lastModified` con toque de recencia al
leer y desempate por nombre (el fichero recién escrito nunca se evicta; la evicción solo borra
`<clave>.webp` dentro de su raíz). `MemoryDishImageCache` mantiene una **LRU de bytes** por clave
(16 entradas, patrón `AssetRecipePhotoSource`), no de bitmaps decodificados: la decodificación y su LRU
son de la fase 4 (UI), donde se pinta. `DishImageCachePolicy` concentra constantes y la selección pura
de víctimas; `AndroidDishImageCache.root(context)` es la **única** costura Android y la raíz se **inyecta**
(`File`) para que todo (salvo esa costura) se pruebe en JVM con un directorio temporal. **No** hay Room
(la decisión de la persona de 2026-10-13 resuelve §11.2 de la propuesta: ficheros, no índice en base),
ni UI, ni cableado, ni set semilla, ni cambios en el dominio ni dependencias nuevas. Ver
`docs/specs/dish-image-cache.md`.

Alcance de la **fase 4** (mitad de integración, feature `dish-image-progressive-ui`): la **costura en
`ui.artwork`** que conecta el nivel generado por delante de la cadena local, sin UI nueva. `RecipeArtwork`
conserva su firma (`recipe`, `modifier`, `glyphSize`) y la geometría aceptada de los 5 puntos de uso, lee
un **segundo seam** con default seguro (`LocalGeneratedRecipeArtworkSource`) y pinta **siempre** una
**capa base** (foto local o degradado + glifo) sobre la que **superpone** la imagen generada con un
**crossfade estándar** de 250 ms. `DishImageArtworkLoader` resuelve por `DishImageResolver` (memoria →
disco → generador de la fase 3), **decodifica** a `ImageBitmap` (`AndroidRecipeImageDecoder`, único
fichero de `ui.artwork` con `android.graphics`) y mantiene una **LRU de 16** imágenes decodificadas con
un límite de **2** resoluciones en vuelo (`DishImageArtworkPolicy`); `MainActivity` cablea la cadena
reutilizando el `aiClient` compartido y la caché de la fase 3. Contrato intacto: **fallo/lentitud/`null`/
códec roto = se conserva la foto local** (degradación silenciosa, nunca excepción ni error de UI) y la
**cancelación se propaga** (salir de pantalla o reciclar la tarjeta cancela la petición). **No** hay
interruptor de usuario ni tope de generaciones por sesión (la otra mitad de la fase 4, feature
`dish-image-settings-toggle`, con su punto de inyección en el proveedor del seam en `MainActivity`);
tampoco dependencias nuevas ni `Log`. la cadena de `recipe-photos` pasa formalmente a **respaldo**. Ver
`docs/specs/dish-image-progressive-ui.md`.

Alcance de la **fase 4** (mitad de ajuste de usuario, feature `dish-image-settings-toggle`): el
**control del gasto** en `data.image` + `ui.profile`, **sin pantalla nueva**. `DishImageSettings` reúne
el **interruptor** y el **tope por sesión** (en memoria; sesión = vida del proceso, **sin**
persistencia: no toca `UserProfile` ni Room) y expone un `StateFlow`
(`enabled`/`used`/`max`/`remaining`/`exhausted`) que alimenta la sección "Imágenes del plato" del
Perfil (`DishImageSettingsSection`, microcopy puro en `DishImageSettingsCopy`). Se gobierna en **dos
puntos**: el proveedor de `LocalGeneratedRecipeArtworkSource` en `MainActivity` gatea el interruptor
(`generatedIfEnabled`: apagado = `null` **sin** leer caché ni generar) y `GatedDishImageGenerator`
(**decorador** del puerto; el adaptador real y el puerto no se editan) impone el **tope** en el borde de
generación (una unidad por petición al proveedor; `null` **sin invocar** al delegado cuando está
agotado; los aciertos de memoria/disco **no** lo consumen). `DishImageGenerationBudget` es un contador
atómico de reserva estricta (la concurrencia es 2) y `DishImageSettingsPolicy` deja las constantes en un
único sitio (`DEFAULT_ENABLED = false` —opt-in: el interruptor arranca **apagado** y no se paga cuota
hasta que la persona lo encienda, feature `dish-image-settings-default-off`, 2026-10-14—,
`MAX_GENERATIONS_PER_SESSION = 12`, patrón
`AiProviderPolicy.RESPONSE_FORMAT_ENABLED`). Semántica **conservadora**: un **fallo** del proveedor
consume la unidad (es donde se paga) y una generación **cancelada** también (la reserva precede a la
llamada); apagar/encender **no** reinicia el tope. Degradación silenciosa intacta: apagado, agotado o
fallo → foto local, nunca error de UI; la `CancellationException` se propaga. Sin dependencias nuevas,
sin persistencia y sin `Log`. Ver `docs/specs/dish-image-settings-toggle.md`.

Alcance de la **fase 5** (feature `dish-visual-descriptor`): el **contrato con la IA** y la **política
pura** del descriptor visual opcional del plato (la entrega en runtime quedó diferida a su feature de
seguimiento `dish-visual-descriptor-wiring`). El wire gana dos campos
**opcionales** por receta, `dishKey` (plato canónico, pensado como clave compartida) e `imagePrompt`
(descripción visual del plato terminado): el DTO del wire los declara como `JsonElement?` para que un
tipo JSON equivocado **nunca** descarte la receta como `SCHEMA` (mismo espíritu que "al gusto") y el
valor puro `com.sazon.app.domain.image.DishVisualDescriptor` los limpia (`of`/`isEmpty`/`NONE`). Los tres
ganchos de la política pasan a aceptar el descriptor como **parámetro opcional con default**
(`DishImageCacheKey.of(recipe, descriptor)`, `DishImagePrompt.build(taxonomy, recipe, descriptor)` y
`DishImageRequest.of(taxonomy, recipe, descriptor)`): con `dishKey` utilizable la clave es la del plato
canónico (misma normalización; `"arroz con pollo"` y `"arroz con pollo fácil"` **comparten** imagen) y con
`imagePrompt` el prompt cambiaba **solo** la cláusula del plato (prefijo, cocina y `STYLE_SUFFIX` intactos).
*(Desde `dish-image-prompt-v2` el `imagePrompt` es el **detalle** y **no** sustituye al título: ver su
alcance más abajo.)*
`AiDecodeResult` expone los descriptores por `id` generado (`descriptors`) solo de recetas **aceptadas**
y, desde la feature de seguimiento `dish-visual-descriptor-wiring` (2026-10-13), **sí se entregan en
runtime**: el adaptador los expone por la costura aditiva `onDescriptors` (una vez por decode exitoso,
nunca en fallo/degradación/cancelación), `data.image.DishVisualDescriptorStore` los guarda **por sesión**
por `id` y `MainActivity` compone `DishImageRequest.of(taxonomy, recipe, descriptorFor(recipe.id))`, con
lo que el `dishKey` del modelo es la clave canónica compartida y el `imagePrompt` el prompt. Sin
descriptor (receta semilla, restaurada del Lote o un Favorito, o `id` desconocido) la app sigue con el
respaldo por título. `Recipe` y `RecipeValidator` **no** cambian
(el descriptor es cosmético: no se valida y **no se persiste**; el almacén es en memoria y se pierde al
recrear la `Activity`) y `AiRecipeIds.canonical` queda **intacto** (el
descriptor no entra en el `id`). **No** sube `DishImageCacheKey.VERSION` porque ni la normalización ni la
plantilla cambian, solo la **fuente** del plato cuando el modelo lo emite. Ver
`docs/specs/dish-visual-descriptor.md` y `docs/specs/dish-visual-descriptor-wiring.md`.

La **entrega en runtime** del descriptor ya **no** está pendiente: la cerró la feature de integración
`dish-visual-descriptor-wiring` (2026-10-13) cableando `AiDecodeResult.descriptors` →
`DishVisualDescriptorStore` → `DishImageArtworkLoader.requestFor` con el argumento `descriptor` que
`DishImageRequest.of` ya aceptaba. La limitación viva es que el descriptor es **efímero por sesión**: una
receta restaurada del Lote o un Favorito se resuelve por título hasta la siguiente llamada de IA.

Alcance de la **fase 6** (feature `dish-style-reference`): el **ajuste con referencia de estilo** en el
adaptador ya aceptado (`DevExpertDishImageGenerator`), **sin** tocar el puerto, el prompt ni la clave.
Cuando el adaptador recibe una `DishImageStyleReference` (bytes + `mediaType` + `fileName`) y el
interruptor `DishImagePolicy.STYLE_REFERENCE_ENABLED` está activo, pide la imagen con **una** petición
`POST /images/edits` (multipart) con las partes `model` = `image-edit` (alias del proveedor),
`prompt` = el prompt de la petición (idéntico, con su `STYLE_SUFFIX`), `size` = `1024x1024` y `image` =
los bytes de la referencia con su `Content-Type` y `filename`; la respuesta se decodifica con
`DishImagePayload` y se reduce con el `DishImageReducer` compartido. El respaldo **solo** se dispara
ante un **rechazo definitivo** del ajuste (`400/404/405/413/415/422`, `DishImagePolicy.EDIT_FALLBACK_STATUSES`)
o una carga `2xx` inutilizable, y en ese caso se reutiliza el camino de texto→imagen de la fase 2 con
el **mismo** `prompt` (nunca más de **dos** llamadas por `generate`). Un fallo de transporte/timeout o
un `401/403/429/5xx` devuelven `null` **sin** respaldo (misma disciplina que `MAX_ATTEMPTS = 1` y que
`AiProviderPolicy.requiresResponseFormatFallback`): el respaldo fallaría igual y duplicaría el gasto.
La referencia es un **asset ya empaquetado y versionado** (la foto neutra `img/recipes/default.<ext>` de
`recipe-photos`, resuelta por extensiones con `DishImageStyleReferencePolicy`), **nunca** una imagen
subida por el usuario: no hay permisos, galería, cámara, `mask`, `n`, `response_format` ni persistencia
nueva. El adaptador sigue siendo **transporte puro y mudo** (sin `Log`, sin `Recipe`/`Taxonomía`, sin
`cacheKey`/`recipeId` en la petición) y `MainActivity` carga la referencia de forma *best-effort*
(`AssetDishImageStyleReference`; `null` = comportamiento exacto de la fase 2). `DishImageCacheKey.VERSION`
**no** sube (el prompt y su normalización no cambian; la referencia **añade** un ancla visual, no
sustituye el `STYLE_SUFFIX`), así que las imágenes ya cacheadas conservan el aspecto anterior hasta que
la LRU las evicte (**estilo mixto transitorio aceptado**; subir a `dish-image-v2` sería uniforme pero
repagaría cuota por cada plato cacheado). Consecuencia de gasto **acotada y reversible**: un rechazo del
ajuste cuesta **una llamada extra** (una desechada) para una unidad del tope por sesión, y
`STYLE_REFERENCE_ENABLED = false` es el rollback de una línea. Sin dependencias nuevas (el multipart vive
en `ktor-client-core`) y sin UI. Ver `docs/specs/dish-style-reference.md`.

Alcance de la **fase 7** (feature `dish-image-seed`): el **set semilla de platos frecuentes** como
**tercera capa de la caché** (propuesta §5), **sin** tocar el puerto, el prompt, la clave, la caché de
disco, el ajuste de sesión ni la UI. `DishImageResolver` recibe un parámetro **nuevo al final con
default** (`seed: DishImageSeedSource? = null`) y amplía su orden a **memoria → disco → semilla →
generador → persistir**: si el disco no tiene copia y la fuente semilla devuelve bytes no vacíos, se
guardan en **memoria** y se devuelven **sin** escribir en disco y **sin** invocar al generador (el
asset ya viaja en el APK; duplicarlo consumiría el tope de 80 MiB y podría perderse por evicción o un
borrado de `cacheDir`). El **disco gana a la semilla** (solo decide el caso degenerado de un plato con
copia generada antes de existir el asset: se conserva la semántica de acierto de la fase 3). La
identidad del asset es la **clave canónica del plato** (`DishImageCacheKey`, ya presente en el
`DishImageRequest`), y la convención es `assets/img/dishes/seed/<cacheKey>.<ext>` con la extensión
probada en orden `webp → jpg → png` (se reutiliza `RecipePhotoAssets.EXTENSIONS`): el mapa clave→ruta
es **lógico y puro** (`data.DishImageSeedAssets`: `ROOT`, `EXTENSIONS`, `SAFE_KEY` = `[a-z0-9-]+`,
`isSafeKey`, `path`, `resolve(cacheKey, exists[, root])`, con `exists` **inyectado**), la lectura real
es la costura Android `data.AssetDishImageSeedSource` (`Dispatchers.IO`, `null` **sin lanzar** si el
asset falta o está vacío; **único** fichero nuevo con `android.*`) y el seam funcional
`data.DishImageSeedSource` permite una fuente falsa en JVM sin red ni cuota. `MainActivity` gana **una
línea** (`seed = AssetDishImageSeedSource(applicationContext)`), detrás del **mismo gate** del
interruptor de la fase 4 (apagado = sin imagen: queda la ficha tipográfica, tampoco las semilla) y **sin** consumir el tope
por sesión (la semilla nunca llega al borde de generación). Restricciones: `DishImageCacheKey.VERSION`
**no** sube (ni la normalización ni la plantilla cambian; el asset solo **añade** un origen de bytes) y
**ningún** plato con foto propia (las 12 recetas semilla tenían la suya en
`img/recipes/seed/<recipeId>.<ext>`, hoy **retirada** con `retire-recipe-photos`) recibe
asset semilla, porque pisaría su foto propia. El set se prepara con
`scripts/prepare-dish-image-seed.sh`, **herramienta de una sola vez fuera de `init.sh` y de runtime**
(nadie la invoca), idempotente (`--force` para regenerar), con lista exacta de claves ordenadas y
plato/cocina, prompt **espejo** de `DishImagePrompt` con la etiqueta de cocina leída de
`docs/domain/taxonomy.json`, modo proveedor (`/images/edits` con la referencia neutra
`img/recipes/default.<ext>` y respaldo **acotado por estado** a `/images/generations`) o modo
`--source <dir>` (**sin gastar cuota**), conversión a WebP q80 ≤960 px cuando hay herramienta,
inventario y presupuesto **≤ 2 MiB** con `exit ≠ 0` ante cualquier desvío (**nunca** un empaquetado
parcial silencioso). Sin dependencias nuevas y sin capturas (`area: data`; el set es **contenido**, no
dirección visual). Ver `docs/specs/dish-image-seed.md`.

Alcance de la correctiva **`dish-image-prompt-v2`** (2026-10-13, `area: integration`): (1) la
**plantilla v2** del prompt (identidad **siempre** = título, detalle del descriptor, ingredientes clave,
`COMPOSITION` y `NEGATIVES` reforzados) y la instrucción más rica del `imagePrompt` en el
`OUTPUT_CONTRACT` de `PromptBuilder` (sigue opcional, se pide descripción visual del plato terminado con
sus ingredientes principales y su aspecto); (2) el **bump obligatorio** de `DishImageCacheKey.VERSION` a
`dish-image-v2` y el **renombrado** (`git mv`, bytes idénticos, sin cuota) de los 6 assets semilla a
`dish-image-v2-*.webp`, con `EXPECTED_SEED_KEYS` del script en v2 y su espejo del prompt acotado
(título+cocina+composición+estilo+negativos; no puede reproducir las cláusulas de `imagePrompt` ni de
ingredientes) anotado como deriva conocida; (3) el **diagnóstico de debug del origen** de la imagen
(`data.image.DishImageOrigin` + `DishImageOriginSink`, `DishImageResolver.onResolved` aditivo con origen
recordado por clave, `RecipeArtworkImagePrecedence.originOf`, seam `ui.artwork.DishImageOriginDiagnostics`
y `Log.d("SazonDishImage", …)` en `MainActivity` **solo** con `BuildConfig.DEBUG`), **sin** cambiar el
pintado, la caché, el tope ni la red. **No** se toca el puerto, el transporte, la caché, la cadena local,
el descriptor ni la UI más allá de la llamada lateral de diagnóstico; sin dependencias nuevas y sin
capturas (`area: integration`). Ver `docs/specs/dish-image-prompt-v2.md`.

## Alternativas consideradas

1. **Usar `recipe.id` como clave de caché.** Identidad de *receta*, no de *plato*: el mismo plato
   buscado dos veces (o regenerado por la IA con otro `id`) pagaría dos veces la misma imagen. La
   propuesta §5 lo descarta expresamente. Descartado.
2. **Fusión semántica de nombres** (`"arroz con pollo"` ≡ `"arroz con pollo fácil"` ≡ `"receta de arroz
   con pollo"`), como sugería la lectura amplia de la propuesta §5. Descartado en esta fase
   (**desviación consciente y acotada**): preferimos una imagen redundante (coste de cuota) a una
   imagen **equivocada** (colisión), y la fusión difusa no es determinista ni testeable. La igualdad
   conceptual es responsabilidad de la fase 5, donde el `dishKey` lo emite el modelo ya normalizado
   conceptualmente; la fragmentación actual es una frontera conocida y probada por test.
3. **Jerarquía de excepciones de generación** (`DishImageUnavailableException`) o un tipo valor
   `DishImage(bytes, mimeType)`. Añade tipos que la fase 2 tendría que redecidir y ningún consumidor
   necesita distinguir "falló" de "no se intentó" para decidir el pintado; `RecipeGenerator` ya fija el
   precedente de no inventar el modelo de errores antes de que exista adaptador. Descartado.
4. **Añadir un campo de imagen a `Recipe`.** Reitera el descarte del ADR 0004 (convención duplicada en
   el dominio y migración de esquema). Descartado.
5. **Resolver la imagen en la UI (fase 4) en vez de en el dominio.** La política (prompt, clave) no se
   podría probar en JVM ni compartir con la caché de la fase 3. Descartado.
6. **Indexar la caché en una base Room propia** (como `favorites.db` / `suggestion_batch.db`), en vez de
   en ficheros. La imagen **ya está** codificada en un fichero y la clave canónica **ya es** un nombre de
   fichero válido, así que el índice no necesita consultas: una base solo añadiría esquema, migraciones y
   una **segunda fuente de verdad** que puede desincronizarse de los bytes. Descartado en la fase 3
   (decisión de la persona, 2026-10-13, que resuelve la pregunta abierta §11.2 de la propuesta). Si el
   crecimiento de ficheros llegara a importar, la salida natural es una consulta puntual del índice, no
   un modelo persistente nuevo.
7. **Guardar en la LRU de memoria los bitmaps decodificados** (lectura literal de la propuesta §5). Se
   descarta en la fase 3: (a) mantiene `data.image` sin `android.graphics` y todo su test en JVM salvo
   la costura; (b) la decodificación y *su* LRU de bitmaps pertenecen a la fase 4 (UI), que es donde se
   pinta con scroll y donde ya existe el patrón aceptado (LRU 16); (c) el valor de la memoria aquí es no
   repetir disco y no repetir proveedor. La LRU guarda **bytes**.
8. **Resolver el set semilla desde la cadena local** (`RecipePhotoSource`/`RecipePhotoAssets`, keyed por
   `Recipe`) **o con un decorador del puerto `DishImageGenerator`.** Descartado en la fase 7: (a) la
   identidad de un asset semilla es la **clave canónica del plato**, que ya viaja en el
   `DishImageRequest` que consume el resolutor, mientras que la cadena local se indexa por `Recipe`
   (`recipeId`/`categoryId`) y obligaría a duplicar el cálculo de la clave en la capa de assets,
   tocando un seam Compose ya aceptado; (b) un decorador haría pasar los bytes semilla por el camino de
   "generado", de modo que el resolutor los **persistiría** en `cacheDir` y consumirían el **tope por
   sesión** como si fueran generación — justo lo que la fase 7 evita. El parámetro **opcional al final**
   del resolutor mantiene la fase 7 aditiva y el comportamiento de la fase 3 intacto.
9. **Persistir el asset semilla en `cacheDir` en la primera lectura.** Duplica bytes que ya viajan en el
   APK, consume el presupuesto de 80 MiB, puede perderse por evicción LRU o por un borrado del sistema
   (`cacheDir` es descartable) y convertiría una lectura determinista en un acierto opcional.
   Descartado: se cachean los bytes solo en la **memoria** del resolutor (y las decodificadas en la LRU
   del loader de la fase 4).

## Consecuencias

- **Invalidación**: cualquier cambio de `STYLE_SUFFIX`, del orden de las cláusulas del prompt o de la
  normalización **obliga** a subir `DishImageCacheKey.VERSION` (regla durable de esta decisión). La fase 5
  **no** la sube: el descriptor solo cambia de **dónde sale** el plato/el slug cuando el modelo lo emite,
  no la normalización ni la forma de la plantilla. La fase 6 **tampoco**: el ajuste con referencia no
  cambia el prompt (la referencia **añade** un ancla visual, no sustituye el `STYLE_SUFFIX`), así que la
  plantilla —uno de los dos disparadores de invalidación— queda intacta. La fase 7 **tampoco** (el set
  semilla solo añade un origen de bytes). La **primera** aplicación de la regla es `dish-image-prompt-v2`
  (plantilla v2), que sube `VERSION` a `dish-image-v2`: los `.webp` de disco de la versión anterior quedan
  **huérfanos** hasta que la LRU los evicta (acotado por el tope de 80 MiB) y los 6 **assets semilla** se
  **renombran** (mismos bytes: no se vuelve a pagar cuota; regenerarlos con
  `scripts/prepare-dish-image-seed.sh --force` es una decisión aparte de la persona).
- **Riesgo de cuota/latencia**: el **transporte** (fase 2), la **caché** (fase 3), la **costura de UI**
  (fase 4) y el **control de gasto** (fase 4, ajuste de usuario) ya existen, así que el riesgo (~2 % de
  la cuota semanal y ~4 s por imagen) **se materializa** al navegar. Está registrado en
  `docs/risks-and-open-questions.md` con la mitigación implementada (clave compartida por nombre de
  plato + caché persistente con evicción LRU y tope de 80 MiB + LRU de bytes en memoria +
  **concurrencia limitada a 2** + **cancelación al salir de pantalla/hacer scroll** +
  **tope de generaciones por sesión** + **interruptor de usuario**) y, desde la fase 7, el **set
  semilla** adelanta la primera imagen de los platos frecuentes del demo: esos platos **no** gastan
  cuota ni esperan red en runtime a cambio de un coste de preparación **de una sola vez** (~2 % de cuota
  por plato, fuera del gate del interruptor y evitable con `--source`).
- **Crecimiento de almacenamiento acotado**: el tope de 80 MiB con evicción LRU deja de ser hipotético en
  la fase 3; el espacio lo ocupa `cacheDir/dish-images`, que es **descartable** por el sistema (si el
  sistema lo vacía, un acierto falla y solo se regenera, volviendo a gastar cuota: comportamiento
  esperado). Subir `DishImageCacheKey.VERSION` deja huérfanos los `.webp` de la versión anterior hasta que la
  LRU los evicte (acotado por el tope; una feature posterior puede borrar por prefijo de versión).
- **La cadena de `recipe-photos` fue el respaldo hasta su retirada**: la imagen generada se inserta
  *delante* y degrada en
  silencio (fallo, lentitud, cancelación o sin red), así que el offline-first no se rompe: sin red se ve
  la **ficha tipográfica** (antes de ADR 0006, la foto local o el degradado + glifo; hoy la cadena local
  **ya no existe**).
- **Adaptador real, caché y costura de UI, con consumidor**: la fase 4 conecta el puerto y su caché en
  `MainActivity`/`ui.artwork`, de modo que la app **sí** puede gastar cuota y sustituir la imagen
  visualmente; el gasto queda acotado por la concurrencia (2), la clave compartida y la caché persistente.
- **Títulos no latinos o solo símbolos** producen slug vacío y caen al respaldo por `id`/por defecto
  (cubierto por test); si alguna vez importa, lo resuelve el `dishKey` de la fase 5.
- **Estilo mixto transitorio por la fase 6**: como `DishImageCacheKey.VERSION` **no** subió en las fases
  5-6, las imágenes ya cacheadas (fases 2-5) mantenían su aspecto anterior hasta que la LRU las evicta,
  así que durante un tiempo convivían imágenes "de texto" y "ajustadas" (sin error y sin gasto extra). La
  dicotomía queda **resuelta de facto** por `dish-image-prompt-v2`: el bump a `dish-image-v2` invalida
  todas las claves y uniforma lo nuevo con el prompt v2 (la generada anterior se repaga **una vez** por
  plato, acotada por la LRU y el tope por sesión), que es el coste explícito de aplicar la plantilla.
- **Entregado y ya sin pendientes de la fase 5**: la **entrega en runtime** del descriptor la cerró
  `dish-visual-descriptor-wiring` (2026-10-13): `onDescriptors` en el adaptador + almacén de sesión en
  `data.image` + `requestFor` con `descriptorFor(recipe.id)`. La limitación viva es que el descriptor es
  **efímero por sesión** (no se persiste; una receta restaurada del Lote o un Favorito se resuelve por
  título hasta la siguiente llamada de IA) y la consecuencia de caché ya anotada (título → `dishKey`:
  una generación extra la primera vez). La fase 6 entrega el ajuste con
  referencia versionada y respaldo por texto, con la entrada de la variante C **cerrada** en
  `DishImagePolicy` (`IMAGES_EDITS_PATH`, `EDIT_MODEL_ALIAS`, `EDIT_FALLBACK_STATUSES`,
  `STYLE_REFERENCE_ENABLED`); la **fase 7** entrega el set semilla (assets versionados por clave
  canónica delante del generador, con la preparación de una sola vez); la fase 4 se entrega
  **completa** (integración + ajuste de usuario), ambas mitades con capturas **omitidas por desviación
  autorizada** registrada en `DESIGN.md` (issues #98 y #101); la revisión puramente visual la hace la
  persona tras el merge. De las preguntas abiertas de la propuesta §11, la decisión Room vs ficheros queda
  **resuelta** en la fase 3 (ficheros en `cacheDir`), el **interruptor de usuario y el tope por sesión
  quedan resueltos** en la fase 4 (constantes en `DishImageSettingsPolicy`), la **coherencia conceptual
  del mismo plato queda cubierta** en la fase 5 vía `dishKey` del modelo (ya **entregado en runtime**) y
  el **set semilla queda
  resuelto** en la fase 7 (assets empaquetados, con `--source` como vía sin cuota); siguen abiertas el
  tamaño objetivo y el tope de disco (fijados aquí en 960 px WebP q80 y 80 MiB, susceptibles de ajuste) y
  la **lista concreta** de platos frecuentes (hoy 6, ampliable de forma aditiva: una línea en el script
  y un título en el test).
