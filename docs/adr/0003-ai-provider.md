# ADR 0003: Proveedor de IA — chat por OpenCode Go; imagen por DevExpert Inference

## Estado

Aceptado. **Enmendado (2026-10-14, issue #141, correctiva `chat-provider-opencode-go`)**: la decisión
se **parte en dos** — el **chat** pasa a **OpenCode Go** (`deepseek-v4.1-flash`) y la **generación de
imágenes de plato sigue en DevExpert Inference**, con base y clave **propias**
(`DishImagePolicy.BASE_URL`; ver `docs/adr/0005-ai-dish-images.md`). Motivo: la **cuota semanal de
DevExpert se agotó**. Se mantienen el HTTP directo sin SDK, `response_format`/fallback, la ausencia de
`json_schema`, la clave por `BuildConfig` y el modo semilla.

## Contexto

La app tiene **una única integración externa**: la API de IA que genera las recetas. El dominio no
depende de un SDK concreto, sino del puerto `RecipeGenerator` (tres operaciones: sugerir por
preferencias, buscar por texto y generar desde ingredientes); la implementación real es lo último que
se conecta (`real-ai-provider`).

El proyecto es un curso de AI Expert, y los alumnos tienen acceso a **DevExpert Inference**, una API
**compatible con OpenAI** incluida con la formación. La decisión abierta era qué proveedor y qué
modelo usar para `real-ai-provider` (antes pregunta 1 de `docs/risks-and-open-questions.md`).

## Decisión

- **Chat: OpenCode Go** (correctiva `chat-provider-opencode-go`, 2026-10-14). API compatible con
  OpenAI.
  - Base URL: `https://opencode.ai/zen/go/v1`.
  - Endpoint: `POST /chat/completions`.
  - Autenticación: `Authorization: Bearer $OPENCODE_API_KEY`.
  - Modelo: **`deepseek-v4.1-flash`** (`AiProviderPolicy.MODEL_ID`). Go **no** expone alias: el id es
    literal y queda fijado en un único sitio; si el proveedor lo renombra, el chat degrada a modo
    semilla hasta cambiar esa constante.
  - Cabeceras **propias por petición**: `x-opencode-session` —**obligatoria**: sin ella el proveedor
    responde `400 MissingSessionID` (verificado a mano el 2026-10-14)— con una constante estable y **no
    sensible** (`"sazon-android"`, nunca la clave) y un `User-Agent` propio
    (`Sazon-Android/1.0`). Van **por petición**, no como defaults del cliente, porque el `aiClient` es
    **compartido** con la cadena de imagen (unos defaults filtrarían la cabecera de sesión a
    DevExpert).
  - El endpoint **Console** (`https://opencode.ai/inference/...`) queda **descartado**: responde
    `402 Insufficient account funds` (verificado a mano el 2026-10-14).
- **Imagen de plato: DevExpert Inference** (sin cambios de proveedor; ver
  `docs/adr/0005-ai-dish-images.md`).
  - Base URL: `https://inference.devexpert.io/v1`, **propia** de la política de imagen
    (`DishImagePolicy.BASE_URL`), que **no** se deriva de la base del chat.
  - Endpoints: `POST /images/generations` y `POST /images/edits`; autenticación
    `Authorization: Bearer $DEVEXPERT_API_KEY`; alias `image`/`image-edit`.
- La app habla con HTTP directo (Ktor) + `kotlinx.serialization`; **no** se adopta ningún SDK
  de proveedor.
- Las **claves** (dos) se inyectan en tiempo de build desde `local.properties` a `BuildConfig`
  (`OPENCODE_API_KEY` para el chat, `DEVEXPERT_API_KEY` para la imagen) y **no se versionan** (ver
  ADR 0002).
- **No se depende de salida estructurada forzada** (`json_schema`/structured outputs): la
  compatibilidad con OpenAI no la garantiza. El contrato del prompt pide JSON y el `RecipeValidator`
  parsea de forma **tolerante** (elemento a elemento, sin lanzar), por lo que la app funciona con
  respuestas imperfectas.
- **Nota (`ai-response-format`, issue #74)**: la app **sí** envía `response_format: {"type":"json_object"}`
  (verificado HTTP 200 con el proveedor del momento, el alias `chat` de DevExpert; hoy se envía igual
  contra OpenCode Go con `deepseek-v4.1-flash`), que solo garantiza **forma** JSON parseable (el dominio
  lo sigue decidiendo el `RecipeValidator`, puerta dura). Es degradable: ante `400`/`422` reintenta
  **una vez** sin la clave, y hay un interruptor (`AiProviderPolicy.RESPONSE_FORMAT_ENABLED`). El
  rechazo a `json_schema`/structured outputs forzados y a pinnear un modelo avanzado se **mantiene**; la
  decisión de fondo no cambia.
- **Nota (`ai-verbose-diagnostics`)**: el *modo de diagnóstico verboso* (interruptor
  `verboseDiagnostics` de `DevExpertRecipeGenerator`, activo solo en debug vía `BuildConfig.DEBUG`) es
  una **ayuda de desarrollo temporal**, apagada en release: añade al evento y al log local el detalle
  de cada descarte (con título) y el `content` crudo del modelo, troceado. No cambia la decisión de
  proveedor, ni el contrato (§4), ni la puerta dura del `RecipeValidator`; aun en debug nunca aparecen
  la clave ni la cabecera `Authorization`.
- Si falta la clave o el proveedor falla o se agota la cuota, la app **degrada a modo semilla**.

## Alternativas consideradas

1. **Otro proveedor compatible con OpenAI** (p. ej. OpenAI, DeepSeek, MiMo directo). Obligaría a
   gestionar otra cuenta, otra clave y otro coste, y a fijar un modelo concreto que puede cambiar. Se
   descartó para el chat original por simplicidad y porque DevExpert Inference ya está disponible para
   el curso. **Materializada después** (correctiva `chat-provider-opencode-go`, 2026-10-14): al agotarse
   la cuota semanal de DevExpert, el chat pasó a **OpenCode Go** (ya disponible, misma forma de
   wire), manteniendo la imagen en DevExpert.
2. **SDK específico del proveedor.** Acopla el dominio a un vendor y complica el testing sin red. El
   puerto `RecipeGenerator` + HTTP directo aísla el proveedor en una sola capa.
3. **Exigir structured outputs.** Sería lo ideal para robustez, pero no está garantizado; añadir una
   dependencia dura a una capacidad no confirmada es un riesgo mayor que parsear de forma tolerante.

## Consecuencias

- El **coste y la cuota** son **semanales** por clave (al 100 % se rechazan las peticiones hasta el
  reset); la política cache-first y el modo semilla actúan como protección. Desde la correctiva
  `chat-provider-opencode-go` hay **dos** cuotas separadas: la **imagen** sigue en la cuota semanal de
  DevExpert y el **chat** consume la de OpenCode Go, que es **compartida con el harness de agentes** de
  este repo (límites por 5 h/semana/mes) y cuyo proveedor **monitoriza el abuso**.
- Cambiar de proveedor es un cambio en **una sola capa** (la implementación de `RecipeGenerator`),
  porque el contrato no filtra detalles del vendor. **La predicción se cumplió** en la correctiva
  `chat-provider-opencode-go`: el dominio (`Recipe`, `RecipeValidator`, `PromptBuilder`, la Taxonomía) y
  el wire OpenAI (`OpenAiChat`) quedaron **intactos**; solo cambiaron base + modelo + cabeceras + clave
  del chat y el desacople de la base de imagen.
- Los alias `chat`/`chat-pro` podían usar un **modelo de respaldo** si el principal falla. En OpenCode Go
  **no hay alias**: el id `deepseek-v4.1-flash` es literal, así que un renombrado del proveedor degrada
  el chat a modo semilla hasta cambiar la constante (sin fallo de la app). La **imagen** conserva sus
  alias (`image`/`image-edit`) en DevExpert.
- La recomendación de la doc de evitar DeepSeek en Android Studio (Agent Mode) **no aplica**: la app
  usa HTTP directo, no el proveedor remoto de Android Studio.
- `matchScore` lo calcula la app (derivado, no persistido), así que no se pide al modelo.
