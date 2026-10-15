# Validación independiente — `app-launcher-icon`

- **Feature**: `app-launcher-icon` (`area: ui`; icono de lanzador propio + splash de marca; el agente diseña el icono)
- **Spec**: `docs/specs/app-launcher-icon.md`
- **Rama / worktree**: `feature/app-launcher-icon`, `/mnt/c/Users/fernando.galiay/Documents/Default-Project-worktrees/app-launcher-icon`
- **Sesiones**: 131 (implementer, `passing`) y 132 (validador independiente, contexto limpio)
- **Estado previo**: `passing` (implementer; trabajo **sin commitear**; `HEAD = e758b43` == `origin/main`, **sin commit, sin push, sin PR**)
- **Veredicto**: **`revise`** (1 hallazgo **High**: la marca del splash se recorta con la máscara de plataforma; ver *Hallazgos*)
- **Veredicto final tras reparación (2026-10-15, sesión 134): `accept`** — ver «Re-validación independiente
  (tras reparación)» al final de este documento; la sección original se conserva como historial del
  `revise`.

## Qué se re-ejecutó (sin fiarse del handoff)

| Check | Comando | Resultado observado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (sigue encadenando `test` + `lint` por `scripts/gradle.sh`; no arranca procesos largos) |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | **exit 0**, `34/34 tareas ejecutadas` (`34 actionable tasks: 34 executed`); parseo propio de los `TEST-*.xml` frescos: **104 suites / 774 tests / 0 fallos / 0 errores / 0 saltados**; `LauncherIconTest` 6, `SplashThemeTest` 4, `DesignTokensTest` 8, `CardTokensTest` 5, `FavoriteTileContrastTest` 4, `MotionTokensTest` 3 |
| Lint forzado | `bash scripts/gradle.sh lint --rerun-tasks` | **exit 0**, `30/30 tareas ejecutadas`; SARIF `app/build/reports/lint-results-debug.sarif`: **13 resultados, 0 errores**, `GradleDependency` 7 / `NewerVersionAvailable` 3 / `AndroidGradlePluginVersion` 2 / `OldTargetApi` 1, **0 bajo `src/` del módulo** (ningún `IconMissingDensityFolder`, ningún `ObsoleteSdkInt`) ⇒ la contingencia de la spec (conservar rasters) **no** se disparó |
| Baseline re-derivada | `git grep -c '@Test'` en `HEAD` | 102 ficheros / 764 anotaciones ⇒ delta **+2 suites / +10 tests** (encaja con `LauncherIconTest` 6 + `SplashThemeTest` 4) |
| Escenarios 1-3 (inspección) | `find`/`grep`/`git status` | `find app/src/main/res \( -name '*.webp' -o -name '*.png' -o -name '*.jpg' \)` = **0**; `grep -rn '3DDC84\|33FFFFFF\|aapt:attr\|M65.3,45.828' app/src/main` = **0**; `grep -rn windowSplashScreen app/src/main` solo en `values-v31/` y `values-night-v31/`; `git status --porcelain` solo `.xml`/`.kt`/`.md` (los 10 `.webp` borrados aparecen como `D`) |
| Alcance | `git diff --name-only HEAD` + `??` | exactamente lo previsto + `app/lint.xml` (desviación, ver hallazgo Low-1); **sin diff** en `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `AndroidManifest.xml`, `values/themes.xml`, `values-night/themes.xml`, `strings.xml`, `ui/**`, `domain/**`, `data/**`, `assets/**`, `docs/design/**`, `docs/adr/**`, `androidTest/**`; `app/build.gradle.kts` solo +2 propiedades de sistema de test |
| No publicación | `git log origin/main..HEAD`, `git ls-remote --heads origin feature/app-launcher-icon` | **0 commits** por delante de `origin/main`, rama remota **inexistente** ⇒ el implementer **no** commiteó, no hizo push ni abrió PR |
| Harness | `git diff feature_list.json` (1 solo hunk) + parseo | `verification` **intacto**; solo `status` (`passing`) + 12 entradas de `evidence`; JSON válido; **0** features en `in_progress` |
| Docs durables | diff de `DESIGN.md`, `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`; `test -d docs/design/concepts/app-launcher-icon` | los tres actualizados y coherentes; carpeta de capturas **no** existe |

### Caso negativo comprobado de forma independiente (no pasa en vacío)

No me fié del resumen: copié el worktree a `/tmp/opencode/neg` (sin `.git`/`build`), **muté el trabajo en disco** y volví a ejecutar
`bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.theme.LauncherIconTest" --tests "com.sazon.app.ui.theme.SplashThemeTest"`:
**BUILD FAILED (exit 1)**, con un fallo atribuible a **cada** mutación (los tests leen los ficheros reales por
`android.manifest.path`/`android.res.dir`, no copias en memoria vacías):

| Mutación independiente | Detección observada |
|---|---|
| `DESIGN.md` `primary: "#123456"` | `la_paleta_del_icono_sale_de_design_md`: `sazon_launcher_motif: DESIGN.md(colors.primary)=#123456 repo=#D9552B` (+ derivas en cadena de los vectores que citan `#D9552B`) |
| `values-night/colors.xml` `sazon_launcher_background` → `#000000` | `sazon_launcher_background (values-night): DESIGN.md(colors.darkBackground)=#241C16 repo=#000000` y `… fillColor en values-night/colors.xml usa #000000, fuera de DESIGN.md` |
| Hex literal fuera de paleta en `ic_launcher_foreground.xml` (`#123456`) | `drawable/ic_launcher_foreground.xml: strokeColor=#123456 no es un color declarado en DESIGN.md` |
| `values-v31/themes.xml` sin `windowSplashScreenAnimatedIcon` | `las_variantes_v31_repiten_el_tema_base_y_anaden_el_splash`: `values-v31: windowSplashScreenAnimatedIcon=null` |
| `monochrome` → `@drawable/ic_launcher_foreground` | `el_icono_adaptativo_declara_las_tres_capas…`: `capas monochrome … expected:<…_monochrome> but was:<…_foreground>` |
| `res/drawable/stray_icon.png` (binario nuevo) | `no_queda_rastro_del_icono_de_plantilla`: `Quedan assets binarios de icono: [drawable/stray_icon.png]` |

Además, la guarda `assertNotEquals` del propio test (`una_paleta_mutada_es_detectada`) saltó al no poder aplicar su reemplazo (la
paleta ya estaba mutada): la comprobación falla de forma ruidosa en vez de pasar en vacío. **Escenario 6 confirmado como no vacío.**

## Escenarios de la spec

1. **Icono adaptativo de tres capas (Scenario 1)** — `mipmap-anydpi/ic_launcher.xml` y `ic_launcher_round.xml` son `<adaptive-icon>` con
   exactamente una capa `background`/`foreground`/`monochrome` → `ic_launcher_background` / `ic_launcher_foreground` /
   `ic_launcher_monochrome`, los tres ficheros existen con `<vector>` + `<path>`, y el manifiesto sigue declarando
   `@mipmap/ic_launcher` / `@mipmap/ic_launcher_round`. **Cumple.**
2. **Paleta desde `DESIGN.md` (Scenario 2)** — `sazon_launcher_background` = `background` `#FFF8F0` (claro) y `darkBackground` `#241C16`
   (oscuro, solo ese token redefine `values-night`), `sazon_launcher_motif` = `primary` `#D9552B`, `sazon_launcher_accent` = `accent`
   `#F2A93B`; el `foreground` usa **motivo y acento**; los cuatro vectores no tienen ningún literal fuera de la paleta salvo `#000000`
   en `ic_launcher_monochrome.xml` (excepción documentada y acotada: el test solo la tolera en ese fichero y la prohíbe en el resto).
   **Cumple.**
3. **Sin rastro del icono de plantilla (Scenario 3)** — 0 binarios en `res/`, 0 coincidencias de `#3DDC84`/`33FFFFFF`/`aapt:attr`/
   `M65.3,45.828`/`strokeWidth="0.8"` (inspección del repo), y ningún binario nuevo en `git status`. **Cumple.**
4. **Splash de Android 12+ (Scenario 4)** — `values-v31/` **y** `values-night-v31/` redeclaran `Theme.Sazon` con el mismo `parent` que su
   variante base, `windowSplashScreenBackground="@color/sazon_window_background"` (`#FFF8F0`/`#241C16`) y
   `windowSplashScreenAnimatedIcon="@drawable/ic_splash_mark"`, sin `windowSplashScreenIconBackgroundColor`. Las **declaraciones** cumplen;
   **la marca renderizada no** (ver hallazgo High: el dibujo cae fuera del círculo de 2/3 que el sistema hace visible).
5. **Tema base sin regresión (Scenario 5)** — `values/themes.xml` y `values-night/themes.xml` **sin diff** (siguen con `windowBackground` y
   `forceDarkAllowed=false`, sin atributos de splash) y las dos variantes v31 repiten ambos items. **Cumple.**
6. **Deriva detectada (Scenario 6)** — caso negativo real y no vacío, verificado además con mi propia copia mutada en `/tmp`. **Cumple.**
7. **`DESIGN.md` (Scenario 7)** — sección `## App Icon` nueva (colocada entre `## Components` y `## Core Screens`), pregunta abierta 2
   marcada como **resuelta (2026-10-15, `app-launcher-icon`)** sin renombrar producto/paquete/`applicationId`, y párrafo de excepción de
   capturas en `Generated Concept Images` (misma clase que #77/#85/#86/#91); `docs/design/concepts/app-launcher-icon/` **no** se crea;
   `docs/technical-discovery.md` (fila del recurso + bullet de verificación + nota de `app/lint.xml`) y
   `docs/risks-and-open-questions.md` (riesgo de caché/`-night` del launcher) actualizados. **Cumple**, con la salvedad del hallazgo High
   (el rational de "mayor escala relativa" es incorrecto y hay que corregirlo).
8. **Nada más cambia (Scenario 8)** — `init.sh` verde, sin dependencias nuevas (`gradle/libs.versions.toml` intacto), 0 warnings nuevos bajo
   `app/src` y alcance acotado (salvo `app/lint.xml`, ver Low-1). **Cumple.**

### Non-goals respetados

Sin renombrar producto/paquete/`applicationId`; sin assets binarios nuevos; sin dependencias (`androidx.core.splashscreen` y
`material-icons` ausentes); sin tocar pantallas, tipografía, paleta/tokens de tema, `recipe-card-*`, estados vacíos ni navegación; sin
splash animado ni `windowSplashScreenAnimationDuration`; sin capturas de Stitch ni carpeta `docs/design/concepts/app-launcher-icon/`; sin
renombrar `mipmap-anydpi/` a `-v26` (se conserva, coherente con el hallazgo de la spec de que la carpeta real es `mipmap-anydpi/`).

## Hallazgos por severidad

### Finding 1 (High): la marca del splash se recorta con la máscara de plataforma (el cuenco pierde su cuerpo)

- **Severidad**: High (defecto visible del comportamiento prometido: "la pantalla de arranque muestra la marca").
- **Evidencia**:
  - `app/src/main/res/drawable/ic_splash_mark.xml`: geometría con extensión radial máxima **44 unidades** de viewport respecto al centro
    `(54,54)` de un canvas de 108 (`borde M20,60h68` con `strokeWidth=8` → hasta `y=98`; caps redondeados hasta `x=16`/`x=92` → radio 38.5).
  - Plataforma (API 31+, que es el caso con `minSdk 33`): `frameworks/base` →
    `SplashscreenIconDrawableFactory.makeIconDrawable()` envuelve un drawable **no adaptativo y no animable** (nuestro `<vector>` plano) en
    `AdaptiveForegroundDrawable`; `AdaptiveForegroundDrawable.updateLayerBounds()` dibuja el hijo en un rectángulo de **1.5× los bounds**
    (`DEFAULT_VIEW_PORT_SCALE = 1/(1+2·1/4) = 2/3` ⇒ `inset = w/(2/3·2) = 0.75w`), mientras `ImmobileIconDrawable` pre-dibuja en un
    **bitmap del tamaño de los bounds**. Es decir: **solo el 2/3 central del canvas del vector es visible** (radio 36 en los ejes;
    ≈40 en las diagonales con la máscara estándar), independientemente de `mNoBackgroundScale`/`mIconSize` (`SplashscreenContentDrawer`).
    La misma regla en `androidx.core:core-splashscreen`: `MASK_FACTOR = 2/3f`, `maskSize = iconSize * MASK_FACTOR`,
    `splashscreen_icon_size_no_background = 288dp` y `MaskedDrawable` (`drawable.bounds = bounds` + clip circular), y la documentación
    pública: "App icon without an icon background … 288×288 dp and fit within a circle 192 dp in diameter … everything outside the circle
    turns invisible (masked)".
  - Cálculo (viewport 108, centro `(54,54)`, límite 36): la marca actual queda **recortada**; fracción visible de la línea central del
    **cuerpo del cuenco** (`M22,62a32,32 0 0,0 64,0`): **0.27** con máscara circular y **0.47** con la máscara octogonal por defecto de AOSP
    (radio 44 en el fondo del cuenco vs 36 de límite); los extremos del borde se recortan ~2.5 unidades y el vaho queda dentro.
    En cambio el **icono de lanzador** sí respeta la zona segura (radio máx. 29.7 ≤ 33), así que la desproporción es claramente no intencionada.
  - `DESIGN.md` (`## App Icon`) justifica la escala con un rationale incorrecto: "el mismo cuenco a **mayor escala relativa** porque el sistema
    lo dibuja enmascarado en un círculo"; la spec fijó esas coordenadas creyendo lo mismo (y su Unknown de "landing visual del splash"
    permite explícitamente **ajustar la escala relativa del vector**, mismo contrato de test).
- **Por qué importa**: el primer frame de la app mostraría un cuenco con el vientre cortado por la máscara (la marca pierde ~la mitad de su
  cuerpo). Es determinable ahora con evidencia de plataforma y con un cambio mínimo; la alternativa es que lo vea la persona tras el merge y
  se convierta en una **feature correctiva** (AGENTS.md), con más coste que arreglarlo aquí. El contrato JVM actual no lo puede detectar
  (no comprueba geometría), así que las 774 pruebas en verde no cubren este aspecto.
- **Cambio requerido**: que toda la geometría de `ic_splash_mark.xml` (trazo incluido) quepa en el **2/3 central del canvas** (radio ≤ 36
  respecto a `(54,54)`), manteniendo el motivo, los roles de color y el contrato de test actuales; y corregir el rationale en `DESIGN.md`.
- **Implementación sugerida**:
  1. En `app/src/main/res/drawable/ic_splash_mark.xml`, envolver los cuatro `<path>` en
     `<group android:pivotX="54" android:pivotY="54" android:scaleX="0.78" android:scaleY="0.78">` (44 × 0.78 ≈ 34.3 ≤ 36; caps 38.5 × 0.78 ≈ 30)
     **o** reescalar las coordenadas a mano. La marca seguirá siendo **mayor** que la del lanzador (29.7 → ~34), que es la intención de la spec.
  2. En `DESIGN.md` (`## App Icon`), sustituir el rationale por el correcto: la marca debe caber en el **círculo interior de 2/3** que el
     sistema hace visible (la misma regla que la zona segura del icono adaptativo) y por eso **no** puede ir "a mayor escala" sin recortarse.
     Revisar si `docs/technical-discovery.md` repite el rationale.
  3. **Recomendado** (cierra el hueco de verificación): en `SplashThemeTest.kt`, añadir una comprobación de geometría de `ic_splash_mark`
     (todos los puntos/control de `pathData` y el semiancho de trazo dentro de radio 36 respecto a `(54,54)`) con su caso negativo
     no vacío (la geometría actual, o una copia mutada, debe devolver ≥ 1 problema).
- **Verificación tras el arreglo**:
  - `bash init.sh` → exit 0; `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` → 0 fallos y, si se añade, el nuevo test de geometría en verde.
  - `bash scripts/gradle.sh lint --rerun-tasks` → 0 errores, 13 resultados, 0 bajo `src/`.
  - Manual (persona, emulador, tras el merge): el splash muestra el cuenco **completo** sobre el fondo cálido, en claro y oscuro.

### Finding 2 (Low, informativo — desviación aceptada): `app/lint.xml`

- **Severidad**: Low. **No requiere reparación**: la juzgo **aceptable**.
- **Evidencia**: `app/lint.xml` (fichero nuevo, fuera de `Expected File Changes`) con `ignore` de `ObsoleteSdkInt` **acotado por ruta** a
  `src/main/res/values-v31` y `src/main/res/values-night-v31`; con él, `bash scripts/gradle.sh lint --rerun-tasks` da **13 resultados y 0 bajo
  `src/`** (sin él el `tools:ignore` en `<style>`/`<resources>` no suprime el aviso, que se ancla a la **carpeta**); el rationale está
  documentado en `DESIGN.md`-adyacentes: `docs/technical-discovery.md`, `PROGRESS.md` (sesión 131) y la evidencia de `feature_list.json`.
  Las carpetas se conservan a propósito (spec: `values-v31`/`values-night-v31` son la puerta de plataforma del splash; los qualifiers
  `night` antes que `version` exigen la variante nocturna).
- **Por qué importa (riesgo residual, acotado)**: un `values-v31/` futuro dentro de esas **dos** rutas exactas quedaría silenciado; no se
  desactiva la regla en el resto del repositorio ni se introduce un baseline de lint. Es el mecanismo mínimo soportado para un aviso de carpeta.
- **Recomendación (no bloquea)**: que la spec o el cierre de la feature recojan la desviación (la spec dice explícitamente que `init.sh` y el
  número de warnings no cambian; `app/lint.xml` es el único añadido fuera de `Expected File Changes`).

### Finding 3 (Low, informativo): endurecimiento del parser XML de test

- **Severidad**: Low (no explotable en esta feature). `AppResourceFiles.parseText()` usa `DocumentBuilderFactory` sin desactivar DTD/entidades
  externas; las entradas son recursos **versionados del propio repositorio** (sin entrada de usuario ni red), así que no hay superficie de
  ataque. **No requiere reparación**; si se toca el parser en el futuro, `setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)`
  lo deja a prueba de XXE.

### Finding 4 (Low, informativo): texto de `feature_list.json` desalineado con la spec

- **Severidad**: Low. El campo `user_visible_behavior` (no modificado por el implementer) describe el motivo como "terracota/oliva" y la spec/
  implementación usan `primary` terracota + `accent` **ámbar**. La spec manda (`accent` `#F2A93B`), y el test lo verifica. **No requiere
  reparación**; conviene alinearlo cuando el orquestador cierre la feature.

## Evaluación de seguridad

Revisión acotada al diff (checklist portable). **Sin hallazgos bloqueantes ni de riesgo alto.** Sin secretos ni credenciales nuevas (ni en
código, tests, docs ni fixtures); sin permisos, cuentas, roles ni superficie de red nuevos (el diff es XML de recursos, temas y tests JVM);
sin entrada de usuario nueva (los datos los leen los tests por rutas absolutas fijadas en `app/build.gradle.kts`, sin interpolar nada);
sin dependencias nuevas (`gradle/libs.versions.toml` intacto, sin `androidx.core.splashscreen` ni `material-icons`); sin logs ni datos
personales; `app/lint.xml` solo afecta a la regla `ObsoleteSdkInt` en dos rutas (no debilita ninguna comprobación de seguridad). Notas:
Finding 2 (riesgo residual acotado de lint) y Finding 3 (endurecimiento opcional del parser de test, no explotable).

## Documentación durable y harness

`DESIGN.md` (`## App Icon` bien colocado, OQ2 resuelta, excepción de capturas), `docs/technical-discovery.md` (fila del recurso, bullet de
verificación JVM, nota de `app/lint.xml`) y `docs/risks-and-open-questions.md` (riesgo de caché/`-night` del launcher) están actualizados y
coherentes con el código, salvo el rationale de escala del splash (Finding 1, paso 2). `docs/domain-model.md`, `AGENTS.md`, `CONTEXT.md`,
`docs/build-brief.md`, `docs/adr/**` y `docs/design/concepts/` correctamente **no** tocados. Harness: `init.sh` intacto y sigue siendo el gate
no bloqueante (exit 0, sin procesos largos); estado de `feature_list.json` válido (0 `in_progress`) y `PROGRESS.md` con la sesión 131 completa
(objetivo, números observados, desviación de `app/lint.xml`, incógnitas y siguiente paso). Repositorio reiniciable por el camino estándar.

## Verificación del handoff del implementer

La evidencia registrada es **real y exacta**: reproduje el gate y, al salir `UP-TO-DATE`, forcé test y lint; mis números (104/774/0/0/0;
30/30 tareas de lint; SARIF con 13 resultados y 0 bajo `src/`; 102 ficheros/764 anotaciones en `HEAD` ⇒ +2 suites/+10 tests; 0 binarios en
`res/`; 0 restos de plantilla; `git status` acotado) coinciden con los suyos. También confirma el punto delicado que declaró (el `tools:ignore`
no suprime el aviso de carpeta y `app/lint.xml` lo acota por ruta) y el estado de no publicación (0 commits por delante, sin rama remota, sin
PR). Lo que **no** cubre su evidencia es el encaje del dibujo del splash en la máscara de 2/3 (Finding 1): sus tests no comprueban geometría y
el defecto no se ve en JVM.

## Veredicto

**`revise`.** La feature cumple literalmente los 8 escenarios y el Validator Checklist (paleta por token, tres capas, sin rastro de plantilla,
splash declarado en `values-v31`/`values-night-v31`, temas base intactos, caso negativo no vacío, docs y alcance correctos, `bash init.sh`
exit 0 con 0 warnings nuevos bajo `app/src`), y el caso negativo se confirma **no vacío** con una copia mutada independiente. La desviación de
`app/lint.xml` es **aceptable** (Low, acotada y documentada). Sin embargo, hay **un hallazgo High**: la geometría de `ic_splash_mark` sale del
círculo interior de 2/3 que la plataforma hace visible, así que el primer frame mostraría un cuenco recortado; se corrige con un cambio mínimo
(escala ≤ 0.78 dentro del canvas, o reescalado equivalente) más el rationale en `DESIGN.md`, y el arreglo está dentro de lo que la propia spec
autoriza para el "landing visual del splash".

**Estado recomendado**: **no** persistir `accepted` (el validador no marca aceptación). Devolver la feature al implementer para el arreglo del
Finding 1 (`in_progress` mientras se corrige; de vuelta a `passing` con la evidencia actualizada, incluido el conteo de tests observado) y
volver a validarla. Sin commit/push/PR por parte del implementer; el commit local y la publicación siguen siendo del orquestador.

---

# Re-validación independiente (tras reparación) — 2026-10-15

- Feature: `app-launcher-icon` · Spec: `docs/specs/app-launcher-icon.md`
- Rol: `validator` (contexto limpio, skill `feature-validator`), **independiente** del implementer
- Estado validado: `passing` (sesión 133, reparación del Finding 1) en la rama `feature/app-launcher-icon`,
  trabajo **sin commitear** (`HEAD = e758b43` == `origin/main`)
- **Veredicto: `accept`**

## Qué se reparó (leído en los artefactos del repo, no del resumen)

- `app/src/main/res/drawable/ic_splash_mark.xml`: los cuatro `<path>` van dentro de un único
  `<group android:pivotX="54" android:pivotY="54" android:scaleX="0.78" android:scaleY="0.78">`; el
  comentario del vector documenta la regla del 2/3 y por qué **no** puede ir a mayor escala. Motivo,
  geometría de los trazos y roles de color (`sazon_launcher_motif` / `sazon_launcher_accent`)
  **idénticos** a la primera implementación: lo único que cambia es la escala del grupo.
- `app/src/test/java/com/sazon/app/ui/theme/AppResourceFiles.kt`: nueva `splashMarkGeometryProblems(xml)`
  (`SPLASH_VISIBLE_FRACTION = 2.0/3.0`): resuelve `M/m`, `h/H`, `v/V`, `l/L`, arcos `a/A` (muestreados con
  parámetro de centro), `c/C`, `q/Q`, `s/S`, `t/T`, aplica los `<group>` (escala/pivote/traslación
  compuestas; **rotación** se declara como problema en vez de medir mal), y exige que cada punto + el
  semiancho de trazo queden dentro del círculo visible (radio = 1/3 del lado menor del viewport → **36** de
  108). Guardas anti-vacío propias: viewport ausente o **cero** `<path>` medibles devuelven problema.
- `SplashThemeTest.kt`: de 4 a **6** tests (`la_marca_del_splash_cabe_en_el_circulo_visible_de_la_plataforma`
  sobre el fichero real + `una_marca_de_splash_desbordada_es_detectada`, con dos XML literales: geometría
  previa desbordada y punto a radio 34 con trazo grueso, más un **control** de trazo fino que no debe dar
  problema).
- `DESIGN.md` (`## App Icon`) y `docs/technical-discovery.md` (fila del recurso + bullet de verificación):
  el rationale de "mayor escala relativa" desaparece y queda la regla correcta (2/3 visible, escala 0.78,
  radio 44 → ~34.3) como **regla durable** para cualquier marca futura del splash.
- `feature_list.json`: `user_visible_behavior` pasa de "terracota/oliva" a "terracota/**ambar**" (Finding 4).
  `PROGRESS.md`: sesión 133 completa (qué se reparó, números observados, negativos).
- **No** se tocaron en la reparación: los otros vectores, `values*/colors.xml`, `values/themes.xml`,
  `values-night/themes.xml`, `values-v31/`/`values-night-v31/`, `app/lint.xml`, el manifiesto, los ficheros
  de build, `scripts/**`, `init.sh` ni la spec.

## Regla de plataforma verificada de nuevo (es el fundamento del hallazgo y del arreglo)

No me fié de la ronda anterior: la volví a contrastar contra fuentes públicas de plataforma.

- Documentación oficial del splash (*Splash screens*, developer.android.com): *"App icon without an icon
  background: this must be 288×288 dp and fit within a circle 192 dp in diameter … Everything outside the
  circle turns invisible (masked)"* y *"As with adaptive icons, one-third of the foreground is masked"* →
  192/288 = **2/3** ⇒ radio visible **36** de los 108 del viewport (el helper usa exactamente ese valor).
- `androidx.core:core-splashscreen` (`SplashScreen.kt`): *"the icon needs to fit within a circle whose
  diameter is 2/3 the size of the icon"* + `MASK_FACTOR = 2/3f`, con los specs 288 dp → círculo de 192 dp.
- `AdaptiveIconDrawable` (AOSP): capas de 108 dp, *"the inner 72 x 72 dp of the icon appears within the
  masked viewport"* y `SAFEZONE_SCALE = 66f/72f` ⇒ la zona segura del **icono de lanzador** es el círculo de
  radio 33, más conservadora que el círculo de 36 del splash. Es la referencia con la que se compara la
  marca (la del splash sigue siendo mayor, como dice `DESIGN.md`).

## Modelo de geometría propio (script independiente en `/tmp/opencode/val2/geom.py`, sin el helper)

Parser propio de `pathData` (comandos absolutos/relativos, arcos muestreados a 360 pasos incluyendo los
extremos, puntos de control), transformación de `<group>` compuesta como `p → scale·p + offset` con el
pivote plegado, y radio máximo respecto a `(54,54)` = `max(|p−c|) + semiancho`.

| Fichero / path | radio geométrico | + semiancho (conservador) | + semiancho si el trazo escala | ¿≤ 36? |
|---|---|---|---|---|
| `ic_splash_mark` `M20,60h68` (borde) | 26.93 | **30.93** | 30.05 | sí |
| `ic_splash_mark` `M22,62a32,32 0 0,0 64,0` (cuerpo) | 31.20 | **35.20** | **34.32** | sí |
| `ic_splash_mark` `M46,48c…` (vaho) | 21.90 | 25.15 | 24.43 | sí |
| `ic_splash_mark` `M62,48c…` (vaho) | 24.42 | 27.67 | 26.95 | sí |
| `ic_splash_mark` **sin** el `<group>` (geometría previa) | 34.53 / **40.00** | **38.53** / **44.00** | 38.53 / 44.00 | **no** |
| `ic_launcher_foreground` / `ic_launcher_monochrome` | máx. 26.68 | máx. **29.18** | 29.18 | sí (≤ 33) |

Conclusiones: el arreglo deja **toda** la geometría (trazo incluido) dentro del círculo visible en los
**dos** modelos de trazo; los números del implementer (35.2 conservador / 34.32 escalado; previos 38.53 y
44.0) **se reproducen exactamente** con un modelo independiente; y la marca del splash (34.32–35.20) sigue
siendo **mayor** que la del icono de lanzador (29.18), como afirma `DESIGN.md`.

## Casos negativos propios: mutación **real** del recurso (no pasa en vacío)

| Mutación independiente del validador | Resultado observado |
|---|---|
| **A (la del implementer, reproducida)**: `scaleX`/`scaleY` `0.78` → `1.0` en `ic_splash_mark.xml` | `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.theme.SplashThemeTest" --rerun-tasks` → **BUILD FAILED (exit 1)**, `6 tests completed, 1 failed` (`SplashThemeTest.kt:82`), mensaje: `group[0]/path[0]: el trazo llega a radio 38.53 > 36.0 … group[0]/path[1]: el trazo llega a radio 44.0 > 36.0` |
| **B (mía, distinta: desbordamiento por traslación, no por escala)**: `android:translateY="8"` dentro del `<group>` | **BUILD FAILED (exit 1)**, `1 failed`, mensaje: `group[0]/path[1]: el trazo llega a radio 43.2 > 36.0` (**exactamente** el valor que predijo mi modelo: 31.20 + 8 + 4) |
| Restauración tras cada mutación | `md5sum` idéntico antes/después (`7d69444eac0d859538c0a64b225ae4d7`) y `diff` vacío; tirada focal final `--tests "com.sazon.app.ui.theme.*"` → **exit 0**, 32 tests / 0 fallos |

⇒ El negativo **no es vacío** (falla con geometría desbordada y pasa con la reparada), la comprobación
aplica bien la transformación del grupo (mi modelo y el helper coinciden al decimal) y el recurso quedó
restaurado byte a byte.

## Gate y verificaciones re-ejecutadas por el validador

| Comprobación | Comando | Resultado observado |
|---|---|---|
| Gate estándar | `bash init.sh` | **exit 0** (test + lint vía `scripts/gradle.sh`); 1.ª pasada **UP-TO-DATE** (6 s) ⇒ forzado |
| Tests forzados | `bash scripts/gradle.sh testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL** en 5m08s, `34/34 tareas ejecutadas`, **exit 0** |
| XML (parseo propio de `TEST-*.xml` frescos) | `app/build/test-results/testDebugUnitTest/` | **104 suites / 776 tests / 0 fallos / 0 errores / 0 saltados**; `LauncherIconTest` **6**, `SplashThemeTest` **6**, `DesignTokensTest` 8, `CardTokensTest` 5, `FavoriteTileContrastTest` 4, `MotionTokensTest` 3 |
| Lint forzado | `bash scripts/gradle.sh lint --rerun-tasks` | **BUILD SUCCESSFUL** en 3m35s, `30/30 tareas ejecutadas`, **exit 0** |
| SARIF (parseo propio) | `app/build/reports/lint-results-debug.sarif` | **13 resultados / 0 errores** (`GradleDependency` 7, `NewerVersionAvailable` 3, `AndroidGradlePluginVersion` 2, `OldTargetApi` 1) y **0 bajo `app/src/`** ⇒ sin `IconMissingDensityFolder` (la contingencia de la spec no se disparó) y sin `ObsoleteSdkInt` |
| Baseline re-derivada | `git grep -c '@Test' HEAD -- '*.kt'` | **103 ficheros / 765 anotaciones**, de las que 1 está en `app/src/androidTest/.../ExampleInstrumentedTest.kt` (**no** entra en la tirada JVM) ⇒ baseline de tests unitarios **102 ficheros / 764 anotaciones** ⇒ delta **+2 suites / +12 tests** (6 + 6), coherente con 104/776 |
| Escenarios 1-3 (inspección) | `find`/`grep`/`git status` | `find app/src/main/res \( -name '*.webp' -o -name '*.png' -o -name '*.jpg' -o -name '*.jpeg' \)` = **0**; `grep -rn '3DDC84\|33FFFFFF\|aapt:attr\|M65.3,45.828\|strokeWidth="0.8"' app/src/main` = **0**; `windowSplashScreen` solo en `values-v31/` y `values-night-v31/`; `git status --porcelain` = 32 entradas, todas `.xml`/`.kt`/`.md`/`app/build.gradle.kts` + las 10 bajas `.webp` (ningún binario nuevo) |
| Alcance | `git status --porcelain`, `git diff --stat HEAD` | lo previsto + `app/lint.xml` (desviación Low-2, aceptada en la ronda 1); **sin diff** en `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `AndroidManifest.xml`, `values/themes.xml`, `values-night/themes.xml`, `strings.xml`, `ui/**`, `domain/**`, `data/**`, `assets/**`, `docs/design/**`, `docs/adr/**`, `androidTest/**`; `app/build.gradle.kts` solo +2 propiedades de sistema de test (sin duplicados) |
| No publicación | `git log --oneline origin/main..HEAD`, `git ls-remote --heads origin feature/app-launcher-icon` | **0 commits** por delante de `origin/main`, rama remota **inexistente** ⇒ sin commit, sin push y sin PR |
| Harness | `feature_list.json` (JSON válido) | `passing`, `verification` intacto, **17** entradas de `evidence` (las de la sesión 133 incluidas y exactas), **0** features en `in_progress`; `user_visible_behavior` con "terracota/ambar" |
| Docs durables | diff de `DESIGN.md`, `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`; `test -d docs/design/concepts/app-launcher-icon` | los tres actualizados y ya **coherentes** (sin el rationale incorrecto); carpeta de capturas **no** existe |

## Escenarios de la spec y Validator Checklist (re-evaluados)

1. **Scenario 1** (tres capas + vectores propios + manifiesto): cumple (`<adaptive-icon>` con una sola capa
   `background`/`foreground`/`monochrome` → `ic_launcher_background`/`ic_launcher_foreground`/
   `ic_launcher_monochrome`; manifiesto con `icon`+`roundIcon`).
2. **Scenario 2** (paleta desde `DESIGN.md`): cumple; `values-night` redefine **solo**
   `sazon_launcher_background`; única excepción `#000000` en la capa monocroma, tolerada **solo** ahí por el
   test.
3. **Scenario 3** (sin rastro de plantilla ni binarios): cumple (0 binarios, 0 marcadores).
4. **Scenario 4** (splash v31 claro/oscuro, sin placa): cumple **y ahora también en el dibujo**: la marca
   cabe en el círculo visible (Finding 1 resuelto).
5. **Scenario 5** (tema base sin regresión): cumple (ambos temas base sin diff y las dos variantes v31
   repiten `windowBackground` + `forceDarkAllowed=false` con el mismo `parent`).
6. **Scenario 6** (negativo no vacío): cumple, con dos mutaciones reales propias (tabla anterior).
7. **Scenario 7** (`DESIGN.md` registra dirección y cierra OQ2): cumple, con el rationale **corregido**.
8. **Scenario 8** (nada más cambia): cumple (gate verde, 0 warnings nuevos bajo `app/src`, sin dependencias
   nuevas, `init.sh` intacto).
- **Validator Checklist**: los 9 puntos, cumplidos (el de geometría no estaba en la spec original y lo
  añadió la reparación a petición de la ronda 1; su caso negativo es real).
- **Non-goals**: sin renombrar producto/paquete/`applicationId`; sin assets binarios; sin dependencias; sin
  tocar pantallas/tipografía/paleta de tema/`recipe-card-*`/navegación; sin splash animado; sin capturas ni
  carpeta de conceptos; `mipmap-anydpi/` conservado.

## Hallazgos por severidad

- **Finding 1 (High) — RESUELTO y verificado.** La geometría de `ic_splash_mark` (trazo incluido) queda en
  30.93 / **35.20** (34.32 si el trazo escala) / 25.15 / 27.67 ≤ 36, con modelo propio, regla de plataforma
  recontrastada y dos negativos reales (38.53 y 44.0 al quitar el grupo; 43.2 con mi traslación). Queda una
  **cota de regresión** JVM que impide volver a desbordarlo.
- **Finding 2 (Low) — se mantiene, aceptable sin reparar.** `app/lint.xml` con `ignore` de `ObsoleteSdkInt`
  **acotado por ruta** a `src/main/res/values-v31` y `src/main/res/values-night-v31`; con él el lint vuelve a
  **13 resultados / 0 bajo `app/src`**. Riesgo residual acotado (un `values-v31` futuro dentro de esas dos
  rutas exactas quedaría silenciado) y documentado en `DESIGN.md`-adyacentes, `docs/technical-discovery.md`,
  `PROGRESS.md` y la evidencia.
- **Finding 3 (Low) — se mantiene, aceptable sin reparar.** `AppResourceFiles.parseText()` sigue sin
  desactivar DTD/entidades externas. No es explotable aquí: solo parsea recursos **versionados del propio
  repositorio** en tests JVM, sin entrada de usuario ni red. Si se toca el parser, añadir
  `disallow-doctype-decl`.
- **Finding 4 (Low) — RESUELTO.** `user_visible_behavior` de `feature_list.json` dice ya
  "terracota/**ambar**", alineado con `primary` + `accent` y con el test.
- **Info-1 (informativo, sin reparación)**: el baseline citado "102 ficheros / 764 anotaciones" es el de la
  tirada JVM (excluye el test instrumentado); el comando `git grep -c '@Test'` **tal cual** da 103/765. El
  delta declarado (**+2 suites / +12 tests**) es correcto en ambos casos y encaja con 104/776.
- **Info-2 (informativo, sin reparación)**: `DESIGN.md` dice que el motivo del lanzador llega a "~29.7";
  con la convención del helper (punto de control/extremo + semiancho) el máximo es **29.18**. Es una cifra
  aproximada y la afirmación que sostiene (≤ 33 de zona segura, y menor que la del splash) es cierta en
  ambos cálculos.
- **Info-3 (informativo, sin reparación)**: el margen de la marca del splash es estrecho (34.32–35.20 frente
  a 36.0, ≤ 4.7 %). No es un defecto —la regla es la documentada y el círculo es el de plataforma—, pero
  cualquier re-escalado futuro debe mantener la cota; el test de geometría es la red de seguridad.

## Evaluación de seguridad

Revisión acotada al diff (checklist portable). **Sin hallazgos bloqueantes ni de riesgo alto.** La
reparación solo cambia **geometría de un vector de recurso** y **código de test JVM**: sin secretos ni
credenciales, sin permisos/cuentas/roles nuevos, sin superficie de red, sin entrada de usuario (los tests
leen rutas absolutas fijadas en `app/build.gradle.kts`), sin logs ni datos personales, sin dependencias
nuevas (`gradle/libs.versions.toml` intacto). El parser XML del test (Finding 3) no es explotable; el
`ignore` de lint (Finding 2) solo afecta a `ObsoleteSdkInt` en dos rutas y no debilita ninguna comprobación
de seguridad.

## Documentación durable y harness

`DESIGN.md` (`## App Icon` con la regla del 2/3 y la escala 0.78; OQ2 resuelta; excepción de capturas),
`docs/technical-discovery.md` (fila del recurso con la regla durable + bullet de los tests JVM + nota de
`app/lint.xml`) y `docs/risks-and-open-questions.md` (cacheo/re-resolución `-night` por el launcher) están
actualizados y **sin el rationale incorrecto**. `docs/domain-model.md`, `AGENTS.md`, `CONTEXT.md`,
`docs/build-brief.md`, `docs/adr/**` y `docs/design/concepts/` correctamente **no** tocados. Harness:
`init.sh` intacto y sigue siendo el gate no bloqueante (exit 0, sin procesos largos); `feature_list.json`
válido (`passing`, 0 `in_progress`) y `PROGRESS.md` con las sesiones 131/132/133 y el siguiente paso.
Repositorio reiniciable por el camino estándar. Trabajo en disco **sin commit**, sin push y sin PR, como
exige el flujo.

## Verificación del handoff del implementer (las 5 afirmaciones)

1. **`<group>` 0.78 con máximo ≤ 36 (35.2 / 34.32)** — **confirmada** en el fichero y con mi propio modelo.
2. **Rationale de `DESIGN.md` corregido + regla durable en `docs/technical-discovery.md`** — **confirmada**
   (la frase "a mayor escala relativa" solo aparece ya en su forma negada correcta; la regla del 2/3 y la
   escala 0.78 están en ambos documentos).
3. **Chequeo JVM de geometría + `SplashThemeTest` con 6 tests y negativo no vacío** — **confirmada**:
   `splashMarkGeometryProblems` existe con guardas anti-vacío; `SplashThemeTest` = 6 tests; el negativo
   literal **y** la mutación real del recurso fallan de verdad (con mis propios números).
4. **Finding 4 aplicado ("terracota/ambar")** — **confirmada**.
5. **Gate: `init.sh` exit 0; 104 suites / 776 tests / 0/0/0; lint 13/0/0 bajo `src/`** — **confirmada**
   re-ejecutando todo con `--rerun-tasks` y parseando yo los XML/SAFIR frescos.

## Veredicto

**`accept`.** El Finding 1 (High) está **reparado y verificado de forma independiente**: la marca del
splash cabe ahora, trazo incluido, en el círculo de 2/3 que la plataforma hace visible (radio 36 de 108),
la regla está recontrastada contra la documentación oficial y `androidx`, el rationale de
`DESIGN.md`/`docs/technical-discovery.md` es correcto, y hay una comprobación JVM de geometría con un
negativo que **no pasa en vacío** (dos mutaciones reales, incluida una mía distinta: falla a radio 43.2).
Los ocho escenarios y el Validator Checklist se cumplen; las dos Low no reparadas (ignore de lint acotado
por ruta, endurecimiento XXE del parser de test) siguen siendo **aceptables** y están documentadas; la
desviación de alcance (`app/lint.xml`) es la misma ya aceptada. Sin hallazgos nuevos de severidad ≥ Low y
sin regresiones (104 suites / 776 tests / 0 fallos, lint 13 resultados / 0 errores / 0 bajo `app/src`).
Sin commit, sin push, sin PR y sin cambios fuera de alcance.

**Estado recomendado**: que el orquestador persista **`accepted`** en `feature_list.json` y registre el
cierre en `PROGRESS.md` (el validador no marca la aceptación), añada una entrada de evidencia con esta
re-validación, y deje la **prueba manual de la persona en el emulador** (icono cuadrado/redondo/tematizado
en claro y oscuro + splash con el cuenco completo) como revisión posterior al merge: si el acabado no
convence, se corrige con una **feature correctiva**, sin reabrir esta.
