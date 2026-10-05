# Feature Implementation Spec: Firma de preferencias determinista

## Source Feature

- `id`: `preference-signature`
- `area`: `domain`
- `depends_on`: `["profile-persistence"]` (satisfecho: `accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`; el cambio de estado lo hacen
  implementer/validador/orquestador.
- `source`: `feature_list.json`

## Goal

Derivar de forma **determinista** un valor (`preferenceSignature`) a partir del **Perfil de
preferencias** (`PreferenceProfile`), de manera que sirva para decidir si un Lote de sugerencias
generado por IA es obsoleto. Dos perfiles de preferencias idénticos deben producir exactamente la
misma firma; cualquier cambio en una preferencia debe cambiarla; y **cambiar la identidad** (nombre o
color de avatar) **no** debe cambiarla.

La firma **excluye la identidad** (regla crítica registrada en `docs/domain-model.md` y `CONTEXT.md`):
editar el nombre o el color de avatar no debe invalidar recetas ni gastar una llamada de IA. Esta
feature entrega lógica pura de dominio y su test JVM; no persiste la firma ni la conecta al Lote de
sugerencias.

## Non-Goals

- **No** se persiste la firma ni se asocia a ninguna fila/entidad. Se guardará con el
  `SuggestionBatch` en `suggestion-batch-cache`.
- **No** se implementa la política cache-first, la detección de lote obsoleto ni el refresco.
- **No** hay UI ni navegación.
- **No** se leen ni validan los `*Id` contra la Taxonomía: la firma hashea lo que recibe. La
  validación de ids es del Validador / de la construcción del perfil.
- **No** se modifica `PreferenceProfile`, `UserProfile`, `ProfileRepository` ni la capa Room
  (`data.profile`). Esta feature es aditiva.
- **No** se añaden dependencias (ni cripto, ni serialización nuevas). `java.security.MessageDigest`
  es JDK estándar.
- **No** se introduce criptografía con clave, sal ni aleatoriedad: la firma debe ser reproducible.

## Job Story

Cuando el usuario edite sus preferencias,
quiero que la app pueda detectar de forma fiable que lo que se le propuso antes ya no encaja,
para refrescar las sugerencias solo cuando de verdad han cambiado las preferencias y no cuando solo
ha cambiado su nombre o su avatar.

## Users And Permissions

- **Usuario local**: no interactúa con esta feature (no hay UI). Se beneficia indirectamente de que
  cambiar de identidad no invalide sugerencias.
- **Desarrollador / agente**: ejecuta y verifica la firma como lógica pura en JVM, sin emulador, sin
  red y sin permisos.

## Acceptance Scenarios

### Scenario 1: dos perfiles idénticos dan la misma firma (verificación exigida)

Given dos `PreferenceProfile` con los mismos valores en todos sus campos de preferencia,
When se calcula la firma de cada uno,
Then ambas firmas son **iguales** (y estables al recalcular).

### Scenario 2: cada preferencia altera la firma (verificación exigida)

Given un `PreferenceProfile` de referencia y su firma,
When se modifica **uno** de estos campos —`dietPatternId`, `hardRestrictionIds`, `softPreferenceIds`,
`cuisineIds`, `preferredIngredientIds`, `dislikedIngredientIds`, `difficultyId`, `timeBucketId` o
`defaultServings`—,
Then la firma resultante es **distinta** de la de referencia (una comprobación por campo).

Añadir o quitar un elemento de una lista también altera la firma.

### Scenario 3: el nombre no altera la firma (verificación exigida)

Given un `UserProfile` y su firma calculada sobre `preferences`,
When se cambia `displayName` (manteniendo `preferences`),
Then la firma es **idéntica** a la anterior.

### Scenario 4: el color de avatar no altera la firma (verificación exigida)

Given un `UserProfile` y su firma calculada sobre `preferences`,
When se cambia `avatarColorId` (manteniendo `preferences`),
Then la firma es **idéntica** a la anterior.

### Scenario 5: el orden de las listas no altera la firma

Given un `PreferenceProfile` con una lista en un orden,
When se pasan **los mismos elementos en otro orden** (o con duplicados) en cualquier lista de
preferencias,
Then la firma es **idéntica**: las listas son conjuntos (multi-select) y el orden no es semántico.

### Scenario 6: los sellos de tiempo no alteran la firma

Given un `PreferenceProfile`,
When se cambia `updatedAt` (o `createdAt` en el `UserProfile` que lo contiene),
Then la firma es **idéntica**: los sellos de tiempo no son una preferencia. Sin esta exclusión,
editar solo el nombre (que avanza `updatedAt`) cambiaría la firma y rompería el Scenario 3.

### Scenario 7: estabilidad del algoritmo (vector dorado)

Given un `PreferenceProfile` fijo,
When se calcula la firma,
Then coincide con un **vector dorado** fijado en el test, de modo que un cambio accidental del
algoritmo se detecte como regresión.

## Repository Research

### Files Inspected

- `feature_list.json` — definición y verificación de `preference-signature`; dependencia
  `profile-persistence` en `accepted`.
- `PROGRESS.md` — sesión 010: `preference-signature` desbloqueada; estado y camino de verificación.
- `CONTEXT.md` — glosario "Firma de preferencias": **excluye** nombre y color de avatar.
- `docs/domain-model.md` — `PreferenceProfile` (campos exactos), qué **incluye** y **excluye** la
  firma, y los Scenarios 4 y 7 de cambio de preferencias/identidad.
- `app/src/main/java/com/sazon/app/domain/profile/UserProfile.kt` — campos reales de `UserProfile` y
  `PreferenceProfile` (todos no-null; `updatedAt: Long`).
- `app/src/main/java/com/sazon/app/domain/profile/ProfileName.kt` — patrón de objeto de dominio puro.
- `app/src/main/java/com/sazon/app/domain/profile/ProfileRepository.kt` — costura de dominio, sin
  Android.
- `app/src/main/java/com/sazon/app/domain/recipe/RecipeJson.kt` — patrón de objeto Kotlin puro con
  serialización.
- `app/src/test/java/com/sazon/app/domain/recipe/RecipeSerializationTest.kt` — patrón de test JUnit 4,
  nombres de test en español con `_`, helpers `private fun` al final.
- `app/build.gradle.kts` — JUnit 4 disponible en `testImplementation`; `domain.profile` ya compila sin
  dependencias nuevas.
- `docs/technical-discovery.md` — "tests unitarios de dominio en JVM" incluye la firma.
- `docs/domain/taxonomy.md` — invariante: `id` en `snake_case`, sin acentos (importante para elegir
  un separador de canonicalización que nunca colisione).
- `docs/risks-and-open-questions.md` — sin preguntas abiertas que bloqueen esta feature.

### Existing Patterns To Follow

- Paquetes de dominio puros `com.sazon.app.domain.<área>`, **sin** `android.*`; `domain.profile` ya
  existe y es donde vive `PreferenceProfile`.
- Identificadores de código en inglés; texto/comentarios de dominio en español.
- Tests JUnit 4 en `app/src/test/...`, sin Robolectric cuando no se necesita Android (este caso).
- Verificación única: `./gradlew test` + `./gradlew lint` vía `init.sh`.

### Current Gaps

- No existe ningún cálculo de firma: ni `PreferenceSignature`, ni test.
- No hay un sitio canónico decidido para la firma; esta spec lo fija en `domain.profile` junto a
  `PreferenceProfile`.

## Technical Approach

Lógica pura, sin Android, sin Room y sin coroutines. Todo vive en
`com.sazon.app.domain.profile`.

### 1. Objeto `PreferenceSignature`

`object PreferenceSignature` con una única entrada:

- `fun of(preferences: PreferenceProfile): String`

El parámetro es **`PreferenceProfile`**, no `UserProfile`: así el compilador garantiza
estructuralmente que la identidad (`displayName`, `avatarColorId`) no puede entrar en la firma. Los
llamantes que tengan un `UserProfile` pasan `profile.preferences`. No se añade una sobrecarga
`of(UserProfile)` para no abrir la puerta a incluir la identidad.

No incluye ningún método de persistencia ni de comparación de lotes (eso es
`suggestion-batch-cache`).

### 2. Qué entra y qué no

- **Incluye** (en este orden fijo): `dietPatternId`, `hardRestrictionIds`, `softPreferenceIds`,
  `cuisineIds`, `preferredIngredientIds`, `dislikedIngredientIds`, `difficultyId`, `timeBucketId`,
  `defaultServings`.
- **Excluye**: `updatedAt` (sello temporal, no preferencia), y toda la identidad
  (`displayName`, `avatarColorId`, `createdAt`).

### 3. Canonicalización (semántica de conjuntos)

Para que la firma sea estable y no produzca falsos "obsoletos":

- Cada lista se normaliza con `distinct().sorted()` (conjunto, sin duplicados, orden fijo).
- Se construye una cadena canónica en **orden de campos fijo**, con un prefijo de versión de
  algoritmo (`"v1"`) y separadores que **no pueden aparecer en un `id`** (los ids son `snake_case`
  `[a-z0-9_]`): usar `\u001F` (unit separator) entre campos y entre elementos de una lista.
  Documentar en el código que el separador se elige porque los ids de la Taxonomía no lo contienen.
- `defaultServings` se incorpora con `toString()`.

Ejemplo conceptual de cadena canónica (los `\u001F` son reales, no texto):

```
v1\u001FdietPatternId\u001F<sorted hardRestrictionIds>\u001F<sorted softPreferenceIds>\u001F...
```

### 4. Hash determinista

- `MessageDigest.getInstance("SHA-256")` sobre `canonical.toByteArray(Charsets.UTF_8)`.
- Codificar en hexadecimal **minúscula** de 64 caracteres (`"%02x".format(byte)` o equivalente).
- SHA-256 es estándar y determinista entre ejecuciones y versiones; evita depender de
  `data class.hashCode()` (acoplado a la estructura de la clase).
- La constante de versión (`"v1"`) permite cambiar el algoritmo en el futuro de forma explícita. Si el
  algoritmo cambia, las firmas guardadas dejan de coincidir y el lote se considerará `stale` una vez
  (comportamiento seguro y aceptable); anotarlo si ocurre.

### 5. Test JVM puro

`PreferenceSignatureTest` en `app/src/test/java/com/sazon/app/domain/profile/`, JUnit 4, sin
Robolectric ni contexto Android. Helpers `private fun perfilDeReferencia()` para construir el
`PreferenceProfile` base y mutarlo con `copy(...)` por campo. Incluir el vector dorado del
Scenario 7 con el valor real obtenido al implementar.

## Expected File Changes

- `app/src/main/java/com/sazon/app/domain/profile/PreferenceSignature.kt` — **crear**; objeto puro con
  `of(preferences)` y la canonicalización.
- `app/src/test/java/com/sazon/app/domain/profile/PreferenceSignatureTest.kt` — **crear**; escenarios
  1–7.
- `docs/technical-discovery.md` — **modificar**; nota breve de que la firma vive en
  `domain.profile` (`PreferenceSignature.of`), pura y sin Android.
- `docs/domain-model.md` — **modificar (mínimo)**; aclarar que la firma trata las listas como
  conjuntos (insensible al orden) y que `updatedAt` también queda excluido por ser sello temporal.
- `PROGRESS.md` y `feature_list.json` — **modificar al implementar**; estado y evidencia (no en esta
  sesión de planificación).

No se tocan `UserProfile.kt`, `ProfileRepository.kt`, `data.profile/**`, el build ni la Taxonomía.

## Visual Design Impact

- UI involved: **no**.
- Design source: no aplica.
- Screens or states affected: ninguna.
- New design artifact required: no.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio omite este fichero por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — la regla "la firma excluye la identidad" ya vive en `CONTEXT.md`
  y `docs/domain-model.md`; esta spec la refuerza y añade la exclusión de `updatedAt`.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas de trabajo no cambian.
- Other docs: `docs/domain-model.md` — update mínimo (semántica de conjuntos y exclusión de
  `updatedAt`); `docs/technical-discovery.md` — update ligero (ubicación de la firma); `PROGRESS.md` y
  `feature_list.json` — update al implementar.

## Implementation Plan

1. Crear `PreferenceSignature` en `domain.profile`: canonicalización de conjuntos y hash SHA-256 hex.
2. Escribir `PreferenceSignatureTest` con los escenarios 1–7; capturar el vector dorado tras la
   primera ejecución correcta.
3. Ejecutar `./gradlew test` y `./gradlew lint` vía `init.sh`; corregir lo que salga.
4. Actualizar `docs/technical-discovery.md` y `docs/domain-model.md`.
5. Registrar evidencia y estado en `feature_list.json` y `PROGRESS.md` (lo hace el implementer).

## Implementation Tasks

- [x] Crear `object PreferenceSignature` en `com.sazon.app.domain.profile`.
- [x] Implementar `of(preferences: PreferenceProfile): String` sin recibir `UserProfile`.
- [x] Canonicalizar: `distinct().sorted()` por lista, orden de campos fijo, separador `\u001F` y
      prefijo de versión `"v1"`.
- [x] Excluir `updatedAt` y la identidad; incluir los nueve campos de preferencia.
- [x] Calcular SHA-256 en hex minúscula sobre UTF-8 con `java.security.MessageDigest`.
- [x] Escribir `PreferenceSignatureTest` con Scenario 1 (iguales → igual firma) y Scenario 2 (una
      comprobación por cada campo de preferencia).
- [x] Añadir Scenario 3 (nombre no altera), Scenario 4 (avatar no altera) y Scenario 6
      (`updatedAt`/`createdAt` no alteran).
- [x] Añadir Scenario 5 (orden/duplicados de lista no alteran) y Scenario 7 (vector dorado).
- [x] Ejecutar `init.sh` en verde y actualizar `docs/technical-discovery.md` y `docs/domain-model.md`.
- [x] Registrar evidencia y estado en `feature_list.json` y `PROGRESS.md`.

## Verification Plan

- `./gradlew test` → verde, con `PreferenceSignatureTest` ejecutado y **0 fallos**. Opcional para
  depurar: `./gradlew test --tests "com.sazon.app.domain.profile.PreferenceSignatureTest"`.
- `./gradlew lint` → sin errores nuevos (los 20 warnings preexistentes son aceptables).
- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → código 0.
- Inspección: `com.sazon.app.domain.profile` no importa `android.*` ni `androidx.*`; no hay cambios
  en `data.profile/**` ni en el build.
- Comprobación semántica: la firma cambia con cada campo de `PreferenceProfile` (salvo `updatedAt`) y
  **no** cambia con `displayName`, `avatarColorId`, `createdAt` ni con reordenar una lista.

Sobre `init.sh`: **no cambia**. Debe seguir ejecutando solo comprobaciones no bloqueantes
(`test` + `lint`), **no** arranca procesos largos y no necesita modos nuevos.

Cobertura E2E: **no aplica**. No hay suite E2E persistente, no hay UI ni flujo de usuario; el
comportamiento observable (igualdad/desigualdad de firmas) es lógica pura y queda cubierto por el
test JVM exigido en `feature_list.json`.

## Evidence To Capture

- Salida de `./gradlew test` con `PreferenceSignatureTest`, número de tests y 0 fallos.
- Salida de `./gradlew lint` sin errores nuevos.
- Salida de `init.sh` con código 0.
- Vector dorado fijado en el test (valor de firma de referencia) para detectar regresiones.
- Confirmación por inspección de que `domain.profile` no importa Android y que no se tocó la
  persistencia.
- Anotación en `feature_list.json` de la evidencia y cambio de estado a `passing`.

## Decisions And Unknowns

### Decisiones fijadas

- **Vive en `domain.profile`** junto a `PreferenceProfile`, como lógica pura y testeable en JVM. No
  se crea un paquete nuevo.
- **`of(preferences: PreferenceProfile)`**: recibir el `PreferenceProfile` (no el `UserProfile`)
  hace que excluir la identidad sea una garantía estructural, no una convención.
- **`updatedAt` queda excluido** además de la identidad: es un sello temporal, no una preferencia.
  Sin esto, editar el nombre avanzaría `updatedAt` y cambiaría la firma (rompería el Scenario 3).
- **Listas como conjuntos**: `distinct().sorted()`. El orden de selección no es semántico; ignorarlo
  evita refrescos innecesarios del Lote.
- **SHA-256 en hex** sobre una cadena canónica con separador `\u001F` y prefijo de versión `"v1"`.
  Determinista, sin aleatoriedad ni sal, y robusto frente a colisiones de concatenación.
- **Sin dependencias nuevas**: solo `java.security` del JDK.
- **API mínima**: un único `of(...)`. La comparación firma↔perfil y la política de staleness son de
  `suggestion-batch-cache`.

### Unknowns

Ninguna ambigüedad de producto. No hay preguntas abiertas de `docs/risks-and-open-questions.md` que
afecten a esta feature.

## Validator Checklist

- [ ] La implementación se mantiene en `domain.profile` y es lógica pura, sin Android, Room ni
      coroutines, y sin UI.
- [ ] `of(...)` recibe `PreferenceProfile`; no existe sobrecarga que acepte `UserProfile`.
- [ ] Los Scenarios 1–7 pasan, con una comprobación por **cada** campo de `PreferenceProfile`.
- [ ] `displayName`, `avatarColorId`, `createdAt` y `updatedAt` **no** alteran la firma.
- [ ] Añadir/quitar elementos y reordenar/duplicar una lista se comportan según la spec (cambia al
      cambiar el conjunto; no cambia al reordenar el mismo conjunto).
- [ ] La firma es determinista y coincide con el vector dorado.
- [ ] No se añaden dependencias ni se modifican `UserProfile`, `ProfileRepository`, `data.profile/**`
      ni el build.
- [ ] `docs/technical-discovery.md` y `docs/domain-model.md` quedan coherentes con lo implementado.
- [ ] `feature_list.json` y `PROGRESS.md` quedan actualizados con evidencia verificable.
