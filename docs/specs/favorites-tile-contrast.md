# Feature Implementation Spec: La miniatura de la lista de Favoritos resalta del fondo

## Source Feature

- `id`: `favorites-tile-contrast`
- `area`: `ui`
- `depends_on`: `["favorites", "recipe-card-typographic"]` (ambas `accepted`)
- `status`: `not_started` — esta spec es el contrato planner → implementer → validator. El planner
  **no** modifica `feature_list.json` ni `PROGRESS.md`.
- `source`: `feature_list.json` (issue **#130**)
- Referencias de diseño: `DESIGN.md` (frontmatter `components.recipe-tile-favorites`, `Components` →
  RecipeListCard, `Core Screens` 6, `Accessibility Baseline`) y la captura **ya aceptada**
  `docs/design/concepts/favorites/favoritos/`.
- **Exención de capturas autorizada** (2026-10-14, issue #130; misma clase que #77/#85/#86/#91/#98/
  #101/#110, registrada en `DESIGN.md` en el bloque *Excepción de capturas*, issues #129–#132): es una
  **correctiva de color** contra `DESIGN.md` y la captura aceptada `favorites/favoritos`, así que
  **no** se exige ni se crea `docs/design/concepts/favorites-tile-contrast/`. El tercer criterio de
  `verification` ("las capturas de referencia de `docs/design/concepts/favorites-tile-contrast/`
  respetadas") queda **reconciliado por esa exención**: la carpeta debe **no existir** y las capturas
  aceptadas deben quedar **sin modificar**.
- La dirección vigente ya está escrita en `DESIGN.md` (revisión de la persona del 2026-10-14, commit
  `45203e2`): `backgroundColor: {colors.primary}`, `textColor: {colors.onPrimary}` y **sin** la clave
  `accentRule`. Esta feature **implementa** esa decisión; no la reabre.

## Goal

En la pestaña **Favoritos**, la **miniatura cuadrada 96×96 (esquina 24) de la izquierda de cada fila**
deja de pintarse sobre `surfaceVariant` —casi indistinguible del fondo blanco de la tarjeta
(**1,21:1** en claro; **1,04:1** contra `darkCardSurface` en oscuro)— y pasa a fondo **`primary`**
(terracota `#D9552B`) con el **monograma en `onPrimary`** (`#FFFFFF`). Con eso la miniatura **resalta**
del fondo de la tarjeta en **claro y en oscuro**: **3,97:1** y **3,31:1**, ambos ≥ **3:1** (WCAG 1.4.11,
contraste de elementos no textuales).

La geometría de la miniatura (**96×96**, esquina **24**) y el resto de la fila (**título**, **meta** en
cifras tabulares, **corazón**, superficie/borde de la tarjeta y swipe "Eliminar") **no cambian**. La
regla terracota de **5 px a la izquierda** que llevaba el tile se **retira**: sobre fondo `primary`
quedaba a **1,00:1** (invisible), y el frontmatter de `DESIGN.md` ya dejó de declarar `accentRule`.

## Non-Goals

- **No** se toca la **rejilla** de Favoritos (`FavoriteRecipeTile` / `RecipeFicha`), ni el buscador, ni
  el orden, ni el toggle lista/rejilla, ni el swipe, ni la tarjeta horizontal en sí (`Surface`, color
  `card.surface`, borde oscuro, forma `SazonShapes.large`).
- **No** se toca el **corazón** (`HeartButton`: `surfaceVariant` + glifo `primary`) ni el **fondo del
  swipe** (`primary` / `onPrimary`).
- **No** se toca el **estado vacío** de Favoritos (`FavoritesScreen.kt` líneas ~325-331: un cuadrado
  decorativo de 96 dp sobre `surfaceVariant` con el glifo de marcador en `primary`). **No** es la
  miniatura de una fila (no hay tarjeta detrás) y la persona pidió la miniatura de la fila; si se
  quiere alinear, es otra correctiva.
- **No** se toca el **monograma en sí** (`ui/home/RecipeMonogram`, lógica pura) ni su tipografía
  (Manrope Bold 30sp) ni su tamaño/line-height/tracking.
- **No** se tocan los tokens del tema (`ui/theme/Color.kt`, `CardTokens.kt`, `CardImageOverlay.kt`,
  `Spacing.kt`, `Shape.kt`) ni se añaden tokens nuevos: la miniatura consume roles que **ya existen**
  (`primary` / `onPrimary`).
- **Sin** dependencias nuevas; **sin** cambios en `app/build.gradle.kts`, `gradle/**`, `init.sh`,
  `scripts/**`, `assets/**`, `AndroidManifest.xml`, `domain/**`, `data/**`.
- **No** se crean ni modifican capturas bajo `docs/design/concepts/**` (exención de #130); en
  particular **no** se crea `docs/design/concepts/favorites-tile-contrast/`.
- **No** se cambia ninguna regla de verificación ni el gate (`init.sh`).

## Job Story

Cuando abro la pestaña Favoritos (en claro o en oscuro) y hojeo la lista,
quiero que la miniatura de cada receta se distinga de un vistazo del fondo de la fila,
para reconocer cada guardada como un ítem con identidad propia y no como una mancha pálida pegada a la
tarjeta.

## Users And Permissions

- **Usuario local**: ve la lista de Favoritos en claro y en oscuro. No hay permisos ni interacción
  nueva; el tap abre el Detalle y el corazón/quitar siguen igual.
- **Desarrollador / agente**: cambia dos roles de color en un composable privado, retira una regla
  muerta y verifica el contrato de contraste en **JVM** (sin emulador, sin red, sin Robolectric).
- **Persona**: revisa el acabado en el emulador tras el merge (AGENTS.md no automatiza UI), contra
  `DESIGN.md` y la captura aceptada `favorites/favoritos`.

## Acceptance Scenarios

### Scenario 1: la miniatura resalta en claro

Given el tema **claro** y una receta favorita con título con iniciales (p. ej. "Arroz con pollo"),
When se pinta `FavoriteRecipeCard` en la lista de Favoritos,
Then la miniatura 96×96 (esquina 24) tiene fondo `MaterialTheme.colorScheme.primary` (`#D9552B`) y el
monograma (Manrope Bold 30sp) se pinta en `MaterialTheme.colorScheme.onPrimary` (`#FFFFFF`); **no**
queda ningún trazo de `surfaceVariant` **dentro** del tile ni la regla de 5 px; el ratio miniatura vs
superficie de la tarjeta (`SazonCardTokens.light.surface` = `#FFFFFF`) es **3,97:1 ≥ 3,0**; el título,
la meta y el corazón siguen con el mismo color, tamaño y posición.

### Scenario 2: la miniatura resalta en oscuro

Given el tema **oscuro** (tarjeta `darkCardSurface` `#3A2E26` + borde `darkCardOutline`) y una receta
favorita,
When se pinta la misma fila,
Then los **mismos** roles `primary` / `onPrimary` dan un ratio miniatura vs superficie de la tarjeta de
**3,31:1 ≥ 3,0** (WCAG 1.4.11), sin que cambie ningún token del modo oscuro (la superficie y el borde
de la tarjeta son los del ADR 0006 y siguen intactos).

### Scenario 3: monograma ausente o vacío (caso negativo)

Given una receta cuyo título no produce iniciales (`RecipeMonogram.of(titulo) == null`: vacío, solo
conectores o sin letras),
When se pinta la fila,
Then el tile sigue siendo un cuadrado sólido `primary` **sin** texto, sin excepción y sin `Text` vacío;
el contraste del bloque contra la tarjeta sigue siendo **3,97:1 / 3,31:1** (el color no depende de que
haya monograma) y el resto de la fila no cambia.

### Scenario 4: contrato de contraste testeado en JVM (y el caso negativo del fondo anterior)

Given la implementación terminada,
When se ejecutan los tests, el que pinna el contrato (nuevo `FavoriteTileContrastTest`) comprueba con la
fórmula WCAG 2.x, sin Android ni Compose runtime:
Then

| Par medido | Hex | Ratio | Umbral |
|---|---|---|---|
| miniatura claro vs tarjeta claro | `primary` #D9552B vs `SazonCardTokens.light.surface` #FFFFFF | **3,9738** (≈3,97) | ≥ 3,0 |
| miniatura oscuro vs tarjeta oscura | `primary` #D9552B vs `SazonCardTokens.dark.surface` #3A2E26 | **3,3056** (≈3,31) | ≥ 3,0 |
| monograma sobre el tile | `SazonOnPrimary` #FFFFFF vs `SazonPrimary` #D9552B | **3,9738** (≈3,97) | ≥ 3,0 (texto **grande**: 30sp Manrope Bold) |
| **(negativo)** fondo anterior claro | `SazonSurfaceVariant` #F2E8DE vs #FFFFFF | **1,2088** (≈1,21) | **< 3,0** |
| **(negativo)** fondo anterior oscuro | `SazonDarkSurfaceVariant` #3D3129 vs #3A2E26 | **1,0445** (≈1,04) | **< 3,0** |

y además que los roles que consume el tile son los medidos: `LightColorScheme.primary == SazonPrimary`
(`#D9552B`) y `onPrimary == SazonOnPrimary` (`#FFFFFF`), idénticos en `DarkColorScheme`, y
`SazonPrimary != SazonSurfaceVariant`. Los dos vectores negativos documentan el defecto: la métrica no
puede pasar en vacío.

### Scenario 5: geometría y resto de la fila intactos; sin código muerto

Given la implementación terminada,
When se inspecciona el diff de `ui/favorites/FavoriteRecipeCard.kt`,
Then la miniatura sigue siendo `96.dp` con `SazonShapes.medium` (24), el monograma conserva
`MONOGRAM_STYLE` (Manrope Bold, 30sp/34sp, tracking −0,04em) y su centrado; **desaparecen**
`TILE_ACCENT_RULE` y el `Box` del filete (`grep -rn "TILE_ACCENT_RULE" app/src` → **0**); `FavoriteText`
(título `titleMedium` `onSurface`, meta `labelSmall` tabular `onSurfaceVariant`), `HeartButton`,
`DismissBackground`, el `Surface` de la tarjeta y el `SwipeToDismissBox` quedan **idénticos**.

### Scenario 6: disciplina de alcance, pureza y sin capturas nuevas

Given la implementación terminada,
When se inspecciona el diff,
Then solo cambian los ficheros esperados (sección *Expected File Changes*); `ui/theme/**` (ningún token
nuevo), `ui/home/RecipeMonogram.kt`, `ui/favorites/FavoritesScreen.kt`, la rejilla, `data/**`,
`domain/**`, `assets/**`, build/lockfile, `init.sh` y `scripts/**` están **intactos** ⏤ 0 hex sueltos
nuevos en `ui/favorites` (los valores viven en `ui/theme`) y **no** existe
`docs/design/concepts/favorites-tile-contrast/`; las capturas aceptadas de `docs/design/concepts/**`
(que aún muestran el tile `surfaceVariant` con la regla de 5 px) quedan **sin tocar**.

### Scenario 7: gate verde y sin dependencias nuevas

Given la implementación terminada,
When se ejecuta `bash init.sh`,
Then `./gradlew test` y `./gradlew lint` terminan en **verde** (exit 0), sin fallos/errores/saltados en
los XML de test, sin hallazgos de lint nuevos bajo `app/src` y con `git diff --name-only` sin cambios en
build/lockfile ni dependencias.

## Repository Research

### Files Inspected

- `feature_list.json` — entrada `favorites-tile-contrast` (líneas 1379-1393: `user_visible_behavior`,
  `verification` de 3 puntos, `notes` con la decisión `primary`/`onPrimary` y la exención de #130) y las
  entradas `accepted` de `favorites` y `recipe-card-typographic`.
- `app/src/main/java/com/sazon/app/ui/favorites/FavoriteRecipeCard.kt` — **el** fichero a cambiar:
  `TILE_SIZE = 96.dp` (l. 53), `TILE_ACCENT_RULE = 5.dp` (l. 56), `MONOGRAM_STYLE` y su KDoc "sobre
  `surfaceVariant`" (l. 58-65), `RecipeMonogramTile` (l. 172-197: `.background(scheme.surfaceVariant)`
  en l. 179, el `Box` del filete en `scheme.primary` en l. 182-188, el monograma en
  `scheme.onSurfaceVariant` en l. 189-195) y su KDoc (l. 166-170); `FavoriteText` (l. 199-225),
  `HeartButton` (l. 227-244) y `DismissBackground` (l. 246-264) como lo que **no** cambia.
- `app/src/main/java/com/sazon/app/ui/theme/Color.kt` — `SazonPrimary` `#D9552B` (l. 13),
  `SazonOnPrimary` `#FFFFFF` (l. 14), `SazonSurfaceVariant` `#F2E8DE` (l. 47), `SazonDarkSurfaceVariant`
  `#3D3129` (l. 64) y los esquemas: `primary`/`onPrimary` **idénticos** en claro (l. 79-80) y oscuro
  (l. 122-123), con `surfaceVariant` distinto en cada variante (l. 96 y l. 139).
- `app/src/main/java/com/sazon/app/ui/theme/CardTokens.kt` — `SazonCardTokens.light.surface` =
  `SazonSurface` `#FFFFFF` (l. 48-52) y `SazonCardTokens.dark.surface` = `SazonDarkCardSurface`
  `#3A2E26` (l. 55-59): las **superficies de la tarjeta** contra las que se mide la miniatura.
- `app/src/main/java/com/sazon/app/ui/theme/Shape.kt` — `SazonShapes.medium = RoundedCornerShape(24.dp)`
  (l. 14): la "esquina 24" que se conserva.
- `app/src/test/java/com/sazon/app/ui/theme/CardTokensTest.kt` — **el patrón** a seguir: test JVM puro
  con `Color`/`toArgb`, `relativeLuminance(hex)`, `contrastRatio(a, b)`, `toHex()`, aserciones
  `>= 3.0` + valor exacto ±0.05 y un test que documenta el defecto anterior para que la métrica **no**
  pase en vacío (l. 27-55, 139-180).
- `app/src/test/java/com/sazon/app/ui/theme/DesignTokensTest.kt` — los tests del paquete `ui.theme`
  acceden a los esquemas `internal` `LightColorScheme`/`DarkColorScheme` (l. 36, 76, 140-171) y validan
  el frontmatter de `DESIGN.md`; `frontmatterSectionMap` solo lee secciones planas (`colors`, `spacing`,
  `rounded`, `typography`, l. 245-264), así que **no** valida `components`.
- `app/src/test/java/com/sazon/app/ui/favorites/FavoritesListTest.kt` — patrón de test del paquete
  `ui.favorites` (lógica pura; no cubre la capa Compose).
- `app/src/main/java/com/sazon/app/ui/home/RecipeMonogram.kt` — lógica pura del monograma
  (`of(title): String?`, `null` sin palabras significativas): **no** cambia.
- `app/src/main/java/com/sazon/app/ui/favorites/FavoritesScreen.kt` — l. 315-338: el cuadrado de 96 dp
  del **estado vacío** sobre `surfaceVariant` (fuera de alcance, ver Non-Goals).
- `DESIGN.md` — frontmatter `components.recipe-tile-favorites` (l. 82-86) ya declara
  `size: "96x96"`, `backgroundColor: "{colors.primary}"`, `textColor: "{colors.onPrimary}"`, `content:
  "monograma de iniciales del plato"` y **sin** `accentRule`; `Components` → RecipeListCard (l. 347-349)
  ya describe la miniatura sobre `primary` con "≈3,97:1 / ≈3,31:1"; `Core Screens` 6 (l. 507-510) ya lo
  dice; *Excepción de capturas* (l. 241-262) nombra esta feature; `Accessibility Baseline` (l. 527-538)
  solo documenta el 3,26:1 del borde de tarjeta.
- `docs/adr/0006-typographic-recipe-cards.md` — **§6** (l. 97-103) dice hoy "monograma de iniciales
  sobre `surfaceVariant` con la **regla terracota** (`primary`) **de 5px a la izquierda** (`DESIGN.md` →
  `components.recipe-tile-favorites.accentRule`)": queda obsoleto.
- `docs/technical-discovery.md` — l. 236-237: "la miniatura 96×96 de la **lista** de Favoritos es un
  **monograma** de iniciales sobre `surfaceVariant` con la **regla terracota de 5px** a la izquierda":
  queda obsoleto.
- `docs/design/concepts/typographic-cards/prototipo-c3.html` (l. 121-122, 568) y las capturas
  `recipe-card-typographic/{recipe-card,favoritos-lista}` y `card-typographic-tokens-and-design/
  tarjeta-oscura` — muestran el tile `surface-variant` + `border-left:5px solid primary` (referencia
  **histórica**, de solo lectura).
- `docs/design/concepts/favorites/favoritos/` — captura aceptada de la pantalla Favoritos
  (dirección visual vigente y referencia de esta correctiva).
- `docs/specs/recipe-card-typographic.md` (l. 66-69) y `docs/specs/recipe-card-l2-unified.md` (l. 43-45,
  122) — contratos aceptados que nombran la miniatura y remiten el color a **esta** feature.

### Existing Patterns To Follow

- **Los tokens viven en `ui/theme`; los composables consumen roles** (`MaterialTheme.colorScheme`), con
  **0 hex sueltos** fuera de `ui/theme` (`FavoriteRecipeCard.kt` ya no declara ningún hex).
- **Tests de contraste JVM puros** con la fórmula WCAG 2.x y valores exactos ±0.05, más un vector
  negativo que documenta el defecto previo (`CardTokensTest`).
- **El frontmatter de `DESIGN.md` es la fuente de verdad**: `DesignTokensTest` compara el documento con
  el tema, así que la implementación debe **coincidir** con lo ya declarado.
- **Correctiva de color = cambio mínimo + evidencia**: mismo patrón que `favorites-sort-controls`
  (issue #85), `profile-stats-heading` (#86) y `home-suggestions-personalized` (#110), incluida la
  exención de capturas registrada en `DESIGN.md`.
- **Verificación canónica**: `bash init.sh` (`./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`); **nunca** `./gradlew` directo ni `./gradlew --stop`.

### Current Gaps

- El tile pinta `scheme.surfaceVariant` + la regla `primary` de 5 px (`RecipeMonogramTile`), justo el
  fondo que `DESIGN.md` ya cambió a `primary` en la revisión del 2026-10-14: **código y diseño están
  desalineados** hasta que esta feature se implemente.
- No hay ningún test que pinne el contraste de la miniatura contra la superficie de la tarjeta: el
  único contrato de contraste de tarjeta es el de `CardTokensTest` (borde oscuro 3,26:1).
- Con `primary` como fondo, la regla terracota de 5 px queda a **1,00:1** (invisible): hoy es código
  muerto que el KDoc sigue describiendo como si se viera.
- `docs/adr/0006` §6 y `docs/technical-discovery.md` describen el contrato anterior.

## Technical Approach

1. **`RecipeMonogramTile` (`ui/favorites/FavoriteRecipeCard.kt`)**: fondo
   `MaterialTheme.colorScheme.primary` (en lugar de `scheme.surfaceVariant`) y monograma
   `MaterialTheme.colorScheme.onPrimary` (en lugar de `scheme.onSurfaceVariant`). **No** se añade ningún
   token nuevo: `primary`/`onPrimary` ya existen y son idénticos en claro y oscuro (`Color.kt`).
2. **Retirar la regla de 5 px** (`TILE_ACCENT_RULE` y su `Box` de `width(5.dp).fillMaxHeight()`), porque
   sobre `primary` es invisible (1,00:1) y `DESIGN.md` ya no declara `accentRule`. El `Box` exterior
   pierde `fillMaxHeight`/`width` y el import de `fillMaxHeight`/`width` solo si dejan de usarse (los
   usos restantes en el fichero son `Modifier.width(SazonSpacing.md/sm)` y `fillMaxWidth`, así que
   `width` **sí** sigue usándose; comprobar `fillMaxHeight` antes de quitar el import).
3. **KDoc reconciliado**: `MONOGRAM_STYLE` ("sobre `surfaceVariant`" → "sobre `primary`") y el KDoc de
   `RecipeMonogramTile` (superficie, esquina 24 y la retirada de la regla terracota; `RecipeMonogram`
   sigue igual). El KDoc de `FavoriteRecipeCard`/`FavoriteRecipeTile` no menciona el color: **no** hace
   falta tocarlo.
4. **Test JVM nuevo** `app/src/test/java/com/sazon/app/ui/theme/FavoriteTileContrastTest.kt` (paquete
   `com.sazon.app.ui.theme`, junto a `CardTokensTest`, para poder leer los esquemas `internal`), con los
   helpers WCAG copiados como privados de fichero (`toHex`, `relativeLuminance`, `contrastRatio`,
   `texto`) y los vectores del **Scenario 4**. `CardTokensTest` **no** se modifica.
5. **Docs durables**: `DESIGN.md` (contrato ya escrito; se completa con la retirada de la regla, el
   ratio en `Accessibility Baseline` y la limpieza de la coletilla de estado), `docs/technical-discovery.md`
   (párrafo de la miniatura) y `docs/adr/0006` §6 (nota de reconciliación).

**Invariante**: lo único que cambia es **el color del fondo del tile y el del monograma** (y la retirada
de un trazo invisible). Tamaño, esquina, tipografía, alineación, el resto de la fila, la rejilla, el
estado vacío, el corazón, el swipe y todos los tokens del tema quedan **iguales**. `DESIGN.md` manda: el
resultado debe coincidir con su frontmatter (`primary`/`onPrimary`), no al revés.

## Expected File Changes

- `app/src/main/java/com/sazon/app/ui/favorites/FavoriteRecipeCard.kt` — modificar; `RecipeMonogramTile`
  con `scheme.primary` + `scheme.onPrimary`, retirada de `TILE_ACCENT_RULE` y su `Box`, KDoc de
  `MONOGRAM_STYLE` y del tile reconciliados (imports ajustados si alguno queda sin uso).
- `app/src/test/java/com/sazon/app/ui/theme/FavoriteTileContrastTest.kt` — **crear**; contrato de
  contraste del Scenario 4 (4 tests ≈ +1 suite / +4 tests) con los helpers WCAG del patrón
  `CardTokensTest`.
- `DESIGN.md` — modificar (mínimo): en `Components` → RecipeListCard, anotar que la miniatura **no**
  lleva ya la regla terracota de 5 px (invisible sobre `primary`); en `Accessibility Baseline`, añadir el
  bullet del ≥3:1 de la miniatura (3,97:1 claro / 3,31:1 oscuro, `primary` `#D9552B`) y el del monograma
  `onPrimary` sobre `primary` (3,97:1, texto grande); en el bloque *Excepción de capturas*, quitar la
  coletilla "Las cuatro quedan `not_started`" (el estado vive en `feature_list.json`) manteniendo que
  **no** se crea `docs/design/concepts/<feature-id>/`. El frontmatter de
  `components.recipe-tile-favorites` ya declara `primary`/`onPrimary`: **confirmarlo** y no reescribirlo
  (si se añade alguna clave, que sea aditiva y solo dentro de esa sección de `components`, que
  `DesignTokensTest` no parsea).
- `docs/technical-discovery.md` — modificar; l. 236-237: la miniatura de la lista se pinta sobre
  `primary` con el monograma en `onPrimary` (≈3,97:1 claro / ≈3,31:1 oscuro, WCAG 1.4.11) y **sin** la
  regla de 5 px (retirada con `favorites-tile-contrast`, issue #130).
- `docs/adr/0006-typographic-recipe-cards.md` — modificar; §6 (l. 97-103): nota de reconciliación
  (2026-10-14, issue #130) — el fondo pasa a `primary` con el monograma en `onPrimary` y la regla
  terracota de 5 px se retira (era invisible sobre `primary`); la referencia a
  `components.recipe-tile-favorites.accentRule` deja de aplicar.
- `docs/design/concepts/**` — **not needed** (exención de capturas; solo lectura, no se crean carpetas).
- `feature_list.json`, `PROGRESS.md` — los actualiza el orquestador; el implementer **no** los toca.

## Visual Design Impact

- UI involved: **sí** (`area: ui`; cambia el color de un elemento visible de la lista de Favoritos).
- Design source: `DESIGN.md` (**manda**; frontmatter `components.recipe-tile-favorites` y `Components` →
  RecipeListCard ya declaran `primary`/`onPrimary` y los ratios) + la captura **ya aceptada**
  `docs/design/concepts/favorites/favoritos/` (dirección visual de la pantalla).
- Screens or states affected: **Favoritos** → pestaña **lista**, fila de cada favorito, en **claro** y
  **oscuro**, con y sin monograma (título sin iniciales). La rejilla y el estado vacío **no**.
- New design artifact required: **no**.
- **UI captures (NOT required — exención autorizada, issue #130)**: `DESIGN.md` (bloque *Excepción de
  capturas*, issues #129–#132) registra que esta feature **no** genera capturas nuevas y que **no** se
  crea `docs/design/concepts/favorites-tile-contrast/`. El implementer **no** crea ni modifica nada bajo
  `docs/design/concepts/**`. La dirección visual aplicable es `DESIGN.md` + la captura aceptada
  `docs/design/concepts/favorites/favoritos/`.
- Desviación declarada (no bloqueante): las capturas y prototipos aceptados
  (`typographic-cards/prototipo-c3.html` l. 121/568, `recipe-card-typographic/recipe-card`,
  `card-typographic-tokens-and-design/tarjeta-oscura`) dibujan todavía el tile `surface-variant` con
  `border-left:5px solid primary`. Son material **histórico de solo lectura** que esta correctiva
  **supersede** por decisión de la persona (2026-10-14) registrada en `DESIGN.md`; **no** se editan.
- Feature-specific visual states: **ninguno nuevo**; el color es el mismo en claro y en oscuro
  (`primary` `#D9552B` / `onPrimary` `#FFFFFF`) porque ambos esquemas comparten el rol.

## Durable Documentation Impact

- `DESIGN.md`: **update** — completar la sección `components.recipe-tile-favorites` / RecipeListCard y
  `Accessibility Baseline` con la retirada de la regla y el ratio ≥3:1 de la miniatura (el
  `primary`/`onPrimary` ya está declarado).
- `docs/technical-discovery.md`: **update** — el párrafo de la miniatura de Favoritos deja de citar
  `surfaceVariant` y la regla de 5 px.
- `docs/adr/0006-typographic-recipe-cards.md`: **update** — §6 reconciliado (fondo `primary`,
  `onPrimary`, regla de 5 px retirada).
- `docs/domain-model.md`: **not needed** — sin entidades, relaciones ni estados.
- `AGENTS.md`: **not needed** — no cambian flujo, arranque ni reglas del repo.
- `docs/design/concepts/**`: **not needed** — exención de capturas; nada que versionar aquí.
- `docs/specs/{recipe-card-typographic,recipe-card-l2-unified}.md`: **not needed** — son contratos
  aceptados que ya remiten el color de la miniatura a esta feature.

## Implementation Plan

1. Cambiar en `RecipeMonogramTile` los dos roles de color (`surfaceVariant` → `primary`,
   `onSurfaceVariant` → `onPrimary`).
2. Retirar la regla terracota de 5 px (`TILE_ACCENT_RULE` + su `Box`) y ajustar imports.
3. Reconciliar los KDoc (`MONOGRAM_STYLE` y `RecipeMonogramTile`).
4. Crear `FavoriteTileContrastTest` con los 4 tests y los vectores del Scenario 4 (incluidos los dos
   negativos) y ejecutar la tirada focal del paquete `ui.theme`.
5. Actualizar `DESIGN.md`, `docs/technical-discovery.md` y `docs/adr/0006` §6.
6. Ejecutar el gate, las inspecciones de alcance/pureza, registrar la evidencia y dejar la feature
   `passing` (la validación independiente es de otro rol; el commit lo crea el orquestador).

## Implementation Tasks

- [ ] `RecipeMonogramTile`: `.background(scheme.primary)` y monograma `color = scheme.onPrimary`
      (`MaterialTheme.colorScheme`), sin tocar tamaño (96), forma (`SazonShapes.medium` = 24),
      `MONOGRAM_STYLE` ni el centrado.
- [ ] Eliminar `TILE_ACCENT_RULE` y el `Box` del filete de 5 px; quitar los imports que queden sin uso
      (`fillMaxHeight`) y conservar los que sí se usan (`width`, `fillMaxWidth`).
- [ ] KDoc de `MONOGRAM_STYLE` y de `RecipeMonogramTile` reconciliados (fondo `primary`, monograma
      `onPrimary`, sin regla terracota; `RecipeMonogram` intacto).
- [ ] `FavoriteTileContrastTest` (paquete `com.sazon.app.ui.theme`), **4 tests** con los vectores del
      Scenario 4: `la_miniatura_cumple_el_3_a_1_de_wcag_1_4_11_contra_la_tarjeta` (`3,9738` claro /
      `3,3056` oscuro, `>= 3.0` y exacto ±0.05), `el_monograma_se_lee_sobre_el_fondo_primary`
      (`onPrimary`/`primary` `3,9738`, `>= 3.0`), `el_fondo_anterior_no_llegaba_al_umbral` (`1,2088`
      claro y `1,0445` oscuro, `< 3.0`) y `los_roles_del_tema_de_la_miniatura_son_primary_y_onPrimary`
      (`LightColorScheme`/`DarkColorScheme.primary/onPrimary` == `SazonPrimary`/`SazonOnPrimary` y
      `SazonPrimary != SazonSurfaceVariant`). Helpers WCAG privados de fichero, como en `CardTokensTest`.
- [ ] Verificar por inspección: `grep -rn "TILE_ACCENT_RULE" app/src` → 0; en
      `RecipeMonogramTile` → 0 `surfaceVariant`; `FavoriteText`, `HeartButton`, `DismissBackground`, el
      `Surface` y el swipe **sin** cambios; `ui/theme/**`, `RecipeMonogram.kt`, `FavoritesScreen.kt`, la
      rejilla, `data/**`, `domain/**`, build/lockfile, `init.sh` y `scripts/**` intactos; 0 hex nuevos en
      `ui/favorites`; `docs/design/concepts/**` sin cambios y **sin** carpeta nueva.
- [ ] `DESIGN.md` (RecipeListCard + `Accessibility Baseline` + limpieza de la coletilla de estado),
      `docs/technical-discovery.md` (l. 236-237) y `docs/adr/0006` §6 actualizados.
- [ ] `bash init.sh` (test + lint) en verde, registrar evidencia (conteos con delta vs `HEAD`) y avisar
      al orquestador (la `feature_list.json`/`PROGRESS.md` las cierra el orquestador).

## Verification Plan

- `bash init.sh` → **exit 0** (gate no bloqueante estándar: `./gradlew test` + `./gradlew lint` vía
  `scripts/gradle.sh`); **no** arranca procesos largos y `init.sh` **no** se modifica.
- Tirada focal: `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.theme.*"
  --rerun-tasks` → `BUILD SUCCESSFUL` (sin regresión de `CardTokensTest` ni `DesignTokensTest`).
- Parseo propio de `app/build/test-results/testDebugUnitTest/TEST-*.xml`: **0 fallos/errores/saltados**;
  re-derivar la baseline en `HEAD` (`git grep -c '@Test'`; referencia del `HEAD` actual: **97 suites /
  733 tests**) y registrar el delta esperado **+1 suite / +4 tests**.
- Lint: `app/build/reports/lint-results-debug.sarif` → **0 errores** y **0** hallazgos nuevos bajo
  `app/src` (los `GradleDependency`/`NewerVersionAvailable`/`AndroidGradlePluginVersion`/`OldTargetApi`
  son preexistentes: 13 warnings).
- Mapeo explícito del `verification` de la feature:
  - "la miniatura pasa a fondo `primary` con el monograma en `onPrimary`": escenarios 1 y 2 por
    inspección del composable + grep (`scheme.primary`/`scheme.onPrimary` en `RecipeMonogramTile`).
  - "el contraste de la miniatura contra la superficie de la tarjeta es ≥ 3:1 en claro y en oscuro
    (WCAG 1.4.11), con un test JVM de contraste como `CardTokensTest`": escenario 4 (los 5 vectores) y
    `bash init.sh` verde.
  - "la geometría de la miniatura (96×96, esquina 24) y el resto de la fila no cambian; sin
    dependencias nuevas": escenarios 5 y 7 (`grep` de `96.dp`/`SazonShapes.medium`, diff del resto de la
    fila, `git diff --name-only` sin build/lockfile).
  - "'bash init.sh' verde (test + lint)": escenario 7.
  - "las capturas de referencia de `docs/design/concepts/favorites-tile-contrast/` respetadas" →
    **reconciliado por la exención de la issue #130** registrada en `DESIGN.md`: la carpeta **no** se
    crea (`test ! -e docs/design/concepts/favorites-tile-contrast`) y `docs/design/concepts/**` queda
    sin cambios (`git status docs/design/concepts` limpio).
- Inspección de alcance y pureza:
  - `grep -rn "TILE_ACCENT_RULE" app/src docs/` → **0**.
  - `sed -n '/private fun RecipeMonogramTile/,/^}/p' app/src/main/java/com/sazon/app/ui/favorites/FavoriteRecipeCard.kt | grep -n "surfaceVariant"` → **0**.
  - `grep -rn "0xFF\|0x[0-9A-Fa-f]\{8\}" app/src/main/java/com/sazon/app/ui/favorites` → sin hex nuevos
    (los valores viven en `ui/theme`).
  - `grep -rn "surfaceVariant" docs/technical-discovery.md` → sin la mención de la miniatura de
    Favoritos.
  - `git diff --name-only` = Expected File Changes (+ esta spec); **sin** diff en `ui/theme/**`,
    `ui/home/RecipeMonogram.kt`, `ui/favorites/FavoritesScreen.kt`, la rejilla (`FavoriteRecipeTile`),
    `data/**`, `domain/**`, `assets/**`, `app/build.gradle.kts`, `gradle/**`, `init.sh`, `scripts/**` ni
    `docs/design/concepts/**`.
- Cobertura **E2E**: **no aplica** — el repo no tiene suite E2E persistente y `AGENTS.md` no automatiza
  UI de emulador; la cobertura canónica es el contrato en JVM (contraste) más la revisión visual de la
  persona.
- **Prueba manual (persona, tras el merge; no automatizable desde WSL)**: en el emulador, abrir
  Favoritos en **lista** con tema **claro** y **oscuro** y comprobar que la miniatura terracota resalta
  del fondo de la fila, que el monograma se lee, que no hay ningún filete de 5 px, que el título, la
  meta y el corazón no se han movido, y que una receta sin iniciales muestra el cuadrado sólido sin
  texto. Un defecto visual se corrige con una **feature correctiva**, sin bloquear el flujo autónomo.

## Evidence To Capture

- Salida de `bash init.sh` (exit 0) y de la tirada focal; conteos de suites/tests con delta vs `HEAD`.
- Los 5 ratios del Scenario 4 con dos decimales (3,97 / 3,31 / 3,97 / 1,21 / 1,04) y la confirmación de
  los roles del tema (`LightColorScheme.primary == SazonPrimary`, etc.) y del caso negativo
  (`SazonPrimary != SazonSurfaceVariant`).
- Resultado de los greps de alcance/pureza (0 `TILE_ACCENT_RULE`, 0 `surfaceVariant` dentro del tile, 0
  hex nuevos, `docs/design/concepts/**` intacto y **sin** carpeta `favorites-tile-contrast`) y de
  `git diff --name-only`.
- `DESIGN.md`, `docs/technical-discovery.md` y `docs/adr/0006` §6 actualizados; confirmación de que el
  implementer **no** hizo commit, push ni PR.

## Decisions And Unknowns

Decisiones cerradas (el implementer no las reabre):

1. **Fondo `primary` + monograma `onPrimary`**: exactamente lo que declara el frontmatter de
   `DESIGN.md` (`components.recipe-tile-favorites`). `primary` da **3,97:1** en claro y **3,31:1** en
   oscuro (≥3:1 de WCAG 1.4.11); `surfaceVariant` daba 1,21:1 / 1,04:1.
2. **Sin tokens nuevos**: `primary`/`onPrimary` ya existen y son idénticos en claro y oscuro; la
   miniatura consume los **roles del tema**, no alias propios (evita duplicar el contrato).
3. **La regla terracota de 5 px se retira**: sobre `primary` mide **1,00:1** (invisible) y `DESIGN.md`
   ya dejó de declarar `accentRule` en la revisión del 2026-10-14. Dejarla sería código muerto y KDoc
   engañoso. No cambia ningún píxel porque era indistinguible del fondo que ahora la rodea.
4. **El monograma es texto grande**: 30sp Manrope Bold ⇒ umbral **3:1** (no 4,5:1); el ratio real es
   3,97:1. Si en el futuro se bajara el tamaño por debajo de 24 px, habría que revisar el umbral.
5. **El estado vacío y la rejilla no se tocan**: el cuadrado de 96 dp del estado vacío es decorativo y
   no vive dentro de una tarjeta (queda como observación, no como trabajo de esta feature).
6. **Sin capturas nuevas**: exención autorizada (#130) registrada en `DESIGN.md`; nada que crear bajo
   `docs/design/concepts/**` y las capturas/prototipos aceptados **no** se editan aunque muestren el
   fondo anterior.
7. **Contraste monotonía**: el ratio se mide contra la **superficie de la tarjeta**
   (`SazonCardTokens.light/dark.surface`), que es el fondo real que rodea a la miniatura (blanco en
   claro, `#3A2E26` en oscuro), no contra el fondo de pantalla.

Incógnitas y riesgos (no bloquean, quedan anotados):

- **Revisión visual de la persona**: 3,97:1 es correcto para el umbral no textual (3:1) pero **por
  debajo** de 4,5:1; el monograma es texto grande y cumple, y la miniatura es un bloque, no texto. Si al
  verlo en el emulador el terracota “grita” demasiado en la lista, la mitigación es una **correctiva**
  (p. ej. `primaryContainer` con `onPrimaryContainer`), no un ajuste improvisado aquí.
- **El corazón** sigue sobre `surfaceVariant` dentro de la misma fila: la combinación
  (`primary` del tile + chip `surfaceVariant`) es la aceptada por `DESIGN.md` y no se replantea.
- **Prototipos/capturas históricos** que muestran el fondo anterior: se documenta la deriva en
  `DESIGN.md`/`docs/adr/0006`, no se editan (son material aceptado de solo lectura).
- **Estado vacío** con un cuadrado `surfaceVariant` muy parecido al tile anterior: queda **fuera** de
  alcance; si la persona lo quiere alineado, es una correctiva posterior.
- **`DesignTokensTest`** parsea el frontmatter de `DESIGN.md` (`colors`/`spacing`/`rounded`/
  `typography`): los retoques de `DESIGN.md` de esta feature viven en `components` y en el texto, así que
  no pueden hacerlo fallar; aun así el gate lo comprueba.

## Validator Checklist

- [ ] Alcance: solo `ui/favorites/FavoriteRecipeCard.kt` (tile), el test nuevo
      `ui/theme/FavoriteTileContrastTest.kt` y las 3 docs durables; **sin** tocar `ui/theme/**`
      (ningún token), `RecipeMonogram.kt`, `FavoritesScreen.kt` (estado vacío), la rejilla, `HeartButton`
      ni el swipe, `data/**`, `domain/**`, `assets/**`, build/lockfile, `init.sh`, `scripts/**` ni
      `docs/design/concepts/**`; **sin** dependencias nuevas.
- [ ] Escenarios 1-3: la miniatura es `primary` con el monograma en `onPrimary` en **claro y oscuro**,
      con el mismo color en ambos; sin monograma el tile sigue sólido y sin errores; geometría 96×96 /
      esquina 24 y el resto de la fila intactos; regla de 5 px **retirada** (no código muerto).
- [ ] Escenario 4: los 5 vectores del contrato (incluidos **dos negativos** que documentan el fondo
      anterior por debajo de 3:1) existen y los roles medidos son los que consume el tile
      (`LightColorScheme`/`DarkColorScheme`); el test no pasa en vacío y no toca `CardTokensTest`.
- [ ] Escenario 5-6: `grep` de `TILE_ACCENT_RULE` → 0, 0 `surfaceVariant` dentro de
      `RecipeMonogramTile`, 0 hex nuevos en `ui/favorites`, `git diff --name-only` = ficheros esperados.
- [ ] Escenario 7: `bash init.sh` verde con evidencia real (comandos, conteos con delta vs `HEAD`,
      ratios, greps) y lint sin hallazgos nuevos bajo `app/src`.
- [ ] Capturas: **no** se exige ni existe `docs/design/concepts/favorites-tile-contrast/` (exención
      #130 registrada en `DESIGN.md`) y las capturas aceptadas de referencia **no** se han modificado.
- [ ] `DESIGN.md` (RecipeListCard + `Accessibility Baseline` + limpieza de la coletilla de estado),
      `docs/technical-discovery.md` y `docs/adr/0006` §6 actualizados; `feature_list.json`/`PROGRESS.md`
      los actualiza el orquestador y el implementer no hizo commit, push ni PR.
- [ ] No se añade comportamiento ajeno a la feature (ni rejilla, ni estado vacío, ni corazón, ni
      buscador/orden/toggle, ni tokens/microcopy/estados nuevos).
