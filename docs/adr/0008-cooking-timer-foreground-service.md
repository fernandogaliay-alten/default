# ADR 0008: La cuenta atrás del temporizador vive en un servicio en primer plano y la sesión del Modo cocina se persiste

## Estado

**Aceptado** (2026-10-15). Implementa la feature `cooking-timer-foreground-service` (`area: integration`,
depende de `cooking-timer-alarm-permission-context`, `accepted`), pedida por la persona tras probar
`cooking-timer-alarm`: el aviso aceptado es **solo el de fin** y la cuenta atrás **no se ve** fuera de la
app. Enmienda de forma **acotada** el Non-Goal "Notificaciones y recordatorios" de `docs/build-brief.md`
(misma clase de enmienda que `cooking-mode`/`cooking-timer-alarm`) y **revisa** las alternativas 3
(`foreground service` descartado) y 4 (persistir el Modo cocina descartado) del ADR
`0007-cooking-timer-alarm.md`.

## Contexto

El Modo cocina (`docs/specs/cooking-mode.md`) tiene un temporizador por paso **puro y determinista**
(`CookingTimerState`) y, desde `cooking-timer-alarm` (ADR 0007), un aviso de fin de sistema (alarma
exacta + receptor + notificación con canal `cooking_timer`, id `1001`). Pero el **reloj** sigue viviendo
en un `LaunchedEffect` de `CookingModeScreen`: **solo late con la composición activa**, así que la cuenta
atrás **no se ve** con la app minimizada o el móvil bloqueado — justo el caso de uso ("pongo el
temporizador y sigo cocinando"). Y el Modo cocina **no se persistía**: al tocar la notificación del
aviso con el proceso muerto, la app abría en su estado de arranque, sin el paso (limitación aceptada y
documentada en el ADR 0007).

Restricciones del repositorio que condicionan la decisión:

- La verificación canónica corre en **JVM** (`bash init.sh` → `./gradlew test` + `lint`); `AGENTS.md`
  **no** automatiza UI de emulador ni el sistema de notificaciones. Lo automatizable es la **decisión
  pura** y lo que toca framework con Robolectric.
- `minSdk = 33`, `targetSdk = 36`, `compileSdk 36.1`; la app **no** se publica en Play
  (`docs/technical-discovery.md`), así que no hay formulario de revisión de tipos de primer plano.
- Patrón aceptado de costura Android: `interface` + default `NoOp…`, lógica **pura** en un paquete sin
  `android.*` y adaptador delgado en `data/`. Degradación **silenciosa** (ADR 0005) y **sin dependencias
  nuevas**.
- `androidx.core` está en **1.10.1**: `NotificationCompat.Builder` tiene `setUsesChronometer`,
  `setChronometerCountDown`, `setOngoing`, `setOnlyAlertOnce` y `setSilent`, pero
  `ServiceCompat.startForeground(...)` **no existe** todavía.
- El Non-Goal de `cooking-timer-alarm` decía explícitamente que el `foreground service` y "volver al
  paso" eran **esta** feature.

## Decisión

1. **El `foreground service` es el dueño de la cuenta atrás visible; la alarma exacta one-shot aceptada
   sigue siendo la única dueña del "¡Listo!"** (no se sustituye). Es el **único** mecanismo que mantiene
   el proceso vivo con la app en segundo plano, pero el aviso de fin debe llegar **siempre** incluso si
   el sistema mata el proceso: con **un solo** publicador no hay sonido/vibración duplicados ni carrera
   entre dos relojes, y se conserva tal cual el comportamiento aceptado (id fijo `1001`, disparo único,
   degradación a alarma inexacta). `USE_EXACT_ALARM`, `POST_NOTIFICATIONS`, `VIBRATE`,
   `SCHEDULE_EXACT_ALARM (maxSdkVersion=32)` y el receptor **no se tocan**.
2. **La cuenta atrás la pinta el sistema**: `setUsesChronometer(true)` + `setChronometerCountDown(true)`
   + `setWhen(fin en hora de pared)` en la notificación persistente (id **fijo** `1000`, `ongoing`,
   `onlyAlertOnce`, `silent`, `CATEGORY_STOPWATCH`, el small icon monocromo aceptado, el **mismo** canal
   `cooking_timer`). Cero despertares por segundo, sin deriva del lado de la app y sin depender de que el
   proceso esté despierto para repintar texto. El servicio solo espera **un** despertar (el instante de
   fin real, monotónico) y entonces retira la notificación (`STOP_FOREGROUND_REMOVE`) y se detiene.
3. **Tipo de primer plano `specialUse`** con su `<property
   android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" …>` y los dos permisos **normales**
   `FOREGROUND_SERVICE` y `FOREGROUND_SERVICE_SPECIAL_USE`. No existe un tipo específico para
   temporizadores y el más afín (`shortService`) está limitado a ~3 minutos; el subtipo documentado es
   "temporizador de cocina en curso, visible en la notificación". `exported="false"` y `stopWithTask` por
   defecto.
4. **La sesión del Modo cocina se persiste en Room, una fila, base propia** (`cooking_session.db`,
   `version = 1`, `exportSchema = false`): columna JSON con `RecipeJson` (patrón `suggestion_batch`) +
   `stepIndex`, `completedOrders` (CSV, criterio del `Saver` aceptado), estado/millis del temporizador y
   el **instante de fin** (`ELAPSED_REALTIME`, solo no nulo En marcha). Base separada para no migrar
   ninguna de las aceptadas. `SharedPreferences` se descarta para una receta completa.
5. **Se persiste por cambio significativo, no por `tick`**: basta con el instante de fin para recalcular
   lo que queda al reanudar, así que no hay una escritura por segundo.
6. **La lógica de la sesión es pura y determinista** (`ui.cooking.CookingSession`): proyecta el instante
   de fin (`of`, `now + remaining`, solo En marcha) y lo recalcula al reanudar (`resumedMode`, con
   `FINISHED`/`0` si el instante ya pasó), con los instantes **inyectados** y sin leer el reloj. La
   costura `CookingSessionStore` tiene default `NoOpCookingSessionStore` y contrato "no lanza". El
   adaptador (`RoomCookingSessionStore`) absorbe con `runCatching`: una fila ilegible se comporta como
   "sin sesión".
7. **La reanudación es explícita**: solo el `Intent` con `EXTRA_RESUME_COOKING` (booleano, lo **único**
   que cruza al sistema junto al instante de fin del servicio) abre el Modo cocina en su paso; un
   arranque normal **no** lo abre, así que una sesión antigua no secuestra el arranque y no hace falta
   ventana de frescura ni reloj de pared. `MainActivity` lo convierte en el estado `resumeRequest` (de
   `onCreate` **y** de `onNewIntent`, con `setIntent`), `MainShell` carga la sesión y abre el overlay si
   es **resumible** (En marcha o Pausado).

   > **Ampliada (`cooking-timer-finish-notification`, 2026-10-16):** la puerta pasa a
   > `CookingSession.reopensFromNotification` (`isResumable` **o** Terminado), porque el usuario puede
   > tocar el aviso "¡Listo!" **después** de que el instante de fin pase: en ese caso la sesión persistida
   > está en Terminado con `remaining = 0` y debe reabrir el paso igual (sin rearmar alarma ni servicio).
   > `isResumable` **no** cambia de semántica; el predicado nuevo lo **compone**.
8. **Reanudar una sesión En marcha rearma el trabajo del sistema**: la sesión puede sobrevivir a un
   proceso cuya alarma y servicio ya no existen, así que la pantalla aplica **una vez**
   `CookingAlarmPolicy.onTimerChange` tratando la reanudación como `IDLE → RUNNING` (misma decisión pura,
   sin política nueva).
9. **El adaptador del aviso suma la pata del servicio**: `CookingAlarmCommand.Schedule(at)` arranca
   además el servicio (`startForegroundService` con un `Intent` que **solo** lleva el instante de fin) y
   `Cancel` lo para (`stopService`), de modo que la notificación persistente existe **solo** En marcha.
   Las dos patas son independientes (cada una con su `runCatching`) y el arranque ocurre siempre desde una
   interacción del usuario con la app en primer plano, así que no aplica la restricción de arranque en
   segundo plano.
10. **`onDispose` no borra la sesión; "Atrás"/"Terminar" sí**: una muerte de actividad o una rotación no
    debe perderla (es justo el caso a cubrir), mientras que salir del modo es una decisión explícita del
    usuario; el borrado se completa **antes** de cerrar la composición.
11. **Sin dependencias nuevas**: `Service`, `NotificationCompat`/`NotificationManagerCompat`,
    `ContextCompat`, `AlarmManager` (aceptado), Room y coroutines ya están. Se usa el
    `startForeground(id, notification)` del framework (no `ServiceCompat`, que no existe en `core 1.10.1`)
    con el tipo declarado en el manifiesto.
12. **Sin cambios en lo aceptado**: `CookingTimerState`, `CookingModeState`, `CookingModeFormat`,
    `CookingAlarmPolicy`, `CookingAlarmCommand`, la costura `CookingAlarm`,
    `NotificationPermissionPolicy`/`NotificationPermissionPrompt` y la UI/microcopy del Modo cocina
    quedan **sin diff**; el aviso de fin sigue publicándose igual (solo gana el flag de reanudación en su
    `Intent` de contenido).

    > **Revisado (`cooking-timer-finish-notification`, 2026-10-16):** esta decisión deja de aplicar tal
    > cual. La feature correctiva del aviso duradero toca `CookingAlarmPolicy` (la arista
    > `RUNNING → FINISHED` pasa de `Cancel` a `None`), `CookingAlarmCommand.Cancel` (KDoc) y la costura
    > `CookingAlarm` (método nuevo `dismissFinishNotification`), que eran las piezas declaradas "sin
    > diff". El servicio (`CookingTimerService`), el receptor, el canal, los ids y el microcopy **siguen**
    > intactos.

Alcance: `ui/cooking/CookingSession.kt` y `CookingSessionStore.kt` (nuevos, puros), `data/cooking/`
(persistencia Room, servicio y notificación persistente; adaptador y receptor con la pata nueva),
`ui/cooking/CookingModeScreen.kt` (persistencia, rearme y borrado al salir), `ui/MainShell.kt`/
`ui/SazonApp.kt`/`MainActivity.kt` (costura, `resumeRequest` y `onNewIntent`), el manifiesto y los tests
JVM/Robolectric. **No** se toca `domain/**`, la Taxonomía, `seed-recipes.json`, `assets/**`,
`ui/theme/**`, las bases Room aceptadas, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `init.sh`,
`scripts/**`, `ic_notification_timer.xml` ni `docs/design/concepts/**`.

## Alternativas consideradas

1. **Sustituir la alarma por el servicio** (que el servicio publique el "¡Listo!" al llegar a cero).
   Ahorraría una pieza, pero el aviso de fin dejaría de llegar si el sistema mata el proceso y chocaría
   con el `Cancel` que la política emite en `RUNNING → FINISHED` (cuando el `tick` de la app llega a cero
   en primer plano), añadiendo una carrera difícil de probar en JVM. Descartado.
   > **Revisada (`cooking-timer-finish-notification`, 2026-10-16):** el `Cancel` de `RUNNING → FINISHED`
   > desapareció (ver ADR 0007, decisión 9 revisada) y esa carrera concreta ya no existe, pero la
   > alternativa **sigue descartada**: el servicio no es un productor fiable del aviso (el `delay` corre
   > en el proceso de la app, que el sistema puede matar), mientras que la alarma la ejecuta el sistema y
   > publica el aviso **siempre**. La misma decisión de "Opción A" (servicio pintor, alarma como único
   > productor) se mantiene.
2. **`shortService`** como tipo de primer plano. Es el más afín pero está limitado a ~3 minutos: un
   temporizador de cocina puede durar más y el servicio moriría a mitad. Descartado.
3. **Bucle de texto por segundo** (republicar la notificación con `NotificationManager.notify` en cada
   `tick`). Da control total del texto, pero son ~60 publicaciones por minuto (batería, IPC) y deriva
   respecto al instante real. Descartado: lo pinta el sistema con el cronómetro.
4. **Persistir la sesión en `SharedPreferences`.** Sencillo para un bit, pero aquí viaja una `Recipe`
   completa (JSON): desborda el uso razonable de preferencias y no da un sitio natural para el borrado ni
   la evolución del esquema. Descartado frente a Room (misma pauta que `suggestion_batch`).
5. **Reanudar siempre al abrir la app** (si hay sesión, abrir el Modo cocina). "Reanudar donde lo dejé"
   sin tocar nada, pero una sesión antigua secuestraría el arranque normal y obligaría a ventanas de
   frescura y reloj de pared. Descartado: la reanudación es **explícita** por el `Intent`.
6. **`START_STICKY`** para que el sistema recree el servicio. Recuperaría la cuenta atrás tras una muerte
   del proceso, pero exigiría releer la sesión desde el propio servicio (una segunda fuente de verdad
   para el paso y el fin) y el sistema podría recrearlo sin notificación útil. Descartado: `START_NOT_STICKY`
   y la sesión persistida es la que devuelve al paso (la cuenta atrás se pierde, el "¡Listo!" no).
7. **Canal propio de importancia baja para la notificación persistente.** Evitaría que la primera
   publicación pudiera alertar (el canal `cooking_timer` es de importancia alta y sus atributos son
   inmutables), pero crearía un segundo canal y un segundo microcopy sin necesidad comprobada. Descartado
   por ahora; queda como remedio de una feature correctiva si la prueba manual lo pide.
8. **`CompositionLocal` para la costura nueva.** Menos parámetros, pero la spec fija la costura **por
   parámetro** (como `cookingAlarm`/`dishImageSettings`) y un `CompositionLocal` escondería la dependencia
   en la firma. Descartado.

## Consecuencias

- **La cuenta atrás se ve con la app minimizada o el móvil bloqueado** (escenario 2) y **desaparece** al
  Pausar/Reiniciar/cambiar de paso/salir (escenario 4), porque la notificación persistente existe **solo**
  En marcha y `Cancel` para el servicio.
- **El "¡Listo!" sigue llegando siempre** (escenario 3): lo publica la alarma aceptada, una sola vez, con
  su id fijo. El servicio no publica nada al terminar, así que no hay duplicados.
  > **Corregida (`cooking-timer-finish-notification`, 2026-10-16):** la alarma era el único productor,
  > pero la política la **cancelaba** en `RUNNING → FINISHED`, así que el aviso **no siempre llegaba**
  > (carrera entre el `tick` de la composición y el disparo de `AlarmManager`). Desde la correctiva esa
  > arista devuelve `None`: la alarma se deja viva y publica **siempre**; el servicio se para **solo**
  > con su `delay` (no se mantiene vivo para sostener el aviso), y la `1001` se retira en la salida
  > explícita con `dismissFinishNotification`.
- **El toque vuelve al paso exacto** (escenarios 5 y 6), con la cuenta atrás recalculada desde el instante
  de fin: con el proceso muerto, la app lee la sesión persistida y monta el paso (En marcha con el tiempo
  restante o Terminado si ya pasó).
- **Una muerte de actividad o una rotación no pierde la sesión**; salir del modo **sí** la borra (una
  notificación obsoleta ya no reanuda nada).
- **Degradación silenciosa** (ADR 0005): sin `POST_NOTIFICATIONS` no hay notificación (el servicio sigue
  corriendo, sin crash) y el Modo cocina, la cuenta atrás en pantalla y el "¡Listo!" en pantalla siguen
  intactos; si `startForeground` falla, el servicio se apaga sin lanzar; una fila ilegible se comporta
  como "sin sesión".
- **Quitar la tarea de recientes o matar el proceso** (escenario 5 del ADR 0007, revisado): la cuenta
  atrás desaparece, pero el "¡Listo!" sigue llegando y el toque **sí** devuelve al paso (sesión
  persistida). No se añade `START_STICKY` ni `RECEIVE_BOOT_COMPLETED`; un **reinicio del dispositivo**
  deja la sesión obsoleta sin notificación que la reanude (se acepta y se documenta).
- **Peso y coste**: una fila con la receta en JSON; una escritura por cambio significativo (no por tick).
  El arranque de la app solo **abre** la base de datos, no la lee, así que no añade trabajo al primer
  frame.
- **Piezas vivas del sistema**: el tipo `specialUse` y los dos permisos de primer plano son superficies
  sensibles a cambios de política; si algún día se publicara en Play, el subtipo exigiría justificación
  en la consola.
- **Canal reutilizado de importancia alta**: la primera publicación de la persistente podría alertar una
  vez (no se puede bajar la importancia de un canal existente). Se mitiga con `onlyAlertOnce`/`silent` y
  se comprueba en la prueba **manual**; un defecto se corrige con una **feature correctiva**.
- **Acabado** (aspecto del cronómetro en la pantalla de bloqueo, orden de las dos notificaciones mientras
  coexisten, tono/patrón): lo revisa la persona en el emulador. Las dos notificaciones son **superficies
  del sistema operativo**, así que no hay pantalla nueva ni capturas de diseño (excepción registrada en
  `DESIGN.md`).
- **Reversible en una línea por pata**: dejar de arrancar el servicio es quitar el bloque de
  `startForegroundService` del adaptador; la sesión persistida es aditiva (nadie la lee si no se toca la
  notificación).
