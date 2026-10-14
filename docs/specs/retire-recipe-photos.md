# Feature Implementation Spec: Retirada de las fotos locales y de la cadena de assets

## Source Feature

- `id`: `retire-recipe-photos` · `area`: `data` · `status`: `not_started` (al planificar)
- `depends_on`: `["dish-artwork-as-card-background"]` (ya `accepted`) · `source`: `feature_list.json`

## Goal

La imagen de una receta dejó de ser una foto local con `dish-artwork-as-card-background` (ADR 0006): la capa
base es la **ficha tipográfica** y, cuando hay imagen, es una **capa de fondo** generada por IA supeditada al
ajuste del usuario. La foto de categoría ya no tiene lectores y es justo la que **no corresponde al plato**
de las recetas `ai-*`.

Esta feature retira la cadena local de runtime y de APK: salen **30 assets** de
`app/src/main/assets/img/recipes/` (12 `seed/<id>.jpg` + 18 `category/cat-<categoryId>.jpg`, **5.954.912 B ≈
5,68 MiB**) con su resolución pura (`data.RecipePhotoAssets.resolve/candidates`), su costura Android
(`data.AssetRecipePhotoSource`), el seam Compose (`ui.artwork.RecipePhotoSource` + `LocalRecipePhotoSource`),
su cableado en `MainActivity`, su test de inventario y el script de empaquetado. Se **conserva**
`img/recipes/default.jpg` (**137.359 B**) como ancla de estilo de `dish-style-reference` (ADR 0005 fase 6),
sin mostrarse en ninguna pantalla, y se **conserva** `img/dishes/seed/**` (6 WebP, 449.844 B). Se limpian
además las referencias residuales (docs durables, KDoc, microcopy y el campo muerto
`recipeCategories[].image`).

## Non-Goals

- No se toca la cadena de imagen por IA (ADR 0005): interruptor, tope por sesión, caché, set semilla,
  descriptor, referencia de estilo ni su transporte. Se mantienen comportamiento, cuota y tests **salvo** el
  valor de diagnóstico huérfano `DishImageOrigin.CATEGORY` y los comentarios que afirmaban que la foto local
  es el respaldo.
- **No** se borra `img/dishes/seed/**` (Decisión 3).
- No se rediseña nada visual (geometría, tokens, layout L2, 16:9/240). El único cambio visible es **una
  cadena de microcopy** ya registrada en `DESIGN.md` (Decisión 6).
- Sin dependencias, sin Coil, sin DI, sin pantallas ni capturas nuevas.
- No se reescribe la evidencia histórica de features aceptadas: `feature_list.json` (evidence/notes de
  `recipe-photos`), `docs/validations/**`, `PROGRESS.md` y las specs que citan el script como **patrón**
  (`dish-image-seed.md`, `dish-style-reference.md`) quedan intactos. La única nota de superación va a
  `docs/specs/recipe-photos.md` y a `docs/ai-dish-images-proposal.md`.
- No se tocan las capturas de `docs/design/concepts/**` (solo una nota de estado en el `README.md` de
  `recipe-photos`).

## Job Story

Cuando reviso qué viaja en la app y veo que una receta generada por IA muestra la foto genérica de su
categoría (que no es ese plato), quiero que la cadena de fotos locales desaparezca del APK y del runtime,
para que la app solo muestre imagen cuando es honesta y el APK adelgace.

## Users And Permissions

- **Persona propietaria / desarrollador**: ejecuta `bash init.sh` y `assembleDebug` e inspecciona el APK;
  único actor que decide el contenido empaquetado.
- **Usuario local**: sin permisos ni ajustes nuevos. Interruptor apagado → ficha tipográfica (ya no "solo la
  foto local"); encendido → imagen generada como capa de fondo, igual que hoy. Sin cuenta ni red nueva.

## Acceptance Scenarios

### Scenario 1: el APK ya no lleva las fotos locales

Given la cadena retirada, When se ejecuta `bash scripts/gradle.sh assembleDebug`, Then `assets/img/recipes/`
del APK contiene **solo** `default.jpg` (137.359 B), con **0** entradas bajo `seed/` y `category/`, y
`assets/img/dishes/seed/` conserva sus 6 WebP (449.844 B).

### Scenario 2: ninguna pantalla muestra una foto local

Given el árbol de composición, When se buscan consumidores de la foto local, Then hay **0** referencias a
`AssetRecipePhotoSource`, `RecipePhotoSource` y `LocalRecipePhotoSource` en `app/src`, `RecipeArtwork` solo
pinta la capa de fondo **generada** sobre la ficha tipográfica y `MainActivity` no provee ningún seam de fotos.

### Scenario 3: el ancla de estilo sigue viva

Given `img/recipes/default.jpg` conservado, When `AssetDishImageStyleReference(context).load()` lee el asset
(Robolectric) y `DishImageStyleReferencePolicy.resolve` recorre las extensiones, Then la referencia no es
`null`, su `mediaType` es `image/jpeg` y su `fileName` `dish-style-reference.jpg`, y una base inexistente
sigue devolviendo `null` sin lanzar.

### Scenario 4: se retira la cadena, no el módulo compartido

Given `data.RecipePhotoAssets` tras la limpieza, When se inspecciona el objeto y sus consumidores, Then
conserva **solo** `ROOT`, `DEFAULT_KEY` y `EXTENSIONS` (sin `resolve`, `candidates` ni `CATEGORY_IDS`),
`DishImageStyleReferencePolicy` deriva `ASSET_BASE == "img/recipes/default"` y
`DishImageSeedAssets.EXTENSIONS == [webp, jpg, png]`, con sus tests en verde.

### Scenario 5: el diagnóstico pierde el valor sin productor

Given `DishImageOrigin` tras la retirada, When se enumeran sus valores y se pinta una imagen, Then el
vocabulario es `GENERATED`/`SEED`/`GLYPH` (sin `CATEGORY`), `RecipeArtworkImagePrecedence.originOf` conserva
su comportamiento (`SEED` si el resolutor sirvió el set semilla, `GENERATED` en cualquier otro caso con
imagen, `GLYPH` sin imagen) y el log `SazonDishImage` sigue emitiendo `origin=<…> id=<…> title=<…>` solo en debug.

### Scenario 6: la microcopy no afirma algo falso

Given el ajuste "Generar imágenes del plato" apagado, When el Perfil pinta su resumen, Then el texto es
`"Desactivado: las recetas se muestran sin imagen."` (ya no `"…solo la foto local."`) y los KDoc de
`DishImageSettingsState.enabled`, `DishImageSettings` y `GatedDishImageGenerator` hablan de la ficha
tipográfica, no de conservar la foto local.

### Scenario 7: la Taxonomía pierde el campo que resolvía la imagen

Given `docs/domain/taxonomy.json` y `RecipeCategory`, When se parsea la Taxonomía real y corre la suite,
Then los 18 items de `recipeCategories` tienen solo `id` y `label` (sin `image`), no existe
`recipeCategories.imageResolution`, `TaxonomyIntegrityTest` y `RecipeArtworkStyleTest` siguen verdes y el
glifo por categoría, el filtro por categoría y el prompt no cambian.

### Scenario 8: los docs durables no la describen como viva

Given los documentos normativos, When se buscan referencias a la cadena local como activa, Then `DESIGN.md`,
`docs/technical-discovery.md`, `docs/domain/taxonomy.md`, `docs/build-brief.md`,
`docs/risks-and-open-questions.md`, `CONTEXT.md` y los ADR 0004/0005/0006 la describen **en pasado/retirada**
(ancla conservada, `scripts/fetch-recipe-photos.sh` **eliminado**), el ADR 0006 marca la quinta feature como
implementada y ningún doc normativo apunta a un fichero borrado.

### Scenario 9: el gate sigue verde

Given la retirada aplicada, When se ejecuta `bash init.sh`, Then exit 0 (test + lint vía
`scripts/gradle.sh`, sin procesos largos), con **97 suites / 731 tests / 0 fallos / 0 errores / 0 saltados**
(baseline 98/742; delta −1 suite / −11 tests = `RecipePhotoAssetsTest`) y lint **0 errores / 13 warnings
preexistentes**, ninguno bajo `app/src`.

## Repository Research

### Files Inspected

- Reglas y estado: `AGENTS.md`, `PROGRESS.md`, `CONTEXT.md`, `feature_list.json`.
- ADR 0004 (ya lleva la nota "Ajustado por 0006"), 0005 (fases 1-7; `default.<ext>` = ancla de la fase 6) y
  0006 (decisión 4: 30 de 31 assets fuera, ancla conservada).
- Docs durables: `docs/build-brief.md`, `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`,
  `DESIGN.md` (registro de capturas, §"Imagen de receta", microcopy ~línea 416, OQ4/OQ7),
  `docs/domain/taxonomy.md` (§3 y §6), `docs/domain/taxonomy.json`, `docs/ai-dish-images-proposal.md`,
  `docs/design/concepts/recipe-photos/{README.md,stitch-prompt.md}` y las specs
  `{recipe-photos,dish-image-seed,dish-style-reference,dish-artwork-as-card-background}.md`.
- Código: `data/RecipePhotoAssets.kt`, `data/AssetRecipePhotoSource.kt`, `ui/artwork/RecipePhotoSource.kt`,
  `MainActivity.kt`, `data/{DishImageSeedAssets,AssetDishImageSeedSource}.kt`,
  `data/ai/image/{DishImageStyleReference,AssetDishImageStyleReference,DevExpertDishImageGenerator}.kt`,
  `data/image/{DishImageOrigin,DishImageOriginSink,DishImageResolver,DishImageCachePolicy,DishImageSettings}.kt`,
  `ui/artwork/{RecipeArtworkImagePrecedence,GeneratedRecipeArtworkSource,DishImageArtworkLoader,DishImageArtworkPolicy,AndroidRecipeImageDecoder}.kt`,
  `ui/profile/DishImageSettingsCopy.kt`, `domain/image/DishImageGenerator.kt`,
  `domain/taxonomy/{Taxonomy,TaxonomyJson}.kt`, `app/build.gradle.kts`, `init.sh`.
- Tests: `data/RecipePhotoAssetsTest.kt` (11 `@Test`), `data/DishImageSeedAssetsTest.kt`,
  `data/ai/image/{AssetDishImageStyleReferenceTest,DishImageStyleReferenceTest}.kt`,
  `data/image/DishImageOriginSinkTest.kt`,
  `ui/artwork/{RecipeArtworkImagePrecedenceTest,GeneratedRecipeArtworkSourceTest}.kt`,
  `ui/profile/DishImageSettingsCopyTest.kt`.
- Inventario real (`find`/`stat`): `img/recipes/seed` 12 / 2.319.009 B; `img/recipes/category` 18 /
  3.635.903 B; `img/recipes/default.jpg` 137.359 B; `img/dishes/seed` 6 / 449.844 B. Baseline en `HEAD`
  (`ddafe9e`): **98 ficheros con `@Test`** (última medición aceptada: 98 suites / 742 tests).

### Existing Patterns To Follow

- **Costura muerta se retira, no se documenta como "sin uso"** (precedente `home-recently-opened-group` #91).
  Contenido versionado y **no ignorado** (`.gitignore` solo ignora `assets/taxonomy.json`): los assets se
  borran con `git rm` (precedente `git mv` de los assets semilla en `dish-image-prompt-v2`).
- **Docs durables en pasado + ADR "Ajustado por…"**: no se reescribe la decisión original; se anota. Nota
  mínima en specs superadas, sin reescribir escenarios históricos (precedente `ai-response-format`).
- Gate: `bash init.sh` (test + lint vía `scripts/gradle.sh`); **nunca** `./gradlew` directo ni `--stop`. Los
  conteos se re-derivan de los XML de `app/build/test-results/` y del SARIF.
- Cero dependencias/hex/assets nuevos; `ui.theme` y la geometría no se tocan.

### Current Gaps

- `RecipePhotoAssets` es hoy **mixto**: la parte retirada (`resolve`/`candidates`/`CATEGORY_IDS`) y tres
  constantes que ADR 0005 **sí** reutiliza (`ROOT`, `DEFAULT_KEY`, `EXTENSIONS`); no se puede borrar en bloque.
- `DishImageOrigin.CATEGORY` existe, está documentado y **no tiene productor** desde
  `dish-artwork-as-card-background`.
- La microcopy del ajuste afirma que se usa "solo la foto local" (`DishImageSettingsCopy.DISABLED_SUMMARY`,
  registrada en `DESIGN.md`).
- `recipeCategories[].image` + `imageResolution` nunca se leen en Kotlin, pero `taxonomy.md` §3 ya
  comprometió su limpieza a esta feature.
- No hay test automático de "el APK no contiene X": la verificación de empaquetado es inspección del APK
  (patrón ya usado en `recipe-photos`/`dish-image-seed`).

## Technical Approach

1. `git rm` los 12 `img/recipes/seed/*` y los 18 `img/recipes/category/*`; **conservar** `default.jpg` y
   `img/dishes/seed/**`. `git rm` también `data/AssetRecipePhotoSource.kt`,
   `ui/artwork/RecipePhotoSource.kt`, `RecipePhotoAssetsTest.kt` y `scripts/fetch-recipe-photos.sh`.
2. Adelgazar `data/RecipePhotoAssets.kt` a `ROOT`/`DEFAULT_KEY`/`EXTENSIONS`, con KDoc reescrito: ya no es la
   cadena de fotos sino el **ancla de estilo** y el orden de extensiones compartido por las dos cadenas de
   assets vivas (fases 6 y 7).
3. `MainActivity`: fuera los 3 imports, el `val recipePhotoSource`, el argumento
   `LocalRecipePhotoSource provides …` y los comentarios de la cadena. `app/build.gradle.kts`: fuera
   `recipePhotosDir`, su comentario y la property `recipe.photos.assets.dir`.
4. Taxonomía: `RecipeCategory` sin `image`; `docs/domain/taxonomy.json` sin las 18 claves `"image"` ni
   `"imageResolution"`, con `recipeCategories.purpose` reescrito.
5. `DishImageOrigin` sin `CATEGORY`; KDoc del enum, de `RecipeArtworkImagePrecedence.originOf` y de
   `DishImageOrigin` ajustados y los 2 tests que usan el valor corregidos.
6. Microcopy nueva en `DishImageSettingsCopy.DISABLED_SUMMARY` + su test + registro en `DESIGN.md`.
7. Barrido de referencias residuales (fuente de verdad: `grep -rn "foto local\|cadena local\|recipe-photos\|
   RecipePhotoAssets\.resolve\|AssetRecipePhotoSource\|RecipePhotoSource" app/src`): todo comentario que
   afirme que la foto local existe, es un nivel o es el respaldo pasa a decir que la base sin imagen es la
   **ficha tipográfica**.
8. Docs durables a pasado/retirada con la medición real (30 assets, ≈5,68 MiB; los "~6,2/~6,4 MiB" de los
   docs son estimaciones previas y se corrigen): ADR 0004 (nota ajustada), 0005 (cadena retirada; `CATEGORY`
   fuera del vocabulario), 0006 (quinta feature implementada), nota de superación en
   `docs/specs/recipe-photos.md` y `docs/ai-dish-images-proposal.md`, y nota de retirada en
   `docs/design/concepts/recipe-photos/README.md`. Gate: `bash init.sh` + `assembleDebug` + inspección del
   APK; re-derivar conteos de los XML.

### Decisiones (fijadas, con motivo)

1. **`RecipePhotoAssets` se adelgaza; no se borra ni se renombra.** Borrarlo obligaría a mudar o duplicar
   `EXTENSIONS`/`ASSET_BASE` y a tocar `data.ai.image`/`data.image` de ADR 0005 sin cambio de comportamiento;
   renombrarlo sería churn en 4 ficheros y 2 tests. Se conserva el nombre con KDoc honesto. *(Alternativa
   descartada: `git mv` a `data.ImageAssetPaths`.)*
2. **Se retira `DishImageOrigin.CATEGORY`** (describe la cadena eliminada, sin productor; precedente #91).
   Diagnóstico puro: no cambia bytes, caché, tope ni red.
3. **Se conserva `img/dishes/seed/**`.** No está muerto: con el interruptor encendido (por defecto,
   `DishImageSettingsPolicy.DEFAULT_ENABLED = true`) es la capa que sirve esos platos **sin red y sin cuota**,
   y el ADR 0006 ya decidió mantenerlo. Retirarlo sería cambio de comportamiento, no limpieza.
4. **Se borra `scripts/fetch-recipe-photos.sh`.** Su única función era empaquetar los assets retirados; dejarlo
   vivo permitiría repoblar un directorio retirado y `init.sh` nunca lo usó. El ancla está versionada en git y
   su procedencia sigue en `docs/design/concepts/recipe-photos/`. *(Alternativa descartada: conservarlo con
   cabecera "retirado".)*
5. **Se limpia `recipeCategories[].image` + `imageResolution`** (comprometido en `taxonomy.md` §3; apunta a
   rutas inexistentes; sin lectores en Kotlin).
6. **Microcopy del interruptor apagado**: `"Desactivado: las recetas se muestran sin imagen."` (evita el
   término interno "ficha tipográfica" ante el usuario y deja de afirmar algo falso). Se registra en
   `DESIGN.md`; la persona puede ajustarla en su revisión visual tras el merge.
7. **Se conservan las capturas de `docs/design/concepts/recipe-photos/`** (solo nota de estado): son la
   procedencia del ancla, no viajan en el APK y borrarlas perdería trazabilidad.

## Expected File Changes

**Borrados**: `app/src/main/assets/img/recipes/seed/**` (12) · `app/src/main/assets/img/recipes/category/**`
(18) · `app/src/main/java/com/sazon/app/data/AssetRecipePhotoSource.kt` ·
`app/src/main/java/com/sazon/app/ui/artwork/RecipePhotoSource.kt` ·
`app/src/test/java/com/sazon/app/data/RecipePhotoAssetsTest.kt` · `scripts/fetch-recipe-photos.sh`.

**Modificados (código/build)**: `data/RecipePhotoAssets.kt` (adelgazar) · `MainActivity.kt` (seam fuera) ·
`app/build.gradle.kts` (property fuera) · `domain/taxonomy/Taxonomy.kt` (`RecipeCategory`) ·
`data/image/DishImageOrigin.kt` (`CATEGORY` fuera) · `ui/profile/DishImageSettingsCopy.kt` (microcopy) ·
`ui/artwork/RecipeArtworkImagePrecedence.kt` (KDoc). Comentarios residuales (paso 8):
`data/image/{DishImageSettings,DishImageCachePolicy,DishImageResolver}.kt`,
`data/{DishImageSeedAssets,AssetDishImageSeedSource}.kt`,
`data/ai/image/{DishImageStyleReference,AssetDishImageStyleReference,DevExpertDishImageGenerator}.kt`,
`domain/image/DishImageGenerator.kt`,
`ui/artwork/{GeneratedRecipeArtworkSource,DishImageArtworkLoader,DishImageArtworkPolicy,AndroidRecipeImageDecoder}.kt`.

**Modificados (tests)**: `data/ai/image/DishImageStyleReferenceTest.kt` (quitar el assert contra
`RecipePhotoAssets.EXTENSIONS`, dejar el literal) · `data/image/DishImageOriginSinkTest.kt` (sustituir
`CATEGORY`) · `ui/artwork/RecipeArtworkImagePrecedenceTest.kt` (quitar el assert de `CATEGORY`) ·
`ui/profile/DishImageSettingsCopyTest.kt` (literal nuevo). Solo comentarios:
`AssetDishImageStyleReferenceTest.kt`, `GeneratedRecipeArtworkSourceTest.kt`, `DishImageSeedAssetsTest.kt`.

**Modificados (docs)**: `docs/adr/0004` · `docs/adr/0005` · `docs/adr/0006` · `DESIGN.md` ·
`docs/technical-discovery.md` · `docs/domain/taxonomy.md` · `docs/domain/taxonomy.json` ·
`docs/build-brief.md` · `docs/risks-and-open-questions.md` · `CONTEXT.md` · `docs/specs/recipe-photos.md`
(nota de superación) · `docs/ai-dish-images-proposal.md` (nota mínima de estado) ·
`docs/design/concepts/recipe-photos/README.md` (nota de retirada). `feature_list.json` y `PROGRESS.md` los
actualiza el flujo, no el implementer.

**No se toca**: `init.sh`, `scripts/gradle.sh`, `scripts/prepare-dish-image-seed.sh`, `.gitignore`,
`gradle/libs.versions.toml`, `ui/theme/**`, `ui/home/**`, `ui/detail/**`, `ui/results/**`, `ui/favorites/**`,
`ui/generator/**`, `ui/MainShell.kt`, `domain/**` (salvo `RecipeCategory`), `data/ai/**` (salvo comentarios),
`assets/seed-recipes.json`, `img/recipes/default.jpg`, `img/dishes/seed/**`, `docs/design/concepts/**`
(capturas), `docs/validations/**` y las specs aceptadas.

## Visual Design Impact

- UI involved: **microcopy solamente** (una cadena ya registrada en `DESIGN.md`; sin pantalla, estado,
  geometría, token ni layout nuevo).
- Design source: `DESIGN.md` (§"Imágenes del plato" y §"Imagen de receta", ya en dirección L2 por ADR 0006).
- Screens or states affected: Perfil → "Imágenes del plato" → resumen con el interruptor **apagado**.
- New design artifact required: **no**. Excepción de capturas de la misma clase autorizada para correctivas
  de microcopy/sin cambio visual (#77, #85, #86, #91, #101, #110): **no** se crea
  `docs/design/concepts/retire-recipe-photos/`. `UI captures (required)`: ninguna.

## Durable Documentation Impact

- `docs/technical-discovery.md`: **update** — cambian los assets empaquetados, el test borrado y el
  presupuesto de tamaño (frontera durable de stack/almacenamiento/verificación).
- `docs/domain-model.md`: **not needed** — ninguna entidad, relación o estado de Receta cambia; el campo
  retirado es de la Taxonomía y su casa es `docs/domain/taxonomy.md`.
- `AGENTS.md`: **not needed** — no cambian flujo de agentes, camino de arranque ni reglas del repo.
- `CONTEXT.md`: **update** — el glosario definía "Categoría de receta" por la imagen local.
- `DESIGN.md`, ADR 0004/0005/0006, `docs/domain/taxonomy.md`, `docs/domain/taxonomy.json`,
  `docs/build-brief.md`, `docs/risks-and-open-questions.md`, `docs/specs/recipe-photos.md`,
  `docs/ai-dish-images-proposal.md`, `docs/design/concepts/recipe-photos/README.md`: **update**.
- `ARCHITECTURE.md` / `CONSTRAINTS.md`: **no existen** en este repo (no se crean). `init.sh`: **not needed**
  (el gate no cambia; ningún paso nuevo que ejecutar ni que imprimir).

## Implementation Plan

1. Borrar assets + costuras + script y adelgazar `RecipePhotoAssets`.
2. Desconectar el seam en `MainActivity` y limpiar el build.
3. Retirar `recipeCategories[].image` y `DishImageOrigin.CATEGORY`, ajustando tests.
4. Microcopy nueva y barrido de KDoc/comentarios residuales.
5. Docs durables + ADR + notas de superación.
6. Gate, `assembleDebug` + inspección del APK y re-derivación de conteos.

## Implementation Tasks

- [ ] `git rm` los 30 assets retirados (12 `seed/` + 18 `category/`).
- [ ] `git rm` `AssetRecipePhotoSource.kt`, `RecipePhotoSource.kt`, `RecipePhotoAssetsTest.kt` y
      `scripts/fetch-recipe-photos.sh`.
- [ ] Adelgazar `RecipePhotoAssets` a `ROOT`/`DEFAULT_KEY`/`EXTENSIONS` con KDoc nuevo.
- [ ] Quitar de `MainActivity` los 3 imports, el `val recipePhotoSource`, el argumento
      `LocalRecipePhotoSource provides …` y el comentario de la cadena local.
- [ ] Quitar de `app/build.gradle.kts` `recipePhotosDir`, su comentario y la property
      `recipe.photos.assets.dir`.
- [ ] Quitar `image` de `RecipeCategory` y las 18 claves + `imageResolution` de `docs/domain/taxonomy.json`
      (ajustar `purpose`).
- [ ] Quitar `CATEGORY` de `DishImageOrigin` y ajustar su KDoc, el de `originOf` y los 2 tests.
- [ ] Cambiar `DishImageSettingsCopy.DISABLED_SUMMARY` y su test.
- [ ] Barrer y corregir los KDoc/comentarios residuales (grep del paso 8).
- [ ] Actualizar docs durables, ADR y notas de superación.
- [ ] Ejecutar el gate, empaquetar e inspeccionar el APK; registrar evidencia.

## Verification Plan

- `bash init.sh` → exit 0 (test + lint vía `scripts/gradle.sh`; sin procesos largos ni emulador, sin cambios
  en `init.sh`). Esperado **97 suites / 731 tests / 0 fallos / 0 errores / 0 saltados**; lint **0 errores /
  13 warnings preexistentes**, 0 bajo `app/src`.
- Baseline re-derivada antes de tocar nada: `git grep -c '@Test' HEAD -- 'app/src/test/**' | wc -l` → 98.
  Delta esperado **−1 suite / −11 tests** (= `RecipePhotoAssetsTest`); cualquier otro delta es un defecto.
- `bash scripts/gradle.sh assembleDebug` + `unzip -l app/build/outputs/apk/debug/app-debug.apk | grep -E
  "img/(recipes|dishes)"` → solo `assets/img/recipes/default.jpg` (137.359 B) y los 6
  `assets/img/dishes/seed/*.webp` (449.844 B); 0 entradas de `seed/` y `category/`.
- Inventario en disco: `find app/src/main/assets/img/recipes -type f` → 1; `find
  app/src/main/assets/img/dishes -type f | wc -l` → 6.
- Greps de alcance (todos a 0): `AssetRecipePhotoSource`, `LocalRecipePhotoSource`, `RecipePhotoSource`,
  `RecipePhotoAssets.resolve`, `RecipePhotoAssets.candidates`, `RecipePhotoAssets.CATEGORY_IDS`,
  `DishImageOrigin.CATEGORY`, `"foto local"`, `"cadena local"` en `app/src`; y
  `test ! -e scripts/fetch-recipe-photos.sh`.
- Focal: `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.data.*" --tests
  "com.sazon.app.ui.artwork.*" --tests "com.sazon.app.ui.profile.*" --tests
  "com.sazon.app.domain.taxonomy.*" --rerun-tasks` en verde (cubre los cuatro paquetes tocados).
- E2E: **no aplica** — el repo no tiene harness E2E persistente (AGENTS.md); la UI se verifica por lógica JVM
  + capturas y esta feature no añade pantalla ni estado visible (solo microcopy, cubierta por JVM).
- Manual (persona, tras el merge; no automatizable desde WSL): con el interruptor apagado el resumen del
  Perfil dice el texto nuevo y ninguna tarjeta o Detalle muestra foto de categoría; encendido, la imagen
  generada sigue apareciendo como capa de fondo.

## Evidence To Capture

- Salida de `bash init.sh` (exit 0) y de la tirada focal, con parseo propio de los XML (**97/731/0/0/0**) y
  del SARIF (0 errores / 13 warnings preexistentes).
- `unzip -l` del APK con el inventario `img/recipes`/`img/dishes`, el tamaño retirado (5.954.912 B ≈ 5,68 MiB)
  y el conservado (137.359 B ancla + 449.844 B semilla).
- Greps de alcance a 0 y `test ! -e scripts/fetch-recipe-photos.sh`; delta de tests (−1 suite / −11) y
  `git diff --stat` solo con lo previsto + docs + spec.

## Validator Checklist

- [ ] El APK no contiene `img/recipes/seed/**` ni `img/recipes/category/**`, y sí el ancla `default.jpg` y
      los 6 `img/dishes/seed/*.webp`.
- [ ] 0 referencias a `AssetRecipePhotoSource`/`RecipePhotoSource`/`LocalRecipePhotoSource` y a los miembros
      borrados de `RecipePhotoAssets` en `app/src`; `MainActivity` sin seam de fotos.
- [ ] `RecipePhotoAssets` conserva solo las tres constantes y `DishImageStyleReferencePolicy` /
      `DishImageSeedAssets` siguen verdes.
- [ ] `DishImageOrigin` sin `CATEGORY`; diagnóstico, caché, tope, interruptor y red **sin cambios** de
      comportamiento (el diff en `data/image`, `data/ai/image` y `ui/artwork` es comentarios + el enum).
- [ ] Microcopy nueva cubierta por test y registrada en `DESIGN.md`; ninguna cadena de UI afirma usar la foto
      local.
- [ ] `docs/domain/taxonomy.json` sin `image` ni `imageResolution` y `RecipeCategory` sin el campo, con las
      suites de Taxonomía/glifos/filtros verdes.
- [ ] Docs durables en pasado/retirada, sin punteros a ficheros borrados; ADR 0006 actualizado; ninguna
      evidencia histórica reescrita.
- [ ] `bash init.sh` verde con 97/731 y lint 0/13; sin dependencias nuevas ni capturas; nada fuera de
      Expected File Changes.
- [ ] `feature_list.json` y `PROGRESS.md` actualizados por el flujo, no por el implementer.

## Findings During Implementation (implementer, sesión 111)

- **Cuarto import retirado en `MainActivity.kt`**: la spec dice "fuera los 3 imports" del seam, pero al
  quitar el `val recipePhotoSource` el import `androidx.compose.ui.graphics.asImageBitmap` (que solo usaba
  la lambda del provider) quedaba huérfano; se retiró también. No cambia el contrato ni el diff esperado
  (es parte de la misma costura).
- **`RecipePhotoAssets.EXTENSIONS` se mantiene y se sigue compartiendo**: `DishImageStyleReferencePolicy` y
  `DishImageSeedAssets` derivan sus extensiones de esa constante, así que el único test que se relajó es el
  assert **redundante** contra `RecipePhotoAssets.EXTENSIONS` en `DishImageStyleReferenceTest`; el literal
  `[webp, jpg, png]` y la cobertura del set semilla (`DishImageSeedAssetsTest`) se conservan.
- **Verificación real**: el gate dio exactamente lo previsto (**97 suites / 731 tests / 0 fallos / 0
  errores / 0 saltados**; lint 0 errores / 13 warnings, 0 bajo `app/src`) y el APK contiene solo
  `assets/img/recipes/default.jpg` (137.359 B) y los 6 `assets/img/dishes/seed/*.webp` (449.844 B). No hubo
  desviaciones de alcance.
