# Feature Implementation Spec: Icono propio de la app y splash de marca

## Source Feature

- `id`: `app-launcher-icon`
- `area`: `ui`
- `depends_on`: `["design-tokens-and-theme"]` (satisfecho: `accepted`, sesión 023)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`.
- `source`: `feature_list.json` (entry `app-launcher-icon`, ampliación pedida por la persona el
  2026-10-15: "la app está sin icono") + `DESIGN.md` (pregunta abierta 2, que esta feature cierra).
- Sin issue de GitHub asociada (alta directa en `feature_list.json`, igual que el resto de la lista).
- Verificación exigida (de la feature): `bash init.sh` en verde **sin dependencias nuevas ni assets
  binarios nuevos**; test JVM de las tres capas del icono (drawables existentes, vectores XML y colores
  de `DESIGN.md`); test JVM del tema del splash (`values-v31` declara fondo e icono, el tema base sigue
  fijando `windowBackground`, los recursos referenciados existen); prueba manual de la persona en
  emulador (claro/oscuro, cuadrado/redondo/tematizado, splash).

## Goal

Sustituir el **icono de plantilla de Android Studio** (fondo `#3DDC84` con rejilla + robot) por un
**icono propio de Sazón** hecho en el repo con **vectores XML** derivados de los tokens de `DESIGN.md`,
más un **splash dedicado** de Android 12+ con la marca sobre el fondo cálido.

La feature entrega: (1) el icono adaptativo completo (`background` + `foreground` + `monochrome`) en
forma cuadrada y redonda, con el fondo cálido de marca en claro (`background` `#FFF8F0`) y su variante
oscura (`darkBackground` `#241C16`) vía `values-night`; (2) la retirada del icono y de los rasters de
plantilla, que con `minSdk 33` son código muerto (el sistema usa siempre `mipmap-anydpi/`); (3) el
**tema del splash** de Android 12+ (`android:windowSplashScreenBackground` +
`android:windowSplashScreenAnimatedIcon`), sin añadir `androidx.core.splashscreen`; (4) **tests JVM**
que ata el icono y el splash a `DESIGN.md` y que no pasan en vacío; y (5) el registro en `DESIGN.md`,
que **cierra la pregunta abierta 2** (nombre e icono definitivos).

**El icono lo diseña el agente** (no la persona): el motivo es un **cuenco lineal humeante** (trazo
terracota `primary` + vaho ámbar `accent`) sobre el fondo cálido de marca, dentro de la zona segura del
icono adaptativo. Es coherente con `DESIGN.md` ("line-art plate icon", terracota para la pieza
principal, ámbar de acento, sin tercera tipografía ni adornos).

## Non-Goals

- **No** se cambia el nombre del producto: sigue siendo **Sazón** (`@string/app_name`). La pregunta
  abierta 2 se cierra con nombre e icono **definitivos**, sin renombrar la app, el paquete ni el
  `applicationId`.
- **No** se añaden **assets binarios** (`.png`, `.webp`, `.jpg`): el icono y el splash son **vectores
  XML** y colores de recursos. Tampoco se añaden iconos de Play Store (la app no se publica).
- **No** se añaden dependencias: en particular **no** entra `androidx.core.splashscreen` (con
  `minSdk 33` los atributos de splash son de plataforma, API 31+) ni `material-icons`.
- **No** se toca el diseño de las pantallas, la tipografía, la paleta, los tokens de tema, la ficha
  tipográfica de tarjeta (`recipe-card-*`, ADR 0006), los estados vacíos ni la navegación.
- **No** se introduce un splash ilustrado con animación propia: solo el splash de sistema con la marca
  sobre el fondo cálido (sin `windowSplashScreenAnimationDuration` ni animación vectorial).
- **No** se generan capturas de Stitch ni **se crea** `docs/design/concepts/app-launcher-icon/`: un
  icono de lanzador no es una pantalla (misma clase de excepción que #77/#85/#86/#91). La dirección
  visual vive en la nueva sección `## App Icon` de `DESIGN.md`.
- **No** se renombra la carpeta `mipmap-anydpi/` a `mipmap-anydpi-v26/` (ver
  `Decisions And Unknowns`): es churn sin efecto funcional con `minSdk 33`.

## Job Story

Cuando acabo de instalar Sazón y la veo en el cajón de aplicaciones, en recientes o en Ajustes,
quiero que su icono sea el de Sazón (marca cálida y terracota) y que el arranque muestre la marca,
para reconocerla de un vistazo y no ver el robot genérico de la plantilla.

## Users And Permissions

- **Usuario local**: único dueño del dispositivo (ADR 0002); ve el icono y el splash. No hay permisos,
  cuentas ni roles nuevos.
- **Desarrollador / agente**: diseña los vectores XML, el tema del splash y los tests JVM. **No**
  ejecuta `./gradlew` directo (usa `bash init.sh` o `bash scripts/gradle.sh`).
- **Persona**: revisa el acabado en el emulador tras el merge (AGENTS.md no automatiza UI de emulador);
  un defecto visual se corrige con una **feature correctiva** nueva.

## Acceptance Scenarios

### Scenario 1: el icono adaptativo declara las tres capas y apunta a vectores propios (verificación exigida 2)

Given `app/src/main/res/mipmap-anydpi/ic_launcher.xml` y `ic_launcher_round.xml`,
When el test JVM los parsea,
Then ambos declaran `background`, `foreground` y `monochrome`; cada `@drawable/...` referenciado existe
como fichero `.xml` que contiene un elemento `<vector>`; `background` → `ic_launcher_background`,
`foreground` → `ic_launcher_foreground` y `monochrome` → `ic_launcher_monochrome` (capa propia, no
reutiliza el motivo a color); y el manifiesto sigue declarando `android:icon="@mipmap/ic_launcher"` y
`android:roundIcon="@mipmap/ic_launcher_round"`.

### Scenario 2: la paleta del icono sale de `DESIGN.md` (verificación exigida 2)

Given el frontmatter de `DESIGN.md` y los recursos de color del repo,
When el test JVM resuelve cada `android:fillColor`/`android:strokeColor` de los cuatro vectores
(`ic_launcher_background`, `ic_launcher_foreground`, `ic_launcher_monochrome`, `ic_splash_mark`),
Then `sazon_launcher_background` vale `background` `#FFF8F0` en `values/` y `darkBackground` `#241C16`
en `values-night/`; `sazon_launcher_motif` vale `primary` `#D9552B`; `sazon_launcher_accent` vale
`accent` `#F2A93B`; el `foreground` usa **motivo y acento** (cobertura de los dos roles); y **ningún**
color literal de los vectores queda fuera de la paleta declarada en `DESIGN.md`, con la única excepción
documentada de `#000000` en `ic_launcher_monochrome.xml` (la silueta que el sistema tiñe por alfa).

### Scenario 3: no queda rastro del icono de plantilla (verificación exigida 1)

Given el código tras la feature,
When se inspecciona `app/src/main/res`,
Then no quedan los rasters `mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher{,_round}.webp` (10
ficheros) ni los colores/geometrías de plantilla (`#3DDC84`, la rejilla con `#33FFFFFF` y trazos de
`strokeWidth="0.8"`, el robot `M65.3,45.828`, el `<gradient>`/`aapt:attr` del degradado de plantilla),
y no aparece ningún asset binario nuevo (`git status --porcelain` solo lista `.xml`, `.kt` y `.md`).

### Scenario 4: el splash de Android 12+ muestra la marca (verificación exigida 3)

Given un dispositivo API 33+ (día o noche),
When la app arranca,
Then `values-v31/themes.xml` **y** `values-night-v31/themes.xml` declaran en `Theme.Sazon`
`android:windowSplashScreenBackground="@color/sazon_window_background"` (que resuelve a `#FFF8F0` en
claro y a `#241C16` en oscuro) y
`android:windowSplashScreenAnimatedIcon="@drawable/ic_splash_mark"`, donde `ic_splash_mark` es un
vector XML existente con colores de la paleta; y **no** se declara
`android:windowSplashScreenIconBackgroundColor` (decisión: la marca va directamente sobre el fondo
cálido, sin placa circular).

### Scenario 5: el tema base no pierde nada (regresión)

Given los temas previos de `design-tokens-and-theme`,
When el test JVM parsea `values/themes.xml` y `values-night/themes.xml`,
Then `Theme.Sazon` sigue declarando `android:windowBackground="@color/sazon_window_background"` y
`android:forceDarkAllowed="false"` en ambos, y las dos variantes de `values-v31` repiten esos mismos
items (el estilo de una carpeta cualificada **no** hereda los items de la variante base).

### Scenario 6: deriva de paleta detectada (caso negativo obligatorio)

Given una paleta mutada (por ejemplo `primary: "#123456"`) o un `values-night/colors.xml` mutado,
When la comprobación de colores se ejecuta contra esa copia,
Then **falla** (devuelve ≥ 1 problema): el test no puede pasar en vacío.

### Scenario 7: `DESIGN.md` registra la dirección y cierra la pregunta abierta 2

Given que hoy el icono es el de plantilla y la pregunta abierta 2 sigue abierta,
When se implementa la feature,
Then `DESIGN.md` gana la sección `## App Icon` (motivo, roles de color, zona segura, capa monocroma y
splash), marca la **pregunta abierta 2 como resuelta** (2026-10-15, `app-launcher-icon`; nombre e icono
definitivos) y registra en `Generated Concept Images` la **excepción de capturas** (no se crea
`docs/design/concepts/app-launcher-icon/`).

### Scenario 8: nada más cambia (verificación exigida 1)

Given el repositorio antes de la feature,
When se aplica,
Then no cambian `app/build.gradle.kts` (salvo las dos propiedades de sistema de test), `gradle/
libs.versions.toml`, `init.sh`, `scripts/**`, `domain/**`, `data/**`, `ui/**`, la Taxonomía,
`seed-recipes.json` ni los assets; no hay dependencias nuevas; y `bash init.sh` sigue en verde con
`lint` sin warnings nuevos.

## Repository Research

### Files Inspected

- `feature_list.json` — entry `app-launcher-icon` (verificación exigida y nota de alcance: el agente
  diseña el icono, se retiran los rasters, splash vía `values-v31`, sin capturas) y su dependencia
  `design-tokens-and-theme` (`accepted`).
- `PROGRESS.md` — sesión 023 (`design-tokens-and-theme` aceptada: tema, `values-night`, fuentes,
  `DesignTokensTest`); convención de evidencia y gate.
- `AGENTS.md` — gate `bash init.sh` (nunca `./gradlew` directo), UI no automatizable en emulador,
  capturas opcionales (excepción autorizada), un defecto visual → feature correctiva.
- `DESIGN.md` — frontmatter (colores incl. `darkBackground`, `darkSurface`, `darkCard*`), `## Colors`,
  `## Components`, `## Generated Concept Images` (plantilla de los párrafos de excepción #77/#85/#86/#91),
  `## Open Design Questions` (id 2 abierta; ids 3/4/6/7 resueltas).
- `app/src/main/AndroidManifest.xml` — `android:icon="@mipmap/ic_launcher"` y
  `android:roundIcon="@mipmap/ic_launcher_round"`; `android:theme="@style/Theme.Sazon"` en aplicación y
  actividad.
- `app/src/main/res/drawable/ic_launcher_background.xml` — vector 108×108 de plantilla: cuadrado
  `#3DDC84` + 30 trazos de rejilla `#33FFFFFF` con `strokeWidth="0.8"` (a sustituir).
- `app/src/main/res/drawable/ic_launcher_foreground.xml` — vector 108×108 de plantilla con `aapt:attr`
  (`<gradient>`) y el robot blanco (`M65.3,45.828…`) (a sustituir).
- `app/src/main/res/mipmap-anydpi/ic_launcher.xml` y `ic_launcher_round.xml` — `<adaptive-icon>` con
  `background`/`foreground`/`monochrome`, los tres apuntando a los drawables de plantilla.
- **Rutas reales, no las de la nota**: la carpeta es `mipmap-anydpi/` (**sin** `-v26`) y contiene las
  dos variantes; los rasters están en `mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher{,_round}.webp`
  (10 ficheros). Con `minSdk 33` el sistema resuelve siempre `mipmap-anydpi/`: los `.webp` son muertos.
- `app/src/main/res/values/colors.xml` — solo `sazon_window_background` `#FFF8F0` (se amplía aquí).
- `app/src/main/res/values-night/colors.xml` — `sazon_window_background` `#241C16` (se amplía aquí).
- `app/src/main/res/values/themes.xml` y `values-night/themes.xml` — `Theme.Sazon` con
  `android:windowBackground` y `android:forceDarkAllowed=false`, parents
  `android:Theme.Material.Light.NoActionBar` / `android:Theme.Material.NoActionBar`. **No existe**
  `values-v31/` (se crea) ni ningún atributo de splash en todo `app/src/main` (grep: 0 coincidencias).
- `app/build.gradle.kts` — patrón de propiedades de sistema para tests
  (`taxonomy.asset.path`, `seed.recipes.asset.path`, `design.tokens.path`, `dish.image.seed.assets.dir`)
  dentro de `tasks.withType<Test>().configureEach`; `minSdk = 33`, `targetSdk = 36`;
  `unitTests.isIncludeAndroidResources = true`.
- `app/src/test/java/com/sazon/app/ui/theme/DesignTokensTest.kt` y `DesignFrontmatter.kt` — patrón del
  test de tokens: lee `DESIGN.md` por `design.tokens.path`, compara hex contra el frontmatter y usa un
  **caso negativo** que muta una copia (no pasa en vacío). `designMarkdown()` y
  `frontmatterSectionMap()` son `internal` del paquete `com.sazon.app.ui.theme` (**reutilizables**).
- `docs/technical-discovery.md` — tabla de recursos (fila "Iconos e imágenes de receta"), párrafo de
  `ui.theme` (fuentes/`values-night`/`DesignTokensTest`) y "Testing and Verification".
- `docs/design/concepts/README.md` — las capturas son material **opcional**; `DESIGN.md` manda.

### Existing Patterns To Follow

- **`DESIGN.md` es la fuente de verdad visual**: los colores de esta feature **son** tokens suyos
  (`background`, `darkBackground`, `primary`, `accent`), no hex inventados.
- **Tests JVM puros** con la ruta real inyectada por **propiedad de sistema** desde
  `app/build.gradle.kts` (nunca rutas relativas al `user.dir`); los tests viven en el **mismo paquete**
  que el parser que reutilizan.
- **Caso negativo obligatorio** que prueba que la comprobación no pasa en vacío (`DesignTokensTest`,
  `TaxonomyIntegrityTest`).
- **Recursos XML en UTF-8 sin BOM** y documentación/comentarios en **español**; identificadores en inglés.
- **`bash init.sh` es el gate no bloqueante** (`./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`), sin procesos largos y sin emulador; `init.sh` **no cambia** con esta feature.

### Current Gaps

- El icono y los drawables son los de plantilla (verde `#3DDC84` + robot); no hay capa monocroma propia
  (hoy el `monochrome` reutiliza el `foreground` de plantilla).
- No existe `values-v31/` ni ningún atributo de splash: el arranque en API 31+ usa el icono de la app
  por defecto (sin marca dedicada).
- No existe ningún test que relacione icono/recursos con `DESIGN.md` (la paleta solo se comprueba para
  el tema Compose).
- `DESIGN.md` no documenta dirección de icono y su pregunta abierta 2 sigue abierta.

## Technical Approach

### 1. Icono adaptativo (vectores XML + colores de recursos)

- `res/values/colors.xml` gana: `sazon_launcher_background` = `#FFF8F0` (`colors.background`),
  `sazon_launcher_motif` = `#D9552B` (`colors.primary`) y `sazon_launcher_accent` = `#F2A93B`
  (`colors.accent`). `res/values-night/colors.xml` redefine **solo**
  `sazon_launcher_background` = `#241C16` (`colors.darkBackground`); los acentos no cambian (misma
  regla que el tema).
- `drawable/ic_launcher_background.xml` (modificar): vector 108×108 con **un** path de relleno
  `M0,0h108v108h-108z` y `android:fillColor="@color/sazon_launcher_background"`. Se borran la rejilla y
  el verde de plantilla.
- `drawable/ic_launcher_foreground.xml` (modificar): vector 108×108 con el **cuenco lineal humeante**,
  todo dentro de la zona segura ([21,87]²):
  - borde del cuenco: `M29,54h46` con `strokeColor="@color/sazon_launcher_motif"`, `strokeWidth="6"`,
    `strokeLineCap="round"`;
  - cuerpo: `M31,56a21,21 0 0,0 42,0` (semicírculo inferior, centro (52,56)) con el mismo trazo;
  - vaho: `M44,44c0,-7 4,-9 4,-16` y `M56,44c0,-7 4,-9 4,-16` con
    `strokeColor="@color/sazon_launcher_accent"`, `strokeWidth="5"`, `strokeLineCap="round"`.
  Se borran el `aapt:attr`/`<gradient>` y el robot. La geometría exacta puede ajustarse dentro de la
  paleta y la zona segura; el acabado fino lo revisa la persona.
- `drawable/ic_launcher_monochrome.xml` (crear): **misma geometría** que el `foreground` con
  `strokeColor="#000000"` (excepción documentada: el sistema sustituye el color por alfa/el tinte
  tematizado). Sin rellenos a color.
- `mipmap-anydpi/ic_launcher.xml` y `ic_launcher_round.xml` (modificar): `background` →
  `ic_launcher_background`, `foreground` → `ic_launcher_foreground`, `monochrome` →
  `ic_launcher_monochrome`. Se mantiene el nombre de la carpeta (`mipmap-anydpi/`).

### 2. Splash de marca (Android 12+, sin dependencias)

- `drawable/ic_splash_mark.xml` (crear): el mismo motivo a mayor escala relativa (el splash lo dibuja
  enmascarado en círculo), vector 108×108 con borde `M20,60h68` y cuerpo `M22,62a32,32 0 0,0 64,0`
  (`strokeWidth="8"`) en `@color/sazon_launcher_motif`, vaho `M46,48c0,-9 6,-12 6,-22` /
  `M62,48c0,-9 6,-12 6,-22` (`strokeWidth="6.5"`) en `@color/sazon_launcher_accent`. Sin fondo propio.
- `res/values-v31/themes.xml` (**crear**) y `res/values-night-v31/themes.xml` (**crear**): cada uno
  **redeclara `Theme.Sazon` completo** (parent `android:Theme.Material.Light.NoActionBar` /
  `android:Theme.Material.NoActionBar`), repitiendo `android:windowBackground` y
  `android:forceDarkAllowed=false` y añadiendo
  `android:windowSplashScreenBackground="@color/sazon_window_background"` y
  `android:windowSplashScreenAnimatedIcon="@drawable/ic_splash_mark"`.
  **Por qué las dos**: en la precedencia de qualifiers, *night* va antes que *platform version*, así que
  en un API 31+ en modo oscuro se elige `values-night/` (o `values-night-v31/`) por delante de
  `values-v31/`: sin la variante nocturna el splash perdería los atributos de marca en oscuro.
  **No** se declara `windowSplashScreenIconBackgroundColor` (decisión).
- `values/themes.xml` y `values-night/themes.xml` **no cambian** (siguen sin atributos de splash: son
  irrelevantes en API 31+ y no rompen nada en un sistema previo).

### 3. Tests JVM (contrato verificable)

- `app/src/test/java/com/sazon/app/ui/theme/AppResourceFiles.kt` (crear, `internal`): lee
  `android.manifest.path` y `android.res.dir` (propiedades de sistema) y resuelve rutas bajo `res/`.
  Parseo XML **sin dependencias nuevas**: `javax.xml.parsers` del JDK (los tests JVM corren sobre la
  JDK; el `android.jar` stub no intercepta `javax.*`) o, si se prefiere, un escáner de texto mínimo al
  estilo de `DesignFrontmatter` (los ficheros son pequeños y planos). **Nunca** `android.util.Xml`/
  `android.*` (stub en test JVM sin Robolectric).
- `app/src/test/java/com/sazon/app/ui/theme/LauncherIconTest.kt` (crear; paquete de
  `DesignFrontmatter.kt`, que reutiliza para la paleta de `DESIGN.md`): Scenarios 1, 2, 3 y 6.
- `app/src/test/java/com/sazon/app/ui/theme/SplashThemeTest.kt` (crear): Scenarios 4, 5 y 6.
- `app/build.gradle.kts` añade junto a las propiedades existentes:
  `systemProperty("android.manifest.path", layout.projectDirectory.file("src/main/AndroidManifest.xml")
  .asFile.absolutePath)` y `systemProperty("android.res.dir", layout.projectDirectory.dir("src/main/res")
  .asFile.absolutePath)`.

## Expected File Changes

**Crear**

- `app/src/main/res/drawable/ic_launcher_monochrome.xml` — silueta monocroma propia.
- `app/src/main/res/drawable/ic_splash_mark.xml` — marca del splash (vector, sin fondo).
- `app/src/main/res/values-v31/themes.xml` y `app/src/main/res/values-night-v31/themes.xml` —
  `Theme.Sazon` con `windowSplashScreenBackground` + `windowSplashScreenAnimatedIcon`.
- `app/src/test/java/com/sazon/app/ui/theme/AppResourceFiles.kt`, `LauncherIconTest.kt`,
  `SplashThemeTest.kt` — tests JVM del icono y del splash.

**Modificar**

- `app/src/main/res/drawable/ic_launcher_background.xml` — fondo cálido de marca por token.
- `app/src/main/res/drawable/ic_launcher_foreground.xml` — cuenco lineal humeante.
- `app/src/main/res/mipmap-anydpi/ic_launcher.xml` y `ic_launcher_round.xml` — `monochrome` propio.
- `app/src/main/res/values/colors.xml` y `values-night/colors.xml` — colores del icono.
- `app/build.gradle.kts` — solo las dos propiedades de sistema de test.
- `DESIGN.md` — sección `## App Icon`, pregunta abierta 2 resuelta y excepción de capturas.
- `docs/technical-discovery.md`, `docs/risks-and-open-questions.md` (ver abajo).
- `PROGRESS.md` y `feature_list.json` — **al implementar** (estado y evidencia), no ahora.

**Borrar**

- `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher{,_round}.webp` (10 ficheros;
  muertos con `minSdk 33`). **Contingencia**: si al ejecutar `lint` aparece un aviso nuevo bajo
  `app/src` provocado por la ausencia de densidades (familia `IconMissingDensityFolder`), se
  **conservan los rasters** y se registra la decisión (un warning nuevo es peor que bytes muertos).

**No se toca**: `gradle/libs.versions.toml`, `init.sh`, `scripts/**`, `AndroidManifest.xml`
(solo se lee), `values/themes.xml`, `values-night/themes.xml`, `strings.xml`, `ui/**`, `domain/**`,
`data/**`, `app/src/main/assets/**`, `docs/design/**`, `docs/adr/**`, `app/src/androidTest/**`.

## Visual Design Impact

- UI involved: **sí** (icono del lanzador/recientes/Ajustes y pantalla de arranque). Ninguna pantalla
  de la app cambia.
- Design source: **`DESIGN.md`** (frontmatter `colors`, `## Colors`, `## Components`,
  `## Accessibility Baseline`). Esta feature **completa** `DESIGN.md` con la sección `## App Icon`, que
  pasa a ser la dirección visual del icono: nombre definitivo **Sazón**, motivo cuenco lineal humeante,
  `background` `#FFF8F0` (claro) / `darkBackground` `#241C16` (oscuro) en la tesela, `primary` `#D9552B`
  en el trazo y `accent` `#F2A93B` en el vaho, contenido dentro de la zona segura de 66/108 y capa
  `monochrome` para iconos tematizados.
- Screens or states affected: icono adaptativo (cuadrado y redondo), icono monocroma tematizado, icono
  en claro y oscuro, splash de arranque en claro y oscuro.
- New design artifact required: **no**. **Excepción de capturas** (misma clase que #77/#85/#86/#91): no
  se crea `docs/design/concepts/app-launcher-icon/` — un icono de lanzador no es una pantalla y la
  dirección queda registrada en `DESIGN.md`.
- Sin tokens nuevos, sin cambios tipográficos, sin estados de pantalla nuevos. `DESIGN.md` manda ante
  cualquier contradicción.

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — fila del recurso (icono de lanzador y splash: vectores XML,
  `values-night` para el fondo, `values-v31`/`values-night-v31` para el splash, sin dependencias) +
  párrafo y bullet en "Testing and Verification" (`LauncherIconTest`/`SplashThemeTest` leen
  `android.manifest.path` y `android.res.dir`).
- `DESIGN.md`: **update** — nueva sección `## App Icon` **inmediatamente antes de `## Core Screens`**
  (tras `## Components`); pregunta abierta 2 marcada como **resuelta** (2026-10-15,
  `app-launcher-icon`); párrafo de **excepción de capturas** en `Generated Concept Images`.
- `docs/risks-and-open-questions.md`: **update (una línea)** — el icono del lanzador lo resuelve el
  launcher: no todos re-resuelven la variante nocturna ni refrescan su caché de iconos, así que el
  acabado (y que el fondo oscuro se vea en oscuro) lo revisa la persona en el emulador.
- `docs/domain-model.md`: **not needed** — no cambian entidades, relaciones ni estados.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas no cambian (`init.sh` intacto).
- `docs/build-brief.md`: **not needed** — el icono no altera alcance ni non-goals del MVP.
- `docs/adr/**`: **not needed** — no hay decisión arquitectónica nueva (no hay dependencia ni capa).
- `docs/design/concepts/`: **no change** — excepción autorizada; no se crea carpeta.
- `ARCHITECTURE.md` / `CONSTRAINTS.md`: **not needed** — el repositorio los omite por decisión del
  harness.

## Implementation Plan

1. Añadir los colores del icono a `values/colors.xml` y `values-night/colors.xml`.
2. Reescribir `ic_launcher_background.xml` y `ic_launcher_foreground.xml`; crear
   `ic_launcher_monochrome.xml`; apuntar `monochrome` de las dos variantes a la capa propia.
3. Crear `ic_splash_mark.xml` y los dos temas de splash (`values-v31`, `values-night-v31`).
4. Borrar los 10 rasters de plantilla y comprobar que no queda ningún color/geometría de plantilla.
5. Añadir las dos propiedades de sistema en `app/build.gradle.kts` y escribir
   `AppResourceFiles.kt`, `LauncherIconTest` y `SplashThemeTest` (con el caso negativo).
6. Actualizar `DESIGN.md` (sección, OQ2 resuelta, excepción de capturas), `docs/technical-discovery.md`
   y `docs/risks-and-open-questions.md`.
7. Ejecutar `bash init.sh` y registrar la evidencia observada (+ estado) en `feature_list.json` y
   `PROGRESS.md`.

## Implementation Tasks

- [ ] Añadir `sazon_launcher_background`/`sazon_launcher_motif`/`sazon_launcher_accent` a
      `values/colors.xml` y el fondo nocturno a `values-night/colors.xml`.
- [ ] Reescribir los dos drawables de plantilla (fondo por token + cuenco lineal humeante) y crear
      `ic_launcher_monochrome.xml`.
- [ ] Apuntar `background`/`foreground`/`monochrome` de `ic_launcher.xml` y `ic_launcher_round.xml`.
- [ ] Crear `ic_splash_mark.xml` y `values-v31/themes.xml` + `values-night-v31/themes.xml`.
- [ ] Borrar los 10 `.webp` de `mipmap-*dpi/` (o documentar la contingencia de lint si aparece).
- [ ] Añadir `android.manifest.path` y `android.res.dir` a `app/build.gradle.kts`.
- [ ] Escribir `AppResourceFiles.kt`, `LauncherIconTest` (escenarios 1-3 y 6) y `SplashThemeTest`
      (escenarios 4-6) en `com.sazon.app.ui.theme`, reutilizando `designMarkdown()`.
- [ ] Actualizar `DESIGN.md`, `docs/technical-discovery.md` y `docs/risks-and-open-questions.md`.
- [ ] Ejecutar `bash init.sh` y registrar la evidencia (test/lint + inspecciones) y el estado.

## Verification Plan

- `bash init.sh` (WSL; enruta `./gradlew test` + `./gradlew lint` por `scripts/gradle.sh`) → **esperado**:
  exit 0, sin procesos largos. `init.sh` **no cambia** y sigue siendo el gate no bloqueante (no arranca
  emulador ni servidor). Se esperan **2 suites nuevas** (`LauncherIconTest`, `SplashThemeTest`) y
  ninguna regresión; el implementer registra el **conteo observado**, no uno heredado.
- `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.theme.*"` (tirada focal) →
  **esperado**: las suites de tokens y las dos nuevas en verde.
- `./gradlew lint` → **esperado**: 0 errores, 13 warnings preexistentes y **0 nuevos bajo `app/src`**.
  Si aparece un aviso nuevo de densidades de icono, se aplica la contingencia (conservar rasters) y se
  registra.
- **Inspección/grep** (evidencia objetiva):
  - `find app/src/main/res -name '*.webp' -o -name '*.png' -o -name '*.jpg'` → **0** ficheros.
  - `grep -rn '3DDC84\|33FFFFFF\|aapt:attr\|M65.3,45.828' app/src/main/res app/src/main/AndroidManifest.xml`
    → **0** coincidencias.
  - `grep -rn 'windowSplashScreen' app/src/main/res` → solo `values-v31/themes.xml` y
    `values-night-v31/themes.xml` (fondo + icono; **sin** `IconBackgroundColor`).
  - `git status --porcelain` → solo `.xml`, `.kt` y `.md` (ningún binario nuevo).
- **Manual (emulador; lo revisa la persona, AGENTS.md no automatiza UI y el `adb` de WSL no ve el
  emulador)**: en claro y oscuro, el icono del cajón/recientes/Ajustes es el de Sazón en forma cuadrada
  y redonda; con iconos tematizados activados el monocromo se ve correcto; el splash de arranque
  muestra la marca sobre el fondo cálido.
- Cobertura E2E: **no aplica** — no hay suite E2E persistente, el cambio no toca autenticación,
  autorización, routing ni flujos de API, y la verificación canónica del repo para recursos es el test
  JVM + `DESIGN.md`.

## Evidence To Capture

- Salida de `bash init.sh` (exit 0) con el nº de suites/tests observado, 0 fallos, y el informe de lint
  (0 errores, warnings preexistentes, 0 bajo `app/src`) — o la contingencia de rasters si se aplicó.
- Salida de las 2 suites nuevas y de su caso negativo (no pasa en vacío).
- Salidas de los grep/`find` del plan de verificación y `git status --porcelain` (sin binarios nuevos).
- `git diff --stat` dentro del alcance previsto (incluidas las 10 bajas del `.webp`).
- Diff de `DESIGN.md` (sección `## App Icon`, OQ2 resuelta, párrafo de excepción) y de las dos docs.
- Resultado manual en emulador (descripción): icono cuadrado/redondo/tematizado y splash, en claro y
  oscuro.
- Anotación en `feature_list.json` (evidencia + `passing` al implementar).

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **Vectores XML y no assets binarios**: lo pide la entrada de la feature y es coherente con
  `AGENTS.md`/ADR 0006 (no se añaden imágenes); además evita el relleno de densidades y deja el icono
  versionado como texto revisable.
- **Fondo con `values-night`**: el fondo cálido de marca en claro y `darkBackground` en oscuro es lo que
  pide la entrada; se implementa con **color de recurso** (no hex literal) para que siga siendo un token
  de `DESIGN.md`.
- **`values-night-v31` además de `values-v31`**: por la precedencia de qualifiers (*night* antes que
  *platform version*), `values-night/` ganaría a `values-v31/` y el splash perdería los atributos de
  marca en modo oscuro. Es la decisión técnica que evita ese fallo silencioso.
- **Sin `windowSplashScreenIconBackgroundColor`**: la marca va directamente sobre el fondo cálido; una
  placa circular detrás no está en la identidad y añadiría riesgo visual.
- **Sin `androidx.core.splashscreen`**: `minSdk 33` garantiza los atributos de plataforma (API 31+); la
  librería solo aporta compatibilidad hacia atrás que aquí no se necesita.
- **`#000000` en la capa monocroma**: excepción documentada y acotada a `ic_launcher_monochrome.xml`
  (el sistema usa alfa/tinte); el test la permite solo ahí.
- **Se conserva `mipmap-anydpi/`** (la nota de la feature dice `-v26`): renombrar no cambia nada con
  `minSdk 33` y sería churn; la carpeta real es `mipmap-anydpi/`.
- **`DESIGN.md` como registro**: la dirección del icono no está hoy en ningún sitio; va a la sección
  nueva `## App Icon` y la OQ2 se cierra ahí, para que un validador no la lea como improvisación.

### Unknowns

- **Re-resolución nocturna del icono**: el icono lo dibuja el launcher; no todos re-resuelven la
  variante `-night` ni invalidan su caché de iconos al cambiar el tema. El fondo oscuro puede no verse
  en algunos launchers; es revisión manual de la persona y, si no se ve, la corrección es una feature
  correctiva (posible fondo único) sin reabrir esta.
- **`IconMissingDensityFolder` al borrar los rasters**: no se ha podido comprobar en esta sesión de
  planificación (no se ejecuta el build). Contingencia explícita en `Expected File Changes`/Verification.
- **Acabado fino del motivo**: la geometría propuesta (cuenco + vaho) es determinista y está dentro de
  la zona segura, pero el encaje óptico a 48–192 dp (y en la máscara redonda) lo juzga la persona; las
  coordenadas pueden ajustarse dentro de la paleta y la zona segura.
- **Landing visual del splash**: la marca se muestra enmascarada en círculo y a tamaño fijo por el
  sistema; si el trazo se ve fino o grueso a ese tamaño, se ajusta la escala relativa del vector (mismo
  contrato de test).

## Validator Checklist

- [ ] El icono adaptativo declara `background`, `foreground` y `monochrome` en las **dos** variantes y
      los tres drawables existen como vectores XML; el manifiesto sigue declarando `icon`/`roundIcon`
      (Scenario 1).
- [ ] Todo color de los vectores resuelve a un token de `DESIGN.md` (fondo claro/oscuro, motivo
      `primary`, acento `accent`), con la única excepción documentada de `#000000` en la capa monocroma;
      el `foreground` usa motivo **y** acento (Scenario 2).
- [ ] No queda ningún rastro del icono de plantilla (rasters, `#3DDC84`, rejilla `#33FFFFFF`,
      `strokeWidth="0.8"`, robot `M65.3,45.828`, `<gradient>`/`aapt:attr`) y no se añade ningún asset
      binario (Scenario 3).
- [ ] `values-v31` **y** `values-night-v31` declaran `windowSplashScreenBackground` y
      `windowSplashScreenAnimatedIcon`, el drawable es un vector con la paleta, y no se declara
      `windowSplashScreenIconBackgroundColor` (Scenario 4).
- [ ] `values/themes.xml` y `values-night/themes.xml` siguen fijando `windowBackground` y
      `forceDarkAllowed=false`, y las variantes v31 repiten esos items (Scenario 5).
- [ ] Los tests incluyen el caso negativo y **no** pasan en vacío (Scenario 6); el conteo de tests es el
      **observado** en el gate, no un número heredado.
- [ ] `DESIGN.md` gana `## App Icon`, cierra la pregunta abierta 2 y registra la excepción de capturas;
      `docs/technical-discovery.md` y `docs/risks-and-open-questions.md` quedan actualizados
      (Scenario 7).
- [ ] `bash init.sh` en verde (test + lint, 0 warnings nuevos bajo `app/src`), sin dependencias nuevas
      (`gradle/libs.versions.toml` intacto) y con `init.sh` sin cambios (Scenario 8).
- [ ] `feature_list.json` y `PROGRESS.md` quedan actualizados con evidencia verificable por el
      implementer/orquestador (nunca por el planner).
