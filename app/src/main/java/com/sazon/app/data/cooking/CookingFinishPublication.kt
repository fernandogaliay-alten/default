package com.sazon.app.data.cooking

/**
 * Decision **pura** sobre si el aviso de fin (`1001`) debe publicarse en una llamada concreta (sin
 * Compose ni Android en el cuerpo; se prueba en JVM con Robolectric al margen). Es la pieza que
 * sostiene la **idempotencia** cuando hay **dos** publicadores potenciales del "¡Listo!": el
 * **servicio** en primer plano (productor primario, publica al vencer su `delay`) y la **alarma
 * exacta** de respaldo (dispara en el instante de fin, o mas tarde si el fabricante la difiere o el
 * proceso murio).
 *
 * El **id fijo** de la notificacion evita dos notificaciones, pero **no** evita el segundo
 * sonido/vibracion: lo que distingue "este fin ya se anuncio" es el **sello de fin** (el instante de fin
 * en **hora de pared**, calculado **una vez** al programar y llevado en los dos `Intent`), que se
 * publica como `when` de la `1001`.
 *
 * Es **determinista** y **nunca lee el reloj**: el instante actual y el `when` del aviso ya mostrado (si
 * lo hay) entran **inyectados**.
 */
enum class CookingFinishPublication {

    /** Procede publicar la `1001` con el sello de este fin. */
    PUBLISH,

    /** Ese fin ya esta anunciado (hay una `1001` con el mismo sello): no se vuelve a alertar. */
    ALREADY_ANNOUNCED,

    /** El sello es mas viejo que la ventana de frescura: es el aviso de un paso **anterior**. */
    STALE,

    ;

    companion object {

        /**
         * Ventana de frescura del sello (ms): un aviso que llega mas tarde que esto es ya "el aviso de
         * un paso anterior" (el fabricante deferio/coalescio la alarma, o el proceso estaba muerto) y se
         * **descarta** en vez de sonar fuera de contexto.
         */
        const val FRESHNESS_WINDOW_MILLIS = 60_000L

        /**
         * Decide con el reloj de pared [nowWallClockMillis], el sello del fin a anunciar
         * [finishWallClockMillis] y el `when` del aviso `1001` **ya mostrado**
         * [shownFinishWallClockMillis] (`null` si no hay ninguno).
         *
         * Orden de reglas: mismo sello mostrado -> [ALREADY_ANNOUNCED]; sello fuera de la ventana de
         * frescura -> [STALE]; en cualquier otro caso -> [PUBLISH]. Un sello ligeramente en el futuro (la
         * alarma puede dispararse antes del instante calculado) **no** es caducado.
         */
        fun decide(
            nowWallClockMillis: Long,
            finishWallClockMillis: Long,
            shownFinishWallClockMillis: Long?,
        ): CookingFinishPublication = when {
            shownFinishWallClockMillis == finishWallClockMillis -> ALREADY_ANNOUNCED

            nowWallClockMillis - finishWallClockMillis > FRESHNESS_WINDOW_MILLIS -> STALE

            else -> PUBLISH
        }
    }
}
