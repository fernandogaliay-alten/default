# Backend propuesto (post-MVP): cuenta, sincronización y proxy de IA

> **Estado: PROPUESTA. No aceptada, no planificada y fuera del MVP.**
> Este documento no cambia el alcance del MVP ni el estado de ninguna feature. Existe para poder
> retomar la implementación en una fase posterior sin rehacer el análisis. Se documenta la **opción B
> (Kotlin + Ktor)** como dirección recomendada.

## 1. Encuadre

Hoy `docs/build-brief.md` y `docs/adr/0002-local-first-no-backend.md` declaran **backend, login,
sincronización en la nube y multi-dispositivo** como *no-objetivo permanente*, y la clave de IA viaja
en el APK (`BuildConfig`). Construir un backend **revierte decisiones aceptadas**: exige un ADR nuevo
que ajuste el 0002 y actualizar discovery. Este documento anticipa ese trabajo.

Control de versiones del análisis: nada de lo aquí descrito está implementado. `feature_list.json`
sigue siendo la fuente de verdad del trabajo en curso y **no** se ha tocado.

## 2. Decision

### Opción elegida (B): Kotlin + Ktor sirviendo el propio dominio

- **Kotlin + Ktor 3 (motor Netty)**, serialización con `kotlinx.serialization` (reutiliza los DTO de
  receta y perfil tal cual).
- **PostgreSQL** con migraciones **Flyway**; acceso con **Exposed** o JDBC.
- **Módulo Kotlin Multiplatform compartido** con la app (`:shared`) para el dominio puro.

**Ventaja decisiva:** `RecipeValidator`, `PromptBuilder`, `PreferenceSignature`, `MatchScore` y el
modelo `Recipe` son **lógica pura sin Android** (ver `docs/technical-discovery.md`). Extraídos a un
módulo compartido, se ejecutan **idénticos en la app y en el servidor**. Así la garantía "ninguna
receta mostrada incumple una Restricción dura" (`build-brief.md`, Success Criteria) pasa a
verificarse **en servidor**, con el mismo código ya testeado en JVM. Es precisamente la inversión que
el ADR 0002 señalaba como coste de añadir backend.

### Alternativas consideradas y descartadas

1. **Supabase / Firebase (BaaS gestionado).** Rapídisimo (Auth + Postgres + RLS listos) y menos ops,
   pero obliga a **reimplementar el validador** fuera de Kotlin, perdiendo el reuso del dominio y con
   riesgo de divergencia entre la validación de la app y la del servidor. Se descarta salvo que la
   prioridad absoluta sea el tiempo, no el control.
2. **Spring Boot.** Más ceremonia y peso del necesario para este tamaño.
3. **Node / FastAPI.** Perdería todo el reuso del dominio Kotlin. Solo tendría sentido sin módulo
   compartido, lo que reintroduce el problema de la validación duplicada.

## 3. Principios de diseño

1. **Offline-first intacto.** La app debe seguir cumpliendo el Success Criterion: sin red, Home (salvo
   búsqueda IA), "Tus recetas" y Favoritos siguen siendo utilizables. El backend **nunca** es requisito
   para arrancar.
2. **Room es la fuente de verdad en el dispositivo; el servidor es una réplica.** La UI nunca espera a
   la red; la sincronización es un proceso de fondo.
3. **La seguridad se valida en servidor, pero se comparte el código con el cliente** (sección 7).
4. **La clave de IA nunca sale del servidor** y nunca vuelve al APK.
5. **Aditivo y por fases.** Cada fase es útil por sí sola y no rompe el MVP.

## 4. Alcance funcional (servicios)

| Servicio | Qué resuelve | Reutiliza del repo |
|---|---|---|
| **Owner de IA (proxy)** | La app no llama al proveedor: el servidor construye el prompt, llama a DevExpert Inference con su clave, valida y devuelve solo recetas aceptadas. | `domain.prompt.PromptBuilder`, `domain.validation.RecipeValidator`, `domain.recipe` |
| **Autenticación / cuenta** | Registro, login, refresh, logout, borrado de cuenta y datos. | Nada (nuevo) |
| **Perfil y preferencias** | CRUD del `UserProfile` (identidad) + `PreferenceProfile`. | `domain.profile` (modelo, `ProfileName`, `PreferenceSignature`) |
| **Favoritos** | CRUD de la copia de `Recipe` marcada como favorita. | `domain.favorites.Favorite`, `domain.recipe.RecipeJson` |
| **Lote de sugerencias por firma** | Generar el lote **una vez por usuario y firma** en servidor: ahorra cuota de IA y unifica validación y caché entre dispositivos. | `domain.suggestions.*` |
| **Resumen de actividad** | `recipesGeneratedCount` persistido + `favoritesCount` derivado. | `domain-model.md` (ActivitySummary) |
| **Telemetría de IA** | Persistir proveedor, modelo, estado, latencia y **motivo de descarte del validador** (la señal más valiosa del producto). | `DiscardReason` |
| *(opcional)* **Taxonomía remota** | `GET /taxonomy?version=` para actualizar opciones sin publicar app. | `docs/domain/taxonomy.json` |

## 5. Superficie HTTP propuesta

```
# Autenticación
POST   /v1/auth/register           {email, password}            -> {accessToken, refreshToken}
POST   /v1/auth/login              {email, password}            -> {accessToken, refreshToken}
POST   /v1/auth/refresh            {refreshToken}               -> {accessToken, refreshToken}
POST   /v1/auth/logout             {refreshToken}               -> 204
DELETE /v1/me                                                   -> 204   (borra cuenta y datos)
GET    /v1/me                                                   -> {accountId, email, createdAt}

# Perfil
GET    /v1/me/profile                                           -> {profile, version, updatedAt}
PUT    /v1/me/profile              {profile, version}           -> {profile, version, updatedAt}

# Favoritos
GET    /v1/me/favorites?since=<ts>                              -> {items[], tombstones[], serverTime}
PUT    /v1/me/favorites/{recipeId} {recipe, savedAt}            -> {version}
DELETE /v1/me/favorites/{recipeId}                              -> {deletedAt}

# Sugerencias (servidor)
GET    /v1/me/suggestions                                       -> {signature, generatedAt, recipes[]}
POST   /v1/me/suggestions/refresh                               -> {signature, generatedAt, recipes[]}

# IA (proxy)
POST   /v1/ai/generate             {ingredients, constraints}   -> {accepted[], discarded[], aiUnavailable}
POST   /v1/ai/search               {query, profile}             -> {accepted[], discarded[], aiUnavailable}
POST   /v1/ai/suggest              {profile}                    -> {accepted[], discarded[]}

# Actividad y ops
GET    /v1/me/activity                                          -> {recipesGeneratedCount, favoritesCount}
GET    /v1/taxonomy?version=                                    -> {version, taxonomy}   (opcional)
GET    /healthz   GET /readyz                                   -> 200
```

Reglas transversales: `Authorization: Bearer <accessToken>`; errores con un cuerpo uniforme
(`code`, `message`) que reutilice `DiscardReason`/códigos estables; paginación por cursor donde aplique
(favoritos); `Idempotency-Key` en escrituras reintentables.

## 6. Modelo de datos (PostgreSQL)

```
accounts           (id UUID PK, email UNIQUE, password_hash, created_at, deleted_at NULL)
refresh_tokens     (id UUID PK, account_id FK, token_hash, expires_at, revoked_at NULL, device_label)
profiles           (account_id PK/FK, display_name, avatar_color_id,
                    created_at, updated_at, version BIGINT)
preferences        (account_id PK/FK, diet_pattern_id, difficulty_id, time_bucket_id,
                    default_servings, hard_restriction_ids JSON, soft_preference_ids JSON,
                    cuisine_ids JSON, preferred_ingredient_ids JSON, disliked_ingredient_ids JSON,
                    updated_at, version BIGINT)
favorites          (account_id FK, recipe_id, recipe JSON, saved_at,
                    deleted_at NULL, version BIGINT, PK(account_id, recipe_id))
ai_recipes         (id UUID PK, account_id FK, recipe JSON, source, generated_at, last_opened_at)
suggestion_batches (account_id PK/FK, preference_signature, generated_at, recipes JSON, version)
ai_requests        (id UUID PK, account_id FK, endpoint, provider, model, status,
                    latency_ms, discard_reasons JSON, created_at)
usage_counters     (account_id FK, period, requests_used, PK(account_id, period))
```

Notas de modelado:

- Las listas de `preferences` en columnas JSON es coherente con `ProfileEntity` de la app (que ya usa
  JSON con `kotlinx.serialization`); no hace falta normalizar en tablas puente.
- `version` por fila habilita *last-write-wins* y el sync por deltas.
- `favorites` guarda una **copia** de la receta (igual que en Room): sobrevive al refresco del lote y al
  borrado de la receta `ai` de origen.
- `deleted_at` (tombstone) es obligatorio en `favorites` para sincronizar borrados entre dispositivos.

## 7. Módulo compartido (Kotlin Multiplatform) y frontera con la app

Esta es la pieza que da sentido a la opción B.

```
Sazón (proyecto Gradle)
├── :app        Android (Compose, Room, assets) — sigue igual
├── :server     Ktor + Flyway + Postgres
└── :shared     Kotlin Multiplatform, SIN Android
    ├── domain.recipe       (Recipe, RecipeJson, MeasureUnit, ...)
    ├── domain.taxonomy      (modelo + parser del JSON)
    ├── domain.profile       (UserProfile, PreferenceProfile, ProfileName, PreferenceSignature)
    ├── domain.validation    (RecipeValidator, RecipeConstraints, DiscardReason, ...)
    ├── domain.prompt        (PromptBuilder, BuiltPrompt, ...)
    └── (opcional) domain.results  (MatchScore, hoy en ui.results)
```

La app **consume** `:shared` (los paquetes conservan su nombre `com.sazon.app.domain.*`), o se
trasladan con un refactor mecánico y sin cambio de comportamiento. `:server` consume el mismo módulo.

**Consecuencia clave:** el `RecipeValidator` se ejecuta en servidor antes de responder a `/v1/ai/*`; el
test negativo obligatorio del `build-brief` (una receta que incumple una Restricción dura no se
muestra) pasa a tener una segunda barrera del lado servidor **con el mismo código**.

Lo que **no** va al módulo compartido: `data.*` (Room, assets, `AssetTaxonomySource`,
`AssetSeedRecipeSource`, `FakeRecipeGenerator`) ni `ui.*` (Compose). El servidor tiene sus propias
implementaciones de acceso a datos y su propio `RecipeGenerator` real (cliente HTTP al proveedor).

## 8. Autenticación y seguridad

- **Contraseñas:** hash con **Argon2id** (o bcrypt coste ≥ 12). Nunca texto plano ni SHA puro.
- **Tokens:** *access token* JWT de vida corta (p. ej. 15 min) + *refresh token* rotatorio persistido
  en `refresh_tokens` (permite revocar dispositivos y detectar reuso). Alternativa más simple: tokens
  opacos en BD sin JWT.
- **Autorización:** el `accountId` del token determina el dueño de cada fila; activar **Row Level
  Security** en Postgres como red de seguridad contra fugas de aislamiento.
- **Registro duplicado / login fallido / enumeración de usuarios:** respuestas y tiempos uniformes.
- **Borrado (`DELETE /v1/me`):** borrado real (o anonimización) de perfil, preferencias, favoritos,
  caché y tokens. Cubre la feature prevista "Borrar mis datos" y la implicación de RGPD.
- **Rate limiting** por cuenta e IP; **cuotas de IA** en `usage_counters` (la cuota del proveedor es
  semanal: al 100 % las peticiones se rechazan hasta el reset, ver `risks-and-open-questions.md`).
- **Transporte:** TLS lo aporta la plataforma.

## 9. Sincronización offline-first

Algoritmo recomendado (simple y suficiente para el tamaño del dominio):

1. **Room sigue siendo la verdad local.** Toda escritura del usuario va primero a Room y se marca
   `dirty = true` con `updatedAt`/`version`.
2. **Cola de salida** con **WorkManager** (reintentos, restricción de red, backoff). Al recuperar red,
   se empuja lo `dirty` y se hace *pull* de deltas (`?since=`).
3. **Resolución: last-write-wins por entidad.** Perfil/preferencias = objeto entero por `updatedAt`.
   Favoritos = por `savedAt`, con tombstones para el borrado.
4. **Reloj:** usar `serverTime` como referencia autoritativa para `since` y desempatar; el `updatedAt`
   del cliente puede desviarse.
5. **El Lote de sugerencias no viaja como estado sincronizable** en el mismo motor: se sirve/regenera
   por firma de preferencias (`GET /v1/me/suggestions`). El `matchScore` no se persiste (derivado).

Límites aceptados y documentables: LWW pierde cambios concurrentes en el **mismo** campo desde dos
dispositivos; es aceptable para perfil y favoritos y debe registrarse como límite conocido.

## 10. Cliente Android (cambios necesarios)

- **Capa de red:** cliente HTTP (Retrofit o Ktor client) + `kotlinx.serialization`, coherente con el
  stack actual. Un `Authenticator` que refresca el access token ante 401.
- **Tokens:** guardar en `EncryptedSharedPreferences` o DataStore cifrado con **Android Keystore**.
  Nunca en `BuildConfig` ni en texto plano.
- **Repositorios offline-first:** nuevos adaptadores **detrás de los puertos existentes**
  (`ProfileRepository`, `FavoriteRepository`, `SuggestionBatchRepository`) que escriben local y encolan
  sync. No se reescribe el dominio.
- **Inyección de dependencias:** hoy `MainActivity` compone todo a mano sin DI. Con red + auth + sync
  se vuelve inmanejable: introducir **Hilt o Koin** (el `technical-discovery.md` ya contempla "Koin o
  manual"; con backend, DI deja de ser opcional).
- **Sincronización en background:** WorkManager para la cola; la UI nunca bloquea por red.
- **`BuildConfig` de IA:** el bloque `DEVEXPERT_API_KEY`/`local.properties` se retira del APK cuando el
  proxy esté en producción (no antes).

Dependencias nuevas esperables en `gradle/libs.versions.toml`: cliente HTTP, `okhttp` (si Retrofit),
`androidx.security:security-crypto` (o DataStore), `androidx.work:work-runtime-ktx`, y Hilt/Koin.

## 11. Despliegue y operaciones

- **Empaquetado:** imagen **Docker multi-stage** (build con Gradle; runtime con JRE 21 slim).
- **Hosting (de más simple a más control):**
  - Fly.io / Render / Railway + Postgres gestionado (o **Neon**/Supabase para la BD): lo más cómodo
    para Ktor; ~5–15 $/mes.
  - **Cloud Run** (scale-to-zero, barato con poco tráfico) + Postgres externo.
  - VPS (p. ej. Hetzner) + Docker Compose: lo más económico a cierto uso; tú pones TLS y backups.
- **CI/CD:** **GitHub Actions** (el repo ya está en GitHub; `github-flow` publica features). Pipeline:
  test -> build de imagen -> push -> deploy al mergear en `main`. Entornos **dev / staging / prod**.
- **Secretos:** `DEVEXPERT_API_KEY`, credenciales de BD y clave de firma JWT en el gestor de secretos de
  la plataforma / GitHub Secrets. Nunca en el repo, nunca en el APK.
- **Migraciones:** Flyway (idempotente al arrancar o como job de release).
- **Observabilidad:** logs estructurados JSON; **OpenTelemetry** o **Sentry**; `/healthz` bajo monitor
  de uptime. Volcar `ai_requests` (proveedor, estado, latencia, motivos de descarte) es el dato más
  valioso del producto.
- **Backups** automáticos de Postgres + prueba de restauración; **rate limiting**; validación de entrada
  en el borde.

**Coste estimado del MVP de backend:** 0–15 $/mes de infraestructura + coste de IA (que el caché por
firma del Lote ayuda a **reducir**).

## 12. Plan por fases (backlog propuesto, NO volcado en `feature_list.json`)

Cada fase es susceptible de convertirse en una feature del harness (`planner` -> `implementer` ->
`validator`) cuando se retome. Orden por retorno/riesgo: primero el proxy (máximo ROI, sin datos de
usuario), luego cuenta y sync.

| # | Fase | Objetivo | Criterio de aceptación (borrador) |
|---|---|---|---|
| 1 | `shared-domain-module` | Extraer el dominio puro a `:shared` (KMP), sin Android. | `:app` sigue compilando y en verde con los paquetes movidos; tests de dominio intactos. |
| 2 | `ai-proxy-backend` | Servicio Ktor mínimo con `/v1/ai/*`; clave de IA en servidor; validación con `:shared`. | E2E: generar/buscar devuelve solo recetas aceptadas; sin clave en el APK; cuota protegida. |
| 3 | `backend-auth` | Cuenta, tokens, refresh, `DELETE /v1/me`. | Registro/login/refresh/logout; borrado real de datos; tests de aislamiento entre cuentas. |
| 4 | `profile-sync` | Adapter remoto tras `ProfileRepository`, LWW por `updatedAt`, WorkManager. | Editar en dos dispositivos converge; sin red sigue funcionando; sin datos en claro. |
| 5 | `favorites-sync` | Adapter remoto tras `FavoriteRepository` con tombstones. | Guardar/quitar converge entre dispositivos; offline intacto. |
| 6 | `server-suggestions` | Lote generado/validado en servidor por firma. | Misma firma no consume IA; dispositivos comparten lote. |
| 7 | `ai-telemetry` | Persistir `ai_requests` (proveedor, estado, latencia, descartes). | Consultable por cuenta/periodo; sin datos personales innecesarios. |
| 8 | `sync-conflict-hardening` | Endurecer concurrencia y recuperación (correctiva). | Casos de conflicto y reintentos cubiertos por tests. |

## 13. Impacto en el harness y la documentación

Cuando se decida construir esto:

1. **ADR nuevo** (`docs/adr/0004-backend-and-sync.md`) que **sustituya parcialmente el 0002**.
2. **`docs/build-brief.md`:** retirar "backend, login, sincronización, multi-dispositivo" de
   *Fuera del MVP de forma permanente* y reubicarlo como evolutivo.
3. **`docs/technical-discovery.md`:** añadir la sección de backend/sync/módulo compartido a medida que
   se acepte (hoy describe solo el stack local).
4. **`docs/risks-and-open-questions.md`:** nuevo riesgo (operar un servicio), supresión del riesgo
   "clave de IA en el APK" una vez exista el proxy, y preguntas abiertas del proveedor de identidad.
5. **`feature_list.json`:** dar de alta las fases de §12 con `depends_on` y verificación.
6. **Capturas de UI obligatorias:** cualquier feature con pantalla nueva (Iniciar sesión / Crear
   cuenta) exige `docs/design/concepts/<feature-id>/` **antes** de implementar (regla de `AGENTS.md`).

## 14. Riesgos y límites

| Riesgo | Impacto | Mitigación |
|---|---|---|
| El backend se convierte en requisito y rompe el offline | Alto | Room como verdad local; sync de fondo; el arranque nunca espera red. |
| La validación del servidor **diverge** de la del cliente | Alto | Módulo `:shared` con el mismo código; no reimplementar el validador. |
| Complejidad de la sincronización (LWW pierde cambios concurrentes) | Medio | LWW por entidad, tombstones, `serverTime`; documentar el límite. |
| Coste de operar un servicio (lo asumía "el alumno no opera nada") | Medio | Hosting gestionado, CI/CD, backups; valorar BaaS si el tiempo manda. |
| Extracción del dominio a KMP con sorpresas de build | Medio | Fase 1 aislada y verificable antes de tocar red. |
| Fuga de datos personales (email/perfil) | Alto | RLS, cifrado de tokens, borrado real, minimización de datos. |
| Cuota/coste de IA se dispara | Medio | Caché por firma en servidor, cuotas por cuenta, modo semilla intacto. |

## 15. Preguntas abiertas

1. ¿Cuenta por **email+password** o **login social** (Google) como primario?
2. ¿Se sincroniza también la **caché de recetas `ai`** y el grupo "Abiertas hace poco", o solo perfil y
   favoritos? (`lastOpenedAt` es dato de dispositivo).
3. ¿El Lote de sugerencias se sirve desde servidor (ahorro) o se mantiene por dispositivo?
4. ¿Se mueve `MatchScore` (`ui.results`) a `:shared` o queda solo en la app?
5. Proveedor de hosting definitivo y política de backups (RPO/RTO).
6. ¿La Taxonomía pasa a ser remota y versionada, o permanece empaquetada? (hoy "suficientemente
   estable").

## 16. Cómo retomar esta propuesta

Checklist de arranque para la próxima sesión que decida desarrollarla:

- [ ] Confirmar la decisión con la persona y redactar el **ADR 0004** (§13.1).
- [ ] Actualizar `build-brief.md`, `technical-discovery.md` y `risks-and-open-questions.md` (§13).
- [ ] Convertir las fases de §12 en features de `feature_list.json` en orden, con `depends_on`.
- [ ] Validar la pieza más arriesgada primero: la **extracción a `:shared`** (fase 1) en su propio
      worktree, sin tocar red.
- [ ] Resolver las preguntas de §15 que bloqueen la fase que se vaya a especificar.
- [ ] Para cualquier pantalla nueva, crear `docs/design/concepts/<feature-id>/` con sus capturas antes
      de implementar.

## 17. Referencias

- `docs/build-brief.md` — no-objectivos y Success Criteria.
- `docs/adr/0002-local-first-no-backend.md` — la decisión que esta propuesta revisaría.
- `docs/adr/0003-ai-provider.md` — proveedor de IA (DevExpert Inference, API compatible con OpenAI).
- `docs/technical-discovery.md` — stack actual y pureza del dominio (base del módulo `:shared`).
- `docs/domain-model.md`, `CONTEXT.md` — entidades y vocabulario a sincronizar.
- `docs/risks-and-open-questions.md` — cuota de IA, clave en el APK y riesgos abiertos.
