# ADR 0005: Las imágenes de plato generadas por IA se piden por un puerto de dominio con prompt y clave canónica

## Estado

**Aceptado parcialmente (fases 1-2)** — features `dish-image-domain` (fase 1) y `dish-image-generator`
(fase 2), 2026-10-13. Fija la decisión de arquitectura y de política (puerto, prompt determinista y
clave canónica) e implementa el **adaptador real** de transporte + códec, y deja **pendientes**, de
forma explícita, las fases 3–7 de `docs/ai-dish-images-proposal.md` (caché, UI progresiva, descriptor
del modelo, referencia de estilo y set semilla). Ajusta **parcialmente**
`docs/adr/0004-recipe-photos-as-assets.md`: la cadena de assets sigue siendo válida y offline, pero
pasa a ser el **respaldo** de la imagen generada cuando la fase 4 la conecte; su afirmación "sin red en
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

## Consecuencias

- **Invalidación**: cualquier cambio de `STYLE_SUFFIX`, del orden de las cláusulas del prompt o de la
  normalización **obliga** a subir `DishImageCacheKey.VERSION` (regla durable de esta decisión).
- **Riesgo de cuota/latencia**: el **transporte** de la fase 2 ya existe, pero sin consumidores, así que
  el riesgo (~2 % de la cuota semanal y ~4 s por imagen) se materializa cuando lo consuman las fases
  3-4; queda registrado en `docs/risks-and-open-questions.md` con la mitigación prevista (clave
  compartida + caché persistente + tope por sesión + interruptor + set semilla).
- **La cadena de `recipe-photos` no cambia** y sigue siendo la única resolución en runtime; el
  offline-first no se rompe porque la generación se inserta *delante* y degrada en silencio.
- **Adaptador real, sin consumidores**: la fase 2 implementa el puerto, pero ninguna ruta de runtime lo
  llama todavía (fases 3-4), así que la app no gasta cuota ni cambia visualmente.
- **Títulos no latinos o solo símbolos** producen slug vacío y caen al respaldo por `id`/por defecto
  (cubierto por test); si alguna vez importa, lo resuelve el `dishKey` de la fase 5.
- **Pendiente (fases 3–7)**: caché en disco y memoria con evicción, UI progresiva con capturas
  obligatorias, `dishKey`/`imagePrompt` en el wire, `image-edit` con referencia de estilo y set semilla
  de platos frecuentes. Las preguntas abiertas de la propuesta §11 (Room vs ficheros, tamaño objetivo y
  tope de disco, set semilla, interruptor de usuario, entrada de la variante C) siguen abiertas y
  **no** se deciden aquí.
