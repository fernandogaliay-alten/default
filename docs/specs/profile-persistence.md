# Feature Implementation Spec: Persistencia del perfil de usuario y de preferencias

## Source Feature

- `id`: `profile-persistence`
- `area`: `data`
- `depends_on`: `["bootstrap-android-project"]` (satisfecho: `accepted`)
- `status`: `not_started` — esta spec queda **pendiente de aprobación de la persona** antes de
  implementar (acuerdo de `AGENTS.md`). El planner no modifica `feature_list.json` ni `PROGRESS.md`;
  el cambio de estado lo hace el implementer/orquestador tras aprobar y ejecutar.
- `source`: `feature_list.json`

## Goal

Que el **Perfil de usuario** (identidad) y el **Perfil de preferencias** sobrevivan al cierre de la
app. Hoy no existe ninguna persistencia: `docs/domain-model.md` define `UserProfile` y
`PreferenceProfile`, y `docs/technical-discovery.md` y la ADR 0002 deciden que viven en **Room, una
fila**, pero no hay ni modelos de dominio de perfil, ni base de datos, ni repositorio.

Esta feature entrega la capa mínima de persistencia: modelos de dominio puros (`domain.profile`), una
interfaz de repositorio, una base de datos Room de una sola fila en `data.profile` y un test JVM que
guarda, relee y compara. No hay UI: el onboarding y la pantalla Perfil consumirán este repositorio en
features posteriores, y la Firma de preferencias (`preference-signature`) leerá `PreferenceProfile` de
aquí.

## Non-Goals

- **No** se captura ni edita nada desde la UI: ni onboarding (`onboarding-shell-and-identity`) ni
  pantalla Perfil (`profile-screen`).
- **No** se implementa la **Firma de preferencias** (`preference-signature`): esta feature solo
  persiste el `PreferenceProfile`; el hash es de la feature siguiente.
- **No** se introduce el flag de onboarding en DataStore ni la máquina de estados
  `not_started → in_progress → completed`. Se difiere a `onboarding-shell-and-identity`, que es quien
  la usa. En concreto, `technical-discovery.md` menciona DataStore para flags; aquí **no** se añade.
- **No** se persisten recetas, favoritos, lotes de sugerencias ni el contador de actividad.
- **No** se valida que los `*Id` existan en la Taxonomía: eso es del Validador / de la construcción
  del perfil. La persistencia guarda lo que recibe (con la única regla de negocio del nombre).
- **No** se monta inyección de dependencias ni se instancia la base de datos en `MainActivity`.
- **No** se hacen migraciones: esquema en versión 1, `exportSchema = false`.

## Job Story

Cuando la persona complete el onboarding o edite su Perfil,
quiero que su nombre, su color de avatar y todas sus preferencias se guarden en el dispositivo,
para que al cerrar y reabrir la app sigan exactamente ahí, sin pedírselos otra vez.

## Users And Permissions

- **Usuario local**: único dueño de los datos; no hay cuentas, roles ni compartición (ADR 0002).
- **Desarrollador / agente**: construye y testea el repositorio en JVM con base de datos en memoria,
  sin permisos ni red.

## Acceptance Scenarios

### Scenario 1: guardar, releer y comparar (verificación exigida)

Given una base de datos vacía,
When se guarda un `UserProfile` completo (identidad + `PreferenceProfile` con todas sus listas) y se
vuelve a leer,
Then el `UserProfile` leído es **igual** al guardado, con todos sus campos y listas en el mismo orden.

### Scenario 2: el nombre se normaliza al guardar

Given un nombre con espacios alrededor (`"  Ana  "`),
When se guarda el perfil,
Then lo persistido y lo releído es `"Ana"` (recortado).

### Scenario 3: nombre inválido se rechaza

Given un nombre vacío o solo espacios, o de más de 40 caracteres tras recortar,
When se intenta guardar el perfil,
Then `save` falla con `IllegalArgumentException` y **no** queda ninguna fila escrita.

### Scenario 4: perfil ausente

Given una base de datos vacía,
When se consulta el perfil,
Then se devuelve `null` (no se inventa un perfil por defecto: eso es del onboarding).

### Scenario 5: sobrescritura de la fila única

Given un perfil ya guardado,
When se guarda otro `UserProfile`,
Then sigue habiendo **una sola** fila y la lectura devuelve el segundo perfil.

### Scenario 6: los cambios se observan como `Flow`

Given una base de datos vacía,
When se recoge `observe()` y se guarda un perfil,
Then el `Flow` emite `null` primero y luego el perfil guardado.

## Repository Research

### Files Inspected

- `app/build.gradle.kts` — no hay Room, ni KSP, ni coroutines declaradas; solo Compose,
  `kotlinx.serialization` y dependencias de test de plantilla.
- `gradle/libs.versions.toml` — catálogo a extender con Room, KSP, Robolectric y coroutines.
- `build.gradle.kts` (raíz) — el plugin de KSP debe declararse aquí con `apply false`.
- `settings.gradle.kts` — `google()` y `mavenCentral()` ya disponibles; no hay repos que tocar.
- `app/src/main/java/com/sazon/app/domain/taxonomy/Taxonomy.kt` — patrón de dominio puro Kotlin.
- `app/src/main/java/com/sazon/app/data/AssetTaxonomySource.kt` — costura `data` → `domain`.
- `app/src/main/java/com/sazon/app/domain/recipe/Recipe.kt` y `RecipeJson.kt` — precedente de paquete
  de dominio sin Android.
- `app/src/test/java/com/sazon/app/domain/recipe/RecipeSerializationTest.kt` — patrón de test JUnit 4.
- `docs/domain-model.md` — campos exactos de `UserProfile` y `PreferenceProfile`.
- `docs/technical-discovery.md` — Room 1 fila, "el dominio no debe depender de Android".
- `docs/adr/0002-local-first-no-backend.md` — todo el estado del usuario en Room.
- `docs/risks-and-open-questions.md` — sin preguntas abiertas que bloqueen esta feature.
- `init.sh` — gate estándar: `./gradlew test` y `./gradlew lint`.

### Existing Patterns To Follow

- Paquetes de dominio puros `com.sazon.app.domain.<área>`, sin `android.*`; Android solo en `data`.
- Identificadores y `@SerialName`/columnas en inglés; texto de dominio en español.
- Tests JUnit 4 en `app/src/test/...`, nombres de test en español con `_`.
- Verificación única: `./gradlew test` + `./gradlew lint` vía `init.sh`.

### Current Gaps

- No existen `domain.profile` ni `data.profile`.
- **No hay Room ni KSP**: hay que añadir dependencias y plugin (KSP) y `testOptions` de Robolectric.
- No hay ningún test que necesite contexto Android; `profile-persistence` será el primero.
- El proyecto **no tiene** `Application` ni inyección; habrá que ofrecer una fábrica de base de datos
  que las features de UI usarán después.

## Technical Approach

### 1. Modelos de dominio (`com.sazon.app.domain.profile`)

Puros Kotlin, sin Android ni anotaciones Room. **Todos los campos son obligatorios (no-null)**: el
onboarding es completo y la ausencia de perfil se representa con la fila entera a `null`.

- `UserProfile(displayName, avatarColorId, createdAt: Long, preferences: PreferenceProfile)`.
- `PreferenceProfile(dietPatternId, hardRestrictionIds, softPreferenceIds, cuisineIds,
  preferredIngredientIds, dislikedIngredientIds, difficultyId, timeBucketId, defaultServings,
  updatedAt: Long)`, con listas inmutables.
- `ProfileName`: regla de negocio del nombre, en un único sitio.
  `const MIN_LENGTH = 1`, `const MAX_LENGTH = 40`, `fun normalize(raw) = raw.trim()`,
  `fun isValid(raw) = normalize(raw).length in MIN_LENGTH..MAX_LENGTH`.

### 2. Repositorio (`com.sazon.app.domain.profile.ProfileRepository`)

Interfaz de dominio, para que el onboarding y el Perfil dependan de la abstracción y no de Room:

- `suspend fun get(): UserProfile?`
- `fun observe(): Flow<UserProfile?>` (Room devuelve `Flow` de forma nativa).
- `suspend fun save(profile: UserProfile)` — normaliza `displayName` con `ProfileName` y exige que sea
  válido; si no, lanza `IllegalArgumentException` **sin escribir**.
- `suspend fun clear()` — útil para tests y para un futuro reinicio de perfil; `DELETE` de la fila.

Requiere `kotlinx-coroutines-core` explícito en el catálogo (hoy solo llega de forma transitiva).

### 3. Room (`com.sazon.app.data.profile`)

- `ProfileEntity` — tabla `user_profile`, **una sola fila** con `id = SINGLETON_ID (1)` como PK; el
  resto de columnas espeja los campos de los dos modelos de dominio (`createdAt`/`updatedAt` como
  `Long` epoch millis).
- `ProfileDao` — `@Query("SELECT * FROM user_profile WHERE id = 1")` → `Flow<ProfileEntity?>` y
  `suspend`; `@Insert(onConflict = OnConflictStrategy.REPLACE)`; `@Query("DELETE FROM user_profile")`.
- `ListStringConverter` — `@TypeConverter` para `List<String>` usando `kotlinx.serialization` (ya
  presente) en lugar de separadores artesanales, para no romper con comas.
- `ProfileMappers` — `ProfileEntity.toDomain()` / `UserProfile.toEntity()`. El mapeo vive en `data`;
  el dominio no conoce la entidad.
- `RoomProfileRepository(dao)` — implementa `ProfileRepository`; en `save`, `UserProfile` → entidad.
- `ProfileDatabase` — `@Database(entities = [ProfileEntity::class], version = 1,
  exportSchema = false)` y una fábrica `ProfileDatabase.build(context)` que instancia la BD (sin
  construirla en `MainActivity`).
- **Persistencia del orden de las listas**: se conserva el orden tal cual (no se ordena) para que el
  ida y vuelta del Scenario 1 sea exacto.

### 4. Estrategia de test JVM (decisión fijada)

La verificación exige "test de persistencia **en JVM** con base de datos en memoria". Room es una
librería Android, así que el test usa **Robolectric** para ejecutar el runtime Android en la JVM:

- `@RunWith(RobolectricTestRunner::class)`, `@Config(sdk = [33])` (alineado con `minSdk`) y
  `ApplicationProvider.getApplicationContext<Context>()`.
- `Room.inMemoryDatabaseBuilder(context, ProfileDatabase::class.java).allowMainThreadQueries().build()`.
- Alternativa considerada: Room KMP con `BundledSQLiteDriver` (JVM puro, sin Robolectric). Se
  descarta por ahora para no adoptar APIs KMP de Room y mantener el setup Android convencional; queda
  registrada por si Robolectric da problemas con el SDK objetivo.

### 5. Versiones y build

- Añadir plugin KSP (id `com.google.devtools.ksp`) en el catálogo y en la raíz con `apply false`.
- Room `androidx.room:room-runtime` + `room-ktx` (`implementation`) y `room-compiler` (`ksp`).
- `kotlinx-coroutines-core` (`implementation`) y `kotlinx-coroutines-test` (`testImplementation`).
- Robolectric y `androidx.test:core` en `testImplementation`.
- `android { testOptions { unitTests.isIncludeAndroidResources = true } }`.
- **Las versiones concretas** (Room, KSP, Robolectric, coroutines) deben fijarse compatibles con
  Kotlin 2.2.10 y AGP 9.3.3; si el formato KSP `<kotlin>-<ksp>` no existe para 2.2.10, elegir la más
  cercana y anotarlo. Es el único punto de riesgo de build de la feature.

## Expected File Changes

- `gradle/libs.versions.toml` — modificar; añadir Room, KSP, Robolectric, `androidx.test:core` y
  coroutines.
- `build.gradle.kts` (raíz) — modificar; declarar el plugin KSP con `apply false`.
- `app/build.gradle.kts` — modificar; aplicar KSP, dependencias de Room/coroutines/tests y
  `testOptions`.
- `gradle.properties` — modificar (hallazgo de implementación); añadir
  `android.disallowKotlinSourceSets=false`, excepción que AGP 9 documenta para que KSP pueda registrar
  sus fuentes generadas con el Kotlin integrado. Sin ella la configuración falla.
- `app/src/main/java/com/sazon/app/domain/profile/UserProfile.kt` — crear; `UserProfile` y
  `PreferenceProfile`.
- `app/src/main/java/com/sazon/app/domain/profile/ProfileName.kt` — crear; regla 1–40 con recorte.
- `app/src/main/java/com/sazon/app/domain/profile/ProfileRepository.kt` — crear; interfaz.
- `app/src/main/java/com/sazon/app/data/profile/ProfileEntity.kt` — crear; entidad de una fila.
- `app/src/main/java/com/sazon/app/data/profile/ProfileDao.kt` — crear; consultas.
- `app/src/main/java/com/sazon/app/data/profile/ListStringConverter.kt` — crear; conversores.
- `app/src/main/java/com/sazon/app/data/profile/ProfileMappers.kt` — crear; entidad ↔ dominio.
- `app/src/main/java/com/sazon/app/data/profile/RoomProfileRepository.kt` — crear; implementación.
- `app/src/main/java/com/sazon/app/data/profile/ProfileDatabase.kt` — crear; `@Database` + fábrica.
- `app/src/test/java/com/sazon/app/data/profile/RoomProfileRepositoryTest.kt` — crear; escenarios 1–6.
- `docs/technical-discovery.md` — modificar; anotar que el perfil vive en `data.profile` (Room).
- `PROGRESS.md` y `feature_list.json` — modificar al implementar; estado y evidencia.

`docs/risks-and-open-questions.md` no cambia: no hay preguntas abiertas asociadas.

## Visual Design Impact

- UI involved: **no**.
- Design source: no aplica.
- Screens or states affected: ninguna.
- New design artifact required: no.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio omite este fichero por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — "el dominio no depende de Android" ya está documentado.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas no cambian.
- Other docs: `docs/technical-discovery.md` — update ligero con la ubicación de la persistencia de
  perfil; `docs/domain-model.md` — sin cambios (este feature implementa lo ya descrito);
  `PROGRESS.md` y `feature_list.json` — update al implementar.

## Implementation Plan

1. Añadir KSP, Room, coroutines (dominio y test) y Robolectric al catálogo y al build; sincronizar.
2. Crear `domain.profile`: `PreferenceProfile`, `UserProfile`, `ProfileName` y `ProfileRepository`.
3. Crear `data.profile`: entidad, DAO, conversores, mappers, repositorio y fábrica de base de datos.
4. Escribir `RoomProfileRepositoryTest` con los seis escenarios (guarda/relee/compara, recorte,
   rechazo, ausencia, fila única, `Flow`).
5. Ejecutar `init.sh`, corregir lo que salga y actualizar `technical-discovery.md`, `PROGRESS.md` y
   `feature_list.json`.

## Implementation Tasks

- [x] Añadir KSP (catálogo + raíz `apply false`) y aplicarlo en `app`.
- [x] Añadir Room (`runtime`, `ktx`, `compiler` por KSP) y `kotlinx-coroutines-core`.
- [x] Añadir Robolectric, `androidx.test:core` y `kotlinx-coroutines-test` en `testImplementation`, y
      `testOptions.unitTests.isIncludeAndroidResources = true`.
- [x] Crear `PreferenceProfile` y `UserProfile` en `domain.profile`.
- [x] Crear `ProfileName` (recorte + 1–40) en `domain.profile`.
- [x] Crear `ProfileRepository` (get/observe/save/clear) en `domain.profile`.
- [x] Crear `ProfileEntity` (tabla `user_profile`, PK `id = 1`) en `data.profile`.
- [x] Crear `ProfileDao` con consulta por `id = 1`, `Flow`, `INSERT ... REPLACE` y `DELETE`.
- [x] Crear `ListStringConverter` con `kotlinx.serialization`.
- [x] Crear `ProfileMappers` y `RoomProfileRepository` (normaliza y valida el nombre en `save`).
- [x] Crear `ProfileDatabase` (`version = 1`, `exportSchema = false`) con fábrica `build(context)`.
- [x] Escribir `RoomProfileRepositoryTest` con los escenarios 1–6.
- [x] Ejecutar `init.sh` en verde y actualizar `technical-discovery.md`, `PROGRESS.md` y
      `feature_list.json`.

## Verification Plan

- `./gradlew test` → verde, con `RoomProfileRepositoryTest` ejecutado y 0 fallos (base de datos en
  memoria sobre Robolectric).
- `./gradlew lint` → sin errores nuevos.
- `& "C:\Program Files\Git\bin\bash.exe" init.sh` → código 0.
- Inspección: `com.sazon.app.domain.profile` no importa `android.*`; el mapeo a/desde la entidad vive
  en `data`.

Sobre `init.sh`: **no cambia**. Debe seguir ejecutando solo comprobaciones no bloqueantes (`test` y
`lint`), **no** arranca procesos largos y no necesita modos nuevos para esta feature. El test con
base de datos en memoria corre dentro de `./gradlew test`, sin emulador.

Cobertura E2E: **no aplica**. No hay suite E2E persistente, no hay UI y el comportamiento observable
(sobrevivir al reinicio) se comprueba a nivel de persistencia con la base de datos en memoria, que es
lo que exige `feature_list.json`. El reinicio manual en emulador se cubrirá en onboarding/Perfil.

## Evidence To Capture

- Salida de `./gradlew test` con el nombre `RoomProfileRepositoryTest`, número de tests y 0 fallos.
- Salida de `./gradlew lint` sin errores nuevos.
- Salida de `init.sh` con código 0.
- Versiones finalmente fijadas de Room, KSP y Robolectric (para reproducer el build).
- Confirmación por inspección de que `domain.profile` no importa Android.
- Anotación en `feature_list.json` de la evidencia y el cambio de estado a `passing`.

## Decisions And Unknowns

### Decisiones fijadas (acordadas con la persona antes de implementar)

- **Test JVM con Robolectric**, fijando `@Config(sdk = [33])` (alineado con `minSdk`). Si Robolectric
  no soportara el SDK objetivo, la alternativa es Room KMP con `BundledSQLiteDriver`; **no** cambiar a
  test instrumentado, que rompería el requisito "en JVM".
- **Listas como columnas con `TypeConverter` JSON** (no un blob único): una columna por lista,
  convertida con `kotlinx.serialization`, explícita y consultable.
- **Todos los campos no-null**: el onboarding es obligatorio y completo; un perfil ausente es la fila
  entera a `null`. Los fallbacks de identidad corrupta (saludo genérico, primer color de avatar) son
  de la UI, no de la persistencia.
- **`exportSchema = false`** y esquema en versión 1: sin migraciones en el MVP. Una migración futura
  que descarte el perfil es pérdida de datos del usuario; activar la exportación y anotarlo cuando se
  toque el esquema.
- **API completa**: `get`, `observe` (`Flow`), `save` y `clear`. El `Flow` lo necesitará la pantalla
  Perfil; se añade ahora para no cambiar la interfaz después.
- **Escritura de perfil completo**: un único `save(UserProfile)` con `INSERT OR REPLACE`; sin métodos
  granulares de identidad/preferencias.
- **Fallo de `save` por nombre inválido → `IllegalArgumentException`**, sin escribir. La UI valida
  antes, así que no debería alcanzarlo.
- **Versiones de build**: el implementer fija la última estable compatible con Kotlin 2.2.10 / AGP
  9.3.3 (Room, KSP, Robolectric, coroutines) y **registra las elegidas** en la evidencia.
- **`createdAt` / `updatedAt` como `Long` epoch millis**: `docs/domain-model.md` no fija el tipo. Los
  aporta el llamante (onboarding/Perfil); la capa de persistencia los guarda tal cual y **no** los
  genera ni los reescribe. Evita lógica de reloj oculta en el repositorio y mantiene el ida y vuelta
  exacto del Scenario 1.

### Unknowns

- **Riesgo de build**: la combinación de versiones KSP/Room/Robolectric con Kotlin 2.2.10 y AGP 9.3.3
  es la única incógnita real. No es ambigüedad de producto; se resuelve al construir y se documenta.

### Findings During Implementation

- **Versiones fijadas** (todas resueltas y verificadas): KSP `2.2.10-2.0.2` (coincide exactamente con
  Kotlin 2.2.10), Room `2.8.5`, Robolectric `4.17`, `androidx.test:core` `1.7.0` y
  `kotlinx-coroutines` `1.11.0` (compilada con Kotlin 2.2.20, misma versión de metadatos 2.2).
- **AGP 9 + KSP**: AGP 9 compila Kotlin de serie (built-in Kotlin) y bloquea el DSL
  `kotlin.sourceSets` que KSP usa para registrar las fuentes generadas. La solución documentada por
  AGP es `android.disallowKotlinSourceSets=false` en `gradle.properties`; se añadió y el build pasa
  (emite un único warning experimental de AGP, no un error). Por eso `gradle.properties` entra en los
  ficheros cambiados.
- El código generado por KSP de Room se compila en `kspDebugKotlin` / `kspDebugUnitTestKotlin`; la
  base de datos en memoria de Robolectric funciona con `@Config(sdk = [33])`.

## Validator Checklist

- [ ] La implementación se mantiene dentro del alcance: sin UI, sin firma, sin DataStore, sin
      migraciones, sin persistir recetas/favoritos.
- [ ] Los seis escenarios de aceptación pasan, incluido el recorte y el rechazo del nombre.
- [ ] El test corre **en JVM** con base de datos **en memoria** (Robolectric), sin emulador.
- [ ] `domain.profile` no importa Android; `data.profile` es quien conoce Room.
- [ ] Los campos espejan `docs/domain-model.md` (`UserProfile`, `PreferenceProfile`) sin añadidos.
- [ ] Se conserva el orden de las listas en el ida y vuelta.
- [ ] `save` no escribe nada cuando el nombre es inválido (Scenario 3).
- [ ] `app/build.gradle.kts`, `build.gradle.kts` y `libs.versions.toml` solo cambian por lo que la
      feature necesita.
- [ ] `feature_list.json`, `PROGRESS.md` y `docs/technical-discovery.md` quedan actualizados y
      coherentes.
