# Feature Implementation Spec: Taxonomía tipada con test de integridad

## Source Feature

- `id`: `taxonomy-in-code`
- `area`: `domain`
- `depends_on`: `["bootstrap-android-project"]` (satisfecho: `accepted`)
- `status`: `not_started` (en el momento de planificar)
- `source`: `feature_list.json`

## Goal

Hacer que la aplicación pueda **leer su propio catálogo de opciones**. Hoy
`docs/domain/taxonomy.json` es la fuente única de verdad del producto (ADR 0001), pero la app no
puede acceder a él: cualquier pantalla o regla que necesite la lista de dietas, alérgenos,
ingredientes o categorías tendría que escribirla a mano, y tendríamos la misma lista copiada en el
onboarding, los filtros, el constructor de prompt y el Validador.

Esta feature entrega el catálogo como pieza de código: un fichero que viaja dentro del APK, un
parser puro que lo convierte en objetos tipados, un catálogo consultable por `id`, y un test que
hace imposible que el catálogo se rompa en silencio.

No hay nada visible para el usuario. Es la cimentación de las 25 features siguientes.

## Non-Goals

- Ninguna UI: no se dibuja nada en pantalla. El onboarding llega en `onboarding-shell-and-identity`.
- Nada de Room ni persistencia.
- Nada de assets de iconos o imágenes. Solo se verifica que el **mapa de fallback** está definido.
- No se implementa el catálogo de recetas semilla (otra feature).
- No se construye el prompt ni se validan respuestas de IA (features posteriores).
- No se exponen los metadatos de documentación del JSON (`cardinality`, `usedIn`, `purpose`...).

## Job Story

Cuando construyo cualquier pantalla o regla que dependa de las opciones del producto,
quiero leerlas del catálogo único,
para no duplicar listas ni divergir de lo que dicen los documentos.

## Users And Permissions

- **Desarrollador / agente**: puede leer el catálogo desde el dominio, sin permisos ni red.
- **Usuario local**: no interactúa con esta feature. Su efecto se verá cuando existan pantallas.

## Acceptance Scenarios

### Scenario 1: el catálogo se carga completo

Given el asset de la Taxonomía generado por el build,
When se parsea,
Then se obtienen las colecciones con sus tamaños reales: 5 patrones de dieta, 11 restricciones
duras, 7 preferencias blandas, 5 tipos de comida, 10 cocinas, 5 franjas de tiempo, 3 dificultades,
8 colores de avatar, 95 ingredientes y 18 categorías de receta.

### Scenario 2: el asset viaja dentro del APK

Given un APK de depuración generado con `./gradlew assembleDebug`,
When se inspecciona su contenido,
Then contiene `assets/taxonomy.json`.

### Scenario 3: consulta por id

Given la Taxonomía cargada,
When se pide `ingredient("tomate")` y `ingredient("no-existe")`,
Then devuelve el objeto del tomate y `null` respectivamente, sin lanzar excepción.

### Scenario 4: las invariantes se cumplen sobre el catálogo real

Given la Taxonomía real,
When se ejecuta el test de integridad,
Then se cumplen: ids únicos por colección, `ingredient.allergens[]` dentro de `hardRestrictions.id`,
`ingredient.tags[]` dentro de la unión de `dietPatterns[].excludedTags`, todos los
`avatarColors[].hex` con formato `#RRGGBB`, y todo `ingredient.category` presente en
`categoryFallbackIcon`.

### Scenario 5: un catálogo roto es rechazado (caso negativo obligatorio)

Given un JSON con una errata deliberada, por ejemplo un ingrediente con
`"allergens": ["glúten"]` (con tilde, id inexistente),
When se ejecuta el test de integridad sobre ese JSON,
Then falla.

Sin este caso negativo el test de integridad podría estar pasando sin comprobar nada.

## Repository Research

### Files Inspected

- `app/build.gradle.kts` — plugins, dependencias y ausencia de configuración de tests. No hay tarea
  de assets.
- `gradle/libs.versions.toml` — **no incluye `kotlinx.serialization`** ni su plugin. Solo Compose,
  AndroidX y JUnit.
- `app/src/main/java/com/sazon/app/` — solo existe `ui/theme`. **No hay paquete `domain` ni `data`.**
- `app/src/test/java/com/sazon/app/` — solo `ExampleUnitTest.kt`, el test de plantilla.
- `docs/domain/taxonomy.json` — la fuente de verdad, con dos formas: colecciones envueltas en
  `{ items: [...] }` con metadatos, y objetos sueltos como `servings` y `_meta`.
- `docs/domain/taxonomy.md` — reglas, invariantes y resolución de rutas de iconos e imágenes.
- `docs/adr/0001-taxonomy-in-code.md` — la Taxonomía vive en código y **no** se persiste.
- `docs/technical-discovery.md` — `kotlinx.serialization` ya figura en el stack; el dominio no debe
  depender de Android para poder testarse sin emulador.
- `docs/specs/bootstrap-android-project.md` — dejó dicho que el test de plantilla se borra cuando
  exista el primero real.

### Existing Patterns To Follow

- Identificadores en inglés; texto y comentarios en español.
- Kotlin DSL para Gradle y catálogo de versiones en `gradle/libs.versions.toml`.
- El camino de verificación es `init.sh` (`./gradlew test` y `./gradlew lint`).
- El puerto `RecipeGenerator` y los ADRs fijan la inversión de dependencias como patrón del proyecto.

### Current Gaps

- **No existe `app/src/main/assets/`.** Nada obliga hoy a que el JSON viaje en el APK.
- **No hay librería de JSON.** Sin ella no hay parseo tipado. `org.json` está descartado: es una API
  de Android y no funciona en los tests de JVM, que es justo donde queremos verificar.
- **No hay paquete de dominio.** Hay que crearlo.
- **El JSON contiene metadatos de documentación** (`cardinality`, `usedIn`, `purpose`, `rule`...) que
  la app no necesita, y un parseo estricto sin declararlos fallaría.
- **`docs/` no se empaqueta en el APK**, así que el fichero necesita llegar a `assets/` de alguna
  forma sin duplicar la fuente.

## Technical Approach

### 1. Cómo llega el JSON al APK

`docs/domain/taxonomy.json` sigue siendo la única fuente editable. Una **tarea `Copy` de Gradle**
(`syncTaxonomy`) lo copia a `app/src/main/assets/`, la ubicación estándar que AGP ya escanea, y las
tareas `merge*Assets` y `test` dependen de ella. El fichero copiado **se ignora en git**: es
artefacto, no fuente.

```
docs/domain/taxonomy.json --(syncTaxonomy)--> app/src/main/assets/taxonomy.json
```

Descartadas: **duplicar** el fichero (institucionaliza la divergencia que llevamos toda la sesión
eliminando) y **moverlo** a `assets/` (rompería las referencias de `domain-model.md`, `taxonomy.md`,
`feature_list.json` y el ADR 0001). Además, el catálogo de recetas semilla necesitará este mismo
patrón, así que se establece una vez.

El plan inicial era un directorio de assets generado en `build/` y registrado con `assets.srcDir`.
**AGP 9.3.3 lo rechaza**: no admite `Provider` en el SourceSet API. Ver Hallazgos.

### 2. Cómo se parsea

`kotlinx.serialization`, ya previsto en `technical-discovery.md`: plugin
`org.jetbrains.kotlin.plugin.serialization` más `kotlinx-serialization-json`, con
`Json { ignoreUnknownKeys = true }`.

Es una decisión con coste: el JSON lleva metadatos de documentación que no queremos modelar. A cambio,
**todo campo que la app consume se declara obligatorio**, así que un renombrado (`label` → `labell`)
sigue rompiendo el test. El riesgo residual es una errata en un campo opcional no modelado.
`org.json` está descartado: es API de Android y no funciona en tests de JVM.

### 3. La costura Android / dominio

Existe para que **el test corra en JVM sin emulador**, que es el objetivo declarado del proyecto:

| Capa | Paquete | Responsabilidad | ¿Android? |
|---|---|---|---|
| Modelo y parser | `com.sazon.app.domain.taxonomy` | `TaxonomyJson.parse(json: String): Taxonomy` | **No** |
| Lectura del asset | `com.sazon.app.data` | `AssetTaxonomySource(context)`: lee y delega | Sí |

El parser recibe un `String`; leer bytes es cosa de la capa de datos.

### 4. Catálogo expuesto

`Taxonomy` expone las colecciones y búsqueda por id: `dietPatterns`, `dietPattern(id)`,
`hardRestrictions`, `restriction(id)`, `ingredients`, `ingredient(id)`, `recipeCategories`,
`categoryFallbackIcon`... Modelos: `DietPattern`, `HardRestriction`, `SoftPreference`, `MealType`,
`Cuisine`, `TimeBucket`, `Difficulty`, `Servings`, `AvatarColor`, `Ingredient`, `RecipeCategory`.
Los imports de Android están **prohibidos** en ese paquete.

### 5. Modo de fallo

**Fallar rápido**: si el asset falta o está malformado, la carga lanza excepción en el arranque. Un
catálogo ilegible significa que nada funciona, y degradar en silencio daría pantallas vacías sin
explicación. No es alcanzable en un build correcto: el test lo cubre.

**Sin validación de invariantes en ejecución.** El test es la garantía; comprobar 95 ingredientes en
cada arranque sería coste sin beneficio.

## Expected File Changes

- `gradle/libs.versions.toml` — modificar; añadir el plugin de serialización y la librería JSON.
- `app/build.gradle.kts` — modificar; aplicar el plugin, declarar dependencias, registrar la tarea
  `Copy` y el `assets.srcDir`, y pasar la ruta del asset al test por propiedad de sistema.
- `app/src/main/java/com/sazon/app/domain/taxonomy/Taxonomy.kt` — crear; modelos y catálogo.
- `app/src/main/java/com/sazon/app/domain/taxonomy/TaxonomyJson.kt` — crear; parser puro.
- `app/src/main/java/com/sazon/app/data/AssetTaxonomySource.kt` — crear; lectura del asset.
- `app/src/test/java/com/sazon/app/domain/taxonomy/TaxonomyIntegrityTest.kt` — crear; integridad,
  tamaños, consulta por id y caso negativo.
- `app/src/test/java/com/sazon/app/ExampleUnitTest.kt` — **eliminar**; lo previó el bootstrap al
  aparecer el primer test real.
- `.gitignore` — modificar; ignorar el asset generado.
- `docs/technical-discovery.md` — modificar; dejar constancia de cómo llega la Taxonomía al APK.

## Visual Design Impact

- UI involved: **no**.
- Design source: no aplica.
- Screens or states affected: ninguna.
- New design artifact required: no.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio omite este fichero por decisión del harness. La
  costura dominio/datos se documenta aquí y en `technical-discovery.md`.
- `CONSTRAINTS.md`: **not needed** — la regla "el dominio no depende de Android" ya está en
  `technical-discovery.md`. Solo si se repite en varias features convendría elevarla.
- `AGENTS.md`: **not needed** — el flujo, el arranque y las reglas no cambian.
- Other docs: `docs/technical-discovery.md` — update; documentar la sincronización
  `docs/domain/taxonomy.json` → assets generados y la elección de `kotlinx.serialization`.
  `PROGRESS.md` y `feature_list.json` — update; estado y evidencia.

## Implementation Plan

1. Añadir `kotlinx.serialization` al catálogo de versiones, al `build.gradle.kts` de la app y al
   `.gitignore`.
2. Registrar la tarea `Copy` y el `assets.srcDir` generado; comprobar que el asset aparece tras
   `./gradlew assembleDebug`.
3. Escribir los modelos y el parser puro en `domain.taxonomy`.
4. Escribir la capa de datos que lee el asset y delega en el parser.
5. Escribir el test de integridad **incluyendo el caso negativo**, y borrar `ExampleUnitTest.kt`.
6. Ejecutar `init.sh`, comprobar el contenido del APK y actualizar la documentación y el harness.

## Implementation Tasks

- [x] Añadir el plugin de serialización y la librería JSON al catálogo de versiones.
- [x] Aplicar el plugin y las dependencias en `app/build.gradle.kts`.
- [x] Crear la tarea `syncTaxonomy` y engancharla a las tareas `merge*Assets` y `test`.
- [x] Ignorar en git el asset generado.
- [x] Escribir los modelos tipados y el catálogo consultable por id.
- [x] Escribir `TaxonomyJson.parse(json: String): Taxonomy`, sin imports de Android.
- [x] Escribir `AssetTaxonomySource` para leer el asset.
- [x] Escribir el test de integridad sobre la Taxonomía real.
- [x] Escribir dos casos negativos con un JSON roto a mano.
- [x] Borrar `ExampleUnitTest.kt`.
- [x] Verificar `init.sh` en verde y que el APK contiene `assets/taxonomy.json`.
- [x] Actualizar `docs/technical-discovery.md`, `PROGRESS.md` y `feature_list.json`.

## Verification Plan

- `./gradlew test` → verde. El caso negativo se implementa como aserción sobre una función de
  validación, no como un test que se espera rojo.
- `./gradlew lint` → sin errores.
- `./gradlew assembleDebug` → APK generado; `unzip -l` debe listar `assets/taxonomy.json`.
- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → código 0.

Cobertura E2E: **no aplica**. No hay suite E2E persistente y la feature no cambia comportamiento
observable. El nivel adecuado es el test de JVM, que es justo lo que la costura dominio/datos permite.

## Evidence To Capture

- Salida de `./gradlew test` con el número de tests ejecutados.
- Salida de `init.sh` con código 0.
- Listado del APK mostrando `assets/taxonomy.json`.
- Resultado del caso negativo: el test que confirma que un catálogo roto es rechazado.
- Confirmación de que el asset generado no aparece en `git status` (ignorado).

## Validator Checklist

- [x] La implementación se mantiene dentro del alcance: sin UI, sin Room, sin assets de imágenes, sin
      catálogo semilla, sin prompt ni validador de recetas.
- [x] Los cinco escenarios de aceptación pasan, incluido el caso negativo.
- [x] El paquete `domain.taxonomy` **no importa nada de Android**; se puede comprobar por inspección y
      el test corre sin emulador.
- [x] El asset generado está ignorado en git y la única fuente editable sigue siendo
      `docs/domain/taxonomy.json`.
- [x] Los tamaños declarados en el escenario 1 coinciden con el JSON real.
- [x] No se han añadido dependencias que no se usen.
- [x] `docs/technical-discovery.md`, `PROGRESS.md` y `feature_list.json` están actualizados de forma
      coherente.
- [x] `ExampleUnitTest.kt` ya no existe.

Veredicto del validador independiente: **`accept`** (ver `PROGRESS.md`, sesión 006).

## Findings During Implementation

1. **El JSON canónico tenía BOM y eso rompía el parseo.** Los cinco tests fallaron en la primera
   ejecución con `JsonDecodingException: ... had '���' instead`. La causa: `docs/domain/taxonomy.json`
   empezaba con BOM de UTF-8, introducido sesiones atrás por un `Set-Content -Encoding UTF8` de
   PowerShell 5.1 (que escribe BOM). Es el **único** fichero con BOM de los 43 versionados.
   Eliminado (3 bytes menos, 22956). Sin el test de integridad, la app habría **fallado al arrancar**
   con un error críptico. Es el mejor argumento posible a favor del caso negativo y del test real.
2. **AGP 9.3.3 rechaza `Provider` en el SourceSet API.** El plan de `assets.srcDir` con la tarea
   `Copy` falla con *"You cannot add Provider instances to the Android SourceSet API"*, y `srcDir`
   además está deprecado. En lugar de adoptar la API de variantes (más ceremonia para un caso
   sencillo), la tarea escribe directamente en `app/src/main/assets/` y las dependencias se declaran
   de forma explícita. Se descartó la salida de emergencia
   `android.sourceset.disallowProvider=false`: desactivar una salvaguarda de AGP no es arreglarlo.
3. **`.gitignore` sí necesitaba cambio.** La spec lo daba por dudoso; el asset se escribe ahora en el
   árbol de fuentes, así que se ignoró explícitamente `app/src/main/assets/taxonomy.json`.
4. **Versión de `kotlinx.serialization`: 1.9.0**, no la última estable (1.11.0). Se eligió una
   contemporánea a Kotlin 2.2.10 para evitar incompatibilidad de metadatos. Lint avisa de que hay
   versión más nueva; es esperado y no bloquea.
