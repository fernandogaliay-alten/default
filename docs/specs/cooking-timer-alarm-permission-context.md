# Feature Implementation Spec: Permiso de notificaciones en contexto y con explicación

## Source Feature

- `id`: `cooking-timer-alarm-permission-context`
- `area`: `ui`
- `depends_on`: `["cooking-timer-alarm"]` (`accepted`: el aviso de fin del temporizador ya existe con su
  canal `cooking_timer`, su alarma exacta y su receptor, y este trabajo **reutiliza** ese mecanismo sin
  tocarlo).
- `status`: `not_started` en `feature_list.json` al planificar. Esta spec es el contrato planner →
  implementer → validator. El planner **no** modifica `feature_list.json` ni `PROGRESS.md`.
- `source`: `feature_list.json` (líneas 1668-1682).
- Verificación exigida por la entrada:
  - `bash init.sh` (test + lint) en verde, **sin** dependencias nuevas y sin tocar build/lockfile.
  - Test **JVM** de la decisión **pura** de pedir el permiso (sin Android/Compose, patrón
    `CookingAlarmPolicy`, permiso e instante **inyectados**): al arrancar la app NO se pide; al arrancar
    el temporizador por primera vez y sin permiso concedido se pide **una** vez y con rationale; con
    permiso concedido no se pide; denegado no se vuelve a pedir en la misma sesión.
  - Prueba **manual** (persona, emulador; no automatizable según `AGENTS.md`): abrir la app **no**
    muestra el diálogo; al pulsar "Iniciar" por primera vez aparece **primero** la explicación y
    **después** el diálogo del sistema; denegarlo deja el Modo cocina y el "¡Listo!" en pantalla
    intactos.
- Correctiva pedida por la persona (2026-10-15, sesión 135) tras probar `cooking-timer-alarm` en el
  emulador: el diálogo de `POST_NOTIFICATIONS` **salta al abrir la app y sin ninguna explicación**.

## Goal

El permiso de notificaciones deja de pedirse al arrancar. Se pide **en contexto**: la **primera vez**
que el usuario arranca el temporizador de un paso del Modo cocina, y siempre **después** de un
**rationale** breve que explica para qué sirve (avisar cuando termina el paso). Si el usuario lo
deniega, **nada** del Modo cocina cambia: el temporizador, la cuenta atrás, el aviso de sistema (si ya
lo hubiera) y el "¡Listo!" en pantalla siguen igual.

La pieza central es, otra vez, una **decisión pura y testeable en JVM**: una política sin Compose ni
Android y **sin leer el reloj** (el permiso y el instante entran inyectados) que, a partir de la
transición del [`CookingTimerState`] ya aceptado y del estado inyectado del permiso, decide si hay que
mostrar el rationale. El adaptador Android (consulta del permiso + lanzador del diálogo del sistema) y
el diálogo Compose son la capa delgada de fuera.

## Non-Goals

- **No** entra el `foreground service`, ni la notificación persistente con la cuenta atrás, ni
  "volver al paso" / persistir la sesión del Modo cocina: eso es `cooking-timer-foreground-service`
  (la feature siguiente, que depende de esta).
- **No** se cambia **nada** del mecanismo aceptado del aviso: `CookingAlarmPolicy`, `CookingAlarm`,
  `data/cooking/**` (alarma, receptor, canal, contenido, id `1001`), los permisos del manifiesto ni el
  "¡Listo!" en pantalla (`CookingTimerState`, `CookingModeState`, `CookingModeFormat` intactos).
- **No** se cambia la **lógica del temporizador** ni sus transiciones, ni se **gatea** el temporizador:
  el reloj y la alarma arrancan igual aunque el rationale o el diálogo del sistema estén en pantalla.
- **No** se implementa `shouldShowRequestPermissionRationale`, ni un cooldown entre intentos, ni
  persistir el "ya se pidió" entre procesos (la memoria es **por sesión**, como hoy): si molesta,
  será una correctiva propia.
- **No** se añaden dependencias (`AlertDialog` es Material 3, ya presente; `ContextCompat` es
  `androidx.core`), ni `strings.xml` de microcopy (el repo no lo usa para UI), ni `DataStore`, ni DI,
  ni librería de navegación.
- **No** entra: recordatorios programados, notificaciones de sugerencias/recetas, lista de la compra,
  multi-idioma, ni rediseñar ninguna pantalla.
- **No** se crea captura de Stitch ni `docs/design/concepts/cooking-timer-alarm-permission-context/`
  (un rationale de permiso no es una pantalla diseñada; se registra la excepción en `DESIGN.md`).

## Job Story

Cuando estoy empezando a cocinar y arranco el temporizador de un paso,
quiero que la app me pida permiso para avisarme **en ese momento** y sabiendo para qué,
para no recibir un diálogo del sistema al abrir la app que no entiendo.

## Users And Permissions

- **Usuario local**: dueño único del dispositivo. Abre la app (sin diálogo), entra en una receta,
  "Empezar a cocinar" y pulsa "Iniciar" en un paso con duración: ve primero el rationale y después el
  diálogo del sistema; puede conceder o denegar.
- **Permiso `POST_NOTIFICATIONS`** (runtime, Android 13+ = todo el rango soportado, `minSdk = 33`): se
  pide **en contexto** —la primera vez que arranca el temporizador de un paso, tras un rationale— y
  **una sola vez por sesión** (proceso). **Denegado** o descartado el rationale: no se vuelve a pedir
  en la sesión y el Modo cocina, la cuenta atrás y el "¡Listo!" en pantalla siguen **intactos**
  (degradación silenciosa, ADR 0005/0007).
- **`USE_EXACT_ALARM` / `VIBRATE` / `SCHEDULE_EXACT_ALARM (maxSdkVersion=32)`**: sin cambios (los
  declara `cooking-timer-alarm`).
- **Sistema operativo**: muestra el diálogo del permiso cuando el lanzador lo invoca; si el usuario ya
  lo denegó de forma permanente, el sistema simplemente no lo muestra (no hay crash).

## Acceptance Scenarios

### Scenario 1: abrir la app no pide nada (verificación exigida)

Given una instalación sin `POST_NOTIFICATIONS` concedido y sin haber arrancado nunca el temporizador,
When el usuario abre la app y navega (Home, Favoritos, Perfil, una receta, el Generador),
Then **no** se muestra el rationale ni el diálogo del sistema: no queda en `MainActivity` ninguna
llamada de petición al arrancar.

### Scenario 2: primer arranque del temporizador ⇒ rationale y después diálogo del sistema (verificación exigida)

Given el Modo cocina abierto en un paso de `minutes = 10`, el permiso sin conceder y la sesión sin
haber pedido todavía,
When el usuario pulsa "Iniciar",
Then la política pura devuelve `AskWithRationale`, se muestra el **rationale** (título, texto y las dos
acciones de `NotificationPermissionCopy`) y, solo al pulsar "Continuar", se lanza el diálogo del
sistema; el temporizador ya está **En marcha** (el rationale no lo bloquea) y la alarma del aviso ya
está programada.

### Scenario 3: permiso ya concedido ⇒ ni rationale ni diálogo

Given `POST_NOTIFICATIONS` concedido (por ejemplo, en una segunda sesión),
When el usuario pulsa "Iniciar",
Then `NotificationPermissionPolicy.onTimerChange` devuelve `None` y no se muestra nada.

### Scenario 4: "Ahora no" no vuelve a molestar en la sesión (verificación exigida)

Given el rationale mostrado y el usuario pulsando "Ahora no" (o descartándolo),
When el usuario pausa, reanuda, cambia de paso y vuelve a arrancar el temporizador, o sale del Modo
cocina y entra otra vez dentro de la misma sesión,
Then **no** se lanza el diálogo del sistema ni se vuelve a mostrar el rationale (el intento queda
registrado con `markAsked(instante)`), y el Modo cocina funciona igual.

### Scenario 5: permiso denegado ⇒ Modo cocina intacto (verificación exigida)

Given el diálogo del sistema denegado,
When el temporizador llega a cero,
Then no hay crash ni diálogo nuevo; el "¡Listo!" en pantalla aparece igual y, con la app en segundo
plano, el aviso de sistema simplemente no se publica (comportamiento aceptado: no se toca ni el
receptor ni `CookingNotifications`).

### Scenario 6: la petición no gatea el temporizador

Given el rationale o el diálogo del sistema en pantalla,
When el temporizador avanza,
Then la cuenta atrás sigue descontando y la alarma de fin sigue programada para el instante correcto:
`alarm.apply(...)` y la petición del permiso conviven en el mismo efecto y ninguna espera a la otra.

### Scenario 7: decisión pura, determinista y sin reloj (verificación exigida)

Given dos transiciones del temporizador y un `NotificationPermissionState` inyectado,
When se evalúa `NotificationPermissionPolicy.onTimerChange(previous, next, state)`,
Then el resultado es **determinista**, **no lee el reloj**, no muta nada y solo puede ser
`AskWithRationale` (arista `IDLE→RUNNING` o `PAUSED→RUNNING` con permiso sin conceder y sin intento
previo) o `None` (ticks, no-ops, permiso concedido, ya pedido en la sesión o cualquier transición que
no sea "arrancar").

### Scenario 8: abrir el Modo cocina, un paso sin duración o salir no piden nada

Given el Modo cocina abierto (temporizador IDLE) o un paso con `minutes <= 0`,
When se compone la pantalla (y, en su caso, se recorre el paso),
Then `onTimerChange` devuelve `None` en la **primera** composición (no es una transición real) y, sin
"Iniciar" alcanzable, nunca se muestra el rationale.

### Scenario 9: el texto del repo deja de contradecir el código (verificación exigida)

Given el repo tal y como queda tras esta feature,
When se leen `docs/specs/cooking-timer-alarm.md`, `docs/adr/0007-cooking-timer-alarm.md`,
`docs/technical-discovery.md`, `docs/risks-and-open-questions.md`, `PROGRESS.md` y
`app/src/main/AndroidManifest.xml`,
Then **ninguna** afirmación vigente dice que el permiso se pide "una vez por proceso al arrancar": todas
apuntan a la petición en contexto con rationale (ver `Reconciliation Of The Incoherent Text`).

## Reconciliation Of The Incoherent Text

Hecho comprobado en el repo (búsqueda propia de `contextual` y de `POST_NOTIFICATIONS`):

- La **única** aparición de "contextual" describe el **parcial interrumpido** que el implementer
  descartó en la sesión 131 (`PROGRESS.md`, sesión 131: "peticion del permiso contextual en 'Iniciar'",
  dentro de la lista de cosas del parcial que se alinearon con la spec). **No** aparece en la evidencia
  de `feature_list.json`.
- La evidencia de `cooking-timer-alarm` (`feature_list.json` líneas 1654-1664) y su spec (§Users And
  Permissions, líneas 64-67) dicen, con razón, **"una vez por proceso al arrancar"**, que es lo que el
  código hacía. El hueco es de **redacción**: el log de la sesión 131 deja un "contextual" suelto que se
  lee como comportamiento entregado, y las frases de estado (spec, ADR 0007 decisión 11 y alternativa
  9, `technical-discovery`, `risks`, comentario del manifiesto) quedan **obsoletas** en cuanto esta
  correctiva cambie el comportamiento.

**Corrección mínima y en alcance** (la decide esta spec; **no** se reescribe la evidencia aceptada):

1. `docs/specs/cooking-timer-alarm.md`: añadir **una** nota de vigencia (p. ej. al cerrar `Source
   Feature`) y actualizar **solo** las frases que afirman el arranque: el bullet de
   `POST_NOTIFICATIONS` de `Users And Permissions` (líneas 64-67), el bullet de `MainActivity` del
   `Technical Approach` §3 (líneas 352-355), la decisión de `Decisions And Unknowns` (línea 573) y el
   ítem del `Validator Checklist` (línea 619). Los escenarios, el plan y las casillas quedan como
   registro histórico.
2. `docs/adr/0007-cooking-timer-alarm.md`: añadir una línea de **enmienda** a la decisión 11 y marcar
   la alternativa 9 ("petición contextual") como **superada** por esta correctiva, con el motivo
   (diálogo al arrancar sin explicación = defecto de UX). El resto del ADR no cambia.
3. `docs/technical-discovery.md` (párrafo del aviso, líneas 417-419) y `docs/risks-and-open-questions.md`
   (fila del riesgo de permiso denegado, línea 175; bullet del ADR 0007, línea 228): sustituir "se pide
   una vez por proceso al arrancar" por "se pide en contexto, al arrancar el temporizador de un paso,
   tras un rationale".
4. `PROGRESS.md`, sesión 131: añadir **un paréntesis** aclaratorio en la frase del parcial descartado
   ("contextual" describía el parcial; el comportamiento aceptado entonces pedía el permiso una vez por
   proceso al arrancar — ver esta correctiva). No se reescribe ningún registro ni evidencia.
5. `app/src/main/AndroidManifest.xml`: **solo** el comentario de `POST_NOTIFICATIONS` (líneas 12-19)
   pasa a describir la petición en contexto. **Los permisos y el receptor no cambian.**

`feature_list.json` de `cooking-timer-alarm` **no** se toca (su evidencia es el registro fiel de lo
validado el 2026-10-15); la reconciliación se registra en la evidencia de **esta** feature.

## Repository Research

### Files Inspected

- `feature_list.json` (líneas 1640-1682) — entradas `cooking-timer-alarm` (verificación, evidencia y
  notes) y `cooking-timer-alarm-permission-context` (alcance exacto de la correctiva y su no-goal del
  `foreground service`).
- `PROGRESS.md` (líneas 13-42, 6274-6446) — estado del aviso aceptado; sesión 131 (implementación y
  parcial descartado), sesión 132 (validación `accept`) y **sesión 135** (triage de la persona: diálogo
  al arrancar sin explicación; alta de esta correctiva).
- `docs/specs/cooking-timer-alarm.md` — contrato aceptado del aviso: §Users And Permissions (líneas
  60-76), §3 del `Technical Approach` (líneas 320-357) y las decisiones (568-577) son justo las frases
  a reconciliar; el mecanismo (canal, alarma, receptor) es el que se **reutiliza**.
- `docs/validations/cooking-timer-alarm.md` — veredicto `accept` y §Scenario 6: la petición se
  documenta como "no bloqueante y una vez por proceso" (`MainActivity.kt:60-66,268-283`).
- `docs/adr/0007-cooking-timer-alarm.md` — decisión **11** ("el permiso se pide una vez por proceso al
  arrancar") y alternativa **9** ("petición contextual… descartada") son las que esta feature invierte.
- `app/src/main/java/com/sazon/app/MainActivity.kt` — el lanzador `requestNotificationsPermission`
  (líneas 60-67, registrado **antes** de `onCreate`) y la llamada al arrancar
  `requestNotificationsPermissionOncePerProcess()` (línea 229) con el flag de compañía `@Volatile`
  (líneas 262-283): es lo que se retira; `CookingNotifications.ensureChannel` (línea 228) se mantiene.
- `app/src/main/java/com/sazon/app/ui/cooking/CookingAlarm.kt` — patrón exacto a imitar: comando
  sellado, política **pura** con instantes inyectados y costura con default no-op.
- `app/src/main/java/com/sazon/app/ui/cooking/CookingModeScreen.kt` — punto de disparo: el
  `LaunchedEffect(state.timer.status)` (líneas 116-125) con `previousTimer` y
  `SystemClock.elapsedRealtime()`, y el `DisposableEffect` de salida (127-131).
- `app/src/main/java/com/sazon/app/ui/cooking/CookingTimerState.kt` — `isRunning`, `hasDuration`,
  `start()`/`resume()`/`pause()`/`reset()`/`tick()` con no-ops: la arista "arrancar" que dispara la
  petición es `!previous.isRunning && next.isRunning`.
- `app/src/main/java/com/sazon/app/ui/SazonApp.kt` / `ui/MainShell.kt` — la costura viaja **por
  parámetro** (`cookingAlarm` con default `NoOpCookingAlarm`, `SazonApp` líneas 57 y 98-99, `MainShell`
  líneas 122 y 344): la nueva costura sigue el mismo camino.
- `app/src/main/java/com/sazon/app/data/cooking/CookingNotifications.kt` +
  `AndroidCookingAlarm.kt` — estilo de adaptador Android ("no lanza", KDoc que justifica permisos) y
  vecindad del paquete para el adaptador nuevo.
- `app/src/test/java/com/sazon/app/ui/cooking/CookingAlarmPolicyTest.kt` — estilo de los tests JVM
  puros (JUnit 4, nombres en español, vectores dorados, casos negativos) que el test nuevo imita.
- `app/src/test/java/com/sazon/app/data/cooking/AndroidCookingAlarmTest.kt` +
  `CookingNotificationsTest.kt` — estilo Robolectric (`@RunWith(RobolectricTestRunner::class)`,
  `@Config(sdk = [33])`, `ApplicationProvider`, `shadowOf(application).grantPermissions/denyPermissions`).
- `app/src/main/AndroidManifest.xml` — permisos (líneas 12-30) y receptor (55-57): **solo** cambia el
  comentario; `POST_NOTIFICATIONS` ya está declarado.
- `app/build.gradle.kts` (líneas 118-126) — `robolectric` y `androidx.test.core` ya están en
  `testImplementation`; `compose-ui-test` solo en `androidTest`: **no** hay tests instrumentados.
- `docs/technical-discovery.md` (líneas 395-424) y `docs/risks-and-open-questions.md` (líneas 175,
  223-230) — frases a reconciliar.
- `DESIGN.md` (`Generated Concept Images` 133-312, `Components` 402+, `Do's and Don'ts` 673-688) — no
  hay sección de diálogos ni tokens de diálogo; sí la regla "una sola acción principal por pantalla" y
  "no inventar pantallas o flujos".
- `CONTEXT.md` — vocabulario del dominio: "Modo cocina" y "Aviso" ya existen; la petición del permiso
  no introduce término nuevo.

### Existing Patterns To Follow

- **Lógica pura + adaptador Android delgado**: la decisión vive en `ui.cooking` (sin `android.*` ni
  `androidx.*`); el acceso al sistema (permiso + lanzador) en `data/cooking`.
- **Costura con default no-op**: `interface` + `object NoOpX` (patrón `NoOpCookingAlarm`,
  `NoOpDishImageSettingsStore`) para que previews, tests y composiciones sin cableado compilen y no
  pidan nada.
- **Inyección total en lo puro**: el permiso y el instante entran por parámetro; nada de reloj
  (`CookingAlarmPolicy`, `CookingTimerState`).
- **Degradación silenciosa** (ADR 0005): el adaptador "no lanza"; denegar el permiso no rompe nada.
- **Microcopy como constantes puras en Kotlin**, no `strings.xml` (el repo no usa recursos de
  microcopy de UI; el contenido del aviso aceptado también es un `const val`).
- **Tests JUnit 4 con nombres en español** y casos negativos; **Robolectric** solo para framework.
- **Sin dependencias nuevas**; `init.sh` sin cambios.

### Current Gaps

- La petición está **atada al arranque** (`MainActivity.onCreate` → `requestNotificationsPermissionOncePerProcess`),
  con un flag de compañía `@Volatile notificationsPermissionRequested`: no hay ninguna costura que la
  capa de UI pueda invocar en contexto.
- No existe **ningún** diálogo en el Modo cocina ni en la app completa (`AlertDialog` no aparece en
  `app/src/main`; el único supervisor es `ModalBottomSheet` del Perfil).
- No hay **memoria de sesión** de la petición fuera de `MainActivity` (el flag de compañía), así que
  tampoco hay forma de que la decisión pura la reciba inyectada.
- No hay test alguno de la petición del permiso (ni JVM ni Robolectric): la cobertura de este
  comportamiento empieza de cero.

## Technical Approach

Estrategia: **política pura + costura no-op + adaptador Android de plataforma + rationale Compose**,
con el mínimo cambio en la pantalla aceptada del Modo cocina.

### 1. `ui/cooking/NotificationPermission.kt` — estado, comando, microcopy y política (puros)

```kotlin
/** Estado **inyectado** de la peticion: permiso actual + instante del ultimo intento de esta sesion. */
data class NotificationPermissionState(
    val granted: Boolean,
    val askedAtMillis: Long? = null,
) {
    /** Registra el intento con el instante **inyectado** (nunca lee el reloj). */
    fun asked(atMillis: Long): NotificationPermissionState = copy(askedAtMillis = atMillis)
}

/** Que debe hacer la capa Compose. */
sealed interface NotificationPermissionCommand {
    /** Mostrar el rationale y, si el usuario acepta, lanzar el dialogo del sistema. */
    data object AskWithRationale : NotificationPermissionCommand

    /** Nada que hacer. */
    data object None : NotificationPermissionCommand
}

/** Microcopy del rationale (constantes puras: sin recursos, sin `strings.xml`). */
object NotificationPermissionCopy {
    const val TITLE = "Avisos del temporizador"
    const val MESSAGE =
        "Para avisarte cuando termine el paso, aunque no estes en la app, Sazon necesita permiso " +
            "para enviarte notificaciones."
    const val CONFIRM = "Continuar"
    const val DISMISS = "Ahora no"
}

/** Politica **pura** (sin Compose ni Android) que decide si hay que pedir el permiso. */
object NotificationPermissionPolicy {
    fun onTimerChange(
        previous: CookingTimerState,
        next: CookingTimerState,
        state: NotificationPermissionState,
    ): NotificationPermissionCommand = when {
        !(next.isRunning && !previous.isRunning) -> NotificationPermissionCommand.None
        state.granted -> NotificationPermissionCommand.None
        state.askedAtMillis != null -> NotificationPermissionCommand.None
        else -> NotificationPermissionCommand.AskWithRationale
    }
}
```

- La **arista de disparo** es la de "arrancar el temporizador" (`!previous.isRunning &&
  next.isRunning`), igual que el `Schedule` de `CookingAlarmPolicy`: la primera composición
  (`IDLE == previous == next`), los ticks, Pausar, Reiniciar, cambiar de paso, llegar a cero y salir
  devuelven `None`.
- `askedAtMillis` es la **memoria de sesión inyectada**: `null` = no se ha pedido todavía;
  no-nulo = ya hubo intento. El **instante** se guarda (no se lee en lo puro) y hoy solo distingue
  "no pedido" de "ya pedido": deja la puerta abierta a un cooldown futuro sin cambiar el contrato.

### 2. `ui/cooking/NotificationPermissionPrompt.kt` — costura (pura, sin Android)

```kotlin
/** Costura del permiso: entrega el estado **inyectado** y lanza el dialogo del sistema. */
interface NotificationPermissionPrompt {
    /** Estado de esta sesion: permiso actual + instante del ultimo intento (`null` = nunca). */
    fun state(): NotificationPermissionState

    /** Lanza el dialogo del sistema. Best-effort: nunca lanza. */
    fun request()

    /** Registra el intento con el instante **inyectado** (memoria de sesion). */
    fun markAsked(atMillis: Long)
}

/** Default **sin efecto**: declara el permiso como concedido para que nada se pida. */
object NoOpNotificationPermissionPrompt : NotificationPermissionPrompt {
    override fun state(): NotificationPermissionState = NotificationPermissionState(granted = true)
    override fun request() = Unit
    override fun markAsked(atMillis: Long) = Unit
}
```

### 3. `data/cooking/AndroidNotificationPermissionPrompt.kt` — adaptador Android

```kotlin
class AndroidNotificationPermissionPrompt(
    context: Context,
    private val launch: (String) -> Unit, // `requestNotificationsPermission::launch`
) : NotificationPermissionPrompt {
    private val appContext = context.applicationContext
    private var askedAtMillis: Long? = null // memoria de sesion (proceso): una vez por sesion

    override fun state() = NotificationPermissionState(
        granted = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED,
        askedAtMillis = askedAtMillis,
    )

    override fun request() {
        runCatching { launch(Manifest.permission.POST_NOTIFICATIONS) } // nunca lanza
    }

    override fun markAsked(atMillis: Long) { askedAtMillis = atMillis }
}
```

- La **memoria de sesión** del flag de compañía que se retira de `MainActivity` pasa a vivir aquí (una
  instancia por proceso, creada en `onCreate`), sin estado estático.
- `state()` es la **inyección**: lee el permiso del sistema y lo entrega tal cual a la política pura.
- `request()` reutiliza el `ActivityResultLauncher` de `MainActivity` (que debe seguir registrándose
  **antes** de `onCreate`); su resultado sigue siendo no-op: el estado se relee en el siguiente
  `state()`.

### 4. `CookingModeScreen.kt` — rationale + disparo (mínimo cambio)

Se añade un parámetro al final (`notificationPermission: NotificationPermissionPrompt =
NoOpNotificationPermissionPrompt`) y se **extiende el efecto existente** (no se añade otro), sin tocar
el reloj (el `LaunchedEffect` que alimenta `tick(1000)` sigue igual), la UI del temporizador ni el
microcopy "¡Listo!":

```kotlin
var showRationale by rememberSaveable { mutableStateOf(false) }

LaunchedEffect(state.timer.status) {
    val now = SystemClock.elapsedRealtime()
    alarm.apply(CookingAlarmPolicy.onTimerChange(previousTimer, state.timer, now))
    val decision = NotificationPermissionPolicy.onTimerChange(
        previous = previousTimer,
        next = state.timer,
        state = notificationPermission.state(),
    )
    if (decision == NotificationPermissionCommand.AskWithRationale) {
        notificationPermission.markAsked(now) // se registra al MOSTRAR el rationale
        showRationale = true
    }
    previousTimer = state.timer
}

if (showRationale) {
    NotificationPermissionRationale(
        onConfirm = { showRationale = false; notificationPermission.request() },
        onDismiss = { showRationale = false },
    )
}
```

- `NotificationPermissionRationale` es un `@Composable` privado con el `AlertDialog` de Material 3:
  título/cuerpo de `NotificationPermissionCopy`, acción de confirmación ("Continuar") en `primary`,
  descarte ("Ahora no") y `onDismissRequest` equivalente al descarte. **No** introduce tokens, colores
  ni formas nuevas (roles de `SazonTheme` y formas por defecto de Material 3).
- El efecto es **el mismo** que ya aplica el aviso: si el rationale aparece, la alarma del aviso **ya**
  se ha programado antes, de modo que la petición nunca retrasa ni gatea el temporizador.
- `markAsked(now)` se hace al mostrar el rationale (no al responder): aunque el usuario no conteste, la
  sesión no vuelve a molestar.

### 5. `MainActivity.kt` — retirar la petición del arranque

- **Eliminar** la llamada `requestNotificationsPermissionOncePerProcess()` (línea 229), el método
  (262-276) y el companion con `@Volatile notificationsPermissionRequested` (278-283).
- **Mantener** `registerForActivityResult(ActivityResultContracts.RequestPermission())` (ahora la usa
  el adaptador) y `CookingNotifications.ensureChannel(applicationContext)` (el canal se sigue creando
  al arrancar para que aparezca en los ajustes del sistema; crearlo no pide permiso ni muestra nada).
- Crear la costura junto a las demás (`AndroidCookingAlarm(applicationContext)`) y pasarla a
  `SazonApp`; `SazonApp` y `MainShell` la reenvían a `CookingModeScreen` como ya hacen con
  `cookingAlarm`.

## Expected File Changes

**Crear**

- `app/src/main/java/com/sazon/app/ui/cooking/NotificationPermission.kt` — estado, comando, microcopy y
  política pura (**sin** imports de Android/Compose).
- `app/src/main/java/com/sazon/app/ui/cooking/NotificationPermissionPrompt.kt` — costura +
  `NoOpNotificationPermissionPrompt` (pura).
- `app/src/main/java/com/sazon/app/data/cooking/AndroidNotificationPermissionPrompt.kt` — adaptador
  (consulta del permiso + lanzador + memoria de sesión), "no lanza".
- `app/src/test/java/com/sazon/app/ui/cooking/NotificationPermissionPolicyTest.kt` — política pura
  (JVM), **la verificación exigida**.
- `app/src/test/java/com/sazon/app/data/cooking/AndroidNotificationPermissionPromptTest.kt` —
  adaptador (Robolectric: permiso concedido/denegado, memoria de sesión, `request()` delega y no
  lanza).

**Modificar**

- `app/src/main/java/com/sazon/app/MainActivity.kt` — retirar la petición del arranque y pasar la
  costura nueva.
- `app/src/main/java/com/sazon/app/ui/SazonApp.kt` — parámetro `notificationPermission` (default
  no-op) y reenvío.
- `app/src/main/java/com/sazon/app/ui/MainShell.kt` — parámetro `notificationPermission` y reenvío a
  `CookingModeScreen`.
- `app/src/main/java/com/sazon/app/ui/cooking/CookingModeScreen.kt` — parámetro + extensión del efecto
  + `NotificationPermissionRationale` (reloj, temporizador, UI y microcopy "¡Listo!" intactos).
- `app/src/main/AndroidManifest.xml` — **solo** el comentario de `POST_NOTIFICATIONS` (petición en
  contexto). Permisos y receptor sin cambios.
- `docs/specs/cooking-timer-alarm.md`, `docs/adr/0007-cooking-timer-alarm.md`,
  `docs/technical-discovery.md`, `docs/risks-and-open-questions.md`, `PROGRESS.md` (sesión 131) y
  `DESIGN.md` — reconciliación de texto y registro de la excepción de capturas, según
  `Reconciliation Of The Incoherent Text` y `Durable Documentation Impact`.
- `feature_list.json` y `PROGRESS.md` — **al implementar** (estado `passing` + evidencia), no en esta
  sesión.

**No se toca** `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`, `scripts/**`,
`domain/**`, `data/**` (salvo el fichero nuevo de `data/cooking/`), `AndroidCookingAlarm.kt`,
`CookingAlarmReceiver.kt`, `CookingNotifications.kt`, `CookingAlarm.kt`, `CookingTimerState.kt`,
`CookingModeState.kt`, `CookingModeFormat.kt`, `ui/theme/**`, los `mipmap` de lanzador, los `assets/**`
ni `docs/design/concepts/**`. La evidencia de `feature_list.json` de `cooking-timer-alarm` **no** se
reescribe.

## Visual Design Impact

- UI involved: **sí, parcialmente** — no hay pantalla nueva; aparece **un diálogo de rationale** dentro
  del Modo cocina ya aceptado, más el diálogo del sistema (que no es de la app).
- Design source: `DESIGN.md` es la fuente de verdad. **No** cubre diálogos (no hay sección ni tokens de
  diálogo): el implementer **no** inventa estilo, usa el `AlertDialog` de Material 3 con los roles de
  `SazonTheme` (superficie, texto y **una sola** acción principal en `primary`, "Continuar"; "Ahora no"
  como descarte sin énfasis) y respeta las reglas vigentes ("una sola acción principal por pantalla",
  sin gradientes ni sombras marcadas). La microcopy es la constante pura `NotificationPermissionCopy`.
- Screens or states affected: `CookingModeScreen` (un estado transitorio nuevo: rationale visible la
  primera vez que arranca el temporizador sin permiso). El resto de estados del Modo cocina no cambia.
- New design artifact required: **no** — no se crea `docs/design/concepts/cooking-timer-alarm-permission-context/`
  ni captura de Stitch (un rationale de permiso del sistema no es una pantalla diseñada). Se registra
  la excepción en `DESIGN.md` (misma clase que las de #77/#85/#86/#91 y `cooking-timer-alarm`) y el
  acabado (claro y oscuro) lo revisa la persona en el emulador tras el merge.
- Tarea del implementer: no inventar tokens; el diálogo no añade color, tipografía ni forma propios.

## Durable Documentation Impact

- `ARCHITECTURE.md`: **not needed** — el repositorio lo omite por decisión del harness.
- `CONSTRAINTS.md`: **not needed** — no nace una regla MUST/MUST NOT de nivel repo.
- `AGENTS.md`: **not needed** — flujo, arranque y reglas no cambian.
- `docs/technical-discovery.md`: **update** — el párrafo del aviso (líneas 417-419) describe la
  petición del permiso en contexto (política pura + rationale + adaptador en `data.cooking`), no al
  arrancar.
- `docs/domain-model.md`: **not needed** — no hay entidad, relación ni estado de dominio nuevos (el
  permiso es estado de UI/plataforma, no de dominio).
- `docs/build-brief.md`: **not needed** — la enmienda acotada del Non-Goal "Notificaciones y
  recordatorios" ya la hizo `cooking-timer-alarm`; el **momento** en que se pide el permiso es una
  decisión de UX, no de alcance (el flujo de producto no cambia).
- `docs/adr/0007-cooking-timer-alarm.md`: **update** — enmienda a la decisión 11 y alternativa 9
  superada (sin reescribir el resto).
- `docs/specs/cooking-timer-alarm.md`: **update** — nota de vigencia + las cuatro frases de estado
  obsoletas (ver `Reconciliation Of The Incoherent Text`); escenarios y casillas quedan como registro.
- `docs/risks-and-open-questions.md`: **update** — las dos menciones al arranque pasan a la petición en
  contexto; la fila del riesgo de permiso denegado añade que la petición **tampoco** gatea el
  temporizador.
- `DESIGN.md`: **update** — nota de excepción de capturas + dónde vive el rationale (diálogo Material 3
  con roles del tema y microcopy de `NotificationPermissionCopy`).
- `PROGRESS.md`: **update** — paréntesis aclaratorio en la sesión 131 (el "contextual" era el parcial
  descartado) y entrada de la sesión de esta feature.
- `CONTEXT.md`: **not needed** — no introduce vocabulario de dominio nuevo.

## Implementation Plan

1. Crear `NotificationPermission.kt` (estado, comando, microcopy, política) y
   `NotificationPermissionPrompt.kt` (costura + no-op) con los tests JVM de la política (vectores
   dorados de la tabla de verdad).
2. Crear `AndroidNotificationPermissionPrompt.kt` (permiso + lanzador + memoria de sesión) y su test
   Robolectric; retirar de `MainActivity` la petición del arranque, el método y el flag, manteniendo el
   lanzador y `ensureChannel`.
3. Extender `CookingModeScreen` (parámetro + efecto + rationale con `AlertDialog`) y cablear
   `SazonApp`/`MainShell` por parámetro con default no-op.
4. Reconciliar los textos (`docs/specs/cooking-timer-alarm.md`, ADR 0007, `technical-discovery`,
   `risks`, comentario del manifiesto, `PROGRESS.md` sesión 131) y registrar la excepción en
   `DESIGN.md`.
5. `bash init.sh` en verde (test + lint) y evidencia registrada.

## Implementation Tasks

- [x] Crear `NotificationPermissionState` (+`asked(atMillis)`), `NotificationPermissionCommand` y
      `NotificationPermissionCopy` en `ui/cooking/NotificationPermission.kt`, **sin** imports de
      Android/Compose.
- [x] Crear `NotificationPermissionPolicy.onTimerChange` (arista `!previous.isRunning &&
      next.isRunning`; `None` si concedido o ya pedido; `AskWithRationale` en el resto).
- [x] Crear `NotificationPermissionPrompt` + `NoOpNotificationPermissionPrompt` en
      `ui/cooking/NotificationPermissionPrompt.kt` (el no-op declara `granted = true`: nada se pide).
- [x] Escribir `NotificationPermissionPolicyTest` (JUnit 4, nombres en español): no se pide en la
      primera composición ni al abrir el modo; `IDLE→RUNNING` sin permiso y sin intento →
      `AskWithRationale`; `PAUSED→RUNNING` tras un intento → `None`; concedido → `None`; ticks,
      Pausar/Reiniciar/cambiar de paso/llegar a cero → `None`; paso sin duración → `None`;
      determinismo y "el instante entra por parámetro" (`asked(1L)`/`asked(9L)` dan estados distintos y
      `askedAtMillis` nunca se lee del reloj); 599 ticks no piden nada.
- [x] Crear `AndroidNotificationPermissionPrompt` (`state()` con `ContextCompat.checkSelfPermission`,
      `request()` que delega el lanzador en `runCatching`, `markAsked` que fija la memoria de sesión).
- [x] Escribir `AndroidNotificationPermissionPromptTest` (Robolectric `@Config(sdk = [33])`, con
      `shadowOf(application).grantPermissions/denyPermissions` y un contador de invocaciones):
      denegado + nunca pedido → `state().granted == false` y `askedAtMillis == null`; `markAsked(1234)`
      → `askedAtMillis == 1234`; concedido → `granted == true`; `request()` invoca el lanzador una vez
      con `POST_NOTIFICATIONS` y **no lanza** si el lanzador lanza.
- [x] Retirar de `MainActivity` la llamada `requestNotificationsPermissionOncePerProcess()`, el método
      y el flag de compañía; conservar el `registerForActivityResult` y `ensureChannel`; construir
      `AndroidNotificationPermissionPrompt(applicationContext, requestNotificationsPermission::launch)`
      y pasarlo a `SazonApp`.
- [x] Añadir el parámetro `notificationPermission` (default `NoOpNotificationPermissionPrompt`) a
      `SazonApp` y `MainShell` y reenviarlo hasta `CookingModeScreen`.
- [x] Extender `CookingModeScreen`: parámetro nuevo, decision en el efecto existente (con
      `SystemClock.elapsedRealtime()` como instante inyectado), `markAsked` al mostrar y
      `NotificationPermissionRationale` (Material 3 `AlertDialog`, microcopy de la constante pura).
- [x] Reconciliar los textos (spec aceptada, ADR 0007 decisiones 11/alternativa 9, `technical-discovery`,
      `risks`, comentario del manifiesto, `PROGRESS.md` sesión 131) y añadir la nota en `DESIGN.md`.
- [x] `bash init.sh` en verde y evidencia registrada en `feature_list.json` (`passing`) y `PROGRESS.md`
      (sin commit/push/PR: eso es del orquestador).

## Verification Plan

- `bash init.sh` → **esperado**: código 0 (`./gradlew test` + `./gradlew lint` vía `scripts/gradle.sh`,
  sin procesos largos) y **`init.sh` sin cambios**.
- `bash scripts/gradle.sh testDebugUnitTest --tests "com.sazon.app.ui.cooking.*" --tests "com.sazon.app.data.cooking.*"`
  → **esperado**: BUILD SUCCESSFUL con las dos suites nuevas en verde
  (`NotificationPermissionPolicyTest` — **verificación exigida** — y
  `AndroidNotificationPermissionPromptTest`) y sin suites modificadas: `CookingAlarmPolicyTest`,
  `CookingTimerStateTest`, `CookingModeStateTest`, `CookingModeFormatTest`, `CookingNotificationsTest`
  y `AndroidCookingAlarmTest` siguen exactamente igual.
- `./gradlew test` (vía `init.sh`) → **esperado**: la baseline de `HEAD b80421a` es **107 suites / 809
  tests** (`git grep -c '@Test' HEAD -- app/src/test`); el delta debe ser **solo** las dos suites y sus
  tests nuevos, con 0 fallos/errores/saltados.
- `./gradlew lint` → **esperado**: 0 errores y **0 warnings nuevos bajo `app/src`** (los 13
  preexistentes se mantienen). Si aparece un warning nuevo (p. ej. por el `AlertDialog` o el lanzador),
  resolverlo con el remedio de estilo del repo y **registrar el hallazgo**.
- Inspección (greps propios del implementer y del validator):
  - `ui/cooking/NotificationPermission.kt` y `NotificationPermissionPrompt.kt` **no** importan
    `android.*`/`androidx.*` (0 imports de framework) y no leen el reloj.
  - `AndroidNotificationPermissionPrompt.kt` es el **único** fichero Android nuevo;
    `AlarmManager`/`PendingIntent`/`NotificationManager` siguen **solo** en `data/cooking/` (más
    manifiesto y `MainActivity`); `AlertDialog` aparece **solo** en `CookingModeScreen.kt`.
  - `MainActivity.kt`: **0** apariciones de `requestNotificationsPermissionOncePerProcess` y de
    `notificationsPermissionRequested`; la petición del arranque ya no existe.
  - `CookingAlarm.kt`, `CookingTimerState.kt`, `CookingModeState.kt`, `CookingModeFormat.kt`,
    `CookingNotifications.kt`, `AndroidCookingAlarm.kt`, `CookingAlarmReceiver.kt` y
    `AndroidManifest.xml` (permisos + receptor) **sin diff funcional**; `app/build.gradle.kts` y
    `gradle/libs.versions.toml` intactos (sin dependencias nuevas).
  - Reconciliación: `grep -rn "una vez por proceso" docs/ app/src/main` no deja ninguna afirmación
    **vigente** sobre el arranque (solo, si acaso, citas históricas de la spec aceptada identificadas
    como tales y el paréntesis aclaratorio de `PROGRESS.md`).
  - No se crea `docs/design/concepts/cooking-timer-alarm-permission-context/`.
- Manual (emulador, lo revisa la persona; `AGENTS.md` no automatiza UI ni el sistema de permisos):
  1. Instalación limpia (o permiso revocado): **abrir la app NO muestra el diálogo** ni el rationale.
  2. Abrir una receta → "Empezar a cocinar" → paso con duración → "Iniciar": aparece **primero** el
     rationale ("Avisos del temporizador") y, al pulsar "Continuar", el **diálogo del sistema**.
  3. Conceder: el temporizador sigue, la cuenta atrás baja y al terminar aparece el "¡Listo!" en
     pantalla y el aviso de sistema de `cooking-timer-alarm` (sin regresión).
  4. Denegar (o "Ahora no"): el Modo cocina, la cuenta atrás y el "¡Listo!" siguen intactos; al
     pausar/reanudar/cambiar de paso **no** se vuelve a pedir en la misma sesión.
  5. Cerrar y reabrir la app: **no** hay diálogo al arrancar; la petición solo reaparece al arrancar
     por primera vez el temporizador en la nueva sesión.
  6. Con el permiso ya concedido, arrancar el temporizador **no** muestra rationale.

Cobertura E2E: **no aplica**. No hay suite E2E persistente y `AGENTS.md` prohíbe automatizar UI de
emulador; la decisión (lógica pura) se cubre en JVM, el adaptador en Robolectric y el
comportamiento del sistema (rationale → diálogo, denegación, no re-pedir) lo revisa la persona.

## Evidence To Capture

- Salida del gate `bash init.sh` (código 0) y recuento propio de los XML: suites/tests totales, 0
  fallos/errores/saltados y delta respecto a la baseline en `HEAD b80421a` (**107 suites / 809 tests**).
- Salida de la tirada focal con `NotificationPermissionPolicyTest` y
  `AndroidNotificationPermissionPromptTest` (nombre de cada test y nº de tests), y confirmación de que
  las seis suites de `cooking` preexistentes **no** cambian.
- Informe de lint: 0 errores y 13 warnings preexistentes, **0 bajo `app/src`** (o el remedio aplicado).
- Inspección registrada: pureza de los dos ficheros nuevos de `ui/cooking`, confinamiento de Android a
  `AndroidNotificationPermissionPrompt`, 0 apariciones de la petición en el arranque, build/lockfile/
  `init.sh`/dominio/avisos aceptados intactos, sin dependencias nuevas, sin carpeta de capturas.
- Reconciliación registrada: ficheros y frases exactas actualizadas (spec aceptada, ADR 0007,
  `technical-discovery`, `risks`, comentario del manifiesto, `PROGRESS.md` sesión 131) y nota de
  `DESIGN.md`.
- Resultado manual del emulador (puntos 1-6), con la limitación de que no es automatizable.
- Anotación en `feature_list.json`/`PROGRESS.md` de la evidencia y del estado `passing` (al
  implementar; **no** en esta sesión).

## Decisions And Unknowns

### Decisiones fijadas (justificadas contra el repositorio)

- **Se retira la petición del arranque**, no se "aplaza": `MainActivity` deja de tener cualquier
  llamada de permiso en `onCreate` (Scenario 1). El canal se sigue creando al arrancar porque crearlo
  no pide permiso ni muestra nada y mantiene el canal en los ajustes del sistema.
- **La petición se dispara en la arista de "arrancar el temporizador"** (`!previous.isRunning &&
  next.isRunning`), la misma que ya dispara el aviso: es el momento en que el permiso **sirve** para
  algo y no se mezcla con abrir pantallas ni con pausar/reanudar.
- **Decisión pura con estado inyectado** (`NotificationPermissionState`): el permiso y el instante
  entran por parámetro, la política no lee el reloj ni toca Android, y los cuatro casos exigidos por la
  entrada quedan en tests JVM (misma clase de verificación que la del aviso aceptado).
- **Costura por parámetro con default no-op**, como `cookingAlarm`/`dishImageSettings` (sin
  `CompositionLocal`): `CookingModeScreen` sigue sin conocer Android y las previews/tests compilan sin
  cableado.
- **La memoria de sesión vive en el adaptador**, no en el composable: hoy el flag era de proceso
  (`MainActivity`), y `CookingModeScreen` se desmonta al salir del modo, así que un `remember` haría
  que se volviera a pedir al reentrar en la misma sesión (violaría el Scenario 4).
- **`markAsked` al mostrar el rationale**, no al responder: si el usuario deja el diálogo sin contestar
  (o muere la composición), la sesión no vuelve a molestar y el comportamiento es determinista.
- **El rationale no gatea el temporizador**: el aviso de fin ya se ha programado antes de la decisión
  del permiso, y el reloj de la UI es un efecto independiente; el diálogo del sistema se lanza después.
- **`AlertDialog` de Material 3 sin tokens nuevos**: `DESIGN.md` no cubre diálogos, así que se usan los
  roles de `SazonTheme` y las reglas vigentes (una acción principal) en vez de inventar un estilo; la
  microcopy se fija en una constante pura testeable.
- **Reconciliación de texto mínima**: se corrigen las frases **de estado** (spec aceptada, ADR 0007,
  `technical-discovery`, `risks`, comentario del manifiesto) más un paréntesis aclaratorio en el log de
  la sesión 131; **no** se reescribe la evidencia de `feature_list.json` (era correcta cuando se
  validó) ni los escenarios/casillas de la spec aceptada.
- **Sin dependencias nuevas**: `AlertDialog` (Material 3), `ContextCompat`/`ActivityResultContracts`
  (androidx ya presentes) y Robolectric/`androidx.test.core` para los tests.

### Unknowns

- **Denegación permanente en el sistema**: si el usuario deniega dos veces, Android deja de mostrar el
  diálogo; nuestro rationale se seguiría mostrando **una vez por sesión** (cada proceso) y tras él el
  lanzador no mostraría nada. Es una limitación conocida y aceptada en esta correctiva (la entrada solo
  pide "no volver a pedir en la misma sesión"); enderezarlo exige inyectar
  `shouldShowRequestPermissionRationale` (o persistir el "ya pedido"), que sería otra feature.
- **Rechazo del rationale y reentrada en el modo** dentro de la misma sesión: cubierto por la memoria
  de sesión del adaptador (Scenario 4); tras **reiniciar el proceso** la app vuelve a mostrar el
  rationale en el primer arranque del temporizador, aunque el permiso esté denegado. Aceptado por
  diseño (una sesión = un proceso, igual que el comportamiento anterior).
- **Aspecto exacto del rationale** (tipografía del título/cuerpo, jerarquía de las dos acciones,
  comportamiento en oscuro y con tipografía dinámica): lo revisa la persona en el emulador tras el
  merge; un defecto se corrige con una **feature correctiva**.
- **Sombra del `AlertDialog`/`ModalBottomSheet` con el temporizador corriendo** (un diálogo encima de un
  overlay animado): no hay test automatizable; se comprueba en la prueba manual (punto 2 y 6).
- **Riesgo de lint del `launch` del lanzador** dentro del adaptador: si lint se queja de una petición
  sin comprobación, el remedio es mantener la comprobación de permiso en el mismo fichero (patrón ya
  usado por `CookingNotifications`) y registrar el hallazgo en la evidencia.

## Validator Checklist

- [ ] La petición del permiso **no** existe en el arranque: `MainActivity.onCreate` no pide nada y no
      queda el flag de compañía ni el método (Scenario 1, **verificación exigida**).
- [ ] Al primer arranque del temporizador sin permiso se muestra **rationale +** diálogo del sistema, y
      el temporizador corre igual (Scenarios 2 y 6, **verificación exigida**).
- [ ] `NotificationPermissionPolicy` es **pura** (0 imports `android.*`/`androidx.*`), **determinista**
      y **sin reloj**: `AskWithRationale` solo en la arista de arrancar sin permiso y sin intento
      previo; `None` con permiso concedido, con intento previo, en ticks/no-ops y fuera de la arista
      (Scenarios 3, 7 y 8, **verificación exigida**).
- [ ] Denegado o descartado el rationale no se vuelve a pedir en la sesión y el Modo cocina, la cuenta
      atrás y el "¡Listo!" siguen intactos (Scenarios 4 y 5, **verificación exigida**).
- [ ] La decisión pura está cubierta en JVM con casos negativos (no solo el camino feliz) y el
      adaptador Android en Robolectric (con permiso concedido/denegado y "no lanza").
- [ ] `CookingAlarmPolicy`, `CookingAlarm`, `CookingTimerState`, `CookingModeState`, `CookingModeFormat`,
      `data/cooking/**` (salvo el fichero nuevo), el manifiesto (permisos + receptor), `domain/**`,
      `init.sh`, `app/build.gradle.kts` y `gradle/libs.versions.toml` quedan **sin cambios**; sin
      dependencias nuevas.
- [ ] El texto vigente del repo ya **no** afirma que el permiso se pide al arrancar (spec aceptada, ADR
      0007 decisiones 11/alternativa 9, `technical-discovery`, `risks`, comentario del manifiesto,
      `PROGRESS.md`) y **no** se ha reescrito la evidencia de `feature_list.json` de
      `cooking-timer-alarm` (Scenario 9, **verificación exigida**).
- [ ] `DESIGN.md` registra la excepción de capturas y **no** se crea
      `docs/design/concepts/cooking-timer-alarm-permission-context/`; el rationale no inventa tokens
      (roles del tema, una sola acción principal, microcopy de `NotificationPermissionCopy`).
- [ ] Gate en verde (`bash init.sh` código 0), con evidencia registrada (suites/tests/delta, lint con 0
      warnings nuevos bajo `app/src`) en `feature_list.json`/`PROGRESS.md`, y **sin** commit/push/PR del
      implementer.
