# ADR 0003: Proveedor de IA — DevExpert Inference

## Estado

Aceptado.

## Contexto

La app tiene **una única integración externa**: la API de IA que genera las recetas. El dominio no
depende de un SDK concreto, sino del puerto `RecipeGenerator` (tres operaciones: sugerir por
preferencias, buscar por texto y generar desde ingredientes); la implementación real es lo último que
se conecta (`real-ai-provider`).

El proyecto es un curso de AI Expert, y los alumnos tienen acceso a **DevExpert Inference**, una API
**compatible con OpenAI** incluida con la formación. La decisión abierta era qué proveedor y qué
modelo usar para `real-ai-provider` (antes pregunta 1 de `docs/risks-and-open-questions.md`).

## Decisión

- Proveedor: **DevExpert Inference**, API compatible con OpenAI.
  - Base URL: `https://inference.devexpert.io/v1`.
  - Endpoint: `POST /chat/completions`.
  - Autenticación: `Authorization: Bearer $DEVEXPERT_API_KEY`.
  - Alias de modelo: **`chat`** (uso diario) y **`chat-pro`** (tareas complejas). El modelo real que
    hay detrás del alias puede cambiar sin que cambie el nombre que usa la app.
- La app habla con HTTP directo (Retrofit/Ktor) + `kotlinx.serialization`; **no** se adopta ningún SDK
  de proveedor.
- La **clave** se inyecta en tiempo de build desde `local.properties` a `BuildConfig` y **no se
  versiona** (ver ADR 0002).
- **No se depende de salida estructurada forzada** (`json_schema`/structured outputs): la
  compatibilidad con OpenAI no la garantiza. El contrato del prompt pide JSON y el `RecipeValidator`
  parsea de forma **tolerante** (elemento a elemento, sin lanzar), por lo que la app funciona con
  respuestas imperfectas.
- **Nota (`ai-response-format`, issue #74)**: la app **sí** envía `response_format: {"type":"json_object"}`
  en el alias `chat` (verificado HTTP 200), que solo garantiza **forma** JSON parseable (el dominio lo
  sigue decidiendo el `RecipeValidator`, puerta dura). Es degradable: ante `400`/`422` reintenta **una
  vez** sin la clave, y hay un interruptor (`AiProviderPolicy.RESPONSE_FORMAT_ENABLED`). El rechazo a
  `json_schema`/structured outputs forzados y a pinnear `chat-pro` se **mantiene**; la decisión de fondo
  no cambia.
- Si falta la clave o el proveedor falla o se agota la cuota, la app **degrada a modo semilla**.

## Alternativas consideradas

1. **Otro proveedor compatible con OpenAI** (p. ej. OpenAI, DeepSeek, MiMo directo). Obligaría a
   gestionar otra cuenta, otra clave y otro coste, y a fijar un modelo concreto que puede cambiar. Se
   descarta por simplicidad y porque DevExpert Inference ya está disponible para el curso.
2. **SDK específico del proveedor.** Acopla el dominio a un vendor y complica el testing sin red. El
   puerto `RecipeGenerator` + HTTP directo aísla el proveedor en una sola capa.
3. **Exigir structured outputs.** Sería lo ideal para robustez, pero no está garantizado; añadir una
   dependencia dura a una capacidad no confirmada es un riesgo mayor que parsear de forma tolerante.

## Consecuencias

- El **coste y la cuota** son **semanales** por clave (al 100 % se rechazan las peticiones hasta el
  reset); la política cache-first y el modo semilla actúan como protección.
- Cambiar de proveedor es un cambio en **una sola capa** (la implementación de `RecipeGenerator`),
  porque el contrato no filtra detalles del vendor.
- Los alias `chat`/`chat-pro` pueden usar un **modelo de respaldo** si el principal falla: mejora la
  disponibilidad, a costa de que el comportamiento pueda variar ligeramente entre llamadas.
- La recomendación de la doc de evitar DeepSeek en Android Studio (Agent Mode) **no aplica**: la app
  usa HTTP directo, no el proveedor remoto de Android Studio.
- `matchScore` lo calcula la app (derivado, no persistido), así que no se pide al modelo.
