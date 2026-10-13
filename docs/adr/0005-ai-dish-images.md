# ADR 0005: Las imágenes de plato generadas por IA se piden por un puerto de dominio con prompt y clave canónica

## Estado

**Aceptado parcialmente (fases 1-4)** — features `dish-image-domain` (fase 1), `dish-image-generator`
(fase 2), `dish-image-cache` (fase 3), `dish-image-progressive-ui` (fase 4, mitad de integración) y
`dish-image-settings-toggle` (fase 4, mitad de ajuste de usuario), 2026-10-13. Fija la decisión de
arquitectura y de política (puerto, prompt determinista y clave canónica), implementa el **adaptador
real** de transporte + códec y la **caché** de disco + memoria, **conecta** el nivel generado por
delante de la cadena local con una progresión con crossfade y añade el **control de gasto**: el
interruptor de usuario (apagado = solo foto local) y el **tope de generaciones por sesión** (una unidad
por petición al proveedor, impuesta por un decorador del puerto). Con eso la **fase 4 queda completa**;
deja **pendientes**, de forma explícita, las fases 5–7 de `docs/ai-dish-images-proposal.md` (descriptor
del modelo, referencia de estilo y set semilla). Ajusta **parcialmente**
`docs/adr/0004-recipe-photos-as-assets.md`: la cadena de assets sigue siendo válida y offline, pero
pasa a ser el **respaldo** de la imagen generada, ya conectada en runtime; su afirmación "sin red en
tiempo de ejecución" se mantiene para la cadena local.

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
   `<prefijo> <plato>[, <cocina>], <coletilla de estilo>`, con
   `Fotografía gastronómica de`, `plato casero` como último recurso y la coletilla de estilo
   (`cerámica artesanal, luz natural suave, estilo editorial gastronómico, sin texto ni manos`) en un
   único lugar. El plato degrada `título → etiqueta de categoría real → plato casero`; la cláusula de
   cocina se **omite** si el id no existe o está vacío (nunca un marcador de "desconocida"). Conserva
   mayúsculas y diacríticos: es texto para un modelo de imagen.
4. **Clave canónica versionada del *nombre del plato*** (`DishImageCacheKey`), no del `recipe.id` ni de
   la clasificación: dos recetas del mismo plato (búsquedas distintas, regeneraciones) **comparten
   imagen** y no repiten cuota. Normalización **tipográfica** (recorte y colapso de espacios, `NFD` y
   eliminación de `\p{Mn}+`, `lowercase(Locale.ROOT)` explícito, todo fuera de `[a-z0-9]` → `-`,
   colapso y recorte de guiones), con formato `dish-image-v1-<slug>`, apto como nombre de fichero y
   como columna de Room. `recipe.id` entra **solo como respaldo** cuando no hay título
   (`dish-image-v1-plato-<slug-id>`) para que dos recetas sin nombre no compartan imagen, y si tampoco
   hay `id` la clave cae a `dish-image-v1-plato`. **Subir `VERSION` es el único mecanismo de
   invalidación** cuando cambien la normalización o la plantilla del prompt: la caché se indexa por
   clave, no por prompt. Misma disciplina que `AiRecipeIds.ALGORITHM_VERSION`.
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
único sitio (`DEFAULT_ENABLED = true`, `MAX_GENERATIONS_PER_SESSION = 12`, patrón
`AiProviderPolicy.RESPONSE_FORMAT_ENABLED`). Semántica **conservadora**: un **fallo** del proveedor
consume la unidad (es donde se paga) y una generación **cancelada** también (la reserva precede a la
llamada); apagar/encender **no** reinicia el tope. Degradación silenciosa intacta: apagado, agotado o
fallo → foto local, nunca error de UI; la `CancellationException` se propaga. Sin dependencias nuevas,
sin persistencia y sin `Log`. Ver `docs/specs/dish-image-settings-toggle.md`.

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

## Consecuencias

- **Invalidación**: cualquier cambio de `STYLE_SUFFIX`, del orden de las cláusulas del prompt o de la
  normalización **obliga** a subir `DishImageCacheKey.VERSION` (regla durable de esta decisión).
- **Riesgo de cuota/latencia**: el **transporte** (fase 2), la **caché** (fase 3), la **costura de UI**
  (fase 4) y el **control de gasto** (fase 4, ajuste de usuario) ya existen, así que el riesgo (~2 % de
  la cuota semanal y ~4 s por imagen) **se materializa** al navegar. Está registrado en
  `docs/risks-and-open-questions.md` con la mitigación implementada (clave compartida por nombre de
  plato + caché persistente con evicción LRU y tope de 80 MiB + LRU de bytes en memoria +
  **concurrencia limitada a 2** + **cancelación al salir de pantalla/hacer scroll** +
  **tope de generaciones por sesión** + **interruptor de usuario**) y la pendiente (**set semilla de
  platos frecuentes**, fase 7).
- **Crecimiento de almacenamiento acotado**: el tope de 80 MiB con evicción LRU deja de ser hipotético en
  la fase 3; el espacio lo ocupa `cacheDir/dish-images`, que es **descartable** por el sistema (si el
  sistema lo vacía, un acierto falla y solo se regenera, volviendo a gastar cuota: comportamiento
  esperado). Subir `DishImageCacheKey.VERSION` deja huérfanos los `dish-image-v1-*.webp` hasta que la
  LRU los evicte (acotado por el tope; una feature posterior puede borrar por prefijo de versión).
- **La cadena de `recipe-photos` pasa a respaldo**: la imagen generada se inserta *delante* y degrada en
  silencio (fallo, lentitud, cancelación o sin red), así que el offline-first no se rompe: sin red se ve
  exactamente lo de hoy (foto local o degradado + glifo).
- **Adaptador real, caché y costura de UI, con consumidor**: la fase 4 conecta el puerto y su caché en
  `MainActivity`/`ui.artwork`, de modo que la app **sí** puede gastar cuota y sustituir la imagen
  visualmente; el gasto queda acotado por la concurrencia (2), la clave compartida y la caché persistente.
- **Títulos no latinos o solo símbolos** producen slug vacío y caen al respaldo por `id`/por defecto
  (cubierto por test); si alguna vez importa, lo resuelve el `dishKey` de la fase 5.
- **Pendiente**: las fases 5–7 (`dishKey`/`imagePrompt` en el wire, `image-edit` con referencia de
  estilo y set semilla de platos frecuentes). La fase 4 se entrega **completa** (integración + ajuste de
  usuario), ambas mitades con capturas **omitidas por desviación autorizada** registrada en `DESIGN.md`
  (issues #98 y #101); la revisión puramente visual la hace la persona tras el merge. De las preguntas
  abiertas de la propuesta §11, la decisión Room vs ficheros queda **resuelta** en la fase 3 (ficheros
  en `cacheDir`) y el **interruptor de usuario y el tope por sesión quedan resueltos** en la fase 4
  (constantes en `DishImageSettingsPolicy`); siguen abiertas el tamaño objetivo y el tope de disco
  (fijados aquí en 960 px WebP q80 y 80 MiB, susceptibles de ajuste), el set semilla y la entrada de la
  variante C.
