# ADR 0001: La Taxonomía vive en código, no en base de datos

## Estado

Aceptado.

## Contexto

Onboarding, filtros locales, constructor de prompt y Validador de recetas tienen que coincidir sobre
el **mismo** catálogo de opciones (dietas, restricciones, preferencias, cocinas, ingredientes,
categorías de receta, colores de avatar). Si dos de esos consumidores divergen, el producto falla de
formas difíciles de diagnosticar: una opción que se puede elegir pero no se envía al modelo, o un
`id` que el validador no reconoce.

Es un proyecto de curso, sin backend, con Usuario local único y sin catálogo remoto.

## Decisión

- La Taxonomía es un **artefacto versionado que viaja dentro de la app**
  (`docs/domain/taxonomy.json` → objetos tipados al arrancar).
- Es la **única fuente de verdad**: la UI, el constructor de prompt y el validador leen de ella.
- Los `id` son estables, en inglés y **nunca se renombran**. Se añaden ids nuevos; los obsoletos se
  deprecan con alias.
- El **estado del usuario** (perfil, preferencias, favoritos, caché de recetas) sí vive en Room. La
  Taxonomía **no** se persiste ni se edita en tiempo de ejecución.
- Los assets visuales (iconos de ingredientes, imágenes por categoría) se resuelven por convención a
  partir de los `id`.

## Alternativas consideradas

1. **Tabla en Room, sembrada en el primer arranque.** Permite editar opciones en el dispositivo, pero
   añade migraciones y crea una segunda fuente de verdad, porque las etiquetas y los iconos siguen
   necesitando el código. El coste no compra nada en el MVP.
2. **Configuración remota o CMS.** Permite cambiar el catálogo sin publicar una versión. Exige
   backend y red, y contradice la decisión de arquitectura local-first (ADR 0002).
3. **Objetos Kotlin escritos a mano, sin JSON.** Evita el parseo, pero duplica la forma de los datos
   en código, la hace opaca para el tooling y complica el test de integridad.

## Consecuencias

- Añadir una opción = añadir un item + su asset + publicar una versión. Es el precio de la
  simplicidad.
- **Renombrar o borrar un `id` rompe preferencias guardadas y cachés.** Es la regla más delicada del
  proyecto y se refuerza con un test de integridad (alérgenos ⊆ restricciones, tags ⊆ dietas, ids
  únicos, todo icono con asset o fallback).
- La Taxonomía es **testeable sin Android y sin red**, lo que la convierte en la base de la
  verificación del harness.
- Migrar a un catálogo remoto en el futuro exigiría añadir una capa de sincronización, pero los `id`
  sobrevivirían: la deuda es acotada.
