package com.sazon.app.data.cooking

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Decision **pura** de publicacion del aviso de fin (`docs/specs/cooking-timer-notification-reliability.md`,
 * escenarios 3, 4 y 6): con **dos** publicadores posibles (el servicio en primer plano y la alarma de
 * respaldo), el id fijo evita dos notificaciones pero **no** dos sonidos/vibraciones; lo que decide si un
 * fin ya esta anunciado es el **sello de fin** (el `when` de la `1001`) y una **ventana de frescura** que
 * descarta el aviso de un paso anterior.
 *
 * Logica pura sin Compose ni Android: el instante actual y el sello del aviso mostrado entran
 * **inyectados**; nunca se lee el reloj.
 */
class CookingFinishPublicationTest {

    private val sello = 1_600_000L

    @Test
    fun sin_aviso_mostrado_y_sello_fresco_se_publica() {
        assertEquals(
            CookingFinishPublication.PUBLISH,
            CookingFinishPublication.decide(
                nowWallClockMillis = sello + 500L,
                finishWallClockMillis = sello,
                shownFinishWallClockMillis = null,
            ),
        )
    }

    @Test
    fun el_mismo_sello_ya_anunciado_no_vuelve_a_publicarse() {
        assertEquals(
            "El servicio ya publico ese fin: la alarma de respaldo no debe volver a alertar",
            CookingFinishPublication.ALREADY_ANNOUNCED,
            CookingFinishPublication.decide(
                nowWallClockMillis = sello + 100L,
                finishWallClockMillis = sello,
                shownFinishWallClockMillis = sello,
            ),
        )
    }

    @Test
    fun un_aviso_mostrado_de_otro_fin_no_bloquea_el_legitimo_siguiente() {
        assertEquals(
            "El id fijo no debe silenciar el fin del paso siguiente",
            CookingFinishPublication.PUBLISH,
            CookingFinishPublication.decide(
                nowWallClockMillis = sello + 100L,
                finishWallClockMillis = sello,
                shownFinishWallClockMillis = sello - 600_000L,
            ),
        )
    }

    @Test
    fun un_sello_mas_viejo_que_la_ventana_de_frescura_se_descarta() {
        assertEquals(
            "La alarma diferida del paso anterior no debe sonar en el paso siguiente",
            CookingFinishPublication.STALE,
            CookingFinishPublication.decide(
                nowWallClockMillis = sello + CookingFinishPublication.FRESHNESS_WINDOW_MILLIS + 1L,
                finishWallClockMillis = sello,
                shownFinishWallClockMillis = null,
            ),
        )
    }

    @Test
    fun el_limite_de_la_ventana_de_frescura_todavia_publica() {
        assertEquals(
            CookingFinishPublication.PUBLISH,
            CookingFinishPublication.decide(
                nowWallClockMillis = sello + CookingFinishPublication.FRESHNESS_WINDOW_MILLIS,
                finishWallClockMillis = sello,
                shownFinishWallClockMillis = null,
            ),
        )
    }

    @Test
    fun el_sello_ya_anunciado_manda_sobre_la_frescura() {
        assertEquals(
            "Si la 1001 con ese sello sigue en la bandeja, no es un aviso caducado",
            CookingFinishPublication.ALREADY_ANNOUNCED,
            CookingFinishPublication.decide(
                nowWallClockMillis = sello + 10 * CookingFinishPublication.FRESHNESS_WINDOW_MILLIS,
                finishWallClockMillis = sello,
                shownFinishWallClockMillis = sello,
            ),
        )
    }

    @Test
    fun un_sello_en_el_futuro_no_es_caducado() {
        assertEquals(
            "La alarma puede dispararse antes del instante calculado",
            CookingFinishPublication.PUBLISH,
            CookingFinishPublication.decide(
                nowWallClockMillis = sello - 200L,
                finishWallClockMillis = sello,
                shownFinishWallClockMillis = null,
            ),
        )
    }

    @Test
    fun la_ventana_de_frescura_es_de_sesenta_segundos() {
        assertEquals(60_000L, CookingFinishPublication.FRESHNESS_WINDOW_MILLIS)
        assertEquals(
            "Justo despues del limite ya se descarta",
            CookingFinishPublication.STALE,
            CookingFinishPublication.decide(
                nowWallClockMillis = sello + 60_001L,
                finishWallClockMillis = sello,
                shownFinishWallClockMillis = null,
            ),
        )
    }
}
