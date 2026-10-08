# Feature Implementation Spec: Tokens de diseño y tema de la app

## Source Feature

- `id`: `design-tokens-and-theme`
- `area`: `ui`
- `depends_on`: `["bootstrap-android-project"]` (satisfecho: `accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`.
- `source`: `feature_list.json`

## Goal

Hacer que `DESIGN.md` sea la **única fuente de colores, tipografía, formas y espaciado** de la app,
materializada en un paquete `com.sazon.app.ui.theme` que cualquier pantalla futura consuma sin
improvisar. Hoy la app arrastra la paleta de plantilla (purple/teal), usa **color dinámico de Material
You** (que cambia con el fondo de pantalla y contradice el diseño) y su `Typography`/`Shapes` son los
valores por defecto de Material 3.

La feature entrega: (1) la paleta y los tokens de `DESIGN.md` como constantes Compose más una
**variante oscura**; (2) `SazonTheme` cableado a esos tokens **sin** color dinámico; (3) el color de
ventana del sistema alineado; (4) un **test JVM** que compara los tokens expuestos con el frontmatter
YAML de `DESIGN.md`, con un caso negativo que prueba que el test no pasa en vacío; y (5) la limpieza de
la paleta de plantilla, los recursos de color sin usar y el aviso `RedundantLabel`.

**Decisión de diseño cerrada:** el modo oscuro **entra en el MVP** (pregunta abierta 3 de `DESIGN.md`,
resuelta el 2026-10-06). `DESIGN.md` solo fija `darkBackground`/`darkSurface` y pide conservar los
acentos terracota y oliva; los roles oscuros que faltan se derivan en esta spec y se marcan como
tales. El repaso puramente visual de la variante oscura lo hará la persona tras el merge.

## Non-Goals

- **No** se implementa ninguna pantalla ni componente (botones, chips, `RecipeCard`): la primera
  pantalla real es `onboarding-shell-and-identity`. Aquí solo nacen los tokens y el tema.
- **No** se rediseña el icono de la app (`ic_launcher_*` sigue siendo el de plantilla): el nombre e
  icono definitivos son la pregunta abierta 2 de `DESIGN.md`.
- **No** se usan ni empaquetan iconos ni fotos de receta (features/pantallas posteriores).
- **No** se toca la paleta de avatares: vive en `avatarColors` de la Taxonomía y la consume la
  identidad.
- **No** se añade color dinámico / Material You en ninguna forma, ni fuentes descargables por red
  (Google Play Services).
- **No** se introduce un tema XML de Material Components (la app es 100 % Compose) ni navegación.

## Job Story

Cuando empiezo a construir las pantallas de Sazón,
quiero que los colores, la tipografía, las formas y el espaciado estén definidos una sola vez,
coincidan con `DESIGN.md` y funcionen en claro y en oscuro,
para que ninguna pantalla improvise color ni se salga de la identidad visual.

## Users And Permissions

- **Desarrollador / agente**: define los tokens, ejecuta el test JVM y el gate; no hay permisos.
- **Usuario local**: no interactúa aún con esta feature; solo percibe que el fondo del arranque y la
  variante clara/oscura siguen `DESIGN.md` y no cambian con su fondo de pantalla.

## Acceptance Scenarios

### Scenario 1: la variante clara expone los tokens de DESIGN.md

Given el frontmatter de `DESIGN.md`,
When se inspecciona `LightColorScheme` y `SazonTypography`,
Then `primary`, `onPrimary`, `primaryContainer`, `secondary`, `tertiary` (accent), `background`,
`surface`, `onBackground`/`onSurface` (text), `outline` y `error` coinciden exactamente con sus hex, y
la escala tipográfica coincide en `fontSize` y `fontWeight`.

### Scenario 2: la variante oscura usa los anclajes de DESIGN.md

Given el modo oscuro del sistema activo,
When `SazonTheme` compone,
Then `background == #241C16` (`darkBackground`), `surface == #312720` (`darkSurface`) y los acentos
`primary == #D9552B` / `secondary == #5F7A3D` / `tertiary == #F2A93B` se conservan, con texto legible
en crema `#FFF8F0`.

### Scenario 3: el fondo de pantalla no cambia ningún color (manual)

Given un dispositivo con Material You y un fondo de pantalla de color vivo,
When se abre la app y se cambia el fondo de pantalla,
Then ningún color de la app cambia: no existe ruta de color dinámico y `forceDarkAllowed = false`.

### Scenario 4: deriva de tokens detectada (caso negativo obligatorio)

Given una copia de `DESIGN.md` con `primary` alterado,
When el test compara con `LightColorScheme.primary`,
Then la comprobación **falla**; y ningún rol de los esquemas expuestos equivale a un color de la
paleta de plantilla (purple/teal).

### Scenario 5: limpieza de plantilla y lint

Given el código tras la feature,
When se ejecuta `./gradlew lint`,
Then no quedan `UnusedResources` por colores de plantilla ni `RedundantLabel`, y no aparecen warnings
nuevos en ficheros de la feature.

## Repository Research

### Files Inspected

- `DESIGN.md` — frontmatter YAML (colores, tipografía, `rounded`, `spacing`, `components`); pregunta
  abierta 3 resuelta: modo oscuro en el MVP.
- `feature_list.json` / `PROGRESS.md` — definición, verificación y sesiones previas; gate
  `./gradlew test` + `./gradlew lint` vía `init.sh`.
- `app/src/main/java/com/sazon/app/ui/theme/Theme.kt` — `dynamicColor = true` (Material You),
  esquemas purple/teal; **a sustituir**.
- `app/src/main/java/com/sazon/app/ui/theme/Color.kt` — constantes purple/teal de plantilla.
- `app/src/main/java/com/sazon/app/ui/theme/Type.kt` — solo `bodyLarge`, `FontFamily.Default`.
- `app/src/main/java/com/sazon/app/MainActivity.kt` — `SazonTheme { Surface(fillMaxSize) }` sin color
  explícito (usa `surface` por defecto).
- `app/src/main/res/values/colors.xml` — 7 colores de plantilla sin usar (purple/teal/black/white).
- `app/src/main/res/values/themes.xml` — `Theme.Sazon` sin `windowBackground` ni `forceDarkAllowed`.
- `app/src/main/AndroidManifest.xml` — `android:label` en la actividad (aviso `RedundantLabel`).
- `app/build.gradle.kts` — patrón de propiedades de sistema para tests
  (`taxonomy.asset.path`, `seed.recipes.asset.path`); `isIncludeAndroidResources = true`.
- `gradle/libs.versions.toml` — Compose BOM `2026.02.01`, Material 3; Robolectric 4.17 disponible.

### Existing Patterns To Follow

- Documentación y comentarios en **español**; identificadores en inglés; paquetes por capa
  (`domain`, `data`, `ui.theme`).
- Los tests corren en **JVM puro** salvo necesidad de Android (entonces Robolectric, ya presente).
- Los paths de ficheros de verdad se inyectan como **propiedades de sistema** desde
  `app/build.gradle.kts` (patrón `seed.recipes.asset.path`).
- Los tests incluyen **casos negativos** que prueban que no pasan en vacío (patrón de
  `TaxonomyIntegrityTest`/`SeedRecipeCatalogTest`).
- `init.sh` es el gate no bloqueante (`./gradlew test` + `./gradlew lint`); no arranca procesos largos.

### Current Gaps

- No existe variante oscura, ni mapeo de roles de Material 3, ni tokens de formas/espaciado.
- No hay ficheros de fuente Manrope/Inter en `app/src/main/res/font/`.
- `DESIGN.md` no declara los roles oscuros derivados (`onSurface`, `outline`, contenedores): esta spec
  los fija para que el implementer no improvise.
- No hay test que detecte la deriva entre `DESIGN.md` y el tema.

## Technical Approach

**Paquete `com.sazon.app.ui.theme` como única fuente de verdad visual.**

1. **`Color.kt`** — constantes `Color` nombradas de la paleta (`SazonPrimary`, `SazonSecondary`,
   `SazonAccent`, `SazonBackground`, `SazonSurface`, `SazonText`, `SazonOutline`, `SazonSuccess`,
   `SazonError`, `SazonDarkBackground`, `SazonDarkSurface`, etc.) y los esquemas
   `LightColorScheme`/`DarkColorScheme` construidos con `lightColorScheme(...)`/`darkColorScheme(...)`.
   **Se sobrescriben explícitamente todos los roles** para que ningún valor por defecto purple/pink de
   Material 3 pueda mostrarse.
2. **`Type.kt`** — `SazonFontFamilies` (Manrope display/h1; Inter body/caption) y `SazonTypography`
   con la escala de `DESIGN.md`:
   - `display` → `displayLarge` 32sp / 700
   - `h1` → `headlineLarge` 28sp / 600
   - `body` → `bodyLarge` 16sp / 400
   - `caption` → `labelSmall` 12sp / 500
   Las fuentes Manrope/Inter son **ficheros OFL empaquetados** en `app/src/main/res/font/`
   (`manrope_semibold.ttf`, `manrope_bold.ttf`, `inter_regular.ttf`, `inter_medium.ttf`) y se cargan
   con `Font(R.font...., FontWeight....)`. Ver riesgo 1 y el plan de respaldo.
3. **`Shape.kt` (nuevo)** — `SazonShapes` (`Shapes`: `extraSmall`/`small` = 16.dp, `medium`/`large`/
   `extraLarge` = 24.dp) y `SazonPillShape = RoundedCornerShape(percent = 50)` (botones y chips).
4. **`Spacing.kt` (nuevo)** — `SazonSpacing` (`sm` 12.dp, `md` 16.dp, `lg` 24.dp) y
   `SazonComponents` (`buttonHeight` 52.dp, `bottomNavHeight` 80.dp, `recipeCardImageRatio` 16/10,
   `bottomNavInactive` `#9A8C7F`), tomados de `components` de `DESIGN.md`.
5. **`Theme.kt`** — `SazonTheme(darkTheme = isSystemInDarkTheme(), content)` que pasa
   `colorScheme`, `typography` y `shapes`. **Se elimina** `dynamicColor`, `dynamicLightColorScheme`,
   `dynamicDarkColorScheme` y `LocalContext`: no hay ninguna ruta que lea el fondo de pantalla.
6. **Sistema** — `values/colors.xml` se sustituye por `sazon_window_background` (`#FFF8F0`);
   `values-night/colors.xml` lo redefine a `#241C16`; `themes.xml` (y `values-night/themes.xml`)
   fijan `android:windowBackground` y `android:forceDarkAllowed=false`. `MainActivity` pinta la
   `Surface` con `MaterialTheme.colorScheme.background` para que el token se use de verdad.
7. **Test** — `DesignTokensTest` (JVM) lee `DESIGN.md` vía la propiedad de sistema
   `design.tokens.path` (añadida en `app/build.gradle.kts`), parsea el frontmatter y compara con los
   tokens. Incluye el caso negativo (documento mutado + ausencia de paleta de plantilla).

### Mapeo de roles (contrato)

Anclados a `DESIGN.md`:

| Rol Material 3 | Claro | Oscuro |
|---|---|---|
| `primary` / `surfaceTint` | `#D9552B` | `#D9552B` |
| `onPrimary` | `#FFFFFF` | `#FFFFFF` |
| `primaryContainer` | `#FFDCCF` | `#6E2C15` |
| `onPrimaryContainer` | `#2B2118` | `#FFDCCF` |
| `secondary` | `#5F7A3D` | `#5F7A3D` |
| `onSecondary` | `#FFFFFF` | `#FFFFFF` |
| `tertiary` (accent) | `#F2A93B` | `#F2A93B` |
| `onTertiary` | `#2B2118` | `#2B2118` |
| `background` | `#FFF8F0` | `#241C16` |
| `onBackground` / `onSurface` | `#2B2118` | `#FFF8F0` |
| `surface` | `#FFFFFF` | `#312720` |
| `outline` | `#E8DDD3` | `#5A4B40` |
| `error` | `#B3261E` | `#B3261E` |
| `onError` | `#FFFFFF` | `#FFFFFF` |

Derivados (no en `DESIGN.md`; primera pasada sujeta a repaso visual por la persona):

- Claros: `secondaryContainer #DCE8CC`, `onSecondaryContainer #2B2118`,
  `tertiaryContainer #FCE3B8`, `onTertiaryContainer #2B2118`, `inversePrimary #A63B1A`,
  `inverseSurface #2B2118`, `inverseOnSurface #FFF8F0`, `surfaceVariant #F2E8DE`,
  `onSurfaceVariant #5A4B40`, `outlineVariant #E8DDD3`.
- Oscuros: `secondaryContainer #2F3D1E`, `onSecondaryContainer #D7E8BF`,
  `tertiaryContainer #4A3413`, `onTertiaryContainer #FFE7BE`, `inversePrimary #FFB59A`,
  `inverseSurface #FFF8F0`, `inverseOnSurface #2B2118`, `surfaceVariant #3D3129`,
  `onSurfaceVariant #E8DDD3`, `outlineVariant #3D3129`.
- `errorContainer`/`onErrorContainer`: heredar los de Material 3 (familia roja, no purple) o fijarlos
  a `#FFDAD6`/`#410E0B` (claro) y `#8C0009`/`#FFDAD6` (oscuro).
- Familia `surfaceContainer*`: `surface` en ambos modos; `surfaceBright = surface`,
  `surfaceDim = background`; `scrim = #000000`.

## Expected File Changes

- `app/src/main/java/com/sazon/app/ui/theme/Color.kt` — modificar; paleta DESIGN + esquemas claro/oscuro.
- `app/src/main/java/com/sazon/app/ui/theme/Type.kt` — modificar; escala tipográfica + familias.
- `app/src/main/java/com/sazon/app/ui/theme/Theme.kt` — modificar; quitar color dinámico, cablear
  tipografía y formas.
- `app/src/main/java/com/sazon/app/ui/theme/Shape.kt` — crear; formas de `DESIGN.md`.
- `app/src/main/java/com/sazon/app/ui/theme/Spacing.kt` — crear; espaciado y tokens de componentes.
- `app/src/main/res/font/{manrope_semibold,manrope_bold,inter_regular,inter_medium}.ttf` — crear;
  fuentes OFL (paso de asset).
- `app/src/main/res/values/colors.xml` — modificar; quitar plantilla, añadir `sazon_window_background`.
- `app/src/main/res/values-night/colors.xml` — crear; fondo de ventana oscuro.
- `app/src/main/res/values/themes.xml` — modificar; `windowBackground` + `forceDarkAllowed=false`.
- `app/src/main/res/values-night/themes.xml` — crear; variante noche.
- `app/src/main/AndroidManifest.xml` — modificar; quitar `android:label` de la actividad.
- `app/src/main/java/com/sazon/app/MainActivity.kt` — modificar; `Surface(color = colorScheme.background)`.
- `app/build.gradle.kts` — modificar; `systemProperty("design.tokens.path", ...)`.
- `app/src/test/java/com/sazon/app/ui/theme/DesignTokensTest.kt` — crear; test de tokens.
- `docs/technical-discovery.md` — actualizar; tema, modo oscuro en MVP, color dinámico desactivado y
  fuentes empaquetadas.
- `PROGRESS.md` / `feature_list.json` — actualizar (implementer/validador/orquestador).

## Visual Design Impact

- UI involved: **sí** (tema global; ninguna pantalla nueva).
- Design source: `DESIGN.md` (fuente de verdad; sección "Responsive Baseline" para el modo oscuro).
- Screens or states affected: el armazón vacío de `MainActivity` (fondo) y, por herencia, toda pantalla
  futura en claro y en oscuro.
- New design artifact required: **no**. Los valores oscuros derivados son una propuesta técnica a
  revisar visualmente; si la persona detecta defectos, se corrige con una feature correctiva.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite deliberadamente.
- `CONSTRAINTS.md`: **not needed** — ninguna regla MUST/MUST NOT nueva de nivel repo; la fuente única
  de estilo ya queda fijada en `DESIGN.md` y en el test.
- `AGENTS.md`: **not needed** — el flujo de trabajo no cambia.
- `docs/technical-discovery.md`: **update** — ubicación del tema, modo oscuro en el MVP, color
  dinámico desactivado y fuentes empaquetadas.
- `DESIGN.md`: **update opcional** — si se decide promover los roles oscuros derivados a tokens
  oficiales, añadirlos al frontmatter; no es requisito de esta feature.
- `docs/risks-and-open-questions.md`: **update** — anotar la licencia OFL de Manrope/Inter si se
  materializan las fuentes.

## Implementation Plan

1. Crear `Color.kt` con la paleta y los dos esquemas, sobrescribiendo todos los roles.
2. Crear `Shape.kt` y `Spacing.kt` con los tokens de formas, espaciado y componentes.
3. Reescribir `Type.kt` con la escala de `DESIGN.md` y las familias Manrope/Inter.
4. Añadir las fuentes `.ttf` a `res/font/` (o aplicar el respaldo documentado si no están).
5. Reescribir `Theme.kt` sin color dinámico y cableando `typography`/`shapes`.
6. Alinear el sistema: `colors.xml` + `values-night`, `themes.xml` + `values-night`, quitar
   `RedundantLabel` y pintar `Surface` con `colorScheme.background`.
7. Añadir `design.tokens.path` en `app/build.gradle.kts` y escribir `DesignTokensTest`.
8. Ejecutar el gate y actualizar `docs/technical-discovery.md`, `PROGRESS.md` y `feature_list.json`.

## Implementation Tasks

- [ ] Reescribir `Color.kt`: paleta `DESIGN.md` + `LightColorScheme`/`DarkColorScheme` con todos los
      roles sobrescritos (sin purple/pink por defecto).
- [ ] Crear `Shape.kt`: `SazonShapes` (16/24) y `SazonPillShape`.
- [ ] Crear `Spacing.kt`: `SazonSpacing` (12/16/24) y `SazonComponents`.
- [ ] Reescribir `Type.kt`: escala display/h1/body/caption y familias Manrope/Inter.
- [ ] Añadir las cuatro fuentes OFL a `res/font/` (o respaldo documentado; ver riesgo 1).
- [ ] Reescribir `Theme.kt` eliminando `dynamicColor` y `LocalContext`.
- [ ] Sustituir `colors.xml`, crear `values-night/colors.xml`, actualizar `themes.xml` y crear
      `values-night/themes.xml` (`windowBackground`, `forceDarkAllowed=false`).
- [ ] Quitar `android:label` de la actividad y pintar `Surface` con `colorScheme.background`.
- [ ] Añadir `systemProperty("design.tokens.path", ...)` en `app/build.gradle.kts`.
- [ ] Crear `DesignTokensTest` (tokens + caso negativo) y dejarlo en verde.
- [ ] Ejecutar `init.sh` y actualizar `docs/technical-discovery.md`, `PROGRESS.md` y
      `feature_list.json`.

## Verification Plan

- `./gradlew test` (vía `init.sh`) → **esperado**: `DesignTokensTest` en verde, sin romper los tests
  existentes (51+).
- `./gradlew lint` (vía `init.sh`) → **esperado**: 0 errores; desaparecen los `UnusedResources` de
  color de plantilla y el `RedundantLabel`; sin warnings nuevos en ficheros de la feature.
- `init.sh` con Git Bash → **esperado**: código 0, sin procesos largos.
- Manual (emulador) — fondo de pantalla: cambiar el fondo en Ajustes → **esperado**: ningún color
  cambia.
- Manual (emulador) — modo oscuro: activar el tema oscuro del sistema → **esperado**: fondo
  `#241C16`, superficies `#312720`, acentos terracota/oliva intactos y texto crema legible.

Sobre `init.sh`: mantiene el gate no bloqueante (`./gradlew test` + `./gradlew lint`); **no** arranca
emulador ni servidor y puede seguir imprimiendo los comandos manuales.

Cobertura E2E: **no aplica**. No existe suite E2E persistente y AGENTS.md prohíbe automatizar pruebas
de UI de emulador; el test JVM cubre la correspondencia de tokens y las comprobaciones visuales
(wallpaper/dark) son manuales.

## Evidence To Capture

- Salida de `DesignTokensTest` (nº de tests y 0 fallos) y del total de tests.
- Salida de `lint` con 0 errores y sin `UnusedResources` de plantilla ni `RedundantLabel`.
- `init.sh` con código 0.
- Resultado manual del cambio de fondo de pantalla y del modo oscuro (descripción; captura opcional).

## Validator Checklist

- [ ] La implementación se mantiene en `ui.theme` + recursos de tema, sin pantallas ni componentes.
- [ ] Los escenarios 1–5 pasan; el test compara de verdad contra `DESIGN.md` y falla con la copia
      mutada (no pasa en vacío).
- [ ] No queda ninguna ruta de color dinámico ni color de la paleta de plantilla en los esquemas.
- [ ] La evidencia de verificación está en `feature_list.json`/`PROGRESS.md`.
- [ ] `lint` no añade warnings nuevos y desaparecen `UnusedResources` de plantilla y `RedundantLabel`.
- [ ] `feature_list.json` y `PROGRESS.md` se actualizan correctamente.
- [ ] No se ha añadido trabajo fuera de alcance (icono, pantallas, fuentes descargables, taxonomía).

## Findings (implementer)

- **Fuentes (riesgo 1, referencia huérfana)**: la spec cita un "riesgo 1" y un "plan de respaldo" que
  no existen como sección en este documento. Se resolvió sin respaldo: las fuentes OFL se obtuvieron de
  las fuentes variables oficiales de Google Fonts (`Manrope[wght].ttf`, `Inter[opsz,wght].ttf`) y se
  instanciaron a estáticas con `fontTools` (`wght=600/700` para Manrope con `opsz` a su valor por
  defecto; `opsz=14 wght=400/500` para Inter). Los `.ttf` resultantes van en `app/src/main/res/font/`.
  El texto de licencia OFL está en `docs/licenses/`. No se introduce ninguna descarga en runtime.
- **`text` de `DESIGN.md`**: el token `colors.text` se mapea a `onBackground` **y** `onSurface` (como
  fija la tabla de roles). El test lo comprueba en ambos.
- **`.gitattributes`**: se añade `*.ttf binary` para que `core.autocrlf=true` no pueda corromper las
  fuentes empaquetadas en un clon (mismo motivo que las entradas de `*.jar` y `*.webp`).
- **`onTertiary`**: se añade la constante explícita `SazonOnAccent` (`#2B2118`), ausente en el
  frontmatter, para que el modo claro y el oscuro compartan el mismo token.
