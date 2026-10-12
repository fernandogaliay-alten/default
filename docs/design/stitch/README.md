# Prompt de Stitch troceado (pantalla a pantalla)

El prompt largo (`docs/design/stitch-prompt.md`) se trunca en Stitch y no genera todo. Aquí está
**troceado por pantalla** para pegarlo en **la misma conversación** de Stitch, un mensaje por parte.

## Cómo usarlo

1. Abre una **sesión/conversación nueva** en Stitch.
2. Pega **primero** `00-contexto.md` (una sola vez). Espera a que responda "OK".
3. Pega **una parte a la vez, en orden** (`01`, `02`, …), **esperando** a que genere cada pantalla antes
   de enviar la siguiente. No las juntes en un solo mensaje.
4. Cada fichero contiene **solo** el texto a pegar: ábrelo, selecciona todo, copia y pega.
5. Revisa cada salida contra `DESIGN.md` (tokens y reglas); descarta o ajusta lo que contradiga.
6. Guarda cada export aceptado en su subcarpeta:
   `docs/design/concepts/<feature-id>/<pantalla>/` con los tres ficheros del export (`.png`, `.html`,
   `design.md`, tal cual los da Stitch). El mapa está más abajo.
7. Registra el estado en `DESIGN.md` ("Generated Concept Images").

## Orden y mapa de carpetas

| Parte | Pantalla | Carpeta destino |
|---|---|---|
| `00-contexto.md` | contexto fijo (producto, visual, navegación) | — (solo contexto) |
| `01-design-system.md` | hoja "Design System" | `design-tokens-and-theme/design-system/` |
| `02-onboarding-bienvenida.md` | onboarding paso 1 | `onboarding-shell-and-identity/bienvenida/` |
| `03-onboarding-dieta.md` | onboarding paso 2 | `onboarding-diet-style-cuisines/dieta/` |
| `04-onboarding-estilo.md` | onboarding paso 3 | `onboarding-diet-style-cuisines/estilo/` |
| `05-onboarding-cocinas.md` | onboarding paso 4 | `onboarding-diet-style-cuisines/cocinas/` |
| `06-onboarding-ingredientes.md` | onboarding paso 5 | `onboarding-ingredients-safety-kitchen/ingredientes/` |
| `07-onboarding-alergias.md` | onboarding paso 6 | `onboarding-ingredients-safety-kitchen/alergias/` |
| `08-onboarding-tu-cocina.md` | onboarding paso 7 | `onboarding-ingredients-safety-kitchen/tu-cocina/` |
| `09-home.md` | Home (incluye fila de filtros "Tus recetas") | `home-offline-blocks/home/` y `local-result-filters/filtros-tus-recetas/` |
| `10-home-sin-conexion.md` | Home sin conexión | `home-offline-blocks/home-sin-conexion/` |
| `11-home-busqueda-ia.md` | Búsqueda IA desde Home | `ai-search/` |
| `12-generador-ingredientes.md` | Generador paso 1 | `generator-ingredients-step/generador-ingredientes/` |
| `13-generador-ajustes.md` | Generador paso 2 | `generator-settings-step/generador-ajustes/` |
| `14-generador-cargando.md` | Generando | `generation-flow-with-validation/generador-cargando/` |
| `15-resultados.md` | Resultados | futuro `results-screen/` (y `generation-flow-with-validation/resultados-preview/`) |
| `16-detalle-receta.md` | Detalle de receta | futuro `recipe-detail-screen/` |
| `17-favoritos.md` | Favoritos | futuro `favorites/` |
| `18-perfil.md` | Perfil | `profile-screen/perfil/` |
| `19-perfil-editar.md` | Perfil · edición de identidad | `profile-screen/perfil-editar/` |
| `20-perfil-sugerencias.md` | Perfil · sección Sugerencias | `manual-suggestions-refresh/perfil-sugerencias/` |
| `21-perfil-resumen.md` | Perfil · sección Resumen | `activity-summary/perfil-resumen/` |

Reglas: dirección visual, no pixel-perfect; manda `DESIGN.md` ante contradicción. Los iconos/imágenes de
Stitch son placeholders (en la app se resuelven con Material Symbols y degradados propios).

## Paquetes listos por feature (`features/`)

Para el **backfill** de capturas (features de UI aceptadas antes de la política de capturas) hay un
prompt **autocontenido por feature** en `features/<feature-id>.md`: incluye el contexto
(`00-contexto.md`) y las partes de sus pantallas, listo para pegar en una conversación de Stitch por
feature y guardar cada export en `docs/design/concepts/<feature-id>/<pantalla>/`. No sustituyen a las
partes de arriba (`00`–`21`), que son la fuente.

| Feature | Pantallas | Parte(s) |
|---|---|---|
| `design-tokens-and-theme` | `design-system` | `01` |
| `onboarding-shell-and-identity` | `bienvenida` | `02` |
| `onboarding-diet-style-cuisines` | `dieta`, `estilo`, `cocinas` | `03`, `04`, `05` |
| `onboarding-ingredients-safety-kitchen` | `ingredientes`, `alergias`, `tu-cocina` | `06`, `07`, `08` |
| `home-offline-blocks` | `home`, `home-sin-conexion` | `09`, `10` |
| `local-result-filters` | `filtros-tus-recetas` | `09` (solo el bloque "Tus recetas") |
| `generator-ingredients-step` | `generador-ingredientes` | `12` |
| `generator-settings-step` | `generador-ajustes` | `13` |
| `generation-flow-with-validation` | `generador-cargando`, `resultados-preview` | `14`, `15` |
| `profile-screen` | `perfil`, `perfil-editar` | `18`, `19` |
| `manual-suggestions-refresh` | `perfil-sugerencias` | `20` |
