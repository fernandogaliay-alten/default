# Imágenes de plato generadas por IA (post-MVP): propuesta

> **Estado: PROPUESTA. No aceptada, no planificada y fuera del MVP.**
> Este documento no cambia el alcance del MVP ni el estado de ninguna feature. Existe para poder
> retomar la implementación en una fase posterior sin rehacer el análisis. Se documenta el enfoque
> **A + C + B** (texto→imagen sin base, ajuste de estilo con `image-edit`, y descriptor de plato
> emitido por el propio modelo) como dirección recomendada.

## 1. Encuadre

Hoy la imagen de una receta se resuelve con la cadena aceptada de `recipe-photos`
(`docs/adr/0004-recipe-photos-as-assets.md`):

```
memoria (LRU 16) → assets propios → assets de categoría → default → placeholder (degradado + glifo)
```

`data.RecipePhotoAssets` (puro) + `data.AssetRecipePhotoSource` (Android) + `ui.artwork.RecipeArtwork`
(Compose) implementan esa cadena. Funciona offline y sin dependencias nuevas, pero tiene un límite
conocido: las **18 fotos de categoría** son genéricas. Una receta `ai` nueva (p. ej. *arroz con pollo
criollo*) cae en la foto genérica de su categoría, de modo que la imagen **no siempre representa el
plato concreto**.

El proveedor de IA real (`docs/adr/0003-ai-provider.md`) expone **generación de imágenes** en el mismo
gateway compatible con OpenAI. Esta propuesta explora usar esa capacidad para que la imagen **sí**
corresponda al plato —el buscado o el devuelto por la IA— sin romper el offline-first ni la
arquitectura de puertos.

Control de versiones del análisis: nada de lo aquí descrito está implementado. `feature_list.json`
sigue siendo la fuente de verdad del trabajo en curso y **no** se ha tocado.

## 2. Decisión propuesta

Se propone combinar tres piezas; cada una resuelve un problema distinto y ninguna es requisito de la
otra.

| Pieza | Qué es | Qué resuelve |
|---|---|---|
| **A. Texto→imagen sin base** | `POST /images/generations` con un prompt de plato construido desde la receta. | Que la imagen represente el plato concreto. |
| **C. Ajuste de estilo con `image-edit`** | `POST /images/edits` usando una de las fotos aceptadas como **referencia de estilo**. | Coherencia visual con `DESIGN.md` entre todas las imágenes. |
| **B. Descriptor de plato del modelo** | Un campo corto (p. ej. `dishKey` + `imagePrompt`) que la **misma** llamada de chat que genera la receta ya emite. | Clave de caché canónica y prompt visual coherente por construcción, sin llamada extra. |

El resultado se cachea y entra **delante** de la cadena actual de `recipe-photos`, que pasa a ser el
respaldo:

```
imagen generada (memoria → disco) → assets propios → assets de categoría → default → placeholder (glifo)
```

## 3. Evidencia: prueba real contra el proveedor

Se ejecutó una prueba manual (una vez, fuera del gate) contra
`https://inference.devexpert.io/v1/images/generations` con la clave de `local.properties`, midiendo
latencia y forma de respuesta.

Petición:

```json
{ "model": "image",
  "prompt": "Fotografía cenital de un plato de arroz con pollo criollo, cerámica artesanal, luz natural suave, estilo editorial gastronómico, sin texto ni manos",
  "size": "1024x1024" }
```

| Tamaño | HTTP | Latencia | Respuesta | Verdicto |
|---|---|---|---|---|
| `512x512` | **400** | 0,8 s | `"Image size 512 is not supported for this model"` | **no soportado** |
| `1024x1024` | 200 | **4,2 s** | `data[0].b64_json` (869 KB, JPEG 1024×1024); `url` vacío; `revised_prompt: null` | correcto |

Hallazgos que condicionan el diseño:

- **Detrás del alias `image` hay un modelo Gemini** (el error lo revela vía LiteLLM). El tamaño mínimo
  útil es 1024; **512 no existe**. Para obtener un tamaño "mediano" hay que **generar a 1024 y reducir
  localmente** (p. ej. WebP q80 a 960 o 512), lo que baja de ~870 KB a ~30–60 KB. Reducir no ahorra
  cuota, solo disco y decodificación.
- **La latencia es de ~4,2 s** (una muestra), muy inferior a los 28–115 s del chat. Es compatible con
  una carga **progresiva**: la UI puede pintar la foto local al instante y sustituirla cuando llegue.
- La respuesta llega como **`b64_json`** (sin URL utilizable), así que la app **decodifica y guarda la
  imagen ella misma**; no depende de una URL externa que pueda caducar.
- Coste orientativo documentado por el proveedor: **~2 % de la cuota semanal por imagen**, con
  independencia del tamaño.
- La imagen resultante coincide con el plato (revisada por la persona). Confirmado el enfoque A.

Los artefactos de la prueba (imagen, respuestas crudas) se guardaron fuera del repo, en
`/tmp/opencode/` y una copia en `C:\Users\fernando.galiay\Downloads\sazon-image-test\`.

## 4. Arquitectura propuesta

Se mantiene el patrón ya aceptado: **puerto en el dominio, adaptador en `data`/`data.ai`, prompt puro
testeable en JVM y degradación silenciosa**.

| Pieza | Módulo / nombre propuesto | Notas |
|---|---|---|
| Puerto | `com.sazon.app.domain.image.DishImageGenerator` | Interfaz `suspend`, keyed por una `DishImageRequest`. Sin Android, sin Ktor. |
| Prompt puro | `com.sazon.app.domain.image.DishImagePrompt` | Construye el prompt desde la `Recipe` + la Taxonomía + la coletilla de estilo. `object` puro, testeado en JVM (patrón `PromptBuilder`). |
| Clave pura | `com.sazon.app.domain.image.DishImageCacheKey` | Canonicaliza (`dishKey`) a una clave estable. Determinista y sin reloj. |
| Adaptador real | `com.sazon.app.data.ai.image.DevExpertDishImageGenerator` | Ktor contra `/images/generations` y `/images/edits`, decodifica `b64_json`. Única capa que conoce Ktor (regla de ADR 0003). |
| Caché de disco | `com.sazon.app.data.image` (base Room propia o ficheros en `cacheDir`) | Mismo patrón que `favorites.db` / `suggestion_batch.db` si se indexa en Room. |
| Costura Compose | `ui.artwork` (ampliar la resolución de imagen) | `RecipeArtwork` conserva firma y geometría (16:10, 96x96, alto 96, hero 240); solo gana un nivel por delante. |

### 4.1 Descriptor de plato (variante B)

El contrato del prompt (`docs/domain/taxonomy.md` §4) vive en `PromptBuilder`. Se propone un campo
**opcional** en el wire de la IA:

```jsonc
{
  "dishKey": "arroz-con-pollo-criollo",   // canónico, específico del plato terminado
  "imagePrompt": "arroz amarillo con pollo desmenuzado, guisantes, zanahoria y aceitunas…",
  // ... resto del contrato §4
}
```

- `dishKey` es la **clave de caché canónica**: dos búsquedas distintas del mismo plato comparten
  imagen y no vuelven a gastar cuota.
- `imagePrompt` es la descripción visual, coherente **por construcción** porque la emite el mismo
  modelo que inventó el plato. No cuesta una llamada extra.
- Es un campo **ignorado por `RecipeValidator`** (la puerta dura no cambia) y por el dominio: si el
  modelo no lo emite, se degrada a construir el prompt desde los campos ya existentes de la `Recipe`
  (`title`, `categoryId`, `cuisineId`, ingredientes principales).
- Riesgo de `dishKey`: si es demasiado grueso ("pollo") habría colisiones. Debe pedirse **específico
  del plato terminado** y usarse como clave **exacta** (sin fusión difusa).

### 4.2 Prompt y estilo

`DishImagePrompt.build(...)` produce algo del orden de:

> `Fotografía gastronómica de <plato>, <cocina>, cerámica artesanal, luz natural suave, estilo
> editorial, sin texto ni manos`

La coletilla de estilo es **fija** y vive en un único lugar (pura, testeada). La variante **C** puede
sustituir parte de ese anclaje por `/images/edits` con una foto aceptada (p. ej. `default.jpg`) como
referencia, para fijar el estilo de la casa con más fuerza que un simple sufijo de texto.

## 5. Estrategia de caché (las tres capas)

Cada capa elimina un coste distinto; no son redundantes.

| Capa | Qué guarda | Alcance | Coste que elimina |
|---|---|---|---|
| **Memoria** | Bitmaps decodificados (LRU, patrón ya existente) | Sesión | Decodificar en cada repintado (scroll). |
| **Disco propio** | El fichero generado (WebP reducido), indexado por la clave | Persistente y **offline** | Red + cuota + espera en usos posteriores. |
| **Assets semilla** | Platos frecuentes preparados de una vez y versionados | Persistente, sin red | Incluso la **primera** generación. |

Decisiones:

- **Clave = `dishKey` canónico** (§4.1), no el texto de la consulta ni el `recipe.id`. Así
  "arroz con pollo", "arroz con pollo fácil" y "receta de arroz con pollo" comparten imagen.
- **Persistencia en disco obligatoria**: sin ella la cuota crecería con el uso repetido. Una imagen se
  paga **una vez** en la vida de la instalación.
- **Evicción LRU con tope de disco** (p. ej. 50–100 MB) para acotar el crecimiento.
- **Set semilla** para los N platos más buscados del demo (mismo patrón que
  `scripts/fetch-recipe-photos.sh`: se preparan una vez y viajan en el APK). Es la mitigación más
  fuerte contra la latencia y el gasto en el caso "demo".

## 6. Latencia y experiencia de uso

La regla es **no hacer esperar nunca**:

1. Pintar la cadena local actual al instante (ya ocurre) y **sustituir** cuando llegue la generada.
2. **Solo lo visible**: generar por tarjeta visible, con cancelación al salir o hacer scroll. Nunca el
   lote de 6 en bloque.
3. **Priorizar** la tarjeta destacada / el hero del Detalle.
4. **Concurrencia limitada** (p. ej. 2 a la vez).
5. **Fallo silencioso**: si tarda o falla, se conserva la foto local. Nunca un error de UI por imagen.
6. **Política de timeouts propia** para imagen (más corta que la de chat), sin reintento agresivo.
7. **Transición suave** (fade/shimmer) para que el reemplazo no "salte".

Guardarraíles de presupuesto: **tope de generaciones por sesión** e **interruptor** para apagarlo por
completo (mismo patrón que `AiProviderPolicy.RESPONSE_FORMAT_ENABLED`).

## 7. Alternativas consideradas

1. **Ampliar la resolución de categorías (más assets).** No resuelve el problema de fondo: el plato
   concreto de una receta `ai` no tiene foto propia. Descartado (ya era el estado previo).
2. **Búsqueda de fotos por nombre (retrieval) en un banco externo.** Más fotorealista y barato por
   imagen, pero añade red de terceros, **licencias** y fiabilidad del match. Se deja como alternativa,
   no como camino principal.
3. **Generación local en el dispositivo (p. ej. Stable Diffusion).** Inviable en el alcance del MVP:
   tamaño, rendimiento y complejidad. Descartado.
4. **Autochequeo con visión** (pasar la imagen a un modelo multimodal y regenerar si no representa el
   plato). Baja la tasa de imágenes raras a costa de cuota; fuera de un primer alcance. Incógnita: si
   el alias `chat` es multimodal.
5. **Descargar en tiempo de ejecución y cachear sin persistir.** Rompería el ahorro de cuota; la
   persistencia en disco es la pieza clave. Descartado.

## 8. Riesgos y límites

| Riesgo | Impacto | Mitigación |
|---|---|---|
| La cuota semanal se dispara (~2 % por imagen) | Medio | Clave canónica + caché persistente + set semilla + tope por sesión + interruptor. |
| Latencia (≈4 s/imagen) percibida al abrir resultados | Bajo | Carga progresiva (local primero, generada después), concurrencia limitada, timeouts propios. |
| Imagen distinta para el mismo prompt (no determinismo) | Bajo | Caché por clave; el test JVM cubre **prompt** y **clave**, no el bitmap. |
| `dishKey` demasiado grueso → colisiones de imagen | Medio | Exigir especificidad en el contrato; clave exacta, sin fusión difusa. |
| El almacenamiento crece sin límite | Bajo | Evicción LRU con tope; reducir a WebP al guardar. |
| Cambio de formato del proveedor de imagen | Medio | Aislado en `data.ai.image` tras el puerto; fallback a la cadena local. |
| Romper el offline-first | Alto | La generación es un nivel **por delante** de la cadena de `recipe-photos`; sin red se degrada a lo actual. |
| No transparentar que la imagen es generada | Bajo | La app es personal y no se distribuye (`risks-and-open-questions.md`); valorar nota si se publicara. |

## 9. Impacto en el harness y la documentación

Cuando se decida construir esto (nada se toca aún):

1. **ADR nuevo** (p. ej. `docs/adr/0005-ai-dish-images.md`) que **ajuste parcialmente** el
   `0004-recipe-photos-as-assets.md` (hoy afirma "sin red en runtime" para las fotos).
2. **`docs/technical-discovery.md`**: describir el puerto, el adaptador, la caché y la nueva cadena de
   resolución.
3. **`docs/risks-and-open-questions.md`**: añadir el riesgo de cuota/latencia y el coste por imagen.
4. **`docs/design/`**: si hay UI nueva (indicador de "generando imagen", transición), cumplir la regla
   de **capturas de referencia obligatorias** antes de implementar.
5. **`feature_list.json`**: dar de alta las fases de §10 con `depends_on` y verificación.

## 10. Plan por fases (backlog propuesto, NO volcado en `feature_list.json`)

Cada fase es susceptible de convertirse en una feature del harness (`planner` → `implementer` →
`validator`). Orden recomendado por riesgo/valor.

| # | Fase | Objetivo | Criterio de aceptación (borrador) |
|---|---|---|---|
| 1 | `dish-image-domain` | Puerto + prompt puro + clave canónica + tests JVM. | Puro, determinista, sin red ni dependencias; casos negativos cubiertos. |
| 2 | `dish-image-generator` | Adaptador real `/images/generations` (b64 → WebP reducido). | Imagen real decodificada y reducida; fallo → degradación sin lanzar. |
| 3 | `dish-image-cache` | Caché en disco + memoria, evicción LRU, clave canónica. | Reuso entre consultas parecidas; sin red tras cachear; tope respetado. |
| 4 | `dish-image-progressive-ui` (`area: ui`) | Carga progresiva en `RecipeArtwork` + guardarraíles. | Local al instante, generada al llegar; capturas de referencia previas. |
| 5 | `dish-visual-descriptor` | Campo `dishKey`/`imagePrompt` en el contrato §4 (variante B). | Modelo emite el descriptor; validador intacto; degradación si falta. |
| 6 | `dish-style-reference` | `image-edit` con referencia de estilo (variante C). | Estilo homogéneo; fallback a texto→imagen. |
| 7 | `dish-image-seed` | Set semilla de platos frecuentes (patrón `fetch-recipe-photos.sh`). | Assets versionados; sin generación en runtime para esos platos. |

## 11. Preguntas abiertas

1. ¿Se adopta la variante **C** (`image-edit` con referencia) desde el principio o se empieza solo con
   **A** y se añade después?
2. ¿Se persiste la caché de imagen en **Room** (índice + fichero) o solo como **ficheros en `cacheDir`**?
3. Tamaño de guardado objetivo (¿960 de ancho para 16:10, o 512?) y tope de disco de la caché.
4. ¿Se acepta un **set semilla** con imágenes de platos concretos, o todo se genera en runtime?
5. ¿Se expone un **ajuste** al usuario para activar/desactivar la generación de imágenes?
6. ¿El alias `chat` es **multimodal** (habilita el autochequeo con visión, §7.4)?

## 12. Cómo retomar esta propuesta

Checklist de arranque para la próxima sesión que decida desarrollarla:

- [ ] Confirmar la decisión con la persona y redactar el **ADR 0005** (§9.1).
- [ ] Actualizar `technical-discovery.md` y `risks-and-open-questions.md` (§9.2–3).
- [ ] Convertir las fases de §10 en features de `feature_list.json` en orden, con `depends_on`.
- [ ] Empezar por la fase **pura** (`dish-image-domain`) en su propio worktree, sin red.
- [ ] Para la fase de UI, crear `docs/design/concepts/<feature-id>/` con sus capturas antes de
      implementar.
- [ ] Fijar la **política de cuota** (tope por sesión + interruptor) antes de la fase 2.

## 13. Referencias

- `docs/adr/0003-ai-provider.md` — proveedor de IA (DevExpert Inference, compatible con OpenAI).
- `docs/adr/0004-recipe-photos-as-assets.md` — cadena de fotos actual que esta propuesta extiende.
- `docs/specs/recipe-photos.md`, `docs/specs/recipe-artwork.md` — implementación vigente de la imagen.
- `docs/technical-discovery.md` — stack, pureza del dominio y proveedor de IA.
- `docs/risks-and-open-questions.md` — cuota de IA y riesgos abiertos.
- Documentación del proveedor: `https://portal.devexpert.io/docs/endpoint` y
  `https://portal.devexpert.io/docs/images` (`/images/generations` con `image`, `/images/edits` con
  `image-edit`).
