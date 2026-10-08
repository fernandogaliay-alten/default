# Risks and Open Questions

## Blocking Next Phase

Ninguno. El harness (`AGENTS.md`, `init.sh`, `PROGRESS.md`, `feature_list.json`) y las features de
dominio, persistencia y UI pueden derivarse ya. La única incógnita relevante afecta a **una** feature
concreta (la integración real de IA), no al arranque del proyecto.

## Implementation-Time Questions

Los números son **posicionales**. Al resolver una pregunta y retirarla de la tabla, el resto se
renumera y hay que actualizar las citas que existan en otros documentos del repositorio.

| # | Pregunta | Impacto | Cuándo decidir |
|---|---|---|---|
| 1 | ¿Errores y reintentos de la IA (timeout, cuota, JSON inválido)? | UX y coste | Antes de la integración |

Nota de renumeración: se han resuelto el proveedor de IA (antes 1), el nº de recetas por generación
(antes 2), la caducidad del lote (antes 3), `matchScore` (antes 4) y el formato de las recetas semilla
(antes 7); las antiguas 5 y 6 pasaron a ser 1 y 2. La que era la pregunta 2 de la tabla (licencias de
assets) también se ha resuelto, así que la tabla queda solo con la 1. Las specs históricas conservan
las citas con la numeración de su momento; la tabla se mantiene posicional.

### Resueltas

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
- **Caducidad del Lote de sugerencias** (antes pregunta 3): **7 días**, en un único sitio configurable.
- **Cálculo de `matchScore`** (antes pregunta 4): lo calcula **la app** (derivado, **no persistido**);
  la app lo recalcula al mostrar resultados. La fórmula se fija en `results-screen`.
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
  tolerante del `RecipeValidator` (no depende de structured outputs). Ver `docs/adr/0003-ai-provider.md`.
- **Licencias de iconos y fotos de receta** (era la pregunta 2 de la tabla): resuelta como **no
  bloqueante**. La app es personal y no se publica, así que la elección de assets es libre y se
  prioriza la comodidad: iconos vía Material Symbols (incluido en Material 3) y fotos con placeholders
  de degradado o imágenes propias. Ver `docs/design/concepts/README.md`.

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
| La Búsqueda con IA es el único punto de Home que exige red y rompe la sensación offline del resto | Media | Medio | Degradar solo ese bloque con aviso claro; el resto de Home intacto |
| La Búsqueda con IA se percibe como "rota" cuando el Perfil bloquea lo que el usuario escribe | Media | Medio | Estado vacío que explica el motivo y enlaza a editar el Perfil |
| El Perfil duplica pantallas del onboarding y divergen | Media | Alto | Regla "un editor, dos entradas": reutilizar los mismos componentes |
| La sincronización de la Taxonomía escribe en el árbol de fuentes y se engancha por nombre de tarea (`merge*Assets`) | Baja | Bajo | `syncTaxonomy` corre en cada build y el asset está ignorado en git; es deuda técnica anotada por el validador, no un fallo |

## Research Tasks

1. Comparar la salida estructurada de los proveedores candidatos (esquema forzado, longitud máxima,
   tratamiento de `enum`).
2. Medir latencia y coste reales de generar 4–6 recetas con el contrato de la Taxonomía.
3. Elegir set de iconos de ingredientes (**no bloqueante**: app personal y no publicada; se prioriza
   comodidad, p. ej. Material Symbols o Noto Emoji).
4. Elegir origen de las fotos de receta (**no bloqueante**: app personal; fotos propias o placeholders
   de degradado).
5. Validar el contrato del prompt contra un modelo real antes de fijarlo como definitivo.
6. Regenerar las pantallas de Stitch a partir de `DESIGN.md` (las actuales quedan obsoletas).

## ADRs

- `docs/adr/0001-taxonomy-in-code.md` — la Taxonomía vive en código y es la única fuente de verdad.
- `docs/adr/0002-local-first-no-backend.md` — todo el estado en el dispositivo; la clave de IA viaja
  en el APK.
- `docs/adr/0003-ai-provider.md` — proveedor de IA: DevExpert Inference (API compatible con OpenAI).

`RecipeGenerator` como interfaz con implementación falsa **no se elevó a ADR**: introducir esa
abstracción más tarde sería un refactor de una sola capa y el patrón es estándar. Queda documentado
en `docs/technical-discovery.md`.
