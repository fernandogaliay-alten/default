# Build Brief

## Problem

Decidir qué cocinar con lo que ya hay en casa cuesta tiempo y termina en decisiones repetitivas o en
desperdicio de ingredientes. El usuario sabe qué tiene, pero no qué puede hacer con ello teniendo en
cuenta su tiempo, su nivel de cocina, sus alergias y sus gustos.

## Current Workaround / Existing System

Búsqueda manual en webs de recetas, redes sociales o vídeos: el usuario filtra a mano, descarta
opciones por alérgenos, improvisa cantidades y rara vez guarda nada reutilizable. No existe un flujo
que parta de "esto es lo que tengo" y aplique de forma fiable las restricciones del usuario.

## Target Users

- **Usuario local único** (el dueño del dispositivo). No hay cuentas, ni roles, ni compartición.
- **Contexto de curso**: el proyecto es a la vez producto y material didáctico de un flujo SDD con
  agentes (spec → implementación → validación).

## Goals

1. Convertir una lista de ingredientes en recetas útiles mediante IA, con pocas preguntas.
2. Que las **restricciones duras** (alérgenos, patrón de dieta) se respeten de forma verificable,
   no solo "pedidas" al modelo.
3. Ofrecer sugerencias relevantes en la pantalla principal a partir del Perfil de preferencias,
   sin depender de la red en cada apertura.
4. Permitir guardar recetas como favoritas, disponibles sin conexión.
5. Recorrer el ciclo SDD completo con el harness: documentos → specs → features → validación.

## Non-Goals

Explícitamente **fuera del MVP**. Son evolutivos (backlog post-MVP), no se descartan:

- Lista de la compra.
- ~~Modo cocina con temporizadores reales.~~ **Ampliación del MVP (issue #76, 2026-10-12): entra en el
  MVP** como feature `cooking-mode` (pasos a pantalla completa con temporizador por paso y aviso en
  pantalla). El Non-Goal "Notificaciones y recordatorios" sigue vigente: el aviso al terminar es
  **visual**, sin notificación de sistema, sonido ni vibración.
- Compartir / exportar recetas.
- Multi-idioma.
- Notificaciones y recordatorios.
- Editor de recetas propias (crear o editar recetas a mano).
- **Ampliación del MVP revocada (issue #83, 2026-10-13; revertida por ADR 0006).** La ampliación de #83
  metía las fotos de plato en el MVP: la feature `recipe-photos` añadió **31 fotos** (12 recetas semilla
  + 18 categorías + 1 por defecto) **empaquetadas como assets offline** en
  `app/src/main/assets/img/recipes/`, con la cadena de respaldo semilla → categoría → por defecto y el
  placeholder dibujado como **último recurso**. Esa ampliación **se revierte**: las recetas `ai-*` caían
  en la **foto de su categoría**, que **no corresponde al plato** (el síntoma de `dish-image-prompt-v2`).
  Con `docs/adr/0006-typographic-recipe-cards.md` la **imagen base de una receta es una ficha
  tipográfica** (categoría, título y los tres ingredientes principales con cantidad, más el filete de
  cocina) y la **cadena local de fotos se retiró** con la feature **`retire-recipe-photos`**
  (2026-10-14): los **30 assets** de `seed/` y `category/` (**5.954.912 B ≈ 5,68 MiB**) salieron del APK
  y se **conserva** solo la foto neutra `img/recipes/default.jpg` (**137.359 B**) como ancla
  de estilo, que no se muestra. La foto generada por IA se conserva **como capa de fondo** del hueco,
  **opt-in** con el interruptor "Generar imágenes del plato" y tope por sesión. Ver
  `docs/adr/0006-typographic-recipe-cards.md`, `docs/adr/0004-recipe-photos-as-assets.md` (ajustado) y
  `docs/adr/0005-ai-dish-images.md` (ajustado).

Fuera del MVP de forma permanente:

- Backend, login, sincronización en la nube o multi-dispositivo.
- Búsqueda en un catálogo remoto de recetas. La búsqueda de la app es **generación con IA**, no
  consulta de catálogo.

## MVP Slice

Un **único corte vertical**, de extremo a extremo:

1. **Onboarding obligatorio** (no saltable, 7 pasos) que construye el Perfil de usuario: identidad
   (nombre y color de avatar) y Perfil de preferencias sobre la Taxonomía.
2. **Home** con cinco bloques: cabecera con la identidad, **Búsqueda con IA**, tarjeta-héroe al
   Generador, Lote de sugerencias (semilla + IA cacheada) y bloque "Tus recetas" con Filtros de
   resultados locales. Solo la búsqueda necesita red; el resto de Home funciona offline.
3. **Generador**: entrada de ingredientes → ajustes (tiempo, tipo de comida, comensales
   precargados desde los comensales habituales, dificultad, restricciones, notas) → llamada a IA.
4. **Validación** de la respuesta contra Taxonomía y Restricciones duras.
5. **Lista de resultados** con nombre, tiempo, dificultad y coincidencia.
6. **Detalle** con ingredientes y cantidades, raciones escalables, pasos con duración y nutrición
   básica.
7. **Favoritos** persistidos en local y consultables sin conexión.
8. **Perfil**: consulta y edición de la identidad y de todas las preferencias, tarjeta de
   alérgenos, Resumen de actividad (recetas generadas y favoritas) y acción **"Actualizar
   sugerencias"** que expone la política de refresco al usuario.

Lo que se puede dejar en versión mínima sin invalidar el aprendizaje: la semilla local puede ser
pequeña (12–20 recetas), la nutrición puede ser estimada, y el proveedor real de IA puede
sustituirse por la implementación falsa durante el desarrollo.

## Validation Plan

- **Tests unitarios de dominio** (sin red, sin Android): integridad de la Taxonomía, construcción
  del prompt, parsing y validación de la respuesta de IA, descarte por restricciones duras, política
  de caché y firma de preferencias, escalado de raciones. Se ejecutan con `FakeRecipeGenerator`.
- **Prueba manual end-to-end** en emulador: onboarding → búsqueda con IA desde Home → añadir
  ingredientes → generar → abrir detalle → guardar favorito → editar preferencias en Perfil → ver
  favorito sin conexión.
- **Caso negativo obligatorio**: usuario con restricción dura marcada recibe una receta que la
  incumple → la app no la muestra. Se cubre tanto en el generador como en la Búsqueda con IA (por
  ejemplo, un vegano que busca "carne").

## Success Criteria

- El flujo completo funciona en un dispositivo/emulador desde el primer arranque.
- Con la red desconectada, el Lote de sugerencias, "Tus recetas" y Favoritos siguen siendo
  utilizables; solo la Búsqueda con IA queda deshabilitada con aviso.
- Ninguna receta mostrada incumple una Restricción dura marcada (verificado por test).
- La app arranca y es usable sin proveedor de IA (modo semilla) si la clave falta o falla.
- El Perfil permite consultar y editar la identidad y todas las preferencias capturadas en el
  onboarding reutilizando **un único editor** (sin pantallas duplicadas).
- `init.sh` ejecuta los tests de dominio en verde de forma reproducible.

## Notes

- Decisiones de discovery que condicionan el MVP:
  - Sin backend: la clave de IA irá en `BuildConfig` (solo demo, nunca distribuir así).
  - Política de sugerencias **cache-first**: la IA se consulta por firma de preferencias cambiada,
    caché caducada o petición explícita del usuario.
  - El proveedor de IA **aún no está decidido** → el dominio depende de una interfaz, no de un SDK.
- Artefactos de discovery relacionados: `docs/domain/taxonomy.md`, `docs/domain-model.md`,
  `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`, `docs/adr/`, `DESIGN.md`.
