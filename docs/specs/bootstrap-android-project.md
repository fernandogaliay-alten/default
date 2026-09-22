# Feature Implementation Spec: Esqueleto del proyecto Android compilable

## Source Feature

- `id`: `bootstrap-android-project`
- `area`: `platform`
- `depends_on`: `[]` (sin prerrequisitos)
- `status`: `not_started` (en el momento de planificar)
- `source`: `feature_list.json`

## Goal

Dejar el repositorio con un esqueleto Android **ejecutable**: el proyecto compila, la app vacía
arranca en un emulador y `init.sh` deja de ser provisional para convertirse en la puerta de
verificación real del repositorio (`./gradlew test` y `./gradlew lint`, sin arrancar procesos
largos).

Esta feature no entrega producto: entrega **capacidad de verificar**. A partir de aquí, cada feature
posterior podrá demostrarse con evidencia objetiva en lugar de con afirmaciones.

## Non-Goals

- Ninguna feature de producto: ni Taxonomía, ni modelo de Receta, ni Room, ni onboarding, ni
  pantallas.
- **No añadir Room** ni ninguna dependencia que no se use todavía. Room entra con
  `profile-persistence`, no aquí. (Corrige lo dicho en conversación previa: declarar una dependencia
  sin uso es ruido, no preparación.)
- No integrar la API de IA ni manejar la clave.
- No crear assets (iconos, imágenes de receta, catálogo semilla).
- No tests instrumentados de Compose ni suite E2E.
- No CI ni configuración de despliegue.
- No cerrar el nombre definitivo del producto: el paquete queda en `com.sazon.app` y el nombre
  visible en "Sazón", ambos revisables antes de publicar.

## Job Story

Cuando empiezo un proyecto Android,
quiero un esqueleto que compile y una puerta de verificación real,
para poder construir cada feature con evidencia objetiva desde el primer día.

## Users And Permissions

- **Desarrollador / agente**: puede crear y modificar el proyecto, ejecutar Gradle y lanzar la app.
  No hay autenticación ni roles (ADR 0002).
- **Usuario local**: no interactúa con esta feature; solo verá una pantalla vacía al arrancar.

## Acceptance Scenarios

### Scenario 1: el gate real sustituye al provisional

Given el repositorio sin ficheros de proyecto Android,
When existe el esqueleto y se ejecuta el camino de arranque,
Then `init.sh` ya no imprime "pre-bootstrap", ejecuta `./gradlew test` y `./gradlew lint`, y termina
con código 0.

### Scenario 2: el proyecto compila

Given el esqueleto creado,
When se ejecuta `./gradlew assembleDebug`,
Then se genera un APK de depuración sin errores.

### Scenario 3: la app arranca vacía

Given un emulador disponible,
When se instala y lanza la app,
Then se muestra una pantalla sin ningún texto de producto (ni "Hello Android" ni equivalente).

### Scenario 4: fallo detectable cuando falta el SDK

Given que este equipo **no** tiene `ANDROID_HOME` ni `ANDROID_SDK_ROOT` definidos,
When se ejecuta `./gradlew test` desde la terminal sin `local.properties`,
Then Gradle falla con "SDK location not found", y la feature se considera incompleta hasta que el
esqueleto resuelva la localización del SDK.

### Scenario 5: el gate no arranca procesos largos

Given el repositorio en verde,
When se ejecuta `init.sh`,
Then el script termina por sí solo y no deja ningún emulador ni servidor en marcha.

## Repository Research

### Files Inspected

- `AGENTS.md` — flujo de arranque, reglas de trabajo y notas de entorno (incluye que `bash` no está
  en el PATH).
- `PROGRESS.md` — estado verificado actual y sesión 001.
- `feature_list.json` — 26 features; esta es la primera y la única sin dependencias.
- `init.sh` — variante provisional pre-bootstrap, que esta feature debe reemplazar.
- `.gitignore` — ya ignora `local.properties`, `build/`, `.gradle/`, `.idea/`.
- `docs/technical-discovery.md` — stack, almacenamiento, verificación y restricciones.
- `docs/risks-and-open-questions.md` — preguntas abiertas y riesgos.
- `DESIGN.md` — dirección visual (no aplica a esta feature).
- `docs/adr/0001-taxonomy-in-code.md`, `docs/adr/0002-local-first-no-backend.md` — decisiones que
  condicionan el stack.

### Existing Patterns To Follow

- Documentación y comentarios en **español**; identificadores de código en inglés.
- `init.sh` es un script **bash**, no PowerShell. En este equipo se invoca con
  `& "C:\Program Files\Git\bin\bash.exe" init.sh` porque `bash` no está en el PATH.
- El repositorio usa Kotlin DSL para Gradle (`build.gradle.kts`) según `technical-discovery.md`.
- `local.properties` está fuera de control de versiones y es donde vivirá también la clave de IA.

### Current Gaps

- **No existe ningún fichero de proyecto Android**: ni `settings.gradle.kts`, ni `build.gradle.kts`,
  ni `gradlew`, ni `app/`, ni `gradle/`.
- **`ANDROID_HOME` y `ANDROID_SDK_ROOT` están vacíos** (comprobado en variables de máquina y de
  usuario). El SDK existe en `%LOCALAPPDATA%\Android\Sdk` con plataformas `android-26` a `android-35`
  y build-tools hasta `36.0.0`, además de `cmdline-tools`, `emulator` y `platform-tools`.
- **`java` en el PATH es JDK 11**, insuficiente para Android Gradle Plugin 8.x. `JAVA_HOME` apunta
  al JBR de Android Studio (`C:\Program Files\Android\Android Studio\jbr`), que es **JDK 17.0.6**.
  Gradle respeta `JAVA_HOME`, así que el build debe funcionar, pero cualquier herramienta que use
  `java` del PATH sin mirar `JAVA_HOME` fallará.
- **`gradle` no está en el PATH**; el wrapper generado por el asistente es la única vía prevista.
- No existe comando E2E persistente ni suite de tests: esta feature **crea** la primera puerta real.

## Technical Approach

Se reparte entre el humano y el agente, porque crear el proyecto con el asistente evita pelearse con
la configuración de Gradle, que no es el objetivo del curso.

1. **Humano — asistente de Android Studio**, con estas opciones fijas:
   - Plantilla: `Empty Activity` (Compose). No `Empty Views Activity` ni plantillas con navegación.
   - Lenguaje: Kotlin. Lenguaje de configuración de build: Kotlin DSL.
   - Nombre: `Sazón`. Paquete / `applicationId`: `com.sazon.app`.
   - `minSdk`: 33, `targetSdk`: 36, `compileSdk`: 36.
2. **Agente — dejar el esqueleto mínimo y veraz**:
   - Reducir `MainActivity` a una `Surface` vacía, sin texto de producto.
   - Garantizar la localización del SDK: `local.properties` con `sdk.dir` apuntando a
     `%LOCALAPPDATA%\Android\Sdk`, ya que no hay variables de entorno. El fichero está ignorado por
     git y no debe versionarse.
   - Sustituir `init.sh` por su variante real, sin arrancar procesos largos.
   - Conservar el test unitario de plantilla como primera puerta en verde, y borrarlo cuando exista
     el primer test real (`taxonomy-in-code`).
   - No tocar `.gitignore` salvo que el asistente genere rutas no cubiertas.

## Expected File Changes

Rutas **provisionales**: el proyecto no existe todavía y el asistente decide parte de la estructura.

- `settings.gradle.kts` — crear; configuración raíz de Gradle (asistente).
- `build.gradle.kts` — crear; build raíz (asistente).
- `gradle/libs.versions.toml` — crear; catálogo de versiones (asistente).
- `gradlew`, `gradlew.bat`, `gradle/wrapper/**` — crear; wrapper de Gradle (asistente).
- `app/build.gradle.kts` — crear; módulo de aplicación (asistente).
- `app/src/main/AndroidManifest.xml` — crear; manifiesto con la actividad lanzadora (asistente).
- `app/src/main/java/com/sazon/app/MainActivity.kt` — modificar; reducir a `Surface` vacía.
- `app/src/test/java/com/sazon/app/ExampleUnitTest.kt` — conservar; primera puerta en verde.
- `local.properties` — crear; `sdk.dir`. **No versionado.**
- `init.sh` — modificar; sustituir la variante provisional por la real.
- `AGENTS.md` — modificar; el camino de arranque deja de ser provisional.
- `PROGRESS.md` — modificar; estado verificado y evidencia.
- `feature_list.json` — modificar; `status` y `evidence` de esta feature.

## Visual Design Impact

- UI involved: **sí, mínima**. Una única pantalla vacía, sin producto.
- Design source: **no aplica** todavía. `DESIGN.md` describe las pantallas de producto, que no
  forman parte de esta feature.
- Screens or states affected: la actividad de arranque, que debe verse vacía.
- New design artifact required: **no**. Sería un error inventar aquí el estilo visual; llega con la
  primera pantalla real (`onboarding-shell-and-identity`).

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio omite deliberadamente este fichero
  (`harness-starter` prohíbe documentos extra). La arquitectura vive en
  `docs/technical-discovery.md` y en los ADRs.
- `CONSTRAINTS.md`: **not needed** — esta feature no introduce ninguna regla MUST/MUST NOT nueva;
  las reglas vigentes ya están en `AGENTS.md`.
- `AGENTS.md`: **update** — el camino de arranque cambia: `init.sh` deja de ser provisional y pasa a
  ser la puerta real. Además conviene registrar los hechos de entorno descubiertos aquí (JDK 11 en el
  PATH frente a JBR 17 en `JAVA_HOME`; SDK sin variables de entorno).
- Other docs: `docs/technical-discovery.md` — update; confirmar que `minSdk` 33 y `com.sazon.app`
  quedan materializados y anotar la localización del SDK. `PROGRESS.md` y `feature_list.json` —
  update; estado y evidencia.

## Implementation Plan

1. Crear el proyecto con el asistente usando exactamente las opciones de la sección Technical
   Approach.
2. Reducir `MainActivity` a una `Surface` vacía y confirmar que no queda texto de plantilla.
3. Asegurar `local.properties` con `sdk.dir` y verificar el build desde terminal.
4. Sustituir `init.sh` por la variante real: comprobaciones no bloqueantes, sin procesos largos.
5. Ejecutar la verificación completa y actualizar `feature_list.json`, `PROGRESS.md`,
   `docs/technical-discovery.md` y `AGENTS.md` con la evidencia.

## Implementation Tasks

- [ ] Crear el proyecto Android con el asistente (Empty Activity, Compose, Kotlin DSL, minSdk 33,
      paquete `com.sazon.app`).
- [ ] Reducir `MainActivity` a una `Surface` vacía, sin texto de producto.
- [ ] Crear `local.properties` con `sdk.dir` apuntando al SDK local (no versionado).
- [ ] Ejecutar `./gradlew test` y `./gradlew assembleDebug` desde la terminal.
- [ ] Sustituir `init.sh` por su variante real y darle el bit de ejecución (`git add --chmod=+x`).
- [ ] Arrancar la app en un emulador y comprobar que la pantalla está vacía.
- [ ] Actualizar `AGENTS.md`, `docs/technical-discovery.md`, `PROGRESS.md` y `feature_list.json`
      con el estado y la evidencia.

## Verification Plan

Comandos disponibles en el estado actual del repositorio (todos desde la raíz):

- `./gradlew test` → **esperado**: pasa en verde (incluye el test unitario de plantilla).
- `./gradlew lint` → **esperado**: sin errores.
- `./gradlew assembleDebug` → **esperado**: genera el APK de depuración.
- Camino de arranque → **esperado**: código 0 y sin el mensaje "pre-bootstrap". En este equipo:
  `& "C:\Program Files\Git\bin\bash.exe" init.sh`.
- Manual en emulador: instalar y lanzar la app → **esperado**: pantalla vacía. Hay AVDs de API 33
  instalados (`Pixel_7a_API_33`, `Pixel_4_API_33`), compatibles con `minSdk` 33.

Sobre `init.sh` (regla del harness):

- Debe ejecutar las comprobaciones **no bloqueantes** del estado actual del repositorio
  (`./gradlew test` y `./gradlew lint`).
- **No debe** arrancar procesos largos: ni el emulador, ni un servidor de desarrollo.
- Puede imprimir al final el comando manual para lanzar la app en el emulador, pero sin ejecutarlo.

Cobertura E2E: **no aplica**. No existe suite E2E persistente, y esta feature no cambia
comportamiento de producto, autenticación, rutas ni API. La comprobación en emulador es manual y
queda como evidencia; añadir tests instrumentados de Compose aquí sería alcance fuera de la feature.

## Evidence To Capture

- Salida de `./gradlew test` en verde.
- Salida de `./gradlew lint` sin errores.
- Salida del camino de arranque con código 0 y sin mensaje pre-bootstrap.
- Captura de pantalla de la app arrancada y vacía.
- Confirmación de que `local.properties` existe y de que `git status` no lo lista (ignorado).

## Validator Checklist

- [ ] La implementación se mantiene dentro del alcance de la feature (sin producto, sin Room, sin
      assets, sin IA).
- [ ] Los cinco escenarios de aceptación pasan.
- [ ] La evidencia de verificación está presente en `feature_list.json` o `PROGRESS.md`.
- [ ] `init.sh` ejecuta comprobaciones no bloqueantes y **no** arranca procesos largos.
- [ ] `local.properties` no está versionado y `.gitignore` lo cubre.
- [ ] `AGENTS.md`, `docs/technical-discovery.md`, `PROGRESS.md` y `feature_list.json` están
      actualizados de forma coherente.
- [ ] No se ha añadido ninguna feature de producto por la puerta de atrás (por ejemplo, una pantalla
      de bienvenida "provisional").

## Findings During Implementation

La spec se corrige aquí, no en silencio:

1. **Versiones de SDK durante la implementación.** Se probó `targetSdk` 33 y se revirtió a **36**:
   el asistente ya lo dejaba en 36 y no hay motivo para bajarlo. Resultado final: `minSdk` 33,
   `targetSdk` 36, `compileSdk` 36. `compileSdk` no puede ser 33 porque AGP 9.3.3 exige uno reciente.
2. **El asistente puso `targetSdk` 36 por defecto.** Se mantiene.
3. **`ObsoleteSdkInt` en `Theme.kt`.** El chequeo `SDK_INT >= S` pasó a ser siempre cierto con
   `minSdk` 33. Eliminado (refactor sin cambio de comportamiento) junto con el import de `Build`.
   Es un fichero fuera de la lista de cambios prevista, tocado por ser consecuencia directa del
   `minSdk` declarado.
4. **`strings.xml` decía `Sazon` sin tilde.** Corregido a `Sazón`.
5. **`local.properties` ya existía** con `sdk.dir`, así que el hueco detectado durante la
   investigación lo resolvió el asistente. Sigue fuera de control de versiones.
6. **`bash` no estaba en el PATH de la sesión**, así que el gate se ejecutó como
   `& "C:\Program Files\Git\bin\bash.exe" init.sh`.

Advertencias de lint que **no** se corrigen aquí, por ser ruido de plantilla que resolverá la feature
de tokens de diseño: 7 recursos de color sin usar, 7 avisos de versiones de dependencias, 1 de versión
de AGP y 1 `RedundantLabel` en el manifiesto.
