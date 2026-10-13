# Validación independiente — `dish-image-settings-toggle`

- **Feature**: `dish-image-settings-toggle` (`area: ui`, fase 4 de `docs/ai-dish-images-proposal.md`, mitad de ajuste del split)
- **Spec**: `docs/specs/dish-image-settings-toggle.md`
- **Rama / worktree**: `feature/dish-image-settings-toggle`, `/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/dish-image-settings-toggle`
- **Sesión**: 093 (agente validador con contexto limpio, independiente del implementer)
- **Estado previo**: `passing` (implementer, misma sesión, trabajo en disco; `HEAD = c0cb069` = `origin/main`, **sin commit, sin push, sin PR**)
- **Veredicto**: **`accept`**

## Qué se re-ejecutó (sin fiarse del handoff)

| Check | Comando | Resultado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (test + lint vía `scripts/gradle.sh`, sin procesos largos); la pasada salió `UP-TO-DATE`, así que se forzó todo |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | exit 0, **34/34 tareas ejecutadas**; parseo propio de los XML frescos (14:41): **84 suites / 593 tests / 0 fallos / 0 errores / 0 saltados** |
| Lint forzado | `bash scripts/gradle.sh lint --rerun-tasks` | exit 0, **30/30 tareas**; SARIF: **0 errores, 13 warnings** (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1), **0 bajo `app/src`** |
| Baseline re-derivada | `git grep -o '@Test' c0cb069 -- app/src/test` + `git ls-tree` | `HEAD`: **81 ficheros / 562 anotaciones** ⇒ delta exacto **+3 suites / +31 tests** (`DishImageGenerationBudgetTest` 7, `DishImageSettingsTest` 16, `DishImageSettingsCopyTest` 8) |
| Tiradas focales (por XML) | filtrado de los XML por paquete | `data.image` **7 suites / 57 tests** (5 aceptadas de fase 3 + 2 nuevas); `ui.profile` **3 suites / 28 tests** (`DishImageSettingsCopyTest` 8 + `ProfileEditStateTest` 9 + `SuggestionsRefreshFormatTest` 11); 0 fallos/errores/saltados |
| Pureza | `grep -E '^import'` + grep de `android\.`/`androidx\.`/`Random`/`currentTimeMillis` | nuevos `data/image/*.kt` solo importan `java.util.concurrent.atomic.AtomicInteger`, `domain.image.*` y `kotlinx.coroutines.flow.*`; **0** Android/Compose/reloj/azar |
| Constante en un único sitio | `grep -rn MAX_GENERATIONS_PER_SESSION app/src` | declarada **solo** en `DishImageSettingsPolicy` (`= 12`); único literal `12` en `main` es esa línea; referenciada por `DishImageGenerationBudget` (default inyectable) y por 2 aserciones de test |
| Alcance | `git status --porcelain` + `git diff --name-only HEAD` | solo los previstos: 4 `main` nuevos (`data/image/{DishImageGenerationBudget,DishImageSettings}.kt`, `ui/profile/{DishImageSettingsCopy,DishImageSettingsSection}.kt`), 4 `main` modificados (`MainActivity.kt`, `ui/SazonApp.kt`, `ui/MainShell.kt`, `ui/profile/ProfileScreen.kt`), 3 tests nuevos, 4 docs (`DESIGN.md`, `docs/technical-discovery.md`, `docs/adr/0005-ai-dish-images.md`, `docs/risks-and-open-questions.md`), `feature_list.json`, `PROGRESS.md` y la spec (untracked); **sin diff** en `ui/artwork/**`, `domain/**`, ficheros aceptados de `data/image`/`data/ai/image`, `ui/theme/**`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, lockfile, `init.sh`, `scripts/**`, `AndroidManifest.xml`, `assets/**`, `seed-recipes.json`, `docs/design/**` |
| UI/estilo | `grep -rn 'Switch\|toggleable' app/src/main/java/com/sazon/app/ui/profile` + hex | solo en `DishImageSettingsSection.kt`; **0** `0x`/`Color(` sueltos; fila `toggleable(role = Role.Switch)` + `Switch(onCheckedChange = null)`; tokens `MaterialTheme.colorScheme.*` y `SazonSpacing.md` |
| Gate de capturas | `test -d docs/design/concepts/dish-image-settings-toggle` + `DESIGN.md` | carpeta **no existe**; párrafo de excepción con formato #77/#85/#86/#91/**#98** en "Generated Concept Images" y sección "Imágenes del plato" en Core Screens 7 + Components |
| No publicación | `git rev-list --left-right --count origin/main...HEAD`, `git ls-remote --heads`, `gh pr list --head` | `0 0`; rama `feature/dish-image-settings-toggle` **no** existe en `origin`; **ningún** PR para la rama; issue **#101** `OPEN` con el título correcto |

## Escenarios verificados (contra el código y los tests reales)

1. **Interruptor apagado ⇒ 0 peticiones (Scenario 1)**: `generatedIfEnabled` corta en seco; `DishImageSettingsTest.interruptor_apagado_no_pide_ninguna_imagen_ni_consume_tope` → productor **0** invocaciones, resultado `null`, `used == 0`; `apagar_el_interruptor_tras_producir...` cubre el paso a apagado en medio. Gate real en `MainActivity.kt` (`LocalGeneratedRecipeArtworkSource provides { recipe -> dishImageSettings.generatedIfEnabled { dishImageLoader.imageFor(recipe) } }`): apagado **no** lee caché ni genera.
2. **Encendido ⇒ produce (Scenario 2)**: `interruptor_encendido_permite_producir_una_vez` → 1 invocación; el decorador agotado devuelve `null` **sin** invocar al delegado (`el_decorador_agotado_no_invoca_al_delegado`, `calls == 2` con tope 2).
3. **Tope por sesión (Scenario 3)**: `DishImageGenerationBudget.tryAcquire()` con CAS (`AtomicInteger`), `DishImageGenerationBudgetTest`: `true,true,false` con max 2, `used/remaining/exhausted`, max 0 → ninguna reserva, `remaining` nunca negativo, **12 `async` concurrentes con max 3 → exactamente 3 éxitos** (determinista, sin reposos). Constante en un único sitio (ver tabla).
4. **Apagado no consume tope y no lo reinicia (Scenario 4)**: `el_interruptor_apagado_no_consume_tope_ni_reserva` (used 0, remaining 2) y `apagar_y_encender_no_reinicia_el_tope_agotado` (max 1 → `exhausted` sigue `true`, `used == 1`, la siguiente reserva sigue cortando).
5. **Estado observable (Scenario 5)**: `DishImageSettingsState(enabled, used, max)` con `remaining = (max-used).coerceAtLeast(0)` y `exhausted = used >= max`; inicial `(true, 0, 12)`; `una_reserva_re_emite...` → `(true,1,12)`; `el_estado_es_un_state_flow_observable` recoge las emisiones del `StateFlow`.
6. **Acierto de caché sin consumo (Scenario 6)**: con `DishImageResolver` + `FileDishImageCache`/`MemoryDishImageCache` reales sobre `TemporaryFolder` y generador falso, dos resoluciones de la misma clave → generador **1** vez y `used == 1`; clave nueva → `used == 2`; y acierto **de disco** con memoria fría → 0 invocaciones del delegado frío, `used == 1` (el decorador está por debajo del resolutor, en el borde de generación).
7. **Microcopy (Scenario 7)**: `DishImageSettingsCopyTest` (8 casos, puro y determinista): `remaining 12` → "Quedan 12 de 12 imágenes en esta sesión."; `1` → singular "Queda 1 de 12…"; `0` → "Tope de esta sesión alcanzado (12 de 12). Se reanudará al reiniciar la app."; apagado → "Desactivado: se usa solo la foto local." (manda sobre el tope); el texto cita el `max` inyectado, no un literal oculto.
8. **Fallo y cancelación (Scenario 8)**: delegado que lanza `IllegalStateException` → el resolutor degrada a `null` y `used == 1` (la reserva se paga); `CancellationException` → **se propaga** (no se convierte en `null`), `used == 1`; el decorador **no** captura excepciones.
9. **Alcance y pureza (Scenario 9)**: `RecipeArtwork` conserva la firma `(recipe, modifier, glyphSize)` y sus **5 puntos de uso** (`RecipeDetailScreen`, `FavoriteRecipeCard` ×2, `RecipeCard`, `ResultRecipeCard`) están **sin diff**; `ui/artwork/**` sin diff; `data/image` solo **gana** ficheros nuevos (los aceptados, sin diff) y los nuevos son puros; único fichero Compose nuevo = la sección de Perfil; sin dependencias nuevas; `docs/design/concepts/dish-image-settings-toggle/` no existe.

## Non-goals respetados

Sin pantalla/ruta/pestaña nuevas (sección al **final** del `LazyColumn` del Perfil, tras "Comensales habituales"); sin persistencia (nada de Room/DataStore/`UserProfile`); sin tocar `RecipeArtwork`, sus 5 usos, `ui/artwork/**`, `domain/**`, los ficheros aceptados de `data/image`/`data/ai/image`, `ui/theme/**`; sin re-resolución al conmutar; sin reset manual del tope; sin fases 5-7; sin dependencias ni cambios en build/lockfile/`init.sh`/`scripts/**`/`AndroidManifest.xml`/assets/Taxonomía/`seed-recipes.json`; sin telemetría ni `Log`.

## Decisiones del orquestador respetadas

- **Desviación de capturas autorizada (2026-10-13)**: `docs/design/concepts/dish-image-settings-toggle/` **no** existe y `DESIGN.md` registra la excepción con el formato de #77/#85/#86/#91/#98 (feature + **issue #101** + motivo + dirección visual aplicable + "no se crea carpeta") **más** la sección "Imágenes del plato" (Core Screens 7 y Components). El gate de capturas queda cubierto por ese registro.
- **`verification` de `feature_list.json` intacto**: el diff de la feature solo añade `status: passing`, `evidence` (11 entradas) y `notes`; las 3 líneas de `verification` no se tocan.
- **Sin commit/push/PR**: `HEAD` == `origin/main` (`c0cb069`), sin rama remota y sin PR; el commit y la publicación son del orquestador (`feature-flow`/`github-flow`).

## Documentación durable

`DESIGN.md` (excepción #101 + sección y microcopy), `docs/technical-discovery.md` (gate del interruptor + tope por sesión en la cadena en runtime + bullet de los 3 tests JVM nuevos), `docs/adr/0005-ai-dish-images.md` (fase 4 **completa**: integración + ajuste; interruptor/tope dejan de estar pendientes; sesión = proceso, sin persistencia; semántica conservadora) y `docs/risks-and-open-questions.md` (gasto de cuota con la mitigación implementada y pendiente solo el set semilla de fase 7) están actualizados y son coherentes entre sí. `docs/domain-model.md` y `AGENTS.md` correctamente **no** tocados; `profile-screen.md` no reabierta.

## Evaluación de seguridad

Sin hallazgos bloqueantes ni de riesgo alto; checklist de seguridad **sin hallazgos relevantes** para el slice. Sin secretos ni credenciales nuevas (la `DEVEXPERT_API_KEY` sigue viniendo de `BuildConfig` y no se toca), sin permisos ni superficie de red nuevos (el `aiClient` se **reutiliza**; la red sigue aislada en `data.ai.image`), sin `Log`/telemetría, sin datos personales y sin persistencia. El "tope por sesión" y el interruptor son **control de gasto de cuota del propio usuario en su dispositivo**, no un control de acceso (no hay multiusuario ni roles: ADR 0002), así que no requieren imposición en servidor; el guardarraíl es estrictamente local y en memoria. El contrato de fallo (`null` + cancelación propagada, degradación silenciosa) es coherente con el ADR 0005.

## Hallazgos por severidad

**Critical / High / Medium: ninguno.**

**Low (informativos, no bloquean la aceptación):**

1. `DishImageSettingsCopyTest.el_resumen_es_determinista` es un test **vacuo**: compara `summary(entry)` consigo mismo con la misma entrada, así que no puede fallar y no añade cobertura real (el determinismo ya lo cubren los casos de valor esperado del mismo suite). Inocuo.
2. `DishImageGenerationBudgetTest.cada_reserva_consume_una_unidad_distinta` afirma `assertNotEquals(used, remaining)`, que se cumple por la aritmética de `max = 12` con `used = 2`, más que por una propiedad semántica del contador; es un test débil (no vacuo). El comportamiento ya lo cubren `las_primeras_n_reservas...` y `remaining_nunca_es_negativo...`.
3. El acoplamiento entre el **gate del interruptor** y la UI (`CompositionLocalProvider` en `MainActivity`) se verifica por inspección/compilación; el test JVM ejercita `generatedIfEnabled` directamente (no hay test de Compose en `testImplementation`). Coherente con AGENTS.md (la UI de emulador la revisa la persona), pero conviene recordarlo en la prueba manual.
4. La spec (Scenario 6) pedía "memoria/disco **falsos**" y el test usa las implementaciones **reales** de caché sobre `TemporaryFolder` con generador falso: es una verificación más estricta que la pedida, no una desviación con impacto.
5. Riesgo residual ya registrado y aceptado por el implementer: una sesión sin red puede agotar el tope con fallos rápidos (un intento consume unidad); reembolsar fallos sería una feature correctiva.

## Verificación del handoff del implementer

La evidencia del implementer es **real y exacta**: re-ejecuté el gate y, al salir `UP-TO-DATE`, forcé test y lint; el parseo propio de los XML y del SARIF coincide con lo registrado (84/593/0/0/0; +3 suites/+31 tests sobre 81/562 de `HEAD`; 0 errores y 13 warnings preexistentes, 0 bajo `app/src`). La tirada focal `data.image` (7/57) y `ui.profile` (3/28), la pureza, el alcance, la rama sin publicar y el `hs_err_pid*.log` retirado (no queda ninguno en la raíz) también se confirman. Nada del resumen del implementer resultó inflado.

## Veredicto

**`accept`.** La feature cumple la spec (Scenarios 1-9 y Validator Checklist verdes con evidencia re-ejecutada), la desviación de capturas está autorizada y registrada, el alcance y la pureza se respetan, la documentación durable es coherente y el repositorio es reiniciable por el camino estándar (`bash init.sh` exit 0). Sin commit/push/PR. Estado recomendado: **`accepted`** (lo persiste el orquestador).
