# Context

Vocabulario compartido del proyecto. **Sin** planes, tareas ni decisiones de implementación.
Los identificadores de código y las claves van en inglés; el texto, en español.

Este documento existe para evitar *vocabulary drift*: que dos agentes (o dos sesiones) llamen
"dieta", "alergia" o "receta guardada" a cosas distintas.

## Glossary

### Taxonomía
Catálogo único de opciones del producto: patrones de dieta, restricciones duras, preferencias
blandas, tipos de comida, cocinas, franjas de tiempo, dificultades, ingredientes, categorías de
receta y colores de avatar. Vive en código, no en base de datos. Detalle en
`docs/domain/taxonomy.md`.

### Receta (Recipe)
Unidad de contenido que la app muestra: título, descripción, tiempo total, dificultad, comensales,
ingredientes con cantidades, pasos y notas. Toda receta tiene un `source`.

### source
Origen de una receta: `seed` (autorada y empaquetada con la app) o `ai` (generada por la IA y
persistida en el dispositivo). La UI las trata igual.

### Receta semilla (seed recipe)
Receta autorada a mano y **empaquetada con la app**: es contenido, no código generado. Siempre
disponible y sin conexión. No confundir con la Taxonomía, que es el catálogo de opciones.

### Ingrediente
Producto que el usuario aporta o que aparece dentro de una receta. Puede ser un ingrediente de la
lista de la Taxonomía (tiene `id`) o un ingrediente libre escrito por el usuario (solo tiene nombre).

### Ingrediente de despensa (pantry)
Ingrediente que se asume disponible sin que el usuario tenga que declararlo (sal, aceite, pimienta,
agua). No se exige en el paso de entrada de ingredientes.

### Perfil de usuario (UserProfile)
Identidad del Usuario local: nombre para mostrar y color del Avatar de iniciales. El nombre es
**obligatorio** en el onboarding. **No** es una preferencia y **no** entra en la Firma de
preferencias. Se captura en el onboarding y se edita en la pantalla Perfil.

### Perfil de preferencias (PreferenceProfile)
Conjunto persistido de elecciones culinarias del usuario: patrón de dieta, restricciones duras,
preferencias blandas, cocinas favoritas, ingredientes preferidos, Ingredientes que evito, nivel de
cocina, tiempo disponible y Comensales habituales. Se captura en el onboarding y se edita en la
pantalla Perfil. Es la parte del Perfil de usuario que sí influye en lo que la app propone.

### Patrón de dieta (diet pattern)
Elección **única** que describe la base alimentaria del usuario (omnívoro, flexitariano,
pescetariano, vegetariano, vegano). Excluye ingredientes por etiquetas.

### Restricción dura (hard restriction)
Alérgeno o intolerancia marcada por el usuario. Es un asunto **de seguridad**, no de gusto: si una
receta la incumple, la receta se descarta. Se pide a la IA **y** se verifica después.

### Preferencia blanda (soft preference)
Preferencia de estilo o salud (Real Fooding, bajo en calorías, keto, económico, aprovechamiento...).
Influye en el ranking y en el prompt, pero **nunca** descarta una receta.

### Avatar de iniciales
Representación visual del Usuario local: las iniciales del nombre sobre uno de los colores de la
Taxonomía. No se suben fotos en el MVP.

### Ingredientes que evito (dislikedIngredientIds)
Ingredientes que el usuario no quiere encontrar en las recetas. Es una **preferencia blanda**:
influye en el prompt y en el ranking, pero **nunca** descarta una receta. No confundir con
Restricción dura.

### Comensales habituales (defaultServings)
Número de raciones que el usuario cocina normalmente. Precarga el stepper del generador y **sí**
entra en la Firma de preferencias.

### Resumen de actividad
Datos que la pantalla Perfil muestra al usuario: recetas generadas y recetas favoritas. Las recetas
generadas son un **contador persistido** (`recipesGeneratedCount`), porque un conteo derivado
mentiría al refrescarse el Lote de sugerencias; las favoritas se calculan por consulta.

### Pantalla Perfil
Pantalla donde el usuario consulta y modifica su Perfil de usuario y su Perfil de preferencias,
consulta su Resumen de actividad y fuerza la actualización del Lote de sugerencias.

### Onboarding
Flujo de primera ejecución, obligatorio y no saltable, que construye el Perfil de usuario: identidad
(nombre y color de avatar) y Perfil de preferencias.

### Firma de preferencias (preferenceSignature)
Valor derivado (hash) del Perfil de preferencias. Si cambia, las sugerencias generadas por IA se
consideran obsoletas y se refrescan. **Excluye** el nombre y el color de avatar: cambiar de
identidad no invalida recetas.

### Lote de sugerencias
Conjunto de recetas que alimenta la pantalla principal, formado por recetas `seed` y, opcionalmente,
recetas `ai` generadas a partir de las preferencias. Se guarda con la firma de preferencias y una
fecha.

### Sugerencia
Receta que la app propone sin que el usuario haya declarado ingredientes. Se obtiene del Lote de
sugerencias, no de una consulta a la IA en cada apertura.

### Búsqueda con IA (AI search)
Petición en texto libre que el usuario escribe en la Home ("algo con pollo y arroz") y que la IA
resuelve devolviendo recetas. **No** es una búsqueda en catálogo: no hay catálogo remoto. Sus
resultados pasan por el Validador de recetas igual que los del generador, y las Restricciones duras
siguen aplicándose aunque el texto escrito las contradiga.

### Bloque "Tus recetas"
Sección de la Home que agrupa las recetas que **ya están en el dispositivo**: favoritas ("Guardadas")
y últimas abiertas ("Abiertas hace poco"). Junto con la pantalla Favoritos, es donde aplican los
Filtros de resultados.

### Filtros de resultados
Filtrado **local y sin red** de las recetas que ya están en el dispositivo: por categoría
(`categoryId`), por texto en la pantalla Favoritos, y por filtros derivados ("Rápido" =
`totalMinutes ≤ 30`; "Vegetariano" = sin ingredientes con tags `meat`, `poultry`, `fish` o
`seafood`). Aplica al bloque "Tus recetas" de la Home y a la pantalla Favoritos.

### Generador de recetas (RecipeGenerator)
Puerto del dominio que abstrae la IA. Tiene tres operaciones: sugerir por preferencias, buscar por
texto libre y generar a partir de ingredientes. La implementación real está por decidir; en tests se
usa una implementación falsa.

### Validador de recetas
Componente que comprueba la respuesta de la IA contra la Taxonomía y las Restricciones duras antes
de mostrarla. Descarta recetas que incumplen seguridad.

### Favorito
Receta que el usuario marca para conservar. Se guarda en local y es independiente de si su `source`
es `seed` o `ai`.

### Categoría de receta
Clasificación (pasta, arroz, sopas, carnes, postres...) que la IA devuelve como `id` de la
Taxonomía y que resuelve la imagen local de la receta.

## Rejected / Ambiguous Terms

### Alergia
Usar **Restricción dura**. Motivo: el concepto incluye intolerancias (lactosa, gluten) y
restricciones de seguridad que no son alergias médicas.

### Dieta
Término ambiguo (¿régimen semanal? ¿plan de comidas? ¿patrón?). Usar **Patrón de dieta** para la
elección única, y **Restricción dura** o **Preferencia blanda** para el resto.

### Receta guardada
Usar **Favorito**. Motivo: "guardada" se confunde con la caché de recetas generadas, que también
persiste en el dispositivo.

### Usuario
Usar **Usuario local**. Motivo: no hay cuentas ni login; el usuario es el dispositivo.

### Recomendación
Usar **Sugerencia**. Motivo: "recomendación" sugiere personalización por historial, y en el MVP no
hay motor de recomendación.

### Buscador
Término ambiguo. Distinguir siempre: **Búsqueda con IA** (texto libre que la IA resuelve generando
recetas) y **Filtros de resultados** (filtrar en local lo que ya está en el dispositivo). Nunca
"buscador" a secas: sugiere un catálogo remoto que no existe.
