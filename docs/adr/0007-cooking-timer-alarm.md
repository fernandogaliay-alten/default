# ADR 0007: El fin del temporizador del Modo cocina avisa con una alarma exacta y una notificación propia

## Estado

**Aceptado** (2026-10-15). Implementa la feature `cooking-timer-alarm` (`area: integration`, depende de
`cooking-mode`, `accepted`), pedida por la persona tras comprobar que el Modo cocina **solo** avisa en
pantalla. Enmienda de forma **acotada** el Non-Goal "Notificaciones y recordatorios" de
`docs/build-brief.md` (misma clase de enmienda que la de `cooking-mode`, issue #76): entra **solo** el
aviso de fin del temporizador, no los recordatorios programados por el usuario ni las notificaciones de
sugerencias/recetas.

## Contexto

El modo cocina (`docs/specs/cooking-mode.md`) tiene un temporizador por paso **puro y determinista**
(`CookingTimerState`) y un aviso **en pantalla** "¡Listo!" (`CookingModeFormat.FINISHED_NOTICE`), pero el
reloj real vive en un `LaunchedEffect` de `CookingModeScreen`: **solo late con la composición activa** y
solo se ve si la app está en primer plano. Si el usuario deja el móvil bloqueado o sale de la app para
hacer otra cosa, se pierde el aviso (que es justo el caso de uso: no quedarse mirando la pantalla).

Restricciones del repositorio que condicionan la decisión:

- La verificación canónica corre en **JVM** (`bash init.sh` → `./gradlew test` + `lint`); AGENTS.md
  **no** automatiza UI de emulador ni el sistema de notificaciones. Así que la única verificación
  automatizable real es la **decisión pura** + lo que toca framework con Robolectric.
- `minSdk = 33`: el permiso `POST_NOTIFICATIONS` es **obligatorio en runtime** (no hay vía "gratis") y su
  ausencia **no** puede romper el flujo.
- Patrón aceptado de costura Android: `interface` + default `NoOp…` (`ui.artwork`,
  `data.image.DishImageSettingsStore`), lógica **pura** en un paquete sin `android.*` y adaptador
  delgado en `data/`. Degradación **silenciosa** (ADR 0005): un fallo nunca rompe la UI.
- Non-Goals vigentes de `cooking-mode`: nada de persistencia del modo, ni `foreground service`, ni
  `WakeLock`, ni dependencias nuevas.

## Decisión

1. **La decisión del aviso es pura y determinista** (`ui.cooking.CookingAlarmPolicy.onTimerChange`),
   con los **instantes inyectados** (`nowMillis`) y **sin leer el reloj**: mismo criterio que
   `CookingTimerState`. Devuelve un `CookingAlarmCommand` (`Schedule(triggerAtMillis)` / `Cancel` /
   `None`). `onExit()` cubre la salida del modo. Escenarios 1, 4, 5, 7 y 8 de la spec.
2. **Costura `CookingAlarm.apply(command)` con default `NoOpCookingAlarm`**, pasada **por parámetro** de
   `MainActivity` → `SazonApp` → `MainShell` → `CookingModeScreen` (como `dishImageSettings`, sin
   `CompositionLocal`): previews, tests y composiciones sin cableado compilan y no hacen nada, y la
   pantalla sigue sin conocer Android.
3. **Alarma exacta de un solo disparo con `AlarmManager`** en el adaptador
   `data.cooking.AndroidCookingAlarm`: `setExactAndAllowWhileIdle(ELAPSED_REALTIME_WAKEUP, at, pi)`; si
   `canScheduleExactAlarms()` fuera `false` **degrada** a `setAndAllowWhileIdle` (inexacta: puede llegar
   tarde, pero **nunca** hay crash). El `requestCode` fijo ⇒ una sola alarma viva; cada programación
   **reemplaza** la anterior.

   > **Enmendada (`cooking-timer-notification-reliability`, 2026-10-16):** la alarma deja de ser el
   > **productor primario** del "¡Listo!" y pasa a ser el **respaldo** del servicio en primer plano, que
   > conoce el instante de fin y publica al vencer su `delay`. La alarma sigue siendo imprescindible para
   > el caso de **proceso muerto o congelado** (la ejecuta el sistema) y la idempotencia entre los dos
   > publicadores la da el **sello de fin**, no el publicador único. El resto de esta decisión (una sola
   > alarma viva, degradación sin crash) no cambia.
4. **`USE_EXACT_ALARM`** (y no `SCHEDULE_EXACT_ALARM`): con `minSdk 33` es un permiso **normal**,
   concedido en la instalación, para apps cuyo caso de uso documentado es un **temporizador** (la app no
   se publica, así que la política de Play no aplica) y evita mandar al usuario a Ajustes. Se acompaña de
   `POST_NOTIFICATIONS` (runtime) y `VIBRATE` (normal).
5. **Receptor de broadcast declarado en el manifiesto** (`data.cooking.CookingAlarmReceiver`,
   `exported="false"`): es lo único que dispara con la app en segundo plano, el proceso muerto o la
   pantalla bloqueada/Doze, sin depender de la composición ni de un `foreground service` (Non-Goal).

   > **Matizada (`cooking-timer-notification-reliability`, 2026-10-16):** el servicio en primer plano ya
   > existe (ADR 0008) y publica el aviso en el instante exacto; el receptor queda como **respaldo** para
   > la muerte/congelación del proceso y, además de publicar, **para el servicio y retira la `1000`** en
   > ese instante (la retirada también es exacta cuando el FGS está congelado o muerto).
6. **Canal propio `cooking_timer`** de importancia **alta**, con sonido y vibración y visibilidad
   pública, creado **idempotentemente** desde `MainActivity` (y de nuevo antes de publicar, defensivo).
7. **Contenido genérico y sin datos sensibles**: `CookingNotifications.notification(context,
   contentIntent, whenMillis)` **no recibe la receta** (título fijo `"¡Listo! El paso ha terminado"`, texto fijo
   `"Toca para volver al modo cocina."`), y el `PendingIntent` de la alarma **no lleva extras**. La
   ausencia de datos de receta es **estructural**, no una convención de la capa Android.

   > **Enmendada (`cooking-timer-notification-reliability`, 2026-10-16):** el `PendingIntent` de la
   > alarma lleva **un escalar**: el **sello del fin** en hora de pared
   > (`CookingNotifications.EXTRA_FINISH_WALL_CLOCK_MILLIS`), que viaja también en el `Intent` del
   > servicio y se publica como `when` de la `1001` para reconocer un fin ya anunciado. Sigue **sin**
   > ningún dato de la receta (ni título, ni paso, ni ingredientes) y la garantía estructural se mantiene
   > (los tests de firma aceptan solo `Context`, `PendingIntent` y ese escalar).
   >
   > **Acotada (`cooking-timer-notification-step-label`, 2026-10-19):** por **decisión explícita de la
   > persona** (mejora pedida el 2026-10-16: "saber en qué paso voy sin abrir la app"), la notificación
   > **persistente** (`ongoing`) acepta además **dos cadenas**: el número del paso —`"Paso N de M"`, la
   > microcopy ya aceptada del encabezado del Modo cocina, sin copy paralelo— y el **título del paso**.
   > Son lo **único** derivado de la receta que puede cruzar al sistema y se **derivan** del paso actual +
   > la receta en `ui.cooking.CookingStepLabel` al armar el trabajo del sistema (no se persisten). El
   > aviso `1001` (`notification`/`notifyFinish`) sigue **sin** aceptar texto y con su copy genérico
   > **intacto**, y el `PendingIntent` de la alarma sigue llevando **solo** el escalar del sello (la
   > etiqueta viaja únicamente al `Intent` del servicio): el camino del respaldo con el proceso muerto no
   > expone nada nuevo. Ni descripción del paso, ni ingredientes, ni notas, ni tips, ni nutrición ni clave
   > de IA cruzan. El canal es `VISIBILITY_PUBLIC`, así que ese texto se lee en la pantalla de bloqueo:
   > el trade-off queda registrado en `docs/risks-and-open-questions.md`.
8. **Id de notificación fijo (`1001`)**: publicarla otra vez **reemplaza** la anterior ⇒ nunca se duplica,
   tampoco con la app en primer plano (donde el `tick` de la app y el receptor pueden coincidir).

   > **Ampliada (`cooking-timer-notification-reliability`, 2026-10-16):** con **dos** publicadores
   > posibles (servicio y alarma) el id fijo evita dos **notificaciones**, pero **no** dos
   > sonidos/vibraciones; la idempotencia la cierra la decisión pura `CookingFinishPublication` sobre el
   > **sello de fin** (mismo sello ya mostrado ⇒ no se republica; sello más viejo que la ventana de
   > frescura de 60 s ⇒ es el aviso de un paso anterior y se descarta). `setOnlyAlertOnce` **no** se usa
   > (silenciaría el fin legítimo siguiente mientras la `1001` anterior siga en la bandeja).
9. **`Cancel` cancela solo la alarma pendiente, no la notificación ya mostrada.** Si la cancelación
   retirara también la notificación, el `Cancel` de `RUNNING → FINISHED` (cuando el `tick` de la app
   llega a cero en primer plano) borraría el aviso recién disparado por el receptor.

   > **Revisada (`cooking-timer-finish-notification`, 2026-10-16):** se comprobó en el emulador que el
   > aviso de fin **no siempre llegaba** con la app en primer plano, por una carrera entre el `tick` de
   > la composición y el disparo de `AlarmManager`: el `Cancel` de `RUNNING → FINISHED` cancelaba la
   > alarma pendiente (y paraba el servicio) antes de que pudiera publicar. La arista `RUNNING → FINISHED`
   > **ya no emite `Cancel`** (devuelve `None`) y el servicio se para solo con su `delay`. `Cancel` sigue
   > con la semántica de esta decisión —**no** retira notificaciones ya mostradas— y la limpieza de la
   > `1001` en la salida explícita es un comando **aparte** de la costura
   > (`CookingAlarm.dismissFinishNotification`), que **no** se emite en el `onDispose`.
   >
   > **Matizada (`cooking-timer-notification-reliability`, 2026-10-16):** con el servicio como productor
   > **primario** del aviso, "la alarma es el **único** productor" ya no aplica: la alarma se deja viva
   > como **respaldo** y la garantía "una sola notificación y una sola alerta" pasa a sostenerse en el
   > **sello de fin**. La semántica de `Cancel` **no** cambia.
10. **Sin persistencia**: el instante de fin vive en la alarma del sistema; el estado del Modo cocina
    sigue sin persistirse (Non-Goal). "Reanudar donde lo dejé" es otra feature.
11. **El permiso se pide una vez por proceso al arrancar** (`MainActivity.onCreate`, con
    `registerForActivityResult`, bandera estática de compañía) de forma **no bloqueante**: no gatea
    ninguna pantalla y, denegado, el Modo cocina, la cuenta atrás y el "¡Listo!" en pantalla siguen
    intactos (degradación silenciosa).

    > **Enmienda (`cooking-timer-alarm-permission-context`, 2026-10-15):** esta decisión queda
    > **superada**. Probar en el emulador el diálogo del sistema **al abrir la app y sin ninguna
    > explicación** resultó un defecto de UX: el permiso pasa a pedirse **en contexto**, la **primera
    > vez** que el usuario arranca el temporizador de un paso y **después** de un rationale breve, con
    > la decisión **pura** `NotificationPermissionPolicy` (permiso e instante inyectados) y el
    > adaptador `data.cooking.AndroidNotificationPermissionPrompt` (que asume la memoria de sesión: una
    > vez por proceso). Denegado o descartado, el Modo cocina, la cuenta atrás y el "¡Listo!" siguen
    > intactos y la petición **no** gatea el temporizador. El resto del ADR no cambia.
12. **Sin dependencias nuevas**: `NotificationCompat` y `NotificationManagerCompat` (`androidx.core`),
    `AlarmManager`, `NotificationChannel` y `ActivityResultContracts` (androidx.activity) ya están.

Alcance: `ui/cooking/CookingAlarm.kt` (comando + política + costura), `data/cooking/` (adaptador, receptor
y notificaciones), `res/drawable/ic_notification_timer.xml` (small icon monocromo), el manifiesto, el
cableado (`MainActivity`/`SazonApp`/`MainShell`/`CookingModeScreen`) y los tests JVM/Robolectric. **No**
se toca la lógica del temporizador aceptada (`CookingTimerState`/`CookingModeState`/`CookingModeFormat`),
ni el "¡Listo!" en pantalla, ni `ui/theme/**`, ni `domain/**`, ni el resto de `data/**`, ni
`app/build.gradle.kts`, ni `gradle/libs.versions.toml`, ni `init.sh`, ni `scripts/**`.

## Alternativas consideradas

1. **`WorkManager` / `androidx.work`.** No garantiza la hora exacta (es lo que la propia documentación
   advierte para *exact timing*) y añadiría una dependencia nueva para un único aviso de fin. Descartado.
2. **Alarma inexacta de entrada (`setAndAllowWhileIdle` siempre).** Sencilla y sin permisos especiales,
   pero el aviso podría retrasarse minutos, que es justo lo que el usuario nota. Se conserva **solo** como
   degradación si el sistema no concede la alarma exacta. Descartado como camino principal.
3. **`foreground service` permanente mientras corre el temporizador.** Da más control y sobrevive al
   proceso, pero exige notificación permanente + permiso de tipo de servicio y contradice el Non-Goal
   ("solo el aviso de fin"). Descartado.
   > **Revisada (`cooking-timer-foreground-service`, 2026-10-15):** la persona pidió ver la **cuenta
   > atrás** fuera de la app tras probar esta feature, así que la alternativa se **adopta** con un
   > alcance distinto al descartado: el servicio es un **pintor** de la cuenta atrás (notificación
   > persistente `1000` con cronómetro descendente del sistema) y **no** sustituye a la alarma, que
   > sigue siendo la única dueña del "¡Listo!". Ver `docs/adr/0008-cooking-timer-foreground-service.md`.
4. **Persistir el Modo cocina (paso, marcado, instante de fin) para "reanudar donde lo dejé".** Es la
   feature que **haría falta** para que tocar la notificación devuelva al paso tras morir el proceso; hoy
   se acepta la limitación (la app abre en su estado de arranque) porque persistir el modo es otro
   alcance (dominio + Room) y un Non-Goal explícito de la entrada. Descartado **en esta feature**.
   > **Adoptada (`cooking-timer-foreground-service`, 2026-10-15):** esa feature llegó y **sí** persiste la
   > sesión del Modo cocina (una fila en `cooking_session.db`, base propia, columna JSON con
   > `RecipeJson`), con lo que el toque devuelve al paso exacto incluso con el proceso muerto. Se
   > mantiene que la persistencia es de la **sesión** (nada de historial ni de preferencias de cocina) y
   > que la reanudación es **explícita** (solo desde la notificación). Ver
   > `docs/adr/0008-cooking-timer-foreground-service.md`.
5. **`CompositionLocal` para la costura** (patrón `LocalGeneratedRecipeArtworkSource`). Menos parámetros,
   pero la spec fija la costura **por parámetro** (como `dishImageSettings`) y un `CompositionLocal` haría
   que la dependencia no se viera en la firma; además los previews necesitarían proveer el default.
   Descartado.
6. **Poner el título y el paso en la notificación** ("Paso 2 de 5 · Tortilla de patatas"). Más
   informativo, pero exige que la receta cruce al `Intent` del sistema y contradice la decisión de
   contenido **genérico** (sin datos de la receta fuera del proceso). Descartado.
7. **Base de tiempo `RTC_WAKEUP` / `System.currentTimeMillis()`.** Se descarta por coherencia con el
   temporizador puro: `ELAPSED_REALTIME_WAKEUP` + `SystemClock.elapsedRealtime()` mide tiempo transcurrido
   real (no "hora del mundo") y no se descoloca si el reloj del sistema cambia.
8. **Pedir `SCHEDULE_EXACT_ALARM`.** Con `minSdk 33` obligaría a mandar al usuario a los Ajustes del
   sistema para un caso de uso (temporizador) que `USE_EXACT_ALARM` cubre sin diálogo. Descartado.
9. **Petición del permiso de forma contextual** (al pulsar "Iniciar", primera vez). Se descarta porque el
   permiso es de arranque y no debe mezclarse con la acción del temporizador: pedirlo una vez por proceso
   al abrir la app es más predecible y no gatea nada. Descartado.
   > **Superada (`cooking-timer-alarm-permission-context`, 2026-10-15):** la correctiva invierte este
   > descarte y adopta la petición contextual con rationale, porque el diálogo al arrancar sin
   > explicación es un defecto de UX (ver la enmienda a la decisión 11).

## Consecuencias

- **Con la app en segundo plano o el móvil bloqueado el aviso funciona** (escenario 2): la alarma la
  ejecuta el sistema y el receptor publica la notificación por el canal propio, con sonido y vibración.
- **Pausar, Reiniciar, cambiar de paso y salir cancelan** (escenario 4): el efecto de la política corre al
  **cambiar de estado** del temporizador, así que los `tick` no reprograman nada; la cancelación es
  idempotente y la salida la cubre también el `DisposableEffect` del `onDispose`.
  > **Matizada (`cooking-timer-notification-reliability`, 2026-10-16):** ya no hay efecto con
  > `state.timer.status` como clave; cada gesto ("Iniciar"/"Reanudar"/"Pausar"/"Reiniciar"/"Anterior"/
  > "Siguiente") consulta la política en el **mismo punto** con la **misma** lectura del reloj que
  > alimenta el estado. La cancelación sigue siendo idempotente y el `onDispose` sigue cubriendo la salida.
- **Disparo único** (escenario 5): `RUNNING → FINISHED` devuelve `Cancel` (no reprograma), los `tick`
  posteriores `None` y el id fijo reemplaza en lugar de duplicar.
  > **Revisada (`cooking-timer-finish-notification`, 2026-10-16):** `RUNNING → FINISHED` ya **no** emite
  > `Cancel`, sino `None`: cancelar ahí abría una carrera con el disparo de `AlarmManager` y el aviso
  > podía no publicarse. La alarma sigue viva y publica una sola vez (id fijo; los `tick` posteriores
  > siguen dando `None`). Tocar el aviso también vuelve al paso con el temporizador ya en Terminado, y
  > "Atrás"/"Terminar" retiran la `1001` con `dismissFinishNotification`
  > (`docs/specs/cooking-timer-finish-notification.md`).
- **Permiso denegado = sin aviso, sin crash** (escenario 6): `notify` comprueba permiso y
  `areNotificationsEnabled()` antes de publicar; el Modo cocina queda intacto. Un paso sin duración nunca
  programa (escenario 7).
- **Proceso muerto o actividad destruida**: la alarma sigue programada y el receptor se recrea para
  publicar; al tocar, la app abre en su estado de arranque (sin Modo cocina) porque el modo **no** se
  persiste. Limitación aceptada y documentada en `docs/risks-and-open-questions.md`.
  > **Revisada (`cooking-timer-foreground-service`, 2026-10-15):** con la sesión del Modo cocina
  > persistida (ADR 0008), tocar la notificación **sí** devuelve al paso exacto —En marcha con el tiempo
  > restante o Terminado si el instante de fin ya pasó—, también con el proceso muerto. El arranque
  > **normal** (lanzador) sigue abriendo la app sin el Modo cocina.
- **Reinicio del dispositivo**: `ELAPSED_REALTIME` y las alarmas no sobreviven a un reinicio; no se añade
  `RECEIVE_BOOT_COMPLETED` (el Modo cocina tampoco sobrevive).
- **Precisión bajo batería agresiva**: `setExactAndAllowWhileIdle` está sujeto a las políticas del
  fabricante y a los límites de Doze; el comportamiento real se comprueba en la prueba **manual** del
  emulador, no automatizable (AGENTS.md).
- **Notificación obsoleta en la bandeja**: como `Cancel` no la retira, tras "Reiniciar" puede quedar el
  aviso ya mostrado en la bandeja (se reemplaza solo con el mismo id en el siguiente disparo). Cosmético,
  corregible con una feature correctiva.
  > **Matizada (`cooking-timer-finish-notification`, 2026-10-16):** "Atrás"/"Terminar" sí retiran la
  > `1001` mediante `CookingAlarm.dismissFinishNotification` (comando aparte, **no** en el `onDispose`);
  > una rotación o un `Reiniciar` siguen sin retirarla (el aviso ya mostrado aguanta hasta que el usuario
  > lo descarte o salga del modo).
- **Canal ya creado con otros ajustes**: Android **no** actualiza los atributos de un canal existente;
  cambiarlos exige cambiar `CHANNEL_ID`. No aplica a un canal nuevo.
- **Acabado (tono, patrón de vibración, aspecto en la pantalla de bloqueo)**: lo revisa la persona en el
  emulador; un defecto se corrige con una feature correctiva. La notificación es una **superficie del
  sistema operativo**, así que no hay pantalla nueva ni capturas de diseño (excepción registrada en
  `DESIGN.md`).
- **Alternativa de una línea para el rollback**: quitar `POST_NOTIFICATIONS`/`USE_EXACT_ALARM` del
  manifiesto no rompe nada (el receptor no publica y el adaptador degrada), pero deja el aviso mudo.
